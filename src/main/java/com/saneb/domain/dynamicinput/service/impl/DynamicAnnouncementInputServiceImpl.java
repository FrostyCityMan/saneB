package com.saneb.domain.dynamicinput.service.impl;

import com.saneb.common.error.ApiException;
import com.saneb.common.error.ErrorCode;
import com.saneb.domain.auth.vo.AuthenticatedUserDetails;
import com.saneb.domain.dynamicinput.dao.DynamicAnnouncementInputDao;
import com.saneb.domain.dynamicinput.dto.AnnouncementInputRequirementsResponse;
import com.saneb.domain.dynamicinput.dto.AnnouncementInputRequirementsSaveRequest;
import com.saneb.domain.dynamicinput.dto.ApplicationInputValuesResponse;
import com.saneb.domain.dynamicinput.dto.ApplicationInputValuesSaveRequest;
import com.saneb.domain.dynamicinput.service.DynamicAnnouncementInputService;
import com.saneb.domain.dynamicinput.vo.AnnouncementInputOptionCommand;
import com.saneb.domain.dynamicinput.vo.AnnouncementInputOptionRow;
import com.saneb.domain.dynamicinput.vo.AnnouncementInputRequirementCommand;
import com.saneb.domain.dynamicinput.vo.AnnouncementInputRequirementRow;
import com.saneb.domain.dynamicinput.vo.ApplicationInputValueCommand;
import com.saneb.domain.dynamicinput.vo.ApplicationInputValueRow;
import com.saneb.domain.dynamicinput.vo.ApplicationProgressInputRow;
import com.saneb.domain.dynamicinput.vo.AuditLogCommand;
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

    private final DynamicAnnouncementInputDao dynamicAnnouncementInputDao;

    public DynamicAnnouncementInputServiceImpl(DynamicAnnouncementInputDao dynamicAnnouncementInputDao) {
        this.dynamicAnnouncementInputDao = dynamicAnnouncementInputDao;
    }

    @Override
    public AnnouncementInputRequirementsResponse selectAnnouncementInputRequirements(UUID announcementId) {
        validateAnnouncementExists(announcementId);
        return toRequirementsResponse(
                announcementId,
                dynamicAnnouncementInputDao.selectAnnouncementInputRequirementList(announcementId),
                dynamicAnnouncementInputDao.selectAnnouncementInputOptionList(announcementId)
        );
    }

    @Override
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
                validateImmutableRequirement(existing, fieldTypeCode, scopeCode, requirement.sensitive());
            }

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

    @Override
    public ApplicationInputValuesResponse selectApplicationInputValues(Authentication authentication, UUID progressId) {
        AuthenticatedUserDetails actor = selectRequiredPrincipal(authentication);
        ApplicationProgressInputRow progress = selectAuthorizedProgress(actor, progressId);
        return selectApplicationInputValuesResponse(progress);
    }

    @Override
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

    private List<AnnouncementInputOptionRow> selectOptionRows(UUID requirementId) {
        return dynamicAnnouncementInputDao.selectAnnouncementInputOptionListByRequirementId(requirementId);
    }

    private void validateRequirementsRequest(AnnouncementInputRequirementsSaveRequest request) {
        List<AnnouncementInputRequirementsSaveRequest.RequirementRequest> requirements = nullToEmpty(request.requirements());
        validateUnique("fieldKey", requirements, requirement -> normalizeCode(requirement.fieldKey()));
        for (AnnouncementInputRequirementsSaveRequest.RequirementRequest requirement : requirements) {
            String fieldTypeCode = normalizeRequiredCode("fieldTypeCode", requirement.fieldTypeCode(), FIELD_TYPE_CODES);
            normalizeRequiredCode("scopeCode", requirement.scopeCode(), SCOPE_CODES);
            if (requirement.sortOrder() < 0) {
                throw validationFailed("sortOrder must be zero or positive.");
            }
            validateUnique("optionCode", nullToEmpty(requirement.options()), option -> normalizeCode(option.optionCode()));
            if (OPTION_FIELD_TYPE_CODES.contains(fieldTypeCode) && nullToEmpty(requirement.options()).isEmpty()) {
                throw validationFailed("Option field type requires at least one option.");
            }
            if (!OPTION_FIELD_TYPE_CODES.contains(fieldTypeCode) && !nullToEmpty(requirement.options()).isEmpty()) {
                throw validationFailed("Non-option field type cannot have options.");
            }
        }
    }

    private void validateImmutableRequirement(
            AnnouncementInputRequirementRow existing,
            String fieldTypeCode,
            String scopeCode,
            Boolean sensitive
    ) {
        if (!existing.fieldTypeCode().equals(fieldTypeCode)
                || !existing.scopeCode().equals(scopeCode)
                || Boolean.TRUE.equals(existing.sensitive()) != Boolean.TRUE.equals(sensitive)) {
            throw validationFailed("fieldTypeCode, scopeCode, and sensitive cannot be changed after progress exists.");
        }
    }

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

    private void validateAnnouncementExists(UUID announcementId) {
        if (dynamicAnnouncementInputDao.selectAnnouncementCount(announcementId) == 0) {
            throw notFound("Announcement was not found.");
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

    private boolean hasAnyOption(ApplicationInputValuesSaveRequest.InputValueRequest value) {
        return normalizeOptionalCode(value.optionCode()) != null || !nullToEmpty(value.optionCodes()).isEmpty();
    }

    private void validateOptionCode(String optionCode, Set<String> validOptionCodes) {
        if (!validOptionCodes.contains(optionCode)) {
            throw validationFailed("optionCode does not belong to the requirement.");
        }
    }

    private String normalizeRequiredCode(String fieldName, String value, Set<String> allowedValues) {
        String normalized = normalizeOptionalCode(value);
        if (normalized == null || !allowedValues.contains(normalized)) {
            throw validationFailed(fieldName + " is invalid.");
        }
        return normalized;
    }

    private String normalizeOptionalCode(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed.toUpperCase(Locale.ROOT);
    }

    private String normalizeCode(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw validationFailed("required text value is blank.");
        }
        return normalized.toUpperCase(Locale.ROOT);
    }

    private String normalizeRequiredText(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw validationFailed("required text value is blank.");
        }
        return normalized;
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

    private ApiException progressConditionNotMet(String message) {
        return new ApiException(ErrorCode.PROGRESS_CONDITION_NOT_MET, HttpStatus.CONFLICT, message);
    }

    private ApiException notFound(String message) {
        return new ApiException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, message);
    }
}
