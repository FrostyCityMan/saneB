package com.saneb.domain.announcementattachment.qa;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.saneb.domain.announcementattachment.classification.AttachmentDocumentRoleClassifier;
import com.saneb.domain.announcementattachment.discovery.AttachmentDiscoveryProfile;
import com.saneb.domain.announcementattachment.discovery.BizInfoAttachmentDiscoveryProfile;
import com.saneb.domain.announcementattachment.extraction.IsolatedAttachmentExtractor;
import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import com.saneb.domain.announcementattachment.worker.AttachmentFileTypeValidator;
import com.saneb.domain.announcementsource.classification.*;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.*;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.text.Normalizer;
import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/** 고정 공식 공고 전체의 첫 관측. 기대값 자동 등록·정책 QA·본문/DB/운영 성공 근거가 아니다. */
@EnabledIfEnvironmentVariable(named="SANEB_ATTACHMENT_OFFICIAL_OBSERVATION", matches="true")
class AnnouncementAttachmentOfficialObservationTest {
    private static final ObjectMapper JSON = new ObjectMapper().enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
    private static final BizInfoAttachmentDiscoveryProfile PROFILE = new BizInfoAttachmentDiscoveryProfile();
    private static AnnouncementSourceClassificationRuleSet rules;
    private static IsolatedAttachmentExtractor extractor;
    private static Path reports;
    private static final long MIB = 1024L * 1024;

    @BeforeAll static void selectContext() throws Exception {
        assertTrue(System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("linux"), "LINUX_ISOLATION_REQUIRED");
        assertTrue(Files.isExecutable(Path.of("/usr/bin/bwrap")) && Files.isExecutable(Path.of("/usr/bin/prlimit")), "LINUX_ISOLATION_REQUIRED");
        rules = AnnouncementAttachmentRealFileQaTest.selectDraftRuleSet();
        extractor = new IsolatedAttachmentExtractor(JSON, System.getProperty("saneb.attachment-observation.extractor"));
        reports = Path.of(System.getProperty("saneb.attachment-observation.report")).toAbsolutePath().normalize();
        Files.createDirectories(reports);
    }

    static Stream<Sample> selectSamples() {
        return Stream.of(
                new Sample("BIZINFO-SEMAS-2026", "PBLN_000000000120120", "2026년 소상공인 도약 지원사업(로컬기업육성ㆍ강한소상공인) 참여 소상공인 모집 공고"),
                new Sample("BIZINFO-ANYANG-2026", "PBLN_000000000117918", "[경기] 안양시 2026년 소상공인 이자차액 보전금 지원계획 공고"),
                new Sample("BIZINFO-SDM-2026", "PBLN_000000000124628", "[서울] 서대문구 2026년 소상공인 라이브커머스 지원사업 참여자 모집 공고"));
    }

