package com.saneb.domain.announcementattachment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.common.error.ApiException;
import com.saneb.common.error.ErrorCode;
import com.saneb.domain.announcementattachment.vo.AttachmentEvaluationRows;
import com.saneb.domain.announcementattachment.vo.AttachmentFileSummaryRow;
import com.saneb.domain.announcementattachment.vo.AttachmentSetRow;
import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import com.saneb.domain.announcementattachment.classification.AttachmentSegmentClassificationEngine;
import com.saneb.domain.announcementattachment.classification.AttachmentSegmentRoleAnalyzer;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.http.HttpStatus;

/** 자동 판정을 수정하지 않고 검수 사유를 도출한다. 구간 근거는 현재 평가에 결합된 추출 텍스트로 재검증한다. */
public final class AttachmentReviewAssessment {
    private AttachmentReviewAssessment() { }
    private static final Set<String> TEXT_REVIEW_REASONS = Set.of("TITLE_GROUP_A_MATCHED", "BODY_GROUP_A_MATCHED",
            "BODY_GROUP_B_MATCHED", "ATTACHMENT_GROUP_A_MATCHED", "ATTACHMENT_GROUP_B_MATCHED");
    public record Result(boolean manualSourceCheckRequired, List<String> requiredCodes) { }

    public static Result select(AttachmentEvaluationRows.Evaluation decision, AttachmentSetRow set,
            List<AttachmentFileSummaryRow> files, ObjectMapper mapper) {
        return select(decision,set,files,List.of(),mapper);
    }
    public static Result select(AttachmentEvaluationRows.Evaluation decision, AttachmentSetRow set,
            List<AttachmentFileSummaryRow> files, List<AttachmentEvaluationRows.SegmentReview> segmentEvidence, ObjectMapper mapper) {
        boolean segmentEngine=AttachmentSegmentClassificationEngine.VERSION.equals(decision.engineVersion());
        Set<java.util.UUID> resolved=segmentEngine?selectResolvedSegments(files,segmentEvidence,mapper):Set.of();
        TreeSet<String> codes = new TreeSet<>();
        if (decision.warningCodesJson() == null || decision.warningCodesJson().length() > 10000) throw invalidEvidence();
        try {
            var warnings = mapper.readTree(decision.warningCodesJson());
            if (warnings == null || !warnings.isArray() || warnings.size() > 100) throw invalidEvidence();
            for (var warning : warnings) {
                if (!warning.isTextual()) throw invalidEvidence();
                codes.add(selectCode(warning.textValue()));
            }
        } catch (JsonProcessingException exception) { throw invalidEvidence(); }
        if (set.warningCodes() == null || set.warningCodes().size() > 20) throw invalidEvidence();
        set.warningCodes().forEach(code -> codes.add(selectCode(code)));
        // 알 수 없는 경고도 추출 텍스트만 확인해서 해제하지 않는다.
        boolean manual = !codes.isEmpty();
        if (!Boolean.TRUE.equals(set.discoveryComplete())
                || set.discoveryStatus() == null || !Set.of("FOUND", "NO_FILES").contains(set.discoveryStatus())) {
            codes.add("ATTACHMENT_DISCOVERY_INCOMPLETE"); manual = true;
        }
        if (set.discoveredCount() == null || set.processedCount() == null || files.size() > 10
                || set.processedCount() != files.size() || set.discoveredCount() != files.size()) {
            codes.add("ATTACHMENT_FILE_SET_INCOMPLETE"); manual = true;
        }
        for (var file : files) {
            if (!set.setId().equals(file.setId())) throw invalidEvidence();
            if (!"SUCCEEDED".equals(file.downloadStatusCode())) {
                codes.add(file.downloadErrorCode() == null ? "ATTACHMENT_DOWNLOAD_INCOMPLETE" : selectCode(file.downloadErrorCode()));
                manual = true;
            }
            if (!"COMPLETE_TEXT".equals(file.qualityCode())) {
                codes.add(file.qualityCode() == null ? "ATTACHMENT_TEXT_NOT_EXTRACTED" : selectCode(file.qualityCode()));
                manual = true;
            }
            if (file.extractionErrorCode() != null) { codes.add(selectCode(file.extractionErrorCode())); manual = true; }
            boolean automaticRole=Set.of("TEXT_RULE","UNKNOWN").contains(file.roleOriginCode()==null?"":file.roleOriginCode());
            boolean unresolvedRole=file.documentRoleCode() == null || "UNKNOWN".equals(file.documentRoleCode());
            if ((unresolvedRole && !(segmentEngine && automaticRole && resolved.contains(file.fileId())))
                    || (segmentEngine && "COMPLETE_TEXT".equals(file.qualityCode()) && !resolved.contains(file.fileId()))) {
                codes.add("ATTACHMENT_ROLE_UNKNOWN"); manual = true;
            }
        }
        if ("REVIEW_REQUIRED".equals(decision.status())) {
            codes.add(selectCode(decision.reason()));
            if (!TEXT_REVIEW_REASONS.contains(decision.reason())) manual = true;
        } else if (!"ACCEPTED".equals(decision.status())) throw invalidEvidence();
        if (codes.size() > 100) throw invalidEvidence();
        return new Result(manual, List.copyOf(codes));
    }
    private static Set<java.util.UUID> selectResolvedSegments(List<AttachmentFileSummaryRow> files,
            List<AttachmentEvaluationRows.SegmentReview> rows,ObjectMapper mapper) {
        if(rows==null || rows.size()!=files.size() || rows.size()>10)throw invalidSegmentEvidence();
        var byFile=new java.util.HashMap<java.util.UUID,AttachmentEvaluationRows.SegmentReview>();
        for(var row:rows)if(row==null || row.fileId()==null || byFile.put(row.fileId(),row)!=null)throw invalidSegmentEvidence();
        var resolved=new java.util.HashSet<java.util.UUID>();
        var seen=new java.util.HashSet<java.util.UUID>();
        var analyzer=new AttachmentSegmentRoleAnalyzer();
        for(var file:files) {
            if(file==null || !seen.add(file.fileId()))throw invalidSegmentEvidence();
            var row=byFile.get(file.fileId());
            if(row==null || !java.util.Objects.equals(row.extractionId(),file.extractionId())
                    || !java.util.Objects.equals(row.qualityCode(),file.qualityCode()))throw invalidSegmentEvidence();
            // 부분/실패 파일은 원문 수동 확인을 유지한다. 다른 파일의 완전 분석으로 덮지 않는다.
            if(!"COMPLETE_TEXT".equals(file.qualityCode()))continue;
            if(row.extractedText()==null || row.blocksJson()==null || row.analysisJson()==null)throw invalidSegmentEvidence();
            try {
                var blocks=mapper.readValue(row.blocksJson(),AttachmentSetEvidence.Block[].class);
                var analysis=mapper.readValue(row.analysisJson(),AttachmentSegmentRoleAnalyzer.Analysis.class);
                if(blocks==null || analysis==null)throw invalidSegmentEvidence();
                var extraction=new AttachmentSetEvidence.Extraction(row.qualityCode(),row.extractedText(),java.util.Arrays.asList(blocks),row.pageCount(),0);
                if(!analyzer.selectAnalysisValid(extraction,analysis))throw invalidSegmentEvidence();
                if("RESOLVED".equals(analysis.statusCode()) && analysis.segments().stream().noneMatch(s->"UNKNOWN".equals(s.roleCode())))resolved.add(file.fileId());
            } catch(JsonProcessingException | IllegalArgumentException exception) {throw invalidSegmentEvidence();}
        }
        return Set.copyOf(resolved);
    }
    private static ApiException invalidSegmentEvidence() {
        return new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_NOT_READY,HttpStatus.CONFLICT,
                "현재 첨부 판정에 연결된 구간 분석과 파일·추출 버전 또는 원문이 일치하지 않습니다. 최신 첨부 처리 결과를 확인한 뒤 다시 검수하세요.");
    }
    private static String selectCode(String code) {
        if (code == null || !code.matches("[A-Z][A-Z0-9_]{0,79}")) throw invalidEvidence();
        return code;
    }
    private static ApiException invalidEvidence() {
        return new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_NOT_READY, HttpStatus.CONFLICT,
                "첨부 검수 근거가 완전하지 않습니다. 처리 결과와 발견·실패 사유를 다시 확인하세요.");
    }
}
