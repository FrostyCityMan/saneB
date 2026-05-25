package com.saneb.domain.partnerverification.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record PartnerVerificationCreateRequest(
        @NotNull(message = "memberUserId is required")
        UUID memberUserId,
        UUID businessProfileId
) {
}
