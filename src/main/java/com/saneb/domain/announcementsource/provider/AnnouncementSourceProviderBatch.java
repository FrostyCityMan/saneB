/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: AnnouncementSourceProviderBatch.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.announcementsource.provider;

import java.util.List;

public record AnnouncementSourceProviderBatch(
        List<AnnouncementSourceProviderItem> items,
        int failedCount,
        String errorMessage
) {

    /**
     * 오류 없는 provider 배치 결과를 생성합니다.
     *
     * @param items 수집 공고 목록
     * @return provider 배치 결과
     */
    public static AnnouncementSourceProviderBatch success(List<AnnouncementSourceProviderItem> items) {
        return new AnnouncementSourceProviderBatch(List.copyOf(items), 0, null);
    }
}
