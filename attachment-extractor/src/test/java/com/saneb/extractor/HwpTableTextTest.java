package com.saneb.extractor;

import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** 실제 HWP 레코드 깊이와 inline 앵커를 갖는 신뢰된 합성 표. 외부 파일 실증과 별도다. */
class HwpTableTextTest {
    static final int TABLE=0x74626c20;
    @TempDir Path root;
    record Paragraph(List<Object> parts) { }
    record Cell(int row,int column,int rowSpan,int columnSpan,List<Paragraph> paragraphs) { }
    record Table(int rows,int columns,List<Cell> cells) { }

    @ParameterizedTest @ValueSource(booleans={false,true})
    void realOleAndCompressedSectionsKeepBeforeCellsAfterAndDistinctScopes(boolean compressed) throws Exception {
        byte[] body=body(paragraph("공고 앞 😀",table(1,2,cell(0,0,"소상공인"),cell(0,1,"지원금")),"공고 뒤"));
        var result=AttachmentExtractorMain.selectExtraction(file(body,compressed));
        assertComplete(result,"공고 앞 😀","소상공인","지원금","공고 뒤");
        assertTrue(result.blocks().get(1).locator().contains(":table:1:cell:0:0:"));
        assertTrue(result.blocks().get(2).locator().contains(":table:1:cell:0:1:"));
        assertTrue(result.blocks().getFirst().locator().endsWith(":segment:1"));
        assertEquals(result.text(),select(body).text());
        assertEquals(1,result.hwpStructure().recordTypes().stream().filter(r->r.tagId()==77).findFirst().orElseThrow().count());
    }

    @Test void cellParagraphsAreNotMergedIntoAnAndScope() throws Exception {
        var cell=new Cell(0,0,1,1,List.of(paragraph("대상 소상공인"),paragraph("별도 문단 지원금")));
        assertComplete(select(body(paragraph(table(1,1,cell)))),"대상 소상공인","별도 문단 지원금");
    }

    @Test void nestedTableKeepsAllThreeLevelsInAnchorOrder() throws Exception {
        var inner=table(1,2,cell(0,0,"중첩 첫 셀"),cell(0,1,"중첩 둘째 셀"));
        var outer=table(1,1,new Cell(0,0,1,1,List.of(paragraph("셀 앞",inner,"셀 뒤"))));
        assertComplete(select(body(paragraph("본문 앞",outer,"본문 뒤"))),"본문 앞","셀 앞","중첩 첫 셀","중첩 둘째 셀","셀 뒤","본문 뒤");
    }

    @Test void repeatedTableControlIdsMatchTheirRespectiveAnchorsInOrder() throws Exception {
        assertComplete(select(body(paragraph("앞",table(1,1,cell(0,0,"표 하나")),"중간",
                table(1,1,cell(0,0,"표 둘")),"뒤"))),"앞","표 하나","중간","표 둘","뒤");
    }

    @Test void rectangularMergedCellsAreExtractedOnceWithoutInventedAdjacentContext() throws Exception {
        var first=new Cell(0,0,2,1,List.of(paragraph("세로 병합")));
        assertComplete(select(body(paragraph(table(2,2,first,cell(0,1,"오른쪽 위"),cell(1,1,"오른쪽 아래"))))),
                "세로 병합","오른쪽 위","오른쪽 아래");
        var horizontal=new Cell(0,0,1,2,List.of(paragraph("가로 병합")));
        assertComplete(select(body(paragraph(table(1,2,horizontal)))),"가로 병합");
    }

    @ParameterizedTest @ValueSource(ints={34,38,47})
    void knownCellHeaderLengthsUseInt32ParagraphCountAndEightByteBase(int length) throws Exception {
        byte[] body=replace(body(paragraph(table(1,1,cell(0,0,"지원 내용")))),72,0,data->java.util.Arrays.copyOf(data,length));
        assertComplete(select(body),"지원 내용");
    }

    @Test void blankCellIsARealCellNotAnAbsentFileOrCompleteEmptyText() throws Exception {
        assertEquals("OCR_REQUIRED",select(body(paragraph(table(1,1,cell(0,0,""))))).qualityCode());
    }

