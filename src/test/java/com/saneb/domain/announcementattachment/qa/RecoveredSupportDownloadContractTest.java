package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.domain.announcementsource.classification.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.StreamSupport;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RecoveredSupportDownloadContractTest {
    @ParameterizedTest @ValueSource(strings={"GANGNAM_SUPPORT","GEUMSAN_SUPPORT","GEOCHANG_SUPPORT","MICHUHOL_SUPPORT","ASAN_SUPPORT","CHEONAN_SUPPORT","DONGDUCHEON_SUPPORT","BUPYEONG_SUPPORT","NAMDONG_SUPPORT"})
    void newNoticeKeepsFailureHistoryAndExistingProfile(String group) {
        var sample=RecoveredSupportDownloadCases.selectCase(group);
        var old=group.equals("BUPYEONG_SUPPORT")?MetroRemainderDownloadCases.selectCase(true)
                :AnnouncementAttachmentBbsOfficialObservationTest.selectCases(group.replace("_SUPPORT", "")).toList().getFirst();
        assertThat(sample.code()).isNotEqualTo(old.code());
        assertThat(sample.source().providerNoticeId()).isNotEqualTo(old.source().providerNoticeId());
        assertThat(sample.profile().selectProfileHash()).isEqualTo(old.profile().selectProfileHash());
        assertThat(sample.source().localSourceCode()).isEqualTo(old.source().localSourceCode());
        assertThat(sample.listUrl()).isEqualTo(old.listUrl());
        assertThat(sample.profile().selectDetailUri(sample.source()).toString()).isEqualTo(sample.source().sourceUrl());
        assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectCases(group).map(c->c.code())).containsExactly(sample.code());
        assertThat(sample.listedFileCount()).isEqualTo(java.util.Set.of("GEOCHANG_SUPPORT","MICHUHOL_SUPPORT","CHEONAN_SUPPORT","DONGDUCHEON_SUPPORT").contains(group)?1:2);
    }

    @ParameterizedTest @ValueSource(strings={"GANGNAM_SUPPORT","GEUMSAN_SUPPORT","GEOCHANG_SUPPORT","MICHUHOL_SUPPORT","ASAN_SUPPORT","CHEONAN_SUPPORT","DONGDUCHEON_SUPPORT","BUPYEONG_SUPPORT","NAMDONG_SUPPORT"})
    void titlePolicyAndFiniteBudgetAreNotRelaxed(String group) throws Exception {
        var sample=RecoveredSupportDownloadCases.selectCase(group);
        var decision=new AnnouncementSourceClassificationEngine().selectDecision(new AnnouncementSourceClassificationInput(
                "LOCAL_GOV_NOTICE",sample.title(),null,null,List.of(),
                AnnouncementSourceClassificationCodes.BodySourceCode.NONE,AnnouncementSourceClassificationCodes.BodyAvailabilityCode.UNAVAILABLE),
                AnnouncementAttachmentRealFileQaTest.selectDraftRuleSet());
        assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectTitleMayProceed(decision)).isTrue();
        assertThat(decision.titleStageCode()).isEqualTo(group.equals("ASAN_SUPPORT")
                ?AnnouncementSourceClassificationCodes.TitleStageCode.GROUP_A_MATCHED
                :AnnouncementSourceClassificationCodes.TitleStageCode.COMBINATION_MATCHED);
        var budget=AnnouncementAttachmentBbsOfficialObservationTest.selectBudget(sample.profile(),true,false);
        assertThat(budget.maximumRequests).isBetween(1,10);
        assertThat(budget.maximumBytes).isLessThanOrEqualTo(44L*1024*1024);
    }

    @ParameterizedTest @ValueSource(strings={"GANGNAM_SUPPORT","GEUMSAN_SUPPORT","GEOCHANG_SUPPORT","MICHUHOL_SUPPORT","ASAN_SUPPORT","CHEONAN_SUPPORT","DONGDUCHEON_SUPPORT","BUPYEONG_SUPPORT","NAMDONG_SUPPORT"})
    void newCatalogReferenceCannotApprovePolicy(String group) throws Exception {
        var sample=RecoveredSupportDownloadCases.selectCase(group);
        var json=new ObjectMapper();
        var catalog=json.readTree(Files.readString(Path.of("src/main/resources/announcement-attachment/provider-qa-catalog-v2.json"))).path("notices");
        var refs=StreamSupport.stream(catalog.spliterator(),false).filter(n->sample.code().equals(n.path("caseCode").asText())).toList();
        assertThat(refs).hasSize(1);
        assertThat(refs.getFirst().path("source")).isEqualTo(json.valueToTree(sample.source()));
        assertThat(refs.getFirst().path("profileCode").asText()).isEqualTo(sample.profile().selectProfileCode());
        assertThat(refs.getFirst().hasNonNull("expectation")).isFalse();
    }
}
