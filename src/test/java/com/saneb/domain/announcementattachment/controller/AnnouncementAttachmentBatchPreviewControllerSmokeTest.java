package com.saneb.domain.announcementattachment.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.saneb.common.error.*;
import com.saneb.common.response.PageResponse;
import com.saneb.domain.announcementattachment.dto.*;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentBatchPreviewService;
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
class AnnouncementAttachmentBatchPreviewControllerSmokeTest {
    private static final UUID BATCH=UUID.randomUUID(),PREVIEW=UUID.randomUUID(),KEY=UUID.randomUUID();
    private static final String ROOT="/api/v2/admin/announcement-attachment-batches/"+BATCH+"/classification-preview";
    private static final String PREPARE="{\"expectedVersion\":5,\"expectedScopeHash\":\""+"a".repeat(64)+"\",\"reason\":\"미리보기\"}";
    private static final String SELECT="{\"expectedVersion\":7,\"expectedPreviewHash\":\""+"b".repeat(64)+"\",\"selectedJobIds\":[],\"reason\":\"선택 해제\"}";
    @Autowired MockMvc mvc;
    @MockitoBean AnnouncementAttachmentBatchPreviewService service;
    private AttachmentBatchPreviewResponses.Preview result() {return new AttachmentBatchPreviewResponses.Preview(PREVIEW,BATCH,"PREVIEW_PARTIAL_FAILED","a".repeat(64),"c".repeat(64),"b".repeat(64),7,7,3,2,1,2,1,1,0,true,true,0,OffsetDateTime.now());}
    @Test void preparationIsCreatedUnselectedPreviewNotAppliedOrHttpCollection() throws Exception {
        when(service.insertPreview(any(),eq(BATCH),eq(KEY),any())).thenReturn(result());
        mvc.perform(post(ROOT).with(user("qa").roles("ADMIN")).with(csrf()).header("Idempotency-Key",KEY).contentType(MediaType.APPLICATION_JSON).content(PREPARE))
                .andExpect(status().isCreated()).andExpect(header().string("Cache-Control","no-store"))
                .andExpect(jsonPath("$.data.statusCode").value("PREVIEW_PARTIAL_FAILED")).andExpect(jsonPath("$.data.selectedItemCount").value(0))
                .andExpect(jsonPath("$.data.currentHttpRequests").value(0)).andExpect(jsonPath("$.data.actorId").doesNotExist());
    }
    @Test void explicitSelectionAcceptsAnEmptyListAndKeepsWrapper() throws Exception {
        when(service.updateSelection(any(),eq(BATCH),eq(KEY),any())).thenReturn(result());
        mvc.perform(put(ROOT+"/selection").with(user("qa").roles("ADMIN")).with(csrf()).header("Idempotency-Key",KEY).contentType(MediaType.APPLICATION_JSON).content(SELECT))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true)).andExpect(jsonPath("$.data.selectedItemCount").value(0));
    }
    @ParameterizedTest @ValueSource(strings={"ADMIN","OPERATOR","APPROVER"})
    void threeReadRolesCanReadCurrentHistoryAndPagedSnapshot(String role) throws Exception {
        when(service.selectCurrentPreviewDetails(any(),eq(BATCH))).thenReturn(result());
        when(service.selectPreviewDetails(any(),eq(BATCH),eq(PREVIEW))).thenReturn(result());
        when(service.selectItemList(any(),eq(BATCH),eq(PREVIEW),eq(2),eq(1))).thenReturn(PageResponse.of(List.of(),2,1,2));
        mvc.perform(get(ROOT).with(user("qa").roles(role))).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"));
        mvc.perform(get(ROOT+"/"+PREVIEW).with(user("qa").roles(role))).andExpect(status().isOk());
        mvc.perform(get(ROOT+"/"+PREVIEW+"/items").with(user("qa").roles(role)).param("page","2").param("size","1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalCount").value(2));
    }
    @ParameterizedTest @ValueSource(strings={"OPERATOR","APPROVER","USER","PARTNER","REVIEWER"})
    void nonAdminCannotCreateOrSelect(String role) throws Exception {
        mvc.perform(post(ROOT).with(user("qa").roles(role)).with(csrf()).header("Idempotency-Key",KEY).contentType(MediaType.APPLICATION_JSON).content(PREPARE)).andExpect(status().isForbidden());
        mvc.perform(put(ROOT+"/selection").with(user("qa").roles(role)).with(csrf()).header("Idempotency-Key",KEY).contentType(MediaType.APPLICATION_JSON).content(SELECT)).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
    @ParameterizedTest @ValueSource(strings={"sourceUrl","filePath","profileCode","passed","policyId","apply"})
    void arbitraryExecutionAndSuccessInputsAreRejected(String field) throws Exception {
        String extra=PREPARE.substring(0,PREPARE.length()-1)+",\""+field+"\":\"untrusted-value\"}";
        mvc.perform(post(ROOT).with(user("qa").roles("ADMIN")).with(csrf()).header("Idempotency-Key",KEY).contentType(MediaType.APPLICATION_JSON).content(extra))
                .andExpect(status().isBadRequest()).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("untrusted-value"))));
        verifyNoInteractions(service);
    }
    @Test void anonymousMissingCsrfMissingKeyAndNullSelectionFailBeforeService() throws Exception {
        mvc.perform(get(ROOT)).andExpect(status().isUnauthorized());
        mvc.perform(post(ROOT).with(user("qa").roles("ADMIN")).header("Idempotency-Key",KEY).contentType(MediaType.APPLICATION_JSON).content(PREPARE)).andExpect(status().isForbidden());
        mvc.perform(post(ROOT).with(user("qa").roles("ADMIN")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(PREPARE)).andExpect(status().isBadRequest());
        mvc.perform(put(ROOT+"/selection").with(user("qa").roles("ADMIN")).with(csrf()).header("Idempotency-Key",KEY).contentType(MediaType.APPLICATION_JSON).content(SELECT.replace("[]","null"))).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
    @Test void stalePreviewAndWrongBatchHistoryKeep409And404Wrappers() throws Exception {
        when(service.updateSelection(any(),eq(BATCH),eq(KEY),any())).thenThrow(new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_VERSION_CONFLICT,HttpStatus.CONFLICT,"미리보기 입력이 바뀌었습니다."));
        mvc.perform(put(ROOT+"/selection").with(user("qa").roles("ADMIN")).with(csrf()).header("Idempotency-Key",KEY).contentType(MediaType.APPLICATION_JSON).content(SELECT))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.success").value(false));
        when(service.selectPreviewDetails(any(),eq(BATCH),eq(PREVIEW))).thenThrow(new ApiException(ErrorCode.RESOURCE_NOT_FOUND,HttpStatus.NOT_FOUND,"이 배치의 미리보기가 아닙니다."));
        mvc.perform(get(ROOT+"/"+PREVIEW).with(user("qa").roles("ADMIN"))).andExpect(status().isNotFound()).andExpect(jsonPath("$.success").value(false));
    }
}
