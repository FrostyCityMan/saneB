package com.saneb.extractor;

import static com.saneb.extractor.ExtractionResult.HwpPartialCause.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;

/** HWP 레코드 계층과 본문 내 컨트롤 위치를 연결한다. 표의 셀 사이 의미 관계는 추정하지 않는다. */
final class HwpSectionText {
    private static final int TABLE = 0x74626c20; // MAKE4CHID('t','b','l',' '), little endian payload
    private static final int SECTION = 0x73656364, COLUMN = 0x636f6c64;
    private static final int HYPERLINK = 0x25686c6b;
    private static final int MAX_NODES = 20_000, MAX_DEPTH = 64, MAX_TABLE_POSITIONS = 20_000;
    private final String section;
    private final TextEvidence evidence;
    private final ArrayDeque<Frame> frames = new ArrayDeque<>();
    private int paragraphIndex, tableIndex, nodes, textUnits;

    HwpSectionText(String section, TextEvidence evidence) { this.section=section; this.evidence=evidence; }

    void insertRecord(int tag, int level, byte[] data) throws IOException {
        while (!frames.isEmpty() && frames.peek().level >= level) {
            Frame closed=frames.pop();
            if (frames.isEmpty()) closed.save(true);
        }
        Frame parent=frames.peek();
        if (tag==66) {
            checkNodeBudget();
            Paragraph paragraph=new Paragraph(level, ++paragraphIndex, data);
            if (parent instanceof Control control) control.insertParagraph(paragraph);
            else if (parent!=null) { parent.loose.add(paragraph); evidence.updateHwpPartial(UNATTACHED_PARAGRAPH); }
            saveFrame(paragraph);
        } else if (tag==67) {
            if (parent instanceof Paragraph paragraph) {
                if (level!=paragraph.level+1) evidence.updateHwpPartial(PARAGRAPH_LEVEL_GAP);
                paragraph.insertText(data);
            } else {
                // 기존 bare PARA_TEXT 입력도 읽되, 컨트롤 안의 소속 불명 문단은 정상 셀로 승격하지 않는다.
                checkNodeBudget();
                Paragraph paragraph=new Paragraph(level, ++paragraphIndex, null);
                paragraph.insertText(data);
                if (parent==null) paragraph.save(true);
                else { parent.loose.add(paragraph); evidence.updateHwpPartial(UNATTACHED_TEXT); }
            }
        } else if (tag==71) {
            checkNodeBudget();
            Control control=new Control(level, data);
            if (parent instanceof Paragraph paragraph) {
                if (level!=paragraph.level+1) evidence.updateHwpPartial(CONTROL_LEVEL_GAP);
                paragraph.controls.add(control);
            } else if (parent!=null) { parent.loose.add(control); evidence.updateHwpPartial(UNATTACHED_CONTROL); }
            else evidence.updateHwpPartial(UNATTACHED_CONTROL);
            saveFrame(control);
        } else if (tag==77 && parent instanceof Control control && control.id==TABLE && level==control.level+1) {
            control.insertTable(data);
        } else if (tag==72 && parent instanceof Control control && control.id==TABLE && level==control.level+1) {
            control.insertCell(data);
        } else if (tag<66 || tag>75 || tag==72) {
            // 알려지지 않은 도형/그림/수식/캡션 등의 텍스트 완전성을 주장하지 않는다.
            evidence.updateHwpPartial(UNSUPPORTED_RECORD);
        }
    }

    void saveEnd() throws IOException {
        while (!frames.isEmpty()) {
            Frame closed=frames.pop();
            if (frames.isEmpty()) closed.save(true);
        }
    }

    private void saveFrame(Frame frame) throws IOException {
        if (frames.size()>=MAX_DEPTH) throw new IOException("LIMIT_EXCEEDED");
        frames.push(frame);
    }

    private void checkNodeBudget() throws IOException { if (++nodes>MAX_NODES) throw new IOException("LIMIT_EXCEEDED"); }

    private abstract class Frame {
        final int level;
        final List<Frame> loose=new ArrayList<>();
        Frame(int level) { this.level=level; }
        abstract void save(boolean reliable) throws IOException;
        void saveLoose() throws IOException {
            if (!loose.isEmpty()) evidence.updateHwpPartial(LOOSE_STRUCTURE);
            for (Frame frame:loose) frame.save(false);
        }
    }

    private record Piece(String text, Integer controlId, int characterCode) {
        Piece(String text,Integer controlId) { this(text,controlId,0); }
    }

