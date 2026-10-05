package com.saneb.domain.announcementsource.service.impl;

import com.saneb.common.error.ApiException;
import com.saneb.common.error.ErrorCode;
import com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentJobDao;
import com.saneb.domain.announcementattachment.vo.AttachmentSourceContextRow;
import com.saneb.domain.announcementsource.classification.*;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.*;
import com.saneb.domain.announcementsource.dao.*;
import com.saneb.domain.announcementsource.dto.SourceBodyRefreshResponses;
import com.saneb.domain.announcementsource.provider.AnnouncementSourceProviderItem;
import com.saneb.domain.announcementsource.provider.content.*;
import com.saneb.domain.announcementsource.service.*;
import com.saneb.domain.announcementsource.vo.*;
import com.saneb.domain.auth.vo.AuthenticatedUserDetails;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 단건 복구: 외부 조회는 preview만, apply는 고정 본문으로 새 버전만 추가한다. */
@Service
public class AnnouncementSourceBodyRefreshServiceImpl implements AnnouncementSourceBodyRefreshService {
    static final String EXTRACTOR_VERSION="NOTICE_BODY_2";
    private final AnnouncementSourceBodyRefreshDao dao;
    private final AnnouncementAttachmentJobDao jobs;
    private final AnnouncementSourceDao sources;
    private final AnnouncementSourceRuleReleaseService rules;
    private final AnnouncementSourceClassificationPersistenceService persistence;
    private final List<ProviderContentClient> clients;
    private final AnnouncementSourceClassificationEngine engine=new AnnouncementSourceClassificationEngine();

    public AnnouncementSourceBodyRefreshServiceImpl(AnnouncementSourceBodyRefreshDao dao,AnnouncementAttachmentJobDao jobs,
            AnnouncementSourceDao sources,AnnouncementSourceRuleReleaseService rules,
            AnnouncementSourceClassificationPersistenceService persistence,List<ProviderContentClient> clients) {
        this.dao=dao;this.jobs=jobs;this.sources=sources;this.rules=rules;this.persistence=persistence;this.clients=List.copyOf(clients);
    }

    @Override @Transactional(timeout=60)
    public SourceBodyRefreshResponses.Preview insertPreview(Authentication auth,UUID sourceId) {
        UUID actor=selectActor(auth);
        var context=jobs.selectSourceContextDetails(sourceId);
        validateContext(context);
        var boundary=selectBoundary(sourceId);
        var source=sources.selectSourceDetails(sourceId);
        if(source==null)throw conflict("현재 공고를 찾을 수 없습니다. 목록에서 다시 선택하세요.");
        var ruleSet=rules.selectActiveRuleSet(context.ruleReleaseId());
        // 현재 규칙의 제목 제외를 본문 복구 경로로 우회하지 않는다.
        var titleResult=engine.selectDecision(new AnnouncementSourceClassificationInput(source.providerCode(),source.title(),null,
                source.agencyName(),List.of(),BodySourceCode.NONE,BodyAvailabilityCode.UNAVAILABLE),ruleSet);
        if(titleResult.semanticStatusCode()==SemanticStatusCode.EXCLUDED)throw conflict("현재 규칙에서 제목 제외 대상입니다. 본문 복구를 실행하지 않습니다.");
        var client=clients.stream().filter(c->source.providerCode().equals(c.selectProviderCode())&&c.isEnabled()).findFirst()
                .orElseThrow(()->conflict("지자체 본문 수집 기능이 비활성 상태입니다. 시스템 설정을 확인하세요."));
        var fetched=client.selectContent(new ProviderContentRequest(source.providerCode(),boundary.registeredSourceId(),
                boundary.registeredSourceUrl(),source.sourceUrl()));
        if(fetched.statusCode()!=ProviderContentCodes.StatusCode.AVAILABLE || fetched.bodyText()==null || fetched.bodyText().isBlank())
            throw conflict("본문 정제에 실패했습니다 ("+fetched.failureCode()+"). 기존 본문과 판정은 변경하지 않았습니다.");
        String body=fetched.bodyText();
        if(body.length()>2_000_000)throw conflict("복구 본문이 2,000,000자를 초과했습니다. 수집 영역을 확인하세요.");
        var result=selectDecision(source,body,ruleSet);
        var preview=new SourceBodyRefreshRows.Preview(UUID.randomUUID(),sourceId,actor,context.baseEvaluationId(),context.contentVersionId(),
                context.ruleReleaseId(),context.sourceVersion(),context.attachmentVersion(),body,selectHash(body),EXTRACTOR_VERSION,
                OffsetDateTime.now().plusMinutes(30),null);
        requireOne(dao.insertPreview(preview));
        saveAudit(actor,"SOURCE_BODY_REFRESH_PREVIEW",sourceId,preview.id());
        return new SourceBodyRefreshResponses.Preview(preview.id(),sourceId,source.bodyText(),body,source.semanticStatusCode(),
                result.semanticStatusCode().name(),result.reasonCode().name(),result.targetCategoryCodes().stream().map(Enum::name).toList(),
                result.supportTypeCodes().stream().map(Enum::name).toList(),preview.expiresAt(),preview.bodyHash(),!Objects.equals(source.bodyText(),body));
    }

