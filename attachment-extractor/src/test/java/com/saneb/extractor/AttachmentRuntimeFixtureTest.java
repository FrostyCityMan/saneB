package com.saneb.extractor;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** 신뢰한 빌드 fixture의 parser/CLI 계약 단위 테스트. Linux 격리 성공과 별도다. */
class AttachmentRuntimeFixtureTest {
    @TempDir Path root;
    @ParameterizedTest @CsvSource({
        "AR-001,COMPLETE_TEXT,PDF,1", "AR-002,OCR_REQUIRED,PDF,0", "AR-003,COMPLETE_TEXT,HWP,1",
        "AR-004,ENCRYPTED,NONE,0", "AR-005,CORRUPT,NONE,0", "AR-006,COMPLETE_TEXT,HWPX,3",
        "AR-007,PARTIAL_TEXT,HWPX,1", "AR-008,CORRUPT,NONE,0", "AR-009,CORRUPT,NONE,0",
        "AR-010,LIMIT_EXCEEDED,NONE,0", "AR-011,UNSUPPORTED,NONE,0", "AR-012,CORRUPT,NONE,0"
    })
    void fixedBuildFixtureHasExactCliQuality(String id, String quality, String format, int blocks) throws Exception {
        Path input = root.resolve("input.bin");
        try (var resource = getClass().getResourceAsStream("/attachment-runtime-qa/" + id + ".bin")) {
            assertNotNull(resource); Files.copy(resource, input);
        }
        PrintStream originalOut = System.out, originalErr = System.err;
        var bytes = new ByteArrayOutputStream();
        try (var capture = new PrintStream(bytes, false, StandardCharsets.US_ASCII)) {
            System.setOut(capture);
            try { AttachmentExtractorMain.main(new String[]{input.toString()}); }
            finally { System.setOut(originalOut); System.setErr(originalErr); }
        }
        var result = new ObjectMapper().readTree(bytes.toByteArray());
        assertEquals(quality, result.path("qualityCode").asText());
        assertEquals("NONE".equals(format) ? "" : format, result.path("format").asText(""));
        assertEquals(blocks, result.path("blocks").size());
        if (id.equals("AR-003") || id.equals("AR-006") || id.equals("AR-007"))
            assertTrue(result.path("text").asText().startsWith("소상공인 지원금 😀"));
        assertFalse(Files.exists(root.getParent().resolve("outside")));
    }
}
