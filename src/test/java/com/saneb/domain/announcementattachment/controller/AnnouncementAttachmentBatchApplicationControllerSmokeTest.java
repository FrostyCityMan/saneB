package com.saneb.domain.announcementattachment.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.saneb.common.error.*;
import com.saneb.common.response.PageResponse;
import com.saneb.domain.announcementattachment.dto.*;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentBatchApplicationService;
import java.time.OffsetDateTime;
import java.util.UUID;
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
class AnnouncementAttachmentBatchApplicationControllerSmokeTest {
    static final UUID BATCH=UUID.randomUUID(),PREVIEW=UUID.randomUUID(),KEY=UUID.randomUUID(),ACTION=UUID.randomUUID();
    static final String ROOT="/api/v2/admin/announcement-attachment-batches/"+BATCH+"/application";
    static final String BODY="{\"expectedVersion\":8,\"expectedPreviewId\":\""+PREVIEW+"\",\"expectedPreviewHash\":\""+"a".repeat(64)+"\",\"expectedItemCount\":2,\"expectedSelectedCount\":1,\"expectedDeletedCount\":1,\"acknowledgeReviewReset\":true,\"reason\":\"명시적 승인\"}";
    @Autowired MockMvc mvc;
    @MockitoBean AnnouncementAttachmentBatchApplicationService service;
    AttachmentBatchApplicationResponse result() {return new AttachmentBatchApplicationResponse(ACTION,BATCH,PREVIEW,"START",8,"APPLYING",9,2,1,1,1,1,1,0,0,0,0,OffsetDateTime.now());}
    @Test void administratorReceivesAcceptedReceiptNotAppliedSuccess() throws Exception {
        when(service.insertAction(any(),eq(BATCH),eq(KEY),eq("START"),any())).thenReturn(result());
        mvc.perform(post(ROOT).with(user("qa").roles("ADMIN")).with(csrf()).header("Idempotency-Key",KEY).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isAccepted()).andExpect(header().string("Cache-Control","no-store")).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.currentStatusCode").value("APPLYING")).andExpect(jsonPath("$.data.appliedCount").value(0))
                .andExpect(jsonPath("$.data.deletedItemCount").value(1)).andExpect(jsonPath("$.data.currentHttpRequests").value(0));
    }
    @ParameterizedTest @ValueSource(strings={"pause","resume"})
    void pauseResumeDispatchKnownOperations(String path) throws Exception {
        when(service.insertAction(any(),eq(BATCH),eq(KEY),eq(path.toUpperCase(java.util.Locale.ROOT)),any())).thenReturn(result());
        mvc.perform(post(ROOT+"/"+path).with(user("qa").roles("ADMIN")).with(csrf()).header("Idempotency-Key",KEY).contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isAccepted());
        verify(service).insertAction(any(),eq(BATCH),eq(KEY),eq(path.toUpperCase(java.util.Locale.ROOT)),any());
    }
    @ParameterizedTest @ValueSource(strings={"OPERATOR","APPROVER","USER","PARTNER","REVIEWER"})
    void nonAdminCannotStartPauseOrResume(String role) throws Exception {
        for(String suffix:new String[]{"","/pause","/resume"})mvc.perform(post(ROOT+suffix).with(user("qa").roles(role)).with(csrf()).header("Idempotency-Key",KEY).contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
    @ParameterizedTest @ValueSource(strings={"ADMIN","OPERATOR","APPROVER"})
    void readRolesCanReadScopedReceipt(String role) throws Exception {
        when(service.selectActionDetails(any(),eq(BATCH),eq(ACTION))).thenReturn(result());
        when(service.selectItemList(any(),eq(BATCH),eq(2),eq(1))).thenReturn(PageResponse.of(java.util.List.of(),2,1,1));
        mvc.perform(get(ROOT+"/actions/"+ACTION).with(user("qa").roles(role))).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"));
        mvc.perform(get(ROOT+"/items").with(user("qa").roles(role)).param("page","2").param("size","1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalCount").value(1));
    }
    @ParameterizedTest @ValueSource(strings={"sourceUrl","force","policyId","selectedJobIds","passed"})
    void rejectsUnapprovedOverridesWithoutEcho(String field) throws Exception {
        String extra=BODY.substring(0,BODY.length()-1)+",\""+field+"\":\"untrusted-value\"}";
        mvc.perform(post(ROOT).with(user("qa").roles("ADMIN")).with(csrf()).header("Idempotency-Key",KEY).contentType(MediaType.APPLICATION_JSON).content(extra))
                .andExpect(status().isBadRequest()).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("untrusted-value"))));
        verifyNoInteractions(service);
    }
    @Test void requiresSessionCsrfUuidAndExplicitReviewReset() throws Exception {
        mvc.perform(post(ROOT).with(csrf()).header("Idempotency-Key",KEY).contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isUnauthorized());
        mvc.perform(post(ROOT).with(user("qa").roles("ADMIN")).header("Idempotency-Key",KEY).contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isForbidden());
        mvc.perform(post(ROOT).with(user("qa").roles("ADMIN")).with(csrf()).header("Idempotency-Key","invalid").contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isBadRequest());
        mvc.perform(post(ROOT).with(user("qa").roles("ADMIN")).with(csrf()).header("Idempotency-Key",KEY).contentType(MediaType.APPLICATION_JSON).content(BODY.replace("\"acknowledgeReviewReset\":true","\"acknowledgeReviewReset\":false"))).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
    @Test void changedInputAndForeignReceiptUseErrorWrappers() throws Exception {
        when(service.insertAction(any(),any(),any(),any(),any())).thenThrow(new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_VERSION_CONFLICT,HttpStatus.CONFLICT,"미리보기가 변경됐습니다."));
        mvc.perform(post(ROOT).with(user("qa").roles("ADMIN")).with(csrf()).header("Idempotency-Key",KEY).contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isConflict()).andExpect(jsonPath("$.success").value(false));
        when(service.selectActionDetails(any(),any(),any())).thenThrow(new ApiException(ErrorCode.RESOURCE_NOT_FOUND,HttpStatus.NOT_FOUND,"해당 배치의 실행 내역이 없습니다."));
        mvc.perform(get(ROOT+"/actions/"+ACTION).with(user("qa").roles("ADMIN"))).andExpect(status().isNotFound()).andExpect(jsonPath("$.success").value(false));
    }
}
