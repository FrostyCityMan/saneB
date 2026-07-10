/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: AnnouncementSourceIdentityNormalizer.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.announcementsource.localgov.support;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class AnnouncementSourceIdentityNormalizer {

    private static final Set<String> TRACKING_PARAMETERS = Set.of(
            "utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content", "gclid", "fbclid", "ref"
    );

    /**
     * 공고 원문 URL을 교차 제공자 비교용 canonical URL로 변환합니다.
     *
     * @param value 원문 URL
     * @return canonical URL
     */
    public String canonicalizeUrl(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            URI uri = new URI(value.trim());
            String scheme = uri.getScheme() == null ? null : uri.getScheme().toLowerCase(Locale.ROOT);
            String host = uri.getHost() == null ? null : uri.getHost().toLowerCase(Locale.ROOT);
            int port = uri.getPort();
            if (("http".equals(scheme) && port == 80) || ("https".equals(scheme) && port == 443)) {
                port = -1;
            }
            String path = uri.getPath();
            if (path == null || path.isBlank()) {
                path = "/";
            } else if (path.length() > 1 && path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
            }
            return new URI(scheme, null, host, port, path, normalizeQuery(uri.getRawQuery()), null).toASCIIString();
        } catch (URISyntaxException exception) {
            return value.trim();
        }
    }

    /**
     * 제목과 기관명을 대소문자·공백에 독립적인 비교 문자열로 변환합니다.
     *
     * @param value 원문 문자열
     * @return 정규화 문자열
     */
    public String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFKC)
                .trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ");
    }

    /**
     * 문자열의 SHA-256 fingerprint를 생성합니다.
     *
     * @param value 원문 문자열
     * @return 64자리 소문자 16진수
     */
    public String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte item : digest) {
                result.append(String.format("%02x", item));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", exception);
        }
    }

    /**
     * 추적 파라미터를 제거하고 나머지 query parameter 순서를 정규화합니다.
     *
     * @param rawQuery 원본 query
     * @return 정규화 query
     */
    private String normalizeQuery(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return null;
        }
        String normalized = Arrays.stream(rawQuery.split("&"))
                .map(this::normalizeParameter)
                .filter(parameter -> parameter != null && !parameter.isBlank())
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.joining("&"));
        return normalized.isBlank() ? null : normalized;
    }

    /**
     * 단일 query parameter를 비교 가능한 값으로 변환합니다.
     *
     * @param rawParameter 원본 query parameter
     * @return 정규화된 query parameter 또는 제거 대상이면 null
     */
    private String normalizeParameter(String rawParameter) {
        String[] pair = rawParameter.split("=", 2);
        String key = URLDecoder.decode(pair[0], StandardCharsets.UTF_8);
        String comparisonKey = key.toLowerCase(Locale.ROOT);
        if (TRACKING_PARAMETERS.contains(comparisonKey) || comparisonKey.startsWith("utm_")) {
            return null;
        }
        String encodedKey = URLEncoder.encode(key, StandardCharsets.UTF_8).replace("+", "%20");
        if (pair.length == 1) {
            return encodedKey;
        }
        String value = URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
        return encodedKey + "=" + URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
