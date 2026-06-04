package com.saneb.domain.adminuser.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminUserStatusUpdateRequest(
        @NotBlank(message = "계정 상태를 선택하세요.")
        String statusCode
) {
}
