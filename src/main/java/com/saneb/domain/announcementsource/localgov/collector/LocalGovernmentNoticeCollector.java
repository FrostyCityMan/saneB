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
import java.net.URI;
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
            ".*(item|list|board|notice|bbs|row|card).*",
            Pattern.CASE_INSENSITIVE
    );
    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

    private final LocalGovernmentNoticeUrlValidator urlValidator;
    private final AnnouncementSourceIdentityNormalizer normalizer;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
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
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(this.timeout)
                .followRedirects(HttpClient.Redirect.NEVER)
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
            uri = urlValidator.validate(source.noticeUrl());
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
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(uri)
                    .GET()
                    .timeout(timeout)
                    .header("User-Agent", userAgent)
                    .header("Accept", "text/html,application/xhtml+xml");
            if (source.etag() != null && !source.etag().isBlank()) {
                requestBuilder.header("If-None-Match", source.etag());
            }
            if (source.lastModifiedValue() != null && !source.lastModifiedValue().isBlank()) {
                requestBuilder.header("If-Modified-Since", source.lastModifiedValue());
            }
            HttpResponse<InputStream> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofInputStream());
            int status = response.statusCode();
            if (status == 304) {
                closeQuietly(response.body());
                return new LocalGovernmentNoticeCollectionOutcome(
                        source.sourceId(), "NO_CHANGE", 0, 0, status, source.etag(), source.lastModifiedValue(),
                        source.lastContentFingerprint(), null, null, List.of()
                );
            }
            if (status >= 300 && status < 400) {
                closeQuietly(response.body());
                if (redirectCount >= MAX_REDIRECTS) {
                    return failure(source.sourceId(), "URL_ERROR", status, "TOO_MANY_REDIRECTS", "공고 URL의 이동 횟수가 너무 많습니다.");
                }
                String location = response.headers().firstValue("Location").orElse(null);
                if (location == null) {
                    return failure(source.sourceId(), "URL_ERROR", status, "REDIRECT_LOCATION_MISSING", "이동할 공고 URL이 없습니다.");
                }
                URI redirectUri = urlValidator.validate(uri.resolve(location).toString());
                return requestAndParse(source, profile, redirectUri, redirectCount + 1);
            }
            if (status == 401 || status == 403) {
                closeQuietly(response.body());
                return failure(source.sourceId(), "ACCESS_BLOCKED", status, "ACCESS_BLOCKED", "기관 사이트가 자동 수집 접근을 허용하지 않습니다.");
            }
            if (status >= 500) {
                closeQuietly(response.body());
                return failure(source.sourceId(), "FAILED", status, "RETRYABLE", "기관 사이트가 일시적으로 응답하지 않습니다.");
            }
            if (status < 200 || status >= 300) {
                closeQuietly(response.body());
                return failure(source.sourceId(), "URL_ERROR", status, "HTTP_ERROR", "기관 사이트 응답 상태를 확인하세요.");
            }
            String contentType = response.headers().firstValue("Content-Type").orElse("").toLowerCase();
            if (!contentType.contains("text/html") && !contentType.contains("application/xhtml+xml")) {
                closeQuietly(response.body());
                return failure(source.sourceId(), "PARSER_UNSUPPORTED", status, "UNSUPPORTED_CONTENT_TYPE", "HTML 형식의 공고 목록만 수집할 수 있습니다.");
            }
            byte[] body = readLimited(response.body());
            String fingerprint = normalizer.hash(Base64.getEncoder().encodeToString(body));
            if (fingerprint.equals(source.lastContentFingerprint())) {
                return new LocalGovernmentNoticeCollectionOutcome(
                        source.sourceId(), "NO_CHANGE", 0, 0, status,
                        response.headers().firstValue("ETag").orElse(null),
                        response.headers().firstValue("Last-Modified").orElse(null),
                        fingerprint, null, null, List.of()
                );
            }
            Document document = Jsoup.parse(new ByteArrayInputStream(body), null, uri.toString());
            return parseDocument(source, profile, document, status,
                    response.headers().firstValue("ETag").orElse(null),
                    response.headers().firstValue("Last-Modified").orElse(null), fingerprint);
        } catch (HttpTimeoutException exception) {
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
            String link = linkElement == null ? null : linkElement.absUrl("href");
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
        for (Element anchor : document.select("a[href]")) {
            String title = anchor.text().replaceAll("\\s+", " ").trim();
            String rawLink = anchor.attr("href").trim();
            String link = anchor.absUrl("href");
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
        if (rawLink.isBlank() || "#".equals(rawLink) || lowerRawLink.startsWith("javascript:")
                || FILE_LINK_PATTERN.matcher(lowerRawLink).matches()
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
        for (int depth = 0; depth < 4 && current != null; depth++) {
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
        return "tr".equals(tagName) || "li".equals(tagName) || "article".equals(tagName)
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
    private LocalDate parseDate(String text, String configuredPattern) {
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
        if (!matcher.find()) {
            return null;
        }
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
