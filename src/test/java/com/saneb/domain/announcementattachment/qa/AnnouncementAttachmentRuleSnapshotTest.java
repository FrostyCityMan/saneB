package com.saneb.domain.announcementattachment.qa;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationRuleSet;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/** 빌드 머신에서만 임시 DB의 DRAFT 규칙을 내보낸다. 서버에서는 DB에 연결하지 않는다. */
@EnabledIfEnvironmentVariable(named = "SANEB_ATTACHMENT_RULE_EXPORT", matches = "true")
class AnnouncementAttachmentRuleSnapshotTest {
    @Test void exportDraftSnapshotForServerQa() throws Exception {
        var rules = AnnouncementAttachmentRealFileQaTest.selectDraftRuleSet();
        var mapper = new ObjectMapper();
        Path output = Path.of(System.getProperty("saneb.attachment-qa.rule-snapshot"));
        Files.createDirectories(output.toAbsolutePath().getParent());
        mapper.writeValue(output.toFile(), rules);
        assertEquals(rules, mapper.readValue(output.toFile(), AnnouncementSourceClassificationRuleSet.class));
        assertEquals("ASCR-000001", rules.releaseCode());
        assertEquals(394, rules.rules().size());
    }
}
