package com.saneb.domain.announcementsource.provider.content;

/**
 * 외부 제공자 상세본문 조회의 내부 상태와 실패 코드를 정의합니다.
 */
public final class ProviderContentCodes {

    private ProviderContentCodes() {
    }

    public enum StatusCode {
        AVAILABLE,
        DISABLED,
        FETCH_FAILED
    }

    public enum FailureCode {
        FEATURE_DISABLED,
        PROVIDER_UNSUPPORTED,
        SOURCE_URL_INVALID,
        DETAIL_URL_INVALID,
        DETAIL_HOST_NOT_ALLOWED,
        DNS_LOOKUP_FAILED,
        ADDRESS_BLOCKED,
        REDIRECT_LOCATION_MISSING,
        REDIRECT_LIMIT_EXCEEDED,
        CONTENT_TYPE_UNSUPPORTED,
        CONTENT_ENCODING_UNSUPPORTED,
        RESPONSE_TOO_LARGE,
        HTTP_STATUS_ERROR,
        HTTP_SERVER_ERROR,
        TIMEOUT,
        NETWORK_ERROR,
        CHARSET_UNSUPPORTED,
        CONTENT_DECODE_FAILED,
        BODY_TEXT_EMPTY,
        INTERRUPTED
    }
}
