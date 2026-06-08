package com.saneb.domain.applog.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record AppLogResponse(
        String logPath,
        boolean available,
        long fileSizeBytes,
        OffsetDateTime lastModifiedAt,
        int requestedLines,
        int returnedLines,
        String levelCode,
        String keyword,
        String message,
        List<AppLogLineResponse> lines
) {

    public record AppLogLineResponse(
            int sequenceNo,
            String content
    ) {
    }
}
