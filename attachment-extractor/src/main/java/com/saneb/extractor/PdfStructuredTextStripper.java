package com.saneb.extractor;

import java.io.*;
import java.util.*;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.cos.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

/** 페이지 출력은 항상 보존한다. 구조·글자·좌표·읽기 순서 중 하나라도 불명확하면 페이지 전체를 비신뢰로 남긴다. */
final class PdfStructuredTextStripper extends PDFTextStripper {
    record Part(PdfStructureScopes.Scope scope,String text) { }
    record Page(String text,List<Part> parts,boolean reliable,boolean partial) { }
    private final Map<Integer,PdfStructureScopes.Binding> bindings;
    private final Map<TextPosition,Integer> identifiers=new IdentityHashMap<>();
    private final Deque<Integer> marked=new ArrayDeque<>();
    private final Set<Integer> opened=new HashSet<>();
    private final Set<Integer> ordered=new HashSet<>();
    private final List<Integer> order=new ArrayList<>();
    private final List<PendingPart> parts=new ArrayList<>();
    private final StringBuilder raw=new StringBuilder();
    private final Map<PdfStructureScopes.Scope,Float> firstX=new HashMap<>();
    private PdfStructureScopes.Scope writingScope, previousScope;
    private int lastId=-1, operators, glyphCount, rawLength;
    private float lastY=-1,lastRight=-1,lastHeight=1;
    private boolean reliable=true,partial;

