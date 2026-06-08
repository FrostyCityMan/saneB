package com.saneb.domain.applog.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.saneb.domain.applog.dto.AppLogResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AppLogServiceImplTest {

    @TempDir
    private Path tempDir;

    @Test
    void selectAppLogReturnsRecentMaskedLines() throws Exception {
        Path logFile = tempDir.resolve("app.log");
        Files.writeString(logFile, """
                2026-06-08 10:00:00 INFO saneB started
                2026-06-08 10:01:00 WARN DB_PASSWORD=raw-secret
                2026-06-08 10:02:00 ERROR payment failed Authorization=Bearer abc.def
                """);

        AppLogServiceImpl service = new AppLogServiceImpl(logFile.toString(), 20);

        AppLogResponse response = service.selectAppLog("ERROR", "payment", 10);

        assertThat(response.available()).isTrue();
        assertThat(response.returnedLines()).isEqualTo(1);
        assertThat(response.lines().get(0).content()).contains("ERROR payment failed");
        assertThat(response.lines().get(0).content()).contains("Authorization=***");
        assertThat(response.lines().get(0).content()).doesNotContain("abc.def");
    }

    @Test
    void selectAppLogReturnsUnavailableWhenFileIsMissing() {
        AppLogServiceImpl service = new AppLogServiceImpl(tempDir.resolve("missing.log").toString(), 20);

        AppLogResponse response = service.selectAppLog(null, null, 120);

        assertThat(response.available()).isFalse();
        assertThat(response.lines()).isEmpty();
        assertThat(response.message()).contains("로그 파일");
    }
}
