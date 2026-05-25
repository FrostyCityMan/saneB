package com.saneb.domain.applicationprogress.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ProgressResultCommand(
        UUID progressId,
        String statusCode,
        String resultCode,
        String resultNote,
        LocalDate resultDate,
        BigDecimal receivedAmount,
        UUID actorUserId
) {
}
