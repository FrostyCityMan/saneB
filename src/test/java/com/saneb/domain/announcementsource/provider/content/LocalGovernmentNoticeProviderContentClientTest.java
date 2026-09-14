package com.saneb.domain.announcementsource.provider.content;

import static org.assertj.core.api.Assertions.assertThat;

import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.BodyAvailabilityCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.BodySourceCode;
import com.saneb.domain.announcementsource.provider.content.ProviderContentCodes.FailureCode;
import com.saneb.domain.announcementsource.provider.content.ProviderContentCodes.StatusCode;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.api.Test;

class LocalGovernmentNoticeProviderContentClientTest {

    private static final int TWO_MEBIBYTES = 2 * 1024 * 1024;
    private static final String HOST = "city.example.go.kr";
    private static final String REGISTERED_URL = "https://" + HOST + "/notices";
    private static final String DETAIL_URL = "https://" + HOST + "/notices/42";
    private static final UUID SOURCE_ID = UUID.fromString("77000000-0000-0000-0000-000000000001");

    private static String bbsHtml(boolean compact, String body) {
        return "<main><header>수출 특허 메뉴</header><p>스타트업 관련 공고</p>"
                + (compact ? "<div class='p-wrap bbs bbs__view'><table class='p-table block'>" : "<table class='bbs_default view'>")
                + (compact ? "<tr><td><span class='p-table__subject_text'>지원사업 제목</span></td></tr>" : "<tr><th>제목</th><td>지원사업 제목</td></tr>")
                + "<tr><td title='내용'>" + body + "</td></tr>"
                + "<tr><th>파일</th><td><span>수출 특허 자료.pdf</span><a href='/www/downloadBbsFile.do?atchmnflNo=1'>다운로드</a></td></tr>"
                + "</table>" + (compact ? "</div>" : "") + "<footer>의회 감사 고시</footer></main>";
    }

    private ProviderContentResult bbsResult(String host, String suffix, String body) {
        var transport = new StubTransport(); transport.enqueue(html(body));
        var result = client(true, transport, publicValidator()).selectContent(new ProviderContentRequest(
                "LOCAL_GOV_NOTICE", SOURCE_ID, "https://" + host + "/www/selectBbsNttList.do",
                "https://" + host + "/www/selectBbsNttView.do" + suffix));
        assertThat(transport.callCount()).isEqualTo(1);
        return result;
    }

    @Test void verifiedBbsModelsExtractOnlyOfficialBodyAndRetainActualExclusionContext() {
        String[][] sites = {{"www.taebaek.go.kr", "25"}, {"www.hsg.go.kr", "65"}, {"www.yw.go.kr", "17"}};
        for (var site : sites) {
            var result = bbsResult(site[0], "?bbsNo=" + site[1] + "&nttNo=42", bbsHtml("65".equals(site[1]),
                    "소상공인 지원금 <nav>투자유치 메뉴</nav> 수출기업 제외 <a href='/apply'>온라인 신청</a>"));
            assertThat(result.statusCode()).isEqualTo(StatusCode.AVAILABLE);
            assertThat(result.bodyText()).isEqualTo("소상공인 지원금 수출기업 제외 온라인 신청");
        }
    }

    @Test void verifiedBbsMissingOrAmbiguousStructureNeverFallsBackToPageText() {
        String valid = bbsHtml(false, "소상공인 지원금");
        for (String html : List.of("<main>소상공인 지원금</main>", valid + valid,
                valid.replace("title='내용'", "title='변경'"), valid.replace("제목</th>", "변경</th>"),
                valid.replace("<td title='내용'>", "<td title='내용'>중복</td><td title='내용'>"))) {
            var result = bbsResult("www.taebaek.go.kr", "?bbsNo=25&nttNo=42", html);
            assertThat(result.failureCode()).isEqualTo(FailureCode.BODY_SELECTOR_CHANGED);
            assertThat(result.bodyAvailabilityCode()).isEqualTo(BodyAvailabilityCode.FETCH_FAILED);
            assertThat(result.bodyText()).isNull();
        }
    }

    @Test void verifiedBbsEmptyBodyCannotUseAttachmentFilenameAsBody() {
        var result = bbsResult("www.yw.go.kr", "?bbsNo=17&nttNo=42", bbsHtml(false, "<nav>메뉴</nav>"));
        assertThat(result.failureCode()).isEqualTo(FailureCode.BODY_TEXT_EMPTY);
        assertThat(result.bodyText()).isNull();
    }

