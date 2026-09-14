package com.saneb.domain.announcementattachment.service.impl;

import com.saneb.common.error.ApiException;
import com.saneb.common.error.ErrorCode;
import com.saneb.common.response.PageResponse;
import com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentCurrentDao;
import com.saneb.domain.announcementattachment.dto.AttachmentSourceResponses;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentCurrentService;
import com.saneb.domain.announcementattachment.service.AttachmentProcessingFlow;
import com.saneb.domain.announcementattachment.vo.AttachmentCurrentSourceRow;
import com.saneb.domain.announcementattachment.vo.AttachmentSourceSearchCondition;
import com.saneb.domain.announcementsource.dao.AnnouncementSourceDao;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ, timeout = 10)
public class AnnouncementAttachmentCurrentServiceImpl implements AnnouncementAttachmentCurrentService {
    private static final Set<String> PROVIDERS = Set.of("BIZINFO", "GOV24_PUBLIC_SERVICE", "LOCAL_GOV_NOTICE");
    private static final Set<String> STATUSES = Set.of("ACCEPTED", "REVIEW_REQUIRED");
    private static final Set<String> JOB_STATUSES = Set.of("NOT_REQUESTED", "PENDING", "RUNNING", "RETRY_WAIT",
            "SUCCEEDED", "PARTIAL_FAILED", "FAILED", "CANCELLED", "CONFLICT");
    private final AnnouncementAttachmentCurrentDao dao;
    private final AnnouncementSourceDao sources;

