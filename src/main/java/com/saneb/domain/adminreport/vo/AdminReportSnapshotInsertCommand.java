package com.saneb.domain.adminreport.vo;

import java.util.UUID;

public record AdminReportSnapshotInsertCommand(
        UUID snapshotId,
        String snapshotTypeCode,
        String snapshotJson,
        UUID actorUserId
) {
}
