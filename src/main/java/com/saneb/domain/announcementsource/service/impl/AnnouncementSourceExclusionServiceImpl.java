package com.saneb.domain.announcementsource.service.impl;

import com.saneb.domain.announcementsource.dao.AnnouncementSourceExclusionDao;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceExclusionSummaryResponse;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceExclusionSummaryResponse.CountItem;
import com.saneb.domain.announcementsource.service.AnnouncementSourceExclusionService;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceExclusionCountRow;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnnouncementSourceExclusionServiceImpl implements AnnouncementSourceExclusionService {
    private final AnnouncementSourceExclusionDao dao;
    public AnnouncementSourceExclusionServiceImpl(AnnouncementSourceExclusionDao dao) { this.dao = dao; }

    @Override
    @Transactional(readOnly = true)
    public AnnouncementSourceExclusionSummaryResponse selectSummaryDetails() {
        var rows = dao.selectExclusionCountList();
        // 빈 테이블도 기준 시각 행을 반환한다. 조회/매핑 실패를 0건으로 대신하지 않는다.
        var anchor = rows.stream().filter(row -> "AS_OF".equals(row.dimension())).findFirst().orElseThrow();
        var providers = List.of("BIZINFO", "GOV24_PUBLIC_SERVICE", "LOCAL_GOV_NOTICE").stream()
                .map(code -> new CountItem(code, selectCount(rows, "PROVIDER", code))).toList();
        var reasons = List.of("TITLE_GROUP_B_MATCHED", "TITLE_COMBINATION_NOT_MATCHED", "OTHER_LEGACY").stream()
                .map(code -> new CountItem(code, selectCount(rows, "REASON", code))).toList();
        return new AnnouncementSourceExclusionSummaryResponse(selectCount(rows, "PURPOSE", "PRODUCTION"),
                selectCount(rows, "PURPOSE", "UNKNOWN"), providers, reasons,
                "PROVIDER_IDENTITY_UNIQUE", "RETAINED_PRODUCTION_BY_LATEST_RUN", anchor.asOf());
    }

    private long selectCount(List<AnnouncementSourceExclusionCountRow> rows, String dimension, String code) {
        return rows.stream().filter(row -> dimension.equals(row.dimension()) && code.equals(row.code()))
                .mapToLong(AnnouncementSourceExclusionCountRow::count).sum();
    }
}
