package com.saneb.domain.announcementattachment.vo;

import java.util.UUID;

/** 내부 예약 계약. 실행 snapshot은 관리자 입력을 그대로 전달하지 않고 시스템 프로필에서 만든다. */
public record AttachmentJobReservation(
        UUID sourceId, UUID policyId, UUID expectedBaseDecisionId,
        int expectedSourceVersion, int expectedAttachmentVersion,
        UUID idempotencyKey, AttachmentExecutionSnapshot execution,
        UUID collectionRunId, boolean bindNewSource
) {
    public AttachmentJobReservation(UUID sourceId,UUID policyId,UUID expectedBaseDecisionId,int expectedSourceVersion,
            int expectedAttachmentVersion,UUID idempotencyKey,AttachmentExecutionSnapshot execution) {
        this(sourceId,policyId,expectedBaseDecisionId,expectedSourceVersion,expectedAttachmentVersion,idempotencyKey,execution,null,false);
    }
}
