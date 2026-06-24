/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: PartnerVerificationService.java
 * 작성자: 김도훈
 *
 */

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

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param memberUserId 입력 값
     *
     * @param partnerUserId 입력 값
     *
     * @param statusCode 입력 값
     *
     * @param current 입력 값
     *
     * @param page 입력 값
     *
     * @param size 입력 값
     *
     * @return 처리 결과
     */
    PageResponse<PartnerVerificationSummaryResponse> selectPartnerVerificationList(
            UUID memberUserId,
            UUID partnerUserId,
            String statusCode,
            Boolean current,
            int page,
            int size
    );

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param authentication 입력 값
     *
     * @param request 입력 값
     *
     * @return 처리 결과
     */
    PartnerVerificationDetailsResponse insertPartnerVerification(
            Authentication authentication,
            PartnerVerificationCreateRequest request
    );

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param verificationId 입력 값
     *
     * @return 처리 결과
     */
    PartnerVerificationDetailsResponse selectPartnerVerificationDetails(UUID verificationId);

    /**
     * 업무 데이터를 수정합니다.
     *
     * @param authentication 입력 값
     *
     * @param verificationId 입력 값
     *
     * @param request 입력 값
     */
    void updateVerificationMemberValues(
            Authentication authentication,
            UUID verificationId,
            VerificationMemberValuesSaveRequest request
    );

    /**
     * 업무 데이터를 수정합니다.
     *
     * @param authentication 입력 값
     *
     * @param verificationId 입력 값
     *
     * @param request 입력 값
     */
    void updateVerificationBusinessValues(
            Authentication authentication,
            UUID verificationId,
            VerificationBusinessValuesSaveRequest request
    );

    /**
     * 업무 데이터를 수정합니다.
     *
     * @param authentication 입력 값
     *
     * @param verificationId 입력 값
     *
     * @param request 입력 값
     */
    void updateVerificationFamilyValues(
            Authentication authentication,
            UUID verificationId,
            VerificationFamilyValuesSaveRequest request
    );

    /**
     * 업무 데이터를 수정합니다.
     *
     * @param authentication 입력 값
     *
     * @param verificationId 입력 값
     *
     * @param request 입력 값
     */
    void updateVerificationDocuments(
            Authentication authentication,
            UUID verificationId,
            VerificationDocumentsSaveRequest request
    );

    /**
     * 업무 데이터를 수정합니다.
     *
     * @param authentication 입력 값
     *
     * @param verificationId 입력 값
     *
     * @param request 입력 값
     */
    void updateVerificationRestrictionFlags(
            Authentication authentication,
            UUID verificationId,
            VerificationRestrictionFlagsSaveRequest request
    );

    /**
     * 업무 데이터를 수정합니다.
     *
     * @param authentication 입력 값
     *
     * @param verificationId 입력 값
     *
     * @param request 입력 값
     *
     * @return 처리 결과
     */
    PartnerVerificationDetailsResponse updatePartnerVerificationStatus(
            Authentication authentication,
            UUID verificationId,
            PartnerVerificationStatusUpdateRequest request
    );
}
