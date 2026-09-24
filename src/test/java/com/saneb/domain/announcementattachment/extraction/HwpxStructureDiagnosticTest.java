package com.saneb.domain.announcementattachment.extraction;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

class HwpxStructureDiagnosticTest {
    private final ObjectMapper json = new ObjectMapper();
    @Test void optionalNumericSummaryIsCopiedAndNeverCopiesTextOrAttributes() throws Exception {
        var input=valid(); input.put("filename","PRIVATE_NAME");
        var result=(ObjectNode)IsolatedAttachmentExtractor.selectHwpxStructureDetails(input);
        assertEquals(6,result.size()); assertFalse(result.toString().contains("PRIVATE"));
        result.put("pictureCount",99); assertEquals(1,input.path("hwpxStructure").path("pictureCount").intValue());
        assertNull(IsolatedAttachmentExtractor.selectHwpxStructureDetails(json.createObjectNode()));
        assertNull(IsolatedAttachmentExtractor.selectHwpxStructureDetails(null));
    }
    @Test void invalidFieldsTypesAndBoundsAreRejectedWithoutEchoingPayload() throws Exception {
        var extra=valid(); counts(extra).put("raw","PRIVATE_CANARY"); invalid(extra);
        var absent=valid(); absent.putNull("hwpxStructure"); invalid(absent);
        var other=valid(); other.put("format","PDF"); invalid(other);
        for (String field : List.of("sectionCount","paragraphCount","pictureCount","oleCount","equationCount","replacementCharacterCount")) {
            for (JsonNode value : List.<JsonNode>of(json.valueToTree(-1),json.valueToTree(Long.MAX_VALUE),json.valueToTree(1.5),
                    json.valueToTree("PRIVATE_CANARY"),json.valueToTree(true))) {
                var input=valid(); counts(input).set(field,value); invalid(input);
            }
            var input=valid(); counts(input).remove(field); invalid(input);
        }
        var sections=valid(); counts(sections).put("sectionCount",2001); invalid(sections);
        var paragraphs=valid(); counts(paragraphs).put("paragraphCount",0); invalid(paragraphs);
        var controls=valid(); counts(controls).put("oleCount",33_554_433); invalid(controls);
    }
    @Test void qualityAndReplacementCountMustAgreeWithActualText() throws Exception {
        var promoted=valid().put("qualityCode","COMPLETE_TEXT"); invalid(promoted);
        var noCause=valid(); counts(noCause).put("pictureCount",0); invalid(noCause);
        noCause.put("qualityCode","COMPLETE_TEXT"); assertNotNull(IsolatedAttachmentExtractor.selectHwpxStructureDetails(noCause));
        var replacement=valid().put("text","😀\ufffd"); counts(replacement).put("pictureCount",0).put("replacementCharacterCount",1);
        assertNotNull(IsolatedAttachmentExtractor.selectHwpxStructureDetails(replacement));
        counts(replacement).put("replacementCharacterCount",0); invalid(replacement);
        var falselyCounted=valid(); counts(falselyCounted).put("replacementCharacterCount",1); invalid(falselyCounted);
        var empty=valid().put("text","").put("qualityCode","OCR_REQUIRED");
        assertNotNull(IsolatedAttachmentExtractor.selectHwpxStructureDetails(empty));
        empty.put("qualityCode","PARTIAL_TEXT"); invalid(empty);
    }
    private ObjectNode valid() {
        var input=json.createObjectNode().put("format","HWPX").put("qualityCode","PARTIAL_TEXT").put("text","PRIVATE_TEXT");
        input.putObject("hwpxStructure").put("sectionCount",1).put("paragraphCount",2).put("pictureCount",1)
                .put("oleCount",0).put("equationCount",0).put("replacementCharacterCount",0);
        return input;
    }
    private ObjectNode counts(ObjectNode input) { return (ObjectNode)input.path("hwpxStructure"); }
    private void invalid(JsonNode input) {
        assertEquals("INVALID_HWPX_STRUCTURE_DIAGNOSTIC",assertThrows(IOException.class,
                ()->IsolatedAttachmentExtractor.selectHwpxStructureDetails(input)).getMessage());
    }
}
