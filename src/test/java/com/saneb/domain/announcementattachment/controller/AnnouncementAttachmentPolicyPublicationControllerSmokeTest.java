package com.saneb.domain.announcementattachment.controller;

import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.saneb.domain.announcementattachment.dto.AttachmentPolicyPublication.*;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentPolicyPublicationService;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** HTTP 보안/계약만 검증한다. 실제 정책 게시가 아닌 Service 대역이다. */
@SpringBootTest(properties="spring.flyway.enabled=false") @AutoConfigureMockMvc
class AnnouncementAttachmentPolicyPublicationControllerSmokeTest {
    private static final UUID POLICY=UUID.randomUUID(),KEY=UUID.randomUUID();
    private static final String PATH="/api/v2/admin/announcement-attachment-policies/"+POLICY+"/publication";
    private static final String BODY="{\"scopeId\":\""+UUID.randomUUID()+"\",\"scopeHash\":\""+"a".repeat(64)+"\",\"expectedVersion\":0,\"acknowledgeNewCollectionBehavior\":true,\"acknowledgeExistingJobsUnchanged\":true,\"acknowledgeNoBackfill\":true,\"reason\":\"게시 검토\"}";
    @Autowired MockMvc mvc;@MockitoBean AnnouncementAttachmentPolicyPublicationService service;
    private Result result(){return new Result(new Receipt(UUID.randomUUID(),POLICY,1,"a".repeat(64),null,null,UUID.randomUUID(),"b".repeat(64),UUID.randomUUID(),"COLLECT_ONLY",OffsetDateTime.now()),false,false,0);}
    @Test void adminPostUsesCsrfKeyAndReturnsPublicationNotBackfill() throws Exception {
        when(service.insertPublication(any(),eq(POLICY),eq(KEY),any())).thenReturn(result());
        mvc.perform(post(PATH).with(user("publication-qa").roles("ADMIN")).with(csrf()).header("Idempotency-Key",KEY).contentType("application/json").content(BODY))
                .andExpect(status().isCreated()).andExpect(header().string("Cache-Control","no-store")).andExpect(jsonPath("$.data.publication.publishedPolicyVersion").value(1))
                .andExpect(jsonPath("$.data.existingDataApplied").value(false)).andExpect(jsonPath("$.data.workerEnabledByRequest").value(false));
    }
    @ParameterizedTest @ValueSource(strings={"ADMIN","OPERATOR","APPROVER"})
    void readRolesGetSinglePolicyHistoricalReceipt(String role)throws Exception{
        when(service.selectPublicationDetails(any(),eq(POLICY))).thenReturn(result());
        mvc.perform(get(PATH).with(user("publication-qa").roles(role))).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"));
    }
    @ParameterizedTest @ValueSource(strings={"OPERATOR","APPROVER","USER","PARTNER","REVIEWER"})
    void otherRolesCannotPublish(String role)throws Exception{
        mvc.perform(post(PATH).with(user("publication-qa").roles(role)).with(csrf()).header("Idempotency-Key",KEY).contentType("application/json").content(BODY)).andExpect(status().isForbidden());verifyNoInteractions(service);
    }
    @ParameterizedTest @ValueSource(strings={"passed","qaResult","modeCode","sourceIds","policyHash"})
    void suppliedQaAndSettingsCannotBypassStoredValidation(String field)throws Exception{
        mvc.perform(post(PATH).with(user("publication-qa").roles("ADMIN")).with(csrf()).header("Idempotency-Key",KEY).contentType("application/json").content(BODY.substring(0,BODY.length()-1)+",\""+field+"\":true}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.success").value(false));verifyNoInteractions(service);
    }
    @Test void missingCsrfKeyConsentAndBadUuidAreDenied()throws Exception{
        mvc.perform(get(PATH)).andExpect(status().isUnauthorized());
        mvc.perform(post(PATH).with(user("publication-qa").roles("ADMIN")).header("Idempotency-Key",KEY).contentType("application/json").content(BODY)).andExpect(status().isForbidden());
        mvc.perform(post(PATH).with(user("publication-qa").roles("ADMIN")).with(csrf()).contentType("application/json").content(BODY)).andExpect(status().isBadRequest());
        mvc.perform(post(PATH).with(user("publication-qa").roles("ADMIN")).with(csrf()).header("Idempotency-Key",KEY).contentType("application/json").content(BODY.replace("true","false"))).andExpect(status().isBadRequest());
        mvc.perform(get(PATH.replace(POLICY.toString(),"invalid")).with(user("publication-qa").roles("ADMIN"))).andExpect(status().isBadRequest());verifyNoInteractions(service);
    }
}
