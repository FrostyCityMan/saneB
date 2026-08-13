/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 프로젝트명: saneB
 * 파일명: AnnouncementSourceClassificationManagementServiceImpl.java
 * 작성자: 김도훈
 */

package com.saneb.domain.announcementsource.service.impl;

import com.saneb.common.error.ApiException;
import com.saneb.common.error.ErrorCode;
import com.saneb.domain.announcementsource.dao.AnnouncementSourceClassificationDao;
import com.saneb.domain.announcementsource.dao.AnnouncementSourceDao;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceClassificationDetailsResponse;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceConfirmedClassificationSaveRequest;
import com.saneb.domain.announcementsource.service.AnnouncementSourceClassificationManagementService;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceAuditLogCommand;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceClassificationDetailsRow;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceClassificationStateRow;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceConfirmedSupportCommand;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceConfirmedTargetCommand;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceReviewHistoryCommand;
import com.saneb.domain.auth.vo.AuthenticatedUserDetails;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
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
public class AnnouncementSourceClassificationManagementServiceImpl
        implements AnnouncementSourceClassificationManagementService {

    private static final Set<String> TARGET_CODES = Set.of(
            "BUSINESS", "PERSONAL", "SPOUSE", "CHILD", "PARENT"
    );
    private static final Set<String> SUPPORT_CODES = Set.of(
            "GENERAL_SUPPORT", "GRANT_SUBSIDY", "POLICY_FINANCE", "GUARANTEE",
            "INTEREST_SUPPORT", "VOUCHER_BENEFIT", "REFUND_REDUCTION"
    );

    private final AnnouncementSourceClassificationDao classificationDao;
    private final AnnouncementSourceDao announcementSourceDao;

    public AnnouncementSourceClassificationManagementServiceImpl(
            AnnouncementSourceClassificationDao classificationDao,
            AnnouncementSourceDao announcementSourceDao
    ) {
        this.classificationDao = classificationDao;
        this.announcementSourceDao = announcementSourceDao;
    }

    @Override
    public AnnouncementSourceClassificationDetailsResponse selectClassificationDetails(UUID sourceId) {
        AnnouncementSourceClassificationDetailsRow row = classificationDao.selectClassificationDetails(sourceId);
        if (row == null) {
            throw new ApiException(
                    ErrorCode.ANNOUNCEMENT_SOURCE_CLASSIFICATION_REQUIRED,
                    HttpStatus.NOT_FOUND,
                    "현재 판정 근거를 찾을 수 없습니다."
            );
        }
        AnnouncementSourceClassificationStateRow state = classificationDao.selectClassificationStateDetails(sourceId);
        List<String> confirmedTargets = classificationDao.selectConfirmedTargetCategoryCodeList(sourceId);
        List<String> confirmedSupports = classificationDao.selectConfirmedSupportTypeCodeList(sourceId);
        String confirmedStatus = confirmedTargets.isEmpty() || confirmedSupports.isEmpty()
                ? "MISSING"
                : state != null && state.confirmedForCurrentDecision() ? "CURRENT" : "STALE";

        return new AnnouncementSourceClassificationDetailsResponse(
                row.sourceId(),
                row.decisionId(),
                row.ruleReleaseCode(),
                row.semanticStatusCode(),
                row.reasonCode(),
                row.titleStageCode(),
                row.bodyStageCode(),
                row.bodySourceCode(),
                row.bodyAvailabilityCode(),
                classificationDao.selectAutomaticTargetCategoryCodeList(sourceId),
                classificationDao.selectAutomaticSupportTypeCodeList(sourceId),
                confirmedStatus,
                confirmedTargets,
                confirmedSupports,
                row.version(),
                row.evaluatedAt(),
                classificationDao.selectClassificationMatchList(sourceId).stream()
                        .map(match -> new AnnouncementSourceClassificationDetailsResponse.MatchResponse(
                                match.ruleGroupCode(),
                                match.canonicalKeyword(),
                                match.matchedTerm(),
                                match.locationCode(),
                                match.startOffset(),
                                match.endOffset(),
                                match.appliedActionCode()
                        ))
                        .toList()
        );
    }

    @Override
    @Transactional
    public AnnouncementSourceClassificationDetailsResponse saveConfirmedClassification(
            Authentication authentication,
            UUID sourceId,
            AnnouncementSourceConfirmedClassificationSaveRequest request
    ) {
        UUID actorUserId = selectActorUserId(authentication);
        AnnouncementSourceClassificationStateRow state = classificationDao.selectClassificationStateDetails(sourceId);
        if (state == null || state.decisionId() == null) {
            throw new ApiException(
                    ErrorCode.ANNOUNCEMENT_SOURCE_CLASSIFICATION_REQUIRED,
                    HttpStatus.CONFLICT,
                    "현재 판정을 먼저 완료해야 합니다."
            );
        }
        if (!state.decisionId().equals(request.expectedClassificationDecisionId())) {
            throw versionConflict();
        }
        List<String> previousTargetCodes = selectSortedCodes(
                classificationDao.selectConfirmedTargetCategoryCodeList(sourceId)
        );
        List<String> previousSupportCodes = selectSortedCodes(
                classificationDao.selectConfirmedSupportTypeCodeList(sourceId)
        );
        List<String> targetCodes = normalizeCodes(request.targetCategoryCodes(), TARGET_CODES, "targetCategoryCodes");
        List<String> supportCodes = normalizeCodes(request.supportTypeCodes(), SUPPORT_CODES, "supportTypeCodes");
        List<String> sortedTargetCodes = selectSortedCodes(targetCodes);
        List<String> sortedSupportCodes = selectSortedCodes(supportCodes);
        String reviewNote = normalizeReviewNote(request.reviewNote());
        if (classificationDao.updateClassificationRowVersion(sourceId, request.expectedVersion()) == 0) {
            throw versionConflict();
        }

        classificationDao.deleteConfirmedTargetCategoryList(sourceId);
        for (String targetCode : targetCodes) {
            classificationDao.insertConfirmedTargetCategory(new AnnouncementSourceConfirmedTargetCommand(
                    UUID.randomUUID(), sourceId, targetCode, state.decisionId(), actorUserId
            ));
        }
        classificationDao.deleteConfirmedSupportTypeList(sourceId);
        for (String supportCode : supportCodes) {
            classificationDao.insertConfirmedSupportType(new AnnouncementSourceConfirmedSupportCommand(
                UUID.randomUUID(), sourceId, supportCode, state.decisionId(), actorUserId
            ));
        }
        announcementSourceDao.insertSourceReviewHistory(new AnnouncementSourceReviewHistoryCommand(
                UUID.randomUUID(),
                sourceId,
                state.reviewStatusCode(),
                state.reviewStatusCode(),
                reviewNote,
                actorUserId
        ));
        announcementSourceDao.insertAuditLog(new AnnouncementSourceAuditLogCommand(
                actorUserId,
                "ANNOUNCEMENT_SOURCE_CLASSIFICATION_CONFIRMED",
                "ANNOUNCEMENT_SOURCE",
                sourceId,
                "SUCCESS",
                selectConfirmationAuditMetadata(
                        sourceId,
                        state.decisionId(),
                        previousTargetCodes,
                        sortedTargetCodes,
                        previousSupportCodes,
                        sortedSupportCodes,
                        reviewNote
                )
        ));
        return selectClassificationDetails(sourceId);
    }

    private String selectConfirmationAuditMetadata(
            UUID sourceId,
            UUID evaluationId,
            List<String> previousTargetCodes,
            List<String> nextTargetCodes,
            List<String> previousSupportCodes,
            List<String> nextSupportCodes,
            String reviewNote
    ) {
        boolean reviewNoteProvided = reviewNote != null;
        String reviewNoteSha256 = reviewNoteProvided ? sha256(reviewNote) : null;
        return "{\"sourceId\":\"" + sourceId
                + "\",\"evaluationId\":\"" + evaluationId
                + "\",\"previousTargetCategoryCodes\":" + selectJsonArray(previousTargetCodes)
                + ",\"nextTargetCategoryCodes\":" + selectJsonArray(nextTargetCodes)
                + ",\"previousSupportTypeCodes\":" + selectJsonArray(previousSupportCodes)
                + ",\"nextSupportTypeCodes\":" + selectJsonArray(nextSupportCodes)
                + ",\"targetCount\":" + nextTargetCodes.size()
                + ",\"supportCount\":" + nextSupportCodes.size()
                + ",\"reviewNoteProvided\":" + reviewNoteProvided
                + ",\"reviewNoteLength\":" + (reviewNoteProvided ? reviewNote.length() : 0)
                + ",\"reviewNoteSha256\":"
                + (reviewNoteSha256 == null ? "null" : "\"" + reviewNoteSha256 + "\"")
                + "}";
    }

    private List<String> selectSortedCodes(List<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return List.of();
        }
        return codes.stream().sorted().toList();
    }

    private String selectJsonArray(List<String> codes) {
        if (codes.isEmpty()) {
            return "[]";
        }
        return "[\"" + String.join("\",\"", codes) + "\"]";
    }

    private String normalizeReviewNote(String reviewNote) {
        if (reviewNote == null || reviewNote.isBlank()) {
            return null;
        }
        return reviewNote.strip();
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(value.getBytes(StandardCharsets.UTF_8))
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 해시 알고리즘을 사용할 수 없습니다.", exception);
        }
    }

    private List<String> normalizeCodes(List<String> values, Set<String> allowed, String fieldName) {
        if (values == null || values.isEmpty()) {
            throw invalidCategory(fieldName + " must contain at least one value.");
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String value : values) {
            if (value == null || value.isBlank()) {
                throw invalidCategory(fieldName + " contains an empty value.");
            }
            String code = value.trim().toUpperCase(Locale.ROOT);
            if (!allowed.contains(code) || !normalized.add(code)) {
                throw invalidCategory(fieldName + " contains an invalid or duplicate value.");
            }
        }
        return List.copyOf(normalized);
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

    private ApiException versionConflict() {
        return new ApiException(
                ErrorCode.ANNOUNCEMENT_SOURCE_VERSION_CONFLICT,
                HttpStatus.CONFLICT,
                "다른 작업자가 판정을 변경했습니다. 최신 상태를 확인한 뒤 다시 시도하세요."
        );
    }
}
