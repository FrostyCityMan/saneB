package com.saneb.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordChangeRequest(
        @NotBlank(message = "현재 비밀번호를 입력해 주세요.")
        String currentPassword,
        @NotBlank(message = "새 비밀번호를 입력해 주세요.")
        @Size(min = 8, max = 16, message = "새 비밀번호는 8~16자로 입력해 주세요.")
        String newPassword
) {
}
