package com.saneb.domain.documentfile.vo;

import java.util.UUID;

public record DocumentSubmissionInsertCommand(
        UUID submissionId,
        UUID fileId,
        UUID submittedBy,
        String resourceTypeCode,
        UUID resourceId,
        String documentTypeCode
) {
}
