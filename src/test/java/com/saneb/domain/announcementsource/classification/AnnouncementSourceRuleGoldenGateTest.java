package com.saneb.domain.announcementsource.classification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.saneb.common.error.ApiException;
import com.saneb.common.error.ErrorCode;
import com.saneb.domain.announcementsource.service.impl.AnnouncementSourceRuleGoldenGate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class AnnouncementSourceRuleGoldenGateTest {

    private final AnnouncementSourceRuleGoldenGate goldenGate = new AnnouncementSourceRuleGoldenGate();

    @Test
    void selectValidatedResultExecutesAllQa01ThroughQa20OnServer() {
        AnnouncementSourceRuleGoldenGate.GoldenGateResult result = goldenGate.selectValidatedResult(
                AnnouncementSourceClassificationGoldenRuleSet.selectRuleSet(),
                "a".repeat(64)
        );

        assertThat(result.runId()).startsWith("GOLDEN-");
        assertThat(result.caseCount()).isEqualTo(20);
        assertThat(result.signatures().keySet()).containsExactlyInAnyOrderElementsOf(
                java.util.stream.IntStream.rangeClosed(1, 20)
                        .mapToObj(number -> "QA-%02d".formatted(number))
                        .toList()
        );
    }

    @Test
    void selectValidatedResultRejectsReleaseWhenRequiredAdministrativePhrasesAreMissing() {
        AnnouncementSourceClassificationRuleSet baseline =
                AnnouncementSourceClassificationGoldenRuleSet.selectRuleSet();
        List<AnnouncementSourceClassificationRule> withoutAdministrativeRules = baseline.rules().stream()
                .filter(rule -> !"AUTO_EXCLUDE_B_ADMINISTRATIVE".equals(rule.groupCode()))
                .toList();
        AnnouncementSourceClassificationRuleSet invalid = new AnnouncementSourceClassificationRuleSet(
                "ASCR-INVALID",
                withoutAdministrativeRules
        );

        assertThatThrownBy(() -> goldenGate.selectValidatedResult(invalid, "b".repeat(64)))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.errorCode()).isEqualTo(ErrorCode.ANNOUNCEMENT_SOURCE_RULE_INVALID);
                    assertThat(exception.httpStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(exception.getMessage()).contains("QA-12");
                });
    }

}
