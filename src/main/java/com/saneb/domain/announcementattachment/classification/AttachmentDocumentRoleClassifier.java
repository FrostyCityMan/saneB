package com.saneb.domain.announcementattachment.classification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/** 단일 추출 텍스트의 역할 근거. 명시적으로 규칙이 고정된 정책에서만 worker가 미확정 역할에 적용한다. */
public final class AttachmentDocumentRoleClassifier {
    public static final String VERSION = "document-role-1.0.2";
    public static final String BLOCKS_HASH_VERSION = "attachment-role-blocks-v1";
    private static final int MAX_CHARACTERS = 1_000_000;
    private static final int MAX_LINES = 20_000;
    private static final int HEADER_CHARACTERS = 600;
    private static final int HEADER_LINES = 3;
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Pattern LINES = Pattern.compile("[^\\r\\n]+");
    private static final String ITEM = "(?:[○□■●※ㅇ\\-•]|[0-9]{1,2}[.)])?\\h*";
    private static final List<Rule> RULES = List.of(
            new Rule("NOTICE_HEADING", "NOTICE", ".{0,120}(?:공고문?|모집\\h*요강)", true),
            new Rule("GUIDE_HEADING", "GUIDE", ".{0,120}(?:지원|사업|신청|모집)\\h*안내(?:문|서)?", true),
            new Rule("FORM_HEADING", "FORM", ".{0,120}(?:신청서|동의서|확인서|서약서|신고서)", true),
            new Rule("REFERENCE_HEADING", "REFERENCE", ".{0,120}(?:자주\\h*묻는\\h*질문|FAQ)", true),
            new Rule("TARGET_SECTION", null, ITEM + "(?:지원\\h*대상|신청\\h*자격)\\h*(?:[:：].{1,160})?", false),
            new Rule("SUPPORT_SECTION", null, ITEM + "(?:지원\\h*내용|지원\\h*규모)\\h*(?:[:：].{1,160})?", false),
            new Rule("APPLICATION_SECTION", null, ITEM + "(?:신청\\h*기간|접수\\h*기간)\\h*(?:[:：].{1,160})?", false),
            // 표의 입력 표제는 콜론이 없을 수 있다. 설명 문장/부분 단어가 아닌 한 줄 전체만 인정한다.
            new Rule("APPLICANT_FIELD", null, ITEM + "(?:신청\\h*인|사업자\\h*등록\\h*번호|성\\h*명)\\h*(?:[:：].{0,160})?", false),
            new Rule("SIGNATURE_FIELD", null, ".{0,120}(?:\\(서명\\)|\\(인\\)|서명\\h*또는\\h*인|\\(\\h*서명\\h*또는\\h*인\\h*\\))", false),
            new Rule("QUESTION_ITEM", null, "Q[.：:]\\h*.{1,160}", false),
            new Rule("ANSWER_ITEM", null, "A[.：:]\\h*.{1,160}", false));
    public static final String RULES_HASH = hash(json(List.of(VERSION, MAX_CHARACTERS, MAX_LINES,
            HEADER_CHARACTERS, HEADER_LINES, BLOCKS_HASH_VERSION, RULES)));

    private record Rule(String code, String role, String expression, boolean heading) { }
    private record CompiledRule(Rule rule, Pattern pattern) { }
    private static final List<CompiledRule> COMPILED = RULES.stream().map(rule -> new CompiledRule(rule,
            Pattern.compile(rule.expression(), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE))).toList();
    // 고정 보은 HWPX 두 파일에서 확인한 정확한 글머리표·괄호형 표제다. 문장 속 단어 검색으로 넓히지 않는다.
    private static final List<Rule> PARENTHESIZED_SECTIONS = List.of(
            new Rule("TARGET_SECTION", null, "❍\\h*\\(\\h*(?:지원\\h*대상|신청\\h*자격)\\h*\\)\\h*(?:[:：]\\h*)?(?=[^\\r\\n]*[\\p{L}\\p{N}])\\S.{0,159}", false),
            new Rule("SUPPORT_SECTION", null, "❍\\h*\\(\\h*(?:지원\\h*내용|지원\\h*규모)\\h*\\)\\h*(?:[:：]\\h*)?(?=[^\\r\\n]*[\\p{L}\\p{N}])\\S.{0,159}", false),
            new Rule("APPLICATION_SECTION", null, "❍\\h*\\(\\h*(?:신청\\h*기간|접수\\h*기간)\\h*\\)\\h*(?:[:：]\\h*)?(?=[^\\r\\n]*[\\p{L}\\p{N}])\\S.{0,159}", false));
    static final String PARENTHESIZED_SECTIONS_HASH = hash(json(List.of("segment-parenthesized-sections-1",RULES_HASH,PARENTHESIZED_SECTIONS)));
    private static final List<CompiledRule> SEGMENT_COMPILED = java.util.stream.Stream.concat(COMPILED.stream(),
            PARENTHESIZED_SECTIONS.stream().map(rule -> new CompiledRule(rule,
                    Pattern.compile(rule.expression(),Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)))).toList();
    static final String QUARTER_NOTICE_EXPRESSION=".{0,120}(?:공고문?|모집\\h*요강)\\h*\\(\\h*[1-4]\\h*분기\\h*\\)";
    private static final Rule QUARTER_NOTICE=new Rule("NOTICE_HEADING","NOTICE",QUARTER_NOTICE_EXPRESSION,true);
    static final String QUARTER_SECTIONS_HASH=hash(json(List.of("segment-quarter-notice-1",PARENTHESIZED_SECTIONS_HASH,QUARTER_NOTICE)));
    private static final List<CompiledRule> QUARTER_COMPILED=java.util.stream.Stream.concat(SEGMENT_COMPILED.stream(),
            java.util.stream.Stream.of(new CompiledRule(QUARTER_NOTICE,Pattern.compile(QUARTER_NOTICE_EXPRESSION)))).toList();
    public record Evidence(String ruleCode, int blockIndex, int startOffset, int endOffset) { }
    public record Assessment(String ruleVersion, String rulesHash, String textHash, String blocksHash,
                             String roleCode, String reasonCode, List<Evidence> evidence) {
        public Assessment { evidence = List.copyOf(evidence); }
    }
    private record Hit(Rule rule, Evidence evidence, boolean initialHeading) { }

