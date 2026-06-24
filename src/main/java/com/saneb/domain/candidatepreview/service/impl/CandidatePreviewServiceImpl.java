/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: CandidatePreviewServiceImpl.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.candidatepreview.service.impl;

import com.saneb.domain.candidatepreview.dao.CandidatePreviewDao;
import com.saneb.domain.candidatepreview.dto.CandidatePreviewRequest;
import com.saneb.domain.candidatepreview.dto.CandidatePreviewRequest.FamilyPreviewRequest;
import com.saneb.domain.candidatepreview.dto.CandidatePreviewResponse;
import com.saneb.domain.candidatepreview.vo.CandidatePreviewRow;
import com.saneb.domain.candidatepreview.vo.CandidatePreviewSearchCondition;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class CandidatePreviewServiceImpl implements com.saneb.domain.candidatepreview.service.CandidatePreviewService {

    private static final String NOTICE = "회원가입 전 임시 확인 결과입니다. 실제 신청 가능 여부는 가입 후 공고별 입력값과 서버 검증 기준으로 확정됩니다.";

    private final CandidatePreviewDao candidatePreviewDao;

    /**
     * 객체를 생성합니다.
     *
     * @param candidatePreviewDao 입력 값
     */
    public CandidatePreviewServiceImpl(CandidatePreviewDao candidatePreviewDao) {
        this.candidatePreviewDao = candidatePreviewDao;
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param request 입력 값
     *
     * @return 처리 결과
     */
    @Override
    public CandidatePreviewResponse selectCandidatePreview(CandidatePreviewRequest request) {
        CandidatePreviewRow row = candidatePreviewDao.selectCandidatePreview(new CandidatePreviewSearchCondition(
                normalizeCode(request.regionCode()),
                request.annualRevenue(),
                businessYears(request.openingDate()),
                age(request.birthYear()),
                normalizeCode(request.ksicCode()),
                booleanCode(resolveFamilyPresence(request.hasSpouse(), request.families(), "SPOUSE")),
                booleanCode(resolveFamilyPresence(request.hasChild(), request.families(), "CHILD")),
                booleanCode(resolveFamilyPresence(request.hasParent(), request.families(), "PARENT"))
        ));
        return new CandidatePreviewResponse(
                row == null ? 0 : row.possibleCandidateCount(),
                row == null ? null : row.minSupportAmount(),
                row == null ? null : row.maxSupportAmount(),
                NOTICE
        );
    }

    /**
     * 입력 값을 표준 형식으로 정규화합니다.
     *
     * @param value 입력 값
     *
     * @return 처리 결과
     */
    private String normalizeCode(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param value 입력 값
     *
     * @return 처리 결과
     */
    private String booleanCode(Boolean value) {
        if (value == null) {
            return null;
        }
        return Boolean.TRUE.equals(value) ? "TRUE" : "FALSE";
    }

    /**
     * 업무 처리에 필요한 값을 해석합니다.
     *
     * @param explicitValue 입력 값
     *
     * @param families 입력 값
     *
     * @param relationTypeCode 입력 값
     *
     * @return 처리 결과
     */
    private Boolean resolveFamilyPresence(Boolean explicitValue, List<FamilyPreviewRequest> families, String relationTypeCode) {
        if (explicitValue != null) {
            return explicitValue;
        }
        if (families == null || families.isEmpty()) {
            return null;
        }
        return families.stream()
                .filter(family -> family != null && relationTypeCode.equals(normalizeCode(family.relationTypeCode())))
                .findAny()
                .map(ignored -> Boolean.TRUE)
                .orElse(null);
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param birthYear 입력 값
     *
     * @return 처리 결과
     */
    private BigDecimal age(Integer birthYear) {
        if (birthYear == null || birthYear < 1900 || birthYear > 2200) {
            return null;
        }
        return BigDecimal.valueOf(LocalDate.now().getYear() - birthYear);
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param openingDate 입력 값
     *
     * @return 처리 결과
     */
    private BigDecimal businessYears(LocalDate openingDate) {
        if (openingDate == null || openingDate.isAfter(LocalDate.now())) {
            return null;
        }
        long months = ChronoUnit.MONTHS.between(openingDate.withDayOfMonth(1), LocalDate.now().withDayOfMonth(1));
        return BigDecimal.valueOf(months)
                .divide(BigDecimal.valueOf(12), 2, RoundingMode.DOWN);
    }
}
