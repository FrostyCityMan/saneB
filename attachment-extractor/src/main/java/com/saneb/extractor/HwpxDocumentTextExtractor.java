package com.saneb.extractor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;

/** 파일을 풀지 않고 제한된 ZIP stream과 외부 entity가 금지된 XML event만 읽는다. */
final class HwpxDocumentTextExtractor {
    private static final String HP = "http://www.hancom.co.kr/hwpml/2011/paragraph";
    ExtractionResult selectExtraction(Path file) throws Exception {
        TextEvidence evidence = new TextEvidence();
        try (ZipFile zip = new ZipFile(file.toFile())) {
            if (zip.size() > 2000) throw new IOException("LIMIT_EXCEEDED");
            Set<String> names = new HashSet<>();
            long total = 0;
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                if (!names.add(name) || name.startsWith("/") || name.contains("\\") || name.contains(":")
                        || java.util.Arrays.asList(name.split("/", -1)).contains("..")) throw new IOException("CORRUPT");
                if (entry.getSize() < 0 || entry.getSize() > 128L * 1024 * 1024
                        || entry.getSize() > Math.max(1L, entry.getCompressedSize()) * 100L)
                    throw new IOException("LIMIT_EXCEEDED");
                total += entry.getSize();
                if (total > 128L * 1024 * 1024) throw new IOException("LIMIT_EXCEEDED");
                // 모든 entry에 실제 byte 상한을 검증한다. embedded 파일은 파싱·추출하지 않는다.
                try (InputStream input = zip.getInputStream(entry)) {
                    long bytes = 0;
                    byte[] buffer = new byte[8192];
                    for (int count; (count = input.read(buffer)) != -1;) {
                        bytes += count;
                        if (bytes > entry.getSize()) throw new IOException("LIMIT_EXCEEDED");
                    }
                }
            }
            if (!names.contains("mimetype")) return ExtractionResult.failure("UNSUPPORTED");
            try (InputStream input = zip.getInputStream(zip.getEntry("mimetype"))) {
                if (!new String(input.readNBytes(128), java.nio.charset.StandardCharsets.US_ASCII).strip()
                        .equals("application/hwp+zip")) return ExtractionResult.failure("UNSUPPORTED");
            }
            var sections = names.stream().filter(name -> name.matches("Contents/section[0-9]+\\.xml"))
                    .sorted(java.util.Comparator.comparingInt(name -> Integer.parseInt(name.substring(16, name.length()-4))))
                    .toList();
            if (sections.isEmpty()) return ExtractionResult.failure("CORRUPT");
            for (String section : sections) {
                try (InputStream input = zip.getInputStream(zip.getEntry(section))) {
                    XMLInputFactory factory = XMLInputFactory.newFactory();
                    factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
                    factory.setProperty("javax.xml.stream.isSupportingExternalEntities", false);
                    factory.setXMLResolver((a,b,c,d) -> { throw new javax.xml.stream.XMLStreamException("EXTERNAL_ENTITY_FORBIDDEN"); });
                    var xml = factory.createXMLStreamReader(input);
                    ArrayDeque<Paragraph> paragraphs = new ArrayDeque<>();
                    int index = 0;
                    int textDepth = 0;
                    int depth = 0;
                    try {
                        while (xml.hasNext()) {
                            int event = xml.next();
                            if (event == XMLStreamConstants.DTD || event == XMLStreamConstants.ENTITY_REFERENCE)
                                throw new IOException("CORRUPT");
                            if (event == XMLStreamConstants.START_ELEMENT) {
                                if (++depth > 128) throw new IOException("LIMIT_EXCEEDED");
                                if (depth == 1 && (!"sec".equals(xml.getLocalName())
                                        || !"http://www.hancom.co.kr/hwpml/2011/section".equals(xml.getNamespaceURI())))
                                    throw new IOException("CORRUPT");
                                if (HP.equals(xml.getNamespaceURI())) {
                                    String name = xml.getLocalName();
                                    if ("p".equals(name)) {
                                        if (!paragraphs.isEmpty()) {
                                            Paragraph parent = paragraphs.peek();
                                            parent.hasNestedParagraph=true;
                                            // 자식 셀/문단을 읽기 전에 앞부분을 기록하여 원문 순서를 보존한다.
                                            parent.saveSegment(evidence,section);
                                        }
                                        paragraphs.push(new Paragraph(++index));
                                    }
                                    if ("t".equals(name)) textDepth++;
                                    if ("pic".equals(name) || "ole".equals(name) || "equation".equals(name)) evidence.updatePartial();
                                }
                            } else if (event == XMLStreamConstants.CHARACTERS && textDepth > 0 && !paragraphs.isEmpty()) {
                                paragraphs.peek().text.append(xml.getText());
                                if (paragraphs.peek().text.length() > TextEvidence.MAX_CHARACTERS * 2) throw new IOException("LIMIT_EXCEEDED");
                            } else if (event == XMLStreamConstants.END_ELEMENT) {
                                if (HP.equals(xml.getNamespaceURI())) {
                                    if ("t".equals(xml.getLocalName())) textDepth--;
                                    if ("p".equals(xml.getLocalName())) {
                                        Paragraph paragraph = paragraphs.pop();
                                        paragraph.saveSegment(evidence,section);
                                    }
                                }
                                depth--;
                            }
                        }
                        if (index == 0) throw new IOException("CORRUPT");
                    } finally { xml.close(); }
                }
            }
        }
        return evidence.selectResult("HWPX", null);
    }
    private static final class Paragraph {
        final int index;
        final StringBuilder text = new StringBuilder();
        boolean hasNestedParagraph;
        int segment;
        Paragraph(int index) { this.index = index; }
        void saveSegment(TextEvidence evidence,String section) throws IOException {
            if (text.toString().isBlank()) { text.setLength(0); return; }
            String locator = section + ":paragraph:" + index;
            // 중첩 전후를 하나의 AND 문맥으로 합치거나 표의 의미를 추정하지 않는다.
            if (hasNestedParagraph) {
                locator += ":segment:" + (++segment);
            }
            // 이 구간은 실제 한 문단의 연속된 텍스트다. 구조를 분리해 누락·재결합 없이
            // 읽었으므로 중첩 자체를 부분 추출로 보지 않는다. pic/OLE/수식은 별도 차단한다.
            evidence.insertBlock(text.toString(),locator,true);
            text.setLength(0);
        }
    }
}
