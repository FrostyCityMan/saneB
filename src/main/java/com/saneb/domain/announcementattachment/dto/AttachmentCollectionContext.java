package com.saneb.domain.announcementattachment.dto;

import java.util.UUID;

/** 수집 조건 조회는 예약·발견·다운로드를 실행하지 않는다. profile 선택이나 외부 URL은 제공하지 않는다. */
public record AttachmentCollectionContext(AttachmentCollectionRequests.Version version,UUID policyId,String policyHash,
        String executionHash,String modeCode,long maximumDownloadBytes,int maximumFileCount,int maximumAttempts,
        int maximumHttpRequests,boolean isAttachmentReviewRequired,String effectCode) { }
