package com.saneb.domain.standardcode.vo;

import java.util.UUID;

public record StandardCodeRow(
        UUID standardCodeId,
        String groupCode,
        String groupName,
        String code,
        String codeName,
        String parentCode,
        Integer levelNo,
        Integer sortOrder,
        Boolean active
) {
}
