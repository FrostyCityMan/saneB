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
import java.util.function.UnaryOperator;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** 공개 HWP 레코드 구조의 합성 각주/미주. 외부 파일 성공 증거와 구분한다. */
class HwpNoteTextTest {
    static final int FOOTNOTE=0x666e2020, ENDNOTE=0x656e2020;
    @TempDir Path root;
    record Record(int tag,int level,byte[] data) { }

    @ParameterizedTest @ValueSource(booleans={false,true})
    void actualOleSectionsPreserveAllNoteParagraphsAndSeparateAndScopes(boolean compressed) throws Exception {
        for(int id:List.of(FOOTNOTE,ENDNOTE)) {
            var result=AttachmentExtractorMain.selectExtraction(file(source(id),compressed));
            assertEquals("COMPLETE_TEXT",result.qualityCode());
            assertEquals(List.of("공고 앞","대상 소상공인 😀","별도 문단 지원금","공고 뒤"),texts(result));
            assertTrue(result.blocks().stream().allMatch(ExtractionResult.Block::scopeReliable));
            assertTrue(result.blocks().get(1).locator().contains(id==FOOTNOTE?":footnote:1:":":endnote:1:"));
            assertScopes(result);
        }
    }

    @Test void shorterLegacyHeaderAndKnownHorizontalListFlagsAreSupported() throws Exception {
        for(int flags:List.of(0,8,16,32,64,80)) {
            var records=change(source(FOOTNOTE),71,0,d->java.util.Arrays.copyOf(d,16));
            records=change(records,72,0,d->{little(d).putInt(4,flags);return d;});
            assertEquals("COMPLETE_TEXT",select(records).qualityCode());
        }
    }

    @ParameterizedTest @ValueSource(ints={0,7,8,14,15,17,20,24,100})
    void unknownListLengthsCannotBecomeComplete(int size) throws Exception {
        assertPartial(change(source(FOOTNOTE),72,0,d->java.util.Arrays.copyOf(d,size)));
    }

    @Test void invalidCountFlagsAndReservedBytesRetainAllTextForReview() throws Exception {
        for(int count:List.of(-1,0,1,3,20001,Integer.MAX_VALUE))
            assertPartial(change(source(FOOTNOTE),72,0,d->{little(d).putInt(0,count);return d;}));
        for(int flags:List.of(1,2,7,24,96,128,-1))
            assertPartial(change(source(FOOTNOTE),72,0,d->{little(d).putInt(4,flags);return d;}));
        for(int offset:List.of(8,12))
            assertPartial(change(source(FOOTNOTE),72,0,d->{little(d).putInt(offset,1);return d;}));
    }

    @Test void invalidHeaderLengthAndNumberShapeRemainPartial() throws Exception {
        for(int length:List.of(4,8,15,17,19,21,24))
            assertPartial(change(source(FOOTNOTE),71,0,d->java.util.Arrays.copyOf(d,length)));
        for(int shape:List.of(-1,17,127,130,65536))
            assertPartial(change(source(FOOTNOTE),71,0,d->{little(d).putInt(12,shape);return d;}));
    }

    @Test void missingDuplicateLateAndWrongDepthListsRemainPartial() throws Exception {
        var records=source(FOOTNOTE);records.remove(3);assertPartial(records);
        records=source(FOOTNOTE);records.add(4,records.get(3));assertPartial(records);
        records=source(FOOTNOTE);var list=records.remove(3);records.add(list);assertPartial(records);
        records=source(FOOTNOTE);list=records.get(3);records.set(3,new Record(72,3,list.data()));assertPartial(records);
    }

    @Test void paragraphCountHeaderLengthUnitsAndFinalMarkerMustMatch() throws Exception {
        assertPartial(change(source(FOOTNOTE),66,1,d->{little(d).putInt(0,1);return d;}));
        assertPartial(change(source(FOOTNOTE),66,1,d->{little(d).putInt(0,little(d).getInt(0)|0x80000000);return d;}));
        assertPartial(change(source(FOOTNOTE),66,2,d->{little(d).putInt(0,little(d).getInt(0)&0x7fffffff);return d;}));
        assertPartial(change(source(FOOTNOTE),66,1,d->java.util.Arrays.copyOf(d,23)));
        var records=source(FOOTNOTE);Record header=records.get(4),text=records.get(5);
        records.set(4,new Record(66,3,header.data()));records.set(5,new Record(67,4,text.data()));assertPartial(records);
    }

    @Test void missingAndWrongAnchorKeepNotesUnreliableRatherThanDroppingThem() throws Exception {
        assertPartial(change(source(FOOTNOTE),67,0,d->utf("공고 앞 공고 뒤")));
        assertPartial(change(source(FOOTNOTE),67,0,d->{int offset=utf("공고 앞").length;
            little(d).putChar(offset,(char)11).putChar(offset+14,(char)11);return d;}));
    }

