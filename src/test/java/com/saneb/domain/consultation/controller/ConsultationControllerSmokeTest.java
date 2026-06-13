package com.saneb.domain.consultation.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.saneb.common.response.PageResponse;
import com.saneb.domain.auth.vo.AuthUserDetailsRow;
import com.saneb.domain.auth.vo.AuthenticatedUserDetails;
import com.saneb.domain.consultation.dto.ConsultationReservationResponse;
import com.saneb.domain.consultation.dto.ConsultationSlotResponse;
import com.saneb.domain.consultation.service.ConsultationService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "spring.flyway.enabled=false")
@AutoConfigureMockMvc
class ConsultationControllerSmokeTest {

    private static final UUID USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000002");
    private static final UUID PARTNER_USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000003");
    private static final UUID SLOT_ID = UUID.fromString("85000000-0000-0000-0000-000000000001");
    private static final UUID RESERVATION_ID = UUID.fromString("85000000-0000-0000-0000-000000000002");
    private static final OffsetDateTime START_AT = OffsetDateTime.parse("2026-06-20T10:00:00+09:00");
    private static final OffsetDateTime END_AT = OffsetDateTime.parse("2026-06-20T10:30:00+09:00");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConsultationService consultationService;

    @BeforeEach
    void setUp() {
        when(consultationService.selectConsultationSlotList(any(), any(), any(), any(), any(), eq(1), eq(20)))
                .thenReturn(PageResponse.of(List.of(slot("OPEN")), 1, 20, 1));
        when(consultationService.insertConsultationSlot(any(), any())).thenReturn(slot("OPEN"));
        when(consultationService.updateConsultationSlotStatus(any(), eq(SLOT_ID), any())).thenReturn(slot("CANCELED"));
        when(consultationService.selectConsultationReservationList(any(), any(), any(), any(), eq(1), eq(20)))
                .thenReturn(PageResponse.of(List.of(reservation("REQUESTED")), 1, 20, 1));
        when(consultationService.insertConsultationReservation(any(), any())).thenReturn(reservation("REQUESTED"));
        when(consultationService.updateConsultationReservationStatus(any(), eq(RESERVATION_ID), any()))
                .thenReturn(reservation("CONFIRMED"));
    }

    @Test
    void selectConsultationSlotListReturnsPagedApiResponse() throws Exception {
        mockMvc.perform(get("/api/v1/consultation-slots")
                        .with(user(userPrincipal()))
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].slotId").value(SLOT_ID.toString()));
    }

    @Test
    void insertConsultationSlotReturnsApiResponse() throws Exception {
        mockMvc.perform(post("/api/v1/consultation-slots")
                        .with(user(partnerPrincipal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "startAt": "2026-06-20T10:00:00+09:00",
                                  "endAt": "2026-06-20T10:30:00+09:00",
                                  "note": "오전 상담"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.statusCode").value("OPEN"));
    }

    @Test
    void insertConsultationSlotRejectsUserRole() throws Exception {
        mockMvc.perform(post("/api/v1/consultation-slots")
                        .with(user(userPrincipal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "startAt": "2026-06-20T10:00:00+09:00",
                                  "endAt": "2026-06-20T10:30:00+09:00"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void insertConsultationReservationReturnsApiResponse() throws Exception {
        mockMvc.perform(post("/api/v1/consultation-reservations")
                        .with(user(userPrincipal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "slotId": "%s",
                                  "requestNote": "전화 상담 희망"
                                }
                                """.formatted(SLOT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.reservationId").value(RESERVATION_ID.toString()))
                .andExpect(jsonPath("$.data.statusCode").value("REQUESTED"));
    }

    @Test
    void updateConsultationReservationStatusReturnsApiResponse() throws Exception {
        mockMvc.perform(patch("/api/v1/consultation-reservations/{reservationId}/status", RESERVATION_ID)
                        .with(user(partnerPrincipal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "statusCode": "CONFIRMED",
                                  "note": "확정"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.statusCode").value("CONFIRMED"));
    }

    @Test
    void selectConsultationReservationListReturnsPagedApiResponse() throws Exception {
        mockMvc.perform(get("/api/v1/consultation-reservations")
                        .with(user(partnerPrincipal()))
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].reservationId").value(RESERVATION_ID.toString()));
    }

    private ConsultationSlotResponse slot(String statusCode) {
        return new ConsultationSlotResponse(
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

    private ConsultationReservationResponse reservation(String statusCode) {
        return new ConsultationReservationResponse(
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

    private AuthenticatedUserDetails userPrincipal() {
        return principal(USER_ID, "local_user", List.of("USER"));
    }

    private AuthenticatedUserDetails partnerPrincipal() {
        return principal(PARTNER_USER_ID, "local_partner", List.of("PARTNER"));
    }

    private AuthenticatedUserDetails principal(UUID userId, String loginId, List<String> roles) {
        return new AuthenticatedUserDetails(
                new AuthUserDetailsRow(
                        userId,
                        loginId,
                        "password-hash",
                        "Local User",
                        "ACTIVE",
                        false,
                        null,
                        null,
                        null
                ),
                roles
        );
    }
}
