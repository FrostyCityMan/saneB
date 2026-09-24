package com.saneb.domain.announcementattachment.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.common.error.ApiException;
import com.saneb.common.error.ErrorCode;
import com.saneb.common.response.PageResponse;
import com.saneb.domain.announcementattachment.classification.AttachmentSegmentClassificationEngine;
import com.saneb.domain.announcementattachment.classification.AttachmentSegmentRoleAnalyzer;
import com.saneb.domain.announcementattachment.classification.AttachmentEngineContract;
import com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentPolicyDao;
import com.saneb.domain.announcementattachment.discovery.AttachmentDiscoveryProfileRegistry;
import com.saneb.domain.announcementattachment.dto.AttachmentPolicyRequests;
import com.saneb.domain.announcementattachment.dto.AttachmentPolicyResponses;
import com.saneb.domain.announcementattachment.extraction.AttachmentRuntimeIdentity;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentPolicyService;
import com.saneb.domain.announcementattachment.vo.AttachmentPolicyManagementRows;
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
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/** 초안 편집은 수집·검증 성공·게시와 다르다. 네트워크, 원문, 파일 실행을 수행하지 않는다. */
@Service
public class AnnouncementAttachmentPolicyServiceImpl implements AnnouncementAttachmentPolicyService {
    private static final Set<String> MODES=Set.of("OFF","COLLECT_ONLY","ENFORCE");
    private static final Set<String> STATES=Set.of("DRAFT","ACTIVE","RETIRED");
    private static final Set<String> PROVIDERS=Set.of("BIZINFO","GOV24_PUBLIC_SERVICE","LOCAL_GOV_NOTICE");
    private final AnnouncementAttachmentPolicyDao dao;
    private final AnnouncementSourceDao audit;
    private final AttachmentDiscoveryProfileRegistry profiles;
    private final ObjectMapper mapper;
    public AnnouncementAttachmentPolicyServiceImpl(AnnouncementAttachmentPolicyDao dao,AnnouncementSourceDao audit,
            AttachmentDiscoveryProfileRegistry profiles,ObjectMapper mapper) {
        this.dao=dao;this.audit=audit;this.profiles=profiles;this.mapper=mapper;
    }
    @Override @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ,timeout=10)
    public PageResponse<AttachmentPolicyResponses.Summary> selectPolicyList(Authentication actor,String status,UUID ruleReleaseId,int page,int size) {
        selectActor(actor,false);
        if(page<1 || size<1 || size>100 || ((long)page-1)*size>Integer.MAX_VALUE) throw invalid("페이지는 1 이상, 페이지 크기는 1~100이어야 합니다.");
        String normalized=status==null || status.isBlank()?null:status.strip();
        if(normalized!=null && !STATES.contains(normalized)) throw invalid("정책 상태는 DRAFT, ACTIVE, RETIRED 중 하나여야 합니다.");
        var condition=new AttachmentPolicyManagementRows.Search(normalized,ruleReleaseId,size,(page-1)*size);
        long count=dao.selectPolicyCount(condition);
        return PageResponse.of(dao.selectPolicyList(condition),page,size,count);
    }
    @Override @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ,timeout=10)
    public AttachmentPolicyResponses.Details selectPolicyDetails(Authentication actor,UUID policyId) {
        selectActor(actor,false);
        return selectDetails(selectPolicy(policyId,false),((AuthenticatedUserDetails)actor.getPrincipal()).roles().contains("ADMIN"));
    }
    @Override @Transactional(timeout=20)
    public AttachmentPolicyResponses.Details insertPolicy(Authentication authentication,UUID key,AttachmentPolicyRequests.Create request) {
        UUID actor=selectActor(authentication,true);
        if(key==null || request==null) throw invalid("정책 생성 입력과 UUID 형식 Idempotency-Key가 필요합니다.");
        validateFields(request.ruleReleaseId(),request.modeCode(),request.maximumSourceBytes(),request.reason());
        validateSegmentVersion(request.segmentRuleVersion());
        var identity=new ArrayList<Object>(List.of("attachment-policy-create-v1",actor,request.ruleReleaseId(),request.modeCode(),request.maximumSourceBytes(),request.reason().strip()));
        // 기존 생략 요청의 멱등 hash는 유지하고 명시한 구간 버전은 별도 요청으로 결합한다.
        if(request.segmentRuleVersion()!=null) identity.add(request.segmentRuleVersion());
        String hash=selectHash(selectJson(identity));
        dao.selectCreationLock(key);
        var existing=dao.selectCreationDetails(key);
        if(existing!=null) return selectSameCreation(existing,actor,hash,"CREATE");
        validateRule(request.ruleReleaseId());
        UUID id=UUID.randomUUID();
        String code="ATT-"+id.toString().replace("-","");
        String segmentVersion=request.segmentRuleVersion()==null?AttachmentSegmentRoleAnalyzer.VERSION:request.segmentRuleVersion();
        var configuration=selectConfiguration(request.maximumSourceBytes(),AttachmentSegmentClassificationEngine.VERSION,
                segmentVersion,AttachmentEngineContract.selectSegmentRulesHash(segmentVersion));
        String manifest=selectJson(selectSystemBindings());
        if(dao.insertPolicy(new AttachmentPolicyManagementRows.Insert(id,code,1,request.modeCode(),request.ruleReleaseId(),selectJson(configuration),manifest,
                actor,null,key,hash,"CREATE"))!=1) throw conflict("정책 초안을 저장하지 못했습니다. 생성 결과를 다시 확인하세요.");
        insertAudit(actor,id,"ATTACHMENT_POLICY_DRAFT_CREATE",request.reason(),Map.of("policyCode",code,"versionNo",1));
        return selectDetails(selectPolicy(id,false));
    }
    @Override @Transactional(timeout=20)
    public AttachmentPolicyResponses.Details updatePolicyDraft(Authentication authentication,UUID policyId,AttachmentPolicyRequests.Update request) {
        UUID actor=selectActor(authentication,true);
        if(policyId==null || request==null || request.expectedVersion()==null || request.expectedVersion()<0 || request.expectedVersion()==Integer.MAX_VALUE)
            throw invalid("수정할 정책과 조회 버전(0~2147483646)이 필요합니다.");
        validateFields(request.ruleReleaseId(),request.modeCode(),request.maximumSourceBytes(),request.reason());
        validateSegmentVersion(request.segmentRuleVersion());
        // 게시 경로와의 잠금 순서는 규칙 → 정책이다. rule이 퇴역하면 초안에 다른 현재 규칙을 지정해야 한다.
        validateRule(request.ruleReleaseId());
        var original=selectPolicy(policyId,true);
        if(!"DRAFT".equals(original.policyStatusCode())) throw new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_POLICY_NOT_DRAFT,HttpStatus.CONFLICT,
                "게시·퇴역 정책은 수정할 수 없습니다. 해당 버전에서 새 개정 초안을 만드세요.");
        if(!request.expectedVersion().equals(original.rowVersion())) throw conflict("조회 이후 정책이 변경됐습니다. 입력을 보존하고 최신 정책을 확인하세요.");
        var existingConfiguration=selectDetails(original).configuration();
        // 일반 초안 편집은 엔진 이관 요청이 아니다. 구 정책의 판정 의미를 묵시적으로 변경하지 않는다.
        if(!existingConfiguration.selectEngineCurrent())
            throw conflict("이 초안의 엔진 버전은 현재 편집할 수 없습니다. 현재 엔진으로 새 정책 초안을 생성하세요.");
        if(request.segmentRuleVersion()!=null && !AttachmentSegmentClassificationEngine.VERSION.equals(existingConfiguration.engineVersion()))
            throw conflict("파일 단위 엔진 정책을 구간 엔진으로 변경할 수 없습니다. 구간 규칙을 사용할 새 정책 초안을 생성하세요.");
        String segmentVersion=request.segmentRuleVersion()==null?existingConfiguration.segmentRuleVersion():request.segmentRuleVersion();
        String settings=selectJson(selectConfiguration(request.maximumSourceBytes(),existingConfiguration.engineVersion(),
                segmentVersion,request.segmentRuleVersion()==null?existingConfiguration.segmentRulesHash():AttachmentEngineContract.selectSegmentRulesHash(segmentVersion)));
        String manifest=selectJson(selectSystemBindings());
        if(dao.updatePolicyDraft(new AttachmentPolicyManagementRows.Update(policyId,request.expectedVersion(),request.modeCode(),request.ruleReleaseId(),settings,manifest))!=1)
            throw conflict("다른 작업이 먼저 정책을 변경했습니다. 입력을 보존하고 최신 버전을 확인하세요.");
        insertAudit(actor,policyId,"ATTACHMENT_POLICY_DRAFT_UPDATE",request.reason(),Map.of("previousVersion",original.rowVersion(),"newVersion",original.rowVersion()+1,
                "configurationHash",selectHash(settings+"\n"+manifest),"segmentRuleChanged",!Objects.equals(existingConfiguration.segmentRuleVersion(),segmentVersion)));
        return selectDetails(selectPolicy(policyId,false));
    }
    @Override @Transactional(timeout=20)
    public AttachmentPolicyResponses.Details insertPolicyRevision(Authentication authentication,UUID policyId,UUID key,AttachmentPolicyRequests.Revision request) {
        UUID actor=selectActor(authentication,true);
        if(policyId==null || key==null || request==null || request.expectedVersion()==null || request.expectedVersion()<0)
            throw invalid("개정 원본 정책·조회 버전·UUID 형식 Idempotency-Key가 필요합니다.");
        validateReason(request.reason());
        String hash=selectHash(selectJson(List.of("attachment-policy-revision-v1",actor,policyId,request.expectedVersion(),request.reason().strip())));
        dao.selectCreationLock(key);
        var existing=dao.selectCreationDetails(key);
        if(existing!=null) return selectSameCreation(existing,actor,hash,"REVISION");
        var locator=selectPolicy(policyId,false);
        dao.selectFamilyLock(locator.policyCode());
        var original=selectPolicy(policyId,true);
        if(!request.expectedVersion().equals(original.rowVersion())) throw conflict("개정 원본 정책이 변경됐습니다. 현재 버전을 확인한 뒤 다시 개정하세요.");
        int latest=dao.selectLatestVersion(original.policyCode());
        if(latest<original.versionNo() || latest==Integer.MAX_VALUE) throw conflict("정책 개정 번호가 올바르지 않거나 최대값에 도달했습니다.");
        // 복사 원본의 설정을 읽을 수 있어야 한다. 게시 hash/시각·검증 성공은 복사하지 않는다.
        selectDetails(original);
        UUID id=UUID.randomUUID();
        if(dao.insertPolicy(new AttachmentPolicyManagementRows.Insert(id,original.policyCode(),latest+1,original.modeCode(),original.ruleReleaseId(),
                original.settingsJson(),original.profileManifestJson(),actor,policyId,key,hash,"REVISION"))!=1) throw conflict("새 개정 초안을 저장하지 못했습니다.");
        insertAudit(actor,id,"ATTACHMENT_POLICY_DRAFT_REVISION",request.reason(),Map.of("copiedFromPolicyId",policyId,"versionNo",latest+1));
        return selectDetails(selectPolicy(id,false));
    }
    private void validateSegmentVersion(String version) {
        if(version!=null && AttachmentEngineContract.selectSegmentRulesHash(version)==null)
            throw invalid("구간 규칙은 segment-role-1.0.0 또는 segment-role-1.0.2를 선택하세요. 생략하면 기존 버전을 유지하고 신규 초안은 1.0.0을 사용합니다.");
    }
    private AttachmentPolicyResponses.Configuration selectConfiguration(long maximumBytes,String engineVersion,String segmentVersion,String segmentHash) {
        // 설치 Linux 런타임 지문은 실제 검증 단계에서만 결합한다. Windows에서 추측하거나 임의 hash를 받지 않는다.
        return new AttachmentPolicyResponses.Configuration(engineVersion,AttachmentRuntimeIdentity.EXTRACTOR_VERSION,null,maximumBytes,
                com.saneb.domain.announcementattachment.classification.AttachmentDocumentRoleClassifier.VERSION,
                com.saneb.domain.announcementattachment.classification.AttachmentDocumentRoleClassifier.RULES_HASH,
                segmentVersion,segmentHash);
    }
    private List<AttachmentPolicyResponses.Profile> selectSystemBindings() {
        if(profiles.selectProfileList().size()>1000) throw conflict("등록된 시스템 첨부 profile 수가 한도를 초과했습니다.");
        var result=new ArrayList<AttachmentPolicyResponses.Profile>();
        var seen=new HashSet<String>();
        for(var profile:profiles.selectProfileList()) {
            String provider=profile.selectProviderCode(),code=profile.selectProfileCode(),hash=profile.selectProfileHash();
            if(provider==null || !PROVIDERS.contains(provider) || code==null || !code.matches("[A-Z][A-Z0-9_]{0,79}") || hash==null || !hash.matches("[0-9a-f]{64}")
                    || !seen.add(provider+":"+code)) throw conflict("시스템 첨부 profile 등록이 중복되거나 유효하지 않습니다. 관리자가 파서를 직접 지정할 수 없습니다.");
            result.add(new AttachmentPolicyResponses.Profile(provider,code,hash));
        }
        result.sort(Comparator.comparing(AttachmentPolicyResponses.Profile::providerCode).thenComparing(AttachmentPolicyResponses.Profile::profileCode));
        return List.copyOf(result);
    }
    private AttachmentPolicyResponses.Details selectDetails(AttachmentPolicyManagementRows.Row row) {
        return selectDetails(row,true);
    }
    private AttachmentPolicyResponses.Details selectDetails(AttachmentPolicyManagementRows.Row row,boolean canEdit) {
        try {
            if(row.settingsJson()==null || row.settingsJson().length()>8192 || row.profileManifestJson()==null || row.profileManifestJson().length()>256000)
                throw conflict("저장된 정책 설정 크기가 유효하지 않습니다. 정책 데이터를 확인하세요.");
            var settings=mapper.readValue(row.settingsJson(),AttachmentPolicyResponses.Configuration.class);
            List<AttachmentPolicyResponses.Profile> bindings=mapper.readValue(row.profileManifestJson(),mapper.getTypeFactory().constructCollectionType(List.class,AttachmentPolicyResponses.Profile.class));
            if(settings==null || bindings==null || bindings.size()>1000 || bindings.stream().anyMatch(Objects::isNull))
                throw conflict("저장된 정책 설정 또는 profile 목록을 확인하세요.");
            boolean draft="DRAFT".equals(row.policyStatusCode());
            return new AttachmentPolicyResponses.Details(row.selectSummary(),settings,bindings,row.copiedFromPolicyId(),draft && canEdit,draft,row.updatedAt());
        } catch(JsonProcessingException exception) { throw conflict("저장된 정책 설정을 읽을 수 없습니다. 서버에서 정책 형식을 확인하세요."); }
    }
    private AttachmentPolicyManagementRows.Row selectPolicy(UUID id,boolean lock) {
        if(id==null) throw notFound();
        var row=dao.selectPolicyDetails(id,lock);
        if(row==null || !id.equals(row.policyId())) throw notFound();
        return row;
    }
    private AttachmentPolicyResponses.Details selectSameCreation(AttachmentPolicyManagementRows.Row row,UUID actor,String hash,String operation) {
        if(!actor.equals(row.createdBy()) || !hash.equals(row.creationRequestHash()) || !operation.equals(row.creationOperationCode()))
            throw conflict("이 멱등 키는 다른 관리자 또는 정책 요청에 사용됐습니다. 새 요청에는 새 키를 사용하세요.");
        return selectDetails(row);
    }
    private void validateRule(UUID id) {
        String status=dao.selectRuleStatus(id);
        if(status==null) throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND,HttpStatus.NOT_FOUND,"연결할 키워드 규칙을 찾을 수 없습니다.");
        if(!Set.of("DRAFT","ACTIVE").contains(status)) throw conflict("퇴역 키워드 규칙으로 초안을 저장할 수 없습니다. 현재 ACTIVE 또는 준비 중인 DRAFT 규칙을 선택하세요.");
    }
    private void validateFields(UUID rule,String mode,Long bytes,String reason) {
        if(rule==null || mode==null || !MODES.contains(mode)) throw invalid("키워드 규칙과 OFF, COLLECT_ONLY, ENFORCE 중 하나의 모드를 선택하세요.");
        if(bytes==null || bytes<1 || bytes>83886080) throw invalid("공고별 누적 다운로드 한도는 1바이트 이상, 80 MiB 이하여야 합니다.");
        validateReason(reason);
    }
    private void validateReason(String reason) {
        if(reason==null || reason.isBlank() || reason.length()>1000) throw invalid("정책 변경 사유는 공백이 아닌 1~1000자여야 합니다.");
    }
    private UUID selectActor(Authentication authentication,boolean write) {
        if(authentication==null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof AuthenticatedUserDetails actor))
            throw new ApiException(ErrorCode.AUTH_REQUIRED,HttpStatus.UNAUTHORIZED,"로그인한 운영 계정이 필요합니다.");
        var permitted=write?Set.of("ADMIN"):Set.of("ADMIN","OPERATOR","APPROVER");
        if(!actor.isEnabled() || actor.passwordResetRequired() || actor.roles().stream().noneMatch(permitted::contains))
            throw new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_ACTION_FORBIDDEN,HttpStatus.FORBIDDEN,
                    write?"첨부 정책 변경은 비밀번호 변경을 완료한 활성 ADMIN만 할 수 있습니다.":"첨부 정책은 비밀번호 변경을 완료한 활성 ADMIN, OPERATOR, APPROVER만 조회할 수 있습니다.");
        return actor.userId();
    }
    private void insertAudit(UUID actor,UUID id,String action,String reason,Map<String,Object> extra) {
        var metadata=new TreeMap<String,Object>(extra);metadata.put("reasonHash",selectHash(reason.strip()));
        audit.insertAuditLog(new AnnouncementSourceAuditLogCommand(actor,action,"ANNOUNCEMENT_ATTACHMENT_POLICY",id,"SUCCESS",selectJson(metadata)));
    }
    private String selectJson(Object value) {
        try { return mapper.writeValueAsString(value); } catch(JsonProcessingException exception) { throw invalid("정책 설정을 직렬화할 수 없습니다."); }
    }
    private String selectHash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch(NoSuchAlgorithmException exception) { throw new IllegalStateException("SHA-256을 사용할 수 없습니다."); }
    }
    private ApiException invalid(String message) { return new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_POLICY_INVALID,HttpStatus.BAD_REQUEST,message); }
    private ApiException conflict(String message) { return new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_VERSION_CONFLICT,HttpStatus.CONFLICT,message); }
    private ApiException notFound() { return new ApiException(ErrorCode.RESOURCE_NOT_FOUND,HttpStatus.NOT_FOUND,"첨부 정책을 찾을 수 없습니다."); }
}
