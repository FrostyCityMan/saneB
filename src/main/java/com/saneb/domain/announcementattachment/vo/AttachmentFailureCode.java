package com.saneb.domain.announcementattachment.vo;

/** 외부 예외 메시지를 저장하지 않기 위한 고정 실패 분류. */
public enum AttachmentFailureCode {
    NETWORK_TIMEOUT(true), NETWORK_UNAVAILABLE(true), HTTP_RATE_LIMITED(true), HTTP_SERVER_ERROR(true),
    DISCOVERY_FAILED(false), DISCOVERY_CHANGED(false), PROFILE_REQUIRED(false), DOWNLOAD_BLOCKED(false), LIMIT_EXCEEDED(false),
    EXTRACTION_FAILED(false), ISOLATION_UNAVAILABLE(false), UNSUPPORTED_FORMAT(false), WORKER_PROCESSING_FAILED(false);

    private final boolean retryable;

    AttachmentFailureCode(boolean retryable) { this.retryable = retryable; }
    public boolean retryable() { return retryable; }
}
