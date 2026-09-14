package com.saneb.domain.announcementattachment.dto;

import com.saneb.common.response.PageResponse;
import com.saneb.domain.announcementattachment.qa.AttachmentProviderQaCatalog.Segment;
import com.saneb.domain.announcementattachment.qa.AttachmentProviderQaCatalog.FormatCoverage;
import com.saneb.domain.announcementattachment.qa.AttachmentProviderQaCatalog.TargetPlan;
import java.time.OffsetDateTime;
import java.util.UUID;

public final class AttachmentProviderQaResponses {
    private AttachmentProviderQaResponses() { }
    public record Preview(UUID policyId,int policyVersion,String snapshotHash,String catalogHash,String planHash,int targetCount,int catalogCaseCount,
                          int executableCaseCount,boolean isExpectationCoverageComplete,boolean isQaPassed,boolean isReservationEnabled,PageResponse<Segment> segments) { }
    public record Coverage(UUID policyId,int policyVersion,String snapshotHash,String catalogHash,String planHash,
                           boolean isExpectationCoverageComplete,boolean isQaPassed,FormatCoverage formatCoverage,PageResponse<TargetPlan> targets) { }
    public record Run(UUID runId,UUID policyId,int policyVersion,String snapshotHash,String catalogHash,String planHash,String statusCode,int rowVersion,
                      int expectedCaseCount,long maximumRequests,long maximumBytes,long requestReservations,long reservedBytes,Integer segmentNo,Integer segmentCount,
                      Integer catalogCaseCount,Integer executableCaseCount,Boolean isExpectationCoverageComplete,Integer maximumSecondsIncludingMargin,
                      boolean isInputVersionsCurrent,boolean isQaPassed,OffsetDateTime createdAt,OffsetDateTime expiresAt,OffsetDateTime completedAt) { }
    public record Item(UUID caseId,int ordinal,String caseCode,String inputHash,String profileHash,int expectedFileCount,String statusCode,int rowVersion,
                       int requestReservations,long reservedBytes,OffsetDateTime startedAt,OffsetDateTime completedAt,String errorCode,String evidenceHash) { }
}
