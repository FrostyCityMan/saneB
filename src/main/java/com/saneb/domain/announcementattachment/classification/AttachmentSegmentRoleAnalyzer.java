package com.saneb.domain.announcementattachment.classification;

import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.regex.Pattern;

/** 원문을 재배열하지 않는 독립 구간 분석기. 파일 역할·기존 evaluation은 변경하지 않는다. */
public final class AttachmentSegmentRoleAnalyzer {
    public static final String VERSION = "segment-role-1.0.0";
    public static final int MAX_SEGMENTS = 200;
    private static final int MAX_LINES = 20_000;
    private static final Pattern LINES = Pattern.compile("[^\\r\\n]+");
    // 한 줄 전체만 경계 후보다. 각 구간의 역할 확정은 기존 구조 규칙으로 다시 검증한다.
    private static final String HEADING_EXPRESSION = ".{0,120}(?:공고문?|모집\\h*요강|"
            + "(?:지원|사업|신청|모집)\\h*안내(?:문|서)?|신청서|동의서|확인서|서약서|신고서|자주\\h*묻는\\h*질문|FAQ)";
    private static final Pattern HEADING = Pattern.compile(HEADING_EXPRESSION, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    public static final String RULES_HASH = selectHash(VERSION + "\n" + MAX_SEGMENTS + "\n" + MAX_LINES + "\n"
            + HEADING_EXPRESSION + "\n" + AttachmentDocumentRoleClassifier.VERSION + "\n"
            + AttachmentDocumentRoleClassifier.RULES_HASH + "\nfull-coverage-codepoints-clipped-original-scopes-v1\n");
    // 실파일 QA 후 별도 정책/worker 연결 대상으로만 제공한다. 기본 버전과 기존 이력의 재현성을 보존한다.
    public static final String PARENTHESIZED_VERSION = "segment-role-1.0.1";
    public static final String PARENTHESIZED_RULES_HASH = selectHash(PARENTHESIZED_VERSION + "\n" + RULES_HASH + "\n"
            + AttachmentDocumentRoleClassifier.PARENTHESIZED_SECTIONS_HASH + "\n");
    public static final String QUARTER_VERSION="segment-role-1.0.2";
    private static final String QUARTER_HEADING_EXPRESSION="(?:"+HEADING_EXPRESSION+"|"+AttachmentDocumentRoleClassifier.QUARTER_NOTICE_EXPRESSION+")";
    private static final Pattern QUARTER_HEADING=Pattern.compile(QUARTER_HEADING_EXPRESSION,Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    public static final String QUARTER_RULES_HASH=selectHash(QUARTER_VERSION+"\n"+PARENTHESIZED_RULES_HASH+"\n"
            +QUARTER_HEADING_EXPRESSION+"\n"+AttachmentDocumentRoleClassifier.QUARTER_SECTIONS_HASH+"\n");

    public record Evidence(String ruleCode, int blockIndex, int startOffset, int endOffset) { }
    public record Segment(int index, int startOffset, int endOffset, String roleCode, String reasonCode,
                          List<Evidence> evidence) {
        public Segment { evidence = List.copyOf(evidence); }
    }
    public record Analysis(String analysisVersion, String rulesHash, String textHash, String blocksHash,
                           int textLength, String statusCode, String reasonCode, List<Segment> segments) {
        public Analysis { segments = List.copyOf(segments); }
    }

    public Analysis selectAnalysis(AttachmentSetEvidence.Extraction extraction) {
        return selectAnalysis(extraction,VERSION,RULES_HASH);
    }
    public Analysis selectAnalysis(AttachmentSetEvidence.Extraction extraction,String version,String rulesHash) {
        boolean parenthesized=PARENTHESIZED_VERSION.equals(version) && PARENTHESIZED_RULES_HASH.equals(rulesHash);
        boolean quarter=QUARTER_VERSION.equals(version) && QUARTER_RULES_HASH.equals(rulesHash);
        if(!quarter && !parenthesized && !(VERSION.equals(version) && RULES_HASH.equals(rulesHash)))throw invalid();
        var result=selectAnalysis(extraction,parenthesized,quarter);
        return new Analysis(version,rulesHash,result.textHash(),result.blocksHash(),result.textLength(),
                result.statusCode(),result.reasonCode(),result.segments());
    }
    private Analysis selectAnalysis(AttachmentSetEvidence.Extraction extraction,boolean parenthesized,boolean quarter) {
        if (extraction == null || extraction.text() == null) throw invalid();
        // 전체 block 정합성도 기존의 엄격한 계약으로 검증한다. 입력 quality를 성공으로 바꾸어 반환하지 않는다.
        var classifier = new AttachmentDocumentRoleClassifier();
        var validated = classifier.selectAssessment(new AttachmentSetEvidence.Extraction("COMPLETE_TEXT",
                extraction.text(), extraction.blocks(), extraction.pageCount(), extraction.durationMs()));
        String text = extraction.text();
        int length = text.codePointCount(0, text.length());
        if (!"COMPLETE_TEXT".equals(extraction.quality()))
            return selectUnresolved(validated, length, "COMPLETE_TEXT_REQUIRED");
        if (text.indexOf('\uFFFD') >= 0)
            return selectUnresolved(validated, length, "STRUCTURE_UNCERTAIN");
        int[] utf16ByPosition = new int[length + 1];
        int[] positionByUtf16 = new int[text.length() + 1];
        for (int offset = 0, position = 0; offset < text.length(); position++) {
            utf16ByPosition[position] = offset;
            positionByUtf16[offset] = position;
            offset += Character.charCount(text.codePointAt(offset));
            utf16ByPosition[position + 1] = offset;
            positionByUtf16[offset] = position + 1;
        }
        var boundaries = new ArrayList<Integer>();
        var uncertainHeadings = new ArrayList<Integer>();
        boundaries.add(0);
        var lines = LINES.matcher(text);
        int lineCount = 0, blockIndex = 0;
        boolean nonblankBefore = false;
        while (lines.find()) {
            String line = lines.group().strip();
            if (line.isEmpty()) continue;
            if (++lineCount > MAX_LINES) return selectUnresolved(validated, length, "SEGMENT_ANALYSIS_LIMIT");
            int startUtf16 = lines.start() + lines.group().indexOf(line);
            int start = positionByUtf16[startUtf16], end = positionByUtf16[startUtf16 + line.length()];
            while (blockIndex < extraction.blocks().size() && extraction.blocks().get(blockIndex).endOffset() <= start) blockIndex++;
            if (blockIndex >= extraction.blocks().size()) throw invalid();
            var block = extraction.blocks().get(blockIndex);
            if (line.length() <= 200 && (quarter?QUARTER_HEADING:HEADING).matcher(line).matches()) {
                if (start < block.startOffset() || end > block.endOffset() || !block.scopeReliable()) {
                    uncertainHeadings.add(start);
                } else if (nonblankBefore) {
                    boundaries.add(positionByUtf16[lines.start()]);
                    if (boundaries.size() > MAX_SEGMENTS) return selectUnresolved(validated, length, "SEGMENT_ANALYSIS_LIMIT");
                }
            }
            nonblankBefore = true;
        }
        boundaries.add(length);
        var segments = new ArrayList<Segment>();
        int firstBlock = 0;
        for (int index = 0; index < boundaries.size() - 1; index++) {
            int start = boundaries.get(index), end = boundaries.get(index + 1);
            var localBlocks = new ArrayList<AttachmentSetEvidence.Block>();
            var originalIndexes = new ArrayList<Integer>();
            while (firstBlock < extraction.blocks().size() && extraction.blocks().get(firstBlock).endOffset() <= start) firstBlock++;
            for (int b = firstBlock; b < extraction.blocks().size(); b++) {
                var block = extraction.blocks().get(b);
                if (block.startOffset() >= end) break;
                localBlocks.add(new AttachmentSetEvidence.Block(localBlocks.size(), Math.max(start, block.startOffset()) - start,
                        Math.min(end, block.endOffset()) - start, block.evidenceScopeId(), block.scopeReliable(), block.locator()));
                originalIndexes.add(block.index());
            }
            var localInput = new AttachmentSetEvidence.Extraction("COMPLETE_TEXT",
                    text.substring(utf16ByPosition[start], utf16ByPosition[end]), localBlocks, extraction.pageCount(), 0);
            var local = quarter ? classifier.selectQuarterSegmentAssessment(localInput)
                    : parenthesized ? classifier.selectParenthesizedSegmentAssessment(localInput) : classifier.selectAssessment(localInput);
            if (uncertainHeadings.stream().anyMatch(position -> position >= start && position < end)) {
                segments.add(new Segment(index, start, end, "UNKNOWN", "STRUCTURE_UNCERTAIN", List.of()));
                continue;
            }
            var evidence = local.evidence().stream().map(item -> new Evidence(item.ruleCode(), originalIndexes.get(item.blockIndex()),
                    start + item.startOffset(), start + item.endOffset())).toList();
            segments.add(new Segment(index, start, end, local.roleCode(), local.reasonCode(), evidence));
        }
        boolean resolved = segments.stream().noneMatch(segment -> "UNKNOWN".equals(segment.roleCode()));
        return new Analysis(VERSION, RULES_HASH, validated.textHash(), validated.blocksHash(), length,
                resolved ? "RESOLVED" : "REVIEW_REQUIRED", resolved ? "SEGMENTS_RESOLVED" : "SEGMENT_CONTEXT_REQUIRED", segments);
    }

    /** 저장하거나 종합 판정에 사용할 때 서버가 같은 입력으로 재현하여 위조/누락을 거부한다. */
    public boolean selectAnalysisValid(AttachmentSetEvidence.Extraction extraction, Analysis analysis) {
        if (analysis == null) return false;
        try { return selectAnalysis(extraction,analysis.analysisVersion(),analysis.rulesHash()).equals(analysis); }
        catch (IllegalArgumentException exception) { return false; }
    }

    private Analysis selectUnresolved(AttachmentDocumentRoleClassifier.Assessment validated, int length, String reason) {
        return new Analysis(VERSION, RULES_HASH, validated.textHash(), validated.blocksHash(), length, "REVIEW_REQUIRED", reason,
                List.of(new Segment(0, 0, length, "UNKNOWN", reason, List.of())));
    }
    private static String selectHash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException exception) { throw new IllegalStateException("SHA-256을 사용할 수 없습니다."); }
    }
    private static IllegalArgumentException invalid() {
        return new IllegalArgumentException("구간 분석에는 원문 전체와 순서·범위가 일치하는 추출 block이 필요합니다.");
    }
}
