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

    public CandidatePreviewServiceImpl(CandidatePreviewDao candidatePreviewDao) {
        this.candidatePreviewDao = candidatePreviewDao;
    }

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

    private String normalizeCode(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String booleanCode(Boolean value) {
        if (value == null) {
            return null;
        }
        return Boolean.TRUE.equals(value) ? "TRUE" : "FALSE";
    }

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

    private BigDecimal age(Integer birthYear) {
        if (birthYear == null || birthYear < 1900 || birthYear > 2200) {
            return null;
        }
        return BigDecimal.valueOf(LocalDate.now().getYear() - birthYear);
    }

    private BigDecimal businessYears(LocalDate openingDate) {
        if (openingDate == null || openingDate.isAfter(LocalDate.now())) {
            return null;
        }
        long months = ChronoUnit.MONTHS.between(openingDate.withDayOfMonth(1), LocalDate.now().withDayOfMonth(1));
        return BigDecimal.valueOf(months)
                .divide(BigDecimal.valueOf(12), 2, RoundingMode.DOWN);
    }
}
