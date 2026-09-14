package com.saneb.domain.announcementattachment.classification;

import com.saneb.domain.announcementattachment.vo.AttachmentExecutionSnapshot;
import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;

/** worker와 저장 검증이 공유하는 고정 텍스트 역할 계약. MANUAL/PROFILE은 자동 판정으로 덮어쓰지 않는다. */
public final class AttachmentFileRoleRules {
    private AttachmentFileRoleRules() { }
    public static AttachmentSetEvidence.File selectAssessedFile(AttachmentSetEvidence.File file, AttachmentExecutionSnapshot execution) {
        if (!execution.selectRoleRulesCurrent()) throw new IllegalArgumentException("ATTACHMENT_ROLE_RULES_CHANGED");
        if (!selectAssessmentRequired(file,execution)) return file;
        var assessment = new AttachmentDocumentRoleClassifier().selectAssessment(file.extraction());
        return new AttachmentSetEvidence.File(file.locator(),file.displayName(),file.detectedType(),assessment.roleCode(),"TEXT_RULE",
                file.downloadStatus(),file.downloadedBytes(),file.binaryHash(),file.failureCode(),file.extraction(),assessment);
    }
    public static boolean selectAssessmentRequired(AttachmentSetEvidence.File file, AttachmentExecutionSnapshot execution) {
        return execution.roleRuleVersion()!=null && file!=null && "UNKNOWN".equals(file.role()) && "UNKNOWN".equals(file.roleOrigin())
                && "SUCCEEDED".equals(file.downloadStatus()) && file.failureCode()==null && file.extraction()!=null
                && "COMPLETE_TEXT".equals(file.extraction().quality());
    }
    public static boolean selectAssessmentValid(AttachmentSetEvidence.File file, AttachmentExecutionSnapshot execution) {
        if (file==null || execution==null || !execution.selectRoleRulesCurrent()) return false;
        if (file.roleAssessment()==null) return !"TEXT_RULE".equals(file.roleOrigin()) && !selectAssessmentRequired(file,execution);
        var assessment=file.roleAssessment();
        if (!"TEXT_RULE".equals(file.roleOrigin()) || !file.role().equals(assessment.roleCode())
                || execution.roleRuleVersion()==null || !execution.roleRuleVersion().equals(assessment.ruleVersion())
                || !execution.roleRulesHash().equals(assessment.rulesHash()) || !"SUCCEEDED".equals(file.downloadStatus())
                || file.failureCode()!=null || file.extraction()==null || !"COMPLETE_TEXT".equals(file.extraction().quality())) return false;
        try { return new AttachmentDocumentRoleClassifier().selectAssessment(file.extraction()).equals(assessment); }
        catch (IllegalArgumentException exception) { return false; }
    }
    public static boolean selectPreservedRoleMatches(String role,String origin,AttachmentSetEvidence.File file,AttachmentExecutionSnapshot execution) {
        if (!selectAssessmentValid(file,execution)) return false;
        if ("UNKNOWN".equals(role) && "UNKNOWN".equals(origin) && file.roleAssessment()!=null) return true;
        return role.equals(file.role()) && origin.equals(file.roleOrigin()) && file.roleAssessment()==null;
    }
}
