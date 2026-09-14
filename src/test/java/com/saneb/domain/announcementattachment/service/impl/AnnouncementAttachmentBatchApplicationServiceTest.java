package com.saneb.domain.announcementattachment.service.impl;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.common.error.ApiException;
import com.saneb.domain.announcementattachment.dao.*;
import com.saneb.domain.announcementattachment.dto.*;
import com.saneb.domain.announcementattachment.vo.*;
import com.saneb.domain.announcementsource.dao.AnnouncementSourceDao;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceAuditLogCommand;
import com.saneb.domain.auth.vo.*;
import java.time.OffsetDateTime;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

class AnnouncementAttachmentBatchApplicationServiceTest {
    final AnnouncementAttachmentBatchApplicationDao dao=mock(AnnouncementAttachmentBatchApplicationDao.class);
    final AnnouncementAttachmentBatchDao batches=mock(AnnouncementAttachmentBatchDao.class);
    final AnnouncementAttachmentBatchPreviewDao previews=mock(AnnouncementAttachmentBatchPreviewDao.class);
    final AnnouncementAttachmentEvaluationDao evaluations=mock(AnnouncementAttachmentEvaluationDao.class);
    final AnnouncementSourceDao audit=mock(AnnouncementSourceDao.class);
    final PlatformTransactionManager transactions=mock(PlatformTransactionManager.class);
    final ObjectMapper mapper=new ObjectMapper().findAndRegisterModules();
    final UUID batchId=UUID.randomUUID(),previewId=UUID.randomUUID(),jobId=UUID.randomUUID(),sourceId=UUID.randomUUID(),actor=UUID.randomUUID(),policyId=UUID.randomUUID(),rule=UUID.randomUUID(),evaluationId=UUID.randomUUID();
    final AttachmentBatchFingerprint hashes=new AttachmentBatchFingerprint(mapper);
    final Map<UUID,AttachmentBatchApplicationRows.Action> actions=new HashMap<>();
    AttachmentBatchRows.Row batch;AttachmentPolicyRow policy;AttachmentBatchPreviewRows.Preview preview;AttachmentBatchPreviewRows.LiveItem live;
    AttachmentBatchApplicationRows.Work work;AnnouncementAttachmentBatchApplicationServiceImpl service;
    @BeforeEach void setup() throws Exception {
        policy=new AttachmentPolicyRow(policyId,"ACTIVE","ENFORCE",rule,"ACTIVE","a".repeat(64),"{}","[]",0);
        batch=new AttachmentBatchRows.Row(batchId,policyId,"PREVIEW_PARTIAL_FAILED","b".repeat(64),100,2,1,8,"{}",mapper.writeValueAsString(policy),actor,UUID.randomUUID(),"c".repeat(64),OffsetDateTime.now());
        live=new AttachmentBatchPreviewRows.LiveItem(jobId,sourceId,"BIZINFO","SUCCEEDED",null,true,true,false,false,"{\"version\":0}","{\"fileCount\":0}");
        refreshPreview();work=new AttachmentBatchApplicationRows.Work(batchId,jobId,sourceId,policyId,evaluationId,previewId,hashes.selectHash(live),actor);
        when(batches.selectBatchDetails(eq(batchId),anyBoolean())).thenAnswer(c->batch);
        when(batches.selectPolicyDetails(eq(policyId),anyBoolean())).thenAnswer(c->policy);
        when(batches.selectSourceLocks(anyList())).thenAnswer(c->c.getArgument(0));
        when(batches.selectItemList(any())).thenReturn(List.of(new AttachmentBatchRows.Item(jobId,sourceId,"BIZINFO","SUCCEEDED",0,0,null,"NOT_REQUESTED","NOT_REQUESTED")));
        when(previews.selectCurrentPreviewDetails(batchId)).thenAnswer(c->preview);
        when(previews.selectLiveItemList(batchId)).thenAnswer(c->List.of(live));
        when(previews.selectLiveItemDetails(batchId,jobId)).thenAnswer(c->live);
        when(previews.selectItemList(any())).thenReturn(List.of(new AttachmentBatchPreviewRows.Item(jobId,sourceId,"BIZINFO","READY",true,true,hashes.selectHash(live),live.evidenceJson())));
        when(dao.selectRequestDetails(any())).thenAnswer(c->actions.values().stream().filter(a->a.key().equals(c.getArgument(0))).findFirst().orElse(null));
        when(dao.selectActionDetails(eq(batchId),any())).thenAnswer(c->actions.get(c.getArgument(1)));
        when(dao.insertAction(any())).thenAnswer(c->{var a=(AttachmentBatchApplicationRows.Action)c.getArgument(0);actions.put(a.id(),a);return 1;});
        when(dao.updateStart(eq(batchId),anyInt(),any())).thenAnswer(c->{saveBatch("APPLYING");return 1;});
        when(dao.updatePause(eq(batchId),anyInt())).thenAnswer(c->{saveBatch("APPLY_PAUSED");return 1;});
        when(dao.updateResume(eq(batchId),anyInt())).thenAnswer(c->{saveBatch("APPLYING");return 1;});
        when(dao.updateJobsPending(batchId,previewId)).thenReturn(1);
        when(dao.selectCounts(batchId)).thenReturn(new AttachmentBatchApplicationRows.Counts(1,1,0,0,0,1));
        when(dao.selectWorkDetails(batchId,jobId)).thenAnswer(c->work);
        when(dao.updateSourceApplication(jobId)).thenReturn(1);when(dao.updateApplied(eq(jobId),anyString())).thenReturn(1);
        when(dao.updateConflict(eq(jobId),anyString())).thenReturn(1);when(dao.updateProgress(batchId)).thenReturn(1);when(dao.updateFailure(jobId)).thenReturn(1);
        when(evaluations.updateEvaluationCurrent(evaluationId,sourceId)).thenReturn(1);
        when(transactions.getTransaction(any())).thenAnswer(c->new SimpleTransactionStatus());
        service=new AnnouncementAttachmentBatchApplicationServiceImpl(dao,batches,previews,evaluations,audit,mapper,transactions);
    }
    void refreshPreview() {preview=new AttachmentBatchPreviewRows.Preview(previewId,batchId,"PREVIEW_PARTIAL_FAILED",batch.scopeHash(),
            hashes.selectHash(Arrays.asList("attachment-batch-preview-input-v1",batch.scopeHash(),batch.itemCount(),batch.deletedItemCount(),policy,List.of(live))),
            "d".repeat(64),batch.rowVersion(),2,1,1,1,1,actor,UUID.randomUUID(),"e".repeat(64),"f".repeat(64),OffsetDateTime.now());}
    void saveBatch(String state) {batch=new AttachmentBatchRows.Row(batchId,policyId,state,batch.scopeHash(),100,2,1,batch.rowVersion()+1,"{}",batch.policySnapshotJson(),actor,batch.idempotencyKey(),batch.requestHash(),batch.createdAt());}
    Authentication auth(String role) {var user=new AuthenticatedUserDetails(new AuthUserDetailsRow(actor,"fixture","unused","QA","ACTIVE",false,null,null,null),List.of(role));return UsernamePasswordAuthenticationToken.authenticated(user,null,user.getAuthorities());}
    AttachmentBatchApplicationRequest request() {return new AttachmentBatchApplicationRequest(batch.rowVersion(),previewId,preview.previewHash(),2,1,1,true,"명시적 적용 승인");}
    void start() {service.insertAction(auth("ADMIN"),batchId,UUID.randomUUID(),"START",request());}
    void queue() {start();clearInvocations(dao,batches,audit,evaluations);when(dao.selectNextWorkDetails()).thenReturn(work);}
    @Test void startOnlyQueuesExactSelectionAndKeepsFullScopeWithoutSourceWrites() {
        var result=service.insertAction(auth("ADMIN"),batchId,UUID.randomUUID(),"START",request());
        assertThat(result.currentStatusCode()).isEqualTo("APPLYING");assertThat(result.scopeItemCount()).isEqualTo(2);assertThat(result.deletedItemCount()).isEqualTo(1);
        assertThat(result.approvedSelectedCount()).isEqualTo(1);assertThat(result.appliedCount()).isZero();assertThat(result.currentHttpRequests()).isZero();
        verify(dao).updateJobsPending(batchId,previewId);verify(dao,never()).updateSourceApplication(any());verifyNoInteractions(evaluations);
    }
    @Test void sameKeyAfterPauseReturnsReceiptWithoutRestartingAndDifferentPayloadConflicts() {
        UUID key=UUID.randomUUID();var original=request();var receipt=service.insertAction(auth("ADMIN"),batchId,key,"START",original);
        service.insertAction(auth("ADMIN"),batchId,UUID.randomUUID(),"PAUSE",request());clearInvocations(dao);
        var again=service.insertAction(auth("ADMIN"),batchId,key,"START",original);assertThat(again.actionId()).isEqualTo(receipt.actionId());assertThat(again.currentStatusCode()).isEqualTo("APPLY_PAUSED");
        verify(dao,never()).updateStart(any(),anyInt(),any());assertThatThrownBy(()->service.insertAction(auth("ADMIN"),batchId,key,"RESUME",request())).hasMessageContaining("멱등 키");
    }
    @Test void pauseAndResumeDoNotResetSelectionsResultsOrBudgets() {
        start();clearInvocations(dao);service.insertAction(auth("ADMIN"),batchId,UUID.randomUUID(),"PAUSE",request());
        assertThat(batch.statusCode()).isEqualTo("APPLY_PAUSED");service.insertAction(auth("ADMIN"),batchId,UUID.randomUUID(),"RESUME",request());
        assertThat(batch.statusCode()).isEqualTo("APPLYING");verify(dao,never()).updateJobsPending(any(),any());verify(dao,never()).updateSourceApplication(any());
    }
    @Test void changedInputCannotBeApproved() {
        live=new AttachmentBatchPreviewRows.LiveItem(jobId,sourceId,"BIZINFO","SUCCEEDED",null,true,false,false,false,"changed","{}");
        assertThatThrownBy(()->start()).hasMessageContaining("근거 입력");verify(dao,never()).insertAction(any());
    }
    @Test void collectOnlyOrRetiredPolicyCannotBePromotedByApplication() {
        policy=new AttachmentPolicyRow(policyId,"ACTIVE","COLLECT_ONLY",rule,"ACTIVE","a".repeat(64),"{}","[]",0);
        assertThatThrownBy(()->start()).hasMessageContaining("ENFORCE");verify(dao,never()).updateJobsPending(any(),any());
    }
    @Test void wrongVersionCountsPreviewOrReviewAcknowledgementAreRejected() {
        var r=request();assertThatThrownBy(()->service.insertAction(auth("ADMIN"),batchId,UUID.randomUUID(),"START",new AttachmentBatchApplicationRequest(r.expectedVersion(),previewId,r.expectedPreviewHash(),2,1,0,true,"승인"))).hasMessageContaining("건수");
        assertThatThrownBy(()->service.insertAction(auth("ADMIN"),batchId,UUID.randomUUID(),"START",new AttachmentBatchApplicationRequest(r.expectedVersion(),previewId,r.expectedPreviewHash(),2,1,1,false,"승인"))).hasMessageContaining("다시 검수");
        assertThatThrownBy(()->service.insertAction(auth("ADMIN"),batchId,UUID.randomUUID(),"START",new AttachmentBatchApplicationRequest(r.expectedVersion()-1,previewId,r.expectedPreviewHash(),2,1,1,true,"승인"))).hasMessageContaining("버전");
        verify(dao,never()).insertAction(any());
    }
    @Test void itemAppliesOnceWithPreviousConfirmationStaleAndRecoveryFingerprint() {
        queue();assertThat(service.saveNextApplication()).isTrue();
        var ordered=inOrder(dao,evaluations);ordered.verify(dao).updateSourceApplication(jobId);ordered.verify(evaluations).updatePreviousConfirmationsStale(sourceId);
        ordered.verify(evaluations).updatePreviousEvaluationsStale(sourceId);ordered.verify(evaluations).updateEvaluationCurrent(evaluationId,sourceId);
        ordered.verify(dao).updateApplied(jobId,hashes.selectHash(live));ordered.verify(dao).updateProgress(batchId);
        when(dao.selectWorkDetails(batchId,jobId)).thenReturn(null);clearInvocations(dao,evaluations);
        assertThat(service.saveNextApplication()).isFalse();verify(dao,never()).updateSourceApplication(any());verifyNoInteractions(evaluations);
    }
    @Test void itemChangedAfterApprovalBecomesConflictWithoutAnyCurrentWrites() {
        queue();live=new AttachmentBatchPreviewRows.LiveItem(jobId,sourceId,"BIZINFO","SUCCEEDED",null,true,false,false,false,"changed","{}");
        assertThat(service.saveNextApplication()).isTrue();verify(dao).updateConflict(jobId,"APPLICATION_INPUT_CHANGED");verify(dao,never()).updateSourceApplication(any());verifyNoInteractions(evaluations);
    }
    @Test void policyChangedAfterApprovalBecomesConflictAndDoesNotLeavePendingForever() {
        queue();policy=new AttachmentPolicyRow(policyId,"RETIRED","ENFORCE",rule,"ACTIVE","a".repeat(64),"{}","[]",1);
        service.saveNextApplication();verify(dao).updateConflict(jobId,"APPLICATION_POLICY_CHANGED");verify(dao,never()).updateSourceApplication(any());
    }
    @Test void protectedLinkOrOtherActiveJobPreventsApplication() {
        queue();live=new AttachmentBatchPreviewRows.LiveItem(jobId,sourceId,"BIZINFO","SUCCEEDED",null,true,true,true,true,live.inputJson(),live.evidenceJson());
        service.saveNextApplication();verify(dao).updateConflict(jobId,"APPLICATION_INPUT_CHANGED");verifyNoInteractions(evaluations);
    }
    @Test void itemMetadataHashChangeEvenWithReadyFlagsIsConflict() {
        queue();live=new AttachmentBatchPreviewRows.LiveItem(jobId,sourceId,"BIZINFO","SUCCEEDED",null,true,true,false,false,"changed",live.evidenceJson());
        service.saveNextApplication();verify(dao).updateConflict(jobId,"APPLICATION_PREVIEW_CHANGED");verifyNoInteractions(evaluations);
    }
    @Test void pauseWhileWorkerWasWaitingDoesNotApply() {
        queue();saveBatch("APPLY_PAUSED");assertThat(service.saveNextApplication()).isFalse();verify(dao,never()).updateSourceApplication(any());
    }
    @Test void sourceDeletionDoesNotReconstructSourceOrApplyDifferentJob() {
        queue();when(batches.selectSourceLocks(List.of(sourceId))).thenReturn(List.of());assertThat(service.saveNextApplication()).isFalse();verify(dao,never()).updateSourceApplication(any());
    }
    @Test void finalSourceCasConflictIsTerminalWithoutRetryOrStalingConfirmation() {
        queue();when(dao.updateSourceApplication(jobId)).thenReturn(0);assertThat(service.saveNextApplication()).isTrue();
        verify(dao).updateConflict(jobId,"APPLICATION_SOURCE_CAS_CONFLICT");verify(dao,never()).updateFailure(any());verifyNoInteractions(evaluations);
    }
    @Test void transactionFailureRollsBackBeforeRecordingBoundedRetry() {
        queue();doThrow(new IllegalStateException("fixture failure")).when(evaluations).updateEvaluationCurrent(evaluationId,sourceId);
        assertThatThrownBy(()->service.saveNextApplication()).isInstanceOf(IllegalStateException.class);
        var ordered=inOrder(transactions,dao);ordered.verify(transactions).rollback(any());ordered.verify(dao).updateFailure(jobId);verify(dao,never()).updateApplied(any(),anyString());
    }
    @Test void emptyQueueOnlyReconcilesDatabaseCounts() {assertThat(service.saveNextApplication()).isFalse();verify(dao).updateEmptyProgress();verifyNoInteractions(evaluations);}
    @ParameterizedTest @ValueSource(strings={"OPERATOR","APPROVER","USER","PARTNER","REVIEWER"})
    void nonAdminCannotApproveApply(String role) {assertThatThrownBy(()->service.insertAction(auth(role),batchId,UUID.randomUUID(),"START",request())).isInstanceOf(ApiException.class);verifyNoInteractions(dao);}
    @Test void pagedItemsKeepCollectionAndApplicationErrorsSeparateAndEnforceLimits() {
        when(dao.selectItemList(any())).thenReturn(List.of(new AttachmentBatchApplicationRows.Item(jobId,sourceId,"SUCCEEDED",true,"CONFLICT","APPLICATION_INPUT_CHANGED",0,null,null,null,null,"NOT_REQUESTED")));
        var result=service.selectItemList(auth("OPERATOR"),batchId,1,20);assertThat(result.totalCount()).isEqualTo(1);
        assertThat(result.items().getFirst().collectionStatusCode()).isEqualTo("SUCCEEDED");assertThat(result.items().getFirst().applicationErrorCode()).isEqualTo("APPLICATION_INPUT_CHANGED");
        assertThatThrownBy(()->service.selectItemList(auth("ADMIN"),batchId,1,101)).hasMessageContaining("1~100");
    }
    @Test void auditAndReceiptDoNotContainRawReasonOrSourceMetadata() {
        var receipt=service.insertAction(auth("ADMIN"),batchId,UUID.randomUUID(),"START",request());var captor=org.mockito.ArgumentCaptor.forClass(AnnouncementSourceAuditLogCommand.class);
        verify(audit).insertAuditLog(captor.capture());assertThat(captor.getValue().metadataJson()).contains("reasonHash","selectedCount").doesNotContain("명시적 적용 승인",sourceId.toString(),"inputJson");
        assertThat(receipt.actionId()).isNotNull();
    }
}
