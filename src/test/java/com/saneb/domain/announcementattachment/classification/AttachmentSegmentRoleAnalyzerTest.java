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
