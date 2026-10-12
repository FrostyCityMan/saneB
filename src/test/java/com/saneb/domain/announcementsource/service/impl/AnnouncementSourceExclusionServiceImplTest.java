package com.saneb.domain.announcementsource.service.impl;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.saneb.domain.announcementsource.dao.AnnouncementSourceExclusionDao;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceExclusionCountRow;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class AnnouncementSourceExclusionServiceImplTest {
    private final AnnouncementSourceExclusionDao dao=mock(AnnouncementSourceExclusionDao.class);
    private final AnnouncementSourceExclusionServiceImpl service=new AnnouncementSourceExclusionServiceImpl(dao);
    private final OffsetDateTime at=OffsetDateTime.parse("2026-10-12T00:00:00Z");
    private AnnouncementSourceExclusionCountRow row(String dimension,String code,long count) {
        return new AnnouncementSourceExclusionCountRow(dimension,code,count,at);
    }
    @Test void emptyDatabaseStillReturnsExplicitZeroAndTime() {
        when(dao.selectExclusionCountList()).thenReturn(List.of(row("AS_OF","AS_OF",0)));
        var result=service.selectSummaryDetails();
        assertThat(result.uniqueExcludedCount()).isZero(); assertThat(result.asOf()).isEqualTo(at);
        assertThat(result.byProvider()).hasSize(3); assertThat(result.byLatestReason()).hasSize(3);
    }
    @Test void qaIsNotIncludedAndUnknownIsSeparate() {
        when(dao.selectExclusionCountList()).thenReturn(List.of(row("AS_OF","AS_OF",0),row("PURPOSE","PRODUCTION",2),
                row("PURPOSE","QA",70),row("PURPOSE","UNKNOWN",3),row("PROVIDER","BIZINFO",2),row("REASON","OTHER_LEGACY",2)));
        var result=service.selectSummaryDetails();
        assertThat(result.uniqueExcludedCount()).isEqualTo(2); assertThat(result.unknownPurposeCount()).isEqualTo(3);
        assertThat(result.byProvider().stream().mapToLong(i->i.count()).sum()).isEqualTo(2);
        assertThat(result.byLatestReason().stream().mapToLong(i->i.count()).sum()).isEqualTo(2);
    }
    @Test void missingResultAndSqlErrorAreNeverZero() {
        when(dao.selectExclusionCountList()).thenReturn(List.of());
        assertThatThrownBy(service::selectSummaryDetails).isInstanceOf(java.util.NoSuchElementException.class);
        when(dao.selectExclusionCountList()).thenThrow(new IllegalStateException("fixture"));
        assertThatThrownBy(service::selectSummaryDetails).isInstanceOf(IllegalStateException.class);
    }
}
