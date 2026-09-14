package com.saneb.domain.announcementattachment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.saneb.domain.announcementattachment.vo.AttachmentCurrentSourceRow;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AttachmentProcessingFlowTest {
    private AttachmentCurrentSourceRow selectCompleteRow() {
        var row = mock(AttachmentCurrentSourceRow.class);
        when(row.reviewRequired()).thenReturn(true);
        when(row.baseDecisionId()).thenReturn(UUID.randomUUID());
        when(row.baseReason()).thenReturn("BODY_UNAVAILABLE");
        when(row.attachmentDecisionId()).thenReturn(UUID.randomUUID());
        when(row.attachmentStatus()).thenReturn("ACCEPTED");
        when(row.attachmentReason()).thenReturn("EXTENDED_TARGET_SUPPORT_CONFIRMED");
        when(row.setId()).thenReturn(UUID.randomUUID());
        when(row.setHash()).thenReturn("a".repeat(64));
        when(row.jobStatus()).thenReturn("SUCCEEDED");
        when(row.discoveryStatus()).thenReturn("FOUND");
        when(row.discoveryComplete()).thenReturn(true);
        when(row.discoveredCount()).thenReturn(1);
        when(row.processedCount()).thenReturn(1);
        return row;
    }
    private void assertFlow(AttachmentCurrentSourceRow row, String status, boolean complete, boolean available) {
        var flow = AttachmentProcessingFlow.selectFlowDetails(row);
        assertThat(flow.statusCode()).isEqualTo(status);
        assertThat(flow.isAutomaticAnalysisComplete()).isEqualTo(complete);
        assertThat(flow.isFinalReviewAvailable()).isEqualTo(available);
    }
    @Test void attachmentCanResolveMissingBodyWithoutRewritingBase() {
        var row = selectCompleteRow();
        assertFlow(row, "READY_FOR_FINAL_REVIEW", true, true);
        assertThat(row.baseReason()).isEqualTo("BODY_UNAVAILABLE");
    }
    @Test void previewAndAbsentBaseAreNeverThreeStageCompletion() {
        var row = selectCompleteRow();
        when(row.reviewRequired()).thenReturn(false);
        assertFlow(row, "NOT_APPLIED", false, false);
        when(row.reviewRequired()).thenReturn(true);
        when(row.baseDecisionId()).thenReturn(null);
        assertFlow(row, "CLASSIFICATION_PENDING", false, false);
    }
    @ParameterizedTest @ValueSource(strings = {"PROFILE_REQUIRED", "BASE_RECLASSIFICATION_REQUIRED", "POLICY_BINDING_CHANGED"})
    void configurationCannotBeResolvedByOldEvaluation(String intake) {
        var row = selectCompleteRow();
        when(row.intakeStatus()).thenReturn(intake);
        assertFlow(row, "CONFIGURATION_REQUIRED", false, false);
    }
    @ParameterizedTest @ValueSource(strings = {"SCOPE_READY", "PENDING", "RUNNING", "RETRY_WAIT", "PAUSED"})
    void activeJobPreventsFinalReviewEvenWithAnOldConfirmation(String status) {
        var row = selectCompleteRow();
        when(row.jobStatus()).thenReturn(status);
        when(row.confirmationId()).thenReturn(UUID.randomUUID());
        when(row.confirmationStatus()).thenReturn("CURRENT");
        assertFlow(row, "AUTOMATIC_PROCESSING", false, false);
    }
    @Test void priorEvidenceAndUnrequestedWorkAreNotReady() {
        var row = selectCompleteRow();
        when(row.attachmentDecisionId()).thenReturn(null);
        when(row.jobStatus()).thenReturn("NOT_REQUESTED");
        assertFlow(row, "AUTOMATIC_PROCESSING", false, false);
        when(row.attachmentStale()).thenReturn(true);
        assertFlow(row, "EVIDENCE_STALE", false, false);
        when(row.attachmentStale()).thenReturn(false);
        when(row.effectiveReason()).thenReturn("ATTACHMENT_STALE");
        assertFlow(row, "EVIDENCE_STALE", false, false);
    }
    @ParameterizedTest @ValueSource(strings = {"SUCCEEDED", "FAILED", "PARTIAL_FAILED", "CANCELLED", "CONFLICT", "FUTURE_STATUS"})
    void terminalJobWithoutCurrentSealedEvaluationDoesNotBecomeReady(String status) {
        var row = selectCompleteRow();
        when(row.attachmentDecisionId()).thenReturn(null);
        when(row.jobStatus()).thenReturn(status);
        assertFlow(row, "TECHNICAL_EXCEPTION", false, false);
    }
    @ParameterizedTest @ValueSource(strings = {"FAILED", "PARTIAL_FAILED", "CANCELLED", "CONFLICT"})
    void failedRefreshDoesNotAdvertiseEarlierEvidenceAsAutomaticSuccess(String status) {
        var row = selectCompleteRow();
        when(row.jobStatus()).thenReturn(status);
        assertFlow(row, "TECHNICAL_EXCEPTION", false, true);
    }
    @ParameterizedTest @ValueSource(strings = {"ATTACHMENT_INCOMPLETE", "BODY_UNAVAILABLE", "BODY_FETCH_FAILED", "FUTURE_REASON"})
    void incompleteOrUnknownReasonRequiresTechnicalExceptionNotAutomaticSuccess(String reason) {
        var row = selectCompleteRow();
        when(row.attachmentStatus()).thenReturn("REVIEW_REQUIRED");
        when(row.attachmentReason()).thenReturn(reason);
        assertFlow(row, "TECHNICAL_EXCEPTION", false, true);
        when(row.confirmationId()).thenReturn(UUID.randomUUID());
        when(row.confirmationStatus()).thenReturn("CURRENT");
        assertFlow(row, "FINAL_REVIEW_CONFIRMED", false, true);
    }
    @ParameterizedTest @ValueSource(strings = {"TITLE_GROUP_A_MATCHED", "BODY_GROUP_A_MATCHED", "BODY_GROUP_B_MATCHED",
            "ATTACHMENT_GROUP_A_MATCHED", "ATTACHMENT_GROUP_B_MATCHED", "ATTACHMENT_CONTEXT_REVIEW", "EXTENDED_COMBINATION_NOT_CONFIRMED"})
    void textAndRoleIssuesRemainFinalReviewIssuesNotAutomaticExclusion(String reason) {
        var row = selectCompleteRow();
        when(row.attachmentStatus()).thenReturn("REVIEW_REQUIRED");
        when(row.attachmentReason()).thenReturn(reason);
        assertFlow(row, "FINAL_REVIEW_EXCEPTION", true, true);
    }
    @Test void noFilesMustBeAnExplicitCompleteZeroAndDoesNotResolveAbsentBody() {
        var row = selectCompleteRow();
        when(row.discoveryStatus()).thenReturn("NO_FILES");
        assertFlow(row, "TECHNICAL_EXCEPTION", false, true);
        when(row.discoveredCount()).thenReturn(0);
        when(row.processedCount()).thenReturn(0);
        when(row.attachmentReason()).thenReturn("TARGET_SUPPORT_CONFIRMED");
        assertFlow(row, "READY_FOR_FINAL_REVIEW", true, true);
        when(row.attachmentReason()).thenReturn("BODY_UNAVAILABLE");
        when(row.attachmentStatus()).thenReturn("REVIEW_REQUIRED");
        assertFlow(row, "TECHNICAL_EXCEPTION", false, true);
    }
    @Test void incompleteDiscoveryAndCountsNeverBecomeComplete() {
        var row = selectCompleteRow();
        when(row.discoveryComplete()).thenReturn(false);
        assertFlow(row, "TECHNICAL_EXCEPTION", false, true);
        when(row.discoveryComplete()).thenReturn(true);
        when(row.processedCount()).thenReturn(0);
        assertFlow(row, "TECHNICAL_EXCEPTION", false, true);
        when(row.processedCount()).thenReturn(null);
        assertFlow(row, "TECHNICAL_EXCEPTION", false, true);
        when(row.discoveredCount()).thenReturn(11);
        when(row.processedCount()).thenReturn(11);
        assertFlow(row, "TECHNICAL_EXCEPTION", false, true);
    }
    @Test void malformedBindingOrStatusFailsClosedWithoutReviewAvailability() {
        var row = selectCompleteRow();
        when(row.setHash()).thenReturn(null);
        assertFlow(row, "TECHNICAL_EXCEPTION", false, false);
        when(row.setHash()).thenReturn("short");
        assertFlow(row, "TECHNICAL_EXCEPTION", false, false);
        when(row.setHash()).thenReturn("a".repeat(64));
        when(row.attachmentStatus()).thenReturn("EXCLUDED");
        assertFlow(row, "TECHNICAL_EXCEPTION", false, false);
        when(row.attachmentStatus()).thenReturn(null);
        assertFlow(row, "TECHNICAL_EXCEPTION", false, false);
        when(row.attachmentStatus()).thenReturn("ACCEPTED");
        when(row.jobStatus()).thenReturn("FUTURE_STATUS");
        assertFlow(row, "TECHNICAL_EXCEPTION", false, false);
    }
    @Test void anyActiveNormalJobOverridesLatestHistoricalTerminalJob() {
        var row = selectCompleteRow();
        when(row.activeNormalJob()).thenReturn(true);
        assertFlow(row, "AUTOMATIC_PROCESSING", false, false);
    }
    @Test void currentConfirmationAndStaleConfirmationAreDifferent() {
        var row = selectCompleteRow();
        when(row.confirmationId()).thenReturn(UUID.randomUUID());
        when(row.confirmationStatus()).thenReturn("STALE");
        assertFlow(row, "READY_FOR_FINAL_REVIEW", true, true);
        when(row.confirmationStatus()).thenReturn("CURRENT");
        assertFlow(row, "FINAL_REVIEW_CONFIRMED", true, true);
    }
}
