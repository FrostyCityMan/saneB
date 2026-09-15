/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: Gov24PublicServiceAnnouncementSourceProviderClient.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.announcementsource.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.common.error.ApiException;
import com.saneb.common.error.ErrorCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceSearchPlan.SearchQuery;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceTextNormalizer;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceCollectionRequestRow;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpStatus;

@Component
public class Gov24PublicServiceAnnouncementSourceProviderClient extends AbstractJsonAnnouncementSourceProviderClient {

    private static final String PROVIDER_CODE = "GOV24_PUBLIC_SERVICE";
    private static final String OFFICIAL_HOST = "api.odcloud.kr";
    private static final String OFFICIAL_LIST_PATH = "/api/gov24/v3/serviceList";
    private static final AnnouncementSourceTextNormalizer TEXT_NORMALIZER = new AnnouncementSourceTextNormalizer();

    private final String baseUrl;
    private final String apiKey;

    /**
     * 객체를 생성합니다.
     *
     * @param objectMapper 입력 값
     *
     * @param baseUrl 입력 값
     *
     * @param apiKey 입력 값
     *
     * @param timeoutMillis 입력 값
     */
    public Gov24PublicServiceAnnouncementSourceProviderClient(
            ObjectMapper objectMapper,
            @Value("$" + "{saneb.announcement-source.providers.gov24.base-url:}") String baseUrl,
            @Value("$" + "{saneb.announcement-source.providers.gov24.api-key:}") String apiKey,
            @Value("$" + "{saneb.announcement-source.providers.gov24.timeout-millis:5000}") int timeoutMillis
    ) {
        super(objectMapper, timeoutMillis);
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
    }

