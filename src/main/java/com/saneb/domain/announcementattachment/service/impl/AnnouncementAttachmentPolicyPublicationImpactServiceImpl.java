package com.saneb.domain.announcementattachment.service.impl;

import com.fasterxml.jackson.databind.*;
import com.saneb.common.error.*;
import com.saneb.domain.announcementattachment.dao.*;
import com.saneb.domain.announcementattachment.dto.*;
import com.saneb.domain.announcementattachment.dto.AttachmentPolicyPublicationImpact.*;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentPolicyPublicationImpactService;
import com.saneb.domain.announcementattachment.vo.*;
import com.saneb.domain.auth.vo.AuthenticatedUserDetails;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** 운영 게시 전 관측값만 제공한다. QA 상세 재실행/검증과 게시 transaction을 대신하지 않는다. */
@Service
public class AnnouncementAttachmentPolicyPublicationImpactServiceImpl implements AnnouncementAttachmentPolicyPublicationImpactService {
    private static final List<String> STEPS=List.of("CLASSIFICATION_GOLDEN","INSTALLED_RUNTIME","PROVIDER_PROFILES","WORKER_DB_RECOVERY");
    private final AnnouncementAttachmentPolicyDao policies;
    private final AnnouncementAttachmentPolicyValidationDao validations;
    private final AnnouncementAttachmentPolicyPublicationImpactDao impact;
    private final ObjectMapper mapper;
    public AnnouncementAttachmentPolicyPublicationImpactServiceImpl(AnnouncementAttachmentPolicyDao policies,AnnouncementAttachmentPolicyValidationDao validations,
            AnnouncementAttachmentPolicyPublicationImpactDao impact,ObjectMapper mapper) {
        this.policies=policies;this.validations=validations;this.impact=impact;this.mapper=mapper.copy().enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
    }
    @Override @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ,timeout=15)
    public AttachmentPolicyPublicationImpact selectImpactDetails(Authentication authentication,UUID policyId) {
        validateActor(authentication);
        if(policyId==null)throw new ApiException(ErrorCode.VALIDATION_FAILED,HttpStatus.BAD_REQUEST,"조회할 정책 ID가 필요합니다.");
        var policy=policies.selectPolicyDetails(policyId,false);
        if(policy==null)throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND,HttpStatus.NOT_FOUND,"해당 첨부 정책을 찾을 수 없습니다.");
        validateSummary(policy.selectSummary());if(!policyId.equals(policy.policyId()))throw conflict();
        var search=new AttachmentPolicyManagementRows.Search("ACTIVE",policy.ruleReleaseId(),2,0);
        long activeCount=policies.selectPolicyCount(search);var activeRows=policies.selectPolicyList(search);
        if(activeCount<0 || activeCount>1 || activeRows==null || activeRows.size()!=activeCount)throw conflict();
        var active=activeRows.isEmpty()?null:activeRows.getFirst();
        if(active!=null){validateSummary(active);if(!"ACTIVE".equals(active.policyStatusCode()) || !policy.ruleReleaseId().equals(active.ruleReleaseId()))throw conflict();}
        var matching=impact.selectCounts(policy.ruleReleaseId());var all=impact.selectCounts(null);validateCounts(matching);validateCounts(all);
        var part=values(matching);var total=values(all);for(int i=0;i<part.size();i++)if(part.get(i)>total.get(i))throw conflict();
        Long maximum=selectMaximum(policy);
        var blockers=new ArrayList<String>();
        if(!"DRAFT".equals(policy.policyStatusCode()))blockers.add("POLICY_NOT_DRAFT");
        if(!"ACTIVE".equals(policy.ruleReleaseStatusCode()))blockers.add("KEYWORD_RULE_NOT_ACTIVE");
        var latest=selectQa(policy,blockers);
        // 4개의 PASSED 표시는 코드/설치/전체 profile의 현재성 재검증과 게시 승인이 아니다.
        blockers.add("PUBLICATION_REVALIDATION_REQUIRED");
        boolean activeRule="ACTIVE".equals(policy.ruleReleaseStatusCode());
        boolean stops=activeRule && "OFF".equals(policy.modeCode());
        boolean lifts=activeRule && !"OFF".equals(policy.modeCode()) && active!=null && "OFF".equals(active.modeCode());
        var observed=new TreeMap<String,Object>();observed.put("schemaVersion",1);observed.put("policy",policy.selectSummary());observed.put("activePolicy",active);
        observed.put("matchingRule",matching);observed.put("allRules",all);observed.put("latestQa",latest);observed.put("blockingReasonCodes",blockers);
        observed.put("settingsHash",hash(policy.settingsJson()));observed.put("profilesHash",hash(policy.profileManifestJson()));
        return new AttachmentPolicyPublicationImpact(policy.selectSummary(),active,matching,all,maximum,stops,lifts,latest,blockers,true,hash(observed),OffsetDateTime.now(),0);
    }
    private Qa selectQa(AttachmentPolicyManagementRows.Row policy,List<String> blockers) {
        var search=new AttachmentPolicyValidationRows.Search(policy.policyId(),1,0);
        long count=validations.selectRunCount(search);var rows=validations.selectRunList(search);
        if(count<0 || rows==null || rows.size()!=Math.min(count,1))throw conflict();
        if(rows.isEmpty()){blockers.add("QA_NOT_REQUESTED");return null;}
        var run=rows.getFirst();
        if(run==null || run.runId()==null || run.ruleReleaseId()==null || !policy.policyId().equals(run.policyId()) || !nonnegative(run.policyVersion()) || !nonnegative(run.ruleVersion()) || !nonnegative(run.rowVersion())
                || run.inputVersionsCurrent()==null || !digest(run.snapshotHash()) || !Set.of("PENDING","RUNNING","CANCEL_REQUESTED","CANCELLED","INCOMPLETE","CONFLICT","FAILED","VERIFIED").contains(run.statusCode()==null?"":run.statusCode()))throw conflict();
        boolean current=Boolean.TRUE.equals(run.inputVersionsCurrent()) && policy.rowVersion().equals(run.policyVersion()) && policy.ruleReleaseId().equals(run.ruleReleaseId());
        if(!current)blockers.add("QA_INPUT_VERSIONS_CHANGED");if(!"VERIFIED".equals(run.statusCode()))blockers.add("QA_NOT_VERIFIED");
        var saved=validations.selectStepList(run.runId());if(saved==null || saved.size()>4)throw conflict();
        var known=new HashMap<String,AttachmentPolicyValidationRows.Step>();
        for(var s:saved)if(s==null || !run.runId().equals(s.runId()) || s.stepCode()==null || !STEPS.contains(s.stepCode()) || known.put(s.stepCode(),s)!=null
                || !Set.of("PASSED","FAILED","MISSING").contains(s.statusCode()==null?"":s.statusCode()) || !digest(s.evidenceHash()))throw conflict();
        var steps=STEPS.stream().map(code->{var s=known.get(code);return new Step(code,s==null?"NOT_RUN":s.statusCode(),s==null?null:s.evidenceHash());}).toList();
        if(steps.stream().anyMatch(s->!"PASSED".equals(s.statusCode())))blockers.add("QA_REQUIRED_STEPS_NOT_PASSED");
        return new Qa(run.runId(),run.statusCode(),run.rowVersion(),run.policyVersion(),run.ruleVersion(),current,run.snapshotHash(),steps,run.completedAt());
    }
    private Long selectMaximum(AttachmentPolicyManagementRows.Row row) {
        try {
            if(row.settingsJson()==null || row.settingsJson().length()>8192 || row.profileManifestJson()==null || row.profileManifestJson().length()>256000)throw conflict();
            var settings=mapper.readTree(row.settingsJson());var manifest=mapper.readTree(row.profileManifestJson());
            var bytes=settings==null?null:settings.get("maximumSourceBytes");
            if(bytes==null || !bytes.isIntegralNumber() || !bytes.canConvertToLong() || bytes.longValue()<1 || bytes.longValue()>83886080 || manifest==null || !manifest.isArray())throw conflict();
            return bytes.longValue();
        }catch(Exception exception){throw conflict();}
    }
    private void validateSummary(AttachmentPolicyResponses.Summary p) {
        if(p==null || p.policyId()==null || p.ruleReleaseId()==null || !nonnegative(p.rowVersion()) || !nonnegative(p.versionNo()) || p.versionNo()<1
                || !Set.of("OFF","COLLECT_ONLY","ENFORCE").contains(p.modeCode()==null?"":p.modeCode())
                || !Set.of("DRAFT","ACTIVE","RETIRED").contains(p.policyStatusCode()==null?"":p.policyStatusCode())
                || !Set.of("DRAFT","ACTIVE","RETIRED").contains(p.ruleReleaseStatusCode()==null?"":p.ruleReleaseStatusCode())
                || (!"DRAFT".equals(p.policyStatusCode()) && (!digest(p.policyHash()) || p.publishedAt()==null)))throw conflict();
    }
    private void validateCounts(Counts c) {
        if(c==null || values(c).stream().anyMatch(v->v==null || v<0 || v>9007199254740991L)
                || c.reviewRequiredSourceCount()>c.boundSourceCount() || c.effectiveAttachmentSourceCount()>c.boundSourceCount()
                || c.linkedSourceCount()>c.boundSourceCount() || c.runningCollectionJobCount()>c.frozenCollectionJobCount())throw conflict();
    }
    private List<Long> values(Counts c){return Arrays.asList(c.boundSourceCount(),c.reviewRequiredSourceCount(),c.effectiveAttachmentSourceCount(),c.linkedSourceCount(),c.frozenCollectionJobCount(),c.runningCollectionJobCount(),c.applicationPendingJobCount(),c.rollbackPendingJobCount(),c.frozenCollectionPlanCount());}
    private boolean nonnegative(Integer n){return n!=null && n>=0;}
    private boolean digest(String s){return s!=null && s.matches("[0-9a-f]{64}");}
    private String hash(Object value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(mapper.writeValueAsString(value).getBytes(StandardCharsets.UTF_8)));}catch(Exception exception){throw conflict();}}
    private void validateActor(Authentication auth) {
        if(auth==null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof AuthenticatedUserDetails actor))throw new ApiException(ErrorCode.AUTH_REQUIRED,HttpStatus.UNAUTHORIZED,"로그인한 운영 계정이 필요합니다.");
        if(!actor.isEnabled() || actor.passwordResetRequired() || actor.roles().stream().noneMatch(Set.of("ADMIN","OPERATOR","APPROVER")::contains))
            throw new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_ACTION_FORBIDDEN,HttpStatus.FORBIDDEN,"정책 게시 영향은 비밀번호 변경을 완료한 활성 ADMIN·OPERATOR·APPROVER만 조회할 수 있습니다.");
    }
    private ApiException conflict(){return new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_VERSION_CONFLICT,HttpStatus.CONFLICT,"게시 영향의 정책·QA 소속·버전·건수 또는 설정이 일치하지 않습니다. 부분 결과를 승인 근거로 사용하지 말고 최신 정책을 다시 확인하세요.");}
}
