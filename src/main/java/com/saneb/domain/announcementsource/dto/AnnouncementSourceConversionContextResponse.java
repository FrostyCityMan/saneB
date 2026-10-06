package com.saneb.domain.announcementsource.dto;

import java.util.List;
import java.util.UUID;

/** 읽기 전용 작업 안내. 최종 쓰기에서는 동일 조건과 버전을 다시 검증한다. */
public record AnnouncementSourceConversionContextResponse(
        String modeCode, boolean convertible, String blockedReason,
        UUID decisionId, Integer version, boolean confirmed,
        List<String> targetCategoryCodes, List<String> supportTypeCodes,
        AnnouncementSourceLinkResponse linkedAnnouncement
) { }
