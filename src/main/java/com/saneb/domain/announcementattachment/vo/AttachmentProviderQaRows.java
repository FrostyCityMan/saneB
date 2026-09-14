package com.saneb.domain.announcementattachment.vo;

import java.time.OffsetDateTime;
import java.util.UUID;

/** 실행 원장 metadata. 제목·URL·문서 내용·QA 기대 문구를 저장하지 않는다. */
public final class AttachmentProviderQaRows {
    private AttachmentProviderQaRows() { }
    public record RunInsert(UUID runId, UUID policyId, int policyVersion, UUID ruleReleaseId, int ruleVersion,
            String snapshotHash, String catalogHash, String executionCodeHash, String runtimeHash, String requiredScopeJson,
            int expectedCaseCount, long maximumRequests, long maximumBytes, UUID requestedBy, UUID idempotencyKey, String requestHash) { }
    public record CaseInsert(UUID caseId, UUID runId, int ordinal, String caseCode, String inputHash, String profileHash,
            int expectedFileCount, int maximumSeconds, int maximumRequests, long maximumBytes) { }
    public record CaseRow(UUID caseId, UUID runId, String caseCode, String inputHash, String profileHash, Integer expectedFileCount,
            Integer maximumSeconds, Integer maximumRequests, Long maximumBytes, Integer requestReservations, Long reservedBytes,
            String statusCode, Integer rowVersion, UUID leaseToken, OffsetDateTime leaseExpiresAt, String runStatusCode,
            String executionCodeHash, String runtimeHash, Boolean inputVersionsCurrent, String evidenceJson) { }
    public record Resource(UUID caseId, UUID leaseToken, String resourceCode, String resourceKey, int slotNo, UUID resourceToken) { }
}
