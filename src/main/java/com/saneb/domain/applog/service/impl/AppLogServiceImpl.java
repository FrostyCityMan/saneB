/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: AppLogServiceImpl.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.applog.service.impl;

import com.saneb.domain.applog.dto.AppLogResponse;
import com.saneb.domain.applog.dto.AppLogResponse.AppLogLineResponse;
import com.saneb.domain.applog.service.AppLogService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AppLogServiceImpl implements AppLogService {

    private static final int DEFAULT_LINES = 120;
    private static final int MAX_REQUEST_LINES = 500;
    private static final int DEFAULT_SCAN_LINES = 3000;
    private static final Set<String> LEVEL_CODES = Set.of("INFO", "WARN", "ERROR", "DEBUG");
    private static final Pattern KEY_VALUE_SECRET_PATTERN = Pattern.compile(
            "(?i)(password|passwd|pwd|secret|token|api[_-]?key|authorization|db_password|payment_webhook_secret|openai_api_key)(\\s*[:=]\\s*)([^\\s,;]+)"
    );
    private static final Pattern AUTHORIZATION_BEARER_PATTERN = Pattern.compile(
            "(?i)(authorization\\s*[:=]\\s*)bearer\\s+[^\\s,;]+"
    );
    private static final Pattern BEARER_PATTERN = Pattern.compile("(?i)(bearer\\s+)[^\\s,;]+");

    private final String configuredLogPath;
    private final int maxScanLines;

    /**
     * 객체를 생성합니다.
     *
     * @param environment 입력 값
     */
    @Autowired
    public AppLogServiceImpl(Environment environment) {
        this(
                environment.getProperty("saneb.logging.app-log-path", "/home/ubuntu/app/app.log"),
                environment.getProperty("saneb.logging.app-log-scan-lines", Integer.class, DEFAULT_SCAN_LINES)
        );
    }

    /**
     * 객체를 생성합니다.
     *
     * @param configuredLogPath 입력 값
     *
     * @param maxScanLines 입력 값
     */
    AppLogServiceImpl(String configuredLogPath, int maxScanLines) {
        this.configuredLogPath = configuredLogPath == null ? "" : configuredLogPath.trim();
        this.maxScanLines = Math.max(MAX_REQUEST_LINES, maxScanLines);
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param levelCode 입력 값
     *
     * @param keyword 입력 값
     *
     * @param lines 입력 값
     *
     * @return 처리 결과
     */
    @Override
    public AppLogResponse selectAppLog(String levelCode, String keyword, int lines) {
        int requestedLines = normalizeLines(lines);
        String normalizedLevelCode = normalizeLevelCode(levelCode);
        String normalizedKeyword = normalizeKeyword(keyword);

        if (!StringUtils.hasText(configuredLogPath)) {
            return unavailable(requestedLines, normalizedLevelCode, normalizedKeyword, "로그 파일 경로가 설정되어 있지 않습니다.");
        }

        Path logPath = Path.of(configuredLogPath).toAbsolutePath().normalize();
        if (!Files.isRegularFile(logPath) || !Files.isReadable(logPath)) {
            return unavailable(requestedLines, normalizedLevelCode, normalizedKeyword, "읽을 수 있는 로그 파일이 없습니다.");
        }

        try {
            List<String> recentLines = selectRecentLines(logPath, Math.max(requestedLines, maxScanLines));
            List<AppLogLineResponse> filteredLines = filterLines(recentLines, normalizedLevelCode, normalizedKeyword, requestedLines);
            OffsetDateTime lastModifiedAt = OffsetDateTime.ofInstant(
                    Files.getLastModifiedTime(logPath).toInstant(),
                    ZoneId.systemDefault()
            );
            return new AppLogResponse(
                    logPath.toString(),
                    true,
                    Files.size(logPath),
                    lastModifiedAt,
                    requestedLines,
                    filteredLines.size(),
                    normalizedLevelCode,
                    normalizedKeyword,
                    filteredLines.isEmpty() ? "조건에 맞는 로그가 없습니다." : "최근 로그를 조회했습니다.",
                    filteredLines
            );
        } catch (IOException exception) {
            return unavailable(requestedLines, normalizedLevelCode, normalizedKeyword, "로그 파일을 읽을 수 없습니다.");
        }
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param logPath 입력 값
     *
     * @param limit 입력 값
     *
     * @return 처리 결과
     *
     * @throws IOException 처리 중 예외가 발생한 경우
     */
    private List<String> selectRecentLines(Path logPath, int limit) throws IOException {
        ArrayDeque<String> recentLines = new ArrayDeque<>();
        try (Stream<String> lines = Files.lines(logPath, StandardCharsets.UTF_8)) {
            lines.forEach(line -> {
                if (recentLines.size() >= limit) {
                    recentLines.removeFirst();
                }
                recentLines.addLast(line);
            });
        }
        return new ArrayList<>(recentLines);
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param sourceLines 입력 값
     *
     * @param levelCode 입력 값
     *
     * @param keyword 입력 값
     *
     * @param requestedLines 입력 값
     *
     * @return 처리 결과
     */
    private List<AppLogLineResponse> filterLines(
            List<String> sourceLines,
            String levelCode,
            String keyword,
            int requestedLines
    ) {
        List<String> filtered = sourceLines.stream()
                .filter(line -> matchesLevel(line, levelCode))
                .filter(line -> matchesKeyword(line, keyword))
                .map(this::maskSensitiveValues)
                .toList();

        int fromIndex = Math.max(0, filtered.size() - requestedLines);
        List<String> selected = filtered.subList(fromIndex, filtered.size());
        List<AppLogLineResponse> responses = new ArrayList<>();
        for (int index = 0; index < selected.size(); index++) {
            responses.add(new AppLogLineResponse(index + 1, selected.get(index)));
        }
        return responses;
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param line 입력 값
     *
     * @param levelCode 입력 값
     *
     * @return 처리 결과
     */
    private boolean matchesLevel(String line, String levelCode) {
        if (!StringUtils.hasText(levelCode)) {
            return true;
        }
        return line.toUpperCase(Locale.ROOT).contains(levelCode);
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param line 입력 값
     *
     * @param keyword 입력 값
     *
     * @return 처리 결과
     */
    private boolean matchesKeyword(String line, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        return line.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT));
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param line 입력 값
     *
     * @return 처리 결과
     */
    private String maskSensitiveValues(String line) {
        String masked = AUTHORIZATION_BEARER_PATTERN.matcher(line).replaceAll("$1***");
        masked = BEARER_PATTERN.matcher(masked).replaceAll("$1***");
        return KEY_VALUE_SECRET_PATTERN.matcher(masked).replaceAll("$1$2***");
    }

    /**
     * 입력 값을 표준 형식으로 정규화합니다.
     *
     * @param lines 입력 값
     *
     * @return 처리 결과
     */
    private int normalizeLines(int lines) {
        if (lines <= 0) {
            return DEFAULT_LINES;
        }
        return Math.min(lines, MAX_REQUEST_LINES);
    }

    /**
     * 입력 값을 표준 형식으로 정규화합니다.
     *
     * @param levelCode 입력 값
     *
     * @return 처리 결과
     */
    private String normalizeLevelCode(String levelCode) {
        if (!StringUtils.hasText(levelCode)) {
            return "";
        }
        String normalized = levelCode.trim().toUpperCase(Locale.ROOT);
        return LEVEL_CODES.contains(normalized) ? normalized : "";
    }

    /**
     * 입력 값을 표준 형식으로 정규화합니다.
     *
     * @param keyword 입력 값
     *
     * @return 처리 결과
     */
    private String normalizeKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return "";
        }
        return keyword.trim();
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param requestedLines 입력 값
     *
     * @param levelCode 입력 값
     *
     * @param keyword 입력 값
     *
     * @param message 입력 값
     *
     * @return 처리 결과
     */
    private AppLogResponse unavailable(int requestedLines, String levelCode, String keyword, String message) {
        return new AppLogResponse(
                configuredLogPath,
                false,
                0,
                null,
                requestedLines,
                0,
                levelCode,
                keyword,
                message,
                List.of()
        );
    }
}
