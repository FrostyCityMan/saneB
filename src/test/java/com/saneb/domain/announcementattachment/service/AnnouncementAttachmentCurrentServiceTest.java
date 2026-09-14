package com.saneb.domain.announcementattachment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.saneb.common.error.ApiException;
import com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentCurrentDao;
import com.saneb.domain.announcementattachment.service.impl.AnnouncementAttachmentCurrentServiceImpl;
import com.saneb.domain.announcementattachment.vo.AttachmentCurrentSourceRow;
import com.saneb.domain.announcementattachment.vo.AttachmentSourceSearchCondition;
import com.saneb.domain.announcementsource.dao.AnnouncementSourceDao;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AnnouncementAttachmentCurrentServiceTest {
    private final AnnouncementAttachmentCurrentDao dao=mock(AnnouncementAttachmentCurrentDao.class);
    private final AnnouncementSourceDao sources=mock(AnnouncementSourceDao.class);
    private final AnnouncementAttachmentCurrentService service=new AnnouncementAttachmentCurrentServiceImpl(dao,sources);
    private final UUID source=UUID.randomUUID(),base=UUID.randomUUID(),attachment=UUID.randomUUID();

    private AttachmentCurrentSourceRow selectRow(boolean required, boolean completed) {
        var row=mock(AttachmentCurrentSourceRow.class);
        when(row.sourceId()).thenReturn(source);
        when(row.baseDecisionId()).thenReturn(base);
        when(row.baseStatus()).thenReturn("REVIEW_REQUIRED");
        when(row.baseReason()).thenReturn("BODY_UNAVAILABLE");
        when(row.baseTargetCodes()).thenReturn("BUSINESS,PERSONAL");
        when(row.reviewRequired()).thenReturn(required);
        when(row.sourceVersion()).thenReturn(4);
        when(row.attachmentVersion()).thenReturn(3);
        when(row.jobStatus()).thenReturn(completed ? "SUCCEEDED" : "NOT_REQUESTED");
        when(row.effectiveStatus()).thenReturn(completed?"ACCEPTED":"REVIEW_REQUIRED");
        when(row.effectiveReason()).thenReturn(completed?"EXTENDED_TARGET_SUPPORT_CONFIRMED":"ATTACHMENT_PENDING");
        if(completed){
            when(row.setId()).thenReturn(UUID.randomUUID());
            when(row.setHash()).thenReturn("a".repeat(64));
            when(row.discoveryStatus()).thenReturn("FOUND");
            when(row.discoveryComplete()).thenReturn(true);
            when(row.discoveredCount()).thenReturn(1);
            when(row.processedCount()).thenReturn(1);
            when(row.attachmentDecisionId()).thenReturn(attachment);
            when(row.attachmentStatus()).thenReturn("ACCEPTED");
            when(row.attachmentReason()).thenReturn("EXTENDED_TARGET_SUPPORT_CONFIRMED");
            when(row.attachmentTargetCodes()).thenReturn("BUSINESS");
            when(row.attachmentSupportCodes()).thenReturn("POLICY_FINANCE,GRANT");
        }
        when(dao.selectSourceDetails(source)).thenReturn(row);
        return row;
    }
    @Test void collectOnlyKeepsBaseEffectiveAndShowsAcceptedPreviewSeparately() {
        selectRow(false,true);
        var response=service.selectClassificationDetails(source);
        assertThat(response.effectiveClassification()).isEqualTo(response.baseClassification());
        assertThat(response.effectiveClassification().decisionId()).isEqualTo(base);
        assertThat(response.effectiveClassification().semanticStatusCode()).isEqualTo("REVIEW_REQUIRED");
        assertThat(response.previewClassification().decisionId()).isEqualTo(attachment);
        assertThat(response.previewClassification().supportTypeCodes()).containsExactly("POLICY_FINANCE","GRANT");
        assertThat(response.processingFlow().statusCode()).isEqualTo("NOT_APPLIED");
        assertThat(response.processingFlow().isAutomaticAnalysisComplete()).isFalse();
    }
    @Test void enforcePendingNeverReturnsOldDecisionTagsOrConfirmation() {
        var row=selectRow(true,false);
        when(row.attachmentStale()).thenReturn(true);
        when(row.confirmationStatus()).thenReturn("STALE");
        var response=service.selectClassificationDetails(source);
        assertThat(response.effectiveClassification().decisionId()).isNull();
        assertThat(response.effectiveClassification().reasonCode()).isEqualTo("ATTACHMENT_PENDING");
        assertThat(response.effectiveClassification().targetCategoryCodes()).isEmpty();
        assertThat(response.effectiveClassification().setHash()).isNull();
        assertThat(response.previewClassification()).isNull();
        assertThat(response.attachmentSummary().isStale()).isTrue();
        assertThat(response.confirmationStatusCode()).isEqualTo("STALE");
        assertThat(response.processingFlow().statusCode()).isEqualTo("EVIDENCE_STALE");
        assertThat(response.processingFlow().isFinalReviewAvailable()).isFalse();
    }
    @Test void enforceSealedUsesAttachmentAndDoesNotClearReviewRequirement() {
        selectRow(true,true);
        var response=service.selectClassificationDetails(source);
        assertThat(response.isAttachmentReviewRequired()).isTrue();
        assertThat(response.effectiveClassification().decisionId()).isEqualTo(attachment);
        assertThat(response.baseClassification().decisionId()).isEqualTo(base);
        assertThat(response.previewClassification()).isNull();
        assertThat(response.processingFlow().statusCode()).isEqualTo("READY_FOR_FINAL_REVIEW");
        assertThat(response.processingFlow().isFinalReviewAvailable()).isTrue();
    }
    @Test void listAndCountReceiveTheSameValidatedCondition() {
        when(dao.selectSourceList(any())).thenReturn(List.of());
        when(dao.selectSourceCount(any())).thenReturn(0L);
        var input=new AttachmentSourceSearchCondition(" BIZINFO ","REVIEW_REQUIRED","",null,null," 소상공인 ",null,null,2,10);
        service.selectSourceList(input);
        var expected=new AttachmentSourceSearchCondition("BIZINFO","REVIEW_REQUIRED",null,null,null,"소상공인",null,null,2,10);
        verify(dao).selectSourceList(expected);
        verify(dao).selectSourceCount(expected);
    }
    @ParameterizedTest @ValueSource(strings={"NOT_APPLIED","CLASSIFICATION_PENDING","CONFIGURATION_REQUIRED",
            "AUTOMATIC_PROCESSING","EVIDENCE_STALE","TECHNICAL_EXCEPTION","READY_FOR_FINAL_REVIEW",
            "FINAL_REVIEW_EXCEPTION","FINAL_REVIEW_CONFIRMED",""})
    void queueStatusIsValidatedAndSharedByListAndFullCount(String flow) {
        when(dao.selectSourceList(any())).thenReturn(List.of());
        when(dao.selectSourceCount(any())).thenReturn(41L);
        var response=service.selectSourceList(new AttachmentSourceSearchCondition(null,null,null,null,null,null,
                null,null,3,20," "+flow+" "));
        var expected=new AttachmentSourceSearchCondition(null,null,null,null,null,null,null,null,3,20,flow.isEmpty()?null:flow);
        verify(dao).selectSourceList(expected);
        verify(dao).selectSourceCount(expected);
        assertThat(response.totalCount()).isEqualTo(41);
        assertThat(response.totalPages()).isEqualTo(3);
    }
    @ParameterizedTest @ValueSource(strings={"READY","ready_for_final_review","EXCLUDED","READY_FOR_FINAL_REVIEW' OR 1=1--"})
    void unknownQueueStatusFailsBeforeSql(String flow) {
        assertThatThrownBy(()->service.selectSourceList(new AttachmentSourceSearchCondition(null,null,null,null,null,null,
                null,null,1,20,flow))).isInstanceOf(ApiException.class)
                .hasMessageContaining("processingFlowStatusCode").hasMessageContaining("TECHNICAL_EXCEPTION");
        verifyNoInteractions(dao,sources);
    }
    @ParameterizedTest @ValueSource(ints={0,-1,101,Integer.MAX_VALUE})
    void pageSizeOutOfRangeFailsBeforeSql(int size) {
        assertThatThrownBy(()->service.selectSourceList(new AttachmentSourceSearchCondition(null,null,null,null,null,null,null,null,1,size)))
                .isInstanceOf(ApiException.class).hasMessageContaining("1~100");
        verifyNoInteractions(dao,sources);
    }
    @Test void invalidStatusAndReversedDateRangeFailBeforeSql() {
        assertThatThrownBy(()->service.selectSourceList(new AttachmentSourceSearchCondition(null,"EXCLUDED",null,null,null,null,null,null,1,20)))
                .isInstanceOf(ApiException.class).hasMessageContaining("제목 제외");
        assertThatThrownBy(()->service.selectSourceList(new AttachmentSourceSearchCondition(null,null,null,null,null,null,
                LocalDate.of(2026,9,10),LocalDate.of(2026,9,9),1,20))).isInstanceOf(ApiException.class).hasMessageContaining("시작일");
        verifyNoInteractions(dao,sources);
    }
    @Test void hiddenOrMissingSourceDoesNotReadBody() {
        assertThatThrownBy(()->service.selectSourceDetails(source)).isInstanceOf(ApiException.class).hasMessageContaining("제목 제외");
        verifyNoInteractions(sources);
    }
}
