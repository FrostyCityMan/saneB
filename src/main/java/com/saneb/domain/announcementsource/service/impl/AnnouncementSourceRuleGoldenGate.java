package com.saneb.domain.announcementsource.service.impl;

import com.saneb.common.error.ApiException;
import com.saneb.common.error.ErrorCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.BodyAvailabilityCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.BodySourceCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationEngine;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationInput;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationResult;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationRuleSet;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** 클라이언트 성공 플래그를 신뢰하지 않고 서버에서 QA-01~20을 다시 실행합니다. */
@Component
public class AnnouncementSourceRuleGoldenGate {

    private static final int GOLDEN_CASE_COUNT = 20;

    private final AnnouncementSourceClassificationEngine engine;

    public AnnouncementSourceRuleGoldenGate() {
        this(new AnnouncementSourceClassificationEngine());
    }

    AnnouncementSourceRuleGoldenGate(AnnouncementSourceClassificationEngine engine) {
        this.engine = engine;
    }

    /** 후보 release의 필수 정책 표본을 검증하고 서버 실행 식별자를 만듭니다. */
    public GoldenGateResult selectValidatedResult(
            AnnouncementSourceClassificationRuleSet ruleSet,
            String snapshotHash
    ) {
        Map<String, String> signatures = new LinkedHashMap<>();
        List<GoldenCase> goldenCases = selectGoldenCases();

        Map<String, AnnouncementSourceClassificationResult> results = new LinkedHashMap<>();
        for (GoldenCase goldenCase : goldenCases) {
            AnnouncementSourceClassificationResult result = decide(ruleSet, goldenCase.toInput());
            validateGoldenCase(goldenCase, result);
            results.put(goldenCase.caseId(), result);
            signatures.put(goldenCase.caseId(), signature(result));
        }

        AnnouncementSourceClassificationResult qa14 = results.get("QA-14");
        Set<String> qa14MatchedRuleTerms = qa14.matches().stream()
                .map(match -> match.matchedRuleTerm().toLowerCase())
                .collect(java.util.stream.Collectors.toSet());
        require("QA-14", qa14MatchedRuleTerms.contains("tips") && qa14MatchedRuleTerms.contains("팁스"),
                "TIPS와 팁스가 모두 일치해야 합니다.");

        GoldenCase qa20 = goldenCases.getLast();
        String qa20Signature = signatures.get("QA-20");
        for (String providerCode : List.of("BIZINFO", "GOV24_PUBLIC_SERVICE", "LOCAL_GOV_NOTICE")) {
            AnnouncementSourceClassificationResult providerResult = decide(
                    ruleSet,
                    qa20.toInput(providerCode)
            );
            require("QA-20", qa20Signature.equals(signature(providerResult)),
                    "provider가 달라도 동일 원문은 같은 판정이어야 합니다.");
        }

        require("GOLDEN", signatures.size() == GOLDEN_CASE_COUNT, "QA-01~20이 모두 실행되어야 합니다.");
        String runId = "GOLDEN-" + sha256(snapshotHash + "\n" + signatures).substring(0, 24);
        return new GoldenGateResult(runId, Map.copyOf(signatures));
    }

    private void validateGoldenCase(
            GoldenCase goldenCase,
            AnnouncementSourceClassificationResult result
    ) {
        requireStatus(goldenCase.caseId(), result, goldenCase.expectedStatusCode());
        requireReason(goldenCase.caseId(), result, goldenCase.expectedReasonCode());
        requireExact(goldenCase.caseId(), "지원대상", names(result.targetCategoryCodes()),
                goldenCase.expectedTargetCategoryCodes());
        requireExact(goldenCase.caseId(), "지원유형", names(result.supportTypeCodes()),
                goldenCase.expectedSupportTypeCodes());
        requireExact(goldenCase.caseId(), "그룹 A", result.groupACodes(), goldenCase.expectedGroupACodes());
        requireExact(goldenCase.caseId(), "그룹 B", result.groupBCodes(), goldenCase.expectedGroupBCodes());
        Set<String> locations = result.matches().stream()
                .map(match -> match.locationCode().name())
                .collect(java.util.stream.Collectors.toSet());
        require(goldenCase.caseId(), locations.equals(goldenCase.expectedMatchLocations()),
                "일치 위치가 " + goldenCase.expectedMatchLocations() + "이어야 합니다. actual=" + locations);
    }

