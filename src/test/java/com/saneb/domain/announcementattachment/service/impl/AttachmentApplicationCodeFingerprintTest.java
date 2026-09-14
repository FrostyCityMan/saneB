package com.saneb.domain.announcementattachment.service.impl;

import static org.assertj.core.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class AttachmentApplicationCodeFingerprintTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String,byte[]> files = new HashMap<>();
    private final List<String> requested = new ArrayList<>();
    private AttachmentApplicationCodeFingerprint fingerprint() {
        return new AttachmentApplicationCodeFingerprint(mapper, path -> {
            requested.add(path);var data = files.get(path);return data == null ? null : new ByteArrayInputStream(data);
        });
    }
    private String hash(byte[] value) throws Exception { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value)); }
    private void catalog(List<AttachmentApplicationCodeFingerprint.Entry> entries) throws Exception {
        files.put(AttachmentApplicationCodeFingerprint.CATALOG, mapper.writeValueAsBytes(new AttachmentApplicationCodeFingerprint.Catalog(1,entries)));
    }
    private AttachmentApplicationCodeFingerprint.Entry entry(String path,String content) throws Exception {
        byte[] data = content.getBytes(StandardCharsets.UTF_8);files.put(path,data);
        return new AttachmentApplicationCodeFingerprint.Entry(path,(long)data.length,hash(data));
    }
    @Test void workerOrMapperChangeProducesNewCodeHashOnlyWithMatchingInstalledBytes() throws Exception {
        var worker = entry("com/saneb/domain/Worker.class","worker-v1");
        var sql = entry("mapper/Worker.xml","mapper-v1");catalog(List.of(worker,sql));
        String first = fingerprint().selectVerifiedHash();assertThat(fingerprint().selectVerifiedHash()).isEqualTo(first);
        var changed = entry(worker.path(),"worker-v2");
        assertThatThrownBy(() -> fingerprint().selectVerifiedHash()).hasMessage("QA_CODE_RESOURCE_CHANGED");
        catalog(List.of(changed,sql));String workerChanged = fingerprint().selectVerifiedHash();assertThat(workerChanged).isNotEqualTo(first);
        var changedSql = entry(sql.path(),"mapper-v2");
        assertThatThrownBy(() -> fingerprint().selectVerifiedHash()).hasMessage("QA_CODE_RESOURCE_CHANGED");
        catalog(List.of(changed,changedSql));assertThat(fingerprint().selectVerifiedHash()).isNotEqualTo(workerChanged);
    }
    @Test void missingCatalogAndResourceAreNotAccepted() throws Exception {
        assertThatThrownBy(() -> fingerprint().selectVerifiedHash()).hasMessage("QA_CODE_CATALOG_MISSING");
        var worker = entry("com/saneb/Worker.class","worker");catalog(List.of(worker));files.remove(worker.path());
        assertThatThrownBy(() -> fingerprint().selectVerifiedHash()).hasMessage("QA_CODE_RESOURCE_MISSING");
    }
    @ParameterizedTest @ValueSource(strings={"../private", "/private", "a/../private", "a//b", "https://example.com", "a\\b", "attachment-qa-code/catalog.json"})
    void malformedPathsNeverReachResourceLoader(String path) throws Exception {
        catalog(List.of(new AttachmentApplicationCodeFingerprint.Entry(path,0L,hash(new byte[0]))));
        assertThatThrownBy(() -> fingerprint().selectVerifiedHash()).hasMessage("QA_CODE_CATALOG_INVALID");
        assertThat(requested).containsExactly(AttachmentApplicationCodeFingerprint.CATALOG);
    }
    @Test void duplicateEntriesAndAggregateLimitFailBeforeOpeningAnyInput() throws Exception {
        var one = entry("one","a");catalog(List.of(one,one));
        assertThatThrownBy(() -> fingerprint().selectVerifiedHash()).hasMessage("QA_CODE_CATALOG_INVALID");
        requested.clear();
        catalog(java.util.stream.IntStream.range(0,5).mapToObj(i -> new AttachmentApplicationCodeFingerprint.Entry("file"+i,64L*1024*1024,"a".repeat(64))).toList());
        assertThatThrownBy(() -> fingerprint().selectVerifiedHash()).hasMessage("QA_CODE_RESOURCE_LIMIT");
        assertThat(requested).containsExactly(AttachmentApplicationCodeFingerprint.CATALOG);
    }
    @Test void malformedDuplicateUnknownAndTrailingJsonAreRejected() {
        for (String input : List.of("{}", "{\"schemaVersion\":1,\"schemaVersion\":1,\"entries\":[]}",
                "{\"schemaVersion\":1,\"entries\":[],\"passed\":true}", "{\"schemaVersion\":1,\"entries\":[]} {}")) {
            files.put(AttachmentApplicationCodeFingerprint.CATALOG,input.getBytes(StandardCharsets.UTF_8));
            assertThatThrownBy(() -> fingerprint().selectVerifiedHash()).hasMessage("QA_CODE_CATALOG_INVALID");
        }
    }
    @Test void shortenedOrExpandedInputIsRejected() throws Exception {
        var one = entry("one","abc");catalog(List.of(one));
        files.put("one",new byte[0]);assertThatThrownBy(() -> fingerprint().selectVerifiedHash()).hasMessage("QA_CODE_RESOURCE_CHANGED");
        files.put("one","abcdef".getBytes(StandardCharsets.UTF_8));assertThatThrownBy(() -> fingerprint().selectVerifiedHash()).hasMessage("QA_CODE_RESOURCE_CHANGED");
    }
    @Test void fileVerificationIsRejectedInsideDatabaseTransaction() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        try { assertThatThrownBy(() -> fingerprint().selectVerifiedHash()).hasMessage("QA_CODE_TRANSACTION_ACTIVE");assertThat(requested).isEmpty(); }
        finally { TransactionSynchronizationManager.setActualTransactionActive(false); }
    }
    @Test void currentBuildCatalogIncludesWorkerStorageMapperMigrationAndUiAndVerifiesActualClasspath() throws Exception {
        var actual = new AttachmentApplicationCodeFingerprint(mapper);
        assertThat(actual.selectVerifiedHash()).matches("[0-9a-f]{64}");
        try (var input = getClass().getResourceAsStream("/"+AttachmentApplicationCodeFingerprint.CATALOG)) {
            var value = mapper.readValue(input,AttachmentApplicationCodeFingerprint.Catalog.class);
            assertThat(value.entries()).extracting(AttachmentApplicationCodeFingerprint.Entry::path).contains(
                    "com/saneb/domain/announcementattachment/service/impl/AnnouncementAttachmentWorkerServiceImpl.class",
                    "com/saneb/domain/announcementattachment/service/impl/AnnouncementAttachmentEvidenceServiceImpl.class",
                    "mapper/announcementattachment/AnnouncementAttachmentEvidenceMapper.xml",
                    "db/migration/V77__add_attachment_policy_publication_receipt.sql",
                    "static/js/saneb-attachment-policies.js");
        }
    }
}
