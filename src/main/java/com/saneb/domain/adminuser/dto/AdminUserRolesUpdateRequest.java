package com.saneb.domain.adminuser.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record AdminUserRolesUpdateRequest(
        @NotEmpty(message = "권한을 하나 이상 선택하세요.")
        List<@NotBlank(message = "권한 값이 비어 있습니다.") String> roleCodes
) {
}
