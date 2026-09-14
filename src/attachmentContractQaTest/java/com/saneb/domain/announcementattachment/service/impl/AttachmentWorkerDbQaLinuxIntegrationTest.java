package com.saneb.domain.announcementattachment.service.impl;

import static org.assertj.core.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.domain.announcementattachment.extraction.AttachmentRuntimeIdentity;
import com.saneb.domain.announcementattachment.vo.AttachmentPolicyValidationRows.Run;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/** 실제 정책 부모 -> private namespace -> 임시 PG/worker -> 전체 결과 -> 게시 검증기 경로. 운영 DB는 사용하지 않는다. */
@EnabledIfEnvironmentVariable(named="SANEB_ATTACHMENT_POLICY_DB_QA",matches="true")
@Timeout(600)
class AttachmentWorkerDbQaLinuxIntegrationTest {
    private final Path root=Path.of("build/install/attachment-contract-qa").toAbsolutePath();
    private final ObjectMapper mapper=new ObjectMapper();
    private final AttachmentWorkerDbQaProcess process=new AttachmentWorkerDbQaProcess(root.toString(),mapper);
    @BeforeAll static void reportThreadBudgetWithoutProcessIdentities() throws Exception {
        var self=Files.readAllLines(Path.of("/proc/self/status"));
        String uid=self.stream().filter(line->line.startsWith("Uid:")).findFirst().orElseThrow().strip().split("\\s+")[1];
        int observed=0;
        try(var entries=Files.list(Path.of("/proc"))) {
            for(var entry:entries.filter(path->path.getFileName().toString().matches("[0-9]+" )).limit(4096).toList()) {
                try {
                    var status=Files.readAllLines(entry.resolve("status"));
                    String owner=status.stream().filter(line->line.startsWith("Uid:")).findFirst().orElse("").strip();
                    if(!owner.isEmpty() && uid.equals(owner.split("\\s+")[1]))
                        observed+=Integer.parseInt(status.stream().filter(line->line.startsWith("Threads:")).findFirst().orElseThrow().strip().split("\\s+")[1]);
                } catch(java.io.IOException ignored) { /* 스냅샷 도중 종료된 프로세스는 집계할 수 없다. */ }
            }
        }
        // 진단용 집계만 출력한다. UID/PID/명령/환경/경로는 출력하지 않으며 통과 근거로 사용하지 않는다.
        System.out.println("QA_SAME_UID_THREADS_OBSERVED="+observed);
        System.out.println("QA_PARENT_JVM_THREADS="+java.lang.management.ManagementFactory.getThreadMXBean().getThreadCount());
    }
    private Set<String> workDirectories() throws Exception {
        try(var paths=Files.list(Path.of("/tmp"))) {
            return paths.map(p->p.getFileName().toString()).filter(p->p.startsWith("saneb-policy-db-qa-")).collect(java.util.stream.Collectors.toSet());
        }
    }
    @Test void parentRunsEveryPackagedContractAndRevalidatesEvidenceAfterCleanup() throws Exception {
        assertThat(System.getProperty("os.name")).isEqualTo("Linux");
        var before=workDirectories();var gate=new AttachmentWorkerDbQaGate(process,mapper);
        String code=new AttachmentApplicationCodeFingerprint(mapper).selectVerifiedHash();
        String extractor=new AttachmentRuntimeIdentity(root.resolve("extractor").toString()).selectIdentity().configHash();
        var identity=gate.selectIdentity(code,extractor);
        var installed=new AttachmentPolicyValidationSnapshotFactory.Runtime(extractor,"b".repeat(64),code,identity);
        var frozen=new AttachmentPolicyValidationSnapshotFactory.Frozen("c".repeat(64),"{}",null,installed);
        var now=OffsetDateTime.now();
        var run=new Run(UUID.randomUUID(),UUID.randomUUID(),0,UUID.randomUUID(),0,frozen.hash(),"{}","RUNNING",1,UUID.randomUUID(),UUID.randomUUID(),"d".repeat(64),
                UUID.randomUUID(),now.plusMinutes(8),null,now,now,null,true);
        var evidence=gate.selectValidatedResult(frozen,run,()->true);
        assertThat(evidence.path("report").path("result").path("passed").intValue()).isEqualTo(identity.caseIds().size());
        assertThat(evidence.path("originalRemoved").booleanValue()).isTrue();
        assertThat(gate.selectValidatedEvidenceHash(evidence,frozen,run)).matches("[0-9a-f]{64}");
        assertThat(mapper.writeValueAsBytes(evidence).length).isLessThanOrEqualTo(32768);
        assertThat(workDirectories()).isEqualTo(before);
    }
    @Test void cancellationTerminatesOwnedNamespaceAndRemovesPartialTemporaryDatabase() throws Exception {
        assertThat(System.getProperty("os.name")).isEqualTo("Linux");
        var before=workDirectories();var polls=new AtomicInteger();
        assertThatThrownBy(()->process.selectResult(false,Instant.now().plusSeconds(30),()->polls.incrementAndGet()<5))
                .isInstanceOf(AttachmentWorkerDbQaProcess.Failure.class).hasMessage("EXECUTION_STOPPED");
        assertThat(workDirectories()).isEqualTo(before);
    }
}
