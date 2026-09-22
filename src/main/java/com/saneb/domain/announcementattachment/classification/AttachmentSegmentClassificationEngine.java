package com.saneb.domain.announcementattachment.classification;

import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** 파일 수/식별자와 기존 A/B 정책을 보존한다. 실행 snapshot에 고정된 별도 엔진에서만 사용한다. */
public final class AttachmentSegmentClassificationEngine {
    public static final String VERSION = "attachment-segment-1.0.0";
    public record FileEvidence(UUID fileId, UUID extractionId, String fileRoleOrigin, AttachmentSetEvidence.Extraction extraction,
                               AttachmentSegmentRoleAnalyzer.Analysis analysis) { }
    public record MatchEvidence(AnnouncementAttachmentClassificationEngine.Match match, Integer segmentIndex, String segmentRole) { }
    public record Result(String engineVersion, AnnouncementAttachmentClassificationEngine.Decision decision,
                         List<MatchEvidence> segmentMatches) {
        public Result { segmentMatches = List.copyOf(segmentMatches); }
    }

    public Result selectDecision(AnnouncementAttachmentClassificationEngine.Input input, List<FileEvidence> evidence) {
        if (input == null || evidence == null || input.files().size() > 10 || evidence.size() != input.files().size()) throw invalid();
        var byId = new HashMap<UUID, FileEvidence>();
        var scopes = new HashMap<UUID, List<AnnouncementAttachmentClassificationEngine.RoleScope>>();
        var analyzer = new AttachmentSegmentRoleAnalyzer();
        boolean manualConflict = false;
        for (var item : evidence) {
            if (item == null || item.fileId() == null || byId.put(item.fileId(), item) != null
                    || item.fileRoleOrigin() == null || !Set.of("UNKNOWN", "TEXT_RULE", "PROFILE", "MANUAL").contains(item.fileRoleOrigin())) throw invalid();
        }
        var seen = new java.util.HashSet<UUID>();
        var seenExtractions = new java.util.HashSet<UUID>();
        for (var file : input.files()) {
            if (file.fileId() == null || !seen.add(file.fileId()) || file.extractionId() != null && !seenExtractions.add(file.extractionId())) throw invalid();
            var item = byId.get(file.fileId());
            if (item == null || !java.util.Objects.equals(file.extractionId(), item.extractionId())) throw invalid();
            if (item.analysis() == null) {
                // 실패/원문 없음 파일은 제거하지 않는다. 기존 엔진이 동일 입력으로 불완전 판정을 유지한다.
                if ("COMPLETE_TEXT".equals(file.quality())) throw invalid();
                continue;
            }
            if (file.extractionId() == null || item.extraction() == null || !java.util.Objects.equals(file.text(), item.extraction().text())
                    || !java.util.Objects.equals(file.quality(), item.extraction().quality())
                    || !file.blocks().equals(item.extraction().blocks().stream().map(block -> new AnnouncementAttachmentClassificationEngine.Block(
                            block.index(), block.startOffset(), block.endOffset(), block.evidenceScopeId(), block.scopeReliable())).toList())
                    || !analyzer.selectAnalysisValid(item.extraction(), item.analysis())) throw invalid();
            boolean fixedRole = Set.of("MANUAL", "PROFILE").contains(item.fileRoleOrigin());
            if (fixedRole && item.analysis().segments().stream().anyMatch(segment -> !segment.roleCode().equals(file.role()))) manualConflict = true;
            var fileScopes = new ArrayList<AnnouncementAttachmentClassificationEngine.RoleScope>();
            int firstBlock = 0;
            for (var segment : item.analysis().segments()) {
                while (firstBlock < file.blocks().size() && file.blocks().get(firstBlock).endOffset() <= segment.startOffset()) firstBlock++;
                for (int b = firstBlock; b < file.blocks().size(); b++) {
                    var block = file.blocks().get(b);
                    if (block.startOffset() >= segment.endOffset()) break;
                    // 같은 원본 block의 경계별 교집합이지 서로 다른 문단/구간을 합친 새 scope가 아니다.
                    var clipped = new AnnouncementAttachmentClassificationEngine.Block(block.index(), Math.max(block.startOffset(), segment.startOffset()),
                            Math.min(block.endOffset(), segment.endOffset()), block.evidenceScopeId() + ":segment:" + segment.index(), block.scopeReliable());
                    String role = fixedRole && !file.role().equals(segment.roleCode()) ? "UNKNOWN" : segment.roleCode();
                    fileScopes.add(new AnnouncementAttachmentClassificationEngine.RoleScope(clipped, role));
                }
            }
            scopes.put(file.fileId(), List.copyOf(fileScopes));
        }
        var decision = new AnnouncementAttachmentClassificationEngine().selectDecision(input, scopes);
        if (manualConflict) {
            var warnings = new java.util.TreeSet<>(decision.warnings()); warnings.add("ATTACHMENT_SEGMENT_ROLE_CONFLICT");
            // 불완전/진행 중 상태의 원래 우선순위는 보존하고 파일 역할과 구간 분석 충돌은 검수에 남긴다.
            String reason = Set.of("ATTACHMENT_PENDING", "ATTACHMENT_INCOMPLETE").contains(decision.reason()) ? decision.reason() : "ATTACHMENT_CONTEXT_REVIEW";
            decision = new AnnouncementAttachmentClassificationEngine.Decision("REVIEW_REQUIRED", reason, List.copyOf(warnings),
                    decision.targetCodes(), decision.supportCodes(), decision.matches());
        }
        var matches = new ArrayList<MatchEvidence>();
        for (var match : decision.matches()) {
            if (byId.get(match.fileId()).analysis() == null) {
                matches.add(new MatchEvidence(match, null, "UNKNOWN"));
                continue;
            }
            var segment = byId.get(match.fileId()).analysis().segments().stream().filter(item ->
                    match.startOffset() >= item.startOffset() && match.endOffset() <= item.endOffset()).findFirst().orElseThrow(AttachmentSegmentClassificationEngine::invalid);
            matches.add(new MatchEvidence(match, segment.index(), segment.roleCode()));
        }
        return new Result(VERSION, decision, matches);
    }
    private static IllegalArgumentException invalid() {
        return new IllegalArgumentException("구간 종합 판정에는 동일 파일·추출 원문·block 전체와 재현 가능한 구간 분석이 필요합니다.");
    }
}