    @Test void verifiedBbsDuplicateOrMissingBoardParameterCannotSelectGenericFallback() {
        for (String query : List.of("?nttNo=42", "?bbsNo=25&bbsNo=99", "?bbsNo=25&%62bsNo=25", "?bbsNo", "?bbsNo=", "?bbsNo=25%20", "?bbsNo=025")) {
            var result = bbsResult("www.taebaek.go.kr", query, bbsHtml(false, "본문"));
            assertThat(result.failureCode()).isEqualTo(FailureCode.BODY_SELECTOR_CHANGED);
        }
    }

    @Test void unmeasuredHostOrBoardRetainsExistingGenericContract() {
        assertThat(bbsResult("another.example.go.kr", "?bbsNo=25", "<main>기존 본문</main>").bodyText()).isEqualTo("기존 본문");
        assertThat(bbsResult("www.taebaek.go.kr", "?bbsNo=999", "<main>다른 게시판</main>").bodyText()).isEqualTo("다른 게시판");
    }

    @Test void observedHoengseongSessionPathStillRequiresUniqueOfficialBody() {
        var result = bbsResult("www.hsg.go.kr", ";jsessionid=synthetic?bbsNo=65&nttNo=42", bbsHtml(true, "소상공인 지원금"));
        assertThat(result.bodyText()).isEqualTo("소상공인 지원금");
        var duplicate = bbsResult("www.hsg.go.kr", "?bbsNo=65&nttNo=42", bbsHtml(true, "본문")
                .replace("</span>", "</span><span class='p-table__subject_text'></span>"));
        assertThat(duplicate.failureCode()).isEqualTo(FailureCode.BODY_SELECTOR_CHANGED);
    }

    private static String seoguHtml(String body) {
        return "<main><header>수출 특허 메뉴</header><div class='card mb-4 program--view'>"
                + "<h2 class='card-header h2'>상세정보</h2><div class='card-body prog bucket-form'>"
                + "<span id='notAncmtMgtNo'>51668</span><span id='notAncmtSj'>소상공인 지원사업</span>"
                + "<span id='depNm'>기관정보</span><span id='chrNm'>담당자 대역</span><span id='telno'>연락처 대역</span>"
                + "<span id='notAncmtCn'>" + body + "</span>"
                + "<div class='bbs--view--file'><span>수출 특허 신청서.hwp</span><a href='/file/download'>다운로드</a></div>"
                + "</div></div><footer>의회 감사 고시</footer></main>";
    }

    private ProviderContentResult seoguResult(String pathAndQuery, String page) {
        var transport = new StubTransport(); transport.enqueue(html(page));
        var result = client(true, transport, publicValidator()).selectContent(new ProviderContentRequest(
                "LOCAL_GOV_NOTICE", SOURCE_ID, "https://www.seogu.go.kr/prog/saeolGosi/GOSI/kor/sub04_02_01/list.do",
                "https://www.seogu.go.kr" + pathAndQuery));
        assertThat(transport.callCount()).isEqualTo(1);
        return result;
    }

    private ProviderContentResult seoguResult(String query, String page, boolean officialPath) {
        return seoguResult((officialPath ? "/prog/saeolGosi/GOSI/kor/sub04_02_01/view.do" : "/another/view.do") + query, page);
    }

    @Test void observedSeoguCardExcludesStaffMenusAndFileNamesButPreservesBodyContext() {
        var result = seoguResult("?notAncmtMgtNo=51668", seoguHtml(
                "소상공인 지원금 <nav>투자유치 메뉴</nav> <span role='navigation'>특허 메뉴</span>"
                        + "수출기업 제외 <a href='/apply'>온라인 신청</a>"), true);
        assertThat(result.statusCode()).isEqualTo(StatusCode.AVAILABLE);
        assertThat(result.bodyText()).isEqualTo("소상공인 지원금 수출기업 제외 온라인 신청");
    }

