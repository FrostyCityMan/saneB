package com.saneb.domain.announcementsource.vo;

import java.util.UUID;

/**
 * 수집 공고 목록에 표시할 현재 유효 다중 분류 코드입니다.
 */
public record AnnouncementSourceClassificationTagSummaryRow(
        UUID sourceId,
        String targetCategoryCodes,
        String supportTypeCodes
) {
}
