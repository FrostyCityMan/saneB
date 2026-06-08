package com.saneb.domain.candidatepreview.service;

import com.saneb.domain.candidatepreview.dto.CandidatePreviewRequest;
import com.saneb.domain.candidatepreview.dto.CandidatePreviewResponse;

public interface CandidatePreviewService {

    CandidatePreviewResponse selectCandidatePreview(CandidatePreviewRequest request);
}
