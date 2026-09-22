package com.saneb.domain.announcementattachment.qa;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.domain.announcementattachment.service.impl.AttachmentApplicationCodeFingerprint;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;

/** 별도 승인 범위의 태백 1공고 또는 옥천 고정 3공고만 실행한다. 게시/기대값 변경은 없다. */
public final class AnnouncementAttachmentBbsObservationProbe {
    static final List<String> OKCHEON_CASES = List.of("OKCHEON-193369", "OKCHEON-193297", "OKCHEON-193187");
    private static final Set<String> ENV = Set.of("PATH", "LANG", "HOME", "TMPDIR", "PWD",
            "SANEB_ATTACHMENT_BBS_OFFICIAL_OBSERVATION", "SANEB_ATTACHMENT_BBS_FIXED_CASE_QA");
    private AnnouncementAttachmentBbsObservationProbe() { }

    static boolean selectComplete(long found, long succeeded, long failed, long skipped, long aborted, long containersFailed) {
        return found == 1 && succeeded == 1 && failed == 0 && skipped == 0 && aborted == 0 && containersFailed == 0;
    }

    static String selectMode(String[] args) {
        if (args.length < 1 || args.length > 2 || !args[0].matches("[a-f0-9]{64}"))
            throw new IllegalArgumentException("PROBE_ARGUMENTS_INVALID");
        if (args.length == 1) return "OBSERVATION";
        if (!Set.of("FIXED", "OKCHEON").contains(args[1])) throw new IllegalArgumentException("PROBE_MODE_INVALID");
        return args[1];
    }

    static boolean selectOkcheonComplete(long found, long succeeded, long failed, long skipped, long aborted, long containersFailed) {
        return found == 3 && succeeded == 3 && failed == 0 && skipped == 0 && aborted == 0 && containersFailed == 0;
    }

    private static boolean selectBoolean(JsonNode node, String field, boolean expected) {
        return node.path(field).isBoolean() && node.path(field).booleanValue() == expected;
    }

    private static boolean selectBounded(JsonNode node, String field, long min, long max) {
        var value = node.path(field);
        return value.isIntegralNumber() && value.canConvertToLong() && value.longValue() >= min && value.longValue() <= max;
    }

