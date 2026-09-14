package com.saneb.domain.announcementattachment.service;

import com.saneb.domain.announcementattachment.vo.AttachmentEvaluationRows;
import java.util.Optional;
import java.util.UUID;

public interface AnnouncementAttachmentEvaluationService {
    /** 봉인된 DB 근거만 판정한다. HTTP/다운로드/추출 호출을 발생시키지 않는다. */
    Optional<AttachmentEvaluationRows.Evaluation> saveJobEvaluation(UUID jobId, UUID leaseToken);
}
