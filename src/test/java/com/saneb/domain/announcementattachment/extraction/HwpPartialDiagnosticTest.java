package com.saneb.domain.announcementattachment.extraction;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

class HwpPartialDiagnosticTest {
    private final ObjectMapper json=new ObjectMapper();
    private ObjectNode valid() {
        var input=json.createObjectNode().put("format","HWP").put("qualityCode","PARTIAL_TEXT");
        input.putArray("hwpPartialCauses").addObject().put("code","CELL_HEADER_INVALID").put("count",3);
        return input;
    }
    private void invalid(JsonNode input) {
        assertEquals("INVALID_HWP_PARTIAL_DIAGNOSTIC",assertThrows(IOException.class,
                ()->IsolatedAttachmentExtractor.selectHwpPartialCauseList(input)).getMessage());
    }
    @Test void boundedFixedCodesAreCopiedWithoutChangingLegacyOrQuality() throws Exception {
        var input=valid();var copy=IsolatedAttachmentExtractor.selectHwpPartialCauseList(input);
        assertEquals(3,copy.get(0).path("count").asInt());
        ((ObjectNode)copy.get(0)).put("count",4);assertEquals(3,input.at("/hwpPartialCauses/0/count").asInt());
        assertNull(IsolatedAttachmentExtractor.selectHwpPartialCauseList(null));
        assertNull(IsolatedAttachmentExtractor.selectHwpPartialCauseList(json.createObjectNode()));
        input.put("qualityCode","COMPLETE_TEXT").putArray("hwpPartialCauses");
        assertTrue(IsolatedAttachmentExtractor.selectHwpPartialCauseList(input).isEmpty());
        input.put("qualityCode","OCR_REQUIRED");assertTrue(IsolatedAttachmentExtractor.selectHwpPartialCauseList(input).isEmpty());
    }
    @Test void arbitraryPayloadsCountsDuplicatesOrderAndContradictoryQualityFail() throws Exception {
        for(String field:List.of("raw","text","filename")) {
            var input=valid();((ObjectNode)input.at("/hwpPartialCauses/0")).put(field,"PRIVATE_CANARY");invalid(input);
        }
        for(String code:List.of("PRIVATE_CANARY","","UNSUPPORTED_RECORD PRIVATE_CANARY")) {
            var input=valid();((ObjectNode)input.at("/hwpPartialCauses/0")).put("code",code);invalid(input);
        }
        for(String count:List.of("0","-1","33554433","2147483648","1.5","true","\"3\"","null")) {
            var input=valid();((ObjectNode)input.at("/hwpPartialCauses/0")).set("count",json.readTree(count));invalid(input);
        }
        for(String code:List.of("CELL_HEADER_INVALID","UNSUPPORTED_RECORD")) {
            var input=valid();input.withArray("/hwpPartialCauses").addObject().put("code",code).put("count",1);invalid(input);
        }
        for(String quality:List.of("COMPLETE_TEXT","CORRUPT","UNSUPPORTED"))invalid(valid().put("qualityCode",quality));
        invalid(valid().put("format","HWPX"));invalid(valid().putNull("hwpPartialCauses"));
        var empty=valid();empty.putArray("hwpPartialCauses");invalid(empty);
    }
    @Test void allProducerCodesInTheirStableOrderAreAcceptedWithoutArbitraryStrings() throws Exception {
        String source=java.nio.file.Files.readString(java.nio.file.Path.of("attachment-extractor/src/main/java/com/saneb/extractor/ExtractionResult.java"));
        var match=java.util.regex.Pattern.compile("enum HwpPartialCause\\s*\\{([^}]+)}").matcher(source);
        assertTrue(match.find());
        var input=valid();var causes=input.putArray("hwpPartialCauses");
        for(String code:match.group(1).split(",")) causes.addObject().put("code",code.strip()).put("count",1);
        assertEquals(29,causes.size());
        assertEquals(causes,IsolatedAttachmentExtractor.selectHwpPartialCauseList(input));
    }
    @Test void newHwpIpcRequiresDiagnosticsWhileLegacyResponseRemainsReadable() throws Exception {
        var input=valid().put("extractorVersion","1.0.6").put("text","가");
        input.putArray("blocks").addObject().put("startOffset",0).put("endOffset",1).put("locator","synthetic").put("scopeReliable",false);
        var validator=IsolatedAttachmentExtractor.class.getDeclaredMethod("validateResult",JsonNode.class);
        validator.setAccessible(true);
        var extractor=new IsolatedAttachmentExtractor(json,"unused");
        assertDoesNotThrow(()->validator.invoke(extractor,input));
        input.remove("hwpPartialCauses");
        var failure=assertThrows(java.lang.reflect.InvocationTargetException.class,()->validator.invoke(extractor,input));
        assertInstanceOf(IOException.class,failure.getCause());
        assertEquals("INVALID_HWP_PARTIAL_DIAGNOSTIC",failure.getCause().getMessage());
        input.put("extractorVersion","1.0.5");
        assertDoesNotThrow(()->validator.invoke(extractor,input));
    }
}