    @ParameterizedTest(name="{0}") @MethodSource("selectSamples") @Timeout(420)
    void observeWholeOfficialAttachmentSetWithoutPromotingExpectations(Sample sample) throws Exception {
        var report = new LinkedHashMap<String,Object>();
        report.put("scope", "OFFICIAL_WHOLE_ATTACHMENT_OBSERVATION_V1");
        report.put("caseCode", sample.code()); report.put("noticeId", sample.notice());
        report.put("observedAt", Instant.now().toString());
        report.put("profileCode", PROFILE.selectProfileCode()); report.put("profileHash", PROFILE.selectProfileHash());
        report.put("rulesSource", "EPHEMERAL_DB_DRAFT_SEED"); report.put("rulesHash", selectHash(rules));
        report.put("roleRuleVersion", AttachmentDocumentRoleClassifier.VERSION);
        report.put("isPolicyQaPassed", false); report.put("isExpectationApproved", false);
        report.put("isBodyPipelineVerified", false); report.put("productionWriteCount", 0);
        report.put("status", "INCOMPLETE");
        var files = new ArrayList<Map<String,Object>>(); report.put("files", files);
        var budget = new Budget();
        Path temporary = null;
        String stage = "TITLE_GATE";
        try (var client = new AttachmentPinnedDownloadClient()) {
            // 고정·검토한 제목을 먼저 평가한다. 제외 제목은 상세/첨부 요청·임시 원문 생성 모두 0이다.
            var title = new AnnouncementSourceClassificationEngine().selectDecision(new AnnouncementSourceClassificationInput(
                    "BIZINFO", sample.title(), null, null, List.of(), BodySourceCode.NONE, BodyAvailabilityCode.UNAVAILABLE), rules);
            report.put("titleStage", title.titleStageCode().name());
            assertNotEquals(SemanticStatusCode.EXCLUDED, title.semanticStatusCode(), "TITLE_NOT_ELIGIBLE");
            assertTrue(Set.of(TitleStageCode.GROUP_A_MATCHED,TitleStageCode.COMBINATION_MATCHED).contains(title.titleStageCode()), "TITLE_NOT_ELIGIBLE");
            temporary = Files.createTempDirectory("saneb-official-observation-");
            Path detail = temporary.resolve("detail.bin");
            stage = "DETAIL_DISCOVERY";
            var uri = PROFILE.selectDetailUri(sample.notice());
            var download = client.selectDownload(AttachmentPinnedDownloadClient.Request.selectGet(uri), PROFILE.selectApprovedHosts(),
                    budget::selectRequestAllowed, detail, MIB, budget::saveBytes);
            assertTrue(Set.of("text/html", "application/xhtml+xml").contains(download.contentType().split(";",2)[0].strip().toLowerCase(Locale.ROOT)), "DETAIL_CONTENT_TYPE_CHANGED");
            AttachmentDiscoveryProfile.Result discovered;
            try (var input = Files.newInputStream(detail)) {
                var page = Jsoup.parse(input, null, uri.toASCIIString());
                stage = "TITLE_CONFIRMATION";
                validateOfficialTitle(page, sample.title());
                report.put("titleHash", selectHash(sample.title()));
                stage = "DETAIL_DISCOVERY";
                discovered = PROFILE.selectDescriptors(sample.notice(), page.outerHtml());
            } finally { Files.deleteIfExists(detail); }
            report.put("discoveryStatus", discovered.status()); report.put("discoveryComplete", discovered.complete());
            report.put("discoveredFileCount", discovered.descriptors().size());
            // 미지원·미실행도 전체 분모에 남긴다. 관측에서는 파일명으로 역할을 추정하지 않는다.
            for (var descriptor : discovered.descriptors()) {
                var row = new LinkedHashMap<String,Object>(); files.add(row);
                row.put("locatorHash", selectHash(descriptor.locator()));
                row.put("formatHint", descriptor.expectedFormat()); row.put("downloadAllowed", descriptor.downloadAllowed());
                row.put("status", "NOT_RUN");
            }
            assertTrue(discovered.complete() && Set.of("FOUND","NO_FILES").contains(discovered.status()), "DISCOVERY_INCOMPLETE");
            for (int index=0; index<discovered.descriptors().size(); index++) {
                var descriptor = discovered.descriptors().get(index); var row = files.get(index);
                if (!descriptor.downloadAllowed()) { row.put("status", "UNSUPPORTED_NOT_DOWNLOADED"); continue; }
                Path binary = temporary.resolve(UUID.randomUUID()+".bin");
                stage = "FILE_DOWNLOAD";
                try {
                    var bytes = client.selectDownload(descriptor.selectRequest(), PROFILE.selectApprovedHosts(), budget::selectRequestAllowed,
                            binary, 20*MIB, budget::saveBytes);
                    row.put("bytes", bytes.bytes()); row.put("binaryHash", bytes.sha256());
                    stage = "FILE_SIGNATURE";
                    String format = new AttachmentFileTypeValidator().selectFormat(binary, bytes, descriptor.expectedFormat(),
                            PROFILE.selectUtf8DispositionOctets(), PROFILE.selectLegacyBinaryContentTypes());
                    row.put("format", format);
                    stage = "ISOLATED_EXTRACTION";
                    JsonNode actual = extractor.selectExtraction(binary);
                    row.put("quality", actual.path("qualityCode").asText());
                    assertTrue(Set.of("COMPLETE_TEXT","PARTIAL_TEXT","OCR_REQUIRED","ENCRYPTED","CORRUPT","UNSUPPORTED","LIMIT_EXCEEDED")
                            .contains(actual.path("qualityCode").asText()), "ISOLATED_EXTRACTION_FAILED");
                    stage = "TEXT_ROLE";
                    row.putAll(selectTextObservation(actual));
                    row.put("status", "OBSERVED");
                } catch (Exception | AssertionError failure) {
                    row.put("status", "FAILED"); row.put("failedStage", stage);
                    // 한 파일의 기술 실패도 숨기지 않되 나머지 공식 파일 관측은 계속한다.
                } finally { Files.deleteIfExists(binary); }
            }
            assertTrue(files.stream().noneMatch(row -> Set.of("NOT_RUN","FAILED").contains(row.get("status"))), "WHOLE_SET_OBSERVATION_INCOMPLETE");
            report.put("status", "OBSERVED_NOT_VALIDATED");
        } catch (Exception | AssertionError failure) {
            report.put("failedStage", stage);
            // URL/헤더/문서 본문/예외 메시지를 JUnit 로그로 복제하지 않는다.
            throw new AssertionError(sample.code()+": "+stage+" / OBSERVATION_INCOMPLETE");
        } finally {
            if (temporary != null) try (var paths = Files.walk(temporary)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
            }
            report.put("originalFilesRemoved", temporary == null || !Files.exists(temporary));
            report.put("requestReservations", budget.requests); report.put("reservedBytes", budget.bytes);
            JSON.writerWithDefaultPrettyPrinter().writeValue(reports.resolve(sample.code()+".json").toFile(), report);
        }
    }

