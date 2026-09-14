package com.saneb.domain.announcementattachment.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.saneb.domain.announcementattachment.vo.AttachmentPolicyValidationRows.Run;

/** 실제 전체 출처/worker QA 실행기와 짝을 이루는 내부 검증 계약. 관리자 제출 JSON/성공 건수만으로 구현하지 않는다. */
public interface AttachmentPolicyAdditionalQaEvidenceVerifier {
    String selectStepCode();
    String selectValidatedEvidenceHash(JsonNode evidence,AttachmentPolicyValidationSnapshotFactory.Frozen snapshot,Run run);
    default String selectValidatedEvidenceHash(JsonNode evidence,AttachmentPolicyValidationSnapshotFactory.Frozen snapshot,Run run,java.time.OffsetDateTime recordedAt) {
        return selectValidatedEvidenceHash(evidence,snapshot,run);
    }
    /** 게시 잠금 안의 DB 현재성 확인만 수행한다. 파일·외부 요청·추출 프로세스를 실행하지 않는다. */
    default void validateCurrentEvidence(JsonNode evidence,AttachmentPolicyValidationSnapshotFactory.Frozen snapshot,Run run) { }
}
