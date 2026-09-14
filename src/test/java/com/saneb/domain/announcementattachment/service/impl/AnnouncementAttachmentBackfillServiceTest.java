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
import com.saneb.domain.auth.vo.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

class AnnouncementAttachmentBackfillServiceTest {
    @Test void gov24CanonicalCountsPreserveScopeAliasAndIdempotentInventory() {
        var scope=new AttachmentBackfillRequests.Scope(policy,List.of("GOV24"),now.minusDays(1),now,null,null,1000);
        when(batches.selectScopeCounts(any())).thenReturn(List.of(new AttachmentBatchRows.Bucket("GOV24_PUBLIC_SERVICE","CANDIDATE",count)));
        var preview=service.selectScopePreview(auth("ADMIN"),scope);
        assertThat(preview.scope().providerCodes()).containsExactly("GOV24");
        assertThat(preview.counts()).containsExactly(new AttachmentBatchRows.Bucket("GOV24","CANDIDATE",count));
        assertThat(preview.candidateCount()).isEqualTo(1001);
        var request=new AttachmentBackfillRequests.Inventory(scope,preview.scopeHash(),count,"정부24 전체 범위");
        var first=service.insertInventory(auth("ADMIN"),key,request);
        clearInvocations(dao,batches,audit);
        assertThat(service.insertInventory(auth("ADMIN"),key,request).runId()).isEqualTo(first.runId());
        verifyNoInteractions(batches,audit);verify(dao,never()).insertItems(any());verify(dao,never()).selectScopeDigest(any());
    }
    private final AnnouncementAttachmentBackfillDao dao=mock(AnnouncementAttachmentBackfillDao.class);
    private final AnnouncementAttachmentBatchDao batches=mock(AnnouncementAttachmentBatchDao.class);
    private final AnnouncementSourceDao audit=mock(AnnouncementSourceDao.class);
    private final UUID policy=UUID.randomUUID(),rule=UUID.randomUUID(),actor=UUID.randomUUID(),key=UUID.randomUUID();
    private final OffsetDateTime now=OffsetDateTime.parse("2026-09-12T00:00:00+09:00");
    private AnnouncementAttachmentBackfillServiceImpl service;
    private AttachmentBackfillRows.Run saved;
    private long count=1001;
    @BeforeEach void setup() {
        service=new AnnouncementAttachmentBackfillServiceImpl(dao,batches,audit,new ObjectMapper().findAndRegisterModules());
        when(batches.selectPolicyDetails(eq(policy),eq(false))).thenReturn(new AttachmentPolicyRow(policy,"ACTIVE","COLLECT_ONLY",rule,"ACTIVE","a".repeat(64),"{}","[]",0));
        when(batches.selectScopeCounts(any())).thenAnswer(call->List.of(new AttachmentBatchRows.Bucket("BIZINFO","CANDIDATE",count),new AttachmentBatchRows.Bucket("BIZINFO","LINKED_PROTECTED",3L)));
        when(dao.selectScopeDigest(any())).thenAnswer(call->new AttachmentBackfillRows.Digest(count,"b".repeat(64)));
        when(dao.selectRequestLock(any())).thenReturn(true);
        when(dao.selectRequestDetails(key)).thenAnswer(call->saved);
        when(dao.insertRun(any())).thenAnswer(call->{var row=(AttachmentBackfillRows.Insert)call.getArgument(0);
            saved=new AttachmentBackfillRows.Run(row.runId(),row.policyId(),row.scopeJson(),row.policySnapshotJson(),row.scopeHash(),row.candidateHash(),row.candidateCount(),row.segmentSize(),row.segmentCount(),0L,0L,actor,key,row.requestHash(),now);return 1;});
        when(dao.insertSegments(any())).thenAnswer(call->saved.segmentCount());when(dao.insertItems(any())).thenAnswer(call->count);
        when(dao.selectRunDetails(any())).thenAnswer(call->saved);
        when(dao.selectRunTotals(any())).thenAnswer(call->new AttachmentBackfillRows.Totals(saved.candidateCount()-saved.deletedItemCount(),saved.segmentCount(),saved.candidateCount(),saved.deletedItemCount()));
    }
    private Authentication auth(String role) {return auth(role,actor,false,"ACTIVE");}
    private Authentication auth(String role,UUID id,boolean reset,String status) {
        var user=new AuthenticatedUserDetails(new AuthUserDetailsRow(id,"fixture","unused","QA",status,reset,null,null,null),List.of(role));
        return UsernamePasswordAuthenticationToken.authenticated(user,null,user.getAuthorities());
    }
    private AttachmentBackfillRequests.Scope scope(int size) {return new AttachmentBackfillRequests.Scope(policy,List.of("BIZINFO"),now.minusDays(1),now,null,null,size);}
    private AttachmentBackfillRequests.Inventory request() {return new AttachmentBackfillRequests.Inventory(scope(1000),service.selectScopePreview(auth("ADMIN"),scope(1000)).scopeHash(),count,"전체 목록 고정");}
    @Test void full1001PreviewIsNotFirst1000OrACollectionApproval() {
        var preview=service.selectScopePreview(auth("ADMIN"),scope(1000));
        assertThat(preview.candidateCount()).isEqualTo(1001);assertThat(preview.segmentCount()).isEqualTo(2);assertThat(preview.previewHttpRequests()).isZero();
        assertThat(preview.canInventory()).isTrue();assertThat(preview.counts()).anyMatch(b->b.reasonCode().equals("LINKED_PROTECTED") && b.count()==3);
        verify(batches,never()).selectCandidateList(any());verify(batches,never()).insertBatch(any());verifyNoInteractions(audit);
        verify(dao,never()).insertRun(any());
    }
    @Test void allCountsUseLongAndNoOverflowInCeilingCalculation() {
        count=Long.MAX_VALUE;var preview=service.selectScopePreview(auth("ADMIN"),scope(1000));
        assertThat(preview.segmentCount()).isEqualTo((Long.MAX_VALUE-1)/1000+1);assertThat(preview.candidateCount()).isEqualTo(Long.MAX_VALUE);
    }
    @Test void inventoryStoresEveryCandidateWithoutCreatingJobsAndReplayDoesNotRerunFilter() {
        var request=request();var first=service.insertInventory(auth("ADMIN"),key,request);
        assertThat(first.statusCode()).isEqualTo("INVENTORIED");assertThat(first.candidateCount()).isEqualTo(1001);assertThat(first.remainingItemCount()).isEqualTo(1001);
        assertThat(first.frozenScope()).containsEntry("inventoryOnly",true).doesNotContainKey("sourceIds");
        clearInvocations(dao,batches,audit);count=2000;
        assertThat(service.insertInventory(auth("ADMIN"),key,request).runId()).isEqualTo(first.runId());
        verify(dao,never()).selectScopeDigest(any());verifyNoInteractions(batches,audit);
    }
    @Test void changedCountOrFingerprintBlocksBeforeMaterialization() {
        var request=request();count=1002;
        assertThatThrownBy(()->service.insertInventory(auth("ADMIN"),key,request)).isInstanceOf(ApiException.class).hasMessageContaining("바뀌었습니다");
        verify(dao,never()).insertRun(any());count=1001;
        when(dao.selectScopeDigest(any())).thenReturn(new AttachmentBackfillRows.Digest(count,"c".repeat(64)));
        assertThatThrownBy(()->service.insertInventory(auth("ADMIN"),key,request)).isInstanceOf(ApiException.class);
        verify(dao,never()).insertRun(any());
    }
    @Test void bucketMismatchAndPartialMaterializationAreNotSuccess() {
        when(dao.selectScopeDigest(any())).thenReturn(new AttachmentBackfillRows.Digest(1000L,"b".repeat(64)));
        assertThatThrownBy(()->service.selectScopePreview(auth("ADMIN"),scope(1000))).isInstanceOf(ApiException.class).hasMessageContaining("전체 집계");
        when(dao.selectScopeDigest(any())).thenReturn(new AttachmentBackfillRows.Digest(count,"b".repeat(64)));
        var request=request();when(dao.insertItems(any())).thenReturn(1000L);
        assertThatThrownBy(()->service.insertInventory(auth("ADMIN"),key,request)).isInstanceOf(ApiException.class).hasMessageContaining("일부만 저장하지 않고");verifyNoInteractions(audit);
    }
    @Test void zeroCandidatesCanBePreviewedButCannotBeFrozen() {
        count=0;var preview=service.selectScopePreview(auth("ADMIN"),scope(100));assertThat(preview.canInventory()).isFalse();assertThat(preview.segmentCount()).isZero();
        assertThatThrownBy(()->service.insertInventory(auth("ADMIN"),key,new AttachmentBackfillRequests.Inventory(scope(100),preview.scopeHash(),1L,"사유"))).isInstanceOf(ApiException.class);
        verify(dao,never()).insertRun(any());
    }
    @Test void inFlightRequestReturnsConflictWithoutWaitingOrWriting() {
        var request=request();when(dao.selectRequestLock(key)).thenReturn(false);
        assertThatThrownBy(()->service.insertInventory(auth("ADMIN"),key,request)).isInstanceOf(ApiException.class).hasMessageContaining("처리 중");
        verify(dao,never()).selectRequestDetails(any());verify(dao,never()).insertRun(any());
    }
    @Test void actorAndPayloadAreBoundToIdempotencyKey() {
        var request=request();service.insertInventory(auth("ADMIN"),key,request);
        assertThatThrownBy(()->service.insertInventory(auth("ADMIN",UUID.randomUUID(),false,"ACTIVE"),key,request)).isInstanceOf(ApiException.class).hasMessageContaining("멱등 키");
        assertThatThrownBy(()->service.insertInventory(auth("ADMIN"),key,new AttachmentBackfillRequests.Inventory(request.scope(),request.expectedScopeHash(),count,"바뀐 사유"))).isInstanceOf(ApiException.class);
    }
    @Test void deletionPreservesOriginalDenominatorAndDoesNotImplyProcessingSuccess() {
        service.insertInventory(auth("ADMIN"),key,request());
        saved=new AttachmentBackfillRows.Run(saved.runId(),policy,saved.scopeJson(),saved.policySnapshotJson(),saved.scopeHash(),saved.candidateHash(),1001L,1000,2L,1001L,1001L,actor,key,saved.requestHash(),now);
        var response=service.selectRunDetails(auth("ADMIN"),saved.runId());assertThat(response.candidateCount()).isEqualTo(1001);assertThat(response.remainingItemCount()).isZero();
        assertThat(response.deletedItemCount()).isEqualTo(1001);assertThat(response.statusCode()).isEqualTo("INVENTORIED");
        when(dao.selectRunTotals(any())).thenReturn(new AttachmentBackfillRows.Totals(1L,2L,1001L,1001L));
        assertThatThrownBy(()->service.selectRunDetails(auth("ADMIN"),saved.runId())).isInstanceOf(ApiException.class).hasMessageContaining("일치하지 않습니다");
    }
    @Test void fixedItemsOnlyAreReadAndChangedInputsRemainVisible() {
        service.insertInventory(auth("ADMIN"),key,request());UUID source=UUID.randomUUID();
        when(dao.selectSegmentDetails(saved.runId(),2L)).thenReturn(new AttachmentBackfillRows.Segment(saved.runId(),2L,1,0,1L));
        when(dao.selectItemList(any())).thenReturn(List.of(new AttachmentBackfillRows.Item(1001L,source,UUID.randomUUID(),UUID.randomUUID(),rule,"BIZINFO","a".repeat(64),false)));
        clearInvocations(batches);var page=service.selectItemList(auth("OPERATOR"),saved.runId(),2,1,100);
        assertThat(page.totalCount()).isEqualTo(1);assertThat(page.items().getFirst().currentInputMatches()).isFalse();verifyNoInteractions(batches);
        assertThatThrownBy(()->service.selectItemList(auth("ADMIN"),saved.runId(),3,1,100)).isInstanceOf(ApiException.class).hasMessageContaining("찾을 수 없습니다");
    }
    @ParameterizedTest @ValueSource(strings={"OPERATOR","APPROVER","USER","PARTNER","REVIEWER"})
    void onlyAdminCanFreeze(String role) {
        var request=request();clearInvocations(dao,batches);
        assertThatThrownBy(()->service.insertInventory(auth(role),key,request)).isInstanceOf(ApiException.class);verifyNoInteractions(dao,batches,audit);
    }
    @Test void anonymousDisabledAndPasswordResetActorsAreRejected() {
        for(var authentication:Arrays.asList(null,auth("ADMIN",actor,true,"ACTIVE"),auth("ADMIN",actor,false,"SUSPENDED")))
            assertThatThrownBy(()->service.selectScopePreview(authentication,scope(100))).isInstanceOf(ApiException.class);
        verifyNoInteractions(dao,batches,audit);
    }
    @ParameterizedTest @ValueSource(ints={0,-1,1001})
    void invalidSegmentSizesAreNotSilentlyClamped(int size) {
        assertThatThrownBy(()->service.selectScopePreview(auth("ADMIN"),scope(size))).isInstanceOf(ApiException.class).hasMessageContaining("1~1000");
    }
    @Test void utcNormalizationDoesNotChangeTheSameScopeHash() {
        var scope=scope(100);var utc=new AttachmentBackfillRequests.Scope(policy,scope.providerCodes(),scope.collectedFrom().withOffsetSameInstant(ZoneOffset.UTC),scope.collectedBefore().withOffsetSameInstant(ZoneOffset.UTC),null,null,100);
        assertThat(service.selectScopePreview(auth("ADMIN"),scope).scopeHash()).isEqualTo(service.selectScopePreview(auth("ADMIN"),utc).scopeHash());
    }
    @Test void duplicateUnsupportedAndReversedScopeAreRejected() {
        for(var providers:List.of(List.of("BIZINFO","BIZINFO"),List.of("UNKNOWN"),List.<String>of()))
            assertThatThrownBy(()->service.selectScopePreview(auth("ADMIN"),new AttachmentBackfillRequests.Scope(policy,providers,now.minusDays(1),now,null,null,100))).isInstanceOf(ApiException.class);
        assertThatThrownBy(()->service.selectScopePreview(auth("ADMIN"),new AttachmentBackfillRequests.Scope(policy,List.of("BIZINFO"),now,now,null,null,100))).isInstanceOf(ApiException.class);
    }
    @Test void inactivePolicyAndInvalidPaginationAreBlocked() {
        when(batches.selectPolicyDetails(policy,false)).thenReturn(new AttachmentPolicyRow(policy,"RETIRED","COLLECT_ONLY",rule,"ACTIVE","a".repeat(64),"{}","[]",1));
        assertThatThrownBy(()->service.selectScopePreview(auth("ADMIN"),scope(100))).isInstanceOf(ApiException.class).hasMessageContaining("ACTIVE");
        assertThatThrownBy(()->service.selectRunList(auth("ADMIN"),0,20)).isInstanceOf(ApiException.class);
        assertThatThrownBy(()->service.selectRunList(auth("ADMIN"),1,101)).isInstanceOf(ApiException.class);
    }
    @Test void springInventoryBoundaryIsRepeatableReadAndPreviewIsReadOnly() throws Exception {
        var write=AnnouncementAttachmentBackfillServiceImpl.class.getMethod("insertInventory",Authentication.class,UUID.class,AttachmentBackfillRequests.Inventory.class)
                .getAnnotation(org.springframework.transaction.annotation.Transactional.class);
        var read=AnnouncementAttachmentBackfillServiceImpl.class.getMethod("selectScopePreview",Authentication.class,AttachmentBackfillRequests.Scope.class)
                .getAnnotation(org.springframework.transaction.annotation.Transactional.class);
        assertThat(write.isolation()).isEqualTo(org.springframework.transaction.annotation.Isolation.REPEATABLE_READ);assertThat(write.timeout()).isEqualTo(30);
        assertThat(read.readOnly()).isTrue();assertThat(read.isolation()).isEqualTo(org.springframework.transaction.annotation.Isolation.REPEATABLE_READ);
    }
}
