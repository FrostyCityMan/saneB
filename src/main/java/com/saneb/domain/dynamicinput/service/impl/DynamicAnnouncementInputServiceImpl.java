/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: DynamicAnnouncementInputServiceImpl.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.dynamicinput.service.impl;

import com.saneb.common.error.ApiException;
import com.saneb.common.error.ErrorCode;
import com.saneb.domain.auth.vo.AuthenticatedUserDetails;
import com.saneb.domain.dynamicinput.dao.DynamicAnnouncementInputDao;
import com.saneb.domain.dynamicinput.dto.AnnouncementInputRequirementsResponse;
import com.saneb.domain.dynamicinput.dto.AnnouncementInputRequirementsSaveRequest;
import com.saneb.domain.dynamicinput.dto.ApplicationInputValuesResponse;
import com.saneb.domain.dynamicinput.dto.ApplicationInputValuesSaveRequest;
import com.saneb.domain.dynamicinput.dto.StandardDocumentFieldResponse;
import com.saneb.domain.dynamicinput.service.DynamicAnnouncementInputService;
import com.saneb.domain.dynamicinput.vo.AnnouncementInputOptionCommand;
import com.saneb.domain.dynamicinput.vo.AnnouncementInputOptionRow;
import com.saneb.domain.dynamicinput.vo.AnnouncementInputRequirementCommand;
import com.saneb.domain.dynamicinput.vo.AnnouncementInputRequirementRow;
import com.saneb.domain.dynamicinput.vo.ApplicationInputValueCommand;
import com.saneb.domain.dynamicinput.vo.ApplicationInputValueRow;
import com.saneb.domain.dynamicinput.vo.ApplicationProgressInputRow;
import com.saneb.domain.dynamicinput.vo.AuditLogCommand;
import com.saneb.domain.dynamicinput.vo.StandardDocumentFieldRow;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
public class DynamicAnnouncementInputServiceImpl implements DynamicAnnouncementInputService {

    private static final String RESOURCE_TYPE = "APPLICATION_PROGRESS";
    private static final Set<String> FIELD_TYPE_CODES = Set.of(
            "TEXT", "TEXTAREA", "NUMBER", "AMOUNT", "DATE", "BOOLEAN", "SELECT", "RADIO", "MULTI_SELECT"
    );
    private static final Set<String> OPTION_FIELD_TYPE_CODES = Set.of("SELECT", "RADIO", "MULTI_SELECT");
    private static final Set<String> SCOPE_CODES = Set.of(
            "BUSINESS", "PERSONAL", "SPOUSE", "CHILD", "PARENT", "APPLICATION", "SUPPORT"
    );
    private static final Set<String> OPERATING_ROLES = Set.of("PARTNER", "OPERATOR", "APPROVER", "ADMIN");
    private static final Set<String> DOCUMENT_TYPE_CODES = Set.of(
            "BUSINESS_REGISTRATION",
            "VAT_TAX_BASE",
            "TAX_EXEMPT_INCOME",
            "INCOME_CERTIFICATE",
            "NATIONAL_TAX_PAID",
            "LOCAL_TAX_PAID",
            "RESIDENT_REGISTRATION",
            "FAMILY_RELATION",
            "HEALTH_INSURANCE_PAYMENT",
            "HEALTH_INSURANCE_QUALIFICATION"
    );

    private final DynamicAnnouncementInputDao dynamicAnnouncementInputDao;

