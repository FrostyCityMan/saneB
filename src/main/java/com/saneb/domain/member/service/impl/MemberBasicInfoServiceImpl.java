package com.saneb.domain.member.service.impl;

import com.saneb.common.error.ApiException;
import com.saneb.common.error.ErrorCode;
import com.saneb.domain.auth.vo.AuthenticatedUserDetails;
import com.saneb.domain.member.dao.MemberBasicInfoDao;
import com.saneb.domain.member.dto.MemberBasicInfoResponse;
import com.saneb.domain.member.dto.MemberBasicInfoResponse.DocumentFieldInputResponse;
import com.saneb.domain.member.dto.MemberBasicInfoResponse.DocumentInputResponse;
import com.saneb.domain.member.dto.MemberBasicInfoSaveRequest;
import com.saneb.domain.member.dto.MemberBasicInfoSaveRequest.DocumentFieldValueRequest;
import com.saneb.domain.member.dto.MemberBasicInfoSaveRequest.DocumentInputSaveRequest;
import com.saneb.domain.member.service.MemberBasicInfoService;
import com.saneb.domain.member.vo.BusinessProfileCommand;
import com.saneb.domain.member.vo.BusinessProfileRow;
import com.saneb.domain.member.vo.FamilyMemberCommand;
import com.saneb.domain.member.vo.FamilyMemberRow;
import com.saneb.domain.member.vo.MemberDocumentFieldRow;
import com.saneb.domain.member.vo.MemberDocumentInputValueCommand;
import com.saneb.domain.member.vo.MemberDocumentInputValueRow;
import com.saneb.domain.member.vo.MemberProfileCommand;
import com.saneb.domain.member.vo.MemberProfileRow;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberBasicInfoServiceImpl implements MemberBasicInfoService {

    private static final Set<String> RELATION_TYPE_CODES = Set.of("SPOUSE", "CHILD", "PARENT");
    private static final Set<String> INCOME_PRESENCE_CODES = Set.of("UNKNOWN", "NONE", "HAS_INCOME");
    private static final Set<String> BUSINESS_TYPE_CODES = Set.of(
            "SOLE_PROPRIETOR",
            "CORPORATION",
            "SIMPLIFIED_TAXPAYER",
            "GENERAL_TAXPAYER",
            "TAX_EXEMPT"
    );
    private static final Set<String> COMPANY_STAGE_CODES = Set.of(
            "PRE_STARTUP",
            "EARLY_STARTUP",
            "OPERATING",
            "SUSPENDED",
            "CLOSURE_PLANNED",
            "CLOSED",
            "RESTART_PREPARING"
    );
    private static final Set<String> DOCUMENT_TEXT_FIELD_TYPES = Set.of("TEXT", "TEXTAREA", "SELECT", "RADIO", "MULTI_SELECT");
    private static final Map<String, String> DOCUMENT_TYPE_LABELS = Map.ofEntries(
            Map.entry("BUSINESS_REGISTRATION", "사업자등록증"),
            Map.entry("VAT_TAX_BASE", "부가세과세표준증명원"),
            Map.entry("TAX_EXEMPT_INCOME", "수입금액증명원(면세사업자)"),
            Map.entry("INCOME_CERTIFICATE", "소득금액증명원"),
            Map.entry("NATIONAL_TAX_PAID", "국세완납증명서"),
            Map.entry("LOCAL_TAX_PAID", "지방세완납증명서"),
            Map.entry("RESIDENT_REGISTRATION", "주민등록등본"),
            Map.entry("FAMILY_RELATION", "가족관계증명서"),
            Map.entry("HEALTH_INSURANCE_PAYMENT", "건강보험료 납부확인서"),
            Map.entry("HEALTH_INSURANCE_QUALIFICATION", "건강보험 자격확인서")
    );

    private final MemberBasicInfoDao memberBasicInfoDao;

    public MemberBasicInfoServiceImpl(MemberBasicInfoDao memberBasicInfoDao) {
        this.memberBasicInfoDao = memberBasicInfoDao;
    }

    @Override
    public MemberBasicInfoResponse selectMyBasicInfo(Authentication authentication) {
        UUID userId = selectCurrentUserId(authentication);
        return selectBasicInfoResponse(userId);
    }

    @Override
    @Transactional
    public MemberBasicInfoResponse saveMyBasicInfo(Authentication authentication, MemberBasicInfoSaveRequest request) {
        UUID userId = selectCurrentUserId(authentication);
        validateBirthYear(request.birthYear(), "출생연도");
        String incomePresenceCode = normalizeCode(request.incomePresenceCode());
        validateOptionalCode(incomePresenceCode, INCOME_PRESENCE_CODES, "소득 여부");

        memberBasicInfoDao.saveMemberProfile(new MemberProfileCommand(
                userId,
                request.birthYear(),
                normalizeCode(request.regionCode()),
                normalizeIncomeFlag(request.hasIncome(), incomePresenceCode),
                incomePresenceCode,
                request.incomeAmount(),
                normalizeCode(request.healthInsuranceBasisCode()),
                userId
        ));

        BusinessProfileCommand businessCommand = selectBusinessProfileCommand(userId, request.business());
        if (businessCommand != null) {
            UUID businessProfileId = memberBasicInfoDao.selectBusinessProfileIdByUserId(userId);
            if (businessProfileId == null) {
                memberBasicInfoDao.insertBusinessProfile(businessCommand);
            } else {
                memberBasicInfoDao.updateBusinessProfile(new BusinessProfileCommand(
                        businessProfileId,
                        businessCommand.userId(),
                        businessCommand.businessRegistrationNo(),
                        businessCommand.businessName(),
                        businessCommand.workplaceRegionCode(),
                        businessCommand.openingDate(),
                        businessCommand.ksicCode(),
                        businessCommand.businessTypeCode(),
                        businessCommand.companyStageCode(),
                        businessCommand.annualRevenue(),
                        businessCommand.annualRevenueYear(),
                        businessCommand.hasPolicyFundUsage(),
                        businessCommand.hasGuaranteeUsage(),
                        businessCommand.actorUserId()
                ));
            }
        }

        memberBasicInfoDao.deleteFamilyMemberList(userId);
        for (MemberBasicInfoSaveRequest.FamilyInfoRequest family : safeFamilies(request.families())) {
            FamilyMemberCommand command = selectFamilyMemberCommand(userId, family);
            memberBasicInfoDao.insertFamilyMember(command);
        }

        if (request.documentInputs() != null) {
            saveDocumentInputValues(userId, request.documentInputs());
        }

        return selectBasicInfoResponse(userId);
    }

    private MemberBasicInfoResponse selectBasicInfoResponse(UUID userId) {
        MemberProfileRow member = memberBasicInfoDao.selectMemberProfileDetails(userId);
        BusinessProfileRow business = memberBasicInfoDao.selectBusinessProfileDetails(userId);
        List<FamilyMemberRow> families = memberBasicInfoDao.selectFamilyMemberList(userId);

        return new MemberBasicInfoResponse(
                userId,
                member == null ? null : member.birthYear(),
                member == null ? null : member.regionCode(),
                member == null ? null : member.hasIncome(),
                member == null ? null : member.incomePresenceCode(),
                member == null ? null : member.incomeAmount(),
                member == null ? null : member.healthInsuranceBasisCode(),
                selectBusinessResponse(business),
                families.stream()
                        .map(row -> new MemberBasicInfoResponse.FamilyInfoResponse(
                                row.familyMemberId(),
                                row.relationTypeCode(),
                                row.birthYear(),
                                row.hasIncome(),
                                row.incomePresenceCode(),
                                row.incomeAmount()
                        ))
                        .toList(),
                selectDocumentInputResponses(userId)
        );
    }

    private MemberBasicInfoResponse.BusinessInfoResponse selectBusinessResponse(BusinessProfileRow row) {
        if (row == null) {
            return null;
        }
        return new MemberBasicInfoResponse.BusinessInfoResponse(
                row.businessRegistrationNo(),
                row.businessName(),
                row.workplaceRegionCode(),
                row.openingDate(),
                row.ksicCode(),
                row.businessTypeCode(),
                row.companyStageCode(),
                row.annualRevenue(),
                row.annualRevenueYear(),
                row.hasPolicyFundUsage(),
                row.hasGuaranteeUsage()
        );
    }

    private List<DocumentInputResponse> selectDocumentInputResponses(UUID userId) {
        List<MemberDocumentFieldRow> fields = memberBasicInfoDao.selectMemberDocumentFieldList();
        Map<UUID, MemberDocumentInputValueRow> valueByFieldId = new HashMap<>();
        for (MemberDocumentInputValueRow value : memberBasicInfoDao.selectMemberDocumentInputValueList(userId)) {
            valueByFieldId.put(value.standardFieldId(), value);
        }

        Map<String, List<MemberDocumentFieldRow>> fieldsByDocument = new LinkedHashMap<>();
        for (MemberDocumentFieldRow field : fields) {
            fieldsByDocument.computeIfAbsent(field.documentTypeCode(), ignored -> new ArrayList<>()).add(field);
        }

        List<DocumentInputResponse> responses = new ArrayList<>();
        for (Map.Entry<String, List<MemberDocumentFieldRow>> entry : fieldsByDocument.entrySet()) {
            List<DocumentFieldInputResponse> fieldResponses = entry.getValue().stream()
                    .map(field -> selectDocumentFieldResponse(field, valueByFieldId.get(field.standardFieldId())))
                    .toList();
            boolean selected = fieldResponses.stream().anyMatch(this::hasDocumentFieldResponseValue);
            responses.add(new DocumentInputResponse(
                    entry.getKey(),
                    DOCUMENT_TYPE_LABELS.getOrDefault(entry.getKey(), entry.getKey()),
                    selected,
                    fieldResponses
            ));
        }
        return responses;
    }

    private DocumentFieldInputResponse selectDocumentFieldResponse(
            MemberDocumentFieldRow field,
            MemberDocumentInputValueRow value
    ) {
        return new DocumentFieldInputResponse(
                field.standardFieldId(),
                field.fieldKey(),
                field.fieldLabel(),
                field.fieldTypeCode(),
                field.scopeCode(),
                Boolean.TRUE.equals(field.requiredDefault()),
                field.sortOrder() == null ? 0 : field.sortOrder(),
                field.helpText(),
                value == null ? null : value.valueText(),
                value == null ? null : value.valueNumber(),
                value == null ? null : value.valueDate(),
                value == null ? null : value.valueBoolean()
        );
    }

    private boolean hasDocumentFieldResponseValue(DocumentFieldInputResponse field) {
        return trimToNull(field.valueText()) != null
                || field.valueNumber() != null
                || field.valueDate() != null
                || field.valueBoolean() != null;
    }

    private void saveDocumentInputValues(UUID userId, List<DocumentInputSaveRequest> documents) {
        List<MemberDocumentFieldRow> fields = memberBasicInfoDao.selectMemberDocumentFieldList();
        Map<UUID, MemberDocumentFieldRow> fieldById = new HashMap<>();
        Map<String, Set<UUID>> fieldIdsByDocument = new HashMap<>();
        for (MemberDocumentFieldRow field : fields) {
            fieldById.put(field.standardFieldId(), field);
            fieldIdsByDocument.computeIfAbsent(field.documentTypeCode(), ignored -> new HashSet<>()).add(field.standardFieldId());
        }

        memberBasicInfoDao.deleteMemberDocumentInputValueList(userId);
        Set<UUID> savedFieldIds = new HashSet<>();
        for (DocumentInputSaveRequest document : safeDocumentInputs(documents)) {
            if (document == null) {
                continue;
            }
            String documentTypeCode = normalizeCode(document.documentTypeCode());
            if (documentTypeCode == null) {
                if (document.fields() != null && document.fields().stream().anyMatch(this::hasDocumentFieldValue)) {
                    throw validationFailed("서류 구분을 선택하세요.");
                }
                continue;
            }
            if (!fieldIdsByDocument.containsKey(documentTypeCode)) {
                throw validationFailed("서류 구분 값이 올바르지 않습니다.");
            }
            for (DocumentFieldValueRequest value : safeDocumentFields(document.fields())) {
                if (value == null) {
                    continue;
                }
                if (value.standardFieldId() == null) {
                    if (hasDocumentFieldValue(value)) {
                        throw validationFailed("서류 입력 항목을 확인하세요.");
                    }
                    continue;
                }
                MemberDocumentFieldRow field = fieldById.get(value.standardFieldId());
                if (field == null || !fieldIdsByDocument.get(documentTypeCode).contains(value.standardFieldId())) {
                    throw validationFailed("서류 입력 항목이 선택한 서류와 일치하지 않습니다.");
                }
                if (!hasDocumentFieldValue(value)) {
                    continue;
                }
                if (!savedFieldIds.add(value.standardFieldId())) {
                    throw validationFailed("같은 서류 항목은 한 번만 입력하세요.");
                }
                memberBasicInfoDao.insertMemberDocumentInputValue(selectDocumentValueCommand(userId, field, value));
            }
        }
    }

    private MemberDocumentInputValueCommand selectDocumentValueCommand(
            UUID userId,
            MemberDocumentFieldRow field,
            DocumentFieldValueRequest value
    ) {
        validateSingleDocumentValue(field, value);
        String fieldTypeCode = normalizeCode(field.fieldTypeCode());
        if (DOCUMENT_TEXT_FIELD_TYPES.contains(fieldTypeCode)) {
            String valueText = normalizeDocumentTextValue(value.valueText());
            if (valueText == null) {
                throw validationFailed(field.fieldLabel() + "은 문자 값으로 입력하세요.");
            }
            return new MemberDocumentInputValueCommand(userId, field.standardFieldId(), valueText, null, null, null, userId);
        }
        if ("NUMBER".equals(fieldTypeCode) || "AMOUNT".equals(fieldTypeCode)) {
            if (value.valueNumber() == null) {
                throw validationFailed(field.fieldLabel() + "은 숫자 또는 금액으로 입력하세요.");
            }
            return new MemberDocumentInputValueCommand(userId, field.standardFieldId(), null, value.valueNumber(), null, null, userId);
        }
        if ("DATE".equals(fieldTypeCode)) {
            if (value.valueDate() == null) {
                throw validationFailed(field.fieldLabel() + "은 날짜로 입력하세요.");
            }
            return new MemberDocumentInputValueCommand(userId, field.standardFieldId(), null, null, value.valueDate(), null, userId);
        }
        if ("BOOLEAN".equals(fieldTypeCode)) {
            if (value.valueBoolean() == null) {
                throw validationFailed(field.fieldLabel() + "은 예 또는 아니오로 선택하세요.");
            }
            return new MemberDocumentInputValueCommand(userId, field.standardFieldId(), null, null, null, value.valueBoolean(), userId);
        }
        throw validationFailed(field.fieldLabel() + "의 입력 유형을 확인하세요.");
    }

    private void validateSingleDocumentValue(MemberDocumentFieldRow field, DocumentFieldValueRequest value) {
        int valueCount = 0;
        if (normalizeDocumentTextValue(value.valueText()) != null) {
            valueCount++;
        }
        if (value.valueNumber() != null) {
            valueCount++;
        }
        if (value.valueDate() != null) {
            valueCount++;
        }
        if (value.valueBoolean() != null) {
            valueCount++;
        }
        if (valueCount > 1) {
            throw validationFailed(field.fieldLabel() + "은 한 가지 값만 입력하세요.");
        }
    }

    private boolean hasDocumentFieldValue(DocumentFieldValueRequest value) {
        return value != null
                && (normalizeDocumentTextValue(value.valueText()) != null
                || value.valueNumber() != null
                || value.valueDate() != null
                || value.valueBoolean() != null);
    }

    private String normalizeDocumentTextValue(String value) {
        return trimToNull(value);
    }

    private List<DocumentInputSaveRequest> safeDocumentInputs(List<DocumentInputSaveRequest> documents) {
        return documents == null ? List.of() : documents;
    }

    private List<DocumentFieldValueRequest> safeDocumentFields(List<DocumentFieldValueRequest> fields) {
        return fields == null ? List.of() : fields;
    }

    private BusinessProfileCommand selectBusinessProfileCommand(
            UUID userId,
            MemberBasicInfoSaveRequest.BusinessInfoRequest business
    ) {
        if (business == null || isEmptyBusiness(business)) {
            return null;
        }
        String businessRegistrationNo = trimToNull(business.businessRegistrationNo());
        String businessName = trimToNull(business.businessName());
        if (businessRegistrationNo == null || businessName == null) {
            throw validationFailed("사업자 정보를 입력할 때는 사업자등록번호와 상호명을 함께 입력하세요.");
        }
        validateBusinessDate(business.openingDate());
        validateYear(business.annualRevenueYear(), "연매출 기준연도");
        String businessTypeCode = normalizeCode(business.businessTypeCode());
        String companyStageCode = normalizeCode(business.companyStageCode());
        validateOptionalCode(businessTypeCode, BUSINESS_TYPE_CODES, "사업자 유형");
        validateOptionalCode(companyStageCode, COMPANY_STAGE_CODES, "사업 상태");

        return new BusinessProfileCommand(
                null,
                userId,
                businessRegistrationNo,
                businessName,
                normalizeCode(business.workplaceRegionCode()),
                business.openingDate(),
                normalizeCode(business.ksicCode()),
                businessTypeCode,
                companyStageCode,
                business.annualRevenue(),
                business.annualRevenueYear(),
                business.hasPolicyFundUsage(),
                business.hasGuaranteeUsage(),
                userId
        );
    }

    private FamilyMemberCommand selectFamilyMemberCommand(
            UUID userId,
            MemberBasicInfoSaveRequest.FamilyInfoRequest family
    ) {
        String relationTypeCode = normalizeCode(family.relationTypeCode());
        validateRequiredCode(relationTypeCode, RELATION_TYPE_CODES, "가족 관계");
        validateBirthYear(family.birthYear(), "가족 출생연도");
        String incomePresenceCode = normalizeCode(family.incomePresenceCode());
        validateOptionalCode(incomePresenceCode, INCOME_PRESENCE_CODES, "가족 소득 여부");
        return new FamilyMemberCommand(
                UUID.randomUUID(),
                userId,
                relationTypeCode,
                family.birthYear(),
                normalizeIncomeFlag(family.hasIncome(), incomePresenceCode),
                incomePresenceCode,
                family.incomeAmount(),
                userId
        );
    }

    private List<MemberBasicInfoSaveRequest.FamilyInfoRequest> safeFamilies(
            List<MemberBasicInfoSaveRequest.FamilyInfoRequest> families
    ) {
        return families == null ? List.of() : families;
    }

    private boolean isEmptyBusiness(MemberBasicInfoSaveRequest.BusinessInfoRequest business) {
        return trimToNull(business.businessRegistrationNo()) == null
                && trimToNull(business.businessName()) == null
                && trimToNull(business.workplaceRegionCode()) == null
                && business.openingDate() == null
                && trimToNull(business.ksicCode()) == null
                && trimToNull(business.businessTypeCode()) == null
                && trimToNull(business.companyStageCode()) == null
                && business.annualRevenue() == null
                && business.annualRevenueYear() == null
                && business.hasPolicyFundUsage() == null
                && business.hasGuaranteeUsage() == null;
    }

    private Boolean normalizeIncomeFlag(Boolean explicitValue, String incomePresenceCode) {
        if ("HAS_INCOME".equals(incomePresenceCode)) {
            return true;
        }
        if ("NONE".equals(incomePresenceCode)) {
            return false;
        }
        return explicitValue;
    }

    private void validateBirthYear(Integer birthYear, String label) {
        validateYear(birthYear, label);
    }

    private void validateYear(Integer year, String label) {
        if (year != null && (year < 1900 || year > 2200)) {
            throw validationFailed(label + "는 1900년부터 2200년 사이로 입력하세요.");
        }
    }

    private void validateBusinessDate(LocalDate openingDate) {
        if (openingDate != null && openingDate.isAfter(LocalDate.now())) {
            throw validationFailed("개업일은 오늘 이후 날짜로 입력할 수 없습니다.");
        }
    }

    private void validateRequiredCode(String value, Set<String> allowedValues, String label) {
        if (value == null || !allowedValues.contains(value)) {
            throw validationFailed(label + " 값이 올바르지 않습니다.");
        }
    }

    private void validateOptionalCode(String value, Set<String> allowedValues, String label) {
        if (value != null && !allowedValues.contains(value)) {
            throw validationFailed(label + " 값이 올바르지 않습니다.");
        }
    }

    private UUID selectCurrentUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ApiException(ErrorCode.AUTH_REQUIRED, HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
        }
        if (authentication.getPrincipal() instanceof AuthenticatedUserDetails principal) {
            return principal.userId();
        }
        UUID userId = memberBasicInfoDao.selectUserIdByLoginId(authentication.getName());
        if (userId == null) {
            throw new ApiException(ErrorCode.AUTH_REQUIRED, HttpStatus.UNAUTHORIZED, "인증 정보를 확인할 수 없습니다.");
        }
        return userId;
    }

    private String normalizeCode(String value) {
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

    private ApiException validationFailed(String message) {
        return new ApiException(ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST, message);
    }
}