    static void validateOfficialTitle(org.jsoup.nodes.Document page,String expected) {
        // 실제 기업마당 템플릿은 내용 없는 og:title을 추가한다. 비어 있지 않은 제목의 중복/변경은 허용하지 않는다.
        var headings=page.select("meta[property=og:title]").stream().map(node->node.attr("content")).filter(value->!value.isBlank()).toList();
        assertEquals(1,headings.size(),"TITLE_STRUCTURE_CHANGED");
        assertTrue(selectNormalizedTitle(expected).equals(selectNormalizedTitle(headings.getFirst())),"TITLE_CHANGED");
    }

    static Map<String,Object> selectTextObservation(JsonNode actual) throws Exception {
        String text = actual.path("text").asText("");
        var observation = new LinkedHashMap<String,Object>();
        observation.put("textHash", selectTextHash(text));
        observation.put("characterCount", text.codePointCount(0,text.length()));
        observation.put("blockCount", actual.path("blocks").size());
        if (!"COMPLETE_TEXT".equals(actual.path("qualityCode").asText())) return observation;
        var blocks = new ArrayList<AttachmentSetEvidence.Block>();
        for (var block : actual.path("blocks")) blocks.add(JSON.treeToValue(block, AttachmentSetEvidence.Block.class));
        var assessment = new AttachmentDocumentRoleClassifier().selectAssessment(new AttachmentSetEvidence.Extraction(
                "COMPLETE_TEXT", text, blocks, null, 0));
        observation.put("roleAssessment", assessment);
        observation.put("roleAssessmentHash", selectHash(assessment));
        return observation;
    }
    private static String selectNormalizedTitle(String value) { return Normalizer.normalize(value,Normalizer.Form.NFKC).replaceAll("\\s+"," ").strip(); }
    static String selectHash(Object value) throws Exception { return selectTextHash(JSON.writeValueAsString(JSON.convertValue(value,Object.class))); }
    private static String selectTextHash(String value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    }
    record Sample(String code,String notice,String title) { @Override public String toString() { return code; } }
    private static final class Budget {
        long requests,bytes;
        boolean selectRequestAllowed(AttachmentPinnedDownloadClient.Request request) {
            return PROFILE.selectApprovedRequest(request) && ++requests <= 44 && !Thread.currentThread().isInterrupted();
        }
        boolean saveBytes(long count) {
            if (count < 0 || bytes > 80*MIB-count || Thread.currentThread().isInterrupted()) return false;
            bytes += count; return true;
        }
    }
}
