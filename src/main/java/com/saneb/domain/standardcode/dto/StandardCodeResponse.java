package com.saneb.domain.standardcode.dto;

import java.util.UUID;

public record StandardCodeResponse(
        UUID standardCodeId,
        String groupCode,
        String groupName,
        String code,
        String codeName,
        String parentCode,
        Integer levelNo,
        int sortOrder,
        boolean active
) {
}
