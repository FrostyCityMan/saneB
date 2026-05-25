package com.saneb.domain.partnerverification.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record VerificationDocumentsSaveRequest(
        @Valid
        List<DocumentRequest> documents
) {

    public record DocumentRequest(
            @NotBlank(message = "documentTypeCode is required")
            @Size(max = 80, message = "documentTypeCode must be 80 characters or less")
            String documentTypeCode,

            @NotBlank(message = "sourceTypeCode is required")
            @Size(max = 50, message = "sourceTypeCode must be 50 characters or less")
            String sourceTypeCode,

            @NotNull(message = "checked is required")
            Boolean checked,

            @Size(max = 2000, message = "note must be 2000 characters or less")
            String note
    ) {
    }
}
