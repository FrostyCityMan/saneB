package com.saneb.db;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class AnnouncementAttachmentOfficialWorkerProbeTest {
    @Test void replacementCharacterDiagnosticCountsOnlyLossMarkersAndPreservesMissingText() throws Exception {
        assertNull(AnnouncementAttachmentOfficialWorkerIntegrationTest.selectReplacementCharacterCount(null));
        assertEquals(0L,AnnouncementAttachmentOfficialWorkerIntegrationTest.selectReplacementCharacterCount(""));
        assertEquals(0L,AnnouncementAttachmentOfficialWorkerIntegrationTest.selectReplacementCharacterCount("한글 😀 정상"));
        var count=AnnouncementAttachmentOfficialWorkerIntegrationTest.selectReplacementCharacterCount("private fixture \ufffd 😀 \ufffd");
        assertEquals(2L,count);
        var output=new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(java.util.Map.of("replacementCharacterCount",count));
        assertEquals("{\"replacementCharacterCount\":2}",output);
    }
    @Test void hwpNoticeUsesExistingTaebaekProfileAndAnIndependentSingleCaseDenominator() {
        var sample=com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.selectCases("TAEBAEK_HWP").toList();
        assertEquals(1,sample.size());assertEquals("TAEBAEK-176153",sample.getFirst().code());assertEquals(1,sample.getFirst().listedFileCount());
        assertEquals("LOCAL_TAEBAEK_BBS_V1",sample.getFirst().profile().selectProfileCode());
        assertTrue(sample.getFirst().profile().selectApprovedRequest(sample.getFirst().profile().selectDetailUri(sample.getFirst().source())));
        assertEquals(java.util.List.of("TAEBAEK-176153"),AnnouncementAttachmentOfficialWorkerProbe.selectCaseCodes("TAEBAEK_HWP"));
        assertTrue(AnnouncementAttachmentOfficialWorkerProbe.selectComplete("TAEBAEK_HWP",1,1,0,0,0,0));
        assertFalse(AnnouncementAttachmentOfficialWorkerProbe.selectComplete("TAEBAEK_HWP",1,0,0,1,0,0));
        assertFalse(AnnouncementAttachmentOfficialWorkerProbe.selectComplete("TAEBAEK_HWP",2,2,0,0,0,0));
    }
    @Test void fixedCaseSharesBoundedRequestsAndIdempotentResourceOwnership() throws Exception {
        var nano=new java.util.concurrent.atomic.AtomicLong();
        var control=new AnnouncementAttachmentOfficialWorkerIntegrationTest.FixedCaseControl(nano::get);
        assertNull(control.selectDownloadPermit("invalid"));var first=control.selectExtractionPermit();assertNotNull(first);
        assertNull(control.selectExtractionPermit());first.close();var second=control.selectDownloadPermit("a".repeat(64));assertNotNull(second);
        first.close();assertTrue(control.inUse.get());second.close();assertFalse(control.inUse.get());
        for(int i=0;i<44;i++)assertTrue(control.saveRequestReservation());assertFalse(control.saveRequestReservation());
        assertFalse(control.saveByteReservation(0));assertFalse(control.saveByteReservation(-1));assertFalse(control.saveByteReservation(Long.MAX_VALUE));
        assertTrue(control.saveByteReservation(83886080));assertFalse(control.saveByteReservation(1));
        nano.set(420_000_000_000L);assertFalse(control.selectExecutionAllowed());assertNull(control.selectExtractionPermit());
    }
    @Test void interruptedFixedCaseCannotReserveOrAcquireResources() {
        var control=new AnnouncementAttachmentOfficialWorkerIntegrationTest.FixedCaseControl(System::nanoTime);
        Thread.currentThread().interrupt();
        try {assertFalse(control.selectExecutionAllowed());assertFalse(control.saveRequestReservation());assertFalse(control.saveByteReservation(1));assertNull(control.selectExtractionPermit());}
        finally {Thread.interrupted();}
        assertEquals(0,control.requests);assertEquals(0,control.bytes);
    }
    @Test void reviewFingerprintUsesCanonicalRoleEvidenceWithoutCopyingOriginalText() throws Exception {
        var role=new com.saneb.domain.announcementattachment.classification.AttachmentDocumentRoleClassifier.Assessment(
                com.saneb.domain.announcementattachment.classification.AttachmentDocumentRoleClassifier.VERSION,
                com.saneb.domain.announcementattachment.classification.AttachmentDocumentRoleClassifier.RULES_HASH,
                "a".repeat(64),"b".repeat(64),"UNKNOWN","INITIAL_HEADING_REQUIRED",java.util.List.of());
        var result=AnnouncementAttachmentOfficialWorkerIntegrationTest.selectRoleFingerprint(role);
        var executor=new com.saneb.domain.announcementattachment.qa.AttachmentProviderQaCaseExecutor(
                null,null,null,null,null,null,new com.fasterxml.jackson.databind.ObjectMapper());
        assertEquals(executor.selectHash(role),result.get("assessmentHash"));
        var locator=new com.fasterxml.jackson.databind.ObjectMapper().readTree("{\"z\":\"private fixture\",\"a\":{\"z\":2,\"a\":1}}");
        assertEquals(executor.selectHash(locator),AnnouncementAttachmentOfficialWorkerIntegrationTest.selectCanonicalHash(locator));
        assertEquals(java.util.Set.of("ruleVersion","rulesHash","roleCode","reasonCode","textHash","blocksHash","assessmentHash"),result.keySet());
        var phrases=AnnouncementAttachmentOfficialWorkerIntegrationTest.selectReviewPhrasePresence("청년농업인 신청서 private fixture contact");
        assertEquals(java.util.Map.of("청년농업인",true,"취업농",false,"신청서",true,"서명",false),phrases);
        assertFalse(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(phrases).contains("private fixture"));
        assertTrue(AnnouncementAttachmentOfficialWorkerIntegrationTest.selectReviewPhrasePresence(null).values().stream().noneMatch(Boolean::booleanValue));
    }
    @Test void rejectsUnverifiedFingerprintMetadataBeforeItCanLeaveTheProbe() {
        var role=new com.saneb.domain.announcementattachment.classification.AttachmentDocumentRoleClassifier.Assessment(
                "private fixture","private fixture","private fixture","private fixture","UNKNOWN","INITIAL_HEADING_REQUIRED",java.util.List.of());
        var failure=assertThrows(IllegalArgumentException.class,()->AnnouncementAttachmentOfficialWorkerIntegrationTest.selectRoleFingerprint(role));
        assertEquals("ROLE_FINGERPRINT_INVALID",failure.getMessage());
    }
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
