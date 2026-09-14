package com.saneb.domain.announcementattachment.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.common.error.ApiException;
import com.saneb.common.error.ErrorCode;
import com.saneb.domain.announcementattachment.classification.AnnouncementAttachmentClassificationEngine;
import com.saneb.domain.announcementattachment.dao.*;
import com.saneb.domain.announcementattachment.discovery.AttachmentDiscoveryProfile;
import com.saneb.domain.announcementattachment.discovery.AttachmentDiscoveryProfileRegistry;
import com.saneb.domain.announcementattachment.dto.*;
import com.saneb.domain.announcementattachment.extraction.AttachmentRuntimeIdentity;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentCollectionService;
import com.saneb.domain.announcementattachment.vo.*;
import com.saneb.domain.announcementsource.dao.AnnouncementSourceDao;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceAuditLogCommand;
import com.saneb.domain.auth.vo.AuthenticatedUserDetails;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/** 단건 전체 수집은 새 근거를 예약할 뿐, 정책 게시·검수 binding 신규 적용·공고 활성화를 하지 않는다. */
@Service
public class AnnouncementAttachmentCollectionServiceImpl implements AnnouncementAttachmentCollectionService {
    private final AnnouncementAttachmentJobDao jobs;
    private final AnnouncementAttachmentIntakeDao intake;
    private final AnnouncementAttachmentCollectionDao collections;
    private final AnnouncementAttachmentRetryDao retries;
    private final AnnouncementSourceDao sources;
    private final AttachmentDiscoveryProfileRegistry profiles;
    private final ObjectMapper mapper;
    private final boolean enabled;
    public AnnouncementAttachmentCollectionServiceImpl(AnnouncementAttachmentJobDao jobs,AnnouncementAttachmentIntakeDao intake,
            AnnouncementAttachmentCollectionDao collections,AnnouncementAttachmentRetryDao retries,AnnouncementSourceDao sources,
            AttachmentDiscoveryProfileRegistry profiles,ObjectMapper mapper,@Value("${saneb.announcement-attachment.worker.enabled:false}") boolean enabled) {
        this.jobs=jobs;this.intake=intake;this.collections=collections;this.retries=retries;this.sources=sources;this.profiles=profiles;this.mapper=mapper;this.enabled=enabled;
    }
    @Override @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ,timeout=10)
    public AttachmentCollectionContext selectCollectionContextDetails(Authentication authentication,UUID sourceId) {
        selectActor(authentication,false);var source=selectSource(sourceId,false);var prepared=selectPrepared(source,false);
        return new AttachmentCollectionContext(selectVersion(source),prepared.policy().policyId(),prepared.policy().policyHash(),prepared.executionHash(),
                prepared.policy().modeCode(),prepared.maximumBytes(),10,3,132,Boolean.TRUE.equals(source.attachmentReviewRequired()),
                Boolean.TRUE.equals(source.attachmentReviewRequired())?"PRESERVE_ENFORCE_AND_STALE_CONFIRMATION":"COLLECT_PREVIEW_ONLY");
    }
    @Override @Transactional(timeout=20)
    public AttachmentJobResponse insertCollectionJob(Authentication authentication,UUID sourceId,UUID key,AttachmentCollectionRequests.Request request) {
        UUID actor=selectActor(authentication,true);validateRequest(sourceId,key,request);
        String requestHash=selectHash(selectJson(List.of("attachment-manual-collect-v1",sourceId,actor,request.version(),request.expectedPolicyId(),
                request.expectedPolicyHash(),request.expectedExecutionHash(),request.maximumDownloadBytes(),request.reason().strip())));
        var source=selectSource(sourceId,true);
        var existing=jobs.selectIdempotentJobDetails(key);
        if(existing!=null) return selectIdempotent(existing,sourceId,actor,requestHash);
        if(!selectVersion(source).equals(request.version())) throw conflict("수집 조건 조회 이후 기본·첨부 판정 또는 버전이 변경됐습니다. 입력을 보존하고 최신 수집 조건을 확인하세요.");
        var prepared=selectPrepared(source,true);
        if(!prepared.policy().policyId().equals(request.expectedPolicyId()) || !prepared.policy().policyHash().equals(request.expectedPolicyHash())
                || !prepared.executionHash().equals(request.expectedExecutionHash()))
            throw conflict("조회한 정책 또는 시스템 수집 실행 버전이 변경됐습니다. 최신 조건과 요청량을 다시 확인하세요.");
        if(request.maximumDownloadBytes()>prepared.maximumBytes()) throw invalid("요청 다운로드 상한이 게시 정책의 공고별 상한을 초과합니다.");
        UUID jobId=UUID.randomUUID();boolean required=Boolean.TRUE.equals(source.attachmentReviewRequired());
        int inserted=jobs.insertAttachmentJob(new AttachmentJobInsertCommand(jobId,sourceId,source.contentVersionId(),source.baseEvaluationId(),source.ruleReleaseId(),
                prepared.policy().policyId(),jobs.selectNextGeneration(sourceId,source.contentVersionId(),prepared.policy().policyId()),source.sourceVersion(),source.attachmentVersion()+1,
                key,requestHash,selectJson(prepared.execution()),request.maximumDownloadBytes(),null,required,required,source.attachmentPolicyId(),
                source.currentAttachmentEvaluationId(),jobs.selectCurrentConfirmationId(sourceId),"COLLECT",null,actor,null));
        if(inserted!=1) {
            existing=jobs.selectIdempotentJobDetails(key);
            if(existing!=null) return selectIdempotent(existing,sourceId,actor,requestHash);
            throw conflict("수집 예약 중 다른 작업이 먼저 생성됐습니다. 기존 작업을 확인하세요.");
        }
        if(jobs.updateAttachmentSourceVersion(sourceId,source.attachmentVersion())!=1) throw conflict("수집 예약 중 첨부 버전이 변경됐습니다. 최신 조건을 확인하세요.");
        jobs.updateAttachmentEvaluationsStale(sourceId);jobs.updateAttachmentConfirmationsStale(sourceId);
        if(intake.updateSourceIntakeStatus(sourceId,"QUEUED")!=1) throw conflict("수집 예약 상태를 기록하지 못했습니다. 최신 원문 상태를 확인하세요.");
        sources.insertAuditLog(new AnnouncementSourceAuditLogCommand(actor,"ATTACHMENT_FULL_COLLECTION","ANNOUNCEMENT_SOURCE",sourceId,"SUCCESS",
                selectJson(Map.of("jobId",jobId,"policyId",prepared.policy().policyId(),"maximumDownloadBytes",request.maximumDownloadBytes(),
                        "maximumHttpRequests",132,"maximumFileCount",10,"reasonHash",selectHash(request.reason().strip())))));
        return AttachmentJobResponse.from(Objects.requireNonNull(jobs.selectJobDetails(jobId)));
    }
    private Prepared selectPrepared(AttachmentSourceContextRow source,boolean lock) {
        if(!enabled) throw conflict("첨부 작업자가 비활성 상태입니다. 운영 설정을 확인한 뒤 수집을 예약하세요.");
        if(intake.selectProtectedLinkExists(source.sourceId())) throw conflict("이미 운영 공고에 연결된 원문은 전체 첨부 재수집으로 덮어쓸 수 없습니다.");
        if(intake.selectActiveJobId(source.sourceId())!=null) throw conflict("이 원문에는 진행 중인 첨부 작업이 있습니다. 기존 작업의 완료·실패 상태를 먼저 확인하세요.");
        var policy=collections.selectActivePolicyDetails(source.ruleReleaseId(),lock);
        if(policy==null || !"ACTIVE".equals(policy.policyStatusCode()) || !"ACTIVE".equals(policy.releaseStatusCode())
                || policy.modeCode()==null || !Set.of("COLLECT_ONLY","ENFORCE").contains(policy.modeCode()) || !source.ruleReleaseId().equals(policy.ruleReleaseId())
                || policy.policyHash()==null || !policy.policyHash().matches("[0-9a-f]{64}") || !retries.selectRetryControlAllowed())
            throw new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_POLICY_NOT_ACTIVE,HttpStatus.CONFLICT,
                    "현재 기본 판정과 같은 ACTIVE 규칙의 게시 수집 정책이 필요합니다. OFF·퇴역·버전 불일치는 수집하지 않습니다.");
        if(Boolean.TRUE.equals(source.attachmentReviewRequired()) && !policy.policyId().equals(source.attachmentPolicyId()))
            throw conflict("기존 첨부 검수 정책과 현재 게시 정책이 다릅니다. 승인된 정책 변경 절차로 검수 연결을 먼저 확인하세요.");
        var locator=intake.selectSourceLocatorDetails(source.sourceId());
        if(locator==null || !source.providerCode().equals(locator.providerCode())) throw profileRequired();
        try {
            var manifest=mapper.readTree(policy.profileManifestJson());var settings=mapper.readTree(policy.settingsJson());
            if(manifest==null || !manifest.isArray() || manifest.size()>1000 || settings==null || !settings.isObject()) throw profileRequired();
            var selected=new ArrayList<AttachmentDiscoveryProfile>();
            for(var item:manifest) {
                if(!source.providerCode().equals(item.path("providerCode").asText())) continue;
                var profile=profiles.selectProfileDetails(source.providerCode(),item.path("profileCode").asText(),item.path("profileHash").asText());
                if(profile.isEmpty()) continue;
                try { profile.get().selectDetailUri(locator.selectDiscoverySource());selected.add(profile.get()); }
                catch(IllegalArgumentException ignored) { /* 시스템 profile의 정확한 source 계약에 맞지 않는다. */ }
            }
            if(selected.size()!=1 || !AnnouncementAttachmentClassificationEngine.VERSION.equals(settings.path("engineVersion").asText())
                    || !AttachmentRuntimeIdentity.EXTRACTOR_VERSION.equals(settings.path("extractorVersion").asText())
                    || !settings.path("maximumSourceBytes").isIntegralNumber() || !settings.path("maximumSourceBytes").canConvertToLong()) throw profileRequired();
            long bytes=settings.path("maximumSourceBytes").asLong();if(bytes<1 || bytes>83886080) throw profileRequired();
            var execution=new AttachmentExecutionSnapshot(selected.getFirst().selectProfileCode(),selected.getFirst().selectProfileHash(),
                    settings.path("engineVersion").asText(),settings.path("extractorVersion").asText(),settings.path("extractorConfigHash").asText());
            if(!collections.selectManualRequestRateAllowed(source.sourceId())) throw new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_COLLECTION_RATE_LIMITED,HttpStatus.TOO_MANY_REQUESTS,
                    "전체 첨부 수집과 실패 파일 재시도는 합산하여 원문별 60초 간격, 최근 24시간 최대 3회입니다. 기존 작업 결과를 확인하세요.");
            return new Prepared(policy,execution,selectHash(selectJson(execution)),bytes);
        } catch(JsonProcessingException|IllegalArgumentException exception) { throw profileRequired(); }
    }
    private AttachmentSourceContextRow selectSource(UUID sourceId,boolean lock) {
        if(sourceId==null) throw notFound();
        var source=lock?jobs.selectSourceContextDetailsForUpdate(sourceId):jobs.selectSourceContextDetails(sourceId);
        if(source==null || !sourceId.equals(source.sourceId()) || !"PRODUCTION".equals(source.dataPurposeCode()) || "EXCLUDED".equals(source.semanticStatusCode())
                || source.baseEvaluationId()==null || source.contentVersionId()==null || source.ruleReleaseId()==null || source.titleStageCode()==null
                || !Set.of("GROUP_A_MATCHED","COMBINATION_MATCHED").contains(source.titleStageCode())) throw notFound();
        if(source.attachmentVersion()==Integer.MAX_VALUE) throw conflict("첨부 버전이 상한에 도달하여 추가 수집을 예약할 수 없습니다.");
        return source;
    }
    private AttachmentCollectionRequests.Version selectVersion(AttachmentSourceContextRow source) {
        return new AttachmentCollectionRequests.Version(source.baseEvaluationId(),source.currentAttachmentEvaluationId(),source.sourceVersion(),source.attachmentVersion());
    }
    private AttachmentJobResponse selectIdempotent(AttachmentJobRow job,UUID sourceId,UUID actor,String hash) {
        if(!sourceId.equals(job.sourceId()) || !actor.equals(job.requestedBy()) || !"COLLECT".equals(job.operationCode()) || !hash.equals(job.requestHash()))
            throw conflict("이 멱등 키는 다른 원문·운영자·수집 요청에 이미 사용됐습니다.");
        return AttachmentJobResponse.from(job);
    }
    private UUID selectActor(Authentication authentication,boolean write) {
        if(authentication==null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof AuthenticatedUserDetails details))
            throw new ApiException(ErrorCode.AUTH_REQUIRED,HttpStatus.UNAUTHORIZED,"로그인한 운영 계정이 필요합니다.");
        var roles=write?Set.of("ADMIN","OPERATOR"):Set.of("ADMIN","OPERATOR","APPROVER");
        if(!details.isEnabled() || details.passwordResetRequired() || details.roles().stream().noneMatch(roles::contains))
            throw new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_ACTION_FORBIDDEN,HttpStatus.FORBIDDEN,
                    write?"전체 첨부 수집은 비밀번호 변경을 완료한 활성 ADMIN 또는 OPERATOR만 예약할 수 있습니다.":"수집 조건은 활성 운영 조회 역할로 비밀번호 변경을 완료한 뒤 확인할 수 있습니다.");
        return details.userId();
    }
    private void validateRequest(UUID sourceId,UUID key,AttachmentCollectionRequests.Request request) {
        if(sourceId==null || key==null || request==null || request.version()==null || request.expectedPolicyId()==null
                || request.expectedPolicyHash()==null || !request.expectedPolicyHash().matches("[0-9a-f]{64}")
                || request.expectedExecutionHash()==null || !request.expectedExecutionHash().matches("[0-9a-f]{64}")
                || request.maximumDownloadBytes()==null || request.maximumDownloadBytes()<1 || request.maximumDownloadBytes()>83886080
                || request.reason()==null || request.reason().isBlank() || request.reason().length()>1000) throw invalid("현재 버전·정책/실행 hash, 1바이트 이상 80 MiB 이하의 다운로드 상한, 1~1000자 수집 사유를 입력하세요.");
        var v=request.version();
        if(v.expectedBaseDecisionId()==null || v.expectedSourceVersion()==null || v.expectedSourceVersion()<0 || v.expectedAttachmentVersion()==null
                || v.expectedAttachmentVersion()<0 || v.expectedAttachmentVersion()==Integer.MAX_VALUE) throw invalid("현재 기본 판정과 유효한 원문·첨부 버전이 필요합니다.");
    }
    private record Prepared(AttachmentPolicyRow policy,AttachmentExecutionSnapshot execution,String executionHash,long maximumBytes) { }
    private String selectJson(Object value) { try { return mapper.writeValueAsString(value); } catch(JsonProcessingException exception) { throw invalid("수집 조건을 직렬화할 수 없습니다."); } }
    private String selectHash(String value) { try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch(NoSuchAlgorithmException exception) { throw new IllegalStateException("SHA-256을 사용할 수 없습니다."); } }
    private ApiException profileRequired() { return new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_PROFILE_REQUIRED,HttpStatus.CONFLICT,
            "이 출처에 정확히 연결된 게시 시스템 profile과 현재 엔진·추출기 설정을 확인할 수 없습니다. 관리자 임의 URL·파서 선택으로 수집할 수 없습니다."); }
    private ApiException invalid(String message) { return new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_COLLECTION_INVALID,HttpStatus.BAD_REQUEST,message); }
    private ApiException conflict(String message) { return new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_VERSION_CONFLICT,HttpStatus.CONFLICT,message); }
    private ApiException notFound() { return new ApiException(ErrorCode.RESOURCE_NOT_FOUND,HttpStatus.NOT_FOUND,"제목 수집 기준을 통과한 운영 원문을 선택하세요. 제목 제외·QA 원문은 수집 대상이 아닙니다."); }
}