    @Test void observedSeoguMissingAmbiguousOrChangedCardNeverUsesWholePageFallback() {
        String valid = seoguHtml("소상공인 지원금");
        for (String page : List.of("<main>소상공인 지원금</main>", valid + valid,
                valid.replace("id='notAncmtCn'", "id='changed'"),
                valid.replace("<span id='notAncmtCn'>", "<span id='notAncmtCn'>중복</span><span id='notAncmtCn'>"),
                valid.replace("<span id='notAncmtSj'>소상공인 지원사업</span>", "<span id='notAncmtSj'></span>"),
                valid.replace("<span id='notAncmtSj'>", "<span id='notAncmtSj'>중복</span><span id='notAncmtSj'>"),
                valid.replace("<span id='notAncmtMgtNo'>", "<span id='notAncmtMgtNo'>51668</span><span id='notAncmtMgtNo'>"),
                valid.replace("id='notAncmtMgtNo'>51668", "id='notAncmtMgtNo'>99999"))) {
            var result = seoguResult("?notAncmtMgtNo=51668", page, true);
            assertThat(result.failureCode()).isEqualTo(FailureCode.BODY_SELECTOR_CHANGED);
            assertThat(result.bodyAvailabilityCode()).isEqualTo(BodyAvailabilityCode.FETCH_FAILED);
            assertThat(result.bodyText()).isNull();
        }
    }

    @Test void observedSeoguInvalidIdentityQueryCannotFallBackToPageText() {
        for (String query : List.of("", "?notAncmtMgtNo=", "?notAncmtMgtNo=51668&notAncmtMgtNo=51668",
                "?notAncmtMgtNo=51668&other=1", "?notAncmtMgtNo=51668%20", "?notAncmtMgtNo=99999")) {
            var result = seoguResult(query, seoguHtml("소상공인 지원금"), true);
            assertThat(result.failureCode()).isEqualTo(FailureCode.BODY_SELECTOR_CHANGED);
            assertThat(result.bodyText()).isNull();
        }
    }

    @Test void observedSeoguEmptyBodyCannotBecomeStaffOrAttachmentText() {
        var result = seoguResult("?notAncmtMgtNo=51668", seoguHtml("<nav>메뉴</nav>"), true);
        assertThat(result.failureCode()).isEqualTo(FailureCode.BODY_TEXT_EMPTY);
        assertThat(result.bodyText()).isNull();
    }

    @Test void otherSeoguBoardKeepsExistingContract() {
        assertThat(seoguResult("?notice=1", "<main>기존 본문</main>", false).bodyText()).isEqualTo("기존 본문");
    }

    @Test
    void selectContentDoesNothingWhileFeatureFlagIsOff() {
        AtomicInteger resolutionCount = new AtomicInteger();
        StubTransport transport = new StubTransport();
        ProviderContentUrlValidator validator = new ProviderContentUrlValidator(host -> {
            resolutionCount.incrementAndGet();
            return new InetAddress[]{publicAddress()};
        });
        LocalGovernmentNoticeProviderContentClient client = client(false, transport, validator);

        ProviderContentResult result = client.selectContent(request());

        assertThat(result.statusCode()).isEqualTo(StatusCode.DISABLED);
        assertThat(result.bodySourceCode()).isEqualTo(BodySourceCode.NONE);
        assertThat(result.bodyAvailabilityCode()).isEqualTo(BodyAvailabilityCode.UNSUPPORTED);
        assertThat(result.failureCode()).isEqualTo(FailureCode.FEATURE_DISABLED);
        assertThat(transport.callCount()).isZero();
        assertThat(resolutionCount).hasValue(0);
    }

    @Test
    void selectContentExtractsStaticHtmlWithoutExecutingScriptOrFollowingLinks() {
        StubTransport transport = new StubTransport();
        transport.enqueue(html(
                """
                        <html><body>
                        <main>소상공인 지원금 안내</main>
                        <a href="/files/notice.pdf">첨부파일</a>
                        <script>fetch('https://outside.example/track')</script>
                        </body></html>
                        """
        ));
        LocalGovernmentNoticeProviderContentClient client = client(true, transport, publicValidator());

        ProviderContentResult result = client.selectContent(request());

        assertThat(result.statusCode()).isEqualTo(StatusCode.AVAILABLE);
        assertThat(result.bodySourceCode()).isEqualTo(BodySourceCode.DETAIL_PAGE_TEXT);
        assertThat(result.bodyAvailabilityCode()).isEqualTo(BodyAvailabilityCode.AVAILABLE);
        assertThat(result.bodyText()).isEqualTo("소상공인 지원금 안내");
        assertThat(result.bodyText()).doesNotContain("첨부파일", "fetch", "outside.example");
        assertThat(result.finalUrl()).isEqualTo(DETAIL_URL);
        assertThat(result.attemptCount()).isEqualTo(1);
        assertThat(transport.requestUris()).containsExactly(URI.create(DETAIL_URL));
        assertThat(transport.lastPinnedAddresses()).containsExactly(publicAddress());
        assertThat(transport.lastReadTimeout()).isEqualTo(Duration.ofSeconds(7));
        assertThat(transport.lastMaxResponseBytes()).isEqualTo(TWO_MEBIBYTES);
    }

