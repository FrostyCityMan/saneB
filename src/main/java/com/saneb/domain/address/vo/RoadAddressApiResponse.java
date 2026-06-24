/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: RoadAddressApiResponse.java
 * 작성자: 김도훈
 *
 */

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