    public AnnouncementAttachmentCurrentServiceImpl(AnnouncementAttachmentCurrentDao dao, AnnouncementSourceDao sources) {
        this.dao = dao;
        this.sources = sources;
    }
    @Override public PageResponse<AttachmentSourceResponses.Summary> selectSourceList(AttachmentSourceSearchCondition request) {
        var condition = selectValidatedCondition(request);
        return PageResponse.of(dao.selectSourceList(condition).stream().map(this::selectSummary).toList(),
                condition.page(), condition.size(), dao.selectSourceCount(condition));
    }
    @Override public AttachmentSourceResponses.Details selectSourceDetails(UUID sourceId) {
        var current = selectRequiredSource(sourceId);
        var content = sources.selectSourceDetails(sourceId);
        if (content == null) throw notFound();
        return new AttachmentSourceResponses.Details(selectSummary(current), new AttachmentSourceResponses.Content(
                content.sourceUrl(), content.bodyText(), content.inquiryText(), content.applicationMethodText(), content.sourceCompletenessCode()));
    }
    @Override public AttachmentSourceResponses.Summary selectClassificationDetails(UUID sourceId) {
        return selectSummary(selectRequiredSource(sourceId));
    }
    private AttachmentCurrentSourceRow selectRequiredSource(UUID sourceId) {
        if (sourceId == null) throw notFound();
        var row = dao.selectSourceDetails(sourceId);
        if (row == null) throw notFound();
        return row;
    }
    private AttachmentSourceResponses.Summary selectSummary(AttachmentCurrentSourceRow row) {
        var base = new AttachmentSourceResponses.Classification(row.baseDecisionId(), row.baseStatus(), row.baseReason(),
                row.ruleReleaseId(), null, null, null, selectCodes(row.baseTargetCodes()), selectCodes(row.baseSupportCodes()));
        var attachment = row.attachmentDecisionId() == null ? null : new AttachmentSourceResponses.Classification(
                row.attachmentDecisionId(), row.attachmentStatus(), row.attachmentReason(), row.ruleReleaseId(), row.setId(),
                row.setHash(), row.inputHash(), selectCodes(row.attachmentTargetCodes()), selectCodes(row.attachmentSupportCodes()));
        boolean required = Boolean.TRUE.equals(row.reviewRequired());
        // pending는 판정 이력이 아니다. 과거 ID/태그를 복사하지 않으며 base와 별도로 검수 대기를 표현한다.
        var effective = !required ? base : attachment != null ? attachment : new AttachmentSourceResponses.Classification(
                null, row.effectiveStatus(), row.effectiveReason(), row.ruleReleaseId(), null, null, null, List.of(), List.of());
        var state = new AttachmentSourceResponses.AttachmentState(row.jobId(), row.jobStatus(), row.jobError(), row.intakeStatus(),
                Boolean.TRUE.equals(row.attachmentStale()), row.discoveryStatus(), row.discoveryComplete(), row.discoveredCount(), row.processedCount());
        return new AttachmentSourceResponses.Summary(row.sourceId(), row.publicCode(), row.providerCode(), row.title(), row.agencyName(),
                row.applicationEndDate(), row.collectedAt(), row.reviewStatusCode(), base, effective, required ? null : attachment,
                state, required, row.policyId(), row.sourceVersion(), row.attachmentVersion(), row.confirmationId(), row.confirmationStatus(),
                AttachmentProcessingFlow.selectFlowDetails(row));
    }
    private List<String> selectCodes(String csv) { return csv == null || csv.isBlank() ? List.of() : Arrays.asList(csv.split(",")); }
    private AttachmentSourceSearchCondition selectValidatedCondition(AttachmentSourceSearchCondition r) {
        if (r == null || r.page() < 1 || r.page() > 1_000_000 || r.size() < 1 || r.size() > 100)
            throw invalid("페이지(page)는 1~1000000, 페이지 크기(size)는 1~100이어야 합니다.");
        String provider=selectOptional(r.providerCode()), status=selectOptional(r.effectiveStatusCode()), job=selectOptional(r.jobStatusCode());
        if (provider != null && !PROVIDERS.contains(provider)) throw invalid("providerCode는 BIZINFO, GOV24_PUBLIC_SERVICE, LOCAL_GOV_NOTICE 중 하나여야 합니다.");
        if (status != null && !STATUSES.contains(status)) throw invalid("effectiveStatusCode는 후보 ACCEPTED 또는 검수 REVIEW_REQUIRED여야 합니다. 제목 제외 원문은 조회하지 않습니다.");
        if (job != null && !JOB_STATUSES.contains(job)) throw invalid("jobStatusCode는 NOT_REQUESTED, PENDING, RUNNING, RETRY_WAIT, SUCCEEDED, PARTIAL_FAILED, FAILED, CANCELLED, CONFLICT 중 하나여야 합니다.");
        String target=selectCategory(r.targetCategoryCode(),"targetCategoryCode"), support=selectCategory(r.supportTypeCode(),"supportTypeCode");
        String keyword=selectOptional(r.keyword());
        String flow=selectOptional(r.processingFlowStatusCode());
        if (flow != null && !AttachmentProcessingFlow.STATUS_CODES.contains(flow))
            throw invalid("processingFlowStatusCode는 NOT_APPLIED, CLASSIFICATION_PENDING, CONFIGURATION_REQUIRED, AUTOMATIC_PROCESSING, EVIDENCE_STALE, TECHNICAL_EXCEPTION, READY_FOR_FINAL_REVIEW, FINAL_REVIEW_EXCEPTION, FINAL_REVIEW_CONFIRMED 중 하나여야 합니다.");
        if (keyword != null && (keyword.length()>100 || keyword.chars().anyMatch(Character::isISOControl)))
            throw invalid("검색어(keyword)는 제어문자 없이 100자 이하여야 합니다.");
        if (r.collectedFrom()!=null && r.collectedTo()!=null && r.collectedFrom().isAfter(r.collectedTo()))
            throw invalid("수집 시작일(collectedFrom)은 종료일(collectedTo)보다 늦을 수 없습니다.");
        return new AttachmentSourceSearchCondition(provider,status,job,target,support,keyword,r.collectedFrom(),r.collectedTo(),r.page(),r.size(),flow);
    }
    private String selectCategory(String input,String field) {
        String value=selectOptional(input);
        if (value != null && !value.matches("[A-Z][A-Z0-9_]{0,59}")) throw invalid(field+"는 60자 이내의 대문자·숫자·밑줄 카테고리 코드여야 합니다.");
        return value;
    }
    private String selectOptional(String value) { return value == null || value.isBlank() ? null : value.strip(); }
    private ApiException notFound() { return new ApiException(ErrorCode.RESOURCE_NOT_FOUND,HttpStatus.NOT_FOUND,"운영 목록에서 조회할 수 있는 원문을 찾을 수 없습니다. 제목 제외·QA 대상은 이 경로에서 제공하지 않습니다."); }
    private ApiException invalid(String message) { return new ApiException(ErrorCode.VALIDATION_FAILED,HttpStatus.BAD_REQUEST,message); }
}
