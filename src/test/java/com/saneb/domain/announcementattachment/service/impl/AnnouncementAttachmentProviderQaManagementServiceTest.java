package com.saneb.domain.announcementattachment.service.impl;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.*;
import com.saneb.common.error.ApiException;
import com.saneb.domain.announcementattachment.dao.*;
import com.saneb.domain.announcementattachment.dto.*;
import com.saneb.domain.announcementattachment.qa.*;
import com.saneb.domain.announcementattachment.vo.*;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationRuleSet;
import com.saneb.domain.announcementsource.dao.AnnouncementSourceDao;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceRuleValidationDetails;
import com.saneb.domain.auth.vo.*;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

class AnnouncementAttachmentProviderQaManagementServiceTest {
    private final AnnouncementAttachmentPolicyDao policies=mock(AnnouncementAttachmentPolicyDao.class);
    private final AnnouncementAttachmentProviderQaDao ledger=mock(AnnouncementAttachmentProviderQaDao.class);
    private final AnnouncementAttachmentProviderQaManagementDao dao=mock(AnnouncementAttachmentProviderQaManagementDao.class);
    private final AttachmentPolicyValidationSnapshotFactory snapshots=mock(AttachmentPolicyValidationSnapshotFactory.class);
    private final AttachmentProviderQaCatalog catalog=mock(AttachmentProviderQaCatalog.class);
    private final AnnouncementSourceDao audit=mock(AnnouncementSourceDao.class);
    private final com.saneb.domain.announcementattachment.service.AnnouncementAttachmentProviderQaExecutionService execution=mock(com.saneb.domain.announcementattachment.service.AnnouncementAttachmentProviderQaExecutionService.class);
    private final PlatformTransactionManager transactions=mock(PlatformTransactionManager.class);
    private final ObjectMapper mapper=new ObjectMapper().findAndRegisterModules().enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
    private final UUID policyId=UUID.randomUUID(),ruleId=UUID.randomUUID(),actorId=UUID.randomUUID(),key=UUID.randomUUID();
    private final OffsetDateTime now=OffsetDateTime.parse("2026-09-12T11:00:00+09:00");
    private final AtomicInteger txCount=new AtomicInteger();
    private final Map<UUID,AttachmentProviderQaRows.RunInsert> runs=new LinkedHashMap<>();
    private final Map<UUID,AttachmentProviderQaManagementRows.PlanInsert> plans=new HashMap<>();
    private final List<AttachmentProviderQaRows.CaseInsert> cases=new ArrayList<>();
    private AttachmentProviderQaCatalog.Prepared prepared;
    private AttachmentPolicyValidationSnapshotFactory.Frozen frozen;
    private final AttachmentPolicyValidationSnapshotFactory.Runtime runtime=new AttachmentPolicyValidationSnapshotFactory.Runtime("a".repeat(64),"b".repeat(64),"c".repeat(64));
    private String state="READY";
    private AnnouncementAttachmentProviderQaManagementServiceImpl service;
    @BeforeEach void setup() throws Exception {
        when(snapshots.json(any())).thenAnswer(c->mapper.writeValueAsString(c.getArgument(0)));
        when(snapshots.hash(any())).thenAnswer(c->hash(c.getArgument(0)));
        when(transactions.getTransaction(any())).thenAnswer(c->{txCount.incrementAndGet();return new SimpleTransactionStatus();});
        doAnswer(c->{txCount.decrementAndGet();return null;}).when(transactions).commit(any());
        doAnswer(c->{txCount.decrementAndGet();return null;}).when(transactions).rollback(any());
        when(snapshots.selectRuntime()).thenAnswer(c->{assertThat(txCount.get()).isZero();return runtime;});
        var policy=new AttachmentPolicyManagementRows.Row(policyId,"TEST",1,0,"DRAFT","COLLECT_ONLY",ruleId,"DRAFT",null,"{}","[]",actorId,now,now,null,null,null,null,null);
        when(policies.selectPolicyDetails(eq(policyId),anyBoolean())).thenReturn(policy);
        var rules=new AnnouncementSourceClassificationRuleSet("TEST",List.of());
        var input=new AttachmentProviderQaCase("CASE-1","TEST","d".repeat(64),null,"합성 계약 테스트",rules,runtime.runtimeHash(),"NO_FILES",true,List.of(),new AttachmentProviderQaCase.Limits(420,3,100));
        var plan=new AttachmentProviderQaCatalog.Plan("TEST","e".repeat(64),"f".repeat(64),List.of(
                new AttachmentProviderQaCatalog.TargetPlan("BIZINFO","PROFILE_MISSING",1,1,0,3,List.of("PDF","HWP","HWPX"),false),
                new AttachmentProviderQaCatalog.TargetPlan("GOV24_PUBLIC_SERVICE","PROFILE_MISSING",0,0,0,3,List.of("PDF","HWP","HWPX"),false)),
                List.of(new AttachmentProviderQaCatalog.CasePlan("CASE-1","BIZINFO","EXPECTED_INPUT_READY","1".repeat(64),0,false,List.of())),
                List.of(new AttachmentProviderQaCatalog.Segment(1,List.of("CASE-1"),3,100,480)),1,false,false);
        prepared=new AttachmentProviderQaCatalog.Prepared(plan,List.of(input));
        frozen=new AttachmentPolicyValidationSnapshotFactory.Frozen("2".repeat(64),mapper.writeValueAsString(Map.of("schemaVersion",6,
                "providerQaPlan",AttachmentProviderQaPlan.selectPlan(List.of(),List.of()),"providerQaCatalog",plan,
                "targets",List.of(Map.of("providerCode","BIZINFO"),Map.of("providerCode","GOV24_PUBLIC_SERVICE")))),
                new AnnouncementSourceRuleValidationDetails(ruleId,0,"DRAFT",null,"3".repeat(64),rules),runtime);
        when(snapshots.selectSnapshot(any(),any())).thenAnswer(c->frozen);
        when(catalog.selectPrepared(any(),any(),anyString(),any())).thenAnswer(c->prepared);
        when(dao.selectActiveRunIds()).thenReturn(List.of());
        when(ledger.insertRun(any())).thenAnswer(c->{var run=(AttachmentProviderQaRows.RunInsert)c.getArgument(0);runs.put(run.runId(),run);return 1;});
        when(dao.insertPlan(any())).thenAnswer(c->{var p=(AttachmentProviderQaManagementRows.PlanInsert)c.getArgument(0);plans.put(p.runId(),p);return 1;});
        when(ledger.insertCase(any())).thenAnswer(c->{cases.add(c.getArgument(0));return 1;});
        when(ledger.updateReady(any())).thenReturn(1);
        when(dao.selectRunDetails(any())).thenAnswer(c->row(c.getArgument(0)));
        when(dao.selectRequestDetails(any())).thenAnswer(c->runs.values().stream().filter(v->v.idempotencyKey().equals(c.getArgument(0))).findFirst().map(v->row(v.runId())).orElse(null));
        when(dao.selectRunList(any())).thenAnswer(c->runs.keySet().stream().map(this::row).toList());
        when(dao.selectRunCount(policyId)).thenAnswer(c->(long)runs.size());
        when(dao.selectCaseCount(any())).thenAnswer(c->(long)cases.size());
        when(dao.selectCaseList(any())).thenAnswer(c->cases.stream().map(v->new AttachmentProviderQaManagementRows.Item(v.caseId(),v.ordinal(),v.caseCode(),v.inputHash(),v.profileHash(),
                v.expectedFileCount(),"PENDING",0,0,0L,null,null,null,null)).toList());
        when(ledger.updateCancellation(any(),anyInt())).thenAnswer(c->{state="CANCEL_REQUESTED";return 1;});
        service=instance(true);
    }
    private String hash(Object value) throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(mapper.writeValueAsBytes(value)));}
    private AnnouncementAttachmentProviderQaManagementServiceImpl instance(boolean enabled){return new AnnouncementAttachmentProviderQaManagementServiceImpl(policies,ledger,dao,snapshots,catalog,audit,mapper,transactions,execution,enabled);}
    private Authentication auth(String role){return auth(role,"ACTIVE",false);}
    private Authentication auth(String role,String status,boolean reset){
        var user=new AuthenticatedUserDetails(new AuthUserDetailsRow(actorId,"qa","unused","QA",status,reset,null,null,null),List.of(role));
        return UsernamePasswordAuthenticationToken.authenticated(user,null,user.getAuthorities());
    }
    private AttachmentProviderQaRequests.Reservation request() throws Exception {
        return new AttachmentProviderQaRequests.Reservation(0,frozen.hash(),prepared.plan().catalogHash(),hash(mapper.convertValue(prepared.plan(),Object.class)),1,1,3L,100L,480,true,true,true,"QA 예약 사유 원문");
    }
    private AttachmentProviderQaRequests.Reservation changed(String field,Object value) throws Exception {
        var json=mapper.valueToTree(request());((com.fasterxml.jackson.databind.node.ObjectNode)json).set(field,mapper.valueToTree(value));
        return mapper.treeToValue(json,AttachmentProviderQaRequests.Reservation.class);
    }
    private AttachmentProviderQaManagementRows.Run row(UUID id){
        var r=runs.get(id);if(r==null)return null;var p=plans.get(id);
        return new AttachmentProviderQaManagementRows.Run(id,r.policyId(),r.policyVersion(),r.ruleReleaseId(),r.ruleVersion(),r.snapshotHash(),r.catalogHash(),r.executionCodeHash(),r.runtimeHash(),
                r.expectedCaseCount(),r.maximumRequests(),r.maximumBytes(),0L,0L,state,1,r.requestedBy(),r.idempotencyKey(),r.requestHash(),now,now.plusDays(1),null,p.planHash(),p.segmentNo(),
                p.segmentCount(),p.catalogCaseCount(),p.executableCaseCount(),p.expectationCoverageComplete(),p.maximumSecondsIncludingMargin(),true);
    }
    private UUID reserve() throws Exception{return service.insertRun(auth("ADMIN"),policyId,key,request()).runId();}
    @Test void previewIsReadOnlyAndKeepsWholeCoverageSeparateFromPartialSegment() {
        var view=service.selectExecutionPlan(auth("APPROVER"),policyId,1,1);
        assertThat(view.targetCount()).isEqualTo(2);assertThat(view.isExpectationCoverageComplete()).isFalse();assertThat(view.isQaPassed()).isFalse();
        assertThat(view.isReservationEnabled()).isTrue();assertThat(view.segments().items()).hasSize(1);verifyNoInteractions(ledger,audit);verify(dao,never()).insertPlan(any());
    }
    @Test void targetCoveragePagesKeepFullDenominatorWithoutReservationOrNetwork() {
        var view=service.selectTargetCoverageList(auth("APPROVER"),policyId,2,1);
        assertThat(view.targets().totalCount()).isEqualTo(2);assertThat(view.targets().items()).singleElement()
                .satisfies(t->assertThat(t.targetKey()).isEqualTo("GOV24_PUBLIC_SERVICE"));
        assertThat(view.isExpectationCoverageComplete()).isFalse();assertThat(view.isQaPassed()).isFalse();assertThat(view.planHash()).matches("[0-9a-f]{64}");
        assertThat(service.selectTargetCoverageList(auth("OPERATOR"),policyId,3,1).targets().items()).isEmpty();
        verifyNoInteractions(ledger,audit,execution);verify(dao,never()).insertPlan(any());assertThat(txCount.get()).isZero();
    }
    @Test void targetCoverageUsesActualFrozenV2MetadataAndDoesNotInventPassedFormats() throws Exception {
        var original=prepared.plan();var target=original.targets().getFirst();
        var coverage=new AttachmentProviderQaCatalog.FormatCoverage("FIXED_SAMPLE_FORMATS_V2",List.of("HWP","HWPX","PDF"),List.of("HWP","HWPX","PDF"));
        var applicability=new AttachmentProviderQaCatalog.FormatApplicability("EXPECTATIONS_UNKNOWN",List.of(),List.of("HWP","HWPX","PDF"),0);
        var targets=List.of(new AttachmentProviderQaCatalog.TargetPlan(target.targetKey(),target.bindingStatusCode(),target.referenceCount(),target.executableCount(),0,3,List.of(),false,applicability),original.targets().get(1));
        var plan=new AttachmentProviderQaCatalog.Plan(original.catalogVersion(),original.catalogHash(),original.scopeHash(),targets,original.cases(),original.segments(),original.executableCount(),false,false,coverage);
        prepared=new AttachmentProviderQaCatalog.Prepared(plan,prepared.inputs());
        var json=(com.fasterxml.jackson.databind.node.ObjectNode)mapper.readTree(frozen.json());json.set("providerQaCatalog",mapper.valueToTree(plan));
        frozen=new AttachmentPolicyValidationSnapshotFactory.Frozen(frozen.hash(),mapper.writeValueAsString(json),frozen.rule(),runtime);
        var view=service.selectTargetCoverageList(auth("ADMIN"),policyId,1,1);
        assertThat(view.formatCoverage()).isEqualTo(coverage);assertThat(view.targets().items().getFirst().formatApplicability()).isEqualTo(applicability);
        assertThat(view.isQaPassed()).isFalse();verifyNoInteractions(ledger,audit,execution);
    }
    @Test void targetCoverageValidatesPagingAndActorBeforeInstalledRuntime() {
        for(int[] range:List.of(new int[]{0,20},new int[]{1,0},new int[]{1,101},new int[]{Integer.MAX_VALUE,100}))
            assertThatThrownBy(()->service.selectTargetCoverageList(auth("ADMIN"),policyId,range[0],range[1])).isInstanceOf(ApiException.class);
        for(String role:List.of("USER","PARTNER","REVIEWER"))
            assertThatThrownBy(()->service.selectTargetCoverageList(auth(role),policyId,1,20)).isInstanceOf(ApiException.class);
        assertThatThrownBy(()->service.selectTargetCoverageList(auth("ADMIN","INACTIVE",false),policyId,1,20)).isInstanceOf(ApiException.class);
        verify(snapshots,never()).selectRuntime();verifyNoInteractions(ledger,audit,execution);
    }
    @Test void catalogWithoutExecutionExpectationsDisablesReservationAndNeverCreatesRun() throws Exception {
        var p=prepared.plan();var empty=new AttachmentProviderQaCatalog.Plan(p.catalogVersion(),p.catalogHash(),p.scopeHash(),p.targets(),
                List.of(new AttachmentProviderQaCatalog.CasePlan("CASE-1","BIZINFO","REFERENCE_ONLY",null,null,false,List.of())),List.of(),0,false,false);
        prepared=new AttachmentProviderQaCatalog.Prepared(empty,List.of());
        var json=(com.fasterxml.jackson.databind.node.ObjectNode)mapper.readTree(frozen.json());json.set("providerQaCatalog",mapper.valueToTree(empty));
        frozen=new AttachmentPolicyValidationSnapshotFactory.Frozen(frozen.hash(),mapper.writeValueAsString(json),frozen.rule(),runtime);
        var preview=service.selectExecutionPlan(auth("ADMIN"),policyId,1,20);
        assertThat(preview.catalogCaseCount()).isEqualTo(1);assertThat(preview.executableCaseCount()).isZero();assertThat(preview.isReservationEnabled()).isFalse();
        assertThat(preview.segments().items()).isEmpty();assertThatThrownBy(this::reserve).hasMessageContaining("실행 가능한 공고 기대값");verify(ledger,never()).insertRun(any());
    }
    @Test void reserveBindsPlanCasesAndFullScopeInOneShortWriteWithoutExecutingOrPublishing() throws Exception {
        var id=reserve();assertThat(runs.get(id).requiredScopeJson()).contains("BIZINFO","GOV24_PUBLIC_SERVICE");assertThat(cases).hasSize(1);
        assertThat(plans.get(id).maximumSecondsIncludingMargin()).isEqualTo(480);assertThat(txCount.get()).isZero();
        var order=inOrder(ledger,dao);order.verify(ledger).selectQueueLock();order.verify(ledger).insertRun(any());order.verify(dao).insertPlan(any());order.verify(ledger).insertCase(any());order.verify(ledger).updateReady(id);
        verify(ledger,never()).updateClaim(any(),any());verify(policies,never()).updatePolicyDraft(any());
        var log=ArgumentCaptor.forClass(com.saneb.domain.announcementsource.vo.AnnouncementSourceAuditLogCommand.class);verify(audit).insertAuditLog(log.capture());
        assertThat(log.getValue().metadataJson()).contains("reasonHash").doesNotContain(request().reason());
    }
    @Test void sameKeyReplaysBeforeDisabledRuntimeQueueOrCooldownAndOtherInputConflicts() throws Exception {
        UUID id=reserve();clearInvocations(snapshots,ledger,dao);
        assertThat(instance(false).insertRun(auth("ADMIN"),policyId,key,request()).runId()).isEqualTo(id);
        verify(snapshots,never()).selectRuntime();verify(ledger,never()).selectQueueLock();verify(dao,never()).selectCoolingDown(any());
        assertThatThrownBy(()->service.insertRun(auth("ADMIN"),policyId,key,changed("reason","다른 사유"))).hasMessageContaining("멱등 키");
        assertThatThrownBy(()->service.insertRun(auth("ADMIN"),UUID.randomUUID(),key,request())).hasMessageContaining("멱등 키");
    }
    @ParameterizedTest @ValueSource(strings={"expectedVersion","expectedSnapshotHash","expectedCatalogHash","expectedPlanHash","segmentNo","expectedCaseCount","maximumRequests","maximumBytes","maximumSecondsIncludingMargin"})
    void exactApprovalMismatchCannotInsert(String field) throws Exception {
        Object value=field.endsWith("Hash")?"9".repeat(64):field.equals("maximumSecondsIncludingMargin")?481:field.equals("maximumBytes")?101:2;
        var request=changed(field,value);assertThatThrownBy(()->service.insertRun(auth("ADMIN"),policyId,key,request)).isInstanceOf(ApiException.class);verify(ledger,never()).insertRun(any());
    }
    @ParameterizedTest @ValueSource(strings={"acknowledgeScope","acknowledgeNetworkBudget","acknowledgeIncompleteCoverage"})
    void requiredAcknowledgementsCannotBeAssumed(String field) throws Exception {
        var request=changed(field,false);assertThatThrownBy(()->service.insertRun(auth("ADMIN"),policyId,key,request)).isInstanceOf(ApiException.class);verify(ledger,never()).insertRun(any());
    }
    @ParameterizedTest @ValueSource(strings={"OPERATOR","APPROVER","USER","PARTNER","REVIEWER"})
    void directServiceWritesRequireActiveAdmin(String role) throws Exception {
        var request=request();assertThatThrownBy(()->service.insertRun(auth(role),policyId,key,request)).isInstanceOf(ApiException.class);
        assertThatThrownBy(()->service.updateCancellation(auth(role),policyId,UUID.randomUUID(),new AttachmentPolicyCheckRequest(1,"취소"))).isInstanceOf(ApiException.class);
        verifyNoInteractions(ledger,dao);
    }
    @Test void disabledOrResetRequiredActorCannotReadAndMissingAuthenticationIsRejected() {
        for(var actor:List.of(auth("ADMIN","INACTIVE",false),auth("ADMIN","ACTIVE",true)))
            assertThatThrownBy(()->service.selectRunList(actor,policyId,1,20)).isInstanceOf(ApiException.class);
        assertThatThrownBy(()->service.selectRunList(null,policyId,1,20)).isInstanceOf(ApiException.class);verifyNoInteractions(dao);
    }
    @Test void offOrActiveQueueAndCooldownRejectNewReservations() throws Exception {
        assertThatThrownBy(()->instance(false).insertRun(auth("ADMIN"),policyId,key,request())).hasMessageContaining("비활성");
        when(dao.selectActiveRunIds()).thenReturn(List.of(UUID.randomUUID()));assertThatThrownBy(this::reserve).hasMessageContaining("다른 Provider");
        when(dao.selectActiveRunIds()).thenReturn(List.of());when(dao.selectCoolingDown(policyId)).thenReturn(true);assertThatThrownBy(this::reserve).hasMessageContaining("60초");
        verify(ledger,never()).insertRun(any());
    }
    @Test void lateSnapshotChangeOrCaseInsertFailureRollsBackReservation() throws Exception {
        var initial=frozen;var changed=new AttachmentPolicyValidationSnapshotFactory.Frozen("9".repeat(64),initial.json(),initial.rule(),initial.runtime());
        doReturn(initial,changed).when(snapshots).selectSnapshot(any(),any());assertThatThrownBy(this::reserve).hasMessageContaining("예약 직전");verify(ledger,never()).insertRun(any());
        doReturn(initial).when(snapshots).selectSnapshot(any(),any());when(ledger.insertCase(any())).thenReturn(0);assertThatThrownBy(this::reserve).hasMessageContaining("저장 상태");
        verify(transactions,atLeastOnce()).rollback(any());verifyNoInteractions(audit);assertThat(txCount.get()).isZero();
    }
    @Test void readPagesAndCancellationWorkOffAndCannotLeakOwnershipOrPolicyScope() throws Exception {
        UUID id=reserve();var off=instance(false);clearInvocations(snapshots);
        assertThat(off.selectRunList(auth("OPERATOR"),policyId,1,20).totalCount()).isEqualTo(1);
        assertThat(off.selectCaseList(auth("APPROVER"),policyId,id,1,20).items()).hasSize(1);
        assertThatThrownBy(()->off.selectRunDetails(auth("ADMIN"),UUID.randomUUID(),id)).hasMessageContaining("찾을 수 없습니다");
        assertThatThrownBy(()->off.updateCancellation(auth("ADMIN"),policyId,id,new AttachmentPolicyCheckRequest(0,"취소"))).hasMessageContaining("버전");
        var cancelled=off.updateCancellation(auth("ADMIN"),policyId,id,new AttachmentPolicyCheckRequest(1,"취소 사유"));assertThat(cancelled.statusCode()).isEqualTo("CANCEL_REQUESTED");
        String json=mapper.writeValueAsString(cancelled);assertThat(json).doesNotContain("requestedBy","idempotencyKey","requestHash","leaseToken","executionCodeHash","runtimeHash");
        assertThat(cancelled.isQaPassed()).isFalse();verify(snapshots,never()).selectRuntime();verify(ledger,never()).deleteCaseResources(any(),any());
        assertThatThrownBy(()->off.updateCancellation(auth("ADMIN"),policyId,id,new AttachmentPolicyCheckRequest(1,"다시"))).hasMessageContaining("대기·실행 중");
    }
    @Test void paginationOverflowAndUnknownPolicyDoNotReadRuntime() {
        assertThatThrownBy(()->service.selectExecutionPlan(auth("ADMIN"),policyId,Integer.MAX_VALUE,100)).isInstanceOf(ApiException.class);
        assertThatThrownBy(()->service.selectRunList(auth("ADMIN"),policyId,0,20)).isInstanceOf(ApiException.class);
        assertThatThrownBy(()->service.selectExecutionPlan(auth("ADMIN"),UUID.randomUUID(),1,20)).isInstanceOf(ApiException.class);verify(snapshots,never()).selectRuntime();
    }
    @Test void snapshotCatalogChangedOrUnsupportedSchemaCannotBePrepared() {
        frozen=new AttachmentPolicyValidationSnapshotFactory.Frozen(frozen.hash(),"{\"schemaVersion\":5}",frozen.rule(),runtime);
        assertThatThrownBy(()->service.selectExecutionPlan(auth("ADMIN"),policyId,1,20)).isInstanceOf(ApiException.class);verify(ledger,never()).insertRun(any());
    }
    private UUID runnable() throws Exception {
        UUID id=reserve();when(dao.selectActiveRunIds()).thenReturn(List.of(id));
        when(ledger.selectCaseList(id)).thenAnswer(c->cases.stream().map(v->new AttachmentProviderQaRows.CaseRow(v.caseId(),v.runId(),v.caseCode(),v.inputHash(),v.profileHash(),
                v.expectedFileCount(),v.maximumSeconds(),v.maximumRequests(),v.maximumBytes(),0,0L,"PENDING",0,null,null,"READY",runtime.executionCodeHash(),runtime.runtimeHash(),true,null)).toList());
        when(execution.saveCase(any(),any())).thenAnswer(c->{assertThat(txCount.get()).isZero();return new com.saneb.domain.announcementattachment.service.AnnouncementAttachmentProviderQaExecutionService.Outcome(c.getArgument(0),"PASSED");});
        return id;
    }
    @Test void coordinatorRebuildsExactApprovedInputAndExecutesOneCaseOutsideTransaction() throws Exception {
        runnable();clearInvocations(snapshots,ledger,dao,audit);
        assertThat(service.saveNextProviderQaRun()).isEqualTo("PASSED");verify(execution,times(1)).saveCase(eq(cases.getFirst().caseId()),eq(prepared.inputs().getFirst()));
        verify(audit,never()).insertAuditLog(any());verify(ledger,never()).insertRun(any());assertThat(txCount.get()).isZero();
    }
    @Test void coordinatorOffAndEmptyQueueNeverReadRuntime() {
        assertThat(instance(false).saveNextProviderQaRun()).isEqualTo("DISABLED");assertThat(service.saveNextProviderQaRun()).isEqualTo("IDLE");
        verifyNoInteractions(execution);verify(snapshots,never()).selectRuntime();
    }
    @Test void coordinatorChangedSnapshotFailsPendingInsteadOfExecutingOrRebinding() throws Exception {
        UUID id=runnable();frozen=new AttachmentPolicyValidationSnapshotFactory.Frozen("9".repeat(64),frozen.json(),frozen.rule(),runtime);
        assertThat(service.saveNextProviderQaRun()).isEqualTo("INPUT_CHANGED");verify(dao).updatePendingInputChanged(id);verifyNoInteractions(execution);
    }
    @Test void coordinatorCannotHideMissingCasesOrChangedCaseHash() throws Exception {
        UUID id=runnable();when(ledger.selectCaseList(id)).thenReturn(List.of());
        assertThat(service.saveNextProviderQaRun()).isEqualTo("INPUT_CHANGED");verify(dao).updatePendingInputChanged(id);verifyNoInteractions(execution);
    }
    @Test void coordinatorRejectsChangedHashAndDoesNotRepeatCompletedCase() throws Exception {
        UUID id=runnable();var c=cases.getFirst();
        cases.set(0,new AttachmentProviderQaRows.CaseInsert(c.caseId(),c.runId(),c.ordinal(),c.caseCode(),"9".repeat(64),c.profileHash(),c.expectedFileCount(),c.maximumSeconds(),c.maximumRequests(),c.maximumBytes()));
        assertThat(service.saveNextProviderQaRun()).isEqualTo("INPUT_CHANGED");verifyNoInteractions(execution);cases.set(0,c);
        when(execution.saveCase(any(),any())).thenAnswer(call->{state="COMPLETED";return new com.saneb.domain.announcementattachment.service.AnnouncementAttachmentProviderQaExecutionService.Outcome(c.caseId(),"PASSED");});
        assertThat(service.saveNextProviderQaRun()).isEqualTo("PASSED");assertThat(service.saveNextProviderQaRun()).isEqualTo("IDLE");verify(execution,times(1)).saveCase(any(),any());
    }
    @Test void coordinatorLeavesUnavailablePreparationPendingAndDoesNotInventInputChange() throws Exception {
        runnable();doThrow(new ApiException(com.saneb.common.error.ErrorCode.ANNOUNCEMENT_ATTACHMENT_POLICY_QA_FAILED,org.springframework.http.HttpStatus.CONFLICT,"테스트 설치 불가"))
                .when(snapshots).selectRuntime();
        assertThat(service.saveNextProviderQaRun()).isEqualTo("PREPARATION_UNAVAILABLE");verify(dao,never()).updatePendingInputChanged(any());verifyNoInteractions(execution);
    }
    @Test void coordinatorLeavesLiveOwnerAndCancelledRunAlone() throws Exception {
        UUID id=runnable();var v=cases.getFirst();
        when(ledger.selectCaseList(id)).thenReturn(List.of(new AttachmentProviderQaRows.CaseRow(v.caseId(),id,v.caseCode(),v.inputHash(),v.profileHash(),0,420,3,100L,0,0L,
                "RUNNING",1,UUID.randomUUID(),now.plusMinutes(8),"RUNNING",runtime.executionCodeHash(),runtime.runtimeHash(),true,null)));
        assertThat(service.saveNextProviderQaRun()).isEqualTo("BUSY");state="CANCEL_REQUESTED";assertThat(service.saveNextProviderQaRun()).isEqualTo("IDLE");
        verifyNoInteractions(execution);verify(ledger,never()).deleteCaseResources(any(),any());
    }
}
