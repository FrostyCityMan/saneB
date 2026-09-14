package com.saneb.domain.announcementattachment.service.impl;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.common.error.ApiException;
import com.saneb.domain.announcementattachment.dao.*;
import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementattachment.dto.*;
import com.saneb.domain.announcementattachment.vo.*;
import com.saneb.domain.announcementsource.dao.AnnouncementSourceDao;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceAuditLogCommand;
import com.saneb.domain.auth.vo.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

class AnnouncementAttachmentBatchServiceTest {
    @Test void gov24AliasIncludesCanonicalCandidateWithoutPretendingMissingProfileIsReady() {
        var scope=selectGov24Scope();
        var preview=service.selectScopePreview(auth("ADMIN"),scope);
        assertThat(preview.scope().providerCodes()).containsExactly("GOV24");
        assertThat(preview.counts()).containsExactly(new AttachmentBatchRows.Bucket("GOV24","CANDIDATE",1L));
        assertThat(preview.items()).singleElement().satisfies(row->{
            assertThat(row.providerCode()).isEqualTo("GOV24_PUBLIC_SERVICE");
            assertThat(row.readinessCode()).isEqualTo("PROFILE_REQUIRED");
        });
        assertThat(preview.candidateCount()).isEqualTo(1);assertThat(preview.canReserve()).isFalse();
        assertThatThrownBy(()->service.insertBatch(auth("ADMIN"),key,new AttachmentBatchRequests.Reservation(scope,preview.scopeHash(),"정부24 범위")))
                .isInstanceOf(ApiException.class).hasMessageContaining("미지원 출처");
        verify(dao,never()).insertScopeJob(any());verify(dao,never()).insertBatch(any());
    }
    @Test void gov24FixedMembershipUsesScopeAliasButKeepsActualJobProviderAndReplay() throws Exception {
        var scope=selectGov24Scope();
        var fixture=mock(AttachmentDiscoveryProfile.class);
        when(fixture.selectProviderCode()).thenReturn("GOV24_PUBLIC_SERVICE");
        when(fixture.selectProfileCode()).thenReturn("GOV24_TEST_ONLY");when(fixture.selectProfileHash()).thenReturn("c".repeat(64));
        when(fixture.selectDetailUri(any(AttachmentDiscoveryProfile.Source.class))).thenReturn(java.net.URI.create("https://example.invalid/fixture"));
        policyRow=new AttachmentPolicyRow(policyRow.policyId(),policyRow.policyStatusCode(),policyRow.modeCode(),rule,policyRow.releaseStatusCode(),
                policyRow.policyHash(),policyRow.settingsJson(),mapper.writeValueAsString(List.of(Map.of("providerCode","GOV24_PUBLIC_SERVICE",
                "profileCode","GOV24_TEST_ONLY","profileHash","c".repeat(64)))),0);
        service=new AnnouncementAttachmentBatchServiceImpl(dao,jobs,intake,new AttachmentDiscoveryProfileRegistry(List.of(fixture)),audit,mapper);
        when(dao.selectFixedSourceCandidateList(any())).thenAnswer(call->List.of(candidate));
        var fixed=new AttachmentBatchRows.FixedScope(UUID.randomUUID(),1,List.of(source));
        var preview=service.selectFixedScopePreview(auth("ADMIN"),scope,fixed);
        assertThat(preview.canReserve()).isTrue();assertThat(preview.counts().getFirst().providerCode()).isEqualTo("GOV24");
        var request=new AttachmentBatchRequests.Reservation(scope,preview.scopeHash(),"정부24 고정 분할");
        var first=service.insertFixedBatch(auth("ADMIN"),key,request,fixed);
        assertThat(items).singleElement().satisfies(row->assertThat(row.providerCode()).isEqualTo("GOV24_PUBLIC_SERVICE"));
        clearInvocations(dao,jobs,intake,audit,fixture);
        assertThat(service.insertFixedBatch(auth("ADMIN"),key,request,fixed).batchId()).isEqualTo(first.batchId());
        verify(dao,never()).selectFixedSourceCandidateList(any());verify(dao,never()).insertScopeJob(any());verifyNoInteractions(jobs,intake,audit,fixture);
    }
    private AttachmentBatchRequests.Scope selectGov24Scope() {
        candidate=new AttachmentBatchRows.Candidate(source,"GOV24_PUBLIC_SERVICE",candidate.contentVersionId(),candidate.baseEvaluationId(),rule,2,3,
                candidate.currentEvaluationId(),policy,candidate.confirmationId(),true,null);
        when(dao.selectScopeCounts(any())).thenReturn(List.of(new AttachmentBatchRows.Bucket("GOV24_PUBLIC_SERVICE","CANDIDATE",1L)));
        when(intake.selectSourceLocatorDetails(source)).thenReturn(new AttachmentWorkerSourceRow("GOV24_PUBLIC_SERVICE","FIXTURE",null,null,null));
        return new AttachmentBatchRequests.Scope(policy,List.of("GOV24"),now.minusDays(1),now,null,null,1);
    }
    @Test void fixedReservationUsesOnlyInternalMembershipAndPreservesStandardJobFingerprint() {
        when(dao.selectFixedSourceCandidateList(any())).thenAnswer(call->List.of(candidate));
        var fixed=new AttachmentBatchRows.FixedScope(UUID.randomUUID(),2,List.of(source));
        var preview=service.selectFixedScopePreview(auth("ADMIN"),scope(),fixed);
        assertThat(preview.candidateCount()).isEqualTo(1);assertThat(preview.remainingCount()).isZero();
        var batch=service.insertFixedBatch(auth("ADMIN"),key,new AttachmentBatchRequests.Reservation(scope(),preview.scopeHash(),"분할 예약"),fixed);
        assertThat(batch.frozenScope()).containsEntry("backfillRunId",fixed.runId().toString()).containsEntry("backfillSegmentNo",2);
        assertThat(items).hasSize(1);assertThat(items.getFirst().job().sourceId()).isEqualTo(source);
        verify(dao,never()).selectCandidateList(any());verify(dao,never()).selectScopeCounts(any());
        var collection=collection();assertThat(service.updateCollectionStart(auth("ADMIN"),batch.batchId(),collection).statusCode()).isEqualTo("COLLECTION_PENDING");
    }
    @Test void fixedReservationRejectsMissingMembersAndDoesNotFillFromOriginalScope() {
        when(dao.selectFixedSourceCandidateList(any())).thenReturn(List.of());
        assertThatThrownBy(()->service.selectFixedScopePreview(auth("ADMIN"),scope(),new AttachmentBatchRows.FixedScope(UUID.randomUUID(),1,List.of(source))))
                .isInstanceOf(ApiException.class).hasMessageContaining("다른 후보로 채우지 않고");
        verify(dao,never()).selectCandidateList(any());verify(dao,never()).insertBatch(any());
        assertThatThrownBy(()->service.selectFixedScopePreview(auth("ADMIN"),scope(),new AttachmentBatchRows.FixedScope(UUID.randomUUID(),1,List.of(source,source))))
                .isInstanceOf(ApiException.class);
    }
    @Test void fixedFingerprintIsBoundToRunAndSegmentAndWriteRequiresExistingTransaction() throws Exception {
        when(dao.selectFixedSourceCandidateList(any())).thenAnswer(call->List.of(candidate));UUID run=UUID.randomUUID();
        var first=service.selectFixedScopePreview(auth("ADMIN"),scope(),new AttachmentBatchRows.FixedScope(run,1,List.of(source)));
        var second=service.selectFixedScopePreview(auth("ADMIN"),scope(),new AttachmentBatchRows.FixedScope(run,2,List.of(source)));
        assertThat(first.scopeHash()).isNotEqualTo(second.scopeHash());
        var annotation=AnnouncementAttachmentBatchServiceImpl.class.getMethod("insertFixedBatch",Authentication.class,UUID.class,AttachmentBatchRequests.Reservation.class,AttachmentBatchRows.FixedScope.class)
                .getAnnotation(org.springframework.transaction.annotation.Transactional.class);
        assertThat(annotation.propagation()).isEqualTo(org.springframework.transaction.annotation.Propagation.MANDATORY);
    }
    private final AnnouncementAttachmentBatchDao dao=mock(AnnouncementAttachmentBatchDao.class);
    private final AnnouncementAttachmentJobDao jobs=mock(AnnouncementAttachmentJobDao.class);
    private final AnnouncementAttachmentIntakeDao intake=mock(AnnouncementAttachmentIntakeDao.class);
    private final AnnouncementSourceDao audit=mock(AnnouncementSourceDao.class);
    private final BizInfoAttachmentDiscoveryProfile profile=new BizInfoAttachmentDiscoveryProfile();
    private final ObjectMapper mapper=new ObjectMapper().findAndRegisterModules();
    private final UUID policy=UUID.randomUUID(),rule=UUID.randomUUID(),source=UUID.randomUUID(),actor=UUID.randomUUID(),key=UUID.randomUUID();
    private final OffsetDateTime now=OffsetDateTime.parse("2026-09-11T15:00:00+09:00");
    private AnnouncementAttachmentBatchServiceImpl service;
    private AttachmentBatchRows.Candidate candidate;
    private AttachmentPolicyRow policyRow;
    private AttachmentBatchRows.Row saved;
    private String jobState="SCOPE_READY";
    private final List<AttachmentBatchRows.ItemInsert> items=new ArrayList<>();
    @BeforeEach void setup() throws Exception {
        policyRow=new AttachmentPolicyRow(policy,"ACTIVE","COLLECT_ONLY",rule,"ACTIVE","a".repeat(64),mapper.writeValueAsString(Map.of(
                "engineVersion","attachment-1.0.0","extractorVersion","1.0.0","extractorConfigHash","b".repeat(64),"maximumSourceBytes",83886080)),
                mapper.writeValueAsString(List.of(Map.of("providerCode","BIZINFO","profileCode",profile.selectProfileCode(),"profileHash",profile.selectProfileHash()))),0);
        when(dao.selectPolicyDetails(eq(policy),anyBoolean())).thenAnswer(c->policyRow);
        candidate=new AttachmentBatchRows.Candidate(source,"BIZINFO",UUID.randomUUID(),UUID.randomUUID(),rule,2,3,UUID.randomUUID(),policy,UUID.randomUUID(),true,null);
        when(dao.selectCandidateList(any())).thenAnswer(c->List.of(candidate));
        when(dao.selectScopeCounts(any())).thenReturn(List.of(new AttachmentBatchRows.Bucket("BIZINFO","CANDIDATE",5L),new AttachmentBatchRows.Bucket("BIZINFO","LINKED_PROTECTED",2L)));
        when(intake.selectSourceLocatorDetails(source)).thenReturn(new AttachmentWorkerSourceRow("BIZINFO","PBLN_000000000100000",null,null,null));
        when(dao.selectSourceLocks(anyList())).thenAnswer(c->c.getArgument(0));when(jobs.selectNextGeneration(any(),any(),any())).thenReturn(1);
        when(dao.insertBatch(any())).thenAnswer(c->{var r=(AttachmentBatchRows.Insert)c.getArgument(0);saved=new AttachmentBatchRows.Row(r.batchId(),r.policyId(),"SCOPE_READY",r.scopeHash(),r.maximumCount(),r.itemCount(),0,0,
                r.scopeJson(),r.policySnapshotJson(),r.requestedBy(),r.idempotencyKey(),r.requestHash(),now);return 1;});
        when(dao.insertScopeJob(any())).thenAnswer(c->{items.add(c.getArgument(0));return 1;});
        when(dao.selectBatchDetails(any(),anyBoolean())).thenAnswer(c->saved!=null && saved.batchId().equals(c.getArgument(0))?saved:null);
        when(dao.selectRequestDetails(key)).thenAnswer(c->saved);
        when(dao.selectJobCounts(any())).thenAnswer(c->items.isEmpty()?List.of():List.of(new AttachmentBatchRows.Bucket("BIZINFO",jobState,(long)items.size())));
        when(dao.selectItemList(any())).thenAnswer(c->items.stream().map(i->new AttachmentBatchRows.Item(i.job().jobId(),i.job().sourceId(),i.providerCode(),saved.statusCode(),2,3,null,"NOT_REQUESTED","NOT_REQUESTED")).toList());
        when(dao.updateScopeCancellation(any(),anyInt())).thenAnswer(c->{saved=new AttachmentBatchRows.Row(saved.batchId(),saved.policyId(),"CANCELLED",saved.scopeHash(),saved.maximumCount(),saved.itemCount(),0,1,saved.scopeJson(),saved.policySnapshotJson(),actor,key,saved.requestHash(),now);return 1;});
        when(dao.updateScopeJobsCancelled(any())).thenAnswer(c->{jobState="CANCELLED";return items.size();});
        when(dao.selectExecutionItemList(any())).thenAnswer(c->items.stream().map(i->new AttachmentBatchRows.ExecutionItem(i.job().jobId(),source,i.providerCode(),jobState,
                i.job().requestHash(),i.job().executionSnapshotJson(),i.job().downloadBudgetBytes(),true)).toList());
        when(dao.selectFixedCandidateList(any())).thenAnswer(c->List.of(candidate));
        when(dao.updateCollectionStart(any(),anyInt(),any(),anyString())).thenAnswer(c->{saveState("COLLECTION_PENDING");return 1;});
        when(dao.updateCollectionJobsPending(any())).thenAnswer(c->{jobState="PENDING";return items.size();});
        when(dao.updateCollectionPause(any(),anyInt())).thenAnswer(c->{saveState("COLLECTION_PAUSED");return 1;});
        when(dao.updateCollectionResume(any(),anyInt())).thenAnswer(c->{saveState("COLLECTING");return 1;});
        service=new AnnouncementAttachmentBatchServiceImpl(dao,jobs,intake,new AttachmentDiscoveryProfileRegistry(List.of(profile)),audit,mapper);
    }
    private Authentication auth(String role) {var user=new AuthenticatedUserDetails(new AuthUserDetailsRow(actor,"fixture","unused","QA","ACTIVE",false,null,null,null),List.of(role));return UsernamePasswordAuthenticationToken.authenticated(user,null,user.getAuthorities());}
    private AttachmentBatchRequests.Scope scope() {return new AttachmentBatchRequests.Scope(policy,List.of("BIZINFO"),now.minusDays(1),now,null,null,1);}
    private AttachmentBatchRequests.Reservation request() {return new AttachmentBatchRequests.Reservation(scope(),service.selectScopePreview(auth("ADMIN"),scope()).scopeHash(),"범위 고정 사유");}
    private void saveState(String state) {saved=new AttachmentBatchRows.Row(saved.batchId(),saved.policyId(),state,saved.scopeHash(),saved.maximumCount(),saved.itemCount(),saved.deletedItemCount(),
            saved.rowVersion()+1,saved.scopeJson(),saved.policySnapshotJson(),actor,key,saved.requestHash(),now);}
    private AttachmentBatchRequests.Collection collection() {return new AttachmentBatchRequests.Collection(saved.rowVersion(),saved.scopeHash(),saved.itemCount(),saved.deletedItemCount(),83886080L,132L,"고정 수집 승인");}
    @Test void collectionChecksFixedFingerprintAndCapsWithoutRerunningFilterOrChangingCurrent() {
        service.insertBatch(auth("ADMIN"),key,request());clearInvocations(dao,jobs,intake,audit);
        var request=collection();var result=service.updateCollectionStart(auth("ADMIN"),saved.batchId(),request);
        assertThat(result.statusCode()).isEqualTo("COLLECTION_PENDING");assertThat(result.jobCounts()).containsEntry("PENDING",1L);
        verify(dao,never()).selectScopeCounts(any());verify(dao,never()).selectCandidateList(any());verify(dao,never()).insertScopeJob(any());verifyNoInteractions(jobs);
        verify(dao).updateCollectionStart(eq(saved.batchId()),eq(0),eq(actor),matches("[0-9a-f]{64}"));
        assertThatThrownBy(()->service.updateCollectionStart(auth("ADMIN"),saved.batchId(),request)).hasMessageContaining("상태");
        verify(dao,times(1)).updateCollectionJobsPending(any());
    }
    @ParameterizedTest @ValueSource(strings={"OPERATOR","APPROVER","USER","PARTNER","REVIEWER"})
    void collectionControlRequiresAdminEvenWithoutController(String role) {
        var request=new AttachmentBatchRequests.Collection(0,"a".repeat(64),1,0,83886080L,132L,"승인");
        assertThatThrownBy(()->service.updateCollectionStart(auth(role),UUID.randomUUID(),request)).isInstanceOf(ApiException.class);
        assertThatThrownBy(()->service.updateCollectionPause(auth(role),UUID.randomUUID(),new AttachmentBatchRequests.Pause(0,"중지"))).isInstanceOf(ApiException.class);
        assertThatThrownBy(()->service.updateCollectionResume(auth(role),UUID.randomUUID(),request)).isInstanceOf(ApiException.class);
        verifyNoInteractions(dao,jobs,intake,audit);
    }
    @Test void staleCapsOrPolicyPreventAllCollectionWrites() {
        service.insertBatch(auth("ADMIN"),key,request());
        assertThatThrownBy(()->service.updateCollectionStart(auth("ADMIN"),saved.batchId(),new AttachmentBatchRequests.Collection(0,saved.scopeHash(),1,0,83886081L,132L,"상한 불일치"))).hasMessageContaining("상한");
        var old=policyRow;policyRow=new AttachmentPolicyRow(policy,"RETIRED","COLLECT_ONLY",rule,"ACTIVE",old.policyHash(),old.settingsJson(),old.profileManifestJson(),1);
        assertThatThrownBy(()->service.updateCollectionStart(auth("ADMIN"),saved.batchId(),collection())).hasMessageContaining("퇴역");
        verify(dao,never()).updateCollectionStart(any(),anyInt(),any(),anyString());verify(dao,never()).updateCollectionJobsPending(any());
    }
    @Test void changedConfirmationOrLocatorBlocksCollectionDespiteUnchangedVersions() {
        service.insertBatch(auth("ADMIN"),key,request());var original=candidate;
        candidate=new AttachmentBatchRows.Candidate(source,"BIZINFO",original.contentVersionId(),original.baseEvaluationId(),rule,2,3,original.currentEvaluationId(),policy,UUID.randomUUID(),true,null);
        assertThatThrownBy(()->service.updateCollectionStart(auth("ADMIN"),saved.batchId(),collection())).hasMessageContaining("기존 검수");
        candidate=original;when(intake.selectSourceLocatorDetails(source)).thenReturn(new AttachmentWorkerSourceRow("BIZINFO","PBLN_000000000100001",null,null,null));
        assertThatThrownBy(()->service.updateCollectionStart(auth("ADMIN"),saved.batchId(),collection())).hasMessageContaining("출처 연결");
        verify(dao,never()).updateCollectionJobsPending(any());
    }
    @Test void linkedOrDeletedSourceDoesNotBecomeASmallerImplicitStartScope() {
        service.insertBatch(auth("ADMIN"),key,request());when(dao.selectFixedCandidateList(any())).thenReturn(List.of());
        assertThatThrownBy(()->service.updateCollectionStart(auth("ADMIN"),saved.batchId(),collection())).hasMessageContaining("연결 공고");
        saved=new AttachmentBatchRows.Row(saved.batchId(),policy,"SCOPE_READY",saved.scopeHash(),1,1,1,1,saved.scopeJson(),saved.policySnapshotJson(),actor,key,saved.requestHash(),now);
        assertThatThrownBy(()->service.updateCollectionStart(auth("ADMIN"),saved.batchId(),collection())).hasMessageContaining("삭제된 원문");
        verify(dao,never()).updateCollectionJobsPending(any());
    }
    @Test void pauseResumePreservesAttemptsBudgetAndTerminalJobs() {
        service.insertBatch(auth("ADMIN"),key,request());service.updateCollectionStart(auth("ADMIN"),saved.batchId(),collection());
        clearInvocations(jobs,dao);jobState="RETRY_WAIT";
        assertThat(service.updateCollectionPause(auth("ADMIN"),saved.batchId(),new AttachmentBatchRequests.Pause(saved.rowVersion(),"일시 중지")).statusCode()).isEqualTo("COLLECTION_PAUSED");
        assertThat(service.updateCollectionResume(auth("ADMIN"),saved.batchId(),collection()).jobCounts()).containsEntry("RETRY_WAIT",1L);
        verify(dao,never()).updateCollectionJobsPending(any());verifyNoInteractions(jobs);
        service.updateCollectionPause(auth("ADMIN"),saved.batchId(),new AttachmentBatchRequests.Pause(saved.rowVersion(),"완료 직전 중지"));
        jobState="FAILED";when(dao.selectFixedCandidateList(any())).thenReturn(List.of());
        assertThat(service.updateCollectionResume(auth("ADMIN"),saved.batchId(),collection()).jobCounts()).containsEntry("FAILED",1L);
    }
    @Test void resumeRevalidatesRemainingJobsAndRequiresFreshVersion() {
        service.insertBatch(auth("ADMIN"),key,request());service.updateCollectionStart(auth("ADMIN"),saved.batchId(),collection());
        var stale=collection();service.updateCollectionPause(auth("ADMIN"),saved.batchId(),new AttachmentBatchRequests.Pause(saved.rowVersion(),"중지"));
        assertThatThrownBy(()->service.updateCollectionResume(auth("ADMIN"),saved.batchId(),stale)).hasMessageContaining("버전");
        when(dao.selectFixedCandidateList(any())).thenReturn(List.of());
        assertThatThrownBy(()->service.updateCollectionResume(auth("ADMIN"),saved.batchId(),collection())).hasMessageContaining("기본 판정");
        verify(dao,never()).updateCollectionResume(any(),anyInt());
    }
    @Test void progressOnlyDelegatesBoundedDbAggregation() {
        when(dao.updateCollectionProgress()).thenReturn(2);assertThat(service.saveCollectionProgress()).isEqualTo(2);
        verify(dao).updateCollectionProgress();verifyNoMoreInteractions(dao);verifyNoInteractions(jobs,intake,audit);
    }
    @Test void previewReportsWholeCountExplicitRemainderAndZeroHttpWithoutWrites() {
        var result=service.selectScopePreview(auth("APPROVER"),scope());
        assertThat(result.candidateCount()).isEqualTo(5);assertThat(result.selectedCount()).isEqualTo(1);assertThat(result.remainingCount()).isEqualTo(4);
        assertThat(result.maximumDownloadBytes()).isEqualTo(83886080);assertThat(result.maximumHttpRequests()).isEqualTo(132);
        assertThat(result.currentHttpRequests()).isZero();assertThat(result.canReserve()).isTrue();
        assertThat(result.items()).singleElement().satisfies(i->assertThat(i.readinessCode()).isEqualTo("READY"));
        verifyNoInteractions(jobs,audit);verify(dao,never()).insertBatch(any());verify(dao,never()).selectSourceLocks(any());
    }
    @Test void reservationMaterializesExactJobsWithoutChangingCurrentSourceOrConfirmation() throws Exception {
        var result=service.insertBatch(auth("ADMIN"),key,request());
        assertThat(result.statusCode()).isEqualTo("SCOPE_READY");assertThat(result.itemCount()).isEqualTo(1);
        assertThat(items).singleElement().satisfies(i->{assertThat(i.batchId()).isEqualTo(result.batchId());assertThat(i.job().expectedAttachmentVersion()).isEqualTo(3);
            assertThat(i.job().applyToSource()).isFalse();assertThat(i.job().previousConfirmationId()).isEqualTo(candidate.confirmationId());});
        verify(jobs,never()).updateAttachmentSourceVersion(any(),anyInt());verify(jobs,never()).updateAttachmentConfirmationsStale(any());verify(jobs,never()).updateAttachmentEvaluationsStale(any());
        verify(intake,never()).updateSourceIntakeStatus(any(),anyString());
        assertThat(saved.scopeJson()).doesNotContain(source.toString(),"PBLN_", "http");
        var log=org.mockito.ArgumentCaptor.forClass(AnnouncementSourceAuditLogCommand.class);verify(audit).insertAuditLog(log.capture());
        assertThat(log.getValue().metadataJson()).doesNotContain("범위 고정 사유").contains("reasonHash");
        assertThat(mapper.writeValueAsString(result)).doesNotContain("requestedBy","idempotencyKey","requestHash","policySnapshotJson");
    }
    @Test void sameKeyDoesNotReexecuteChangedScopeAndDifferentReasonConflicts() {
        var request=request();var first=service.insertBatch(auth("ADMIN"),key,request);
        when(dao.selectCandidateList(any())).thenReturn(List.of());
        assertThat(service.insertBatch(auth("ADMIN"),key,request).batchId()).isEqualTo(first.batchId());
        assertThatThrownBy(()->service.insertBatch(auth("ADMIN"),key,new AttachmentBatchRequests.Reservation(scope(),request.expectedScopeHash(),"다른 사유"))).isInstanceOf(ApiException.class);
        verify(dao,times(1)).insertBatch(any());
    }
    @ParameterizedTest @ValueSource(strings={"OPERATOR","APPROVER","USER","PARTNER","REVIEWER"})
    void directMutationRequiresAdmin(String role) {
        assertThatThrownBy(()->service.insertBatch(auth(role),key,new AttachmentBatchRequests.Reservation(scope(),"a".repeat(64),"사유"))).isInstanceOf(ApiException.class);
        verifyNoInteractions(dao,jobs,intake,audit);
    }
    @Test void sourceAndCountChangesInvalidateScopeRatherThanSilentlyReducingIt() {
        var request=request();candidate=new AttachmentBatchRows.Candidate(source,"BIZINFO",candidate.contentVersionId(),candidate.baseEvaluationId(),rule,2,4,candidate.currentEvaluationId(),policy,candidate.confirmationId(),true,null);
        assertThatThrownBy(()->service.insertBatch(auth("ADMIN"),key,request)).hasMessageContaining("바뀌었습니다");verify(dao,never()).insertBatch(any());
    }
    @Test void missingProfileAndDifferentRuleRemainVisibleAndBlockReservation() {
        when(intake.selectSourceLocatorDetails(source)).thenReturn(null);
        var result=service.selectScopePreview(auth("ADMIN"),scope());assertThat(result.selectedCount()).isEqualTo(1);assertThat(result.canReserve()).isFalse();
        assertThat(result.items().getFirst().readinessCode()).isEqualTo("PROFILE_REQUIRED");
        assertThatThrownBy(()->service.insertBatch(auth("ADMIN"),key,new AttachmentBatchRequests.Reservation(scope(),result.scopeHash(),"사유"))).hasMessageContaining("준비 사유");
        candidate=new AttachmentBatchRows.Candidate(source,"BIZINFO",candidate.contentVersionId(),candidate.baseEvaluationId(),UUID.randomUUID(),2,3,null,null,null,false,null);
        assertThat(service.selectScopePreview(auth("ADMIN"),scope()).items().getFirst().readinessCode()).isEqualTo("BASE_RECLASSIFICATION_REQUIRED");
    }
    @Test void concurrentSourceDisappearanceOrChangedPolicyFailsWholeReservation() {
        var request=request();when(dao.selectSourceLocks(anyList())).thenReturn(List.of());
        assertThatThrownBy(()->service.insertBatch(auth("ADMIN"),key,request)).hasMessageContaining("삭제");verify(dao,never()).insertBatch(any());
    }
    @Test void changedStoredLocatorInvalidatesScopeWithoutExposingTheUrl() throws Exception {
        var request=request();
        when(intake.selectSourceLocatorDetails(source)).thenReturn(new AttachmentWorkerSourceRow("BIZINFO","PBLN_000000000100000","https://www.bizinfo.go.kr/changed",null,null));
        var changed=service.selectScopePreview(auth("ADMIN"),scope());assertThat(changed.scopeHash()).isNotEqualTo(request.expectedScopeHash());
        assertThat(mapper.writeValueAsString(changed)).doesNotContain("https://", "sourceUrl");
        assertThatThrownBy(()->service.insertBatch(auth("ADMIN"),key,request)).hasMessageContaining("바뀌었습니다");
    }
    @Test void cancellationKeepsScopeAndRequiresCurrentVersion() {
        var batch=service.insertBatch(auth("ADMIN"),key,request());
        assertThatThrownBy(()->service.updateScopeCancellation(auth("ADMIN"),batch.batchId(),new AttachmentBatchRequests.Cancellation(1,"취소"))).hasMessageContaining("현재 버전");
        var cancelled=service.updateScopeCancellation(auth("ADMIN"),batch.batchId(),new AttachmentBatchRequests.Cancellation(0,"취소"));
        assertThat(cancelled.statusCode()).isEqualTo("CANCELLED");assertThat(cancelled.scopeHash()).isEqualTo(batch.scopeHash());assertThat(cancelled.itemCount()).isEqualTo(1);
        verify(jobs,never()).updateAttachmentSourceVersion(any(),anyInt());
    }
    @Test void invalidRangesProvidersAndLimitsFailBeforeQueries() {
        for(var value:List.of(new AttachmentBatchRequests.Scope(policy,List.of("BIZINFO","BIZINFO"),now.minusDays(1),now,null,null,1),
                new AttachmentBatchRequests.Scope(policy,List.of("ARBITRARY"),now.minusDays(1),now,null,null,1),
                new AttachmentBatchRequests.Scope(policy,List.of("BIZINFO"),now,now,null,null,1),
                new AttachmentBatchRequests.Scope(policy,List.of("BIZINFO"),now.minusDays(1),now,null,null,1001)))
            assertThatThrownBy(()->service.selectScopePreview(auth("ADMIN"),value)).isInstanceOf(ApiException.class);
        verifyNoInteractions(dao,jobs,intake,audit);
    }
}
