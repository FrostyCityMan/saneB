/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: StandardCodeServiceImpl.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.standardcode.service.impl;

import com.saneb.common.error.ApiException;
import com.saneb.common.error.ErrorCode;
import com.saneb.common.response.PageResponse;
import com.saneb.domain.standardcode.dao.StandardCodeDao;
import com.saneb.domain.standardcode.dto.StandardCodeGroupResponse;
import com.saneb.domain.standardcode.dto.StandardCodeResponse;
import com.saneb.domain.standardcode.service.StandardCodeService;
import com.saneb.domain.standardcode.vo.StandardCodeGroupRow;
import com.saneb.domain.standardcode.vo.StandardCodeRow;
import com.saneb.domain.standardcode.vo.StandardCodeSearchCondition;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class StandardCodeServiceImpl implements StandardCodeService {

    private static final int MAX_PAGE_SIZE = 100;

    private final StandardCodeDao standardCodeDao;

    /**
     * 객체를 생성합니다.
     *
     * @param standardCodeDao 입력 값
     */
    public StandardCodeServiceImpl(StandardCodeDao standardCodeDao) {
        this.standardCodeDao = standardCodeDao;
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @return 처리 결과
     */
    @Override
    public List<StandardCodeGroupResponse> selectStandardCodeGroupList() {
        return standardCodeDao.selectStandardCodeGroupList().stream()
                .map(this::toGroupResponse)
                .toList();
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param groupCode 입력 값
     *
     * @param keyword 입력 값
     *
     * @param active 입력 값
     *
     * @param page 입력 값
     *
     * @param size 입력 값
     *
     * @return 처리 결과
     */
    @Override
    public PageResponse<StandardCodeResponse> selectStandardCodeList(
            String groupCode,
            String keyword,
            Boolean active,
            int page,
            int size
    ) {
        validatePageRequest(page, size);
        String normalizedGroupCode = normalizeRequiredCode("groupCode", groupCode);
        StandardCodeSearchCondition condition = new StandardCodeSearchCondition(
                normalizedGroupCode,
                trimToNull(keyword),
                active,
                page,
                size,
                (page - 1) * size
        );
        long totalCount = standardCodeDao.selectStandardCodeCount(condition);
        List<StandardCodeResponse> items = standardCodeDao.selectStandardCodeList(condition).stream()
                .map(this::toCodeResponse)
                .toList();
        return PageResponse.of(items, page, size, totalCount);
    }

    /**
     * 업무 데이터를 응답 형식으로 변환합니다.
     *
     * @param row 입력 값
     *
     * @return 처리 결과
     */
    private StandardCodeGroupResponse toGroupResponse(StandardCodeGroupRow row) {
        return new StandardCodeGroupResponse(
                row.standardCodeGroupId(),
                row.groupCode(),
                row.groupName(),
                row.sourceName(),
                row.sourceUrl(),
                row.versionLabel(),
                Boolean.TRUE.equals(row.active())
        );
    }

    /**
     * 업무 데이터를 응답 형식으로 변환합니다.
     *
     * @param row 입력 값
     *
     * @return 처리 결과
     */
    private StandardCodeResponse toCodeResponse(StandardCodeRow row) {
        return new StandardCodeResponse(
                row.standardCodeId(),
                row.groupCode(),
                row.groupName(),
                row.code(),
                row.codeName(),
                row.parentCode(),
                row.levelNo(),
                row.sortOrder() == null ? 0 : row.sortOrder(),
                Boolean.TRUE.equals(row.active())
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
            throw new ApiException(
                    ErrorCode.INVALID_PAGE_REQUEST,
                    HttpStatus.BAD_REQUEST,
                    "page must be 1 or greater and size must be between 1 and 100."
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
     * @return 처리 결과
     */
    private String normalizeRequiredCode(String fieldName, String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new ApiException(
                    ErrorCode.VALIDATION_FAILED,
                    HttpStatus.BAD_REQUEST,
                    fieldName + " is required."
            );
        }
        return normalized.toUpperCase(Locale.ROOT);
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
}
