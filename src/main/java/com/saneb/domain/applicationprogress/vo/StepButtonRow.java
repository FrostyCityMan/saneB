package com.saneb.domain.applicationprogress.vo;

import java.util.UUID;

public record StepButtonRow(
        UUID stepId,
        String buttonCode,
        String buttonLabel,
        String buttonActionCode,
        UUID nextStepId,
        int sortOrder
) {
}
