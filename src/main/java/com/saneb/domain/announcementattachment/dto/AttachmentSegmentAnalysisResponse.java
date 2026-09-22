package com.saneb.domain.announcementattachment.dto;

import com.saneb.domain.announcementattachment.classification.AttachmentSegmentRoleAnalyzer;
import java.time.OffsetDateTime;
import java.util.UUID;

/** SHADOW 분석 완료와 운영 분류 적용을 구분한다. 파일 역할은 자동 분석으로 덮어쓰지 않는다. */
public record AttachmentSegmentAnalysisResponse(UUID sourceId, UUID setId, UUID fileId, UUID extractionId,
        String fileRoleCode, String fileRoleOriginCode, String applicationMode, String analysisState,
        UUID analysisId, OffsetDateTime analyzedAt, AttachmentSegmentRoleAnalyzer.Analysis analysis) { }
