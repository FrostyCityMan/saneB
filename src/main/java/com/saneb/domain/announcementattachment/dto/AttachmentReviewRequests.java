package com.saneb.domain.announcementattachment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public final class AttachmentReviewRequests {
    private AttachmentReviewRequests() { }
    public record Version(
            @NotNull(message="기본 판정 ID가 필요합니다.") UUID expectedBaseDecisionId,
            @NotNull(message="첨부 판정 ID가 필요합니다.") UUID expectedAttachmentDecisionId,
            @NotNull(message="원문 버전이 필요합니다.") @PositiveOrZero(message="원문 버전은 0 이상이어야 합니다.") Integer expectedSourceVersion,
            @NotNull(message="첨부 버전이 필요합니다.") @PositiveOrZero(message="첨부 버전은 0 이상이어야 합니다.") Integer expectedAttachmentVersion,
            @NotBlank(message="첨부 집합 hash가 필요합니다.") @Pattern(regexp="[0-9a-f]{64}",message="첨부 집합 hash는 SHA-256 소문자 64자리여야 합니다.") String expectedSetHash) { }
    public record Confirmation(
            @NotNull(message="확인할 판정과 버전이 필요합니다.") @Valid Version version,
            @NotEmpty(message="지원대상을 한 개 이상 선택하세요.") @Size(max=5,message="지원대상은 최대 5개입니다.") List<@NotBlank String> targetCategoryCodes,
            @NotEmpty(message="지원형태를 한 개 이상 선택하세요.") @Size(max=7,message="지원형태는 최대 7개입니다.") List<@NotBlank String> supportTypeCodes,
            @NotBlank(message="검수 확인 방법을 선택하세요.") String reviewMethodCode,
            @NotNull(message="확인한 실패·검수 사유 목록이 필요합니다.") @Size(max=100,message="확인 사유는 최대 100개입니다.") List<@NotBlank @Size(max=80) String> acknowledgedErrorCodes,
            @NotBlank(message="직접 확인한 내용과 검수 사유를 입력하세요.") @Size(max=1000,message="검수 사유는 1000자 이하여야 합니다.") String reviewNote) {
        @Override public String toString() { return "AttachmentConfirmation[reviewNote=REDACTED]"; }
    }
    public record Conversion(
            @NotNull(message="전환할 판정과 버전이 필요합니다.") @Valid Version version,
            @NotNull(message="현재 검수 확인 ID가 필요합니다.") UUID expectedConfirmationId,
            @NotBlank(message="대표 지원대상을 선택하세요.") @Size(max=30) String primaryTargetCategoryCode,
            @Size(max=50) String incomeJudgementCode) { }
}
