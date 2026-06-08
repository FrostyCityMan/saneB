package com.saneb.domain.consent.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ConsentSaveRequest(
        @NotBlank(message = "동의 항목을 선택해 주세요.")
        String consentCode,
        @NotNull(message = "동의 여부를 확인해 주세요.")
        @AssertTrue(message = "동의 여부를 확인해 주세요.")
        Boolean consented
) {
}
