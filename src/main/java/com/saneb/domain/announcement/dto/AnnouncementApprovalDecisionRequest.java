/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: AnnouncementApprovalDecisionRequest.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.announcement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnnouncementApprovalDecisionRequest(
        @NotBlank(message = "승인 처리 상태를 선택하세요.")
        String approvalStatusCode,
        @Size(max = 1000, message = "승인 처리 메모는 1000자 이하로 입력하세요.")
        String decisionNote
) {
}