    @Test void missingControlCannotJoinTextAcrossTheUnresolvedAnchor() throws Exception {
        byte[] text=join("대상 소상공인".getBytes(StandardCharsets.UTF_16LE),anchor(),"지원금".getBytes(StandardCharsets.UTF_16LE));
        var result=select(join(record(66,0,header(text.length/2,true)),record(67,1,text)));
        assertEquals("PARTIAL_TEXT",result.qualityCode());
        assertEquals(List.of("대상 소상공인","지원금"),texts(result));assertScopes(result);
    }

    @Test void missingAnchorRetainsCellTextButDoesNotAssertItsPosition() throws Exception {
        byte[] data=replace(body(paragraph("본문",table(1,1,cell(0,0,"셀 텍스트")))),67,0,
                ignored->"본문".getBytes(StandardCharsets.UTF_16LE));
        var result=select(data);
        assertEquals("PARTIAL_TEXT",result.qualityCode());assertTrue(result.text().contains("셀 텍스트"));
        assertFalse(result.blocks().getLast().scopeReliable());
    }

    @ParameterizedTest @ValueSource(ints={0,-1,2,Integer.MAX_VALUE})
    void invalidCellParagraphCountNeverBecomesComplete(int count) throws Exception {
        var result=select(mutate(body(paragraph(table(1,1,cell(0,0,"보존할 셀")))),72,0,b->b.putInt(0,count)));
        assertEquals("PARTIAL_TEXT",result.qualityCode());assertEquals("보존할 셀",result.text());
        assertFalse(result.blocks().getFirst().scopeReliable());
    }

    @Test void paragraphCountAndLastInListFlagMustBothMatch() throws Exception {
        byte[] source=body(paragraph(table(1,1,cell(0,0,"내용"))));
        assertEquals("PARTIAL_TEXT",select(mutate(source,66,1,b->b.putInt(0,3))).qualityCode());
        assertEquals("PARTIAL_TEXT",select(mutate(source,66,1,b->b.putInt(0,0x80000064))).qualityCode());
    }

    @Test void overlapHolesAndOutOfOrderCellsNeverBecomeComplete() throws Exception {
        for (Table table:List.of(table(1,2,cell(0,0,"하나"),cell(0,0,"중복")),
                table(1,2,cell(0,0,"빠진 셀")),table(1,2,cell(0,1,"역순 앞"),cell(0,0,"역순 뒤")))) {
            var result=select(body(paragraph(table)));assertEquals("PARTIAL_TEXT",result.qualityCode());
            assertTrue(result.blocks().stream().noneMatch(ExtractionResult.Block::scopeReliable));
        }
    }

    @ParameterizedTest @ValueSource(ints={0,3,65535})
    void invalidOrOutOfRangeSpanDoesNotBecomeComplete(int span) throws Exception {
        assertEquals("PARTIAL_TEXT",select(mutate(body(paragraph(table(1,2,cell(0,0,"첫째"),cell(0,1,"둘째")))),
                72,0,b->b.putShort(12,(short)span))).qualityCode());
    }

    @Test void mismatchedDeclaredRowCountsAndDuplicateTableMetadataArePartial() throws Exception {
        byte[] source=body(paragraph(table(1,1,cell(0,0,"내용"))));
        assertEquals("PARTIAL_TEXT",select(mutate(source,77,0,b->b.putShort(18,(short)0))).qualityCode());
        byte[] duplicated=replace(source,77,0,data->data); // 원문 record 앞에 같은 TABLE record를 한 번 더 삽입한다.
        var records=readRecords(duplicated);var out=new ByteArrayOutputStream();
        for(byte[] record:records) { out.write(record);if((little(record).getInt(0)&1023)==77)out.write(record); }
        assertEquals("PARTIAL_TEXT",select(out.toByteArray()).qualityCode());
    }

    @Test void unknownCellExtensionIsNotSilentlyIgnored() throws Exception {
        assertEquals("PARTIAL_TEXT",select(mutate(body(paragraph(table(1,1,cell(0,0,"내용")))),72,0,
                b->b.put(38,(byte)0xff))).qualityCode());
    }

