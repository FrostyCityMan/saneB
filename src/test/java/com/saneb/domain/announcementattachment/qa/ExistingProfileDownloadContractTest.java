package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import com.saneb.domain.announcementsource.classification.*;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.*;

class ExistingProfileDownloadContractTest {
    @Test void batchUsesExistingBoundReferencesWithoutChangingTitlePolicy()throws Exception{
        var cases=AnnouncementAttachmentBbsOfficialObservationTest.selectBatchCases("EXISTING_FIRST").toList();
        assertThat(cases).extracting(c->c.code()).containsExactly("TAEBAEK-184816","JECHEON-403530","CHUNGJU-70852");
        for(var sample:cases){
            assertThat(sample.profile().selectDetailUri(sample.source())).isNotNull();
            assertThat(sample.expectedTitleStopStage()).isNull();
            var decision=new AnnouncementSourceClassificationEngine().selectDecision(new AnnouncementSourceClassificationInput(
                    "LOCAL_GOV_NOTICE",sample.title(),null,null,List.of(),BodySourceCode.NONE,BodyAvailabilityCode.UNAVAILABLE),AnnouncementAttachmentRealFileQaTest.selectDraftRuleSet());
            assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectTitleMayProceed(decision)).isTrue();
            var budget=AnnouncementAttachmentBbsOfficialObservationTest.selectBudget(sample.profile(),true,false);
            assertThat(budget.maximumRequests).isEqualTo(6);assertThat(budget.maximumBytes).isEqualTo(23*1024*1024);
            var full=AnnouncementAttachmentBbsOfficialObservationTest.selectBudget(sample.profile(),false,false);
            assertThat(full.maximumRequests).isEqualTo(44);assertThat(full.maximumBytes).isEqualTo(80*1024*1024);
        }
    }
    @Test void originalNegativeCasesRemainNegative()throws Exception{
        var cases=AnnouncementAttachmentBbsOfficialObservationTest.selectBatchCases("JECHEON,CHUNGJU").toList();
        var negativeCodes=Set.of("JECHEON-403587","CHUNGJU-72625","CHUNGJU-72039");
        var negatives=cases.stream().filter(c->negativeCodes.contains(c.code())).toList();
        assertThat(negatives).extracting(c->c.code()).containsExactlyInAnyOrderElementsOf(negativeCodes);
        for(var sample:negatives){
            var decision=new AnnouncementSourceClassificationEngine().selectDecision(new AnnouncementSourceClassificationInput(
                    "LOCAL_GOV_NOTICE",sample.title(),null,null,List.of(),BodySourceCode.NONE,BodyAvailabilityCode.UNAVAILABLE),AnnouncementAttachmentRealFileQaTest.selectDraftRuleSet());
            assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectPlannedTitleStop(sample,decision)).isTrue();
        }
    }
}
