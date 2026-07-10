/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: LocalGovernmentNoticeParserFullQaTest.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.announcementsource.localgov.collector;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named = "SANEB_LOCAL_GOV_PARSER_QA", matches = "true")
class LocalGovernmentNoticeParserFullQaTest {

    private static final int EXPECTED_SOURCE_COUNT = 244;
    private static final int MAX_RESPONSE_BYTES = 3 * 1024 * 1024;
    private static final Pattern SOURCE_INSERT_PATTERN = Pattern.compile(
            "INSERT INTO local_government_notice_sources\\s*\\(.*?\\) VALUES \\((.*?)\\) ON CONFLICT DO NOTHING;",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    private static final Pattern SQL_STRING_PATTERN = Pattern.compile("'((?:''|[^'])*)'");
    private static final Pattern DATE_PATTERN = Pattern.compile(
            "(20\\d{2})[.\\-/년\\s]+(\\d{1,2})[.\\-/월\\s]+(\\d{1,2})"
    );
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
    private static final List<ParserProfile> PROFILES = List.of(
            new ParserProfile("SAEOL_GOSI", "table tbody tr", "td a", "td:last-child", "td a", "yyyy-MM-dd"),
            new ParserProfile("SPRING_BBS", "table tbody tr", "td a", "td:nth-last-child(2)", "td a", "yyyy-MM-dd"),
            new ParserProfile("JSP_BBS", "table tbody tr", "td a", "td:nth-last-child(2)", "td a", "yyyy.MM.dd"),
            new ParserProfile("TC_GOSI", "table tbody tr", "td a", "td:nth-last-child(2)", "td a", "yyyy-MM-dd"),
            new ParserProfile("GENERIC_TABLE", "table tbody tr", "td a", "td:last-child", "td a", null),
            new ParserProfile("GENERIC_LIST", "ul li, ol li", "a", "time, .date", "a", null),
            new ParserProfile("HEURISTIC_NOTICE", null, null, null, null, null)
    );

    private final HttpClient httpClient = selectHttpClient();
    private final Map<String, Semaphore> domainSemaphores = new ConcurrentHashMap<>();

    /**
     * 정적 seed의 모든 지자체 URL을 한 번씩 요청하고 파서 후보별 추출 결과를 보고서로 저장합니다.
     *
     * @throws Exception URL 검사 또는 보고서 저장 실패
     */
    @Test
    void inspectAllSeededLocalGovernmentNoticeSources() throws Exception {
        List<SourceSeed> sources = selectSourceSeedList();
        assertThat(sources).hasSize(EXPECTED_SOURCE_COUNT);

        List<QaResult> results = inspectAll(sources);
        assertThat(results).hasSize(EXPECTED_SOURCE_COUNT);
        assertThat(results).extracting(QaResult::publicCode).doesNotHaveDuplicates();

        Path outputDirectory = Path.of(System.getProperty(
                "saneb.local-gov-qa.output-dir",
                "build/reports/local-government-parser-qa"
        ));
        Files.createDirectories(outputDirectory);
        writeCsv(outputDirectory.resolve("지자체_파서_전수_QA.csv"), results);
        writeSummary(outputDirectory.resolve("지자체_파서_전수_QA_요약.md"), results);
    }

    /**
     * 지자체 URL을 제한된 병렬도로 검사합니다.
     *
     * @param sources 지자체 URL seed 목록
     * @return 관리코드 순서로 정렬된 QA 결과
     * @throws Exception 병렬 실행 실패
     */
    private List<QaResult> inspectAll(List<SourceSeed> sources) throws Exception {
        try (ExecutorService executor = Executors.newFixedThreadPool(12)) {
            List<Future<QaResult>> futures = sources.stream()
                    .map(source -> executor.submit(() -> inspect(source)))
                    .toList();
            List<QaResult> results = new ArrayList<>();
            for (Future<QaResult> future : futures) {
                results.add(future.get());
            }
            return results.stream()
                    .sorted(Comparator.comparing(QaResult::publicCode))
                    .toList();
        }
    }

    /**
     * 단일 URL을 요청하고 모든 실행 가능한 정적 파서 결과를 비교합니다.
     *
     * @param source 지자체 URL seed
     * @return 최적 파서와 추출 품질
     */
    private QaResult inspect(SourceSeed source) {
        long startedAt = System.nanoTime();
        URI uri;
        try {
            uri = URI.create(source.noticeUrl());
        } catch (RuntimeException exception) {
            return failure(source, "URL_ERROR", null, exception.getMessage(), startedAt);
        }
        Semaphore domainSemaphore = domainSemaphores.computeIfAbsent(
                String.valueOf(uri.getHost()).toLowerCase(Locale.ROOT),
                ignored -> new Semaphore(1)
        );
        try {
            domainSemaphore.acquire();
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(15))
                    .header("User-Agent", "saneB-notice-collector/1.0")
                    .header("Accept", "text/html,application/xhtml+xml")
                    .header("Accept-Language", "ko-KR,ko;q=0.9")
                    .GET()
                    .build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return failure(source, "HTTP_ERROR", response.statusCode(), "HTTP " + response.statusCode(), startedAt);
            }
            if (response.body().length > MAX_RESPONSE_BYTES) {
                return failure(source, "RESPONSE_TOO_LARGE", response.statusCode(), "응답 크기 제한 초과", startedAt);
            }
            Document document = Jsoup.parse(
                    new ByteArrayInputStream(response.body()),
                    null,
                    response.uri().toString()
            );
            List<ProfileResult> profileResults = PROFILES.stream()
                    .map(profile -> inspectProfile(document, profile))
                    .toList();
            ProfileResult best = selectBestProfile(profileResults);
            String status = selectStatus(best);
            return new QaResult(
                    source.publicCode(), source.sidoName(), source.sigunguName(), source.institutionName(),
                    source.noticeUrl(), status, best.profileCode(), best.discoveredCount(), best.validCount(),
                    best.invalidCount(), response.statusCode(), null, null, String.join(" | ", best.samples()),
                    elapsedMillis(startedAt)
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return failure(source, "INTERRUPTED", null, "검사가 중단되었습니다.", startedAt);
        } catch (IOException exception) {
            return failure(source, "NETWORK_ERROR", null, exception.getMessage(), startedAt);
        } catch (RuntimeException exception) {
            return failure(source, "PARSER_ERROR", null, exception.getMessage(), startedAt);
        } finally {
            if (domainSemaphore.availablePermits() == 0) {
                domainSemaphore.release();
            }
        }
    }