    public static boolean selectRulesCurrent(String version, String rulesHash) {
        return (version == null && rulesHash == null) || (VERSION.equals(version) && RULES_HASH.equals(rulesHash));
    }

    /** 파일명·URL·다른 파일·이전 수동 역할을 받지 않는다. 역할 적용 여부는 별도 저장 계약이 결정한다. */
    public Assessment selectAssessment(AttachmentSetEvidence.Extraction extraction) {
        return selectAssessment(extraction,COMPILED);
    }
    /** 새 구간 규칙 내부에서만 사용한다. 기존 파일 역할 이력과 기본 규칙의 버전·hash는 변경하지 않는다. */
    Assessment selectParenthesizedSegmentAssessment(AttachmentSetEvidence.Extraction extraction) {
        var result=selectAssessment(extraction,SEGMENT_COMPILED);
        return new Assessment("segment-parenthesized-sections-1",PARENTHESIZED_SECTIONS_HASH,
                result.textHash(),result.blocksHash(),result.roleCode(),result.reasonCode(),result.evidence());
    }
    Assessment selectQuarterSegmentAssessment(AttachmentSetEvidence.Extraction extraction) {
        var result=selectAssessment(extraction,QUARTER_COMPILED);
        return new Assessment("segment-quarter-notice-1",QUARTER_SECTIONS_HASH,
                result.textHash(),result.blocksHash(),result.roleCode(),result.reasonCode(),result.evidence());
    }
    private Assessment selectAssessment(AttachmentSetEvidence.Extraction extraction,List<CompiledRule> compiledRules) {
        if (extraction == null || !"COMPLETE_TEXT".equals(extraction.quality()))
            return result(null, null, "UNKNOWN", "COMPLETE_TEXT_REQUIRED", List.of());
        String text = extraction.text();
        if (text == null || text.isBlank() || text.indexOf('\0') >= 0 || text.length() > MAX_CHARACTERS * 2)
            throw invalid();
        int count = text.codePointCount(0, text.length());
        if (count > MAX_CHARACTERS || extraction.blocks().isEmpty() || extraction.blocks().size() > MAX_LINES) throw invalid();
        int[] codePoints = new int[text.length() + 1];
        for (int offset = 0, position = 0; offset < text.length();) {
            int next = offset + Character.charCount(text.codePointAt(offset));
            codePoints[offset] = position++;
            codePoints[next] = position;
            offset = next;
        }
        validateBlocks(text, count, extraction.blocks());
        String textHash = hash(text), blocksHash = selectBlocksHash(extraction.blocks());
        if (extraction.blocks().stream().anyMatch(block -> !block.scopeReliable()))
            return result(textHash, blocksHash, "UNKNOWN", "STRUCTURE_UNCERTAIN", List.of());
        var hits = new ArrayList<Hit>();
        var lines = LINES.matcher(text);
        int lineCount = 0, blockIndex = 0;
        while (lines.find()) {
            String line = lines.group().strip();
            if (line.isEmpty()) continue;
            if (++lineCount > MAX_LINES) return result(textHash, blocksHash, "UNKNOWN", "ROLE_ANALYSIS_LIMIT", List.of());
            int startUtf16 = lines.start() + lines.group().indexOf(line);
            int start = codePoints[startUtf16], end = codePoints[startUtf16 + line.length()];
            while (blockIndex < extraction.blocks().size() && extraction.blocks().get(blockIndex).endOffset() <= start) blockIndex++;
            if (blockIndex >= extraction.blocks().size()) throw invalid();
            var block = extraction.blocks().get(blockIndex);
            // 한 줄이 block 경계를 넘으면 단일 위치의 근거로 합성하지 않는다.
            if (start < block.startOffset() || end > block.endOffset() || line.length() > 200) continue;
            for (var compiled : compiledRules) if (compiled.pattern().matcher(line).matches()) {
                hits.add(new Hit(compiled.rule(), new Evidence(compiled.rule().code(), block.index(), start, end),
                        lineCount <= HEADER_LINES && end <= HEADER_CHARACTERS));
                if (hits.size() > 100) return result(textHash, blocksHash, "UNKNOWN", "ROLE_ANALYSIS_LIMIT", List.of());
            }
        }
        Set<String> initialRoles = new HashSet<>(), allRoles = new HashSet<>();
        for (var hit : hits) if (hit.rule().heading()) {
            allRoles.add(hit.rule().role());
            if (hit.initialHeading()) initialRoles.add(hit.rule().role());
        }
        List<Evidence> headings = hits.stream().filter(hit -> hit.rule().heading()).map(Hit::evidence).toList();
        if (allRoles.size() > 1) return result(textHash, blocksHash, "UNKNOWN", "MIXED_DOCUMENT_ROLES", headings);
        if (initialRoles.size() != 1) return result(textHash, blocksHash, "UNKNOWN", "INITIAL_HEADING_REQUIRED", headings);
        String role = initialRoles.iterator().next();
        List<String> required = switch (role) {
            case "NOTICE", "GUIDE" -> List.of("TARGET_SECTION", "SUPPORT_SECTION", "APPLICATION_SECTION");
            case "FORM" -> List.of("APPLICANT_FIELD", "SIGNATURE_FIELD");
            case "REFERENCE" -> List.of("QUESTION_ITEM", "ANSWER_ITEM");
            default -> throw invalid();
        };
        var selected = new ArrayList<Evidence>();
        selected.add(hits.stream().filter(hit -> hit.rule().heading() && hit.initialHeading()).findFirst().orElseThrow().evidence());
        for (String code : required) {
            var match = hits.stream().filter(hit -> code.equals(hit.rule().code())).findFirst();
            if (match.isEmpty()) return result(textHash, blocksHash, "UNKNOWN", "ROLE_STRUCTURE_INCOMPLETE", selected);
            selected.add(match.get().evidence());
        }
        return result(textHash, blocksHash, role, "ROLE_TEXT_STRUCTURE_MATCHED", selected);
    }

