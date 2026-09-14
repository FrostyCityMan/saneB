package com.saneb.domain.announcementattachment.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** CLASSIFICATION_GOLDEN 이력만 표현한다. 전체 검증·게시 가능 여부를 뜻하는 필드는 없다. */
public record AttachmentPolicyCheckResponse(UUID checkId,UUID policyId,Integer policyVersion,String policySnapshotHash,
        UUID ruleReleaseId,Integer ruleVersion,String ruleSnapshotHash,String ruleContentHash,String checkTypeCode,
        String suiteVersion,String engineVersion,String resultHash,Integer caseCount,List<String> caseIds,
        boolean isCurrent,OffsetDateTime createdAt) {
    public AttachmentPolicyCheckResponse { caseIds=List.copyOf(caseIds); }
}
