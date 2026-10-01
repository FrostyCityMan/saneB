package com.saneb.domain.announcementattachment.qa;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.saneb.domain.announcementattachment.service.impl.AttachmentApplicationCodeFingerprint;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

/** 서울 임시 격리 전용. 실패 영수증도 보존하며 추출·DB·정책 승인으로 승격하지 않는다. */
public final class RegionalCollectionObservationProbe {
    static final String MODE = "SEOUL_COLLECTION_01";
    static final String MODE_TWO = "SEOUL_COLLECTION_02";
    static final String GROUPS = "POCHEON,GANGNEUNG,CHUNGBUK,GONGJU,PYEONGTAEK,NAMHAE";
    static final List<String> CASES = List.of("POCHEON-64129", "GANGNEUNG-60798", "CHUNGBUK-67302",
            "GONGJU-59971", "PYEONGTAEK-95902", "NAMHAE-35694");
    static final String GROUPS_TWO = "EUNPYEONG_SUPPORT,SEODAEMUN,ICHEON,ASAN_SUPPORT,BONGHWA,YEONGDONG";
    static final List<String> CASES_TWO = List.of("EUNPYEONG-50607", "SEODAEMUN-313956", "ICHEON-70639",
            "ASAN-76469", "BONGHWA-32956", "YEONGDONG-759FDCD3");
    private static final Set<String> ENV = Set.of("PATH", "LANG", "HOME", "TMPDIR", "PWD",
            "SANEB_ATTACHMENT_BBS_OFFICIAL_OBSERVATION");
    private static final Set<String> FIELDS = Set.of("scope", "caseCode", "observedAt", "titleInputSource",
            "profileCode", "profileHash", "isPolicyQaPassed", "isExpectationApproved", "productionWriteCount",
            "status", "expectedListedFileCount", "collectionOnly", "isExtractionVerified", "isWholeTextAnalysisComplete",
            "rulesSource", "rulesHash", "titleStage", "titleReason", "requiresFinalAdminVerification",
            "bodyStatus", "bodyFailureCode", "bodyAttempts", "bodyRedirects", "bodyStageComplete", "bodyHash",
            "bodyCharacterCount", "bodyDecision", "bodyReason", "detailIdentityVerified", "discoveryStatus",
            "discoveryComplete", "discoveredFileCount", "discoveryWarningCodes", "discoveryError", "files",
            "collectionStageComplete", "downloadedFileCount", "failedFileCount", "unsupportedFileCount",
            "notRunFileCount", "fileListChanged", "failedStage", "failureCode", "originalFilesRemoved",
            "maximumRequestReservations", "maximumReservedBytes", "requestReservationsIncludingBodyUpperBound",
            "reservedBytesIncludingBodyUpperBound");
    private static final Set<String> FILE_FIELDS = Set.of("locatorHash", "formatHint", "downloadAllowed", "status",
            "bytes", "binaryHash", "format", "failedStage", "failureCode", "downloadTrace");

    private RegionalCollectionObservationProbe() { }

    static List<AnnouncementAttachmentBbsOfficialObservationTest.ObservationCase> selectCases() {
        return selectCases(MODE);
    }

    static boolean selectSupportedMode(String mode) { return MODE.equals(mode) || MODE_TWO.equals(mode); }

    static List<AnnouncementAttachmentBbsOfficialObservationTest.ObservationCase> selectCases(String mode) {
        if (!selectSupportedMode(mode)) throw new IllegalArgumentException("COLLECTION_MODE_INVALID");
        boolean second = MODE_TWO.equals(mode);
        var cases = AnnouncementAttachmentBbsOfficialObservationTest.selectBatchCases(second ? GROUPS_TWO : GROUPS).toList();
        if (!(second ? CASES_TWO : CASES).equals(cases.stream().map(AnnouncementAttachmentBbsOfficialObservationTest.ObservationCase::code).toList()))
            throw new IllegalArgumentException("COLLECTION_SCOPE_CHANGED");
        for (var sample : cases) {
            var budget = AnnouncementAttachmentBbsOfficialObservationTest.selectBudget(sample.profile(), true, false);
            long expectedRequests = second && CASES_TWO.indexOf(sample.code()) < 2 ? 7 : 6;
            if (budget.maximumRequests != expectedRequests || budget.maximumBytes != 24117248L)
                throw new IllegalArgumentException("COLLECTION_BUDGET_CHANGED");
        }
        return cases;
    }

