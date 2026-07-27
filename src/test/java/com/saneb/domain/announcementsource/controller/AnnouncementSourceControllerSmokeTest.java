/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: AnnouncementSourceControllerSmokeTest.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.announcementsource.controller;

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
import com.saneb.domain.announcementsource.dto.AnnouncementSourceAttachmentResponse;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceCollectionRequestResponse;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceCollectionRunDetailsResponse;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceCollectionRunItemResponse;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceCollectionRunResponse;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceDetailsResponse;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceDuplicateCandidateResponse;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceHighlightResponse;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceLinkResponse;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceSummaryResponse;
import com.saneb.domain.announcementsource.service.AnnouncementSourceService;
import com.saneb.domain.auth.vo.AuthUserDetailsRow;
import com.saneb.domain.auth.vo.AuthenticatedUserDetails;
import java.time.LocalDate;
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
class AnnouncementSourceControllerSmokeTest {

    private static final UUID USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000002");
    private static final UUID REQUEST_ID = UUID.fromString("93000000-0000-0000-0000-000000000001");
    private static final UUID RUN_ID = UUID.fromString("93000000-0000-0000-0000-000000000002");
    private static final UUID SOURCE_ID = UUID.fromString("93000000-0000-0000-0000-000000000003");
    private static final UUID ANNOUNCEMENT_ID = UUID.fromString("93000000-0000-0000-0000-000000000004");
    private static final UUID CANDIDATE_ID = UUID.fromString("93000000-0000-0000-0000-000000000005");
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-06-20T10:00:00+09:00");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AnnouncementSourceService announcementSourceService;

    /**
     * 업무 처리를 수행합니다.
     */
    @BeforeEach
    void setUp() {
        when(announcementSourceService.insertCollectionRequest(any(), any())).thenReturn(collectionRequest("APPROVAL_PENDING"));
        when(announcementSourceService.selectCollectionRequestList(any(), any(), any(), eq(1), eq(20)))
                .thenReturn(PageResponse.of(List.of(collectionRequest("APPROVAL_PENDING")), 1, 20, 1));
        when(announcementSourceService.selectCollectionRequestDetails(REQUEST_ID)).thenReturn(collectionRequest("APPROVAL_PENDING"));
        when(announcementSourceService.updateCollectionRequestApproval(any(), eq(REQUEST_ID), any()))
                .thenReturn(collectionRequest("APPROVED"));
        when(announcementSourceService.insertCollectionRun(REQUEST_ID)).thenReturn(collectionRun("COMPLETED"));
        when(announcementSourceService.selectCollectionRunList(any(), any(), eq(1), eq(20)))
                .thenReturn(PageResponse.of(List.of(collectionRun("COMPLETED")), 1, 20, 1));
        when(announcementSourceService.selectCollectionRunDetails(RUN_ID))
                .thenReturn(new AnnouncementSourceCollectionRunDetailsResponse(
                        collectionRun("COMPLETED"),
                        List.of(collectionRunItem())
                ));
        when(announcementSourceService.selectSourceList(any(), any(), any(), eq(1), eq(20)))
                .thenReturn(PageResponse.of(List.of(sourceSummary()), 1, 20, 1));
        when(announcementSourceService.selectSourceDetails(SOURCE_ID)).thenReturn(sourceDetails("REVIEW_PENDING"));
        when(announcementSourceService.updateSourceReviewStatus(any(), eq(SOURCE_ID), any()))
                .thenReturn(sourceDetails("REVIEW_COMPLETED"));
        when(announcementSourceService.updateDuplicateCandidateDecision(any(), eq(SOURCE_ID), eq(CANDIDATE_ID), any()))
                .thenReturn(sourceDetails("REVIEW_COMPLETED"));
        when(announcementSourceService.insertOperationalAnnouncement(any(), eq(SOURCE_ID), any()))
                .thenReturn(new AnnouncementSourceLinkResponse(SOURCE_ID, "SRC-000001", ANNOUNCEMENT_ID, "ANN-000001"));
    }

