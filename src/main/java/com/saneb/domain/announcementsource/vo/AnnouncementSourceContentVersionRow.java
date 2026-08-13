package com.saneb.domain.announcementsource.vo;

import java.util.UUID;

/**
 * 재분류에 사용할 최신 불변 원문 버전입니다.
 */
public record AnnouncementSourceContentVersionRow(
        UUID contentVersionId,
        UUID sourceId,
        String title,
        String bodyText,
        String bodySourceCode,
        String bodyAvailabilityCode,
        String sourceUrl
) {
}
