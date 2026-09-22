package com.saneb.extractor;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class HwpRecordLimitFixtureTest {
    @TempDir Path temporary;

    @ParameterizedTest @ValueSource(ints = {20_000, 20_001})
    void generatedOleReachesParagraphBudgetRatherThanByteOrCompressionLimit(int count) throws Exception {
        Path file = temporary.resolve("synthetic.hwp");
        try (var input = getClass().getResourceAsStream("/hwp-record-limit-qa/paragraphs-" + count + ".hwp")) {
            assertNotNull(input);
            Files.copy(input, file);
        }
        assertTrue(Files.size(file) < 1024 * 1024);
        try (var ole = new POIFSFileSystem(file.toFile(), true)) {
            try (var header = ole.getRoot().createDocumentInputStream("FileHeader")) {
                assertEquals(0, ByteBuffer.wrap(header.readAllBytes()).order(ByteOrder.LITTLE_ENDIAN).getInt(36));
            }
            var directory = (org.apache.poi.poifs.filesystem.DirectoryEntry) ole.getRoot().getEntry("BodyText");
            try (var section = new org.apache.poi.poifs.filesystem.DocumentInputStream(
                    (org.apache.poi.poifs.filesystem.DocumentEntry) directory.getEntry("Section0"))) {
                var records = ByteBuffer.wrap(section.readAllBytes()).order(ByteOrder.LITTLE_ENDIAN);
                assertEquals(count * 28, records.remaining());
                for (int i = 0; i < count; i++) {
                    assertEquals(66 | (24 << 20), records.getInt());
                    assertEquals(0x80000001, records.getInt());
                    records.position(records.position() + 20);
                }
                assertFalse(records.hasRemaining());
            }
        }
        if (count == 20_000) assertEquals("OCR_REQUIRED", AttachmentExtractorMain.selectExtraction(file).qualityCode());
        else {
            var failure = assertThrows(IOException.class, () -> AttachmentExtractorMain.selectExtraction(file));
            assertEquals("LIMIT_EXCEEDED", failure.getMessage());
            // 실제 IPC 실패 계약은 빈 문자열이 아닌 JSON null과 빈 근거 배열이다.
            var output = new java.io.ByteArrayOutputStream();
            AttachmentExtractorMain.saveResult(output, ExtractionResult.failure(failure.getMessage()));
            var result = new com.fasterxml.jackson.databind.ObjectMapper().readTree(output.toByteArray());
            assertTrue(result.path("text").isNull());
            assertEquals("LIMIT_EXCEEDED", result.path("errorCode").asText());
            assertTrue(result.path("blocks").isArray());
            assertEquals(0, result.path("blocks").size());
        }
    }
}
