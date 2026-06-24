/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: RoadAddressHttpClient.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.address.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.common.error.ApiException;
import com.saneb.common.error.ErrorCode;
import com.saneb.domain.address.config.RoadAddressProperties;
import com.saneb.domain.address.vo.AddressSearchCondition;
import com.saneb.domain.address.vo.RoadAddressApiResponse;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class RoadAddressHttpClient implements RoadAddressClient {

    private final RoadAddressProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    /**
     * 객체를 생성합니다.
     *
     * @param properties 입력 값
     *
     * @param objectMapper 입력 값
     */
    public RoadAddressHttpClient(RoadAddressProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(selectTimeoutMillis(properties.timeoutMillis())))
                .build();
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param condition 입력 값
     *
     * @return 처리 결과
     */
    @Override
    public RoadAddressApiResponse selectRoadAddressList(AddressSearchCondition condition) {
        HttpRequest request = HttpRequest.newBuilder(selectUri(condition))
                .timeout(Duration.ofMillis(selectTimeoutMillis(properties.timeoutMillis())))
                .GET()
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ApiException(
                        ErrorCode.INTERNAL_ERROR,
                        HttpStatus.BAD_GATEWAY,
                        "주소 검색 서비스 응답이 원활하지 않습니다. 잠시 후 다시 시도하세요."
                );
            }
            return objectMapper.readValue(response.body(), RoadAddressApiResponse.class);
        } catch (IOException exception) {
            throw new ApiException(
                    ErrorCode.INTERNAL_ERROR,
                    HttpStatus.BAD_GATEWAY,
                    "주소 검색 서비스 응답을 해석하지 못했습니다. 잠시 후 다시 시도하세요."
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ApiException(
                    ErrorCode.INTERNAL_ERROR,
                    HttpStatus.BAD_GATEWAY,
                    "주소 검색 서비스 연결이 중단되었습니다. 잠시 후 다시 시도하세요."
            );
        }
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param condition 입력 값
     *
     * @return 처리 결과
     */
    private URI selectUri(AddressSearchCondition condition) {
        List<String> params = new ArrayList<>();
        params.add(queryParam("confmKey", properties.apiKey()));
        params.add(queryParam("currentPage", String.valueOf(condition.page())));
        params.add(queryParam("countPerPage", String.valueOf(condition.size())));
        params.add(queryParam("keyword", condition.keyword()));
        params.add(queryParam("resultType", "json"));
        params.add(queryParam("hstryYn", condition.includeHistory() ? "Y" : "N"));
        params.add(queryParam("firstSort", condition.firstSort()));
        params.add(queryParam("addInfoYn", "Y"));
        String separator = properties.baseUrl().contains("?") ? "&" : "?";
        return URI.create(properties.baseUrl() + separator + String.join("&", params));
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param name 입력 값
     *
     * @param value 입력 값
     *
     * @return 처리 결과
     */
    private String queryParam(String name, String value) {
        return URLEncoder.encode(name, StandardCharsets.UTF_8)
                + "="
                + URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param timeoutMillis 입력 값
     *
     * @return 처리 결과
     */
    private long selectTimeoutMillis(int timeoutMillis) {
        return Math.max(timeoutMillis, 1000);
    }
}
