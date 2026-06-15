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

    public RoadAddressHttpClient(RoadAddressProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(selectTimeoutMillis(properties.timeoutMillis())))
                .build();
    }

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

    private String queryParam(String name, String value) {
        return URLEncoder.encode(name, StandardCharsets.UTF_8)
                + "="
                + URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private long selectTimeoutMillis(int timeoutMillis) {
        return Math.max(timeoutMillis, 1000);
    }
}
