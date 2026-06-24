/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: DynamicAnnouncementInputService.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.dynamicinput.service;

import com.saneb.domain.dynamicinput.dto.AnnouncementInputRequirementsResponse;
import com.saneb.domain.dynamicinput.dto.AnnouncementInputRequirementsSaveRequest;
import com.saneb.domain.dynamicinput.dto.ApplicationInputValuesResponse;
import com.saneb.domain.dynamicinput.dto.ApplicationInputValuesSaveRequest;
import com.saneb.domain.dynamicinput.dto.StandardDocumentFieldResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;

public interface DynamicAnnouncementInputService {

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param announcementId 입력 값
     *
     * @return 처리 결과
     */
    AnnouncementInputRequirementsResponse selectAnnouncementInputRequirements(UUID announcementId);

    /**
     * 업무 데이터를 저장합니다.
     *
     * @param authentication 입력 값
     *
     * @param announcementId 입력 값
     *
     * @param request 입력 값
     *
     * @return 처리 결과
     */
    AnnouncementInputRequirementsResponse saveAnnouncementInputRequirements(
            Authentication authentication,
            UUID announcementId,
            AnnouncementInputRequirementsSaveRequest request
    );

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param authentication 입력 값
     *
     * @param progressId 입력 값
     *
     * @return 처리 결과
     */
    ApplicationInputValuesResponse selectApplicationInputValues(Authentication authentication, UUID progressId);

    /**
     * 업무 데이터를 저장합니다.
     *
     * @param authentication 입력 값
     *
     * @param progressId 입력 값
     *
     * @param request 입력 값
     *
     * @return 처리 결과
     */
    ApplicationInputValuesResponse saveApplicationInputValues(
            Authentication authentication,
            UUID progressId,
            ApplicationInputValuesSaveRequest request
    );

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param documentTypeCode 입력 값
     *
     * @param scopeCode 입력 값
     *
     * @return 처리 결과
     */
    List<StandardDocumentFieldResponse> selectStandardDocumentFieldList(
            String documentTypeCode,
            String scopeCode
    );
}
