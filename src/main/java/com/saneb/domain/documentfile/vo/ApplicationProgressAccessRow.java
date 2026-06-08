package com.saneb.domain.documentfile.vo;

import java.util.UUID;

public record ApplicationProgressAccessRow(
        UUID progressId,
        UUID memberUserId
) {
}
