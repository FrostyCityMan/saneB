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

/** 공개 레코드 구조의 합성 입력. 실파일 재검증이나 정상 기대값 승인이 아니다. */
class HwpHeaderFooterTextTest {
    static final int HEADER=0x68656164, FOOTER=0x666f6f74;
    @TempDir Path root;
    record Record(int tag,int level,byte[] data) { }

    @ParameterizedTest @ValueSource(booleans={false,true})
    void actualOlePreservesHeaderAndFooterWithoutMergingBodyScopes(boolean compressed) throws Exception {
        for(int id:List.of(HEADER,FOOTER)) for(int page:List.of(0,1,2)) {
            var records=change(source(id),71,0,d->{little(d).putInt(4,page);return d;});
            var result=AttachmentExtractorMain.selectExtraction(file(records,compressed));
            assertEquals("COMPLETE_TEXT",result.qualityCode());
            assertEquals(List.of("공고 앞","대상 소상공인 😀","별도 문단 지원금","공고 뒤"),texts(result));
            assertTrue(result.blocks().stream().allMatch(ExtractionResult.Block::scopeReliable));
            assertTrue(result.blocks().get(1).locator().contains(id==HEADER?":header:1:":":footer:1:"));
            assertScopes(result);
        }
    }

    @Test void legacyHeaderAndExplicitHorizontalListFlagsAreSupported() throws Exception {
        for(int flags:List.of(0,8,16,32,64,80)) {
            var records=change(source(HEADER),71,0,d->java.util.Arrays.copyOf(d,8));
            records=change(records,72,0,d->{little(d).putInt(4,flags);return d;});
            assertEquals("COMPLETE_TEXT",select(records).qualityCode());
        }
        var records=change(source(HEADER),71,0,d->{little(d).putInt(8,-1);return d;});
        assertEquals("COMPLETE_TEXT",select(records).qualityCode()); // createIndex는 텍스트/페이지 계산 값이 아니다.
    }

    @ParameterizedTest @ValueSource(ints={0,4,7,9,11,13,16,18,100})
    void unknownControlLengthsRemainPartial(int size) throws Exception {
        var records=change(source(HEADER),71,0,d->java.util.Arrays.copyOf(d,size));
        var result=select(records);assertEquals("PARTIAL_TEXT",result.qualityCode());
        assertTrue(result.text().contains("대상 소상공인 😀"));assertScopes(result);
    }

    @ParameterizedTest @ValueSource(ints={0,8,14,16,18,24,33,35,100})
    void unknownListLengthsRemainPartial(int size) throws Exception {
        assertPartial(change(source(HEADER),72,0,d->java.util.Arrays.copyOf(d,size)));
    }

    @Test void unknownPageFlagsAndListFlagsCountsAndExtensionsCannotBecomeComplete() throws Exception {
        for(int flags:List.of(-1,3,4,256,Integer.MAX_VALUE))
            assertPartial(change(source(HEADER),71,0,d->{little(d).putInt(4,flags);return d;}));
        for(int flags:List.of(-1,1,2,7,24,96,128))
            assertPartial(change(source(HEADER),72,0,d->{little(d).putInt(4,flags);return d;}));
        for(int count:List.of(-1,0,1,3,20001,Integer.MAX_VALUE))
            assertPartial(change(source(HEADER),72,0,d->{little(d).putInt(0,count);return d;}));
        for(int offset=16;offset<34;offset++) {
            final int index=offset;
            assertPartial(change(source(HEADER),72,0,d->{d[index]=1;return d;}));
        }
    }

    @Test void missingDuplicateLateAndWrongDepthListsRemainPartial() throws Exception {
        var records=source(HEADER);records.remove(3);assertPartial(records);
        records=source(HEADER);records.add(4,records.get(3));assertPartial(records);
        records=source(HEADER);var list=records.remove(3);records.add(list);assertPartial(records);
        records=source(HEADER);list=records.get(3);records.set(3,new Record(72,3,list.data()));assertPartial(records);
    }

    @Test void paragraphCountsUnitsFinalMarkerAndDepthMustMatch() throws Exception {
        assertPartial(change(source(HEADER),66,1,d->{little(d).putInt(0,1);return d;}));
        assertPartial(change(source(HEADER),66,1,d->{little(d).putInt(0,little(d).getInt(0)|0x80000000);return d;}));
        assertPartial(change(source(HEADER),66,2,d->{little(d).putInt(0,little(d).getInt(0)&0x7fffffff);return d;}));
        assertPartial(change(source(HEADER),66,1,d->java.util.Arrays.copyOf(d,23)));
        var records=source(HEADER);var header=records.get(4);var text=records.get(5);
        records.set(4,new Record(66,3,header.data()));records.set(5,new Record(67,4,text.data()));assertPartial(records);
    }

