package com.saneb.extractor;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AttachmentTextOrderAndPdfContentTest {
    @TempDir Path root;

    @Test void unusedImageResourceDoesNotMakeExtractedTextPartial() throws Exception {
        try (var pdf = new PDDocument()) {
            PDPage page = new PDPage(); pdf.addPage(page);
            page.setResources(new PDResources());
            page.getResources().add(LosslessFactory.createFromImage(pdf, new BufferedImage(1,1,BufferedImage.TYPE_INT_RGB)));
            insertText(pdf,page);
            assertEquals("COMPLETE_TEXT",selectPdf(pdf).qualityCode());
        }
    }

    @Test void paintedImageStillRequiresReviewEvenWithText() throws Exception {
        try (var pdf = new PDDocument()) {
            PDPage page = new PDPage(); pdf.addPage(page); insertText(pdf,page);
            try (var content = new PDPageContentStream(pdf,page,PDPageContentStream.AppendMode.APPEND,true)) {
                content.drawImage(LosslessFactory.createFromImage(pdf,new BufferedImage(1,1,BufferedImage.TYPE_INT_RGB)),20,20);
            }
            assertEquals("PARTIAL_TEXT",selectPdf(pdf).qualityCode());
        }
    }

    @Test void inlineImageIsNotMissedBecauseItHasNoXObjectResource() throws Exception {
        try (var pdf = new PDDocument()) {
            PDPage page = new PDPage(); pdf.addPage(page); insertText(pdf,page);
            var stream = new org.apache.pdfbox.pdmodel.common.PDStream(pdf,
                    new java.io.ByteArrayInputStream("BI /W 1 /H 1 /BPC 8 /CS /RGB ID abc EI\n".getBytes(StandardCharsets.US_ASCII)));
            var streams = new java.util.ArrayList<org.apache.pdfbox.pdmodel.common.PDStream>();
            page.getContentStreams().forEachRemaining(streams::add); streams.add(stream); page.setContents(streams);
            assertEquals("PARTIAL_TEXT",selectPdf(pdf).qualityCode());
        }
    }

    @Test void usedFormKeepsConservativeQualityUntilItsVisualContentIsProven() throws Exception {
        try (var pdf = new PDDocument()) {
            PDPage page = new PDPage(); pdf.addPage(page); insertText(pdf,page);
            PDFormXObject form = new PDFormXObject(pdf);
            form.setResources(new PDResources());
            form.setBBox(new org.apache.pdfbox.pdmodel.common.PDRectangle(100,100));
            try (var output = form.getContentStream().createOutputStream()) { output.write("q Q".getBytes(StandardCharsets.US_ASCII)); }
            try (var content = new PDPageContentStream(pdf,page,PDPageContentStream.AppendMode.APPEND,true)) { content.drawForm(form); }
            assertEquals("PARTIAL_TEXT",selectPdf(pdf).qualityCode());
        }
    }

    @Test void missingDrawnObjectNeverBecomesCompleteText() throws Exception {
        try (var pdf = new PDDocument()) {
            PDPage page = new PDPage(); pdf.addPage(page); insertText(pdf,page);
            var stream = new org.apache.pdfbox.pdmodel.common.PDStream(pdf,
                    new java.io.ByteArrayInputStream("/Missing Do\n".getBytes(StandardCharsets.US_ASCII)));
            var streams = new java.util.ArrayList<org.apache.pdfbox.pdmodel.common.PDStream>();
            page.getContentStreams().forEachRemaining(streams::add); streams.add(stream); page.setContents(streams);
            assertNotEquals("COMPLETE_TEXT",selectPdf(pdf).qualityCode());
        }
    }

    @Test void hwpxMixedParagraphPreservesBeforeCellAfterOrderWithoutJoiningScopes() throws Exception {
        var result = selectHwpx("<hp:p><hp:run><hp:t>앞 😀</hp:t><hp:tbl><hp:tr><hp:tc>"
                + "<hp:p><hp:run><hp:t>셀</hp:t></hp:run></hp:p></hp:tc></hp:tr></hp:tbl>"
                + "<hp:t>뒤</hp:t></hp:run></hp:p><hp:p><hp:run><hp:t>다음</hp:t></hp:run></hp:p>");
        assertEquals("앞 😀\n셀\n뒤\n다음",result.text());
        assertEquals("COMPLETE_TEXT",result.qualityCode());
        assertEquals(4,result.blocks().size());
        assertTrue(result.blocks().get(0).scopeReliable());
        assertTrue(result.blocks().get(1).scopeReliable());
        assertTrue(result.blocks().get(2).scopeReliable());
        assertTrue(result.blocks().get(3).scopeReliable());
        assertEquals(4,result.blocks().stream().map(ExtractionResult.Block::evidenceScopeId).distinct().count());
        validateOffsets(result);
    }

    @Test void hwpxNestedCellsPreserveOrderAcrossSeveralLevelsAndSiblingCells() throws Exception {
        var result = selectHwpx("<hp:p><hp:t>A</hp:t><hp:p><hp:t>B</hp:t><hp:p><hp:t>C</hp:t></hp:p>"
                + "<hp:t>D</hp:t></hp:p><hp:t>E</hp:t><hp:p><hp:t>F</hp:t></hp:p><hp:t>G</hp:t></hp:p>");
        assertEquals("A\nB\nC\nD\nE\nF\nG",result.text());
        assertEquals(7,result.blocks().stream().map(ExtractionResult.Block::locator).distinct().count());
        assertEquals("COMPLETE_TEXT",result.qualityCode()); validateOffsets(result);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"pic","ole","equation"})
    void hwpxUnextractedContentRemainsPartialEvenAfterMixedTextOrderIsRestored(String element) throws Exception {
        var result=selectHwpx("<hp:p><hp:t>앞</hp:t><hp:p><hp:t>셀</hp:t></hp:p><hp:"
                +element+"/><hp:t>뒤</hp:t></hp:p>");
        assertEquals("앞\n셀\n뒤",result.text()); assertEquals("PARTIAL_TEXT",result.qualityCode());
        validateOffsets(result);
    }

    @Test void hwpxEmptyContainerDoesNotInvalidateCompleteCellParagraphs() throws Exception {
        var result = selectHwpx("<hp:p><hp:tbl><hp:tr><hp:tc><hp:p><hp:t>셀1</hp:t></hp:p></hp:tc>"
                + "<hp:tc><hp:p><hp:t>셀2</hp:t></hp:p></hp:tc></hp:tr></hp:tbl></hp:p>");
        assertEquals("셀1\n셀2",result.text()); assertEquals("COMPLETE_TEXT",result.qualityCode());
        assertTrue(result.blocks().stream().allMatch(ExtractionResult.Block::scopeReliable)); validateOffsets(result);
    }

    private ExtractionResult selectPdf(PDDocument pdf) throws Exception {
        Path file = root.resolve(java.util.UUID.randomUUID()+".pdf"); pdf.save(file.toFile());
        return AttachmentExtractorMain.selectExtraction(file);
    }
    private void insertText(PDDocument pdf,PDPage page) throws Exception {
        try (var content = new PDPageContentStream(pdf,page)) {
            content.beginText(); content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA),12);
            content.newLineAtOffset(30,700); content.showText("Business grant notice"); content.endText();
        }
    }
    private ExtractionResult selectHwpx(String content) throws Exception {
        Path file = root.resolve(java.util.UUID.randomUUID()+".hwpx");
        String xml = "<hs:sec xmlns:hs=\"http://www.hancom.co.kr/hwpml/2011/section\" xmlns:hp=\"http://www.hancom.co.kr/hwpml/2011/paragraph\">"+content+"</hs:sec>";
        try (var zip = new ZipOutputStream(Files.newOutputStream(file))) {
            zip.putNextEntry(new ZipEntry("mimetype")); zip.write("application/hwp+zip".getBytes(StandardCharsets.US_ASCII)); zip.closeEntry();
            zip.putNextEntry(new ZipEntry("Contents/section0.xml")); zip.write(xml.getBytes(StandardCharsets.UTF_8)); zip.closeEntry();
        }
        return AttachmentExtractorMain.selectExtraction(file);
    }
    private void validateOffsets(ExtractionResult result) {
        int offset=0;
        for (var block:result.blocks()) {
            assertEquals(offset,block.startOffset());
            assertTrue(block.endOffset()>block.startOffset()); offset=block.endOffset()+1;
        }
        assertEquals(result.text().codePointCount(0,result.text().length()),offset-1);
    }
}
