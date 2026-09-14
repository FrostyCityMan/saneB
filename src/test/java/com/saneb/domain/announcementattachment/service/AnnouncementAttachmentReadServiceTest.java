package com.saneb.domain.announcementattachment.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.saneb.common.error.ApiException;
import com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentEvidenceDao;
import com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentJobDao;
import com.saneb.domain.announcementattachment.service.impl.AnnouncementAttachmentReadServiceImpl;
import com.saneb.domain.announcementattachment.vo.AttachmentSourceContextRow;
import com.saneb.domain.announcementattachment.vo.AttachmentBlockRow;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AnnouncementAttachmentReadServiceTest {
    private final AnnouncementAttachmentJobDao jobs=mock(AnnouncementAttachmentJobDao.class);
    private final AnnouncementAttachmentEvidenceDao evidence=mock(AnnouncementAttachmentEvidenceDao.class);
    private final AnnouncementAttachmentReadService service=new AnnouncementAttachmentReadServiceImpl(jobs,evidence);
    private final UUID source=UUID.randomUUID(),set=UUID.randomUUID(),extraction=UUID.randomUUID();
    private void source(String state) {
        when(jobs.selectSourceContextDetails(source)).thenReturn(new AttachmentSourceContextRow(source,"BIZINFO",
                "QA".equals(state)?"QA":"PRODUCTION","EXCLUDED".equals(state)?"EXCLUDED":"REVIEW_REQUIRED",
                "NO_BASE".equals(state)?null:UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),
                "NO_TITLE".equals(state)?null:"TITLE_FAILED".equals(state)?"GROUP_B_MATCHED":"COMBINATION_MATCHED",0,0,false,null,null));
    }
    @ParameterizedTest @ValueSource(strings={"QA","EXCLUDED","NO_BASE","NO_TITLE","TITLE_FAILED"})
    void ineligibleSourceCannotReadSetsFilesOrTextEvenIfItStillExists(String state) {
        source(state);
        assertThatThrownBy(()->service.selectAttachmentSetList(source,1,20)).isInstanceOf(ApiException.class);
        assertThatThrownBy(()->service.selectAttachmentFileList(source,set,1,20)).isInstanceOf(ApiException.class);
        assertThatThrownBy(()->service.selectAttachmentBlockList(source,extraction,1,10,0,2000)).isInstanceOf(ApiException.class);
        verifyNoInteractions(evidence);
    }
    @Test void missingOrNullSourceNeverReadsEvidence() {
        assertThatThrownBy(()->service.selectAttachmentSetList(source,1,20)).isInstanceOf(ApiException.class);
        assertThatThrownBy(()->service.selectAttachmentSetList(null,1,20)).isInstanceOf(ApiException.class);
        verifyNoInteractions(evidence);
    }
    @Test void otherSourceSetOrExtractionIs404RatherThanEmptyData() {
        source("ALLOWED");
        // 단건 SQL에 행이 없으면 null이다. Mockito의 Long 기본값 0과 구분한다.
        when(evidence.selectExtractionBlockCount(source,extraction)).thenReturn(null);
        assertThatThrownBy(()->service.selectAttachmentFileList(source,set,1,20)).isInstanceOf(ApiException.class);
        assertThatThrownBy(()->service.selectAttachmentBlockList(source,extraction,1,10,0,2000)).isInstanceOf(ApiException.class);
        verify(evidence,never()).selectFileList(any(),any(),anyInt(),anyInt());
        verify(evidence,never()).selectBlockList(any(),any(),anyInt(),anyInt(),anyInt(),anyInt());
    }
    @Test void visibleSourceCanReadPagedSetsAndBoundedTextAtTheOriginalExtraction() {
        source("ALLOWED");when(evidence.selectSetList(source,20,20)).thenReturn(List.of());
        when(evidence.selectSetCount(source)).thenReturn(20L);
        assertThat(service.selectAttachmentSetList(source,2,20).totalCount()).isEqualTo(20);
        when(evidence.selectExtractionBlockCount(source,extraction)).thenReturn(1L);
        when(evidence.selectBlockList(source,extraction,0,10,2,4)).thenReturn(List.of(new AttachmentBlockRow(0,0,20,"paragraph:0",true,"paragraph:0","지원금",2,6,true)));
        var blocks=service.selectAttachmentBlockList(source,extraction,1,10,2,4);
        assertThat(blocks.items().getFirst().text()).isEqualTo("지원금");assertThat(blocks.items().getFirst().hasMoreText()).isTrue();
        verify(evidence).selectBlockList(source,extraction,0,10,2,4);
    }
    @ParameterizedTest @ValueSource(ints={0,4001,Integer.MAX_VALUE})
    void textLimitCannotBypassBoundedRead(int limit) {
        assertThatThrownBy(()->service.selectAttachmentBlockList(source,extraction,1,10,0,limit)).isInstanceOf(ApiException.class).hasMessageContaining("1~4000");
        verifyNoInteractions(jobs,evidence);
    }
}