    private List<GoldenCase> selectGoldenCases() {
        return List.of(
                availableCase("QA-01", "BIZINFO", "소상공인 정책자금 지원사업",
                        "소상공인을 대상으로 정책자금 융자를 지원합니다.", "중소벤처기업부", List.of(),
                        BodySourceCode.PROVIDER_SUMMARY, "ACCEPTED", "TARGET_SUPPORT_CONFIRMED",
                        List.of("BUSINESS"), List.of("GENERAL_SUPPORT", "POLICY_FINANCE"),
                        List.of(), List.of(), Set.of("TITLE", "BODY")),
                availableCase("QA-02", "GOV24_PUBLIC_SERVICE", "주민 지원",
                        "지역주민을 위한 일반 안내입니다.", "테스트시청", List.of(),
                        BodySourceCode.PROVIDER_SUMMARY, "EXCLUDED", "TITLE_COMBINATION_NOT_MATCHED",
                        List.of("PERSONAL"), List.of("GENERAL_SUPPORT"),
                        List.of(), List.of(), Set.of("TITLE")),
                availableCase("QA-03", "LOCAL_GOV_NOTICE", "스마트공장 지원사업",
                        "소상공인을 대상으로 경영 지원사업을 운영합니다.", "테스트구청", List.of(),
                        BodySourceCode.DETAIL_PAGE_TEXT, "REVIEW_REQUIRED", "TITLE_GROUP_A_MATCHED",
                        List.of("BUSINESS"), List.of("GENERAL_SUPPORT"),
                        List.of("REVIEW_A_MANUFACTURING_TECH_PRODUCT"), List.of(), Set.of("TITLE", "BODY")),
                availableCase("QA-04", "BIZINFO", "기술창업 지원사업",
                        "기술창업 기업을 위한 지원사업입니다.", "창업진흥원", List.of(),
                        BodySourceCode.PROVIDER_SUMMARY, "EXCLUDED", "TITLE_GROUP_B_MATCHED",
                        List.of(), List.of("GENERAL_SUPPORT"),
                        List.of("REVIEW_A_MANUFACTURING_TECH_PRODUCT"),
                        List.of("AUTO_EXCLUDE_B_INVESTMENT_STARTUP"), Set.of("TITLE")),
                availableCase("QA-05", "BIZINFO", "소상공인 수출바우처",
                        "소상공인의 해외진출을 돕는 바우처입니다.", "중소벤처기업부", List.of(),
                        BodySourceCode.PROVIDER_SUMMARY, "EXCLUDED", "TITLE_GROUP_B_MATCHED",
                        List.of("BUSINESS"), List.of("VOUCHER_BENEFIT"),
                        List.of(), List.of("AUTO_EXCLUDE_B_EXPORT"), Set.of("TITLE")),
                availableCase("QA-06", "GOV24_PUBLIC_SERVICE", "소상공인 정책자금 안내",
                        "소상공인의 수출 판로를 지원합니다.", "테스트기관", List.of(),
                        BodySourceCode.PROVIDER_SUMMARY, "REVIEW_REQUIRED", "BODY_GROUP_B_MATCHED",
                        List.of("BUSINESS"), List.of("GENERAL_SUPPORT", "POLICY_FINANCE"),
                        List.of(), List.of("AUTO_EXCLUDE_B_EXPORT"), Set.of("TITLE", "BODY")),
                availableCase("QA-07", "BIZINFO", "중진공 소상공인 경영안정자금",
                        "중소벤처기업진흥공단은 소상공인에게 경영안정자금을 지원합니다.",
                        "중소벤처기업진흥공단", List.of("중진공"), BodySourceCode.PROVIDER_SUMMARY,
                        "ACCEPTED", "TARGET_SUPPORT_CONFIRMED", List.of("BUSINESS"),
                        List.of("GENERAL_SUPPORT", "POLICY_FINANCE"), List.of(), List.of(),
                        Set.of("TITLE", "BODY")),
                availableCase("QA-08", "LOCAL_GOV_NOTICE", "소상공인 채용지원금",
                        "소상공인에게 채용지원금을 지급하는 사업입니다.", "테스트시청", List.of(),
                        BodySourceCode.DETAIL_PAGE_TEXT, "ACCEPTED", "TARGET_SUPPORT_CONFIRMED",
                        List.of("BUSINESS"), List.of("GENERAL_SUPPORT", "GRANT_SUBSIDY"),
                        List.of(), List.of(), Set.of("TITLE", "BODY")),
                availableCase("QA-09", "LOCAL_GOV_NOTICE", "소상공인 대상 입찰 참여 지원",
                        "소상공인의 입찰 참여 비용을 지원하는 사업입니다.", "테스트구청", List.of(),
                        BodySourceCode.DETAIL_PAGE_TEXT, "ACCEPTED", "TARGET_SUPPORT_CONFIRMED",
                        List.of("BUSINESS"), List.of("GENERAL_SUPPORT"),
                        List.of(), List.of(), Set.of("TITLE", "BODY")),
                availableCase("QA-10", "LOCAL_GOV_NOTICE", "2026년 지원사업 고시",
                        "지원사업 시행 내용을 고시합니다.", "테스트시청", List.of(),
                        BodySourceCode.DETAIL_PAGE_TEXT, "EXCLUDED", "TITLE_COMBINATION_NOT_MATCHED",
                        List.of(), List.of("GENERAL_SUPPORT"), List.of(), List.of(), Set.of("TITLE")),
                availableCase("QA-11", "BIZINFO", "소상공인 병의원 경영지원금",
                        "소상공인 병의원의 경영지원금을 지원합니다.", "테스트기관", List.of(),
                        BodySourceCode.PROVIDER_SUMMARY, "ACCEPTED", "TARGET_SUPPORT_CONFIRMED",
                        List.of("BUSINESS"), List.of("GENERAL_SUPPORT", "GRANT_SUBSIDY"),
                        List.of(), List.of(), Set.of("TITLE", "BODY")),
                availableCase("QA-12", "LOCAL_GOV_NOTICE", "공무원 채용",
                        "지방공무원 채용 절차를 안내합니다.", "테스트시청", List.of(),
                        BodySourceCode.DETAIL_PAGE_TEXT, "EXCLUDED", "TITLE_GROUP_B_MATCHED",
                        List.of(), List.of(), List.of(), List.of("AUTO_EXCLUDE_B_ADMINISTRATIVE"),
                        Set.of("TITLE")),
                availableCase("QA-13", "LOCAL_GOV_NOTICE", "공사 입찰",
                        "청사 공사 입찰 절차를 안내합니다.", "테스트시청", List.of(),
                        BodySourceCode.DETAIL_PAGE_TEXT, "EXCLUDED", "TITLE_GROUP_B_MATCHED",
                        List.of(), List.of(), List.of(), List.of("AUTO_EXCLUDE_B_ADMINISTRATIVE"),
                        Set.of("TITLE")),
                availableCase("QA-14", "BIZINFO", "TIPS·팁스 창업 지원사업",
                        "TIPS 참여기업 모집 안내입니다.", "창업진흥원", List.of(),
                        BodySourceCode.PROVIDER_SUMMARY, "EXCLUDED", "TITLE_GROUP_B_MATCHED",
                        List.of(), List.of("GENERAL_SUPPORT"), List.of(),
                        List.of("AUTO_EXCLUDE_B_INVESTMENT_STARTUP"), Set.of("TITLE")),
                availableCase("QA-15", "BIZINFO", "수출인증 지원사업",
                        "인증 획득 비용을 지원합니다.", "테스트기관", List.of(),
                        BodySourceCode.PROVIDER_SUMMARY, "EXCLUDED", "TITLE_GROUP_B_MATCHED",
                        List.of(), List.of("GENERAL_SUPPORT"), List.of("REVIEW_A_IP_CERTIFICATION"),
                        List.of("AUTO_EXCLUDE_B_EXPORT"), Set.of("TITLE")),
                availableCase("QA-16", "LOCAL_GOV_NOTICE", "스마트공장 소상공인 지원사업",
                        "소상공인의 수출 판로를 지원하는 사업입니다.", "테스트시청", List.of(),
                        BodySourceCode.DETAIL_PAGE_TEXT, "REVIEW_REQUIRED", "BODY_GROUP_B_MATCHED",
                        List.of("BUSINESS"), List.of("GENERAL_SUPPORT"),
                        List.of("REVIEW_A_MANUFACTURING_TECH_PRODUCT"),
                        List.of("AUTO_EXCLUDE_B_EXPORT"), Set.of("TITLE", "BODY")),
                new GoldenCase("QA-17", "LOCAL_GOV_NOTICE", "소상공인 정책자금 지원사업", null,
                        "테스트시청", List.of(), BodySourceCode.NONE, BodyAvailabilityCode.UNAVAILABLE,
                        "REVIEW_REQUIRED", "BODY_UNAVAILABLE", List.of("BUSINESS"),
                        List.of("GENERAL_SUPPORT", "POLICY_FINANCE"), List.of(), List.of(), Set.of("TITLE")),
                availableCase("QA-18", "BIZINFO", "소상공인 정책자금 안내",
                        "소상공인 정책자금 지원 대상과 방법을 안내합니다.", "테스트기관", List.of(),
                        BodySourceCode.PROVIDER_FULL_TEXT, "ACCEPTED", "TARGET_SUPPORT_CONFIRMED",
                        List.of("BUSINESS"), List.of("GENERAL_SUPPORT", "POLICY_FINANCE"),
                        List.of(), List.of(), Set.of("TITLE", "BODY")),
                availableCase("QA-19", "GOV24_PUBLIC_SERVICE", "청년 소상공인 배우자 취업지원금",
                        "청년 소상공인과 배우자에게 취업지원금을 지급합니다.", "테스트기관", List.of(),
                        BodySourceCode.PROVIDER_SUMMARY, "ACCEPTED", "TARGET_SUPPORT_CONFIRMED",
                        List.of("BUSINESS", "PERSONAL", "SPOUSE"),
                        List.of("GENERAL_SUPPORT", "GRANT_SUBSIDY"), List.of(), List.of(),
                        Set.of("TITLE", "BODY")),
                availableCase("QA-20", "BIZINFO", "자영업자 보증료 지원사업",
                        "자영업자를 위한 특례보증과 보증료 지원 내용을 안내합니다.", "테스트기관", List.of(),
                        BodySourceCode.PROVIDER_FULL_TEXT, "ACCEPTED", "TARGET_SUPPORT_CONFIRMED",
                        List.of("BUSINESS"), List.of("GENERAL_SUPPORT", "GUARANTEE"),
                        List.of(), List.of(), Set.of("TITLE", "BODY"))
        );
    }

