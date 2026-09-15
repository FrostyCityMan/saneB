package com.saneb.domain.announcementattachment.extraction;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class HwpStructureDiagnosticTest {
    private final ObjectMapper json=new ObjectMapper();

    @Test void onlyNumericSummaryLeavesTheExtractionAndCopyCannotMutateOriginal() throws Exception {
        var result=valid();result.put("text","private fixture text");result.put("filename","private fixture name");
        var summary=IsolatedAttachmentExtractor.selectHwpStructureDetails(result);
        assertEquals(4,summary.size());assertFalse(summary.toString().contains("private fixture"));
        ((ObjectNode)summary).put("recordCount",99);
        assertEquals(2,result.path("hwpStructure").path("recordCount").asInt());
        assertNull(IsolatedAttachmentExtractor.selectHwpStructureDetails(json.createObjectNode()));
        assertNull(IsolatedAttachmentExtractor.selectHwpStructureDetails(null));
    }

    @Test void rejectsUnknownFieldsAndStringPayloadWithoutReflectingTheirContents() throws Exception {
        var extra=valid();((ObjectNode)extra.path("hwpStructure")).put("raw","private fixture");assertInvalid(extra);
        var nested=valid();((ObjectNode)nested.path("hwpStructure").path("recordTypes").get(0)).put("raw","private fixture");assertInvalid(nested);
        var text=valid();((ObjectNode)text.path("hwpStructure")).put("recordCount","private fixture");assertInvalid(text);
        var other=valid().put("format","PDF");assertInvalid(other);
        var absent=valid();absent.putNull("hwpStructure");assertInvalid(absent);
    }

    @Test void rejectsInconsistentUnsortedDuplicateAndOutOfRangeRecords() throws Exception {
        for(String types:new String[]{"[{\"tagId\":67,\"count\":1}]",
                "[{\"tagId\":77,\"count\":1},{\"tagId\":67,\"count\":1}]",
                "[{\"tagId\":67,\"count\":1},{\"tagId\":67,\"count\":1}]",
                "[{\"tagId\":1024,\"count\":2}]","[{\"tagId\":-1,\"count\":2}]",
                "[{\"tagId\":67,\"count\":0}]","[{\"tagId\":67,\"count\":-1}]",
                "[{\"tagId\":67,\"count\":2147483648}]","[{\"tagId\":67,\"count\":2.0}]"}) {
            var input=valid();((ObjectNode)input.path("hwpStructure")).set("recordTypes",json.readTree(types));assertInvalid(input);
        }
        for(String field:new String[]{"sectionCount","recordCount","maximumLevel"}) {
            var negative=valid();((ObjectNode)negative.path("hwpStructure")).put(field,-1);assertInvalid(negative);
            var overflow=valid();((ObjectNode)overflow.path("hwpStructure")).put(field,Long.MAX_VALUE);assertInvalid(overflow);
        }
    }

    @Test void all1024TagsAreBoundedAndZeroRecordsRetainExplicitEmptyMeaning() throws Exception {
        var input=valid();var summary=(ObjectNode)input.path("hwpStructure");var types=json.createArrayNode();
        for(int tag=0;tag<1024;tag++)types.addObject().put("tagId",tag).put("count",1);
        summary.put("recordCount",1024).put("maximumLevel",1023).set("recordTypes",types);
        assertEquals(1024,IsolatedAttachmentExtractor.selectHwpStructureDetails(input).path("recordTypes").size());
        types.addObject().put("tagId",0).put("count",1);summary.put("recordCount",1025);assertInvalid(input);
        summary.put("recordCount",0).put("maximumLevel",0).set("recordTypes",json.createArrayNode());
        assertTrue(IsolatedAttachmentExtractor.selectHwpStructureDetails(input).path("recordTypes").isEmpty());
        summary.put("maximumLevel",1);assertInvalid(input);
    }

    private ObjectNode valid()throws Exception {
        return (ObjectNode)json.readTree("""
                {"format":"HWP","hwpStructure":{"sectionCount":1,"recordCount":2,"maximumLevel":2,
                "recordTypes":[{"tagId":67,"count":1},{"tagId":77,"count":1}]}}
                """);
    }
    private void assertInvalid(JsonNode value) {
        var error=assertThrows(IOException.class,()->IsolatedAttachmentExtractor.selectHwpStructureDetails(value));
        assertEquals("INVALID_HWP_STRUCTURE_DIAGNOSTIC",error.getMessage());
    }
}
