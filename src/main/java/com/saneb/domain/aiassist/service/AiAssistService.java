package com.saneb.domain.aiassist.service;

import com.saneb.common.response.PageResponse;
import com.saneb.domain.aiassist.dto.AiAssistCreateRequest;
import com.saneb.domain.aiassist.dto.AiAssistResponse;
import com.saneb.domain.aiassist.dto.AiAssistReviewRequest;
import java.util.UUID;
import org.springframework.security.core.Authentication;

public interface AiAssistService {

    AiAssistResponse insertAiAssistRequest(Authentication authentication, AiAssistCreateRequest request);

    PageResponse<AiAssistResponse> selectAiAssistRequestList(
            Authentication authentication,
            String assistTypeCode,
            String resourceType,
            String reviewStatusCode,
            int page,
            int size
    );

    AiAssistResponse selectAiAssistRequestDetails(Authentication authentication, UUID requestId);

    AiAssistResponse updateAiAssistResultReview(Authentication authentication, UUID resultId, AiAssistReviewRequest request);
}
