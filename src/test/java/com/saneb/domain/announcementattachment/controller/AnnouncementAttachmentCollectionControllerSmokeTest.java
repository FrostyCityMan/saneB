package com.saneb.domain.announcementattachment.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.common.error.ApiException;
import com.saneb.common.error.ErrorCode;
import com.saneb.domain.announcementattachment.dto.*;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentCollectionService;
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

@SpringBootTest(properties="spring.flyway.enabled=false")
@AutoConfigureMockMvc
class AnnouncementAttachmentCollectionControllerSmokeTest {
    private static final UUID SOURCE=UUID.randomUUID(),POLICY=UUID.randomUUID(),BASE=UUID.randomUUID(),KEY=UUID.randomUUID();
    private static final String ROOT="/api/v2/admin/announcement-sources/"+SOURCE;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @MockitoBean AnnouncementAttachmentCollectionService service;
    private AttachmentCollectionRequests.Version version() { return new AttachmentCollectionRequests.Version(BASE,null,0,0); }
    private String body(long bytes) throws Exception { return mapper.writeValueAsString(new AttachmentCollectionRequests.Request(version(),POLICY,"a".repeat(64),"b".repeat(64),bytes,"전체 수집 QA")); }
    @ParameterizedTest @ValueSource(strings={"ADMIN","OPERATOR","APPROVER"})
    void readRolesSeeNoNetworkScopeAndInitialVersionWithoutPrivateLocators(String role) throws Exception {
        when(service.selectCollectionContextDetails(any(),eq(SOURCE))).thenReturn(new AttachmentCollectionContext(version(),POLICY,"a".repeat(64),"b".repeat(64),"ENFORCE",83886080,10,3,132,false,"COLLECT_PREVIEW_ONLY"));
        mvc.perform(get(ROOT+"/attachment-collection-context").with(user("qa").roles(role))).andExpect(status().isOk())
                .andExpect(header().string("Cache-Control","no-store")).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.maximumHttpRequests").value(132)).andExpect(jsonPath("$.data.version.expectedAttachmentDecisionId").isEmpty())
                .andExpect(jsonPath("$.data.effectCode").value("COLLECT_PREVIEW_ONLY")).andExpect(jsonPath("$.data.profileCode").doesNotExist())
                .andExpect(jsonPath("$.data.sourceUrl").doesNotExist());
    }
    @ParameterizedTest @ValueSource(strings={"ADMIN","OPERATOR"})
    void writeRolesCanReserveInitialFullCollectionWithCsrfAndIdempotency(String role) throws Exception {
        when(service.insertCollectionJob(any(),eq(SOURCE),eq(KEY),any())).thenReturn(new AttachmentJobResponse(SOURCE,UUID.randomUUID(),null,"COLLECT","PENDING",0,1,1,null));
        mvc.perform(post(ROOT+"/attachment-jobs/collection").with(user("qa").roles(role)).with(csrf()).header("Idempotency-Key",KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(body(83886080)))
                .andExpect(status().isAccepted()).andExpect(header().string("Cache-Control","no-store"))
                .andExpect(jsonPath("$.data.operationCode").value("COLLECT")).andExpect(jsonPath("$.data.jobStatusCode").value("PENDING"));
        var request=org.mockito.ArgumentCaptor.forClass(AttachmentCollectionRequests.Request.class);
        verify(service).insertCollectionJob(any(),eq(SOURCE),eq(KEY),request.capture());
        org.assertj.core.api.Assertions.assertThat(request.getValue().version().expectedAttachmentDecisionId()).isNull();
    }
    @ParameterizedTest @ValueSource(strings={"APPROVER","USER","PARTNER","REVIEWER"})
    void nonWriteRolesCannotStartCollectionEvenWithCsrf(String role) throws Exception {
        mvc.perform(post(ROOT+"/attachment-jobs/collection").with(user("qa").roles(role)).with(csrf()).header("Idempotency-Key",KEY)
                .contentType(MediaType.APPLICATION_JSON).content(body(1))).andExpect(status().isForbidden());verifyNoInteractions(service);
    }
    @Test void csrfAndRequestKeyAreMandatory() throws Exception {
        mvc.perform(post(ROOT+"/attachment-jobs/collection").with(user("qa").roles("ADMIN")).header("Idempotency-Key",KEY)
                .contentType(MediaType.APPLICATION_JSON).content(body(1))).andExpect(status().isForbidden()).andExpect(jsonPath("$.success").value(false));
        mvc.perform(post(ROOT+"/attachment-jobs/collection").with(user("qa").roles("ADMIN")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(body(1))).andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Idempotency-Key")));
        verifyNoInteractions(service);
    }
    @Test void invalidVersionsBudgetAndHashCannotReachService() throws Exception {
        mvc.perform(post(ROOT+"/attachment-jobs/collection").with(user("qa").roles("ADMIN")).with(csrf()).header("Idempotency-Key",KEY)
                .contentType(MediaType.APPLICATION_JSON).content(body(83886081))).andExpect(status().isBadRequest());
        var bad=new AttachmentCollectionRequests.Request(new AttachmentCollectionRequests.Version(BASE,null,0,null),POLICY,"bad","b".repeat(64),1L,"QA");
        mvc.perform(post(ROOT+"/attachment-jobs/collection").with(user("qa").roles("ADMIN")).with(csrf()).header("Idempotency-Key",KEY)
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(bad))).andExpect(status().isBadRequest());verifyNoInteractions(service);
    }
    @Test void anonymousAndExternalReadRolesCannotReadContext() throws Exception {
        mvc.perform(get(ROOT+"/attachment-collection-context")).andExpect(status().isUnauthorized());
        mvc.perform(get(ROOT+"/attachment-collection-context").with(user("qa").roles("USER"))).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
    @Test void staleAndRateErrorsRemainSpecificWrappers() throws Exception {
        when(service.insertCollectionJob(any(),eq(SOURCE),eq(KEY),any())).thenThrow(new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_VERSION_CONFLICT,HttpStatus.CONFLICT,"정책이 변경됐습니다. 입력을 보존하고 조건을 다시 확인하세요."));
        mvc.perform(post(ROOT+"/attachment-jobs/collection").with(user("qa").roles("ADMIN")).with(csrf()).header("Idempotency-Key",KEY)
                .contentType(MediaType.APPLICATION_JSON).content(body(1))).andExpect(status().isConflict())
                .andExpect(jsonPath("$.data.errorCode").value("ANNOUNCEMENT_ATTACHMENT_VERSION_CONFLICT"));
        when(service.selectCollectionContextDetails(any(),eq(SOURCE))).thenThrow(new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_COLLECTION_RATE_LIMITED,HttpStatus.TOO_MANY_REQUESTS,"원문별 24시간 최대 3회입니다."));
        mvc.perform(get(ROOT+"/attachment-collection-context").with(user("qa").roles("ADMIN"))).andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.data.errorCode").value("ANNOUNCEMENT_ATTACHMENT_COLLECTION_RATE_LIMITED"));
    }
}
