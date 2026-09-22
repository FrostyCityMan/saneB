package com.saneb.domain.announcementattachment.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.common.error.ApiException;
import com.saneb.common.error.ErrorCode;
import com.saneb.domain.announcementattachment.classification.AnnouncementAttachmentClassificationEngine;
import com.saneb.domain.announcementattachment.classification.AttachmentSegmentClassificationEngine;
import com.saneb.domain.announcementattachment.classification.AttachmentSegmentRoleAnalyzer;
import com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentEvaluationDao;
import com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentEvidenceDao;
import com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentJobDao;
import com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentSegmentDao;
import com.saneb.domain.announcementattachment.vo.AttachmentSegmentRows;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentEvaluationService;
import com.saneb.domain.announcementattachment.vo.AttachmentEvaluationCommands;
import com.saneb.domain.announcementattachment.vo.AttachmentEvaluationRows;
import com.saneb.domain.announcementattachment.vo.AttachmentExecutionSnapshot;
import com.saneb.domain.announcementattachment.vo.AttachmentJobRow;
import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import com.saneb.domain.announcementattachment.vo.AttachmentSetRow;
import com.saneb.domain.announcementattachment.vo.AttachmentSourceContextRow;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.*;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationResult;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationRuleSet;
import com.saneb.domain.announcementsource.service.AnnouncementSourceRuleReleaseService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/** 입력 snapshot → transaction 밖의 CPU 판정 → source/job fence 확정. 운영 활성화는 하지 않는다. */
@Service
public class AnnouncementAttachmentEvaluationServiceImpl implements AnnouncementAttachmentEvaluationService {
    private final AnnouncementAttachmentJobDao jobs;
    private final AnnouncementAttachmentEvidenceDao evidence;
    private final AnnouncementAttachmentEvaluationDao evaluations;
    private final AnnouncementAttachmentSegmentDao segments;
    private final AnnouncementSourceRuleReleaseService rules;
    private final ObjectMapper mapper;
    private final TransactionTemplate snapshot;
    private final TransactionTemplate write;

    public AnnouncementAttachmentEvaluationServiceImpl(AnnouncementAttachmentJobDao jobs,
            AnnouncementAttachmentEvidenceDao evidence, AnnouncementAttachmentEvaluationDao evaluations, AnnouncementAttachmentSegmentDao segments,
            AnnouncementSourceRuleReleaseService rules, ObjectMapper mapper, PlatformTransactionManager transactions) {
        this.jobs = jobs;
        this.evidence = evidence;
        this.evaluations = evaluations;
        this.segments = segments;
        this.rules = rules;
        this.mapper = mapper;
        snapshot = new TransactionTemplate(transactions);
        snapshot.setReadOnly(true);
        snapshot.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        snapshot.setTimeout(15);
        write = new TransactionTemplate(transactions);
        write.setTimeout(30);
    }

    @Override @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Optional<AttachmentEvaluationRows.Evaluation> saveJobEvaluation(UUID jobId, UUID leaseToken) {
        if (jobId == null || leaseToken == null) throw conflict("첨부 작업 ID와 실행 권한이 필요합니다.");
        var completed = evaluations.selectJobEvaluationDetails(jobId, leaseToken);
        if (completed != null) return Optional.of(completed);
        Prepared prepared = snapshot.execute(status -> selectPrepared(jobId, leaseToken));
        if (prepared == null) return Optional.ofNullable(evaluations.selectJobEvaluationDetails(jobId, leaseToken));
        var computed = selectComputed(prepared);
        return Objects.requireNonNull(write.execute(status -> saveDecision(prepared, leaseToken, computed)));
    }

