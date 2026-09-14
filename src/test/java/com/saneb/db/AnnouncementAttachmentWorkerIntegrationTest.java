package com.saneb.db;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.common.error.GlobalExceptionHandler;
import com.saneb.domain.announcementattachment.controller.AnnouncementAttachmentController;
import com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentJobDao;
import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementattachment.dto.*;
import com.saneb.domain.announcementattachment.extraction.*;
import com.saneb.domain.announcementattachment.service.*;
import com.saneb.domain.announcementattachment.service.impl.AnnouncementAttachmentWorkerServiceImpl;
import com.saneb.domain.announcementattachment.vo.*;
import com.saneb.domain.announcementattachment.worker.*;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient;
import com.saneb.domain.announcementsource.service.AnnouncementSourceRuleReleaseService;
import com.saneb.domain.auth.vo.*;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** HTTP만 합성 응답. 실제 worker/격리 parser/PG/MyBatis/검수 및 HTTP 직렬화를 연결한다. 운영/권한 E2E는 아니다. */
@EnabledIfEnvironmentVariable(named="SANEB_ATTACHMENT_WORKER_QA", matches="true")
@Timeout(value=3, unit=TimeUnit.MINUTES)
class AnnouncementAttachmentWorkerIntegrationTest {
    private static EmbeddedPostgres postgres;
    private static AnnotationConfigApplicationContext context;
    private static JdbcTemplate sql;
    private static UUID actor, release;
    private static AttachmentRuntimeIdentity runtime;
    private static AttachmentExecutionSnapshot execution;
    private static String distribution;
    private static final BizInfoAttachmentDiscoveryProfile PROFILE = new BizInfoAttachmentDiscoveryProfile();
    private static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();
    @TempDir Path directory;
    private UUID policy;
    private FixtureClient client;
    private CountingExtractor extractor;
    private AnnouncementAttachmentWorkerService worker;

