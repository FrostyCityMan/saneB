/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: AnnouncementSourceProviderItem.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.announcementsource.provider;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record AnnouncementSourceProviderItem(
        String providerCode,
        String providerNoticeId,
        String title,
        String agencyName,
        LocalDate applicationStartDate,
        LocalDate applicationEndDate,
        OffsetDateTime postedAt,
        OffsetDateTime modifiedAt,
        String sourceUrl,
        String bodyText,
        String inquiryText,
        String applicationMethodText,
        String sourceCompletenessCode,
        String missingFieldsJson,
        String rawPayloadJson,
        String rawHash,
        List<AnnouncementSourceProviderAttachment> attachments,
        UUID localGovernmentSourceId,
        String semanticStatusCode,
        String semanticReasonCode,
        String semanticMatchedKeywords
) {

    /**
     * 의미 필터를 적용하지 않는 외부 제공자 항목을 생성합니다.
     *
     * @param providerCode 제공자 코드
     * @param providerNoticeId 제공자 공고 식별자
     * @param title 공고명
     * @param agencyName 기관명
     * @param applicationStartDate 신청 시작일
     * @param applicationEndDate 신청 종료일
     * @param postedAt 등록일시
     * @param modifiedAt 수정일시
     * @param sourceUrl 원문 URL
     * @param bodyText 본문
     * @param inquiryText 문의처
     * @param applicationMethodText 신청방법
     * @param sourceCompletenessCode 원문 완전성 코드
     * @param missingFieldsJson 누락 필드 메타데이터
     * @param rawPayloadJson 원문 payload
     * @param rawHash 원문 해시
     * @param attachments 첨부파일 목록
     * @param localGovernmentSourceId 지자체 출처 식별자
     */
    public AnnouncementSourceProviderItem(
            String providerCode,
            String providerNoticeId,
            String title,
            String agencyName,
            LocalDate applicationStartDate,
            LocalDate applicationEndDate,
            OffsetDateTime postedAt,
            OffsetDateTime modifiedAt,
            String sourceUrl,
            String bodyText,
            String inquiryText,
            String applicationMethodText,
            String sourceCompletenessCode,
            String missingFieldsJson,
            String rawPayloadJson,
            String rawHash,
            List<AnnouncementSourceProviderAttachment> attachments,
            UUID localGovernmentSourceId
    ) {
        this(
                providerCode, providerNoticeId, title, agencyName, applicationStartDate, applicationEndDate,
                postedAt, modifiedAt, sourceUrl, bodyText, inquiryText, applicationMethodText,
                sourceCompletenessCode, missingFieldsJson, rawPayloadJson, rawHash, attachments,
                localGovernmentSourceId, "ACCEPTED", "PROVIDER_TRUSTED", null
        );
    }

    /**
     * 지자체 출처의 정적 의미 판정 결과를 적용한 새 항목을 반환합니다.
     *
     * @param statusCode 판정 상태
     * @param reasonCode 판정 사유
     * @param matchedKeywords 일치 키워드
     * @return 의미 판정이 적용된 항목
     */
    public AnnouncementSourceProviderItem withSemanticDecision(
            String statusCode,
            String reasonCode,
            String matchedKeywords
    ) {
        return new AnnouncementSourceProviderItem(
                providerCode, providerNoticeId, title, agencyName, applicationStartDate, applicationEndDate,
                postedAt, modifiedAt, sourceUrl, bodyText, inquiryText, applicationMethodText,
                sourceCompletenessCode, missingFieldsJson, rawPayloadJson, rawHash, attachments,
                localGovernmentSourceId, statusCode, reasonCode, matchedKeywords
        );
    }

    public AnnouncementSourceProviderItem withBodyText(String nextBodyText) {
        return new AnnouncementSourceProviderItem(
                providerCode, providerNoticeId, title, agencyName, applicationStartDate, applicationEndDate,
                postedAt, modifiedAt, sourceUrl, nextBodyText, inquiryText, applicationMethodText,
                sourceCompletenessCode, missingFieldsJson, rawPayloadJson, rawHash, attachments,
                localGovernmentSourceId, semanticStatusCode, semanticReasonCode, semanticMatchedKeywords
        );
    }
}