    @Test
    void selectContentExcludesAttachmentNamesFromClassificationBodyEvidence() {
        StubTransport transport = new StubTransport();
        transport.enqueue(html(
                """
                        <html><body>
                        <main>
                        <p>소상공인 지원금 본문</p>
                        <section class="attachment-list">
                        <span>첨부파일</span>
                        <span>수출자료.pdf</span>
                        <a href="/download?fileId=101">다운로드</a>
                        </section>
                        <a download href="/storage/102">수출 통계</a>
                        <a href="/files/research.hwp">R&amp;D자료.hwp</a>
                        <a href="/attachment/download?fileId=103">제조 기술 자료</a>
                        <a href="#" onclick="downloadFile('104')">특허자료.docx</a>
                        <a href="/notices/42/details">상세보기</a>
                        <a href="/apply?noticeId=42">온라인 신청</a>
                        </main>
                        </body></html>
                        """
        ));
        LocalGovernmentNoticeProviderContentClient client = client(true, transport, publicValidator());

        ProviderContentResult result = client.selectContent(request());

        assertThat(result.statusCode()).isEqualTo(StatusCode.AVAILABLE);
        assertThat(result.bodyText()).isEqualTo("소상공인 지원금 본문 상세보기 온라인 신청");
        assertThat(result.bodyText()).doesNotContain(
                "수출자료.pdf",
                "R&D자료.hwp",
                "제조 기술 자료",
                "특허자료.docx",
                "수출자료",
                "R&D자료",
                "수출 통계",
                "첨부파일"
        );
        assertThat(transport.requestUris()).containsExactly(URI.create(DETAIL_URL));
    }

    @Test
    void selectContentRejectsArbitraryDetailHostBeforeTransport() {
        StubTransport transport = new StubTransport();
        LocalGovernmentNoticeProviderContentClient client = client(true, transport, publicValidator());
        ProviderContentRequest request = new ProviderContentRequest(
                "LOCAL_GOV_NOTICE",
                SOURCE_ID,
                REGISTERED_URL,
                "https://outside.example/notices/42"
        );

        ProviderContentResult result = client.selectContent(request);

        assertThat(result.statusCode()).isEqualTo(StatusCode.FETCH_FAILED);
        assertThat(result.failureCode()).isEqualTo(FailureCode.DETAIL_HOST_NOT_ALLOWED);
        assertThat(transport.callCount()).isZero();
    }

    @Test
    void selectContentBlocksPrivateAddressBeforeTransport() {
        StubTransport transport = new StubTransport();
        ProviderContentUrlValidator validator = new ProviderContentUrlValidator(
                host -> new InetAddress[]{address(10, 0, 0, 7)}
        );
        LocalGovernmentNoticeProviderContentClient client = client(true, transport, validator);

        ProviderContentResult result = client.selectContent(request());

        assertThat(result.failureCode()).isEqualTo(FailureCode.ADDRESS_BLOCKED);
        assertThat(transport.callCount()).isZero();
    }

    @Test
    void selectContentRechecksDnsImmediatelyBeforeTransport() {
        StubTransport transport = new StubTransport();
        AtomicInteger resolutionCount = new AtomicInteger();
        ProviderContentUrlValidator validator = new ProviderContentUrlValidator(host -> {
            if (resolutionCount.incrementAndGet() < 3) {
                return new InetAddress[]{publicAddress()};
            }
            return new InetAddress[]{address(10, 0, 0, 8)};
        });
        LocalGovernmentNoticeProviderContentClient client = client(true, transport, validator);

        ProviderContentResult result = client.selectContent(request());

        assertThat(result.failureCode()).isEqualTo(FailureCode.ADDRESS_BLOCKED);
        assertThat(resolutionCount).hasValue(3);
        assertThat(transport.callCount()).isZero();
    }

