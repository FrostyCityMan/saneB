package com.saneb.domain.announcementattachment.service.impl;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.common.error.ApiException;
import com.saneb.domain.announcementattachment.dao.*;
import com.saneb.domain.announcementattachment.dto.AttachmentPolicyPublicationImpact.*;
import com.saneb.domain.announcementattachment.vo.*;
import com.saneb.domain.auth.vo.*;
import java.time.OffsetDateTime;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.*;

class AnnouncementAttachmentPolicyPublicationImpactServiceTest {
    private final AnnouncementAttachmentPolicyDao policies=mock(AnnouncementAttachmentPolicyDao.class);
    private final AnnouncementAttachmentPolicyValidationDao qa=mock(AnnouncementAttachmentPolicyValidationDao.class);
    private final AnnouncementAttachmentPolicyPublicationImpactDao impact=mock(AnnouncementAttachmentPolicyPublicationImpactDao.class);
    private final AnnouncementAttachmentPolicyPublicationImpactServiceImpl service=new AnnouncementAttachmentPolicyPublicationImpactServiceImpl(policies,qa,impact,new ObjectMapper().findAndRegisterModules());
    private final UUID policyId=UUID.randomUUID(),activeId=UUID.randomUUID(),ruleId=UUID.randomUUID(),runId=UUID.randomUUID();
    private final OffsetDateTime at=OffsetDateTime.parse("2026-09-12T00:00:00Z");
    private AttachmentPolicyManagementRows.Row row(UUID id,String status,String mode){return new AttachmentPolicyManagementRows.Row(id,"ATT-QA",1,3,status,mode,ruleId,"ACTIVE","DRAFT".equals(status)?null:"a".repeat(64),
            "{\"maximumSourceBytes\":1024}","[]",UUID.randomUUID(),at,at,"DRAFT".equals(status)?null:at,null,null,null,null);}
    private Counts counts(long n){return new Counts(n,n,n,n,n,n,n,n,n);}
    private Authentication actor(String role,String status,boolean reset){var user=new AuthenticatedUserDetails(new AuthUserDetailsRow(UUID.randomUUID(),"fixture","unused","QA",status,reset,null,null,null),List.of(role));return UsernamePasswordAuthenticationToken.authenticated(user,null,user.getAuthorities());}
    private Authentication actor(){return actor("ADMIN","ACTIVE",false);}
    @BeforeEach void setup(){when(policies.selectPolicyDetails(policyId,false)).thenReturn(row(policyId,"DRAFT","OFF"));when(policies.selectPolicyCount(any())).thenReturn(1L);
        when(policies.selectPolicyList(any())).thenReturn(List.of(row(activeId,"ACTIVE","COLLECT_ONLY").selectSummary()));when(impact.selectCounts(ruleId)).thenReturn(counts(2));when(impact.selectCounts(null)).thenReturn(counts(3));
        when(qa.selectRunCount(any())).thenReturn(0L);when(qa.selectRunList(any())).thenReturn(List.of());}
    @ParameterizedTest @ValueSource(strings={"ADMIN","OPERATOR","APPROVER"})
    void readRolesSeeRuleAndGlobalScopeWithoutClaimingPublicationOrMakingWrites(String role){var result=service.selectImpactDetails(actor(role,"ACTIVE",false),policyId);
        assertThat(result.matchingRule().boundSourceCount()).isEqualTo(2);assertThat(result.allRules().boundSourceCount()).isEqualTo(3);
        assertThat(result.wouldStopNewExternalRequests()).isTrue();assertThat(result.wouldLiftGlobalOffStop()).isFalse();assertThat(result.requiresPublicationRevalidation()).isTrue();
        assertThat(result.blockingReasonCodes()).contains("QA_NOT_REQUESTED","PUBLICATION_REVALIDATION_REQUIRED");assertThat(result.latestQa()).isNull();assertThat(result.currentHttpRequests()).isZero();
        verify(policies).selectPolicyDetails(policyId,false);verify(policies).selectPolicyCount(any());verify(policies).selectPolicyList(any());verifyNoMoreInteractions(policies);}
    @Test void removingCurrentOffIsReportedWithoutActuallyLiftingStop(){when(policies.selectPolicyDetails(policyId,false)).thenReturn(row(policyId,"DRAFT","ENFORCE"));when(policies.selectPolicyList(any())).thenReturn(List.of(row(activeId,"ACTIVE","OFF").selectSummary()));
        var result=service.selectImpactDetails(actor(),policyId);assertThat(result.wouldLiftGlobalOffStop()).isTrue();assertThat(result.wouldStopNewExternalRequests()).isFalse();}
    @Test void noActivePolicyReturnsNullNotInventedOff(){when(policies.selectPolicyCount(any())).thenReturn(0L);when(policies.selectPolicyList(any())).thenReturn(List.of());assertThat(service.selectImpactDetails(actor(),policyId).activePolicyForRule()).isNull();}
    @Test void hashIsStableAcrossObservationTimesAndChangesWithScopeOrActivePolicy(){var first=service.selectImpactDetails(actor(),policyId);assertThat(service.selectImpactDetails(actor(),policyId).observedImpactHash()).isEqualTo(first.observedImpactHash());
        when(impact.selectCounts(null)).thenReturn(counts(4));assertThat(service.selectImpactDetails(actor(),policyId).observedImpactHash()).isNotEqualTo(first.observedImpactHash());
        when(impact.selectCounts(null)).thenReturn(counts(3));when(policies.selectPolicyList(any())).thenReturn(List.of(row(activeId,"ACTIVE","OFF").selectSummary()));assertThat(service.selectImpactDetails(actor(),policyId).observedImpactHash()).isNotEqualTo(first.observedImpactHash());}
    private void qa(String status,boolean current,List<AttachmentPolicyValidationRows.Step> steps){var run=new AttachmentPolicyValidationRows.Run(runId,policyId,3,ruleId,4,"b".repeat(64),null,status,2,null,null,null,null,null,null,at,at,at,current);
        when(qa.selectRunCount(any())).thenReturn(1L);when(qa.selectRunList(any())).thenReturn(List.of(run));when(qa.selectStepList(runId)).thenReturn(steps);}
    private AttachmentPolicyValidationRows.Step step(String code,String state){return new AttachmentPolicyValidationRows.Step(runId,code,state,"{\"private\":\"must not expose\"}","c".repeat(64),at);}
    @Test void incompleteOrStaleQaCannotBecomeApproval(){qa("INCOMPLETE",false,List.of(step("CLASSIFICATION_GOLDEN","PASSED"),step("INSTALLED_RUNTIME","PASSED"),step("PROVIDER_PROFILES","MISSING")));
        var result=service.selectImpactDetails(actor(),policyId);assertThat(result.blockingReasonCodes()).contains("QA_NOT_VERIFIED","QA_INPUT_VERSIONS_CHANGED","QA_REQUIRED_STEPS_NOT_PASSED","PUBLICATION_REVALIDATION_REQUIRED");
        assertThat(result.latestQa().steps().getLast().statusCode()).isEqualTo("NOT_RUN");assertThat(result.latestQa().toString()).doesNotContain("private","must not expose");}
    @Test void evenFourPassedStepsDoNotReplaceRuntimeRevalidationOrPublicationConsent(){qa("VERIFIED",true,List.of(step("CLASSIFICATION_GOLDEN","PASSED"),step("INSTALLED_RUNTIME","PASSED"),step("PROVIDER_PROFILES","PASSED"),step("WORKER_DB_RECOVERY","PASSED")));
        var result=service.selectImpactDetails(actor(),policyId);assertThat(result.blockingReasonCodes()).containsExactly("PUBLICATION_REVALIDATION_REQUIRED");assertThat(result.requiresPublicationRevalidation()).isTrue();}
    @Test void duplicateUnknownAndCrossRunStepsAreRejected(){var s=step("CLASSIFICATION_GOLDEN","PASSED");for(var steps:List.of(List.of(s,s),List.of(step("UNKNOWN","PASSED")),List.of(new AttachmentPolicyValidationRows.Step(UUID.randomUUID(),s.stepCode(),s.statusCode(),s.evidenceJson(),s.evidenceHash(),at)))){
        qa("INCOMPLETE",true,steps);assertThatThrownBy(()->service.selectImpactDetails(actor(),policyId)).isInstanceOf(ApiException.class);}}
    @Test void inconsistentCountsAndMultipleActivePoliciesAreRejected(){for(Counts value:List.of(counts(-1),counts(9007199254740992L),new Counts(1L,2L,0L,0L,0L,0L,0L,0L,0L),counts(4))){when(impact.selectCounts(ruleId)).thenReturn(value);assertThatThrownBy(()->service.selectImpactDetails(actor(),policyId)).isInstanceOf(ApiException.class);}
        when(impact.selectCounts(ruleId)).thenReturn(counts(2));when(policies.selectPolicyCount(any())).thenReturn(2L);assertThatThrownBy(()->service.selectImpactDetails(actor(),policyId)).isInstanceOf(ApiException.class);}
    @Test void missingMalformedOrNonDraftPolicyIsNotSilentlyAccepted(){assertThatThrownBy(()->service.selectImpactDetails(actor(),null)).isInstanceOf(ApiException.class);assertThatThrownBy(()->service.selectImpactDetails(actor(),UUID.randomUUID())).isInstanceOf(ApiException.class);
        when(policies.selectPolicyDetails(policyId,false)).thenReturn(row(policyId,"ACTIVE","OFF"));assertThat(service.selectImpactDetails(actor(),policyId).blockingReasonCodes()).contains("POLICY_NOT_DRAFT");}
    @ParameterizedTest @ValueSource(strings={"USER","PARTNER","REVIEWER"})
    void externalRolesCannotRead(String role){assertThatThrownBy(()->service.selectImpactDetails(actor(role,"ACTIVE",false),policyId)).isInstanceOf(ApiException.class);verifyNoInteractions(policies,qa,impact);}
    @Test void inactiveResetAndUnauthenticatedCannotRead(){for(var invalid:List.of(actor("ADMIN","INACTIVE",false),actor("ADMIN","ACTIVE",true),UsernamePasswordAuthenticationToken.unauthenticated("fixture",null)))assertThatThrownBy(()->service.selectImpactDetails(invalid,policyId)).isInstanceOf(ApiException.class);
        assertThatThrownBy(()->service.selectImpactDetails(null,policyId)).isInstanceOf(ApiException.class);verifyNoInteractions(policies,qa,impact);}
    @Test void oneReadOnlySnapshotHasBoundedTransaction() throws Exception {var a=service.getClass().getMethod("selectImpactDetails",Authentication.class,UUID.class).getAnnotation(Transactional.class);assertThat(a.readOnly()).isTrue();assertThat(a.isolation()).isEqualTo(Isolation.REPEATABLE_READ);assertThat(a.timeout()).isEqualTo(15);}
}
