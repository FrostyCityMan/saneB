package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AnnouncementAttachmentOfficialObservationContractTest {
    private static final ObjectMapper JSON = new ObjectMapper();

    @Test void diagnosticFailureCodePreservesOnlyKnownCodesAndNeverExternalErrorText() {
        assertThat(AnnouncementAttachmentOfficialObservationTest.selectFailureCode(new java.io.IOException("ATTACHMENT_DNS_TIMEOUT")))
                .isEqualTo("ATTACHMENT_DNS_TIMEOUT");
        assertThat(AnnouncementAttachmentOfficialObservationTest.selectFailureCode(new java.io.IOException("ATTACHMENT_DNS_LOOKUP_FAILED")))
                .isEqualTo("ATTACHMENT_DNS_LOOKUP_FAILED");
        assertThat(AnnouncementAttachmentOfficialObservationTest.selectFailureCode(new java.io.IOException("ATTACHMENT_TOTAL_TIMEOUT")))
                .isEqualTo("ATTACHMENT_TOTAL_TIMEOUT");
        assertThat(AnnouncementAttachmentOfficialObservationTest.selectFailureCode(new java.io.IOException("ATTACHMENT_HTTP_503")))
                .isEqualTo("ATTACHMENT_HTTP_503");
        assertThat(AnnouncementAttachmentOfficialObservationTest.selectFailureCode(new javax.net.ssl.SSLHandshakeException("PRIVATE_CANARY")))
                .isEqualTo("TLS_FAILED");
        assertThat(AnnouncementAttachmentOfficialObservationTest.selectFailureCode(new java.net.SocketTimeoutException("PRIVATE_CANARY")))
                .isEqualTo("TRANSPORT_TIMEOUT");
        assertThat(AnnouncementAttachmentOfficialObservationTest.selectFailureCode(new java.net.UnknownHostException("PRIVATE_CANARY")))
                .isEqualTo("DNS_FAILED");
        for (String message : List.of("ATTACHMENT_HTTP_503 PRIVATE_CANARY", "ATTACHMENT_DNS_TIMEOUT PRIVATE_CANARY", "ATTACHMENT_DNS_LOOKUP_FAILED PRIVATE_CANARY", "PRIVATE_CANARY"))
            assertThat(AnnouncementAttachmentOfficialObservationTest.selectFailureCode(new java.io.IOException(message)))
                    .isEqualTo("TRANSPORT_FAILED");
        assertThat(AnnouncementAttachmentOfficialObservationTest.selectFailureCode(new AssertionError("PRIVATE_CANARY")))
                .isEqualTo("OBSERVATION_ASSERTION_FAILED");
        assertThat(AnnouncementAttachmentOfficialObservationTest.selectFailureCode(new IllegalArgumentException("PRIVATE_CANARY")))
                .isEqualTo("OBSERVATION_FAILED");
    }

    @Test void officialEmptyTemplateTitleIsIgnoredButMissingConflictingOrChangedContentTitlesFail() {
        var valid=org.jsoup.Jsoup.parse("<meta property='og:title' content='소상공인 지원 공고'><meta property='og:title' content=''>");
        assertThatCode(()->AnnouncementAttachmentOfficialObservationTest.validateOfficialTitle(valid,"소상공인 지원 공고")).doesNotThrowAnyException();
        for(String html:List.of("<meta property='og:title' content=''>",
                "<meta property='og:title' content='소상공인 지원 공고'><meta property='og:title' content='다른 공고'>",
                "<meta property='og:title' content='변경된 공고'>")) {
            assertThatThrownBy(()->AnnouncementAttachmentOfficialObservationTest.validateOfficialTitle(org.jsoup.Jsoup.parse(html),"소상공인 지원 공고"))
                    .isInstanceOf(AssertionError.class);
        }
    }

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
        assertThat(result).doesNotContainKeys("roleAssessment","roleAssessmentHash","roleStructureObservation");
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

    @Test void structureSignalsKeepCurrentFormAssessmentWithoutCopyingText() throws Exception {
        String text = "😀 참여 신청서\n성명\nPRIVATE_PERSON_CANARY\n사업자등록번호\n(인)";
        var blocks = new java.util.ArrayList<Map<String,Object>>();
        int offset = 0;
        for (String line : text.split("\n")) {
            int end = offset + line.codePointCount(0,line.length());
            blocks.add(Map.of("index",blocks.size(),"startOffset",offset,"endOffset",end,"evidenceScopeId","cell-"+blocks.size(),
                    "scopeReliable",true,"locator","private-locator-"+blocks.size()));
            offset = end+1;
        }
        var output=JSON.valueToTree(AnnouncementAttachmentOfficialObservationTest.selectTextObservation(JSON.valueToTree(
                Map.of("qualityCode","COMPLETE_TEXT","text",text,"blocks",blocks))));
        assertThat(output.path("roleAssessment").path("roleCode").asText()).isEqualTo("FORM");
        assertThat(output.path("roleAssessment").path("ruleVersion").asText()).isEqualTo("document-role-1.0.2");
        var observation=output.path("roleStructureObservation");
        assertThat(observation.path("isTruncated").asBoolean()).isFalse();
        assertThat(observation.path("signals").size()).isEqualTo(4);
        var field=observation.path("signals").get(1);
        assertThat(field.path("code").asText()).isEqualTo("APPLICANT_LABEL");
        assertThat(field.path("tokenIndexes").toString()).isEqualTo("[1]");
        assertThat(field.path("isWholeLineToken").asBoolean()).isTrue();
        assertThat(field.path("hasColon").asBoolean()).isFalse();
        assertThat(field.path("blockIndex").asInt()).isEqualTo(1);
        assertThat(field.path("startOffset").asInt()).isEqualTo("😀 참여 신청서\n".codePointCount(0,"😀 참여 신청서\n".length()));
        assertThat(output.toString()).doesNotContain("PRIVATE_PERSON_CANARY","참여","성명","사업자등록번호","private-locator","cell-","(인)");
    }

    @Test void boundedStructureObservationMarksTruncationWithoutDiscardingMatchCount() throws Exception {
        String text="지원대상\n".repeat(200);
        var input=JSON.valueToTree(Map.of("qualityCode","COMPLETE_TEXT","text",text,"blocks",List.of(
                Map.of("index",0,"startOffset",0,"endOffset",text.length(),"evidenceScopeId","p1","scopeReliable",true,"locator","page:1"))));
        var output=JSON.valueToTree(AnnouncementAttachmentOfficialObservationTest.selectTextObservation(input));
        var observation=output.path("roleStructureObservation");
        assertThat(observation.path("signalMatchCount").asInt()).isEqualTo(200);
        assertThat(observation.path("signals").size()).isEqualTo(128);
        assertThat(observation.path("isTruncated").asBoolean()).isTrue();
        assertThat(output.path("roleAssessment").path("roleCode").asText()).isEqualTo("UNKNOWN");
    }

    @Test void observationStopsAtLineLimitAndDoesNotTreatItsPartialCountAsComplete() throws Exception {
        String text="자료\n".repeat(20_000)+"지원대상";
        var output=JSON.valueToTree(AnnouncementAttachmentOfficialObservationTest.selectTextObservation(JSON.valueToTree(
                Map.of("qualityCode","COMPLETE_TEXT","text",text,"blocks",List.of(
                        Map.of("index",0,"startOffset",0,"endOffset",text.length(),"evidenceScopeId","p1","scopeReliable",true,"locator","page:1"))))));
        var observation=output.path("roleStructureObservation");
        assertThat(observation.path("inspectedNonblankLineCount").asInt()).isEqualTo(20_000);
        assertThat(observation.path("isLineLimitReached").asBoolean()).isTrue();
        assertThat(observation.path("isTruncated").asBoolean()).isTrue();
        assertThat(observation.path("signalMatchCount").asInt()).isZero();
        assertThat(output.path("roleAssessment").path("reasonCode").asText()).isEqualTo("ROLE_ANALYSIS_LIMIT");
    }

    @Test void crossBlockOrUnreliableSignalsCannotClaimReliableSingleBlockEvidence() throws Exception {
        String text="지원대상\n성명";
        var output=JSON.valueToTree(AnnouncementAttachmentOfficialObservationTest.selectTextObservation(JSON.valueToTree(
                Map.of("qualityCode","COMPLETE_TEXT","text",text,"blocks",List.of(
                        Map.of("index",0,"startOffset",0,"endOffset",2,"evidenceScopeId","a","scopeReliable",true,"locator","a"),
                        Map.of("index",1,"startOffset",2,"endOffset",4,"evidenceScopeId","b","scopeReliable",true,"locator","b"),
                        Map.of("index",2,"startOffset",5,"endOffset",7,"evidenceScopeId","c","scopeReliable",false,"locator","c"))))));
        var signals=output.path("roleStructureObservation").path("signals");
        assertThat(signals.get(0).path("blockIndex").asInt()).isEqualTo(-1);
        assertThat(signals.get(1).path("blockIndex").asInt()).isEqualTo(2);
        assertThat(signals).allMatch(signal -> !signal.path("isSingleReliableBlock").asBoolean());
        assertThat(output.path("roleAssessment").path("roleCode").asText()).isEqualTo("UNKNOWN");
    }
}
