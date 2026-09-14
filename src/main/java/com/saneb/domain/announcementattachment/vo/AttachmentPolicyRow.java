package com.saneb.domain.announcementattachment.vo;

import java.util.UUID;

public record AttachmentPolicyRow(
        UUID policyId, String policyStatusCode, String modeCode, UUID ruleReleaseId,
        String releaseStatusCode, String policyHash, String settingsJson,
        String profileManifestJson, Integer rowVersion
) { }
