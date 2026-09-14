package com.saneb.domain.announcementattachment.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.common.error.ApiException;
import com.saneb.common.error.ErrorCode;
import com.saneb.domain.announcementattachment.dto.AttachmentReviewRequests;
import com.saneb.domain.announcementattachment.dto.AttachmentReviewResponses;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentReviewService;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceLinkResponse;
import java.time.OffsetDateTime;
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
class AnnouncementAttachmentReviewControllerSmokeTest {
    private static final UUID SOURCE=UUID.randomUUID(), EVALUATION=UUID.randomUUID(), CONFIRMATION=UUID.randomUUID();
    private static final String ROOT="/api/v2/admin/announcement-sources/"+SOURCE+"/attachment-classification";
    private final AttachmentReviewRequests.Version version=new AttachmentReviewRequests.Version(UUID.randomUUID(),EVALUATION,1,2,"a".repeat(64));
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @MockitoBean AnnouncementAttachmentReviewService service;
    private String confirmationBody() throws Exception {
        return mapper.writeValueAsString(new AttachmentReviewRequests.Confirmation(version,List.of("BUSINESS"),List.of("POLICY_FINANCE"),
                "MANUAL_SOURCE_CHECK",List.of("OCR_REQUIRED"),"공개 원문 전체 수동 검수 QA"));
    }
    private String conversionBody() throws Exception {
        return mapper.writeValueAsString(new AttachmentReviewRequests.Conversion(version,CONFIRMATION,"BUSINESS",null));
    }
    @ParameterizedTest @ValueSource(strings={"ADMIN","OPERATOR","APPROVER"})
    void readRolesReceiveVersionedReviewContextWithoutCache(String role) throws Exception {
        when(service.selectReviewContextDetails(SOURCE)).thenReturn(new AttachmentReviewResponses.Context(SOURCE,version,"REVIEW_REQUIRED","ATTACHMENT_INCOMPLETE",true,List.of("OCR_REQUIRED")));
        mvc.perform(get(ROOT+"/review-context").with(user("qa").roles(role))).andExpect(status().isOk())
                .andExpect(header().string("Cache-Control","no-store")).andExpect(jsonPath("$.data.manualSourceCheckRequired").value(true))
                .andExpect(jsonPath("$.data.version.expectedAttachmentVersion").value(2));
    }
    @ParameterizedTest @ValueSource(strings={"ADMIN","OPERATOR"})
    void writerCanConfirmAndCreateDraftUsingWrappedNoStoreEndpoints(String role) throws Exception {
        when(service.insertConfirmation(any(),eq(SOURCE),any(),any())).thenReturn(new AttachmentReviewResponses.Confirmation(SOURCE,CONFIRMATION,EVALUATION,
                version.expectedSetHash(),1,3,"MANUAL_SOURCE_CHECK",true,OffsetDateTime.now()));
        when(service.insertOperationalAnnouncement(any(),eq(SOURCE),any())).thenReturn(new AnnouncementSourceLinkResponse(SOURCE,"SRC-QA",UUID.randomUUID(),"ANN-QA"));
        mvc.perform(post(ROOT+"/confirmations").with(user("qa").roles(role)).with(csrf()).header("Idempotency-Key",UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON).content(confirmationBody())).andExpect(status().isOk())
                .andExpect(header().string("Cache-Control","no-store")).andExpect(jsonPath("$.data.attachmentVersion").value(3))
                .andExpect(jsonPath("$.data.reviewNote").doesNotExist());
        mvc.perform(post(ROOT+"/announcements").with(user("qa").roles(role)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(conversionBody()))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store")).andExpect(jsonPath("$.data.announcementCode").value("ANN-QA"));
    }
    @Test void contextAdditivelyReturnsStoredTagsAndLinkWithoutPrivateConfirmationData() throws Exception {
        var confirmed=new AttachmentReviewResponses.ConfirmedClassification(new AttachmentReviewResponses.Confirmation(SOURCE,CONFIRMATION,EVALUATION,
                version.expectedSetHash(),1,2,"EXTRACTED_TEXT",true,OffsetDateTime.now()),List.of("BUSINESS","PERSONAL"),List.of("POLICY_FINANCE"));
        when(service.selectReviewContextDetails(SOURCE)).thenReturn(new AttachmentReviewResponses.Context(SOURCE,version,"ACCEPTED","TARGET_SUPPORT_MATCH",false,List.of(),confirmed,null));
        mvc.perform(get(ROOT+"/review-context").with(user("qa").roles("APPROVER"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.confirmedClassification.targetCategoryCodes[1]").value("PERSONAL"))
                .andExpect(jsonPath("$.data.confirmedClassification.confirmation.reviewNote").doesNotExist())
                .andExpect(jsonPath("$.data.confirmedClassification.confirmation.idempotencyKey").doesNotExist())
                .andExpect(header().string("Cache-Control","no-store"));
    }
    @ParameterizedTest @ValueSource(strings={"APPROVER","USER","PARTNER","REVIEWER"})
    void readOnlyAndExternalRolesCannotMutateEvenWithCsrf(String role) throws Exception {
        mvc.perform(post(ROOT+"/confirmations").with(user("qa").roles(role)).with(csrf()).header("Idempotency-Key",UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON).content(confirmationBody())).andExpect(status().isForbidden());
        mvc.perform(post(ROOT+"/announcements").with(user("qa").roles(role)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(conversionBody()))
                .andExpect(status().isForbidden()); verifyNoInteractions(service);
    }
    @Test void anonymousAndExternalRoleCannotReadReviewContext() throws Exception {
        mvc.perform(get(ROOT+"/review-context")).andExpect(status().isUnauthorized());
        mvc.perform(get(ROOT+"/review-context").with(user("qa").roles("USER"))).andExpect(status().isForbidden()); verifyNoInteractions(service);
    }
    @Test void bothWritesRequireCsrfAndConfirmationRequiresUuidKey() throws Exception {
        mvc.perform(post(ROOT+"/confirmations").with(user("qa").roles("ADMIN")).header("Idempotency-Key",UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON).content(confirmationBody())).andExpect(status().isForbidden());
        mvc.perform(post(ROOT+"/announcements").with(user("qa").roles("ADMIN")).contentType(MediaType.APPLICATION_JSON).content(conversionBody()))
                .andExpect(status().isForbidden());
        mvc.perform(post(ROOT+"/confirmations").with(user("qa").roles("ADMIN")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(confirmationBody()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Idempotency-Key")));
        mvc.perform(post(ROOT+"/confirmations").with(user("qa").roles("ADMIN")).with(csrf()).header("Idempotency-Key","not-uuid")
                .contentType(MediaType.APPLICATION_JSON).content(confirmationBody())).andExpect(status().isBadRequest()); verifyNoInteractions(service);
    }
    @Test void invalidNestedVersionCannotReachService() throws Exception {
        mvc.perform(post(ROOT+"/confirmations").with(user("qa").roles("ADMIN")).with(csrf()).header("Idempotency-Key",UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON).content(confirmationBody().replace("\"expectedSourceVersion\":1","\"expectedSourceVersion\":-1")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.success").value(false)); verifyNoInteractions(service);
    }
    @Test void staleConflictAndWrongSourceAreNotSuccess() throws Exception {
        when(service.insertConfirmation(any(),eq(SOURCE),any(),any())).thenThrow(new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_VERSION_CONFLICT,HttpStatus.CONFLICT,"첨부 버전이 변경됐습니다. 입력을 유지하고 다시 확인하세요."));
        mvc.perform(post(ROOT+"/confirmations").with(user("qa").roles("ADMIN")).with(csrf()).header("Idempotency-Key",UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON).content(confirmationBody())).andExpect(status().isConflict())
                .andExpect(jsonPath("$.data.errorCode").value("ANNOUNCEMENT_ATTACHMENT_VERSION_CONFLICT"));
        when(service.insertOperationalAnnouncement(any(),eq(SOURCE),any())).thenThrow(new ApiException(ErrorCode.RESOURCE_NOT_FOUND,HttpStatus.NOT_FOUND,"해당 원문에 속한 확인을 찾을 수 없습니다."));
        mvc.perform(post(ROOT+"/announcements").with(user("qa").roles("ADMIN")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(conversionBody()))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.success").value(false));
    }
}
