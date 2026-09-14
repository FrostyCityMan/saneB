package com.saneb.domain.announcementattachment.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.saneb.common.error.ApiException;
import com.saneb.common.error.ErrorCode;
import com.saneb.common.response.PageResponse;
import com.saneb.domain.announcementattachment.dto.AttachmentSourceResponses;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentCurrentService;
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

@SpringBootTest(properties="spring.flyway.enabled=false")
@AutoConfigureMockMvc
class AnnouncementAttachmentCurrentControllerSmokeTest {
    private static final String ROOT="/api/v2/admin/announcement-sources";
    @Autowired MockMvc mvc;
    @MockitoBean AnnouncementAttachmentCurrentService service;

    @ParameterizedTest @ValueSource(strings={"ADMIN","OPERATOR","APPROVER"})
    void listReadRolesUsePagedNoStoreContract(String role) throws Exception {
        when(service.selectSourceList(any())).thenReturn(PageResponse.of(List.of(),1,20,0));
        mvc.perform(get(ROOT).with(user("qa").roles(role))).andExpect(status().isOk())
                .andExpect(header().string("Cache-Control","no-store"))
                .andExpect(jsonPath("$.success").value(true)).andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.totalCount").value(0));
    }
    @ParameterizedTest @ValueSource(strings={"USER","PARTNER","REVIEWER"})
    void nonReadRolesCannotUseAnyNewCurrentEndpoint(String role) throws Exception {
        for(String path:List.of(ROOT,ROOT+"/"+UUID.randomUUID(),ROOT+"/"+UUID.randomUUID()+"/attachment-classification"))
            mvc.perform(get(path).with(user("qa").roles(role))).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
    @Test void anonymousReceives401() throws Exception {
        mvc.perform(get(ROOT)).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.success").value(false));
        verifyNoInteractions(service);
    }
    @Test void additiveQueueFilterAndPageAreForwardedWithoutChangingLegacyDefaults() throws Exception {
        when(service.selectSourceList(any())).thenReturn(PageResponse.of(List.of(),2,20,0));
        mvc.perform(get(ROOT).param("processingFlowStatusCode","TECHNICAL_EXCEPTION").param("page","2")
                .with(user("qa").roles("OPERATOR"))).andExpect(status().isOk());
        org.mockito.Mockito.verify(service).selectSourceList(new com.saneb.domain.announcementattachment.vo.AttachmentSourceSearchCondition(
                null,null,null,null,null,null,null,null,2,20,"TECHNICAL_EXCEPTION"));
    }
    @Test void malformedParametersHavePreciseKoreanErrors() throws Exception {
        mvc.perform(get(ROOT+"/bad-uuid").with(user("qa").roles("ADMIN")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("UUID")));
        mvc.perform(get(ROOT+"?collectedFrom=2026-02-31").with(user("qa").roles("ADMIN")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("YYYY-MM-DD")));
        mvc.perform(get(ROOT+"?page=NaN").with(user("qa").roles("ADMIN")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("정수")));
        verifyNoInteractions(service);
    }
    @Test void hiddenSourceReturns404InsteadOfRawBody() throws Exception {
        when(service.selectSourceDetails(any())).thenThrow(new ApiException(ErrorCode.RESOURCE_NOT_FOUND,HttpStatus.NOT_FOUND,"조회할 수 없는 원문입니다."));
        mvc.perform(get(ROOT+"/"+UUID.randomUUID()).with(user("qa").roles("OPERATOR")))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.bodyText").doesNotExist());
    }
    @Test void additiveFlowHasExactBooleanNamesOnListDetailAndClassificationEndpoints() throws Exception {
        var id = UUID.randomUUID();
        var base = new AttachmentSourceResponses.Classification(UUID.randomUUID(), "REVIEW_REQUIRED", "BODY_UNAVAILABLE",
                UUID.randomUUID(), null, null, null, List.of(), List.of());
        var summary = new AttachmentSourceResponses.Summary(id, "QA-SOURCE", "BIZINFO", "공개 공고", "기관", null, null,
                "REVIEW_REQUIRED", base, base, null, null, true, UUID.randomUUID(), 4, 7, null, "NONE",
                new AttachmentSourceResponses.ProcessingFlow("AUTOMATIC_PROCESSING", false, false));
        when(service.selectSourceList(any())).thenReturn(PageResponse.of(List.of(summary), 1, 20, 1));
        when(service.selectSourceDetails(id)).thenReturn(new AttachmentSourceResponses.Details(summary,
                new AttachmentSourceResponses.Content(null, null, null, null, "BODY_UNAVAILABLE")));
        when(service.selectClassificationDetails(id)).thenReturn(summary);
        String[] paths = {ROOT, ROOT + "/" + id, ROOT + "/" + id + "/attachment-classification"};
        String[] prefixes = {"$.data.items[0]", "$.data.source", "$.data"};
        for (int index = 0; index < paths.length; index++) {
            mvc.perform(get(paths[index]).with(user("qa").roles("OPERATOR")))
                    .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                    .andExpect(jsonPath(prefixes[index] + ".processingFlow.statusCode").value("AUTOMATIC_PROCESSING"))
                    .andExpect(jsonPath(prefixes[index] + ".processingFlow.isAutomaticAnalysisComplete").value(false))
                    .andExpect(jsonPath(prefixes[index] + ".processingFlow.isFinalReviewAvailable").value(false))
                    .andExpect(jsonPath(prefixes[index] + ".baseClassification.reasonCode").value("BODY_UNAVAILABLE"));
        }
    }
}
