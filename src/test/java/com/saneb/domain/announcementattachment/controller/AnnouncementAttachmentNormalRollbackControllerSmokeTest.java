package com.saneb.domain.announcementattachment.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.saneb.common.error.*;
import com.saneb.domain.announcementattachment.dto.AttachmentNormalRollbackResponses.*;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentNormalRollbackService;
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
class AnnouncementAttachmentNormalRollbackControllerSmokeTest {
    static final UUID SOURCE=UUID.randomUUID(),JOB=UUID.randomUUID(),KEY=UUID.randomUUID(),ACTION=UUID.randomUUID();
    static final String ROOT="/api/v2/admin/announcement-sources/"+SOURCE+"/attachment-jobs/"+JOB+"/rollback";
    static final String BODY="{\"expectedSourceVersion\":0,\"expectedAttachmentVersion\":2,\"expectedPreviewHash\":\""+"a".repeat(64)+"\",\"expectedBaseReopen\":false,\"expectedConfirmationRestore\":true,\"acknowledgeBindingRestoration\":true,\"reason\":\"원복 영향 승인\"}";
    @Autowired MockMvc mvc;@MockitoBean AnnouncementAttachmentNormalRollbackService service;
    Receipt receipt(){return new Receipt(ACTION,SOURCE,JOB,"APPLIED","ROLLED_BACK",0,3,false,true,1,0,OffsetDateTime.now());}
    @ParameterizedTest @ValueSource(strings={"ADMIN","OPERATOR","APPROVER"})
    void recoveryJobHistoryIsScopedPagedAndReadOnly(String role) throws Exception {
        var row=new JobSummary(SOURCE,JOB,"COLLECT","FAILED","PENDING","ROLLED_BACK",ACTION,OffsetDateTime.now());
        when(service.selectJobList(any(),eq(SOURCE),eq(2),eq(10))).thenReturn(com.saneb.common.response.PageResponse.of(List.of(row),2,10,11));
        mvc.perform(get("/api/v2/admin/announcement-sources/"+SOURCE+"/attachment-recovery-jobs?page=2&size=10").with(user("qa").roles(role)))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"))
                .andExpect(jsonPath("$.data.totalCount").value(11)).andExpect(jsonPath("$.data.items[0].jobStatusCode").value("FAILED"))
                .andExpect(jsonPath("$.data.items[0].actionId").value(ACTION.toString())).andExpect(jsonPath("$.data.items[0].reasonHash").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].idempotencyKey").doesNotExist());
        verify(service,never()).insertRollback(any(),any(),any(),any(),any());
    }
    @ParameterizedTest @ValueSource(strings={"USER","PARTNER","REVIEWER"})
    void externalRolesCannotReadRecoveryHistory(String role) throws Exception {
        mvc.perform(get("/api/v2/admin/announcement-sources/"+SOURCE+"/attachment-recovery-jobs").with(user("qa").roles(role))).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
    @Test void historyRequiresSessionUuidAndIntegerPage() throws Exception {
        String path="/api/v2/admin/announcement-sources/"+SOURCE+"/attachment-recovery-jobs";
        mvc.perform(get(path)).andExpect(status().isUnauthorized());
        mvc.perform(get(path+"?page=bad").with(user("qa").roles("ADMIN"))).andExpect(status().isBadRequest()).andExpect(header().string("Cache-Control","no-store"));
        mvc.perform(get(path.replace(SOURCE.toString(),"bad")).with(user("qa").roles("ADMIN"))).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
    @Test void adminReceivesOnlyCompletedSingleSourceReceipt() throws Exception {when(service.insertRollback(any(),eq(SOURCE),eq(JOB),eq(KEY),any())).thenReturn(receipt());mvc.perform(post(ROOT).with(user("qa").roles("ADMIN")).with(csrf()).header("Idempotency-Key",KEY).contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store")).andExpect(jsonPath("$.data.statusCode").value("ROLLED_BACK")).andExpect(jsonPath("$.data.targetCount").value(1)).andExpect(jsonPath("$.data.currentHttpRequests").value(0));}
    @ParameterizedTest @ValueSource(strings={"OPERATOR","APPROVER","USER","PARTNER","REVIEWER"}) void nonAdminCannotApprove(String role) throws Exception {mvc.perform(post(ROOT).with(user("qa").roles(role)).with(csrf()).header("Idempotency-Key",KEY).contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isForbidden());verifyNoInteractions(service);}
    @ParameterizedTest @ValueSource(strings={"ADMIN","OPERATOR","APPROVER"}) void readRolesCanInspectPreviewAndReceipt(String role) throws Exception {when(service.selectPreviewDetails(any(),eq(SOURCE),eq(JOB))).thenReturn(new Preview(SOURCE,JOB,"APPLIED","SUCCEEDED","APPLIED","NOT_REQUESTED","READY",0,2,"a".repeat(64),false,true,false,1,0));when(service.selectActionDetails(any(),eq(SOURCE),eq(JOB),eq(ACTION))).thenReturn(receipt());for(String suffix:List.of("/preview","/actions/"+ACTION))mvc.perform(get(ROOT+suffix).with(user("qa").roles(role))).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"));verify(service,never()).insertRollback(any(),any(),any(),any(),any());}
    @ParameterizedTest @ValueSource(strings={"force","policyId","sourceUrl","selectedJobIds"}) void unknownOverridesAreRejectedWithoutEcho(String field) throws Exception {mvc.perform(post(ROOT).with(user("qa").roles("ADMIN")).with(csrf()).header("Idempotency-Key",KEY).contentType(MediaType.APPLICATION_JSON).content(BODY.substring(0,BODY.length()-1)+",\""+field+"\":\"untrusted-value\"}")).andExpect(status().isBadRequest()).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("untrusted-value"))));verifyNoInteractions(service);}
    @Test void sessionCsrfUuidAndAcknowledgementAreRequired() throws Exception {mvc.perform(post(ROOT).with(csrf()).header("Idempotency-Key",KEY).contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isUnauthorized());mvc.perform(post(ROOT).with(user("qa").roles("ADMIN")).header("Idempotency-Key",KEY).contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isForbidden());mvc.perform(post(ROOT).with(user("qa").roles("ADMIN")).with(csrf()).header("Idempotency-Key","bad").contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isBadRequest());mvc.perform(post(ROOT).with(user("qa").roles("ADMIN")).with(csrf()).header("Idempotency-Key",KEY).contentType(MediaType.APPLICATION_JSON).content(BODY.replace("\"acknowledgeBindingRestoration\":true","\"acknowledgeBindingRestoration\":false"))).andExpect(status().isBadRequest());verifyNoInteractions(service);}
    @Test void conflictsAndForeignReceiptsReturnWrappers() throws Exception {when(service.insertRollback(any(),any(),any(),any(),any())).thenThrow(new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_VERSION_CONFLICT,HttpStatus.CONFLICT,"원문 또는 검수가 변경됐습니다."));mvc.perform(post(ROOT).with(user("qa").roles("ADMIN")).with(csrf()).header("Idempotency-Key",KEY).contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isConflict()).andExpect(jsonPath("$.success").value(false));when(service.selectActionDetails(any(),any(),any(),any())).thenThrow(new ApiException(ErrorCode.RESOURCE_NOT_FOUND,HttpStatus.NOT_FOUND,"해당 영수증이 없습니다."));mvc.perform(get(ROOT+"/actions/"+ACTION).with(user("qa").roles("ADMIN"))).andExpect(status().isNotFound()).andExpect(jsonPath("$.success").value(false));}
}
