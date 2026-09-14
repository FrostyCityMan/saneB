package com.saneb.domain.announcementattachment.service.impl;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.saneb.common.error.ApiException;
import com.saneb.common.error.ErrorCode;
import com.saneb.domain.announcementattachment.classification.AnnouncementAttachmentClassificationEngine;
import com.saneb.domain.announcementsource.classification.*;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AnnouncementAttachmentPolicyGoldenGateTest {
    private AnnouncementSourceClassificationRule rule(String code,RuleGroupKindCode kind,String term,TargetCategoryCode target,SupportTypeCode support) {
        return new AnnouncementSourceClassificationRule(code,code,kind,term,StrengthCode.STRONG,target,support,
                List.of(AnnouncementSourceClassificationTerm.canonical(term,MatchModeCode.NORMALIZED_PHRASE)),true);
    }
    private AnnouncementSourceClassificationRuleSet rules() {
        return new AnnouncementSourceClassificationRuleSet("GOLDEN-TEST",List.of(
                rule("TARGET",RuleGroupKindCode.TARGET,"소상공인",TargetCategoryCode.BUSINESS,null),
                rule("SUPPORT",RuleGroupKindCode.SUPPORT_TYPE,"지원금",null,SupportTypeCode.GRANT_SUBSIDY),
                rule("A",RuleGroupKindCode.REVIEW_A,"특허",null,null),rule("B",RuleGroupKindCode.AUTO_EXCLUDE_B,"수출",null,null)));
    }
    @Test void currentRuleSetRunsThirtyCasesWithDeterministicResultsAndNoPublicationClaim() {
        var gate=new AnnouncementAttachmentPolicyGoldenGate();var result=gate.selectValidatedResult(rules(),"a".repeat(64));
        assertThat(result.caseCount()).isEqualTo(30);assertThat(result.caseIds()).hasSize(30).doesNotHaveDuplicates().contains("AG-001","AG-030");
        assertThat(result).isEqualTo(gate.selectValidatedResult(rules(),"a".repeat(64)));
        assertThat(result.resultHash()).matches("[0-9a-f]{64}");assertThat(result.ruleContentHash()).matches("[0-9a-f]{64}");
        assertThat(result.toString()).doesNotContain("소상공인","수출","published","ENFORCE");
        assertThatThrownBy(()->result.caseIds().add("FAKE")).isInstanceOf(UnsupportedOperationException.class);
    }
    @Test void actualRuleContentAndSnapshotAreBothBoundToResult() {
        var gate=new AnnouncementAttachmentPolicyGoldenGate();var original=gate.selectValidatedResult(rules(),"a".repeat(64));
        var snapshot=gate.selectValidatedResult(rules(),"b".repeat(64));assertThat(snapshot.ruleContentHash()).isEqualTo(original.ruleContentHash());
        assertThat(snapshot.resultHash()).isNotEqualTo(original.resultHash());
        var expanded=new ArrayList<>(rules().rules());expanded.add(rule("EXTRA",RuleGroupKindCode.REVIEW_A,"시제품",null,null));
        var changed=gate.selectValidatedResult(new AnnouncementSourceClassificationRuleSet("GOLDEN-TEST",expanded),"a".repeat(64));
        assertThat(changed.ruleContentHash()).isNotEqualTo(original.ruleContentHash());assertThat(changed.resultHash()).isNotEqualTo(original.resultHash());
    }
    @ParameterizedTest @ValueSource(strings={"TARGET","SUPPORT","A","B"})
    void removingCriticalRulesCannotPassPolicyGoldenValidation(String removed) {
        var changed=new AnnouncementSourceClassificationRuleSet("CHANGED",rules().rules().stream().filter(rule->!rule.ruleCode().equals(removed)).toList());
        assertThatThrownBy(()->new AnnouncementAttachmentPolicyGoldenGate().selectValidatedResult(changed,"a".repeat(64)))
                .isInstanceOfSatisfying(ApiException.class,error->assertThat(error.errorCode()).isEqualTo(ErrorCode.ANNOUNCEMENT_ATTACHMENT_POLICY_QA_FAILED));
    }
    @Test void rawUnexpectedExceptionIsNotReturnedAsQaEvidence() {
        var broken=mock(AnnouncementAttachmentClassificationEngine.class);when(broken.selectDecision(any())).thenThrow(new IllegalStateException("untrusted-fixture-content"));
        var gate=new AnnouncementAttachmentPolicyGoldenGate(new AnnouncementSourceClassificationEngine(),broken);
        assertThatThrownBy(()->gate.selectValidatedResult(rules(),"a".repeat(64))).isInstanceOf(ApiException.class)
                .hasMessageContaining("AG-001").hasMessageNotContaining("untrusted-fixture-content");
    }
    @Test void permissiveClassifierCannotMakeNegativeCasesPass() {
        var broken=spy(new AnnouncementAttachmentClassificationEngine());
        doReturn(new AnnouncementAttachmentClassificationEngine.Decision("ACCEPTED","EXTENDED_TARGET_SUPPORT_CONFIRMED",List.of(),List.of("BUSINESS"),List.of("GRANT_SUBSIDY"),List.of()))
                .when(broken).selectDecision(argThat(input->input!=null && !input.files().isEmpty() && input.files().getFirst().role().equals("FORM")));
        assertThatThrownBy(()->new AnnouncementAttachmentPolicyGoldenGate(new AnnouncementSourceClassificationEngine(),broken).selectValidatedResult(rules(),"a".repeat(64)))
                .isInstanceOf(ApiException.class).hasMessageContaining("AG-003");
    }
    @Test void titleBypassIsDetectedEvenWhenAllPositiveCasesAreCorrect() {
        var broken=spy(new AnnouncementAttachmentClassificationEngine());
        doReturn(new AnnouncementAttachmentClassificationEngine.Decision("REVIEW_REQUIRED","ATTACHMENT_PENDING",List.of(),List.of(),List.of(),List.of()))
                .when(broken).selectDecision(argThat(input->input!=null && input.base().semanticStatusCode()==SemanticStatusCode.EXCLUDED));
        assertThatThrownBy(()->new AnnouncementAttachmentPolicyGoldenGate(new AnnouncementSourceClassificationEngine(),broken).selectValidatedResult(rules(),"a".repeat(64)))
                .isInstanceOf(ApiException.class).hasMessageContaining("AG-021");
    }
    @Test void malformedOrEmptyRuleMetadataIsRejectedBeforeExecution() {
        var engine=mock(AnnouncementAttachmentClassificationEngine.class);var gate=new AnnouncementAttachmentPolicyGoldenGate(new AnnouncementSourceClassificationEngine(),engine);
        for(String hash:Arrays.asList(null,"","bad","A".repeat(64))) assertThatThrownBy(()->gate.selectValidatedResult(rules(),hash)).isInstanceOf(ApiException.class);
        assertThatThrownBy(()->gate.selectValidatedResult(null,"a".repeat(64))).isInstanceOf(ApiException.class);
        assertThatThrownBy(()->gate.selectValidatedResult(new AnnouncementSourceClassificationRuleSet("EMPTY",List.of()),"a".repeat(64))).isInstanceOf(ApiException.class);
        verifyNoInteractions(engine);
    }
}
