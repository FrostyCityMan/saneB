package com.saneb.domain.announcementattachment.dto;

import com.saneb.domain.announcementattachment.vo.AttachmentJobRow;
import java.util.UUID;

/** 예약 시 버전은 작업 완료 후의 현재 source 버전이 아니다. lease/내부 요청 hash는 반환하지 않는다. */
public record AttachmentJobResponse(UUID sourceId, UUID jobId, UUID setId, String operationCode, String jobStatusCode,
        Integer sourceVersionAtReservation, Integer attachmentVersionAtReservation, Integer generation, String errorCode) {
    public static AttachmentJobResponse from(AttachmentJobRow row) {
        return new AttachmentJobResponse(row.sourceId(),row.jobId(),row.setId(),row.operationCode(),row.jobStatusCode(),
                row.expectedSourceVersion(),row.expectedAttachmentVersion(),row.generation(),row.errorCode());
    }
}