    private final class Paragraph extends Frame {
        final int index;
        final int headerLength, headerFlags;
        final List<Piece> pieces=new ArrayList<>();
        final List<Control> controls=new ArrayList<>();
        String cellLocation="";
        int textRecords, characterUnits;
        boolean hasContentAnchor;
        Paragraph(int level,int index,byte[] header) {
            super(level); this.index=index;
            // 거대한 미해석 헤더 payload를 문단 트리에 보관하지 않는다.
            headerLength=header==null?0:header.length; headerFlags=headerLength>=4?integer(header,0):0;
        }

        void insertText(byte[] data) throws IOException {
            if (data.length%2!=0) throw new IOException("CORRUPT");
            textUnits+=data.length/2;
            if (textUnits>TextEvidence.MAX_CHARACTERS*2) throw new IOException("LIMIT_EXCEEDED");
            characterUnits+=data.length/2;
            if (++textRecords>1) { evidence.updateHwpPartial(MULTIPLE_TEXT_RECORDS); pieces.add(new Piece("\n",null)); }
            var buffer=ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
            var text=new StringBuilder();
            while (buffer.hasRemaining()) {
                char ch=buffer.getChar();
                if (ch>=32) { text.append(ch); continue; }
                if ((ch>=1 && ch<=9) || ch==11 || ch==12 || (ch>=14 && ch<=23)) {
                    if (buffer.remaining()<14) throw new IOException("CORRUPT");
                    int id=buffer.getInt(); buffer.position(buffer.position()+8);
                    if (buffer.getChar()!=ch) throw new IOException("CORRUPT");
                    if (ch==9) text.append('\t');
                    else if (ch==4 || (ch>=5 && ch<=8) || ch==19 || ch==20) {
                        // inline은 별도 CTRL_HEADER를 요구하지 않는다. 문장 경계는 보존한다.
                        pieces.add(new Piece(text.toString(),null)); text.setLength(0);
                        pieces.add(new Piece("",id,ch)); hasContentAnchor=true;
                        if (ch!=4) evidence.updateHwpPartial(UNSUPPORTED_INLINE_CONTROL);
                    } else {
                        pieces.add(new Piece(text.toString(),null)); text.setLength(0);
                        pieces.add(new Piece(null,id,ch));
                        if (id==TABLE && ch!=11) evidence.updateHwpPartial(TABLE_ANCHOR_TYPE);
                        if (id!=SECTION && id!=COLUMN) hasContentAnchor=true;
                    }
                } else if (ch==10 || ch==13) text.append('\n');
                else if (ch==24) text.append('-');
                else text.append(' ');
                if (pieces.size()>MAX_NODES) throw new IOException("LIMIT_EXCEEDED");
            }
            pieces.add(new Piece(text.toString(),null));
        }

        boolean selectCellHeaderValid(boolean last) {
            if ((headerLength!=22 && headerLength!=24) || textRecords>1) return false;
            int count=headerFlags&0x7fffffff;
            return (headerFlags<0)==last && count>0 && (count==characterUnits || (count==1 && characterUnits==0));
        }

        @Override void save(boolean reliable) throws IOException {
            boolean fieldsValid=selectFieldsValid();
            reliable=reliable && fieldsValid;
            var remaining=new HashMap<Integer,ArrayDeque<Control>>();
            for (Control control:controls) remaining.computeIfAbsent(control.id,key->new ArrayDeque<>()).add(control);
            var text=new StringBuilder(); int segment=0;
            boolean split=hasContentAnchor || controls.stream().anyMatch(c->c.id!=SECTION && c.id!=COLUMN) || !loose.isEmpty();
            for (Piece piece:pieces) {
                if (piece.controlId()==null) { text.append(piece.text()); continue; }
                if (piece.characterCode()==4 || (piece.characterCode()>=5 && piece.characterCode()<=8)
                        || piece.characterCode()==19 || piece.characterCode()==20) {
                    saveText(text,++segment,reliable);
                    continue;
                }
                var queue=remaining.get(piece.controlId());
                Control control=queue==null?null:queue.poll();
                if ((piece.controlId()==SECTION || piece.controlId()==COLUMN) && (control==null || control.selectLayoutOnly())) {
                    if (control!=null) control.save(reliable);
                    continue;
                }
                saveText(text,split?++segment:0,reliable);
                if (control==null) evidence.updateHwpPartial(MISSING_CONTROL);
                else if (!(fieldsValid && piece.characterCode()==3 && control.selectPassiveHyperlink())) control.save(reliable);
            }
            saveText(text,split?++segment:0,reliable);
            // 앵커 누락 시 텍스트를 버리지는 않지만 위치를 추정한 정상 근거로 사용하지 않는다.
            for (Control control:controls) {
                var queue=remaining.get(control.id);
                if (queue!=null && queue.remove(control)) {
                    if (!control.selectLayoutOnly()) evidence.updateHwpPartial(UNANCHORED_CONTROL);
                    control.save(false);
                }
            }
            saveLoose();
        }