    @Test void missingWrongRepeatedAndMisplacedAnchorsAreNotValid() throws Exception {
        assertPartial(change(source(HEADER),67,0,d->utf("공고 앞 공고 뒤")));
        for(int code:List.of(11,17,18,21))
            assertPartial(change(source(HEADER),67,0,d->joinUnchecked(utf("공고 앞"),anchor(code,HEADER),utf("공고 뒤"))));
        var records=source(HEADER);var control=records.get(2);records.set(2,new Record(71,2,control.data()));
        assertEquals("PARTIAL_TEXT",select(records).qualityCode());
        records=source(HEADER);records.set(1,new Record(67,1,joinUnchecked(anchor(16,HEADER),anchor(16,HEADER))));
        assertEquals("PARTIAL_TEXT",select(records).qualityCode());
        records=source(HEADER);records.remove(2);assertEquals("PARTIAL_TEXT",select(records).qualityCode());
    }

    @Test void unknownRecordsAndOutOfParagraphMetadataAreNotIgnored() throws Exception {
        for(int tag:List.of(68,69,70,73,74,75,76,85)) {
            var records=source(HEADER);records.add(4,new Record(tag,2,new byte[4]));assertPartial(records);
        }
    }

    @Test void repeatedHeaderFooterControlsKeepUniqueLocationsAndStableOrder() throws Exception {
        var records=source(HEADER);
        records.set(1,new Record(67,1,joinUnchecked(utf("앞"),anchor(16,HEADER),utf("중간"),anchor(16,FOOTER),utf("뒤"))));
        records.addAll(source(FOOTER).subList(2,8));
        var result=select(records);assertEquals("COMPLETE_TEXT",result.qualityCode());
        assertEquals(List.of("앞","대상 소상공인 😀","별도 문단 지원금","중간","대상 소상공인 😀","별도 문단 지원금","뒤"),texts(result));
        assertTrue(result.blocks().get(4).locator().contains(":footer:2:"));assertScopes(result);
    }

    @Test void emptyParagraphIsSupportedButUnrecordedTextIsNeverInvented() throws Exception {
        var records=source(HEADER);records=change(records,72,0,d->{little(d).putInt(0,1);return d;});
        records.subList(4,8).clear();records.add(new Record(66,2,paragraphHeader(1,true)));
        var result=select(records);assertEquals("COMPLETE_TEXT",result.qualityCode());
        assertEquals(List.of("공고 앞","공고 뒤"),texts(result));assertScopes(result);
    }

    @Test void malformedNestedNumberPreservesTextButCannotPromoteDocument() throws Exception {
        var records=source(HEADER);byte[] text=joinUnchecked(utf("별도 문단 지원금"),anchor(18,0x61746e6f));
        records.set(6,new Record(66,2,paragraphHeader(text.length/2,true)));
        records.set(7,new Record(67,3,text));records.add(new Record(71,3,little(new byte[16]).putInt(0x61746e6f).array()));
        var result=select(records);assertEquals("PARTIAL_TEXT",result.qualityCode());
        assertTrue(result.text().contains("별도 문단 지원금"));assertScopes(result);
    }

    private void assertPartial(List<Record> records) throws Exception {
        var result=select(records);assertEquals("PARTIAL_TEXT",result.qualityCode());
        assertTrue(result.text().contains("대상 소상공인 😀"));assertTrue(result.text().contains("별도 문단 지원금"));
        assertTrue(result.blocks().stream().filter(b->b.locator().contains(":header:")).noneMatch(ExtractionResult.Block::scopeReliable));
        assertScopes(result);
    }
    private void assertScopes(ExtractionResult result) {
        assertEquals(result.blocks().size(),result.blocks().stream().map(ExtractionResult.Block::evidenceScopeId).distinct().count());
        int offset=0;for(var block:result.blocks()) {assertEquals(offset,block.startOffset());assertEquals(block.locator(),block.evidenceScopeId());offset=block.endOffset()+1;}
        assertEquals(result.text().codePointCount(0,result.text().length()),offset-1);
    }
    private List<String> texts(ExtractionResult result) {return result.blocks().stream().map(b->result.text().substring(result.text().offsetByCodePoints(0,b.startOffset()),result.text().offsetByCodePoints(0,b.endOffset()))).toList();}
    private List<Record> source(int id) {
        byte[] text=joinUnchecked(utf("공고 앞"),anchor(16,id),utf("공고 뒤"));
        var records=new ArrayList<Record>();records.add(new Record(66,0,paragraphHeader(text.length/2,true)));records.add(new Record(67,1,text));
        records.add(new Record(71,1,little(new byte[12]).putInt(id).putInt(0).putInt(1).array()));
        records.add(new Record(72,2,little(new byte[34]).putInt(2).putInt(0).putInt(10000).putInt(2000).array()));
        for(String value:List.of("대상 소상공인 😀","별도 문단 지원금")) {
            byte[] note=utf(value);records.add(new Record(66,2,paragraphHeader(note.length/2,value.startsWith("별도"))));records.add(new Record(67,3,note));
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
    private byte[] paragraphHeader(int units,boolean last){return little(new byte[24]).putInt(units|(last?0x80000000:0)).array();}
    private byte[] anchor(int code,int id){return little(new byte[16]).putChar((char)code).putInt(id).putLong(0).putChar((char)code).array();}
    private byte[] joinUnchecked(byte[]... parts){var bytes=new ByteArrayOutputStream();for(var part:parts)bytes.writeBytes(part);return bytes.toByteArray();}
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
