package com.saneb.domain.announcementattachment.qa;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementattachment.worker.AttachmentFileTypeValidator;
import com.saneb.domain.announcementsource.classification.*;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/** 고정 공개 지원사업 3건. 제목 통과 후 본문/첨부 signature까지만 검증하며 추출·운영 쓰기는 없다. */
@EnabledIfEnvironmentVariable(named="SANEB_ATTACHMENT_PROFILE_QA", matches="true")
class ChungjuEminwonProfileLiveQaTest {
    @TempDir Path directory;
    private static AnnouncementSourceClassificationRuleSet rules;
    record Sample(String id, String title, String format) { @Override public String toString() { return id; } }
    static Stream<Sample> selectCases() {
        return Stream.of(new Sample("72625", "2026년 교통약자 차량용 보조기기 설치 추가지원 사업 공고(3차)", "HWPX"),
                new Sample("72039", "2026년 충주시 중소기업육성기금 지원계획 변경 공고", "HWPX"),
                new Sample("70852", "2026년 결혼·출산가정 대출이자 지원사업 공고", "HWP"));
    }
    @BeforeAll static void selectDraftRules() throws Exception { rules = AnnouncementAttachmentRealFileQaTest.selectDraftRuleSet(); }
    @ParameterizedTest(name="충주 고정 지원사업 {0}") @MethodSource("selectCases") @Timeout(150)
    void validatesTitleThenBodyAndAllDeclaredFiles(Sample sample) throws Exception {
        var profile = new ChungjuEminwonAttachmentDiscoveryProfile();
        String url = "https://www.chungju.go.kr/www/selectEminwonView.do?key=510&ancmt_mgt_no=" + sample.id();
        var n = new AnnouncementSourceIdentityNormalizer();
        var source = new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE", n.hash(n.canonicalizeUrl(url)), url, "LGS-000137", "SAEOL_GOSI");
        var report = new LinkedHashMap<String,Object>(); var files = new ArrayList<Map<String,Object>>();
        var requests = new AtomicLong(); var bytes = new AtomicLong();
        report.put("scope", "FIXED_TITLE_BODY_DISCOVERY_SIGNATURE_ONLY"); report.put("caseCode", "CHUNGJU-" + sample.id());
        report.put("profileCode", profile.selectProfileCode()); report.put("profileHash", profile.selectProfileHash());
        report.put("observedAt", Instant.now().toString()); report.put("status", "FAILED"); report.put("files", files);
        report.put("productionWriteCount", 0); report.put("isPolicyQaPassed", false); report.put("extractionExecuted", false);
        Path detail = directory.resolve("detail.html"), binary = directory.resolve("attachment.bin");
        String stage = "TITLE";
        try (var client = new AttachmentPinnedDownloadClient()) {
            var title = new AnnouncementSourceClassificationEngine().selectDecision(new AnnouncementSourceClassificationInput(
                    "LOCAL_GOV_NOTICE", sample.title(), null, null, List.of(), BodySourceCode.NONE, BodyAvailabilityCode.UNAVAILABLE), rules);
            report.put("titleStage", title.titleStageCode()); report.put("titleReason", title.reasonCode());
            assertEquals("70852".equals(sample.id()) ? TitleStageCode.COMBINATION_MATCHED : TitleStageCode.COMBINATION_NOT_MATCHED,
                    title.titleStageCode(), "FIXED_TITLE_STAGE_CHANGED");
            if (!AnnouncementAttachmentBbsOfficialObservationTest.selectTitleMayProceed(title)) {
                report.put("status", "TITLE_STOPPED_NOT_FETCHED"); return;
            }
            stage = "BODY";
            var body = new LocalGovernmentNoticeProviderContentClient(true, 3000, 7000, 1024 * 1024, 0, 1, "saneB-notice-collector/1.0")
                    .selectContent(new ProviderContentRequest("LOCAL_GOV_NOTICE", UUID.fromString("77000000-0000-0000-0000-000000000001"),
                            "https://www.chungju.go.kr/www/selectEminwonList.do?key=510", url));
            requests.addAndGet(body.attemptCount()); bytes.addAndGet(2L * 1024 * 1024);
            report.put("bodyStatus", body.statusCode()); report.put("bodyFailure", body.failureCode());
            report.put("bodyCharacterCount", body.bodyText() == null ? 0 : body.bodyText().codePointCount(0, body.bodyText().length()));
            // 본문 실패도 첨부 발견을 가로막지 않는다. 모든 관측을 남긴 후 본문 성공 여부를 별도로 검증한다.
            stage = "DETAIL";
            AttachmentPinnedDownloadClient.ByteReservation budget = value -> bytes.addAndGet(value) <= 30L * 1024 * 1024;
            var initial = AttachmentPinnedDownloadClient.Request.selectGet(profile.selectDetailUri(source));
            var response = client.selectDownload(initial, profile.selectApprovedHosts(), r -> {
                requests.incrementAndGet(); return profile.selectApprovedRequest(initial, r);
            }, detail, 1024 * 1024, budget);
            assertTrue(response.contentType() != null && response.contentType().startsWith("text/html"), "DETAIL_TYPE_CHANGED");
            AttachmentDiscoveryProfile.Result found;
            try (var input = Files.newInputStream(detail)) {
                var doc = Jsoup.parse(input, null, url);
                var table = ChungjuEminwonNoticePage.selectTable(doc);
                assertTrue(sample.title().equals(ChungjuEminwonNoticePage.selectCell(table, "제목").text()), "TITLE_CHANGED");
                found = profile.selectDescriptors(source, doc.outerHtml());
            }
            Files.delete(detail); stage = "DISCOVERY";
            report.put("discoveryStatus", found.status()); report.put("discoveredCount", found.descriptors().size());
            assertTrue(found.complete() && "FOUND".equals(found.status()), "DISCOVERY_INCOMPLETE");
            assertEquals(1, found.descriptors().size(), "FILE_SET_CHANGED");
            stage = "DOWNLOAD_SIGNATURE";
            for (var file : found.descriptors()) {
                assertEquals(sample.format(), file.expectedFormat(), "FILE_FORMAT_CHANGED");
                assertTrue(file.downloadAllowed(), "UNSUPPORTED_FILE"); assertEquals("UNKNOWN", file.documentRole());
                var fileRequest = file.selectRequest();
                var downloaded = client.selectDownload(fileRequest, profile.selectApprovedHosts(), r -> {
                    requests.incrementAndGet(); return profile.selectApprovedRequest(fileRequest, r);
                }, binary, 20L * 1024 * 1024, budget);
                // 실패 metadata에도 MIME만 남긴다. 다운로드 헤더/원문/파일명/URL은 출력하지 않는다.
                report.put("lastDownloadMediaType", downloaded.contentType() == null ? "MISSING" : downloaded.contentType().split(";", 2)[0]);
                String format = new AttachmentFileTypeValidator().selectFormat(binary, downloaded, file.expectedFormat(),
                        profile.selectUtf8DispositionOctets(), profile.selectLegacyBinaryContentTypes());
                files.add(Map.of("signatureFormat", format, "bytes", downloaded.bytes(), "binaryHash", downloaded.sha256()));
                Files.delete(binary);
            }
            assertTrue(body.statusCode() == ProviderContentCodes.StatusCode.AVAILABLE, "BODY_NOT_AVAILABLE");
            assertEquals(1, body.attemptCount(), "BODY_RETRIED"); assertEquals(0, body.redirectCount(), "BODY_REDIRECTED");
            assertEquals(3, requests.get(), "UNEXPECTED_REQUESTS");
            report.put("status", "TITLE_BODY_DISCOVERY_SIGNATURE_PASSED");
        } catch (Exception | AssertionError failure) {
            String code = failure.getMessage() != null && failure.getMessage().matches("[A-Z][A-Z0-9_]{1,79}") ? failure.getMessage() : failure.getClass().getSimpleName();
            report.put("failureStage", stage); report.put("failureCode", code);
            throw new AssertionError("CHUNGJU:" + stage + "/" + code);
        } finally {
            Files.deleteIfExists(detail); Files.deleteIfExists(binary);
            boolean cleaned; try (var entries = Files.list(directory)) { cleaned = entries.findAny().isEmpty(); }
            report.put("originalFilesRemoved", cleaned); report.put("requestCount", requests.get()); report.put("reservedBytes", bytes.get());
            Path root = Path.of(System.getProperty("saneb.attachment-profile-qa.report", "build/reports/attachment-profile-discovery-qa"));
            Files.createDirectories(root); Files.writeString(root.resolve("CHUNGJU-" + sample.id() + ".json"), new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(report));
            assertTrue(cleaned, "ORIGINAL_NOT_REMOVED");
        }
    }
}
