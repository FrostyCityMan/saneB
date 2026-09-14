package com.saneb.domain.announcementattachment.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.*;

public final class AttachmentProviderQaRequests {
    private AttachmentProviderQaRequests() { }
    public record Reservation(
            @NotNull(message="정책 조회 버전이 필요합니다.") @Min(value=0,message="정책 조회 버전은 0 이상이어야 합니다.") Integer expectedVersion,
            @NotBlank(message="조회한 snapshot 지문이 필요합니다.") @Pattern(regexp="[0-9a-f]{64}",message="조회한 snapshot 지문 64자리가 필요합니다.") String expectedSnapshotHash,
            @NotBlank(message="조회한 catalog 지문이 필요합니다.") @Pattern(regexp="[0-9a-f]{64}",message="조회한 catalog 지문 64자리가 필요합니다.") String expectedCatalogHash,
            @NotBlank(message="조회한 실행 계획 지문이 필요합니다.") @Pattern(regexp="[0-9a-f]{64}",message="조회한 실행 계획 지문 64자리가 필요합니다.") String expectedPlanHash,
            @NotNull(message="분할 번호가 필요합니다.") @Min(value=1,message="분할 번호는 1 이상이어야 합니다.") @Max(value=10000,message="분할 번호는 10000 이하여야 합니다.") Integer segmentNo,
            @NotNull(message="분할 공고 수가 필요합니다.") @Min(value=1,message="공고 수는 1 이상이어야 합니다.") @Max(value=10000,message="공고 수는 10000 이하여야 합니다.") Integer expectedCaseCount,
            @NotNull(message="최대 요청 횟수가 필요합니다.") @Min(value=1,message="최대 요청 횟수는 1 이상이어야 합니다.") @Max(value=440000,message="최대 요청 횟수는 440000 이하여야 합니다.") Long maximumRequests,
            @NotNull(message="최대 다운로드 바이트가 필요합니다.") @Min(value=1,message="최대 다운로드 바이트는 1 이상이어야 합니다.") @Max(value=838860800000L,message="최대 다운로드 바이트는 838860800000 이하여야 합니다.") Long maximumBytes,
            @NotNull(message="여유 포함 최대 시간이 필요합니다.") @Min(value=61,message="여유 포함 최대 시간은 61초 이상이어야 합니다.") @Max(value=82800,message="여유 포함 최대 시간은 82800초 이하여야 합니다.") Integer maximumSecondsIncludingMargin,
            @NotNull(message="분할 범위를 확인해야 합니다.") @AssertTrue(message="고정된 분할 공고 범위를 확인해야 합니다.") Boolean acknowledgeScope,
            @NotNull(message="외부 요청 예산을 확인해야 합니다.") @AssertTrue(message="외부 요청 횟수와 다운로드 최대량을 확인해야 합니다.") Boolean acknowledgeNetworkBudget,
            @NotNull(message="전체 기대값 미완료 인지 여부를 지정해야 합니다.") Boolean acknowledgeIncompleteCoverage,
            @NotBlank(message="QA 예약 사유를 입력하세요.") @Size(max=1000,message="예약 사유는 1000자 이하여야 합니다.") String reason) {
        @JsonAnySetter public void rejectUnknown(String key,Object value){throw new IllegalArgumentException("정의하지 않은 Provider QA 예약 입력입니다.");}
        @Override public String toString(){return "ProviderQaReservation[reason=REDACTED]";}
    }
}
