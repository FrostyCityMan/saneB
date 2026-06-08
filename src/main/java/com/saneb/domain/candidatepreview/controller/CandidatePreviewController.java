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
