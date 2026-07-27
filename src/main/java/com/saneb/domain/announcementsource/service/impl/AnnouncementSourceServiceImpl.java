/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: AnnouncementSourceServiceImpl.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.announcementsource.service.impl;

import com.saneb.common.error.ApiException;
import com.saneb.common.error.ErrorCode;
import com.saneb.common.response.PageResponse;
import com.saneb.domain.announcement.dao.AnnouncementDao;
import com.saneb.domain.announcement.vo.AnnouncementDetailsRow;
import com.saneb.domain.announcement.vo.AnnouncementSaveCommand;
import com.saneb.domain.announcementsource.dao.AnnouncementSourceDao;
import com.saneb.domain.announcementsource.dao.LocalGovernmentNoticeDao;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceAttachmentResponse;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceCollectionApprovalRequest;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceCollectionRequestCreateRequest;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceCollectionRequestResponse;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceCollectionRunDetailsResponse;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceCollectionRunItemResponse;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceCollectionRunResponse;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceDetailsResponse;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceDuplicateCandidateResponse;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceDuplicateDecisionRequest;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceHighlightResponse;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceLinkResponse;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceReviewStatusUpdateRequest;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceSummaryResponse;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceSnapshotDuplicateResponse;
import com.saneb.domain.announcementsource.localgov.dto.LocalGovernmentNoticeCollectionResultResponse;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceToAnnouncementRequest;
import com.saneb.domain.announcementsource.provider.AnnouncementSourceProviderAttachment;
import com.saneb.domain.announcementsource.provider.AnnouncementSourceProviderBatch;
import com.saneb.domain.announcementsource.provider.AnnouncementSourceProviderClient;
import com.saneb.domain.announcementsource.provider.AnnouncementSourceProviderItem;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.service.AnnouncementSourceHighlightService;
import com.saneb.domain.announcementsource.service.AnnouncementSourceService;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceAttachmentCommand;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceAuditLogCommand;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceCollectionApprovalCommand;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceCollectionRequestCommand;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceCollectionRequestRow;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceCollectionRequestSearchCondition;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceCollectionRunCommand;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceCollectionRunItemCommand;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceCollectionRunRow;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceCollectionRunSearchCondition;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceDuplicateCandidateCommand;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceDuplicateCandidateRow;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceDuplicateDecisionCommand;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceHighlightCommand;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceLinkCommand;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceReviewHistoryCommand;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceReviewStatusCommand;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceSearchCondition;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceSnapshotCommand;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceSnapshotDuplicateCommand;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceSnapshotDuplicateDecisionCommand;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceSnapshotRow;
import com.saneb.domain.auth.vo.AuthenticatedUserDetails;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnnouncementSourceServiceImpl implements AnnouncementSourceService {

    private static final Set<String> PROVIDER_CODES = Set.of("BIZINFO", "GOV24_PUBLIC_SERVICE", "LOCAL_GOV_NOTICE");
    private static final Set<String> REQUEST_TYPE_CODES = Set.of("BATCH", "MANUAL");
    private static final Set<String> APPROVAL_STATUS_CODES = Set.of("APPROVED", "REJECTED", "CANCELED", "EXPIRED");
    private static final Set<String> DUPLICATE_DECISION_ACTION_CODES = Set.of("CREATE_NEW", "UPDATE_EXISTING", "IGNORE");
    private static final Set<String> REVIEW_STATUS_CODES = Set.of(
            "COLLECTED",
            "REVIEW_PENDING",
            "CONDITION_INPUT_REQUIRED",
            "REVIEW_COMPLETED",
            "ACTIVATED",
            "ARCHIVED",
            "DUPLICATE",
            "SKIPPED_ENDED"
    );
    private static final String DEFAULT_TARGET_TYPE_CODE = "BUSINESS";
    private static final String DEFAULT_INCOME_JUDGEMENT_CODE = "VAT_TAX_BASE_ONLY";

    private final AnnouncementSourceDao announcementSourceDao;
    private final LocalGovernmentNoticeDao localGovernmentNoticeDao;
    private final AnnouncementDao announcementDao;
    private final AnnouncementSourceHighlightService highlightService;
    private final Map<String, AnnouncementSourceProviderClient> providerClients;
    private final AnnouncementSourceIdentityNormalizer identityNormalizer = new AnnouncementSourceIdentityNormalizer();

    /**
     * 객체를 생성합니다.
     *
     * @param announcementSourceDao 입력 값
     *
     * @param localGovernmentNoticeDao 지자체 출처 수집 결과 DAO
     *
     * @param announcementDao 입력 값
     *
     * @param highlightService 입력 값
     *
     * @param providerClients 입력 값
     */
    public AnnouncementSourceServiceImpl(
            AnnouncementSourceDao announcementSourceDao,
            LocalGovernmentNoticeDao localGovernmentNoticeDao,
            AnnouncementDao announcementDao,
            AnnouncementSourceHighlightService highlightService,
            List<AnnouncementSourceProviderClient> providerClients
    ) {
        this.announcementSourceDao = announcementSourceDao;
        this.localGovernmentNoticeDao = localGovernmentNoticeDao;
        this.announcementDao = announcementDao;
        this.highlightService = highlightService;
        this.providerClients = providerClients.stream()
                .collect(Collectors.toUnmodifiableMap(AnnouncementSourceProviderClient::selectProviderCode, Function.identity()));
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
    @Transactional
    public AnnouncementSourceCollectionRequestResponse insertCollectionRequest(
            Authentication authentication,
            AnnouncementSourceCollectionRequestCreateRequest request
    ) {
        UUID actorUserId = selectActorUserId(authentication);
        String providerCode = normalizeRequiredCode("providerCode", request.providerCode(), PROVIDER_CODES);
        String requestTypeCode = normalizeRequiredCode("requestTypeCode", request.requestTypeCode(), REQUEST_TYPE_CODES);
        if (!"MANUAL".equals(requestTypeCode)) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST, "관리자 화면에서는 수동 수집 요청만 생성할 수 있습니다.");
        }
        UUID requestId = UUID.randomUUID();
        announcementSourceDao.insertCollectionRequest(new AnnouncementSourceCollectionRequestCommand(
                requestId,
                providerCode,
                requestTypeCode,
                actorUserId,
                nullIfBlank(request.requestedFrom()),
                nullIfBlank(request.searchKeyword()),
                nullIfBlank(request.searchRegionCode()),
                nullIfBlank(request.searchCategoryCode()),
                request.startDate(),
                request.endDate(),
                request.maxCount(),
                nullIfBlank(request.requestNote()),
                null,
                null,
                null,
                null,
                null
        ));
        insertAudit(actorUserId, "ANNOUNCEMENT_SOURCE_COLLECTION_REQUEST_CREATE", "ANNOUNCEMENT_SOURCE_COLLECTION_REQUEST", requestId,
                "{\"providerCode\":\"" + providerCode + "\",\"requestTypeCode\":\"" + requestTypeCode + "\"}");
        return selectCollectionRequestDetails(requestId);
    }

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param providerCode 입력 값
     *
     * @param maxCount 입력 값
     *
     * @return 처리 결과
     */
    @Override
    @Transactional
    public AnnouncementSourceCollectionRequestResponse insertBatchCollectionRequest(String providerCode, Integer maxCount) {
        String normalizedProviderCode = normalizeRequiredCode("providerCode", providerCode, PROVIDER_CODES);
        if (announcementSourceDao.selectOpenBatchRequestCount(normalizedProviderCode, null, null) > 0) {
            return AnnouncementSourceCollectionRequestResponse.from(
                    announcementSourceDao.selectCollectionRequestList(new AnnouncementSourceCollectionRequestSearchCondition(
                            normalizedProviderCode,
                            "BATCH",
                            "APPROVAL_PENDING",
                            1,
                            0
                    )).get(0)
            );
        }
        UUID requestId = UUID.randomUUID();
        announcementSourceDao.insertCollectionRequest(new AnnouncementSourceCollectionRequestCommand(
                requestId,
                normalizedProviderCode,
                "BATCH",
                null,
                "SCHEDULED_BATCH",
                null,
                null,
                null,
                LocalDate.now(),
                LocalDate.now().plusMonths(3),
                maxCount,
                "자동 배치가 생성한 외부 공고 수집 승인 요청입니다.",
                null,
                null,
                null,
                null,
                null
        ));
        return selectCollectionRequestDetails(requestId);
    }

    /**
     * 단일 지자체 URL의 수동 수집 승인 요청을 등록합니다.
     *
     * @param authentication 인증 정보
     * @param localGovernmentSourceId 지자체 URL 식별자
     * @param maxCount 최대 수집 건수
     * @param requestNote 요청 메모
     * @return 수집 승인 요청
     */
    @Override
    @Transactional
    public AnnouncementSourceCollectionRequestResponse insertLocalGovernmentCollectionRequest(
            Authentication authentication,
            UUID localGovernmentSourceId,
            Integer maxCount,
            String requestNote
    ) {
        UUID requestId = UUID.randomUUID();
        announcementSourceDao.insertCollectionRequest(new AnnouncementSourceCollectionRequestCommand(
                requestId, "LOCAL_GOV_NOTICE", "MANUAL", selectActorUserId(authentication), "LOCAL_GOVERNMENT_ADMIN",
                null, null, null, null, null, maxCount, nullIfBlank(requestNote), localGovernmentSourceId,
                null, null, null, null
        ));
        return selectCollectionRequestDetails(requestId);
    }

    /**
     * 승인된 정기 스케줄 근거로 자동 실행용 승인 요청을 등록합니다.
     *
     * @param scheduleId 승인 스케줄 식별자
     * @param maxCount 최대 수집 URL 수
     * @return 승인된 수집 요청
     */
    @Override
    @Transactional
    public AnnouncementSourceCollectionRequestResponse insertApprovedScheduledCollectionRequest(
            UUID scheduleId,
            Integer maxCount
    ) {
        UUID requestId = UUID.randomUUID();
        announcementSourceDao.insertCollectionRequest(new AnnouncementSourceCollectionRequestCommand(
                requestId, "LOCAL_GOV_NOTICE", "BATCH", null, "APPROVED_SCHEDULE", null, null, null,
                LocalDate.now(), LocalDate.now(), maxCount, "사전 승인된 지자체 정기 수집 일정의 자동 실행 요청입니다.",
                null, scheduleId, "APPROVED", null, "승인된 정기 수집 일정에 따른 자동 실행"
        ));
        return selectCollectionRequestDetails(requestId);
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param providerCode 입력 값
     *
     * @param requestTypeCode 입력 값
     *
     * @param requestStatusCode 입력 값
     *
     * @param page 입력 값
     *
     * @param size 입력 값
     *
     * @return 처리 결과
     */
    @Override
    public PageResponse<AnnouncementSourceCollectionRequestResponse> selectCollectionRequestList(
            String providerCode,
            String requestTypeCode,
            String requestStatusCode,
            int page,
            int size
    ) {
        AnnouncementSourceCollectionRequestSearchCondition condition = new AnnouncementSourceCollectionRequestSearchCondition(
                normalizeOptionalCode(providerCode),
                normalizeOptionalCode(requestTypeCode),
                normalizeOptionalCode(requestStatusCode),
                size,
                (page - 1) * size
        );
        long totalCount = announcementSourceDao.selectCollectionRequestCount(condition);
        List<AnnouncementSourceCollectionRequestResponse> items = announcementSourceDao.selectCollectionRequestList(condition)
                .stream()
                .map(AnnouncementSourceCollectionRequestResponse::from)
                .toList();
        return PageResponse.of(items, page, size, totalCount);
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param requestId 입력 값
     *
     * @return 처리 결과
     */
    @Override
    public AnnouncementSourceCollectionRequestResponse selectCollectionRequestDetails(UUID requestId) {
        return AnnouncementSourceCollectionRequestResponse.from(selectCollectionRequestRow(requestId));
    }

    /**
     * 업무 데이터를 수정합니다.
     *
     * @param authentication 입력 값
     *
     * @param requestId 입력 값
     *
     * @param request 입력 값
     *
     * @return 처리 결과
     */
    @Override
    @Transactional
    public AnnouncementSourceCollectionRequestResponse updateCollectionRequestApproval(
            Authentication authentication,
            UUID requestId,
            AnnouncementSourceCollectionApprovalRequest request
    ) {
        UUID actorUserId = selectActorUserId(authentication);
        String statusCode = normalizeRequiredCode("approvalStatusCode", request.approvalStatusCode(), APPROVAL_STATUS_CODES);
        selectCollectionRequestRow(requestId);
        int updated = announcementSourceDao.updateCollectionRequestApproval(new AnnouncementSourceCollectionApprovalCommand(
                requestId,
                statusCode,
                actorUserId,
                nullIfBlank(request.approvalNote())
        ));
        if (updated == 0) {
            throw new ApiException(ErrorCode.INVALID_STATUS_TRANSITION, HttpStatus.BAD_REQUEST, "승인 대기 상태의 수집 요청만 처리할 수 있습니다.");
        }
        insertAudit(actorUserId, "ANNOUNCEMENT_SOURCE_COLLECTION_REQUEST_STATUS_UPDATE", "ANNOUNCEMENT_SOURCE_COLLECTION_REQUEST", requestId,
                "{\"statusCode\":\"" + statusCode + "\"}");
        return selectCollectionRequestDetails(requestId);
    }

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param requestId 입력 값
     *
     * @return 처리 결과
     */
    @Override
    @Transactional
    public AnnouncementSourceCollectionRunResponse insertCollectionRun(UUID requestId) {
        AnnouncementSourceCollectionRequestRow request = selectCollectionRequestRow(requestId);
        if (!"APPROVED".equals(request.requestStatusCode())) {
            throw new ApiException(ErrorCode.INVALID_STATUS_TRANSITION, HttpStatus.BAD_REQUEST, "승인된 수집 요청만 실행할 수 있습니다.");
        }
        AnnouncementSourceProviderClient providerClient = providerClients.get(request.providerCode());
        if (providerClient == null) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST, "지원하지 않는 외부 공고 API 제공자입니다.");
        }

        UUID runId = UUID.randomUUID();
        announcementSourceDao.insertCollectionRun(new AnnouncementSourceCollectionRunCommand(
                runId,
                requestId,
                "RUNNING",
                0,
                0,
                0,
                0,
                0,
                0,
                null
        ));

        RunCounter counter = new RunCounter();
        String errorMessage = null;
        try {
            AnnouncementSourceProviderBatch batch = "LOCAL_GOV_NOTICE".equals(request.providerCode())
                    ? providerClient.selectSourceBatch(request, runId)
                    : AnnouncementSourceProviderBatch.success(providerClient.selectSourceItemList(request));
            counter.totalCount = batch.items().size() + batch.failedCount();
            counter.failedCount = batch.failedCount();
            errorMessage = batch.errorMessage();
            for (AnnouncementSourceProviderItem item : batch.items()) {
                handleProviderItem(runId, item, counter);
            }
        } catch (RuntimeException exception) {
            counter.failedCount++;
            errorMessage = exception.getMessage();
        }

        String runStatusCode = selectRunStatusCode(counter, errorMessage);
        announcementSourceDao.updateCollectionRunResult(new AnnouncementSourceCollectionRunCommand(
                runId,
                requestId,
                runStatusCode,
                counter.totalCount,
                counter.collectedCount,
                counter.skippedEndedCount,
                counter.duplicateCount,
                counter.failedCount,
                counter.excludedCount,
                errorMessage
        ));
        return AnnouncementSourceCollectionRunResponse.from(selectCollectionRunRow(runId));
    }

    /**
     * provider item을 저장합니다.
     *
     * @param runId 입력 값
     *
     * @param item 입력 값
     *
     * @param counter 입력 값
     */
    private void handleProviderItem(UUID runId, AnnouncementSourceProviderItem item, RunCounter counter) {
        if ("EXCLUDED".equals(item.semanticStatusCode())) {
            counter.excludedCount++;
            insertRunItem(runId, null, item, "EXCLUDED", null);
            updateProviderItemResult(runId, item, "EXCLUDED");
            return;
        }
        if (item.applicationEndDate() != null && item.applicationEndDate().isBefore(LocalDate.now())) {
            counter.skippedEndedCount++;
            insertRunItem(runId, null, item, "SKIPPED_ENDED", null);
            updateProviderItemResult(runId, item, "SKIPPED_ENDED");
            return;
        }

        AnnouncementSourceSnapshotRow duplicate = selectDuplicateSource(item);
        if (duplicate != null) {
            counter.duplicateCount++;
            insertRunItem(runId, duplicate.sourceId(), item, "DUPLICATE", null);
            updateProviderItemResult(runId, item, "DUPLICATE");
            return;
        }

        String canonicalSourceUrl = identityNormalizer.canonicalizeUrl(item.sourceUrl());
        String normalizedTitle = identityNormalizer.normalizeText(item.title());
        String normalizedAgencyName = identityNormalizer.normalizeText(item.agencyName());
        LocalDate postedDate = item.postedAt() == null ? null : item.postedAt().toLocalDate();
        AnnouncementSourceSnapshotRow exactDuplicate = announcementSourceDao.selectExactSourceAcrossProviders(
                item.providerCode(), canonicalSourceUrl, normalizedTitle, normalizedAgencyName, postedDate
        );

        UUID sourceId = UUID.randomUUID();
        String reviewStatusCode = exactDuplicate == null ? "REVIEW_PENDING" : "DUPLICATE";
        announcementSourceDao.insertSourceSnapshot(new AnnouncementSourceSnapshotCommand(
                sourceId,
                item.providerCode(),
                item.providerNoticeId(),
                item.title(),
                item.agencyName(),
                item.applicationStartDate(),
                item.applicationEndDate(),
                item.postedAt(),
                item.modifiedAt(),
                item.sourceUrl(),
                item.bodyText(),
                item.inquiryText(),
                item.applicationMethodText(),
                item.sourceCompletenessCode(),
                item.missingFieldsJson(),
                item.rawPayloadJson(),
                item.rawHash(),
                reviewStatusCode,
                item.localGovernmentSourceId(),
                canonicalSourceUrl,
                normalizedTitle,
                normalizedAgencyName,
                postedDate,
                item.semanticStatusCode(),
                item.semanticReasonCode(),
                item.semanticMatchedKeywords()
        ));
        int sortOrder = 0;
        for (AnnouncementSourceProviderAttachment attachment : item.attachments()) {
            announcementSourceDao.insertSourceAttachment(new AnnouncementSourceAttachmentCommand(
                    UUID.randomUUID(),
                    sourceId,
                    attachment.fileName(),
                    attachment.fileUrl(),
                    attachment.fileTypeCode(),
                    sortOrder++
            ));
        }
        for (AnnouncementSourceHighlightCommand highlight : highlightService.selectHighlightList(
                sourceId,
                item.bodyText(),
                item.inquiryText(),
                item.applicationMethodText()
        )) {
            announcementSourceDao.insertSourceHighlight(highlight);
        }
        if (exactDuplicate != null) {
            insertCrossProviderDuplicate(sourceId, exactDuplicate, "EXACT_DUPLICATE", "AUTO_CONFIRMED");
            counter.duplicateCount++;
            insertRunItem(runId, sourceId, item, "DUPLICATE", null);
            updateProviderItemResult(runId, item, "DUPLICATE");
            return;
        }
        for (AnnouncementSourceSnapshotRow similar : announcementSourceDao.selectSimilarSourceAcrossProvidersList(sourceId)) {
            insertCrossProviderDuplicate(sourceId, similar, "SIMILAR", "PENDING");
        }
        insertDuplicateCandidates(sourceId);
        counter.collectedCount++;
        insertRunItem(runId, sourceId, item, "COLLECTED", null);
        updateProviderItemResult(runId, item, "COLLECTED");
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param requestId 입력 값
     *
     * @param runStatusCode 입력 값
     *
     * @param page 입력 값
     *
     * @param size 입력 값
     *
     * @return 처리 결과
     */
    @Override
    public PageResponse<AnnouncementSourceCollectionRunResponse> selectCollectionRunList(
            UUID requestId,
            String runStatusCode,
            int page,
            int size
    ) {
        AnnouncementSourceCollectionRunSearchCondition condition = new AnnouncementSourceCollectionRunSearchCondition(
                requestId,
                normalizeOptionalCode(runStatusCode),
                size,
                (page - 1) * size
        );
        long totalCount = announcementSourceDao.selectCollectionRunCount(condition);
        List<AnnouncementSourceCollectionRunResponse> items = announcementSourceDao.selectCollectionRunList(condition)
                .stream()
                .map(AnnouncementSourceCollectionRunResponse::from)
                .toList();
        return PageResponse.of(items, page, size, totalCount);
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param runId 입력 값
     *
     * @return 처리 결과
     */
    @Override
    public AnnouncementSourceCollectionRunDetailsResponse selectCollectionRunDetails(UUID runId) {
        AnnouncementSourceCollectionRunResponse run = AnnouncementSourceCollectionRunResponse.from(selectCollectionRunRow(runId));
        List<AnnouncementSourceCollectionRunItemResponse> items = announcementSourceDao.selectCollectionRunItemList(runId)
                .stream()
                .map(AnnouncementSourceCollectionRunItemResponse::from)
                .toList();
        List<LocalGovernmentNoticeCollectionResultResponse> sourceResults =
                localGovernmentNoticeDao.selectCollectionResultListByRunId(runId).stream()
                        .map(LocalGovernmentNoticeCollectionResultResponse::from)
                        .toList();
        return new AnnouncementSourceCollectionRunDetailsResponse(run, items, sourceResults);
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param providerCode 입력 값
     *
     * @param reviewStatusCode 입력 값
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
    public PageResponse<AnnouncementSourceSummaryResponse> selectSourceList(
            String providerCode,
            String reviewStatusCode,
            String semanticStatusCode,
            String keyword,
            int page,
            int size
    ) {
        AnnouncementSourceSearchCondition condition = new AnnouncementSourceSearchCondition(
                normalizeOptionalCode(providerCode),
                normalizeOptionalCode(reviewStatusCode),
                normalizeOptionalCode(semanticStatusCode),
                nullIfBlank(keyword),
                size,
                (page - 1) * size
        );
        long totalCount = announcementSourceDao.selectSourceCount(condition);
        List<AnnouncementSourceSummaryResponse> items = announcementSourceDao.selectSourceList(condition)
                .stream()
                .map(AnnouncementSourceSummaryResponse::from)
                .toList();
        return PageResponse.of(items, page, size, totalCount);
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param sourceId 입력 값
     *
     * @return 처리 결과
     */
    @Override
    public AnnouncementSourceDetailsResponse selectSourceDetails(UUID sourceId) {
        AnnouncementSourceSnapshotRow source = selectSourceRow(sourceId);
        return AnnouncementSourceDetailsResponse.from(
                source,
                announcementSourceDao.selectSourceAttachmentList(sourceId).stream()
                        .map(AnnouncementSourceAttachmentResponse::from)
                        .toList(),
                announcementSourceDao.selectSourceHighlightList(sourceId).stream()
                        .map(AnnouncementSourceHighlightResponse::from)
                        .toList(),
                announcementSourceDao.selectDuplicateCandidateList(sourceId).stream()
                        .map(AnnouncementSourceDuplicateCandidateResponse::from)
                        .toList(),
                announcementSourceDao.selectSnapshotDuplicateList(sourceId).stream()
                        .map(AnnouncementSourceSnapshotDuplicateResponse::from)
                        .toList()
        );
    }

    /**
     * 업무 데이터를 수정합니다.
     *
     * @param authentication 입력 값
     *
     * @param sourceId 입력 값
     *
     * @param request 입력 값
     *
     * @return 처리 결과
     */
    @Override
    @Transactional
    public AnnouncementSourceDetailsResponse updateSourceReviewStatus(
            Authentication authentication,
            UUID sourceId,
            AnnouncementSourceReviewStatusUpdateRequest request
    ) {
        UUID actorUserId = selectActorUserId(authentication);
        AnnouncementSourceSnapshotRow source = selectSourceRow(sourceId);
        String nextStatusCode = normalizeRequiredCode("reviewStatusCode", request.reviewStatusCode(), REVIEW_STATUS_CODES);
        validateReviewStatusTransition(source, nextStatusCode);
        int updated = announcementSourceDao.updateSourceReviewStatus(new AnnouncementSourceReviewStatusCommand(sourceId, nextStatusCode));
        if (updated == 0) {
            throw notFound("수집 원문을 찾을 수 없습니다.");
        }
        announcementSourceDao.insertSourceReviewHistory(new AnnouncementSourceReviewHistoryCommand(
                UUID.randomUUID(),
                sourceId,
                source.reviewStatusCode(),
                nextStatusCode,
                nullIfBlank(request.reason()),
                actorUserId
        ));
        insertAudit(actorUserId, "ANNOUNCEMENT_SOURCE_REVIEW_STATUS_UPDATE", "ANNOUNCEMENT_SOURCE", sourceId,
                "{\"previousStatusCode\":\"" + source.reviewStatusCode() + "\",\"nextStatusCode\":\"" + nextStatusCode + "\"}");
        return selectSourceDetails(sourceId);
    }

    /**
     * 업무 데이터를 수정합니다.
     *
     * @param authentication 입력 값
     *
     * @param sourceId 입력 값
     *
     * @param candidateId 입력 값
     *
     * @param request 입력 값
     *
     * @return 처리 결과
     */
    @Override
    @Transactional
    public AnnouncementSourceDetailsResponse updateDuplicateCandidateDecision(
            Authentication authentication,
            UUID sourceId,
            UUID candidateId,
            AnnouncementSourceDuplicateDecisionRequest request
    ) {
        UUID actorUserId = selectActorUserId(authentication);
        AnnouncementSourceSnapshotRow source = selectSourceRow(sourceId);
        AnnouncementSourceDuplicateCandidateRow candidate = selectDuplicateCandidateRow(sourceId, candidateId);
        String actionCode = normalizeRequiredCode(
                "decisionActionCode",
                request == null ? null : request.decisionActionCode(),
                DUPLICATE_DECISION_ACTION_CODES
        );

        if ("UPDATE_EXISTING".equals(actionCode)) {
            updateExistingAnnouncementFromSource(actorUserId, source, candidate, request);
            updateDuplicateDecisionOnly(
                    sourceId,
                    candidateId,
                    "UPDATE_EXISTING_SELECTED",
                    actorUserId,
                    defaultIfBlank(request.decisionNote(), "기존 운영 공고 업데이트를 선택했습니다.")
            );
            String nextStatusCode = "APPROVED".equals(candidate.approvalStatusCode())
                    && "NORMAL".equals(candidate.manualStatusCode()) ? "ACTIVATED" : "REVIEW_COMPLETED";
            announcementSourceDao.updateSourceReviewStatus(new AnnouncementSourceReviewStatusCommand(sourceId, nextStatusCode));
            announcementSourceDao.insertSourceReviewHistory(new AnnouncementSourceReviewHistoryCommand(
                    UUID.randomUUID(),
                    sourceId,
                    source.reviewStatusCode(),
                    nextStatusCode,
                    "중복 후보 검수 후 기존 운영 공고를 업데이트했습니다.",
                    actorUserId
            ));
            insertAudit(actorUserId, "ANNOUNCEMENT_SOURCE_DUPLICATE_DECISION", "ANNOUNCEMENT_SOURCE", sourceId,
                    "{\"decisionStatusCode\":\"UPDATE_EXISTING_SELECTED\",\"nextStatusCode\":\"" + nextStatusCode + "\"}");
            return selectSourceDetails(sourceId);
        }

        String decisionStatusCode = selectDecisionStatusCode(actionCode);
        String decisionNote = defaultIfBlank(request.decisionNote(), selectDefaultDecisionNote(actionCode));
        updateDuplicateDecisionOnly(sourceId, candidateId, decisionStatusCode, actorUserId, decisionNote);
        insertAudit(actorUserId, "ANNOUNCEMENT_SOURCE_DUPLICATE_DECISION", "ANNOUNCEMENT_SOURCE", sourceId,
                "{\"decisionStatusCode\":\"" + decisionStatusCode + "\"}");
        return selectSourceDetails(sourceId);
    }

    /**
     * 교차 제공자 유사 후보의 운영자 결정을 저장합니다.
     *
     * @param authentication 인증 정보
     * @param sourceId 원문 식별자
     * @param duplicateId 교차 중복 관계 식별자
     * @param request 결정 요청
     * @return 원문 상세
     */
    @Override
    @Transactional
    public AnnouncementSourceDetailsResponse updateSnapshotDuplicateDecision(
            Authentication authentication,
            UUID sourceId,
            UUID duplicateId,
            AnnouncementSourceDuplicateDecisionRequest request
    ) {
        selectSourceRow(sourceId);
        String actionCode = normalizeRequiredCode(
                "decisionActionCode", request == null ? null : request.decisionActionCode(), DUPLICATE_DECISION_ACTION_CODES
        );
        String decisionStatusCode = selectDecisionStatusCode(actionCode);
        int updated = announcementSourceDao.updateSnapshotDuplicateDecision(
                new AnnouncementSourceSnapshotDuplicateDecisionCommand(
                        sourceId, duplicateId, decisionStatusCode, selectActorUserId(authentication),
                        defaultIfBlank(request.decisionNote(), selectDefaultDecisionNote(actionCode))
                )
        );
        if (updated == 0) {
            throw new ApiException(
                    ErrorCode.INVALID_STATUS_TRANSITION,
                    HttpStatus.BAD_REQUEST,
                    "운영자 판단 대기 상태의 교차 제공자 유사 후보만 처리할 수 있습니다."
            );
        }
        insertAudit(selectActorUserId(authentication), "ANNOUNCEMENT_SOURCE_DUPLICATE_DECISION", "ANNOUNCEMENT_SOURCE", sourceId,
                "{\"decisionStatusCode\":\"" + decisionStatusCode + "\"}");
        return selectSourceDetails(sourceId);
    }

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param authentication 입력 값
     *
     * @param sourceId 입력 값
     *
     * @param request 입력 값
     *
     * @return 처리 결과
     */
    @Override
    @Transactional
    public AnnouncementSourceLinkResponse insertOperationalAnnouncement(
            Authentication authentication,
            UUID sourceId,
            AnnouncementSourceToAnnouncementRequest request
    ) {
        UUID actorUserId = selectActorUserId(authentication);
        AnnouncementSourceSnapshotRow source = selectSourceRow(sourceId);
        if ("EXCLUDED".equals(source.semanticStatusCode())) {
            throw new ApiException(
                    ErrorCode.INVALID_STATUS_TRANSITION,
                    HttpStatus.BAD_REQUEST,
                    "지원사업과 무관한 것으로 분류된 원문은 운영 공고로 전환할 수 없습니다."
            );
        }
        if (!Set.of("REVIEW_PENDING", "CONDITION_INPUT_REQUIRED", "REVIEW_COMPLETED")
                .contains(source.reviewStatusCode())) {
            throw new ApiException(
                    ErrorCode.INVALID_STATUS_TRANSITION,
                    HttpStatus.BAD_REQUEST,
                    "검수 가능한 신규 원문만 운영 공고 초안으로 전환할 수 있습니다."
            );
        }
        if (announcementSourceDao.selectPendingDuplicateCandidateCount(sourceId) > 0
                || announcementSourceDao.selectPendingSnapshotDuplicateCount(sourceId) > 0) {
            throw new ApiException(
                    ErrorCode.INVALID_STATUS_TRANSITION,
                    HttpStatus.BAD_REQUEST,
                    "중복 또는 유사 공고 후보를 먼저 검수한 뒤 신규 등록 여부를 선택하세요."
            );
        }
        UUID announcementId = UUID.randomUUID();
        String targetTypeCode = defaultIfBlank(request == null ? null : request.targetTypeCode(), DEFAULT_TARGET_TYPE_CODE);
        String incomeJudgementCode = defaultIfBlank(
                request == null ? null : request.incomeJudgementCode(),
                DEFAULT_INCOME_JUDGEMENT_CODE
        );
        announcementDao.insertAnnouncement(new AnnouncementSaveCommand(
                announcementId,
                targetTypeCode,
                source.title(),
                defaultIfBlank(source.agencyName(), "기관 미확인"),
                source.bodyText(),
                source.applicationStartDate(),
                source.applicationEndDate(),
                incomeJudgementCode,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                actorUserId
        ));
        announcementSourceDao.insertSourceLink(new AnnouncementSourceLinkCommand(
                UUID.randomUUID(),
                sourceId,
                announcementId,
                actorUserId
        ));
        announcementSourceDao.updateSourceReviewStatus(new AnnouncementSourceReviewStatusCommand(sourceId, "CONDITION_INPUT_REQUIRED"));
        announcementSourceDao.insertSourceReviewHistory(new AnnouncementSourceReviewHistoryCommand(
                UUID.randomUUID(),
                sourceId,
                source.reviewStatusCode(),
                "CONDITION_INPUT_REQUIRED",
                "운영 공고 DRAFT로 전환했습니다.",
                actorUserId
        ));
        insertAudit(actorUserId, "ANNOUNCEMENT_SOURCE_DRAFT_CREATE", "ANNOUNCEMENT_SOURCE", sourceId,
                "{\"announcementId\":\"" + announcementId + "\",\"nextStatusCode\":\"CONDITION_INPUT_REQUIRED\"}");
        AnnouncementDetailsRow announcement = announcementDao.selectAnnouncementDetails(announcementId);
        return new AnnouncementSourceLinkResponse(sourceId, source.publicCode(), announcementId, announcement.announcementCode());
    }

    /**
     * 중복/유사 후보를 저장합니다.
     *
     * @param sourceId 입력 값
     */
    private void insertDuplicateCandidates(UUID sourceId) {
        for (AnnouncementSourceDuplicateCandidateRow candidate :
                announcementSourceDao.selectActiveAnnouncementDuplicateCandidateList(sourceId)) {
            announcementSourceDao.insertDuplicateCandidate(new AnnouncementSourceDuplicateCandidateCommand(
                    UUID.randomUUID(),
                    sourceId,
                    candidate.announcementId(),
                    candidate.matchTypeCode(),
                    candidate.titleMatched(),
                    candidate.agencyMatched(),
                    candidate.providerNoticeMatched(),
                    candidate.periodMatched(),
                    candidate.sourceUrlMatched(),
                    candidate.similarityReason()
            ));
        }
    }

    /**
     * 교차 제공자 중복 또는 유사 관계를 canonical UUID 순서로 저장합니다.
     *
     * @param sourceId 새 원문 식별자
     * @param candidate 비교 원문
     * @param matchTypeCode 일치 유형
     * @param decisionStatusCode 결정 상태
     */
    private void insertCrossProviderDuplicate(
            UUID sourceId,
            AnnouncementSourceSnapshotRow candidate,
            String matchTypeCode,
            String decisionStatusCode
    ) {
        AnnouncementSourceSnapshotRow source = selectSourceRow(sourceId);
        boolean titleMatched = identityNormalizer.normalizeText(source.title())
                .equals(identityNormalizer.normalizeText(candidate.title()));
        boolean agencyMatched = identityNormalizer.normalizeText(source.agencyName())
                .equals(identityNormalizer.normalizeText(candidate.agencyName()));
        LocalDate sourcePostedDate = source.postedAt() == null ? null : source.postedAt().toLocalDate();
        LocalDate candidatePostedDate = candidate.postedAt() == null ? null : candidate.postedAt().toLocalDate();
        boolean postedDateMatched = Objects.equals(sourcePostedDate, candidatePostedDate);
        boolean sourceUrlMatched = Objects.equals(
                identityNormalizer.canonicalizeUrl(source.sourceUrl()),
                identityNormalizer.canonicalizeUrl(candidate.sourceUrl())
        );
        String reason = "EXACT_DUPLICATE".equals(matchTypeCode)
                ? "원문 URL 또는 제목·기관·등록일이 다른 제공자 원문과 일치합니다."
                : "제목과 기관이 같지만 등록일 또는 원문 URL이 달라 운영자 확인이 필요합니다.";
        announcementSourceDao.insertSnapshotDuplicate(new AnnouncementSourceSnapshotDuplicateCommand(
                UUID.randomUUID(), sourceId, candidate.sourceId(), matchTypeCode, titleMatched, agencyMatched,
                postedDateMatched, sourceUrlMatched, reason, decisionStatusCode
        ));
    }

    /**
     * 중복/유사 후보 row를 조회합니다.
     *
     * @param sourceId 입력 값
     *
     * @param candidateId 입력 값
     *
     * @return 처리 결과
     */
    private AnnouncementSourceDuplicateCandidateRow selectDuplicateCandidateRow(UUID sourceId, UUID candidateId) {
        AnnouncementSourceDuplicateCandidateRow row = announcementSourceDao.selectDuplicateCandidateDetails(sourceId, candidateId);
        if (row == null) {
            throw notFound("중복 또는 유사 공고 후보를 찾을 수 없습니다.");
        }
        return row;
    }

    /**
     * 후보 결정 상태를 수정합니다.
     *
     * @param sourceId 입력 값
     *
     * @param candidateId 입력 값
     *
     * @param decisionStatusCode 입력 값
     *
     * @param actorUserId 입력 값
     *
     * @param decisionNote 입력 값
     */
    private void updateDuplicateDecisionOnly(
            UUID sourceId,
            UUID candidateId,
            String decisionStatusCode,
            UUID actorUserId,
            String decisionNote
    ) {
        int updated = announcementSourceDao.updateDuplicateCandidateDecision(new AnnouncementSourceDuplicateDecisionCommand(
                candidateId,
                sourceId,
                decisionStatusCode,
                actorUserId,
                nullIfBlank(decisionNote)
        ));
        if (updated == 0) {
            throw new ApiException(ErrorCode.INVALID_STATUS_TRANSITION, HttpStatus.BAD_REQUEST, "검수 대기 상태의 후보만 처리할 수 있습니다.");
        }
    }

    /**
     * 기존 운영 공고를 수집 원문 기준으로 갱신합니다.
     *
     * @param actorUserId 입력 값
     *
     * @param source 입력 값
     *
     * @param candidate 입력 값
     *
     * @param request 입력 값
     */
    private void updateExistingAnnouncementFromSource(
            UUID actorUserId,
            AnnouncementSourceSnapshotRow source,
            AnnouncementSourceDuplicateCandidateRow candidate,
            AnnouncementSourceDuplicateDecisionRequest request
    ) {
        AnnouncementDetailsRow announcement = announcementDao.selectAnnouncementDetails(candidate.announcementId());
        if (announcement == null) {
            throw notFound("업데이트할 운영 공고를 찾을 수 없습니다.");
        }
        int updated = announcementDao.updateAnnouncement(new AnnouncementSaveCommand(
                candidate.announcementId(),
                defaultIfBlank(request.targetTypeCode(), announcement.targetTypeCode()),
                defaultIfBlank(source.title(), announcement.title()),
                defaultIfBlank(source.agencyName(), announcement.agencyName()),
                defaultIfBlank(source.bodyText(), announcement.summary()),
                source.applicationStartDate() == null ? announcement.applicationStartDate() : source.applicationStartDate(),
                source.applicationEndDate() == null ? announcement.applicationEndDate() : source.applicationEndDate(),
                defaultIfBlank(request.incomeJudgementCode(), announcement.incomeJudgementCode()),
                announcement.minAmount(),
                announcement.maxAmount(),
                actorUserId
        ));
        if (updated == 0) {
            throw notFound("업데이트할 운영 공고를 찾을 수 없습니다.");
        }
        announcementSourceDao.insertSourceLink(new AnnouncementSourceLinkCommand(
                UUID.randomUUID(),
                source.sourceId(),
                candidate.announcementId(),
                actorUserId
        ));
    }

    /**
     * 결정 action을 저장 상태로 변환합니다.
     *
     * @param actionCode 입력 값
     *
     * @return 처리 결과
     */
    private String selectDecisionStatusCode(String actionCode) {
        if ("CREATE_NEW".equals(actionCode)) {
            return "CREATE_NEW_SELECTED";
        }
        if ("IGNORE".equals(actionCode)) {
            return "IGNORED";
        }
        return "UPDATE_EXISTING_SELECTED";
    }

    /**
     * 기본 결정 사유를 반환합니다.
     *
     * @param actionCode 입력 값
     *
     * @return 처리 결과
     */
    private String selectDefaultDecisionNote(String actionCode) {
        if ("CREATE_NEW".equals(actionCode)) {
            return "신규 운영 공고 등록을 선택했습니다.";
        }
        if ("IGNORE".equals(actionCode)) {
            return "후보 무시를 선택했습니다.";
        }
        return "기존 운영 공고 업데이트를 선택했습니다.";
    }

    /**
     * 중복 source를 조회합니다.
     *
     * @param item 입력 값
     *
     * @return 처리 결과
     */
    private AnnouncementSourceSnapshotRow selectDuplicateSource(AnnouncementSourceProviderItem item) {
        if (item.providerNoticeId() != null && !item.providerNoticeId().isBlank()) {
            AnnouncementSourceSnapshotRow row = announcementSourceDao.selectSourceByProviderNoticeId(
                    item.providerCode(),
                    item.providerNoticeId()
            );
            if (row != null) {
                return row;
            }
        }
        if (item.sourceUrl() != null && !item.sourceUrl().isBlank()) {
            AnnouncementSourceSnapshotRow row = announcementSourceDao.selectSourceByUrl(item.providerCode(), item.sourceUrl());
            if (row != null) {
                return row;
            }
        }
        return announcementSourceDao.selectSourceByRawHash(item.providerCode(), item.rawHash());
    }

    /**
     * 실행 항목을 등록합니다.
     *
     * @param runId 입력 값
     *
     * @param sourceId 입력 값
     *
     * @param item 입력 값
     *
     * @param statusCode 입력 값
     *
     * @param errorMessage 입력 값
     */
    private void insertRunItem(
            UUID runId,
            UUID sourceId,
            AnnouncementSourceProviderItem item,
            String statusCode,
            String errorMessage
    ) {
        announcementSourceDao.insertCollectionRunItem(new AnnouncementSourceCollectionRunItemCommand(
                UUID.randomUUID(),
                runId,
                sourceId,
                item.providerNoticeId(),
                item.sourceUrl(),
                statusCode,
                item.semanticReasonCode(),
                item.semanticMatchedKeywords(),
                errorMessage
        ));
    }

    /**
     * provider별 URL 수집 결과에 통합 저장 결과를 반영합니다.
     *
     * @param runId 수집 실행 식별자
     * @param item 수집 공고
     * @param itemStatusCode 통합 저장 결과
     */
    private void updateProviderItemResult(
            UUID runId,
            AnnouncementSourceProviderItem item,
            String itemStatusCode
    ) {
        AnnouncementSourceProviderClient providerClient = providerClients.get(item.providerCode());
        if (providerClient != null) {
            providerClient.updateItemResult(runId, item, itemStatusCode);
        }
    }

    /**
     * 개인정보 원문 없이 공고 수집 운영 감사 로그를 저장합니다.
     *
     * @param actorUserId 처리 사용자 식별자
     * @param actionCode 작업 코드
     * @param resourceType 대상 유형
     * @param resourceId 대상 식별자
     * @param metadataJson 비식별 상태 메타데이터
     */
    private void insertAudit(
            UUID actorUserId,
            String actionCode,
            String resourceType,
            UUID resourceId,
            String metadataJson
    ) {
        announcementSourceDao.insertAuditLog(new AnnouncementSourceAuditLogCommand(
                actorUserId, actionCode, resourceType, resourceId, "SUCCESS", metadataJson
        ));
    }

    /**
     * 수집 원문 검수 상태 전이와 활성화 조건을 검증합니다.
     *
     * @param source 현재 원문
     * @param nextStatusCode 변경할 상태
     */
    private void validateReviewStatusTransition(
            AnnouncementSourceSnapshotRow source,
            String nextStatusCode
    ) {
        if ("EXCLUDED".equals(source.semanticStatusCode()) && !"ARCHIVED".equals(nextStatusCode)) {
            throw new ApiException(
                    ErrorCode.INVALID_STATUS_TRANSITION,
                    HttpStatus.BAD_REQUEST,
                    "수집 제외 판정 원문은 보관 처리만 할 수 있습니다."
            );
        }
        boolean valid = switch (source.reviewStatusCode()) {
            case "COLLECTED" -> Set.of("REVIEW_PENDING", "DUPLICATE", "SKIPPED_ENDED", "ARCHIVED").contains(nextStatusCode);
            case "REVIEW_PENDING" -> Set.of("CONDITION_INPUT_REQUIRED", "REVIEW_COMPLETED", "DUPLICATE", "ARCHIVED").contains(nextStatusCode);
            case "CONDITION_INPUT_REQUIRED" -> Set.of("REVIEW_COMPLETED", "ARCHIVED").contains(nextStatusCode);
            case "REVIEW_COMPLETED" -> Set.of("CONDITION_INPUT_REQUIRED", "ACTIVATED", "ARCHIVED").contains(nextStatusCode);
            case "ACTIVATED" -> "ARCHIVED".equals(nextStatusCode);
            case "DUPLICATE", "SKIPPED_ENDED" -> "ARCHIVED".equals(nextStatusCode);
            default -> false;
        };
        if (!valid) {
            throw new ApiException(
                    ErrorCode.INVALID_STATUS_TRANSITION,
                    HttpStatus.BAD_REQUEST,
                    "현재 검수 상태에서 선택한 상태로 변경할 수 없습니다."
            );
        }
        if ("ACTIVATED".equals(nextStatusCode)
                && announcementSourceDao.selectApprovedLinkedAnnouncementCount(source.sourceId()) == 0) {
            throw new ApiException(
                    ErrorCode.INVALID_STATUS_TRANSITION,
                    HttpStatus.BAD_REQUEST,
                    "승인되어 정상 노출 중인 운영 공고와 연결된 원문만 활성 처리할 수 있습니다."
            );
        }
    }

    /**
     * 실행 상태를 산출합니다.
     *
     * @param counter 입력 값
     *
     * @param errorMessage 입력 값
     *
     * @return 처리 결과
     */
    private String selectRunStatusCode(RunCounter counter, String errorMessage) {
        if (errorMessage != null && counter.collectedCount == 0 && counter.duplicateCount == 0
                && counter.skippedEndedCount == 0 && counter.excludedCount == 0) {
            return "FAILED";
        }
        if (counter.failedCount > 0) {
            return "PARTIAL_FAILED";
        }
        return "COMPLETED";
    }

    /**
     * 수집 요청 row를 조회합니다.
     *
     * @param requestId 입력 값
     *
     * @return 처리 결과
     */
    private AnnouncementSourceCollectionRequestRow selectCollectionRequestRow(UUID requestId) {
        AnnouncementSourceCollectionRequestRow row = announcementSourceDao.selectCollectionRequestDetails(requestId);
        if (row == null) {
            throw notFound("수집 요청을 찾을 수 없습니다.");
        }
        return row;
    }

    /**
     * 수집 실행 row를 조회합니다.
     *
     * @param runId 입력 값
     *
     * @return 처리 결과
     */
    private AnnouncementSourceCollectionRunRow selectCollectionRunRow(UUID runId) {
        AnnouncementSourceCollectionRunRow row = announcementSourceDao.selectCollectionRunDetails(runId);
        if (row == null) {
            throw notFound("수집 실행 이력을 찾을 수 없습니다.");
        }
        return row;
    }

    /**
     * 수집 원문 row를 조회합니다.
     *
     * @param sourceId 입력 값
     *
     * @return 처리 결과
     */
    private AnnouncementSourceSnapshotRow selectSourceRow(UUID sourceId) {
        AnnouncementSourceSnapshotRow row = announcementSourceDao.selectSourceDetails(sourceId);
        if (row == null) {
            throw notFound("수집 원문을 찾을 수 없습니다.");
        }
        return row;
    }

    /**
     * 인증 사용자 ID를 조회합니다.
     *
     * @param authentication 입력 값
     *
     * @return 처리 결과
     */
    private UUID selectActorUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ApiException(ErrorCode.AUTH_REQUIRED, HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
        }
        if (authentication.getPrincipal() instanceof AuthenticatedUserDetails principal) {
            return principal.userId();
        }
        throw new ApiException(ErrorCode.AUTH_REQUIRED, HttpStatus.UNAUTHORIZED, "인증 사용자 정보를 확인할 수 없습니다.");
    }

    /**
     * 코드를 검증하고 정규화합니다.
     *
     * @param fieldName 입력 값
     *
     * @param value 입력 값
     *
     * @param allowedCodes 입력 값
     *
     * @return 처리 결과
     */
    private String normalizeRequiredCode(String fieldName, String value, Set<String> allowedCodes) {
        String normalized = normalizeOptionalCode(value);
        if (normalized == null || !allowedCodes.contains(normalized)) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST, fieldName + " 값이 올바르지 않습니다.");
        }
        return normalized;
    }

    /**
     * 선택 코드 값을 정규화합니다.
     *
     * @param value 입력 값
     *
     * @return 처리 결과
     */
    private String normalizeOptionalCode(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase();
    }

    /**
     * 공백 값을 null로 변환합니다.
     *
     * @param value 입력 값
     *
     * @return 처리 결과
     */
    private String nullIfBlank(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    /**
     * 기본 값을 적용합니다.
     *
     * @param value 입력 값
     *
     * @param defaultValue 입력 값
     *
     * @return 처리 결과
     */
    private String defaultIfBlank(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim();
    }

    /**
     * 리소스 미존재 예외를 생성합니다.
     *
     * @param message 입력 값
     *
     * @return 처리 결과
     */
    private ApiException notFound(String message) {
        return new ApiException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, message);
    }

    private static class RunCounter {
        private int totalCount;
        private int collectedCount;
        private int skippedEndedCount;
        private int duplicateCount;
        private int failedCount;
        private int excludedCount;
    }
}
