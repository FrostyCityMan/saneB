package com.saneb.domain.announcementattachment.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.saneb.common.response.PageResponse;
import com.saneb.domain.announcementattachment.dto.*;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentBackfillService;
import java.time.OffsetDateTime;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties="spring.flyway.enabled=false") @AutoConfigureMockMvc
class AnnouncementAttachmentBackfillControllerSmokeTest {
    private static final UUID POLICY=UUID.randomUUID(),RUN=UUID.randomUUID(),KEY=UUID.randomUUID();
    private static final String ROOT="/api/v2/admin/announcement-attachment-backfills";
    private static final String SCOPE="{\"policyId\":\""+POLICY+"\",\"providerCodes\":[\"BIZINFO\"],\"collectedFrom\":\"2026-08-01T00:00:00+09:00\",\"collectedBefore\":\"2026-09-01T00:00:00+09:00\",\"segmentSize\":1000}";
    private static final String INPUT="{\"scope\":"+SCOPE+",\"expectedScopeHash\":\""+"a".repeat(64)+"\",\"expectedCandidateCount\":1001,\"reason\":\"전체 목록 고정\"}";
    @Autowired MockMvc mvc;
    @MockitoBean AnnouncementAttachmentBackfillService service;
    private AttachmentBackfillResponses.Inventory response() {return new AttachmentBackfillResponses.Inventory(RUN,POLICY,"INVENTORIED","a".repeat(64),"b".repeat(64),0,1001,1001,0,1000,2,Map.of("inventoryOnly",true),OffsetDateTime.now());}
    @Test void createdInventoryIsNotAJobOrCollectionApproval() throws Exception {
        when(service.insertInventory(any(),eq(KEY),any())).thenReturn(response());
        mvc.perform(post(ROOT).with(user("qa").roles("ADMIN")).with(csrf()).header("Idempotency-Key",KEY).contentType(MediaType.APPLICATION_JSON).content(INPUT))
                .andExpect(status().isCreated()).andExpect(header().string("Cache-Control","no-store")).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.statusCode").value("INVENTORIED")).andExpect(jsonPath("$.data.candidateCount").value(1001))
                .andExpect(jsonPath("$.data.segmentCount").value(2)).andExpect(jsonPath("$.data.requestHash").doesNotExist()).andExpect(jsonPath("$.data.requestedBy").doesNotExist());
    }
    @ParameterizedTest @ValueSource(strings={"ADMIN","OPERATOR","APPROVER"})
    void readRolesCanPreviewAndNavigateFrozenPages(String role) throws Exception {
        when(service.selectRunList(any(),eq(2),eq(10))).thenReturn(PageResponse.of(List.of(response()),2,10,11));
        mvc.perform(post(ROOT+"/scope-preview").with(user("qa").roles(role)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(SCOPE)).andExpect(status().isOk());
        mvc.perform(get(ROOT).with(user("qa").roles(role)).param("page","2").param("size","10")).andExpect(status().isOk()).andExpect(jsonPath("$.data.totalPages").value(2));
        for(String suffix:List.of("/"+RUN,"/"+RUN+"/segments","/"+RUN+"/segments/2/items"))
            mvc.perform(get(ROOT+suffix).with(user("qa").roles(role))).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"));
    }
    @ParameterizedTest @ValueSource(strings={"OPERATOR","APPROVER","USER","PARTNER","REVIEWER"})
    void nonAdminCannotFreeze(String role) throws Exception {
        mvc.perform(post(ROOT).with(user("qa").roles(role)).with(csrf()).header("Idempotency-Key",KEY).contentType(MediaType.APPLICATION_JSON).content(INPUT)).andExpect(status().isForbidden());verifyNoInteractions(service);
    }
    @ParameterizedTest @ValueSource(strings={"USER","PARTNER","REVIEWER"})
    void externalRolesCannotReadAnyInventory(String role) throws Exception {
        for(String suffix:List.of("","/"+RUN,"/"+RUN+"/segments","/"+RUN+"/segments/2/items"))
            mvc.perform(get(ROOT+suffix).with(user("qa").roles(role))).andExpect(status().isForbidden());verifyNoInteractions(service);
    }
    @ParameterizedTest @ValueSource(strings={"maximumCount","sourceIds","sourceUrl","profileCode","passed","includeLinked"})
    void unknownNestedScopeCannotNarrowOrBypassTheFullInventory(String field) throws Exception {
        String body=SCOPE.substring(0,SCOPE.length()-1)+",\""+field+"\":\"untrusted-input\"}";
        mvc.perform(post(ROOT+"/scope-preview").with(user("qa").roles("ADMIN")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("untrusted-input"))));verifyNoInteractions(service);
    }
    @Test void anonymousMissingCsrfKeyAndCandidateCountAreRejected() throws Exception {
        mvc.perform(get(ROOT)).andExpect(status().isUnauthorized());
        for(String path:List.of(ROOT,ROOT+"/scope-preview"))
            mvc.perform(post(path).with(user("qa").roles("ADMIN")).header("Idempotency-Key",KEY).contentType(MediaType.APPLICATION_JSON).content(path.equals(ROOT)?INPUT:SCOPE)).andExpect(status().isForbidden());
        mvc.perform(post(ROOT).with(user("qa").roles("ADMIN")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(INPUT)).andExpect(status().isBadRequest());
        mvc.perform(post(ROOT).with(user("qa").roles("ADMIN")).with(csrf()).header("Idempotency-Key",KEY).contentType(MediaType.APPLICATION_JSON).content(INPUT.replace("\"expectedCandidateCount\":1001,",""))).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}
