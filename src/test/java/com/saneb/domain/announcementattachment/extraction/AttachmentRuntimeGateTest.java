package com.saneb.domain.announcementattachment.extraction;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** 격리 실행 mock의 결과 검증·정리 계약 테스트다. Linux 실제 실행의 성공 증거가 아니다. */
class AttachmentRuntimeGateTest {
    @TempDir Path root;
    private final ObjectMapper mapper = new ObjectMapper();
    private final AttachmentRuntimeIdentity identity = mock(AttachmentRuntimeIdentity.class);
    private final IsolatedAttachmentExtractor extractor = mock(IsolatedAttachmentExtractor.class);
    private final List<Path> originals = new ArrayList<>();
    private final AtomicInteger calls = new AtomicInteger();
    private Consumer<ObjectNode> alterFirst = result -> { };
    private AttachmentRuntimeGate gate;
    @BeforeEach void setup() throws Exception {
        when(identity.selectIdentity()).thenReturn(new AttachmentRuntimeIdentity.Identity("1.0.0", "a".repeat(64), 5, 100));
        when(extractor.selectExtraction(any())).thenAnswer(call -> {
            Path path = call.getArgument(0); originals.add(path);
            assertThat(Files.size(path)).isBetween(1L, 1024L * 1024);
            int index = calls.incrementAndGet();
            ObjectNode result = response(index);
            if (index == 1) alterFirst.accept(result);
            return result;
        });
        gate = new AttachmentRuntimeGate(identity, extractor, new AttachmentTemporaryStorage(root.toString()));
    }
    private ObjectNode response(int index) {
        String quality = switch (index) {
            case 1, 3, 6 -> "COMPLETE_TEXT"; case 2 -> "OCR_REQUIRED"; case 4 -> "ENCRYPTED";
            case 7 -> "PARTIAL_TEXT"; case 10 -> "LIMIT_EXCEEDED"; case 11 -> "UNSUPPORTED"; default -> "CORRUPT";
        };
        String format = switch (index) { case 1, 2 -> "PDF"; case 3 -> "HWP"; case 6, 7 -> "HWPX"; default -> null; };
        String text = switch (index) {
            case 1 -> "Business grant notice"; case 3, 7 -> "소상공인 지원금 😀";
            case 6 -> "소상공인 지원금 😀\n지원 한도\n100만원"; default -> "";
        };
        ObjectNode result = mapper.createObjectNode().put("qualityCode", quality).put("format", format)
                .put("extractorVersion", "1.0.0").put("text", text);
        if (index == 1 || index == 2) result.put("pageCount", 1); else result.putNull("pageCount");
        if (format == null) result.put("errorCode", quality); else result.putNull("errorCode");
        var blocks = result.putArray("blocks"); int offset = 0, blockIndex = 0;
        if (!text.isEmpty()) for (String paragraph : text.split("\n")) {
            String locator = index == 1 ? "page:1" : index == 3 ? "Section0:paragraph:1" : "Contents/section0.xml:paragraph:" + (blockIndex + 1);
            int end = offset + paragraph.codePointCount(0, paragraph.length());
            blocks.addObject().put("index", blockIndex++).put("startOffset", offset).put("endOffset", end)
                    .put("locator", locator).put("evidenceScopeId", locator).put("scopeReliable", index != 1);
            offset = end + 1;
        }
        return result;
    }
    private void assertClean() throws Exception {
        assertThat(originals).allSatisfy(path -> assertThat(Files.exists(path.getParent())).isFalse());
        if (Files.exists(root.resolve("announcement-attachment-tmp"))) try (var files = Files.list(root.resolve("announcement-attachment-tmp"))) {
            assertThat(files.map(path -> path.getFileName().toString()).toList()).containsExactlyInAnyOrder(".owner", ".quota.lock");
        }
    }
    @Test void executesAllFixedInputsAndReturnsOnlyHashedEvidenceAfterCleanup() throws Exception {
        var result = gate.selectValidatedResult();
        assertThat(result.caseCount()).isEqualTo(12); assertThat(calls.get()).isEqualTo(12);
        assertThat(result.scope()).isEqualTo("SYNTHETIC_INSTALLED_RUNTIME");
        assertThat(result.runtimeHash()).isEqualTo("a".repeat(64));
        assertThat(result.suiteHash()).matches("[0-9a-f]{64}"); assertThat(result.resultHash()).matches("[0-9a-f]{64}");
        assertThat(result.cases()).extracting(AttachmentRuntimeGate.CaseResult::caseId)
                .containsExactly("AR-001", "AR-002", "AR-003", "AR-004", "AR-005", "AR-006", "AR-007", "AR-008", "AR-009", "AR-010", "AR-011", "AR-012");
        assertThat(result.cases()).allSatisfy(row -> { assertThat(row.originalRemoved()).isTrue(); assertThat(row.inputHash()).matches("[0-9a-f]{64}"); });
        assertThat(result.toString()).doesNotContain("소상공인", "Business grant notice", root.toString());
        assertThat(result.completedAt()).isAfterOrEqualTo(result.startedAt()); verify(identity, times(2)).selectIdentity(); assertClean();
    }
    @Test void storedRuntimeEvidenceMustMatchCurrentFixturesAndDoesNotReexecuteExtractor() throws Exception {
        var result=gate.selectValidatedResult();clearInvocations(extractor);
        gate.validateStoredResult(result,"a".repeat(64));verifyNoInteractions(extractor);assertClean();
        assertThatThrownBy(()->gate.validateStoredResult(result,"b".repeat(64))).hasMessageContaining("QA_RESULT_BINDING_INVALID");
        var rows=new ArrayList<>(result.cases());var first=rows.getFirst();rows.set(0,new AttachmentRuntimeGate.CaseResult(first.caseId(),first.inputHash(),first.qualityCode(),first.format(),first.textHash(),first.characterCount(),first.blockCount(),false));
        var altered=new AttachmentRuntimeGate.Result(result.runId(),result.scope(),result.suiteVersion(),result.suiteHash(),result.runtimeHash(),result.extractorVersion(),result.resultHash(),12,rows,result.startedAt(),result.completedAt());
        assertThatThrownBy(()->gate.validateStoredResult(altered,"a".repeat(64))).hasMessageContaining("QA_STORED_CASE_CHANGED");verifyNoInteractions(extractor);
    }
    @ParameterizedTest @ValueSource(strings={"quality", "format", "text", "version", "page", "error", "index", "start", "end", "locator", "scope", "reliable", "missing-block"})
    void rejectsIncorrectQualityTextOrEvidenceCoordinatesAndDeletesOriginal(String field) throws Exception {
        alterFirst = result -> {
            ObjectNode block = (ObjectNode) result.path("blocks").get(0);
            switch (field) {
                case "quality" -> result.put("qualityCode", "PARTIAL_TEXT"); case "format" -> result.put("format", "HWP");
                case "text" -> result.put("text", "Wrong"); case "version" -> result.put("extractorVersion", "old");
                case "page" -> result.putNull("pageCount"); case "error" -> result.put("errorCode", "FAILED");
                case "index" -> block.put("index", 1); case "start" -> block.put("startOffset", 1);
                case "end" -> block.put("endOffset", 1); case "locator" -> block.put("locator", "page:2");
                case "scope" -> block.put("evidenceScopeId", "wrong"); case "reliable" -> block.put("scopeReliable", true);
                default -> result.putArray("blocks");
            }
        };
        assertThatThrownBy(() -> gate.selectValidatedResult()).isInstanceOf(AttachmentRuntimeGate.GateFailure.class).hasMessageStartingWith("AR-001:");
        assertThat(calls.get()).isEqualTo(1); assertClean();
    }
    @Test void runtimeChangeAfterExecutionCannotBecomeSuccess() throws Exception {
        when(identity.selectIdentity()).thenReturn(new AttachmentRuntimeIdentity.Identity("1.0.0", "a".repeat(64), 5, 100),
                new AttachmentRuntimeIdentity.Identity("1.0.0", "b".repeat(64), 5, 100));
        assertThatThrownBy(() -> gate.selectValidatedResult()).hasMessage("FINALIZE:RUNTIME_OR_SUITE_CHANGED"); assertClean();
    }
    @Test void unavailableIdentityMakesNoTemporaryFileOrParserCallAndDoesNotLeakException() throws Exception {
        when(identity.selectIdentity()).thenThrow(new IOException("sensitive-internal-path"));
        assertThatThrownBy(() -> gate.selectValidatedResult()).hasMessage("SETUP:RUNTIME_IDENTITY_UNAVAILABLE");
        verifyNoInteractions(extractor); try (var entries = Files.list(root)) { assertThat(entries.findAny()).isEmpty(); }
    }
    @Test void thrownExtractorFailureIsSanitizedAndGateCanRunAgain() throws Exception {
        doAnswer(call -> { originals.add(call.getArgument(0)); throw new IOException("sensitive-document-error"); })
                .doAnswer(call -> response(calls.incrementAndGet())).when(extractor).selectExtraction(any());
        assertThatThrownBy(() -> gate.selectValidatedResult()).hasMessage("AR-001:RUNTIME_EXECUTION_FAILED"); assertClean();
        assertThat(gate.selectValidatedResult().caseCount()).isEqualTo(12); assertClean();
    }
    @Test void concurrentExecutionIsRejectedWithoutSecondParserOrWorkspace() throws Exception {
        alterFirst = result -> assertThatThrownBy(() -> gate.selectValidatedResult()).hasMessage("SETUP:RUNTIME_QA_BUSY");
        assertThat(gate.selectValidatedResult().caseCount()).isEqualTo(12); assertThat(calls.get()).isEqualTo(12); assertClean();
    }
    @Test void unexpectedWorkspaceFileBlocksCleanupAndCannotProduceSuccess() throws Exception {
        alterFirst = result -> {
            try { Files.writeString(originals.getFirst().getParent().resolve("foreign-file"), "QA"); }
            catch (IOException exception) { throw new IllegalStateException(exception); }
        };
        assertThatThrownBy(() -> gate.selectValidatedResult()).hasMessage("AR-001:TEMPORARY_CLEANUP_FAILED");
        assertThat(Files.exists(originals.getFirst().getParent().resolve("foreign-file"))).isTrue();
    }
    @Test void changedInputCannotKeepOriginalFixtureHash() throws Exception {
        alterFirst = result -> {
            try { Files.writeString(originals.getFirst(), "changed QA bytes"); }
            catch (IOException exception) { throw new IllegalStateException(exception); }
        };
        assertThatThrownBy(() -> gate.selectValidatedResult()).hasMessage("AR-001:QA_INPUT_CHANGED"); assertClean();
    }
    @Test @EnabledOnOs(OS.WINDOWS) void actualWindowsIdentityFailsClosedWithoutHostParserFallback() throws Exception {
        var real = new AttachmentRuntimeGate(new AttachmentRuntimeIdentity("unused"), extractor, new AttachmentTemporaryStorage(root.toString()));
        assertThatThrownBy(() -> real.selectValidatedResult()).hasMessage("SETUP:ISOLATION_UNAVAILABLE"); verifyNoInteractions(extractor);
    }
    @Test void cancellationIsPreservedAndRunsNoParser() throws Exception {
        Thread.currentThread().interrupt();
        try { assertThatThrownBy(() -> gate.selectValidatedResult()).hasMessage("AR-001:CANCELLED"); verifyNoInteractions(extractor); }
        finally { Thread.interrupted(); }
        assertClean();
    }
    @Test void leaseGuardStopsBeforeAnotherFileAndCleansCurrentOriginal() throws Exception {
        assertThatThrownBy(() -> gate.selectValidatedResult(() -> calls.get()==0)).hasMessage("AR-002:CANCELLED");
        assertThat(calls.get()).isEqualTo(1); assertClean();
    }
    @Test void suiteIdentityReadDoesNotExecuteParserOrCreateOriginal() throws Exception {
        assertThat(gate.selectSuiteHash()).matches("[0-9a-f]{64}").isEqualTo(gate.selectSuiteHash());
        verifyNoInteractions(identity,extractor); assertClean();
    }
}
