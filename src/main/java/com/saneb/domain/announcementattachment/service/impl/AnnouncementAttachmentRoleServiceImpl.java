package com.saneb.domain.announcementattachment.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.common.error.ApiException;
import com.saneb.common.error.ErrorCode;
import com.saneb.domain.announcementattachment.classification.AnnouncementAttachmentClassificationEngine;
import com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentCurrentDao;
import com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentEvidenceDao;
import com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentJobDao;
import com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentReviewDao;
import com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentRoleDao;
import com.saneb.domain.announcementattachment.dto.AttachmentJobResponse;
import com.saneb.domain.announcementattachment.dto.AttachmentRoleRequest;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentRoleService;
import com.saneb.domain.announcementattachment.vo.AttachmentEvidenceCommands;
import com.saneb.domain.announcementattachment.vo.AttachmentExecutionSnapshot;
import com.saneb.domain.announcementattachment.vo.AttachmentJobInsertCommand;
import com.saneb.domain.announcementattachment.vo.AttachmentRoleRows;
import com.saneb.domain.announcementsource.dao.AnnouncementSourceDao;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceAuditLogCommand;
import com.saneb.domain.auth.vo.AuthenticatedUserDetails;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** DB 근거 복제와 예약만 수행한다. HTTP/파일 추출과 CPU 판정은 이 transaction에서 실행하지 않는다. */
@Service
public class AnnouncementAttachmentRoleServiceImpl implements AnnouncementAttachmentRoleService {
    private static final Set<String> ROLES=Set.of("NOTICE","GUIDE","FORM","REFERENCE","UNKNOWN");
    private final AnnouncementAttachmentJobDao jobs;
    private final AnnouncementAttachmentCurrentDao current;
    private final AnnouncementAttachmentEvidenceDao evidence;
    private final AnnouncementAttachmentReviewDao reviews;
    private final AnnouncementAttachmentRoleDao roles;
    private final AnnouncementSourceDao sources;
    private final ObjectMapper mapper;
    public AnnouncementAttachmentRoleServiceImpl(AnnouncementAttachmentJobDao jobs,AnnouncementAttachmentCurrentDao current,
            AnnouncementAttachmentEvidenceDao evidence,AnnouncementAttachmentReviewDao reviews,AnnouncementAttachmentRoleDao roles,
            AnnouncementSourceDao sources,ObjectMapper mapper) {
        this.jobs=jobs; this.current=current; this.evidence=evidence; this.reviews=reviews; this.roles=roles; this.sources=sources; this.mapper=mapper;
    }

