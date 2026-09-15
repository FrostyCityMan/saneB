package com.saneb.domain.announcementsource.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.saneb.common.error.ApiException;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceSearchPlan.SearchQuery;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceCollectionRequestRow;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;

class Gov24PublicServiceAnnouncementSourceProviderClientTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String ENDPOINT = "https://api.odcloud.kr/api/gov24/v3/serviceList";
    private static final String TEST_CREDENTIAL = "fixture-only+/=";

    @Test
    void usesOfficialPaginationAndEncodedTitleConditionWithoutLegacyParameters() {
        RecordingClient client = new RecordingClient(ENDPOINT, envelope(7, "소상공인 지원"));
        var result = client.selectSourceItemList(request("소상공인 + 지원", 7));

        assertThat(result).hasSize(1);
        assertThat(client.requests).hasSize(1);
        assertThat(query(client.requests.getFirst())).containsExactlyInAnyOrderEntriesOf(Map.of(
                "serviceKey", TEST_CREDENTIAL, "page", "1", "perPage", "7",
                "cond[서비스명::LIKE]", "소상공인 + 지원"));
        assertThat(client.requests.getFirst().getPath()).isEqualTo("/api/gov24/v3/serviceList");
        assertThat(result.getFirst().attachments()).isEmpty();
    }

    @CsvSource({"1,1", "500,500", "900,500"})
    @ParameterizedTest
    void boundsCandidatePageWithoutAddingMoreNetworkCalls(int requested, int expected) {
        RecordingClient client = new RecordingClient(ENDPOINT, envelope(expected));
        assertThat(client.selectSourceItemList(request(null, requested))).isEmpty();
        assertThat(query(client.requests.getFirst())).containsEntry("perPage", String.valueOf(expected))
                .doesNotContainKey("cond[서비스명::LIKE]");
        assertThat(client.requests).hasSize(1);
    }

    @Test
    void defaultsToOneHundredCandidates() {
        RecordingClient client = new RecordingClient(ENDPOINT, envelope(100));
        assertThat(client.selectSourceItemList(request(null, null))).isEmpty();
        assertThat(query(client.requests.getFirst())).containsEntry("perPage", "100");
    }

    @Test
    void combinationUsesTypedTargetRemotelyWithoutBypassingCommonTitleClassification() {
        ObjectNode response = envelope(100, "소상공인 경영안정 자금 지원", "지원 확대 소상공인 안내",
                "소상공인 정보", "일반 지원", "소상공인 지원 수출");
        ((ObjectNode) response.path("data").get(2)).put("지원내용", "본문에만 지원이라는 단어가 있음");
        RecordingClient client = new RecordingClient(ENDPOINT, response);
        SearchQuery plan = plan("소상공인", "지원");

        var result = client.selectSearchPlanItemList(request("기존 수동 검색어", 100), plan);

        assertThat(query(client.requests.getFirst())).containsEntry("cond[서비스명::LIKE]", "소상공인");
        assertThat(result).extracting(AnnouncementSourceProviderItem::title)
                .containsExactly("소상공인 경영안정 자금 지원", "지원 확대 소상공인 안내",
                        "소상공인 정보", "일반 지원", "소상공인 지원 수출");
        // 조합과 A/B 여부를 adapter가 확정하지 않는다. 공통 TITLE 단계가 저장 전에 판단한다.
        assertThat(client.requests).hasSize(1);
    }

    @Test
    void doesNotSplitMultiwordTargetOrTurnCandidateRetrievalIntoFinalFiltering() {
        RecordingClient client = new RecordingClient(ENDPOINT,
                envelope(100, "청년 창업 지원 안내", "청년 지원 창업 안내", "청년 창업", "지원 안내"));
        var result = client.selectSearchPlanItemList(request(null, 100), plan("청년 창업", "지원"));
        assertThat(query(client.requests.getFirst())).containsEntry("cond[서비스명::LIKE]", "청년 창업");
        assertThat(result).hasSize(4);
    }

    @Test
    void preservesCandidateTitlesForCommonRuleNormalization() {
        RecordingClient client = new RecordingClient(ENDPOINT, envelope(100, "ＳＭＥ  경영 지원", "SME 정책 정보"));
        assertThat(client.selectSearchPlanItemList(request(null, 100), plan("sme", "지원")))
                .extracting(AnnouncementSourceProviderItem::title).containsExactly("ＳＭＥ 경영 지원", "SME 정책 정보");
    }

    @Test
    void doesNotDiscardTitleAExceptionJustBecauseSupportRepresentativeIsMissing() {
        RecordingClient client = new RecordingClient(ENDPOINT, envelope(100, "소상공인 기술개발"));
        assertThat(client.selectSearchPlanItemList(request(null, 100), plan("소상공인", "지원")))
                .extracting(AnnouncementSourceProviderItem::title).containsExactly("소상공인 기술개발");
        assertThat(client.requests).hasSize(1);
    }

    @Test
    void preservesCustomEndpointContractAndDefaultSearchPlan() {
        RecordingClient client = new RecordingClient("https://example.go.kr/gov24?format=custom", envelope(100));
        client.selectSearchPlanItemList(request(null, 100), plan("청년 창업", "지원"));
        assertThat(query(client.requests.getFirst())).containsExactlyInAnyOrderEntriesOf(Map.of(
                "format", "custom", "serviceKey", TEST_CREDENTIAL, "type", "json", "pageNo", "1",
                "numOfRows", "100", "keyword", "청년 창업 지원"));
    }

    @ValueSource(strings = {
            "http://api.odcloud.kr/api/gov24/v3/serviceList",
            "https://api.odcloud.kr:444/api/gov24/v3/serviceList",
            "https://api.odcloud.kr/api/gov24/v3/serviceDetail",
            "https://api.odcloud.kr/api/gov24/v3/serviceList?serviceKey=fixture-only",
            "https://api.odcloud.kr/api/gov24/v3/serviceList#fragment",
            "https://fixture-only@api.odcloud.kr/api/gov24/v3/serviceList",
            "https://api.odcloud.kr/api/gov24/v3/%73erviceList",
            "not an endpoint", "relative/path"
    })
    @ParameterizedTest
    void refusesInvalidOfficialEndpointBeforeAnyRequestWithoutEchoingInput(String endpoint) {
        RecordingClient client = new RecordingClient(endpoint, envelope(100));
        assertThatThrownBy(() -> client.selectSourceItemList(request(null, 100)))
                .isInstanceOf(ApiException.class).hasMessageNotContaining(endpoint)
                .hasMessageNotContaining(TEST_CREDENTIAL).hasMessageNotContaining("fixture-only@");
        assertThat(client.requests).isEmpty();
    }

    @Test
    void refusesUnmappedScopeInsteadOfPretendingRemoteFilteringSucceeded() {
        for (int option = 0; option < 4; option++) {
            var request = new AnnouncementSourceCollectionRequestRow(UUID.randomUUID(), "ASR-TEST",
                    "GOV24_PUBLIC_SERVICE", "MANUAL", "APPROVED", null, null, null, "소상공인",
                    option == 0 ? "SEOUL" : null, option == 1 ? "BUSINESS" : null,
                    option == 2 ? LocalDate.of(2026, 9, 1) : null,
                    option == 3 ? LocalDate.of(2026, 9, 30) : null,
                    100, null, null, null, null, null, null, null, null);
            RecordingClient client = new RecordingClient(ENDPOINT, envelope(100));
            assertThatThrownBy(() -> client.selectSourceItemList(request))
                    .isInstanceOf(ApiException.class).hasMessageContaining("지역·내부 카테고리·신청기간");
            assertThat(client.requests).isEmpty();
        }
    }

    @ValueSource(ints = {0, -1})
    @ParameterizedTest
    void refusesNonpositiveLimit(int maximum) {
        RecordingClient client = new RecordingClient(ENDPOINT, envelope(100));
        assertThatThrownBy(() -> client.selectSourceItemList(request(null, maximum)))
                .isInstanceOf(ApiException.class).hasMessageContaining("1건 이상");
        assertThat(client.requests).isEmpty();
    }

    @NullAndEmptySource
    @ValueSource(strings = {" ", "!!!"})
    @ParameterizedTest
    void refusesEmptySearchPlanTerms(String term) {
        RecordingClient client = new RecordingClient(ENDPOINT, envelope(100));
        assertThatThrownBy(() -> client.selectSearchPlanItemList(request(null, 100), plan(term, "지원")))
                .isInstanceOf(ApiException.class);
        assertThat(client.requests).isEmpty();
    }

    @Test
    void refusesMismatchedCombination() {
        RecordingClient client = new RecordingClient(ENDPOINT, envelope(100));
        assertThatThrownBy(() -> client.selectSearchPlanItemList(request(null, 100),
                new SearchQuery(1, "TARGET", "소상공인", "SUPPORT", "지원", "다른 검색어")))
                .isInstanceOf(ApiException.class);
        assertThat(client.requests).isEmpty();
    }

    @ValueSource(strings = {"null", "[]", "{}", "{\"data\":[]}",
            "{\"code\":401,\"message\":\"fixture-only-error\",\"items\":[]}",
            "{\"page\":1,\"perPage\":100,\"currentCount\":0,\"data\":{}}",
            "{\"page\":2,\"perPage\":100,\"currentCount\":0,\"data\":[]}",
            "{\"page\":1,\"perPage\":10,\"currentCount\":0,\"data\":[]}",
            "{\"page\":1,\"perPage\":100,\"currentCount\":1,\"data\":[]}",
            "{\"page\":1,\"perPage\":100,\"currentCount\":\"0\",\"data\":[]}",
            "{\"page\":1,\"perPage\":100,\"currentCount\":1,\"data\":[{}]}"})
    @ParameterizedTest
    void doesNotTreatErrorOrChangedSchemaAsSuccessfulEmptyCollection(String json) throws Exception {
        RecordingClient client = new RecordingClient(ENDPOINT, MAPPER.readTree(json));
        assertThatThrownBy(() -> client.selectSourceItemList(request(null, 100)))
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> assertThat(exception.httpStatus()).isEqualTo(HttpStatus.BAD_GATEWAY))
                .hasMessageNotContaining("fixture-only-error").hasMessageNotContaining(TEST_CREDENTIAL);
        assertThat(client.requests).hasSize(1);
    }

    @Test
    void refusesOversizedPageEvenWithConsistentReportedCount() {
        RecordingClient client = new RecordingClient(ENDPOINT, envelope(1, "소상공인 지원", "청년 지원"));
        assertThatThrownBy(() -> client.selectSourceItemList(request(null, 1))).isInstanceOf(ApiException.class);
    }

    @Test
    void mapsOfficialTelephoneFieldWithoutInferringAttachmentAbsence() {
        ObjectNode response = envelope(100, "소상공인 지원");
        ((ObjectNode) response.path("data").get(0)).put("전화문의", "공식 문의 안내")
                .put("지원내용", "지원 본문").put("첨부파일명", "fixture.pdf");
        RecordingClient client = new RecordingClient(ENDPOINT, response);
        var item = client.selectSourceItemList(request(null, 100)).getFirst();
        assertThat(item.inquiryText()).isEqualTo("공식 문의 안내");
        assertThat(item.bodyText()).isEqualTo("지원 본문");
        assertThat(item.attachments()).isEmpty();
        assertThat(item.rawPayloadJson()).doesNotContain("fixture.pdf", "첨부파일명");
    }

    private static SearchQuery plan(String target, String support) {
        return new SearchQuery(1, "TARGET", target, "SUPPORT", support, target + " " + support);
    }

    private static AnnouncementSourceCollectionRequestRow request(String keyword, Integer maximum) {
        return new AnnouncementSourceCollectionRequestRow(UUID.randomUUID(), "ASR-TEST", "GOV24_PUBLIC_SERVICE",
                "MANUAL", "APPROVED", null, null, null, keyword, null, null, null, null, maximum,
                null, null, null, null, null, null, null, null);
    }

    private static ObjectNode envelope(int pageSize, String... titles) {
        ObjectNode root = MAPPER.createObjectNode().put("page", 1).put("perPage", pageSize)
                .put("currentCount", titles.length).put("totalCount", titles.length).put("matchCount", titles.length);
        var items = root.putArray("data");
        for (int i = 0; i < titles.length; i++) {
            items.addObject().put("서비스ID", "FIXTURE-" + i).put("서비스명", titles[i]);
        }
        return root;
    }

    private static Map<String, String> query(URI uri) {
        Map<String, String> values = new LinkedHashMap<>();
        Arrays.stream(uri.getRawQuery().split("&")).forEach(pair -> {
            String[] parts = pair.split("=", 2);
            values.put(URLDecoder.decode(parts[0], StandardCharsets.UTF_8),
                    URLDecoder.decode(parts[1], StandardCharsets.UTF_8));
        });
        return values;
    }

    private static final class RecordingClient extends Gov24PublicServiceAnnouncementSourceProviderClient {
        private final JsonNode response;
        private final List<URI> requests = new ArrayList<>();

        private RecordingClient(String endpoint, JsonNode response) {
            super(MAPPER, endpoint, TEST_CREDENTIAL, 1000);
            this.response = response;
        }

        @Override
        protected JsonNode selectJson(URI uri) {
            requests.add(uri);
            return response;
        }
    }
}
