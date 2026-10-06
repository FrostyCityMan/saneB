package com.saneb.domain.announcementsource.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AnnouncementSourceBaseReviewRequest(
        @NotNull @Valid AnnouncementSourceV2ToAnnouncementRequest classification,
        @NotBlank(message = "자료를 확인한 검수 사유를 입력하세요.") @Size(max = 1000) String reviewNote
) { }
