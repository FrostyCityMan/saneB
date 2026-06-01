package com.saneb.domain.auth.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank(message = "loginId is required")
        @Size(min = 4, max = 100, message = "loginId must be between 4 and 100 characters")
        @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "loginId contains invalid characters")
        String loginId,
        @NotBlank(message = "password is required")
        @Size(min = 8, max = 100, message = "password must be between 8 and 100 characters")
        String password,
        @NotBlank(message = "passwordConfirm is required")
        String passwordConfirm,
        @NotBlank(message = "name is required")
        @Size(max = 100, message = "name must be 100 characters or less")
        String name,
        @Size(max = 30, message = "phone must be 30 characters or less")
        String phone,
        @Email(message = "email must be valid")
        @Size(max = 255, message = "email must be 255 characters or less")
        String email,
        @NotNull(message = "termsAgreed is required")
        @AssertTrue(message = "termsAgreed must be true")
        Boolean termsAgreed,
        @NotNull(message = "privacyAgreed is required")
        @AssertTrue(message = "privacyAgreed must be true")
        Boolean privacyAgreed
) {
}