    @Test void unknownNestedContentIsNotIgnoredOrPromoted() throws Exception {
        var records=source(FOOTNOTE);records.add(4,new Record(85,2,new byte[4]));assertPartial(records);
        records=source(FOOTNOTE);records.add(4,new Record(73,2,new byte[4]));assertPartial(records);
        records=source(FOOTNOTE);
        byte[] text=join(utf("別途 원문"),anchor(18,0x61746e6f));
        records.add(new Record(66,2,header(text.length/2,true)));
        records.add(new Record(67,3,text));
        records.add(new Record(71,3,little(new byte[16]).putInt(0x61746e6f).array()));
        var result=select(records);assertEquals("PARTIAL_TEXT",result.qualityCode());
        assertTrue(result.text().contains("別途 원문"));
        assertTrue(result.hwpPartialCauses().stream().anyMatch(c->c.code()==ExtractionResult.HwpPartialCause.UNSUPPORTED_CONTROL));
    }

    @Test void repeatedNoteIdsKeepSeparateLocationsAndOrder() throws Exception {
        var records=source(FOOTNOTE);
        records.set(1,new Record(67,1,join(utf("공고 앞"),anchor(17,FOOTNOTE),utf("중간"),anchor(17,FOOTNOTE),utf("공고 뒤"))));
        records.addAll(source(FOOTNOTE).subList(2,8));
        var result=select(records);
        assertEquals("COMPLETE_TEXT",result.qualityCode());
        assertEquals(List.of("공고 앞","대상 소상공인 😀","별도 문단 지원금","중간","대상 소상공인 😀","별도 문단 지원금","공고 뒤"),texts(result));
        assertTrue(result.blocks().get(4).locator().contains(":footnote:2:"));assertScopes(result);
    }

    private void assertPartial(List<Record> records) throws Exception {
        var result=select(records);assertEquals("PARTIAL_TEXT",result.qualityCode());
        assertTrue(result.text().contains("대상 소상공인 😀"));assertTrue(result.text().contains("별도 문단 지원금"));
        assertTrue(result.blocks().stream().filter(b->b.locator().contains(":footnote:" )).noneMatch(ExtractionResult.Block::scopeReliable));
        assertScopes(result);
    }
    private void assertScopes(ExtractionResult result) {
        assertEquals(result.blocks().size(),result.blocks().stream().map(ExtractionResult.Block::evidenceScopeId).distinct().count());
        int offset=0;for(var block:result.blocks()) {assertEquals(offset,block.startOffset());assertEquals(block.locator(),block.evidenceScopeId());offset=block.endOffset()+1;}
        assertEquals(result.text().codePointCount(0,result.text().length()),offset-1);
    }
    private List<String> texts(ExtractionResult result) {return result.blocks().stream().map(b->result.text().substring(result.text().offsetByCodePoints(0,b.startOffset()),result.text().offsetByCodePoints(0,b.endOffset()))).toList();}
    private List<Record> source(int id) throws Exception {
        byte[] text=join(utf("공고 앞"),anchor(17,id),utf("공고 뒤"));
        var records=new ArrayList<Record>();records.add(new Record(66,0,header(text.length/2,true)));records.add(new Record(67,1,text));
        records.add(new Record(71,1,little(new byte[20]).putInt(id).putInt(1).array()));
        records.add(new Record(72,2,little(new byte[16]).putInt(2).array()));
        for(String value:List.of("대상 소상공인 😀","별도 문단 지원금")) {
            byte[] note=utf(value);records.add(new Record(66,2,header(note.length/2,value.startsWith("별도"))));records.add(new Record(67,3,note));
        }
        return records;
    }
    private List<Record> change(List<Record> records,int tag,int occurrence,UnaryOperator<byte[]> mutation) {
        int count=0;for(int i=0;i<records.size();i++){var record=records.get(i);if(record.tag()==tag && count++==occurrence){records.set(i,new Record(tag,record.level(),mutation.apply(record.data().clone())));break;}}
        return records;
    }
    private ExtractionResult select(List<Record> records) throws Exception {
        var evidence=new TextEvidence();var parser=new HwpSectionText("Section0",evidence);
        for(var record:records)parser.insertRecord(record.tag(),record.level(),record.data());parser.saveEnd();return evidence.selectResult("HWP",null);
    }
    private byte[] utf(String value){return value.getBytes(StandardCharsets.UTF_16LE);}
    private ByteBuffer little(byte[] bytes){return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);}
    private byte[] header(int units,boolean last){return little(new byte[24]).putInt(units|(last?0x80000000:0)).array();}
    private byte[] anchor(int code,int id){return little(new byte[16]).putChar((char)code).putInt(id).putLong(0).putChar((char)code).array();}
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
