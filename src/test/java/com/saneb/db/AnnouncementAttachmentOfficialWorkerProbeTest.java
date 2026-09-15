package com.saneb.db;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class AnnouncementAttachmentOfficialWorkerProbeTest {
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
