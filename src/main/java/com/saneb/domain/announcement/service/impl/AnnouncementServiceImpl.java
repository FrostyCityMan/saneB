package com.saneb.domain.announcement.service.impl;

import com.saneb.common.error.ApiException;
import com.saneb.common.error.ErrorCode;
import com.saneb.common.response.PageResponse;
import com.saneb.domain.announcement.dao.AnnouncementDao;
import com.saneb.domain.announcement.dto.AnnouncementApprovalDecisionRequest;
import com.saneb.domain.announcement.dto.AnnouncementApprovalRequestCreateRequest;
import com.saneb.domain.announcement.dto.AnnouncementConditionsSaveRequest;
import com.saneb.domain.announcement.dto.AnnouncementDetailsResponse;
import com.saneb.domain.announcement.dto.AnnouncementManualStatusUpdateRequest;
import com.saneb.domain.announcement.dto.AnnouncementSaveRequest;
import com.saneb.domain.announcement.dto.AnnouncementStepsSaveRequest;
import com.saneb.domain.announcement.dto.AnnouncementSummaryResponse;
import com.saneb.domain.announcement.service.AnnouncementService;
import com.saneb.domain.announcement.vo.AnnouncementApprovalDecisionCommand;
import com.saneb.domain.announcement.vo.AnnouncementApprovalRequestCommand;
import com.saneb.domain.announcement.vo.AnnouncementApprovalStatusCommand;
import com.saneb.domain.announcement.vo.AnnouncementDetailsRow;
import com.saneb.domain.announcement.vo.AnnouncementDocumentRequirementCommand;
import com.saneb.domain.announcement.vo.AnnouncementDocumentRequirementRow;
import com.saneb.domain.announcement.vo.AnnouncementIndustryConditionCommand;
import com.saneb.domain.announcement.vo.AnnouncementIndustryConditionRow;
import com.saneb.domain.announcement.vo.AnnouncementManualStatusCommand;
import com.saneb.domain.announcement.vo.AnnouncementNumericConditionCommand;
import com.saneb.domain.announcement.vo.AnnouncementNumericConditionRow;
import com.saneb.domain.announcement.vo.AnnouncementOptionCommand;
import com.saneb.domain.announcement.vo.AnnouncementOptionConditionCommand;
import com.saneb.domain.announcement.vo.AnnouncementOptionConditionRow;
import com.saneb.domain.announcement.vo.AnnouncementOptionRow;
import com.saneb.domain.announcement.vo.AnnouncementProgressStepCommand;
import com.saneb.domain.announcement.vo.AnnouncementProgressStepRow;
import com.saneb.domain.announcement.vo.AnnouncementSaveCommand;
import com.saneb.domain.announcement.vo.AnnouncementSearchCondition;
import com.saneb.domain.announcement.vo.AnnouncementStatusHistoryCommand;
import com.saneb.domain.announcement.vo.AnnouncementStepButtonCommand;
import com.saneb.domain.announcement.vo.AnnouncementStepButtonRow;
import com.saneb.domain.announcement.vo.AnnouncementStepDocumentCommand;
import com.saneb.domain.announcement.vo.AnnouncementStepDocumentRow;
import com.saneb.domain.announcement.vo.AnnouncementStandardDocumentFieldRow;
import com.saneb.domain.announcement.vo.AnnouncementSummaryRow;
import com.saneb.domain.auth.vo.AuthenticatedUserDetails;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnnouncementServiceImpl implements AnnouncementService {

    private static final int MAX_PAGE_SIZE = 100;

    private static final Set<String> TARGET_TYPE_CODES = Set.of(
            "BUSINESS", "PERSONAL", "SPOUSE", "CHILD", "PARENT"
    );
    private static final Set<String> MANUAL_STATUS_CODES = Set.of(
            "NORMAL", "PAUSED", "EARLY_CLOSED", "SUSPENDED", "BUDGET_EXHAUSTED", "CLOSED", "HIDDEN"
    );
    private static final Set<String> APPROVAL_STATUS_CODES = Set.of(
            "DRAFT", "REQUESTED", "APPROVED", "REJECTED", "CANCELED"
    );
    private static final Set<String> APPROVAL_DECISION_STATUS_CODES = Set.of(
            "APPROVED", "REJECTED", "CANCELED"
    );
    private static final Set<String> INCOME_JUDGEMENT_CODES = Set.of(
            "INCOME_CERT_ONLY",
            "HEALTH_INSURANCE_ONLY",
            "VAT_TAX_BASE_ONLY",
            "ANY_ONE_DOCUMENT",
            "INCOME_OR_HEALTH_INSURANCE",
            "NO_LIMIT"
    );
    private static final Set<String> INDUSTRY_CONDITION_TYPE_CODES = Set.of("INCLUDE", "EXCLUDE");
    private static final Set<String> CONDITION_SCOPE_CODES = Set.of(
            "BUSINESS", "PERSONAL", "SPOUSE", "CHILD", "PARENT", "APPLICATION", "SUPPORT"
    );
    private static final Set<String> COMPARATOR_CODES = Set.of("GTE", "LTE", "GT", "LT", "EQ", "BETWEEN");
    private static final Set<String> NUMERIC_CONDITION_FIELD_TYPE_CODES = Set.of("NUMBER", "AMOUNT", "DATE");
    private static final Set<String> OPTION_CONDITION_FIELD_TYPE_CODES = Set.of(
            "BOOLEAN", "SELECT", "RADIO", "MULTI_SELECT"
    );

    private final AnnouncementDao announcementDao;

    public AnnouncementServiceImpl(AnnouncementDao announcementDao) {
        this.announcementDao = announcementDao;
    }

    @Override
    public PageResponse<AnnouncementSummaryResponse> selectAnnouncementList(
            String keyword,
            String targetTypeCode,
            String manualStatusCode,
            String approvalStatusCode,
            int page,
            int size
    ) {
        validatePageRequest(page, size);
        String normalizedTargetTypeCode = normalizeOptionalCode(targetTypeCode);
        String normalizedManualStatusCode = normalizeOptionalCode(manualStatusCode);
        String normalizedApprovalStatusCode = normalizeOptionalCode(approvalStatusCode);
        validateOptionalCode("targetTypeCode", normalizedTargetTypeCode, TARGET_TYPE_CODES);
        validateOptionalCode("manualStatusCode", normalizedManualStatusCode, MANUAL_STATUS_CODES);
        validateOptionalCode("approvalStatusCode", normalizedApprovalStatusCode, APPROVAL_STATUS_CODES);

        AnnouncementSearchCondition condition = new AnnouncementSearchCondition(
                trimToNull(keyword),
                normalizedTargetTypeCode,
                normalizedManualStatusCode,
                normalizedApprovalStatusCode,
                page,
                size,
                (page - 1) * size
        );
        long totalCount = announcementDao.selectAnnouncementCount(condition);
        List<AnnouncementSummaryResponse> items = announcementDao.selectAnnouncementList(condition).stream()
                .map(this::toSummaryResponse)
                .toList();
        return PageResponse.of(items, page, size, totalCount);
    }

    @Override
    @Transactional
    public AnnouncementDetailsResponse insertAnnouncement(Authentication authentication, AnnouncementSaveRequest request) {
        UUID actorUserId = selectRequiredActorUserId(authentication);
        validateSaveRequest(request);

        UUID announcementId = UUID.randomUUID();
        announcementDao.insertAnnouncement(toSaveCommand(announcementId, request, actorUserId));
        replaceAnnouncementOptions(announcementId, request.options(), actorUserId);
        return selectAnnouncementDetails(announcementId);
    }

    @Override
    public AnnouncementDetailsResponse selectAnnouncementDetails(UUID announcementId) {
        AnnouncementDetailsRow row = selectAnnouncementDetailsRow(announcementId);
        return toDetailsResponse(
                row,
                announcementDao.selectAnnouncementOptionList(announcementId),
                announcementDao.selectAnnouncementIndustryConditionList(announcementId),
                announcementDao.selectAnnouncementNumericConditionList(announcementId),
                announcementDao.selectAnnouncementOptionConditionList(announcementId),
                announcementDao.selectAnnouncementDocumentRequirementList(announcementId),
                announcementDao.selectAnnouncementProgressStepList(announcementId),
                announcementDao.selectAnnouncementStepDocumentList(announcementId),
                announcementDao.selectAnnouncementStepButtonList(announcementId)
        );
    }

    @Override
    @Transactional
    public AnnouncementDetailsResponse updateAnnouncement(
            Authentication authentication,
            UUID announcementId,
            AnnouncementSaveRequest request
    ) {
        UUID actorUserId = selectRequiredActorUserId(authentication);
        selectAnnouncementDetailsRow(announcementId);
        validateSaveRequest(request);

        int updatedCount = announcementDao.updateAnnouncement(toSaveCommand(announcementId, request, actorUserId));
        if (updatedCount == 0) {
            throw notFound();
        }
        replaceAnnouncementOptions(announcementId, request.options(), actorUserId);
        return selectAnnouncementDetails(announcementId);
    }

    @Override
    @Transactional
    public void updateAnnouncementConditions(
            Authentication authentication,
            UUID announcementId,
            AnnouncementConditionsSaveRequest request
    ) {
        UUID actorUserId = selectRequiredActorUserId(authentication);
        selectAnnouncementDetailsRow(announcementId);
        validateConditionsRequest(request);

        announcementDao.deleteAnnouncementIndustryConditions(announcementId);
        announcementDao.deleteAnnouncementNumericConditions(announcementId);
        announcementDao.deleteAnnouncementOptionConditions(announcementId);
        announcementDao.deleteAnnouncementDocumentRequirements(announcementId);

        for (AnnouncementConditionsSaveRequest.IndustryConditionRequest condition
                : nullToEmpty(request.industryConditions())) {
            announcementDao.insertAnnouncementIndustryCondition(new AnnouncementIndustryConditionCommand(
                    announcementId,
                    normalizeRequiredCode("conditionTypeCode", condition.conditionTypeCode(), INDUSTRY_CONDITION_TYPE_CODES),
                    normalizeText(condition.ksicCode()),
                    actorUserId
            ));
        }
        for (AnnouncementConditionsSaveRequest.NumericConditionRequest condition
                : nullToEmpty(request.numericConditions())) {
            String comparatorCode = normalizeRequiredCode("comparatorCode", condition.comparatorCode(), COMPARATOR_CODES);
            announcementDao.insertAnnouncementNumericCondition(new AnnouncementNumericConditionCommand(
                    announcementId,
                    normalizeRequiredCode("conditionScopeCode", condition.conditionScopeCode(), CONDITION_SCOPE_CODES),
                    normalizeText(condition.conditionKey()),
                    comparatorCode,
                    condition.valueNumber(),
                    condition.minNumber(),
                    condition.maxNumber(),
                    normalizeOptionalCode(condition.unitCode()),
                    condition.standardFieldId(),
                    actorUserId
            ));
        }
        for (AnnouncementConditionsSaveRequest.OptionConditionRequest condition
                : nullToEmpty(request.optionConditions())) {
            announcementDao.insertAnnouncementOptionCondition(new AnnouncementOptionConditionCommand(
                    announcementId,
                    normalizeRequiredCode("conditionScopeCode", condition.conditionScopeCode(), CONDITION_SCOPE_CODES),
                    normalizeText(condition.conditionKey()),
                    normalizeText(condition.optionCode()),
                    trimToNull(condition.optionText()),
                    condition.standardFieldId(),
                    actorUserId
            ));
        }
        for (AnnouncementConditionsSaveRequest.DocumentRequirementRequest requirement
                : nullToEmpty(request.documentRequirements())) {
            announcementDao.insertAnnouncementDocumentRequirement(new AnnouncementDocumentRequirementCommand(
                    announcementId,
                    normalizeText(requirement.documentTypeCode()),
                    requirement.required(),
                    requirement.sortOrder(),
                    requirement.standardFieldId(),
                    actorUserId
            ));
        }
    }

    @Override
    @Transactional
    public void updateAnnouncementSteps(
            Authentication authentication,
            UUID announcementId,
            AnnouncementStepsSaveRequest request
    ) {
        UUID actorUserId = selectRequiredActorUserId(authentication);
        selectAnnouncementDetailsRow(announcementId);
        validateStepsRequest(request);

        announcementDao.deleteAnnouncementStepButtons(announcementId);
        announcementDao.deleteAnnouncementStepDocuments(announcementId);
        announcementDao.deleteAnnouncementProgressSteps(announcementId);

        for (AnnouncementStepsSaveRequest.StepRequest step : nullToEmpty(request.steps())) {
            UUID stepId = UUID.randomUUID();
            announcementDao.insertAnnouncementProgressStep(new AnnouncementProgressStepCommand(
                    stepId,
                    announcementId,
                    step.stepOrder(),
                    normalizeText(step.stepName()),
                    trimToNull(step.guideMessage()),
                    trimToNull(step.actionGuide()),
                    normalizeText(step.completionConditionCode()),
                    normalizeOptionalCode(step.nextConditionCode()),
                    step.active() == null ? Boolean.TRUE : step.active(),
                    actorUserId
            ));

            for (AnnouncementStepsSaveRequest.StepDocumentRequest document : nullToEmpty(step.documents())) {
                announcementDao.insertAnnouncementStepDocument(new AnnouncementStepDocumentCommand(
                        stepId,
                        normalizeText(document.documentTypeCode()),
                        document.required(),
                        document.sortOrder(),
                        actorUserId
                ));
            }

            for (AnnouncementStepsSaveRequest.ButtonRequest button : nullToEmpty(step.buttons())) {
                announcementDao.insertAnnouncementStepButton(new AnnouncementStepButtonCommand(
                        stepId,
                        normalizeText(button.buttonCode()),
                        normalizeText(button.buttonLabel()),
                        normalizeText(button.buttonActionCode()),
                        button.nextStepId(),
                        button.sortOrder(),
                        actorUserId
                ));
            }
        }
    }

    @Override
    @Transactional
    public void updateAnnouncementManualStatus(
            Authentication authentication,
            UUID announcementId,
            AnnouncementManualStatusUpdateRequest request
    ) {
        UUID actorUserId = selectRequiredActorUserId(authentication);
        AnnouncementDetailsRow row = selectAnnouncementDetailsRow(announcementId);
        String nextStatusCode = normalizeRequiredCode(
                "manualStatusCode",
                request.manualStatusCode(),
                MANUAL_STATUS_CODES
        );
        if (row.manualStatusCode().equals(nextStatusCode)) {
            return;
        }

        int updatedCount = announcementDao.updateAnnouncementManualStatus(new AnnouncementManualStatusCommand(
                announcementId,
                nextStatusCode,
                actorUserId
        ));
        if (updatedCount == 0) {
            throw notFound();
        }
        announcementDao.insertAnnouncementStatusHistory(new AnnouncementStatusHistoryCommand(
                announcementId,
                row.manualStatusCode(),
                nextStatusCode,
                trimToNull(request.reason()),
                actorUserId
        ));
    }

    @Override
    @Transactional
    public AnnouncementDetailsResponse insertAnnouncementApprovalRequest(
            Authentication authentication,
            UUID announcementId,
            AnnouncementApprovalRequestCreateRequest request
    ) {
        UUID actorUserId = selectRequiredActorUserId(authentication);
        AnnouncementDetailsRow row = selectAnnouncementDetailsRow(announcementId);
        if ("APPROVED".equals(row.approvalStatusCode()) || "REQUESTED".equals(row.approvalStatusCode())) {
            throw invalidStatusTransition("이미 승인되었거나 승인 요청 중인 공고입니다.");
        }

        announcementDao.insertAnnouncementApprovalRequest(new AnnouncementApprovalRequestCommand(
                announcementId,
                actorUserId,
                trimToNull(request.requestNote())
        ));
        updateApprovalStatus(announcementId, "REQUESTED", actorUserId);
        return selectAnnouncementDetails(announcementId);
    }

    @Override
    @Transactional
    public AnnouncementDetailsResponse updateAnnouncementApproval(
            Authentication authentication,
            UUID announcementId,
            AnnouncementApprovalDecisionRequest request
    ) {
        UUID actorUserId = selectRequiredActorUserId(authentication);
        AnnouncementDetailsRow row = selectAnnouncementDetailsRow(announcementId);
        String approvalStatusCode = normalizeRequiredCode(
                "approvalStatusCode",
                request.approvalStatusCode(),
                APPROVAL_DECISION_STATUS_CODES
        );
        if (!"REQUESTED".equals(row.approvalStatusCode())) {
            throw invalidStatusTransition("승인 요청 상태인 공고만 승인 처리할 수 있습니다.");
        }
        if (announcementDao.selectRequestedApprovalRequestCount(announcementId) <= 0) {
            throw invalidStatusTransition("처리할 승인 요청이 없습니다.");
        }

        int decidedCount = announcementDao.updateAnnouncementApprovalDecision(new AnnouncementApprovalDecisionCommand(
                announcementId,
                approvalStatusCode,
                trimToNull(request.decisionNote()),
                actorUserId
        ));
        if (decidedCount == 0) {
            throw invalidStatusTransition("처리할 승인 요청이 없습니다.");
        }
        updateApprovalStatus(announcementId, approvalStatusCode, actorUserId);
        return selectAnnouncementDetails(announcementId);
    }

    private void replaceAnnouncementOptions(
            UUID announcementId,
            List<AnnouncementSaveRequest.OptionRequest> options,
            UUID actorUserId
    ) {
        announcementDao.deleteAnnouncementOptions(announcementId);
        for (AnnouncementSaveRequest.OptionRequest option : nullToEmpty(options)) {
            announcementDao.insertAnnouncementOption(new AnnouncementOptionCommand(
                    announcementId,
                    normalizeText(option.optionGroupCode()),
                    normalizeText(option.optionCode()),
                    actorUserId
            ));
        }
    }

    private AnnouncementSaveCommand toSaveCommand(
            UUID announcementId,
            AnnouncementSaveRequest request,
            UUID actorUserId
    ) {
        return new AnnouncementSaveCommand(
                announcementId,
                normalizeRequiredCode("targetTypeCode", request.targetTypeCode(), TARGET_TYPE_CODES),
                normalizeText(request.title()),
                normalizeText(request.agencyName()),
                trimToNull(request.summary()),
                request.applicationStartDate(),
                request.applicationEndDate(),
                normalizeRequiredCode("incomeJudgementCode", request.incomeJudgementCode(), INCOME_JUDGEMENT_CODES),
                request.minAmount(),
                request.maxAmount(),
                actorUserId
        );
    }

    private AnnouncementDetailsResponse toDetailsResponse(
            AnnouncementDetailsRow row,
            List<AnnouncementOptionRow> options,
            List<AnnouncementIndustryConditionRow> industryConditions,
            List<AnnouncementNumericConditionRow> numericConditions,
            List<AnnouncementOptionConditionRow> optionConditions,
            List<AnnouncementDocumentRequirementRow> documentRequirements,
            List<AnnouncementProgressStepRow> steps,
            List<AnnouncementStepDocumentRow> stepDocuments,
            List<AnnouncementStepButtonRow> stepButtons
    ) {
        Map<UUID, List<AnnouncementStepDocumentRow>> documentsByStepId = stepDocuments.stream()
                .collect(Collectors.groupingBy(AnnouncementStepDocumentRow::stepId));
        Map<UUID, List<AnnouncementStepButtonRow>> buttonsByStepId = stepButtons.stream()
                .collect(Collectors.groupingBy(AnnouncementStepButtonRow::stepId));

        return new AnnouncementDetailsResponse(
                row.announcementId(),
                row.announcementCode(),
                row.targetTypeCode(),
                row.title(),
                row.agencyName(),
                row.summary(),
                row.applicationStartDate(),
                row.applicationEndDate(),
                row.manualStatusCode(),
                row.approvalStatusCode(),
                row.incomeJudgementCode(),
                row.minAmount(),
                row.maxAmount(),
                row.createdAt(),
                row.updatedAt(),
                options.stream()
                        .map(option -> new AnnouncementDetailsResponse.OptionResponse(
                                option.optionGroupCode(),
                                option.optionCode()
                        ))
                        .toList(),
                new AnnouncementDetailsResponse.ConditionsResponse(
                        industryConditions.stream()
                                .map(condition -> new AnnouncementDetailsResponse.IndustryConditionResponse(
                                        condition.conditionTypeCode(),
                                        condition.ksicCode()
                                ))
                                .toList(),
                        numericConditions.stream()
                                .map(condition -> new AnnouncementDetailsResponse.NumericConditionResponse(
                                        condition.conditionScopeCode(),
                                        condition.conditionKey(),
                                        condition.comparatorCode(),
                                        condition.valueNumber(),
                                        condition.minNumber(),
                                        condition.maxNumber(),
                                        condition.unitCode(),
                                        condition.standardFieldId()
                                ))
                                .toList(),
                        optionConditions.stream()
                                .map(condition -> new AnnouncementDetailsResponse.OptionConditionResponse(
                                        condition.conditionScopeCode(),
                                        condition.conditionKey(),
                                        condition.optionCode(),
                                        condition.optionText(),
                                        condition.standardFieldId()
                                ))
                                .toList(),
                        documentRequirements.stream()
                                .map(requirement -> new AnnouncementDetailsResponse.DocumentRequirementResponse(
                                        requirement.documentTypeCode(),
                                        requirement.required(),
                                        requirement.sortOrder(),
                                        requirement.standardFieldId()
                                ))
                                .toList()
                ),
                steps.stream()
                        .map(step -> toProgressStepResponse(
                                step,
                                documentsByStepId.getOrDefault(step.stepId(), List.of()),
                                buttonsByStepId.getOrDefault(step.stepId(), List.of())
                        ))
                        .toList()
        );
    }

    private AnnouncementDetailsResponse.ProgressStepResponse toProgressStepResponse(
            AnnouncementProgressStepRow step,
            List<AnnouncementStepDocumentRow> documents,
            List<AnnouncementStepButtonRow> buttons
    ) {
        return new AnnouncementDetailsResponse.ProgressStepResponse(
                step.stepId(),
                step.stepOrder(),
                step.stepName(),
                step.guideMessage(),
                step.actionGuide(),
                step.completionConditionCode(),
                step.nextConditionCode(),
                step.active(),
                buttons.stream()
                        .map(button -> new AnnouncementDetailsResponse.StepButtonResponse(
                                button.buttonCode(),
                                button.buttonLabel(),
                                button.buttonActionCode(),
                                button.nextStepId(),
                                button.sortOrder()
                        ))
                        .toList(),
                documents.stream()
                        .map(document -> new AnnouncementDetailsResponse.StepDocumentResponse(
                                document.documentTypeCode(),
                                document.required(),
                                document.sortOrder()
                        ))
                        .toList()
        );
    }

    private AnnouncementSummaryResponse toSummaryResponse(AnnouncementSummaryRow row) {
        return new AnnouncementSummaryResponse(
                row.announcementId(),
                row.announcementCode(),
                row.targetTypeCode(),
                row.title(),
                row.agencyName(),
                row.applicationStartDate(),
                row.applicationEndDate(),
                row.manualStatusCode(),
                row.approvalStatusCode(),
                row.minAmount(),
                row.maxAmount(),
                row.createdAt(),
                row.updatedAt()
        );
    }

    private AnnouncementDetailsRow selectAnnouncementDetailsRow(UUID announcementId) {
        AnnouncementDetailsRow row = announcementDao.selectAnnouncementDetails(announcementId);
        if (row == null) {
            throw notFound();
        }
        return row;
    }

    private void validateSaveRequest(AnnouncementSaveRequest request) {
        normalizeRequiredCode("targetTypeCode", request.targetTypeCode(), TARGET_TYPE_CODES);
        normalizeRequiredCode("incomeJudgementCode", request.incomeJudgementCode(), INCOME_JUDGEMENT_CODES);
        validateAmountRange(request.minAmount(), request.maxAmount());
        if (request.applicationStartDate() != null
                && request.applicationEndDate() != null
                && request.applicationStartDate().isAfter(request.applicationEndDate())) {
            throw validation("applicationStartDate must not be after applicationEndDate.");
        }
    }

    private void validateConditionsRequest(AnnouncementConditionsSaveRequest request) {
        for (AnnouncementConditionsSaveRequest.IndustryConditionRequest condition
                : nullToEmpty(request.industryConditions())) {
            normalizeRequiredCode("conditionTypeCode", condition.conditionTypeCode(), INDUSTRY_CONDITION_TYPE_CODES);
        }
        for (AnnouncementConditionsSaveRequest.NumericConditionRequest condition
                : nullToEmpty(request.numericConditions())) {
            String comparatorCode = normalizeRequiredCode("comparatorCode", condition.comparatorCode(), COMPARATOR_CODES);
            String scopeCode = normalizeRequiredCode("conditionScopeCode", condition.conditionScopeCode(), CONDITION_SCOPE_CODES);
            validateConditionStandardField(condition.standardFieldId(), scopeCode, NUMERIC_CONDITION_FIELD_TYPE_CODES);
            validateNumericCondition(comparatorCode, condition.valueNumber(), condition.minNumber(), condition.maxNumber());
        }
        for (AnnouncementConditionsSaveRequest.OptionConditionRequest condition
                : nullToEmpty(request.optionConditions())) {
            String scopeCode = normalizeRequiredCode("conditionScopeCode", condition.conditionScopeCode(), CONDITION_SCOPE_CODES);
            validateConditionStandardField(condition.standardFieldId(), scopeCode, OPTION_CONDITION_FIELD_TYPE_CODES);
        }
        for (AnnouncementConditionsSaveRequest.DocumentRequirementRequest requirement
                : nullToEmpty(request.documentRequirements())) {
            validateDocumentRequirementStandardField(requirement.standardFieldId(), requirement.documentTypeCode());
        }
    }

    private void validateStepsRequest(AnnouncementStepsSaveRequest request) {
        List<AnnouncementStepsSaveRequest.StepRequest> steps = nullToEmpty(request.steps());
        validateUnique("stepOrder", steps, AnnouncementStepsSaveRequest.StepRequest::stepOrder);
        for (AnnouncementStepsSaveRequest.StepRequest step : steps) {
            validateUnique("buttonCode", nullToEmpty(step.buttons()), AnnouncementStepsSaveRequest.ButtonRequest::buttonCode);
            validateUnique(
                    "documentTypeCode",
                    nullToEmpty(step.documents()),
                    AnnouncementStepsSaveRequest.StepDocumentRequest::documentTypeCode
            );
        }
    }

    private void validatePageRequest(int page, int size) {
        if (page < 1 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new ApiException(
                    ErrorCode.INVALID_PAGE_REQUEST,
                    HttpStatus.BAD_REQUEST,
                    "page must be 1 or greater and size must be between 1 and 100."
            );
        }
    }

    private void validateAmountRange(BigDecimal minAmount, BigDecimal maxAmount) {
        if (minAmount != null && maxAmount != null && minAmount.compareTo(maxAmount) > 0) {
            throw validation("minAmount must be less than or equal to maxAmount.");
        }
    }

    private void validateNumericCondition(
            String comparatorCode,
            BigDecimal valueNumber,
            BigDecimal minNumber,
            BigDecimal maxNumber
    ) {
        if ("BETWEEN".equals(comparatorCode)) {
            if (minNumber == null || maxNumber == null || minNumber.compareTo(maxNumber) > 0) {
                throw validation("BETWEEN numeric condition requires minNumber <= maxNumber.");
            }
            return;
        }
        if (valueNumber == null) {
            throw validation("numeric condition requires valueNumber unless comparatorCode is BETWEEN.");
        }
    }

    private void validateConditionStandardField(
            UUID standardFieldId,
            String scopeCode,
            Set<String> allowedFieldTypeCodes
    ) {
        if (standardFieldId == null) {
            return;
        }
        AnnouncementStandardDocumentFieldRow field = selectStandardDocumentField(standardFieldId);
        if (!Boolean.TRUE.equals(field.conditionEligible())) {
            throw validation("이 표준 서류 항목은 조건으로 사용할 수 없습니다.");
        }
        if (!allowedFieldTypeCodes.contains(field.fieldTypeCode())) {
            throw validation("선택한 표준 서류 항목은 이 조건 유형에 사용할 수 없습니다.");
        }
        if (!field.scopeCode().equals(scopeCode)) {
            throw validation("표준 서류 항목의 적용 범위와 조건 범위가 일치해야 합니다.");
        }
    }

    private void validateDocumentRequirementStandardField(UUID standardFieldId, String documentTypeCode) {
        if (standardFieldId == null) {
            return;
        }
        AnnouncementStandardDocumentFieldRow field = selectStandardDocumentField(standardFieldId);
        if (!field.documentTypeCode().equals(normalizeOptionalCode(documentTypeCode))) {
            throw validation("필요 서류와 표준 서류 항목의 서류 종류가 일치해야 합니다.");
        }
    }

    private AnnouncementStandardDocumentFieldRow selectStandardDocumentField(UUID standardFieldId) {
        AnnouncementStandardDocumentFieldRow field = announcementDao.selectStandardDocumentFieldDetails(standardFieldId);
        if (field == null || !Boolean.TRUE.equals(field.selectable())) {
            throw validation("선택할 수 없는 표준 서류 항목입니다.");
        }
        return field;
    }

    private <T, K> void validateUnique(String fieldName, List<T> values, Function<T, K> keySelector) {
        long distinctCount = values.stream()
                .map(keySelector)
                .filter(key -> key != null)
                .map(key -> key instanceof String text ? normalizeText(text) : key)
                .distinct()
                .count();
        long nonNullCount = values.stream()
                .map(keySelector)
                .filter(key -> key != null)
                .count();
        if (distinctCount != nonNullCount) {
            throw validation(fieldName + " must be unique.");
        }
    }

    private UUID selectRequiredActorUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ApiException(ErrorCode.AUTH_REQUIRED, HttpStatus.UNAUTHORIZED, "Authentication is required.");
        }
        if (authentication.getPrincipal() instanceof AuthenticatedUserDetails principal) {
            return principal.userId();
        }
        throw new ApiException(
                ErrorCode.AUTH_REQUIRED,
                HttpStatus.UNAUTHORIZED,
                "Authenticated user id is required."
        );
    }

    private void validateOptionalCode(String fieldName, String value, Set<String> allowedValues) {
        if (value != null && !allowedValues.contains(value)) {
            throw validation(fieldName + " is not supported.");
        }
    }

    private String normalizeRequiredCode(String fieldName, String value, Set<String> allowedValues) {
        String normalizedValue = normalizeOptionalCode(value);
        if (normalizedValue == null || !allowedValues.contains(normalizedValue)) {
            throw validation(fieldName + " is not supported.");
        }
        return normalizedValue;
    }

    private String normalizeOptionalCode(String value) {
        String normalizedValue = trimToNull(value);
        return normalizedValue == null ? null : normalizedValue.toUpperCase(Locale.ROOT);
    }

    private String normalizeText(String value) {
        String normalizedValue = trimToNull(value);
        if (normalizedValue == null) {
            throw validation("required text value is blank.");
        }
        return normalizedValue;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmedValue = value.trim();
        return trimmedValue.isEmpty() ? null : trimmedValue;
    }

    private <T> List<T> nullToEmpty(List<T> values) {
        return values == null ? List.of() : values;
    }

    private ApiException validation(String message) {
        return new ApiException(ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST, message);
    }

    private void updateApprovalStatus(UUID announcementId, String approvalStatusCode, UUID actorUserId) {
        int updatedCount = announcementDao.updateAnnouncementApprovalStatus(new AnnouncementApprovalStatusCommand(
                announcementId,
                approvalStatusCode,
                actorUserId
        ));
        if (updatedCount == 0) {
            throw notFound();
        }
    }

    private ApiException invalidStatusTransition(String message) {
        return new ApiException(ErrorCode.INVALID_STATUS_TRANSITION, HttpStatus.CONFLICT, message);
    }

    private ApiException notFound() {
        return new ApiException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "Announcement was not found.");
    }
}
