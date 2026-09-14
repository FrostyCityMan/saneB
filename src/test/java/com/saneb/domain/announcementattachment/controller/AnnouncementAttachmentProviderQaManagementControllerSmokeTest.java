package com.saneb.domain.announcementattachment.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.saneb.common.response.PageResponse;
import com.saneb.domain.announcementattachment.dto.AttachmentProviderQaResponses;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentProviderQaManagementService;
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
class AnnouncementAttachmentProviderQaManagementControllerSmokeTest {
    private static final UUID POLICY=UUID.randomUUID(),RUN=UUID.randomUUID(),KEY=UUID.randomUUID();
    private static final String ROOT="/api/v2/admin/announcement-attachment-policies/"+POLICY+"/provider-qa-runs";
    private static final String INPUT="""
            {"expectedVersion":0,"expectedSnapshotHash":"%s","expectedCatalogHash":"%s","expectedPlanHash":"%s",
             "segmentNo":1,"expectedCaseCount":1,"maximumRequests":3,"maximumBytes":100,"maximumSecondsIncludingMargin":480,
             "acknowledgeScope":true,"acknowledgeNetworkBudget":true,"acknowledgeIncompleteCoverage":true,"reason":"분할 QA 예약"}
            """.formatted("a".repeat(64),"b".repeat(64),"c".repeat(64));
    @Autowired MockMvc mvc;
    @MockitoBean AnnouncementAttachmentProviderQaManagementService service;
    private AttachmentProviderQaResponses.Run response(String status) {
        var now=OffsetDateTime.parse("2026-09-12T11:00:00+09:00");
        return new AttachmentProviderQaResponses.Run(RUN,POLICY,0,"a".repeat(64),"b".repeat(64),"c".repeat(64),status,1,1,3,100,0,0,1,1,1,1,false,480,true,false,now,now.plusDays(1),null);
    }
    @Test void adminReservationReturns202WithSafeMetadataAndNoPolicyPass() throws Exception {
        when(service.insertRun(any(),eq(POLICY),eq(KEY),any())).thenReturn(response("READY"));
        mvc.perform(post(ROOT).with(user("qa").roles("ADMIN")).with(csrf()).header("Idempotency-Key",KEY).contentType(MediaType.APPLICATION_JSON).content(INPUT))
                .andExpect(status().isAccepted()).andExpect(header().string("Cache-Control","no-store")).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.statusCode").value("READY")).andExpect(jsonPath("$.data.isQaPassed").value(false))
                .andExpect(jsonPath("$.data.isExpectationCoverageComplete").value(false)).andExpect(jsonPath("$.data.requestHash").doesNotExist())
                .andExpect(jsonPath("$.data.idempotencyKey").doesNotExist()).andExpect(jsonPath("$.data.leaseToken").doesNotExist());
    }
    @ParameterizedTest @ValueSource(strings={"ADMIN","OPERATOR","APPROVER"})
    void readRolesReceiveSeparatePagedPlansRunsAndCases(String role) throws Exception {
        when(service.selectExecutionPlan(any(),eq(POLICY),eq(1),eq(20))).thenReturn(new AttachmentProviderQaResponses.Preview(POLICY,0,"a".repeat(64),"b".repeat(64),"c".repeat(64),2,9,0,false,false,false,PageResponse.of(List.of(),1,20,0)));
        when(service.selectRunList(any(),eq(POLICY),eq(2),eq(10))).thenReturn(PageResponse.of(List.of(response("COMPLETED")),2,10,11));
        when(service.selectRunDetails(any(),eq(POLICY),eq(RUN))).thenReturn(response("COMPLETED"));
        when(service.selectCaseList(any(),eq(POLICY),eq(RUN),eq(1),eq(20))).thenReturn(PageResponse.of(List.of(),1,20,0));
        mvc.perform(get(ROOT+"/execution-plan").with(user("qa").roles(role))).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"))
                .andExpect(jsonPath("$.data.executableCaseCount").value(0)).andExpect(jsonPath("$.data.isReservationEnabled").value(false));
        mvc.perform(get(ROOT).with(user("qa").roles(role)).param("page","2").param("size","10"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalCount").value(11)).andExpect(jsonPath("$.data.items[0].isQaPassed").value(false));
        mvc.perform(get(ROOT+"/"+RUN).with(user("qa").roles(role))).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"));
        mvc.perform(get(ROOT+"/"+RUN+"/cases").with(user("qa").roles(role))).andExpect(status().isOk()).andExpect(jsonPath("$.data.items").isArray());
    }
    @ParameterizedTest @ValueSource(strings={"OPERATOR","APPROVER","USER","PARTNER","REVIEWER"})
    void onlyAdminCanReserveOrCancel(String role) throws Exception {
        mvc.perform(post(ROOT).with(user("qa").roles(role)).with(csrf()).header("Idempotency-Key",KEY).contentType(MediaType.APPLICATION_JSON).content(INPUT)).andExpect(status().isForbidden());
        mvc.perform(put(ROOT+"/"+RUN+"/cancellation").with(user("qa").roles(role)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"expectedVersion\":1,\"reason\":\"취소\"}"))
                .andExpect(status().isForbidden());verifyNoInteractions(service);
    }
    @ParameterizedTest @ValueSource(strings={"ADMIN","OPERATOR","APPROVER"})
    void targetCoverageIsPagedReadOnlyMetadataWithNoImplicitQaSuccess(String role) throws Exception {
        var applicability=new com.saneb.domain.announcementattachment.qa.AttachmentProviderQaCatalog.FormatApplicability("EXPECTATIONS_UNKNOWN",List.of(),List.of("HWP","HWPX","PDF"),0);
        var target=new com.saneb.domain.announcementattachment.qa.AttachmentProviderQaCatalog.TargetPlan("LOCAL_GOV_NOTICE:LGS-000138","SYSTEM_BINDING_MATCHED",3,0,0,3,List.of(),false,applicability);
        var coverage=new com.saneb.domain.announcementattachment.qa.AttachmentProviderQaCatalog.FormatCoverage("FIXED_SAMPLE_FORMATS_V2",List.of("HWP","HWPX","PDF"),List.of("HWP","HWPX","PDF"));
        when(service.selectTargetCoverageList(any(),eq(POLICY),eq(2),eq(10))).thenReturn(new AttachmentProviderQaResponses.Coverage(POLICY,0,"a".repeat(64),"b".repeat(64),"c".repeat(64),false,false,coverage,PageResponse.of(List.of(target),2,10,225)));
        mvc.perform(get(ROOT+"/execution-plan/targets").with(user("qa").roles(role)).param("page","2").param("size","10"))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store")).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isQaPassed").value(false)).andExpect(jsonPath("$.data.targets.totalCount").value(225))
                .andExpect(jsonPath("$.data.targets.items[0].formatApplicability.statusCode").value("EXPECTATIONS_UNKNOWN"))
                .andExpect(jsonPath("$.data.targets.items[0].sourceUrl").doesNotExist()).andExpect(jsonPath("$.data.formatCoverage.missingFormats.length()").value(3));
        verify(service).selectTargetCoverageList(any(),eq(POLICY),eq(2),eq(10));verifyNoMoreInteractions(service);
    }
    @Test void targetCoverageRejectsUnauthorizedOrMalformedAccessWithoutCallingService() throws Exception {
        var route=ROOT+"/execution-plan/targets";mvc.perform(get(route)).andExpect(status().isUnauthorized());
        for(String role:List.of("USER","PARTNER","REVIEWER"))mvc.perform(get(route).with(user("qa").roles(role))).andExpect(status().isForbidden());
        mvc.perform(get(route).with(user("qa").roles("ADMIN")).param("page","bad")).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
    @ParameterizedTest @ValueSource(strings={"passed","filePath","runtimeHash","targetIds","profileCode","statusCode"})
    void unknownExecutionInputOrSuccessCannotReachService(String field) throws Exception {
        String json=INPUT.strip();json=json.substring(0,json.length()-1)+",\""+field+"\":\"untrusted-provider-input\"}";
        mvc.perform(post(ROOT).with(user("qa").roles("ADMIN")).with(csrf()).header("Idempotency-Key",KEY).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.success").value(false)).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("untrusted-provider-input"))));
        verifyNoInteractions(service);
    }
    @ParameterizedTest @ValueSource(strings={"\"maximumRequests\":0","\"segmentNo\":0","\"expectedCaseCount\":10001","\"maximumSecondsIncludingMargin\":82801","\"acknowledgeScope\":false","\"acknowledgeNetworkBudget\":false"})
    void invalidBudgetOrMissingAcknowledgmentCannotReachService(String replacement) throws Exception {
        String field=replacement.substring(0,replacement.indexOf(':'));String input=INPUT.replaceAll(field+":(?:[0-9]+|true)",replacement);
        mvc.perform(post(ROOT).with(user("qa").roles("ADMIN")).with(csrf()).header("Idempotency-Key",KEY).contentType(MediaType.APPLICATION_JSON).content(input))
                .andExpect(status().isBadRequest());verifyNoInteractions(service);
    }
    @Test void cancellationRequiresCsrfAndVersionAndDoesNotReportImmediateCleanup() throws Exception {
        when(service.updateCancellation(any(),eq(POLICY),eq(RUN),any())).thenReturn(response("CANCEL_REQUESTED"));
        var route=ROOT+"/"+RUN+"/cancellation";var input="{\"expectedVersion\":1,\"reason\":\"취소\"}";
        mvc.perform(put(route).with(user("qa").roles("ADMIN")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(input))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.statusCode").value("CANCEL_REQUESTED"));
        mvc.perform(put(route).with(user("qa").roles("ADMIN")).contentType(MediaType.APPLICATION_JSON).content(input)).andExpect(status().isForbidden());
    }
    @Test void unauthenticatedMissingKeyAndMalformedIdentifiersCannotReachService() throws Exception {
        mvc.perform(get(ROOT)).andExpect(status().isUnauthorized());
        for(String role:List.of("USER","PARTNER","REVIEWER"))mvc.perform(get(ROOT).with(user("qa").roles(role))).andExpect(status().isForbidden());
        mvc.perform(post(ROOT).with(user("qa").roles("ADMIN")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(INPUT)).andExpect(status().isBadRequest());
        mvc.perform(get(ROOT).with(user("qa").roles("ADMIN")).param("page","invalid")).andExpect(status().isBadRequest());
        mvc.perform(get(ROOT+"/not-uuid").with(user("qa").roles("ADMIN"))).andExpect(status().isBadRequest());verifyNoInteractions(service);
    }
}