    private static void validateMetadata(JsonNode value) {
        if (value.isObject()) { value.elements().forEachRemaining(RegionalCollectionObservationProbe::validateMetadata); return; }
        if (value.isArray()) {
            if (value.size() > 32) throw new IllegalArgumentException("REPORT_METADATA_INVALID");
            value.elements().forEachRemaining(RegionalCollectionObservationProbe::validateMetadata); return;
        }
        if (value.isNull() || value.isBoolean() || value.isIntegralNumber()) return;
        if (!value.isTextual() || !value.textValue().matches("[A-Za-z0-9_.:+-]{1,128}"))
            throw new IllegalArgumentException("REPORT_METADATA_INVALID");
    }

    static ObjectNode selectReport(JsonNode report, int ordinal, Instant start, Instant end) {
        return selectReport(report, MODE, ordinal, start, end);
    }

    static ObjectNode selectReport(JsonNode report, String mode, int ordinal, Instant start, Instant end) {
        var sample = selectCases(mode).get(ordinal);
        long requestLimit = MODE_TWO.equals(mode) && ordinal < 2 ? 7 : 6;
        if (!report.isObject() || !sample.code().equals(report.path("caseCode").asText())
                || !sample.profile().selectProfileHash().equals(report.path("profileHash").asText())
                || !sample.profile().selectProfileCode().equals(report.path("profileCode").asText())
                || !"OFFICIAL_THREE_STAGE_OBSERVATION_V1".equals(report.path("scope").asText()))
            throw new IllegalArgumentException("REPORT_SCOPE_CHANGED");
        var at = Instant.parse(report.path("observedAt").asText());
        if (at.isBefore(start) || at.isAfter(end)) throw new IllegalArgumentException("REPORT_TIME_CHANGED");
        for (String key : List.of("collectionOnly", "originalFilesRemoved"))
            if (!report.path(key).isBoolean() || !report.path(key).booleanValue()) throw new IllegalArgumentException("REPORT_BOUNDARY_INVALID");
        for (String key : List.of("isExtractionVerified", "isWholeTextAnalysisComplete", "isPolicyQaPassed", "isExpectationApproved"))
            if (!report.path(key).isBoolean() || report.path(key).booleanValue()) throw new IllegalArgumentException("REPORT_BOUNDARY_INVALID");
        for (var entry : java.util.Map.of("productionWriteCount", 0L, "maximumRequestReservations", requestLimit,
                "maximumReservedBytes", 24117248L).entrySet())
            if (!report.path(entry.getKey()).isIntegralNumber() || report.path(entry.getKey()).longValue() != entry.getValue())
                throw new IllegalArgumentException("REPORT_BUDGET_INVALID");
        for (var entry : java.util.Map.of("requestReservationsIncludingBodyUpperBound", requestLimit, "reservedBytesIncludingBodyUpperBound", 24117248L).entrySet()) {
            var value = report.path(entry.getKey());
            if (!value.isIntegralNumber() || !value.canConvertToLong() || value.longValue() < 0 || value.longValue() > entry.getValue())
                throw new IllegalArgumentException("REPORT_BUDGET_INVALID");
        }
        if (!Set.of("INCOMPLETE", "COLLECTION_ONLY_OBSERVED_NOT_APPROVED", "COLLECTION_ONLY_PARTIAL_NOT_APPROVED",
                "TITLE_EXCLUDED_NOT_FETCHED", "TITLE_NOT_ELIGIBLE_NOT_FETCHED").contains(report.path("status").asText())
                || !report.path("files").isArray() || report.path("files").size() > 8)
            throw new IllegalArgumentException("REPORT_STATUS_INVALID");
        var safe = ((ObjectNode) report).deepCopy();
        safe.retain(FIELDS);
        for (var file : safe.path("files")) {
            if (!file.isObject()) throw new IllegalArgumentException("REPORT_FILE_INVALID");
            ((ObjectNode) file).retain(FILE_FIELDS);
            if (file.has("downloadTrace")) {
                if (!file.path("downloadTrace").isObject()) throw new IllegalArgumentException("REPORT_FILE_INVALID");
                ((ObjectNode) file.path("downloadTrace")).retain("schemaVersion", "step", "phase", "transportInvocations", "completedTransports");
            }
            if ("DOWNLOADED".equals(file.path("status").asText()) && (!file.path("binaryHash").asText().matches("[a-f0-9]{64}")
                    || !file.path("bytes").isIntegralNumber() || file.path("bytes").longValue() <= 0
                    || file.path("bytes").longValue() > 20971520 || !Set.of("PDF", "HWP", "HWPX").contains(file.path("format").asText())))
                throw new IllegalArgumentException("REPORT_FILE_INVALID");
        }
        validateMetadata(safe);
        return safe;
    }

