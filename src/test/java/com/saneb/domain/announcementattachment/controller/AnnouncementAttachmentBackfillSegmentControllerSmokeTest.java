package com.saneb.domain.announcementattachment.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.saneb.common.error.*;
import com.saneb.domain.announcementattachment.dto.*;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentBackfillSegmentService;
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
class AnnouncementAttachmentBackfillSegmentControllerSmokeTest {
    private static final UUID RUN=UUID.randomUUID(),KEY=UUID.randomUUID(),BATCH=UUID.randomUUID();
    private static final String ROOT="/api/v2/admin/announcement-attachment-backfills/"+RUN;
    private static final String SEGMENT=ROOT+"/segments/2";
    private static final String INPUT="{\"expectedRunVersion\":1,\"expectedSegmentHash\":\""+"a".repeat(64)+"\",\"expectedRemainingItemCount\":1,\"expectedDeletedItemCount\":1,\"reason\":\"분할 예약\"}";
    @Autowired MockMvc mvc;
    @MockitoBean AnnouncementAttachmentBackfillSegmentService service;
    @Test void adminGets201ReservationReceiptNotCollectionSuccess() throws Exception {
        when(service.insertReservation(any(),eq(RUN),eq(2L),eq(KEY),any())).thenReturn(new AttachmentBackfillSegmentResponses.Reservation(RUN,2,BATCH,1,2,1,1,"a".repeat(64),"SCOPE_READY",OffsetDateTime.now()));
        mvc.perform(post(SEGMENT+"/reservation").with(user("qa").roles("ADMIN")).with(csrf()).header("Idempotency-Key",KEY).contentType(MediaType.APPLICATION_JSON).content(INPUT))
                .andExpect(status().isCreated()).andExpect(header().string("Cache-Control","no-store")).andExpect(jsonPath("$.data.currentBatchStatusCode").value("SCOPE_READY"))
                .andExpect(jsonPath("$.data.originalItemCount").value(2)).andExpect(jsonPath("$.data.reservedItemCount").value(1)).andExpect(jsonPath("$.data.deletedBeforeReservation").value(1))
                .andExpect(jsonPath("$.data.requestHash").doesNotExist()).andExpect(jsonPath("$.data.requestedBy").doesNotExist());
    }
    @ParameterizedTest @ValueSource(strings={"ADMIN","OPERATOR","APPROVER"})
    void readRolesCanReadPreviewAndFullSummary(String role) throws Exception {
        when(service.selectSummaryDetails(any(),eq(RUN))).thenReturn(new AttachmentBackfillSegmentResponses.Summary(RUN,3,2,1,2,1,1,1,0,Map.of("SCOPE_READY",1L,"UNRESERVED",1L),Map.of("NOT_REQUESTED",1L,"UNRESERVED",1L),Map.of("NOT_REQUESTED",1L,"UNRESERVED",1L)));
        mvc.perform(get(SEGMENT+"/reservation-preview").with(user("qa").roles(role))).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"));
        mvc.perform(get(ROOT+"/summary").with(user("qa").roles(role))).andExpect(status().isOk()).andExpect(jsonPath("$.data.candidateCount").value(3))
                .andExpect(jsonPath("$.data.collectionCounts.UNRESERVED").value(1)).andExpect(jsonPath("$.data.applicationCounts.APPLIED").doesNotExist());
    }
    @ParameterizedTest @ValueSource(strings={"OPERATOR","APPROVER","USER","PARTNER","REVIEWER"})
    void onlyAdminCanReserve(String role) throws Exception {
        mvc.perform(post(SEGMENT+"/reservation").with(user("qa").roles(role)).with(csrf()).header("Idempotency-Key",KEY).contentType(MediaType.APPLICATION_JSON).content(INPUT)).andExpect(status().isForbidden());verifyNoInteractions(service);
    }
    @ParameterizedTest @ValueSource(strings={"USER","PARTNER","REVIEWER"})
    void nonOperationalRolesCannotRead(String role) throws Exception {
        mvc.perform(get(ROOT+"/summary").with(user("qa").roles(role))).andExpect(status().isForbidden());
        mvc.perform(get(SEGMENT+"/reservation-preview").with(user("qa").roles(role))).andExpect(status().isForbidden());verifyNoInteractions(service);
    }
    @ParameterizedTest @ValueSource(strings={"sourceIds","sourceUrl","profileCode","batchId","force","approved"})
    void arbitraryTargetsOrApprovalOverridesAreRejected(String field) throws Exception {
        String body=INPUT.substring(0,INPUT.length()-1)+",\""+field+"\":\"untrusted-input\"}";
        mvc.perform(post(SEGMENT+"/reservation").with(user("qa").roles("ADMIN")).with(csrf()).header("Idempotency-Key",KEY).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("untrusted-input"))));verifyNoInteractions(service);
    }
    @Test void authenticationCsrfRequiredFieldsAndKeyAreRequired() throws Exception {
        mvc.perform(get(ROOT+"/summary")).andExpect(status().isUnauthorized());
        mvc.perform(post(SEGMENT+"/reservation").with(user("qa").roles("ADMIN")).header("Idempotency-Key",KEY).contentType(MediaType.APPLICATION_JSON).content(INPUT)).andExpect(status().isForbidden());
        mvc.perform(post(SEGMENT+"/reservation").with(user("qa").roles("ADMIN")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(INPUT)).andExpect(status().isBadRequest());
        mvc.perform(post(SEGMENT+"/reservation").with(user("qa").roles("ADMIN")).with(csrf()).header("Idempotency-Key",KEY).contentType(MediaType.APPLICATION_JSON).content(INPUT.replace("\"expectedDeletedItemCount\":1,",""))).andExpect(status().isBadRequest());verifyNoInteractions(service);
    }
    @Test void conflictAndMissingSegmentKeepErrorWrappers() throws Exception {
        when(service.selectReservationPreview(any(),eq(RUN),eq(2L))).thenThrow(new ApiException(ErrorCode.RESOURCE_NOT_FOUND,HttpStatus.NOT_FOUND,"분할이 없습니다."));
        mvc.perform(get(SEGMENT+"/reservation-preview").with(user("qa").roles("ADMIN"))).andExpect(status().isNotFound()).andExpect(jsonPath("$.success").value(false));
        when(service.insertReservation(any(),eq(RUN),eq(2L),eq(KEY),any())).thenThrow(new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_VERSION_CONFLICT,HttpStatus.CONFLICT,"분할이 변경됐습니다."));
        mvc.perform(post(SEGMENT+"/reservation").with(user("qa").roles("ADMIN")).with(csrf()).header("Idempotency-Key",KEY).contentType(MediaType.APPLICATION_JSON).content(INPUT)).andExpect(status().isConflict()).andExpect(jsonPath("$.success").value(false));
    }
}