    /**
     * 객체를 생성합니다.
     *
     * @param dynamicAnnouncementInputDao 입력 값
     */
    public DynamicAnnouncementInputServiceImpl(DynamicAnnouncementInputDao dynamicAnnouncementInputDao) {
        this.dynamicAnnouncementInputDao = dynamicAnnouncementInputDao;
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param announcementId 입력 값
     *
     * @return 처리 결과
     */
    @Override
    public AnnouncementInputRequirementsResponse selectAnnouncementInputRequirements(UUID announcementId) {
        validateAnnouncementExists(announcementId);
        return toRequirementsResponse(
                announcementId,
                dynamicAnnouncementInputDao.selectAnnouncementInputRequirementList(announcementId),
                dynamicAnnouncementInputDao.selectAnnouncementInputOptionList(announcementId)
        );
    }

    /**
     * 업무 데이터를 저장합니다.
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
     * 업무 데이터를 저장합니다.
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
    public AnnouncementInputRequirementsResponse saveAnnouncementInputRequirements(
            Authentication authentication,
            UUID announcementId,
            AnnouncementInputRequirementsSaveRequest request
    ) {
        UUID actorUserId = selectRequiredPrincipal(authentication).userId();
        validateAnnouncementExists(announcementId);
        validateRequirementsRequest(request);

        List<AnnouncementInputRequirementRow> existingRequirements =
                dynamicAnnouncementInputDao.selectAnnouncementInputRequirementList(announcementId);
        Map<String, AnnouncementInputRequirementRow> existingByFieldKey = existingRequirements.stream()
                .collect(Collectors.toMap(AnnouncementInputRequirementRow::fieldKey, Function.identity()));
        boolean progressExists = dynamicAnnouncementInputDao.selectApplicationProgressCountByAnnouncementId(announcementId) > 0;

        List<AnnouncementInputRequirementsSaveRequest.RequirementRequest> requestedRequirements =
                nullToEmpty(request.requirements());
        Set<String> requestedFieldKeys = requestedRequirements.stream()
                .map(requirement -> normalizeCode(requirement.fieldKey()))
                .collect(Collectors.toCollection(HashSet::new));

        if (progressExists) {
            for (AnnouncementInputRequirementRow existing : existingRequirements) {
                if (!requestedFieldKeys.contains(existing.fieldKey())) {
                    throw validationFailed("Existing requirement cannot be deleted after progress exists.");
                }
            }
        }

        for (AnnouncementInputRequirementsSaveRequest.RequirementRequest requirement : requestedRequirements) {
            String fieldKey = normalizeCode(requirement.fieldKey());
            String fieldTypeCode = normalizeRequiredCode("fieldTypeCode", requirement.fieldTypeCode(), FIELD_TYPE_CODES);
            String scopeCode = normalizeRequiredCode("scopeCode", requirement.scopeCode(), SCOPE_CODES);
            AnnouncementInputRequirementRow existing = existingByFieldKey.get(fieldKey);
            if (existing != null && progressExists) {
                validateImmutableRequirement(existing, fieldTypeCode, scopeCode, requirement.sensitive(), requirement.standardFieldId());
            }
            StandardDocumentFieldRow standardField = selectSelectableStandardField(requirement.standardFieldId());
            validateRequirementStandardField(standardField, fieldKey, fieldTypeCode, scopeCode);

            UUID requirementId = existing == null ? UUID.randomUUID() : existing.requirementId();
            AnnouncementInputRequirementCommand command = new AnnouncementInputRequirementCommand(
                    requirementId,
                    announcementId,
                    fieldKey,
                    normalizeRequiredText(requirement.fieldLabel()),
                    fieldTypeCode,
                    scopeCode,
                    Boolean.TRUE.equals(requirement.required()),
                    Boolean.TRUE.equals(requirement.sensitive()),
                    requirement.sortOrder(),
                    requirement.standardFieldId(),
                    trimToNull(requirement.helpText()),
                    actorUserId
            );
            if (existing == null) {
                dynamicAnnouncementInputDao.insertAnnouncementInputRequirement(command);
            } else {
                dynamicAnnouncementInputDao.updateAnnouncementInputRequirement(command);
            }
            saveOptions(requirementId, fieldTypeCode, requirement.options(), actorUserId);
        }

        if (!progressExists) {
            for (AnnouncementInputRequirementRow existing : existingRequirements) {
                if (!requestedFieldKeys.contains(existing.fieldKey())) {
                    dynamicAnnouncementInputDao.deleteAnnouncementInputOptionsByRequirementId(existing.requirementId());
                    dynamicAnnouncementInputDao.deleteAnnouncementInputRequirement(existing.requirementId());
                }
            }
        }

        return selectAnnouncementInputRequirements(announcementId);
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param authentication 입력 값
     *
     * @param progressId 입력 값
     *
     * @return 처리 결과
     */
    @Override
    public ApplicationInputValuesResponse selectApplicationInputValues(Authentication authentication, UUID progressId) {
        AuthenticatedUserDetails actor = selectRequiredPrincipal(authentication);
        ApplicationProgressInputRow progress = selectAuthorizedProgress(actor, progressId);
        return selectApplicationInputValuesResponse(progress);
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param documentTypeCode 입력 값
     *
     * @param scopeCode 입력 값
     *
     * @return 처리 결과
     */
    @Override
    public List<StandardDocumentFieldResponse> selectStandardDocumentFieldList(
            String documentTypeCode,
            String scopeCode
    ) {
        String normalizedDocumentTypeCode = normalizeOptionalCode(documentTypeCode);
        String normalizedScopeCode = normalizeOptionalCode(scopeCode);
        validateOptionalCode("documentTypeCode", normalizedDocumentTypeCode, DOCUMENT_TYPE_CODES);
        validateOptionalCode("scopeCode", normalizedScopeCode, SCOPE_CODES);
        return dynamicAnnouncementInputDao.selectStandardDocumentFieldList(
                        normalizedDocumentTypeCode,
                        normalizedScopeCode
                )
                .stream()
                .map(this::toStandardDocumentFieldResponse)
                .toList();
    }

    /**
     * 업무 데이터를 저장합니다.
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
     * 업무 데이터를 저장합니다.
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
    public ApplicationInputValuesResponse saveApplicationInputValues(
            Authentication authentication,
            UUID progressId,
            ApplicationInputValuesSaveRequest request
    ) {
        AuthenticatedUserDetails actor = selectRequiredPrincipal(authentication);
        ApplicationProgressInputRow progress = selectAuthorizedProgress(actor, progressId);
        List<AnnouncementInputRequirementRow> requirements =
                dynamicAnnouncementInputDao.selectAnnouncementInputRequirementList(progress.announcementId());
        Map<UUID, AnnouncementInputRequirementRow> requirementsById = requirements.stream()
                .collect(Collectors.toMap(AnnouncementInputRequirementRow::requirementId, Function.identity()));
        Map<UUID, Set<String>> optionCodesByRequirementId = dynamicAnnouncementInputDao
                .selectAnnouncementInputOptionList(progress.announcementId())
                .stream()
                .collect(Collectors.groupingBy(
                        AnnouncementInputOptionRow::requirementId,
                        Collectors.mapping(AnnouncementInputOptionRow::optionCode, Collectors.toSet())
                ));

        List<ApplicationInputValuesSaveRequest.InputValueRequest> values = nullToEmpty(request.values());
        validateInputValuesRequest(values, requirements, requirementsById, optionCodesByRequirementId);

        dynamicAnnouncementInputDao.deleteApplicationInputValues(progressId);
        int insertedRowCount = 0;
        int sensitiveRowCount = 0;
        for (ApplicationInputValuesSaveRequest.InputValueRequest value : values) {
            AnnouncementInputRequirementRow requirement = requirementsById.get(value.requirementId());
            List<ApplicationInputValueCommand> commands = toInputValueCommands(
                    progressId,
                    requirement,
                    value,
                    actor.userId()
            );
            for (ApplicationInputValueCommand command : commands) {
                dynamicAnnouncementInputDao.insertApplicationInputValue(command);
                insertedRowCount++;
                if (Boolean.TRUE.equals(requirement.sensitive())) {
                    sensitiveRowCount++;
                }
            }
        }
        dynamicAnnouncementInputDao.touchApplicationProgress(progressId, actor.userId());

        insertAudit(actor.userId(), progressId, metadata(
                "requirementCount", String.valueOf(values.size()),
                "rowCount", String.valueOf(insertedRowCount),
                "sensitiveCount", String.valueOf(sensitiveRowCount)
        ));
        return selectApplicationInputValuesResponse(progress);
    }

    /**
     * 업무 데이터를 저장합니다.
     *
     * @param requirementId 입력 값
     *
     * @param fieldTypeCode 입력 값
     *
     * @param options 입력 값
     *
     * @param actorUserId 입력 값
     */
    private void saveOptions(
            UUID requirementId,
            String fieldTypeCode,
            List<AnnouncementInputRequirementsSaveRequest.OptionRequest> options,
            UUID actorUserId
    ) {
        List<AnnouncementInputRequirementsSaveRequest.OptionRequest> requestedOptions = nullToEmpty(options);
        List<AnnouncementInputOptionRow> existingOptions = selectOptionRows(requirementId);
        Map<String, AnnouncementInputOptionRow> existingByCode = existingOptions.stream()
                .collect(Collectors.toMap(AnnouncementInputOptionRow::optionCode, Function.identity()));
        Set<String> requestedOptionCodes = requestedOptions.stream()
                .map(option -> normalizeCode(option.optionCode()))
                .collect(Collectors.toCollection(HashSet::new));

        for (AnnouncementInputOptionRow existing : existingOptions) {
            if (!requestedOptionCodes.contains(existing.optionCode())) {
                if (dynamicAnnouncementInputDao.selectApplicationInputValueCountByRequirementOption(
                        requirementId,
                        existing.optionCode()
                ) > 0) {
                    throw validationFailed("Option code cannot be deleted because submitted input values exist.");
                }
                dynamicAnnouncementInputDao.deleteAnnouncementInputOption(requirementId, existing.optionCode());
            }
        }

        for (AnnouncementInputRequirementsSaveRequest.OptionRequest option : requestedOptions) {
            String optionCode = normalizeCode(option.optionCode());
            AnnouncementInputOptionRow existing = existingByCode.get(optionCode);
            AnnouncementInputOptionCommand command = new AnnouncementInputOptionCommand(
                    existing == null ? UUID.randomUUID() : existing.optionId(),
                    requirementId,
                    optionCode,
                    normalizeRequiredText(option.optionLabel()),
                    option.sortOrder(),
                    actorUserId
            );
            if (existing == null) {
                dynamicAnnouncementInputDao.insertAnnouncementInputOption(command);
            } else {
                dynamicAnnouncementInputDao.updateAnnouncementInputOption(command);
            }
        }

        if (OPTION_FIELD_TYPE_CODES.contains(fieldTypeCode) && requestedOptions.isEmpty()) {
            throw validationFailed("Option field type requires at least one option.");
        }
        if (!OPTION_FIELD_TYPE_CODES.contains(fieldTypeCode) && !requestedOptions.isEmpty()) {
            throw validationFailed("Non-option field type cannot have options.");
        }
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param requirementId 입력 값
     *
     * @return 처리 결과
     */
    private List<AnnouncementInputOptionRow> selectOptionRows(UUID requirementId) {
        return dynamicAnnouncementInputDao.selectAnnouncementInputOptionListByRequirementId(requirementId);
    }

    /**
     * 요청 값과 업무 규칙을 검증합니다.
     *
     * @param request 입력 값
     */
    private void validateRequirementsRequest(AnnouncementInputRequirementsSaveRequest request) {
        List<AnnouncementInputRequirementsSaveRequest.RequirementRequest> requirements = nullToEmpty(request.requirements());
        validateUnique("fieldKey", requirements, requirement -> normalizeCode(requirement.fieldKey()));
        for (AnnouncementInputRequirementsSaveRequest.RequirementRequest requirement : requirements) {
            String fieldTypeCode = normalizeRequiredCode("fieldTypeCode", requirement.fieldTypeCode(), FIELD_TYPE_CODES);
            normalizeRequiredCode("scopeCode", requirement.scopeCode(), SCOPE_CODES);
            if (requirement.sortOrder() < 0) {
                throw validationFailed("sortOrder must be zero or positive.");
            }
            StandardDocumentFieldRow standardField = selectSelectableStandardField(requirement.standardFieldId());
            validateRequirementStandardField(
                    standardField,
                    normalizeCode(requirement.fieldKey()),
                    fieldTypeCode,
                    normalizeRequiredCode("scopeCode", requirement.scopeCode(), SCOPE_CODES)
            );
            validateUnique("optionCode", nullToEmpty(requirement.options()), option -> normalizeCode(option.optionCode()));
            if (OPTION_FIELD_TYPE_CODES.contains(fieldTypeCode) && nullToEmpty(requirement.options()).isEmpty()) {
                throw validationFailed("Option field type requires at least one option.");
            }
            if (!OPTION_FIELD_TYPE_CODES.contains(fieldTypeCode) && !nullToEmpty(requirement.options()).isEmpty()) {
                throw validationFailed("Non-option field type cannot have options.");
            }
        }
    }

    /**
     * 요청 값과 업무 규칙을 검증합니다.
     *
     * @param existing 입력 값
     *
     * @param fieldTypeCode 입력 값
     *
     * @param scopeCode 입력 값
     *
     * @param sensitive 입력 값
     *
     * @param standardFieldId 입력 값
     */
    private void validateImmutableRequirement(
            AnnouncementInputRequirementRow existing,
            String fieldTypeCode,
            String scopeCode,
            Boolean sensitive,
            UUID standardFieldId
    ) {
        if (!existing.fieldTypeCode().equals(fieldTypeCode)
                || !existing.scopeCode().equals(scopeCode)
                || Boolean.TRUE.equals(existing.sensitive()) != Boolean.TRUE.equals(sensitive)
                || !equalsNullable(existing.standardFieldId(), standardFieldId)) {
            throw validationFailed("fieldTypeCode, scopeCode, sensitive, and standardFieldId cannot be changed after progress exists.");
        }
    }

    /**
     * 요청 값과 업무 규칙을 검증합니다.
     *
     * @param values 입력 값
     *
     * @param requirements 입력 값
     *
     * @param requirementsById 입력 값
     *
     * @param optionCodesByRequirementId 입력 값
     */
    private void validateInputValuesRequest(
            List<ApplicationInputValuesSaveRequest.InputValueRequest> values,
            List<AnnouncementInputRequirementRow> requirements,
            Map<UUID, AnnouncementInputRequirementRow> requirementsById,
            Map<UUID, Set<String>> optionCodesByRequirementId
    ) {
        validateUnique("requirementId", values, ApplicationInputValuesSaveRequest.InputValueRequest::requirementId);
        Set<UUID> submittedRequirementIds = values.stream()
                .map(ApplicationInputValuesSaveRequest.InputValueRequest::requirementId)
                .collect(Collectors.toSet());
        for (AnnouncementInputRequirementRow requirement : requirements) {
            if (Boolean.TRUE.equals(requirement.required()) && !submittedRequirementIds.contains(requirement.requirementId())) {
                throw progressConditionNotMet("Required input value is missing.");
            }
        }
        for (ApplicationInputValuesSaveRequest.InputValueRequest value : values) {
            AnnouncementInputRequirementRow requirement = requirementsById.get(value.requirementId());
            if (requirement == null) {
                throw validationFailed("requirementId does not belong to the progress announcement.");
            }
            validateValueForFieldType(value, requirement, optionCodesByRequirementId.getOrDefault(
                    requirement.requirementId(),
                    Set.of()
            ));
        }
    }

    /**
     * 요청 값과 업무 규칙을 검증합니다.
     *
     * @param value 입력 값
     *
     * @param requirement 입력 값
     *
     * @param validOptionCodes 입력 값
     */
    private void validateValueForFieldType(
            ApplicationInputValuesSaveRequest.InputValueRequest value,
            AnnouncementInputRequirementRow requirement,
            Set<String> validOptionCodes
    ) {
        String fieldTypeCode = requirement.fieldTypeCode();
        if ("TEXT".equals(fieldTypeCode) || "TEXTAREA".equals(fieldTypeCode)) {
            if (trimToNull(value.valueText()) == null || scalarCount(value) != 1 || hasAnyOption(value)) {
                throw validationFailed("Text input requires valueText only.");
            }
            return;
        }
        if ("NUMBER".equals(fieldTypeCode) || "AMOUNT".equals(fieldTypeCode)) {
            if (value.valueNumber() == null || scalarCount(value) != 1 || hasAnyOption(value)) {
                throw validationFailed("Number input requires valueNumber only.");
            }
            return;
        }
        if ("DATE".equals(fieldTypeCode)) {
            if (value.valueDate() == null || scalarCount(value) != 1 || hasAnyOption(value)) {
                throw validationFailed("Date input requires valueDate only.");
            }
            return;
        }
        if ("BOOLEAN".equals(fieldTypeCode)) {
            if (value.valueBoolean() == null || scalarCount(value) != 1 || hasAnyOption(value)) {
                throw validationFailed("Boolean input requires valueBoolean only.");
            }
            return;
        }
        if ("SELECT".equals(fieldTypeCode) || "RADIO".equals(fieldTypeCode)) {
            String optionCode = normalizeOptionalCode(value.optionCode());
            if (optionCode == null || scalarCount(value) != 0 || !nullToEmpty(value.optionCodes()).isEmpty()) {
                throw validationFailed("Single option input requires optionCode only.");
            }
            validateOptionCode(optionCode, validOptionCodes);
            return;
        }
        if ("MULTI_SELECT".equals(fieldTypeCode)) {
            List<String> optionCodes = nullToEmpty(value.optionCodes()).stream()
                    .map(this::normalizeOptionalCode)
                    .toList();
            if (optionCodes.isEmpty() || optionCodes.stream().anyMatch(optionCode -> optionCode == null)
                    || optionCodes.stream().distinct().count() != optionCodes.size()
                    || scalarCount(value) != 0 || normalizeOptionalCode(value.optionCode()) != null) {
                throw validationFailed("Multi select input requires unique optionCodes only.");
            }
            optionCodes.forEach(optionCode -> validateOptionCode(optionCode, validOptionCodes));
        }
    }

    /**
     * 업무 데이터를 응답 형식으로 변환합니다.
     *
     * @param progressId 입력 값
     *
     * @param requirement 입력 값
     *
     * @param value 입력 값
     *
     * @param actorUserId 입력 값
     *
     * @return 처리 결과
     */
    private List<ApplicationInputValueCommand> toInputValueCommands(
            UUID progressId,
            AnnouncementInputRequirementRow requirement,
            ApplicationInputValuesSaveRequest.InputValueRequest value,
            UUID actorUserId
    ) {
        String fieldTypeCode = requirement.fieldTypeCode();
        if ("MULTI_SELECT".equals(fieldTypeCode)) {
            return nullToEmpty(value.optionCodes()).stream()
                    .map(this::normalizeCode)
                    .map(optionCode -> optionValueCommand(progressId, requirement.requirementId(), optionCode, actorUserId))
                    .toList();
        }
        if ("SELECT".equals(fieldTypeCode) || "RADIO".equals(fieldTypeCode)) {
            return List.of(optionValueCommand(
                    progressId,
                    requirement.requirementId(),
                    normalizeCode(value.optionCode()),
                    actorUserId
            ));
        }
        return List.of(new ApplicationInputValueCommand(
                UUID.randomUUID(),
                progressId,
                requirement.requirementId(),
                trimToNull(value.valueText()),
                value.valueNumber(),
                value.valueDate(),
                value.valueBoolean(),
                null,
                actorUserId
        ));
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param progressId 입력 값
     *
     * @param requirementId 입력 값
     *
     * @param optionCode 입력 값
     *
     * @param actorUserId 입력 값
     *
     * @return 처리 결과
     */
    private ApplicationInputValueCommand optionValueCommand(
            UUID progressId,
            UUID requirementId,
            String optionCode,
            UUID actorUserId
    ) {
        return new ApplicationInputValueCommand(
                UUID.randomUUID(),
                progressId,
                requirementId,
                null,
                null,
                null,
                null,
                optionCode,
                actorUserId
        );
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param progress 입력 값
     *
     * @return 처리 결과
     */
    private ApplicationInputValuesResponse selectApplicationInputValuesResponse(ApplicationProgressInputRow progress) {
        List<AnnouncementInputRequirementRow> requirements =
                dynamicAnnouncementInputDao.selectAnnouncementInputRequirementList(progress.announcementId());
        Map<UUID, List<ApplicationInputValueRow>> valuesByRequirementId =
                dynamicAnnouncementInputDao.selectApplicationInputValueList(progress.progressId()).stream()
                        .collect(Collectors.groupingBy(ApplicationInputValueRow::requirementId));
        List<ApplicationInputValuesResponse.InputValueResponse> values = requirements.stream()
                .map(requirement -> toInputValueResponse(
                        requirement,
                        valuesByRequirementId.getOrDefault(requirement.requirementId(), List.of())
                ))
                .toList();
        return new ApplicationInputValuesResponse(progress.progressId(), progress.announcementId(), values);
    }

    /**
     * 업무 데이터를 응답 형식으로 변환합니다.
     *
     * @param requirement 입력 값
     *
     * @param rows 입력 값
     *
     * @return 처리 결과
     */
    private ApplicationInputValuesResponse.InputValueResponse toInputValueResponse(
            AnnouncementInputRequirementRow requirement,
            List<ApplicationInputValueRow> rows
    ) {
        List<ApplicationInputValueRow> sortedRows = rows.stream()
                .sorted(Comparator.comparing(row -> row.optionCode() == null ? "" : row.optionCode()))
                .toList();
        ApplicationInputValueRow firstRow = sortedRows.isEmpty() ? null : sortedRows.getFirst();
        List<String> optionCodes = sortedRows.stream()
                .map(ApplicationInputValueRow::optionCode)
                .filter(optionCode -> optionCode != null)
                .toList();
        return new ApplicationInputValuesResponse.InputValueResponse(
                requirement.requirementId(),
                requirement.fieldKey(),
                requirement.fieldLabel(),
                requirement.fieldTypeCode(),
                requirement.scopeCode(),
                Boolean.TRUE.equals(requirement.required()),
                Boolean.TRUE.equals(requirement.sensitive()),
                requirement.sortOrder(),
                requirement.helpText(),
                firstRow == null ? null : firstRow.valueText(),
                firstRow == null ? null : firstRow.valueNumber(),
                firstRow == null ? null : firstRow.valueDate(),
                firstRow == null ? null : firstRow.valueBoolean(),
                firstRow == null ? null : firstRow.optionCode(),
                optionCodes,
                firstRow == null ? null : firstRow.submittedBy(),
                firstRow == null ? null : firstRow.submittedAt()
        );
    }

    /**
     * 업무 데이터를 응답 형식으로 변환합니다.
     *
     * @param announcementId 입력 값
     *
     * @param requirements 입력 값
     *
     * @param options 입력 값
     *
     * @return 처리 결과
     */
    private AnnouncementInputRequirementsResponse toRequirementsResponse(
            UUID announcementId,
            List<AnnouncementInputRequirementRow> requirements,
            List<AnnouncementInputOptionRow> options
    ) {
        Map<UUID, List<AnnouncementInputOptionRow>> optionsByRequirementId = nullToEmpty(options).stream()
                .collect(Collectors.groupingBy(
                        AnnouncementInputOptionRow::requirementId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
        return new AnnouncementInputRequirementsResponse(
                announcementId,
                nullToEmpty(requirements).stream()
                        .map(requirement -> toRequirementResponse(
                                requirement,
                                optionsByRequirementId.getOrDefault(requirement.requirementId(), List.of())
                        ))
                        .toList()
        );
    }

    /**
     * 업무 데이터를 응답 형식으로 변환합니다.
     *
     * @param requirement 입력 값
     *
     * @param options 입력 값
     *
     * @return 처리 결과
     */
    private AnnouncementInputRequirementsResponse.RequirementResponse toRequirementResponse(
            AnnouncementInputRequirementRow requirement,
            List<AnnouncementInputOptionRow> options
    ) {
        return new AnnouncementInputRequirementsResponse.RequirementResponse(
                requirement.requirementId(),
                requirement.fieldKey(),
                requirement.fieldLabel(),
                requirement.fieldTypeCode(),
                requirement.scopeCode(),
                Boolean.TRUE.equals(requirement.required()),
                Boolean.TRUE.equals(requirement.sensitive()),
                requirement.sortOrder(),
                requirement.standardFieldId(),
                requirement.helpText(),
                nullToEmpty(options).stream()
                        .map(option -> new AnnouncementInputRequirementsResponse.OptionResponse(
                                option.optionId(),
                                option.optionCode(),
                                option.optionLabel(),
                                option.sortOrder()
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
    private StandardDocumentFieldResponse toStandardDocumentFieldResponse(StandardDocumentFieldRow row) {
        return new StandardDocumentFieldResponse(
                row.standardFieldId(),
                row.documentTypeCode(),
                row.fieldKey(),
                row.fieldLabel(),
                row.fieldTypeCode(),
                row.scopeCode(),
                Boolean.TRUE.equals(row.requiredDefault()),
                Boolean.TRUE.equals(row.conditionEligible()),
                selectConditionUsageCode(row.conditionUsageCode(), row.conditionEligible()),
                row.sortOrder() == null ? 0 : row.sortOrder(),
                row.helpText()
        );
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param conditionUsageCode 입력 값
     *
     * @param conditionEligible 입력 값
     *
     * @return 처리 결과
     */
    private String selectConditionUsageCode(String conditionUsageCode, Boolean conditionEligible) {
        String normalized = normalizeOptionalCode(conditionUsageCode);
        if (normalized != null) {
            return normalized;
        }
        return Boolean.TRUE.equals(conditionEligible) ? "CONDITION_READY" : "INPUT_ONLY";
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param actor 입력 값
     *
     * @param progressId 입력 값
     *
     * @return 처리 결과
     */
    private ApplicationProgressInputRow selectAuthorizedProgress(AuthenticatedUserDetails actor, UUID progressId) {
        ApplicationProgressInputRow progress = dynamicAnnouncementInputDao.selectApplicationProgressForInput(progressId);
        if (progress == null) {
            throw notFound("Application progress was not found.");
        }
        if (!hasOperatingRole(actor) && !progress.memberUserId().equals(actor.userId())) {
            throw new ApiException(ErrorCode.AUTH_FORBIDDEN, HttpStatus.FORBIDDEN, "Progress access is forbidden.");
        }
        return progress;
    }

    /**
     * 요청 값과 업무 규칙을 검증합니다.
     *
     * @param announcementId 입력 값
     */
    private void validateAnnouncementExists(UUID announcementId) {
        if (dynamicAnnouncementInputDao.selectAnnouncementCount(announcementId) == 0) {
            throw notFound("Announcement was not found.");
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
     * 업무 데이터를 조회합니다.
     *
     * @param standardFieldId 입력 값
     *
     * @return 처리 결과
     */
    private StandardDocumentFieldRow selectSelectableStandardField(UUID standardFieldId) {
        if (standardFieldId == null) {
            return null;
        }
        StandardDocumentFieldRow field = dynamicAnnouncementInputDao.selectStandardDocumentFieldDetails(standardFieldId);
        if (field == null || !Boolean.TRUE.equals(field.selectable())) {
            throw validationFailed("선택할 수 없는 표준 서류 항목입니다.");
        }
        return field;
    }

    /**
     * 요청 값과 업무 규칙을 검증합니다.
     *
     * @param standardField 입력 값
     *
     * @param fieldKey 입력 값
     *
     * @param fieldTypeCode 입력 값
     *
     * @param scopeCode 입력 값
     */
    private void validateRequirementStandardField(
            StandardDocumentFieldRow standardField,
            String fieldKey,
            String fieldTypeCode,
            String scopeCode
    ) {
        if (standardField == null) {
            return;
        }
        if (!standardField.fieldKey().equals(fieldKey)
                || !standardField.fieldTypeCode().equals(fieldTypeCode)
                || !standardField.scopeCode().equals(scopeCode)) {
            throw validationFailed("표준 서류 항목과 입력 항목의 식별값, 입력 유형, 적용 범위가 일치해야 합니다.");
        }
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param value 입력 값
     *
     * @return 처리 결과
     */
    private int scalarCount(ApplicationInputValuesSaveRequest.InputValueRequest value) {
        int count = 0;
        if (trimToNull(value.valueText()) != null) {
            count++;
        }
        if (value.valueNumber() != null) {
            count++;
        }
        if (value.valueDate() != null) {
            count++;
        }
        if (value.valueBoolean() != null) {
            count++;
        }
        return count;
    }

    /**
     * 조건 충족 여부를 확인합니다.
     *
     * @param value 입력 값
     *
     * @return 처리 결과
     */
    private boolean hasAnyOption(ApplicationInputValuesSaveRequest.InputValueRequest value) {
        return normalizeOptionalCode(value.optionCode()) != null || !nullToEmpty(value.optionCodes()).isEmpty();
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param left 입력 값
     *
     * @param right 입력 값
     *
     * @return 처리 결과
     */
    private boolean equalsNullable(UUID left, UUID right) {
        return left == null ? right == null : left.equals(right);
    }

    /**
     * 요청 값과 업무 규칙을 검증합니다.
     *
     * @param optionCode 입력 값
     *
     * @param validOptionCodes 입력 값
     */
    private void validateOptionCode(String optionCode, Set<String> validOptionCodes) {
        if (!validOptionCodes.contains(optionCode)) {
            throw validationFailed("optionCode does not belong to the requirement.");
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
     * 입력 값을 표준 형식으로 정규화합니다.
     *
     * @param value 입력 값
     *
     * @return 처리 결과
     */
    private String normalizeCode(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw validationFailed("required text value is blank.");
        }
        return normalized.toUpperCase(Locale.ROOT);
    }

    /**
     * 입력 값을 표준 형식으로 정규화합니다.
     *
     * @param value 입력 값
     *
     * @return 처리 결과
     */
    private String normalizeRequiredText(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw validationFailed("required text value is blank.");
        }
        return normalized;
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
        List<K> keys = new ArrayList<>();
        for (T value : values) {
            K key = keySelector.apply(value);
            if (key != null) {
                keys.add(key);
            }
        }
        if (keys.stream().distinct().count() != keys.size()) {
            throw validationFailed(fieldName + " must be unique.");
        }
    }

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param actorUserId 입력 값
     *
     * @param progressId 입력 값
     *
     * @param metadataJson 입력 값
     */
    private void insertAudit(UUID actorUserId, UUID progressId, String metadataJson) {
        dynamicAnnouncementInputDao.insertAuditLog(new AuditLogCommand(
                actorUserId,
                "APPLICATION_INPUT_VALUES_SAVE",
                RESOURCE_TYPE,
                progressId,
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
     * @param message 입력 값
     *
     * @return 처리 결과
     */
    private ApiException progressConditionNotMet(String message) {
        return new ApiException(ErrorCode.PROGRESS_CONDITION_NOT_MET, HttpStatus.CONFLICT, message);
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param message 입력 값
     *
     * @return 처리 결과
     */
    private ApiException notFound(String message) {
        return new ApiException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, message);
    }
}
