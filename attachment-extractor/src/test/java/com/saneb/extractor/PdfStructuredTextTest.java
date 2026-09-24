package com.saneb.extractor;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import org.apache.pdfbox.cos.*;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.*;
import org.apache.pdfbox.pdmodel.documentinterchange.markedcontent.PDPropertyList;
import org.apache.pdfbox.pdmodel.font.*;
import org.apache.pdfbox.pdmodel.graphics.state.RenderingMode;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** 실제 생성 PDF를 파싱한다. 합성 표본이며 외부 지자체 파일 검증을 대신하지 않는다. */
class PdfStructuredTextTest {
    @TempDir Path root;
    @Test void separateTaggedParagraphsKeepIndependentScopesAndCompleteText() throws Exception {
        try(var fixture=new Fixture()) {
            fixture.paragraph("Target: small business",700);fixture.paragraph("Support: grant",650);
            var result=extract(fixture);assertEquals("COMPLETE_TEXT",result.qualityCode());
            assertEquals("Target: small business\nSupport: grant",result.text());assertEquals(2,result.blocks().size());
            assertTrue(result.blocks().stream().allMatch(ExtractionResult.Block::scopeReliable));
            assertEquals(2,result.blocks().stream().map(ExtractionResult.Block::evidenceScopeId).distinct().count());validateOffsets(result);
            render(fixture,"paragraphs");
        }
    }
    @Test void tableCellsShareOnlyTheirValidatedRowNeverTheWholeTable() throws Exception {
        try(var fixture=new Fixture()) {
            fixture.row("Target: small business","Support: grant",700);
            fixture.row("Target: farmers","Support: loan",650);
            var result=extract(fixture);assertEquals("COMPLETE_TEXT",result.qualityCode());assertEquals(2,result.blocks().size());
            assertTrue(result.blocks().stream().allMatch(ExtractionResult.Block::scopeReliable));
            assertNotEquals(result.blocks().get(0).evidenceScopeId(),result.blocks().get(1).evidenceScopeId());
            assertTrue(result.blocks().getFirst().locator().contains(":row:"));assertTrue(result.text().contains("Support: grant"));validateOffsets(result);
            render(fixture,"table-rows");
        }
    }
    @ParameterizedTest @ValueSource(strings={"noTags","noMarkedFlag","noParentTree","parentMismatch","duplicateReference","missingReference",
            "unmarkedText","reusedMcid","unknownRole","wrongPage","readingOrder","actualText","invisible","overlap","largeParagraphGap","mergedCell",
            "overflowParentKey","negativeParentKey"})
    void ambiguousOrContradictoryStructurePreservesTextWithoutReliableScopes(String scenario) throws Exception {
        try(var fixture=new Fixture()) {
            var first=fixture.paragraph("Target: small business",700);
            switch(scenario) {
                case "noTags"->fixture.pdf.getDocumentCatalog().setStructureTreeRoot(null);
                case "noMarkedFlag"->fixture.pdf.getDocumentCatalog().getMarkInfo().setMarked(false);
                case "noParentTree"->fixture.tree.getCOSObject().removeItem(COSName.PARENT_TREE);
                case "parentMismatch"->fixture.parentArray.set(0,new COSDictionary());
                case "overflowParentKey"->fixture.page.getCOSObject().setItem(COSName.STRUCT_PARENTS,COSInteger.get(4294967296L));
                case "negativeParentKey"->fixture.page.getCOSObject().setInt(COSName.STRUCT_PARENTS,-1);
                case "duplicateReference"->first.appendKid(0);
                case "missingReference"->first.appendKid(1);
                case "unmarkedText"->fixture.text(null,"Support: grant",40,650,false);
                case "reusedMcid"->fixture.text(0,"Support: grant",40,650,false);
                case "unknownRole"->first.setStructureType("UnsupportedCustomRole");
                case "wrongPage"->{var page=new PDPage();fixture.pdf.addPage(page);first.setPage(page);}
                case "readingOrder"->{var second=fixture.paragraph("Support: grant",650);fixture.document.setKids(List.of(second,first));}
                case "actualText"->first.getCOSObject().setString(COSName.ACTUAL_TEXT,"hidden replacement");
                case "invisible"->{var item=fixture.element("P",fixture.document);int id=fixture.bind(item);fixture.text(id,"Support: grant",40,650,true);}
                case "overlap"->fixture.paragraph("Support: grant",700);
                case "largeParagraphGap"->{int id=fixture.bind(first);fixture.text(id,"Support: grant",40,550,false);}
                case "mergedCell"->{var cell=fixture.row("Target: farmers","Support: loan",600);var attributes=new COSDictionary();attributes.setInt(COSName.getPDFName("ColSpan"),2);cell.getCOSObject().setItem(COSName.A,attributes);}
                default->throw new AssertionError();
            }
            var result=extract(fixture);var baseline=new org.apache.pdfbox.text.PDFTextStripper();baseline.setSortByPosition(true);baseline.setSuppressDuplicateOverlappingText(false);
            assertEquals(baseline.getText(fixture.pdf).strip(),result.text(),scenario);
            assertTrue(result.blocks().stream().noneMatch(ExtractionResult.Block::scopeReliable),scenario);validateOffsets(result);
        }
    }
    @Test void cyclicStructureAndParentTreesFailClosedWithoutRecursionOverflow() throws Exception {
        try(var fixture=new Fixture()) {
            var first=fixture.paragraph("Target: small business",700);
            first.getCOSObject().setItem(COSName.K,first.getCOSObject());assertNull(PdfStructureScopes.select(fixture.pdf));
        }
        try(var fixture=new Fixture()) {
            fixture.paragraph("Target: small business",700);var parent=new COSDictionary();var children=new COSArray();children.add(parent);parent.setItem(COSName.KIDS,children);
            fixture.tree.getCOSObject().setItem(COSName.PARENT_TREE,parent);assertNull(PdfStructureScopes.select(fixture.pdf));
        }
    }
    @Test void markedContentReferenceMustBindSameOwnerAndPage() throws Exception {
        try(var fixture=new Fixture()) {
            var first=fixture.paragraph("Target: small business",700);var reference=new COSDictionary();reference.setName(COSName.TYPE,"MCR");
            reference.setInt(COSName.MCID,0);reference.setItem(COSName.PG,fixture.page);first.getCOSObject().setItem(COSName.K,reference);
            assertTrue(extract(fixture).blocks().getFirst().scopeReliable());
            reference.setItem(COSName.getPDFName("Stm"),new COSDictionary());assertFalse(extract(fixture).blocks().getFirst().scopeReliable());
        }
    }
    private ExtractionResult extract(Fixture fixture) throws Exception {
        var file=root.resolve(UUID.randomUUID()+".pdf");fixture.pdf.save(file.toFile());return AttachmentExtractorMain.selectExtraction(file);
    }
    private void validateOffsets(ExtractionResult result) {
        int offset=0;for(var block:result.blocks()){assertEquals(offset,block.startOffset());assertTrue(block.endOffset()>offset);offset=block.endOffset()+1;}
        assertEquals(result.text().codePointCount(0,result.text().length()),offset-1);
    }
    private void render(Fixture fixture,String name) throws Exception {
        var directory=Files.createDirectories(Path.of("build","reports","pdf-structure-qa"));
        fixture.pdf.save(directory.resolve(name+".pdf").toFile());
        assertTrue(ImageIO.write(new PDFRenderer(fixture.pdf).renderImageWithDPI(0,96),"png",directory.resolve(name+".png").toFile()));
    }
    private static final class Fixture implements AutoCloseable {
        final PDDocument pdf=new PDDocument();final PDPage page=new PDPage();final PDStructureTreeRoot tree=new PDStructureTreeRoot();
        final PDStructureElement document=new PDStructureElement("Document",tree);final COSArray parentArray=new COSArray();int nextId;PDStructureElement table;
        Fixture() {
            pdf.addPage(page);page.getCOSObject().setInt(COSName.STRUCT_PARENTS,0);tree.appendKid(document);
            pdf.getDocumentCatalog().setStructureTreeRoot(tree);var marked=new PDMarkInfo();marked.setMarked(true);pdf.getDocumentCatalog().setMarkInfo(marked);
            var nums=new COSArray();nums.add(COSInteger.ZERO);nums.add(parentArray);var parent=new COSDictionary();parent.setItem(COSName.NUMS,nums);
            tree.getCOSObject().setItem(COSName.PARENT_TREE,parent);
        }
        PDStructureElement element(String type,PDStructureElement parent) {
            var node=new PDStructureElement(type,parent);node.setPage(page);parent.appendKid(node);return node;
        }
        int bind(PDStructureElement owner){int id=nextId++;owner.appendKid(id);parentArray.add(owner);return id;}
        PDStructureElement paragraph(String text,float y) throws Exception {
            var node=element("P",document);text(bind(node),text,40,y,false);return node;
        }
        PDStructureElement row(String left,String right,float y) throws Exception {
            if(table==null)table=element("Table",document);var row=element("TR",table);var cell=element("TD",row);
            text(bind(cell),left,40,y,false);var second=element("TD",row);text(bind(second),right,300,y,false);return cell;
        }
        void text(Integer id,String text,float x,float y,boolean invisible) throws Exception {
            try(var content=new PDPageContentStream(pdf,page,PDPageContentStream.AppendMode.APPEND,true,true)) {
                if(id!=null){var properties=new COSDictionary();properties.setInt(COSName.MCID,id);content.beginMarkedContent(COSName.P,PDPropertyList.create(properties));}
                content.beginText();content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA),12);
                if(invisible)content.setRenderingMode(RenderingMode.NEITHER);content.newLineAtOffset(x,y);content.showText(text);content.endText();
                if(id!=null)content.endMarkedContent();
            }
        }
        @Override public void close() throws Exception {pdf.close();}
    }
}
