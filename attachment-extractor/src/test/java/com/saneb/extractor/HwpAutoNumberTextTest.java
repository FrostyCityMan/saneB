package com.saneb.extractor;

import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** 공개 형식의 합성 번호 fixture다. 실제 외부 파일의 지원 성공과 구분한다. */
class HwpAutoNumberTextTest {
    private static final int AUTO_NUMBER=0x61746e6f;
    @TempDir Path root;
    private record Record(int tag,int level,byte[] data) { }

    @ParameterizedTest @ValueSource(booleans={false,true})
    void compressedAndPlainOlePreserveStoredNumberDecorationsAndIndependentScopes(boolean compressed) throws Exception {
        for(int kind:List.of(1,2,3,4,5)) {
            var result=AttachmentExtractorMain.selectExtraction(file(source(header(kind,42,'(',')')),compressed));
            assertEquals("1.0.13",result.extractorVersion());
            assertEquals("COMPLETE_TEXT",result.qualityCode());
            assertEquals("대상 소상공인 😀\n(42)\n지원금 안내",result.text());
            assertEquals(3,result.blocks().size());
            assertTrue(result.blocks().stream().allMatch(ExtractionResult.Block::scopeReliable));
            assertEquals("Section0:paragraph:1:auto-number:1",result.blocks().get(1).locator());
            assertScopes(result);
        }
    }

    @Test void zeroDecorationsUnsignedMaximumAndFootnoteSuperscriptArePreserved() throws Exception {
        assertEquals("대상 소상공인 😀\n65535\n지원금 안내",select(source(header(1,65535,'\0','\0'))).text());
        var result=select(source(header(0x1001,1,'※',')')));
        assertEquals("COMPLETE_TEXT",result.qualityCode());assertTrue(result.text().contains("※1)"));
    }

    @ParameterizedTest @ValueSource(ints={0,4,8,10,12,14,15,17,20,24})
    void truncatedAndExtendedHeadersRemainPartial(int length) throws Exception {
        var data=java.util.Arrays.copyOf(header(1,1,'(',')'),length);
        assertPartial(select(source(data)));
    }

    @Test void dynamicPageNumbersUnknownKindsShapesFlagsAndZeroNumbersRemainPartial() throws Exception {
        for(int flags:List.of(0,6,15,0x11,0x21,0x801,0x811,0x1002,0x2001,0x80000001,-1))
            assertPartial(select(source(header(flags,1,'(',')'))));
        assertPartial(select(source(header(1,0,'(',')'))));
        var data=header(1,1,'(',')');little(data).putChar(10,'※');
        assertPartial(select(source(data)));
    }

    @Test void invalidDecorationCharactersCannotBeTrustedOrInjectedAsNumberText() throws Exception {
        for(char invalid:new char[]{'\n','\t','\u007f','\ud800','\udfff','\u202e','\ufffd','\ufffe','\uffff'}) {
            for(boolean before:List.of(false,true)) {
                var result=select(source(header(1,1,before?invalid:'(',before?')':invalid)));
                assertPartial(result);assertEquals(2,result.blocks().size());
            }
        }
    }

    @Test void missingWrongAndWrongDepthAnchorsPreserveNumberButMarkItUnreliable() throws Exception {
        var records=source(header(1,7,'(',')'));
        records.set(1,new Record(67,1,utf("대상 소상공인 😀 지원금 안내")));
        assertUnreliableNumber(select(records));
        for(int code:List.of(3,11,17,21)) {
            records=source(header(1,7,'(',')'));
            records.set(1,new Record(67,1,join(utf("대상 소상공인 😀"),anchor(code,AUTO_NUMBER),utf("지원금 안내"))));
            assertUnreliableNumber(select(records));
        }
        records=source(header(1,7,'(',')'));records.set(2,new Record(71,2,records.get(2).data()));
        assertUnreliableNumber(select(records));
    }

    @Test void noMatchingHeaderOrDuplicateHeadersDoNotBecomeComplete() throws Exception {
        var records=source(header(1,7,'(',')'));records.remove(2);assertPartial(select(records));
        records=source(header(1,7,'(',')'));records.add(records.get(2));
        var result=select(records);assertPartial(result);
        assertEquals(2,result.blocks().stream().filter(b->b.locator().contains(":auto-number:")).count());
        assertEquals(1,result.blocks().stream().filter(b->b.locator().contains(":auto-number:")&&!b.scopeReliable()).count());
        assertScopes(result);
    }

    @Test void everyChildIncludingKnownLayoutRecordsInvalidatesLeafNumber() throws Exception {
        for(int tag:List.of(68,69,70,72,73,74,75,76,85,87)) {
            var records=source(header(1,7,'(',')'));records.add(new Record(tag,2,new byte[4]));
            assertUnreliableNumber(select(records));
        }
        var records=source(header(1,7,'(',')'));
        records.add(new Record(66,2,paragraphHeader(7,true)));records.add(new Record(67,3,utf("추가 조건 원문")));
        var result=select(records);assertUnreliableNumber(result);assertTrue(result.text().contains("추가 조건 원문"));
        assertFalse(result.blocks().get(2).scopeReliable());
    }