    @Test void pictureEquationAndFutureRecordsStillInvalidateAnOtherwiseValidTable() throws Exception {
        for(int tag:new int[]{76,85,88,95,115,1023}) {
            byte[] source=body(paragraph(table(1,1,cell(0,0,"표 내용"))));
            assertEquals("PARTIAL_TEXT",select(join(source,record(tag,3,new byte[8]))).qualityCode());
        }
    }

    @Test void malformedTableControlHeaderDoesNotPromoteTheTable() throws Exception {
        byte[] source=body(paragraph(table(1,1,cell(0,0,"내용"))));
        assertEquals("PARTIAL_TEXT",select(replace(source,71,0,data->java.util.Arrays.copyOf(data,4))).qualityCode());
    }

    @Test void malformedAnchorLengthOrClosingCharacterIsCorrupt() throws Exception {
        assertEquals("CORRUPT",assertThrows(IOException.class,()->select(record(67,0,new byte[]{11,0,1,0}))).getMessage());
        byte[] anchor=anchor();anchor[14]=12;
        assertEquals("CORRUPT",assertThrows(IOException.class,()->select(record(67,0,anchor))).getMessage());
    }

    @Test void tableIdInsideALayoutControlCharacterIsNotAValidTableAnchor() throws Exception {
        byte[] source=mutate(body(paragraph(table(1,1,cell(0,0,"내용")))),67,0,b->{b.putShort(0,(short)2);b.putShort(14,(short)2);});
        assertEquals("PARTIAL_TEXT",select(source).qualityCode());
    }

    @Test void emptyParagraphNodesCannotExhaustTheHeapBeforeTextEvidenceHasAnyBlocks() throws Exception {
        var body=new ByteArrayOutputStream();
        for(int i=0;i<20_001;i++)body.write(record(66,0,header(1,true)));
        assertEquals("LIMIT_EXCEEDED",assertThrows(IOException.class,()->select(body.toByteArray())).getMessage());
        body.reset();body.write(record(71,0,little(new byte[4]).putInt(0x73656364).array()));
        for(int i=0;i<20_001;i++)body.write(record(67,1,new byte[0]));
        assertEquals("LIMIT_EXCEEDED",assertThrows(IOException.class,()->select(body.toByteArray())).getMessage());
    }

    @Test void tablePositionAndNestedFrameBudgetsFailWithBoundedError() throws Exception {
        byte[] source=mutate(body(paragraph(table(1,1,cell(0,0,"내용")))),77,0,b->{b.putShort(4,(short)65535);b.putShort(6,(short)65535);});
        assertEquals("LIMIT_EXCEEDED",assertThrows(IOException.class,()->select(source)).getMessage());
        Paragraph paragraph=paragraph("끝");
        for(int i=0;i<40;i++)paragraph=paragraph(table(1,1,new Cell(0,0,1,1,List.of(paragraph))));
        byte[] nested=body(paragraph);
        assertEquals("LIMIT_EXCEEDED",assertThrows(IOException.class,()->select(nested)).getMessage());
    }

    @Test void layoutControlsDoNotSplitAContiguousParagraph() throws Exception {
        byte[] marker=anchor();little(marker).putInt(2,0x73656364);marker[0]=2;marker[14]=2;
        byte[] text=join("앞".getBytes(StandardCharsets.UTF_16LE),marker,"뒤".getBytes(StandardCharsets.UTF_16LE));
        byte[] ctrl=new byte[24];little(ctrl).putInt(0,0x73656364);
        assertComplete(select(join(record(66,0,header(text.length/2,true)),record(67,1,text),record(71,1,ctrl),record(73,2,new byte[40]))),"앞뒤");
    }

