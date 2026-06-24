/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: CandidatePreviewController.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.candidatepreview.controller;

import com.saneb.common.response.ApiResponse;
import com.saneb.domain.candidatepreview.dto.CandidatePreviewRequest;
import com.saneb.domain.candidatepreview.dto.CandidatePreviewResponse;
import com.saneb.domain.candidatepreview.service.CandidatePreviewService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pre-signup")
public class CandidatePreviewController {

    private final CandidatePreviewService candidatePreviewService;

    public CandidatePreviewController(CandidatePreviewService candidatePreviewService) {
        this.candidatePreviewService = candidatePreviewService;
    }

    @PostMapping("/candidate-preview")
    public ApiResponse<CandidatePreviewResponse> selectCandidatePreview(
            @Valid @RequestBody CandidatePreviewRequest request
    ) {
        return ApiResponse.success(candidatePreviewService.selectCandidatePreview(request));
    }
}
