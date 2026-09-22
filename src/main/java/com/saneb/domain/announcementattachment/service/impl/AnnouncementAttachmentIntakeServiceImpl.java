package com.saneb.domain.announcementattachment.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.common.error.ApiException;
import com.saneb.common.error.ErrorCode;
import com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentIntakeDao;
import com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentJobDao;
import com.saneb.domain.announcementattachment.discovery.AttachmentDiscoveryProfile;
import com.saneb.domain.announcementattachment.discovery.AttachmentDiscoveryProfileRegistry;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentIntakeService;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentJobService;
import com.saneb.domain.announcementattachment.vo.AttachmentCollectionPlan;
import com.saneb.domain.announcementattachment.vo.AttachmentExecutionSnapshot;
import com.saneb.domain.announcementattachment.vo.AttachmentJobReservation;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 수집 항목 transaction에서 예약만 수행한다. HTTP/디스크/추출은 worker가 담당한다. */
@Service
public class AnnouncementAttachmentIntakeServiceImpl implements AnnouncementAttachmentIntakeService {
    private final AnnouncementAttachmentIntakeDao intake;
    private final AnnouncementAttachmentJobDao jobs;
    private final AnnouncementAttachmentJobService jobService;
    private final AttachmentDiscoveryProfileRegistry profiles;
    private final ObjectMapper mapper;
    private final boolean enabled;
    public AnnouncementAttachmentIntakeServiceImpl(AnnouncementAttachmentIntakeDao intake,AnnouncementAttachmentJobDao jobs,
            AnnouncementAttachmentJobService jobService,AttachmentDiscoveryProfileRegistry profiles,ObjectMapper mapper,
            @Value("${saneb.announcement-attachment.worker.enabled:false}") boolean enabled) {
        this.intake=intake; this.jobs=jobs; this.jobService=jobService; this.profiles=profiles; this.mapper=mapper; this.enabled=enabled;
    }
    @Override @Transactional
    public AttachmentCollectionPlan saveCollectionPlan(UUID runId,UUID ruleReleaseId) {
        if (!enabled) return null;
        if (ruleReleaseId==null) {
            validateNoUnmatchedEnforcePolicy(null);
            return null;
        }
        var existing=intake.selectCollectionPlanDetails(runId);
        if (existing!=null) {
            if (!existing.ruleReleaseId().equals(ruleReleaseId)) throw new IllegalStateException("수집 실행의 고정 기본 규칙이 변경되었습니다.");
            if ("NO_POLICY".equals(existing.statusCode())) validateNoUnmatchedEnforcePolicy(ruleReleaseId);
            return existing;
        }
        UUID policyId=intake.selectActivePolicyId(ruleReleaseId);
        var policy=policyId==null ? null : jobs.selectPolicyDetails(policyId);
        if (policy==null) validateNoUnmatchedEnforcePolicy(ruleReleaseId);
        var plan=new AttachmentCollectionPlan(runId,ruleReleaseId,policyId,policy==null ? "NO_POLICY" : "OFF".equals(policy.modeCode()) ? "OFF" : "FROZEN");
        intake.insertCollectionPlan(plan);
        var saved=intake.selectCollectionPlanDetails(runId);
        if (saved==null || !saved.ruleReleaseId().equals(ruleReleaseId)) throw new IllegalStateException("수집 실행의 첨부 정책을 고정하지 못했습니다.");
        return saved;
    }
    private void validateNoUnmatchedEnforcePolicy(UUID ruleReleaseId) {
        if (intake.selectUnmatchedEnforcePolicyId(ruleReleaseId)!=null)
            throw new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_RULE_POLICY_MISMATCH,HttpStatus.CONFLICT,
                    "현재 키워드 규칙과 일치하는 첨부 정책이 없어 수집을 중지했습니다. 관리자에서 현재 ACTIVE 키워드 규칙의 첨부 정책을 검증·게시한 뒤 새 수집을 실행하세요.");
    }
    @Override @Transactional(propagation=Propagation.MANDATORY)
    public void saveCollectedSource(UUID sourceId,AttachmentCollectionPlan plan,boolean newSource) {
        if (!enabled || plan==null || !"FROZEN".equals(plan.statusCode())) return;
        if (!plan.equals(intake.selectCollectionPlanDetails(plan.runId()))) throw new IllegalStateException("첨부 수집 계획이 저장된 실행과 다릅니다.");
        var source=jobs.selectSourceContextDetailsForUpdate(sourceId);
        if (source==null || !"PRODUCTION".equals(source.dataPurposeCode()) || "EXCLUDED".equals(source.semanticStatusCode())
                || source.baseEvaluationId()==null || source.titleStageCode()==null
                || !Set.of("GROUP_A_MATCHED","COMBINATION_MATCHED").contains(source.titleStageCode())) return;
        if (intake.selectProtectedLinkExists(sourceId)) { intake.updateSourceIntakeStatus(sourceId,"PROTECTED_LINK"); return; }
        if (!Objects.equals(source.ruleReleaseId(),plan.ruleReleaseId())) { intake.updateSourceIntakeStatus(sourceId,"BASE_RECLASSIFICATION_REQUIRED"); return; }
        if (Boolean.TRUE.equals(source.attachmentReviewRequired()) && !Objects.equals(source.attachmentPolicyId(),plan.policyId())) {
            intake.updateSourceIntakeStatus(sourceId,"POLICY_BINDING_CHANGED"); return;
        }
        UUID key=UUID.nameUUIDFromBytes(("saneb-attachment-intake-v1:"+plan.runId()+":"+sourceId+":"+source.baseEvaluationId()+":"+plan.policyId())
                .getBytes(StandardCharsets.UTF_8));
        if (jobs.selectIdempotentJobDetails(key)!=null || intake.selectActiveJobId(sourceId)!=null) {
            intake.updateSourceIntakeStatus(sourceId,"QUEUED"); return;
        }
        if (intake.selectRecentCheckExists(sourceId)) {
            intake.updateSourceIntakeStatus(sourceId,"RECHECK_NOT_DUE"); return;
        }
        var policy=jobs.selectPolicyDetails(plan.policyId());
        var locator=intake.selectSourceLocatorDetails(sourceId);
        if (policy==null || locator==null) { intake.updateSourceIntakeStatus(sourceId,"PROFILE_REQUIRED"); return; }
        try {
            var manifest=mapper.readTree(policy.profileManifestJson());
            var matches=new ArrayList<AttachmentDiscoveryProfile>();
            for (var item:manifest) {
                var selected=profiles.selectProfileDetails(locator.providerCode(),item.path("profileCode").asText(),item.path("profileHash").asText());
                if (selected.isEmpty() || !locator.providerCode().equals(item.path("providerCode").asText())) continue;
                try { selected.get().selectDetailUri(locator.selectDiscoverySource()); matches.add(selected.get()); }
                catch (IllegalArgumentException ignored) { /* 해당 source의 시스템 profile이 아니다. */ }
            }
            if (matches.size()!=1) { intake.updateSourceIntakeStatus(sourceId,"PROFILE_REQUIRED"); return; }
            var profile=matches.getFirst();
            var settings=mapper.readTree(policy.settingsJson());
            var execution=new AttachmentExecutionSnapshot(profile.selectProfileCode(),profile.selectProfileHash(),
                    settings.path("engineVersion").asText(),settings.path("extractorVersion").asText(),settings.path("extractorConfigHash").asText(),
                    settings.path("roleRuleVersion").asText(null),settings.path("roleRulesHash").asText(null),
                    settings.path("segmentRuleVersion").asText(null),settings.path("segmentRulesHash").asText(null));
            if(!execution.selectRoleRulesCurrent() || !execution.selectEngineCurrent()) { intake.updateSourceIntakeStatus(sourceId,"PROFILE_REQUIRED"); return; }
            jobService.insertAttachmentJob(new AttachmentJobReservation(sourceId,policy.policyId(),source.baseEvaluationId(),
                    source.sourceVersion(),source.attachmentVersion(),key,execution,plan.runId(),newSource && "ENFORCE".equals(policy.modeCode())));
            intake.updateSourceIntakeStatus(sourceId,"QUEUED");
        } catch (JsonProcessingException exception) { throw new IllegalStateException("게시된 첨부 정책의 실행 계약을 읽을 수 없습니다."); }
    }
}
