package com.saneb.domain.announcementattachment.service.impl;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.domain.announcementattachment.dao.*;
import com.saneb.domain.announcementattachment.dto.*;
import com.saneb.domain.announcementattachment.vo.*;
import com.saneb.domain.announcementattachment.vo.AttachmentBatchRollbackRows.*;
import com.saneb.domain.announcementsource.dao.AnnouncementSourceDao;
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

class AnnouncementAttachmentBatchRollbackServiceTest {
    final AnnouncementAttachmentBatchRollbackDao dao=mock(AnnouncementAttachmentBatchRollbackDao.class);
    final AnnouncementAttachmentBatchDao batches=mock(AnnouncementAttachmentBatchDao.class);
    final AnnouncementAttachmentBatchPreviewDao previews=mock(AnnouncementAttachmentBatchPreviewDao.class);
    final AnnouncementAttachmentEvaluationDao evaluations=mock(AnnouncementAttachmentEvaluationDao.class);
    final AnnouncementSourceDao audit=mock(AnnouncementSourceDao.class);
    final PlatformTransactionManager transactions=mock(PlatformTransactionManager.class);
    final ObjectMapper mapper=new ObjectMapper().findAndRegisterModules();final AttachmentBatchFingerprint hash=new AttachmentBatchFingerprint(mapper);
    final UUID batchId=UUID.randomUUID(),jobId=UUID.randomUUID(),sourceId=UUID.randomUUID(),actorId=UUID.randomUUID(),policyId=UUID.randomUUID(),confirmation=UUID.randomUUID(),previous=UUID.randomUUID();
    final Map<UUID,Action> actions=new HashMap<>();final List<Target> targets=new ArrayList<>();
    AttachmentBatchRows.Row batch;Candidate row;AttachmentBatchPreviewRows.LiveItem live;Work work;AnnouncementAttachmentBatchRollbackServiceImpl service;
    @BeforeEach void setup() {
        batch=new AttachmentBatchRows.Row(batchId,policyId,"APPLY_PARTIAL_FAILED","a".repeat(64),100,2,1,7,"{}","{}",actorId,UUID.randomUUID(),"b".repeat(64),OffsetDateTime.now());
        live=new AttachmentBatchPreviewRows.LiveItem(jobId,sourceId,"BIZINFO","SUCCEEDED",null,true,false,false,false,"{}","{}");
        row=new Candidate(jobId,sourceId,"BIZINFO","APPLIED","NOT_REQUESTED",previous,confirmation,true,true,true,true,hash.selectHash(live),null,0,null);
        when(batches.selectBatchDetails(eq(batchId),anyBoolean())).thenAnswer(i->batch);
        when(batches.selectSourceLocks(anyList())).thenAnswer(i->i.getArgument(0));
        when(batches.selectItemList(any())).thenReturn(List.of(new AttachmentBatchRows.Item(jobId,sourceId,"BIZINFO","SUCCEEDED",0,0,null,"APPLIED","NOT_REQUESTED")));
        when(dao.selectCandidateList(batchId)).thenAnswer(i->List.of(row));when(dao.selectCandidateDetails(batchId,jobId)).thenAnswer(i->row);
        when(previews.selectLiveItemList(batchId)).thenAnswer(i->List.of(live));when(previews.selectLiveItemDetails(batchId,jobId)).thenAnswer(i->live);
        when(dao.selectRequestDetails(any())).thenAnswer(i->actions.values().stream().filter(a->a.key().equals(i.getArgument(0))).findFirst().orElse(null));
        when(dao.selectActionDetails(eq(batchId),any())).thenAnswer(i->actions.get(i.getArgument(1)));
        when(dao.insertAction(any())).thenAnswer(i->{Action a=i.getArgument(0);actions.put(a.id(),a);return 1;});
        when(dao.insertTarget(any())).thenAnswer(i->{targets.add(i.getArgument(0));return 1;});
        when(dao.updateStart(eq(batchId),anyInt(),any())).thenAnswer(i->{batch=new AttachmentBatchRows.Row(batchId,policyId,"ROLLING_BACK",batch.scopeHash(),100,2,1,batch.rowVersion()+1,"{}","{}",actorId,batch.idempotencyKey(),batch.requestHash(),batch.createdAt());return 1;});
        when(dao.updateJobsPending(eq(batchId),any())).thenReturn(1);when(dao.selectCounts(any())).thenReturn(new Counts(1,1,0,0,0));
        when(dao.selectWorkDetails(batchId,jobId)).thenAnswer(i->work);when(dao.selectNextWorkDetails()).thenAnswer(i->work);
        when(dao.insertConfirmationRestoration(any())).thenReturn(1);when(dao.updateSourceRestoration(jobId)).thenReturn(1);
        when(dao.updateConfirmationCurrent(jobId)).thenReturn(1);when(dao.updateRolledBack(eq(jobId),anyBoolean())).thenReturn(1);
        when(dao.updateConflict(eq(jobId),anyString())).thenReturn(1);when(dao.updateProgress(batchId)).thenReturn(1);when(dao.updateFailure(jobId)).thenReturn(1);
        when(evaluations.updateEvaluationCurrent(previous,sourceId)).thenReturn(1);when(transactions.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        service=new AnnouncementAttachmentBatchRollbackServiceImpl(dao,batches,previews,evaluations,audit,mapper,transactions);
    }
    Authentication auth(String role){var user=new AuthenticatedUserDetails(new AuthUserDetailsRow(actorId,"qa","unused","QA","ACTIVE",false,null,null,null),List.of(role));return UsernamePasswordAuthenticationToken.authenticated(user,null,user.getAuthorities());}
    AttachmentBatchRollbackRequest request(){var p=service.selectPreviewDetails(auth("ADMIN"),batchId);return new AttachmentBatchRollbackRequest(p.version(),p.previewHash(),p.scopeCount(),p.targetCount(),p.deletedCount(),p.baseReopenCount(),p.confirmationRestoreCount(),p.cancelPendingCount(),true,"원복 영향 승인");}
    void queue(){var result=service.insertRollback(auth("ADMIN"),batchId,UUID.randomUUID(),request());var t=targets.getFirst();work=new Work(result.actionId(),batchId,jobId,sourceId,actorId,t.inputHash(),t.readinessCode(),t.eligible(),t.confirmationRestores());clearInvocations(dao,batches,evaluations,audit);}
    @Test void previewHasNoWritesAndKeepsDeletedScopeAndConfirmationEffect(){var p=service.selectPreviewDetails(auth("APPROVER"),batchId);assertThat(p.scopeCount()).isEqualTo(2);assertThat(p.deletedCount()).isEqualTo(1);assertThat(p.targetCount()).isEqualTo(1);assertThat(p.confirmationRestoreCount()).isEqualTo(1);assertThat(p.currentHttpRequests()).isZero();verify(dao,never()).insertAction(any());verifyNoInteractions(evaluations,audit);}
    @Test void approvalQueuesWithoutRestoringAndSameKeyNeverRequeues(){UUID key=UUID.randomUUID();var r=request();var first=service.insertRollback(auth("ADMIN"),batchId,key,r);assertThat(first.statusCode()).isEqualTo("ROLLING_BACK");assertThat(first.rolledBackCount()).isZero();assertThat(service.insertRollback(auth("ADMIN"),batchId,key,r).actionId()).isEqualTo(first.actionId());verify(dao,times(1)).updateStart(any(),anyInt(),any());verify(dao,never()).updateSourceRestoration(any());}
    @Test void changedImpactAndFalseAcknowledgementRejectBeforeApproval(){var r=request();var wrong=new AttachmentBatchRollbackRequest(r.expectedVersion(),r.expectedPreviewHash(),2,1,1,1,1,0,true,"변경된 영향");assertThatThrownBy(()->service.insertRollback(auth("ADMIN"),batchId,UUID.randomUUID(),wrong)).hasMessageContaining("영향");var unchecked=new AttachmentBatchRollbackRequest(r.expectedVersion(),r.expectedPreviewHash(),2,1,1,0,1,0,false,"원복");assertThatThrownBy(()->service.insertRollback(auth("ADMIN"),batchId,UUID.randomUUID(),unchecked)).hasMessageContaining("확인");verify(dao,never()).insertAction(any());}
    @Test void foreignActorCannotReuseApprovedKey(){UUID key=UUID.randomUUID();var r=request();service.insertRollback(auth("ADMIN"),batchId,key,r);var user=new AuthenticatedUserDetails(new AuthUserDetailsRow(UUID.randomUUID(),"other","unused","QA","ACTIVE",false,null,null,null),List.of("ADMIN"));assertThatThrownBy(()->service.insertRollback(UsernamePasswordAuthenticationToken.authenticated(user,null,user.getAuthorities()),batchId,key,r)).hasMessageContaining("멱등 키");}
    @ParameterizedTest @ValueSource(strings={"OPERATOR","APPROVER","USER","PARTNER"}) void onlyAdminApprovesEvenViaDirectService(String role){var r=request();clearInvocations(dao);assertThatThrownBy(()->service.insertRollback(auth(role),batchId,UUID.randomUUID(),r)).hasMessageContaining("ADMIN");verifyNoInteractions(dao);}
    @Test void workerRecordsRestorationBeforeSourceCasAndKeepsOriginalReview(){queue();assertThat(service.saveNextRollback()).isTrue();var order=inOrder(dao,evaluations);order.verify(dao).insertConfirmationRestoration(any());order.verify(dao).updateSourceRestoration(jobId);order.verify(evaluations).updatePreviousEvaluationsStale(sourceId);order.verify(evaluations).updateEvaluationCurrent(previous,sourceId);order.verify(dao).updateConfirmationCurrent(jobId);order.verify(dao).updateRolledBack(jobId,true);verify(evaluations,never()).updatePreviousConfirmationsStale(any());}
    @Test void changedLiveInputAfterApprovalConflictsWithoutSourceWrite(){queue();live=new AttachmentBatchPreviewRows.LiveItem(jobId,sourceId,"BIZINFO","SUCCEEDED",null,true,false,false,false,"changed","{}");assertThat(service.saveNextRollback()).isTrue();verify(dao).updateConflict(jobId,"ROLLBACK_INPUT_CHANGED");verify(dao,never()).updateSourceRestoration(any());verify(dao,never()).insertConfirmationRestoration(any());}
    @ParameterizedTest @ValueSource(strings={"binding","previous","link","job"}) void changedBindingsAndProtectedWorkNeverRestore(String kind){queue();if(kind.equals("link")||kind.equals("job"))live=new AttachmentBatchPreviewRows.LiveItem(jobId,sourceId,"BIZINFO","SUCCEEDED",null,true,false,kind.equals("link"),kind.equals("job"),"{}","{}");else row=new Candidate(jobId,sourceId,"BIZINFO","APPLIED","NOT_REQUESTED",previous,confirmation,true,!kind.equals("binding"),!kind.equals("previous"),true,row.appliedInputHash(),null,0,null);service.saveNextRollback();verify(dao).updateConflict(eq(jobId),anyString());verify(dao,never()).updateSourceRestoration(any());}
    @Test void noPreviousConfirmationRestoresBaseWithoutInventingReview(){row=new Candidate(jobId,sourceId,"BIZINFO","APPLIED","NOT_REQUESTED",null,null,false,true,true,false,row.appliedInputHash(),null,0,null);assertThat(service.selectPreviewDetails(auth("ADMIN"),batchId).baseReopenCount()).isEqualTo(1);queue();service.saveNextRollback();verify(dao,never()).insertConfirmationRestoration(any());verify(dao,never()).updateConfirmationCurrent(any());verify(evaluations,never()).updateEvaluationCurrent(any(),any());verify(dao).updateRolledBack(jobId,false);}
    @Test void stalePriorConfirmationRemainsStale(){row=new Candidate(jobId,sourceId,"BIZINFO","APPLIED","NOT_REQUESTED",previous,confirmation,true,true,true,false,row.appliedInputHash(),null,0,null);assertThat(service.selectPreviewDetails(auth("ADMIN"),batchId).staleConfirmationCount()).isEqualTo(1);queue();service.saveNextRollback();verify(dao,never()).insertConfirmationRestoration(any());verify(dao).updateRolledBack(jobId,false);}
    @Test void transactionFailureRecordsBoundedRetryWithoutFakeCompletion(){queue();when(dao.updateSourceRestoration(jobId)).thenReturn(0);assertThatThrownBy(()->service.saveNextRollback()).hasMessageContaining("취소");verify(transactions).rollback(any());verify(dao).updateFailure(jobId);verify(dao,never()).updateRolledBack(any(),anyBoolean());}
    @Test void retryMetadataDoesNotInvalidateApprovedInput(){queue();row=new Candidate(jobId,sourceId,"BIZINFO","APPLIED","NOT_REQUESTED",previous,confirmation,true,true,true,true,row.appliedInputHash(),"ROLLBACK_TRANSACTION_FAILED",1,OffsetDateTime.now().minusSeconds(1));service.saveNextRollback();verify(dao).updateRolledBack(jobId,true);}
    @Test void deletedSourceAndAlreadyCompletedWorkDoNotRestoreTwice(){queue();when(batches.selectSourceLocks(List.of(sourceId))).thenReturn(List.of());assertThat(service.saveNextRollback()).isFalse();verify(dao,never()).updateSourceRestoration(any());when(batches.selectSourceLocks(anyList())).thenAnswer(i->i.getArgument(0));when(dao.selectWorkDetails(batchId,jobId)).thenReturn(null);assertThat(service.saveNextRollback()).isFalse();verify(dao,never()).updateSourceRestoration(any());}
    @Test void emptyQueueReconcilesDeletedBatchesWithoutCallingSources(){assertThat(service.saveNextRollback()).isFalse();verify(dao).updateEmptyProgress();verify(batches,never()).selectSourceLocks(anyList());}
}
