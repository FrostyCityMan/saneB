/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: AnnouncementSourceDuplicateDecisionRequest.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.announcementsource.dto;

import jakarta.validation.constraints.Size;

public record AnnouncementSourceDuplicateDecisionRequest(
        @Size(max = 40, message = "결정 action 코드는 40자 이하여야 합니다.")
        String decisionActionCode,

        @Size(max = 30, message = "대상 유형 코드는 30자 이하여야 합니다.")
        String targetTypeCode,

        @Size(max = 40, message = "소득 판단 기준 코드는 40자 이하여야 합니다.")
        String incomeJudgementCode,

        @Size(max = 500, message = "검수 메모는 500자 이하여야 합니다.")
        String decisionNote
) {
}
