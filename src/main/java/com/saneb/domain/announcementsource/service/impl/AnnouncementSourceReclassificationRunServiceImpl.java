package com.saneb.domain.announcementsource.service.impl;

import com.saneb.common.error.ApiException;
import com.saneb.common.error.ErrorCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.BodyAvailabilityCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.BodySourceCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationEngine;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationInput;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationResult;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationRuleSet;
import com.saneb.domain.announcementsource.dao.AnnouncementSourceClassificationDao;
import com.saneb.domain.announcementsource.dao.AnnouncementSourceDao;
import com.saneb.domain.announcementsource.dao.AnnouncementSourceReclassificationRunDao;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceReclassificationRunActionRequest;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceReclassificationRunPreviewRequest;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceReclassificationRunResponse;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceReclassificationRunStatusRequest;
import com.saneb.domain.announcementsource.provider.AnnouncementSourceProviderItem;
import com.saneb.domain.announcementsource.service.AnnouncementSourceClassificationPersistenceService;
import com.saneb.domain.announcementsource.service.AnnouncementSourceReclassificationRunService;
import com.saneb.domain.announcementsource.service.AnnouncementSourceRuleReleaseService;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceAuditLogCommand;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceClassificationStateRow;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceContentVersionRow;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceReclassificationRunInsertCommand;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceReclassificationRunItemRow;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceReclassificationRunRow;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceSnapshotRow;
import com.saneb.domain.auth.vo.AuthenticatedUserDetails;
import com.saneb.domain.operation.dao.OperationDao;
import com.saneb.domain.operation.vo.OperationTaskInsertCommand;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class AnnouncementSourceReclassificationRunServiceImpl
        implements AnnouncementSourceReclassificationRunService {

    private static final Logger log = LoggerFactory.getLogger(AnnouncementSourceReclassificationRunServiceImpl.class);
    private static final Set<String> PROVIDER_CODES = Set.of(
            "BIZINFO", "GOV24_PUBLIC_SERVICE", "LOCAL_GOV_NOTICE"
    );
    private static final Set<String> PRESERVED_REVIEW_STATUS_CODES = Set.of(
            "ACTIVATED", "REVIEW_COMPLETED", "CONDITION_INPUT_REQUIRED",
            "DUPLICATE", "SKIPPED_ENDED", "ARCHIVED"
    );
    private static final String APPLY_CONFIRMATION = "기존 원문 재분류 적용";
    private static final String ROLLBACK_CONFIRMATION = "기존 원문 재분류 원복";

    private final AnnouncementSourceReclassificationRunDao runDao;
    private final AnnouncementSourceClassificationDao classificationDao;
    private final AnnouncementSourceDao sourceDao;
    private final AnnouncementSourceRuleReleaseService ruleReleaseService;
    private final AnnouncementSourceClassificationPersistenceService persistenceService;
    private final OperationDao operationDao;
    private final TransactionTemplate transactionTemplate;
    private final AnnouncementSourceClassificationEngine engine = new AnnouncementSourceClassificationEngine();

    public AnnouncementSourceReclassificationRunServiceImpl(
            AnnouncementSourceReclassificationRunDao runDao,
            AnnouncementSourceClassificationDao classificationDao,
            AnnouncementSourceDao sourceDao,
            AnnouncementSourceRuleReleaseService ruleReleaseService,
            AnnouncementSourceClassificationPersistenceService persistenceService,
            OperationDao operationDao,
            PlatformTransactionManager transactionManager
    ) {
        this.runDao = runDao;
        this.classificationDao = classificationDao;
        this.sourceDao = sourceDao;
        this.ruleReleaseService = ruleReleaseService;
        this.persistenceService = persistenceService;
        this.operationDao = operationDao;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    @Transactional
    public AnnouncementSourceReclassificationRunResponse insertPreviewRun(
            Authentication authentication,
            AnnouncementSourceReclassificationRunPreviewRequest request
    ) {
        UUID actorUserId = selectActorUserId(authentication);
        String providerCode = normalizeProviderCode(request.providerCode());
        ruleReleaseService.selectActiveRuleSet(request.ruleReleaseId());
        UUID runId = UUID.randomUUID();
        int inserted = runDao.insertRun(new AnnouncementSourceReclassificationRunInsertCommand(
                runId,
                request.ruleReleaseId(),
                providerCode,
                request.collectedFrom(),
                request.collectedTo(),
                Boolean.TRUE.equals(request.includeLinkedAnnouncements()),
                request.maximumCount(),
                request.batchSize(),
                sha256(request.changeReason().trim()),
                actorUserId
        ));
        if (inserted != 1) {
            throw new ApiException(
                    ErrorCode.ANNOUNCEMENT_SOURCE_RULE_RELEASE_NOT_ACTIVE,
                    HttpStatus.CONFLICT,
                    "사용 중인 규칙 버전으로만 운영 원문 영향도 미리보기를 시작할 수 있습니다."
            );
        }
        runDao.insertRunTargetItems(runId);
        runDao.updateRunTotalCount(runId);
        AnnouncementSourceReclassificationRunRow run = selectRunRow(runId);
        insertAudit(
                actorUserId,
                "ANNOUNCEMENT_SOURCE_RECLASSIFICATION_PREVIEW_CREATED",
                runId,
                "{\"ruleReleaseId\":\"" + run.ruleReleaseId()
                        + "\",\"totalCount\":" + run.totalCount()
                        + ",\"includeLinkedAnnouncements\":" + run.includeLinkedAnnouncements() + "}"
        );
        return AnnouncementSourceReclassificationRunResponse.from(run);
    }

    @Override
    public List<AnnouncementSourceReclassificationRunResponse> selectRunList() {
        return runDao.selectRunList(20).stream()
                .map(AnnouncementSourceReclassificationRunResponse::from)
                .toList();
    }

    @Override
    public AnnouncementSourceReclassificationRunResponse selectRunDetails(UUID runId) {
        return AnnouncementSourceReclassificationRunResponse.from(selectRunRow(runId));
    }

    @Override
    @Transactional
    public AnnouncementSourceReclassificationRunResponse updateApplicationStarted(
            Authentication authentication,
            UUID runId,
            AnnouncementSourceReclassificationRunActionRequest request
    ) {
        UUID actorUserId = selectActorUserId(authentication);
        requireConfirmation(request.confirmationText(), APPLY_CONFIRMATION);
        AnnouncementSourceReclassificationRunRow run = selectRunRow(runId);
        updateRunStatus(
                run,
                request.expectedVersion(),
                List.of("PREVIEW_COMPLETED"),
                "APPLY_PENDING",
                sha256(request.changeReason().trim())
        );
        insertAudit(
                actorUserId,
                "ANNOUNCEMENT_SOURCE_RECLASSIFICATION_APPLY_STARTED",
                runId,
                "{\"ruleReleaseId\":\"" + run.ruleReleaseId() + "\",\"totalCount\":" + run.totalCount() + "}"
        );
        return selectRunDetails(runId);
    }

    @Override
    @Transactional
    public AnnouncementSourceReclassificationRunResponse updateApplicationPaused(
            Authentication authentication,
            UUID runId,
            AnnouncementSourceReclassificationRunStatusRequest request
    ) {
        UUID actorUserId = selectActorUserId(authentication);
        AnnouncementSourceReclassificationRunRow run = selectRunRow(runId);
        updateRunStatus(
                run,
                request.expectedVersion(),
                List.of("APPLY_PENDING", "APPLY_RUNNING"),
                "APPLY_PAUSED",
                sha256(request.changeReason().trim())
        );
        insertAudit(actorUserId, "ANNOUNCEMENT_SOURCE_RECLASSIFICATION_APPLY_PAUSED", runId, "{}");
        return selectRunDetails(runId);
    }

    @Override
    @Transactional
    public AnnouncementSourceReclassificationRunResponse updateApplicationResumed(
            Authentication authentication,
            UUID runId,
            AnnouncementSourceReclassificationRunStatusRequest request
    ) {
        UUID actorUserId = selectActorUserId(authentication);
        AnnouncementSourceReclassificationRunRow run = selectRunRow(runId);
        updateRunStatus(
                run,
                request.expectedVersion(),
                List.of("APPLY_PAUSED"),
                "APPLY_PENDING",
                sha256(request.changeReason().trim())
        );
        insertAudit(actorUserId, "ANNOUNCEMENT_SOURCE_RECLASSIFICATION_APPLY_RESUMED", runId, "{}");
        return selectRunDetails(runId);
    }

    @Override
    @Transactional
    public AnnouncementSourceReclassificationRunResponse updateRollbackStarted(
            Authentication authentication,
            UUID runId,
            AnnouncementSourceReclassificationRunActionRequest request
    ) {
        UUID actorUserId = selectActorUserId(authentication);
        requireConfirmation(request.confirmationText(), ROLLBACK_CONFIRMATION);
        AnnouncementSourceReclassificationRunRow run = selectRunRow(runId);
        if (runDao.selectNonReversibleRunItemCount(runId) > 0) {
            throw new ApiException(
                    ErrorCode.ANNOUNCEMENT_SOURCE_NOT_CONVERTIBLE,
                    HttpStatus.CONFLICT,
                    "제목 자동 제외로 원문이 비식별 삭제된 항목이 포함되어 이 실행은 원복할 수 없습니다."
            );
        }
        updateRunStatus(
                run,
                request.expectedVersion(),
                List.of("APPLY_PAUSED", "APPLY_COMPLETED", "APPLY_PARTIAL_FAILED"),
                "ROLLBACK_PENDING",
                sha256(request.changeReason().trim())
        );
        insertAudit(
                actorUserId,
                "ANNOUNCEMENT_SOURCE_RECLASSIFICATION_ROLLBACK_STARTED",
                runId,
                "{\"appliedCount\":" + run.appliedCount() + "}"
        );
        return selectRunDetails(runId);
    }

    @Override
    public void insertNextRunBatch() {
        AnnouncementSourceReclassificationRunRow selected = runDao.selectNextRunnableRunDetails();
        if (selected == null) {
            return;
        }
        AnnouncementSourceReclassificationRunRow run = selectRunningRun(selected);
        switch (run.runStatusCode()) {
            case "PREVIEW_RUNNING" -> insertPreviewBatch(run);
            case "APPLY_RUNNING" -> insertApplicationBatch(run);
            case "ROLLBACK_RUNNING" -> insertRollbackBatch(run);
            default -> {
                // A concurrent administrator transition won the optimistic update.
            }
        }
    }

    private AnnouncementSourceReclassificationRunRow selectRunningRun(
            AnnouncementSourceReclassificationRunRow selected
    ) {
        String nextStatus = switch (selected.runStatusCode()) {
            case "PREVIEW_PENDING" -> "PREVIEW_RUNNING";
            case "APPLY_PENDING" -> "APPLY_RUNNING";
            case "ROLLBACK_PENDING" -> "ROLLBACK_RUNNING";
            default -> selected.runStatusCode();
        };
        if (!nextStatus.equals(selected.runStatusCode())) {
            int updated = runDao.updateRunStatus(
                    selected.runId(),
                    selected.rowVersion(),
                    List.of(selected.runStatusCode()),
                    nextStatus,
                    null
            );
            if (updated == 0) {
                return selected;
            }
            return selectRunRow(selected.runId());
        }
        return selected;
    }

    private void insertPreviewBatch(AnnouncementSourceReclassificationRunRow run) {
        AnnouncementSourceClassificationRuleSet ruleSet =
                ruleReleaseService.selectPublishedRuleSet(run.ruleReleaseId());
        List<AnnouncementSourceReclassificationRunItemRow> items =
                runDao.selectRunItemList(run.runId(), "PENDING", run.batchSize());
        if (items.isEmpty()) {
            finishRun(
                    run.runId(), "PREVIEW_RUNNING", "PREVIEW_COMPLETED", "PREVIEW_PARTIAL_FAILED",
                    List.of("PREVIEW_CONFLICT", "PREVIEW_FAILED")
            );
            return;
        }
        items.forEach(item -> processItem(
                item,
                "PENDING",
                "PREVIEW_CONFLICT",
                "PREVIEW_FAILED",
                () -> insertPreviewItem(item, ruleSet)
        ));
    }

    private void insertApplicationBatch(AnnouncementSourceReclassificationRunRow run) {
        AnnouncementSourceClassificationRuleSet ruleSet =
                ruleReleaseService.selectPublishedRuleSet(run.ruleReleaseId());
        List<AnnouncementSourceReclassificationRunItemRow> items =
                runDao.selectRunItemList(run.runId(), "PREVIEWED", run.batchSize());
        if (items.isEmpty()) {
            finishRun(
                    run.runId(), "APPLY_RUNNING", "APPLY_COMPLETED", "APPLY_PARTIAL_FAILED",
                    List.of("APPLY_CONFLICT", "APPLY_FAILED")
            );
            return;
        }
        items.forEach(item -> processItem(
                item,
                "PREVIEWED",
                "APPLY_CONFLICT",
                "APPLY_FAILED",
                () -> insertApplicationItem(item, run, ruleSet)
        ));
    }

    private void insertRollbackBatch(AnnouncementSourceReclassificationRunRow run) {
        List<AnnouncementSourceReclassificationRunItemRow> items =
                runDao.selectRunItemList(run.runId(), "APPLIED", run.batchSize());
        if (items.isEmpty()) {
            finishRun(
                    run.runId(), "ROLLBACK_RUNNING", "ROLLBACK_COMPLETED", "ROLLBACK_PARTIAL_FAILED",
                    List.of("ROLLBACK_CONFLICT", "ROLLBACK_FAILED")
            );
            return;
        }
        items.forEach(item -> processItem(
                item,
                "APPLIED",
                "ROLLBACK_CONFLICT",
                "ROLLBACK_FAILED",
                () -> insertRollbackItem(item)
        ));
    }

    private void processItem(
            AnnouncementSourceReclassificationRunItemRow item,
            String expectedStatusCode,
            String conflictStatusCode,
            String failureStatusCode,
            Runnable action
    ) {
        try {
            transactionTemplate.executeWithoutResult(status -> action.run());
        } catch (RuntimeException exception) {
            String nextStatus = isConflict(exception) ? conflictStatusCode : failureStatusCode;
            String errorCode = selectErrorCode(exception);
            String errorMessage = selectSafeErrorMessage(exception);
            transactionTemplate.executeWithoutResult(status -> runDao.updateItemFailure(
                    item.itemId(), expectedStatusCode, nextStatus, errorCode, errorMessage
            ));
            log.error(
                    "공고 재분류 실행 항목 처리에 실패했습니다. runId={}, sourceId={}, phase={}, errorCode={}",
                    item.runId(), item.sourceId(), expectedStatusCode, errorCode, exception
            );
        }
    }

    private void insertPreviewItem(
            AnnouncementSourceReclassificationRunItemRow item,
            AnnouncementSourceClassificationRuleSet ruleSet
    ) {
        SourceContext context = selectCurrentSourceContext(item);
        AnnouncementSourceClassificationResult result = selectResult(context, ruleSet);
        if (runDao.updateItemPreviewed(
                item.itemId(),
                result.semanticStatusCode().name(),
                result.reasonCode().name(),
                selectPredictionHash(result)
        ) != 1) {
            throw versionConflict();
        }
    }

    private void insertApplicationItem(
            AnnouncementSourceReclassificationRunItemRow item,
            AnnouncementSourceReclassificationRunRow run,
            AnnouncementSourceClassificationRuleSet ruleSet
    ) {
        SourceContext context = selectCurrentSourceContext(item);
        AnnouncementSourceClassificationResult result = selectResult(context, ruleSet);
        if (!selectPredictionHash(result).equals(item.predictionHash())) {
            throw new ApiException(
                    ErrorCode.ANNOUNCEMENT_SOURCE_VERSION_CONFLICT,
                    HttpStatus.CONFLICT,
                    "미리보기 이후 판정 결과가 달라졌습니다. 새 영향도 미리보기를 실행해 주세요."
            );
        }
        AnnouncementSourceProviderItem providerItem = selectProviderItem(context, result);
        String nextReviewStatus = selectNextReviewStatus(context.source(), result);
        UUID evaluationId = persistenceService.saveExistingContentEvaluation(
                item.sourceId(),
                item.contentVersionId(),
                run.ruleReleaseId(),
                providerItem,
                result,
                nextReviewStatus,
                item.expectedClassificationVersion()
        );
        insertOperationalReviewTaskWhenRequired(run.requestedBy(), item.sourceId(), result);
        if (runDao.updateItemApplied(
                item.itemId(), evaluationId, item.expectedClassificationVersion() + 1
        ) != 1) {
            throw versionConflict();
        }
    }

    private void insertRollbackItem(AnnouncementSourceReclassificationRunItemRow item) {
        AnnouncementSourceClassificationStateRow state =
                classificationDao.selectClassificationStateDetails(item.sourceId());
        if (state == null
                || !item.appliedEvaluationId().equals(state.decisionId())
                || !item.appliedClassificationVersion().equals(state.classificationRowVersion())) {
            throw versionConflict();
        }
        if (runDao.updateAppliedEvaluationNotCurrent(item.sourceId(), item.appliedEvaluationId()) != 1) {
            throw versionConflict();
        }
        if (item.previousEvaluationId() != null) {
            if (runDao.updatePreviousEvaluationCurrent(item.sourceId(), item.previousEvaluationId()) != 1) {
                throw versionConflict();
            }
            runDao.updateConfirmedTargetCurrentForEvaluation(item.sourceId(), item.previousEvaluationId());
            runDao.updateConfirmedSupportCurrentForEvaluation(item.sourceId(), item.previousEvaluationId());
        }
        if (runDao.updateSnapshotProjectionRollback(
                item.sourceId(),
                item.previousSemanticStatusCode(),
                item.previousSemanticReasonCode(),
                item.previousSemanticMatchedKeywords(),
                item.previousReviewStatusCode(),
                item.appliedClassificationVersion()
        ) != 1) {
            throw versionConflict();
        }
        if (runDao.updateItemRolledBack(item.itemId()) != 1) {
            throw versionConflict();
        }
    }

    private SourceContext selectCurrentSourceContext(AnnouncementSourceReclassificationRunItemRow item) {
        AnnouncementSourceSnapshotRow source = sourceDao.selectSourceDetails(item.sourceId());
        AnnouncementSourceContentVersionRow content =
                classificationDao.selectLatestContentVersionDetails(item.sourceId());
        if (source == null || content == null) {
            throw new ApiException(
                    ErrorCode.RESOURCE_NOT_FOUND,
                    HttpStatus.NOT_FOUND,
                    "재분류 대상 원문 또는 불변 원문 버전을 찾을 수 없습니다."
            );
        }
        if (!item.contentVersionId().equals(content.contentVersionId())
                || !Integer.valueOf(item.expectedClassificationVersion()).equals(source.classificationRowVersion())) {
            throw versionConflict();
        }
        return new SourceContext(source, content);
    }

    private AnnouncementSourceClassificationResult selectResult(
            SourceContext context,
            AnnouncementSourceClassificationRuleSet ruleSet
    ) {
        AnnouncementSourceContentVersionRow content = context.content();
        return engine.selectDecision(
                new AnnouncementSourceClassificationInput(
                        context.source().providerCode(),
                        content.title(),
                        content.bodyText(),
                        context.source().agencyName(),
                        List.of(),
                        BodySourceCode.valueOf(content.bodySourceCode()),
                        BodyAvailabilityCode.valueOf(content.bodyAvailabilityCode())
                ),
                ruleSet
        );
    }

    private AnnouncementSourceProviderItem selectProviderItem(
            SourceContext context,
            AnnouncementSourceClassificationResult result
    ) {
        AnnouncementSourceSnapshotRow source = context.source();
        AnnouncementSourceContentVersionRow content = context.content();
        return new AnnouncementSourceProviderItem(
                source.providerCode(), source.providerNoticeId(), content.title(), source.agencyName(),
                source.applicationStartDate(), source.applicationEndDate(), source.postedAt(), source.modifiedAt(),
                content.sourceUrl(), content.bodyText(), source.inquiryText(), source.applicationMethodText(),
                source.sourceCompletenessCode(), source.missingFieldsJson(), null, source.rawHash(), List.of(), null
        ).withSemanticDecision(
                result.semanticStatusCode().name(), result.reasonCode().name(), null
        );
    }

    private String selectNextReviewStatus(
            AnnouncementSourceSnapshotRow source,
            AnnouncementSourceClassificationResult result
    ) {
        if (sourceDao.selectLinkedAnnouncementDetails(source.sourceId()) != null
                || PRESERVED_REVIEW_STATUS_CODES.contains(source.reviewStatusCode())) {
            return source.reviewStatusCode();
        }
        return "EXCLUDED".equals(result.semanticStatusCode().name()) ? "ARCHIVED" : "REVIEW_PENDING";
    }

    private void insertOperationalReviewTaskWhenRequired(
            UUID actorUserId,
            UUID sourceId,
            AnnouncementSourceClassificationResult result
    ) {
        if (!"EXCLUDED".equals(result.semanticStatusCode().name())
                || sourceDao.selectLinkedAnnouncementDetails(sourceId) == null
                || operationDao.selectOpenOperationTaskCount("GENERAL", "ANNOUNCEMENT_SOURCE", sourceId) > 0) {
            return;
        }
        operationDao.insertOperationTask(new OperationTaskInsertCommand(
                UUID.randomUUID(),
                "GENERAL",
                "HIGH",
                "재분류 제외 공고 운영 확인",
                "연결된 운영 공고의 원문이 새 규칙에서 자동 제외로 판정되었습니다. 운영 공고 상태를 수동 확인해 주세요.",
                "ANNOUNCEMENT_SOURCE",
                sourceId,
                OffsetDateTime.now().plusDays(1),
                actorUserId
        ));
    }

    private void finishRun(
            UUID runId,
            String runningStatus,
            String completedStatus,
            String partialStatus,
            List<String> phaseFailureStatusCodes
    ) {
        AnnouncementSourceReclassificationRunRow current = selectRunRow(runId);
        String nextStatus = runDao.selectRunItemStatusCount(runId, phaseFailureStatusCodes) > 0
                ? partialStatus : completedStatus;
        if (runDao.updateRunStatus(
                runId, current.rowVersion(), List.of(runningStatus), nextStatus, null
        ) != 1) {
            return;
        }
        AnnouncementSourceReclassificationRunRow completed = selectRunRow(runId);
        insertAudit(
                completed.requestedBy(),
                "ANNOUNCEMENT_SOURCE_RECLASSIFICATION_" + nextStatus,
                runId,
                "{\"totalCount\":" + completed.totalCount()
                        + ",\"acceptedCount\":" + completed.acceptedCount()
                        + ",\"reviewRequiredCount\":" + completed.reviewRequiredCount()
                        + ",\"excludedCount\":" + completed.excludedCount()
                        + ",\"conflictCount\":" + completed.conflictCount()
                        + ",\"failedCount\":" + completed.failedCount() + "}"
        );
    }

    private void updateRunStatus(
            AnnouncementSourceReclassificationRunRow run,
            int expectedVersion,
            List<String> expectedStatuses,
            String nextStatus,
            String reasonHash
    ) {
        if (run.rowVersion() != expectedVersion
                || runDao.updateRunStatus(run.runId(), expectedVersion, expectedStatuses, nextStatus, reasonHash) != 1) {
            throw versionConflict();
        }
    }

    private AnnouncementSourceReclassificationRunRow selectRunRow(UUID runId) {
        AnnouncementSourceReclassificationRunRow row = runDao.selectRunDetails(runId);
        if (row == null) {
            throw new ApiException(
                    ErrorCode.RESOURCE_NOT_FOUND,
                    HttpStatus.NOT_FOUND,
                    "공고 재분류 실행 이력을 찾을 수 없습니다."
            );
        }
        return row;
    }

    private String normalizeProviderCode(String providerCode) {
        if (providerCode == null || providerCode.isBlank()) {
            return null;
        }
        String normalized = providerCode.trim().toUpperCase(java.util.Locale.ROOT);
        if (!PROVIDER_CODES.contains(normalized)) {
            throw new ApiException(
                    ErrorCode.VALIDATION_FAILED,
                    HttpStatus.BAD_REQUEST,
                    "providerCode는 BIZINFO, GOV24_PUBLIC_SERVICE, LOCAL_GOV_NOTICE 중 하나여야 합니다."
            );
        }
        return normalized;
    }

    private void requireConfirmation(String actual, String expected) {
        if (!expected.equals(actual == null ? null : actual.trim())) {
            throw new ApiException(
                    ErrorCode.VALIDATION_FAILED,
                    HttpStatus.BAD_REQUEST,
                    "확인 문구에 '" + expected + "'을 정확히 입력해 주세요."
            );
        }
    }

    private UUID selectActorUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUserDetails details)) {
            throw new ApiException(ErrorCode.AUTH_REQUIRED, HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
        }
        return details.userId();
    }

    private String selectPredictionHash(AnnouncementSourceClassificationResult result) {
        List<String> matchKeys = new ArrayList<>(result.matches().stream()
                .map(match -> String.join(":",
                        match.ruleCode(), match.matchedRuleTerm(), match.locationCode().name(),
                        match.appliedActionCode().name(), Boolean.toString(match.maskedByProtectedMetadata())
                ))
                .toList());
        matchKeys.sort(Comparator.naturalOrder());
        String canonical = String.join("|",
                result.semanticStatusCode().name(),
                result.reasonCode().name(),
                result.titleStageCode().name(),
                result.bodyStageCode().name(),
                result.bodySourceCode().name(),
                result.bodyAvailabilityCode().name(),
                result.targetCategoryCodes().stream().map(Enum::name).sorted().collect(Collectors.joining(",")),
                result.supportTypeCodes().stream().map(Enum::name).sorted().collect(Collectors.joining(",")),
                String.join(",", matchKeys)
        );
        return sha256(canonical);
    }

    private boolean isConflict(RuntimeException exception) {
        return exception instanceof ApiException apiException
                && apiException.errorCode() == ErrorCode.ANNOUNCEMENT_SOURCE_VERSION_CONFLICT;
    }

    private String selectErrorCode(RuntimeException exception) {
        if (exception instanceof ApiException apiException) {
            return apiException.errorCode().name();
        }
        return ErrorCode.INTERNAL_ERROR.name();
    }

    private String selectSafeErrorMessage(RuntimeException exception) {
        if (exception instanceof ApiException) {
            String message = exception.getMessage();
            return message == null || message.isBlank() ? "재분류 정책 검증에 실패했습니다." : truncate(message, 500);
        }
        return "재분류 처리 중 내부 오류가 발생했습니다. 관리자 로그를 확인하세요.";
    }

    private String truncate(String value, int maximumLength) {
        return value.length() <= maximumLength ? value : value.substring(0, maximumLength);
    }

    private ApiException versionConflict() {
        return new ApiException(
                ErrorCode.ANNOUNCEMENT_SOURCE_VERSION_CONFLICT,
                HttpStatus.CONFLICT,
                "미리보기 이후 원문 또는 분류 상태가 변경되었습니다. 새 영향도 미리보기를 실행해 주세요."
        );
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 해시 알고리즘을 사용할 수 없습니다.", exception);
        }
    }

    private void insertAudit(UUID actorUserId, String actionCode, UUID resourceId, String metadataJson) {
        sourceDao.insertAuditLog(new AnnouncementSourceAuditLogCommand(
                actorUserId,
                actionCode,
                "ANNOUNCEMENT_SOURCE_RECLASSIFICATION_RUN",
                resourceId,
                "SUCCESS",
                metadataJson
        ));
    }

    private record SourceContext(
            AnnouncementSourceSnapshotRow source,
            AnnouncementSourceContentVersionRow content
    ) {
    }
}