    @Override @Transactional(timeout=20)
    public SourceBodyRefreshResponses.Applied savePreview(Authentication auth,UUID sourceId,UUID previewId) {
        UUID actor=selectActor(auth);
        var context=jobs.selectSourceContextDetailsForUpdate(sourceId);
        var preview=dao.selectPreviewDetailsForUpdate(sourceId,previewId);
        if(preview==null || !actor.equals(preview.requestedBy()))throw conflict("이 계정이 생성한 해당 공고의 본문 미리보기가 아닙니다.");
        if(preview.appliedEvaluationId()!=null)return new SourceBodyRefreshResponses.Applied(preview.id(),sourceId,preview.appliedEvaluationId(),"APPLIED");
        validateContext(context);
        selectBoundary(sourceId);
        validatePreview(preview,context);
        var source=sources.selectSourceDetails(sourceId);
        if(source==null)throw conflict("복구할 현재 공고를 찾을 수 없습니다.");
        var result=selectDecision(source,preview.bodyText(),rules.selectActiveRuleSet(preview.ruleReleaseId()));
        if(result.semanticStatusCode()==SemanticStatusCode.EXCLUDED)throw conflict("현재 규칙에서 제목 제외 대상입니다. 본문 복구 적용을 중단했습니다.");
        if(Objects.equals(source.bodyText(),preview.bodyText()))throw conflict("본문 변경이 없습니다. 새 본문 버전을 만들 필요가 없습니다.");
        var item=new AnnouncementSourceProviderItem(source.providerCode(),source.providerNoticeId(),source.title(),source.agencyName(),
                source.applicationStartDate(),source.applicationEndDate(),source.postedAt(),source.modifiedAt(),source.sourceUrl(),
                preview.bodyText(),source.inquiryText(),source.applicationMethodText(),source.sourceCompletenessCode(),source.missingFieldsJson(),
                null,source.rawHash(),List.of(),null).withSemanticDecision(result.semanticStatusCode().name(),result.reasonCode().name(),null);
        requireOne(dao.updateBody(sourceId,preview.bodyText(),preview.sourceVersion()));
        UUID evaluation=persistence.saveChangedContentEvaluation(sourceId,null,preview.ruleReleaseId(),item,result,"REVIEW_PENDING",preview.sourceVersion());
        requireOne(dao.updateApplied(preview.id(),evaluation));
        saveAudit(actor,"SOURCE_BODY_REFRESH_APPLIED",sourceId,preview.id());
        return new SourceBodyRefreshResponses.Applied(preview.id(),sourceId,evaluation,"APPLIED");
    }