    @Test
    void selectContentUsesSemanticMainAreaInsteadOfNavigationAndFooter() {
        StubTransport transport = new StubTransport();
        transport.enqueue(html(
                """
                        <html><body>
                        <nav>해외진출 수출바우처 메뉴</nav>
                        <main><h1>소상공인 정책자금 안내</h1><p>신청 대상과 지원 내용입니다.</p></main>
                        <footer>기관 채용공고</footer>
                        </body></html>
                        """
        ));
        LocalGovernmentNoticeProviderContentClient client = client(true, transport, publicValidator());

        ProviderContentResult result = client.selectContent(request());

        assertThat(result.statusCode()).isEqualTo(StatusCode.AVAILABLE);
        assertThat(result.bodyText()).isEqualTo("소상공인 정책자금 안내 신청 대상과 지원 내용입니다.");
        assertThat(result.bodyText()).doesNotContain("수출바우처", "채용공고");
    }

    @Test
    void selectContentExcludesNestedNavigationButKeepsActualNoticeTermsAndApplicationLinks() {
        StubTransport transport = new StubTransport();
        transport.enqueue(html("""
                <main>
                <nav><a href="/exports">수출바우처 메뉴</a></nav>
                <div role="navigation"><a href="/patents">특허 메뉴</a></div>
                <p>소상공인 지원금 본문</p>
                <p>수출기업은 지원 대상에서 제외합니다.</p>
                <a href="/apply?noticeId=42">온라인 신청</a>
                </main>
                """));

        ProviderContentResult result = client(true, transport, publicValidator()).selectContent(request());

        assertThat(result.statusCode()).isEqualTo(StatusCode.AVAILABLE);
        assertThat(result.bodyText()).isEqualTo("소상공인 지원금 본문 수출기업은 지원 대상에서 제외합니다. 온라인 신청");
        assertThat(transport.requestUris()).containsExactly(URI.create(DETAIL_URL));
    }

    @Test
    void selectContentExcludesNavigationWhenOnlyBodyFallbackIsAvailable() {
        StubTransport transport = new StubTransport();
        transport.enqueue(html("""
                <body>
                <nav>수출바우처 메뉴</nav>
                <div role="navigation">기술창업 메뉴</div>
                <section role="note">소상공인 지원금 본문</section>
                <p>탐색 메뉴 변경 안내도 본문 문장이면 보존합니다.</p>
                </body>
                """));

        ProviderContentResult result = client(true, transport, publicValidator()).selectContent(request());

        assertThat(result.statusCode()).isEqualTo(StatusCode.AVAILABLE);
        assertThat(result.bodyText()).isEqualTo("소상공인 지원금 본문 탐색 메뉴 변경 안내도 본문 문장이면 보존합니다.");
        assertThat(transport.requestUris()).containsExactly(URI.create(DETAIL_URL));
    }

    @Test
    void selectContentDoesNotTreatNavigationOnlyPageAsAvailableBody() {
        StubTransport transport = new StubTransport();
        transport.enqueue(html("<main><nav>소상공인 지원금</nav><div role='navigation'>융자 지원</div></main>"));

        ProviderContentResult result = client(true, transport, publicValidator()).selectContent(request());

        assertThat(result.statusCode()).isEqualTo(StatusCode.FETCH_FAILED);
        assertThat(result.failureCode()).isEqualTo(FailureCode.BODY_TEXT_EMPTY);
        assertThat(transport.requestUris()).containsExactly(URI.create(DETAIL_URL));
    }

    @Test
    void selectContentDoesNotFallBackToNavigationWhenSemanticAreaIsEmpty() {
        StubTransport transport = new StubTransport();
        transport.enqueue(html(
                """
                        <html><body>
                        <nav>소상공인 수출바우처 메뉴</nav>
                        <main><a href="/files/export.pdf">수출자료.pdf</a></main>
                        </body></html>
                        """
        ));
        LocalGovernmentNoticeProviderContentClient client = client(true, transport, publicValidator());

        ProviderContentResult result = client.selectContent(request());

        assertThat(result.statusCode()).isEqualTo(StatusCode.FETCH_FAILED);
        assertThat(result.failureCode()).isEqualTo(FailureCode.BODY_TEXT_EMPTY);
    }

