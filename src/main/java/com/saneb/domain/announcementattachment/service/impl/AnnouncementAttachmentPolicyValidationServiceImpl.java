package com.saneb.domain.announcementattachment.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.common.error.ApiException;
import com.saneb.common.error.ErrorCode;
import com.saneb.common.response.PageResponse;
import com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentPolicyDao;
import com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentPolicyValidationDao;
import com.saneb.domain.announcementattachment.dto.AttachmentPolicyCheckRequest;
import com.saneb.domain.announcementattachment.dto.AttachmentPolicyValidationResponse;
import com.saneb.domain.announcementattachment.dto.AttachmentProviderQaPlanResponse;
import com.saneb.domain.announcementattachment.extraction.AttachmentRuntimeGate;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentPolicyValidationService;
import com.saneb.domain.announcementattachment.vo.AttachmentPolicyManagementRows;
import com.saneb.domain.announcementattachment.vo.AttachmentPolicyValidationRows;
import com.saneb.domain.announcementsource.dao.AnnouncementSourceDao;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceAuditLogCommand;
import com.saneb.domain.auth.vo.AuthenticatedUserDetails;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.*;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class AnnouncementAttachmentPolicyValidationServiceImpl implements AnnouncementAttachmentPolicyValidationService {
    private static final List<String> STEPS=List.of("CLASSIFICATION_GOLDEN","INSTALLED_RUNTIME","PROVIDER_PROFILES","WORKER_DB_RECOVERY");
    private final AnnouncementAttachmentPolicyDao policies;
    private final AnnouncementAttachmentPolicyValidationDao dao;
    private final AttachmentPolicyValidationSnapshotFactory snapshots;
    private final AnnouncementAttachmentPolicyGoldenGate golden;
    private final AttachmentRuntimeGate runtime;
    private final AttachmentWorkerDbQaGate workerDb;
    private final AttachmentProviderQaEvidenceGate providerQa;
    private final AnnouncementSourceDao audit;
    private final ObjectMapper mapper;
    private final boolean enabled;
    private final TransactionTemplate read,write;
    public AnnouncementAttachmentPolicyValidationServiceImpl(AnnouncementAttachmentPolicyDao policies,AnnouncementAttachmentPolicyValidationDao dao,
            AttachmentPolicyValidationSnapshotFactory snapshots,AnnouncementAttachmentPolicyGoldenGate golden,AttachmentRuntimeGate runtime,AttachmentWorkerDbQaGate workerDb,
            AttachmentProviderQaEvidenceGate providerQa,AnnouncementSourceDao audit,ObjectMapper mapper,PlatformTransactionManager transactions,
            @Value("${saneb.announcement-attachment.policy-validation.enabled:false}") boolean enabled) {
        this.policies=policies;this.dao=dao;this.snapshots=snapshots;this.golden=golden;this.runtime=runtime;this.audit=audit;this.mapper=mapper;this.enabled=enabled;
        this.workerDb=workerDb;this.providerQa=providerQa;
        read=new TransactionTemplate(transactions);read.setReadOnly(true);read.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);read.setTimeout(10);
        write=new TransactionTemplate(transactions);write.setTimeout(10);
    }
    @Override @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ,timeout=10)
    public AttachmentProviderQaPlanResponse selectProviderQaPlan(Authentication actor,UUID policyId,int page,int size) {
        selectActor(actor,false);var policy=selectPolicy(policyId,false);
        if(page<1 || size<1 || size>100 || ((long)page-1)*size>Integer.MAX_VALUE) throw invalid("페이지는 1 이상, 크기는 1~100이어야 합니다.");
        var plan=snapshots.selectProviderQaPlan();
        int from=Math.min((page-1)*size,plan.items().size()),to=Math.min(from+size,plan.items().size());
        return new AttachmentProviderQaPlanResponse(snapshots.hash(plan),snapshots.selectPolicyManifestCurrent(policy),plan.summary(),
                PageResponse.of(plan.items().subList(from,to),page,size,plan.items().size()));
    }
    @Override @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ,timeout=10)
    public PageResponse<AttachmentPolicyValidationResponse> selectRunList(Authentication actor,UUID policyId,int page,int size) {
        selectActor(actor,false);selectPolicy(policyId,false);
        if(page<1 || size<1 || size>100 || ((long)page-1)*size>Integer.MAX_VALUE) throw invalid("페이지는 1 이상, 크기는 1~100이어야 합니다.");
        var search=new AttachmentPolicyValidationRows.Search(policyId,size,(page-1)*size);
        return PageResponse.of(dao.selectRunList(search).stream().map(this::selectResponse).toList(),page,size,dao.selectRunCount(search));
    }
    @Override @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ,timeout=10)
    public AttachmentPolicyValidationResponse selectRunDetails(Authentication actor,UUID policyId,UUID runId) {
        selectActor(actor,false);return selectResponse(selectRun(policyId,runId,false));
    }
    @Override @Transactional(propagation=Propagation.NOT_SUPPORTED)
    public AttachmentPolicyValidationResponse insertRun(Authentication authentication,UUID policyId,UUID key,AttachmentPolicyCheckRequest request) {
        UUID actor=selectActor(authentication,true);validateRequest(request);
        if(policyId==null || key==null) throw invalid("정책 ID와 UUID 형식 Idempotency-Key가 필요합니다.");
        String requestHash=snapshots.hash(List.of("attachment-policy-validation-v1",actor,policyId,request.expectedVersion(),request.reason().strip()));
        var previous=read.execute(tx->dao.selectRequestDetails(key));
        if(previous!=null) return read.execute(tx->selectSame(previous,actor,policyId,requestHash));
        if(!enabled) throw conflict("정책 QA worker가 비활성 상태입니다. 서버 설정과 격리 실행 환경을 준비한 뒤 예약하세요.");
        // 파일/코드 지문 읽기는 예약 transaction 밖에서 한다. 이미 완료한 같은 요청은 재실행하지 않는다.
        var installed=snapshots.selectRuntime();
        return write.execute(tx->{
            dao.selectQueueLock();dao.updateExpired();
            var existing=dao.selectRequestDetails(key);
            if(existing!=null) return selectSame(existing,actor,policyId,requestHash);
            if(dao.selectActiveCount()>0) throw conflict("다른 정책 QA가 대기 또는 실행 중입니다. 완료·취소 결과를 확인한 뒤 예약하세요.");
            if(dao.selectCoolingDown(policyId) || dao.selectRecentCount(policyId)>=3)
                throw conflict("정책 QA는 같은 정책에서 60초 간격, 최근 24시간 최대 3회까지 예약할 수 있습니다. 실패·취소도 횟수에 포함됩니다.");
            var locator=selectPolicy(policyId,false);policies.selectRuleStatus(locator.ruleReleaseId());
            var policy=selectPolicy(policyId,true);validateDraft(policy,request.expectedVersion());
            if(!locator.ruleReleaseId().equals(policy.ruleReleaseId())) throw conflict("잠금 대기 중 정책의 규칙이 바뀌었습니다. 최신 초안을 다시 확인하세요.");
            var frozen=snapshots.selectSnapshot(policy,installed);
            UUID id=UUID.randomUUID();
            if(dao.insertRun(new AttachmentPolicyValidationRows.Insert(id,policyId,policy.rowVersion(),frozen.rule().releaseId(),frozen.rule().rowVersion(),
                    frozen.hash(),frozen.json(),actor,key,requestHash))!=1) throw conflict("정책 QA 예약을 저장하지 못했습니다.");
            insertAudit(actor,policyId,"ATTACHMENT_POLICY_QA_RESERVE",id,request.reason());
            return selectResponse(selectRun(policyId,id,false));
        });
    }
    @Override @Transactional(timeout=10)
    public AttachmentPolicyValidationResponse updateCancellation(Authentication authentication,UUID policyId,UUID runId,AttachmentPolicyCheckRequest request) {
        UUID actor=selectActor(authentication,true);validateRequest(request);dao.selectQueueLock();
        var row=selectRun(policyId,runId,true);
        if(!request.expectedVersion().equals(row.rowVersion())) throw conflict("조회 이후 QA 실행 상태가 바뀌었습니다. 현재 실행 버전을 다시 확인하세요.");
        if(!Set.of("PENDING","RUNNING").contains(row.statusCode())) throw conflict("대기·실행 중인 QA만 취소할 수 있습니다. 이미 종료되었거나 취소 중인 결과를 확인하세요.");
        if(dao.updateCancellation(runId,request.expectedVersion())!=1) throw conflict("QA 상태가 동시에 변경됐습니다. 취소 결과를 다시 확인하세요.");
        insertAudit(actor,policyId,"ATTACHMENT_POLICY_QA_CANCEL",runId,request.reason());
        return selectResponse(selectRun(policyId,runId,false));
    }
    @Override @Transactional(propagation=Propagation.NOT_SUPPORTED)
    public String saveNextValidationRun() {
        if(!enabled) return "DISABLED";
        var run=write.execute(tx->{
            dao.selectQueueLock();dao.updateExpired();
            var pending=dao.selectPendingDetails();if(pending==null) return null;
            UUID token=UUID.randomUUID();
            if(dao.updateClaim(pending.runId(),token)!=1) throw conflict("QA 예약 소유권을 얻지 못했습니다.");
            if(dao.insertExtractionLease(pending.runId(),token)!=1) {tx.setRollbackOnly();return null;}
            return dao.selectRunDetails(pending.runId(),false);
        });
        if(run==null) return "IDLE";
        String stage="PREPARE";
        try {
            var frozen=selectCurrentSnapshot(run);
            stage="CLASSIFICATION_GOLDEN";
            if(!selectAllowed(run)) return saveFinish(run,"CANCELLED","EXECUTION_STOPPED");
            var configuration=frozen.selectConfiguration();
            var result=golden.selectValidatedResult(frozen.rule().ruleSet(),frozen.rule().calculatedSnapshotHash(),configuration);
            if(!AnnouncementAttachmentPolicyGoldenGate.selectContractCurrent(result,configuration)
                    || !result.ruleSnapshotHash().equals(frozen.rule().calculatedSnapshotHash())) throw conflict("분류 QA snapshot과 필수 case 목록이 일치하지 않습니다.");
            saveStep(run,stage,"PASSED",result);
            stage="INSTALLED_RUNTIME";
            var extracted=runtime.selectValidatedResult(()->selectAllowed(run));
            if(extracted.caseCount()!=AttachmentRuntimeGate.CASE_COUNT || extracted.cases().size()!=AttachmentRuntimeGate.CASE_COUNT || !AttachmentRuntimeGate.SCOPE.equals(extracted.scope())
                    || !AttachmentRuntimeGate.SUITE_VERSION.equals(extracted.suiteVersion())
                    || !extracted.cases().stream().map(AttachmentRuntimeGate.CaseResult::caseId).toList()
                        .equals(java.util.stream.IntStream.rangeClosed(1,AttachmentRuntimeGate.CASE_COUNT).mapToObj(n->String.format(java.util.Locale.ROOT,"AR-%03d",n)).toList())
                    || extracted.cases().stream().anyMatch(c->!c.originalRemoved()) || !extracted.runtimeHash().equals(frozen.runtime().runtimeHash())
                    || !extracted.suiteHash().equals(frozen.runtime().runtimeSuiteHash())) throw conflict("설치 runtime QA 지문이 예약과 다릅니다.");
            saveStep(run,stage,"PASSED",extracted);
            // 실제 Provider 전체 파일 증거는 별도다. DB 합성 suite 성공으로 대신하지 않는다.
            stage="PROVIDER_PROFILES";
            var provider=providerQa.selectAssessment(frozen,run,()->selectAllowed(run));
            if(provider==null || !Set.of("PASSED","MISSING","FAILED","CANCELLED").contains(provider.status()))throw conflict("수집원 QA 결과를 확인하지 못했습니다.");
            if("CANCELLED".equals(provider.status()))return saveFinish(run,"CANCELLED","EXECUTION_STOPPED");
            boolean providerPassed="PASSED".equals(provider.status());
            if(providerPassed && provider.evidence()==null)throw conflict("수집원 QA가 통과했으나 전체 실행 근거가 없습니다.");
            saveStep(run,stage,provider.status(),providerPassed?mapper.convertValue(provider.evidence(),Object.class):Map.of("reasonCode",provider.reasonCode()));
            if("FAILED".equals(provider.status()))return saveFinish(run,"FAILED","PROVIDER_QA_FAILED");
            stage="WORKER_DB_RECOVERY";
            var database=workerDb.selectValidatedResult(frozen,run,()->selectAllowed(run));
            if(database==null) throw conflict("독립 DB QA의 실제 실행 근거가 없습니다.");
            // JSONB는 object field 순서를 보존하지 않는다. Map으로 정규화해 저장/게시 지문을 동일하게 계산한다.
            saveStep(run,stage,"PASSED",mapper.convertValue(database,Object.class));
            // saveFinish가 잠금 밖 설치 재검증 후 잠금 안 DB 입력을 재대조한다. 동일한 inventory를 두 번 실행하지 않는다.
            return saveFinish(run,providerPassed?"VERIFIED":"INCOMPLETE",providerPassed?null:"REQUIRED_QA_EVIDENCE_MISSING");
        } catch(AttachmentWorkerDbQaProcess.Failure exception) {
            saveFailureStep(run,stage,Map.of("reasonCode",exception.selectCode()));
            return saveFinish(run,"EXECUTION_STOPPED".equals(exception.selectCode())?"CANCELLED":"FAILED","WORKER_DB_QA_FAILED");
        } catch(AttachmentRuntimeGate.GateFailure exception) {
            saveFailureStep(run,stage,Map.of("reasonCode",exception.selectCode(),"caseId",exception.selectCaseId()));
            return saveFinish(run,"FAILED","RUNTIME_QA_FAILED");
        } catch(ApiException exception) {
            boolean failed=exception.errorCode()==ErrorCode.ANNOUNCEMENT_ATTACHMENT_POLICY_QA_FAILED;
            saveFailureStep(run,stage,Map.of("reasonCode",failed?"QA_CHECK_FAILED":"VALIDATION_INPUT_CHANGED"));
            return saveFinish(run,failed?"FAILED":"CONFLICT",failed?"QA_CHECK_FAILED":"VALIDATION_INPUT_OR_RESULT_CHANGED");
        } catch(RuntimeException exception) {
            // DB 장애로 상태 저장까지 실패하면 live lease를 유지하고 만료 회수에서 FAILED로 처리한다.
            saveFailureStep(run,stage,Map.of("reasonCode","QA_EXECUTION_FAILED"));
            return saveFinish(run,"FAILED","QA_EXECUTION_FAILED");
        } finally {
            write.executeWithoutResult(tx->dao.deleteExtractionLease(run.runId(),run.leaseToken()));
        }
    }
    private AttachmentPolicyValidationSnapshotFactory.Frozen selectCurrentSnapshot(AttachmentPolicyValidationRows.Run run) {
        var installed=snapshots.selectRuntime();
        return read.execute(tx->{
            var policy=selectPolicy(run.policyId(),false);validateDraft(policy,run.policyVersion());
            var current=snapshots.selectSnapshot(policy,installed);
            if(!run.snapshotHash().equals(current.hash())) throw conflict("예약 이후 정책·규칙·profile·대상·설치 지문이 바뀌었습니다. 새 QA를 예약하세요.");
            return current;
        });
    }
    private boolean selectAllowed(AttachmentPolicyValidationRows.Run run) {
        return !Thread.currentThread().isInterrupted() && Boolean.TRUE.equals(read.execute(tx->dao.selectExecutionAllowed(run.runId(),run.leaseToken())));
    }
    private void saveStep(AttachmentPolicyValidationRows.Run run,String step,String status,Object evidence) {
        String json=snapshots.json(evidence);
        if(json.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>32768) throw conflict("QA 단계 증거의 저장 한도를 초과했습니다.");
        write.executeWithoutResult(tx->{
            if(dao.insertStep(run.runId(),run.leaseToken(),step,status,json,snapshots.hash(evidence))!=1)
                throw conflict("QA 실행 소유권이 만료되었거나 취소됐습니다.");
        });
    }
    private void saveFailureStep(AttachmentPolicyValidationRows.Run run,String step,Map<String,Object> evidence) {
        if(!STEPS.contains(step)) return;
        if(selectAllowed(run) && Boolean.TRUE.equals(read.execute(tx->dao.selectStepList(run.runId()).stream().noneMatch(s->step.equals(s.stepCode())))))
            saveStep(run,step,"FAILED",evidence);
    }
    private String saveFinish(AttachmentPolicyValidationRows.Run run,String status,String error) {
        var installed=Set.of("INCOMPLETE","VERIFIED").contains(status)?snapshots.selectRuntime():null;
        return write.execute(tx->{
            if("VERIFIED".equals(status)) {
                var stored=dao.selectStepList(run.runId());
                if(stored.size()!=STEPS.size() || !stored.stream().map(AttachmentPolicyValidationRows.Step::stepCode).collect(java.util.stream.Collectors.toSet()).equals(Set.copyOf(STEPS))
                        || stored.stream().anyMatch(s->!run.runId().equals(s.runId()) || !"PASSED".equals(s.statusCode())))
                    throw conflict("필수 QA 네 단계의 저장된 통과 근거가 모두 있어야 검증을 완료할 수 있습니다.");
            }
            if(installed!=null) {
                policies.selectRuleStatus(run.ruleReleaseId());
                var policy=selectPolicy(run.policyId(),true);validateDraft(policy,run.policyVersion());
                if(!run.snapshotHash().equals(snapshots.selectSnapshot(policy,installed).hash())) throw conflict("완료 저장 직전 QA 입력이 바뀌었습니다.");
            }
            if(dao.updateFinished(run.runId(),run.leaseToken(),status,error)!=1) return "LEASE_LOST";
            return dao.selectRunDetails(run.runId(),false).statusCode();
        });
    }
    private AttachmentPolicyManagementRows.Row selectPolicy(UUID id,boolean lock) {
        var row=id==null?null:policies.selectPolicyDetails(id,lock);
        if(row==null || !id.equals(row.policyId())) throw notFound();return row;
    }
    private AttachmentPolicyValidationRows.Run selectRun(UUID policyId,UUID runId,boolean lock) {
        var row=runId==null?null:dao.selectRunDetails(runId,lock);
        if(row==null || policyId==null || !policyId.equals(row.policyId())) throw notFound();return row;
    }
    private AttachmentPolicyValidationResponse selectSame(AttachmentPolicyValidationRows.Run row,UUID actor,UUID policyId,String requestHash) {
        if(!actor.equals(row.requestedBy()) || !policyId.equals(row.policyId()) || !requestHash.equals(row.requestHash()))
            throw conflict("이 멱등 키는 다른 관리자·정책·입력의 QA 예약에 사용됐습니다. 새 요청에는 새 키를 사용하세요.");
        return selectResponse(row);
    }
    private AttachmentPolicyValidationResponse selectResponse(AttachmentPolicyValidationRows.Run row) {
        var saved=dao.selectStepList(row.runId());
        List<AttachmentPolicyValidationResponse.Step> steps=new ArrayList<>();
        for(String code:STEPS) {
            var step=saved.stream().filter(s->code.equals(s.stepCode())).findFirst().orElse(null);
            if(step==null) steps.add(new AttachmentPolicyValidationResponse.Step(code,"NOT_RUN",Map.of(),null));
            else try {
                if(step.evidenceJson()==null || step.evidenceJson().length()>32768) throw conflict("QA 단계 이력을 읽을 수 없습니다.");
                Map<String,Object> value=mapper.readValue(step.evidenceJson(),mapper.getTypeFactory().constructMapType(Map.class,String.class,Object.class));
                steps.add(new AttachmentPolicyValidationResponse.Step(code,step.statusCode(),value,step.evidenceHash()));
            } catch(Exception exception) {throw conflict("저장된 QA 단계 이력을 읽을 수 없습니다.");}
        }
        return new AttachmentPolicyValidationResponse(row.runId(),row.policyId(),row.policyVersion(),row.ruleReleaseId(),row.ruleVersion(),row.snapshotHash(),
                row.statusCode(),row.rowVersion(),Boolean.TRUE.equals(row.inputVersionsCurrent()),row.errorCode(),row.createdAt(),row.startedAt(),row.completedAt(),steps);
    }
    private void validateDraft(AttachmentPolicyManagementRows.Row row,Integer version) {
        if(!"DRAFT".equals(row.policyStatusCode()) || !version.equals(row.rowVersion())) throw conflict("QA는 조회한 버전의 DRAFT 정책에서만 실행할 수 있습니다. 현재 초안을 확인하세요.");
    }
    private void validateRequest(AttachmentPolicyCheckRequest request) {
        if(request==null || request.expectedVersion()==null || request.expectedVersion()<0 || request.reason()==null || request.reason().isBlank() || request.reason().length()>1000)
            throw invalid("조회 버전은 0 이상, 사유는 공백이 아닌 1~1000자여야 합니다.");
    }
    private UUID selectActor(Authentication authentication,boolean change) {
        if(authentication==null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof AuthenticatedUserDetails actor))
            throw new ApiException(ErrorCode.AUTH_REQUIRED,HttpStatus.UNAUTHORIZED,"로그인한 운영 계정이 필요합니다.");
        var roles=change?Set.of("ADMIN"):Set.of("ADMIN","OPERATOR","APPROVER");
        if(!actor.isEnabled() || actor.passwordResetRequired() || actor.roles().stream().noneMatch(roles::contains))
            throw new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_ACTION_FORBIDDEN,HttpStatus.FORBIDDEN,
                    change?"정책 QA 예약·취소는 비밀번호 변경을 완료한 활성 ADMIN만 할 수 있습니다.":"정책 QA 이력은 활성 ADMIN, OPERATOR, APPROVER만 조회할 수 있습니다.");
        return actor.userId();
    }
    private void insertAudit(UUID actor,UUID policy,String action,UUID run,String reason) {
        audit.insertAuditLog(new AnnouncementSourceAuditLogCommand(actor,action,"ANNOUNCEMENT_ATTACHMENT_POLICY",policy,"SUCCESS",
                snapshots.json(Map.of("runId",run,"reasonHash",snapshots.hash(reason.strip())))));
    }
    private ApiException invalid(String message) {return new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_POLICY_INVALID,HttpStatus.BAD_REQUEST,message);}
    private ApiException conflict(String message) {return new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_VERSION_CONFLICT,HttpStatus.CONFLICT,message);}
    private ApiException notFound() {return new ApiException(ErrorCode.RESOURCE_NOT_FOUND,HttpStatus.NOT_FOUND,"해당 정책 또는 정책의 QA 실행을 찾을 수 없습니다.");}
}
