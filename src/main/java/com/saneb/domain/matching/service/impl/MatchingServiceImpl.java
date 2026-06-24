/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: MatchingServiceImpl.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.matching.service.impl;

import com.saneb.common.error.ApiException;
import com.saneb.common.error.ErrorCode;
import com.saneb.common.response.PageResponse;
import com.saneb.domain.auth.vo.AuthenticatedUserDetails;
import com.saneb.domain.matching.dao.MatchingDao;
import com.saneb.domain.matching.dto.MatchingCandidateGenerateRequest;
import com.saneb.domain.matching.dto.MatchingCandidateGenerateResponse;
import com.saneb.domain.matching.dto.MatchingCaseCreateRequest;
import com.saneb.domain.matching.dto.MatchingCaseDetailsResponse;
import com.saneb.domain.matching.dto.MatchingCaseStatusUpdateRequest;
import com.saneb.domain.matching.dto.MatchingCaseSummaryResponse;
import com.saneb.domain.matching.dto.MatchingFinalRecalculateRequest;
import com.saneb.domain.matching.dto.MatchingMemberLookupResponse;
import com.saneb.domain.matching.dto.MatchingResultDetailResponse;
import com.saneb.domain.matching.service.MatchingService;
import com.saneb.domain.matching.vo.AnnouncementMatchingRow;
import com.saneb.domain.matching.vo.AuditLogCommand;
import com.saneb.domain.matching.vo.MatchingCandidateAnnouncementRow;
import com.saneb.domain.matching.vo.MatchingCaseCreateCommand;
import com.saneb.domain.matching.vo.MatchingCaseRow;
import com.saneb.domain.matching.vo.MatchingCaseSearchCondition;
import com.saneb.domain.matching.vo.MatchingCaseStageStatusCommand;
import com.saneb.domain.matching.vo.MatchingCaseStatusCommand;
import com.saneb.domain.matching.vo.MatchingMemberLookupRow;
import com.saneb.domain.matching.vo.MatchingMemberLookupSearchCondition;
import com.saneb.domain.matching.vo.MatchingResultDetailCommand;
import com.saneb.domain.matching.vo.MatchingResultDetailRow;
import com.saneb.domain.matching.vo.VerificationMatchingRow;
import com.saneb.domain.operation.dao.OperationDao;
import com.saneb.domain.operation.vo.NotificationDeliveryLogCommand;
import com.saneb.domain.operation.vo.NotificationMessageInsertCommand;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class MatchingServiceImpl implements MatchingService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final String RESOURCE_TYPE = "MATCHING_CASE";
    private static final String RESULT_SCOPE_CODE = "APPLICATION";
    private static final String RESULT_CONDITION_KEY = "RESTRICTION_FLAGS";
    private static final String BASIC_PROFILE_CONDITION_KEY = "BASIC_PROFILE_CONDITIONS";
    private static final String BASIC_STAGE_CODE = "BASIC";
    private static final String FINAL_STAGE_CODE = "FINAL";
    private static final String BASIC_BASIS_CODE = "BASIC_INFO";
    private static final String PARTNER_BASIS_CODE = "PARTNER_INPUT";
    private static final String DOCUMENT_BASIS_CODE = "DOCUMENT_INPUT";

    private static final Set<String> MATCHING_STATUS_CODES = Set.of(
            "MATCHED", "NOT_MATCHED", "REVIEW_REQUIRED", "BLOCKED", "PROGRESSED"
    );
    private static final Set<String> MATCHING_STAGE_CODES = Set.of(BASIC_STAGE_CODE, FINAL_STAGE_CODE);
    private static final Set<String> MATCHING_BASIS_CODES = Set.of(
            BASIC_BASIS_CODE, PARTNER_BASIS_CODE, DOCUMENT_BASIS_CODE
    );
    private static final List<String> HARD_BLOCK_RESTRICTION_CODES = List.of(
            "POLICY_FUND_RESTRICTED",
            "GUARANTEE_RESTRICTED",
            "CREDIT_RECOVERY",
            "PERSONAL_REHABILITATION",
            "BANKRUPTCY_HISTORY",
            "TAX_DELINQUENCY",
            "OVERDUE_HISTORY"
    );
    private static final List<String> REVIEW_RESTRICTION_CODES = List.of(
            "SAME_BUSINESS_SUSPECTED",
            "SPOUSE_TRANSFER_SUSPECTED",
            "FAMILY_BYPASS_SUSPECTED",
            "CLOSED_REOPEN_SUSPECTED",
            "NEEDS_REVIEW"
    );

    private final MatchingDao matchingDao;
    private final OperationDao operationDao;
    private final TransactionTemplate auditTransactionTemplate;

    /**
     * 객체를 생성합니다.
     *
     * @param matchingDao 입력 값
     *
     * @param operationDao 입력 값
     *
     * @param transactionManager 입력 값
     */
    public MatchingServiceImpl(
            MatchingDao matchingDao,
            OperationDao operationDao,
            PlatformTransactionManager transactionManager
    ) {
        this.matchingDao = matchingDao;
        this.operationDao = operationDao;
        this.auditTransactionTemplate = new TransactionTemplate(transactionManager);
        this.auditTransactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
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
    public MatchingCandidateGenerateResponse insertMatchingCandidates(
            Authentication authentication,
            MatchingCandidateGenerateRequest request
    ) {
        UUID actorUserId = selectRequiredActorUserId(authentication);
        return saveMatchingCandidates(
                actorUserId,
                request.memberUserId(),
                BASIC_STAGE_CODE,
                BASIC_BASIS_CODE,
                false,
                "MATCHING_CANDIDATE_GENERATE"
        );
    }

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param actorUserId 입력 값
     *
     * @param memberUserId 입력 값
     *
     * @return 처리 결과
     */
    @Override
    /**
     * 업무 데이터를 등록합니다.
     *
     * @param actorUserId 입력 값
     *
     * @param memberUserId 입력 값
     *
     * @return 처리 결과
     */
    @Transactional
    public MatchingCandidateGenerateResponse insertBasicMatchingCandidates(UUID actorUserId, UUID memberUserId) {
        return saveMatchingCandidates(
                actorUserId,
                memberUserId,
                BASIC_STAGE_CODE,
                BASIC_BASIS_CODE,
                false,
                "MATCHING_BASIC_AUTO_GENERATE"
        );
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
    public MatchingCandidateGenerateResponse insertFinalMatchingCandidates(
            Authentication authentication,
            MatchingFinalRecalculateRequest request
    ) {
        UUID actorUserId = selectRequiredActorUserId(authentication);
        return saveMatchingCandidates(
                actorUserId,
                request.memberUserId(),
                FINAL_STAGE_CODE,
                DOCUMENT_BASIS_CODE,
                true,
                "MATCHING_FINAL_RECALCULATE"
        );
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
    public MatchingCaseDetailsResponse insertMatchingCase(
            Authentication authentication,
            MatchingCaseCreateRequest request
    ) {
        UUID actorUserId = selectRequiredActorUserId(authentication);
        validateAnnouncement(actorUserId, request.announcementId());
        if (request.verificationId() != null) {
            validateVerification(actorUserId, request.verificationId(), request.memberUserId());
        }

        MatchingCaseRow existing = matchingDao.selectMatchingCaseDetailsByStageBusinessKey(
                request.announcementId(),
                request.memberUserId(),
                request.verificationId(),
                FINAL_STAGE_CODE
        );
        if (existing != null) {
            insertAudit(actorUserId, "MATCHING_CASE_CREATE", existing.matchingCaseId(), "SUCCESS", metadata(
                    "createdCount", "0",
                    "skippedCount", "1",
                    "failureReasonCode", "DUPLICATE_MATCHING_CASE"
            ));
            return toDetailsResponse(existing);
        }

        MatchingDecision decision = decideMatching(request.verificationId());
        UUID matchingCaseId = UUID.randomUUID();
        matchingDao.insertMatchingCase(new MatchingCaseCreateCommand(
                matchingCaseId,
                request.announcementId(),
                request.memberUserId(),
                request.verificationId(),
                decision.statusCode(),
                decision.reasonCode(),
                FINAL_STAGE_CODE,
                PARTNER_BASIS_CODE,
                actorUserId
        ));
        matchingDao.insertMatchingResultDetail(new MatchingResultDetailCommand(
                UUID.randomUUID(),
                matchingCaseId,
                RESULT_SCOPE_CODE,
                RESULT_CONDITION_KEY,
                decision.resultCode(),
                decision.basisValue(),
                decision.requiredValue(),
                decision.reason(),
                actorUserId
        ));
        insertAudit(actorUserId, "MATCHING_CASE_CREATE", matchingCaseId, "SUCCESS", metadata(
                "createdCount", "1",
                "skippedCount", "0",
                "statusCode", decision.statusCode()
        ));
        return selectMatchingCaseDetails(matchingCaseId);
    }

    /**
     * 업무 데이터를 저장합니다.
     *
     * @param actorUserId 입력 값
     *
     * @param memberUserId 입력 값
     *
     * @param matchingStageCode 입력 값
     *
     * @param matchingBasisCode 입력 값
     *
     * @param finalMatching 입력 값
     *
     * @param auditActionCode 입력 값
     *
     * @return 처리 결과
     */
    private MatchingCandidateGenerateResponse saveMatchingCandidates(
            UUID actorUserId,
            UUID memberUserId,
            String matchingStageCode,
            String matchingBasisCode,
            boolean finalMatching,
            String auditActionCode
    ) {
        if (actorUserId == null || memberUserId == null || matchingDao.selectMatchingMemberUserCount(memberUserId) == 0) {
            insertFailureAudit(actorUserId, "MEMBER_USER_NOT_FOUND");
            throw notFound();
        }

        List<MatchingCandidateAnnouncementRow> candidates =
                matchingDao.selectEligibleAnnouncementCandidateList(memberUserId, finalMatching);
        List<UUID> eligibleAnnouncementIds = candidates.stream()
                .map(MatchingCandidateAnnouncementRow::announcementId)
                .toList();
        int staleCount = matchingDao.updateMatchingCaseStageNotEligible(new MatchingCaseStageStatusCommand(
                memberUserId,
                matchingStageCode,
                "NOT_MATCHED",
                actorUserId,
                eligibleAnnouncementIds
        ));

        int createdCount = 0;
        int skippedCount = staleCount;
        List<UUID> createdMatchingCaseIds = new ArrayList<>();
        for (MatchingCandidateAnnouncementRow candidate : candidates) {
            MatchingCaseRow existing = matchingDao.selectMatchingCaseDetailsByStageBusinessKey(
                    candidate.announcementId(),
                    memberUserId,
                    null,
                    matchingStageCode
            );
            if (existing != null) {
                if (!"MATCHED".equals(existing.statusCode()) || !matchingBasisCode.equals(existing.matchingBasisCode())) {
                    matchingDao.updateMatchingCaseStageStatus(
                            candidate.announcementId(),
                            memberUserId,
                            null,
                            matchingStageCode,
                            "MATCHED",
                            matchingBasisCode,
                            actorUserId
                    );
                }
                skippedCount++;
                continue;
            }

            UUID matchingCaseId = UUID.randomUUID();
            matchingDao.insertMatchingCase(new MatchingCaseCreateCommand(
                    matchingCaseId,
                    candidate.announcementId(),
                    memberUserId,
                    null,
                    "MATCHED",
                    null,
                    matchingStageCode,
                    matchingBasisCode,
                    actorUserId
            ));
            createdMatchingCaseIds.add(matchingCaseId);
            matchingDao.insertMatchingResultDetail(new MatchingResultDetailCommand(
                    UUID.randomUUID(),
                    matchingCaseId,
                    RESULT_SCOPE_CODE,
                    BASIC_PROFILE_CONDITION_KEY,
                    "PASS",
                    String.valueOf(candidate.matchedConditionCount() == null ? 0 : candidate.matchedConditionCount()),
                    String.valueOf(candidate.checkedConditionCount() == null ? 0 : candidate.checkedConditionCount()),
                    selectCandidateReason(matchingStageCode),
                    actorUserId
            ));
            createdCount++;
        }
        if (BASIC_STAGE_CODE.equals(matchingStageCode) && createdCount > 0) {
            insertNewCandidateNotification(actorUserId, memberUserId, createdCount, createdMatchingCaseIds.get(0));
        }

        insertAudit(actorUserId, auditActionCode, memberUserId, "SUCCESS", metadata(
                "createdCount", String.valueOf(createdCount),
                "skippedCount", String.valueOf(skippedCount),
                "candidateCount", String.valueOf(candidates.size())
        ));
        List<MatchingCaseSummaryResponse> candidateList = selectMatchingCaseList(
                null,
                memberUserId,
                null,
                "MATCHED",
                matchingStageCode,
                null,
                1,
                MAX_PAGE_SIZE
        ).items();
        return new MatchingCandidateGenerateResponse(memberUserId, createdCount, skippedCount, candidateList);
    }

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param actorUserId 입력 값
     *
     * @param memberUserId 입력 값
     *
     * @param createdCount 입력 값
     *
     * @param firstMatchingCaseId 입력 값
     */
    private void insertNewCandidateNotification(
            UUID actorUserId,
            UUID memberUserId,
            int createdCount,
            UUID firstMatchingCaseId
    ) {
        UUID notificationId = UUID.randomUUID();
        operationDao.insertNotificationMessage(new NotificationMessageInsertCommand(
                notificationId,
                memberUserId,
                null,
                "IN_APP",
                "새로운 가능 공고가 확인되었습니다",
                "기본정보 기준으로 현재 확인 가능한 공고 " + createdCount + "건이 새로 확인되었습니다.",
                "SENT",
                "MATCHING_CASE",
                firstMatchingCaseId,
                actorUserId
        ));
        operationDao.insertNotificationDeliveryLog(new NotificationDeliveryLogCommand(
                UUID.randomUUID(),
                notificationId,
                "IN_APP",
                "INTERNAL",
                "SUCCESS",
                1,
                null,
                null,
                null,
                "{\"source\":\"BASIC_MATCHING_CANDIDATE_GENERATE\",\"createdCount\":\"" + createdCount + "\"}"
        ));
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param announcementId 입력 값
     *
     * @param memberUserId 입력 값
     *
     * @param verificationId 입력 값
     *
     * @param statusCode 입력 값
     *
     * @param matchingStageCode 입력 값
     *
     * @param matchingBasisCode 입력 값
     *
     * @param page 입력 값
     *
     * @param size 입력 값
     *
     * @return 처리 결과
     */
    @Override
    public PageResponse<MatchingCaseSummaryResponse> selectMatchingCaseList(
            UUID announcementId,
            UUID memberUserId,
            UUID verificationId,
            String statusCode,
            String matchingStageCode,
            String matchingBasisCode,
            int page,
            int size
    ) {
        validatePageRequest(page, size);
        String normalizedStatusCode = normalizeOptionalCode(statusCode);
        validateOptionalCode("statusCode", normalizedStatusCode, MATCHING_STATUS_CODES);
        String normalizedStageCode = normalizeOptionalCode(matchingStageCode);
        validateOptionalCode("matchingStageCode", normalizedStageCode, MATCHING_STAGE_CODES);
        String normalizedBasisCode = normalizeOptionalCode(matchingBasisCode);
        validateOptionalCode("matchingBasisCode", normalizedBasisCode, MATCHING_BASIS_CODES);

        MatchingCaseSearchCondition condition = new MatchingCaseSearchCondition(
                announcementId,
                memberUserId,
                verificationId,
                normalizedStatusCode,
                normalizedStageCode,
                normalizedBasisCode,
                page,
                size,
                (page - 1) * size
        );
        long totalCount = matchingDao.selectMatchingCaseCount(condition);
        List<MatchingCaseSummaryResponse> items = matchingDao.selectMatchingCaseList(condition)
                .stream()
                .map(this::toSummaryResponse)
                .toList();
        return PageResponse.of(items, page, size, totalCount);
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param authentication 입력 값
     *
     * @param page 입력 값
     *
     * @param size 입력 값
     *
     * @return 처리 결과
     */
    @Override
    public PageResponse<MatchingCaseSummaryResponse> selectMyBasicMatchingCaseList(
            Authentication authentication,
            int page,
            int size
    ) {
        UUID userId = selectRequiredActorUserId(authentication);
        return selectMatchingCaseList(null, userId, null, null, BASIC_STAGE_CODE, null, page, size);
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param announcementId 입력 값
     *
     * @param memberUserId 입력 값
     *
     * @param statusCode 입력 값
     *
     * @param page 입력 값
     *
     * @param size 입력 값
     *
     * @return 처리 결과
     */
    @Override
    public PageResponse<MatchingCaseSummaryResponse> selectFinalMatchingCaseList(
            UUID announcementId,
            UUID memberUserId,
            String statusCode,
            int page,
            int size
    ) {
        String effectiveStatusCode = normalizeOptionalCode(statusCode);
        if (effectiveStatusCode == null) {
            effectiveStatusCode = "MATCHED";
        }
        return selectMatchingCaseList(
                announcementId,
                memberUserId,
                null,
                effectiveStatusCode,
                FINAL_STAGE_CODE,
                null,
                page,
                size
        );
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param keyword 입력 값
     *
     * @param page 입력 값
     *
     * @param size 입력 값
     *
     * @return 처리 결과
     */
    @Override
    public PageResponse<MatchingMemberLookupResponse> selectMatchingMemberLookupList(
            String keyword,
            int page,
            int size
    ) {
        validateLookupPageRequest(page, size);
        MatchingMemberLookupSearchCondition condition = new MatchingMemberLookupSearchCondition(
                trimToNull(keyword),
                page,
                size,
                (page - 1) * size
        );
        long totalCount = matchingDao.selectMatchingMemberLookupCount(condition);
        List<MatchingMemberLookupResponse> items = matchingDao.selectMatchingMemberLookupList(condition).stream()
                .map(this::toMemberLookupResponse)
                .toList();
        return PageResponse.of(items, page, size, totalCount);
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param matchingCaseId 입력 값
     *
     * @return 처리 결과
     */
    @Override
    public MatchingCaseDetailsResponse selectMatchingCaseDetails(UUID matchingCaseId) {
        return toDetailsResponse(selectMatchingCaseRow(matchingCaseId));
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param matchingCaseId 입력 값
     *
     * @return 처리 결과
     */
    @Override
    public List<MatchingResultDetailResponse> selectMatchingResultDetailList(UUID matchingCaseId) {
        selectMatchingCaseRow(matchingCaseId);
        return matchingDao.selectMatchingResultDetailList(matchingCaseId)
                .stream()
                .map(this::toResultDetailResponse)
                .toList();
    }

    /**
     * 업무 데이터를 수정합니다.
     *
     * @param authentication 입력 값
     *
     * @param matchingCaseId 입력 값
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
     * @param matchingCaseId 입력 값
     *
     * @param request 입력 값
     *
     * @return 처리 결과
     */
    @Transactional
    public MatchingCaseDetailsResponse updateMatchingCaseStatus(
            Authentication authentication,
            UUID matchingCaseId,
            MatchingCaseStatusUpdateRequest request
    ) {
        UUID actorUserId = selectRequiredActorUserId(authentication);
        MatchingCaseRow before = selectMatchingCaseRow(matchingCaseId);
        String statusCode = normalizeRequiredCode("statusCode", request.statusCode(), MATCHING_STATUS_CODES);
        String blockedReasonCode = trimToNull(request.blockedReasonCode());

        if (before.statusCode().equals(statusCode) && equalsNullable(before.blockedReasonCode(), blockedReasonCode)) {
            return toDetailsResponse(before);
        }

        int updatedCount = matchingDao.updateMatchingCaseStatus(new MatchingCaseStatusCommand(
                matchingCaseId,
                statusCode,
                blockedReasonCode,
                actorUserId
        ));
        if (updatedCount == 0) {
            throw notFound();
        }
        insertAudit(actorUserId, "MATCHING_CASE_STATUS_UPDATE", matchingCaseId, "SUCCESS", metadata(
                "beforeStatusCode", before.statusCode(),
                "afterStatusCode", statusCode,
                "blockedReasonCode", blockedReasonCode == null ? "" : blockedReasonCode
        ));
        return selectMatchingCaseDetails(matchingCaseId);
    }

    /**
     * 요청 값과 업무 규칙을 검증합니다.
     *
     * @param actorUserId 입력 값
     *
     * @param announcementId 입력 값
     */
    private void validateAnnouncement(UUID actorUserId, UUID announcementId) {
        AnnouncementMatchingRow row = matchingDao.selectAnnouncementForMatching(announcementId);
        if (row == null) {
            insertFailureAudit(actorUserId, "ANNOUNCEMENT_NOT_FOUND");
            throw notFound();
        }
        if (!"APPROVED".equals(row.approvalStatusCode())) {
            insertFailureAudit(actorUserId, "ANNOUNCEMENT_NOT_APPROVED");
            throw new ApiException(
                    ErrorCode.ANNOUNCEMENT_NOT_APPROVED,
                    HttpStatus.CONFLICT,
                    "Announcement is not approved."
            );
        }
        if (!"NORMAL".equals(row.manualStatusCode())) {
            insertFailureAudit(actorUserId, "ANNOUNCEMENT_NOT_OPEN");
            throw new ApiException(
                    ErrorCode.MATCHING_BLOCKED,
                    HttpStatus.CONFLICT,
                    "Announcement manual status does not allow matching."
            );
        }
    }

    /**
     * 요청 값과 업무 규칙을 검증합니다.
     *
     * @param actorUserId 입력 값
     *
     * @param verificationId 입력 값
     *
     * @param memberUserId 입력 값
     */
    private void validateVerification(UUID actorUserId, UUID verificationId, UUID memberUserId) {
        VerificationMatchingRow row = matchingDao.selectVerificationForMatching(verificationId);
        if (row == null) {
            insertFailureAudit(actorUserId, "VERIFICATION_NOT_FOUND");
            throw notFound();
        }
        if (!row.memberUserId().equals(memberUserId)) {
            insertFailureAudit(actorUserId, "VERIFICATION_MEMBER_MISMATCH");
            throw validationFailed("verificationId does not belong to memberUserId.");
        }
        if (!"VERIFIED".equals(row.statusCode()) || !Boolean.TRUE.equals(row.current())) {
            insertFailureAudit(actorUserId, "VERIFICATION_NOT_VERIFIED");
            throw new ApiException(
                    ErrorCode.VERIFICATION_NOT_VERIFIED,
                    HttpStatus.CONFLICT,
                    "Verification is not verified current data."
            );
        }
        if (Boolean.TRUE.equals(row.matchingBlocked())) {
            insertFailureAudit(actorUserId, "VERIFICATION_MATCHING_BLOCKED");
            throw new ApiException(
                    ErrorCode.MATCHING_BLOCKED,
                    HttpStatus.CONFLICT,
                    "Verification is blocked from matching."
            );
        }
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param verificationId 입력 값
     *
     * @return 처리 결과
     */
    private MatchingDecision decideMatching(UUID verificationId) {
        if (verificationId == null) {
            return new MatchingDecision(
                    "MATCHED",
                    "PASS",
                    null,
                    "VERIFICATION_NOT_PROVIDED",
                    "VERIFICATION_NOT_REQUIRED",
                    "Manual matching without verification."
            );
        }
        List<String> checkedRestrictionCodes = matchingDao.selectCheckedRestrictionFlagCodeList(verificationId);
        String hardBlockCode = firstContained(HARD_BLOCK_RESTRICTION_CODES, checkedRestrictionCodes);
        if (hardBlockCode != null) {
            return new MatchingDecision(
                    "BLOCKED",
                    "FAIL",
                    hardBlockCode,
                    hardBlockCode,
                    "NO_BLOCKING_RESTRICTION",
                    "Hard restriction flag exists."
            );
        }
        String reviewCode = firstContained(REVIEW_RESTRICTION_CODES, checkedRestrictionCodes);
        if (reviewCode != null) {
            return new MatchingDecision(
                    "REVIEW_REQUIRED",
                    "REVIEW_REQUIRED",
                    reviewCode,
                    reviewCode,
                    "NO_REVIEW_RESTRICTION",
                    "Review restriction flag exists."
            );
        }
        return new MatchingDecision(
                "MATCHED",
                "PASS",
                null,
                "NONE",
                "NO_BLOCKING_RESTRICTION",
                "No checked restriction flag."
        );
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param orderedCodes 입력 값
     *
     * @param checkedCodes 입력 값
     *
     * @return 처리 결과
     */
    private String firstContained(List<String> orderedCodes, List<String> checkedCodes) {
        for (String code : orderedCodes) {
            if (checkedCodes.contains(code)) {
                return code;
            }
        }
        return null;
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param matchingStageCode 입력 값
     *
     * @return 처리 결과
     */
    private String selectCandidateReason(String matchingStageCode) {
        if (FINAL_STAGE_CODE.equals(matchingStageCode)) {
            return "저장된 기본정보와 서류별 선택 입력값이 공고 조건을 충족했습니다.";
        }
        return "저장된 기본정보가 공고 조건을 충족했습니다.";
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param matchingCaseId 입력 값
     *
     * @return 처리 결과
     */
    private MatchingCaseRow selectMatchingCaseRow(UUID matchingCaseId) {
        MatchingCaseRow row = matchingDao.selectMatchingCaseDetails(matchingCaseId);
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
    private MatchingCaseSummaryResponse toSummaryResponse(MatchingCaseRow row) {
        return new MatchingCaseSummaryResponse(
                row.matchingCaseId(),
                row.matchingCaseCode(),
                row.announcementId(),
                row.announcementCode(),
                row.memberUserId(),
                row.memberUserCode(),
                row.verificationId(),
                row.verificationCode(),
                row.statusCode(),
                row.blockedReasonCode(),
                row.matchingStageCode(),
                row.matchingBasisCode(),
                row.matchedAt(),
                row.createdAt(),
                row.updatedAt(),
                row.announcementTitle(),
                row.agencyName(),
                row.targetTypeCode(),
                row.minAmount(),
                row.maxAmount(),
                row.applicationStartDate(),
                row.applicationEndDate(),
                row.memberLoginId(),
                row.memberName(),
                Boolean.TRUE.equals(row.progressCreated())
        );
    }

    /**
     * 업무 데이터를 응답 형식으로 변환합니다.
     *
     * @param row 입력 값
     *
     * @return 처리 결과
     */
    private MatchingMemberLookupResponse toMemberLookupResponse(MatchingMemberLookupRow row) {
        return new MatchingMemberLookupResponse(
                row.userId(),
                row.userCode(),
                row.loginId(),
                row.name(),
                row.statusCode(),
                row.createdAt()
        );
    }

    /**
     * 업무 데이터를 응답 형식으로 변환합니다.
     *
     * @param row 입력 값
     *
     * @return 처리 결과
     */
    private MatchingCaseDetailsResponse toDetailsResponse(MatchingCaseRow row) {
        return new MatchingCaseDetailsResponse(
                row.matchingCaseId(),
                row.matchingCaseCode(),
                row.announcementId(),
                row.announcementCode(),
                row.memberUserId(),
                row.memberUserCode(),
                row.verificationId(),
                row.verificationCode(),
                row.statusCode(),
                row.blockedReasonCode(),
                row.matchingStageCode(),
                row.matchingBasisCode(),
                row.matchedAt(),
                row.reviewedBy(),
                row.reviewedAt(),
                row.createdAt(),
                row.updatedAt()
        );
    }

    /**
     * 업무 데이터를 응답 형식으로 변환합니다.
     *
     * @param row 입력 값
     *
     * @return 처리 결과
     */
    private MatchingResultDetailResponse toResultDetailResponse(MatchingResultDetailRow row) {
        return new MatchingResultDetailResponse(
                row.matchingResultDetailId(),
                row.matchingCaseId(),
                row.conditionScopeCode(),
                row.conditionKey(),
                row.resultCode(),
                row.basisValue(),
                row.requiredValue(),
                row.reason(),
                row.createdAt()
        );
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
     * 요청 값과 업무 규칙을 검증합니다.
     *
     * @param page 입력 값
     *
     * @param size 입력 값
     */
    private void validateLookupPageRequest(int page, int size) {
        if (page < 1 || size < 1 || size > 50) {
            throw new ApiException(ErrorCode.INVALID_PAGE_REQUEST, HttpStatus.BAD_REQUEST, "Invalid lookup page request.");
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
     * @param left 입력 값
     *
     * @param right 입력 값
     *
     * @return 처리 결과
     */
    private boolean equalsNullable(String left, String right) {
        return left == null ? right == null : left.equals(right);
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
     * @param failureReasonCode 입력 값
     */
    private void insertFailureAudit(UUID actorUserId, String failureReasonCode) {
        auditTransactionTemplate.executeWithoutResult(status -> insertAudit(
                actorUserId,
                "MATCHING_CASE_CREATE",
                null,
                "FAIL",
                metadata(
                        "createdCount", "0",
                        "skippedCount", "0",
                        "failureReasonCode", failureReasonCode
                )
        ));
    }

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param actorUserId 입력 값
     *
     * @param actionCode 입력 값
     *
     * @param resourceId 입력 값
     *
     * @param resultCode 입력 값
     *
     * @param metadataJson 입력 값
     */
    private void insertAudit(UUID actorUserId, String actionCode, UUID resourceId, String resultCode, String metadataJson) {
        matchingDao.insertAuditLog(new AuditLogCommand(
                actorUserId,
                actionCode,
                RESOURCE_TYPE,
                resourceId,
                resultCode,
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
     * @return 처리 결과
     */
    private ApiException notFound() {
        return new ApiException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "Matching resource was not found.");
    }

    private record MatchingDecision(
            String statusCode,
            String resultCode,
            String reasonCode,
            String basisValue,
            String requiredValue,
            String reason
    ) {
    }
}
