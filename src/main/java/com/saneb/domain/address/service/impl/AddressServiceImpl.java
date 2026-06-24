/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: AddressServiceImpl.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.address.service.impl;

import com.saneb.common.error.ApiException;
import com.saneb.common.error.ErrorCode;
import com.saneb.common.response.PageResponse;
import com.saneb.domain.address.client.RoadAddressClient;
import com.saneb.domain.address.config.RoadAddressProperties;
import com.saneb.domain.address.dto.AddressSearchResponse;
import com.saneb.domain.address.service.AddressService;
import com.saneb.domain.address.vo.AddressSearchCondition;
import com.saneb.domain.address.vo.RoadAddressApiResponse;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class AddressServiceImpl implements AddressService {

    private static final Set<String> FIRST_SORT_CODES = Set.of("none", "road", "location");

    private final RoadAddressProperties properties;
    private final RoadAddressClient roadAddressClient;

    /**
     * 객체를 생성합니다.
     *
     * @param properties 입력 값
     *
     * @param roadAddressClient 입력 값
     */
    public AddressServiceImpl(RoadAddressProperties properties, RoadAddressClient roadAddressClient) {
        this.properties = properties;
        this.roadAddressClient = roadAddressClient;
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param keyword 입력 값
     *
     * @param page 입력 값
     *
     * @param size 입력 값
     *
     * @param firstSort 입력 값
     *
     * @param includeHistory 입력 값
     *
     * @return 처리 결과
     */
    @Override
    public PageResponse<AddressSearchResponse> selectRoadAddressList(
            String keyword,
            Integer page,
            Integer size,
            String firstSort,
            Boolean includeHistory
    ) {
        String normalizedKeyword = trimToNull(keyword);
        if (normalizedKeyword == null || normalizedKeyword.length() < 2) {
            throw validationFailed("주소 검색어는 두 글자 이상 입력하세요.");
        }
        int normalizedPage = page == null ? 1 : page;
        int normalizedSize = size == null ? 10 : size;
        if (normalizedPage < 1) {
            throw validationFailed("주소 검색 페이지는 1 이상으로 입력하세요.");
        }
        if (normalizedSize < 1 || normalizedSize > 20) {
            throw validationFailed("주소 검색 결과 수는 1개부터 20개까지 선택하세요.");
        }
        String normalizedFirstSort = normalizeFirstSort(firstSort);
        if (!properties.enabled() || trimToNull(properties.apiKey()) == null || trimToNull(properties.baseUrl()) == null) {
            throw new ApiException(
                    ErrorCode.INTERNAL_ERROR,
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "주소 검색 서비스가 설정되어 있지 않습니다."
            );
        }

        RoadAddressApiResponse apiResponse = roadAddressClient.selectRoadAddressList(new AddressSearchCondition(
                normalizedKeyword,
                normalizedPage,
                normalizedSize,
                normalizedFirstSort,
                Boolean.TRUE.equals(includeHistory)
        ));
        RoadAddressApiResponse.Common common = apiResponse == null
                || apiResponse.results() == null ? null : apiResponse.results().common();
        if (common == null) {
            throw addressServiceFailed();
        }
        if (!"0".equals(common.errorCode())) {
            String message = trimToNull(common.errorMessage());
            throw new ApiException(
                    ErrorCode.INTERNAL_ERROR,
                    HttpStatus.BAD_GATEWAY,
                    message == null ? "주소 검색 서비스가 일시적으로 원활하지 않습니다." : message
            );
        }
        List<AddressSearchResponse> items = safeJusoList(apiResponse).stream()
                .map(this::selectAddressSearchResponse)
                .toList();
        return PageResponse.of(items, normalizedPage, normalizedSize, parseLong(common.totalCount()));
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param juso 입력 값
     *
     * @return 처리 결과
     */
    private AddressSearchResponse selectAddressSearchResponse(RoadAddressApiResponse.Juso juso) {
        return new AddressSearchResponse(
                trimToNull(juso.zipNo()),
                trimToNull(juso.roadAddr()),
                trimToNull(juso.roadAddrPart1()),
                trimToNull(juso.roadAddrPart2()),
                trimToNull(juso.jibunAddr()),
                trimToNull(juso.siNm()),
                trimToNull(juso.sggNm()),
                trimToNull(juso.emdNm()),
                trimToNull(juso.admCd()),
                trimToNull(juso.rnMgtSn()),
                trimToNull(juso.bdMgtSn()),
                trimToNull(juso.bdNm()),
                "1".equals(juso.aptYn()) || "Y".equalsIgnoreCase(String.valueOf(juso.aptYn()))
        );
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param apiResponse 입력 값
     *
     * @return 처리 결과
     */
    private List<RoadAddressApiResponse.Juso> safeJusoList(RoadAddressApiResponse apiResponse) {
        if (apiResponse == null || apiResponse.results() == null || apiResponse.results().juso() == null) {
            return List.of();
        }
        return apiResponse.results().juso();
    }

    /**
     * 입력 값을 표준 형식으로 정규화합니다.
     *
     * @param firstSort 입력 값
     *
     * @return 처리 결과
     */
    private String normalizeFirstSort(String firstSort) {
        String normalized = trimToNull(firstSort);
        if (normalized == null) {
            return "none";
        }
        String lower = normalized.toLowerCase(Locale.ROOT);
        if (!FIRST_SORT_CODES.contains(lower)) {
            throw validationFailed("주소 검색 정렬 값이 올바르지 않습니다.");
        }
        return lower;
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param value 입력 값
     *
     * @return 처리 결과
     */
    private long parseLong(String value) {
        try {
            return Long.parseLong(String.valueOf(value == null ? "0" : value));
        } catch (NumberFormatException exception) {
            throw addressServiceFailed();
        }
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @return 처리 결과
     */
    private ApiException addressServiceFailed() {
        return new ApiException(
                ErrorCode.INTERNAL_ERROR,
                HttpStatus.BAD_GATEWAY,
                "주소 검색 서비스 응답이 올바르지 않습니다. 잠시 후 다시 시도하세요."
        );
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param message 입력 값
     *
     * @return 처리 결과
     */
    private ApiException validationFailed(String message) {
        return new ApiException(ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST, message);
    }

    /**
     * 문자열 입력 값을 정리합니다.
     *
     * @param value 입력 값
     *
     * @return 처리 결과
     */
    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
