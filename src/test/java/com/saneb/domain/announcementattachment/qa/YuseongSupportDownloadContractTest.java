package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.saneb.domain.announcementsource.classification.*;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.*;

class YuseongSupportDownloadContractTest {
    @Test void catalogKeepsTheFixedSourceReferenceUnapproved() throws Exception {
        var sample = YuseongSupportDownloadCases.selectCase();
        try (var input = getClass().getResourceAsStream("/announcement-attachment/provider-qa-catalog-v2.json")) {
            var catalog = new com.fasterxml.jackson.databind.ObjectMapper().readTree(input);
            var notices = java.util.stream.StreamSupport.stream(catalog.path("notices").spliterator(), false)
                    .filter(n -> sample.code().equals(n.path("caseCode").asText())).toList();
            assertThat(notices).hasSize(1);
            var reference = notices.getFirst();
            assertThat(reference.path("expectation").isNull()).isTrue();
            assertThat(reference.path("profileCode").asText()).isEqualTo(sample.profile().selectProfileCode());
            var source = reference.path("source");
            assertThat(source.path("sourceUrl").asText()).isEqualTo(sample.source().sourceUrl());
            assertThat(source.path("providerNoticeId").asText()).isEqualTo(sample.source().providerNoticeId());
            assertThat(source.path("localSourceCode").asText()).isEqualTo(sample.source().localSourceCode());
            assertThat(source.path("listParserProfileCode").asText()).isEqualTo(sample.source().listParserProfileCode());
        }
    }

    @Test void officialSupportSampleUsesExistingProfileAndTitleRules() throws Exception {
        var samples = AnnouncementAttachmentBbsOfficialObservationTest.selectBatchCases("YUSEONG_SUPPORT").toList();
        assertThat(samples).hasSize(1);
        var sample = samples.getFirst();
        assertThat(sample.code()).isEqualTo("YUSEONG-49380");
        assertThat(sample.listedFileCount()).isEqualTo(2);
        assertThat(sample.profile().selectDetailUri(sample.source()).toString()).isEqualTo(sample.source().sourceUrl());
        assertThat(sample.profile().selectProfileHash()).isEqualTo(DaejeonNextDownloadCases.selectCase("YUSEONG").profile().selectProfileHash());
        var decision = new AnnouncementSourceClassificationEngine().selectDecision(new AnnouncementSourceClassificationInput(
                "LOCAL_GOV_NOTICE", sample.title(), null, null, List.of(), BodySourceCode.NONE, BodyAvailabilityCode.UNAVAILABLE),
                AnnouncementAttachmentRealFileQaTest.selectDraftRuleSet());
        assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectTitleMayProceed(decision)).isTrue();
        assertThat(DaejeonNextDownloadCases.selectCase("YUSEONG").expectedTitleStopStage()).isEqualTo(TitleStageCode.COMBINATION_NOT_MATCHED);
        var budget = AnnouncementAttachmentBbsOfficialObservationTest.selectBudget(sample.profile(), true, false);
        assertThat(budget.maximumRequests).isEqualTo(7);
        assertThat(budget.maximumBytes).isEqualTo(26 * 1024 * 1024);
    }

    @Test void generalElderlyKeywordDoesNotImplyParentEligibility() throws Exception {
        var decision = new AnnouncementSourceClassificationEngine().selectDecision(new AnnouncementSourceClassificationInput(
                "LOCAL_GOV_NOTICE", "2026년 노인 일자리 및 사회활동 지원사업 참여자 모집 공고", null, null,
                List.of(), BodySourceCode.NONE, BodyAvailabilityCode.UNAVAILABLE), AnnouncementAttachmentRealFileQaTest.selectDraftRuleSet());
        assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectTitleMayProceed(decision)).isFalse();
    }
}
