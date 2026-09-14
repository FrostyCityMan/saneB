package com.saneb.domain.announcementattachment.service.impl;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.common.error.ApiException;
import com.saneb.domain.announcementattachment.dao.*;
import com.saneb.domain.announcementattachment.dto.AttachmentPolicyPublication.*;
import com.saneb.domain.announcementattachment.dto.AttachmentPolicyPublicationScope;
import com.saneb.domain.announcementattachment.vo.*;
import com.saneb.domain.announcementsource.dao.AnnouncementSourceDao;
import com.saneb.domain.auth.vo.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.*;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** 게시 흐름의 대역 시험이다. 실제 QA 성공 또는 PostgreSQL 정책 게시 증거가 아니다. */
class AnnouncementAttachmentPolicyPublicationServiceTest {
    private final AnnouncementAttachmentPolicyPublicationDao dao=mock(AnnouncementAttachmentPolicyPublicationDao.class);
    private final AnnouncementAttachmentPolicyPublicationScopeDao scopes=mock(AnnouncementAttachmentPolicyPublicationScopeDao.class);
    private final AnnouncementAttachmentPolicyDao policies=mock(AnnouncementAttachmentPolicyDao.class);
    private final AnnouncementAttachmentPolicyValidationDao qa=mock(AnnouncementAttachmentPolicyValidationDao.class);
    private final AttachmentPolicyValidationSnapshotFactory snapshots=mock(AttachmentPolicyValidationSnapshotFactory.class);
    private final AttachmentPolicyPublicationQaVerifier verifier=mock(AttachmentPolicyPublicationQaVerifier.class);
    private final AnnouncementSourceDao audit=mock(AnnouncementSourceDao.class);
    private final PlatformTransactionManager transactions=mock(PlatformTransactionManager.class);
    private final UUID policyId=UUID.randomUUID(),scopeId=UUID.randomUUID(),ruleId=UUID.randomUUID(),runId=UUID.randomUUID(),actorId=UUID.randomUUID(),key=UUID.randomUUID(),previousId=UUID.randomUUID();
    private final String scopeHash="b".repeat(64),qaHash="a".repeat(64);
    private final Request request=new Request(scopeId,scopeHash,0,true,true,true,"정책 게시 검토 사유");
    private final AttachmentPolicyManagementRows.Row policy=mock(AttachmentPolicyManagementRows.Row.class);
    private final AttachmentPolicyValidationRows.Run run=mock(AttachmentPolicyValidationRows.Run.class);
    private final AttachmentPolicyValidationSnapshotFactory.Runtime installed=new AttachmentPolicyValidationSnapshotFactory.Runtime("c".repeat(64),"d".repeat(64),"e".repeat(64));
    private final AttachmentPolicyValidationSnapshotFactory.Frozen frozen=new AttachmentPolicyValidationSnapshotFactory.Frozen(qaHash,"{}",null,installed);
    private AnnouncementAttachmentPolicyPublicationServiceImpl service;
    private AttachmentPolicyPublicationRows.Insert inserted;
    private Receipt receipt;
    private Authentication actor(String role){var p=new AuthenticatedUserDetails(new AuthUserDetailsRow(actorId,"publication-qa","unused","QA","ACTIVE",false,null,null,null),List.of(role));return UsernamePasswordAuthenticationToken.authenticated(p,null,p.getAuthorities());}
    @BeforeEach void setup(){
        var mapper=new ObjectMapper().findAndRegisterModules();
        when(snapshots.hash(any())).thenAnswer(c->HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(mapper.writeValueAsBytes(c.getArgument(0)))));
        when(snapshots.json(any())).thenAnswer(c->mapper.writeValueAsString(c.getArgument(0)));
        when(transactions.getTransaction(any())).thenAnswer(c->{TransactionSynchronizationManager.setActualTransactionActive(true);return new SimpleTransactionStatus();});
        doAnswer(c->{TransactionSynchronizationManager.setActualTransactionActive(false);return null;}).when(transactions).commit(any());
        doAnswer(c->{TransactionSynchronizationManager.setActualTransactionActive(false);return null;}).when(transactions).rollback(any());
        when(snapshots.selectRuntime()).thenAnswer(c->{assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();return installed;});
        when(snapshots.selectSnapshot(policy,installed)).thenReturn(frozen);
        var now=OffsetDateTime.now();when(scopes.selectScopeDetails(policyId,scopeId)).thenReturn(new AttachmentPolicyPublicationScope.Summary(scopeId,policyId,0,ruleId,1,"ENFORCE",runId,qaHash,3L,scopeHash,now,now.plusMinutes(10)));
        when(scopes.selectScopeCurrent(scopeId)).thenReturn(true);when(policies.selectPolicyDetails(policyId,false)).thenReturn(policy);
        when(policy.policyId()).thenReturn(policyId);when(policy.rowVersion()).thenReturn(0);when(policy.policyStatusCode()).thenReturn("DRAFT");when(policy.ruleReleaseStatusCode()).thenReturn("ACTIVE");
        when(qa.selectRunDetails(runId,false)).thenReturn(run);when(qa.selectRunList(any())).thenReturn(List.of(run));when(qa.selectStepList(runId)).thenReturn(List.of());
        when(run.runId()).thenReturn(runId);when(run.policyId()).thenReturn(policyId);when(run.policyVersion()).thenReturn(0);when(run.ruleReleaseId()).thenReturn(ruleId);when(run.ruleVersion()).thenReturn(1);when(run.snapshotHash()).thenReturn(qaHash);
        when(verifier.selectValidatedEvidenceHash(eq(run),any(),eq(frozen))).thenAnswer(c->{assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();return "e".repeat(64);});
        when(dao.insertPublication(any())).thenAnswer(c->{inserted=c.getArgument(0);receipt=new Receipt(inserted.publicationId(),policyId,1,qaHash,previousId,2,scopeId,scopeHash,runId,"ENFORCE",OffsetDateTime.now());return 1;});
        when(dao.selectReceiptDetails(policyId)).thenAnswer(c->receipt);when(dao.updatePreviousPolicyRetired(any())).thenReturn(1);when(dao.updatePolicyActive(any())).thenReturn(1);
        service=new AnnouncementAttachmentPolicyPublicationServiceImpl(dao,scopes,policies,qa,snapshots,verifier,audit,transactions);
    }
    @AfterEach void clearTransactionMarker(){TransactionSynchronizationManager.setActualTransactionActive(false);}
    @Test void verifiesOutsideTransactionThenRechecksUnderNowaitBoundaryAndPublishesAtomically(){
        var result=service.insertPublication(actor("ADMIN"),policyId,key,request);
        assertThat(result.publication().policyHash()).isEqualTo(qaHash);assertThat(result.existingDataApplied()).isFalse();assertThat(result.workerEnabledByRequest()).isFalse();assertThat(result.currentHttpRequests()).isZero();
        var order=inOrder(snapshots,dao,scopes,verifier,audit,transactions);
        order.verify(snapshots).selectRuntime();order.verify(scopes).selectScopeDetails(policyId,scopeId);
        order.verify(verifier).selectValidatedEvidenceHash(eq(run),any(),eq(frozen));order.verify(snapshots).selectRuntime();
        order.verify(dao).selectPublicationLock();order.verify(scopes).selectScopeDetails(policyId,scopeId);
        order.verify(verifier).validateCurrentEvidence(eq(run),any(),eq(frozen));order.verify(dao).insertPublication(any());
        order.verify(dao).updatePreviousPolicyRetired(inserted.publicationId());order.verify(dao).updatePolicyActive(inserted.publicationId());order.verify(audit).insertAuditLog(any());order.verify(transactions).commit(any());
        var auditCaptor=org.mockito.ArgumentCaptor.forClass(com.saneb.domain.announcementsource.vo.AnnouncementSourceAuditLogCommand.class);verify(audit).insertAuditLog(auditCaptor.capture());
        assertThat(auditCaptor.getValue().metadataJson()).doesNotContain(request.reason(),key.toString()).contains("existingDataApplied");
    }
    @Test void noPreviousPolicyExpectsZeroRetirements(){
        doAnswer(c->{inserted=c.getArgument(0);receipt=new Receipt(inserted.publicationId(),policyId,1,qaHash,null,null,scopeId,scopeHash,runId,"OFF",OffsetDateTime.now());return 1;}).when(dao).insertPublication(any());
        when(dao.updatePreviousPolicyRetired(any())).thenReturn(0);assertThat(service.insertPublication(actor("ADMIN"),policyId,key,request).publication().previousPolicyId()).isNull();
    }
    @Test void changedProviderAttemptUnderPublicationLockRollsBackWithoutWrites(){
        doAnswer(c->{assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
            throw new ApiException(com.saneb.common.error.ErrorCode.ANNOUNCEMENT_ATTACHMENT_VERSION_CONFLICT,org.springframework.http.HttpStatus.CONFLICT,"수집원 QA 변경");
        }).when(verifier).validateCurrentEvidence(eq(run),any(),eq(frozen));
        assertThatThrownBy(()->service.insertPublication(actor("ADMIN"),policyId,key,request)).hasMessageContaining("수집원 QA 변경");
        verify(dao).selectPublicationLock();verify(dao,never()).insertPublication(any());verifyNoInteractions(audit);verify(transactions).rollback(any());
    }
    @Test void sameKeyReturnsOriginalWithoutRuntimeOrWritesEvenAfterScopeExpires(){
        service.insertPublication(actor("ADMIN"),policyId,key,request);
        when(dao.selectRequestDetails(key)).thenReturn(new AttachmentPolicyPublicationRows.Request(receipt.publicationId(),policyId,actorId,inserted.requestHash()));
        clearInvocations(dao,scopes,snapshots,policies,qa,verifier,audit);
        assertThat(service.insertPublication(actor("ADMIN"),policyId,key,request).publication()).isEqualTo(receipt);
        verify(dao,never()).selectPublicationLock();verify(snapshots,never()).selectRuntime();verifyNoInteractions(scopes,policies,qa,verifier,audit);
    }
    @Test void changedRequestCannotReuseSuccessfulKey(){
        service.insertPublication(actor("ADMIN"),policyId,key,request);when(dao.selectRequestDetails(key)).thenReturn(new AttachmentPolicyPublicationRows.Request(receipt.publicationId(),policyId,actorId,inserted.requestHash()));
        assertThatThrownBy(()->service.insertPublication(actor("ADMIN"),policyId,key,new Request(scopeId,scopeHash,0,true,true,true,"다른 사유"))).isInstanceOf(ApiException.class);
    }
    @Test void changedScopeOrMissingQaBlocksBeforePublication(){
        when(scopes.selectScopeCurrent(scopeId)).thenReturn(false);
        assertThatThrownBy(()->service.insertPublication(actor("ADMIN"),policyId,key,request)).isInstanceOf(ApiException.class);
        when(scopes.selectScopeCurrent(scopeId)).thenReturn(true);when(qa.selectRunList(any())).thenReturn(List.of());
        assertThatThrownBy(()->service.insertPublication(actor("ADMIN"),policyId,key,request)).isInstanceOf(ApiException.class);
        verify(dao,never()).insertPublication(any());verifyNoInteractions(audit);
    }
    @Test void evidenceFailureNeverWritesPolicyOrReceipt(){
        when(verifier.selectValidatedEvidenceHash(eq(run),any(),eq(frozen))).thenThrow(new ApiException(com.saneb.common.error.ErrorCode.ANNOUNCEMENT_ATTACHMENT_VERSION_CONFLICT,org.springframework.http.HttpStatus.CONFLICT,"전체 QA 필요"));
        assertThatThrownBy(()->service.insertPublication(actor("ADMIN"),policyId,key,request)).isInstanceOf(ApiException.class);
        verify(dao,never()).insertPublication(any());verify(dao,never()).updatePolicyActive(any());verifyNoInteractions(audit);verify(dao,never()).selectPublicationLock();
    }
    @Test void partialRetirementOrActivationRollsBackWholeTransaction(){
        when(dao.updatePreviousPolicyRetired(any())).thenReturn(0);assertThatThrownBy(()->service.insertPublication(actor("ADMIN"),policyId,key,request)).isInstanceOf(ApiException.class);
        when(dao.updatePreviousPolicyRetired(any())).thenReturn(1);when(dao.updatePolicyActive(any())).thenReturn(0);assertThatThrownBy(()->service.insertPublication(actor("ADMIN"),policyId,key,request)).isInstanceOf(ApiException.class);
        verify(transactions,times(2)).rollback(any());verifyNoInteractions(audit);
    }
    @Test void busyLockReturnsConflictAndNeverRetriesAutomatically(){
        when(dao.selectPublicationLock()).thenThrow(new CannotAcquireLockException("private SQL"));
        assertThatThrownBy(()->service.insertPublication(actor("ADMIN"),policyId,key,request)).isInstanceOf(ApiException.class).hasMessageContaining("원래 요청 키").hasMessageNotContaining("private SQL");
        verify(dao,times(1)).selectPublicationLock();verify(dao,never()).insertPublication(any());verifyNoInteractions(audit);
    }
    @Test void changedInstalledRuntimeBeforeFinalWriteBlocksPublication(){
        when(snapshots.selectRuntime()).thenReturn(installed,new AttachmentPolicyValidationSnapshotFactory.Runtime("f".repeat(64),installed.runtimeSuiteHash(),installed.executionCodeHash()));
        assertThatThrownBy(()->service.insertPublication(actor("ADMIN"),policyId,key,request)).isInstanceOf(ApiException.class).hasMessageContaining("설치 런타임");
        verify(dao,never()).insertPublication(any());verifyNoInteractions(audit);verify(dao,never()).selectPublicationLock();
    }
    @Test void changedApplicationCodeBeforeWriteBlocksEvenWhenExtractorIsUnchanged(){
        when(snapshots.selectRuntime()).thenReturn(installed,new AttachmentPolicyValidationSnapshotFactory.Runtime(installed.runtimeHash(),installed.runtimeSuiteHash(),"f".repeat(64)));
        assertThatThrownBy(()->service.insertPublication(actor("ADMIN"),policyId,key,request)).hasMessageContaining("애플리케이션 코드");
        verify(dao,never()).insertPublication(any());verify(dao,never()).selectPublicationLock();
    }
    @Test void changedEvidenceAfterPreflightCannotBePublishedUnderLock(){
        var different=mock(AttachmentPolicyValidationRows.Step.class);
        when(qa.selectStepList(runId)).thenReturn(List.of(),List.of(different));
        assertThatThrownBy(()->service.insertPublication(actor("ADMIN"),policyId,key,request)).hasMessageContaining("단계 근거가 바뀌었습니다");
        verify(dao,never()).insertPublication(any());verify(transactions).rollback(any());
    }
    @Test void changedSnapshotAfterPreflightCannotReuseVerifiedHash(){
        when(snapshots.selectSnapshot(policy,installed)).thenReturn(frozen,new AttachmentPolicyValidationSnapshotFactory.Frozen("f".repeat(64),"{}",null,installed));
        assertThatThrownBy(()->service.insertPublication(actor("ADMIN"),policyId,key,request)).hasMessageContaining("단계 근거가 바뀌었습니다");
        verify(dao,never()).insertPublication(any());verify(transactions).rollback(any());
    }
    @Test void sameRequestPublishedDuringPreflightReturnsOriginalReceipt(){
        service.insertPublication(actor("ADMIN"),policyId,key,request);
        var original = receipt;
        when(dao.selectRequestDetails(key)).thenReturn(null,new AttachmentPolicyPublicationRows.Request(original.publicationId(),policyId,actorId,inserted.requestHash()));
        when(policy.policyStatusCode()).thenReturn("ACTIVE");
        clearInvocations(dao,audit);
        assertThat(service.insertPublication(actor("ADMIN"),policyId,key,request).publication()).isEqualTo(original);
        verify(dao,never()).insertPublication(any());verifyNoInteractions(audit);
    }
    @ParameterizedTest @ValueSource(strings={"OPERATOR","APPROVER","USER","PARTNER"})
    void onlyAdminCanPublish(String role){assertThatThrownBy(()->service.insertPublication(actor(role),policyId,key,request)).isInstanceOf(ApiException.class);verifyNoInteractions(dao,scopes,policies,snapshots);}
    @Test void exactImpactConsentAndReasonAreRequiredInService(){
        for(var invalid:Arrays.asList(null,new Request(scopeId,scopeHash,0,false,true,true,"사유"),new Request(scopeId,scopeHash,0,true,false,true,"사유"),new Request(scopeId,scopeHash,0,true,true,false,"사유"),new Request(scopeId,scopeHash,0,true,true,true," "),new Request(scopeId,"bad",0,true,true,true,"사유")))
            assertThatThrownBy(()->service.insertPublication(actor("ADMIN"),policyId,key,invalid)).isInstanceOf(ApiException.class);
        verifyNoInteractions(dao,scopes,snapshots);
    }
    @ParameterizedTest @ValueSource(strings={"ADMIN","OPERATOR","APPROVER"})
    void readRolesSeeHistoricalReceiptWithoutReexecution(String role){
        service.insertPublication(actor("ADMIN"),policyId,key,request);clearInvocations(scopes,snapshots,verifier,audit);
        assertThat(service.selectPublicationDetails(actor(role),policyId).publication()).isEqualTo(receipt);verifyNoInteractions(scopes,snapshots,verifier,audit);
    }
}
