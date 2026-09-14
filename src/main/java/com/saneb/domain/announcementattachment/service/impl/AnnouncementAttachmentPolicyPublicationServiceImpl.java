package com.saneb.domain.announcementattachment.service.impl;

import com.saneb.common.error.*;
import com.saneb.domain.announcementattachment.dao.*;
import com.saneb.domain.announcementattachment.dto.AttachmentPolicyPublication.*;
import com.saneb.domain.announcementattachment.dto.AttachmentPolicyPublicationScope;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentPolicyPublicationService;
import com.saneb.domain.announcementattachment.vo.AttachmentPolicyManagementRows;
import com.saneb.domain.announcementattachment.vo.AttachmentPolicyPublicationRows;
import com.saneb.domain.announcementattachment.vo.AttachmentPolicyValidationRows;
import com.saneb.domain.announcementsource.dao.AnnouncementSourceDao;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceAuditLogCommand;
import com.saneb.domain.auth.vo.AuthenticatedUserDetails;
import java.time.OffsetDateTime;
import java.util.*;
import org.springframework.dao.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.*;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class AnnouncementAttachmentPolicyPublicationServiceImpl implements AnnouncementAttachmentPolicyPublicationService {
    private final AnnouncementAttachmentPolicyPublicationDao dao;
    private final AnnouncementAttachmentPolicyPublicationScopeDao scopes;
    private final AnnouncementAttachmentPolicyDao policies;
    private final AnnouncementAttachmentPolicyValidationDao qa;
    private final AttachmentPolicyValidationSnapshotFactory snapshots;
    private final AttachmentPolicyPublicationQaVerifier verifier;
    private final AnnouncementSourceDao audit;
    private final TransactionTemplate read,write;
    public AnnouncementAttachmentPolicyPublicationServiceImpl(AnnouncementAttachmentPolicyPublicationDao dao,AnnouncementAttachmentPolicyPublicationScopeDao scopes,
            AnnouncementAttachmentPolicyDao policies,AnnouncementAttachmentPolicyValidationDao qa,AttachmentPolicyValidationSnapshotFactory snapshots,
            AttachmentPolicyPublicationQaVerifier verifier,AnnouncementSourceDao audit,PlatformTransactionManager transactions){
        this.dao=dao;this.scopes=scopes;this.policies=policies;this.qa=qa;this.snapshots=snapshots;this.verifier=verifier;this.audit=audit;
        read=new TransactionTemplate(transactions);read.setReadOnly(true);read.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);read.setTimeout(15);
        // 잠금을 획득한 뒤의 최신 committed 상태를 읽는다. 잠금 전에 만든 RR snapshot을 재사용하지 않는다.
        write=new TransactionTemplate(transactions);write.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);write.setTimeout(15);
    }
    @Override @Transactional(propagation=Propagation.NOT_SUPPORTED)
    public Result insertPublication(Authentication authentication,UUID policyId,UUID key,Request request){
        UUID actor=selectActor(authentication,true);validateRequest(policyId,key,request);
        String reasonHash=snapshots.hash(request.reason().strip());
        String requestHash=snapshots.hash(List.of("attachment-policy-publication-v1",actor,policyId,request.scopeId(),request.scopeHash(),request.expectedVersion(),reasonHash));
        var previous=read.execute(tx->dao.selectRequestDetails(key));
        if(previous!=null)return read.execute(tx->selectSame(previous,actor,policyId,requestHash));
        // 설치 파일/격리 도구 identity는 쓰기 잠금 밖에서 읽는다. 같은 요청의 재전송은 재검증하지 않는다.
        var installed=snapshots.selectRuntime();
        try {
            var prepared=read.execute(tx->selectPrepared(policyId,request,installed));
            // 전체 코드/fixture/추가 QA 검증은 DB transaction 밖에서 수행한다.
            String evidenceHash=verifier.selectValidatedEvidenceHash(prepared.run(),prepared.steps(),prepared.frozen());
            if(!installed.equals(snapshots.selectRuntime()))throw conflict("QA 근거 확인 중 설치 런타임 또는 애플리케이션 코드가 바뀌었습니다. 새 설치에서 전체 QA를 다시 실행하세요.");
            return write.execute(tx->{
                dao.selectPublicationLock();
                var winner=dao.selectRequestDetails(key);if(winner!=null)return selectSame(winner,actor,policyId,requestHash);
                // 잠금 대기 사이의 변경은 최신 DB 상태/불변 근거와 다시 비교한다. 앞선 RR 결과만 믿고 게시하지 않는다.
                var current=selectPrepared(policyId,request,installed);
                if(!prepared.scope().equals(current.scope()) || !prepared.policy().equals(current.policy())
                        || !prepared.run().equals(current.run()) || !prepared.steps().equals(current.steps())
                        || !prepared.frozen().hash().equals(current.frozen().hash()) || !prepared.frozen().json().equals(current.frozen().json()))
                    throw conflict("QA 근거 확인 이후 정책·범위·최신 실행 또는 단계 근거가 바뀌었습니다. 현재 입력의 전체 QA와 게시 범위를 다시 확인하세요.");
                verifier.validateCurrentEvidence(current.run(),current.steps(),current.frozen());
                var scope=current.scope();var run=current.run();
                UUID id=UUID.randomUUID();
                if(dao.insertPublication(new AttachmentPolicyPublicationRows.Insert(id,policyId,scope.scopeId(),actor,key,requestHash,reasonHash,evidenceHash,installed.runtimeHash()))!=1)
                    throw conflict("게시 범위의 준비 관리자와 현재 관리자가 다르거나 게시 이력이 변경됐습니다.");
                var receipt=selectReceipt(policyId);
                if(!id.equals(receipt.publicationId()) || !scope.scopeId().equals(receipt.scopeId()) || !scope.qaSnapshotHash().equals(receipt.policyHash()))throw conflict("게시 영수증의 정책·범위·지문이 일치하지 않습니다.");
                if(dao.updatePreviousPolicyRetired(id)!=(receipt.previousPolicyId()==null?0:1) || dao.updatePolicyActive(id)!=1)
                    throw conflict("이전 정책 퇴역과 새 정책 게시를 함께 완료하지 못했습니다. 전체 변경을 취소합니다.");
                audit.insertAuditLog(new AnnouncementSourceAuditLogCommand(actor,"ATTACHMENT_POLICY_PUBLISH","ANNOUNCEMENT_ATTACHMENT_POLICY",policyId,"SUCCESS",
                        snapshots.json(Map.of("publicationId",id,"scopeId",scope.scopeId(),"scopeHash",scope.scopeHash(),"qaRunId",run.runId(),"evidenceHash",evidenceHash,"reasonHash",reasonHash,"existingDataApplied",false))));
                return new Result(receipt,false,false,0);
            });
        }catch(ApiException exception){
            // 잠금 밖 검증 중 같은 요청의 다른 실행이 먼저 게시했으면 원래 영수증을 반환한다.
            var winner=read.execute(tx->dao.selectRequestDetails(key));
            if(winner!=null)return read.execute(tx->selectSame(winner,actor,policyId,requestHash));
            throw exception;
        }catch(PessimisticLockingFailureException exception){
            // 다른 게시가 끝나기 전에는 성공/실패를 추정하지 않는다. 원래 키로 재전송할 수 있다.
            throw conflict("수집·검수·규칙 또는 다른 게시 작업이 DB를 사용 중입니다. 잠금을 기다리거나 자동 실행하지 않았습니다. 원래 요청 키를 유지하고 결과를 다시 확인하세요.");
        }catch(DataIntegrityViolationException exception){
            var winner=read.execute(tx->dao.selectRequestDetails(key));
            if(winner!=null)return read.execute(tx->selectSame(winner,actor,policyId,requestHash));
            throw conflict("게시 중 정책·범위·QA 제약이 변경됐습니다. 원래 요청 키로 게시 결과를 먼저 확인하세요.");
        }
    }
    private record Prepared(AttachmentPolicyPublicationScope.Summary scope,AttachmentPolicyManagementRows.Row policy,
            AttachmentPolicyValidationRows.Run run,List<AttachmentPolicyValidationRows.Step> steps,
            AttachmentPolicyValidationSnapshotFactory.Frozen frozen) { }
    private Prepared selectPrepared(UUID policyId,Request request,AttachmentPolicyValidationSnapshotFactory.Runtime installed){
        var scope=scopes.selectScopeDetails(policyId,request.scopeId());
        if(scope==null || !policyId.equals(scope.policyId()) || !request.scopeId().equals(scope.scopeId()))throw notFound();
        if(!request.expectedVersion().equals(scope.policyVersion()) || !request.scopeHash().equals(scope.scopeHash())
                || scope.expiresAt()==null || !scope.expiresAt().isAfter(OffsetDateTime.now()) || !scopes.selectScopeCurrent(scope.scopeId()))
            throw conflict("승인한 게시 준비 범위가 만료되었거나 ID·상태·버전이 바뀌었습니다. 변경된 영향을 다시 검토하고 새 범위를 준비하세요.");
        if(scope.qaRunId()==null || scope.qaSnapshotHash()==null)throw conflict("준비 범위에 최신 전체 QA 성공이 연결되지 않았습니다. 전체 QA 후 새 게시 범위를 준비하세요.");
        var policy=policies.selectPolicyDetails(policyId,false);
        if(policy==null || !policyId.equals(policy.policyId()) || !request.expectedVersion().equals(policy.rowVersion())
                || !"DRAFT".equals(policy.policyStatusCode()) || !"ACTIVE".equals(policy.ruleReleaseStatusCode()))throw conflict("현재 ACTIVE 규칙의 최신 DRAFT 정책만 게시할 수 있습니다.");
        var run=qa.selectRunDetails(scope.qaRunId(),false);
        var latest=qa.selectRunList(new AttachmentPolicyValidationRows.Search(policyId,1,0));
        if(run==null || !policyId.equals(run.policyId()) || !scope.qaRunId().equals(run.runId()) || latest.size()!=1 || !run.runId().equals(latest.getFirst().runId())
                || !scope.policyVersion().equals(run.policyVersion()) || !scope.ruleReleaseId().equals(run.ruleReleaseId())
                || !scope.ruleVersion().equals(run.ruleVersion()) || !scope.qaSnapshotHash().equals(run.snapshotHash()))
            throw conflict("준비 범위의 QA와 최신 실행의 정책·규칙·지문이 일치하지 않습니다. 이전 성공으로 최신 실패를 대신할 수 없습니다.");
        return new Prepared(scope,policy,run,List.copyOf(qa.selectStepList(run.runId())),snapshots.selectSnapshot(policy,installed));
    }
    @Override @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ,timeout=15)
    public Result selectPublicationDetails(Authentication actor,UUID policyId){selectActor(actor,false);return new Result(selectReceipt(policyId),false,false,0);}
    private Result selectSame(AttachmentPolicyPublicationRows.Request row,UUID actor,UUID policyId,String hash){
        if(!actor.equals(row.actorId()) || !policyId.equals(row.policyId()) || !hash.equals(row.requestHash()))throw conflict("이 멱등 키는 다른 관리자·정책·게시 입력에 사용됐습니다.");
        var receipt=selectReceipt(policyId);if(!row.publicationId().equals(receipt.publicationId()))throw conflict("원래 게시 요청과 영수증의 소속이 일치하지 않습니다.");
        return new Result(receipt,false,false,0);
    }
    private Receipt selectReceipt(UUID policyId){
        var r=policyId==null?null:dao.selectReceiptDetails(policyId);
        if(r==null || !policyId.equals(r.policyId()))throw notFound();
        if(r.publicationId()==null || r.scopeId()==null || r.qaRunId()==null || r.publishedPolicyVersion()==null || r.publishedPolicyVersion()<1
                || !digest(r.policyHash()) || !digest(r.scopeHash()) || r.modeCode()==null || !Set.of("OFF","COLLECT_ONLY","ENFORCE").contains(r.modeCode())
                || r.publishedAt()==null || (r.previousPolicyId()==null)!=(r.previousPolicyVersion()==null)
                || r.previousPolicyVersion()!=null&&r.previousPolicyVersion()<0)throw conflict("저장된 게시 영수증의 버전·지문·소속이 유효하지 않습니다.");
        return r;
    }
    private void validateRequest(UUID policy,UUID key,Request r){
        if(policy==null || key==null || r==null || r.scopeId()==null || !digest(r.scopeHash()) || r.expectedVersion()==null || r.expectedVersion()<0 || r.expectedVersion()==Integer.MAX_VALUE
                || !Boolean.TRUE.equals(r.acknowledgeNewCollectionBehavior()) || !Boolean.TRUE.equals(r.acknowledgeExistingJobsUnchanged()) || !Boolean.TRUE.equals(r.acknowledgeNoBackfill())
                || r.reason()==null || r.reason().isBlank() || r.reason().length()>1000)throw new ApiException(ErrorCode.VALIDATION_FAILED,HttpStatus.BAD_REQUEST,"정확한 준비 UUID·SHA-256 지문·조회 버전과 신규 수집/기존 작업/별도 배치 영향 세 가지 확인, 공백이 아닌 1~1000자 사유가 필요합니다.");
    }
    private UUID selectActor(Authentication authentication,boolean change){
        if(authentication==null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof AuthenticatedUserDetails user))throw new ApiException(ErrorCode.AUTH_REQUIRED,HttpStatus.UNAUTHORIZED,"로그인한 운영 계정이 필요합니다.");
        if(!user.isEnabled() || user.passwordResetRequired() || user.roles().stream().noneMatch((change?Set.of("ADMIN"):Set.of("ADMIN","OPERATOR","APPROVER"))::contains))
            throw new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_ACTION_FORBIDDEN,HttpStatus.FORBIDDEN,change?"정책 게시는 비밀번호 변경을 완료한 활성 ADMIN만 할 수 있습니다.":"게시 영수증은 활성 ADMIN·OPERATOR·APPROVER만 조회할 수 있습니다.");
        return user.userId();
    }
    private boolean digest(String value){return value!=null&&value.matches("[0-9a-f]{64}");}
    private ApiException conflict(String message){return new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_VERSION_CONFLICT,HttpStatus.CONFLICT,message);}
    private ApiException notFound(){return new ApiException(ErrorCode.RESOURCE_NOT_FOUND,HttpStatus.NOT_FOUND,"선택 정책의 게시 준비 범위 또는 게시 영수증을 찾을 수 없습니다.");}
}
