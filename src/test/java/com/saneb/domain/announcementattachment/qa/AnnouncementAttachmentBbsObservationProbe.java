package com.saneb.domain.announcementattachment.qa;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.domain.announcementattachment.service.impl.AttachmentApplicationCodeFingerprint;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Set;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;

/** 승인된 태백 1공고의 기존 JUnit 관측을 임시 패키지에서 실행한다. 게시/기대값 변경은 없다. */
public final class AnnouncementAttachmentBbsObservationProbe {
    private static final Set<String> ENV = Set.of("PATH", "LANG", "HOME", "TMPDIR", "PWD",
            "SANEB_ATTACHMENT_BBS_OFFICIAL_OBSERVATION");
    private AnnouncementAttachmentBbsObservationProbe() { }

    static boolean selectComplete(long found, long succeeded, long failed, long skipped, long aborted, long containersFailed) {
        return found == 1 && succeeded == 1 && failed == 0 && skipped == 0 && aborted == 0 && containersFailed == 0;
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
            if (args.length != 1 || !args[0].matches("[a-f0-9]{64}")
                    || !"Linux".equals(System.getProperty("os.name")) || "root".equals(System.getProperty("user.name"))
                    || !"/work".equals(Path.of("").toAbsolutePath().toString())
                    || !"/work/tmp".equals(System.getProperty("java.io.tmpdir"))
                    || !"true".equals(System.getenv("SANEB_ATTACHMENT_BBS_OFFICIAL_OBSERVATION"))
                    || !ENV.containsAll(System.getenv().keySet())) throw new IllegalStateException();
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
            System.setProperty("saneb.attachment-observation.group", "TAEBAEK");
            stage = "JUNIT_EXECUTION";
            var listener = new SummaryGeneratingListener();
            var request = LauncherDiscoveryRequestBuilder.request()
                    .selectors(DiscoverySelectors.selectClass(AnnouncementAttachmentBbsOfficialObservationTest.class))
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
            Path path = Path.of("/work/reports/TAEBAEK-184816.json");
            if (!Files.isRegularFile(path) || Files.size(path) > 65536) throw new IllegalStateException();
            JsonNode report = json.readTree(Files.readAllBytes(path));
            result.put("report", report);
            stage = "FINAL_IDENTITY";
            if (!codeHash.equals(new AttachmentApplicationCodeFingerprint(json).selectVerifiedHash())) throw new IllegalStateException();
            passed = selectReportComplete(report) && selectComplete(summary.getTestsFoundCount(), summary.getTestsSucceededCount(),
                    summary.getTestsFailedCount(), summary.getTestsSkippedCount(), summary.getTestsAbortedCount(), summary.getContainersFailedCount());
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
