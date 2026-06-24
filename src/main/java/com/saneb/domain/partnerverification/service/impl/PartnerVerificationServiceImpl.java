/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: PartnerVerificationServiceImpl.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.partnerverification.service.impl;

import com.saneb.common.error.ApiException;
import com.saneb.common.error.ErrorCode;
import com.saneb.common.response.PageResponse;
import com.saneb.domain.auth.vo.AuthenticatedUserDetails;
import com.saneb.domain.partnerverification.dao.PartnerVerificationDao;
import com.saneb.domain.partnerverification.dto.PartnerVerificationCreateRequest;
import com.saneb.domain.partnerverification.dto.PartnerVerificationDetailsResponse;
import com.saneb.domain.partnerverification.dto.PartnerVerificationStatusUpdateRequest;
import com.saneb.domain.partnerverification.dto.PartnerVerificationSummaryResponse;
import com.saneb.domain.partnerverification.dto.VerificationBusinessValuesSaveRequest;
import com.saneb.domain.partnerverification.dto.VerificationDocumentsSaveRequest;
import com.saneb.domain.partnerverification.dto.VerificationFamilyValuesSaveRequest;
import com.saneb.domain.partnerverification.dto.VerificationMemberValuesSaveRequest;
import com.saneb.domain.partnerverification.dto.VerificationRestrictionFlagsSaveRequest;
import com.saneb.domain.partnerverification.service.PartnerVerificationService;
import com.saneb.domain.partnerverification.vo.AuditLogCommand;
import com.saneb.domain.partnerverification.vo.PartnerVerificationCreateCommand;
import com.saneb.domain.partnerverification.vo.PartnerVerificationRow;
import com.saneb.domain.partnerverification.vo.PartnerVerificationSearchCondition;
import com.saneb.domain.partnerverification.vo.PartnerVerificationStatusCommand;
import com.saneb.domain.partnerverification.vo.VerificationBusinessValuesCommand;
import com.saneb.domain.partnerverification.vo.VerificationBusinessValuesRow;
import com.saneb.domain.partnerverification.vo.VerificationDocumentCommand;
import com.saneb.domain.partnerverification.vo.VerificationDocumentRow;
import com.saneb.domain.partnerverification.vo.VerificationFamilyValueCommand;
import com.saneb.domain.partnerverification.vo.VerificationFamilyValueRow;
import com.saneb.domain.partnerverification.vo.VerificationMemberValuesCommand;
import com.saneb.domain.partnerverification.vo.VerificationMemberValuesRow;
import com.saneb.domain.partnerverification.vo.VerificationRestrictionFlagCommand;
import com.saneb.domain.partnerverification.vo.VerificationRestrictionFlagRow;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PartnerVerificationServiceImpl implements PartnerVerificationService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final String RESOURCE_TYPE = "PARTNER_VERIFICATION";

    private static final Set<String> VERIFICATION_STATUS_CODES = Set.of(
            "DRAFT", "SUBMITTED", "REVIEWING", "VERIFIED", "REJECTED", "EXPIRED"
    );
    private static final Set<String> MUTABLE_STATUS_CODES = Set.of("DRAFT", "SUBMITTED", "REVIEWING");
    private static final Set<String> RELATION_TYPE_CODES = Set.of("SPOUSE", "CHILD", "PARENT");
    private static final Set<String> RESTRICTION_CODES = Set.of(
            "SAME_BUSINESS_SUSPECTED",
            "SPOUSE_TRANSFER_SUSPECTED",
            "FAMILY_BYPASS_SUSPECTED",
            "CLOSED_REOPEN_SUSPECTED",
            "POLICY_FUND_RESTRICTED",
            "GUARANTEE_RESTRICTED",
            "CREDIT_RECOVERY",
            "PERSONAL_REHABILITATION",
            "BANKRUPTCY_HISTORY",
            "TAX_DELINQUENCY",
            "OVERDUE_HISTORY",
            "NEEDS_REVIEW"
    );
    private static final Set<String> DOCUMENT_SOURCE_TYPE_CODES = Set.of(
            "USER_UPLOAD", "E_CERT", "PARTNER_CHECK", "OPERATOR_CHECK"
    );
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

    private final PartnerVerificationDao partnerVerificationDao;

    /**
     * 객체를 생성합니다.
     *
     * @param partnerVerificationDao 입력 값
     */
    public PartnerVerificationServiceImpl(PartnerVerificationDao partnerVerificationDao) {
        this.partnerVerificationDao = partnerVerificationDao;
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param memberUserId 입력 값
     *
     * @param partnerUserId 입력 값
     *
     * @param statusCode 입력 값
     *
     * @param current 입력 값
     *
     * @param page 입력 값
     *
     * @param size 입력 값
     *
     * @return 처리 결과
     */
    @Override
    public PageResponse<PartnerVerificationSummaryResponse> selectPartnerVerificationList(
            UUID memberUserId,
            UUID partnerUserId,
            String statusCode,
            Boolean current,
            int page,
            int size
    ) {
        validatePageRequest(page, size);
        String normalizedStatusCode = normalizeOptionalCode(statusCode);
        validateOptionalCode("statusCode", normalizedStatusCode, VERIFICATION_STATUS_CODES);

        PartnerVerificationSearchCondition condition = new PartnerVerificationSearchCondition(
                memberUserId,
                partnerUserId,
                normalizedStatusCode,
                current,
                page,
                size,
                (page - 1) * size
        );
        long totalCount = partnerVerificationDao.selectPartnerVerificationCount(condition);
        List<PartnerVerificationSummaryResponse> items = partnerVerificationDao.selectPartnerVerificationList(condition)
                .stream()
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
    public PartnerVerificationDetailsResponse insertPartnerVerification(
            Authentication authentication,
            PartnerVerificationCreateRequest request
    ) {
        UUID actorUserId = selectRequiredActorUserId(authentication);
        validateUserExists(request.memberUserId());
        validateBusinessProfileExists(request.businessProfileId());

        UUID verificationId = UUID.randomUUID();
        partnerVerificationDao.updateCurrentVerificationInactiveByMemberUserId(request.memberUserId(), actorUserId);
        partnerVerificationDao.insertPartnerVerification(new PartnerVerificationCreateCommand(
                verificationId,
                request.memberUserId(),
                actorUserId,
                request.businessProfileId(),
                actorUserId
        ));
        insertAudit(actorUserId, "PARTNER_VERIFICATION_CREATE", verificationId, metadata(
                "statusCode", "DRAFT",
                "current", "true"
        ));
        return selectPartnerVerificationDetails(verificationId);
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param verificationId 입력 값
     *
     * @return 처리 결과
     */
    @Override
    public PartnerVerificationDetailsResponse selectPartnerVerificationDetails(UUID verificationId) {
        PartnerVerificationRow row = selectPartnerVerificationRow(verificationId);
        return toDetailsResponse(
                row,
                partnerVerificationDao.selectVerificationMemberValues(verificationId),
                partnerVerificationDao.selectVerificationBusinessValues(verificationId),
                partnerVerificationDao.selectVerificationFamilyValueList(verificationId),
                partnerVerificationDao.selectVerificationRestrictionFlagList(verificationId),
                partnerVerificationDao.selectVerificationDocumentList(verificationId)
        );
    }

    /**
     * 업무 데이터를 수정합니다.
     *
     * @param authentication 입력 값
     *
     * @param verificationId 입력 값
     *
     * @param request 입력 값
     */
    @Override
    /**
     * 업무 데이터를 수정합니다.
     *
     * @param authentication 입력 값
     *
     * @param verificationId 입력 값
     *
     * @param request 입력 값
     */
    @Transactional
    public void updateVerificationMemberValues(
            Authentication authentication,
            UUID verificationId,
            VerificationMemberValuesSaveRequest request
    ) {
        UUID actorUserId = selectRequiredActorUserId(authentication);
        PartnerVerificationRow row = selectPartnerVerificationRow(verificationId);
        ensureMutable(row);

        partnerVerificationDao.deleteVerificationMemberValues(verificationId);
        partnerVerificationDao.insertVerificationMemberValues(new VerificationMemberValuesCommand(
                verificationId,
                request.birthYear(),
                trimToNull(request.address()),
                normalizeOptionalCode(request.regionCode()),
                request.householder(),
                request.householdMember(),
                normalizeOptionalCode(request.healthInsuranceBasisCode()),
                request.hasIncome(),
                actorUserId
        ));
        insertAudit(actorUserId, "PARTNER_VERIFICATION_MEMBER_VALUES_SAVE", verificationId, metadata("section", "member"));
    }

    /**
     * 업무 데이터를 수정합니다.
     *
     * @param authentication 입력 값
     *
     * @param verificationId 입력 값
     *
     * @param request 입력 값
     */
    @Override
    /**
     * 업무 데이터를 수정합니다.
     *
     * @param authentication 입력 값
     *
     * @param verificationId 입력 값
     *
     * @param request 입력 값
     */
    @Transactional
    public void updateVerificationBusinessValues(
            Authentication authentication,
            UUID verificationId,
            VerificationBusinessValuesSaveRequest request
    ) {
        UUID actorUserId = selectRequiredActorUserId(authentication);
        PartnerVerificationRow row = selectPartnerVerificationRow(verificationId);
        ensureMutable(row);

        partnerVerificationDao.deleteVerificationBusinessValues(verificationId);
        partnerVerificationDao.insertVerificationBusinessValues(new VerificationBusinessValuesCommand(
                verificationId,
                request.annualRevenue(),
                request.employeeCount(),
                request.regularEmployeeCount(),
                normalizeOptionalCode(request.taxStatusCode()),
                request.niceCreditScore(),
                request.kcbCreditScore(),
                request.hasExistingLoan(),
                request.hasPolicyFundUsage(),
                request.hasGuaranteeUsage(),
                request.financialCheckedOn(),
                actorUserId
        ));
        insertAudit(actorUserId, "PARTNER_VERIFICATION_BUSINESS_VALUES_SAVE", verificationId, metadata("section", "business"));
    }

    /**
     * 업무 데이터를 수정합니다.
     *
     * @param authentication 입력 값
     *
     * @param verificationId 입력 값
     *
     * @param request 입력 값
     */
    @Override
    /**
     * 업무 데이터를 수정합니다.
     *
     * @param authentication 입력 값
     *
     * @param verificationId 입력 값
     *
     * @param request 입력 값
     */
    @Transactional
    public void updateVerificationFamilyValues(
            Authentication authentication,
            UUID verificationId,
            VerificationFamilyValuesSaveRequest request
    ) {
        UUID actorUserId = selectRequiredActorUserId(authentication);
        PartnerVerificationRow row = selectPartnerVerificationRow(verificationId);
        ensureMutable(row);

        partnerVerificationDao.deleteVerificationFamilyValues(verificationId);
        for (VerificationFamilyValuesSaveRequest.FamilyValueRequest familyValue
                : nullToEmpty(request.familyValues())) {
            String relationTypeCode = normalizeRequiredCode("relationTypeCode", familyValue.relationTypeCode(), RELATION_TYPE_CODES);
            partnerVerificationDao.insertVerificationFamilyValue(new VerificationFamilyValueCommand(
                    verificationId,
                    relationTypeCode,
                    familyValue.birthYear(),
                    trimToNull(familyValue.address()),
                    normalizeOptionalCode(familyValue.schoolAgeStatusCode()),
                    normalizeOptionalCode(familyValue.enrollmentStatusCode()),
                    familyValue.cohabiting(),
                    familyValue.supported(),
                    familyValue.hasIncome(),
                    actorUserId
            ));
        }
        insertAudit(actorUserId, "PARTNER_VERIFICATION_FAMILY_VALUES_SAVE", verificationId, metadata(
                "section", "family",
                "count", String.valueOf(nullToEmpty(request.familyValues()).size())
        ));
    }

    /**
     * 업무 데이터를 수정합니다.
     *
     * @param authentication 입력 값
     *
     * @param verificationId 입력 값
     *
     * @param request 입력 값
     */
    @Override
    /**
     * 업무 데이터를 수정합니다.
     *
     * @param authentication 입력 값
     *
     * @param verificationId 입력 값
     *
     * @param request 입력 값
     */
    @Transactional
    public void updateVerificationDocuments(
            Authentication authentication,
            UUID verificationId,
            VerificationDocumentsSaveRequest request
    ) {
        UUID actorUserId = selectRequiredActorUserId(authentication);
        PartnerVerificationRow row = selectPartnerVerificationRow(verificationId);
        ensureMutable(row);

        partnerVerificationDao.deleteVerificationDocuments(verificationId);
        for (VerificationDocumentsSaveRequest.DocumentRequest document : nullToEmpty(request.documents())) {
            String documentTypeCode = normalizeRequiredCode("documentTypeCode", document.documentTypeCode(), DOCUMENT_TYPE_CODES);
            String sourceTypeCode = normalizeRequiredCode("sourceTypeCode", document.sourceTypeCode(), DOCUMENT_SOURCE_TYPE_CODES);
            partnerVerificationDao.insertVerificationDocument(new VerificationDocumentCommand(
                    verificationId,
                    documentTypeCode,
                    sourceTypeCode,
                    document.checked(),
                    Boolean.TRUE.equals(document.checked()) ? actorUserId : null,
                    trimToNull(document.note()),
                    actorUserId
            ));
        }
        insertAudit(actorUserId, "PARTNER_VERIFICATION_DOCUMENTS_SAVE", verificationId, metadata(
                "section", "documents",
                "count", String.valueOf(nullToEmpty(request.documents()).size())
        ));
    }

    /**
     * 업무 데이터를 수정합니다.
     *
     * @param authentication 입력 값
     *
     * @param verificationId 입력 값
     *
     * @param request 입력 값
     */
    @Override
    /**
     * 업무 데이터를 수정합니다.
     *
     * @param authentication 입력 값
     *
     * @param verificationId 입력 값
     *
     * @param request 입력 값
     */
    @Transactional
    public void updateVerificationRestrictionFlags(
            Authentication authentication,
            UUID verificationId,
            VerificationRestrictionFlagsSaveRequest request
    ) {
        UUID actorUserId = selectRequiredActorUserId(authentication);
        PartnerVerificationRow row = selectPartnerVerificationRow(verificationId);
        ensureMutable(row);

        partnerVerificationDao.deleteVerificationRestrictionFlags(verificationId);
        for (VerificationRestrictionFlagsSaveRequest.RestrictionFlagRequest restrictionFlag
                : nullToEmpty(request.restrictionFlags())) {
            String restrictionCode = normalizeRequiredCode("restrictionCode", restrictionFlag.restrictionCode(), RESTRICTION_CODES);
            partnerVerificationDao.insertVerificationRestrictionFlag(new VerificationRestrictionFlagCommand(
                    verificationId,
                    restrictionCode,
                    restrictionFlag.checked(),
                    trimToNull(restrictionFlag.note()),
                    actorUserId
            ));
        }
        insertAudit(actorUserId, "PARTNER_VERIFICATION_RESTRICTION_FLAGS_SAVE", verificationId, metadata(
                "section", "restrictionFlags",
                "count", String.valueOf(nullToEmpty(request.restrictionFlags()).size())
        ));
    }

    /**
     * 업무 데이터를 수정합니다.
     *
     * @param authentication 입력 값
     *
     * @param verificationId 입력 값
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
     * @param verificationId 입력 값
     *
     * @param request 입력 값
     *
     * @return 처리 결과
     */
    @Transactional
    public PartnerVerificationDetailsResponse updatePartnerVerificationStatus(
            Authentication authentication,
            UUID verificationId,
            PartnerVerificationStatusUpdateRequest request
    ) {
        UUID actorUserId = selectRequiredActorUserId(authentication);
        PartnerVerificationRow row = selectPartnerVerificationRow(verificationId);
        String beforeStatusCode = row.statusCode();
        String afterStatusCode = normalizeRequiredCode("statusCode", request.statusCode(), VERIFICATION_STATUS_CODES);

        if (beforeStatusCode.equals(afterStatusCode)) {
            return selectPartnerVerificationDetails(verificationId);
        }
        validateStatusTransition(beforeStatusCode, afterStatusCode);

        int updatedCount = partnerVerificationDao.updatePartnerVerificationStatus(new PartnerVerificationStatusCommand(
                verificationId,
                afterStatusCode,
                actorUserId,
                trimToNull(request.reviewNote())
        ));
        if (updatedCount == 0) {
            throw notFound();
        }

        String actionCode = switch (afterStatusCode) {
            case "SUBMITTED" -> "PARTNER_VERIFICATION_SUBMIT";
            case "VERIFIED" -> "PARTNER_VERIFICATION_VERIFY";
            case "REJECTED" -> "PARTNER_VERIFICATION_REJECT";
            default -> null;
        };
        if (actionCode != null) {
            insertAudit(actorUserId, actionCode, verificationId, metadata(
                    "beforeStatusCode", beforeStatusCode,
                    "afterStatusCode", afterStatusCode
            ));
        }
        return selectPartnerVerificationDetails(verificationId);
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param verificationId 입력 값
     *
     * @return 처리 결과
     */
    private PartnerVerificationRow selectPartnerVerificationRow(UUID verificationId) {
        PartnerVerificationRow row = partnerVerificationDao.selectPartnerVerificationDetails(verificationId);
        if (row == null) {
            throw notFound();
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
    private PartnerVerificationSummaryResponse toSummaryResponse(PartnerVerificationRow row) {
        return new PartnerVerificationSummaryResponse(
                row.verificationId(),
                row.memberUserId(),
                row.partnerUserId(),
                row.businessProfileId(),
                row.verificationCode(),
                row.memberUserCode(),
                row.partnerUserCode(),
                row.statusCode(),
                Boolean.TRUE.equals(row.current()),
                Boolean.TRUE.equals(row.matchingBlocked()),
                row.submittedAt(),
                row.verifiedAt(),
                row.createdAt(),
                row.updatedAt()
        );
    }

    /**
     * 업무 데이터를 응답 형식으로 변환합니다.
     *
     * @param row 입력 값
     *
     * @param memberValues 입력 값
     *
     * @param businessValues 입력 값
     *
     * @param familyValues 입력 값
     *
     * @param restrictionFlags 입력 값
     *
     * @param documents 입력 값
     *
     * @return 처리 결과
     */
    private PartnerVerificationDetailsResponse toDetailsResponse(
            PartnerVerificationRow row,
            VerificationMemberValuesRow memberValues,
            VerificationBusinessValuesRow businessValues,
            List<VerificationFamilyValueRow> familyValues,
            List<VerificationRestrictionFlagRow> restrictionFlags,
            List<VerificationDocumentRow> documents
    ) {
        return new PartnerVerificationDetailsResponse(
                row.verificationId(),
                row.memberUserId(),
                row.partnerUserId(),
                row.businessProfileId(),
                row.verificationCode(),
                row.memberUserCode(),
                row.partnerUserCode(),
                row.statusCode(),
                Boolean.TRUE.equals(row.current()),
                Boolean.TRUE.equals(row.matchingBlocked()),
                row.submittedAt(),
                row.verifiedAt(),
                row.reviewedBy(),
                row.reviewNote(),
                row.createdAt(),
                row.updatedAt(),
                toMemberValuesResponse(memberValues),
                toBusinessValuesResponse(businessValues),
                nullToEmpty(familyValues).stream().map(this::toFamilyValueResponse).toList(),
                nullToEmpty(restrictionFlags).stream().map(this::toRestrictionFlagResponse).toList(),
                nullToEmpty(documents).stream().map(this::toDocumentResponse).toList()
        );
    }

    /**
     * 업무 데이터를 응답 형식으로 변환합니다.
     *
     * @param row 입력 값
     *
     * @return 처리 결과
     */
    private PartnerVerificationDetailsResponse.MemberValuesResponse toMemberValuesResponse(
            VerificationMemberValuesRow row
    ) {
        if (row == null) {
            return null;
        }
        return new PartnerVerificationDetailsResponse.MemberValuesResponse(
                row.birthYear(),
                row.address(),
                row.regionCode(),
                row.householder(),
                row.householdMember(),
                row.healthInsuranceBasisCode(),
                row.hasIncome()
        );
    }

    /**
     * 업무 데이터를 응답 형식으로 변환합니다.
     *
     * @param row 입력 값
     *
     * @return 처리 결과
     */
    private PartnerVerificationDetailsResponse.BusinessValuesResponse toBusinessValuesResponse(
            VerificationBusinessValuesRow row
    ) {
        if (row == null) {
            return null;
        }
        return new PartnerVerificationDetailsResponse.BusinessValuesResponse(
                row.annualRevenue(),
                row.employeeCount(),
                row.regularEmployeeCount(),
                row.taxStatusCode(),
                row.niceCreditScore(),
                row.kcbCreditScore(),
                row.hasExistingLoan(),
                row.hasPolicyFundUsage(),
                row.hasGuaranteeUsage(),
                row.financialCheckedOn()
        );
    }

    /**
     * 업무 데이터를 응답 형식으로 변환합니다.
     *
     * @param row 입력 값
     *
     * @return 처리 결과
     */
    private PartnerVerificationDetailsResponse.FamilyValueResponse toFamilyValueResponse(
            VerificationFamilyValueRow row
    ) {
        return new PartnerVerificationDetailsResponse.FamilyValueResponse(
                row.relationTypeCode(),
                row.birthYear(),
                row.address(),
                row.schoolAgeStatusCode(),
                row.enrollmentStatusCode(),
                row.cohabiting(),
                row.supported(),
                row.hasIncome()
        );
    }

    /**
     * 업무 데이터를 응답 형식으로 변환합니다.
     *
     * @param row 입력 값
     *
     * @return 처리 결과
     */
    private PartnerVerificationDetailsResponse.RestrictionFlagResponse toRestrictionFlagResponse(
            VerificationRestrictionFlagRow row
    ) {
        return new PartnerVerificationDetailsResponse.RestrictionFlagResponse(
                row.restrictionCode(),
                Boolean.TRUE.equals(row.checked()),
                row.note()
        );
    }

    /**
     * 업무 데이터를 응답 형식으로 변환합니다.
     *
     * @param row 입력 값
     *
     * @return 처리 결과
     */
    private PartnerVerificationDetailsResponse.DocumentResponse toDocumentResponse(VerificationDocumentRow row) {
        return new PartnerVerificationDetailsResponse.DocumentResponse(
                row.documentTypeCode(),
                row.sourceTypeCode(),
                Boolean.TRUE.equals(row.checked()),
                row.checkedBy(),
                row.checkedAt(),
                row.note()
        );
    }

    /**
     * 요청 값과 업무 규칙을 검증합니다.
     *
     * @param userId 입력 값
     */
    private void validateUserExists(UUID userId) {
        if (partnerVerificationDao.selectUserCountById(userId) == 0) {
            throw notFound();
        }
    }

    /**
     * 요청 값과 업무 규칙을 검증합니다.
     *
     * @param businessProfileId 입력 값
     */
    private void validateBusinessProfileExists(UUID businessProfileId) {
        if (businessProfileId != null && partnerVerificationDao.selectBusinessProfileCountById(businessProfileId) == 0) {
            throw notFound();
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
            throw new ApiException(ErrorCode.INVALID_PAGE_REQUEST, HttpStatus.BAD_REQUEST, "Invalid page request.");
        }
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param row 입력 값
     */
    private void ensureMutable(PartnerVerificationRow row) {
        if (!MUTABLE_STATUS_CODES.contains(row.statusCode())) {
            throw new ApiException(
                    ErrorCode.INVALID_STATUS_TRANSITION,
                    HttpStatus.CONFLICT,
                    "Terminal verification cannot be changed."
            );
        }
    }

    /**
     * 요청 값과 업무 규칙을 검증합니다.
     *
     * @param beforeStatusCode 입력 값
     *
     * @param afterStatusCode 입력 값
     */
    private void validateStatusTransition(String beforeStatusCode, String afterStatusCode) {
        boolean valid = switch (beforeStatusCode) {
            case "DRAFT" -> "SUBMITTED".equals(afterStatusCode);
            case "SUBMITTED" -> "REVIEWING".equals(afterStatusCode);
            case "REVIEWING" -> Set.of("VERIFIED", "REJECTED", "EXPIRED").contains(afterStatusCode);
            default -> false;
        };
        if (!valid) {
            throw new ApiException(
                    ErrorCode.INVALID_STATUS_TRANSITION,
                    HttpStatus.CONFLICT,
                    "Verification status transition is not allowed."
            );
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
                "Database backed authentication is required."
        );
    }

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param actorUserId 입력 값
     *
     * @param actionCode 입력 값
     *
     * @param verificationId 입력 값
     *
     * @param metadataJson 입력 값
     */
    private void insertAudit(UUID actorUserId, String actionCode, UUID verificationId, String metadataJson) {
        partnerVerificationDao.insertAuditLog(new AuditLogCommand(
                actorUserId,
                actionCode,
                RESOURCE_TYPE,
                verificationId,
                "SUCCESS",
                metadataJson
        ));
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param key 입력 값
     *
     * @param value 입력 값
     *
     * @return 처리 결과
     */
    private String metadata(String key, String value) {
        return "{\"" + key + "\":\"" + value + "\"}";
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
     * @return 처리 결과
     */
    private String metadata(String key1, String value1, String key2, String value2) {
        return "{\"" + key1 + "\":\"" + value1 + "\",\"" + key2 + "\":\"" + value2 + "\"}";
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
        return new ApiException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "Partner verification was not found.");
    }
}
