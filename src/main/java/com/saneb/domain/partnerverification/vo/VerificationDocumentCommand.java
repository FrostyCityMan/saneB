package com.saneb.domain.partnerverification.vo;

import java.util.UUID;

public record VerificationDocumentCommand(
        UUID verificationId,
        String documentTypeCode,
        String sourceTypeCode,
        Boolean checked,
        UUID checkedBy,
        String note,
        UUID actorUserId
) {
}