    private void validateBlocks(String text, int count, List<AttachmentSetEvidence.Block> blocks) {
        int previous = 0, utf16End = 0;
        var scopes = new HashSet<String>();
        for (int index = 0; index < blocks.size(); index++) {
            var block = blocks.get(index);
            if (block == null || block.index() != index || block.startOffset() < previous || block.endOffset() <= block.startOffset()
                    || block.endOffset() > count || block.evidenceScopeId() == null || block.evidenceScopeId().isBlank()
                    || block.evidenceScopeId().length() > 300 || !scopes.add(block.evidenceScopeId())
                    || block.locator() == null || block.locator().isBlank() || block.locator().length() > 300) throw invalid();
            int start = text.offsetByCodePoints(utf16End, block.startOffset() - previous);
            if (!text.substring(utf16End, start).isBlank()) throw invalid();
            utf16End = text.offsetByCodePoints(start, block.endOffset() - block.startOffset());
            previous = block.endOffset();
        }
        if (!text.substring(utf16End).isBlank()) throw invalid();
    }
    private Assessment result(String textHash, String blocksHash, String role, String reason, List<Evidence> evidence) {
        return new Assessment(VERSION, RULES_HASH, textHash, blocksHash, role, reason, evidence);
    }
    /** PostgreSQL과 같은 정수/UTF-8 base64 표현이다. JSON key 순서나 공백에 의존하지 않는다. */
    private String selectBlocksHash(List<AttachmentSetEvidence.Block> blocks) {
        var canonical = new StringBuilder(BLOCKS_HASH_VERSION).append('\n');
        var encoder = java.util.Base64.getEncoder();
        for (var block : blocks) canonical.append(block.index()).append(':').append(block.startOffset()).append(':')
                .append(block.endOffset()).append(':').append(encoder.encodeToString(block.evidenceScopeId().getBytes(StandardCharsets.UTF_8)))
                .append(':').append(block.scopeReliable() ? '1' : '0').append(':')
                .append(encoder.encodeToString(block.locator().getBytes(StandardCharsets.UTF_8))).append('\n');
        return hash(canonical.toString());
    }
    private static String json(Object value) {
        try { return JSON.writeValueAsString(value); }
        catch (JsonProcessingException exception) { throw invalid(); }
    }
    private static String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException exception) { throw new IllegalStateException("SHA-256을 사용할 수 없습니다."); }
    }
    private static IllegalArgumentException invalid() {
        return new IllegalArgumentException("문서 역할 판정에는 완전한 텍스트와 순서·범위·위치가 일치하는 추출 근거가 필요합니다.");
    }
}
