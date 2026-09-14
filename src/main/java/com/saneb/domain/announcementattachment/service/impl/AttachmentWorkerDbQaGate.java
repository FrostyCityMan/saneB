package com.saneb.domain.announcementattachment.service.impl;

import com.fasterxml.jackson.databind.*;
import com.saneb.domain.announcementattachment.extraction.AttachmentRuntimeIdentity;
import com.saneb.domain.announcementattachment.vo.AttachmentPolicyValidationRows.Run;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import java.util.function.BooleanSupplier;
import org.springframework.stereotype.Component;
import static com.saneb.domain.announcementattachment.service.impl.AttachmentWorkerDbQaProcess.Failure;

/** 실제 자식 실행과 게시 재검증이 공유하는 고정 suite/case/설치 계약. Provider 실파일 증거는 별도다. */
@Component
public final class AttachmentWorkerDbQaGate implements AttachmentPolicyAdditionalQaEvidenceVerifier {
    static final String SCOPE="SYNTHETIC_WORKER_DB_CONTRACTS_V2";
    static final Set<String> SUITES=Set.of("com.saneb.db.AnnouncementAttachmentJobIntegrationTest",
            "com.saneb.db.AnnouncementAttachmentMigrationTest","com.saneb.db.AnnouncementAttachmentBackfillIntegrationTest",
            "com.saneb.db.AnnouncementAttachmentWorkerIntegrationTest");
    private final AttachmentWorkerDbQaProcess process;
    private final ObjectMapper mapper;
    public record Identity(String artifactHash,String executionCodeHash,String extractorRuntimeHash,
            Map<String,Integer> suites,List<String> caseIds) {
        public Identity {suites=Map.copyOf(suites);caseIds=List.copyOf(caseIds);}
    }
    public AttachmentWorkerDbQaGate(AttachmentWorkerDbQaProcess process,ObjectMapper mapper) {
        this.process=process;this.mapper=mapper.copy().enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
    }
    @Override public String selectStepCode() {return "WORKER_DB_RECOVERY";}
    public Identity selectIdentity(String executionCodeHash,String runtimeHash) {
        try {
            var before=new AttachmentRuntimeIdentity(process.selectDistribution().resolve("extractor").toString()).selectIdentity();
            if(!runtimeHash.equals(before.configHash())) throw new Failure("QA_EXTRACTOR_MISMATCH");
            var result=process.selectResult(true,Instant.now().plusSeconds(30),()->!Thread.currentThread().isInterrupted());
            validateEnvelope(result.report());
            if(!result.originalRemoved() || !executionCodeHash.equals(text(result.report(),"executionCodeHash"))
                    || !result.report().path("extractorRuntimeHash").isNull()) throw new Failure("QA_APPLICATION_MISMATCH");
            var identity=selectReportIdentity(result.report(),runtimeHash,true);
            if(!runtimeHash.equals(new AttachmentRuntimeIdentity(process.selectDistribution().resolve("extractor").toString()).selectIdentity().configHash()))
                throw new Failure("QA_EXTRACTOR_CHANGED");
            return identity;
        } catch(Failure failure) {throw failure;}
        catch(Exception exception) {throw new Failure("QA_ARTIFACT_UNAVAILABLE");}
    }
    public JsonNode selectValidatedResult(AttachmentPolicyValidationSnapshotFactory.Frozen snapshot,Run run,BooleanSupplier allowed) {
        if(run==null || run.leaseExpiresAt()==null || !"RUNNING".equals(run.statusCode()) || run.leaseToken()==null)
            throw new Failure("QA_LEASE_REQUIRED");
        validateIdentity(snapshot.runtime().workerDbQa(),snapshot.runtime().executionCodeHash(),snapshot.runtime().runtimeHash());
        // inventory 재검증 최대30초 + namespace/pipe 정리 + 짧은 완료 transaction을 남긴다. lease는 연장하지 않는다.
        var result=process.selectResult(false,run.leaseExpiresAt().toInstant().minusSeconds(60),allowed);
        if(!allowed.getAsBoolean()) throw new Failure("EXECUTION_STOPPED");
        var evidence=mapper.valueToTree(Map.of("schemaVersion",1,"policyRunId",run.runId().toString(),"snapshotHash",snapshot.hash(),
                "startedAt",result.startedAt().toString(),"completedAt",result.completedAt().toString(),
                "originalRemoved",result.originalRemoved(),"report",result.report()));
        validateEvidence(evidence,snapshot,run);
        return evidence;
    }
    @Override public String selectValidatedEvidenceHash(JsonNode evidence,AttachmentPolicyValidationSnapshotFactory.Frozen snapshot,Run run) {
        // 호출자가 이미 현재 설치/입력을 재검증해 얻은 frozen만 사용한다. 이 메서드는 DB 잠금 안에서 프로세스를 실행하지 않는다.
        validateEvidence(evidence,snapshot,run);
        try {return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(mapper.writeValueAsBytes(mapper.convertValue(evidence,Object.class))));}
        catch(Exception exception) {throw new Failure("QA_EVIDENCE_HASH_FAILED");}
    }
    @Override public String selectValidatedEvidenceHash(JsonNode evidence,AttachmentPolicyValidationSnapshotFactory.Frozen snapshot,Run run,OffsetDateTime recordedAt) {
        try {
            if(recordedAt==null || Instant.parse(text(evidence,"completedAt")).isAfter(recordedAt.toInstant())) throw new Failure("QA_RECORD_TIME_INVALID");
        } catch(Failure failure) {throw failure;}
        catch(Exception exception) {throw new Failure("QA_RECORD_TIME_INVALID");}
        return selectValidatedEvidenceHash(evidence,snapshot,run);
    }
    private void validateEvidence(JsonNode evidence,AttachmentPolicyValidationSnapshotFactory.Frozen snapshot,Run run) {
        try {
            fields(evidence,Set.of("schemaVersion","policyRunId","snapshotHash","startedAt","completedAt","originalRemoved","report"));
            if(number(evidence,"schemaVersion")!=1 || !text(evidence,"policyRunId").equals(run.runId().toString())
                    || !text(evidence,"snapshotHash").equals(snapshot.hash()) || !snapshot.hash().equals(run.snapshotHash())
                    || !evidence.path("originalRemoved").isBoolean() || !evidence.path("originalRemoved").booleanValue()
                    || mapper.writeValueAsBytes(evidence).length>32768) throw new Failure("QA_EVIDENCE_INVALID");
            Instant start=Instant.parse(text(evidence,"startedAt")),end=Instant.parse(text(evidence,"completedAt"));
            if(run.startedAt()==null || start.isBefore(run.startedAt().toInstant()) || end.isBefore(start)
                    || Duration.between(start,end).compareTo(Duration.ofSeconds(600))>0
                    || (run.completedAt()!=null && end.isAfter(run.completedAt().toInstant()))) throw new Failure("QA_EXECUTION_TIME_INVALID");
            JsonNode report=evidence.path("report");validateEnvelope(report);
            Instant childStart=Instant.parse(text(report,"startedAt")),childEnd=Instant.parse(text(report,"completedAt"));
            UUID.fromString(text(report,"runId"));
            if(childStart.isBefore(start) || childEnd.isBefore(childStart) || childEnd.isAfter(end)) throw new Failure("QA_CHILD_TIME_INVALID");
            var expected=snapshot.runtime().workerDbQa();
            validateIdentity(expected,snapshot.runtime().executionCodeHash(),snapshot.runtime().runtimeHash());
            var actual=selectReportIdentity(report,snapshot.runtime().runtimeHash(),false);
            if(!expected.equals(actual)) throw new Failure("QA_ARTIFACT_OR_CASES_CHANGED");
        } catch(Failure failure) {throw failure;}
        catch(Exception exception) {throw new Failure("QA_EVIDENCE_INVALID");}
    }
    static void validateIdentity(Identity value,String codeHash,String runtimeHash) {
        if(value==null || !hash(value.artifactHash()) || !hash(codeHash) || !hash(runtimeHash)
                || !codeHash.equals(value.executionCodeHash()) || !runtimeHash.equals(value.extractorRuntimeHash())
                || !value.suites().keySet().equals(SUITES) || value.suites().values().stream().anyMatch(n->n==null || n<1 || n>10000)
                || value.caseIds().isEmpty() || value.caseIds().size()>10000 || value.caseIds().stream().anyMatch(v->!hash(v))
                || new HashSet<>(value.caseIds()).size()!=value.caseIds().size()
                || value.suites().values().stream().mapToInt(Integer::intValue).sum()!=value.caseIds().size()
                || !value.caseIds().equals(value.caseIds().stream().sorted().toList())) throw new Failure("QA_IDENTITY_INVALID");
    }
    static Identity selectReportIdentity(JsonNode report,String runtimeHash,boolean inventory) {
        JsonNode result=report.path("result");
        fields(result,Set.of("status","discovered","passed","failed","skipped","notRun","failedContainers","suites","cases"));
        int count=number(result,"discovered");
        if(count<1 || count>10000 || !text(result,"status").equals(inventory?"INVENTORY_ONLY":"PASSED")
                || number(result,"failed")!=0 || number(result,"skipped")!=0 || number(result,"failedContainers")!=0
                || number(result,"passed")!=(inventory?0:count) || number(result,"notRun")!=(inventory?count:0)) throw new Failure("QA_CASES_INCOMPLETE");
        if(!inventory && !runtimeHash.equals(text(report,"extractorRuntimeHash"))) throw new Failure("QA_EXTRACTOR_MISMATCH");
        var suites=new TreeMap<String,Integer>();
        if(!result.path("suites").isArray() || result.path("suites").size()!=4) throw new Failure("QA_SUITES_INVALID");
        for(var suite:result.path("suites")) {
            fields(suite,Set.of("suite","discovered","passed","failed","skipped","notRun"));
            int n=number(suite,"discovered");String name=text(suite,"suite");
            if(n<1 || !SUITES.contains(name) || suites.putIfAbsent(name,n)!=null || number(suite,"failed")!=0 || number(suite,"skipped")!=0
                    || number(suite,"passed")!=(inventory?0:n) || number(suite,"notRun")!=(inventory?n:0)) throw new Failure("QA_SUITES_INCOMPLETE");
        }
        if(!result.path("cases").isArray() || result.path("cases").size()!=count) throw new Failure("QA_CASES_INCOMPLETE");
        var cases=new ArrayList<String>();
        for(var item:result.path("cases")) {
            fields(item,Set.of("caseIdHash","status"));
            if(!text(item,"status").equals(inventory?"NOT_RUN":"PASSED")) throw new Failure("QA_CASES_INCOMPLETE");
            cases.add(text(item,"caseIdHash"));
        }
        Collections.sort(cases);
        var identity=new Identity(text(report,"artifactHash"),text(report,"executionCodeHash"),runtimeHash,suites,cases);
        validateIdentity(identity,identity.executionCodeHash(),runtimeHash);
        return identity;
    }
    private static void validateEnvelope(JsonNode value) {
        fields(value,Set.of("scope","runId","reportSchemaVersion","startedAt","completedAt","artifactHash","result","executionCodeHash","extractorRuntimeHash"));
        if(number(value,"reportSchemaVersion")!=2 || !SCOPE.equals(text(value,"scope"))) throw new Failure("QA_REPORT_SCOPE_INVALID");
    }
    private static void fields(JsonNode value,Set<String> expected) {
        if(!value.isObject()) throw new Failure("QA_REPORT_INVALID");
        var actual=new HashSet<String>();value.fieldNames().forEachRemaining(actual::add);
        if(!actual.equals(expected)) throw new Failure("QA_REPORT_FIELDS_INVALID");
    }
    private static String text(JsonNode value,String key) {if(!value.path(key).isTextual()) throw new Failure("QA_REPORT_TYPE_INVALID");return value.path(key).textValue();}
    private static int number(JsonNode value,String key) {if(!value.path(key).isIntegralNumber() || !value.path(key).canConvertToInt()) throw new Failure("QA_REPORT_TYPE_INVALID");return value.path(key).intValue();}
    private static boolean hash(String value) {return value!=null && value.matches("[0-9a-f]{64}");}
}
