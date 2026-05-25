package com.saneb.domain.matching.service;

import com.saneb.common.response.PageResponse;
import com.saneb.domain.matching.dto.MatchingCaseCreateRequest;
import com.saneb.domain.matching.dto.MatchingCaseDetailsResponse;
import com.saneb.domain.matching.dto.MatchingCaseStatusUpdateRequest;
import com.saneb.domain.matching.dto.MatchingCaseSummaryResponse;
import com.saneb.domain.matching.dto.MatchingResultDetailResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;

public interface MatchingService {

    MatchingCaseDetailsResponse insertMatchingCase(
            Authentication authentication,
            MatchingCaseCreateRequest request
    );

    PageResponse<MatchingCaseSummaryResponse> selectMatchingCaseList(
            UUID announcementId,
            UUID memberUserId,
            UUID verificationId,
            String statusCode,
            int page,
            int size
    );

    MatchingCaseDetailsResponse selectMatchingCaseDetails(UUID matchingCaseId);

    List<MatchingResultDetailResponse> selectMatchingResultDetailList(UUID matchingCaseId);

    MatchingCaseDetailsResponse updateMatchingCaseStatus(
            Authentication authentication,
            UUID matchingCaseId,
            MatchingCaseStatusUpdateRequest request
    );
}
