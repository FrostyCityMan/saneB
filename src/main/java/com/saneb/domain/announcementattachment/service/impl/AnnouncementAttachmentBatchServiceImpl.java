package com.saneb.domain.announcementattachment.service.impl;

import com.fasterxml.jackson.databind.*;
import com.saneb.common.error.*;
import com.saneb.common.response.PageResponse;
import com.saneb.domain.announcementattachment.dao.*;
import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementattachment.dto.*;
import com.saneb.domain.announcementattachment.extraction.AttachmentRuntimeIdentity;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentBatchService;
import com.saneb.domain.announcementattachment.vo.*;
import com.saneb.domain.announcementsource.dao.AnnouncementSourceDao;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceAuditLogCommand;
import com.saneb.domain.auth.vo.AuthenticatedUserDetails;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.ZoneOffset;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** 고정 범위와 명시적 수집 제어. HTTP는 worker에서 실행하고 현재 근거 적용/운영 공고 변경과 분리한다. */
@Service
public class AnnouncementAttachmentBatchServiceImpl implements AnnouncementAttachmentBatchService {
    private final AnnouncementAttachmentBatchDao dao;
    private final AnnouncementAttachmentJobDao jobs;
    private final AnnouncementAttachmentIntakeDao intake;
    private final AttachmentDiscoveryProfileRegistry profiles;
    private final AnnouncementSourceDao audit;
    private final ObjectMapper mapper;
    public AnnouncementAttachmentBatchServiceImpl(AnnouncementAttachmentBatchDao dao,AnnouncementAttachmentJobDao jobs,
            AnnouncementAttachmentIntakeDao intake,AttachmentDiscoveryProfileRegistry profiles,AnnouncementSourceDao audit,ObjectMapper mapper) {
        this.dao=dao;this.jobs=jobs;this.intake=intake;this.profiles=profiles;this.audit=audit;
        this.mapper=mapper.copy().enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
    }
    @Override @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ,timeout=20)
    public AttachmentBatchResponses.Preview selectScopePreview(Authentication actor,AttachmentBatchRequests.Scope scope) {
        selectActor(actor,false);return selectPrepared(normalize(scope),false).preview();
    }
    @Override @Transactional(timeout=30)
    public AttachmentBatchResponses.Batch insertBatch(Authentication authentication,UUID key,AttachmentBatchRequests.Reservation request) {
        return insertPreparedBatch(authentication,key,request,null);
    }
    @Override @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ,timeout=20)
    public AttachmentBatchResponses.Preview selectFixedScopePreview(Authentication actor,AttachmentBatchRequests.Scope scope,AttachmentBatchRows.FixedScope fixed) {
        selectActor(actor,false);return selectPrepared(normalize(scope),false,normalizeFixed(fixed)).preview();
    }
    @Override @Transactional(propagation=Propagation.MANDATORY,timeout=30)
    public AttachmentBatchResponses.Batch insertFixedBatch(Authentication authentication,UUID key,AttachmentBatchRequests.Reservation request,AttachmentBatchRows.FixedScope fixed) {
        return insertPreparedBatch(authentication,key,request,normalizeFixed(fixed));
    }
    private AttachmentBatchResponses.Batch insertPreparedBatch(Authentication authentication,UUID key,AttachmentBatchRequests.Reservation request,AttachmentBatchRows.FixedScope fixed) {
        UUID actor=selectActor(authentication,true);
        if(key==null || request==null || request.expectedScopeHash()==null || !request.expectedScopeHash().matches("[0-9a-f]{64}"))
            throw invalid("UUID 멱등 키와 조회한 배치 범위 지문이 필요합니다.");
        validateReason(request.reason());var scope=normalize(request.scope());
        String requestHash=hash(Arrays.asList("attachment-backfill-scope-v1",actor,scope,request.expectedScopeHash(),request.reason().strip()));
        if(fixed!=null)requestHash=hash(Arrays.asList("attachment-backfill-segment-v1",fixed,requestHash));
        dao.selectRequestLock(key);var existing=dao.selectRequestDetails(key);
        if(existing!=null) {
            if(!actor.equals(existing.requestedBy()) || !requestHash.equals(existing.requestHash())) throw conflict("이 멱등 키는 다른 운영자·범위·사유의 배치에 사용됐습니다.");
            return response(existing);
        }
        var before=selectPrepared(scope,false,fixed);
        validateReady(before,request.expectedScopeHash());
        var ids=before.candidates().stream().map(AttachmentBatchRows.Candidate::sourceId).sorted().toList();
        if(!new HashSet<>(dao.selectSourceLocks(ids)).equals(new HashSet<>(ids))) throw conflict("범위 고정 중 원문이 삭제됐습니다. 범위를 다시 조회하세요.");
        var prepared=selectPrepared(scope,true,fixed);validateReady(prepared,request.expectedScopeHash());
        UUID batchId=UUID.randomUUID();var preview=prepared.preview();
        Map<String,Object> savedScope=new TreeMap<>();savedScope.put("schemaVersion",1);savedScope.put("filter",scope);
        savedScope.put("counts",preview.counts());savedScope.put("candidateCount",preview.candidateCount());savedScope.put("selectedCount",preview.selectedCount());
        savedScope.put("remainingCount",preview.remainingCount());savedScope.put("maximumDownloadBytes",preview.maximumDownloadBytes());
        savedScope.put("maximumHttpRequests",preview.maximumHttpRequests());savedScope.put("policyHash",preview.policyHash());
        if(fixed!=null) {savedScope.put("backfillRunId",fixed.runId());savedScope.put("backfillSegmentNo",fixed.segmentNo());}
        // scope에는 source 식별자/URL을 복사하지 않는다. 삭제 시 FK cascade되는 jobs만 대상 identity를 가진다.
        requireOne(dao.insertBatch(new AttachmentBatchRows.Insert(batchId,scope.policyId(),preview.scopeHash(),scope.maximumCount(),preview.selectedCount(),
                json(savedScope),json(prepared.policy()),actor,key,requestHash,hash(request.reason().strip()))));
        for(var source:prepared.candidates()) {
            UUID jobId=UUID.randomUUID();var execution=prepared.executions().get(source.sourceId());
            long bytes=preview.maximumDownloadBytes()/preview.selectedCount();
            var job=new AttachmentJobInsertCommand(jobId,source.sourceId(),source.contentVersionId(),source.baseEvaluationId(),source.ruleReleaseId(),scope.policyId(),
                    jobs.selectNextGeneration(source.sourceId(),source.contentVersionId(),scope.policyId()),source.sourceVersion(),source.attachmentVersion(),
                    UUID.randomUUID(),hash(Arrays.asList(batchId,source,execution,prepared.locatorHashes().get(source.sourceId()))),json(execution),bytes,null,false,Boolean.TRUE.equals(source.reviewRequired()),
                    source.currentPolicyId(),source.currentEvaluationId(),source.confirmationId(),"COLLECT",null,actor,null);
            requireOne(dao.insertScopeJob(new AttachmentBatchRows.ItemInsert(batchId,job,source.providerCode())));
        }
        insertAudit(actor,batchId,"ATTACHMENT_BATCH_SCOPE_RESERVED",request.reason());
        return response(selectBatch(batchId,false));
    }
    @Override @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ,timeout=10)
    public AttachmentBatchResponses.Batch selectBatchDetails(Authentication actor,UUID batchId) {selectActor(actor,false);return response(selectBatch(batchId,false));}
    @Override @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ,timeout=10)
    public PageResponse<AttachmentBatchResponses.Batch> selectBatchList(Authentication actor,int page,int size) {
        selectActor(actor,false);var search=search(null,page,size);
        return PageResponse.of(dao.selectBatchList(search).stream().map(this::response).toList(),page,size,dao.selectBatchCount());
    }
    @Override @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ,timeout=10)
    public PageResponse<AttachmentBatchRows.Item> selectItemList(Authentication actor,UUID batchId,int page,int size) {
        selectActor(actor,false);var row=selectBatch(batchId,false);var search=search(batchId,page,size);
        return PageResponse.of(dao.selectItemList(search),page,size,row.itemCount()-row.deletedItemCount());
    }
    @Override @Transactional(timeout=30)
    public AttachmentBatchResponses.Batch updateScopeCancellation(Authentication authentication,UUID batchId,AttachmentBatchRequests.Cancellation request) {
        UUID actor=selectActor(authentication,true);
        if(request==null || request.expectedVersion()==null || request.expectedVersion()<0) throw invalid("취소할 배치의 현재 버전이 필요합니다.");
        validateReason(request.reason());var row=selectBatch(batchId,false);
        var ids=dao.selectItemList(new AttachmentBatchRows.Search(batchId,1000,0)).stream().map(AttachmentBatchRows.Item::sourceId).sorted().toList();
        if(!ids.isEmpty()) dao.selectSourceLocks(ids);
        row=selectBatch(batchId,true);
        if(!request.expectedVersion().equals(row.rowVersion()) || !"SCOPE_READY".equals(row.statusCode()))
            throw conflict("수집 전 SCOPE_READY 배치의 현재 버전만 취소할 수 있습니다. 실행 중지·원복은 별도 절차입니다.");
        requireOne(dao.updateScopeCancellation(batchId,request.expectedVersion()));
        if(dao.updateScopeJobsCancelled(batchId)!=row.itemCount()-row.deletedItemCount()) throw conflict("취소 중 항목 상태가 바뀌었습니다. 배치 전체를 다시 확인하세요.");
        insertAudit(actor,batchId,"ATTACHMENT_BATCH_SCOPE_CANCELLED",request.reason());return response(selectBatch(batchId,false));
    }
    @Override @Transactional(timeout=30)
    public AttachmentBatchResponses.Batch updateCollectionStart(Authentication authentication,UUID batchId,AttachmentBatchRequests.Collection request) {
        UUID actor=selectActor(authentication,true);validateCollectionRequest(request);
        var row=selectExecutionLocks(batchId);
        validateCollectionScope(row,request,"SCOPE_READY");
        if(row.deletedItemCount()!=0) throw conflict("예약 후 삭제된 원문이 있습니다. 수집 전 예약을 취소하고 남은 범위를 새로 고정하세요.");
        validateFrozenExecution(row,true);
        requireOne(dao.updateCollectionStart(batchId,row.rowVersion(),actor,hash(Arrays.asList("attachment-batch-collection-v1",actor,batchId,request))));
        if(dao.updateCollectionJobsPending(batchId)!=row.itemCount()) throw conflict("수집 시작 중 예약 항목 상태가 바뀌었습니다. 일부만 시작하지 않고 전체 요청을 취소했습니다.");
        insertAudit(actor,batchId,"ATTACHMENT_BATCH_COLLECTION_STARTED",request.reason());return response(selectBatch(batchId,false));
    }
    @Override @Transactional(timeout=30)
    public AttachmentBatchResponses.Batch updateCollectionPause(Authentication authentication,UUID batchId,AttachmentBatchRequests.Pause request) {
        UUID actor=selectActor(authentication,true);
        if(request==null || request.expectedVersion()==null || request.expectedVersion()<0) throw invalid("중지할 배치의 현재 버전이 필요합니다.");
        validateReason(request.reason());var row=selectBatch(batchId,true);
        if(!request.expectedVersion().equals(row.rowVersion()) || !Set.of("COLLECTION_PENDING","COLLECTING").contains(row.statusCode()))
            throw conflict("수집 대기·진행 중인 배치의 현재 버전만 중지할 수 있습니다. 현재 상태를 다시 조회하세요.");
        requireOne(dao.updateCollectionPause(batchId,row.rowVersion()));
        insertAudit(actor,batchId,"ATTACHMENT_BATCH_COLLECTION_PAUSED",request.reason());return response(selectBatch(batchId,false));
    }
    @Override @Transactional(timeout=30)
    public AttachmentBatchResponses.Batch updateCollectionResume(Authentication authentication,UUID batchId,AttachmentBatchRequests.Collection request) {
        UUID actor=selectActor(authentication,true);validateCollectionRequest(request);
        var row=selectExecutionLocks(batchId);validateCollectionScope(row,request,"COLLECTION_PAUSED");
        validateFrozenExecution(row,false);
        requireOne(dao.updateCollectionResume(batchId,row.rowVersion()));
        insertAudit(actor,batchId,"ATTACHMENT_BATCH_COLLECTION_RESUMED",request.reason());return response(selectBatch(batchId,false));
    }
    @Override @Transactional(timeout=10)
    public int saveCollectionProgress() {return dao.updateCollectionProgress();}
    private AttachmentBatchRows.Row selectExecutionLocks(UUID batchId) {
        var observed=selectBatch(batchId,false);
        var ids=dao.selectItemList(new AttachmentBatchRows.Search(batchId,1000,0)).stream().map(AttachmentBatchRows.Item::sourceId).sorted().toList();
        if(!ids.isEmpty() && !new HashSet<>(dao.selectSourceLocks(ids)).equals(new HashSet<>(ids)))
            throw conflict("수집 제어 중 원문이 삭제됐습니다. 삭제 건수와 현재 버전을 다시 확인하세요.");
        dao.selectPolicyDetails(observed.policyId(),true);
        return selectBatch(batchId,true);
    }
    private void validateCollectionRequest(AttachmentBatchRequests.Collection request) {
        if(request==null || request.expectedVersion()==null || request.expectedVersion()<0 || request.expectedScopeHash()==null
                || !request.expectedScopeHash().matches("[0-9a-f]{64}") || request.expectedItemCount()==null || request.expectedItemCount()<1 || request.expectedItemCount()>1000
                || request.expectedDeletedItemCount()==null || request.expectedDeletedItemCount()<0
                || request.expectedMaximumDownloadBytes()==null || request.expectedMaximumDownloadBytes()<1
                || request.expectedMaximumHttpRequests()==null || request.expectedMaximumHttpRequests()<1)
            throw invalid("현재 버전·64자리 범위 지문·1~1000 고정 대상 수·삭제 건수·고정 최대 bytes/HTTP를 모두 확인하세요.");
        validateReason(request.reason());
    }
    private void validateCollectionScope(AttachmentBatchRows.Row row,AttachmentBatchRequests.Collection request,String requiredState) {
        if(!requiredState.equals(row.statusCode()) || !Objects.equals(row.rowVersion(),request.expectedVersion())
                || !Objects.equals(row.scopeHash(),request.expectedScopeHash()) || !Objects.equals(row.itemCount(),request.expectedItemCount())
                || !Objects.equals(row.deletedItemCount(),request.expectedDeletedItemCount()))
            throw conflict("배치 상태·버전·고정 범위·대상/삭제 건수가 달라졌습니다. 상세를 다시 확인한 후 실행하세요.");
        try {
            var scope=mapper.readTree(row.scopeJson());
            if(!scope.path("maximumDownloadBytes").isIntegralNumber() || !scope.path("maximumHttpRequests").isIntegralNumber()
                    || scope.path("maximumDownloadBytes").asLong()!=request.expectedMaximumDownloadBytes()
                    || scope.path("maximumHttpRequests").asLong()!=request.expectedMaximumHttpRequests())
                throw conflict("확인한 최대 bytes/HTTP가 고정 범위 상한과 다릅니다. 임의로 상한을 늘리거나 줄일 수 없습니다.");
        } catch(ApiException exception) {throw exception;}catch(Exception exception) {throw conflict("배치 실행 상한을 읽을 수 없습니다.");}
    }
    private void validateFrozenExecution(AttachmentBatchRows.Row row,boolean starting) {
        try {
            var policy=dao.selectPolicyDetails(row.policyId(),true);
            if(policy==null || !"ACTIVE".equals(policy.policyStatusCode()) || !"ACTIVE".equals(policy.releaseStatusCode())
                    || !Set.of("COLLECT_ONLY","ENFORCE").contains(policy.modeCode())
                    || !policy.equals(mapper.readValue(row.policySnapshotJson(),AttachmentPolicyRow.class)))
                throw conflict("예약한 게시 정책·규칙이 변경 또는 퇴역됐습니다. 기존 배치를 새 정책으로 자동 실행하지 않습니다.");
            var configuration=selectConfiguration(policy);var fixed=dao.selectExecutionItemList(row.batchId());
            if(fixed.size()+row.deletedItemCount()!=row.itemCount()) throw conflict("고정 대상과 현재 작업·삭제 건수 합계가 다릅니다.");
            var current=new HashMap<UUID,AttachmentBatchRows.Candidate>();
            for(var value:dao.selectFixedCandidateList(row.batchId())) {
                if(current.put(value.sourceId(),value)!=null) throw conflict("고정 원문의 현재 판정이 중복됐습니다.");
            }
            for(var item:fixed) {
                if(!starting && Set.of("SUCCEEDED","PARTIAL_FAILED","FAILED","CONFLICT","CANCELLED").contains(item.statusCode())) continue;
                if(starting?!"SCOPE_READY".equals(item.statusCode()):!Set.of("PENDING","RUNNING","RETRY_WAIT").contains(item.statusCode()))
                    throw conflict("고정 작업의 수집 상태가 변경됐습니다. 실패 항목은 재개로 다시 생성하지 않습니다.");
                var source=current.get(item.sourceId());
                if(source==null || source.activeJobId()!=null || !policy.ruleReleaseId().equals(source.ruleReleaseId())
                        || !item.providerCode().equals(source.providerCode()) || !Boolean.TRUE.equals(item.locatorUnchanged())
                        || !Objects.equals(item.downloadBudgetBytes(),configuration.maximumSourceBytes()))
                    throw conflict("고정 대상의 기본 판정·출처·연결 공고·다운로드 예산이 달라졌습니다. 현재 항목을 확인하세요.");
                var execution=selectExecution(source,policy,configuration);var locator=intake.selectSourceLocatorDetails(source.sourceId());
                if(execution==null || locator==null || !execution.equals(mapper.readValue(item.executionSnapshotJson(),AttachmentExecutionSnapshot.class))
                        || !item.requestHash().equals(hash(Arrays.asList(row.batchId(),source,execution,hash(locator)))))
                    throw conflict("예약 이후 원문·첨부 버전·기존 검수·profile 또는 출처 연결이 바뀌었습니다. 변경된 입력으로 수집하지 않습니다.");
            }
        } catch(ApiException exception) {throw exception;}catch(Exception exception) {throw conflict("고정 수집 실행 지문을 검증할 수 없습니다.");}
    }
    private Prepared selectPrepared(AttachmentBatchRequests.Scope scope,boolean lockPolicy) {
        return selectPrepared(scope,lockPolicy,null);
    }
    private Prepared selectPrepared(AttachmentBatchRequests.Scope scope,boolean lockPolicy,AttachmentBatchRows.FixedScope fixed) {
        var policy=dao.selectPolicyDetails(scope.policyId(),lockPolicy);
        if(policy==null) throw notFound();
        if(!"ACTIVE".equals(policy.policyStatusCode()) || !"ACTIVE".equals(policy.releaseStatusCode())
                || !Set.of("COLLECT_ONLY","ENFORCE").contains(policy.modeCode()) || policy.policyHash()==null || !policy.policyHash().matches("[0-9a-f]{64}"))
            throw new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_POLICY_NOT_ACTIVE,HttpStatus.CONFLICT,"배치에는 현재 ACTIVE 규칙의 게시된 COLLECT_ONLY 또는 ENFORCE 정책이 필요합니다.");
        var configuration=selectConfiguration(policy);
        List<AttachmentBatchRows.Bucket> buckets;List<AttachmentBatchRows.Candidate> candidates;
        if(fixed==null) {
            buckets=dao.selectScopeCounts(scope).stream().map(AttachmentProviderScope::selectScopeBucket).toList();
            candidates=dao.selectCandidateList(scope);
        }
        else {
            if(fixed.sourceIds().size()>scope.maximumCount())throw invalid("고정 분할의 잔여 대상 수가 분할 크기를 초과했습니다.");
            candidates=dao.selectFixedSourceCandidateList(fixed.sourceIds());
            if(candidates.size()!=fixed.sourceIds().size() || !new HashSet<>(candidates.stream().map(AttachmentBatchRows.Candidate::sourceId).toList()).equals(new HashSet<>(fixed.sourceIds()))
                    || candidates.stream().anyMatch(c->!scope.providerCodes().contains(AttachmentProviderScope.selectScopeCode(c.providerCode()))))
                throw conflict("고정 분할의 원문·기본 판정·보호 연결이 달라졌습니다. 다른 후보로 채우지 않고 예약을 취소했습니다.");
            var counts=new TreeMap<String,Long>();candidates.forEach(c->counts.merge(AttachmentProviderScope.selectScopeCode(c.providerCode()),1L,Long::sum));
            buckets=counts.entrySet().stream().map(e->new AttachmentBatchRows.Bucket(e.getKey(),"CANDIDATE",e.getValue())).toList();
        }
        long total=buckets.stream().filter(b->"CANDIDATE".equals(b.reasonCode())).mapToLong(AttachmentBatchRows.Bucket::count).sum();
        if(candidates.size()!=Math.min(scope.maximumCount(),total) || candidates.stream().map(AttachmentBatchRows.Candidate::sourceId).distinct().count()!=candidates.size())
            throw conflict("범위 집계와 대상 목록이 다릅니다. 현재 범위를 다시 조회하세요.");
        var executions=new TreeMap<UUID,AttachmentExecutionSnapshot>();var locatorHashes=new TreeMap<UUID,String>();var responses=new ArrayList<AttachmentBatchResponses.Candidate>();
        for(var source:candidates) {
            String readiness="READY";AttachmentExecutionSnapshot execution=null;
            if(!policy.ruleReleaseId().equals(source.ruleReleaseId())) readiness="BASE_RECLASSIFICATION_REQUIRED";
            else if(source.activeJobId()!=null) readiness="ACTIVE_JOB";
            else if(source.sourceVersion()==null || source.attachmentVersion()==null || source.sourceVersion()<0 || source.attachmentVersion()<0
                    || source.sourceVersion()==Integer.MAX_VALUE || source.attachmentVersion()==Integer.MAX_VALUE) readiness="VERSION_LIMIT";
            else {execution=selectExecution(source,policy,configuration);if(execution==null) readiness="PROFILE_REQUIRED";}
            if(execution!=null) {
                var locator=intake.selectSourceLocatorDetails(source.sourceId());
                if(locator==null) throw conflict("출처 연결이 조회 중 바뀌었습니다. 범위를 다시 확인하세요.");
                locatorHashes.put(source.sourceId(),hash(locator));executions.put(source.sourceId(),execution);
            }
            responses.add(new AttachmentBatchResponses.Candidate(source.sourceId(),source.providerCode(),source.baseEvaluationId(),source.sourceVersion(),source.attachmentVersion(),readiness,
                    execution==null?null:hash(Arrays.asList(execution,locatorHashes.get(source.sourceId())))));
        }
        String scopeHash=hash(Arrays.asList("attachment-backfill-scope-v1",scope,policy,buckets,candidates,responses));
        if(fixed!=null)scopeHash=hash(Arrays.asList("attachment-backfill-segment-v1",fixed,scopeHash));
        var preview=new AttachmentBatchResponses.Preview(scope,scopeHash,policy.ruleReleaseId(),policy.policyHash(),List.copyOf(buckets),total,candidates.size(),total-candidates.size(),
                configuration.maximumSourceBytes()*candidates.size(),132L*candidates.size(),0,!candidates.isEmpty() && responses.stream().allMatch(r->"READY".equals(r.readinessCode())),List.copyOf(responses));
        return new Prepared(preview,policy,candidates,executions,locatorHashes);
    }
    private AttachmentPolicyResponses.Configuration selectConfiguration(AttachmentPolicyRow policy) {
        try {
            if(policy.settingsJson()==null || policy.settingsJson().length()>8192 || policy.profileManifestJson()==null || policy.profileManifestJson().length()>256000) throw new IllegalArgumentException();
            var value=mapper.readValue(policy.settingsJson(),AttachmentPolicyResponses.Configuration.class);
            if(!value.selectEngineCurrent() || !AttachmentRuntimeIdentity.EXTRACTOR_VERSION.equals(value.extractorVersion())
                    || value.extractorConfigHash()==null || !value.extractorConfigHash().matches("[0-9a-f]{64}") || value.maximumSourceBytes()==null
                    || value.maximumSourceBytes()<1 || value.maximumSourceBytes()>83886080) throw new IllegalArgumentException();
            return value;
        } catch(Exception exception) {throw conflict("게시 정책의 엔진·추출기 설치 지문 또는 1~80 MiB 다운로드 상한이 유효하지 않습니다.");}
    }
    private AttachmentExecutionSnapshot selectExecution(AttachmentBatchRows.Candidate source,AttachmentPolicyRow policy,AttachmentPolicyResponses.Configuration configuration) {
        var locator=intake.selectSourceLocatorDetails(source.sourceId());if(locator==null || !source.providerCode().equals(locator.providerCode())) return null;
        try {
            var manifest=mapper.readTree(policy.profileManifestJson());if(!manifest.isArray() || manifest.size()>1000) return null;
            var matched=new ArrayList<AttachmentDiscoveryProfile>();
            for(var binding:manifest) {
                if(!source.providerCode().equals(binding.path("providerCode").asText())) continue;
                var profile=profiles.selectProfileDetails(source.providerCode(),binding.path("profileCode").asText(),binding.path("profileHash").asText());
                if(profile.isPresent()) try {profile.get().selectDetailUri(locator.selectDiscoverySource());matched.add(profile.get());} catch(IllegalArgumentException ignored) { }
            }
            if(matched.size()!=1)return null;var profile=matched.getFirst();
            var execution=new AttachmentExecutionSnapshot(profile.selectProfileCode(),profile.selectProfileHash(),configuration.engineVersion(),
                    configuration.extractorVersion(),configuration.extractorConfigHash(),configuration.roleRuleVersion(),configuration.roleRulesHash(),
                    configuration.segmentRuleVersion(),configuration.segmentRulesHash());
            return execution.selectRoleRulesCurrent()?execution:null;
        } catch(Exception exception) {return null;}
    }
    private AttachmentBatchRequests.Scope normalize(AttachmentBatchRequests.Scope value) {
        if(value==null || value.policyId()==null || value.providerCodes()==null || value.providerCodes().isEmpty() || value.providerCodes().size()>3
                || value.providerCodes().stream().anyMatch(p->p==null || !Set.of("BIZINFO","GOV24","LOCAL_GOV_NOTICE").contains(p))
                || value.providerCodes().stream().distinct().count()!=value.providerCodes().size()) throw invalid("정책과 중복 없는 지원 출처 1~3개를 지정하세요.");
        if(value.collectedFrom()==null || value.collectedBefore()==null || !value.collectedFrom().isBefore(value.collectedBefore())
                || (value.deadlineFrom()!=null && value.deadlineThrough()!=null && value.deadlineFrom().isAfter(value.deadlineThrough()))
                || value.maximumCount()==null || value.maximumCount()<1 || value.maximumCount()>1000) throw invalid("수집 시작은 종료보다 이전이어야 하며 마감일 범위는 역전될 수 없습니다. 최대 건수는 1~1000입니다.");
        return new AttachmentBatchRequests.Scope(value.policyId(),value.providerCodes().stream().sorted().toList(),value.collectedFrom().withOffsetSameInstant(ZoneOffset.UTC),
                value.collectedBefore().withOffsetSameInstant(ZoneOffset.UTC),value.deadlineFrom(),value.deadlineThrough(),value.maximumCount());
    }
    private AttachmentBatchRows.FixedScope normalizeFixed(AttachmentBatchRows.FixedScope value) {
        if(value==null || value.runId()==null || value.segmentNo()<1 || value.sourceIds()==null || value.sourceIds().isEmpty() || value.sourceIds().size()>1000
                || value.sourceIds().stream().anyMatch(Objects::isNull) || value.sourceIds().stream().distinct().count()!=value.sourceIds().size())
            throw invalid("내부 고정 분할에는 전체 목록 ID·양수 분할 번호와 중복 없는 잔여 대상 1~1000건이 필요합니다.");
        return new AttachmentBatchRows.FixedScope(value.runId(),value.segmentNo(),value.sourceIds().stream().sorted().toList());
    }
    private void validateReady(Prepared prepared,String hash) {
        if(!prepared.preview().scopeHash().equals(hash)) throw conflict("미리보기 이후 대상·건수·버전·정책·시스템 profile이 바뀌었습니다. 새 범위와 요청 상한을 확인하세요.");
        if(!prepared.preview().canReserve()) throw conflict("선택된 범위에 대상이 없거나 진행 중 작업·기본 규칙 불일치·미지원 출처가 있습니다. 항목별 준비 사유를 해결한 뒤 다시 조회하세요.");
    }
    private AttachmentBatchRows.Row selectBatch(UUID id,boolean lock) {var row=id==null?null:dao.selectBatchDetails(id,lock);if(row==null)throw notFound();return row;}
    private AttachmentBatchResponses.Batch response(AttachmentBatchRows.Row row) {
        var counts=new TreeMap<String,Long>();dao.selectJobCounts(row.batchId()).forEach(c->counts.merge(c.reasonCode(),c.count(),Long::sum));
        long remaining=counts.values().stream().mapToLong(Long::longValue).sum();
        if(row.itemCount()==null || remaining+row.deletedItemCount()!=row.itemCount()) throw conflict("고정 대상 수와 현재 항목·삭제 건수 합계가 다릅니다. 배치 무결성을 확인하세요.");
        try {
            Map<String,Object> scope=mapper.readValue(row.scopeJson(),mapper.getTypeFactory().constructMapType(Map.class,String.class,Object.class));
            return new AttachmentBatchResponses.Batch(row.batchId(),row.policyId(),row.statusCode(),row.scopeHash(),row.rowVersion(),row.itemCount(),(int)remaining,row.deletedItemCount(),counts,scope,row.createdAt());
        } catch(ApiException exception){throw exception;}catch(Exception exception){throw conflict("고정 배치 범위 이력을 읽을 수 없습니다.");}
    }
    private AttachmentBatchRows.Search search(UUID id,int page,int size) {if(page<1 || size<1 || size>100 || ((long)page-1)*size>Integer.MAX_VALUE)throw invalid("페이지는 1 이상, 크기는 1~100이어야 합니다.");return new AttachmentBatchRows.Search(id,size,(page-1)*size);}
    private UUID selectActor(Authentication authentication,boolean change) {
        if(authentication==null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof AuthenticatedUserDetails actor))
            throw new ApiException(ErrorCode.AUTH_REQUIRED,HttpStatus.UNAUTHORIZED,"로그인한 운영 계정이 필요합니다.");
        if(!actor.isEnabled() || actor.passwordResetRequired() || actor.roles().stream().noneMatch((change?Set.of("ADMIN"):Set.of("ADMIN","OPERATOR","APPROVER"))::contains))
            throw new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_ACTION_FORBIDDEN,HttpStatus.FORBIDDEN,change?"배치 예약·취소·수집 제어는 활성 ADMIN만 가능합니다.":"배치 범위는 활성 ADMIN·OPERATOR·APPROVER만 조회할 수 있습니다.");
        return actor.userId();
    }
    private void validateReason(String reason) {if(reason==null || reason.isBlank() || reason.length()>1000)throw invalid("배치 사유를 공백이 아닌 1~1000자로 입력하세요.");}
    private void requireOne(int count) {if(count!=1)throw conflict("배치 저장 중 상태나 버전이 변경됐습니다. 일부만 반영하지 않고 전체 요청을 취소했습니다. 현재 상태를 다시 조회하세요.");}
    private void insertAudit(UUID actor,UUID batchId,String action,String reason) {audit.insertAuditLog(new AnnouncementSourceAuditLogCommand(actor,action,"ANNOUNCEMENT_ATTACHMENT_BATCH",batchId,"SUCCESS",json(Map.of("reasonHash",hash(reason.strip())))));}
    private String json(Object value) {try{return mapper.writeValueAsString(value);}catch(Exception exception){throw conflict("배치 범위 지문을 직렬화할 수 없습니다.");}}
    private String hash(Object value) {try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json(value).getBytes(StandardCharsets.UTF_8)));}catch(Exception exception){throw conflict("배치 범위 지문을 계산할 수 없습니다.");}}
    private record Prepared(AttachmentBatchResponses.Preview preview,AttachmentPolicyRow policy,List<AttachmentBatchRows.Candidate> candidates,Map<UUID,AttachmentExecutionSnapshot> executions,Map<UUID,String> locatorHashes) { }
    private ApiException invalid(String message){return new ApiException(ErrorCode.VALIDATION_FAILED,HttpStatus.BAD_REQUEST,message);}
    private ApiException conflict(String message){return new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_VERSION_CONFLICT,HttpStatus.CONFLICT,message);}
    private ApiException notFound(){return new ApiException(ErrorCode.RESOURCE_NOT_FOUND,HttpStatus.NOT_FOUND,"해당 관리 배치 또는 정책을 찾을 수 없습니다.");}
}
