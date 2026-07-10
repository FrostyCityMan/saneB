/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: AbstractJsonAnnouncementSourceProviderClient.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.announcementsource.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.common.error.ApiException;
import com.saneb.common.error.ErrorCode;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;

abstract class AbstractJsonAnnouncementSourceProviderClient implements AnnouncementSourceProviderClient {

    private static final DateTimeFormatter BASIC_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    protected final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final int timeoutMillis;

    /**
     * 객체를 생성합니다.
     *
     * @param objectMapper 입력 값
     *
     * @param timeoutMillis 입력 값
     */
    protected AbstractJsonAnnouncementSourceProviderClient(ObjectMapper objectMapper, int timeoutMillis) {
        this.objectMapper = objectMapper;
        this.timeoutMillis = Math.max(1000, timeoutMillis);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(this.timeoutMillis))
                .build();
    }

    /**
     * 외부 API를 호출합니다.
     *
     * @param uri 입력 값
     *
     * @return 처리 결과
     */
    protected JsonNode selectJson(URI uri) {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofMillis(timeoutMillis))
                .GET()
                .header("Accept", "application/json")
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ApiException(
                        ErrorCode.INTERNAL_ERROR,
                        HttpStatus.BAD_GATEWAY,
                        "외부 공고 API 호출에 실패했습니다. 상태 코드: " + response.statusCode()
                );
            }
            return objectMapper.readTree(response.body());
        } catch (IOException exception) {
            throw new ApiException(ErrorCode.INTERNAL_ERROR, HttpStatus.BAD_GATEWAY, "외부 공고 API 응답을 읽을 수 없습니다.");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ApiException(ErrorCode.INTERNAL_ERROR, HttpStatus.BAD_GATEWAY, "외부 공고 API 호출이 중단되었습니다.");
        }
    }

    /**
     * URL query 값을 인코딩합니다.
     *
     * @param value 입력 값
     *
     * @return 처리 결과
     */
    protected String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    /**
     * API key 설정 여부를 확인합니다.
     *
     * @param apiKey 입력 값
     */
    protected void validateApiKey(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST, "외부 공고 API 인증키가 설정되지 않았습니다.");
        }
    }

    /**
     * JSON 배열 후보를 조회합니다.
     *
     * @param root 입력 값
     *
     * @param paths 입력 값
     *
     * @return 처리 결과
     */
    protected List<JsonNode> selectItemNodes(JsonNode root, List<List<String>> paths) {
        for (List<String> path : paths) {
            JsonNode node = root;
            for (String part : path) {
                node = node == null ? null : node.path(part);
            }
            if (node != null && node.isArray()) {
                List<JsonNode> items = new ArrayList<>();
                node.forEach(items::add);
                return items;
            }
            if (node != null && node.isObject()) {
                return List.of(node);
            }
        }
        return List.of();
    }

    /**
     * JSON 문자열 값을 조회합니다.
     *
     * @param node 입력 값
     *
     * @param fieldNames 입력 값
     *
     * @return 처리 결과
     */
    protected String selectText(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode value = node.path(fieldName);
            if (!value.isMissingNode() && !value.isNull()) {
                String text = value.asText();
                if (text != null && !text.isBlank()) {
                    return stripHtml(text.trim());
                }
            }
        }
        return null;
    }

    /**
     * HTML 태그를 제거합니다.
     *
     * @param text 입력 값
     *
     * @return 처리 결과
     */
    protected String stripHtml(String text) {
        if (text == null) {
            return null;
        }
        return text
                .replaceAll("(?is)<br\\s*/?>", "\n")
                .replaceAll("(?is)</p>", "\n")
                .replaceAll("(?is)<[^>]+>", " ")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replaceAll("[ \\t\\x0B\\f\\r]+", " ")
                .replaceAll("\\n\\s+", "\n")
                .trim();
    }

    /**
     * 날짜 범위를 해석합니다.
     *
     * @param periodText 입력 값
     *
     * @return 처리 결과
     */
    protected DateRange selectDateRange(String periodText) {
        if (periodText == null || periodText.isBlank()) {
            return new DateRange(null, null);
        }
        String normalized = periodText.replace("~", " ").replace("-", "").replace(".", "").replace("/", " ");
        List<LocalDate> dates = new ArrayList<>();
        for (String token : normalized.split("\\s+")) {
            if (token.matches("\\d{8}")) {
                try {
                    dates.add(LocalDate.parse(token, BASIC_DATE));
                } catch (DateTimeParseException ignored) {
                    // Ignore unparsable provider fragments.
                }
            }
        }
        LocalDate startDate = dates.isEmpty() ? null : dates.get(0);
        LocalDate endDate = dates.size() < 2 ? null : dates.get(1);
        return new DateRange(startDate, endDate);
    }

    /**
     * 날짜를 해석합니다.
     *
     * @param text 입력 값
     *
     * @return 처리 결과
     */
    protected OffsetDateTime selectDateTime(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        for (DateTimeFormatter formatter : List.of(DateTimeFormatter.ISO_OFFSET_DATE_TIME, DATE_TIME)) {
            try {
                if (formatter == DATE_TIME) {
                    return LocalDate.parse(text.substring(0, 10)).atStartOfDay(ZoneId.of("Asia/Seoul")).toOffsetDateTime();
                }
                return OffsetDateTime.parse(text, formatter);
            } catch (RuntimeException ignored) {
                // Try next parser.
            }
        }
        if (text.length() >= 8 && text.substring(0, 8).matches("\\d{8}")) {
            try {
                return LocalDate.parse(text.substring(0, 8), BASIC_DATE)
                        .atStartOfDay(ZoneId.of("Asia/Seoul"))
                        .toOffsetDateTime();
            } catch (DateTimeParseException ignored) {
                return null;
            }
        }
        return null;
    }

    /**
     * 누락 필드 JSON을 생성합니다.
     *
     * @param fields 입력 값
     *
     * @return 처리 결과
     */
    protected String selectMissingFieldsJson(Map<String, String> fields) {
        List<String> missing = fields.entrySet().stream()
                .filter(entry -> entry.getValue() == null || entry.getValue().isBlank())
                .map(Map.Entry::getKey)
                .toList();
        try {
            return objectMapper.writeValueAsString(Map.of("missingFields", missing));
        } catch (IOException exception) {
            return "{\"missingFields\":[]}";
        }
    }

    /**
     * 원문 완성도 코드를 산출합니다.
     *
     * @param fields 입력 값
     *
     * @return 처리 결과
     */
    protected String selectCompletenessCode(Map<String, String> fields) {
        long presentCount = fields.values().stream()
                .filter(value -> value != null && !value.isBlank())
                .count();
        if (presentCount >= fields.size() - 1L) {
            return "COMPLETE";
        }
        if (presentCount >= Math.max(2, fields.size() / 2)) {
            return "PARTIAL";
        }
        return "MINIMAL";
    }

    /**
     * JSON 원문을 문자열로 변환합니다.
     *
     * @param node 입력 값
     *
     * @return 처리 결과
     */
    protected String selectRawPayloadJson(JsonNode node) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (IOException exception) {
            return "{}";
        }
    }

    /**
     * 원문 hash를 생성합니다.
     *
     * @param providerCode 입력 값
     *
     * @param rawPayloadJson 입력 값
     *
     * @return 처리 결과
     */
    protected String selectRawHash(String providerCode, String rawPayloadJson) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((providerCode + ":" + rawPayloadJson).getBytes(StandardCharsets.UTF_8));
            return String.format("%064x", new BigInteger(1, hash));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is not available.", exception);
        }
    }

    /**
     * 첨부 파일 정보를 조회합니다.
     *
     * @param node 입력 값
     *
     * @param nameFields 입력 값
     *
     * @param urlFields 입력 값
     *
     * @return 처리 결과
     */
    protected List<AnnouncementSourceProviderAttachment> selectAttachments(
            JsonNode node,
            List<String> nameFields,
            List<String> urlFields
    ) {
        List<AnnouncementSourceProviderAttachment> attachments = new ArrayList<>();
        for (int index = 0; index < urlFields.size(); index++) {
            String url = selectText(node, urlFields.get(index));
            if (url == null || url.isBlank()) {
                continue;
            }
            String name = index < nameFields.size() ? selectText(node, nameFields.get(index)) : null;
            attachments.add(new AnnouncementSourceProviderAttachment(name, url, selectFileTypeCode(name, url)));
        }
        return attachments;
    }

    /**
     * 파일 유형을 추정합니다.
     *
     * @param fileName 입력 값
     *
     * @param fileUrl 입력 값
     *
     * @return 처리 결과
     */
    private String selectFileTypeCode(String fileName, String fileUrl) {
        String source = Optional.ofNullable(fileName).orElse(fileUrl).toLowerCase();
        if (source.contains(".pdf")) {
            return "PDF";
        }
        if (source.contains(".hwp") || source.contains(".hwpx")) {
            return "HWP";
        }
        if (source.contains(".doc") || source.contains(".docx")) {
            return "WORD";
        }
        if (source.contains(".xls") || source.contains(".xlsx")) {
            return "EXCEL";
        }
        return "URL";
    }

    /**
     * 키-값 맵을 생성합니다.
     *
     * @param entries 입력 값
     *
     * @return 처리 결과
     */
    protected Map<String, String> selectFieldMap(String... entries) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (int index = 0; index + 1 < entries.length; index += 2) {
            fields.put(entries[index], entries[index + 1]);
        }
        return fields;
    }

    /**
     * 배열을 재귀적으로 탐색합니다.
     *
     * @param node 입력 값
     *
     * @return 처리 결과
     */
    protected List<JsonNode> selectFirstArray(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return List.of();
        }
        if (node.isArray()) {
            List<JsonNode> items = new ArrayList<>();
            node.forEach(items::add);
            return items;
        }
        if (node.isObject()) {
            Iterator<JsonNode> iterator = node.elements();
            while (iterator.hasNext()) {
                List<JsonNode> nested = selectFirstArray(iterator.next());
                if (!nested.isEmpty()) {
                    return nested;
                }
            }
        }
        return List.of();
    }

    protected record DateRange(LocalDate startDate, LocalDate endDate) {
    }
}