    @BeforeAll static void start() throws Exception {
        distribution = System.getProperty("saneb.attachment-qa.extractor-root");
        assertThat(distribution).as("설치된 격리 추출기 경로가 필요합니다").isNotBlank();
        runtime = new AttachmentRuntimeIdentity(distribution);
        var installed = runtime.selectIdentity(); // Windows/설치 불가에서 DB 시작을 시도하지 않는다.
        execution = new AttachmentExecutionSnapshot(PROFILE.selectProfileCode(), PROFILE.selectProfileHash(),
                "attachment-1.0.0", installed.extractorVersion(), installed.configHash());
        postgres = EmbeddedPostgres.builder().setPort(0).setServerConfig("listen_addresses", "127.0.0.1").start();
        try {
            DataSource data = postgres.getPostgresDatabase();
            Flyway.configure().dataSource(data).locations("classpath:db/migration").load().migrate();
            sql = new JdbcTemplate(data);
            context = new AnnotationConfigApplicationContext();
            context.registerBean(DataSource.class, () -> data);
            context.register(AnnouncementAttachmentJobIntegrationTest.TestConfiguration.class);
            context.refresh();
            actor = UUID.randomUUID();
            sql.update("INSERT INTO users(id,login_id,password_hash,name,status_code,password_reset_required) VALUES (?,?,?,'worker 합성 QA','ACTIVE',false)",
                    actor, "worker-flow-fixture", "unused-fixture-hash");
            release = sql.queryForObject("SELECT id FROM announcement_source_classification_rule_releases ORDER BY version_no LIMIT 1", UUID.class);
            var rule = context.getBean(AnnouncementSourceRuleReleaseService.class).selectRuleValidationDetails(release);
            sql.update("UPDATE announcement_source_classification_rule_releases SET release_status_code='ACTIVE',activated_at=now(),rule_snapshot_hash=? WHERE id=?",
                    rule.calculatedSnapshotHash(), release);
        } catch (Exception exception) { stop(); throw exception; }
    }
    @AfterAll static void stop() throws Exception {
        try { if (context != null) context.close(); }
        finally { if (postgres != null) postgres.close(); }
    }
    @BeforeEach void prepare() throws Exception {
        // 이 클래스가 직접 만든 임시 loopback DB만 초기화한다. 앞 사례 실패의 대기 job이 다음 사례를 점유하지 않게 한다.
        sql.update("DELETE FROM announcement_source_links");
        sql.update("DELETE FROM announcement_source_snapshots");
        execution=new AttachmentExecutionSnapshot(PROFILE.selectProfileCode(),PROFILE.selectProfileHash(),execution.engineVersion(),execution.extractorVersion(),execution.extractorConfigHash());
        policy = insertPolicy("ENFORCE");
        client = new FixtureClient(); extractor = new CountingExtractor(); worker = selectWorker();
    }
    private UUID insertPolicy(String mode) throws Exception {
        sql.update("UPDATE announcement_attachment_policies SET policy_status_code='RETIRED',row_version=row_version+1 WHERE policy_status_code='ACTIVE'");
        UUID id = UUID.randomUUID();
        String settings = MAPPER.writeValueAsString(new AttachmentPolicyResponses.Configuration(execution.engineVersion(),execution.extractorVersion(),
                execution.extractorConfigHash(),83886080L,execution.roleRuleVersion(),execution.roleRulesHash()));
        String manifest = MAPPER.writeValueAsString(List.of(Map.of("providerCode", "BIZINFO", "profileCode", PROFILE.selectProfileCode(), "profileHash", PROFILE.selectProfileHash())));
        sql.update("INSERT INTO announcement_attachment_policies(id,policy_code,version_no,policy_status_code,mode_code,rule_release_id,policy_hash,settings_json,profile_manifest_json,created_by,published_at) VALUES (?,?,1,'ACTIVE',?,?,repeat('d',64),CAST(? AS jsonb),CAST(? AS jsonb),?,now())",
                id, id.toString(), mode, release, settings, manifest, actor);
        return id;
    }
    @AfterEach void cleanup() throws Exception { if (client != null) client.close(); }
    private <T> T bean(Class<T> type) { return context.getBean(type); }
    private AnnouncementAttachmentWorkerService selectWorker() {
        return new AnnouncementAttachmentWorkerServiceImpl(bean(AnnouncementAttachmentJobService.class), bean(AnnouncementAttachmentEvidenceService.class),
                bean(AnnouncementAttachmentEvaluationService.class), bean(AnnouncementAttachmentRetryService.class), new AttachmentDiscoveryProfileRegistry(List.of(PROFILE)),
                runtime, new AttachmentTemporaryStorage(directory.toString()), new AttachmentDownloadGateway(bean(AnnouncementAttachmentJobService.class), client),
                new AttachmentFileTypeValidator(), extractor, MAPPER);
    }
    private AttachmentJobReservation selectRequest() { return selectRequest("UNAVAILABLE", true); }
    private AttachmentJobReservation selectRequest(String bodyAvailability, boolean enforce) {
        UUID source = UUID.randomUUID(), content = UUID.randomUUID(), base = UUID.randomUUID();
        String notice = "PBLN_" + String.format(Locale.ROOT, "%015d", Math.floorMod(source.getLeastSignificantBits(), 999999999999999L));
        String title = "소상공인 지원금 worker QA " + source;
        sql.update("INSERT INTO announcement_source_snapshots(id,provider_code,provider_notice_id,title,raw_hash,semantic_status_code,is_attachment_review_required,attachment_policy_id) VALUES (?,'BIZINFO',?,?,?,'REVIEW_REQUIRED',?,?)",
                source, notice, title, source.toString().replace("-", "").repeat(2), enforce, enforce ? policy : null);
        sql.update("INSERT INTO announcement_source_content_versions(id,source_id,raw_hash,title,body_source_code,body_availability_code,collected_at) VALUES (?,?,repeat('a',64),?,'NONE',?,now())", content, source, title, bodyAvailability);
        sql.update("INSERT INTO announcement_source_classification_evaluations(id,source_id,content_version_id,rule_release_id,engine_version,body_source_code,body_availability_code,title_stage_code,body_stage_code,decision_status_code,reason_code,is_current) VALUES (?,?,?,?,'fixture','NONE',?,'COMBINATION_MATCHED',?,'REVIEW_REQUIRED',?,true)",
                base, source, content, release, bodyAvailability, bodyAvailability, "FETCH_FAILED".equals(bodyAvailability) ? "BODY_FETCH_FAILED" : "BODY_UNAVAILABLE");
        return new AttachmentJobReservation(source, policy, base, 0, 0, UUID.randomUUID(), execution);
    }
    private AttachmentJobRow reserve(AttachmentJobReservation request) { return bean(AnnouncementAttachmentJobService.class).insertAttachmentJob(request); }
    private AttachmentEvidenceResponses.SetSummary set(UUID source) { return bean(AnnouncementAttachmentReadService.class).selectAttachmentSetList(source, 1, 20).items().getFirst(); }
    private List<AttachmentEvidenceResponses.FileSummary> files(UUID source) { return bean(AnnouncementAttachmentReadService.class).selectAttachmentFileList(source, set(source).setId(), 1, 20).items(); }
    private Authentication auth() {
        var principal = new AuthenticatedUserDetails(new AuthUserDetailsRow(actor, "worker-flow-fixture", "unused", "합성 QA", "ACTIVE", false, null, null, null), List.of("ADMIN"));
        return UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities());
    }
    private AttachmentReviewRequests.Confirmation review(UUID source) {
        var state = bean(AnnouncementAttachmentReviewService.class).selectReviewContextDetails(source);
        return new AttachmentReviewRequests.Confirmation(state.version(), List.of("BUSINESS", "PERSONAL"), List.of("POLICY_FINANCE"),
                state.manualSourceCheckRequired() ? "MANUAL_SOURCE_CHECK" : "EXTRACTED_TEXT", state.requiredAcknowledgementCodes(), "합성 입력의 전체 파일 확인");
    }
    private void assertTemporaryEmpty() throws Exception { assertTemporaryEmpty(true); }
    private void assertTemporaryEmpty(boolean workspaceCreated) throws Exception {
        try (var paths = Files.walk(directory)) {
            var files = paths.filter(Files::isRegularFile).map(path -> path.getFileName().toString()).toList();
            if (workspaceCreated) assertThat(files).containsExactlyInAnyOrder(".owner", ".quota.lock");
            else assertThat(files).isEmpty();
        }
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_attachment_resource_leases", Integer.class)).isZero();
    }

    @Test void actualWorkerStoresAllThreeFormatsAndUnsupportedFileThenHttpReadsExactEvidence() throws Exception {
        var request = selectRequest(); var job = reserve(request);
        assertThat(worker.saveNextAttachmentJob().statusCode()).isEqualTo("EVALUATED");
        // 미지원 파일도 처리 분모에 남는다. PDF/HWP/HWPX 추출 성공이 전체 작업 성공은 아니다.
        assertThat(bean(AnnouncementAttachmentJobDao.class).selectJobDetails(job.jobId()).jobStatusCode()).isEqualTo("PARTIAL_FAILED");
        var stored = set(request.sourceId());
        assertThat(stored.setStatusCode()).isEqualTo("SEALED");assertThat(stored.discoveredCount()).isEqualTo(4);assertThat(stored.processedCount()).isEqualTo(4);
        assertThat(files(request.sourceId())).extracting(AttachmentEvidenceResponses.FileSummary::qualityCode).containsExactly("COMPLETE_TEXT", "COMPLETE_TEXT", "COMPLETE_TEXT", null);
        assertThat(files(request.sourceId()).getLast().downloadStatusCode()).isEqualTo("BLOCKED");
        assertThat(client.requests).isEqualTo(4);assertThat(extractor.calls).isEqualTo(3);
        var hwp = files(request.sourceId()).get(1);
        var blocks = bean(AnnouncementAttachmentReadService.class).selectAttachmentBlockList(request.sourceId(), hwp.extractionId(), 1, 10, 0, 2000);
        assertThat(blocks.items()).extracting(AttachmentEvidenceResponses.Block::text).anyMatch(text -> text.contains("소상공인 지원금"));
        // 실제 Controller/Service/DB 응답·직렬화 검사. standalone MockMvc는 역할/CSRF 필터 E2E가 아니다.
        var http = MockMvcBuilders.standaloneSetup(new AnnouncementAttachmentController(bean(AnnouncementAttachmentReadService.class)))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        http.perform(get("/api/v2/admin/announcement-sources/{source}/attachment-sets/{set}/files", request.sourceId(), stored.setId()))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.success").value(true)).andExpect(jsonPath("$.data.totalCount").value(4));
        http.perform(get("/api/v2/admin/announcement-sources/{source}/attachment-extractions/{extraction}/blocks", UUID.randomUUID(), hwp.extractionId()))
                .andExpect(status().isNotFound());
        assertTemporaryEmpty();
    }
    @Test void roleChangeStalesConfirmationAndDraftRequiresFreshReviewWithoutAutomaticActivation() throws Exception {
        var request = selectRequest(); reserve(request);assertThat(worker.saveNextAttachmentJob().statusCode()).isEqualTo("EVALUATED");
        var reviews = bean(AnnouncementAttachmentReviewService.class);
        var old = reviews.insertConfirmation(auth(), request.sourceId(), UUID.randomUUID(), review(request.sourceId()));
        var oldVersion = reviews.selectReviewContextDetails(request.sourceId()).version();
        var roles = files(request.sourceId()).stream().map(file -> new AttachmentRoleRequest.FileRole(file.fileId(), file.qualityCode() == null ? "REFERENCE" : "NOTICE")).toList();
        int networkBefore = client.requests, extractionBefore = extractor.calls;
        bean(AnnouncementAttachmentRoleService.class).insertRoleChange(auth(), request.sourceId(), UUID.randomUUID(),
                new AttachmentRoleRequest(oldVersion, set(request.sourceId()).setId(), roles, "역할 근거 재확인"));
        assertThat(selectWorker().saveNextAttachmentJob().statusCode()).isEqualTo("EVALUATED");
        assertThat(client.requests).isEqualTo(networkBefore);assertThat(extractor.calls).isEqualTo(extractionBefore);
        assertThat(reviews.selectReviewContextDetails(request.sourceId()).confirmedClassification()).isNull();
        assertThatThrownBy(() -> reviews.insertOperationalAnnouncement(auth(), request.sourceId(), new AttachmentReviewRequests.Conversion(oldVersion, old.confirmationId(), "BUSINESS", null)))
                .isInstanceOf(com.saneb.common.error.ApiException.class);
        var confirmed = reviews.insertConfirmation(auth(), request.sourceId(), UUID.randomUUID(), review(request.sourceId()));
        var conversion = new AttachmentReviewRequests.Conversion(reviews.selectReviewContextDetails(request.sourceId()).version(), confirmed.confirmationId(), "BUSINESS", null);
        var draft = reviews.insertOperationalAnnouncement(auth(), request.sourceId(), conversion);
        assertThat(reviews.insertOperationalAnnouncement(auth(), request.sourceId(), conversion)).isEqualTo(draft);
        assertThat(sql.queryForObject("SELECT approval_status_code FROM announcements WHERE id=?", String.class, draft.announcementId())).isEqualTo("DRAFT");
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_source_links WHERE source_id=?", Integer.class, request.sourceId())).isEqualTo(1);
        assertTemporaryEmpty();
    }
    @Test void ocrPartialEncryptionAndDownloadFailureRemainVisibleAndRequireReview() throws Exception {
        client.samples = List.of(new Sample("AR-002", "scan.pdf"), new Sample("AR-007", "partial.hwpx"), new Sample("AR-004", "encrypted.hwp"), new Sample("AR-001", "blocked.pdf"));
        client.failureIndex = 3;client.failureCode = "ATTACHMENT_HTTP_404";
        var request = selectRequest();reserve(request);assertThat(worker.saveNextAttachmentJob().statusCode()).isEqualTo("EVALUATED");
        assertThat(files(request.sourceId())).extracting(AttachmentEvidenceResponses.FileSummary::qualityCode).containsExactly("OCR_REQUIRED", "PARTIAL_TEXT", "ENCRYPTED", null);
        assertThat(files(request.sourceId()).getLast().downloadErrorCode()).isEqualTo("DOWNLOAD_BLOCKED");
        var state = bean(AnnouncementAttachmentReviewService.class).selectReviewContextDetails(request.sourceId());
        assertThat(state.manualSourceCheckRequired()).isTrue();assertThat(state.requiredAcknowledgementCodes()).isNotEmpty();
        assertThat(sql.queryForObject("SELECT decision_status_code FROM announcement_source_attachment_evaluations WHERE source_id=? AND is_current", String.class, request.sourceId())).isEqualTo("REVIEW_REQUIRED");
        assertTemporaryEmpty();
    }
    @Test void restartedWorkerReusesCommittedCheckpointsAndOnlyDownloadsFailedFileAgain() throws Exception {
        client.samples = List.of(new Sample("AR-003", "first.hwp"), new Sample("AR-006", "second.hwpx"));client.failureIndex = 1;
        var request = selectRequest();var job = reserve(request);
        assertThat(worker.saveNextAttachmentJob().statusCode()).isEqualTo("HTTP_SERVER_ERROR");
        assertThat(bean(AnnouncementAttachmentJobDao.class).selectJobDetails(job.jobId()).jobStatusCode()).isEqualTo("RETRY_WAIT");
        assertThat(bean(AnnouncementAttachmentReadService.class).selectAttachmentSetList(request.sourceId(), 1, 20).items()).isEmpty();
        assertTemporaryEmpty();
        client.failureIndex = -1;
        sql.update("UPDATE announcement_attachment_jobs SET next_attempt_at=clock_timestamp()-interval '1 second' WHERE id=?", job.jobId());
        assertThat(selectWorker().saveNextAttachmentJob().statusCode()).isEqualTo("EVALUATED");
        assertThat(client.fileRequests.get(0)).isEqualTo(1);assertThat(client.fileRequests.get(1)).isEqualTo(2);
        assertThat(extractor.calls).isEqualTo(2);assertThat(files(request.sourceId())).hasSize(2);
        assertTemporaryEmpty();
    }
    @Test void sourceDeletedDuringRealExtractionCannotBeRecreatedByLateWorkerResult() throws Exception {
        client.samples = List.of(new Sample("AR-006", "notice.hwpx"));
        var request = selectRequest();reserve(request);
        extractor.afterExtraction = () -> sql.update("DELETE FROM announcement_source_snapshots WHERE id=?", request.sourceId());
        assertThat(worker.saveNextAttachmentJob().statusCode()).isEqualTo("DEFERRED");
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_source_snapshots WHERE id=?", Integer.class, request.sourceId())).isZero();
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_attachment_jobs WHERE source_id=?", Integer.class, request.sourceId())).isZero();
        assertTemporaryEmpty();
    }

    @Test void bodyFetchFailureStillCollectsAttachmentsAndPreservesBaseFailureEvidence() throws Exception {
        var request = selectRequest("FETCH_FAILED", true);
        var baseBefore = selectBase(request);
        reserve(request);
        assertThat(worker.saveNextAttachmentJob().statusCode()).isEqualTo("EVALUATED");
        assertThat(files(request.sourceId()).stream().filter(file -> "COMPLETE_TEXT".equals(file.qualityCode())).count()).isEqualTo(3);
        assertThat(client.requests).isEqualTo(4);assertThat(extractor.calls).isEqualTo(3);
        assertThat(selectBase(request)).isEqualTo(baseBefore);
        assertThat(baseBefore.get("body_availability_code")).isEqualTo("FETCH_FAILED");
        assertThat(baseBefore.get("reason_code")).isEqualTo("BODY_FETCH_FAILED");
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_source_links WHERE source_id=?", Integer.class, request.sourceId())).isZero();
        assertTemporaryEmpty();
    }

    @Test void titleExclusionAfterReservationStopsWorkerBeforeAnyRequestOrExtraction() throws Exception {
        var request = selectRequest();var job = reserve(request);
        sql.update("UPDATE announcement_source_snapshots SET semantic_status_code='EXCLUDED' WHERE id=?", request.sourceId());
        assertThat(worker.saveNextAttachmentJob().statusCode()).isEqualTo("IDLE");
        assertThat(bean(AnnouncementAttachmentJobDao.class).selectJobDetails(job.jobId()).jobStatusCode()).isEqualTo("CONFLICT");
        assertThat(client.requests).isZero();assertThat(extractor.calls).isZero();
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_source_attachment_sets WHERE source_id=?", Integer.class, request.sourceId())).isZero();
        assertThatThrownBy(() -> bean(AnnouncementAttachmentReadService.class).selectAttachmentSetList(request.sourceId(), 1, 20))
                .isInstanceOf(com.saneb.common.error.ApiException.class);
        assertTemporaryEmpty(false);
    }

    @Test void collectOnlyStoresPreviewWithoutBindingReviewOrChangingBaseOrCreatingAnnouncement() throws Exception {
        policy = insertPolicy("COLLECT_ONLY");
        var request = selectRequest("UNAVAILABLE", false);var baseBefore = selectBase(request);
        int announcementsBefore = sql.queryForObject("SELECT count(1) FROM announcements", Integer.class);
        reserve(request);assertThat(worker.saveNextAttachmentJob().statusCode()).isEqualTo("EVALUATED");
        assertThat(files(request.sourceId())).hasSize(4);
        assertThat(selectBase(request)).isEqualTo(baseBefore);
        assertThat(sql.queryForObject("SELECT is_attachment_review_required FROM announcement_source_snapshots WHERE id=?", Boolean.class, request.sourceId())).isFalse();
        assertThat(sql.queryForObject("SELECT attachment_policy_id FROM announcement_source_snapshots WHERE id=?", UUID.class, request.sourceId())).isNull();
        assertThat(sql.queryForObject("SELECT classification_row_version FROM announcement_source_snapshots WHERE id=?", Integer.class, request.sourceId())).isZero();
        assertThat(sql.queryForObject("SELECT count(1) FROM announcements", Integer.class)).isEqualTo(announcementsBefore);
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_source_links WHERE source_id=?", Integer.class, request.sourceId())).isZero();
        assertTemporaryEmpty();
    }

    private void enableRoleRules() throws Exception {
        execution=new AttachmentExecutionSnapshot(execution.profileCode(),execution.profileHash(),execution.engineVersion(),execution.extractorVersion(),execution.extractorConfigHash(),
                com.saneb.domain.announcementattachment.classification.AttachmentDocumentRoleClassifier.VERSION,
                com.saneb.domain.announcementattachment.classification.AttachmentDocumentRoleClassifier.RULES_HASH);
        policy=insertPolicy("ENFORCE");
    }
    @Test void textRoleRulesBindActualExtractionAndManualCopyPreservesOriginalAutomaticEvidence() throws Exception {
        enableRoleRules();client.samples=List.of(new Sample("AR-006","arbitrary.hwpx"));client.roleDocument=selectRoleDocument();
        var request=selectRequest();reserve(request);
        assertThat(worker.saveNextAttachmentJob().statusCode()).isEqualTo("EVALUATED");
        var original=files(request.sourceId()).getFirst();
        assertThat(original.documentRoleCode()).isEqualTo("NOTICE");assertThat(original.roleOriginCode()).isEqualTo("TEXT_RULE");
        assertThat(original.roleExtractionId()).isEqualTo(original.extractionId());
        assertThat(original.roleAssessment().evidence()).hasSize(4);
        assertThat(sql.queryForObject("SELECT attachment_role_blocks_hash(blocks_json) FROM announcement_source_attachment_extractions WHERE id=?",String.class,original.extractionId()))
                .isEqualTo(original.roleAssessment().blocksHash());
        var http=MockMvcBuilders.standaloneSetup(new AnnouncementAttachmentController(bean(AnnouncementAttachmentReadService.class)))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        http.perform(get("/api/v2/admin/announcement-sources/{source}/attachment-sets/{set}/files",request.sourceId(),set(request.sourceId()).setId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.items[0].roleOriginCode").value("TEXT_RULE"))
                .andExpect(jsonPath("$.data.items[0].roleAssessment.roleCode").value("NOTICE"))
                .andExpect(jsonPath("$.data.items[0].roleExtractionId").value(original.extractionId().toString()));
        var state=bean(AnnouncementAttachmentReviewService.class).selectReviewContextDetails(request.sourceId());
        bean(AnnouncementAttachmentRoleService.class).insertRoleChange(auth(),request.sourceId(),UUID.randomUUID(),
                new AttachmentRoleRequest(state.version(),set(request.sourceId()).setId(),List.of(new AttachmentRoleRequest.FileRole(original.fileId(),"REFERENCE")),"합성 문서의 관리자 역할 수정"));
        assertThat(selectWorker().saveNextAttachmentJob().statusCode()).isEqualTo("EVALUATED");
        var copy=files(request.sourceId()).getFirst();
        assertThat(copy.documentRoleCode()).isEqualTo("REFERENCE");assertThat(copy.roleOriginCode()).isEqualTo("MANUAL");
        assertThat(copy.roleAssessment()).isEqualTo(original.roleAssessment());
        assertThat(copy.roleExtractionId()).isEqualTo(copy.extractionId()).isNotEqualTo(original.extractionId());
        assertThat(copy.reusedFromExtractionId()).isEqualTo(original.extractionId());
        assertThat(client.requests).isEqualTo(2);assertThat(extractor.calls).isEqualTo(1);
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_source_links WHERE source_id=?",Integer.class,request.sourceId())).isZero();
        assertTemporaryEmpty();
    }
    @Test void restartedWorkerPreservesTextRoleCheckpointAndDoesNotInferSuccessFromIncompleteEvidence() throws Exception {
        enableRoleRules();client.samples=List.of(new Sample("AR-003","first.hwp"),new Sample("AR-006","second.hwpx"));client.failureIndex=1;
        var request=selectRequest();var job=reserve(request);
        assertThat(worker.saveNextAttachmentJob().statusCode()).isEqualTo("HTTP_SERVER_ERROR");
        client.failureIndex=-1;
        sql.update("UPDATE announcement_attachment_jobs SET next_attempt_at=clock_timestamp()-interval '1 second' WHERE id=?",job.jobId());
        assertThat(selectWorker().saveNextAttachmentJob().statusCode()).isEqualTo("EVALUATED");
        assertThat(client.fileRequests.get(0)).isEqualTo(1);assertThat(extractor.calls).isEqualTo(2);
        assertThat(files(request.sourceId())).allSatisfy(file->{
            assertThat(file.roleOriginCode()).isEqualTo("TEXT_RULE");assertThat(file.documentRoleCode()).isEqualTo("UNKNOWN");
            assertThat(file.roleAssessment()).isNotNull();assertThat(file.roleExtractionId()).isEqualTo(file.extractionId());
        });
        assertThat(sql.queryForObject("SELECT decision_status_code FROM announcement_source_attachment_evaluations WHERE source_id=? AND is_current",String.class,request.sourceId())).isEqualTo("REVIEW_REQUIRED");
        assertTemporaryEmpty();
    }
    @Test void selectedPartialRetryInfersFreshRoleAndCopiesUnselectedAssessmentWithoutDownloadingAgain() throws Exception {
        enableRoleRules();client.samples=List.of(new Sample("AR-006","first.hwpx"),new Sample("AR-007","second.hwpx"));
        var request=selectRequest();reserve(request);assertThat(worker.saveNextAttachmentJob().statusCode()).isEqualTo("EVALUATED");
        var before=files(request.sourceId());assertThat(before.get(1).qualityCode()).isEqualTo("PARTIAL_TEXT");
        assertThat(before.get(1).roleAssessment()).isNull();
        var state=bean(AnnouncementAttachmentReviewService.class).selectReviewContextDetails(request.sourceId());
        bean(AnnouncementAttachmentRetryService.class).insertFileRetry(auth(),request.sourceId(),UUID.randomUUID(),new AttachmentRetryRequest(
                state.version(),set(request.sourceId()).setId(),List.of(before.get(1).fileId()),83886080L,"합성 부분 추출 파일만 재시도"));
        client.roleDocument=selectRoleDocument();
        assertThat(selectWorker().saveNextAttachmentJob().statusCode()).isEqualTo("EVALUATED");
        var after=files(request.sourceId());
        assertThat(after.getFirst().roleAssessment()).isEqualTo(before.getFirst().roleAssessment());
        assertThat(after.getFirst().reusedFromExtractionId()).isEqualTo(before.getFirst().extractionId());
        assertThat(after.getFirst().roleExtractionId()).isEqualTo(after.getFirst().extractionId());
        assertThat(after.get(1).documentRoleCode()).isEqualTo("NOTICE");assertThat(after.get(1).roleOriginCode()).isEqualTo("TEXT_RULE");
        assertThat(after.get(1).roleAssessment().roleCode()).isEqualTo("NOTICE");assertThat(after.get(1).reusedFromExtractionId()).isNull();
        assertThat(client.fileRequests.get(0)).isEqualTo(1);assertThat(client.fileRequests.get(1)).isEqualTo(2);assertThat(extractor.calls).isEqualTo(3);
        assertTemporaryEmpty();
    }
    @Test void spacedFormMarkersPersistThroughWorkerDbAndApiWithoutBecomingPrimaryEvidence() throws Exception {
        enableRoleRules();client.samples=List.of(new Sample("AR-006","공고.hwpx"));
        client.roleDocument=selectRoleDocument(List.of("😀 지원 신청서", "성   명", "( 서명 또는 인 )", "소상공인 지원금"));
        var request=selectRequest();var baseBefore=selectBase(request);reserve(request);
        assertThat(worker.saveNextAttachmentJob().statusCode()).isEqualTo("EVALUATED");
        var file=files(request.sourceId()).getFirst();
        assertThat(file.documentRoleCode()).isEqualTo("FORM");assertThat(file.roleOriginCode()).isEqualTo("TEXT_RULE");
        assertThat(file.roleAssessment().ruleVersion()).isEqualTo("document-role-1.0.2");
        assertThat(file.roleAssessment().evidence()).extracting(com.saneb.domain.announcementattachment.classification.AttachmentDocumentRoleClassifier.Evidence::ruleCode)
                .containsExactly("FORM_HEADING","APPLICANT_FIELD","SIGNATURE_FIELD");
        assertThat(file.roleExtractionId()).isEqualTo(file.extractionId());
        assertThat(sql.queryForObject("SELECT attachment_role_blocks_hash(blocks_json) FROM announcement_source_attachment_extractions WHERE id=?",String.class,file.extractionId()))
                .isEqualTo(file.roleAssessment().blocksHash());
        var http=MockMvcBuilders.standaloneSetup(new AnnouncementAttachmentController(bean(AnnouncementAttachmentReadService.class)))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        http.perform(get("/api/v2/admin/announcement-sources/{source}/attachment-sets/{set}/files",request.sourceId(),set(request.sourceId()).setId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.items[0].roleAssessment.roleCode").value("FORM"))
                .andExpect(jsonPath("$.data.items[0].roleAssessment.ruleVersion").value("document-role-1.0.2"));
        assertThat(sql.queryForObject("SELECT reason_code FROM announcement_source_attachment_evaluations WHERE source_id=? AND is_current",String.class,request.sourceId()))
                .isEqualTo("EXTENDED_COMBINATION_NOT_CONFIRMED");
        assertThat(selectBase(request)).isEqualTo(baseBefore);
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_source_links WHERE source_id=?",Integer.class,request.sourceId())).isZero();
        assertThat(client.requests).isEqualTo(2);assertThat(extractor.calls).isEqualTo(1);assertTemporaryEmpty();
    }
    private static byte[] selectRoleDocument() throws IOException {
        return selectRoleDocument(List.of("😀 지원사업 공고","지원대상: 소상공인","지원내용: 지원금","신청기간: 9월"));
    }
    private static byte[] selectRoleDocument(List<String> lines) throws IOException {
        // 실제 HWPX 바이트를 격리 parser에 넣는다. 공식 사이트 파일 QA 증거와는 구분한다.
        var bytes=new java.io.ByteArrayOutputStream();
        try(var zip=new java.util.zip.ZipOutputStream(bytes)) {
            String opening="<hs:sec xmlns:hs=\"http://www.hancom.co.kr/hwpml/2011/section\" xmlns:hp=\"http://www.hancom.co.kr/hwpml/2011/paragraph\">";
            StringBuilder xml=new StringBuilder(opening);
            for(String line:lines)
                xml.append("<hp:p><hp:run><hp:t>").append(line).append("</hp:t></hp:run></hp:p>");
            for(var entry:Map.of("mimetype","application/hwp+zip","Contents/section0.xml",xml.append("</hs:sec>").toString()).entrySet()) {
                var item=new java.util.zip.ZipEntry(entry.getKey());item.setTimeLocal(java.time.LocalDateTime.of(2020,1,1,0,0));
                zip.putNextEntry(item);zip.write(entry.getValue().getBytes(StandardCharsets.UTF_8));zip.closeEntry();
            }
        }
        return bytes.toByteArray();
    }
    private Map<String,Object> selectBase(AttachmentJobReservation request) {
        return sql.queryForMap("SELECT id,content_version_id,body_source_code,body_availability_code,title_stage_code,body_stage_code,decision_status_code,reason_code,is_current FROM announcement_source_classification_evaluations WHERE id=?",
                request.expectedBaseDecisionId());
    }

    record Sample(String id, String name) { }
    private final class CountingExtractor extends IsolatedAttachmentExtractor {
        int calls; Runnable afterExtraction = () -> { };
        CountingExtractor() { super(MAPPER, distribution); }
        @Override public com.fasterxml.jackson.databind.JsonNode selectExtraction(Path input) throws IOException {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();calls++;
            var result = super.selectExtraction(input);afterExtraction.run();return result;
        }
    }
    static final class FixtureClient extends AttachmentPinnedDownloadClient {
        List<Sample> samples = List.of(new Sample("AR-001", "notice.pdf"), new Sample("AR-003", "guide.hwp"), new Sample("AR-006", "form.hwpx"), new Sample(null, "unsupported.xlsx"));
        int requests, failureIndex = -1;String failureCode = "ATTACHMENT_HTTP_503";byte[] roleDocument;
        final Map<Integer,Integer> fileRequests = new HashMap<>();
        @Override public Download selectDownload(Request request, Set<String> hosts, Predicate<Request> approved, Path output, long maximum, ByteReservation bytes) throws IOException {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            assertThat(hosts).containsExactly("www.bizinfo.go.kr");assertThat(approved.test(request)).isTrue();requests++;
            byte[] content;String type;
            if (request.uri().getPath().contains("selectSIIA")) {
                StringBuilder page = new StringBuilder("<html><meta property='og:title' content='합성 worker QA'><div class='attached_file_list'><ul>");
                for (int i=0; i<samples.size(); i++) page.append("<li><span class='file_name'>").append(samples.get(i).name()).append("</span><a href='/cmm/fms/fileDown.do?atchFileId=FILE_000000000000001&amp;fileSn=").append(i).append("'>다운로드</a></li>");
                content = page.append("</ul></div></html>").toString().getBytes(StandardCharsets.UTF_8);type = "text/html;charset=UTF-8";
            } else {
                int index = Integer.parseInt(request.uri().getQuery().split("fileSn=")[1]);fileRequests.merge(index, 1, Integer::sum);
                if (index == failureIndex) throw new IOException(failureCode);
                var sample = samples.get(index);assertThat(sample.id()).isNotNull();
                if(roleDocument!=null) content=roleDocument;
                else try (var input = AnnouncementAttachmentWorkerIntegrationTest.class.getResourceAsStream("/attachment-runtime-qa/" + sample.id() + ".bin")) {
                    if (input == null) throw new IOException("FIXTURE_MISSING");content = input.readAllBytes();
                }
                type = "application/octet-stream";
            }
            assertThat((long)content.length).isLessThanOrEqualTo(maximum);assertThat(bytes.reserve(content.length)).isTrue();
            Files.write(output, content, StandardOpenOption.CREATE_NEW);
            try { return new Download(content.length, HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content)), type); }
            catch (java.security.NoSuchAlgorithmException exception) { throw new IOException("HASH_UNAVAILABLE"); }
        }
    }
}
