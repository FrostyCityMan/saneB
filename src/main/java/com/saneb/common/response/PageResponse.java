/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: PageResponse.java
 * 작성자: 김도훈
 *
 */

package com.saneb.common.response;

import java.util.List;

public record PageResponse<T>(
        List<T> items,
        int page,
        int size,
        long totalCount,
        int totalPages
) {

    /**
     * 업무 처리를 수행합니다.
     *
     * @param items 입력 값
     *
     * @param page 입력 값
     *
     * @param size 입력 값
     *
     * @param totalCount 입력 값
     *
     * @return 처리 결과
     */
    public static <T> PageResponse<T> of(List<T> items, int page, int size, long totalCount) {
        int totalPages = size <= 0 ? 0 : (int) Math.ceil((double) totalCount / size);
        return new PageResponse<>(items, page, size, totalCount, totalPages);
    }
}
