/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: AddressServiceImplTest.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.address.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.saneb.common.error.ApiException;
import com.saneb.common.response.PageResponse;
import com.saneb.domain.address.client.RoadAddressClient;
import com.saneb.domain.address.config.RoadAddressProperties;
import com.saneb.domain.address.dto.AddressSearchResponse;
import com.saneb.domain.address.vo.AddressSearchCondition;
import com.saneb.domain.address.vo.RoadAddressApiResponse;
import java.util.List;
import org.junit.jupiter.api.Test;

class AddressServiceImplTest {

    /**
     * 업무 데이터를 조회합니다.
     */
    @Test
    void selectRoadAddressListMapsJusoResponse() {
        AddressServiceImpl service = new AddressServiceImpl(
                new RoadAddressProperties(true, "https://example.test/address", "test-key", 5000),
                new StubRoadAddressClient(new RoadAddressApiResponse(new RoadAddressApiResponse.Results(
                        new RoadAddressApiResponse.Common("1", "1", "10", "0", "정상"),
                        List.of(new RoadAddressApiResponse.Juso(
                                "30112",
                                "세종특별자치시 도움6로 42",
                                "세종특별자치시 도움6로 42",
                                "",
                                "세종특별자치시 어진동 572",
                                "세종특별자치시",
                                "",
                                "어진동",
                                "3611010300",
                                "361103258001",
                                "3611010300105720000000001",
                                "행정안전부",
                                "0"
                        ))
                )))
        );

        PageResponse<AddressSearchResponse> response = service.selectRoadAddressList("세종 도움6로 42", 1, 10, "road", false);

        assertThat(response.totalCount()).isEqualTo(1);
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).postalCode()).isEqualTo("30112");
        assertThat(response.items().get(0).legalDongCode()).isEqualTo("3611010300");
    }

    /**
     * 업무 데이터를 조회합니다.
     */
    @Test
    void selectRoadAddressListRejectsShortKeyword() {
        AddressServiceImpl service = new AddressServiceImpl(
                new RoadAddressProperties(true, "https://example.test/address", "test-key", 5000),
                new StubRoadAddressClient(null)
        );

        assertThatThrownBy(() -> service.selectRoadAddressList("세", 1, 10, "road", false))
                .isInstanceOf(ApiException.class)
                .hasMessage("주소 검색어는 두 글자 이상 입력하세요.");
    }

    /**
     * 업무 데이터를 조회합니다.
     */
    @Test
    void selectRoadAddressListRejectsDisabledApi() {
        AddressServiceImpl service = new AddressServiceImpl(
                new RoadAddressProperties(false, "https://example.test/address", "test-key", 5000),
                new StubRoadAddressClient(null)
        );

        assertThatThrownBy(() -> service.selectRoadAddressList("세종", 1, 10, "road", false))
                .isInstanceOf(ApiException.class)
                .hasMessage("주소 검색 서비스가 설정되어 있지 않습니다.");
    }

    private record StubRoadAddressClient(RoadAddressApiResponse response) implements RoadAddressClient {

        /**
         * 업무 데이터를 조회합니다.
         *
         * @param condition 입력 값
         *
         * @return 처리 결과
         */
        @Override
        public RoadAddressApiResponse selectRoadAddressList(AddressSearchCondition condition) {
            return response;
        }
    }
}
