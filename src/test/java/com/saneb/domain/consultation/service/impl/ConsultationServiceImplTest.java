package com.saneb.domain.consultation.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

    @BeforeEach
    void setUp() {
        consultationService = new ConsultationServiceImpl(consultationDao);
    }

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

    @Test
    void insertConsultationReservationHoldsOpenSlotAndWritesHistory() {
        when(consultationDao.selectConsultationSlotDetails(SLOT_ID)).thenReturn(slot("OPEN"));
        when(consultationDao.selectConsultationReservationDetails(any())).thenReturn(reservation("REQUESTED"));

        var response = consultationService.insertConsultationReservation(
                authentication(USER_ID, List.of("USER")),
                new ConsultationReservationCreateRequest(SLOT_ID, null, null, null, "전화 상담 희망")
        );

        ArgumentCaptor<ConsultationReservationInsertCommand> reservationCaptor =
                ArgumentCaptor.forClass(ConsultationReservationInsertCommand.class);
        verify(consultationDao).insertConsultationReservation(reservationCaptor.capture());
        assertThat(reservationCaptor.getValue().memberUserId()).isEqualTo(USER_ID);
        assertThat(reservationCaptor.getValue().partnerUserId()).isEqualTo(PARTNER_USER_ID);
        verify(consultationDao).updateConsultationSlotStatus(any());
        verify(consultationDao).insertConsultationHistory(any());
        verify(consultationDao).insertAuditLog(any());
        assertThat(response.statusCode()).isEqualTo("REQUESTED");
    }

    @Test
    void updateConsultationReservationStatusAllowsPartnerToConfirm() {
        when(consultationDao.selectConsultationReservationDetails(RESERVATION_ID))
                .thenReturn(reservation("REQUESTED"), reservation("CONFIRMED"));
        when(consultationDao.updateConsultationReservationStatus(any())).thenReturn(1);

        var response = consultationService.updateConsultationReservationStatus(
                authentication(PARTNER_USER_ID, List.of("PARTNER")),
                RESERVATION_ID,
                new ConsultationReservationStatusUpdateRequest("confirmed", "확정")
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

    @Test
    void updateConsultationReservationStatusRejectsUserConfirm() {
        when(consultationDao.selectConsultationReservationDetails(RESERVATION_ID)).thenReturn(reservation("REQUESTED"));

        assertThatThrownBy(() -> consultationService.updateConsultationReservationStatus(
                authentication(USER_ID, List.of("USER")),
                RESERVATION_ID,
                new ConsultationReservationStatusUpdateRequest("CONFIRMED", null)
        ))
                .isInstanceOf(ApiException.class)
                .extracting(error -> ((ApiException) error).errorCode())
                .isEqualTo(ErrorCode.AUTH_FORBIDDEN);
    }

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

    private ConsultationReservationRow reservation(String statusCode) {
        return new ConsultationReservationRow(
                RESERVATION_ID,
                SLOT_ID,
                USER_ID,
                PARTNER_USER_ID,
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
