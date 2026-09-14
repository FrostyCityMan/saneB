package com.saneb.domain.announcementattachment.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.common.error.ApiException;
import com.saneb.common.error.ErrorCode;
import com.saneb.domain.announcementattachment.dto.*;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentRetryService;
import java.util.List;
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
class AnnouncementAttachmentRetryControllerSmokeTest {
    private static final UUID SOURCE=UUID.randomUUID();
    private static final String ROOT="/api/v2/admin/announcement-sources/"+SOURCE+"/attachment-jobs";
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @MockitoBean AnnouncementAttachmentRetryService service;
    private String body(long bytes) throws Exception {
        return mapper.writeValueAsString(new AttachmentRetryRequest(new AttachmentReviewRequests.Version(UUID.randomUUID(),UUID.randomUUID(),2,3,"a".repeat(64)),
                UUID.randomUUID(),List.of(UUID.randomUUID()),bytes,"실패한 공개 첨부 재시도 QA"));
    }
    @ParameterizedTest @ValueSource(strings={"ADMIN","OPERATOR"})
    void writersReceive202NotCompletedOrActivated(String role) throws Exception {
        when(service.insertFileRetry(any(),eq(SOURCE),any(),any())).thenReturn(new AttachmentJobResponse(SOURCE,UUID.randomUUID(),null,"RETRY_FILES","PENDING",2,4,2,null));
        mvc.perform(post(ROOT).with(user("qa").roles(role)).with(csrf()).header("Idempotency-Key",UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON).content(body(100)))
                .andExpect(status().isAccepted()).andExpect(header().string("Cache-Control","no-store")).andExpect(jsonPath("$.data.operationCode").value("RETRY_FILES"))
                .andExpect(jsonPath("$.data.jobStatusCode").value("PENDING")).andExpect(jsonPath("$.data.leaseToken").doesNotExist())
                .andExpect(jsonPath("$.data.executionSnapshotJson").doesNotExist());
    }
    @ParameterizedTest @ValueSource(strings={"APPROVER","USER","PARTNER","REVIEWER"})
    void nonWritersCannotReserveEvenWithCsrf(String role) throws Exception {
        mvc.perform(post(ROOT).with(user("qa").roles(role)).with(csrf()).header("Idempotency-Key",UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON).content(body(100)))
                .andExpect(status().isForbidden());verifyNoInteractions(service);
    }
    @Test void csrfIdempotencyAndBudgetAreCheckedBeforeMutation() throws Exception {
        mvc.perform(post(ROOT).with(user("qa").roles("ADMIN")).header("Idempotency-Key",UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON).content(body(100)))
                .andExpect(status().isForbidden());
        mvc.perform(post(ROOT).with(user("qa").roles("ADMIN")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body(100)))
                .andExpect(status().isBadRequest());
        mvc.perform(post(ROOT).with(user("qa").roles("ADMIN")).with(csrf()).header("Idempotency-Key",UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON).content(body(83886081)))
                .andExpect(status().isBadRequest());verifyNoInteractions(service);
    }
    @Test void conflictAndRateLimitUseWrappersWithoutSuccessResponse() throws Exception {
        when(service.insertFileRetry(any(),any(),any(),any())).thenThrow(new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_VERSION_CONFLICT,HttpStatus.CONFLICT,"첨부 버전이 변경됐습니다."));
        mvc.perform(post(ROOT).with(user("qa").roles("ADMIN")).with(csrf()).header("Idempotency-Key",UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON).content(body(100)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.success").value(false));
        doThrow(new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_RETRY_RATE_LIMITED,HttpStatus.TOO_MANY_REQUESTS,"60초 간격이 필요합니다."))
                .when(service).insertFileRetry(any(),any(),any(),any());
        mvc.perform(post(ROOT).with(user("qa").roles("ADMIN")).with(csrf()).header("Idempotency-Key",UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON).content(body(100)))
                .andExpect(status().isTooManyRequests()).andExpect(jsonPath("$.success").value(false));
    }
    @Test void anonymousAndForeignSourceAreRejected() throws Exception {
        mvc.perform(post(ROOT).with(csrf()).header("Idempotency-Key",UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON).content(body(100)))
                .andExpect(status().isUnauthorized());
        when(service.insertFileRetry(any(),any(),any(),any())).thenThrow(new ApiException(ErrorCode.RESOURCE_NOT_FOUND,HttpStatus.NOT_FOUND,"이 원문의 파일이 아닙니다."));
        mvc.perform(post(ROOT).with(user("qa").roles("ADMIN")).with(csrf()).header("Idempotency-Key",UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON).content(body(100)))
                .andExpect(status().isNotFound());
    }
}
