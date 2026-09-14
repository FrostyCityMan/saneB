package com.saneb.domain.announcementattachment.service.impl;

import com.fasterxml.jackson.databind.*;
import com.saneb.common.error.*;
import com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentProviderQaEvidenceDao;
import com.saneb.domain.announcementattachment.qa.*;
import com.saneb.domain.announcementattachment.qa.AttachmentProviderQaStoredResultVerifier.Failure;
import com.saneb.domain.announcementattachment.vo.AttachmentProviderQaEvidenceRows.*;
import com.saneb.domain.announcementattachment.vo.AttachmentProviderQaManagementRows;
import com.saneb.domain.announcementattachment.vo.AttachmentPolicyValidationRows;
import java.time.Instant;
import java.util.*;
import java.util.function.BooleanSupplier;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** 전체 불변 실행 근거 수집기. 이 컴포넌트만으로 정책 VERIFIED/게시를 변경하지 않는다. */
@Component
public final class AttachmentProviderQaEvidenceGate implements AttachmentPolicyAdditionalQaEvidenceVerifier {
    private final AttachmentProviderQaCatalog catalog;
    private final AttachmentProviderQaStoredResultVerifier verifier;
    private final AnnouncementAttachmentProviderQaEvidenceDao dao;
    private final ObjectMapper mapper;
    private final TransactionTemplate read;
    public record SegmentEvidence(int segmentNo,UUID runId,int rowVersion,int caseCount,int fileCount,long requestReservations,long reservedBytes,
                                  String caseEvidenceHash,String completedAt){ }
    public record Evidence(int schemaVersion,String scope,UUID policyRunId,String snapshotHash,String catalogHash,String planHash,String executionCodeHash,String runtimeHash,
                           int targetCount,int caseCount,int fileCount,List<SegmentEvidence> segments) {
        public Evidence{segments=List.copyOf(segments);}
    }
    public record Assessment(String status,String reasonCode,Evidence evidence){ }
    public AttachmentProviderQaEvidenceGate(AttachmentProviderQaCatalog catalog,AttachmentProviderQaStoredResultVerifier verifier,
            AnnouncementAttachmentProviderQaEvidenceDao dao,ObjectMapper mapper,PlatformTransactionManager transactions){
        this.catalog=catalog;this.verifier=verifier;this.dao=dao;this.mapper=mapper.copy();
        read=new TransactionTemplate(transactions);read.setReadOnly(true);read.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);read.setTimeout(10);
        read.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }
    @Override public String selectStepCode(){return "PROVIDER_PROFILES";}
    @Override public String selectValidatedEvidenceHash(JsonNode evidence,AttachmentPolicyValidationSnapshotFactory.Frozen frozen,AttachmentPolicyValidationRows.Run run) {
        var current=selectAssessment(frozen,run,()->!Thread.currentThread().isInterrupted());
        try {
            if(!"PASSED".equals(current.status()) || current.evidence()==null
                    || !mapper.readTree(mapper.writeValueAsString(current.evidence())).equals(evidence))throw publicationConflict();
            return verifier.hash(current.evidence());
        } catch(ApiException exception){throw exception;}
        catch(Exception exception){throw publicationConflict();}
    }
    @Override public void validateCurrentEvidence(JsonNode evidence,AttachmentPolicyValidationSnapshotFactory.Frozen frozen,AttachmentPolicyValidationRows.Run run) {
        // V81의 게시 잠금 안에서 사용한다. 완료 run/case/plan은 DB에서 불변이므로 최신 run 소속·버전만 재확인한다.
        if(!TransactionSynchronizationManager.isActualTransactionActive() || TransactionSynchronizationManager.isCurrentTransactionReadOnly())
            throw publicationConflict();
        try {
            var saved=mapper.copy().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).treeToValue(evidence,Evidence.class);
            if(saved.schemaVersion()!=1 || !"ALL_REQUIRED_PROVIDER_QA".equals(saved.scope()) || !Objects.equals(saved.policyRunId(),run.runId())
                    || !Objects.equals(saved.snapshotHash(),frozen.hash()) || saved.segments().isEmpty())throw publicationConflict();
            var latest=dao.selectLatestRunList(new Scope(run.policyId(),saved.snapshotHash(),saved.catalogHash(),saved.planHash()));
            if(latest.size()!=saved.segments().size())throw publicationConflict();
            for(int i=0;i<latest.size();i++) {
                var actual=latest.get(i);var expected=saved.segments().get(i);
                if(!Objects.equals(actual.runId(),expected.runId()) || !"COMPLETED".equals(actual.statusCode())
                        || !Objects.equals(actual.segmentNo(),expected.segmentNo()) || !Objects.equals(actual.rowVersion(),expected.rowVersion())
                        || !Boolean.TRUE.equals(actual.inputVersionsCurrent()) || !Objects.equals(actual.expectedCaseCount(),expected.caseCount())
                        || !Objects.equals(actual.requestReservations(),expected.requestReservations()) || !Objects.equals(actual.reservedBytes(),expected.reservedBytes())
                        || actual.completedAt()==null || !actual.completedAt().toInstant().toString().equals(expected.completedAt()))throw publicationConflict();
            }
        } catch(ApiException exception){throw exception;}
        catch(Exception exception){throw publicationConflict();}
    }
    private ApiException publicationConflict(){return new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_VERSION_CONFLICT,HttpStatus.CONFLICT,
            "수집원 QA의 전체 근거가 없거나 최신 분할 실행이 바뀌었습니다. 현재 수집원 QA와 정책 전체 QA를 다시 확인하세요.");}
    public Assessment selectAssessment(AttachmentPolicyValidationSnapshotFactory.Frozen frozen,AttachmentPolicyValidationRows.Run policyRun,BooleanSupplier allowed) {
        try {
            check(allowed);
            require(frozen!=null && policyRun!=null && policyRun.startedAt()!=null && !policyRun.startedAt().toInstant().isAfter(Instant.now())
                    && Set.of("RUNNING","VERIFIED").contains(policyRun.statusCode()) && Boolean.TRUE.equals(policyRun.inputVersionsCurrent())
                    && frozen.runtime()!=null && isHash(frozen.hash()) && isHash(frozen.runtime().executionCodeHash()) && isHash(frozen.runtime().runtimeHash())
                    && Objects.equals(frozen.hash(),policyRun.snapshotHash()),"POLICY_SNAPSHOT_CHANGED");
            var json=mapper.readTree(frozen.json());
            require(json.path("schemaVersion").asInt()==6 && json.path("policyId").asText().equals(policyRun.policyId().toString())
                    && json.path("policyVersion").isIntegralNumber() && json.path("policyVersion").asInt()==policyRun.policyVersion()
                    && Objects.equals(frozen.rule().releaseId(),policyRun.ruleReleaseId()) && Objects.equals(frozen.rule().rowVersion(),policyRun.ruleVersion()),"POLICY_SNAPSHOT_CHANGED");
            var scope=mapper.treeToValue(json.path("providerQaPlan"),AttachmentProviderQaPlan.Plan.class);
            var prepared=catalog.selectPrepared(scope,frozen.rule().ruleSet(),frozen.runtime().runtimeHash(),Instant.now());var plan=prepared.plan();
            require(mapper.readTree(mapper.writeValueAsString(plan)).equals(json.path("providerQaCatalog")),"CURRENT_CATALOG_CHANGED");
            if(!plan.isExpectationCoverageComplete())return new Assessment("MISSING","ALL_PROVIDER_EXPECTATIONS_REQUIRED",null);
            require(!plan.isQaPassed() && !plan.segments().isEmpty() && plan.executableCount()==prepared.inputs().size() && plan.cases().size()==prepared.inputs().size()
                    && plan.targets().size()==scope.items().size() && plan.targets().stream().allMatch(t->t.isExpectationCoverageComplete() && t.normalNoticeCount()>=3
                    && t.requiredNormalNoticeCount()==3 && t.missingFormats().isEmpty() && "SYSTEM_BINDING_MATCHED".equals(t.bindingStatusCode())),"FULL_EXPECTATION_SCOPE_INVALID");
            var inputs=new LinkedHashMap<String,AttachmentProviderQaCase>();var entries=new LinkedHashMap<String,AttachmentProviderQaCatalog.CasePlan>();
            for(var input:prepared.inputs()){AttachmentProviderQaCaseContract.validate(input);require(inputs.put(input.caseId(),input)==null,"DUPLICATE_CASE_INPUT");}
            for(var entry:plan.cases())require("EXPECTED_INPUT_READY".equals(entry.statusCode()) && entries.put(entry.caseCode(),entry)==null
                    && inputs.containsKey(entry.caseCode()) && Objects.equals(entry.inputHash(),verifier.hash(inputs.get(entry.caseCode()))),"CASE_EXPECTATION_CHANGED");
            String planHash=verifier.hash(plan);var selectedScope=new Scope(policyRun.policyId(),frozen.hash(),plan.catalogHash(),planHash);
            return read.execute(tx->{
                check(allowed);var runs=dao.selectLatestRunList(selectedScope);
                if(runs.size()<plan.segments().size())return new Assessment("MISSING","ALL_SEGMENT_RUNS_REQUIRED",null);
                require(runs.size()==plan.segments().size(),"SEGMENT_SCOPE_CHANGED");
                var ids=new HashSet<UUID>();var runIds=new HashSet<UUID>();var codes=new HashSet<String>();var evidence=new ArrayList<SegmentEvidence>();int allFiles=0;
                for(int segmentIndex=0;segmentIndex<plan.segments().size();segmentIndex++) {
                    check(allowed);var segment=plan.segments().get(segmentIndex);var run=runs.get(segmentIndex);
                    require(segment.ordinal()==segmentIndex+1,"SEGMENT_SEQUENCE_CHANGED");
                    if(!"COMPLETED".equals(run.statusCode()))return new Assessment("MISSING","LATEST_SEGMENT_NOT_COMPLETED",null);
                    validateRun(run,segment,plan,selectedScope,frozen,policyRun);
                    require(runIds.add(run.runId()),"DUPLICATE_SEGMENT_RUN");
                    try {require(mapper.readTree(dao.selectRequiredScope(run.runId())).equals(json.path("targets")),"RUN_REQUIRED_SCOPE_CHANGED");}
                    catch(Failure failure){throw failure;}catch(Exception exception){throw new Failure("RUN_REQUIRED_SCOPE_CHANGED");}
                    long count=dao.selectEvidenceCount(run.runId());require(count==segment.caseCodes().size(),"CASE_COUNT_CHANGED");
                    long requests=0,bytes=0;int fileCount=0;var caseDigests=new ArrayList<Map<String,Object>>();Instant previousCaseEnd=null;
                    for(int offset=0;offset<count;offset+=100) {
                        check(allowed);var page=dao.selectEvidenceList(new Page(run.runId(),100,offset));require(page.size()==Math.min(100,count-offset),"CASE_PAGE_INCOMPLETE");
                        for(int index=0;index<page.size();index++) {
                            var row=page.get(index);String code=segment.caseCodes().get(offset+index);var input=inputs.get(code);var entry=entries.get(code);
                            require(row!=null && row.ordinal()!=null && row.ordinal()==offset+index+1 && Objects.equals(code,row.caseCode()) && ids.add(row.caseId()) && codes.add(code)
                                    && entry!=null && input!=null && Objects.equals(entry.expectedFileCount(),row.expectedFileCount()),"CASE_SEQUENCE_CHANGED");
                            var result=verifier.selectVerifiedResult(row,run,input,policyRun.startedAt().toInstant());
                            require(previousCaseEnd==null || !row.startedAt().toInstant().isBefore(previousCaseEnd),"CASE_EXECUTION_OVERLAP");previousCaseEnd=row.completedAt().toInstant();
                            require(!entry.normalNotice() || result.allTextComplete(),"NORMAL_NOTICE_NOT_COMPLETE");
                            requests=Math.addExact(requests,result.requestReservations());bytes=Math.addExact(bytes,result.reservedBytes());fileCount=Math.addExact(fileCount,result.fileCount());
                            caseDigests.add(Map.of("caseId",row.caseId(),"ordinal",row.ordinal(),"inputHash",row.inputHash(),"evidenceHash",row.evidenceHash(),"rowVersion",row.rowVersion(),
                                    "startedAt",row.startedAt().toInstant().toString(),"completedAt",row.completedAt().toInstant().toString()));
                        }
                    }
                    require(Objects.equals(run.requestReservations(),requests) && Objects.equals(run.reservedBytes(),bytes),"RUN_USAGE_CHANGED");
                    evidence.add(new SegmentEvidence(segment.ordinal(),run.runId(),run.rowVersion(),segment.caseCodes().size(),fileCount,requests,bytes,verifier.hash(caseDigests),run.completedAt().toInstant().toString()));
                    allFiles=Math.addExact(allFiles,fileCount);
                }
                // 분할 선택/재검증 순서는 관리자가 정할 수 있다. 분할 번호가 아니라 실제 생성 시각 순으로 전역 실행 중첩만 검사한다.
                Instant previousRunEnd=null;
                for(var completed:runs.stream().sorted(Comparator.comparing(AttachmentProviderQaManagementRows.Run::createdAt)).toList()) {
                    require(previousRunEnd==null || !completed.createdAt().toInstant().isBefore(previousRunEnd),"RUN_EXECUTION_OVERLAP");previousRunEnd=completed.completedAt().toInstant();
                }
                require(codes.equals(inputs.keySet()),"ALL_CASES_REQUIRED");check(allowed);
                var result=new Evidence(1,"ALL_REQUIRED_PROVIDER_QA",policyRun.runId(),frozen.hash(),plan.catalogHash(),planHash,frozen.runtime().executionCodeHash(),frozen.runtime().runtimeHash(),
                        plan.targets().size(),codes.size(),allFiles,evidence);
                try {require(mapper.writeValueAsBytes(result).length<=32768,"AGGREGATE_EVIDENCE_LIMIT");}
                catch(Failure failure){throw failure;}catch(Exception exception){throw new Failure("AGGREGATE_EVIDENCE_INVALID");}
                return new Assessment("PASSED","ALL_FROZEN_SEGMENTS_VERIFIED",result);
            });
        }catch(Failure failure){return new Assessment("EXECUTION_STOPPED".equals(failure.selectCode())?"CANCELLED":"FAILED",failure.selectCode(),null);}
        catch(Exception exception){return new Assessment("FAILED","PROVIDER_EVIDENCE_UNAVAILABLE",null);}
    }
    private static void validateRun(AttachmentProviderQaManagementRows.Run run,AttachmentProviderQaCatalog.Segment segment,AttachmentProviderQaCatalog.Plan plan,Scope scope,
                                    AttachmentPolicyValidationSnapshotFactory.Frozen frozen,AttachmentPolicyValidationRows.Run policyRun) {
        require(run.runId()!=null && Objects.equals(scope.policyId(),run.policyId()) && Objects.equals(scope.snapshotHash(),run.snapshotHash())
                && Objects.equals(scope.catalogHash(),run.catalogHash()) && Objects.equals(scope.planHash(),run.planHash()) && Boolean.TRUE.equals(run.inputVersionsCurrent())
                && Objects.equals(run.policyVersion(),policyRun.policyVersion()) && Objects.equals(run.ruleReleaseId(),policyRun.ruleReleaseId()) && Objects.equals(run.ruleVersion(),policyRun.ruleVersion())
                && Objects.equals(run.executionCodeHash(),frozen.runtime().executionCodeHash()) && Objects.equals(run.runtimeHash(),frozen.runtime().runtimeHash())
                && Objects.equals(run.segmentNo(),segment.ordinal()) && Objects.equals(run.segmentCount(),plan.segments().size()) && Objects.equals(run.catalogCaseCount(),plan.cases().size())
                && Objects.equals(run.executableCaseCount(),plan.executableCount()) && Boolean.TRUE.equals(run.expectationCoverageComplete()) && run.rowVersion()!=null && run.rowVersion()>=2
                && Objects.equals(run.expectedCaseCount(),segment.caseCodes().size()) && Objects.equals(run.maximumRequests(),segment.maximumRequests())
                && Objects.equals(run.maximumBytes(),segment.maximumBytes()) && run.maximumSecondsIncludingMargin()!=null && run.maximumSecondsIncludingMargin()==segment.maximumSecondsIncludingMargin(),"RUN_PLAN_CHANGED");
        require(run.createdAt()!=null && run.completedAt()!=null && run.expiresAt()!=null && !run.completedAt().isBefore(run.createdAt())
                && !run.completedAt().isAfter(run.expiresAt()) && !run.completedAt().isAfter(policyRun.startedAt()),"RUN_TIME_INVALID");
    }
    private static void check(BooleanSupplier allowed){require(allowed!=null && !Thread.currentThread().isInterrupted() && allowed.getAsBoolean(),"EXECUTION_STOPPED");}
    private static void require(boolean accepted,String code){if(!accepted)throw new Failure(code);}
    private static boolean isHash(String value){return value!=null && value.matches("[0-9a-f]{64}");}
}
