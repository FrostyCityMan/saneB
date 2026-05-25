package com.saneb.domain.partnerverification.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record VerificationRestrictionFlagsSaveRequest(
        @Valid
        List<RestrictionFlagRequest> restrictionFlags
) {

    public record RestrictionFlagRequest(
            @NotBlank(message = "restrictionCode is required")
            @Size(max = 80, message = "restrictionCode must be 80 characters or less")
            String restrictionCode,

            @NotNull(message = "checked is required")
            Boolean checked,

            @Size(max = 2000, message = "note must be 2000 characters or less")
            String note
    ) {
    }
}
