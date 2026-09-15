package com.saneb.extractor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;
import org.apache.poi.poifs.filesystem.DirectoryNode;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;

/** HWP 5의 OLE container와 bounded BodyText 레코드만 읽고 스크립트·개체는 실행하지 않는다. */
final class HwpDocumentTextExtractor {
    ExtractionResult selectExtraction(Path file) throws IOException {
        TextEvidence evidence = new TextEvidence();
        int sectionCount = 0, recordCount = 0, maximumLevel = 0;
        int[] recordTypes = new int[1024];
        try (POIFSFileSystem ole = new POIFSFileSystem(file.toFile(), true)) {
            DirectoryNode root = ole.getRoot();
            if (!root.hasEntry("FileHeader")) return ExtractionResult.failure("UNSUPPORTED");
            byte[] header;
            try (InputStream input = root.createDocumentInputStream("FileHeader")) { header = input.readNBytes(256); }
            if (header.length != 256 || !new String(header,0,17,StandardCharsets.US_ASCII).equals("HWP Document File")
                    || header[35] != 5) return ExtractionResult.failure("UNSUPPORTED");
            int flags = ByteBuffer.wrap(header,36,4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            if ((flags & 2) != 0) return ExtractionResult.failure("ENCRYPTED");
            if ((flags & 4) != 0) return ExtractionResult.failure("UNSUPPORTED");
            DirectoryNode body = (DirectoryNode) root.getEntry("BodyText");
            var sections = new ArrayList<String>();
            body.forEach(entry -> { if (entry.getName().matches("Section[0-9]+")) sections.add(entry.getName()); });
            sections.sort(Comparator.comparingInt(name -> Integer.parseInt(name.substring(7))));
            if (sections.isEmpty()) return ExtractionResult.failure("CORRUPT");
            sectionCount = sections.size();
            long remaining = 128L * 1024 * 1024;
            for (String section : sections) {
                long compressedSize=((org.apache.poi.poifs.filesystem.DocumentEntry)body.getEntry(section)).getSize();
                long sectionBudget=remaining;
                Inflater inflater = new Inflater(true);
                try (InputStream raw = body.createDocumentInputStream(section);
                     InputStream input = (flags & 1) != 0 ? new InflaterInputStream(raw,inflater,8192) : raw) {
                    int paragraph = 0;
                    for (;;) {
                        byte[] recordHeader = input.readNBytes(4);
                        if (recordHeader.length == 0) break;
                        if (recordHeader.length != 4) throw new IOException("CORRUPT");
                        int record = ByteBuffer.wrap(recordHeader).order(ByteOrder.LITTLE_ENDIAN).getInt();
                        int tag = record & 1023;
                        int size = record >>> 20;
                        remaining -= 4;
                        if (size == 4095) {
                            byte[] extended = input.readNBytes(4);
                            if (extended.length != 4) throw new IOException("CORRUPT");
                            size = ByteBuffer.wrap(extended).order(ByteOrder.LITTLE_ENDIAN).getInt();
                            remaining -= 4;
                        }
                        if (size < 0 || size > remaining || size > 8 * 1024 * 1024) throw new IOException("LIMIT_EXCEEDED");
                        byte[] data = input.readNBytes(size);
                        if (data.length != size) throw new IOException("CORRUPT");
                        remaining -= size;
                        if ((flags & 1) != 0 && sectionBudget-remaining>Math.max(1L,compressedSize)*100L)
                            throw new IOException("LIMIT_EXCEEDED");
                        recordCount++;
                        recordTypes[tag]++;
                        maximumLevel = Math.max(maximumLevel, (record >>> 10) & 1023);
                        if (tag == 67) evidence.insertBlock(selectParagraph(data), section + ":paragraph:" + (++paragraph), true);
                        // 문단/레이아웃 metadata(66~75) 외 구조는 별도 지원 검증이 필요하다.
                        // 글맵시/양식/차트/미래 tag를 76~88 범위 밖이라는 이유로 완전 추출 처리하지 않는다.
                        if (tag < 66 || tag > 75) evidence.updatePartial();
                    }
                } finally { inflater.end(); }
            }
        }
        var types = new ArrayList<ExtractionResult.RecordType>();
        for (int tag = 0; tag < recordTypes.length; tag++)
            if (recordTypes[tag] > 0) types.add(new ExtractionResult.RecordType(tag, recordTypes[tag]));
        var result = evidence.selectResult("HWP", null);
        return new ExtractionResult(result.format(), result.extractorVersion(), result.qualityCode(),
                result.text(), result.blocks(), result.pageCount(), result.errorCode(),
                new ExtractionResult.HwpStructure(sectionCount, recordCount, maximumLevel, types));
    }
    private String selectParagraph(byte[] bytes) throws IOException {
        if (bytes.length % 2 != 0) throw new IOException("CORRUPT");
        var buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        StringBuilder text = new StringBuilder();
        while (buffer.hasRemaining()) {
            char ch = buffer.getChar();
            if (ch >= 32) { text.append(ch); continue; }
            if (ch == 9) text.append('\t');
            else if (ch == 10 || ch == 13) text.append('\n');
            else text.append(' ');
            if ((ch >= 1 && ch <= 9) || (ch >= 11 && ch <= 12) || (ch >= 14 && ch <= 23)) {
                if (buffer.remaining() < 14) throw new IOException("CORRUPT");
                buffer.position(buffer.position() + 14);
            }
        }
        return text.toString();
    }
}
