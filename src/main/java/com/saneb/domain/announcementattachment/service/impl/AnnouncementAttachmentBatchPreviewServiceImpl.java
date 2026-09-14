package com.saneb.domain.announcementattachment.service.impl;

import com.fasterxml.jackson.databind.*;
import com.saneb.common.error.*;
import com.saneb.common.response.PageResponse;
import com.saneb.domain.announcementattachment.dao.*;
import com.saneb.domain.announcementattachment.dto.*;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentBatchPreviewService;
import com.saneb.domain.announcementattachment.vo.*;
import com.saneb.domain.announcementsource.dao.AnnouncementSourceDao;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceAuditLogCommand;
import com.saneb.domain.auth.vo.AuthenticatedUserDetails;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** 저장된 봉인 근거의 승인용 snapshot. HTTP/추출기/현재 판정 적용 서비스에 의존하지 않는다. */
@Service
public class AnnouncementAttachmentBatchPreviewServiceImpl implements AnnouncementAttachmentBatchPreviewService {
    private static final Set<String> PREVIEW_STATES=Set.of("PREVIEW_READY","PREVIEW_PARTIAL_FAILED");
    private static final Set<String> READY_STATES=Set.of("COLLECTED","COLLECTION_PARTIAL_FAILED","PREVIEW_READY","PREVIEW_PARTIAL_FAILED");
    private static final Set<String> TERMINAL=Set.of("SUCCEEDED","PARTIAL_FAILED","FAILED","CONFLICT","CANCELLED");
    private final AnnouncementAttachmentBatchPreviewDao dao;
    private final AnnouncementAttachmentBatchDao batches;
    private final AnnouncementSourceDao audit;
    private final ObjectMapper mapper;
    private final AttachmentBatchFingerprint fingerprint;
    public AnnouncementAttachmentBatchPreviewServiceImpl(AnnouncementAttachmentBatchPreviewDao dao,AnnouncementAttachmentBatchDao batches,
            AnnouncementSourceDao audit,ObjectMapper mapper) {
        this.dao=dao;this.batches=batches;this.audit=audit;this.mapper=mapper.copy().enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
        this.fingerprint=new AttachmentBatchFingerprint(this.mapper);
    }
    @Override @Transactional(timeout=30)
    public AttachmentBatchPreviewResponses.Preview insertPreview(Authentication authentication,UUID batchId,UUID key,AttachmentBatchPreviewRequests.Preparation request) {
        UUID actor=selectActor(authentication,true);
        if(request==null)throw invalid("미리보기 생성 입력이 필요합니다.");
        validateRequest(key,request.expectedVersion(),request.expectedScopeHash(),request.reason());
        String requestHash=hash(Arrays.asList("attachment-batch-preview-v1",actor,batchId,request.expectedVersion(),request.expectedScopeHash(),request.reason().strip()));
        var existing=selectIdempotent(key,actor,batchId,requestHash);if(existing!=null)return response(existing);
        var locked=selectLocked(batchId);var batch=locked.batch();validateVersion(batch,request.expectedVersion(),READY_STATES);
        if(!batch.scopeHash().equals(request.expectedScopeHash()))throw conflict("고정 범위 지문이 다릅니다. 배치 상세를 다시 조회하세요.");
        var live=selectLive(batch);return insertSnapshot(actor,key,requestHash,request.reason(),locked,live,List.of());
    }
    @Override @Transactional(timeout=30)
    public AttachmentBatchPreviewResponses.Preview updateSelection(Authentication authentication,UUID batchId,UUID key,AttachmentBatchPreviewRequests.Selection request) {
        UUID actor=selectActor(authentication,true);
        if(request==null)throw invalid("미리보기 선택 입력이 필요합니다.");
        validateRequest(key,request.expectedVersion(),request.expectedPreviewHash(),request.reason());
        if(request.selectedJobIds()==null || request.selectedJobIds().size()>1000 || request.selectedJobIds().stream().anyMatch(Objects::isNull)
                || request.selectedJobIds().stream().distinct().count()!=request.selectedJobIds().size())
            throw invalid("선택 작업은 중복 없는 UUID 목록으로 최대 1000개입니다. 전체 선택 해제는 빈 목록을 입력하세요.");
        var selected=request.selectedJobIds().stream().sorted().toList();
        String requestHash=hash(Arrays.asList("attachment-batch-preview-selection-v1",actor,batchId,request.expectedVersion(),request.expectedPreviewHash(),selected,request.reason().strip()));
        var existing=selectIdempotent(key,actor,batchId,requestHash);if(existing!=null)return response(existing);
        var locked=selectLocked(batchId);var batch=locked.batch();validateVersion(batch,request.expectedVersion(),PREVIEW_STATES);
        var previous=dao.selectCurrentPreviewDetails(batchId);
        if(previous==null || !request.expectedPreviewHash().equals(previous.previewHash()) || !Objects.equals(previous.batchVersion(),batch.rowVersion()))
            throw conflict("선택 기준 미리보기나 배치 버전이 바뀌었습니다. 현재 미리보기를 다시 확인하세요.");
        var live=selectLive(batch);
        if(!previous.inputHash().equals(inputHash(batch,locked.policy(),live)))
            throw conflict("미리보기 이후 source·봉인 근거·검수·정책 입력이 바뀌었습니다. 새 미리보기를 명시적으로 생성하세요.");
        Set<UUID> eligible=new HashSet<>();live.stream().filter(i->"READY".equals(readiness(i))).forEach(i->eligible.add(i.jobId()));
        if(!eligible.containsAll(selected)) throw conflict("선택한 작업에 다른 배치·실패·불완전 근거·변경된 원문이 있습니다. 이 미리보기의 READY 항목만 선택하세요.");
        return insertSnapshot(actor,key,requestHash,request.reason(),locked,live,selected);
    }
    @Override @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ,timeout=15)
    public AttachmentBatchPreviewResponses.Preview selectCurrentPreviewDetails(Authentication actor,UUID batchId) {
        selectActor(actor,false);selectBatch(batchId,false);var preview=dao.selectCurrentPreviewDetails(batchId);if(preview==null)throw notFound();return response(preview);
    }
    @Override @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ,timeout=15)
    public AttachmentBatchPreviewResponses.Preview selectPreviewDetails(Authentication actor,UUID batchId,UUID previewId) {
        selectActor(actor,false);return response(selectPreview(batchId,previewId));
    }
    @Override @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ,timeout=15)
    public PageResponse<AttachmentBatchPreviewResponses.Item> selectItemList(Authentication actor,UUID batchId,UUID previewId,int page,int size) {
        selectActor(actor,false);selectPreview(batchId,previewId);
        if(page<1 || size<1 || size>100 || ((long)page-1)*size>Integer.MAX_VALUE)throw invalid("페이지는 1 이상, 크기는 1~100이어야 합니다.");
        var rows=dao.selectItemList(new AttachmentBatchPreviewRows.Search(previewId,size,(page-1)*size));
        return PageResponse.of(rows.stream().map(i->new AttachmentBatchPreviewResponses.Item(i.jobId(),i.sourceId(),i.providerCode(),i.readinessCode(),
                Boolean.TRUE.equals(i.eligible()),Boolean.TRUE.equals(i.selected()),metadata(i.evidenceJson()))).toList(),page,size,dao.selectItemCount(previewId));
    }
    private AttachmentBatchPreviewResponses.Preview insertSnapshot(UUID actor,UUID key,String requestHash,String reason,Locked locked,
            List<AttachmentBatchPreviewRows.LiveItem> live,List<UUID> selected) {
        var batch=locked.batch();int eligible=(int)live.stream().filter(i->"READY".equals(readiness(i))).count();
        String status=eligible==batch.itemCount()?"PREVIEW_READY":"PREVIEW_PARTIAL_FAILED";
        String inputHash=inputHash(batch,locked.policy(),live);UUID previewId=UUID.randomUUID();
        int finalVersion=batch.rowVersion()+2;
        String previewHash=hash(Arrays.asList("attachment-batch-preview-snapshot-v1",batch.batchId(),finalVersion,batch.scopeHash(),inputHash,selected));
        var preview=new AttachmentBatchPreviewRows.Preview(previewId,batch.batchId(),status,batch.scopeHash(),inputHash,previewHash,finalVersion,
                batch.itemCount(),live.size(),batch.deletedItemCount(),eligible,selected.size(),actor,key,requestHash,hash(reason.strip()),null);
        requireOne(dao.updatePreviewRunning(batch.batchId(),batch.rowVersion()));requireOne(dao.insertPreview(preview));
        var selectedSet=new HashSet<>(selected);
        for(var item:live) {
            String ready=readiness(item);var evidence=metadata(item.evidenceJson());
            evidence.put("jobStatusCode",item.jobStatusCode());evidence.put("jobErrorCode",item.jobErrorCode());
            insertTagDifferences(evidence);
            String evidenceJson=json(evidence);if(evidenceJson.getBytes(StandardCharsets.UTF_8).length>60000)throw conflict("항목별 근거 요약이 저장 한도를 넘었습니다. 배치를 적용하지 말고 근거 구성을 확인하세요.");
            requireOne(dao.insertItem(new AttachmentBatchPreviewRows.ItemInsert(previewId,batch.batchId(),item.jobId(),ready,"READY".equals(ready),selectedSet.contains(item.jobId()),hash(item),evidenceJson)));
        }
        if(dao.updateJobSelection(batch.batchId(),selected)!=live.size())throw conflict("선택 저장 중 고정 항목이 달라졌습니다. 일부만 선택하지 않고 전체 요청을 취소했습니다.");
        requireOne(dao.updatePreviewReady(batch.batchId(),batch.rowVersion()+1,status,previewHash,previewId));
        audit.insertAuditLog(new AnnouncementSourceAuditLogCommand(actor,"ATTACHMENT_BATCH_PREVIEW_SAVED","ANNOUNCEMENT_ATTACHMENT_BATCH",batch.batchId(),"SUCCESS",
                json(Map.of("previewId",previewId,"previewHash",previewHash,"selectedCount",selected.size(),"reasonHash",hash(reason.strip())))));
        return response(selectPreview(batch.batchId(),previewId));
    }
    private AttachmentBatchPreviewResponses.Preview response(AttachmentBatchPreviewRows.Preview preview) {
        var batch=selectBatch(preview.batchId(),false);var current=dao.selectCurrentPreviewDetails(preview.batchId());
        int available=Math.toIntExact(dao.selectItemCount(preview.previewId()));
        boolean currentPointer=current!=null && preview.previewId().equals(current.previewId());
        boolean versionsCurrent=Objects.equals(preview.batchVersion(),batch.rowVersion());
        var policy=batches.selectPolicyDetails(batch.policyId(),false);
        boolean inputsCurrent=currentPointer && versionsCurrent && available==preview.remainingItemCount()
                && policyMatches(batch,policy) && preview.inputHash().equals(inputHash(batch,policy,selectLive(batch)));
        return new AttachmentBatchPreviewResponses.Preview(preview.previewId(),preview.batchId(),preview.statusCode(),preview.scopeHash(),preview.inputHash(),preview.previewHash(),
                preview.batchVersion(),batch.rowVersion(),preview.itemCount(),preview.remainingItemCount(),preview.deletedItemCount(),available,batch.deletedItemCount(),
                preview.eligibleItemCount(),preview.selectedItemCount(),currentPointer,inputsCurrent,0,preview.createdAt());
    }
    private Locked selectLocked(UUID id) {
        var before=selectBatch(id,false);var ids=batches.selectItemList(new AttachmentBatchRows.Search(id,1000,0)).stream().map(AttachmentBatchRows.Item::sourceId).sorted().toList();
        if(!ids.isEmpty() && !new HashSet<>(batches.selectSourceLocks(ids)).equals(new HashSet<>(ids)))throw conflict("미리보기 잠금 중 원문이 삭제됐습니다. 현재 범위와 삭제 건수를 확인하세요.");
        var policy=batches.selectPolicyDetails(before.policyId(),true);var batch=selectBatch(id,true);
        if(!policyMatches(batch,policy))throw conflict("현재 ACTIVE 정책·규칙이 수집 시 고정 정책과 다릅니다. 새 정책으로 미리보기나 선택을 자동 교체하지 않습니다.");
        return new Locked(batch,policy);
    }
    private List<AttachmentBatchPreviewRows.LiveItem> selectLive(AttachmentBatchRows.Row batch) {
        var live=dao.selectLiveItemList(batch.batchId());
        if(live.size()>1000 || live.size()+batch.deletedItemCount()!=batch.itemCount() || live.stream().map(AttachmentBatchPreviewRows.LiveItem::jobId).distinct().count()!=live.size())
            throw conflict("고정 대상 수와 현재 작업·삭제 건수 합계가 다릅니다. 전체 범위를 확인하세요.");
        if(live.stream().anyMatch(i->!TERMINAL.contains(i.jobStatusCode())))throw conflict("아직 끝나지 않은 수집 작업이 있습니다. 수집 종료 후 미리보기를 생성하세요.");
        return live.stream().sorted(Comparator.comparing(AttachmentBatchPreviewRows.LiveItem::jobId)).toList();
    }
    private String readiness(AttachmentBatchPreviewRows.LiveItem item) {
        if(!"SUCCEEDED".equals(item.jobStatusCode()))return "COLLECTION_NOT_SUCCESSFUL";
        if(!Boolean.TRUE.equals(item.evidenceComplete()))return "EVIDENCE_INCOMPLETE";
        if(Boolean.TRUE.equals(item.protectedLink()))return "PROTECTED_LINK";
        if(Boolean.TRUE.equals(item.activeOtherJob()))return "ACTIVE_JOB";
        if(!Boolean.TRUE.equals(item.frozenSourceCurrent()))return "SOURCE_CHANGED";
        return "READY";
    }
    private String inputHash(AttachmentBatchRows.Row batch,AttachmentPolicyRow policy,List<AttachmentBatchPreviewRows.LiveItem> live) {
        return hash(Arrays.asList("attachment-batch-preview-input-v1",batch.scopeHash(),batch.itemCount(),batch.deletedItemCount(),policy,live));
    }
    private boolean policyMatches(AttachmentBatchRows.Row batch,AttachmentPolicyRow policy) {
        try {return policy!=null && "ACTIVE".equals(policy.policyStatusCode()) && "ACTIVE".equals(policy.releaseStatusCode())
                && Set.of("COLLECT_ONLY","ENFORCE").contains(policy.modeCode()) && policy.equals(mapper.readValue(batch.policySnapshotJson(),AttachmentPolicyRow.class));}
        catch(Exception exception){return false;}
    }
    private AttachmentBatchPreviewRows.Preview selectIdempotent(UUID key,UUID actor,UUID batchId,String requestHash) {
        dao.selectRequestLock(key);var value=dao.selectRequestDetails(key);
        if(value!=null && (!actor.equals(value.actorId()) || !batchId.equals(value.batchId()) || !requestHash.equals(value.requestHash())))
            throw conflict("이 멱등 키는 다른 운영자·배치·요청에 사용됐습니다. 동일 요청에만 재사용하세요.");
        return value;
    }
    private void validateRequest(UUID key,Integer version,String fingerprint,String reason) {
        if(key==null || version==null || version<0 || version>Integer.MAX_VALUE-2 || fingerprint==null || !fingerprint.matches("[0-9a-f]{64}"))
            throw invalid("UUID 멱등 키·0~2147483645 배치 버전·64자리 지문이 필요합니다.");
        if(reason==null || reason.isBlank() || reason.length()>1000)throw invalid("사유는 공백이 아닌 1~1000자로 입력하세요.");
    }
    private void validateVersion(AttachmentBatchRows.Row batch,int version,Set<String> states) {
        if(!states.contains(batch.statusCode()) || batch.rowVersion()!=version)throw conflict("배치 단계 또는 버전이 바뀌었습니다. 수집 종료/현재 미리보기와 최신 버전을 확인하세요.");
    }
    private AttachmentBatchRows.Row selectBatch(UUID id,boolean lock) {var value=id==null?null:batches.selectBatchDetails(id,lock);if(value==null)throw notFound();return value;}
    private AttachmentBatchPreviewRows.Preview selectPreview(UUID batchId,UUID id) {var value=id==null?null:dao.selectPreviewDetails(batchId,id);if(value==null)throw notFound();return value;}
    private UUID selectActor(Authentication authentication,boolean change) {
        if(authentication==null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof AuthenticatedUserDetails actor))
            throw new ApiException(ErrorCode.AUTH_REQUIRED,HttpStatus.UNAUTHORIZED,"로그인한 운영 계정이 필요합니다.");
        if(!actor.isEnabled() || actor.passwordResetRequired() || actor.roles().stream().noneMatch((change?Set.of("ADMIN"):Set.of("ADMIN","OPERATOR","APPROVER"))::contains))
            throw new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_ACTION_FORBIDDEN,HttpStatus.FORBIDDEN,change?"미리보기 생성·선택 변경은 활성 ADMIN만 가능합니다.":"미리보기는 활성 ADMIN·OPERATOR·APPROVER만 조회할 수 있습니다.");
        return actor.userId();
    }
    private Map<String,Object> metadata(String text) {
        try {if(text==null || text.getBytes(StandardCharsets.UTF_8).length>65536)throw new IllegalArgumentException();
            return mapper.readValue(text,mapper.getTypeFactory().constructMapType(TreeMap.class,String.class,Object.class));
        }catch(Exception exception){throw conflict("저장된 항목 근거 요약을 읽을 수 없습니다.");}
    }
    private void insertTagDifferences(Map<String,Object> values) {
        for(String kind:List.of("Target","Support"))for(String previous:List.of("base","previous","confirmed")) {
            var from=codes(values.get(previous+kind+"Codes"));var to=codes(values.get("proposed"+kind+"Codes"));
            values.put(previous+kind+"Added",to.stream().filter(c->!from.contains(c)).toList());
            values.put(previous+kind+"Removed",from.stream().filter(c->!to.contains(c)).toList());
        }
    }
    private List<String> codes(Object value) {if(!(value instanceof List<?> list))return List.of();return list.stream().filter(String.class::isInstance).map(String.class::cast).distinct().sorted().toList();}
    private String json(Object value) {try{return mapper.writeValueAsString(value);}catch(Exception exception){throw conflict("미리보기 근거를 직렬화할 수 없습니다.");}}
    private String hash(Object value) {try{return fingerprint.selectHash(value);}catch(Exception exception){throw conflict("미리보기 지문을 계산할 수 없습니다.");}}
    private void requireOne(int count) {if(count!=1)throw conflict("미리보기 저장 중 충돌했습니다. 일부 선택/이력을 반영하지 않고 전체 요청을 취소했습니다.");}
    private ApiException invalid(String message){return new ApiException(ErrorCode.VALIDATION_FAILED,HttpStatus.BAD_REQUEST,message);}
    private ApiException conflict(String message){return new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_VERSION_CONFLICT,HttpStatus.CONFLICT,message);}
    private ApiException notFound(){return new ApiException(ErrorCode.RESOURCE_NOT_FOUND,HttpStatus.NOT_FOUND,"해당 배치의 미리보기 이력이 없습니다.");}
    private record Locked(AttachmentBatchRows.Row batch,AttachmentPolicyRow policy) { }
}
