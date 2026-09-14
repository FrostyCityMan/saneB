package com.saneb.domain.announcementattachment.controller;

import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.saneb.common.response.PageResponse;
import com.saneb.domain.announcementattachment.dto.AttachmentPolicyPublicationScope.*;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentPolicyPublicationScopeService;
import java.time.OffsetDateTime;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** 실제 Spring Security/JSON 계약, Service는 대역이다. 브라우저/DB 실행 증거가 아니다. */
@SpringBootTest(properties="spring.flyway.enabled=false") @AutoConfigureMockMvc
class AnnouncementAttachmentPolicyPublicationScopeControllerSmokeTest {
    private static final UUID POLICY=UUID.randomUUID(),SCOPE=UUID.randomUUID(),KEY=UUID.randomUUID();
    private static final String PATH="/api/v2/admin/announcement-attachment-policies/"+POLICY+"/publication-scopes";
    private static final String BODY="{\"expectedVersion\":3,\"reason\":\"게시 준비 검토\"}";
    @Autowired MockMvc mvc;@MockitoBean AnnouncementAttachmentPolicyPublicationScopeService service;
    private Summary summary(){var now=OffsetDateTime.now();return new Summary(SCOPE,POLICY,3,UUID.randomUUID(),4,"COLLECT_ONLY",null,null,1001L,"a".repeat(64),now,now.plusMinutes(10));}
    @Test void adminCreatesPreparationOnlyWithCsrfAndKey() throws Exception {
        when(service.insertScope(any(),eq(POLICY),eq(KEY),any())).thenReturn(new Details(summary(),false,true,false,true,0));
        mvc.perform(post(PATH).with(user("scope-qa").roles("ADMIN")).with(csrf()).header("Idempotency-Key",KEY).contentType("application/json").content(BODY))
                .andExpect(status().isCreated()).andExpect(header().string("Cache-Control","no-store"))
                .andExpect(jsonPath("$.data.scope.itemCount").value(1001)).andExpect(jsonPath("$.data.isApproval").value(false))
                .andExpect(jsonPath("$.data.requiresPublicationRevalidation").value(true)).andExpect(jsonPath("$.data.currentHttpRequests").value(0))
                .andExpect(jsonPath("$.data.canPublish").doesNotExist()).andExpect(jsonPath("$.data.scope.requestHash").doesNotExist());
    }
    @ParameterizedTest @ValueSource(strings={"ADMIN","OPERATOR","APPROVER"})
    void readRolesReceiveParentBoundPagedMetadata(String role) throws Exception {
        when(service.selectScopeList(any(),eq(POLICY),eq(1),eq(20))).thenReturn(PageResponse.of(List.of(summary()),1,20,1));
        when(service.selectScopeDetails(any(),eq(POLICY),eq(SCOPE))).thenReturn(new Details(summary(),false,true,false,true,0));
        when(service.selectItemList(any(),eq(POLICY),eq(SCOPE),eq(2),eq(100))).thenReturn(PageResponse.of(List.of(new Item("SOURCE",UUID.randomUUID(),"b".repeat(64))),2,100,1001));
        mvc.perform(get(PATH).with(user("scope-qa").roles(role))).andExpect(status().isOk()).andExpect(jsonPath("$.data.totalCount").value(1));
        mvc.perform(get(PATH+"/"+SCOPE).with(user("scope-qa").roles(role))).andExpect(status().isOk()).andExpect(jsonPath("$.data.isApproval").value(false));
        mvc.perform(get(PATH+"/"+SCOPE+"/items?page=2&size=100").with(user("scope-qa").roles(role)))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store")).andExpect(jsonPath("$.data.totalCount").value(1001))
                .andExpect(jsonPath("$.data.items[0].sourceUrl").doesNotExist());
    }
    @ParameterizedTest @ValueSource(strings={"OPERATOR","APPROVER","USER","PARTNER","REVIEWER"})
    void nonAdminsCannotPrepareEvenWithCsrf(String role) throws Exception {
        mvc.perform(post(PATH).with(user("scope-qa").roles(role)).with(csrf()).header("Idempotency-Key",KEY).contentType("application/json").content(BODY)).andExpect(status().isForbidden());verifyNoInteractions(service);
    }
    @ParameterizedTest @ValueSource(strings={"USER","PARTNER","REVIEWER"})
    void externalRolesCannotRead(String role) throws Exception {mvc.perform(get(PATH).with(user("scope-qa").roles(role))).andExpect(status().isForbidden());verifyNoInteractions(service);}
    @Test void anonymousAndMissingCsrfAreRejected() throws Exception {
        mvc.perform(get(PATH)).andExpect(status().isUnauthorized());
        mvc.perform(post(PATH).with(user("scope-qa").roles("ADMIN")).header("Idempotency-Key",KEY).contentType("application/json").content(BODY)).andExpect(status().isForbidden());verifyNoInteractions(service);
    }
    @ParameterizedTest @ValueSource(strings={"sourceIds","observedImpactHash","scopeHash","passed","qaRunId","policyStatusCode","maximumCount"})
    void clientCannotForgeFixedMembershipOrQa(String field) throws Exception {
        mvc.perform(post(PATH).with(user("scope-qa").roles("ADMIN")).with(csrf()).header("Idempotency-Key",KEY).contentType("application/json")
                .content(BODY.substring(0,BODY.length()-1)+",\""+field+"\":\"forged\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.success").value(false));verifyNoInteractions(service);
    }
    @Test void invalidUuidHeaderAndValidatedBodyReturnWrapper() throws Exception {
        mvc.perform(get(PATH+"/bad").with(user("scope-qa").roles("ADMIN"))).andExpect(status().isBadRequest()).andExpect(jsonPath("$.success").value(false));
        mvc.perform(post(PATH).with(user("scope-qa").roles("ADMIN")).with(csrf()).contentType("application/json").content(BODY)).andExpect(status().isBadRequest());
        mvc.perform(post(PATH).with(user("scope-qa").roles("ADMIN")).with(csrf()).header("Idempotency-Key","bad").contentType("application/json").content(BODY)).andExpect(status().isBadRequest());
        for(String body:List.of("{\"expectedVersion\":-1,\"reason\":\"사유\"}","{\"expectedVersion\":3,\"reason\":\" \"}"))
            mvc.perform(post(PATH).with(user("scope-qa").roles("ADMIN")).with(csrf()).header("Idempotency-Key",KEY).contentType("application/json").content(body)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.success").value(false));
        verifyNoInteractions(service);
    }
}
