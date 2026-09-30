package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.domain.announcementsource.classification.*;
import static com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.*;
import java.nio.file.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

class AnnouncementAttachmentJungguObservationContractTest {
    @Test void singlePdfCampaignKeepsWholeTwoFileNoticeWithinRemainingBudget() {
        var samples=AnnouncementAttachmentBbsOfficialObservationTest.selectCases("JUNGGU_PDF").toList();
        assertThat(samples).hasSize(1);assertThat(samples.getFirst().code()).isEqualTo("JUNGGU-33626");
        assertThat(samples.getFirst().listedFileCount()).isEqualTo(2);
        assertThat(samples.getFirst().expectedTitleStopStage()).isNull();
        var budget=new AnnouncementAttachmentBbsOfficialObservationTest.Budget(samples.getFirst().profile(),false);
        assertThat(budget.maximumRequests).isEqualTo(6);assertThat(budget.maximumBytes).isEqualTo(24117248);
        assertThat(22+budget.maximumRequests).isLessThanOrEqualTo(30);
        assertThat(11611889L+budget.maximumBytes).isLessThanOrEqualTo(81788928L);
    }
    @Test void fixedIdentityWholeFileCountsAndNegativeSampleMatchTheCatalog() throws Exception {
        var samples=AnnouncementAttachmentBbsOfficialObservationTest.selectCases("JUNGGU").toList();
        assertThat(samples).extracting(AnnouncementAttachmentBbsOfficialObservationTest.ObservationCase::code)
                .containsExactly("JUNGGU-34196","JUNGGU-33626","JUNGGU-33315");
        assertThat(samples).extracting(AnnouncementAttachmentBbsOfficialObservationTest.ObservationCase::listedFileCount).containsExactly(1,2,1);
        var json=new ObjectMapper();var catalog=json.readTree(Files.readString(Path.of("src/main/resources/announcement-attachment/provider-qa-catalog-v2.json"))).path("notices");
        for(var sample:samples) {
            var reference=java.util.stream.StreamSupport.stream(catalog.spliterator(),false).filter(n->sample.code().equals(n.path("caseCode").asText())).findFirst().orElseThrow();
            assertThat(reference.path("source")).isEqualTo(json.valueToTree(sample.source()));
            assertThat(reference.hasNonNull("expectation")).isFalse();
            assertThat(sample.profile().selectProfileCode()).isEqualTo("LOCAL_DAEGU_JUNGGU_GET_V1");
            // 공통 Request 변경 후 세 고정 공고를 재관측한 지문. 정책 expectation은 계속 null이다.
            assertThat(sample.profile().selectProfileHash()).isEqualTo("20bb0d35ea1faba2cc77dd58a5e7e62156640e9846c2737d2b5112efcccbbbcc");
            assertThat(sample.titleLayout()).isEqualTo(AnnouncementAttachmentBbsOfficialObservationTest.TitleLayout.JUNGGU_LABEL);
            assertThat(sample.expectedTitleStopStage()).isEqualTo(sample.code().endsWith("33315")?TitleStageCode.COMBINATION_NOT_MATCHED:null);
        }
    }
    @Test @EnabledOnOs(OS.LINUX)
    void freshMigrationSeedPermitsTwoTitlesAndStopsTheThirdBeforeBodyAndFiles() throws Exception {
        var rules=AnnouncementAttachmentRealFileQaTest.selectDraftRuleSet();
        var engine=new AnnouncementSourceClassificationEngine();
        for(var sample:AnnouncementAttachmentBbsOfficialObservationTest.selectCases("JUNGGU").toList()) {
            var result=engine.selectDecision(new AnnouncementSourceClassificationInput("LOCAL_GOV_NOTICE",sample.title(),null,null,List.of(),BodySourceCode.NONE,BodyAvailabilityCode.UNAVAILABLE),rules);
            boolean stopped=sample.code().endsWith("33315");
            assertThat(result.titleStageCode()).isEqualTo(stopped?TitleStageCode.COMBINATION_NOT_MATCHED:TitleStageCode.COMBINATION_MATCHED);
            assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectPlannedTitleStop(sample,result)).isEqualTo(stopped);
            assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectTitleMayProceed(result)).isEqualTo(!stopped);
        }
    }
    @Test void eachCaseHasFixedBudgetAndTheCampaignRetainsPriorUsage() {
        for(var sample:AnnouncementAttachmentBbsOfficialObservationTest.selectCases("JUNGGU").toList()) {
            for(boolean diagnostic:List.of(false,true)) {
                var budget=new AnnouncementAttachmentBbsOfficialObservationTest.Budget(sample.profile(),diagnostic);
                assertThat(budget.maximumRequests).isEqualTo(6);assertThat(budget.maximumBytes).isEqualTo(24117248);
                assertThat(budget.requests).isZero();assertThat(budget.bytes).isZero();
                budget.reserveBody();assertThat(budget.requests).isEqualTo(2);
                assertThatThrownBy(budget::reserveBody).isInstanceOf(IllegalStateException.class);
                var request=com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request.selectGet(sample.profile().selectDetailUri(sample.source()));
                for(int i=0;i<4;i++)assertThat(budget.selectRequestAllowed(request)).isTrue();
                assertThat(budget.selectRequestAllowed(request)).isFalse();
                assertThat(budget.saveBytes(21L*1024*1024)).isTrue();assertThat(budget.saveBytes(1)).isFalse();
            }
        }
        // 음성 표본은 판정 변화 시에도 reserveBody 이전에 실패하므로 추가 요청 상한은2건만 합산한다.
        assertThat(13+12).isLessThanOrEqualTo(30);
        assertThat(6908001L+2*24117248L).isLessThanOrEqualTo(78L*1024*1024);
    }
    @Test void officialTitleCannotComeFromAnotherFormNestedTableOrChangedNotice() {
        var sample=AnnouncementAttachmentBbsOfficialObservationTest.selectCases("JUNGGU").findFirst().orElseThrow();
        String table="<table class='boardView'><tr><th>제목</th><td>"+sample.title()+"</td></tr></table>";
        String html="<form name='form1' method='post'>"+table+"</form>";
        java.util.function.Consumer<String> check=value->AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(org.jsoup.Jsoup.parse(value),sample.title(),sample.titleLayout());
        assertThatCode(()->check.accept(html)).doesNotThrowAnyException();
        for(String invalid:List.of(html+html,html.replace("boardView","other"),html.replace("method='post'","method='get'"),
                html.replace("제목","내용"),html.replace(sample.title(),"다른 공고"),html.replace("</form>",table+"</form>"),
                html.replace(sample.title(),"<table><tr><td>"+sample.title()+"</td></tr></table>"))) {
            assertThatThrownBy(()->check.accept(invalid)).isInstanceOf(AssertionError.class);
        }
    }
}
