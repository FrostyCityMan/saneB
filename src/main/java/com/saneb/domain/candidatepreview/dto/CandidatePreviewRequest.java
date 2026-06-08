package com.saneb.domain.candidatepreview.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CandidatePreviewRequest(
        @Size(max = 30, message = "regionCode must be 30 characters or less")
        String regionCode,

        @DecimalMin(value = "0", message = "annualRevenue must be 0 or greater")
        BigDecimal annualRevenue,

        LocalDate openingDate,
        Boolean hasSpouse,
        Boolean hasChild
) {
}
