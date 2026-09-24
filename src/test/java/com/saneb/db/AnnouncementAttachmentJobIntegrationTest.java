package com.saneb.db;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.domain.announcementattachment.extraction.AttachmentRuntimeIdentity;
import com.saneb.common.error.ApiException;
import com.saneb.config.typehandler.UuidTypeHandler;
import com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentJobDao;
import com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentEvidenceDao;
import com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentEvaluationDao;
import com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentReviewDao;
import com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentRoleDao;
import com.saneb.domain.announcementattachment.dto.AttachmentRoleRequest;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentRoleService;
import com.saneb.domain.announcementattachment.service.impl.AnnouncementAttachmentRoleServiceImpl;
import com.saneb.domain.announcementattachment.dto.AttachmentReviewRequests;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentReviewService;
import com.saneb.domain.announcementattachment.service.impl.AnnouncementAttachmentReviewServiceImpl;
import com.saneb.domain.announcement.dao.AnnouncementDao;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentEvaluationService;
import com.saneb.domain.announcementattachment.service.impl.AnnouncementAttachmentEvaluationServiceImpl;
import com.saneb.domain.announcementsource.dao.AnnouncementSourceRuleReleaseDao;
import com.saneb.domain.announcementsource.service.AnnouncementSourceRuleReleaseService;
import com.saneb.domain.announcementsource.service.impl.AnnouncementSourceRuleReleaseServiceImpl;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentEvidenceService;
import com.saneb.domain.announcementattachment.service.impl.AnnouncementAttachmentEvidenceServiceImpl;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentReadService;
import com.saneb.domain.announcementattachment.service.impl.AnnouncementAttachmentReadServiceImpl;
import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentJobService;
import com.saneb.domain.announcementattachment.service.AttachmentLegacyPathGuard;
import com.saneb.domain.announcementsource.dao.AnnouncementSourceDao;
import com.saneb.domain.announcementattachment.service.impl.AnnouncementAttachmentJobServiceImpl;
import com.saneb.domain.announcementattachment.vo.AttachmentExecutionSnapshot;
import com.saneb.domain.announcementattachment.vo.AttachmentFailureCode;
import com.saneb.domain.announcementattachment.vo.AttachmentJobReservation;
import com.saneb.domain.announcementattachment.vo.AttachmentJobRow;
import com.saneb.domain.announcementattachment.vo.AttachmentResourceLease;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

/** 외부 DB/네트워크를 사용하지 않는 실제 PostgreSQL + MyBatis + Spring transaction 검증. */
@EnabledIfEnvironmentVariable(named = "SANEB_ATTACHMENT_JOB_TEST", matches = "true")
class AnnouncementAttachmentJobIntegrationTest {
    private static EmbeddedPostgres postgres;
    private static AnnotationConfigApplicationContext context;
    private static JdbcTemplate sql;
    private static AnnouncementAttachmentJobService service;
    private static AnnouncementAttachmentJobDao dao;
    private static AnnouncementAttachmentEvidenceService evidenceService;
    private static AnnouncementAttachmentEvidenceDao evidenceDao;
    private static UUID actor;
    private static UUID release;
    private UUID policy;
    private static final com.saneb.domain.announcementattachment.discovery.BizInfoAttachmentDiscoveryProfile PROFILE =
            new com.saneb.domain.announcementattachment.discovery.BizInfoAttachmentDiscoveryProfile();
    private static final String PROFILE_HASH = PROFILE.selectProfileHash();
    private static final String CONFIG_HASH = "b".repeat(64);
    private static final AttachmentExecutionSnapshot EXECUTION = new AttachmentExecutionSnapshot(
            PROFILE.selectProfileCode(), PROFILE_HASH, "attachment-1.0.0", AttachmentRuntimeIdentity.EXTRACTOR_VERSION, CONFIG_HASH);

    @BeforeAll static void startIsolatedDatabase() throws Exception {
        postgres = EmbeddedPostgres.builder().setPort(0).setServerConfig("listen_addresses", "127.0.0.1").start();
        try {
            DataSource dataSource = postgres.getPostgresDatabase();
            Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();
            sql = new JdbcTemplate(dataSource);
            actor = UUID.randomUUID();
            sql.update("INSERT INTO users(id,login_id,password_hash,name,status_code,password_reset_required) VALUES (?,?,?,'첨부 작업 검증','ACTIVE',false)",
                    actor, "attachment-job-fixture", "unused-fixture-hash");
            release = sql.queryForObject("SELECT id FROM announcement_source_classification_rule_releases ORDER BY version_no LIMIT 1", UUID.class);
            sql.update("UPDATE announcement_source_classification_rule_releases SET release_status_code='ACTIVE',activated_at=now(),rule_snapshot_hash=repeat('c',64) WHERE id=?", release);
            context = new AnnotationConfigApplicationContext();
            context.registerBean(DataSource.class, () -> dataSource);
            context.register(TestConfiguration.class);
            context.refresh();
            service = context.getBean(AnnouncementAttachmentJobService.class);
            dao = context.getBean(AnnouncementAttachmentJobDao.class);
            evidenceService = context.getBean(AnnouncementAttachmentEvidenceService.class);
            evidenceDao = context.getBean(AnnouncementAttachmentEvidenceDao.class);
        } catch (Exception exception) {
            closeResources();
            throw exception;
        }
    }

    @AfterAll static void closeResources() throws Exception {
        try { if (context != null) context.close(); }
        finally { if (postgres != null) postgres.close(); }
    }

    @BeforeEach void seedIsolatedPolicy() throws Exception {
        // 이 클래스가 소유한 임시 DB에만 실행한다. 운영 데이터/계정과 연결하지 않는다.
        // 불변 이력의 DELETE 보호를 유지하며, 테스트 소유 QA/게시 준비 테이블만 함께 초기화한다.
        sql.execute("TRUNCATE announcement_attachment_provider_qa_run_plans,announcement_attachment_provider_qa_cases,announcement_attachment_provider_qa_runs,announcement_attachment_policy_publications,announcement_attachment_policy_publication_scope_items,announcement_attachment_policy_publication_scopes,announcement_attachment_policy_validation_steps,announcement_attachment_resource_leases,announcement_attachment_policy_validation_runs");
        // 테스트가 만든 loopback 임시 DB만 초기화한다. 관리 batch의 DELETE 방어를 우회하는 운영 옵션은 두지 않는다.
        sql.execute("TRUNCATE announcement_attachment_batches CASCADE");
        sql.update("DELETE FROM announcement_source_links");
        sql.update("DELETE FROM announcement_source_snapshots");
        sql.update("DELETE FROM announcement_attachment_collection_plans");
        sql.update("DELETE FROM announcement_attachment_policy_checks");
        sql.update("DELETE FROM announcement_attachment_policies");
        sql.update("UPDATE announcement_source_classification_rule_releases SET rule_snapshot_hash=repeat('c',64) WHERE id=?",release);
        policy = UUID.randomUUID();
        String settings = new ObjectMapper().writeValueAsString(java.util.Map.of(
                "engineVersion", EXECUTION.engineVersion(), "extractorVersion", EXECUTION.extractorVersion(),
                "extractorConfigHash", CONFIG_HASH, "maximumSourceBytes", 80L * 1024 * 1024));
        String manifest = new ObjectMapper().writeValueAsString(List.of(java.util.Map.of(
                "providerCode", "BIZINFO", "profileCode", EXECUTION.profileCode(), "profileHash", PROFILE_HASH)));
        sql.update("""
                INSERT INTO announcement_attachment_policies
                (id,policy_code,version_no,policy_status_code,mode_code,rule_release_id,policy_hash,
                 settings_json,profile_manifest_json,created_by,published_at)
                VALUES (?, ?, 1, 'ACTIVE','COLLECT_ONLY',?,repeat('d',64),CAST(? AS jsonb),CAST(? AS jsonb),?,now())
                """, policy, policy.toString(), release, settings, manifest, actor);
    }

    @Test void att029And030ReserveIdempotentlyWithoutChangingBaseOrAllowingDifferentRequests() {
        var request = selectRequest();
        var first = service.insertAttachmentJob(request);
        var repeated = service.insertAttachmentJob(request);
        assertThat(repeated.jobId()).isEqualTo(first.jobId());
        assertThat(first.expectedAttachmentVersion()).isEqualTo(1);
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_attachment_jobs", Integer.class)).isEqualTo(1);
        assertThat(sql.queryForObject("SELECT classification_row_version FROM announcement_source_snapshots WHERE id=?", Integer.class, request.sourceId())).isZero();
        assertThat(sql.queryForObject("SELECT is_attachment_review_required FROM announcement_source_snapshots WHERE id=?", Boolean.class, request.sourceId())).isFalse();
        var changed = new AttachmentJobReservation(request.sourceId(), policy, request.expectedBaseDecisionId(),
                1, 0, request.idempotencyKey(), EXECUTION);
        assertThatThrownBy(() -> service.insertAttachmentJob(changed)).isInstanceOf(ApiException.class)
                .hasMessageContaining("멱등 키");
        assertThat(dao.selectJobDetails(first.jobId()).requestHash()).isEqualTo(first.requestHash());
    }

    @Test void att029SimultaneousSameKeyReturnsExactlyOneJob() throws Exception {
        var request = selectRequest();
        var gate = new CountDownLatch(1);
        Callable<UUID> reserve = () -> { gate.await(5, TimeUnit.SECONDS); return service.insertAttachmentJob(request).jobId(); };
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(reserve);
            var second = executor.submit(reserve);
            gate.countDown();
            assertThat(first.get(10, TimeUnit.SECONDS)).isEqualTo(second.get(10, TimeUnit.SECONDS));
        }
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_attachment_jobs", Integer.class)).isEqualTo(1);
    }

    @Test void att031TwoWorkersCannotClaimTheSameJob() throws Exception {
        service.insertAttachmentJob(selectRequest());
        var gate = new CountDownLatch(1);
        Callable<Boolean> claim = () -> { gate.await(5, TimeUnit.SECONDS); return service.saveNextJobClaim().isPresent(); };
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(claim);
            var second = executor.submit(claim);
            gate.countDown();
            assertThat(List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true, false);
        }
    }

    @Test void att031ExpiredOwnerCannotHeartbeatChargeOrFinishAndAttemptsAreBounded() {
        var reservation = service.insertAttachmentJob(selectRequest());
        var first = service.saveNextJobClaim().orElseThrow();
        assertThat(service.saveDownloadBytes(first.jobId(), first.leaseToken(), 123)).isTrue();
        updateLeaseExpired(first);
        assertThat(service.saveJobHeartbeat(first.jobId(), first.leaseToken())).isFalse();
        assertThat(service.saveDownloadBytes(first.jobId(), first.leaseToken(), 1)).isFalse();
        assertThat(service.saveJobFailure(first.jobId(), first.leaseToken(), AttachmentFailureCode.NETWORK_TIMEOUT)).isFalse();
        assertThat(service.saveNextJobClaim()).isEmpty();
        assertThat(dao.selectJobDetails(first.jobId()).jobStatusCode()).isEqualTo("RETRY_WAIT");
        for (int expectedAttempt = 2; expectedAttempt <= 3; expectedAttempt++) {
            sql.update("UPDATE announcement_attachment_jobs SET next_attempt_at=now()-interval '1 second' WHERE id=?", first.jobId());
            var next = service.saveNextJobClaim().orElseThrow();
            assertThat(next.attemptCount()).isEqualTo(expectedAttempt);
            assertThat(next.leaseToken()).isNotEqualTo(first.leaseToken());
            assertThat(next.reservedDownloadBytes()).isEqualTo(123);
            assertThat(service.saveJobFailure(first.jobId(), first.leaseToken(), AttachmentFailureCode.EXTRACTION_FAILED)).isFalse();
            updateLeaseExpired(next);
            assertThat(service.saveNextJobClaim()).isEmpty();
        }
        assertThat(dao.selectJobDetails(reservation.jobId()).jobStatusCode()).isEqualTo("FAILED");
        assertThat(dao.selectJobDetails(reservation.jobId()).attemptCount()).isEqualTo(3);
    }

    @Test void att024And060CumulativeBytesCannotExceedHardCapOrBeRefunded() {
        service.insertAttachmentJob(selectRequest());
        var job = service.saveNextJobClaim().orElseThrow();
        long cap = 80L * 1024 * 1024;
        assertThat(service.saveDownloadBytes(job.jobId(), job.leaseToken(), cap - 1)).isTrue();
        assertThat(service.saveDownloadBytes(job.jobId(), job.leaseToken(), 2)).isFalse();
        assertThat(service.saveDownloadBytes(job.jobId(), job.leaseToken(), 1)).isTrue();
        assertThat(service.saveDownloadBytes(job.jobId(), job.leaseToken(), 1)).isFalse();
        assertThatThrownBy(() -> sql.update("UPDATE announcement_attachment_jobs SET reserved_download_bytes=0 WHERE id=?", job.jobId()))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(service.saveJobFailure(job.jobId(), job.leaseToken(), AttachmentFailureCode.NETWORK_TIMEOUT)).isTrue();
        assertThat(dao.selectJobDetails(job.jobId()).reservedDownloadBytes()).isEqualTo(cap);
    }

    @Test void att060DownloadAndHostAndExtractionCapsAreSharedAndTokenFenced() {
        for (int i = 0; i < 3; i++) service.insertAttachmentJob(selectRequest());
        var first = service.saveNextJobClaim().orElseThrow();
        var second = service.saveNextJobClaim().orElseThrow();
        var third = service.saveNextJobClaim().orElseThrow();
        var firstLease = service.saveDownloadLease(first.jobId(), first.leaseToken(), "1".repeat(64)).orElseThrow();
        assertThat(service.saveDownloadLease(second.jobId(), second.leaseToken(), "1".repeat(64))).isEmpty();
        var secondLease = service.saveDownloadLease(second.jobId(), second.leaseToken(), "2".repeat(64)).orElseThrow();
        assertThat(service.saveDownloadLease(third.jobId(), third.leaseToken(), "3".repeat(64))).isEmpty();
        service.deleteResourceLease(new AttachmentResourceLease(first.jobId(), UUID.randomUUID(), firstLease.resourceTokens()));
        assertThat(service.saveDownloadLease(third.jobId(), third.leaseToken(), "3".repeat(64))).isEmpty();
        service.deleteResourceLease(firstLease);
        assertThat(service.saveDownloadLease(third.jobId(), third.leaseToken(), "3".repeat(64))).isPresent();
        var extraction = service.saveExtractionLease(first.jobId(), first.leaseToken()).orElseThrow();
        assertThat(service.saveExtractionLease(second.jobId(), second.leaseToken())).isEmpty();
        service.deleteResourceLease(extraction);
        assertThat(service.saveExtractionLease(second.jobId(), second.leaseToken())).isPresent();
        service.deleteResourceLease(secondLease);
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_attachment_resource_leases WHERE resource_code='DOWNLOAD'", Integer.class)).isEqualTo(1);
    }

    @Test void att034And035ChangedOrDeletedSourcesAreNotClaimedForNetworkWork() {
        var request = selectRequest();
        var job = service.insertAttachmentJob(request);
        sql.update("UPDATE announcement_source_snapshots SET classification_row_version=classification_row_version+1 WHERE id=?", request.sourceId());
        assertThat(service.saveNextJobClaim()).isEmpty();
        assertThat(dao.selectJobDetails(job.jobId()).jobStatusCode()).isEqualTo("CONFLICT");
        var next = selectRequest();
        var deleted = service.insertAttachmentJob(next);
        sql.update("DELETE FROM announcement_source_snapshots WHERE id=?", next.sourceId());
        assertThat(service.saveNextJobClaim()).isEmpty();
        assertThat(dao.selectJobDetails(deleted.jobId())).isNull();
    }

    @Test void att001And002TitleExcludedOrMissingCombinationCannotReserveAnyJob() {
        var request = selectRequest();
        sql.update("UPDATE announcement_source_snapshots SET semantic_status_code='EXCLUDED' WHERE id=?", request.sourceId());
        assertThatThrownBy(() -> service.insertAttachmentJob(request)).isInstanceOf(ApiException.class).hasMessageContaining("제목 수집 기준");
        sql.update("UPDATE announcement_source_snapshots SET semantic_status_code='REVIEW_REQUIRED' WHERE id=?", request.sourceId());
        sql.update("UPDATE announcement_source_classification_evaluations SET title_stage_code='COMBINATION_NOT_MATCHED' WHERE id=?", request.expectedBaseDecisionId());
        assertThatThrownBy(() -> service.insertAttachmentJob(request)).isInstanceOf(ApiException.class).hasMessageContaining("제목 수집 기준");
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_attachment_jobs", Integer.class)).isZero();
    }

    @Test void att061PolicyProfileAndRuleMustMatchWithoutUpdatingSourceOnFailure() {
        var request = selectRequest();
        var unregistered = new AttachmentJobReservation(request.sourceId(), policy, request.expectedBaseDecisionId(),
                0, 0, UUID.randomUUID(), new AttachmentExecutionSnapshot("UNREGISTERED", PROFILE_HASH,
                EXECUTION.engineVersion(), EXECUTION.extractorVersion(), CONFIG_HASH));
        assertThatThrownBy(() -> service.insertAttachmentJob(unregistered)).isInstanceOf(ApiException.class).hasMessageContaining("출처 프로필");
        UUID otherRelease = UUID.randomUUID();
        int version = sql.queryForObject("SELECT max(version_no)+1 FROM announcement_source_classification_rule_releases", Integer.class);
        sql.update("INSERT INTO announcement_source_classification_rule_releases(id,release_code,version_no) VALUES (?,?,?)", otherRelease, otherRelease.toString(), version);
        sql.update("UPDATE announcement_source_classification_evaluations SET rule_release_id=? WHERE id=?", otherRelease, request.expectedBaseDecisionId());
        assertThatThrownBy(() -> service.insertAttachmentJob(request)).isInstanceOf(ApiException.class).hasMessageContaining("규칙 버전이 다릅니다");
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_attachment_jobs", Integer.class)).isZero();
        assertThat(sql.queryForObject("SELECT attachment_row_version FROM announcement_source_snapshots WHERE id=?", Integer.class, request.sourceId())).isZero();
    }

    @Test void frozenExecutionCannotBeEditedAndLegacyUnfrozenJobCannotRun() {
        var request = selectRequest();
        var job = service.insertAttachmentJob(request);
        assertThatThrownBy(() -> sql.update("UPDATE announcement_attachment_jobs SET execution_snapshot_json='{}' WHERE id=?", job.jobId()))
                .isInstanceOf(DataIntegrityViolationException.class);
        var legacy = selectRequest();
        UUID content = sql.queryForObject("SELECT content_version_id FROM announcement_source_classification_evaluations WHERE id=?", UUID.class, legacy.expectedBaseDecisionId());
        sql.update("""
                INSERT INTO announcement_attachment_jobs(source_id,content_version_id,base_evaluation_id,rule_release_id,
                    policy_id,generation,expected_source_version,expected_attachment_version,job_status_code,idempotency_key,request_hash)
                VALUES (?,?,?,?,?,1,0,0,'PENDING',?,repeat('e',64))
                """, legacy.sourceId(), content, legacy.expectedBaseDecisionId(), release, policy, UUID.randomUUID());
        assertThat(service.saveNextJobClaim().orElseThrow().jobId()).isEqualTo(job.jobId());
        assertThat(service.saveNextJobClaim()).isEmpty();
    }

    @Test void att041LegacyGuardLocksUnboundSourceUntilMutationFinishes() throws Exception {
        var source = selectRequest();
        var sourceDao = context.getBean(SqlSessionTemplate.class).getMapper(AnnouncementSourceDao.class);
        var transaction = new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
        var locked = new CountDownLatch(1);
        var releaseLock = new CountDownLatch(1);
        var writerStarted = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            try {
                var legacy = executor.submit(() -> transaction.executeWithoutResult(status -> {
                    Boolean required = sourceDao.selectAttachmentReviewRequiredDetailsForUpdate(source.sourceId());
                    assertThat(required).isFalse();
                    AttachmentLegacyPathGuard.validate(required);
                    locked.countDown();
                    try { assertThat(releaseLock.await(5, TimeUnit.SECONDS)).isTrue(); }
                    catch (InterruptedException exception) { Thread.currentThread().interrupt(); throw new IllegalStateException(exception); }
                }));
                assertThat(locked.await(5, TimeUnit.SECONDS)).isTrue();
                var binding = executor.submit(() -> {
                    writerStarted.countDown();
                    return sql.update("UPDATE announcement_source_snapshots SET is_attachment_review_required=true,attachment_policy_id=? WHERE id=?",
                            policy, source.sourceId());
                });
                assertThat(writerStarted.await(5, TimeUnit.SECONDS)).isTrue();
                assertThatThrownBy(() -> binding.get(100, TimeUnit.MILLISECONDS))
                        .isInstanceOf(java.util.concurrent.TimeoutException.class);
                releaseLock.countDown();
                legacy.get(5, TimeUnit.SECONDS);
                assertThat(binding.get(5, TimeUnit.SECONDS)).isEqualTo(1);
            } finally { releaseLock.countDown(); }
        }
        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> AttachmentLegacyPathGuard.validate(
                sourceDao.selectAttachmentReviewRequiredDetailsForUpdate(source.sourceId()))))
                .isInstanceOf(ApiException.class).hasMessageContaining("첨부 종합 검수");
    }

    @Test void att012And057NoFilesSealsEvidenceButDoesNotPretendClassificationCompleted() {
        var request = selectRequest();
        service.insertAttachmentJob(request);
        var job = service.saveNextJobClaim().orElseThrow();
        var input = new AttachmentSetEvidence("NO_FILES", true, List.of());
        var first = evidenceService.saveAttachmentSet(job.jobId(), job.leaseToken(), input).orElseThrow();
        var again = evidenceService.saveAttachmentSet(job.jobId(), job.leaseToken(), input).orElseThrow();
        assertThat(again.setId()).isEqualTo(first.setId());
        assertThat(first.setStatus()).isEqualTo("SEALED");
        assertThat(first.discoveryStatus()).isEqualTo("NO_FILES");
        assertThat(first.discoveredCount()).isZero();
        assertThat(dao.selectJobDetails(job.jobId()).jobStatusCode()).isEqualTo("RUNNING");
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_source_attachment_evaluations", Integer.class)).isZero();
        assertThat(sql.queryForObject("SELECT classification_row_version FROM announcement_source_snapshots WHERE id=?", Integer.class, request.sourceId())).isZero();
        assertThat(sql.queryForObject("SELECT is_attachment_review_required FROM announcement_source_snapshots WHERE id=?", Boolean.class, request.sourceId())).isFalse();
        assertThat(evidenceDao.selectSetDetails(UUID.randomUUID(), first.setId())).isNull();
    }

    @Test void retryCheckpointSurvivesLeaseRecoveryWithoutPretendingSealedAndKeepsOriginalTime() {
        service.insertAttachmentJob(selectRequest()); var first=service.saveNextJobClaim().orElseThrow();
        assertThat(service.saveDownloadBytes(first.jobId(),first.leaseToken(),15)).isTrue();
        var complete=selectCheckpointFile();
        assertThat(evidenceService.saveFileCheckpoint(first.jobId(),first.leaseToken(),complete)).isTrue();
        assertThat(evidenceService.saveFileCheckpoint(first.jobId(),first.leaseToken(),complete)).isTrue();
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_attachment_file_checkpoints WHERE job_id=?",Integer.class,first.jobId())).isEqualTo(1);
        assertThat(evidenceDao.selectSetCount(first.sourceId())).isZero();
        assertThatThrownBy(()->sql.update("UPDATE announcement_attachment_file_checkpoints SET created_at=clock_timestamp() WHERE job_id=?",first.jobId()))
                .isInstanceOf(DataIntegrityViolationException.class);
        updateLeaseExpired(first);
        assertThat(evidenceService.selectFileCheckpoint(first.jobId(),first.leaseToken(),complete.locator())).isEmpty();
        service.saveNextJobClaim();
        sql.update("UPDATE announcement_attachment_jobs SET next_attempt_at=now()-interval '1 second' WHERE id=?",first.jobId());
        var next=service.saveNextJobClaim().orElseThrow();
        assertThat(next.jobId()).isEqualTo(first.jobId());
        assertThat(next.leaseToken()).isNotEqualTo(first.leaseToken());
        assertThat(evidenceService.selectFileCheckpoint(next.jobId(),next.leaseToken(),complete.locator())).contains(complete);
        var set=evidenceService.saveAttachmentSet(next.jobId(),next.leaseToken(),new AttachmentSetEvidence("FOUND",true,List.of(complete))).orElseThrow();
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_attachment_file_checkpoints WHERE job_id=?",Integer.class,next.jobId())).isZero();
        var saved=evidenceDao.selectFileList(next.sourceId(),set.setId(),0,10).getFirst();
        assertThat(saved.extractedAt().toInstant().toEpochMilli()).isEqualTo(complete.extraction().completedAtEpochMs());
        assertThat(next.reservedDownloadBytes()).isEqualTo(15);
    }

    @Test void checkpointCannotCrossSourceOrSurviveTerminalConflict() throws Exception {
        var request=selectRequest(); service.insertAttachmentJob(request); var job=service.saveNextJobClaim().orElseThrow();
        service.saveDownloadBytes(job.jobId(),job.leaseToken(),15);
        var complete=selectCheckpointFile(); evidenceService.saveFileCheckpoint(job.jobId(),job.leaseToken(),complete);
        var other=selectRequest();
        assertThatThrownBy(()->sql.update("INSERT INTO announcement_attachment_file_checkpoints(job_id,source_id,stable_locator_hash,file_result_json) VALUES (?,?,repeat('a',64),?::jsonb)",
                job.jobId(),other.sourceId(),new ObjectMapper().writeValueAsString(complete))).isInstanceOf(DataIntegrityViolationException.class);
        sql.update("UPDATE announcement_source_snapshots SET classification_row_version=classification_row_version+1 WHERE id=?",job.sourceId());
        assertThat(dao.selectJobDetails(job.jobId()).jobStatusCode()).isEqualTo("CONFLICT");
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_attachment_file_checkpoints WHERE job_id=?",Integer.class,job.jobId())).isZero();
        assertThat(evidenceService.selectFileCheckpoint(job.jobId(),job.leaseToken(),complete.locator())).isEmpty();
    }

    @Test void checkpointRejectsIncompleteExtractionAndBoundedFileCountAtDatabase() throws Exception {
        service.insertAttachmentJob(selectRequest()); var job=service.saveNextJobClaim().orElseThrow();
        var complete=selectCheckpointFile(); String json=new ObjectMapper().writeValueAsString(complete);
        String insert="INSERT INTO announcement_attachment_file_checkpoints(job_id,source_id,stable_locator_hash,file_result_json) VALUES (?,?,?,?::jsonb)";
        assertThatThrownBy(()->sql.update(insert,job.jobId(),job.sourceId(),"a".repeat(64),json.replace("COMPLETE_TEXT","PARTIAL_TEXT")))
                .isInstanceOf(DataIntegrityViolationException.class);
        for(int i=0;i<30;i++) assertThat(sql.update(insert,job.jobId(),job.sourceId(),String.format("%064x",i),json)).isEqualTo(1);
        assertThatThrownBy(()->sql.update(insert,job.jobId(),job.sourceId(),"b".repeat(64),json)).isInstanceOf(DataIntegrityViolationException.class);
    }

    private AttachmentSetEvidence.File selectCheckpointFile() {
        var file=selectFileEvidence(100); var x=file.extraction();
        return new AttachmentSetEvidence.File(file.locator(),file.displayName(),file.detectedType(),file.role(),file.roleOrigin(),file.downloadStatus(),
                file.downloadedBytes(),file.binaryHash(),file.failureCode(),
                new AttachmentSetEvidence.Extraction(x.quality(),x.text(),x.blocks(),x.pageCount(),x.durationMs(),System.currentTimeMillis()-1000));
    }

    @Test void att014And032SealsSuccessfulAndFailedFileEvidenceWithoutMutatingBase() {
        var request = selectRequest();
        service.insertAttachmentJob(request);
        var job = service.saveNextJobClaim().orElseThrow();
        assertThat(service.saveDownloadBytes(job.jobId(), job.leaseToken(), 15)).isTrue();
        var complete = selectFileEvidence(100);
        var failed = new AttachmentSetEvidence.File(
                new AttachmentSetEvidence.Locator(EXECUTION.profileCode(), "/download/file", java.util.Map.of("fileId", "fixture-2")),
                "두 번째 공고문.pdf", "PDF", "NOTICE", "PROFILE", "FAILED", 0, null,
                AttachmentFailureCode.NETWORK_TIMEOUT, null);
        var result = new AttachmentSetEvidence("FOUND", true, List.of(complete, failed));
        var set = evidenceService.saveAttachmentSet(job.jobId(), job.leaseToken(), result).orElseThrow();
        assertThat(set.processedCount()).isEqualTo(2);
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_source_attachment_files WHERE set_id=?", Integer.class, set.setId())).isEqualTo(2);
        assertThat(sql.queryForObject("SELECT character_count FROM announcement_source_attachment_extractions WHERE set_id=?", Integer.class, set.setId()))
                .isEqualTo("소상공인 지원금 😀".codePointCount(0, "소상공인 지원금 😀".length()));
        assertThat(sql.queryForObject("SELECT error_code FROM announcement_source_attachment_files WHERE set_id=? AND download_status_code='FAILED'", String.class, set.setId()))
                .isEqualTo("NETWORK_TIMEOUT");
        var repeated = new AttachmentSetEvidence("FOUND", true, List.of(selectFileEvidence(200), failed));
        assertThat(evidenceService.saveAttachmentSet(job.jobId(), job.leaseToken(), repeated).orElseThrow().setId()).isEqualTo(set.setId());
        assertThat(sql.queryForObject("SELECT duration_ms FROM announcement_source_attachment_extractions WHERE set_id=?", Integer.class, set.setId())).isEqualTo(100);
        assertThatThrownBy(() -> sql.update("UPDATE announcement_source_attachment_files SET document_role_code='FORM' WHERE set_id=?", set.setId()))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(sql.queryForObject("SELECT current_attachment_evaluation_id FROM announcement_source_snapshots WHERE id=?", UUID.class, request.sourceId())).isNull();
        assertThat(sql.queryForObject("SELECT decision_status_code FROM announcement_source_classification_evaluations WHERE id=?", String.class, request.expectedBaseDecisionId()))
                .isEqualTo("REVIEW_REQUIRED");
    }

    @Test void att031ExpiredLeaseCannotPersistEvenACompletedSet() {
        service.insertAttachmentJob(selectRequest());
        var job = service.saveNextJobClaim().orElseThrow();
        updateLeaseExpired(job);
        assertThat(evidenceService.saveAttachmentSet(job.jobId(), job.leaseToken(), new AttachmentSetEvidence("NO_FILES", true, List.of())))
                .isEmpty();
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_source_attachment_sets", Integer.class)).isZero();
    }

    @Test void att034ChangedSourceProducesConflictInsteadOfPersistingLateEvidence() {
        var request = selectRequest();
        service.insertAttachmentJob(request);
        var job = service.saveNextJobClaim().orElseThrow();
        sql.update("UPDATE announcement_source_snapshots SET classification_row_version=classification_row_version+1 WHERE id=?", request.sourceId());
        assertThat(evidenceService.saveAttachmentSet(job.jobId(), job.leaseToken(), new AttachmentSetEvidence("NO_FILES", true, List.of()))).isEmpty();
        assertThat(dao.selectJobDetails(job.jobId()).jobStatusCode()).isEqualTo("CONFLICT");
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_source_attachment_sets", Integer.class)).isZero();
    }

    @Test void att024ReportedBytesMustHaveBeenReservedBeforeEvidenceStorage() {
        service.insertAttachmentJob(selectRequest());
        var job = service.saveNextJobClaim().orElseThrow();
        assertThatThrownBy(() -> evidenceService.saveAttachmentSet(job.jobId(), job.leaseToken(),
                new AttachmentSetEvidence("FOUND", true, List.of(selectFileEvidence(100)))))
                .isInstanceOf(ApiException.class).hasMessageContaining("예약된 다운로드 예산");
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_source_attachment_sets", Integer.class)).isZero();
    }

    @Test void att013DiscoveryFailureCannotMasqueradeAsVerifiedNoFiles() {
        service.insertAttachmentJob(selectRequest());
        var job = service.saveNextJobClaim().orElseThrow();
        assertThatThrownBy(() -> evidenceService.saveAttachmentSet(job.jobId(), job.leaseToken(),
                new AttachmentSetEvidence("NO_FILES", false, List.of())))
                .isInstanceOf(ApiException.class).hasMessageContaining("정상 확인");
        var set = evidenceService.saveAttachmentSet(job.jobId(), job.leaseToken(),
                new AttachmentSetEvidence("FAILED", false, List.of())).orElseThrow();
        assertThat(set.discoveryStatus()).isEqualTo("FAILED");
        assertThat(set.discoveryComplete()).isFalse();
    }

    @Test void att035And038SourceDeletionCascadesSealedEvidenceAndCannotBeResurrected() {
        var request = selectRequest();
        service.insertAttachmentJob(request);
        var job = service.saveNextJobClaim().orElseThrow();
        service.saveDownloadBytes(job.jobId(), job.leaseToken(), 15);
        var result = new AttachmentSetEvidence("FOUND", true, List.of(selectFileEvidence(100)));
        evidenceService.saveAttachmentSet(job.jobId(), job.leaseToken(), result).orElseThrow();
        sql.update("DELETE FROM announcement_source_snapshots WHERE id=?", request.sourceId());
        assertThat(evidenceService.saveAttachmentSet(job.jobId(), job.leaseToken(), result)).isEmpty();
        for (String table : List.of("announcement_source_attachment_sets", "announcement_source_attachment_files", "announcement_source_attachment_extractions")) {
            assertThat(sql.queryForObject("SELECT count(1) FROM " + table, Integer.class)).isZero();
        }
    }

    @Test void concurrentSetCompletionPersistsExactlyOneImmutableSet() throws Exception {
        service.insertAttachmentJob(selectRequest());
        var job = service.saveNextJobClaim().orElseThrow();
        var input = new AttachmentSetEvidence("NO_FILES", true, List.of());
        var gate = new CountDownLatch(1);
        Callable<UUID> save = () -> { gate.await(5, TimeUnit.SECONDS); return evidenceService.saveAttachmentSet(job.jobId(), job.leaseToken(), input).orElseThrow().setId(); };
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(save);
            var second = executor.submit(save);
            gate.countDown();
            assertThat(first.get(10, TimeUnit.SECONDS)).isEqualTo(second.get(10, TimeUnit.SECONDS));
        }
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_source_attachment_sets", Integer.class)).isEqualTo(1);
    }

    private AttachmentSetEvidence.File selectFileEvidence(int durationMs) {
        String text = "소상공인 지원금 😀";
        return new AttachmentSetEvidence.File(
                new AttachmentSetEvidence.Locator(EXECUTION.profileCode(), "/download/file", java.util.Map.of("fileId", "fixture-1")),
                "공고문.hwpx", "HWPX", "NOTICE", "PROFILE", "SUCCEEDED", 15, "a".repeat(64), null,
                new AttachmentSetEvidence.Extraction("COMPLETE_TEXT", text,
                        List.of(new AttachmentSetEvidence.Block(0, 0, text.codePointCount(0, text.length()), "paragraph:0", true, "paragraph:0")),
                        null, durationMs));
    }

    @Test void att034And044BaseChangeInvalidatesCurrentEvidenceAndConfirmationButRetainsReviewGuard() {
        var request = selectRequest();
        service.insertAttachmentJob(request);
        var job = service.saveNextJobClaim().orElseThrow();
        var set = evidenceService.saveAttachmentSet(job.jobId(), job.leaseToken(),
                new AttachmentSetEvidence("NO_FILES", true, List.of())).orElseThrow();
        UUID evaluationId = UUID.randomUUID();
        var transaction = new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
        transaction.executeWithoutResult(status -> {
            sql.update("""
                    INSERT INTO announcement_source_attachment_evaluations(id,source_id,content_version_id,base_evaluation_id,
                        set_id,policy_id,rule_release_id,engine_version,input_hash,decision_hash,decision_status_code,reason_code,is_current)
                    VALUES (?,?,?,?,?,?,?,'fixture',repeat('e',64),repeat('f',64),'REVIEW_REQUIRED','BODY_UNAVAILABLE',true)
                    """, evaluationId, job.sourceId(), job.contentVersionId(), job.baseEvaluationId(), set.setId(), policy, release);
            sql.update("UPDATE announcement_source_snapshots SET current_attachment_evaluation_id=?,attachment_policy_id=?,is_attachment_review_required=true WHERE id=?",
                    evaluationId, policy, job.sourceId());
            sql.update("""
                    INSERT INTO announcement_source_attachment_confirmations(source_id,evaluation_id,set_hash,confirmed_by,
                        review_method_code,acknowledged_error_codes_json,review_note,idempotency_key,request_hash)
                    VALUES (?,?,?,?,'MANUAL_SOURCE_CHECK','[]','격리 검증용 확정',?,repeat('a',64))
                    """, job.sourceId(), evaluationId, set.manifestHash(), actor, UUID.randomUUID());
        });
        sql.update("UPDATE announcement_source_snapshots SET classification_row_version=classification_row_version+1 WHERE id=?", job.sourceId());
        assertThat(sql.queryForObject("SELECT current_attachment_evaluation_id FROM announcement_source_snapshots WHERE id=?", UUID.class, job.sourceId())).isNull();
        assertThat(sql.queryForObject("SELECT attachment_row_version FROM announcement_source_snapshots WHERE id=?", Integer.class, job.sourceId())).isEqualTo(2);
        assertThat(sql.queryForObject("SELECT is_attachment_review_required FROM announcement_source_snapshots WHERE id=?", Boolean.class, job.sourceId())).isTrue();
        assertThat(sql.queryForObject("SELECT attachment_policy_id FROM announcement_source_snapshots WHERE id=?", UUID.class, job.sourceId())).isEqualTo(policy);
        assertThat(sql.queryForObject("SELECT is_current FROM announcement_source_attachment_evaluations WHERE id=?", Boolean.class, evaluationId)).isFalse();
        assertThat(sql.queryForObject("SELECT is_current FROM announcement_source_attachment_confirmations WHERE evaluation_id=?", Boolean.class, evaluationId)).isFalse();
        assertThat(dao.selectJobDetails(job.jobId()).jobStatusCode()).isEqualTo("CONFLICT");
        assertThat(service.saveDownloadBytes(job.jobId(), job.leaseToken(), 1)).isFalse();
    }

    @Test void att036And052ReadApiQueriesStayWithinSourceAndBoundCodePointExcerpts() {
        var readService = context.getBean(AnnouncementAttachmentReadService.class);
        var request = selectRequest();
        service.insertAttachmentJob(request);
        var job = service.saveNextJobClaim().orElseThrow();
        service.saveDownloadBytes(job.jobId(), job.leaseToken(), 15);
        var set = evidenceService.saveAttachmentSet(job.jobId(), job.leaseToken(),
                new AttachmentSetEvidence("FOUND", true, List.of(selectFileEvidence(100)))).orElseThrow();
        var sets = readService.selectAttachmentSetList(job.sourceId(), 1, 10);
        assertThat(sets.totalCount()).isEqualTo(1);
        assertThat(sets.items().getFirst().sealedAt()).isNotNull();
        var files = readService.selectAttachmentFileList(job.sourceId(), set.setId(), 1, 10);
        assertThat(files.totalCount()).isEqualTo(1);
        assertThat(files.items().getFirst().qualityCode()).isEqualTo("COMPLETE_TEXT");
        UUID extraction = files.items().getFirst().extractionId();
        var first = readService.selectAttachmentBlockList(job.sourceId(), extraction, 1, 1, 0, 4).items().getFirst();
        assertThat(first.text()).isEqualTo("소상공인");
        assertThat(first.textStartOffset()).isZero();
        assertThat(first.textEndOffset()).isEqualTo(4);
        assertThat(first.hasMoreText()).isTrue();
        var last = readService.selectAttachmentBlockList(job.sourceId(), extraction, 1, 1, 9, 4).items().getFirst();
        assertThat(last.text()).isEqualTo("😀");
        assertThat(last.textEndOffset()).isEqualTo(10);
        assertThat(last.hasMoreText()).isFalse();
        UUID otherSource = selectRequest().sourceId();
        assertThatThrownBy(() -> readService.selectAttachmentFileList(otherSource, set.setId(), 1, 10))
                .isInstanceOf(ApiException.class).hasMessageContaining("이 원문에 속한");
        assertThatThrownBy(() -> readService.selectAttachmentBlockList(otherSource, extraction, 1, 1, 0, 4))
                .isInstanceOf(ApiException.class).hasMessageContaining("이 원문에 속한");
        assertThatThrownBy(() -> readService.selectAttachmentBlockList(job.sourceId(), extraction, 1, 100, 0, 4000))
                .isInstanceOf(ApiException.class).hasMessageContaining("페이지 크기");
        assertThatThrownBy(() -> readService.selectAttachmentBlockList(job.sourceId(), extraction, 1, 1, 0, 1_000_000))
                .isInstanceOf(ApiException.class).hasMessageContaining("조회 길이");
    }

    private AttachmentJobReservation selectRequest() {
        UUID source = UUID.randomUUID(), content = UUID.randomUUID(), base = UUID.randomUUID();
        sql.update("INSERT INTO announcement_source_snapshots(id,provider_code,title,raw_hash,semantic_status_code) VALUES (?,'BIZINFO','소상공인 지원금',?,'REVIEW_REQUIRED')",
                source, source.toString().replace("-", "").repeat(2));
        sql.update("INSERT INTO announcement_source_content_versions(id,source_id,raw_hash,title,body_source_code,body_availability_code,collected_at) VALUES (?,?,repeat('a',64),'소상공인 지원금','NONE','UNAVAILABLE',now())", content, source);
        sql.update("""
                INSERT INTO announcement_source_classification_evaluations
                (id,source_id,content_version_id,rule_release_id,engine_version,body_source_code,body_availability_code,
                 title_stage_code,body_stage_code,decision_status_code,reason_code,is_current)
                VALUES (?,?,?,?,'fixture','NONE','UNAVAILABLE','COMBINATION_MATCHED','UNAVAILABLE','REVIEW_REQUIRED','BODY_UNAVAILABLE',true)
                """, base, source, content, release);
        return new AttachmentJobReservation(source, policy, base, 0, 0, UUID.randomUUID(), EXECUTION);
    }

    @Test void att016And036SealedEvidenceUsesPublishedRulesAndPersistsMatchesAndTags() {
        var request = selectRequest();
        service.insertAttachmentJob(request);
        var job = service.saveNextJobClaim().orElseThrow();
        service.saveDownloadBytes(job.jobId(), job.leaseToken(), 15);
        var set = evidenceService.saveAttachmentSet(job.jobId(), job.leaseToken(),
                new AttachmentSetEvidence("FOUND", true, List.of(selectFileEvidence(100)))).orElseThrow();
        var result = context.getBean(AnnouncementAttachmentEvaluationService.class).saveJobEvaluation(job.jobId(), job.leaseToken()).orElseThrow();
        assertThat(result.status()).isEqualTo("ACCEPTED");
        assertThat(result.setId()).isEqualTo(set.setId());
        assertThat(result.current()).isTrue();
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_source_attachment_evaluation_inputs WHERE evaluation_id=?", Integer.class, result.evaluationId())).isEqualTo(1);
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_source_attachment_matches WHERE evaluation_id=?", Integer.class, result.evaluationId())).isGreaterThanOrEqualTo(2);
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_source_attachment_tags WHERE evaluation_id=?", Integer.class, result.evaluationId())).isGreaterThanOrEqualTo(2);
        assertThat(sql.queryForObject("SELECT semantic_status_code FROM announcement_source_snapshots WHERE id=?", String.class, job.sourceId())).isEqualTo("REVIEW_REQUIRED");
        assertThat(sql.queryForObject("SELECT is_attachment_review_required FROM announcement_source_snapshots WHERE id=?", Boolean.class, job.sourceId())).isFalse();
        assertThat(sql.queryForObject("SELECT attachment_row_version FROM announcement_source_snapshots WHERE id=?", Integer.class, job.sourceId())).isEqualTo(2);
        assertThat(dao.selectJobDetails(job.jobId()).jobStatusCode()).isEqualTo("SUCCEEDED");
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_source_links WHERE source_id=?", Integer.class, job.sourceId())).isZero();
    }

    @Test void segmentEvaluationBindsGuideAndFormWithoutPromotingFormOrChangingBase() throws Exception {
        var job=selectSegmentJob(false);
        var evaluator=context.getBean(AnnouncementAttachmentEvaluationService.class);
        var result=evaluator.saveJobEvaluation(job.jobId(),job.leaseToken()).orElseThrow();
        assertThat(result.status()).isEqualTo("ACCEPTED");
        assertThat(evaluator.saveJobEvaluation(job.jobId(),job.leaseToken()).orElseThrow().evaluationId()).isEqualTo(result.evaluationId());
        assertThat(sql.queryForObject("SELECT engine_version FROM announcement_source_attachment_evaluations WHERE id=?",String.class,result.evaluationId()))
                .isEqualTo(com.saneb.domain.announcementattachment.classification.AttachmentSegmentClassificationEngine.VERSION);
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_attachment_segment_analyses WHERE source_id=?",Integer.class,job.sourceId())).isEqualTo(1);
        assertThat(sql.queryForList("SELECT analysis_json->'segments'->0->>'roleCode' AS first_role,analysis_json->'segments'->1->>'roleCode' AS second_role FROM announcement_attachment_segment_analyses WHERE source_id=?",job.sourceId()))
                .containsExactly(java.util.Map.of("first_role","GUIDE","second_role","FORM"));
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_source_attachment_evaluation_inputs WHERE evaluation_id=? AND segment_analysis_id IS NOT NULL",Integer.class,result.evaluationId())).isEqualTo(1);
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_source_attachment_matches WHERE evaluation_id=? AND segment_index=1 AND applied_action_code='CONTEXT_ONLY'",Integer.class,result.evaluationId())).isPositive();
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_source_attachment_matches WHERE evaluation_id=? AND (segment_analysis_id IS NULL OR (segment_index=1 AND applied_action_code<>'CONTEXT_ONLY'))",Integer.class,result.evaluationId())).isZero();
        assertThat(sql.queryForObject("SELECT document_role_code FROM announcement_source_attachment_files WHERE source_id=?",String.class,job.sourceId())).isEqualTo("UNKNOWN");
        assertThat(sql.queryForObject("SELECT semantic_status_code FROM announcement_source_snapshots WHERE id=?",String.class,job.sourceId())).isEqualTo("REVIEW_REQUIRED");
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_source_links WHERE source_id=?",Integer.class,job.sourceId())).isZero();
        assertThat(dao.selectJobDetails(job.jobId()).jobStatusCode()).isEqualTo("SUCCEEDED");
        // 근거 삭제·교체로 판정을 사후 변경하지 못한다. 원문 삭제 cascade만 허용한다.
        for(String statement:List.of(
                "UPDATE announcement_source_attachment_evaluation_inputs SET segment_analysis_id=NULL WHERE evaluation_id=?",
                "DELETE FROM announcement_source_attachment_evaluation_inputs WHERE evaluation_id=?",
                "UPDATE announcement_source_attachment_matches SET segment_index=0 WHERE evaluation_id=?",
                "DELETE FROM announcement_source_attachment_matches WHERE evaluation_id=?",
                "DELETE FROM announcement_source_attachment_evaluations WHERE id=?")) {
            assertThatThrownBy(()->sql.update(statement,result.evaluationId())).isInstanceOf(DataIntegrityViolationException.class);
        }
        // 기존 유일성 위반이 아니라 신규 구간 보호 trigger 자체에서 거부해야 한다.
        String copyMatch="""
                INSERT INTO announcement_source_attachment_matches(evaluation_id,source_id,set_id,file_id,extraction_id,rule_release_id,
                    keyword_group_id,keyword_rule_id,keyword_term_id,block_index,start_offset,end_offset,applied_action_code,segment_analysis_id,segment_index)
                SELECT evaluation_id,source_id,set_id,file_id,extraction_id,rule_release_id,keyword_group_id,keyword_rule_id,keyword_term_id,
                    block_index,start_offset,end_offset,%s,segment_analysis_id,%s
                FROM announcement_source_attachment_matches WHERE evaluation_id=? AND segment_index=1 LIMIT 1
                """;
        assertThatThrownBy(()->sql.update(copyMatch.formatted("'TAG'","segment_index"),result.evaluationId()))
                .isInstanceOf(DataIntegrityViolationException.class).hasStackTraceContaining("attachment segment context cannot become decision evidence");
        assertThatThrownBy(()->sql.update(copyMatch.formatted("applied_action_code","0"),result.evaluationId()))
                .isInstanceOf(DataIntegrityViolationException.class).hasStackTraceContaining("attachment segment match position invalid");
        assertThatThrownBy(()->sql.update(copyMatch.formatted("applied_action_code","segment_index"),result.evaluationId()))
                .isInstanceOf(DataIntegrityViolationException.class).hasStackTraceContaining("attachment segment matches require creation transaction");
        assertThatThrownBy(()->sql.update("""
                INSERT INTO announcement_source_attachment_evaluation_inputs(evaluation_id,source_id,set_id,file_id,extraction_id,document_role_code,input_status_code)
                SELECT evaluation_id,source_id,set_id,file_id,extraction_id,document_role_code,input_status_code
                FROM announcement_source_attachment_evaluation_inputs WHERE evaluation_id=?
                """,result.evaluationId())).isInstanceOf(DataIntegrityViolationException.class)
                .hasStackTraceContaining("complete attachment requires segment analysis");
        sql.update("DELETE FROM announcement_source_snapshots WHERE id=?",job.sourceId());
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_attachment_segment_analyses WHERE source_id=?",Integer.class,job.sourceId())).isZero();
    }

    @Test void segmentEvaluationKeepsFailedFilesAndDoesNotAcceptPartialEvidence() throws Exception {
        var job=selectSegmentJob(true);
        var result=context.getBean(AnnouncementAttachmentEvaluationService.class).saveJobEvaluation(job.jobId(),job.leaseToken()).orElseThrow();
        assertThat(result.status()).isEqualTo("REVIEW_REQUIRED");
        assertThat(sql.queryForObject("SELECT reason_code FROM announcement_source_attachment_evaluations WHERE id=?",String.class,result.evaluationId())).isEqualTo("ATTACHMENT_INCOMPLETE");
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_source_attachment_evaluation_inputs WHERE evaluation_id=?",Integer.class,result.evaluationId())).isEqualTo(2);
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_source_attachment_evaluation_inputs WHERE evaluation_id=? AND extraction_id IS NULL AND segment_analysis_id IS NULL AND input_status_code='FAILED'",Integer.class,result.evaluationId())).isEqualTo(1);
        assertThat(dao.selectJobDetails(job.jobId()).jobStatusCode()).isEqualTo("PARTIAL_FAILED");
    }

    @Test void resolvedMixedSegmentReviewUsesExtractedEvidenceAndCreatesOnlyOneDraft() throws Exception {
        var job=selectSegmentJob(false,true);
        var result=context.getBean(AnnouncementAttachmentEvaluationService.class).saveJobEvaluation(job.jobId(),job.leaseToken()).orElseThrow();
        assertThat(result.status()).isEqualTo("ACCEPTED");
        var state=reviewService().selectReviewContextDetails(job.sourceId());
        assertThat(state.manualSourceCheckRequired()).isFalse();assertThat(state.requiredAcknowledgementCodes()).isEmpty();
        assertThat(sql.queryForObject("SELECT document_role_code FROM announcement_source_attachment_files WHERE source_id=?",String.class,job.sourceId())).isEqualTo("UNKNOWN");
        assertThatThrownBy(()->reviewService().insertOperationalAnnouncement(reviewActor(),job.sourceId(),
                new AttachmentReviewRequests.Conversion(state.version(),UUID.randomUUID(),"BUSINESS",null))).isInstanceOf(ApiException.class);
        var request=selectReviewRequest(job.sourceId());assertThat(request.reviewMethodCode()).isEqualTo("EXTRACTED_TEXT");
        UUID key=UUID.randomUUID();
        var confirmation=reviewService().insertConfirmation(reviewActor(),job.sourceId(),key,request);
        assertThat(reviewService().insertConfirmation(reviewActor(),job.sourceId(),key,request)).isEqualTo(confirmation);
        var conversion=new AttachmentReviewRequests.Conversion(reviewService().selectReviewContextDetails(job.sourceId()).version(),confirmation.confirmationId(),"BUSINESS",null);
        var draft=reviewService().insertOperationalAnnouncement(reviewActor(),job.sourceId(),conversion);
        assertThat(reviewService().insertOperationalAnnouncement(reviewActor(),job.sourceId(),conversion)).isEqualTo(draft);
        assertThat(sql.queryForObject("SELECT approval_status_code FROM announcements WHERE id=?",String.class,draft.announcementId())).isEqualTo("DRAFT");
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_approval_requests WHERE announcement_id=?",Integer.class,draft.announcementId())).isZero();
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_source_links WHERE source_id=?",Integer.class,job.sourceId())).isEqualTo(1);
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_target_category_assignments WHERE announcement_id=?",Integer.class,draft.announcementId())).isEqualTo(2);
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_support_type_assignments WHERE announcement_id=?",Integer.class,draft.announcementId())).isEqualTo(1);
        assertThat(sql.queryForObject("SELECT decision_status_code FROM announcement_source_attachment_evaluations WHERE id=?",String.class,result.evaluationId())).isEqualTo("ACCEPTED");
    }
    @Test void failedFileAlongsideResolvedSegmentsStillRequiresManualOriginalCheck() throws Exception {
        var job=selectSegmentJob(true,true);
        context.getBean(AnnouncementAttachmentEvaluationService.class).saveJobEvaluation(job.jobId(),job.leaseToken()).orElseThrow();
        var state=reviewService().selectReviewContextDetails(job.sourceId());
        assertThat(state.manualSourceCheckRequired()).isTrue();assertThat(state.requiredAcknowledgementCodes()).contains("NETWORK_TIMEOUT","ATTACHMENT_TEXT_NOT_EXTRACTED");
        var request=new AttachmentReviewRequests.Confirmation(state.version(),List.of("BUSINESS"),List.of("POLICY_FINANCE"),"EXTRACTED_TEXT",state.requiredAcknowledgementCodes(),"격리 검증용 잘못된 확인 방법");
        assertThatThrownBy(()->reviewService().insertConfirmation(reviewActor(),job.sourceId(),UUID.randomUUID(),request)).isInstanceOf(ApiException.class);
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_source_attachment_confirmations WHERE source_id=?",Integer.class,job.sourceId())).isZero();
    }
    private AttachmentJobRow selectSegmentJob(boolean includeFailed) throws Exception {
        return selectSegmentJob(includeFailed,false);
    }
    private AttachmentJobRow selectSegmentJob(boolean includeFailed,boolean enforce) throws Exception {
        return selectSegmentJob(includeFailed,enforce,
                com.saneb.domain.announcementattachment.classification.AttachmentSegmentRoleAnalyzer.VERSION,false);
    }
    private AttachmentJobRow selectSegmentJob(boolean includeFailed,boolean enforce,String segmentVersion,boolean quarterHeading) throws Exception {
        var execution=new AttachmentExecutionSnapshot(EXECUTION.profileCode(),PROFILE_HASH,
                com.saneb.domain.announcementattachment.classification.AttachmentSegmentClassificationEngine.VERSION,
                EXECUTION.extractorVersion(),CONFIG_HASH,null,null,
                segmentVersion,com.saneb.domain.announcementattachment.classification.AttachmentEngineContract.selectSegmentRulesHash(segmentVersion));
        // 아직 예약/자료가 없는 테스트 소유 정책만 교체한다. 운영 게시 검증을 우회하는 기능은 추가하지 않는다.
        String manifest=sql.queryForObject("SELECT profile_manifest_json::text FROM announcement_attachment_policies WHERE id=?",String.class,policy);
        sql.update("DELETE FROM announcement_attachment_policies WHERE id=?",policy);
        String settings=new ObjectMapper().writeValueAsString(java.util.Map.of("engineVersion",execution.engineVersion(),
                "extractorVersion",execution.extractorVersion(),"extractorConfigHash",CONFIG_HASH,"maximumSourceBytes",80L*1024*1024,
                "segmentRuleVersion",execution.segmentRuleVersion(),"segmentRulesHash",execution.segmentRulesHash()));
        sql.update("INSERT INTO announcement_attachment_policies(id,policy_code,version_no,policy_status_code,mode_code,rule_release_id,policy_hash,settings_json,profile_manifest_json,created_by,published_at) VALUES (?,?,1,'ACTIVE',?,?,repeat('d',64),?::jsonb,?::jsonb,?,now())",
                policy,policy.toString(),enforce?"ENFORCE":"COLLECT_ONLY",release,settings,manifest,actor);
        var base=selectRequest();
        if(enforce)sql.update("UPDATE announcement_source_snapshots SET title=?,is_attachment_review_required=true,attachment_policy_id=? WHERE id=?",
                "구간 최종 검수 QA "+base.sourceId(),policy,base.sourceId());
        service.insertAttachmentJob(new AttachmentJobReservation(base.sourceId(),policy,base.expectedBaseDecisionId(),0,0,base.idempotencyKey(),execution));
        var job=service.saveNextJobClaim().orElseThrow();
        assertThat(service.saveDownloadBytes(job.jobId(),job.leaseToken(),15)).isTrue();
        String text="😀 사업 지원 안내\n지원대상: 소상공인 지원금\n지원내용: 경영지원\n신청기간: 9월\n지원 신청서\n성 명\n(서명 또는 인)\n수출 특허 지원금";
        if(quarterHeading) text=text.replace("😀 사업 지원 안내","참여자 모집 공고(3분기)")
                .replace("지원대상:","❍ (지원대상)").replace("지원내용:","❍ (지원내용)").replace("신청기간:","❍ (신청기간)");
        var blocks=new java.util.ArrayList<AttachmentSetEvidence.Block>(); int offset=0;
        for(String line:text.split("\n")) {
            int end=offset+line.codePointCount(0,line.length());
            blocks.add(new AttachmentSetEvidence.Block(blocks.size(),offset,end,"p:"+blocks.size(),true,"p:"+blocks.size())); offset=end+1;
        }
        var files=new java.util.ArrayList<AttachmentSetEvidence.File>();
        files.add(new AttachmentSetEvidence.File(new AttachmentSetEvidence.Locator(execution.profileCode(),"/download/file",java.util.Map.of("fileId","fixture-1")),
                "혼합 안내.hwpx","HWPX","UNKNOWN","UNKNOWN","SUCCEEDED",15,"a".repeat(64),null,
                new AttachmentSetEvidence.Extraction("COMPLETE_TEXT",text,blocks,null,100)));
        if(includeFailed) files.add(new AttachmentSetEvidence.File(new AttachmentSetEvidence.Locator(execution.profileCode(),"/download/file",java.util.Map.of("fileId","fixture-2")),
                "미완료 첨부.pdf","PDF","UNKNOWN","UNKNOWN","FAILED",0,null,AttachmentFailureCode.NETWORK_TIMEOUT,null));
        evidenceService.saveAttachmentSet(job.jobId(),job.leaseToken(),new AttachmentSetEvidence("FOUND",true,files)).orElseThrow();
        return job;
    }

    @Test void legacyWorkerKeepsQuarterNoticeUnknownAndVersionedGetDoesNotCreateNewAnalysis() throws Exception {
        validatePinnedQuarterAnalysis("segment-role-1.0.0");
    }
    @Test void quarterWorkerPersistsPinnedAnalysisAndLegacyShadowCannotRebindItsEvaluation() throws Exception {
        validatePinnedQuarterAnalysis("segment-role-1.0.2");
    }
    private void validatePinnedQuarterAnalysis(String version) throws Exception {
        boolean quarter="segment-role-1.0.2".equals(version);
        var job=selectSegmentJob(false,false,version,true);
        var evaluator=context.getBean(AnnouncementAttachmentEvaluationService.class);
        var evaluation=evaluator.saveJobEvaluation(job.jobId(),job.leaseToken()).orElseThrow();
        assertThat(evaluation.status()).isEqualTo(quarter?"ACCEPTED":"REVIEW_REQUIRED");
        UUID extraction=sql.queryForObject("SELECT extraction_id FROM announcement_source_attachment_evaluation_inputs WHERE evaluation_id=?",UUID.class,evaluation.evaluationId());
        UUID bound=sql.queryForObject("SELECT segment_analysis_id FROM announcement_source_attachment_evaluation_inputs WHERE evaluation_id=?",UUID.class,evaluation.evaluationId());
        assertThat(sql.queryForObject("SELECT analysis_version FROM announcement_attachment_segment_analyses WHERE id=?",String.class,bound)).isEqualTo(version);
        assertThat(sql.queryForObject("SELECT analysis_json->'segments'->0->>'roleCode' FROM announcement_attachment_segment_analyses WHERE id=?",String.class,bound))
                .isEqualTo(quarter?"NOTICE":"UNKNOWN");
        var session=context.getBean(SqlSessionTemplate.class);
        var segments=new com.saneb.domain.announcementattachment.service.impl.AnnouncementAttachmentSegmentServiceImpl(
                session.getMapper(com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentSegmentDao.class),session.getMapper(AnnouncementSourceDao.class),new ObjectMapper());
        assertThat(segments.selectAnalysisDetails(job.sourceId(),extraction,version).analysisId()).isEqualTo(bound);
        var boundRead=segments.selectEvaluationAnalysisDetails(job.sourceId(),extraction,evaluation.evaluationId());
        assertThat(boundRead.evaluationId()).isEqualTo(evaluation.evaluationId());
        assertThat(boundRead.policyId()).isEqualTo(evaluation.policyId());
        assertThat(boundRead.segmentAnalysis().analysisId()).isEqualTo(bound);
        assertThat(boundRead.segmentAnalysis().analysis().analysisVersion()).isEqualTo(version);
        assertThatThrownBy(()->segments.selectEvaluationAnalysisDetails(job.sourceId(),extraction,UUID.randomUUID())).isInstanceOf(ApiException.class);
        assertThatThrownBy(()->segments.selectEvaluationAnalysisDetails(UUID.randomUUID(),extraction,evaluation.evaluationId())).isInstanceOf(ApiException.class);
        assertThat(segments.selectAnalysisDetails(job.sourceId(),extraction,"segment-role-1.0.2").analysisState()).isEqualTo(quarter?"ANALYZED":"NOT_ANALYZED");
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_attachment_segment_analyses WHERE source_id=?",Integer.class,job.sourceId())).isEqualTo(1);
        if(quarter) {
            assertThat(segments.selectAnalysisDetails(job.sourceId(),extraction).analysisState()).isEqualTo("NOT_ANALYZED");
            var legacy=new TransactionTemplate(context.getBean(PlatformTransactionManager.class)).execute(tx->segments.insertAnalysis(reviewActor(),job.sourceId(),extraction));
            assertThat(legacy.analysis().analysisVersion()).isEqualTo("segment-role-1.0.0");
            assertThat(legacy.analysis().segments().getFirst().roleCode()).isEqualTo("UNKNOWN");
            assertThat(legacy.analysisId()).isNotEqualTo(bound);
            assertThat(segments.selectAnalysisDetails(job.sourceId(),extraction).analysisId()).isEqualTo(legacy.analysisId());
            assertThat(segments.selectEvaluationAnalysisDetails(job.sourceId(),extraction,evaluation.evaluationId()).segmentAnalysis().analysisId()).isEqualTo(bound);
            assertThat(segments.selectAnalysisDetails(job.sourceId(),extraction,version).analysisId()).isEqualTo(bound);
            assertThat(sql.queryForObject("SELECT count(1) FROM announcement_attachment_segment_analyses WHERE source_id=?",Integer.class,job.sourceId())).isEqualTo(2);
        }
        assertThat(evaluator.saveJobEvaluation(job.jobId(),job.leaseToken()).orElseThrow().evaluationId()).isEqualTo(evaluation.evaluationId());
        assertThat(sql.queryForObject("SELECT segment_analysis_id FROM announcement_source_attachment_evaluation_inputs WHERE evaluation_id=?",UUID.class,evaluation.evaluationId())).isEqualTo(bound);
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_source_links WHERE source_id=?",Integer.class,job.sourceId())).isZero();
        assertThat(sql.queryForObject("SELECT semantic_status_code FROM announcement_source_snapshots WHERE id=?",String.class,job.sourceId())).isEqualTo("REVIEW_REQUIRED");
    }

    @Test void att029CompletedEvaluationIsIdempotentOnlyForItsLeaseOwner() {
        service.insertAttachmentJob(selectRequest());
        var job = service.saveNextJobClaim().orElseThrow();
        evidenceService.saveAttachmentSet(job.jobId(), job.leaseToken(), new AttachmentSetEvidence("NO_FILES", true, List.of()));
        var evaluator = context.getBean(AnnouncementAttachmentEvaluationService.class);
        var first = evaluator.saveJobEvaluation(job.jobId(), job.leaseToken()).orElseThrow();
        assertThat(first.status()).isEqualTo("REVIEW_REQUIRED");
        assertThat(first.reason()).isEqualTo("BODY_UNAVAILABLE");
        assertThat(evaluator.saveJobEvaluation(job.jobId(), job.leaseToken()).orElseThrow().evaluationId()).isEqualTo(first.evaluationId());
        assertThat(evaluator.saveJobEvaluation(job.jobId(), UUID.randomUUID())).isEmpty();
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_source_attachment_evaluations", Integer.class)).isEqualTo(1);
        assertThat(sql.queryForObject("SELECT attachment_row_version FROM announcement_source_snapshots WHERE id=?", Integer.class, job.sourceId())).isEqualTo(2);
    }

    @Test void att029ConcurrentEvaluationFinalizersPublishExactlyOneCurrentResult() throws Exception {
        service.insertAttachmentJob(selectRequest());
        var job = service.saveNextJobClaim().orElseThrow();
        evidenceService.saveAttachmentSet(job.jobId(), job.leaseToken(), new AttachmentSetEvidence("NO_FILES", true, List.of()));
        var evaluator = context.getBean(AnnouncementAttachmentEvaluationService.class);
        var gate = new CountDownLatch(1);
        Callable<UUID> evaluate = () -> { gate.await(5, TimeUnit.SECONDS); return evaluator.saveJobEvaluation(job.jobId(), job.leaseToken()).orElseThrow().evaluationId(); };
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(evaluate);
            var second = executor.submit(evaluate);
            gate.countDown();
            assertThat(first.get(15, TimeUnit.SECONDS)).isEqualTo(second.get(15, TimeUnit.SECONDS));
        }
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_source_attachment_evaluations WHERE is_current", Integer.class)).isEqualTo(1);
    }

    @Test void att031UnsealedOrExpiredWorkerCannotPublishEvaluation() {
        service.insertAttachmentJob(selectRequest());
        var job = service.saveNextJobClaim().orElseThrow();
        var evaluator = context.getBean(AnnouncementAttachmentEvaluationService.class);
        assertThatThrownBy(() -> evaluator.saveJobEvaluation(job.jobId(), job.leaseToken()))
                .isInstanceOf(ApiException.class).hasMessageContaining("봉인");
        evidenceService.saveAttachmentSet(job.jobId(), job.leaseToken(), new AttachmentSetEvidence("NO_FILES", true, List.of()));
        updateLeaseExpired(job);
        assertThat(evaluator.saveJobEvaluation(job.jobId(), job.leaseToken())).isEmpty();
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_source_attachment_evaluations", Integer.class)).isZero();
    }

    @Test void att020And023MixedFailurePersistsEveryInputAndNeverReportsFullSuccess() {
        service.insertAttachmentJob(selectRequest());
        var job = service.saveNextJobClaim().orElseThrow();
        service.saveDownloadBytes(job.jobId(), job.leaseToken(), 15);
        var failed = new AttachmentSetEvidence.File(new AttachmentSetEvidence.Locator(EXECUTION.profileCode(),
                "/download/file", java.util.Map.of("fileId", "failed-2")), "추가 공고문.pdf", null,
                "NOTICE", "PROFILE", "FAILED", 0, null, AttachmentFailureCode.NETWORK_TIMEOUT, null);
        evidenceService.saveAttachmentSet(job.jobId(), job.leaseToken(),
                new AttachmentSetEvidence("FOUND", true, List.of(selectFileEvidence(100), failed)));
        var result = context.getBean(AnnouncementAttachmentEvaluationService.class).saveJobEvaluation(job.jobId(), job.leaseToken()).orElseThrow();
        assertThat(result.status()).isEqualTo("REVIEW_REQUIRED");
        assertThat(result.reason()).isEqualTo("ATTACHMENT_INCOMPLETE");
        assertThat(result.warningCodesJson()).contains("NETWORK_TIMEOUT");
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_source_attachment_evaluation_inputs WHERE evaluation_id=?", Integer.class, result.evaluationId())).isEqualTo(2);
        assertThat(dao.selectJobDetails(job.jobId()).jobStatusCode()).isEqualTo("PARTIAL_FAILED");
    }

    @Test void att033NewGenerationKeepsSeparateSelectedEvidenceAndStalesOldDecision() {
        var request = selectRequest();
        service.insertAttachmentJob(request);
        var firstJob = service.saveNextJobClaim().orElseThrow();
        var firstSet = evidenceService.saveAttachmentSet(firstJob.jobId(), firstJob.leaseToken(), new AttachmentSetEvidence("NO_FILES", true, List.of())).orElseThrow();
        var evaluator = context.getBean(AnnouncementAttachmentEvaluationService.class);
        var first = evaluator.saveJobEvaluation(firstJob.jobId(), firstJob.leaseToken()).orElseThrow();
        service.insertAttachmentJob(new AttachmentJobReservation(request.sourceId(), policy, request.expectedBaseDecisionId(),
                0, 2, UUID.randomUUID(), EXECUTION));
        assertThat(dao.selectSourceContextDetails(request.sourceId()).currentAttachmentEvaluationId()).isNull();
        assertThat(sql.queryForObject("SELECT is_current FROM announcement_source_attachment_evaluations WHERE id=?",
                Boolean.class, first.evaluationId())).isFalse();
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_source_attachment_evaluations", Integer.class)).isEqualTo(1);
        var secondJob = service.saveNextJobClaim().orElseThrow();
        var secondSet = evidenceService.saveAttachmentSet(secondJob.jobId(), secondJob.leaseToken(), new AttachmentSetEvidence("NO_FILES", true, List.of())).orElseThrow();
        var second = evaluator.saveJobEvaluation(secondJob.jobId(), secondJob.leaseToken()).orElseThrow();
        assertThat(secondSet.manifestHash()).isNotEqualTo(firstSet.manifestHash());
        assertThat(second.inputHash()).isNotEqualTo(first.inputHash());
        assertThat(second.evaluationId()).isNotEqualTo(first.evaluationId());
        assertThat(sql.queryForObject("SELECT is_current FROM announcement_source_attachment_evaluations WHERE id=?", Boolean.class, first.evaluationId())).isFalse();
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_source_attachment_evaluations", Integer.class)).isEqualTo(2);
    }

    @Test void currentProjectionSeparatesCollectOnlyPreviewFromListFiltersAndCount() {
        var request=selectRequest();
        service.insertAttachmentJob(request);
        var job=service.saveNextJobClaim().orElseThrow();
        service.saveDownloadBytes(job.jobId(),job.leaseToken(),15);
        evidenceService.saveAttachmentSet(job.jobId(),job.leaseToken(),new AttachmentSetEvidence("FOUND",true,List.of(selectFileEvidence(100))));
        var evaluated=context.getBean(AnnouncementAttachmentEvaluationService.class).saveJobEvaluation(job.jobId(),job.leaseToken()).orElseThrow();
        var reader=context.getBean(com.saneb.domain.announcementattachment.service.AnnouncementAttachmentCurrentService.class);
        var details=reader.selectClassificationDetails(request.sourceId());
        assertThat(details.baseClassification().decisionId()).isEqualTo(request.expectedBaseDecisionId());
        assertThat(details.effectiveClassification().semanticStatusCode()).isEqualTo("REVIEW_REQUIRED");
        assertThat(details.previewClassification().decisionId()).isEqualTo(evaluated.evaluationId());
        assertThat(details.previewClassification().targetCategoryCodes()).contains("BUSINESS");
        assertThat(details.processingFlow().statusCode()).isEqualTo("NOT_APPLIED");
        assertThat(details.processingFlow().isAutomaticAnalysisComplete()).isFalse();
        var accepted=reader.selectSourceList(selectCurrentSearch("ACCEPTED",null));
        assertThat(accepted.totalCount()).isZero();
        assertThat(accepted.items()).isEmpty();
        var reviews=reader.selectSourceList(selectCurrentSearch("REVIEW_REQUIRED",null));
        assertThat(reviews.totalCount()).isEqualTo(1);
        assertThat(reviews.items()).containsExactly(details);
    }

    @Test void currentProjectionEnforcePendingStalesBeforeWorkerAndOffDoesNotRestoreBase() {
        var request=selectRequest();
        sql.update("UPDATE announcement_source_snapshots SET is_attachment_review_required=true,attachment_policy_id=? WHERE id=?",policy,request.sourceId());
        service.insertAttachmentJob(request);
        var reader=context.getBean(com.saneb.domain.announcementattachment.service.AnnouncementAttachmentCurrentService.class);
        assertThat(reader.selectClassificationDetails(request.sourceId()).effectiveClassification().decisionId()).isNull();
        assertThat(reader.selectClassificationDetails(request.sourceId()).processingFlow().statusCode()).isEqualTo("AUTOMATIC_PROCESSING");
        assertThat(reader.selectClassificationDetails(request.sourceId()).processingFlow().isFinalReviewAvailable()).isFalse();
        var job=service.saveNextJobClaim().orElseThrow();
        service.saveDownloadBytes(job.jobId(),job.leaseToken(),15);
        evidenceService.saveAttachmentSet(job.jobId(),job.leaseToken(),new AttachmentSetEvidence("FOUND",true,List.of(selectFileEvidence(100))));
        var first=context.getBean(AnnouncementAttachmentEvaluationService.class).saveJobEvaluation(job.jobId(),job.leaseToken()).orElseThrow();
        var accepted=reader.selectSourceList(selectCurrentSearch("ACCEPTED","BUSINESS"));
        assertThat(accepted.totalCount()).isEqualTo(1);
        assertThat(accepted.items().getFirst().effectiveClassification().decisionId()).isEqualTo(first.evaluationId());
        service.insertAttachmentJob(new AttachmentJobReservation(request.sourceId(),policy,request.expectedBaseDecisionId(),0,2,UUID.randomUUID(),EXECUTION));
        sql.update("UPDATE announcement_attachment_policies SET policy_status_code='RETIRED',row_version=row_version+1 WHERE id=?",policy);
        insertOffControlPolicy();
        var pending=reader.selectClassificationDetails(request.sourceId());
        assertThat(pending.effectiveClassification().decisionId()).isNull();
        assertThat(pending.effectiveClassification().reasonCode()).isEqualTo("ATTACHMENT_PENDING");
        assertThat(pending.attachmentSummary().isStale()).isTrue();
        assertThat(pending.attachmentSummary().jobStatusCode()).isEqualTo("PENDING");
        assertThat(pending.attachmentSummary().totalCount()).isNull();
        assertThat(reader.selectSourceList(selectCurrentSearch("ACCEPTED",null)).totalCount()).isZero();
        assertThat(reader.selectSourceList(selectCurrentSearch("REVIEW_REQUIRED","BUSINESS")).totalCount()).isZero();
        assertThat(pending.isAttachmentReviewRequired()).isTrue();
    }

    @Test void currentProjectionUsesConfirmedAttachmentTagsWithoutChangingAutomaticHistory() {
        var request=selectRequest();
        sql.update("UPDATE announcement_source_snapshots SET is_attachment_review_required=true,attachment_policy_id=? WHERE id=?",policy,request.sourceId());
        service.insertAttachmentJob(request);
        var job=service.saveNextJobClaim().orElseThrow();
        service.saveDownloadBytes(job.jobId(),job.leaseToken(),15);
        var set=evidenceService.saveAttachmentSet(job.jobId(),job.leaseToken(),new AttachmentSetEvidence("FOUND",true,List.of(selectFileEvidence(100)))).orElseThrow();
        var evaluated=context.getBean(AnnouncementAttachmentEvaluationService.class).saveJobEvaluation(job.jobId(),job.leaseToken()).orElseThrow();
        UUID confirmation=UUID.randomUUID();
        sql.update("""
            INSERT INTO announcement_source_attachment_confirmations
            (id,source_id,evaluation_id,set_hash,confirmed_by,review_method_code,acknowledged_error_codes_json,review_note,is_current,idempotency_key,request_hash,
             confirmed_source_version,confirmed_attachment_version)
            VALUES (?,?,?,?,?,'EXTRACTED_TEXT','[]','격리 확인 fixture',true,?,repeat('a',64),0,2)
            """,confirmation,request.sourceId(),evaluated.evaluationId(),set.manifestHash(),actor,UUID.randomUUID());
        sql.update("""
            INSERT INTO announcement_source_attachment_tags(evaluation_id,confirmation_id,target_category_id,origin_code)
            SELECT ?,?,id,'CONFIRMED' FROM announcement_target_categories WHERE category_code='PERSONAL'
            """,evaluated.evaluationId(),confirmation);
        var reader=context.getBean(com.saneb.domain.announcementattachment.service.AnnouncementAttachmentCurrentService.class);
        var details=reader.selectClassificationDetails(request.sourceId());
        assertThat(details.effectiveClassification().targetCategoryCodes()).containsExactly("PERSONAL");
        assertThat(details.confirmationStatusCode()).isEqualTo("CURRENT");
        assertThat(details.confirmationId()).isEqualTo(confirmation);
        assertThat(details.processingFlow().statusCode()).isEqualTo("FINAL_REVIEW_CONFIRMED");
        assertThat(reader.selectSourceList(selectCurrentSearch("ACCEPTED","BUSINESS")).totalCount()).isZero();
        assertThat(reader.selectSourceList(selectCurrentSearch("ACCEPTED","PERSONAL")).totalCount()).isEqualTo(1);
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_source_attachment_tags WHERE evaluation_id=? AND origin_code='AUTO'",Integer.class,evaluated.evaluationId())).isGreaterThan(1);
    }

    @Test void currentProjectionDoesNotReturnExcludedSourceOrItsTitle() {
        var request=selectRequest();
        sql.update("UPDATE announcement_source_snapshots SET semantic_status_code='EXCLUDED' WHERE id=?",request.sourceId());
        var reader=context.getBean(com.saneb.domain.announcementattachment.service.AnnouncementAttachmentCurrentService.class);
        assertThat(reader.selectSourceList(selectCurrentSearch(null,null)).totalCount()).isZero();
        assertThatThrownBy(()->reader.selectSourceDetails(request.sourceId())).isInstanceOf(ApiException.class);
    }

    private com.saneb.domain.announcementattachment.vo.AttachmentSourceSearchCondition selectCurrentSearch(String status,String target) {
        return new com.saneb.domain.announcementattachment.vo.AttachmentSourceSearchCondition("BIZINFO",status,null,target,null,null,null,null,1,20);
    }

    @Test void processingQueueMatchesJavaProjectionForAllNineStatesAndFullPageCounts() {
        // 이 테스트 소유 loopback DB에만 21건 이상의 같은 상태를 만들고 LIMIT 전 count를 검증한다.
        for(int index=0;index<21;index++) selectRequest();
        var pending=selectRequest();
        sql.update("UPDATE announcement_source_snapshots SET is_attachment_review_required=true,attachment_policy_id=? WHERE id=?",policy,pending.sourceId());
        sql.update("UPDATE announcement_source_classification_evaluations SET is_current=false WHERE id=?",pending.expectedBaseDecisionId());
        var configured=selectRequest();
        sql.update("UPDATE announcement_source_snapshots SET is_attachment_review_required=true,attachment_policy_id=?,attachment_intake_status_code='PROFILE_REQUIRED' WHERE id=?",policy,configured.sourceId());
        var automatic=selectRequest();
        sql.update("UPDATE announcement_source_snapshots SET is_attachment_review_required=true,attachment_policy_id=? WHERE id=?",policy,automatic.sourceId());

        UUID confirmedSource=null;
        for(String kind:List.of("TECHNICAL_EXCEPTION","READY_FOR_FINAL_REVIEW","FINAL_REVIEW_EXCEPTION","EVIDENCE_STALE","FINAL_REVIEW_CONFIRMED")) {
            var request=selectRequest();
            sql.update("UPDATE announcement_source_snapshots SET is_attachment_review_required=true,attachment_policy_id=? WHERE id=?",policy,request.sourceId());
            service.insertAttachmentJob(request);
            var job=service.saveNextJobClaim().orElseThrow();
            assertThat(job.sourceId()).isEqualTo(request.sourceId());
            var file=selectFileEvidence(100);
            if(kind.equals("FINAL_REVIEW_EXCEPTION")) file=new AttachmentSetEvidence.File(file.locator(),file.displayName(),file.detectedType(),
                    "UNKNOWN","UNKNOWN",file.downloadStatus(),file.downloadedBytes(),file.binaryHash(),file.failureCode(),file.extraction());
            boolean noFiles=kind.equals("TECHNICAL_EXCEPTION");
            if(!noFiles) service.saveDownloadBytes(job.jobId(),job.leaseToken(),15);
            var set=evidenceService.saveAttachmentSet(job.jobId(),job.leaseToken(),new AttachmentSetEvidence(
                    noFiles?"NO_FILES":"FOUND",true,noFiles?List.of():List.of(file))).orElseThrow();
            var evaluation=context.getBean(AnnouncementAttachmentEvaluationService.class).saveJobEvaluation(job.jobId(),job.leaseToken()).orElseThrow();
            if(kind.equals("EVIDENCE_STALE")) {
                // 현재 포인터와 평가 플래그는 같은 transaction에서 해제한다. DB 무결성 제약을 우회하지 않는다.
                new TransactionTemplate(context.getBean(PlatformTransactionManager.class)).executeWithoutResult(status -> {
                    sql.update("UPDATE announcement_source_snapshots SET current_attachment_evaluation_id=NULL WHERE id=?",request.sourceId());
                    sql.update("UPDATE announcement_source_attachment_evaluations SET is_current=false WHERE id=?",evaluation.evaluationId());
                });
            }
            if(kind.equals("FINAL_REVIEW_CONFIRMED")) {
                confirmedSource=request.sourceId();
                sql.update("""
                    INSERT INTO announcement_source_attachment_confirmations
                    (id,source_id,evaluation_id,set_hash,confirmed_by,review_method_code,acknowledged_error_codes_json,review_note,is_current,idempotency_key,request_hash,
                     confirmed_source_version,confirmed_attachment_version)
                    VALUES (?,?,?,?,?,'EXTRACTED_TEXT','[]','격리 대기열 검증',true,?,repeat('a',64),0,2)
                    """,UUID.randomUUID(),request.sourceId(),evaluation.evaluationId(),set.manifestHash(),actor,UUID.randomUUID());
            }
        }
        var reader=context.getBean(com.saneb.domain.announcementattachment.service.AnnouncementAttachmentCurrentService.class);
        var all=reader.selectSourceList(new com.saneb.domain.announcementattachment.vo.AttachmentSourceSearchCondition(
                null,null,null,null,null,null,null,null,1,100));
        assertThat(all.totalCount()).isEqualTo(29);
        assertThat(all.items()).extracting(row->row.processingFlow().statusCode())
                .containsAll(com.saneb.domain.announcementattachment.service.AttachmentProcessingFlow.STATUS_CODES);
        for(String code:com.saneb.domain.announcementattachment.service.AttachmentProcessingFlow.STATUS_CODES) {
            var expected=all.items().stream().filter(row->row.processingFlow().statusCode().equals(code)).toList();
            var found=new java.util.ArrayList<UUID>();
            for(int pageNo=1;pageNo<=Math.max(1,(expected.size()+4)/5)+1;pageNo++) {
                var filtered=reader.selectSourceList(new com.saneb.domain.announcementattachment.vo.AttachmentSourceSearchCondition(
                        "BIZINFO",null,null,null,null,"소상공인",null,null,pageNo,5,code));
                assertThat(filtered.totalCount()).as(code).isEqualTo(expected.size());
                assertThat(filtered.items()).allSatisfy(row->assertThat(row.processingFlow().statusCode()).isEqualTo(code));
                found.addAll(filtered.items().stream().map(row->row.sourceId()).toList());
            }
            assertThat(found).as(code).containsExactlyElementsOf(expected.stream().map(row->row.sourceId()).toList());
        }
        assertThat(reader.selectClassificationDetails(confirmedSource).processingFlow().isAutomaticAnalysisComplete()).isTrue();
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_source_links",Integer.class)).isZero();
    }

    @Test void att044AcceptedAttachmentCannotClearExistingEnforceGuard() {
        var request = selectRequest();
        sql.update("UPDATE announcement_source_snapshots SET attachment_policy_id=?,is_attachment_review_required=true WHERE id=?", policy, request.sourceId());
        service.insertAttachmentJob(request);
        var job = service.saveNextJobClaim().orElseThrow();
        service.saveDownloadBytes(job.jobId(), job.leaseToken(), 15);
        evidenceService.saveAttachmentSet(job.jobId(), job.leaseToken(), new AttachmentSetEvidence("FOUND", true, List.of(selectFileEvidence(100))));
        var result = context.getBean(AnnouncementAttachmentEvaluationService.class).saveJobEvaluation(job.jobId(), job.leaseToken()).orElseThrow();
        assertThat(result.status()).isEqualTo("ACCEPTED");
        assertThat(sql.queryForObject("SELECT is_attachment_review_required FROM announcement_source_snapshots WHERE id=?", Boolean.class, job.sourceId())).isTrue();
        assertThatThrownBy(() -> AttachmentLegacyPathGuard.validate(dao.selectSourceContextDetails(job.sourceId()).attachmentReviewRequired()))
                .isInstanceOf(ApiException.class);
    }

    @Test void att001And031WorkerLocatorIsUnavailableForWrongOwnerExcludedOrChangedBase() {
        var request = selectRequest();
        service.insertAttachmentJob(request);
        var job = service.saveNextJobClaim().orElseThrow();
        assertThat(service.selectWorkerSourceDetails(job.jobId(),job.leaseToken()).orElseThrow().providerCode()).isEqualTo("BIZINFO");
        assertThat(service.selectWorkerSourceDetails(job.jobId(),UUID.randomUUID())).isEmpty();
        sql.update("UPDATE announcement_source_snapshots SET semantic_status_code='EXCLUDED' WHERE id=?",job.sourceId());
        assertThat(service.selectWorkerSourceDetails(job.jobId(),job.leaseToken())).isEmpty();
        sql.update("UPDATE announcement_source_snapshots SET semantic_status_code='REVIEW_REQUIRED',classification_row_version=classification_row_version+1 WHERE id=?",job.sourceId());
        assertThat(service.selectWorkerSourceDetails(job.jobId(),job.leaseToken())).isEmpty();
    }

    @Test void att040OffImmediatelyBlocksExternalBytesAndResourcesButNotSealedRecovery() {
        service.insertAttachmentJob(selectRequest());
        var job=service.saveNextJobClaim().orElseThrow();
        assertThat(service.selectExternalExecutionAllowed(job.jobId(),job.leaseToken())).isTrue();
        assertThat(service.selectExternalExecutionAllowed(job.jobId(),UUID.randomUUID())).isFalse();
        evidenceService.saveAttachmentSet(job.jobId(),job.leaseToken(),new AttachmentSetEvidence("NO_FILES",true,List.of()));
        sql.update("UPDATE announcement_attachment_policies SET policy_status_code='RETIRED',row_version=row_version+1 WHERE id=?",policy);
        // 퇴역은 고정 버전을 바꾸지 않는다. 새 OFF 정책만 실행 중 요청까지 중지한다.
        assertThat(service.selectExternalExecutionAllowed(job.jobId(),job.leaseToken())).isTrue();
        insertOffControlPolicy();
        assertThat(service.selectExternalExecutionAllowed(job.jobId(),job.leaseToken())).isFalse();
        assertThat(service.saveDownloadBytes(job.jobId(),job.leaseToken(),1)).isFalse();
        assertThat(service.saveDownloadLease(job.jobId(),job.leaseToken(),"2".repeat(64))).isEmpty();
        assertThat(service.saveExtractionLease(job.jobId(),job.leaseToken())).isEmpty();
        assertThat(service.saveJobFailure(job.jobId(),job.leaseToken(),AttachmentFailureCode.NETWORK_TIMEOUT)).isTrue();
        sql.update("UPDATE announcement_attachment_jobs SET next_attempt_at=now()-interval '1 second' WHERE id=?",job.jobId());
        var resumed=service.saveNextJobClaim().orElseThrow();
        assertThat(resumed.setId()).isNotNull();
        assertThat(context.getBean(AnnouncementAttachmentEvaluationService.class).saveJobEvaluation(resumed.jobId(),resumed.leaseToken())).isPresent();
    }

    @Test void resourceDeferralDoesNotConsumeAnUnstartedAttemptOrPermitOffNetworkClaim() {
        service.insertAttachmentJob(selectRequest());
        var job=service.saveNextJobClaim().orElseThrow();
        assertThat(service.saveJobDeferred(job.jobId(),UUID.randomUUID(),false)).isFalse();
        assertThat(service.saveJobDeferred(job.jobId(),job.leaseToken(),false)).isTrue();
        assertThat(dao.selectJobDetails(job.jobId()).attemptCount()).isZero();
        sql.update("UPDATE announcement_attachment_jobs SET next_attempt_at=now()-interval '1 second' WHERE id=?",job.jobId());
        var resumed=service.saveNextJobClaim().orElseThrow();
        assertThat(resumed.attemptCount()).isEqualTo(1);
        assertThat(service.saveDownloadBytes(resumed.jobId(),resumed.leaseToken(),20)).isTrue();
        assertThat(service.saveJobDeferred(resumed.jobId(),resumed.leaseToken(),true)).isTrue();
        assertThat(dao.selectJobDetails(job.jobId()).attemptCount()).isEqualTo(1);
        assertThat(dao.selectJobDetails(job.jobId()).reservedDownloadBytes()).isEqualTo(20);
        sql.update("UPDATE announcement_attachment_jobs SET next_attempt_at=now()-interval '1 second' WHERE id=?",job.jobId());
        sql.update("UPDATE announcement_attachment_policies SET policy_status_code='RETIRED',row_version=row_version+1 WHERE id=?",policy);
        insertOffControlPolicy();
        assertThat(service.saveNextJobClaim()).isEmpty();
    }

    private void insertOffControlPolicy() {
        UUID off=UUID.randomUUID();
        sql.update("""
            INSERT INTO announcement_attachment_policies
            (id,policy_code,version_no,policy_status_code,mode_code,rule_release_id,policy_hash,settings_json,profile_manifest_json,created_by,published_at)
            SELECT ?,?,1,'ACTIVE','OFF',rule_release_id,repeat('e',64),settings_json,profile_manifest_json,created_by,now()
            FROM announcement_attachment_policies WHERE id=?
            """,off,off.toString(),policy);
    }

    @Test void att013SelectorWarningIsBoundToImmutableManifestAndReadApi() {
        assertDiscoveryWarningBoundToImmutableManifestAndReadApi("ATTACHMENT_SELECTOR_CHANGED");
    }
    @Test void att013DownloadFormWarningIsBoundToImmutableManifestAndReadApi() {
        assertDiscoveryWarningBoundToImmutableManifestAndReadApi("ATTACHMENT_DOWNLOAD_FORM_CHANGED");
    }
    private void assertDiscoveryWarningBoundToImmutableManifestAndReadApi(String warning) {
        service.insertAttachmentJob(selectRequest());
        var job=service.saveNextJobClaim().orElseThrow();
        var set=evidenceService.saveAttachmentSet(job.jobId(),job.leaseToken(),new AttachmentSetEvidence("FAILED",false,List.of(),
                List.of(warning))).orElseThrow();
        assertThat(set.warningCodes()).containsExactly(warning);
        assertThat(context.getBean(AnnouncementAttachmentReadService.class).selectAttachmentSetList(job.sourceId(),1,10).items().getFirst().warningCodes())
                .containsExactly(warning);
        assertThatThrownBy(() -> evidenceService.saveAttachmentSet(job.jobId(),job.leaseToken(),new AttachmentSetEvidence("FAILED",false,List.of(),
                List.of("ATTACHMENT_LINK_UNRESOLVED")))).isInstanceOf(ApiException.class).hasMessageContaining("이미 저장된");
        assertThatThrownBy(() -> sql.update("UPDATE announcement_source_attachment_sets SET discovery_warning_codes_json='[]'::jsonb WHERE id=?",set.setId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private void updateLeaseExpired(AttachmentJobRow job) {
        sql.update("UPDATE announcement_attachment_jobs SET lease_expires_at=now()-interval '1 second' WHERE id=?", job.jobId());
    }

    @Test void att040And041AutomaticNewEnforceBindingIsAtomicAndRecordsRecoveryMetadata() {
        updateFixturePolicyMode("ENFORCE");
        UUID run=insertFixtureRun();
        var request=selectIntakeSource(run);
        var intake=context.getBean(com.saneb.domain.announcementattachment.service.AnnouncementAttachmentIntakeService.class);
        var plan=intake.saveCollectionPlan(run,release);
        var transaction=new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
        transaction.executeWithoutResult(status -> {
            intake.saveCollectedSource(request.sourceId(),plan,true);
            var source=dao.selectSourceContextDetails(request.sourceId());
            assertThat(source.attachmentReviewRequired()).isTrue();
            assertThat(source.attachmentPolicyId()).isEqualTo(policy);
            assertThat(source.currentAttachmentEvaluationId()).isNull();
            assertThatThrownBy(() -> AttachmentLegacyPathGuard.validate(source.attachmentReviewRequired())).isInstanceOf(ApiException.class);
        });
        var job=service.saveNextJobClaim().orElseThrow();
        assertThat(sql.queryForObject("SELECT previous_is_review_required FROM announcement_attachment_jobs WHERE id=?",Boolean.class,job.jobId())).isFalse();
        assertThat(sql.queryForObject("SELECT applied_attachment_version FROM announcement_attachment_jobs WHERE id=?",Integer.class,job.jobId())).isEqualTo(1);
        assertThat(sql.queryForObject("SELECT application_status_code FROM announcement_attachment_jobs WHERE id=?",String.class,job.jobId())).isEqualTo("PENDING");
        evidenceService.saveAttachmentSet(job.jobId(),job.leaseToken(),new AttachmentSetEvidence("NO_FILES",true,List.of()));
        var result=context.getBean(AnnouncementAttachmentEvaluationService.class).saveJobEvaluation(job.jobId(),job.leaseToken()).orElseThrow();
        assertThat(sql.queryForObject("SELECT applied_evaluation_id FROM announcement_attachment_jobs WHERE id=?",UUID.class,job.jobId())).isEqualTo(result.evaluationId());
        assertThat(sql.queryForObject("SELECT applied_attachment_version FROM announcement_attachment_jobs WHERE id=?",Integer.class,job.jobId())).isEqualTo(2);
        assertThat(sql.queryForObject("SELECT application_status_code FROM announcement_attachment_jobs WHERE id=?",String.class,job.jobId())).isEqualTo("APPLIED");
        assertThat(sql.queryForObject("SELECT reservation_source_version FROM announcement_attachment_jobs WHERE id=?",Integer.class,job.jobId())).isZero();
        assertThat(sql.queryForObject("SELECT reservation_attachment_version FROM announcement_attachment_jobs WHERE id=?",Integer.class,job.jobId())).isZero();
        assertThat(sql.queryForObject("SELECT applied_source_version FROM announcement_attachment_jobs WHERE id=?",Integer.class,job.jobId())).isZero();
        assertThat(sql.queryForObject("SELECT applied_input_hash FROM announcement_attachment_jobs WHERE id=?",String.class,job.jobId())).matches("[0-9a-f]{64}");
        assertThat(sql.queryForObject("SELECT applied_input_hash=attachment_normal_job_application_hash(id,applied_evaluation_id) FROM announcement_attachment_jobs WHERE id=?",Boolean.class,job.jobId())).isTrue();
        transaction.executeWithoutResult(status -> intake.saveCollectedSource(request.sourceId(),plan,false));
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_attachment_jobs WHERE source_id=?",Integer.class,request.sourceId())).isEqualTo(1);
    }

    @Test void normalReservationEvidenceCannotBeRewrittenAfterPreviousBindingBecomesStale() {
        var job=service.insertAttachmentJob(selectRequest());
        assertThat(sql.queryForObject("SELECT reservation_attachment_version FROM announcement_attachment_jobs WHERE id=?",Integer.class,job.jobId())).isZero();
        assertThat(job.expectedAttachmentVersion()).isEqualTo(1);
        assertThat(sql.queryForObject("SELECT is_reservation_previous_evaluation_current FROM announcement_attachment_jobs WHERE id=?",Boolean.class,job.jobId())).isFalse();
        assertThat(sql.queryForObject("SELECT is_reservation_confirmation_valid FROM announcement_attachment_jobs WHERE id=?",Boolean.class,job.jobId())).isFalse();
        assertThatThrownBy(()->sql.update("UPDATE announcement_attachment_jobs SET reservation_attachment_version=10 WHERE id=?",job.jobId())).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(()->sql.update("UPDATE announcement_attachment_jobs SET previous_is_review_required=true WHERE id=?",job.jobId())).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(()->sql.update("UPDATE announcement_attachment_jobs SET is_reservation_confirmation_valid=true WHERE id=?",job.jobId())).isInstanceOf(DataIntegrityViolationException.class);
    }
    @Test void normalAppliedEvidenceIsImmutableAndCannotPretendRollbackWasApproved() {
        UUID source=insertReviewFixture(false);
        UUID job=sql.queryForObject("SELECT id FROM announcement_attachment_jobs WHERE source_id=?",UUID.class,source);
        String original=sql.queryForObject("SELECT applied_input_hash FROM announcement_attachment_jobs WHERE id=?",String.class,job);
        assertThat(original).matches("[0-9a-f]{64}");
        assertThatThrownBy(()->sql.update("UPDATE announcement_attachment_jobs SET applied_input_hash=repeat('0',64) WHERE id=?",job)).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(()->sql.update("UPDATE announcement_attachment_jobs SET application_status_code='PENDING' WHERE id=?",job)).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(()->sql.update("UPDATE announcement_attachment_jobs SET rollback_status_code='ROLLED_BACK' WHERE id=?",job)).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(sql.queryForObject("SELECT applied_input_hash FROM announcement_attachment_jobs WHERE id=?",String.class,job)).isEqualTo(original);
    }
    @Test void normalAppliedFingerprintChangesAfterSubsequentConfirmationWithoutRewritingHistory() {
        UUID source=insertReviewFixture(false);
        UUID job=sql.queryForObject("SELECT id FROM announcement_attachment_jobs WHERE source_id=?",UUID.class,source);
        String applied=sql.queryForObject("SELECT applied_input_hash FROM announcement_attachment_jobs WHERE id=?",String.class,job);
        reviewService().insertConfirmation(reviewActor(),source,UUID.randomUUID(),selectReviewRequest(source));
        assertThat(sql.queryForObject("SELECT applied_input_hash FROM announcement_attachment_jobs WHERE id=?",String.class,job)).isEqualTo(applied);
        assertThat(sql.queryForObject("SELECT attachment_normal_job_application_hash(id,applied_evaluation_id) FROM announcement_attachment_jobs WHERE id=?",String.class,job)).isNotEqualTo(applied);
    }
    @Test void normalPreviewDoesNotGainAppliedRecoveryEvidence() {
        service.insertAttachmentJob(selectRequest());var job=service.saveNextJobClaim().orElseThrow();
        evidenceService.saveAttachmentSet(job.jobId(),job.leaseToken(),new AttachmentSetEvidence("NO_FILES",true,List.of()));
        context.getBean(AnnouncementAttachmentEvaluationService.class).saveJobEvaluation(job.jobId(),job.leaseToken()).orElseThrow();
        assertThat(sql.queryForObject("SELECT application_status_code FROM announcement_attachment_jobs WHERE id=?",String.class,job.jobId())).isEqualTo("NOT_REQUESTED");
        assertThat(sql.queryForObject("SELECT applied_input_hash FROM announcement_attachment_jobs WHERE id=?",String.class,job.jobId())).isNull();
        assertThat(sql.queryForObject("SELECT applied_source_version FROM announcement_attachment_jobs WHERE id=?",Integer.class,job.jobId())).isNull();
        assertThatThrownBy(()->sql.update("UPDATE announcement_attachment_jobs SET application_status_code='APPLIED' WHERE id=?",job.jobId())).isInstanceOf(DataIntegrityViolationException.class);
    }
    @Test void normalReservationCapturesAValidConfirmationBeforeStalingIt() { assertNormalReservationConfirmationValidity(false); }
    @Test void normalReservationDoesNotBlessAnAlreadyStaleConfirmation() { assertNormalReservationConfirmationValidity(true); }
    private void assertNormalReservationConfirmationValidity(boolean stale) {
        UUID source=insertReviewFixture(false);
        reviewService().insertConfirmation(reviewActor(),source,UUID.randomUUID(),selectReviewRequest(source));
        if(stale)sql.update("UPDATE announcement_source_snapshots SET attachment_row_version=attachment_row_version+1 WHERE id=?",source);
        var before=dao.selectSourceContextDetails(source);
        var request=new AttachmentJobReservation(source,policy,before.baseEvaluationId(),before.sourceVersion(),before.attachmentVersion(),UUID.randomUUID(),EXECUTION);
        var reserved=service.insertAttachmentJob(request);
        assertThat(sql.queryForObject("SELECT reservation_attachment_version FROM announcement_attachment_jobs WHERE id=?",Integer.class,reserved.jobId())).isEqualTo(before.attachmentVersion());
        assertThat(sql.queryForObject("SELECT is_reservation_previous_evaluation_current FROM announcement_attachment_jobs WHERE id=?",Boolean.class,reserved.jobId())).isTrue();
        assertThat(sql.queryForObject("SELECT is_reservation_confirmation_valid FROM announcement_attachment_jobs WHERE id=?",Boolean.class,reserved.jobId())).isEqualTo(!stale);
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_source_attachment_confirmations WHERE source_id=? AND is_current",Integer.class,source)).isZero();
    }
    @Test void normalEnforceRejectsChangedLocatorAtFinalApplicationAndRollsBackTheEvaluationWrite() {
        updateFixturePolicyMode("ENFORCE");var request=selectRequest();
        sql.update("UPDATE announcement_source_snapshots SET is_attachment_review_required=true,attachment_policy_id=? WHERE id=?",policy,request.sourceId());
        service.insertAttachmentJob(request);var job=service.saveNextJobClaim().orElseThrow();
        evidenceService.saveAttachmentSet(job.jobId(),job.leaseToken(),new AttachmentSetEvidence("NO_FILES",true,List.of()));
        sql.update("UPDATE announcement_source_snapshots SET provider_notice_id=coalesce(provider_notice_id,'')||'-changed' WHERE id=?",request.sourceId());
        assertThat(sql.queryForObject("SELECT reservation_locator_hash IS DISTINCT FROM attachment_source_locator_hash(source_id) FROM announcement_attachment_jobs WHERE id=?",
                Boolean.class,job.jobId())).isTrue();
        assertThatThrownBy(()->context.getBean(AnnouncementAttachmentEvaluationService.class).saveJobEvaluation(job.jobId(),job.leaseToken())).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(dao.selectSourceContextDetails(request.sourceId()).attachmentVersion()).isEqualTo(job.expectedAttachmentVersion());
        assertThat(dao.selectSourceContextDetails(request.sourceId()).currentAttachmentEvaluationId()).isNull();
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_source_attachment_evaluations WHERE source_id=?",Integer.class,request.sourceId())).isZero();
    }
    @Test void normalPartialFailureKeepsFailureStatusWhileRecordingItsAppliedReviewGuard() {
        UUID source=insertReviewFixture(true);
        assertThat(sql.queryForObject("SELECT job_status_code FROM announcement_attachment_jobs WHERE source_id=?",String.class,source)).isEqualTo("PARTIAL_FAILED");
        assertThat(sql.queryForObject("SELECT application_status_code FROM announcement_attachment_jobs WHERE source_id=?",String.class,source)).isEqualTo("APPLIED");
        assertThat(sql.queryForObject("SELECT applied_input_hash=attachment_normal_job_application_hash(id,applied_evaluation_id) FROM announcement_attachment_jobs WHERE source_id=?",Boolean.class,source)).isTrue();
        assertThat(dao.selectSourceContextDetails(source).attachmentReviewRequired()).isTrue();
    }
    @Test void att040ExistingSourceCollectionDoesNotAutomaticallyOptIntoEnforce() {
        updateFixturePolicyMode("ENFORCE");
        UUID originalRun=insertFixtureRun(),newRun=insertFixtureRun();
        var request=selectIntakeSource(originalRun);
        var intake=context.getBean(com.saneb.domain.announcementattachment.service.AnnouncementAttachmentIntakeService.class);
        var plan=intake.saveCollectionPlan(newRun,release);
        var transaction=new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
        transaction.executeWithoutResult(status -> intake.saveCollectedSource(request.sourceId(),plan,false));
        assertThat(dao.selectSourceContextDetails(request.sourceId()).attachmentReviewRequired()).isFalse();
        assertThat(dao.selectSourceContextDetails(request.sourceId()).attachmentPolicyId()).isNull();
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_attachment_jobs WHERE source_id=?",Integer.class,request.sourceId())).isEqualTo(1);
    }

    @Test void sameContentOnALaterRunWaits24HoursThenCreatesANewGeneration() {
        UUID firstRun=insertFixtureRun();
        var request=selectIntakeSource(firstRun);
        var intake=context.getBean(com.saneb.domain.announcementattachment.service.AnnouncementAttachmentIntakeService.class);
        var firstPlan=intake.saveCollectionPlan(firstRun,release);
        var transaction=new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
        transaction.executeWithoutResult(status -> intake.saveCollectedSource(request.sourceId(),firstPlan,true));
        var first=service.saveNextJobClaim().orElseThrow();
        evidenceService.saveAttachmentSet(first.jobId(),first.leaseToken(),new AttachmentSetEvidence("NO_FILES",true,List.of()));
        context.getBean(AnnouncementAttachmentEvaluationService.class).saveJobEvaluation(first.jobId(),first.leaseToken()).orElseThrow();
        var secondPlan=intake.saveCollectionPlan(insertFixtureRun(),release);
        transaction.executeWithoutResult(status -> intake.saveCollectedSource(request.sourceId(),secondPlan,false));
        assertThat(service.saveNextJobClaim()).isEmpty();
        assertThat(sql.queryForObject("SELECT attachment_intake_status_code FROM announcement_source_snapshots WHERE id=?",String.class,request.sourceId()))
                .isEqualTo("RECHECK_NOT_DUE");
        assertThat(dao.selectSourceContextDetails(request.sourceId()).currentAttachmentEvaluationId()).isNotNull();
        assertThat(dao.selectSourceContextDetails(request.sourceId()).attachmentVersion()).isEqualTo(2);
        // 임시 DB fixture의 시각만 이동한다. 테스트/운영에서 24시간 sleep 또는 제한 우회 flag를 사용하지 않는다.
        sql.update("UPDATE announcement_attachment_jobs SET created_at=clock_timestamp()-interval '25 hours' WHERE id=?",first.jobId());
        transaction.executeWithoutResult(status -> intake.saveCollectedSource(request.sourceId(),secondPlan,false));
        var second=service.saveNextJobClaim().orElseThrow();
        assertThat(second.generation()).isEqualTo(2);
        assertThat(second.contentVersionId()).isEqualTo(first.contentVersionId());
        assertThat(second.expectedAttachmentVersion()).isEqualTo(3);
        assertThat(dao.selectSourceContextDetails(request.sourceId()).attachmentReviewRequired()).isFalse();
    }

    @Test void intakeAndPendingGuardRollbackWithTheSourceTransaction() {
        updateFixturePolicyMode("ENFORCE");
        UUID run=insertFixtureRun();
        var request=selectIntakeSource(run);
        var intake=context.getBean(com.saneb.domain.announcementattachment.service.AnnouncementAttachmentIntakeService.class);
        var plan=intake.saveCollectionPlan(run,release);
        var transaction=new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
            intake.saveCollectedSource(request.sourceId(),plan,true);
            throw new IllegalStateException("FIXTURE_ROLLBACK");
        })).hasMessage("FIXTURE_ROLLBACK");
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_attachment_jobs",Integer.class)).isZero();
        var source=dao.selectSourceContextDetails(request.sourceId());
        assertThat(source.attachmentReviewRequired()).isFalse();
        assertThat(source.attachmentVersion()).isZero();
        assertThat(source.attachmentPolicyId()).isNull();
    }

    @Test void att061FrozenRunKeepsPublishedPolicyAfterRetirementAndOffPreventsClaim() {
        UUID run=insertFixtureRun();
        var request=selectIntakeSource(run);
        var intake=context.getBean(com.saneb.domain.announcementattachment.service.AnnouncementAttachmentIntakeService.class);
        var plan=intake.saveCollectionPlan(run,release);
        sql.update("UPDATE announcement_attachment_policies SET policy_status_code='RETIRED',row_version=row_version+1 WHERE id=?",policy);
        insertOffControlPolicy();
        assertThat(intake.saveCollectionPlan(run,release)).isEqualTo(plan);
        var transaction=new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
        transaction.executeWithoutResult(status -> intake.saveCollectedSource(request.sourceId(),plan,true));
        assertThat(sql.queryForObject("SELECT policy_id FROM announcement_attachment_jobs WHERE source_id=?",UUID.class,request.sourceId())).isEqualTo(plan.policyId());
        assertThat(service.saveNextJobClaim()).isEmpty();
        assertThatThrownBy(() -> sql.update("UPDATE announcement_attachment_collection_plans SET plan_status_code='OFF' WHERE run_id=?",run))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test void att001ExcludedAndUnsupportedSourcesNeverCreateExecutableJobs() {
        UUID run=insertFixtureRun();
        var request=selectIntakeSource(run);
        var intake=context.getBean(com.saneb.domain.announcementattachment.service.AnnouncementAttachmentIntakeService.class);
        var plan=intake.saveCollectionPlan(run,release);
        var transaction=new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
        sql.update("UPDATE announcement_source_snapshots SET semantic_status_code='EXCLUDED' WHERE id=?",request.sourceId());
        transaction.executeWithoutResult(status -> intake.saveCollectedSource(request.sourceId(),plan,true));
        assertThat(sql.queryForObject("SELECT attachment_intake_status_code FROM announcement_source_snapshots WHERE id=?",String.class,request.sourceId())).isEqualTo("NOT_REQUESTED");
        sql.update("UPDATE announcement_source_snapshots SET semantic_status_code='REVIEW_REQUIRED',provider_notice_id='unrecognized' WHERE id=?",request.sourceId());
        transaction.executeWithoutResult(status -> intake.saveCollectedSource(request.sourceId(),plan,true));
        assertThat(sql.queryForObject("SELECT attachment_intake_status_code FROM announcement_source_snapshots WHERE id=?",String.class,request.sourceId())).isEqualTo("PROFILE_REQUIRED");
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_attachment_jobs",Integer.class)).isZero();
    }

    @Test void aRunWithoutPolicyCannotAdoptOneMidCollection() {
        sql.update("UPDATE announcement_attachment_policies SET policy_status_code='RETIRED',row_version=row_version+1 WHERE id=?",policy);
        var intake=context.getBean(com.saneb.domain.announcementattachment.service.AnnouncementAttachmentIntakeService.class);
        UUID run=insertFixtureRun();
        var plan=intake.saveCollectionPlan(run,release);
        assertThat(plan.statusCode()).isEqualTo("NO_POLICY");
        insertOffControlPolicy();
        assertThat(intake.saveCollectionPlan(run,release)).isEqualTo(plan);
    }

    @Test void unmatchedEnforceOnRetiredRuleBlocksCollectionBeforePlanOrSourceMutation() {
        UUID oldPolicy=insertRetiredRulePolicy("ENFORCE");
        sql.update("UPDATE announcement_attachment_policies SET policy_status_code='RETIRED',row_version=row_version+1 WHERE id=?",policy);
        var request=selectRequest();
        var before=dao.selectSourceContextDetails(request.sourceId());
        var intake=context.getBean(com.saneb.domain.announcementattachment.service.AnnouncementAttachmentIntakeService.class);
        var intakeDao=context.getBean(SqlSessionTemplate.class).getMapper(com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentIntakeDao.class);
        var transaction=new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
        UUID selectedPolicy=transaction.execute(status -> intakeDao.selectUnmatchedEnforcePolicyId(release));
        assertThat(selectedPolicy).isEqualTo(oldPolicy);
        assertThatThrownBy(() -> intake.saveCollectionPlan(insertFixtureRun(),release))
                .isInstanceOf(ApiException.class).hasMessageContaining("검증·게시");
        assertThatThrownBy(() -> intake.saveCollectionPlan(insertFixtureRun(),null))
                .isInstanceOf(ApiException.class).hasMessageContaining("현재 키워드 규칙");
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_attachment_collection_plans",Integer.class)).isZero();
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_attachment_jobs",Integer.class)).isZero();
        assertThat(dao.selectSourceContextDetails(request.sourceId())).isEqualTo(before);
    }

    @Test void matchingPolicyOverridesOlderEnforceAndExplicitOffRemainsOff() {
        insertRetiredRulePolicy("ENFORCE");
        var intake=context.getBean(com.saneb.domain.announcementattachment.service.AnnouncementAttachmentIntakeService.class);
        var frozen=intake.saveCollectionPlan(insertFixtureRun(),release);
        assertThat(frozen.policyId()).isEqualTo(policy);
        assertThat(frozen.statusCode()).isEqualTo("FROZEN");
        updateFixturePolicyMode("OFF");
        var off=intake.saveCollectionPlan(insertFixtureRun(),release);
        assertThat(off.policyId()).isEqualTo(policy);
        assertThat(off.statusCode()).isEqualTo("OFF");
        assertThat(intake.saveCollectionPlan(frozen.runId(),release)).isEqualTo(frozen);
    }

    @Test void ruleRetiredBetweenRunContextAndPolicySelectionCannotSilentlyDisableEnforce() {
        updateFixturePolicyMode("ENFORCE");
        UUID run=insertFixtureRun();
        var intake=context.getBean(com.saneb.domain.announcementattachment.service.AnnouncementAttachmentIntakeService.class);
        var transaction=new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
        transaction.executeWithoutResult(status -> {
            // 임시 DB transaction을 항상 원복하여 다른 테스트의 ACTIVE seed를 보존한다.
            status.setRollbackOnly();
            sql.update("UPDATE announcement_source_classification_rule_releases SET release_status_code='RETIRED',retired_at=now() WHERE id=?",release);
            assertThatThrownBy(() -> intake.saveCollectionPlan(run,release)).isInstanceOf(ApiException.class).hasMessageContaining("검증·게시");
            assertThat(sql.queryForObject("SELECT count(1) FROM announcement_attachment_collection_plans WHERE run_id=?",Integer.class,run)).isZero();
        });
        assertThat(sql.queryForObject("SELECT release_status_code FROM announcement_source_classification_rule_releases WHERE id=?",String.class,release))
                .isEqualTo("ACTIVE");
    }

    @Test void frozenNoPolicyIsNotOverwrittenAndUnmatchedEnforceDoesNotSilentlyPassOnRetry() {
        insertRetiredRulePolicy("COLLECT_ONLY");
        sql.update("UPDATE announcement_attachment_policies SET policy_status_code='RETIRED',row_version=row_version+1 WHERE id=?",policy);
        var intake=context.getBean(com.saneb.domain.announcementattachment.service.AnnouncementAttachmentIntakeService.class);
        var frozen=intake.saveCollectionPlan(insertFixtureRun(),release);
        assertThat(frozen.statusCode()).isEqualTo("NO_POLICY");
        insertRetiredRulePolicy("ENFORCE");
        assertThatThrownBy(() -> intake.saveCollectionPlan(frozen.runId(),release))
                .isInstanceOf(ApiException.class).hasMessageContaining("현재 키워드 규칙");
        assertThat(sql.queryForObject("SELECT plan_status_code FROM announcement_attachment_collection_plans WHERE run_id=?",String.class,frozen.runId()))
                .isEqualTo("NO_POLICY");
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_attachment_jobs",Integer.class)).isZero();
    }

    private UUID insertRetiredRulePolicy(String mode) {
        // 이 클래스 전용 임시 DB의 과거 규칙/정책 fixture다. ACTIVE 키워드 규칙은 변경하지 않는다.
        UUID retiredRelease=UUID.randomUUID(),oldPolicy=UUID.randomUUID();
        sql.update("""
            INSERT INTO announcement_source_classification_rule_releases
            (id,release_code,version_no,release_status_code,rule_snapshot_hash,activated_at,retired_at)
            SELECT ?,?,coalesce(max(version_no),0)+1,'RETIRED',repeat('f',64),now(),now()
            FROM announcement_source_classification_rule_releases
            """,retiredRelease,retiredRelease.toString());
        sql.update("""
            INSERT INTO announcement_attachment_policies
            (id,policy_code,version_no,policy_status_code,mode_code,rule_release_id,policy_hash,settings_json,profile_manifest_json,created_by,published_at)
            SELECT ?,?,1,'ACTIVE',?,?,repeat('e',64),settings_json,profile_manifest_json,created_by,now()
            FROM announcement_attachment_policies WHERE id=?
            """,oldPolicy,oldPolicy.toString(),mode,retiredRelease,policy);
        return oldPolicy;
    }

    @Test void existingSourceCannotBeLabeledNewByADifferentCollectionRun() {
        updateFixturePolicyMode("ENFORCE");
        var request=selectIntakeSource(insertFixtureRun());
        var intake=context.getBean(com.saneb.domain.announcementattachment.service.AnnouncementAttachmentIntakeService.class);
        var plan=intake.saveCollectionPlan(insertFixtureRun(),release);
        var transaction=new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> intake.saveCollectedSource(request.sourceId(),plan,true)))
                .isInstanceOf(ApiException.class).hasMessageContaining("새로 저장한");
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_attachment_jobs",Integer.class)).isZero();
        assertThat(dao.selectSourceContextDetails(request.sourceId()).attachmentReviewRequired()).isFalse();
    }

    private UUID insertFixtureRun() {
        UUID request=UUID.randomUUID(),run=UUID.randomUUID();
        sql.update("INSERT INTO announcement_source_collection_requests(id,provider_code,request_type_code,request_status_code) VALUES (?,'BIZINFO','MANUAL','APPROVED')",request);
        sql.update("INSERT INTO announcement_source_collection_runs(id,request_id,run_status_code) VALUES (?,?,'RUNNING')",run,request);
        return run;
    }
    private AttachmentJobReservation selectIntakeSource(UUID run) {
        var request=selectRequest();
        String notice="PBLN_"+String.format("%015d",Integer.toUnsignedLong(request.sourceId().hashCode()));
        sql.update("UPDATE announcement_source_snapshots SET provider_notice_id=? WHERE id=?",notice,request.sourceId());
        sql.update("UPDATE announcement_source_classification_evaluations SET run_id=? WHERE id=?",run,request.expectedBaseDecisionId());
        return request;
    }
    private void updateFixturePolicyMode(String mode) {
        UUID replacement=UUID.randomUUID();
        sql.update("UPDATE announcement_attachment_policies SET policy_status_code='RETIRED',row_version=row_version+1 WHERE id=?",policy);
        sql.update("""
            INSERT INTO announcement_attachment_policies
            (id,policy_code,version_no,policy_status_code,mode_code,rule_release_id,policy_hash,settings_json,profile_manifest_json,created_by,published_at)
            SELECT ?,?,1,'ACTIVE',?,rule_release_id,repeat('e',64),settings_json,profile_manifest_json,created_by,now()
            FROM announcement_attachment_policies WHERE id=?
            """,replacement,replacement.toString(),mode,policy);
        policy=replacement;
    }

    private AnnouncementAttachmentReviewService reviewService() { return context.getBean(AnnouncementAttachmentReviewService.class); }
    private org.springframework.security.core.Authentication reviewActor() {
        var principal=new com.saneb.domain.auth.vo.AuthenticatedUserDetails(new com.saneb.domain.auth.vo.AuthUserDetailsRow(
                actor,"attachment-job-fixture","unused","첨부 작업 검증","ACTIVE",false,null,null,null),List.of("ADMIN"));
        return org.springframework.security.authentication.UsernamePasswordAuthenticationToken.authenticated(principal,null,principal.getAuthorities());
    }
    private UUID insertReviewFixture(boolean failed) {
        updateFixturePolicyMode("ENFORCE");
        var request=selectRequest();
        // 공고 UNIQUE 검증과 다른 테스트의 DRAFT 이력을 구분하는 격리 fixture 제목이다.
        sql.update("UPDATE announcement_source_snapshots SET title=?,is_attachment_review_required=true,attachment_policy_id=? WHERE id=?",
                "첨부 검수 QA "+request.sourceId(),policy,request.sourceId());
        service.insertAttachmentJob(request);
        var job=service.saveNextJobClaim().orElseThrow();
        if (!failed) service.saveDownloadBytes(job.jobId(),job.leaseToken(),15);
        evidenceService.saveAttachmentSet(job.jobId(),job.leaseToken(),failed ? new AttachmentSetEvidence("FAILED",false,List.of())
                : new AttachmentSetEvidence("FOUND",true,List.of(selectFileEvidence(100)))).orElseThrow();
        context.getBean(AnnouncementAttachmentEvaluationService.class).saveJobEvaluation(job.jobId(),job.leaseToken()).orElseThrow();
        return request.sourceId();
    }
    private AttachmentReviewRequests.Confirmation selectReviewRequest(UUID source) {
        var state=reviewService().selectReviewContextDetails(source);
        return new AttachmentReviewRequests.Confirmation(state.version(),List.of("PERSONAL","BUSINESS"),List.of("POLICY_FINANCE"),
                state.manualSourceCheckRequired()?"MANUAL_SOURCE_CHECK":"EXTRACTED_TEXT",state.requiredAcknowledgementCodes(),"격리 DB 전체 원문 확인 QA");
    }
    @Test void reviewConfirmationAndDraftAreVersionBoundImmutableAndNeverAutoActivated() {
        UUID source=insertReviewFixture(false);
        var request=selectReviewRequest(source); UUID key=UUID.randomUUID();
        var confirmed=reviewService().insertConfirmation(reviewActor(),source,key,request);
        assertThat(confirmed.attachmentVersion()).isEqualTo(request.version().expectedAttachmentVersion()+1);
        assertThat(reviewService().insertConfirmation(reviewActor(),source,key,request)).isEqualTo(confirmed);
        var reader=context.getBean(com.saneb.domain.announcementattachment.service.AnnouncementAttachmentCurrentService.class);
        var visible=reader.selectClassificationDetails(source);
        assertThat(visible.confirmationId()).isEqualTo(confirmed.confirmationId());
        assertThat(visible.effectiveClassification().targetCategoryCodes()).containsExactly("BUSINESS","PERSONAL");
        var restored=reviewService().selectReviewContextDetails(source);
        assertThat(restored.confirmedClassification().confirmation().confirmationId()).isEqualTo(confirmed.confirmationId());
        assertThat(restored.confirmedClassification().targetCategoryCodes()).containsExactly("BUSINESS","PERSONAL");
        assertThat(restored.confirmedClassification().supportTypeCodes()).containsExactly("POLICY_FINANCE");
        assertThatThrownBy(()->sql.update("UPDATE announcement_source_attachment_confirmations SET review_note='변조' WHERE id=?",confirmed.confirmationId()))
                .isInstanceOf(DataIntegrityViolationException.class);
        var conversion=new AttachmentReviewRequests.Conversion(reviewService().selectReviewContextDetails(source).version(),confirmed.confirmationId(),"BUSINESS",null);
        var draft=reviewService().insertOperationalAnnouncement(reviewActor(),source,conversion);
        assertThat(reviewService().insertOperationalAnnouncement(reviewActor(),source,conversion)).isEqualTo(draft);
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_source_links WHERE source_id=?",Integer.class,source)).isEqualTo(1);
        assertThat(sql.queryForObject("SELECT approval_status_code FROM announcements WHERE id=?",String.class,draft.announcementId())).isEqualTo("DRAFT");
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_approval_requests WHERE announcement_id=?",Integer.class,draft.announcementId())).isZero();
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_target_category_assignments WHERE announcement_id=?",Integer.class,draft.announcementId())).isEqualTo(2);
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_support_type_assignments WHERE announcement_id=?",Integer.class,draft.announcementId())).isEqualTo(1);
        assertThat(reader.selectClassificationDetails(source).confirmationStatusCode()).isEqualTo("CURRENT");
        var linkedContext=reviewService().selectReviewContextDetails(source);
        assertThat(linkedContext.confirmedClassification()).isNull();
        assertThat(linkedContext.linkedAnnouncement().announcementId()).isEqualTo(draft.announcementId());
        assertThat(sql.queryForObject("SELECT classification_row_version FROM announcement_source_snapshots WHERE id=?",Integer.class,source)).isZero();
        assertThat(sql.queryForObject("SELECT is_attachment_review_required FROM announcement_source_snapshots WHERE id=?",Boolean.class,source)).isTrue();
        assertThatThrownBy(()->sql.update("UPDATE announcement_source_links SET attachment_request_hash=repeat('b',64) WHERE source_id=?",source))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
    @Test void concurrentConfirmationsAllowOneWriterAndReturnConflictForStaleVersion() throws Exception {
        UUID source=insertReviewFixture(false); var request=selectReviewRequest(source);
        var gate=new CountDownLatch(1);
        Callable<Integer> write=()-> { gate.await(5,TimeUnit.SECONDS);
            try { reviewService().insertConfirmation(reviewActor(),source,UUID.randomUUID(),request); return 200; }
            catch(ApiException exception) { return exception.httpStatus().value(); } };
        try(var executor=Executors.newFixedThreadPool(2)) {
            var first=executor.submit(write); var second=executor.submit(write); gate.countDown();
            assertThat(List.of(first.get(15,TimeUnit.SECONDS),second.get(15,TimeUnit.SECONDS))).containsExactlyInAnyOrder(200,409);
        }
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_source_attachment_confirmations WHERE source_id=?",Integer.class,source)).isEqualTo(1);
    }
    @Test void concurrentSameDraftRequestCreatesOneAnnouncement() throws Exception {
        UUID source=insertReviewFixture(false);
        var confirmed=reviewService().insertConfirmation(reviewActor(),source,UUID.randomUUID(),selectReviewRequest(source));
        var conversion=new AttachmentReviewRequests.Conversion(reviewService().selectReviewContextDetails(source).version(),confirmed.confirmationId(),"BUSINESS",null);
        var gate=new CountDownLatch(1);
        Callable<UUID> write=()->{gate.await(5,TimeUnit.SECONDS); return reviewService().insertOperationalAnnouncement(reviewActor(),source,conversion).announcementId();};
        try(var executor=Executors.newFixedThreadPool(2)) {
            var first=executor.submit(write); var second=executor.submit(write); gate.countDown();
            assertThat(first.get(15,TimeUnit.SECONDS)).isEqualTo(second.get(15,TimeUnit.SECONDS));
        }
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_source_links WHERE source_id=?",Integer.class,source)).isEqualTo(1);
    }
    @Test void newAttachmentReservationStalesConfirmationAndPreventsOldDraftRequest() {
        UUID source=insertReviewFixture(false);
        var confirmed=reviewService().insertConfirmation(reviewActor(),source,UUID.randomUUID(),selectReviewRequest(source));
        var version=reviewService().selectReviewContextDetails(source).version();
        var conversion=new AttachmentReviewRequests.Conversion(version,confirmed.confirmationId(),"BUSINESS",null);
        service.insertAttachmentJob(new AttachmentJobReservation(source,policy,version.expectedBaseDecisionId(),
                version.expectedSourceVersion(),version.expectedAttachmentVersion(),UUID.randomUUID(),EXECUTION));
        assertThat(sql.queryForObject("SELECT is_current FROM announcement_source_attachment_confirmations WHERE id=?",Boolean.class,confirmed.confirmationId())).isFalse();
        assertThatThrownBy(()->reviewService().insertOperationalAnnouncement(reviewActor(),source,conversion)).isInstanceOf(ApiException.class);
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_source_links WHERE source_id=?",Integer.class,source)).isZero();
    }
    @Test void failedDiscoveryCanOnlyBeManuallyConfirmedWithoutChangingFailureOrAutoDecision() {
        UUID source=insertReviewFixture(true); var request=selectReviewRequest(source);
        assertThat(request.reviewMethodCode()).isEqualTo("MANUAL_SOURCE_CHECK");
        var invalid=new AttachmentReviewRequests.Confirmation(request.version(),request.targetCategoryCodes(),request.supportTypeCodes(),"EXTRACTED_TEXT",
                request.acknowledgedErrorCodes(),request.reviewNote());
        assertThatThrownBy(()->reviewService().insertConfirmation(reviewActor(),source,UUID.randomUUID(),invalid)).isInstanceOf(ApiException.class);
        var confirmed=reviewService().insertConfirmation(reviewActor(),source,UUID.randomUUID(),request);
        assertThat(confirmed.isCurrent()).isTrue();
        assertThat(sql.queryForObject("SELECT decision_status_code FROM announcement_source_attachment_evaluations WHERE id=?",String.class,confirmed.evaluationId())).isEqualTo("REVIEW_REQUIRED");
        assertThat(sql.queryForObject("SELECT discovery_status_code FROM announcement_source_attachment_sets WHERE source_id=?",String.class,source)).isEqualTo("FAILED");
    }
    @Test void crossSourceIdempotencyKeyRaceRollsBackLosingConfirmationVersionAndTags() throws Exception {
        UUID firstSource=insertReviewFixture(false),secondSource=insertReviewFixture(false),sharedKey=UUID.randomUUID();
        var firstRequest=selectReviewRequest(firstSource); var secondRequest=selectReviewRequest(secondSource);
        var gate=new CountDownLatch(1);
        java.util.function.BiFunction<UUID,AttachmentReviewRequests.Confirmation,Integer> write=(source,request)->{
            try { gate.await(5,TimeUnit.SECONDS); reviewService().insertConfirmation(reviewActor(),source,sharedKey,request); return 200; }
            catch(ApiException exception) { return exception.httpStatus().value(); }
            catch(InterruptedException exception) { Thread.currentThread().interrupt(); throw new IllegalStateException(exception); }
        };
        try(var executor=Executors.newFixedThreadPool(2)) {
            var first=executor.submit(()->write.apply(firstSource,firstRequest)); var second=executor.submit(()->write.apply(secondSource,secondRequest));
            gate.countDown();
            assertThat(List.of(first.get(15,TimeUnit.SECONDS),second.get(15,TimeUnit.SECONDS))).containsExactlyInAnyOrder(200,409);
        }
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_source_attachment_confirmations WHERE idempotency_key=?",Integer.class,sharedKey)).isEqualTo(1);
        assertThat(sql.queryForList("SELECT attachment_row_version FROM announcement_source_snapshots ORDER BY attachment_row_version",Integer.class)).containsExactly(2,3);
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_source_attachment_tags WHERE origin_code='CONFIRMED'",Integer.class)).isEqualTo(3);
    }

    private AnnouncementAttachmentRoleService roleService() { return context.getBean(AnnouncementAttachmentRoleService.class); }
    private AttachmentRoleRequest selectRoleRequest(UUID source,String role) {
        var version=reviewService().selectReviewContextDetails(source).version();
        var current=context.getBean(com.saneb.domain.announcementattachment.service.AnnouncementAttachmentCurrentService.class).selectClassificationDetails(source);
        var setId=current.effectiveClassification().setId();
        var files=evidenceDao.selectFileList(source,setId,0,10);
        return new AttachmentRoleRequest(version,setId,files.stream().map(file->new AttachmentRoleRequest.FileRole(file.fileId(),role)).toList(),"격리 DB 문서 역할 검수 QA");
    }
    @Test void roleChangeCreatesNewGenerationStalesConfirmationAndPreservesExtractionEvidence() {
        UUID source=insertReviewFixture(false);
        var confirmed=reviewService().insertConfirmation(reviewActor(),source,UUID.randomUUID(),selectReviewRequest(source));
        var request=selectRoleRequest(source,"REFERENCE"); var original=evidenceDao.selectFileList(source,request.expectedSetId(),0,10).getFirst();
        var reserved=roleService().insertRoleChange(reviewActor(),source,UUID.randomUUID(),request);
        assertThat(reserved.operationCode()).isEqualTo("ROLE_CHANGE"); assertThat(reserved.setId()).isNotEqualTo(request.expectedSetId());
        assertThat(sql.queryForObject("SELECT is_current FROM announcement_source_attachment_confirmations WHERE id=?",Boolean.class,confirmed.confirmationId())).isFalse();
        var reader=context.getBean(com.saneb.domain.announcementattachment.service.AnnouncementAttachmentCurrentService.class);
        assertThat(reader.selectClassificationDetails(source).effectiveClassification().decisionId()).isNull();
        var copied=evidenceDao.selectFileList(source,reserved.setId(),0,10).getFirst();
        assertThat(copied.fileId()).isNotEqualTo(original.fileId()); assertThat(copied.documentRoleCode()).isEqualTo("REFERENCE");
        assertThat(copied.roleOriginCode()).isEqualTo("MANUAL"); assertThat(copied.reusedFromExtractionId()).isEqualTo(original.extractionId());
        assertThat(copied.extractedAt()).isEqualTo(original.extractedAt()); assertThat(copied.qualityCode()).isEqualTo(original.qualityCode());
        assertThat(copied.binaryHash()).isEqualTo(original.binaryHash());
        assertThat(evidenceDao.selectFileList(source,request.expectedSetId(),0,10).getFirst().documentRoleCode()).isEqualTo("NOTICE");
        var claimed=service.saveNextJobClaim().orElseThrow(); assertThat(claimed.jobId()).isEqualTo(reserved.jobId());
        assertThat(service.selectExternalExecutionAllowed(claimed.jobId(),claimed.leaseToken())).isFalse();
        assertThat(service.saveDownloadBytes(claimed.jobId(),claimed.leaseToken(),1)).isFalse();
        context.getBean(AnnouncementAttachmentEvaluationService.class).saveJobEvaluation(claimed.jobId(),claimed.leaseToken()).orElseThrow();
        var result=reader.selectClassificationDetails(source);
        assertThat(result.confirmationStatusCode()).isEqualTo("STALE"); assertThat(result.confirmationId()).isNull();
        assertThat(result.effectiveClassification().setId()).isEqualTo(reserved.setId());
        assertThat(result.effectiveClassification().semanticStatusCode()).isEqualTo("REVIEW_REQUIRED");
        assertThat(sql.queryForObject("SELECT classification_row_version FROM announcement_source_snapshots WHERE id=?",Integer.class,source)).isZero();
        assertThat(dao.selectJobDetails(claimed.jobId()).reservedDownloadBytes()).isZero();
    }
    @Test void offDoesNotPreventRoleReevaluationOfFrozenSealedEvidenceOrClearGuard() {
        UUID source=insertReviewFixture(false); var request=selectRoleRequest(source,"GUIDE");
        UUID boundPolicy=policy; updateFixturePolicyMode("OFF");
        var reserved=roleService().insertRoleChange(reviewActor(),source,UUID.randomUUID(),request);
        assertThat(dao.selectJobDetails(reserved.jobId()).policyId()).isEqualTo(boundPolicy);
        var claimed=service.saveNextJobClaim().orElseThrow();
        assertThat(claimed.jobId()).isEqualTo(reserved.jobId()); assertThat(service.selectExternalExecutionAllowed(claimed.jobId(),claimed.leaseToken())).isFalse();
        assertThat(context.getBean(AnnouncementAttachmentEvaluationService.class).saveJobEvaluation(claimed.jobId(),claimed.leaseToken())).isPresent();
        assertThat(sql.queryForObject("SELECT is_attachment_review_required FROM announcement_source_snapshots WHERE id=?",Boolean.class,source)).isTrue();
        assertThat(sql.queryForObject("SELECT attachment_policy_id FROM announcement_source_snapshots WHERE id=?",UUID.class,source)).isEqualTo(boundPolicy);
    }
    @Test void concurrentSameRoleRequestCreatesExactlyOneCopiedSetAndJob() throws Exception {
        UUID source=insertReviewFixture(false),key=UUID.randomUUID(); var request=selectRoleRequest(source,"GUIDE"); var gate=new CountDownLatch(1);
        Callable<UUID> change=()->{gate.await(5,TimeUnit.SECONDS);return roleService().insertRoleChange(reviewActor(),source,key,request).jobId();};
        try(var executor=Executors.newFixedThreadPool(2)) {
            var first=executor.submit(change); var second=executor.submit(change); gate.countDown();
            assertThat(first.get(15,TimeUnit.SECONDS)).isEqualTo(second.get(15,TimeUnit.SECONDS));
        }
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_source_attachment_sets WHERE source_id=?",Integer.class,source)).isEqualTo(2);
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_attachment_jobs WHERE source_id=? AND operation_code='ROLE_CHANGE'",Integer.class,source)).isEqualTo(1);
    }
    @Test void reusedExtractionCannotCrossSourceOrChangeOriginalText() {
        UUID firstSource=insertReviewFixture(false),secondSource=insertReviewFixture(false);
        UUID firstExtraction=sql.queryForObject("SELECT id FROM announcement_source_attachment_extractions WHERE source_id=?",UUID.class,firstSource);
        UUID secondExtraction=sql.queryForObject("SELECT id FROM announcement_source_attachment_extractions WHERE source_id=?",UUID.class,secondSource);
        var source=dao.selectSourceContextDetails(secondSource); UUID copySet=UUID.randomUUID(),copyFile=UUID.randomUUID();
        sql.update("INSERT INTO announcement_source_attachment_sets(id,source_id,content_version_id,policy_id,data_purpose_code,profile_hash) VALUES (?,?,?,?,'PRODUCTION',?)",
                copySet,secondSource,source.contentVersionId(),policy,PROFILE_HASH);
        sql.update("INSERT INTO announcement_source_attachment_files(id,set_id,source_id,stable_locator_hash,safe_locator_json,download_status_code,downloaded_bytes,binary_hash,sort_order) VALUES (?,?,?,repeat('a',64),'{}','SUCCEEDED',15,repeat('a',64),0)",copyFile,copySet,secondSource);
        String reuseSql="""
            INSERT INTO announcement_source_attachment_extractions(id,file_id,set_id,source_id,attempt_no,extractor_code,extractor_version,extractor_config_hash,
                quality_code,extracted_text,text_hash,blocks_json,character_count,page_count,duration_ms,error_code,created_at,reused_from_extraction_id)
            SELECT ?,?,?,?,original.attempt_no,original.extractor_code,original.extractor_version,original.extractor_config_hash,
                original.quality_code,CASE WHEN ? THEN '변조된 원문' ELSE original.extracted_text END,original.text_hash,original.blocks_json,
                original.character_count,original.page_count,original.duration_ms,original.error_code,original.created_at,original.id
            FROM announcement_source_attachment_extractions original WHERE original.id=?
            """;
        assertThatThrownBy(()->sql.update(reuseSql,UUID.randomUUID(),copyFile,copySet,secondSource,false,firstExtraction)).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(()->sql.update(reuseSql,UUID.randomUUID(),copyFile,copySet,secondSource,true,secondExtraction)).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(sql.update(reuseSql,UUID.randomUUID(),copyFile,copySet,secondSource,false,secondExtraction)).isEqualTo(1);
    }

    private com.saneb.domain.announcementattachment.service.AnnouncementAttachmentRetryService retryService() {
        return context.getBean(com.saneb.domain.announcementattachment.service.AnnouncementAttachmentRetryService.class);
    }
    private record RetryFixture(UUID source,UUID set,UUID failedFile,com.saneb.domain.announcementattachment.vo.AttachmentFileSummaryRow successfulFile) { }
    private RetryFixture insertRetryFixture() {
        updateFixturePolicyMode("ENFORCE");var request=selectRequest();
        sql.update("UPDATE announcement_source_snapshots SET is_attachment_review_required=true,attachment_policy_id=? WHERE id=?",policy,request.sourceId());
        service.insertAttachmentJob(request);var job=service.saveNextJobClaim().orElseThrow();service.saveDownloadBytes(job.jobId(),job.leaseToken(),15);
        var failed=new AttachmentSetEvidence.File(new AttachmentSetEvidence.Locator(EXECUTION.profileCode(),"/download/file",java.util.Map.of("fileId","retry-2")),
                "실패 공고문.hwpx",null,"NOTICE","PROFILE","FAILED",0,null,AttachmentFailureCode.NETWORK_TIMEOUT,null);
        var set=evidenceService.saveAttachmentSet(job.jobId(),job.leaseToken(),new AttachmentSetEvidence("FOUND",true,List.of(selectFileEvidence(100),failed))).orElseThrow();
        context.getBean(AnnouncementAttachmentEvaluationService.class).saveJobEvaluation(job.jobId(),job.leaseToken()).orElseThrow();
        var files=evidenceDao.selectFileList(job.sourceId(),set.setId(),0,10);
        return new RetryFixture(job.sourceId(),set.setId(),files.get(1).fileId(),files.getFirst());
    }
    private com.saneb.domain.announcementattachment.dto.AttachmentRetryRequest selectRetryRequest(RetryFixture fixture) {
        return new com.saneb.domain.announcementattachment.dto.AttachmentRetryRequest(reviewService().selectReviewContextDetails(fixture.source()).version(),
                fixture.set(),List.of(fixture.failedFile()),8L,"격리 DB 실패 첨부 재시도 QA");
    }
    @Test void manualRetryPersistsOnlySelectedNewEvidenceAndReusesSuccessWithOriginalTime() throws Exception {
        var fixture=insertRetryFixture();var confirmation=reviewService().insertConfirmation(reviewActor(),fixture.source(),UUID.randomUUID(),selectReviewRequest(fixture.source()));
        var request=selectRetryRequest(fixture);var reserved=retryService().insertFileRetry(reviewActor(),fixture.source(),UUID.randomUUID(),request);
        assertThat(reserved.setId()).isNull();assertThat(reserved.operationCode()).isEqualTo("RETRY_FILES");
        assertThat(sql.queryForObject("SELECT is_current FROM announcement_source_attachment_confirmations WHERE id=?",Boolean.class,confirmation.confirmationId())).isFalse();
        var claimed=service.saveNextJobClaim().orElseThrow();assertThat(claimed.jobId()).isEqualTo(reserved.jobId());
        assertThat(service.saveDownloadBytes(claimed.jobId(),claimed.leaseToken(),8)).isTrue();
        var plan=retryService().selectRetryFileList(claimed.jobId(),claimed.leaseToken());var selected=plan.stream().filter(f->f.selected()).findFirst().orElseThrow();
        var success=selectFileEvidence(100);var fresh=new AttachmentSetEvidence.File(new ObjectMapper().readValue(selected.locatorJson(),AttachmentSetEvidence.Locator.class),
                selected.displayName(),"HWPX",selected.role(),selected.roleOrigin(),"SUCCEEDED",8,"c".repeat(64),null,success.extraction());
        var result=evidenceService.saveRetriedAttachmentSet(claimed.jobId(),claimed.leaseToken(),new AttachmentSetEvidence("FOUND",true,List.of(fresh))).orElseThrow();
        var files=evidenceDao.selectFileList(fixture.source(),result.setId(),0,10);assertThat(files).hasSize(2);
        assertThat(files.getFirst().reusedFromExtractionId()).isEqualTo(fixture.successfulFile().extractionId());
        assertThat(files.getFirst().extractedAt()).isEqualTo(fixture.successfulFile().extractedAt());
        assertThat(files.get(1).reusedFromExtractionId()).isNull();assertThat(files.get(1).binaryHash()).isEqualTo("c".repeat(64));
        assertThat(dao.selectJobDetails(claimed.jobId()).reservedDownloadBytes()).isEqualTo(8);
        context.getBean(AnnouncementAttachmentEvaluationService.class).saveJobEvaluation(claimed.jobId(),claimed.leaseToken()).orElseThrow();
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_source_links WHERE source_id=?",Integer.class,fixture.source())).isZero();
        assertThat(sql.queryForObject("SELECT is_attachment_review_required FROM announcement_source_snapshots WHERE id=?",Boolean.class,fixture.source())).isTrue();
        assertThat(evidenceDao.selectFileList(fixture.source(),fixture.set(),0,10).get(1).downloadStatusCode()).isEqualTo("FAILED");
        sql.update("DELETE FROM announcement_source_snapshots WHERE id=?",fixture.source());
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_attachment_retry_files WHERE source_id=?",Integer.class,fixture.source())).isZero();
    }
    @Test void simultaneousManualRetryKeyCreatesOneScopeAndNoExtraGeneration() throws Exception {
        var fixture=insertRetryFixture();var request=selectRetryRequest(fixture);UUID key=UUID.randomUUID();var start=new CountDownLatch(1);
        Callable<UUID> call=()->{start.await(5,TimeUnit.SECONDS);return retryService().insertFileRetry(reviewActor(),fixture.source(),key,request).jobId();};
        try(var executor=Executors.newFixedThreadPool(2)) {var first=executor.submit(call);var second=executor.submit(call);start.countDown();
            assertThat(first.get(15,TimeUnit.SECONDS)).isEqualTo(second.get(15,TimeUnit.SECONDS));}
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_attachment_jobs WHERE source_id=? AND operation_code='RETRY_FILES'",Integer.class,fixture.source())).isEqualTo(1);
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_attachment_retry_files WHERE source_id=?",Integer.class,fixture.source())).isEqualTo(1);
        assertThat(context.getBean(SqlSessionTemplate.class).getMapper(com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentRetryDao.class).selectRetryRateAllowed(fixture.source())).isFalse();
    }
    @Test void manualRetryScopeCannotAddSuccessfulFilesOrMutateAfterReservation() {
        var fixture=insertRetryFixture();var request=selectRetryRequest(fixture);var reserved=retryService().insertFileRetry(reviewActor(),fixture.source(),UUID.randomUUID(),request);
        assertThatThrownBy(()->sql.update("INSERT INTO announcement_attachment_retry_files(job_id,source_id,reference_set_id,file_id) VALUES (?,?,?,?)",
                reserved.jobId(),fixture.source(),fixture.set(),fixture.successfulFile().fileId())).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(()->sql.update("UPDATE announcement_attachment_retry_files SET file_id=? WHERE job_id=?",fixture.successfulFile().fileId(),reserved.jobId()))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(()->sql.update("DELETE FROM announcement_attachment_retry_files WHERE job_id=?",reserved.jobId())).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_attachment_retry_files WHERE job_id=?",Integer.class,reserved.jobId())).isEqualTo(1);
    }

    private com.saneb.domain.announcementattachment.service.AnnouncementAttachmentHistoryService historyService() {
        return context.getBean(com.saneb.domain.announcementattachment.service.AnnouncementAttachmentHistoryService.class);
    }
    @Test void historyReadsFailedInputsAndFrozenKeywordCoordinatesWithoutChangingVersions() {
        var fixture=insertRetryFixture();var original=dao.selectSourceContextDetails(fixture.source());
        UUID evaluation=original.currentAttachmentEvaluationId();
        var details=historyService().selectEvaluationDetails(fixture.source(),evaluation);
        assertThat(details.evaluation().usageCode()).isEqualTo("CURRENT_EFFECTIVE");
        assertThat(details.inputCount()).isEqualTo(2);assertThat(details.autoTargetCategoryCodes()).contains("BUSINESS");
        var inputs=historyService().selectInputList(fixture.source(),evaluation,1,20);
        assertThat(inputs.items()).hasSize(2);assertThat(inputs.totalCount()).isEqualTo(2);
        assertThat(inputs.items().get(1).fileId()).isEqualTo(fixture.failedFile());
        assertThat(inputs.items().get(1).extractionId()).isNull();assertThat(inputs.items().get(1).downloadStatusCode()).isEqualTo("FAILED");
        var matches=historyService().selectMatchList(fixture.source(),evaluation,fixture.successfulFile().fileId(),1,100);
        assertThat(matches.items()).isNotEmpty();assertThat(matches.totalCount()).isEqualTo(matches.items().size());
        assertThat(matches.items()).allSatisfy(match->{
            assertThat(match.extractionId()).isEqualTo(fixture.successfulFile().extractionId());
            assertThat(match.blockIndex()).isZero();assertThat(match.endOffset()).isGreaterThan(match.startOffset());
            assertThat(match.termText()).isNotBlank();
        });
        assertThat(historyService().selectMatchList(fixture.source(),evaluation,fixture.failedFile(),1,20).totalCount()).isZero();
        var after=dao.selectSourceContextDetails(fixture.source());assertThat(after.attachmentVersion()).isEqualTo(original.attachmentVersion());
        assertThat(after.currentAttachmentEvaluationId()).isEqualTo(evaluation);
    }
    @Test void historicalInputsStayFrozenAfterNewGenerationAndCrossSourceIdsAreRejected() {
        var fixture=insertRetryFixture();var original=dao.selectSourceContextDetails(fixture.source());UUID old=original.currentAttachmentEvaluationId();
        var before=historyService().selectInputList(fixture.source(),old,1,20);
        service.insertAttachmentJob(new AttachmentJobReservation(fixture.source(),policy,original.baseEvaluationId(),original.sourceVersion(),
                original.attachmentVersion(),UUID.randomUUID(),EXECUTION));
        assertThat(historyService().selectEvaluationDetails(fixture.source(),old).evaluation().usageCode()).isEqualTo("NOT_CURRENT");
        var job=service.saveNextJobClaim().orElseThrow();
        evidenceService.saveAttachmentSet(job.jobId(),job.leaseToken(),new AttachmentSetEvidence("NO_FILES",true,List.of()));
        var fresh=context.getBean(AnnouncementAttachmentEvaluationService.class).saveJobEvaluation(job.jobId(),job.leaseToken()).orElseThrow();
        assertThat(historyService().selectEvaluationList(fixture.source(),1,1).totalCount()).isEqualTo(2);
        assertThat(historyService().selectEvaluationDetails(fixture.source(),fresh.evaluationId()).evaluation().usageCode()).isEqualTo("CURRENT_EFFECTIVE");
        assertThat(historyService().selectInputList(fixture.source(),old,1,20)).isEqualTo(before);
        assertThat(historyService().selectInputList(fixture.source(),fresh.evaluationId(),1,20).totalCount()).isZero();
        assertThatThrownBy(()->historyService().selectMatchList(fixture.source(),fresh.evaluationId(),fixture.successfulFile().fileId(),1,20))
                .isInstanceOf(ApiException.class);
        var other=selectRequest();
        assertThatThrownBy(()->historyService().selectEvaluationDetails(other.sourceId(),old)).isInstanceOf(ApiException.class);
        assertThatThrownBy(()->historyService().selectInputList(other.sourceId(),old,1,20)).isInstanceOf(ApiException.class);
        assertThatThrownBy(()->historyService().selectMatchList(other.sourceId(),old,null,1,20)).isInstanceOf(ApiException.class);
    }
    @Test void collectOnlyHistoryIsPreviewAndHiddenSourceCannotExposeStoredEvidence() {
        var request=selectRequest();service.insertAttachmentJob(request);var job=service.saveNextJobClaim().orElseThrow();
        service.saveDownloadBytes(job.jobId(),job.leaseToken(),15);
        evidenceService.saveAttachmentSet(job.jobId(),job.leaseToken(),new AttachmentSetEvidence("FOUND",true,List.of(selectFileEvidence(100))));
        var result=context.getBean(AnnouncementAttachmentEvaluationService.class).saveJobEvaluation(job.jobId(),job.leaseToken()).orElseThrow();
        assertThat(historyService().selectEvaluationDetails(request.sourceId(),result.evaluationId()).evaluation().usageCode()).isEqualTo("CURRENT_PREVIEW");
        sql.update("UPDATE announcement_source_snapshots SET semantic_status_code='EXCLUDED' WHERE id=?",request.sourceId());
        assertThatThrownBy(()->historyService().selectEvaluationList(request.sourceId(),1,20)).isInstanceOf(ApiException.class);
        assertThatThrownBy(()->historyService().selectEvaluationDetails(request.sourceId(),result.evaluationId())).isInstanceOf(ApiException.class);
        var rawReader=context.getBean(AnnouncementAttachmentReadService.class);
        var file=evidenceDao.selectFileList(request.sourceId(),result.setId(),0,10).getFirst();
        assertThatThrownBy(()->rawReader.selectAttachmentSetList(request.sourceId(),1,20)).isInstanceOf(ApiException.class);
        assertThatThrownBy(()->rawReader.selectAttachmentFileList(request.sourceId(),result.setId(),1,20)).isInstanceOf(ApiException.class);
        assertThatThrownBy(()->rawReader.selectAttachmentBlockList(request.sourceId(),file.extractionId(),1,10,0,2000)).isInstanceOf(ApiException.class);
    }

    private com.saneb.domain.announcementattachment.service.AnnouncementAttachmentCollectionService collectionService() {
        return context.getBean(com.saneb.domain.announcementattachment.service.AnnouncementAttachmentCollectionService.class);
    }
    private long collectionLocatorSequence;
    private void insertCollectionLocator(UUID source) {
        // 이 클래스의 임시 DB 식별자다. 실제 공식 서버 요청은 하지 않는다.
        String noticeId=String.format(java.util.Locale.ROOT,"PBLN_%015d",++collectionLocatorSequence);
        sql.update("UPDATE announcement_source_snapshots SET provider_notice_id=? WHERE id=?",noticeId,source);
    }
    private com.saneb.domain.announcementattachment.dto.AttachmentCollectionRequests.Request selectCollectionRequest(UUID source) {
        var context=collectionService().selectCollectionContextDetails(reviewActor(),source);
        return new com.saneb.domain.announcementattachment.dto.AttachmentCollectionRequests.Request(context.version(),context.policyId(),context.policyHash(),context.executionHash(),
                context.maximumDownloadBytes(),"격리 DB 전체 첨부 수집 QA");
    }
    @Test void manualInitialCollectionContextIsReadOnlyAndUnboundEnforceStaysPreview() {
        updateFixturePolicyMode("ENFORCE");var source=selectRequest().sourceId();insertCollectionLocator(source);
        var request=selectCollectionRequest(source);
        assertThat(request.version().expectedAttachmentDecisionId()).isNull();
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_attachment_jobs WHERE source_id=?",Integer.class,source)).isZero();
        assertThat(dao.selectSourceContextDetails(source).attachmentVersion()).isZero();
        var job=collectionService().insertCollectionJob(reviewActor(),source,UUID.randomUUID(),request);
        assertThat(job.jobStatusCode()).isEqualTo("PENDING");assertThat(dao.selectJobDetails(job.jobId()).requestedBy()).isEqualTo(actor);
        assertThat(dao.selectSourceContextDetails(source).attachmentReviewRequired()).isFalse();
        assertThat(dao.selectSourceContextDetails(source).attachmentPolicyId()).isNull();
        var claimed=service.saveNextJobClaim().orElseThrow();
        evidenceService.saveAttachmentSet(claimed.jobId(),claimed.leaseToken(),new AttachmentSetEvidence("NO_FILES",true,List.of()));
        var result=context.getBean(AnnouncementAttachmentEvaluationService.class).saveJobEvaluation(claimed.jobId(),claimed.leaseToken()).orElseThrow();
        assertThat(historyService().selectEvaluationDetails(source,result.evaluationId()).evaluation().usageCode()).isEqualTo("CURRENT_PREVIEW");
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_source_links WHERE source_id=?",Integer.class,source)).isZero();
    }
    @Test void manualCollectionAndFailedRetryConsumeOneSharedRateWindowWithoutLosingGuard() {
        var fixture=insertRetryFixture();insertCollectionLocator(fixture.source());
        var confirmation=reviewService().insertConfirmation(reviewActor(),fixture.source(),UUID.randomUUID(),selectReviewRequest(fixture.source()));
        var request=selectCollectionRequest(fixture.source());collectionService().insertCollectionJob(reviewActor(),fixture.source(),UUID.randomUUID(),request);
        assertThat(sql.queryForObject("SELECT is_current FROM announcement_source_attachment_confirmations WHERE id=?",Boolean.class,confirmation.confirmationId())).isFalse();
        assertThat(dao.selectSourceContextDetails(fixture.source()).attachmentReviewRequired()).isTrue();
        assertThat(dao.selectSourceContextDetails(fixture.source()).currentAttachmentEvaluationId()).isNull();
        var job=service.saveNextJobClaim().orElseThrow();service.saveDownloadBytes(job.jobId(),job.leaseToken(),15);
        var failed=new AttachmentSetEvidence.File(new AttachmentSetEvidence.Locator(EXECUTION.profileCode(),"/download/file",java.util.Map.of("fileId","collection-retry-2")),
                "실패 공고문.hwpx",null,"NOTICE","PROFILE","FAILED",0,null,AttachmentFailureCode.NETWORK_TIMEOUT,null);
        var set=evidenceService.saveAttachmentSet(job.jobId(),job.leaseToken(),new AttachmentSetEvidence("FOUND",true,List.of(selectFileEvidence(100),failed))).orElseThrow();
        context.getBean(AnnouncementAttachmentEvaluationService.class).saveJobEvaluation(job.jobId(),job.leaseToken()).orElseThrow();
        var files=evidenceDao.selectFileList(fixture.source(),set.setId(),0,10);
        var retryFixture=new RetryFixture(fixture.source(),set.setId(),files.get(1).fileId(),files.getFirst());
        assertThatThrownBy(()->retryService().insertFileRetry(reviewActor(),fixture.source(),UUID.randomUUID(),selectRetryRequest(retryFixture)))
                .isInstanceOfSatisfying(ApiException.class,e->assertThat(e.errorCode()).isEqualTo(com.saneb.common.error.ErrorCode.ANNOUNCEMENT_ATTACHMENT_RETRY_RATE_LIMITED));
        assertThatThrownBy(()->collectionService().selectCollectionContextDetails(reviewActor(),fixture.source()))
                .isInstanceOfSatisfying(ApiException.class,e->assertThat(e.errorCode()).isEqualTo(com.saneb.common.error.ErrorCode.ANNOUNCEMENT_ATTACHMENT_COLLECTION_RATE_LIMITED));
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_attachment_jobs WHERE source_id=? AND requested_by IS NOT NULL",Integer.class,fixture.source())).isEqualTo(1);
        assertThat(evidenceDao.selectFileList(fixture.source(),fixture.set(),0,10).get(1).downloadStatusCode()).isEqualTo("FAILED");
    }
    @Test void simultaneousManualCollectionKeyCreatesExactlyOneJobAndOneVersionChange() throws Exception {
        var source=selectRequest().sourceId();insertCollectionLocator(source);var request=selectCollectionRequest(source);
        UUID key=UUID.randomUUID();var start=new CountDownLatch(1);
        Callable<UUID> call=()->{start.await(5,TimeUnit.SECONDS);return collectionService().insertCollectionJob(reviewActor(),source,key,request).jobId();};
        try(var executor=Executors.newFixedThreadPool(2)) {var first=executor.submit(call);var second=executor.submit(call);start.countDown();
            assertThat(first.get(15,TimeUnit.SECONDS)).isEqualTo(second.get(15,TimeUnit.SECONDS));}
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_attachment_jobs WHERE source_id=?",Integer.class,source)).isEqualTo(1);
        assertThat(dao.selectSourceContextDetails(source).attachmentVersion()).isEqualTo(1);
        assertThat(collectionService().insertCollectionJob(reviewActor(),source,key,request).jobStatusCode()).isEqualTo("PENDING");
    }
    @Test void manualCollectionRejectsChangedConditionWithoutCreatingAJob() {
        var source=selectRequest().sourceId();insertCollectionLocator(source);var request=selectCollectionRequest(source);
        sql.update("UPDATE announcement_source_snapshots SET classification_row_version=classification_row_version+1 WHERE id=?",source);
        assertThatThrownBy(()->collectionService().insertCollectionJob(reviewActor(),source,UUID.randomUUID(),request)).isInstanceOf(ApiException.class);
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_attachment_jobs WHERE source_id=?",Integer.class,source)).isZero();
    }

    private com.saneb.domain.announcementattachment.service.AnnouncementAttachmentPolicyService policyService() {
        return context.getBean(com.saneb.domain.announcementattachment.service.AnnouncementAttachmentPolicyService.class);
    }
    private com.saneb.domain.announcementattachment.service.AnnouncementAttachmentPolicyPublicationImpactService publicationImpactService() {
        return context.getBean(com.saneb.domain.announcementattachment.service.AnnouncementAttachmentPolicyPublicationImpactService.class);
    }
    private com.saneb.domain.announcementattachment.service.AnnouncementAttachmentPolicyPublicationScopeService publicationScopeService() {
        return context.getBean(com.saneb.domain.announcementattachment.service.AnnouncementAttachmentPolicyPublicationScopeService.class);
    }
    private com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentPolicyPublicationScopeDao publicationScopeDao() {
        return context.getBean(SqlSessionTemplate.class).getMapper(com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentPolicyPublicationScopeDao.class);
    }
    private UUID publicationScopeDraft(){return policyService().insertPolicy(reviewActor(),UUID.randomUUID(),policyCreateRequest()).policy().policyId();}
    private com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentPolicyPublicationDao publicationDao(){
        return context.getBean(SqlSessionTemplate.class).getMapper(com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentPolicyPublicationDao.class);
    }
    private com.saneb.domain.announcementattachment.dto.AttachmentPolicyPublicationScope.Details publicationDatabaseFixture(){
        UUID draft=publicationScopeDraft(),run=UUID.randomUUID(),token=UUID.randomUUID();
        var validation=context.getBean(SqlSessionTemplate.class).getMapper(com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentPolicyValidationDao.class);
        int ruleVersion=sql.queryForObject("SELECT row_version FROM announcement_source_classification_rule_releases WHERE id=?",Integer.class,release);
        // DB transition 계약을 위한 임시 metadata fixture다. 실제 QA 근거가 아니며 Java 게시 verifier를 통과하지 못한다.
        String input=sql.queryForObject("SELECT jsonb_build_object('installed',jsonb_build_object('runtimeHash',repeat('b',64)),'settings',settings_json)::text FROM announcement_attachment_policies WHERE id=?",String.class,draft);
        validation.insertRun(new com.saneb.domain.announcementattachment.vo.AttachmentPolicyValidationRows.Insert(run,draft,0,release,ruleVersion,"c".repeat(64),input,actor,UUID.randomUUID(),"d".repeat(64)));
        validation.updateClaim(run,token);validation.insertExtractionLease(run,token);
        for(String step:List.of("CLASSIFICATION_GOLDEN","INSTALLED_RUNTIME","PROVIDER_PROFILES","WORKER_DB_RECOVERY"))validation.insertStep(run,token,step,"PASSED","{}","e".repeat(64));
        validation.updateFinished(run,token,"VERIFIED",null);validation.deleteExtractionLease(run,token);
        return publicationScopeService().insertScope(reviewActor(),draft,UUID.randomUUID(),new com.saneb.domain.announcementattachment.dto.AttachmentPolicyPublicationScope.Prepare(0,"DB 게시 계약 QA"));
    }
    private UUID insertPublicationDatabaseReceipt(com.saneb.domain.announcementattachment.dto.AttachmentPolicyPublicationScope.Summary scope){
        UUID id=UUID.randomUUID();
        publicationDao().selectPublicationLock();
        assertThat(publicationDao().insertPublication(new com.saneb.domain.announcementattachment.vo.AttachmentPolicyPublicationRows.Insert(id,scope.policyId(),scope.scopeId(),actor,UUID.randomUUID(),"a".repeat(64),"b".repeat(64),"c".repeat(64),"b".repeat(64)))).isEqualTo(1);
        return id;
    }
    @Test void publicationLockBlocksProviderWritersButAllowsReadersUntilCommit() throws Exception {
        try(var owner=postgres.getPostgresDatabase().getConnection();var writer=postgres.getPostgresDatabase().getConnection()) {
            owner.setAutoCommit(false);
            try(var lock=owner.createStatement();var probe=writer.createStatement()) {
                lock.execute("SELECT attachment_policy_publication_lock()");probe.execute("SET lock_timeout='500ms'");
                for(String table:List.of("announcement_attachment_provider_qa_runs","announcement_attachment_provider_qa_cases","announcement_attachment_provider_qa_run_plans")) {
                    try(var rows=probe.executeQuery("SELECT count(1) FROM "+table)){assertThat(rows.next()).isTrue();}
                    String column=table.equals("announcement_attachment_provider_qa_runs")?"id":"run_id";
                    // 고정된 테스트 테이블에 행 변경 없는 UPDATE도 필요한 DML 잠금을 얻지 못해야 한다.
                    assertThatThrownBy(()->probe.execute("UPDATE "+table+" SET "+column+"="+column+" WHERE false"))
                            .isInstanceOf(java.sql.SQLException.class).satisfies(e->assertThat(((java.sql.SQLException)e).getSQLState()).isEqualTo("55P03"));
                }
                owner.commit();
                assertThat(probe.executeUpdate("UPDATE announcement_attachment_provider_qa_runs SET created_xid=created_xid WHERE false")).isZero();
            } finally {owner.rollback();}
        }
    }
    @Test void activeProviderWriterRejectsPublicationNowaitWithoutPartialLocks() throws Exception {
        try(var writer=postgres.getPostgresDatabase().getConnection();var publication=postgres.getPostgresDatabase().getConnection()) {
            writer.setAutoCommit(false);publication.setAutoCommit(false);
            try(var hold=writer.createStatement();var attempt=publication.createStatement()) {
                hold.execute("LOCK TABLE announcement_attachment_provider_qa_runs IN ROW EXCLUSIVE MODE");
                assertThatThrownBy(()->attempt.execute("SELECT attachment_policy_publication_lock()"))
                        .isInstanceOf(java.sql.SQLException.class).satisfies(e->assertThat(((java.sql.SQLException)e).getSQLState()).isEqualTo("55P03"));
                publication.rollback();writer.rollback();
                attempt.execute("SELECT attachment_policy_publication_lock()");publication.commit();
            } finally {writer.rollback();publication.rollback();}
        }
    }
    @Test void publicationDatabaseChangesOnlyPoliciesAndKeepsFrozenJobsAndSources() {
        var reserved=service.insertAttachmentJob(selectRequest());var before=dao.selectJobDetails(reserved.jobId());
        var scope=publicationDatabaseFixture().scope();var tx=new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
        UUID id=tx.execute(s->{UUID receipt=insertPublicationDatabaseReceipt(scope);assertThat(publicationDao().updatePreviousPolicyRetired(receipt)).isEqualTo(1);assertThat(publicationDao().updatePolicyActive(receipt)).isEqualTo(1);return receipt;});
        assertThat(publicationDao().selectReceiptDetails(scope.policyId()).publicationId()).isEqualTo(id);
        assertThat(sql.queryForObject("SELECT policy_status_code FROM announcement_attachment_policies WHERE id=?",String.class,policy)).isEqualTo("RETIRED");
        assertThat(sql.queryForObject("SELECT policy_status_code FROM announcement_attachment_policies WHERE id=?",String.class,scope.policyId())).isEqualTo("ACTIVE");
        assertThat(sql.queryForObject("SELECT settings_json->>'extractorConfigHash' FROM announcement_attachment_policies WHERE id=?",String.class,scope.policyId())).isEqualTo("b".repeat(64));
        assertThat(dao.selectJobDetails(reserved.jobId())).isEqualTo(before);
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_source_links",Integer.class)).isZero();
        assertThatThrownBy(()->sql.update("DELETE FROM announcement_attachment_policy_publications WHERE id=?",id)).isInstanceOf(DataIntegrityViolationException.class);
    }
    @Test void publicationDatabaseRejectsReceiptOnlyOrPartialPolicySwap() {
        var scope=publicationDatabaseFixture().scope();var tx=new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
        assertPublicationScopeConstraint(()->tx.executeWithoutResult(s->insertPublicationDatabaseReceipt(scope)));
        assertPublicationScopeConstraint(()->tx.executeWithoutResult(s->{UUID id=insertPublicationDatabaseReceipt(scope);publicationDao().updatePreviousPolicyRetired(id);}));
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_attachment_policy_publications",Integer.class)).isZero();
        assertThat(sql.queryForObject("SELECT policy_status_code FROM announcement_attachment_policies WHERE id=?",String.class,policy)).isEqualTo("ACTIVE");
    }
    @Test void publicationDatabaseRejectsStaleScopeWithoutChangingActivePolicy() {
        var scope=publicationDatabaseFixture().scope();sql.update("UPDATE announcement_attachment_policies SET row_version=row_version+1 WHERE id=?",scope.policyId());
        var tx=new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
        assertPublicationScopeConstraint(()->tx.executeWithoutResult(s->insertPublicationDatabaseReceipt(scope)));
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_attachment_policy_publications",Integer.class)).isZero();
    }
    @Test void publicationDatabaseNeverWaitsForAnExistingWriter()throws Exception{
        var scope=publicationDatabaseFixture().scope();
        try(var other=context.getBean(DataSource.class).getConnection()){
            other.setAutoCommit(false);
            try(var statement=other.createStatement()){statement.execute("LOCK TABLE announcement_source_snapshots IN ROW EXCLUSIVE MODE");}
            var tx=new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
            assertThatThrownBy(()->tx.executeWithoutResult(s->insertPublicationDatabaseReceipt(scope))).satisfies(error->{
                Throwable cause=error;while(cause.getCause()!=null)cause=cause.getCause();assertThat(cause).isInstanceOf(java.sql.SQLException.class);assertThat(((java.sql.SQLException)cause).getSQLState()).isEqualTo("55P03");
            });
            other.rollback();
        }
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_attachment_policy_publications",Integer.class)).isZero();
    }
    private void insertOpenPublicationScope(UUID scope,UUID draft){
        assertThat(publicationScopeDao().insertScope(new com.saneb.domain.announcementattachment.vo.AttachmentPolicyPublicationScopeRows.Insert(
                scope,draft,0,actor,UUID.randomUUID(),"a".repeat(64),"b".repeat(64)))).isEqualTo(1);
    }
    private void assertPublicationScopeConstraint(org.assertj.core.api.ThrowableAssert.ThrowingCallable action){
        // statement 실패는 DataAccessException, deferred COMMIT 실패는 TransactionSystemException으로 감싸질 수 있다.
        // wrapper 종류로 DB 계약을 추측하지 않고 실제 PostgreSQL CHECK 위반 SQLSTATE를 확인한다.
        assertThatThrownBy(action).satisfies(error->{
            Throwable cause=error;while(cause.getCause()!=null)cause=cause.getCause();
            assertThat(cause).isInstanceOf(java.sql.SQLException.class);
            assertThat(((java.sql.SQLException)cause).getSQLState()).isEqualTo("23514");
        });
    }
    @Test void publicationScopeFreezesOver1000MembersAndReplaysWithoutPublishing() {
        UUID draft=publicationScopeDraft(),key=UUID.randomUUID();
        // 테스트 소유 임시 DB의 퇴역 정책 metadata만 만든다. 실제 정책을 활성화하지 않는다.
        sql.update("""
                INSERT INTO announcement_attachment_policies(id,policy_code,version_no,policy_status_code,mode_code,rule_release_id,
                    policy_hash,settings_json,profile_manifest_json,created_by,published_at)
                SELECT gen_random_uuid(),'SCOPE-QA-'||g,1,'RETIRED','OFF',?,repeat('d',64),'{}'::jsonb,'[]'::jsonb,?,now()
                FROM generate_series(1,1001) g
                """,release,actor);
        var request=new com.saneb.domain.announcementattachment.dto.AttachmentPolicyPublicationScope.Prepare(0,"범위 검증");
        var result=publicationScopeService().insertScope(reviewActor(),draft,key,request);
        assertThat(result.scope().itemCount()).isGreaterThan(1000);
        assertThat(result.scope().itemCount()).isEqualTo(sql.queryForObject("SELECT count(1) FROM attachment_policy_publication_members(?)",Long.class,draft));
        assertThat(result.isScopeCurrent()).isTrue();assertThat(result.isApproval()).isFalse();assertThat(result.currentHttpRequests()).isZero();
        assertThat(publicationScopeService().insertScope(reviewActor(),draft,key,request).scope()).isEqualTo(result.scope());
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_attachment_policy_publication_scopes",Integer.class)).isEqualTo(1);
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_attachment_jobs",Integer.class)).isZero();
        assertThat(sql.queryForObject("SELECT policy_status_code FROM announcement_attachment_policies WHERE id=?",String.class,draft)).isEqualTo("DRAFT");
        assertThat(publicationScopeService().selectItemList(reviewActor(),draft,result.scope().scopeId(),2,100).items()).hasSize(100);
    }
    @Test void publicationScopeRejectsOpenOrPartialCommit() {
        UUID draft=publicationScopeDraft();var tx=new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
        assertPublicationScopeConstraint(()->tx.executeWithoutResult(s->insertOpenPublicationScope(UUID.randomUUID(),draft)));
        UUID id=UUID.randomUUID();
        assertPublicationScopeConstraint(()->tx.executeWithoutResult(s->{insertOpenPublicationScope(id,draft);
            sql.update("INSERT INTO announcement_attachment_policy_publication_scope_items(scope_id,entity_type_code,entity_id,state_hash) SELECT ?,m.entity_type_code,m.entity_id,m.state_hash FROM attachment_policy_publication_members(?) m WHERE m.entity_type_code='POLICY' AND m.entity_id=?",id,draft,draft);
            publicationScopeDao().updateScopeSealed(id);
        }));
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_attachment_policy_publication_scopes",Integer.class)).isZero();
    }
    @Test void publicationScopeRejectsSameCountDifferentIdsAndStateHashes() {
        UUID draft=publicationScopeDraft();var tx=new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
        for(boolean replaceId:List.of(true,false)){
            UUID id=UUID.randomUUID();
            assertPublicationScopeConstraint(()->tx.executeWithoutResult(s->{insertOpenPublicationScope(id,draft);
                sql.update("""
                        INSERT INTO announcement_attachment_policy_publication_scope_items(scope_id,entity_type_code,entity_id,state_hash)
                        SELECT ?,m.entity_type_code,CASE WHEN ? AND m.entity_id=? THEN gen_random_uuid() ELSE m.entity_id END,
                            CASE WHEN NOT ? AND m.entity_id=? THEN repeat('f',64) ELSE m.state_hash END
                        FROM attachment_policy_publication_members(?) m
                        """,id,replaceId,draft,replaceId,draft,draft);
                publicationScopeDao().updateScopeSealed(id);
            }));
        }
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_attachment_policy_publication_scope_items",Integer.class)).isZero();
    }
    @Test void publicationScopeCannotSealAgainstChangedPolicyVersionInsideItsTransaction() {
        UUID draft=publicationScopeDraft();var tx=new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
        assertPublicationScopeConstraint(()->tx.executeWithoutResult(s->{UUID id=UUID.randomUUID();insertOpenPublicationScope(id,draft);
            sql.update("UPDATE announcement_attachment_policies SET row_version=row_version+1 WHERE id=?",draft);
            publicationScopeDao().insertScopeMembers(id,draft);publicationScopeDao().updateScopeSealed(id);
        }));
        assertThat(sql.queryForObject("SELECT row_version FROM announcement_attachment_policies WHERE id=?",Integer.class,draft)).isZero();
    }
    @Test void publicationScopeHistoryIsImmutableAndPolicyChangesInvalidateCurrentness() {
        UUID draft=publicationScopeDraft();var result=publicationScopeService().insertScope(reviewActor(),draft,UUID.randomUUID(),new com.saneb.domain.announcementattachment.dto.AttachmentPolicyPublicationScope.Prepare(0,"범위 검증"));
        UUID id=result.scope().scopeId();
        assertThatThrownBy(()->sql.update("DELETE FROM announcement_attachment_policy_publication_scopes WHERE id=?",id)).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(()->sql.update("UPDATE announcement_attachment_policy_publication_scopes SET expires_at=expires_at+interval '1 minute' WHERE id=?",id)).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(()->sql.update("DELETE FROM announcement_attachment_policy_publication_scope_items WHERE scope_id=?",id)).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(()->sql.update("INSERT INTO announcement_attachment_policy_publication_scope_items VALUES (?,'SOURCE',gen_random_uuid(),repeat('e',64))",id)).isInstanceOf(DataIntegrityViolationException.class);
        sql.update("UPDATE announcement_attachment_policies SET row_version=row_version+1 WHERE id=?",draft);
        var changed=publicationScopeService().selectScopeDetails(reviewActor(),draft,id);assertThat(changed.isScopeCurrent()).isFalse();assertThat(changed.scope()).isEqualTo(result.scope());
    }
    @Test void simultaneousPublicationScopeKeyReturnsOneImmutableReceipt() throws Exception {
        UUID draft=publicationScopeDraft(),key=UUID.randomUUID();var gate=new CountDownLatch(1);
        var request=new com.saneb.domain.announcementattachment.dto.AttachmentPolicyPublicationScope.Prepare(0,"동일 준비 요청");
        Callable<UUID> action=()->{gate.await(5,TimeUnit.SECONDS);return publicationScopeService().insertScope(reviewActor(),draft,key,request).scope().scopeId();};
        try(var executor=Executors.newFixedThreadPool(2)){var a=executor.submit(action);var b=executor.submit(action);gate.countDown();assertThat(a.get(20,TimeUnit.SECONDS)).isEqualTo(b.get(20,TimeUnit.SECONDS));}
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_attachment_policy_publication_scopes",Integer.class)).isEqualTo(1);
        assertThat(sql.queryForObject("SELECT count(1) FROM audit_logs WHERE resource_id=? AND action_code='ATTACHMENT_POLICY_SCOPE_PREPARE'",Integer.class,draft)).isEqualTo(1);
    }
    @Test void publicationImpactReportsEmptyDraftScopeWithoutChangingPolicyOrAudit() {
        var draft=policyService().insertPolicy(reviewActor(),UUID.randomUUID(),policyCreateRequest());
        long audits=sql.queryForObject("SELECT count(1) FROM audit_logs",Long.class);
        var result=publicationImpactService().selectImpactDetails(reviewActor(),draft.policy().policyId());
        assertThat(result.activePolicyForRule().policyId()).isEqualTo(policy);
        assertThat(result.matchingRule().boundSourceCount()).isZero();assertThat(result.allRules().frozenCollectionJobCount()).isZero();
        assertThat(result.requiresPublicationRevalidation()).isTrue();assertThat(result.blockingReasonCodes()).contains("QA_NOT_REQUESTED");
        assertThat(policyService().selectPolicyDetails(reviewActor(),draft.policy().policyId())).isEqualTo(draft);
        assertThat(sql.queryForObject("SELECT count(1) FROM audit_logs",Long.class)).isEqualTo(audits);
    }
    @Test void publicationImpactCountsProductionBindingsAndJobsWithoutChangingThem() {
        updateFixturePolicyMode("ENFORCE");var first=selectRequest();var second=selectRequest();
        var firstJob=service.insertAttachmentJob(first);service.insertAttachmentJob(second);
        sql.update("UPDATE announcement_source_snapshots SET attachment_policy_id=?,is_attachment_review_required=true WHERE id IN (?,?)",policy,first.sourceId(),second.sourceId());
        sql.update("UPDATE announcement_source_snapshots SET data_purpose_code='QA' WHERE id=?",second.sourceId());
        var sourceBefore=dao.selectSourceContextDetails(first.sourceId());var jobBefore=dao.selectJobDetails(firstJob.jobId());
        var draft=policyService().insertPolicy(reviewActor(),UUID.randomUUID(),policyCreateRequest());
        var result=publicationImpactService().selectImpactDetails(reviewActor(),draft.policy().policyId());
        assertThat(result.matchingRule().boundSourceCount()).isEqualTo(1);assertThat(result.matchingRule().reviewRequiredSourceCount()).isEqualTo(1);
        assertThat(result.allRules().frozenCollectionJobCount()).isEqualTo(1);assertThat(result.allRules().runningCollectionJobCount()).isZero();
        assertThat(dao.selectSourceContextDetails(first.sourceId())).isEqualTo(sourceBefore);assertThat(dao.selectJobDetails(firstJob.jobId())).isEqualTo(jobBefore);
    }
    @Test void publicationImpactRetainsRetiredPolicyPlansAndExcludesQaCollectionRequests() {
        UUID first=insertFixtureRun(),second=insertFixtureRun();
        for(UUID run:List.of(first,second))sql.update("INSERT INTO announcement_attachment_collection_plans(run_id,rule_release_id,policy_id,plan_status_code) VALUES (?,?,?,'FROZEN')",run,release,policy);
        sql.update("UPDATE announcement_source_collection_requests SET data_purpose_code='QA' WHERE id=(SELECT request_id FROM announcement_source_collection_runs WHERE id=?)",second);
        var draft=policyService().insertPolicy(reviewActor(),UUID.randomUUID(),policyCreateRequest());
        var before=publicationImpactService().selectImpactDetails(reviewActor(),draft.policy().policyId());
        assertThat(before.matchingRule().frozenCollectionPlanCount()).isEqualTo(1);assertThat(before.allRules().frozenCollectionPlanCount()).isEqualTo(1);
        updateFixturePolicyMode("OFF");var after=publicationImpactService().selectImpactDetails(reviewActor(),draft.policy().policyId());
        assertThat(after.matchingRule()).isEqualTo(before.matchingRule());assertThat(after.observedImpactHash()).isNotEqualTo(before.observedImpactHash());
        assertThat(after.wouldLiftGlobalOffStop()).isTrue();assertThat(after.requiresPublicationRevalidation()).isTrue();
    }
    private com.saneb.domain.announcementattachment.dto.AttachmentPolicyRequests.Create policyCreateRequest() {
        return new com.saneb.domain.announcementattachment.dto.AttachmentPolicyRequests.Create(release,"ENFORCE",83886080L,"초안 생성 원문은 감사 로그에 저장하지 않음");
    }
    @Test void managedPolicyCreateAndEditKeepOnlyDraftAndCreationRetryReturnsCurrentResource() {
        var key=UUID.randomUUID();var initial=policyService().insertPolicy(reviewActor(),key,policyCreateRequest());var id=initial.policy().policyId();
        assertThat(initial.policy().policyStatusCode()).isEqualTo("DRAFT");assertThat(initial.policy().policyHash()).isNull();
        assertThat(initial.configuration().extractorConfigHash()).isNull();assertThat(initial.systemProfileBindings()).hasSize(1);
        assertThat(initial.isDraftValidationRequired()).isTrue();
        var updated=policyService().updatePolicyDraft(reviewActor(),id,new com.saneb.domain.announcementattachment.dto.AttachmentPolicyRequests.Update(0,release,"OFF",1L,"초안 수정 QA"));
        assertThat(updated.policy().rowVersion()).isEqualTo(1);assertThat(updated.configuration().maximumSourceBytes()).isEqualTo(1L);
        var repeated=policyService().insertPolicy(reviewActor(),key,policyCreateRequest());assertThat(repeated).isEqualTo(updated);
        var list=policyService().selectPolicyList(reviewActor(),"DRAFT",release,1,10);
        assertThat(list.totalCount()).isEqualTo(1);assertThat(list.items()).containsExactly(updated.policy());
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_attachment_policies WHERE creation_idempotency_key=?",Integer.class,key)).isEqualTo(1);
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_attachment_jobs",Integer.class)).isZero();
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_attachment_collection_plans",Integer.class)).isZero();
        var metadata=sql.queryForList("SELECT metadata_json::text FROM audit_logs WHERE resource_id=? ORDER BY created_at",String.class,id);
        assertThat(metadata).hasSize(2).allSatisfy(json->assertThat(json).contains("reasonHash").doesNotContain("초안 생성 원문","초안 수정 QA","settingsJson"));
        assertThat(sql.queryForObject("SELECT policy_status_code FROM announcement_attachment_policies WHERE id=?",String.class,policy)).isEqualTo("ACTIVE");
    }
    @Test void concurrentPolicyCreationWithSameKeyCreatesOneDraftAndOneAudit() throws Exception {
        var key=UUID.randomUUID();var gate=new CountDownLatch(1);
        Callable<UUID> action=()->{gate.await(5,TimeUnit.SECONDS);return policyService().insertPolicy(reviewActor(),key,policyCreateRequest()).policy().policyId();};
        UUID id;
        try(var executor=Executors.newFixedThreadPool(2)) {
            var first=executor.submit(action);var second=executor.submit(action);gate.countDown();
            id=first.get(10,TimeUnit.SECONDS);assertThat(second.get(10,TimeUnit.SECONDS)).isEqualTo(id);
        }
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_attachment_policies WHERE creation_idempotency_key=?",Integer.class,key)).isEqualTo(1);
        assertThat(sql.queryForObject("SELECT count(1) FROM audit_logs WHERE resource_id=?",Integer.class,id)).isEqualTo(1);
    }
    @Test void policyRevisionsFromDifferentParentsSerializeOneFamilyWithoutCopyingPublication() throws Exception {
        var request=new com.saneb.domain.announcementattachment.dto.AttachmentPolicyRequests.Revision(0,"개정 QA");
        var first=policyService().insertPolicyRevision(reviewActor(),policy,UUID.randomUUID(),request);
        assertThat(first.policy().versionNo()).isEqualTo(2);assertThat(first.policy().policyHash()).isNull();assertThat(first.policy().publishedAt()).isNull();
        assertThat(first.copiedFromPolicyId()).isEqualTo(policy);assertThat(first.configuration().extractorConfigHash()).isEqualTo(CONFIG_HASH);
        assertThat(first.isDraftValidationRequired()).isTrue();
        var gate=new CountDownLatch(1);
        try(var executor=Executors.newFixedThreadPool(2)) {
            var a=executor.submit(()->{gate.await(5,TimeUnit.SECONDS);return policyService().insertPolicyRevision(reviewActor(),policy,UUID.randomUUID(),request);});
            var b=executor.submit(()->{gate.await(5,TimeUnit.SECONDS);return policyService().insertPolicyRevision(reviewActor(),first.policy().policyId(),UUID.randomUUID(),request);});
            gate.countDown();var left=a.get(10,TimeUnit.SECONDS);var right=b.get(10,TimeUnit.SECONDS);
            assertThat(List.of(left.policy().versionNo(),right.policy().versionNo())).containsExactlyInAnyOrder(3,4);
            assertThat(left.policy().policyCode()).isEqualTo(right.policy().policyCode());
        }
        assertThat(sql.queryForObject("SELECT row_version FROM announcement_attachment_policies WHERE id=?",Integer.class,policy)).isZero();
        assertThat(sql.queryForObject("SELECT policy_status_code FROM announcement_attachment_policies WHERE id=?",String.class,policy)).isEqualTo("ACTIVE");
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_attachment_jobs",Integer.class)).isZero();
    }
    @Test void concurrentPolicyEditsAllowOneVersionAndConflictWithoutExtraAudit() throws Exception {
        var created=policyService().insertPolicy(reviewActor(),UUID.randomUUID(),policyCreateRequest());var id=created.policy().policyId();
        var request=new com.saneb.domain.announcementattachment.dto.AttachmentPolicyRequests.Update(0,release,"OFF",1L,"동시 수정 QA");
        var gate=new CountDownLatch(1);
        Callable<Boolean> action=()->{gate.await(5,TimeUnit.SECONDS);try {policyService().updatePolicyDraft(reviewActor(),id,request);return true;}
            catch(ApiException conflict) {assertThat(conflict.errorCode()).isEqualTo(com.saneb.common.error.ErrorCode.ANNOUNCEMENT_ATTACHMENT_VERSION_CONFLICT);return false;}};
        try(var executor=Executors.newFixedThreadPool(2)) {
            var a=executor.submit(action);var b=executor.submit(action);gate.countDown();
            assertThat(List.of(a.get(10,TimeUnit.SECONDS),b.get(10,TimeUnit.SECONDS))).containsExactlyInAnyOrder(true,false);
        }
        assertThat(policyService().selectPolicyDetails(reviewActor(),id).policy().rowVersion()).isEqualTo(1);
        assertThat(sql.queryForObject("SELECT count(1) FROM audit_logs WHERE resource_id=?",Integer.class,id)).isEqualTo(2);
    }
    @Test void managedPolicyIdentityAndExactVersionAdvanceAreEnforcedByDatabase() {
        var created=policyService().insertPolicy(reviewActor(),UUID.randomUUID(),policyCreateRequest());var id=created.policy().policyId();
        // SQL fragments are fixed test constants, not request inputs; only this test-owned database is used.
        for(String change:List.of("policy_code='changed'","version_no=2","creation_idempotency_key=gen_random_uuid()",
                "creation_request_hash=repeat('f',64)","creation_operation_code='REVISION'","copied_from_policy_id=id","created_at=created_at+interval '1 second'")) {
            assertThatThrownBy(()->sql.update("UPDATE announcement_attachment_policies SET "+change+",row_version=row_version+1 WHERE id=?",id))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }
        assertThatThrownBy(()->sql.update("UPDATE announcement_attachment_policies SET mode_code='OFF' WHERE id=?",id)).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(()->sql.update("UPDATE announcement_attachment_policies SET row_version=row_version+2 WHERE id=?",id)).isInstanceOf(DataIntegrityViolationException.class);
        for(String family:List.of("unrelated-family",created.policy().policyCode())) {
            int version=family.equals("unrelated-family")?2:1;
            assertThatThrownBy(()->sql.update("""
                    INSERT INTO announcement_attachment_policies(policy_code,version_no,mode_code,rule_release_id,settings_json,
                        profile_manifest_json,created_by,creation_idempotency_key,creation_request_hash,creation_operation_code,copied_from_policy_id)
                    VALUES (?,?,'OFF',?,'{}'::jsonb,'[]'::jsonb,?,gen_random_uuid(),repeat('a',64),'REVISION',?)
                    """,family,version,release,actor,id)).isInstanceOf(DataIntegrityViolationException.class);
        }
        assertThat(policyService().selectPolicyDetails(reviewActor(),id)).isEqualTo(created);
    }
    @Test void policyCreationKeyCannotBeReusedForOtherBodyActorOrOperation() {
        var key=UUID.randomUUID();var created=policyService().insertPolicy(reviewActor(),key,policyCreateRequest());
        assertThatThrownBy(()->policyService().insertPolicy(reviewActor(),key,new com.saneb.domain.announcementattachment.dto.AttachmentPolicyRequests.Create(release,"OFF",1L,"다른 QA"))).isInstanceOf(ApiException.class);
        assertThatThrownBy(()->policyService().insertPolicyRevision(reviewActor(),policy,key,new com.saneb.domain.announcementattachment.dto.AttachmentPolicyRequests.Revision(0,"개정 QA"))).isInstanceOf(ApiException.class);
        var other=new com.saneb.domain.auth.vo.AuthenticatedUserDetails(new com.saneb.domain.auth.vo.AuthUserDetailsRow(UUID.randomUUID(),"other-policy-qa","unused","다른 관리자","ACTIVE",false,null,null,null),List.of("ADMIN"));
        var authentication=org.springframework.security.authentication.UsernamePasswordAuthenticationToken.authenticated(other,null,other.getAuthorities());
        assertThatThrownBy(()->policyService().insertPolicy(authentication,key,policyCreateRequest())).isInstanceOf(ApiException.class);
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_attachment_policies WHERE creation_idempotency_key=?",Integer.class,key)).isEqualTo(1);
        assertThat(sql.queryForObject("SELECT count(1) FROM audit_logs WHERE resource_id=?",Integer.class,created.policy().policyId())).isEqualTo(1);
    }
    @Test void activeAndRetiredPoliciesCannotBeEditedButRetiredRevisionIsAnUnpublishedDraft() {
        var request=new com.saneb.domain.announcementattachment.dto.AttachmentPolicyRequests.Update(0,release,"OFF",1L,"수정 QA");
        assertThatThrownBy(()->policyService().updatePolicyDraft(reviewActor(),policy,request)).isInstanceOf(ApiException.class).hasMessageContaining("새 개정");
        sql.update("UPDATE announcement_attachment_policies SET policy_status_code='RETIRED',row_version=row_version+1 WHERE id=?",policy);
        assertThatThrownBy(()->policyService().updatePolicyDraft(reviewActor(),policy,new com.saneb.domain.announcementattachment.dto.AttachmentPolicyRequests.Update(1,release,"OFF",1L,"수정 QA")))
                .isInstanceOf(ApiException.class).hasMessageContaining("새 개정");
        var key=UUID.randomUUID();var revision=new com.saneb.domain.announcementattachment.dto.AttachmentPolicyRequests.Revision(1,"퇴역 개정 QA");
        var copied=policyService().insertPolicyRevision(reviewActor(),policy,key,revision);
        assertThat(copied.policy().policyStatusCode()).isEqualTo("DRAFT");assertThat(copied.policy().publishedAt()).isNull();
        assertThat(policyService().insertPolicyRevision(reviewActor(),policy,key,revision)).isEqualTo(copied);
    }

    private com.saneb.domain.announcementattachment.service.AnnouncementAttachmentPolicyCheckService policyCheckService() {
        return context.getBean(com.saneb.domain.announcementattachment.service.AnnouncementAttachmentPolicyCheckService.class);
    }
    private UUID insertPolicyCheckFixture() {
        return insertPolicyCheckFixture(false);
    }
    private UUID insertPolicyCheckFixture(boolean segmentEngine) {
        var snapshot=context.getBean(AnnouncementSourceRuleReleaseService.class).selectRuleValidationDetails(release);
        // 테스트 소유 DB의 게시 fixture 지문을 실제 초기 seed 내용과 맞춘다. 운영 규칙은 변경하지 않는다.
        sql.update("UPDATE announcement_source_classification_rule_releases SET rule_snapshot_hash=? WHERE id=?",snapshot.calculatedSnapshotHash(),release);
        if(segmentEngine)return policyService().insertPolicy(reviewActor(),UUID.randomUUID(),policyCreateRequest()).policy().policyId();
        UUID id=UUID.randomUUID();
        // 과거 엔진의 관리 초안은 별도 fixture로 삽입한다. 신규 작성 경로·버전 증가 제약을 수정하거나 우회하지 않는다.
        assertThat(sql.update("""
                INSERT INTO announcement_attachment_policies(id,policy_code,version_no,mode_code,rule_release_id,settings_json,
                    profile_manifest_json,created_by,creation_idempotency_key,creation_request_hash,creation_operation_code)
                SELECT ?,?,1,'COLLECT_ONLY',rule_release_id,
                    (settings_json-'extractorConfigHash') || jsonb_build_object('extractorConfigHash',NULL,'roleRuleVersion',CAST(? AS text),'roleRulesHash',CAST(? AS text)),
                    profile_manifest_json,created_by,gen_random_uuid(),repeat('a',64),'CREATE'
                FROM announcement_attachment_policies WHERE id=?
                """,id,"OLD-"+id.toString().replace("-",""),
                com.saneb.domain.announcementattachment.classification.AttachmentDocumentRoleClassifier.VERSION,
                com.saneb.domain.announcementattachment.classification.AttachmentDocumentRoleClassifier.RULES_HASH,policy)).isEqualTo(1);
        return id;
    }
    @Test void policyClassificationCheckRunsCurrentPersistedRulesAndKeepsPublicationSeparate() {
        UUID id=insertPolicyCheckFixture(),key=UUID.randomUUID();
        var result=policyCheckService().insertClassificationCheck(reviewActor(),id,key,new com.saneb.domain.announcementattachment.dto.AttachmentPolicyCheckRequest(0,"분류 검증 원문"));
        assertThat(result.caseCount()).isEqualTo(30);assertThat(result.caseIds()).hasSize(30);assertThat(result.isCurrent()).isTrue();
        assertThat(result.checkTypeCode()).isEqualTo("CLASSIFICATION_GOLDEN");
        assertThat(result.ruleSnapshotHash()).isEqualTo(context.getBean(AnnouncementSourceRuleReleaseService.class).selectRuleValidationDetails(release).calculatedSnapshotHash());
        var history=policyCheckService().selectCheckList(reviewActor(),id,1,20);assertThat(history.totalCount()).isEqualTo(1);assertThat(history.items()).containsExactly(result);
        var policyDetails=policyService().selectPolicyDetails(reviewActor(),id);assertThat(policyDetails.policy().policyStatusCode()).isEqualTo("DRAFT");
        assertThat(policyDetails.policy().rowVersion()).isZero();assertThat(policyDetails.configuration().extractorConfigHash()).isNull();assertThat(policyDetails.isDraftValidationRequired()).isTrue();
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_attachment_jobs",Integer.class)).isZero();
        assertThat(sql.queryForObject("SELECT metadata_json::text FROM audit_logs WHERE resource_id=? AND action_code='ATTACHMENT_POLICY_CLASSIFICATION_CHECK'",String.class,id))
                .contains("resultHash","reasonHash").doesNotContain("분류 검증 원문","소상공인");
    }
    @Test void segmentPolicyClassificationCheckPersistsFiftyTwoCasesWithoutPublishing() throws Exception {
        UUID id=insertPolicyCheckFixture(true),key=UUID.randomUUID();
        var configuration=policyService().selectPolicyDetails(reviewActor(),id).configuration();
        assertThat(configuration.segmentRuleVersion()).isEqualTo(com.saneb.domain.announcementattachment.classification.AttachmentSegmentRoleAnalyzer.VERSION);
        assertThat(configuration.segmentRulesHash()).isEqualTo(com.saneb.domain.announcementattachment.classification.AttachmentSegmentRoleAnalyzer.RULES_HASH);
        var request=new com.saneb.domain.announcementattachment.dto.AttachmentPolicyCheckRequest(0,"구간 분류 검증 원문");
        var result=policyCheckService().insertClassificationCheck(reviewActor(),id,key,request);
        assertThat(result.caseCount()).isEqualTo(52);assertThat(result.caseIds()).hasSize(52).contains("AG-030","SG-001","SG-022");
        assertThat(result.engineVersion()).isEqualTo(com.saneb.domain.announcementattachment.classification.AttachmentSegmentClassificationEngine.VERSION);
        assertThat(result.isCurrent()).isTrue();
        assertThat(policyCheckService().insertClassificationCheck(reviewActor(),id,key,request)).isEqualTo(result);
        assertThat(policyCheckService().selectCheckList(reviewActor(),id,1,20).items()).containsExactly(result);
        var details=policyService().selectPolicyDetails(reviewActor(),id);
        assertThat(details.policy().policyStatusCode()).isEqualTo("DRAFT");assertThat(details.policy().rowVersion()).isZero();
        assertThat(details.policy().publishedAt()).isNull();assertThat(details.isDraftValidationRequired()).isTrue();
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_attachment_jobs",Integer.class)).isZero();
        assertThat(sql.queryForObject("SELECT metadata_json::text FROM audit_logs WHERE resource_id=? AND action_code='ATTACHMENT_POLICY_CLASSIFICATION_CHECK'",String.class,id))
                .contains("resultHash","reasonHash").doesNotContain("구간 분류 검증 원문","소상공인");
    }
    @Test void selectedSegmentRuleChangesDraftAndInvalidatesPreviousGoldenEvidenceWithoutPublication() {
        UUID id=insertPolicyCheckFixture(true),key=UUID.randomUUID();
        var oldRequest=new com.saneb.domain.announcementattachment.dto.AttachmentPolicyCheckRequest(0,"이전 구간 규칙 검증");
        var old=policyCheckService().insertClassificationCheck(reviewActor(),id,key,oldRequest);
        var changed=policyService().updatePolicyDraft(reviewActor(),id,new com.saneb.domain.announcementattachment.dto.AttachmentPolicyRequests.Update(
                0,release,"ENFORCE",83886080L,"구간 버전 선택 QA","segment-role-1.0.2"));
        assertThat(changed.configuration().segmentRuleVersion()).isEqualTo("segment-role-1.0.2");
        assertThat(changed.configuration().segmentRulesHash()).isEqualTo(com.saneb.domain.announcementattachment.classification.AttachmentSegmentRoleAnalyzer.QUARTER_RULES_HASH);
        assertThat(changed.policy().rowVersion()).isEqualTo(1);assertThat(changed.policy().policyStatusCode()).isEqualTo("DRAFT");
        assertThat(changed.policy().policyHash()).isNull();assertThat(changed.policy().publishedAt()).isNull();
        assertThat(policyCheckService().selectCheckList(reviewActor(),id,1,20).items().getFirst().isCurrent()).isFalse();
        assertThat(policyCheckService().insertClassificationCheck(reviewActor(),id,key,oldRequest).isCurrent()).isFalse();
        var fresh=policyCheckService().insertClassificationCheck(reviewActor(),id,UUID.randomUUID(),
                new com.saneb.domain.announcementattachment.dto.AttachmentPolicyCheckRequest(1,"새 구간 규칙 검증"));
        assertThat(fresh.caseCount()).isEqualTo(52);assertThat(fresh.isCurrent()).isTrue();
        assertThat(fresh.resultHash()).isNotEqualTo(old.resultHash());
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_attachment_jobs",Integer.class)).isZero();
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_source_links",Integer.class)).isZero();
    }
    @Test void simultaneousPolicyClassificationChecksWithSameKeyPersistOnce() throws Exception {
        UUID id=insertPolicyCheckFixture(),key=UUID.randomUUID();var gate=new CountDownLatch(1);
        var request=new com.saneb.domain.announcementattachment.dto.AttachmentPolicyCheckRequest(0,"동시 검증 QA");
        Callable<UUID> action=()->{gate.await(5,TimeUnit.SECONDS);return policyCheckService().insertClassificationCheck(reviewActor(),id,key,request).checkId();};
        try(var executor=Executors.newFixedThreadPool(2)) {
            var a=executor.submit(action);var b=executor.submit(action);gate.countDown();assertThat(a.get(20,TimeUnit.SECONDS)).isEqualTo(b.get(20,TimeUnit.SECONDS));
        }
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_attachment_policy_checks WHERE policy_id=?",Integer.class,id)).isEqualTo(1);
        assertThat(sql.queryForObject("SELECT count(1) FROM audit_logs WHERE resource_id=? AND action_code='ATTACHMENT_POLICY_CLASSIFICATION_CHECK'",Integer.class,id)).isEqualTo(1);
    }
    @Test void policyCheckHistoryBecomesStaleAndCannotBeChangedOrConnectedToAnotherVersion() {
        UUID id=insertPolicyCheckFixture(),key=UUID.randomUUID();var request=new com.saneb.domain.announcementattachment.dto.AttachmentPolicyCheckRequest(0,"이력 QA");
        var result=policyCheckService().insertClassificationCheck(reviewActor(),id,key,request);
        policyService().updatePolicyDraft(reviewActor(),id,new com.saneb.domain.announcementattachment.dto.AttachmentPolicyRequests.Update(0,release,"OFF",1L,"초안 변경 QA"));
        assertThat(policyCheckService().selectCheckList(reviewActor(),id,1,20).items().getFirst().isCurrent()).isFalse();
        var retry=policyCheckService().insertClassificationCheck(reviewActor(),id,key,request);assertThat(retry.checkId()).isEqualTo(result.checkId());assertThat(retry.isCurrent()).isFalse();
        assertThatThrownBy(()->sql.update("UPDATE announcement_attachment_policy_checks SET result_hash=repeat('f',64) WHERE id=?",result.checkId())).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(()->sql.update("DELETE FROM announcement_attachment_policies WHERE id=?",id)).isInstanceOf(DataIntegrityViolationException.class);
        var checkDao=context.getBean(SqlSessionTemplate.class).getMapper(com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentPolicyCheckDao.class);
        var row=checkDao.selectCheckDetails(key);
        assertThatThrownBy(()->checkDao.insertCheck(new com.saneb.domain.announcementattachment.vo.AttachmentPolicyCheckRows.Insert(UUID.randomUUID(),id,0,row.policySnapshotHash(),release,row.ruleVersion(),
                row.ruleSnapshotHash(),row.ruleContentHash(),row.suiteVersion(),row.engineVersion(),row.resultHash(),row.caseCount(),row.caseIdsJson(),actor,UUID.randomUUID(),"a".repeat(64))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentProviderQaDao providerQaDao() {
        return context.getBean(SqlSessionTemplate.class).getMapper(com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentProviderQaDao.class);
    }
    private record ProviderQaFixture(UUID run,UUID policy,List<UUID> cases) { }
    private com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentProviderQaManagementDao providerQaManagementDao() {
        return context.getBean(SqlSessionTemplate.class).getMapper(com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentProviderQaManagementDao.class);
    }
    private com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentProviderQaEvidenceDao providerQaEvidenceDao() {
        return context.getBean(SqlSessionTemplate.class).getMapper(com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentProviderQaEvidenceDao.class);
    }
    private void insertProviderQaPlan(UUID run,int count,int seconds) {
        assertThat(providerQaManagementDao().insertPlan(new com.saneb.domain.announcementattachment.vo.AttachmentProviderQaManagementRows.PlanInsert(
                run,"1".repeat(64),1,1,count,count,false,seconds))).isEqualTo(1);
    }
    private com.saneb.domain.announcementattachment.vo.AttachmentProviderQaRows.RunInsert providerQaRun(UUID id,UUID policyId,int count) {
        int version=sql.queryForObject("SELECT row_version FROM announcement_source_classification_rule_releases WHERE id=?",Integer.class,release);
        return new com.saneb.domain.announcementattachment.vo.AttachmentProviderQaRows.RunInsert(id,policyId,0,release,version,
                "a".repeat(64),"b".repeat(64),"c".repeat(64),"d".repeat(64),"[{\"providerCode\":\"BIZINFO\"},{\"providerCode\":\"GOV24_PUBLIC_SERVICE\"}]",
                count,3L*count,100L*count,actor,UUID.randomUUID(),"e".repeat(64));
    }
    private com.saneb.domain.announcementattachment.vo.AttachmentProviderQaRows.CaseInsert providerQaCase(UUID id,UUID run,int ordinal) {
        return new com.saneb.domain.announcementattachment.vo.AttachmentProviderQaRows.CaseInsert(id,run,ordinal,"CASE-"+ordinal,"f".repeat(64),"0".repeat(64),0,420,3,100);
    }
    private ProviderQaFixture providerQaFixture(int count) {
        return providerQaFixture(count,0);
    }
    private ProviderQaFixture providerQaFixture(int count,int expectedFilesPerCase) {
        UUID id=UUID.randomUUID(),draft=insertPolicyCheckFixture();var cases=new java.util.ArrayList<UUID>();
        new TransactionTemplate(context.getBean(PlatformTransactionManager.class)).executeWithoutResult(tx->{
            providerQaDao().selectQueueLock();assertThat(providerQaDao().insertRun(providerQaRun(id,draft,count))).isEqualTo(1);
            for(int i=1;i<=count;i++){UUID item=UUID.randomUUID();cases.add(item);var original=providerQaCase(item,id,i);
                assertThat(providerQaDao().insertCase(new com.saneb.domain.announcementattachment.vo.AttachmentProviderQaRows.CaseInsert(item,id,i,
                        original.caseCode(),original.inputHash(),original.profileHash(),expectedFilesPerCase,420,3,100))).isEqualTo(1);}
            insertProviderQaPlan(id,count,count*480);
            assertThat(providerQaDao().updateReady(id)).isEqualTo(1);
        });return new ProviderQaFixture(id,draft,List.copyOf(cases));
    }
    private void providerQaWrite(UUID run,Runnable action) {
        new TransactionTemplate(context.getBean(PlatformTransactionManager.class)).executeWithoutResult(tx->{providerQaDao().selectQueueLock();providerQaDao().selectRunLock(run);action.run();});
    }
    private UUID claimProviderQa(ProviderQaFixture f,int index) {
        UUID token=UUID.randomUUID();providerQaWrite(f.run(),()->{
            providerQaDao().updateRunStarted(f.run());assertThat(providerQaDao().updateClaim(f.cases().get(index),token)).isEqualTo(1);
        });return token;
    }
    private com.saneb.domain.announcementattachment.vo.AttachmentProviderQaRows.Resource providerResource(UUID item,UUID token,String code,int slot) {
        return new com.saneb.domain.announcementattachment.vo.AttachmentProviderQaRows.Resource(item,token,code,"HOST".equals(code)?"1".repeat(64):"GLOBAL",slot,UUID.randomUUID());
    }
    private String providerEvidence(UUID item) throws Exception {
        var row=providerQaDao().selectCaseDetails(item);var json=new ObjectMapper().createObjectNode();
        // 합성 metadata로 DB 제약만 검증한다. 실제 Provider 실행 결과로 게시하거나 재사용하지 않는다.
        json.put("scope","SINGLE_FIXED_NOTICE_PROVIDER_QA").put("status","PASSED").put("caseId",row.caseCode()).put("inputHash",row.inputHash())
                .put("profileHash",row.profileHash()).put("runtimeHash",row.runtimeHash()).put("originalFilesRemoved",true).put("isPolicyQaPassed",false)
                .put("expectedFileCount",0).put("requestReservations",row.requestReservations()).put("reservedBytes",row.reservedBytes()).put("fixtureOnly",true);
        json.putArray("files");return json.toString();
    }
    @Test void providerQaEvidenceQueriesSelectLatestAttemptIncludingPendingInsteadOfPriorCompleted() throws Exception {
        var first=providerQaFixture(1);UUID item=first.cases().getFirst(),token=claimProviderQa(first,0);String json=providerEvidence(item);
        providerQaWrite(first.run(),()->{assertThat(providerQaDao().updateFinished(item,token,"PASSED",null,json,"1".repeat(64))).isEqualTo(1);assertThat(providerQaDao().updateRunFinished(first.run())).isEqualTo(1);});
        UUID second=UUID.randomUUID();
        new TransactionTemplate(context.getBean(PlatformTransactionManager.class)).executeWithoutResult(tx->{
            providerQaDao().selectQueueLock();providerQaDao().insertRun(providerQaRun(second,first.policy(),1));
            providerQaDao().insertCase(providerQaCase(UUID.randomUUID(),second,1));insertProviderQaPlan(second,1,480);providerQaDao().updateReady(second);
        });
        var scope=new com.saneb.domain.announcementattachment.vo.AttachmentProviderQaEvidenceRows.Scope(first.policy(),"a".repeat(64),"b".repeat(64),"1".repeat(64));
        assertThat(providerQaEvidenceDao().selectLatestRunList(scope)).singleElement().satisfies(r->{assertThat(r.runId()).isEqualTo(second);assertThat(r.statusCode()).isEqualTo("READY");});
        providerQaWrite(second,()->{providerQaManagementDao().updatePendingInputChanged(second);providerQaDao().updateRunFinished(second);});
        assertThat(providerQaEvidenceDao().selectLatestRunList(scope)).singleElement().satisfies(r->{assertThat(r.runId()).isEqualTo(second);assertThat(r.statusCode()).isEqualTo("FAILED");});
        assertThat(providerQaEvidenceDao().selectLatestRunList(new com.saneb.domain.announcementattachment.vo.AttachmentProviderQaEvidenceRows.Scope(first.policy(),"9".repeat(64),"b".repeat(64),"1".repeat(64)))).isEmpty();
    }
    @Test void providerQaEvidenceProjectionPreservesPendingAndFinishedRowsWithRealTimesAndHashes() throws Exception {
        var f=providerQaFixture(2);UUID item=f.cases().getFirst(),token=claimProviderQa(f,0);String json=providerEvidence(item);
        providerQaWrite(f.run(),()->assertThat(providerQaDao().updateFinished(item,token,"PASSED",null,json,"1".repeat(64))).isEqualTo(1));
        assertThat(providerQaEvidenceDao().selectEvidenceCount(f.run())).isEqualTo(2);
        var page=providerQaEvidenceDao().selectEvidenceList(new com.saneb.domain.announcementattachment.vo.AttachmentProviderQaEvidenceRows.Page(f.run(),1,0));
        assertThat(page).singleElement().satisfies(row->{assertThat(row.caseId()).isEqualTo(item);assertThat(row.statusCode()).isEqualTo("PASSED");assertThat(row.evidenceHash()).isEqualTo("1".repeat(64));
            assertThat(row.evidenceJson()).contains("fixtureOnly");assertThat(row.startedAt()).isNotNull();assertThat(row.completedAt()).isAfterOrEqualTo(row.startedAt());assertThat(row.maximumBytes()).isEqualTo(100);});
        assertThat(providerQaEvidenceDao().selectEvidenceList(new com.saneb.domain.announcementattachment.vo.AttachmentProviderQaEvidenceRows.Page(f.run(),1,1)))
                .singleElement().satisfies(row->{assertThat(row.caseCode()).isEqualTo("CASE-2");assertThat(row.statusCode()).isEqualTo("PENDING");assertThat(row.evidenceJson()).isNull();});
        assertThat(new ObjectMapper().readTree(providerQaEvidenceDao().selectRequiredScope(f.run())).size()).isEqualTo(2);
        assertThat(providerQaEvidenceDao().selectEvidenceList(new com.saneb.domain.announcementattachment.vo.AttachmentProviderQaEvidenceRows.Page(f.run(),1,2))).isEmpty();
    }
    @Test void providerQaSegmentDigestSurvivesJsonbStorageAndTerminalEvidenceIsImmutable() throws Exception {
        // 합성 파일 metadata의 실제 PostgreSQL 저장 계약이다. 실파일 실행·정책 QA 성공을 의미하지 않는다.
        var f=providerQaFixture(1,1);UUID item=f.cases().getFirst(),token=claimProviderQa(f,0);
        var mapper=new ObjectMapper();var json=(com.fasterxml.jackson.databind.node.ObjectNode)mapper.readTree(providerEvidence(item));
        json.put("expectedFileCount",1);json.putArray("files").addObject().put("segmentAnalysisHash","7".repeat(64)).put("roleAssessmentHash","8".repeat(64));
        var verifier=new com.saneb.domain.announcementattachment.qa.AttachmentProviderQaStoredResultVerifier(mapper);
        String evidence=mapper.writeValueAsString(json),hash=verifier.hash(mapper.convertValue(json,Object.class));
        providerQaWrite(f.run(),()->assertThat(providerQaDao().updateFinished(item,token,"PASSED",null,evidence,hash)).isEqualTo(1));
        var rows=providerQaEvidenceDao().selectEvidenceList(new com.saneb.domain.announcementattachment.vo.AttachmentProviderQaEvidenceRows.Page(f.run(),1,0));
        assertThat(rows).hasSize(1);var saved=mapper.readTree(rows.getFirst().evidenceJson());
        assertThat(saved.path("files").get(0).path("segmentAnalysisHash").asText()).isEqualTo("7".repeat(64));
        assertThat(verifier.hash(mapper.convertValue(saved,Object.class))).isEqualTo(hash);assertThat(rows.getFirst().evidenceHash()).isEqualTo(hash);
        assertThat(rows.getFirst().evidenceJson()).doesNotContain("roleCodes","startOffset","소상공인");
        assertThatThrownBy(()->sql.update("UPDATE announcement_attachment_provider_qa_cases SET evidence_json=jsonb_set(evidence_json,'{files,0,segmentAnalysisHash}',to_jsonb(repeat('6',64))),row_version=row_version+1 WHERE id=?",item))
                .hasStackTraceContaining("provider QA case input and terminal results are immutable");
        assertThat(policyService().selectPolicyDetails(reviewActor(),f.policy()).policy().policyStatusCode()).isEqualTo("DRAFT");
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_attachment_jobs",Integer.class)).isZero();
    }
    @Test void providerQaApprovedPlanIsRequiredAndTimeMustEqualAllCases() {
        UUID id=UUID.randomUUID(),draft=insertPolicyCheckFixture();
        for(int seconds:List.of(0,479)) {
            assertThatThrownBy(()->new TransactionTemplate(context.getBean(PlatformTransactionManager.class)).executeWithoutResult(tx->{
                providerQaDao().insertRun(providerQaRun(id,draft,1));providerQaDao().insertCase(providerQaCase(UUID.randomUUID(),id,1));
                if(seconds>0) insertProviderQaPlan(id,1,seconds);providerQaDao().updateReady(id);
            })).hasStackTraceContaining("provider QA run requires its complete approved segment plan");
            assertThat(sql.queryForObject("SELECT count(1) FROM announcement_attachment_provider_qa_runs",Integer.class)).isZero();
            assertThat(sql.queryForObject("SELECT count(1) FROM announcement_attachment_provider_qa_run_plans",Integer.class)).isZero();
        }
    }
    @Test void providerQaApprovedPlanCannotBeReplacedAndReadProjectionKeepsFullDenominator() {
        var f=providerQaFixture(2);var management=providerQaManagementDao();var row=management.selectRunDetails(f.run());
        assertThat(row.planHash()).isEqualTo("1".repeat(64));assertThat(row.maximumSecondsIncludingMargin()).isEqualTo(960);
        assertThat(row.inputVersionsCurrent()).isTrue();assertThat(row.expectationCoverageComplete()).isFalse();assertThat(row.executableCaseCount()).isEqualTo(2);
        assertThat(management.selectRequestDetails(row.idempotencyKey()).runId()).isEqualTo(f.run());
        assertThat(management.selectRunCount(f.policy())).isEqualTo(1);assertThat(management.selectRunCount(UUID.randomUUID())).isZero();
        assertThat(management.selectRunList(new com.saneb.domain.announcementattachment.vo.AttachmentProviderQaManagementRows.Search(f.policy(),1,1))).isEmpty();
        assertThat(management.selectCaseCount(f.run())).isEqualTo(2);
        assertThat(management.selectCaseList(new com.saneb.domain.announcementattachment.vo.AttachmentProviderQaManagementRows.CaseSearch(f.run(),1,1)))
                .extracting(com.saneb.domain.announcementattachment.vo.AttachmentProviderQaManagementRows.Item::caseCode).containsExactly("CASE-2");
        assertThat(management.selectCoolingDown(f.policy())).isTrue();assertThat(management.selectActiveRunIds()).containsExactly(f.run());
        assertThat(management.selectNextCaseId(f.run())).isEqualTo(f.cases().getFirst());
        assertThatThrownBy(()->sql.update("UPDATE announcement_attachment_provider_qa_run_plans SET plan_hash=repeat('2',64) WHERE run_id=?",f.run()))
                .hasStackTraceContaining("provider QA approved plan is immutable");
        assertThatThrownBy(()->sql.update("DELETE FROM announcement_attachment_provider_qa_run_plans WHERE run_id=?",f.run()))
                .hasStackTraceContaining("provider QA approved plan is immutable");
    }
    @Test void providerQaChangedPendingInputsCannotCancelALiveOwner() {
        var f=providerQaFixture(2);UUID token=claimProviderQa(f,0);
        providerQaWrite(f.run(),()->{assertThat(providerQaManagementDao().updatePendingInputChanged(f.run())).isEqualTo(1);assertThat(providerQaDao().updateRunFinished(f.run())).isZero();});
        var live=providerQaDao().selectCaseDetails(f.cases().getFirst());assertThat(live.statusCode()).isEqualTo("RUNNING");assertThat(live.leaseToken()).isEqualTo(token);
        assertThat(providerQaDao().selectCaseDetails(f.cases().get(1)).statusCode()).isEqualTo("FAILED");assertThat(providerQaManagementDao().selectCaseCount(f.run())).isEqualTo(2);
    }
    @Test void providerQaPreparationMustCommitAllCasesAndSealAtomically() {
        UUID id=UUID.randomUUID(),draft=insertPolicyCheckFixture();
        assertThatThrownBy(()->new TransactionTemplate(context.getBean(PlatformTransactionManager.class)).executeWithoutResult(tx->providerQaDao().insertRun(providerQaRun(id,draft,1))))
                .hasStackTraceContaining("provider QA preparation must seal in the same transaction");
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_attachment_provider_qa_runs",Integer.class)).isZero();
        assertThatThrownBy(()->new TransactionTemplate(context.getBean(PlatformTransactionManager.class)).executeWithoutResult(tx->{
            providerQaDao().insertRun(providerQaRun(id,draft,2));providerQaDao().insertCase(providerQaCase(UUID.randomUUID(),id,1));providerQaDao().updateReady(id);
        })).hasStackTraceContaining("provider QA needs all frozen cases and exact budgets");
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_attachment_provider_qa_cases",Integer.class)).isZero();
    }
    @Test void providerQaReadyRequiresExactSumOfPerCaseBudgets() {
        UUID id=UUID.randomUUID(),draft=insertPolicyCheckFixture();
        assertThatThrownBy(()->new TransactionTemplate(context.getBean(PlatformTransactionManager.class)).executeWithoutResult(tx->{
            providerQaDao().insertRun(providerQaRun(id,draft,1));providerQaDao().insertCase(new com.saneb.domain.announcementattachment.vo.AttachmentProviderQaRows.CaseInsert(
                    UUID.randomUUID(),id,1,"CASE-1","f".repeat(64),"0".repeat(64),0,420,2,100));providerQaDao().updateReady(id);
        })).hasStackTraceContaining("provider QA needs all frozen cases and exact budgets");
    }
    @Test void providerQaRowsPreserveFullFrozenScopeWithoutSourceJobsOrPolicyWrites() {
        var f=providerQaFixture(2);var rows=providerQaDao().selectCaseList(f.run());assertThat(rows).hasSize(2);
        assertThat(rows).extracting(com.saneb.domain.announcementattachment.vo.AttachmentProviderQaRows.CaseRow::caseCode).containsExactly("CASE-1","CASE-2");
        assertThat(rows).allSatisfy(r->{assertThat(r.statusCode()).isEqualTo("PENDING");assertThat(r.runStatusCode()).isEqualTo("READY");assertThat(r.inputVersionsCurrent()).isTrue();assertThat(r.evidenceJson()).isNull();});
        assertThat(providerQaDao().selectRunId(f.cases().getFirst())).isEqualTo(f.run());
        assertThat(sql.queryForObject("SELECT jsonb_array_length(required_scope_json) FROM announcement_attachment_provider_qa_runs WHERE id=?",Integer.class,f.run())).isEqualTo(2);
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_attachment_jobs",Integer.class)).isZero();
        assertThat(sql.queryForObject("SELECT policy_status_code FROM announcement_attachment_policies WHERE id=?",String.class,f.policy())).isEqualTo("DRAFT");
    }
    @Test void providerQaFrozenHistoryCannotBeEditedDeletedOrAppended() {
        var f=providerQaFixture(1);UUID item=f.cases().getFirst();
        assertThatThrownBy(()->sql.update("UPDATE announcement_attachment_provider_qa_runs SET catalog_hash=repeat('1',64),row_version=row_version+1 WHERE id=?",f.run())).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(()->sql.update("UPDATE announcement_attachment_provider_qa_cases SET input_hash=repeat('1',64),row_version=row_version+1 WHERE id=?",item)).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(()->sql.update("DELETE FROM announcement_attachment_provider_qa_cases WHERE id=?",item)).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(()->sql.update("DELETE FROM announcement_attachment_provider_qa_runs WHERE id=?",f.run())).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(()->providerQaDao().insertCase(providerQaCase(UUID.randomUUID(),f.run(),2))).isInstanceOf(DataIntegrityViolationException.class);
    }
    @Test void providerQaClaimHasSingleOwnerAndCannotExtendOrReplay() {
        var f=providerQaFixture(2);UUID item=f.cases().getFirst(),token=claimProviderQa(f,0);
        providerQaWrite(f.run(),()->{assertThat(providerQaDao().updateClaim(item,UUID.randomUUID())).isZero();assertThat(providerQaDao().updateClaim(f.cases().get(1),UUID.randomUUID())).isZero();});
        assertThat(providerQaDao().selectCaseDetails(item).leaseToken()).isEqualTo(token);
        assertThat(providerQaDao().selectExecutionAllowed(item,token)).isTrue();assertThat(providerQaDao().selectExecutionAllowed(item,UUID.randomUUID())).isFalse();
        assertThatThrownBy(()->sql.update("UPDATE announcement_attachment_provider_qa_cases SET lease_expires_at=lease_expires_at+interval '1 second',row_version=row_version+1 WHERE id=?",item)).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(()->sql.update("UPDATE announcement_attachment_provider_qa_cases SET lease_token=?,row_version=row_version+1 WHERE id=?",UUID.randomUUID(),item)).isInstanceOf(DataIntegrityViolationException.class);
    }
    @Test void providerQaBudgetRequiresOwnedDownloadAndHostAndUpdatesBothLedgersAtomically() {
        var f=providerQaFixture(1);UUID item=f.cases().getFirst(),token=claimProviderQa(f,0);var download=providerResource(item,token,"DOWNLOAD",1);var host=providerResource(item,token,"HOST",1);
        providerQaWrite(f.run(),()->{
            assertThat(providerQaDao().updateUsage(item,token,1,0)).isZero();assertThat(providerQaDao().insertResource(download)).isEqualTo(1);
            assertThat(providerQaDao().updateUsage(item,token,1,0)).isZero();assertThat(providerQaDao().insertResource(host)).isEqualTo(1);
            for(int i=0;i<3;i++)assertThat(providerQaDao().updateUsage(item,token,1,0)).isEqualTo(1);
            assertThat(providerQaDao().updateUsage(item,token,1,0)).isZero();assertThat(providerQaDao().updateUsage(item,token,0,99)).isEqualTo(1);
            assertThat(providerQaDao().updateUsage(item,token,0,2)).isZero();assertThat(providerQaDao().updateUsage(item,token,0,1)).isEqualTo(1);
            assertThat(providerQaDao().updateUsage(item,token,-1,0)).isZero();assertThat(providerQaDao().updateUsage(item,token,0,-1)).isZero();
            assertThat(providerQaDao().updateUsage(item,UUID.randomUUID(),1,0)).isZero();
        });
        var row=providerQaDao().selectCaseDetails(item);assertThat(row.requestReservations()).isEqualTo(3);assertThat(row.reservedBytes()).isEqualTo(100);
        assertThat(sql.queryForObject("SELECT request_reservations FROM announcement_attachment_provider_qa_runs WHERE id=?",Long.class,f.run())).isEqualTo(3);
        assertThat(sql.queryForObject("SELECT reserved_bytes FROM announcement_attachment_provider_qa_runs WHERE id=?",Long.class,f.run())).isEqualTo(100);
        assertThatThrownBy(()->sql.update("UPDATE announcement_attachment_provider_qa_cases SET reserved_bytes=0,row_version=row_version+1 WHERE id=?",item)).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(()->sql.update("UPDATE announcement_attachment_provider_qa_runs SET reserved_bytes=0,row_version=row_version+1 WHERE id=?",f.run())).isInstanceOf(DataIntegrityViolationException.class);
    }
    @Test void providerQaCannotReleaseOtherResourceOwnerOrUseThirdDownloadSlot() {
        var f=providerQaFixture(1);UUID item=f.cases().getFirst(),token=claimProviderQa(f,0);var lease=providerResource(item,token,"DOWNLOAD",1);
        assertThat(providerQaDao().insertResource(lease)).isEqualTo(1);
        assertThat(providerQaDao().deleteResource(providerResource(item,token,"DOWNLOAD",1))).isZero();assertThat(providerQaDao().deleteCaseResources(item,UUID.randomUUID())).isZero();
        assertThatThrownBy(()->providerQaDao().insertResource(providerResource(item,token,"DOWNLOAD",3))).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(providerQaDao().deleteResource(lease)).isEqualTo(1);assertThat(providerQaDao().deleteResource(lease)).isZero();
    }
    @Test void providerQaAndRegularWorkerShareExtractionAndClearExpiredOwnerFields() {
        service.insertAttachmentJob(selectRequest());var job=service.saveNextJobClaim().orElseThrow();var f=providerQaFixture(1);UUID token=claimProviderQa(f,0);var lease=providerResource(f.cases().getFirst(),token,"EXTRACTION",1);
        var regular=service.saveExtractionLease(job.jobId(),job.leaseToken()).orElseThrow();assertThat(providerQaDao().insertResource(lease)).isZero();service.deleteResourceLease(regular);
        assertThat(providerQaDao().insertResource(lease)).isEqualTo(1);assertThat(service.saveExtractionLease(job.jobId(),job.leaseToken())).isEmpty();
        // 이 테스트가 소유한 임시 DB의 자원 임대만 만료시킨다. 운영 lease를 수정하지 않는다.
        sql.update("UPDATE announcement_attachment_resource_leases SET lease_expires_at=clock_timestamp()-interval '1 second' WHERE provider_qa_case_id=?",f.cases().getFirst());
        var replacement=service.saveExtractionLease(job.jobId(),job.leaseToken()).orElseThrow();
        assertThat(sql.queryForObject("SELECT provider_qa_case_id IS NULL AND provider_qa_lease_token IS NULL FROM announcement_attachment_resource_leases WHERE resource_code='EXTRACTION'",Boolean.class)).isTrue();
        assertThat(providerQaDao().deleteResource(lease)).isZero();service.deleteResourceLease(replacement);
    }
    @Test void providerQaAndPolicyQaUseTheSameExtractionSlot() {
        UUID validation=insertValidationFixture(),validationToken=claimValidation(validation);var f=providerQaFixture(1);UUID token=claimProviderQa(f,0);var lease=providerResource(f.cases().getFirst(),token,"EXTRACTION",1);
        assertThat(providerQaDao().insertResource(lease)).isZero();assertThat(validationDao().deleteExtractionLease(validation,validationToken)).isEqualTo(1);
        assertThat(providerQaDao().insertResource(lease)).isEqualTo(1);assertThat(validationDao().insertExtractionLease(validation,validationToken)).isZero();
        sql.update("UPDATE announcement_attachment_resource_leases SET lease_expires_at=clock_timestamp()-interval '1 second' WHERE provider_qa_case_id=?",f.cases().getFirst());
        assertThat(validationDao().insertExtractionLease(validation,validationToken)).isEqualTo(1);
        assertThat(sql.queryForObject("SELECT provider_qa_case_id IS NULL AND provider_qa_lease_token IS NULL FROM announcement_attachment_resource_leases WHERE resource_code='EXTRACTION'",Boolean.class)).isTrue();
        assertThat(providerQaDao().deleteResource(lease)).isZero();
    }
    @Test void providerQaSuccessRequiresCleanMatchingEvidenceAndAllCasesBeforeCompletion() throws Exception {
        var f=providerQaFixture(2);UUID item=f.cases().getFirst(),token=claimProviderQa(f,0);String evidence=providerEvidence(item);
        assertThatThrownBy(()->providerQaDao().updateFinished(item,token,"PASSED",null,null,null)).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(()->providerQaDao().updateFinished(item,token,"PASSED",null,evidence.replace("\"originalFilesRemoved\":true","\"originalFilesRemoved\":false"),"1".repeat(64))).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(()->providerQaDao().updateFinished(item,token,"PASSED",null,evidence.replace("CASE-1","CASE-WRONG"),"1".repeat(64))).isInstanceOf(DataIntegrityViolationException.class);
        providerQaWrite(f.run(),()->{assertThat(providerQaDao().updateFinished(item,token,"PASSED",null,evidence,"1".repeat(64))).isEqualTo(1);assertThat(providerQaDao().updateRunFinished(f.run())).isZero();});
        assertThatThrownBy(()->sql.update("UPDATE announcement_attachment_provider_qa_runs SET run_status_code='COMPLETED',completed_at=clock_timestamp(),row_version=row_version+1 WHERE id=?",f.run())).isInstanceOf(DataIntegrityViolationException.class);
        UUID next=f.cases().get(1),nextToken=claimProviderQa(f,1);String nextEvidence=providerEvidence(next);
        providerQaWrite(f.run(),()->{assertThat(providerQaDao().updateFinished(next,nextToken,"PASSED",null,nextEvidence,"2".repeat(64))).isEqualTo(1);assertThat(providerQaDao().updateRunFinished(f.run())).isEqualTo(1);});
        assertThat(providerQaDao().selectCaseDetails(item).runStatusCode()).isEqualTo("COMPLETED");
        assertThat(sql.queryForObject("SELECT policy_status_code FROM announcement_attachment_policies WHERE id=?",String.class,f.policy())).isEqualTo("DRAFT");
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_attachment_policy_validation_steps",Integer.class)).isZero();
        assertThat(providerQaDao().updateFinished(item,token,"FAILED","LATE_RESULT",null,null)).isZero();
    }
    @Test void providerQaCancellationPreservesEveryCaseAndOverridesLateSuccess() throws Exception {
        var f=providerQaFixture(2);UUID item=f.cases().getFirst(),token=claimProviderQa(f,0);String evidence=providerEvidence(item);
        providerQaWrite(f.run(),()->{
            assertThat(providerQaDao().updateCancellation(f.run(),1)).isZero();assertThat(providerQaDao().updateCancellation(f.run(),2)).isEqualTo(1);
            assertThat(providerQaDao().updateUnstartedCancelled(f.run())).isEqualTo(1);assertThat(providerQaDao().selectExecutionAllowed(item,token)).isFalse();
            assertThat(providerQaDao().updateRunFinished(f.run())).isZero();assertThat(providerQaDao().updateFinished(item,token,"PASSED",null,evidence,"1".repeat(64))).isEqualTo(1);
            assertThat(providerQaDao().updateRunFinished(f.run())).isEqualTo(1);
        });assertThat(providerQaDao().selectCaseList(f.run())).allSatisfy(r->{assertThat(r.statusCode()).isEqualTo("CANCELLED");assertThat(r.runStatusCode()).isEqualTo("CANCELLED");});
    }
    @Test void providerQaExpiredOwnerIsFailedAndCannotSubmitLateEvidence() throws Exception {
        var f=providerQaFixture(1);UUID item=f.cases().getFirst(),token=UUID.randomUUID();
        providerQaWrite(f.run(),()->{providerQaDao().updateRunStarted(f.run());sql.update("UPDATE announcement_attachment_provider_qa_cases SET case_status_code='RUNNING',row_version=row_version+1,lease_token=?,started_at=clock_timestamp(),lease_expires_at=clock_timestamp()+interval '100 milliseconds' WHERE id=?",token,item);});
        sql.execute("SELECT pg_sleep(0.2)");assertThat(providerQaDao().selectExecutionAllowed(item,token)).isFalse();
        assertThat(providerQaDao().updateFinished(item,token,"PASSED",null,providerEvidence(item),"1".repeat(64))).isZero();
        providerQaWrite(f.run(),()->{assertThat(providerQaDao().updateExpiredCases(f.run())).isEqualTo(1);assertThat(providerQaDao().updateRunFinished(f.run())).isEqualTo(1);});
        var row=providerQaDao().selectCaseDetails(item);assertThat(row.statusCode()).isEqualTo("FAILED");assertThat(row.runStatusCode()).isEqualTo("FAILED");assertThat(row.leaseToken()).isNull();assertThat(row.evidenceJson()).isNull();
        assertThat(sql.queryForObject("SELECT error_code FROM announcement_attachment_provider_qa_cases WHERE id=?",String.class,item)).isEqualTo("LEASE_EXPIRED");
    }
    @Test void providerQaPolicyChangeStopsFurtherRequestsAndCannotBecomeSuccess() throws Exception {
        var f=providerQaFixture(1);UUID item=f.cases().getFirst(),token=claimProviderQa(f,0);String evidence=providerEvidence(item);
        sql.update("UPDATE announcement_attachment_policies SET row_version=row_version+1 WHERE id=?",f.policy());
        assertThat(providerQaDao().selectExecutionAllowed(item,token)).isFalse();assertThat(providerQaDao().selectCaseDetails(item).inputVersionsCurrent()).isFalse();
        providerQaWrite(f.run(),()->{assertThat(providerQaDao().updateFinished(item,token,"PASSED",null,evidence,"1".repeat(64))).isEqualTo(1);providerQaDao().updateRunFinished(f.run());});
        assertThat(providerQaDao().selectCaseDetails(item).statusCode()).isEqualTo("FAILED");
        assertThat(sql.queryForObject("SELECT error_code FROM announcement_attachment_provider_qa_cases WHERE id=?",String.class,item)).isEqualTo("INPUT_CHANGED");
    }
    @Test void providerQaConcurrentClaimsConsumeOnlyOneCase() throws Exception {
        var f=providerQaFixture(2);var gate=new CountDownLatch(1);
        Callable<Integer> first=()->{gate.await(5,TimeUnit.SECONDS);return new TransactionTemplate(context.getBean(PlatformTransactionManager.class)).execute(tx->{providerQaDao().selectQueueLock();providerQaDao().selectRunLock(f.run());providerQaDao().updateRunStarted(f.run());return providerQaDao().updateClaim(f.cases().getFirst(),UUID.randomUUID());});};
        Callable<Integer> second=()->{gate.await(5,TimeUnit.SECONDS);return new TransactionTemplate(context.getBean(PlatformTransactionManager.class)).execute(tx->{providerQaDao().selectQueueLock();providerQaDao().selectRunLock(f.run());providerQaDao().updateRunStarted(f.run());return providerQaDao().updateClaim(f.cases().get(1),UUID.randomUUID());});};
        try(var pool=Executors.newFixedThreadPool(2)){var a=pool.submit(first);var b=pool.submit(second);gate.countDown();assertThat(a.get(20,TimeUnit.SECONDS)+b.get(20,TimeUnit.SECONDS)).isEqualTo(1);}
        assertThat(providerQaDao().selectCaseList(f.run())).filteredOn(r->"RUNNING".equals(r.statusCode())).hasSize(1);
    }

    private com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentPolicyValidationDao validationDao() {
        return context.getBean(SqlSessionTemplate.class).getMapper(com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentPolicyValidationDao.class);
    }
    private UUID insertValidationFixture() {
        UUID id=insertPolicyCheckFixture(),run=UUID.randomUUID();
        int ruleVersion=sql.queryForObject("SELECT row_version FROM announcement_source_classification_rule_releases WHERE id=?",Integer.class,release);
        assertThat(validationDao().insertRun(new com.saneb.domain.announcementattachment.vo.AttachmentPolicyValidationRows.Insert(run,id,0,release,ruleVersion,
                "a".repeat(64),"{\"schemaVersion\":1}",actor,UUID.randomUUID(),"b".repeat(64)))).isEqualTo(1);
        return run;
    }
    private UUID claimValidation(UUID run) {
        UUID token=UUID.randomUUID();
        new TransactionTemplate(context.getBean(PlatformTransactionManager.class)).executeWithoutResult(tx->{
            validationDao().selectQueueLock();assertThat(validationDao().updateClaim(run,token)).isEqualTo(1);
            assertThat(validationDao().insertExtractionLease(run,token)).isEqualTo(1);
        });return token;
    }
    @Test void validationReservationMapsMetadataAndDoesNotCreateSourceJobs() {
        UUID run=insertValidationFixture();var row=validationDao().selectRunDetails(run,false);
        assertThat(row.statusCode()).isEqualTo("PENDING");assertThat(row.inputVersionsCurrent()).isTrue();assertThat(row.leaseToken()).isNull();
        var search=new com.saneb.domain.announcementattachment.vo.AttachmentPolicyValidationRows.Search(row.policyId(),20,0);
        assertThat(validationDao().selectRunCount(search)).isEqualTo(1);assertThat(validationDao().selectRunList(search)).singleElement()
                .satisfies(v->assertThat(v.inputSnapshotJson()).isNull());
        assertThat(validationDao().selectRequestDetails(row.idempotencyKey()).runId()).isEqualTo(run);
        assertThat(validationDao().selectCoolingDown(row.policyId())).isTrue();assertThat(validationDao().selectRecentCount(row.policyId())).isEqualTo(1);
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_attachment_jobs",Integer.class)).isZero();
    }
    @Test void validationHasOnlyOneActiveRunAndCannotReuseMutableInputs() {
        UUID run=insertValidationFixture();var row=validationDao().selectRunDetails(run,false);
        assertThatThrownBy(()->validationDao().insertRun(new com.saneb.domain.announcementattachment.vo.AttachmentPolicyValidationRows.Insert(UUID.randomUUID(),row.policyId(),0,release,row.ruleVersion(),
                "a".repeat(64),"{}",actor,UUID.randomUUID(),"b".repeat(64)))).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(()->sql.update("UPDATE announcement_attachment_policy_validation_runs SET snapshot_hash=repeat('f',64),row_version=row_version+1 WHERE id=?",run))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(validationDao().selectActiveCount()).isEqualTo(1);
    }
    @Test void validationAndCollectionWorkersShareOneExtractionSlot() {
        UUID run=insertValidationFixture(),token=claimValidation(run);
        service.insertAttachmentJob(selectRequest());var job=service.saveNextJobClaim().orElseThrow();
        assertThat(service.saveExtractionLease(job.jobId(),job.leaseToken())).isEmpty();
        assertThat(validationDao().updateCancellation(run,1)).isEqualTo(1);
        assertThat(validationDao().updateFinished(run,token,"INCOMPLETE","QA_REQUIRED")).isEqualTo(1);
        assertThat(validationDao().selectRunDetails(run,false).statusCode()).isEqualTo("CANCELLED");
        assertThat(validationDao().deleteExtractionLease(run,token)).isEqualTo(1);
        var acquired=service.saveExtractionLease(job.jobId(),job.leaseToken()).orElseThrow();
        assertThat(sql.queryForObject("SELECT policy_validation_id IS NULL FROM announcement_attachment_resource_leases WHERE resource_code='EXTRACTION'",Boolean.class)).isTrue();
        service.deleteResourceLease(acquired);
    }
    @Test void busyCollectionSlotRollsBackPolicyQaClaim() {
        service.insertAttachmentJob(selectRequest());var job=service.saveNextJobClaim().orElseThrow();
        var acquired=service.saveExtractionLease(job.jobId(),job.leaseToken()).orElseThrow();
        UUID run=insertValidationFixture(),token=UUID.randomUUID();
        new TransactionTemplate(context.getBean(PlatformTransactionManager.class)).executeWithoutResult(tx->{
            validationDao().selectQueueLock();assertThat(validationDao().updateClaim(run,token)).isEqualTo(1);
            assertThat(validationDao().insertExtractionLease(run,token)).isZero();tx.setRollbackOnly();
        });
        assertThat(validationDao().selectRunDetails(run,false).statusCode()).isEqualTo("PENDING");
        assertThat(validationDao().selectRunDetails(run,false).leaseToken()).isNull();service.deleteResourceLease(acquired);
    }
    @Test void validationEvidenceAndTerminalStateAreImmutableAndPartialCannotVerify() {
        UUID run=insertValidationFixture(),token=claimValidation(run);
        assertThat(validationDao().insertStep(run,token,"CLASSIFICATION_GOLDEN","PASSED","{\"caseCount\":30}","c".repeat(64))).isEqualTo(1);
        assertThatThrownBy(()->validationDao().updateFinished(run,token,"VERIFIED",null)).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(()->sql.update("UPDATE announcement_attachment_policy_validation_steps SET evidence_hash=repeat('f',64) WHERE run_id=?",run)).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(()->sql.update("DELETE FROM announcement_attachment_policy_validation_steps WHERE run_id=?",run)).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(validationDao().updateFinished(run,token,"INCOMPLETE","QA_REQUIRED")).isEqualTo(1);
        assertThat(validationDao().selectStepList(run)).singleElement().satisfies(s->assertThat(s.stepCode()).isEqualTo("CLASSIFICATION_GOLDEN"));
        assertThatThrownBy(()->sql.update("UPDATE announcement_attachment_policy_validation_runs SET run_status_code='PENDING',row_version=row_version+1 WHERE id=?",run))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(validationDao().insertStep(run,token,"INSTALLED_RUNTIME","PASSED","{}","d".repeat(64))).isZero();
    }
    @Test void validationCancellationAndForeignTokensFenceEvidenceAndLeaseRelease() {
        UUID run=insertValidationFixture(),token=claimValidation(run),other=UUID.randomUUID();
        assertThat(validationDao().selectExecutionAllowed(run,token)).isTrue();
        assertThat(validationDao().selectExecutionAllowed(run,other)).isFalse();
        assertThat(validationDao().deleteExtractionLease(run,other)).isZero();
        assertThat(validationDao().insertStep(run,other,"INSTALLED_RUNTIME","PASSED","{}","d".repeat(64))).isZero();
        assertThat(validationDao().updateCancellation(run,0)).isZero();assertThat(validationDao().updateCancellation(run,1)).isEqualTo(1);
        assertThat(validationDao().selectExecutionAllowed(run,token)).isFalse();
        assertThat(validationDao().insertStep(run,token,"INSTALLED_RUNTIME","PASSED","{}","d".repeat(64))).isZero();
        assertThat(validationDao().updateFinished(run,token,"VERIFIED",null)).isEqualTo(1);
        assertThat(validationDao().selectRunDetails(run,false).statusCode()).isEqualTo("CANCELLED");
    }
    @Test void validationTargetInventoryIncludesEveryEnabledSourceRegardlessOfRecentFailure() {
        var targets=validationDao().selectTargetList();
        assertThat(targets.size()).isEqualTo(Math.min(1001,sql.queryForObject("SELECT count(1) FROM local_government_notice_sources WHERE is_enabled AND deleted_at IS NULL",Integer.class)));
        assertThat(targets).as("migration seeds must provide an enabled source for this regression").isNotEmpty();
        {
            UUID id=targets.getFirst().sourceId();
            var template=new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
            template.executeWithoutResult(tx->{
                sql.update("UPDATE local_government_notice_sources SET collection_status_code='FAILED' WHERE id=?",id);
                assertThat(validationDao().selectTargetList()).extracting(com.saneb.domain.announcementattachment.vo.AttachmentPolicyValidationRows.Target::sourceId).contains(id);tx.setRollbackOnly();
            });
        }
    }
    @Test void twoPolicyWorkersCannotClaimOneReservation() throws Exception {
        UUID run=insertValidationFixture();var start=new CountDownLatch(1);
        Callable<Boolean> attempt=()->{start.await(5,TimeUnit.SECONDS);return new TransactionTemplate(context.getBean(PlatformTransactionManager.class)).execute(tx->{
            validationDao().selectQueueLock();var pending=validationDao().selectPendingDetails();if(pending==null)return false;
            UUID token=UUID.randomUUID();assertThat(validationDao().updateClaim(pending.runId(),token)).isEqualTo(1);
            assertThat(validationDao().insertExtractionLease(pending.runId(),token)).isEqualTo(1);return true;
        });};
        try(var executor=Executors.newFixedThreadPool(2)) {
            var first=executor.submit(attempt);var second=executor.submit(attempt);start.countDown();
            assertThat(List.of(first.get(10,TimeUnit.SECONDS),second.get(10,TimeUnit.SECONDS))).containsExactlyInAnyOrder(true,false);
        }
        assertThat(validationDao().selectRunDetails(run,false).rowVersion()).isEqualTo(1);
    }

    @Test void validationRunDeletionAndCancellationOwnerReplacementAreRejected() {
        UUID run=insertValidationFixture(),token=claimValidation(run);
        assertThatThrownBy(()->sql.update("DELETE FROM announcement_attachment_policy_validation_runs WHERE id=?",run))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(()->sql.update("UPDATE announcement_attachment_policy_validation_runs SET run_status_code='CANCEL_REQUESTED',lease_token=?,row_version=row_version+1 WHERE id=?",UUID.randomUUID(),run))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(()->sql.update("UPDATE announcement_attachment_policy_validation_runs SET run_status_code='CANCEL_REQUESTED',lease_expires_at=lease_expires_at+interval '1 minute',row_version=row_version+1 WHERE id=?",run))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(()->sql.update("UPDATE announcement_attachment_policy_validation_runs SET run_status_code='CANCEL_REQUESTED',started_at=started_at+interval '1 second',row_version=row_version+1 WHERE id=?",run))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(validationDao().selectRunDetails(run,false).leaseToken()).isEqualTo(token);
    }

    @Test void validationEvidenceWithoutItsGlobalSlotIsRejected() {
        UUID run=insertValidationFixture(),token=claimValidation(run);
        assertThat(validationDao().deleteExtractionLease(run,token)).isEqualTo(1);
        assertThat(validationDao().selectExecutionAllowed(run,token)).isFalse();
        assertThatThrownBy(()->validationDao().insertStep(run,token,"CLASSIFICATION_GOLDEN","PASSED","{}","c".repeat(64)))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(validationDao().selectStepList(run)).isEmpty();
    }

    @Test void expiredValidationCannotCompleteAndIsRecoveredAsExpiredFailure() {
        UUID run=insertValidationFixture(),token=UUID.randomUUID();
        // 테스트 소유 DB에서 짧은 최초 lease를 만들고 실제 DB 시각의 만료를 확인한다.
        assertThat(sql.update("UPDATE announcement_attachment_policy_validation_runs SET run_status_code='RUNNING',row_version=1,lease_token=?,lease_expires_at=clock_timestamp()+interval '100 milliseconds',started_at=clock_timestamp() WHERE id=?",token,run)).isEqualTo(1);
        sql.execute("SELECT pg_sleep(0.2)");
        assertThat(validationDao().updateFinished(run,token,"INCOMPLETE","QA_REQUIRED")).isZero();
        assertThatThrownBy(()->sql.update("UPDATE announcement_attachment_policy_validation_runs SET run_status_code='INCOMPLETE',row_version=row_version+1,lease_token=NULL,lease_expires_at=NULL,completed_at=clock_timestamp() WHERE id=?",run))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(validationDao().updateExpired()).isEqualTo(1);
        var row=validationDao().selectRunDetails(run,false);
        assertThat(row.statusCode()).isEqualTo("FAILED");assertThat(row.errorCode()).isEqualTo("LEASE_EXPIRED");
        assertThat(row.leaseToken()).isNull();assertThat(row.startedAt()).isNotNull();
    }

    @Test void fourContractProofRowsPermitOnlyCurrentVersionCompletion() {
        UUID run=insertValidationFixture(),token=claimValidation(run);
        // 아래 데이터는 trigger 계약 fixture다. 실제 네 가지 QA 실행 성공 증거가 아니다.
        for(String step:List.of("CLASSIFICATION_GOLDEN","INSTALLED_RUNTIME","PROVIDER_PROFILES","WORKER_DB_RECOVERY"))
            assertThat(validationDao().insertStep(run,token,step,"PASSED","{\"scope\":\"DB_CONTRACT_FIXTURE\"}","c".repeat(64))).isEqualTo(1);
        UUID policyId=validationDao().selectRunDetails(run,false).policyId();
        new TransactionTemplate(context.getBean(PlatformTransactionManager.class)).executeWithoutResult(tx->{
            sql.update("UPDATE announcement_attachment_policies SET row_version=row_version+1 WHERE id=?",policyId);
            assertThatThrownBy(()->validationDao().updateFinished(run,token,"VERIFIED",null)).isInstanceOf(DataIntegrityViolationException.class);
            tx.setRollbackOnly();
        });
        assertThat(validationDao().updateFinished(run,token,"VERIFIED",null)).isEqualTo(1);
        assertThat(validationDao().selectRunDetails(run,false).statusCode()).isEqualTo("VERIFIED");
        assertThat(sql.queryForObject("SELECT policy_status_code FROM announcement_attachment_policies WHERE id=?",String.class,policyId)).isEqualTo("DRAFT");
    }

    private com.saneb.domain.announcementattachment.service.AnnouncementAttachmentBatchService batchService() {
        return context.getBean(com.saneb.domain.announcementattachment.service.AnnouncementAttachmentBatchService.class);
    }
    private com.saneb.domain.announcementattachment.dto.AttachmentBatchRequests.Scope batchScope(int maximum) {
        return new com.saneb.domain.announcementattachment.dto.AttachmentBatchRequests.Scope(policy,List.of("BIZINFO"),java.time.OffsetDateTime.now().minusDays(1),java.time.OffsetDateTime.now().plusDays(1),null,null,maximum);
    }
    private com.saneb.domain.announcementattachment.dto.AttachmentBatchRequests.Reservation batchRequest(com.saneb.domain.announcementattachment.dto.AttachmentBatchRequests.Scope scope) {
        var preview=batchService().selectScopePreview(reviewActor(),scope);
        return new com.saneb.domain.announcementattachment.dto.AttachmentBatchRequests.Reservation(scope,preview.scopeHash(),"테스트 소유 배치 범위");
    }
    @Test void batchScopeIsFrozenInJobsAndCannotBeClaimedBeforeCollection() {
        UUID source=selectRequest().sourceId();insertCollectionLocator(source);var original=dao.selectSourceContextDetails(source);
        var request=batchRequest(batchScope(100));var batch=batchService().insertBatch(reviewActor(),UUID.randomUUID(),request);
        assertThat(batch.statusCode()).isEqualTo("SCOPE_READY");assertThat(batch.itemCount()).isEqualTo(1);
        assertThat(dao.selectSourceContextDetails(source)).isEqualTo(original);assertThat(service.saveNextJobClaim()).isEmpty();
        UUID added=selectRequest().sourceId();insertCollectionLocator(added);
        var items=batchService().selectItemList(reviewActor(),batch.batchId(),1,100);
        assertThat(items.totalCount()).isEqualTo(1);assertThat(items.items()).singleElement().satisfies(i->assertThat(i.sourceId()).isEqualTo(source));
        assertThat(context.getBean(SqlSessionTemplate.class).getMapper(AnnouncementAttachmentReviewDao.class).selectActiveNormalJobExists(source)).isFalse();
    }
    @Test void batchScopeCancellationKeepsHistoryAndReleasesSourceReservation() {
        UUID source=selectRequest().sourceId();insertCollectionLocator(source);
        var batch=batchService().insertBatch(reviewActor(),UUID.randomUUID(),batchRequest(batchScope(100)));
        var result=batchService().updateScopeCancellation(reviewActor(),batch.batchId(),new com.saneb.domain.announcementattachment.dto.AttachmentBatchRequests.Cancellation(0,"범위 취소"));
        assertThat(result.statusCode()).isEqualTo("CANCELLED");assertThat(result.jobCounts()).containsEntry("CANCELLED",1L);
        assertThat(result.scopeHash()).isEqualTo(batch.scopeHash());assertThat(service.insertAttachmentJob(new AttachmentJobReservation(source,policy,dao.selectSourceContextDetails(source).baseEvaluationId(),0,0,UUID.randomUUID(),EXECUTION))).isNotNull();
        assertThatThrownBy(()->sql.update("DELETE FROM announcement_attachment_batches WHERE id=?",batch.batchId())).isInstanceOf(DataIntegrityViolationException.class);
    }
    @Test void batchSourceDeletionRetainsOnlyCountAndDoesNotExpandScope() {
        UUID source=selectRequest().sourceId();insertCollectionLocator(source);var batch=batchService().insertBatch(reviewActor(),UUID.randomUUID(),batchRequest(batchScope(100)));
        sql.update("DELETE FROM announcement_source_snapshots WHERE id=?",source);
        var after=batchService().selectBatchDetails(reviewActor(),batch.batchId());
        assertThat(after.itemCount()).isEqualTo(1);assertThat(after.deletedItemCount()).isEqualTo(1);assertThat(after.remainingItemCount()).isZero();
        assertThat(new ObjectMapper().valueToTree(after.frozenScope()).toString()).doesNotContain(source.toString());
        assertThat(batchService().selectItemList(reviewActor(),batch.batchId(),1,100).items()).isEmpty();
    }
    @Test void batchSameKeyConcurrentReservationsHaveOneBatchAndJob() throws Exception {
        UUID source=selectRequest().sourceId();insertCollectionLocator(source);var request=batchRequest(batchScope(100));UUID key=UUID.randomUUID();var start=new CountDownLatch(1);
        Callable<UUID> call=()->{start.await(5,TimeUnit.SECONDS);return batchService().insertBatch(reviewActor(),key,request).batchId();};
        try(var executor=Executors.newFixedThreadPool(2)) {var a=executor.submit(call);var b=executor.submit(call);start.countDown();assertThat(a.get(10,TimeUnit.SECONDS)).isEqualTo(b.get(10,TimeUnit.SECONDS));}
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_attachment_batches",Integer.class)).isEqualTo(1);
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_attachment_jobs",Integer.class)).isEqualTo(1);
    }
    @Test void batchScopeRejectsChangedRowsAndIncompleteMaterialization() {
        UUID source=selectRequest().sourceId();insertCollectionLocator(source);var request=batchRequest(batchScope(100));
        sql.update("UPDATE announcement_source_snapshots SET classification_row_version=classification_row_version+1 WHERE id=?",source);
        assertThatThrownBy(()->batchService().insertBatch(reviewActor(),UUID.randomUUID(),request)).isInstanceOf(ApiException.class);
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_attachment_batches",Integer.class)).isZero();
        var batch=batchService().insertBatch(reviewActor(),UUID.randomUUID(),batchRequest(batchScope(100)));
        assertThatThrownBy(()->sql.update("UPDATE announcement_attachment_batches SET scope_item_count=2,row_version=row_version+1 WHERE id=?",batch.batchId())).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(()->sql.update("UPDATE announcement_attachment_jobs SET job_status_code='PENDING' WHERE batch_id=?",batch.batchId())).isInstanceOf(DataIntegrityViolationException.class);
    }

    private com.saneb.domain.announcementattachment.dto.AttachmentBatchRequests.Collection batchCollection(
            com.saneb.domain.announcementattachment.dto.AttachmentBatchResponses.Batch batch) {
        return new com.saneb.domain.announcementattachment.dto.AttachmentBatchRequests.Collection(batch.rowVersion(),batch.scopeHash(),batch.itemCount(),batch.deletedItemCount(),
                ((Number)batch.frozenScope().get("maximumDownloadBytes")).longValue(),((Number)batch.frozenScope().get("maximumHttpRequests")).longValue(),"테스트 소유 고정 수집 실행");
    }
    @Test void batchCollectionCompletesOnlyAfterSealedEvaluationAndNeverChangesCurrent() {
        UUID source=selectRequest().sourceId();insertCollectionLocator(source);var original=dao.selectSourceContextDetails(source);
        var batch=batchService().insertBatch(reviewActor(),UUID.randomUUID(),batchRequest(batchScope(100)));
        UUID later=selectRequest().sourceId();insertCollectionLocator(later);
        var started=batchService().updateCollectionStart(reviewActor(),batch.batchId(),batchCollection(batch));
        assertThat(started.statusCode()).isEqualTo("COLLECTION_PENDING");assertThat(started.itemCount()).isEqualTo(1);
        assertThat(batchService().saveCollectionProgress()).isZero();
        var job=service.saveNextJobClaim().orElseThrow();assertThat(job.sourceId()).isEqualTo(source);
        assertThat(service.selectExternalExecutionAllowed(job.jobId(),job.leaseToken())).isTrue();
        batchService().saveCollectionProgress();assertThat(batchService().selectBatchDetails(reviewActor(),batch.batchId()).statusCode()).isEqualTo("COLLECTING");
        assertThat(service.saveDownloadBytes(job.jobId(),job.leaseToken(),15)).isTrue();
        evidenceService.saveAttachmentSet(job.jobId(),job.leaseToken(),new AttachmentSetEvidence("FOUND",true,List.of(selectFileEvidence(100)))).orElseThrow();
        assertThat(batchService().saveCollectionProgress()).isZero();
        var evaluation=context.getBean(AnnouncementAttachmentEvaluationService.class).saveJobEvaluation(job.jobId(),job.leaseToken()).orElseThrow();
        assertThat(evaluation.current()).isFalse();assertThat(dao.selectSourceContextDetails(source)).isEqualTo(original);
        assertThat(batchService().saveCollectionProgress()).isEqualTo(1);
        var complete=batchService().selectBatchDetails(reviewActor(),batch.batchId());
        assertThat(complete.statusCode()).isEqualTo("COLLECTED");assertThat(complete.jobCounts()).containsEntry("SUCCEEDED",1L);
        assertThat(batchService().saveCollectionProgress()).isZero();assertThat(service.saveNextJobClaim()).isEmpty();
    }
    @Test void batchPauseBlocksNewHttpAndResumeDoesNotResetBudgetAttemptsOrApproval() {
        UUID source=selectRequest().sourceId();insertCollectionLocator(source);
        var batch=batchService().insertBatch(reviewActor(),UUID.randomUUID(),batchRequest(batchScope(100)));
        var started=batchService().updateCollectionStart(reviewActor(),batch.batchId(),batchCollection(batch));
        var job=service.saveNextJobClaim().orElseThrow();service.saveDownloadBytes(job.jobId(),job.leaseToken(),15);
        var paused=batchService().updateCollectionPause(reviewActor(),batch.batchId(),new com.saneb.domain.announcementattachment.dto.AttachmentBatchRequests.Pause(started.rowVersion(),"중지"));
        assertThat(service.selectExternalExecutionAllowed(job.jobId(),job.leaseToken())).isFalse();assertThat(service.saveNextJobClaim()).isEmpty();
        assertThat(batchService().saveCollectionProgress()).isZero();
        var resumed=batchService().updateCollectionResume(reviewActor(),batch.batchId(),batchCollection(paused));
        assertThat(resumed.statusCode()).isEqualTo("COLLECTING");assertThat(service.selectExternalExecutionAllowed(job.jobId(),job.leaseToken())).isTrue();
        var same=dao.selectJobDetails(job.jobId());assertThat(same.attemptCount()).isEqualTo(1);assertThat(same.reservedDownloadBytes()).isEqualTo(15L);
        assertThat(same.leaseToken()).isEqualTo(job.leaseToken());
        assertThatThrownBy(()->batchService().updateCollectionResume(reviewActor(),batch.batchId(),batchCollection(paused))).isInstanceOf(ApiException.class);
        assertThatThrownBy(()->sql.update("UPDATE announcement_attachment_batches SET collection_approval_hash=repeat('f',64),row_version=row_version+1 WHERE id=?",batch.batchId())).isInstanceOf(DataIntegrityViolationException.class);
    }
    @Test void batchStartRejectsLocatorMutationAndCannotBypassApprovalUsingSql() {
        UUID source=selectRequest().sourceId();insertCollectionLocator(source);
        var batch=batchService().insertBatch(reviewActor(),UUID.randomUUID(),batchRequest(batchScope(100)));
        sql.update("UPDATE announcement_source_snapshots SET source_url='https://www.bizinfo.go.kr/changed' WHERE id=?",source);
        assertThatThrownBy(()->batchService().updateCollectionStart(reviewActor(),batch.batchId(),batchCollection(batch))).isInstanceOf(ApiException.class);
        assertThat(batchService().selectBatchDetails(reviewActor(),batch.batchId()).jobCounts()).containsEntry("SCOPE_READY",1L);
        assertThatThrownBy(()->sql.update("UPDATE announcement_attachment_batches SET batch_status_code='COLLECTING',row_version=row_version+1 WHERE id=?",batch.batchId())).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(()->sql.update("UPDATE announcement_attachment_jobs SET frozen_locator_hash=repeat('f',64) WHERE batch_id=?",batch.batchId())).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(()->sql.update("UPDATE announcement_attachment_jobs SET previous_is_review_required=true WHERE batch_id=?",batch.batchId())).isInstanceOf(DataIntegrityViolationException.class);
    }
    @Test void changedLocatorAfterStartTerminatesAsConflictInsteadOfUnboundedDeferredRetries() {
        UUID source=selectRequest().sourceId();insertCollectionLocator(source);
        var batch=batchService().insertBatch(reviewActor(),UUID.randomUUID(),batchRequest(batchScope(100)));
        batchService().updateCollectionStart(reviewActor(),batch.batchId(),batchCollection(batch));
        sql.update("UPDATE announcement_source_snapshots SET source_url='https://www.bizinfo.go.kr/changed' WHERE id=?",source);
        assertThat(service.saveNextJobClaim()).isEmpty();batchService().saveCollectionProgress();
        var result=batchService().selectBatchDetails(reviewActor(),batch.batchId());
        assertThat(result.statusCode()).isEqualTo("COLLECTION_PARTIAL_FAILED");assertThat(result.jobCounts()).containsEntry("CONFLICT",1L);
        assertThat(batchService().selectItemList(reviewActor(),batch.batchId(),1,100).items().getFirst().errorCode()).isEqualTo("FROZEN_INPUT_CHANGED");
        assertThat(sql.queryForObject("SELECT reserved_download_bytes FROM announcement_attachment_jobs WHERE batch_id=?",Long.class,batch.batchId())).isZero();
    }
    @Test void pausedBatchDeletedItemsRemainExplicitAndNeverBecomeCollectedSuccess() {
        UUID source=selectRequest().sourceId();insertCollectionLocator(source);
        var batch=batchService().insertBatch(reviewActor(),UUID.randomUUID(),batchRequest(batchScope(100)));
        var started=batchService().updateCollectionStart(reviewActor(),batch.batchId(),batchCollection(batch));
        var paused=batchService().updateCollectionPause(reviewActor(),batch.batchId(),new com.saneb.domain.announcementattachment.dto.AttachmentBatchRequests.Pause(started.rowVersion(),"중지"));
        sql.update("DELETE FROM announcement_source_snapshots WHERE id=?",source);
        assertThat(batchService().saveCollectionProgress()).isZero();
        assertThatThrownBy(()->batchService().updateCollectionResume(reviewActor(),batch.batchId(),batchCollection(paused))).isInstanceOf(ApiException.class);
        var current=batchService().selectBatchDetails(reviewActor(),batch.batchId());
        batchService().updateCollectionResume(reviewActor(),batch.batchId(),batchCollection(current));batchService().saveCollectionProgress();
        var result=batchService().selectBatchDetails(reviewActor(),batch.batchId());
        assertThat(result.statusCode()).isEqualTo("COLLECTION_PARTIAL_FAILED");assertThat(result.itemCount()).isEqualTo(1);assertThat(result.deletedItemCount()).isEqualTo(1);
        assertThat(result.remainingItemCount()).isZero();
    }
    @Test void batchConcurrentCollectionStartsActivateFrozenJobsExactlyOnce() throws Exception {
        UUID source=selectRequest().sourceId();insertCollectionLocator(source);
        var batch=batchService().insertBatch(reviewActor(),UUID.randomUUID(),batchRequest(batchScope(100)));var request=batchCollection(batch);
        var gate=new CountDownLatch(1);Callable<Boolean> call=()->{gate.await(5,TimeUnit.SECONDS);try {batchService().updateCollectionStart(reviewActor(),batch.batchId(),request);return true;}catch(ApiException conflict){return false;}};
        try(var executor=Executors.newFixedThreadPool(2)) {var a=executor.submit(call);var b=executor.submit(call);gate.countDown();assertThat(List.of(a.get(10,TimeUnit.SECONDS),b.get(10,TimeUnit.SECONDS))).containsExactlyInAnyOrder(true,false);}
        assertThat(batchService().selectBatchDetails(reviewActor(),batch.batchId()).jobCounts()).containsEntry("PENDING",1L);
        assertThat(sql.queryForObject("SELECT row_version FROM announcement_attachment_jobs WHERE batch_id=?",Integer.class,batch.batchId())).isEqualTo(1);
    }

    private com.saneb.domain.announcementattachment.service.AnnouncementAttachmentBatchPreviewService batchPreviewService() {
        return context.getBean(com.saneb.domain.announcementattachment.service.AnnouncementAttachmentBatchPreviewService.class);
    }
    private com.saneb.domain.announcementattachment.dto.AttachmentBatchResponses.Batch completeBatchForPreview() {
        return completeBatchForPreview(selectRequest().sourceId());
    }
    private com.saneb.domain.announcementattachment.dto.AttachmentBatchResponses.Batch completeBatchForPreview(UUID source) {
        insertCollectionLocator(source);
        var batch=batchService().insertBatch(reviewActor(),UUID.randomUUID(),batchRequest(batchScope(100)));
        batchService().updateCollectionStart(reviewActor(),batch.batchId(),batchCollection(batch));
        var job=service.saveNextJobClaim().orElseThrow();service.saveDownloadBytes(job.jobId(),job.leaseToken(),15);
        evidenceService.saveAttachmentSet(job.jobId(),job.leaseToken(),new AttachmentSetEvidence("FOUND",true,List.of(selectFileEvidence(100)))).orElseThrow();
        context.getBean(AnnouncementAttachmentEvaluationService.class).saveJobEvaluation(job.jobId(),job.leaseToken()).orElseThrow();
        batchService().saveCollectionProgress();return batchService().selectBatchDetails(reviewActor(),batch.batchId());
    }
    private com.saneb.domain.announcementattachment.dto.AttachmentBatchPreviewRequests.Preparation batchPreviewRequest(
            com.saneb.domain.announcementattachment.dto.AttachmentBatchResponses.Batch batch) {
        return new com.saneb.domain.announcementattachment.dto.AttachmentBatchPreviewRequests.Preparation(batch.rowVersion(),batch.scopeHash(),"테스트 소유 봉인 미리보기");
    }
    @Test void batchPreviewAndSelectionFreezeEvidenceWithoutChangingCurrentOrOldHistory() {
        var batch=completeBatchForPreview();var job=batchService().selectItemList(reviewActor(),batch.batchId(),1,100).items().getFirst();
        var before=dao.selectSourceContextDetails(job.sourceId());UUID key=UUID.randomUUID();var request=batchPreviewRequest(batch);
        var preview=batchPreviewService().insertPreview(reviewActor(),batch.batchId(),key,request);
        assertThat(preview.statusCode()).isEqualTo("PREVIEW_READY");assertThat(preview.selectedItemCount()).isZero();assertThat(preview.inputsCurrent()).isTrue();
        var item=batchPreviewService().selectItemList(reviewActor(),batch.batchId(),preview.previewId(),1,100).items().getFirst();
        assertThat(item.eligible()).isTrue();assertThat(item.evidence()).containsEntry("fileCount",1).containsEntry("downloadFailedCount",0);
        var selected=batchPreviewService().updateSelection(reviewActor(),batch.batchId(),UUID.randomUUID(),new com.saneb.domain.announcementattachment.dto.AttachmentBatchPreviewRequests.Selection(
                preview.currentBatchVersion(),preview.previewHash(),List.of(job.jobId()),"명시적 선택"));
        assertThat(selected.selectedItemCount()).isEqualTo(1);assertThat(selected.inputHash()).isEqualTo(preview.inputHash());assertThat(selected.previewHash()).isNotEqualTo(preview.previewHash());
        assertThat(selected.inputsCurrent()).isTrue();assertThat(dao.selectSourceContextDetails(job.sourceId())).isEqualTo(before);
        assertThat(sql.queryForObject("SELECT is_selected_for_application FROM announcement_attachment_jobs WHERE id=?",Boolean.class,job.jobId())).isTrue();
        assertThat(batchPreviewService().selectItemList(reviewActor(),batch.batchId(),preview.previewId(),1,100).items().getFirst().selected()).isFalse();
        assertThat(batchPreviewService().insertPreview(reviewActor(),batch.batchId(),key,request).currentPreview()).isFalse();
        assertThat(batchPreviewService().selectCurrentPreviewDetails(reviewActor(),batch.batchId()).previewId()).isEqualTo(selected.previewId());
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_attachment_batch_previews WHERE batch_id=?",Integer.class,batch.batchId())).isEqualTo(2);
    }
    @Test void batchPreviewRejectsChangedSourceOnSelectionButKeepsConflictVisibleInNewPreview() {
        var batch=completeBatchForPreview();var preview=batchPreviewService().insertPreview(reviewActor(),batch.batchId(),UUID.randomUUID(),batchPreviewRequest(batch));
        var item=batchPreviewService().selectItemList(reviewActor(),batch.batchId(),preview.previewId(),1,100).items().getFirst();
        sql.update("UPDATE announcement_source_snapshots SET classification_row_version=classification_row_version+1 WHERE id=?",item.sourceId());
        assertThat(batchPreviewService().selectCurrentPreviewDetails(reviewActor(),batch.batchId()).inputsCurrent()).isFalse();
        assertThatThrownBy(()->batchPreviewService().updateSelection(reviewActor(),batch.batchId(),UUID.randomUUID(),new com.saneb.domain.announcementattachment.dto.AttachmentBatchPreviewRequests.Selection(
                preview.currentBatchVersion(),preview.previewHash(),List.of(item.jobId()),"변경 뒤 선택"))).isInstanceOf(ApiException.class);
        var current=batchService().selectBatchDetails(reviewActor(),batch.batchId());
        var changed=batchPreviewService().insertPreview(reviewActor(),batch.batchId(),UUID.randomUUID(),batchPreviewRequest(current));
        assertThat(changed.statusCode()).isEqualTo("PREVIEW_PARTIAL_FAILED");assertThat(changed.eligibleItemCount()).isZero();
        assertThat(batchPreviewService().selectItemList(reviewActor(),batch.batchId(),changed.previewId(),1,100).items().getFirst().readinessCode()).isEqualTo("SOURCE_CHANGED");
    }
    @Test void batchPreviewSourceDeletionCascadesOnlyItemMetadataAndInvalidatesSnapshot() {
        var batch=completeBatchForPreview();var preview=batchPreviewService().insertPreview(reviewActor(),batch.batchId(),UUID.randomUUID(),batchPreviewRequest(batch));
        var item=batchPreviewService().selectItemList(reviewActor(),batch.batchId(),preview.previewId(),1,100).items().getFirst();
        assertThatThrownBy(()->sql.update("DELETE FROM announcement_attachment_batch_preview_items WHERE preview_id=?",preview.previewId())).isInstanceOf(DataIntegrityViolationException.class);
        sql.update("DELETE FROM announcement_source_snapshots WHERE id=?",item.sourceId());
        var after=batchPreviewService().selectPreviewDetails(reviewActor(),batch.batchId(),preview.previewId());
        assertThat(after.snapshotRemainingItemCount()).isEqualTo(1);assertThat(after.availableItemCount()).isZero();assertThat(after.currentDeletedItemCount()).isEqualTo(1);assertThat(after.inputsCurrent()).isFalse();
        assertThat(batchPreviewService().selectItemList(reviewActor(),batch.batchId(),preview.previewId(),1,100).items()).isEmpty();
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_attachment_batch_previews WHERE batch_id=?",Integer.class,batch.batchId())).isEqualTo(1);
        assertThatThrownBy(()->sql.update("DELETE FROM announcement_attachment_batch_previews WHERE id=?",preview.previewId())).isInstanceOf(DataIntegrityViolationException.class);
    }
    @Test void batchPreviewHistoryAndJobSelectionCannotBeEditedOutsideNewSnapshot() {
        var batch=completeBatchForPreview();var preview=batchPreviewService().insertPreview(reviewActor(),batch.batchId(),UUID.randomUUID(),batchPreviewRequest(batch));
        assertThatThrownBy(()->sql.update("UPDATE announcement_attachment_batch_previews SET selected_item_count=1 WHERE id=?",preview.previewId())).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(()->sql.update("UPDATE announcement_attachment_batch_preview_items SET is_selected=true WHERE preview_id=?",preview.previewId())).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(()->sql.update("UPDATE announcement_attachment_jobs SET is_selected_for_application=true,row_version=row_version+1 WHERE batch_id=?",batch.batchId())).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(()->sql.update("UPDATE announcement_attachment_batches SET preview_hash=repeat('f',64),row_version=row_version+1 WHERE id=?",batch.batchId())).isInstanceOf(DataIntegrityViolationException.class);
    }
    @Test void batchPreviewConcurrentSameKeyCreatesOneFullSnapshot() throws Exception {
        var batch=completeBatchForPreview();UUID key=UUID.randomUUID();var request=batchPreviewRequest(batch);var gate=new CountDownLatch(1);
        Callable<UUID> call=()->{gate.await(5,TimeUnit.SECONDS);return batchPreviewService().insertPreview(reviewActor(),batch.batchId(),key,request).previewId();};
        try(var executor=Executors.newFixedThreadPool(2)) {var a=executor.submit(call);var b=executor.submit(call);gate.countDown();assertThat(a.get(10,TimeUnit.SECONDS)).isEqualTo(b.get(10,TimeUnit.SECONDS));}
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_attachment_batch_previews WHERE batch_id=?",Integer.class,batch.batchId())).isEqualTo(1);
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_attachment_batch_preview_items WHERE batch_id=?",Integer.class,batch.batchId())).isEqualTo(1);
    }
    @Test void batchPreviewPreservesFailedCollectionItemAndRejectsItsSelection() {
        UUID source=selectRequest().sourceId();insertCollectionLocator(source);var batch=batchService().insertBatch(reviewActor(),UUID.randomUUID(),batchRequest(batchScope(100)));
        batchService().updateCollectionStart(reviewActor(),batch.batchId(),batchCollection(batch));var job=service.saveNextJobClaim().orElseThrow();
        service.saveJobFailure(job.jobId(),job.leaseToken(),AttachmentFailureCode.ISOLATION_UNAVAILABLE);batchService().saveCollectionProgress();
        batch=batchService().selectBatchDetails(reviewActor(),batch.batchId());var preview=batchPreviewService().insertPreview(reviewActor(),batch.batchId(),UUID.randomUUID(),batchPreviewRequest(batch));
        assertThat(preview.statusCode()).isEqualTo("PREVIEW_PARTIAL_FAILED");assertThat(preview.eligibleItemCount()).isZero();assertThat(preview.availableItemCount()).isEqualTo(1);
        var fixedBatch=batch;
        assertThatThrownBy(()->batchPreviewService().updateSelection(reviewActor(),fixedBatch.batchId(),UUID.randomUUID(),new com.saneb.domain.announcementattachment.dto.AttachmentBatchPreviewRequests.Selection(
                preview.currentBatchVersion(),preview.previewHash(),List.of(job.jobId()),"실패 항목 선택"))).isInstanceOf(ApiException.class);
        assertThat(batchPreviewService().selectItemList(reviewActor(),batch.batchId(),preview.previewId(),1,100).items().getFirst().evidence()).containsEntry("jobErrorCode","ISOLATION_UNAVAILABLE");
    }

    private com.saneb.domain.announcementattachment.service.AnnouncementAttachmentBatchApplicationService batchApplicationService() {
        return context.getBean(com.saneb.domain.announcementattachment.service.AnnouncementAttachmentBatchApplicationService.class);
    }
    private com.saneb.domain.announcementattachment.service.AnnouncementAttachmentBatchHistoryService batchHistoryService() {
        return context.getBean(com.saneb.domain.announcementattachment.service.AnnouncementAttachmentBatchHistoryService.class);
    }
    @Test void approvalHistoryPagesStayCompleteWhileLaterApprovalsAdvanceTheBatch() {
        var preview=prepareApplicationPreview();UUID batchId=preview.batchId();
        var ids=new java.util.HashSet<UUID>();ids.add(batchApplicationService().insertAction(reviewActor(),batchId,UUID.randomUUID(),"START",applicationRequest(preview)).actionId());
        for(int i=0;i<12;i++)for(String action:List.of("PAUSE","RESUME"))
            ids.add(batchApplicationService().insertAction(reviewActor(),batchId,UUID.randomUUID(),action,applicationRequest(preview)).actionId());
        var first=batchHistoryService().selectActionList(reviewActor(),batchId,null,1,10);assertThat(first.history().totalCount()).isEqualTo(25);
        batchApplicationService().insertAction(reviewActor(),batchId,UUID.randomUUID(),"PAUSE",applicationRequest(preview));
        batchApplicationService().insertAction(reviewActor(),batchId,UUID.randomUUID(),"RESUME",applicationRequest(preview));
        var seen=new java.util.ArrayList<UUID>();
        for(int page=1;page<=3;page++) {
            var result=batchHistoryService().selectActionList(reviewActor(),batchId,first.throughVersion(),page,10);
            assertThat(result.history().totalCount()).isEqualTo(25);assertThat(result.throughVersion()).isEqualTo(first.throughVersion());
            assertThat(result.currentBatchVersion()).isGreaterThan(first.throughVersion());
            seen.addAll(result.history().items().stream().map(com.saneb.domain.announcementattachment.dto.AttachmentBatchHistoryResponses.Entry::actionId).toList());
        }
        assertThat(seen).hasSize(25).doesNotHaveDuplicates().containsExactlyInAnyOrderElementsOf(ids);
        assertThat(batchHistoryService().selectActionList(reviewActor(),batchId,null,1,100).history().totalCount()).isEqualTo(27);
        assertThat(batchService().selectBatchDetails(reviewActor(),batchId).statusCode()).isEqualTo("APPLYING");
    }
    @Test void sourceDeletionDoesNotRewriteOrHideOriginalApprovalHistory() {
        var preview=prepareApplicationPreview();UUID batchId=preview.batchId();var action=batchApplicationService().insertAction(reviewActor(),batchId,UUID.randomUUID(),"START",applicationRequest(preview));
        var before=batchHistoryService().selectActionList(reviewActor(),batchId,null,1,20);
        var item=batchService().selectItemList(reviewActor(),batchId,1,100).items().getFirst();sql.update("DELETE FROM announcement_source_snapshots WHERE id=?",item.sourceId());
        var after=batchHistoryService().selectActionList(reviewActor(),batchId,before.throughVersion(),1,20);
        assertThat(after.history()).isEqualTo(before.history());assertThat(after.history().items().getFirst().actionId()).isEqualTo(action.actionId());
        assertThat(after.history().items().getFirst().deletedCountAtAcceptance()).isZero();assertThat(batchService().selectBatchDetails(reviewActor(),batchId).deletedItemCount()).isEqualTo(1);
    }
    @Test void mixedApplicationAndRollbackHistoryRetainsOriginalImpactAfterCompletion() {
        var preview=prepareApplicationPreview();UUID batchId=preview.batchId();var app=batchApplicationService().insertAction(reviewActor(),batchId,UUID.randomUUID(),"START",applicationRequest(preview));
        assertThat(batchApplicationService().saveNextApplication()).isTrue();var p=batchRollbackService().selectPreviewDetails(reviewActor(),batchId);
        var rollback=batchRollbackService().insertRollback(reviewActor(),batchId,UUID.randomUUID(),rollbackRequest(p));
        var before=batchHistoryService().selectActionList(reviewActor(),batchId,null,1,20);assertThat(before.history().totalCount()).isEqualTo(2);
        assertThat(before.history().items()).extracting(com.saneb.domain.announcementattachment.dto.AttachmentBatchHistoryResponses.Entry::actionKind).containsExactly("ROLLBACK","APPLICATION");
        assertThat(before.history().items()).extracting(com.saneb.domain.announcementattachment.dto.AttachmentBatchHistoryResponses.Entry::actionId).containsExactly(rollback.actionId(),app.actionId());
        assertThat(before.history().items().getFirst().approvedBaseReopenCount()).isEqualTo(p.baseReopenCount());
        assertThat(batchRollbackService().saveNextRollback()).isTrue();
        assertThat(batchHistoryService().selectActionList(reviewActor(),batchId,before.throughVersion(),1,20).history()).isEqualTo(before.history());
        assertThat(batchRollbackService().selectActionDetails(reviewActor(),batchId,rollback.actionId()).statusCode()).isEqualTo("ROLLED_BACK");
    }
    private com.saneb.domain.announcementattachment.dto.AttachmentBatchPreviewResponses.Preview prepareApplicationPreview() {
        updateFixturePolicyMode("ENFORCE");var batch=completeBatchForPreview();
        var preview=batchPreviewService().insertPreview(reviewActor(),batch.batchId(),UUID.randomUUID(),batchPreviewRequest(batch));
        var job=batchService().selectItemList(reviewActor(),batch.batchId(),1,100).items().getFirst();
        return batchPreviewService().updateSelection(reviewActor(),batch.batchId(),UUID.randomUUID(),new com.saneb.domain.announcementattachment.dto.AttachmentBatchPreviewRequests.Selection(
                preview.currentBatchVersion(),preview.previewHash(),List.of(job.jobId()),"검증 소유 명시적 적용 선택"));
    }
    private com.saneb.domain.announcementattachment.dto.AttachmentBatchApplicationRequest applicationRequest(
            com.saneb.domain.announcementattachment.dto.AttachmentBatchPreviewResponses.Preview preview) {
        var batch=batchService().selectBatchDetails(reviewActor(),preview.batchId());
        return new com.saneb.domain.announcementattachment.dto.AttachmentBatchApplicationRequest(batch.rowVersion(),preview.previewId(),preview.previewHash(),
                batch.itemCount(),preview.selectedItemCount(),batch.deletedItemCount(),true,"테스트 소유 명시적 적용");
    }
    @Test void batchApplicationQueuesThenAppliesExactCurrentWithoutCreatingOperationalAnnouncement() {
        var preview=prepareApplicationPreview();var item=batchService().selectItemList(reviewActor(),preview.batchId(),1,100).items().getFirst();
        var before=dao.selectSourceContextDetails(item.sourceId());UUID key=UUID.randomUUID();var request=applicationRequest(preview);
        var action=batchApplicationService().insertAction(reviewActor(),preview.batchId(),key,"START",request);
        assertThat(action.appliedCount()).isZero();assertThat(action.pendingCount()).isEqualTo(1);assertThat(dao.selectSourceContextDetails(item.sourceId())).isEqualTo(before);
        assertThat(batchApplicationService().saveNextApplication()).isTrue();var after=dao.selectSourceContextDetails(item.sourceId());
        assertThat(after.attachmentVersion()).isEqualTo(before.attachmentVersion()+1);assertThat(after.sourceVersion()).isEqualTo(before.sourceVersion());
        assertThat(sql.queryForObject("SELECT is_attachment_review_required FROM announcement_source_snapshots WHERE id=?",Boolean.class,item.sourceId())).isTrue();
        assertThat(sql.queryForObject("SELECT applied_input_hash FROM announcement_attachment_jobs WHERE id=?",String.class,item.jobId())).matches("[0-9a-f]{64}");
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_source_links WHERE source_id=?",Integer.class,item.sourceId())).isZero();
        var again=batchApplicationService().insertAction(reviewActor(),preview.batchId(),key,"START",request);
        assertThat(again.actionId()).isEqualTo(action.actionId());assertThat(again.currentStatusCode()).isEqualTo("APPLIED");assertThat(again.appliedCount()).isEqualTo(1);
        assertThat(batchApplicationService().saveNextApplication()).isFalse();assertThat(dao.selectSourceContextDetails(item.sourceId())).isEqualTo(after);
    }
    @Test void batchApplicationChangedInputIsTerminalConflictNotSilentRepreview() {
        var preview=prepareApplicationPreview();var item=batchService().selectItemList(reviewActor(),preview.batchId(),1,100).items().getFirst();
        batchApplicationService().insertAction(reviewActor(),preview.batchId(),UUID.randomUUID(),"START",applicationRequest(preview));
        sql.update("UPDATE announcement_source_snapshots SET attachment_row_version=attachment_row_version+1 WHERE id=?",item.sourceId());
        var before=dao.selectSourceContextDetails(item.sourceId());assertThat(batchApplicationService().saveNextApplication()).isTrue();
        assertThat(dao.selectSourceContextDetails(item.sourceId())).isEqualTo(before);
        assertThat(sql.queryForObject("SELECT application_status_code FROM announcement_attachment_jobs WHERE id=?",String.class,item.jobId())).isEqualTo("CONFLICT");
        assertThat(batchService().selectBatchDetails(reviewActor(),preview.batchId()).statusCode()).isEqualTo("APPLY_PARTIAL_FAILED");
    }
    @Test void batchApplicationPauseResumeRequiresExplicitActionAndPreservesApprovedSelection() {
        var preview=prepareApplicationPreview();batchApplicationService().insertAction(reviewActor(),preview.batchId(),UUID.randomUUID(),"START",applicationRequest(preview));
        var pause=batchApplicationService().insertAction(reviewActor(),preview.batchId(),UUID.randomUUID(),"PAUSE",applicationRequest(preview));
        assertThat(batchApplicationService().saveNextApplication()).isFalse();assertThat(pause.pendingCount()).isEqualTo(1);
        batchApplicationService().insertAction(reviewActor(),preview.batchId(),UUID.randomUUID(),"RESUME",applicationRequest(preview));
        assertThat(batchApplicationService().saveNextApplication()).isTrue();assertThat(batchService().selectBatchDetails(reviewActor(),preview.batchId()).statusCode()).isEqualTo("APPLIED");
    }
    @Test void batchConcurrentApplicationProcessesOneSelectedItemExactlyOnce() throws Exception {
        var preview=prepareApplicationPreview();var item=batchService().selectItemList(reviewActor(),preview.batchId(),1,100).items().getFirst();var before=dao.selectSourceContextDetails(item.sourceId());
        batchApplicationService().insertAction(reviewActor(),preview.batchId(),UUID.randomUUID(),"START",applicationRequest(preview));
        var gate=new CountDownLatch(1);Callable<Boolean> call=()->{gate.await(5,TimeUnit.SECONDS);return batchApplicationService().saveNextApplication();};
        try(var executor=Executors.newFixedThreadPool(2)) {var a=executor.submit(call);var b=executor.submit(call);gate.countDown();assertThat(List.of(a.get(10,TimeUnit.SECONDS),b.get(10,TimeUnit.SECONDS))).containsExactlyInAnyOrder(true,false);}
        assertThat(dao.selectSourceContextDetails(item.sourceId()).attachmentVersion()).isEqualTo(before.attachmentVersion()+1);
    }
    @Test void batchApplicationDeletedSelectedItemRemainsInOriginalScopeAndCannotBecomeWholeSuccess() {
        var preview=prepareApplicationPreview();var item=batchService().selectItemList(reviewActor(),preview.batchId(),1,100).items().getFirst();
        var action=batchApplicationService().insertAction(reviewActor(),preview.batchId(),UUID.randomUUID(),"START",applicationRequest(preview));
        sql.update("DELETE FROM announcement_source_snapshots WHERE id=?",item.sourceId());assertThat(batchApplicationService().saveNextApplication()).isFalse();
        var result=batchApplicationService().selectActionDetails(reviewActor(),preview.batchId(),action.actionId());
        assertThat(result.currentStatusCode()).isEqualTo("APPLY_PARTIAL_FAILED");assertThat(result.scopeItemCount()).isEqualTo(1);assertThat(result.approvedSelectedCount()).isEqualTo(1);
        assertThat(result.deletedItemCount()).isEqualTo(1);assertThat(result.remainingItemCount()).isZero();assertThat(result.appliedCount()).isZero();
    }
    @Test void batchApplicationExpiresPriorConfirmationButPreservesItsTagsAndBlocksOldConversion() {
        UUID source=insertReviewFixture(false);var confirmed=reviewService().insertConfirmation(reviewActor(),source,UUID.randomUUID(),selectReviewRequest(source));
        var oldConversion=new AttachmentReviewRequests.Conversion(reviewService().selectReviewContextDetails(source).version(),confirmed.confirmationId(),"BUSINESS",null);
        int tags=sql.queryForObject("SELECT count(1) FROM announcement_source_attachment_tags WHERE confirmation_id=?",Integer.class,confirmed.confirmationId());
        var batch=completeBatchForPreview(source);var preview=batchPreviewService().insertPreview(reviewActor(),batch.batchId(),UUID.randomUUID(),batchPreviewRequest(batch));
        var item=batchService().selectItemList(reviewActor(),batch.batchId(),1,100).items().getFirst();
        var selected=batchPreviewService().updateSelection(reviewActor(),batch.batchId(),UUID.randomUUID(),new com.saneb.domain.announcementattachment.dto.AttachmentBatchPreviewRequests.Selection(
                preview.currentBatchVersion(),preview.previewHash(),List.of(item.jobId()),"확인 보존 후 명시적 적용"));
        assertThat(sql.queryForObject("SELECT is_current FROM announcement_source_attachment_confirmations WHERE id=?",Boolean.class,confirmed.confirmationId())).isTrue();
        batchApplicationService().insertAction(reviewActor(),batch.batchId(),UUID.randomUUID(),"START",applicationRequest(selected));batchApplicationService().saveNextApplication();
        assertThat(sql.queryForObject("SELECT is_current FROM announcement_source_attachment_confirmations WHERE id=?",Boolean.class,confirmed.confirmationId())).isFalse();
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_source_attachment_tags WHERE confirmation_id=?",Integer.class,confirmed.confirmationId())).isEqualTo(tags);
        assertThat(sql.queryForObject("SELECT previous_confirmation_id FROM announcement_attachment_jobs WHERE id=?",UUID.class,item.jobId())).isEqualTo(confirmed.confirmationId());
        assertThatThrownBy(()->reviewService().insertOperationalAnnouncement(reviewActor(),source,oldConversion)).isInstanceOf(ApiException.class);
    }
    // 이 fixture는 DB 복구 근거 계약만 검증한다. 승인 API/전체 batch 원복 worker의 성공 증거가 아니다.
    private record ConfirmationRestorationFixture(UUID source,UUID job,com.saneb.domain.announcementattachment.dto.AttachmentReviewResponses.Confirmation confirmation) { }
    private com.saneb.domain.announcementattachment.service.AnnouncementAttachmentBatchRollbackService batchRollbackService() {
        return context.getBean(com.saneb.domain.announcementattachment.service.AnnouncementAttachmentBatchRollbackService.class);
    }
    private com.saneb.domain.announcementattachment.dto.AttachmentBatchRollbackRequest rollbackRequest(com.saneb.domain.announcementattachment.dto.AttachmentBatchRollbackResponses.Preview p) {
        return new com.saneb.domain.announcementattachment.dto.AttachmentBatchRollbackRequest(p.version(),p.previewHash(),p.scopeCount(),p.targetCount(),p.deletedCount(),p.baseReopenCount(),p.confirmationRestoreCount(),p.cancelPendingCount(),true,"테스트 소유 원복 영향 승인");
    }
    @Test void rollbackRestoresBaseBindingWithoutCreatingDraftOrDeletingEvidence() {
        var selected=prepareApplicationPreview();batchApplicationService().insertAction(reviewActor(),selected.batchId(),UUID.randomUUID(),"START",applicationRequest(selected));batchApplicationService().saveNextApplication();
        var p=batchRollbackService().selectPreviewDetails(reviewActor(),selected.batchId());assertThat(p.baseReopenCount()).isEqualTo(1);assertThat(p.confirmationRestoreCount()).isZero();
        var item=batchService().selectItemList(reviewActor(),selected.batchId(),1,100).items().getFirst();var before=dao.selectSourceContextDetails(item.sourceId());
        UUID key=UUID.randomUUID();var request=rollbackRequest(p);var receipt=batchRollbackService().insertRollback(reviewActor(),p.batchId(),key,request);
        assertThat(receipt.statusCode()).isEqualTo("ROLLING_BACK");assertThat(receipt.rolledBackCount()).isZero();
        assertThat(batchRollbackService().saveNextRollback()).isTrue();var after=dao.selectSourceContextDetails(item.sourceId());
        assertThat(after.attachmentVersion()).isEqualTo(before.attachmentVersion()+1);assertThat(after.sourceVersion()).isEqualTo(before.sourceVersion());
        assertThat(sql.queryForObject("SELECT is_attachment_review_required FROM announcement_source_snapshots WHERE id=?",Boolean.class,item.sourceId())).isFalse();
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_source_attachment_evaluations WHERE source_id=?",Integer.class,item.sourceId())).isPositive();
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_source_links WHERE source_id=?",Integer.class,item.sourceId())).isZero();
        assertThat(batchRollbackService().insertRollback(reviewActor(),p.batchId(),key,request).statusCode()).isEqualTo("ROLLED_BACK");
        assertThat(batchRollbackService().saveNextRollback()).isFalse();
    }
    @Test void rollbackApprovalDoesNotOverwriteReviewAddedBeforeWorkerRuns() {
        var f=prepareConfirmationRestoration(false);UUID batchId=sql.queryForObject("SELECT batch_id FROM announcement_attachment_jobs WHERE id=?",UUID.class,f.job());
        var p=batchRollbackService().selectPreviewDetails(reviewActor(),batchId);var receipt=batchRollbackService().insertRollback(reviewActor(),batchId,UUID.randomUUID(),rollbackRequest(p));
        var newer=reviewService().insertConfirmation(reviewActor(),f.source(),UUID.randomUUID(),selectReviewRequest(f.source()));var before=dao.selectSourceContextDetails(f.source());
        assertThat(batchRollbackService().saveNextRollback()).isTrue();assertThat(dao.selectSourceContextDetails(f.source())).isEqualTo(before);
        assertThat(batchRollbackService().selectActionDetails(reviewActor(),batchId,receipt.actionId()).conflictCount()).isEqualTo(1);
        assertThat(reviewService().selectReviewContextDetails(f.source()).confirmedClassification().confirmation().confirmationId()).isEqualTo(newer.confirmationId());
    }
    @Test void rollbackConcurrentWorkersRestoreExactlyOnce() throws Exception {
        var f=prepareConfirmationRestoration(false);UUID batchId=sql.queryForObject("SELECT batch_id FROM announcement_attachment_jobs WHERE id=?",UUID.class,f.job());
        var p=batchRollbackService().selectPreviewDetails(reviewActor(),batchId);batchRollbackService().insertRollback(reviewActor(),batchId,UUID.randomUUID(),rollbackRequest(p));
        var before=dao.selectSourceContextDetails(f.source());var gate=new CountDownLatch(1);Callable<Boolean> call=()->{gate.await(5,TimeUnit.SECONDS);return batchRollbackService().saveNextRollback();};
        try(var pool=Executors.newFixedThreadPool(2)){var a=pool.submit(call);var b=pool.submit(call);gate.countDown();assertThat((a.get(15,TimeUnit.SECONDS)?1:0)+(b.get(15,TimeUnit.SECONDS)?1:0)).isEqualTo(1);}
        assertThat(dao.selectSourceContextDetails(f.source()).attachmentVersion()).isEqualTo(before.attachmentVersion()+1);
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_attachment_confirmation_restorations WHERE job_id=?",Integer.class,f.job())).isEqualTo(1);
    }
    @Test void deletedRollbackTargetPreservesOriginalApprovalCountsAndEndsPartial() {
        var f=prepareConfirmationRestoration(false);UUID batchId=sql.queryForObject("SELECT batch_id FROM announcement_attachment_jobs WHERE id=?",UUID.class,f.job());
        var p=batchRollbackService().selectPreviewDetails(reviewActor(),batchId);var receipt=batchRollbackService().insertRollback(reviewActor(),batchId,UUID.randomUUID(),rollbackRequest(p));
        sql.update("DELETE FROM announcement_source_snapshots WHERE id=?",f.source());assertThat(batchRollbackService().saveNextRollback()).isFalse();
        var after=batchRollbackService().selectActionDetails(reviewActor(),batchId,receipt.actionId());assertThat(after.approvedTargetCount()).isEqualTo(1);
        assertThat(after.remainingTargetCount()).isZero();assertThat(after.deletedCount()).isEqualTo(1);assertThat(after.statusCode()).isEqualTo("ROLLBACK_PARTIAL_FAILED");
    }
    @Test void stalePriorConfirmationStaysStaleWhileItsPreviousEvaluationCanBeRestored() {
        var f=prepareConfirmationRestoration(true);UUID batchId=sql.queryForObject("SELECT batch_id FROM announcement_attachment_jobs WHERE id=?",UUID.class,f.job());
        var p=batchRollbackService().selectPreviewDetails(reviewActor(),batchId);assertThat(p.staleConfirmationCount()).isEqualTo(1);assertThat(p.confirmationRestoreCount()).isZero();
        batchRollbackService().insertRollback(reviewActor(),batchId,UUID.randomUUID(),rollbackRequest(p));assertThat(batchRollbackService().saveNextRollback()).isTrue();
        assertThat(sql.queryForObject("SELECT current_attachment_evaluation_id FROM announcement_source_snapshots WHERE id=?",UUID.class,f.source())).isEqualTo(f.confirmation().evaluationId());
        assertThat(reviewService().selectReviewContextDetails(f.source()).confirmedClassification()).isNull();
    }
    @Test void rollbackDatabaseRejectsUnapprovedStateAndChangingApprovalOrResult() {
        var f=prepareConfirmationRestoration(false);UUID batchId=sql.queryForObject("SELECT batch_id FROM announcement_attachment_jobs WHERE id=?",UUID.class,f.job());
        assertThatThrownBy(()->sql.update("UPDATE announcement_attachment_jobs SET rollback_status_code='ROLLED_BACK',row_version=row_version+1 WHERE id=?",f.job())).isInstanceOf(DataIntegrityViolationException.class);
        var p=batchRollbackService().selectPreviewDetails(reviewActor(),batchId);var receipt=batchRollbackService().insertRollback(reviewActor(),batchId,UUID.randomUUID(),rollbackRequest(p));
        assertThatThrownBy(()->sql.update("UPDATE announcement_attachment_batch_rollback_actions SET reason_hash=repeat('f',64) WHERE id=?",receipt.actionId())).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(()->sql.update("DELETE FROM announcement_attachment_batch_rollback_items WHERE action_id=?",receipt.actionId())).isInstanceOf(DataIntegrityViolationException.class);
        batchRollbackService().saveNextRollback();
        assertThatThrownBy(()->sql.update("UPDATE announcement_attachment_jobs SET rollback_status_code='NOT_REQUESTED',row_version=row_version+1 WHERE id=?",f.job())).isInstanceOf(DataIntegrityViolationException.class);
    }
    @Test void rollbackFromPausedApplicationCancelsPendingWithoutPretendingWholeScopeWasRestored() {
        updateFixturePolicyMode("ENFORCE");UUID first=selectRequest().sourceId(),second=selectRequest().sourceId();insertCollectionLocator(first);insertCollectionLocator(second);
        var batch=batchService().insertBatch(reviewActor(),UUID.randomUUID(),batchRequest(batchScope(100)));batchService().updateCollectionStart(reviewActor(),batch.batchId(),batchCollection(batch));
        for(int n=0;n<2;n++){var job=service.saveNextJobClaim().orElseThrow();service.saveDownloadBytes(job.jobId(),job.leaseToken(),15);
            evidenceService.saveAttachmentSet(job.jobId(),job.leaseToken(),new AttachmentSetEvidence("FOUND",true,List.of(selectFileEvidence(100)))).orElseThrow();
            context.getBean(AnnouncementAttachmentEvaluationService.class).saveJobEvaluation(job.jobId(),job.leaseToken()).orElseThrow();}
        batchService().saveCollectionProgress();batch=batchService().selectBatchDetails(reviewActor(),batch.batchId());
        var p=batchPreviewService().insertPreview(reviewActor(),batch.batchId(),UUID.randomUUID(),batchPreviewRequest(batch));
        var jobs=batchService().selectItemList(reviewActor(),batch.batchId(),1,100).items();
        var selected=batchPreviewService().updateSelection(reviewActor(),batch.batchId(),UUID.randomUUID(),new com.saneb.domain.announcementattachment.dto.AttachmentBatchPreviewRequests.Selection(
                p.currentBatchVersion(),p.previewHash(),jobs.stream().map(com.saneb.domain.announcementattachment.vo.AttachmentBatchRows.Item::jobId).toList(),"전체 두 항목 적용"));
        batchApplicationService().insertAction(reviewActor(),batch.batchId(),UUID.randomUUID(),"START",applicationRequest(selected));batchApplicationService().saveNextApplication();
        batchApplicationService().insertAction(reviewActor(),batch.batchId(),UUID.randomUUID(),"PAUSE",applicationRequest(selected));
        var rollback=batchRollbackService().selectPreviewDetails(reviewActor(),batch.batchId());assertThat(rollback.targetCount()).isEqualTo(1);assertThat(rollback.cancelPendingCount()).isEqualTo(1);
        var receipt=batchRollbackService().insertRollback(reviewActor(),batch.batchId(),UUID.randomUUID(),rollbackRequest(rollback));
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_attachment_jobs WHERE batch_id=? AND application_error_code='APPLICATION_CANCELLED_BY_ROLLBACK'",Integer.class,batch.batchId())).isEqualTo(1);
        assertThat(batchApplicationService().saveNextApplication()).isFalse();assertThat(batchRollbackService().saveNextRollback()).isTrue();
        var result=batchRollbackService().selectActionDetails(reviewActor(),batch.batchId(),receipt.actionId());assertThat(result.scopeCount()).isEqualTo(2);assertThat(result.rolledBackCount()).isEqualTo(1);assertThat(result.statusCode()).isEqualTo("ROLLBACK_PARTIAL_FAILED");
    }
    @Test void rollbackKeepsOperationalDraftCreatedAfterApproval() {
        var f=prepareConfirmationRestoration(false);UUID batchId=sql.queryForObject("SELECT batch_id FROM announcement_attachment_jobs WHERE id=?",UUID.class,f.job());
        var p=batchRollbackService().selectPreviewDetails(reviewActor(),batchId);batchRollbackService().insertRollback(reviewActor(),batchId,UUID.randomUUID(),rollbackRequest(p));
        var confirmed=reviewService().insertConfirmation(reviewActor(),f.source(),UUID.randomUUID(),selectReviewRequest(f.source()));
        var draft=reviewService().insertOperationalAnnouncement(reviewActor(),f.source(),new AttachmentReviewRequests.Conversion(reviewService().selectReviewContextDetails(f.source()).version(),confirmed.confirmationId(),"BUSINESS",null));
        var before=dao.selectSourceContextDetails(f.source());batchRollbackService().saveNextRollback();assertThat(dao.selectSourceContextDetails(f.source())).isEqualTo(before);
        assertThat(sql.queryForObject("SELECT announcement_id FROM announcement_source_links WHERE source_id=?",UUID.class,f.source())).isEqualTo(draft.announcementId());
        assertThat(sql.queryForObject("SELECT approval_status_code FROM announcements WHERE id=?",String.class,draft.announcementId())).isEqualTo("DRAFT");
    }
    private ConfirmationRestorationFixture prepareConfirmationRestoration(boolean stalePriorVersion) {
        UUID source=insertReviewFixture(false);var confirmation=reviewService().insertConfirmation(reviewActor(),source,UUID.randomUUID(),selectReviewRequest(source));
        if(stalePriorVersion)sql.update("UPDATE announcement_source_snapshots SET attachment_row_version=attachment_row_version+1 WHERE id=?",source);
        var batch=completeBatchForPreview(source);var preview=batchPreviewService().insertPreview(reviewActor(),batch.batchId(),UUID.randomUUID(),batchPreviewRequest(batch));
        var item=batchService().selectItemList(reviewActor(),batch.batchId(),1,100).items().getFirst();
        var selected=batchPreviewService().updateSelection(reviewActor(),batch.batchId(),UUID.randomUUID(),new com.saneb.domain.announcementattachment.dto.AttachmentBatchPreviewRequests.Selection(
                preview.currentBatchVersion(),preview.previewHash(),List.of(item.jobId()),"테스트 소유 복원 근거 준비"));
        batchApplicationService().insertAction(reviewActor(),batch.batchId(),UUID.randomUUID(),"START",applicationRequest(selected));
        assertThat(batchApplicationService().saveNextApplication()).isTrue();
        assertThat(sql.queryForObject("SELECT application_status_code FROM announcement_attachment_jobs WHERE id=?",String.class,item.jobId())).isEqualTo("APPLIED");
        return new ConfirmationRestorationFixture(source,item.jobId(),confirmation);
    }
    private UUID insertConfirmationRestorationEvidence(ConfirmationRestorationFixture fixture) {
        UUID id=UUID.randomUUID();
        assertThat(sql.update("""
                INSERT INTO announcement_attachment_confirmation_restorations(id,job_id,source_id,confirmation_id,
                    source_version,attachment_version,applied_input_hash,restored_by,idempotency_key,request_hash)
                SELECT ?,id,source_id,previous_confirmation_id,applied_source_version,applied_attachment_version+1,applied_input_hash,?,?,repeat('a',64)
                FROM announcement_attachment_jobs WHERE id=?
                """,id,actor,UUID.randomUUID(),fixture.job())).isEqualTo(1);
        return id;
    }
    private UUID completeConfirmationRestoration(ConfirmationRestorationFixture fixture) {
        UUID batchId=sql.queryForObject("SELECT batch_id FROM announcement_attachment_jobs WHERE id=?",UUID.class,fixture.job());
        var rollback=batchRollbackService();var preview=rollback.selectPreviewDetails(reviewActor(),batchId);
        rollback.insertRollback(reviewActor(),batchId,UUID.randomUUID(),rollbackRequest(preview));assertThat(rollback.saveNextRollback()).isTrue();
        return sql.queryForObject("SELECT id FROM announcement_attachment_confirmation_restorations WHERE job_id=?",UUID.class,fixture.job());
    }
    @Test void confirmationRestorationPreservesOriginalReceiptAndMakesOnlyRestoredBindingCurrent() {
        var fixture=prepareConfirmationRestoration(false);var before=dao.selectSourceContextDetails(fixture.source());
        UUID restoration=completeConfirmationRestoration(fixture);var restored=reviewService().selectReviewContextDetails(fixture.source());
        assertThat(restored.confirmedClassification().confirmation()).isEqualTo(fixture.confirmation());
        assertThat(restored.confirmedClassification().binding().restorationId()).isEqualTo(restoration);
        assertThat(restored.version().expectedAttachmentVersion()).isEqualTo(before.attachmentVersion()+1);
        assertThat(restored.confirmedClassification().binding().attachmentVersion()).isEqualTo(restored.version().expectedAttachmentVersion());
        assertThat(restored.confirmedClassification().targetCategoryCodes()).containsExactly("BUSINESS","PERSONAL");
        var oldRequest=new AttachmentReviewRequests.Conversion(new AttachmentReviewRequests.Version(restored.version().expectedBaseDecisionId(),
                fixture.confirmation().evaluationId(),fixture.confirmation().sourceVersion(),fixture.confirmation().attachmentVersion(),fixture.confirmation().setHash()),
                fixture.confirmation().confirmationId(),"BUSINESS",null);
        assertThatThrownBy(()->reviewService().insertOperationalAnnouncement(reviewActor(),fixture.source(),oldRequest)).isInstanceOf(ApiException.class);
        var draft=reviewService().insertOperationalAnnouncement(reviewActor(),fixture.source(),new AttachmentReviewRequests.Conversion(restored.version(),fixture.confirmation().confirmationId(),"BUSINESS",null));
        assertThat(sql.queryForObject("SELECT approval_status_code FROM announcements WHERE id=?",String.class,draft.announcementId())).isEqualTo("DRAFT");
        var current=context.getBean(SqlSessionTemplate.class).getMapper(com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentCurrentDao.class).selectSourceDetails(fixture.source());
        assertThat(current.confirmationId()).isEqualTo(fixture.confirmation().confirmationId());
    }
    @Test void confirmationCannotBeRevivedWithOnlyCurrentFlagOrIncompleteRestoration() {
        var fixture=prepareConfirmationRestoration(false);var before=dao.selectSourceContextDetails(fixture.source());
        assertThatThrownBy(()->sql.update("UPDATE announcement_source_attachment_confirmations SET is_current=true WHERE id=?",fixture.confirmation().confirmationId()))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(()->insertConfirmationRestorationEvidence(fixture)).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(dao.selectSourceContextDetails(fixture.source())).isEqualTo(before);
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_attachment_confirmation_restorations",Integer.class)).isZero();
    }
    @Test void stalePriorConfirmationDoesNotGainValidityFromRollback() {
        var fixture=prepareConfirmationRestoration(true);
        assertThatThrownBy(()->insertConfirmationRestorationEvidence(fixture)).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(sql.queryForObject("SELECT is_current FROM announcement_source_attachment_confirmations WHERE id=?",Boolean.class,fixture.confirmation().confirmationId())).isFalse();
    }
    @Test void laterManualConfirmationBlocksRestorationWithoutOverwritingNewReview() {
        var fixture=prepareConfirmationRestoration(false);
        var newer=reviewService().insertConfirmation(reviewActor(),fixture.source(),UUID.randomUUID(),selectReviewRequest(fixture.source()));
        assertThatThrownBy(()->insertConfirmationRestorationEvidence(fixture)).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(reviewService().selectReviewContextDetails(fixture.source()).confirmedClassification().confirmation().confirmationId()).isEqualTo(newer.confirmationId());
    }
    @Test void restorationEvidenceCannotBeEditedOrDeletedButFollowsSourceDeletion() {
        var fixture=prepareConfirmationRestoration(false);UUID restoration=completeConfirmationRestoration(fixture);
        assertThatThrownBy(()->sql.update("UPDATE announcement_attachment_confirmation_restorations SET attachment_version=attachment_version+1 WHERE id=?",restoration)).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(()->sql.update("DELETE FROM announcement_attachment_confirmation_restorations WHERE id=?",restoration)).isInstanceOf(DataIntegrityViolationException.class);
        sql.update("DELETE FROM announcement_source_snapshots WHERE id=?",fixture.source());
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_attachment_confirmation_restorations WHERE id=?",Integer.class,restoration)).isZero();
    }
    @Test void laterBaseVersionInvalidatesRestoredConfirmationAndDoesNotReturnOldTagsAsCurrent() {
        var fixture=prepareConfirmationRestoration(false);completeConfirmationRestoration(fixture);
        sql.update("UPDATE announcement_source_snapshots SET classification_row_version=classification_row_version+1 WHERE id=?",fixture.source());
        var current=context.getBean(SqlSessionTemplate.class).getMapper(com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentCurrentDao.class).selectSourceDetails(fixture.source());
        assertThat(current.confirmationId()).isNull();
        assertThat(sql.queryForObject("SELECT is_current FROM announcement_source_attachment_confirmations WHERE id=?",Boolean.class,fixture.confirmation().confirmationId())).isFalse();
    }
    @Test void batchApplicationProtectsOperationalDraftLinkedAfterApproval() {
        UUID source=insertReviewFixture(false);var confirmed=reviewService().insertConfirmation(reviewActor(),source,UUID.randomUUID(),selectReviewRequest(source));
        var batch=completeBatchForPreview(source);var preview=batchPreviewService().insertPreview(reviewActor(),batch.batchId(),UUID.randomUUID(),batchPreviewRequest(batch));
        var item=batchService().selectItemList(reviewActor(),batch.batchId(),1,100).items().getFirst();
        var selected=batchPreviewService().updateSelection(reviewActor(),batch.batchId(),UUID.randomUUID(),new com.saneb.domain.announcementattachment.dto.AttachmentBatchPreviewRequests.Selection(
                preview.currentBatchVersion(),preview.previewHash(),List.of(item.jobId()),"후속 전환 충돌 검증"));
        batchApplicationService().insertAction(reviewActor(),batch.batchId(),UUID.randomUUID(),"START",applicationRequest(selected));
        var conversion=new AttachmentReviewRequests.Conversion(reviewService().selectReviewContextDetails(source).version(),confirmed.confirmationId(),"BUSINESS",null);
        var draft=reviewService().insertOperationalAnnouncement(reviewActor(),source,conversion);var before=dao.selectSourceContextDetails(source);
        batchApplicationService().saveNextApplication();assertThat(dao.selectSourceContextDetails(source)).isEqualTo(before);
        assertThat(sql.queryForObject("SELECT application_status_code FROM announcement_attachment_jobs WHERE id=?",String.class,item.jobId())).isEqualTo("CONFLICT");
        assertThat(sql.queryForObject("SELECT approval_status_code FROM announcements WHERE id=?",String.class,draft.announcementId())).isEqualTo("DRAFT");
        assertThat(sql.queryForObject("SELECT announcement_id FROM announcement_source_links WHERE source_id=?",UUID.class,source)).isEqualTo(draft.announcementId());
    }
    @Test void batchApplicationDatabaseRejectsBypassAndImmutableApprovalOrResultEdits() {
        var preview=prepareApplicationPreview();var item=batchService().selectItemList(reviewActor(),preview.batchId(),1,100).items().getFirst();
        assertThatThrownBy(()->sql.update("UPDATE announcement_attachment_jobs SET application_status_code='APPLIED' WHERE id=?",item.jobId())).isInstanceOf(DataIntegrityViolationException.class);
        var action=batchApplicationService().insertAction(reviewActor(),preview.batchId(),UUID.randomUUID(),"START",applicationRequest(preview));
        assertThatThrownBy(()->sql.update("UPDATE announcement_attachment_batch_application_actions SET reason_hash=repeat('0',64) WHERE id=?",action.actionId())).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(()->sql.update("UPDATE announcement_attachment_batches SET application_approval_id=NULL WHERE id=?",preview.batchId())).isInstanceOf(DataIntegrityViolationException.class);
        batchApplicationService().saveNextApplication();
        assertThatThrownBy(()->sql.update("UPDATE announcement_attachment_jobs SET application_status_code='PENDING' WHERE id=?",item.jobId())).isInstanceOf(DataIntegrityViolationException.class);
    }

    private com.saneb.domain.announcementattachment.service.AnnouncementAttachmentNormalRollbackService normalRollback() {
        return context.getBean(com.saneb.domain.announcementattachment.service.AnnouncementAttachmentNormalRollbackService.class);
    }
    private com.saneb.domain.announcementattachment.dto.AttachmentNormalRollbackRequest normalRequest(com.saneb.domain.announcementattachment.dto.AttachmentNormalRollbackResponses.Preview p) {
        return new com.saneb.domain.announcementattachment.dto.AttachmentNormalRollbackRequest(p.sourceVersion(),p.attachmentVersion(),p.previewHash(),p.baseReopens(),p.confirmationRestores(),true,"테스트 소유 일반 작업 복구 승인");
    }
    private ConfirmationRestorationFixture normalRecoveryFixture(boolean failed,boolean stale) {
        UUID source=insertReviewFixture(false);var confirmed=reviewService().insertConfirmation(reviewActor(),source,UUID.randomUUID(),selectReviewRequest(source));
        if(stale)sql.update("UPDATE announcement_source_snapshots SET attachment_row_version=attachment_row_version+1 WHERE id=?",source);
        var before=dao.selectSourceContextDetails(source);
        service.insertAttachmentJob(new AttachmentJobReservation(source,policy,before.baseEvaluationId(),before.sourceVersion(),before.attachmentVersion(),UUID.randomUUID(),EXECUTION));
        var job=service.saveNextJobClaim().orElseThrow();
        if(failed)service.saveJobFailure(job.jobId(),job.leaseToken(),AttachmentFailureCode.DISCOVERY_FAILED);
        else {evidenceService.saveAttachmentSet(job.jobId(),job.leaseToken(),new AttachmentSetEvidence("NO_FILES",true,List.of()));context.getBean(AnnouncementAttachmentEvaluationService.class).saveJobEvaluation(job.jobId(),job.leaseToken()).orElseThrow();}
        return new ConfirmationRestorationFixture(source,job.jobId(),confirmed);
    }
    private ConfirmationRestorationFixture newEnforceRecoveryFixture(boolean failed) {
        updateFixturePolicyMode("ENFORCE");UUID run=insertFixtureRun();var request=selectIntakeSource(run);
        var intake=context.getBean(com.saneb.domain.announcementattachment.service.AnnouncementAttachmentIntakeService.class);var plan=intake.saveCollectionPlan(run,release);
        new TransactionTemplate(context.getBean(PlatformTransactionManager.class)).executeWithoutResult(status->intake.saveCollectedSource(request.sourceId(),plan,true));
        var job=service.saveNextJobClaim().orElseThrow();
        if(failed)service.saveJobFailure(job.jobId(),job.leaseToken(),AttachmentFailureCode.DISCOVERY_FAILED);
        else {evidenceService.saveAttachmentSet(job.jobId(),job.leaseToken(),new AttachmentSetEvidence("NO_FILES",true,List.of()));context.getBean(AnnouncementAttachmentEvaluationService.class).saveJobEvaluation(job.jobId(),job.leaseToken()).orElseThrow();}
        return new ConfirmationRestorationFixture(request.sourceId(),job.jobId(),null);
    }
    @Test void normalRollbackRestoresOriginalConfirmationAndAllowsOnlyCurrentVersionDraft() {
        var f=normalRecoveryFixture(false,false);var p=normalRollback().selectPreviewDetails(reviewActor(),f.source(),f.job());assertThat(p.readinessCode()).isEqualTo("READY");assertThat(p.confirmationRestores()).isTrue();
        UUID key=UUID.randomUUID();var request=normalRequest(p);var receipt=normalRollback().insertRollback(reviewActor(),f.source(),f.job(),key,request);
        assertThat(normalRollback().insertRollback(reviewActor(),f.source(),f.job(),key,request)).isEqualTo(receipt);
        var state=reviewService().selectReviewContextDetails(f.source());assertThat(state.confirmedClassification().confirmation()).isEqualTo(f.confirmation());
        assertThat(state.confirmedClassification().binding().attachmentVersion()).isEqualTo(p.attachmentVersion()+1);
        assertThat(sql.queryForObject("SELECT application_status_code FROM announcement_attachment_jobs WHERE id=?",String.class,f.job())).isEqualTo("APPLIED");
        var draft=reviewService().insertOperationalAnnouncement(reviewActor(),f.source(),new AttachmentReviewRequests.Conversion(state.version(),f.confirmation().confirmationId(),"BUSINESS",null));
        assertThat(sql.queryForObject("SELECT approval_status_code FROM announcements WHERE id=?",String.class,draft.announcementId())).isEqualTo("DRAFT");
        assertThat(normalRollback().selectActionDetails(reviewActor(),f.source(),f.job(),receipt.actionId())).isEqualTo(receipt);
    }
    @Test void normalRecoveryHistoryPagesPreservedFailuresAndReceiptsWithinVisibleSourceOnly() {
        var f=newEnforceRecoveryFixture(true);var before=dao.selectSourceContextDetails(f.source());
        var history=normalRollback().selectJobList(reviewActor(),f.source(),1,10);
        assertThat(history.totalCount()).isEqualTo(1);assertThat(history.items()).hasSize(1);
        assertThat(history.items().getFirst().sourceId()).isEqualTo(f.source());assertThat(history.items().getFirst().jobId()).isEqualTo(f.job());
        assertThat(history.items().getFirst().jobStatusCode()).isEqualTo("FAILED");assertThat(history.items().getFirst().actionId()).isNull();
        assertThat(normalRollback().selectJobList(reviewActor(),f.source(),2,10).items()).isEmpty();
        assertThat(dao.selectSourceContextDetails(f.source())).isEqualTo(before);
        var p=normalRollback().selectPreviewDetails(reviewActor(),f.source(),f.job());var r=normalRollback().insertRollback(reviewActor(),f.source(),f.job(),UUID.randomUUID(),normalRequest(p));
        var restored=normalRollback().selectJobList(reviewActor(),f.source(),1,10).items().getFirst();
        assertThat(restored.actionId()).isEqualTo(r.actionId());assertThat(restored.jobStatusCode()).isEqualTo("FAILED");assertThat(restored.applicationStatusCode()).isEqualTo("PENDING");
        assertThat(normalRollback().selectActionDetails(reviewActor(),f.source(),f.job(),restored.actionId())).isEqualTo(r);
        assertThatThrownBy(()->normalRollback().selectJobList(reviewActor(),UUID.randomUUID(),1,10)).isInstanceOf(ApiException.class);
        var hidden=selectRequest();
        sql.update("UPDATE announcement_source_snapshots SET data_purpose_code='QA' WHERE id=?",hidden.sourceId());
        assertThatThrownBy(()->normalRollback().selectJobList(reviewActor(),hidden.sourceId(),1,10)).isInstanceOf(ApiException.class);
    }
    @Test void failedNormalReservationRestoresConfirmationWithoutChangingFailureOrClaimingApplied() {
        var f=normalRecoveryFixture(true,false);var p=normalRollback().selectPreviewDetails(reviewActor(),f.source(),f.job());assertThat(p.modeCode()).isEqualTo("FAILED_RESERVATION");assertThat(p.confirmationRestores()).isTrue();
        normalRollback().insertRollback(reviewActor(),f.source(),f.job(),UUID.randomUUID(),normalRequest(p));
        assertThat(reviewService().selectReviewContextDetails(f.source()).confirmedClassification().confirmation()).isEqualTo(f.confirmation());
        assertThat(sql.queryForObject("SELECT job_status_code FROM announcement_attachment_jobs WHERE id=?",String.class,f.job())).isEqualTo("FAILED");
        assertThat(sql.queryForObject("SELECT application_status_code FROM announcement_attachment_jobs WHERE id=?",String.class,f.job())).isEqualTo("PENDING");
        assertThat(sql.queryForObject("SELECT applied_input_hash FROM announcement_attachment_jobs WHERE id=?",String.class,f.job())).isNull();
        assertThatThrownBy(()->sql.update("UPDATE announcement_attachment_jobs SET job_status_code='PENDING' WHERE id=?",f.job())).isInstanceOf(DataIntegrityViolationException.class);
    }
    @Test void failedNewEnforceReservationRestoresBaseAndKeepsFailureEvidence() { assertNormalBaseRecovery(true); }
    @Test void appliedNewEnforceJobRestoresBaseWithoutDeletingEvidence() { assertNormalBaseRecovery(false); }
    private void assertNormalBaseRecovery(boolean failed) {
        var f=newEnforceRecoveryFixture(failed);var p=normalRollback().selectPreviewDetails(reviewActor(),f.source(),f.job());assertThat(p.baseReopens()).isTrue();
        normalRollback().insertRollback(reviewActor(),f.source(),f.job(),UUID.randomUUID(),normalRequest(p));var s=dao.selectSourceContextDetails(f.source());
        assertThat(s.attachmentReviewRequired()).isFalse();assertThat(s.attachmentPolicyId()).isNull();assertThat(s.currentAttachmentEvaluationId()).isNull();assertThat(s.attachmentVersion()).isEqualTo(p.attachmentVersion()+1);
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_attachment_jobs WHERE source_id=?",Integer.class,f.source())).isEqualTo(1);
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_source_links WHERE source_id=?",Integer.class,f.source())).isZero();
    }
    @Test void normalRollbackRejectsNewConfirmationAfterPreview() { assertNormalLaterReviewProtected(false); }
    @Test void normalRollbackRejectsDraftAfterPreviewWithoutModifyingItsLink() { assertNormalLaterReviewProtected(true); }
    private void assertNormalLaterReviewProtected(boolean draft) {
        var f=normalRecoveryFixture(false,false);var p=normalRollback().selectPreviewDetails(reviewActor(),f.source(),f.job());
        var confirmed=reviewService().insertConfirmation(reviewActor(),f.source(),UUID.randomUUID(),selectReviewRequest(f.source()));
        if(draft)reviewService().insertOperationalAnnouncement(reviewActor(),f.source(),new AttachmentReviewRequests.Conversion(reviewService().selectReviewContextDetails(f.source()).version(),confirmed.confirmationId(),"BUSINESS",null));
        var before=dao.selectSourceContextDetails(f.source());assertThatThrownBy(()->normalRollback().insertRollback(reviewActor(),f.source(),f.job(),UUID.randomUUID(),normalRequest(p))).isInstanceOf(ApiException.class);
        assertThat(dao.selectSourceContextDetails(f.source())).isEqualTo(before);assertThat(sql.queryForObject("SELECT count(1) FROM announcement_attachment_normal_rollback_actions WHERE job_id=?",Integer.class,f.job())).isZero();
    }
    @Test void failedReservationAgencyChangeInvalidatesRecoveryEvenWithoutVersionChange() {
        var f=newEnforceRecoveryFixture(true);var p=normalRollback().selectPreviewDetails(reviewActor(),f.source(),f.job());sql.update("UPDATE announcement_source_snapshots SET agency_name='변경된 기관 QA' WHERE id=?",f.source());
        assertThat(normalRollback().selectPreviewDetails(reviewActor(),f.source(),f.job()).readinessCode()).isEqualTo("CURRENT_BINDING_CHANGED");
        assertThatThrownBy(()->normalRollback().insertRollback(reviewActor(),f.source(),f.job(),UUID.randomUUID(),normalRequest(p))).isInstanceOf(ApiException.class);
    }
    @Test void concurrentSameKeyNormalRecoveryCommitsExactlyOneReceipt() throws Exception {
        var f=normalRecoveryFixture(false,false);var request=normalRequest(normalRollback().selectPreviewDetails(reviewActor(),f.source(),f.job()));UUID key=UUID.randomUUID();var gate=new CountDownLatch(1);
        Callable<UUID> task=()->{gate.await(5,TimeUnit.SECONDS);return normalRollback().insertRollback(reviewActor(),f.source(),f.job(),key,request).actionId();};
        try(var pool=Executors.newFixedThreadPool(2)){var a=pool.submit(task);var b=pool.submit(task);gate.countDown();assertThat(a.get(20,TimeUnit.SECONDS)).isEqualTo(b.get(20,TimeUnit.SECONDS));}
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_attachment_normal_rollback_actions WHERE job_id=?",Integer.class,f.job())).isEqualTo(1);
    }
    @Test void normalApprovalCannotCommitWithoutRestoringSourceAndJob() {
        var f=normalRecoveryFixture(false,false);var p=normalRollback().selectPreviewDetails(reviewActor(),f.source(),f.job());var before=dao.selectSourceContextDetails(f.source());
        var action=new com.saneb.domain.announcementattachment.vo.AttachmentNormalRollbackRows.Action(UUID.randomUUID(),f.job(),f.source(),p.modeCode(),p.sourceVersion(),p.attachmentVersion(),p.previewHash(),p.baseReopens(),p.confirmationRestores(),actor,UUID.randomUUID(),"1".repeat(64),"2".repeat(64),null);
        var normalDao=context.getBean(SqlSessionTemplate.class).getMapper(com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentNormalRollbackDao.class);
        assertThatThrownBy(()->new TransactionTemplate(context.getBean(PlatformTransactionManager.class)).executeWithoutResult(status->normalDao.insertAction(action)))
                .isInstanceOf(org.springframework.transaction.TransactionSystemException.class)
                .hasRootCauseInstanceOf(org.postgresql.util.PSQLException.class)
                .satisfies(error -> assertThat(((org.postgresql.util.PSQLException)org.springframework.core.NestedExceptionUtils.getMostSpecificCause(error)).getSQLState()).isEqualTo("23514"));
        assertThat(dao.selectSourceContextDetails(f.source())).isEqualTo(before);
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_attachment_normal_rollback_actions WHERE id=?",Integer.class,action.id())).isZero();
    }
    @Test void normalReceiptAndRecoveryResultAreImmutableButSourceCascadeIsAllowed() {
        var f=normalRecoveryFixture(false,false);var p=normalRollback().selectPreviewDetails(reviewActor(),f.source(),f.job());var r=normalRollback().insertRollback(reviewActor(),f.source(),f.job(),UUID.randomUUID(),normalRequest(p));
        assertThatThrownBy(()->sql.update("UPDATE announcement_attachment_normal_rollback_actions SET reason_hash=repeat('0',64) WHERE id=?",r.actionId())).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(()->sql.update("DELETE FROM announcement_attachment_normal_rollback_actions WHERE id=?",r.actionId())).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(()->sql.update("UPDATE announcement_attachment_jobs SET restored_attachment_version=0 WHERE id=?",f.job())).isInstanceOf(DataIntegrityViolationException.class);
        sql.update("DELETE FROM announcement_source_snapshots WHERE id=?",f.source());assertThat(sql.queryForObject("SELECT count(1) FROM announcement_attachment_normal_rollback_actions WHERE id=?",Integer.class,r.actionId())).isZero();
    }
    @Test void normalRollbackNeverRevalidatesAnAlreadyStalePriorConfirmation() {
        var f=normalRecoveryFixture(false,true);var p=normalRollback().selectPreviewDetails(reviewActor(),f.source(),f.job());assertThat(p.staleConfirmationRemains()).isTrue();assertThat(p.confirmationRestores()).isFalse();
        normalRollback().insertRollback(reviewActor(),f.source(),f.job(),UUID.randomUUID(),normalRequest(p));assertThat(reviewService().selectReviewContextDetails(f.source()).confirmedClassification()).isNull();
        assertThat(sql.queryForObject("SELECT is_current FROM announcement_source_attachment_confirmations WHERE id=?",Boolean.class,f.confirmation().confirmationId())).isFalse();
    }
    @Test void newReservationRecognizesCompletedNormalRestorationBinding() {
        var f=normalRecoveryFixture(true,false);var p=normalRollback().selectPreviewDetails(reviewActor(),f.source(),f.job());normalRollback().insertRollback(reviewActor(),f.source(),f.job(),UUID.randomUUID(),normalRequest(p));
        var s=dao.selectSourceContextDetails(f.source());var job=service.insertAttachmentJob(new AttachmentJobReservation(f.source(),policy,s.baseEvaluationId(),s.sourceVersion(),s.attachmentVersion(),UUID.randomUUID(),EXECUTION));
        assertThat(sql.queryForObject("SELECT is_reservation_confirmation_valid FROM announcement_attachment_jobs WHERE id=?",Boolean.class,job.jobId())).isTrue();
    }
    @org.springframework.boot.test.context.TestConfiguration(proxyBeanMethods = false)
    @EnableTransactionManagement
    static class TestConfiguration {
        @Bean PlatformTransactionManager transactionManager(DataSource source) { return new DataSourceTransactionManager(source); }
        @Bean SqlSessionTemplate session(DataSource source) throws Exception {
            var factory = new SqlSessionFactoryBean();
            factory.setDataSource(source);
            factory.setTypeHandlers(new UuidTypeHandler());
            factory.setMapperLocations(new ClassPathResource("mapper/announcementattachment/AnnouncementAttachmentJobMapper.xml"),
                    new ClassPathResource("mapper/announcementattachment/AnnouncementAttachmentIntakeMapper.xml"),
                    new ClassPathResource("mapper/announcementattachment/AnnouncementAttachmentCurrentMapper.xml"),
                    new ClassPathResource("mapper/announcementattachment/AnnouncementAttachmentReviewMapper.xml"),
                    new ClassPathResource("mapper/announcementattachment/AnnouncementAttachmentRoleMapper.xml"),
                    new ClassPathResource("mapper/announcementattachment/AnnouncementAttachmentRetryMapper.xml"),
                    new ClassPathResource("mapper/announcementattachment/AnnouncementAttachmentHistoryMapper.xml"),
                    new ClassPathResource("mapper/announcementattachment/AnnouncementAttachmentCollectionMapper.xml"),
                    new ClassPathResource("mapper/announcementattachment/AnnouncementAttachmentPolicyMapper.xml"),
                    new ClassPathResource("mapper/announcementattachment/AnnouncementAttachmentPolicyCheckMapper.xml"),
                    new ClassPathResource("mapper/announcementattachment/AnnouncementAttachmentPolicyValidationMapper.xml"),
                    new ClassPathResource("mapper/announcementattachment/AnnouncementAttachmentProviderQaMapper.xml"),
                    new ClassPathResource("mapper/announcementattachment/AnnouncementAttachmentProviderQaManagementMapper.xml"),
                    new ClassPathResource("mapper/announcementattachment/AnnouncementAttachmentProviderQaEvidenceMapper.xml"),
                    new ClassPathResource("mapper/announcementattachment/AnnouncementAttachmentBatchMapper.xml"),
                    new ClassPathResource("mapper/announcementattachment/AnnouncementAttachmentBatchPreviewMapper.xml"),
                    new ClassPathResource("mapper/announcementattachment/AnnouncementAttachmentBatchApplicationMapper.xml"),
                    new ClassPathResource("mapper/announcementattachment/AnnouncementAttachmentBatchHistoryMapper.xml"),
                    new ClassPathResource("mapper/announcementattachment/AnnouncementAttachmentPolicyPublicationImpactMapper.xml"),
                    new ClassPathResource("mapper/announcementattachment/AnnouncementAttachmentPolicyPublicationScopeMapper.xml"),
                    new ClassPathResource("mapper/announcementattachment/AnnouncementAttachmentPolicyPublicationMapper.xml"),
                    new ClassPathResource("mapper/announcementattachment/AnnouncementAttachmentBatchRollbackMapper.xml"),
                    new ClassPathResource("mapper/announcementattachment/AnnouncementAttachmentNormalRollbackMapper.xml"),
                    new ClassPathResource("mapper/announcement/AnnouncementMapper.xml"),
                    new ClassPathResource("mapper/announcementattachment/AnnouncementAttachmentEvidenceMapper.xml"),
                    new ClassPathResource("mapper/announcementattachment/AnnouncementAttachmentEvaluationMapper.xml"),
                    new ClassPathResource("mapper/announcementattachment/AnnouncementAttachmentSegmentMapper.xml"),
                    new ClassPathResource("mapper/announcementsource/AnnouncementSourceRuleReleaseMapper.xml"),
                    new ClassPathResource("mapper/announcementsource/AnnouncementSourceClassificationMapper.xml"),
                    new ClassPathResource("mapper/announcementsource/AnnouncementSourceMapper.xml"));
            return new SqlSessionTemplate(factory.getObject());
        }
        @Bean AnnouncementAttachmentJobDao dao(SqlSessionTemplate session) { return session.getMapper(AnnouncementAttachmentJobDao.class); }
        @Bean com.saneb.domain.announcementattachment.service.AnnouncementAttachmentPolicyPublicationScopeService publicationScopeService(SqlSessionTemplate session,PlatformTransactionManager transactions) {
            return new com.saneb.domain.announcementattachment.service.impl.AnnouncementAttachmentPolicyPublicationScopeServiceImpl(
                    session.getMapper(com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentPolicyPublicationScopeDao.class),
                    session.getMapper(com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentPolicyDao.class),
                    session.getMapper(AnnouncementSourceDao.class),new ObjectMapper().findAndRegisterModules(),transactions);
        }
        @Bean com.saneb.domain.announcementattachment.service.AnnouncementAttachmentPolicyPublicationImpactService publicationImpactService(SqlSessionTemplate session) {
            return new com.saneb.domain.announcementattachment.service.impl.AnnouncementAttachmentPolicyPublicationImpactServiceImpl(
                    session.getMapper(com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentPolicyDao.class),
                    session.getMapper(com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentPolicyValidationDao.class),
                    session.getMapper(com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentPolicyPublicationImpactDao.class),new ObjectMapper().findAndRegisterModules());
        }
        @Bean com.saneb.domain.announcementattachment.service.AnnouncementAttachmentBatchHistoryService batchHistoryService(SqlSessionTemplate session) {
            return new com.saneb.domain.announcementattachment.service.impl.AnnouncementAttachmentBatchHistoryServiceImpl(
                    session.getMapper(com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentBatchHistoryDao.class),
                    session.getMapper(com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentBatchDao.class));
        }
        @Bean com.saneb.domain.announcementattachment.service.AnnouncementAttachmentNormalRollbackService normalRollbackService(SqlSessionTemplate session) {
            return new com.saneb.domain.announcementattachment.service.impl.AnnouncementAttachmentNormalRollbackServiceImpl(
                    session.getMapper(com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentNormalRollbackDao.class),
                    session.getMapper(AnnouncementAttachmentEvaluationDao.class),session.getMapper(AnnouncementSourceDao.class),new ObjectMapper().findAndRegisterModules());
        }
        @Bean com.saneb.domain.announcementattachment.service.AnnouncementAttachmentBatchRollbackService batchRollbackService(SqlSessionTemplate session,PlatformTransactionManager manager) {
            return new com.saneb.domain.announcementattachment.service.impl.AnnouncementAttachmentBatchRollbackServiceImpl(
                    session.getMapper(com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentBatchRollbackDao.class),
                    session.getMapper(com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentBatchDao.class),
                    session.getMapper(com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentBatchPreviewDao.class),
                    session.getMapper(AnnouncementAttachmentEvaluationDao.class),session.getMapper(AnnouncementSourceDao.class),new ObjectMapper().findAndRegisterModules(),manager);
        }
        @Bean com.saneb.domain.announcementattachment.service.AnnouncementAttachmentBatchService batchService(SqlSessionTemplate session,AnnouncementAttachmentJobDao jobs) {
            return new com.saneb.domain.announcementattachment.service.impl.AnnouncementAttachmentBatchServiceImpl(
                    session.getMapper(com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentBatchDao.class),jobs,
                    session.getMapper(com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentIntakeDao.class),
                    new com.saneb.domain.announcementattachment.discovery.AttachmentDiscoveryProfileRegistry(List.of(PROFILE)),
                    session.getMapper(AnnouncementSourceDao.class),new ObjectMapper().findAndRegisterModules());
        }
        @Bean com.saneb.domain.announcementattachment.service.AnnouncementAttachmentPolicyCheckService policyCheckService(SqlSessionTemplate session,AnnouncementSourceRuleReleaseService rules,PlatformTransactionManager transactions) {
            return new com.saneb.domain.announcementattachment.service.impl.AnnouncementAttachmentPolicyCheckServiceImpl(
                    session.getMapper(com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentPolicyDao.class),
                    session.getMapper(com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentPolicyCheckDao.class),rules,
                    new com.saneb.domain.announcementattachment.service.impl.AnnouncementAttachmentPolicyGoldenGate(),session.getMapper(AnnouncementSourceDao.class),new ObjectMapper(),transactions);
        }
        @Bean com.saneb.domain.announcementattachment.service.AnnouncementAttachmentBatchApplicationService batchApplicationService(SqlSessionTemplate session,PlatformTransactionManager transactions) {
            return new com.saneb.domain.announcementattachment.service.impl.AnnouncementAttachmentBatchApplicationServiceImpl(
                    session.getMapper(com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentBatchApplicationDao.class),
                    session.getMapper(com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentBatchDao.class),
                    session.getMapper(com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentBatchPreviewDao.class),
                    session.getMapper(com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentEvaluationDao.class),session.getMapper(AnnouncementSourceDao.class),new ObjectMapper().findAndRegisterModules(),transactions);
        }
        @Bean com.saneb.domain.announcementattachment.service.AnnouncementAttachmentBatchPreviewService batchPreviewService(SqlSessionTemplate session) {
            return new com.saneb.domain.announcementattachment.service.impl.AnnouncementAttachmentBatchPreviewServiceImpl(
                    session.getMapper(com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentBatchPreviewDao.class),
                    session.getMapper(com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentBatchDao.class),session.getMapper(AnnouncementSourceDao.class),new ObjectMapper().findAndRegisterModules());
        }
        @Bean com.saneb.domain.announcementattachment.service.AnnouncementAttachmentPolicyService policyService(SqlSessionTemplate session) {
            return new com.saneb.domain.announcementattachment.service.impl.AnnouncementAttachmentPolicyServiceImpl(
                    session.getMapper(com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentPolicyDao.class),session.getMapper(AnnouncementSourceDao.class),
                    new com.saneb.domain.announcementattachment.discovery.AttachmentDiscoveryProfileRegistry(List.of(PROFILE)),new ObjectMapper());
        }
        @Bean com.saneb.domain.announcementattachment.service.AnnouncementAttachmentCollectionService collectionService(SqlSessionTemplate session,AnnouncementAttachmentJobDao jobs) {
            return new com.saneb.domain.announcementattachment.service.impl.AnnouncementAttachmentCollectionServiceImpl(jobs,
                    session.getMapper(com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentIntakeDao.class),
                    session.getMapper(com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentCollectionDao.class),
                    session.getMapper(com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentRetryDao.class),session.getMapper(AnnouncementSourceDao.class),
                    new com.saneb.domain.announcementattachment.discovery.AttachmentDiscoveryProfileRegistry(List.of(PROFILE)),new ObjectMapper(),true);
        }
        @Bean com.saneb.domain.announcementattachment.service.AnnouncementAttachmentHistoryService historyService(SqlSessionTemplate session) {
            return new com.saneb.domain.announcementattachment.service.impl.AnnouncementAttachmentHistoryServiceImpl(
                    session.getMapper(com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentCurrentDao.class),
                    session.getMapper(AnnouncementAttachmentEvaluationDao.class),
                    session.getMapper(com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentHistoryDao.class),new ObjectMapper());
        }
        @Bean com.saneb.domain.announcementattachment.service.AnnouncementAttachmentRetryService retryService(SqlSessionTemplate session,AnnouncementAttachmentJobDao jobs,AnnouncementAttachmentEvidenceDao evidence) {
            return new com.saneb.domain.announcementattachment.service.impl.AnnouncementAttachmentRetryServiceImpl(jobs,
                    session.getMapper(com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentCurrentDao.class),evidence,
                    session.getMapper(AnnouncementAttachmentReviewDao.class),session.getMapper(AnnouncementAttachmentRoleDao.class),
                    session.getMapper(com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentRetryDao.class),session.getMapper(AnnouncementSourceDao.class),
                    new com.saneb.domain.announcementattachment.discovery.AttachmentDiscoveryProfileRegistry(List.of(PROFILE)),new ObjectMapper());
        }
        @Bean AnnouncementAttachmentRoleService roleService(SqlSessionTemplate session,AnnouncementAttachmentJobDao jobs,AnnouncementAttachmentEvidenceDao evidence) {
            return new AnnouncementAttachmentRoleServiceImpl(jobs,session.getMapper(com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentCurrentDao.class),
                    evidence,session.getMapper(AnnouncementAttachmentReviewDao.class),session.getMapper(AnnouncementAttachmentRoleDao.class),session.getMapper(AnnouncementSourceDao.class),new ObjectMapper());
        }
        @Bean AnnouncementAttachmentReviewService reviewService(SqlSessionTemplate session,AnnouncementAttachmentJobDao jobs,
                AnnouncementAttachmentEvidenceDao evidence,AnnouncementAttachmentEvaluationDao evaluations) {
            return new AnnouncementAttachmentReviewServiceImpl(jobs,
                    session.getMapper(com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentCurrentDao.class),evaluations,evidence,
                    session.getMapper(AnnouncementAttachmentReviewDao.class),session.getMapper(AnnouncementSourceDao.class),
                    session.getMapper(AnnouncementDao.class),new ObjectMapper());
        }
        @Bean com.saneb.domain.announcementattachment.service.AnnouncementAttachmentIntakeService intakeService(SqlSessionTemplate session,
                AnnouncementAttachmentJobDao jobs,AnnouncementAttachmentJobService service) {
            return new com.saneb.domain.announcementattachment.service.impl.AnnouncementAttachmentIntakeServiceImpl(
                    session.getMapper(com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentIntakeDao.class),jobs,service,
                    new com.saneb.domain.announcementattachment.discovery.AttachmentDiscoveryProfileRegistry(List.of(PROFILE)),new ObjectMapper(),true);
        }
        @Bean AnnouncementAttachmentEvidenceDao evidenceDao(SqlSessionTemplate session) { return session.getMapper(AnnouncementAttachmentEvidenceDao.class); }
        @Bean AnnouncementAttachmentEvaluationDao evaluationDao(SqlSessionTemplate session) { return session.getMapper(AnnouncementAttachmentEvaluationDao.class); }
        @Bean AnnouncementSourceRuleReleaseService ruleService(SqlSessionTemplate session) {
            return new AnnouncementSourceRuleReleaseServiceImpl(session.getMapper(AnnouncementSourceRuleReleaseDao.class), null);
        }
        @Bean AnnouncementAttachmentEvaluationService evaluationService(AnnouncementAttachmentJobDao jobs,
                AnnouncementAttachmentEvidenceDao evidence, AnnouncementAttachmentEvaluationDao evaluations,
                AnnouncementSourceRuleReleaseService rules, PlatformTransactionManager transactions, SqlSessionTemplate session) {
            return new AnnouncementAttachmentEvaluationServiceImpl(jobs, evidence, evaluations,
                    session.getMapper(com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentSegmentDao.class),rules,new ObjectMapper(),transactions);
        }
        @Bean AnnouncementAttachmentEvidenceService evidenceService(SqlSessionTemplate session,AnnouncementAttachmentJobDao jobs, AnnouncementAttachmentEvidenceDao evidence) {
            return new AnnouncementAttachmentEvidenceServiceImpl(jobs, evidence,
                    session.getMapper(com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentRetryDao.class),
                    session.getMapper(AnnouncementAttachmentRoleDao.class),new ObjectMapper());
        }
        @Bean AnnouncementAttachmentReadService readService(AnnouncementAttachmentJobDao jobs, AnnouncementAttachmentEvidenceDao evidence) {
            return new AnnouncementAttachmentReadServiceImpl(jobs, evidence);
        }
        @Bean com.saneb.domain.announcementattachment.service.AnnouncementAttachmentCurrentService currentService(SqlSessionTemplate session) {
            return new com.saneb.domain.announcementattachment.service.impl.AnnouncementAttachmentCurrentServiceImpl(
                    session.getMapper(com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentCurrentDao.class),
                    session.getMapper(AnnouncementSourceDao.class));
        }
        @Bean AnnouncementAttachmentJobService service(AnnouncementAttachmentJobDao dao) {
            return new AnnouncementAttachmentJobServiceImpl(dao, new ObjectMapper());
        }
    }
}
