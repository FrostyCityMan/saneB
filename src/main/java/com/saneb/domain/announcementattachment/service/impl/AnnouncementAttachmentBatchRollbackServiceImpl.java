package com.saneb.domain.announcementattachment.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.common.error.*;
import com.saneb.common.response.PageResponse;
import com.saneb.domain.announcementattachment.dao.*;
import com.saneb.domain.announcementattachment.dto.*;
import com.saneb.domain.announcementattachment.dto.AttachmentBatchRollbackResponses.*;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentBatchRollbackService;
import com.saneb.domain.announcementattachment.vo.*;
import com.saneb.domain.announcementattachment.vo.AttachmentBatchRollbackRows.*;
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

/** 원복은 외부 호출/재분류/운영 공고 삭제가 아니다. 승인된 현재 연결만 버전을 증가시켜 복원한다. */
@Service
public class AnnouncementAttachmentBatchRollbackServiceImpl implements AnnouncementAttachmentBatchRollbackService {
    private static final Set<String> STARTABLE=Set.of("APPLIED","APPLY_PARTIAL_FAILED","APPLY_PAUSED");
    private final AnnouncementAttachmentBatchRollbackDao dao;
    private final AnnouncementAttachmentBatchDao batches;
    private final AnnouncementAttachmentBatchPreviewDao previews;
    private final AnnouncementAttachmentEvaluationDao evaluations;
    private final AnnouncementSourceDao audit;
    private final ObjectMapper mapper;
    private final AttachmentBatchFingerprint hash;
    private final TransactionTemplate transaction;
    public AnnouncementAttachmentBatchRollbackServiceImpl(AnnouncementAttachmentBatchRollbackDao dao,AnnouncementAttachmentBatchDao batches,
            AnnouncementAttachmentBatchPreviewDao previews,AnnouncementAttachmentEvaluationDao evaluations,AnnouncementSourceDao audit,ObjectMapper mapper,PlatformTransactionManager manager) {
        this.dao=dao;this.batches=batches;this.previews=previews;this.evaluations=evaluations;this.audit=audit;this.mapper=mapper;hash=new AttachmentBatchFingerprint(mapper);
        transaction=new TransactionTemplate(manager);transaction.setTimeout(20);
    }
    private record Plan(Candidate row,Item item,String inputHash) { }
    @Override @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ,timeout=20)
    public Preview selectPreviewDetails(Authentication actor,UUID batchId) {
        actor(actor,false);var batch=batch(batchId,false);requireStartable(batch);return preview(batch,plans(batchId));
    }
    @Override @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ,timeout=20)
    public PageResponse<Item> selectItemList(Authentication actor,UUID batchId,int page,int size) {
        actor(actor,false);batch(batchId,false);
        if(page<1 || size<1 || size>100 || ((long)page-1)*size>Integer.MAX_VALUE)throw invalid("페이지는 1 이상, 크기는 1~100이어야 합니다.");
        var plans=plans(batchId);return PageResponse.of(plans.stream().skip((long)(page-1)*size).limit(size).map(Plan::item).toList(),page,size,plans.size());
    }
    @Override @Transactional(timeout=30)
    public Receipt insertRollback(Authentication authentication,UUID batchId,UUID key,AttachmentBatchRollbackRequest request) {
        UUID actor=actor(authentication,true);validate(key,request);String requestHash=hash.selectHash(Arrays.asList("attachment-batch-rollback-v1",actor,batchId,request));
        dao.selectRequestLock(key);var existing=dao.selectRequestDetails(key);
        if(existing!=null) {
            if(!actor.equals(existing.actorId()) || !Objects.equals(batchId,existing.batchId()) || !requestHash.equals(existing.requestHash()))throw conflict("이 멱등 키는 다른 원복 요청에 사용됐습니다. 최초 요청과 같은 내용으로만 재사용하세요.");
            return receipt(existing);
        }
        batch(batchId,false);
        var ids=batches.selectItemList(new AttachmentBatchRows.Search(batchId,1000,0)).stream().map(AttachmentBatchRows.Item::sourceId).sorted().toList();
        if(!ids.isEmpty() && !new HashSet<>(batches.selectSourceLocks(ids)).equals(new HashSet<>(ids)))throw conflict("승인 중 원문이 삭제됐습니다. 삭제 건수와 원복 영향 미리보기를 다시 확인하세요.");
        var batch=batch(batchId,true);requireStartable(batch);var plans=plans(batchId);var preview=preview(batch,plans);
        if(preview.eligibleCount()<1)throw conflict("변경 없이 복구할 수 있는 적용 항목이 없습니다. 충돌 사유를 확인하세요.");
        if(!Objects.equals(request.expectedVersion(),preview.version()) || !request.expectedPreviewHash().equals(preview.previewHash())
                || request.expectedScopeCount()!=preview.scopeCount() || request.expectedTargetCount()!=preview.targetCount() || request.expectedDeletedCount()!=preview.deletedCount()
                || request.expectedBaseReopenCount()!=preview.baseReopenCount() || request.expectedConfirmationRestoreCount()!=preview.confirmationRestoreCount()
                || request.expectedCancelPendingCount()!=preview.cancelPendingCount())throw conflict("버전·원복 범위·지문·영향 건수가 바뀌었습니다. 최신 미리보기를 확인한 뒤 다시 승인하세요.");
        var action=new Action(UUID.randomUUID(),batchId,batch.rowVersion(),preview.previewHash(),preview.scopeCount(),preview.targetCount(),preview.eligibleCount(),
                preview.deletedCount(),preview.baseReopenCount(),preview.confirmationRestoreCount(),preview.cancelPendingCount(),actor,key,requestHash,hash.selectHash(request.reason().strip()),null);
        one(dao.insertAction(action));
        for(var plan:plans)if(plan.item().target())one(dao.insertTarget(new Target(action.id(),batchId,plan.row().jobId(),plan.inputHash(),plan.item().readinessCode(),plan.item().eligible(),plan.item().baseReopens(),plan.item().confirmationRestores())));
        one(dao.updateStart(batchId,batch.rowVersion(),action.id()));
        if(dao.updateJobsPending(batchId,action.id())!=preview.targetCount() || dao.updateCancelApplication(batchId)!=preview.cancelPendingCount())throw conflict("원복 대상 또는 취소할 적용 대기가 변경됐습니다. 승인 전체를 취소했습니다.");
        audit(actor,batchId,"ATTACHMENT_BATCH_ROLLBACK_START",Map.of("actionId",action.id(),"targetCount",preview.targetCount(),"baseReopenCount",preview.baseReopenCount(),"confirmationRestoreCount",preview.confirmationRestoreCount(),"cancelPendingCount",preview.cancelPendingCount(),"reasonHash",action.reasonHash()));
        return receipt(dao.selectActionDetails(batchId,action.id()));
    }
    @Override @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ,timeout=15)
    public Receipt selectActionDetails(Authentication actor,UUID batchId,UUID actionId) {actor(actor,false);var action=dao.selectActionDetails(batchId,actionId);if(action==null)throw notFound();return receipt(action);}
    @Override @Transactional(propagation=Propagation.NOT_SUPPORTED)
    public boolean saveNextRollback() {
        var candidate=dao.selectNextWorkDetails();if(candidate==null){transaction.executeWithoutResult(status->dao.updateEmptyProgress());return false;}
        try{return Boolean.TRUE.equals(transaction.execute(status->saveItem(candidate)));}
        catch(RuntimeException failure) {
            transaction.executeWithoutResult(status->{
                if(batches.selectSourceLocks(List.of(candidate.sourceId())).isEmpty())return;
                var batch=batches.selectBatchDetails(candidate.batchId(),true);
                if(batch!=null && "ROLLING_BACK".equals(batch.statusCode()) && dao.updateFailure(candidate.jobId())==1)dao.updateProgress(candidate.batchId());
            });throw failure;
        }
    }
    private boolean saveItem(Work candidate) {
        if(batches.selectSourceLocks(List.of(candidate.sourceId())).isEmpty())return false;
        if(!"ROLLING_BACK".equals(batch(candidate.batchId(),true).statusCode()))return false;
        var work=dao.selectWorkDetails(candidate.batchId(),candidate.jobId());if(work==null)return false;
        var row=dao.selectCandidateDetails(work.batchId(),work.jobId());var live=previews.selectLiveItemDetails(work.batchId(),work.jobId());var plan=plan(row,live);
        String error=!Boolean.TRUE.equals(work.eligible())?work.readinessCode():!plan.item().eligible()?plan.item().readinessCode():
                !work.inputHash().equals(plan.inputHash())?"ROLLBACK_INPUT_CHANGED":null;
        if(error!=null) {one(dao.updateConflict(work.jobId(),error));one(dao.updateProgress(work.batchId()));audit(work.actorId(),work.batchId(),"ATTACHMENT_BATCH_ROLLBACK_CONFLICT",Map.of("jobId",work.jobId(),"code",error));return true;}
        boolean restore=Boolean.TRUE.equals(work.confirmationRestores());
        if(restore)one(dao.insertConfirmationRestoration(new Restoration(UUID.randomUUID(),work.jobId(),work.actorId(),UUID.randomUUID(),hash.selectHash(Arrays.asList(work.actionId(),work.jobId(),work.inputHash())))));
        one(dao.updateSourceRestoration(work.jobId()));evaluations.updatePreviousEvaluationsStale(work.sourceId());
        if(row.previousEvaluationId()!=null)one(evaluations.updateEvaluationCurrent(row.previousEvaluationId(),work.sourceId()));
        if(restore)one(dao.updateConfirmationCurrent(work.jobId()));
        one(dao.updateRolledBack(work.jobId(),restore));one(dao.updateProgress(work.batchId()));
        audit(work.actorId(),work.batchId(),"ATTACHMENT_BATCH_ITEM_ROLLED_BACK",Map.of("actionId",work.actionId(),"jobId",work.jobId(),"confirmationRestored",restore,"baseReopened",!Boolean.TRUE.equals(row.previousReviewRequired())));return true;
    }
    private List<Plan> plans(UUID batchId) {
        var live=new HashMap<UUID,AttachmentBatchPreviewRows.LiveItem>();for(var item:previews.selectLiveItemList(batchId))live.put(item.jobId(),item);
        return dao.selectCandidateList(batchId).stream().sorted(Comparator.comparing(Candidate::sourceId).thenComparing(Candidate::jobId)).map(row->plan(row,live.get(row.jobId()))).toList();
    }
    private Plan plan(Candidate row,AttachmentBatchPreviewRows.LiveItem live) {
        if(row==null)throw conflict("원복 대상이 삭제됐습니다. 최신 상태를 조회하세요.");
        boolean target="APPLIED".equals(row.applicationStatusCode());
        String reason=!target?"NOT_APPLIED":!"NOT_REQUESTED".equals(row.rollbackStatusCode())?"ROLLBACK_"+row.rollbackStatusCode():
                !Boolean.TRUE.equals(row.currentBindingMatches())?"ROLLBACK_BINDING_CHANGED":!Boolean.TRUE.equals(row.previousBindingValid())?"ROLLBACK_PREVIOUS_BINDING_INVALID":
                live==null || Boolean.TRUE.equals(live.protectedLink()) || Boolean.TRUE.equals(live.activeOtherJob()) || !Objects.equals(row.appliedInputHash(),hash.selectHash(live))?"ROLLBACK_INPUT_CHANGED":"READY";
        boolean eligible="READY".equals(reason),restore=eligible && Boolean.TRUE.equals(row.previousConfirmationValid());
        var item=new Item(row.jobId(),row.sourceId(),row.providerCode(),row.applicationStatusCode(),row.rollbackStatusCode(),reason,target,eligible,
                eligible && !Boolean.TRUE.equals(row.previousReviewRequired()),restore,eligible && row.previousConfirmationId()!=null && !restore,row.errorCode(),row.attemptCount(),row.nextAttemptAt());
        // 재시도 횟수·진행 오류는 입력 근거가 아니다. 실패 기록 자체가 재시도의 승인 지문을 바꾸지 않게 한다.
        var normalized=new Candidate(row.jobId(),row.sourceId(),row.providerCode(),row.applicationStatusCode(),row.rollbackStatusCode(),row.previousEvaluationId(),row.previousConfirmationId(),row.previousReviewRequired(),
                row.currentBindingMatches(),row.previousBindingValid(),row.previousConfirmationValid(),row.appliedInputHash(),null,0,null);
        return new Plan(row,item,hash.selectHash(Arrays.asList("attachment-rollback-item-v1",normalized,live)));
    }
    private Preview preview(AttachmentBatchRows.Row batch,List<Plan> plans) {
        if(plans.size()+batch.deletedItemCount()!=batch.itemCount())throw conflict("고정 범위와 남은/삭제 건수가 일치하지 않습니다. 원복하지 않고 확인이 필요합니다.");
        int targets=0,eligible=0,base=0,confirmed=0,stale=0,pending=0;
        for(var p:plans) {if(p.item().target())targets++;if(p.item().eligible())eligible++;if(p.item().baseReopens())base++;if(p.item().confirmationRestores())confirmed++;if(p.item().staleConfirmationRemains())stale++;if("PENDING".equals(p.row().applicationStatusCode()))pending++;}
        String fingerprint=hash.selectHash(Arrays.asList("attachment-batch-rollback-preview-v1",batch.scopeHash(),batch.rowVersion(),batch.itemCount(),batch.deletedItemCount(),plans.stream().map(Plan::inputHash).toList()));
        return new Preview(batch.batchId(),batch.rowVersion(),batch.statusCode(),fingerprint,batch.itemCount(),plans.size(),batch.deletedItemCount(),targets,eligible,targets-eligible,base,confirmed,stale,pending,0);
    }
    private Receipt receipt(Action action) {if(action==null)throw notFound();var batch=batch(action.batchId(),false);var c=dao.selectCounts(action.id());return new Receipt(action.id(),action.batchId(),action.expectedVersion(),batch.statusCode(),batch.rowVersion(),action.scopeCount(),action.targetCount(),action.eligibleCount(),action.baseReopenCount(),action.confirmationRestoreCount(),action.cancelPendingCount(),c.remaining(),batch.deletedItemCount(),c.pending(),c.rolledBack(),c.conflicts(),c.failed(),0,action.createdAt());}
    private AttachmentBatchRows.Row batch(UUID id,boolean lock) {var row=id==null?null:batches.selectBatchDetails(id,lock);if(row==null)throw notFound();return row;}
    private void requireStartable(AttachmentBatchRows.Row row){if(!STARTABLE.contains(row.statusCode()))throw conflict("적용 완료·부분 완료 또는 적용 중지 배치에서만 원복 미리보기와 승인을 할 수 있습니다.");}
    private void validate(UUID key,AttachmentBatchRollbackRequest r) {
        if(key==null || r==null || r.expectedVersion()==null || r.expectedVersion()<0 || r.expectedVersion()>Integer.MAX_VALUE-1 || r.expectedPreviewHash()==null || !r.expectedPreviewHash().matches("[0-9a-f]{64}"))throw invalid("UUID 멱등 키, 현재 버전과 64자리 원복 미리보기 지문을 입력하세요.");
        var counts=Arrays.asList(r.expectedScopeCount(),r.expectedTargetCount(),r.expectedDeletedCount(),r.expectedBaseReopenCount(),r.expectedConfirmationRestoreCount(),r.expectedCancelPendingCount());
        if(counts.stream().anyMatch(n->n==null || n<0 || n>1000) || r.expectedScopeCount()<1 || r.expectedTargetCount()<1 || counts.stream().anyMatch(n->n>r.expectedScopeCount()))throw invalid("전체·대상·삭제·복구 영향·취소 대기 건수는 미리보기의 0~1000 범위 값과 일치해야 합니다.");
        if(!Boolean.TRUE.equals(r.acknowledgeBindingRestoration()) || r.reason()==null || r.reason().isBlank() || r.reason().length()>1000)throw invalid("이전 판정·검수 복구와 적용 대기 취소를 확인하고 사유를 1~1000자로 입력하세요.");
    }
    private UUID actor(Authentication authentication,boolean write) {
        if(authentication==null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof AuthenticatedUserDetails actor))throw new ApiException(ErrorCode.AUTH_REQUIRED,HttpStatus.UNAUTHORIZED,"로그인한 운영 계정이 필요합니다.");
        if(!actor.isEnabled() || actor.passwordResetRequired() || actor.roles().stream().noneMatch((write?Set.of("ADMIN"):Set.of("ADMIN","OPERATOR","APPROVER"))::contains))throw new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_ACTION_FORBIDDEN,HttpStatus.FORBIDDEN,write?"원복 승인은 활성 ADMIN만 가능합니다.":"원복 내역은 활성 ADMIN·OPERATOR·APPROVER만 조회할 수 있습니다.");return actor.userId();
    }
    private void audit(UUID actor,UUID batchId,String code,Object metadata) {try {audit.insertAuditLog(new AnnouncementSourceAuditLogCommand(actor,code,"ANNOUNCEMENT_ATTACHMENT_BATCH",batchId,"SUCCESS",mapper.writeValueAsString(metadata)));}catch(com.fasterxml.jackson.core.JsonProcessingException failure){throw new IllegalStateException("원복 감사 metadata 직렬화 실패");}}
    private void one(int count){if(count!=1)throw conflict("원복 중 현재 연결이 변경됐습니다. 항목 전체 변경을 취소합니다.");}
    private ApiException invalid(String message){return new ApiException(ErrorCode.VALIDATION_FAILED,HttpStatus.BAD_REQUEST,message);}
    private ApiException conflict(String message){return new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_VERSION_CONFLICT,HttpStatus.CONFLICT,message);}
    private ApiException notFound(){return new ApiException(ErrorCode.RESOURCE_NOT_FOUND,HttpStatus.NOT_FOUND,"해당 배치의 원복 내역이 없습니다.");}
}
