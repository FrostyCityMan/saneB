package com.saneb.domain.announcementattachment.service.impl;

import com.fasterxml.jackson.databind.*;
import com.saneb.common.error.*;
import com.saneb.common.response.PageResponse;
import com.saneb.domain.announcementattachment.dao.*;
import com.saneb.domain.announcementattachment.dto.*;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentBackfillService;
import com.saneb.domain.announcementattachment.vo.*;
import com.saneb.domain.announcementsource.dao.AnnouncementSourceDao;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceAuditLogCommand;
import com.saneb.domain.auth.vo.AuthenticatedUserDetails;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.ZoneOffset;
import java.util.*;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** 전체 목록 고정과 분할 탐색. worker/job/네트워크 서비스에는 의존하지 않는다. */
@Service
public class AnnouncementAttachmentBackfillServiceImpl implements AnnouncementAttachmentBackfillService {
    private final AnnouncementAttachmentBackfillDao dao;
    private final AnnouncementAttachmentBatchDao batches;
    private final AnnouncementSourceDao audit;
    private final ObjectMapper mapper;
    public AnnouncementAttachmentBackfillServiceImpl(AnnouncementAttachmentBackfillDao dao,AnnouncementAttachmentBatchDao batches,
            AnnouncementSourceDao audit,ObjectMapper mapper) {
        this.dao=dao;this.batches=batches;this.audit=audit;
        this.mapper=mapper.copy().enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
    }
    @Override @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ,timeout=30)
    public AttachmentBackfillResponses.Preview selectScopePreview(Authentication actor,AttachmentBackfillRequests.Scope scope) {
        selectActor(actor,false);return selectPrepared(normalize(scope)).preview();
    }
    @Override @Transactional(isolation=Isolation.REPEATABLE_READ,timeout=30)
    public AttachmentBackfillResponses.Inventory insertInventory(Authentication authentication,UUID key,AttachmentBackfillRequests.Inventory request) {
        UUID actor=selectActor(authentication,true);
        if(key==null || request==null || request.expectedScopeHash()==null || !request.expectedScopeHash().matches("[0-9a-f]{64}")
                || request.expectedCandidateCount()==null || request.expectedCandidateCount()<1)
            throw invalid("UUID 멱등 키와 조회한 전체 범위 지문·1 이상의 전체 후보 수가 필요합니다.");
        if(request.reason()==null || request.reason().isBlank() || request.reason().length()>1000)
            throw invalid("전체 목록 고정 사유를 공백이 아닌 1~1000자로 입력하세요.");
        var scope=normalize(request.scope());
        var requestHash=hash(Arrays.asList("attachment-full-inventory-v1",actor,scope,request.expectedScopeHash(),request.expectedCandidateCount(),request.reason().strip()));
        if(!dao.selectRequestLock(key)) throw conflict("같은 멱등 키의 목록 고정이 처리 중입니다. 입력을 바꾸지 말고 잠시 후 같은 키로 다시 조회·요청하세요.");
        var existing=dao.selectRequestDetails(key);
        if(existing!=null) {
            if(!actor.equals(existing.requestedBy()) || !requestHash.equals(existing.requestHash()))
                throw conflict("이 멱등 키는 다른 운영자·범위·후보 수·사유에 사용됐습니다. 최초 요청을 변경할 수 없습니다.");
            return response(existing);
        }
        var prepared=selectPrepared(scope);var preview=prepared.preview();
        if(!preview.canInventory() || !request.expectedScopeHash().equals(preview.scopeHash()) || request.expectedCandidateCount()!=preview.candidateCount())
            throw conflict("미리보기 이후 전체 후보·입력·분할 크기·정책 또는 보호 제외 건수가 바뀌었습니다. 전체 범위를 다시 확인하세요.");
        UUID runId=UUID.randomUUID();
        var frozenScope=Map.of("schemaVersion",1,"filter",scope,"counts",preview.counts(),"inventoryOnly",true,"previewHttpRequests",0);
        try {
            if(dao.insertRun(new AttachmentBackfillRows.Insert(runId,scope.policyId(),json(frozenScope),json(prepared.policy()),preview.scopeHash(),preview.candidateHash(),
                    preview.candidateCount(),scope.segmentSize(),preview.segmentCount(),actor,key,requestHash,hash(request.reason().strip())))!=1)
                throw conflict("전체 목록 관리 기록 저장에 실패했습니다. 일부만 저장하지 않고 취소했습니다.");
            var command=new AttachmentBackfillRows.Materialize(runId,scope);
            if(dao.insertSegments(command)!=preview.segmentCount() || dao.insertItems(command)!=preview.candidateCount())
                throw conflict("고정 분할·전체 후보 수가 미리보기와 다릅니다. 일부만 저장하지 않고 취소했습니다.");
            var result=response(selectRun(runId));
            audit.insertAuditLog(new AnnouncementSourceAuditLogCommand(actor,"ATTACHMENT_BACKFILL_INVENTORIED","ANNOUNCEMENT_ATTACHMENT_BACKFILL",runId,
                    "SUCCESS",json(Map.of("reasonHash",hash(request.reason().strip()),"candidateCount",preview.candidateCount(),"segmentCount",preview.segmentCount()))));
            return result;
        } catch(ConcurrencyFailureException | DataIntegrityViolationException exception) {
            throw conflict("전체 목록 고정 중 원문 삭제·중복 요청 또는 무결성 충돌이 발생했습니다. 입력을 보존하고 같은 멱등 키로 결과를 다시 확인하세요.");
        }
    }
    @Override @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ,timeout=20)
    public AttachmentBackfillResponses.Inventory selectRunDetails(Authentication actor,UUID runId) {selectActor(actor,false);return response(selectRun(runId));}
    @Override @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ,timeout=20)
    public PageResponse<AttachmentBackfillResponses.Inventory> selectRunList(Authentication actor,int page,int size) {
        selectActor(actor,false);var search=search(null,null,page,size);long total=dao.selectRunCount();validatePageTotal(total,size);
        return PageResponse.of(dao.selectRunList(search).stream().map(this::response).toList(),page,size,total);
    }
    @Override @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ,timeout=20)
    public PageResponse<AttachmentBackfillRows.Segment> selectSegmentList(Authentication actor,UUID runId,int page,int size) {
        selectActor(actor,false);var search=search(runId,null,page,size);var run=selectRun(runId);validatePageTotal(run.segmentCount(),size);
        var rows=dao.selectSegmentList(search);rows.forEach(this::validateSegment);
        return PageResponse.of(rows,page,size,run.segmentCount());
    }
    @Override @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ,timeout=20)
    public PageResponse<AttachmentBackfillRows.Item> selectItemList(Authentication actor,UUID runId,long segmentNo,int page,int size) {
        selectActor(actor,false);if(segmentNo<1)throw invalid("분할 번호는 1 이상이어야 합니다.");
        var search=search(runId,segmentNo,page,size);selectRun(runId);
        var segment=dao.selectSegmentDetails(runId,segmentNo);if(segment==null)throw notFound();validateSegment(segment);
        return PageResponse.of(dao.selectItemList(search),page,size,segment.remainingItemCount());
    }
    private Prepared selectPrepared(AttachmentBackfillRequests.Scope scope) {
        var policy=batches.selectPolicyDetails(scope.policyId(),false);
        if(policy==null)throw notFound();
        if(!"ACTIVE".equals(policy.policyStatusCode()) || !"ACTIVE".equals(policy.releaseStatusCode())
                || !Set.of("COLLECT_ONLY","ENFORCE").contains(policy.modeCode()) || policy.policyHash()==null || !policy.policyHash().matches("[0-9a-f]{64}"))
            throw new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_POLICY_NOT_ACTIVE,HttpStatus.CONFLICT,"현재 ACTIVE 규칙에 연결된 게시 COLLECT_ONLY 또는 ENFORCE 정책이 필요합니다.");
        var counts=batches.selectScopeCounts(new AttachmentBatchRequests.Scope(scope.policyId(),scope.providerCodes(),scope.collectedFrom(),scope.collectedBefore(),
                scope.deadlineFrom(),scope.deadlineThrough(),scope.segmentSize())).stream().map(AttachmentProviderScope::selectScopeBucket).toList();
        long candidateCount=0;
        var unique=new HashSet<String>();
        for(var bucket:counts) {
            if(bucket.count()==null || bucket.count()<0 || !scope.providerCodes().contains(bucket.providerCode())
                    || !Set.of("CANDIDATE","TITLE_OR_BASE_NOT_ELIGIBLE","LINKED_PROTECTED").contains(bucket.reasonCode())
                    || !unique.add(bucket.providerCode()+":"+bucket.reasonCode())) throw conflict("전체 범위 집계 항목이 중복되거나 올바르지 않습니다.");
            if("CANDIDATE".equals(bucket.reasonCode())) {
                try {candidateCount=Math.addExact(candidateCount,bucket.count());}catch(ArithmeticException exception){throw conflict("전체 후보 집계가 정수 표현 범위를 초과했습니다. 처리 범위를 재검토하세요.");}
            }
        }
        var digest=dao.selectScopeDigest(scope);
        if(digest==null || digest.candidateCount()==null || digest.candidateCount()!=candidateCount || digest.candidateHash()==null || !digest.candidateHash().matches("[0-9a-f]{64}"))
            throw conflict("전체 집계와 실제 후보 지문 건수가 다릅니다. 범위를 축소하지 않고 조회를 취소했습니다.");
        long segments=candidateCount==0?0:(candidateCount-1)/scope.segmentSize()+1;
        var scopeHash=hash(Arrays.asList("attachment-full-inventory-v1",scope,policy,counts,digest));
        return new Prepared(new AttachmentBackfillResponses.Preview(scope,scopeHash,digest.candidateHash(),policy.ruleReleaseId(),policy.policyHash(),
                List.copyOf(counts),candidateCount,segments,0,candidateCount>0),policy);
    }
    private AttachmentBackfillResponses.Inventory response(AttachmentBackfillRows.Run row) {
        var totals=dao.selectRunTotals(row.runId());
        if(totals==null || totals.remainingItemCount()==null || totals.remainingItemCount()<0 || row.deletedItemCount()<0
                || totals.remainingItemCount()!=row.candidateCount()-row.deletedItemCount()
                || !Objects.equals(totals.segmentCount(),row.segmentCount()) || !Objects.equals(totals.initialSegmentItemCount(),row.candidateCount())
                || !Objects.equals(totals.deletedSegmentItemCount(),row.deletedItemCount()))
            throw conflict("최초 전체 후보·분할·현재 잔여·삭제 건수가 일치하지 않습니다. 처리 완료로 집계할 수 없습니다.");
        try {
            Map<String,Object> scope=mapper.readValue(row.scopeJson(),mapper.getTypeFactory().constructMapType(Map.class,String.class,Object.class));
            return new AttachmentBackfillResponses.Inventory(row.runId(),row.policyId(),"INVENTORIED",row.scopeHash(),row.candidateHash(),row.rowVersion(),
                    row.candidateCount(),totals.remainingItemCount(),row.deletedItemCount(),row.segmentSize(),row.segmentCount(),scope,row.createdAt());
        } catch(Exception exception) {throw conflict("고정한 전체 범위 이력을 읽을 수 없습니다.");}
    }
    private void validateSegment(AttachmentBackfillRows.Segment row) {
        if(row.itemCount()==null || row.deletedItemCount()==null || row.remainingItemCount()==null || row.remainingItemCount()<0
                || row.itemCount()!=row.remainingItemCount()+row.deletedItemCount()) throw conflict("분할 최초 대상·잔여·삭제 건수가 일치하지 않습니다.");
    }
    private AttachmentBackfillRows.Run selectRun(UUID id) {var row=id==null?null:dao.selectRunDetails(id);if(row==null)throw notFound();return row;}
    private AttachmentBackfillRows.Search search(UUID runId,Long segmentNo,int page,int size) {
        if(page<1 || size<1 || size>100)throw invalid("페이지는 1 이상, 페이지 크기는 1~100이어야 합니다.");
        return new AttachmentBackfillRows.Search(runId,segmentNo,size,((long)page-1)*size);
    }
    private void validatePageTotal(long total,int size) {
        if(total<0 || total>(long)Integer.MAX_VALUE*size)throw conflict("전체 페이지 수가 API 표현 범위를 초과했습니다. 전체 건수를 잘라 반환하지 않습니다.");
    }
    private AttachmentBackfillRequests.Scope normalize(AttachmentBackfillRequests.Scope value) {
        if(value==null || value.policyId()==null || value.providerCodes()==null || value.providerCodes().isEmpty() || value.providerCodes().size()>3
                || value.providerCodes().stream().anyMatch(p->p==null || !Set.of("BIZINFO","GOV24","LOCAL_GOV_NOTICE").contains(p))
                || value.providerCodes().stream().distinct().count()!=value.providerCodes().size())throw invalid("정책과 중복 없는 지원 출처 1~3개를 지정하세요.");
        if(value.collectedFrom()==null || value.collectedBefore()==null || !value.collectedFrom().isBefore(value.collectedBefore())
                || (value.deadlineFrom()!=null && value.deadlineThrough()!=null && value.deadlineFrom().isAfter(value.deadlineThrough()))
                || value.segmentSize()==null || value.segmentSize()<1 || value.segmentSize()>1000)
            throw invalid("수집 시작은 종료보다 이전이어야 하며 마감일 범위는 역전될 수 없습니다. 분할 크기는 1~1000입니다. 전체 건수 상한이 아닙니다.");
        return new AttachmentBackfillRequests.Scope(value.policyId(),value.providerCodes().stream().sorted().toList(),value.collectedFrom().withOffsetSameInstant(ZoneOffset.UTC),
                value.collectedBefore().withOffsetSameInstant(ZoneOffset.UTC),value.deadlineFrom(),value.deadlineThrough(),value.segmentSize());
    }
    private UUID selectActor(Authentication authentication,boolean change) {
        if(authentication==null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof AuthenticatedUserDetails actor))
            throw new ApiException(ErrorCode.AUTH_REQUIRED,HttpStatus.UNAUTHORIZED,"로그인한 운영 계정이 필요합니다.");
        if(!actor.isEnabled() || actor.passwordResetRequired() || actor.roles().stream().noneMatch((change?Set.of("ADMIN"):Set.of("ADMIN","OPERATOR","APPROVER"))::contains))
            throw new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_ACTION_FORBIDDEN,HttpStatus.FORBIDDEN,change?"전체 목록 고정은 활성 ADMIN만 가능합니다.":"전체 범위는 활성 ADMIN·OPERATOR·APPROVER만 조회할 수 있습니다.");
        return actor.userId();
    }
    private String json(Object value) {try{return mapper.writeValueAsString(value);}catch(Exception exception){throw conflict("전체 범위 지문을 직렬화할 수 없습니다.");}}
    private String hash(Object value) {try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json(value).getBytes(StandardCharsets.UTF_8)));}catch(Exception exception){throw conflict("전체 범위 지문을 계산할 수 없습니다.");}}
    private record Prepared(AttachmentBackfillResponses.Preview preview,AttachmentPolicyRow policy) { }
    private ApiException invalid(String message){return new ApiException(ErrorCode.VALIDATION_FAILED,HttpStatus.BAD_REQUEST,message);}
    private ApiException conflict(String message){return new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_VERSION_CONFLICT,HttpStatus.CONFLICT,message);}
    private ApiException notFound(){return new ApiException(ErrorCode.RESOURCE_NOT_FOUND,HttpStatus.NOT_FOUND,"해당 전체 목록·분할 또는 정책을 찾을 수 없습니다.");}
}
