package com.saneb.domain.address.client;

import com.saneb.domain.address.vo.AddressSearchCondition;
import com.saneb.domain.address.vo.RoadAddressApiResponse;

public interface RoadAddressClient {

    RoadAddressApiResponse selectRoadAddressList(AddressSearchCondition condition);
}
