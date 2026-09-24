package com.saneb.domain.announcementattachment.extraction;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.stereotype.Component;

/** 명시적 QA 호출 전용. 서버에 포함된 합성 입력만 실제 격리 추출하며 전체 정책 QA 성공으로 승격하지 않는다. */
@Component
public final class AttachmentRuntimeGate {
    public static final String SUITE_VERSION = "attachment-runtime-2";
    public static final String SCOPE = "SYNTHETIC_INSTALLED_RUNTIME";
    public static final int CASE_COUNT = 14;
    private static final String KOREAN = "소상공인 지원금 😀";
    private final AttachmentRuntimeIdentity identity;
    private final IsolatedAttachmentExtractor extractor;
    private final AttachmentTemporaryStorage storage;
    private final AtomicBoolean running = new AtomicBoolean();

    public AttachmentRuntimeGate(AttachmentRuntimeIdentity identity, IsolatedAttachmentExtractor extractor, AttachmentTemporaryStorage storage) {
        this.identity = identity; this.extractor = extractor; this.storage = storage;
    }
    public record CaseResult(String caseId, String inputHash, String qualityCode, String format,
            String textHash, int characterCount, int blockCount, boolean originalRemoved) { }
    public record Result(UUID runId, String scope, String suiteVersion, String suiteHash, String runtimeHash,
            String extractorVersion, String resultHash, int caseCount, List<CaseResult> cases,
            Instant startedAt, Instant completedAt) {
        public Result { cases = List.copyOf(cases); }
    }
    public static final class GateFailure extends IOException {
        private final String caseId;
        private final String code;
        private GateFailure(String caseId, String code) { super(caseId + ":" + code); this.caseId = caseId; this.code = code; }
        public String selectCaseId() { return caseId; }
        public String selectCode() { return code; }
    }
    private record Sample(String id, String quality, String format, String text, List<String> locators, boolean reliable) { }
    private record Prepared(Sample sample, byte[] input) { }
    private record Snapshot(String hash, List<Prepared> inputs) { }

    public Result selectValidatedResult() throws GateFailure {
        return selectValidatedResult(() -> true);
    }
    public String selectSuiteHash() throws GateFailure { return selectSnapshot().hash(); }
    /** 저장된 불변 QA 결과를 현재 합성 fixture와 대조한다. 파일 추출이나 외부 요청을 새로 실행하지 않는다. */
    public void validateStoredResult(Result result,String runtimeHash) throws GateFailure {
        var snapshot=selectSnapshot();
        if(result==null || result.runId()==null || !SCOPE.equals(result.scope()) || !SUITE_VERSION.equals(result.suiteVersion())
                || !snapshot.hash().equals(result.suiteHash()) || !runtimeHash.equals(result.runtimeHash())
                || !AttachmentRuntimeIdentity.EXTRACTOR_VERSION.equals(result.extractorVersion()) || result.caseCount()!=CASE_COUNT
                || result.cases().size()!=CASE_COUNT || result.startedAt()==null || result.completedAt()==null
                || result.completedAt().isBefore(result.startedAt()))throw failure("STORED","QA_RESULT_BINDING_INVALID");
        var digest=selectDigest();savePart(digest,"suite",snapshot.hash());savePart(digest,"runtime",runtimeHash);
        for(int i=0;i<snapshot.inputs().size();i++){
            var input=snapshot.inputs().get(i);var sample=input.sample();var stored=result.cases().get(i);
            var expected=new CaseResult(sample.id(),hash(input.input()),sample.quality(),sample.format(),
                    hash(sample.text().getBytes(StandardCharsets.UTF_8)),sample.text().codePointCount(0,sample.text().length()),sample.locators().size(),true);
            if(!expected.equals(stored))throw failure(sample.id(),"QA_STORED_CASE_CHANGED");
            savePart(digest,stored.caseId(),List.of(stored.inputHash(),stored.qualityCode(),stored.format(),stored.textHash(),stored.characterCount(),stored.blockCount(),stored.originalRemoved()).toString());
        }
        if(!HexFormat.of().formatHex(digest.digest()).equals(result.resultHash()))throw failure("STORED","QA_RESULT_HASH_INVALID");
    }
    public Result selectValidatedResult(java.util.function.BooleanSupplier executionAllowed) throws GateFailure {
        if (executionAllowed == null) throw failure("SETUP", "EXECUTION_GUARD_REQUIRED");
        if (!running.compareAndSet(false, true)) throw failure("SETUP", "RUNTIME_QA_BUSY");
        try {
            UUID run = UUID.randomUUID();
            Instant started = Instant.now();
            var before = selectIdentity();
            var snapshot = selectSnapshot();
            List<CaseResult> results = new ArrayList<>();
            for (Prepared input : snapshot.inputs()) {
                if (Thread.currentThread().isInterrupted() || !executionAllowed.getAsBoolean()) throw failure(input.sample().id(), "CANCELLED");
                results.add(selectCase(run, input));
            }
            if (!executionAllowed.getAsBoolean()) throw failure("FINALIZE", "CANCELLED");
            if (!before.equals(selectIdentity()) || !snapshot.hash().equals(selectSnapshot().hash()))
                throw failure("FINALIZE", "RUNTIME_OR_SUITE_CHANGED");
            var digest = selectDigest();
            savePart(digest, "suite", snapshot.hash()); savePart(digest, "runtime", before.configHash());
            for (CaseResult result : results) {
                savePart(digest, result.caseId(), List.of(result.inputHash(), result.qualityCode(), result.format(), result.textHash(),
                        result.characterCount(), result.blockCount(), result.originalRemoved()).toString());
            }
            return new Result(run, SCOPE, SUITE_VERSION, snapshot.hash(), before.configHash(), before.extractorVersion(),
                    HexFormat.of().formatHex(digest.digest()), results.size(), results, started, Instant.now());
        } finally { running.set(false); }
    }