    @Test
    void selectContentAllowsThreeSameHostRedirects() {
        StubTransport transport = new StubTransport();
        transport.enqueue(redirect("/notices/43"));
        transport.enqueue(redirect("/notices/44"));
        transport.enqueue(redirect("/notices/45"));
        transport.enqueue(html("<html><body>소상공인 정책자금</body></html>"));
        LocalGovernmentNoticeProviderContentClient client = client(true, transport, publicValidator());

        ProviderContentResult result = client.selectContent(request());

        assertThat(result.statusCode()).isEqualTo(StatusCode.AVAILABLE);
        assertThat(result.redirectCount()).isEqualTo(3);
        assertThat(result.finalUrl()).isEqualTo("https://" + HOST + "/notices/45");
        assertThat(transport.callCount()).isEqualTo(4);
    }

    @Test
    void selectContentRejectsFourthRedirect() {
        StubTransport transport = new StubTransport();
        transport.enqueue(redirect("/notices/43"));
        transport.enqueue(redirect("/notices/44"));
        transport.enqueue(redirect("/notices/45"));
        transport.enqueue(redirect("/notices/46"));
        LocalGovernmentNoticeProviderContentClient client = client(true, transport, publicValidator());

        ProviderContentResult result = client.selectContent(request());

        assertThat(result.failureCode()).isEqualTo(FailureCode.REDIRECT_LIMIT_EXCEEDED);
        assertThat(result.redirectCount()).isEqualTo(3);
        assertThat(transport.callCount()).isEqualTo(4);
    }

    @Test
    void selectContentRejectsCrossHostRedirectWithoutFollowingIt() {
        StubTransport transport = new StubTransport();
        transport.enqueue(redirect("https://outside.example/notices/42"));
        LocalGovernmentNoticeProviderContentClient client = client(true, transport, publicValidator());

        ProviderContentResult result = client.selectContent(request());

        assertThat(result.failureCode()).isEqualTo(FailureCode.DETAIL_HOST_NOT_ALLOWED);
        assertThat(transport.callCount()).isEqualTo(1);
    }

    @Test
    void selectContentRetriesTimeoutOnce() {
        StubTransport transport = new StubTransport();
        transport.enqueue(new TimeoutException("stub timeout"));
        transport.enqueue(html("<html><body>청년 지원사업</body></html>"));
        LocalGovernmentNoticeProviderContentClient client = client(true, transport, publicValidator());

        ProviderContentResult result = client.selectContent(request());

        assertThat(result.statusCode()).isEqualTo(StatusCode.AVAILABLE);
        assertThat(result.attemptCount()).isEqualTo(2);
        assertThat(transport.callCount()).isEqualTo(2);
    }

    @Test
    void selectContentRetriesServerErrorOnlyOnce() {
        StubTransport transport = new StubTransport();
        transport.enqueue(response(503, "text/html", "일시 오류".getBytes(StandardCharsets.UTF_8)));
        transport.enqueue(response(503, "text/html", "반복 오류".getBytes(StandardCharsets.UTF_8)));
        LocalGovernmentNoticeProviderContentClient client = client(true, transport, publicValidator());

        ProviderContentResult result = client.selectContent(request());

        assertThat(result.failureCode()).isEqualTo(FailureCode.HTTP_SERVER_ERROR);
        assertThat(result.attemptCount()).isEqualTo(2);
        assertThat(transport.callCount()).isEqualTo(2);
    }

    @Test
    void selectContentDoesNotRetryClientErrorOrNetworkFailure() {
        StubTransport clientErrorTransport = new StubTransport();
        clientErrorTransport.enqueue(response(404, "text/html", new byte[0]));
        StubTransport networkTransport = new StubTransport();
        networkTransport.enqueue(new IOException("stub connection failure"));

        ProviderContentResult clientError = client(
                true,
                clientErrorTransport,
                publicValidator()
        ).selectContent(request());
        ProviderContentResult networkError = client(
                true,
                networkTransport,
                publicValidator()
        ).selectContent(request());

        assertThat(clientError.failureCode()).isEqualTo(FailureCode.HTTP_STATUS_ERROR);
        assertThat(clientErrorTransport.callCount()).isEqualTo(1);
        assertThat(networkError.failureCode()).isEqualTo(FailureCode.NETWORK_ERROR);
        assertThat(networkTransport.callCount()).isEqualTo(1);
    }

