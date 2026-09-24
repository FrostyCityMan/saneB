package com.saneb.db;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.saneb.common.error.GlobalExceptionHandler;
import com.saneb.domain.announcementattachment.controller.*;
import com.saneb.domain.announcementattachment.classification.AttachmentDocumentRoleClassifier;
import com.saneb.domain.announcementattachment.classification.AttachmentSegmentRoleAnalyzer;
import com.saneb.domain.announcementattachment.classification.AttachmentSegmentClassificationEngine;
import com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentSegmentDao;
import com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentJobDao;
import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementattachment.dto.*;
import com.saneb.domain.announcementattachment.extraction.*;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.ObservationCase;
import com.saneb.domain.announcementattachment.service.*;
import com.saneb.domain.announcementattachment.service.impl.AnnouncementAttachmentWorkerServiceImpl;
import com.saneb.domain.announcementattachment.vo.*;
import com.saneb.domain.announcementattachment.worker.*;
import com.saneb.domain.announcementsource.classification.*;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.*;
import com.saneb.domain.announcementsource.dao.AnnouncementSourceClassificationDao;
import com.saneb.domain.announcementsource.provider.AnnouncementSourceProviderItem;
import com.saneb.domain.announcementsource.provider.content.*;
import com.saneb.domain.announcementsource.service.*;
import com.saneb.domain.announcementsource.service.impl.AnnouncementSourceClassificationPersistenceServiceImpl;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.sql.DataSource;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.flywaydb.core.Flyway;

/** 공식 파일 → 실제 worker/격리 추출기/임시 DB/API. 정책 승인·운영·인증·브라우저 E2E 증거가 아니다. */
@EnabledIfEnvironmentVariable(named="SANEB_ATTACHMENT_OFFICIAL_WORKER_QA",matches="true")
class AnnouncementAttachmentOfficialWorkerIntegrationTest {
    private static final ObjectMapper JSON=new ObjectMapper().findAndRegisterModules().disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private static final long MIB=1024L*1024;
    private static EmbeddedPostgres postgres;
    private static AnnotationConfigApplicationContext context;
    private static JdbcTemplate sql;
    private static UUID actor,release;
    private static String distribution;
    private static AttachmentRuntimeIdentity runtime;
    @TempDir Path temporary;

    static Stream<ObservationCase> selectCases() {
        // 제목 제외 표본도 유지한다. 임의 URL·전체 기관 실행 모드는 제공하지 않는다.
        String group=System.getProperty("saneb.attachment-official-worker.group","YANGPYEONG");
        var expected=AnnouncementAttachmentOfficialWorkerProbe.selectCaseCodes(group);
        var samples=AnnouncementAttachmentBbsOfficialObservationTest.selectCases(AnnouncementAttachmentOfficialWorkerProbe.selectObservationGroup(group)).toList();
        assertEquals(expected,samples.stream().map(ObservationCase::code).toList(),"OFFICIAL_WORKER_CASES_CHANGED");
        return samples.stream();
    }
    @BeforeAll static void start() throws Exception {
        distribution=System.getProperty("saneb.attachment-qa.extractor-root");
        assertNotNull(distribution,"INSTALLED_EXTRACTOR_REQUIRED");
        runtime=new AttachmentRuntimeIdentity(distribution);runtime.selectIdentity();
        startDatabase();
    }
    static void startDatabase() throws Exception {
        postgres=EmbeddedPostgres.builder().setPort(0).setServerConfig("listen_addresses","127.0.0.1").start();
        try {
            DataSource data=postgres.getPostgresDatabase();
            Flyway.configure().dataSource(data).locations("classpath:db/migration").load().migrate();
            sql=new JdbcTemplate(data);context=new AnnotationConfigApplicationContext();
            context.registerBean(DataSource.class,()->data);
            context.register(AnnouncementAttachmentJobIntegrationTest.TestConfiguration.class);
            context.registerBean(AnnouncementSourceClassificationPersistenceService.class,()->
                    new AnnouncementSourceClassificationPersistenceServiceImpl(context.getBean(SqlSessionTemplate.class).getMapper(AnnouncementSourceClassificationDao.class)));
            context.refresh();actor=UUID.randomUUID();
            sql.update("INSERT INTO users(id,login_id,password_hash,name,status_code,password_reset_required) VALUES (?,?,?,'공식 파일 임시 QA','ACTIVE',false)",
                    actor,"official-worker-fixture","unused-fixture-hash");
            release=sql.queryForObject("SELECT id FROM announcement_source_classification_rule_releases ORDER BY version_no LIMIT 1",UUID.class);
            var validation=bean(AnnouncementSourceRuleReleaseService.class).selectRuleValidationDetails(release);
            // 직접 소유한 임시 DB의 실행용 fixture다. 게시 QA 통과나 운영 규칙 활성화를 만들지 않는다.
            sql.update("UPDATE announcement_source_classification_rule_releases SET release_status_code='ACTIVE',activated_at=now(),rule_snapshot_hash=? WHERE id=?",
                    validation.calculatedSnapshotHash(),release);
        } catch(Exception failure) {stop();throw failure;}
    }
    @AfterAll static void stop() throws Exception {
        try {if(context!=null)context.close();} finally {if(postgres!=null)postgres.close();}
    }
    @BeforeEach void isolateCase() {
        // 외부 접속 정보를 받지 않고 이 클래스가 직접 생성한 loopback DB만 정리한다.
        sql.update("DELETE FROM announcement_source_links");sql.update("DELETE FROM announcement_source_snapshots");
        sql.update("UPDATE announcement_attachment_policies SET policy_status_code='RETIRED',row_version=row_version+1 WHERE policy_status_code='ACTIVE'");
    }
    private static <T>T bean(Class<T> type){return context.getBean(type);}

