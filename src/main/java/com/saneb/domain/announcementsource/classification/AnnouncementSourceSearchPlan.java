package com.saneb.domain.announcementsource.classification;

import java.util.List;
import java.util.UUID;

/**
 * 한 수집 실행 동안 고정되는 제공자 검색 계획입니다.
 */
public record AnnouncementSourceSearchPlan(
        UUID ruleReleaseId,
        String providerCode,
        StrategyCode strategyCode,
        List<SearchQuery> queries,
        String canonicalJson,
        String sha256
) {

    public AnnouncementSourceSearchPlan {
        queries = queries == null ? List.of() : List.copyOf(queries);
    }

    public enum StrategyCode {
        KEYWORD_COMBINATIONS,
        COLLECT_ALL
    }

    public record SearchQuery(
            int order,
            String targetGroupCode,
            String targetTerm,
            String supportGroupCode,
            String supportTerm,
            String keyword
    ) {
    }
}
