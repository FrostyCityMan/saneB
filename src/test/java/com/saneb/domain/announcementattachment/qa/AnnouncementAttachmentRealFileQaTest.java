package com.saneb.domain.announcementattachment.qa;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.domain.announcementattachment.classification.AnnouncementAttachmentClassificationEngine;
import com.saneb.domain.announcementattachment.classification.AnnouncementAttachmentClassificationEngine.Block;
import com.saneb.domain.announcementattachment.classification.AnnouncementAttachmentClassificationEngine.FileInput;
import com.saneb.domain.announcementattachment.classification.AnnouncementAttachmentClassificationEngine.Input;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.*;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationEngine;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationInput;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationRuleSet;
import com.saneb.domain.announcementsource.dao.AnnouncementSourceClassificationDao;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient;
import com.saneb.domain.announcementsource.service.impl.AnnouncementSourceActiveRuleServiceImpl;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceClassificationRuleTermRow;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.flywaydb.core.Flyway;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.jdbc.core.JdbcTemplate;

/** 운영 DB/규칙을 사용하지 않는 명시 실행 전용 실제 공개 첨부 QA. */
@EnabledIfEnvironmentVariable(named = "SANEB_ATTACHMENT_REAL_FILE_QA", matches = "true")
class AnnouncementAttachmentRealFileQaTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String HOST = "https://www.bizinfo.go.kr";
    private static final DockerAttachmentQaRunner RUNNER = new DockerAttachmentQaRunner();
    private static AnnouncementSourceClassificationRuleSet rules;
    private static String ruleHash;
    private static Path library;
    private static Path reportRoot;

    @BeforeAll static void selectQaContext() throws Exception {
        library = Path.of(System.getProperty("saneb.attachment-qa.library")).toRealPath();
        reportRoot = Path.of(System.getProperty("saneb.attachment-qa.report")).toAbsolutePath().normalize();
        Files.createDirectories(reportRoot);
        assertEquals("ISOLATION_PROBE_OK", RUNNER.selectIsolationProbe());
        rules = selectDraftRuleSet();
        ruleHash = selectHash(MAPPER.writeValueAsBytes(rules));
    }

    static AnnouncementSourceClassificationRuleSet selectDraftRuleSet() throws Exception {
        // 이 테스트가 생성한 별도 loopback DB에만 migration을 적용한다. 실제 DB 설정은 받지 않는다.
        try (var pg = EmbeddedPostgres.builder().setPort(0).setServerConfig("listen_addresses", "127.0.0.1").start()) {
            Flyway.configure().dataSource(pg.getPostgresDatabase()).locations("classpath:db/migration").load().migrate();
            var sql = new JdbcTemplate(pg.getPostgresDatabase());
            List<AnnouncementSourceClassificationRuleTermRow> terms = sql.query("""
                    -- 격리 QA의 DRAFT seed만 읽는다. ACTIVE 게시/수정은 하지 않는다.
                    SELECT r.id AS release_id, r.release_code, k.rule_code, g.group_code, g.group_kind_code,
                           c.category_code, s.support_type_code, k.strength_code, k.is_enabled AS rule_enabled,
                           t.term_type_code, t.term_text, t.match_mode_code, t.is_discovery_term, t.discovery_order,
                           t.is_classification_term, t.is_enabled AS term_enabled
                    FROM announcement_source_classification_rule_releases r
                    JOIN announcement_source_classification_rule_groups g ON g.release_id=r.id
                    JOIN announcement_source_classification_keyword_rules k ON k.group_id=g.id
                    JOIN announcement_source_classification_keyword_terms t ON t.keyword_rule_id=k.id AND t.group_id=g.id
                    LEFT JOIN announcement_target_categories c ON c.id=g.target_category_id
                    LEFT JOIN announcement_support_types s ON s.id=g.support_type_id
                    WHERE r.release_code='ASCR-000001' AND r.release_status_code='DRAFT'
                      AND g.is_enabled AND k.is_enabled AND t.is_enabled
                      AND (t.is_classification_term OR g.group_kind_code='PROTECTED_METADATA')
                    ORDER BY g.sort_order, k.sort_order, t.term_type_code, t.id
                    """, (rs, index) -> new AnnouncementSourceClassificationRuleTermRow(
                    rs.getObject("release_id", UUID.class), rs.getString("release_code"), rs.getString("rule_code"),
                    rs.getString("group_code"), rs.getString("group_kind_code"), rs.getString("category_code"),
                    rs.getString("support_type_code"), rs.getString("strength_code"), rs.getBoolean("rule_enabled"),
                    rs.getString("term_type_code"), rs.getString("term_text"), rs.getString("match_mode_code"),
                    rs.getBoolean("is_discovery_term"), rs.getObject("discovery_order", Integer.class),
                    rs.getBoolean("is_classification_term"), rs.getBoolean("term_enabled")));
            assertFalse(terms.isEmpty(), "격리 DB의 DRAFT seed가 필요합니다.");
            var dao = mock(AnnouncementSourceClassificationDao.class);
            when(dao.selectActiveClassificationRuleTermList()).thenReturn(terms);
            // 기존 row → rule 변환만 재사용한다. 이 메모리 값은 운영 ACTIVE 상태가 아니다.
            var draft = new AnnouncementSourceActiveRuleServiceImpl(dao).selectActiveRuleSet().ruleSet();
            assertEquals(0, sql.queryForObject("-- QA 규칙 미활성 확인\nSELECT count(1) FROM announcement_source_classification_rule_releases WHERE release_status_code='ACTIVE'", Integer.class));
            assertEquals(0, sql.queryForObject("-- QA 첨부 정책 부재 확인\nSELECT count(1) FROM announcement_attachment_policies", Integer.class));
            return draft;
        }
    }

    static Stream<Sample> selectSampleList() {
        return Stream.of(
                new Sample("PDF-SDM-2026", "PBLN_000000000124628", "FILE_000000000765684", 4, "PDF", "UNKNOWN"),
                new Sample("PDF-SEMAS-2026", "PBLN_000000000120120", "FILE_000000000749023", 0, "PDF", "NOTICE"),
                new Sample("HWP-ANYANG-2026", "PBLN_000000000117918", "FILE_000000000742032", 0, "HWP", "NOTICE"),
                new Sample("HWPX-SDM-2026", "PBLN_000000000124628", "FILE_000000000765683", 1, "HWPX", "NOTICE"));
    }

    @ParameterizedTest(name = "{0}") @MethodSource("selectSampleList") @Timeout(120)
    void actualPublicBytesAreExtractedAndClassifiedWithoutPublication(Sample sample) throws Exception {
        Path temporary = Files.createTempDirectory("saneb-attachment-real-qa-");
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("caseId", sample.caseId());
        report.put("checkedAt", Instant.now().toString());
        report.put("noticeId", sample.noticeId());
        report.put("ruleSource", "EPHEMERAL_DB_DRAFT_SEED");
        report.put("ruleReleaseCode", rules.releaseCode());
        report.put("ruleHash", ruleHash);
        report.put("ruleCount", rules.rules().size());
        report.put("isolation", "DOCKER_QA_ONLY");
        report.put("image", DockerAttachmentQaRunner.IMAGE);
        report.put("productionWriteCount", 0);
        report.put("evaluationScope", "SINGLE_SELECTED_FILE_DIAGNOSTIC");
        report.put("entireNoticeAttachmentSetVerified", false);
        report.put("passed", false);
        String stage = "DETAIL_FETCH";
        try (var download = new AttachmentPinnedDownloadClient()) {
            Path html = temporary.resolve(UUID.randomUUID() + ".bin");
            String detailUrl = HOST + "/sii/siia/selectSIIA200Detail.do?pblancId=" + sample.noticeId();
            download.selectDownload(URI.create(detailUrl), Set.of("www.bizinfo.go.kr"), html, 1024 * 1024);
            var page = Jsoup.parse(Files.readString(html, StandardCharsets.UTF_8), HOST);
            var titleElement = page.selectFirst("meta[property=og:title]");
            assertNotNull(titleElement, "QA_TITLE_ELEMENT_MISSING");
            String title = titleElement.attr("content");
            Files.delete(html);
            stage = "TITLE_GATE";
            var base = new AnnouncementSourceClassificationEngine().selectDecision(new AnnouncementSourceClassificationInput(
                    "BIZINFO", title, null, null, List.of(), BodySourceCode.NONE, BodyAvailabilityCode.UNAVAILABLE), rules);
            report.put("titleStage", base.titleStageCode().name());
            report.put("baseReason", base.reasonCode().name());
            assertNotEquals(SemanticStatusCode.EXCLUDED, base.semanticStatusCode(), "QA_TITLE_EXCLUDED_NO_ATTACHMENT_REQUEST");
            assertTrue(Set.of(TitleStageCode.GROUP_A_MATCHED, TitleStageCode.COMBINATION_MATCHED).contains(base.titleStageCode()), "QA_TITLE_NOT_ELIGIBLE");
            String relative = "/cmm/fms/fileDown.do?atchFileId=" + sample.fileId() + "&fileSn=" + sample.fileSn();
            assertTrue(page.select("a[href]").stream().anyMatch(link -> relative.equals(link.attr("href"))), "QA_DIRECT_ATTACHMENT_LINK_MISSING");
            Path file = temporary.resolve(UUID.randomUUID() + ".bin");
            stage = "FILE_DOWNLOAD";
            var bytes = download.selectDownload(URI.create(HOST + relative), Set.of("www.bizinfo.go.kr"), file, 20L * 1024 * 1024);
            report.put("downloadedBytes", bytes.bytes());
            report.put("binarySha256", bytes.sha256());
            stage = "ISOLATED_EXTRACTION";
            JsonNode extracted = RUNNER.selectExtraction(library, file);
            Files.delete(file);
            report.put("format", extracted.path("format").asText());
            report.put("quality", extracted.path("qualityCode").asText());
            assertEquals(sample.format(), extracted.path("format").asText(), "QA_SIGNATURE_OR_FORMAT_CHANGED");
            String text = extracted.path("text").asText("");
            report.put("textSha256", selectHash(text.getBytes(StandardCharsets.UTF_8)));
            report.put("characterCount", text.codePointCount(0, text.length()));
            report.put("blockCount", extracted.path("blocks").size());
            report.put("pageCount", extracted.path("pageCount").isNull() ? null : extracted.path("pageCount").asInt());
            report.put("containsSmallBusinessKeyword", text.contains("소상공인"));
            assertTrue(Set.of("COMPLETE_TEXT", "PARTIAL_TEXT", "OCR_REQUIRED").contains(extracted.path("qualityCode").asText()), "QA_EXTRACTION_NOT_SUPPORTED");
            if (sample.role().equals("NOTICE")) assertTrue(text.contains("소상공인"), "QA_EXPECTED_KOREAN_KEYWORD_MISSING");
            List<Block> blocks = new ArrayList<>();
            for (JsonNode block : extracted.path("blocks")) blocks.add(new Block(block.path("index").asInt(),
                    block.path("startOffset").asInt(), block.path("endOffset").asInt(),
                    block.path("evidenceScopeId").asText(), block.path("scopeReliable").asBoolean()));
            stage = "CLASSIFICATION";
            // 선택한 파일 하나의 진단 입력만 sealed로 취급한다. 공고 전체 첨부 수집 완료를 뜻하지 않는다.
            var decision = new AnnouncementAttachmentClassificationEngine().selectDecision(new Input(base, rules,
                    true, "FOUND", true, List.of(new FileInput(UUID.randomUUID(), UUID.randomUUID(), sample.role(),
                    extracted.path("qualityCode").asText(), text, blocks, null)), null, List.of()));
            report.put("manualQaRole", sample.role());
            report.put("decisionStatus", decision.status());
            report.put("reason", decision.reason());
            report.put("warningCodes", decision.warnings());
            report.put("targetCodes", decision.targetCodes());
            report.put("supportCodes", decision.supportCodes());
            report.put("matchCount", decision.matches().size());
            report.put("reviewMatchCount", decision.matches().stream().filter(match -> match.action().equals("REVIEW_REQUIRED")).count());
            assertNotEquals("EXCLUDED", decision.status(), "QA_ATTACHMENT_MUST_NOT_EXCLUDE");
            if (!extracted.path("qualityCode").asText().equals("COMPLETE_TEXT") || sample.role().equals("UNKNOWN"))
                assertEquals("REVIEW_REQUIRED", decision.status(), "QA_UNCERTAIN_MUST_REQUIRE_REVIEW");
            assertEquals(ReasonCode.BODY_UNAVAILABLE, base.reasonCode(), "QA_BASE_WAS_CHANGED");
            report.put("passed", true);
        } catch (Exception | AssertionError error) {
            report.put("failedStage", stage);
            report.put("errorType", error.getClass().getSimpleName());
            // HTTP·파서 예외의 원문 URL/문서 내용을 JUnit XML로 전달하지 않는다.
            throw new AssertionError(sample.caseId() + ": " + stage + " / " + error.getClass().getSimpleName());
        } finally {
            try (var files = Files.walk(temporary)) {
                for (Path path : files.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
            }
            report.put("originalFilesRemoved", !Files.exists(temporary));
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(reportRoot.resolve(sample.caseId() + ".json").toFile(), report);
        }
    }

    @AfterAll static void clearContext() { rules = null; }

    private static String selectHash(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    record Sample(String caseId, String noticeId, String fileId, int fileSn, String format, String role) {
        @Override public String toString() { return caseId; }
    }
}
