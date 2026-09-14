package com.saneb.domain.announcementattachment.controller;

import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.saneb.common.error.*;
import com.saneb.domain.announcementattachment.dto.AttachmentPolicyPublicationImpact;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentPolicyPublicationImpactService;
import java.time.OffsetDateTime;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties="spring.flyway.enabled=false") @AutoConfigureMockMvc
class AnnouncementAttachmentPolicyPublicationImpactControllerSmokeTest {
    private static final UUID POLICY=UUID.randomUUID();private static final String PATH="/api/v2/admin/announcement-attachment-policies/"+POLICY+"/publication-impact";
    @Autowired MockMvc mvc;@MockitoBean AnnouncementAttachmentPolicyPublicationImpactService service;
    @ParameterizedTest @ValueSource(strings={"ADMIN","OPERATOR","APPROVER"})
    void internalRolesReceiveReadOnlyObservedImpact(String role) throws Exception {
        var counts=new AttachmentPolicyPublicationImpact.Counts(0L,0L,0L,0L,0L,0L,0L,0L,0L);
        when(service.selectImpactDetails(any(),eq(POLICY))).thenReturn(new AttachmentPolicyPublicationImpact(null,null,counts,counts,1024L,false,false,null,List.of("QA_NOT_REQUESTED","PUBLICATION_REVALIDATION_REQUIRED"),true,"a".repeat(64),OffsetDateTime.now(),0));
        mvc.perform(get(PATH).with(user("impact-qa").roles(role))).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"))
                .andExpect(jsonPath("$.success").value(true)).andExpect(jsonPath("$.data.requiresPublicationRevalidation").value(true)).andExpect(jsonPath("$.data.currentHttpRequests").value(0))
                .andExpect(jsonPath("$.data.canPublish").doesNotExist()).andExpect(jsonPath("$.data.sourceIds").doesNotExist()).andExpect(jsonPath("$.data.requestHash").doesNotExist());
    }
    @ParameterizedTest @ValueSource(strings={"USER","PARTNER","REVIEWER"})
    void externalRolesCannotRead(String role) throws Exception {mvc.perform(get(PATH).with(user("impact-qa").roles(role))).andExpect(status().isForbidden());verifyNoInteractions(service);}
    @Test void anonymousCannotRead() throws Exception {mvc.perform(get(PATH)).andExpect(status().isUnauthorized());verifyNoInteractions(service);}
    @Test void malformedUuidReturnsWrapper() throws Exception {mvc.perform(get(PATH.replace(POLICY.toString(),"bad")).with(user("impact-qa").roles("ADMIN"))).andExpect(status().isBadRequest()).andExpect(jsonPath("$.success").value(false));verifyNoInteractions(service);}
    @Test void missingPolicyAndBadSnapshotRemain404And409() throws Exception {
        when(service.selectImpactDetails(any(),eq(POLICY))).thenThrow(new ApiException(ErrorCode.RESOURCE_NOT_FOUND,HttpStatus.NOT_FOUND,"정책 없음"));mvc.perform(get(PATH).with(user("impact-qa").roles("ADMIN"))).andExpect(status().isNotFound());
        when(service.selectImpactDetails(any(),eq(POLICY))).thenThrow(new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_VERSION_CONFLICT,HttpStatus.CONFLICT,"현재 영향 확인"));mvc.perform(get(PATH).with(user("impact-qa").roles("ADMIN"))).andExpect(status().isConflict());}
    @Test void impactEndpointCannotPublishEvenWithCsrf() throws Exception {mvc.perform(post(PATH).with(user("impact-qa").roles("ADMIN")).with(csrf())).andExpect(status().isMethodNotAllowed());verifyNoInteractions(service);}
}