    private GoldenCase availableCase(
            String caseId,
            String providerCode,
            String title,
            String bodyText,
            String agencyName,
            List<String> agencyAliases,
            BodySourceCode bodySourceCode,
            String expectedStatusCode,
            String expectedReasonCode,
            List<String> expectedTargetCategoryCodes,
            List<String> expectedSupportTypeCodes,
            List<String> expectedGroupACodes,
            List<String> expectedGroupBCodes,
            Set<String> expectedMatchLocations
    ) {
        return new GoldenCase(
                caseId,
                providerCode,
                title,
                bodyText,
                agencyName,
                agencyAliases,
                bodySourceCode,
                BodyAvailabilityCode.AVAILABLE,
                expectedStatusCode,
                expectedReasonCode,
                expectedTargetCategoryCodes,
                expectedSupportTypeCodes,
                expectedGroupACodes,
                expectedGroupBCodes,
                expectedMatchLocations
        );
    }

    private AnnouncementSourceClassificationResult decide(
            AnnouncementSourceClassificationRuleSet ruleSet,
            AnnouncementSourceClassificationInput input
    ) {
        return engine.selectDecision(input, ruleSet);
    }

    private void requireStatus(String caseId, AnnouncementSourceClassificationResult result, String expected) {
        require(caseId, expected.equals(result.semanticStatusCode().name()),
                "상태가 " + expected + "이어야 합니다. actual="
                        + result.semanticStatusCode().name() + ", reason=" + result.reasonCode().name());
    }