    static void validatePreview(SourceBodyRefreshRows.Preview p,AttachmentSourceContextRow c) {
        if(!p.expiresAt().isAfter(OffsetDateTime.now()))throw conflict("본문 미리보기의 30분 유효기간이 지났습니다. 새 미리보기를 생성하세요.");
        if(!EXTRACTOR_VERSION.equals(p.extractorVersion()) || !selectHash(p.bodyText()).equals(p.bodyHash()))
            throw conflict("본문 미리보기의 정제 버전 또는 지문이 변경됐습니다. 새 미리보기를 생성하세요.");
        if(!Objects.equals(p.baseEvaluationId(),c.baseEvaluationId()) || !Objects.equals(p.contentVersionId(),c.contentVersionId())
                || !Objects.equals(p.ruleReleaseId(),c.ruleReleaseId()) || !Objects.equals(p.sourceVersion(),c.sourceVersion())
                || !Objects.equals(p.attachmentVersion(),c.attachmentVersion()))
            throw conflict("본문·첨부·규칙 버전이 변경됐습니다. 적용하지 않았으므로 새 미리보기에서 차이를 확인하세요.");
    }
    private void validateContext(AttachmentSourceContextRow c) {
        if(c==null || !"PRODUCTION".equals(c.dataPurposeCode()) || !"LOCAL_GOV_NOTICE".equals(c.providerCode())
                || c.baseEvaluationId()==null || c.contentVersionId()==null || c.ruleReleaseId()==null
                || c.sourceVersion()==null || c.attachmentVersion()==null || "EXCLUDED".equals(c.semanticStatusCode())
                || !Set.of("GROUP_A_MATCHED","COMBINATION_MATCHED").contains(Objects.toString(c.titleStageCode(),"")))
            throw conflict("기본 판정이 있는 비제외 지자체 공고만 단건 본문 복구할 수 있습니다. 정부24는 대상이 아닙니다.");
        if(Boolean.TRUE.equals(c.attachmentReviewRequired()))
            throw conflict("첨부 판정 적용이 연결된 공고입니다. 기존 첨부 적용의 검증된 원복 절차를 먼저 완료하세요.");
    }
    private SourceBodyRefreshRows.Boundary selectBoundary(UUID sourceId) {
        var b=dao.selectBoundaryDetails(sourceId);
        if(b==null || b.blocked())throw conflict("운영 공고 연결 또는 진행 중 첨부 작업이 있거나 수집처가 비활성입니다. 연결 상태를 확인한 뒤 다시 시도하세요.");
        return b;
    }
    private AnnouncementSourceClassificationResult selectDecision(AnnouncementSourceSnapshotRow s,String body,AnnouncementSourceClassificationRuleSet r) {
        return engine.selectDecision(new AnnouncementSourceClassificationInput(s.providerCode(),s.title(),body,s.agencyName(),List.of(),
                BodySourceCode.DETAIL_PAGE_TEXT,BodyAvailabilityCode.AVAILABLE),r);
    }
    static String selectHash(String body) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(body.getBytes(StandardCharsets.UTF_8))); }
        catch(NoSuchAlgorithmException e){throw new IllegalStateException("SHA-256을 사용할 수 없습니다.",e);}
    }
    private UUID selectActor(Authentication auth) {
        if(auth==null || !(auth.getPrincipal() instanceof AuthenticatedUserDetails actor))
            throw new ApiException(ErrorCode.VALIDATION_FAILED,HttpStatus.FORBIDDEN,"관리자 로그인 계정을 확인하세요.");
        return actor.userId();
    }
    private void saveAudit(UUID actor,String action,UUID source,UUID preview) {
        sources.insertAuditLog(new AnnouncementSourceAuditLogCommand(actor,action,"ANNOUNCEMENT_SOURCE",source,"SUCCESS","{\"previewId\":\""+preview+"\"}"));
    }
    private static void requireOne(int count) { if(count!=1)throw conflict("본문 복구 저장 중 버전이 변경됐습니다. 새 미리보기를 생성하세요."); }
    private static ApiException conflict(String message) {return new ApiException(ErrorCode.ANNOUNCEMENT_SOURCE_VERSION_CONFLICT,HttpStatus.CONFLICT,message);}
}
