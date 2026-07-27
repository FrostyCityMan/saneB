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
import com.saneb.domain.announcementsource.vo.AnnouncementSourceCollectionRequestRow;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Gov24PublicServiceAnnouncementSourceProviderClient extends AbstractJsonAnnouncementSourceProviderClient {

    private static final String PROVIDER_CODE = "GOV24_PUBLIC_SERVICE";

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
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new com.saneb.common.error.ApiException(
                    com.saneb.common.error.ErrorCode.VALIDATION_FAILED,
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "정부24 공공서비스 API URL이 설정되지 않았습니다."
            );
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
                .map(this::toItem)
                .toList();
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
    private AnnouncementSourceProviderItem toItem(JsonNode node) {
        String title = selectText(node, "서비스명", "svcNm", "serviceName", "title", "pblancNm");
        String agencyName = selectText(node, "소관기관명", "jurMnofNm", "agencyName", "author", "jrsdInsttNm");
        String providerNoticeId = selectText(node, "서비스ID", "svcId", "serviceId", "id", "pblancId");
        String sourceUrl = selectText(node, "상세조회URL", "dtlUrl", "detailUrl", "sourceUrl", "link", "pblancUrl");
        String bodyText = selectText(node, "지원내용", "서비스목적요약", "sprtCn", "summary", "description", "bsnsSumryCn");
        String applicationMethodText = selectText(node, "신청방법", "신청방법명", "aplyMtdNm", "applicationMethod", "reqstMthPapersCn");
        String inquiryText = selectText(node, "문의처", "문의처명", "inqplCtadrList", "inquiry", "refrncNm");
        DateRange dateRange = selectDateRange(selectText(node, "신청기한", "신청기간", "aplyEndYmd", "reqstBeginEndDe"));
        String rawPayloadJson = selectRawPayloadJson(node);
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
                selectAttachments(node, List.of("첨부파일명", "fileName"), List.of("첨부파일URL", "fileUrl")),
                null
        );
    }
}