    // Node 관측 보고서 검증과 동일하게 관측 성공과 완전 추출·정책 승인을 분리한다.
    static boolean selectOkcheonReportsComplete(List<JsonNode> reports, Instant startedAt, Instant endedAt) {
        if (reports == null || reports.size() != 3 || startedAt == null || endedAt == null || endedAt.isBefore(startedAt)) return false;
        var seen = new HashSet<String>();
        for (var report : reports) {
            if (report == null || !OKCHEON_CASES.contains(report.path("caseCode").asText())
                    || !seen.add(report.path("caseCode").asText())) return false;
            try {
                var observedAt = Instant.parse(report.path("observedAt").asText());
                if (observedAt.isBefore(startedAt) || observedAt.isAfter(endedAt)) return false;
            } catch (java.time.format.DateTimeParseException failure) { return false; }
            if (!"OFFICIAL_THREE_STAGE_OBSERVATION_V1".equals(report.path("scope").asText())
                    || !"LOCAL_OKCHEON_BBS_V1".equals(report.path("profileCode").asText())
                    || !selectBounded(report, "productionWriteCount", 0, 0)
                    || !selectBoolean(report, "isPolicyQaPassed", false) || !selectBoolean(report, "isExpectationApproved", false)
                    || !selectBoolean(report, "originalFilesRemoved", true) || !selectBounded(report, "expectedListedFileCount", 1, 1)
                    || !selectBounded(report, "maximumRequestReservations", 44, 44)
                    || !selectBounded(report, "maximumReservedBytes", 83886080L, 83886080L)
                    || !selectBounded(report, "requestReservationsIncludingBodyUpperBound", 0, 44)
                    || !selectBounded(report, "reservedBytesIncludingBodyUpperBound", 0, 83886080L)
                    || !report.path("files").isArray()) return false;
            if ("OKCHEON-193187".equals(report.path("caseCode").asText())) {
                if (!"TITLE_NOT_ELIGIBLE_NOT_FETCHED".equals(report.path("status").asText())
                        || !"COMBINATION_NOT_MATCHED".equals(report.path("titleStage").asText())
                        || !"TITLE_COMBINATION_NOT_MATCHED".equals(report.path("titleReason").asText())
                        || !report.path("files").isEmpty() || report.hasNonNull("bodyStatus") || report.hasNonNull("discoveryStatus")
                        || !selectBounded(report, "requestReservationsIncludingBodyUpperBound", 0, 0)
                        || !selectBounded(report, "reservedBytesIncludingBodyUpperBound", 0, 0)
                        || !selectBoolean(report, "isWholeTextAnalysisComplete", false)
                        || !selectBoolean(report, "requiresFinalAdminVerification", false)) return false;
                continue;
            }
            if (!"OBSERVED_NOT_VALIDATED".equals(report.path("status").asText())
                    || !"COMBINATION_MATCHED".equals(report.path("titleStage").asText())
                    || !selectBoolean(report, "bodyStageComplete", true) || !"AVAILABLE".equals(report.path("bodyStatus").asText())
                    || !"FOUND".equals(report.path("discoveryStatus").asText()) || !selectBoolean(report, "discoveryComplete", true)
                    || !selectBounded(report, "discoveredFileCount", 1, 1) || report.path("files").size() != 1
                    || !selectBoolean(report, "requiresFinalAdminVerification", true)
                    || !selectBounded(report, "requestReservationsIncludingBodyUpperBound", 3, 44)
                    || !selectBounded(report, "reservedBytesIncludingBodyUpperBound", 1, 83886080L)
                    || !Set.of("ACCEPTED", "REVIEW_REQUIRED").contains(report.path("decisionStatus").asText())) return false;
            var file = report.path("files").get(0);
            if (!"OBSERVED".equals(file.path("status").asText()) || !"HWPX".equals(file.path("format").asText())
                    || !selectBounded(file, "bytes", 1, 20971520)
                    || !Set.of("COMPLETE_TEXT", "PARTIAL_TEXT", "OCR_REQUIRED", "ENCRYPTED", "CORRUPT", "UNSUPPORTED", "LIMIT_EXCEEDED")
                            .contains(file.path("quality").asText())) return false;
            boolean complete = "COMPLETE_TEXT".equals(file.path("quality").asText());
            if (complete && (!selectBounded(file, "characterCount", 1, 1000000) || !selectBounded(file, "blockCount", 1, 20000)
                    || !Set.of("NOTICE", "GUIDE", "FORM", "REFERENCE", "UNKNOWN").contains(file.path("roleAssessment").path("roleCode").asText()))) return false;
            if (!selectBoolean(report, "isWholeTextAnalysisComplete", complete)) return false;
        }
        return true;
    }

    static boolean selectReportComplete(JsonNode report) {
        return report != null && "TAEBAEK-184816".equals(report.path("caseCode").asText())
                && "OFFICIAL_THREE_STAGE_OBSERVATION_V1".equals(report.path("scope").asText())
                && "OBSERVED_NOT_VALIDATED".equals(report.path("status").asText())
                && report.path("productionWriteCount").asInt(-1) == 0
                && report.path("isPolicyQaPassed").isBoolean() && !report.path("isPolicyQaPassed").asBoolean()
                && report.path("isExpectationApproved").isBoolean() && !report.path("isExpectationApproved").asBoolean()
                && report.path("originalFilesRemoved").asBoolean(false)
                && report.path("requiresFinalAdminVerification").asBoolean(false)
                && report.path("bodyStageComplete").asBoolean(false)
                && report.path("files").isArray() && report.path("files").size() == 2
                && report.path("maximumRequestReservations").asInt(-1) == 44
                && report.path("maximumReservedBytes").asLong(-1) == 83886080L
                && report.path("requestReservationsIncludingBodyUpperBound").asLong(-1) >= 0
                && report.path("requestReservationsIncludingBodyUpperBound").asLong(-1) <= 44
                && report.path("reservedBytesIncludingBodyUpperBound").asLong(-1) >= 0
                && report.path("reservedBytesIncludingBodyUpperBound").asLong(-1) <= 83886080L;
    }

