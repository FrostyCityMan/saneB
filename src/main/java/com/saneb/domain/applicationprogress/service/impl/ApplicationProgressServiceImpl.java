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

    private final ApplicationProgressDao applicationProgressDao;
    private final DynamicAnnouncementInputDao dynamicAnnouncementInputDao;

    public ApplicationProgressServiceImpl(
            ApplicationProgressDao applicationProgressDao,
            DynamicAnnouncementInputDao dynamicAnnouncementInputDao
    ) {
        this.applicationProgressDao = applicationProgressDao;
        this.dynamicAnnouncementInputDao = dynamicAnnouncementInputDao;
    }

    @Override
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
        if (!hasProgressStartOperatingRole(actor) && !matchingCase.memberUserId().equals(actor.userId())) {
            throw new ApiException(
                    ErrorCode.AUTH_FORBIDDEN,
                    HttpStatus.FORBIDDEN,
                    "본인의 공고만 진행할 수 있습니다."
            );
        }
        if (!"MATCHED".equals(matchingCase.statusCode())) {
            throw new ApiException(
                    ErrorCode.PROGRESS_CONDITION_NOT_MET,
                    HttpStatus.CONFLICT,
                    "Only MATCHED matching cases can start application progress."
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

    @Override
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
        if (applicationProgressDao.selectRequiredUncheckedDocumentCount(progressId, stepId) > 0) {
            throw new ApiException(
                    ErrorCode.PROGRESS_CONDITION_NOT_MET,
                    HttpStatus.CONFLICT,
                    "Required step documents are not checked."
            );
        }
        if (dynamicAnnouncementInputDao.selectMissingRequiredApplicationInputCount(progressId) > 0) {
            throw new ApiException(
                    ErrorCode.PROGRESS_CONDITION_NOT_MET,
                    HttpStatus.CONFLICT,
                    "Required application input values are missing."
            );
        }

        UUID nextStepId = button.nextStepId() == null
                ? applicationProgressDao.selectNextActiveStepId(progress.announcementId(), stepState.stepOrder())
                : button.nextStepId();
        if (nextStepId != null && applicationProgressDao.selectApplicationStepState(progressId, nextStepId) == null) {
            throw new ApiException(ErrorCode.PROGRESS_CONDITION_NOT_MET, HttpStatus.CONFLICT, "Next step is not initialized.");
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

        if (nextStepId == null) {
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

    @Override
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

    @Override
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

    @Override
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

    private ApplicationProgressRow selectApplicationProgressRow(UUID progressId) {
        ApplicationProgressRow row = applicationProgressDao.selectApplicationProgressDetails(progressId);
        if (row == null) {
            throw notFound();
        }
        return row;
    }

    private ApplicationStepStateRow selectApplicationStepState(UUID progressId, UUID stepId) {
        ApplicationStepStateRow row = applicationProgressDao.selectApplicationStepState(progressId, stepId);
        if (row == null) {
            throw new ApiException(ErrorCode.PROGRESS_STEP_LOCKED, HttpStatus.CONFLICT, "Progress step is not initialized.");
        }
        return row;
    }

    private ApplicationProgressSummaryResponse toSummaryResponse(ApplicationProgressRow row) {
        return new ApplicationProgressSummaryResponse(
                row.progressId(),
                row.matchingCaseId(),
                row.announcementId(),
                row.memberUserId(),
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

    private ApplicationProgressDetailsResponse.StepStateResponse toStepStateResponse(ApplicationStepStateRow row) {
        return new ApplicationProgressDetailsResponse.StepStateResponse(
                row.stepStateId(),
                row.stepId(),
                row.stepOrder(),
                row.stepName(),
                row.statusCode(),
                row.startedAt(),
                row.completedAt()
        );
    }

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

    private void validatePageRequest(int page, int size) {
        if (page < 1 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new ApiException(ErrorCode.INVALID_PAGE_REQUEST, HttpStatus.BAD_REQUEST, "Invalid page request.");
        }
    }

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

    private boolean hasOperatingRole(AuthenticatedUserDetails actor) {
        return actor.roles().stream().anyMatch(OPERATING_ROLES::contains);
    }

    private boolean hasProgressStartOperatingRole(AuthenticatedUserDetails actor) {
        return actor.roles().stream().anyMatch(role -> Set.of("OPERATOR", "ADMIN").contains(role));
    }

    private String normalizeRequiredCode(String fieldName, String value, Set<String> allowedValues) {
        String normalized = normalizeOptionalCode(value);
        if (normalized == null || !allowedValues.contains(normalized)) {
            throw validationFailed(fieldName + " is invalid.");
        }
        return normalized;
    }

    private void validateOptionalCode(String fieldName, String value, Set<String> allowedValues) {
        if (value != null && !allowedValues.contains(value)) {
            throw validationFailed(fieldName + " is invalid.");
        }
    }

    private String normalizeOptionalCode(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed.toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private <T> List<T> nullToEmpty(List<T> values) {
        return values == null ? List.of() : values;
    }

    private String safeActionInputJson(String buttonCode, boolean inputProvided) {
        return "{\"buttonCode\":\"" + safeValue(buttonCode) + "\",\"inputProvided\":" + inputProvided + "}";
    }

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

    private String metadata(String key1, String value1, String key2, String value2, String key3, String value3) {
        return "{\"" + key1 + "\":\"" + safeValue(value1) + "\",\""
                + key2 + "\":\"" + safeValue(value2) + "\",\""
                + key3 + "\":\"" + safeValue(value3) + "\"}";
    }

    private String safeValue(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private ApiException validationFailed(String message) {
        return new ApiException(ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST, message);
    }

    private ApiException notFound() {
        return new ApiException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "Application progress was not found.");
    }
}
