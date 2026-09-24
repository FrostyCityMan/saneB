package com.saneb.domain.announcementattachment.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.common.error.ApiException;
import com.saneb.common.error.ErrorCode;
import com.saneb.domain.announcementattachment.classification.AttachmentSegmentRoleAnalyzer;
import com.saneb.domain.announcementattachment.classification.AttachmentEngineContract;
import com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentSegmentDao;
import com.saneb.domain.announcementattachment.dto.AttachmentSegmentAnalysisResponse;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentSegmentService;
import com.saneb.domain.announcementattachment.vo.AttachmentSegmentRows;
import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import com.saneb.domain.announcementsource.dao.AnnouncementSourceDao;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceAuditLogCommand;
import com.saneb.domain.auth.vo.AuthenticatedUserDetails;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnnouncementAttachmentSegmentServiceImpl implements AnnouncementAttachmentSegmentService {
    private final AnnouncementAttachmentSegmentDao dao;
    private final AnnouncementSourceDao sources;
    private final ObjectMapper json;
    private final AttachmentSegmentRoleAnalyzer analyzer = new AttachmentSegmentRoleAnalyzer();
    public AnnouncementAttachmentSegmentServiceImpl(AnnouncementAttachmentSegmentDao dao, AnnouncementSourceDao sources, ObjectMapper json) {
        this.dao = dao; this.sources = sources; this.json = json;
    }

    @Override @Transactional(readOnly=true, timeout=10)
    public AttachmentSegmentAnalysisResponse selectAnalysisDetails(UUID sourceId, UUID extractionId) {
        return selectAnalysisDetails(sourceId, extractionId, AttachmentSegmentRoleAnalyzer.VERSION);
    }

    @Override @Transactional(readOnly=true, timeout=10)
    public AttachmentSegmentAnalysisResponse selectAnalysisDetails(UUID sourceId, UUID extractionId, String analysisVersion) {
        String rulesHash = AttachmentEngineContract.selectSegmentRulesHash(analysisVersion);
        if (rulesHash == null) throw new ApiException(ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST,
                "analysisVersion은 segment-role-1.0.0, segment-role-1.0.2, segment-role-1.0.3 중 하나여야 합니다. 생략하면 기존 1.0.0 분석을 조회합니다.");
        var input = selectInput(sourceId, extractionId);
        return selectResponse(input, dao.selectAnalysisDetails(sourceId, extractionId, analysisVersion, rulesHash), analysisVersion, rulesHash);
    }

    @Override @Transactional(readOnly=true, timeout=10)
    public AttachmentSegmentAnalysisResponse.EvaluationBinding selectEvaluationAnalysisDetails(UUID sourceId, UUID extractionId, UUID evaluationId) {
        if (evaluationId == null) throw notFound();
        var input = selectInput(sourceId, extractionId);
        var binding = dao.selectEvaluationBindingDetails(sourceId, extractionId, evaluationId);
        if (binding == null) throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND,
                "선택한 판정에 연결된 구간 분석이 없습니다. 구간 엔진 사용 여부와 해당 판정의 파일 집합을 확인하세요. 독립 분석으로 대체하지 않습니다.");
        String rulesHash = AttachmentEngineContract.selectSegmentRulesHash(binding.analysisVersion());
        if (!evaluationId.equals(binding.evaluationId()) || !sourceId.equals(binding.sourceId())
                || !input.setId().equals(binding.setId()) || !input.fileId().equals(binding.fileId())
                || !extractionId.equals(binding.extractionId()) || binding.policyId() == null || binding.analysisId() == null
                || binding.evaluationCurrent() == null || rulesHash == null || !rulesHash.equals(binding.rulesHash())
                || !Set.of("NOTICE", "GUIDE", "FORM", "REFERENCE", "UNKNOWN").contains(binding.evaluatedFileRoleCode() == null ? "" : binding.evaluatedFileRoleCode())) throw notReady();
        var stored = dao.selectAnalysisDetails(sourceId, extractionId, binding.analysisVersion(), rulesHash);
        if (stored == null || !binding.analysisId().equals(stored.id())) throw notReady();
        return new AttachmentSegmentAnalysisResponse.EvaluationBinding(evaluationId, binding.policyId(), binding.evaluationCurrent(),
                binding.evaluatedFileRoleCode(), selectResponse(input, stored, binding.analysisVersion(), rulesHash));
    }

    @Override @Transactional(timeout=20)
    public AttachmentSegmentAnalysisResponse insertAnalysis(Authentication authentication, UUID sourceId, UUID extractionId) {
        UUID actor = selectActor(authentication);
        var input = selectInput(sourceId, extractionId);
        var stored = dao.selectAnalysisDetails(sourceId, extractionId, AttachmentSegmentRoleAnalyzer.VERSION, AttachmentSegmentRoleAnalyzer.RULES_HASH);
        if (stored != null) return selectResponse(input, stored);
        AttachmentSegmentRoleAnalyzer.Analysis analysis;
        try { analysis = analyzer.selectAnalysis(selectExtraction(input)); }
        catch (IllegalArgumentException exception) { throw notReady(); }
        int inserted = dao.insertAnalysis(new AttachmentSegmentRows.Insert(UUID.randomUUID(), sourceId, input.setId(), input.fileId(), extractionId,
                analysis.analysisVersion(), analysis.rulesHash(), analysis.textHash(), analysis.blocksHash(), selectJson(analysis)));
        stored = dao.selectAnalysisDetails(sourceId, extractionId, AttachmentSegmentRoleAnalyzer.VERSION, AttachmentSegmentRoleAnalyzer.RULES_HASH);
        if (stored == null || inserted < 0 || inserted > 1) throw notReady();
        if (inserted == 1) sources.insertAuditLog(new AnnouncementSourceAuditLogCommand(actor, "ATTACHMENT_SEGMENTS_ANALYZE", "ANNOUNCEMENT_SOURCE", sourceId,
                "SUCCESS", selectJson(Map.of("analysisId", stored.id(), "extractionId", extractionId, "analysisVersion", analysis.analysisVersion(),
                        "rulesHash", analysis.rulesHash(), "segmentCount", analysis.segments().size(), "applicationMode", "SHADOW"))));
        return selectResponse(input, stored);
    }

    private AttachmentSegmentRows.Input selectInput(UUID sourceId, UUID extractionId) {
        if (sourceId == null || extractionId == null) throw notFound();
        var input = dao.selectExtractionDetails(sourceId, extractionId);
        if (input == null || !sourceId.equals(input.sourceId()) || !extractionId.equals(input.extractionId())) throw notFound();
        return input;
    }
    private AttachmentSetEvidence.Extraction selectExtraction(AttachmentSegmentRows.Input input) {
        try {
            if (input.blocksJson() == null || input.extractedText() == null) throw notReady();
            var blocks = json.readValue(input.blocksJson(), AttachmentSetEvidence.Block[].class);
            if (blocks == null) throw notReady();
            return new AttachmentSetEvidence.Extraction(input.qualityCode(), input.extractedText(), Arrays.asList(blocks), input.pageCount(), 0);
        } catch (JsonProcessingException | IllegalArgumentException exception) { throw notReady(); }
    }
    private AttachmentSegmentAnalysisResponse selectResponse(AttachmentSegmentRows.Input input, AttachmentSegmentRows.Stored stored) {
        return selectResponse(input, stored, AttachmentSegmentRoleAnalyzer.VERSION, AttachmentSegmentRoleAnalyzer.RULES_HASH);
    }
    private AttachmentSegmentAnalysisResponse selectResponse(AttachmentSegmentRows.Input input, AttachmentSegmentRows.Stored stored, String version, String hash) {
        AttachmentSegmentRoleAnalyzer.Analysis analysis = null;
        if (stored != null) {
            if (!input.sourceId().equals(stored.sourceId()) || !input.extractionId().equals(stored.extractionId())
                    || !input.fileId().equals(stored.fileId()) || !input.setId().equals(stored.setId())) throw notReady();
            try { analysis = json.readValue(stored.analysisJson(), AttachmentSegmentRoleAnalyzer.Analysis.class); }
            catch (JsonProcessingException exception) { throw notReady(); }
            if (analysis == null || !version.equals(analysis.analysisVersion()) || !hash.equals(analysis.rulesHash())
                    || !analyzer.selectAnalysisValid(selectExtraction(input), analysis)) throw notReady();
        }
        return new AttachmentSegmentAnalysisResponse(input.sourceId(), input.setId(), input.fileId(), input.extractionId(), input.fileRoleCode(), input.fileRoleOriginCode(),
                "SHADOW", stored == null ? "NOT_ANALYZED" : "ANALYZED", stored == null ? null : stored.id(), stored == null ? null : stored.createdAt(), analysis);
    }
    private UUID selectActor(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof AuthenticatedUserDetails details))
            throw new ApiException(ErrorCode.AUTH_REQUIRED, HttpStatus.UNAUTHORIZED, "로그인한 운영 계정으로 구간 분석을 요청하세요.");
        if (!details.isEnabled() || details.passwordResetRequired() || details.roles().stream().noneMatch(Set.of("ADMIN", "OPERATOR")::contains))
            throw new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_ACTION_FORBIDDEN, HttpStatus.FORBIDDEN, "활성 ADMIN 또는 OPERATOR 계정으로 비밀번호 변경을 완료한 뒤 구간 분석을 요청하세요.");
        return details.userId();
    }
    private String selectJson(Object value) {
        try { return json.writeValueAsString(value); }
        catch (JsonProcessingException exception) { throw notReady(); }
    }
    private ApiException notFound() { return new ApiException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "이 원문에서 분석할 수 있는 봉인된 첨부 추출 이력을 찾을 수 없습니다."); }
    private ApiException notReady() { return new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_NOT_READY, HttpStatus.CONFLICT, "구간 분석에 필요한 전체 원문·block 위치 또는 저장 근거가 일치하지 않습니다. 첨부 추출 결과를 확인하세요."); }
}
