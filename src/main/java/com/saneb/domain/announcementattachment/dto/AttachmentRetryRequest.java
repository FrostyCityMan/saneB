package com.saneb.domain.announcementattachment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.UUID;

public record AttachmentRetryRequest(
        @NotNull(message="재시도할 현재 판정과 버전이 필요합니다.") @Valid AttachmentReviewRequests.Version version,
        @NotNull(message="현재 첨부 집합 ID가 필요합니다.") UUID expectedSetId,
        @NotEmpty(message="재시도할 실패 파일을 선택하세요.") @Size(max=10,message="실패 파일은 최대 10개까지 선택할 수 있습니다.")
        List<@NotNull(message="실패 파일 ID가 필요합니다.") UUID> fileIds,
        @NotNull(message="이번 작업의 다운로드 상한을 확인하세요.") @Min(value=1,message="다운로드 상한은 1바이트 이상이어야 합니다.")
        @Max(value=83886080,message="공고별 다운로드 상한은 80 MiB 이하여야 합니다.") Long maximumDownloadBytes,
        @NotBlank(message="재시도 사유를 입력하세요.") @Size(max=1000,message="재시도 사유는 1000자 이하여야 합니다.") String reason) {
    @Override public String toString() { return "AttachmentRetryRequest[reason=REDACTED]"; }
}
