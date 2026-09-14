package com.saneb.domain.announcementattachment.service.impl;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** 빌드가 열거한 전체 업무 클래스/리소스를 실제 classpath 바이트와 대조한다. 성공 QA나 운영 적용 증거는 아니다. */
public final class AttachmentApplicationCodeFingerprint {
    static final String CATALOG = "attachment-qa-code/catalog.json";
    private static final int MAX_CATALOG_BYTES = 2 * 1024 * 1024;
    private static final long MAX_FILE_BYTES = 64L * 1024 * 1024;
    private static final long MAX_TOTAL_BYTES = 256L * 1024 * 1024;
    private final ObjectMapper mapper;
    private final Resources resources;

    @FunctionalInterface interface Resources { InputStream open(String path) throws IOException; }
    record Entry(String path, Long size, String sha256) { }
    record Catalog(Integer schemaVersion, List<Entry> entries) { }
    static final class Failure extends IOException {
        private final String code;
        Failure(String code) { super(code); this.code = code; }
        String selectCode() { return code; }
    }
    public AttachmentApplicationCodeFingerprint(ObjectMapper mapper) {
        this(mapper, path -> AttachmentApplicationCodeFingerprint.class.getResourceAsStream("/" + path));
    }
    AttachmentApplicationCodeFingerprint(ObjectMapper mapper, Resources resources) {
        this.mapper = mapper.copy().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                DeserializationFeature.FAIL_ON_TRAILING_TOKENS).enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
        this.resources = resources;
    }
    public String selectVerifiedHash() throws IOException {
        if (TransactionSynchronizationManager.isActualTransactionActive()) throw new Failure("QA_CODE_TRANSACTION_ACTIVE");
        byte[] encoded;
        try (var input = resources.open(CATALOG)) {
            if (input == null) throw new Failure("QA_CODE_CATALOG_MISSING");
            encoded = input.readNBytes(MAX_CATALOG_BYTES + 1);
            if (encoded.length > MAX_CATALOG_BYTES) throw new Failure("QA_CODE_CATALOG_LIMIT");
        }
        Catalog catalog;
        try { catalog = mapper.readValue(encoded, Catalog.class); }
        catch (Exception exception) { throw new Failure("QA_CODE_CATALOG_INVALID"); }
        if (catalog == null || !Integer.valueOf(1).equals(catalog.schemaVersion()) || catalog.entries() == null
                || catalog.entries().isEmpty() || catalog.entries().size() > 10000) throw new Failure("QA_CODE_CATALOG_INVALID");
        var names = new HashSet<String>();
        long total = 0;
        // 모든 경로/상한을 먼저 검사한다. 목록의 값은 파일시스템/URL 요청으로 해석하지 않는다.
        for (Entry entry : catalog.entries()) {
            if (entry == null || !selectSafePath(entry.path()) || !names.add(entry.path()) || entry.size() == null
                    || entry.size() < 0 || entry.size() > MAX_FILE_BYTES || entry.sha256() == null
                    || !entry.sha256().matches("[0-9a-f]{64}")) throw new Failure("QA_CODE_CATALOG_INVALID");
            total += entry.size();
            if (total > MAX_TOTAL_BYTES) throw new Failure("QA_CODE_RESOURCE_LIMIT");
        }
        for (Entry entry : catalog.entries()) {
            try (var input = resources.open(entry.path())) {
                if (input == null) throw new Failure("QA_CODE_RESOURCE_MISSING");
                var digest = selectDigest();
                long size = 0;
                byte[] buffer = new byte[65536];
                for (int count; (count = input.read(buffer)) != -1;) {
                    size += count;
                    if (size > entry.size()) throw new Failure("QA_CODE_RESOURCE_CHANGED");
                    digest.update(buffer, 0, count);
                }
                if (size != entry.size() || !HexFormat.of().formatHex(digest.digest()).equals(entry.sha256()))
                    throw new Failure("QA_CODE_RESOURCE_CHANGED");
            }
        }
        return HexFormat.of().formatHex(selectDigest().digest(encoded));
    }
    private boolean selectSafePath(String path) {
        return path != null && !path.isBlank() && path.length() <= 512 && !path.startsWith("/")
                && !path.contains("\\") && !path.contains(":") && !path.equals(CATALOG)
                && path.codePoints().noneMatch(Character::isISOControl)
                && java.util.Arrays.stream(path.split("/", -1)).noneMatch(part -> part.isEmpty() || part.equals(".") || part.equals(".."));
    }
    private MessageDigest selectDigest() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (java.security.NoSuchAlgorithmException exception) { throw new IllegalStateException("SHA-256을 사용할 수 없습니다."); }
    }
}
