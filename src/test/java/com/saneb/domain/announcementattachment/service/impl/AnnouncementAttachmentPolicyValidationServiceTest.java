package com.saneb.domain.announcementattachment.service.impl;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.common.error.*;
import com.saneb.domain.announcementattachment.dao.*;
import com.saneb.domain.announcementattachment.dto.*;
import com.saneb.domain.announcementattachment.extraction.AttachmentRuntimeGate;
import com.saneb.domain.announcementattachment.vo.*;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationRuleSet;
import com.saneb.domain.announcementsource.dao.AnnouncementSourceDao;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceRuleValidationDetails;
import com.saneb.domain.auth.vo.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

class AnnouncementAttachmentPolicyValidationServiceTest {
    private final AnnouncementAttachmentPolicyDao policies=mock(AnnouncementAttachmentPolicyDao.class);
    private final AnnouncementAttachmentPolicyValidationDao dao=mock(AnnouncementAttachmentPolicyValidationDao.class);
    private final AttachmentPolicyValidationSnapshotFactory snapshots=mock(AttachmentPolicyValidationSnapshotFactory.class);
    private final AnnouncementAttachmentPolicyGoldenGate golden=mock(AnnouncementAttachmentPolicyGoldenGate.class);
    private final AttachmentRuntimeGate runtime=mock(AttachmentRuntimeGate.class);
    private final AttachmentWorkerDbQaGate workerDb=mock(AttachmentWorkerDbQaGate.class);
    private final AttachmentProviderQaEvidenceGate providerQa=mock(AttachmentProviderQaEvidenceGate.class);
    private final AnnouncementSourceDao audit=mock(AnnouncementSourceDao.class);
    private final PlatformTransactionManager transactions=mock(PlatformTransactionManager.class);
    private final ObjectMapper mapper=new ObjectMapper().findAndRegisterModules();
    private final UUID policyId=UUID.randomUUID(),ruleId=UUID.randomUUID(),actorId=UUID.randomUUID(),key=UUID.randomUUID();
    private final OffsetDateTime now=OffsetDateTime.parse("2026-09-11T14:00:00+09:00");
    private final AtomicInteger activeTransactions=new AtomicInteger();
    private final Map<UUID,AttachmentPolicyValidationRows.Run> runs=new LinkedHashMap<>();
    private final List<AttachmentPolicyValidationRows.Step> steps=new ArrayList<>();
    private final AttachmentPolicyValidationSnapshotFactory.Runtime installed=new AttachmentPolicyValidationSnapshotFactory.Runtime("a".repeat(64),"b".repeat(64),"e".repeat(64));
    private AttachmentPolicyValidationSnapshotFactory.Frozen frozen;
    private AnnouncementAttachmentPolicyValidationServiceImpl service;
    private boolean lease=true;
    private Runnable afterRuntime=()->{};
    @BeforeEach void setup() throws Exception {
        when(snapshots.json(any())).thenAnswer(c->mapper.writeValueAsString(c.getArgument(0)));
        when(snapshots.hash(any())).thenAnswer(c->HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(mapper.writeValueAsBytes(c.getArgument(0)))));
        var policy=new AttachmentPolicyManagementRows.Row(policyId,"ATT-QA",1,0,"DRAFT","ENFORCE",ruleId,"DRAFT",null,"{}","[]",actorId,now,now,null,null,null,null,null);
        when(policies.selectPolicyDetails(eq(policyId),anyBoolean())).thenReturn(policy);
        var rule=new AnnouncementSourceRuleValidationDetails(ruleId,0,"DRAFT",null,"d".repeat(64),new AnnouncementSourceClassificationRuleSet("QA",List.of()));
        var plan=AttachmentProviderQaPlan.selectPlan(List.of(),List.of());
        frozen=new AttachmentPolicyValidationSnapshotFactory.Frozen("c".repeat(64),mapper.writeValueAsString(Map.of("providerQaPlan",plan,"settings",Map.of("engineVersion","attachment-1.0.0"))),rule,installed);
        when(snapshots.selectProviderQaPlan()).thenReturn(plan);
        when(snapshots.selectRuntime()).thenReturn(installed);when(snapshots.selectSnapshot(any(),any())).thenAnswer(c->frozen);
        when(transactions.getTransaction(any())).thenAnswer(c->{activeTransactions.incrementAndGet();return new SimpleTransactionStatus();});
        doAnswer(c->{activeTransactions.decrementAndGet();return null;}).when(transactions).commit(any());
        doAnswer(c->{activeTransactions.decrementAndGet();return null;}).when(transactions).rollback(any());
        when(dao.insertRun(any())).thenAnswer(c->{var v=(AttachmentPolicyValidationRows.Insert)c.getArgument(0);
            runs.put(v.runId(),new AttachmentPolicyValidationRows.Run(v.runId(),v.policyId(),v.policyVersion(),v.ruleReleaseId(),v.ruleVersion(),v.snapshotHash(),v.inputSnapshotJson(),
                    "PENDING",0,v.requestedBy(),v.idempotencyKey(),v.requestHash(),null,null,null,now,null,null,true));return 1;});
        when(dao.selectRunDetails(any(),anyBoolean())).thenAnswer(c->runs.get(c.getArgument(0)));
        when(dao.selectRequestDetails(any())).thenAnswer(c->runs.values().stream().filter(v->v.idempotencyKey().equals(c.getArgument(0))).findFirst().orElse(null));
        when(dao.selectStepList(any())).thenAnswer(c->steps.stream().filter(s->s.runId().equals(c.getArgument(0))).toList());
        when(dao.selectPendingDetails()).thenAnswer(c->runs.values().stream().filter(v->"PENDING".equals(v.statusCode())).findFirst().orElse(null));
        when(dao.updateClaim(any(),any())).thenAnswer(c->{replace(c.getArgument(0),"RUNNING",c.getArgument(1),null);return 1;});
        when(dao.insertExtractionLease(any(),any())).thenAnswer(c->lease?1:0);
        when(dao.selectExecutionAllowed(any(),any())).thenAnswer(c->lease && "RUNNING".equals(runs.get(c.getArgument(0)).statusCode()));
        when(dao.insertStep(any(),any(),anyString(),anyString(),anyString(),anyString())).thenAnswer(c->{
            steps.add(new AttachmentPolicyValidationRows.Step(c.getArgument(0),c.getArgument(2),c.getArgument(3),c.getArgument(4),c.getArgument(5),now));return 1;});
        when(dao.updateFinished(any(),any(),anyString(),nullable(String.class))).thenAnswer(c->{
            UUID id=c.getArgument(0);String state="CANCEL_REQUESTED".equals(runs.get(id).statusCode())?"CANCELLED":c.getArgument(2);
            replace(id,state,null,c.getArgument(3));return 1;});
        when(dao.updateCancellation(any(),anyInt())).thenAnswer(c->{var row=runs.get(c.getArgument(0));replace(row.runId(),"PENDING".equals(row.statusCode())?"CANCELLED":"CANCEL_REQUESTED",row.leaseToken(),"CANCELLED_BY_ADMIN");return 1;});
        when(golden.selectValidatedResult(any(),anyString(),any())).thenAnswer(c->{assertThat(activeTransactions.get()).isZero();return goldenResult();});
        when(runtime.selectValidatedResult(any(BooleanSupplier.class))).thenAnswer(c->{assertThat(activeTransactions.get()).isZero();afterRuntime.run();return runtimeResult();});
        when(workerDb.selectValidatedResult(any(),any(),any())).thenAnswer(c->{
            assertThat(activeTransactions.get()).isZero();assertThat(((BooleanSupplier)c.getArgument(2)).getAsBoolean()).isTrue();
            return mapper.readTree("{\"realGateContractTestDouble\":true}");});
        when(providerQa.selectAssessment(any(),any(),any())).thenAnswer(c->{assertThat(activeTransactions.get()).isZero();
            return new AttachmentProviderQaEvidenceGate.Assessment("MISSING","ALL_PROVIDER_EXPECTATIONS_REQUIRED",null);});
        service=instance(true);
    }
    private AnnouncementAttachmentPolicyValidationServiceImpl instance(boolean enabled) {
        return new AnnouncementAttachmentPolicyValidationServiceImpl(policies,dao,snapshots,golden,runtime,workerDb,providerQa,audit,mapper,transactions,enabled);
    }
    private void replace(UUID id,String state,UUID token,String error) {
        var v=runs.get(id);boolean live=Set.of("RUNNING","CANCEL_REQUESTED").contains(state);
        runs.put(id,new AttachmentPolicyValidationRows.Run(id,v.policyId(),v.policyVersion(),v.ruleReleaseId(),v.ruleVersion(),v.snapshotHash(),v.inputSnapshotJson(),
                state,v.rowVersion()+1,v.requestedBy(),v.idempotencyKey(),v.requestHash(),live?token:null,live?now.plusMinutes(8):null,error,now,now,live?null:now,true));
    }
    private AnnouncementAttachmentPolicyGoldenGate.Result goldenResult() {return new AnnouncementAttachmentPolicyGoldenGate.Result(AnnouncementAttachmentPolicyGoldenGate.SUITE_VERSION,"attachment-1.0.0","QA","d".repeat(64),"e".repeat(64),"f".repeat(64),30,
            java.util.stream.IntStream.rangeClosed(1,30).mapToObj(n->String.format("AG-%03d",n)).toList());}
    private AttachmentRuntimeGate.Result runtimeResult() {return new AttachmentRuntimeGate.Result(UUID.randomUUID(),AttachmentRuntimeGate.SCOPE,AttachmentRuntimeGate.SUITE_VERSION,"b".repeat(64),"a".repeat(64),"1.0.0","f".repeat(64),AttachmentRuntimeGate.CASE_COUNT,
            java.util.stream.IntStream.rangeClosed(1,AttachmentRuntimeGate.CASE_COUNT).mapToObj(n->new AttachmentRuntimeGate.CaseResult(String.format("AR-%03d",n),"a".repeat(64),"COMPLETE_TEXT","PDF","b".repeat(64),10,1,true)).toList(),Instant.now(),Instant.now());}
    private Authentication auth(String role) {
        var actor=new AuthenticatedUserDetails(new AuthUserDetailsRow(actorId,"qa","unused","QA","ACTIVE",false,null,null,null),List.of(role));
        return UsernamePasswordAuthenticationToken.authenticated(actor,null,actor.getAuthorities());
    }
    private AttachmentPolicyCheckRequest request() {return new AttachmentPolicyCheckRequest(0,"운영 검증 사유 원문");}
    private UUID reserve() {return service.insertRun(auth("ADMIN"),policyId,key,request()).runId();}
    private void selectSegmentSnapshot() throws Exception {
        var tree=(com.fasterxml.jackson.databind.node.ObjectNode)mapper.readTree(frozen.json());
        var settings=tree.putObject("settings");
        settings.put("engineVersion",com.saneb.domain.announcementattachment.classification.AttachmentSegmentClassificationEngine.VERSION);
        settings.put("segmentRuleVersion",com.saneb.domain.announcementattachment.classification.AttachmentSegmentRoleAnalyzer.VERSION);
        settings.put("segmentRulesHash",com.saneb.domain.announcementattachment.classification.AttachmentSegmentRoleAnalyzer.RULES_HASH);
        frozen=new AttachmentPolicyValidationSnapshotFactory.Frozen("c".repeat(64),tree.toString(),frozen.rule(),installed);
    }
    @Test void segmentCoordinatorRequiresAllFiftyTwoCasesAndStillNeedsProviderEvidence() throws Exception {
        selectSegmentSnapshot();var config=frozen.selectConfiguration();
        var result=new AnnouncementAttachmentPolicyGoldenGate.Result(AttachmentSegmentPolicyGoldenGate.SUITE_VERSION,config.engineVersion(),"QA","d".repeat(64),"e".repeat(64),"f".repeat(64),52,
                AnnouncementAttachmentPolicyGoldenGate.selectCaseIds(config.engineVersion()));
        when(golden.selectValidatedResult(any(),anyString(),any())).thenReturn(result);
        reserve();assertThat(service.saveNextValidationRun()).isEqualTo("INCOMPLETE");
        assertThat(steps.getFirst().evidenceJson()).contains("SG-022",config.engineVersion());
        verify(golden).selectValidatedResult(frozen.rule().ruleSet(),frozen.rule().calculatedSnapshotHash(),config);
    }
    @Test void segmentCoordinatorRejectsLegacyGoldenBeforeRuntimeOrProviderExecution() throws Exception {
        selectSegmentSnapshot();reserve();
        assertThat(service.saveNextValidationRun()).isEqualTo("CONFLICT");
        verifyNoInteractions(runtime,providerQa,workerDb);
    }
    @Test void reservationIsPendingAndDoesNotExecuteOrPublishAnything() {
        UUID id=reserve();var value=service.selectRunDetails(auth("APPROVER"),policyId,id);
        assertThat(value.statusCode()).isEqualTo("PENDING");assertThat(value.steps()).hasSize(4).allSatisfy(s->assertThat(s.statusCode()).isEqualTo("NOT_RUN"));
        verifyNoInteractions(golden,runtime);verify(policies,never()).updatePolicyDraft(any());
        var log=org.mockito.ArgumentCaptor.forClass(com.saneb.domain.announcementsource.vo.AnnouncementSourceAuditLogCommand.class);
        verify(audit).insertAuditLog(log.capture());assertThat(log.getValue().metadataJson()).doesNotContain(request().reason()).contains("reasonHash");
    }
    @Test void sameKeyIsIdempotentEvenWhenWorkerBecomesDisabledAndDifferentInputConflicts() {
        UUID first=reserve();assertThat(instance(false).insertRun(auth("ADMIN"),policyId,key,request()).runId()).isEqualTo(first);
        assertThatThrownBy(()->service.insertRun(auth("ADMIN"),policyId,key,new AttachmentPolicyCheckRequest(0,"다른 사유"))).isInstanceOf(ApiException.class);
        verify(dao,times(1)).insertRun(any());
    }
    @ParameterizedTest @ValueSource(strings={"OPERATOR","APPROVER","USER","PARTNER","REVIEWER"})
    void directServiceChangesRequireAdmin(String role) {
        assertThatThrownBy(()->service.insertRun(auth(role),policyId,key,request())).isInstanceOf(ApiException.class);
        verifyNoInteractions(snapshots,dao);
    }
    @Test void disabledWorkerAndActiveQueueOrQuotaCannotReserve() {
        assertThatThrownBy(()->instance(false).insertRun(auth("ADMIN"),policyId,key,request())).hasMessageContaining("비활성");
        when(dao.selectActiveCount()).thenReturn(1);assertThatThrownBy(this::reserve).hasMessageContaining("다른 정책 QA");
        when(dao.selectActiveCount()).thenReturn(0);when(dao.selectCoolingDown(policyId)).thenReturn(true);assertThatThrownBy(this::reserve).hasMessageContaining("60초");
        when(dao.selectCoolingDown(policyId)).thenReturn(false);when(dao.selectRecentCount(policyId)).thenReturn(3);assertThatThrownBy(this::reserve).hasMessageContaining("3회");
        verify(dao,never()).insertRun(any());
    }
    @Test void actualWorkerDbGateIsCalledOutsideTransactionButMissingProviderStillPreventsVerified() {
        UUID id=reserve();assertThat(service.saveNextValidationRun()).isEqualTo("INCOMPLETE");
        assertThat(steps).extracting(AttachmentPolicyValidationRows.Step::statusCode).containsExactly("PASSED","PASSED","MISSING","PASSED");
        assertThat(service.selectRunDetails(auth("ADMIN"),policyId,id).statusCode()).isEqualTo("INCOMPLETE");
        assertThat(activeTransactions.get()).isZero();verify(dao).deleteExtractionLease(eq(id),any());verify(policies,never()).updatePolicyDraft(any());
    }
    private void providerPassed() {
        doAnswer(c->{assertThat(activeTransactions.get()).isZero();
            var run=c.getArgument(1,AttachmentPolicyValidationRows.Run.class);
            return new AttachmentProviderQaEvidenceGate.Assessment("PASSED","ALL_FROZEN_SEGMENTS_VERIFIED",
                    new AttachmentProviderQaEvidenceGate.Evidence(1,"ALL_REQUIRED_PROVIDER_QA",run.runId(),frozen.hash(),"a".repeat(64),"b".repeat(64),
                            installed.executionCodeHash(),installed.runtimeHash(),2,6,18,List.of(
                            new AttachmentProviderQaEvidenceGate.SegmentEvidence(1,UUID.randomUUID(),4,6,18,24,2400,"c".repeat(64),now.minusSeconds(1).toInstant().toString()))));
        }).when(providerQa).selectAssessment(any(),any(),any());
    }
    @Test void allFourSavedStepsProduceVerifiedWithoutPublishingOrChangingPolicy() {
        providerPassed();UUID id=reserve();assertThat(service.saveNextValidationRun()).isEqualTo("VERIFIED");
        assertThat(steps).hasSize(4).allSatisfy(s->assertThat(s.statusCode()).isEqualTo("PASSED"));
        assertThat(runs.get(id).errorCode()).isNull();verify(policies,never()).updatePolicyDraft(any());
        verify(dao).deleteExtractionLease(eq(id),any());assertThat(activeTransactions.get()).isZero();
    }
    @ParameterizedTest @ValueSource(strings={"FAILED","CANCELLED"})
    void providerFailureOrCancellationDoesNotRunDatabaseQaOrBecomeVerified(String status) {
        doReturn(new AttachmentProviderQaEvidenceGate.Assessment(status,"CASE_EVIDENCE_INVALID",null)).when(providerQa).selectAssessment(any(),any(),any());
        reserve();assertThat(service.saveNextValidationRun()).isEqualTo(status);verifyNoInteractions(workerDb);
        assertThat(steps).noneMatch(s->s.stepCode().equals("PROVIDER_PROFILES") && s.statusCode().equals("PASSED"));
    }
    @Test void nullProviderEvidenceCannotProducePassed() {
        doReturn(new AttachmentProviderQaEvidenceGate.Assessment("PASSED","INVALID",null)).when(providerQa).selectAssessment(any(),any(),any());
        reserve();assertThat(service.saveNextValidationRun()).isEqualTo("CONFLICT");verifyNoInteractions(workerDb);
    }
    @Test void savedStepsMustMatchAllFourBeforeVerified() {
        providerPassed();reserve();
        doAnswer(c->{steps.removeIf(s->"PROVIDER_PROFILES".equals(s.stepCode()));return mapper.readTree("{\"testDouble\":true}");})
                .when(workerDb).selectValidatedResult(any(),any(),any());
        assertThat(service.saveNextValidationRun()).isEqualTo("CONFLICT");
        verify(dao,never()).updateFinished(any(),any(),eq("VERIFIED"),any());
    }
    @Test void snapshotChangedBeforeExecutionProducesConflictWithoutParser() {
        UUID id=reserve();frozen=new AttachmentPolicyValidationSnapshotFactory.Frozen("9".repeat(64),"{}",frozen.rule(),installed);
        assertThat(service.saveNextValidationRun()).isEqualTo("CONFLICT");verifyNoInteractions(golden,runtime);
        assertThat(steps).isEmpty();verify(dao).deleteExtractionLease(eq(id),any());
    }
    @Test void missingExtractionSlotRollsBackClaimAndNeverRunsChecks() {
        reserve();lease=false;assertThat(service.saveNextValidationRun()).isEqualTo("IDLE");verifyNoInteractions(golden,runtime);
        // 실제 rollback 복원은 PostgreSQL 통합 테스트에서 확인한다.
    }
    @Test void failedGoldenDoesNotBecomeInputConflictOrInvokeParser() {
        reserve();when(golden.selectValidatedResult(any(),anyString(),any())).thenThrow(new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_POLICY_QA_FAILED,HttpStatus.CONFLICT,"AG-001: QA"));
        assertThat(service.saveNextValidationRun()).isEqualTo("FAILED");verifyNoInteractions(runtime);
        assertThat(steps).singleElement().satisfies(s->assertThat(s.statusCode()).isEqualTo("FAILED"));
    }
    @Test void cancellationDuringRuntimeCannotBeOverwrittenByIncomplete() {
        UUID id=reserve();afterRuntime=()->{var row=runs.get(id);replace(id,"CANCEL_REQUESTED",row.leaseToken(),"CANCELLED_BY_ADMIN");};
        // 실제 DB는 취소 후 step 쓰기를 거부한다.
        when(dao.insertStep(any(),any(),eq("INSTALLED_RUNTIME"),anyString(),anyString(),anyString())).thenReturn(0);
        assertThat(service.saveNextValidationRun()).isEqualTo("CANCELLED");
        assertThat(steps).extracting(AttachmentPolicyValidationRows.Step::stepCode).containsExactly("CLASSIFICATION_GOLDEN");
    }
    @Test void contextChangeAfterRuntimeCannotBecomeCompletedEvidence() {
        reserve();afterRuntime=()->frozen=new AttachmentPolicyValidationSnapshotFactory.Frozen("9".repeat(64),"{}",frozen.rule(),installed);
        assertThat(service.saveNextValidationRun()).isEqualTo("CONFLICT");
    }
    @Test void cancellationRequiresSamePolicyAndCurrentRunVersion() {
        UUID id=reserve();
        assertThatThrownBy(()->service.selectRunDetails(auth("ADMIN"),UUID.randomUUID(),id)).isInstanceOf(ApiException.class);
        assertThatThrownBy(()->service.updateCancellation(auth("ADMIN"),policyId,id,new AttachmentPolicyCheckRequest(1,"취소"))).hasMessageContaining("조회 이후");
        assertThat(service.updateCancellation(auth("ADMIN"),policyId,id,request()).statusCode()).isEqualTo("CANCELLED");
        assertThatThrownBy(()->service.updateCancellation(auth("ADMIN"),policyId,id,new AttachmentPolicyCheckRequest(1,"취소"))).hasMessageContaining("대기·실행 중");
    }
    @Test void expiredOwnerCannotReturnSuccessAndStillReleasesOnlyOwnSlot() {
        UUID id=reserve();doReturn(0).when(dao).updateFinished(any(),any(),anyString(),nullable(String.class));
        assertThat(service.saveNextValidationRun()).isEqualTo("LEASE_LOST");verify(dao).deleteExtractionLease(eq(id),any());
    }
    @Test void databaseQaFailureIsRecordedWithoutOpeningPublication() {
        reserve();doThrow(new AttachmentWorkerDbQaProcess.Failure("QA_CASES_INCOMPLETE")).when(workerDb).selectValidatedResult(any(),any(),any());
        assertThat(service.saveNextValidationRun()).isEqualTo("FAILED");
        assertThat(steps).last().satisfies(step->{assertThat(step.stepCode()).isEqualTo("WORKER_DB_RECOVERY");assertThat(step.statusCode()).isEqualTo("FAILED");});
        verify(policies,never()).updatePolicyDraft(any());
    }
    @Test void missingDatabaseResultCannotBeStoredAsPassed() {
        reserve();doReturn(null).when(workerDb).selectValidatedResult(any(),any(),any());
        assertThat(service.saveNextValidationRun()).isEqualTo("CONFLICT");
        assertThat(steps).filteredOn(s->s.stepCode().equals("WORKER_DB_RECOVERY")).noneMatch(s->s.statusCode().equals("PASSED"));
    }
    @Test void twelveCaseRuntimeResultCannotPassCurrentPolicyValidation() throws Exception {
        reserve(); var current=runtimeResult();
        var incomplete=new AttachmentRuntimeGate.Result(current.runId(),current.scope(),current.suiteVersion(),current.suiteHash(),current.runtimeHash(),
                current.extractorVersion(),current.resultHash(),12,current.cases().subList(0,12),current.startedAt(),current.completedAt());
        doReturn(incomplete).when(runtime).selectValidatedResult(any(BooleanSupplier.class));
        assertThat(service.saveNextValidationRun()).isEqualTo("CONFLICT");
        assertThat(steps).filteredOn(s->s.stepCode().equals("INSTALLED_RUNTIME")).noneMatch(s->s.statusCode().equals("PASSED"));
        verifyNoInteractions(workerDb); verify(policies,never()).updatePolicyDraft(any());
    }
    @Test void providerPlanReadIsPagedAndNeverRunsQaOrMutatesPolicy() {
        var response=service.selectProviderQaPlan(auth("OPERATOR"),policyId,2,1);
        assertThat(response.summary().targetCount()).isEqualTo(2);assertThat(response.targets().items()).hasSize(1);
        assertThat(response.targets().items().getFirst().providerCode()).isEqualTo("GOV24_PUBLIC_SERVICE");
        assertThat(response.summary().currentHttpRequests()).isZero();assertThat(response.summary().isQaPassed()).isFalse();
        verifyNoInteractions(golden,runtime,workerDb,audit);verify(dao,never()).insertRun(any());
        assertThatThrownBy(()->service.selectProviderQaPlan(auth("ADMIN"),policyId,1,101)).hasMessageContaining("1~100");
        assertThatThrownBy(()->service.selectProviderQaPlan(auth("USER"),policyId,1,20)).isInstanceOf(ApiException.class);
    }
}
