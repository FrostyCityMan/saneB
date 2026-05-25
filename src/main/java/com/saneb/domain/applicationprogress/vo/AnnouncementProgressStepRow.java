package com.saneb.domain.applicationprogress.vo;

import java.util.UUID;

public record AnnouncementProgressStepRow(
        UUID stepId,
        Integer stepOrder,
        String stepName,
        Boolean active
) {
}
