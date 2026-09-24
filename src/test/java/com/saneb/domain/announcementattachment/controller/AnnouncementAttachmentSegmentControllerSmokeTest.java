package com.saneb.domain.announcementattachment.controller;

import com.saneb.domain.announcementattachment.dto.AttachmentSegmentAnalysisResponse;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentSegmentService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties="spring.flyway.enabled=false")
@AutoConfigureMockMvc
class AnnouncementAttachmentSegmentControllerSmokeTest {
    private static final UUID SOURCE = UUID.randomUUID(), EXTRACTION = UUID.randomUUID();
    private static final String URL = "/api/v2/admin/announcement-sources/" + SOURCE + "/attachment-extractions/" + EXTRACTION + "/segment-analysis";
    @Autowired MockMvc mvc;
    @MockitoBean AnnouncementAttachmentSegmentService service;
    private AttachmentSegmentAnalysisResponse result() {
        return new AttachmentSegmentAnalysisResponse(SOURCE, UUID.randomUUID(), UUID.randomUUID(), EXTRACTION,
                "UNKNOWN", "TEXT_RULE", "SHADOW", "NOT_ANALYZED", null, null, null);
    }
    @ParameterizedTest @ValueSource(strings = {"ADMIN", "OPERATOR", "APPROVER"})
    void authorizedGetReturnsNoStoreWrapperAndNeverCreatesAnalysis(String role) throws Exception {
        when(service.selectAnalysisDetails(SOURCE, EXTRACTION)).thenReturn(result());
        mvc.perform(get(URL).with(user("fixture").roles(role))).andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store")).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.analysisState").value("NOT_ANALYZED"));
        verify(service, never()).insertAnalysis(any(), any(), any());
    }
    @ParameterizedTest @ValueSource(strings = {"ADMIN", "OPERATOR", "APPROVER"})
    void evaluationBoundReadUsesOnlySelectedIdsAndNoStore(String role) throws Exception {
        UUID evaluation=UUID.randomUUID(),policy=UUID.randomUUID();
        when(service.selectEvaluationAnalysisDetails(SOURCE,EXTRACTION,evaluation)).thenReturn(
                new AttachmentSegmentAnalysisResponse.EvaluationBinding(evaluation,policy,false,"FORM",result()));
        mvc.perform(get(URL+"/evaluations/"+evaluation).with(user("fixture").roles(role)))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"))
                .andExpect(jsonPath("$.data.evaluationId").value(evaluation.toString()))
                .andExpect(jsonPath("$.data.policyId").value(policy.toString()))
                .andExpect(jsonPath("$.data.evaluationCurrent").value(false));
        verify(service).selectEvaluationAnalysisDetails(SOURCE,EXTRACTION,evaluation);
        verifyNoMoreInteractions(service);
    }
    @Test void boundReadRejectsAnonymousOtherRolesAndMalformedEvaluationId() throws Exception {
        String path=URL+"/evaluations/"+UUID.randomUUID();
        mvc.perform(get(path)).andExpect(status().isUnauthorized());
        for(String role:new String[]{"USER","PARTNER","REVIEWER"})
            mvc.perform(get(path).with(user("fixture").roles(role))).andExpect(status().isForbidden());
        mvc.perform(get(URL+"/evaluations/invalid").with(user("fixture").roles("ADMIN")))
                .andExpect(status().isBadRequest()).andExpect(header().string("Cache-Control","no-store"))
                .andExpect(jsonPath("$.message").value("판정 ID는 UUID 형식이어야 합니다. 분류 이력에서 판정을 다시 선택하세요."));
        verifyNoInteractions(service);
    }
    @Test void missingOrCorruptBindingErrorsRemainNoStoreWithoutFallingBack() throws Exception {
        UUID evaluation=UUID.randomUUID();
        for(var status:new org.springframework.http.HttpStatus[]{org.springframework.http.HttpStatus.NOT_FOUND,org.springframework.http.HttpStatus.CONFLICT}) {
            doThrow(new com.saneb.common.error.ApiException(com.saneb.common.error.ErrorCode.ANNOUNCEMENT_ATTACHMENT_NOT_READY,
                    status,"선택한 판정의 구간 근거를 확인하세요.")).when(service).selectEvaluationAnalysisDetails(SOURCE,EXTRACTION,evaluation);
            mvc.perform(get(URL+"/evaluations/"+evaluation).with(user("fixture").roles("ADMIN")))
                    .andExpect(status().is(status.value())).andExpect(header().string("Cache-Control","no-store"))
                    .andExpect(jsonPath("$.success").value(false));
        }
        verify(service,never()).selectAnalysisDetails(any(),any());verify(service,never()).insertAnalysis(any(),any(),any());
    }
    @ParameterizedTest @ValueSource(strings = {"ADMIN", "OPERATOR"})
    void postUsesOnlyPathIdentifiersNotUserSuppliedTextOrRoles(String role) throws Exception {
        when(service.insertAnalysis(any(), eq(SOURCE), eq(EXTRACTION))).thenReturn(result());
        mvc.perform(post(URL).with(user("fixture").roles(role)).with(csrf()))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.data.applicationMode").value("SHADOW"));
        verify(service).insertAnalysis(any(), eq(SOURCE), eq(EXTRACTION));
    }
    @ParameterizedTest @ValueSource(strings = {"APPROVER", "USER", "PARTNER", "REVIEWER"})
    void nonWritersAreForbiddenEvenWithCsrf(String role) throws Exception {
        mvc.perform(post(URL).with(user("fixture").roles(role)).with(csrf())).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
    @Test void missingCsrfOrAuthenticationNeverInvokesAnalysis() throws Exception {
        mvc.perform(post(URL).with(user("fixture").roles("ADMIN"))).andExpect(status().isForbidden());
        mvc.perform(get(URL)).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }
    @Test void invalidIdentifierHasActionableKoreanError() throws Exception {
        mvc.perform(get(URL.replace(EXTRACTION.toString(), "invalid")).with(user("fixture").roles("ADMIN")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("원문 ID와 첨부 추출 ID는 UUID 형식이어야 합니다."));
        verifyNoInteractions(service);
    }
    @ParameterizedTest @ValueSource(strings={"ADMIN","OPERATOR","APPROVER"})
    void explicitAnalysisVersionKeepsAuthorizationAndReadOnlyNoStoreContract(String role) throws Exception {
        for(String version:java.util.List.of("segment-role-1.0.2","segment-role-1.0.3")) {
        when(service.selectAnalysisDetails(SOURCE,EXTRACTION,version)).thenReturn(result());
        mvc.perform(get(URL).param("analysisVersion",version).with(user("fixture").roles(role)))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"))
                .andExpect(jsonPath("$.data.analysisState").value("NOT_ANALYZED"));
        verify(service).selectAnalysisDetails(SOURCE,EXTRACTION,version);
        }
        verify(service,never()).selectAnalysisDetails(SOURCE,EXTRACTION);verify(service,never()).insertAnalysis(any(),any(),any());
    }
}
