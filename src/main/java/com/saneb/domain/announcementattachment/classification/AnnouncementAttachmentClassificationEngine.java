package com.saneb.domain.announcementattachment.classification;

import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.*;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationEngine;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationMatch;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationResult;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationRuleSet;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** 파일·문단 사이의 AND를 허용하지 않는 I/O 없는 종합 분류기. */
public final class AnnouncementAttachmentClassificationEngine {
    public static final String VERSION = "attachment-1.0.0";
    public record Block(int index, int startOffset, int endOffset, String evidenceScopeId, boolean scopeReliable) { }
    public record FileInput(UUID fileId, UUID extractionId, String role, String quality, String text,
            List<Block> blocks, String errorCode) {
        public FileInput { blocks = blocks == null ? List.of() : List.copyOf(blocks); }
    }
    public record Input(AnnouncementSourceClassificationResult base, AnnouncementSourceClassificationRuleSet rules,
            boolean sealed, String discoveryStatus, boolean discoveryComplete,
            List<FileInput> files, String agencyName, List<String> agencyAliases) {
        public Input { files = List.copyOf(files); agencyAliases = agencyAliases == null ? List.of() : List.copyOf(agencyAliases); }
    }
    public record Match(UUID fileId, UUID extractionId, int blockIndex, int startOffset, int endOffset,
            AnnouncementSourceClassificationMatch keyword, String action) { }
    public record Decision(String status, String reason, List<String> warnings, List<String> targetCodes,
            List<String> supportCodes, List<Match> matches) { }
    record RoleScope(Block block, String role) { }

    public Decision selectDecision(Input input) {
        return selectDecision(input, Map.of());
    }

