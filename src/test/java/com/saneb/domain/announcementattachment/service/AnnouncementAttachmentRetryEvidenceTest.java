package com.saneb.domain.announcementattachment.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.common.error.ApiException;
import com.saneb.domain.announcementattachment.dao.*;
import com.saneb.domain.announcementattachment.service.impl.AnnouncementAttachmentEvidenceServiceImpl;
import com.saneb.domain.announcementattachment.vo.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AnnouncementAttachmentRetryEvidenceTest {
    private final ObjectMapper mapper=new ObjectMapper();
    private final AnnouncementAttachmentJobDao jobs=mock(AnnouncementAttachmentJobDao.class);
    private final AnnouncementAttachmentEvidenceDao evidence=mock(AnnouncementAttachmentEvidenceDao.class);
    private final AnnouncementAttachmentRetryDao retries=mock(AnnouncementAttachmentRetryDao.class);
    private final AnnouncementAttachmentRoleDao roles=mock(AnnouncementAttachmentRoleDao.class);
    private final AnnouncementAttachmentEvidenceService service=new AnnouncementAttachmentEvidenceServiceImpl(jobs,evidence,retries,roles,mapper);
    private AttachmentJobRow job;
    private AttachmentSetEvidence.File fresh;
    private List<AttachmentRetryFileRow> plan;
    private UUID successfulExtraction;
    @BeforeEach void configure() throws Exception {
        var execution=new AttachmentExecutionSnapshot("FIXTURE","a".repeat(64),"1.0.0","1.0.0","b".repeat(64));
        job=new AttachmentJobRow(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),null,null,
                2,0,3,"RUNNING",1,null,UUID.randomUUID(),OffsetDateTime.now().plusMinutes(2),null,UUID.randomUUID(),"a".repeat(64),mapper.writeValueAsString(execution),
                8L,8L,0,"RETRY_FILES",UUID.randomUUID(),UUID.randomUUID());
        when(jobs.selectJobDetails(job.jobId())).thenReturn(job);when(jobs.selectOwnedJobDetailsForUpdate(job.jobId(),job.leaseToken())).thenReturn(job);
        when(jobs.selectSourceContextDetailsForUpdate(job.sourceId())).thenReturn(new AttachmentSourceContextRow(job.sourceId(),"BIZINFO","PRODUCTION","REVIEW_REQUIRED",
                job.baseEvaluationId(),job.contentVersionId(),job.ruleReleaseId(),"COMBINATION_MATCHED",0,3,true,job.policyId(),null));
        when(evidence.selectSetDetails(job.sourceId(),job.referenceSetId())).thenReturn(new AttachmentSetRow(job.referenceSetId(),job.sourceId(),job.contentVersionId(),job.policyId(),
                "FOUND","SEALED","a".repeat(64),execution.profileHash(),3,3,true,null,null,null,List.of()));
        var rows=new ArrayList<AttachmentRetryFileRow>();var originalFiles=new ArrayList<AttachmentRoleRows.File>();
        for(int i=0;i<3;i++) {
            var locator=new AttachmentSetEvidence.Locator("FIXTURE","/download",Map.of("fileId","qa"+i));String json=mapper.writeValueAsString(locator);
            String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json.getBytes(StandardCharsets.UTF_8)));
            UUID id=UUID.randomUUID(),extraction=i==0?UUID.randomUUID():null;if(i==0) successfulExtraction=extraction;
            rows.add(new AttachmentRetryFileRow(id,json,hash,"QA.pdf","NOTICE","MANUAL","PDF",i==1));
            originalFiles.add(new AttachmentRoleRows.File(id,extraction,"NOTICE","MANUAL",hash,i==0?"a".repeat(64):null,
                    i==0?"SUCCEEDED":"FAILED",i,i==0?"1.0.0":null,i==0?execution.extractorConfigHash():null));
            if(i==1) fresh=new AttachmentSetEvidence.File(locator,"QA.pdf","PDF","NOTICE","MANUAL","SUCCEEDED",8,"c".repeat(64),null,
                    new AttachmentSetEvidence.Extraction("COMPLETE_TEXT","지원 대상",List.of(new AttachmentSetEvidence.Block(0,0,5,"p1",true,"p1")),1,10,System.currentTimeMillis()-1000));
        }
        plan=List.copyOf(rows);when(retries.selectRetryFileList(job.jobId(),job.leaseToken())).thenReturn(plan);
        when(roles.selectFileCopyList(job.sourceId(),job.referenceSetId())).thenReturn(originalFiles);when(evidence.selectFileCount(job.sourceId(),job.referenceSetId())).thenReturn(3L);
        when(evidence.insertSet(any())).thenAnswer(call->{var set=(AttachmentEvidenceCommands.SetInsert)call.getArgument(0);
            when(evidence.selectSetDetails(job.sourceId(),set.setId())).thenReturn(new AttachmentSetRow(set.setId(),job.sourceId(),job.contentVersionId(),job.policyId(),
                    "FOUND","SEALED","b".repeat(64),execution.profileHash(),3,3,true,null,null,null,List.of()));return 1;});
        when(evidence.insertFile(any())).thenReturn(1);when(evidence.insertExtraction(any())).thenReturn(1);when(roles.insertFileCopy(any())).thenReturn(1);
        when(roles.insertExtractionCopy(any())).thenReturn(1);when(evidence.updateSetSealed(any(),any(),any(),anyBoolean(),anyInt(),any())).thenReturn(1);
        when(evidence.updateJobSet(any(),any(),any())).thenReturn(1);
    }
    private AttachmentSetEvidence result() { return new AttachmentSetEvidence("FOUND",true,List.of(fresh)); }
    @Test void copiesAllUnselectedEvidenceAndOnlyChargesNewlyReceivedBytes() {
        var saved=service.saveRetriedAttachmentSet(job.jobId(),job.leaseToken(),result()).orElseThrow();assertThat(saved.setId()).isNotEqualTo(job.referenceSetId());
        verify(roles,times(2)).insertFileCopy(any());var copy=ArgumentCaptor.forClass(AttachmentRoleRows.Copy.class);verify(roles).insertExtractionCopy(copy.capture());
        assertThat(copy.getValue().originalExtractionId()).isEqualTo(successfulExtraction);
        var freshWrite=ArgumentCaptor.forClass(AttachmentEvidenceCommands.FileInsert.class);verify(evidence).insertFile(freshWrite.capture());
        assertThat(freshWrite.getValue().sortOrder()).isEqualTo(1);assertThat(freshWrite.getValue().roleOrigin()).isEqualTo("MANUAL");
        assertThat(freshWrite.getValue().downloadedBytes()).isEqualTo(8);verify(evidence).updateSetSealed(any(),any(),eq("FOUND"),eq(true),eq(3),any());
        verify(evidence).deleteFileCheckpoints(job.jobId());
    }
    @Test void missingSelectionOrForgedRoleCannotSeal() {
        assertThatThrownBy(()->service.saveRetriedAttachmentSet(job.jobId(),job.leaseToken(),new AttachmentSetEvidence("FOUND",true,List.of())))
                .isInstanceOf(ApiException.class).hasMessageContaining("전체의 결과");
        var changed=new AttachmentSetEvidence.File(fresh.locator(),fresh.displayName(),fresh.detectedType(),"GUIDE","MANUAL",fresh.downloadStatus(),
                fresh.downloadedBytes(),fresh.binaryHash(),null,fresh.extraction());
        assertThatThrownBy(()->service.saveRetriedAttachmentSet(job.jobId(),job.leaseToken(),new AttachmentSetEvidence("FOUND",true,List.of(changed))))
                .isInstanceOf(ApiException.class).hasMessageContaining("역할을 바꾸지 않습니다");verify(evidence,never()).insertSet(any());
    }
    @Test void genericWholeCollectionPathCannotBypassRetryScope() {
        assertThatThrownBy(()->service.saveAttachmentSet(job.jobId(),job.leaseToken(),result())).isInstanceOf(ApiException.class).hasMessageContaining("전체 수집 저장 경로");
        verify(evidence,never()).insertSet(any());
    }
    @Test void retryCheckpointRequiresSelectedFixedRole() {
        when(evidence.insertFileCheckpoint(any(),any(),anyString(),anyString())).thenReturn(1);
        assertThat(service.saveFileCheckpoint(job.jobId(),job.leaseToken(),fresh)).isTrue();
        var unknown=new AttachmentSetEvidence.File(new AttachmentSetEvidence.Locator("FIXTURE","/download",Map.of("fileId","foreign")),fresh.displayName(),fresh.detectedType(),
                fresh.role(),fresh.roleOrigin(),fresh.downloadStatus(),fresh.downloadedBytes(),fresh.binaryHash(),null,fresh.extraction());
        assertThatThrownBy(()->service.saveFileCheckpoint(job.jobId(),job.leaseToken(),unknown)).isInstanceOf(ApiException.class).hasMessageContaining("범위 밖");
    }
    @Test void expiredFinalFenceCannotReportSuccessfulSeal() {
        when(evidence.updateJobSet(any(),any(),any())).thenReturn(0);
        assertThatThrownBy(()->service.saveRetriedAttachmentSet(job.jobId(),job.leaseToken(),result())).isInstanceOf(ApiException.class).hasMessageContaining("lease가 만료");
        verify(evidence,never()).deleteFileCheckpoints(any());
    }
}
