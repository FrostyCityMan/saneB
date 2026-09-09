package com.saneb.domain.announcementattachment.classification;

import static org.assertj.core.api.Assertions.*;
import com.saneb.domain.announcementattachment.classification.AnnouncementAttachmentClassificationEngine.*;
import com.saneb.domain.announcementsource.classification.*;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.*;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AnnouncementAttachmentClassificationEngineTest {
    private final AnnouncementAttachmentClassificationEngine engine=new AnnouncementAttachmentClassificationEngine();
    private final AnnouncementSourceClassificationRuleSet rules=new AnnouncementSourceClassificationRuleSet("TEST-1",List.of(
            selectRule("대상",RuleGroupKindCode.TARGET,"소상공인",TargetCategoryCode.BUSINESS,null),
            selectRule("지원",RuleGroupKindCode.SUPPORT_TYPE,"지원금",null,SupportTypeCode.GRANT_SUBSIDY),
            selectRule("A",RuleGroupKindCode.REVIEW_A,"특허",null,null),
            selectRule("B",RuleGroupKindCode.AUTO_EXCLUDE_B,"수출",null,null)));

    @Test void attachmentCanSupplementMissingBodyWithoutChangingBase() {
        var base=selectBase("소상공인 지원금",null);
        var result=engine.selectDecision(selectInput(base,List.of(selectFile("NOTICE","소상공인 지원금","COMPLETE_TEXT",true))));
        assertThat(result.status()).isEqualTo("ACCEPTED");
        assertThat(result.reason()).isEqualTo("EXTENDED_TARGET_SUPPORT_CONFIRMED");
        assertThat(base.reasonCode()).isEqualTo(ReasonCode.BODY_UNAVAILABLE);
    }
    @Test void attachmentGroupBRequiresReviewNeverTitleExclusion() {
        var result=engine.selectDecision(selectInput(selectBase("소상공인 지원금",null),
                List.of(selectFile("NOTICE","소상공인 수출 지원금 특허","COMPLETE_TEXT",true))));
        assertThat(result.status()).isEqualTo("REVIEW_REQUIRED");
        assertThat(result.reason()).isEqualTo("ATTACHMENT_GROUP_B_MATCHED");
        assertThat(result.matches()).noneMatch(match -> "EXCLUDED".equals(match.action()));
    }
    @Test void separateFilesCannotSatisfyAnd() {
        var result=engine.selectDecision(selectInput(selectBase("소상공인 지원금",null),List.of(
                selectFile("NOTICE","소상공인","COMPLETE_TEXT",true),selectFile("NOTICE","지원금","COMPLETE_TEXT",true))));
        assertThat(result.reason()).isEqualTo("EXTENDED_COMBINATION_NOT_CONFIRMED");
    }
    @Test void separateParagraphsCannotSatisfyAnd() {
        var file=new FileInput(UUID.randomUUID(),UUID.randomUUID(),"NOTICE","COMPLETE_TEXT","소상공인\n지원금",
                List.of(new Block(0,0,4,"p1",true),new Block(1,5,8,"p2",true)),null);
        assertThat(engine.selectDecision(selectInput(selectBase("소상공인 지원금",null),List.of(file))).reason())
                .isEqualTo("EXTENDED_COMBINATION_NOT_CONFIRMED");
    }
    @Test void formIsContextOnlyEvenWhenItContainsFullCombination() {
        var result=engine.selectDecision(selectInput(selectBase("소상공인 지원금",null),List.of(selectFile("FORM","소상공인 수출 지원금","COMPLETE_TEXT",true))));
        assertThat(result.reason()).isEqualTo("EXTENDED_COMBINATION_NOT_CONFIRMED");
        assertThat(result.matches()).allMatch(match -> "CONTEXT_ONLY".equals(match.action()));
    }
    @Test void unknownRoleAndUnreliablePdfScopeRequireReview() {
        for (var file:List.of(selectFile("UNKNOWN","소상공인 지원금","COMPLETE_TEXT",true),
                selectFile("NOTICE","소상공인 지원금","COMPLETE_TEXT",false))) {
            assertThat(engine.selectDecision(selectInput(selectBase("소상공인 지원금",null),List.of(file))).reason())
                    .isEqualTo("ATTACHMENT_CONTEXT_REVIEW");
        }
    }
    @Test void partialFileCannotBeHiddenByAnotherSuccessfulFile() {
        var result=engine.selectDecision(selectInput(selectBase("소상공인 지원금",null),List.of(
                selectFile("NOTICE","소상공인 지원금","COMPLETE_TEXT",true),selectFile("NOTICE","소상공인 지원금","PARTIAL_TEXT",true))));
        assertThat(result.reason()).isEqualTo("ATTACHMENT_INCOMPLETE");
    }
    @Test void discoveryFailureIsNotNoFiles() {
        var input=new Input(selectBase("소상공인 지원금",null),rules,true,"FAILED",false,List.of(),null,List.of());
        assertThat(engine.selectDecision(input).reason()).isEqualTo("ATTACHMENT_INCOMPLETE");
    }
    @Test void verifiedNoFilesPreservesBaseAndPendingRemainsReview() {
        var base=selectBase("소상공인 지원금",null);
        assertThat(engine.selectDecision(new Input(base,rules,true,"NO_FILES",true,List.of(),null,List.of())).reason())
                .isEqualTo("BODY_UNAVAILABLE");
        assertThat(engine.selectDecision(new Input(base,rules,false,"PENDING",false,List.of(),null,List.of())).reason())
                .isEqualTo("ATTACHMENT_PENDING");
    }
    @Test void excludedOrUnmatchedTitleCannotEnterAttachmentEngine() {
        for (String title:List.of("소상공인 수출 지원금","행사 안내")) {
            assertThatIllegalArgumentException().isThrownBy(() -> engine.selectDecision(selectInput(selectBase(title,null),List.of())));
        }
    }
    @Test void mismatchedRuleReleaseIsRejectedBeforeEvaluation() {
        var input=new Input(selectBase("소상공인 지원금",null),new AnnouncementSourceClassificationRuleSet("TEST-2",rules.rules()),true,"NO_FILES",true,List.of(),null,List.of());
        assertThatIllegalArgumentException().isThrownBy(() -> engine.selectDecision(input)).withMessage("BASE_RECLASSIFICATION_REQUIRED");
    }
    @Test void negativeEligibilityIsNotPositiveCandidate() {
        var result=engine.selectDecision(selectInput(selectBase("소상공인 지원금",null),List.of(selectFile("NOTICE","소상공인 지원금 지원 대상에서 제외","COMPLETE_TEXT",true))));
        assertThat(result.reason()).isEqualTo("ATTACHMENT_CONTEXT_REVIEW");
    }
    @Test void exactTitleRuleDoesNotMatchAttachment() {
        var exact=new AnnouncementSourceClassificationRuleSet("TEST-1",List.of(new AnnouncementSourceClassificationRule(
                "B","B",RuleGroupKindCode.AUTO_EXCLUDE_B,"수출",StrengthCode.STRONG,null,null,
                List.of(AnnouncementSourceClassificationTerm.canonical("수출",MatchModeCode.EXACT_TITLE)),true)));
        assertThat(new AnnouncementSourceClassificationEngine().selectAttachmentScope("수출",null,List.of(),exact).matches()).isEmpty();
    }
    @Test void invalidCodePointOffsetIsRejected() {
        var file=new FileInput(UUID.randomUUID(),UUID.randomUUID(),"NOTICE","COMPLETE_TEXT","😀지원금",
                List.of(new Block(0,0,5,"p",true)),null);
        assertThatIllegalArgumentException().isThrownBy(() -> engine.selectDecision(selectInput(selectBase("소상공인 지원금",null),List.of(file))));
    }
    private Input selectInput(AnnouncementSourceClassificationResult base,List<FileInput> files) {
        return new Input(base,rules,true,"FOUND",true,files,null,List.of());
    }
    private FileInput selectFile(String role,String text,String quality,boolean reliable) {
        return new FileInput(UUID.randomUUID(),UUID.randomUUID(),role,quality,text,
                List.of(new Block(0,0,text.codePointCount(0,text.length()),"p1",reliable)),null);
    }
    private AnnouncementSourceClassificationResult selectBase(String title,String body) {
        return new AnnouncementSourceClassificationEngine().selectDecision(new AnnouncementSourceClassificationInput("BIZINFO",title,body,null,List.of(),
                body==null?BodySourceCode.NONE:BodySourceCode.PROVIDER_FULL_TEXT,
                body==null?BodyAvailabilityCode.UNAVAILABLE:BodyAvailabilityCode.AVAILABLE),rules);
    }
    private AnnouncementSourceClassificationRule selectRule(String code,RuleGroupKindCode kind,String term,TargetCategoryCode target,SupportTypeCode support) {
        return new AnnouncementSourceClassificationRule(code,code,kind,term,StrengthCode.STRONG,target,support,
                List.of(AnnouncementSourceClassificationTerm.canonical(term,MatchModeCode.NORMALIZED_PHRASE)),true);
    }
}
