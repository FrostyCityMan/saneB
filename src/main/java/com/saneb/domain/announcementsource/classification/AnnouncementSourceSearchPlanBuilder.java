package com.saneb.domain.announcementsource.classification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.common.error.ApiException;
import com.saneb.common.error.ErrorCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.RuleGroupKindCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceSearchPlan.SearchQuery;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceSearchPlan.StrategyCode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * ACTIVE 규칙의 discovery term만 사용해 결정론적인 검색 계획을 만듭니다.
 */
@Component
public class AnnouncementSourceSearchPlanBuilder {

    private final ObjectMapper objectMapper;

    public AnnouncementSourceSearchPlanBuilder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public AnnouncementSourceSearchPlan selectPlan(
            UUID releaseId,
            String providerCode,
            List<AnnouncementSourceDiscoveryTerm> discoveryTerms,
            int maximumCombinationCount
    ) {
        StrategyCode strategyCode = "LOCAL_GOV_NOTICE".equals(providerCode)
                ? StrategyCode.COLLECT_ALL
                : StrategyCode.KEYWORD_COMBINATIONS;
        List<SearchQuery> queries = strategyCode == StrategyCode.COLLECT_ALL
                ? List.of()
                : selectCombinationList(discoveryTerms, maximumCombinationCount);
        String canonicalJson = selectCanonicalJson(releaseId, providerCode, strategyCode, queries);
        return new AnnouncementSourceSearchPlan(
                releaseId,
                providerCode,
                strategyCode,
                queries,
                canonicalJson,
                selectSha256(canonicalJson)
        );
    }

    private List<SearchQuery> selectCombinationList(
            List<AnnouncementSourceDiscoveryTerm> discoveryTerms,
            int maximumCombinationCount
    ) {
        if (maximumCombinationCount < 1) {
            throw new IllegalArgumentException("maximumCombinationCount must be positive");
        }
        Comparator<AnnouncementSourceDiscoveryTerm> order = Comparator
                .comparingInt(AnnouncementSourceDiscoveryTerm::discoveryOrder)
                .thenComparing(AnnouncementSourceDiscoveryTerm::groupCode)
                .thenComparing(AnnouncementSourceDiscoveryTerm::termText);
        List<AnnouncementSourceDiscoveryTerm> targets = discoveryTerms.stream()
                .filter(term -> term.groupKindCode() == RuleGroupKindCode.TARGET)
                .sorted(order)
                .toList();
        List<AnnouncementSourceDiscoveryTerm> supports = discoveryTerms.stream()
                .filter(term -> term.groupKindCode() == RuleGroupKindCode.SUPPORT_TYPE)
                .sorted(order)
                .toList();
        if (targets.isEmpty() || supports.isEmpty()) {
            throw new ApiException(
                    ErrorCode.ANNOUNCEMENT_SOURCE_RULE_INVALID,
                    HttpStatus.CONFLICT,
                    "외부 검색에 사용할 지원대상·지원형태 대표 검색어가 모두 필요합니다."
            );
        }
        long requestedCount = (long) targets.size() * supports.size();
        if (requestedCount > maximumCombinationCount) {
            throw new ApiException(
                    ErrorCode.ANNOUNCEMENT_SOURCE_RULE_INVALID,
                    HttpStatus.CONFLICT,
                    "대표 검색어 조합 수가 실행 상한을 초과했습니다. 규칙 버전의 discovery term을 줄여 주세요."
            );
        }
        List<SearchQuery> queries = new ArrayList<>((int) requestedCount);
        int queryOrder = 1;
        for (AnnouncementSourceDiscoveryTerm target : targets) {
            for (AnnouncementSourceDiscoveryTerm support : supports) {
                queries.add(new SearchQuery(
                        queryOrder++,
                        target.groupCode(),
                        target.termText(),
                        support.groupCode(),
                        support.termText(),
                        target.termText() + " " + support.termText()
                ));
            }
        }
        return List.copyOf(queries);
    }

    private String selectCanonicalJson(
            UUID releaseId,
            String providerCode,
            StrategyCode strategyCode,
            List<SearchQuery> queries
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("ruleReleaseId", releaseId.toString());
        payload.put("providerCode", providerCode);
        payload.put("strategyCode", strategyCode.name());
        payload.put("queries", queries);
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("공고 수집 검색 계획을 직렬화하지 못했습니다.", exception);
        }
    }

    private String selectSha256(String canonicalJson) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonicalJson.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 해시 알고리즘을 사용할 수 없습니다.", exception);
        }
    }
}
