package com.saneb.domain.announcementsource.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

public record AnnouncementSourceBaseReviewRequest(
        @NotNull @Valid AnnouncementSourceV2ToAnnouncementRequest classification,
        @NotBlank(message = "자료를 확인한 검수 사유를 입력하세요.") @Size(max = 1000) String reviewNote,
        @Pattern(regexp = "EXTRACTED_TEXT|MANUAL_SOURCE_CHECK", message = "검수 방법은 추출 텍스트 확인 또는 원문 전체 직접 확인을 선택하세요.")
        String reviewMethodCode
) {
    public AnnouncementSourceBaseReviewRequest(AnnouncementSourceV2ToAnnouncementRequest classification, String reviewNote) {
        this(classification, reviewNote, null);
    }
}
