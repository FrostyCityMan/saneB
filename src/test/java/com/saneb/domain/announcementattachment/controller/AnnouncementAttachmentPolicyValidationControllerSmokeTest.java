package com.saneb.domain.announcementattachment.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.saneb.common.error.*;
import com.saneb.common.response.PageResponse;
import com.saneb.domain.announcementattachment.dto.*;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentPolicyValidationService;
import java.time.OffsetDateTime;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties="spring.flyway.enabled=false") @AutoConfigureMockMvc
class AnnouncementAttachmentPolicyValidationControllerSmokeTest {
    private static final UUID POLICY=UUID.randomUUID(),RUN=UUID.randomUUID(),KEY=UUID.randomUUID();
    private static final String ROOT="/api/v2/admin/announcement-attachment-policies/"+POLICY+"/validation-runs";
    private static final String INPUT="{\"expectedVersion\":0,\"reason\":\"QA 예약\"}";
    @Autowired MockMvc mvc;
    @MockitoBean AnnouncementAttachmentPolicyValidationService service;
    private AttachmentPolicyValidationResponse response(String state) {
        return new AttachmentPolicyValidationResponse(RUN,POLICY,0,UUID.randomUUID(),0,"a".repeat(64),state,0,true,null,
                OffsetDateTime.parse("2026-09-11T14:00:00+09:00"),null,null,List.of());
    }
    @Test void reservationReturns202AndDoesNotClaimPublicationReady() throws Exception {
        when(service.insertRun(any(),eq(POLICY),eq(KEY),any())).thenReturn(response("PENDING"));
        mvc.perform(post(ROOT).with(user("qa").roles("ADMIN")).with(csrf()).header("Idempotency-Key",KEY).contentType(MediaType.APPLICATION_JSON).content(INPUT))
                .andExpect(status().isAccepted()).andExpect(header().string("Cache-Control","no-store"))
                .andExpect(jsonPath("$.data.statusCode").value("PENDING")).andExpect(jsonPath("$.data.publicationReady").doesNotExist())
                .andExpect(jsonPath("$.data.inputSnapshotJson").doesNotExist()).andExpect(jsonPath("$.data.requestedBy").doesNotExist());
    }
    @ParameterizedTest @ValueSource(strings={"ADMIN","OPERATOR","APPROVER"})
    void readRolesSeePagedStatusAndSourceScopedDetails(String role) throws Exception {
        when(service.selectRunList(any(),eq(POLICY),eq(2),eq(10))).thenReturn(PageResponse.of(List.of(response("INCOMPLETE")),2,10,11));
        when(service.selectRunDetails(any(),eq(POLICY),eq(RUN))).thenReturn(response("INCOMPLETE"));
        mvc.perform(get(ROOT).with(user("qa").roles(role)).param("page","2").param("size","10"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalPages").value(2)).andExpect(jsonPath("$.data.items[0].statusCode").value("INCOMPLETE"));
        mvc.perform(get(ROOT+"/"+RUN).with(user("qa").roles(role))).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"));
    }
    @Test void cancellationUsesRunVersionAndCsrf() throws Exception {
        when(service.updateCancellation(any(),eq(POLICY),eq(RUN),any())).thenReturn(response("CANCEL_REQUESTED"));
        mvc.perform(put(ROOT+"/"+RUN+"/cancellation").with(user("qa").roles("ADMIN")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(INPUT))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.statusCode").value("CANCEL_REQUESTED"));
        mvc.perform(put(ROOT+"/"+RUN+"/cancellation").with(user("qa").roles("ADMIN")).contentType(MediaType.APPLICATION_JSON).content(INPUT)).andExpect(status().isForbidden());
    }
    @ParameterizedTest @ValueSource(strings={"OPERATOR","APPROVER","USER","PARTNER","REVIEWER"})
    void onlyAdminCanReserveOrCancel(String role) throws Exception {
        mvc.perform(post(ROOT).with(user("qa").roles(role)).with(csrf()).header("Idempotency-Key",KEY).contentType(MediaType.APPLICATION_JSON).content(INPUT)).andExpect(status().isForbidden());
        mvc.perform(put(ROOT+"/"+RUN+"/cancellation").with(user("qa").roles(role)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(INPUT)).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
    @ParameterizedTest @ValueSource(strings={"passed","filePath","runtimeHash","targetIds","profileCode","statusCode"})
    void clientCannotSubmitSuccessOrExecutionInputs(String field) throws Exception {
        String json=INPUT.substring(0,INPUT.length()-1)+",\""+field+"\":\"untrusted-qa-input\"}";
        mvc.perform(post(ROOT).with(user("qa").roles("ADMIN")).with(csrf()).header("Idempotency-Key",KEY).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.data.errorCode").value("ANNOUNCEMENT_ATTACHMENT_POLICY_INVALID"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("untrusted-qa-input"))));
        verifyNoInteractions(service);
    }
    @Test void unauthenticatedUnauthorizedAndMissingKeyCannotReachService() throws Exception {
        mvc.perform(get(ROOT)).andExpect(status().isUnauthorized());
        for(String role:List.of("USER","PARTNER","REVIEWER")) mvc.perform(get(ROOT).with(user("qa").roles(role))).andExpect(status().isForbidden());
        mvc.perform(post(ROOT).with(user("qa").roles("ADMIN")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(INPUT)).andExpect(status().isBadRequest());
        mvc.perform(post(ROOT).with(user("qa").roles("ADMIN")).header("Idempotency-Key",KEY).contentType(MediaType.APPLICATION_JSON).content(INPUT)).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
    @Test void wrongPolicyAndConflictKeepWrappersAndDoNotReportSuccess() throws Exception {
        when(service.selectRunDetails(any(),eq(POLICY),eq(RUN))).thenThrow(new ApiException(ErrorCode.RESOURCE_NOT_FOUND,HttpStatus.NOT_FOUND,"정책의 QA 실행을 찾을 수 없습니다."));
        mvc.perform(get(ROOT+"/"+RUN).with(user("qa").roles("ADMIN"))).andExpect(status().isNotFound()).andExpect(jsonPath("$.success").value(false));
        when(service.insertRun(any(),eq(POLICY),eq(KEY),any())).thenThrow(new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_VERSION_CONFLICT,HttpStatus.CONFLICT,"최신 정책을 확인하세요."));
        mvc.perform(post(ROOT).with(user("qa").roles("ADMIN")).with(csrf()).header("Idempotency-Key",KEY).contentType(MediaType.APPLICATION_JSON).content(INPUT))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.success").value(false));
    }
    @ParameterizedTest @ValueSource(strings={"ADMIN","OPERATOR","APPROVER"})
    void providerQaPlanIsReadOnlyPagedAndCannotClaimQaSuccess(String role) throws Exception {
        var summary=new AttachmentProviderQaPlanResponse.Summary(2,1,1,0,0,1,0,6,false,false,0,false);
        var item=new AttachmentProviderQaPlanResponse.Item("GOV24_PUBLIC_SERVICE",null,null,null,"PROFILE_MISSING","시스템 프로필과 실파일 검증이 필요합니다.",List.of(),3,List.of("PDF","HWP","HWPX"));
        when(service.selectProviderQaPlan(any(),eq(POLICY),eq(2),eq(1))).thenReturn(new AttachmentProviderQaPlanResponse("a".repeat(64),false,summary,PageResponse.of(List.of(item),2,1,2)));
        mvc.perform(get("/api/v2/admin/announcement-attachment-policies/"+POLICY+"/provider-qa-plan").with(user("qa").roles(role)).param("page","2").param("size","1"))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"))
                .andExpect(jsonPath("$.data.summary.isQaPassed").value(false)).andExpect(jsonPath("$.data.summary.currentHttpRequests").value(0))
                .andExpect(jsonPath("$.data.targets.totalPages").value(2)).andExpect(jsonPath("$.data.targets.items[0].statusCode").value("PROFILE_MISSING"));
        verify(service,never()).insertRun(any(),any(),any(),any());
    }
    @Test void providerQaPlanRejectsOtherRolesMalformedIdsAndMutationMethods() throws Exception {
        String url="/api/v2/admin/announcement-attachment-policies/"+POLICY+"/provider-qa-plan";
        mvc.perform(get(url)).andExpect(status().isUnauthorized());
        for(String role:List.of("USER","PARTNER","REVIEWER")) mvc.perform(get(url).with(user("qa").roles(role))).andExpect(status().isForbidden());
        mvc.perform(get(url.replace(POLICY.toString(),"not-uuid")).with(user("qa").roles("ADMIN"))).andExpect(status().isBadRequest());
        mvc.perform(get(url).with(user("qa").roles("ADMIN")).param("page","not-number")).andExpect(status().isBadRequest());
        mvc.perform(post(url).with(user("qa").roles("ADMIN")).with(csrf())).andExpect(status().isMethodNotAllowed());
        verifyNoInteractions(service);
    }
}
