package com.saneb.domain.announcementattachment.discovery;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.domain.announcementattachment.worker.AttachmentFileTypeValidator;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/** 지원사업 고정 공개 표본. 개인 합격자/주소 명단을 다운로드하지 않으며 추출·DB·운영 활성화는 별도 Gate다. */
@EnabledIfEnvironmentVariable(named = "SANEB_ATTACHMENT_PROFILE_QA", matches = "true")
class StandardBbsAttachmentProfileLiveQaTest {
    @TempDir Path directory;
    record Sample(int profileIndex, String noticeId, int fileCount, List<String> formats, String expectedRejection) {
        Sample(int profileIndex, String noticeId, int fileCount, List<String> formats) {
            this(profileIndex, noticeId, fileCount, formats, null);
        }
    }
    static Stream<Sample> selectCases() {
        return Stream.of(new Sample(0, "185101", 2, List.of("HWPX", "HWPX")),
                new Sample(0, "184816", 2, List.of("HWPX", "HWPX")), new Sample(0, "184827", 1, List.of("HWPX")),
                new Sample(1, "424679", 1, List.of("HWPX")), new Sample(1, "424078", 2, List.of("HWPX", "HWPX")),
                new Sample(1, "424077", 2, List.of("HWPX", "HWPX")), new Sample(2, "157529", 2, List.of("PDF", "PDF")),
                new Sample(2, "157016", 1, List.of("HWPX")), new Sample(2, "156846", 1, List.of("HWPX")));
    }
    static Stream<Sample> selectWonjuCases() {
        return Stream.of(new Sample(3, "491704", 4, List.of("HWPX", "HWPX", "HWPX", "HWPX")),
                new Sample(3, "491507", 1, List.of("HWPX"), "ATTACHMENT_FORMAT_MISMATCH"),
                new Sample(3, "491340", 1, List.of("HWPX")));
    }
    @ParameterizedTest(name = "원주 공식 지원사업 표본 {index}") @MethodSource("selectWonjuCases") @Timeout(150)
    void discoversWonjuFilesAndChecksBoundedProductionTransport(Sample sample) throws Exception {
        discoversAllFilesAndChecksBoundedProductionTransport(sample);
    }
    @ParameterizedTest(name = "BBS 공식 지원사업 표본 {index}") @MethodSource("selectCases") @Timeout(150)
    void discoversAllFilesAndChecksBoundedProductionTransport(Sample sample) throws Exception {
        var site = StandardBbsAttachmentDiscoveryProfileTest.selectCases().toList().get(sample.profileIndex());
        var profile = site.profile(); var source = StandardBbsAttachmentDiscoveryProfileTest.selectSource(site, sample.noticeId());
        Path detail = directory.resolve("detail.html"), binary = directory.resolve("attachment.bin");
        var report = new LinkedHashMap<String, Object>(); var files = new ArrayList<Map<String, Object>>();
        var requests = new AtomicLong(); var reserved = new AtomicLong();
        report.put("profileCode", profile.selectProfileCode()); report.put("profileHash", profile.selectProfileHash());
        report.put("caseId", profile.selectProfileCode() + "-" + sample.noticeId()); report.put("startedAt", Instant.now().toString());
        report.put("scope", "FIXED_PUBLIC_PAGE_DISCOVERY_DOWNLOAD_SIGNATURE_ONLY"); report.put("extractionExecuted", false);
        report.put("policyQaPassed", false); report.put("expectedRejection", sample.expectedRejection());
        report.put("databaseWrites", 0); report.put("operatingActivation", false); report.put("status", "FAILED");
        String stage = "DETAIL";
        try (var client = new AttachmentPinnedDownloadClient()) {
            AttachmentPinnedDownloadClient.ByteReservation budget = bytes -> reserved.addAndGet(bytes) <= 50L * 1024 * 1024;
            var downloaded = client.selectDownload(AttachmentPinnedDownloadClient.Request.selectGet(profile.selectDetailUri(source)),
                    profile.selectApprovedHosts(), r -> { requests.incrementAndGet(); return profile.selectApprovedRequest(r); }, detail, 1024L * 1024, budget);
            assertTrue(downloaded.contentType() != null && downloaded.contentType().toLowerCase(Locale.ROOT).startsWith("text/html"), "DETAIL_CONTENT_TYPE_CHANGED");
            report.put("detailHash", downloaded.sha256()); AttachmentDiscoveryProfile.Result result;
            try (var input = Files.newInputStream(detail)) {
                result = profile.selectDescriptors(source, Jsoup.parse(input, null, profile.selectDetailUri(source).toASCIIString()).outerHtml());
            }
            Files.delete(detail); stage = "DISCOVERY";
            report.put("discoveryStatus", result.status()); report.put("discoveredCount", result.descriptors().size()); report.put("warnings", result.warnings());
            assertEquals("FOUND", result.status(), "DISCOVERY_STRUCTURE_CHANGED"); assertTrue(result.complete(), "DISCOVERY_INCOMPLETE");
            assertEquals(sample.fileCount(), result.descriptors().size(), "FILE_LIST_CHANGED");
            assertEquals(sample.formats(), result.descriptors().stream().map(AttachmentDiscoveryProfile.Descriptor::expectedFormat).toList(), "FILE_FORMAT_CHANGED");
            stage = "DOWNLOAD_SIGNATURE";
            for (var descriptor : result.descriptors()) {
                assertTrue(descriptor.downloadAllowed(), "FORMAT_SUPPORT_CHANGED"); assertEquals("UNKNOWN", descriptor.documentRole(), "AUTOMATIC_ROLE_FORBIDDEN");
                try {
                    var downloadedFile = client.selectDownload(descriptor.selectRequest(), profile.selectApprovedHosts(),
                            r -> { requests.incrementAndGet(); return profile.selectApprovedRequest(r); }, binary, 20L * 1024 * 1024, budget);
                    if (sample.expectedRejection() != null) {
                        assertEquals(1, sample.fileCount(), "NEGATIVE_SAMPLE_SCOPE_CHANGED");
                        var rejected = assertThrows(java.io.IOException.class, () -> new AttachmentFileTypeValidator()
                                .selectFormat(binary, downloadedFile, descriptor.expectedFormat(), profile.selectUtf8DispositionOctets(), profile.selectLegacyBinaryContentTypes()));
                        assertEquals(sample.expectedRejection(), rejected.getMessage(), "OFFICIAL_REJECTION_CHANGED");
                        files.add(Map.of("status", "REJECTED", "failureCode", sample.expectedRejection(), "bytes", downloadedFile.bytes(), "binaryHash", downloadedFile.sha256()));
                        continue;
                    }
                    String format = new AttachmentFileTypeValidator().selectFormat(binary, downloadedFile, descriptor.expectedFormat(),
                            profile.selectUtf8DispositionOctets(), profile.selectLegacyBinaryContentTypes());
                    assertTrue(downloadedFile.bytes() > 0, "EMPTY_BINARY");
                    files.add(Map.of("attachmentIdHash", descriptor.locator().identifiers().get("attachmentId"), "status", "DOWNLOADED_SIGNATURE_VALID",
                            "bytes", downloadedFile.bytes(), "binaryHash", downloadedFile.sha256(), "signatureFormat", format,
                            "legacyMimeUsed", profile.selectLegacyBinaryContentTypes().contains(downloadedFile.contentType().split(";", 2)[0].trim().toLowerCase(Locale.ROOT))));
                } finally { Files.deleteIfExists(binary); }
            }
            assertEquals(1L + sample.fileCount(), requests.get(), "UNEXPECTED_REDIRECT_OR_PREVIEW_REQUEST");
            report.put("status", sample.expectedRejection() == null ? "DISCOVERY_DOWNLOAD_SIGNATURE_PASSED" : "DOWNLOAD_REJECTED_AS_EXPECTED");
        } catch (Exception | AssertionError failure) {
            String safe = failure.getMessage() != null && failure.getMessage().matches("[A-Z][A-Z0-9_]{1,79}") ? failure.getMessage() : failure.getClass().getSimpleName();
            report.put("failureCode", safe); report.put("failureStage", stage);
            throw new AssertionError(profile.selectProfileCode() + ": " + stage + "/" + safe);
        } finally {
            Files.deleteIfExists(detail); Files.deleteIfExists(binary); boolean cleaned;
            try (var entries = Files.list(directory)) { cleaned = entries.findAny().isEmpty(); }
            report.put("temporaryCleaned", cleaned); report.put("files", files); report.put("requestCount", requests.get());
            report.put("reservedBytes", reserved.get()); report.put("finishedAt", Instant.now().toString());
            Path reports = Path.of(System.getProperty("saneb.attachment-profile-qa.report", "build/reports/attachment-profile-discovery-qa"));
            Files.createDirectories(reports); Files.writeString(reports.resolve(profile.selectProfileCode() + "-" + sample.noticeId() + ".json"),
                    new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(report));
            assertTrue(cleaned, "TEMPORARY_ORIGINAL_NOT_REMOVED");
        }
    }
}
