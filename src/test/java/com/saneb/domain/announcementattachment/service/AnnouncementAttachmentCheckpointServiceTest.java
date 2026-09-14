package com.saneb.domain.announcementattachment.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.common.error.ApiException;
import com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentEvidenceDao;
import com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentJobDao;
import com.saneb.domain.announcementattachment.service.impl.AnnouncementAttachmentEvidenceServiceImpl;
import com.saneb.domain.announcementattachment.vo.*;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** DB 실행 증거가 아닌 checkpoint 검증/호출 경계 단위 테스트. */
class AnnouncementAttachmentCheckpointServiceTest {
    private final ObjectMapper mapper=new ObjectMapper();
    private final AnnouncementAttachmentJobDao jobs=mock(AnnouncementAttachmentJobDao.class);
    private final AnnouncementAttachmentEvidenceDao evidence=mock(AnnouncementAttachmentEvidenceDao.class);
    private final AnnouncementAttachmentEvidenceService service=new AnnouncementAttachmentEvidenceServiceImpl(jobs,evidence,
            mock(com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentRetryDao.class),
            mock(com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentRoleDao.class),mapper);
    private AttachmentJobRow job;
    private AttachmentSourceContextRow source;
    private AttachmentSetEvidence.File file;

    @BeforeEach void configure() throws Exception {
        var execution=new AttachmentExecutionSnapshot("FIXTURE","a".repeat(64),"1.0.0","1.0.0","b".repeat(64));
        job=new AttachmentJobRow(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),
                UUID.randomUUID(),null,null,1,0,1,"RUNNING",1,null,UUID.randomUUID(),OffsetDateTime.now().plusMinutes(2),null,
                UUID.randomUUID(),"c".repeat(64),mapper.writeValueAsString(execution),83886080L,15L,0);
        source=new AttachmentSourceContextRow(job.sourceId(),"BIZINFO","PRODUCTION","REVIEW_REQUIRED",job.baseEvaluationId(),
                job.contentVersionId(),job.ruleReleaseId(),"COMBINATION_MATCHED",0,1,false,null,null);
        file=new AttachmentSetEvidence.File(new AttachmentSetEvidence.Locator("FIXTURE","/download",Map.of("fileId","qa1")),
                "공고문.pdf","PDF","NOTICE","PROFILE","SUCCEEDED",15,"d".repeat(64),null,
                new AttachmentSetEvidence.Extraction("COMPLETE_TEXT","소상공인 지원",List.of(
                        new AttachmentSetEvidence.Block(0,0,7,"p1",true,"page:1")),1,100,System.currentTimeMillis()-1000));
        when(jobs.selectJobDetails(job.jobId())).thenReturn(job);
        when(jobs.selectSourceContextDetailsForUpdate(job.sourceId())).thenReturn(source);
        when(jobs.selectOwnedJobDetailsForUpdate(job.jobId(),job.leaseToken())).thenReturn(job);
        when(evidence.insertFileCheckpoint(any(),any(),anyString(),anyString())).thenReturn(1);
    }
    @Test void storesOnlyValidatedSuccessfulEvidenceAfterSourceThenJobLock() {
        assertThat(service.saveFileCheckpoint(job.jobId(),job.leaseToken(),file)).isTrue();
        var order=inOrder(jobs,evidence);
        order.verify(jobs).selectSourceContextDetailsForUpdate(job.sourceId());
        order.verify(jobs).selectOwnedJobDetailsForUpdate(job.jobId(),job.leaseToken());
        order.verify(evidence).insertFileCheckpoint(eq(job.jobId()),eq(job.leaseToken()),matches("[0-9a-f]{64}"),contains("completedAtEpochMs"));
        verify(evidence,never()).insertSet(any());
        verify(evidence,never()).updateJobSet(any(),any(),any());
    }
    @Test void expiredLeaseCannotWriteCheckpoint() {
        when(jobs.selectOwnedJobDetailsForUpdate(job.jobId(),job.leaseToken())).thenReturn(null);
        assertThat(service.saveFileCheckpoint(job.jobId(),job.leaseToken(),file)).isFalse();
        verifyNoInteractions(evidence);
    }
    @Test void changedSourceConflictsBeforeCheckpointWrite() {
        when(jobs.selectSourceContextDetailsForUpdate(job.sourceId())).thenReturn(new AttachmentSourceContextRow(source.sourceId(),
                source.providerCode(),source.dataPurposeCode(),source.semanticStatusCode(),source.baseEvaluationId(),source.contentVersionId(),
                source.ruleReleaseId(),source.titleStageCode(),1,2,false,null,null));
        assertThat(service.saveFileCheckpoint(job.jobId(),job.leaseToken(),file)).isFalse();
        verify(jobs).updateJobConflict(job.jobId(),job.leaseToken());
        verifyNoInteractions(evidence);
    }
    @ParameterizedTest @ValueSource(strings={"PARTIAL_TEXT","OCR_REQUIRED","FAILED","TIMEOUT","ENCRYPTED","CORRUPT","UNSUPPORTED","ISOLATION_UNAVAILABLE"})
    void unsuccessfulExtractionCannotBecomeReusableCheckpoint(String quality) {
        var changed=selectWithExtraction(new AttachmentSetEvidence.Extraction(quality,"",List.of(),null,10,System.currentTimeMillis()));
        assertThatThrownBy(() -> service.saveFileCheckpoint(job.jobId(),job.leaseToken(),changed))
                .isInstanceOf(ApiException.class).hasMessageContaining("완전 추출 성공");
        verifyNoInteractions(evidence);
    }
    @Test void emptyCompleteTextAndFutureTimestampAreRejected() {
        assertThatThrownBy(() -> service.saveFileCheckpoint(job.jobId(),job.leaseToken(),selectWithExtraction(
                new AttachmentSetEvidence.Extraction("COMPLETE_TEXT","",List.of(),1,100,System.currentTimeMillis()))))
                .isInstanceOf(ApiException.class).hasMessageContaining("완전 추출에는");
        assertThatThrownBy(() -> service.saveFileCheckpoint(job.jobId(),job.leaseToken(),selectWithExtraction(
                new AttachmentSetEvidence.Extraction("COMPLETE_TEXT",file.extraction().text(),file.extraction().blocks(),1,100,System.currentTimeMillis()+60000))))
                .isInstanceOf(ApiException.class).hasMessageContaining("미래 시각");
        verifyNoInteractions(evidence);
    }
    @Test void sameRequestIsIdempotentButDifferentSuccessCannotOverwrite() throws Exception {
        when(evidence.insertFileCheckpoint(any(),any(),anyString(),anyString())).thenReturn(0);
        when(evidence.selectFileCheckpoint(eq(job.jobId()),eq(job.leaseToken()),anyString())).thenReturn(mapper.writeValueAsString(file));
        assertThat(service.saveFileCheckpoint(job.jobId(),job.leaseToken(),file)).isTrue();
        var changed=selectWithExtraction(new AttachmentSetEvidence.Extraction("COMPLETE_TEXT",file.extraction().text(),file.extraction().blocks(),1,200,
                file.extraction().completedAtEpochMs()));
        assertThatThrownBy(() -> service.saveFileCheckpoint(job.jobId(),job.leaseToken(),changed))
                .isInstanceOf(ApiException.class).hasMessageContaining("덮어쓸 수 없습니다");
    }
    @Test void readsExactLocatorWithoutRefreshingTimestampOrExposingOtherJobs() throws Exception {
        when(evidence.selectFileCheckpoint(eq(job.jobId()),eq(job.leaseToken()),anyString())).thenReturn(mapper.writeValueAsString(file));
        assertThat(service.selectFileCheckpoint(job.jobId(),job.leaseToken(),file.locator())).contains(file);
        assertThat(service.selectFileCheckpoint(UUID.randomUUID(),job.leaseToken(),file.locator())).isEmpty();
        verify(evidence,never()).insertFileCheckpoint(any(),any(),anyString(),anyString());
    }
    @Test void mismatchedLocatorAndCorruptJsonFailClosed() throws Exception {
        when(evidence.selectFileCheckpoint(any(),any(),anyString())).thenReturn(mapper.writeValueAsString(file));
        var foreign=new AttachmentSetEvidence.Locator("FIXTURE","/download",Map.of("fileId","other"));
        assertThatThrownBy(() -> service.selectFileCheckpoint(job.jobId(),job.leaseToken(),foreign)).isInstanceOf(ApiException.class);
        when(evidence.selectFileCheckpoint(any(),any(),anyString())).thenReturn("invalid fixture");
        assertThatThrownBy(() -> service.selectFileCheckpoint(job.jobId(),job.leaseToken(),file.locator()))
                .isInstanceOf(ApiException.class).hasMessageContaining("해석할 수 없습니다");
    }
    private AttachmentSetEvidence.File selectWithExtraction(AttachmentSetEvidence.Extraction value) {
        return new AttachmentSetEvidence.File(file.locator(),file.displayName(),file.detectedType(),file.role(),file.roleOrigin(),
                file.downloadStatus(),file.downloadedBytes(),file.binaryHash(),file.failureCode(),value);
    }
}
