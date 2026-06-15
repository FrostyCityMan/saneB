package com.saneb.domain.member.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record MemberBasicInfoSaveRequest(
        Integer birthYear,
        @Size(max = 30, message = "지역 코드는 30자 이하로 입력하세요.")
        String regionCode,
        @Size(max = 20, message = "우편번호는 20자 이하로 입력하세요.")
        String postalCode,
        @Size(max = 500, message = "도로명주소는 500자 이하로 입력하세요.")
        String roadAddress,
        @Size(max = 500, message = "지번주소는 500자 이하로 입력하세요.")
        String jibunAddress,
        @Size(max = 300, message = "상세주소는 300자 이하로 입력하세요.")
        String detailAddress,
        @Size(max = 100, message = "시도명은 100자 이하로 입력하세요.")
        String sidoName,
        @Size(max = 100, message = "시군구명은 100자 이하로 입력하세요.")
        String sigunguName,
        @Size(max = 100, message = "읍면동명은 100자 이하로 입력하세요.")
        String eupmyeondongName,
        @Size(max = 30, message = "법정동 코드는 30자 이하로 입력하세요.")
        String legalDongCode,
        @Size(max = 30, message = "도로명 코드는 30자 이하로 입력하세요.")
        String roadNameCode,
        @Size(max = 50, message = "건물관리번호는 50자 이하로 입력하세요.")
        String buildingManagementNo,
        @Size(max = 30, message = "주소 입력 출처 코드는 30자 이하로 입력하세요.")
        String addressSourceCode,
        Boolean hasIncome,
        @Size(max = 30, message = "소득 여부 코드는 30자 이하로 입력하세요.")
        String incomePresenceCode,
        @DecimalMin(value = "0", message = "소득 금액은 0 이상으로 입력하세요.")
        BigDecimal incomeAmount,
        @Size(max = 50, message = "건강보험 자격 코드는 50자 이하로 입력하세요.")
        String healthInsuranceBasisCode,
        @Valid
        BusinessInfoRequest business,
        @Valid
        List<FamilyInfoRequest> families,
        @Valid
        List<InterviewResponseRequest> interviewResponses,
        @Valid
        List<DocumentInputSaveRequest> documentInputs
) {

    public record BusinessInfoRequest(
            @Size(max = 100, message = "대표자명은 100자 이하로 입력하세요.")
            String representativeName,
            @Size(max = 30, message = "사업자등록번호는 30자 이하로 입력하세요.")
            String businessRegistrationNo,
            @Size(max = 200, message = "상호명은 200자 이하로 입력하세요.")
            String businessName,
            @Size(max = 30, message = "사업장 지역 코드는 30자 이하로 입력하세요.")
            String workplaceRegionCode,
            @Size(max = 20, message = "사업장 우편번호는 20자 이하로 입력하세요.")
            String workplacePostalCode,
            @Size(max = 500, message = "사업장 도로명주소는 500자 이하로 입력하세요.")
            String workplaceRoadAddress,
            @Size(max = 500, message = "사업장 지번주소는 500자 이하로 입력하세요.")
            String workplaceJibunAddress,
            @Size(max = 300, message = "사업장 상세주소는 300자 이하로 입력하세요.")
            String workplaceDetailAddress,
            @Size(max = 100, message = "사업장 시도명은 100자 이하로 입력하세요.")
            String workplaceSidoName,
            @Size(max = 100, message = "사업장 시군구명은 100자 이하로 입력하세요.")
            String workplaceSigunguName,
            @Size(max = 100, message = "사업장 읍면동명은 100자 이하로 입력하세요.")
            String workplaceEupmyeondongName,
            @Size(max = 30, message = "사업장 법정동 코드는 30자 이하로 입력하세요.")
            String workplaceLegalDongCode,
            @Size(max = 30, message = "사업장 도로명 코드는 30자 이하로 입력하세요.")
            String workplaceRoadNameCode,
            @Size(max = 50, message = "사업장 건물관리번호는 50자 이하로 입력하세요.")
            String workplaceBuildingManagementNo,
            @Size(max = 30, message = "사업장 주소 입력 출처 코드는 30자 이하로 입력하세요.")
            String workplaceAddressSourceCode,
            LocalDate openingDate,
            @Size(max = 30, message = "업종 코드는 30자 이하로 입력하세요.")
            String ksicCode,
            @Size(max = 50, message = "사업자 유형 코드는 50자 이하로 입력하세요.")
            String businessTypeCode,
            @Size(max = 50, message = "사업 상태 코드는 50자 이하로 입력하세요.")
            String companyStageCode,
            @DecimalMin(value = "0", message = "연매출은 0 이상으로 입력하세요.")
            BigDecimal annualRevenue,
            Integer annualRevenueYear,
            @Min(value = 0, message = "직원 수는 0 이상으로 입력하세요.")
            Integer employeeCount,
            @Min(value = 0, message = "상시근로자 수는 0 이상으로 입력하세요.")
            Integer regularEmployeeCount,
            @Min(value = 0, message = "신규 채용 예정 인원은 0 이상으로 입력하세요.")
            Integer plannedHireCount,
            @Min(value = 0, message = "NICE 신용 점수는 0 이상으로 입력하세요.")
            @Max(value = 1000, message = "NICE 신용 점수는 1000 이하로 입력하세요.")
            Integer niceCreditScore,
            @Min(value = 0, message = "KCB 신용 점수는 0 이상으로 입력하세요.")
            @Max(value = 1000, message = "KCB 신용 점수는 1000 이하로 입력하세요.")
            Integer kcbCreditScore,
            Boolean hasExistingLoan,
            Boolean hasPolicyFundUsage,
            Boolean hasGuaranteeUsage
    ) {
    }

    public record FamilyInfoRequest(
            @NotBlank(message = "가족 관계를 선택하세요.")
            @Size(max = 30, message = "가족 관계 코드는 30자 이하로 입력하세요.")
            String relationTypeCode,
            Integer birthYear,
            @Size(max = 50, message = "학령 상태 코드는 50자 이하로 입력하세요.")
            String schoolAgeStatusCode,
            @Size(max = 50, message = "재학 상태 코드는 50자 이하로 입력하세요.")
            String enrollmentStatusCode,
            Boolean cohabiting,
            Boolean supported,
            Boolean hasIncome,
            @Size(max = 30, message = "가족 소득 여부 코드는 30자 이하로 입력하세요.")
            String incomePresenceCode,
            @DecimalMin(value = "0", message = "가족 소득 금액은 0 이상으로 입력하세요.")
            BigDecimal incomeAmount
    ) {
    }

    public record InterviewResponseRequest(
            @NotBlank(message = "확인 질문 코드를 선택하세요.")
            @Size(max = 64, message = "확인 질문 코드는 64자 이하로 입력하세요.")
            String questionCode,
            @NotBlank(message = "확인 질문 답변을 선택하세요.")
            @Size(max = 16, message = "확인 질문 답변은 16자 이하로 입력하세요.")
            String answerCode,
            @Size(max = 1000, message = "기타 제한 메모는 1000자 이하로 입력하세요.")
            String note
    ) {
    }

    public record DocumentInputSaveRequest(
            @Size(max = 80, message = "서류 구분 코드는 80자 이하로 입력하세요.")
            String documentTypeCode,
            @Valid
            List<DocumentFieldValueRequest> fields
    ) {
    }

    public record DocumentFieldValueRequest(
            UUID standardFieldId,
            @Size(max = 2000, message = "서류 입력값은 2000자 이하로 입력하세요.")
            String valueText,
            @DecimalMin(value = "0", message = "서류 숫자값은 0 이상으로 입력하세요.")
            BigDecimal valueNumber,
            LocalDate valueDate,
            Boolean valueBoolean
    ) {
    }
}
