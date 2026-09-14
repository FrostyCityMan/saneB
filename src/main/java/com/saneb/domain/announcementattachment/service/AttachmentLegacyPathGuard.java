package com.saneb.domain.announcementattachment.service;

import com.saneb.common.error.ApiException;
import com.saneb.common.error.ErrorCode;
import org.springframework.http.HttpStatus;

/** 기존 원문 API는 첨부 종합 검수의 판정·확정 버전을 갱신할 수 없습니다. */
public final class AttachmentLegacyPathGuard {
    private AttachmentLegacyPathGuard() {
    }

    public static void validate(Boolean attachmentReviewRequired) {
        if (Boolean.TRUE.equals(attachmentReviewRequired)) {
            throw new ApiException(
                    ErrorCode.ANNOUNCEMENT_SOURCE_NOT_CONVERTIBLE,
                    HttpStatus.CONFLICT,
                    "첨부 종합 검수가 연결된 원문입니다. 첨부 검수 화면에서 현재 판정과 분류를 확인한 뒤 처리해 주세요."
            );
        }
    }
}
