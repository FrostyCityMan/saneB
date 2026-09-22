package com.saneb.domain.announcementattachment.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.saneb.common.error.ApiException;
import com.saneb.common.error.ErrorCode;
import com.saneb.common.response.PageResponse;
import com.saneb.domain.announcementattachment.dao.*;
import com.saneb.domain.announcementattachment.dto.*;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentPolicyCheckService;
import com.saneb.domain.announcementattachment.vo.*;
import com.saneb.domain.announcementsource.dao.AnnouncementSourceDao;
import com.saneb.domain.announcementsource.service.AnnouncementSourceRuleReleaseService;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceAuditLogCommand;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceRuleValidationDetails;
import com.saneb.domain.auth.vo.AuthenticatedUserDetails;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.*;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.TransactionTemplate;

/** 읽기 snapshot → DB transaction 밖 정답 세트 → 짧은 잠금/버전 재검증으로 이력을 저장한다. */
@Service
public class AnnouncementAttachmentPolicyCheckServiceImpl implements AnnouncementAttachmentPolicyCheckService {
    private final AnnouncementAttachmentPolicyDao policies;
    private final AnnouncementAttachmentPolicyCheckDao checks;
    private final AnnouncementSourceRuleReleaseService rules;
    private final AnnouncementAttachmentPolicyGoldenGate golden;
    private final AnnouncementSourceDao audit;
    private final ObjectMapper mapper;
    private final TransactionTemplate read;
    private final TransactionTemplate write;
    public AnnouncementAttachmentPolicyCheckServiceImpl(AnnouncementAttachmentPolicyDao policies,AnnouncementAttachmentPolicyCheckDao checks,
            AnnouncementSourceRuleReleaseService rules,AnnouncementAttachmentPolicyGoldenGate golden,AnnouncementSourceDao audit,ObjectMapper mapper,PlatformTransactionManager transactions) {
        this.policies=policies;this.checks=checks;this.rules=rules;this.golden=golden;this.audit=audit;
        this.mapper=mapper.copy().enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
        read=new TransactionTemplate(transactions);read.setReadOnly(true);read.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);read.setTimeout(10);
        write=new TransactionTemplate(transactions);write.setTimeout(10);
    }
    @Override @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ,timeout=10)
    public PageResponse<AttachmentPolicyCheckResponse> selectCheckList(Authentication authentication,UUID policyId,int page,int size) {
        selectActor(authentication,false);selectPolicy(policyId,false);
        if(page<1 || size<1 || size>100 || ((long)page-1)*size>Integer.MAX_VALUE) throw invalid("페이지는 1 이상, 페이지 크기는 1~100이어야 합니다.");
        var search=new AttachmentPolicyCheckRows.Search(policyId,size,(page-1)*size);
        long count=checks.selectCheckCount(search);
        return PageResponse.of(checks.selectCheckList(search).stream().map(this::selectResponse).toList(),page,size,count);
    }
    @Override @Transactional(propagation=Propagation.NOT_SUPPORTED)
    public AttachmentPolicyCheckResponse insertClassificationCheck(Authentication authentication,UUID policyId,UUID key,AttachmentPolicyCheckRequest request) {
        UUID actor=selectActor(authentication,true);
        if(policyId==null || key==null || request==null || request.expectedVersion()==null || request.expectedVersion()<0
                || request.reason()==null || request.reason().isBlank() || request.reason().length()>1000)
            throw invalid("정책 ID·UUID 멱등 키·0 이상 조회 버전·공백 아닌 1~1000자 검증 사유가 필요합니다.");
        String requestHash=hash(List.of("attachment-policy-classification-check-v1",actor,policyId,request.expectedVersion(),request.reason().strip()));
        var prepared=read.execute(tx->{
            var existing=checks.selectCheckDetails(key);
            if(existing!=null) return new Prepared(null,selectSame(existing,actor,policyId,requestHash));
            var policy=selectPolicy(policyId,false);validateDraft(policy,request.expectedVersion());
            return new Prepared(selectSnapshot(policy),null);
        });
        if(prepared==null) throw conflict("정책 검증 snapshot을 읽지 못했습니다.");
        if(prepared.existing()!=null) return prepared.existing();
        var frozen=prepared.snapshot();
        var result=golden.selectValidatedResult(frozen.rule().ruleSet(),frozen.rule().calculatedSnapshotHash(),frozen.configuration());
        return write.execute(tx->{
            checks.selectRequestLock(key);
            var existing=checks.selectCheckDetails(key);
            if(existing!=null) return selectSame(existing,actor,policyId,requestHash);
            // 다른 정책 쓰기와 동일하게 규칙 → 정책 순서로 잠근다. 엔진별 정답 실행은 transaction 밖에서 끝났다.
            policies.selectRuleStatus(frozen.rule().releaseId());
            var currentPolicy=selectPolicy(policyId,true);validateDraft(currentPolicy,request.expectedVersion());
            var current=selectSnapshot(currentPolicy);
            if(!frozen.policyHash().equals(current.policyHash()) || !frozen.rule().equals(current.rule()))
                throw conflict("검증 중 정책 또는 규칙이 바뀌었습니다. 입력을 보존하고 현재 버전으로 다시 검증하세요.");
            if(!AnnouncementAttachmentPolicyGoldenGate.selectContractCurrent(result,current.configuration()) || !result.ruleSnapshotHash().equals(current.rule().calculatedSnapshotHash()))
                throw conflict("서버 분류 검증 결과가 현재 snapshot과 일치하지 않습니다.");
            UUID id=UUID.randomUUID();
            if(checks.insertCheck(new AttachmentPolicyCheckRows.Insert(id,policyId,currentPolicy.rowVersion(),current.policyHash(),current.rule().releaseId(),
                    current.rule().rowVersion(),result.ruleSnapshotHash(),result.ruleContentHash(),result.suiteVersion(),result.engineVersion(),result.resultHash(),
                    result.caseCount(),json(result.caseIds()),actor,key,requestHash))!=1) throw conflict("분류 검증 이력을 저장하지 못했습니다.");
            audit.insertAuditLog(new AnnouncementSourceAuditLogCommand(actor,"ATTACHMENT_POLICY_CLASSIFICATION_CHECK","ANNOUNCEMENT_ATTACHMENT_POLICY",policyId,"SUCCESS",
                    json(Map.of("checkId",id,"policyVersion",currentPolicy.rowVersion(),"caseCount",result.caseCount(),"resultHash",result.resultHash(),"reasonHash",hash(request.reason().strip())))));
            return selectResponse(checks.selectCheckDetails(key));
        });
    }
    private Snapshot selectSnapshot(AttachmentPolicyManagementRows.Row policy) {
        var rule=rules.selectRuleValidationDetails(policy.ruleReleaseId());
        if(rule==null || !policy.ruleReleaseId().equals(rule.releaseId()) || rule.rowVersion()==null || rule.rowVersion()<0
                || !Set.of("DRAFT","ACTIVE").contains(rule.releaseStatusCode())) throw conflict("검증할 정책에는 현재 DRAFT 또는 ACTIVE 키워드 규칙이 필요합니다.");
        if("ACTIVE".equals(rule.releaseStatusCode()) && !Objects.equals(rule.persistedSnapshotHash(),rule.calculatedSnapshotHash()))
            throw conflict("게시된 키워드 규칙의 저장 지문과 현재 내용이 다릅니다. 규칙 무결성을 확인하세요.");
        try {
            if(policy.settingsJson()==null || policy.settingsJson().length()>8192 || policy.profileManifestJson()==null || policy.profileManifestJson().length()>256000)
                throw conflict("정책 설정 snapshot 크기가 유효하지 않습니다.");
            var configuration=mapper.readValue(policy.settingsJson(),com.saneb.domain.announcementattachment.dto.AttachmentPolicyResponses.Configuration.class);
            if(configuration==null || !configuration.selectEngineCurrent())throw conflict("정책의 엔진·구간 규칙 버전과 지문이 현재 코드와 다릅니다.");
            return new Snapshot(policy,rule,hash(List.of("attachment-policy-draft-v1",policy.policyId(),policy.rowVersion(),policy.modeCode(),rule.releaseId(),
                    mapper.readValue(policy.settingsJson(),Object.class),mapper.readValue(policy.profileManifestJson(),Object.class))),configuration);
        } catch(ApiException exception) { throw exception; }
        catch(Exception exception) { throw conflict("정책 설정 snapshot을 읽지 못했습니다. 서버의 정책 형식을 확인하세요."); }
    }
    private AttachmentPolicyManagementRows.Row selectPolicy(UUID id,boolean lock) {
        var row=id==null?null:policies.selectPolicyDetails(id,lock);
        if(row==null || !id.equals(row.policyId())) throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND,HttpStatus.NOT_FOUND,"첨부 정책을 찾을 수 없습니다.");
        return row;
    }
    private void validateDraft(AttachmentPolicyManagementRows.Row row,Integer version) {
        if(!"DRAFT".equals(row.policyStatusCode())) throw new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_POLICY_NOT_DRAFT,HttpStatus.CONFLICT,"새 검증은 DRAFT 정책에서 실행하세요. 게시·퇴역 정책은 개정 초안을 먼저 만드세요.");
        if(!version.equals(row.rowVersion())) throw conflict("조회 이후 정책이 변경됐습니다. 입력을 보존하고 현재 버전을 확인하세요.");
    }
    private AttachmentPolicyCheckResponse selectSame(AttachmentPolicyCheckRows.Row row,UUID actor,UUID policyId,String requestHash) {
        if(!actor.equals(row.requestedBy()) || !policyId.equals(row.policyId()) || !requestHash.equals(row.requestHash()))
            throw conflict("이 멱등 키는 다른 관리자·정책·검증 요청에 사용됐습니다. 새 요청에는 새 키를 사용하세요.");
        return selectResponse(row);
    }
    private AttachmentPolicyCheckResponse selectResponse(AttachmentPolicyCheckRows.Row row) {
        if(row==null) throw conflict("분류 검증 이력을 찾을 수 없습니다.");
        try {
            if(row.caseIdsJson()==null || row.caseIdsJson().length()>16384) throw conflict("저장된 검증 목록 크기가 올바르지 않습니다.");
            List<String> ids=mapper.readValue(row.caseIdsJson(),mapper.getTypeFactory().constructCollectionType(List.class,String.class));
            if(ids==null || ids.size()!=row.caseCount() || !ids.equals(AnnouncementAttachmentPolicyGoldenGate.selectCaseIds(row.engineVersion())))
                throw conflict("저장된 검증 항목이 올바르지 않습니다.");
            return new AttachmentPolicyCheckResponse(row.checkId(),row.policyId(),row.policyVersion(),row.policySnapshotHash(),row.ruleReleaseId(),row.ruleVersion(),row.ruleSnapshotHash(),
                    row.ruleContentHash(),row.checkTypeCode(),row.suiteVersion(),row.engineVersion(),row.resultHash(),row.caseCount(),ids,Boolean.TRUE.equals(row.isCurrent()),row.createdAt());
        } catch(ApiException exception) { throw exception; }
        catch(Exception exception) { throw conflict("저장된 분류 검증 이력을 읽을 수 없습니다."); }
    }
    private UUID selectActor(Authentication authentication,boolean writeAccess) {
        if(authentication==null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof AuthenticatedUserDetails actor))
            throw new ApiException(ErrorCode.AUTH_REQUIRED,HttpStatus.UNAUTHORIZED,"로그인한 운영 계정이 필요합니다.");
        var permitted=writeAccess?Set.of("ADMIN"):Set.of("ADMIN","OPERATOR","APPROVER");
        if(!actor.isEnabled() || actor.passwordResetRequired() || actor.roles().stream().noneMatch(permitted::contains))
            throw new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_ACTION_FORBIDDEN,HttpStatus.FORBIDDEN,
                    writeAccess?"정책 검증 실행은 비밀번호 변경을 완료한 활성 ADMIN만 할 수 있습니다.":"정책 검증 이력은 활성 ADMIN, OPERATOR, APPROVER만 조회할 수 있습니다.");
        return actor.userId();
    }
    private String json(Object value) { try {return mapper.writeValueAsString(value);}catch(Exception exception){throw invalid("검증 metadata를 직렬화하지 못했습니다.");} }
    private String hash(Object value) {try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json(value).getBytes(StandardCharsets.UTF_8)));}catch(ApiException exception){throw exception;}catch(Exception exception){throw invalid("검증 지문을 생성하지 못했습니다.");} }
    private ApiException invalid(String message) {return new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_POLICY_INVALID,HttpStatus.BAD_REQUEST,message);}
    private ApiException conflict(String message) {return new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_VERSION_CONFLICT,HttpStatus.CONFLICT,message);}
    private record Snapshot(AttachmentPolicyManagementRows.Row policy,AnnouncementSourceRuleValidationDetails rule,String policyHash,
                            com.saneb.domain.announcementattachment.dto.AttachmentPolicyResponses.Configuration configuration) { }
    private record Prepared(Snapshot snapshot,AttachmentPolicyCheckResponse existing) { }
}
