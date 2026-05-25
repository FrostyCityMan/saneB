package com.saneb.domain.dynamicinput.vo;

import java.util.UUID;

public record ApplicationProgressInputRow(
        UUID progressId,
        UUID announcementId,
        UUID memberUserId
) {
}
