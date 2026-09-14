package com.saneb.domain.announcementattachment.service;

import com.saneb.domain.announcementattachment.qa.AttachmentProviderQaCase;
import java.util.UUID;

/** 시스템 catalog/orchestrator 내부 계약. 사용자 제공 URL/성공 결과를 실행하는 API가 아니다. */
public interface AnnouncementAttachmentProviderQaExecutionService {
    Outcome saveCase(UUID caseId, AttachmentProviderQaCase fixedInput);
    record Outcome(UUID caseId, String statusCode) { }
}
