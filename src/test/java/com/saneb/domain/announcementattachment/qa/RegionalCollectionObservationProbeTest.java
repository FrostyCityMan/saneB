package com.saneb.domain.announcementattachment.qa;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 네트워크 없이 고정 범위·예산·실패 보존·원문 비출력 경계를 검증한다. */
class RegionalCollectionObservationProbeTest {
    private final Instant start = Instant.parse("2026-10-01T00:00:00Z");
    private final Instant end = start.plusSeconds(600);

    private ObjectNode selectFailure(int ordinal) {
        var sample = RegionalCollectionObservationProbe.selectCases().get(ordinal);
        var row = new ObjectMapper().createObjectNode();
        row.put("caseCode", sample.code()).put("profileCode", sample.profile().selectProfileCode())
                .put("profileHash", sample.profile().selectProfileHash()).put("observedAt", start.plusSeconds(1).toString())
                .put("scope", "OFFICIAL_THREE_STAGE_OBSERVATION_V1").put("status", "INCOMPLETE")
                .put("failedStage", "DETAIL_DISCOVERY").put("failureCode", "TIMEOUT")
                .put("productionWriteCount", 0).put("maximumRequestReservations", 6).put("maximumReservedBytes", 24117248)
                .put("requestReservationsIncludingBodyUpperBound", 3).put("reservedBytesIncludingBodyUpperBound", 2097152);
        for (String key : List.of("collectionOnly", "originalFilesRemoved")) row.put(key, true);
        for (String key : List.of("isExtractionVerified", "isWholeTextAnalysisComplete", "isPolicyQaPassed", "isExpectationApproved")) row.put(key, false);
        row.putArray("files");
        return row;
    }

    @Test void fixedSixKeepEachBudgetAndDistinctSources() {
        var cases = RegionalCollectionObservationProbe.selectCases();
        assertEquals(6, cases.size());
        assertEquals(6, cases.stream().map(sample -> sample.source().localSourceCode()).distinct().count());
        assertEquals(RegionalCollectionObservationProbe.CASES, cases.stream().map(AnnouncementAttachmentBbsOfficialObservationTest.ObservationCase::code).toList());
    }

    @Test void secondBatchHasSixDifferentFixedCasesAndThirtyEightRequestLimit() {
        var cases = RegionalCollectionObservationProbe.selectCases(RegionalCollectionObservationProbe.MODE_TWO);
        assertEquals(RegionalCollectionObservationProbe.CASES_TWO, cases.stream().map(AnnouncementAttachmentBbsOfficialObservationTest.ObservationCase::code).toList());
        assertEquals(6, cases.stream().map(sample -> sample.source().localSourceCode()).distinct().count());
        assertTrue(cases.stream().noneMatch(sample -> RegionalCollectionObservationProbe.CASES.contains(sample.code())));
        assertEquals(38, cases.stream().mapToLong(sample -> AnnouncementAttachmentBbsOfficialObservationTest.selectBudget(sample.profile(), true, false).maximumRequests).sum());
        assertThrows(IllegalArgumentException.class, () -> RegionalCollectionObservationProbe.selectCases("SEOUL_COLLECTION_04"));
    }

    @Test void dongducheonBatchKeepsTwoKnownCasesAndTwelveRequests() {
        var mode = RegionalCollectionObservationProbe.MODE_THREE;
        var cases = RegionalCollectionObservationProbe.selectCases(mode);
        assertEquals(List.of("DONGDUCHEON-44176", "DONGDUCHEON-45339"), cases.stream().map(AnnouncementAttachmentBbsOfficialObservationTest.ObservationCase::code).toList());
        assertEquals(12, cases.stream().mapToLong(sample -> AnnouncementAttachmentBbsOfficialObservationTest.selectBudget(sample.profile(), true, false).maximumRequests).sum());
        for (int i = 0; i < 2; i++) {
            var sample = cases.get(i); var row = selectFailure(i);
            row.put("caseCode", sample.code()).put("profileCode", sample.profile().selectProfileCode()).put("profileHash", sample.profile().selectProfileHash());
            assertEquals(sample.code(), RegionalCollectionObservationProbe.selectReport(row, mode, i, start, end).path("caseCode").asText());
            row.put("requestReservationsIncludingBodyUpperBound", 7); int ordinal = i;
            assertThrows(IllegalArgumentException.class, () -> RegionalCollectionObservationProbe.selectReport(row, mode, ordinal, start, end));
        }
    }

