package com.saneb.domain.announcement.dto;

import jakarta.validation.constraints.Size;

public record AnnouncementApprovalRequestCreateRequest(
        @Size(max = 1000, message = "승인 요청 메모는 1000자 이하로 입력하세요.")
        String requestNote
) {
}
