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
