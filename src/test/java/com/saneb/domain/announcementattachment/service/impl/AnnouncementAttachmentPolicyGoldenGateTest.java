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
    private com.saneb.domain.announcementattachment.dto.AttachmentPolicyResponses.Configuration segmentConfiguration() {
        return new com.saneb.domain.announcementattachment.dto.AttachmentPolicyResponses.Configuration(
                com.saneb.domain.announcementattachment.classification.AttachmentSegmentClassificationEngine.VERSION,"1.0.3","b".repeat(64),83886080L,null,null,
                com.saneb.domain.announcementattachment.classification.AttachmentSegmentRoleAnalyzer.VERSION,
                com.saneb.domain.announcementattachment.classification.AttachmentSegmentRoleAnalyzer.RULES_HASH);
    }
    @Test void segmentPolicyRunsAllLegacyAndSegmentCasesAndBindsBothRuleVersions() {
        var gate=new AnnouncementAttachmentPolicyGoldenGate();var settings=segmentConfiguration();
        var result=gate.selectValidatedResult(rules(),"a".repeat(64),settings);
        assertThat(result.caseCount()).isEqualTo(52);
        assertThat(result.caseIds()).containsExactlyElementsOf(AnnouncementAttachmentPolicyGoldenGate.selectCaseIds(settings.engineVersion()));
        assertThat(result).isEqualTo(gate.selectValidatedResult(rules(),"a".repeat(64),settings));
        assertThat(AnnouncementAttachmentPolicyGoldenGate.selectContractCurrent(result,settings)).isTrue();
        var legacy=gate.selectValidatedResult(rules(),"a".repeat(64));
        assertThat(AnnouncementAttachmentPolicyGoldenGate.selectContractCurrent(legacy,settings)).isFalse();
        assertThat(result.resultHash()).isNotEqualTo(legacy.resultHash());
        assertThat(result.toString()).doesNotContain("소상공인","신청서","미확인");
    }
    @Test void segmentSeparationFixturesKeepTargetAndSupplementarySupportInDifferentScopes() {
        var allRules=new ArrayList<>(rules().rules());
        allRules.add(new AnnouncementSourceClassificationRule("WEAK-SUPPORT","WEAK-SUPPORT",RuleGroupKindCode.SUPPORT_TYPE,
                "지원",StrengthCode.SUPPLEMENTARY,null,SupportTypeCode.GENERAL_SUPPORT,
                List.of(AnnouncementSourceClassificationTerm.canonical("지원",MatchModeCode.NORMALIZED_PHRASE)),true));
        var seedLike=new AnnouncementSourceClassificationRuleSet("WITH-SUPPLEMENTARY",allRules);
        var base=new AnnouncementSourceClassificationEngine();
        assertThat(base.selectAttachmentScope("지원대상: 소상공인",null,List.of(),seedLike).combinationMatched()).isTrue();
        assertThat(base.selectAttachmentScope("신청자격: 소상공인",null,List.of(),seedLike).combinationMatched()).isFalse();
        var result=new AnnouncementAttachmentPolicyGoldenGate().selectValidatedResult(seedLike,"a".repeat(64),segmentConfiguration());
        assertThat(result.caseIds()).hasSize(52).contains("SG-005","SG-006","SG-007");
    }
    @ParameterizedTest @ValueSource(strings={"segment-role-1.0.2","segment-role-1.0.3","segment-role-1.0.4"})
    void selectedPolicyPinsEveryFixtureAndLeavesOldGoldenReproducible(String version) {
        var gate=new AnnouncementAttachmentPolicyGoldenGate();var old=segmentConfiguration();
        var newer=new com.saneb.domain.announcementattachment.dto.AttachmentPolicyResponses.Configuration(old.engineVersion(),old.extractorVersion(),
                old.extractorConfigHash(),old.maximumSourceBytes(),old.roleRuleVersion(),old.roleRulesHash(),
                version,com.saneb.domain.announcementattachment.classification.AttachmentEngineContract.selectSegmentRulesHash(version));
        var oldResult=gate.selectValidatedResult(rules(),"a".repeat(64),old);
        var newResult=gate.selectValidatedResult(rules(),"a".repeat(64),newer);
        assertThat(newResult.caseCount()).isEqualTo(52);assertThat(newResult.resultHash()).isNotEqualTo(oldResult.resultHash());
        assertThat(gate.selectValidatedResult(rules(),"a".repeat(64),old)).isEqualTo(oldResult);
        assertThat(gate.selectValidatedResult(rules(),"a".repeat(64),newer)).isEqualTo(newResult);
        var fixtures=new AttachmentSegmentPolicyGoldenGate().selectValidatedSignatures(rules(),newer.segmentRuleVersion(),newer.segmentRulesHash());
        @SuppressWarnings("unchecked")
        var files=(List<com.saneb.domain.announcementattachment.classification.AttachmentSegmentClassificationEngine.FileEvidence>)((List<?>)fixtures.get("SG-002")).get(1);
        assertThat(files).allSatisfy(f->{assertThat(f.analysis().analysisVersion()).isEqualTo(newer.segmentRuleVersion());
            assertThat(f.analysis().rulesHash()).isEqualTo(newer.segmentRulesHash());});
        assertThat(files.getFirst().analysis().segments()).extracting(s->s.roleCode()).containsExactly("NOTICE","FORM");
        if("segment-role-1.0.4".equals(version)) {
            var file=files.getFirst();
            assertThat(file.extraction().text().lines().filter(line->line.startsWith("신청인 : ")).findFirst().orElseThrow()).hasSize(187);
            var previous=new com.saneb.domain.announcementattachment.classification.AttachmentSegmentRoleAnalyzer().selectAnalysis(file.extraction(),
                    com.saneb.domain.announcementattachment.classification.AttachmentSegmentRoleAnalyzer.STRUCTURAL_VERSION,
                    com.saneb.domain.announcementattachment.classification.AttachmentSegmentRoleAnalyzer.STRUCTURAL_RULES_HASH);
            assertThat(previous.segments()).extracting(s->s.roleCode()).containsExactly("NOTICE","UNKNOWN");
        }
    }
    @Test void segmentContractRejectsReorderedMissingOrWrongEngineResults() {
        var gate=new AnnouncementAttachmentPolicyGoldenGate();var settings=segmentConfiguration();
        var good=gate.selectValidatedResult(rules(),"a".repeat(64),settings);
        var reversed=new ArrayList<>(good.caseIds());Collections.reverse(reversed);
        for(var ids:List.of(reversed,good.caseIds().subList(0,51))) {
            var bad=new AnnouncementAttachmentPolicyGoldenGate.Result(good.suiteVersion(),good.engineVersion(),good.ruleReleaseCode(),good.ruleSnapshotHash(),good.ruleContentHash(),good.resultHash(),ids.size(),ids);
            assertThat(AnnouncementAttachmentPolicyGoldenGate.selectContractCurrent(bad,settings)).isFalse();
        }
        var bad=new com.saneb.domain.announcementattachment.dto.AttachmentPolicyResponses.Configuration(settings.engineVersion(),"1.0.3",null,83886080L,null,null,settings.segmentRuleVersion(),"0".repeat(64));
        assertThatThrownBy(()->gate.selectValidatedResult(rules(),"a".repeat(64),bad)).isInstanceOf(ApiException.class).hasMessageContaining("ENGINE");
    }
    @Test void falselyPromotedFormEvidenceFailsSegmentGoldenEvenWhenDecisionStillMatches() {
        var segment=spy(new com.saneb.domain.announcementattachment.classification.AttachmentSegmentClassificationEngine());
        doAnswer(call->{
            var actual=(com.saneb.domain.announcementattachment.classification.AttachmentSegmentClassificationEngine.Result)call.callRealMethod();
            var matches=actual.segmentMatches().stream().map(value->{
                if(!"FORM".equals(value.segmentRole()))return value;
                var m=value.match();var changed=new AnnouncementAttachmentClassificationEngine.Match(m.fileId(),m.extractionId(),m.blockIndex(),m.startOffset(),m.endOffset(),m.keyword(),"TAG");
                return new com.saneb.domain.announcementattachment.classification.AttachmentSegmentClassificationEngine.MatchEvidence(changed,value.segmentIndex(),value.segmentRole());
            }).toList();
            var d=actual.decision();var changed=new AnnouncementAttachmentClassificationEngine.Decision(d.status(),d.reason(),d.warnings(),d.targetCodes(),d.supportCodes(),matches.stream().map(com.saneb.domain.announcementattachment.classification.AttachmentSegmentClassificationEngine.MatchEvidence::match).toList());
            return new com.saneb.domain.announcementattachment.classification.AttachmentSegmentClassificationEngine.Result(actual.engineVersion(),changed,matches);
        }).when(segment).selectDecision(any(),anyList());
        var gate=new AnnouncementAttachmentPolicyGoldenGate(new AnnouncementSourceClassificationEngine(),new AnnouncementAttachmentClassificationEngine(),new AttachmentSegmentPolicyGoldenGate(segment));
        assertThatThrownBy(()->gate.selectValidatedResult(rules(),"a".repeat(64),segmentConfiguration())).isInstanceOf(ApiException.class).hasMessageContaining("SEGMENT");
    }
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
