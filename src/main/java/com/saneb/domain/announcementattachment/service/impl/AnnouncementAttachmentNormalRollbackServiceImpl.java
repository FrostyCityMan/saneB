package com.saneb.domain.announcementattachment.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.common.error.*;
import com.saneb.common.response.PageResponse;
import com.saneb.domain.announcementattachment.dao.*;
import com.saneb.domain.announcementattachment.dto.AttachmentNormalRollbackRequest;
import com.saneb.domain.announcementattachment.dto.AttachmentNormalRollbackResponses.*;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentNormalRollbackService;
import com.saneb.domain.announcementattachment.vo.AttachmentNormalRollbackRows.*;
import com.saneb.domain.announcementsource.dao.AnnouncementSourceDao;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceAuditLogCommand;
import com.saneb.domain.auth.vo.AuthenticatedUserDetails;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** 승인·복원·영수증을 단일 원문 transaction으로 완료한다. 다운로드/추출/자동 활성화는 없다. */
@Service
public class AnnouncementAttachmentNormalRollbackServiceImpl implements AnnouncementAttachmentNormalRollbackService {
    private final AnnouncementAttachmentNormalRollbackDao dao;
    private final AnnouncementAttachmentEvaluationDao evaluations;
    private final AnnouncementSourceDao audit;
    private final ObjectMapper mapper;
    private final AttachmentBatchFingerprint hash;
    public AnnouncementAttachmentNormalRollbackServiceImpl(AnnouncementAttachmentNormalRollbackDao dao,
            AnnouncementAttachmentEvaluationDao evaluations,AnnouncementSourceDao audit,ObjectMapper mapper) {
        this.dao=dao;this.evaluations=evaluations;this.audit=audit;this.mapper=mapper;this.hash=new AttachmentBatchFingerprint(mapper);
    }
    @Override @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ,timeout=15)
    public PageResponse<JobSummary> selectJobList(Authentication authentication,UUID sourceId,int page,int size) {
        actor(authentication,false);
        if(page<1 || page>1_000_000 || size<1 || size>100)throw invalid("페이지(page)는 1~1000000, 페이지 크기(size)는 1~100이어야 합니다.");
        if(sourceId==null || !sourceId.equals(dao.selectVisibleSourceDetails(sourceId)))throw notFound();
        return PageResponse.of(dao.selectJobList(sourceId,(page-1)*size,size),page,size,dao.selectJobCount(sourceId));
    }
    @Override @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ,timeout=15)
    public Preview selectPreviewDetails(Authentication authentication,UUID sourceId,UUID jobId) {
        actor(authentication,false);var s=state(sourceId,jobId);
        return new Preview(sourceId,jobId,s.modeCode(),s.jobStatusCode(),s.applicationStatusCode(),s.rollbackStatusCode(),s.readinessCode(),
                s.sourceVersion(),s.attachmentVersion(),s.previewHash(),Boolean.TRUE.equals(s.baseReopens()),Boolean.TRUE.equals(s.confirmationRestores()),Boolean.TRUE.equals(s.staleConfirmationRemains()),1,0);
    }
    @Override @Transactional(timeout=20)
    public Receipt insertRollback(Authentication authentication,UUID sourceId,UUID jobId,UUID key,AttachmentNormalRollbackRequest request) {
        UUID actor=actor(authentication,true);validate(sourceId,jobId,key,request);
        String requestHash=hash.selectHash(Arrays.asList("attachment-normal-rollback-v1",actor,sourceId,jobId,request));
        dao.selectRequestLock(key);var existing=dao.selectRequestDetails(key);
        if(existing!=null) {
            if(!sourceId.equals(existing.sourceId()) || !jobId.equals(existing.jobId()) || !actor.equals(existing.actorId()) || !requestHash.equals(existing.requestHash()))
                throw conflict("이 멱등 키는 다른 원복 요청에 사용됐습니다. 최초 원문·작업·요청 내용으로만 재사용하세요.");
            return receipt(existing);
        }
        if(!sourceId.equals(dao.selectSourceLock(sourceId)) || !jobId.equals(dao.selectJobLock(sourceId,jobId)))throw notFound();
        var s=state(sourceId,jobId);
        if(!"READY".equals(s.readinessCode()))throw conflict(selectReason(s.readinessCode()));
        if(!Objects.equals(request.expectedSourceVersion(),s.sourceVersion()) || !Objects.equals(request.expectedAttachmentVersion(),s.attachmentVersion())
                || !request.expectedPreviewHash().equals(s.previewHash()) || !request.expectedBaseReopen().equals(s.baseReopens())
                || !request.expectedConfirmationRestore().equals(s.confirmationRestores()))
            throw conflict("미리보기 이후 버전·원복 지문·기본 경로 재개 또는 검수 복원 효과가 바뀌었습니다. 최신 영향을 확인하세요.");
        var action=new Action(UUID.randomUUID(),jobId,sourceId,s.modeCode(),s.sourceVersion(),s.attachmentVersion(),s.previewHash(),s.baseReopens(),s.confirmationRestores(),actor,key,requestHash,hash.selectHash(request.reason().strip()),null);
        one(dao.insertAction(action));one(dao.updateSourceRestoration(action.id()));evaluations.updatePreviousEvaluationsStale(sourceId);
        if(s.previousEvaluationId()!=null)one(evaluations.updateEvaluationCurrent(s.previousEvaluationId(),sourceId));
        if(Boolean.TRUE.equals(s.confirmationRestores()))one(dao.updateConfirmationCurrent(action.id()));
        one(dao.updateRolledBack(action.id()));
        try{audit.insertAuditLog(new AnnouncementSourceAuditLogCommand(actor,"ATTACHMENT_NORMAL_ROLLBACK","ANNOUNCEMENT_SOURCE",sourceId,"SUCCESS",
                mapper.writeValueAsString(Map.of("actionId",action.id(),"jobId",jobId,"modeCode",s.modeCode(),"baseReopened",s.baseReopens(),"confirmationRestored",s.confirmationRestores(),"reasonHash",action.reasonHash()))));}
        catch(JsonProcessingException failure){throw new IllegalStateException("원복 감사 metadata를 기록하지 못했습니다.");}
        return receipt(dao.selectActionDetails(sourceId,jobId,action.id()));
    }
    @Override @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ,timeout=15)
    public Receipt selectActionDetails(Authentication authentication,UUID sourceId,UUID jobId,UUID actionId) {
        actor(authentication,false);if(sourceId==null || jobId==null || actionId==null)throw notFound();return receipt(dao.selectActionDetails(sourceId,jobId,actionId));
    }
    private State state(UUID sourceId,UUID jobId) {
        if(sourceId==null || jobId==null)throw notFound();String json=dao.selectStateJson(sourceId,jobId);if(json==null)throw notFound();
        try {var state=mapper.readValue(json,State.class);
            if(state==null || !sourceId.equals(state.sourceId()) || !jobId.equals(state.jobId()))throw notFound();
            if(state.sourceVersion()==null || state.sourceVersion()<0 || state.attachmentVersion()==null || state.attachmentVersion()<0
                    || state.jobVersion()==null || state.modeCode()==null || !Set.of("APPLIED","FAILED_RESERVATION","UNAVAILABLE").contains(state.modeCode())
                    || state.inputHash()==null || !state.inputHash().matches("[0-9a-f]{64}") || state.previewHash()==null || !state.previewHash().matches("[0-9a-f]{64}")
                    || state.readinessCode()==null || state.baseReopens()==null || state.confirmationRestores()==null || state.staleConfirmationRemains()==null)
                throw conflict("저장된 원복 근거가 완전하지 않습니다. 원문을 변경하지 않고 운영 점검이 필요합니다.");
            if(Boolean.TRUE.equals(state.confirmationRestores()) && (state.previousEvaluationId()==null || state.previousConfirmationId()==null || Boolean.TRUE.equals(state.baseReopens())))
                throw conflict("이전 검수 확인 식별자와 복구 영향이 일치하지 않습니다. 원문을 변경하지 않고 운영 점검이 필요합니다.");
            return state;
        }catch(JsonProcessingException failure){throw conflict("저장된 원복 근거를 해석하지 못했습니다. 원문을 변경하지 않고 운영 점검이 필요합니다.");}
    }
    private Receipt receipt(Action a){if(a==null)throw notFound();return new Receipt(a.id(),a.sourceId(),a.jobId(),a.modeCode(),"ROLLED_BACK",a.expectedSourceVersion(),Math.addExact(a.expectedAttachmentVersion(),1),Boolean.TRUE.equals(a.baseReopens()),Boolean.TRUE.equals(a.confirmationRestores()),1,0,a.createdAt());}
    private void validate(UUID source,UUID job,UUID key,AttachmentNormalRollbackRequest r) {
        if(source==null || job==null || key==null || r==null || r.expectedSourceVersion()==null || r.expectedSourceVersion()<0
                || r.expectedAttachmentVersion()==null || r.expectedAttachmentVersion()<0 || r.expectedAttachmentVersion()>2147483646
                || r.expectedPreviewHash()==null || !r.expectedPreviewHash().matches("[0-9a-f]{64}"))throw invalid("원문·작업·멱등 키의 UUID, 현재 버전과 64자리 원복 미리보기 지문이 필요합니다.");
        if(r.expectedBaseReopen()==null || r.expectedConfirmationRestore()==null || !Boolean.TRUE.equals(r.acknowledgeBindingRestoration())
                || r.reason()==null || r.reason().isBlank() || r.reason().length()>1000)throw invalid("기본 경로 재개·검수 복구 효과를 확인하고 사유를 1~1000자로 입력하세요.");
    }
    private UUID actor(Authentication authentication,boolean write) {
        if(authentication==null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof AuthenticatedUserDetails actor))throw new ApiException(ErrorCode.AUTH_REQUIRED,HttpStatus.UNAUTHORIZED,"로그인한 운영 계정이 필요합니다.");
        if(!actor.isEnabled() || actor.passwordResetRequired() || actor.roles().stream().noneMatch((write?Set.of("ADMIN"):Set.of("ADMIN","OPERATOR","APPROVER"))::contains))
            throw new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_ACTION_FORBIDDEN,HttpStatus.FORBIDDEN,write?"원복은 활성 ADMIN만 승인할 수 있습니다.":"원복 근거는 활성 ADMIN·OPERATOR·APPROVER만 조회할 수 있습니다.");return actor.userId();
    }
    private String selectReason(String code){return switch(code){
        case "ALREADY_RECOVERED" -> "이미 복구한 작업입니다. 최초 요청의 멱등 키나 원복 영수증을 확인하세요.";
        case "JOB_NOT_TERMINAL" -> "완료된 적용 또는 실패로 종료된 예약만 복구할 수 있습니다. 진행 중인 수집은 먼저 종료돼야 합니다.";
        case "RECOVERY_EVIDENCE_MISSING" -> "예약 당시 복구 근거가 없는 과거 작업입니다. 버전을 추정해 원복하지 않습니다.";
        case "PREVIOUS_BINDING_INVALID" -> "이전 판정의 본문·규칙·첨부 연결이 유효하지 않아 복원할 수 없습니다.";
        default -> "원문·첨부 입력이나 버전이 바뀌었거나 후속 검수·공고 연결·다른 작업이 존재합니다. 현재 작업을 덮어쓰지 않습니다.";};}
    private void one(int count){if(count!=1)throw conflict("복구 중 현재 연결이 바뀌었습니다. 승인과 원문 변경 전체를 취소합니다.");}
    private ApiException invalid(String message){return new ApiException(ErrorCode.VALIDATION_FAILED,HttpStatus.BAD_REQUEST,message);}
    private ApiException conflict(String message){return new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_VERSION_CONFLICT,HttpStatus.CONFLICT,message);}
    private ApiException notFound(){return new ApiException(ErrorCode.RESOURCE_NOT_FOUND,HttpStatus.NOT_FOUND,"해당 원문에 속한 일반 첨부 작업과 원복 영수증을 찾을 수 없습니다.");}
}
