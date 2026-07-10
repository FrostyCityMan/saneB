/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: LocalGovernmentNoticeAnnouncementSourceProviderClient.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.announcementsource.provider;

import com.saneb.domain.announcementsource.dao.LocalGovernmentNoticeDao;
import com.saneb.domain.announcementsource.localgov.collector.LocalGovernmentNoticeCollectionOutcome;
import com.saneb.domain.announcementsource.localgov.collector.LocalGovernmentNoticeCollector;
import com.saneb.domain.announcementsource.localgov.vo.LocalGovernmentNoticeCollectionResultCommand;
import com.saneb.domain.announcementsource.localgov.vo.LocalGovernmentNoticeParserProfileRow;
import com.saneb.domain.announcementsource.localgov.vo.LocalGovernmentNoticeSourceCollectionStatusCommand;
import com.saneb.domain.announcementsource.localgov.vo.LocalGovernmentNoticeSourceRow;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceCollectionRequestRow;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class LocalGovernmentNoticeAnnouncementSourceProviderClient implements AnnouncementSourceProviderClient {

    private static final String PROVIDER_CODE = "LOCAL_GOV_NOTICE";

    private final LocalGovernmentNoticeDao localGovernmentNoticeDao;
    private final LocalGovernmentNoticeCollector collector;

    /**
     * 지자체 공고 provider를 생성합니다.
     *
     * @param localGovernmentNoticeDao 지자체 공고 DAO
     * @param collector 안전한 HTML 수집기
     */
    public LocalGovernmentNoticeAnnouncementSourceProviderClient(
            LocalGovernmentNoticeDao localGovernmentNoticeDao,
            LocalGovernmentNoticeCollector collector
    ) {
        this.localGovernmentNoticeDao = localGovernmentNoticeDao;
        this.collector = collector;
    }

    /**
     * 지자체 공고 provider 코드를 반환합니다.
     *
     * @return LOCAL_GOV_NOTICE
     */
    @Override
    public String selectProviderCode() {
        return PROVIDER_CODE;
    }

    /**
     * 승인된 요청 범위의 지자체 공고를 조회합니다.
     *
     * @param request 승인된 수집 요청
     * @return 수집 공고 목록
     */
    @Override
    public List<AnnouncementSourceProviderItem> selectSourceItemList(AnnouncementSourceCollectionRequestRow request) {
        List<AnnouncementSourceProviderItem> items = new ArrayList<>();
        for (LocalGovernmentNoticeSourceRow source : selectTargetSources(request)) {
            LocalGovernmentNoticeParserProfileRow profile = localGovernmentNoticeDao.selectParserProfileDetails(
                    source.parserProfileCode()
            );
            items.addAll(collector.collect(source, profile).items());
        }
        return List.copyOf(items);
    }

    /**
     * URL별 결과를 보존하며 지자체 공고 배치를 수집합니다.
     *
     * @param request 승인된 수집 요청
     * @param runId 수집 실행 식별자
     * @return provider 배치 결과
     */
    @Override
    public AnnouncementSourceProviderBatch selectSourceBatch(
            AnnouncementSourceCollectionRequestRow request,
            UUID runId
    ) {
        List<AnnouncementSourceProviderItem> items = new ArrayList<>();
        int failedCount = 0;
        int failedSourceCount = 0;
        for (LocalGovernmentNoticeSourceRow source : selectTargetSources(request)) {
            LocalGovernmentNoticeParserProfileRow profile = localGovernmentNoticeDao.selectParserProfileDetails(
                    source.parserProfileCode()
            );
            LocalGovernmentNoticeCollectionOutcome outcome = collector.collect(source, profile);
            items.addAll(outcome.items());
            failedCount += outcome.failedCount();
            if (isFailedStatus(outcome.resultStatusCode())) {
                failedSourceCount++;
            }
            localGovernmentNoticeDao.insertCollectionResult(new LocalGovernmentNoticeCollectionResultCommand(
                    UUID.randomUUID(), runId, source.sourceId(), outcome.resultStatusCode(), outcome.discoveredCount(),
                    0, 0, outcome.failedCount(), outcome.httpStatus(), outcome.errorCode(), outcome.errorMessage()
            ));
            localGovernmentNoticeDao.updateSourceCollectionStatus(new LocalGovernmentNoticeSourceCollectionStatusCommand(
                    source.sourceId(), selectSourceStatus(outcome.resultStatusCode()), outcome.httpStatus(),
                    outcome.errorCode(), outcome.errorMessage(), outcome.etag(), outcome.lastModifiedValue(),
                    outcome.contentFingerprint(), !isFailedStatus(outcome.resultStatusCode())
            ));
        }
        String errorMessage = failedSourceCount > 0
                ? failedSourceCount + "개 기관 URL 수집에 실패했습니다. URL별 결과를 확인하세요."
                : null;
        return new AnnouncementSourceProviderBatch(List.copyOf(items), failedCount, errorMessage);
    }

    /**
     * 통합 중복 검사 결과를 URL 단위 집계에 반영합니다.
     *
     * @param runId 수집 실행 식별자
     * @param item 수집 공고
     * @param itemStatusCode 통합 저장 결과
     */
    @Override
    public void updateItemResult(UUID runId, AnnouncementSourceProviderItem item, String itemStatusCode) {
        if (item.localGovernmentSourceId() == null) {
            return;
        }
        int newIncrement = "COLLECTED".equals(itemStatusCode) ? 1 : 0;
        int duplicateIncrement = "DUPLICATE".equals(itemStatusCode) ? 1 : 0;
        localGovernmentNoticeDao.updateCollectionResultCounts(
                runId, item.localGovernmentSourceId(), newIncrement, duplicateIncrement
        );
    }

    /**
     * 요청이 지정한 단일 URL 또는 활성 URL 목록을 조회합니다.
     *
     * @param request 승인된 수집 요청
     * @return 수집 대상 URL 목록
     */
    private List<LocalGovernmentNoticeSourceRow> selectTargetSources(AnnouncementSourceCollectionRequestRow request) {
        int maxCount = request.maxCount() == null ? 100 : Math.max(1, request.maxCount());
        return localGovernmentNoticeDao.selectEnabledSourceList(request.localGovernmentSourceId(), maxCount);
    }

    /**
     * URL 상태 테이블에 저장할 상태를 선택합니다.
     *
     * @param resultStatusCode URL 단위 실행 결과
     * @return URL 관리 상태
     */
    private String selectSourceStatus(String resultStatusCode) {
        return "PARTIAL_FAILED".equals(resultStatusCode) ? "SUCCESS" : resultStatusCode;
    }

    /**
     * URL 실행 결과가 실패 계열인지 확인합니다.
     *
     * @param resultStatusCode URL 단위 실행 결과
     * @return 실패 계열이면 true
     */
    private boolean isFailedStatus(String resultStatusCode) {
        return switch (resultStatusCode) {
            case "FAILED", "URL_ERROR", "ACCESS_BLOCKED", "PARSER_UNSUPPORTED" -> true;
            default -> false;
        };
    }
}
