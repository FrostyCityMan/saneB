package com.saneb.domain.applicationprogress.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ApplicationProgressStartRequest(
        @NotNull UUID matchingCaseId
) {
}
