package com.saneb.domain.applicationprogress.vo;

import java.util.UUID;

public record StepButtonRow(
        UUID stepId,
        String buttonCode,
        String buttonActionCode,
        UUID nextStepId
) {
}
