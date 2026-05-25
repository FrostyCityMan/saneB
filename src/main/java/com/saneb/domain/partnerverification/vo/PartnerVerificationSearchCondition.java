package com.saneb.domain.partnerverification.vo;

import java.util.UUID;

public record PartnerVerificationSearchCondition(
        UUID memberUserId,
        UUID partnerUserId,
        String statusCode,
        Boolean current,
        int page,
        int size,
        int offset
) {
}