    // 구간 엔진은 별도 버전/전체 원문 검증 후에만 이 경로를 호출한다. 기존 공개 진입점은 파일 역할 그대로다.
    Decision selectDecision(Input input, Map<UUID, List<RoleScope>> segmentedScopes) {
        if (input.files().size()>10) throw new IllegalArgumentException("첨부파일은 공고당 최대 10개까지 판정할 수 있습니다.");
        if (input.base().semanticStatusCode() == SemanticStatusCode.EXCLUDED
                || !Set.of(TitleStageCode.GROUP_A_MATCHED, TitleStageCode.COMBINATION_MATCHED).contains(input.base().titleStageCode()))
            throw new IllegalArgumentException("제목 수집 기준을 통과한 공고만 첨부를 판정할 수 있습니다.");
        if (!input.base().ruleReleaseCode().equals(input.rules().releaseCode()))
            throw new IllegalArgumentException("BASE_RECLASSIFICATION_REQUIRED");
        if (!input.sealed()) return selectReview("ATTACHMENT_PENDING", List.of(), List.of(), List.of(), List.of());
        List<String> warnings = new ArrayList<>();
        List<Match> matches = new ArrayList<>();
        Set<String> targets = new LinkedHashSet<>();
        Set<String> supports = new LinkedHashSet<>();
        input.base().targetCategoryCodes().forEach(code -> targets.add(code.name()));
        input.base().supportTypeCodes().forEach(code -> supports.add(code.name()));
        boolean incomplete = !input.discoveryComplete() || !Set.of("FOUND","NO_FILES").contains(input.discoveryStatus());
        if (incomplete) warnings.add("ATTACHMENT_DISCOVERY_INCOMPLETE");
        if ("NO_FILES".equals(input.discoveryStatus()) && !input.files().isEmpty())
            throw new IllegalArgumentException("첨부 없음 상태에 파일 근거가 포함되었습니다.");
        if ("FOUND".equals(input.discoveryStatus()) && input.files().isEmpty())
            throw new IllegalArgumentException("첨부 발견 상태에는 파일 근거가 필요합니다.");
        boolean context = false;
        boolean groupA = false;
        boolean groupB = false;
        boolean combination = false;
        var engine = new AnnouncementSourceClassificationEngine();
        long work=0;
        long termCount=input.rules().rules().stream().mapToLong(rule -> rule.terms().size()).sum();
        for (FileInput file : input.files()) {
            if (!Set.of("NOTICE","GUIDE","FORM","REFERENCE","UNKNOWN").contains(file.role()))
                throw new IllegalArgumentException("지원하지 않는 첨부 문서 역할입니다.");
            if ((segmentedScopes.isEmpty() || !segmentedScopes.containsKey(file.fileId())) && "UNKNOWN".equals(file.role())) {
                context = true; warnings.add("ATTACHMENT_ROLE_UNKNOWN");
            }
            if (!"COMPLETE_TEXT".equals(file.quality()) || file.text() == null || file.text().isBlank()) {
                incomplete = true;
                warnings.add(file.errorCode() == null ? "ATTACHMENT_TEXT_INCOMPLETE" : file.errorCode());
            }
            if (file.text() == null) continue;
            int total = file.text().codePointCount(0,file.text().length());
            if (total>1_000_000 || file.blocks().size()>20000) throw new IllegalArgumentException("첨부 근거 처리 한도를 초과했습니다.");
            int previousEnd = 0;
            Set<String> scopes = new LinkedHashSet<>();
            var fileScopes = segmentedScopes.isEmpty() ? null : segmentedScopes.get(file.fileId());
            if (fileScopes == null) fileScopes = file.blocks().stream().map(block -> new RoleScope(block, file.role())).toList();
            for (RoleScope roleScope : fileScopes) {
                Block block = roleScope.block();
                if (!Set.of("NOTICE","GUIDE","FORM","REFERENCE","UNKNOWN").contains(roleScope.role()))
                    throw new IllegalArgumentException("지원하지 않는 첨부 구간 역할입니다.");
                boolean primary = Set.of("NOTICE","GUIDE").contains(roleScope.role());
                if ("UNKNOWN".equals(roleScope.role())) { context = true; warnings.add("ATTACHMENT_ROLE_UNKNOWN"); }
                if (block.startOffset() < previousEnd || block.endOffset() <= block.startOffset()
                        || block.endOffset() > total || block.evidenceScopeId() == null
                        || !scopes.add(block.evidenceScopeId())) throw new IllegalArgumentException("첨부 근거 위치가 올바르지 않습니다.");
                previousEnd = block.endOffset();
                work+=(long)(block.endOffset()-block.startOffset())*Math.max(1,termCount);
                if (block.endOffset()-block.startOffset()>16384 || work>50_000_000 || matches.size()>20000) {
                    incomplete=true; warnings.add("ATTACHMENT_CLASSIFICATION_LIMIT"); continue;
                }
                String text = file.text().substring(file.text().offsetByCodePoints(0,block.startOffset()),
                        file.text().offsetByCodePoints(0,block.endOffset()));
                AnnouncementSourceClassificationEngine.ScopeResult scope;
                try { scope=engine.selectAttachmentScope(text,input.agencyName(),input.agencyAliases(),input.rules()); }
                catch (IllegalArgumentException exception) {
                    if (!"ATTACHMENT_MATCH_LIMIT".equals(exception.getMessage())) throw exception;
                    incomplete=true; warnings.add("ATTACHMENT_CLASSIFICATION_LIMIT"); continue;
                }
                for (var match : scope.matches()) matches.add(new Match(file.fileId(),file.extractionId(),block.index(),
                        block.startOffset()+match.startOffset(),block.startOffset()+match.endOffset(),match,
                        primary ? match.appliedActionCode().name() : "CONTEXT_ONLY"));
                if (!primary) continue;
                if (!block.scopeReliable()) { context = true; warnings.add("ATTACHMENT_SCOPE_UNCERTAIN"); }
                boolean negative = text.matches("(?s).*(?:지원\\s*불가|지원\\s*제외|지원\\s*대상.{0,12}제외|해당하지\\s*않).*" );
                if (negative) { context = true; warnings.add("ATTACHMENT_NEGATIVE_CONTEXT"); }
                groupA |= !scope.groupACodes().isEmpty();
                groupB |= !scope.groupBCodes().isEmpty();
                scope.targets().forEach(code -> targets.add(code.name()));
                scope.supports().forEach(code -> supports.add(code.name()));
                combination |= block.scopeReliable() && !negative && scope.combinationMatched()
                        && "COMPLETE_TEXT".equals(file.quality());
            }
            if (file.blocks().isEmpty()) { incomplete = true; warnings.add("ATTACHMENT_SCOPE_MISSING"); }
        }
        List<String> targetCodes = targets.stream().sorted().toList();
        List<String> supportCodes = supports.stream().sorted().toList();
        warnings = warnings.stream().distinct().sorted().toList();
        if (incomplete) return selectReview("ATTACHMENT_INCOMPLETE",warnings,targetCodes,supportCodes,matches);
        if (context) return selectReview("ATTACHMENT_CONTEXT_REVIEW",warnings,targetCodes,supportCodes,matches);
        if (input.base().titleStageCode() == TitleStageCode.GROUP_A_MATCHED)
            return selectReview("TITLE_GROUP_A_MATCHED",warnings,targetCodes,supportCodes,matches);
        if (Set.of(BodyStageCode.GROUP_A_MATCHED,BodyStageCode.GROUP_B_MATCHED).contains(input.base().bodyStageCode()))
            return selectReview(input.base().reasonCode().name(),warnings,targetCodes,supportCodes,matches);
        if (groupB) return selectReview("ATTACHMENT_GROUP_B_MATCHED",warnings,targetCodes,supportCodes,matches);
        if (groupA) return selectReview("ATTACHMENT_GROUP_A_MATCHED",warnings,targetCodes,supportCodes,matches);
        if ("NO_FILES".equals(input.discoveryStatus()))
            return new Decision(input.base().semanticStatusCode().name(),input.base().reasonCode().name(),warnings,targetCodes,supportCodes,matches);
        boolean completeBody = input.base().semanticStatusCode() == SemanticStatusCode.ACCEPTED
                && Set.of(BodySourceCode.PROVIDER_FULL_TEXT,BodySourceCode.DETAIL_PAGE_TEXT).contains(input.base().bodySourceCode());
        return combination || completeBody
                ? new Decision("ACCEPTED","EXTENDED_TARGET_SUPPORT_CONFIRMED",warnings,targetCodes,supportCodes,matches)
                : selectReview("EXTENDED_COMBINATION_NOT_CONFIRMED",warnings,targetCodes,supportCodes,matches);
    }
    private Decision selectReview(String reason,List<String> warnings,List<String> targets,List<String> supports,List<Match> matches) {
        return new Decision("REVIEW_REQUIRED",reason,List.copyOf(warnings),List.copyOf(targets),List.copyOf(supports),List.copyOf(matches));
    }
}
