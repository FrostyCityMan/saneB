package com.saneb.domain.announcementattachment.service.impl;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.common.error.ApiException;
import com.saneb.domain.announcementattachment.dao.*;
import com.saneb.domain.announcementattachment.dto.*;
import com.saneb.domain.announcementattachment.vo.*;
import com.saneb.domain.announcementsource.classification.*;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.*;
import com.saneb.domain.announcementsource.dao.AnnouncementSourceDao;
import com.saneb.domain.announcementsource.service.AnnouncementSourceRuleReleaseService;
import com.saneb.domain.announcementsource.vo.*;
import com.saneb.domain.auth.vo.*;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

class AnnouncementAttachmentPolicyCheckServiceTest {
    private final AnnouncementAttachmentPolicyDao policies=mock(AnnouncementAttachmentPolicyDao.class);
    private final AnnouncementAttachmentPolicyCheckDao checks=mock(AnnouncementAttachmentPolicyCheckDao.class);
    private final AnnouncementSourceRuleReleaseService rules=mock(AnnouncementSourceRuleReleaseService.class);
    private final AnnouncementAttachmentPolicyGoldenGate golden=spy(new AnnouncementAttachmentPolicyGoldenGate());
    private final AnnouncementSourceDao audit=mock(AnnouncementSourceDao.class);
    private final PlatformTransactionManager transactions=mock(PlatformTransactionManager.class);
    private final ObjectMapper mapper=new ObjectMapper();
    private final UUID policyId=UUID.randomUUID(),ruleId=UUID.randomUUID(),actor=UUID.randomUUID(),key=UUID.randomUUID();
    private final OffsetDateTime now=OffsetDateTime.parse("2026-09-11T12:00:00+09:00");
    private final Map<UUID,AttachmentPolicyCheckRows.Row> saved=new HashMap<>();
    private final AtomicInteger activeTransactions=new AtomicInteger();
    private AttachmentPolicyManagementRows.Row policy;
    private AnnouncementSourceRuleValidationDetails snapshot;
    private Runnable afterGolden=()->{};
    private AnnouncementAttachmentPolicyCheckServiceImpl service;
    private Authentication auth(String role) {return auth(actor,role,"ACTIVE",false);}
    private Authentication auth(UUID id,String role,String status,boolean reset) {
        var principal=new AuthenticatedUserDetails(new AuthUserDetailsRow(id,"policy-check-qa","unused","정책 검증",status,reset,null,null,null),List.of(role));
        return UsernamePasswordAuthenticationToken.authenticated(principal,null,principal.getAuthorities());
    }
    private AnnouncementSourceClassificationRule rule(String code,RuleGroupKindCode kind,String term,TargetCategoryCode target,SupportTypeCode support) {
        return new AnnouncementSourceClassificationRule(code,code,kind,term,StrengthCode.STRONG,target,support,List.of(AnnouncementSourceClassificationTerm.canonical(term,MatchModeCode.NORMALIZED_PHRASE)),true);
    }
    private AttachmentPolicyCheckRequest request() {return new AttachmentPolicyCheckRequest(0,"관리자 검증 사유 원문");}
    private AttachmentPolicyManagementRows.Row policy(int version,String state) {
        return new AttachmentPolicyManagementRows.Row(policyId,"ATT-CHECK",1,version,state,"ENFORCE",ruleId,"DRAFT",null,
                "{\"engineVersion\":\"attachment-1.0.0\",\"maximumSourceBytes\":83886080}","[]",actor,now,now,null,null,null,null,null);
    }
    @BeforeEach void setup() {
        policy=policy(0,"DRAFT");
        snapshot=new AnnouncementSourceRuleValidationDetails(ruleId,1,"DRAFT",null,"a".repeat(64),new AnnouncementSourceClassificationRuleSet("CHECK-RULES",List.of(
                rule("T",RuleGroupKindCode.TARGET,"소상공인",TargetCategoryCode.BUSINESS,null),rule("S",RuleGroupKindCode.SUPPORT_TYPE,"지원금",null,SupportTypeCode.GRANT_SUBSIDY),
                rule("A",RuleGroupKindCode.REVIEW_A,"특허",null,null),rule("B",RuleGroupKindCode.AUTO_EXCLUDE_B,"수출",null,null))));
        when(policies.selectPolicyDetails(eq(policyId),anyBoolean())).thenAnswer(call->policy);
        when(rules.selectRuleValidationDetails(ruleId)).thenAnswer(call->snapshot);
        when(checks.selectCheckDetails(any())).thenAnswer(call->current(saved.get(call.getArgument(0))));
        when(checks.insertCheck(any())).thenAnswer(call->{
            var command=(AttachmentPolicyCheckRows.Insert)call.getArgument(0);
            saved.put(command.idempotencyKey(),new AttachmentPolicyCheckRows.Row(command.checkId(),command.policyId(),command.policyVersion(),command.policySnapshotHash(),command.ruleReleaseId(),
                    command.ruleVersion(),command.ruleSnapshotHash(),command.ruleContentHash(),"CLASSIFICATION_GOLDEN",command.suiteVersion(),command.engineVersion(),command.resultHash(),command.caseCount(),
                    command.caseIdsJson(),command.requestedBy(),command.idempotencyKey(),command.requestHash(),true,now));return 1;
        });
        when(transactions.getTransaction(any())).thenAnswer(call->{activeTransactions.incrementAndGet();return new SimpleTransactionStatus();});
        doAnswer(call->{activeTransactions.decrementAndGet();return null;}).when(transactions).commit(any());
        doAnswer(call->{activeTransactions.decrementAndGet();return null;}).when(transactions).rollback(any());
        doAnswer(call->{assertThat(activeTransactions.get()).isZero();var result=call.callRealMethod();afterGolden.run();return result;}).when(golden).selectValidatedResult(any(),anyString(),any());
        service=new AnnouncementAttachmentPolicyCheckServiceImpl(policies,checks,rules,golden,audit,mapper,transactions);
    }
    private AttachmentPolicyCheckRows.Row current(AttachmentPolicyCheckRows.Row row) {
        if(row==null) return null;
        return new AttachmentPolicyCheckRows.Row(row.checkId(),row.policyId(),row.policyVersion(),row.policySnapshotHash(),row.ruleReleaseId(),row.ruleVersion(),row.ruleSnapshotHash(),row.ruleContentHash(),
                row.checkTypeCode(),row.suiteVersion(),row.engineVersion(),row.resultHash(),row.caseCount(),row.caseIdsJson(),row.requestedBy(),row.idempotencyKey(),row.requestHash(),
                row.policyVersion().equals(policy.rowVersion()) && row.ruleVersion().equals(snapshot.rowVersion()) && "DRAFT".equals(policy.policyStatusCode()),row.createdAt());
    }
    @Test void classificationRunsOutsideTransactionThenRechecksAndStoresNonSensitiveEvidence() {
        var result=service.insertClassificationCheck(auth("ADMIN"),policyId,key,request());
        assertThat(result.checkTypeCode()).isEqualTo("CLASSIFICATION_GOLDEN");assertThat(result.caseCount()).isEqualTo(30);assertThat(result.isCurrent()).isTrue();
        assertThat(result.policySnapshotHash()).matches("[0-9a-f]{64}");assertThat(result.ruleSnapshotHash()).isEqualTo(snapshot.calculatedSnapshotHash());
        assertThat(activeTransactions.get()).isZero();verify(transactions,times(2)).commit(any());
        var order=inOrder(policies,checks);order.verify(checks).selectRequestLock(key);order.verify(policies).selectRuleStatus(ruleId);order.verify(policies).selectPolicyDetails(policyId,true);order.verify(checks).insertCheck(any());
        var log=org.mockito.ArgumentCaptor.forClass(AnnouncementSourceAuditLogCommand.class);verify(audit).insertAuditLog(log.capture());
        assertThat(log.getValue().metadataJson()).contains("checkId","caseCount","reasonHash").doesNotContain("관리자 검증 사유 원문","소상공인","settingsJson");
        verify(policies,never()).updatePolicyDraft(any());verify(policies,never()).insertPolicy(any());
    }
    @Test void segmentPolicyPersistsFiftyTwoCasesWithoutPublishingOrChangingTheDraft() throws Exception {
        var configuration=(com.fasterxml.jackson.databind.node.ObjectNode)mapper.readTree(policy.settingsJson());
        configuration.put("engineVersion",com.saneb.domain.announcementattachment.classification.AttachmentSegmentClassificationEngine.VERSION);
        configuration.put("segmentRuleVersion",com.saneb.domain.announcementattachment.classification.AttachmentSegmentRoleAnalyzer.VERSION);
        configuration.put("segmentRulesHash",com.saneb.domain.announcementattachment.classification.AttachmentSegmentRoleAnalyzer.RULES_HASH);
        policy=new AttachmentPolicyManagementRows.Row(policyId,"ATT-CHECK",1,0,"DRAFT","ENFORCE",ruleId,"DRAFT",null,
                configuration.toString(),"[]",actor,now,now,null,null,null,null,null);
        var result=service.insertClassificationCheck(auth("ADMIN"),policyId,key,request());
        assertThat(result.caseCount()).isEqualTo(52);assertThat(result.caseIds()).contains("AG-030","SG-022");
        assertThat(result.engineVersion()).isEqualTo(com.saneb.domain.announcementattachment.classification.AttachmentSegmentClassificationEngine.VERSION);
        assertThat(service.insertClassificationCheck(auth("ADMIN"),policyId,key,request()).checkId()).isEqualTo(result.checkId());
        verify(policies,never()).updatePolicyDraft(any());verify(policies,never()).insertPolicy(any());
    }
    @Test void repeatedRequestReturnsSameCheckWithFreshStalenessWithoutRerunningGolden() {
        var first=service.insertClassificationCheck(auth("ADMIN"),policyId,key,request());policy=policy(1,"DRAFT");
        var repeated=service.insertClassificationCheck(auth("ADMIN"),policyId,key,request());
        assertThat(repeated.checkId()).isEqualTo(first.checkId());assertThat(repeated.isCurrent()).isFalse();
        verify(golden,times(1)).selectValidatedResult(any(),anyString(),any());verify(checks,times(1)).insertCheck(any());verify(audit,times(1)).insertAuditLog(any());
    }
    @Test void reusedKeyCannotCrossActorPolicyOrRequest() {
        service.insertClassificationCheck(auth("ADMIN"),policyId,key,request());
        assertThatThrownBy(()->service.insertClassificationCheck(auth(UUID.randomUUID(),"ADMIN","ACTIVE",false),policyId,key,request())).isInstanceOf(ApiException.class).hasMessageContaining("멱등 키");
        assertThatThrownBy(()->service.insertClassificationCheck(auth("ADMIN"),UUID.randomUUID(),key,request())).isInstanceOf(ApiException.class).hasMessageContaining("멱등 키");
        assertThatThrownBy(()->service.insertClassificationCheck(auth("ADMIN"),policyId,key,new AttachmentPolicyCheckRequest(0,"다른 사유"))).isInstanceOf(ApiException.class).hasMessageContaining("멱등 키");
        verify(checks,times(1)).insertCheck(any());
    }
    @ParameterizedTest @ValueSource(strings={"POLICY","RULE"})
    void concurrentInputChangePreventsSavingEarlierSuccess(String kind) {
        afterGolden=()->{if(kind.equals("POLICY"))policy=policy(1,"DRAFT");else snapshot=new AnnouncementSourceRuleValidationDetails(ruleId,2,"DRAFT",null,"a".repeat(64),snapshot.ruleSet());};
        assertThatThrownBy(()->service.insertClassificationCheck(auth("ADMIN"),policyId,key,request())).isInstanceOf(ApiException.class).hasMessageContaining("입력을 보존");
        verify(checks,never()).insertCheck(any());verifyNoInteractions(audit);verify(transactions).rollback(any());assertThat(activeTransactions.get()).isZero();
    }
    @Test void changedPublishedRuleHashAndRetiredRuleAreRejectedBeforeGolden() {
        snapshot=new AnnouncementSourceRuleValidationDetails(ruleId,1,"ACTIVE","b".repeat(64),"a".repeat(64),snapshot.ruleSet());
        assertThatThrownBy(()->service.insertClassificationCheck(auth("ADMIN"),policyId,key,request())).isInstanceOf(ApiException.class).hasMessageContaining("무결성");
        snapshot=new AnnouncementSourceRuleValidationDetails(ruleId,1,"RETIRED","a".repeat(64),"a".repeat(64),snapshot.ruleSet());
        assertThatThrownBy(()->service.insertClassificationCheck(auth("ADMIN"),policyId,key,request())).isInstanceOf(ApiException.class).hasMessageContaining("DRAFT 또는 ACTIVE");
        verify(golden,never()).selectValidatedResult(any(),anyString(),any());verify(checks,never()).insertCheck(any());
    }
    @Test void goldenFailureCannotBeStoredAsSuccessfulValidation() {
        snapshot=new AnnouncementSourceRuleValidationDetails(ruleId,1,"DRAFT",null,"a".repeat(64),new AnnouncementSourceClassificationRuleSet("BROKEN",snapshot.ruleSet().rules().stream().filter(row->!row.ruleCode().equals("B")).toList()));
        assertThatThrownBy(()->service.insertClassificationCheck(auth("ADMIN"),policyId,key,request())).isInstanceOf(ApiException.class).hasMessageContaining("분류 검증");
        verify(checks,never()).insertCheck(any());verifyNoInteractions(audit);assertThat(activeTransactions.get()).isZero();
    }
    @ParameterizedTest @ValueSource(strings={"ACTIVE","RETIRED"})
    void onlyDraftPolicyCanStartNewCheck(String state) {
        policy=policy(0,state);assertThatThrownBy(()->service.insertClassificationCheck(auth("ADMIN"),policyId,key,request())).isInstanceOf(ApiException.class).hasMessageContaining("개정 초안");
        verifyNoInteractions(rules);verify(golden,never()).selectValidatedResult(any(),anyString(),any());
    }
    @ParameterizedTest @ValueSource(strings={"OPERATOR","APPROVER","USER","PARTNER","REVIEWER"})
    void onlyAdminMayRunCheckEvenWithDirectServiceInvocation(String role) {
        assertThatThrownBy(()->service.insertClassificationCheck(auth(role),policyId,key,request())).isInstanceOf(ApiException.class).hasMessageContaining("ADMIN");verifyNoInteractions(policies,checks,rules,transactions);
    }
    @ParameterizedTest @ValueSource(strings={"ADMIN","OPERATOR","APPROVER"})
    void readRolesGetPagedHistoryWithoutMutation(String role) {
        service.insertClassificationCheck(auth("ADMIN"),policyId,key,request());clearInvocations(policies,checks,golden,audit,rules);
        when(checks.selectCheckCount(any())).thenReturn(1L);when(checks.selectCheckList(any())).thenReturn(List.of(saved.get(key)));
        var response=service.selectCheckList(auth(role),policyId,1,20);assertThat(response.totalCount()).isEqualTo(1);
        assertThat(response.items().getFirst().checkTypeCode()).isEqualTo("CLASSIFICATION_GOLDEN");
        verify(policies,never()).selectPolicyDetails(any(),eq(true));verifyNoInteractions(golden,audit,rules);
    }
    @Test void absentActorInactiveResetAndInvalidInputAreRejected() {
        for(var authentication:Arrays.asList(null,auth(actor,"ADMIN","SUSPENDED",false),auth(actor,"ADMIN","ACTIVE",true)))
            assertThatThrownBy(()->service.insertClassificationCheck(authentication,policyId,key,request())).isInstanceOf(ApiException.class);
        for(var input:Arrays.asList(null,new AttachmentPolicyCheckRequest(null,"QA"),new AttachmentPolicyCheckRequest(-1,"QA"),new AttachmentPolicyCheckRequest(0," ")))
            assertThatThrownBy(()->service.insertClassificationCheck(auth("ADMIN"),policyId,key,input)).isInstanceOf(ApiException.class);
        verifyNoInteractions(policies,checks,rules,transactions);
    }
}
