package com.saneb.domain.announcementattachment.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.common.error.*;
import com.saneb.common.response.PageResponse;
import com.saneb.domain.announcementattachment.dto.*;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentPolicyCheckService;
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

@SpringBootTest(properties="spring.flyway.enabled=false") @AutoConfigureMockMvc
class AnnouncementAttachmentPolicyCheckControllerSmokeTest {
    private static final UUID POLICY=UUID.randomUUID(),KEY=UUID.randomUUID();
    private static final String ROOT="/api/v2/admin/announcement-attachment-policies/"+POLICY+"/classification-checks";
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @MockitoBean AnnouncementAttachmentPolicyCheckService service;
    private String request() throws Exception {return mapper.writeValueAsString(new AttachmentPolicyCheckRequest(3,"분류 검증"));}
    private AttachmentPolicyCheckResponse response(boolean current) {
        return new AttachmentPolicyCheckResponse(UUID.randomUUID(),POLICY,3,"a".repeat(64),UUID.randomUUID(),2,"b".repeat(64),"c".repeat(64),"CLASSIFICATION_GOLDEN",
                "attachment-golden-1.0.0","attachment-1.0.0","d".repeat(64),30,java.util.stream.IntStream.rangeClosed(1,30).mapToObj(i->String.format("AG-%03d",i)).toList(),current,OffsetDateTime.parse("2026-09-11T12:00:00+09:00"));
    }
    @Test void adminRunsServerClassificationCheckWithoutPublicationClaims() throws Exception {
        when(service.insertClassificationCheck(any(),eq(POLICY),eq(KEY),any())).thenReturn(response(true));
        mvc.perform(post(ROOT).with(user("qa").roles("ADMIN")).with(csrf()).header("Idempotency-Key",KEY).contentType(MediaType.APPLICATION_JSON).content(request()))
                .andExpect(status().isCreated()).andExpect(header().string("Cache-Control","no-store"))
                .andExpect(jsonPath("$.data.checkTypeCode").value("CLASSIFICATION_GOLDEN")).andExpect(jsonPath("$.data.caseCount").value(30))
                .andExpect(jsonPath("$.data.isCurrent").value(true)).andExpect(jsonPath("$.data.publicationReady").doesNotExist())
                .andExpect(jsonPath("$.data.policyStatusCode").doesNotExist()).andExpect(jsonPath("$.data.reason").doesNotExist());
        verify(service).insertClassificationCheck(any(),eq(POLICY),eq(KEY),eq(new AttachmentPolicyCheckRequest(3,"분류 검증")));
    }
    @ParameterizedTest @ValueSource(strings={"ADMIN","OPERATOR","APPROVER"})
    void readRolesCanSeePagedHistoryAndStaleState(String role) throws Exception {
        when(service.selectCheckList(any(),eq(POLICY),eq(2),eq(10))).thenReturn(PageResponse.of(List.of(response(false)),2,10,11));
        mvc.perform(get(ROOT).with(user("qa").roles(role)).param("page","2").param("size","10")).andExpect(status().isOk())
                .andExpect(header().string("Cache-Control","no-store")).andExpect(jsonPath("$.data.totalPages").value(2))
                .andExpect(jsonPath("$.data.items[0].isCurrent").value(false)).andExpect(jsonPath("$.data.items[0].requestedBy").doesNotExist());
    }
    @ParameterizedTest @ValueSource(strings={"OPERATOR","APPROVER","USER","PARTNER","REVIEWER"})
    void nonAdminCannotRunCheck(String role) throws Exception {
        mvc.perform(post(ROOT).with(user("qa").roles(role)).with(csrf()).header("Idempotency-Key",KEY).contentType(MediaType.APPLICATION_JSON).content(request())).andExpect(status().isForbidden());verifyNoInteractions(service);
    }
    @Test void csrfAndUuidKeyAreMandatory() throws Exception {
        mvc.perform(post(ROOT).with(user("qa").roles("ADMIN")).header("Idempotency-Key",KEY).contentType(MediaType.APPLICATION_JSON).content(request())).andExpect(status().isForbidden());
        mvc.perform(post(ROOT).with(user("qa").roles("ADMIN")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(request())).andExpect(status().isBadRequest());
        mvc.perform(post(ROOT).with(user("qa").roles("ADMIN")).with(csrf()).header("Idempotency-Key","invalid").contentType(MediaType.APPLICATION_JSON).content(request())).andExpect(status().isBadRequest());verifyNoInteractions(service);
    }
    @ParameterizedTest @ValueSource(strings={"passed","caseCount","ruleReleaseId","extractorConfigHash","url","settingsJson"})
    void clientCannotSupplySuccessOrExecutionInputs(String field) throws Exception {
        var json=(com.fasterxml.jackson.databind.node.ObjectNode)mapper.readTree(request());json.put(field,"untrusted-check-input");
        mvc.perform(post(ROOT).with(user("qa").roles("ADMIN")).with(csrf()).header("Idempotency-Key",KEY).contentType(MediaType.APPLICATION_JSON).content(json.toString()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.data.errorCode").value("ANNOUNCEMENT_ATTACHMENT_POLICY_INVALID"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("untrusted-check-input"))));verifyNoInteractions(service);
    }
    @Test void staleAndGoldenFailureAre409NotSuccessfulChecks() throws Exception {
        for(var code:List.of(ErrorCode.ANNOUNCEMENT_ATTACHMENT_VERSION_CONFLICT,ErrorCode.ANNOUNCEMENT_ATTACHMENT_POLICY_QA_FAILED)) {
            when(service.insertClassificationCheck(any(),eq(POLICY),eq(KEY),any())).thenThrow(new ApiException(code,HttpStatus.CONFLICT,"정책과 규칙을 다시 확인하세요."));
            mvc.perform(post(ROOT).with(user("qa").roles("ADMIN")).with(csrf()).header("Idempotency-Key",KEY).contentType(MediaType.APPLICATION_JSON).content(request()))
                    .andExpect(status().isConflict()).andExpect(jsonPath("$.success").value(false)).andExpect(jsonPath("$.data.errorCode").value(code.name()));
        }
    }
    @Test void invalidVersionReasonAndExternalReadsAreRejected() throws Exception {
        for(var input:List.of(new AttachmentPolicyCheckRequest(null,"QA"),new AttachmentPolicyCheckRequest(-1,"QA"),new AttachmentPolicyCheckRequest(0," "),new AttachmentPolicyCheckRequest(0,"x".repeat(1001)))) {
            mvc.perform(post(ROOT).with(user("qa").roles("ADMIN")).with(csrf()).header("Idempotency-Key",KEY).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(input))).andExpect(status().isBadRequest());
        }
        mvc.perform(get(ROOT)).andExpect(status().isUnauthorized());
        for(String role:List.of("USER","PARTNER","REVIEWER")) mvc.perform(get(ROOT).with(user("qa").roles(role))).andExpect(status().isForbidden());verifyNoInteractions(service);
    }
}