    @Override @Transactional(timeout=20)
    public AttachmentJobResponse insertRoleChange(Authentication authentication,UUID sourceId,UUID idempotencyKey,AttachmentRoleRequest request) {
        UUID actor=selectActor(authentication);
        validateRequest(sourceId,idempotencyKey,request);
        var requestedRoles=selectRequestedRoles(request);
        String requestHash=selectHash(selectJson(List.of("attachment-role-change-v1",sourceId,actor,request.version(),request.expectedSetId(),
                requestedRoles,request.reason().strip())));
        var source=jobs.selectSourceContextDetailsForUpdate(sourceId);
        if(source==null || !"PRODUCTION".equals(source.dataPurposeCode()) || "EXCLUDED".equals(source.semanticStatusCode())
                || source.titleStageCode()==null || !Set.of("GROUP_A_MATCHED","COMBINATION_MATCHED").contains(source.titleStageCode())) throw notFound();
        var existing=jobs.selectIdempotentJobDetails(idempotencyKey);
        if(existing!=null) {
            if(!sourceId.equals(existing.sourceId()) || !actor.equals(existing.requestedBy()) || !"ROLE_CHANGE".equals(existing.operationCode())
                    || !requestHash.equals(existing.requestHash())) throw conflict("이 멱등 키는 다른 역할 변경 요청에 이미 사용됐습니다.");
            return AttachmentJobResponse.from(existing);
        }
        if(reviews.selectConversionLinkDetails(sourceId)!=null) throw conflict("이미 운영 공고에 연결된 원문은 역할 변경으로 덮어쓸 수 없습니다.");
        var set=evidence.selectSetDetails(sourceId,request.expectedSetId());
        if(set==null) throw notFound();
        var row=current.selectSourceDetails(sourceId);
        var version=request.version();
        if(row==null || !version.expectedBaseDecisionId().equals(source.baseEvaluationId())
                || !version.expectedAttachmentDecisionId().equals(source.currentAttachmentEvaluationId())
                || !version.expectedSourceVersion().equals(source.sourceVersion()) || !version.expectedAttachmentVersion().equals(source.attachmentVersion())
                || !Objects.equals(row.attachmentDecisionId(),source.currentAttachmentEvaluationId()) || !request.expectedSetId().equals(row.setId())
                || !version.expectedSetHash().equals(set.manifestHash()) || !Objects.equals(row.setHash(),set.manifestHash()))
            throw conflict("본문·첨부 판정 또는 버전이 변경됐습니다. 입력을 유지한 채 최신 첨부 전체를 다시 확인하세요.");
        if(!"SEALED".equals(set.setStatus()) || reviews.selectActiveNormalJobExists(sourceId))
            throw notReady("첨부 전체 처리와 봉인이 끝난 뒤 역할을 변경할 수 있습니다.");
        if(!Objects.equals(source.contentVersionId(),set.contentVersionId())
                || (Boolean.TRUE.equals(source.attachmentReviewRequired()) && !set.policyId().equals(source.attachmentPolicyId())))
            throw conflict("현재 원문과 첨부 정책 연결이 다릅니다. 최신 근거를 다시 처리해야 합니다.");
        var originalJob=roles.selectSetJobDetails(sourceId,set.setId(),row.attachmentDecisionId());
        if(originalJob==null || !Objects.equals(source.baseEvaluationId(),originalJob.baseEvaluationId())
                || !Objects.equals(source.ruleReleaseId(),originalJob.ruleReleaseId()) || !set.policyId().equals(originalJob.policyId()))
            throw notReady("현재 첨부 판정에 연결된 고정 실행 버전이 없습니다. 먼저 첨부 수집 결과를 확인하세요.");
        var execution=selectExecution(originalJob.executionSnapshotJson());
        if(!AnnouncementAttachmentClassificationEngine.VERSION.equals(execution.engineVersion()) || !set.profileHash().equals(execution.profileHash()))
            throw conflict("저장된 근거의 엔진·프로필 버전과 현재 역할 재판정 조건이 다릅니다.");
        var policy=jobs.selectPolicyDetails(set.policyId());
        if(policy==null || policy.policyHash()==null || !source.ruleReleaseId().equals(policy.ruleReleaseId())
                || !List.of("ACTIVE","RETIRED").contains(policy.policyStatusCode()) || !List.of("ACTIVE","RETIRED").contains(policy.releaseStatusCode()))
            throw conflict("저장된 근거에 연결된 게시 규칙·정책을 확인할 수 없습니다. 초안으로 재판정하지 않습니다.");
        // OFF는 새 HTTP를 중지한다. 봉인 근거의 역할 재검토는 기존 고정 정책/검수 의무로 계속할 수 있다.
        var files=roles.selectFileCopyList(sourceId,set.setId());
        if(files==null || files.isEmpty() || files.size()>10 || files.size()!=evidence.selectFileCount(sourceId,set.setId())
                || !Objects.equals(set.discoveredCount(),files.size()) || !Objects.equals(set.processedCount(),files.size()))
            throw notReady("변경할 첨부 전체의 파일·처리 건수가 일치하지 않습니다.");
        if(files.size()!=requestedRoles.size()) throw invalid("현재 첨부 전체의 파일 ID와 역할을 한 번씩 제출하세요.");
        boolean changed=false;
        for(var file:files) {
            String role=requestedRoles.get(file.fileId().toString());
            if(role==null) throw notFound();
            changed|=!role.equals(file.role());
            if(file.extractionId()!=null && (!execution.extractorVersion().equals(file.extractorVersion())
                    || !execution.extractorConfigHash().equals(file.extractorConfigHash())))
                throw conflict("재사용할 추출 결과의 버전이 원래 작업과 다릅니다. 역할만 변경해 합칠 수 없습니다.");
        }
        if(!changed) throw invalid("현재와 다른 문서 역할을 한 개 이상 선택하세요. 동일 역할로 새 세대를 만들지 않습니다.");
        UUID jobId=UUID.randomUUID(),newSetId=UUID.randomUUID();
        var copies=new ArrayList<AttachmentRoleRows.Copy>();
        for(var file:files) {
            String role=requestedRoles.get(file.fileId().toString());
            copies.add(new AttachmentRoleRows.Copy(sourceId,set.setId(),file.fileId(),file.extractionId(),newSetId,UUID.randomUUID(),
                    file.extractionId()==null?null:UUID.randomUUID(),role,role.equals(file.role())?file.roleOrigin():"MANUAL"));
        }
        String manifest=selectHash(selectJson(List.of("attachment-role-manifest-v1",set.setId(),set.manifestHash(),newSetId,execution,copies)));
        requireOne(evidence.insertSet(new AttachmentEvidenceCommands.SetInsert(newSetId,sourceId,source.contentVersionId(),set.policyId(),source.dataPurposeCode(),set.profileHash())));
        for(var copy:copies) {
            requireOne(roles.insertFileCopy(copy));
            if(copy.originalExtractionId()!=null) requireOne(roles.insertExtractionCopy(copy));
        }
        requireOne(evidence.updateSetSealed(newSetId,manifest,set.discoveryStatus(),Boolean.TRUE.equals(set.discoveryComplete()),files.size(),selectJson(set.warningCodes())));
        requireOne(jobs.insertAttachmentJob(new AttachmentJobInsertCommand(jobId,sourceId,source.contentVersionId(),source.baseEvaluationId(),source.ruleReleaseId(),
                set.policyId(),jobs.selectNextGeneration(sourceId,source.contentVersionId(),set.policyId()),source.sourceVersion(),source.attachmentVersion()+1,
                idempotencyKey,requestHash,originalJob.executionSnapshotJson(),originalJob.downloadBudgetBytes(),null,
                Boolean.TRUE.equals(source.attachmentReviewRequired()),Boolean.TRUE.equals(source.attachmentReviewRequired()),source.attachmentPolicyId(),
                source.currentAttachmentEvaluationId(),jobs.selectCurrentConfirmationId(sourceId),"ROLE_CHANGE",set.setId(),actor,newSetId)));
        requireOne(jobs.updateAttachmentSourceVersion(sourceId,source.attachmentVersion()));
        jobs.updateAttachmentEvaluationsStale(sourceId); jobs.updateAttachmentConfirmationsStale(sourceId);
        sources.insertAuditLog(new AnnouncementSourceAuditLogCommand(actor,"ATTACHMENT_ROLES_UPDATE","ANNOUNCEMENT_SOURCE",sourceId,"SUCCESS",
                selectJson(Map.of("jobId",jobId,"previousSetId",set.setId(),"newSetId",newSetId,"sourceVersion",source.sourceVersion(),
                        "attachmentVersion",source.attachmentVersion()+1,"fileCount",files.size(),"reasonHash",selectHash(request.reason().strip())))));
        return AttachmentJobResponse.from(Objects.requireNonNull(jobs.selectJobDetails(jobId)));
    }
    @Override @Transactional(readOnly=true,timeout=10)
    public AttachmentJobResponse selectJobDetails(UUID sourceId,UUID jobId) {
        if(sourceId==null || jobId==null || current.selectSourceDetails(sourceId)==null) throw notFound();
        var job=jobs.selectJobDetails(jobId);
        if(job==null || !sourceId.equals(job.sourceId())) throw notFound();
        return AttachmentJobResponse.from(job);
    }
    private Map<String,String> selectRequestedRoles(AttachmentRoleRequest request) {
        var normalized=new TreeMap<String,String>();
        for(var file:request.fileRoles()) {
            if(file==null || file.fileId()==null || file.documentRoleCode()==null) throw invalid("각 파일 ID와 문서 역할을 입력하세요.");
            String role=file.documentRoleCode().strip().toUpperCase(Locale.ROOT);
            if(!ROLES.contains(role)) throw invalid("문서 역할은 NOTICE(공고문), GUIDE(지원 안내), FORM(신청 양식), REFERENCE(참고자료), UNKNOWN(미확인) 중 하나여야 합니다.");
            if(normalized.put(file.fileId().toString(),role)!=null) throw invalid("같은 파일 ID의 역할을 중복 제출할 수 없습니다.");
        }
        return normalized;
    }
    private void validateRequest(UUID sourceId,UUID key,AttachmentRoleRequest request) {
        if(sourceId==null || key==null || request==null || request.version()==null || request.expectedSetId()==null
                || request.fileRoles()==null || request.fileRoles().isEmpty() || request.fileRoles().size()>10
                || request.reason()==null || request.reason().isBlank() || request.reason().length()>1000)
            throw invalid("원문·집합 ID, 멱등 키, 1~10개 첨부 전체 역할과 1~1000자 변경 사유가 필요합니다.");
        var v=request.version();
        if(v.expectedBaseDecisionId()==null || v.expectedAttachmentDecisionId()==null || v.expectedSourceVersion()==null || v.expectedSourceVersion()<0
                || v.expectedAttachmentVersion()==null || v.expectedAttachmentVersion()<0 || v.expectedAttachmentVersion()==Integer.MAX_VALUE
                || v.expectedSetHash()==null || !v.expectedSetHash().matches("[0-9a-f]{64}"))
            throw invalid("현재 기본·첨부 판정 ID, 0 이상의 버전 및 소문자 SHA-256 첨부 집합 hash가 필요합니다.");
    }
    private UUID selectActor(Authentication authentication) {
        if(authentication==null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof AuthenticatedUserDetails details))
            throw new ApiException(ErrorCode.AUTH_REQUIRED,HttpStatus.UNAUTHORIZED,"로그인한 운영 계정이 필요합니다.");
        if(!details.isEnabled() || details.passwordResetRequired() || details.roles().stream().noneMatch(Set.of("ADMIN","OPERATOR")::contains))
            throw new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_ACTION_FORBIDDEN,HttpStatus.FORBIDDEN,"활성 ADMIN 또는 OPERATOR 계정으로 비밀번호 변경을 완료한 뒤 역할을 변경할 수 있습니다.");
        return details.userId();
    }
    private AttachmentExecutionSnapshot selectExecution(String json) {
        if(json==null) throw notReady("원래 작업의 고정 실행 버전이 없습니다.");
        try {
            var execution=mapper.readValue(json,AttachmentExecutionSnapshot.class);
            if(execution==null) throw notReady("원래 작업의 고정 실행 버전이 없습니다.");
            return execution;
        }
        catch(JsonProcessingException exception) { throw notReady("원래 작업의 고정 실행 버전을 해석할 수 없습니다."); }
    }
    private String selectJson(Object input) {
        try { return mapper.writeValueAsString(input); } catch(JsonProcessingException exception) { throw invalid("역할 변경 입력을 저장할 수 없습니다."); }
    }
    private String selectHash(String input) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8))); }
        catch(NoSuchAlgorithmException exception) { throw new IllegalStateException("SHA-256을 사용할 수 없습니다."); }
    }
    private void requireOne(int count) { if(count!=1) throw conflict("역할 변경 중 다른 작업이 상태를 변경했습니다. 최신 근거를 다시 확인하세요."); }
    private ApiException invalid(String message) { return new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_ROLE_INVALID,HttpStatus.BAD_REQUEST,message); }
    private ApiException conflict(String message) { return new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_VERSION_CONFLICT,HttpStatus.CONFLICT,message); }
    private ApiException notReady(String message) { return new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_NOT_READY,HttpStatus.CONFLICT,message); }
    private ApiException notFound() { return new ApiException(ErrorCode.RESOURCE_NOT_FOUND,HttpStatus.NOT_FOUND,"이 원문에 속하는 현재 첨부 집합·파일·작업을 찾을 수 없습니다."); }
}
