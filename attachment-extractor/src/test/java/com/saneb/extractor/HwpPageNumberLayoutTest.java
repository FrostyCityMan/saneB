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

/** 명세의 제한 교집합만 지원하는 합성 입력. 실제 공고의 쪽 번호 성공 증거가 아니다. */
class HwpPageNumberLayoutTest {
    private static final int PAGE_NUMBER=0x70676e70;
    @TempDir Path root;
    private record Record(int tag,int level,byte[] data) { }

    @ParameterizedTest @ValueSource(booleans={false,true})
    void compressedAndPlainOleRecognizeLayoutWithoutInventingPageText(boolean compressed) throws Exception {
        for(int position=0;position<=10;position++) for(int decoration:List.of(0,45)) {
            var result=AttachmentExtractorMain.selectExtraction(file(source(header(position<<8,0,0,decoration,45)),compressed));
            assertComplete(result,"대상 소상공인 😀\n지원금 안내");
            assertEquals(ExtractionResult.VERSION,result.extractorVersion());
            assertEquals(2,result.blocks().size());assertNull(result.pageCount());
            assertEquals("Section0:paragraph:1:segment:1",result.blocks().getFirst().locator());
            assertEquals("Section0:paragraph:1:segment:2",result.blocks().getLast().locator());
            assertTrue(result.hwpStructure().controlHeaders().stream().anyMatch(h->h.kind().equals("PAGE_NUMBER")));
        }
    }

    @ParameterizedTest @ValueSource(ints={0,3,4,8,12,14,15,17,20,24})
    void unknownHeaderLengthsRemainPartial(int length) throws Exception {
        assertPartial(select(source(java.util.Arrays.copyOf(header(),length))));
    }

    @Test void nonDecimalUnknownPositionAndReservedFlagsRemainPartial() throws Exception {
        for(int flags:List.of(1,2,0x80,0xff,0xb00,0xc00,0xf00,0x1000,0x10000,Integer.MIN_VALUE,-1))
            assertPartial(select(source(header(flags,0,0,45,45))));
    }

    @Test void ambiguousNumberSymbolsDecorationsAndFixedTailAreNotDiscardedAsSupported() throws Exception {
        for(int offset:List.of(8,10,12,14)) for(int value:List.of(1,2,32,65,0x203b,0x202e,0xd800,0xffff)) {
            var data=header();little(data).putShort(offset,(short)value);
            assertPartial(select(source(data)));
        }
        var data=header();little(data).putShort(14,(short)0);assertPartial(select(source(data)));
        data=header();little(data).putShort(8,(short)45);assertPartial(select(source(data)));
        data=header();little(data).putShort(10,(short)45);assertPartial(select(source(data)));
    }

    @Test void missingWrongAnchorKindsDepthAndHeaderIdentityRemainPartial() throws Exception {
        var records=source(header());records.set(1,new Record(67,1,utf("대상 소상공인 😀 지원금 안내")));
        assertPartial(select(records));
        for(int code:List.of(1,2,3,4,5,9,11,12,14,15,16,17,18,19,20,22,23)) {
            records=source(header());records.set(1,new Record(67,1,join(utf("대상 소상공인 😀"),anchor(code,PAGE_NUMBER),utf("지원금 안내"))));
            assertPartial(select(records));
        }
        records=source(header());records.set(2,new Record(71,2,header()));assertPartial(select(records));
        var wrong=header();little(wrong).putInt(0,0x70676e71);assertPartial(select(source(wrong)));
    }

    @Test void missingDuplicateOrUnanchoredHeadersAndExtraAnchorsCannotBecomeComplete() throws Exception {
        var records=source(header());records.remove(2);assertPartial(select(records));
        records=source(header());records.add(new Record(71,1,header()));assertPartial(select(records));
        records=source(header());records.set(1,new Record(67,1,join(utf("대상 소상공인 😀"),anchor(21,PAGE_NUMBER),anchor(21,PAGE_NUMBER),utf("지원금 안내"))));
        assertPartial(select(records));
        records=source(header());records.add(new Record(71,0,header()));assertPartial(select(records));
    }