    static boolean selectFixedReportComplete(JsonNode report) {
        if(report==null||!"TAEBAEK-184816".equals(report.path("caseCode").asText())
                ||!"FIXED_CASE_EXECUTOR_QA_EPHEMERAL_ONLY".equals(report.path("scope").asText())
                ||!"FIXED_EXPECTATIONS_MATCHED_REVIEW_REQUIRED".equals(report.path("status").asText())
                ||report.path("productionWriteCount").asInt(-1)!=0
                ||!report.path("originalFilesRemoved").asBoolean(false))return false;
        for(String key:Set.of("isPolicyQaPassed","isExpectationCoverageComplete","normalNotice"))
            if(!report.path(key).isBoolean()||report.path(key).asBoolean())return false;
        var result=report.path("result");
        if(!"SINGLE_FIXED_NOTICE_PROVIDER_QA".equals(result.path("scope").asText())
                ||!"TAEBAEK-184816".equals(result.path("caseId").asText())
                ||!"PASSED".equals(result.path("status").asText())
                ||!"FIXED_NOTICE_EXPECTATIONS_MATCHED".equals(result.path("reasonCode").asText())
                ||result.path("expectedFileCount").asInt(-1)!=2||result.path("discoveredFileCount").asInt(-1)!=2
                ||!result.path("discoveryComplete").asBoolean(false)||!result.path("allTextComplete").asBoolean(false)
                ||!result.path("originalFilesRemoved").asBoolean(false)||!result.path("isPolicyQaPassed").isBoolean()
                ||result.path("isPolicyQaPassed").asBoolean()||!result.path("files").isArray()||result.path("files").size()!=2
                ||result.path("requestReservations").asLong(-1)<1||result.path("requestReservations").asLong()>39
                ||result.path("reservedBytes").asLong(-1)<1||result.path("reservedBytes").asLong()>81508141L)return false;
        for(var file:result.path("files"))if(!"PASSED".equals(file.path("status").asText())
                ||!"COMPLETE_TEXT".equals(file.path("quality").asText())||!file.path("roleAssessmentHash").asText().matches("[a-f0-9]{64}"))return false;
        return true;
    }

