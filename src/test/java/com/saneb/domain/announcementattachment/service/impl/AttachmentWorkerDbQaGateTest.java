package com.saneb.domain.announcementattachment.service.impl;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import com.saneb.domain.announcementattachment.vo.AttachmentPolicyValidationRows.Run;
import java.time.*;
import java.util.*;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

class AttachmentWorkerDbQaGateTest {
    private final ObjectMapper mapper=new ObjectMapper();
    private final AttachmentWorkerDbQaProcess process=mock(AttachmentWorkerDbQaProcess.class);
    private final AttachmentWorkerDbQaGate gate=new AttachmentWorkerDbQaGate(process,mapper);
    private final Instant start=Instant.now().minusSeconds(10),end=start.plusSeconds(5);
    private final AttachmentPolicyValidationSnapshotFactory.Runtime runtime=new AttachmentPolicyValidationSnapshotFactory.Runtime("a".repeat(64),"b".repeat(64),"c".repeat(64),identity("c".repeat(64),"a".repeat(64)));
    private final AttachmentPolicyValidationSnapshotFactory.Frozen frozen=new AttachmentPolicyValidationSnapshotFactory.Frozen("d".repeat(64),"{}",null,runtime);
    private final Run run=new Run(UUID.randomUUID(),UUID.randomUUID(),0,UUID.randomUUID(),0,frozen.hash(),"{}","RUNNING",1,UUID.randomUUID(),UUID.randomUUID(),"e".repeat(64),
            UUID.randomUUID(),OffsetDateTime.now().plusMinutes(5),null,OffsetDateTime.ofInstant(start.minusSeconds(1),ZoneOffset.UTC),OffsetDateTime.ofInstant(start.minusSeconds(1),ZoneOffset.UTC),null,true);
    static AttachmentWorkerDbQaGate.Identity identity(String code,String runtime) {
        var suites=new TreeMap<String,Integer>();AttachmentWorkerDbQaGate.SUITES.forEach(s->suites.put(s,1));
        return new AttachmentWorkerDbQaGate.Identity("f".repeat(64),code,runtime,suites,List.of("1".repeat(64),"2".repeat(64),"3".repeat(64),"4".repeat(64)));
    }
    private ObjectNode report(boolean inventory) {
        var result=mapper.createObjectNode().put("status",inventory?"INVENTORY_ONLY":"PASSED").put("discovered",4).put("passed",inventory?0:4)
                .put("failed",0).put("skipped",0).put("notRun",inventory?4:0).put("failedContainers",0);
        var suites=result.putArray("suites");runtime.workerDbQa().suites().keySet().stream().sorted().forEach(s->suites.addObject().put("suite",s).put("discovered",1)
                .put("passed",inventory?0:1).put("failed",0).put("skipped",0).put("notRun",inventory?1:0));
        var cases=result.putArray("cases");runtime.workerDbQa().caseIds().forEach(id->cases.addObject().put("caseIdHash",id).put("status",inventory?"NOT_RUN":"PASSED"));
        var report=mapper.createObjectNode().put("scope",AttachmentWorkerDbQaGate.SCOPE).put("reportSchemaVersion",2).put("runId",UUID.randomUUID().toString())
                .put("startedAt",start.plusSeconds(1).toString()).put("completedAt",end.minusSeconds(1).toString())
                .put("artifactHash",runtime.workerDbQa().artifactHash()).put("executionCodeHash",runtime.executionCodeHash());
        if(inventory) report.putNull("extractorRuntimeHash");else report.put("extractorRuntimeHash",runtime.runtimeHash());
        report.set("result",result);return report;
    }
    private JsonNode execute(ObjectNode report,boolean cleaned) {
        when(process.selectResult(eq(false),any(),any())).thenReturn(new AttachmentWorkerDbQaProcess.Result(report,start,end,cleaned));
        return gate.selectValidatedResult(frozen,run,()->true);
    }
    @Test void actualProcessResultBindsPolicyIdentityAllCasesAndCleanupAndUsesLeaseReserve() {
        var evidence=execute(report(false),true);
        assertThat(evidence.path("policyRunId").asText()).isEqualTo(run.runId().toString());
        assertThat(gate.selectValidatedEvidenceHash(evidence,frozen,run)).matches("[0-9a-f]{64}");
        verify(process).selectResult(eq(false),eq(run.leaseExpiresAt().toInstant().minusSeconds(60)),any());
        assertThat(gate.selectStepCode()).isEqualTo("WORKER_DB_RECOVERY");
    }
    @Test void inventoryNeverBecomesPassedEvidence() {
        assertThatThrownBy(()->execute(report(true),true)).hasMessage("QA_CASES_INCOMPLETE");
    }
    @Test void artifactApplicationAndRuntimeMismatchRejectsExecution() {
        for(String key:List.of("artifactHash","executionCodeHash","extractorRuntimeHash")) {
            var report=report(false).put(key,"9".repeat(64));
            assertThatThrownBy(()->execute(report,true)).isInstanceOf(AttachmentWorkerDbQaProcess.Failure.class);
        }
    }
    @Test void missingDuplicateChangedCasesAndOverstatedSuiteCountsCannotPass() {
        List<Consumer<ObjectNode>> mutations=List.of(
                r->((ArrayNode)r.path("result").path("cases")).remove(0),
                r->((ObjectNode)r.path("result").path("cases").get(0)).put("caseIdHash","2".repeat(64)),
                r->((ObjectNode)r.path("result").path("cases").get(0)).put("caseIdHash","9".repeat(64)),
                r->((ObjectNode)r.path("result").path("suites").get(0)).put("discovered",2),
                r->((ObjectNode)r.path("result")).put("passed",5),
                r->((ObjectNode)r.path("result")).put("passed","4"),
                r->((ObjectNode)r.path("result")).put("failedContainers",1),
                r->((ObjectNode)r.path("result").path("cases").get(0)).put("status","SKIPPED"));
        for(var mutation:mutations) {var value=report(false);mutation.accept(value);assertThatThrownBy(()->execute(value,true)).isInstanceOf(AttachmentWorkerDbQaProcess.Failure.class);}
    }
    @Test void rawAdditionalFieldsInvalidScopeAndOldReportSchemaAreRejected() {
        for(var report:List.of(report(false).put("rawSql","not retained"),report(false).put("scope","OTHER"),report(false).put("reportSchemaVersion",1)))
            assertThatThrownBy(()->execute(report,true)).isInstanceOf(AttachmentWorkerDbQaProcess.Failure.class);
    }
    @Test void failedCleanupCancellationAndChildTimeOutsideParentAreRejected() {
        assertThatThrownBy(()->execute(report(false),false)).hasMessage("QA_EVIDENCE_INVALID");
        assertThatThrownBy(()->execute(report(false).put("startedAt",start.minusSeconds(1).toString()),true)).hasMessage("QA_CHILD_TIME_INVALID");
        when(process.selectResult(eq(false),any(),any())).thenReturn(new AttachmentWorkerDbQaProcess.Result(report(false),start,end,true));
        assertThatThrownBy(()->gate.selectValidatedResult(frozen,run,()->false)).hasMessage("EXECUTION_STOPPED");
    }
    @Test void publicationRejectsEvidenceFromOtherRunOrSnapshotAndNeverRunsProcess() {
        var evidence=(ObjectNode)execute(report(false),true);clearInvocations(process);
        evidence.put("policyRunId",UUID.randomUUID().toString());
        assertThatThrownBy(()->gate.selectValidatedEvidenceHash(evidence,frozen,run)).hasMessage("QA_EVIDENCE_INVALID");
        evidence.put("policyRunId",run.runId().toString()).put("snapshotHash","9".repeat(64));
        assertThatThrownBy(()->gate.selectValidatedEvidenceHash(evidence,frozen,run)).hasMessage("QA_EVIDENCE_INVALID");
        verifyNoInteractions(process);
    }
    @Test void inventoryIdentityIsStableAndSortedButNeverContainsSuccessCounts() {
        var identity=AttachmentWorkerDbQaGate.selectReportIdentity(report(true),runtime.runtimeHash(),true);
        assertThat(identity).isEqualTo(runtime.workerDbQa());assertThat(identity.caseIds()).isSorted();
    }
    @Test void jsonbObjectReorderingDoesNotChangeEvidenceHashButChangedValuesDo() throws Exception {
        var evidence=execute(report(false),true);
        String expected=gate.selectValidatedEvidenceHash(evidence,frozen,run);
        var reverse=new ObjectMapper().convertValue(evidence,new com.fasterxml.jackson.core.type.TypeReference<Map<String,Object>>(){});
        var reversed=new LinkedHashMap<String,Object>();reverse.keySet().stream().sorted(Comparator.reverseOrder()).forEach(key->reversed.put(key,reverse.get(key)));
        assertThat(gate.selectValidatedEvidenceHash(mapper.valueToTree(reversed),frozen,run)).isEqualTo(expected);
        var sorted=new ObjectMapper().enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
        String saved=HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(sorted.writeValueAsBytes(mapper.convertValue(evidence,Object.class))));
        assertThat(expected).isEqualTo(saved);
    }
    @Test void completionAfterStoredStepIsRejectedByPublicationContract() {
        var evidence=execute(report(false),true);
        assertThatThrownBy(()->gate.selectValidatedEvidenceHash(evidence,frozen,run,OffsetDateTime.ofInstant(end.minusSeconds(1),ZoneOffset.UTC)))
                .hasMessage("QA_RECORD_TIME_INVALID");
        assertThat(gate.selectValidatedEvidenceHash(evidence,frozen,run,OffsetDateTime.ofInstant(end,ZoneOffset.UTC))).matches("[0-9a-f]{64}");
    }
}
