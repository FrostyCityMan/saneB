package com.saneb.domain.candidatepreview.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CandidatePreviewRequest(
        @Size(max = 100, message = "대표자명은 100자 이하로 입력하세요.")
        String representativeName,

        Integer birthYear,

        @Size(max = 30, message = "지역 코드는 30자 이하로 입력하세요.")
        String regionCode,

        @Size(max = 30, message = "업종 코드는 30자 이하로 입력하세요.")
        String ksicCode,

        @DecimalMin(value = "0", message = "연매출은 0 이상으로 입력하세요.")
        BigDecimal annualRevenue,

        LocalDate openingDate,
        Boolean hasSpouse,
        Boolean hasChild,
        Boolean hasParent,
        List<FamilyPreviewRequest> families
) {

    public record FamilyPreviewRequest(
            @Size(max = 30, message = "가족 관계 코드는 30자 이하로 입력하세요.")
            String relationTypeCode,
            Integer birthYear,
            @Size(max = 50, message = "학령 상태 코드는 50자 이하로 입력하세요.")
            String schoolAgeStatusCode,
            Boolean cohabiting
    ) {
    }
}
