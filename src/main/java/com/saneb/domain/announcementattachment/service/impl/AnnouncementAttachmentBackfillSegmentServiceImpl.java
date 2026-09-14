package com.saneb.domain.announcementattachment.service.impl;

import com.fasterxml.jackson.databind.*;
import com.saneb.common.error.*;
import com.saneb.domain.announcementattachment.dao.*;
import com.saneb.domain.announcementattachment.dto.*;
import com.saneb.domain.announcementattachment.service.*;
import com.saneb.domain.announcementattachment.vo.*;
import com.saneb.domain.announcementsource.dao.AnnouncementSourceDao;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceAuditLogCommand;
import com.saneb.domain.auth.vo.AuthenticatedUserDetails;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** 전체 목록 소속과 기존 배치 실행의 연결. 예약은 SCOPE_READY이며 네트워크/현재 근거 적용을 시작하지 않는다. */
@Service
public class AnnouncementAttachmentBackfillSegmentServiceImpl implements AnnouncementAttachmentBackfillSegmentService {
    private final AnnouncementAttachmentBackfillSegmentDao dao;
    private final AnnouncementAttachmentBatchDao batchDao;
    private final AnnouncementAttachmentBatchService batches;
    private final AnnouncementAttachmentBackfillService inventory;
    private final AnnouncementSourceDao audit;
    private final ObjectMapper mapper;
    public AnnouncementAttachmentBackfillSegmentServiceImpl(AnnouncementAttachmentBackfillSegmentDao dao,AnnouncementAttachmentBatchDao batchDao,
            AnnouncementAttachmentBatchService batches,AnnouncementAttachmentBackfillService inventory,AnnouncementSourceDao audit,ObjectMapper mapper) {
        this.dao=dao;this.batchDao=batchDao;this.batches=batches;this.inventory=inventory;this.audit=audit;
        this.mapper=mapper.copy().enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
    }
    @Override @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ,timeout=20)
    public AttachmentBackfillSegmentResponses.Preview selectReservationPreview(Authentication actor,UUID runId,long segmentNo) {
        selectActor(actor,false);validateIdentity(runId,segmentNo);return selectPrepared(actor,runId,segmentNo,false).preview();
    }
    @Override @Transactional(isolation=Isolation.READ_COMMITTED,timeout=30)
    public AttachmentBackfillSegmentResponses.Reservation insertReservation(Authentication authentication,UUID runId,long segmentNo,UUID key,AttachmentBackfillSegmentRequests.Reservation request) {
        UUID actor=selectActor(authentication,true);validateIdentity(runId,segmentNo);validateRequest(key,request);
        String requestHash=hash(Arrays.asList("attachment-ledger-reservation-v1",actor,runId,segmentNo,request.expectedRunVersion(),request.expectedSegmentHash(),
                request.expectedRemainingItemCount(),request.expectedDeletedItemCount(),request.reason().strip()));
        // 기존 batch key도 source보다 먼저 잠근다. 같은 UUID의 일반 예약과 source/key 역순 경합을 만들지 않는다.
        dao.selectRequestLock(key);batchDao.selectRequestLock(key);var existing=dao.selectRequestDetails(key);
        if(existing!=null) {
            if(!actor.equals(existing.requestedBy()) || !runId.equals(existing.runId()) || segmentNo!=existing.segmentNo() || !requestHash.equals(existing.requestHash()))
                throw conflict("이 멱등 키는 다른 운영자·전체 목록·분할·지문·사유에 사용됐습니다. 최초 요청을 변경할 수 없습니다.");
            return receipt(existing);
        }
        var observed=selectRun(runId,false);
        var ids=dao.selectFixedItemList(runId,segmentNo).stream().map(AttachmentBackfillRows.Item::sourceId).sorted().toList();
        if(!ids.isEmpty() && !new HashSet<>(batchDao.selectSourceLocks(ids)).equals(new HashSet<>(ids)))
            throw conflict("분할 잠금 중 원문이 삭제됐습니다. 삭제 건수와 잔여 범위를 다시 확인하세요.");
        batchDao.selectPolicyDetails(observed.policyId(),true);
        var prepared=selectPrepared(authentication,runId,segmentNo,true);var preview=prepared.preview();
        if(!preview.canReserve() || request.expectedRunVersion()!=preview.runVersion() || !request.expectedSegmentHash().equals(preview.segmentHash())
                || request.expectedRemainingItemCount()!=preview.remainingItemCount() || request.expectedDeletedItemCount()!=preview.deletedItemCount())
            throw conflict("분할의 전체 목록 버전·잔여/삭제 건수·입력·정책·준비 상태가 달라졌거나 이미 예약됐습니다. 최신 미리보기를 확인하세요.");
        var batch=batches.insertFixedBatch(authentication,key,new AttachmentBatchRequests.Reservation(prepared.scope(),preview.batchPreview().scopeHash(),request.reason()),prepared.fixed());
        if(!"SCOPE_READY".equals(batch.statusCode()) || batch.itemCount()!=preview.remainingItemCount() || batch.deletedItemCount()!=0
                || !batch.scopeHash().equals(preview.batchPreview().scopeHash())) throw conflict("고정 분할 예약 결과가 확인한 범위와 다릅니다. 전체 요청을 취소했습니다.");
        if(dao.insertLink(new AttachmentBackfillSegmentRows.Insert(runId,segmentNo,batch.batchId(),preview.deletedItemCount(),preview.runVersion(),preview.segmentHash(),
                actor,key,requestHash,hash(request.reason().strip())))!=1)throw conflict("분할과 배치의 연결에 실패했습니다. 일부만 예약하지 않고 전체 요청을 취소했습니다.");
        var link=dao.selectLinkDetails(runId,segmentNo);if(link==null)throw conflict("저장한 분할 예약 영수증을 찾을 수 없습니다.");
        audit.insertAuditLog(new AnnouncementSourceAuditLogCommand(actor,"ATTACHMENT_BACKFILL_SEGMENT_RESERVED","ANNOUNCEMENT_ATTACHMENT_BACKFILL",runId,"SUCCESS",
                json(Map.of("segmentNo",segmentNo,"batchId",batch.batchId(),"reservedCount",preview.remainingItemCount(),"deletedBeforeReservation",preview.deletedItemCount(),"reasonHash",hash(request.reason().strip())))));
        return receipt(link);
    }
    @Override @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ,timeout=30)
    public AttachmentBackfillSegmentResponses.Summary selectSummaryDetails(Authentication actor,UUID runId) {
        selectActor(actor,false);if(runId==null)throw invalid("전체 목록 ID가 필요합니다.");var run=inventory.selectRunDetails(actor,runId);var totals=dao.selectTotals(runId);
        if(totals==null || totals.missingJobCount()!=0 || totals.remainingItemCount()!=run.remainingItemCount()
                || totals.reservedSegmentCount()<0 || totals.reservedSegmentCount()>run.segmentCount()
                || totals.reservedRemainingItemCount()<0 || totals.unreservedItemCount()<0
                || totals.reservedRemainingItemCount()!=run.remainingItemCount()-totals.unreservedItemCount()
                || totals.unreservedInputChangedCount()<0 || totals.unreservedInputChangedCount()>totals.unreservedItemCount())
            throw conflict("전체 분할의 잔여 대상·배치 연결·미예약 건수가 일치하지 않습니다. 일부 결과를 전체 처리로 집계할 수 없습니다.");
        Map<String,Map<String,Long>> dimensions=new TreeMap<>();
        for(String name:List.of("COLLECTION","APPLICATION","ROLLBACK"))dimensions.put(name,new TreeMap<>());
        for(var outcome:dao.selectOutcomeCounts(runId)) {
            var dimension=dimensions.get(outcome.dimensionCode());
            if(dimension==null || outcome.statusCode()==null || "MISSING_JOB".equals(outcome.statusCode()) || outcome.count()==null || outcome.count()<0
                    || dimension.putIfAbsent(outcome.statusCode(),outcome.count())!=null)throw conflict("전체 처리 상태 집계가 중복되거나 올바르지 않습니다.");
        }
        for(var dimension:dimensions.values()) {
            long total=0;try {for(long count:dimension.values())total=Math.addExact(total,count);}catch(ArithmeticException exception){throw conflict("전체 상태 집계가 정수 범위를 초과했습니다.");}
            if(total!=run.remainingItemCount() || dimension.getOrDefault("UNRESERVED",0L).longValue()!=totals.unreservedItemCount())
                throw conflict("수집·적용·원복 각각의 전체 분모가 잔여 대상 수와 다릅니다.");
        }
        return new AttachmentBackfillSegmentResponses.Summary(runId,run.candidateCount(),run.remainingItemCount(),run.deletedItemCount(),run.segmentCount(),totals.reservedSegmentCount(),
                totals.reservedRemainingItemCount(),totals.unreservedItemCount(),totals.unreservedInputChangedCount(),dimensions.get("COLLECTION"),dimensions.get("APPLICATION"),dimensions.get("ROLLBACK"));
    }
    private Prepared selectPrepared(Authentication actor,UUID runId,long segmentNo,boolean lock) {
        var run=selectRun(runId,lock);var segment=dao.selectSegmentDetails(runId,segmentNo,lock);if(segment==null)throw notFound();
        if(segment.itemCount()==null || segment.deletedItemCount()==null || segment.remainingItemCount()==null || segment.remainingItemCount()<0 || segment.remainingItemCount()>1000
                || segment.itemCount()!=segment.remainingItemCount()+segment.deletedItemCount())throw conflict("분할 최초 대상·잔여·삭제 건수가 일치하지 않습니다.");
        var link=dao.selectLinkDetails(runId,segmentNo);
        AttachmentBatchRequests.Scope scope=null;AttachmentBatchRows.FixedScope fixed=null;AttachmentBatchResponses.Preview batchPreview=null;
        List<AttachmentBackfillRows.Item> items=List.of();String readiness="ALREADY_RESERVED";
        if(link==null) {
            items=dao.selectFixedItemList(runId,segmentNo);
            if(items.size()!=segment.remainingItemCount() || items.stream().map(AttachmentBackfillRows.Item::sourceId).distinct().count()!=items.size())throw conflict("고정 분할 소속과 잔여 건수가 다릅니다.");
            if(items.isEmpty())readiness="ALL_ITEMS_DELETED";
            else if(items.stream().anyMatch(item->!Boolean.TRUE.equals(item.currentInputMatches())))readiness="INPUT_CHANGED";
            else {
                var policy=batchDao.selectPolicyDetails(run.policyId(),false);
                try {
                    if(policy==null || !policy.equals(mapper.readValue(run.policySnapshotJson(),AttachmentPolicyRow.class)))readiness="POLICY_CHANGED";
                    else {
                        var filter=mapper.treeToValue(mapper.readTree(run.scopeJson()).path("filter"),AttachmentBackfillRequests.Scope.class);
                        scope=new AttachmentBatchRequests.Scope(run.policyId(),filter.providerCodes(),filter.collectedFrom(),filter.collectedBefore(),filter.deadlineFrom(),filter.deadlineThrough(),run.segmentSize());
                        fixed=new AttachmentBatchRows.FixedScope(runId,segmentNo,items.stream().map(AttachmentBackfillRows.Item::sourceId).toList());
                        batchPreview=batches.selectFixedScopePreview(actor,scope,fixed);readiness=batchPreview.canReserve()?"READY":"BATCH_NOT_READY";
                    }
                } catch(ApiException exception){throw exception;}catch(Exception exception){throw conflict("고정한 정책·분할 범위 이력을 읽을 수 없습니다.");}
            }
        }
        String segmentHash=hash(Arrays.asList("attachment-ledger-segment-v1",runId,segmentNo,run.rowVersion(),run.scopeHash(),segment,items,readiness,
                link==null?null:link.batchId(),batchPreview==null?null:batchPreview.scopeHash()));
        var preview=new AttachmentBackfillSegmentResponses.Preview(runId,segmentNo,run.rowVersion(),segment.itemCount(),segment.remainingItemCount().intValue(),segment.deletedItemCount(),
                segmentHash,readiness,"READY".equals(readiness),link==null?null:link.batchId(),batchPreview);
        return new Prepared(preview,scope,fixed);
    }
    private AttachmentBackfillSegmentResponses.Reservation receipt(AttachmentBackfillSegmentRows.Link link) {
        if(link.originalItemCount()!=link.reservedItemCount()+link.deletedBeforeReservation())throw conflict("예약 영수증의 최초·예약·삭제 건수가 일치하지 않습니다.");
        return new AttachmentBackfillSegmentResponses.Reservation(link.runId(),link.segmentNo(),link.batchId(),link.expectedRunVersion(),link.originalItemCount(),link.reservedItemCount(),
                link.deletedBeforeReservation(),link.segmentHash(),link.currentBatchStatusCode(),link.createdAt());
    }
    private AttachmentBackfillRows.Run selectRun(UUID id,boolean lock) {var run=dao.selectRunDetails(id,lock);if(run==null)throw notFound();return run;}
    private void validateIdentity(UUID runId,long segmentNo) {if(runId==null || segmentNo<1)throw invalid("전체 목록 ID와1이상의 분할 번호가 필요합니다.");}
    private void validateRequest(UUID key,AttachmentBackfillSegmentRequests.Reservation value) {
        if(key==null || value==null || value.expectedRunVersion()==null || value.expectedRunVersion()<0 || value.expectedSegmentHash()==null || !value.expectedSegmentHash().matches("[0-9a-f]{64}")
                || value.expectedRemainingItemCount()==null || value.expectedRemainingItemCount()<1 || value.expectedRemainingItemCount()>1000
                || value.expectedDeletedItemCount()==null || value.expectedDeletedItemCount()<0 || value.expectedDeletedItemCount()>999
                || value.reason()==null || value.reason().isBlank() || value.reason().length()>1000)
            throw invalid("UUID 멱등 키·목록 버전·64자리 분할 지문·잔여1~1000건·삭제0~999건·사유1~1000자를 확인하세요.");
    }
    private UUID selectActor(Authentication authentication,boolean write) {
        if(authentication==null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof AuthenticatedUserDetails actor))
            throw new ApiException(ErrorCode.AUTH_REQUIRED,HttpStatus.UNAUTHORIZED,"로그인한 운영 계정이 필요합니다.");
        if(!actor.isEnabled() || actor.passwordResetRequired() || actor.roles().stream().noneMatch((write?Set.of("ADMIN"):Set.of("ADMIN","OPERATOR","APPROVER"))::contains))
            throw new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_ACTION_FORBIDDEN,HttpStatus.FORBIDDEN,write?"분할 예약은 활성 ADMIN만 가능합니다.":"분할 조회는 활성 ADMIN·OPERATOR·APPROVER만 가능합니다.");
        return actor.userId();
    }
    private record Prepared(AttachmentBackfillSegmentResponses.Preview preview,AttachmentBatchRequests.Scope scope,AttachmentBatchRows.FixedScope fixed) { }
    private String json(Object value) {try{return mapper.writeValueAsString(value);}catch(Exception exception){throw conflict("분할 지문을 직렬화할 수 없습니다.");}}
    private String hash(Object value) {try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json(value).getBytes(StandardCharsets.UTF_8)));}catch(Exception exception){throw conflict("분할 지문을 계산할 수 없습니다.");}}
    private ApiException invalid(String message){return new ApiException(ErrorCode.VALIDATION_FAILED,HttpStatus.BAD_REQUEST,message);}
    private ApiException conflict(String message){return new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_VERSION_CONFLICT,HttpStatus.CONFLICT,message);}
    private ApiException notFound(){return new ApiException(ErrorCode.RESOURCE_NOT_FOUND,HttpStatus.NOT_FOUND,"해당 전체 목록 또는 분할을 찾을 수 없습니다.");}
}
