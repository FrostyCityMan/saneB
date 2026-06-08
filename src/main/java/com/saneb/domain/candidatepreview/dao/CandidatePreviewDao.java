package com.saneb.domain.candidatepreview.dao;

import com.saneb.domain.candidatepreview.vo.CandidatePreviewRow;
import com.saneb.domain.candidatepreview.vo.CandidatePreviewSearchCondition;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CandidatePreviewDao {

    CandidatePreviewRow selectCandidatePreview(CandidatePreviewSearchCondition condition);
}
