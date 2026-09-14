package com.saneb.domain.announcementattachment.service.impl;

import com.saneb.common.error.*;
import com.saneb.common.response.PageResponse;
import com.saneb.domain.announcementattachment.dao.*;
import com.saneb.domain.announcementattachment.dto.AttachmentBatchHistoryResponses.*;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentBatchHistoryService;
import com.saneb.domain.announcementattachment.vo.AttachmentBatchHistorySearch;
import com.saneb.domain.auth.vo.AuthenticatedUserDetails;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class AnnouncementAttachmentBatchHistoryServiceImpl implements AnnouncementAttachmentBatchHistoryService {
    private final AnnouncementAttachmentBatchHistoryDao dao;
    private final AnnouncementAttachmentBatchDao batches;
    public AnnouncementAttachmentBatchHistoryServiceImpl(AnnouncementAttachmentBatchHistoryDao dao,AnnouncementAttachmentBatchDao batches) {this.dao=dao;this.batches=batches;}

    @Override @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ,timeout=15)
    public History selectActionList(Authentication authentication,UUID batchId,Integer throughVersion,int page,int size) {
        if(authentication==null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof AuthenticatedUserDetails actor))
            throw new ApiException(ErrorCode.AUTH_REQUIRED,HttpStatus.UNAUTHORIZED,"로그인한 운영 계정이 필요합니다.");
        if(!actor.isEnabled() || actor.passwordResetRequired() || actor.roles().stream().noneMatch(Set.of("ADMIN","OPERATOR","APPROVER")::contains))
            throw new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_ACTION_FORBIDDEN,HttpStatus.FORBIDDEN,"승인 이력은 활성 ADMIN·OPERATOR·APPROVER만 조회할 수 있습니다.");
        if(batchId==null || page<1 || size<1 || size>100 || throughVersion!=null && throughVersion<0)
            throw new ApiException(ErrorCode.VALIDATION_FAILED,HttpStatus.BAD_REQUEST,"배치 ID는 UUID, 페이지는 1 이상, 크기는 1~100, 조회 기준 버전은 0 이상의 정수여야 합니다.");
        var batch=batches.selectBatchDetails(batchId,false);
        if(batch==null)throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND,HttpStatus.NOT_FOUND,"해당 배치를 찾을 수 없습니다. 배치 목록에서 다시 선택하세요.");
        if(batch.rowVersion()==null || batch.rowVersion()<0)throw conflict();
        int bound=throughVersion==null?batch.rowVersion():throughVersion;
        if(bound>batch.rowVersion())throw new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_VERSION_CONFLICT,HttpStatus.CONFLICT,"조회 기준 버전이 현재 배치보다 큽니다. 최신 승인 목록을 처음부터 조회하세요.");
        var search=new AttachmentBatchHistorySearch(batchId,bound,size,((long)page-1)*size);
        long total=dao.selectActionCount(search);
        if(total<0 || total>(long)Integer.MAX_VALUE*size)throw conflict();
        var entries=dao.selectActionList(search);
        if(entries==null || entries.size()!=Math.min(size,Math.max(0,total-search.offset())))throw conflict();
        var seen=new HashSet<String>();int previous=bound;
        for(var entry:entries) {
            validate(entry,batchId,bound,batch.itemCount());
            if(entry.acceptedFromVersion()>previous || !seen.add(entry.actionKind()+":"+entry.actionId()))throw conflict();
            previous=entry.acceptedFromVersion();
        }
        return new History(batchId,bound,batch.rowVersion(),PageResponse.of(List.copyOf(entries),page,size,total),0);
    }
    private void validate(Entry e,UUID batchId,int bound,Integer scopeCount) {
        if(e==null || e.actionId()==null || !batchId.equals(e.batchId()) || e.acceptedAt()==null || !count(e.acceptedFromVersion())
                || e.acceptedFromVersion()>=bound || !count(e.scopeItemCount()) || e.scopeItemCount()<1 || e.scopeItemCount()>1000
                || !Objects.equals(scopeCount,e.scopeItemCount()) || !count(e.approvedTargetCount()) || e.approvedTargetCount()<1 || e.approvedTargetCount()>e.scopeItemCount()
                || !count(e.deletedCountAtAcceptance()) || e.deletedCountAtAcceptance()>e.scopeItemCount())throw conflict();
        if("APPLICATION".equals(e.actionKind())) {
            if(!Set.of("START","PAUSE","RESUME").contains(e.actionCode()==null?"":e.actionCode()) || e.previewId()==null
                    || e.approvedEligibleCount()!=null || e.approvedBaseReopenCount()!=null || e.approvedConfirmationRestoreCount()!=null || e.cancelledPendingCount()!=null)throw conflict();
        } else if("ROLLBACK".equals(e.actionKind())) {
            if(!"START".equals(e.actionCode()) || e.previewId()!=null || !count(e.approvedEligibleCount()) || e.approvedEligibleCount()<1 || e.approvedEligibleCount()>e.approvedTargetCount()
                    || !count(e.approvedBaseReopenCount()) || e.approvedBaseReopenCount()>e.approvedEligibleCount()
                    || !count(e.approvedConfirmationRestoreCount()) || e.approvedConfirmationRestoreCount()>e.approvedEligibleCount()
                    || !count(e.cancelledPendingCount()) || e.cancelledPendingCount()>e.scopeItemCount()
                    || e.approvedBaseReopenCount()+e.approvedConfirmationRestoreCount()>e.approvedEligibleCount()
                    || e.approvedTargetCount()+e.deletedCountAtAcceptance()+e.cancelledPendingCount()>e.scopeItemCount())throw conflict();
        } else throw conflict();
    }
    private boolean count(Integer value){return value!=null && value>=0;}
    private ApiException conflict(){return new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_VERSION_CONFLICT,HttpStatus.CONFLICT,"승인 이력의 배치·버전·건수 또는 페이지 합계가 일치하지 않습니다. 일부를 전체로 표시하지 않고 조회를 중단했습니다.");}
}
