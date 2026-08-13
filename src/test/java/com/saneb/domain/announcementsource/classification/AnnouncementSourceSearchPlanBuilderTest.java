package com.saneb.domain.announcementsource.classification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.common.error.ApiException;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.RuleGroupKindCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceSearchPlan.StrategyCode;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AnnouncementSourceSearchPlanBuilderTest {

    private static final UUID RELEASE_ID =
            UUID.fromString("95000000-0000-0000-0000-000000000001");

    private final AnnouncementSourceSearchPlanBuilder builder =
            new AnnouncementSourceSearchPlanBuilder(new ObjectMapper());

    @Test
    void selectsDeterministicTargetSupportCombinations() {
        List<AnnouncementSourceDiscoveryTerm> terms = List.of(
                new AnnouncementSourceDiscoveryTerm("SUPPORT_GRANT", RuleGroupKindCode.SUPPORT_TYPE, "지원금", 1),
                new AnnouncementSourceDiscoveryTerm("TARGET_PERSONAL", RuleGroupKindCode.TARGET, "본인", 2),
                new AnnouncementSourceDiscoveryTerm("TARGET_BUSINESS", RuleGroupKindCode.TARGET, "소상공인", 1),
                new AnnouncementSourceDiscoveryTerm("SUPPORT_FINANCE", RuleGroupKindCode.SUPPORT_TYPE, "정책자금", 2)
        );

        AnnouncementSourceSearchPlan first = builder.selectPlan(
                RELEASE_ID, "BIZINFO", terms, 10
        );
        AnnouncementSourceSearchPlan second = builder.selectPlan(
                RELEASE_ID, "BIZINFO", terms.reversed(), 10
        );

        assertThat(first.strategyCode()).isEqualTo(StrategyCode.KEYWORD_COMBINATIONS);
        assertThat(first.queries()).extracting(AnnouncementSourceSearchPlan.SearchQuery::keyword)
                .containsExactly(
                        "소상공인 지원금",
                        "소상공인 정책자금",
                        "본인 지원금",
                        "본인 정책자금"
                );
        assertThat(second.canonicalJson()).isEqualTo(first.canonicalJson());
        assertThat(second.sha256()).isEqualTo(first.sha256()).hasSize(64);
    }

    @Test
    void localGovernmentUsesCollectAllWithoutKeywordCalls() {
        AnnouncementSourceSearchPlan plan = builder.selectPlan(
                RELEASE_ID, "LOCAL_GOV_NOTICE", List.of(), 50
        );

        assertThat(plan.strategyCode()).isEqualTo(StrategyCode.COLLECT_ALL);
        assertThat(plan.queries()).isEmpty();
        assertThat(plan.canonicalJson()).contains("COLLECT_ALL");
    }

    @Test
    void rejectsIncompleteOrExcessiveDiscoveryPlans() {
        List<AnnouncementSourceDiscoveryTerm> incomplete = List.of(
                new AnnouncementSourceDiscoveryTerm("TARGET_BUSINESS", RuleGroupKindCode.TARGET, "소상공인", 1)
        );
        assertThatThrownBy(() -> builder.selectPlan(RELEASE_ID, "GOV24_PUBLIC_SERVICE", incomplete, 50))
                .isInstanceOf(ApiException.class);

        List<AnnouncementSourceDiscoveryTerm> excessive = List.of(
                new AnnouncementSourceDiscoveryTerm("TARGET_BUSINESS", RuleGroupKindCode.TARGET, "소상공인", 1),
                new AnnouncementSourceDiscoveryTerm("TARGET_PERSONAL", RuleGroupKindCode.TARGET, "본인", 2),
                new AnnouncementSourceDiscoveryTerm("SUPPORT_GRANT", RuleGroupKindCode.SUPPORT_TYPE, "지원금", 1),
                new AnnouncementSourceDiscoveryTerm("SUPPORT_FINANCE", RuleGroupKindCode.SUPPORT_TYPE, "정책자금", 2)
        );
        assertThatThrownBy(() -> builder.selectPlan(RELEASE_ID, "BIZINFO", excessive, 3))
                .isInstanceOf(ApiException.class);
    }
}
