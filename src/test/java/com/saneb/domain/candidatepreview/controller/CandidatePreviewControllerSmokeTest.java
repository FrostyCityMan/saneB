/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: CandidatePreviewControllerSmokeTest.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.candidatepreview.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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

    /**
     * 업무 처리를 수행합니다.
     */
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

    /**
     * 업무 처리를 수행합니다.
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
    @Test
    void anonymousUserCanSelectCandidatePreview() throws Exception {
        mockMvc.perform(post("/api/v1/pre-signup/candidate-preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "representativeName": "홍길동",
                                  "birthYear": 1988,
                                  "regionCode": "SEOUL",
                                  "ksicCode": "47911",
                                  "annualRevenue": 30000000,
                                  "openingDate": "2024-01-01",
                                  "hasSpouse": true,
                                  "hasChild": false,
                                  "hasParent": true,
                                  "families": [
                                    {
                                      "relationTypeCode": "PARENT",
                                      "birthYear": 1955,
                                      "cohabiting": true
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.possibleCandidateCount").value(3))
                .andExpect(jsonPath("$.data.minSupportAmount").value(1000000))
                .andExpect(jsonPath("$.data.maxSupportAmount").value(5000000))
                .andExpect(jsonPath("$.data.criteriaNotice").value("회원가입 전 임시 확인 결과입니다."));
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
    @Test
    void anonymousUserCanOpenCandidatePreviewPage() throws Exception {
        mockMvc.perform(get("/candidate-preview"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("대표자명")))
                .andExpect(content().string(containsString("가족 간단 정보")))
                .andExpect(content().string(containsString("간단 결과 확인하기")))
                .andExpect(content().string(containsString("saneb-loading.js")));
    }
}