    @Test void repeatedIdsFollowAnchorOrderWithoutMergingNeighboringEvidence() throws Exception {
        var records=source(header(1,7,'(',')'));
        records.set(1,new Record(67,1,join(utf("대상 소상공인 😀"),anchor(18,AUTO_NUMBER),utf("중간"),anchor(18,AUTO_NUMBER),utf("지원금 안내"))));
        records.add(new Record(71,1,header(4,8,'[',']')));
        var result=select(records);assertEquals("COMPLETE_TEXT",result.qualityCode());
        assertEquals("대상 소상공인 😀\n(7)\n중간\n[8]\n지원금 안내",result.text());assertScopes(result);
    }

    @Test void numberInsideValidatedFootnoteKeepsItsContainingLocation() throws Exception {
        var records=new ArrayList<Record>();
        byte[] text=join(utf("본문 앞"),anchor(17,0x666e2020),utf("본문 뒤"));
        records.add(new Record(66,0,paragraphHeader(text.length/2,true)));records.add(new Record(67,1,text));
        records.add(new Record(71,1,little(new byte[20]).putInt(0x666e2020).putInt(1).array()));
        records.add(new Record(72,2,little(new byte[16]).putInt(1).array()));
        text=join(anchor(18,AUTO_NUMBER),utf("각주 조건"));
        records.add(new Record(66,2,paragraphHeader(text.length/2,true)));records.add(new Record(67,3,text));
        records.add(new Record(71,3,header(0x1001,1,'\0',')')));
        var result=select(records);assertEquals("COMPLETE_TEXT",result.qualityCode());
        assertEquals("본문 앞\n1)\n각주 조건\n본문 뒤",result.text());
        assertEquals("Section0:footnote:1:paragraph:2:auto-number:1",result.blocks().get(1).locator());assertScopes(result);
    }

    private void assertPartial(ExtractionResult result) {
        assertEquals("PARTIAL_TEXT",result.qualityCode());
        assertTrue(result.text().contains("대상 소상공인 😀"));assertTrue(result.text().contains("지원금 안내"));
        assertFalse(result.hwpPartialCauses().isEmpty());assertScopes(result);
    }
    private void assertUnreliableNumber(ExtractionResult result) {
        assertPartial(result);var numbers=result.blocks().stream().filter(b->b.locator().contains(":auto-number:")).toList();
        assertEquals(1,numbers.size());assertFalse(numbers.getFirst().scopeReliable());assertTrue(result.text().contains("(7)"));
    }
    private void assertScopes(ExtractionResult result) {
        assertEquals(result.blocks().size(),result.blocks().stream().map(ExtractionResult.Block::evidenceScopeId).distinct().count());
        int offset=0;for(var block:result.blocks()) {assertEquals(offset,block.startOffset());assertEquals(block.locator(),block.evidenceScopeId());offset=block.endOffset()+1;}
        assertEquals(result.text().codePointCount(0,result.text().length()),offset-1);
    }
    private List<Record> source(byte[] number) throws Exception {
        byte[] text=join(utf("대상 소상공인 😀"),anchor(18,AUTO_NUMBER),utf("지원금 안내"));
        return new ArrayList<>(List.of(new Record(66,0,paragraphHeader(text.length/2,true)),new Record(67,1,text),new Record(71,1,number)));
    }
    private ExtractionResult select(List<Record> records) throws Exception {
        var evidence=new TextEvidence();var parser=new HwpSectionText("Section0",evidence);
        for(var record:records)parser.insertRecord(record.tag(),record.level(),record.data());parser.saveEnd();return evidence.selectResult("HWP",null);
    }
    private byte[] header(int flags,int number,char before,char after) {return little(new byte[16]).putInt(AUTO_NUMBER).putInt(flags).putShort((short)number).putChar('\0').putChar(before).putChar(after).array();}
    private byte[] paragraphHeader(int units,boolean last){return little(new byte[24]).putInt(units|(last?0x80000000:0)).array();}
    private byte[] anchor(int code,int id){return little(new byte[16]).putChar((char)code).putInt(id).putLong(0).putChar((char)code).array();}
    private byte[] utf(String value){return value.getBytes(StandardCharsets.UTF_16LE);}
    private ByteBuffer little(byte[] bytes){return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);}
    private byte[] join(byte[]... parts)throws Exception{var bytes=new ByteArrayOutputStream();for(var part:parts)bytes.write(part);return bytes.toByteArray();}
    private Path file(List<Record> records,boolean compressed)throws Exception {
        var out=new ByteArrayOutputStream();for(var record:records){out.write(little(new byte[4]).putInt(record.tag()|(record.level()<<10)|(record.data().length<<20)).array());out.write(record.data());}
        byte[] body=out.toByteArray(),header=new byte[256];System.arraycopy("HWP Document File".getBytes(StandardCharsets.US_ASCII),0,header,0,17);
        header[35]=5;header[33]=3;little(header).putInt(36,compressed?1:0);
        if(compressed){out=new ByteArrayOutputStream();var deflater=new Deflater(Deflater.DEFAULT_COMPRESSION,true);try(var zip=new DeflaterOutputStream(out,deflater)){zip.write(body);}finally{deflater.end();}body=out.toByteArray();}
        Path path=root.resolve(java.util.UUID.randomUUID()+".hwp");
        try(var ole=new POIFSFileSystem()){
            ole.getRoot().createDocument("FileHeader",new ByteArrayInputStream(header));ole.getRoot().createDirectory("BodyText").createDocument("Section0",new ByteArrayInputStream(body));
            try(var output=Files.newOutputStream(path)){ole.writeFilesystem(output);}
        }
        return path;
    }
}
