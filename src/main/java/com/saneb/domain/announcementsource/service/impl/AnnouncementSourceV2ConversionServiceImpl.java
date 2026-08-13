/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 프로젝트명: saneB
 * 파일명: AnnouncementSourceV2ConversionServiceImpl.java
 * 작성자: 김도훈
 */

package com.saneb.domain.announcementsource.service.impl;

import com.saneb.common.error.ApiException;
import com.saneb.common.error.ErrorCode;
import com.saneb.domain.announcement.dao.AnnouncementDao;
import com.saneb.domain.announcement.vo.AnnouncementDetailsRow;
import com.saneb.domain.announcement.vo.AnnouncementSaveCommand;
import com.saneb.domain.announcement.vo.AnnouncementSupportTypeAssignmentCommand;
import com.saneb.domain.announcement.vo.AnnouncementTargetCategoryAssignmentCommand;
import com.saneb.domain.announcementsource.dao.AnnouncementSourceClassificationDao;
import com.saneb.domain.announcementsource.dao.AnnouncementSourceDao;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceLinkResponse;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceV2ToAnnouncementRequest;
import com.saneb.domain.announcementsource.service.AnnouncementSourceV2ConversionService;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceAuditLogCommand;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceClassificationStateRow;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceLinkCommand;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceLinkedAnnouncementRow;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceReviewHistoryCommand;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceReviewStatusCommand;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceSnapshotRow;
import com.saneb.domain.auth.vo.AuthenticatedUserDetails;
import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnnouncementSourceV2ConversionServiceImpl implements AnnouncementSourceV2ConversionService {

    private static final Set<String> TARGET_CODES = Set.of(
            "BUSINESS", "PERSONAL", "SPOUSE", "CHILD", "PARENT"
    );
    private static final Set<String> SUPPORT_CODES = Set.of(
            "GENERAL_SUPPORT", "GRANT_SUBSIDY", "POLICY_FINANCE", "GUARANTEE",
            "INTEREST_SUPPORT", "VOUCHER_BENEFIT", "REFUND_REDUCTION"
    );
    private static final Set<String> INCOME_CODES = Set.of(
            "INCOME_CERT_ONLY", "HEALTH_INSURANCE_ONLY", "VAT_TAX_BASE_ONLY",
            "ANY_ONE_DOCUMENT", "INCOME_OR_HEALTH_INSURANCE", "NO_LIMIT"
    );

    private final AnnouncementSourceDao announcementSourceDao;
    private final AnnouncementSourceClassificationDao classificationDao;
    private final AnnouncementDao announcementDao;

    public AnnouncementSourceV2ConversionServiceImpl(
            AnnouncementSourceDao announcementSourceDao,
            AnnouncementSourceClassificationDao classificationDao,
            AnnouncementDao announcementDao
    ) {
        this.announcementSourceDao = announcementSourceDao;
        this.classificationDao = classificationDao;
        this.announcementDao = announcementDao;
    }

    @Override
    @Transactional
    public AnnouncementSourceLinkResponse insertOperationalAnnouncement(
            Authentication authentication,
            UUID sourceId,
        AnnouncementSourceV2ToAnnouncementRequest request
    ) {
        UUID actorUserId = selectActorUserId(authentication);
        AnnouncementSourceSnapshotRow source = announcementSourceDao.selectSourceDetailsForUpdate(sourceId);
        if (source == null) {
            throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "수집 원문을 찾을 수 없습니다.");
        }
        AnnouncementSourceLinkedAnnouncementRow linkedAnnouncement =
                announcementSourceDao.selectLinkedAnnouncementDetails(sourceId);
        if (linkedAnnouncement != null) {
            return new AnnouncementSourceLinkResponse(
                    sourceId,
                    source.publicCode(),
                    linkedAnnouncement.announcementId(),
                    linkedAnnouncement.announcementCode()
            );
        }
        AnnouncementSourceClassificationStateRow state = classificationDao.selectClassificationStateDetails(sourceId);
        validateState(source, state, request);

        List<String> targetCodes = normalizeCodes(request.targetCategoryCodes(), TARGET_CODES, "targetCategoryCodes");
        List<String> supportCodes = normalizeCodes(request.supportTypeCodes(), SUPPORT_CODES, "supportTypeCodes");
        String primaryTarget = normalizeCode(
                request.primaryTargetCategoryCode(), TARGET_CODES, "primaryTargetCategoryCode"
        );
        if (!targetCodes.contains(primaryTarget)) {
            throw invalidCategory("대표 지원대상은 지원대상 목록에 포함되어야 합니다.");
        }
        if (!new LinkedHashSet<>(classificationDao.selectConfirmedTargetCategoryCodeList(sourceId))
                .equals(new LinkedHashSet<>(targetCodes))
                || !new LinkedHashSet<>(classificationDao.selectConfirmedSupportTypeCodeList(sourceId))
                .equals(new LinkedHashSet<>(supportCodes))) {
            throw new ApiException(
                    ErrorCode.ANNOUNCEMENT_SOURCE_CLASSIFICATION_REQUIRED,
                    HttpStatus.CONFLICT,
                    "현재 확정 분류와 전환 요청의 지원대상·지원형태가 일치하지 않습니다."
            );
        }
        if (announcementSourceDao.selectPendingDuplicateCandidateCount(sourceId) > 0
                || announcementSourceDao.selectPendingSnapshotDuplicateCount(sourceId) > 0) {
            throw new ApiException(
                    ErrorCode.ANNOUNCEMENT_SOURCE_NOT_CONVERTIBLE,
                    HttpStatus.CONFLICT,
                    "중복 또는 유사 공고 후보를 먼저 검수해야 합니다."
            );
        }

        String incomeCode = request.incomeJudgementCode() == null || request.incomeJudgementCode().isBlank()
                ? "VAT_TAX_BASE_ONLY"
                : normalizeCode(request.incomeJudgementCode(), INCOME_CODES, "incomeJudgementCode");
        UUID announcementId = UUID.randomUUID();
        announcementDao.insertAnnouncement(new AnnouncementSaveCommand(
                announcementId,
                primaryTarget,
                source.title(),
                source.agencyName() == null || source.agencyName().isBlank() ? "기관 미확인" : source.agencyName(),
                source.bodyText(),
                source.applicationStartDate(),
                source.applicationEndDate(),
                incomeCode,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                actorUserId
        ));
        for (String targetCode : targetCodes) {
            announcementDao.insertAnnouncementTargetCategoryAssignment(
                    new AnnouncementTargetCategoryAssignmentCommand(
                            UUID.randomUUID(), announcementId, targetCode, primaryTarget.equals(targetCode),
                            "SOURCE_CONFIRMED", actorUserId
                    )
            );
        }
        for (String supportCode : supportCodes) {
            announcementDao.insertAnnouncementSupportTypeAssignment(
                    new AnnouncementSupportTypeAssignmentCommand(
                            UUID.randomUUID(), announcementId, supportCode, "SOURCE_CONFIRMED", actorUserId
                    )
            );
        }
        announcementSourceDao.insertSourceLink(new AnnouncementSourceLinkCommand(
                UUID.randomUUID(), sourceId, announcementId, actorUserId
        ));
        announcementSourceDao.updateSourceReviewStatus(new AnnouncementSourceReviewStatusCommand(
                sourceId, "CONDITION_INPUT_REQUIRED"
        ));
        announcementSourceDao.insertSourceReviewHistory(new AnnouncementSourceReviewHistoryCommand(
                UUID.randomUUID(), sourceId, source.reviewStatusCode(), "CONDITION_INPUT_REQUIRED",
                "확정된 다중 분류로 운영 공고 DRAFT를 생성했습니다.", actorUserId
        ));
        announcementSourceDao.insertAuditLog(new AnnouncementSourceAuditLogCommand(
                actorUserId,
                "ANNOUNCEMENT_SOURCE_V2_DRAFT_CREATE",
                "ANNOUNCEMENT_SOURCE",
                sourceId,
                "SUCCESS",
                "{\"announcementId\":\"" + announcementId + "\",\"targetCount\":"
                        + targetCodes.size() + ",\"supportCount\":" + supportCodes.size() + "}"
        ));
        AnnouncementDetailsRow announcement = announcementDao.selectAnnouncementDetails(announcementId);
        return new AnnouncementSourceLinkResponse(
                sourceId, source.publicCode(), announcementId, announcement.announcementCode()
        );
    }

    private void validateState(
            AnnouncementSourceSnapshotRow source,
            AnnouncementSourceClassificationStateRow state,
            AnnouncementSourceV2ToAnnouncementRequest request
    ) {
        if (state == null || state.decisionId() == null) {
            throw new ApiException(
                    ErrorCode.ANNOUNCEMENT_SOURCE_CLASSIFICATION_REQUIRED,
                    HttpStatus.CONFLICT,
                    "현재 판정과 관리자 확정 분류가 필요합니다."
            );
        }
        if (!state.decisionId().equals(request.expectedClassificationDecisionId())
                || !state.classificationRowVersion().equals(request.expectedVersion())) {
            throw new ApiException(
                    ErrorCode.ANNOUNCEMENT_SOURCE_VERSION_CONFLICT,
                    HttpStatus.CONFLICT,
                    "판정이 변경되었습니다. 최신 상태를 다시 확인하세요."
            );
        }
        if (!state.confirmedForCurrentDecision()) {
            throw new ApiException(
                    ErrorCode.ANNOUNCEMENT_SOURCE_CLASSIFICATION_REQUIRED,
                    HttpStatus.CONFLICT,
                    "현재 판정을 기준으로 지원대상과 지원형태를 다시 확정해야 합니다."
            );
        }
        if ("EXCLUDED".equals(state.decisionStatusCode())) {
            throw notConvertible("자동 제외된 원문은 운영 공고로 전환할 수 없습니다.");
        }
        if ("REVIEW_REQUIRED".equals(state.decisionStatusCode())
                && !"REVIEW_COMPLETED".equals(source.reviewStatusCode())) {
            throw notConvertible("관리자 검수를 완료한 뒤 운영 공고로 전환할 수 있습니다.");
        }
        if (!Set.of("ACCEPTED", "REVIEW_REQUIRED").contains(state.decisionStatusCode())) {
            throw notConvertible("현재 판정 상태에서는 운영 공고로 전환할 수 없습니다.");
        }
    }

    private List<String> normalizeCodes(List<String> values, Set<String> allowed, String fieldName) {
        if (values == null || values.isEmpty()) {
            throw invalidCategory(fieldName + " must contain at least one value.");
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String value : values) {
            String code = normalizeCode(value, allowed, fieldName);
            if (!normalized.add(code)) {
                throw invalidCategory(fieldName + " must not contain duplicate values.");
            }
        }
        return List.copyOf(normalized);
    }

    private String normalizeCode(String value, Set<String> allowed, String fieldName) {
        if (value == null || value.isBlank()) {
            throw invalidCategory(fieldName + " is required.");
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!allowed.contains(normalized)) {
            throw invalidCategory(fieldName + " is invalid.");
        }
        return normalized;
    }

    private UUID selectActorUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUserDetails details)) {
            throw new ApiException(ErrorCode.AUTH_REQUIRED, HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
        }
        return details.userId();
    }

    private ApiException invalidCategory(String message) {
        return new ApiException(ErrorCode.ANNOUNCEMENT_SOURCE_CATEGORY_INVALID, HttpStatus.BAD_REQUEST, message);
    }

    private ApiException notConvertible(String message) {
        return new ApiException(ErrorCode.ANNOUNCEMENT_SOURCE_NOT_CONVERTIBLE, HttpStatus.CONFLICT, message);
    }
}
