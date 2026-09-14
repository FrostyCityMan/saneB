package com.saneb.domain.announcementattachment.classification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.*;

class AttachmentDocumentRoleClassifierTest {
    private final AttachmentDocumentRoleClassifier classifier = new AttachmentDocumentRoleClassifier();
    private static final String SECTIONS = "\n지원대상: 소상공인\n지원내용: 경영지원금\n신청기간: 2026년 9월";

    @ParameterizedTest @CsvSource({"소상공인 지원사업 공고,NOTICE", "사업 지원 안내,GUIDE"})
    void primaryRoleRequiresHeadingAndAllThreeSections(String heading, String expected) {
        var result = classifier.selectAssessment(extraction(heading + SECTIONS));
        assertThat(result.roleCode()).isEqualTo(expected);
        assertThat(result.reasonCode()).isEqualTo("ROLE_TEXT_STRUCTURE_MATCHED");
        assertThat(result.evidence()).hasSize(4);
    }
    @Test void formRequiresApplicantAndSignatureRatherThanFilenameOrMention() {
        var result = classifier.selectAssessment(extraction("경영지원 신청서\n신청인: \n사업자등록번호: \n(서명)"));
        assertThat(result.roleCode()).isEqualTo("FORM");
        assertThat(result.evidence()).hasSize(3);
        assertThat(classifier.selectAssessment(extraction("신청서\n작성 방법을 읽어주세요.")).roleCode()).isEqualTo("UNKNOWN");
    }
    @Test void referenceRequiresFaqAndBothQuestionAndAnswer() {
        assertThat(classifier.selectAssessment(extraction("지원사업 FAQ\nQ. 신청은 언제 합니까?\nA. 공고문을 확인하세요.")).roleCode()).isEqualTo("REFERENCE");
        assertThat(classifier.selectAssessment(extraction("지원사업 FAQ\nQ. 신청은 언제 합니까?")).roleCode()).isEqualTo("UNKNOWN");
        assertThat(classifier.selectAssessment(extraction("참고자료\n소상공인 지원금")).roleCode()).isEqualTo("UNKNOWN");
    }
    @ParameterizedTest @ValueSource(strings = {"신청인", "신청 인", "사업자등록번호", "사업자 등록 번호", "성명", "성   명", "성 명：", "○ 성 명", "1. 신청 인"})
    void formRecognizesWholeInputLabelWithoutRequiringColon(String label) {
        var result = classifier.selectAssessment(extraction("지원 신청서\n" + label + "\n신청인: (서명 또는 인)"));
        assertThat(result.roleCode()).isEqualTo("FORM");
        assertThat(result.evidence()).extracting(AttachmentDocumentRoleClassifier.Evidence::ruleCode)
                .containsExactly("FORM_HEADING", "APPLICANT_FIELD", "SIGNATURE_FIELD");
        var evidence = result.evidence().get(1);
        assertThat(evidence.endOffset() - evidence.startOffset()).isEqualTo(label.codePointCount(0, label.length()));
    }
    @ParameterizedTest @ValueSource(strings = {"(서명 또는 인)", "( 서명  또는  인 )", "신청인: (서명 또는 인)", "(서명)", "(인)", "서명 또는 인"})
    void formRequiresSignatureMarkerAtEndOfItsOwnLine(String signature) {
        assertThat(classifier.selectAssessment(extraction("지원 신청서\n성 명\n" + signature)).roleCode()).isEqualTo("FORM");
    }
    @ParameterizedTest @ValueSource(strings = {"성명을 입력하세요", "신청인에게 안내", "사업자등록번호 확인 방법", "대표자", "성명 생년월일", "성명확인", "신청인 여부", "성명 (필수)"})
    void arbitraryApplicantMentionsCannotReplaceAWholeField(String label) {
        var result = classifier.selectAssessment(extraction("지원 신청서\n" + label + "\n(서명 또는 인)"));
        assertThat(result.roleCode()).isEqualTo("UNKNOWN");
        assertThat(result.reasonCode()).isEqualTo("ROLE_STRUCTURE_INCOMPLETE");
    }
    @ParameterizedTest @ValueSource(strings = {"서명", "인", "(서명 또는 인)을 기재하세요", "(서명 또는 인) 제출", "서명 또는\n인", "(서명 또는 인]"})
    void signatureMentionOrSplitMarkerDoesNotCompleteAForm(String signature) {
        assertThat(classifier.selectAssessment(extraction("지원 신청서\n성 명\n" + signature)).roleCode()).isEqualTo("UNKNOWN");
    }
    @Test void markerImprovementDoesNotPromoteMissingHeadingMixedDocumentOrUncertainBlock() {
        assertThat(classifier.selectAssessment(extraction("기타 안내\n성 명\n(서명 또는 인)")).roleCode()).isEqualTo("UNKNOWN");
        assertThat(classifier.selectAssessment(extraction("지원사업 공고" + SECTIONS + "\n지원 신청서\n성 명\n(서명 또는 인)"))
                .reasonCode()).isEqualTo("MIXED_DOCUMENT_ROLES");
        String text = "지원 신청서\n성 명\n(서명 또는 인)";
        int split = text.indexOf("또는");
        var blocks = List.of(new AttachmentSetEvidence.Block(0, 0, split, "a", true, "a"),
                new AttachmentSetEvidence.Block(1, split, text.length(), "b", true, "b"));
        assertThat(classifier.selectAssessment(new AttachmentSetEvidence.Extraction("COMPLETE_TEXT", text, blocks, 1, 1)).roleCode()).isEqualTo("UNKNOWN");
    }
    @Test void mixedNoticeAndAttachedFormRemainUnknownEvenWithCompletePrimaryStructure() {
        var result = classifier.selectAssessment(extraction("소상공인 지원사업 공고" + SECTIONS + "\n경영지원 신청서\n신청인: \n(서명)"));
        assertThat(result.roleCode()).isEqualTo("UNKNOWN");
        assertThat(result.reasonCode()).isEqualTo("MIXED_DOCUMENT_ROLES");
        assertThat(result.evidence()).extracting(AttachmentDocumentRoleClassifier.Evidence::ruleCode)
                .containsExactly("NOTICE_HEADING", "FORM_HEADING");
    }
    @ParameterizedTest @ValueSource(strings = {"지원대상", "지원내용", "신청기간"})
    void eachMissingPrimarySectionRequiresReview(String missing) {
        var result = classifier.selectAssessment(extraction(("지원사업 공고" + SECTIONS).replace(missing, "기타")));
        assertThat(result.roleCode()).isEqualTo("UNKNOWN");
        assertThat(result.reasonCode()).isEqualTo("ROLE_STRUCTURE_INCOMPLETE");
    }
    @ParameterizedTest @ValueSource(strings = {"PARTIAL_TEXT", "OCR_REQUIRED", "FAILED", "ENCRYPTED", "CORRUPT"})
    void nonCompleteTextNeverProducesRoleEvenWhenTextLooksValid(String quality) {
        var original = extraction("지원사업 공고" + SECTIONS);
        var result = classifier.selectAssessment(new AttachmentSetEvidence.Extraction(quality, original.text(), original.blocks(), 1, 1));
        assertThat(result.roleCode()).isEqualTo("UNKNOWN");
        assertThat(result.reasonCode()).isEqualTo("COMPLETE_TEXT_REQUIRED");
        assertThat(result.evidence()).isEmpty();
    }
    @Test void noCrossFileCombinationAndNoRoleFromKeywordsOnly() {
        assertThat(classifier.selectAssessment(extraction("지원사업 공고\n지원대상: 소상공인")).roleCode()).isEqualTo("UNKNOWN");
        assertThat(classifier.selectAssessment(extraction("지원내용: 지원금\n신청기간: 9월")).roleCode()).isEqualTo("UNKNOWN");
        assertThat(classifier.selectAssessment(extraction("소상공인 지원금 본문입니다.\n공고문을 참고하세요.")).roleCode()).isEqualTo("UNKNOWN");
    }
    @Test void documentHeadingMustBeInFirstThreeNonblankLinesAndFirstSixHundredCodePoints() {
        assertThat(classifier.selectAssessment(extraction("안내\n안내\n안내\n지원사업 공고" + SECTIONS)).reasonCode()).isEqualTo("INITIAL_HEADING_REQUIRED");
        assertThat(classifier.selectAssessment(extraction("가".repeat(600) + "\n지원사업 공고" + SECTIONS)).reasonCode()).isEqualTo("INITIAL_HEADING_REQUIRED");
        assertThat(classifier.selectAssessment(extraction("\n\n지원사업 공고" + SECTIONS)).roleCode()).isEqualTo("NOTICE");
    }
    @Test void evidenceUsesActualCodePointOffsetsWithoutCopyingOriginalText() throws Exception {
        String text = "😀 지원사업 공고" + SECTIONS;
        var result = classifier.selectAssessment(extraction(text));
        assertThat(result.roleCode()).isEqualTo("NOTICE");
        var header = result.evidence().getFirst();
        assertThat(header.startOffset()).isZero();
        assertThat(header.endOffset()).isEqualTo(text.substring(0, text.indexOf('\n')).codePointCount(0, text.indexOf('\n')));
        String json = new ObjectMapper().writeValueAsString(result);
        assertThat(json).doesNotContain("소상공인", "경영지원금", "2026년", "😀");
        assertThat(result.ruleVersion()).isEqualTo(AttachmentDocumentRoleClassifier.VERSION);
        assertThat(result.rulesHash()).matches("[0-9a-f]{64}");
        assertThat(result.textHash()).matches("[0-9a-f]{64}");
        assertThat(result.blocksHash()).matches("[0-9a-f]{64}");
    }
    @Test void completeEvidenceCanUseSeveralBlocksWithinSameFile() {
        String text = "지원사업 공고" + SECTIONS;
        var blocks = new java.util.ArrayList<AttachmentSetEvidence.Block>();
        int start = 0;
        for (String line : text.split("\n")) {
            int end = start + line.codePointCount(0, line.length());
            blocks.add(new AttachmentSetEvidence.Block(blocks.size(), start, end, "p:" + blocks.size(), true, "p:" + blocks.size()));
            start = end + 1;
        }
        var result = classifier.selectAssessment(new AttachmentSetEvidence.Extraction("COMPLETE_TEXT", text, blocks, 1, 1));
        assertThat(result.roleCode()).isEqualTo("NOTICE");
        assertThat(result.evidence()).extracting(AttachmentDocumentRoleClassifier.Evidence::blockIndex).containsExactly(0, 1, 2, 3);
    }
    @Test void lineAcrossBlockBoundaryIsNotInventedAsSingleEvidence() {
        String text = "지원사업 공고" + SECTIONS;
        var blocks = List.of(new AttachmentSetEvidence.Block(0, 0, 3, "a", true, "a"),
                new AttachmentSetEvidence.Block(1, 3, text.length(), "b", true, "b"));
        assertThat(classifier.selectAssessment(new AttachmentSetEvidence.Extraction("COMPLETE_TEXT", text, blocks, 1, 1)).roleCode()).isEqualTo("UNKNOWN");
    }
    @Test void uncertainStructureStaysUnknown() {
        String text = "지원사업 공고" + SECTIONS;
        var blocks = List.of(new AttachmentSetEvidence.Block(0, 0, text.length(), "a", false, "a"));
        assertThat(classifier.selectAssessment(new AttachmentSetEvidence.Extraction("COMPLETE_TEXT", text, blocks, 1, 1)).reasonCode()).isEqualTo("STRUCTURE_UNCERTAIN");
    }
    @Test void omittedTextOverlappingOffsetsAndWrongBlockIndexesAreRejected() {
        String text = "지원사업 공고" + SECTIONS;
        for (var blocks : List.of(
                List.of(new AttachmentSetEvidence.Block(0, 1, text.length(), "a", true, "a")),
                List.of(new AttachmentSetEvidence.Block(0, 0, 10, "a", true, "a"), new AttachmentSetEvidence.Block(1, 9, text.length(), "b", true, "b")),
                List.of(new AttachmentSetEvidence.Block(1, 0, text.length(), "a", true, "a"))))
            assertThatIllegalArgumentException().isThrownBy(() -> classifier.selectAssessment(new AttachmentSetEvidence.Extraction("COMPLETE_TEXT", text, blocks, 1, 1)));
    }
    @Test void ruleAndInputFingerprintsAreDeterministicAndInputChangesAreVisible() {
        var first = classifier.selectAssessment(extraction("지원사업 공고" + SECTIONS));
        assertThat(classifier.selectAssessment(extraction("지원사업 공고" + SECTIONS))).isEqualTo(first);
        var changed = classifier.selectAssessment(extraction("지원사업 공고" + SECTIONS + "\n문의"));
        assertThat(changed.rulesHash()).isEqualTo(first.rulesHash());
        assertThat(changed.textHash()).isNotEqualTo(first.textHash());
        assertThat(changed.blocksHash()).isNotEqualTo(first.blocksHash());
    }
    @Test void resourceLimitDoesNotTurnTruncatedEvidenceIntoCompleteRole() {
        String text = "지원사업 공고" + SECTIONS + "\n기타".repeat(20000);
        assertThat(classifier.selectAssessment(extraction(text)).reasonCode()).isEqualTo("ROLE_ANALYSIS_LIMIT");
        assertThatIllegalArgumentException().isThrownBy(() -> classifier.selectAssessment(extraction("가".repeat(1_000_001))));
    }
    private static AttachmentSetEvidence.Extraction extraction(String text) {
        return new AttachmentSetEvidence.Extraction("COMPLETE_TEXT", text, List.of(new AttachmentSetEvidence.Block(0, 0,
                text.codePointCount(0, text.length()), "page:1", true, "page:1")), 1, 1);
    }
}
