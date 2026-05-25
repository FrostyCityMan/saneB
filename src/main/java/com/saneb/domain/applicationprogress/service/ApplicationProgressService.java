package com.saneb.domain.applicationprogress.service;

import com.saneb.common.response.PageResponse;
import com.saneb.domain.applicationprogress.dto.ApplicationProgressDetailsResponse;
import com.saneb.domain.applicationprogress.dto.ApplicationProgressStartRequest;
import com.saneb.domain.applicationprogress.dto.ApplicationProgressSummaryResponse;
import com.saneb.domain.applicationprogress.dto.ProgressActionRequest;
import com.saneb.domain.applicationprogress.dto.ProgressChecklistSaveRequest;
import com.saneb.domain.applicationprogress.dto.ProgressReceiptSaveRequest;
import com.saneb.domain.applicationprogress.dto.ProgressResultSaveRequest;
import java.util.UUID;
import org.springframework.security.core.Authentication;

public interface ApplicationProgressService {

    ApplicationProgressDetailsResponse insertApplicationProgress(
            Authentication authentication,
            ApplicationProgressStartRequest request
    );

    PageResponse<ApplicationProgressSummaryResponse> selectApplicationProgressList(
            Authentication authentication,
            UUID announcementId,
            UUID memberUserId,
            UUID matchingCaseId,
            String statusCode,
            int page,
            int size
    );

    ApplicationProgressDetailsResponse selectApplicationProgressDetails(UUID progressId);

    ApplicationProgressDetailsResponse updateProgressStepAction(
            Authentication authentication,
            UUID progressId,
            UUID stepId,
            ProgressActionRequest request
    );

    ApplicationProgressDetailsResponse saveProgressStepDocuments(
            Authentication authentication,
            UUID progressId,
            UUID stepId,
            ProgressChecklistSaveRequest request
    );

    ApplicationProgressDetailsResponse updateProgressReceipt(
            Authentication authentication,
            UUID progressId,
            ProgressReceiptSaveRequest request
    );

    ApplicationProgressDetailsResponse updateProgressResult(
            Authentication authentication,
            UUID progressId,
            ProgressResultSaveRequest request
    );
}
