package com.saneb.domain.announcementattachment.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;

/** 과거 VERIFIED의 의미를 유지하는 불변 snapshot 계약. */
public enum AttachmentPolicyValidationContract {
    STRICT_V1("VERIFIED", "PROVIDER_PROFILES"),
    COLLECTION_SAFETY_V1("COLLECTION_VERIFIED", "COLLECTION_SAFETY");

    private final String success;
    private final String providerStep;
    AttachmentPolicyValidationContract(String success, String providerStep) {
        this.success = success; this.providerStep = providerStep;
    }
    public String selectSuccessStatus() { return success; }
    public List<String> selectSteps() {
        return List.of("CLASSIFICATION_GOLDEN", "INSTALLED_RUNTIME", providerStep, "WORKER_DB_RECOVERY");
    }
    public static AttachmentPolicyValidationContract selectForMode(String mode) {
        return "COLLECT_ONLY".equals(mode) ? COLLECTION_SAFETY_V1 : STRICT_V1;
    }
    public static AttachmentPolicyValidationContract selectSnapshot(String json) {
        if (json == null) return STRICT_V1; // 과거 metadata projection과 이력 호환
        try {
            if (json.length() > 2097152) throw new IllegalArgumentException();
            var input = new ObjectMapper().enable(com.fasterxml.jackson.core.JsonParser.Feature.STRICT_DUPLICATE_DETECTION).readTree(json);
            var code = input.get("validationContractCode");
            if (code == null) return STRICT_V1;
            if (!code.isTextual()) throw new IllegalArgumentException();
            var contract = valueOf(code.textValue());
            if (contract == COLLECTION_SAFETY_V1 && !"COLLECT_ONLY".equals(input.path("modeCode").asText()))
                throw new IllegalArgumentException();
            return contract;
        } catch (Exception exception) {
            throw new IllegalArgumentException("정책 QA의 검증 계약 또는 적용 모드가 유효하지 않습니다.");
        }
    }
}
