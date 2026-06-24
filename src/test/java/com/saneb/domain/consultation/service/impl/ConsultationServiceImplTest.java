/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: ConsultationServiceImplTest.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.consultation.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.saneb.common.error.ApiException;
import com.saneb.common.error.ErrorCode;
import com.saneb.domain.auth.vo.AuthUserDetailsRow;
import com.saneb.domain.auth.vo.AuthenticatedUserDetails;
import com.saneb.domain.consultation.dao.ConsultationDao;
import com.saneb.domain.consultation.dto.ConsultationReservationCreateRequest;
import com.saneb.domain.consultation.dto.ConsultationReservationStatusUpdateRequest;
import com.saneb.domain.consultation.dto.ConsultationSlotCreateRequest;
import com.saneb.domain.consultation.vo.ConsultationHistoryCommand;
import com.saneb.domain.consultation.vo.ConsultationReservationInsertCommand;
import com.saneb.domain.consultation.vo.ConsultationReservationRow;
import com.saneb.domain.consultation.vo.ConsultationReservationStatusCommand;
import com.saneb.domain.consultation.vo.ConsultationSlotInsertCommand;
import com.saneb.domain.consultation.vo.ConsultationSlotRow;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class ConsultationServiceImplTest {

    private static final UUID USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000002");
    private static final UUID PARTNER_USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000003");
    private static final UUID SLOT_ID = UUID.fromString("85000000-0000-0000-0000-000000000001");
    private static final UUID RESERVATION_ID = UUID.fromString("85000000-0000-0000-0000-000000000002");
    private static final OffsetDateTime START_AT = OffsetDateTime.parse("2026-06-20T10:00:00+09:00");
    private static final OffsetDateTime END_AT = OffsetDateTime.parse("2026-06-20T10:30:00+09:00");

    @Mock
    private ConsultationDao consultationDao;

    private ConsultationServiceImpl consultationService;

    /**
     * 업무 처리를 수행합니다.
     */
    @BeforeEach
    void setUp() {
        consultationService = new ConsultationServiceImpl(consultationDao);
    }

    /**
     * 업무 데이터를 등록합니다.
     */
    @Test
    void insertConsultationSlotUsesPartnerActorWhenPartnerCreatesSlot() {
        when(consultationDao.selectConsultationSlotDetails(any())).thenReturn(slot("OPEN"));

        var response = consultationService.insertConsultationSlot(
                authentication(PARTNER_USER_ID, List.of("PARTNER")),
                new ConsultationSlotCreateRequest(null, START_AT, END_AT, "오전 상담")
        );

        ArgumentCaptor<ConsultationSlotInsertCommand> captor =
                ArgumentCaptor.forClass(ConsultationSlotInsertCommand.class);
        verify(consultationDao).insertConsultationSlot(captor.capture());
        assertThat(captor.getValue().partnerUserId()).isEqualTo(PARTNER_USER_ID);
        assertThat(response.statusCode()).isEqualTo("OPEN");
    }

    /**
     * 업무 데이터를 등록합니다.
     */
    @Test
    void insertConsultationSlotRejectsInvalidTime() {
        assertThatThrownBy(() -> consultationService.insertConsultationSlot(
                authentication(PARTNER_USER_ID, List.of("PARTNER")),
                new ConsultationSlotCreateRequest(null, END_AT, START_AT, null)
        ))
                .isInstanceOf(ApiException.class)
                .extracting(error -> ((ApiException) error).errorCode())
                .isEqualTo(ErrorCode.VALIDATION_FAILED);
    }

    /**
     * 업무 데이터를 등록합니다.
     */
    @Test
    void insertConsultationReservationCreatesSlotlessUserRequest() {
        when(consultationDao.selectConsultationReservationDetails(any())).thenReturn(slotlessReservation("REQUESTED"));

        var response = consultationService.insertConsultationReservation(
                authentication(USER_ID, List.of("USER")),
                new ConsultationReservationCreateRequest(null, null, null, null, null, null, null, "전화 상담 희망")
        );

        ArgumentCaptor<ConsultationReservationInsertCommand> reservationCaptor =
                ArgumentCaptor.forClass(ConsultationReservationInsertCommand.class);
        verify(consultationDao).insertConsultationReservation(reservationCaptor.capture());
        assertThat(reservationCaptor.getValue().memberUserId()).isEqualTo(USER_ID);
        assertThat(reservationCaptor.getValue().slotId()).isNull();
        assertThat(reservationCaptor.getValue().partnerUserId()).isNull();
        verify(consultationDao, never()).updateConsultationSlotStatus(any());
        verify(consultationDao).insertConsultationHistory(any());
        verify(consultationDao).insertAuditLog(any());
        assertThat(response.statusCode()).isEqualTo("REQUESTED");
    }

    /**
     * 업무 데이터를 등록합니다.
     */
    @Test
    void insertConsultationReservationAllowsOperatorToAssignSlot() {
        when(consultationDao.selectConsultationSlotDetails(SLOT_ID)).thenReturn(slot("OPEN"));
        when(consultationDao.selectUserCount(PARTNER_USER_ID)).thenReturn(1L);
        when(consultationDao.selectConsultationReservationDetails(any())).thenReturn(reservation("REQUESTED"));

        var response = consultationService.insertConsultationReservation(
                authentication(USER_ID, List.of("OPERATOR")),
                new ConsultationReservationCreateRequest(SLOT_ID, USER_ID, null, null, null, null, null, "전화 상담 희망")
        );

        ArgumentCaptor<ConsultationReservationInsertCommand> reservationCaptor =
                ArgumentCaptor.forClass(ConsultationReservationInsertCommand.class);
        verify(consultationDao).insertConsultationReservation(reservationCaptor.capture());
        assertThat(reservationCaptor.getValue().memberUserId()).isEqualTo(USER_ID);
        assertThat(reservationCaptor.getValue().slotId()).isEqualTo(SLOT_ID);
        assertThat(reservationCaptor.getValue().partnerUserId()).isEqualTo(PARTNER_USER_ID);
        verify(consultationDao).updateConsultationSlotStatus(any());
        assertThat(response.statusCode()).isEqualTo("REQUESTED");
    }

    /**
     * 업무 데이터를 수정합니다.
     */
    @Test
    void updateConsultationReservationStatusAllowsPartnerToConfirm() {
        when(consultationDao.selectConsultationReservationDetails(RESERVATION_ID))
                .thenReturn(reservation("REQUESTED"), reservation("CONFIRMED"));
        when(consultationDao.updateConsultationReservationStatus(any())).thenReturn(1);

        var response = consultationService.updateConsultationReservationStatus(
                authentication(PARTNER_USER_ID, List.of("PARTNER")),
                RESERVATION_ID,
                new ConsultationReservationStatusUpdateRequest("confirmed", null, null, null, "확정")
        );

        ArgumentCaptor<ConsultationReservationStatusCommand> statusCaptor =
                ArgumentCaptor.forClass(ConsultationReservationStatusCommand.class);
        verify(consultationDao).updateConsultationReservationStatus(statusCaptor.capture());
        assertThat(statusCaptor.getValue().statusCode()).isEqualTo("CONFIRMED");
        ArgumentCaptor<ConsultationHistoryCommand> historyCaptor =
                ArgumentCaptor.forClass(ConsultationHistoryCommand.class);
        verify(consultationDao).insertConsultationHistory(historyCaptor.capture());
        assertThat(historyCaptor.getValue().beforeStatusCode()).isEqualTo("REQUESTED");
        assertThat(response.statusCode()).isEqualTo("CONFIRMED");
    }

    /**
     * 업무 데이터를 수정합니다.
     */
    @Test
    void updateConsultationReservationStatusRejectsUserConfirm() {
        when(consultationDao.selectConsultationReservationDetails(RESERVATION_ID)).thenReturn(reservation("REQUESTED"));

        assertThatThrownBy(() -> consultationService.updateConsultationReservationStatus(
                authentication(USER_ID, List.of("USER")),
                RESERVATION_ID,
                new ConsultationReservationStatusUpdateRequest("CONFIRMED", null, null, null, null)
        ))
                .isInstanceOf(ApiException.class)
                .extracting(error -> ((ApiException) error).errorCode())
                .isEqualTo(ErrorCode.AUTH_FORBIDDEN);
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param statusCode 입력 값
     *
     * @return 처리 결과
     */
    private ConsultationSlotRow slot(String statusCode) {
        return new ConsultationSlotRow(
                SLOT_ID,
                PARTNER_USER_ID,
                START_AT,
                END_AT,
                statusCode,
                "오전 상담",
                START_AT.minusDays(1),
                START_AT.minusDays(1)
        );
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param statusCode 입력 값
     *
     * @return 처리 결과
     */
    private ConsultationReservationRow reservation(String statusCode) {
        return new ConsultationReservationRow(
                RESERVATION_ID,
                "CNS-000001",
                SLOT_ID,
                USER_ID,
                "USR-000001",
                PARTNER_USER_ID,
                "USR-000002",
                null,
                null,
                null,
                null,
                START_AT,
                END_AT,
                statusCode,
                "전화 상담 희망",
                "CONFIRMED".equals(statusCode) ? "확정" : null,
                "CONFIRMED".equals(statusCode) ? START_AT.minusDays(1) : null,
                null,
                null,
                START_AT.minusDays(1),
                START_AT.minusDays(1)
        );
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param statusCode 입력 값
     *
     * @return 처리 결과
     */
    private ConsultationReservationRow slotlessReservation(String statusCode) {
        return new ConsultationReservationRow(
                RESERVATION_ID,
                "CNS-000001",
                null,
                USER_ID,
                "USR-000001",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                statusCode,
                "전화 상담 희망",
                null,
                null,
                null,
                null,
                START_AT.minusDays(1),
                START_AT.minusDays(1)
        );
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param userId 입력 값
     *
     * @param roles 입력 값
     *
     * @return 처리 결과
     */
    private UsernamePasswordAuthenticationToken authentication(UUID userId, List<String> roles) {
        AuthenticatedUserDetails principal = new AuthenticatedUserDetails(
                new AuthUserDetailsRow(
                        userId,
                        "user01",
                        "hash",
                        "Local User",
                        "ACTIVE",
                        false,
                        null,
                        null,
                        null
                ),
                roles
        );
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }
}