    public static void main(String[] args) {
        PrintStream output = System.out;
        System.setOut(new PrintStream(OutputStream.nullOutputStream()));
        System.setErr(new PrintStream(OutputStream.nullOutputStream()));
        var json = new ObjectMapper();
        var result = new LinkedHashMap<String, Object>();
        String mode = args.length == 2 && selectSupportedMode(args[1]) ? args[1] : MODE;
        result.put("kind", "BBS_OBSERVATION_PROBE"); result.put("verificationMode", mode);
        result.put("completionMeaning", "RECEIPTS_ONLY_NOT_COLLECTION_SUCCESS");
        result.put("productionDatabaseUsed", false); result.put("isPolicyQaPassed", false);
        result.put("isExpectationApproved", false); result.put("isExtractionVerified", false);
        var reports = new ArrayList<JsonNode>(); result.put("reports", reports);
        boolean complete = false;
        try {
            if (args.length != 2 || !args[0].matches("[a-f0-9]{64}") || !selectSupportedMode(args[1])
                    || !"Linux".equals(System.getProperty("os.name")) || "root".equals(System.getProperty("user.name"))
                    || !"/work".equals(Path.of("").toAbsolutePath().toString()) || !"/work/tmp".equals(System.getProperty("java.io.tmpdir"))
                    || !ENV.containsAll(System.getenv().keySet()) || !"true".equals(System.getenv("SANEB_ATTACHMENT_BBS_OFFICIAL_OBSERVATION")))
                throw new IllegalArgumentException("COLLECTION_BOUNDARY_INVALID");
            String codeHash = new AttachmentApplicationCodeFingerprint(json).selectVerifiedHash();
            if (!args[0].equals(codeHash)) throw new IllegalArgumentException("CODE_IDENTITY_CHANGED");
            result.put("executionCodeHash", codeHash);
            javax.crypto.Cipher.getInstance("AES/GCM/NoPadding"); javax.net.ssl.SSLContext.getDefault();
            System.setProperty("saneb.attachment-observation.collection-only", "true");
            System.setProperty("saneb.attachment-observation.report", "/work/reports");
            System.setProperty("saneb.attachment-observation.report-label", "");
            var cases = selectCases(mode);
            Instant start = Instant.now();
            int executionErrors = 0;
            for (int i = 0; i < cases.size(); i++) {
                try { new AnnouncementAttachmentBbsOfficialObservationTest().observesTitleBodyAndWholeAttachmentSetWithoutPublication(cases.get(i)); }
                catch (Exception | AssertionError failure) { executionErrors++; }
                Path path = Path.of("/work/reports", cases.get(i).code() + ".json");
                if (!Files.isRegularFile(path) || Files.isSymbolicLink(path) || Files.size(path) > 32768)
                    throw new IllegalArgumentException("REPORT_MISSING_OR_OVERSIZED");
                reports.add(selectReport(json.readTree(Files.readAllBytes(path)), mode, i, start, Instant.now()));
            }
            result.put("caseExecutionErrors", executionErrors);
            result.put("downloadedFiles", reports.stream().flatMap(row -> java.util.stream.StreamSupport.stream(row.path("files").spliterator(), false))
                    .filter(file -> "DOWNLOADED".equals(file.path("status").asText())).count());
            complete = codeHash.equals(new AttachmentApplicationCodeFingerprint(json).selectVerifiedHash());
        } catch (Exception | LinkageError | AssertionError failure) {
            result.put("failureCode", "COLLECTION_RECEIPTS_INCOMPLETE");
            result.put("failureType", failure.getClass().getSimpleName());
        }
        result.put("status", complete ? "PASSED" : "INCOMPLETE");
        try { output.println(json.writeValueAsString(result)); }
        catch (Exception ignored) { output.println("{\"kind\":\"BBS_OBSERVATION_PROBE\",\"status\":\"INCOMPLETE\"}"); }
        System.exit(complete ? 0 : 1);
    }
}
