/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: ConsultationService.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.consultation.service;

import com.saneb.common.response.PageResponse;
import com.saneb.domain.consultation.dto.ConsultationReservationCreateRequest;
import com.saneb.domain.consultation.dto.ConsultationReservationResponse;
import com.saneb.domain.consultation.dto.ConsultationReservationStatusUpdateRequest;
import com.saneb.domain.consultation.dto.ConsultationSlotCreateRequest;
import com.saneb.domain.consultation.dto.ConsultationSlotResponse;
import com.saneb.domain.consultation.dto.ConsultationSlotStatusUpdateRequest;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.security.core.Authentication;

public interface ConsultationService {

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param authentication 입력 값
     *
     * @param partnerUserId 입력 값
     *
     * @param statusCode 입력 값
     *
     * @param startFrom 입력 값
     *
     * @param startTo 입력 값
     *
     * @param page 입력 값
     *
     * @param size 입력 값
     *
     * @return 처리 결과
     */
    PageResponse<ConsultationSlotResponse> selectConsultationSlotList(
            Authentication authentication,
            UUID partnerUserId,
            String statusCode,
            OffsetDateTime startFrom,
            OffsetDateTime startTo,
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
    ConsultationSlotResponse insertConsultationSlot(Authentication authentication, ConsultationSlotCreateRequest request);

    /**
     * 업무 데이터를 수정합니다.
     *
     * @param authentication 입력 값
     *
     * @param slotId 입력 값
     *
     * @param request 입력 값
     *
     * @return 처리 결과
     */
    ConsultationSlotResponse updateConsultationSlotStatus(
            Authentication authentication,
            UUID slotId,
            ConsultationSlotStatusUpdateRequest request
    );

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param authentication 입력 값
     *
     * @param memberUserId 입력 값
     *
     * @param partnerUserId 입력 값
     *
     * @param statusCode 입력 값
     *
     * @param page 입력 값
     *
     * @param size 입력 값
     *
     * @return 처리 결과
     */
    PageResponse<ConsultationReservationResponse> selectConsultationReservationList(
            Authentication authentication,
            UUID memberUserId,
            UUID partnerUserId,
            String statusCode,
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
    ConsultationReservationResponse insertConsultationReservation(
            Authentication authentication,
            ConsultationReservationCreateRequest request
    );

    /**
     * 업무 데이터를 수정합니다.
     *
     * @param authentication 입력 값
     *
     * @param reservationId 입력 값
     *
     * @param request 입력 값
     *
     * @return 처리 결과
     */
    ConsultationReservationResponse updateConsultationReservationStatus(
            Authentication authentication,
            UUID reservationId,
            ConsultationReservationStatusUpdateRequest request
    );
}