    public static void main(String[] args) {
        PrintStream output = System.out;
        System.setOut(new PrintStream(OutputStream.nullOutputStream()));
        System.setErr(new PrintStream(OutputStream.nullOutputStream()));
        var result = new LinkedHashMap<String, Object>();
        result.put("kind", "BBS_OBSERVATION_PROBE");
        result.put("productionDatabaseUsed", false);
        result.put("isPolicyQaPassed", false);
        result.put("isExpectationApproved", false);
        boolean passed = false;
        String stage = "BOUNDARY";
        try {
            String mode = selectMode(args);
            boolean fixed = "FIXED".equals(mode);
            boolean okcheon = "OKCHEON".equals(mode);
            if (!"Linux".equals(System.getProperty("os.name")) || "root".equals(System.getProperty("user.name"))
                    || !"/work".equals(Path.of("").toAbsolutePath().toString())
                    || !"/work/tmp".equals(System.getProperty("java.io.tmpdir"))
                    || !"true".equals(System.getenv(fixed?"SANEB_ATTACHMENT_BBS_FIXED_CASE_QA":"SANEB_ATTACHMENT_BBS_OFFICIAL_OBSERVATION"))
                    || System.getenv(fixed?"SANEB_ATTACHMENT_BBS_OFFICIAL_OBSERVATION":"SANEB_ATTACHMENT_BBS_FIXED_CASE_QA")!=null
                    || !ENV.containsAll(System.getenv().keySet())) throw new IllegalStateException();
            result.put("verificationMode", mode);
            var json = new ObjectMapper();
            stage = "CODE_IDENTITY";
            String codeHash = new AttachmentApplicationCodeFingerprint(json).selectVerifiedHash();
            if (!args[0].equals(codeHash)) throw new IllegalStateException();
            result.put("executionCodeHash", codeHash);
            stage = "JCE_TLS_RUNTIME";
            javax.crypto.Cipher.getInstance("AES/GCM/NoPadding");
            javax.net.ssl.SSLContext.getDefault();
            System.setProperty("saneb.attachment-observation.extractor", "/qa/extractor");
            System.setProperty("saneb.attachment-observation.report", "/work/reports");
            System.setProperty("saneb.attachment-observation.group", okcheon ? "OKCHEON" : "TAEBAEK");
            if(fixed) {
                // 동일 승인44요청/80MiB에서 앞선 관측5요청/2,377,939bytes를 공제한다.
                System.setProperty("saneb.attachment-fixed.maximum-requests","39");
                System.setProperty("saneb.attachment-fixed.maximum-bytes","81508141");
            }
            stage = "JUNIT_EXECUTION";
            Instant startedAt = Instant.now();
            var listener = new SummaryGeneratingListener();
            var request = LauncherDiscoveryRequestBuilder.request()
                    .selectors(DiscoverySelectors.selectClass(fixed?AnnouncementAttachmentBbsFixedCaseQaTest.class:AnnouncementAttachmentBbsOfficialObservationTest.class))
                    .configurationParameter("junit.jupiter.execution.parallel.enabled", "false")
                    .configurationParameter("junit.jupiter.tempdir.cleanup.mode.default", "ALWAYS").build();
            LauncherFactory.create().execute(request, listener);
            var summary = listener.getSummary();
            result.put("found", summary.getTestsFoundCount());
            result.put("passed", summary.getTestsSucceededCount());
            result.put("failed", summary.getTestsFailedCount());
            result.put("skipped", summary.getTestsSkippedCount());
            result.put("aborted", summary.getTestsAbortedCount());
            result.put("failedContainers", summary.getContainersFailedCount());
            result.put("failureTypes", summary.getFailures().stream().map(f -> f.getException().getClass().getSimpleName()).distinct().limit(8).toList());
            stage = "REPORTS";
            var reports = new ArrayList<JsonNode>();
            if (okcheon) result.put("reports", reports);
            for (String name : okcheon ? OKCHEON_CASES : List.of(fixed ? "TAEBAEK-184816-fixed-case" : "TAEBAEK-184816")) {
                Path path = Path.of("/work/reports", name + ".json");
                if (!Files.isRegularFile(path) || Files.size(path) > 65536) throw new IllegalStateException();
                reports.add(json.readTree(Files.readAllBytes(path)));
            }
            if (!okcheon) result.put("report", reports.getFirst());
            stage = "FINAL_IDENTITY";
            if (!codeHash.equals(new AttachmentApplicationCodeFingerprint(json).selectVerifiedHash())) throw new IllegalStateException();
            passed = okcheon ? selectOkcheonReportsComplete(reports, startedAt, Instant.now())
                    && selectOkcheonComplete(summary.getTestsFoundCount(), summary.getTestsSucceededCount(), summary.getTestsFailedCount(),
                            summary.getTestsSkippedCount(), summary.getTestsAbortedCount(), summary.getContainersFailedCount())
                    : (fixed ? selectFixedReportComplete(reports.getFirst()) : selectReportComplete(reports.getFirst()))
                    && selectComplete(summary.getTestsFoundCount(), summary.getTestsSucceededCount(), summary.getTestsFailedCount(),
                            summary.getTestsSkippedCount(), summary.getTestsAbortedCount(), summary.getContainersFailedCount());
        } catch (Exception | LinkageError | AssertionError failure) {
            result.put("failedStage", stage);
            result.put("failureCode", "BBS_OBSERVATION_INCOMPLETE");
            result.put("failureType", failure.getClass().getSimpleName());
        }
        result.put("status", passed ? "PASSED" : "INCOMPLETE");
        try { output.println(new ObjectMapper().writeValueAsString(result)); }
        catch (Exception ignored) { output.println("{\"kind\":\"BBS_OBSERVATION_PROBE\",\"status\":\"INCOMPLETE\"}"); }
        System.exit(passed ? 0 : 1);
    }
}
