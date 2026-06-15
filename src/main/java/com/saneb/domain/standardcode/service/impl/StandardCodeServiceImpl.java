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

    public StandardCodeServiceImpl(StandardCodeDao standardCodeDao) {
        this.standardCodeDao = standardCodeDao;
    }

    @Override
    public List<StandardCodeGroupResponse> selectStandardCodeGroupList() {
        return standardCodeDao.selectStandardCodeGroupList().stream()
                .map(this::toGroupResponse)
                .toList();
    }

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

    private void validatePageRequest(int page, int size) {
        if (page < 1 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new ApiException(
                    ErrorCode.INVALID_PAGE_REQUEST,
                    HttpStatus.BAD_REQUEST,
                    "page must be 1 or greater and size must be between 1 and 100."
            );
        }
    }

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

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
