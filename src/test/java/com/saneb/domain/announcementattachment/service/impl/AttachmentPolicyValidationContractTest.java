package com.saneb.domain.announcementattachment.service.impl;

import static org.assertj.core.api.Assertions.*;
import org.junit.jupiter.api.Test;

class AttachmentPolicyValidationContractTest {
    @Test void legacyKeepsStrictMeaningEvenForHistoricalCollectionMode() {
        assertThat(AttachmentPolicyValidationContract.selectSnapshot("{\"modeCode\":\"COLLECT_ONLY\"}"))
                .isEqualTo(AttachmentPolicyValidationContract.STRICT_V1);
        assertThat(AttachmentPolicyValidationContract.STRICT_V1.selectSteps()).contains("PROVIDER_PROFILES").doesNotContain("COLLECTION_SAFETY");
    }
    @Test void newCollectionHasDistinctStatusAndScope() {
        var c=AttachmentPolicyValidationContract.selectForMode("COLLECT_ONLY");
        assertThat(c.selectSuccessStatus()).isEqualTo("COLLECTION_VERIFIED");
        assertThat(c.selectSteps()).hasSize(4).contains("COLLECTION_SAFETY","INSTALLED_RUNTIME","WORKER_DB_RECOVERY");
        assertThat(AttachmentPolicyValidationContract.selectForMode("ENFORCE").selectSuccessStatus()).isEqualTo("VERIFIED");
    }
    @Test void unknownNullDuplicateAndWrongModeContractAreRejected() {
        for(String json:java.util.List.of("{\"validationContractCode\":null}","{\"validationContractCode\":\"FUTURE\"}",
                "{\"validationContractCode\":\"COLLECTION_SAFETY_V1\",\"modeCode\":\"ENFORCE\"}",
                "{\"validationContractCode\":\"STRICT_V1\",\"validationContractCode\":\"STRICT_V1\"}"))
            assertThatThrownBy(()->AttachmentPolicyValidationContract.selectSnapshot(json)).isInstanceOf(IllegalArgumentException.class);
    }
}
