/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: MemberBasicInfoServiceImpl.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.member.service.impl;

import com.saneb.common.error.ApiException;
import com.saneb.common.error.ErrorCode;
import com.saneb.domain.auth.vo.AuthenticatedUserDetails;
import com.saneb.domain.member.dao.MemberBasicInfoDao;
import com.saneb.domain.member.dto.MemberBasicInfoResponse;
import com.saneb.domain.member.dto.MemberBasicInfoResponse.DocumentFieldInputResponse;
import com.saneb.domain.member.dto.MemberBasicInfoResponse.DocumentInputResponse;
import com.saneb.domain.member.dto.MemberBasicInfoResponse.InterviewResponse;
import com.saneb.domain.member.dto.MemberBasicInfoSaveRequest;
import com.saneb.domain.member.dto.MemberBasicInfoSaveRequest.DocumentFieldValueRequest;
import com.saneb.domain.member.dto.MemberBasicInfoSaveRequest.DocumentInputSaveRequest;
import com.saneb.domain.member.dto.MemberBasicInfoSaveRequest.InterviewResponseRequest;
import com.saneb.domain.member.service.MemberBasicInfoService;
import com.saneb.domain.member.vo.BusinessProfileCommand;
import com.saneb.domain.member.vo.BusinessProfileRow;
import com.saneb.domain.member.vo.FamilyMemberCommand;
import com.saneb.domain.member.vo.FamilyMemberRow;
import com.saneb.domain.member.vo.MemberDocumentFieldRow;
import com.saneb.domain.member.vo.MemberDocumentInputValueCommand;
import com.saneb.domain.member.vo.MemberDocumentInputValueRow;
import com.saneb.domain.member.vo.MemberInterviewResponseCommand;
import com.saneb.domain.member.vo.MemberInterviewResponseRow;
import com.saneb.domain.member.vo.MemberProfileCommand;
import com.saneb.domain.member.vo.MemberProfileRow;
import com.saneb.domain.matching.service.MatchingService;
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
    private static final Set<String> SCHOOL_AGE_STATUS_CODES = Set.of(
            "PRESCHOOL",
            "ELEMENTARY",
            "MIDDLE_HIGH",
            "COLLEGE",
            "NONE"
    );
    private static final Set<String> ENROLLMENT_STATUS_CODES = Set.of(
            "ENROLLED",
            "NOT_ENROLLED",
            "UNKNOWN"
    );
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
    private static final Set<String> ADDRESS_SOURCE_CODES = Set.of("JUSO_API", "MANUAL");
    private static final Set<String> INTERVIEW_QUESTION_CODES = Set.of(
            "SAME_BUSINESS_IN_PROGRESS",
            "DUPLICATE_SUPPORT_USAGE",
            "BUSINESS_ACTUALLY_OPERATING",
            "OTHER_RESTRICTION"
    );
    private static final Set<String> INTERVIEW_ANSWER_CODES = Set.of("YES", "NO", "UNKNOWN");
    private static final Map<String, String> INTERVIEW_QUESTION_LABELS = Map.of(
            "SAME_BUSINESS_IN_PROGRESS", "기존 동일 사업 진행 여부",
            "DUPLICATE_SUPPORT_USAGE", "중복 지원 여부",
            "BUSINESS_ACTUALLY_OPERATING", "실제 사업 운영 여부",
            "OTHER_RESTRICTION", "기타 제한 여부"
    );
    private static final Map<String, String> INTERVIEW_ANSWER_LABELS = Map.of(
            "YES", "예",
            "NO", "아니오",
            "UNKNOWN", "잘 모르겠음"
    );
    private static final Set<String> DOCUMENT_TEXT_FIELD_TYPES = Set.of("TEXT", "TEXTAREA", "SELECT", "RADIO", "MULTI_SELECT");
    private static final Map<String, String> DOCUMENT_TYPE_LABELS = Map.ofEntries(
            /**
             * 업무 처리를 수행합니다.
             *
             * @param BUSINESS_REGISTRATION 입력 값
             *
             * @return 처리 결과
             */
            Map.entry("BUSINESS_REGISTRATION", "사업자등록증"),
            /**
             * 업무 처리를 수행합니다.
             *
             * @param VAT_TAX_BASE 입력 값
             *
             * @return 처리 결과
             */
            Map.entry("VAT_TAX_BASE", "부가세과세표준증명원"),
            /**
             * 업무 처리를 수행합니다.
             *
             * @param TAX_EXEMPT_INCOME 입력 값
             *
             * @return 처리 결과
             */
            Map.entry("TAX_EXEMPT_INCOME", "수입금액증명원(면세사업자)"),
            /**
             * 업무 처리를 수행합니다.
             *
             * @param INCOME_CERTIFICATE 입력 값
             *
             * @return 처리 결과
             */
            Map.entry("INCOME_CERTIFICATE", "소득금액증명원"),
            /**
             * 업무 처리를 수행합니다.
             *
             * @param NATIONAL_TAX_PAID 입력 값
             *
             * @return 처리 결과
             */
            Map.entry("NATIONAL_TAX_PAID", "국세완납증명서"),
            /**
             * 업무 처리를 수행합니다.
             *
             * @param LOCAL_TAX_PAID 입력 값
             *
             * @return 처리 결과
             */
            Map.entry("LOCAL_TAX_PAID", "지방세완납증명서"),
            /**
             * 업무 처리를 수행합니다.
             *
             * @param RESIDENT_REGISTRATION 입력 값
             *
             * @return 처리 결과
             */
            Map.entry("RESIDENT_REGISTRATION", "주민등록등본"),
            /**
             * 업무 처리를 수행합니다.
             *
             * @param FAMILY_RELATION 입력 값
             *
             * @return 처리 결과
             */
            Map.entry("FAMILY_RELATION", "가족관계증명서"),
            /**
             * 업무 처리를 수행합니다.
             *
             * @param HEALTH_INSURANCE_PAYMENT 입력 값
             *
             * @return 처리 결과
             */
            Map.entry("HEALTH_INSURANCE_PAYMENT", "건강보험료 납부확인서"),
            /**
             * 업무 처리를 수행합니다.
             *
             * @param HEALTH_INSURANCE_QUALIFICATION 입력 값
             *
             * @return 처리 결과
             */
            Map.entry("HEALTH_INSURANCE_QUALIFICATION", "건강보험 자격확인서")
    );

    private final MemberBasicInfoDao memberBasicInfoDao;
    private final MatchingService matchingService;

    /**
     * 객체를 생성합니다.
     *
     * @param memberBasicInfoDao 입력 값
     *
     * @param matchingService 입력 값
     */
    public MemberBasicInfoServiceImpl(MemberBasicInfoDao memberBasicInfoDao, MatchingService matchingService) {
        this.memberBasicInfoDao = memberBasicInfoDao;
        this.matchingService = matchingService;
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param authentication 입력 값
     *
     * @return 처리 결과
     */
    @Override
    public MemberBasicInfoResponse selectMyBasicInfo(Authentication authentication) {
        UUID userId = selectCurrentUserId(authentication);
        return selectBasicInfoResponse(userId);
    }

    /**
     * 업무 데이터를 저장합니다.
     *
     * @param authentication 입력 값
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
     * @param request 입력 값
     *
     * @return 처리 결과
     */
    @Transactional
    public MemberBasicInfoResponse saveMyBasicInfo(Authentication authentication, MemberBasicInfoSaveRequest request) {
        UUID userId = selectCurrentUserId(authentication);
        return saveBasicInfo(userId, userId, request);
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param authentication 입력 값
     *
     * @param userId 입력 값
     *
     * @return 처리 결과
     */
    @Override
    public MemberBasicInfoResponse selectMemberBasicInfo(Authentication authentication, UUID userId) {
        selectCurrentUserId(authentication);
        validateUserExists(userId);
        return selectBasicInfoResponse(userId);
    }

    /**
     * 업무 데이터를 저장합니다.
     *
     * @param authentication 입력 값
     *
     * @param userId 입력 값
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
     * @param userId 입력 값
     *
     * @param request 입력 값
     *
     * @return 처리 결과
     */
    @Transactional
    public MemberBasicInfoResponse saveMemberBasicInfo(
            Authentication authentication,
            UUID userId,
            MemberBasicInfoSaveRequest request
    ) {
        UUID actorUserId = selectCurrentUserId(authentication);
        validateUserExists(userId);
        return saveBasicInfo(userId, actorUserId, request);
    }

    /**
     * 업무 데이터를 저장합니다.
     *
     * @param userId 입력 값
     *
     * @param actorUserId 입력 값
     *
     * @param request 입력 값
     *
     * @return 처리 결과
     */
    private MemberBasicInfoResponse saveBasicInfo(UUID userId, UUID actorUserId, MemberBasicInfoSaveRequest request) {
        validateBirthYear(request.birthYear(), "출생연도");
        String incomePresenceCode = normalizeCode(request.incomePresenceCode());
        validateOptionalCode(incomePresenceCode, INCOME_PRESENCE_CODES, "소득 여부");
        String addressSourceCode = normalizeCode(request.addressSourceCode());
        validateOptionalCode(addressSourceCode, ADDRESS_SOURCE_CODES, "주소 입력 출처");

        memberBasicInfoDao.saveMemberProfile(new MemberProfileCommand(
                userId,
                request.birthYear(),
                normalizeCode(request.regionCode()),
                trimToNull(request.postalCode()),
                trimToNull(request.roadAddress()),
                trimToNull(request.jibunAddress()),
                trimToNull(request.detailAddress()),
                trimToNull(request.sidoName()),
                trimToNull(request.sigunguName()),
                trimToNull(request.eupmyeondongName()),
                normalizeCode(request.legalDongCode()),
                trimToNull(request.roadNameCode()),
                trimToNull(request.buildingManagementNo()),
                addressSourceCode,
                normalizeIncomeFlag(request.hasIncome(), incomePresenceCode),
                incomePresenceCode,
                request.incomeAmount(),
                normalizeCode(request.healthInsuranceBasisCode()),
                actorUserId
        ));

        BusinessProfileCommand businessCommand = selectBusinessProfileCommand(userId, actorUserId, request.business());
        if (businessCommand != null) {
            UUID businessProfileId = memberBasicInfoDao.selectBusinessProfileIdByUserId(userId);
            if (businessProfileId == null) {
                memberBasicInfoDao.insertBusinessProfile(businessCommand);
            } else {
                memberBasicInfoDao.updateBusinessProfile(new BusinessProfileCommand(
                        businessProfileId,
                        businessCommand.userId(),
                        businessCommand.representativeName(),
                        businessCommand.businessRegistrationNo(),
                        businessCommand.businessName(),
                        businessCommand.workplaceRegionCode(),
                        businessCommand.workplacePostalCode(),
                        businessCommand.workplaceRoadAddress(),
                        businessCommand.workplaceJibunAddress(),
                        businessCommand.workplaceDetailAddress(),
                        businessCommand.workplaceSidoName(),
                        businessCommand.workplaceSigunguName(),
                        businessCommand.workplaceEupmyeondongName(),
                        businessCommand.workplaceLegalDongCode(),
                        businessCommand.workplaceRoadNameCode(),
                        businessCommand.workplaceBuildingManagementNo(),
                        businessCommand.workplaceAddressSourceCode(),
                        businessCommand.openingDate(),
                        businessCommand.ksicCode(),
                        businessCommand.businessTypeCode(),
                        businessCommand.companyStageCode(),
                        businessCommand.annualRevenue(),
                        businessCommand.annualRevenueYear(),
                        businessCommand.employeeCount(),
                        businessCommand.regularEmployeeCount(),
                        businessCommand.plannedHireCount(),
                        businessCommand.niceCreditScore(),
                        businessCommand.kcbCreditScore(),
                        businessCommand.hasExistingLoan(),
                        businessCommand.hasPolicyFundUsage(),
                        businessCommand.hasGuaranteeUsage(),
                        businessCommand.actorUserId()
                ));
            }
        }

        memberBasicInfoDao.deleteFamilyMemberList(userId);
        for (MemberBasicInfoSaveRequest.FamilyInfoRequest family : safeFamilies(request.families())) {
            FamilyMemberCommand command = selectFamilyMemberCommand(userId, actorUserId, family);
            memberBasicInfoDao.insertFamilyMember(command);
        }

        if (request.interviewResponses() != null) {
            saveInterviewResponses(userId, actorUserId, request.interviewResponses());
        }

        if (request.documentInputs() != null) {
            saveDocumentInputValues(userId, actorUserId, request.documentInputs());
        }

        matchingService.insertBasicMatchingCandidates(actorUserId, userId);
        return selectBasicInfoResponse(userId);
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param userId 입력 값
     *
     * @return 처리 결과
     */
    private MemberBasicInfoResponse selectBasicInfoResponse(UUID userId) {
        MemberProfileRow member = memberBasicInfoDao.selectMemberProfileDetails(userId);
        BusinessProfileRow business = memberBasicInfoDao.selectBusinessProfileDetails(userId);
        List<FamilyMemberRow> families = memberBasicInfoDao.selectFamilyMemberList(userId);

        return new MemberBasicInfoResponse(
                userId,
                member == null ? null : member.birthYear(),
                member == null ? null : member.regionCode(),
                member == null ? null : member.postalCode(),
                member == null ? null : member.roadAddress(),
                member == null ? null : member.jibunAddress(),
                member == null ? null : member.detailAddress(),
                member == null ? null : member.sidoName(),
                member == null ? null : member.sigunguName(),
                member == null ? null : member.eupmyeondongName(),
                member == null ? null : member.legalDongCode(),
                member == null ? null : member.roadNameCode(),
                member == null ? null : member.buildingManagementNo(),
                member == null ? null : member.addressSourceCode(),
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
                                row.schoolAgeStatusCode(),
                                row.enrollmentStatusCode(),
                                row.cohabiting(),
                                row.supported(),
                                row.hasIncome(),
                                row.incomePresenceCode(),
                                row.incomeAmount()
                        ))
                        .toList(),
                selectInterviewResponses(userId),
                selectDocumentInputResponses(userId)
        );
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param row 입력 값
     *
     * @return 처리 결과
     */
    private MemberBasicInfoResponse.BusinessInfoResponse selectBusinessResponse(BusinessProfileRow row) {
        if (row == null) {
            return null;
        }
        return new MemberBasicInfoResponse.BusinessInfoResponse(
                row.representativeName(),
                row.businessRegistrationNo(),
                row.businessName(),
                row.workplaceRegionCode(),
                row.workplacePostalCode(),
                row.workplaceRoadAddress(),
                row.workplaceJibunAddress(),
                row.workplaceDetailAddress(),
                row.workplaceSidoName(),
                row.workplaceSigunguName(),
                row.workplaceEupmyeondongName(),
                row.workplaceLegalDongCode(),
                row.workplaceRoadNameCode(),
                row.workplaceBuildingManagementNo(),
                row.workplaceAddressSourceCode(),
                row.openingDate(),
                row.ksicCode(),
                row.businessTypeCode(),
                row.companyStageCode(),
                row.annualRevenue(),
                row.annualRevenueYear(),
                row.employeeCount(),
                row.regularEmployeeCount(),
                row.plannedHireCount(),
                row.niceCreditScore(),
                row.kcbCreditScore(),
                row.hasExistingLoan(),
                row.hasPolicyFundUsage(),
                row.hasGuaranteeUsage()
        );
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param userId 입력 값
     *
     * @return 처리 결과
     */
    private List<InterviewResponse> selectInterviewResponses(UUID userId) {
        return memberBasicInfoDao.selectMemberInterviewResponseList(userId).stream()
                .map(row -> new InterviewResponse(
                        row.questionCode(),
                        INTERVIEW_QUESTION_LABELS.getOrDefault(row.questionCode(), row.questionCode()),
                        row.answerCode(),
                        INTERVIEW_ANSWER_LABELS.getOrDefault(row.answerCode(), row.answerCode()),
                        row.note()
                ))
                .toList();
    }

    /**
     * 업무 데이터를 저장합니다.
     *
     * @param userId 입력 값
     *
     * @param actorUserId 입력 값
     *
     * @param responses 입력 값
     */
    private void saveInterviewResponses(UUID userId, UUID actorUserId, List<InterviewResponseRequest> responses) {
        memberBasicInfoDao.deleteMemberInterviewResponseList(userId);
        Set<String> savedQuestionCodes = new HashSet<>();
        for (InterviewResponseRequest response : safeInterviewResponses(responses)) {
            if (response == null) {
                continue;
            }
            String questionCode = normalizeCode(response.questionCode());
            String answerCode = normalizeCode(response.answerCode());
            validateRequiredCode(questionCode, INTERVIEW_QUESTION_CODES, "확인 질문");
            validateRequiredCode(answerCode, INTERVIEW_ANSWER_CODES, "확인 질문 답변");
            if (!savedQuestionCodes.add(questionCode)) {
                throw validationFailed("같은 확인 질문은 한 번만 저장하세요.");
            }
            memberBasicInfoDao.insertMemberInterviewResponse(new MemberInterviewResponseCommand(
                    userId,
                    questionCode,
                    answerCode,
                    trimToNull(response.note()),
                    actorUserId
            ));
        }
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param userId 입력 값
     *
     * @return 처리 결과
     */
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

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param field 입력 값
     *
     * @param value 입력 값
     *
     * @return 처리 결과
     */
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

    /**
     * 조건 충족 여부를 확인합니다.
     *
     * @param field 입력 값
     *
     * @return 처리 결과
     */
    private boolean hasDocumentFieldResponseValue(DocumentFieldInputResponse field) {
        return trimToNull(field.valueText()) != null
                || field.valueNumber() != null
                || field.valueDate() != null
                || field.valueBoolean() != null;
    }

    /**
     * 업무 데이터를 저장합니다.
     *
     * @param userId 입력 값
     *
     * @param actorUserId 입력 값
     *
     * @param documents 입력 값
     */
    private void saveDocumentInputValues(UUID userId, UUID actorUserId, List<DocumentInputSaveRequest> documents) {
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
                memberBasicInfoDao.insertMemberDocumentInputValue(selectDocumentValueCommand(userId, actorUserId, field, value));
            }
        }
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param userId 입력 값
     *
     * @param actorUserId 입력 값
     *
     * @param field 입력 값
     *
     * @param value 입력 값
     *
     * @return 처리 결과
     */
    private MemberDocumentInputValueCommand selectDocumentValueCommand(
            UUID userId,
            UUID actorUserId,
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
            return new MemberDocumentInputValueCommand(userId, field.standardFieldId(), valueText, null, null, null, actorUserId);
        }
        if ("NUMBER".equals(fieldTypeCode) || "AMOUNT".equals(fieldTypeCode)) {
            if (value.valueNumber() == null) {
                throw validationFailed(field.fieldLabel() + "은 숫자 또는 금액으로 입력하세요.");
            }
            return new MemberDocumentInputValueCommand(userId, field.standardFieldId(), null, value.valueNumber(), null, null, actorUserId);
        }
        if ("DATE".equals(fieldTypeCode)) {
            if (value.valueDate() == null) {
                throw validationFailed(field.fieldLabel() + "은 날짜로 입력하세요.");
            }
            return new MemberDocumentInputValueCommand(userId, field.standardFieldId(), null, null, value.valueDate(), null, actorUserId);
        }
        if ("BOOLEAN".equals(fieldTypeCode)) {
            if (value.valueBoolean() == null) {
                throw validationFailed(field.fieldLabel() + "은 예 또는 아니오로 선택하세요.");
            }
            return new MemberDocumentInputValueCommand(userId, field.standardFieldId(), null, null, null, value.valueBoolean(), actorUserId);
        }
        throw validationFailed(field.fieldLabel() + "의 입력 유형을 확인하세요.");
    }

    /**
     * 요청 값과 업무 규칙을 검증합니다.
     *
     * @param field 입력 값
     *
     * @param value 입력 값
     */
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

    /**
     * 조건 충족 여부를 확인합니다.
     *
     * @param value 입력 값
     *
     * @return 처리 결과
     */
    private boolean hasDocumentFieldValue(DocumentFieldValueRequest value) {
        return value != null
                && (normalizeDocumentTextValue(value.valueText()) != null
                || value.valueNumber() != null
                || value.valueDate() != null
                || value.valueBoolean() != null);
    }

    /**
     * 입력 값을 표준 형식으로 정규화합니다.
     *
     * @param value 입력 값
     *
     * @return 처리 결과
     */
    private String normalizeDocumentTextValue(String value) {
        return trimToNull(value);
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param documents 입력 값
     *
     * @return 처리 결과
     */
    private List<DocumentInputSaveRequest> safeDocumentInputs(List<DocumentInputSaveRequest> documents) {
        return documents == null ? List.of() : documents;
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param fields 입력 값
     *
     * @return 처리 결과
     */
    private List<DocumentFieldValueRequest> safeDocumentFields(List<DocumentFieldValueRequest> fields) {
        return fields == null ? List.of() : fields;
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param userId 입력 값
     *
     * @param actorUserId 입력 값
     *
     * @param business 입력 값
     *
     * @return 처리 결과
     */
    private BusinessProfileCommand selectBusinessProfileCommand(
            UUID userId,
            UUID actorUserId,
            MemberBasicInfoSaveRequest.BusinessInfoRequest business
    ) {
        if (business == null || isEmptyBusiness(business)) {
            return null;
        }
        String businessRegistrationNo = trimToNull(business.businessRegistrationNo());
        String businessName = trimToNull(business.businessName());
        validateBusinessDate(business.openingDate());
        validateYear(business.annualRevenueYear(), "연매출 기준연도");
        validateNonNegative(business.employeeCount(), "직원 수");
        validateNonNegative(business.regularEmployeeCount(), "상시근로자 수");
        validateNonNegative(business.plannedHireCount(), "신규 채용 예정 인원");
        validateCreditScore(business.niceCreditScore(), "NICE 신용 점수");
        validateCreditScore(business.kcbCreditScore(), "KCB 신용 점수");
        String businessTypeCode = normalizeCode(business.businessTypeCode());
        String companyStageCode = normalizeCode(business.companyStageCode());
        String workplaceAddressSourceCode = normalizeCode(business.workplaceAddressSourceCode());
        validateOptionalCode(businessTypeCode, BUSINESS_TYPE_CODES, "사업자 유형");
        validateOptionalCode(companyStageCode, COMPANY_STAGE_CODES, "사업 상태");
        validateOptionalCode(workplaceAddressSourceCode, ADDRESS_SOURCE_CODES, "사업장 주소 입력 출처");

        return new BusinessProfileCommand(
                null,
                userId,
                trimToNull(business.representativeName()),
                businessRegistrationNo,
                businessName,
                normalizeCode(business.workplaceRegionCode()),
                trimToNull(business.workplacePostalCode()),
                trimToNull(business.workplaceRoadAddress()),
                trimToNull(business.workplaceJibunAddress()),
                trimToNull(business.workplaceDetailAddress()),
                trimToNull(business.workplaceSidoName()),
                trimToNull(business.workplaceSigunguName()),
                trimToNull(business.workplaceEupmyeondongName()),
                normalizeCode(business.workplaceLegalDongCode()),
                trimToNull(business.workplaceRoadNameCode()),
                trimToNull(business.workplaceBuildingManagementNo()),
                workplaceAddressSourceCode,
                business.openingDate(),
                normalizeCode(business.ksicCode()),
                businessTypeCode,
                companyStageCode,
                business.annualRevenue(),
                business.annualRevenueYear(),
                business.employeeCount(),
                business.regularEmployeeCount(),
                business.plannedHireCount(),
                business.niceCreditScore(),
                business.kcbCreditScore(),
                business.hasExistingLoan(),
                business.hasPolicyFundUsage(),
                business.hasGuaranteeUsage(),
                actorUserId
        );
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param userId 입력 값
     *
     * @param actorUserId 입력 값
     *
     * @param family 입력 값
     *
     * @return 처리 결과
     */
    private FamilyMemberCommand selectFamilyMemberCommand(
            UUID userId,
            UUID actorUserId,
            MemberBasicInfoSaveRequest.FamilyInfoRequest family
    ) {
        String relationTypeCode = normalizeCode(family.relationTypeCode());
        validateRequiredCode(relationTypeCode, RELATION_TYPE_CODES, "가족 관계");
        validateBirthYear(family.birthYear(), "가족 출생연도");
        String schoolAgeStatusCode = normalizeCode(family.schoolAgeStatusCode());
        validateOptionalCode(schoolAgeStatusCode, SCHOOL_AGE_STATUS_CODES, "자녀 학령 상태");
        String enrollmentStatusCode = normalizeCode(family.enrollmentStatusCode());
        validateOptionalCode(enrollmentStatusCode, ENROLLMENT_STATUS_CODES, "자녀 재학 상태");
        String incomePresenceCode = normalizeCode(family.incomePresenceCode());
        validateOptionalCode(incomePresenceCode, INCOME_PRESENCE_CODES, "가족 소득 여부");
        return new FamilyMemberCommand(
                UUID.randomUUID(),
                userId,
                relationTypeCode,
                family.birthYear(),
                schoolAgeStatusCode,
                enrollmentStatusCode,
                family.cohabiting(),
                family.supported(),
                normalizeIncomeFlag(family.hasIncome(), incomePresenceCode),
                incomePresenceCode,
                family.incomeAmount(),
                actorUserId
        );
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param families 입력 값
     *
     * @return 처리 결과
     */
    private List<MemberBasicInfoSaveRequest.FamilyInfoRequest> safeFamilies(
            List<MemberBasicInfoSaveRequest.FamilyInfoRequest> families
    ) {
        return families == null ? List.of() : families;
    }

    /**
     * 조건 충족 여부를 확인합니다.
     *
     * @param business 입력 값
     *
     * @return 처리 결과
     */
    private boolean isEmptyBusiness(MemberBasicInfoSaveRequest.BusinessInfoRequest business) {
        return trimToNull(business.businessRegistrationNo()) == null
                && trimToNull(business.representativeName()) == null
                && trimToNull(business.businessName()) == null
                && trimToNull(business.workplaceRegionCode()) == null
                && trimToNull(business.workplacePostalCode()) == null
                && trimToNull(business.workplaceRoadAddress()) == null
                && trimToNull(business.workplaceJibunAddress()) == null
                && trimToNull(business.workplaceDetailAddress()) == null
                && trimToNull(business.workplaceSidoName()) == null
                && trimToNull(business.workplaceSigunguName()) == null
                && trimToNull(business.workplaceEupmyeondongName()) == null
                && trimToNull(business.workplaceLegalDongCode()) == null
                && trimToNull(business.workplaceRoadNameCode()) == null
                && trimToNull(business.workplaceBuildingManagementNo()) == null
                && trimToNull(business.workplaceAddressSourceCode()) == null
                && business.openingDate() == null
                && trimToNull(business.ksicCode()) == null
                && trimToNull(business.businessTypeCode()) == null
                && trimToNull(business.companyStageCode()) == null
                && business.annualRevenue() == null
                && business.annualRevenueYear() == null
                && business.employeeCount() == null
                && business.regularEmployeeCount() == null
                && business.plannedHireCount() == null
                && business.niceCreditScore() == null
                && business.kcbCreditScore() == null
                && business.hasExistingLoan() == null
                && business.hasPolicyFundUsage() == null
                && business.hasGuaranteeUsage() == null;
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param responses 입력 값
     *
     * @return 처리 결과
     */
    private List<InterviewResponseRequest> safeInterviewResponses(List<InterviewResponseRequest> responses) {
        return responses == null ? List.of() : responses;
    }

    /**
     * 입력 값을 표준 형식으로 정규화합니다.
     *
     * @param explicitValue 입력 값
     *
     * @param incomePresenceCode 입력 값
     *
     * @return 처리 결과
     */
    private Boolean normalizeIncomeFlag(Boolean explicitValue, String incomePresenceCode) {
        if ("HAS_INCOME".equals(incomePresenceCode)) {
            return true;
        }
        if ("NONE".equals(incomePresenceCode)) {
            return false;
        }
        return explicitValue;
    }

    /**
     * 요청 값과 업무 규칙을 검증합니다.
     *
     * @param birthYear 입력 값
     *
     * @param label 입력 값
     */
    private void validateBirthYear(Integer birthYear, String label) {
        validateYear(birthYear, label);
    }

    /**
     * 요청 값과 업무 규칙을 검증합니다.
     *
     * @param year 입력 값
     *
     * @param label 입력 값
     */
    private void validateYear(Integer year, String label) {
        if (year != null && (year < 1900 || year > 2200)) {
            throw validationFailed(label + "는 1900년부터 2200년 사이로 입력하세요.");
        }
    }

    /**
     * 요청 값과 업무 규칙을 검증합니다.
     *
     * @param openingDate 입력 값
     */
    private void validateBusinessDate(LocalDate openingDate) {
        if (openingDate != null && openingDate.isAfter(LocalDate.now())) {
            throw validationFailed("개업일은 오늘 이후 날짜로 입력할 수 없습니다.");
        }
    }

    /**
     * 요청 값과 업무 규칙을 검증합니다.
     *
     * @param value 입력 값
     *
     * @param label 입력 값
     */
    private void validateNonNegative(Integer value, String label) {
        if (value != null && value < 0) {
            throw validationFailed(label + "는 0 이상으로 입력하세요.");
        }
    }

    /**
     * 요청 값과 업무 규칙을 검증합니다.
     *
     * @param value 입력 값
     *
     * @param label 입력 값
     */
    private void validateCreditScore(Integer value, String label) {
        if (value != null && (value < 0 || value > 1000)) {
            throw validationFailed(label + "는 0부터 1000 사이로 입력하세요.");
        }
    }

    /**
     * 요청 값과 업무 규칙을 검증합니다.
     *
     * @param value 입력 값
     *
     * @param allowedValues 입력 값
     *
     * @param label 입력 값
     */
    private void validateRequiredCode(String value, Set<String> allowedValues, String label) {
        if (value == null || !allowedValues.contains(value)) {
            throw validationFailed(label + " 값이 올바르지 않습니다.");
        }
    }

    /**
     * 요청 값과 업무 규칙을 검증합니다.
     *
     * @param value 입력 값
     *
     * @param allowedValues 입력 값
     *
     * @param label 입력 값
     */
    private void validateOptionalCode(String value, Set<String> allowedValues, String label) {
        if (value != null && !allowedValues.contains(value)) {
            throw validationFailed(label + " 값이 올바르지 않습니다.");
        }
    }

    /**
     * 요청 값과 업무 규칙을 검증합니다.
     *
     * @param userId 입력 값
     */
    private void validateUserExists(UUID userId) {
        if (userId == null || memberBasicInfoDao.selectUserCountByUserId(userId) < 1) {
            throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다.");
        }
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param authentication 입력 값
     *
     * @return 처리 결과
     */
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

    /**
     * 입력 값을 표준 형식으로 정규화합니다.
     *
     * @param value 입력 값
     *
     * @return 처리 결과
     */
    private String normalizeCode(String value) {
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
     * @param message 입력 값
     *
     * @return 처리 결과
     */
    private ApiException validationFailed(String message) {
        return new ApiException(ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST, message);
    }
}
