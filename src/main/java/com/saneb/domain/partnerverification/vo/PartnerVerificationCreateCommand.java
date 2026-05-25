package com.saneb.domain.partnerverification.vo;

import java.util.UUID;

public record PartnerVerificationCreateCommand(
        UUID id,
        UUID memberUserId,
        UUID partnerUserId,
        UUID businessProfileId,
        UUID actorUserId
) {
}
