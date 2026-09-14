package com.saneb.domain.announcementattachment.service.impl;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import com.fasterxml.jackson.databind.*;
import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class AttachmentWorkerDbQaProcessTest {
    private final ObjectMapper mapper=new ObjectMapper().enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .enable(com.fasterxml.jackson.core.JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
    private Process child(byte[] output,boolean finishes,int exit) throws Exception {
        Process value=mock(Process.class);
        when(value.getInputStream()).thenReturn(new ByteArrayInputStream(output));when(value.getErrorStream()).thenReturn(InputStream.nullInputStream());
        when(value.getOutputStream()).thenReturn(new ByteArrayOutputStream());when(value.exitValue()).thenReturn(exit);
        when(value.descendants()).thenAnswer(c->Stream.empty());when(value.isAlive()).thenReturn(!finishes);
        when(value.waitFor(anyLong(),any())).thenAnswer(c->{Thread.sleep(2);return finishes;});
        when(value.waitFor(5,TimeUnit.SECONDS)).thenReturn(true);
        return value;
    }
    private AttachmentWorkerDbQaProcess.Result execute(Process child) {
        return AttachmentWorkerDbQaProcess.selectProcessResult(()->child,Instant.now().plusSeconds(3),30,()->true,mapper);
    }
    @Test void outputIsReadConcurrentlyAndSuccessIsNotYetMarkedAsFilesystemCleanup() throws Exception {
        var result=execute(child("{\"status\":\"PASSED\"}".getBytes(),true,0));
        assertThat(result.report().path("status").asText()).isEqualTo("PASSED");assertThat(result.originalRemoved()).isFalse();
    }
    @Test void cancelledExecutionKillsOnlyOwnedProcessAndDoesNotReportSuccess() throws Exception {
        var child=child("{}".getBytes(),false,0);AtomicInteger polls=new AtomicInteger();
        assertThatThrownBy(()->AttachmentWorkerDbQaProcess.selectProcessResult(()->child,Instant.now().plusSeconds(5),30,()->polls.incrementAndGet()<3,mapper))
                .hasMessage("EXECUTION_STOPPED");verify(child).destroyForcibly();verify(child).waitFor(5,TimeUnit.SECONDS);
    }
    @Test void expiredDeadlineNeverLaunchesChild() {
        AtomicInteger launches=new AtomicInteger();
        assertThatThrownBy(()->AttachmentWorkerDbQaProcess.selectProcessResult(()->{launches.incrementAndGet();return null;},Instant.now().minusSeconds(1),30,()->true,mapper))
                .hasMessage("EXECUTION_STOPPED");assertThat(launches.get()).isZero();
    }
    @Test void monotonicTimeoutTerminatesChild() throws Exception {
        var child=child("{}".getBytes(),false,0);
        assertThatThrownBy(()->AttachmentWorkerDbQaProcess.selectProcessResult(()->child,Instant.now().plusMillis(40),30,()->true,mapper))
                .hasMessage("QA_DEADLINE_EXCEEDED");verify(child).destroyForcibly();
    }
    @Test void oversizedOutputTerminatesWithoutWaitingForFullChildRun() throws Exception {
        var child=child(new byte[AttachmentWorkerDbQaProcess.MAX_OUTPUT+1],false,0);
        assertThatThrownBy(()->execute(child)).hasMessage("QA_OUTPUT_LIMIT");verify(child).destroyForcibly();
    }
    @Test void failedChildAndNonObjectDuplicateOrTrailingJsonCannotBecomeEvidence() throws Exception {
        assertThatThrownBy(()->execute(child("{}".getBytes(),true,1))).hasMessage("QA_CHILD_FAILED");
        for(String value:new String[]{"[]","null","{} {}","{\"status\":1,\"status\":2}","not-json"})
            assertThatThrownBy(()->execute(child(value.getBytes(),true,0))).isInstanceOf(AttachmentWorkerDbQaProcess.Failure.class);
    }
    @Test void childStartupDiagnosticsReturnOnlyFixedCodesNeverRawOutput() throws Exception {
        String[][] cases={
                {"bwrap: fork: Resource temporarily unavailable","QA_CHILD_PROCESS_LIMIT"},
                {"Failed to start thread: pthread_create failed (EAGAIN)","QA_CHILD_PROCESS_LIMIT"},
                {"Could not reserve enough space for object heap","QA_CHILD_MEMORY_LIMIT"},
                {"Error: Could not find or load main class private.example.Main","QA_CHILD_CLASS_LOADING_FAILED"},
                {"bwrap: Creating new namespace failed: Operation not permitted","QA_ISOLATION_PERMISSION_FAILED"},
                {"bwrap: Can't find source path /private/example: No such file or directory","QA_ISOLATION_MOUNT_FAILED"},
                {"bwrap: unknown failure","QA_ISOLATION_START_FAILED"},
                {"unrecognized private diagnostic","QA_CHILD_FAILED"}
        };
        for(var entry:cases) {
            var child=child("{}".getBytes(),true,1);
            when(child.getErrorStream()).thenReturn(new ByteArrayInputStream((entry[0]+"\nprivate-marker-do-not-persist").getBytes()));
            assertThatThrownBy(()->execute(child)).hasMessage(entry[1]).hasNoCause();
        }
        assertThatThrownBy(()->execute(child("pthread_create failed".getBytes(),true,1))).hasMessage("QA_CHILD_PROCESS_LIMIT");
    }
    @Test void malformedSuccessfulOutputIsStillRejectedWithoutStrippingJvmWarnings() throws Exception {
        assertThatThrownBy(()->execute(child("not-json private-marker".getBytes(),true,0))).hasMessage("QA_REPORT_PARSE_FAILED").hasNoCause();
        assertThatThrownBy(()->execute(child("[warning] pthread_create failed\n{}".getBytes(),true,0))).hasMessage("QA_CHILD_PROCESS_LIMIT").hasNoCause();
        assertThatThrownBy(()->execute(child("{} {}".getBytes(),true,0))).hasMessage("QA_REPORT_PARSE_FAILED").hasNoCause();
    }
    @Test void startFailureDoesNotExposeCommandPathOrCause() {
        assertThatThrownBy(()->AttachmentWorkerDbQaProcess.selectProcessResult(()->{throw new IOException("private-path-marker");},
                Instant.now().plusSeconds(3),30,()->true,mapper)).hasMessage("QA_PROCESS_START_FAILED").hasNoCause();
    }
    @Test void pipeSetupFailureRemainsDifferentFromProcessStartFailure() throws Exception {
        var child=child("{}".getBytes(),true,0);
        when(child.getOutputStream()).thenReturn(new OutputStream() {
            @Override public void write(int value) { }
            @Override public void close() throws IOException { throw new IOException("private-pipe-marker"); }
        });
        assertThatThrownBy(()->execute(child)).hasMessage("QA_PROCESS_IO_FAILED").hasNoCause();
        verify(child).waitFor(5,TimeUnit.SECONDS);
    }
    @Test void oversizedStderrTerminatesWithoutWaitingForChildOrPersistingOutput() throws Exception {
        var child=child("{}".getBytes(),false,0);
        when(child.getErrorStream()).thenReturn(new ByteArrayInputStream(new byte[AttachmentWorkerDbQaProcess.MAX_ERROR_OUTPUT+1]));
        assertThatThrownBy(()->execute(child)).hasMessage("QA_OUTPUT_LIMIT");verify(child).destroyForcibly();
    }
    @Test void stderrCannotTurnFailedChildIntoSuccessOrChangeValidSuccessReport() throws Exception {
        var failed=child("{\"status\":\"PASSED\"}".getBytes(),true,1);
        assertThatThrownBy(()->execute(failed)).hasMessage("QA_CHILD_FAILED");
        var success=child("{\"status\":\"PASSED\"}".getBytes(),true,0);
        when(success.getErrorStream()).thenReturn(new ByteArrayInputStream("private-marker-do-not-persist".getBytes()));
        assertThat(execute(success).report().toString()).isEqualTo("{\"status\":\"PASSED\"}");
    }
    @Test void inabilityToTerminateOwnedNamespaceOverridesChildSuccess() throws Exception {
        var child=child("{}".getBytes(),true,0);when(child.waitFor(5,TimeUnit.SECONDS)).thenReturn(false);
        assertThatThrownBy(()->execute(child)).hasMessage("QA_PROCESS_CLEANUP_FAILED");
    }
    @Test void interruptIsPreservedAndChildIsNotLaunched() {
        try {
            Thread.currentThread().interrupt();
            assertThatThrownBy(()->AttachmentWorkerDbQaProcess.selectProcessResult(()->{throw new AssertionError("must not launch");},Instant.now().plusSeconds(5),30,
                    ()->!Thread.currentThread().isInterrupted(),mapper)).hasMessage("EXECUTION_STOPPED");
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {Thread.interrupted();}
    }
    @Test void commandRetainsIsolationBoundsAndSharesOnlyQaSubdirectories() throws Exception {
        Path root=Path.of("/isolated-qa"),work=Path.of("/tmp/saneb-policy-db-qa-test");
        var command=AttachmentWorkerDbQaProcess.selectCommand(root,Path.of(System.getProperty("java.home")),work,false);
        assertThat(command).contains("--unshare-all","--die-with-parent","--clearenv","--cap-drop","ALL","--as=2147483648","--cpu=480","/qa/lib/*")
                .doesNotContain(root.toString(),"/opt","/home","--share-net","/bin/sh","bash","--inventory");
        assertThat(AttachmentWorkerDbQaProcess.selectCommand(root,Path.of(System.getProperty("java.home")),work,true)).last().isEqualTo("--inventory");
    }
    @Test void nonLinuxCannotUseHostFallback() {
        if("Linux".equals(System.getProperty("os.name"))) return;
        assertThatThrownBy(()->new AttachmentWorkerDbQaProcess("/absent-qa",mapper).selectResult(false,Instant.now().plusSeconds(5),()->true))
                .hasMessage("ISOLATION_UNAVAILABLE");
    }
}