    @Test void secondBatchReceiptsCannotBorrowFirstBatchOrExceedOwnBudget() {
        for (int i = 0; i < 6; i++) {
            var sample = RegionalCollectionObservationProbe.selectCases(RegionalCollectionObservationProbe.MODE_TWO).get(i);
            var row = selectFailure(i);
            row.put("caseCode", sample.code()).put("profileCode", sample.profile().selectProfileCode()).put("profileHash", sample.profile().selectProfileHash());
            row.put("maximumRequestReservations", i < 2 ? 7 : 6);
            assertEquals(sample.code(), RegionalCollectionObservationProbe.selectReport(row, RegionalCollectionObservationProbe.MODE_TWO, i, start, end).path("caseCode").asText());
            int ordinal = i;
            assertThrows(IllegalArgumentException.class, () -> RegionalCollectionObservationProbe.selectReport(row, ordinal, start, end));
            row.put("requestReservationsIncludingBodyUpperBound", 8);
            assertThrows(IllegalArgumentException.class, () -> RegionalCollectionObservationProbe.selectReport(row, RegionalCollectionObservationProbe.MODE_TWO, ordinal, start, end));
        }
    }

    @Test void allFailuresAreValidReceiptsButNotCollectionSuccess() {
        for (int i = 0; i < 6; i++) {
            var safe = RegionalCollectionObservationProbe.selectReport(selectFailure(i), i, start, end);
            assertEquals("INCOMPLETE", safe.path("status").asText());
            assertEquals("TIMEOUT", safe.path("failureCode").asText());
            assertTrue(safe.path("files").isEmpty());
            assertFalse(safe.path("isExtractionVerified").asBoolean());
        }
    }

    @Test void mixedSuccessAndFailureRemainSeparateAndRawDataIsRemoved() {
        var row = selectFailure(5);
        row.put("status", "COLLECTION_ONLY_PARTIAL_NOT_APPROVED").put("bodyText", "원문은 전송하지 않는다").put("url", "https://private.invalid");
        var files = row.withArray("files");
        files.addObject().put("status", "DOWNLOADED").put("bytes", 102547).put("format", "HWPX")
                .put("binaryHash", "a".repeat(64)).put("text", "추출 원문 금지");
        files.addObject().put("status", "FAILED").put("failureCode", "TIMEOUT");
        var safe = RegionalCollectionObservationProbe.selectReport(row, 5, start, end);
        assertFalse(safe.has("bodyText")); assertFalse(safe.has("url")); assertFalse(safe.at("/files/0").has("text"));
        assertEquals("DOWNLOADED", safe.at("/files/0/status").asText());
        assertEquals("FAILED", safe.at("/files/1/status").asText());
    }

    @Test void scopeFingerprintTimeAndBudgetsCannotBeChanged() {
        for (String key : List.of("caseCode", "profileCode", "profileHash", "scope", "observedAt")) {
            var row = selectFailure(0); row.put(key, key.equals("observedAt") ? start.minusSeconds(1).toString() : "CHANGED");
            assertThrows(RuntimeException.class, () -> RegionalCollectionObservationProbe.selectReport(row, 0, start, end), key);
        }
        for (String key : List.of("productionWriteCount", "maximumRequestReservations", "maximumReservedBytes",
                "requestReservationsIncludingBodyUpperBound", "reservedBytesIncludingBodyUpperBound")) {
            var row = selectFailure(0); row.put(key, Long.MAX_VALUE);
            assertThrows(IllegalArgumentException.class, () -> RegionalCollectionObservationProbe.selectReport(row, 0, start, end), key);
        }
    }

    @Test void extractionPolicyAndCleanupCannotBeFalselyPromoted() {
        for (String key : List.of("isExtractionVerified", "isWholeTextAnalysisComplete", "isPolicyQaPassed", "isExpectationApproved", "originalFilesRemoved", "collectionOnly")) {
            var row = selectFailure(0); row.put(key, !row.path(key).asBoolean());
            assertThrows(IllegalArgumentException.class, () -> RegionalCollectionObservationProbe.selectReport(row, 0, start, end), key);
        }
    }

    @Test void downloadedFileNeedsBytesHashAndRecognizedFormat() {
        var row = selectFailure(0); row.withArray("files").addObject().put("status", "DOWNLOADED").put("format", "HWPX").put("bytes", 10);
        assertThrows(IllegalArgumentException.class, () -> RegionalCollectionObservationProbe.selectReport(row, 0, start, end));
    }
}
