package com.saneb.domain.announcementattachment.vo;

import java.time.LocalDate;

public record AttachmentSourceSearchCondition(String providerCode, String effectiveStatusCode, String jobStatusCode,
        String targetCategoryCode, String supportTypeCode, String keyword, LocalDate collectedFrom, LocalDate collectedTo,
        int page, int size, String processingFlowStatusCode) {
    public AttachmentSourceSearchCondition(String providerCode, String effectiveStatusCode, String jobStatusCode,
            String targetCategoryCode, String supportTypeCode, String keyword, LocalDate collectedFrom,
            LocalDate collectedTo, int page, int size) {
        this(providerCode, effectiveStatusCode, jobStatusCode, targetCategoryCode, supportTypeCode, keyword,
                collectedFrom, collectedTo, page, size, null);
    }
    public int offset() { return (page - 1) * size; }
}
