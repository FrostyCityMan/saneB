package com.saneb.domain.announcementattachment.qa;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.domain.announcementattachment.classification.AnnouncementAttachmentClassificationEngine;
import com.saneb.domain.announcementattachment.classification.AnnouncementAttachmentClassificationEngine.Block;
import com.saneb.domain.announcementattachment.classification.AnnouncementAttachmentClassificationEngine.FileInput;
import com.saneb.domain.announcementattachment.classification.AnnouncementAttachmentClassificationEngine.Input;
import com.saneb.domain.announcementattachment.extraction.IsolatedAttachmentExtractor;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.*;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationEngine;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationInput;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationRuleSet;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient;
import java.io.IOException;
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
import org.jsoup.Jsoup;

/** 명시적 CLI만 제공한다. Spring/스케줄러/DB를 시작하지 않고 고정 공개 표본만 검사한다. */
public final class AnnouncementAttachmentServerQa {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String HOST = "https://www.bizinfo.go.kr";
    private AnnouncementAttachmentServerQa() { }

    public static void main(String[] args) {
        boolean passed = false;
        try {
            if (args.length != 2 || !System.getProperty("os.name").toLowerCase(java.util.Locale.ROOT).contains("linux"))
                throw new IOException("QA_ENVIRONMENT_INVALID");
            Path distribution = Path.of(args[0]).toRealPath();
            Path temporary = Path.of(args[1]).toRealPath();
            Path snapshot = distribution.resolve("qa-rules.json");
            if (Files.size(snapshot) > 2 * 1024 * 1024) throw new IOException("QA_RULE_LIMIT");
            byte[] ruleBytes = Files.readAllBytes(snapshot);
            var rules = MAPPER.readValue(ruleBytes, AnnouncementSourceClassificationRuleSet.class);
            if (!"ASCR-000001".equals(rules.releaseCode()) || rules.rules().size() != 394)
                throw new IOException("QA_RULE_SNAPSHOT_INVALID");
            var extractor = new IsolatedAttachmentExtractor(MAPPER, distribution.toString());
            int failures = 0;
            for (Sample sample : selectSamples()) {
                var result = selectSampleResult(sample, temporary, extractor, rules, selectHash(ruleBytes));
                System.out.println("ATTACHMENT_SERVER_QA=" + MAPPER.writeValueAsString(result));
                if (!Boolean.TRUE.equals(result.get("passed"))) failures++;
            }
            passed = failures == 0;
            System.out.println("ATTACHMENT_SERVER_QA_SUMMARY=" + MAPPER.writeValueAsString(
                    Map.of("samples", 4, "failures", failures, "passed", passed, "productionWriteCount", 0)));
        } catch (Exception error) {
            // URL・본문・환경값・stack trace를 CodeDeploy/SSM 로그에 전달하지 않는다.
            System.out.println("ATTACHMENT_SERVER_QA_SETUP_FAILED=" + error.getClass().getSimpleName());
        }
        if (!passed) System.exit(1);
    }

    static List<Sample> selectSamples() {
        return List.of(
                new Sample("PDF-SDM-2026", "PBLN_000000000124628", "FILE_000000000765684", 4, "PDF", "UNKNOWN"),
                new Sample("PDF-SEMAS-2026", "PBLN_000000000120120", "FILE_000000000749023", 0, "PDF", "NOTICE"),
                new Sample("HWP-ANYANG-2026", "PBLN_000000000117918", "FILE_000000000742032", 0, "HWP", "NOTICE"),
                new Sample("HWPX-SDM-2026", "PBLN_000000000124628", "FILE_000000000765683", 1, "HWPX", "NOTICE"));
    }

