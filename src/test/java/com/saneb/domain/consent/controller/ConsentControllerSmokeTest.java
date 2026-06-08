package com.saneb.domain.consent.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.saneb.domain.consent.dto.CurrentConsentResponse;
import com.saneb.domain.consent.dto.UserConsentResponse;
import com.saneb.domain.consent.service.ConsentService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "spring.flyway.enabled=false")
@AutoConfigureMockMvc
class ConsentControllerSmokeTest {

    static final UUID CONSENT_VERSION_ID = UUID.fromString("70000000-0000-0000-0000-000000000001");
    static final UUID USER_CONSENT_ID = UUID.fromString("71000000-0000-0000-0000-000000000001");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConsentService consentService;

    @BeforeEach
    void setUp() {
        when(consentService.selectCurrentConsentList()).thenReturn(List.of(sampleCurrentConsent()));
        when(consentService.selectMyConsentList(any())).thenReturn(List.of(sampleUserConsent()));
        when(consentService.insertMyConsent(any(), any(), any())).thenReturn(sampleUserConsent());
    }

    @Test
    void selectCurrentConsentListAllowsAnonymousUser() throws Exception {
        mockMvc.perform(get("/api/v1/consents/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].consentCode").value("PRIVACY_POLICY"))
                .andExpect(jsonPath("$.data[0].consentName").value("개인정보 처리방침"));
    }

    @Test
    @WithMockUser(username = "user01", roles = "USER")
    void selectMyConsentListReturnsApiResponse() throws Exception {
        mockMvc.perform(get("/api/v1/users/me/consents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].userConsentId").value(USER_CONSENT_ID.toString()));
    }

    @Test
    @WithMockUser(username = "user01", roles = "USER")
    void insertMyConsentReturnsApiResponse() throws Exception {
        mockMvc.perform(post("/api/v1/users/me/consents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "consentCode": "E_CERT",
                                  "consented": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.consentCode").value("PRIVACY_POLICY"));
    }

    @Test
    void selectMyConsentListRejectsAnonymousUser() throws Exception {
        mockMvc.perform(get("/api/v1/users/me/consents"))
                .andExpect(status().isUnauthorized());
    }

    static CurrentConsentResponse sampleCurrentConsent() {
        return new CurrentConsentResponse(
                CONSENT_VERSION_ID,
                "PRIVACY_POLICY",
                "개인정보 처리방침",
                1,
                true,
                OffsetDateTime.parse("2026-06-08T10:00:00+09:00")
        );
    }

    static UserConsentResponse sampleUserConsent() {
        return new UserConsentResponse(
                USER_CONSENT_ID,
                CONSENT_VERSION_ID,
                "PRIVACY_POLICY",
                "개인정보 처리방침",
                1,
                true,
                OffsetDateTime.parse("2026-06-08T10:00:00+09:00")
        );
    }
}
