package com.saneb.domain.applicationprogress.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

public record ProgressActionRequest(
        @NotBlank String buttonCode,
        Map<String, Object> input
) {
}
