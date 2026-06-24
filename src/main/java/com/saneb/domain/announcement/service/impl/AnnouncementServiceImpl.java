/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: AnnouncementServiceImpl.java
 * 작성자: 김도훈
 *
 */

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
import java.time.LocalDate;
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
    private static final Set<String> STEP_COMPLETION_CONDITION_CODES = Set.of(
            "BUTTON_CLICK",
            "ALL_REQUIRED_DOCUMENTS_CHECKED",
            "REQUIRED_INPUTS_SAVED",
            "RECEIPT_SAVED",
            "RESULT_SAVED",
            "DOCUMENT_SUBMITTED",
            "STATUS_CONFIRMED"
    );
    private static final Set<String> STEP_BUTTON_ACTION_CODES = Set.of(
            "MOVE_NEXT", "COMPLETE_STEP", "STOP_PROGRESS"
    );

    private final AnnouncementDao announcementDao;

    /**
     * 객체를 생성합니다.
     *
     * @param announcementDao 입력 값
     */
    public AnnouncementServiceImpl(AnnouncementDao announcementDao) {
        this.announcementDao = announcementDao;
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param keyword 입력 값
     *
     * @param targetTypeCode 입력 값
     *
     * @param manualStatusCode 입력 값
     *
     * @param approvalStatusCode 입력 값
     *
     * @param page 입력 값
     *
     * @param size 입력 값
     *
     * @return 처리 결과
     */
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
    public AnnouncementDetailsResponse insertAnnouncement(Authentication authentication, AnnouncementSaveRequest request) {
        UUID actorUserId = selectRequiredActorUserId(authentication);
        validateSaveRequest(request);

        UUID announcementId = UUID.randomUUID();
        announcementDao.insertAnnouncement(toSaveCommand(announcementId, request, actorUserId));
        replaceAnnouncementOptions(announcementId, request.options(), actorUserId);
        return selectAnnouncementDetails(announcementId);
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param announcementId 입력 값
     *
     * @return 처리 결과
     */
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

    /**
     * 업무 데이터를 수정합니다.
     *
     * @param authentication 입력 값
     *
     * @param announcementId 입력 값
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
     * @param announcementId 입력 값
     *
     * @param request 입력 값
     *
     * @return 처리 결과
     */
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

    /**
     * 업무 데이터를 수정합니다.
     *
     * @param authentication 입력 값
     *
     * @param announcementId 입력 값
     *
     * @param request 입력 값
     */
    @Override
    /**
     * 업무 데이터를 수정합니다.
     *
     * @param authentication 입력 값
     *
     * @param announcementId 입력 값
     *
     * @param request 입력 값
     */
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

    /**
     * 업무 데이터를 수정합니다.
     *
     * @param authentication 입력 값
     *
     * @param announcementId 입력 값
     *
     * @param request 입력 값
     */
    @Override
    /**
     * 업무 데이터를 수정합니다.
     *
     * @param authentication 입력 값
     *
     * @param announcementId 입력 값
     *
     * @param request 입력 값
     */
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

    /**
     * 업무 데이터를 수정합니다.
     *
     * @param authentication 입력 값
     *
     * @param announcementId 입력 값
     *
     * @param request 입력 값
     */
    @Override
    /**
     * 업무 데이터를 수정합니다.
     *
     * @param authentication 입력 값
     *
     * @param announcementId 입력 값
     *
     * @param request 입력 값
     */
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

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param authentication 입력 값
     *
     * @param announcementId 입력 값
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
     * @param announcementId 입력 값
     *
     * @param request 입력 값
     *
     * @return 처리 결과
     */
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

    /**
     * 업무 데이터를 수정합니다.
     *
     * @param authentication 입력 값
     *
     * @param announcementId 입력 값
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
     * @param announcementId 입력 값
     *
     * @param request 입력 값
     *
     * @return 처리 결과
     */
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

    /**
     * 업무 처리를 수행합니다.
     *
     * @param announcementId 입력 값
     *
     * @param options 입력 값
     *
     * @param actorUserId 입력 값
     */
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

    /**
     * 업무 데이터를 응답 형식으로 변환합니다.
     *
     * @param announcementId 입력 값
     *
     * @param request 입력 값
     *
     * @param actorUserId 입력 값
     *
     * @return 처리 결과
     */
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

    /**
     * 업무 데이터를 응답 형식으로 변환합니다.
     *
     * @param row 입력 값
     *
     * @param options 입력 값
     *
     * @param industryConditions 입력 값
     *
     * @param numericConditions 입력 값
     *
     * @param optionConditions 입력 값
     *
     * @param documentRequirements 입력 값
     *
     * @param steps 입력 값
     *
     * @param stepDocuments 입력 값
     *
     * @param stepButtons 입력 값
     *
     * @return 처리 결과
     */
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
        AnnouncementStatusPolicy.AnnouncementStatusView status = AnnouncementStatusPolicy.selectStatus(
                row.applicationStartDate(),
                row.applicationEndDate(),
                row.manualStatusCode(),
                LocalDate.now()
        );

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
                status.automaticStatusCode(),
                status.automaticStatusLabel(),
                status.effectiveStatusCode(),
                status.effectiveStatusLabel(),
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

    /**
     * 업무 데이터를 응답 형식으로 변환합니다.
     *
     * @param step 입력 값
     *
     * @param documents 입력 값
     *
     * @param buttons 입력 값
     *
     * @return 처리 결과
     */
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

    /**
     * 업무 데이터를 응답 형식으로 변환합니다.
     *
     * @param row 입력 값
     *
     * @return 처리 결과
     */
    private AnnouncementSummaryResponse toSummaryResponse(AnnouncementSummaryRow row) {
        AnnouncementStatusPolicy.AnnouncementStatusView status = AnnouncementStatusPolicy.selectStatus(
                row.applicationStartDate(),
                row.applicationEndDate(),
                row.manualStatusCode(),
                LocalDate.now()
        );
        return new AnnouncementSummaryResponse(
                row.announcementId(),
                row.announcementCode(),
                row.targetTypeCode(),
                row.title(),
                row.agencyName(),
                row.applicationStartDate(),
                row.applicationEndDate(),
                row.manualStatusCode(),
                status.automaticStatusCode(),
                status.automaticStatusLabel(),
                status.effectiveStatusCode(),
                status.effectiveStatusLabel(),
                row.receptionTypeCode(),
                row.approvalStatusCode(),
                row.minAmount(),
                row.maxAmount(),
                row.createdAt(),
                row.updatedAt()
        );
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param announcementId 입력 값
     *
     * @return 처리 결과
     */
    private AnnouncementDetailsRow selectAnnouncementDetailsRow(UUID announcementId) {
        AnnouncementDetailsRow row = announcementDao.selectAnnouncementDetails(announcementId);
        if (row == null) {
            throw notFound();
        }
        return row;
    }

    /**
     * 요청 값과 업무 규칙을 검증합니다.
     *
     * @param request 입력 값
     */
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

    /**
     * 요청 값과 업무 규칙을 검증합니다.
     *
     * @param request 입력 값
     */
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

    /**
     * 요청 값과 업무 규칙을 검증합니다.
     *
     * @param request 입력 값
     */
    private void validateStepsRequest(AnnouncementStepsSaveRequest request) {
        List<AnnouncementStepsSaveRequest.StepRequest> steps = nullToEmpty(request.steps());
        validateUnique("stepOrder", steps, AnnouncementStepsSaveRequest.StepRequest::stepOrder);
        for (AnnouncementStepsSaveRequest.StepRequest step : steps) {
            normalizeRequiredCode("completionConditionCode", step.completionConditionCode(), STEP_COMPLETION_CONDITION_CODES);
            validateUnique("buttonCode", nullToEmpty(step.buttons()), AnnouncementStepsSaveRequest.ButtonRequest::buttonCode);
            for (AnnouncementStepsSaveRequest.ButtonRequest button : nullToEmpty(step.buttons())) {
                normalizeRequiredCode("buttonActionCode", button.buttonActionCode(), STEP_BUTTON_ACTION_CODES);
            }
            validateUnique(
                    "documentTypeCode",
                    nullToEmpty(step.documents()),
                    AnnouncementStepsSaveRequest.StepDocumentRequest::documentTypeCode
            );
        }
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
            throw new ApiException(
                    ErrorCode.INVALID_PAGE_REQUEST,
                    HttpStatus.BAD_REQUEST,
                    "page must be 1 or greater and size must be between 1 and 100."
            );
        }
    }

    /**
     * 요청 값과 업무 규칙을 검증합니다.
     *
     * @param minAmount 입력 값
     *
     * @param maxAmount 입력 값
     */
    private void validateAmountRange(BigDecimal minAmount, BigDecimal maxAmount) {
        if (minAmount != null && maxAmount != null && minAmount.compareTo(maxAmount) > 0) {
            throw validation("minAmount must be less than or equal to maxAmount.");
        }
    }

    /**
     * 요청 값과 업무 규칙을 검증합니다.
     *
     * @param comparatorCode 입력 값
     *
     * @param valueNumber 입력 값
     *
     * @param minNumber 입력 값
     *
     * @param maxNumber 입력 값
     */
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

    /**
     * 요청 값과 업무 규칙을 검증합니다.
     *
     * @param standardFieldId 입력 값
     *
     * @param scopeCode 입력 값
     *
     * @param allowedFieldTypeCodes 입력 값
     */
    private void validateConditionStandardField(
            UUID standardFieldId,
            String scopeCode,
            Set<String> allowedFieldTypeCodes
    ) {
        if (standardFieldId == null) {
            return;
        }
        AnnouncementStandardDocumentFieldRow field = selectStandardDocumentField(standardFieldId);
        String conditionUsageCode = normalizeOptionalCode(field.conditionUsageCode());
        if (!"CONDITION_READY".equals(conditionUsageCode)) {
            if ("STANDARDIZATION_REQUIRED".equals(conditionUsageCode)) {
                throw validation("표준 코드 매핑이 필요한 항목은 자동 조건으로 저장할 수 없습니다. 업종은 표준산업분류 코드(KSIC) 업종 조건으로 입력해 주세요.");
            }
            throw validation("이 표준 서류 항목은 조건으로 사용할 수 없습니다.");
        }
        if (!allowedFieldTypeCodes.contains(field.fieldTypeCode())) {
            throw validation("선택한 표준 서류 항목은 이 조건 유형에 사용할 수 없습니다.");
        }
        if (!field.scopeCode().equals(scopeCode)) {
            throw validation("표준 서류 항목의 적용 범위와 조건 범위가 일치해야 합니다.");
        }
    }

    /**
     * 요청 값과 업무 규칙을 검증합니다.
     *
     * @param standardFieldId 입력 값
     *
     * @param documentTypeCode 입력 값
     */
    private void validateDocumentRequirementStandardField(UUID standardFieldId, String documentTypeCode) {
        if (standardFieldId == null) {
            return;
        }
        AnnouncementStandardDocumentFieldRow field = selectStandardDocumentField(standardFieldId);
        if (!field.documentTypeCode().equals(normalizeOptionalCode(documentTypeCode))) {
            throw validation("필요 서류와 표준 서류 항목의 서류 종류가 일치해야 합니다.");
        }
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param standardFieldId 입력 값
     *
     * @return 처리 결과
     */
    private AnnouncementStandardDocumentFieldRow selectStandardDocumentField(UUID standardFieldId) {
        AnnouncementStandardDocumentFieldRow field = announcementDao.selectStandardDocumentFieldDetails(standardFieldId);
        if (field == null || !Boolean.TRUE.equals(field.selectable())) {
            throw validation("선택할 수 없는 표준 서류 항목입니다.");
        }
        return field;
    }

    /**
     * 요청 값과 업무 규칙을 검증합니다.
     *
     * @param fieldName 입력 값
     *
     * @param values 입력 값
     *
     * @param keySelector 입력 값
     *
     * @return 처리 결과
     */
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

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param authentication 입력 값
     *
     * @return 처리 결과
     */
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
            throw validation(fieldName + " is not supported.");
        }
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
        String normalizedValue = normalizeOptionalCode(value);
        if (normalizedValue == null || !allowedValues.contains(normalizedValue)) {
            throw validation(fieldName + " is not supported.");
        }
        return normalizedValue;
    }

    /**
     * 입력 값을 표준 형식으로 정규화합니다.
     *
     * @param value 입력 값
     *
     * @return 처리 결과
     */
    private String normalizeOptionalCode(String value) {
        String normalizedValue = trimToNull(value);
        return normalizedValue == null ? null : normalizedValue.toUpperCase(Locale.ROOT);
    }

    /**
     * 입력 값을 표준 형식으로 정규화합니다.
     *
     * @param value 입력 값
     *
     * @return 처리 결과
     */
    private String normalizeText(String value) {
        String normalizedValue = trimToNull(value);
        if (normalizedValue == null) {
            throw validation("required text value is blank.");
        }
        return normalizedValue;
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
        String trimmedValue = value.trim();
        return trimmedValue.isEmpty() ? null : trimmedValue;
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
     * @param message 입력 값
     *
     * @return 처리 결과
     */
    private ApiException validation(String message) {
        return new ApiException(ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST, message);
    }

    /**
     * 업무 데이터를 수정합니다.
     *
     * @param announcementId 입력 값
     *
     * @param approvalStatusCode 입력 값
     *
     * @param actorUserId 입력 값
     */
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

    /**
     * 업무 처리를 수행합니다.
     *
     * @param message 입력 값
     *
     * @return 처리 결과
     */
    private ApiException invalidStatusTransition(String message) {
        return new ApiException(ErrorCode.INVALID_STATUS_TRANSITION, HttpStatus.CONFLICT, message);
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @return 처리 결과
     */
    private ApiException notFound() {
        return new ApiException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "Announcement was not found.");
    }
}