    private void assertComplete(ExtractionResult result,String... expected) {
        assertEquals("COMPLETE_TEXT",result.qualityCode());assertEquals(List.of(expected),texts(result));
        assertTrue(result.hwpPartialCauses().isEmpty());
        assertTrue(result.blocks().stream().allMatch(ExtractionResult.Block::scopeReliable));assertScopes(result);
    }
    @Test void partialCausesDistinguishCellControlAndMetadataFailuresWithoutRawText() throws Exception {
        byte[] source=body(paragraph(table(1,1,cell(0,0,"PRIVATE_CANARY"))));
        assertCause(select(replace(source,71,0,data->java.util.Arrays.copyOf(data,4))),"TABLE_CONTROL_HEADER");
        assertCause(select(mutate(source,72,0,b->b.put(38,(byte)1))),"CELL_HEADER_INVALID");
        assertCause(select(mutate(source,72,0,b->b.putShort(12,(short)0))),"CELL_GEOMETRY_INVALID");
        assertCause(select(mutate(source,72,0,b->b.putInt(0,2))),"CELL_PARAGRAPH_COUNT");
        assertCause(select(mutate(source,66,1,b->b.putInt(0,2))),"CELL_PARAGRAPH_HEADER");
        assertCause(select(mutate(source,77,0,b->b.putShort(18,(short)0))),"TABLE_ROW_COUNTS");
        assertCause(select(join(source,record(87,1,new byte[4]))),"UNSUPPORTED_RECORD");
        assertCause(select(body(paragraph("PRIVATE_CANARY\ufffd"))),"REPLACEMENT_CHARACTER");
    }
    private void assertCause(ExtractionResult result,String code) throws Exception {
        assertEquals("PARTIAL_TEXT",result.qualityCode());
        assertTrue(result.hwpPartialCauses().stream().anyMatch(c->c.code().name().equals(code)&&c.count()>0));
        var json=new com.fasterxml.jackson.databind.ObjectMapper().valueToTree(result.hwpPartialCauses());
        assertFalse(json.toString().contains("PRIVATE_CANARY"));
        assertTrue(result.text().contains("PRIVATE_CANARY"));
    }
    private void assertScopes(ExtractionResult result) {
        assertEquals(result.blocks().size(),result.blocks().stream().map(ExtractionResult.Block::evidenceScopeId).distinct().count());
        int offset=0;
        for(var block:result.blocks()) {
            assertEquals(offset,block.startOffset());assertEquals(block.locator(),block.evidenceScopeId());
            assertTrue(block.locator().length()<300);offset=block.endOffset()+1;
        }
        assertEquals(result.text().codePointCount(0,result.text().length()),offset-1);
    }
    private List<String> texts(ExtractionResult result) {
        return result.blocks().stream().map(b->result.text().substring(result.text().offsetByCodePoints(0,b.startOffset()),
                result.text().offsetByCodePoints(0,b.endOffset()))).toList();
    }
    private Paragraph paragraph(Object...parts){return new Paragraph(List.of(parts));}
    private Cell cell(int row,int column,String text){return new Cell(row,column,1,1,List.of(paragraph(text)));}
    private Table table(int rows,int columns,Cell...cells){return new Table(rows,columns,List.of(cells));}
    private ByteBuffer little(byte[] bytes){return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);}
    private byte[] header(int count,boolean last){byte[] bytes=new byte[24];little(bytes).putInt(count|(last?0x80000000:0));return bytes;}
    private byte[] anchor(){return ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN).putShort((short)11).putInt(TABLE).putLong(0).putShort((short)11).array();}
    private byte[] body(Paragraph paragraph)throws Exception{var out=new ByteArrayOutputStream();writeParagraph(out,paragraph,0,true);return out.toByteArray();}
    private void writeParagraph(ByteArrayOutputStream out,Paragraph paragraph,int level,boolean last)throws Exception {
        var text=new ByteArrayOutputStream();
        for(Object part:paragraph.parts())text.write(part instanceof Table?anchor():part.toString().getBytes(StandardCharsets.UTF_16LE));
        text.write(new byte[]{13,0});
        out.write(record(66,level,header(text.size()/2,last)));out.write(record(67,level+1,text.toByteArray()));
        for(Object part:paragraph.parts())if(part instanceof Table table)writeTable(out,table,level+1);
    }
    private void writeTable(ByteArrayOutputStream out,Table table,int level)throws Exception {
        byte[] ctrl=new byte[46];little(ctrl).putInt(TABLE);out.write(record(71,level,ctrl));
        byte[] metadata=new byte[22+2*table.rows()];var buffer=little(metadata);buffer.putShort(4,(short)table.rows()).putShort(6,(short)table.columns());
        for(int row=0;row<table.rows();row++){int index=row;buffer.putShort(18+2*row,(short)table.cells().stream().filter(c->c.row()==index).count());}
        out.write(record(77,level+1,metadata));
        for(Cell cell:table.cells()) {
            byte[] list=new byte[47];little(list).putInt(cell.paragraphs().size()).putInt(0).putShort((short)cell.column()).putShort((short)cell.row())
                    .putShort((short)cell.columnSpan()).putShort((short)cell.rowSpan()).putInt(5000).putInt(3000);
            out.write(record(72,level+1,list));
            for(int i=0;i<cell.paragraphs().size();i++)writeParagraph(out,cell.paragraphs().get(i),level+1,i==cell.paragraphs().size()-1);
        }
    }
    private byte[] record(int tag,int level,byte[] data)throws IOException {
        var out=new ByteArrayOutputStream();out.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(tag|(level<<10)|(Math.min(data.length,4095)<<20)).array());
        if(data.length>=4095)out.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(data.length).array());out.write(data);return out.toByteArray();
    }
    private byte[] join(byte[]...values)throws IOException{var out=new ByteArrayOutputStream();for(byte[] value:values)out.write(value);return out.toByteArray();}
    private List<byte[]> readRecords(byte[] body) {
        List<byte[]> records=new ArrayList<>();var data=little(body);
        while(data.hasRemaining()){int start=data.position(),record=data.getInt(),size=record>>>20;if(size==4095)size=data.getInt();data.position(data.position()+size);records.add(java.util.Arrays.copyOfRange(body,start,data.position()));}
        return records;
    }
    private byte[] mutate(byte[] body,int tag,int occurrence,Consumer<ByteBuffer> mutation)throws Exception {
        return replace(body,tag,occurrence,data->{mutation.accept(little(data));return data;});
    }
    private byte[] replace(byte[] body,int tag,int occurrence,java.util.function.UnaryOperator<byte[]> mutation)throws Exception {
        var out=new ByteArrayOutputStream();int found=0;
        for(byte[] bytes:readRecords(body)) {
            int header=little(bytes).getInt(),length=header>>>20,offset=length==4095?8:4;
            if((header&1023)==tag && found++==occurrence)out.write(record(tag,(header>>>10)&1023,mutation.apply(java.util.Arrays.copyOfRange(bytes,offset,bytes.length))));
            else out.write(bytes);
        }
        return out.toByteArray();
    }
    private ExtractionResult select(byte[] body)throws IOException {
        var evidence=new TextEvidence();var parser=new HwpSectionText("Section0",evidence);
        for(byte[] bytes:readRecords(body)){int header=little(bytes).getInt();parser.insertRecord(header&1023,(header>>>10)&1023,java.util.Arrays.copyOfRange(bytes,(header>>>20)==4095?8:4,bytes.length));}
        parser.saveEnd();return evidence.selectResult("HWP",null);
    }
    private Path file(byte[] body,boolean compressed)throws Exception {
        byte[] header=new byte[256];System.arraycopy("HWP Document File".getBytes(StandardCharsets.US_ASCII),0,header,0,17);
        header[35]=5;header[33]=3;little(header).putInt(36,compressed?1:0);
        if(compressed){var out=new ByteArrayOutputStream();var deflater=new Deflater(Deflater.DEFAULT_COMPRESSION,true);try(var zip=new DeflaterOutputStream(out,deflater)){zip.write(body);}finally{deflater.end();}body=out.toByteArray();}
        Path target=root.resolve(java.util.UUID.randomUUID()+".hwp");
        try(var ole=new POIFSFileSystem()) {
            ole.getRoot().createDocument("FileHeader",new ByteArrayInputStream(header));
            ole.getRoot().createDirectory("BodyText").createDocument("Section0",new ByteArrayInputStream(body));
            try(var output=Files.newOutputStream(target)){ole.writeFilesystem(output);}
        }
        return target;
    }
}