    private static Map<String, Object> selectSampleResult(Sample sample, Path temporary,
            IsolatedAttachmentExtractor extractor, AnnouncementSourceClassificationRuleSet rules, String ruleHash) throws Exception {
        Path work = Files.createTempDirectory(temporary, "sample-");
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("caseId", sample.caseId());
        report.put("checkedAt", Instant.now().toString());
        report.put("ruleSource", "BUILD_MACHINE_DRAFT_SNAPSHOT");
        report.put("ruleHash", ruleHash);
        report.put("ruleReleaseCode", rules.releaseCode());
        report.put("evaluationScope", "SINGLE_SELECTED_FILE_DIAGNOSTIC");
        report.put("entireNoticeAttachmentSetVerified", false);
        report.put("productionWriteCount", 0);
        report.put("passed", false);
        String stage = "DETAIL_DOWNLOAD";
        try {
            var download = new AttachmentPinnedDownloadClient();
            Path detail = work.resolve("detail.bin");
            download.selectDownload(URI.create(HOST + "/sii/siia/selectSIIA200Detail.do?pblancId=" + sample.noticeId()),
                    Set.of("www.bizinfo.go.kr"), detail, 1024 * 1024);
            var page = Jsoup.parse(Files.readString(detail, StandardCharsets.UTF_8), HOST);
            Files.delete(detail);
            var title = page.selectFirst("meta[property=og:title]");
            if (title == null) throw new IOException("QA_TITLE_MISSING");
            stage = "TITLE_GATE";
            var base = new AnnouncementSourceClassificationEngine().selectDecision(new AnnouncementSourceClassificationInput(
                    "BIZINFO", title.attr("content"), null, null, List.of(), BodySourceCode.NONE, BodyAvailabilityCode.UNAVAILABLE), rules);
            report.put("titleStage", base.titleStageCode().name());
            if (base.semanticStatusCode() == SemanticStatusCode.EXCLUDED
                    || !Set.of(TitleStageCode.GROUP_A_MATCHED, TitleStageCode.COMBINATION_MATCHED).contains(base.titleStageCode()))
                throw new IOException("QA_TITLE_NOT_ELIGIBLE");
            String relative = "/cmm/fms/fileDown.do?atchFileId=" + sample.fileId() + "&fileSn=" + sample.fileSn();
            if (page.select("a[href]").stream().noneMatch(link -> relative.equals(link.attr("href"))))
                throw new IOException("QA_ATTACHMENT_LINK_MISSING");
            stage = "FILE_DOWNLOAD";
            Path binary = work.resolve("input.bin");
            var downloaded = download.selectDownload(URI.create(HOST + relative), Set.of("www.bizinfo.go.kr"), binary, 20L * 1024 * 1024);
            report.put("bytes", downloaded.bytes());
            report.put("binarySha256", downloaded.sha256());
            stage = "LINUX_EXTRACTION";
            JsonNode extracted = extractor.selectExtraction(binary);
            Files.delete(binary);
            String quality = extracted.path("qualityCode").asText();
            report.put("quality", quality);
            report.put("format", extracted.path("format").asText());
            if (!sample.format().equals(extracted.path("format").asText())
                    || !Set.of("COMPLETE_TEXT", "PARTIAL_TEXT", "OCR_REQUIRED").contains(quality))
                throw new IOException("QA_EXTRACTION_FAILED");
            String text = extracted.path("text").asText("");
            report.put("characterCount", text.codePointCount(0, text.length()));
            report.put("textSha256", selectHash(text.getBytes(StandardCharsets.UTF_8)));
            report.put("blockCount", extracted.path("blocks").size());
            report.put("containsSmallBusinessKeyword", text.contains("소상공인"));
            if (sample.role().equals("NOTICE") && !text.contains("소상공인")) throw new IOException("QA_KOREAN_TEXT_MISSING");
            List<Block> blocks = new ArrayList<>();
            for (JsonNode block : extracted.path("blocks")) blocks.add(new Block(block.path("index").asInt(),
                    block.path("startOffset").asInt(), block.path("endOffset").asInt(),
                    block.path("evidenceScopeId").asText(), block.path("scopeReliable").asBoolean()));
            stage = "CLASSIFICATION";
            var decision = new AnnouncementAttachmentClassificationEngine().selectDecision(new Input(base, rules,
                    true, "FOUND", true, List.of(new FileInput(UUID.randomUUID(), UUID.randomUUID(), sample.role(),
                    quality, text, blocks, null)), null, List.of()));
            report.put("status", decision.status());
            report.put("reason", decision.reason());
            report.put("warningCodes", decision.warnings());
            report.put("matchCount", decision.matches().size());
            if ("EXCLUDED".equals(decision.status()) || ((!"COMPLETE_TEXT".equals(quality) || "UNKNOWN".equals(sample.role()))
                    && !"REVIEW_REQUIRED".equals(decision.status()))) throw new IOException("QA_DECISION_INVALID");
            report.put("passed", true);
        } catch (Exception error) {
            report.put("failedStage", stage);
            report.put("errorType", error.getClass().getSimpleName());
        } finally {
            try (var entries = Files.walk(work)) {
                for (Path path : entries.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
            }
            report.put("originalFilesRemoved", !Files.exists(work));
        }
        return report;
    }

    private static String selectHash(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }
    record Sample(String caseId, String noticeId, String fileId, int fileSn, String format, String role) { }
}
