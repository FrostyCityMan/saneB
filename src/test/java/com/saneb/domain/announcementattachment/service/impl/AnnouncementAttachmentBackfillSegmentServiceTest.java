package com.saneb.domain.announcementattachment.service.impl;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.common.error.ApiException;
import com.saneb.domain.announcementattachment.dao.*;
import com.saneb.domain.announcementattachment.dto.*;
import com.saneb.domain.announcementattachment.service.*;
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

class AnnouncementAttachmentBackfillSegmentServiceTest {
    private final AnnouncementAttachmentBackfillSegmentDao dao=mock(AnnouncementAttachmentBackfillSegmentDao.class);
    private final AnnouncementAttachmentBatchDao batchDao=mock(AnnouncementAttachmentBatchDao.class);
    private final AnnouncementAttachmentBatchService batches=mock(AnnouncementAttachmentBatchService.class);
    private final AnnouncementAttachmentBackfillService inventory=mock(AnnouncementAttachmentBackfillService.class);
    private final AnnouncementSourceDao audit=mock(AnnouncementSourceDao.class);
    private final ObjectMapper mapper=new ObjectMapper().findAndRegisterModules();
    private final UUID runId=UUID.randomUUID(),policy=UUID.randomUUID(),rule=UUID.randomUUID(),source=UUID.randomUUID(),actor=UUID.randomUUID(),key=UUID.randomUUID(),batchId=UUID.randomUUID();
    private final OffsetDateTime now=OffsetDateTime.parse("2026-09-12T00:00:00Z");
    private AnnouncementAttachmentBackfillSegmentServiceImpl service;
    private AttachmentBackfillRows.Run run;
    private AttachmentBackfillRows.Segment segment;
    private AttachmentBackfillRows.Item item;
    private AttachmentPolicyRow policyRow;
    private AttachmentBackfillSegmentRows.Link link;
    @BeforeEach void setup() throws Exception {
        policyRow=new AttachmentPolicyRow(policy,"ACTIVE","COLLECT_ONLY",rule,"ACTIVE","a".repeat(64),"{}","[]",0);
        var scope=new AttachmentBackfillRequests.Scope(policy,List.of("BIZINFO"),now.minusDays(1),now,null,null,2);
        run=new AttachmentBackfillRows.Run(runId,policy,mapper.writeValueAsString(Map.of("filter",scope)),mapper.writeValueAsString(policyRow),"c".repeat(64),"d".repeat(64),3L,2,2L,1L,1L,actor,UUID.randomUUID(),"e".repeat(64),now);
        segment=new AttachmentBackfillRows.Segment(runId,1L,2,1,1L);
        item=new AttachmentBackfillRows.Item(1L,source,UUID.randomUUID(),UUID.randomUUID(),rule,"BIZINFO","b".repeat(64),true);
        when(dao.selectRunDetails(eq(runId),anyBoolean())).thenAnswer(call->run);
        when(dao.selectSegmentDetails(eq(runId),eq(1L),anyBoolean())).thenAnswer(call->segment);
        when(dao.selectFixedItemList(runId,1L)).thenAnswer(call->segment.remainingItemCount()==0?List.of():List.of(item));
        when(dao.selectLinkDetails(runId,1L)).thenAnswer(call->link);when(dao.selectRequestDetails(key)).thenAnswer(call->link);
        when(batchDao.selectPolicyDetails(eq(policy),anyBoolean())).thenAnswer(call->policyRow);
        when(batchDao.selectSourceLocks(any())).thenAnswer(call->call.getArgument(0));
        when(batches.selectFixedScopePreview(any(),any(),any())).thenAnswer(call->new AttachmentBatchResponses.Preview(call.getArgument(1),"f".repeat(64),rule,"a".repeat(64),List.of(new AttachmentBatchRows.Bucket("BIZINFO","CANDIDATE",1L)),1L,1,0L,83886080L,132L,0,true,List.of()));
        when(batches.insertFixedBatch(any(),eq(key),any(),any())).thenReturn(new AttachmentBatchResponses.Batch(batchId,policy,"SCOPE_READY","f".repeat(64),0,1,1,0,Map.of("SCOPE_READY",1L),Map.of(),now));
        when(dao.insertLink(any())).thenAnswer(call->{var row=(AttachmentBackfillSegmentRows.Insert)call.getArgument(0);
            link=new AttachmentBackfillSegmentRows.Link(row.runId(),row.segmentNo(),row.batchId(),row.deletedBeforeReservation(),row.expectedRunVersion(),row.segmentHash(),row.requestedBy(),row.idempotencyKey(),row.requestHash(),2,1,"SCOPE_READY",now);return 1;});
        when(inventory.selectRunDetails(any(),eq(runId))).thenReturn(new AttachmentBackfillResponses.Inventory(runId,policy,"INVENTORIED",run.scopeHash(),run.candidateHash(),1,3,2,1,2,2,Map.of(),now));
        when(dao.selectTotals(runId)).thenReturn(new AttachmentBackfillSegmentRows.Totals(2L,1L,1L,1L,0L,0L));
        when(dao.selectOutcomeCounts(runId)).thenReturn(List.of(new AttachmentBackfillSegmentRows.Outcome("COLLECTION","SCOPE_READY",1L),new AttachmentBackfillSegmentRows.Outcome("COLLECTION","UNRESERVED",1L),
                new AttachmentBackfillSegmentRows.Outcome("APPLICATION","NOT_REQUESTED",1L),new AttachmentBackfillSegmentRows.Outcome("APPLICATION","UNRESERVED",1L),
                new AttachmentBackfillSegmentRows.Outcome("ROLLBACK","NOT_REQUESTED",1L),new AttachmentBackfillSegmentRows.Outcome("ROLLBACK","UNRESERVED",1L)));
        service=new AnnouncementAttachmentBackfillSegmentServiceImpl(dao,batchDao,batches,inventory,audit,mapper);
    }
    private Authentication auth(String role) {return auth(role,actor);}
    private Authentication auth(String role,UUID id) {var user=new AuthenticatedUserDetails(new AuthUserDetailsRow(id,"fixture","unused","QA","ACTIVE",false,null,null,null),List.of(role));return UsernamePasswordAuthenticationToken.authenticated(user,null,user.getAuthorities());}
    private AttachmentBackfillSegmentRequests.Reservation request() {var preview=service.selectReservationPreview(auth("ADMIN"),runId,1);return new AttachmentBackfillSegmentRequests.Reservation(preview.runVersion(),preview.segmentHash(),preview.remainingItemCount(),preview.deletedItemCount(),"분할 예약");}
    @Test void previewUsesRemainingFixedIdsAndSeparatesOriginalDeletionAndCurrentHttpCaps() {
        var preview=service.selectReservationPreview(auth("OPERATOR"),runId,1);
        assertThat(preview.originalItemCount()).isEqualTo(2);assertThat(preview.deletedItemCount()).isEqualTo(1);assertThat(preview.remainingItemCount()).isEqualTo(1);assertThat(preview.canReserve()).isTrue();
        assertThat(preview.batchPreview().currentHttpRequests()).isZero();assertThat(preview.batchPreview().maximumHttpRequests()).isEqualTo(132);
        verify(batches).selectFixedScopePreview(any(),any(),eq(new AttachmentBatchRows.FixedScope(runId,1,List.of(source))));
        verify(batchDao,never()).selectCandidateList(any());verify(dao,never()).insertLink(any());verifyNoInteractions(audit);
    }
    @Test void reservationAtomicallyLinksExactBatchAndLocksKeysBeforeSources() {
        var request=request();clearInvocations(dao,batchDao,batches);
        var result=service.insertReservation(auth("ADMIN"),runId,1,key,request);
        assertThat(result.batchId()).isEqualTo(batchId);assertThat(result.originalItemCount()).isEqualTo(2);assertThat(result.reservedItemCount()).isEqualTo(1);assertThat(result.deletedBeforeReservation()).isEqualTo(1);
        assertThat(result.currentBatchStatusCode()).isEqualTo("SCOPE_READY");
        var order=inOrder(dao,batchDao,batches);order.verify(dao).selectRequestLock(key);order.verify(batchDao).selectRequestLock(key);
        order.verify(batchDao).selectSourceLocks(List.of(source));order.verify(batchDao).selectPolicyDetails(policy,true);
        order.verify(dao).selectRunDetails(runId,true);order.verify(dao).selectSegmentDetails(runId,1,true);
        order.verify(batches).insertFixedBatch(any(),eq(key),any(),eq(new AttachmentBatchRows.FixedScope(runId,1,List.of(source))));order.verify(dao).insertLink(any());
        verify(batches,never()).updateCollectionStart(any(),any(),any());verify(batches,never()).insertBatch(any(),any(),any());
    }
    @Test void sameKeyReplayReturnsOriginalLinkWithoutCheckingChangedPolicyOrInputs() {
        var request=request();service.insertReservation(auth("ADMIN"),runId,1,key,request);clearInvocations(dao,batchDao,batches,audit);
        when(dao.selectFixedItemList(runId,1L)).thenReturn(List.of());
        assertThat(service.insertReservation(auth("ADMIN"),runId,1,key,request).batchId()).isEqualTo(batchId);
        verify(dao,never()).selectFixedItemList(any(),anyLong());verify(batchDao,never()).selectSourceLocks(any());verifyNoInteractions(batches,audit);
        assertThatThrownBy(()->service.insertReservation(auth("ADMIN",UUID.randomUUID()),runId,1,key,request)).isInstanceOf(ApiException.class).hasMessageContaining("멱등 키");
        assertThatThrownBy(()->service.insertReservation(auth("ADMIN"),runId,2,key,request)).isInstanceOf(ApiException.class);
    }
    @Test void secondReservationCannotReplaceCancelledOrFailedBatch() {
        var request=request();service.insertReservation(auth("ADMIN"),runId,1,key,request);
        link=new AttachmentBackfillSegmentRows.Link(runId,1L,batchId,1,1L,link.segmentHash(),actor,key,link.requestHash(),2,1,"CANCELLED",now);
        assertThat(service.selectReservationPreview(auth("ADMIN"),runId,1).readinessCode()).isEqualTo("ALREADY_RESERVED");
        assertThatThrownBy(()->service.insertReservation(auth("ADMIN"),runId,1,UUID.randomUUID(),request)).isInstanceOf(ApiException.class).hasMessageContaining("이미 예약");
        verify(dao,times(1)).insertLink(any());
    }
    @Test void changedInputOrPolicyAndAllDeletedSegmentAreNotSilentlySkippedOrFilled() {
        item=new AttachmentBackfillRows.Item(item.ordinal(),source,item.contentVersionId(),item.baseEvaluationId(),rule,"BIZINFO",item.inputHash(),false);
        assertThat(service.selectReservationPreview(auth("ADMIN"),runId,1).readinessCode()).isEqualTo("INPUT_CHANGED");
        item=new AttachmentBackfillRows.Item(item.ordinal(),source,item.contentVersionId(),item.baseEvaluationId(),rule,"BIZINFO",item.inputHash(),true);
        policyRow=new AttachmentPolicyRow(policy,"RETIRED","COLLECT_ONLY",rule,"ACTIVE","a".repeat(64),"{}","[]",1);
        assertThat(service.selectReservationPreview(auth("ADMIN"),runId,1).readinessCode()).isEqualTo("POLICY_CHANGED");
        segment=new AttachmentBackfillRows.Segment(runId,1L,2,2,0L);
        assertThat(service.selectReservationPreview(auth("ADMIN"),runId,1).readinessCode()).isEqualTo("ALL_ITEMS_DELETED");verifyNoInteractions(batches,audit);
    }
    @Test void stalePreviewAndDeletionDuringLockDoNotReserveAnything() {
        var request=request();var changed=new AttachmentBackfillSegmentRequests.Reservation(0L,request.expectedSegmentHash(),1,1,"분할 예약");
        assertThatThrownBy(()->service.insertReservation(auth("ADMIN"),runId,1,key,changed)).isInstanceOf(ApiException.class);
        when(batchDao.selectSourceLocks(any())).thenReturn(List.of());
        assertThatThrownBy(()->service.insertReservation(auth("ADMIN"),runId,1,key,request)).isInstanceOf(ApiException.class).hasMessageContaining("삭제됐습니다");
        verify(batches,never()).insertFixedBatch(any(),any(),any(),any());verify(dao,never()).insertLink(any());
    }
    @Test void mismatchedBatchReceiptOrFailedLinkCannotClaimSuccessfulReservation() {
        var request=request();when(batches.insertFixedBatch(any(),any(),any(),any())).thenReturn(new AttachmentBatchResponses.Batch(batchId,policy,"COLLECTING","f".repeat(64),1,1,1,0,Map.of(),Map.of(),now));
        assertThatThrownBy(()->service.insertReservation(auth("ADMIN"),runId,1,key,request)).isInstanceOf(ApiException.class).hasMessageContaining("결과가");verify(dao,never()).insertLink(any());
        when(batches.insertFixedBatch(any(),any(),any(),any())).thenReturn(new AttachmentBatchResponses.Batch(batchId,policy,"SCOPE_READY","f".repeat(64),0,1,1,0,Map.of(),Map.of(),now));
        doReturn(0).when(dao).insertLink(any());
        assertThatThrownBy(()->service.insertReservation(auth("ADMIN"),runId,1,key,request)).isInstanceOf(ApiException.class).hasMessageContaining("연결에 실패");verifyNoInteractions(audit);
    }
    @Test void summaryHasThreeSeparateDenominatorsAndDoesNotCountUnreservedOrDeletedAsSuccess() {
        var summary=service.selectSummaryDetails(auth("APPROVER"),runId);
        assertThat(summary.candidateCount()).isEqualTo(3);assertThat(summary.deletedItemCount()).isEqualTo(1);assertThat(summary.unreservedItemCount()).isEqualTo(1);
        assertThat(summary.collectionCounts()).containsExactlyInAnyOrderEntriesOf(Map.of("SCOPE_READY",1L,"UNRESERVED",1L));
        assertThat(summary.applicationCounts()).doesNotContainKey("APPLIED");assertThat(summary.rollbackCounts()).doesNotContainKey("ROLLED_BACK");verifyNoInteractions(batches,audit);
    }
    @Test void missingJobOrDimensionCountMismatchIsAConflictNotPartialSummary() {
        when(dao.selectTotals(runId)).thenReturn(new AttachmentBackfillSegmentRows.Totals(2L,1L,1L,1L,0L,1L));
        assertThatThrownBy(()->service.selectSummaryDetails(auth("ADMIN"),runId)).isInstanceOf(ApiException.class);
        when(dao.selectTotals(runId)).thenReturn(new AttachmentBackfillSegmentRows.Totals(2L,1L,1L,1L,0L,0L));when(dao.selectOutcomeCounts(runId)).thenReturn(List.of());
        assertThatThrownBy(()->service.selectSummaryDetails(auth("ADMIN"),runId)).isInstanceOf(ApiException.class).hasMessageContaining("분모");
    }
    @ParameterizedTest @ValueSource(strings={"OPERATOR","APPROVER","USER","PARTNER","REVIEWER"})
    void onlyAdminCanReserve(String role) {var request=request();clearInvocations(dao,batchDao,batches);assertThatThrownBy(()->service.insertReservation(auth(role),runId,1,key,request)).isInstanceOf(ApiException.class);verifyNoInteractions(dao,batchDao,batches,audit);}
    @Test void identityAndRequestValidationCannotReachReservation() {
        assertThatThrownBy(()->service.selectReservationPreview(auth("ADMIN"),runId,0)).isInstanceOf(ApiException.class);
        assertThatThrownBy(()->service.selectReservationPreview(null,runId,1)).isInstanceOf(ApiException.class);
        assertThatThrownBy(()->service.insertReservation(auth("ADMIN"),runId,1,key,null)).isInstanceOf(ApiException.class);
        verifyNoInteractions(dao,batchDao,batches,audit);
    }
}