    private AttachmentRuntimeIdentity.Identity selectIdentity() throws GateFailure {
        try {
            var value = identity.selectIdentity();
            if (value == null || !AttachmentRuntimeIdentity.EXTRACTOR_VERSION.equals(value.extractorVersion())
                    || value.configHash() == null || !value.configHash().matches("[0-9a-f]{64}") || value.libraryCount() < 1 || value.libraryBytes() < 1)
                throw failure("SETUP", "RUNTIME_IDENTITY_INVALID");
            return value;
        } catch (GateFailure exception) { throw exception; }
        catch (IOException | RuntimeException exception) {
            throw failure("SETUP", "ISOLATION_UNAVAILABLE".equals(exception.getMessage()) ? "ISOLATION_UNAVAILABLE" : "RUNTIME_IDENTITY_UNAVAILABLE");
        }
    }

    private CaseResult selectCase(UUID run, Prepared prepared) throws GateFailure {
        Sample sample = prepared.sample();
        Path directory = null;
        JsonNode result = null;
        GateFailure rejected = null;
        try (var work = storage.insertWorkspace(run, UUID.randomUUID())) {
            Path binary = work.selectBinaryPath(); directory = binary.getParent();
            Files.write(binary, prepared.input(), StandardOpenOption.CREATE_NEW);
            result = extractor.selectExtraction(binary);
            validate(sample, result);
            // 추출기는 입력을 읽기 전용으로 받는다. 변경된 입력을 원래 fixture 증거로 저장하지 않는다.
            try (var stream = Files.newInputStream(binary)) {
                if (!MessageDigest.isEqual(prepared.input(), stream.readNBytes(prepared.input().length + 1))) throw failure(sample.id(), "QA_INPUT_CHANGED");
            }
        } catch (GateFailure exception) { rejected = exception; }
        catch (IOException | RuntimeException exception) { rejected = failure(sample.id(), "RUNTIME_EXECUTION_FAILED"); }
        // close 실패도 성공 이력에 포함하지 않는다. 원본·소유 marker·lock 하위 폴더 전체 부재를 요구한다.
        if (directory != null && !Files.notExists(directory)) throw failure(sample.id(), "TEMPORARY_CLEANUP_FAILED");
        if (rejected != null) throw rejected;
        if (result == null) throw failure(sample.id(), "RUNTIME_RESULT_MISSING");
        String text = result.path("text").asText("");
        return new CaseResult(sample.id(), hash(prepared.input()), sample.quality(), sample.format(), hash(text.getBytes(StandardCharsets.UTF_8)),
                text.codePointCount(0, text.length()), result.path("blocks").size(), true);
    }

    private void validate(Sample sample, JsonNode result) throws GateFailure {
        if (result == null || !result.isObject() || !sample.quality().equals(result.path("qualityCode").asText())
                || !sample.format().equals(result.path("format").asText(""))
                || !AttachmentRuntimeIdentity.EXTRACTOR_VERSION.equals(result.path("extractorVersion").asText())
                || !sample.text().equals(result.path("text").asText("")) || !result.path("blocks").isArray()
                || sample.locators().size() != result.path("blocks").size()) throw failure(sample.id(), "UNEXPECTED_EXTRACTION_RESULT");
        boolean failureQuality = List.of("ENCRYPTED", "CORRUPT", "UNSUPPORTED", "LIMIT_EXCEEDED").contains(sample.quality());
        if (failureQuality ? !sample.quality().equals(result.path("errorCode").asText()) : !result.path("errorCode").isNull())
            throw failure(sample.id(), "UNEXPECTED_ERROR_CODE");
        if ("PDF".equals(sample.format()) ? !result.path("pageCount").isIntegralNumber() || result.path("pageCount").asInt() != 1
                : !result.path("pageCount").isNull()) throw failure(sample.id(), "UNEXPECTED_PAGE_COUNT");
        int offset = 0;
        String[] paragraphs = sample.text().split("\n", -1);
        for (int index = 0; index < sample.locators().size(); index++) {
            JsonNode block = result.path("blocks").get(index);
            String paragraph = paragraphs[index];
            int end = offset + paragraph.codePointCount(0, paragraph.length());
            if (!block.path("index").isIntegralNumber() || block.path("index").asInt() != index
                    || !block.path("startOffset").isIntegralNumber() || block.path("startOffset").asInt() != offset
                    || !block.path("endOffset").isIntegralNumber() || block.path("endOffset").asInt() != end
                    || !sample.locators().get(index).equals(block.path("locator").asText())
                    || !sample.locators().get(index).equals(block.path("evidenceScopeId").asText())
                    || !block.path("scopeReliable").isBoolean() || block.path("scopeReliable").asBoolean() != sample.reliable())
                throw failure(sample.id(), "UNEXPECTED_EVIDENCE_LOCATION");
            offset = end + 1;
        }
    }

