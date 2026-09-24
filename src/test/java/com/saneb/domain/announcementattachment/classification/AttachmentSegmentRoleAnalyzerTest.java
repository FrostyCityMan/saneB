package com.saneb.domain.announcementattachment.classification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.*;

class AttachmentSegmentRoleAnalyzerTest {
    private static final String GUIDE = "사업 지원 안내\n지원대상: 소상공인\n지원내용: 지원금\n신청기간: 9월";
    private static final String FORM = "지원 신청서\n성 명\n(서명 또는 인)";
    private final AttachmentSegmentRoleAnalyzer analyzer = new AttachmentSegmentRoleAnalyzer();

    private static final String PARENTHESIZED_GUIDE="사업 지원 안내\n❍(신청자격) 소상공인\n❍(지원내용) 지원금\n❍(신청기간) 9월";
    private AttachmentSegmentRoleAnalyzer.Analysis selectParenthesized(AttachmentSetEvidence.Extraction input) {
        return analyzer.selectAnalysis(input,AttachmentSegmentRoleAnalyzer.PARENTHESIZED_VERSION,AttachmentSegmentRoleAnalyzer.PARENTHESIZED_RULES_HASH);
    }
    private AttachmentSegmentRoleAnalyzer.Analysis selectQuarter(AttachmentSetEvidence.Extraction input) {
        return analyzer.selectAnalysis(input,AttachmentSegmentRoleAnalyzer.QUARTER_VERSION,AttachmentSegmentRoleAnalyzer.QUARTER_RULES_HASH);
    }
    private AttachmentSegmentRoleAnalyzer.Analysis selectStructural(AttachmentSetEvidence.Extraction input) {
        return analyzer.selectAnalysis(input,AttachmentSegmentRoleAnalyzer.STRUCTURAL_VERSION,AttachmentSegmentRoleAnalyzer.STRUCTURAL_RULES_HASH);
    }
    private AttachmentSegmentRoleAnalyzer.Analysis selectLongForm(AttachmentSetEvidence.Extraction input) {
        return analyzer.selectAnalysis(input,AttachmentSegmentRoleAnalyzer.LONG_FORM_VERSION,AttachmentSegmentRoleAnalyzer.LONG_FORM_RULES_HASH);
    }
    private static String selectLongSignature(int length) {
        String prefix="신청인 : (신청자) (서명 또는 인) (동의자) ",suffix="(서명 또는 인)";
        return prefix+" ".repeat(length-prefix.length()-suffix.length())+suffix;
    }
    @ParameterizedTest @ValueSource(ints={187,200})
    void explicitLongFormCandidateRecognizesBoundedApplicantSignatureLine(int length) {
        var input=selectExtraction("미확인 서문 😀\n"+GUIDE+"\n개인정보 수집 및 이용 동의서\n"+selectLongSignature(length));
        var old=selectStructural(input);var result=selectLongForm(input);
        assertThat(old.segments()).extracting(AttachmentSegmentRoleAnalyzer.Segment::roleCode).containsExactly("UNKNOWN","GUIDE","UNKNOWN");
        assertThat(result.segments()).extracting(AttachmentSegmentRoleAnalyzer.Segment::roleCode).containsExactly("UNKNOWN","GUIDE","FORM");
        assertThat(result.statusCode()).isEqualTo("REVIEW_REQUIRED");assertThat(result.textHash()).isEqualTo(old.textHash());assertThat(result.blocksHash()).isEqualTo(old.blocksHash());
        for(int i=0;i<old.segments().size();i++) {
            assertThat(result.segments().get(i).startOffset()).isEqualTo(old.segments().get(i).startOffset());
            assertThat(result.segments().get(i).endOffset()).isEqualTo(old.segments().get(i).endOffset());
        }
        assertThat(selectStructural(input)).isEqualTo(old);assertThat(analyzer.selectAnalysisValid(input,result)).isTrue();assertCoverage(input,result);
        assertThat(AttachmentEngineContract.selectSegmentCurrent(result.analysisVersion(),result.rulesHash())).isFalse();
        assertThat(AttachmentSegmentRoleAnalyzer.STRUCTURAL_RULES_HASH).isEqualTo("8b9fdd872f3eb9890146d6e360408204ff285f4b23977e07693834aceec66d43");
    }
    @Test void longFormCandidateDoesNotRelaxWholeLineLengthMissingFieldsOrCrossBlockEvidence() {
        for(String text:List.of("동의서\n"+selectLongSignature(201),"동의서\n신청인 : "+" ".repeat(170),
                "동의서\n다른 사람 "+" ".repeat(160)+"(서명 또는 인)","동의서\n"+selectLongSignature(187)+" 끝")) {
            assertThat(selectLongForm(selectExtraction(text)).segments()).allMatch(s->"UNKNOWN".equals(s.roleCode()));
        }
        var original=selectExtraction("동의서\n"+selectLongSignature(187));var last=original.blocks().getLast();
        var blocks=List.of(original.blocks().getFirst(),new AttachmentSetEvidence.Block(1,last.startOffset(),last.startOffset()+5,"p:1a",true,"p:1a"),
                new AttachmentSetEvidence.Block(2,last.startOffset()+5,last.endOffset(),"p:1b",true,"p:1b"));
        assertThat(selectLongForm(new AttachmentSetEvidence.Extraction("COMPLETE_TEXT",original.text(),blocks,1,0)).segments()).allMatch(s->"UNKNOWN".equals(s.roleCode()));
        assertThat(selectLongForm(new AttachmentSetEvidence.Extraction("PARTIAL_TEXT",original.text(),original.blocks(),1,0)).reasonCode()).isEqualTo("COMPLETE_TEXT_REQUIRED");
        assertThat(selectLongForm(selectExtraction((GUIDE+"\n").repeat(201))).reasonCode()).isEqualTo("SEGMENT_ANALYSIS_LIMIT");
    }
    @Test void numberedApplicationSubsectionStaysInsideAlreadyResolvedNoticeWithoutChangingLegacy() {
        var input=selectExtraction("미확인 표지\n"+PARENTHESIZED_GUIDE.replace("사업 지원 안내","참여자 모집 공고(3분기)")
                +"\n3. 신청안내\n❍(신청기간) 9월\n원문 신청 조건\n"+FORM);
        var legacy=selectQuarter(input);var result=selectStructural(input);
        assertThat(legacy.segments()).extracting(AttachmentSegmentRoleAnalyzer.Segment::roleCode).containsExactly("UNKNOWN","NOTICE","UNKNOWN","FORM");
        assertThat(result.segments()).extracting(AttachmentSegmentRoleAnalyzer.Segment::roleCode).containsExactly("UNKNOWN","NOTICE","FORM");
        assertThat(result.segments().get(1).evidence()).isEqualTo(legacy.segments().get(1).evidence());
        assertThat(result.statusCode()).isEqualTo("REVIEW_REQUIRED");
        assertThat(result.textHash()).isEqualTo(legacy.textHash());assertThat(result.blocksHash()).isEqualTo(legacy.blocksHash());
        assertThat(selectQuarter(input)).isEqualTo(legacy);assertThat(analyzer.selectAnalysisValid(input,result)).isTrue();assertCoverage(input,result);
        assertThat(AttachmentSegmentRoleAnalyzer.QUARTER_RULES_HASH).isEqualTo("2f02f48368ce3f42557dd62094dec8e6b99d44e27d0f51f265a2fd737aabdd82");
        // 명시 실행 허용과 문서 판정은 별개다. 서문 UNKNOWN 때문에 REVIEW_REQUIRED는 그대로다.
        assertThat(AttachmentEngineContract.selectCurrent(AttachmentSegmentClassificationEngine.VERSION,result.analysisVersion(),result.rulesHash())).isTrue();
    }
    @ParameterizedTest @ValueSource(strings={"신청 안내","3. 다른 사업 신청안내","0. 신청안내","100. 신청안내","3. 신청안내 참고"})
    void structuralCandidateDoesNotInventInternalSectionFromDifferentHeadings(String heading) {
        var input=selectExtraction(PARENTHESIZED_GUIDE.replace("사업 지원 안내","공고문")+"\n"+heading+"\n❍(신청기간) 9월\n"+FORM);
        assertThat(selectStructural(input).segments()).isEqualTo(selectQuarter(input).segments());
    }
    @Test void internalSectionCannotBorrowMissingNoticeConditionsOrHideUnreliableBlocks() {
        var input=selectExtraction("공고문\n❍(신청자격) 소상공인\n3. 신청안내\n❍(지원내용) 지원금\n❍(신청기간) 9월");
        assertThat(selectStructural(input).segments()).hasSize(2).allMatch(s->"UNKNOWN".equals(s.roleCode()));
        var complete=selectExtraction(PARENTHESIZED_GUIDE.replace("사업 지원 안내","공고문")+"\n3. 신청안내\n❍(신청기간) 9월");
        var blocks=new ArrayList<>(complete.blocks());var last=blocks.getLast();
        blocks.set(blocks.size()-1,new AttachmentSetEvidence.Block(last.index(),last.startOffset(),last.endOffset(),last.evidenceScopeId(),false,last.locator()));
        var unreliable=new AttachmentSetEvidence.Extraction("COMPLETE_TEXT",complete.text(),blocks,1,0);
        assertThat(selectStructural(unreliable).segments()).extracting(AttachmentSegmentRoleAnalyzer.Segment::roleCode).containsExactly("NOTICE","UNKNOWN");
        assertThat(selectStructural(new AttachmentSetEvidence.Extraction("PARTIAL_TEXT",complete.text(),complete.blocks(),1,0)).reasonCode()).isEqualTo("COMPLETE_TEXT_REQUIRED");
    }
    @Test void completeNumberedGuideAndGuideParentRemainSeparateDocuments() {
        String section=PARENTHESIZED_GUIDE.replace("사업 지원 안내","3. 신청안내");
        for(String parent:List.of(PARENTHESIZED_GUIDE,PARENTHESIZED_GUIDE.replace("사업 지원 안내","공고문"))) {
            var input=selectExtraction(parent+"\n"+section);
            assertThat(selectStructural(input).segments()).isEqualTo(selectQuarter(input).segments());
        }
    }
    @Test void adjacentIdenticalConsentHeadingsAreReassessedAsOneFormWithoutTextLoss() {
        var input=selectExtraction("개인정보 수집 및 이용동의서\r\n \r\n개인정보 수집 및 이용 동의서\r\n성 명\r\n(서명 또는 인)");
        assertThat(selectQuarter(input).segments()).extracting(AttachmentSegmentRoleAnalyzer.Segment::roleCode).containsExactly("UNKNOWN","FORM");
        var result=selectStructural(input);
        assertThat(result.segments()).singleElement().satisfies(s->assertThat(s.roleCode()).isEqualTo("FORM"));
        assertThat(result.statusCode()).isEqualTo("RESOLVED");assertThat(analyzer.selectAnalysisValid(input,result)).isTrue();assertCoverage(input,result);
        assertThat(result.segments().getFirst().evidence()).extracting(AttachmentSegmentRoleAnalyzer.Evidence::blockIndex).containsExactly(0,2,3);
    }
    @Test void repeatedFormsWithInterveningContentOrDifferentTitlesNeverMerge() {
        for(String text:List.of(FORM+"\n"+FORM,"지원 신청서\n알 수 없는 조건\n"+FORM,"다른 신청서\n"+FORM)) {
            var input=selectExtraction(text);
            assertThat(selectStructural(input).segments()).isEqualTo(selectQuarter(input).segments());
        }
        var input=selectExtraction("지원 신청서\n지원 신청서\n설명만 있고 필수 입력 항목 없음");
        assertThat(selectStructural(input).segments()).singleElement().satisfies(s->assertThat(s.roleCode()).isEqualTo("UNKNOWN"));
    }
    @Test void quarterNoticeHeadingSeparatesRealStructureWithoutBorrowingLaterGuideConditions() {
        String notice=PARENTHESIZED_GUIDE.replace("사업 지원 안내","참여자 모집 공고 (3분기)");
        var input=selectExtraction("기관 공고 번호\n사업명 표지\n"+notice+"\n신청 안내\n❍(신청기간) 9월\n"+FORM);
        var previous=selectParenthesized(input);var updated=selectQuarter(input);
        assertThat(AttachmentSegmentRoleAnalyzer.PARENTHESIZED_RULES_HASH).isEqualTo("9bba150694efbdaa10b853f492b748f643521f8a7c82b9e8d8bd28f86ed7d9e3");
        assertThat(previous.segments()).extracting(AttachmentSegmentRoleAnalyzer.Segment::roleCode).containsExactly("UNKNOWN","UNKNOWN","FORM");
        assertThat(updated.segments()).extracting(AttachmentSegmentRoleAnalyzer.Segment::roleCode).containsExactly("UNKNOWN","NOTICE","UNKNOWN","FORM");
        assertThat(updated.statusCode()).isEqualTo("REVIEW_REQUIRED");
        assertThat(updated.segments().get(1).evidence()).extracting(AttachmentSegmentRoleAnalyzer.Evidence::ruleCode)
                .containsExactly("NOTICE_HEADING","TARGET_SECTION","SUPPORT_SECTION","APPLICATION_SECTION");
        assertThat(updated.textHash()).isEqualTo(previous.textHash());assertThat(updated.blocksHash()).isEqualTo(previous.blocksHash());
        assertThat(analyzer.selectAnalysisValid(input,previous)).isTrue();assertThat(analyzer.selectAnalysisValid(input,updated)).isTrue();
        assertCoverage(input,updated);
        assertThat(AttachmentEngineContract.selectCurrent(AttachmentSegmentClassificationEngine.VERSION,updated.analysisVersion(),updated.rulesHash())).isTrue();
    }
    @ParameterizedTest @ValueSource(strings={"참여자 모집 공고(0분기)","참여자 모집 공고(5분기)","참여자 모집 공고(3분기) 참고", "참여자 모집 공고(3분기", "참여자 모집 공고(담당자 확인)","참여자 모집 공고(3분기)입니다."})
    void quarterHeadingDoesNotAcceptArbitrarySuffixesOrMentions(String heading) {
        assertThat(selectQuarter(selectExtraction(PARENTHESIZED_GUIDE.replace("사업 지원 안내",heading))).segments())
                .allMatch(s->"UNKNOWN".equals(s.roleCode()));
    }
    @Test void quarterHeadingCannotResolveUnreliablePdfOrPartialExtraction() {
        var input=selectExtraction(PARENTHESIZED_GUIDE.replace("사업 지원 안내","참여자 모집 공고(1분기)"));
        assertThat(selectQuarter(input).statusCode()).isEqualTo("RESOLVED");
        assertThat(selectQuarter(new AttachmentSetEvidence.Extraction("PARTIAL_TEXT",input.text(),input.blocks(),1,0)).reasonCode()).isEqualTo("COMPLETE_TEXT_REQUIRED");
        var pdf=new AttachmentSetEvidence.Extraction("COMPLETE_TEXT",input.text(),List.of(new AttachmentSetEvidence.Block(0,0,input.text().length(),"page:1",false,"page:1")),1,0);
        assertThat(selectQuarter(pdf).reasonCode()).isEqualTo("SEGMENT_CONTEXT_REQUIRED");
        assertThat(selectQuarter(pdf).segments()).allMatch(s->"UNKNOWN".equals(s.roleCode()));
    }
    @Test void exactObservedParenthesizedSectionsResolveOnlyInExplicitNewVersion() {
        var input=selectExtraction(PARENTHESIZED_GUIDE+"\n"+FORM);
        var old=analyzer.selectAnalysis(input);var updated=selectParenthesized(input);
        assertThat(AttachmentSegmentRoleAnalyzer.RULES_HASH).isEqualTo("fb807a5fcf11c102badcc35cc4b60c6abe7fa36672e2aa431e3b5f2dc16bcdde");
        assertThat(old.statusCode()).isEqualTo("REVIEW_REQUIRED");
        assertThat(updated.statusCode()).isEqualTo("RESOLVED");
        assertThat(updated.segments()).extracting(AttachmentSegmentRoleAnalyzer.Segment::roleCode).containsExactly("GUIDE","FORM");
        assertThat(updated.textHash()).isEqualTo(old.textHash());assertThat(updated.blocksHash()).isEqualTo(old.blocksHash());
        assertThat(analyzer.selectAnalysisValid(input,old)).isTrue();assertThat(analyzer.selectAnalysisValid(input,updated)).isTrue();
        assertThat(analyzer.selectAnalysis(input)).isEqualTo(old);
        assertThat(new AttachmentDocumentRoleClassifier().selectAssessment(selectExtraction(PARENTHESIZED_GUIDE)).roleCode()).isEqualTo("UNKNOWN");
        assertCoverage(input,updated);
    }
    @ParameterizedTest @ValueSource(strings={"문장에서 신청자격 언급", "❍(신청자격 제외) 소상공인", "❍(신청자격 소상공인", "❍신청자격) 소상공인", "❍(신청자격)", "❍(신청자격)   ", "❍(신청자격):", "❍(신청자격) ： …", "다른 글 ❍(신청자격) 소상공인"})
    void mentionsMalformedOrEmptyLabelsCannotSupplyRequiredSection(String line) {
        var result=selectParenthesized(selectExtraction(PARENTHESIZED_GUIDE.replace("❍(신청자격) 소상공인",line)));
        assertThat(result.segments().getFirst().roleCode()).isEqualTo("UNKNOWN");
    }
    @Test void explicitSynonymsWhitespaceAndUnicodeOffsetsPreserveEvidence() {
        var input=selectExtraction("사업 지원 안내\r\n❍ ( 지원 대상 ) : 소상공인 😀\r\n❍( 지원 규모 )：지원금\r\n❍ ( 접수 기간 ) 9월");
        var result=selectParenthesized(input);
        assertThat(result.statusCode()).isEqualTo("RESOLVED");
        assertThat(result.segments().getFirst().evidence()).extracting(AttachmentSegmentRoleAnalyzer.Evidence::ruleCode)
                .containsExactly("GUIDE_HEADING","TARGET_SECTION","SUPPORT_SECTION","APPLICATION_SECTION");
        assertCoverage(input,result);
        assertThat(analyzer.selectAnalysisValid(input,result)).isTrue();
    }
    @Test void newRulesKeepUnknownPrefixPartialQualityAndOriginalBlockBounds() {
        var input=selectExtraction("미확인 서문\n"+PARENTHESIZED_GUIDE);
        assertThat(selectParenthesized(input).segments()).extracting(AttachmentSegmentRoleAnalyzer.Segment::roleCode).containsExactly("UNKNOWN","GUIDE");
        var partial=new AttachmentSetEvidence.Extraction("PARTIAL_TEXT",input.text(),input.blocks(),1,1);
        assertThat(selectParenthesized(partial).reasonCode()).isEqualTo("COMPLETE_TEXT_REQUIRED");
        var original=selectExtraction(PARENTHESIZED_GUIDE);var split=new ArrayList<>(original.blocks());
        var target=split.remove(1);int cut=target.startOffset()+4;
        split.add(1,new AttachmentSetEvidence.Block(1,target.startOffset(),cut,"split:1",true,"split:1"));
        split.add(2,new AttachmentSetEvidence.Block(2,cut,target.endOffset(),"split:2",true,"split:2"));
        for(int i=3;i<split.size();i++){var b=split.get(i);split.set(i,new AttachmentSetEvidence.Block(i,b.startOffset(),b.endOffset(),b.evidenceScopeId(),b.scopeReliable(),b.locator()));}
        assertThat(selectParenthesized(new AttachmentSetEvidence.Extraction("COMPLETE_TEXT",original.text(),split,1,1)).segments().getFirst().roleCode()).isEqualTo("UNKNOWN");
    }
    @Test void versionHashCrossBindingCannotReplayOrEnterCurrentWorker() {
        var input=selectExtraction(PARENTHESIZED_GUIDE);
        assertThatThrownBy(()->analyzer.selectAnalysis(input,AttachmentSegmentRoleAnalyzer.PARENTHESIZED_VERSION,AttachmentSegmentRoleAnalyzer.RULES_HASH)).isInstanceOf(IllegalArgumentException.class);
        assertThat(AttachmentEngineContract.selectCurrent(AttachmentSegmentClassificationEngine.VERSION,AttachmentSegmentRoleAnalyzer.PARENTHESIZED_VERSION,AttachmentSegmentRoleAnalyzer.PARENTHESIZED_RULES_HASH)).isFalse();
    }

