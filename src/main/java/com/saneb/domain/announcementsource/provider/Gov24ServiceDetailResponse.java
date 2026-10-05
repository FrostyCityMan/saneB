package com.saneb.domain.announcementsource.provider;

import com.fasterxml.jackson.databind.JsonNode;

/** 공식 상세 API의 단건 계약. 네트워크·분류·저장은 수행하지 않는다. */
public record Gov24ServiceDetailResponse(
        String serviceId, String title, String purpose, String target,
        String selectionCriteria, String supportContent, String applicationMethod,
        String applicationPeriod, String requiredDocuments, String agencyName,
        String inquiry, String applicationSiteUrl
) {
    /** page=1/perPage=1/서비스ID EQ로 요청한 응답만 해석한다. 빈 응답은 별도 실패다. */
    public static Gov24ServiceDetailResponse selectDetails(String requestedServiceId, JsonNode root) {
        if (requestedServiceId == null || requestedServiceId.isBlank()) {
            throw new IllegalArgumentException("정부24 상세 조회에는 서비스ID가 필요합니다.");
        }
        if (root == null || !root.isObject() || !selectNumber(root, "page", 1)
                || !selectNumber(root, "perPage", 1) || !root.path("data").isArray()) {
            throw selectInvalid();
        }
        JsonNode data = root.path("data");
        if (data.isEmpty() && selectNumber(root, "currentCount", 0)) {
            throw new IllegalArgumentException("정부24 상세 응답에 요청한 서비스가 없습니다.");
        }
        if (data.size() != 1 || !selectNumber(root, "currentCount", 1) || !data.get(0).isObject()) {
            throw selectInvalid();
        }
        JsonNode item = data.get(0);
        String serviceId = selectText(item, "서비스ID");
        if (!requestedServiceId.equals(serviceId)) {
            throw new IllegalArgumentException("정부24 상세 응답의 서비스ID가 요청한 서비스와 다릅니다.");
        }
        String title = selectText(item, "서비스명");
        if (title == null) throw selectInvalid();
        return new Gov24ServiceDetailResponse(serviceId, title,
                selectText(item, "서비스목적"), selectText(item, "지원대상"),
                selectText(item, "선정기준"), selectText(item, "지원내용"),
                selectText(item, "신청방법"), selectText(item, "신청기한"),
                selectText(item, "구비서류"), selectText(item, "소관기관명"),
                selectText(item, "문의처"), selectText(item, "온라인신청사이트URL"));
    }

    private static boolean selectNumber(JsonNode root, String name, int expected) {
        JsonNode value = root.path(name);
        return value.isIntegralNumber() && value.canConvertToInt() && value.intValue() == expected;
    }

    private static String selectText(JsonNode item, String name) {
        JsonNode value = item.get(name);
        if (value == null || value.isNull()) return null;
        if (!value.isTextual()) throw selectInvalid();
        return value.textValue().isBlank() ? null : value.textValue().strip();
    }

    private static IllegalArgumentException selectInvalid() {
        return new IllegalArgumentException("정부24 상세 응답의 페이지·건수 또는 필드 형식이 공식 단건 계약과 다릅니다.");
    }

    /** 원격 텍스트·연락처·URL이 진단 로그에 자동 복사되지 않게 한다. */
    @Override public String toString() { return "Gov24ServiceDetailResponse[fields=REDACTED]"; }
}
