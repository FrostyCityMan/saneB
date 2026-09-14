package com.saneb.domain.announcementattachment.service;

import com.saneb.domain.announcementattachment.vo.AttachmentFailureCode;
import com.saneb.domain.announcementattachment.vo.AttachmentJobReservation;
import com.saneb.domain.announcementattachment.vo.AttachmentJobRow;
import com.saneb.domain.announcementattachment.vo.AttachmentResourceLease;
import com.saneb.domain.announcementattachment.vo.AttachmentWorkerSourceRow;
import java.util.Optional;
import java.util.UUID;

/** 내부 수집/worker 계약. 외부 HTTP·문서 추출은 이 짧은 transaction 바깥에서 수행한다. */
public interface AnnouncementAttachmentJobService {
    AttachmentJobRow insertAttachmentJob(AttachmentJobReservation request);
    Optional<AttachmentJobRow> saveNextJobClaim();
    Optional<AttachmentWorkerSourceRow> selectWorkerSourceDetails(UUID jobId, UUID leaseToken);
    boolean selectExternalExecutionAllowed(UUID jobId, UUID leaseToken);
    boolean saveJobDeferred(UUID jobId, UUID leaseToken, boolean requestStarted);
    boolean saveJobHeartbeat(UUID jobId, UUID leaseToken);
    boolean saveJobFailure(UUID jobId, UUID leaseToken, AttachmentFailureCode errorCode);
    boolean saveDownloadBytes(UUID jobId, UUID leaseToken, long bytes);
    Optional<AttachmentResourceLease> saveDownloadLease(UUID jobId, UUID leaseToken, String hostHash);
    Optional<AttachmentResourceLease> saveExtractionLease(UUID jobId, UUID leaseToken);
    void deleteResourceLease(AttachmentResourceLease lease);
}