    @ParameterizedTest @ValueSource(ints={68,69,70,72,73,74,75,76,77,85,87})
    void evenKnownLayoutChildrenInvalidateTheLeaf(int tag) throws Exception {
        var records=source(header());records.add(new Record(tag,2,new byte[4]));assertPartial(select(records));
    }

    @Test void childParagraphAndBareTextArePreservedButUnreliable() throws Exception {
        for(boolean withHeader:List.of(false,true)) {
            var records=source(header());
            if(withHeader)records.add(new Record(66,2,paragraphHeader(7,true)));
            records.add(new Record(67,withHeader?3:2,utf("숨긴 조건 😀")));
            var result=select(records);assertPartial(result);
            var hidden=result.blocks().stream().filter(b->result.text().substring(
                    result.text().offsetByCodePoints(0,b.startOffset()),result.text().offsetByCodePoints(0,b.endOffset())).equals("숨긴 조건 😀")).findFirst().orElseThrow();
            assertFalse(hidden.scopeReliable());assertTrue(result.text().contains("숨긴 조건 😀"));
        }
    }

    @Test void nestedControlAndUnknownPictureAreNotHiddenBySupportedLayout() throws Exception {
        var records=source(header());records.add(new Record(71,2,header()));assertPartial(select(records));
        records=source(header());
        records.set(1,new Record(67,1,join(utf("대상 소상공인 😀"),anchor(21,PAGE_NUMBER),utf("지원금 안내"),anchor(11,0x67736f20))));
        records.add(new Record(71,1,little(new byte[4]).putInt(0x67736f20).array()));
        records.add(new Record(85,2,new byte[4]));
        var result=select(records);assertPartial(result);
        assertTrue(result.hwpPartialCauses().stream().anyMatch(c->c.code()==ExtractionResult.HwpPartialCause.UNSUPPORTED_RECORD));
    }

    @Test void repeatedLayoutsRetainTextOrderDistinctScopesAndNoGeneratedPageValues() throws Exception {
        var records=source(header());
        records.set(1,new Record(67,1,join(utf("대상 소상공인 😀"),anchor(21,PAGE_NUMBER),utf("중간"),anchor(21,PAGE_NUMBER),utf("지원금 안내"))));
        records.add(new Record(71,1,header(0xa00,0,0,0,45)));
        var result=select(records);assertComplete(result,"대상 소상공인 😀\n중간\n지원금 안내");assertEquals(3,result.blocks().size());
    }

    @Test void emptyLayoutIsNotAnExtractedDocument() throws Exception {
        var records=source(header());records.set(1,new Record(67,1,anchor(21,PAGE_NUMBER)));
        var result=select(records);assertEquals("OCR_REQUIRED",result.qualityCode());assertTrue(result.blocks().isEmpty());
    }

    @Test void validatedNoteAndTableContainmentRetainsParentScope() throws Exception {
        for(boolean table:List.of(false,true)) {
            var records=new ArrayList<Record>();int parentId=table?0x74626c20:0x666e2020;
            byte[] text=join(utf("본문 앞"),anchor(table?11:17,parentId),utf("본문 뒤"));
            records.add(new Record(66,0,paragraphHeader(text.length/2,true)));records.add(new Record(67,1,text));
            records.add(new Record(71,1,little(new byte[table?40:20]).putInt(parentId).array()));
            if(table) {
                byte[] metadata=new byte[22];little(metadata).putShort(4,(short)1).putShort(6,(short)1).putShort(18,(short)1);
                records.add(new Record(77,2,metadata));
                byte[] cell=new byte[34];little(cell).putInt(0,1).putShort(12,(short)1).putShort(14,(short)1);
                records.add(new Record(72,2,cell));
            } else records.add(new Record(72,2,little(new byte[16]).putInt(1).array()));
            text=join(utf("대상 소상공인 😀"),anchor(21,PAGE_NUMBER),utf("지원금 안내"));
            records.add(new Record(66,2,paragraphHeader(text.length/2,true)));records.add(new Record(67,3,text));
            records.add(new Record(71,3,header()));
            var result=select(records);assertComplete(result,"본문 앞\n대상 소상공인 😀\n지원금 안내\n본문 뒤");
            String parent=table?":table:1:cell:0:0:":":footnote:1:";
            assertTrue(result.blocks().get(1).locator().contains(parent));assertTrue(result.blocks().get(2).locator().contains(parent));
        }
    }

