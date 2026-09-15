package com.saneb.db;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class AnnouncementAttachmentOfficialWorkerProbeTest {
    @Test void taebaekIsAnExplicitSingleNoticeGroupAndCannotReplaceYangpyeongCoverage() {
        assertEquals(java.util.List.of("TAEBAEK-184816"),AnnouncementAttachmentOfficialWorkerProbe.selectCaseCodes("TAEBAEK"));
        assertTrue(AnnouncementAttachmentOfficialWorkerProbe.selectComplete("TAEBAEK",1,1,0,0,0,0));
        assertFalse(AnnouncementAttachmentOfficialWorkerProbe.selectComplete("TAEBAEK",1,0,0,1,0,0));
        assertFalse(AnnouncementAttachmentOfficialWorkerProbe.selectComplete("TAEBAEK",3,3,0,0,0,0));
        assertFalse(AnnouncementAttachmentOfficialWorkerProbe.selectComplete("YANGPYEONG",1,1,0,0,0,0));
        assertEquals(java.util.List.of("TAEBAEK-184816"),
                com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.selectCases("TAEBAEK").map(c->c.code()).toList());
        assertThrows(IllegalArgumentException.class,()->AnnouncementAttachmentOfficialWorkerProbe.selectCaseCodes("ALL"));
        assertThrows(IllegalArgumentException.class,()->AnnouncementAttachmentOfficialWorkerProbe.selectCaseCodes("https://example.invalid"));
    }
    @Test void reportsOnlyAllowlistedRoleCodesAndCounts() throws Exception {
        var evidence=java.util.List.of(
                new com.saneb.domain.announcementattachment.classification.AttachmentDocumentRoleClassifier.Evidence("NOTICE_HEADING",0,0,10),
                new com.saneb.domain.announcementattachment.classification.AttachmentDocumentRoleClassifier.Evidence("FORM_HEADING",10,100,110));
        var assessment=new com.saneb.domain.announcementattachment.classification.AttachmentDocumentRoleClassifier.Assessment(
                "private fixture","private fixture","private fixture","private fixture","UNKNOWN","MIXED_DOCUMENT_ROLES",evidence);
        var result=AnnouncementAttachmentOfficialWorkerIntegrationTest.selectRoleDiagnostic(assessment);
        assertEquals(java.util.Set.of("assessmentPresent","reasonCode","matchedRuleCodes","evidenceCount","evidenceBlockCount"),result.keySet());
        assertEquals("MIXED_DOCUMENT_ROLES",result.get("reasonCode"));
        assertEquals(java.util.List.of("FORM_HEADING","NOTICE_HEADING"),result.get("matchedRuleCodes"));
        assertEquals(2,result.get("evidenceCount"));assertEquals(2L,result.get("evidenceBlockCount"));
        assertFalse(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(result).contains("private fixture"));
        assertEquals(java.util.Map.of("assessmentPresent",false),AnnouncementAttachmentOfficialWorkerIntegrationTest.selectRoleDiagnostic(null));
    }
    @Test void rejectsUnexpectedRoleDiagnosticCodesWithoutEchoingThem() {
        var assessment=new com.saneb.domain.announcementattachment.classification.AttachmentDocumentRoleClassifier.Assessment(
                "v","h","t","b","UNKNOWN","private fixture",java.util.List.of());
        var failure=assertThrows(IllegalArgumentException.class,()->AnnouncementAttachmentOfficialWorkerIntegrationTest.selectRoleDiagnostic(assessment));
        assertEquals("ROLE_DIAGNOSTIC_CODE_INVALID",failure.getMessage());
    }
    @Test void requiresAllThreeWithoutSkippedOrFailedContainers() {
        assertTrue(AnnouncementAttachmentOfficialWorkerProbe.selectComplete(3,3,0,0,0,0));
        assertFalse(AnnouncementAttachmentOfficialWorkerProbe.selectComplete(2,2,0,0,0,0));
        assertFalse(AnnouncementAttachmentOfficialWorkerProbe.selectComplete(4,4,0,0,0,0));
        assertFalse(AnnouncementAttachmentOfficialWorkerProbe.selectComplete(3,2,1,0,0,0));
        assertFalse(AnnouncementAttachmentOfficialWorkerProbe.selectComplete(3,2,0,1,0,0));
        assertFalse(AnnouncementAttachmentOfficialWorkerProbe.selectComplete(3,2,0,0,1,0));
        assertFalse(AnnouncementAttachmentOfficialWorkerProbe.selectComplete(3,3,0,0,0,1));
    }
    @Test void includesExcludedTitleInDenominator() {
        assertEquals(AnnouncementAttachmentOfficialWorkerProbe.CASES,
                AnnouncementAttachmentOfficialWorkerIntegrationTest.selectCases().map(c->c.code()).toList());
    }
    @Test void diagnosticTraceDoesNotExposeExceptionMessages() throws Exception {
        var failure=new ExceptionInInitializerError(new IllegalStateException("private fixture text"));
        String output=new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(
                AnnouncementAttachmentOfficialWorkerProbe.selectFailureTrace(failure));
        assertFalse(output.contains("private fixture text"));
        assertTrue(output.contains("IllegalStateException"));
        assertTrue(output.contains("ExceptionInInitializerError"));
    }
    @Test void comparesWireNumbersWithoutDroppingFieldsOrValueChecks() throws Exception {
        var mapper=new com.fasterxml.jackson.databind.ObjectMapper();
        var dto=java.util.Map.of("downloadedBytes",116740L,"characterCount",11398);
        var wire=mapper.readTree("{\"downloadedBytes\":116740,\"characterCount\":11398}");
        assertNotEquals(mapper.valueToTree(dto),wire); // 기존 공식 시험의 false negative 재현
        assertEquals(AnnouncementAttachmentOfficialWorkerIntegrationTest.selectWireTree(dto),wire);
        assertNotEquals(AnnouncementAttachmentOfficialWorkerIntegrationTest.selectWireTree(dto),
                mapper.readTree("{\"downloadedBytes\":116741,\"characterCount\":11398}"));
        assertNotEquals(AnnouncementAttachmentOfficialWorkerIntegrationTest.selectWireTree(dto),
                mapper.readTree("{\"downloadedBytes\":116740}"));
    }
}
