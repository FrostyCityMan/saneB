package com.saneb.domain.announcementattachment.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.common.error.ApiException;
import com.saneb.common.error.ErrorCode;
import com.saneb.domain.announcementattachment.dto.AttachmentJobResponse;
import com.saneb.domain.announcementattachment.dto.AttachmentRoleRequest;
import com.saneb.domain.announcementattachment.dto.AttachmentReviewRequests;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentRoleService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties="spring.flyway.enabled=false")
@AutoConfigureMockMvc
class AnnouncementAttachmentRoleControllerSmokeTest {
    private static final UUID SOURCE=UUID.randomUUID(),JOB=UUID.randomUUID(),SET=UUID.randomUUID();
    private static final String ROOT="/api/v2/admin/announcement-sources/"+SOURCE;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @MockitoBean AnnouncementAttachmentRoleService service;
    private AttachmentJobResponse result() { return new AttachmentJobResponse(SOURCE,JOB,SET,"ROLE_CHANGE","PENDING",2,4,2,null); }
    private String body() throws Exception {
        return mapper.writeValueAsString(new AttachmentRoleRequest(new AttachmentReviewRequests.Version(UUID.randomUUID(),UUID.randomUUID(),2,3,"a".repeat(64)),
                SET,List.of(new AttachmentRoleRequest.FileRole(UUID.randomUUID(),"GUIDE")),"공개 첨부 역할 검수 QA"));
    }
    @ParameterizedTest @ValueSource(strings={"ADMIN","OPERATOR"})
    void writersReceive202QueuedResultNotCompletedClassification(String role) throws Exception {
        when(service.insertRoleChange(any(),eq(SOURCE),any(),any())).thenReturn(result());
        mvc.perform(put(ROOT+"/attachment-roles").with(user("qa").roles(role)).with(csrf()).header("Idempotency-Key",UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON).content(body())).andExpect(status().isAccepted()).andExpect(header().string("Cache-Control","no-store"))
                .andExpect(jsonPath("$.data.operationCode").value("ROLE_CHANGE")).andExpect(jsonPath("$.data.jobStatusCode").value("PENDING"))
                .andExpect(jsonPath("$.data.leaseToken").doesNotExist()).andExpect(jsonPath("$.data.requestHash").doesNotExist());
    }
    @ParameterizedTest @ValueSource(strings={"ADMIN","OPERATOR","APPROVER"})
    void readRolesCanPollSafeSourceScopedJob(String role) throws Exception {
        when(service.selectJobDetails(SOURCE,JOB)).thenReturn(result());
        mvc.perform(get(ROOT+"/attachment-jobs/"+JOB).with(user("qa").roles(role))).andExpect(status().isOk())
                .andExpect(header().string("Cache-Control","no-store")).andExpect(jsonPath("$.data.attachmentVersionAtReservation").value(4));
    }
    @ParameterizedTest @ValueSource(strings={"APPROVER","USER","PARTNER","REVIEWER"})
    void nonWritersCannotChangeRolesEvenWithCsrf(String role) throws Exception {
        mvc.perform(put(ROOT+"/attachment-roles").with(user("qa").roles(role)).with(csrf()).header("Idempotency-Key",UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON).content(body())).andExpect(status().isForbidden()); verifyNoInteractions(service);
    }
    @Test void csrfAndIdempotencyAreRequiredBeforeMutation() throws Exception {
        mvc.perform(put(ROOT+"/attachment-roles").with(user("qa").roles("ADMIN")).header("Idempotency-Key",UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON).content(body())).andExpect(status().isForbidden());
        mvc.perform(put(ROOT+"/attachment-roles").with(user("qa").roles("ADMIN")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Idempotency-Key")));
        verifyNoInteractions(service);
    }
    @Test void anonymousAndWrongSourceAndVersionConflictAreNotSuccessful() throws Exception {
        mvc.perform(get(ROOT+"/attachment-jobs/"+JOB)).andExpect(status().isUnauthorized());
        when(service.selectJobDetails(SOURCE,JOB)).thenThrow(new ApiException(ErrorCode.RESOURCE_NOT_FOUND,HttpStatus.NOT_FOUND,"이 원문의 작업을 찾을 수 없습니다."));
        mvc.perform(get(ROOT+"/attachment-jobs/"+JOB).with(user("qa").roles("ADMIN"))).andExpect(status().isNotFound());
        when(service.insertRoleChange(any(),eq(SOURCE),any(),any())).thenThrow(new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_VERSION_CONFLICT,HttpStatus.CONFLICT,"첨부 버전이 변경됐습니다."));
        mvc.perform(put(ROOT+"/attachment-roles").with(user("qa").roles("ADMIN")).with(csrf()).header("Idempotency-Key",UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON).content(body())).andExpect(status().isConflict()).andExpect(jsonPath("$.success").value(false));
    }
}