        /** 한 문단 안에서 닫힌 하이퍼링크 표시값만 지원한다. 명령 문자열은 읽거나 실행하지 않는다. */
        boolean selectFieldsValid() {
            var hyperlinks=new ArrayDeque<Control>();
            for (Control control:controls) if (control.id==HYPERLINK) hyperlinks.add(control);
            Integer open=null;
            boolean valid=true, seen=false;
            for (Piece piece:pieces) {
                if (piece.characterCode()==3) {
                    seen=true;
                    if (open!=null || piece.controlId()!=HYPERLINK) valid=false;
                    open=piece.controlId();
                    Control control=piece.controlId()==HYPERLINK?hyperlinks.poll():null;
                    if (control==null || !control.selectPassiveHyperlink()) {
                        valid=false;
                        if (control!=null) evidence.updateHwpPartial(FIELD_HEADER_INVALID);
                    }
                } else if (piece.characterCode()==4) {
                    seen=true;
                    if (open==null || piece.controlId()!=(open&0x00ffffff)) valid=false;
                    open=null;
                } else if (piece.controlId()!=null && open!=null) valid=false;
            }
            if (open!=null) valid=false;
            if (seen && !valid) evidence.updateHwpPartial(FIELD_RANGE_INVALID);
            return valid;
        }

        void saveText(StringBuilder text,int segment,boolean reliable) throws IOException {
            String locator=section+cellLocation+":paragraph:"+index+(segment>0?":segment:"+segment:"");
            evidence.insertBlock(text.toString(),locator,reliable); text.setLength(0);
        }
    }

    private final class Control extends Frame {
        final int id, tableNumber;
        final List<Paragraph> paragraphs=new ArrayList<>();
        final List<Cell> cells=new ArrayList<>();
        boolean validHeader, tableSeen, passiveHyperlink;
        final java.util.EnumSet<ExtractionResult.HwpPartialCause> invalidTableReasons = java.util.EnumSet.noneOf(ExtractionResult.HwpPartialCause.class);
        int rows, columns;
        int[] rowCellCounts;
        Cell currentCell;
        Control(int level,byte[] data) {
            super(level); id=data.length>=4?integer(data,0):0; tableNumber=id==TABLE?++tableIndex:0;
            // 개체 공통 필드는 ctrl ID 포함40바이트다. 뒤의 분할 방지/설명은 선택 영역이다.
            validHeader=id!=TABLE || data.length==40 || data.length==44 || (data.length>=46 && data.length==46+2*unsigned(data,44))
                    // 실제 고정 공고에서 검증한 빈 설명 뒤 0값2바이트 확장만 추가 지원한다.
                    || (data.length==48 && unsigned(data,44)==0 && unsigned(data,46)==0);
            if (!validHeader) invalidTableReasons.add(TABLE_CONTROL_HEADER);
            if (id==HYPERLINK && data.length>=15) {
                int expected=15+2*unsigned(data,9);
                // 명세의 길이 또는 공개 작성기에서 확인된 0 padding 4바이트만 허용한다.
                passiveHyperlink=(data.length==expected || (data.length==expected+4 && integer(data,expected)==0))
                        && integer(data,expected-4)!=0 && data[8]==0 && (integer(data,4)&~0x0000f801)==0;
            }
        }
        void invalidateTable(ExtractionResult.HwpPartialCause cause) { validHeader=false; invalidTableReasons.add(cause); }
        boolean selectLayoutOnly() { return (id==SECTION || id==COLUMN) && paragraphs.isEmpty() && loose.isEmpty(); }
        boolean selectPassiveHyperlink() { return passiveHyperlink && paragraphs.isEmpty() && loose.isEmpty(); }

        void insertParagraph(Paragraph paragraph) throws IOException {
            if (paragraph.level!=level+1) invalidateTable(TABLE_PARAGRAPH_LEVEL);
            paragraphs.add(paragraph);
            if (id==TABLE && currentCell!=null) {
                currentCell.paragraphs.add(paragraph);
                paragraph.cellLocation=":table:"+tableNumber+":cell:"+currentCell.row+":"+currentCell.column;
            } else if (id==TABLE) invalidateTable(TABLE_PARAGRAPH_WITHOUT_CELL);
        }

        void insertTable(byte[] data) throws IOException {
            if (tableSeen) { invalidateTable(TABLE_METADATA_INVALID); return; }
            tableSeen=true;
            if (data.length<20) { invalidateTable(TABLE_METADATA_INVALID); return; }
            rows=unsigned(data,4); columns=unsigned(data,6);
            if (rows==0 || columns==0) { invalidateTable(TABLE_METADATA_INVALID); return; }
            if ((long)rows*columns>MAX_TABLE_POSITIONS) throw new IOException("LIMIT_EXCEEDED");
            int base=20+2*rows;
            if (data.length<base) { invalidateTable(TABLE_METADATA_INVALID); return; }
            if (data.length!=base && (data.length<base+2 || data.length!=base+2+10*unsigned(data,base))) invalidateTable(TABLE_METADATA_INVALID);
            rowCellCounts=new int[rows];
            for (int row=0;row<rows;row++) {
                rowCellCounts[row]=unsigned(data,18+2*row);
                if (rowCellCounts[row]>columns) invalidateTable(TABLE_METADATA_INVALID);
            }
        }

