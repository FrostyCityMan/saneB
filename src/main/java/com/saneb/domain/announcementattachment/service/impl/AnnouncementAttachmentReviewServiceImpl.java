package com.saneb.domain.announcementattachment.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.common.error.ApiException;
import com.saneb.common.error.ErrorCode;
import com.saneb.domain.announcement.dao.AnnouncementDao;
import com.saneb.domain.announcement.vo.AnnouncementSaveCommand;
import com.saneb.domain.announcement.vo.AnnouncementSupportTypeAssignmentCommand;
import com.saneb.domain.announcement.vo.AnnouncementTargetCategoryAssignmentCommand;
import com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentCurrentDao;
import com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentEvidenceDao;
import com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentEvaluationDao;
import com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentJobDao;
import com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentReviewDao;
import com.saneb.domain.announcementattachment.dto.AttachmentReviewRequests;
import com.saneb.domain.announcementattachment.dto.AttachmentReviewResponses;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentReviewService;
import com.saneb.domain.announcementattachment.service.AttachmentReviewAssessment;
import com.saneb.domain.announcementattachment.vo.AttachmentCurrentSourceRow;
import com.saneb.domain.announcementattachment.vo.AttachmentEvaluationRows;
import com.saneb.domain.announcementattachment.vo.AttachmentReviewRows;
import com.saneb.domain.announcementattachment.vo.AttachmentSourceContextRow;
import com.saneb.domain.announcementsource.dao.AnnouncementSourceDao;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceLinkResponse;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceAuditLogCommand;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceReviewHistoryCommand;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceReviewStatusCommand;
import com.saneb.domain.auth.vo.AuthenticatedUserDetails;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnnouncementAttachmentReviewServiceImpl implements AnnouncementAttachmentReviewService {
    private static final Set<String> TARGETS = Set.of("BUSINESS", "PERSONAL", "SPOUSE", "CHILD", "PARENT");
    private static final Set<String> SUPPORTS = Set.of("GENERAL_SUPPORT", "GRANT_SUBSIDY", "POLICY_FINANCE", "GUARANTEE",
            "INTEREST_SUPPORT", "VOUCHER_BENEFIT", "REFUND_REDUCTION");
    private static final Set<String> INCOMES = Set.of("INCOME_CERT_ONLY", "HEALTH_INSURANCE_ONLY", "VAT_TAX_BASE_ONLY",
            "ANY_ONE_DOCUMENT", "INCOME_OR_HEALTH_INSURANCE", "NO_LIMIT");
    private static final Set<String> METHODS = Set.of("EXTRACTED_TEXT", "MANUAL_SOURCE_CHECK");
    private final AnnouncementAttachmentJobDao jobs;
    private final AnnouncementAttachmentCurrentDao current;
    private final AnnouncementAttachmentEvaluationDao evaluations;
    private final AnnouncementAttachmentEvidenceDao evidence;
    private final AnnouncementAttachmentReviewDao reviews;
    private final AnnouncementSourceDao sources;
    private final AnnouncementDao announcements;
    private final ObjectMapper mapper;

    public AnnouncementAttachmentReviewServiceImpl(AnnouncementAttachmentJobDao jobs, AnnouncementAttachmentCurrentDao current,
            AnnouncementAttachmentEvaluationDao evaluations, AnnouncementAttachmentEvidenceDao evidence,
            AnnouncementAttachmentReviewDao reviews, AnnouncementSourceDao sources, AnnouncementDao announcements, ObjectMapper mapper) {
        this.jobs=jobs; this.current=current; this.evaluations=evaluations; this.evidence=evidence;
        this.reviews=reviews; this.sources=sources; this.announcements=announcements; this.mapper=mapper;
    }

    @Override
    @Transactional(readOnly=true, isolation=Isolation.REPEATABLE_READ, timeout=10)
    public AttachmentReviewResponses.Context selectReviewContextDetails(UUID sourceId) {
        var source = selectSource(sourceId, false);
        var ready = selectReady(source, null);
        var link = reviews.selectConversionLinkDetails(sourceId);
        AttachmentReviewResponses.ConfirmedClassification confirmed = null;
        if (ready.row().confirmationId() != null && link == null) {
            var saved = reviews.selectConfirmationDetails(sourceId, ready.row().confirmationId());
            var binding = selectBinding(saved);
            if (saved != null && Boolean.TRUE.equals(saved.current()) && sourceId.equals(saved.sourceId())
                    && ready.row().confirmationId().equals(saved.id()) && ready.decision().evaluationId().equals(saved.evaluationId())
                    && ready.row().setHash().equals(saved.setHash()) && matchesBinding(source,binding)) {
                confirmed = new AttachmentReviewResponses.ConfirmedClassification(selectResponse(saved),
                        selectCodes(reviews.selectConfirmedTargetCodeList(saved.id()), TARGETS, "확정 지원대상"),
                        selectCodes(reviews.selectConfirmedSupportCodeList(saved.id()), SUPPORTS, "확정 지원형태"),binding);
            }
        }
        return new AttachmentReviewResponses.Context(sourceId, selectVersion(ready.row()), ready.decision().status(),
                ready.decision().reason(), ready.assessment().manualSourceCheckRequired(), ready.assessment().requiredCodes(),
                confirmed, link == null ? null : new AttachmentReviewResponses.LinkedAnnouncement(link.announcementId(),link.announcementCode()));
    }

    @Override
    @Transactional(timeout=15)
    public AttachmentReviewResponses.Confirmation insertConfirmation(Authentication authentication, UUID sourceId,
            UUID idempotencyKey, AttachmentReviewRequests.Confirmation request) {
        UUID actor = selectActor(authentication);
        if (sourceId == null || idempotencyKey == null || request == null) throw invalid("원문 ID, Idempotency-Key와 검수 입력이 필요합니다.");
        validateVersion(request.version());
        var targets = selectCodes(request.targetCategoryCodes(), TARGETS, "지원대상");
        var supports = selectCodes(request.supportTypeCodes(), SUPPORTS, "지원형태");
        String method = selectCode(request.reviewMethodCode(), METHODS, "검수 방법");
        String note = request.reviewNote() == null ? "" : request.reviewNote().strip();
        if (note.isBlank() || note.length() > 1000) throw invalid("직접 확인한 내용과 검수 사유를 1~1000자로 입력하세요.");
        List<String> acknowledgements = selectAcknowledgements(request.acknowledgedErrorCodes());
        String hash = selectHash(selectJson(List.of("attachment-confirmation-v1", sourceId, actor, request.version(),
                targets, supports, method, acknowledgements, note)));
        // source 잠금을 먼저 확보하여 재수집/확인/전환이 동일한 순서로 직렬화되도록 한다.
        var source = selectSource(sourceId, true);
        var previous = reviews.selectIdempotentConfirmationDetails(idempotencyKey);
        if (previous != null) {
            if (!sourceId.equals(previous.sourceId()) || !hash.equals(previous.requestHash()))
                throw conflict("이미 사용한 Idempotency-Key입니다. 원래 요청을 재시도하거나 새 키로 검수하세요.");
            return selectResponse(previous);
        }
        if (reviews.selectConversionLinkDetails(sourceId) != null) throw conflict("이미 공고에 연결된 원문은 이 경로에서 다시 확정할 수 없습니다.");
        var ready = selectReady(source, request.version());
        if (ready.assessment().manualSourceCheckRequired() && !"MANUAL_SOURCE_CHECK".equals(method))
            throw reviewRequired("실패·불확실한 첨부가 있습니다. 전체 원문을 직접 확인한 후 MANUAL_SOURCE_CHECK로 검수하세요.");
        if (!ready.assessment().requiredCodes().equals(acknowledgements))
            throw reviewRequired("현재 실패·검수 사유를 빠짐없이 확인해야 합니다. 검수 기준을 다시 조회하고 해당 사유만 확인 목록에 포함하세요.");
        validateCatalog(targets, supports);
        updateVersion(source);
        reviews.updateConfirmationsStale(sourceId);
        UUID confirmationId = UUID.randomUUID();
        requireOne(reviews.insertConfirmation(new AttachmentReviewRows.ConfirmationInsert(confirmationId, sourceId,
                ready.decision().evaluationId(), ready.row().setHash(), actor, method, selectJson(acknowledgements), note,
                idempotencyKey, hash, source.sourceVersion(), source.attachmentVersion()+1)));
        for (var code : targets) requireOne(reviews.insertConfirmedTarget(new AttachmentReviewRows.Tag(ready.decision().evaluationId(), confirmationId, code)));
        for (var code : supports) requireOne(reviews.insertConfirmedSupport(new AttachmentReviewRows.Tag(ready.decision().evaluationId(), confirmationId, code)));
        sources.insertAuditLog(new AnnouncementSourceAuditLogCommand(actor, "ATTACHMENT_CONFIRMATION_INSERT", "ANNOUNCEMENT_SOURCE", sourceId,
                "SUCCESS", selectJson(Map.of("confirmationId",confirmationId,"evaluationId",ready.decision().evaluationId(),
                "setHash",ready.row().setHash(),"sourceVersion",source.sourceVersion(),"attachmentVersion",source.attachmentVersion()+1,
                "targetCount",targets.size(),"supportCount",supports.size(),"method",method,"noteHash",selectHash(note)))));
        var saved = reviews.selectConfirmationDetails(sourceId, confirmationId);
        if (saved == null) throw internal();
        return selectResponse(saved);
    }

    @Override
    @Transactional(timeout=15)
    public AnnouncementSourceLinkResponse insertOperationalAnnouncement(Authentication authentication, UUID sourceId,
            AttachmentReviewRequests.Conversion request) {
        UUID actor = selectActor(authentication);
        if (sourceId == null || request == null || request.expectedConfirmationId() == null) throw invalid("원문 ID와 현재 검수 확인 ID가 필요합니다.");
        validateVersion(request.version());
        String primary = selectCode(request.primaryTargetCategoryCode(), TARGETS, "대표 지원대상");
        String income = request.incomeJudgementCode() == null || request.incomeJudgementCode().isBlank()
                ? "VAT_TAX_BASE_ONLY" : selectCode(request.incomeJudgementCode(), INCOMES, "소득 판단 방식");
        String hash = selectHash(selectJson(List.of("attachment-draft-v1",sourceId,request.version(),request.expectedConfirmationId(),primary,income)));
        var source = selectSource(sourceId, true);
        var content = sources.selectSourceDetails(sourceId);
        if (content == null) throw notFound();
        var link = reviews.selectConversionLinkDetails(sourceId);
        if (link != null) {
            if (!request.expectedConfirmationId().equals(link.confirmationId()) || !hash.equals(link.requestHash()))
                throw conflict("이미 공고에 연결됐으며 최초 전환 요청과 다릅니다. 기존 공고를 확인하세요.");
            return new AnnouncementSourceLinkResponse(sourceId,content.publicCode(),link.announcementId(),link.announcementCode());
        }
        var ready = selectReady(source, request.version());
        var confirmation = reviews.selectConfirmationDetails(sourceId, request.expectedConfirmationId());
        if (confirmation == null) throw notFound();
        var binding = selectBinding(confirmation);
        if (!Boolean.TRUE.equals(confirmation.current()) || !ready.decision().evaluationId().equals(confirmation.evaluationId())
                || !sourceId.equals(confirmation.sourceId()) || !request.expectedConfirmationId().equals(confirmation.id())
                || !ready.row().setHash().equals(confirmation.setHash()) || !matchesBinding(source,binding))
            throw conflict("현재 판정·첨부 버전으로 검수 확인을 다시 완료해야 DRAFT를 생성할 수 있습니다.");
        var targets = selectCodes(reviews.selectConfirmedTargetCodeList(confirmation.id()), TARGETS, "확정 지원대상");
        var supports = selectCodes(reviews.selectConfirmedSupportCodeList(confirmation.id()), SUPPORTS, "확정 지원형태");
        if (!targets.contains(primary)) throw invalid("대표 지원대상은 현재 검수 확인에 저장된 지원대상 중에서 선택하세요.");
        validateCatalog(targets, supports);
        if (sources.selectPendingDuplicateCandidateCount(sourceId)>0 || sources.selectPendingSnapshotDuplicateCount(sourceId)>0)
            throw new ApiException(ErrorCode.ANNOUNCEMENT_SOURCE_NOT_CONVERTIBLE,HttpStatus.CONFLICT,"중복 또는 유사 공고 후보를 먼저 검수해야 합니다.");
        UUID announcementId=UUID.randomUUID();
        announcements.insertAnnouncement(new AnnouncementSaveCommand(announcementId, primary, content.title(),
                content.agencyName()==null || content.agencyName().isBlank() ? "기관 미확인" : content.agencyName(), content.bodyText(),
                content.applicationStartDate(),content.applicationEndDate(),income,BigDecimal.ZERO,BigDecimal.ZERO,actor));
        var announcement=announcements.selectAnnouncementDetails(announcementId);
        if (announcement==null || !"DRAFT".equals(announcement.approvalStatusCode())) throw internal();
        for (var code:targets) announcements.insertAnnouncementTargetCategoryAssignment(new AnnouncementTargetCategoryAssignmentCommand(
                UUID.randomUUID(),announcementId,code,primary.equals(code),"SOURCE_CONFIRMED",actor));
        for (var code:supports) announcements.insertAnnouncementSupportTypeAssignment(new AnnouncementSupportTypeAssignmentCommand(
                UUID.randomUUID(),announcementId,code,"SOURCE_CONFIRMED",actor));
        requireOne(reviews.insertConversionLink(new AttachmentReviewRows.LinkInsert(UUID.randomUUID(),sourceId,announcementId,actor,confirmation.id(),hash)));
        updateVersion(source);
        requireOne(sources.updateSourceReviewStatus(new AnnouncementSourceReviewStatusCommand(sourceId,"CONDITION_INPUT_REQUIRED")));
        sources.insertSourceReviewHistory(new AnnouncementSourceReviewHistoryCommand(UUID.randomUUID(),sourceId,content.reviewStatusCode(),
                "CONDITION_INPUT_REQUIRED","특정 첨부 판정에 대한 관리자 확인과 확정 다중 분류로 DRAFT를 생성했습니다.",actor));
        sources.insertAuditLog(new AnnouncementSourceAuditLogCommand(actor,"ATTACHMENT_DRAFT_INSERT","ANNOUNCEMENT_SOURCE",sourceId,"SUCCESS",
                selectJson(Map.of("announcementId",announcementId,"confirmationId",confirmation.id(),"evaluationId",ready.decision().evaluationId(),
                "requestHash",hash,"targetCount",targets.size(),"supportCount",supports.size()))));
        return new AnnouncementSourceLinkResponse(sourceId,content.publicCode(),announcementId,announcement.announcementCode());
    }

    private AttachmentSourceContextRow selectSource(UUID sourceId, boolean lock) {
        if (sourceId==null) throw notFound();
        var source=lock ? jobs.selectSourceContextDetailsForUpdate(sourceId) : jobs.selectSourceContextDetails(sourceId);
        if (source==null || !"PRODUCTION".equals(source.dataPurposeCode()) || "EXCLUDED".equals(source.semanticStatusCode())
                || source.titleStageCode()==null || !Set.of("GROUP_A_MATCHED","COMBINATION_MATCHED").contains(source.titleStageCode())) throw notFound();
        return source;
    }
    private record Ready(AttachmentCurrentSourceRow row, AttachmentEvaluationRows.Evaluation decision, AttachmentReviewAssessment.Result assessment) { }
    private Ready selectReady(AttachmentSourceContextRow source, AttachmentReviewRequests.Version expected) {
        var row=current.selectSourceDetails(source.sourceId());
        if (row==null) throw notFound();
        if (expected!=null && evaluations.selectEvaluationDetails(source.sourceId(), expected.expectedAttachmentDecisionId())==null) throw notFound();
        if (expected!=null && (!Objects.equals(expected.expectedBaseDecisionId(),source.baseEvaluationId())
                || !Objects.equals(expected.expectedAttachmentDecisionId(),source.currentAttachmentEvaluationId())
                || !Objects.equals(expected.expectedSourceVersion(),source.sourceVersion())
                || !Objects.equals(expected.expectedAttachmentVersion(),source.attachmentVersion())))
            throw conflict("본문 또는 첨부 판정이 변경됐습니다. 입력을 유지한 채 최신 판정과 버전을 다시 확인하세요.");
        if (!Boolean.TRUE.equals(source.attachmentReviewRequired())) throw reviewRequired("첨부 검수 정책이 적용되지 않은 미리보기는 이 경로에서 확정·전환할 수 없습니다.");
        if (row.attachmentDecisionId()==null || reviews.selectActiveNormalJobExists(source.sourceId()))
            throw new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_NOT_READY,HttpStatus.CONFLICT,"첨부가 처리 중이거나 현재 판정이 없습니다. 처리가 끝난 뒤 다시 확인하세요.");
        if (!Objects.equals(row.attachmentDecisionId(),source.currentAttachmentEvaluationId())
                || !Objects.equals(row.baseDecisionId(),source.baseEvaluationId()) || !Objects.equals(row.sourceVersion(),source.sourceVersion())
                || !Objects.equals(row.attachmentVersion(),source.attachmentVersion())) throw conflict("현재 판정 연결과 버전이 일치하지 않습니다.");
        var decision=evaluations.selectEvaluationDetails(source.sourceId(),row.attachmentDecisionId());
        var set=evidence.selectSetDetails(source.sourceId(),row.setId());
        if (decision==null || set==null || !Boolean.TRUE.equals(decision.current()) || !"SEALED".equals(set.setStatus())
                || !Objects.equals(source.baseEvaluationId(),decision.baseEvaluationId()) || !Objects.equals(source.ruleReleaseId(),decision.ruleReleaseId())
                || !Objects.equals(source.attachmentPolicyId(),decision.policyId()) || !Objects.equals(decision.setId(),set.setId())
                || !Objects.equals(source.contentVersionId(),set.contentVersionId()) || !Objects.equals(decision.policyId(),set.policyId())
                || row.setHash()==null || !row.setHash().equals(set.manifestHash())) throw conflict("봉인된 첨부 집합과 현재 판정 연결이 일치하지 않습니다. 최신 처리가 필요합니다.");
        if (expected!=null && !expected.expectedSetHash().equals(row.setHash())) throw conflict("첨부 집합 hash가 변경됐습니다. 최신 첨부 전체를 다시 확인하세요.");
        var files=evidence.selectFileList(source.sourceId(),set.setId(),0,11);
        if (files==null || evidence.selectFileCount(source.sourceId(),set.setId())!=files.size()) throw conflict("첨부 파일 목록을 완전히 확인할 수 없습니다. 처리가 끝난 뒤 다시 확인하세요.");
        return new Ready(row,decision,AttachmentReviewAssessment.select(decision,set,files,mapper));
    }
    private AttachmentReviewRequests.Version selectVersion(AttachmentCurrentSourceRow row) {
        return new AttachmentReviewRequests.Version(row.baseDecisionId(),row.attachmentDecisionId(),row.sourceVersion(),row.attachmentVersion(),row.setHash());
    }
    private void validateVersion(AttachmentReviewRequests.Version version) {
        if (version==null || version.expectedBaseDecisionId()==null || version.expectedAttachmentDecisionId()==null
                || version.expectedSourceVersion()==null || version.expectedSourceVersion()<0 || version.expectedAttachmentVersion()==null
                || version.expectedAttachmentVersion()<0 || version.expectedAttachmentVersion()==Integer.MAX_VALUE
                || version.expectedSetHash()==null || !version.expectedSetHash().matches("[0-9a-f]{64}"))
            throw invalid("기본·첨부 판정 ID, 0 이상의 원문·첨부 버전과 SHA-256 소문자 64자리 첨부 집합 hash가 필요합니다.");
    }
    private void updateVersion(AttachmentSourceContextRow source) {
        requireOne(reviews.updateSourceVersion(source.sourceId(),source.sourceVersion(),source.attachmentVersion(),source.currentAttachmentEvaluationId()));
    }
    private void validateCatalog(List<String> targets,List<String> supports) {
        if (!targets.equals(reviews.selectEnabledTargetCodeList(targets)) || !supports.equals(reviews.selectEnabledSupportCodeList(supports)))
            throw conflict("선택한 분류에 비활성 또는 없는 항목이 있습니다. 사용 가능한 지원대상·지원형태를 다시 선택하세요.");
    }
    private List<String> selectCodes(List<String> values,Set<String> allowed,String label) {
        if (values==null || values.isEmpty() || values.size()>allowed.size()) throw invalid(label+"은 1~"+allowed.size()+"개 선택해야 합니다.");
        var sorted=new TreeSet<String>();
        for (var value:values) if (!sorted.add(selectCode(value,allowed,label))) throw invalid(label+"에 같은 항목을 중복 선택할 수 없습니다.");
        return List.copyOf(sorted);
    }
    private String selectCode(String value,Set<String> allowed,String label) {
        String code=value==null ? "" : value.strip().toUpperCase(Locale.ROOT);
        if (!allowed.contains(code)) throw invalid(label+"은 다음 코드 중에서 선택하세요: "+String.join(", ",new TreeSet<>(allowed)));
        return code;
    }
    private List<String> selectAcknowledgements(List<String> values) {
        if (values==null || values.size()>100) throw invalid("확인 사유는 현재 조회한 코드 목록으로 최대 100개까지 입력하세요.");
        var sorted=new TreeSet<String>();
        for (var value:values) if (value==null || !value.matches("[A-Z][A-Z0-9_]{0,79}") || !sorted.add(value))
            throw invalid("확인 사유는 중복 없이 조회된 영문 대문자 코드 그대로 입력하세요.");
        return List.copyOf(sorted);
    }
    private UUID selectActor(Authentication authentication) {
        if (authentication==null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof AuthenticatedUserDetails details))
            throw new ApiException(ErrorCode.AUTH_REQUIRED,HttpStatus.UNAUTHORIZED,"로그인한 운영 계정이 필요합니다.");
        if (!details.isEnabled() || details.passwordResetRequired() || details.roles().stream().noneMatch(Set.of("ADMIN","OPERATOR")::contains))
            throw new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_ACTION_FORBIDDEN,HttpStatus.FORBIDDEN,"활성 ADMIN 또는 OPERATOR 계정으로 비밀번호 변경을 완료한 뒤 검수·전환할 수 있습니다.");
        return details.userId();
    }
    private AttachmentReviewResponses.Confirmation selectResponse(AttachmentReviewRows.Confirmation row) {
        return new AttachmentReviewResponses.Confirmation(row.sourceId(),row.id(),row.evaluationId(),row.setHash(),row.sourceVersion(),row.attachmentVersion(),
                row.reviewMethod(),Boolean.TRUE.equals(row.current()),row.confirmedAt());
    }
    private AttachmentReviewResponses.ConfirmationBinding selectBinding(AttachmentReviewRows.Confirmation row) {
        if(row==null)return null;
        var restored=reviews.selectRestoredBindingDetails(row.sourceId(),row.id());
        if(restored==null)return new AttachmentReviewResponses.ConfirmationBinding(null,row.id(),row.sourceId(),row.sourceVersion(),row.attachmentVersion());
        // 복구 근거가 손상되면 원래 버전으로 조용히 fallback하지 않는다. 식별자·단조 증가를 함께 확인한다.
        if(restored.restorationId()==null || !row.id().equals(restored.confirmationId()) || !row.sourceId().equals(restored.sourceId())
                || row.sourceVersion()==null || row.attachmentVersion()==null || !row.sourceVersion().equals(restored.sourceVersion())
                || restored.attachmentVersion()==null || restored.attachmentVersion()<=row.attachmentVersion())return null;
        return new AttachmentReviewResponses.ConfirmationBinding(restored.restorationId(),row.id(),row.sourceId(),restored.sourceVersion(),restored.attachmentVersion());
    }
    private boolean matchesBinding(AttachmentSourceContextRow source,AttachmentReviewResponses.ConfirmationBinding binding) {
        return binding!=null && source.sourceId().equals(binding.sourceId()) && source.sourceVersion().equals(binding.sourceVersion())
                && source.attachmentVersion().equals(binding.attachmentVersion());
    }
    private String selectJson(Object value) {
        try { return mapper.writeValueAsString(value); } catch (JsonProcessingException exception) { throw internal(); }
    }
    private String selectHash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException exception) { throw internal(); }
    }
    private void requireOne(int count) { if (count!=1) throw conflict("검수 또는 전환 중 상태가 변경됐습니다. 최신 상태를 다시 확인하세요."); }
    private ApiException invalid(String message) { return new ApiException(ErrorCode.VALIDATION_FAILED,HttpStatus.BAD_REQUEST,message); }
    private ApiException conflict(String message) { return new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_VERSION_CONFLICT,HttpStatus.CONFLICT,message); }
    private ApiException reviewRequired(String message) { return new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_REVIEW_REQUIRED,HttpStatus.CONFLICT,message); }
    private ApiException notFound() { return new ApiException(ErrorCode.RESOURCE_NOT_FOUND,HttpStatus.NOT_FOUND,"해당 원문에 속하는 현재 검수 대상을 찾을 수 없습니다."); }
    private ApiException internal() { return new ApiException(ErrorCode.INTERNAL_ERROR,HttpStatus.INTERNAL_SERVER_ERROR,"검수·DRAFT 저장 조건을 충족하지 못했습니다. 변경은 저장하지 않습니다."); }
}
