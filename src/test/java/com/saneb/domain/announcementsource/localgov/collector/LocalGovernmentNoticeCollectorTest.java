/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: LocalGovernmentNoticeCollectorTest.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.announcementsource.localgov.collector;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.localgov.support.LocalGovernmentNoticeUrlValidator;
import com.saneb.domain.announcementsource.localgov.vo.LocalGovernmentNoticeParserProfileRow;
import com.saneb.domain.announcementsource.localgov.vo.LocalGovernmentNoticeSourceRow;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.net.UnknownHostException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LocalGovernmentNoticeCollectorTest {

    private LocalGovernmentNoticeCollector collector;

    /**
     * 네트워크 호출 없이 파서 보조 규칙을 검사할 수집기를 준비합니다.
     */
    @BeforeEach
    void setUp() {
        collector = new LocalGovernmentNoticeCollector(
                new LocalGovernmentNoticeUrlValidator(),
                new AnnouncementSourceIdentityNormalizer(),
                new ObjectMapper(),
                1000,
                1024 * 1024,
                1,
                "saneB-test"
        );
    }

    /**
     * 두 자리 연도 등록일을 2000년대 날짜로 변환합니다.
     */
    @Test
    void parseDateConvertsTwoDigitYear() {
        assertThat(collector.parseDate("등록일 26.07.13", null))
                .isEqualTo(LocalDate.of(2026, 7, 13));
    }

    /**
     * 당일 신규 공고의 시각 전용 표기는 서울 기준 오늘 등록일로 변환합니다.
     */
    @Test
    void parsePostedDateConvertsTimeOnlyValueToToday() {
        assertThat(collector.parsePostedDate("15:36:26", "yyyy-MM-dd"))
                .isEqualTo(LocalDate.now(ZoneId.of("Asia/Seoul")));
    }

    /**
     * 제한시간 초과와 네트워크 연결 오류를 모두 전송 재시도 대상으로 분류합니다.
     */
    @Test
    void isRetryableTransportFailureIncludesNetworkErrors() {
        assertThat(collector.isRetryableTransportFailure(selectFailureOutcome("RETRYABLE"))).isTrue();
        assertThat(collector.isRetryableTransportFailure(selectFailureOutcome("NETWORK_ERROR"))).isTrue();
        assertThat(collector.isRetryableTransportFailure(selectFailureOutcome("HTTP_ERROR"))).isFalse();
    }

    /**
     * 정상 수집 결과의 빈 오류 코드가 재시도 판정에서 예외를 만들지 않는지 검증합니다.
     */
    @Test
    void successfulOutcomeWithNoErrorCodeIsNotRetryable() {
        LocalGovernmentNoticeCollectionOutcome outcome = new LocalGovernmentNoticeCollectionOutcome(
                UUID.randomUUID(), "SUCCESS", 1, 0, 200, null, null,
                "fingerprint", null, null, java.util.List.of()
        );

        assertThat(collector.isRetryableTransportFailure(outcome)).isFalse();
    }

    /**
     * 새글 표식이 제목 링크 앞에 있어도 제목 클래스형 선택자가 공고 링크를 찾는지 검증합니다.
     */
    @Test
    void subjectNoticeSelectorAcceptsMarkerBeforeTitleLink() {
        Document document = Jsoup.parse("""
                <table><tbody><tr>
                  <td class="subject"><span class="new">새글</span><a href="/notice/1">지원사업 안내</a></td>
                  <td>담당부서</td><td>2026-07-27</td>
                </tr></tbody></table>
                """);

        Element title = document.selectFirst(
                "td.subject > a, td.title > a, td.bb-list-title > a"
        );

        assertThat(title).isNotNull();
        assertThat(title.text()).isEqualTo("지원사업 안내");
    }

    /**
     * 성북구 검색 폼 내부 행을 제외하고 실제 고시공고 행만 선택하는지 검증합니다.
     */
    @Test
    void seongbukSelectorIgnoresSearchFormRows() {
        Document document = Jsoup.parse("""
                <table><tbody><tr><td>검색</td></tr></tbody></table>
                <table class="p-table simple"><tbody class="text_center">
                  <tr><td>1</td><td class="p-subject"><a href="/notice/1">공고 제목</a></td><td>2026-07-27</td></tr>
                </tbody></table>
                """);

        assertThat(document.select(
                "table.p-table.simple tbody.text_center > tr:has(> td.p-subject > a)"
        )).singleElement();
    }

    /**
     * 창원시 목록에서 상세 링크가 없는 행과 전화번호 링크를 공고 후보로 선택하지 않는지 검증합니다.
     */
    @Test
    void changwonSelectorIgnoresRowsWithoutDetailLink() {
        Document document = Jsoup.parse("""
                <table class="t3"><tbody class="tb">
                  <tr><td>1</td><td class="tal"><a class="a1" href="?amode=view&id=1">공고 제목</a></td>
                      <td><a href="tel:055-000-0000">전화</a></td><td>2026-07-27</td><td></td></tr>
                  <tr><td>2</td><td class="tal">상세 링크 없는 공고</td>
                      <td><a href="tel:055-000-0001">전화</a></td><td>2026-07-27</td><td></td></tr>
                </tbody></table>
                """);

        Elements rows = document.select("table.t3 tbody.tb > tr:has(> td.tal > a.a1)");

        assertThat(rows).singleElement();
        assertThat(rows.first().selectFirst("td.tal > a.a1").text()).isEqualTo("공고 제목");
    }

    /**
     * DNS 조회 실패를 원문 없이 안전한 운영 진단 코드로 변환합니다.
     */
    @Test
    void selectNetworkFailureClassifiesUnknownHost() {
        LocalGovernmentNoticeCollectionOutcome result = collector.selectNetworkFailure(
                UUID.randomUUID(),
                new IOException("wrapped", new UnknownHostException("private-host.example"))
        );

        assertThat(result.errorCode()).isEqualTo("DNS_LOOKUP_FAILED");
        assertThat(result.errorMessage()).isEqualTo("기관 사이트 주소를 조회하지 못했습니다.");
    }

    /**
     * data-url에 저장된 상세 경로를 절대 URL로 해석합니다.
     */
    @Test
    void selectResolvedLinkReadsDataUrl() {
        Document document = Jsoup.parse(
                "<a data-url='/notice/view.do?nttId=1234'>공고 상세 제목</a>",
                "https://example.go.kr/notice/list.do"
        );
        Element element = document.selectFirst("a");

        LocalGovernmentNoticeCollector.ResolvedLink result = collector.selectResolvedLink(element);

        assertThat(result).isNotNull();
        assertThat(result.rawLink()).isEqualTo("/notice/view.do?nttId=1234");
        assertThat(result.absoluteLink()).isEqualTo("https://example.go.kr/notice/view.do?nttId=1234");
    }

    /**
     * 공공 게시판이 query 공백을 인코딩하지 않은 경우에도 안전한 URI로 정규화합니다.
     */
    @Test
    void selectResolvedLinkEncodesWhitespaceInPublicBoardUrl() {
        Document document = Jsoup.parse(
                "<a href='/board/view.example?orderBy=REGISTER_DATE DESC&dataSid=100'>공고 상세 제목</a>",
                "https://example.go.kr/board/list.example"
        );

        LocalGovernmentNoticeCollector.ResolvedLink result =
                collector.selectResolvedLink(document.selectFirst("a"));

        assertThat(result).isNotNull();
        assertThat(result.absoluteLink())
                .isEqualTo("https://example.go.kr/board/view.example?orderBy=REGISTER_DATE%20DESC&dataSid=100");
    }

    /**
     * onclick 문자열에 명시된 상세 경로만 실행 없이 해석합니다.
     */
    @Test
    void selectResolvedLinkReadsExplicitScriptPath() {
        Document document = Jsoup.parse(
                "<a href='#' onclick=\"openNotice('/bbs/detail.do?seq=77')\">공고 상세 제목</a>",
                "https://example.go.kr/bbs/list.do"
        );
        Element element = document.selectFirst("a");

        LocalGovernmentNoticeCollector.ResolvedLink result = collector.selectResolvedLink(element);

        assertThat(result).isNotNull();
        assertThat(result.absoluteLink()).isEqualTo("https://example.go.kr/bbs/detail.do?seq=77");
    }

    /**
     * URL이 없는 임의 JavaScript 함수는 상세 링크로 추측하지 않습니다.
     */
    @Test
    void selectResolvedLinkRejectsOpaqueScriptFunction() {
        Document document = Jsoup.parse(
                "<a href='javascript:openNotice(77)'>공고 상세 제목</a>",
                "https://example.go.kr/bbs/list.do"
        );

        assertThat(collector.selectResolvedLink(document.selectFirst("a"))).isNull();
    }

    /**
     * 검증된 JSON 필드 매핑으로 공고 제목·날짜·상세 URL을 추출합니다.
     */
    @Test
    void parseJsonDocumentUsesConfiguredFieldMapping() {
        UUID sourceId = UUID.randomUUID();
        LocalGovernmentNoticeSourceRow source = new LocalGovernmentNoticeSourceRow(
                sourceId, "LGS-TEST", "11", "서울", "110", "테스트구", "LOCAL_GOVERNMENT", "테스트기관",
                "https://example.go.kr", "https://example.go.kr/notices/", "https://example.go.kr/api/notices",
                "json", "DEFAULT", "GET", null, "TEST_JSON", "테스트 JSON", null, "HIGH", "VERIFIED",
                "GENERAL_NOTICE", "KEYWORD_FILTERED", true, null, null, "테스트 의미 검증",
                false, "READY", null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null
        );
        LocalGovernmentNoticeParserProfileRow profile = new LocalGovernmentNoticeParserProfileRow(
                "TEST_JSON", "테스트 JSON", "GENERIC_JSON", null, null, null, null, "yyyy-MM-dd",
                "JSON", "items", "title", "date", "id", "/notices/view?id={value}",
                "AUTO", null, null, null, true
        );
        byte[] body = """
                {"items":[{"title":"2026년 지원사업 공고","date":"2026-07-13","id":"100"}]}
                """.getBytes(StandardCharsets.UTF_8);

        LocalGovernmentNoticeCollectionOutcome result = collector.parseJsonDocument(
                source, profile, body, 200, null, null, "fingerprint"
        );

        assertThat(result.resultStatusCode()).isEqualTo("SUCCESS");
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().getFirst().sourceUrl())
                .isEqualTo("https://example.go.kr/notices/view?id=100");
    }

    /**
     * 최근 수집 범위를 벗어난 과거 JSON 공고는 필드 누락 실패로 계산하지 않습니다.
     */
    @Test
    void parseJsonDocumentSkipsOldNoticeWithoutPartialFailure() {
        UUID sourceId = UUID.randomUUID();
        LocalGovernmentNoticeSourceRow source = new LocalGovernmentNoticeSourceRow(
                sourceId, "LGS-TEST", "11", "서울", "110", "테스트구", "LOCAL_GOVERNMENT", "테스트기관",
                "https://example.go.kr", "https://example.go.kr/notices/", "https://example.go.kr/api/notices",
                "json", "DEFAULT", "GET", null, "TEST_JSON", "테스트 JSON", null, "HIGH", "VERIFIED",
                "GENERAL_NOTICE", "KEYWORD_FILTERED", true, null, null, "테스트 의미 검증",
                false, "READY", null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null
        );
        LocalGovernmentNoticeParserProfileRow profile = new LocalGovernmentNoticeParserProfileRow(
                "TEST_JSON", "테스트 JSON", "GENERIC_JSON", null, null, null, null, "yyyy-MM-dd",
                "JSON", "items", "title", "date", "id", "/notices/view?id={value}",
                "AUTO", null, null, null, true
        );
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        byte[] body = ("""
                {"items":[
                  {"title":"최근 지원사업 공고","date":"%s","id":"100"},
                  {"title":"과거 지원사업 공고","date":"%s","id":"99"}
                ]}
                """.formatted(today.minusDays(1), today.minusYears(2))).getBytes(StandardCharsets.UTF_8);

        LocalGovernmentNoticeCollectionOutcome result = collector.parseJsonDocument(
                source, profile, body, 200, null, null, "fingerprint"
        );

        assertThat(result.resultStatusCode()).isEqualTo("SUCCESS");
        assertThat(result.discoveredCount()).isEqualTo(1);
        assertThat(result.failedCount()).isZero();
        assertThat(result.items()).hasSize(1);
    }

    /**
     * boardView 함수의 리터럴 인자를 검증된 상세 URL 템플릿에 대입합니다.
     */
    @Test
    void selectResolvedLinkBuildsUrlFromSafeFunctionArguments() {
        Document document = Jsoup.parse(
                "<a href='#' onclick=\"boardView('listForm','writer','Y','153420','22','0301010000','1')\">공고 제목</a>",
                "https://example.go.kr/portal/bbs/list.do?ptIdx=22&mId=0301010000"
        );

        LocalGovernmentNoticeCollector.ResolvedLink result = collector.selectResolvedLink(
                document.selectFirst("a"),
                selectTemplateProfile(
                        "boardView",
                        "/portal/bbs/view.do?mId={arg:6}&bIdx={arg:4}&ptIdx={arg:5}"
                ),
                document.location()
        );

        assertThat(result).isNotNull();
        assertThat(result.absoluteLink()).isEqualTo(
                "https://example.go.kr/portal/bbs/view.do?mId=0301010000&bIdx=153420&ptIdx=22"
        );
    }

    /**
     * data 속성과 목록 URL query를 조합해 상세 URL을 생성합니다.
     */
    @Test
    void selectResolvedLinkBuildsUrlFromAttributeAndQuery() {
        Document document = Jsoup.parse(
                "<a href='#' data-req-get-p-idx='408999'>공고 제목</a>",
                "https://example.go.kr/portal/board/post/list.do?bcIdx=500&mid=0501010000"
        );

        LocalGovernmentNoticeCollector.ResolvedLink result = collector.selectResolvedLink(
                document.selectFirst("a"),
                selectTemplateProfile(
                        null,
                        "/portal/board/post/view.do?bcIdx={query:bcIdx}&mid={query:mid}&idx={attr:data-req-get-p-idx}"
                ),
                document.location()
        );

        assertThat(result).isNotNull();
        assertThat(result.absoluteLink()).isEqualTo(
                "https://example.go.kr/portal/board/post/view.do?bcIdx=500&mid=0501010000&idx=408999"
        );
    }

    /**
     * hidden input 값과 함수 인자를 함께 사용해 ICMS 상세 URL을 생성합니다.
     */
    @Test
    void selectResolvedLinkBuildsUrlFromDocumentInput() {
        Document document = Jsoup.parse(
                "<input name='bbsId' value='BBS_00029'>"
                        + "<a href='javascript:;' onclick=\"fn_icms_navi_common('view','795285')\">공고 제목</a>",
                "https://example.go.kr/index.do?menu_id=00000854"
        );

        LocalGovernmentNoticeCollector.ResolvedLink result = collector.selectResolvedLink(
                document.selectFirst("a"),
                selectTemplateProfile(
                        "fn_icms_navi_common",
                        "/index.do?menu_id={query:menu_id}&menu_link=/icms/bbs/selectBoardArticle.do"
                                + "&bbsId={input:bbsId}&nttId={arg:2}"
                ),
                document.location()
        );

        assertThat(result).isNotNull();
        assertThat(result.absoluteLink()).contains("bbsId=BBS_00029").contains("nttId=795285");
    }

    /**
     * goTo.view 함수의 점 표기 함수명과 네 개 인자를 안전하게 해석합니다.
     */
    @Test
    void selectResolvedLinkBuildsUrlFromQualifiedFunctionName() {
        Document document = Jsoup.parse(
                "<a href='#' onclick=\"goTo.view('list','67285','145','0404010000'); return false;\">공고 제목</a>",
                "https://www.ulju.ulsan.kr/ulju/bbs/list.do?ptIdx=145&mId=0404010000"
        );

        LocalGovernmentNoticeCollector.ResolvedLink result = collector.selectResolvedLink(
                document.selectFirst("a"),
                selectTemplateProfile("goTo.view", "view.do?mId={arg:4}&bIdx={arg:2}&ptIdx={arg:3}"),
                document.location()
        );

        assertThat(result).isNotNull();
        assertThat(result.absoluteLink()).isEqualTo(
                "https://www.ulju.ulsan.kr/ulju/bbs/view.do?mId=0404010000&bIdx=67285&ptIdx=145"
        );
    }

    /**
     * 양천 새올 함수의 단일 리터럴 인자를 동일 경로 상세 URL로 변환합니다.
     */
    @Test
    void selectResolvedLinkBuildsYangcheonSeolUrl() {
        Document document = Jsoup.parse(
                "<a href=\"javascript:doSeolContentDeailView('47490');\">공고 제목</a>",
                "https://www.yangcheon.go.kr/site/yangcheon/ex/seol/seolCollectList.do"
        );

        LocalGovernmentNoticeCollector.ResolvedLink result = collector.selectResolvedLink(
                document.selectFirst("a"),
                selectTemplateProfile(
                        "doSeolContentDeailView",
                        "seolContentDeailView.do?not_ancmt_mgt_no={arg:1}"
                ),
                document.location()
        );

        assertThat(result).isNotNull();
        assertThat(result.absoluteLink()).isEqualTo(
                "https://www.yangcheon.go.kr/site/yangcheon/ex/seol/"
                        + "seolContentDeailView.do?not_ancmt_mgt_no=47490"
        );
    }

    /**
     * 대전시 통합 목록은 고정 허용된 구청 전자민원 호스트로만 상세 링크를 생성합니다.
     */
    @Test
    void selectDaejeonEminwonLinkUsesReviewedHostMap() {
        Document document = Jsoup.parse(
                "<a href='#' onclick=\"popupCenterNew('seogu','51276')\">도로 점용 공고</a>",
                "https://www.daejeon.go.kr/drh/MediaList.do?menuSeq=2558"
        );

        LocalGovernmentNoticeCollector.ResolvedLink result =
                collector.selectDaejeonEminwonLink(document.selectFirst("a"));

        assertThat(result).isNotNull();
        assertThat(result.absoluteLink())
                .startsWith("https://eminwon.seogu.go.kr/")
                .endsWith("not_ancmt_mgt_no=51276");
    }

    /**
     * 전송 재시도 판정용 실패 결과를 생성합니다.
     *
     * @param errorCode 검사할 오류 코드
     * @return 실패 수집 결과
     */
    private LocalGovernmentNoticeCollectionOutcome selectFailureOutcome(String errorCode) {
        return new LocalGovernmentNoticeCollectionOutcome(
                UUID.randomUUID(), "FAILED", 0, 1, null, null, null,
                null, errorCode, "테스트 오류", java.util.List.of()
        );
    }

    /**
     * 대전시 통합 목록에서 알려지지 않은 기관 코드는 외부 링크로 변환하지 않습니다.
     */
    @Test
    void selectDaejeonEminwonLinkRejectsUnknownOrganization() {
        Document document = Jsoup.parse(
                "<a href='#' onclick=\"popupCenterNew('unknown','51276')\">위조 공고</a>",
                "https://www.daejeon.go.kr/drh/MediaList.do?menuSeq=2558"
        );

        assertThat(collector.selectDaejeonEminwonLink(document.selectFirst("a"))).isNull();
    }

    /**
     * 공개 게시판의 폼 POST 설정을 UTF-8 URL 인코딩 본문으로 변환합니다.
     */
    @Test
    void selectRequestFormBodyEncodesPublicBoardFields() {
        String result = collector.selectRequestFormBody("""
                {"method":"selectListOfrNotAncmt","title":"고시공고","pageIndex":""}
                """);

        assertThat(result)
                .contains("method=selectListOfrNotAncmt")
                .contains("title=%EA%B3%A0%EC%8B%9C%EA%B3%B5%EA%B3%A0")
                .contains("pageIndex=");
    }

    /**
     * 폼 POST 설정에 객체 값이나 실행 가능한 필드명이 있으면 거부합니다.
     */
    @Test
    void selectRequestFormBodyRejectsInvalidConfiguration() {
        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> collector.selectRequestFormBody("{\"method-name\":{\"nested\":true}}")
        ).isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * 함수 호출에 표현식이 포함되면 URL 템플릿 적용을 거부합니다.
     */
    @Test
    void selectResolvedLinkRejectsExecutableFunctionExpression() {
        Document document = Jsoup.parse(
                "<a href='#' onclick=\"boardView(document.cookie,'153420')\">공고 제목</a>",
                "https://example.go.kr/portal/bbs/list.do"
        );

        LocalGovernmentNoticeCollector.ResolvedLink result = collector.selectResolvedLink(
                document.selectFirst("a"),
                selectTemplateProfile("boardView", "/portal/bbs/view.do?bIdx={arg:1}"),
                document.location()
        );

        assertThat(result).isNull();
    }

    /**
     * 템플릿 결과가 다른 기관 host를 가리키면 링크 생성을 거부합니다.
     */
    @Test
    void selectResolvedLinkRejectsCrossHostTemplate() {
        Document document = Jsoup.parse(
                "<a href='#' onclick=\"openView('100')\">공고 제목</a>",
                "https://example.go.kr/notices"
        );

        LocalGovernmentNoticeCollector.ResolvedLink result = collector.selectResolvedLink(
                document.selectFirst("a"),
                selectTemplateProfile("openView", "https://invalid.example/view?id={arg:1}"),
                document.location()
        );

        assertThat(result).isNull();
    }

    /**
     * 테스트용 안전 링크 템플릿 프로필을 생성합니다.
     *
     * @param functionName 함수명 또는 속성 기반이면 null
     * @param linkUrlTemplate URL 템플릿
     * @return 파서 프로필
     */
    private LocalGovernmentNoticeParserProfileRow selectTemplateProfile(
            String functionName,
            String linkUrlTemplate
    ) {
        return new LocalGovernmentNoticeParserProfileRow(
                "TEST_TEMPLATE", "테스트 템플릿", "GENERIC_TABLE",
                "table tbody tr", "td a", "td:last-child", "td a", null,
                "HTML", null, null, null, null, null,
                "SAFE_TEMPLATE", functionName, null, linkUrlTemplate, true
        );
    }
}
