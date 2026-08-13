package com.saneb.domain.announcementsource.provider.content;

import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.BodyAvailabilityCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.BodySourceCode;
import com.saneb.domain.announcementsource.provider.content.ProviderContentCodes.FailureCode;
import com.saneb.domain.announcementsource.provider.content.ProviderContentCodes.StatusCode;
import java.net.URI;
import java.util.UUID;

/**
 * 상세본문 조회 결과입니다. 실패는 수집 run 예외가 아니라 구조화된 결과로 반환합니다.
 */
public record ProviderContentResult(
        String providerCode,
        UUID registeredSourceId,
        StatusCode statusCode,
        BodySourceCode bodySourceCode,
        BodyAvailabilityCode bodyAvailabilityCode,
        String bodyText,
        String finalUrl,
        Integer httpStatus,
        FailureCode failureCode,
        int attemptCount,
        int redirectCount
) {

    public static ProviderContentResult disabled(ProviderContentRequest request) {
        return new ProviderContentResult(
                request.providerCode(),
                request.registeredSourceId(),
                StatusCode.DISABLED,
                BodySourceCode.NONE,
                BodyAvailabilityCode.UNSUPPORTED,
                null,
                null,
                null,
                FailureCode.FEATURE_DISABLED,
                0,
                0
        );
    }

    public static ProviderContentResult available(
            ProviderContentRequest request,
            String bodyText,
            URI finalUri,
            int httpStatus,
            int attemptCount,
            int redirectCount
    ) {
        return new ProviderContentResult(
                request.providerCode(),
                request.registeredSourceId(),
                StatusCode.AVAILABLE,
                BodySourceCode.DETAIL_PAGE_TEXT,
                BodyAvailabilityCode.AVAILABLE,
                bodyText,
                finalUri.toASCIIString(),
                httpStatus,
                null,
                attemptCount,
                redirectCount
        );
    }

    public static ProviderContentResult failure(
            ProviderContentRequest request,
            FailureCode failureCode,
            URI finalUri,
            Integer httpStatus,
            int attemptCount,
            int redirectCount
    ) {
        return new ProviderContentResult(
                request.providerCode(),
                request.registeredSourceId(),
                StatusCode.FETCH_FAILED,
                BodySourceCode.DETAIL_PAGE_TEXT,
                BodyAvailabilityCode.FETCH_FAILED,
                null,
                finalUri == null ? null : finalUri.toASCIIString(),
                httpStatus,
                failureCode,
                attemptCount,
                redirectCount
        );
    }
}
