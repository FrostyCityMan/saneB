package com.saneb.domain.announcementattachment.extraction;

import static org.assertj.core.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import org.junit.jupiter.api.Test;

class PdfStructureDiagnosticContractTest {
    private final ObjectMapper json=new ObjectMapper();
    private ObjectNode result() {
        var root=json.createObjectNode().put("format","PDF").put("extractorVersion","1.0.14").put("qualityCode","COMPLETE_TEXT").put("text","grant").put("pageCount",1);
        root.putObject("pdfStructure").put("pageCount",1).put("reliablePageCount",0).put("externalObjectInvocationCount",0)
                .put("inlineImageInvocationCount",0).put("blankPageCount",0).put("replacementCharacterCount",0);
        return root;
    }
    private void rejected(ObjectNode root) {assertThatThrownBy(()->IsolatedAttachmentExtractor.selectPdfStructureDetails(root)).isInstanceOf(java.io.IOException.class).hasMessage("INVALID_PDF_STRUCTURE_DIAGNOSTIC");}
    @Test void oldIpcCanOmitDiagnosticsAndUnreliableGeometryDoesNotMakeTextPartial() throws Exception {
        assertThat(IsolatedAttachmentExtractor.selectPdfStructureDetails(null)).isNull();
        assertThat(IsolatedAttachmentExtractor.selectPdfStructureDetails(json.createObjectNode())).isNull();
        var root=result();var value=IsolatedAttachmentExtractor.selectPdfStructureDetails(root);
        assertThat(value).isEqualTo(root.path("pdfStructure"));assertThat(value).isNotSameAs(root.path("pdfStructure"));
    }
    @Test void executedObjectsBlankPagesAndReplacementCharactersCannotBecomeComplete() throws Exception {
        for(String field:List.of("externalObjectInvocationCount","inlineImageInvocationCount","blankPageCount","replacementCharacterCount")) {
            var root=result();((ObjectNode)root.path("pdfStructure")).put(field,1);
            if(field.equals("replacementCharacterCount"))root.put("text","grant\ufffd");
            rejected(root);root.put("qualityCode","PARTIAL_TEXT");assertThat(IsolatedAttachmentExtractor.selectPdfStructureDetails(root)).isNotNull();
            root.put("qualityCode","OCR_REQUIRED");rejected(root);
        }
    }
    @Test void diagnosticsRejectUnknownFieldsWrongTypesAndInvalidRanges() {
        for(String field:List.of("pageCount","reliablePageCount","externalObjectInvocationCount","inlineImageInvocationCount","blankPageCount","replacementCharacterCount")) {
            var root=result();((ObjectNode)root.path("pdfStructure")).put(field,"1");rejected(root);
            root=result();((ObjectNode)root.path("pdfStructure")).put(field,-1);rejected(root);
            root=result();((ObjectNode)root.path("pdfStructure")).put(field,40_000_001);rejected(root);
            root=result();((ObjectNode)root.path("pdfStructure")).remove(field);rejected(root);
        }
        var root=result();((ObjectNode)root.path("pdfStructure")).put("rawResourceName","not allowed");rejected(root);
        root=result();root.put("format","HWP");rejected(root);
        root=result();root.putNull("pdfStructure");rejected(root);
        root=result();root.put("pageCount","1");rejected(root);
    }
    @Test void countsMustAgreeWithDocumentTextAndPerPageOperatorCeiling() {
        for(String field:List.of("pageCount","reliablePageCount","blankPageCount")) {
            var root=result();((ObjectNode)root.path("pdfStructure")).put(field,2);rejected(root);
        }
        var root=result();root.put("text","grant\ufffd").put("qualityCode","PARTIAL_TEXT");rejected(root);
        root=result();root.put("qualityCode","PARTIAL_TEXT");rejected(root);
        root=result();root.put("qualityCode","PARTIAL_TEXT");((ObjectNode)root.path("pdfStructure")).put("externalObjectInvocationCount",200001);rejected(root);
        root=result();root.put("qualityCode","PARTIAL_TEXT");((ObjectNode)root.path("pdfStructure")).put("externalObjectInvocationCount",1).put("reliablePageCount",1);rejected(root);
    }
    @Test void emptyDocumentsRemainOcrRequired() throws Exception {
        var root=result();root.put("text","").put("pageCount",0).put("qualityCode","OCR_REQUIRED");((ObjectNode)root.path("pdfStructure")).put("pageCount",0);
        assertThat(IsolatedAttachmentExtractor.selectPdfStructureDetails(root)).isNotNull();
        root.put("qualityCode","COMPLETE_TEXT");rejected(root);
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"1.0.14",AttachmentRuntimeIdentity.EXTRACTOR_VERSION})
    void currentPdfIpcRequiresDiagnosticsWhileLegacyResponseRemainsReadable(String version) throws Exception {
        var root=result().put("extractorVersion",version);
        root.putArray("blocks").addObject().put("index",0).put("startOffset",0).put("endOffset",5)
                .put("evidenceScopeId","synthetic").put("locator","synthetic").put("scopeReliable",false);
        var validator=IsolatedAttachmentExtractor.class.getDeclaredMethod("validateResult",com.fasterxml.jackson.databind.JsonNode.class);
        validator.setAccessible(true);
        var extractor=new IsolatedAttachmentExtractor(json,"unused");
        assertThatCode(()->validator.invoke(extractor,root)).doesNotThrowAnyException();
        root.remove("pdfStructure");
        assertThatThrownBy(()->validator.invoke(extractor,root)).isInstanceOf(java.lang.reflect.InvocationTargetException.class)
                .hasCauseInstanceOf(java.io.IOException.class).hasRootCauseMessage("INVALID_PDF_STRUCTURE_DIAGNOSTIC");
        root.put("extractorVersion","1.0.13");
        assertThatCode(()->validator.invoke(extractor,root)).doesNotThrowAnyException();
    }
}
