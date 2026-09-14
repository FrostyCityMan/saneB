package com.saneb.domain.announcementattachment.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.saneb.common.error.*;
import com.saneb.common.response.PageResponse;
import com.saneb.domain.announcementattachment.dto.*;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentBatchService;
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
class AnnouncementAttachmentBatchControllerSmokeTest {
    private static final UUID POLICY=UUID.randomUUID(),BATCH=UUID.randomUUID(),KEY=UUID.randomUUID();
    private static final String ROOT="/api/v2/admin/announcement-attachment-batches";
    private static final String SCOPE="{\"policyId\":\""+POLICY+"\",\"providerCodes\":[\"BIZINFO\"],\"collectedFrom\":\"2026-08-01T00:00:00+09:00\",\"collectedBefore\":\"2026-09-01T00:00:00+09:00\",\"maximumCount\":100}";
    private static final String INPUT="{\"scope\":"+SCOPE+",\"expectedScopeHash\":\""+"a".repeat(64)+"\",\"reason\":\"범위 예약\"}";
    private static final String COLLECTION="{\"expectedVersion\":0,\"expectedScopeHash\":\""+"a".repeat(64)+"\",\"expectedItemCount\":2,\"expectedDeletedItemCount\":0,\"expectedMaximumDownloadBytes\":167772160,\"expectedMaximumHttpRequests\":264,\"reason\":\"수집 승인\"}";
    @Autowired MockMvc mvc;
    @MockitoBean AnnouncementAttachmentBatchService service;
    private AttachmentBatchResponses.Batch response(String state) {return new AttachmentBatchResponses.Batch(BATCH,POLICY,state,"a".repeat(64),0,2,2,0,Map.of(state,2L),Map.of("remainingCount",100),OffsetDateTime.now());}
    @Test void reservationIs201ScopeReadyNotCollectionOrApplication() throws Exception {
        when(service.insertBatch(any(),eq(KEY),any())).thenReturn(response("SCOPE_READY"));
        mvc.perform(post(ROOT).with(user("qa").roles("ADMIN")).with(csrf()).header("Idempotency-Key",KEY).contentType(MediaType.APPLICATION_JSON).content(INPUT))
                .andExpect(status().isCreated()).andExpect(header().string("Cache-Control","no-store"))
                .andExpect(jsonPath("$.data.statusCode").value("SCOPE_READY")).andExpect(jsonPath("$.data.frozenScope.remainingCount").value(100))
                .andExpect(jsonPath("$.data.requestedBy").doesNotExist()).andExpect(jsonPath("$.data.requestHash").doesNotExist());
    }
    @ParameterizedTest @ValueSource(strings={"ADMIN","OPERATOR","APPROVER"})
    void threeReadRolesCanPreviewAndReadPagedMetadata(String role) throws Exception {
        when(service.selectBatchList(any(),eq(2),eq(10))).thenReturn(PageResponse.of(List.of(response("SCOPE_READY")),2,10,11));
        when(service.selectBatchDetails(any(),eq(BATCH))).thenReturn(response("SCOPE_READY"));
        mvc.perform(post(ROOT+"/scope-preview").with(user("qa").roles(role)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(SCOPE)).andExpect(status().isOk());
        mvc.perform(get(ROOT).with(user("qa").roles(role)).param("page","2").param("size","10"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalPages").value(2));
        mvc.perform(get(ROOT+"/"+BATCH).with(user("qa").roles(role))).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"));
    }
    @ParameterizedTest @ValueSource(strings={"OPERATOR","APPROVER","USER","PARTNER","REVIEWER"})
    void nonAdminCannotReserveOrCancel(String role) throws Exception {
        mvc.perform(post(ROOT).with(user("qa").roles(role)).with(csrf()).header("Idempotency-Key",KEY).contentType(MediaType.APPLICATION_JSON).content(INPUT)).andExpect(status().isForbidden());
        mvc.perform(put(ROOT+"/"+BATCH+"/scope-cancellation").with(user("qa").roles(role)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"expectedVersion\":0,\"reason\":\"취소\"}"))
                .andExpect(status().isForbidden());verifyNoInteractions(service);
    }
    @ParameterizedTest @ValueSource(strings={"sourceIds","sourceUrl","profileCode","passed","maximumHttpRequests","includeLinked"})
    void arbitraryTargetsAndExecutionControlsAreRejectedAtNestedScope(String field) throws Exception {
        String scope=SCOPE.substring(0,SCOPE.length()-1)+",\""+field+"\":\"untrusted-input\"}";
        mvc.perform(post(ROOT+"/scope-preview").with(user("qa").roles("ADMIN")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(scope))
                .andExpect(status().isBadRequest()).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("untrusted-input"))));
        verifyNoInteractions(service);
    }
    @Test void missingCsrfKeyAndAnonymousCannotReachService() throws Exception {
        mvc.perform(get(ROOT)).andExpect(status().isUnauthorized());
        mvc.perform(post(ROOT+"/scope-preview").with(user("qa").roles("ADMIN")).contentType(MediaType.APPLICATION_JSON).content(SCOPE)).andExpect(status().isForbidden());
        mvc.perform(post(ROOT).with(user("qa").roles("ADMIN")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(INPUT)).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
    @Test void conflictNotFoundAndCancellationRetainWrappers() throws Exception {
        when(service.selectBatchDetails(any(),eq(BATCH))).thenThrow(new ApiException(ErrorCode.RESOURCE_NOT_FOUND,HttpStatus.NOT_FOUND,"배치가 없습니다."));
        mvc.perform(get(ROOT+"/"+BATCH).with(user("qa").roles("ADMIN"))).andExpect(status().isNotFound()).andExpect(jsonPath("$.success").value(false));
        when(service.insertBatch(any(),eq(KEY),any())).thenThrow(new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_VERSION_CONFLICT,HttpStatus.CONFLICT,"범위가 바뀌었습니다."));
        mvc.perform(post(ROOT).with(user("qa").roles("ADMIN")).with(csrf()).header("Idempotency-Key",KEY).contentType(MediaType.APPLICATION_JSON).content(INPUT))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.success").value(false));
        when(service.updateScopeCancellation(any(),eq(BATCH),any())).thenReturn(response("CANCELLED"));
        mvc.perform(put(ROOT+"/"+BATCH+"/scope-cancellation").with(user("qa").roles("ADMIN")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"expectedVersion\":0,\"reason\":\"취소\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.statusCode").value("CANCELLED"));
    }
    @Test void adminCollectionControlsUseWrappersAndDoNotImplyApplication() throws Exception {
        when(service.updateCollectionStart(any(),eq(BATCH),any())).thenReturn(response("COLLECTION_PENDING"));
        when(service.updateCollectionPause(any(),eq(BATCH),any())).thenReturn(response("COLLECTION_PAUSED"));
        when(service.updateCollectionResume(any(),eq(BATCH),any())).thenReturn(response("COLLECTING"));
        for(String suffix:List.of("collection","collection-pause","collection-resume")) {
            String body=suffix.equals("collection-pause")?"{\"expectedVersion\":0,\"reason\":\"중지\"}":COLLECTION;
            mvc.perform(put(ROOT+"/"+BATCH+"/"+suffix).with(user("qa").roles("ADMIN")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store")).andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.statusCode").value(suffix.equals("collection")?"COLLECTION_PENDING":suffix.equals("collection-pause")?"COLLECTION_PAUSED":"COLLECTING"));
        }
    }
    @ParameterizedTest @ValueSource(strings={"OPERATOR","APPROVER","USER","PARTNER","REVIEWER"})
    void collectionControlIsAdminOnly(String role) throws Exception {
        for(String suffix:List.of("collection","collection-pause","collection-resume")) {
            String body=suffix.equals("collection-pause")?"{\"expectedVersion\":0,\"reason\":\"중지\"}":COLLECTION;
            mvc.perform(put(ROOT+"/"+BATCH+"/"+suffix).with(user("qa").roles(role)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
        }
        verifyNoInteractions(service);
    }
    @Test void collectionRejectsUnknownFieldsMissingCapsCsrfAndStaleVersion() throws Exception {
        for(String body:List.of("{\"expectedVersion\":0,\"reason\":\"승인\"}",COLLECTION.substring(0,COLLECTION.length()-1)+",\"sourceUrl\":\"untrusted-value\"}"))
            mvc.perform(put(ROOT+"/"+BATCH+"/collection").with(user("qa").roles("ADMIN")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest()).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("untrusted-value"))));
        mvc.perform(put(ROOT+"/"+BATCH+"/collection").with(user("qa").roles("ADMIN")).contentType(MediaType.APPLICATION_JSON).content(COLLECTION)).andExpect(status().isForbidden());
        verifyNoInteractions(service);
        when(service.updateCollectionStart(any(),eq(BATCH),any())).thenThrow(new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_VERSION_CONFLICT,HttpStatus.CONFLICT,"배치 버전이 바뀌었습니다."));
        mvc.perform(put(ROOT+"/"+BATCH+"/collection").with(user("qa").roles("ADMIN")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(COLLECTION))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.success").value(false));
    }
}
