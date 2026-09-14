package com.saneb.domain.announcementattachment.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.common.error.ApiException;
import com.saneb.domain.announcementattachment.dao.*;
import com.saneb.domain.announcementattachment.service.impl.AnnouncementAttachmentHistoryServiceImpl;
import com.saneb.domain.announcementattachment.vo.*;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AnnouncementAttachmentHistoryServiceTest {
    private final AnnouncementAttachmentCurrentDao current=mock(AnnouncementAttachmentCurrentDao.class);
    private final AnnouncementAttachmentEvaluationDao evaluations=mock(AnnouncementAttachmentEvaluationDao.class);
    private final AnnouncementAttachmentHistoryDao history=mock(AnnouncementAttachmentHistoryDao.class);
    private final AnnouncementAttachmentHistoryService service=new AnnouncementAttachmentHistoryServiceImpl(current,evaluations,history,new ObjectMapper());
    private final UUID source=UUID.randomUUID(),evaluation=UUID.randomUUID(),file=UUID.randomUUID();
    private final OffsetDateTime time=OffsetDateTime.parse("2026-09-10T01:00:00Z");
    private AttachmentCurrentSourceRow source(boolean required,boolean currentResult) {
        var row=mock(AttachmentCurrentSourceRow.class);when(row.sourceId()).thenReturn(source);
        when(row.reviewRequired()).thenReturn(required);when(row.attachmentDecisionId()).thenReturn(currentResult?evaluation:null);
        when(row.sourceVersion()).thenReturn(8);when(row.attachmentVersion()).thenReturn(12);
        when(current.selectSourceDetails(source)).thenReturn(row);return row;
    }
    private AttachmentEvaluationRows.Evaluation evaluation(boolean isCurrent,String warnings) {
        var row=new AttachmentEvaluationRows.Evaluation(evaluation,source,UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),
                UUID.randomUUID(),"ATTACHMENT_V1","a".repeat(64),"b".repeat(64),"REVIEW_REQUIRED","ATTACHMENT_INCOMPLETE",warnings,isCurrent,time);
        when(evaluations.selectEvaluationDetails(source,evaluation)).thenReturn(row);return row;
    }
    @Test void enforceCurrentAndStoredVersionsAreReportedWithoutOverwritingStatus() {
        source(true,true);var row=evaluation(true,"[\"ATTACHMENT_INCOMPLETE\"]");
        when(history.selectAutoTargetList(source,evaluation)).thenReturn(List.of("BUSINESS","PERSONAL"));
        when(history.selectAutoSupportList(source,evaluation)).thenReturn(List.of("POLICY_FINANCE"));
        when(history.selectInputCount(source,evaluation)).thenReturn(2L);when(history.selectMatchCount(source,evaluation,null)).thenReturn(5L);
        var response=service.selectEvaluationDetails(source,evaluation);
        assertThat(response.evaluation().usageCode()).isEqualTo("CURRENT_EFFECTIVE");
        assertThat(response.evaluation().semanticStatusCode()).isEqualTo("REVIEW_REQUIRED");
        assertThat(response.evaluation().ruleReleaseId()).isEqualTo(row.ruleReleaseId());
        assertThat(response.autoTargetCategoryCodes()).containsExactly("BUSINESS","PERSONAL");
        assertThat(response.inputCount()).isEqualTo(2);assertThat(response.matchCount()).isEqualTo(5);
        assertThat(response.currentSourceVersion()).isEqualTo(8);assertThat(response.currentAttachmentVersion()).isEqualTo(12);
    }
    @Test void collectOnlyCurrentRemainsPreviewAndHistoricalFlagIsNotEnoughForEffective() {
        var currentRow=source(false,true);var row=evaluation(true,"[]");
        when(history.selectEvaluationList(source,0,20)).thenReturn(List.of(row));when(history.selectEvaluationCount(source)).thenReturn(1L);
        assertThat(service.selectEvaluationList(source,1,20).items().getFirst().usageCode()).isEqualTo("CURRENT_PREVIEW");
        when(currentRow.attachmentDecisionId()).thenReturn(UUID.randomUUID());when(currentRow.reviewRequired()).thenReturn(true);
        assertThat(service.selectEvaluationList(source,1,20).items().getFirst().usageCode()).isEqualTo("NOT_CURRENT");
    }
    @Test void pendingHasNoCurrentHistoryEvenWhenAnOldFlagIsTrue() {
        source(true,false);var row=evaluation(true,"[]");when(history.selectEvaluationList(source,20,10)).thenReturn(List.of(row));
        when(history.selectEvaluationCount(source)).thenReturn(21L);
        var response=service.selectEvaluationList(source,3,10);
        assertThat(response.items().getFirst().usageCode()).isEqualTo("NOT_CURRENT");assertThat(response.totalPages()).isEqualTo(3);
        assertThat(response.items().getFirst().evaluatedAt()).isEqualTo(time);
        verify(history).selectEvaluationList(source,20,10);verify(history).selectEvaluationCount(source);
    }
    @Test void nonCurrentEvaluationCannotBePromotedByMatchingPointerAlone() {
        source(true,true);var row=evaluation(false,"[]");when(history.selectEvaluationList(source,0,20)).thenReturn(List.of(row));
        assertThat(service.selectEvaluationList(source,1,20).items().getFirst().usageCode()).isEqualTo("NOT_CURRENT");
    }
    @Test void failedInputWithNoExtractionAndOriginalSuccessTimeArePreserved() {
        source(true,true);evaluation(true,"[]");UUID extraction=UUID.randomUUID(),original=UUID.randomUUID();
        when(history.selectInputList(source,evaluation,0,20)).thenReturn(List.of(
                new AttachmentHistoryRows.Input(file,extraction,"GUIDE","COMPLETE_TEXT","SUCCEEDED",null,"COMPLETE_TEXT",null,time,original),
                new AttachmentHistoryRows.Input(UUID.randomUUID(),null,"NOTICE","FAILED","FAILED","NETWORK_TIMEOUT",null,null,null,null)));
        when(history.selectInputCount(source,evaluation)).thenReturn(2L);
        var response=service.selectInputList(source,evaluation,1,20);
        assertThat(response.items()).hasSize(2);assertThat(response.items().getFirst().extractionId()).isEqualTo(extraction);
        assertThat(response.items().getFirst().extractedAt()).isEqualTo(time);assertThat(response.items().getFirst().reusedFromExtractionId()).isEqualTo(original);
        assertThat(response.items().get(1).extractionId()).isNull();assertThat(response.items().get(1).downloadErrorCode()).isEqualTo("NETWORK_TIMEOUT");
    }
    @Test void matchesKeepFrozenFileTermAndOffsetsWithTheSameCountScope() {
        source(true,true);evaluation(true,"[]");when(history.selectInputExists(source,evaluation,file)).thenReturn(true);
        var match=new AttachmentHistoryRows.Match(UUID.randomUUID(),file,UUID.randomUUID(),"NOTICE",UUID.randomUUID(),"BUSINESS",UUID.randomUUID(),
                "BUSINESS_SMALL",UUID.randomUUID(),"소상공인","TAG",3,100,104);
        when(history.selectMatchList(source,evaluation,file,10,10)).thenReturn(List.of(match));when(history.selectMatchCount(source,evaluation,file)).thenReturn(11L);
        var response=service.selectMatchList(source,evaluation,file,2,10);
        assertThat(response.items().getFirst().keywordTermId()).isEqualTo(match.keywordTermId());
        assertThat(response.items().getFirst().extractionId()).isEqualTo(match.extractionId());
        assertThat(response.items().getFirst().blockIndex()).isEqualTo(3);assertThat(response.items().getFirst().startOffset()).isEqualTo(100);
        assertThat(response.totalCount()).isEqualTo(11);verify(history).selectMatchCount(source,evaluation,file);
    }
    @Test void fileFromAnotherEvaluationIs404NotEmptySuccess() {
        source(true,true);evaluation(true,"[]");
        assertThatThrownBy(()->service.selectMatchList(source,evaluation,file,1,20)).isInstanceOf(ApiException.class);
        verify(history,never()).selectMatchList(any(),any(),any(),anyInt(),anyInt());verify(history,never()).selectMatchCount(any(),any(),any());
    }
    @Test void hiddenSourceDoesNotQueryAnyHistoricalEvidence() {
        assertThatThrownBy(()->service.selectEvaluationList(source,1,20)).isInstanceOf(ApiException.class).hasMessageContaining("제목 제외");
        assertThatThrownBy(()->service.selectInputList(source,evaluation,1,20)).isInstanceOf(ApiException.class);
        verifyNoInteractions(evaluations,history);
    }
    @Test void missingOrForeignEvaluationDoesNotQueryTagsInputsOrMatches() {
        source(false,false);
        assertThatThrownBy(()->service.selectEvaluationDetails(source,evaluation)).isInstanceOf(ApiException.class);
        assertThatThrownBy(()->service.selectMatchList(source,evaluation,null,1,20)).isInstanceOf(ApiException.class);
        verifyNoInteractions(history);
    }
    @ParameterizedTest @ValueSource(ints={0,-1,101,Integer.MAX_VALUE})
    void invalidPageSizeFailsBeforeDatabase(int size) {
        assertThatThrownBy(()->service.selectEvaluationList(source,1,size)).isInstanceOf(ApiException.class).hasMessageContaining("1~100");
        verifyNoInteractions(current,evaluations,history);
    }
    @Test void pageBoundsFailBeforeOffsetOverflow() {
        assertThatThrownBy(()->service.selectMatchList(source,evaluation,null,Integer.MAX_VALUE,100)).isInstanceOf(ApiException.class);
        assertThatThrownBy(()->service.selectInputList(source,evaluation,0,20)).isInstanceOf(ApiException.class);
        verifyNoInteractions(current,evaluations,history);
    }
    @Test void invalidStoredWarningDoesNotExposeItsContentOrPretendThereAreNoWarnings() {
        source(true,true);var row=evaluation(true,"[\"raw unexpected detail\"]");when(history.selectEvaluationList(source,0,20)).thenReturn(List.of(row));
        assertThatThrownBy(()->service.selectEvaluationList(source,1,20)).isInstanceOf(IllegalStateException.class)
                .hasMessageNotContaining("raw unexpected detail").hasMessageContaining("경고 코드");
    }
}
