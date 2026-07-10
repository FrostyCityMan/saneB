/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: BizInfoAnnouncementSourceProviderClient.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.announcementsource.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceCollectionRequestRow;
import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BizInfoAnnouncementSourceProviderClient extends AbstractJsonAnnouncementSourceProviderClient {

    private static final String PROVIDER_CODE = "BIZINFO";

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
    public BizInfoAnnouncementSourceProviderClient(
            ObjectMapper objectMapper,
            @Value("$" + "{saneb.announcement-source.providers.bizinfo.base-url:https://www.bizinfo.go.kr/uss/rss/bizinfoApi.do}") String baseUrl,
            @Value("$" + "{saneb.announcement-source.providers.bizinfo.api-key:}") String apiKey,
            @Value("$" + "{saneb.announcement-source.providers.bizinfo.timeout-millis:5000}") int timeoutMillis
    ) {
        super(objectMapper, timeoutMillis);
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
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
        URI uri = URI.create(selectRequestUrl(request));
        JsonNode root = selectJson(uri);
        List<JsonNode> nodes = selectItemNodes(root, List.of(
                List.of("jsonArray", "item"),
                List.of("channel", "item"),
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
                .append("crtfcKey=").append(encode(apiKey))
                .append("&dataType=json")
                .append("&pageIndex=1")
                .append("&pageUnit=").append(request.maxCount() == null ? 100 : Math.min(request.maxCount(), 500));
        if (request.searchCategoryCode() != null && !request.searchCategoryCode().isBlank()) {
            builder.append("&searchLclasId=").append(encode(request.searchCategoryCode()));
        }
        List<String> hashtags = new ArrayList<>();
        if (request.searchKeyword() != null && !request.searchKeyword().isBlank()) {
            hashtags.add(request.searchKeyword());
        }
        if (request.searchRegionCode() != null && !request.searchRegionCode().isBlank()) {
            hashtags.add(request.searchRegionCode());
        }
        if (!hashtags.isEmpty()) {
            builder.append("&hashtags=").append(encode(String.join(",", hashtags)));
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
        String title = selectText(node, "pblancNm", "title");
        String agencyName = selectText(node, "jrsdInsttNm", "author");
        String providerNoticeId = selectText(node, "pblancId", "seq");
        String sourceUrl = selectText(node, "pblancUrl", "link", "rceptEngnHmpgUrl");
        String bodyText = selectText(node, "bsnsSumryCn", "description");
        String applicationMethodText = selectText(node, "reqstMthPapersCn", "rceptEngnHmpgUrl");
        String inquiryText = selectText(node, "refrncNm");
        DateRange dateRange = selectDateRange(selectText(node, "reqstBeginEndDe", "reqstDt"));
        String rawPayloadJson = selectRawPayloadJson(node);
        Map<String, String> fields = selectFieldMap(
                "title", title,
                "agencyName", agencyName,
                "applicationPeriod", selectText(node, "reqstBeginEndDe", "reqstDt"),
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
                selectDateTime(selectText(node, "creatPnttm", "pubDate")),
                null,
                sourceUrl,
                bodyText,
                inquiryText,
                applicationMethodText,
                selectCompletenessCode(fields),
                selectMissingFieldsJson(fields),
                rawPayloadJson,
                selectRawHash(PROVIDER_CODE, rawPayloadJson),
                selectAttachments(
                        node,
                        List.of("fileNm", "printFileNm"),
                        List.of("flpthNm", "printFlpthNm")
                ),
                null
        );
    }
}
