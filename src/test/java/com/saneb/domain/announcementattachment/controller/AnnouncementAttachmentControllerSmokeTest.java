package com.saneb.domain.announcementattachment.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.saneb.common.error.ApiException;
import com.saneb.common.error.ErrorCode;
import com.saneb.common.response.PageResponse;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentReadService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "spring.flyway.enabled=false")
@AutoConfigureMockMvc
class AnnouncementAttachmentControllerSmokeTest {
    private static final UUID SOURCE = UUID.randomUUID();
    private static final String ROOT = "/api/v2/admin/announcement-sources/" + SOURCE;
    @Autowired MockMvc mvc;
    @MockitoBean AnnouncementAttachmentReadService service;

    @ParameterizedTest @ValueSource(strings = {"ADMIN", "OPERATOR", "APPROVER"})
    void internalReadRolesReceivePagedWrapper(String role) throws Exception {
        when(service.selectAttachmentSetList(SOURCE, 1, 20)).thenReturn(PageResponse.of(List.of(), 1, 20, 0));
        mvc.perform(get(ROOT + "/attachment-sets").with(user("qa").roles(role)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.data.items").isArray()).andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.totalCount").value(0));
    }

    @ParameterizedTest @ValueSource(strings = {"USER", "PARTNER", "REVIEWER"})
    void externalRolesCannotReadAttachmentEvidence(String role) throws Exception {
        mvc.perform(get(ROOT + "/attachment-sets").with(user("qa").roles(role)))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test void anonymousCannotReadEvidence() throws Exception {
        mvc.perform(get(ROOT + "/attachment-sets")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
        verifyNoInteractions(service);
    }

    @Test void invalidIdentifierAndPageReturnSafeValidationWrapper() throws Exception {
        mvc.perform(get(ROOT + "/attachment-sets/not-a-uuid/files").with(user("qa").roles("ADMIN")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.data.errorCode").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("UUID 형식")));
        mvc.perform(get(ROOT + "/attachment-sets?page=not-an-integer").with(user("qa").roles("ADMIN")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("정수")));
        verifyNoInteractions(service);
    }

    @Test void textBlocksAreNotCachedAndAreNeverWholeFileDownloads() throws Exception {
        UUID extraction = UUID.randomUUID();
        when(service.selectAttachmentBlockList(SOURCE, extraction, 1, 10, 0, 2000))
                .thenReturn(PageResponse.of(List.of(), 1, 10, 0));
        mvc.perform(get(ROOT + "/attachment-extractions/" + extraction + "/blocks").with(user("qa").roles("OPERATOR")))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.data.fileUrl").doesNotExist()).andExpect(jsonPath("$.data.leaseToken").doesNotExist());
    }

    @Test void sourceMismatchReturns404Wrapper() throws Exception {
        when(service.selectAttachmentFileList(any(), any(), anyInt(), anyInt())).thenThrow(
                new ApiException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "이 원문에 속한 첨부 근거를 찾을 수 없습니다."));
        mvc.perform(get(ROOT + "/attachment-sets/" + UUID.randomUUID() + "/files").with(user("qa").roles("ADMIN")))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.errorCode").value("RESOURCE_NOT_FOUND"));
    }

    @ParameterizedTest @ValueSource(strings = {
            "/api/v2/admin/announcement-sources/00000000-0000-0000-0000-000000000001/attachment-jobs",
            "/api/v2/admin/announcement-sources/00000000-0000-0000-0000-000000000001/attachment-classification/confirmations",
            "/api/v2/admin/announcement-attachment-policies",
            "/api/v2/admin/announcement-attachment-batches/00000000-0000-0000-0000-000000000001/application"})
    void newMutationNamespacesRequireCsrfBeforeDispatch(String path) throws Exception {
        mvc.perform(post(path).with(user("qa").roles("ADMIN")))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("보안 확인")));
        // 확인·재시도·정책·배치 적용 handler는 등록되어 있으며 필수 헤더/본문을 검증한다.
        mvc.perform(post(path).with(user("qa").roles("ADMIN")).with(csrf()))
                .andExpect(status().isBadRequest());
    }
}