    @Test
    void selectContentRejectsNonHtmlAndOversizedResponse() {
        StubTransport nonHtmlTransport = new StubTransport();
        nonHtmlTransport.enqueue(response(200, "application/pdf", new byte[]{1, 2, 3}));
        StubTransport oversizedTransport = new StubTransport();
        oversizedTransport.enqueue(response(200, "text/html", new byte[1025]));

        ProviderContentResult nonHtml = client(
                true,
                nonHtmlTransport,
                publicValidator()
        ).selectContent(request());
        ProviderContentResult oversized = client(
                true,
                1024,
                oversizedTransport,
                publicValidator()
        ).selectContent(request());

        assertThat(nonHtml.failureCode()).isEqualTo(FailureCode.CONTENT_TYPE_UNSUPPORTED);
        assertThat(oversized.failureCode()).isEqualTo(FailureCode.RESPONSE_TOO_LARGE);
    }

    @Test
    void selectContentDecodesMs949DeclaredByHtmlMeta() {
        Charset ms949 = Charset.forName("MS949");
        byte[] body = """
                <html><head><meta charset="MS949"></head>
                <body>소상공인 보조금 안내</body></html>
                """.getBytes(ms949);
        StubTransport transport = new StubTransport();
        transport.enqueue(response(200, "text/html", body));

        ProviderContentResult result = client(
                true,
                transport,
                publicValidator()
        ).selectContent(request());

        assertThat(result.statusCode()).isEqualTo(StatusCode.AVAILABLE);
        assertThat(result.bodyText()).isEqualTo("소상공인 보조금 안내");
    }

    @Test
    void selectContentEnforcesSizeLimitAfterGzipDecompression() {
        StubTransport transport = new StubTransport();
        transport.enqueue(gzipHtml("<html><body>" + "소상공인".repeat(1000) + "</body></html>"));

        ProviderContentResult result = client(
                true,
                1024,
                transport,
                publicValidator()
        ).selectContent(request());

        assertThat(result.failureCode()).isEqualTo(FailureCode.RESPONSE_TOO_LARGE);
        assertThat(transport.callCount()).isEqualTo(1);
    }

