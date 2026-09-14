package com.saneb.domain.announcementattachment.service.impl;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.domain.announcementattachment.dao.*;
import com.saneb.domain.announcementattachment.dto.*;
import com.saneb.domain.announcementattachment.vo.AttachmentNormalRollbackRows.*;
import com.saneb.domain.announcementsource.dao.AnnouncementSourceDao;
import com.saneb.domain.auth.vo.*;
import java.time.OffsetDateTime;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

class AnnouncementAttachmentNormalRollbackServiceTest {
    final AnnouncementAttachmentNormalRollbackDao dao=mock(AnnouncementAttachmentNormalRollbackDao.class);
    final AnnouncementAttachmentEvaluationDao evaluations=mock(AnnouncementAttachmentEvaluationDao.class);
    final AnnouncementSourceDao audit=mock(AnnouncementSourceDao.class);
    final ObjectMapper mapper=new ObjectMapper().findAndRegisterModules();
    final UUID source=UUID.randomUUID(),job=UUID.randomUUID(),actorId=UUID.randomUUID(),previous=UUID.randomUUID(),confirmation=UUID.randomUUID();
    final Map<UUID,Action> actions=new HashMap<>();State state;AnnouncementAttachmentNormalRollbackServiceImpl service;
    @BeforeEach void setup() throws Exception {
        state=state("APPLIED","READY",false,true,false,previous,confirmation);
        when(dao.selectStateJson(source,job)).thenAnswer(i->mapper.writeValueAsString(state));
        when(dao.selectSourceLock(source)).thenReturn(source);when(dao.selectJobLock(source,job)).thenReturn(job);
        when(dao.selectRequestDetails(any())).thenAnswer(i->actions.values().stream().filter(a->a.key().equals(i.getArgument(0))).findFirst().orElse(null));
        when(dao.insertAction(any())).thenAnswer(i->{Action a=i.getArgument(0);actions.put(a.id(),new Action(a.id(),a.jobId(),a.sourceId(),a.modeCode(),a.expectedSourceVersion(),a.expectedAttachmentVersion(),a.previewHash(),a.baseReopens(),a.confirmationRestores(),a.actorId(),a.key(),a.requestHash(),a.reasonHash(),OffsetDateTime.now()));return 1;});
        when(dao.selectActionDetails(eq(source),eq(job),any())).thenAnswer(i->actions.get(i.getArgument(2)));
        when(dao.updateSourceRestoration(any())).thenReturn(1);when(dao.updateConfirmationCurrent(any())).thenReturn(1);when(dao.updateRolledBack(any())).thenReturn(1);
        when(evaluations.updateEvaluationCurrent(previous,source)).thenReturn(1);
        service=new AnnouncementAttachmentNormalRollbackServiceImpl(dao,evaluations,audit,mapper);
    }
    State state(String mode,String readiness,boolean base,boolean restored,boolean stale,UUID oldEval,UUID oldConfirmation){return new State(job,source,4,mode.equals("APPLIED")?"SUCCEEDED":"FAILED",mode.equals("APPLIED")?"APPLIED":"PENDING","NOT_REQUESTED",mode,0,2,oldEval,oldConfirmation,readiness,base,restored,stale,"b".repeat(64),"a".repeat(64));}
    Authentication auth(String role){return auth(actorId,role,"ACTIVE",false);}
    Authentication auth(UUID id,String role,String status,boolean reset){var user=new AuthenticatedUserDetails(new AuthUserDetailsRow(id,"qa","unused","QA",status,reset,null,null,null),List.of(role));return UsernamePasswordAuthenticationToken.authenticated(user,null,user.getAuthorities());}
    AttachmentNormalRollbackRequest request(){return new AttachmentNormalRollbackRequest(0,2,"a".repeat(64),state.baseReopens(),state.confirmationRestores(),true,"이전 연결 복원 승인");}
    @ParameterizedTest @ValueSource(strings={"ADMIN","OPERATOR","APPROVER"})
    void historyUsesSourceScopeAndBoundedPageWithoutPreparingOrWriting(String role) {
        when(dao.selectVisibleSourceDetails(source)).thenReturn(source);
        var row=new AttachmentNormalRollbackResponses.JobSummary(source,job,"COLLECT","FAILED","PENDING","ROLLED_BACK",UUID.randomUUID(),OffsetDateTime.now());
        when(dao.selectJobList(source,10,10)).thenReturn(List.of(row));when(dao.selectJobCount(source)).thenReturn(11L);
        var page=service.selectJobList(auth(role),source,2,10);
        assertThat(page.items()).containsExactly(row);assertThat(page.totalPages()).isEqualTo(2);assertThat(page.totalCount()).isEqualTo(11);
        verify(dao,never()).selectStateJson(any(),any());verify(dao,never()).insertAction(any());verifyNoInteractions(evaluations,audit);
    }
    @Test void historyRejectsInvalidPagingAndHiddenSourceBeforeListing() {
        for(int[] p:List.of(new int[]{0,10},new int[]{1000001,10},new int[]{1,0},new int[]{1,101}))
            assertThatThrownBy(()->service.selectJobList(auth("ADMIN"),source,p[0],p[1])).hasMessageContaining("페이지");
        verifyNoInteractions(dao);
        assertThatThrownBy(()->service.selectJobList(auth("ADMIN"),source,1,10)).hasMessageContaining("찾을 수");
        verify(dao,never()).selectJobList(any(),anyInt(),anyInt());
    }
    @Test void historyChecksActiveReadRoleAtTheServiceBoundary() {
        assertThatThrownBy(()->service.selectJobList(auth("USER"),source,1,10)).hasMessageContaining("활성");
        assertThatThrownBy(()->service.selectJobList(auth(actorId,"ADMIN","ACTIVE",true),source,1,10)).hasMessageContaining("활성");
        verifyNoInteractions(dao);
    }
    @Test void previewDoesNotWriteOrExposeInputAndPreservesFailureMode(){state=state("FAILED_RESERVATION","READY",true,false,false,null,null);var p=service.selectPreviewDetails(auth("APPROVER"),source,job);assertThat(p.jobStatusCode()).isEqualTo("FAILED");assertThat(p.baseReopens()).isTrue();assertThat(p.targetCount()).isEqualTo(1);assertThat(p.currentHttpRequests()).isZero();verify(dao,never()).insertAction(any());verifyNoInteractions(evaluations,audit);}
    @Test void explicitApprovalRestoresSourceEvaluationAndConfirmationBeforeReceipt(){var receipt=service.insertRollback(auth("ADMIN"),source,job,UUID.randomUUID(),request());assertThat(receipt.statusCode()).isEqualTo("ROLLED_BACK");assertThat(receipt.restoredAttachmentVersion()).isEqualTo(3);assertThat(receipt.confirmationRestored()).isTrue();var order=inOrder(dao,evaluations);order.verify(dao).selectSourceLock(source);order.verify(dao).selectJobLock(source,job);order.verify(dao).insertAction(any());order.verify(dao).updateSourceRestoration(any());order.verify(evaluations).updatePreviousEvaluationsStale(source);order.verify(evaluations).updateEvaluationCurrent(previous,source);order.verify(dao).updateConfirmationCurrent(any());order.verify(dao).updateRolledBack(any());verify(evaluations,never()).updatePreviousConfirmationsStale(any());}
    @Test void failedReservationRestoresBaseWithoutPretendingCollectionSucceeded(){state=state("FAILED_RESERVATION","READY",true,false,false,null,null);var r=service.insertRollback(auth("ADMIN"),source,job,UUID.randomUUID(),request());assertThat(r.modeCode()).isEqualTo("FAILED_RESERVATION");assertThat(r.baseReopened()).isTrue();verify(evaluations,never()).updateEvaluationCurrent(any(),any());verify(dao,never()).updateConfirmationCurrent(any());}
    @Test void stalePreviousConfirmationIsNotRevived(){state=state("APPLIED","READY",false,false,true,previous,confirmation);service.insertRollback(auth("ADMIN"),source,job,UUID.randomUUID(),request());verify(dao,never()).updateConfirmationCurrent(any());verify(evaluations).updateEvaluationCurrent(previous,source);}
    @Test void sameKeyReturnsOriginalReceiptWithoutReapplyingAfterCurrentChanges(){UUID key=UUID.randomUUID();var request=request();var r=service.insertRollback(auth("ADMIN"),source,job,key,request);state=state("APPLIED","ALREADY_RECOVERED",false,false,false,previous,confirmation);assertThat(service.insertRollback(auth("ADMIN"),source,job,key,request)).isEqualTo(r);verify(dao,times(1)).updateSourceRestoration(any());}
    @Test void reusedKeyWithOtherActorOrPayloadIsRejected(){UUID key=UUID.randomUUID();var request=request();service.insertRollback(auth("ADMIN"),source,job,key,request);assertThatThrownBy(()->service.insertRollback(auth(UUID.randomUUID(),"ADMIN","ACTIVE",false),source,job,key,request)).hasMessageContaining("멱등 키");var changed=new AttachmentNormalRollbackRequest(0,2,"a".repeat(64),false,true,true,"다른 사유");assertThatThrownBy(()->service.insertRollback(auth("ADMIN"),source,job,key,changed)).hasMessageContaining("멱등 키");}
    @ParameterizedTest @ValueSource(strings={"CURRENT_BINDING_CHANGED","PREVIOUS_BINDING_INVALID","JOB_NOT_TERMINAL","RECOVERY_EVIDENCE_MISSING","ALREADY_RECOVERED"}) void unsafeStateNeverWrites(String reason){state=state("APPLIED",reason,false,true,false,previous,confirmation);assertThatThrownBy(()->service.insertRollback(auth("ADMIN"),source,job,UUID.randomUUID(),request())).isInstanceOf(com.saneb.common.error.ApiException.class);verify(dao,never()).insertAction(any());verifyNoInteractions(evaluations,audit);}
    @Test void mismatchedEffectHashAndVersionRejectBeforeApproval(){var wrong=new AttachmentNormalRollbackRequest(0,2,"a".repeat(64),true,true,true,"원복");assertThatThrownBy(()->service.insertRollback(auth("ADMIN"),source,job,UUID.randomUUID(),wrong)).hasMessageContaining("효과");var version=new AttachmentNormalRollbackRequest(1,2,"a".repeat(64),false,true,true,"원복");assertThatThrownBy(()->service.insertRollback(auth("ADMIN"),source,job,UUID.randomUUID(),version)).hasMessageContaining("버전");verify(dao,never()).insertAction(any());}
    @ParameterizedTest @ValueSource(strings={"OPERATOR","APPROVER","USER","PARTNER"}) void directServiceApprovalRequiresAdmin(String role){assertThatThrownBy(()->service.insertRollback(auth(role),source,job,UUID.randomUUID(),request())).hasMessageContaining("ADMIN");verifyNoInteractions(dao,evaluations,audit);}
    @Test void suspendedAndResetAccountsCannotReadOrWrite(){assertThatThrownBy(()->service.selectPreviewDetails(auth(actorId,"ADMIN","SUSPENDED",false),source,job)).hasMessageContaining("활성");assertThatThrownBy(()->service.insertRollback(auth(actorId,"ADMIN","ACTIVE",true),source,job,UUID.randomUUID(),request())).hasMessageContaining("활성");verifyNoInteractions(dao);}
    @Test void unknownSourceAndForeignJobFailBeforeStateOrWrites(){when(dao.selectJobLock(source,job)).thenReturn(null);assertThatThrownBy(()->service.insertRollback(auth("ADMIN"),source,job,UUID.randomUUID(),request())).hasMessageContaining("찾을 수");verify(dao,never()).selectStateJson(any(),any());verify(dao,never()).insertAction(any());}
    @Test void sourceCasFailureNeverRestoresEvaluationOrClaimsCompletion(){when(dao.updateSourceRestoration(any())).thenReturn(0);assertThatThrownBy(()->service.insertRollback(auth("ADMIN"),source,job,UUID.randomUUID(),request())).hasMessageContaining("전체를 취소");verifyNoInteractions(evaluations,audit);verify(dao,never()).updateRolledBack(any());}
    @Test void incompleteAndForeignEvidenceFailClosed() throws Exception {when(dao.selectStateJson(source,job)).thenReturn("{}");assertThatThrownBy(()->service.selectPreviewDetails(auth("ADMIN"),source,job)).hasMessageContaining("찾을 수");verify(dao,never()).insertAction(any());}
    @Test void missingAcknowledgementAndVersionOverflowFailBeforeDatabase(){var invalid=new AttachmentNormalRollbackRequest(0,Integer.MAX_VALUE,"a".repeat(64),false,true,true,"원복");assertThatThrownBy(()->service.insertRollback(auth("ADMIN"),source,job,UUID.randomUUID(),invalid)).hasMessageContaining("버전");var unconfirmed=new AttachmentNormalRollbackRequest(0,2,"a".repeat(64),false,true,false,"원복");assertThatThrownBy(()->service.insertRollback(auth("ADMIN"),source,job,UUID.randomUUID(),unconfirmed)).hasMessageContaining("효과");verifyNoInteractions(dao);}
}
