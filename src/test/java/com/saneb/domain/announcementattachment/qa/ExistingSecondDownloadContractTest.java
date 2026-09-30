package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.saneb.domain.announcementsource.classification.*;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.*;

class ExistingSecondDownloadContractTest {
    @Test void fixedReferencesUseExistingProfilesAndUnchangedTitleRules()throws Exception {
        var cases=AnnouncementAttachmentBbsOfficialObservationTest.selectBatchCases("EXISTING_SECOND").toList();
        assertThat(cases).extracting(c->c.code()).containsExactly("WONJU-482226","DAEJEON_SEOGU-49944");
        for(var sample:cases){
            assertThat(sample.profile().selectDetailUri(sample.source())).isNotNull();
            var decision=new AnnouncementSourceClassificationEngine().selectDecision(new AnnouncementSourceClassificationInput(
                    "LOCAL_GOV_NOTICE",sample.title(),null,null,List.of(),BodySourceCode.NONE,BodyAvailabilityCode.UNAVAILABLE),AnnouncementAttachmentRealFileQaTest.selectDraftRuleSet());
            assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectTitleMayProceed(decision)).as(sample.code()).isTrue();
            var budget=AnnouncementAttachmentBbsOfficialObservationTest.selectBudget(sample.profile(),true,false);
            assertThat(budget.maximumRequests).isEqualTo(6);
            assertThat(budget.maximumBytes).isEqualTo(23*1024*1024);
        }
    }
    @Test void unseededPregnancyTargetIsNotSilentlyPromoted()throws Exception {
        var decision=new AnnouncementSourceClassificationEngine().selectDecision(new AnnouncementSourceClassificationInput(
                "LOCAL_GOV_NOTICE","2026년 임산부 친환경농산물 지원사업 추가 신청 안내 공고",null,null,List.of(),BodySourceCode.NONE,BodyAvailabilityCode.UNAVAILABLE),AnnouncementAttachmentRealFileQaTest.selectDraftRuleSet());
        assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectTitleMayProceed(decision)).isFalse();
    }
    @Test void titleChecksStayInsideTheOfficialContainer(){
        var cases=ExistingSecondDownloadCases.selectCases().toList();
        var wonju=cases.getFirst();var seogu=cases.getLast();
        String w="<div class='bbs_wrap'><div class='p-wrap bbs bbs__view'><table class='p-table'><tr><th>제목</th><td>"+wonju.title()+"</td></tr></table></div></div>";
        String s="<div class='card program--view'><span id='notAncmtSj'>"+seogu.title()+"</span></div>";
        AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(org.jsoup.Jsoup.parse(w),wonju.title(),wonju.titleLayout());
        AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(org.jsoup.Jsoup.parse(s),seogu.title(),seogu.titleLayout());
        assertThatThrownBy(()->AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(org.jsoup.Jsoup.parse(w+w),wonju.title(),wonju.titleLayout())).isInstanceOf(AssertionError.class);
        assertThatThrownBy(()->AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(org.jsoup.Jsoup.parse(s.replace("program--view","menu")),seogu.title(),seogu.titleLayout())).isInstanceOf(AssertionError.class);
    }
}
