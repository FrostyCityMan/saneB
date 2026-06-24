/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: PasswordChangeRequest.java
 * 작성자: 김도훈
 *
 */

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
