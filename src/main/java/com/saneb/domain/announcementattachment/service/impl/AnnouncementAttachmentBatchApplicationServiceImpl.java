package com.saneb.domain.announcementattachment.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.common.error.*;
import com.saneb.common.response.PageResponse;
import com.saneb.domain.announcementattachment.dao.*;
import com.saneb.domain.announcementattachment.dto.*;
import com.saneb.domain.announcementattachment.service.*;
import com.saneb.domain.announcementattachment.vo.*;
import com.saneb.domain.announcementsource.dao.AnnouncementSourceDao;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceAuditLogCommand;
import com.saneb.domain.auth.vo.AuthenticatedUserDetails;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.TransactionTemplate;

/** 명시적으로 승인된 고정 선택만 적용한다. 외부 호출/다운로드/재분류/운영 공고 생성은 하지 않는다. */
@Service
public class AnnouncementAttachmentBatchApplicationServiceImpl implements AnnouncementAttachmentBatchApplicationService {
    private final AnnouncementAttachmentBatchApplicationDao dao;
    private final AnnouncementAttachmentBatchDao batches;
    private final AnnouncementAttachmentBatchPreviewDao previews;
    private final AnnouncementAttachmentEvaluationDao evaluations;
    private final AnnouncementSourceDao audit;
    private final ObjectMapper mapper;
    private final AttachmentBatchFingerprint fingerprint;
    private final TransactionTemplate transaction;
    public AnnouncementAttachmentBatchApplicationServiceImpl(AnnouncementAttachmentBatchApplicationDao dao,AnnouncementAttachmentBatchDao batches,
            AnnouncementAttachmentBatchPreviewDao previews,AnnouncementAttachmentEvaluationDao evaluations,AnnouncementSourceDao audit,
            ObjectMapper mapper,PlatformTransactionManager manager) {
        this.dao=dao;this.batches=batches;this.previews=previews;this.evaluations=evaluations;this.audit=audit;this.mapper=mapper;
        fingerprint=new AttachmentBatchFingerprint(mapper);transaction=new TransactionTemplate(manager);transaction.setTimeout(20);
    }
    @Override @Transactional(timeout=30)
    public AttachmentBatchApplicationResponse insertAction(Authentication authentication,UUID batchId,UUID key,String code,AttachmentBatchApplicationRequest request) {
        UUID actor=selectActor(authentication,true);validate(key,code,request);
        String requestHash=fingerprint.selectHash(Arrays.asList("attachment-batch-application-v1",actor,batchId,code,request));
        dao.selectRequestLock(key);var existing=dao.selectRequestDetails(key);
        if(existing!=null) {
            if(!actor.equals(existing.actorId()) || !Objects.equals(batchId,existing.batchId()) || !requestHash.equals(existing.requestHash()))
                throw conflict("이 멱등 키는 다른 실행 요청에 사용됐습니다. 최초 요청과 같은 내용에만 재사용하세요.");
            return response(existing);
        }
        var before=batch(batchId,false);AttachmentPolicyRow policy=null;
        if("START".equals(code)) {
            var ids=batches.selectItemList(new AttachmentBatchRows.Search(batchId,1000,0)).stream().map(AttachmentBatchRows.Item::sourceId).sorted().toList();
            if(!ids.isEmpty() && !new HashSet<>(batches.selectSourceLocks(ids)).equals(new HashSet<>(ids)))throw conflict("승인 중 원문이 삭제됐습니다. 전체 범위와 삭제 건수를 다시 확인하세요.");
            policy=batches.selectPolicyDetails(before.policyId(),true);
        }
        var batch=batch(batchId,true);var preview=previews.selectCurrentPreviewDetails(batchId);
        if(batch.rowVersion()!=request.expectedVersion() || preview==null || !preview.previewId().equals(request.expectedPreviewId())
                || !preview.previewHash().equals(request.expectedPreviewHash()) || batch.itemCount()!=request.expectedItemCount()
                || preview.selectedItemCount()!=request.expectedSelectedCount() || batch.deletedItemCount()!=request.expectedDeletedCount())
            throw conflict("배치 버전·미리보기·전체/선택/삭제 건수가 바뀌었습니다. 최신 상세를 다시 확인한 뒤 승인하세요.");
        if("START".equals(code)) {
            if(!Set.of("PREVIEW_READY","PREVIEW_PARTIAL_FAILED").contains(batch.statusCode()) || !Objects.equals(preview.batchVersion(),batch.rowVersion()))
                throw conflict("선택이 확정된 현재 미리보기에서만 적용을 시작할 수 있습니다.");
            if(!policyMatches(batch,policy))throw conflict("적용에는 수집 시 고정된 현재 ACTIVE ENFORCE 정책·규칙이 필요합니다. COLLECT_ONLY 결과를 자동 승격하지 않습니다.");
            var live=previews.selectLiveItemList(batchId).stream().sorted(Comparator.comparing(AttachmentBatchPreviewRows.LiveItem::jobId)).toList();
            String input=fingerprint.selectHash(Arrays.asList("attachment-batch-preview-input-v1",batch.scopeHash(),batch.itemCount(),batch.deletedItemCount(),policy,live));
            if(live.size()+batch.deletedItemCount()!=batch.itemCount() || !input.equals(preview.inputHash()))
                throw conflict("승인 미리보기의 근거 입력이 바뀌었습니다. 새 미리보기를 만든 후 다시 선택하세요.");
            var selected=previews.selectItemList(new AttachmentBatchPreviewRows.Search(preview.previewId(),1000,0)).stream().filter(i->Boolean.TRUE.equals(i.selected())).toList();
            if(selected.size()!=preview.selectedItemCount() || selected.stream().anyMatch(i->!Boolean.TRUE.equals(i.eligible()) || !"READY".equals(i.readinessCode())))
                throw conflict("선택한 성공 항목과 현재 남은 항목이 다릅니다. 일부 대상만 임의로 적용하지 않습니다.");
        } else if(!("PAUSE".equals(code)?"APPLYING":"APPLY_PAUSED").equals(batch.statusCode())) {
            throw conflict("중지는 적용 중, 재개는 적용 중지 상태에서만 가능합니다. 현재 배치 단계를 확인하세요.");
        }
        var action=new AttachmentBatchApplicationRows.Action(UUID.randomUUID(),batchId,preview.previewId(),code,batch.rowVersion(),batch.itemCount(),
                preview.selectedItemCount(),batch.deletedItemCount(),actor,key,requestHash,fingerprint.selectHash(request.reason().strip()),null);
        requireOne(dao.insertAction(action));
        switch(code) {
            case "START" -> {requireOne(dao.updateStart(batchId,batch.rowVersion(),action.id()));if(dao.updateJobsPending(batchId,preview.previewId())!=preview.selectedItemCount())throw conflict("선택 대상 예약이 충돌했습니다. 전체 적용 요청을 취소했습니다.");}
            case "PAUSE" -> requireOne(dao.updatePause(batchId,batch.rowVersion()));
            case "RESUME" -> requireOne(dao.updateResume(batchId,batch.rowVersion()));
            default -> throw invalid("지원하지 않는 배치 실행 명령입니다.");
        }
        insertAudit(actor,batchId,"ATTACHMENT_BATCH_APPLICATION_"+code,Map.of("actionId",action.id(),"previewId",preview.previewId(),"selectedCount",preview.selectedItemCount(),"reasonHash",action.reasonHash()));
        return response(dao.selectActionDetails(batchId,action.id()));
    }
    @Override @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ,timeout=15)
    public AttachmentBatchApplicationResponse selectActionDetails(Authentication actor,UUID batchId,UUID actionId) {
        selectActor(actor,false);var action=dao.selectActionDetails(batchId,actionId);if(action==null)throw notFound();return response(action);
    }
    @Override @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ,timeout=15)
    public PageResponse<AttachmentBatchApplicationRows.Item> selectItemList(Authentication actor,UUID batchId,int page,int size) {
        selectActor(actor,false);batch(batchId,false);
        if(page<1 || size<1 || size>100 || ((long)page-1)*size>Integer.MAX_VALUE)throw invalid("페이지는 1 이상, 크기는 1~100이어야 합니다.");
        return PageResponse.of(dao.selectItemList(new AttachmentBatchRows.Search(batchId,size,(page-1)*size)),page,size,dao.selectCounts(batchId).remaining());
    }
    @Override @Transactional(propagation=Propagation.NOT_SUPPORTED)
    public boolean saveNextApplication() {
        var candidate=dao.selectNextWorkDetails();
        if(candidate==null) {transaction.executeWithoutResult(status->dao.updateEmptyProgress());return false;}
        try {return Boolean.TRUE.equals(transaction.execute(status->saveItem(candidate)));}
        catch(RuntimeException failure) {
            // 부분 source 변경 transaction이 끝난 뒤 실패 횟수만 별도 저장한다. 원문/DB 예외를 로그·감사에 복사하지 않는다.
            transaction.executeWithoutResult(status->{
                if(batches.selectSourceLocks(List.of(candidate.sourceId())).isEmpty())return;
                var batch=batches.selectBatchDetails(candidate.batchId(),true);
                if(batch!=null && "APPLYING".equals(batch.statusCode()) && dao.updateFailure(candidate.jobId())==1)dao.updateProgress(candidate.batchId());
            });
            throw failure;
        }
    }
    private boolean saveItem(AttachmentBatchApplicationRows.Work candidate) {
        if(batches.selectSourceLocks(List.of(candidate.sourceId())).isEmpty())return false;
        var policy=batches.selectPolicyDetails(candidate.policyId(),true);var batch=batch(candidate.batchId(),true);
        if(!"APPLYING".equals(batch.statusCode()))return false;
        var work=dao.selectWorkDetails(candidate.batchId(),candidate.jobId());if(work==null)return false;
        var live=previews.selectLiveItemDetails(work.batchId(),work.jobId());
        String error=!policyMatches(batch,policy)?"APPLICATION_POLICY_CHANGED":!ready(live)?"APPLICATION_INPUT_CHANGED":
                !work.itemInputHash().equals(fingerprint.selectHash(live))?"APPLICATION_PREVIEW_CHANGED":null;
        if(error!=null) {
            requireOne(dao.updateConflict(work.jobId(),error));requireOne(dao.updateProgress(work.batchId()));
            insertAudit(work.actorId(),work.batchId(),"ATTACHMENT_BATCH_ITEM_CONFLICT",Map.of("jobId",work.jobId(),"code",error));return true;
        }
        // 이전 확인이 아직 current인 시점에 source CAS를 수행한다. deferred binding 검증은 transaction 종료 시 실행된다.
        int changed=dao.updateSourceApplication(work.jobId());
        if(changed==0) {
            requireOne(dao.updateConflict(work.jobId(),"APPLICATION_SOURCE_CAS_CONFLICT"));requireOne(dao.updateProgress(work.batchId()));
            insertAudit(work.actorId(),work.batchId(),"ATTACHMENT_BATCH_ITEM_CONFLICT",Map.of("jobId",work.jobId(),"code","APPLICATION_SOURCE_CAS_CONFLICT"));return true;
        }
        requireOne(changed);
        evaluations.updatePreviousConfirmationsStale(work.sourceId());evaluations.updatePreviousEvaluationsStale(work.sourceId());
        requireOne(evaluations.updateEvaluationCurrent(work.evaluationId(),work.sourceId()));
        var applied=previews.selectLiveItemDetails(work.batchId(),work.jobId());
        if(applied==null)throw conflict("적용 후 근거를 읽을 수 없습니다. 항목 전체 변경을 취소합니다.");
        requireOne(dao.updateApplied(work.jobId(),fingerprint.selectHash(applied)));requireOne(dao.updateProgress(work.batchId()));
        insertAudit(work.actorId(),work.batchId(),"ATTACHMENT_BATCH_ITEM_APPLIED",Map.of("jobId",work.jobId(),"evaluationId",work.evaluationId(),"previewId",work.previewId()));
        return true;
    }
    private boolean ready(AttachmentBatchPreviewRows.LiveItem live) {return live!=null && "SUCCEEDED".equals(live.jobStatusCode()) && Boolean.TRUE.equals(live.evidenceComplete())
            && Boolean.TRUE.equals(live.frozenSourceCurrent()) && !Boolean.TRUE.equals(live.protectedLink()) && !Boolean.TRUE.equals(live.activeOtherJob());}
    private boolean policyMatches(AttachmentBatchRows.Row batch,AttachmentPolicyRow policy) {
        try{return policy!=null && "ACTIVE".equals(policy.policyStatusCode()) && "ACTIVE".equals(policy.releaseStatusCode()) && "ENFORCE".equals(policy.modeCode())
                && policy.equals(mapper.readValue(batch.policySnapshotJson(),AttachmentPolicyRow.class));}catch(Exception ignored){return false;}
    }
    private AttachmentBatchApplicationResponse response(AttachmentBatchApplicationRows.Action action) {
        if(action==null)throw notFound();var batch=batch(action.batchId(),false);var c=dao.selectCounts(action.batchId());
        return new AttachmentBatchApplicationResponse(action.id(),action.batchId(),action.previewId(),action.actionCode(),action.expectedVersion(),batch.statusCode(),batch.rowVersion(),
                action.itemCount(),action.selectedCount(),c.remaining(),batch.deletedItemCount(),c.selectedRemaining(),c.pending(),c.applied(),c.conflicts(),c.failed(),0,action.createdAt());
    }
    private AttachmentBatchRows.Row batch(UUID id,boolean lock) {var row=id==null?null:batches.selectBatchDetails(id,lock);if(row==null)throw notFound();return row;}
    private void validate(UUID key,String code,AttachmentBatchApplicationRequest r) {
        if(key==null || !Set.of("START","PAUSE","RESUME").contains(code==null?"":code) || r==null || r.expectedVersion()==null || r.expectedVersion()<0 || r.expectedVersion()>Integer.MAX_VALUE-1
                || r.expectedPreviewId()==null || r.expectedPreviewHash()==null || !r.expectedPreviewHash().matches("[0-9a-f]{64}")
                || r.expectedItemCount()==null || r.expectedItemCount()<1 || r.expectedItemCount()>1000 || r.expectedSelectedCount()==null || r.expectedSelectedCount()<1
                || r.expectedSelectedCount()>r.expectedItemCount() || r.expectedDeletedCount()==null || r.expectedDeletedCount()<0 || r.expectedDeletedCount()>r.expectedItemCount())
            throw invalid("UUID 멱등 키·미리보기 ID, 현재 버전·64자리 지문, 1~1000 전체 건수와 유효한 선택/삭제 건수가 필요합니다.");
        if(!Boolean.TRUE.equals(r.acknowledgeReviewReset()))throw invalid("이전 첨부 확인이 만료되고 다시 검수해야 함을 확인하세요.");
        if(r.reason()==null || r.reason().isBlank() || r.reason().length()>1000)throw invalid("실행 사유는 공백이 아닌 1~1000자로 입력하세요.");
    }
    private UUID selectActor(Authentication authentication,boolean write) {
        if(authentication==null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof AuthenticatedUserDetails actor))
            throw new ApiException(ErrorCode.AUTH_REQUIRED,HttpStatus.UNAUTHORIZED,"로그인한 운영 계정이 필요합니다.");
        if(!actor.isEnabled() || actor.passwordResetRequired() || actor.roles().stream().noneMatch((write?Set.of("ADMIN"):Set.of("ADMIN","OPERATOR","APPROVER"))::contains))
            throw new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_ACTION_FORBIDDEN,HttpStatus.FORBIDDEN,write?"배치 적용·중지·재개는 활성 ADMIN만 가능합니다.":"실행 내역은 활성 ADMIN·OPERATOR·APPROVER만 조회할 수 있습니다.");
        return actor.userId();
    }
    private void insertAudit(UUID actor,UUID batchId,String action,Object metadata) {
        try {audit.insertAuditLog(new AnnouncementSourceAuditLogCommand(actor,action,"ANNOUNCEMENT_ATTACHMENT_BATCH",batchId,"SUCCESS",mapper.writeValueAsString(metadata)));}
        catch(com.fasterxml.jackson.core.JsonProcessingException exception){throw new IllegalStateException("배치 감사 metadata 직렬화에 실패했습니다.");}
    }
    private void requireOne(int result){if(result!=1)throw conflict("적용 중 버전 또는 선택이 바뀌었습니다. 현재 항목 전체를 취소합니다.");}
    private ApiException invalid(String message){return new ApiException(ErrorCode.VALIDATION_FAILED,HttpStatus.BAD_REQUEST,message);}
    private ApiException conflict(String message){return new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_VERSION_CONFLICT,HttpStatus.CONFLICT,message);}
    private ApiException notFound(){return new ApiException(ErrorCode.RESOURCE_NOT_FOUND,HttpStatus.NOT_FOUND,"해당 배치의 실행 내역이 없습니다.");}
}