    PdfStructuredTextStripper(Map<Integer,PdfStructureScopes.Binding> bindings) {
        this.bindings=bindings;setSortByPosition(true);setSuppressDuplicateOverlappingText(false);
        if(bindings.isEmpty())reliable=false;
    }
    Page selectPage(PDDocument document,int pageNumber) throws IOException {
        setStartPage(pageNumber);setEndPage(pageNumber);
        if(document.getPage(pageNumber-1).getRotation()!=0)reliable=false;
        try {
            writeText(document,new Writer() {
                @Override public void write(char[] buffer,int offset,int length) throws IOException {save(new String(buffer,offset,length));}
                @Override public void flush(){ }
                @Override public void close(){ }
            });
        } catch(EvidenceLimit exception) {throw new IOException("LIMIT_EXCEEDED");}
        if(!marked.isEmpty() || !order.equals(new ArrayList<>(bindings.keySet())) || opened.size()!=bindings.size())reliable=false;
        return new Page(raw.toString(),parts.stream().map(p->new Part(p.scope,p.text.toString())).toList(),reliable&&!partial,partial);
    }
    private void save(String text) throws IOException {
        rawLength+=text.codePointCount(0,text.length());if(rawLength>TextEvidence.MAX_CHARACTERS)throw new IOException("LIMIT_EXCEEDED");
        raw.append(text);
        var scope=writingScope;
        if(scope==null) {
            if(!text.isBlank()){reliable=false;return;}
            if(parts.isEmpty())return;scope=parts.getLast().scope;
        }
        if(!parts.isEmpty() && parts.getLast().scope.equals(scope))parts.getLast().text.append(text);
        else {if(parts.size()>=20000)throw new IOException("LIMIT_EXCEEDED");parts.add(new PendingPart(scope,text));}
    }
    @Override protected void processOperator(Operator operator,List<COSBase> operands) throws IOException {
        if(++operators>200000)throw new IOException("LIMIT_EXCEEDED");
        String name=operator.getName();
        if("Do".equals(name) || "BI".equals(name)){partial=true;reliable=false;}
        if("W".equals(name) || "W*".equals(name))reliable=false;
        super.processOperator(operator,operands);
    }
    @Override public void beginMarkedContentSequence(COSName tag,COSDictionary properties) {
        if(marked.size()>=64)throw new EvidenceLimit();
        int id=marked.isEmpty()?-1:marked.peek();
        if(COSName.ARTIFACT.equals(tag) || "ReversedChars".equals(tag.getName())){reliable=false;id=-1;}
        if(properties!=null) {
            if(properties.containsKey(COSName.ACTUAL_TEXT) || properties.containsKey(COSName.ALT))reliable=false;
            var value=properties.getDictionaryObject(COSName.MCID);
            if(value!=null) {
                if(!(value instanceof COSInteger number) || number.longValue()<0 || number.longValue()>20000){reliable=false;id=-1;}
                else {id=((COSInteger)value).intValue();if(!opened.add(id) || !marked.isEmpty() && marked.peek()!=-1)reliable=false;}
            }
        }
        marked.push(id);super.beginMarkedContentSequence(tag,properties);
    }
    @Override public void endMarkedContentSequence() {
        if(marked.isEmpty())reliable=false;else marked.pop();super.endMarkedContentSequence();
    }
    @Override protected void processTextPosition(TextPosition position) {
        if(++glyphCount>TextEvidence.MAX_CHARACTERS)throw new EvidenceLimit();
        identifiers.put(position,marked.isEmpty()?-1:marked.peek());
        var rendering=getGraphicsState().getTextState().getRenderingMode();
        if(rendering.isClip() || (!rendering.isFill()&&!rendering.isStroke())
                || rendering.isFill() && getGraphicsState().getNonStrokeAlphaConstant()<1
                || rendering.isStroke() && getGraphicsState().getAlphaConstant()<1)reliable=false;
        super.processTextPosition(position);
    }
    @Override protected void writeString(String text,List<TextPosition> positions) throws IOException {
        int offset=0;
        for(var position:positions) {
            String glyph=position.getUnicode();int start=glyph==null||glyph.isEmpty()?-1:text.indexOf(glyph,offset);
            if(start<0 || !text.substring(offset,start).isBlank()) {
                reliable=false;writingScope=null;output.write(text.substring(offset));return;
            }
            int id=identifiers.getOrDefault(position,-1);var binding=bindings.get(id);
            if(binding==null)reliable=false;
            var scope=binding==null?null:binding.scope();
            validateGeometry(position,scope,id);
            writingScope=scope;output.write(text.substring(offset,start+glyph.length()));offset=start+glyph.length();
        }
        if(!text.substring(offset).isBlank())reliable=false;
        output.write(text.substring(offset));writingScope=null;
    }
    private void validateGeometry(TextPosition position,PdfStructureScopes.Scope scope,int id) {
        float x=position.getXDirAdj(),y=position.getYDirAdj(),width=position.getWidthDirAdj(),height=position.getHeightDir();
        if(scope==null || position.getDir()!=0 || !Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(width) || !Float.isFinite(height)
                || width<=0 || height<=0 || x<0 || y<0 || x+width>position.getPageWidth()+1 || y>position.getPageHeight()+1)reliable=false;
        if(lastY>=0) {
            float tolerance=Math.max(1,Math.min(height,lastHeight)*0.25f);
            boolean sameLine=Math.abs(y-lastY)<=tolerance;
            if(y<lastY-tolerance || sameLine && x<lastRight-0.25f)reliable=false;
            if(scope!=null && scope.equals(previousScope)) {
                if(sameLine) {
                    float maximumGap=Math.max(12,3*position.getWidthOfSpace());
                    if(x-lastRight>maximumGap && !(scope.tableRow() && id!=lastId))reliable=false;
                } else if(scope.tableRow() || y-lastY>Math.max(height,lastHeight)*2.2f
                        || Math.abs(x-firstX.getOrDefault(scope,x))>height*2)reliable=false;
            }
        }
        if(id!=lastId) {if(!ordered.add(id))reliable=false;order.add(id);}
        if(scope!=null)firstX.putIfAbsent(scope,x);
        lastId=id;lastY=y;lastRight=x+width;lastHeight=height;previousScope=scope;
    }
    private static final class PendingPart {
        final PdfStructureScopes.Scope scope;final StringBuilder text;
        PendingPart(PdfStructureScopes.Scope scope,String value){this.scope=scope;this.text=new StringBuilder(value);}
    }
    private static final class EvidenceLimit extends RuntimeException { }
}
