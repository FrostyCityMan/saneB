package com.saneb.domain.announcementattachment.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public final class AttachmentNormalRollbackResponses {
    private AttachmentNormalRollbackResponses() { }
    /** 복구 가능 여부를 추정하지 않는 일반 작업 이력. 승인 전 별도 preview가 필요하다. */
    public record JobSummary(UUID sourceId,UUID jobId,String operationCode,String jobStatusCode,String applicationStatusCode,
            String rollbackStatusCode,UUID actionId,OffsetDateTime createdAt) { }
    public record Preview(UUID sourceId,UUID jobId,String modeCode,String jobStatusCode,String applicationStatusCode,
            String rollbackStatusCode,String readinessCode,Integer sourceVersion,Integer attachmentVersion,String previewHash,
            boolean baseReopens,boolean confirmationRestores,boolean staleConfirmationRemains,int targetCount,int currentHttpRequests) { }
    /** 버전과 효과는 해당 복구 영수증의 값이다. 이후 원문의 현재 상태를 뜻하지 않는다. */
    public record Receipt(UUID actionId,UUID sourceId,UUID jobId,String modeCode,String statusCode,
            int restoredSourceVersion,int restoredAttachmentVersion,boolean baseReopened,boolean confirmationRestored,
            int targetCount,int currentHttpRequests,OffsetDateTime recordedAt) { }
}
