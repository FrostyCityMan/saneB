package com.saneb.domain.announcementattachment.vo;

import java.time.OffsetDateTime;
import java.util.UUID;

public final class AttachmentNormalRollbackRows {
    private AttachmentNormalRollbackRows() { }
    public record State(UUID jobId,UUID sourceId,Integer jobVersion,String jobStatusCode,String applicationStatusCode,
            String rollbackStatusCode,String modeCode,Integer sourceVersion,Integer attachmentVersion,
            UUID previousEvaluationId,UUID previousConfirmationId,String readinessCode,Boolean baseReopens,
            Boolean confirmationRestores,Boolean staleConfirmationRemains,String inputHash,String previewHash) { }
    public record Action(UUID id,UUID jobId,UUID sourceId,String modeCode,Integer expectedSourceVersion,Integer expectedAttachmentVersion,
            String previewHash,Boolean baseReopens,Boolean confirmationRestores,UUID actorId,UUID key,String requestHash,String reasonHash,OffsetDateTime createdAt) { }
}
