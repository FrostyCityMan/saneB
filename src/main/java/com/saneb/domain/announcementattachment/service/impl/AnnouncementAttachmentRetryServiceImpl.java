package com.saneb.domain.announcementattachment.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.common.error.ApiException;
import com.saneb.common.error.ErrorCode;
import com.saneb.domain.announcementattachment.classification.AnnouncementAttachmentClassificationEngine;
import com.saneb.domain.announcementattachment.dao.*;
import com.saneb.domain.announcementattachment.discovery.AttachmentDiscoveryProfileRegistry;
import com.saneb.domain.announcementattachment.dto.AttachmentJobResponse;
import com.saneb.domain.announcementattachment.dto.AttachmentRetryRequest;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentRetryService;
import com.saneb.domain.announcementattachment.vo.*;
import com.saneb.domain.announcementsource.dao.AnnouncementSourceDao;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceAuditLogCommand;
import com.saneb.domain.auth.vo.AuthenticatedUserDetails;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 이미 게시된 정책의 단건 실패 파일만 고정한다. 예약 transaction에서 네트워크/추출은 실행하지 않는다. */
@Service
public class AnnouncementAttachmentRetryServiceImpl implements AnnouncementAttachmentRetryService {
    private final AnnouncementAttachmentJobDao jobs;
    private final AnnouncementAttachmentCurrentDao current;
    private final AnnouncementAttachmentEvidenceDao evidence;
    private final AnnouncementAttachmentReviewDao reviews;
    private final AnnouncementAttachmentRoleDao roles;
    private final AnnouncementAttachmentRetryDao retries;
    private final AnnouncementSourceDao sources;
    private final AttachmentDiscoveryProfileRegistry profiles;
    private final ObjectMapper mapper;
    public AnnouncementAttachmentRetryServiceImpl(AnnouncementAttachmentJobDao jobs,AnnouncementAttachmentCurrentDao current,
            AnnouncementAttachmentEvidenceDao evidence,AnnouncementAttachmentReviewDao reviews,AnnouncementAttachmentRoleDao roles,
            AnnouncementAttachmentRetryDao retries,AnnouncementSourceDao sources,AttachmentDiscoveryProfileRegistry profiles,ObjectMapper mapper) {
        this.jobs=jobs;this.current=current;this.evidence=evidence;this.reviews=reviews;this.roles=roles;
        this.retries=retries;this.sources=sources;this.profiles=profiles;this.mapper=mapper;
    }
    @Override @Transactional(timeout=20)
    public AttachmentJobResponse insertFileRetry(Authentication authentication,UUID sourceId,UUID key,AttachmentRetryRequest request) {
        UUID actor=selectActor(authentication); validateRequest(sourceId,key,request);
        var selected=request.fileIds().stream().map(UUID::toString).sorted().toList();
        if(new HashSet<>(selected).size()!=selected.size()) throw invalid("같은 실패 파일을 중복 선택할 수 없습니다.");
        String hash=selectHash(selectJson(List.of("attachment-file-retry-v1",sourceId,actor,request.version(),request.expectedSetId(),selected,
                request.maximumDownloadBytes(),request.reason().strip())));
        var source=jobs.selectSourceContextDetailsForUpdate(sourceId);
        if(source==null || !"PRODUCTION".equals(source.dataPurposeCode()) || "EXCLUDED".equals(source.semanticStatusCode())
                || source.titleStageCode()==null || !Set.of("GROUP_A_MATCHED","COMBINATION_MATCHED").contains(source.titleStageCode())) throw notFound();
        var existing=jobs.selectIdempotentJobDetails(key);
        if(existing!=null) {
            if(!sourceId.equals(existing.sourceId()) || !actor.equals(existing.requestedBy()) || !"RETRY_FILES".equals(existing.operationCode())
                    || !hash.equals(existing.requestHash())) throw conflict("이 멱등 키는 다른 첨부 재시도 요청에 이미 사용됐습니다.");
            return AttachmentJobResponse.from(existing);
        }
        if(reviews.selectConversionLinkDetails(sourceId)!=null) throw conflict("이미 운영 공고에 연결된 원문은 첨부 재시도로 덮어쓸 수 없습니다.");
        var set=evidence.selectSetDetails(sourceId,request.expectedSetId()); if(set==null) throw notFound();
        var row=current.selectSourceDetails(sourceId); var v=request.version();
        if(row==null || !v.expectedBaseDecisionId().equals(source.baseEvaluationId())
                || !v.expectedAttachmentDecisionId().equals(source.currentAttachmentEvaluationId())
                || !v.expectedSourceVersion().equals(source.sourceVersion()) || !v.expectedAttachmentVersion().equals(source.attachmentVersion())
                || !Objects.equals(row.attachmentDecisionId(),source.currentAttachmentEvaluationId()) || !request.expectedSetId().equals(row.setId())
                || !v.expectedSetHash().equals(set.manifestHash()) || !Objects.equals(row.setHash(),set.manifestHash()))
            throw conflict("현재 원문·첨부 판정 또는 버전이 변경됐습니다. 선택한 입력을 유지하고 최신 근거를 다시 확인하세요.");
        if(!"SEALED".equals(set.setStatus()) || !Boolean.TRUE.equals(set.discoveryComplete()) || !"FOUND".equals(set.discoveryStatus())
                || reviews.selectActiveNormalJobExists(sourceId))
            throw conflict("첨부 전체 발견과 봉인이 끝난 뒤 실패 파일을 재시도할 수 있습니다. 발견 실패는 전체 재수집이 필요합니다.");
        if(!Objects.equals(source.contentVersionId(),set.contentVersionId())
                || (Boolean.TRUE.equals(source.attachmentReviewRequired()) && !set.policyId().equals(source.attachmentPolicyId())))
            throw conflict("현재 원문과 첨부 정책 연결이 다릅니다. 기존 검수 의무를 해제하지 않고 재확인해야 합니다.");
        var original=roles.selectSetJobDetails(sourceId,set.setId(),row.attachmentDecisionId());
        if(original==null || !Objects.equals(original.baseEvaluationId(),source.baseEvaluationId())
                || !Objects.equals(original.ruleReleaseId(),source.ruleReleaseId()) || !set.policyId().equals(original.policyId()))
            throw conflict("현재 근거의 고정 실행 작업을 확인할 수 없습니다.");
        var execution=selectExecution(original.executionSnapshotJson());
        if(!set.profileHash().equals(execution.profileHash())) throw conflict("원래 첨부 근거와 고정 profile hash가 다릅니다.");
        validatePolicy(source,execution,jobs.selectPolicyDetails(set.policyId()),request.maximumDownloadBytes());
        var files=evidence.selectFileList(sourceId,set.setId(),0,11);
        if(files==null || files.isEmpty() || files.size()>10 || files.size()!=evidence.selectFileCount(sourceId,set.setId())
                || !Objects.equals(set.discoveredCount(),files.size()) || !Objects.equals(set.processedCount(),files.size()))
            throw conflict("봉인된 첨부 전체의 파일·처리 건수가 일치하지 않습니다.");
        for(UUID id:request.fileIds()) {
            var file=files.stream().filter(f->id.equals(f.fileId())).findFirst().orElseThrow(this::notFound);
            if(!selectRetryable(file)) throw invalid("완전 추출 성공·다운로드 차단·OCR 필요·암호 문서·미지원 파일은 실패 재시도 대상이 아닙니다. 해당 상태의 수동 확인 안내를 따르세요.");
        }
        var originalFiles=roles.selectFileCopyList(sourceId,set.setId());
        if(originalFiles==null || originalFiles.size()!=files.size()) throw conflict("보존할 원래 파일 전체의 근거가 일치하지 않습니다.");
        for(var file:originalFiles)
            if(file.extractionId()!=null && (!execution.extractorVersion().equals(file.extractorVersion())
                    || !execution.extractorConfigHash().equals(file.extractorConfigHash())))
                throw conflict("보존할 추출 근거의 실행 버전이 현재 고정 작업과 다릅니다.");
        if(!retries.selectRetryRateAllowed(sourceId)) throw new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_RETRY_RATE_LIMITED,HttpStatus.TOO_MANY_REQUESTS,
                "전체 첨부 수집과 실패 파일 재시도는 합산하여 원문별 60초 간격, 최근 24시간 최대 3회입니다. 기존 작업 결과를 확인한 뒤 다시 요청하세요.");
        UUID jobId=UUID.randomUUID();
        requireOne(jobs.insertAttachmentJob(new AttachmentJobInsertCommand(jobId,sourceId,source.contentVersionId(),source.baseEvaluationId(),source.ruleReleaseId(),
                set.policyId(),jobs.selectNextGeneration(sourceId,source.contentVersionId(),set.policyId()),source.sourceVersion(),source.attachmentVersion()+1,
                key,hash,original.executionSnapshotJson(),request.maximumDownloadBytes(),null,Boolean.TRUE.equals(source.attachmentReviewRequired()),
                Boolean.TRUE.equals(source.attachmentReviewRequired()),source.attachmentPolicyId(),source.currentAttachmentEvaluationId(),
                jobs.selectCurrentConfirmationId(sourceId),"RETRY_FILES",set.setId(),actor,null)));
        for(UUID fileId:request.fileIds()) requireOne(retries.insertRetryFile(jobId,sourceId,set.setId(),fileId));
        requireOne(jobs.updateAttachmentSourceVersion(sourceId,source.attachmentVersion()));
        jobs.updateAttachmentEvaluationsStale(sourceId);jobs.updateAttachmentConfirmationsStale(sourceId);
        sources.insertAuditLog(new AnnouncementSourceAuditLogCommand(actor,"ATTACHMENT_FILES_RETRY","ANNOUNCEMENT_SOURCE",sourceId,"SUCCESS",
                selectJson(Map.of("jobId",jobId,"referenceSetId",set.setId(),"selectedFileCount",selected.size(),"maximumDownloadBytes",request.maximumDownloadBytes(),
                        "maximumHttpRequests",3*(1+selected.size())*4,"reasonHash",selectHash(request.reason().strip())))));
        return AttachmentJobResponse.from(Objects.requireNonNull(jobs.selectJobDetails(jobId)));
    }
    @Override @Transactional(readOnly=true,timeout=10)
    public List<AttachmentRetryFileRow> selectRetryFileList(UUID jobId,UUID leaseToken) {
        if(jobId==null || leaseToken==null) return List.of();
        return List.copyOf(retries.selectRetryFileList(jobId,leaseToken));
    }
    private boolean selectRetryable(AttachmentFileSummaryRow file) {
        return Set.of("FAILED","CANCELLED").contains(file.downloadStatusCode()) || ("SUCCEEDED".equals(file.downloadStatusCode())
                && file.qualityCode()!=null && Set.of("PARTIAL_TEXT","CORRUPT","LIMIT_EXCEEDED","TIMEOUT","FAILED","ISOLATION_UNAVAILABLE").contains(file.qualityCode()));
    }
    private void validatePolicy(AttachmentSourceContextRow source,AttachmentExecutionSnapshot execution,AttachmentPolicyRow policy,long maximumBytes) {
        if(policy==null || !"ACTIVE".equals(policy.policyStatusCode()) || !"ACTIVE".equals(policy.releaseStatusCode())
                || policy.policyHash()==null || "OFF".equals(policy.modeCode()) || !source.ruleReleaseId().equals(policy.ruleReleaseId())
                || !retries.selectRetryControlAllowed()) throw conflict("현재 ACTIVE 규칙과 게시된 수집 정책이 필요합니다. OFF/퇴역 정책으로 새 재시도를 시작하지 않습니다.");
        try {
            var settings=mapper.readTree(policy.settingsJson());var manifest=mapper.readTree(policy.profileManifestJson());
            boolean allowed=false;
            for(var profile:manifest) if(source.providerCode().equals(profile.path("providerCode").asText())
                    && execution.profileCode().equals(profile.path("profileCode").asText()) && execution.profileHash().equals(profile.path("profileHash").asText())) allowed=true;
            if(!allowed || !AnnouncementAttachmentClassificationEngine.VERSION.equals(execution.engineVersion())
                    || !execution.engineVersion().equals(settings.path("engineVersion").asText())
                    || !execution.extractorVersion().equals(settings.path("extractorVersion").asText())
                    || !execution.extractorConfigHash().equals(settings.path("extractorConfigHash").asText())
                    || !settings.path("maximumSourceBytes").isIntegralNumber() || !settings.path("maximumSourceBytes").canConvertToLong()
                    || settings.path("maximumSourceBytes").asLong()<1 || settings.path("maximumSourceBytes").asLong()>83886080
                    || maximumBytes>settings.path("maximumSourceBytes").asLong()
                    || profiles.selectProfileDetails(source.providerCode(),execution.profileCode(),execution.profileHash()).isEmpty())
                throw conflict("게시된 정책의 시스템 profile·추출 버전 또는 다운로드 상한과 재시도 조건이 다릅니다.");
        } catch(JsonProcessingException exception) { throw conflict("게시된 첨부 정책 설정을 해석할 수 없습니다."); }
    }
    private void validateRequest(UUID sourceId,UUID key,AttachmentRetryRequest request) {
        if(sourceId==null || key==null || request==null || request.version()==null || request.expectedSetId()==null
                || request.fileIds()==null || request.fileIds().isEmpty() || request.fileIds().size()>10 || request.fileIds().stream().anyMatch(Objects::isNull)
                || request.maximumDownloadBytes()==null || request.maximumDownloadBytes()<1 || request.maximumDownloadBytes()>83886080
                || request.reason()==null || request.reason().isBlank() || request.reason().length()>1000) throw invalid("현재 판정·집합, 1~10개 실패 파일, 최대 80 MiB 다운로드 상한과 1~1000자 사유를 입력하세요.");
        var v=request.version();
        if(v.expectedBaseDecisionId()==null || v.expectedAttachmentDecisionId()==null || v.expectedSourceVersion()==null || v.expectedSourceVersion()<0
                || v.expectedAttachmentVersion()==null || v.expectedAttachmentVersion()<0 || v.expectedAttachmentVersion()==Integer.MAX_VALUE
                || v.expectedSetHash()==null || !v.expectedSetHash().matches("[0-9a-f]{64}")) throw invalid("현재 기본·첨부 판정 ID, 유효한 버전 및 소문자 SHA-256 집합 hash가 필요합니다.");
    }
    private UUID selectActor(Authentication authentication) {
        if(authentication==null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof AuthenticatedUserDetails details))
            throw new ApiException(ErrorCode.AUTH_REQUIRED,HttpStatus.UNAUTHORIZED,"로그인한 운영 계정이 필요합니다.");
        if(!details.isEnabled() || details.passwordResetRequired() || details.roles().stream().noneMatch(Set.of("ADMIN","OPERATOR")::contains))
            throw new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_ACTION_FORBIDDEN,HttpStatus.FORBIDDEN,"활성 ADMIN 또는 OPERATOR 계정으로 비밀번호 변경을 완료한 뒤 재시도할 수 있습니다.");
        return details.userId();
    }
    private AttachmentExecutionSnapshot selectExecution(String json) {
        try { var value=mapper.readValue(json,AttachmentExecutionSnapshot.class);if(value==null) throw conflict("고정 실행 버전이 없습니다.");return value; }
        catch(JsonProcessingException|IllegalArgumentException exception) { throw conflict("고정 실행 버전을 확인할 수 없습니다."); }
    }
    private String selectJson(Object value) { try { return mapper.writeValueAsString(value); } catch(JsonProcessingException exception) { throw invalid("재시도 입력을 저장할 수 없습니다."); } }
    private String selectHash(String value) { try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch(NoSuchAlgorithmException exception) { throw new IllegalStateException("SHA-256을 사용할 수 없습니다."); } }
    private void requireOne(int count) { if(count!=1) throw conflict("재시도 예약 중 상태가 변경됐습니다. 최신 근거를 다시 확인하세요."); }
    private ApiException invalid(String message) { return new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_RETRY_INVALID,HttpStatus.BAD_REQUEST,message); }
    private ApiException conflict(String message) { return new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_VERSION_CONFLICT,HttpStatus.CONFLICT,message); }
    private ApiException notFound() { return new ApiException(ErrorCode.RESOURCE_NOT_FOUND,HttpStatus.NOT_FOUND,"이 원문에 속하는 현재 첨부 집합·파일을 찾을 수 없습니다."); }
}
