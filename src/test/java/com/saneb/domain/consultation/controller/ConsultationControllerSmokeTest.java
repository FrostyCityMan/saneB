/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: ConsultationControllerSmokeTest.java
 * 작성자: 김도훈
 *
 */

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

    /**
     * 업무 처리를 수행합니다.
     */
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

    /**
     * 업무 데이터를 조회합니다.
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
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

    /**
     * 업무 데이터를 등록합니다.
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
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

    /**
     * 업무 데이터를 등록합니다.
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
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

    /**
     * 업무 데이터를 등록합니다.
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
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

    /**
     * 업무 데이터를 수정합니다.
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
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

    /**
     * 업무 데이터를 조회합니다.
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
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

    /**
     * 업무 처리를 수행합니다.
     *
     * @param statusCode 입력 값
     *
     * @return 처리 결과
     */
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

    /**
     * 업무 처리를 수행합니다.
     *
     * @param statusCode 입력 값
     *
     * @return 처리 결과
     */
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

    /**
     * 업무 처리를 수행합니다.
     *
     * @return 처리 결과
     */
    private AuthenticatedUserDetails userPrincipal() {
        return principal(USER_ID, "local_user", List.of("USER"));
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @return 처리 결과
     */
    private AuthenticatedUserDetails partnerPrincipal() {
        return principal(PARTNER_USER_ID, "local_partner", List.of("PARTNER"));
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param userId 입력 값
     *
     * @param loginId 입력 값
     *
     * @param roles 입력 값
     *
     * @return 처리 결과
     */
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