    /**
     * 한 HTML 문서에 단일 정적 파서 프로필을 적용합니다.
     *
     * @param document HTML 문서
     * @param profile 파서 프로필
     * @return 발견·유효·무효 건수와 표본
     */
    private ProfileResult inspectProfile(Document document, ParserProfile profile) {
        if ("HEURISTIC_NOTICE".equals(profile.profileCode())) {
            return inspectHeuristicProfile(document, profile);
        }
        Elements rows = document.select(profile.listItemSelector());
        int validCount = 0;
        int invalidCount = 0;
        List<String> samples = new ArrayList<>();
        for (Element row : rows) {
            Element titleElement = row.selectFirst(profile.titleSelector());
            Element dateElement = row.selectFirst(profile.dateSelector());
            Element linkElement = row.selectFirst(profile.linkSelector());
            String title = titleElement == null ? null : titleElement.text().trim();
            String rawLink = linkElement == null ? null : linkElement.attr("href").trim();
            String link = linkElement == null ? null : linkElement.absUrl("href");
            LocalDate date = dateElement == null ? null : parseDate(dateElement.text(), profile.datePattern());
            if (title == null || title.isBlank() || title.length() > 500
                    || rawLink == null || rawLink.isBlank() || "#".equals(rawLink)
                    || rawLink.toLowerCase(Locale.ROOT).startsWith("javascript:")
                    || link == null || !(link.startsWith("http://") || link.startsWith("https://"))
                    || date == null || !isRecentPostedDate(date)) {
                invalidCount++;
                continue;
            }
            validCount++;
            if (samples.size() < 3) {
                samples.add(title.replaceAll("\\s+", " ").trim() + " / " + date + " / " + link);
            }
        }
        return new ProfileResult(profile.profileCode(), rows.size(), validCount, invalidCount, List.copyOf(samples));
    }

