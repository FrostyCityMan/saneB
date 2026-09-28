package com.saneb.domain.announcementattachment.discovery;

import static org.junit.jupiter.api.Assertions.*;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

/** 고정 표본/제목 구조만 오프라인 검증한다. 외부 HTTP·파일 추출은 없다. */
class JungguSupportReferenceContractTest {
    private final String title=SaeolAttachmentProfileLiveQaTest.JUNGGU_SUPPORT_TITLES.get("33626");
    private String html(String row) {
        return "<form name='form1' method='post'><table class='boardView'>"+row+"</table></form>";
    }
    private String row(String value) {return "<tr><th>제목</th><td>"+value+"</td></tr>";}
    private void validate(String value) {SaeolAttachmentProfileLiveQaTest.validateJungguTitle(Jsoup.parse(value),title);}

    @Test void exactThreeSourcesAndWholeFileCountsAreFixed() {
        var cases=SaeolAttachmentProfileLiveQaTest.selectJungguSupportCases().toList();
        assertEquals(java.util.List.of("34196","33626","33315"),cases.stream().map(SaeolGetAttachmentDiscoveryProfileTest.Case::noticeId).toList());
        assertEquals(java.util.List.of(1,2,1),cases.stream().map(SaeolGetAttachmentDiscoveryProfileTest.Case::fileCount).toList());
        for(var sample:cases) {
            assertEquals("LGS-000045",sample.sourceCode());
            assertEquals("LOCAL_DAEGU_JUNGGU_GET_V1",sample.profile().selectProfileCode());
            var source=SaeolGetAttachmentDiscoveryProfileTest.selectSource(sample,"https");
            assertTrue(sample.profile().selectApprovedRequest(sample.profile().selectDetailUri(source)));
            assertTrue(sample.profile().selectDetailUri(source).getQuery().contains("not_ancmt_mgt_no="+sample.noticeId()));
        }
    }
    @Test void matchingOfficialTitlePassesWithDisplayWhitespace() {validate(html(row(title.replace(" ","  "))));}
    @Test void changedTitleIsRejected() {assertThrows(AssertionError.class,()->validate(html(row("다른 공고"))));}
    @Test void duplicateFormsAreRejected() {assertThrows(AssertionError.class,()->validate(html(row(title))+html(row(title))));}
    @Test void duplicateTitleLabelsAreRejected() {assertThrows(AssertionError.class,()->validate(html(row(title)+row(title))));}
    @Test void nestedTitleCannotReplaceOfficialTitle() {assertThrows(AssertionError.class,()->validate(html("<tr><td><table>"+row(title)+"</table></td></tr>")));}
    @Test void nestedContentInTitleIsRejected() {assertThrows(AssertionError.class,()->validate(html(row("<table><tr><td>"+title+"</td></tr></table>"))));}
    @Test void differentBoardOrMethodIsRejected() {
        assertThrows(AssertionError.class,()->validate(html(row(title)).replace("boardView","bbsView")));
        assertThrows(AssertionError.class,()->validate(html(row(title)).replace("method='post'","method='get'")));
    }
    @Test void arbitraryExpectedTitleIsRejected() {
        assertThrows(AssertionError.class,()->SaeolAttachmentProfileLiveQaTest.validateJungguTitle(Jsoup.parse(html(row("임의 제목"))),"임의 제목"));
    }
}
