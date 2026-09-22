package com.saneb.db;

import static org.assertj.core.api.Assertions.assertThat;

import com.saneb.config.typehandler.UuidTypeHandler;
import com.saneb.domain.announcementattachment.vo.AttachmentEvidenceCommands;
import com.saneb.domain.announcementattachment.vo.AttachmentRoleRows;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/** XML/namespace/parameter type 검증이다. 실제 PostgreSQL SQL/trigger 실행 성공을 뜻하지 않는다. */
class AnnouncementAttachmentMapperBindingTest {
    @Test void segmentAnalysisMapperBindsSourceAndVersionWithoutUpdatingExistingEvidence() {
        String prefix="com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentSegmentDao.";
        for(var method:com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentSegmentDao.class.getDeclaredMethods())
            assertThat(configuration.hasStatement(prefix+method.getName())).as(method.getName()).isTrue();
        var args=Map.of("sourceId",UUID.randomUUID(),"extractionId",UUID.randomUUID(),"analysisVersion","segment-role-1.0.0","rulesHash","a".repeat(64));
        var input=configuration.getMappedStatement(prefix+"selectExtractionDetails").getBoundSql(args);
        assertThat(input.getSql()).contains("aset.set_status_code='SEALED'","s.data_purpose_code='PRODUCTION'","b.is_current","x.source_id=? AND x.id=?","newer.attempt_no>x.attempt_no");
        assertThat(input.getParameterMappings()).extracting(p->p.getProperty()).containsExactly("sourceId","extractionId");
        var read=configuration.getMappedStatement(prefix+"selectAnalysisDetails").getBoundSql(args);
        assertThat(read.getSql()).doesNotContain("INSERT", "UPDATE", "extracted_text", "SELECT *");
        assertThat(read.getParameterMappings()).extracting(p->p.getProperty()).containsExactly("sourceId","extractionId","analysisVersion","rulesHash");
        var command=new com.saneb.domain.announcementattachment.vo.AttachmentSegmentRows.Insert(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),"segment-role-1.0.0","a".repeat(64),"b".repeat(64),"c".repeat(64),"{}");
        var insert=configuration.getMappedStatement(prefix+"insertAnalysis").getBoundSql(command);
        assertThat(insert.getSql()).contains("ON CONFLICT (extraction_id,analysis_version,rules_hash) DO NOTHING").doesNotContain("UPDATE ", "announcement_source_snapshots", "announcement_source_attachment_files");
        assertThat(insert.getParameterMappings()).hasSize(10).allSatisfy(p->assertThat(p.getTypeHandler()).isNotNull());
    }
    @Test void conversionLinkUsesFlywayPublicCodeColumnWithoutChangingResponseAlias() {
        var bound=configuration.getMappedStatement("com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentReviewDao.selectConversionLinkDetails")
                .getBoundSql(Map.of("sourceId",UUID.randomUUID()));
        assertThat(bound.getSql()).contains("a.public_code AS announcement_code").doesNotContain("a.announcement_code");
        assertThat(bound.getParameterMappings()).extracting(p->p.getProperty()).containsExactly("sourceId");
    }
    @Test void providerQaEvidenceSelectsLatestAttemptNotLatestSuccessAndPagesFullCases() {
        String prefix="com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentProviderQaEvidenceDao.";
        for(var method:com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentProviderQaEvidenceDao.class.getDeclaredMethods())
            assertThat(configuration.hasStatement(prefix+method.getName())).as(method.getName()).isTrue();
        var scope=new com.saneb.domain.announcementattachment.vo.AttachmentProviderQaEvidenceRows.Scope(UUID.randomUUID(),"a".repeat(64),"b".repeat(64),"c".repeat(64));
        var latest=configuration.getMappedStatement(prefix+"selectLatestRunList").getBoundSql(scope);
        assertThat(latest.getSql()).contains("DISTINCT ON (b.segment_no)","ORDER BY b.segment_no,r.created_at DESC,r.id DESC")
                .doesNotContain("run_status_code='COMPLETED'","run_status_code IN","LIMIT","SELECT *");
        assertThat(latest.getParameterMappings()).extracting(p->p.getProperty()).containsExactly("policyId","snapshotHash","catalogHash","planHash");
        var page=configuration.getMappedStatement(prefix+"selectEvidenceList").getBoundSql(new com.saneb.domain.announcementattachment.vo.AttachmentProviderQaEvidenceRows.Page(UUID.randomUUID(),100,100));
        assertThat(page.getSql()).contains("evidence_json::text","ORDER BY ordinal LIMIT ? OFFSET ?").doesNotContain("lease_token","source_url","body_text","extracted_text");
        assertThat(configuration.getMappedStatement(prefix+"selectEvidenceCount").getBoundSql(Map.of("runId",UUID.randomUUID())).getSql()).doesNotContain("PASSED","LIMIT");
    }
    @Test void providerQaManagementBindsAllMethodsPagesSafeMetadataAndKeepsOriginalHistory() {
        String prefix="com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentProviderQaManagementDao.";
        for(var method:com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentProviderQaManagementDao.class.getDeclaredMethods())
            assertThat(configuration.hasStatement(prefix+method.getName())).as(method.getName()).isTrue();
        var args=Map.of("runId",UUID.randomUUID(),"policyId",UUID.randomUUID(),"size",20,"offset",20);
        var items=configuration.getMappedStatement(prefix+"selectCaseList").getBoundSql(args);
        assertThat(items.getSql()).contains("ORDER BY ordinal LIMIT ? OFFSET ?").doesNotContain("evidence_json", "lease_token", "SELECT *", "source_url", "body_text");
        assertThat(items.getParameterMappings()).extracting(p->p.getProperty()).containsExactly("runId","size","offset");
        var runs=configuration.getMappedStatement(prefix+"selectRunList").getBoundSql(args);
        assertThat(runs.getSql()).contains("LEFT JOIN announcement_attachment_provider_qa_run_plans","p.row_version=r.policy_row_version","ORDER BY r.created_at DESC,r.id LIMIT ? OFFSET ?");
        assertThat(configuration.getMappedStatement(prefix+"selectCaseCount").getBoundSql(args).getSql()).doesNotContain("PASSED","LIMIT");
        assertThat(configuration.getMappedStatement(prefix+"updatePendingInputChanged").getBoundSql(args).getSql()).contains("c.case_status_code='PENDING'").doesNotContain("lease_token=");
        var plan=new com.saneb.domain.announcementattachment.vo.AttachmentProviderQaManagementRows.PlanInsert(UUID.randomUUID(),"a".repeat(64),1,2,10,2,false,480);
        assertThat(configuration.getMappedStatement(prefix+"insertPlan").getBoundSql(plan).getParameterMappings()).hasSize(8).allSatisfy(p->assertThat(p.getTypeHandler()).isNotNull());
    }
    @Test void providerQaMapperBindsAllMethodsAndSharesSlotsWithAtomicNonRefundableUsage() {
        String prefix="com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentProviderQaDao.";
        for(var method:com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentProviderQaDao.class.getDeclaredMethods())
            assertThat(configuration.hasStatement(prefix+method.getName())).as(method.getName()).isTrue();
        var args=Map.of("caseId",UUID.randomUUID(),"leaseToken",UUID.randomUUID(),"requests",1,"bytes",0L,"runId",UUID.randomUUID());
        var budget=configuration.getMappedStatement(prefix+"updateUsage").getBoundSql(args);
        assertThat(budget.getSql()).contains("WITH candidate AS", "c.request_reservations+?<=c.maximum_requests", "r.request_reservations+?<=r.maximum_requests",
                "c.reserved_bytes+?<=c.maximum_bytes", "r.reserved_bytes+?<=r.maximum_bytes", "resource_code='DOWNLOAD'", "resource_code='HOST'", "RETURNING c.run_id");
        assertThat(budget.getParameterMappings()).allSatisfy(p->assertThat(p.getTypeHandler()).isNotNull());
        for(String mapper:java.util.List.of("AnnouncementAttachmentJobDao.insertResourceLease","AnnouncementAttachmentPolicyValidationDao.insertExtractionLease",
                "AnnouncementAttachmentProviderQaDao.insertResource")) {
            String query=configuration.getMappedStatement("com.saneb.domain.announcementattachment.dao."+mapper).getBoundSql(args).getSql();
            assertThat(query).contains("provider_qa_case_id", "provider_qa_lease_token", "announcement_attachment_resource_leases.lease_expires_at");
        }
        assertThat(configuration.getMappedStatement(prefix+"updateFinished").getBoundSql(args).getSql())
                .contains("WHEN r.run_status_code='CANCEL_REQUESTED' THEN 'CANCELLED'","c.lease_token=?","c.lease_expires_at>clock_timestamp()");
    }
    @Test void publicationMapperChangesOnlyOnePreviousAndCurrentPolicyUnderReceiptBinding(){
        String prefix="com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentPolicyPublicationDao.";
        var params=Map.of("publicationId",UUID.randomUUID());
        for(String method:java.util.List.of("updatePreviousPolicyRetired","updatePolicyActive")){
            var sql=configuration.getMappedStatement(prefix+method).getBoundSql(params).getSql();
            assertThat(sql).contains("x.created_xid=pg_current_xact_id()","p.row_version=").doesNotContain("UPDATE announcement_source","UPDATE announcement_attachment_jobs","SELECT *");
        }
        assertThat(configuration.getMappedStatement(prefix+"selectPublicationLock").getBoundSql(null).getSql()).contains("attachment_policy_publication_lock()");
        for(var method:com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentPolicyPublicationDao.class.getDeclaredMethods())assertThat(configuration.hasStatement(prefix+method.getName())).isTrue();
    }
    @Test void publicationScopeUsesUnboundedDbMembershipAndTwoWayCurrentComparison() {
        String prefix="com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentPolicyPublicationScopeDao.";
        var params=Map.of("scopeId",UUID.randomUUID(),"policyId",UUID.randomUUID());
        var members=configuration.getMappedStatement(prefix+"insertScopeMembers").getBoundSql(params);
        assertThat(members.getSql()).contains("attachment_policy_publication_members(?)").doesNotContain("LIMIT ","maximumCount","SELECT *");
        assertThat(members.getParameterMappings()).extracting(p->p.getProperty()).containsExactly("scopeId","policyId");
        var current=configuration.getMappedStatement(prefix+"selectScopeCurrent").getBoundSql(params);
        assertThat(current.getSql()).contains("EXCEPT (SELECT i.entity_type_code","EXCEPT (SELECT m.entity_type_code","p.row_version=s.policy_row_version","s.expires_at>clock_timestamp()");
        var search=new com.saneb.domain.announcementattachment.vo.AttachmentPolicyPublicationScopeRows.Search(UUID.randomUUID(),UUID.randomUUID(),100,100);
        var page=configuration.getMappedStatement(prefix+"selectItemList").getBoundSql(search);
        assertThat(page.getSql()).contains("LIMIT ? OFFSET ?").doesNotContain("body_text","source_url","extracted_text","requested_by","request_hash");
        for(var method:com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentPolicyPublicationScopeDao.class.getDeclaredMethods())
            assertThat(configuration.hasStatement(prefix+method.getName())).as(method.getName()).isTrue();
    }
    @Test void policyPublicationImpactSeparatesRuleAndGlobalCountsWithoutWritingOrJoiningRowsTwice() {
        String statement="com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentPolicyPublicationImpactDao.selectCounts";
        var scoped=configuration.getMappedStatement(statement).getBoundSql(Map.of("ruleReleaseId",UUID.randomUUID()));
        assertThat(scoped.getSql()).contains("s.data_purpose_code='PRODUCTION'","q.data_purpose_code='PRODUCTION'","p.rule_release_id=?","j.rule_release_id=?",
                "EXISTS (SELECT 1 FROM announcement_source_links","CROSS JOIN jobs CROSS JOIN plans","j.rollback_action_id IS NOT NULL")
                .doesNotContain("SELECT *","LIMIT ","FOR UPDATE","FOR SHARE","UPDATE announcement","source_url","body_text","extracted_text");
        assertThat(scoped.getParameterMappings()).extracting(p->p.getProperty()).containsExactly("ruleReleaseId","ruleReleaseId","ruleReleaseId");
        var all=configuration.getMappedStatement(statement).getBoundSql(java.util.Collections.singletonMap("ruleReleaseId",null));
        assertThat(all.getParameterMappings()).isEmpty();assertThat(all.getSql()).doesNotContain("rule_release_id=?");
    }
    @Test void batchApprovalHistoryKeepsStableVersionBoundAndNeverJoinsDeletedSources() {
        String prefix="com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentBatchHistoryDao.";
        var search=new com.saneb.domain.announcementattachment.vo.AttachmentBatchHistorySearch(UUID.randomUUID(),25,10,20L);
        var count=configuration.getMappedStatement(prefix+"selectActionCount").getBoundSql(search);
        var page=configuration.getMappedStatement(prefix+"selectActionList").getBoundSql(search);
        for(var query:java.util.List.of(count,page)) {
            assertThat(query.getSql()).contains("announcement_attachment_batch_application_actions", "announcement_attachment_batch_rollback_actions", "UNION ALL", "a.expected_version<?")
                    .doesNotContain("announcement_attachment_jobs", "announcement_source_snapshots", "actor_id", "reason_hash", "idempotency_key", "FOR UPDATE", "SELECT *");
            assertThat(query.getParameterMappings()).allSatisfy(p->assertThat(p.getTypeHandler()).isNotNull());
        }
        assertThat(count.getSql()).doesNotContain("LIMIT ");
        assertThat(page.getSql()).contains("ORDER BY accepted_from_version DESC,action_kind DESC,action_id DESC LIMIT ? OFFSET ?");
        assertThat(page.getParameterMappings()).extracting(p->p.getProperty()).containsExactly("batchId","throughVersion","batchId","throughVersion","size","offset");
    }
    @Test void fixedReservationReadsAndSummaryNeverRerunOriginalScopeOrHideUnreservedItems() {
        var parameters=Map.of("runId",UUID.randomUUID(),"segmentNo",2L,"lock",false);
        String prefix="com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentBackfillSegmentDao.";
        var fixed=configuration.getMappedStatement(prefix+"selectFixedItemList").getBoundSql(parameters);
        assertThat(fixed.getSql()).contains("WHERE run_id=? AND segment_no=?", "current_input_matches").doesNotContain("LIMIT ","collected_at", "FOR UPDATE", "source_url");
        var read=configuration.getMappedStatement(prefix+"selectRunDetails").getBoundSql(parameters);
        assertThat(read.getSql()).doesNotContain("FOR UPDATE");
        var write=configuration.getMappedStatement(prefix+"selectRunDetails").getBoundSql(Map.of("runId",UUID.randomUUID(),"lock",true));
        assertThat(write.getSql()).contains("FOR UPDATE");
        var summary=configuration.getMappedStatement(prefix+"selectOutcomeCounts").getBoundSql(parameters);
        assertThat(summary.getSql()).contains("UNRESERVED", "MISSING_JOB", "('COLLECTION',c.job_status_code)", "('APPLICATION',c.application_status_code)", "('ROLLBACK',c.rollback_status_code)")
                .doesNotContain("LIMIT ", "SELECT *", "source_url", "extracted_text");
        var candidates=configuration.getMappedStatement("com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentBatchDao.selectFixedSourceCandidateList")
                .getBoundSql(Map.of("sourceIds",java.util.List.of(UUID.randomUUID(),UUID.randomUUID())));
        assertThat(candidates.getSql()).contains("s.id IN", "NOT EXISTS (SELECT 1 FROM announcement_source_links").doesNotContain("LIMIT ", "collected_at");
    }
    @Test void fullInventoryNeverUsesPageLimitForFreezeAndReadsOnlyFixedSegmentMembership() {
        String prefix="com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentBackfillDao.";
        var scope=new com.saneb.domain.announcementattachment.dto.AttachmentBackfillRequests.Scope(UUID.randomUUID(),java.util.List.of("BIZINFO"),
                OffsetDateTime.parse("2026-08-01T00:00:00Z"),OffsetDateTime.parse("2026-09-01T00:00:00Z"),java.time.LocalDate.of(2026,8,1),java.time.LocalDate.of(2026,9,1),1000);
        for(String statement:java.util.List.of("selectScopeDigest","insertSegments","insertItems")) {
            var parameters=Map.of("scope",scope,"runId",UUID.randomUUID());
            var bound=configuration.getMappedStatement(prefix+statement).getBoundSql(parameters);
            assertThat(bound.getSql()).contains("row_number() OVER (ORDER BY s.collected_at,s.id)","s.data_purpose_code='PRODUCTION'","s.application_end_date>=?","s.application_end_date<=?",
                    "NOT EXISTS (SELECT 1 FROM announcement_source_links", "attachment_backfill_input_hash(s.id)")
                    .doesNotContain("LIMIT ","SELECT *","source_url","body_text","extracted_text","SKIP LOCKED");
            assertThat(bound.getParameterMappings()).allSatisfy(parameter->assertThat(parameter.getTypeHandler()).isNotNull());
            if(statement.equals("insertSegments")) {
                assertThat(bound.getSql()).contains("AS segment_no", "GROUP BY segment_no");
                assertThat(bound.getParameterMappings()).filteredOn(parameter->parameter.getProperty().equals("scope.segmentSize")).hasSize(1);
            }
        }
        var page=configuration.getMappedStatement(prefix+"selectItemList").getBoundSql(new com.saneb.domain.announcementattachment.vo.AttachmentBackfillRows.Search(UUID.randomUUID(),2L,100,0));
        assertThat(page.getSql()).contains("WHERE run_id=? AND segment_no=?","ORDER BY ordinal LIMIT ? OFFSET ?","coalesce(input_hash=attachment_backfill_input_hash(source_id),false)")
                .doesNotContain("collected_at", "INSERT ", "UPDATE ", "source_url", "body_text");
        assertThat(page.getParameterMappings()).extracting(p->p.getProperty()).containsExactly("runId","segmentNo","size","offset");
    }
    @Test void normalRollbackUsesSameSourceJobPreviewAndLeavesApplicationHistoryUnchanged() {
        String prefix="com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentNormalRollbackDao.";
        var params=Map.of("sourceId",UUID.randomUUID(),"jobId",UUID.randomUUID(),"actionId",UUID.randomUUID());
        var preview=configuration.getMappedStatement(prefix+"selectStateJson").getBoundSql(params);
        assertThat(preview.getSql()).contains("attachment_normal_job_recovery_state(j.id)","j.source_id=? AND j.batch_id IS NULL","s.semantic_status_code<>'EXCLUDED'")
                .doesNotContain("extracted_text","review_note","source_url","UPDATE ");
        assertThat(preview.getParameterMappings()).extracting(p->p.getProperty()).containsExactly("jobId","sourceId");
        assertThat(configuration.getMappedStatement(prefix+"updateSourceRestoration").getBoundSql(params).getSql())
                .contains("attachment_row_version=s.attachment_row_version+1","s.attachment_row_version=a.expected_attachment_version","->>'previewHash'=a.preview_hash")
                .doesNotContain("SET classification_row_version=", "UPDATE announcements ");
        assertThat(configuration.getMappedStatement(prefix+"updateRolledBack").getBoundSql(params).getSql())
                .contains("normal_rollback_action_id=a.id","rollback_status_code='ROLLED_BACK'")
                .doesNotContain("SET job_status_code", "application_status_code=", "applied_input_hash=", "DELETE ");
    }
    @Test void rollbackSqlKeepsExplicitScopeAndVersionCasWithoutNetworkOrOriginalText() {
        var prefix="com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentBatchRollbackDao.";
        var params=Map.of("batchId",UUID.randomUUID(),"jobId",UUID.randomUUID());
        var candidate=configuration.getMappedStatement(prefix+"selectCandidateDetails").getBoundSql(params);
        assertThat(candidate.getSql()).contains("attachment_confirmation_restored_binding","s.attachment_row_version=j.applied_attachment_version").doesNotContain("review_note","body_text","extracted_text");
        assertThat(candidate.getParameterMappings()).extracting(p->p.getProperty()).containsExactly("batchId","jobId");
        assertThat(configuration.getMappedStatement(prefix+"updateSourceRestoration").getBoundSql(params).getSql())
                .contains("attachment_row_version=s.attachment_row_version+1","s.classification_row_version=j.applied_source_version","NOT EXISTS(SELECT 1 FROM announcement_source_links");
        assertThat(configuration.getMappedStatement(prefix+"updateFailure").getBoundSql(params).getSql()).contains("rollback_attempt_count<3","interval '30 seconds'");
        assertThat(configuration.getMappedStatement(prefix+"updateProgress").getBoundSql(params).getSql()).contains("b.scope_item_count","ROLLBACK_PARTIAL_FAILED");
    }
    private static Configuration configuration;
    private static final String EVIDENCE="com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentEvidenceDao.";
    private static final String ROLE="com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentRoleDao.";
    @Test void confirmationReadersUseCompletedRestorationAndKeepOriginalReceiptFields() {
        String review="com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentReviewDao.";
        var parameters=Map.of("sourceId",UUID.randomUUID(),"confirmationId",UUID.randomUUID());
        var bound=configuration.getMappedStatement(review+"selectRestoredBindingDetails").getBoundSql(parameters);
        assertThat(bound.getSql()).contains("attachment_confirmation_restored_binding(?,?)").doesNotContain("review_note","confirmed_by");
        assertThat(bound.getParameterMappings()).extracting(p->p.getProperty()).containsExactly("sourceId","confirmationId");
        assertThat(configuration.getMappedStatement(review+"selectConfirmationDetails").getBoundSql(parameters).getSql())
                .contains("confirmed_source_version,confirmed_attachment_version,confirmed_at").doesNotContain("restored_binding");
        var current=configuration.getMappedStatement("com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentCurrentDao.selectSourceDetails").getBoundSql(parameters).getSql();
        assertThat(current).contains("attachment_confirmation_restored_binding(c.source_id,c.id)","cf.effective_source_version=s.classification_row_version",
                "cf.effective_attachment_version=s.attachment_row_version-1","confirmed_link.attachment_confirmation_id=cf.id");
    }
    @BeforeAll static void loadXmlWithoutDatabase() throws Exception {
        configuration=new Configuration();
        configuration.getTypeHandlerRegistry().register(new UuidTypeHandler());
        for(var resource:new PathMatchingResourcePatternResolver().getResources("classpath*:mapper/**/*.xml")) {
            try(var input=resource.getInputStream()) {
                new XMLMapperBuilder(input,configuration,resource.toString(),configuration.getSqlFragments()).parse();
            }
        }
    }
    @Test void checkpointQueriesBindSourceScopedLeaseWithoutRawSubstitution() {
        var parameters=Map.of("jobId",UUID.randomUUID(),"leaseToken",UUID.randomUUID(),"locatorHash","a".repeat(64),"fileJson","{}");
        var read=configuration.getMappedStatement(EVIDENCE+"selectFileCheckpoint").getBoundSql(parameters);
        assertThat(read.getSql()).contains("e.id=j.base_evaluation_id","j.lease_token=?","c.stable_locator_hash=?");
        assertThat(read.getParameterMappings()).extracting(p->p.getProperty()).containsExactly("jobId","locatorHash","leaseToken");
        var insert=configuration.getMappedStatement(EVIDENCE+"insertFileCheckpoint").getBoundSql(parameters);
        assertThat(insert.getSql()).contains("ON CONFLICT (job_id,stable_locator_hash) DO NOTHING");
        assertThat(insert.getParameterMappings()).extracting(p->p.getProperty()).containsExactly("locatorHash","fileJson","jobId","leaseToken");
    }
    @Test void retryScopeQueriesBindOnlyKnownIdsAndLease() {
        String prefix="com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentRetryDao.";
        var read=configuration.getMappedStatement(prefix+"selectRetryFileList").getBoundSql(Map.of("jobId",UUID.randomUUID(),"leaseToken",UUID.randomUUID()));
        assertThat(read.getSql()).contains("f.set_id=j.reference_set_id","j.operation_code='RETRY_FILES'");
        assertThat(read.getParameterMappings()).extracting(p->p.getProperty()).containsExactly("jobId","leaseToken");
        var insert=configuration.getMappedStatement(prefix+"insertRetryFile").getBoundSql(Map.of("jobId",UUID.randomUUID(),"sourceId",UUID.randomUUID(),"setId",UUID.randomUUID(),"fileId",UUID.randomUUID()));
        assertThat(insert.getParameterMappings()).extracting(p->p.getProperty()).containsExactly("jobId","sourceId","setId","fileId");
    }
    @Test void collectionReadDoesNotLockAndManualApisShareTheSameRateScope() {
        String prefix="com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentCollectionDao.";
        var read=configuration.getMappedStatement(prefix+"selectActivePolicyDetails").getBoundSql(Map.of("ruleReleaseId",UUID.randomUUID(),"lock",false));
        var write=configuration.getMappedStatement(prefix+"selectActivePolicyDetails").getBoundSql(Map.of("ruleReleaseId",UUID.randomUUID(),"lock",true));
        assertThat(read.getSql()).doesNotContain("FOR SHARE");assertThat(write.getSql()).contains("FOR SHARE OF p,r");
        assertThat(read.getParameterMappings()).extracting(p->p.getProperty()).containsExactly("ruleReleaseId");
        var params=Map.of("sourceId",UUID.randomUUID());
        var collect=configuration.getMappedStatement(prefix+"selectManualRequestRateAllowed").getBoundSql(params);
        var retry=configuration.getMappedStatement("com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentRetryDao.selectRetryRateAllowed").getBoundSql(params);
        for(var bound:java.util.List.of(collect,retry)) {
            assertThat(bound.getSql()).contains("requested_by IS NOT NULL","operation_code IN ('COLLECT','RETRY_FILES')","interval '24 hours'","interval '60 seconds'");
            assertThat(bound.getParameterMappings()).extracting(p->p.getProperty()).containsExactly("sourceId");
        }
    }
    @Test void extractionCompletionTimeHasJdbcHandlerAndNullableCompatibility() {
        var insert=new AttachmentEvidenceCommands.ExtractionInsert(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),
                1,"ISOLATED_JAVA","1.0.0","b".repeat(64),"COMPLETE_TEXT","검증","c".repeat(64),"[]",2,1,100,null,OffsetDateTime.now());
        var bound=configuration.getMappedStatement(EVIDENCE+"insertExtraction").getBoundSql(insert);
        var timestamp=bound.getParameterMappings().getLast();
        assertThat(timestamp.getProperty()).isEqualTo("completedAt");
        assertThat(timestamp.getJavaType()).isEqualTo(OffsetDateTime.class);
        assertThat(timestamp.getTypeHandler()).isNotNull();
        assertThat(bound.getSql()).contains("COALESCE(?,clock_timestamp())");
    }
    @Test void unmatchedEnforceQueryKeepsRetiredRulePoliciesAndBindsNullableReleaseSafely() {
        var statement=configuration.getMappedStatement("com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentIntakeDao.selectUnmatchedEnforcePolicyId");
        var parameters=new java.util.HashMap<String,Object>();
        parameters.put("ruleReleaseId",null);
        var bound=statement.getBoundSql(parameters);
        assertThat(bound.getSql()).contains("p.policy_status_code='ACTIVE'","p.mode_code='ENFORCE'",
                "p.rule_release_id IS DISTINCT FROM ?","OR NOT EXISTS", "r.release_status_code='ACTIVE'",
                "ORDER BY p.id LIMIT 1 FOR SHARE OF p")
                .doesNotContain("JOIN announcement_source_classification_rule_releases");
        assertThat(bound.getParameterMappings()).extracting(p->p.getProperty()).containsExactly("ruleReleaseId");
        assertThat(bound.getParameterMappings().getFirst().getJdbcType()).isEqualTo(org.apache.ibatis.type.JdbcType.OTHER);
        assertThat(statement.isUseCache()).isFalse();
    }
    @Test void historyQueriesKeepSourceEvaluationAndExactExtractionScope() {
        String prefix="com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentHistoryDao.";
        var params=Map.of("sourceId",UUID.randomUUID(),"evaluationId",UUID.randomUUID(),"fileId",UUID.randomUUID(),"offset",0,"size",20);
        var matches=configuration.getMappedStatement(prefix+"selectMatchList").getBoundSql(params);
        var count=configuration.getMappedStatement(prefix+"selectMatchCount").getBoundSql(params);
        assertThat(matches.getSql()).contains("i.extraction_id=m.extraction_id","g.release_id=m.rule_release_id","AND m.file_id=?");
        assertThat(count.getSql()).contains("i.extraction_id=m.extraction_id","AND m.file_id=?");
        assertThat(matches.getSql()).doesNotContain("extracted_text","blocks_json","safe_locator_json","is_enabled");
        assertThat(matches.getParameterMappings()).extracting(p->p.getProperty()).containsExactly("sourceId","evaluationId","fileId","size","offset");
        var inputs=configuration.getMappedStatement(prefix+"selectInputList").getBoundSql(params);
        assertThat(inputs.getSql()).contains("LEFT JOIN announcement_source_attachment_extractions x ON x.id=i.extraction_id")
                .doesNotContain("attempt_no DESC");
        var history=configuration.getMappedStatement(prefix+"selectEvaluationList").getBoundSql(params);
        assertThat(history.getSql()).contains("WHERE e.source_id=?","ORDER BY e.evaluated_at DESC,e.id DESC");
    }
    @Test void policyListAndCountShareFiltersAndOnlyMutationLocksRows() {
        String prefix="com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentPolicyDao.";
        var search=new com.saneb.domain.announcementattachment.vo.AttachmentPolicyManagementRows.Search("DRAFT",UUID.randomUUID(),10,20);
        var list=configuration.getMappedStatement(prefix+"selectPolicyList").getBoundSql(search);
        var count=configuration.getMappedStatement(prefix+"selectPolicyCount").getBoundSql(search);
        assertThat(list.getSql()).contains("p.policy_status_code=?","p.rule_release_id=?","ORDER BY p.created_at DESC,p.id DESC LIMIT ? OFFSET ?")
                .doesNotContain("settings_json","profile_manifest_json","creation_request_hash","FOR UPDATE");
        assertThat(count.getSql()).contains("p.policy_status_code=?","p.rule_release_id=?");
        assertThat(list.getParameterMappings()).extracting(p->p.getProperty()).containsExactly("policyStatusCode","ruleReleaseId","size","offset");
        assertThat(count.getParameterMappings()).extracting(p->p.getProperty()).containsExactly("policyStatusCode","ruleReleaseId");
        assertThat(configuration.getMappedStatement(prefix+"selectPolicyDetails").getBoundSql(Map.of("policyId",UUID.randomUUID(),"lock",false)).getSql()).doesNotContain("FOR UPDATE");
        assertThat(configuration.getMappedStatement(prefix+"selectPolicyDetails").getBoundSql(Map.of("policyId",UUID.randomUUID(),"lock",true)).getSql()).contains("FOR UPDATE OF p");
    }
    @Test void policyWritesBindDraftCasAndSeparateCreationAndFamilyLocks() {
        String prefix="com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentPolicyDao.";
        var key=configuration.getMappedStatement(prefix+"selectCreationLock").getBoundSql(Map.of("key",UUID.randomUUID()));
        var family=configuration.getMappedStatement(prefix+"selectFamilyLock").getBoundSql(Map.of("policyCode","ATT-QA"));
        assertThat(key.getSql()).contains("pg_advisory_xact_lock(hashtextextended(CAST(? AS text),73101))");
        assertThat(family.getSql()).contains("pg_advisory_xact_lock(hashtextextended(?,73102))");
        assertThat(key.getParameterMappings()).extracting(p->p.getProperty()).containsExactly("key");
        var request=new com.saneb.domain.announcementattachment.vo.AttachmentPolicyManagementRows.Update(UUID.randomUUID(),3,"OFF",UUID.randomUUID(),"{}","[]");
        var update=configuration.getMappedStatement(prefix+"updatePolicyDraft").getBoundSql(request);
        assertThat(update.getSql()).contains("policy_hash=NULL,row_version=row_version+1","policy_status_code='DRAFT' AND row_version=?")
                .doesNotContain("published_at=","creation_request_hash=","UPDATE announcement_source");
        assertThat(update.getParameterMappings()).extracting(p->p.getProperty()).containsExactly("modeCode","ruleReleaseId","settingsJson","profileManifestJson","policyId","expectedVersion");
        var insert=new com.saneb.domain.announcementattachment.vo.AttachmentPolicyManagementRows.Insert(UUID.randomUUID(),"ATT-QA",1,"OFF",UUID.randomUUID(),"{}","[]",UUID.randomUUID(),null,UUID.randomUUID(),"a".repeat(64),"CREATE");
        assertThat(configuration.getMappedStatement(prefix+"insertPolicy").getBoundSql(insert).getSql()).contains("'DRAFT'").doesNotContain("'ACTIVE'","published_at","policy_hash");
        assertThat(configuration.getMappedStatement(prefix+"selectLatestVersion").isFlushCacheRequired()).isTrue();
        assertThat(configuration.getMappedStatement(prefix+"selectCreationDetails").isFlushCacheRequired()).isTrue();
    }
    @Test void classificationCheckHistoryBindsPolicyAndRuleVersionsWithoutLockOrRawContent() {
        String prefix="com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentPolicyCheckDao.";
        var search=new com.saneb.domain.announcementattachment.vo.AttachmentPolicyCheckRows.Search(UUID.randomUUID(),10,20);
        var list=configuration.getMappedStatement(prefix+"selectCheckList").getBoundSql(search);
        assertThat(list.getSql()).contains("p.row_version=c.policy_row_version","r.row_version=c.rule_row_version","WHERE c.policy_id=?","ORDER BY c.created_at DESC,c.id DESC LIMIT ? OFFSET ?")
                .doesNotContain("FOR UPDATE","settings_json","extracted_text","body_text");
        assertThat(list.getParameterMappings()).extracting(p->p.getProperty()).containsExactly("policyId","size","offset");
        assertThat(configuration.getMappedStatement(prefix+"selectCheckCount").getBoundSql(search).getSql()).contains("WHERE c.policy_id=?");
        assertThat(configuration.getMappedStatement(prefix+"selectCheckDetails").isFlushCacheRequired()).isTrue();
    }
    @Test void policyQaHistoryBindsPolicyPaginationWithoutLoadingInputSnapshots() {
        String prefix="com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentPolicyValidationDao.";
        var search=new com.saneb.domain.announcementattachment.vo.AttachmentPolicyValidationRows.Search(UUID.randomUUID(),20,40);
        var list=configuration.getMappedStatement(prefix+"selectRunList").getBoundSql(search);
        assertThat(list.getSql()).contains("NULL::text AS input_snapshot_json", "p.row_version=v.policy_row_version", "r.row_version=v.rule_row_version",
                "WHERE v.policy_id=?", "ORDER BY v.created_at DESC,v.id DESC LIMIT ? OFFSET ?")
                .doesNotContain("v.input_snapshot_json::text", "FOR UPDATE", "extracted_text");
        assertThat(list.getParameterMappings()).extracting(p->p.getProperty()).containsExactly("policyId","size","offset");
        assertThat(configuration.getMappedStatement(prefix+"selectRunCount").getBoundSql(search).getSql()).contains("WHERE policy_id=?");
        assertThat(configuration.getMappedStatement(prefix+"selectRunDetails").getBoundSql(Map.of("runId",UUID.randomUUID(),"lock",true)).getSql())
                .contains("WHERE v.id=?", "FOR UPDATE OF v");
    }
    @Test void policyQaClaimsTheWorkersGlobalSlotAndLeavesRoomForFileCleanup() {
        String prefix="com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentPolicyValidationDao.";
        var params=Map.of("runId",UUID.randomUUID(),"leaseToken",UUID.randomUUID());
        var claim=configuration.getMappedStatement(prefix+"updateClaim").getBoundSql(params);
        assertThat(claim.getSql()).contains("interval '8 minutes'", "WHERE id=? AND run_status_code='PENDING'");
        var slot=configuration.getMappedStatement(prefix+"insertExtractionLease").getBoundSql(params);
        assertThat(slot.getSql()).contains("SELECT 'EXTRACTION','GLOBAL',1", "DO UPDATE SET job_id=NULL,job_lease_token=NULL",
                "announcement_attachment_resource_leases.lease_expires_at<=clock_timestamp()");
        assertThat(slot.getParameterMappings()).extracting(p->p.getProperty()).containsExactly("runId","leaseToken");
        var allowed=configuration.getMappedStatement(prefix+"selectExecutionAllowed").getBoundSql(params);
        assertThat(allowed.getSql()).contains("l.policy_validation_lease_token=v.lease_token", "v.lease_token=?",
                "v.run_status_code='RUNNING'", "v.lease_expires_at>clock_timestamp()+interval '40 seconds'",
                "l.lease_expires_at>clock_timestamp()+interval '40 seconds'");
        assertThat(configuration.getMappedStatement(prefix+"selectExecutionAllowed").isFlushCacheRequired()).isTrue();
    }
    @Test void policyQaCompletionAndEvidenceAreFencedAndCancellationWins() {
        String prefix="com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentPolicyValidationDao.";
        var params=Map.of("runId",UUID.randomUUID(),"leaseToken",UUID.randomUUID(),"stepCode","INSTALLED_RUNTIME",
                "statusCode","INCOMPLETE","evidenceJson","{}","evidenceHash","a".repeat(64),"errorCode","QA_REQUIRED","expectedVersion",1);
        var step=configuration.getMappedStatement(prefix+"insertStep").getBoundSql(params);
        assertThat(step.getSql()).contains("WHERE id=? AND lease_token=?", "run_status_code='RUNNING' AND lease_expires_at>clock_timestamp()");
        assertThat(step.getParameterMappings()).extracting(p->p.getProperty()).containsExactly("stepCode","statusCode","evidenceJson","evidenceHash","runId","leaseToken");
        var finish=configuration.getMappedStatement(prefix+"updateFinished").getBoundSql(params);
        assertThat(finish.getSql()).contains("WHEN run_status_code='CANCEL_REQUESTED' THEN 'CANCELLED'", "WHERE id=? AND lease_token=?",
                "lease_expires_at>clock_timestamp()").doesNotContain("UPDATE announcement_attachment_policies");
        assertThat(configuration.getMappedStatement(prefix+"updateCancellation").getBoundSql(params).getSql()).contains("WHERE id=? AND row_version=?");
        assertThat(configuration.getMappedStatement(prefix+"deleteExtractionLease").getBoundSql(params).getSql())
                .contains("WHERE policy_validation_id=? AND policy_validation_lease_token=?");
    }
    @Test void policyQaTargetsDoNotHideRecentlyFailedEnabledSources() {
        String prefix="com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentPolicyValidationDao.";
        assertThat(configuration.getMappedStatement(prefix+"selectTargetList").getBoundSql(Map.of()).getSql())
                .contains("LEFT JOIN local_government_notice_parser_profiles", "WHERE s.is_enabled AND s.deleted_at IS NULL ORDER BY s.id LIMIT 1001")
                .doesNotContain("collection_status_code", "validation_status_code");
    }
    @Test void batchScopeCountsAndCandidatesShareFiltersWithoutSilentReadinessExclusion() {
        String prefix="com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentBatchDao.";
        var scope=new com.saneb.domain.announcementattachment.dto.AttachmentBatchRequests.Scope(UUID.randomUUID(),java.util.List.of("BIZINFO","GOV24"),OffsetDateTime.now().minusDays(1),OffsetDateTime.now(),java.time.LocalDate.now(),java.time.LocalDate.now().plusDays(2),100);
        var count=configuration.getMappedStatement(prefix+"selectScopeCounts").getBoundSql(scope);
        var list=configuration.getMappedStatement(prefix+"selectCandidateList").getBoundSql(scope);
        for(var bound:java.util.List.of(count,list)) assertThat(bound.getSql()).contains("s.data_purpose_code='PRODUCTION'", "s.collected_at>=? AND s.collected_at<?",
                "s.application_end_date>=?", "s.application_end_date<=?", "announcement_source_links",
                "CASE WHEN ?='GOV24' THEN 'GOV24_PUBLIC_SERVICE' ELSE ? END");
        assertThat(list.getSql()).contains("ORDER BY s.collected_at,s.id LIMIT ?", "AS active_job_id")
                .doesNotContain("s.source_url", "e.rule_release_id=?", "FOR UPDATE", "job_status_code='SUCCEEDED'");
        assertThat(count.getSql()).contains("'LINKED_PROTECTED'", "'TITLE_OR_BASE_NOT_ELIGIBLE'", "'CANDIDATE'");
        assertThat(configuration.getMappedStatement(prefix+"selectPolicyDetails").getBoundSql(Map.of("policyId",UUID.randomUUID(),"lock",false)).getSql()).doesNotContain("FOR SHARE");
    }
    @Test void fullInventoryGov24FilterUsesSameBoundCanonicalTranslationForDigestAndAllMaterialization() {
        String prefix="com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentBackfillDao.";
        var scope=new com.saneb.domain.announcementattachment.dto.AttachmentBackfillRequests.Scope(UUID.randomUUID(),java.util.List.of("GOV24"),
                OffsetDateTime.now().minusDays(1),OffsetDateTime.now(),null,null,1000);
        for(String method:java.util.List.of("selectScopeDigest","insertSegments","insertItems")) {
            var bound=configuration.getMappedStatement(prefix+method).getBoundSql(Map.of("scope",scope,"runId",UUID.randomUUID()));
            assertThat(bound.getSql().replaceAll("(?m)--[^\\r\\n]*", "")).contains("s.provider_code IN", "CASE WHEN ?='GOV24' THEN 'GOV24_PUBLIC_SERVICE' ELSE ? END")
                    .doesNotContain("LIMIT", "CASE WHEN s.provider_code");
            var values=bound.getParameterMappings().stream().filter(p->p.getProperty().startsWith("__frch_provider_"))
                    .map(p->bound.getAdditionalParameter(p.getProperty())).toList();
            assertThat(values).containsExactly("GOV24","GOV24");
        }
    }
    @Test void batchScopeJobIsNotPendingAndNeitherReservationNorCancellationMutatesSource() {
        String prefix="com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentBatchDao.";
        var cancel=configuration.getMappedStatement(prefix+"updateScopeCancellation").getBoundSql(Map.of("batchId",UUID.randomUUID(),"expectedVersion",1));
        assertThat(cancel.getSql()).contains("WHERE id=? AND row_version=? AND batch_status_code='SCOPE_READY'")
                .doesNotContain("UPDATE announcement_source_snapshots");
        var job=configuration.getMappedStatement(prefix+"insertScopeJob").getBoundSql(Map.of("batchId",UUID.randomUUID(),"job",Map.of(),"providerCode","BIZINFO"));
        assertThat(job.getSql()).contains("'SCOPE_READY'", "frozen_provider_code", "batch_id").doesNotContain("'PENDING'", "UPDATE announcement_source_snapshots", "is_current");
        var items=configuration.getMappedStatement(prefix+"selectItemList").getBoundSql(new com.saneb.domain.announcementattachment.vo.AttachmentBatchRows.Search(UUID.randomUUID(),20,0));
        assertThat(items.getSql()).contains("WHERE batch_id=?", "LIMIT ? OFFSET ?").doesNotContain("announcement_source_snapshots", "scope_json", "source_url", "title");
    }
    @Test void batchCollectionControlsOnlyFrozenJobsAndRequireApprovedCas() {
        String prefix="com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentBatchDao.";
        var parameters=Map.of("batchId",UUID.randomUUID(),"expectedVersion",0,"actorId",UUID.randomUUID(),"approvalHash","a".repeat(64));
        String start=configuration.getMappedStatement(prefix+"updateCollectionStart").getBoundSql(parameters).getSql();
        assertThat(start).contains("approved_by=?", "collection_approval_hash=?", "row_version=?", "batch_status_code='SCOPE_READY' AND deleted_item_count=0");
        String pending=configuration.getMappedStatement(prefix+"updateCollectionJobsPending").getBoundSql(parameters).getSql();
        assertThat(pending).contains("WHERE batch_id=? AND job_status_code='SCOPE_READY'").doesNotContain("INSERT", "attempt_count=", "reserved_download_bytes=", "UPDATE announcement_source_snapshots");
        assertThat(configuration.getMappedStatement(prefix+"selectFixedCandidateList").getBoundSql(parameters).getSql())
                .contains("j.batch_id=?", "announcement_source_links", "other.id<>").doesNotContain("collected_at", "LIMIT", "source_url");
        for(String action:java.util.List.of("updateCollectionPause","updateCollectionResume"))
            assertThat(configuration.getMappedStatement(prefix+action).getBoundSql(parameters).getSql()).contains("row_version=?")
                    .doesNotContain("UPDATE announcement_attachment_jobs", "approved_by=", "scope_json=");
    }
    @Test void batchProgressRequiresAllItemsAndSealedEvidenceAndDoesNotUnpauseOrApply() {
        String prefix="com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentBatchDao.";
        String progress=configuration.getMappedStatement(prefix+"updateCollectionProgress").getBoundSql(Map.of()).getSql();
        assertThat(progress).contains("FOR UPDATE SKIP LOCKED LIMIT 100", "remaining+deleted_item_count=scope_item_count",
                "succeeded=scope_item_count AND deleted_item_count=0", "j.preview_evaluation_id IS NOT NULL", "sealed.set_status_code='SEALED'", "'COLLECTION_PARTIAL_FAILED'")
                .doesNotContain("COLLECTION_PAUSED", "APPLIED", "UPDATE announcement_source_snapshots");
        String guard=configuration.getMappedStatement("com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentJobDao.selectExternalExecutionAllowed")
                .getBoundSql(Map.of("jobId",UUID.randomUUID(),"leaseToken",UUID.randomUUID())).getSql();
        assertThat(guard).contains("b.collection_started_at IS NOT NULL", "b.collection_approval_hash IS NOT NULL", "attachment_batch_job_input_unchanged(j.id)");
    }
    @Test void roleCopyAndCurrentProjectionResolveAllCrossNamespaceMappings() {
        var copy=new AttachmentRoleRows.Copy(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),
                UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),"GUIDE","MANUAL");
        var bound=configuration.getMappedStatement(ROLE+"insertExtractionCopy").getBoundSql(copy);
        assertThat(bound.getSql()).contains("original.created_at,original.id","original.source_id=?");
        var current=configuration.getMappedStatement("com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentCurrentDao.selectSourceDetails")
                .getBoundSql(Map.of("sourceId",copy.sourceId()));
        assertThat(current.getSql()).contains("q.set_id").doesNotContain("last_set_id");
        assertThat(current.getSql()).contains("active_job.source_id=s.id", "active_job.batch_id IS NULL",
                "active_job.job_status_code IN ('SCOPE_READY','PENDING','RUNNING','RETRY_WAIT','PAUSED')", "AS active_normal_job");
    }
    @Test void queueCountAndListShareExactStatusPredicateBeforePagination() {
        String prefix="com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentCurrentDao.";
        var search=new com.saneb.domain.announcementattachment.vo.AttachmentSourceSearchCondition(
                "BIZINFO","REVIEW_REQUIRED",null,null,null,"소상공인",null,null,2,20,"TECHNICAL_EXCEPTION");
        var page=configuration.getMappedStatement(prefix+"selectSourceList").getBoundSql(search);
        var count=configuration.getMappedStatement(prefix+"selectSourceCount").getBoundSql(search);
        // 공백 정규화 이후 마지막 WHERE가 아닌 실제 source 범위부터 동일해야 한다.
        String pageSql=page.getSql().replaceAll("\\s+"," "), countSql=count.getSql().replaceAll("\\s+"," ");
        String marker="WHERE s.data_purpose_code";
        assertThat(pageSql.substring(pageSql.indexOf(marker),pageSql.lastIndexOf(" ORDER BY s.collected_at"))).isEqualTo(countSql.substring(countSql.indexOf(marker)).stripTrailing());
        for(String code:com.saneb.domain.announcementattachment.service.AttachmentProcessingFlow.STATUS_CODES)
            assertThat(countSql).contains("'"+code+"'");
        assertThat(countSql).contains("active_job.batch_id IS NULL", "aset.manifest_hash ~ '^[0-9a-f]{64}$'",
                "IS NOT TRUE", "BETWEEN 1 AND 10", "sealed.set_status_code='SEALED'")
                .doesNotContain("SELECT *", "FOR UPDATE", "INSERT ", "UPDATE ", "DELETE ", "OFFSET");
        assertThat(count.getParameterMappings()).extracting(p->p.getProperty()).containsExactly(
                "providerCode","effectiveStatusCode","processingFlowStatusCode","keyword","keyword");
        assertThat(page.getParameterMappings()).extracting(p->p.getProperty()).endsWith("size","page","size");
        var noFlow=new com.saneb.domain.announcementattachment.vo.AttachmentSourceSearchCondition(null,null,null,null,null,null,null,null,1,20);
        assertThat(configuration.getMappedStatement(prefix+"selectSourceCount").getBoundSql(noFlow).getSql())
                .doesNotContain("'READY_FOR_FINAL_REVIEW'");
    }
    @Test void batchPreviewUsesExactSealedInputsAndNeverCollectsOrReadsRawContent() {
        String prefix="com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentBatchPreviewDao.";
        String live=configuration.getMappedStatement(prefix+"selectLiveItemList").getBoundSql(Map.of("batchId",UUID.randomUUID())).getSql();
        assertThat(live).contains("WHERE j.batch_id=?", "sealed.set_status_code='SEALED'", "files.input_count=files.file_count",
                "files.download_failed_count=0 AND files.incomplete_count=0", "x.id=i.extraction_id", "e.id=j.preview_evaluation_id", "AND sealed.set_status_code='SEALED'", "e.set_id=sealed.id",
                "LIMIT 1001", "LIMIT 11", "'baseTargetCodes'", "'previousTargetCodes'", "'proposedTargetCodes'", "'confirmedTargetCodes'", "t.origin_code='CONFIRMED'")
                .doesNotContain("extracted_text", "display_name", "s.source_url", "base.title,", "ORDER BY e.attempt_no", "UPDATE ", "INSERT ");
        assertThat(configuration.getMappedStatement(prefix+"selectItemList").getBoundSql(new com.saneb.domain.announcementattachment.vo.AttachmentBatchPreviewRows.Search(UUID.randomUUID(),20,0)).getSql())
                .contains("i.evidence_json::text", "j.id=i.job_id AND j.batch_id=i.batch_id", "WHERE i.preview_id=?").doesNotContain("scope_json", "source_url");
    }
    @Test void batchApplicationBindsExactApprovalSelectionAndSourceVersionsWithoutHttpOrRawEvidence() {
        String prefix="com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentBatchApplicationDao.";
        var parameters=Map.of("batchId",UUID.randomUUID(),"jobId",UUID.randomUUID(),"previewId",UUID.randomUUID());
        String work=configuration.getMappedStatement(prefix+"selectWorkDetails").getBoundSql(parameters).getSql();
        assertThat(work).contains("a.id=b.application_approval_id", "i.preview_id=j.application_preview_id", "b.batch_status_code='APPLYING'", "j.application_status_code='PENDING'", "j.id=?")
                .doesNotContain("source_url","extracted_text","download_url","SELECT *");
        String pending=configuration.getMappedStatement(prefix+"updateJobsPending").getBoundSql(parameters).getSql();
        assertThat(pending).contains("i.is_selected AND i.is_eligible", "j.application_status_code='NOT_REQUESTED'", "i.batch_id=j.batch_id");
        String source=configuration.getMappedStatement(prefix+"updateSourceApplication").getBoundSql(parameters).getSql();
        assertThat(source).contains("attachment_row_version=s.attachment_row_version+1", "is_attachment_review_required=true", "attachment_batch_job_input_unchanged(j.id)")
                .doesNotContain("UPDATE announcements ", "classification_row_version=classification_row_version+1");
        var bound=configuration.getMappedStatement("com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentBatchPreviewDao.selectLiveItemDetails").getBoundSql(parameters);
        assertThat(bound.getSql()).contains("sealed.set_status_code='SEALED'", "e.set_id=sealed.id", "j.id=?");
        assertThat(bound.getParameterMappings()).extracting(p->p.getProperty()).containsExactly("batchId","jobId");
    }
    @Test void batchApplicationProgressNeverClaimsSubsetAsWholeSuccessAndRetriesAreBounded() {
        String prefix="com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentBatchApplicationDao.";
        String progress=configuration.getMappedStatement(prefix+"updateProgress").getBoundSql(Map.of("batchId",UUID.randomUUID())).getSql();
        assertThat(progress).contains("b.deleted_item_count=0", "=b.scope_item_count", "ELSE 'APPLY_PARTIAL_FAILED'", "b.batch_status_code='APPLYING'");
        String failure=configuration.getMappedStatement(prefix+"updateFailure").getBoundSql(Map.of("jobId",UUID.randomUUID())).getSql();
        assertThat(failure).contains("application_attempt_count+1>=3", "interval '30 seconds'", "application_status_code='PENDING'", "application_attempt_count<3");
    }
    @Test void normalEvaluationCompletesEvidenceInsideTheLeaseFencedWrite() {
        var bound=configuration.getMappedStatement("com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentEvaluationDao.updateJobCompleted")
                .getBoundSql(Map.of("jobId",UUID.randomUUID(),"leaseToken",UUID.randomUUID(),"evaluationId",UUID.randomUUID(),"previewHash","a".repeat(64),"jobStatus","SUCCEEDED"));
        assertThat(bound.getSql()).contains("applied_source_version=CASE WHEN batch_id IS NULL AND application_status_code='PENDING' THEN expected_source_version",
                "THEN attachment_normal_job_application_hash(id,?) ELSE applied_input_hash END", "lease_token=? AND job_status_code='RUNNING' AND lease_expires_at>clock_timestamp()")
                .doesNotContain("UPDATE announcements ", "reservation_attachment_version=", "previous_confirmation_id=", "extracted_text");
        assertThat(bound.getParameterMappings()).extracting(p->p.getProperty()).containsExactly(
                "jobStatus","evaluationId","previewHash","evaluationId","evaluationId","jobId","leaseToken");
    }
    @Test void normalRecoveryHistoryKeepsListAndCountScopedAndDoesNotLeakExecutionOrReason() {
        String prefix="com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentNormalRollbackDao.";
        var parameters=Map.of("sourceId",UUID.randomUUID(),"size",10,"offset",20);
        for(String query:java.util.List.of("selectJobList","selectJobCount")) {
            String sql=configuration.getMappedStatement(prefix+query).getBoundSql(parameters).getSql();
            assertThat(sql).contains("j.source_id=?", "j.batch_id IS NULL", "s.data_purpose_code='PRODUCTION'", "s.semantic_status_code<>'EXCLUDED'")
                    .doesNotContain("reason_hash", "idempotency_key", "execution_snapshot_json", "source_url", "extracted_text", "FOR UPDATE", "UPDATE ", "SELECT *");
        }
        var bound=configuration.getMappedStatement(prefix+"selectJobList").getBoundSql(parameters);
        assertThat(bound.getSql()).contains("j.normal_rollback_action_id", "ORDER BY j.created_at DESC,j.id DESC LIMIT ? OFFSET ?");
        assertThat(bound.getParameterMappings()).extracting(p->p.getProperty()).containsExactly("sourceId","size","offset");
    }
    @Test void batchSelectionSupportsClearAndOnlyChangesFixedJobsWithVersionedPreviewPointer() {
        String prefix="com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentBatchPreviewDao.";
        String clear=configuration.getMappedStatement(prefix+"updateJobSelection").getBoundSql(Map.of("batchId",UUID.randomUUID(),"selectedJobIds",java.util.List.of())).getSql();
        assertThat(clear).contains("is_selected_for_application=", "false", "WHERE batch_id=?").doesNotContain(" IN ()", "UPDATE announcement_source_snapshots");
        String selected=configuration.getMappedStatement(prefix+"updateJobSelection").getBoundSql(Map.of("batchId",UUID.randomUUID(),"selectedJobIds",java.util.List.of(UUID.randomUUID()))).getSql();
        assertThat(selected).contains("id IN", "WHERE batch_id=?");
        String ready=configuration.getMappedStatement(prefix+"updatePreviewReady").getBoundSql(Map.of("batchId",UUID.randomUUID(),"expectedVersion",1,"statusCode","PREVIEW_READY","previewHash","a".repeat(64),"previewId",UUID.randomUUID())).getSql();
        assertThat(ready).contains("current_preview_id=?", "preview_hash=?", "row_version=? AND batch_status_code='PREVIEW_RUNNING'").doesNotContain("is_current", "attachment_policy_id");
    }
}
