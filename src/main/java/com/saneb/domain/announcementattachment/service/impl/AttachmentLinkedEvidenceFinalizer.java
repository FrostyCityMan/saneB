package com.saneb.domain.announcementattachment.service.impl;

import com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentJobDao;
import java.util.UUID;

/** 호출자의 완료/실패 transaction 안에서만 실행하는 연결 근거 후처리. */
final class AttachmentLinkedEvidenceFinalizer {
    private AttachmentLinkedEvidenceFinalizer() { }
    static boolean saveNotice(AnnouncementAttachmentJobDao jobs, UUID jobId) {
        var job=jobs.selectJobDetails(jobId);
        if(job==null || job.batchId()==null) return true;
        jobs.selectSourceContextDetailsForUpdate(job.sourceId());
        jobs.selectLinkedConnectionLocks(job.sourceId());
        Boolean unchanged=jobs.selectLinkedTerminalConnectionState(jobId);
        if(unchanged==null) return true; // 일반 배치와 재시도 대기는 별도 경고 대상이 아니다.
        if(!unchanged) {
            jobs.updateLinkedTerminalConflict(jobId);
            return false;
        }
        jobs.insertLinkedReviewNotice(jobId);
        return true;
    }
}