        void insertCell(byte[] data) throws IOException {
            if (cells.size()>=MAX_NODES) throw new IOException("LIMIT_EXCEEDED");
            currentCell=new Cell(data); cells.add(currentCell);
            if (!tableSeen) invalidateTable(TABLE_METADATA_MISSING); // 표 앞 LIST_HEADER(캡션)는 셀로 추정하지 않는다.
        }

        boolean selectTableValid() {
            if (!validHeader) { invalidTableReasons.forEach(evidence::updateHwpPartial); return false; }
            if (!tableSeen || rowCellCounts==null) return tableFailure(TABLE_METADATA_MISSING);
            if (!loose.isEmpty()) return tableFailure(TABLE_LOOSE_STRUCTURE);
            var covered=new BitSet(rows*columns); int[] counts=new int[rows]; int paragraphCount=0, previous=-1;
            for (Cell cell:cells) {
                if (!cell.valid) return tableFailure(CELL_HEADER_INVALID);
                if (cell.row>=rows || cell.column>=columns || cell.rowSpan<1 || cell.columnSpan<1
                        || cell.row+cell.rowSpan>rows || cell.column+cell.columnSpan>columns) return tableFailure(CELL_GEOMETRY_INVALID);
                if (cell.paragraphCount!=cell.paragraphs.size()) return tableFailure(CELL_PARAGRAPH_COUNT);
                int position=cell.row*columns+cell.column;
                if (position<=previous) return tableFailure(CELL_ORDER_INVALID);
                previous=position; counts[cell.row]++; paragraphCount+=cell.paragraphs.size();
                for (int i=0;i<cell.paragraphs.size();i++)
                    if (!cell.paragraphs.get(i).selectCellHeaderValid(i==cell.paragraphs.size()-1)) return tableFailure(CELL_PARAGRAPH_HEADER);
                for (int row=cell.row;row<cell.row+cell.rowSpan;row++) {
                    int start=row*columns+cell.column, end=start+cell.columnSpan;
                    int occupied=covered.nextSetBit(start);
                    if (occupied>=0 && occupied<end) return tableFailure(CELL_OVERLAP);
                    covered.set(start,end);
                }
            }
            if (paragraphCount!=paragraphs.size()) return tableFailure(TABLE_PARAGRAPH_COUNT);
            if (covered.cardinality()!=rows*columns) return tableFailure(TABLE_COVERAGE);
            if (!java.util.Arrays.equals(counts,rowCellCounts)) return tableFailure(TABLE_ROW_COUNTS);
            return true;
        }
        boolean tableFailure(ExtractionResult.HwpPartialCause cause) { evidence.updateHwpPartial(cause); return false; }

        @Override void save(boolean reliable) throws IOException {
            boolean valid=id==TABLE?selectTableValid():selectLayoutOnly();
            if (!valid && id!=TABLE) evidence.updateHwpPartial(UNSUPPORTED_CONTROL);
            for (Paragraph paragraph:paragraphs) paragraph.save(reliable && valid);
            saveLoose();
        }
    }

    private static final class Cell {
        final List<Paragraph> paragraphs=new ArrayList<>();
        int paragraphCount, row, column, rowSpan, columnSpan;
        boolean valid;
        Cell(byte[] data) {
            if (data.length<34) return;
            // 공개 작성기/읽기 구현의 LIST_HEADER는 paragraphCount INT32 + flags UINT32다.
            paragraphCount=integer(data,0); column=unsigned(data,8); row=unsigned(data,10);
            columnSpan=unsigned(data,12); rowSpan=unsigned(data,14);
            valid=paragraphCount>0 && paragraphCount<=MAX_NODES && (data.length==34 || data.length==38
                    || (data.length==47 && data[38]==0 && allZero(data,39)));
        }
        private static boolean allZero(byte[] data,int from) { for (int i=from;i<data.length;i++) if (data[i]!=0) return false; return true; }
    }

    private static int unsigned(byte[] data,int offset) { return Short.toUnsignedInt(ByteBuffer.wrap(data,offset,2).order(ByteOrder.LITTLE_ENDIAN).getShort()); }
    private static int integer(byte[] data,int offset) { return ByteBuffer.wrap(data,offset,4).order(ByteOrder.LITTLE_ENDIAN).getInt(); }
}
