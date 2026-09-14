package com.saneb.domain.announcementattachment.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.common.error.*;
import com.saneb.common.response.PageResponse;
import com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentPolicyDao;
import com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentPolicyPublicationScopeDao;
import com.saneb.domain.announcementattachment.dto.AttachmentPolicyPublicationScope.*;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentPolicyPublicationScopeService;
import com.saneb.domain.announcementattachment.vo.AttachmentPolicyPublicationScopeRows.*;
import com.saneb.domain.announcementsource.dao.AnnouncementSourceDao;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceAuditLogCommand;
import com.saneb.domain.auth.vo.AuthenticatedUserDetails;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.*;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.TransactionTemplate;

/** 전체 준비 범위를 불변 원장으로 저장한다. 승인·정책 게시·외부 요청·기존 판정 변경은 수행하지 않는다. */
@Service
public class AnnouncementAttachmentPolicyPublicationScopeServiceImpl implements AnnouncementAttachmentPolicyPublicationScopeService {
    private final AnnouncementAttachmentPolicyPublicationScopeDao dao;
    private final AnnouncementAttachmentPolicyDao policies;
    private final AnnouncementSourceDao audit;
    private final ObjectMapper mapper;
    private final TransactionTemplate read,write;
    public AnnouncementAttachmentPolicyPublicationScopeServiceImpl(AnnouncementAttachmentPolicyPublicationScopeDao dao,
            AnnouncementAttachmentPolicyDao policies,AnnouncementSourceDao audit,ObjectMapper mapper,PlatformTransactionManager transactions) {
        this.dao=dao;this.policies=policies;this.audit=audit;this.mapper=mapper;
        read=new TransactionTemplate(transactions);read.setReadOnly(true);read.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);read.setTimeout(15);
        write=new TransactionTemplate(transactions);write.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);write.setTimeout(20);
    }
    @Override @Transactional(propagation=Propagation.NOT_SUPPORTED)
    public Details insertScope(Authentication authentication,UUID policyId,UUID key,Prepare request) {
        UUID actor=selectActor(authentication,true);
        if(policyId==null || key==null || request==null || request.expectedVersion()==null || request.expectedVersion()<0
                || request.reason()==null || request.reason().isBlank() || request.reason().length()>1000)
            throw invalid("정책 UUID·UUID 멱등 키·0 이상의 조회 버전과 공백이 아닌 1~1000자 사유를 입력하세요.");
        String reasonHash=hash(request.reason().strip());
        String requestHash=hash(json(List.of("attachment-policy-publication-scope-request-v1",actor,policyId,request.expectedVersion(),reasonHash)));
        var previous=read.execute(tx->dao.selectRequestDetails(key));
        if(previous!=null)return read.execute(tx->selectSame(previous,actor,policyId,requestHash));
        try {
            return write.execute(tx->{
                var policy=policies.selectPolicyDetails(policyId,false);
                if(policy==null)throw notFound();
                if(!policyId.equals(policy.policyId()) || !"DRAFT".equals(policy.policyStatusCode())
                        || !"ACTIVE".equals(policy.ruleReleaseStatusCode()) || !request.expectedVersion().equals(policy.rowVersion()))
                    throw conflict("현재 ACTIVE 키워드 규칙을 참조하는 최신 DRAFT 정책에서 준비 범위를 고정하세요.");
                UUID id=UUID.randomUUID();
                if(dao.insertScope(new Insert(id,policyId,request.expectedVersion(),actor,key,requestHash,reasonHash))!=1)
                    throw conflict("조회한 정책·규칙 버전이 바뀌었습니다. 최신 초안을 다시 확인하세요.");
                long members=dao.insertScopeMembers(id,policyId);
                if(members<1 || dao.updateScopeSealed(id)!=1)throw conflict("게시 준비 범위 전체를 봉인하지 못했습니다. 부분 범위는 사용할 수 없습니다.");
                var saved=selectStored(policyId,id);
                if(saved.itemCount()!=members)throw conflict("고정한 게시 준비 항목 수가 실제 전체 범위와 다릅니다.");
                audit.insertAuditLog(new AnnouncementSourceAuditLogCommand(actor,"ATTACHMENT_POLICY_SCOPE_PREPARE","ANNOUNCEMENT_ATTACHMENT_POLICY",
                        policyId,"SUCCESS",json(Map.of("scopeId",id,"scopeHash",saved.scopeHash(),"itemCount",members,"reasonHash",reasonHash,"isApproval",false))));
                return selectDetails(saved);
            });
        } catch(DataIntegrityViolationException exception) {
            // 같은 키의 동시 INSERT 패자는 abort된 transaction 밖에서 승자의 불변 원장을 읽는다.
            // 새로운 키로 다시 실행하거나 일부 범위를 남기지 않는다. 그 외 SQL 제약 오류 원문은 노출하지 않는다.
            var winner=read.execute(tx->dao.selectRequestDetails(key));
            if(winner!=null)return read.execute(tx->selectSame(winner,actor,policyId,requestHash));
            throw conflict("게시 준비 범위 또는 입력이 저장 중 변경됐습니다. 원래 요청 키를 유지하고 최신 정책·범위를 확인하세요.");
        }
    }
    @Override @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ,timeout=15)
    public Details selectScopeDetails(Authentication actor,UUID policyId,UUID scopeId) {
        selectActor(actor,false);return selectDetails(selectStored(policyId,scopeId));
    }
    @Override @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ,timeout=15)
    public PageResponse<Summary> selectScopeList(Authentication actor,UUID policyId,int page,int size) {
        selectActor(actor,false);var search=selectSearch(policyId,null,page,size);
        if(policies.selectPolicyDetails(policyId,false)==null)throw notFound();
        var rows=dao.selectScopeList(search);rows.forEach(row->{
            validateSummary(row);
            if(!policyId.equals(row.policyId()))throw conflict("조회한 정책과 게시 준비 원장의 소속이 일치하지 않습니다.");
        });
        return PageResponse.of(rows,page,size,dao.selectScopeCount(search));
    }
    @Override @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ,timeout=15)
    public PageResponse<Item> selectItemList(Authentication actor,UUID policyId,UUID scopeId,int page,int size) {
        selectActor(actor,false);var search=selectSearch(policyId,scopeId,page,size);var scope=selectStored(policyId,scopeId);
        var items=dao.selectItemList(search);
        if(items.stream().anyMatch(i->i==null || i.entityId()==null || i.entityTypeCode()==null || !Set.of("POLICY","SOURCE","JOB","COLLECTION_PLAN","COLLECTOR").contains(i.entityTypeCode())
                || !digest(i.stateHash())))throw conflict("저장된 게시 준비 항목의 식별자 또는 상태 지문을 확인할 수 없습니다.");
        return PageResponse.of(items,page,size,scope.itemCount());
    }
    private Search selectSearch(UUID policyId,UUID scopeId,int page,int size) {
        if(policyId==null || page<1 || size<1 || size>100 || ((long)page-1)*size>Integer.MAX_VALUE)
            throw invalid("정책 UUID와 1 이상의 페이지, 1~100의 목록 크기를 지정하세요.");
        return new Search(policyId,scopeId,size,(page-1)*size);
    }
    private Details selectSame(Request row,UUID actor,UUID policyId,String hash) {
        if(!actor.equals(row.requestedBy()) || !policyId.equals(row.policyId()) || !hash.equals(row.requestHash()))
            throw conflict("이 멱등 키는 다른 관리자·정책·입력의 게시 준비에 사용됐습니다. 새 작업에만 새 키를 사용하세요.");
        return selectDetails(selectStored(policyId,row.scopeId()));
    }
    private Summary selectStored(UUID policyId,UUID scopeId) {
        var row=policyId==null || scopeId==null?null:dao.selectScopeDetails(policyId,scopeId);
        if(row==null || !policyId.equals(row.policyId()) || !scopeId.equals(row.scopeId()))throw notFound();
        validateSummary(row);return row;
    }
    private void validateSummary(Summary s) {
        if(s==null || s.scopeId()==null || s.policyId()==null || s.ruleReleaseId()==null || s.policyVersion()==null || s.policyVersion()<0
                || s.ruleVersion()==null || s.ruleVersion()<0 || s.modeCode()==null || !Set.of("OFF","COLLECT_ONLY","ENFORCE").contains(s.modeCode())
                || s.itemCount()==null || s.itemCount()<1 || s.itemCount()>9007199254740991L || !digest(s.scopeHash())
                || (s.qaRunId()==null)!=(s.qaSnapshotHash()==null) || s.qaSnapshotHash()!=null&&!digest(s.qaSnapshotHash())
                || s.createdAt()==null || s.expiresAt()==null || !s.expiresAt().isAfter(s.createdAt())
                || s.expiresAt().isAfter(s.createdAt().plusMinutes(15)))throw conflict("저장된 게시 준비 범위의 버전·건수·지문·유효기간이 유효하지 않습니다.");
    }
    private Details selectDetails(Summary row) {
        boolean expired=!row.expiresAt().isAfter(OffsetDateTime.now());
        return new Details(row,expired,!expired&&dao.selectScopeCurrent(row.scopeId()),false,true,0);
    }
    private UUID selectActor(Authentication authentication,boolean writeAccess) {
        if(authentication==null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof AuthenticatedUserDetails actor))
            throw new ApiException(ErrorCode.AUTH_REQUIRED,HttpStatus.UNAUTHORIZED,"로그인한 운영 계정이 필요합니다.");
        var roles=writeAccess?Set.of("ADMIN"):Set.of("ADMIN","OPERATOR","APPROVER");
        if(!actor.isEnabled() || actor.passwordResetRequired() || actor.roles().stream().noneMatch(roles::contains))
            throw new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_ACTION_FORBIDDEN,HttpStatus.FORBIDDEN,
                    writeAccess?"게시 준비 범위 고정은 비밀번호 변경을 완료한 활성 ADMIN만 할 수 있습니다.":"게시 준비 범위는 활성 ADMIN·OPERATOR·APPROVER만 조회할 수 있습니다.");
        return actor.userId();
    }
    private boolean digest(String value){return value!=null&&value.matches("[0-9a-f]{64}");}
    private String json(Object value){try{return mapper.writeValueAsString(value);}catch(Exception e){throw invalid("게시 준비 metadata를 직렬화할 수 없습니다.");}}
    private String hash(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw invalid("게시 준비 지문을 계산할 수 없습니다.");}}
    private ApiException invalid(String message){return new ApiException(ErrorCode.VALIDATION_FAILED,HttpStatus.BAD_REQUEST,message);}
    private ApiException conflict(String message){return new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_VERSION_CONFLICT,HttpStatus.CONFLICT,message);}
    private ApiException notFound(){return new ApiException(ErrorCode.RESOURCE_NOT_FOUND,HttpStatus.NOT_FOUND,"선택 정책의 게시 준비 범위를 찾을 수 없습니다.");}
}
