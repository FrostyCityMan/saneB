package com.saneb.domain.auth.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank(message = "아이디를 입력해 주세요.")
        @Size(min = 4, max = 100, message = "아이디는 4~100자로 입력해 주세요.")
        @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "아이디는 영문, 숫자, 마침표, 밑줄, 하이픈만 사용할 수 있습니다.")
        String loginId,
        @NotBlank(message = "비밀번호를 입력해 주세요.")
        @Size(min = 8, max = 16, message = "비밀번호는 8~16자로 입력해 주세요.")
        String password,
        @NotBlank(message = "비밀번호 확인을 입력해 주세요.")
        String passwordConfirm,
        @NotBlank(message = "이름을 입력해 주세요.")
        @Size(max = 100, message = "이름은 100자 이하로 입력해 주세요.")
        String name,
        @Size(max = 30, message = "휴대폰 번호는 30자 이하로 입력해 주세요.")
        String phone,
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        @Size(max = 255, message = "이메일은 255자 이하로 입력해 주세요.")
        String email,
        @NotNull(message = "이용약관에 동의해 주세요.")
        @AssertTrue(message = "이용약관에 동의해 주세요.")
        Boolean termsAgreed,
        @NotNull(message = "개인정보 처리방침에 동의해 주세요.")
        @AssertTrue(message = "개인정보 처리방침에 동의해 주세요.")
        Boolean privacyAgreed
) {
}
