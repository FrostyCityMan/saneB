package com.saneb.domain.announcementattachment.extraction;

import static org.assertj.core.api.Assertions.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** 격리 응답의 잘못된 근거가 파일 단위 실패 대신 전체 DB 저장 실패로 번지지 않아야 한다. */
class AttachmentExtractorEvidenceBoundaryTest {
    private final ObjectMapper mapper = new ObjectMapper();

    private ObjectNode selectResult() {
        var result = mapper.createObjectNode().put("format", "PDF").put("qualityCode", "COMPLETE_TEXT")
                .put("text", "가😀나다");
        var blocks = result.putArray("blocks");
        blocks.addObject().put("index", 0).put("startOffset", 0).put("endOffset", 2)
                .put("evidenceScopeId", "p:1").put("scopeReliable", true).put("locator", "p:1");
        blocks.addObject().put("index", 1).put("startOffset", 2).put("endOffset", 4)
                .put("evidenceScopeId", "p:2").put("scopeReliable", false).put("locator", "p:2");
        return result;
    }

    private void validate(JsonNode result) throws Exception {
        var method = IsolatedAttachmentExtractor.class.getDeclaredMethod("validateResult", JsonNode.class);
        method.setAccessible(true);
        try { method.invoke(new IsolatedAttachmentExtractor(mapper, "unused"), result); }
        catch (InvocationTargetException failure) { throw (Exception) failure.getCause(); }
    }

    @Test void validCodePointOffsetsAndUnreliableScopesRemainReadable() throws Exception {
        validate(selectResult());
        validate(selectResult().put("qualityCode", "PARTIAL_TEXT"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"missingIndex", "stringIndex", "duplicateIndex", "overflowIndex", "fractionOffset",
            "stringOffset", "overflowOffset", "missingScope", "blankScope", "numericScope", "duplicateScope",
            "longScope", "missingLocator", "blankLocator", "numericLocator", "longLocator", "nonObject"})
    void malformedEvidenceIsRejectedBeforeWorkerStorage(String variant) throws Exception {
        var result = selectResult();
        var block = (ObjectNode) result.path("blocks").get(1);
        switch (variant) {
            case "missingIndex" -> block.remove("index");
            case "stringIndex" -> block.put("index", "1");
            case "duplicateIndex" -> block.put("index", 0);
            case "overflowIndex" -> block.put("index", 4294967297L);
            case "fractionOffset" -> block.put("startOffset", 2.5);
            case "stringOffset" -> block.put("startOffset", "2");
            case "overflowOffset" -> block.put("endOffset", 4294967300L);
            case "missingScope" -> block.remove("evidenceScopeId");
            case "blankScope" -> block.put("evidenceScopeId", " ");
            case "numericScope" -> block.put("evidenceScopeId", 1);
            case "duplicateScope" -> block.put("evidenceScopeId", "p:1");
            case "longScope" -> block.put("evidenceScopeId", "a".repeat(301));
            case "missingLocator" -> block.remove("locator");
            case "blankLocator" -> block.put("locator", " ");
            case "numericLocator" -> block.put("locator", 1);
            case "longLocator" -> block.put("locator", "a".repeat(301));
            case "nonObject" -> result.withArray("/blocks").set(1, mapper.getNodeFactory().textNode("PRIVATE_CANARY"));
            default -> throw new AssertionError(variant);
        }
        assertThatThrownBy(() -> validate(result)).isInstanceOf(IOException.class).hasMessage("INVALID_EXTRACTOR_RESULT");
    }
}
