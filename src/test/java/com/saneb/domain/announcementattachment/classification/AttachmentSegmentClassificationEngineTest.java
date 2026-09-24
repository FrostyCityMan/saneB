package com.saneb.domain.announcementattachment.classification;

import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import com.saneb.domain.announcementsource.classification.*;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.assertj.core.api.Assertions.*;

class AttachmentSegmentClassificationEngineTest {
    private static final String GUIDE = "사업 지원 안내\n지원대상: 소상공인 지원금\n지원내용: 경영지원\n신청기간: 9월";
    private static final String FORM = "지원 신청서\n성 명\n(서명 또는 인)\n수출 특허 지원금";
    private final AttachmentSegmentClassificationEngine engine = new AttachmentSegmentClassificationEngine();
    private final AnnouncementSourceClassificationRuleSet rules = new AnnouncementSourceClassificationRuleSet("SEGMENT-TEST", List.of(
            selectRule("대상", RuleGroupKindCode.TARGET, "소상공인", TargetCategoryCode.BUSINESS, null),
            selectRule("지원", RuleGroupKindCode.SUPPORT_TYPE, "지원금", null, SupportTypeCode.GRANT_SUBSIDY),
            selectRule("A", RuleGroupKindCode.REVIEW_A, "특허", null, null), selectRule("B", RuleGroupKindCode.AUTO_EXCLUDE_B, "수출", null, null)));
    private record Fixture(AnnouncementAttachmentClassificationEngine.FileInput file, AttachmentSegmentClassificationEngine.FileEvidence evidence) { }

    @ParameterizedTest @CsvSource({"특허,ATTACHMENT_GROUP_A_MATCHED", "수출,ATTACHMENT_GROUP_B_MATCHED", "특허 수출,ATTACHMENT_GROUP_B_MATCHED"})
    void structuralCandidateKeepsInternalApplicationABAndFormContextSeparate(String keyword,String reason) {
        var fixture=selectStructuralFixture(GUIDE.replace("사업 지원 안내","공고문")+"\n3. 신청안내\n"+keyword
                +"\n지원 신청서\n지원 신청서\n성 명\n(서명 또는 인)\n수출 특허 지원금");
        var result=engine.selectDecision(selectInput(null,List.of(fixture)),List.of(fixture.evidence()));
        assertThat(result.decision().status()).isEqualTo("REVIEW_REQUIRED");assertThat(result.decision().reason()).isEqualTo(reason);
        assertThat(result.segmentMatches()).filteredOn(m->"FORM".equals(m.segmentRole())).isNotEmpty()
                .allMatch(m->"CONTEXT_ONLY".equals(m.match().action()));
    }
    @Test void structuralCandidateNeverJoinsKeywordsFromDifferentOriginalParagraphs() {
        var fixture=selectStructuralFixture(GUIDE.replace("사업 지원 안내","공고문").replace("소상공인 지원금","소상공인")
                +"\n3. 신청안내\n지원금\n"+FORM);
        var result=engine.selectDecision(selectInput(null,List.of(fixture)),List.of(fixture.evidence()));
        assertThat(result.decision().reason()).isEqualTo("EXTENDED_COMBINATION_NOT_CONFIRMED");
    }
    private Fixture selectStructuralFixture(String text) {
        var old=selectFixture(text,"UNKNOWN","TEXT_RULE","COMPLETE_TEXT");
        var candidate=new AttachmentSegmentRoleAnalyzer().selectAnalysis(old.evidence().extraction(),
                AttachmentSegmentRoleAnalyzer.STRUCTURAL_VERSION,AttachmentSegmentRoleAnalyzer.STRUCTURAL_RULES_HASH);
        return new Fixture(old.file(),new AttachmentSegmentClassificationEngine.FileEvidence(old.file().fileId(),old.file().extractionId(),
                "TEXT_RULE",old.evidence().extraction(),candidate));
    }

