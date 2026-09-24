package com.saneb.extractor;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class HwpxStructureEvidenceTest {
    @TempDir Path root;
    private static final String OPEN = "<hs:sec xmlns:hs='http://www.hancom.co.kr/hwpml/2011/section' "
            + "xmlns:hp='http://www.hancom.co.kr/hwpml/2011/paragraph' xmlns:other='urn:other'>";

    @ParameterizedTest @ValueSource(strings={"pic","ole","equation"})
    void eachControlPreservesPartialQualityAndReportsOnlyItsCount(String name) throws Exception {
        var result = extract(List.of(paragraph("합성_PRIVATE_CANARY") + "<hp:" + name + " attr='PRIVATE_ATTR'/>"));
        assertEquals("PARTIAL_TEXT", result.qualityCode());
        var counts = result.hwpxStructure();
        assertEquals(1, counts.sectionCount()); assertEquals(1, counts.paragraphCount());
        assertEquals("pic".equals(name) ? 1 : 0, counts.pictureCount());
        assertEquals("ole".equals(name) ? 1 : 0, counts.oleCount());
        assertEquals("equation".equals(name) ? 1 : 0, counts.equationCount());
        assertEquals(0, counts.replacementCharacterCount());
        var json = new ObjectMapper().valueToTree(counts);
        assertEquals(6, json.size()); assertFalse(json.toString().contains("PRIVATE"));
        assertTrue(result.text().contains("PRIVATE_CANARY"));
    }
    @Test void replacementCharacterIsASeparateReasonWithoutInventingAControl() throws Exception {
        var result = extract(List.of(paragraph("😀 \ufffd 합성 \ufffd")));
        assertEquals("PARTIAL_TEXT", result.qualityCode());
        assertEquals(new ExtractionResult.HwpxStructure(1,1,0,0,0,2), result.hwpxStructure());
        assertEquals(8, result.blocks().getFirst().endOffset());
    }
    @Test void multipleSectionsAndNestedParagraphsKeepOriginalOrderAndScopes() throws Exception {
        var result = extract(List.of("<hp:p><hp:t>앞</hp:t>" + paragraph("안") + "<hp:t>뒤</hp:t></hp:p>", paragraph("끝")));
        assertEquals("COMPLETE_TEXT", result.qualityCode()); assertEquals("앞\n안\n뒤\n끝", result.text());
        assertEquals(new ExtractionResult.HwpxStructure(2,3,0,0,0,0), result.hwpxStructure());
        assertEquals(4, result.blocks().size()); assertTrue(result.blocks().stream().allMatch(ExtractionResult.Block::scopeReliable));
    }
    @Test void countersAccumulateButEmptyTextStillRequiresOcr() throws Exception {
        var result = extract(List.of("<hp:p/><hp:pic/><hp:pic/><hp:ole/>","<hp:p/><hp:equation/>"));
        assertEquals("OCR_REQUIRED", result.qualityCode()); assertTrue(result.text().isEmpty());
        assertEquals(new ExtractionResult.HwpxStructure(2,2,2,1,1,0), result.hwpxStructure());
    }
    @Test void foreignNamespaceAndAttributeContentsAreNotCountedAsHancomControls() throws Exception {
        var result = extract(List.of(paragraph("본문") + "<other:pic attr='hp:ole'/><other:equation/>"));
        assertEquals("COMPLETE_TEXT", result.qualityCode());
        assertEquals(new ExtractionResult.HwpxStructure(1,1,0,0,0,0), result.hwpxStructure());
    }
    @Test void legacyAndFailureJsonDoNotGainEmptyDiagnosticKeys() throws Exception {
        var json = new ObjectMapper();
        assertFalse(json.valueToTree(ExtractionResult.failure("CORRUPT")).has("hwpxStructure"));
        var legacy = new ExtractionResult("PDF",ExtractionResult.VERSION,"OCR_REQUIRED","",List.of(),1,null);
        assertEquals(7,json.valueToTree(legacy).size());
    }
    private String paragraph(String text) { return "<hp:p><hp:run><hp:t>" + text + "</hp:t></hp:run></hp:p>"; }
    private ExtractionResult extract(List<String> sections) throws Exception {
        Path path = root.resolve(java.util.UUID.randomUUID()+".hwpx");
        try (var zip = new ZipOutputStream(Files.newOutputStream(path))) {
            zip.putNextEntry(new ZipEntry("mimetype")); zip.write("application/hwp+zip".getBytes(StandardCharsets.US_ASCII)); zip.closeEntry();
            for (int index=0; index<sections.size(); index++) {
                zip.putNextEntry(new ZipEntry("Contents/section"+index+".xml"));
                zip.write((OPEN+sections.get(index)+"</hs:sec>").getBytes(StandardCharsets.UTF_8)); zip.closeEntry();
            }
        }
        return AttachmentExtractorMain.selectExtraction(path);
    }
}
