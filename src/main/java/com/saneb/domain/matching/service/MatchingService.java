package com.saneb.domain.matching.service;

import com.saneb.common.response.PageResponse;
import com.saneb.domain.matching.dto.MatchingCandidateGenerateRequest;
import com.saneb.domain.matching.dto.MatchingCandidateGenerateResponse;
import com.saneb.domain.matching.dto.MatchingCaseCreateRequest;
import com.saneb.domain.matching.dto.MatchingCaseDetailsResponse;
import com.saneb.domain.matching.dto.MatchingCaseStatusUpdateRequest;
import com.saneb.domain.matching.dto.MatchingCaseSummaryResponse;
import com.saneb.domain.matching.dto.MatchingMemberLookupResponse;
import com.saneb.domain.matching.dto.MatchingResultDetailResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;

public interface MatchingService {

    MatchingCaseDetailsResponse insertMatchingCase(
            Authentication authentication,
            MatchingCaseCreateRequest request
    );

    MatchingCandidateGenerateResponse insertMatchingCandidates(
            Authentication authentication,
            MatchingCandidateGenerateRequest request
    );

    PageResponse<MatchingCaseSummaryResponse> selectMatchingCaseList(
            UUID announcementId,
            UUID memberUserId,
            UUID verificationId,
            String statusCode,
            int page,
            int size
    );

    PageResponse<MatchingMemberLookupResponse> selectMatchingMemberLookupList(
            String keyword,
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
