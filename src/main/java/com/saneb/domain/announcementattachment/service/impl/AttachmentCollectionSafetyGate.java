package com.saneb.domain.announcementattachment.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.domain.announcementattachment.vo.AttachmentPolicyValidationRows.Run;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Component;

/** 시스템 결합 증거다. 외부 다운로드·추출 성공 또는 분류 정답 완료 증거가 아니다. */
@Component
public final class AttachmentCollectionSafetyGate implements AttachmentPolicyAdditionalQaEvidenceVerifier {
    private final AttachmentPolicyValidationSnapshotFactory snapshots;
    private final ObjectMapper mapper;
    public AttachmentCollectionSafetyGate(AttachmentPolicyValidationSnapshotFactory snapshots, ObjectMapper mapper) {
        this.snapshots = snapshots; this.mapper = mapper;
    }
    @Override public String selectStepCode() { return "COLLECTION_SAFETY"; }
    public JsonNode selectEvidence(AttachmentPolicyValidationSnapshotFactory.Frozen frozen, Run run) {
        try {
            if (AttachmentPolicyValidationContract.selectSnapshot(frozen.json()) != AttachmentPolicyValidationContract.COLLECTION_SAFETY_V1
                    || !frozen.hash().equals(run.snapshotHash())) throw new IllegalArgumentException();
            var current = snapshots.selectProviderQaPlan();
            if (!mapper.valueToTree(current).equals(mapper.readTree(frozen.json()).path("providerQaPlan")))
                throw new IllegalArgumentException();
            var counts = new TreeMap<String, Long>();
            current.items().forEach(item -> counts.merge(item.statusCode(), 1L, Long::sum));
            if (counts.getOrDefault("SYSTEM_BINDING_MATCHED", 0L) < 1) throw new IllegalArgumentException();
            return mapper.valueToTree(Map.of("schemaVersion", 1, "validationContractCode", "COLLECTION_SAFETY_V1",
                    "policyRunId", run.runId().toString(), "snapshotHash", frozen.hash(),
                    "planHash", snapshots.hash(current), "targetCount", current.items().size(), "bindingStatusCounts", counts,
                    "externalDownloadProven", false, "classificationEnforceProven", false));
        } catch (Exception exception) {
            throw new IllegalArgumentException("수집 전용 QA의 전체 대상·프로필 결합이 변경됐거나 실행 가능한 결합이 없습니다.");
        }
    }
    @Override public String selectValidatedEvidenceHash(JsonNode evidence, AttachmentPolicyValidationSnapshotFactory.Frozen frozen, Run run) {
        if (!selectEvidence(frozen, run).equals(evidence)) throw new IllegalArgumentException("수집 안전성 증거가 현재 입력과 다릅니다.");
        return snapshots.hash(mapper.convertValue(evidence, Object.class));
    }
    @Override public void validateCurrentEvidence(JsonNode evidence, AttachmentPolicyValidationSnapshotFactory.Frozen frozen, Run run) {
        selectValidatedEvidenceHash(evidence, frozen, run);
    }
}
