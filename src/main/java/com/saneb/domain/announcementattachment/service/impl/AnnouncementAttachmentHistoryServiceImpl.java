package com.saneb.domain.announcementattachment.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.common.error.ApiException;
import com.saneb.common.error.ErrorCode;
import com.saneb.common.response.PageResponse;
import com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentCurrentDao;
import com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentEvaluationDao;
import com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentHistoryDao;
import com.saneb.domain.announcementattachment.dto.AttachmentHistoryResponses;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentHistoryService;
import com.saneb.domain.announcementattachment.vo.AttachmentCurrentSourceRow;
import com.saneb.domain.announcementattachment.vo.AttachmentEvaluationRows;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ,timeout=10)
public class AnnouncementAttachmentHistoryServiceImpl implements AnnouncementAttachmentHistoryService {
    private final AnnouncementAttachmentCurrentDao current;
    private final AnnouncementAttachmentEvaluationDao evaluations;
    private final AnnouncementAttachmentHistoryDao history;
    private final ObjectMapper mapper;
    public AnnouncementAttachmentHistoryServiceImpl(AnnouncementAttachmentCurrentDao current,
            AnnouncementAttachmentEvaluationDao evaluations,AnnouncementAttachmentHistoryDao history,ObjectMapper mapper) {
        this.current=current;this.evaluations=evaluations;this.history=history;this.mapper=mapper;
    }
    @Override public PageResponse<AttachmentHistoryResponses.Summary> selectEvaluationList(UUID sourceId,int page,int size) {
        validatePage(page,size);var source=selectSource(sourceId);
        return PageResponse.of(history.selectEvaluationList(sourceId,(page-1)*size,size).stream()
                .map(row->selectSummary(row,source)).toList(),page,size,history.selectEvaluationCount(sourceId));
    }
    @Override public AttachmentHistoryResponses.Details selectEvaluationDetails(UUID sourceId,UUID evaluationId) {
        var source=selectSource(sourceId);var evaluation=selectEvaluation(sourceId,evaluationId);
        return new AttachmentHistoryResponses.Details(selectSummary(evaluation,source),
                List.copyOf(history.selectAutoTargetList(sourceId,evaluationId)),List.copyOf(history.selectAutoSupportList(sourceId,evaluationId)),
                history.selectInputCount(sourceId,evaluationId),history.selectMatchCount(sourceId,evaluationId,null),
                source.sourceVersion(),source.attachmentVersion());
    }
    @Override public PageResponse<AttachmentHistoryResponses.Input> selectInputList(UUID sourceId,UUID evaluationId,int page,int size) {
        validatePage(page,size);selectSource(sourceId);selectEvaluation(sourceId,evaluationId);
        return PageResponse.of(history.selectInputList(sourceId,evaluationId,(page-1)*size,size).stream()
                .map(AttachmentHistoryResponses.Input::from).toList(),page,size,history.selectInputCount(sourceId,evaluationId));
    }
    @Override public PageResponse<AttachmentHistoryResponses.Match> selectMatchList(UUID sourceId,UUID evaluationId,UUID fileId,int page,int size) {
        validatePage(page,size);selectSource(sourceId);selectEvaluation(sourceId,evaluationId);
        if(fileId!=null && !history.selectInputExists(sourceId,evaluationId,fileId)) throw notFound();
        return PageResponse.of(history.selectMatchList(sourceId,evaluationId,fileId,(page-1)*size,size).stream()
                .map(AttachmentHistoryResponses.Match::from).toList(),page,size,history.selectMatchCount(sourceId,evaluationId,fileId));
    }
    private AttachmentCurrentSourceRow selectSource(UUID sourceId) {
        if(sourceId==null) throw notFound();
        var source=current.selectSourceDetails(sourceId);
        if(source==null || !sourceId.equals(source.sourceId())) throw notFound();
        return source;
    }
    private AttachmentEvaluationRows.Evaluation selectEvaluation(UUID sourceId,UUID evaluationId) {
        if(evaluationId==null) throw notFound();
        var row=evaluations.selectEvaluationDetails(sourceId,evaluationId);
        if(row==null || !sourceId.equals(row.sourceId())) throw notFound();
        return row;
    }
    private AttachmentHistoryResponses.Summary selectSummary(AttachmentEvaluationRows.Evaluation row,AttachmentCurrentSourceRow source) {
        // DB current flag만으로 미리보기를 effective 판정으로 올리지 않는다. 동일 조회 snapshot의 현재 projection을 따른다.
        String usage=Boolean.TRUE.equals(row.current()) && row.evaluationId().equals(source.attachmentDecisionId())
                ? Boolean.TRUE.equals(source.reviewRequired()) ? "CURRENT_EFFECTIVE" : "CURRENT_PREVIEW" : "NOT_CURRENT";
        return new AttachmentHistoryResponses.Summary(row.evaluationId(),row.sourceId(),row.baseEvaluationId(),row.setId(),row.policyId(),
                row.ruleReleaseId(),row.engineVersion(),row.inputHash(),row.decisionHash(),row.status(),row.reason(),
                selectWarnings(row.warningCodesJson()),usage,row.evaluatedAt());
    }
    private List<String> selectWarnings(String json) {
        try {
            List<String> codes=mapper.readValue(json,new TypeReference<>() { });
            if(codes==null || codes.size()>100 || codes.stream().anyMatch(c->c==null || !c.matches("[A-Z0-9_]{1,80}")))
                throw new IllegalStateException("저장된 첨부 판정 경고 코드 형식이 올바르지 않습니다.");
            return List.copyOf(codes);
        } catch(JsonProcessingException|IllegalArgumentException exception) {
            throw new IllegalStateException("저장된 첨부 판정 경고 코드를 읽을 수 없습니다.");
        }
    }
    private void validatePage(int page,int size) {
        if(page<1 || page>1_000_000 || size<1 || size>100) throw new ApiException(ErrorCode.VALIDATION_FAILED,HttpStatus.BAD_REQUEST,
                "페이지(page)는 1~1000000, 페이지 크기(size)는 1~100이어야 합니다.");
    }
    private ApiException notFound() { return new ApiException(ErrorCode.RESOURCE_NOT_FOUND,HttpStatus.NOT_FOUND,
            "조회할 수 있는 원문과 그 원문에 속한 첨부 판정·파일을 선택하세요. 제목 제외 원문은 조회할 수 없습니다."); }
}
