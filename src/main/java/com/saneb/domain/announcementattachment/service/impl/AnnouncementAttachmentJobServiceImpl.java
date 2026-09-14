package com.saneb.domain.announcementattachment.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.common.error.ApiException;
import com.saneb.common.error.ErrorCode;
import com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentJobDao;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentJobService;
import com.saneb.domain.announcementattachment.vo.AttachmentExecutionSnapshot;
import com.saneb.domain.announcementattachment.vo.AttachmentFailureCode;
import com.saneb.domain.announcementattachment.vo.AttachmentJobInsertCommand;
import com.saneb.domain.announcementattachment.vo.AttachmentJobReservation;
import com.saneb.domain.announcementattachment.vo.AttachmentJobRow;
import com.saneb.domain.announcementattachment.vo.AttachmentPolicyRow;
import com.saneb.domain.announcementattachment.vo.AttachmentResourceLease;
import com.saneb.domain.announcementattachment.vo.AttachmentSourceContextRow;
import com.saneb.domain.announcementattachment.vo.AttachmentWorkerSourceRow;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnnouncementAttachmentJobServiceImpl implements AnnouncementAttachmentJobService {
    private static final int JOB_LEASE_SECONDS = 120;
    // 30초 외부 처리 deadline 뒤 process/stream 정리에 필요한 여유를 포함한다.
    private static final int RESOURCE_LEASE_SECONDS = 45;
    private static final long SOURCE_BYTE_HARD_CAP = 80L * 1024 * 1024;
    private final AnnouncementAttachmentJobDao dao;
    private final ObjectMapper mapper;

    public AnnouncementAttachmentJobServiceImpl(AnnouncementAttachmentJobDao dao, ObjectMapper mapper) {
        this.dao = dao;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public AttachmentJobRow insertAttachmentJob(AttachmentJobReservation request) {
        validateRequest(request);
        String requestHash = selectHash(selectJson(request));
        AttachmentJobRow existing = dao.selectIdempotentJobDetails(request.idempotencyKey());
        if (existing != null) return selectSameRequest(existing, requestHash);

        AttachmentSourceContextRow source = dao.selectSourceContextDetailsForUpdate(request.sourceId());
        if (source == null) throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND,
                "첨부를 수집할 원문을 찾을 수 없습니다.");
        // 같은 source의 동시 재요청은 잠금 대기 중 첫 transaction이 예약했을 수 있다.
        existing = dao.selectIdempotentJobDetails(request.idempotencyKey());
        if (existing != null) return selectSameRequest(existing, requestHash);
        if (!"PRODUCTION".equals(source.dataPurposeCode()) || "EXCLUDED".equals(source.semanticStatusCode())
                || source.baseEvaluationId() == null || source.titleStageCode() == null
                || !Set.of("GROUP_A_MATCHED", "COMBINATION_MATCHED").contains(source.titleStageCode())) {
            throw new ApiException(ErrorCode.ANNOUNCEMENT_SOURCE_NOT_CONVERTIBLE, HttpStatus.CONFLICT,
                    "제목 수집 기준을 통과한 운영 원문만 첨부 수집을 예약할 수 있습니다.");
        }
        if (!request.expectedBaseDecisionId().equals(source.baseEvaluationId())
                || request.expectedSourceVersion() != source.sourceVersion()
                || request.expectedAttachmentVersion() != source.attachmentVersion()) throw selectVersionConflict();
        AttachmentPolicyRow policy = dao.selectPolicyDetails(request.policyId());
        long byteBudget = validatePolicy(policy, source, request.execution(), request.collectionRunId());
        if (request.bindNewSource() && (request.collectionRunId()==null || !"ENFORCE".equals(policy.modeCode())
                || !dao.selectNewSourceBindingAllowed(source.sourceId(),request.collectionRunId())))
            throw new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_VERSION_CONFLICT,HttpStatus.CONFLICT,
                    "같은 수집 실행에서 새로 저장한 미연결 원문만 신규 ENFORCE 검수로 연결할 수 있습니다.");
        if (Boolean.TRUE.equals(source.attachmentReviewRequired())
                && !request.policyId().equals(source.attachmentPolicyId())) {
            throw new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_VERSION_CONFLICT, HttpStatus.CONFLICT,
                    "이미 적용된 첨부 정책이 다릅니다. 기존 검수와 정책 복구 절차를 먼저 확인하세요.");
        }
        UUID jobId = UUID.randomUUID();
        int inserted = dao.insertAttachmentJob(new AttachmentJobInsertCommand(
                jobId, source.sourceId(), source.contentVersionId(), source.baseEvaluationId(),
                source.ruleReleaseId(), policy.policyId(),
                dao.selectNextGeneration(source.sourceId(), source.contentVersionId(), policy.policyId()),
                source.sourceVersion(), source.attachmentVersion() + 1,
                request.idempotencyKey(), requestHash, selectJson(request.execution()), byteBudget,request.collectionRunId(),
                request.bindNewSource() || Boolean.TRUE.equals(source.attachmentReviewRequired()),
                Boolean.TRUE.equals(source.attachmentReviewRequired()),source.attachmentPolicyId(),source.currentAttachmentEvaluationId(),
                dao.selectCurrentConfirmationId(source.sourceId())));
        if (inserted != 1) {
            existing = dao.selectIdempotentJobDetails(request.idempotencyKey());
            if (existing != null) return selectSameRequest(existing, requestHash);
            throw new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_VERSION_CONFLICT, HttpStatus.CONFLICT,
                    "이 원문에는 진행 중인 첨부 작업이 있습니다. 기존 작업의 상태를 확인하세요.");
        }
        if (dao.updateAttachmentSourceVersion(source.sourceId(), source.attachmentVersion()) != 1)
            throw selectVersionConflict();
        dao.updateAttachmentEvaluationsStale(source.sourceId());
        if (request.bindNewSource() && dao.updateNewSourceBinding(source.sourceId(),policy.policyId(),source.attachmentVersion()+1)!=1)
            throw selectVersionConflict();
        dao.updateAttachmentConfirmationsStale(source.sourceId());
        return dao.selectJobDetails(jobId);
    }

    @Override
    @Transactional
    public Optional<AttachmentJobRow> saveNextJobClaim() {
        dao.updateExpiredJobLeases();
        AttachmentJobRow job = dao.updateNextJobLease(UUID.randomUUID(), JOB_LEASE_SECONDS);
        if (job == null) return Optional.empty();
        AttachmentSourceContextRow source = dao.selectSourceContextDetails(job.sourceId());
        if (source == null || !Objects.equals(source.baseEvaluationId(), job.baseEvaluationId())
                || !Objects.equals(source.contentVersionId(), job.contentVersionId())
                || !Objects.equals(source.ruleReleaseId(), job.ruleReleaseId())
                || !Objects.equals(source.sourceVersion(), job.expectedSourceVersion())
                || !Objects.equals(source.attachmentVersion(), job.expectedAttachmentVersion())
                || "EXCLUDED".equals(source.semanticStatusCode())) {
            dao.updateJobConflict(job.jobId(), job.leaseToken());
            return Optional.empty();
        }
        if(job.batchId()!=null && !dao.selectBatchExecutionUnchanged(job.jobId())) {
            dao.updateJobFrozenInputConflict(job.jobId(),job.leaseToken());
            return Optional.empty();
        }
        return Optional.of(job);
    }

    @Override
    @Transactional
    public boolean saveJobHeartbeat(UUID jobId, UUID leaseToken) {
        return dao.updateJobHeartbeat(jobId, leaseToken, JOB_LEASE_SECONDS) == 1;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AttachmentWorkerSourceRow> selectWorkerSourceDetails(UUID jobId, UUID leaseToken) {
        return Optional.ofNullable(dao.selectWorkerSourceDetails(jobId, leaseToken));
    }

    @Override @Transactional(readOnly = true)
    public boolean selectExternalExecutionAllowed(UUID jobId, UUID leaseToken) {
        return dao.selectExternalExecutionAllowed(jobId, leaseToken);
    }

    @Override @Transactional
    public boolean saveJobDeferred(UUID jobId, UUID leaseToken, boolean requestStarted) {
        return dao.updateJobDeferred(jobId, leaseToken, requestStarted) == 1;
    }

    @Override
    @Transactional
    public boolean saveJobFailure(UUID jobId, UUID leaseToken, AttachmentFailureCode errorCode) {
        Objects.requireNonNull(errorCode, "고정 실패 코드가 필요합니다.");
        return dao.updateJobFailure(jobId, leaseToken, errorCode.retryable(), errorCode.name(),
                ThreadLocalRandom.current().nextInt(11)) == 1;
    }

    @Override
    @Transactional
    public boolean saveDownloadBytes(UUID jobId, UUID leaseToken, long bytes) {
        if (bytes <= 0 || bytes > SOURCE_BYTE_HARD_CAP)
            throw new IllegalArgumentException("다운로드 사용량은 1바이트 이상, 공고별 80 MiB 상한 이하여야 합니다.");
        return dao.updateJobDownloadBudget(jobId, leaseToken, bytes) == 1;
    }

    @Override
    @Transactional
    public Optional<AttachmentResourceLease> saveDownloadLease(UUID jobId, UUID leaseToken, String hostHash) {
        if (hostHash == null || !hostHash.matches("[0-9a-f]{64}"))
            throw new IllegalArgumentException("기관별 자원 예약에는 검증한 host의 SHA-256이 필요합니다.");
        UUID global = UUID.randomUUID();
        boolean reserved = false;
        for (int slot = 1; slot <= 2; slot++) {
            if (dao.insertResourceLease("DOWNLOAD", "GLOBAL", slot, jobId, leaseToken, global,
                    RESOURCE_LEASE_SECONDS) == 1) { reserved = true; break; }
        }
        if (!reserved) return Optional.empty();
        UUID host = UUID.randomUUID();
        if (dao.insertResourceLease("HOST", hostHash, 1, jobId, leaseToken, host, RESOURCE_LEASE_SECONDS) != 1) {
            dao.deleteResourceLease(global, jobId, leaseToken);
            return Optional.empty();
        }
        return Optional.of(new AttachmentResourceLease(jobId, leaseToken, List.of(global, host)));
    }

    @Override
    @Transactional
    public Optional<AttachmentResourceLease> saveExtractionLease(UUID jobId, UUID leaseToken) {
        UUID resource = UUID.randomUUID();
        if (dao.insertResourceLease("EXTRACTION", "GLOBAL", 1, jobId, leaseToken, resource,
                RESOURCE_LEASE_SECONDS) != 1) return Optional.empty();
        return Optional.of(new AttachmentResourceLease(jobId, leaseToken, List.of(resource)));
    }

    @Override
    @Transactional
    public void deleteResourceLease(AttachmentResourceLease lease) {
        for (UUID token : lease.resourceTokens()) dao.deleteResourceLease(token, lease.jobId(), lease.jobLeaseToken());
    }

    private long validatePolicy(AttachmentPolicyRow policy, AttachmentSourceContextRow source,
                                AttachmentExecutionSnapshot execution, UUID collectionRunId) {
        boolean frozen=collectionRunId!=null && policy!=null
                && dao.selectFrozenRunPolicyMatches(collectionRunId,policy.policyId(),source.ruleReleaseId());
        if (collectionRunId!=null && !frozen) throw selectVersionConflict();
        if (policy == null || !("ACTIVE".equals(policy.policyStatusCode()) || (frozen && "RETIRED".equals(policy.policyStatusCode())))
                || !("ACTIVE".equals(policy.releaseStatusCode()) || (frozen && "RETIRED".equals(policy.releaseStatusCode()))) || "OFF".equals(policy.modeCode())) {
            throw new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_POLICY_NOT_ACTIVE, HttpStatus.CONFLICT,
                    "활성 키워드 규칙과 첨부 수집 정책이 필요합니다. OFF 정책으로는 수집을 예약할 수 없습니다.");
        }
        if (!policy.ruleReleaseId().equals(source.ruleReleaseId()))
            throw new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_BASE_RECLASSIFICATION_REQUIRED, HttpStatus.CONFLICT,
                    "현재 제목·본문 판정과 첨부 정책의 규칙 버전이 다릅니다. 승인된 기본 재분류를 먼저 수행하세요.");
        try {
            JsonNode manifest = mapper.readTree(policy.profileManifestJson());
            boolean allowed = false;
            for (JsonNode profile : manifest) {
                if (source.providerCode().equals(profile.path("providerCode").asText())
                        && execution.profileCode().equals(profile.path("profileCode").asText())
                        && execution.profileHash().equals(profile.path("profileHash").asText())) allowed = true;
            }
            JsonNode settings = mapper.readTree(policy.settingsJson());
            if (!allowed || !execution.selectRoleRulesCurrent()
                    || !Objects.equals(execution.roleRuleVersion(),settings.path("roleRuleVersion").asText(null))
                    || !Objects.equals(execution.roleRulesHash(),settings.path("roleRulesHash").asText(null))
                    || !execution.engineVersion().equals(settings.path("engineVersion").asText())
                    || !execution.extractorVersion().equals(settings.path("extractorVersion").asText())
                    || !execution.extractorConfigHash().equals(settings.path("extractorConfigHash").asText())) {
                throw new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_PROFILE_REQUIRED, HttpStatus.CONFLICT,
                        "출처 프로필 또는 추출기 버전이 게시된 정책과 일치하지 않습니다. 정책 검증이 필요합니다.");
            }
            JsonNode budgetNode = settings.path("maximumSourceBytes");
            if (!budgetNode.isIntegralNumber() || !budgetNode.canConvertToLong()) throw selectInvalidPolicy();
            long budget = budgetNode.longValue();
            if (budget < 1 || budget > SOURCE_BYTE_HARD_CAP) throw selectInvalidPolicy();
            return budget;
        } catch (JsonProcessingException exception) {
            throw selectInvalidPolicy();
        }
    }

    private void validateRequest(AttachmentJobReservation request) {
        if (request == null || request.sourceId() == null || request.policyId() == null
                || request.expectedBaseDecisionId() == null || request.idempotencyKey() == null
                || request.execution() == null || request.expectedSourceVersion() < 0
                || request.expectedAttachmentVersion() < 0)
            throw new ApiException(ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST,
                    "원문·정책·현재 판정 ID, 멱등 키, 실행 버전과 0 이상의 원문·첨부 버전이 필요합니다.");
    }

    private AttachmentJobRow selectSameRequest(AttachmentJobRow existing, String hash) {
        if (!hash.equals(existing.requestHash())) throw new ApiException(
                ErrorCode.ANNOUNCEMENT_ATTACHMENT_VERSION_CONFLICT, HttpStatus.CONFLICT,
                "같은 멱등 키로 다른 첨부 수집을 요청할 수 없습니다. 새 요청에는 새 키를 사용하세요.");
        return existing;
    }

    private ApiException selectVersionConflict() {
        return new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_VERSION_CONFLICT, HttpStatus.CONFLICT,
                "원문 또는 첨부 버전이 변경되었습니다. 최신 판정을 확인한 뒤 다시 요청하세요.");
    }

    private ApiException selectInvalidPolicy() {
        return new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_PROFILE_REQUIRED, HttpStatus.CONFLICT,
                "첨부 정책의 다운로드 상한 또는 실행 설정이 유효하지 않습니다. 정책을 다시 검증하세요.");
    }

    private String selectJson(Object value) {
        try { return mapper.writeValueAsString(value); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("첨부 실행 계약을 직렬화하지 못했습니다."); }
    }

    private static String selectHash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException exception) { throw new IllegalStateException("SHA-256을 사용할 수 없습니다."); }
    }
}
