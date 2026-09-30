package com.saneb.domain.announcementattachment.qa;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Windows 전송 실패의 Linux 비교 대상과 누적 요청 상한을 고정한다. 외부 요청은 하지 않는다. */
class RegionalTransportLinuxContractTest {
    @Test
    void remainingTwelveRegionsUseExistingCasesAndBoundedSequentialBatches() {
        var first = AnnouncementAttachmentBbsOfficialObservationTest.selectBatchCases(
                "EUNPYEONG_SUPPORT,SEODAEMUN,GEOMDAN,ICHEON,DONGDUCHEON_YOUTH,SOKCHO").toList();
        var second = AnnouncementAttachmentBbsOfficialObservationTest.selectBatchCases(
                "YEONGDONG,ASAN_SUPPORT,UISEONG,SEONGJU,BONGHWA,NAMHAE").toList();
        assertEquals(6, first.size()); assertEquals(6, second.size());
        var all = java.util.stream.Stream.concat(first.stream(), second.stream()).toList();
        assertEquals(List.of("EUNPYEONG-50607", "SEODAEMUN-313956", "GEOMDAN-235", "ICHEON-70639",
                "DONGDUCHEON-45339", "SOKCHO-32983", "YEONGDONG-759FDCD3", "ASAN-76469",
                "UISEONG-39093", "SEONGJU-586507", "BONGHWA-32956", "NAMHAE-35694"),
                all.stream().map(AnnouncementAttachmentBbsOfficialObservationTest.ObservationCase::code).toList());
        assertEquals(12, all.stream().map(sample -> sample.source().localSourceCode()).distinct().count());
        long requests = 0, bytes = 0;
        for (var sample : all) {
            var budget = AnnouncementAttachmentBbsOfficialObservationTest.selectBudget(sample.profile(), true, false);
            requests += budget.maximumRequests; bytes += budget.maximumBytes;
            org.junit.jupiter.api.Assertions.assertTrue(budget.maximumRequests <= 7, sample.code());
            assertEquals(23L * 1024 * 1024, budget.maximumBytes, sample.code());
        }
        assertEquals(74, requests);
        assertEquals(276L * 1024 * 1024, bytes);
    }
    @Test
    void fixedSixCasesKeepCollectionOnlyBudget() {
        var cases = AnnouncementAttachmentBbsOfficialObservationTest.selectBatchCases(
                "POCHEON,GANGNEUNG,CHUNGBUK,GONGJU,PYEONGTAEK,SEONGNAM").toList();
        assertEquals(List.of("POCHEON-64129", "GANGNEUNG-60798", "CHUNGBUK-67302",
                "GONGJU-59971", "PYEONGTAEK-95902", "SEONGNAM-144735"),
                cases.stream().map(AnnouncementAttachmentBbsOfficialObservationTest.ObservationCase::code).toList());
        for (var sample : cases) {
            var budget = AnnouncementAttachmentBbsOfficialObservationTest.selectBudget(sample.profile(), true, false);
            assertEquals(6, budget.maximumRequests, sample.code());
            assertEquals(23L * 1024 * 1024, budget.maximumBytes, sample.code());
        }
    }
}
