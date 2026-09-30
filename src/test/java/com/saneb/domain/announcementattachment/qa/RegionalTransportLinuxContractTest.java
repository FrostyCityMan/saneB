package com.saneb.domain.announcementattachment.qa;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Windows 전송 실패의 Linux 비교 대상과 누적 요청 상한을 고정한다. 외부 요청은 하지 않는다. */
class RegionalTransportLinuxContractTest {
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
