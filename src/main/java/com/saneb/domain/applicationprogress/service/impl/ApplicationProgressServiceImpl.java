/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: ApplicationProgressServiceImpl.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.applicationprogress.service.impl;

import com.saneb.common.error.ApiException;
import com.saneb.common.error.ErrorCode;
import com.saneb.common.response.PageResponse;
import com.saneb.domain.applicationprogress.dao.ApplicationProgressDao;
import com.saneb.domain.applicationprogress.dto.ApplicationProgressDetailsResponse;
import com.saneb.domain.applicationprogress.dto.ApplicationProgressStartRequest;
import com.saneb.domain.applicationprogress.dto.ApplicationProgressSummaryResponse;
import com.saneb.domain.applicationprogress.dto.ProgressActionRequest;
import com.saneb.domain.applicationprogress.dto.ProgressChecklistSaveRequest;
import com.saneb.domain.applicationprogress.dto.ProgressReceiptSaveRequest;
import com.saneb.domain.applicationprogress.dto.ProgressResultSaveRequest;
import com.saneb.domain.applicationprogress.service.ApplicationProgressService;
import com.saneb.domain.applicationprogress.vo.ApplicationActionLogCommand;
import com.saneb.domain.applicationprogress.vo.ApplicationChecklistRow;
import com.saneb.domain.applicationprogress.vo.ApplicationChecklistSaveCommand;
import com.saneb.domain.applicationprogress.vo.ApplicationProgressCreateCommand;
import com.saneb.domain.applicationprogress.vo.ApplicationProgressRow;
import com.saneb.domain.applicationprogress.vo.ApplicationProgressSearchCondition;
import com.saneb.domain.applicationprogress.vo.ApplicationStepStateCreateCommand;
import com.saneb.domain.applicationprogress.vo.ApplicationStepStateRow;
import com.saneb.domain.applicationprogress.vo.AnnouncementProgressStepRow;
import com.saneb.domain.applicationprogress.vo.AuditLogCommand;
import com.saneb.domain.applicationprogress.vo.MatchingCaseProgressRow;
import com.saneb.domain.applicationprogress.vo.ProgressReceiptCommand;
import com.saneb.domain.applicationprogress.vo.ProgressResultCommand;
import com.saneb.domain.applicationprogress.vo.StepButtonRow;
import com.saneb.domain.auth.vo.AuthenticatedUserDetails;
import com.saneb.domain.dynamicinput.dao.DynamicAnnouncementInputDao;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApplicationProgressServiceImpl implements ApplicationProgressService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final String RESOURCE_TYPE = "APPLICATION_PROGRESS";

    private static final Set<String> PROGRESS_STATUS_CODES = Set.of(
            "READY", "IN_PROGRESS", "WAITING_RESULT", "APPROVED", "REJECTED",
            "SUPPLEMENT_REQUESTED", "STOPPED", "COMPLETED"
    );
    private static final Set<String> RESULT_CODES = Set.of(
            "APPROVED", "REJECTED", "SUPPLEMENT_REQUESTED", "STOPPED"
    );
    private static final Set<String> OPERATING_ROLES = Set.of("PARTNER", "OPERATOR", "APPROVER", "REVIEWER", "ADMIN");
    private static final String ACTION_STOP_PROGRESS = "STOP_PROGRESS";

    private final ApplicationProgressDao applicationProgressDao;
    private final DynamicAnnouncementInputDao dynamicAnnouncementInputDao;

    /**
     * 객체를 생성합니다.
     *
     * @param applicationProgressDao 입력 값
     *
     * @param dynamicAnnouncementInputDao 입력 값
     */
    public ApplicationProgressServiceImpl(
            ApplicationProgressDao applicationProgressDao,
            DynamicAnnouncementInputDao dynamicAnnouncementInputDao
    ) {
        this.applicationProgressDao = applicationProgressDao;
        this.dynamicAnnouncementInputDao = dynamicAnnouncementInputDao;
    }

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param authentication 입력 값
     *
     * @param request 입력 값
     *
     * @return 처리 결과
     */
    @Override
    /**
     * 업무 데이터를 등록합니다.
     *
     * @param authentication 입력 값
     *
     * @param request 입력 값
     *
     * @return 처리 결과
     */
    @Transactional
    public ApplicationProgressDetailsResponse insertApplicationProgress(
            Authentication authentication,
            ApplicationProgressStartRequest request
    ) {
        AuthenticatedUserDetails actor = selectRequiredPrincipal(authentication);
        UUID actorUserId = actor.userId();

        ApplicationProgressRow existing = applicationProgressDao.selectApplicationProgressByMatchingCaseId(request.matchingCaseId());
        if (existing != null) {
            insertAudit(actorUserId, "APPLICATION_PROGRESS_CREATE", existing.progressId(), metadata(
                    "createdCount", "0",
                    "skippedCount", "1",
                    "reasonCode", "DUPLICATE_PROGRESS"
            ));
            return selectApplicationProgressDetails(existing.progressId());
        }

        MatchingCaseProgressRow matchingCase = applicationProgressDao.selectMatchingCaseForProgress(request.matchingCaseId());
        if (matchingCase == null) {
            throw notFound();
        }
        if (!hasProgressStartOperatingRole(actor)) {
            throw new ApiException(
                    ErrorCode.AUTH_FORBIDDEN,
                    HttpStatus.FORBIDDEN,
                    "관리자 또는 운영자만 신청 진행을 시작할 수 있습니다."
            );
        }
        if (!"MATCHED".equals(matchingCase.statusCode())) {
            throw new ApiException(
                    ErrorCode.PROGRESS_CONDITION_NOT_MET,
                    HttpStatus.CONFLICT,
                    "Only MATCHED matching cases can start application progress."
            );
        }
        if (!"FINAL".equals(matchingCase.matchingStageCode())) {
            throw new ApiException(
                    ErrorCode.PROGRESS_CONDITION_NOT_MET,
                    HttpStatus.CONFLICT,
                    "최종 매칭으로 확인된 공고만 신청 진행을 시작할 수 있습니다."
            );
        }

        List<AnnouncementProgressStepRow> activeSteps =
                applicationProgressDao.selectActiveAnnouncementProgressStepList(matchingCase.announcementId());
        if (activeSteps.isEmpty()) {
            throw new ApiException(
                    ErrorCode.PROGRESS_CONDITION_NOT_MET,
                    HttpStatus.CONFLICT,
                    "Active announcement progress step does not exist."
            );
        }

        UUID progressId = UUID.randomUUID();
        UUID firstStepId = activeSteps.getFirst().stepId();
        applicationProgressDao.insertApplicationProgress(new ApplicationProgressCreateCommand(
                progressId,
                matchingCase.matchingCaseId(),
                matchingCase.announcementId(),
                matchingCase.memberUserId(),
                firstStepId,
                actorUserId
        ));

        for (AnnouncementProgressStepRow step : activeSteps) {
            String statusCode = firstStepId.equals(step.stepId()) ? "READY" : "LOCKED";
            applicationProgressDao.insertApplicationStepState(new ApplicationStepStateCreateCommand(
                    UUID.randomUUID(),
                    progressId,
                    step.stepId(),
                    statusCode,
                    actorUserId
            ));
        }

        int updatedCount = applicationProgressDao.updateMatchingCaseStatusToProgressed(
                matchingCase.matchingCaseId(),
                actorUserId
        );
        if (updatedCount == 0) {
            throw new ApiException(
                    ErrorCode.PROGRESS_CONDITION_NOT_MET,
                    HttpStatus.CONFLICT,
                    "Matching case status was changed before progress creation."
            );
        }

        insertAudit(actorUserId, "APPLICATION_PROGRESS_CREATE", progressId, metadata(
                "createdCount", "1",
                "stepCount", String.valueOf(activeSteps.size()),
                "matchingCaseStatusCode", "PROGRESSED"
        ));
        return selectApplicationProgressDetails(progressId);
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param authentication 입력 값
     *
     * @param announcementId 입력 값
     *
     * @param memberUserId 입력 값
     *
     * @param matchingCaseId 입력 값
     *
     * @param statusCode 입력 값
     *
     * @param page 입력 값
     *
     * @param size 입력 값
     *
     * @return 처리 결과
     */
    @Override
    public PageResponse<ApplicationProgressSummaryResponse> selectApplicationProgressList(
            Authentication authentication,
            UUID announcementId,
            UUID memberUserId,
            UUID matchingCaseId,
            String statusCode,
            int page,
            int size
    ) {
        validatePageRequest(page, size);
        AuthenticatedUserDetails actor = selectRequiredPrincipal(authentication);
        UUID effectiveMemberUserId = hasOperatingRole(actor) ? memberUserId : actor.userId();
        String normalizedStatusCode = normalizeOptionalCode(statusCode);
        validateOptionalCode("statusCode", normalizedStatusCode, PROGRESS_STATUS_CODES);

        ApplicationProgressSearchCondition condition = new ApplicationProgressSearchCondition(
                announcementId,
                effectiveMemberUserId,
                matchingCaseId,
                normalizedStatusCode,
                page,
                size,
                (page - 1) * size
        );
        long totalCount = applicationProgressDao.selectApplicationProgressCount(condition);
        List<ApplicationProgressSummaryResponse> items = applicationProgressDao.selectApplicationProgressList(condition)
                .stream()
                .map(this::toSummaryResponse)
                .toList();
        return PageResponse.of(items, page, size, totalCount);
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param progressId 입력 값
     *
     * @return 처리 결과
     */
    @Override
    public ApplicationProgressDetailsResponse selectApplicationProgressDetails(UUID progressId) {
        ApplicationProgressRow row = selectApplicationProgressRow(progressId);
        return toDetailsResponse(
                row,
                applicationProgressDao.selectApplicationStepStateList(progressId),
                applicationProgressDao.selectApplicationChecklistList(progressId),
                applicationProgressDao.selectStepButtonList(progressId)
        );
    }

    /**
     * 업무 데이터를 수정합니다.
     *
     * @param authentication 입력 값
     *
     * @param progressId 입력 값
     *
     * @param stepId 입력 값
     *
     * @param request 입력 값
     *
     * @return 처리 결과
     */
    @Override
    /**
     * 업무 데이터를 수정합니다.
     *
     * @param authentication 입력 값
     *
     * @param progressId 입력 값
     *
     * @param stepId 입력 값
     *
     * @param request 입력 값
     *
     * @return 처리 결과
     */
    @Transactional
    public ApplicationProgressDetailsResponse updateProgressStepAction(
            Authentication authentication,
            UUID progressId,
            UUID stepId,
            ProgressActionRequest request
    ) {
        UUID actorUserId = selectRequiredPrincipal(authentication).userId();
        ApplicationProgressRow progress = selectApplicationProgressRow(progressId);
        ApplicationStepStateRow stepState = selectApplicationStepState(progressId, stepId);
        if (!Set.of("READY", "IN_PROGRESS").contains(stepState.statusCode())) {
            throw new ApiException(ErrorCode.PROGRESS_STEP_LOCKED, HttpStatus.CONFLICT, "Progress step is locked.");
        }

        String buttonCode = normalizeOptionalCode(request.buttonCode());
        if (buttonCode == null) {
            throw validationFailed("buttonCode is invalid.");
        }
        StepButtonRow button = applicationProgressDao.selectStepButton(stepId, buttonCode);
        if (button == null) {
            throw new ApiException(ErrorCode.PROGRESS_CONDITION_NOT_MET, HttpStatus.CONFLICT, "Step button does not exist.");
        }

        boolean stopProgress = ACTION_STOP_PROGRESS.equals(normalizeOptionalCode(button.buttonActionCode()));
        if (!stopProgress) {
            validateCurrentStepCanMove(progress, stepState);
        }

        UUID nextStepId = null;
        if (!stopProgress) {
            nextStepId = button.nextStepId() == null
                    ? applicationProgressDao.selectNextActiveStepId(progress.announcementId(), stepState.stepOrder())
                    : button.nextStepId();
            if (nextStepId != null && applicationProgressDao.selectApplicationStepState(progressId, nextStepId) == null) {
                throw new ApiException(ErrorCode.PROGRESS_CONDITION_NOT_MET, HttpStatus.CONFLICT, "Next step is not initialized.");
            }
        }

        applicationProgressDao.insertApplicationActionLog(new ApplicationActionLogCommand(
                progressId,
                stepId,
                actorUserId,
                button.buttonActionCode(),
                button.buttonCode(),
                safeActionInputJson(button.buttonCode(), request.input() != null && !request.input().isEmpty())
        ));
        applicationProgressDao.updateApplicationStepStateStatus(progressId, stepId, "COMPLETED", actorUserId);

        if (stopProgress) {
            applicationProgressDao.updateApplicationProgressCurrentStep(progressId, null, "STOPPED", actorUserId);
        } else if (nextStepId == null) {
            applicationProgressDao.updateApplicationProgressCurrentStep(progressId, null, "WAITING_RESULT", actorUserId);
        } else {
            applicationProgressDao.updateApplicationStepStateStatus(progressId, nextStepId, "READY", actorUserId);
            applicationProgressDao.updateApplicationProgressCurrentStep(progressId, nextStepId, "IN_PROGRESS", actorUserId);
        }

        insertAudit(actorUserId, "APPLICATION_PROGRESS_STEP_ACTION", progressId, metadata(
                "stepId", stepId.toString(),
                "buttonCode", button.buttonCode(),
                "actionCode", button.buttonActionCode()
        ));
        return selectApplicationProgressDetails(progressId);
    }

    /**
     * 업무 데이터를 저장합니다.
     *
     * @param authentication 입력 값
     *
     * @param progressId 입력 값
     *
     * @param stepId 입력 값
     *
     * @param request 입력 값
     *
     * @return 처리 결과
     */
    @Override
    /**
     * 업무 데이터를 저장합니다.
     *
     * @param authentication 입력 값
     *
     * @param progressId 입력 값
     *
     * @param stepId 입력 값
     *
     * @param request 입력 값
     *
     * @return 처리 결과
     */
    @Transactional
    public ApplicationProgressDetailsResponse saveProgressStepDocuments(
            Authentication authentication,
            UUID progressId,
            UUID stepId,
            ProgressChecklistSaveRequest request
    ) {
        UUID actorUserId = selectRequiredPrincipal(authentication).userId();
        selectApplicationProgressRow(progressId);
        selectApplicationStepState(progressId, stepId);

        for (ProgressChecklistSaveRequest.DocumentRequest document : nullToEmpty(request.documents())) {
            if (applicationProgressDao.selectStepDocumentBelongsToStepCount(stepId, document.stepDocumentId()) == 0) {
                throw new ApiException(
                        ErrorCode.PROGRESS_CONDITION_NOT_MET,
                        HttpStatus.CONFLICT,
                        "Step document does not belong to the progress step."
                );
            }
            applicationProgressDao.saveApplicationChecklist(new ApplicationChecklistSaveCommand(
                    progressId,
                    document.stepDocumentId(),
                    document.checked(),
                    actorUserId
            ));
        }
        applicationProgressDao.touchApplicationProgress(progressId, actorUserId);

        insertAudit(actorUserId, "APPLICATION_PROGRESS_DOCUMENTS_SAVE", progressId, metadata(
                "stepId", stepId.toString(),
                "documentCount", String.valueOf(nullToEmpty(request.documents()).size()),
                "section", "checklist"
        ));
        return selectApplicationProgressDetails(progressId);
    }

    /**
     * 업무 데이터를 수정합니다.
     *
     * @param authentication 입력 값
     *
     * @param progressId 입력 값
     *
     * @param request 입력 값
     *
     * @return 처리 결과
     */
    @Override
    /**
     * 업무 데이터를 수정합니다.
     *
     * @param authentication 입력 값
     *
     * @param progressId 입력 값
     *
     * @param request 입력 값
     *
     * @return 처리 결과
     */
    @Transactional
    public ApplicationProgressDetailsResponse updateProgressReceipt(
            Authentication authentication,
            UUID progressId,
            ProgressReceiptSaveRequest request
    ) {
        UUID actorUserId = selectRequiredPrincipal(authentication).userId();
        selectApplicationProgressRow(progressId);
        int updatedCount = applicationProgressDao.updateApplicationProgressReceipt(new ProgressReceiptCommand(
                progressId,
                trimToNull(request.receiptNo()),
                request.receiptDate(),
                actorUserId
        ));
        if (updatedCount == 0) {
            throw notFound();
        }
        insertAudit(actorUserId, "APPLICATION_PROGRESS_RECEIPT_SAVE", progressId, metadata(
                "receiptSaved", "true",
                "statusCode", "WAITING_RESULT",
                "dateProvided", "true"
        ));
        return selectApplicationProgressDetails(progressId);
    }

    /**
     * 업무 데이터를 수정합니다.
     *
     * @param authentication 입력 값
     *
     * @param progressId 입력 값
     *
     * @param request 입력 값
     *
     * @return 처리 결과
     */
    @Override
    /**
     * 업무 데이터를 수정합니다.
     *
     * @param authentication 입력 값
     *
     * @param progressId 입력 값
     *
     * @param request 입력 값
     *
     * @return 처리 결과
     */
    @Transactional
    public ApplicationProgressDetailsResponse updateProgressResult(
            Authentication authentication,
            UUID progressId,
            ProgressResultSaveRequest request
    ) {
        UUID actorUserId = selectRequiredPrincipal(authentication).userId();
        selectApplicationProgressRow(progressId);
        String resultCode = normalizeRequiredCode("resultCode", request.resultCode(), RESULT_CODES);
        if (request.receivedAmount() != null && request.receivedAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw validationFailed("receivedAmount must be zero or positive.");
        }
        if (request.receivedAmount() != null && !"APPROVED".equals(resultCode)) {
            throw validationFailed("receivedAmount can be stored only for APPROVED result.");
        }

        int updatedCount = applicationProgressDao.updateApplicationProgressResult(new ProgressResultCommand(
                progressId,
                resultCode,
                resultCode,
                trimToNull(request.resultNote()),
                request.resultDate(),
                request.receivedAmount(),
                actorUserId
        ));
        if (updatedCount == 0) {
            throw notFound();
        }
        insertAudit(actorUserId, "APPLICATION_PROGRESS_RESULT_SAVE", progressId, metadata(
                "resultCode", resultCode,
                "receivedAmountProvided", String.valueOf(request.receivedAmount() != null),
                "statusCode", resultCode
        ));
        return selectApplicationProgressDetails(progressId);
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param progressId 입력 값
     *
     * @return 처리 결과
     */
    private ApplicationProgressRow selectApplicationProgressRow(UUID progressId) {
        ApplicationProgressRow row = applicationProgressDao.selectApplicationProgressDetails(progressId);
        if (row == null) {
            throw notFound();
        }
        return row;
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param progressId 입력 값
     *
     * @param stepId 입력 값
     *
     * @return 처리 결과
     */
    private ApplicationStepStateRow selectApplicationStepState(UUID progressId, UUID stepId) {
        ApplicationStepStateRow row = applicationProgressDao.selectApplicationStepState(progressId, stepId);
        if (row == null) {
            throw new ApiException(ErrorCode.PROGRESS_STEP_LOCKED, HttpStatus.CONFLICT, "Progress step is not initialized.");
        }
        return row;
    }

    /**
     * 업무 데이터를 응답 형식으로 변환합니다.
     *
     * @param row 입력 값
     *
     * @return 처리 결과
     */
    private ApplicationProgressSummaryResponse toSummaryResponse(ApplicationProgressRow row) {
        return new ApplicationProgressSummaryResponse(
                row.progressId(),
                row.matchingCaseId(),
                row.announcementId(),
                row.memberUserId(),
                row.progressCode(),
                row.matchingCaseCode(),
                row.announcementCode(),
                row.memberUserCode(),
                row.currentStepId(),
                row.statusCode(),
                row.receiptNo(),
                row.receiptDate(),
                row.resultCode(),
                row.resultDate(),
                row.receivedAmount(),
                row.createdAt(),
                row.updatedAt()
        );
    }

    /**
     * 업무 데이터를 응답 형식으로 변환합니다.
     *
     * @param row 입력 값
     *
     * @param stepStates 입력 값
     *
     * @param checklists 입력 값
     *
     * @param stepButtons 입력 값
     *
     * @return 처리 결과
     */
    private ApplicationProgressDetailsResponse toDetailsResponse(
            ApplicationProgressRow row,
            List<ApplicationStepStateRow> stepStates,
            List<ApplicationChecklistRow> checklists,
            List<StepButtonRow> stepButtons
    ) {
        return new ApplicationProgressDetailsResponse(
                row.progressId(),
                row.matchingCaseId(),
                row.announcementId(),
                row.memberUserId(),
                row.progressCode(),
                row.matchingCaseCode(),
                row.announcementCode(),
                row.memberUserCode(),
                row.currentStepId(),
                row.statusCode(),
                row.receiptNo(),
                row.receiptDate(),
                row.resultCode(),
                row.resultNote(),
                row.resultDate(),
                row.receivedAmount(),
                row.createdAt(),
                row.updatedAt(),
                nullToEmpty(stepStates).stream().map(this::toStepStateResponse).toList(),
                nullToEmpty(checklists).stream().map(this::toChecklistResponse).toList(),
                nullToEmpty(stepButtons).stream().map(this::toStepButtonResponse).toList()
        );
    }

    /**
     * 업무 데이터를 응답 형식으로 변환합니다.
     *
     * @param row 입력 값
     *
     * @return 처리 결과
     */
    private ApplicationProgressDetailsResponse.StepStateResponse toStepStateResponse(ApplicationStepStateRow row) {
        return new ApplicationProgressDetailsResponse.StepStateResponse(
                row.stepStateId(),
                row.stepId(),
                row.stepOrder(),
                row.stepName(),
                row.guideMessage(),
                row.actionGuide(),
                row.completionConditionCode(),
                row.statusCode(),
                row.startedAt(),
                row.completedAt()
        );
    }

    /**
     * 요청 값과 업무 규칙을 검증합니다.
     *
     * @param progress 입력 값
     *
     * @param stepState 입력 값
     */
    private void validateCurrentStepCanMove(ApplicationProgressRow progress, ApplicationStepStateRow stepState) {
        UUID progressId = progress.progressId();
        UUID stepId = stepState.stepId();
        if (applicationProgressDao.selectRequiredUncheckedDocumentCount(progressId, stepId) > 0) {
            throw new ApiException(
                    ErrorCode.PROGRESS_CONDITION_NOT_MET,
                    HttpStatus.CONFLICT,
                    "필수 서류를 모두 확인한 뒤 다음 단계로 이동할 수 있습니다."
            );
        }
        if (dynamicAnnouncementInputDao.selectMissingRequiredApplicationInputCount(progressId) > 0) {
            throw new ApiException(
                    ErrorCode.PROGRESS_CONDITION_NOT_MET,
                    HttpStatus.CONFLICT,
                    "필수 입력값을 저장한 뒤 다음 단계로 이동할 수 있습니다."
            );
        }

        String completionConditionCode = normalizeOptionalCode(stepState.completionConditionCode());
        if ("RECEIPT_SAVED".equals(completionConditionCode)
                && (trimToNull(progress.receiptNo()) == null || progress.receiptDate() == null)) {
            throw new ApiException(
                    ErrorCode.PROGRESS_CONDITION_NOT_MET,
                    HttpStatus.CONFLICT,
                    "접수번호와 접수일을 저장한 뒤 다음 단계로 이동할 수 있습니다."
            );
        }
        if ("RESULT_SAVED".equals(completionConditionCode)
                && (trimToNull(progress.resultCode()) == null || progress.resultDate() == null)) {
            throw new ApiException(
                    ErrorCode.PROGRESS_CONDITION_NOT_MET,
                    HttpStatus.CONFLICT,
                    "최종 결과와 결과일을 저장한 뒤 다음 단계로 이동할 수 있습니다."
            );
        }
    }

    /**
     * 업무 데이터를 응답 형식으로 변환합니다.
     *
     * @param row 입력 값
     *
     * @return 처리 결과
     */
    private ApplicationProgressDetailsResponse.ChecklistResponse toChecklistResponse(ApplicationChecklistRow row) {
        return new ApplicationProgressDetailsResponse.ChecklistResponse(
                row.checklistId(),
                row.stepDocumentId(),
                row.stepId(),
                row.documentTypeCode(),
                Boolean.TRUE.equals(row.required()),
                Boolean.TRUE.equals(row.checked()),
                row.checkedAt(),
                row.checkedBy()
        );
    }

    /**
     * 업무 데이터를 응답 형식으로 변환합니다.
     *
     * @param row 입력 값
     *
     * @return 처리 결과
     */
    private ApplicationProgressDetailsResponse.StepButtonResponse toStepButtonResponse(StepButtonRow row) {
        return new ApplicationProgressDetailsResponse.StepButtonResponse(
                row.stepId(),
                row.buttonCode(),
                row.buttonLabel(),
                row.buttonActionCode(),
                row.nextStepId(),
                row.sortOrder()
        );
    }

    /**
     * 요청 값과 업무 규칙을 검증합니다.
     *
     * @param page 입력 값
     *
     * @param size 입력 값
     */
    private void validatePageRequest(int page, int size) {
        if (page < 1 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new ApiException(ErrorCode.INVALID_PAGE_REQUEST, HttpStatus.BAD_REQUEST, "Invalid page request.");
        }
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param authentication 입력 값
     *
     * @return 처리 결과
     */
    private AuthenticatedUserDetails selectRequiredPrincipal(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ApiException(ErrorCode.AUTH_REQUIRED, HttpStatus.UNAUTHORIZED, "Authentication is required.");
        }
        if (authentication.getPrincipal() instanceof AuthenticatedUserDetails principal) {
            return principal;
        }
        throw new ApiException(
                ErrorCode.AUTH_REQUIRED,
                HttpStatus.UNAUTHORIZED,
                "Database backed authentication is required."
        );
    }

    /**
     * 조건 충족 여부를 확인합니다.
     *
     * @param actor 입력 값
     *
     * @return 처리 결과
     */
    private boolean hasOperatingRole(AuthenticatedUserDetails actor) {
        return actor.roles().stream().anyMatch(OPERATING_ROLES::contains);
    }

    /**
     * 조건 충족 여부를 확인합니다.
     *
     * @param actor 입력 값
     *
     * @return 처리 결과
     */
    private boolean hasProgressStartOperatingRole(AuthenticatedUserDetails actor) {
        return actor.roles().stream().anyMatch(role -> Set.of("OPERATOR", "ADMIN").contains(role));
    }

    /**
     * 입력 값을 표준 형식으로 정규화합니다.
     *
     * @param fieldName 입력 값
     *
     * @param value 입력 값
     *
     * @param allowedValues 입력 값
     *
     * @return 처리 결과
     */
    private String normalizeRequiredCode(String fieldName, String value, Set<String> allowedValues) {
        String normalized = normalizeOptionalCode(value);
        if (normalized == null || !allowedValues.contains(normalized)) {
            throw validationFailed(fieldName + " is invalid.");
        }
        return normalized;
    }

    /**
     * 요청 값과 업무 규칙을 검증합니다.
     *
     * @param fieldName 입력 값
     *
     * @param value 입력 값
     *
     * @param allowedValues 입력 값
     */
    private void validateOptionalCode(String fieldName, String value, Set<String> allowedValues) {
        if (value != null && !allowedValues.contains(value)) {
            throw validationFailed(fieldName + " is invalid.");
        }
    }

    /**
     * 입력 값을 표준 형식으로 정규화합니다.
     *
     * @param value 입력 값
     *
     * @return 처리 결과
     */
    private String normalizeOptionalCode(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed.toUpperCase(Locale.ROOT);
    }

    /**
     * 문자열 입력 값을 정리합니다.
     *
     * @param value 입력 값
     *
     * @return 처리 결과
     */
    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param values 입력 값
     *
     * @return 처리 결과
     */
    private <T> List<T> nullToEmpty(List<T> values) {
        return values == null ? List.of() : values;
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param buttonCode 입력 값
     *
     * @param inputProvided 입력 값
     *
     * @return 처리 결과
     */
    private String safeActionInputJson(String buttonCode, boolean inputProvided) {
        return "{\"buttonCode\":\"" + safeValue(buttonCode) + "\",\"inputProvided\":" + inputProvided + "}";
    }

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param actorUserId 입력 값
     *
     * @param actionCode 입력 값
     *
     * @param resourceId 입력 값
     *
     * @param metadataJson 입력 값
     */
    private void insertAudit(UUID actorUserId, String actionCode, UUID resourceId, String metadataJson) {
        applicationProgressDao.insertAuditLog(new AuditLogCommand(
                actorUserId,
                actionCode,
                RESOURCE_TYPE,
                resourceId,
                "SUCCESS",
                metadataJson
        ));
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param key1 입력 값
     *
     * @param value1 입력 값
     *
     * @param key2 입력 값
     *
     * @param value2 입력 값
     *
     * @param key3 입력 값
     *
     * @param value3 입력 값
     *
     * @return 처리 결과
     */
    private String metadata(String key1, String value1, String key2, String value2, String key3, String value3) {
        return "{\"" + key1 + "\":\"" + safeValue(value1) + "\",\""
                + key2 + "\":\"" + safeValue(value2) + "\",\""
                + key3 + "\":\"" + safeValue(value3) + "\"}";
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param value 입력 값
     *
     * @return 처리 결과
     */
    private String safeValue(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param message 입력 값
     *
     * @return 처리 결과
     */
    private ApiException validationFailed(String message) {
        return new ApiException(ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST, message);
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @return 처리 결과
     */
    private ApiException notFound() {
        return new ApiException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "Application progress was not found.");
    }
}
