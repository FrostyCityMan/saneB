package com.saneb.domain.announcementsource.provider.content;

import com.saneb.domain.announcementsource.provider.content.ProviderContentCodes.FailureCode;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URLDecoder;
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
        this.httpTransport = new PinnedProviderContentHttpTransport(connectTimeout);
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
                ProviderContentRequestTarget requestTarget = urlValidator.selectRequestTarget(
                        currentUri,
                        allowedHost
                );
                ProviderContentHttpResponse response = httpTransport.selectResponse(
                        requestTarget,
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
        // main 내부 또는 body 대체 경로에서도 메뉴의 키워드를 공고 본문 근거로 사용하지 않는다.
        // 일반 링크·문장·기관명은 유지하며, 명시된 탐색 역할만 제거한다.
        document.select("nav, [role=navigation]").remove();
        deleteAttachmentLinkElements(document);
        Element contentElement = selectContentElement(document, sourceUri);
        String bodyText = contentElement.text()
                .replace('\u00a0', ' ')
                .replaceAll("\\s+", " ")
                .trim();
        if (bodyText.isEmpty()) {
            throw new ContentFailureException(FailureCode.BODY_TEXT_EMPTY);
        }
        return bodyText;
    }

    private Element selectContentElement(Document document, URI sourceUri) {
        // 실측된 기관의 정확한 게시판만 좁힌다. 같은 parser의 다른 기관까지 지원한다고 추정하지 않는다.
        String host = sourceUri.getHost().toLowerCase(Locale.ROOT);
        if ("www.seogu.go.kr".equals(host)
                && "/prog/saeolGosi/GOSI/kor/sub04_02_01/view.do".equals(sourceUri.getPath())) {
            return selectSeoguContentElement(document, sourceUri);
        }
        if (("eminwon.bsnamgu.go.kr".equals(host) || "eminwon.dalseong.daegu.kr".equals(host))
                && "/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do".equals(sourceUri.getPath())) {
            return selectSaeolContentElement(document, sourceUri, "eminwon.bsnamgu.go.kr".equals(host));
        }
        if (("eminwon.jung.daegu.kr".equals(host) || "eminwon.haman.go.kr".equals(host) || "eminwon.ihc.go.kr".equals(host))
                && "/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do".equals(sourceUri.getPath())) {
            return selectSaeolPlainCellContentElement(document, sourceUri, "eminwon.jung.daegu.kr".equals(host));
        }
        if ("www.busan.go.kr".equals(host) && "/nbgosi/view".equals(sourceUri.getPath()))
            return selectBusanContentElement(document, sourceUri);
        if ("child.gangbuk.go.kr".equals(host) && "/portal/bbs/B0000245/view.do".equals(sourceUri.getPath()))
            return selectGangbukContentElement(document, sourceUri);
        if ("www.boeun.go.kr".equals(host) && "/www/selectBbsNttView.do".equals(sourceUri.getPath()))
            return selectBoeunContentElement(document, sourceUri);
        if (("www.wonju.go.kr".equals(host) || "www.jecheon.go.kr".equals(host)) && "/www/selectBbsNttView.do".equals(sourceUri.getPath())) {
            boolean jecheon = "www.jecheon.go.kr".equals(host);
            var parameters = selectBodyDetailParameters(sourceUri);
            if (jecheon && "".equals(parameters.get("id"))) parameters.remove("id");
            if (!(jecheon ? "18" : "140").equals(parameters.get("bbsNo")) || !(jecheon ? "5233" : "216").equals(parameters.get("key"))
                    || !parameters.getOrDefault("nttNo", "").matches("[1-9][0-9]{0,14}")
                    || !java.util.Set.of("key", "bbsNo", "nttNo", "searchCtgry", "searchCnd", "searchKrwd", "pageIndex", "pageUnit", "integrDeptCode")
                        .containsAll(parameters.keySet()))
                throw new ContentFailureException(FailureCode.BODY_SELECTOR_CHANGED);
            var tables = document.select(jecheon ? "div.p-wrap.bbs.bbs__view > table.p-table.block"
                    : "div.bbs_wrap > div.p-wrap.bbs.bbs__view > table.p-table");
            if (tables.size() != 1) throw new ContentFailureException(FailureCode.BODY_SELECTOR_CHANGED);
            var table = tables.getFirst();
            var titles = table.select("th").stream().filter(e -> e.closest("table") == table && "제목".equals(e.text().trim())
                    && e.nextElementSibling() != null && "td".equals(e.nextElementSibling().tagName())
                    && !e.nextElementSibling().text().isBlank()).toList();
            var contents = table.select("td[title=내용]").stream().filter(e -> e.closest("table") == table).toList();
            if (titles.size() != 1 || contents.size() != 1) throw new ContentFailureException(FailureCode.BODY_SELECTOR_CHANGED);
            return contents.getFirst();
        }
        String board = switch (host) {
            case "www.taebaek.go.kr" -> "25";
            case "www.hsg.go.kr" -> "65";
            case "www.yw.go.kr" -> "17";
            default -> null;
        };
        String path = sourceUri.getPath();
        boolean detailPath = "/www/selectBbsNttView.do".equals(path)
                || ("www.hsg.go.kr".equals(host) && path.matches("/www/selectBbsNttView\\.do;jsessionid=[A-Za-z0-9.-]{1,128}"));
        if (board != null && detailPath && selectBoardParameter(sourceUri, board)) {
            boolean compact = "www.hsg.go.kr".equals(host);
            var tables = document.select(compact ? "div.p-wrap.bbs.bbs__view > table.p-table.block" : "table.bbs_default.view");
            if (tables.size() != 1) throw new ContentFailureException(FailureCode.BODY_SELECTOR_CHANGED);
            Element table = tables.getFirst();
            long titleCount = compact ? (table.select("span.p-table__subject_text").size() == 1
                    && !table.select("span.p-table__subject_text").text().isBlank() ? 1 : 0)
                    : table.select("th").stream().filter(e -> "제목".equals(e.text().trim()) && e.nextElementSibling() != null
                            && "td".equals(e.nextElementSibling().tagName()) && !e.nextElementSibling().text().isBlank()).count();
            var content = table.select("td[title=내용]");
            if (titleCount != 1 || content.size() != 1) throw new ContentFailureException(FailureCode.BODY_SELECTOR_CHANGED);
            return content.getFirst();
        }
        for (String selector : new String[]{"main", "[role=main]", "article"}) {
            Element candidate = document.selectFirst(selector);
            if (candidate != null) {
                return candidate;
            }
        }
        return document.body();
    }

    private Element selectBoeunContentElement(Document document, URI sourceUri) {
        var parameters = selectBodyDetailParameters(sourceUri);
        if (!"66".equals(parameters.get("bbsNo")) || !"194".equals(parameters.get("key"))
                || !parameters.getOrDefault("nttNo", "").matches("[1-9][0-9]{0,14}")
                || !java.util.Set.of("key", "bbsNo", "nttNo", "searchCtgry", "searchCnd", "searchKrwd", "pageIndex", "pageUnit", "integrDeptCode")
                    .containsAll(parameters.keySet()))
            throw new ContentFailureException(FailureCode.BODY_SELECTOR_CHANGED);
        var tables = document.select("div.p-wrap.bbs.bbs__view > table.p-table.block");
        if (tables.size() != 1) throw new ContentFailureException(FailureCode.BODY_SELECTOR_CHANGED);
        var table = tables.getFirst();
        var titles = table.select("span.p-table__subject_text").stream().filter(e -> e.closest("table") == table).toList();
        var contents = table.select("td[title=내용]").stream().filter(e -> e.closest("table") == table).toList();
        if (titles.size() != 1 || titles.getFirst().text().isBlank() || contents.size() != 1)
            throw new ContentFailureException(FailureCode.BODY_SELECTOR_CHANGED);
        return contents.getFirst();
    }

    private Element selectSeoguContentElement(Document document, URI sourceUri) {
        String query = sourceUri.getRawQuery();
        if (query == null || !query.matches("notAncmtMgtNo=[0-9]{1,15}"))
            throw new ContentFailureException(FailureCode.BODY_SELECTOR_CHANGED);
        var cards = document.select("div.card.program--view");
        if (cards.size() != 1) throw new ContentFailureException(FailureCode.BODY_SELECTOR_CHANGED);
        var card = cards.getFirst();
        var identifiers = card.select("span#notAncmtMgtNo");
        var titles = card.select("span#notAncmtSj");
        var contents = card.select("span#notAncmtCn");
        // 제목·공고번호·본문이 분리된 공식 카드만 사용한다. 담당자·첨부명·주변 메뉴는 포함하지 않는다.
        if (identifiers.size() != 1 || !query.substring("notAncmtMgtNo=".length()).equals(identifiers.getFirst().text().strip())
                || titles.size() != 1 || titles.getFirst().text().isBlank() || contents.size() != 1)
            throw new ContentFailureException(FailureCode.BODY_SELECTOR_CHANGED);
        return contents.getFirst();
    }

    private Element selectSaeolForm(Document document, URI sourceUri) {
        Map<String, String> required = Map.of("context", "NTIS", "homepage_pbs_yn", "Y", "jndinm", "OfrNotAncmtEJB",
                "method", "selectOfrNotAncmt", "methodnm", "selectOfrNotAncmtRegst",
                "subCheck", "eminwon.ihc.go.kr".equals(sourceUri.getHost()) ? "N" : "Y");
        var parameters = selectBodyDetailParameters(sourceUri);
        if (parameters.size() != 7 || !parameters.entrySet().containsAll(required.entrySet())
                || !parameters.getOrDefault("not_ancmt_mgt_no", "").matches("[0-9]{1,15}"))
            throw new ContentFailureException(FailureCode.BODY_SELECTOR_CHANGED);
        var forms = document.select("form[name=form1][method=post]");
        if (forms.size() != 1) throw new ContentFailureException(FailureCode.BODY_SELECTOR_CHANGED);
        return forms.getFirst();
    }

    private Map<String, String> selectBodyDetailParameters(URI sourceUri) {
        var parameters = new java.util.HashMap<String, String>();
        String query = sourceUri.getRawQuery();
        if (query == null || query.length() > 4096) throw new ContentFailureException(FailureCode.BODY_SELECTOR_CHANGED);
        try {
            for (String pair : query.split("&", -1)) {
                String[] parts = pair.split("=", -1);
                if (parts.length != 2 || !parts[0].matches("[A-Za-z_]+")
                        || parameters.putIfAbsent(parts[0], URLDecoder.decode(parts[1], StandardCharsets.UTF_8)) != null)
                    throw new ContentFailureException(FailureCode.BODY_SELECTOR_CHANGED);
            }
        } catch (IllegalArgumentException exception) { throw new ContentFailureException(FailureCode.BODY_SELECTOR_CHANGED); }
        return parameters;
    }

    private Element selectBusanContentElement(Document document, URI sourceUri) {
        var parameters = selectBodyDetailParameters(sourceUri);
        if (!parameters.keySet().containsAll(java.util.Set.of("sno", "gosiGbn"))
                || !java.util.Set.of("sno", "gosiGbn", "curPage").containsAll(parameters.keySet())
                || !parameters.get("sno").matches("[0-9]{1,15}") || !"A".equals(parameters.get("gosiGbn"))
                || (parameters.containsKey("curPage") && !parameters.get("curPage").matches("[1-9][0-9]{0,6}")))
            throw new ContentFailureException(FailureCode.BODY_SELECTOR_CHANGED);
        var views = document.select("div.boardView");
        if (views.size() != 1) throw new ContentFailureException(FailureCode.BODY_SELECTOR_CHANGED);
        Element view = views.getFirst();
        var titles = view.select("div.form-group > h4.form-data-subject");
        var contents = view.select("div.form-group > dl.form-data-content");
        if (titles.size() != 1 || titles.getFirst().text().isBlank() || contents.size() != 1)
            throw new ContentFailureException(FailureCode.BODY_SELECTOR_CHANGED);
        Element content = contents.getFirst();
        if (content.childrenSize() != 2 || !"dt".equals(content.child(0).tagName())
                || !"내용".equals(content.child(0).text().strip()) || !"dd".equals(content.child(1).tagName()))
            throw new ContentFailureException(FailureCode.BODY_SELECTOR_CHANGED);
        return content.child(1);
    }

    private Element selectGangbukContentElement(Document document, URI sourceUri) {
        var parameters = selectBodyDetailParameters(sourceUri);
        if (!parameters.keySet().equals(java.util.Set.of("menuNo", "nttId")) || !"200082".equals(parameters.get("menuNo"))
                || !parameters.getOrDefault("nttId", "").matches("[0-9]{1,15}"))
            throw new ContentFailureException(FailureCode.BODY_SELECTOR_CHANGED);
        var forms = document.select("form#board");
        if (forms.size() != 1) throw new ContentFailureException(FailureCode.BODY_SELECTOR_CHANGED);
        Element form = forms.getFirst();
        var ids = form.children().stream().filter(e -> "input".equals(e.tagName()) && "nttId".equals(e.attr("name"))).toList();
        var views = form.children().stream().filter(e -> "div".equals(e.tagName()) && e.hasClass("bd-view")).toList();
        if (ids.size() != 1 || !"hidden".equalsIgnoreCase(ids.getFirst().attr("type"))
                || !parameters.get("nttId").equals(ids.getFirst().val()) || views.size() != 1)
            throw new ContentFailureException(FailureCode.BODY_SELECTOR_CHANGED);
        Element view = views.getFirst();
        var titles = view.children().stream().filter(e -> "h3".equals(e.tagName()) && e.hasClass("bd-view__subject")).toList();
        var contents = view.children().stream().filter(e -> "dl".equals(e.tagName())).toList();
        if (titles.size() != 1 || titles.getFirst().text().isBlank() || contents.size() != 1
                || contents.getFirst().childrenSize() != 1 || !"dd".equals(contents.getFirst().child(0).tagName()))
            throw new ContentFailureException(FailureCode.BODY_SELECTOR_CHANGED);
        // 메타데이터 table-dl·첨부 목록·공공누리 opentype의 dd와 본문을 구분한다.
        return contents.getFirst().child(0);
    }

    private Element selectSaeolContentElement(Document document, URI sourceUri, boolean namgu) {
        var tables = selectSaeolForm(document, sourceUri).select(namgu ? "table.table_03" : "table.bbsView");
        if (tables.size() != 1) throw new ContentFailureException(FailureCode.BODY_SELECTOR_CHANGED);
        Element table = tables.getFirst();
        var titles = namgu ? table.select("th[colspan=4]").stream().toList()
                : table.select("th").stream().filter(e -> "제목".equals(e.text().strip())).toList();
        if (titles.size() != 1) throw new ContentFailureException(FailureCode.BODY_SELECTOR_CHANGED);
        Element title = namgu ? titles.getFirst() : titles.getFirst().nextElementSibling();
        var contents = table.select(namgu ? "td[colspan=4] > div.view01_con" : "td[colspan=4].con.l");
        if (title == null || (!namgu && !"td".equals(title.tagName())) || title.text().isBlank() || contents.size() != 1)
            throw new ContentFailureException(FailureCode.BODY_SELECTOR_CHANGED);
        return contents.getFirst();
    }

    private Element selectSaeolPlainCellContentElement(Document document, URI sourceUri, boolean junggu) {
        var tables = selectSaeolForm(document, sourceUri).select(junggu ? "table.boardView"
                : "table[width=100%][border=0][cellspacing=1][cellpadding=0]");
        if (tables.size() != 1) throw new ContentFailureException(FailureCode.BODY_SELECTOR_CHANGED);
        Element table = tables.getFirst();
        var titles = table.select(junggu || "eminwon.ihc.go.kr".equals(sourceUri.getHost()) ? "th" : "td").stream()
                .filter(e -> e.closest("table") == table && "제목".equals(e.text().strip())).toList();
        if (titles.size() != 1) throw new ContentFailureException(FailureCode.BODY_SELECTOR_CHANGED);
        Element title = titles.getFirst().nextElementSibling();
        // 실측 본문 셀의 고유 style을 확인한다. 장식/첨부/중첩 표의 셀은 선택하지 않는다.
        var contents = table.select("td[colspan=4][style]").stream().filter(e -> e.closest("table") == table
                && e.attr("style").matches("(?i)\\s*word-break\\s*:\\s*break-all\\s*;?\\s*")).toList();
        if (title == null || !"td".equals(title.tagName()) || title.text().isBlank() || contents.size() != 1)
            throw new ContentFailureException(FailureCode.BODY_SELECTOR_CHANGED);
        return contents.getFirst();
    }

    private boolean selectBoardParameter(URI uri, String board) {
        String found = null;
        if (uri.getRawQuery() == null) throw new ContentFailureException(FailureCode.BODY_SELECTOR_CHANGED);
        try {
            for (String pair : uri.getRawQuery().split("&")) {
                String[] part = pair.split("=", 2);
                if (!"bbsNo".equals(URLDecoder.decode(part[0], StandardCharsets.UTF_8))) continue;
                if (found != null || part.length != 2) throw new ContentFailureException(FailureCode.BODY_SELECTOR_CHANGED);
                found = URLDecoder.decode(part[1], StandardCharsets.UTF_8);
            }
        } catch (IllegalArgumentException exception) { throw new ContentFailureException(FailureCode.BODY_SELECTOR_CHANGED); }
        if (found == null || !found.matches("[1-9][0-9]{0,8}")) throw new ContentFailureException(FailureCode.BODY_SELECTOR_CHANGED);
        return board.equals(found);
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
