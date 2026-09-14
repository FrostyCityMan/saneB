package com.saneb.domain.announcementattachment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.common.error.ApiException;
import com.saneb.common.error.ErrorCode;
import com.saneb.domain.announcementattachment.vo.AttachmentEvaluationRows;
import com.saneb.domain.announcementattachment.vo.AttachmentFileSummaryRow;
import com.saneb.domain.announcementattachment.vo.AttachmentSetRow;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.http.HttpStatus;

/** 자동 판정을 수정하지 않고 사람이 확인해야 할 사유만 도출한다. 원문은 입력받지 않는다. */
public final class AttachmentReviewAssessment {
    private AttachmentReviewAssessment() { }
    private static final Set<String> TEXT_REVIEW_REASONS = Set.of("TITLE_GROUP_A_MATCHED", "BODY_GROUP_A_MATCHED",
            "BODY_GROUP_B_MATCHED", "ATTACHMENT_GROUP_A_MATCHED", "ATTACHMENT_GROUP_B_MATCHED");
    public record Result(boolean manualSourceCheckRequired, List<String> requiredCodes) { }

    public static Result select(AttachmentEvaluationRows.Evaluation decision, AttachmentSetRow set,
            List<AttachmentFileSummaryRow> files, ObjectMapper mapper) {
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
            if (file.documentRoleCode() == null || "UNKNOWN".equals(file.documentRoleCode())) {
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
    private static String selectCode(String code) {
        if (code == null || !code.matches("[A-Z][A-Z0-9_]{0,79}")) throw invalidEvidence();
        return code;
    }
    private static ApiException invalidEvidence() {
        return new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_NOT_READY, HttpStatus.CONFLICT,
                "첨부 검수 근거가 완전하지 않습니다. 처리 결과와 발견·실패 사유를 다시 확인하세요.");
    }
}
