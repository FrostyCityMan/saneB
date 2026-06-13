package com.saneb.domain.consultation.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.saneb.domain.auth.dto.AuthMeResponse;
import com.saneb.domain.auth.service.AuthService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "spring.flyway.enabled=false")
@AutoConfigureMockMvc
class ConsultationViewControllerSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Test
    @WithMockUser(username = "user01", roles = "USER")
    void selectConsultationPageUsesRequestLabelForUser() throws Exception {
        when(authService.selectAuthMe(any())).thenReturn(authResponse("USER"));

        mockMvc.perform(get("/app/consultations"))
                .andExpect(status().isOk())
                .andExpect(view().name("app/consultations"))
                .andExpect(content().string(containsString("상담 요청 | 사내비")))
                .andExpect(content().string(containsString("상담 요청")))
                .andExpect(content().string(containsString("상담이 필요한 내용을 남기면 운영자가 확인한 뒤 담당자를 배정합니다.")))
                .andExpect(content().string(not(containsString("상담 관리"))))
                .andExpect(content().string(not(containsString("담당자 배정 및 상태 변경"))));
    }

    @Test
    @WithMockUser(username = "admin01", roles = "ADMIN")
    void selectConsultationPageUsesManagementLabelForAdmin() throws Exception {
        when(authService.selectAuthMe(any())).thenReturn(authResponse("ADMIN"));

        mockMvc.perform(get("/app/consultations"))
                .andExpect(status().isOk())
                .andExpect(view().name("app/consultations"))
                .andExpect(content().string(containsString("상담 관리 | 사내비")))
                .andExpect(content().string(containsString("상담 관리")))
                .andExpect(content().string(containsString("운영자는 요청 건을 확인해 담당자와 상태를 수동으로 배정합니다.")))
                .andExpect(content().string(containsString("담당자 배정 및 상태 변경")));
    }

    private AuthMeResponse authResponse(String role) {
        String defaultRoute = "ADMIN".equals(role) ? "/app/admin/dashboard" : "/app/dashboard";
        return new AuthMeResponse(
                UUID.fromString("10000000-0000-0000-0000-000000000002"),
                role.toLowerCase() + "01",
                "ADMIN".equals(role) ? "관리자" : "일반 사용자",
                List.of(role),
                role,
                defaultRoute,
                false,
                new AuthMeResponse.ProfileResponse(null, null, null)
        );
    }
}
