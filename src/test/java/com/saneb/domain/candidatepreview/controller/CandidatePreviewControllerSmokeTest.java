package com.saneb.domain.candidatepreview.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.saneb.domain.candidatepreview.dto.CandidatePreviewResponse;
import com.saneb.domain.candidatepreview.service.CandidatePreviewService;
import java.math.BigDecimal;
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
class CandidatePreviewControllerSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CandidatePreviewService candidatePreviewService;

    @BeforeEach
    void setUp() {
        when(candidatePreviewService.selectCandidatePreview(any()))
                .thenReturn(new CandidatePreviewResponse(
                        3,
                        new BigDecimal("1000000"),
                        new BigDecimal("5000000"),
                        "회원가입 전 임시 확인 결과입니다."
                ));
    }

    @Test
    void anonymousUserCanSelectCandidatePreview() throws Exception {
        mockMvc.perform(post("/api/v1/pre-signup/candidate-preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "regionCode": "SEOUL",
                                  "annualRevenue": 30000000,
                                  "openingDate": "2024-01-01",
                                  "hasSpouse": true,
                                  "hasChild": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.possibleCandidateCount").value(3))
                .andExpect(jsonPath("$.data.minSupportAmount").value(1000000))
                .andExpect(jsonPath("$.data.maxSupportAmount").value(5000000))
                .andExpect(jsonPath("$.data.criteriaNotice").value("회원가입 전 임시 확인 결과입니다."));
    }
}
