/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: AppLogServiceImplTest.java
 * 작성자: 김도훈
 *
 */

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

    /**
     * 업무 데이터를 조회합니다.
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
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

    /**
     * 업무 데이터를 조회합니다.
     */
    @Test
    void selectAppLogReturnsUnavailableWhenFileIsMissing() {
        AppLogServiceImpl service = new AppLogServiceImpl(tempDir.resolve("missing.log").toString(), 20);

        AppLogResponse response = service.selectAppLog(null, null, 120);

        assertThat(response.available()).isFalse();
        assertThat(response.lines()).isEmpty();
        assertThat(response.message()).contains("로그 파일");
    }
}
