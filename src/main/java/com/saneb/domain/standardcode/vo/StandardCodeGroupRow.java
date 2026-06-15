package com.saneb.domain.standardcode.vo;

import java.util.UUID;

public record StandardCodeGroupRow(
        UUID standardCodeGroupId,
        String groupCode,
        String groupName,
        String sourceName,
        String sourceUrl,
        String versionLabel,
        Boolean active
) {
}
