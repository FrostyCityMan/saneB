/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: AnnouncementSourceDetailsResponse.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.announcementsource.dto;

import com.saneb.domain.announcementsource.vo.AnnouncementSourceSnapshotRow;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record AnnouncementSourceDetailsResponse(
        UUID sourceId,
        String publicCode,
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
        String reviewStatusCode,
        String semanticStatusCode,
        String semanticReasonCode,
        String semanticMatchedKeywords,
        OffsetDateTime collectedAt,
        List<AnnouncementSourceAttachmentResponse> attachments,
        List<AnnouncementSourceHighlightResponse> highlights,
        List<AnnouncementSourceDuplicateCandidateResponse> duplicateCandidates,
        List<AnnouncementSourceSnapshotDuplicateResponse> sourceDuplicates
) {

    /**
     * 업무 데이터를 응답 형식으로 변환합니다.
     *
     * @param row 입력 값
     *
     * @param attachments 입력 값
     *
     * @param highlights 입력 값
     *
     * @param duplicateCandidates 입력 값
     *
     * @param sourceDuplicates 교차 제공자 중복 후보
     *
     * @return 처리 결과
     */
    public static AnnouncementSourceDetailsResponse from(
            AnnouncementSourceSnapshotRow row,
            List<AnnouncementSourceAttachmentResponse> attachments,
            List<AnnouncementSourceHighlightResponse> highlights,
            List<AnnouncementSourceDuplicateCandidateResponse> duplicateCandidates,
            List<AnnouncementSourceSnapshotDuplicateResponse> sourceDuplicates
    ) {
        return new AnnouncementSourceDetailsResponse(
                row.sourceId(),
                row.publicCode(),
                row.providerCode(),
                row.providerNoticeId(),
                row.title(),
                row.agencyName(),
                row.applicationStartDate(),
                row.applicationEndDate(),
                row.postedAt(),
                row.modifiedAt(),
                row.sourceUrl(),
                row.bodyText(),
                row.inquiryText(),
                row.applicationMethodText(),
                row.sourceCompletenessCode(),
                row.missingFieldsJson(),
                row.reviewStatusCode(),
                row.semanticStatusCode(),
                row.semanticReasonCode(),
                row.semanticMatchedKeywords(),
                row.collectedAt(),
                attachments,
                highlights,
                duplicateCandidates,
                sourceDuplicates
        );
    }
}