    private Snapshot selectSnapshot() throws GateFailure {
        var digest = selectDigest(); savePart(digest, "suite", SUITE_VERSION);
        for (Class<?> code : List.of(AttachmentRuntimeGate.class, Sample.class, Prepared.class, Snapshot.class, CaseResult.class, Result.class,
                AttachmentTemporaryStorage.class, AttachmentTemporaryStorage.Workspace.class, AttachmentRuntimeIdentity.class, AttachmentRuntimeIdentity.Identity.class)) {
            String name = code.getName().substring(code.getName().lastIndexOf('.') + 1) + ".class";
            try (var stream = code.getResourceAsStream(name)) {
                if (stream == null) throw failure("SETUP", "QA_CODE_MISSING");
                byte[] bytes = stream.readNBytes(1024 * 1024 + 1);
                if (bytes.length > 1024 * 1024) throw failure("SETUP", "QA_CODE_LIMIT");
                savePart(digest, code.getName(), hash(bytes));
            } catch (IOException exception) {
                if (exception instanceof GateFailure known) throw known;
                throw failure("SETUP", "QA_CODE_UNREADABLE");
            }
        }
        List<Prepared> inputs = new ArrayList<>();
        for (Sample sample : selectSamples()) {
            try (var stream = AttachmentRuntimeGate.class.getResourceAsStream("/attachment-runtime-qa/" + sample.id() + ".bin")) {
                if (stream == null) throw failure(sample.id(), "QA_FIXTURE_MISSING");
                byte[] bytes = stream.readNBytes(1024 * 1024 + 1);
                if (bytes.length == 0 || bytes.length > 1024 * 1024) throw failure(sample.id(), "QA_FIXTURE_LIMIT");
                savePart(digest, sample.id(), hash(bytes));
                inputs.add(new Prepared(sample, bytes));
            } catch (IOException exception) {
                if (exception instanceof GateFailure known) throw known;
                throw failure(sample.id(), "QA_FIXTURE_UNREADABLE");
            }
        }
        return new Snapshot(HexFormat.of().formatHex(digest.digest()), List.copyOf(inputs));
    }

    private static List<Sample> selectSamples() {
        return List.of(
                new Sample("AR-001", "COMPLETE_TEXT", "PDF", "Business grant notice", List.of("page:1"), false),
                new Sample("AR-002", "OCR_REQUIRED", "PDF", "", List.of(), false),
                new Sample("AR-003", "COMPLETE_TEXT", "HWP", KOREAN, List.of("Section0:paragraph:1"), true),
                new Sample("AR-004", "ENCRYPTED", "", "", List.of(), false),
                new Sample("AR-005", "CORRUPT", "", "", List.of(), false),
                new Sample("AR-006", "COMPLETE_TEXT", "HWPX", KOREAN + "\n지원 한도\n100만원",
                        List.of("Contents/section0.xml:paragraph:1", "Contents/section0.xml:paragraph:2", "Contents/section0.xml:paragraph:3"), true),
                new Sample("AR-007", "PARTIAL_TEXT", "HWPX", KOREAN, List.of("Contents/section0.xml:paragraph:1"), true),
                new Sample("AR-008", "CORRUPT", "", "", List.of(), false),
                new Sample("AR-009", "CORRUPT", "", "", List.of(), false),
                new Sample("AR-010", "LIMIT_EXCEEDED", "", "", List.of(), false),
                new Sample("AR-011", "UNSUPPORTED", "", "", List.of(), false),
                new Sample("AR-012", "CORRUPT", "", "", List.of(), false),
                new Sample("AR-013", "COMPLETE_TEXT", "PDF", "Target business\nSupport grant",
                        List.of("page:1:paragraph:1", "page:1:paragraph:2"), true),
                new Sample("AR-014", "COMPLETE_TEXT", "PDF", "Target business Support grant\nTarget farmers Support loan",
                        List.of("page:1:row:1", "page:1:row:2"), true));
    }
    private static GateFailure failure(String id, String code) { return new GateFailure(id, code); }
    private static MessageDigest selectDigest() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (java.security.NoSuchAlgorithmException exception) { throw new IllegalStateException("SHA-256을 사용할 수 없습니다."); }
    }
    private static String hash(byte[] value) { return HexFormat.of().formatHex(selectDigest().digest(value)); }
    private static void savePart(MessageDigest digest, String key, String value) { digest.update((key + "\0" + value + "\n").getBytes(StandardCharsets.UTF_8)); }
}