    /**
     * 정부24 공공서비스 API 호출 설정이 준비됐는지 확인합니다.
     *
     * @return API URL과 인증키가 모두 있으면 true
     */
    @Override
    public boolean isConfigured() {
        return baseUrl != null && !baseUrl.isBlank() && apiKey != null && !apiKey.isBlank();
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @return 처리 결과
     */
    @Override
    public String selectProviderCode() {
        return PROVIDER_CODE;
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param request 입력 값
     *
     * @return 처리 결과
     */
    @Override
    public List<AnnouncementSourceProviderItem> selectSourceItemList(AnnouncementSourceCollectionRequestRow request) {
        validateApiKey(apiKey);
        URI endpoint = selectEndpoint();
        if (OFFICIAL_HOST.equalsIgnoreCase(endpoint.getHost())) {
            return selectOfficialItemList(endpoint, request, request.searchKeyword());
        }
        JsonNode root = selectJson(URI.create(selectRequestUrl(request)));
        List<JsonNode> nodes = selectItemNodes(root, List.of(
                List.of("data"),
                List.of("body", "items", "item"),
                List.of("response", "body", "items", "item"),
                List.of("items"),
                List.of("item")
        ));
        if (nodes.isEmpty()) {
            nodes = selectFirstArray(root);
        }
        return nodes.stream()
                .map(this::selectProviderItem)
                .toList();
    }

    @Override
    public List<AnnouncementSourceProviderItem> selectSearchPlanItemList(
            AnnouncementSourceCollectionRequestRow request,
            SearchQuery query
    ) {
        URI endpoint = selectEndpoint();
        if (!OFFICIAL_HOST.equalsIgnoreCase(endpoint.getHost())) {
            return selectSourceItemList(request.withSearchKeyword(query.keyword()));
        }
        validateApiKey(apiKey);
        if (query == null || !hasText(query.targetTerm()) || !hasText(query.supportTerm())
                || !(query.targetTerm() + " " + query.supportTerm()).equals(query.keyword())) {
            throw validationFailure("정부24 검색 계획에 지원대상·지원형태 대표어와 일치하는 조합이 필요합니다.");
        }
        String target = TEXT_NORMALIZER.selectNormalizedText(query.targetTerm()).normalizedText();
        String support = TEXT_NORMALIZER.selectNormalizedText(query.supportTerm()).normalizedText();
        if (target.isBlank() || support.isBlank()) {
            throw validationFailure("정부24 대표 검색어에는 문자 또는 숫자가 포함되어야 합니다.");
        }
        // LIKE에 합친 문장을 보내지 않는다. 후보를 여기서 버리면 TITLE A 예외가 우회된다.
        // 같은 제목의 대상·지원 조합과 A/B 예외는 고정 release의 공통 TITLE 단계에서 판정한다.
        return selectOfficialItemList(endpoint, request, query.targetTerm());
    }

    private URI selectEndpoint() {
        if (!hasText(baseUrl)) {
            throw validationFailure("정부24 공공서비스 API URL이 설정되지 않았습니다.");
        }
        try {
            URI endpoint = URI.create(baseUrl);
            if (endpoint.getHost() == null) {
                throw validationFailure("정부24 공공서비스 API URL에 유효한 호스트가 필요합니다.");
            }
            return endpoint;
        } catch (IllegalArgumentException exception) {
            // 입력 URL과 인증정보가 예외 메시지에 포함되지 않게 한다.
            throw validationFailure("정부24 공공서비스 API URL 형식이 잘못되었습니다.");
        }
    }

    private List<AnnouncementSourceProviderItem> selectOfficialItemList(
            URI endpoint,
            AnnouncementSourceCollectionRequestRow request,
            String keyword
    ) {
        if (!"https".equalsIgnoreCase(endpoint.getScheme())
                || (endpoint.getPort() != -1 && endpoint.getPort() != 443)
                || !OFFICIAL_LIST_PATH.equals(endpoint.getRawPath())
                || endpoint.getRawUserInfo() != null || endpoint.getRawQuery() != null
                || endpoint.getRawFragment() != null) {
            throw validationFailure("정부24 공식 API URL은 HTTPS의 /api/gov24/v3/serviceList 경로로 설정하고 쿼리·인증정보·조각을 넣지 마세요.");
        }
        if (hasText(request.searchRegionCode()) || hasText(request.searchCategoryCode())
                || request.startDate() != null || request.endDate() != null) {
            throw validationFailure("정부24 공식 목록 API에는 지역·내부 카테고리·신청기간의 동일한 검색 조건이 없습니다. 해당 조건을 비우고 제목 검색을 사용해 주세요.");
        }
        int maximumCount = request.maxCount() == null ? 100 : request.maxCount();
        if (maximumCount < 1) {
            throw validationFailure("정부24 수집 요청 건수는 1건 이상이어야 합니다.");
        }
        int pageSize = Math.min(maximumCount, 500);
        StringBuilder url = new StringBuilder(endpoint.toString())
                .append("?serviceKey=").append(encode(apiKey))
                .append("&page=1&perPage=").append(pageSize);
        if (hasText(keyword)) {
            url.append('&').append(encode("cond[서비스명::LIKE]")).append('=').append(encode(keyword));
        }
        JsonNode root = selectJson(URI.create(url.toString()));
        JsonNode data = root == null ? null : root.get("data");
        if (root == null || !root.isObject() || data == null || !data.isArray()
                || data.size() > pageSize || !matchesCount(root.get("page"), 1)
                || !matchesCount(root.get("perPage"), pageSize)
                || !matchesCount(root.get("currentCount"), data.size())) {
            throw responseFailure();
        }
        for (JsonNode node : data) {
            if (!node.isObject() || !node.path("서비스ID").isTextual()
                    || !hasText(node.path("서비스ID").textValue())
                    || !node.path("서비스명").isTextual() || !hasText(node.path("서비스명").textValue())) {
                throw responseFailure();
            }
        }
        return selectItemNodes(root, List.of(List.of("data"))).stream().map(this::selectProviderItem).toList();
    }

    private boolean matchesCount(JsonNode node, int expected) {
        return node != null && node.isIntegralNumber() && node.canConvertToInt() && node.intValue() == expected;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private ApiException validationFailure(String message) {
        return new ApiException(ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST, message);
    }

    private ApiException responseFailure() {
        return new ApiException(ErrorCode.INTERNAL_ERROR, HttpStatus.BAD_GATEWAY,
                "정부24 공식 목록 응답의 페이지·건수·서비스 ID·제목 형식이 계약과 다릅니다. 수집 성공 0건으로 처리하지 않습니다.");
    }

    /**
     * 요청 URL을 생성합니다.
     *
     * @param request 입력 값
     *
     * @return 처리 결과
     */
    private String selectRequestUrl(AnnouncementSourceCollectionRequestRow request) {
        StringBuilder builder = new StringBuilder(baseUrl)
                .append(baseUrl.contains("?") ? "&" : "?")
                .append("serviceKey=").append(encode(apiKey))
                .append("&type=json")
                .append("&pageNo=1")
                .append("&numOfRows=").append(request.maxCount() == null ? 100 : Math.min(request.maxCount(), 500));
        if (request.searchKeyword() != null && !request.searchKeyword().isBlank()) {
            builder.append("&keyword=").append(encode(request.searchKeyword()));
        }
        if (request.searchRegionCode() != null && !request.searchRegionCode().isBlank()) {
            builder.append("&region=").append(encode(request.searchRegionCode()));
        }
        return builder.toString();
    }

    /**
     * provider item으로 변환합니다.
     *
     * @param node 입력 값
     *
     * @return 처리 결과
     */
    AnnouncementSourceProviderItem selectProviderItem(JsonNode node) {
        String title = selectText(node, "서비스명", "svcNm", "serviceName", "title", "pblancNm");
        String agencyName = selectText(node, "소관기관명", "jurMnofNm", "agencyName", "author", "jrsdInsttNm");
        String providerNoticeId = selectText(node, "서비스ID", "svcId", "serviceId", "id", "pblancId");
        String sourceUrl = selectText(node, "상세조회URL", "dtlUrl", "detailUrl", "sourceUrl", "link", "pblancUrl");
        String bodyText = selectText(node, "지원내용", "서비스목적요약", "sprtCn", "summary", "description", "bsnsSumryCn");
        String applicationMethodText = selectText(node, "신청방법", "신청방법명", "aplyMtdNm", "applicationMethod", "reqstMthPapersCn");
        String inquiryText = selectText(node, "문의처", "문의처명", "전화문의", "inqplCtadrList", "inquiry", "refrncNm");
        DateRange dateRange = selectDateRange(selectText(node, "신청기한", "신청기간", "aplyEndYmd", "reqstBeginEndDe"));
        String rawPayloadJson = selectRawPayloadJsonWithoutAttachments(node);
        Map<String, String> fields = selectFieldMap(
                "title", title,
                "agencyName", agencyName,
                "applicationPeriod", selectText(node, "신청기한", "신청기간", "aplyEndYmd", "reqstBeginEndDe"),
                "sourceUrl", sourceUrl,
                "bodyText", bodyText,
                "inquiryText", inquiryText,
                "applicationMethodText", applicationMethodText
        );
        return new AnnouncementSourceProviderItem(
                PROVIDER_CODE,
                providerNoticeId,
                title == null ? "제목 없음" : title,
                agencyName,
                dateRange.startDate(),
                dateRange.endDate(),
                selectDateTime(selectText(node, "등록일시", "등록일", "createdAt", "creatPnttm")),
                selectDateTime(selectText(node, "수정일시", "수정일", "modifiedAt")),
                sourceUrl,
                bodyText,
                inquiryText,
                applicationMethodText,
                selectCompletenessCode(fields),
                selectMissingFieldsJson(fields),
                rawPayloadJson,
                selectRawHash(PROVIDER_CODE, rawPayloadJson),
                List.of(),
                null
        );
    }
}
