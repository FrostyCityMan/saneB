package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AnnouncementAttachmentOfficialObservationContractTest {
    private static final ObjectMapper JSON = new ObjectMapper();

    @Test void completeActualTextRetainsRoleAndLocationProofWithoutRawDocumentContent() throws Exception {
        String text="소상공인 지원 공고\n지원대상\n지원내용\n신청기간\nPRIVATE_CANARY_DO_NOT_COPY";
        var input=JSON.valueToTree(Map.of("qualityCode","COMPLETE_TEXT","text",text,"blocks",List.of(
                Map.of("index",0,"startOffset",0,"endOffset",text.length(),"evidenceScopeId","p1","scopeReliable",true,"locator","page:1"))));
        var actual=AnnouncementAttachmentOfficialObservationTest.selectTextObservation(input);
        var output=JSON.valueToTree(actual);
        assertThat(output.path("roleAssessment").path("roleCode").asText()).isEqualTo("NOTICE");
        assertThat(output.path("roleAssessment").path("evidence").size()).isEqualTo(4);
        assertThat(output.path("roleAssessmentHash").asText()).matches("[0-9a-f]{64}");
        assertThat(output.toString()).doesNotContain("PRIVATE_CANARY","소상공인","지원대상","page:1","isPolicyQaPassed","isExpectationApproved");
        assertThat(output.path("textHash").asText()).isEqualTo(output.path("roleAssessment").path("textHash").asText());
    }
    @Test void incompleteExtractionCannotAcquireRoleEvidence() throws Exception {
        var result=AnnouncementAttachmentOfficialObservationTest.selectTextObservation(JSON.valueToTree(
                Map.of("qualityCode","PARTIAL_TEXT","text","소상공인 지원 공고","blocks",List.of())));
        assertThat(result).doesNotContainKeys("roleAssessment","roleAssessmentHash");
        assertThat(result).containsKeys("textHash","characterCount","blockCount");
    }
    @Test void unknownRoleRemainsUnknownAndBrokenLocationProofFails() throws Exception {
        var input=JSON.valueToTree(Map.of("qualityCode","COMPLETE_TEXT","text","일반자료","blocks",List.of(
                Map.of("index",0,"startOffset",0,"endOffset",4,"evidenceScopeId","p1","scopeReliable",true,"locator","page:1"))));
        var output=JSON.valueToTree(AnnouncementAttachmentOfficialObservationTest.selectTextObservation(input));
        assertThat(output.path("roleAssessment").path("roleCode").asText()).isEqualTo("UNKNOWN");
        ((com.fasterxml.jackson.databind.node.ObjectNode)input.path("blocks").get(0)).put("endOffset",3);
        assertThatThrownBy(()->AnnouncementAttachmentOfficialObservationTest.selectTextObservation(input)).isInstanceOf(IllegalArgumentException.class);
    }
}
