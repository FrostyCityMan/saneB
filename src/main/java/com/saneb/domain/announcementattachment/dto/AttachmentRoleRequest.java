package com.saneb.domain.announcementattachment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record AttachmentRoleRequest(
        @NotNull(message="역할을 변경할 현재 판정과 버전이 필요합니다.") @Valid AttachmentReviewRequests.Version version,
        @NotNull(message="현재 첨부 집합 ID가 필요합니다.") UUID expectedSetId,
        @NotEmpty(message="첨부 전체의 역할 목록이 필요합니다.") @Size(max=10,message="공고별 첨부 역할은 최대 10개입니다.") List<@Valid FileRole> fileRoles,
        @NotBlank(message="역할 변경 사유를 입력하세요.") @Size(max=1000,message="역할 변경 사유는 1000자 이하여야 합니다.") String reason) {
    public record FileRole(@NotNull(message="첨부 파일 ID가 필요합니다.") UUID fileId,
            @NotBlank(message="문서 역할을 선택하세요.") String documentRoleCode) { }
    @Override public String toString() { return "AttachmentRoleRequest[reason=REDACTED]"; }
}
