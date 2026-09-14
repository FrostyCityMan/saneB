package com.saneb.domain.announcementattachment.service.impl;

import com.saneb.common.error.ApiException;
import com.saneb.common.error.ErrorCode;
import com.saneb.common.response.PageResponse;
import com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentEvidenceDao;
import com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentJobDao;
import com.saneb.domain.announcementattachment.dto.AttachmentEvidenceResponses;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentReadService;
import java.util.UUID;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ, timeout = 10)
public class AnnouncementAttachmentReadServiceImpl implements AnnouncementAttachmentReadService {
    private final AnnouncementAttachmentJobDao jobs;
    private final AnnouncementAttachmentEvidenceDao evidence;
    public AnnouncementAttachmentReadServiceImpl(AnnouncementAttachmentJobDao jobs, AnnouncementAttachmentEvidenceDao evidence) {
        this.jobs = jobs;
        this.evidence = evidence;
    }
    @Override public PageResponse<AttachmentEvidenceResponses.SetSummary> selectAttachmentSetList(UUID sourceId, int page, int size) {
        validatePage(page, size, 100);
        validateSource(sourceId);
        return PageResponse.of(evidence.selectSetList(sourceId, (page - 1) * size, size).stream()
                .map(AttachmentEvidenceResponses.SetSummary::from).toList(), page, size, evidence.selectSetCount(sourceId));
    }
    @Override public PageResponse<AttachmentEvidenceResponses.FileSummary> selectAttachmentFileList(UUID sourceId, UUID setId, int page, int size) {
        validatePage(page, size, 100);
        validateSource(sourceId);
        if (evidence.selectSetDetails(sourceId, setId) == null) throw notFound();
        return PageResponse.of(evidence.selectFileList(sourceId, setId, (page - 1) * size, size).stream()
                .map(AttachmentEvidenceResponses.FileSummary::from).toList(), page, size, evidence.selectFileCount(sourceId, setId));
    }
    @Override public PageResponse<AttachmentEvidenceResponses.Block> selectAttachmentBlockList(UUID sourceId, UUID extractionId,
            int page, int size, int textOffset, int textLimit) {
        validatePage(page, size, 20);
        if (textOffset < 0 || textOffset > 1_000_000 || textLimit < 1 || textLimit > 4000)
            throw invalid("본문 위치(textOffset)는 0~1000000, 조회 길이(textLimit)는 1~4000자여야 합니다.");
        validateSource(sourceId);
        Long total = evidence.selectExtractionBlockCount(sourceId, extractionId);
        if (total == null) throw notFound();
        return PageResponse.of(evidence.selectBlockList(sourceId, extractionId, (page - 1) * size, size, textOffset, textLimit)
                .stream().map(AttachmentEvidenceResponses.Block::from).toList(), page, size, total);
    }
    private void validatePage(int page, int size, int max) {
        if (page < 1 || page > 1_000_000 || size < 1 || size > max)
            throw invalid("페이지(page)는 1~1000000, 페이지 크기(size)는 1~" + max + "이어야 합니다.");
    }
    private void validateSource(UUID sourceId) {
        if(sourceId==null) throw notFound();
        var source=jobs.selectSourceContextDetails(sourceId);
        if(source==null || !sourceId.equals(source.sourceId()) || !"PRODUCTION".equals(source.dataPurposeCode())
                || "EXCLUDED".equals(source.semanticStatusCode()) || source.baseEvaluationId()==null || source.titleStageCode()==null
                || !Set.of("GROUP_A_MATCHED","COMBINATION_MATCHED").contains(source.titleStageCode())) throw notFound();
    }
    private ApiException notFound() { return new ApiException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "이 원문에 속한 첨부 근거를 찾을 수 없습니다."); }
    private ApiException invalid(String message) { return new ApiException(ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST, message); }
}
