package com.saneb.domain.announcementattachment.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.common.error.ApiException;
import com.saneb.common.error.ErrorCode;
import com.saneb.common.response.PageResponse;
import com.saneb.domain.announcementattachment.dao.*;
import com.saneb.domain.announcementattachment.dto.AttachmentPolicyCheckRequest;
import com.saneb.domain.announcementattachment.dto.AttachmentProviderQaRequests.Reservation;
import com.saneb.domain.announcementattachment.dto.AttachmentProviderQaResponses;
import com.saneb.domain.announcementattachment.qa.AttachmentProviderQaCatalog;
import com.saneb.domain.announcementattachment.qa.AttachmentProviderQaCase;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentProviderQaManagementService;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentProviderQaExecutionService;
import com.saneb.domain.announcementattachment.vo.AttachmentPolicyManagementRows;
import com.saneb.domain.announcementattachment.vo.AttachmentProviderQaManagementRows;
import com.saneb.domain.announcementattachment.vo.AttachmentProviderQaRows;
import com.saneb.domain.announcementsource.dao.AnnouncementSourceDao;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceAuditLogCommand;
import com.saneb.domain.auth.vo.AuthenticatedUserDetails;
import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.*;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class AnnouncementAttachmentProviderQaManagementServiceImpl implements AnnouncementAttachmentProviderQaManagementService {
    private final AnnouncementAttachmentPolicyDao policies;
    private final AnnouncementAttachmentProviderQaDao ledger;
    private final AnnouncementAttachmentProviderQaManagementDao dao;
    private final AttachmentPolicyValidationSnapshotFactory snapshots;
    private final AttachmentProviderQaCatalog catalog;
    private final AnnouncementAttachmentProviderQaExecutionService execution;
    private final AnnouncementSourceDao audit;
    private final ObjectMapper mapper;
    private final boolean enabled;
    private final TransactionTemplate read,write;
    public AnnouncementAttachmentProviderQaManagementServiceImpl(AnnouncementAttachmentPolicyDao policies,AnnouncementAttachmentProviderQaDao ledger,
            AnnouncementAttachmentProviderQaManagementDao dao,AttachmentPolicyValidationSnapshotFactory snapshots,AttachmentProviderQaCatalog catalog,
            AnnouncementSourceDao audit,ObjectMapper mapper,PlatformTransactionManager transactions,AnnouncementAttachmentProviderQaExecutionService execution,
            @Value("${saneb.announcement-attachment.provider-qa.enabled:false}") boolean enabled) {
        this.policies=policies;this.ledger=ledger;this.dao=dao;this.snapshots=snapshots;this.catalog=catalog;this.audit=audit;this.mapper=mapper;this.enabled=enabled;
        this.execution=execution;
        read=new TransactionTemplate(transactions);read.setReadOnly(true);read.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);read.setTimeout(10);
        write=new TransactionTemplate(transactions);write.setTimeout(10);
    }
    private record Prepared(AttachmentPolicyManagementRows.Row policy,AttachmentPolicyValidationSnapshotFactory.Frozen frozen,
                            AttachmentProviderQaCatalog.Prepared catalog,String planHash,String scopeJson) {
        @Override public String toString(){return "ProviderQaPrepared[input=REDACTED]";}
    }
    @Override @Transactional(propagation=Propagation.NOT_SUPPORTED)
    public AttachmentProviderQaResponses.Preview selectExecutionPlan(Authentication actor,UUID policyId,int page,int size) {
        selectActor(actor,false);int offset=validatePage(page,size);
        var prepared=selectPrepared(policyId);var plan=prepared.catalog().plan();
        int from=Math.min(offset,plan.segments().size()),to=(int)Math.min((long)from+size,plan.segments().size());
        return new AttachmentProviderQaResponses.Preview(policyId,prepared.policy().rowVersion(),prepared.frozen().hash(),plan.catalogHash(),prepared.planHash(),
                plan.targets().size(),plan.cases().size(),plan.executableCount(),plan.isExpectationCoverageComplete(),false,enabled && !plan.segments().isEmpty(),
                PageResponse.of(plan.segments().subList(from,to),page,size,plan.segments().size()));
    }
    @Override @Transactional(propagation=Propagation.NOT_SUPPORTED)
    public AttachmentProviderQaResponses.Coverage selectTargetCoverageList(Authentication actor,UUID policyId,int page,int size) {
        selectActor(actor,false);int offset=validatePage(page,size);
        var prepared=selectPrepared(policyId);var plan=prepared.catalog().plan();
        int from=Math.min(offset,plan.targets().size()),to=(int)Math.min((long)from+size,plan.targets().size());
        return new AttachmentProviderQaResponses.Coverage(policyId,prepared.policy().rowVersion(),prepared.frozen().hash(),plan.catalogHash(),prepared.planHash(),
                plan.isExpectationCoverageComplete(),false,plan.formatCoverage(),PageResponse.of(plan.targets().subList(from,to),page,size,plan.targets().size()));
    }
    @Override @Transactional(propagation=Propagation.NOT_SUPPORTED)
    public AttachmentProviderQaResponses.Run insertRun(Authentication authentication,UUID policyId,UUID key,Reservation request) {
        UUID actor=selectActor(authentication,true);validateRequest(request);
        if(policyId==null || key==null) throw invalid("정책 ID와 UUID 형식 Idempotency-Key가 필요합니다.");
        String requestHash=snapshots.hash(List.of("attachment-provider-qa-reservation-v1",actor,policyId,request.expectedVersion(),request.expectedSnapshotHash(),
                request.expectedCatalogHash(),request.expectedPlanHash(),request.segmentNo(),request.expectedCaseCount(),request.maximumRequests(),request.maximumBytes(),
                request.maximumSecondsIncludingMargin(),request.acknowledgeScope(),request.acknowledgeNetworkBudget(),request.acknowledgeIncompleteCoverage(),request.reason().strip()));
        var previous=read.execute(tx->dao.selectRequestDetails(key));
        if(previous!=null) return selectSame(previous,actor,policyId,requestHash);
        if(!enabled) throw conflict("Provider QA 실행이 비활성 상태입니다. 격리 환경과 실행 설정을 준비한 뒤 예약하세요.");
        var prepared=selectPrepared(policyId);var plan=prepared.catalog().plan();
        var segment=selectApprovedSegment(prepared,request);
        // 입력/설치 파일 읽기와 실행 입력 구성은 쓰기 transaction 밖에서 끝낸다.
        var inputs=new HashMap<String,AttachmentProviderQaCase>();
        for(var input:prepared.catalog().inputs()) if(inputs.put(input.caseId(),input)!=null) throw conflict("고정 QA 실행 입력에 중복 공고 코드가 있습니다.");
        var casePlans=new HashMap<String,AttachmentProviderQaCatalog.CasePlan>();
        for(var item:plan.cases()) if(casePlans.put(item.caseCode(),item)!=null) throw conflict("고정 QA 계획에 중복 공고 코드가 있습니다.");
        for(String code:segment.caseCodes()) {
            var input=inputs.get(code);var item=casePlans.get(code);
            if(input==null || item==null || !"EXPECTED_INPUT_READY".equals(item.statusCode()) || !hashMatches(item.inputHash())
                    || !Objects.equals(item.expectedFileCount(),input.files().size())) throw conflict("분할 공고의 실제 실행 입력이 준비되지 않았습니다. 계획을 다시 조회하세요.");
        }
        return write.execute(tx->{
            ledger.selectQueueLock();
            var existing=dao.selectRequestDetails(key);
            if(existing!=null) return selectSame(existing,actor,policyId,requestHash);
            for(UUID active:dao.selectActiveRunIds()) {
                ledger.selectRunLock(active);ledger.updateExpiredCases(active);ledger.updateUnstartedCancelled(active);ledger.updateRunFinished(active);
            }
            if(!dao.selectActiveRunIds().isEmpty()) throw conflict("다른 Provider QA가 대기·실행·취소 중입니다. 종료 상태를 확인한 뒤 예약하세요.");
            if(dao.selectCoolingDown(policyId)) throw conflict("같은 정책의 새 Provider QA는 60초 간격으로 예약할 수 있습니다. 실패·취소도 간격에 포함됩니다.");
            policies.selectRuleStatus(prepared.policy().ruleReleaseId());var policy=selectPolicy(policyId,true);
            validateDraft(policy,request.expectedVersion());
            if(!policy.ruleReleaseId().equals(prepared.policy().ruleReleaseId())) throw conflict("준비 이후 정책의 규칙이 바뀌었습니다. 계획을 다시 조회하세요.");
            var current=snapshots.selectSnapshot(policy,prepared.frozen().runtime());
            if(!current.hash().equals(prepared.frozen().hash())) throw conflict("예약 직전 정책·규칙·대상·catalog가 바뀌었습니다. 최신 계획을 확인하세요.");
            UUID run=UUID.randomUUID();
            requireOne(ledger.insertRun(new AttachmentProviderQaRows.RunInsert(run,policyId,policy.rowVersion(),current.rule().releaseId(),current.rule().rowVersion(),
                    current.hash(),plan.catalogHash(),current.runtime().executionCodeHash(),current.runtime().runtimeHash(),prepared.scopeJson(),segment.caseCodes().size(),
                    segment.maximumRequests(),segment.maximumBytes(),actor,key,requestHash)));
            requireOne(dao.insertPlan(new AttachmentProviderQaManagementRows.PlanInsert(run,prepared.planHash(),segment.ordinal(),plan.segments().size(),plan.cases().size(),
                    plan.executableCount(),plan.isExpectationCoverageComplete(),Math.toIntExact(segment.maximumSecondsIncludingMargin()))));
            int ordinal=0;
            for(String code:segment.caseCodes()) {
                var input=inputs.get(code);var limits=input.limits();
                requireOne(ledger.insertCase(new AttachmentProviderQaRows.CaseInsert(UUID.randomUUID(),run,++ordinal,code,casePlans.get(code).inputHash(),input.profileHash(),
                        input.files().size(),limits.maximumSeconds(),limits.maximumRequestReservations(),limits.maximumReservedBytes())));
            }
            requireOne(ledger.updateReady(run));
            insertAudit(actor,policyId,"ATTACHMENT_PROVIDER_QA_RESERVE",run,request.reason());
            return selectResponse(selectRun(policyId,run));
        });
    }
    @Override @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ,timeout=10)
    public PageResponse<AttachmentProviderQaResponses.Run> selectRunList(Authentication actor,UUID policyId,int page,int size) {
        selectActor(actor,false);int offset=validatePage(page,size);selectPolicy(policyId,false);
        return PageResponse.of(dao.selectRunList(new AttachmentProviderQaManagementRows.Search(policyId,size,offset)).stream().map(this::selectResponse).toList(),
                page,size,dao.selectRunCount(policyId));
    }
    @Override @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ,timeout=10)
    public AttachmentProviderQaResponses.Run selectRunDetails(Authentication actor,UUID policyId,UUID runId) {
        selectActor(actor,false);return selectResponse(selectRun(policyId,runId));
    }
    @Override @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ,timeout=10)
    public PageResponse<AttachmentProviderQaResponses.Item> selectCaseList(Authentication actor,UUID policyId,UUID runId,int page,int size) {
        selectActor(actor,false);int offset=validatePage(page,size);selectRun(policyId,runId);
        var items=dao.selectCaseList(new AttachmentProviderQaManagementRows.CaseSearch(runId,size,offset)).stream()
                .map(c->new AttachmentProviderQaResponses.Item(c.caseId(),c.ordinal(),c.caseCode(),c.inputHash(),c.profileHash(),c.expectedFileCount(),c.statusCode(),c.rowVersion(),
                        c.requestReservations(),c.reservedBytes(),c.startedAt(),c.completedAt(),c.errorCode(),c.evidenceHash())).toList();
        return PageResponse.of(items,page,size,dao.selectCaseCount(runId));
    }
    @Override @Transactional(timeout=10)
    public AttachmentProviderQaResponses.Run updateCancellation(Authentication authentication,UUID policyId,UUID runId,AttachmentPolicyCheckRequest request) {
        UUID actor=selectActor(authentication,true);
        if(request==null || request.expectedVersion()==null || request.expectedVersion()<0 || request.reason()==null || request.reason().isBlank() || request.reason().length()>1000)
            throw invalid("실행 조회 버전은 0 이상, 취소 사유는 공백이 아닌 1~1000자여야 합니다.");
        if(policyId==null || runId==null) throw notFound();
        ledger.selectQueueLock();ledger.selectRunLock(runId);var row=selectRun(policyId,runId);
        if(!request.expectedVersion().equals(row.rowVersion())) throw conflict("조회 이후 QA 실행 버전이 바뀌었습니다. 최신 이력을 확인하세요.");
        if(!Set.of("READY","RUNNING").contains(row.statusCode())) throw conflict("대기·실행 중인 QA만 취소할 수 있습니다. 이미 종료되었거나 취소 중인 결과를 확인하세요.");
        requireOne(ledger.updateCancellation(runId,request.expectedVersion()));ledger.updateUnstartedCancelled(runId);ledger.updateExpiredCases(runId);ledger.updateRunFinished(runId);
        insertAudit(actor,policyId,"ATTACHMENT_PROVIDER_QA_CANCEL",runId,request.reason());
        return selectResponse(selectRun(policyId,runId));
    }
    @Override @Transactional(propagation=Propagation.NOT_SUPPORTED)
    public String saveNextProviderQaRun() {
        if(!enabled) return "DISABLED";
        var run=write.execute(tx->{
            ledger.selectQueueLock();var active=dao.selectActiveRunIds();if(active.isEmpty()) return null;
            UUID id=active.getFirst();ledger.selectRunLock(id);ledger.updateExpiredCases(id);ledger.updateUnstartedCancelled(id);ledger.updateRunFinished(id);
            var current=dao.selectRunDetails(id);
            return current!=null && Set.of("READY","RUNNING").contains(current.statusCode())?current:null;
        });
        if(run==null) return "IDLE";
        if(!Boolean.TRUE.equals(run.inputVersionsCurrent()) || run.planHash()==null || run.segmentNo()==null || run.maximumSecondsIncludingMargin()==null)
            return savePendingInputChanged(run.runId());
        var stored=read.execute(tx->ledger.selectCaseList(run.runId()));
        if(stored.stream().anyMatch(c->"RUNNING".equals(c.statusCode()))) return "BUSY";
        Prepared current;
        try {current=selectPrepared(run.policyId());}
        catch(ApiException exception) {
            // 설치/DB 재검증 불가를 성공이나 입력 변경으로 추정하지 않는다. 취소/만료 정리는 다음 poll에서도 실행된다.
            return "PREPARATION_UNAVAILABLE";
        }
        var plan=current.catalog().plan();
        var segment=plan.segments().stream().filter(s->Objects.equals(s.ordinal(),run.segmentNo())).findFirst().orElse(null);
        if(segment==null || !Objects.equals(run.snapshotHash(),current.frozen().hash()) || !Objects.equals(run.catalogHash(),plan.catalogHash())
                || !Objects.equals(run.planHash(),current.planHash()) || !Objects.equals(run.executionCodeHash(),current.frozen().runtime().executionCodeHash())
                || !Objects.equals(run.runtimeHash(),current.frozen().runtime().runtimeHash()) || !Objects.equals(run.policyVersion(),current.policy().rowVersion())
                || !Objects.equals(run.ruleReleaseId(),current.frozen().rule().releaseId()) || !Objects.equals(run.ruleVersion(),current.frozen().rule().rowVersion())
                || !Objects.equals(run.segmentCount(),plan.segments().size()) || !Objects.equals(run.catalogCaseCount(),plan.cases().size())
                || !Objects.equals(run.executableCaseCount(),plan.executableCount()) || !Objects.equals(run.expectationCoverageComplete(),plan.isExpectationCoverageComplete())
                || !Objects.equals(run.maximumSecondsIncludingMargin().longValue(),segment.maximumSecondsIncludingMargin())
                || !Objects.equals(run.maximumRequests(),segment.maximumRequests()) || !Objects.equals(run.maximumBytes(),segment.maximumBytes())
                || !Objects.equals(run.expectedCaseCount(),segment.caseCodes().size()) || stored.size()!=run.expectedCaseCount()
                || !stored.stream().map(AttachmentProviderQaRows.CaseRow::caseCode).toList().equals(segment.caseCodes())) return savePendingInputChanged(run.runId());
        var inputs=new HashMap<String,AttachmentProviderQaCase>();current.catalog().inputs().forEach(c->inputs.put(c.caseId(),c));
        var entries=new HashMap<String,AttachmentProviderQaCatalog.CasePlan>();plan.cases().forEach(c->entries.put(c.caseCode(),c));
        for(var row:stored) {
            var input=inputs.get(row.caseCode());var entry=entries.get(row.caseCode());
            if(input==null || entry==null || !Objects.equals(row.inputHash(),entry.inputHash()) || !Objects.equals(row.profileHash(),input.profileHash())
                    || !Objects.equals(row.expectedFileCount(),input.files().size()) || !Objects.equals(row.maximumSeconds(),input.limits().maximumSeconds())
                    || !Objects.equals(row.maximumRequests(),input.limits().maximumRequestReservations()) || !Objects.equals(row.maximumBytes(),input.limits().maximumReservedBytes()))
                return savePendingInputChanged(run.runId());
        }
        var next=stored.stream().filter(c->"PENDING".equals(c.statusCode())).findFirst().orElse(null);
        if(next==null) return "IDLE";
        // 실제 HTTP/추출은 transaction 밖에서 한 공고만 실행한다. 실행 Service가 claim 직전/요청마다 소유권을 다시 확인한다.
        var result=execution.saveCase(next.caseId(),inputs.get(next.caseCode()));
        return "INPUT_CHANGED".equals(result.statusCode())?savePendingInputChanged(run.runId()):result.statusCode();
    }
    private String savePendingInputChanged(UUID runId) {
        return write.execute(tx->{
            ledger.selectQueueLock();ledger.selectRunLock(runId);ledger.updateExpiredCases(runId);ledger.updateUnstartedCancelled(runId);
            dao.updatePendingInputChanged(runId);ledger.updateRunFinished(runId);return "INPUT_CHANGED";
        });
    }
    private Prepared selectPrepared(UUID policyId) {
        // 없는 정책은 비싼 설치 검증보다 먼저 거부한다. 쓰기 시에는 다시 잠금/현재성 대조한다.
        read.execute(tx->{var policy=selectPolicy(policyId,false);validateDraft(policy,policy.rowVersion());return policy.policyId();});
        var installed=snapshots.selectRuntime();
        return read.execute(tx->{
            var policy=selectPolicy(policyId,false);validateDraft(policy,policy.rowVersion());var frozen=snapshots.selectSnapshot(policy,installed);
            try {
                var json=mapper.readTree(frozen.json());
                if(json.path("schemaVersion").asInt()!=6 || !json.path("targets").isArray() || !json.path("providerQaCatalog").isObject()) throw conflict("현재 전체 QA 계획이 snapshot에 없습니다.");
                var scope=mapper.treeToValue(json.path("providerQaPlan"),AttachmentProviderQaPlan.Plan.class);
                var prepared=catalog.selectPrepared(scope,frozen.rule().ruleSet(),installed.runtimeHash(),Instant.now());
                // long 필드의 valueToTree(LongNode)와 JSON 재조회(IntNode)의 타입 차이를 값 변경으로 오인하지 않는다.
                if(!mapper.readTree(snapshots.json(prepared.plan())).equals(json.path("providerQaCatalog"))) throw conflict("공고 기대값의 유효기간 또는 계획이 바뀌었습니다. 다시 조회하세요.");
                return new Prepared(policy,frozen,prepared,snapshots.hash(mapper.convertValue(prepared.plan(),Object.class)),json.path("targets").toString());
            } catch(ApiException exception){throw exception;}
            catch(Exception exception){throw conflict("고정된 Provider QA 전체 계획을 읽지 못했습니다. 현재 코드와 catalog를 확인하세요.");}
        });
    }
    private AttachmentProviderQaCatalog.Segment selectApprovedSegment(Prepared prepared,Reservation request) {
        var plan=prepared.catalog().plan();
        if(!request.expectedVersion().equals(prepared.policy().rowVersion()) || !request.expectedSnapshotHash().equals(prepared.frozen().hash())
                || !request.expectedCatalogHash().equals(plan.catalogHash()) || !request.expectedPlanHash().equals(prepared.planHash()))
            throw conflict("조회 이후 정책·catalog·실행 계획 지문이 바뀌었습니다. 최신 계획을 다시 확인하세요.");
        var segment=plan.segments().stream().filter(s->s.ordinal()==request.segmentNo()).findFirst().orElseThrow(()->conflict("선택한 분할에 실행 가능한 공고 기대값이 없습니다."));
        if(segment.caseCodes().size()!=request.expectedCaseCount() || segment.maximumRequests()!=request.maximumRequests() || segment.maximumBytes()!=request.maximumBytes()
                || segment.maximumSecondsIncludingMargin()!=request.maximumSecondsIncludingMargin()) throw conflict("확인한 공고 수·요청·다운로드·시간 상한이 서버의 고정 분할 계획과 다릅니다.");
        if(!plan.isExpectationCoverageComplete() && !Boolean.TRUE.equals(request.acknowledgeIncompleteCoverage()))
            throw invalid("전체 수집원·파일 형식의 기대값이 아직 준비되지 않았습니다. 일부 분할 QA임을 확인해야 예약할 수 있습니다.");
        return segment;
    }
    private AttachmentProviderQaManagementRows.Run selectRun(UUID policy,UUID run) {
        var row=run==null?null:dao.selectRunDetails(run);if(row==null || policy==null || !policy.equals(row.policyId())) throw notFound();return row;
    }
    private AttachmentPolicyManagementRows.Row selectPolicy(UUID id,boolean lock) {
        var policy=id==null?null:policies.selectPolicyDetails(id,lock);if(policy==null || !id.equals(policy.policyId())) throw notFound();return policy;
    }
    private void validateDraft(AttachmentPolicyManagementRows.Row policy,Integer version) {
        if(!"DRAFT".equals(policy.policyStatusCode()) || !Objects.equals(version,policy.rowVersion())) throw conflict("조회한 버전의 DRAFT 정책에서만 QA를 준비할 수 있습니다.");
    }
    private AttachmentProviderQaResponses.Run selectSame(AttachmentProviderQaManagementRows.Run row,UUID actor,UUID policy,String hash) {
        if(!actor.equals(row.requestedBy()) || !policy.equals(row.policyId()) || !hash.equals(row.requestHash())) throw conflict("이 멱등 키는 다른 관리자·정책·입력에 사용됐습니다. 새 요청에는 새 키를 사용하세요.");
        return selectResponse(row);
    }
    private AttachmentProviderQaResponses.Run selectResponse(AttachmentProviderQaManagementRows.Run row) {
        return new AttachmentProviderQaResponses.Run(row.runId(),row.policyId(),row.policyVersion(),row.snapshotHash(),row.catalogHash(),row.planHash(),row.statusCode(),row.rowVersion(),
                row.expectedCaseCount(),row.maximumRequests(),row.maximumBytes(),row.requestReservations(),row.reservedBytes(),row.segmentNo(),row.segmentCount(),row.catalogCaseCount(),
                row.executableCaseCount(),row.expectationCoverageComplete(),row.maximumSecondsIncludingMargin(),Boolean.TRUE.equals(row.inputVersionsCurrent()),false,
                row.createdAt(),row.expiresAt(),row.completedAt());
    }
    private void validateRequest(Reservation r) {
        if(r==null || r.expectedVersion()==null || r.expectedVersion()<0) throw invalid("정책 조회 버전은 0 이상이어야 합니다.");
        if(!hashMatches(r.expectedSnapshotHash()) || !hashMatches(r.expectedCatalogHash()) || !hashMatches(r.expectedPlanHash())) throw invalid("조회한 snapshot·catalog·실행 계획의 소문자 16진수 지문 64자리가 각각 필요합니다.");
        if(r.segmentNo()==null || r.segmentNo()<1 || r.segmentNo()>10000 || r.expectedCaseCount()==null || r.expectedCaseCount()<1 || r.expectedCaseCount()>10000)
            throw invalid("분할 번호와 공고 수는 1~10000이어야 합니다.");
        if(r.maximumRequests()==null || r.maximumRequests()<1 || r.maximumRequests()>440000 || r.maximumBytes()==null || r.maximumBytes()<1 || r.maximumBytes()>838860800000L
                || r.maximumSecondsIncludingMargin()==null || r.maximumSecondsIncludingMargin()<61 || r.maximumSecondsIncludingMargin()>82800)
            throw invalid("상한은 요청 1~440000회, 다운로드 1~838860800000바이트, 여유 포함 시간 61~82800초여야 합니다. 조회한 분할의 정확한 값을 사용하세요.");
        if(!Boolean.TRUE.equals(r.acknowledgeScope()) || !Boolean.TRUE.equals(r.acknowledgeNetworkBudget()) || r.acknowledgeIncompleteCoverage()==null)
            throw invalid("분할 범위·외부 요청 예산을 확인하고 전체 기대값 미완료 인지 여부를 지정하세요.");
        if(r.reason()==null || r.reason().isBlank() || r.reason().length()>1000) throw invalid("예약 사유는 공백이 아닌 1~1000자여야 합니다.");
    }
    private boolean hashMatches(String hash){return hash!=null && hash.matches("[0-9a-f]{64}");}
    private int validatePage(int page,int size) {
        if(page<1 || size<1 || size>100 || ((long)page-1)*size>Integer.MAX_VALUE) throw invalid("페이지는 1 이상, 크기는 1~100이며 조회 위치는 정수 범위 이내여야 합니다.");
        return (page-1)*size;
    }
    private UUID selectActor(Authentication authentication,boolean change) {
        if(authentication==null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof AuthenticatedUserDetails actor))
            throw new ApiException(ErrorCode.AUTH_REQUIRED,HttpStatus.UNAUTHORIZED,"로그인한 운영 계정이 필요합니다.");
        var roles=change?Set.of("ADMIN"):Set.of("ADMIN","OPERATOR","APPROVER");
        if(!actor.isEnabled() || actor.passwordResetRequired() || actor.roles().stream().noneMatch(roles::contains))
            throw new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_ACTION_FORBIDDEN,HttpStatus.FORBIDDEN,
                    change?"Provider QA 예약·취소는 비밀번호 변경을 완료한 활성 ADMIN만 할 수 있습니다.":"Provider QA 이력은 활성 ADMIN, OPERATOR, APPROVER만 조회할 수 있습니다.");
        return actor.userId();
    }
    private void requireOne(int changed){if(changed!=1) throw conflict("Provider QA 저장 상태가 달라졌습니다. 최신 계획과 실행 이력을 확인하세요.");}
    private void insertAudit(UUID actor,UUID policy,String action,UUID run,String reason) {
        audit.insertAuditLog(new AnnouncementSourceAuditLogCommand(actor,action,"ANNOUNCEMENT_ATTACHMENT_POLICY",policy,"SUCCESS",
                snapshots.json(Map.of("runId",run,"reasonHash",snapshots.hash(reason.strip())))));
    }
    private ApiException invalid(String message){return new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_POLICY_INVALID,HttpStatus.BAD_REQUEST,message);}
    private ApiException conflict(String message){return new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_VERSION_CONFLICT,HttpStatus.CONFLICT,message);}
    private ApiException notFound(){return new ApiException(ErrorCode.RESOURCE_NOT_FOUND,HttpStatus.NOT_FOUND,"해당 정책 또는 정책의 Provider QA 실행을 찾을 수 없습니다.");}
}
