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
    private com.fasterxml.jackson.databind.node.ObjectNode failedReport() {
        var root=mapper.createObjectNode().put("scope","SYNTHETIC_WORKER_DB_CONTRACTS_V2").put("reportSchemaVersion",2);
        var result=root.putObject("result").put("status","FAILED").put("discovered",4).put("passed",3).put("failed",1)
                .put("skipped",0).put("notRun",0).put("failedContainers",0);
        var suites=result.putArray("suites");var cases=result.putArray("cases");int i=0;
        for(String name:java.util.List.of("AnnouncementAttachmentJobIntegrationTest","AnnouncementAttachmentMigrationTest",
                "AnnouncementAttachmentBackfillIntegrationTest","AnnouncementAttachmentWorkerIntegrationTest")) {
            suites.addObject().put("suite","com.saneb.db."+name).put("discovered",1).put("passed",i==0?0:1)
                    .put("failed",i==0?1:0).put("skipped",0).put("notRun",0);
            cases.addObject().put("caseIdHash",Integer.toHexString(++i).repeat(64)).put("status",i==1?"FAILED":"PASSED");
        }
        return root;
    }
    @Test void failedContractReportKeepsOnlyFixedSuiteCountsAndCaseHashes() throws Exception {
        var report=failedReport();report.put("privateException","PRIVATE_CANARY");
        var child=child(mapper.writeValueAsBytes(report),true,1);
        try {execute(child);fail("nonzero child must fail");}
        catch(AttachmentWorkerDbQaProcess.Failure failure) {
            assertThat(failure).hasMessage("QA_CHILD_FAILED").hasNoCause();
            var safe=failure.selectDiagnostics();assertThat(safe).isNotNull();assertThat(safe.discovered()).isEqualTo(4);
            assertThat(safe.failed()).isEqualTo(1);assertThat(safe.casesTruncated()).isFalse();
            assertThat(safe.cases()).singleElement().satisfies(c->assertThat(c.caseIdHash()).isEqualTo("1".repeat(64)));
            assertThat(safe.suites().getFirst().suiteCode()).isEqualTo("JOB_CONTRACTS");
            assertThat(mapper.writeValueAsString(safe)).doesNotContain("PRIVATE_CANARY","privateException","com.saneb.db");
        }
        verify(child).waitFor(5,TimeUnit.SECONDS);
    }
    @Test void forgedCountsTypesScopesAndPrivateIdentifiersDoNotEnterDiagnostics() throws Exception {
        for(String field:java.util.List.of("scope","version","floatVersion","overflowVersion","count","numericString","hash","status","suite","duplicate","countMismatch","trailing","duplicateJson")) {
            var root=failedReport();var r=(com.fasterxml.jackson.databind.node.ObjectNode)root.path("result");
            switch(field) {
                case "scope" -> root.put("scope","PRIVATE_CANARY");case "version" -> root.put("reportSchemaVersion",1);
                case "floatVersion" -> root.put("reportSchemaVersion",2.0);case "overflowVersion" -> root.put("reportSchemaVersion",4294967298L);
                case "count" -> r.put("failed",10001);
                case "numericString" -> r.put("failed","1");case "hash" -> ((com.fasterxml.jackson.databind.node.ObjectNode)r.path("cases").get(0)).put("caseIdHash","PRIVATE_CANARY");
                case "status" -> ((com.fasterxml.jackson.databind.node.ObjectNode)r.path("cases").get(0)).put("status","PRIVATE_CANARY");
                case "suite" -> ((com.fasterxml.jackson.databind.node.ObjectNode)r.path("suites").get(0)).put("suite","PRIVATE_CANARY");
                case "duplicate" -> r.withArray("cases").set(1,r.path("cases").get(0).deepCopy());
                case "countMismatch" -> ((com.fasterxml.jackson.databind.node.ObjectNode)r.path("cases").get(0)).put("status","PASSED");
                default -> { }
            }
            String json=mapper.writeValueAsString(root);
            if(field.equals("trailing"))json+=" {}";
            if(field.equals("duplicateJson"))json=json.replaceFirst("\\{","{\"reportSchemaVersion\":2,");
            assertThat(AttachmentWorkerDbQaProcess.selectFailureDiagnostics(json.getBytes(java.nio.charset.StandardCharsets.UTF_8),mapper)).as(field).isNull();
        }
    }
    @Test void passedOrNonReportFailureCannotBecomeFailedContractEvidence() throws Exception {
        var report=failedReport();report.withObject("result").put("status","PASSED");
        for(byte[] bytes:java.util.List.of(mapper.writeValueAsBytes(report),"not-json PRIVATE_CANARY".getBytes(),new byte[AttachmentWorkerDbQaProcess.MAX_OUTPUT+1]))
            assertThat(AttachmentWorkerDbQaProcess.selectFailureDiagnostics(bytes,mapper)).isNull();
        assertThat(new AttachmentWorkerDbQaProcess.Failure("QA_CHILD_FAILED").selectDiagnostics()).isNull();
    }
    @Test void failedCaseDiagnosticsAreBoundedWithoutReducingTheFailureCount() throws Exception {
        var root=failedReport();var r=root.withObject("result");r.put("discovered",40).put("passed",0).put("failed",40);
        var cases=r.withArray("cases").removeAll();
        for(int i=0;i<40;i++)cases.addObject().put("caseIdHash",String.format(java.util.Locale.ROOT,"%064x",i)).put("status","FAILED");
        for(var item:r.withArray("suites"))((com.fasterxml.jackson.databind.node.ObjectNode)item).put("discovered",10).put("passed",0).put("failed",10);
        var safe=AttachmentWorkerDbQaProcess.selectFailureDiagnostics(mapper.writeValueAsBytes(root),mapper);
        assertThat(safe).isNotNull();assertThat(safe.failed()).isEqualTo(40);assertThat(safe.cases()).hasSize(32);assertThat(safe.casesTruncated()).isTrue();
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
