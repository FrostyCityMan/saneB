package com.saneb.extractor.qa;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;

/** 빌드 전용. 임의 문서/URL을 받지 않으며 생성기와 파서 JAR은 서버 artifact에 포함하지 않는다. */
public final class AttachmentRuntimeFixtureGenerator {
    private AttachmentRuntimeFixtureGenerator() { }
    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("QA 출력 경로가 필요합니다.");
        Path root = Files.createDirectories(Path.of(args[0]));
        savePdf(root.resolve("AR-001.bin"), true);
        savePdf(root.resolve("AR-002.bin"), false);
        saveHwp(root.resolve("AR-003.bin"), 0, false);
        saveHwp(root.resolve("AR-004.bin"), 2, false);
        saveHwp(root.resolve("AR-005.bin"), 0, true);
        String opening = "<hs:sec xmlns:hs=\"http://www.hancom.co.kr/hwpml/2011/section\" xmlns:hp=\"http://www.hancom.co.kr/hwpml/2011/paragraph\">";
        String paragraph = "<hp:p><hp:run><hp:t>소상공인 지원금 😀</hp:t></hp:run></hp:p>";
        saveHwpx(root.resolve("AR-006.bin"), opening + paragraph
                + "<hp:tbl><hp:tr><hp:tc><hp:p><hp:run><hp:t>지원 한도</hp:t></hp:run></hp:p></hp:tc>"
                + "<hp:tc><hp:p><hp:run><hp:t>100만원</hp:t></hp:run></hp:p></hp:tc></hp:tr></hp:tbl></hs:sec>", null);
        saveHwpx(root.resolve("AR-007.bin"), opening + paragraph + "<hp:pic/></hs:sec>", null);
        saveHwpx(root.resolve("AR-008.bin"), "<!DOCTYPE x [<!ENTITY x SYSTEM 'file:///qa-canary-never-mounted'>]><x>&x;</x>", null);
        saveHwpx(root.resolve("AR-009.bin"), opening + paragraph + "</hs:sec>", "../outside");
        saveHwpx(root.resolve("AR-010.bin"), opening + "<hp:p><hp:run><hp:t>" + "a".repeat(100000) + "</hp:t></hp:run></hp:p></hs:sec>", null);
        Files.writeString(root.resolve("AR-011.bin"), "<html>QA 오류 응답</html>");
        Files.writeString(root.resolve("AR-012.bin"), "%PDF-1.7\ninvalid fixture\n");
    }
    private static void savePdf(Path file, boolean text) throws Exception {
        try (PDDocument pdf = new PDDocument()) {
            PDPage page = new PDPage(); pdf.addPage(page);
            // PDF ID를 빌드 시각에서 유도하지 않는다. 반복 생성에서 같은 입력을 유지한다.
            var ids = new org.apache.pdfbox.cos.COSArray();
            ids.add(new org.apache.pdfbox.cos.COSString("saneb-runtime-qa"));
            ids.add(new org.apache.pdfbox.cos.COSString("saneb-runtime-qa"));
            pdf.getDocument().setDocumentID(ids);
            if (text) try (var stream = new PDPageContentStream(pdf, page)) {
                stream.beginText(); stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                stream.newLineAtOffset(30, 700); stream.showText("Business grant notice"); stream.endText();
            }
            pdf.save(file.toFile());
        }
    }
    private static void saveHwp(Path file, int flags, boolean truncated) throws Exception {
        byte[] header = new byte[256];
        System.arraycopy("HWP Document File".getBytes(StandardCharsets.US_ASCII), 0, header, 0, 17);
        header[35] = 5; ByteBuffer.wrap(header, 36, 4).order(ByteOrder.LITTLE_ENDIAN).putInt(flags);
        byte[] text = "소상공인 지원금 😀".getBytes(StandardCharsets.UTF_16LE);
        var body = new ByteArrayOutputStream();
        body.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(67 | ((text.length + (truncated ? 2 : 0)) << 20)).array());
        body.write(text);
        try (var ole = new POIFSFileSystem()) {
            ole.getRoot().createDocument("FileHeader", new ByteArrayInputStream(header));
            ole.getRoot().createDirectory("BodyText").createDocument("Section0", new ByteArrayInputStream(body.toByteArray()));
            try (var output = Files.newOutputStream(file)) { ole.writeFilesystem(output); }
        }
    }
    private static void saveHwpx(Path file, String xml, String extra) throws Exception {
        try (var zip = new ZipOutputStream(Files.newOutputStream(file))) {
            saveEntry(zip, "mimetype", "application/hwp+zip");
            saveEntry(zip, "Contents/section0.xml", xml);
            if (extra != null) saveEntry(zip, extra, "QA");
        }
    }
    private static void saveEntry(ZipOutputStream zip, String name, String content) throws Exception {
        var entry = new ZipEntry(name); entry.setTimeLocal(LocalDateTime.of(2020, 1, 1, 0, 0));
        zip.putNextEntry(entry); zip.write(content.getBytes(StandardCharsets.UTF_8)); zip.closeEntry();
    }
}
