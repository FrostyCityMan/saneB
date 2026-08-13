package com.saneb.domain.announcementsource.provider.content;

import com.saneb.domain.announcementsource.provider.content.ProviderContentCodes.FailureCode;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpTimeoutException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 등록된 지자체 source와 같은 공식 host의 정적 HTML 상세본문만 조회합니다.
 */
@Component
public class LocalGovernmentNoticeProviderContentClient implements ProviderContentClient {

    private static final String PROVIDER_CODE = "LOCAL_GOV_NOTICE";
    private static final int MAX_TRANSPORT_ATTEMPTS = 2;
    private static final long ABSOLUTE_CONNECT_TIMEOUT_MILLIS = 3000L;
    private static final long ABSOLUTE_READ_TIMEOUT_MILLIS = 7000L;
    private static final int ABSOLUTE_MAX_RESPONSE_BYTES = 2 * 1024 * 1024;
    private static final int ABSOLUTE_MAX_REDIRECTS = 3;
    private static final int ABSOLUTE_MAX_HOST_CONCURRENCY = 2;
    private static final Pattern CHARSET_PATTERN = Pattern.compile(
            "charset\\s*=\\s*['\\\"]?([^\\s;'\\\">]+)",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern META_CHARSET_PATTERN = Pattern.compile(
            "<meta\\b[^>]*charset\\s*=\\s*['\\\"]?\\s*([^\\s;'\\\">/]+)",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern ATTACHMENT_FILE_EXTENSION_PATTERN = Pattern.compile(
            "\\.(?:pdf|hwp|hwpx|doc|docx|xls|xlsx|ppt|pptx|zip|rar|7z|alz|egg|tar|gz|tgz|rtf|txt|csv"
                    + "|odt|ods|odp)"
                    + "(?:$|[./?#&])",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern ATTACHMENT_ENDPOINT_PATTERN = Pattern.compile(
            "(?:^|[/?:&=._;-])"
                    + "(?:attachments?|attachmentdownloads?|attachfiles?(?:downs?)?|atchfiles?(?:downs?)?"
                    + "|downloads?|downloadfiles?|filedownloads?|filedowns?|getfiles?|openfiles?|viewfiles?"
                    + "|files?)"
                    + "(?:$|[/?:&=._;-])",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern ATTACHMENT_QUERY_PARAMETER_PATTERN = Pattern.compile(
            "(?:^|[?&])"
                    + "(?:attachment|attachfile|atchfile|download|file)"
                    + "(?:id|no|sn|seq|name|url|path)?=",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern ATTACHMENT_HANDLER_PATTERN = Pattern.compile(
            "(?:attachments?|attachfiles?|atchfiles?|downloads?|downloadfile|filedownloads?|filedowns?"
                    + "|openfile|getfile|viewfile)",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern ATTACHMENT_CONTAINER_MARKER_PATTERN = Pattern.compile(
            "(?:^|[-_\\s])(?:attachments?|attach|atchfiles?|downloads?|files?)(?:$|[-_\\s])",
            Pattern.CASE_INSENSITIVE
    );

    private final boolean enabled;
    private final Duration readTimeout;
    private final int maxResponseBytes;
    private final int maxRedirects;
    private final int maxHostConcurrency;
    private final String userAgent;
    private final ProviderContentHttpTransport httpTransport;
    private final ProviderContentUrlValidator urlValidator;
    private final Map<String, Semaphore> hostSemaphores = new ConcurrentHashMap<>();

    @Autowired
    public LocalGovernmentNoticeProviderContentClient(
            @Value("${saneb.announcement-source.local-government.detail-body.enabled:false}") boolean enabled,
            @Value("${saneb.announcement-source.local-government.detail-body.connect-timeout-millis:3000}")
            int connectTimeoutMillis,
            @Value("${saneb.announcement-source.local-government.detail-body.read-timeout-millis:7000}")
            int readTimeoutMillis,
            @Value("${saneb.announcement-source.local-government.detail-body.max-response-bytes:2097152}")
            int maxResponseBytes,
            @Value("${saneb.announcement-source.local-government.detail-body.max-redirects:3}")
            int maxRedirects,
            @Value("${saneb.announcement-source.local-government.detail-body.max-host-concurrency:2}")
            int maxHostConcurrency,
            @Value("${saneb.announcement-source.local-government.user-agent:saneB-notice-collector/1.0}")
            String userAgent
    ) {
        Duration connectTimeout = Duration.ofMillis(Math.min(
                ABSOLUTE_CONNECT_TIMEOUT_MILLIS,
                Math.max(1, connectTimeoutMillis)
        ));
        this.enabled = enabled;
        this.readTimeout = Duration.ofMillis(Math.min(
                ABSOLUTE_READ_TIMEOUT_MILLIS,
                Math.max(1, readTimeoutMillis)
        ));
        this.maxResponseBytes = Math.min(
                ABSOLUTE_MAX_RESPONSE_BYTES,
                Math.max(1024, maxResponseBytes)
        );
        this.maxRedirects = Math.min(ABSOLUTE_MAX_REDIRECTS, Math.max(0, maxRedirects));
        this.maxHostConcurrency = Math.min(
                ABSOLUTE_MAX_HOST_CONCURRENCY,
                Math.max(1, maxHostConcurrency)
        );
        this.userAgent = selectUserAgent(userAgent);
        this.httpTransport = new JdkProviderContentHttpTransport(connectTimeout);
        this.urlValidator = new ProviderContentUrlValidator(InetAddress::getAllByName);
    }

    LocalGovernmentNoticeProviderContentClient(
            boolean enabled,
            Duration readTimeout,
            int maxResponseBytes,
            int maxRedirects,
            int maxHostConcurrency,
            String userAgent,
            ProviderContentHttpTransport httpTransport,
            ProviderContentUrlValidator urlValidator
    ) {
        this.enabled = enabled;
        Duration requestedReadTimeout = Objects.requireNonNull(readTimeout, "readTimeout is required");
        if (requestedReadTimeout.isNegative() || requestedReadTimeout.isZero()) {
            requestedReadTimeout = Duration.ofMillis(1);
        }
        this.readTimeout = requestedReadTimeout.compareTo(Duration.ofMillis(ABSOLUTE_READ_TIMEOUT_MILLIS)) > 0
                ? Duration.ofMillis(ABSOLUTE_READ_TIMEOUT_MILLIS)
                : requestedReadTimeout;
        this.maxResponseBytes = Math.min(
                ABSOLUTE_MAX_RESPONSE_BYTES,
                Math.max(1024, maxResponseBytes)
        );
        this.maxRedirects = Math.min(ABSOLUTE_MAX_REDIRECTS, Math.max(0, maxRedirects));
        this.maxHostConcurrency = Math.min(
                ABSOLUTE_MAX_HOST_CONCURRENCY,
                Math.max(1, maxHostConcurrency)
        );
        this.userAgent = selectUserAgent(userAgent);
        this.httpTransport = Objects.requireNonNull(httpTransport, "httpTransport is required");
        this.urlValidator = Objects.requireNonNull(urlValidator, "urlValidator is required");
    }

    @Override
    public String selectProviderCode() {
        return PROVIDER_CODE;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public ProviderContentResult selectContent(ProviderContentRequest request) {
        Objects.requireNonNull(request, "request is required");
        if (!enabled) {
            return ProviderContentResult.disabled(request);
        }
        if (!PROVIDER_CODE.equals(request.providerCode())) {
            return ProviderContentResult.failure(
                    request,
                    FailureCode.PROVIDER_UNSUPPORTED,
                    null,
                    null,
                    0,
                    0
            );
        }

        ProviderContentUrlValidator.ValidatedRequest validatedRequest;
        try {
            validatedRequest = urlValidator.selectValidatedRequest(
                    request.registeredSourceUrl(),
                    request.officialDetailUrl()
            );
        } catch (ProviderContentValidationException exception) {
            return ProviderContentResult.failure(
                    request,
                    exception.selectFailureCode(),
                    null,
                    null,
                    0,
                    0
            );
        }

        Semaphore semaphore = hostSemaphores.computeIfAbsent(
                validatedRequest.allowedHost(),
                ignored -> new Semaphore(maxHostConcurrency)
        );
        boolean acquired = false;
        try {
            semaphore.acquire();
            acquired = true;
            return selectContentWithRetry(request, validatedRequest);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return ProviderContentResult.failure(
                    request,
                    FailureCode.INTERRUPTED,
                    validatedRequest.detailUri(),
                    null,
                    0,
                    0
            );
        } finally {
            if (acquired) {
                semaphore.release();
            }
        }
    }

    private ProviderContentResult selectContentWithRetry(
            ProviderContentRequest request,
            ProviderContentUrlValidator.ValidatedRequest validatedRequest
    ) throws InterruptedException {
        for (int attempt = 1; attempt <= MAX_TRANSPORT_ATTEMPTS; attempt++) {
            FetchAttempt outcome = selectRedirectChain(
                    validatedRequest.detailUri(),
                    validatedRequest.allowedHost()
            );
            if (outcome.available()) {
                return ProviderContentResult.available(
                        request,
                        outcome.bodyText(),
                        outcome.finalUri(),
                        outcome.httpStatus(),
                        attempt,
                        outcome.redirectCount()
                );
            }
            if (!outcome.retryable() || attempt == MAX_TRANSPORT_ATTEMPTS) {
                return ProviderContentResult.failure(
                        request,
                        outcome.failureCode(),
                        outcome.finalUri(),
                        outcome.httpStatus(),
                        attempt,
                        outcome.redirectCount()
                );
            }
        }
        throw new IllegalStateException("detail body retry result is missing");
    }

    private FetchAttempt selectRedirectChain(URI initialUri, String allowedHost) throws InterruptedException {
        URI currentUri = initialUri;
        int redirectCount = 0;
        while (true) {
            Integer responseStatus = null;
            try {
                urlValidator.validateBeforeRequest(currentUri, allowedHost);
                ProviderContentHttpResponse response = httpTransport.selectResponse(
                        currentUri,
                        readTimeout,
                        maxResponseBytes,
                        userAgent
                );
                int statusCode = response.statusCode();
                responseStatus = statusCode;
                if (statusCode >= 500 && statusCode <= 599) {
                    return FetchAttempt.failure(
                            FailureCode.HTTP_SERVER_ERROR,
                            currentUri,
                            statusCode,
                            true,
                            redirectCount
                    );
                }
                if (selectRedirectStatus(statusCode)) {
                    if (redirectCount >= maxRedirects) {
                        return FetchAttempt.failure(
                                FailureCode.REDIRECT_LIMIT_EXCEEDED,
                                currentUri,
                                statusCode,
                                false,
                                redirectCount
                        );
                    }
                    currentUri = urlValidator.selectRedirectUri(
                            currentUri,
                            response.selectFirstHeader("location"),
                            allowedHost
                    );
                    redirectCount++;
                    continue;
                }
                if (statusCode < 200 || statusCode >= 300) {
                    return FetchAttempt.failure(
                            FailureCode.HTTP_STATUS_ERROR,
                            currentUri,
                            statusCode,
                            false,
                            redirectCount
                    );
                }
                String contentType = response.selectFirstHeader("content-type");
                if (!selectHtmlContentType(contentType)) {
                    return FetchAttempt.failure(
                            FailureCode.CONTENT_TYPE_UNSUPPORTED,
                            currentUri,
                            statusCode,
                            false,
                            redirectCount
                    );
                }
                byte[] body = selectEntityBody(
                        response.body(),
                        response.selectFirstHeader("content-encoding")
                );
                if (body.length > maxResponseBytes) {
                    return FetchAttempt.failure(
                            FailureCode.RESPONSE_TOO_LARGE,
                            currentUri,
                            statusCode,
                            false,
                            redirectCount
                    );
                }
                String bodyText = selectBodyText(body, contentType, currentUri);
                return FetchAttempt.available(bodyText, currentUri, statusCode, redirectCount);
            } catch (ProviderContentValidationException exception) {
                return FetchAttempt.failure(
                        exception.selectFailureCode(),
                        currentUri,
                        null,
                        false,
                        redirectCount
                );
            } catch (ProviderContentResponseTooLargeException exception) {
                return FetchAttempt.failure(
                        FailureCode.RESPONSE_TOO_LARGE,
                        currentUri,
                        null,
                        false,
                        redirectCount
                );
            } catch (HttpTimeoutException | TimeoutException exception) {
                return FetchAttempt.failure(
                        FailureCode.TIMEOUT,
                        currentUri,
                        null,
                        true,
                        redirectCount
                );
            } catch (ContentFailureException exception) {
                return FetchAttempt.failure(
                        exception.failureCode,
                        currentUri,
                        responseStatus,
                        false,
                        redirectCount
                );
            } catch (IOException exception) {
                return FetchAttempt.failure(
                        FailureCode.NETWORK_ERROR,
                        currentUri,
                        null,
                        false,
                        redirectCount
                );
            }
        }
    }

    private boolean selectRedirectStatus(int statusCode) {
        return statusCode == 300
                || statusCode == 301
                || statusCode == 302
                || statusCode == 303
                || statusCode == 307
                || statusCode == 308;
    }

    private boolean selectHtmlContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return false;
        }
        int delimiterIndex = contentType.indexOf(';');
        String mediaType = delimiterIndex < 0 ? contentType : contentType.substring(0, delimiterIndex);
        return "text/html".equals(mediaType.trim().toLowerCase(Locale.ROOT));
    }

    private String selectBodyText(byte[] body, String contentType, URI sourceUri) {
        Charset charset = selectCharset(body, contentType);
        String html = selectDecodedText(body, charset);
        Document document = Jsoup.parse(html, sourceUri.toASCIIString());
        document.select("script, style, noscript, template, iframe, object, embed").remove();
        deleteAttachmentLinkElements(document);
        String bodyText = document.body().text()
                .replace('\u00a0', ' ')
                .replaceAll("\\s+", " ")
                .trim();
        if (bodyText.isEmpty()) {
            throw new ContentFailureException(FailureCode.BODY_TEXT_EMPTY);
        }
        return bodyText;
    }

    private void deleteAttachmentLinkElements(Document document) {
        for (Element link : document.select("a")) {
            if (selectAttachmentLink(link)) {
                Element attachmentContainer = selectAttachmentContainer(link);
                if (attachmentContainer == null) {
                    link.remove();
                } else {
                    attachmentContainer.remove();
                }
            }
        }
    }

    private Element selectAttachmentContainer(Element link) {
        Element candidate = link.parent();
        for (int depth = 0; candidate != null && depth < 4; depth++) {
            if ("body".equals(candidate.tagName())) {
                return null;
            }
            if (selectAttachmentContainerMarker(candidate)
                    && selectOnlyAttachmentLinks(candidate)) {
                return candidate;
            }
            candidate = candidate.parent();
        }
        return null;
    }

    private boolean selectAttachmentContainerMarker(Element element) {
        String marker = String.join(
                " ",
                element.id(),
                element.className(),
                element.attr("data-role"),
                element.attr("data-type")
        ).replaceAll("([a-z0-9])([A-Z])", "$1-$2");
        return ATTACHMENT_CONTAINER_MARKER_PATTERN.matcher(marker).find();
    }

    private boolean selectOnlyAttachmentLinks(Element container) {
        for (Element link : container.select("a")) {
            if (!selectAttachmentLink(link)) {
                return false;
            }
        }
        return !container.select("a").isEmpty();
    }

    private boolean selectAttachmentLink(Element link) {
        if (link.hasAttr("download")) {
            return true;
        }
        String href = link.attr("href").trim();
        String normalizedHref = href.toLowerCase(Locale.ROOT).replace("%2e", ".");
        if (ATTACHMENT_FILE_EXTENSION_PATTERN.matcher(normalizedHref).find()
                || ATTACHMENT_ENDPOINT_PATTERN.matcher(normalizedHref).find()
                || ATTACHMENT_QUERY_PARAMETER_PATTERN.matcher(normalizedHref).find()) {
            return true;
        }
        return (normalizedHref.startsWith("javascript:")
                && ATTACHMENT_HANDLER_PATTERN.matcher(normalizedHref).find())
                || ATTACHMENT_HANDLER_PATTERN.matcher(link.attr("onclick")).find();
    }

    private byte[] selectEntityBody(byte[] body, String contentEncoding) {
        if (contentEncoding == null
                || contentEncoding.isBlank()
                || "identity".equalsIgnoreCase(contentEncoding.trim())) {
            return body;
        }
        if (!"gzip".equalsIgnoreCase(contentEncoding.trim())) {
            throw new ContentFailureException(FailureCode.CONTENT_ENCODING_UNSUPPORTED);
        }
        try (GZIPInputStream input = new GZIPInputStream(new ByteArrayInputStream(body));
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int total = 0;
            int count;
            while ((count = input.read(buffer)) != -1) {
                total += count;
                if (total > maxResponseBytes) {
                    throw new ContentFailureException(FailureCode.RESPONSE_TOO_LARGE);
                }
                output.write(buffer, 0, count);
            }
            return output.toByteArray();
        } catch (ContentFailureException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new ContentFailureException(FailureCode.CONTENT_DECODE_FAILED);
        }
    }

    private Charset selectCharset(byte[] body, String contentType) {
        Matcher headerMatcher = CHARSET_PATTERN.matcher(contentType);
        if (headerMatcher.find()) {
            return selectCharsetByName(headerMatcher.group(1));
        }
        int scanLength = Math.min(body.length, 16 * 1024);
        String metaScan = new String(body, 0, scanLength, StandardCharsets.ISO_8859_1);
        Matcher metaMatcher = META_CHARSET_PATTERN.matcher(metaScan);
        if (metaMatcher.find()) {
            return selectCharsetByName(metaMatcher.group(1));
        }
        return StandardCharsets.UTF_8;
    }

    private Charset selectCharsetByName(String charsetName) {
        try {
            return Charset.forName(charsetName.trim());
        } catch (IllegalArgumentException exception) {
            throw new ContentFailureException(FailureCode.CHARSET_UNSUPPORTED);
        }
    }

    private String selectDecodedText(byte[] body, Charset charset) {
        try {
            CharBuffer decoded = charset.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(body));
            String value = decoded.toString();
            return value.startsWith("\ufeff") ? value.substring(1) : value;
        } catch (CharacterCodingException exception) {
            throw new ContentFailureException(FailureCode.CONTENT_DECODE_FAILED);
        }
    }

    private static String selectUserAgent(String userAgent) {
        return userAgent == null || userAgent.isBlank()
                ? "saneB-notice-collector/1.0"
                : userAgent.trim();
    }

    private record FetchAttempt(
            boolean available,
            String bodyText,
            URI finalUri,
            Integer httpStatus,
            FailureCode failureCode,
            boolean retryable,
            int redirectCount
    ) {

        private static FetchAttempt available(
                String bodyText,
                URI finalUri,
                int httpStatus,
                int redirectCount
        ) {
            return new FetchAttempt(
                    true,
                    bodyText,
                    finalUri,
                    httpStatus,
                    null,
                    false,
                    redirectCount
            );
        }

        private static FetchAttempt failure(
                FailureCode failureCode,
                URI finalUri,
                Integer httpStatus,
                boolean retryable,
                int redirectCount
        ) {
            return new FetchAttempt(
                    false,
                    null,
                    finalUri,
                    httpStatus,
                    failureCode,
                    retryable,
                    redirectCount
            );
        }
    }

    private static final class ContentFailureException extends RuntimeException {

        private final FailureCode failureCode;

        private ContentFailureException(FailureCode failureCode) {
            this.failureCode = failureCode;
        }
    }
}
