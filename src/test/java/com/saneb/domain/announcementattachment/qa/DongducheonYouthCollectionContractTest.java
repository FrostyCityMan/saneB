package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.domain.announcementsource.classification.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;

class DongducheonYouthCollectionContractTest {
    @Test void selectSeparateEligibleSampleWithoutChangingPreviousTitleStop() throws Exception {
        var selected=CapitalFourthDownloadCases.selectDongducheonYouthCase();
        assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectCases("DONGDUCHEON_YOUTH").map(c->c.code()).toList()).containsExactly("DONGDUCHEON-45339");
        assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectCases("DONGDUCHEON").map(c->c.code()).toList()).containsExactly("DONGDUCHEON-44784");
        var engine=new AnnouncementSourceClassificationEngine();
        for(var sample:List.of(selected,CapitalFourthDownloadCases.selectCase("DONGDUCHEON"))) {
            var decision=engine.selectDecision(new AnnouncementSourceClassificationInput("LOCAL_GOV_NOTICE",sample.title(),null,null,List.of(),
                    AnnouncementSourceClassificationCodes.BodySourceCode.NONE,AnnouncementSourceClassificationCodes.BodyAvailabilityCode.UNAVAILABLE),AnnouncementAttachmentRealFileQaTest.selectDraftRuleSet());
            assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectTitleMayProceed(decision)).isEqualTo(sample==selected);
        }
        assertThat(selected.profile().selectDetailUri(selected.source()).toString()).isEqualTo(selected.source().sourceUrl());
        var budget=new AnnouncementAttachmentBbsOfficialObservationTest.Budget(selected.profile(),false);
        assertThat(budget.maximumRequests).isEqualTo(6);assertThat(budget.maximumBytes).isEqualTo(23L*1024*1024);
    }
    @Test void selectReferenceOnlyCatalogWithExactIdentity() throws Exception {
        var mapper=new ObjectMapper();var selected=CapitalFourthDownloadCases.selectDongducheonYouthCase();
        var notices=mapper.readTree(Files.readString(Path.of("src/main/resources/announcement-attachment/provider-qa-catalog-v2.json"))).path("notices");
        var matched=StreamSupport.stream(notices.spliterator(),false).filter(n->selected.code().equals(n.path("caseCode").asText())).toList();
        assertThat(matched).hasSize(1);assertThat(matched.getFirst().path("source")).isEqualTo(mapper.valueToTree(selected.source()));
        assertThat(matched.getFirst().path("profileCode").asText()).isEqualTo(selected.profile().selectProfileCode());
        assertThat(matched.getFirst().hasNonNull("expectation")).isFalse();
    }
}
