package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AnnouncementAttachmentBbsOfficialObservationContractTest {
    @Test void currentDraftTitleMustPassBeforeAnyBodyOrAttachmentRequestAndBodyReviewDoesNotStopFiles() throws Exception {
        var rules=AnnouncementAttachmentRealFileQaTest.selectDraftRuleSet();
        var engine=new com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationEngine();
        var title=engine.selectDecision(new com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationInput("LOCAL_GOV_NOTICE",
                AnnouncementAttachmentBbsOfficialObservationTest.TITLE,null,null,List.of(),
                com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.BodySourceCode.NONE,
                com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.BodyAvailabilityCode.UNAVAILABLE),rules);
        assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectTitleMayProceed(title)).isTrue();
        var body=engine.selectDecision(new com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationInput("LOCAL_GOV_NOTICE",
                AnnouncementAttachmentBbsOfficialObservationTest.TITLE,"청년 지원 수출 지원",null,List.of(),
                com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.BodySourceCode.DETAIL_PAGE_TEXT,
                com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.BodyAvailabilityCode.AVAILABLE),rules);
        assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectTitleMayProceed(body)).isTrue();
        assertThat(body.semanticStatusCode()).isEqualTo(com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.SemanticStatusCode.REVIEW_REQUIRED);
        var blocked=engine.selectDecision(new com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationInput("LOCAL_GOV_NOTICE",
                "청년 수출 지원",null,null,List.of(),
                com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.BodySourceCode.NONE,
                com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.BodyAvailabilityCode.UNAVAILABLE),rules);
        assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectTitleMayProceed(blocked)).isFalse();
    }
    @Test void exactOfficialTitleIgnoresNestedLabelsButRejectsDifferentOrMissingIdentity() {
        var page=org.jsoup.Jsoup.parse("<table class='bbs_default view'><tr><th>제목</th><td>공식 지원 공고</td></tr><tr><td><table><tr><th>제목</th><td>중첩 표</td></tr></table></td></tr></table>");
        assertThatCode(()->AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,"공식 지원 공고")).doesNotThrowAnyException();
        assertThatThrownBy(()->AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,"다른 공고")).isInstanceOf(AssertionError.class);
        assertThatThrownBy(()->AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(org.jsoup.Jsoup.parse("<title>공식 지원 공고</title>"),"공식 지원 공고")).isInstanceOf(AssertionError.class);
    }
    @Test void bodyUpperBoundIsReservedBeforeFileBytesAndCannotBeReservedTwice() {
        var b=new AnnouncementAttachmentBbsOfficialObservationTest.Budget();b.reserveBody();assertThat(b.requests).isEqualTo(2);assertThat(b.bytes).isEqualTo(2*1024*1024);
        assertThatThrownBy(b::reserveBody).isInstanceOf(IllegalStateException.class);assertThat(b.saveBytes(78L*1024*1024)).isTrue();assertThat(b.saveBytes(1)).isFalse();assertThat(b.saveBytes(-1)).isFalse();
    }
    @Test void requestReservationsIncludeBodyAndNeverExceedWholeCaseLimit() {
        var b=new AnnouncementAttachmentBbsOfficialObservationTest.Budget();b.reserveBody();
        var request=com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request.selectGet(java.net.URI.create(
                "https://www.taebaek.go.kr/www/selectBbsNttView.do?key=352&bbsNo=25&nttNo=184816"));
        for(int i=0;i<42;i++)assertThat(b.selectRequestAllowed(request)).isTrue();assertThat(b.selectRequestAllowed(request)).isFalse();assertThat(b.requests).isEqualTo(44);
        assertThat(b.selectRequestAllowed(com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request.selectGet(java.net.URI.create("https://example.com/")))).isFalse();
        assertThat(b.requests).isEqualTo(44);
    }
    @Test void fileInputUsesOnlyExtractedRoleAndBlocksAndUnknownRemainsUnknown() throws Exception {
        var json=new ObjectMapper();String text="일반자료";var actual=json.valueToTree(Map.of("qualityCode","COMPLETE_TEXT","text",text,"blocks",List.of(Map.of(
                "index",0,"startOffset",0,"endOffset",4,"evidenceScopeId","p1","scopeReliable",true,"locator","page:1"))));
        var observed=json.valueToTree(AnnouncementAttachmentOfficialObservationTest.selectTextObservation(actual));
        var file=AnnouncementAttachmentBbsOfficialObservationTest.selectFileInput(actual,observed);assertThat(file.role()).isEqualTo("UNKNOWN");assertThat(file.text()).isEqualTo(text);assertThat(file.blocks()).hasSize(1);
        assertThat(file.blocks().getFirst().endOffset()).isEqualTo(4);
    }
}