    @ParameterizedTest(name="{0}") @MethodSource("selectCases") @Timeout(420)
    void officialFilesPassThroughWorkerDatabaseAndApi(ObservationCase sample) throws Exception {
        var report=new LinkedHashMap<String,Object>();var rows=new ArrayList<Map<String,Object>>();
        report.put("scope","OFFICIAL_WORKER_EPHEMERAL_DB_API_V1");report.put("caseCode",sample.code());report.put("observedAt",Instant.now().toString());
        report.put("status","INCOMPLETE");report.put("titleInputSource","FIXED_OFFICIAL_SAMPLE");report.put("policySource","EPHEMERAL_DB_FIXTURE");
        report.put("productionWriteCount",0);report.put("isPolicyQaPassed",false);report.put("isExpectationApproved",false);report.put("files",rows);
        report.put("isWholeTextAnalysisComplete",false);report.put("isAuthenticatedBrowserE2e",false);
        String stage="TITLE_GATE";boolean cleanup=false;
        try(var client=new OfficialClient(sample)) {
            try {
                var rules=bean(AnnouncementSourceRuleReleaseService.class).selectPublishedRuleSet(release);
                var engine=new AnnouncementSourceClassificationEngine();
                var title=engine.selectDecision(new AnnouncementSourceClassificationInput("LOCAL_GOV_NOTICE",sample.title(),null,null,List.of(),BodySourceCode.NONE,BodyAvailabilityCode.UNAVAILABLE),rules);
                report.put("titleStage",title.titleStageCode());report.put("titleReason",title.reasonCode());
                if(selectPlannedTitleStop(sample,title)) {
                    assertEquals(0,sql.queryForObject("SELECT count(1) FROM announcement_source_snapshots",Integer.class));
                    assertEquals(0,client.requests);report.put("status","TITLE_EXCLUDED_NOT_FETCHED");return;
                }
                assertTrue(AnnouncementAttachmentBbsOfficialObservationTest.selectTitleMayProceed(title),"TITLE_NOT_ELIGIBLE");
                stage="BODY_COLLECTION";client.reserveBody();
                UUID localSource=sql.queryForObject("SELECT id FROM local_government_notice_sources WHERE public_code=?",UUID.class,sample.source().localSourceCode());
                var body=new LocalGovernmentNoticeProviderContentClient(true,3000,7000,(int)MIB,0,1,"saneB-notice-collector/1.0")
                        .selectContent(new ProviderContentRequest("LOCAL_GOV_NOTICE",localSource,sample.listUrl(),sample.source().sourceUrl()));
                boolean bodyComplete=AnnouncementAttachmentBbsOfficialObservationTest.selectBodyComplete(body);
                report.put("bodyStatus",body.statusCode());report.put("bodyFailureCode",body.failureCode());report.put("bodyAttempts",body.attemptCount());
                report.put("bodyStageComplete",bodyComplete);
                var base=engine.selectDecision(new AnnouncementSourceClassificationInput("LOCAL_GOV_NOTICE",sample.title(),bodyComplete?body.bodyText():null,null,List.of(),body.bodySourceCode(),body.bodyAvailabilityCode()),rules);
                report.put("bodyDecision",base.semanticStatusCode());report.put("bodyReason",base.reasonCode());
                // BODY의 A/B/부족/실패 결과로 첨부를 끊지 않는다. 실제 결과를 저장해 worker의 입력으로 사용한다.
                assertTrue(AnnouncementAttachmentBbsOfficialObservationTest.selectTitleMayProceed(base),"TITLE_DECISION_CHANGED");
                stage="BASE_PERSISTENCE";var installed=runtime.selectIdentity();var profile=sample.profile();
                String group=System.getProperty("saneb.attachment-official-worker.group","YANGPYEONG");
                boolean segmentMode=AnnouncementAttachmentOfficialWorkerProbe.selectSegmentMode(group);
                var execution=selectExecution(group,sample,installed.extractorVersion(),installed.configHash());
                var request=insertSourceRequest(sample,bodyComplete?body.bodyText():null,base,execution);
                UUID source=request.sourceId();
                var job=bean(AnnouncementAttachmentJobService.class).insertAttachmentJob(request);
                var extractor=new ActualExtractor();
                var worker=new AnnouncementAttachmentWorkerServiceImpl(bean(AnnouncementAttachmentJobService.class),bean(AnnouncementAttachmentEvidenceService.class),
                        bean(AnnouncementAttachmentEvaluationService.class),bean(AnnouncementAttachmentRetryService.class),new AttachmentDiscoveryProfileRegistry(List.of(profile)),runtime,
                        new AttachmentTemporaryStorage(temporary.toString()),new AttachmentDownloadGateway(bean(AnnouncementAttachmentJobService.class),client),new AttachmentFileTypeValidator(),extractor,JSON);
                report.put("profileCode",profile.selectProfileCode());report.put("profileHash",profile.selectProfileHash());
                report.put("extractorVersion",execution.extractorVersion());report.put("extractorConfigHash",execution.extractorConfigHash());report.put("roleRuleVersion",execution.roleRuleVersion());
                if(segmentMode) {
                    report.put("engineVersion",execution.engineVersion());report.put("segmentRuleVersion",execution.segmentRuleVersion());
                    report.put("segmentRulesHash",execution.segmentRulesHash());
                }
                stage="ACTUAL_WORKER";var outcome=worker.saveNextAttachmentJob();report.put("workerStatus",outcome.statusCode());
                report.put("jobStatus",bean(AnnouncementAttachmentJobDao.class).selectJobDetails(job.jobId()).jobStatusCode());
                assertEquals("EVALUATED",outcome.statusCode(),"WORKER_NOT_EVALUATED");
                stage="DATABASE_API";var read=bean(AnnouncementAttachmentReadService.class);
                var set=read.selectAttachmentSetList(source,1,20).items().getFirst();
                var files=read.selectAttachmentFileList(source,set.setId(),1,20).items();
                report.put("discoveryComplete",set.discoveryComplete());report.put("discoveredFileCount",set.discoveredCount());report.put("processedFileCount",set.processedCount());
                assertTrue(set.discoveryComplete());assertEquals("SEALED",set.setStatusCode());
                assertEquals(sample.listedFileCount(),set.discoveredCount());assertEquals(set.discoveredCount(),set.processedCount());
                assertEquals(sample.listedFileCount(),files.size());
                var controllers=new ArrayList<Object>(List.of(new AnnouncementAttachmentController(read),new AnnouncementAttachmentCurrentController(bean(AnnouncementAttachmentCurrentService.class))));
                AnnouncementAttachmentSegmentService segments=segmentMode?selectSegmentService():null;
                if(segmentMode) {
                    controllers.add(new AnnouncementAttachmentSegmentController(segments));
                    controllers.add(new AnnouncementAttachmentReviewController(bean(AnnouncementAttachmentReviewService.class)));
                }
                var http=MockMvcBuilders.standaloneSetup(controllers.toArray())
                        .setControllerAdvice(new GlobalExceptionHandler())
                        .setMessageConverters(new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter(JSON)).build();
                var fileJson=selectApi(http,"/api/v2/admin/announcement-sources/"+source+"/attachment-sets/"+set.setId()+"/files");
                assertEquals(files.size(),fileJson.path("totalCount").asInt());
                for(int i=0;i<files.size();i++) {
                    var file=files.get(i);var row=new LinkedHashMap<String,Object>();rows.add(row);
                    row.put("format",file.detectedTypeCode());row.put("downloadStatus",file.downloadStatusCode());row.put("downloadErrorCode",file.downloadErrorCode());
                    row.put("bytes",file.downloadedBytes());row.put("binaryHash",file.binaryHash());row.put("quality",file.qualityCode());row.put("characterCount",file.characterCount());
                    row.put("roleCode",file.documentRoleCode());row.put("roleOrigin",file.roleOriginCode());
                    row.put("roleDiagnostic",selectRoleDiagnostic(file.roleAssessment()));
                    row.put("workerLocatorHash",sql.queryForObject("SELECT stable_locator_hash FROM announcement_source_attachment_files WHERE id=?",String.class,file.fileId()));
                    var safeLocator=JSON.readTree(sql.queryForObject("SELECT safe_locator_json::text FROM announcement_source_attachment_files WHERE id=?",String.class,file.fileId()));
                    row.put("locatorHash",selectCanonicalHash(safeLocator));
                    assertTrue(selectWireTree(file).equals(fileJson.path("items").get(i)),"API_FILE_PROJECTION_MISMATCH");
                    if(file.extractionId()!=null) {
                        var actual=extractor.byBinaryHash.get(file.binaryHash());assertNotNull(actual,"ACTUAL_EXTRACTION_MISSING");
                        assertEquals(actual.path("qualityCode").asText(),file.qualityCode());
                        long segmented=0,unreliable=0;
                        for (var block:actual.path("blocks")) {
                            if(block.path("locator").asText().contains(":segment:"))segmented++;
                            if(!block.path("scopeReliable").asBoolean())unreliable++;
                        }
                        row.put("segmentedParagraphBlockCount",segmented);row.put("unreliableBlockCount",unreliable);
                        String storedText=sql.queryForObject("SELECT extracted_text FROM announcement_source_attachment_extractions WHERE id=?",String.class,file.extractionId());
                        String expectedText=Set.of("COMPLETE_TEXT","PARTIAL_TEXT").contains(file.qualityCode())?actual.path("text").asText():null;
                        assertTrue(Objects.equals(expectedText,storedText),"PERSISTED_TEXT_MISMATCH");
                        row.put("textHash",storedText==null?null:hash(storedText));
                        row.put("blockCount",actual.path("blocks").size());
                        row.put("replacementCharacterCount",selectReplacementCharacterCount(storedText));
                        var hwpStructure=IsolatedAttachmentExtractor.selectHwpStructureDetails(actual);
                        if("HWP".equals(file.detectedTypeCode()))assertNotNull(hwpStructure,"HWP_STRUCTURE_DIAGNOSTIC_MISSING");
                        if(hwpStructure!=null)row.put("hwpStructure",hwpStructure);
                        row.put("reviewPhrasePresence",selectReviewPhrasePresence(storedText));
                        if("COMPLETE_TEXT".equals(file.qualityCode()))assertNotNull(file.roleAssessment(),"TEXT_ROLE_ASSESSMENT_MISSING");
                        if(file.roleAssessment()!=null) {
                            row.put("roleFingerprint",selectRoleFingerprint(file.roleAssessment()));
                            assertEquals(file.extractionId(),file.roleExtractionId());
                            assertEquals(execution.roleRuleVersion(),file.roleAssessment().ruleVersion());
                            assertEquals(sql.queryForObject("SELECT attachment_role_blocks_hash(blocks_json) FROM announcement_source_attachment_extractions WHERE id=?",String.class,file.extractionId()),file.roleAssessment().blocksHash());
                        }
                        var blocks=read.selectAttachmentBlockList(source,file.extractionId(),1,10,0,2000);
                        var blockJson=selectApi(http,"/api/v2/admin/announcement-sources/"+source+"/attachment-extractions/"+file.extractionId()+"/blocks?page=1&size=10&textOffset=0&textLimit=2000");
                        assertTrue(selectWireTree(blocks).equals(blockJson),"API_BLOCK_PROJECTION_MISMATCH");
                        if(segmentMode)saveSegmentVerification(source,file,actual,segments,http,row);
                        var wrong=http.perform(get("/api/v2/admin/announcement-sources/{source}/attachment-extractions/{extraction}/blocks",UUID.randomUUID(),file.extractionId())).andReturn();
                        assertEquals(404,wrong.getResponse().getStatus());
                    } else {
                        assertEquals("BLOCKED",file.downloadStatusCode(),"SUPPORTED_FILE_INCOMPLETE");
                        assertEquals("UNSUPPORTED_FORMAT",file.downloadErrorCode(),"SUPPORTED_FILE_INCOMPLETE");
                    }
                }
                assertEquals(AnnouncementAttachmentOfficialWorkerProbe.selectExpectedExtractionCount(sample.code()),extractor.calls,"EXPECTED_SUPPORTED_FILE_NOT_EXTRACTED");
                var summary=bean(AnnouncementAttachmentCurrentService.class).selectClassificationDetails(source);
                var summaryJson=selectApi(http,"/api/v2/admin/announcement-sources/"+source+"/attachment-classification");
                assertTrue(selectWireTree(summary).equals(summaryJson),"API_CLASSIFICATION_PROJECTION_MISMATCH");
                report.put("processingStatus",summary.processingFlow().statusCode());report.put("decisionStatus",summary.effectiveClassification().semanticStatusCode());
                report.put("decisionReason",summary.effectiveClassification().reasonCode());report.put("extractorCalls",extractor.calls);
                assertNotEquals("EXCLUDED",summary.effectiveClassification().semanticStatusCode());
                assertEquals(0,sql.queryForObject("SELECT count(1) FROM announcement_source_links WHERE source_id=?",Integer.class,source));
                assertEquals(0,sql.queryForObject("SELECT count(1) FROM announcement_source_attachment_confirmations WHERE source_id=?",Integer.class,source));
                if(segmentMode) {
                    assertEquals(execution.engineVersion(),sql.queryForObject("SELECT engine_version FROM announcement_source_attachment_evaluations WHERE source_id=? AND is_current",String.class,source));
                    assertEquals(files.size(),sql.queryForObject("SELECT count(1) FROM announcement_source_attachment_evaluation_inputs i JOIN announcement_source_attachment_evaluations e ON e.id=i.evaluation_id WHERE e.source_id=? AND e.is_current",Integer.class,source));
                    assertEquals(0,sql.queryForObject("SELECT count(1) FROM announcement_source_attachment_matches m JOIN announcement_source_attachment_evaluations e ON e.id=m.evaluation_id JOIN announcement_attachment_segment_analyses a ON a.id=m.segment_analysis_id WHERE e.source_id=? AND e.is_current AND a.analysis_json->'segments'->m.segment_index->>'roleCode' IN ('FORM','REFERENCE','UNKNOWN') AND m.applied_action_code<>'CONTEXT_ONLY'",Integer.class,source));
                    if(rows.stream().anyMatch(r->((Number)r.getOrDefault("unknownSegmentCount",0)).longValue()>0))
                        assertEquals("REVIEW_REQUIRED",summary.effectiveClassification().semanticStatusCode(),"UNKNOWN_SEGMENT_MUST_REMAIN_REVIEW");
                    report.put("segmentDatabaseApiVerified",true);
                    stage="SEGMENT_REVIEW_CONTEXT";
                    var review=bean(AnnouncementAttachmentReviewService.class).selectReviewContextDetails(source);
                    String reviewUrl="/api/v2/admin/announcement-sources/"+source+"/attachment-classification/review-context";
                    assertEquals(selectWireTree(review),selectApi(http,reviewUrl),"REVIEW_CONTEXT_PROJECTION_MISMATCH");
                    var response=http.perform(get(reviewUrl)).andReturn().getResponse();
                    assertEquals(200,response.getStatus());assertEquals("no-store",response.getHeader("Cache-Control"));
                    assertEquals(404,http.perform(get("/api/v2/admin/announcement-sources/{source}/attachment-classification/review-context",UUID.randomUUID())).andReturn().getResponse().getStatus());
                    if(rows.stream().anyMatch(r->((Number)r.getOrDefault("unknownSegmentCount",0)).longValue()>0)) {
                        assertTrue(review.manualSourceCheckRequired(),"UNKNOWN_SEGMENT_ORIGINAL_CHECK_REQUIRED");
                        assertTrue(review.requiredAcknowledgementCodes().contains("ATTACHMENT_ROLE_UNKNOWN"));
                    }
                    assertNull(review.confirmedClassification());assertNull(review.linkedAnnouncement());
                    assertEquals(review,bean(AnnouncementAttachmentReviewService.class).selectReviewContextDetails(source),"REVIEW_GET_MUST_NOT_CHANGE_VERSION");
                    assertEquals(0,sql.queryForObject("SELECT count(1) FROM announcement_source_links WHERE source_id=?",Integer.class,source));
                    assertEquals(0,sql.queryForObject("SELECT count(1) FROM announcement_source_attachment_confirmations WHERE source_id=?",Integer.class,source));
                    report.put("segmentReviewContextVerified",true);
                    report.put("manualSourceCheckRequired",review.manualSourceCheckRequired());
                }
                report.put("requiresFinalAdminVerification",true);
                boolean whole=bodyComplete&&files.stream().allMatch(f->"COMPLETE_TEXT".equals(f.qualityCode()));
                report.put("isWholeTextAnalysisComplete",whole);
                if(!whole)assertFalse(summary.processingFlow().isAutomaticAnalysisComplete(),"INCOMPLETE_MUST_NOT_BE_READY");
                stage="BODY_COMPLETENESS";assertTrue(bodyComplete,"BODY_OBSERVATION_INCOMPLETE");
                if("TAEBAEK-184816".equals(sample.code())) {
                    stage="FIXED_EXPECTATION_RECHECK";
                    var fixedReport=new LinkedHashMap<String,Object>();report.put("fixedExpectationQa",fixedReport);
                    saveFixedExpectationResult(sample,client,extractor,fixedReport);
                }
                report.put("status","WORKER_DB_API_OBSERVED_NOT_APPROVED");
            } catch(Exception|AssertionError failure) {
                report.put("failedStage",stage);report.put("failureCode","OFFICIAL_WORKER_INCOMPLETE");
                // JDBC/HTTP/JSON assertion의 cause에는 실제 원문이 포함될 수 있어 로그·JUnit으로 전달하지 않는다.
                throw new AssertionError(sample.code()+": "+stage+" / OFFICIAL_WORKER_INCOMPLETE");
            } finally {
                try(var paths=Files.walk(temporary)) {
                    cleanup=paths.filter(Files::isRegularFile).allMatch(p->Set.of(".owner",".quota.lock").contains(p.getFileName().toString()));
                }
                report.put("originalFilesRemoved",cleanup);report.put("remainingResourceLeases",sql.queryForObject("SELECT count(1) FROM announcement_attachment_resource_leases",Integer.class));
                report.put("maximumRequestReservations",client.maximumRequests);report.put("maximumReservedBytes",client.maximumBytes);
                report.put("requestReservationsIncludingBodyUpperBound",client.requests);report.put("reservedBytesIncludingBodyUpperBound",client.bytes);
                Path output=Path.of(System.getProperty("saneb.attachment-official-worker.report"));Files.createDirectories(output);
                JSON.writerWithDefaultPrettyPrinter().writeValue(output.resolve(sample.code()+".json").toFile(),report);
                assertTrue(cleanup,"ORIGINAL_FILE_CLEANUP_INCOMPLETE");assertEquals(0,report.get("remainingResourceLeases"));
            }
        }
    }
    static AttachmentExecutionSnapshot selectExecution(String group,ObservationCase sample,String version,String configHash) {
        boolean segment=AnnouncementAttachmentOfficialWorkerProbe.selectSegmentMode(group);
        if(!AnnouncementAttachmentOfficialWorkerProbe.selectCaseCodes(group).contains(sample.code()))throw new IllegalArgumentException("OFFICIAL_WORKER_CASE_INVALID");
        return new AttachmentExecutionSnapshot(sample.profile().selectProfileCode(),sample.profile().selectProfileHash(),
                segment?AttachmentSegmentClassificationEngine.VERSION:"attachment-1.0.0",version,configHash,
                AttachmentDocumentRoleClassifier.VERSION,AttachmentDocumentRoleClassifier.RULES_HASH,
                segment?AttachmentSegmentRoleAnalyzer.VERSION:null,segment?AttachmentSegmentRoleAnalyzer.RULES_HASH:null);
    }
    static AnnouncementAttachmentSegmentService selectSegmentService() {
        var session=bean(SqlSessionTemplate.class);
        return new com.saneb.domain.announcementattachment.service.impl.AnnouncementAttachmentSegmentServiceImpl(
                session.getMapper(AnnouncementAttachmentSegmentDao.class),
                session.getMapper(com.saneb.domain.announcementsource.dao.AnnouncementSourceDao.class),JSON);
    }
    private static void saveSegmentVerification(UUID source,AttachmentEvidenceResponses.FileSummary file,JsonNode actual,
            AnnouncementAttachmentSegmentService service,MockMvc http,Map<String,Object> row) throws Exception {
        var before=sql.queryForObject("SELECT count(1) FROM announcement_attachment_segment_analyses WHERE source_id=?",Integer.class,source);
        var stored=service.selectAnalysisDetails(source,file.extractionId());
        var api=selectApi(http,"/api/v2/admin/announcement-sources/"+source+"/attachment-extractions/"+file.extractionId()+"/segment-analysis");
        assertEquals(selectWireTree(stored),api,"SEGMENT_API_PROJECTION_MISMATCH");
        assertEquals(before,sql.queryForObject("SELECT count(1) FROM announcement_attachment_segment_analyses WHERE source_id=?",Integer.class,source),"SEGMENT_GET_MUST_NOT_WRITE");
        var wrong=http.perform(get("/api/v2/admin/announcement-sources/{source}/attachment-extractions/{extraction}/segment-analysis",UUID.randomUUID(),file.extractionId())).andReturn();
        assertEquals(404,wrong.getResponse().getStatus());
        if(actual.path("text").asText("").isBlank()) {
            assertEquals("NOT_ANALYZED",stored.analysisState());assertNull(stored.analysis());return;
        }
        var blocks=JSON.treeToValue(actual.path("blocks"),AttachmentSetEvidence.Block[].class);
        var input=new AttachmentSetEvidence.Extraction(file.qualityCode(),actual.path("text").asText(),Arrays.asList(blocks),actual.path("pageCount").isIntegralNumber()?actual.path("pageCount").intValue():null,0);
        var expected=new AttachmentSegmentRoleAnalyzer().selectAnalysis(input);
        assertEquals("ANALYZED",stored.analysisState());assertEquals(expected,stored.analysis());
        assertEquals(file.fileId(),stored.fileId());assertEquals(file.setId(),stored.setId());
        assertEquals(file.documentRoleCode(),stored.fileRoleCode());assertEquals(file.roleOriginCode(),stored.fileRoleOriginCode());
        assertEquals(1,sql.queryForObject("SELECT count(1) FROM announcement_source_attachment_evaluation_inputs i JOIN announcement_source_attachment_evaluations e ON e.id=i.evaluation_id WHERE e.source_id=? AND e.is_current AND i.extraction_id=? AND i.segment_analysis_id=?",Integer.class,source,file.extractionId(),stored.analysisId()));
        row.put("segmentAnalysisHash",selectCanonicalHash(expected));row.put("segmentCount",expected.segments().size());
        row.put("unknownSegmentCount",expected.segments().stream().filter(s->"UNKNOWN".equals(s.roleCode())).count());
        row.put("segmentEvaluationInputBound",true);row.put("segmentApiProjectionMatched",true);
        // 동일 설치 추출 결과를 메모리에서만 대조한다. 후보 규칙을 정책/평가/DB에 적용하지 않는다.
        row.put("candidateSegmentComparison",selectCandidateSegmentComparison(input,expected));
    }
    static Map<String,Object> selectCandidateSegmentComparison(AttachmentSetEvidence.Extraction input,
            AttachmentSegmentRoleAnalyzer.Analysis legacy) throws Exception {
        var analyzer=new AttachmentSegmentRoleAnalyzer();
        var candidate=analyzer.selectAnalysis(input,AttachmentSegmentRoleAnalyzer.PARENTHESIZED_VERSION,
                AttachmentSegmentRoleAnalyzer.PARENTHESIZED_RULES_HASH);
        assertEquals(legacy,analyzer.selectAnalysis(input),"LEGACY_ANALYSIS_CHANGED");
        assertEquals(legacy.textHash(),candidate.textHash());assertEquals(legacy.blocksHash(),candidate.blocksHash());
        assertTrue(analyzer.selectAnalysisValid(input,candidate));
        assertEquals(legacy.segments().size(),candidate.segments().size());
        for(int i=0;i<legacy.segments().size();i++) {
            assertEquals(legacy.segments().get(i).startOffset(),candidate.segments().get(i).startOffset());
            assertEquals(legacy.segments().get(i).endOffset(),candidate.segments().get(i).endOffset());
        }
        return Map.of("analysisVersion",candidate.analysisVersion(),"rulesHash",candidate.rulesHash(),
                "analysisHash",selectCanonicalHash(candidate),"statusCode",candidate.statusCode(),
                "sameInputAndBoundariesVerified",true,"persistedOrApplied",false,
                "segments",candidate.segments().stream().map(s->Map.of("index",s.index(),"roleCode",s.roleCode(),
                        "reasonCode",s.reasonCode(),"evidenceCodes",s.evidence().stream().map(AttachmentSegmentRoleAnalyzer.Evidence::ruleCode).toList())).toList());
    }
    static boolean selectPlannedTitleStop(ObservationCase sample,AnnouncementSourceClassificationResult title) {
        if(AnnouncementAttachmentOfficialWorkerProbe.selectTitleStopExpected(sample.code())) {
            assertEquals(TitleStageCode.COMBINATION_NOT_MATCHED,title.titleStageCode(),"FIXED_TITLE_STOP_CHANGED");
            assertEquals(ReasonCode.TITLE_COMBINATION_NOT_MATCHED,title.reasonCode(),"FIXED_TITLE_STOP_REASON_CHANGED");
            assertEquals(SemanticStatusCode.EXCLUDED,title.semanticStatusCode(),"FIXED_TITLE_STOP_BECAME_ELIGIBLE");
            return true;
        }
        assertTrue(AnnouncementAttachmentBbsOfficialObservationTest.selectTitleMayProceed(title),"FIXED_TITLE_CANDIDATE_STOPPED");
        return false;
    }
    static AttachmentJobReservation insertSourceRequest(ObservationCase sample,String text,AnnouncementSourceClassificationResult base,AttachmentExecutionSnapshot execution) throws Exception {
        UUID policy=insertPolicy(execution),source=UUID.randomUUID();
        UUID localSource=sql.queryForObject("SELECT id FROM local_government_notice_sources WHERE public_code=?",UUID.class,sample.source().localSourceCode());
        sql.update("UPDATE local_government_notice_sources SET is_enabled=true WHERE id=?",localSource);
        sql.update("INSERT INTO announcement_source_snapshots(id,provider_code,provider_notice_id,title,raw_hash,source_url,body_text,local_government_source_id,semantic_status_code,is_attachment_review_required,attachment_policy_id) VALUES (?,'LOCAL_GOV_NOTICE',?,?,repeat('a',64),?,?,?,'REVIEW_REQUIRED',true,?)",
                source,sample.source().providerNoticeId(),sample.title(),sample.source().sourceUrl(),text,localSource,policy);
        var item=new AnnouncementSourceProviderItem("LOCAL_GOV_NOTICE",sample.source().providerNoticeId(),sample.title(),null,null,null,null,null,
                sample.source().sourceUrl(),text,null,null,"PARTIAL","[]","{}","a".repeat(64),List.of(),localSource);
        UUID baseId=bean(AnnouncementSourceClassificationPersistenceService.class).saveNewContentEvaluation(source,null,release,item,base,"REVIEW_PENDING");
        var current=bean(AnnouncementAttachmentJobDao.class).selectSourceContextDetails(source);
        assertEquals(baseId,current.baseEvaluationId());
        return new AttachmentJobReservation(source,policy,baseId,current.sourceVersion(),current.attachmentVersion(),UUID.randomUUID(),execution);
    }
    static AnnouncementSourceClassificationRuleSet selectRules(){return bean(AnnouncementSourceRuleReleaseService.class).selectPublishedRuleSet(release);}
    static <T>T selectService(Class<T> type){return bean(type);}
    private static void saveFixedExpectationResult(ObservationCase sample,OfficialClient client,ActualExtractor extractor,Map<String,Object> report) throws Exception {
        try(var profilesContext=new AnnotationConfigApplicationContext(StandardBbsAttachmentProfileConfiguration.class,
                SaeolGetAttachmentProfileConfiguration.class,LegalBoardAttachmentProfileConfiguration.class,
                BizInfoAttachmentDiscoveryProfile.class,SeoguSaeolAttachmentDiscoveryProfile.class,HwacheonPostAttachmentDiscoveryProfile.class)) {
            var profiles=List.copyOf(profilesContext.getBeansOfType(AttachmentDiscoveryProfile.class).values());
            var registry=new AttachmentDiscoveryProfileRegistry(profiles);
            var targets=sql.query("""
                    -- 운영 대상이 아닌 이 시험의 전체 미삭제 seed다. 비활성/미구현 기관을 분모에서 빼지 않는다.
                    SELECT id,public_code,parser_profile_code,notice_url
                    FROM local_government_notice_sources WHERE deleted_at IS NULL ORDER BY public_code
                    """,(rs,index)->new AttachmentPolicyValidationRows.Target(rs.getObject("id",UUID.class),rs.getString("public_code"),
                            rs.getString("parser_profile_code"),rs.getString("notice_url"),"{}"));
            var scope=com.saneb.domain.announcementattachment.service.impl.AttachmentProviderQaPlan.selectPlan(profiles,targets);
            var prepared=new com.saneb.domain.announcementattachment.qa.AttachmentProviderQaCatalog(JSON,registry)
                    .selectPrepared(scope,selectRules(),runtime.selectIdentity().configHash(),Instant.now());
            report.put("scopeSource","EPHEMERAL_ALL_UNDELETED_SEEDS_AND_NATIONAL_PROVIDERS");report.put("targetCount",targets.size()+2);
            report.put("catalogHash",prepared.plan().catalogHash());report.put("executableCount",prepared.plan().executableCount());
            report.put("isExpectationCoverageComplete",prepared.plan().isExpectationCoverageComplete());
            assertEquals(targets.size()+2,prepared.plan().targets().size());
            assertFalse(prepared.plan().isExpectationCoverageComplete());assertFalse(prepared.plan().isQaPassed());
            var plan=prepared.plan().cases().stream().filter(c->sample.code().equals(c.caseCode())).findFirst().orElseThrow();
            report.put("caseState",plan.statusCode());report.put("normalNotice",plan.normalNotice());
            assertEquals("EXPECTED_INPUT_READY",plan.statusCode(),"FIXED_EXPECTATION_NOT_READY");assertFalse(plan.normalNotice());
            var input=prepared.inputs().stream().filter(c->sample.code().equals(c.caseId())).findFirst().orElseThrow();
            assertEquals(2,input.files().size());
            assertEquals(List.of("UNKNOWN","FORM"),input.files().stream().map(f->f.roleExpectation().roleCode()).toList());
            var control=new FixedCaseControl(System::nanoTime);
            int before=extractor.calls;
            var executor=new com.saneb.domain.announcementattachment.qa.AttachmentProviderQaCaseExecutor(registry,client,
                    new AttachmentTemporaryStorage(System.getProperty("java.io.tmpdir")+"/fixed-expectation"),runtime,extractor,new AttachmentFileTypeValidator(),JSON);
            var result=executor.selectResult(input,control);
            report.put("result",result);
            assertEquals("PASSED",result.status(),"FIXED_EXPECTATION_RECHECK_FAILED");
            assertEquals("FIXED_NOTICE_EXPECTATIONS_MATCHED",result.reasonCode());
            assertEquals(2,result.discoveredFileCount());assertTrue(result.discoveryComplete());assertTrue(result.allTextComplete());
            assertTrue(result.originalFilesRemoved());assertFalse(result.isPolicyQaPassed());assertFalse(control.inUse.get());
            assertEquals(2,extractor.calls-before);assertEquals(control.requests,result.requestReservations());assertEquals(control.bytes,result.reservedBytes());
        }
    }
    /** 단일 시험 자원. 외부 요청은 OfficialClient의 본문+worker+재검증 통합44회/80MiB 상한도 적용한다. */
    static final class FixedCaseControl implements com.saneb.domain.announcementattachment.qa.AttachmentProviderQaCaseExecutor.ExecutionControl {
        final java.util.function.LongSupplier clock;final long started;long requests,bytes;
        final java.util.concurrent.atomic.AtomicBoolean inUse=new java.util.concurrent.atomic.AtomicBoolean();
        FixedCaseControl(java.util.function.LongSupplier clock){this.clock=clock;started=clock.getAsLong();}
        public boolean selectExecutionAllowed(){return !Thread.currentThread().isInterrupted()&&clock.getAsLong()-started<420_000_000_000L;}
        public AutoCloseable selectDownloadPermit(String host){return host!=null&&host.matches("[0-9a-f]{64}")?selectExtractionPermit():null;}
        public AutoCloseable selectExtractionPermit(){
            if(!selectExecutionAllowed()||!inUse.compareAndSet(false,true))return null;
            var closed=new java.util.concurrent.atomic.AtomicBoolean();return ()->{if(closed.compareAndSet(false,true))inUse.set(false);};
        }
        public boolean saveRequestReservation(){if(!selectExecutionAllowed()||requests>=44)return false;requests++;return true;}
        public boolean saveByteReservation(long size){if(!selectExecutionAllowed()||size<=0||size>80*MIB-bytes)return false;bytes+=size;return true;}
    }
    private static UUID insertPolicy(AttachmentExecutionSnapshot execution) throws Exception {
        UUID id=UUID.randomUUID();
        String settings=JSON.writeValueAsString(selectPolicyConfiguration(execution));
        String manifest=JSON.writeValueAsString(List.of(Map.of("providerCode","LOCAL_GOV_NOTICE","profileCode",execution.profileCode(),"profileHash",execution.profileHash())));
        sql.update("INSERT INTO announcement_attachment_policies(id,policy_code,version_no,policy_status_code,mode_code,rule_release_id,policy_hash,settings_json,profile_manifest_json,created_by,published_at) VALUES (?,?,1,'ACTIVE','ENFORCE',?,repeat('d',64),CAST(? AS jsonb),CAST(? AS jsonb),?,now())",id,id.toString(),release,settings,manifest,actor);
        return id;
    }
    static AttachmentPolicyResponses.Configuration selectPolicyConfiguration(AttachmentExecutionSnapshot execution) {
        return new AttachmentPolicyResponses.Configuration(execution.engineVersion(),execution.extractorVersion(),execution.extractorConfigHash(),
                (execution.segmentRuleVersion()==null?80L:32L)*MIB,execution.roleRuleVersion(),execution.roleRulesHash(),execution.segmentRuleVersion(),execution.segmentRulesHash());
    }
    private static JsonNode selectApi(MockMvc http,String path) throws Exception {
        var response=http.perform(get(path)).andReturn().getResponse();assertEquals(200,response.getStatus());assertEquals("no-store",response.getHeader("Cache-Control"));
        var json=JSON.readTree(response.getContentAsByteArray());assertTrue(json.path("success").asBoolean());return json.path("data");
    }
    // DTO의 LongNode와 HTTP JSON을 읽은 IntNode는 값이 같아도 equals가 false다.
    // 기대값도 실제 wire serialization을 거쳐 비교하며 필드·값·배열 순서 검증은 유지한다.
    static JsonNode selectWireTree(Object value) throws Exception {return JSON.readTree(JSON.writeValueAsBytes(value));}
    /** 사전 검토용 지문만 반환한다. catalog 생성·승인·갱신은 수행하지 않는다. */
    static Map<String,Object> selectRoleFingerprint(AttachmentDocumentRoleClassifier.Assessment assessment) throws Exception {
        selectRoleDiagnostic(assessment);
        if(assessment==null || !AttachmentDocumentRoleClassifier.VERSION.equals(assessment.ruleVersion())
                || !AttachmentDocumentRoleClassifier.RULES_HASH.equals(assessment.rulesHash())
                || assessment.textHash()==null || !assessment.textHash().matches("[0-9a-f]{64}")
                || assessment.blocksHash()==null || !assessment.blocksHash().matches("[0-9a-f]{64}")
                || !Set.of("NOTICE","GUIDE","FORM","REFERENCE","UNKNOWN").contains(assessment.roleCode()))
            throw new IllegalArgumentException("ROLE_FINGERPRINT_INVALID");
        return Map.of("ruleVersion",assessment.ruleVersion(),"rulesHash",assessment.rulesHash(),
                "roleCode",assessment.roleCode(),"reasonCode",assessment.reasonCode(),
                "textHash",assessment.textHash(),"blocksHash",assessment.blocksHash(),"assessmentHash",selectCanonicalHash(assessment));
    }
    static String selectCanonicalHash(Object value) throws Exception {
        var canonical=JSON.copy().enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
        return hash(canonical.writeValueAsString(canonical.convertValue(value,Object.class)));
    }
    /** 공개 표본의 고정 업무 문구 존재 여부만 확인하며 원문/담당자/연락처를 반환하지 않는다. */
    static Map<String,Boolean> selectReviewPhrasePresence(String text) {
        var result=new LinkedHashMap<String,Boolean>();
        for(String phrase:List.of("청년농업인","취업농","신청서","서명"))result.put(phrase,text!=null&&text.contains(phrase));
        return result;
    }
    /** 원문 없이 문자 해석 손실 징후만 센다. 미추출(null)을 손실 0건으로 바꾸지 않는다. */
    static Long selectReplacementCharacterCount(String text) {
        return text==null?null:text.codePoints().filter(value->value==0xfffd).count();
    }
    /** 원문·파일명·URL·위치 원문 없이 실제 저장된 역할 판정의 고정 코드만 진단한다. */
    static Map<String,Object> selectRoleDiagnostic(AttachmentDocumentRoleClassifier.Assessment assessment) {
        if(assessment==null)return Map.of("assessmentPresent",false);
        Set<String> reasons=Set.of("COMPLETE_TEXT_REQUIRED","STRUCTURE_UNCERTAIN","ROLE_ANALYSIS_LIMIT",
                "MIXED_DOCUMENT_ROLES","INITIAL_HEADING_REQUIRED","ROLE_STRUCTURE_INCOMPLETE","ROLE_TEXT_STRUCTURE_MATCHED");
        Set<String> rules=Set.of("NOTICE_HEADING","GUIDE_HEADING","FORM_HEADING","REFERENCE_HEADING",
                "TARGET_SECTION","SUPPORT_SECTION","APPLICATION_SECTION","APPLICANT_FIELD","SIGNATURE_FIELD","QUESTION_ITEM","ANSWER_ITEM");
        if(!reasons.contains(assessment.reasonCode()) || assessment.evidence().stream().anyMatch(e->!rules.contains(e.ruleCode())))
            throw new IllegalArgumentException("ROLE_DIAGNOSTIC_CODE_INVALID");
        return Map.of("assessmentPresent",true,"reasonCode",assessment.reasonCode(),
                "matchedRuleCodes",assessment.evidence().stream().map(AttachmentDocumentRoleClassifier.Evidence::ruleCode).distinct().sorted().toList(),
                "evidenceCount",assessment.evidence().size(),
                "evidenceBlockCount",assessment.evidence().stream().map(AttachmentDocumentRoleClassifier.Evidence::blockIndex).distinct().count());
    }
    private static String hash(String text) throws Exception {return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));}
    private static final class ActualExtractor extends IsolatedAttachmentExtractor {
        int calls;final Map<String,JsonNode> byBinaryHash=new HashMap<>();
        ActualExtractor(){super(JSON,distribution);}
        @Override public JsonNode selectExtraction(Path input) throws IOException {
            assertFalse(TransactionSynchronizationManager.isActualTransactionActive());calls++;
            var result=super.selectExtraction(input);
            try {byBinaryHash.put(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(input))),result);}
            catch(java.security.NoSuchAlgorithmException failure){throw new IOException("HASH_UNAVAILABLE");}
            return result;
        }
    }
    private static final class OfficialClient extends AttachmentPinnedDownloadClient {
        final ObservationCase sample;final long maximumRequests,maximumBytes;long requests,bytes;
        OfficialClient(ObservationCase sample){
            this.sample=sample;
            String group=System.getProperty("saneb.attachment-official-worker.group","YANGPYEONG");
            maximumRequests=AnnouncementAttachmentOfficialWorkerProbe.selectMaximumRequests(group);
            maximumBytes=AnnouncementAttachmentOfficialWorkerProbe.selectMaximumBytes(group);
        }
        void reserveBody(){requests=2;bytes=2*MIB;}
        @Override public Download selectDownload(Request request,Set<String> hosts,Predicate<Request> approved,Path output,long maximum,ByteReservation reservation) throws IOException {
            assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
            var downloaded=super.selectDownload(request,hosts,r->{
                if(requests>=maximumRequests||Thread.currentThread().isInterrupted()||!sample.profile().selectApprovedRequest(request,r)||!approved.test(r))return false;
                requests++;return true;
            },output,maximum,count->{
                if(count<0||bytes>maximumBytes-count||Thread.currentThread().isInterrupted()||!reservation.reserve(count))return false;
                bytes+=count;return true;
            });
            if(request.uri().equals(sample.profile().selectDetailUri(sample.source()))) {
                try(var input=Files.newInputStream(output)) {
                    AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(Jsoup.parse(input,null,request.uri().toASCIIString()),sample.title(),sample.titleLayout());
                }
            }
            return downloaded;
        }
    }
}
