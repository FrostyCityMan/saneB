package com.saneb.domain.announcementattachment.service.impl;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.common.error.ApiException;
import com.saneb.domain.announcementattachment.dao.*;
import com.saneb.domain.announcementattachment.dto.AttachmentPolicyPublicationScope.*;
import com.saneb.domain.announcementattachment.vo.AttachmentPolicyPublicationScopeRows.*;
import com.saneb.domain.announcementattachment.vo.AttachmentPolicyManagementRows;
import com.saneb.domain.announcementsource.dao.AnnouncementSourceDao;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceAuditLogCommand;
import com.saneb.domain.auth.vo.*;
import java.time.OffsetDateTime;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.*;
import org.springframework.transaction.support.SimpleTransactionStatus;

/** Service/transaction 대역 검증. 실제 DB trigger와 동시성은 별도 PostgreSQL 테스트에서 검사한다. */
class AnnouncementAttachmentPolicyPublicationScopeServiceTest {
    private final AnnouncementAttachmentPolicyPublicationScopeDao dao=mock(AnnouncementAttachmentPolicyPublicationScopeDao.class);
    private final AnnouncementAttachmentPolicyDao policies=mock(AnnouncementAttachmentPolicyDao.class);
    private final AnnouncementSourceDao audit=mock(AnnouncementSourceDao.class);
    private final PlatformTransactionManager transactions=mock(PlatformTransactionManager.class);
    private final UUID actorId=UUID.randomUUID(),policyId=UUID.randomUUID(),ruleId=UUID.randomUUID(),key=UUID.randomUUID();
    private final Prepare request=new Prepare(3,"게시 준비 검토 사유");
    private AnnouncementAttachmentPolicyPublicationScopeServiceImpl service;
    private Insert inserted;
    private Summary stored;
    private final AttachmentPolicyManagementRows.Row policy=mock(AttachmentPolicyManagementRows.Row.class);
    private Authentication actor(String role,String status,boolean reset,UUID id){
        var principal=new AuthenticatedUserDetails(new AuthUserDetailsRow(id,"scope-qa","unused","QA",status,reset,null,null,null),List.of(role));
        return UsernamePasswordAuthenticationToken.authenticated(principal,null,principal.getAuthorities());
    }
    private Authentication actor(){return actor("ADMIN","ACTIVE",false,actorId);}
    private Summary summary(UUID id,UUID parent,String mode,OffsetDateTime created,OffsetDateTime expires){return new Summary(id,parent,3,ruleId,4,mode,null,null,1001L,"a".repeat(64),created,expires);}
    @BeforeEach void setup(){
        when(transactions.getTransaction(any())).thenAnswer(i->new SimpleTransactionStatus());
        service=new AnnouncementAttachmentPolicyPublicationScopeServiceImpl(dao,policies,audit,new ObjectMapper(),transactions);
        when(policy.policyId()).thenReturn(policyId);when(policy.policyStatusCode()).thenReturn("DRAFT");
        when(policy.ruleReleaseStatusCode()).thenReturn("ACTIVE");when(policy.rowVersion()).thenReturn(3);
        when(policies.selectPolicyDetails(policyId,false)).thenReturn(policy);
        when(dao.insertScope(any())).thenAnswer(i->{inserted=i.getArgument(0);var now=OffsetDateTime.now();stored=summary(inserted.scopeId(),policyId,"COLLECT_ONLY",now,now.plusMinutes(10));return 1;});
        when(dao.insertScopeMembers(any(),eq(policyId))).thenReturn(1001L);when(dao.updateScopeSealed(any())).thenReturn(1);
        when(dao.selectScopeDetails(eq(policyId),any())).thenAnswer(i->stored);
        when(dao.selectScopeCurrent(any())).thenReturn(true);
    }
    @Test void fixesMoreThanOneThousandMembersWithoutApprovalOrExternalRequests(){
        var result=service.insertScope(actor(),policyId,key,request);
        assertThat(result.scope().itemCount()).isEqualTo(1001);assertThat(result.isScopeCurrent()).isTrue();assertThat(result.isExpired()).isFalse();
        assertThat(result.isApproval()).isFalse();assertThat(result.requiresPublicationRevalidation()).isTrue();assertThat(result.currentHttpRequests()).isZero();
        assertThat(inserted.requestHash()).matches("[a-f0-9]{64}");assertThat(inserted.reasonHash()).matches("[a-f0-9]{64}");
        var command=ArgumentCaptor.forClass(AnnouncementSourceAuditLogCommand.class);verify(audit).insertAuditLog(command.capture());
        assertThat(command.getValue().actionCode()).isEqualTo("ATTACHMENT_POLICY_SCOPE_PREPARE");
        assertThat(command.getValue().metadataJson()).contains("\"isApproval\":false","scopeHash").doesNotContain(request.reason(),key.toString());
        verify(policies).selectPolicyDetails(policyId,false);verifyNoMoreInteractions(policies);
        var definitions=ArgumentCaptor.forClass(TransactionDefinition.class);verify(transactions,times(2)).getTransaction(definitions.capture());
        assertThat(definitions.getAllValues()).allSatisfy(d->assertThat(d.getIsolationLevel()).isEqualTo(TransactionDefinition.ISOLATION_REPEATABLE_READ));
        assertThat(definitions.getAllValues().get(0).isReadOnly()).isTrue();assertThat(definitions.getAllValues().get(1).getTimeout()).isEqualTo(20);
    }
    @Test void sameKeyReturnsExpiredOriginalRatherThanPreparingAgain(){
        service.insertScope(actor(),policyId,key,request);var id=inserted.scopeId();
        when(dao.selectRequestDetails(key)).thenReturn(new Request(id,policyId,actorId,inserted.requestHash()));
        var old=OffsetDateTime.now().minusMinutes(20);stored=summary(id,policyId,"COLLECT_ONLY",old,old.plusMinutes(10));
        clearInvocations(dao,policies,audit);
        var result=service.insertScope(actor(),policyId,key,request);
        assertThat(result.scope().scopeId()).isEqualTo(id);assertThat(result.isExpired()).isTrue();assertThat(result.isScopeCurrent()).isFalse();
        verify(dao,never()).insertScope(any());verify(dao,never()).selectScopeCurrent(any());verifyNoInteractions(policies,audit);
    }
    @Test void sameKeyRejectsOtherActorPolicyOrInput(){
        service.insertScope(actor(),policyId,key,request);when(dao.selectRequestDetails(key)).thenReturn(new Request(inserted.scopeId(),policyId,actorId,inserted.requestHash()));
        clearInvocations(dao,audit);
        assertThatThrownBy(()->service.insertScope(actor("ADMIN","ACTIVE",false,UUID.randomUUID()),policyId,key,request)).isInstanceOf(ApiException.class);
        assertThatThrownBy(()->service.insertScope(actor(),UUID.randomUUID(),key,request)).isInstanceOf(ApiException.class);
        assertThatThrownBy(()->service.insertScope(actor(),policyId,key,new Prepare(3,"다른 사유"))).isInstanceOf(ApiException.class);
        verify(dao,never()).insertScope(any());verifyNoInteractions(audit);
    }
    @Test void simultaneousInsertLoserReadsWinnerOnlyAfterRollback(){
        UUID id=UUID.randomUUID();var now=OffsetDateTime.now();stored=summary(id,policyId,"COLLECT_ONLY",now,now.plusMinutes(10));
        doAnswer(i->{inserted=i.getArgument(0);throw new DuplicateKeyException("private SQL must not escape");}).when(dao).insertScope(any());
        when(dao.selectRequestDetails(key)).thenAnswer(i->inserted==null?null:new Request(id,policyId,actorId,inserted.requestHash()));
        assertThat(service.insertScope(actor(),policyId,key,request).scope().scopeId()).isEqualTo(id);
        var order=inOrder(dao,transactions);order.verify(dao).selectRequestDetails(key);order.verify(dao).insertScope(any());
        order.verify(transactions).rollback(any());order.verify(dao).selectRequestDetails(key);verifyNoInteractions(audit);
    }
    @Test void integrityFailureWithoutWinnerReturnsConflictWithoutSqlDetails(){
        doThrow(new DuplicateKeyException("private SQL must not escape")).when(dao).insertScope(any());
        assertThatThrownBy(()->service.insertScope(actor(),policyId,key,request)).isInstanceOf(ApiException.class).hasMessageNotContaining("private SQL");
        verify(transactions).rollback(any());verifyNoInteractions(audit);
    }
    @ParameterizedTest @ValueSource(strings={"OPERATOR","APPROVER","USER","PARTNER","REVIEWER"})
    void onlyAdminCanPrepare(String role){assertThatThrownBy(()->service.insertScope(actor(role,"ACTIVE",false,actorId),policyId,key,request)).isInstanceOf(ApiException.class);verifyNoInteractions(dao,policies,audit);}
    @ParameterizedTest @ValueSource(strings={"ADMIN","OPERATOR","APPROVER"})
    void internalRolesCanReadButReadNeverBecomesApproval(String role){var now=OffsetDateTime.now();stored=summary(UUID.randomUUID(),policyId,"OFF",now,now.plusMinutes(10));
        when(dao.selectScopeCurrent(stored.scopeId())).thenReturn(false);
        var result=service.selectScopeDetails(actor(role,"ACTIVE",false,actorId),policyId,stored.scopeId());
        assertThat(result.isScopeCurrent()).isFalse();assertThat(result.isApproval()).isFalse();verifyNoInteractions(audit);}
    @Test void inactiveResetAndAnonymousAreDeniedBeforeQueries(){
        for(var invalid:List.of(actor("ADMIN","INACTIVE",false,actorId),actor("ADMIN","ACTIVE",true,actorId),UsernamePasswordAuthenticationToken.unauthenticated("qa",null)))
            assertThatThrownBy(()->service.insertScope(invalid,policyId,key,request)).isInstanceOf(ApiException.class);
        assertThatThrownBy(()->service.insertScope(null,policyId,key,request)).isInstanceOf(ApiException.class);verifyNoInteractions(dao,policies,audit);
    }
    @Test void requestValidationCannotBeBypassedThroughService(){
        for(var invalid:Arrays.asList(null,new Prepare(null,"사유"),new Prepare(-1,"사유"),new Prepare(3,null),new Prepare(3," "),new Prepare(3,"a".repeat(1001))))
            assertThatThrownBy(()->service.insertScope(actor(),policyId,key,invalid)).isInstanceOf(ApiException.class);
        assertThatThrownBy(()->service.insertScope(actor(),null,key,request)).isInstanceOf(ApiException.class);
        assertThatThrownBy(()->service.insertScope(actor(),policyId,null,request)).isInstanceOf(ApiException.class);verifyNoInteractions(dao,policies,audit);
    }
    @Test void policyMustRemainCurrentDraftOnActiveRule(){
        when(policy.rowVersion()).thenReturn(4);assertThatThrownBy(()->service.insertScope(actor(),policyId,key,request)).isInstanceOf(ApiException.class);
        when(policy.rowVersion()).thenReturn(3);when(policy.policyStatusCode()).thenReturn("ACTIVE");assertThatThrownBy(()->service.insertScope(actor(),policyId,key,request)).isInstanceOf(ApiException.class);
        when(policy.policyStatusCode()).thenReturn("DRAFT");when(policy.ruleReleaseStatusCode()).thenReturn("RETIRED");assertThatThrownBy(()->service.insertScope(actor(),policyId,key,request)).isInstanceOf(ApiException.class);
        verify(dao,never()).insertScope(any());verifyNoInteractions(audit);
    }
    @Test void emptyMembershipFailedSealAndCountMismatchRollback(){
        when(dao.insertScopeMembers(any(),eq(policyId))).thenReturn(0L);assertThatThrownBy(()->service.insertScope(actor(),policyId,key,request)).isInstanceOf(ApiException.class);
        when(dao.insertScopeMembers(any(),eq(policyId))).thenReturn(1001L);when(dao.updateScopeSealed(any())).thenReturn(0);assertThatThrownBy(()->service.insertScope(actor(),policyId,key,request)).isInstanceOf(ApiException.class);
        when(dao.updateScopeSealed(any())).thenReturn(1);when(dao.insertScopeMembers(any(),eq(policyId))).thenReturn(1002L);assertThatThrownBy(()->service.insertScope(actor(),policyId,key,request)).isInstanceOf(ApiException.class);
        verify(transactions,times(3)).rollback(any());verifyNoInteractions(audit);
    }
    @Test void detailAndListRejectCrossPolicyAndMalformedStoredData(){
        var now=OffsetDateTime.now();UUID id=UUID.randomUUID();stored=summary(id,UUID.randomUUID(),"OFF",now,now.plusMinutes(10));
        assertThatThrownBy(()->service.selectScopeDetails(actor(),policyId,id)).isInstanceOf(ApiException.class);
        when(dao.selectScopeList(any())).thenReturn(List.of(stored));assertThatThrownBy(()->service.selectScopeList(actor(),policyId,1,20)).isInstanceOf(ApiException.class);
        stored=summary(id,policyId,null,now,now.plusMinutes(10));assertThatThrownBy(()->service.selectScopeDetails(actor(),policyId,id)).isInstanceOf(ApiException.class);
        stored=summary(id,policyId,"OFF",now,now.plusMinutes(16));assertThatThrownBy(()->service.selectScopeDetails(actor(),policyId,id)).isInstanceOf(ApiException.class);
    }
    @Test void itemPaginationUsesFrozenCountAndRejectsNullType(){
        var now=OffsetDateTime.now();UUID id=UUID.randomUUID();stored=summary(id,policyId,"OFF",now,now.plusMinutes(10));
        when(dao.selectItemList(any())).thenReturn(List.of(new Item("POLICY",policyId,"b".repeat(64))));
        var result=service.selectItemList(actor(),policyId,id,2,100);assertThat(result.totalCount()).isEqualTo(1001);assertThat(result.totalPages()).isEqualTo(11);
        verify(dao).selectItemList(new Search(policyId,id,100,100));
        when(dao.selectItemList(any())).thenReturn(List.of(new Item(null,policyId,"b".repeat(64))));
        assertThatThrownBy(()->service.selectItemList(actor(),policyId,id,1,20)).isInstanceOf(ApiException.class);
        assertThatThrownBy(()->service.selectItemList(actor(),policyId,id,0,20)).isInstanceOf(ApiException.class);
        assertThatThrownBy(()->service.selectItemList(actor(),policyId,id,Integer.MAX_VALUE,100)).isInstanceOf(ApiException.class);
    }
}
