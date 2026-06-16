package com.saneb.domain.applicationprogress.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ApplicationProgressDetailsResponse(
        UUID progressId,
        UUID matchingCaseId,
        UUID announcementId,
        UUID memberUserId,
        String progressCode,
        String matchingCaseCode,
        String announcementCode,
        String memberUserCode,
        UUID currentStepId,
        String statusCode,
        String receiptNo,
        LocalDate receiptDate,
        String resultCode,
        String resultNote,
        LocalDate resultDate,
        BigDecimal receivedAmount,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<StepStateResponse> stepStates,
        List<ChecklistResponse> checklists,
        List<StepButtonResponse> stepButtons
) {

    public record StepStateResponse(
            UUID stepStateId,
            UUID stepId,
            int stepOrder,
            String stepName,
            String guideMessage,
            String actionGuide,
            String completionConditionCode,
            String statusCode,
            OffsetDateTime startedAt,
            OffsetDateTime completedAt
    ) {
    }

    public record ChecklistResponse(
            UUID checklistId,
            UUID stepDocumentId,
            UUID stepId,
            String documentTypeCode,
            boolean required,
            boolean checked,
            OffsetDateTime checkedAt,
            UUID checkedBy
    ) {
    }

    public record StepButtonResponse(
            UUID stepId,
            String buttonCode,
            String buttonLabel,
            String buttonActionCode,
            UUID nextStepId,
            int sortOrder
    ) {
    }
}
