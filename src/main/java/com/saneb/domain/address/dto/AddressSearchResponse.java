package com.saneb.domain.address.dto;

public record AddressSearchResponse(
        String postalCode,
        String roadAddress,
        String roadAddressPart1,
        String roadAddressPart2,
        String jibunAddress,
        String sidoName,
        String sigunguName,
        String eupmyeondongName,
        String legalDongCode,
        String roadNameCode,
        String buildingManagementNo,
        String buildingName,
        boolean apartment
) {
}
