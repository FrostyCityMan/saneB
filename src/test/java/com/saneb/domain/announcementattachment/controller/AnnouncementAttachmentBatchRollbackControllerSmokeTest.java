package com.saneb.domain.announcementattachment.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.saneb.common.error.*;
import com.saneb.common.response.PageResponse;
import com.saneb.domain.announcementattachment.dto.AttachmentBatchRollbackResponses.*;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentBatchRollbackService;
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
class AnnouncementAttachmentBatchRollbackControllerSmokeTest {
    static final UUID BATCH=UUID.randomUUID(),KEY=UUID.randomUUID(),ACTION=UUID.randomUUID();
    static final String ROOT="/api/v2/admin/announcement-attachment-batches/"+BATCH+"/rollback";
    static final String BODY="{\"expectedVersion\":8,\"expectedPreviewHash\":\""+"a".repeat(64)+"\",\"expectedScopeCount\":2,\"expectedTargetCount\":1,\"expectedDeletedCount\":1,\"expectedBaseReopenCount\":0,\"expectedConfirmationRestoreCount\":1,\"expectedCancelPendingCount\":0,\"acknowledgeBindingRestoration\":true,\"reason\":\"원복 영향 승인\"}";
    @Autowired MockMvc mvc;@MockitoBean AnnouncementAttachmentBatchRollbackService service;
    Receipt receipt(){return new Receipt(ACTION,BATCH,8,"ROLLING_BACK",9,2,1,1,0,1,0,1,1,1,0,0,0,0,OffsetDateTime.now());}
    @Test void adminReceivesQueuedReceiptNotRollbackSuccess() throws Exception {
        when(service.insertRollback(any(),eq(BATCH),eq(KEY),any())).thenReturn(receipt());
        mvc.perform(post(ROOT).with(user("qa").roles("ADMIN")).with(csrf()).header("Idempotency-Key",KEY).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isAccepted()).andExpect(header().string("Cache-Control","no-store")).andExpect(jsonPath("$.data.statusCode").value("ROLLING_BACK"))
                .andExpect(jsonPath("$.data.rolledBackCount").value(0)).andExpect(jsonPath("$.data.deletedCount").value(1)).andExpect(jsonPath("$.data.currentHttpRequests").value(0));
    }
    @ParameterizedTest @ValueSource(strings={"OPERATOR","APPROVER","USER","PARTNER","REVIEWER"}) void nonAdminCannotApprove(String role) throws Exception {
        mvc.perform(post(ROOT).with(user("qa").roles(role)).with(csrf()).header("Idempotency-Key",KEY).contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isForbidden());verifyNoInteractions(service);
    }
    @ParameterizedTest @ValueSource(strings={"ADMIN","OPERATOR","APPROVER"}) void readRolesCanInspectImpactAndResults(String role) throws Exception {
        when(service.selectPreviewDetails(any(),eq(BATCH))).thenReturn(new Preview(BATCH,8,"APPLY_PARTIAL_FAILED","a".repeat(64),2,1,1,1,1,0,0,1,0,0,0));
        when(service.selectActionDetails(any(),eq(BATCH),eq(ACTION))).thenReturn(receipt());when(service.selectItemList(any(),eq(BATCH),eq(1),eq(20))).thenReturn(PageResponse.of(List.of(),1,20,0));
        for(String suffix:List.of("/preview","/items","/actions/"+ACTION))mvc.perform(get(ROOT+suffix).with(user("qa").roles(role))).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"));
        verify(service,never()).insertRollback(any(),any(),any(),any());
    }
    @ParameterizedTest @ValueSource(strings={"force","selectedJobIds","sourceUrl","passed","policyId"}) void unknownOverridesAreRejected(String field) throws Exception {
        mvc.perform(post(ROOT).with(user("qa").roles("ADMIN")).with(csrf()).header("Idempotency-Key",KEY).contentType(MediaType.APPLICATION_JSON)
                .content(BODY.substring(0,BODY.length()-1)+",\""+field+"\":\"untrusted-value\"}"))
                .andExpect(status().isBadRequest()).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("untrusted-value"))));verifyNoInteractions(service);
    }
    @Test void sessionCsrfUuidAndImpactAcknowledgementAreRequired() throws Exception {
        mvc.perform(post(ROOT).with(csrf()).header("Idempotency-Key",KEY).contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isUnauthorized());
        mvc.perform(post(ROOT).with(user("qa").roles("ADMIN")).header("Idempotency-Key",KEY).contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isForbidden());
        mvc.perform(post(ROOT).with(user("qa").roles("ADMIN")).with(csrf()).header("Idempotency-Key","bad").contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isBadRequest());
        mvc.perform(post(ROOT).with(user("qa").roles("ADMIN")).with(csrf()).header("Idempotency-Key",KEY).contentType(MediaType.APPLICATION_JSON).content(BODY.replace("\"acknowledgeBindingRestoration\":true","\"acknowledgeBindingRestoration\":false"))).andExpect(status().isBadRequest());verifyNoInteractions(service);
    }
    @Test void changedScopeAndForeignActionReturnErrorWrappers() throws Exception {
        when(service.insertRollback(any(),any(),any(),any())).thenThrow(new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_VERSION_CONFLICT,HttpStatus.CONFLICT,"원복 범위가 변경됐습니다."));
        mvc.perform(post(ROOT).with(user("qa").roles("ADMIN")).with(csrf()).header("Idempotency-Key",KEY).contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isConflict()).andExpect(jsonPath("$.success").value(false));
        when(service.selectActionDetails(any(),any(),any())).thenThrow(new ApiException(ErrorCode.RESOURCE_NOT_FOUND,HttpStatus.NOT_FOUND,"해당 배치의 원복 내역이 없습니다."));
        mvc.perform(get(ROOT+"/actions/"+ACTION).with(user("qa").roles("ADMIN"))).andExpect(status().isNotFound()).andExpect(jsonPath("$.success").value(false));
    }
}
