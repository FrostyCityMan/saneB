package com.saneb.domain.member.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record MemberDocumentInputValueCommand(
        UUID userId,
        UUID standardFieldId,
        String valueText,
        BigDecimal valueNumber,
        LocalDate valueDate,
        Boolean valueBoolean,
        UUID actorUserId
) {
}
