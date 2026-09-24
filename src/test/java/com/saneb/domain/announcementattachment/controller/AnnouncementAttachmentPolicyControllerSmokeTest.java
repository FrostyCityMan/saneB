package com.saneb.domain.announcementattachment.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.common.error.ApiException;
import com.saneb.common.error.ErrorCode;
import com.saneb.common.response.PageResponse;
import com.saneb.domain.announcementattachment.dto.*;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentPolicyService;
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

@SpringBootTest(properties="spring.flyway.enabled=false")
@AutoConfigureMockMvc
class AnnouncementAttachmentPolicyControllerSmokeTest {
    private static final String ROOT="/api/v2/admin/announcement-attachment-policies";
    private static final UUID ID=UUID.randomUUID(),RULE=UUID.randomUUID(),KEY=UUID.randomUUID();
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @MockitoBean AnnouncementAttachmentPolicyService service;
    private AttachmentPolicyResponses.Summary summary() {
        return new AttachmentPolicyResponses.Summary(ID,"ATT-QA",1,0,"DRAFT","ENFORCE",RULE,"ACTIVE",null,OffsetDateTime.parse("2026-09-11T11:00:00+09:00"),null);
    }
    private AttachmentPolicyResponses.Details details() {
        return new AttachmentPolicyResponses.Details(summary(),new AttachmentPolicyResponses.Configuration("attachment-v1","1.0.0",null,83886080L),
                List.of(new AttachmentPolicyResponses.Profile("BIZINFO","BIZINFO_DETAIL_V1","a".repeat(64))),null,true,true,summary().createdAt());
    }
    private String create() throws Exception { return mapper.writeValueAsString(new AttachmentPolicyRequests.Create(RULE,"ENFORCE",83886080L,"정책 QA")); }
    private String update() throws Exception { return mapper.writeValueAsString(new AttachmentPolicyRequests.Update(0,RULE,"OFF",1L,"정책 QA")); }
    private String revision() throws Exception { return mapper.writeValueAsString(new AttachmentPolicyRequests.Revision(0,"개정 QA")); }
    @ParameterizedTest @ValueSource(strings={"ADMIN","OPERATOR","APPROVER"})
    void readRolesSeePagedSummaryAndSystemOwnedConfigurationWithoutPrivateInputs(String role) throws Exception {
        when(service.selectPolicyList(any(),eq("DRAFT"),eq(RULE),eq(2),eq(10))).thenReturn(PageResponse.of(List.of(summary()),2,10,11));
        when(service.selectPolicyDetails(any(),eq(ID))).thenReturn(details());
        mvc.perform(get(ROOT).param("status","DRAFT").param("ruleReleaseId",RULE.toString()).param("page","2").param("size","10").with(user("qa").roles(role)))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"))
                .andExpect(jsonPath("$.success").value(true)).andExpect(jsonPath("$.data.totalCount").value(11)).andExpect(jsonPath("$.data.totalPages").value(2))
                .andExpect(jsonPath("$.data.items[0].policyStatusCode").value("DRAFT")).andExpect(jsonPath("$.data.items[0].settingsJson").doesNotExist());
        mvc.perform(get(ROOT+"/"+ID).with(user("qa").roles(role))).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"))
                .andExpect(jsonPath("$.data.configuration.extractorConfigHash").isEmpty()).andExpect(jsonPath("$.data.policy.policyHash").isEmpty())
                .andExpect(jsonPath("$.data.systemProfileBindings[0].profileCode").value("BIZINFO_DETAIL_V1"))
                .andExpect(jsonPath("$.data.isDraftValidationRequired").value(true)).andExpect(jsonPath("$.data.creationRequestHash").doesNotExist())
                .andExpect(jsonPath("$.data.createdBy").doesNotExist()).andExpect(jsonPath("$.data.reason").doesNotExist());
    }
    @Test void adminCanCreateEditAndReviseButResponseIsOnlyDraft() throws Exception {
        when(service.insertPolicy(any(),eq(KEY),any())).thenReturn(details());
        when(service.updatePolicyDraft(any(),eq(ID),any())).thenReturn(details());
        when(service.insertPolicyRevision(any(),eq(ID),eq(KEY),any())).thenReturn(details());
        mvc.perform(post(ROOT).with(user("qa").roles("ADMIN")).with(csrf()).header("Idempotency-Key",KEY).contentType(MediaType.APPLICATION_JSON).content(create()))
                .andExpect(status().isCreated()).andExpect(header().string("Cache-Control","no-store")).andExpect(jsonPath("$.data.policy.policyStatusCode").value("DRAFT"))
                .andExpect(jsonPath("$.data.policy.publishedAt").isEmpty());
        mvc.perform(put(ROOT+"/"+ID).with(user("qa").roles("ADMIN")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(update()))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"));
        mvc.perform(post(ROOT+"/"+ID+"/revisions").with(user("qa").roles("ADMIN")).with(csrf()).header("Idempotency-Key",KEY).contentType(MediaType.APPLICATION_JSON).content(revision()))
                .andExpect(status().isCreated()).andExpect(header().string("Cache-Control","no-store"));
        verify(service).insertPolicy(any(),eq(KEY),eq(new AttachmentPolicyRequests.Create(RULE,"ENFORCE",83886080L,"정책 QA")));
        verify(service).updatePolicyDraft(any(),eq(ID),eq(new AttachmentPolicyRequests.Update(0,RULE,"OFF",1L,"정책 QA")));
        verify(service).insertPolicyRevision(any(),eq(ID),eq(KEY),eq(new AttachmentPolicyRequests.Revision(0,"개정 QA")));
    }
    @ParameterizedTest @ValueSource(strings={"segment-role-1.0.0","segment-role-1.0.2","segment-role-1.0.3"})
    void adminCanExplicitlyChooseKnownSegmentVersionWithoutSendingHash(String version) throws Exception {
        var create=new AttachmentPolicyRequests.Create(RULE,"OFF",1L,"구간 선택",version);
        var update=new AttachmentPolicyRequests.Update(0,RULE,"OFF",1L,"구간 선택",version);
        when(service.insertPolicy(any(),eq(KEY),eq(create))).thenReturn(details());
        when(service.updatePolicyDraft(any(),eq(ID),eq(update))).thenReturn(details());
        mvc.perform(post(ROOT).with(user("qa").roles("ADMIN")).with(csrf()).header("Idempotency-Key",KEY)
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(create))).andExpect(status().isCreated());
        mvc.perform(put(ROOT+"/"+ID).with(user("qa").roles("ADMIN")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(update))).andExpect(status().isOk());
        verify(service).insertPolicy(any(),eq(KEY),eq(create));verify(service).updatePolicyDraft(any(),eq(ID),eq(update));
    }
    @ParameterizedTest @ValueSource(strings={"","segment-role-1.0.1","segment-role-1.0.2 ","future"})
    void invalidSegmentVersionIsRejectedBeforeService(String version) throws Exception {
        mvc.perform(post(ROOT).with(user("qa").roles("ADMIN")).with(csrf()).header("Idempotency-Key",KEY).contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new AttachmentPolicyRequests.Create(RULE,"OFF",1L,"구간 선택",version)))).andExpect(status().isBadRequest());
        mvc.perform(put(ROOT+"/"+ID).with(user("qa").roles("ADMIN")).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new AttachmentPolicyRequests.Update(0,RULE,"OFF",1L,"구간 선택",version)))).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
    @ParameterizedTest @ValueSource(strings={"OPERATOR","APPROVER","USER","PARTNER","REVIEWER"})
    void everyNonAdminRoleIsDeniedForAllDraftMutations(String role) throws Exception {
        mvc.perform(post(ROOT).with(user("qa").roles(role)).with(csrf()).header("Idempotency-Key",KEY).contentType(MediaType.APPLICATION_JSON).content(create())).andExpect(status().isForbidden());
        mvc.perform(put(ROOT+"/"+ID).with(user("qa").roles(role)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(update())).andExpect(status().isForbidden());
        mvc.perform(post(ROOT+"/"+ID+"/revisions").with(user("qa").roles(role)).with(csrf()).header("Idempotency-Key",KEY).contentType(MediaType.APPLICATION_JSON).content(revision())).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
    @Test void csrfIsMandatoryForExactRootAndBothNestedMutations() throws Exception {
        mvc.perform(post(ROOT).with(user("qa").roles("ADMIN")).header("Idempotency-Key",KEY).contentType(MediaType.APPLICATION_JSON).content(create())).andExpect(status().isForbidden());
        mvc.perform(put(ROOT+"/"+ID).with(user("qa").roles("ADMIN")).contentType(MediaType.APPLICATION_JSON).content(update())).andExpect(status().isForbidden());
        mvc.perform(post(ROOT+"/"+ID+"/revisions").with(user("qa").roles("ADMIN")).header("Idempotency-Key",KEY).contentType(MediaType.APPLICATION_JSON).content(revision())).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
    @Test void creationAndRevisionRequireUuidIdempotencyKey() throws Exception {
        for(String key:List.of("","not-a-uuid")) {
            var createRequest=post(ROOT).with(user("qa").roles("ADMIN")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(create());
            var reviseRequest=post(ROOT+"/"+ID+"/revisions").with(user("qa").roles("ADMIN")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(revision());
            if(!key.isEmpty()) { createRequest.header("Idempotency-Key",key);reviseRequest.header("Idempotency-Key",key); }
            mvc.perform(createRequest).andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Idempotency-Key")));
            mvc.perform(reviseRequest).andExpect(status().isBadRequest());
        }
        verifyNoInteractions(service);
    }
    @ParameterizedTest @ValueSource(strings={"url","parser","profileManifest","extractorConfigHash","segmentRulesHash","policyStatusCode","validationPassed","settingsJson"})
    void unknownAndSystemOwnedFieldsAreRejectedForEveryInput(String field) throws Exception {
        for(int operation=0;operation<3;operation++) {
            var tree=mapper.readTree(operation==0?create():operation==1?update():revision());
            ((com.fasterxml.jackson.databind.node.ObjectNode)tree).put(field,"untrusted-input-must-not-be-echoed");
            var request=operation==0?post(ROOT):operation==1?put(ROOT+"/"+ID):post(ROOT+"/"+ID+"/revisions");
            mvc.perform(request.with(user("qa").roles("ADMIN")).with(csrf()).header("Idempotency-Key",KEY).contentType(MediaType.APPLICATION_JSON).content(tree.toString()))
                    .andExpect(status().isBadRequest()).andExpect(header().string("Cache-Control","no-store"))
                    .andExpect(jsonPath("$.data.errorCode").value("ANNOUNCEMENT_ATTACHMENT_POLICY_INVALID"))
                    .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("untrusted-input-must-not-be-echoed"))));
        }
        verifyNoInteractions(service);
    }
    @ParameterizedTest @ValueSource(strings={"ruleReleaseId","modeCode","maximumSourceBytes","reason"})
    void requiredCreateAndEditFieldsCannotBeNull(String field) throws Exception {
        for(int operation=0;operation<2;operation++) {
            var tree=(com.fasterxml.jackson.databind.node.ObjectNode)mapper.readTree(operation==0?create():update());tree.putNull(field);
            var request=operation==0?post(ROOT):put(ROOT+"/"+ID);
            mvc.perform(request.with(user("qa").roles("ADMIN")).with(csrf()).header("Idempotency-Key",KEY).contentType(MediaType.APPLICATION_JSON).content(tree.toString())).andExpect(status().isBadRequest());
        }
        verifyNoInteractions(service);
    }
    @Test void invalidLimitsModesReasonsAndVersionsCannotReachService() throws Exception {
        for(var bad:List.of(new AttachmentPolicyRequests.Create(RULE,"ENFORCE",0L,"QA"),new AttachmentPolicyRequests.Create(RULE,"ENFORCE",83886081L,"QA"),
                new AttachmentPolicyRequests.Create(RULE,"INVALID",1L,"QA"),new AttachmentPolicyRequests.Create(RULE,"OFF",1L," "),new AttachmentPolicyRequests.Create(RULE,"OFF",1L,"x".repeat(1001)))) {
            mvc.perform(post(ROOT).with(user("qa").roles("ADMIN")).with(csrf()).header("Idempotency-Key",KEY).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(bad))).andExpect(status().isBadRequest());
        }
        for(Integer version:Arrays.asList(null,-1,Integer.MAX_VALUE)) {
            mvc.perform(put(ROOT+"/"+ID).with(user("qa").roles("ADMIN")).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(new AttachmentPolicyRequests.Update(version,RULE,"OFF",1L,"QA")))).andExpect(status().isBadRequest());
        }
        for(Integer version:Arrays.asList(null,-1)) {
            mvc.perform(post(ROOT+"/"+ID+"/revisions").with(user("qa").roles("ADMIN")).with(csrf()).header("Idempotency-Key",KEY).contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(new AttachmentPolicyRequests.Revision(version,"QA")))).andExpect(status().isBadRequest());
        }
        verifyNoInteractions(service);
    }
    @Test void conflictAndPublishedPolicyErrorsPreserveSpecificWrappers() throws Exception {
        for(var code:List.of(ErrorCode.ANNOUNCEMENT_ATTACHMENT_VERSION_CONFLICT,ErrorCode.ANNOUNCEMENT_ATTACHMENT_POLICY_NOT_DRAFT)) {
            when(service.updatePolicyDraft(any(),eq(ID),any())).thenThrow(new ApiException(code,HttpStatus.CONFLICT,"입력을 보존하고 최신 정책을 확인하세요."));
            mvc.perform(put(ROOT+"/"+ID).with(user("qa").roles("ADMIN")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(update()))
                    .andExpect(status().isConflict()).andExpect(jsonPath("$.success").value(false)).andExpect(jsonPath("$.data.errorCode").value(code.name()));
        }
    }
    @Test void anonymousAndExternalRolesCannotReadPolicies() throws Exception {
        for(String path:List.of(ROOT,ROOT+"/"+ID)) {
            mvc.perform(get(path)).andExpect(status().isUnauthorized());
            for(String role:List.of("USER","PARTNER","REVIEWER")) mvc.perform(get(path).with(user("qa").roles(role))).andExpect(status().isForbidden());
        }
        verifyNoInteractions(service);
    }
}
