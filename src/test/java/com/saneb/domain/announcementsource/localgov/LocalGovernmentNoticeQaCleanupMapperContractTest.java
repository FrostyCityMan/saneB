package com.saneb.domain.announcementsource.localgov;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class LocalGovernmentNoticeQaCleanupMapperContractTest {

    private static final String MAPPER_RESOURCE =
            "/mapper/announcementsource/LocalGovernmentNoticeMapper.xml";

    @Test
    void cleanupStatementsFilterExplicitQaRowsAndProtectProductionState() throws IOException {
        String xml;
        try (var input = getClass().getResourceAsStream(MAPPER_RESOURCE)) {
            assertThat(input).as("지자체 Mapper resource").isNotNull();
            xml = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }

        assertThat(statement(xml, "select", "selectLinkedQaSnapshotCount"))
                .contains("ass.data_purpose_code = 'QA'");
        assertThat(statement(xml, "delete", "deleteQaScheduleExecutionList"))
                .contains("ascr.data_purpose_code = 'QA'")
                .doesNotContain("announcement_source_collection_schedules");
        assertThat(statement(xml, "delete", "deleteQaCollectionRunList"))
                .contains("ascr.data_purpose_code = 'QA'");
        assertThat(statement(xml, "delete", "deleteQaCollectionRequestList"))
                .contains("data_purpose_code = 'QA'");
        assertThat(statement(xml, "delete", "deleteQaSnapshotList"))
                .contains("data_purpose_code = 'QA'");
        assertThat(statement(xml, "update", "resetQaSourceCollectionState"))
                .contains("data_purpose_code = 'QA'", "data_purpose_code = 'PRODUCTION'", "NOT EXISTS");
    }

    private String statement(String xml, String elementName, String id) {
        Pattern pattern = Pattern.compile(
                "(?s)<" + elementName + "\\s+id=\\\"" + Pattern.quote(id)
                        + "\\\"[^>]*>(.*?)</" + elementName + ">"
        );
        Matcher matcher = pattern.matcher(xml);
        assertThat(matcher.find()).as(id + " statement").isTrue();
        return matcher.group(1);
    }
}
