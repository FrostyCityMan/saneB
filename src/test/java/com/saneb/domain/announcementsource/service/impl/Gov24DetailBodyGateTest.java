package com.saneb.domain.announcementsource.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.saneb.domain.announcementsource.provider.AnnouncementSourceProviderClient;
import com.saneb.domain.announcementsource.provider.AnnouncementSourceProviderItem;
import com.saneb.domain.announcementsource.service.AnnouncementSourceClassificationCoordinator;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.BodyAvailabilityCode;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.util.ReflectionTestUtils;

/** 본문 준비 단계만 검사한다. 실제 수집·DB·운영 성공을 주장하지 않는다. */
class Gov24DetailBodyGateTest {
    @ParameterizedTest
    @CsvSource({"false,true,true,false", "true,false,true,false", "true,true,false,false", "true,true,true,true"})
    void summaryIsEnrichedOnlyWithEnabledRunAndAllowedTitle(boolean feature, boolean runEnabled,
            boolean titleAllowed, boolean expectedCall) {
        var provider = mock(AnnouncementSourceProviderClient.class);
        var coordinator = mock(AnnouncementSourceClassificationCoordinator.class);
        when(provider.selectProviderCode()).thenReturn("GOV24_PUBLIC_SERVICE");
        when(provider.isDetailBodyEnabled()).thenReturn(feature);
        var service = new AnnouncementSourceServiceImpl(null, null, null, null, List.of(provider), List.of(), coordinator, null);
        var run = new AnnouncementSourceClassificationCoordinator.RunContext(runEnabled, UUID.randomUUID(), null, null);
        var item = selectItem();
        when(coordinator.selectBodyFetchRequired(run, item)).thenReturn(titleAllowed);
        when(provider.selectDetailBody("TEST-001")).thenReturn(
                new AnnouncementSourceProviderClient.ProviderDetailBody("공식 상세", BodyAvailabilityCode.AVAILABLE));
        Object prepared = ReflectionTestUtils.invokeMethod(service, "selectProviderContent", run, item);
        AnnouncementSourceProviderItem enriched = ReflectionTestUtils.invokeMethod(prepared, "item");
        assertThat(enriched.bodyText()).isEqualTo(expectedCall ? "공식 상세" : "기존 요약");
        assertThat(enriched.rawHash()).isEqualTo(item.rawHash());
        assertThat(enriched.attachments()).isEmpty();
        verify(provider, times(expectedCall ? 1 : 0)).selectDetailBody("TEST-001");
    }

    @ParameterizedTest
    @CsvSource({"FETCH_FAILED", "UNSUPPORTED"})
    void failedDetailPreservesSummaryButDoesNotClaimFullTextAvailable(BodyAvailabilityCode failure) {
        var provider = mock(AnnouncementSourceProviderClient.class);
        var coordinator = mock(AnnouncementSourceClassificationCoordinator.class);
        when(provider.selectProviderCode()).thenReturn("GOV24_PUBLIC_SERVICE");
        when(provider.isDetailBodyEnabled()).thenReturn(true);
        var service = new AnnouncementSourceServiceImpl(null, null, null, null, List.of(provider), List.of(), coordinator, null);
        var run = new AnnouncementSourceClassificationCoordinator.RunContext(true, UUID.randomUUID(), null, null);
        var item = selectItem();
        when(coordinator.selectBodyFetchRequired(run, item)).thenReturn(true);
        when(provider.selectDetailBody("TEST-001")).thenReturn(new AnnouncementSourceProviderClient.ProviderDetailBody(null, failure));
        Object prepared = ReflectionTestUtils.invokeMethod(service, "selectProviderContent", run, item);
        AnnouncementSourceProviderItem enriched = ReflectionTestUtils.invokeMethod(prepared, "item");
        BodyAvailabilityCode availability = ReflectionTestUtils.invokeMethod(prepared, "bodyAvailabilityCode");
        assertThat(enriched).isSameAs(item);
        assertThat(availability).isEqualTo(failure);
        verify(provider, times(1)).selectDetailBody("TEST-001");
    }

    private AnnouncementSourceProviderItem selectItem() {
        return new AnnouncementSourceProviderItem("GOV24_PUBLIC_SERVICE", "TEST-001", "소상공인 지원", "기관",
                null, null, null, null, "https://example.invalid/detail", "기존 요약", null, null,
                "PARTIAL", "[]", "{}", "original-hash", List.of(), null);
    }
}
