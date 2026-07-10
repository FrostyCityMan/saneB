/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: AnnouncementSourceDuplicateCandidateResponse.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.announcementsource.dto;

import com.saneb.domain.announcementsource.vo.AnnouncementSourceDuplicateCandidateRow;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AnnouncementSourceDuplicateCandidateResponse(
        UUID candidateId,
        UUID sourceId,
        UUID announcementId,
        String announcementCode,
        String announcementTitle,
        String agencyName,
        LocalDate applicationStartDate,
        LocalDate applicationEndDate,
        String matchTypeCode,
        String matchTypeLabel,
        boolean titleMatched,
        boolean agencyMatched,
        boolean providerNoticeMatched,
        boolean periodMatched,
        boolean sourceUrlMatched,
        String similarityReason,
        String decisionStatusCode,
        String decisionStatusLabel,
        String linkedSourceCode,
        String linkedProviderNoticeId,
        String linkedSourceUrl,
        OffsetDateTime decidedAt
) {

    /**
     * 업무 데이터를 응답 형식으로 변환합니다.
     *
     * @param row 입력 값
     *
     * @return 처리 결과
     */
    public static AnnouncementSourceDuplicateCandidateResponse from(AnnouncementSourceDuplicateCandidateRow row) {
        return new AnnouncementSourceDuplicateCandidateResponse(
                row.candidateId(),
                row.sourceId(),
                row.announcementId(),
                row.announcementCode(),
                row.announcementTitle(),
                row.agencyName(),
                row.applicationStartDate(),
                row.applicationEndDate(),
                row.matchTypeCode(),
                matchTypeLabel(row.matchTypeCode()),
                row.titleMatched(),
                row.agencyMatched(),
                row.providerNoticeMatched(),
                row.periodMatched(),
                row.sourceUrlMatched(),
                row.similarityReason(),
                row.decisionStatusCode(),
                decisionStatusLabel(row.decisionStatusCode()),
                row.linkedSourceCode(),
                row.linkedProviderNoticeId(),
                row.linkedSourceUrl(),
                row.decidedAt()
        );
    }

    /**
     * 중복 유형 한글명을 반환합니다.
     *
     * @param code 입력 값
     *
     * @return 처리 결과
     */
    private static String matchTypeLabel(String code) {
        if ("EXACT_DUPLICATE".equals(code)) {
            return "동일 공고";
        }
        if ("SIMILAR".equals(code)) {
            return "유사 공고";
        }
        return code;
    }

    /**
     * 결정 상태 한글명을 반환합니다.
     *
     * @param code 입력 값
     *
     * @return 처리 결과
     */
    private static String decisionStatusLabel(String code) {
        if ("PENDING".equals(code)) {
            return "검수 필요";
        }
        if ("CREATE_NEW_SELECTED".equals(code)) {
            return "신규 등록 선택";
        }
        if ("UPDATE_EXISTING_SELECTED".equals(code)) {
            return "기존 공고 업데이트 선택";
        }
        if ("IGNORED".equals(code)) {
            return "무시";
        }
        return code;
    }
}
