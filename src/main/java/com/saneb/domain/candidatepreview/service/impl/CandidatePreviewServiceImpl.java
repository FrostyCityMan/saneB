package com.saneb.domain.candidatepreview.service.impl;

import com.saneb.domain.candidatepreview.dao.CandidatePreviewDao;
import com.saneb.domain.candidatepreview.dto.CandidatePreviewRequest;
import com.saneb.domain.candidatepreview.dto.CandidatePreviewResponse;
import com.saneb.domain.candidatepreview.vo.CandidatePreviewRow;
import com.saneb.domain.candidatepreview.vo.CandidatePreviewSearchCondition;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
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
                booleanCode(request.hasSpouse()),
                booleanCode(request.hasChild())
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

    private BigDecimal businessYears(LocalDate openingDate) {
        if (openingDate == null || openingDate.isAfter(LocalDate.now())) {
            return null;
        }
        long months = ChronoUnit.MONTHS.between(openingDate.withDayOfMonth(1), LocalDate.now().withDayOfMonth(1));
        return BigDecimal.valueOf(months)
                .divide(BigDecimal.valueOf(12), 2, RoundingMode.DOWN);
    }
}