    @Test void resolvedGuidePlusFormReducesUnknownReviewWithoutPromotingFormKeywords() {
        var fixture = selectFixture(GUIDE + "\n" + FORM, "UNKNOWN", "TEXT_RULE", "COMPLETE_TEXT");
        var input = selectInput(null, List.of(fixture));
        assertThat(new AnnouncementAttachmentClassificationEngine().selectDecision(input).reason()).isEqualTo("ATTACHMENT_CONTEXT_REVIEW");
        var result = engine.selectDecision(input, List.of(fixture.evidence()));
        assertThat(result.engineVersion()).isEqualTo(AttachmentSegmentClassificationEngine.VERSION);
        assertThat(result.decision().status()).isEqualTo("ACCEPTED");
        assertThat(result.segmentMatches()).filteredOn(match -> match.segmentIndex() == 1)
                .isNotEmpty().allMatch(match -> "CONTEXT_ONLY".equals(match.match().action()));
        assertThat(result.decision().matches()).allMatch(match -> match.fileId().equals(fixture.file().fileId()) && match.extractionId().equals(fixture.file().extractionId()));
        assertThat(fixture.file().role()).isEqualTo("UNKNOWN");
    }
    @Test void differentParagraphsAndSegmentsCannotSatisfyAnd() {
        var paragraphs = selectFixture(GUIDE.replace("소상공인 지원금", "소상공인").replace("경영지원", "지원금"), "UNKNOWN", "TEXT_RULE", "COMPLETE_TEXT");
        var segments = selectFixture(GUIDE.replace("소상공인 지원금", "소상공인") + "\n" + FORM, "UNKNOWN", "TEXT_RULE", "COMPLETE_TEXT");
        for (var fixture : List.of(paragraphs, segments)) {
            var result = engine.selectDecision(selectInput(null, List.of(fixture)), List.of(fixture.evidence()));
            assertThat(result.decision().reason()).isEqualTo("EXTENDED_COMBINATION_NOT_CONFIRMED");
        }
    }
    @Test void targetInOneFileAndSupportInAnotherCannotSatisfyAnd() {
        var first = selectFixture(GUIDE.replace("소상공인 지원금", "소상공인"), "UNKNOWN", "TEXT_RULE", "COMPLETE_TEXT");
        var second = selectFixture(GUIDE.replace("소상공인 지원금", "지원금"), "UNKNOWN", "TEXT_RULE", "COMPLETE_TEXT");
        assertThat(engine.selectDecision(selectInput(null, List.of(first, second)), List.of(first.evidence(), second.evidence())).decision().reason())
                .isEqualTo("EXTENDED_COMBINATION_NOT_CONFIRMED");
    }
    @ParameterizedTest @CsvSource({"특허,ATTACHMENT_GROUP_A_MATCHED", "수출,ATTACHMENT_GROUP_B_MATCHED", "특허 수출,ATTACHMENT_GROUP_B_MATCHED"})
    void noticeABPolicyRemainsFinalReviewNotAutomaticExclusion(String keyword, String reason) {
        var fixture = selectFixture(GUIDE + "\n" + keyword + "\n" + FORM, "UNKNOWN", "TEXT_RULE", "COMPLETE_TEXT");
        var result = engine.selectDecision(selectInput(null, List.of(fixture)), List.of(fixture.evidence()));
        assertThat(result.decision().status()).isEqualTo("REVIEW_REQUIRED"); assertThat(result.decision().reason()).isEqualTo(reason);
    }
    @Test void unknownTextOrPartialQualityStillRequiresReviewEvenWithSufficientBody() {
        var unknown = selectFixture("미확인 주의 조건\n" + GUIDE + "\n" + FORM, "UNKNOWN", "TEXT_RULE", "COMPLETE_TEXT");
        var partial = selectFixture(GUIDE + "\n" + FORM, "UNKNOWN", "TEXT_RULE", "PARTIAL_TEXT");
        assertThat(engine.selectDecision(selectInput("소상공인 지원금", List.of(unknown)), List.of(unknown.evidence())).decision().reason()).isEqualTo("ATTACHMENT_CONTEXT_REVIEW");
        assertThat(engine.selectDecision(selectInput("소상공인 지원금", List.of(partial)), List.of(partial.evidence())).decision().reason()).isEqualTo("ATTACHMENT_INCOMPLETE");
    }
    @Test void manualRoleConflictCannotBeSilentlyOverwrittenBySegmentRole() {
        var fixture = selectFixture(GUIDE + "\n" + FORM, "GUIDE", "MANUAL", "COMPLETE_TEXT");
        var result = engine.selectDecision(selectInput(null, List.of(fixture)), List.of(fixture.evidence()));
        assertThat(result.decision().status()).isEqualTo("REVIEW_REQUIRED");
        assertThat(result.decision().warnings()).contains("ATTACHMENT_SEGMENT_ROLE_CONFLICT");
        assertThat(result.segmentMatches()).filteredOn(match -> match.segmentIndex() == 1).allMatch(match -> "CONTEXT_ONLY".equals(match.match().action()));
        assertThat(fixture.file().role()).isEqualTo("GUIDE");
    }
    @Test void wrongFileAndAlteredAnalysisAreRejected() {
        var fixture = selectFixture(GUIDE, "UNKNOWN", "TEXT_RULE", "COMPLETE_TEXT");
        var wrong = new AttachmentSegmentClassificationEngine.FileEvidence(UUID.randomUUID(), fixture.file().extractionId(), "TEXT_RULE", fixture.evidence().extraction(), fixture.evidence().analysis());
        assertThatIllegalArgumentException().isThrownBy(() -> engine.selectDecision(selectInput(null, List.of(fixture)), List.of(wrong)));
        var wrongExtraction = new AttachmentSegmentClassificationEngine.FileEvidence(fixture.file().fileId(), UUID.randomUUID(), "TEXT_RULE", fixture.evidence().extraction(), fixture.evidence().analysis());
        assertThatIllegalArgumentException().isThrownBy(() -> engine.selectDecision(selectInput(null, List.of(fixture)), List.of(wrongExtraction)));
        var input = fixture.evidence().extraction();
        var altered = new AttachmentSetEvidence.Extraction(input.quality(), input.text() + "위조", input.blocks(), 1, 1);
        var forged = new AttachmentSegmentClassificationEngine.FileEvidence(fixture.file().fileId(), fixture.file().extractionId(), "TEXT_RULE", altered, fixture.evidence().analysis());
        assertThatIllegalArgumentException().isThrownBy(() -> engine.selectDecision(selectInput(null, List.of(fixture)), List.of(forged)));
    }
    @Test void fileWithoutExtractionIsNotOmittedOrReportedAsAccepted() {
        var normal=selectFixture(GUIDE+"\n"+FORM,"UNKNOWN","UNKNOWN","COMPLETE_TEXT");
        var failed=new AnnouncementAttachmentClassificationEngine.FileInput(UUID.randomUUID(),null,"UNKNOWN","FAILED",null,List.of(),"NETWORK_UNAVAILABLE");
        var failedEvidence=new AttachmentSegmentClassificationEngine.FileEvidence(failed.fileId(),null,"UNKNOWN",null,null);
        var input=new AnnouncementAttachmentClassificationEngine.Input(selectBase("소상공인 지원금","소상공인 지원금"),rules,true,"FOUND",true,List.of(normal.file(),failed),null,List.of());
        assertThat(engine.selectDecision(input,List.of(normal.evidence(),failedEvidence)).decision().reason()).isEqualTo("ATTACHMENT_INCOMPLETE");
        assertThatIllegalArgumentException().isThrownBy(()->engine.selectDecision(input,List.of(normal.evidence())));
    }
    @Test void titleExclusionAndBaseReviewPoliciesCannotBeOverridden() {
        var fixture = selectFixture(GUIDE + "\n" + FORM, "UNKNOWN", "TEXT_RULE", "COMPLETE_TEXT");
        var excludedBase = selectBase("소상공인 수출 지원금", null);
        var input = new AnnouncementAttachmentClassificationEngine.Input(excludedBase, rules, true, "FOUND", true, List.of(fixture.file()), null, List.of());
        assertThatIllegalArgumentException().isThrownBy(() -> engine.selectDecision(input, List.of(fixture.evidence())));
        assertThat(engine.selectDecision(selectInput("소상공인 수출 지원금", List.of(fixture)), List.of(fixture.evidence())).decision().reason()).isEqualTo("BODY_GROUP_B_MATCHED");
    }
    private Fixture selectFixture(String text, String role, String origin, String quality) {
        var blocks = new ArrayList<AttachmentSetEvidence.Block>(); int start = 0;
        for (String line : text.split("\n")) {
            int end = start + line.codePointCount(0, line.length());
            blocks.add(new AttachmentSetEvidence.Block(blocks.size(), start, end, "p:" + blocks.size(), true, "p:" + blocks.size())); start = end + 1;
        }
        var extraction = new AttachmentSetEvidence.Extraction(quality, text, blocks, 1, 1);
        var file = new AnnouncementAttachmentClassificationEngine.FileInput(UUID.randomUUID(), UUID.randomUUID(), role, quality, text,
                blocks.stream().map(block -> new AnnouncementAttachmentClassificationEngine.Block(block.index(), block.startOffset(), block.endOffset(), block.evidenceScopeId(), block.scopeReliable())).toList(), null);
        return new Fixture(file, new AttachmentSegmentClassificationEngine.FileEvidence(file.fileId(), file.extractionId(), origin, extraction, new AttachmentSegmentRoleAnalyzer().selectAnalysis(extraction)));
    }
    private AnnouncementAttachmentClassificationEngine.Input selectInput(String body, List<Fixture> fixtures) {
        return new AnnouncementAttachmentClassificationEngine.Input(selectBase("소상공인 지원금", body), rules, true, "FOUND", true, fixtures.stream().map(Fixture::file).toList(), null, List.of());
    }
    private AnnouncementSourceClassificationResult selectBase(String title, String body) {
        return new AnnouncementSourceClassificationEngine().selectDecision(new AnnouncementSourceClassificationInput("BIZINFO", title, body, null, List.of(),
                body == null ? BodySourceCode.NONE : BodySourceCode.PROVIDER_FULL_TEXT,
                body == null ? BodyAvailabilityCode.UNAVAILABLE : BodyAvailabilityCode.AVAILABLE), rules);
    }
    private AnnouncementSourceClassificationRule selectRule(String code, RuleGroupKindCode kind, String term, TargetCategoryCode target, SupportTypeCode support) {
        return new AnnouncementSourceClassificationRule(code, code, kind, term, StrengthCode.STRONG, target, support,
                List.of(AnnouncementSourceClassificationTerm.canonical(term, MatchModeCode.NORMALIZED_PHRASE)), true);
    }
}
