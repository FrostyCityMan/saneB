package com.saneb.extractor;

import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AttachmentExtractorTest {
    @TempDir Path root;

    @Test void pdfTextIsExtractedWithoutInventingReliableTableScopes() throws Exception {
        Path file = root.resolve("notice.bin");
        try (PDDocument pdf = new PDDocument()) {
            PDPage page = new PDPage(); pdf.addPage(page);
            try (var content = new PDPageContentStream(pdf,page)) {
                content.beginText(); content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA),12);
                content.newLineAtOffset(30,700); content.showText("Business grant notice"); content.endText();
            }
            pdf.save(file.toFile());
        }
        var result = AttachmentExtractorMain.selectExtraction(file);
        assertEquals("PDF",result.format());
        assertEquals("COMPLETE_TEXT",result.qualityCode());
        assertTrue(result.text().contains("Business grant notice"));
        assertFalse(result.blocks().getFirst().scopeReliable());
        assertEquals("page:1",result.blocks().getFirst().locator());
    }
    @Test void blankPdfRequiresOcrAndDoesNotBecomeNoFiles() throws Exception {
        Path file = root.resolve("image.pdf");
        try (PDDocument pdf = new PDDocument()) { pdf.addPage(new PDPage()); pdf.save(file.toFile()); }
        assertEquals("OCR_REQUIRED",AttachmentExtractorMain.selectExtraction(file).qualityCode());
    }
    @Test void encryptedPdfIsExplicitlyBlocked() throws Exception {
        Path file = root.resolve("locked.pdf");
        try (PDDocument pdf = new PDDocument()) {
            pdf.addPage(new PDPage());
            pdf.protect(new StandardProtectionPolicy("fixture-owner","fixture-reader",new AccessPermission()));
            pdf.save(file.toFile());
        }
        assertEquals("ENCRYPTED",AttachmentExtractorMain.selectExtraction(file).qualityCode());
    }
    @Test void htmlNamedPdfIsNotAPdf() throws Exception {
        Path file = root.resolve("notice.pdf"); Files.writeString(file,"<html>오류</html>");
        assertEquals("UNSUPPORTED",AttachmentExtractorMain.selectExtraction(file).qualityCode());
    }
    @Test void fileLimitIsCheckedBeforeParsing() throws Exception {
        Path file = root.resolve("large.pdf");
        try (var random = new java.io.RandomAccessFile(file.toFile(),"rw")) { random.setLength(20L*1024*1024+1); }
        assertEquals("LIMIT_EXCEEDED",assertThrows(java.io.IOException.class,
                () -> AttachmentExtractorMain.selectExtraction(file)).getMessage());
    }
    @Test void hwpxKeepsKoreanParagraphAndCellLocationsSeparate() throws Exception {
        String xml = "<hs:sec xmlns:hs=\"http://www.hancom.co.kr/hwpml/2011/section\" xmlns:hp=\"http://www.hancom.co.kr/hwpml/2011/paragraph\">"
                + "<hp:p><hp:run><hp:t>소상공인 지원금 😀</hp:t></hp:run></hp:p>"
                + "<hp:tbl><hp:tr><hp:tc><hp:p><hp:run><hp:t>지원 한도</hp:t></hp:run></hp:p></hp:tc>"
                + "<hp:tc><hp:p><hp:run><hp:t>100만원</hp:t></hp:run></hp:p></hp:tc></hp:tr></hp:tbl></hs:sec>";
        var result = AttachmentExtractorMain.selectExtraction(selectHwpx(xml,null));
        assertEquals("COMPLETE_TEXT",result.qualityCode());
        assertNull(result.pageCount());
        assertEquals(3,result.blocks().size());
        assertTrue(result.text().contains("소상공인 지원금 😀"));
        assertEquals("소상공인 지원금 😀".codePointCount(0,"소상공인 지원금 😀".length()),result.blocks().getFirst().endOffset());
        assertNotEquals(result.blocks().get(1).evidenceScopeId(),result.blocks().get(2).evidenceScopeId());
    }
    @Test void hwpxDtdAndExternalEntityAreRejected() throws Exception {
        Path file = selectHwpx("<!DOCTYPE x [<!ENTITY x SYSTEM 'file:///missing-canary'>]><x>&x;</x>",null);
        assertThrows(Exception.class,() -> AttachmentExtractorMain.selectExtraction(file));
    }
    @Test void zipTraversalIsRejectedWithoutCreatingFiles() throws Exception {
        Path file = selectHwpx("<x/>","../outside");
        assertThrows(Exception.class,() -> AttachmentExtractorMain.selectExtraction(file));
        assertFalse(Files.exists(root.resolve("outside")));
    }
    @Test void archiveBombIsRejected() throws Exception {
        Path file = selectHwpx("<x>" + "a".repeat(100000) + "</x>",null);
        assertEquals("LIMIT_EXCEEDED",assertThrows(java.io.IOException.class,
                () -> AttachmentExtractorMain.selectExtraction(file)).getMessage());
    }
    @Test void hwp5KoreanRecordsHaveParagraphNotFakePageNumbers() throws Exception {
        Path file = selectHwp("소상공인 지원금",0,false);
        var result = AttachmentExtractorMain.selectExtraction(file);
        assertEquals("HWP",result.format()); assertEquals("COMPLETE_TEXT",result.qualityCode());
        assertEquals("소상공인 지원금",result.text()); assertNull(result.pageCount());
        assertEquals("Section0:paragraph:1",result.blocks().getFirst().locator());
    }
    @Test void hwpEncryptedFlagIsNotIgnored() throws Exception {
        assertEquals("ENCRYPTED",AttachmentExtractorMain.selectExtraction(selectHwp("내용",2,false)).qualityCode());
    }
    @Test void hwpTruncatedRecordIsNotCompleteText() throws Exception {
        Path file = selectHwp("내용",0,true);
        assertThrows(java.io.IOException.class,() -> AttachmentExtractorMain.selectExtraction(file));
    }
    private Path selectHwpx(String xml,String extraName) throws Exception {
        Path file = root.resolve(java.util.UUID.randomUUID()+".hwpx");
        try (var zip = new ZipOutputStream(Files.newOutputStream(file))) {
            zip.putNextEntry(new ZipEntry("mimetype")); zip.write("application/hwp+zip".getBytes(StandardCharsets.US_ASCII)); zip.closeEntry();
            zip.putNextEntry(new ZipEntry("Contents/section0.xml")); zip.write(xml.getBytes(StandardCharsets.UTF_8)); zip.closeEntry();
            if (extraName != null) { zip.putNextEntry(new ZipEntry(extraName)); zip.write(1); zip.closeEntry(); }
        }
        return file;
    }
    private Path selectHwp(String text,int flags,boolean truncated) throws Exception {
        Path file = root.resolve(java.util.UUID.randomUUID()+".hwp");
        byte[] header = new byte[256];
        System.arraycopy("HWP Document File".getBytes(StandardCharsets.US_ASCII),0,header,0,17);
        header[35]=5; ByteBuffer.wrap(header,36,4).order(ByteOrder.LITTLE_ENDIAN).putInt(flags);
        byte[] para = text.getBytes(StandardCharsets.UTF_16LE);
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        body.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(67 | ((para.length+(truncated?2:0))<<20)).array());
        body.write(para);
        try (var ole = new POIFSFileSystem()) {
            ole.getRoot().createDocument("FileHeader",new ByteArrayInputStream(header));
            ole.getRoot().createDirectory("BodyText").createDocument("Section0",new ByteArrayInputStream(body.toByteArray()));
            try (var output=Files.newOutputStream(file)) { ole.writeFilesystem(output); }
        }
        return file;
    }
}
