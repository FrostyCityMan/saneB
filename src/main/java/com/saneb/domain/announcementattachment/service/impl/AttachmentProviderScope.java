package com.saneb.domain.announcementattachment.service.impl;

import com.saneb.domain.announcementattachment.vo.AttachmentBatchRows;

/** 기존 배치 필터 별칭은 API 경계에만 사용한다. 후보·작업·profile의 실제 Provider 코드는 바꾸지 않는다. */
final class AttachmentProviderScope {
    private AttachmentProviderScope() { }

    static String selectScopeCode(String sourceCode) {
        return "GOV24_PUBLIC_SERVICE".equals(sourceCode) ? "GOV24" : sourceCode;
    }

    static AttachmentBatchRows.Bucket selectScopeBucket(AttachmentBatchRows.Bucket row) {
        return new AttachmentBatchRows.Bucket(selectScopeCode(row.providerCode()), row.reasonCode(), row.count());
    }
}
