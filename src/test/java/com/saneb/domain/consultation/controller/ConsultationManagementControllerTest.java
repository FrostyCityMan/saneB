package com.saneb.domain.consultation.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.saneb.common.error.ApiException;
import com.saneb.common.error.ErrorCode;
import com.saneb.domain.consultation.dto.ConsultationManagementDetailsResponse;
import com.saneb.domain.consultation.service.ConsultationService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "spring.flyway.enabled=false")
@AutoConfigureMockMvc
class ConsultationManagementControllerTest {
    private static final String URL = "/api/v2/admin/consultation-reservations/10000000-0000-0000-0000-000000000099";
    @Autowired MockMvc mvc;
    @MockitoBean ConsultationService service;

    @Test void checksRolesAndNoStoreForDetails() throws Exception {
        when(service.selectConsultationManagementDetails(any(), any())).thenReturn(new ConsultationManagementDetailsResponse(null, List.of("CANCELED")));
        for (String role : List.of("ADMIN", "OPERATOR")) {
            mvc.perform(get(URL).with(user("tester").roles(role))).andExpect(status().isOk())
                    .andExpect(header().string("Cache-Control", "no-store"))
                    .andExpect(jsonPath("$.data.allowedNextStatusCodes[0]").value("CANCELED"));
        }
        for (String role : List.of("USER", "PARTNER", "REVIEWER", "APPROVER")) {
            mvc.perform(get(URL).with(user("tester").roles(role))).andExpect(status().isForbidden());
        }
        mvc.perform(get(URL)).andExpect(status().isUnauthorized());
    }

    @Test void validatesVersionCsrfAndConflict() throws Exception {
        String body = "{\"statusCode\":\"CANCELED\",\"expectedUpdatedAt\":\"2026-10-12T10:00:00.123456+09:00\"}";
        mvc.perform(patch(URL + "/status").with(user("tester").roles("ADMIN")).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        mvc.perform(patch(URL + "/status").with(user("tester").roles("ADMIN")).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"statusCode\":\"CANCELED\"}")).andExpect(status().isBadRequest());
        when(service.updateConsultationManagementStatus(any(), any(), any())).thenThrow(new ApiException(
                ErrorCode.CONSULTATION_VERSION_CONFLICT, HttpStatus.CONFLICT, "다른 사용자가 상담을 수정했습니다. 최신 내용을 확인하세요."));
        mvc.perform(patch(URL + "/status").with(user("tester").roles("ADMIN")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.success").value(false));
    }
}