    /**
     * 상세 링크 주변에서 등록일을 찾는 제한형 휴리스틱 파서를 검사합니다.
     *
     * @param document HTML 문서
     * @param profile 휴리스틱 프로필
     * @return 유효 공고 링크와 표본
     */
    private ProfileResult inspectHeuristicProfile(Document document, ParserProfile profile) {
        int validCount = 0;
        Set<String> seenLinks = new HashSet<>();
        List<String> samples = new ArrayList<>();
        String sourceHost = normalizeHost(URI.create(document.location()).getHost());
        for (Element anchor : document.select("a[href]")) {
            String title = anchor.text().replaceAll("\\s+", " ").trim();
            String rawLink = anchor.attr("href").trim();
            String link = anchor.absUrl("href");
            if (!isNoticeLinkCandidate(title, rawLink, link, sourceHost) || !seenLinks.add(link)) {
                continue;
            }
            LocalDate date = selectNearbyDate(anchor);
            if (date == null || !isRecentPostedDate(date)) {
                continue;
            }
            validCount++;
            if (samples.size() < 3) {
                samples.add(title + " / " + date + " / " + link);
            }
        }
        return new ProfileResult(profile.profileCode(), validCount, validCount, 0, List.copyOf(samples));
    }

    /**
     * 휴리스틱 파서가 사용할 수 있는 동일 기관 도메인의 상세 링크인지 확인합니다.
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
     * 링크의 상위 HTML 영역을 최대 네 단계까지 확인해 등록일을 찾습니다.
     *
     * @param anchor 공고 상세 링크
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
        LocalDate today = LocalDate.now();
        return !postedDate.isBefore(today.minusYears(1)) && !postedDate.isAfter(today);
    }

    /**
     * 운영 수집기와 동일한 규칙으로 날짜를 변환합니다.
     *
     * @param text 날짜 원문
     * @param configuredPattern 파서 날짜 패턴
     * @return 날짜 또는 null
     */
    private LocalDate parseDate(String text, String configuredPattern) {
        if (text == null || text.isBlank()) {
            return null;
        }
        if (configuredPattern != null) {
            try {
                return LocalDate.parse(text.trim(), DateTimeFormatter.ofPattern(configuredPattern));
            } catch (DateTimeParseException ignored) {
                // 운영 수집기와 동일하게 숫자 추출 규칙을 한 번 더 적용합니다.
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
     * 최적 파서의 유효 추출률로 QA 상태를 결정합니다.
     *
     * @param best 최적 파서 결과
     * @return PASS, PARTIAL 또는 PARSER_UNSUPPORTED
     */
    private String selectStatus(ProfileResult best) {
        if (best.validCount() == 0) {
            return "PARSER_UNSUPPORTED";
        }
        double validRatio = best.discoveredCount() == 0
                ? 0.0 : (double) best.validCount() / best.discoveredCount();
        return validRatio >= 0.8 ? "PASS" : "PARTIAL";
    }

    /**
     * 정적 파서 통과 결과를 우선하고, 정적 파서가 미통과할 때만 휴리스틱 결과를 사용합니다.
     *
     * @param results 파서별 결과
     * @return 최종 추천 파서 결과
     */
    private ProfileResult selectBestProfile(List<ProfileResult> results) {
        Comparator<ProfileResult> comparator = Comparator.comparingInt(ProfileResult::validCount)
                .thenComparing(Comparator.comparingInt(ProfileResult::invalidCount).reversed());
        return results.stream()
                .filter(result -> !"HEURISTIC_NOTICE".equals(result.profileCode()))
                .filter(result -> "PASS".equals(selectStatus(result)))
                .max(comparator)
                .orElseGet(() -> results.stream().max(comparator).orElseThrow());
    }

    /**
     * V29 migration에서 지자체 URL seed를 읽습니다.
     *
     * @return 지자체 URL seed 목록
     * @throws IOException migration 읽기 실패
     */
    private List<SourceSeed> selectSourceSeedList() throws IOException {
        String migration = Files.readString(
                Path.of("src/main/resources/db/migration/V29__seed_local_government_notice_sources.sql"),
                StandardCharsets.UTF_8
        );
        Matcher blockMatcher = SOURCE_INSERT_PATTERN.matcher(migration);
        List<SourceSeed> sources = new ArrayList<>();
        int sequence = 1;
        while (blockMatcher.find()) {
            List<String> values = selectSqlStrings(blockMatcher.group(1));
            if (values.size() < 14) {
                throw new IllegalStateException("지자체 URL seed 형식을 해석할 수 없습니다: " + sequence);
            }
            sources.add(new SourceSeed(
                    "LGS-" + String.format(Locale.ROOT, "%06d", sequence++),
                    values.get(2), values.get(4), values.get(6), values.get(8)
            ));
        }
        return List.copyOf(sources);
    }

    /**
     * SQL 블록의 작은따옴표 문자열을 순서대로 추출합니다.
     *
     * @param valuesBlock VALUES 내부 문자열
     * @return SQL 문자열 값 목록
     */
    private List<String> selectSqlStrings(String valuesBlock) {
        Matcher matcher = SQL_STRING_PATTERN.matcher(valuesBlock);
        List<String> values = new ArrayList<>();
        while (matcher.find()) {
            values.add(matcher.group(1).replace("''", "'"));
        }
        return values;
    }

    /**
     * 전체 QA 결과를 UTF-8 CSV로 저장합니다.
     *
     * @param path 출력 경로
     * @param results QA 결과
     * @throws IOException 저장 실패
     */
    private void writeCsv(Path path, List<QaResult> results) throws IOException {
        StringBuilder csv = new StringBuilder("\uFEFF관리코드,시도,시군구,기관명,URL,QA상태,추천파서,발견,유효,무효,HTTP,오류코드,오류메시지,표본,응답시간ms\n");
        for (QaResult result : results) {
            csv.append(Arrays.asList(
                            result.publicCode(), result.sidoName(), result.sigunguName(), result.institutionName(),
                            result.noticeUrl(), result.statusCode(), result.profileCode(), result.discoveredCount(),
                            result.validCount(), result.invalidCount(), result.httpStatus(), result.errorCode(),
                            result.errorMessage(), result.samples(), result.elapsedMillis()
                    ).stream().map(this::csvValue).collect(Collectors.joining(",")))
                    .append('\n');
        }
        Files.writeString(path, csv.toString(), StandardCharsets.UTF_8);
    }

    /**
     * QA 상태와 추천 파서 집계를 Markdown으로 저장합니다.
     *
     * @param path 출력 경로
     * @param results QA 결과
     * @throws IOException 저장 실패
     */
    private void writeSummary(Path path, List<QaResult> results) throws IOException {
        Map<String, Long> statusCounts = results.stream().collect(Collectors.groupingBy(
                QaResult::statusCode,
                LinkedHashMap::new,
                Collectors.counting()
        ));
        Map<String, Long> profileCounts = results.stream()
                .filter(result -> result.profileCode() != null && !result.profileCode().isBlank())
                .collect(Collectors.groupingBy(QaResult::profileCode, LinkedHashMap::new, Collectors.counting()));
        StringBuilder markdown = new StringBuilder();
        markdown.append("# 지자체 파서 전수 QA 요약\n\n")
                .append("- 검사 대상: ").append(results.size()).append("곳\n")
                .append("- 운영 DB 수집 여부: 없음\n")
                .append("- 필수 추출값: 제목, 등록일, 원문 URL\n\n")
                .append("## 상태 집계\n\n| 상태 | 건수 |\n|---|---:|\n");
        statusCounts.forEach((status, count) -> markdown.append('|').append(status).append('|').append(count).append("|\n"));
        markdown.append("\n## 추천 파서 집계\n\n| 파서 | 건수 |\n|---|---:|\n");
        profileCounts.forEach((profile, count) -> markdown.append('|').append(profile).append('|').append(count).append("|\n"));
        markdown.append("\n## 미통과 목록\n\n| 관리코드 | 기관 | 상태 | 오류 |\n|---|---|---|---|\n");
        results.stream().filter(result -> !"PASS".equals(result.statusCode())).forEach(result -> markdown
                .append('|').append(result.publicCode())
                .append('|').append(escapeMarkdown(result.institutionName()))
                .append('|').append(result.statusCode())
                .append('|').append(escapeMarkdown(result.errorCode() == null ? "추출률 미달" : result.errorCode()))
                .append("|\n"));
        Files.writeString(path, markdown.toString(), StandardCharsets.UTF_8);
    }

    /**
     * CSV 셀 값을 안전하게 이스케이프합니다.
     *
     * @param value 셀 값
     * @return CSV 문자열
     */
    private String csvValue(Object value) {
        String text = value == null ? "" : String.valueOf(value);
        return '"' + text.replace("\"", "\"\"").replace("\r", " ").replace("\n", " ") + '"';
    }

    /**
     * Markdown 표 구분자를 이스케이프합니다.
     *
     * @param value 표 값
     * @return 이스케이프 문자열
     */
    private String escapeMarkdown(String value) {
        return value == null ? "" : value.replace("|", "\\|").replace("\r", " ").replace("\n", " ");
    }

    /**
     * URL 요청 실패 결과를 생성합니다.
     *
     * @param source 지자체 URL seed
     * @param errorCode 오류 코드
     * @param httpStatus HTTP 상태
     * @param errorMessage 오류 메시지
     * @param startedAt 시작 시각
     * @return 실패 QA 결과
     */
    private QaResult failure(
            SourceSeed source,
            String errorCode,
            Integer httpStatus,
            String errorMessage,
            long startedAt
    ) {
        return new QaResult(
                source.publicCode(), source.sidoName(), source.sigunguName(), source.institutionName(),
                source.noticeUrl(), "FAILED", null, 0, 0, 0, httpStatus, errorCode,
                errorMessage == null ? "" : errorMessage, "", elapsedMillis(startedAt)
        );
    }

    /**
     * 경과 시간을 밀리초로 반환합니다.
     *
     * @param startedAt 시작 nano time
     * @return 경과 밀리초
     */
    private long elapsedMillis(long startedAt) {
        return Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
    }

    /**
     * Windows에서는 운영체제 인증서 저장소를 사용하고 그 외 환경에서는 JVM 기본 저장소를 사용합니다.
     *
     * @return QA HTTP 클라이언트
     */
    private HttpClient selectHttpClient() {
        HttpClient.Builder builder = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL);
        if (!System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("windows")) {
            return builder.build();
        }
        try {
            KeyStore windowsRoot = KeyStore.getInstance("Windows-ROOT");
            windowsRoot.load(null, null);
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
                    TrustManagerFactory.getDefaultAlgorithm()
            );
            trustManagerFactory.init(windowsRoot);
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManagerFactory.getTrustManagers(), new SecureRandom());
            return builder.sslContext(sslContext).build();
        } catch (Exception exception) {
            return builder.build();
        }
    }

    private record ParserProfile(
            String profileCode,
            String listItemSelector,
            String titleSelector,
            String dateSelector,
            String linkSelector,
            String datePattern
    ) {
    }

    private record SourceSeed(
            String publicCode,
            String sidoName,
            String sigunguName,
            String institutionName,
            String noticeUrl
    ) {
    }

    private record ProfileResult(
            String profileCode,
            int discoveredCount,
            int validCount,
            int invalidCount,
            List<String> samples
    ) {
    }

    private record QaResult(
            String publicCode,
            String sidoName,
            String sigunguName,
            String institutionName,
            String noticeUrl,
            String statusCode,
            String profileCode,
            int discoveredCount,
            int validCount,
            int invalidCount,
            Integer httpStatus,
            String errorCode,
            String errorMessage,
            String samples,
            long elapsedMillis
    ) {
    }
}
