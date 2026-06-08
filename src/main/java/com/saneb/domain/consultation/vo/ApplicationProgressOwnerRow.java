package com.saneb.domain.consultation.vo;

import java.util.UUID;

public record ApplicationProgressOwnerRow(
        UUID progressId,
        UUID memberUserId
) {
}