    private Prepared selectPrepared(UUID jobId, UUID token) {
        AttachmentJobRow job = jobs.selectJobDetails(jobId);
        if (job == null || !"RUNNING".equals(job.jobStatusCode()) || !token.equals(job.leaseToken())) return null;
        AttachmentSourceContextRow source = jobs.selectSourceContextDetails(job.sourceId());
        if (!selectSameSource(source, job)) return null;
        AttachmentSetRow set = job.setId() == null ? null : evidence.selectSetDetails(job.sourceId(), job.setId());
        if (set == null || !"SEALED".equals(set.setStatus()))
            throw new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_NOT_READY, HttpStatus.CONFLICT,
                    "모든 첨부의 성공·실패 결과가 저장되고 묶음이 봉인된 뒤 판정할 수 있습니다.");
        AttachmentExecutionSnapshot execution = selectExecution(job.executionSnapshotJson());
        if (!execution.selectEngineCurrent() || !execution.selectRoleRulesCurrent())
            throw conflict("작업에 고정된 첨부 분류 엔진이 현재 실행 코드와 다릅니다.");
        var base = evaluations.selectBaseDetails(job.sourceId(), job.baseEvaluationId());
        String policyHash = evaluations.selectPolicyHash(job.policyId(), job.ruleReleaseId());
        if (base == null || base.releaseHash() == null || policyHash == null
                || !job.contentVersionId().equals(set.contentVersionId()) || !job.policyId().equals(set.policyId())
                || !execution.profileHash().equals(set.profileHash())) throw conflict("첨부 묶음과 본문·정책·프로필 버전이 다릅니다.");
        var ruleSet = rules.selectPublishedRuleSet(job.ruleReleaseId());
        var baseResult = new AnnouncementSourceClassificationResult(base.providerCode(), base.releaseCode(),
                SemanticStatusCode.valueOf(base.status()), ReasonCode.valueOf(base.reason()),
                TitleStageCode.valueOf(base.titleStage()), BodyStageCode.valueOf(base.bodyStage()),
                BodySourceCode.valueOf(base.bodySource()), BodyAvailabilityCode.valueOf(base.bodyAvailability()),
                evaluations.selectBaseTargetList(base.evaluationId()).stream().map(TargetCategoryCode::valueOf).toList(),
                evaluations.selectBaseSupportList(base.evaluationId()).stream().map(SupportTypeCode::valueOf).toList(),
                List.of(), List.of(), List.of());
        var files = evaluations.selectFileInputList(job.sourceId(), set.setId());
        if (files.size() != set.discoveredCount() || files.size() > 10) throw conflict("봉인된 파일 수와 판정 입력 수가 다릅니다.");
        var fileInputs = files.stream().map(this::selectFileInput).toList();
        var input = new AnnouncementAttachmentClassificationEngine.Input(baseResult, ruleSet, true,
                set.discoveryStatus(), Boolean.TRUE.equals(set.discoveryComplete()), fileInputs, base.agencyName(), List.of());
        String inputHash = selectHash(new InputFingerprint(1, base, baseResult, ruleSet, policyHash,
                set.manifestHash(), execution));
        return new Prepared(job, source, base, set, files, input, inputHash, execution);
    }

    private Computed selectComputed(Prepared prepared) {
        if (prepared.execution().segmentRuleVersion() == null) {
            var decision = new AnnouncementAttachmentClassificationEngine().selectDecision(prepared.input());
            return new Computed(AnnouncementAttachmentClassificationEngine.VERSION, prepared.inputHash(), decision, selectHash(decision), List.of(), List.of());
        }
        var analyzer = new AttachmentSegmentRoleAnalyzer();
        var files = new ArrayList<AttachmentSegmentClassificationEngine.FileEvidence>();
        for (var file : prepared.files()) {
            AttachmentSetEvidence.Extraction extraction = null;
            AttachmentSegmentRoleAnalyzer.Analysis analysis = null;
            if (file.extractionId() != null && file.text() != null && !file.text().isBlank()) {
                extraction = new AttachmentSetEvidence.Extraction(file.quality(), file.text(), selectBlocks(file), null, 0);
                try { analysis = analyzer.selectAnalysis(extraction); }
                catch (IllegalArgumentException exception) {
                    if ("COMPLETE_TEXT".equals(file.quality())) throw conflict("완료로 저장된 첨부의 전체 구간·block 근거가 일치하지 않습니다.");
                    // 실패 추출에 구조 근거가 부족해도 파일 자체를 판정 입력에서 제거하지 않는다.
                }
            }
            files.add(new AttachmentSegmentClassificationEngine.FileEvidence(file.fileId(), file.extractionId(), file.roleOrigin(), extraction, analysis));
        }
        var result = new AttachmentSegmentClassificationEngine().selectDecision(prepared.input(), files);
        var identities = files.stream().map(file -> new SegmentFileFingerprint(file.fileId(), file.extractionId(), file.fileRoleOrigin(), file.analysis())).toList();
        String inputHash = selectHash(new SegmentInputFingerprint(2, prepared.inputHash(), identities));
        String decisionHash = selectHash(List.of("attachment-segment-decision-v1", selectHash(result.decision()),
                result.segmentMatches().stream().map(match -> new SegmentMatchFingerprint(match.match().fileId(), match.match().extractionId(),
                        match.match().blockIndex(), match.match().startOffset(), match.match().endOffset(), match.segmentIndex(), match.segmentRole())).toList()));
        return new Computed(result.engineVersion(), inputHash, result.decision(), decisionHash, files, result.segmentMatches());
    }

    private Optional<AttachmentEvaluationRows.Evaluation> saveDecision(Prepared prepared, UUID token, Computed computed) {
        var decision = computed.decision();
        String decisionHash = computed.decisionHash();
        var source = jobs.selectSourceContextDetailsForUpdate(prepared.job().sourceId());
        var job = jobs.selectOwnedJobDetailsForUpdate(prepared.job().jobId(), token);
        if (job == null) return Optional.ofNullable(evaluations.selectJobEvaluationDetails(prepared.job().jobId(), token));
        // Agency 보호 metadata도 input이다. 본문 version이 그대로여도 변경됐다면 늦은 결과를 적용하지 않는다.
        if (!selectSameSource(source, job) || !Objects.equals(job.setId(), prepared.set().setId())
                || !Objects.equals(prepared.base(), evaluations.selectBaseDetails(job.sourceId(), job.baseEvaluationId()))) {
            jobs.updateJobConflict(job.jobId(), token);
            return Optional.empty();
        }
        var prior = evaluations.selectInputEvaluationDetails(job.sourceId(), computed.inputHash(), computed.engineVersion());
        UUID evaluationId;
        if (prior != null) {
            if (!prior.setId().equals(job.setId()) || !prior.decisionHash().equals(decisionHash))
                throw conflict("같은 판정 입력의 저장 근거가 달라졌습니다. 새 수집 세대로 다시 확인해 주세요.");
            evaluationId = prior.evaluationId();
        } else {
            var analysisIds = insertSegmentAnalyses(prepared, computed);
            var segmentIndexes = new LinkedHashMap<AnnouncementAttachmentClassificationEngine.Match,Integer>();
            for (var match : computed.segmentMatches()) segmentIndexes.put(match.match(), match.segmentIndex());
            evaluationId = UUID.randomUUID();
            requireOne(evaluations.insertEvaluation(new AttachmentEvaluationCommands.Evaluation(evaluationId, job.sourceId(),
                    job.contentVersionId(), job.baseEvaluationId(), job.setId(), job.policyId(), job.ruleReleaseId(),
                    computed.engineVersion(), computed.inputHash(), decisionHash,
                    decision.status(), decision.reason(), selectJson(decision.warnings()))));
            for (var file : prepared.files()) requireOne(evaluations.insertInput(new AttachmentEvaluationCommands.Input(
                    evaluationId, job.sourceId(), job.setId(), file.fileId(), file.extractionId(), file.role(),
                    file.extractionId() == null ? file.downloadStatus() : file.quality(), analysisIds.get(file.fileId()))));
            for (var match : decision.matches()) requireOne(evaluations.insertMatch(new AttachmentEvaluationCommands.Match(
                    evaluationId, job.sourceId(), job.setId(), match.fileId(), match.extractionId(), job.ruleReleaseId(),
                    match.keyword().groupCode(), match.keyword().ruleCode(), match.keyword().matchedRuleTerm(),
                    match.blockIndex(), match.startOffset(), match.endOffset(), match.action(), analysisIds.get(match.fileId()), segmentIndexes.get(match))));
            for (String code : decision.targetCodes()) requireOne(evaluations.insertTargetTag(evaluationId, code));
            for (String code : decision.supportCodes()) requireOne(evaluations.insertSupportTag(evaluationId, code));
        }
        // 배치는 미리보기만 저장한다. 일반 작업도 기존 검수 요구 flag·정책·base를 변경하지 않는다.
        if (job.batchId() == null) {
            evaluations.updatePreviousEvaluationsStale(job.sourceId());
            evaluations.updatePreviousConfirmationsStale(job.sourceId());
            requireOne(evaluations.updateEvaluationCurrent(evaluationId, job.sourceId()));
            requireOne(evaluations.updateSourceEvaluation(job.sourceId(), evaluationId,
                    job.expectedSourceVersion(), job.expectedAttachmentVersion()));
        }
        boolean incomplete = !Boolean.TRUE.equals(prepared.set().discoveryComplete())
                || !List.of("FOUND", "NO_FILES").contains(prepared.set().discoveryStatus())
                || prepared.files().stream().anyMatch(file -> !"SUCCEEDED".equals(file.downloadStatus()) || !"COMPLETE_TEXT".equals(file.quality()))
                || decision.warnings().contains("ATTACHMENT_CLASSIFICATION_LIMIT");
        String previewHash = selectHash(new PreviewFingerprint(job.sourceId(), job.baseEvaluationId(), job.policyId(),
                job.setId(), computed.inputHash(), decisionHash, job.expectedSourceVersion(), job.expectedAttachmentVersion()));
        requireOne(evaluations.updateJobCompleted(job.jobId(), token, evaluationId, previewHash,
                incomplete ? "PARTIAL_FAILED" : "SUCCEEDED"));
        return Optional.of(evaluations.selectEvaluationDetails(job.sourceId(), evaluationId));
    }

    private Map<UUID,UUID> insertSegmentAnalyses(Prepared prepared, Computed computed) {
        var ids = new LinkedHashMap<UUID,UUID>();
        for (var file : computed.files()) {
            var analysis = file.analysis();
            if (analysis == null) continue;
            var sourceId = prepared.job().sourceId();
            var stored = segments.selectAnalysisDetails(sourceId, file.extractionId(), analysis.analysisVersion(), analysis.rulesHash());
            if (stored == null) {
                int count = segments.insertAnalysis(new AttachmentSegmentRows.Insert(UUID.randomUUID(), sourceId, prepared.set().setId(), file.fileId(), file.extractionId(),
                        analysis.analysisVersion(), analysis.rulesHash(), analysis.textHash(), analysis.blocksHash(), selectJson(analysis)));
                if (count < 0 || count > 1) throw conflict("구간 분석 저장 건수가 예상과 다릅니다.");
                stored = segments.selectAnalysisDetails(sourceId, file.extractionId(), analysis.analysisVersion(), analysis.rulesHash());
            }
            if (stored == null || !sourceId.equals(stored.sourceId()) || !file.extractionId().equals(stored.extractionId())
                    || !file.fileId().equals(stored.fileId()) || !prepared.set().setId().equals(stored.setId())) throw conflict("구간 분석의 원문·파일·추출 결합이 다릅니다.");
            try {
                if (!analysis.equals(mapper.readValue(stored.analysisJson(), AttachmentSegmentRoleAnalyzer.Analysis.class)))
                    throw conflict("같은 버전으로 저장된 구간 분석의 근거가 다릅니다.");
            } catch (JsonProcessingException exception) { throw conflict("저장된 구간 분석을 재현할 수 없습니다."); }
            ids.put(file.fileId(), stored.id());
        }
        return ids;
    }

    private AnnouncementAttachmentClassificationEngine.FileInput selectFileInput(AttachmentEvaluationRows.File file) {
        List<AttachmentSetEvidence.Block> blocks = selectBlocks(file);
        return new AnnouncementAttachmentClassificationEngine.FileInput(file.fileId(), file.extractionId(), file.role(),
                file.quality() == null ? "FAILED" : file.quality(), file.text(), blocks.stream().map(block ->
                new AnnouncementAttachmentClassificationEngine.Block(block.index(), block.startOffset(), block.endOffset(),
                        block.evidenceScopeId(), block.scopeReliable())).toList(), file.errorCode());
    }

    private List<AttachmentSetEvidence.Block> selectBlocks(AttachmentEvaluationRows.File file) {
        try {
            List<AttachmentSetEvidence.Block> blocks = file.blocksJson() == null ? List.of() : mapper.readValue(file.blocksJson(), new TypeReference<>() { });
            if (blocks == null || blocks.stream().anyMatch(Objects::isNull)) throw conflict("저장된 첨부 block 목록이 비어 있거나 손상되었습니다.");
            return blocks;
        } catch (JsonProcessingException exception) { throw conflict("저장된 첨부 근거 위치를 해석할 수 없습니다."); }
    }

    private boolean selectSameSource(AttachmentSourceContextRow source, AttachmentJobRow job) {
        return source != null && "PRODUCTION".equals(source.dataPurposeCode()) && !"EXCLUDED".equals(source.semanticStatusCode())
                && Objects.equals(source.baseEvaluationId(), job.baseEvaluationId())
                && Objects.equals(source.contentVersionId(), job.contentVersionId())
                && Objects.equals(source.ruleReleaseId(), job.ruleReleaseId())
                && Objects.equals(source.sourceVersion(), job.expectedSourceVersion())
                && Objects.equals(source.attachmentVersion(), job.expectedAttachmentVersion());
    }
    private AttachmentExecutionSnapshot selectExecution(String json) {
        if (json == null) throw conflict("작업의 고정 실행 버전이 없습니다.");
        try { return mapper.readValue(json, AttachmentExecutionSnapshot.class); }
        catch (JsonProcessingException exception) { throw conflict("작업의 고정 실행 버전을 확인할 수 없습니다."); }
    }
    private void requireOne(int count) { if (count != 1) throw conflict("판정 근거 또는 최신 버전이 달라 저장하지 못했습니다."); }
    private ApiException conflict(String message) {
        return new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_VERSION_CONFLICT, HttpStatus.CONFLICT, message);
    }
    private String selectJson(Object value) {
        try { return mapper.writeValueAsString(value); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("첨부 판정 입력 직렬화에 실패했습니다."); }
    }
    private String selectHash(Object value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(selectJson(value).getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException exception) { throw new IllegalStateException("SHA-256을 사용할 수 없습니다."); }
    }
    private record Prepared(AttachmentJobRow job, AttachmentSourceContextRow source, AttachmentEvaluationRows.Base base,
                            AttachmentSetRow set, List<AttachmentEvaluationRows.File> files,
                            AnnouncementAttachmentClassificationEngine.Input input, String inputHash, AttachmentExecutionSnapshot execution) { }
    private record Computed(String engineVersion, String inputHash, AnnouncementAttachmentClassificationEngine.Decision decision, String decisionHash,
                            List<AttachmentSegmentClassificationEngine.FileEvidence> files, List<AttachmentSegmentClassificationEngine.MatchEvidence> segmentMatches) { }
    private record SegmentFileFingerprint(UUID fileId, UUID extractionId, String roleOrigin, AttachmentSegmentRoleAnalyzer.Analysis analysis) { }
    private record SegmentInputFingerprint(int schemaVersion, String originalInputHash, List<SegmentFileFingerprint> files) { }
    private record SegmentMatchFingerprint(UUID fileId, UUID extractionId, int blockIndex, int startOffset, int endOffset, Integer segmentIndex, String segmentRole) { }
    private record InputFingerprint(int schemaVersion, AttachmentEvaluationRows.Base base,
                                    AnnouncementSourceClassificationResult baseResult, AnnouncementSourceClassificationRuleSet rules,
                                    String policyHash, String manifestHash, AttachmentExecutionSnapshot execution) { }
    private record PreviewFingerprint(UUID sourceId, UUID baseId, UUID policyId, UUID setId,
                                      String inputHash, String decisionHash, int sourceVersion, int attachmentVersion) { }
}
