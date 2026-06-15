package com.saneb.domain.standardcode.dto;

import java.util.UUID;

public record StandardCodeGroupResponse(
        UUID standardCodeGroupId,
        String groupCode,
        String groupName,
        String sourceName,
        String sourceUrl,
        String versionLabel,
        boolean active
) {
}