    @Test void mixedDocumentGetsSeparateRolesWithoutChangingLegacyFileAssessment() {
        var input = selectExtraction(GUIDE + "\n" + FORM);
        var legacy = new AttachmentDocumentRoleClassifier().selectAssessment(input);
        var result = analyzer.selectAnalysis(input);
        assertThat(legacy.reasonCode()).isEqualTo("MIXED_DOCUMENT_ROLES");
        assertThat(result.statusCode()).isEqualTo("RESOLVED");
        assertThat(result.segments()).extracting(AttachmentSegmentRoleAnalyzer.Segment::roleCode).containsExactly("GUIDE", "FORM");
        assertThat(result.textHash()).isEqualTo(legacy.textHash());
        assertThat(result.blocksHash()).isEqualTo(legacy.blocksHash());
        assertThat(new AttachmentDocumentRoleClassifier().selectAssessment(input)).isEqualTo(legacy);
        assertCoverage(input, result);
    }
    @Test void unknownPreambleIsRetainedRatherThanDiscarded() {
        var input = selectExtraction("별도 확인이 필요한 내용\n" + GUIDE + "\n" + FORM);
        var result = analyzer.selectAnalysis(input);
        assertThat(result.statusCode()).isEqualTo("REVIEW_REQUIRED");
        assertThat(result.segments()).extracting(AttachmentSegmentRoleAnalyzer.Segment::roleCode).containsExactly("UNKNOWN", "GUIDE", "FORM");
        assertCoverage(input, result);
    }
    @Test void blankPrefixAndCrLfDoNotInventAnUnknownRegion() {
        var input = selectExtraction(" \r\n\r\n" + GUIDE.replace("\n", "\r\n") + "\r\n" + FORM.replace("\n", "\r\n") + "\r\n ");
        var result = analyzer.selectAnalysis(input);
        assertThat(result.statusCode()).isEqualTo("RESOLVED");
        assertCoverage(input, result);
    }
    @Test void sameRoleHeadingStillSeparatesDifferentPrograms() {
        var input = selectExtraction(GUIDE + "\n" + GUIDE.replace("소상공인", "청년"));
        assertThat(analyzer.selectAnalysis(input).segments()).hasSize(2)
                .allMatch(segment -> "GUIDE".equals(segment.roleCode()));
    }
    @Test void requiredSectionsCannotBeBorrowedFromNextSegment() {
        var result = analyzer.selectAnalysis(selectExtraction("사업 지원 안내\n지원대상: 소상공인\n다른 사업 안내\n지원내용: 지원금\n신청기간: 9월"));
        assertThat(result.statusCode()).isEqualTo("REVIEW_REQUIRED");
        assertThat(result.segments()).hasSize(2).allMatch(segment -> "UNKNOWN".equals(segment.roleCode()));
    }
    @Test void mentionDoesNotActAsHeadingAndBareFormHeadingDoesNotResolveRole() {
        var result = analyzer.selectAnalysis(selectExtraction(GUIDE + "\n신청서를 제출하세요.\n붙임 신청서\n작성 방법은 담당자 문의"));
        assertThat(result.segments()).hasSize(2);
        assertThat(result.segments().get(1).roleCode()).isEqualTo("UNKNOWN");
    }
    @Test void unreliablePdfRemainsUnknownNotRebrandedAsReliableSegments() {
        String text = GUIDE + "\n" + FORM;
        var input = new AttachmentSetEvidence.Extraction("COMPLETE_TEXT", text,
                List.of(new AttachmentSetEvidence.Block(0, 0, text.length(), "page:1", false, "page:1")), 1, 1);
        var result = analyzer.selectAnalysis(input);
        assertThat(result.segments()).singleElement().satisfies(segment -> {
            assertThat(segment.roleCode()).isEqualTo("UNKNOWN");
            assertThat(segment.reasonCode()).isEqualTo("STRUCTURE_UNCERTAIN");
        });
        assertCoverage(input, result);
    }
    @Test void segmentMayClipOriginalReliableBlockButEvidenceKeepsOriginalIndex() {
        String text = GUIDE + "\n" + FORM;
        var input = new AttachmentSetEvidence.Extraction("COMPLETE_TEXT", text,
                List.of(new AttachmentSetEvidence.Block(0, 0, text.length(), "p:0", true, "p:0")), 1, 1);
        var result = analyzer.selectAnalysis(input);
        assertThat(result.segments()).hasSize(2);
        assertThat(result.segments().get(1).evidence()).allMatch(e -> e.blockIndex() == 0 && e.startOffset() > GUIDE.length());
        assertCoverage(input, result);
    }
    @Test void headingAcrossBlocksCannotBeSynthesizedIntoReliableHeading() {
        String text = GUIDE + "\n" + FORM;
        int cut = GUIDE.length() + 3;
        var input = new AttachmentSetEvidence.Extraction("COMPLETE_TEXT", text, List.of(
                new AttachmentSetEvidence.Block(0, 0, cut, "p:0", true, "p:0"),
                new AttachmentSetEvidence.Block(1, cut, text.length(), "p:1", true, "p:1")), 1, 1);
        var result = analyzer.selectAnalysis(input);
        // 경계로 인식되지 않는 혼합 문서를 확정하지 않는지 확인한다.
        assertThat(result.statusCode()).isEqualTo("REVIEW_REQUIRED");
    }
    @ParameterizedTest @ValueSource(strings = {"PARTIAL_TEXT", "FAILED", "OCR_REQUIRED"})
    void qualityFailureCannotBeHiddenByResolvedRoles(String quality) {
        var original = selectExtraction(GUIDE + "\n" + FORM);
        var input = new AttachmentSetEvidence.Extraction(quality, original.text(), original.blocks(), 1, 1);
        var result = analyzer.selectAnalysis(input);
        assertThat(result.reasonCode()).isEqualTo("COMPLETE_TEXT_REQUIRED");
        assertThat(result.segments()).singleElement().extracting(AttachmentSegmentRoleAnalyzer.Segment::roleCode).isEqualTo("UNKNOWN");
        assertCoverage(input, result);
    }
    @Test void replacementCharacterMakesStructureUncertain() {
        assertThat(analyzer.selectAnalysis(selectExtraction(GUIDE + "\n\uFFFD")).reasonCode()).isEqualTo("STRUCTURE_UNCERTAIN");
    }
    @Test void emojiOffsetsPointToOriginalTextAndJsonContainsNoRawText() throws Exception {
        var input = selectExtraction("😀 " + GUIDE + "\n" + FORM);
        var result = analyzer.selectAnalysis(input);
        assertThat(result.statusCode()).isEqualTo("RESOLVED");
        assertCoverage(input, result);
        String json = new ObjectMapper().writeValueAsString(result);
        assertThat(json).doesNotContain("소상공인", "신청서", "성 명", "😀");
        assertThat(analyzer.selectAnalysis(input)).isEqualTo(result);
    }
    @Test void tooManySegmentsReturnWholeUnresolvedRangeRatherThanTruncatedSuccess() {
        var input = selectExtraction((GUIDE + "\n").repeat(201));
        var result = analyzer.selectAnalysis(input);
        assertThat(result.reasonCode()).isEqualTo("SEGMENT_ANALYSIS_LIMIT");
        assertCoverage(input, result);
    }
    @Test void editedAnalysisOrDifferentTextCannotPassReproduction() {
        var input = selectExtraction(GUIDE + "\n" + FORM);
        var result = analyzer.selectAnalysis(input);
        var changed = new AttachmentSegmentRoleAnalyzer.Analysis(result.analysisVersion(), result.rulesHash(), result.textHash(), result.blocksHash(),
                result.textLength(), result.statusCode(), result.reasonCode(), result.segments().subList(0, 1));
        assertThat(analyzer.selectAnalysisValid(input, changed)).isFalse();
        assertThat(analyzer.selectAnalysisValid(selectExtraction(GUIDE), result)).isFalse();
        assertThat(analyzer.selectAnalysisValid(input, result)).isTrue();
    }
    private static AttachmentSetEvidence.Extraction selectExtraction(String text) {
        var blocks = new ArrayList<AttachmentSetEvidence.Block>();
        int start = 0;
        for (String line : text.split("\n", -1)) {
            int end = start + line.codePointCount(0, line.length());
            if (!line.isBlank()) blocks.add(new AttachmentSetEvidence.Block(blocks.size(), start, end, "p:" + blocks.size(), true, "p:" + blocks.size()));
            start = end + 1;
        }
        return new AttachmentSetEvidence.Extraction("COMPLETE_TEXT", text, blocks, 1, 1);
    }
    private static void assertCoverage(AttachmentSetEvidence.Extraction input, AttachmentSegmentRoleAnalyzer.Analysis analysis) {
        int end = 0, index = 0;
        for (var segment : analysis.segments()) {
            assertThat(segment.index()).isEqualTo(index++);
            assertThat(segment.startOffset()).isEqualTo(end);
            assertThat(segment.endOffset()).isGreaterThan(end);
            for (var evidence : segment.evidence()) {
                var block = input.blocks().get(evidence.blockIndex());
                assertThat(evidence.startOffset()).isGreaterThanOrEqualTo(Math.max(segment.startOffset(), block.startOffset()));
                assertThat(evidence.endOffset()).isLessThanOrEqualTo(Math.min(segment.endOffset(), block.endOffset()));
            }
            end = segment.endOffset();
        }
        assertThat(end).isEqualTo(input.text().codePointCount(0, input.text().length()));
    }
}
