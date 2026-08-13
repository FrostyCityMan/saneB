package com.saneb.domain.announcementsource.classification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.saneb.domain.announcementsource.dao.AnnouncementSourceClassificationDao;
import com.saneb.domain.announcementsource.provider.AnnouncementSourceProviderItem;
import com.saneb.domain.announcementsource.service.AnnouncementSourceActiveRuleService;
import com.saneb.domain.announcementsource.service.AnnouncementSourceClassificationCoordinator;
import com.saneb.domain.announcementsource.service.AnnouncementSourceClassificationPersistenceService;
import com.saneb.domain.announcementsource.service.impl.AnnouncementSourceClassificationCoordinatorImpl;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AnnouncementSourceClassificationCoordinatorTitleGateTest {

    private final AnnouncementSourceClassificationCoordinatorImpl coordinator =
            new AnnouncementSourceClassificationCoordinatorImpl(
                    true,
                    mock(AnnouncementSourceActiveRuleService.class),
                    mock(AnnouncementSourceClassificationDao.class),
                    mock(AnnouncementSourceSearchPlanBuilder.class),
                    mock(AnnouncementSourceClassificationPersistenceService.class),
                    50
            );
    private final AnnouncementSourceClassificationCoordinator.RunContext runContext =
            new AnnouncementSourceClassificationCoordinator.RunContext(
                    true,
                    UUID.randomUUID(),
                    AnnouncementSourceClassificationGoldenRuleSet.selectRuleSet(),
                    null
            );

    @Test
    void requiresBodyOnlyForTitleCombinationOrGroupA() {
        assertThat(coordinator.selectBodyFetchRequired(
                runContext, item("소상공인 지원사업 안내")
        )).isTrue();
        assertThat(coordinator.selectBodyFetchRequired(
                runContext, item("스마트공장 참여기업 모집")
        )).isTrue();
        assertThat(coordinator.selectBodyFetchRequired(
                runContext, item("수출기업 해외진출 지원")
        )).isFalse();
        assertThat(coordinator.selectBodyFetchRequired(
                runContext, item("지역 축제 개최 안내")
        )).isFalse();
    }

    private AnnouncementSourceProviderItem item(String title) {
        OffsetDateTime now = OffsetDateTime.parse("2026-08-12T10:00:00+09:00");
        return new AnnouncementSourceProviderItem(
                "LOCAL_GOV_NOTICE",
                UUID.randomUUID().toString(),
                title,
                "테스트구청",
                LocalDate.of(2026, 8, 12),
                LocalDate.of(2026, 8, 31),
                now,
                now,
                "https://example.go.kr/notice/1",
                null,
                null,
                null,
                "PARTIAL",
                "{}",
                "{}",
                "test-hash",
                List.of(),
                UUID.randomUUID()
        );
    }
}