    /**
     * 업무 데이터를 등록합니다.
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
    @Test
    void insertCollectionRequestReturnsApiResponse() throws Exception {
        mockMvc.perform(post("/api/v1/admin/announcement-source-collections/requests")
                        .with(user(operatorPrincipal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "providerCode": "BIZINFO",
                                  "requestTypeCode": "MANUAL",
                                  "searchKeyword": "소상공인",
                                  "maxCount": 100
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.publicCode").value("ASR-000001"))
                .andExpect(jsonPath("$.data.requestStatusCode").value("APPROVAL_PENDING"));
    }

    /**
     * 업무 데이터를 수정합니다.
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
    @Test
    void approveAndRunCollectionRequestReturnApiResponses() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/announcement-source-collections/requests/{requestId}/approval", REQUEST_ID)
                        .with(user(approverPrincipal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "approvalStatusCode": "APPROVED",
                                  "approvalNote": "승인"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requestStatusCode").value("APPROVED"));

        mockMvc.perform(post("/api/v1/admin/announcement-source-collections/requests/{requestId}/runs", REQUEST_ID)
                        .with(user(approverPrincipal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.runStatusCode").value("COMPLETED"));
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
    @Test
    void selectSourceDetailsReturnsHighlightsAndAttachments() throws Exception {
        mockMvc.perform(get("/api/v1/admin/announcement-sources/{sourceId}", SOURCE_ID)
                        .with(user(operatorPrincipal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.publicCode").value("SRC-000001"))
                .andExpect(jsonPath("$.data.attachments[0].fileUrl").value("https://example.com/file.pdf"))
                .andExpect(jsonPath("$.data.highlights[0].highlightTypeCode").value("TARGET"))
                .andExpect(jsonPath("$.data.duplicateCandidates[0].matchTypeCode").value("EXACT_DUPLICATE"));
    }

    /**
     * 업무 데이터를 수정합니다.
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
    @Test
    void updateDuplicateCandidateDecisionReturnsSourceDetails() throws Exception {
        mockMvc.perform(patch(
                        "/api/v1/admin/announcement-sources/{sourceId}/duplicate-candidates/{candidateId}/decision",
                        SOURCE_ID,
                        CANDIDATE_ID
                )
                        .with(user(operatorPrincipal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "decisionActionCode": "UPDATE_EXISTING",
                                  "decisionNote": "기존 공고를 업데이트합니다."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.duplicateCandidates[0].decisionStatusCode").value("PENDING"));
    }

    /**
     * 업무 데이터를 등록합니다.
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
    @Test
    void insertOperationalAnnouncementReturnsPublicCodes() throws Exception {
        mockMvc.perform(post("/api/v1/admin/announcement-sources/{sourceId}/announcements", SOURCE_ID)
                        .with(user(operatorPrincipal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetTypeCode": "BUSINESS",
                                  "incomeJudgementCode": "VAT_TAX_BASE_ONLY"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sourcePublicCode").value("SRC-000001"))
                .andExpect(jsonPath("$.data.announcementCode").value("ANN-000001"));
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param statusCode 입력 값
     *
     * @return 처리 결과
     */
    private AnnouncementSourceCollectionRequestResponse collectionRequest(String statusCode) {
        return new AnnouncementSourceCollectionRequestResponse(
                REQUEST_ID,
                "ASR-000001",
                "BIZINFO",
                "MANUAL",
                statusCode,
                USER_ID,
                NOW,
                "ADMIN_BUTTON",
                "소상공인",
                null,
                null,
                LocalDate.parse("2026-06-01"),
                LocalDate.parse("2026-09-30"),
                100,
                "요청",
                "APPROVED".equals(statusCode) ? USER_ID : null,
                "APPROVED".equals(statusCode) ? NOW : null,
                "승인",
                NOW,
                NOW
        );
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param statusCode 입력 값
     *
     * @return 처리 결과
     */
    private AnnouncementSourceCollectionRunResponse collectionRun(String statusCode) {
        return new AnnouncementSourceCollectionRunResponse(
                RUN_ID,
                "ASRUN-000001",
                REQUEST_ID,
                "ASR-000001",
                "BIZINFO",
                "MANUAL",
                statusCode,
                NOW,
                NOW,
                1,
                1,
                0,
                0,
                0,
                null,
                NOW
        );
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @return 처리 결과
     */
    private AnnouncementSourceCollectionRunItemResponse collectionRunItem() {
        return new AnnouncementSourceCollectionRunItemResponse(
                UUID.randomUUID(),
                RUN_ID,
                SOURCE_ID,
                "SRC-000001",
                "BIZ-1",
                "https://example.com",
                "COLLECTED",
                null,
                NOW
        );
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @return 처리 결과
     */
    private AnnouncementSourceSummaryResponse sourceSummary() {
        return new AnnouncementSourceSummaryResponse(
                SOURCE_ID,
                "SRC-000001",
                "BIZINFO",
                "BIZ-1",
                "서울 소상공인 지원",
                "서울시",
                LocalDate.parse("2026-06-01"),
                LocalDate.parse("2026-09-30"),
                "https://example.com",
                "COMPLETE",
                "REVIEW_PENDING",
                NOW,
                NOW
        );
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param statusCode 입력 값
     *
     * @return 처리 결과
     */
    private AnnouncementSourceDetailsResponse sourceDetails(String statusCode) {
        return new AnnouncementSourceDetailsResponse(
                SOURCE_ID,
                "SRC-000001",
                "BIZINFO",
                "BIZ-1",
                "서울 소상공인 지원",
                "서울시",
                LocalDate.parse("2026-06-01"),
                LocalDate.parse("2026-09-30"),
                NOW,
                NOW,
                "https://example.com",
                "지원대상: 서울 소재 소상공인",
                "문의처: 서울시",
                "신청방법: 온라인",
                "COMPLETE",
                "{\"missingFields\":[]}",
                statusCode,
                NOW,
                List.of(new AnnouncementSourceAttachmentResponse(UUID.randomUUID(), "공고문", "https://example.com/file.pdf", "PDF", 0)),
                List.of(new AnnouncementSourceHighlightResponse(UUID.randomUUID(), "TARGET", "지원대상: 서울 소재 소상공인", 0, 20, 1, "RULE_HEADING")),
                List.of(new AnnouncementSourceDuplicateCandidateResponse(
                        CANDIDATE_ID,
                        SOURCE_ID,
                        ANNOUNCEMENT_ID,
                        "ANN-000001",
                        "서울 소상공인 지원",
                        "서울시",
                        LocalDate.parse("2026-06-01"),
                        LocalDate.parse("2026-09-30"),
                        "EXACT_DUPLICATE",
                        "동일 공고",
                        true,
                        true,
                        true,
                        true,
                        true,
                        "사업명 일치, 주관기관 일치, 공고번호 일치, 신청기간 일치, 원문 URL 일치",
                        "PENDING",
                        "검수 필요",
                        "SRC-000000",
                        "BIZ-1",
                        "https://example.com",
                        null
                )),
                List.of()
        );
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @return 처리 결과
     */
    private AuthenticatedUserDetails operatorPrincipal() {
        return principal("local_operator", List.of("OPERATOR"));
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @return 처리 결과
     */
    private AuthenticatedUserDetails approverPrincipal() {
        return principal("local_approver", List.of("APPROVER"));
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param loginId 입력 값
     *
     * @param roles 입력 값
     *
     * @return 처리 결과
     */
    private AuthenticatedUserDetails principal(String loginId, List<String> roles) {
        return new AuthenticatedUserDetails(
                new AuthUserDetailsRow(
                        USER_ID,
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
