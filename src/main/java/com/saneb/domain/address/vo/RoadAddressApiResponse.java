package com.saneb.domain.address.vo;

import java.util.List;

public record RoadAddressApiResponse(
        Results results
) {

    public record Results(
            Common common,
            List<Juso> juso
    ) {
    }

    public record Common(
            String totalCount,
            String currentPage,
            String countPerPage,
            String errorCode,
            String errorMessage
    ) {
    }

    public record Juso(
            String zipNo,
            String roadAddr,
            String roadAddrPart1,
            String roadAddrPart2,
            String jibunAddr,
            String siNm,
            String sggNm,
            String emdNm,
            String admCd,
            String rnMgtSn,
            String bdMgtSn,
            String bdNm,
            String aptYn
    ) {
    }
}