    private void assertComplete(ExtractionResult result,String expected) {
        assertEquals("COMPLETE_TEXT",result.qualityCode());assertEquals(expected,result.text());
        assertTrue(result.hwpPartialCauses().isEmpty());assertTrue(result.blocks().stream().allMatch(ExtractionResult.Block::scopeReliable));assertScopes(result);
    }
    private void assertPartial(ExtractionResult result) {
        assertEquals("PARTIAL_TEXT",result.qualityCode());
        assertTrue(result.text().contains("대상 소상공인 😀"));assertTrue(result.text().contains("지원금 안내"));
        assertFalse(result.hwpPartialCauses().isEmpty());assertScopes(result);
    }
    private void assertScopes(ExtractionResult result) {
        assertEquals(result.blocks().size(),result.blocks().stream().map(ExtractionResult.Block::evidenceScopeId).distinct().count());
        int offset=0;for(var block:result.blocks()) {assertEquals(offset,block.startOffset());assertEquals(block.locator(),block.evidenceScopeId());offset=block.endOffset()+1;}
        assertEquals(result.text().codePointCount(0,result.text().length()),offset-1);
    }
    private List<Record> source(byte[] layout)throws Exception {
        byte[] text=join(utf("대상 소상공인 😀"),anchor(21,PAGE_NUMBER),utf("지원금 안내"));
        return new ArrayList<>(List.of(new Record(66,0,paragraphHeader(text.length/2,true)),new Record(67,1,text),new Record(71,1,layout)));
    }
    private ExtractionResult select(List<Record> records)throws Exception {
        var evidence=new TextEvidence();var parser=new HwpSectionText("Section0",evidence);
        for(var record:records)parser.insertRecord(record.tag(),record.level(),record.data());parser.saveEnd();return evidence.selectResult("HWP",null);
    }
    private byte[] header(){return header(0x500,0,0,45,45);}
    private byte[] header(int flags,int a,int b,int c,int d){return little(new byte[16]).putInt(PAGE_NUMBER).putInt(flags).putShort((short)a).putShort((short)b).putShort((short)c).putShort((short)d).array();}
    private byte[] paragraphHeader(int units,boolean last){return little(new byte[24]).putInt(units|(last?0x80000000:0)).array();}
    private byte[] anchor(int code,int id){return little(new byte[16]).putChar((char)code).putInt(id).putLong(0).putChar((char)code).array();}
    private byte[] utf(String value){return value.getBytes(StandardCharsets.UTF_16LE);}
    private ByteBuffer little(byte[] data){return ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);}
    private byte[] join(byte[]... parts)throws Exception{var out=new ByteArrayOutputStream();for(var part:parts)out.write(part);return out.toByteArray();}
    private Path file(List<Record> records,boolean compressed)throws Exception {
        var out=new ByteArrayOutputStream();for(var record:records){out.write(little(new byte[4]).putInt(record.tag()|(record.level()<<10)|(record.data().length<<20)).array());out.write(record.data());}
        byte[] body=out.toByteArray(),fileHeader=new byte[256];System.arraycopy("HWP Document File".getBytes(StandardCharsets.US_ASCII),0,fileHeader,0,17);
        fileHeader[35]=5;fileHeader[33]=3;little(fileHeader).putInt(36,compressed?1:0);
        if(compressed){out=new ByteArrayOutputStream();var deflater=new Deflater(Deflater.DEFAULT_COMPRESSION,true);try(var zip=new DeflaterOutputStream(out,deflater)){zip.write(body);}finally{deflater.end();}body=out.toByteArray();}
        Path path=root.resolve(java.util.UUID.randomUUID()+".hwp");
        try(var ole=new POIFSFileSystem()){
            ole.getRoot().createDocument("FileHeader",new ByteArrayInputStream(fileHeader));ole.getRoot().createDirectory("BodyText").createDocument("Section0",new ByteArrayInputStream(body));
            try(var output=Files.newOutputStream(path)){ole.writeFilesystem(output);}
        }
        return path;
    }
}
