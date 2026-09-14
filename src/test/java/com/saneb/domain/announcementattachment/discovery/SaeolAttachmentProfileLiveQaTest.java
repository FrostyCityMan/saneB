package com.saneb.domain.announcementattachment.discovery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.domain.announcementattachment.worker.AttachmentFileTypeValidator;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/** 고정 공개 표본의 실제 발견·다운로드·signature 검증만 수행한다. 외부 binary를 Windows에서 파싱하지 않는다. */
@EnabledIfEnvironmentVariable(named = "SANEB_ATTACHMENT_PROFILE_QA", matches = "true")
class SaeolAttachmentProfileLiveQaTest {
    @TempDir Path directory;

    static java.util.stream.Stream<SaeolGetAttachmentDiscoveryProfileTest.Case> selectLiveCases() {
        return java.util.stream.Stream.concat(SaeolGetAttachmentDiscoveryProfileTest.selectCases(), java.util.stream.Stream.of(
                new SaeolGetAttachmentDiscoveryProfileTest.Case(new HwacheonPostAttachmentDiscoveryProfile(), "LGS-000130",
                        "SAFE_SAEOL_EMINWON_LEGACY", "td", "33897", 1, "N"),
                new SaeolGetAttachmentDiscoveryProfileTest.Case(new HwacheonPostAttachmentDiscoveryProfile(), "LGS-000130",
                        "SAFE_SAEOL_EMINWON_LEGACY", "td", "33895", 2, "N")));
    }

    @ParameterizedTest(name = "새올 실측 프로필 {index}")
    @MethodSource("selectLiveCases")
    @Timeout(180)
    void fixedOfficialPageAndAllListedFilesUseProductionDownloadBoundary(SaeolGetAttachmentDiscoveryProfileTest.Case sample) throws Exception {
        var profile = sample.profile();
        var source = SaeolGetAttachmentDiscoveryProfileTest.selectSource(sample, "https");
        Path detail = directory.resolve("detail.html"), binary = directory.resolve("attachment.bin");
        var report = new LinkedHashMap<String, Object>();
        report.put("profileCode", profile.selectProfileCode()); report.put("profileHash", profile.selectProfileHash());
        report.put("noticeId", sample.noticeId());
        report.put("sourceCode", sample.sourceCode()); report.put("startedAt", Instant.now().toString());
        report.put("scope", "FIXED_PUBLIC_PAGE_DISCOVERY_DOWNLOAD_SIGNATURE_ONLY");
        report.put("extractionExecuted", false); report.put("databaseWrites", 0); report.put("operatingActivation", false);
        report.put("status", "FAILED");
        var results = new ArrayList<Map<String, Object>>();
        var reserved = new AtomicLong(); var requests = new AtomicLong();
        String stage = "DETAIL";
        try (var client = new AttachmentPinnedDownloadClient()) {
            AttachmentPinnedDownloadClient.ByteReservation budget = bytes -> reserved.addAndGet(bytes) <= 80L * 1024 * 1024;
            var detailResult = client.selectDownload(AttachmentPinnedDownloadClient.Request.selectGet(profile.selectDetailUri(source)),
                    profile.selectApprovedHosts(), request -> { requests.incrementAndGet(); return profile.selectApprovedRequest(request); },
                    detail, 1024L * 1024, budget);
            report.put("detailHash", detailResult.sha256());
            assertTrue(detailResult.contentType() != null && detailResult.contentType().toLowerCase(java.util.Locale.ROOT).startsWith("text/html"),
                    "공식 상세 응답이 HTML이 아닙니다.");
            AttachmentDiscoveryProfile.Result discovered;
            try (var input = Files.newInputStream(detail)) {
                discovered = profile.selectDescriptors(source, Jsoup.parse(input, null, profile.selectDetailUri(source).toASCIIString()).outerHtml());
            }
            Files.delete(detail);
            stage = "DISCOVERY";
            report.put("discoveryStatus", discovered.status()); report.put("discoveredCount", discovered.descriptors().size());
            report.put("discoveryWarnings", discovered.warnings());
            assertEquals("FOUND", discovered.status(), "실측한 첨부 구조가 달라졌습니다. 새 표본을 확인해야 합니다.");
            assertTrue(discovered.complete(), "일부 링크를 해석하지 못했습니다.");
            assertEquals(sample.fileCount(), discovered.descriptors().size(), "고정 공개 표본의 파일 목록이 변경되었습니다.");
            stage = "DOWNLOAD_SIGNATURE";
            for (var descriptor : discovered.descriptors()) {
                assertTrue(descriptor.downloadAllowed(), "고정 표본에 비지원 형식이 있습니다.");
                assertEquals("UNKNOWN", descriptor.documentRole(), "파일명을 근거로 문서 역할을 자동 확정하면 안 됩니다.");
                try {
                    var downloaded = client.selectDownload(descriptor.selectRequest(), profile.selectApprovedHosts(),
                            request -> { requests.incrementAndGet(); return profile.selectApprovedRequest(request); }, binary, 20L * 1024 * 1024, budget);
                    String format = new AttachmentFileTypeValidator().selectFormat(binary, downloaded, descriptor.expectedFormat());
                    assertTrue(downloaded.bytes() > 0 && downloaded.bytes() <= 20L * 1024 * 1024, "다운로드 byte 상한을 확인하세요.");
                    results.add(Map.of("attachmentIdHash", descriptor.locator().identifiers().get("attachmentId"),
                            "binaryHash", downloaded.sha256(), "bytes", downloaded.bytes(), "signatureFormat", format));
                } finally { Files.deleteIfExists(binary); }
            }
            report.put("status", "DISCOVERY_DOWNLOAD_SIGNATURE_PASSED");
        } catch (Exception | AssertionError failure) {
            String safeCode = failure.getMessage() != null && failure.getMessage().matches("[A-Z][A-Z0-9_]{1,79}")
                    ? failure.getMessage() : failure.getClass().getSimpleName();
            report.put("failureCode", safeCode); report.put("failureStage", stage);
            // URL query·파일명·원문을 Gradle/JUnit 보고서에 복사하지 않는다.
            throw new AssertionError(profile.selectProfileCode() + ": " + stage + "/" + safeCode);
        } finally {
            Files.deleteIfExists(detail); Files.deleteIfExists(binary);
            boolean cleaned;
            try (var entries = Files.list(directory)) { cleaned = entries.findAny().isEmpty(); }
            report.put("temporaryCleaned", cleaned); report.put("files", results); report.put("requestCount", requests.get());
            report.put("reservedBytes", reserved.get()); report.put("finishedAt", Instant.now().toString());
            Path reports = Path.of(System.getProperty("saneb.attachment-profile-qa.report", "build/reports/attachment-profile-discovery-qa"));
            Files.createDirectories(reports);
            Files.writeString(reports.resolve(profile.selectProfileCode() + "-" + sample.noticeId() + ".json"), new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(report));
            assertTrue(cleaned, "실측 QA의 임시 원본이 남아 있습니다.");
        }
    }
}
