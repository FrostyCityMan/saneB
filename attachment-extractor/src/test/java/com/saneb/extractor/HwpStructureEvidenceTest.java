package com.saneb.extractor;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class HwpStructureEvidenceTest {
    @TempDir Path root;

    @Test void recordsOnlyBoundedNumericStructureAndKeepsTextInItsExistingField() throws Exception {
        byte[] first=records(record(66,0,new byte[24]),text(1,"소상공인 지원사업"),record(77,2,new byte[24]));
        byte[] second=records(text(4,"합성 비공개 연락처 대신 테스트 문구"),text(4,"신청서"));
        var result=AttachmentExtractorMain.selectExtraction(file(List.of(first,second),false));
        assertEquals("PARTIAL_TEXT",result.qualityCode());
        assertEquals(3,result.blocks().size());assertEquals(2,result.hwpStructure().sectionCount());
        assertEquals(5,result.hwpStructure().recordCount());assertEquals(4,result.hwpStructure().maximumLevel());
        assertEquals(List.of(new ExtractionResult.RecordType(66,1),new ExtractionResult.RecordType(67,3),
                new ExtractionResult.RecordType(77,1)),result.hwpStructure().recordTypes());
        var json=new ObjectMapper().valueToTree(result.hwpStructure());
        assertEquals(4,json.size());assertFalse(json.toString().contains("소상공인"));
        assertFalse(json.toString().contains("연락처"));
        assertTrue(result.text().contains("소상공인 지원사업"));
        assertThrows(UnsupportedOperationException.class,()->result.hwpStructure().recordTypes().clear());
    }

    @ParameterizedTest @ValueSource(ints={0,76,77,84,85,86,87,88,89,90,91,92,93,95,98,115,1023})
    void uninterpretedAndFutureRecordTypesNeverBecomeCompleteBecauseTheyExceedTheOldRange(int tag) throws Exception {
        var result=AttachmentExtractorMain.selectExtraction(file(List.of(records(text(1,"지원사업 내용"),record(tag,2,new byte[4]))),false));
        assertEquals("PARTIAL_TEXT",result.qualityCode());assertEquals("지원사업 내용",result.text());
        assertTrue(result.hwpStructure().recordTypes().contains(new ExtractionResult.RecordType(tag,1)));
    }

    @Test void paragraphLayoutMetadataDoesNotByItselfInvalidateText() throws Exception {
        var result=AttachmentExtractorMain.selectExtraction(file(List.of(records(record(66,0,new byte[24]),
                text(1,"사업자 지원금"),record(68,1,new byte[8]),record(69,1,new byte[36]))),true));
        assertEquals("COMPLETE_TEXT",result.qualityCode());assertEquals(4,result.hwpStructure().recordCount());
        assertEquals(1,result.hwpStructure().maximumLevel());
    }

    @Test void extendedRecordLengthAndMaximumEncodedLevelAreCountedWithoutCopyingPayload() throws Exception {
        var result=AttachmentExtractorMain.selectExtraction(file(List.of(text(1023,"가".repeat(3000))),false));
        assertEquals("COMPLETE_TEXT",result.qualityCode());assertEquals(1,result.hwpStructure().recordCount());
        assertEquals(1023,result.hwpStructure().maximumLevel());assertEquals(3000,result.text().length());
    }

    @Test void emptyBodyIsOcrRequiredWithZeroRecordsNotACompleteTextDocument() throws Exception {
        var result=AttachmentExtractorMain.selectExtraction(file(List.of(new byte[0]),false));
        assertEquals("OCR_REQUIRED",result.qualityCode());assertEquals(0,result.hwpStructure().recordCount());
        assertEquals(0,result.hwpStructure().maximumLevel());assertTrue(result.hwpStructure().recordTypes().isEmpty());
    }

    @Test void existingNonHwpAndFailureJsonDoesNotGainAnEmptyDiagnosticField() throws Exception {
        var json=new ObjectMapper();
        assertFalse(json.valueToTree(ExtractionResult.failure("CORRUPT")).has("hwpStructure"));
        var legacy=new ExtractionResult("PDF",ExtractionResult.VERSION,"OCR_REQUIRED","",List.of(),1,null);
        assertEquals(7,json.valueToTree(legacy).size());assertFalse(json.valueToTree(legacy).has("hwpStructure"));
    }

    private byte[] text(int level,String text){return record(67,level,text.getBytes(StandardCharsets.UTF_16LE));}
    private byte[] record(int tag,int level,byte[] data) {
        int extended=data.length>=4095?4:0;
        var buffer=ByteBuffer.allocate(4+extended+data.length).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(tag|(level<<10)|(Math.min(4095,data.length)<<20));
        if(extended>0)buffer.putInt(data.length);
        buffer.put(data);return buffer.array();
    }
    private byte[] records(byte[]... parts)throws Exception {
        var bytes=new ByteArrayOutputStream();for(var part:parts)bytes.write(part);return bytes.toByteArray();
    }
    private Path file(List<byte[]> sections,boolean compressed)throws Exception {
        Path target=root.resolve(java.util.UUID.randomUUID()+".hwp");byte[] header=new byte[256];
        System.arraycopy("HWP Document File".getBytes(StandardCharsets.US_ASCII),0,header,0,17);
        header[35]=5;ByteBuffer.wrap(header,36,4).order(ByteOrder.LITTLE_ENDIAN).putInt(compressed?1:0);
        try(var ole=new POIFSFileSystem()) {
            ole.getRoot().createDocument("FileHeader",new ByteArrayInputStream(header));
            var body=ole.getRoot().createDirectory("BodyText");
            for(int index=0;index<sections.size();index++) {
                byte[] data=sections.get(index);
                if(compressed) {
                    var bytes=new ByteArrayOutputStream();var deflater=new Deflater(Deflater.DEFAULT_COMPRESSION,true);
                    try(var zip=new DeflaterOutputStream(bytes,deflater)){zip.write(data);}finally{deflater.end();}
                    data=bytes.toByteArray();
                }
                body.createDocument("Section"+index,new ByteArrayInputStream(data));
            }
            try(var output=Files.newOutputStream(target)){ole.writeFilesystem(output);}
        }
        return target;
    }
}
