/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: LocalGovernmentNoticeCollector.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.announcementsource.localgov.collector;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.common.error.ApiException;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.localgov.support.LocalGovernmentNoticeUrlValidator;
import com.saneb.domain.announcementsource.localgov.vo.LocalGovernmentNoticeParserProfileRow;
import com.saneb.domain.announcementsource.localgov.vo.LocalGovernmentNoticeSourceRow;
import com.saneb.domain.announcementsource.provider.AnnouncementSourceProviderItem;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.SocketTimeoutException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LocalGovernmentNoticeCollector {

    private static final int MAX_REDIRECTS = 5;
    private static final Pattern DATE_PATTERN = Pattern.compile("(20\\d{2})[.\\-/년\\s]+(\\d{1,2})[.\\-/월\\s]+(\\d{1,2})");
    private static final Pattern SHORT_DATE_PATTERN = Pattern.compile(
            "(?<!\\d)(\\d{2})[.\\-/\\s]+(\\d{1,2})[.\\-/\\s]+(\\d{1,2})(?!\\d)"
    );
    private static final Pattern SCRIPT_PATH_PATTERN = Pattern.compile(
            "['\"]((?:https?://|/|\\./|\\.\\./)[^'\"]+)['\"]",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern SAFE_TEMPLATE_PLACEHOLDER_PATTERN = Pattern.compile(
            "\\{(arg:[1-9]|arg:1\\d|arg:20|attr:[A-Za-z0-9_-]{1,80}|query:[A-Za-z0-9_-]{1,80}|input:[A-Za-z0-9_-]{1,80})}"
    );
    private static final Pattern SAFE_UNQUOTED_ARGUMENT_PATTERN = Pattern.compile("[A-Za-z0-9_~-]{1,200}");
    private static final Pattern FORM_FIELD_NAME_PATTERN = Pattern.compile("[A-Za-z][A-Za-z0-9_]{0,79}");
    private static final Pattern NON_NOTICE_TITLE_PATTERN = Pattern.compile(
            "^(홈|로그인|로그아웃|회원가입|검색|목록|이전|다음|처음|끝|더보기|전체보기|바로가기|사이트맵)$"
    );
    private static final Pattern FILE_LINK_PATTERN = Pattern.compile(
            ".*\\.(pdf|hwp|hwpx|doc|docx|xls|xlsx|ppt|pptx|zip|jpg|jpeg|png|gif)(?:[?#].*)?$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern NON_NOTICE_LINK_PATTERN = Pattern.compile(
            ".*(download|filedown|rss|login|logout|sitemap).*",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern DETAIL_LINK_PATTERN = Pattern.compile(
            ".*(view|detail|selectbbsnttview|dataSid=|nttId=|nttNo=|articleId=|articleNo=|boardSeq=|bbsSeq=|jsb_key=|[?&](seq|idx|no|id)=\\d+|/\\d{3,}(?:[/?#].*)?$).*",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern STRUCTURED_CONTAINER_PATTERN = Pattern.compile(
            ".*(item|list|board|notice|bbs|row|card|post|subject|content|cont|box|unit).*",
            Pattern.CASE_INSENSITIVE
    );
    private static final List<String> LINK_ATTRIBUTE_NAMES = List.of(
            "href", "data-url", "data-href", "data-action", "data-link", "data-view-url"
    );
    private static final Map<String, String> DAEJEON_EMINWON_HOSTS = Map.of(
            "seogu", "eminwon.seogu.go.kr",
            "yuseonggu", "eminwon.yuseong.go.kr",
            "djjunggu", "eminwon.djjunggu.go.kr",
            "donggu", "eminwon.donggu.go.kr",
            "daedeokgu", "eminwon.daedeok.go.kr"
    );
    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");
    private static final String BROWSER_COMPATIBLE_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/124.0 Safari/537.36";

    private final LocalGovernmentNoticeUrlValidator urlValidator;
    private final AnnouncementSourceIdentityNormalizer normalizer;
    private final ObjectMapper objectMapper;
    private final HttpClient defaultHttpClient;
    private final HttpClient browserHttp1Client;
    private final Duration timeout;
    private final int maxResponseBytes;
    private final String userAgent;
    private final Semaphore globalSemaphore;
    private final Map<String, Semaphore> domainSemaphores = new ConcurrentHashMap<>();

    /**
     * 안전한 지자체 HTML 수집기를 생성합니다.
     *
     * @param urlValidator SSRF URL 검증기
     * @param normalizer 공고 식별값 정규화기
     * @param objectMapper JSON 직렬화기
     * @param timeoutMillis 요청 제한시간
     * @param maxResponseBytes 최대 응답 크기
     * @param maxConcurrency 전체 동시 수집 수
     * @param userAgent 수집 User-Agent
     */
    public LocalGovernmentNoticeCollector(
            LocalGovernmentNoticeUrlValidator urlValidator,
            AnnouncementSourceIdentityNormalizer normalizer,
            ObjectMapper objectMapper,
            @Value("$" + "{saneb.announcement-source.local-government.timeout-millis:8000}") int timeoutMillis,
            @Value("$" + "{saneb.announcement-source.local-government.max-response-bytes:2097152}") int maxResponseBytes,
            @Value("$" + "{saneb.announcement-source.local-government.max-concurrency:4}") int maxConcurrency,
            @Value("$" + "{saneb.announcement-source.local-government.user-agent:saneB-notice-collector/1.0}") String userAgent
    ) {
        this.urlValidator = urlValidator;
        this.normalizer = normalizer;
        this.objectMapper = objectMapper;
        this.timeout = Duration.ofMillis(Math.max(1000, timeoutMillis));
        this.maxResponseBytes = Math.max(1024, maxResponseBytes);
        this.userAgent = userAgent;
        this.globalSemaphore = new Semaphore(Math.max(1, maxConcurrency));
        this.defaultHttpClient = HttpClient.newBuilder()
                .connectTimeout(this.timeout)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        this.browserHttp1Client = HttpClient.newBuilder()
                .connectTimeout(this.timeout)
                .followRedirects(HttpClient.Redirect.NEVER)
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    /**
     * 단일 지자체 URL에서 제목·등록일·원문 URL을 수집합니다.
     *
     * @param source URL 관리 정보
     * @param profile 파서 프로필
     * @return URL 단위 수집 결과
     */
    public LocalGovernmentNoticeCollectionOutcome collect(
            LocalGovernmentNoticeSourceRow source,
            LocalGovernmentNoticeParserProfileRow profile
    ) {
        URI uri;
        try {
            String requestUrl = source.collectionEndpointUrl() == null || source.collectionEndpointUrl().isBlank()
                    ? source.noticeUrl() : source.collectionEndpointUrl();
            uri = urlValidator.validate(requestUrl);
        } catch (RuntimeException exception) {
            return failure(source.sourceId(), "URL_ERROR", null, "URL_VALIDATION_FAILED", exception.getMessage());
        }
        Semaphore domainSemaphore = domainSemaphores.computeIfAbsent(uri.getHost().toLowerCase(), key -> new Semaphore(1));
        boolean globalAcquired = false;
        boolean domainAcquired = false;
        try {
            globalSemaphore.acquire();
            globalAcquired = true;
            domainSemaphore.acquire();
            domainAcquired = true;
            return collectWithRetry(source, profile, uri);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return failure(source.sourceId(), "FAILED", null, "COLLECTION_INTERRUPTED", "수집 작업이 중단되었습니다.");
        } finally {
            if (domainAcquired) {
                domainSemaphore.release();
            }
            if (globalAcquired) {
                globalSemaphore.release();
            }
        }
    }

    /**
     * timeout과 일시적 5xx에 한해 최대 한 번 재시도합니다.
     *
     * @param source URL 관리 정보
     * @param profile 파서 프로필
     * @param uri 최초 요청 URI
     * @return URL 단위 수집 결과
     */
    private LocalGovernmentNoticeCollectionOutcome collectWithRetry(
            LocalGovernmentNoticeSourceRow source,
            LocalGovernmentNoticeParserProfileRow profile,
            URI uri
    ) {
        LocalGovernmentNoticeCollectionOutcome outcome = requestAndParse(source, profile, uri, 0);
        if ("RETRYABLE".equals(outcome.errorCode())) {
            return requestAndParse(source, profile, uri, 0);
        }
        return outcome;
    }

    /**
     * redirect를 수동 검증하며 HTML을 요청하고 파싱합니다.
     *
     * @param source URL 관리 정보
     * @param profile 파서 프로필
     * @param uri 요청 URI
     * @param redirectCount 현재 redirect 횟수
     * @return URL 단위 수집 결과
     */
    private LocalGovernmentNoticeCollectionOutcome requestAndParse(
            LocalGovernmentNoticeSourceRow source,
            LocalGovernmentNoticeParserProfileRow profile,
            URI uri,
            int redirectCount
    ) {
        try {
            if (usesLegacyBrowser(source)) {
                return requestAndParseLegacy(source, profile, uri, redirectCount);
            }
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(uri)
                    .timeout(timeout)
                    .header("User-Agent", usesBrowserCompatibleRequest(source)
                            ? BROWSER_COMPATIBLE_USER_AGENT : userAgent)
                    .header("Accept", profile != null && "JSON".equals(profile.responseTypeCode())
                            ? "application/json,text/plain;q=0.9,*/*;q=0.5"
                            : "text/html,application/xhtml+xml");
            if (usesBrowserCompatibleRequest(source)) {
                requestBuilder
                        .header("Accept-Language", "ko-KR,ko;q=0.9,en;q=0.5")
                        .header("Cache-Control", "no-cache")
                        .header("Pragma", "no-cache")
                        .header("Upgrade-Insecure-Requests", "1");
                if (source.collectionEndpointUrl() != null && !source.collectionEndpointUrl().isBlank()) {
                    requestBuilder.header("Referer", source.noticeUrl());
                }
            }
            if (source.etag() != null && !source.etag().isBlank()) {
                requestBuilder.header("If-None-Match", source.etag());
            }
            if (source.lastModifiedValue() != null && !source.lastModifiedValue().isBlank()) {
                requestBuilder.header("If-Modified-Since", source.lastModifiedValue());
            }
            HttpResponse<InputStream> response = selectHttpClient(source)
                    .send(selectRequest(source, requestBuilder), HttpResponse.BodyHandlers.ofInputStream());
            return handleResponse(
                    source, profile, uri, redirectCount, response.statusCode(), response.body(),
                    response.headers().firstValue("Content-Type").orElse(""),
                    response.headers().firstValue("ETag").orElse(null),
                    response.headers().firstValue("Last-Modified").orElse(null),
                    response.headers().firstValue("Location").orElse(null)
            );
        } catch (HttpTimeoutException exception) {
            return failure(source.sourceId(), "FAILED", null, "RETRYABLE", "기관 사이트 응답 시간이 초과되었습니다.");
        } catch (SocketTimeoutException exception) {
            return failure(source.sourceId(), "FAILED", null, "RETRYABLE", "기관 사이트 응답 시간이 초과되었습니다.");
        } catch (IOException exception) {
            return failure(source.sourceId(), "FAILED", null, "NETWORK_ERROR", "기관 사이트 연결에 실패했습니다.");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return failure(source.sourceId(), "FAILED", null, "COLLECTION_INTERRUPTED", "수집 작업이 중단되었습니다.");
        } catch (ApiException exception) {
            return failure(source.sourceId(), "URL_ERROR", null, "REDIRECT_URL_BLOCKED", exception.getMessage());
        } catch (RuntimeException exception) {
            return failure(source.sourceId(), "PARSER_UNSUPPORTED", null, "PARSER_ERROR", "공고 목록 구조를 해석하지 못했습니다.");
        }
    }

    /**
     * 구형 공공 웹서버와 호환되는 URLConnection으로 GET 요청을 수행합니다.
     *
     * @param source URL 관리 정보
     * @param profile 파서 프로필
     * @param uri 요청 URI
     * @param redirectCount 현재 redirect 횟수
     * @return URL 단위 수집 결과
     * @throws IOException 네트워크 또는 응답 읽기 오류
     */
    private LocalGovernmentNoticeCollectionOutcome requestAndParseLegacy(
            LocalGovernmentNoticeSourceRow source,
            LocalGovernmentNoticeParserProfileRow profile,
            URI uri,
            int redirectCount
    ) throws IOException {
        if (!"GET".equalsIgnoreCase(source.requestMethodCode())) {
            throw new IOException("구형 브라우저 호환 요청은 GET 방식만 지원합니다.");
        }
        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
        int timeoutMillis = (int) Math.min(Integer.MAX_VALUE, timeout.multipliedBy(2).toMillis());
        connection.setConnectTimeout(timeoutMillis);
        connection.setReadTimeout(timeoutMillis);
        connection.setInstanceFollowRedirects(false);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", BROWSER_COMPATIBLE_USER_AGENT);
        connection.setRequestProperty("Accept", profile != null && "JSON".equals(profile.responseTypeCode())
                ? "application/json,text/plain;q=0.9,*/*;q=0.5"
                : "text/html,application/xhtml+xml");
        connection.setRequestProperty("Accept-Language", "ko-KR,ko;q=0.9,en;q=0.5");
        connection.setRequestProperty("Cache-Control", "no-cache");
        connection.setRequestProperty("Pragma", "no-cache");
        if (source.collectionEndpointUrl() != null && !source.collectionEndpointUrl().isBlank()) {
            connection.setRequestProperty("Referer", source.noticeUrl());
        }
        if (source.etag() != null && !source.etag().isBlank()) {
            connection.setRequestProperty("If-None-Match", source.etag());
        }
        if (source.lastModifiedValue() != null && !source.lastModifiedValue().isBlank()) {
            connection.setRequestProperty("If-Modified-Since", source.lastModifiedValue());
        }
        try {
            int status = connection.getResponseCode();
            InputStream body = status >= 200 && status < 300
                    ? connection.getInputStream() : InputStream.nullInputStream();
            return handleResponse(
                    source, profile, uri, redirectCount, status, body,
                    connection.getHeaderField("Content-Type"),
                    connection.getHeaderField("ETag"),
                    connection.getHeaderField("Last-Modified"),
                    connection.getHeaderField("Location")
            );
        } finally {
            connection.disconnect();
        }
    }

    /**
     * 전송 방식과 무관하게 HTTP 상태, 크기 제한, 파서 실행을 동일하게 처리합니다.
     *
     * @param source URL 관리 정보
     * @param profile 파서 프로필
     * @param uri 최종 요청 URI
     * @param redirectCount 현재 redirect 횟수
     * @param status HTTP 상태
     * @param responseBody 응답 stream
     * @param contentType 응답 Content-Type
     * @param etag 응답 ETag
     * @param lastModifiedValue 응답 Last-Modified
     * @param location redirect Location
     * @return URL 단위 수집 결과
     * @throws IOException 응답 읽기 오류
     */
    private LocalGovernmentNoticeCollectionOutcome handleResponse(
            LocalGovernmentNoticeSourceRow source,
            LocalGovernmentNoticeParserProfileRow profile,
            URI uri,
            int redirectCount,
            int status,
            InputStream responseBody,
            String contentType,
            String etag,
            String lastModifiedValue,
            String location
    ) throws IOException {
        if (status == 304) {
            closeQuietly(responseBody);
            return new LocalGovernmentNoticeCollectionOutcome(
                    source.sourceId(), "NO_CHANGE", 0, 0, status, source.etag(), source.lastModifiedValue(),
                    source.lastContentFingerprint(), null, null, List.of()
            );
        }
        if (status >= 300 && status < 400) {
            closeQuietly(responseBody);
            if (redirectCount >= MAX_REDIRECTS) {
                return failure(source.sourceId(), "URL_ERROR", status, "TOO_MANY_REDIRECTS", "공고 URL의 이동 횟수가 너무 많습니다.");
            }
            if (location == null || location.isBlank()) {
                return failure(source.sourceId(), "URL_ERROR", status, "REDIRECT_LOCATION_MISSING", "이동할 공고 URL이 없습니다.");
            }
            URI redirectUri = urlValidator.validate(uri.resolve(location).toString());
            return requestAndParse(source, profile, redirectUri, redirectCount + 1);
        }
        if (status == 401 || status == 403) {
            closeQuietly(responseBody);
            return failure(source.sourceId(), "ACCESS_BLOCKED", status, "ACCESS_BLOCKED", "기관 사이트가 자동 수집 접근을 허용하지 않습니다.");
        }
        if (status >= 500) {
            closeQuietly(responseBody);
            return failure(source.sourceId(), "FAILED", status, "RETRYABLE", "기관 사이트가 일시적으로 응답하지 않습니다.");
        }
        if (status < 200 || status >= 300) {
            closeQuietly(responseBody);
            return failure(source.sourceId(), "URL_ERROR", status, "HTTP_ERROR", "기관 사이트 응답 상태를 확인하세요.");
        }
        String normalizedContentType = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        boolean jsonProfile = profile != null && "JSON".equals(profile.responseTypeCode());
        boolean supportedContentType = jsonProfile
                ? normalizedContentType.contains("application/json") || normalizedContentType.contains("text/json")
                : normalizedContentType.contains("text/html") || normalizedContentType.contains("application/xhtml+xml");
        if (!supportedContentType) {
            closeQuietly(responseBody);
            return failure(source.sourceId(), "PARSER_UNSUPPORTED", status, "UNSUPPORTED_CONTENT_TYPE",
                    jsonProfile ? "JSON 수집 endpoint 응답 형식을 확인하세요." : "HTML 형식의 공고 목록만 수집할 수 있습니다.");
        }
        byte[] body = readLimited(responseBody);
        String fingerprint = normalizer.hash(Base64.getEncoder().encodeToString(body));
        if (fingerprint.equals(source.lastContentFingerprint())) {
            return new LocalGovernmentNoticeCollectionOutcome(
                    source.sourceId(), "NO_CHANGE", 0, 0, status, etag, lastModifiedValue,
                    fingerprint, null, null, List.of()
            );
        }
        if (jsonProfile) {
            return parseJsonDocument(source, profile, body, status, etag, lastModifiedValue, fingerprint);
        }
        Document document = Jsoup.parse(new ByteArrayInputStream(body), null, uri.toString());
        return parseDocument(source, profile, document, status, etag, lastModifiedValue, fingerprint);
    }

    /**
     * 파서 프로필의 CSS selector로 최소 공고 정보를 추출합니다.
     *
     * @param source URL 관리 정보
     * @param profile 파서 프로필
     * @param document HTML 문서
     * @param httpStatus HTTP 상태
     * @param etag ETag
     * @param lastModifiedValue Last-Modified
     * @param fingerprint 응답 fingerprint
     * @return URL 단위 수집 결과
     */
    private LocalGovernmentNoticeCollectionOutcome parseDocument(
            LocalGovernmentNoticeSourceRow source,
            LocalGovernmentNoticeParserProfileRow profile,
            Document document,
            int httpStatus,
            String etag,
            String lastModifiedValue,
            String fingerprint
    ) {
        if (profile == null || !profile.enabled() || "MANUAL_ONLY".equals(profile.parserTypeCode())) {
            return failure(source.sourceId(), "PARSER_UNSUPPORTED", httpStatus, "PARSER_NOT_CONFIGURED", "수집 파서를 먼저 지정하세요.");
        }
        if ("HEURISTIC_NOTICE".equals(profile.parserTypeCode())) {
            return parseHeuristicDocument(source, document, httpStatus, etag, lastModifiedValue, fingerprint);
        }
        if ("DAEJEON_EMINWON".equals(profile.parserTypeCode())) {
            return parseDaejeonEminwonDocument(
                    source, document, httpStatus, etag, lastModifiedValue, fingerprint
            );
        }
        Elements rows = document.select(profile.listItemSelector());
        if (rows.isEmpty()) {
            return failure(source.sourceId(), "PARSER_UNSUPPORTED", httpStatus, "LIST_SELECTOR_NOT_MATCHED", "공고 목록을 찾지 못했습니다. 파서를 확인하세요.");
        }
        List<AnnouncementSourceProviderItem> items = new ArrayList<>();
        int invalidCount = 0;
        for (Element row : rows) {
            Element titleElement = row.selectFirst(profile.titleSelector());
            Element linkElement = row.selectFirst(profile.linkSelector());
            Element dateElement = row.selectFirst(profile.dateSelector());
            String title = titleElement == null ? null : titleElement.text().trim();
            ResolvedLink resolvedLink = linkElement == null
                    ? null : selectResolvedLink(linkElement, profile, source.noticeUrl());
            String link = resolvedLink == null ? null : resolvedLink.absoluteLink();
            LocalDate postedDate = dateElement == null ? null : parseDate(dateElement.text(), profile.datePattern());
            if (title == null || title.isBlank() || link == null || link.isBlank()
                    || postedDate == null || !isRecentPostedDate(postedDate)) {
                invalidCount++;
                continue;
            }
            String canonicalUrl = normalizer.canonicalizeUrl(link);
            String providerNoticeId = normalizer.hash(canonicalUrl);
            Map<String, Object> rawPayload = new LinkedHashMap<>();
            rawPayload.put("title", title);
            rawPayload.put("postedDate", postedDate.toString());
            rawPayload.put("sourceUrl", canonicalUrl);
            items.add(new AnnouncementSourceProviderItem(
                    "LOCAL_GOV_NOTICE", providerNoticeId, title, source.institutionName(), null, null,
                    postedDate.atStartOfDay(KOREA_ZONE).toOffsetDateTime(), null, canonicalUrl,
                    null, null, null, "MINIMAL", "[\"bodyText\",\"attachment\",\"inquiryText\",\"applicationMethodText\"]",
                    writeJson(rawPayload), normalizer.hash(title + "|" + postedDate + "|" + canonicalUrl), List.of(), source.sourceId()
            ));
        }
        if (items.isEmpty()) {
            return failure(source.sourceId(), "PARSER_UNSUPPORTED", httpStatus, "REQUIRED_FIELDS_MISSING", "제목, 등록일, 원문 URL을 갖춘 공고를 찾지 못했습니다.");
        }
        String status = invalidCount > 0 ? "PARTIAL_FAILED" : "SUCCESS";
        return new LocalGovernmentNoticeCollectionOutcome(
                source.sourceId(), status, rows.size(), invalidCount, httpStatus, etag, lastModifiedValue,
                fingerprint, invalidCount > 0 ? "ITEM_FIELDS_MISSING" : null,
                invalidCount > 0 ? "일부 행에서 제목, 등록일 또는 원문 URL을 찾지 못했습니다." : null,
                List.copyOf(items)
        );
    }

    /**
     * 대전시 통합 목록의 구청별 새올 전자민원 상세 링크를 고정 허용 목록으로 해석합니다.
     *
     * @param source 대전시 통합 공고 수집원
     * @param document 대전시 통합 목록 문서
     * @param httpStatus HTTP 상태
     * @param etag ETag
     * @param lastModifiedValue Last-Modified
     * @param fingerprint 응답 fingerprint
     * @return 대전시 구청 공고 수집 결과
     */
    private LocalGovernmentNoticeCollectionOutcome parseDaejeonEminwonDocument(
            LocalGovernmentNoticeSourceRow source,
            Document document,
            int httpStatus,
            String etag,
            String lastModifiedValue,
            String fingerprint
    ) {
        Elements rows = document.select("table tbody tr:has(td.subject a[onclick*=popupCenterNew])");
        List<AnnouncementSourceProviderItem> items = new ArrayList<>();
        int invalidCount = 0;
        for (Element row : rows) {
            Element titleElement = row.selectFirst("td.subject a[onclick*=popupCenterNew]");
            Element dateElement = row.selectFirst("td.date");
            ResolvedLink resolvedLink = selectDaejeonEminwonLink(titleElement);
            String title = titleElement == null ? null : titleElement.text().trim();
            LocalDate postedDate = dateElement == null ? null : parseDate(dateElement.text(), "yyyy-MM-dd");
            if (title == null || title.isBlank() || postedDate == null || !isRecentPostedDate(postedDate)
                    || resolvedLink == null) {
                invalidCount++;
                continue;
            }
            String canonicalUrl = normalizer.canonicalizeUrl(resolvedLink.absoluteLink());
            Map<String, Object> rawPayload = new LinkedHashMap<>();
            rawPayload.put("title", title);
            rawPayload.put("postedDate", postedDate.toString());
            rawPayload.put("sourceUrl", canonicalUrl);
            items.add(new AnnouncementSourceProviderItem(
                    "LOCAL_GOV_NOTICE", normalizer.hash(canonicalUrl), title, source.institutionName(),
                    null, null, postedDate.atStartOfDay(KOREA_ZONE).toOffsetDateTime(), null, canonicalUrl,
                    null, null, null, "MINIMAL",
                    "[\"bodyText\",\"attachment\",\"inquiryText\",\"applicationMethodText\"]",
                    writeJson(rawPayload), normalizer.hash(title + "|" + postedDate + "|" + canonicalUrl),
                    List.of(), source.sourceId()
            ));
        }
        if (items.isEmpty()) {
            return failure(source.sourceId(), "PARSER_UNSUPPORTED", httpStatus,
                    "DAEJEON_EMINWON_ITEMS_NOT_FOUND", "대전시 구청 공고 상세 링크를 찾지 못했습니다.");
        }
        return new LocalGovernmentNoticeCollectionOutcome(
                source.sourceId(), invalidCount > 0 ? "PARTIAL_FAILED" : "SUCCESS",
                rows.size(), invalidCount, httpStatus, etag, lastModifiedValue, fingerprint,
                invalidCount > 0 ? "ITEM_FIELDS_MISSING" : null,
                invalidCount > 0 ? "일부 대전시 구청 공고의 필수값이 비어 있습니다." : null,
                List.copyOf(items)
        );
    }

    /**
     * 대전시 목록의 함수 인자를 검증하고 고정된 구청 전자민원 호스트의 상세 URL을 생성합니다.
     *
     * @param element popupCenterNew 함수가 있는 제목 링크
     * @return 안전하게 생성된 상세 링크 또는 null
     */
    ResolvedLink selectDaejeonEminwonLink(Element element) {
        if (element == null) {
            return null;
        }
        List<String> arguments = selectFunctionArguments(element, "popupCenterNew");
        if (arguments == null || arguments.size() != 2) {
            return null;
        }
        String host = DAEJEON_EMINWON_HOSTS.get(arguments.get(0));
        String managementNumber = arguments.get(1);
        if (host == null || !managementNumber.matches("[0-9]{1,30}")) {
            return null;
        }
        String absoluteLink = "https://" + host
                + "/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do"
                + "?subCheck=Y&jndinm=OfrNotAncmtEJB&context=NTIS"
                + "&method=selectOfrNotAncmt&methodnm=selectOfrNotAncmtRegst"
                + "&not_ancmt_mgt_no=" + managementNumber;
        return new ResolvedLink(element.attr("onclick").trim(), absoluteLink);
    }

    /**
     * DB에 검증된 JSON 필드 매핑으로 제목·등록일·원문 URL을 추출합니다.
     *
     * @param source URL 관리 정보
     * @param profile JSON 파서 프로필
     * @param body JSON 응답
     * @param httpStatus HTTP 상태
     * @param etag ETag
     * @param lastModifiedValue Last-Modified
     * @param fingerprint 응답 fingerprint
     * @return URL 단위 수집 결과
     */
    LocalGovernmentNoticeCollectionOutcome parseJsonDocument(
            LocalGovernmentNoticeSourceRow source,
            LocalGovernmentNoticeParserProfileRow profile,
            byte[] body,
            int httpStatus,
            String etag,
            String lastModifiedValue,
            String fingerprint
    ) {
        if (profile == null || !profile.enabled() || !"GENERIC_JSON".equals(profile.parserTypeCode())) {
            return failure(source.sourceId(), "PARSER_UNSUPPORTED", httpStatus,
                    "JSON_PARSER_NOT_CONFIGURED", "JSON 수집 파서를 먼저 지정하세요.");
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode itemNodes = selectJsonPath(root, profile.jsonItemsPath());
            if (itemNodes == null || !itemNodes.isArray()) {
                return failure(source.sourceId(), "PARSER_UNSUPPORTED", httpStatus,
                        "JSON_ITEMS_NOT_FOUND", "JSON 응답에서 공고 목록을 찾지 못했습니다.");
            }
            List<AnnouncementSourceProviderItem> items = new ArrayList<>();
            int invalidCount = 0;
            for (JsonNode itemNode : itemNodes) {
                String title = selectJsonText(itemNode, profile.jsonTitleField());
                String dateText = selectJsonText(itemNode, profile.jsonDateField());
                String linkValue = selectJsonText(itemNode, profile.jsonLinkField());
                LocalDate postedDate = parseDate(dateText, profile.datePattern());
                String link = resolveJsonLink(source.noticeUrl(), profile.jsonLinkTemplate(), linkValue);
                if (title == null || title.isBlank() || title.length() > 500
                        || postedDate == null || !isRecentPostedDate(postedDate) || link == null) {
                    invalidCount++;
                    continue;
                }
                String canonicalUrl = normalizer.canonicalizeUrl(link);
                Map<String, Object> rawPayload = new LinkedHashMap<>();
                rawPayload.put("title", title);
                rawPayload.put("postedDate", postedDate.toString());
                rawPayload.put("sourceUrl", canonicalUrl);
                items.add(new AnnouncementSourceProviderItem(
                        "LOCAL_GOV_NOTICE", normalizer.hash(canonicalUrl), title, source.institutionName(), null, null,
                        postedDate.atStartOfDay(KOREA_ZONE).toOffsetDateTime(), null, canonicalUrl,
                        null, null, null, "MINIMAL", "[\"bodyText\",\"attachment\",\"inquiryText\",\"applicationMethodText\"]",
                        writeJson(rawPayload), normalizer.hash(title + "|" + postedDate + "|" + canonicalUrl),
                        List.of(), source.sourceId()
                ));
            }
            if (items.isEmpty()) {
                return failure(source.sourceId(), "PARSER_UNSUPPORTED", httpStatus,
                        "JSON_REQUIRED_FIELDS_MISSING", "JSON 공고에서 제목, 등록일, 원문 URL을 찾지 못했습니다.");
            }
            return new LocalGovernmentNoticeCollectionOutcome(
                    source.sourceId(), invalidCount > 0 ? "PARTIAL_FAILED" : "SUCCESS",
                    itemNodes.size(), invalidCount, httpStatus, etag, lastModifiedValue, fingerprint,
                    invalidCount > 0 ? "JSON_ITEM_FIELDS_MISSING" : null,
                    invalidCount > 0 ? "일부 JSON 공고의 필수값이 비어 있습니다." : null,
                    List.copyOf(items)
            );
        } catch (IOException exception) {
            return failure(source.sourceId(), "PARSER_UNSUPPORTED", httpStatus,
                    "JSON_PARSE_ERROR", "JSON 공고 응답을 해석하지 못했습니다.");
        }
    }

    /**
     * 점으로 구분된 JSON 객체 경로를 순서대로 조회합니다.
     *
     * @param root JSON root
     * @param path 점 구분 경로
     * @return 경로의 JSON node 또는 null
     */
    private JsonNode selectJsonPath(JsonNode root, String path) {
        if (root == null || path == null || path.isBlank()) {
            return null;
        }
        JsonNode current = root;
        for (String name : path.split("\\.")) {
            current = current.path(name);
            if (current.isMissingNode() || current.isNull()) {
                return null;
            }
        }
        return current;
    }

    /**
     * JSON 객체의 단일 필드를 공백 정리한 문자열로 조회합니다.
     *
     * @param node JSON 객체
     * @param fieldName 필드명
     * @return 문자열 또는 null
     */
    private String selectJsonText(JsonNode node, String fieldName) {
        if (node == null || fieldName == null || fieldName.isBlank()) {
            return null;
        }
        JsonNode value = node.path(fieldName);
        return value.isMissingNode() || value.isNull() ? null : value.asText().trim();
    }

    /**
     * 검증된 링크 template의 값 자리에 JSON 식별자를 넣어 동일 기관 URL을 생성합니다.
     *
     * @param noticeUrl 사용자용 공고 목록 URL
     * @param template 링크 template
     * @param value JSON 링크 식별자
     * @return 상세 URL 또는 null
     */
    private String resolveJsonLink(String noticeUrl, String template, String value) {
        if (noticeUrl == null || template == null || !template.contains("{value}")
                || value == null || value.isBlank()) {
            return null;
        }
        try {
            URI baseUri = URI.create(noticeUrl);
            String encodedValue = URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
            URI resolvedUri = baseUri.resolve(template.replace("{value}", encodedValue));
            if (!normalizeHost(baseUri.getHost()).equals(normalizeHost(resolvedUri.getHost()))) {
                return null;
            }
            return resolvedUri.toString();
        } catch (RuntimeException exception) {
            return null;
        }
    }

    /**
     * 상세 링크와 가까운 HTML 영역에서 등록일을 찾아 비표준 게시판의 최소 공고 정보를 추출합니다.
     *
     * @param source URL 관리 정보
     * @param document HTML 문서
     * @param httpStatus HTTP 상태
     * @param etag ETag
     * @param lastModifiedValue Last-Modified
     * @param fingerprint 응답 fingerprint
     * @return URL 단위 수집 결과
     */
    private LocalGovernmentNoticeCollectionOutcome parseHeuristicDocument(
            LocalGovernmentNoticeSourceRow source,
            Document document,
            int httpStatus,
            String etag,
            String lastModifiedValue,
            String fingerprint
    ) {
        List<AnnouncementSourceProviderItem> items = new ArrayList<>();
        Set<String> seenUrls = new HashSet<>();
        String sourceHost = normalizeHost(URI.create(document.location()).getHost());
        for (Element anchor : document.select(
                "a[href], a[onclick], a[data-url], a[data-href], a[data-action], a[data-link], a[data-view-url]"
        )) {
            String title = selectLinkTitle(anchor);
            ResolvedLink resolvedLink = selectResolvedLink(anchor);
            String rawLink = resolvedLink == null ? "" : resolvedLink.rawLink();
            String link = resolvedLink == null ? "" : resolvedLink.absoluteLink();
            if (!isNoticeLinkCandidate(title, rawLink, link, sourceHost) || !seenUrls.add(link)) {
                continue;
            }
            LocalDate postedDate = selectNearbyDate(anchor);
            if (postedDate == null || !isRecentPostedDate(postedDate)) {
                continue;
            }
            String canonicalUrl = normalizer.canonicalizeUrl(link);
            String providerNoticeId = normalizer.hash(canonicalUrl);
            Map<String, Object> rawPayload = new LinkedHashMap<>();
            rawPayload.put("title", title);
            rawPayload.put("postedDate", postedDate.toString());
            rawPayload.put("sourceUrl", canonicalUrl);
            items.add(new AnnouncementSourceProviderItem(
                    "LOCAL_GOV_NOTICE", providerNoticeId, title, source.institutionName(), null, null,
                    postedDate.atStartOfDay(KOREA_ZONE).toOffsetDateTime(), null, canonicalUrl,
                    null, null, null, "MINIMAL", "[\"bodyText\",\"attachment\",\"inquiryText\",\"applicationMethodText\"]",
                    writeJson(rawPayload), normalizer.hash(title + "|" + postedDate + "|" + canonicalUrl), List.of(), source.sourceId()
            ));
        }
        if (items.isEmpty()) {
            return failure(source.sourceId(), "PARSER_UNSUPPORTED", httpStatus, "HEURISTIC_ITEMS_NOT_FOUND", "공고 상세 링크와 등록일을 함께 찾지 못했습니다.");
        }
        return new LocalGovernmentNoticeCollectionOutcome(
                source.sourceId(), "SUCCESS", items.size(), 0, httpStatus, etag, lastModifiedValue,
                fingerprint, null, null, List.copyOf(items)
        );
    }

    /**
     * 공고 상세 링크로 사용할 수 있는 제목과 URL인지 확인합니다.
     *
     * @param title 링크 제목
     * @param rawLink 원본 href
     * @param absoluteLink 절대 URL
     * @param sourceHost 수집원 host
     * @return 공고 링크 후보이면 true
     */
    private boolean isNoticeLinkCandidate(String title, String rawLink, String absoluteLink, String sourceHost) {
        if (title.length() < 5 || title.length() > 200 || NON_NOTICE_TITLE_PATTERN.matcher(title).matches()
                || FILE_LINK_PATTERN.matcher(title).matches() || title.startsWith("RSS ")) {
            return false;
        }
        String lowerRawLink = rawLink.toLowerCase(Locale.ROOT);
        if (rawLink.isBlank() || FILE_LINK_PATTERN.matcher(lowerRawLink).matches()
                || NON_NOTICE_LINK_PATTERN.matcher(lowerRawLink).matches()
                || !DETAIL_LINK_PATTERN.matcher(lowerRawLink).matches()) {
            return false;
        }
        try {
            URI linkUri = URI.create(absoluteLink);
            String scheme = linkUri.getScheme();
            return ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                    && sourceHost.equals(normalizeHost(linkUri.getHost()));
        } catch (RuntimeException exception) {
            return false;
        }
    }

    /**
     * 링크에서 가까운 행·목록·카드 영역을 최대 네 단계까지 확인해 등록일을 찾습니다.
     *
     * @param anchor 상세 링크
     * @return 등록일 또는 null
     */
    private LocalDate selectNearbyDate(Element anchor) {
        Element current = anchor;
        for (int depth = 0; depth < 6 && current != null; depth++) {
            if (!isStructuredContainer(current)) {
                current = current.parent();
                continue;
            }
            String text = current.text();
            if (text.length() > 2000) {
                return null;
            }
            for (Element dateElement : current.select(
                    "time, .date, .regdate, .wdate, [class*=date], [class*=reg-date], [class*=write-date]"
            )) {
                LocalDate date = parseDate(dateElement.text(), null);
                if (date != null) {
                    return date;
                }
            }
            List<LocalDate> dates = selectDateList(text);
            if (text.length() <= 500 && dates.size() == 1) {
                return dates.getFirst();
            }
            current = current.parent();
        }
        return null;
    }

    /**
     * 문자열에 포함된 모든 4자리 연도 날짜를 추출합니다.
     *
     * @param text 목록 행 텍스트
     * @return 날짜 목록
     */
    private List<LocalDate> selectDateList(String text) {
        Matcher matcher = DATE_PATTERN.matcher(text);
        List<LocalDate> dates = new ArrayList<>();
        while (matcher.find()) {
            try {
                dates.add(LocalDate.of(
                        Integer.parseInt(matcher.group(1)),
                        Integer.parseInt(matcher.group(2)),
                        Integer.parseInt(matcher.group(3))
                ));
            } catch (RuntimeException ignored) {
                // 잘못된 날짜 한 건은 무시하고 같은 행의 다른 날짜를 확인합니다.
            }
        }
        Matcher shortMatcher = SHORT_DATE_PATTERN.matcher(text);
        while (shortMatcher.find()) {
            try {
                dates.add(LocalDate.of(
                        2000 + Integer.parseInt(shortMatcher.group(1)),
                        Integer.parseInt(shortMatcher.group(2)),
                        Integer.parseInt(shortMatcher.group(3))
                ));
            } catch (RuntimeException ignored) {
                // 잘못된 날짜 한 건은 무시하고 같은 행의 다른 날짜를 확인합니다.
            }
        }
        return dates;
    }

    /**
     * 날짜를 함께 읽을 수 있는 반복 행·목록·카드 컨테이너인지 확인합니다.
     *
     * @param element 링크 상위 요소
     * @return 구조화 컨테이너이면 true
     */
    private boolean isStructuredContainer(Element element) {
        String tagName = element.tagName();
        return "tr".equals(tagName) || "ul".equals(tagName) || "li".equals(tagName)
                || "article".equals(tagName) || "dl".equals(tagName)
                || STRUCTURED_CONTAINER_PATTERN.matcher(element.className()).matches();
    }

    /**
     * URL 비교를 위해 www 접두사를 제거한 host를 반환합니다.
     *
     * @param host URL host
     * @return 정규화 host
     */
    private String normalizeHost(String host) {
        if (host == null) {
            return "";
        }
        String normalized = host.toLowerCase(Locale.ROOT);
        return normalized.startsWith("www.") ? normalized.substring(4) : normalized;
    }

    /**
     * 현재 운영 대상에서 사용할 수 있는 최근 등록일인지 확인합니다.
     *
     * @param postedDate 공고 등록일
     * @return 오늘 이전 1년 범위이면 true
     */
    private boolean isRecentPostedDate(LocalDate postedDate) {
        LocalDate today = LocalDate.now(KOREA_ZONE);
        return !postedDate.isBefore(today.minusYears(1)) && !postedDate.isAfter(today);
    }

    /**
     * 다양한 공고 등록일 표기를 LocalDate로 변환합니다.
     *
     * @param text 날짜 문자열
     * @param configuredPattern 프로필 날짜 패턴
     * @return 변환된 날짜 또는 null
     */
    LocalDate parseDate(String text, String configuredPattern) {
        if (text == null || text.isBlank()) {
            return null;
        }
        if (configuredPattern != null && !configuredPattern.isBlank()) {
            try {
                return LocalDate.parse(text.trim(), DateTimeFormatter.ofPattern(configuredPattern));
            } catch (DateTimeParseException ignored) {
                // 기관별 부가 문구가 포함되면 아래 숫자 추출 규칙으로 다시 시도합니다.
            }
        }
        Matcher matcher = DATE_PATTERN.matcher(text);
        if (matcher.find()) {
            try {
                return LocalDate.of(
                        Integer.parseInt(matcher.group(1)),
                        Integer.parseInt(matcher.group(2)),
                        Integer.parseInt(matcher.group(3))
                );
            } catch (RuntimeException exception) {
                return null;
            }
        }
        Matcher shortMatcher = SHORT_DATE_PATTERN.matcher(text);
        if (!shortMatcher.find()) {
            return null;
        }
        try {
            return LocalDate.of(
                    2000 + Integer.parseInt(shortMatcher.group(1)),
                    Integer.parseInt(shortMatcher.group(2)),
                    Integer.parseInt(shortMatcher.group(3))
            );
        } catch (RuntimeException exception) {
            return null;
        }
    }

    /**
     * 링크 요소에서 화면에 표시할 공고 제목을 선택합니다.
     *
     * @param element 링크 요소
     * @return 정규화된 제목
     */
    String selectLinkTitle(Element element) {
        Element conciseTitle = element.selectFirst(".title, .tit, .subject, .t1, strong");
        String title = (conciseTitle == null ? element.text() : conciseTitle.text())
                .replaceAll("\\s+", " ").trim();
        if (title.isBlank()) {
            title = element.attr("title").replaceAll("\\s+", " ").trim();
        }
        return title;
    }

    /**
     * href와 안전한 data 속성에서 동일 호스트의 상세 URL 후보를 해석합니다.
     * JavaScript는 실행하지 않고 문자열 안에 명시된 URL만 사용합니다.
     *
     * @param element 링크 요소
     * @return 해석된 링크 또는 null
     */
    ResolvedLink selectResolvedLink(Element element) {
        for (String attributeName : LINK_ATTRIBUTE_NAMES) {
            String rawValue = element.attr(attributeName).trim();
            ResolvedLink resolvedLink = resolveLinkValue(element, rawValue);
            if (resolvedLink != null) {
                return resolvedLink;
            }
        }
        return resolveLinkValue(element, element.attr("onclick").trim());
    }

    /**
     * 파서 프로필의 링크 전략에 따라 직접 링크 또는 안전한 URL 템플릿을 해석합니다.
     *
     * @param element 링크 요소
     * @param profile 파서 프로필
     * @param noticeUrl 사용자에게 보여주는 공고 목록 URL
     * @return 해석된 링크 또는 null
     */
    ResolvedLink selectResolvedLink(
            Element element,
            LocalGovernmentNoticeParserProfileRow profile,
            String noticeUrl
    ) {
        if (profile == null || !"SAFE_TEMPLATE".equals(profile.linkStrategyCode())) {
            return selectResolvedLink(element);
        }
        return resolveTemplateLink(
                element,
                profile.linkFunctionName(),
                profile.linkFunctionArgumentCount(),
                profile.linkUrlTemplate(),
                noticeUrl
        );
    }

    /**
     * 허용된 인자·속성·쿼리·hidden input만 템플릿에 대입해 동일 기관 URL을 생성합니다.
     *
     * @param element 링크 요소
     * @param functionName JavaScript 함수명, 속성 기반 템플릿이면 null
     * @param functionArgumentCount 허용된 함수 인자 수
     * @param template URL 템플릿
     * @param noticeUrl 사용자에게 보여주는 공고 목록 URL
     * @return 해석된 링크 또는 null
     */
    private ResolvedLink resolveTemplateLink(
            Element element,
            String functionName,
            Integer functionArgumentCount,
            String template,
            String noticeUrl
    ) {
        if (template == null || template.isBlank() || template.length() > 1000
                || noticeUrl == null || noticeUrl.isBlank()) {
            return null;
        }
        List<String> arguments = functionName == null || functionName.isBlank()
                ? List.of() : selectFunctionArguments(element, functionName);
        if (functionName != null && !functionName.isBlank() && arguments == null) {
            return null;
        }
        if (functionArgumentCount != null && functionArgumentCount != arguments.size()) {
            return null;
        }
        Matcher matcher = SAFE_TEMPLATE_PLACEHOLDER_PATTERN.matcher(template);
        StringBuffer resolvedTemplate = new StringBuffer();
        int placeholderCount = 0;
        while (matcher.find()) {
            placeholderCount++;
            String value = selectTemplateValue(element, noticeUrl, arguments, matcher.group(1));
            if (value == null || value.length() > 300) {
                return null;
            }
            String encodedValue = URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
            matcher.appendReplacement(resolvedTemplate, Matcher.quoteReplacement(encodedValue));
        }
        matcher.appendTail(resolvedTemplate);
        if (placeholderCount == 0 || resolvedTemplate.indexOf("{") >= 0 || resolvedTemplate.indexOf("}") >= 0) {
            return null;
        }
        try {
            URI baseUri = URI.create(noticeUrl);
            URI resolvedUri = baseUri.resolve(resolvedTemplate.toString());
            String scheme = resolvedUri.getScheme();
            if (!("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                    || !normalizeHost(baseUri.getHost()).equals(normalizeHost(resolvedUri.getHost()))) {
                return null;
            }
            return new ResolvedLink(resolvedTemplate.toString(), resolvedUri.toString());
        } catch (RuntimeException exception) {
            return null;
        }
    }

    /**
     * URL 템플릿 placeholder에 대응하는 안전한 문자열 값을 조회합니다.
     *
     * @param element 링크 요소
     * @param noticeUrl 공고 목록 URL
     * @param arguments 검증된 함수 인자
     * @param placeholder placeholder 본문
     * @return 대입 값 또는 null
     */
    private String selectTemplateValue(
            Element element,
            String noticeUrl,
            List<String> arguments,
            String placeholder
    ) {
        int separatorIndex = placeholder.indexOf(':');
        String type = placeholder.substring(0, separatorIndex);
        String name = placeholder.substring(separatorIndex + 1);
        return switch (type) {
            case "arg" -> selectArgument(arguments, name);
            case "attr" -> selectElementAttribute(element, name);
            case "query" -> selectQueryParameter(noticeUrl, name);
            case "input" -> selectDocumentInput(element, name);
            default -> null;
        };
    }

    /**
     * 1부터 시작하는 번호로 함수 인자를 선택합니다.
     *
     * @param arguments 함수 인자 목록
     * @param indexText 1부터 시작하는 인자 번호
     * @return 인자 또는 null
     */
    private String selectArgument(List<String> arguments, String indexText) {
        try {
            int index = Integer.parseInt(indexText) - 1;
            return index >= 0 && index < arguments.size() ? arguments.get(index) : null;
        } catch (RuntimeException exception) {
            return null;
        }
    }

    /**
     * 링크 요소의 명시된 단일 속성값을 조회합니다.
     *
     * @param element 링크 요소
     * @param attributeName 속성명
     * @return 속성값 또는 null
     */
    private String selectElementAttribute(Element element, String attributeName) {
        String value = element.attr(attributeName).trim();
        return value.isBlank() ? null : value;
    }

    /**
     * 공고 목록 URL의 단일 query parameter를 조회합니다.
     *
     * @param noticeUrl 공고 목록 URL
     * @param parameterName parameter명
     * @return decoded 값 또는 null
     */
    private String selectQueryParameter(String noticeUrl, String parameterName) {
        try {
            String query = URI.create(noticeUrl).getRawQuery();
            if (query == null) {
                return null;
            }
            for (String part : query.split("&")) {
                int separatorIndex = part.indexOf('=');
                String rawName = separatorIndex < 0 ? part : part.substring(0, separatorIndex);
                if (parameterName.equals(URLDecoder.decode(rawName, StandardCharsets.UTF_8))) {
                    String rawValue = separatorIndex < 0 ? "" : part.substring(separatorIndex + 1);
                    return URLDecoder.decode(rawValue, StandardCharsets.UTF_8);
                }
            }
            return null;
        } catch (RuntimeException exception) {
            return null;
        }
    }

    /**
     * 현재 문서에서 지정된 hidden/input 값을 조회합니다.
     *
     * @param element 링크 요소
     * @param inputName input name
     * @return input 값 또는 null
     */
    private String selectDocumentInput(Element element, String inputName) {
        Element input = element.ownerDocument() == null
                ? null : element.ownerDocument().getElementsByAttributeValue("name", inputName).first();
        if (input == null) {
            return null;
        }
        String value = input.attr("value").trim();
        return value.isBlank() ? null : value;
    }

    /**
     * 링크 속성에서 지정된 함수 호출을 찾아 리터럴 인자만 해석합니다.
     *
     * @param element 링크 요소
     * @param functionName 허용된 함수명
     * @return 인자 목록, 호출이 없거나 표현식이 포함되면 null
     */
    private List<String> selectFunctionArguments(Element element, String functionName) {
        if (!functionName.matches("[A-Za-z_$][A-Za-z0-9_$.]{0,119}")) {
            return null;
        }
        Pattern callPattern = Pattern.compile(
                "(?<![A-Za-z0-9_$])" + Pattern.quote(functionName) + "\\s*\\(([^)]{0,2000})\\)",
                Pattern.CASE_INSENSITIVE
        );
        List<String> candidates = new ArrayList<>();
        candidates.add(element.attr("onclick"));
        for (String attributeName : LINK_ATTRIBUTE_NAMES) {
            candidates.add(element.attr(attributeName));
        }
        for (String candidate : candidates) {
            Matcher matcher = callPattern.matcher(candidate);
            if (matcher.find()) {
                return parseSafeFunctionArguments(matcher.group(1));
            }
        }
        return null;
    }

    /**
     * JavaScript 실행 없이 문자열·숫자 리터럴 인자만 분리합니다.
     *
     * @param argumentText 함수 괄호 내부 문자열
     * @return 인자 목록 또는 허용되지 않은 표현식이면 null
     */
    List<String> parseSafeFunctionArguments(String argumentText) {
        List<String> arguments = new ArrayList<>();
        int index = 0;
        while (index < argumentText.length()) {
            while (index < argumentText.length() && Character.isWhitespace(argumentText.charAt(index))) {
                index++;
            }
            if (index >= argumentText.length()) {
                break;
            }
            String value;
            char current = argumentText.charAt(index);
            if (current == '\'' || current == '"') {
                char quote = current;
                StringBuilder builder = new StringBuilder();
                index++;
                boolean closed = false;
                while (index < argumentText.length()) {
                    char character = argumentText.charAt(index++);
                    if (character == '\\' && index < argumentText.length()) {
                        char escaped = argumentText.charAt(index++);
                        if (escaped != quote && escaped != '\\') {
                            return null;
                        }
                        builder.append(escaped);
                    } else if (character == quote) {
                        closed = true;
                        break;
                    } else {
                        builder.append(character);
                    }
                }
                if (!closed) {
                    return null;
                }
                value = builder.toString();
            } else {
                int start = index;
                while (index < argumentText.length() && argumentText.charAt(index) != ',') {
                    index++;
                }
                value = argumentText.substring(start, index).trim();
                if (!SAFE_UNQUOTED_ARGUMENT_PATTERN.matcher(value).matches()) {
                    return null;
                }
            }
            if (value.length() > 300 || arguments.size() >= 20) {
                return null;
            }
            arguments.add(value);
            while (index < argumentText.length() && Character.isWhitespace(argumentText.charAt(index))) {
                index++;
            }
            if (index < argumentText.length()) {
                if (argumentText.charAt(index) != ',') {
                    return null;
                }
                index++;
            }
        }
        return arguments.isEmpty() ? null : List.copyOf(arguments);
    }

    /**
     * 직접 URL 또는 스크립트 문자열 내부의 명시적 URL을 절대 URL로 변환합니다.
     *
     * @param element 기준 링크 요소
     * @param rawValue 원본 속성값
     * @return 해석된 링크 또는 null
     */
    private ResolvedLink resolveLinkValue(Element element, String rawValue) {
        if (rawValue == null || rawValue.isBlank() || "#".equals(rawValue)) {
            return null;
        }
        String candidate = rawValue;
        if (candidate.toLowerCase(Locale.ROOT).startsWith("javascript:") || candidate.contains("(")) {
            Matcher matcher = SCRIPT_PATH_PATTERN.matcher(candidate);
            if (!matcher.find()) {
                return null;
            }
            candidate = matcher.group(1);
        }
        candidate = candidate.replace(" ", "%20");
        try {
            URI baseUri = URI.create(element.baseUri());
            URI resolvedUri = baseUri.resolve(candidate);
            String scheme = resolvedUri.getScheme();
            if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
                return null;
            }
            return new ResolvedLink(candidate, resolvedUri.toString());
        } catch (RuntimeException exception) {
            return null;
        }
    }

    /**
     * 기관별 요청 프로필에 맞는 HTTP 클라이언트를 선택합니다.
     *
     * @param source 지자체 URL 정보
     * @return 요청 클라이언트
     */
    private HttpClient selectHttpClient(LocalGovernmentNoticeSourceRow source) {
        return usesBrowserHttp1(source) ? browserHttp1Client : defaultHttpClient;
    }

    /**
     * 브라우저 호환 HTTP/1.1 요청 정책 적용 여부를 확인합니다.
     *
     * @param source 지자체 URL 정보
     * @return 적용 대상이면 true
     */
    private boolean usesBrowserHttp1(LocalGovernmentNoticeSourceRow source) {
        return "BROWSER_HTTP1".equals(source.requestProfileCode());
    }

    /**
     * 구형 공공 웹서버용 URLConnection 요청 정책 적용 여부를 확인합니다.
     *
     * @param source 지자체 URL 정보
     * @return 적용 대상이면 true
     */
    private boolean usesLegacyBrowser(LocalGovernmentNoticeSourceRow source) {
        return "LEGACY_BROWSER".equals(source.requestProfileCode());
    }

    /**
     * 브라우저에 가까운 요청 헤더가 필요한 프로필인지 확인합니다.
     *
     * @param source 지자체 URL 정보
     * @return 브라우저 호환 헤더 적용 대상이면 true
     */
    private boolean usesBrowserCompatibleRequest(LocalGovernmentNoticeSourceRow source) {
        return usesBrowserHttp1(source) || usesLegacyBrowser(source);
    }

    /**
     * 출처별 공개 목록 요청 방식을 적용합니다.
     *
     * @param source 지자체 공고 출처
     * @param requestBuilder 공통 헤더가 설정된 요청 builder
     * @return 전송 가능한 HTTP 요청
     */
    private HttpRequest selectRequest(
            LocalGovernmentNoticeSourceRow source,
            HttpRequest.Builder requestBuilder
    ) {
        if (!"POST_FORM".equals(source.requestMethodCode())) {
            return requestBuilder.GET().build();
        }
        return requestBuilder
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(selectRequestFormBody(source.requestFormJson())))
                .build();
    }

    /**
     * DB에 저장된 공개 게시판 폼 설정을 안전한 URL 인코딩 본문으로 변환합니다.
     *
     * @param requestFormJson 문자열 값만 가진 JSON 객체
     * @return application/x-www-form-urlencoded 본문
     */
    String selectRequestFormBody(String requestFormJson) {
        if (requestFormJson == null || requestFormJson.isBlank()) {
            throw new IllegalArgumentException("폼 POST 요청 설정이 없습니다.");
        }
        try {
            JsonNode root = objectMapper.readTree(requestFormJson);
            if (!root.isObject() || root.size() == 0 || root.size() > 50) {
                throw new IllegalArgumentException("폼 POST 요청 설정은 1개 이상 50개 이하의 객체여야 합니다.");
            }
            List<String> fields = new ArrayList<>();
            root.fields().forEachRemaining(entry -> {
                String fieldName = entry.getKey();
                JsonNode fieldValue = entry.getValue();
                if (!FORM_FIELD_NAME_PATTERN.matcher(fieldName).matches()
                        || fieldValue == null || !fieldValue.isTextual()
                        || fieldValue.textValue().length() > 2000) {
                    throw new IllegalArgumentException("폼 POST 요청 설정 형식이 올바르지 않습니다.");
                }
                fields.add(URLEncoder.encode(fieldName, StandardCharsets.UTF_8)
                        + "=" + URLEncoder.encode(fieldValue.textValue(), StandardCharsets.UTF_8));
            });
            String body = String.join("&", fields);
            if (body.getBytes(StandardCharsets.UTF_8).length > 64 * 1024) {
                throw new IllegalArgumentException("폼 POST 요청 설정 크기가 제한을 초과했습니다.");
            }
            return body;
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("폼 POST 요청 설정 JSON이 올바르지 않습니다.", exception);
        }
    }

    record ResolvedLink(String rawLink, String absoluteLink) {
    }

    /**
     * 응답 크기 제한을 지키며 InputStream을 읽습니다.
     *
     * @param input 응답 stream
     * @return 제한 이내 응답 bytes
     * @throws IOException stream 또는 크기 제한 오류
     */
    private byte[] readLimited(InputStream input) throws IOException {
        try (InputStream source = input; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int total = 0;
            int count;
            while ((count = source.read(buffer)) != -1) {
                total += count;
                if (total > maxResponseBytes) {
                    throw new IOException("응답 크기 제한을 초과했습니다.");
                }
                output.write(buffer, 0, count);
            }
            return output.toByteArray();
        }
    }

    /**
     * 최소 원문 데이터를 JSON으로 직렬화합니다.
     *
     * @param payload 최소 원문 데이터
     * @return JSON 문자열
     */
    private String writeJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("공고 원문 메타데이터를 저장할 수 없습니다.", exception);
        }
    }

    /**
     * URL 단위 실패 결과를 생성합니다.
     *
     * @param sourceId URL 식별자
     * @param statusCode 수집 상태
     * @param httpStatus HTTP 상태
     * @param errorCode 오류 코드
     * @param errorMessage 한글 오류 메시지
     * @return 실패 결과
     */
    private LocalGovernmentNoticeCollectionOutcome failure(
            java.util.UUID sourceId,
            String statusCode,
            Integer httpStatus,
            String errorCode,
            String errorMessage
    ) {
        return new LocalGovernmentNoticeCollectionOutcome(
                sourceId, statusCode, 0, 1, httpStatus, null, null, null,
                errorCode, errorMessage, List.of()
        );
    }

    /**
     * 사용하지 않는 응답 stream을 안전하게 닫습니다.
     *
     * @param input 응답 stream
     */
    private void closeQuietly(InputStream input) {
        try {
            input.close();
        } catch (IOException ignored) {
            // 연결 반환을 위한 최선 노력이며, 수집 결과를 바꾸지 않습니다.
        }
    }
}