    private void requireReason(String caseId, AnnouncementSourceClassificationResult result, String expected) {
        require(caseId, expected.equals(result.reasonCode().name()),
                "사유가 " + expected + "이어야 합니다. actual=" + result.reasonCode().name());
    }

    private void requireExact(String caseId, String field, List<String> actual, List<String> expected) {
        require(caseId, actual.equals(expected),
                field + " 값이 " + expected + "이어야 합니다. actual=" + actual);
    }

    private void require(String caseId, boolean passed, String message) {
        if (!passed) {
            throw new ApiException(
                    ErrorCode.ANNOUNCEMENT_SOURCE_RULE_INVALID,
                    HttpStatus.CONFLICT,
                    caseId + " Golden QA 실패: " + message
            );
        }
    }

    private List<String> names(List<? extends Enum<?>> codes) {
        return codes.stream().map(Enum::name).toList();
    }

    private String signature(AnnouncementSourceClassificationResult result) {
        return String.join("|",
                result.semanticStatusCode().name(),
                result.reasonCode().name(),
                String.join(",", names(result.targetCategoryCodes())),
                String.join(",", names(result.supportTypeCodes())),
                String.join(",", result.groupACodes()),
                String.join(",", result.groupBCodes())
        );
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", exception);
        }
    }

    private record GoldenCase(
            String caseId,
            String providerCode,
            String title,
            String bodyText,
            String agencyName,
            List<String> agencyAliases,
            BodySourceCode bodySourceCode,
            BodyAvailabilityCode bodyAvailabilityCode,
            String expectedStatusCode,
            String expectedReasonCode,
            List<String> expectedTargetCategoryCodes,
            List<String> expectedSupportTypeCodes,
            List<String> expectedGroupACodes,
            List<String> expectedGroupBCodes,
            Set<String> expectedMatchLocations
    ) {
        private GoldenCase {
            agencyAliases = List.copyOf(agencyAliases);
            expectedTargetCategoryCodes = List.copyOf(expectedTargetCategoryCodes);
            expectedSupportTypeCodes = List.copyOf(expectedSupportTypeCodes);
            expectedGroupACodes = List.copyOf(expectedGroupACodes);
            expectedGroupBCodes = List.copyOf(expectedGroupBCodes);
            expectedMatchLocations = Set.copyOf(expectedMatchLocations);
        }

        private AnnouncementSourceClassificationInput toInput() {
            return toInput(providerCode);
        }

        private AnnouncementSourceClassificationInput toInput(String overriddenProviderCode) {
            return new AnnouncementSourceClassificationInput(
                    overriddenProviderCode,
                    title,
                    bodyText,
                    agencyName,
                    agencyAliases,
                    bodySourceCode,
                    bodyAvailabilityCode
            );
        }
    }

    /** 서버가 실행한 Golden QA 결과입니다. */
    public record GoldenGateResult(String runId, Map<String, String> signatures) {
        public GoldenGateResult {
            signatures = Map.copyOf(signatures);
        }

        public int caseCount() {
            return signatures.size();
        }

        public int selectChangedCaseCount(GoldenGateResult previous) {
            if (previous == null) {
                return 0;
            }
            List<String> changed = new ArrayList<>();
            signatures.forEach((caseId, signature) -> {
                if (!signature.equals(previous.signatures().get(caseId))) {
                    changed.add(caseId);
                }
            });
            return changed.size();
        }
    }
}
