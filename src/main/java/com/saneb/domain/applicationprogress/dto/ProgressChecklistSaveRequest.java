package com.saneb.domain.applicationprogress.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record ProgressChecklistSaveRequest(
        @Valid List<DocumentRequest> documents
) {

    public record DocumentRequest(
            @NotNull UUID stepDocumentId,
            @NotNull Boolean checked
    ) {
    }
}
