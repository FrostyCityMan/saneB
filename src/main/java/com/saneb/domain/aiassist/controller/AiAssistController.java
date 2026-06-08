package com.saneb.domain.aiassist.controller;

import com.saneb.common.response.ApiResponse;
import com.saneb.common.response.PageResponse;
import com.saneb.domain.aiassist.dto.AiAssistCreateRequest;
import com.saneb.domain.aiassist.dto.AiAssistResponse;
import com.saneb.domain.aiassist.dto.AiAssistReviewRequest;
import com.saneb.domain.aiassist.service.AiAssistService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai-assist")
public class AiAssistController {

    private final AiAssistService aiAssistService;

    public AiAssistController(AiAssistService aiAssistService) {
        this.aiAssistService = aiAssistService;
    }

    @PostMapping("/requests")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ApiResponse<AiAssistResponse> insertAiAssistRequest(
            Authentication authentication,
            @Valid @RequestBody AiAssistCreateRequest request
    ) {
        return ApiResponse.success(aiAssistService.insertAiAssistRequest(authentication, request));
    }

    @GetMapping("/requests")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ApiResponse<PageResponse<AiAssistResponse>> selectAiAssistRequestList(
            Authentication authentication,
            @RequestParam(required = false) String assistTypeCode,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String reviewStatusCode,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(aiAssistService.selectAiAssistRequestList(
                authentication,
                assistTypeCode,
                resourceType,
                reviewStatusCode,
                page,
                size
        ));
    }

    @GetMapping("/requests/{requestId}")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ApiResponse<AiAssistResponse> selectAiAssistRequestDetails(
            Authentication authentication,
            @PathVariable UUID requestId
    ) {
        return ApiResponse.success(aiAssistService.selectAiAssistRequestDetails(authentication, requestId));
    }

    @PatchMapping("/results/{resultId}/review")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ApiResponse<AiAssistResponse> updateAiAssistResultReview(
            Authentication authentication,
            @PathVariable UUID resultId,
            @Valid @RequestBody AiAssistReviewRequest request
    ) {
        return ApiResponse.success(aiAssistService.updateAiAssistResultReview(authentication, resultId, request));
    }
}
