package com.saneb.domain.announcementattachment.service.impl;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentJobDao;
import com.saneb.domain.announcementattachment.vo.*;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentBatchService;
import com.saneb.domain.announcementattachment.worker.AnnouncementAttachmentBatchProgressScheduler;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AnnouncementAttachmentBatchClaimTest {
    private final AnnouncementAttachmentJobDao dao=mock(AnnouncementAttachmentJobDao.class);
    private final AnnouncementAttachmentJobServiceImpl service=new AnnouncementAttachmentJobServiceImpl(dao,new ObjectMapper());
    private final UUID source=UUID.randomUUID(),base=UUID.randomUUID(),content=UUID.randomUUID(),rule=UUID.randomUUID(),policy=UUID.randomUUID();
    private AttachmentJobRow job(UUID batch) {
        var row=new AttachmentJobRow(UUID.randomUUID(),source,content,base,rule,policy,batch,null,1,0,0,"RUNNING",1,null,UUID.randomUUID(),OffsetDateTime.now().plusMinutes(2),null,
                UUID.randomUUID(),"a".repeat(64),"{}",83886080L,0L,1);
        when(dao.updateNextJobLease(any(),anyInt())).thenReturn(row);
        when(dao.selectSourceContextDetails(source)).thenReturn(new AttachmentSourceContextRow(source,"BIZINFO","PRODUCTION","REVIEW_REQUIRED",base,content,rule,"COMBINATION_MATCHED",0,0,false,null,null));
        return row;
    }
    @Test void changedBatchInputBecomesDistinctTerminalConflictInsteadOfAnotherClaim() {
        var job=job(UUID.randomUUID());when(dao.selectBatchExecutionUnchanged(job.jobId())).thenReturn(false);
        assertThat(service.saveNextJobClaim()).isEmpty();verify(dao).updateJobFrozenInputConflict(job.jobId(),job.leaseToken());
        verify(dao,never()).updateJobConflict(any(),any());verify(dao,never()).updateJobDeferred(any(),any(),anyBoolean());
    }
    @Test void unchangedBatchInputPreservesLeaseAndReachesWorker() {
        var job=job(UUID.randomUUID());when(dao.selectBatchExecutionUnchanged(job.jobId())).thenReturn(true);
        assertThat(service.saveNextJobClaim()).contains(job);verify(dao,never()).updateJobFrozenInputConflict(any(),any());
    }
    @Test void normalSourceCollectionDoesNotRequireNewBatchContract() {
        var job=job(null);assertThat(service.saveNextJobClaim()).contains(job);verify(dao,never()).selectBatchExecutionUnchanged(any());
    }
    @Test void sourceVersionConflictKeepsExistingReason() {
        var job=job(UUID.randomUUID());when(dao.selectSourceContextDetails(source)).thenReturn(null);
        assertThat(service.saveNextJobClaim()).isEmpty();verify(dao).updateJobConflict(job.jobId(),job.leaseToken());
        verify(dao,never()).updateJobFrozenInputConflict(any(),any());
    }
    @Test void schedulerOnlyAggregatesAndContainsFailureWithoutStartingWork() {
        var batches=mock(AnnouncementAttachmentBatchService.class);var scheduler=new AnnouncementAttachmentBatchProgressScheduler(batches);
        when(batches.saveCollectionProgress()).thenThrow(new IllegalStateException("test-only failure"));
        assertThatCode(scheduler::saveCollectionProgress).doesNotThrowAnyException();verify(batches).saveCollectionProgress();verifyNoMoreInteractions(batches);
    }
}