    @Test
    void selectContentLimitsSameHostConcurrencyToTwo() throws Exception {
        BlockingTransport transport = new BlockingTransport();
        LocalGovernmentNoticeProviderContentClient client = client(true, transport, publicValidator());
        ExecutorService executor = Executors.newFixedThreadPool(3);
        try {
            List<Future<ProviderContentResult>> futures = List.of(
                    executor.submit(() -> client.selectContent(request("101"))),
                    executor.submit(() -> client.selectContent(request("102"))),
                    executor.submit(() -> client.selectContent(request("103")))
            );

            assertThat(transport.awaitFirstTwo()).isTrue();
            assertThat(transport.callCount()).isEqualTo(2);
            assertThat(transport.maxActive()).isEqualTo(2);
            transport.release();

            for (Future<ProviderContentResult> future : futures) {
                assertThat(future.get(3, TimeUnit.SECONDS).statusCode()).isEqualTo(StatusCode.AVAILABLE);
            }
            assertThat(transport.callCount()).isEqualTo(3);
            assertThat(transport.maxActive()).isEqualTo(2);
        } finally {
            transport.release();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(3, TimeUnit.SECONDS)).isTrue();
        }
    }

    private LocalGovernmentNoticeProviderContentClient client(
            boolean enabled,
            ProviderContentHttpTransport transport,
            ProviderContentUrlValidator validator
    ) {
        return client(enabled, TWO_MEBIBYTES, transport, validator);
    }

    private LocalGovernmentNoticeProviderContentClient client(
            boolean enabled,
            int maxResponseBytes,
            ProviderContentHttpTransport transport,
            ProviderContentUrlValidator validator
    ) {
        return new LocalGovernmentNoticeProviderContentClient(
                enabled,
                Duration.ofSeconds(7),
                maxResponseBytes,
                3,
                2,
                "saneB-test-client/1.0",
                transport,
                validator
        );
    }

    private ProviderContentUrlValidator publicValidator() {
        return new ProviderContentUrlValidator(host -> new InetAddress[]{publicAddress()});
    }

    private ProviderContentRequest request() {
        return request("42");
    }

    private ProviderContentRequest request(String detailId) {
        return new ProviderContentRequest(
                "LOCAL_GOV_NOTICE",
                SOURCE_ID,
                REGISTERED_URL,
                "https://" + HOST + "/notices/" + detailId
        );
    }

    private static ProviderContentHttpResponse html(String html) {
        return response(200, "text/html; charset=UTF-8", html.getBytes(StandardCharsets.UTF_8));
    }

    private static ProviderContentHttpResponse redirect(String location) {
        return new ProviderContentHttpResponse(
                302,
                Map.of("Location", List.of(location)),
                new byte[0]
        );
    }

    private static ProviderContentHttpResponse response(int status, String contentType, byte[] body) {
        return new ProviderContentHttpResponse(
                status,
                Map.of("Content-Type", List.of(contentType)),
                body
        );
    }

    private static ProviderContentHttpResponse gzipHtml(String html) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try (GZIPOutputStream gzip = new GZIPOutputStream(output)) {
                gzip.write(html.getBytes(StandardCharsets.UTF_8));
            }
            return new ProviderContentHttpResponse(
                    200,
                    Map.of(
                            "Content-Type", List.of("text/html; charset=UTF-8"),
                            "Content-Encoding", List.of("gzip")
                    ),
                    output.toByteArray()
            );
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static InetAddress publicAddress() {
        return address(203, 0, 113, 10);
    }

    private static InetAddress address(int first, int second, int third, int fourth) {
        try {
            return InetAddress.getByAddress(new byte[]{
                    (byte) first,
                    (byte) second,
                    (byte) third,
                    (byte) fourth
            });
        } catch (UnknownHostException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static final class StubTransport implements ProviderContentHttpTransport {

        private final Deque<Object> outcomes = new ArrayDeque<>();
        private final List<ProviderContentRequestTarget> requestTargets = new ArrayList<>();
        private Duration lastReadTimeout;
        private int lastMaxResponseBytes;

        private void enqueue(Object outcome) {
            outcomes.addLast(outcome);
        }

        @Override
        public ProviderContentHttpResponse selectResponse(
                ProviderContentRequestTarget requestTarget,
                Duration readTimeout,
                int maxResponseBytes,
                String userAgent
        ) throws IOException, TimeoutException {
            requestTargets.add(requestTarget);
            lastReadTimeout = readTimeout;
            lastMaxResponseBytes = maxResponseBytes;
            Object outcome = outcomes.removeFirst();
            if (outcome instanceof IOException ioException) {
                throw ioException;
            }
            if (outcome instanceof TimeoutException timeoutException) {
                throw timeoutException;
            }
            return (ProviderContentHttpResponse) outcome;
        }

        private int callCount() {
            return requestTargets.size();
        }

        private List<URI> requestUris() {
            return requestTargets.stream().map(ProviderContentRequestTarget::uri).toList();
        }

        private List<InetAddress> lastPinnedAddresses() {
            return requestTargets.getLast().pinnedAddresses();
        }

        private Duration lastReadTimeout() {
            return lastReadTimeout;
        }

        private int lastMaxResponseBytes() {
            return lastMaxResponseBytes;
        }
    }

    private static final class BlockingTransport implements ProviderContentHttpTransport {

        private final AtomicInteger active = new AtomicInteger();
        private final AtomicInteger maxActive = new AtomicInteger();
        private final AtomicInteger callCount = new AtomicInteger();
        private final CountDownLatch firstTwo = new CountDownLatch(2);
        private final CountDownLatch release = new CountDownLatch(1);

        @Override
        public ProviderContentHttpResponse selectResponse(
                ProviderContentRequestTarget requestTarget,
                Duration readTimeout,
                int maxResponseBytes,
                String userAgent
        ) throws InterruptedException {
            int currentActive = active.incrementAndGet();
            maxActive.accumulateAndGet(currentActive, Math::max);
            callCount.incrementAndGet();
            firstTwo.countDown();
            try {
                release.await();
                return html("<html><body>소상공인 지원사업</body></html>");
            } finally {
                active.decrementAndGet();
            }
        }

        private boolean awaitFirstTwo() throws InterruptedException {
            return firstTwo.await(3, TimeUnit.SECONDS);
        }

        private void release() {
            release.countDown();
        }

        private int callCount() {
            return callCount.get();
        }

        private int maxActive() {
            return maxActive.get();
        }
    }
}
