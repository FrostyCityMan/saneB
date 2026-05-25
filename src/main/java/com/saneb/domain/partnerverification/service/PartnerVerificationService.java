package com.saneb.domain.partnerverification.service;

import com.saneb.common.response.PageResponse;
import com.saneb.domain.partnerverification.dto.PartnerVerificationCreateRequest;
import com.saneb.domain.partnerverification.dto.PartnerVerificationDetailsResponse;
import com.saneb.domain.partnerverification.dto.PartnerVerificationStatusUpdateRequest;
import com.saneb.domain.partnerverification.dto.PartnerVerificationSummaryResponse;
import com.saneb.domain.partnerverification.dto.VerificationBusinessValuesSaveRequest;
import com.saneb.domain.partnerverification.dto.VerificationDocumentsSaveRequest;
import com.saneb.domain.partnerverification.dto.VerificationFamilyValuesSaveRequest;
import com.saneb.domain.partnerverification.dto.VerificationMemberValuesSaveRequest;
import com.saneb.domain.partnerverification.dto.VerificationRestrictionFlagsSaveRequest;
import java.util.UUID;
import org.springframework.security.core.Authentication;

public interface PartnerVerificationService {

    PageResponse<PartnerVerificationSummaryResponse> selectPartnerVerificationList(
            UUID memberUserId,
            UUID partnerUserId,
            String statusCode,
            Boolean current,
            int page,
            int size
    );

    PartnerVerificationDetailsResponse insertPartnerVerification(
            Authentication authentication,
            PartnerVerificationCreateRequest request
    );

    PartnerVerificationDetailsResponse selectPartnerVerificationDetails(UUID verificationId);

    void updateVerificationMemberValues(
            Authentication authentication,
            UUID verificationId,
            VerificationMemberValuesSaveRequest request
    );

    void updateVerificationBusinessValues(
            Authentication authentication,
            UUID verificationId,
            VerificationBusinessValuesSaveRequest request
    );

    void updateVerificationFamilyValues(
            Authentication authentication,
            UUID verificationId,
            VerificationFamilyValuesSaveRequest request
    );

    void updateVerificationDocuments(
            Authentication authentication,
            UUID verificationId,
            VerificationDocumentsSaveRequest request
    );

    void updateVerificationRestrictionFlags(
            Authentication authentication,
            UUID verificationId,
            VerificationRestrictionFlagsSaveRequest request
    );

    PartnerVerificationDetailsResponse updatePartnerVerificationStatus(
            Authentication authentication,
            UUID verificationId,
            PartnerVerificationStatusUpdateRequest request
    );
}
