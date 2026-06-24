/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: MemberProfileRow.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.member.vo;

import java.math.BigDecimal;
import java.util.UUID;

public record MemberProfileRow(
        UUID userId,
        Integer birthYear,
        String regionCode,
        String postalCode,
        String roadAddress,
        String jibunAddress,
        String detailAddress,
        String sidoName,
        String sigunguName,
        String eupmyeondongName,
        String legalDongCode,
        String roadNameCode,
        String buildingManagementNo,
        String addressSourceCode,
        Boolean hasIncome,
        String incomePresenceCode,
        BigDecimal incomeAmount,
        String healthInsuranceBasisCode
) {
}
