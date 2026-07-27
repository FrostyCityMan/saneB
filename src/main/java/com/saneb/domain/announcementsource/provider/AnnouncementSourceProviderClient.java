/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: AnnouncementSourceProviderClient.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.announcementsource.provider;

import com.saneb.domain.announcementsource.vo.AnnouncementSourceCollectionRequestRow;
import java.util.List;
import java.util.UUID;

public interface AnnouncementSourceProviderClient {

    /**
     * 외부 제공자 호출에 필요한 설정이 준비됐는지 확인합니다.
     *
     * @return 호출 가능하면 true
     */
    default boolean isConfigured() {
        return true;
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @return 처리 결과
     */
    String selectProviderCode();

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param request 입력 값
     *
     * @return 처리 결과
     */
    List<AnnouncementSourceProviderItem> selectSourceItemList(AnnouncementSourceCollectionRequestRow request);

    /**
     * 실행 식별자가 필요한 provider를 포함해 수집 배치를 조회합니다.
     *
     * @param request 승인된 수집 요청
     * @param runId 수집 실행 식별자
     * @return provider 배치 결과
     */
    default AnnouncementSourceProviderBatch selectSourceBatch(
            AnnouncementSourceCollectionRequestRow request,
            UUID runId
    ) {
        return AnnouncementSourceProviderBatch.success(selectSourceItemList(request));
    }

    /**
     * 통합 중복 검사 후 provider별 URL 결과 건수를 반영합니다.
     *
     * @param runId 수집 실행 식별자
     * @param item 수집 공고
     * @param itemStatusCode 통합 저장 결과
     */
    default void updateItemResult(UUID runId, AnnouncementSourceProviderItem item, String itemStatusCode) {
        // API provider는 URL 단위 집계가 없으므로 기본 구현에서 처리하지 않습니다.
    }
}
