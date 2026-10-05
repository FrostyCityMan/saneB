package com.saneb.domain.announcementsource.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class Gov24ServiceDetailResponseTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ObjectNode selectResponse() {
        ObjectNode root = MAPPER.createObjectNode().put("page", 1).put("perPage", 1).put("currentCount", 1);
        root.putArray("data").addObject().put("서비스ID", "TEST-001").put("서비스명", "테스트 지원")
                .put("지원내용", "첫 문단\n둘째 문단").put("구비서류", "신청서 제출 안내")
                .put("온라인신청사이트URL", "https://example.invalid/apply");
        return root;
    }

    @Test void keepsBodyDocumentsAndApplicationLinkSeparateWithoutInventingFiles() {
        var value = Gov24ServiceDetailResponse.selectDetails("TEST-001", selectResponse());
        assertThat(value.supportContent()).isEqualTo("첫 문단\n둘째 문단");
        assertThat(value.requiredDocuments()).isEqualTo("신청서 제출 안내");
        assertThat(value.applicationSiteUrl()).isEqualTo("https://example.invalid/apply");
        assertThat(value.purpose()).isNull();
        assertThat(value.toString()).isEqualTo("Gov24ServiceDetailResponse[fields=REDACTED]");
    }

    @Test void missingServiceIsNotSuccessfulEmptyBody() {
        var root = selectResponse().put("currentCount", 0);
        root.putArray("data");
        assertThatThrownBy(() -> Gov24ServiceDetailResponse.selectDetails("TEST-001", root))
                .hasMessage("정부24 상세 응답에 요청한 서비스가 없습니다.");
    }

    @Test void rejectsDifferentServiceWithoutEchoingRemoteData() {
        var root = selectResponse();
        ((ObjectNode) root.path("data").get(0)).put("서비스ID", "DO-NOT-LOG");
        assertThatThrownBy(() -> Gov24ServiceDetailResponse.selectDetails("TEST-001", root))
                .hasMessage("정부24 상세 응답의 서비스ID가 요청한 서비스와 다릅니다.")
                .hasMessageNotContaining("DO-NOT-LOG");
    }

    @ParameterizedTest
    @ValueSource(strings = {"page", "perPage", "currentCount"})
    void refusesStringDecimalAndOutOfRangeCounters(String name) {
        for (var invalid : new com.fasterxml.jackson.databind.JsonNode[]{
                MAPPER.getNodeFactory().textNode("1"), MAPPER.getNodeFactory().numberNode(1.0),
                MAPPER.getNodeFactory().numberNode(4294967297L), MAPPER.getNodeFactory().numberNode(2)}) {
            var root = selectResponse(); root.set(name, invalid);
            assertThatThrownBy(() -> Gov24ServiceDetailResponse.selectDetails("TEST-001", root))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test void rejectsExtraRowsAndErrorEnvelopes() {
        var root = selectResponse();
        ((com.fasterxml.jackson.databind.node.ArrayNode) root.path("data")).add(root.path("data").get(0).deepCopy());
        assertThatThrownBy(() -> Gov24ServiceDetailResponse.selectDetails("TEST-001", root))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Gov24ServiceDetailResponse.selectDetails("TEST-001",
                MAPPER.createObjectNode().put("error", "DO-NOT-LOG")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageNotContaining("DO-NOT-LOG");
    }

    @ParameterizedTest
    @ValueSource(strings = {"서비스ID", "서비스명", "지원내용", "구비서류", "온라인신청사이트URL"})
    void refusesNonTextFields(String field) {
        var root = selectResponse();
        ((ObjectNode) root.path("data").get(0)).putObject(field).put("value", "DO-NOT-LOG");
        assertThatThrownBy(() -> Gov24ServiceDetailResponse.selectDetails("TEST-001", root))
                .isInstanceOf(IllegalArgumentException.class).hasMessageNotContaining("DO-NOT-LOG");
    }

    @Test void blankTitleIsNotAValidDetail() {
        var root = selectResponse();
        ((ObjectNode) root.path("data").get(0)).put("서비스명", "  ");
        assertThatThrownBy(() -> Gov24ServiceDetailResponse.selectDetails("TEST-001", root))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
