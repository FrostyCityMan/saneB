package com.saneb.domain.address.service;

import com.saneb.common.response.PageResponse;
import com.saneb.domain.address.dto.AddressSearchResponse;

public interface AddressService {

    PageResponse<AddressSearchResponse> selectRoadAddressList(
            String keyword,
            Integer page,
            Integer size,
            String firstSort,
            Boolean includeHistory
    );
}
