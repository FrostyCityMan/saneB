package com.saneb.domain.announcementsource.classification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.BodyAvailabilityCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.BodySourceCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.BodyStageCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.ReasonCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.SemanticStatusCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.TitleStageCode;
import com.saneb.domain.announcementsource.dao.AnnouncementSourceClassificationDao;
import com.saneb.domain.announcementsource.provider.AnnouncementSourceProviderItem;
import com.saneb.domain.announcementsource.service.AnnouncementSourceActiveRuleService;
import com.saneb.domain.announcementsource.service.AnnouncementSourceClassificationCoordinator;
import com.saneb.domain.announcementsource.service.AnnouncementSourceClassificationPersistenceService;
import com.saneb.domain.announcementsource.service.impl.AnnouncementSourceClassificationCoordinatorImpl;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AnnouncementSourceClassificationCoordinatorContentVersionTest {

    private static final UUID SOURCE_ID = UUID.fromString("98000000-0000-0000-0000-000000000001");

    private final AnnouncementSourceClassificationDao classificationDao =
            mock(AnnouncementSourceClassificationDao.class);
    private final AnnouncementSourceClassificationCoordinatorImpl coordinator =
            new AnnouncementSourceClassificationCoordinatorImpl(
                    true,
                    mock(AnnouncementSourceActiveRuleService.class),
                    classificationDao,
                    mock(AnnouncementSourceSearchPlanBuilder.class),
                    mock(AnnouncementSourceClassificationPersistenceService.class),
                    50
            );

    @Test
    void requiresAppendWhenOnlyDetailBodyChanges() {
        AnnouncementSourceProviderItem previousItem = item("기존 상세 본문");
        AnnouncementSourceProviderItem changedItem = item("변경된 상세 본문");
        AnnouncementSourceClassificationResult result = result();
        when(classificationDao.selectLatestContentVersionHash(SOURCE_ID))
                .thenReturn(AnnouncementSourceContentHasher.selectHash(previousItem, result));

        boolean appendRequired = coordinator.selectContentVersionAppendRequired(
                SOURCE_ID,
                prepared(changedItem, result)
        );

        assertThat(changedItem.rawHash()).isEqualTo(previousItem.rawHash());
        assertThat(appendRequired).isTrue();
    }

    @Test
    void keepsDuplicateWhenCanonicalContentHashIsUnchanged() {
        AnnouncementSourceProviderItem item = item("동일 상세 본문");
        AnnouncementSourceClassificationResult result = result();
        when(classificationDao.selectLatestContentVersionHash(SOURCE_ID))
                .thenReturn(AnnouncementSourceContentHasher.selectHash(item, result));

        boolean appendRequired = coordinator.selectContentVersionAppendRequired(
                SOURCE_ID,
                prepared(item, result)
        );

        assertThat(appendRequired).isFalse();
    }

    @Test
    void keepsDuplicateWhenSourceUrlsHaveSameCanonicalForm() {
        AnnouncementSourceProviderItem previousItem = item(
                "동일 상세 본문",
                "https://EXAMPLE.go.kr:443/notices/1/?b=2&a=1&utm_source=test"
        );
        AnnouncementSourceProviderItem currentItem = item(
                "동일 상세 본문",
                "https://example.go.kr/notices/1?a=1&b=2"
        );
        AnnouncementSourceClassificationResult result = result();
        when(classificationDao.selectLatestContentVersionHash(SOURCE_ID))
                .thenReturn(AnnouncementSourceContentHasher.selectHash(previousItem, result));

        boolean appendRequired = coordinator.selectContentVersionAppendRequired(
                SOURCE_ID,
                prepared(currentItem, result)
        );

        assertThat(appendRequired).isFalse();
    }

    private AnnouncementSourceClassificationCoordinator.PreparedClassification prepared(
            AnnouncementSourceProviderItem item,
            AnnouncementSourceClassificationResult result
    ) {
        return new AnnouncementSourceClassificationCoordinator.PreparedClassification(
                true, UUID.randomUUID(), item, result
        );
    }

    private AnnouncementSourceProviderItem item(String bodyText) {
        return item(bodyText, "https://example.go.kr/notices/1");
    }

    private AnnouncementSourceProviderItem item(String bodyText, String sourceUrl) {
        return new AnnouncementSourceProviderItem(
                "LOCAL_GOV_NOTICE",
                "LOCAL-1",
                "소상공인 지원",
                "테스트구청",
                null,
                null,
                null,
                null,
                sourceUrl,
                bodyText,
                null,
                null,
                "COMPLETE",
                "{}",
                "{}",
                "a".repeat(64),
                List.of(),
                UUID.randomUUID()
        );
    }

    private AnnouncementSourceClassificationResult result() {
        return new AnnouncementSourceClassificationResult(
                "LOCAL_GOV_NOTICE",
                "ACTIVE-1",
                SemanticStatusCode.ACCEPTED,
                ReasonCode.TARGET_SUPPORT_CONFIRMED,
                TitleStageCode.COMBINATION_MATCHED,
                BodyStageCode.COMBINATION_CONFIRMED,
                BodySourceCode.DETAIL_PAGE_TEXT,
                BodyAvailabilityCode.AVAILABLE,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
    }
}
