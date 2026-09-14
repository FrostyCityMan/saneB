/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: MigrationContractTest.java
 * 작성자: 김도훈
 *
 */

package com.saneb.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class MigrationContractTest {
    @Test void backfillMembershipUsesExactOrderedTuplesWithoutRemovingDeferredGuards() throws IOException {
        var sql=new ClassPathResource("db/migration/V83__compare_backfill_batch_membership_as_ordered_sets.sql").getContentAsString(StandardCharsets.UTF_8);
        assertThat(sql).contains("CREATE OR REPLACE FUNCTION check_attachment_backfill_segment_batch()", "ORDER BY j.source_id,j.content_version_id,j.base_evaluation_id,j.rule_release_id,j.frozen_provider_code",
                "ORDER BY i.source_id,i.content_version_id,i.base_evaluation_id,i.rule_release_id,i.provider_code", "IS DISTINCT FROM",
                "binding.scope_item_count+binding.deleted_before_reservation<>binding.item_count",
                "binding.batch_deleted_count+binding.deleted_before_reservation<>binding.deleted_item_count",
                "count(1) FROM announcement_attachment_jobs WHERE batch_id=batch_key", "batch_scope ? 'backfillRunId' OR batch_scope ? 'backfillSegmentNo'",
                "backfill marked batch requires its atomic segment receipt", "backfill segment jobs and original deletion denominators must match exactly", "ERRCODE='23514'");
        assertThat(sql).doesNotContain("DROP ","ALTER TABLE", "DISABLE TRIGGER", "SET CONSTRAINTS", "digest(", "DISTINCT j.", "DISTINCT i.", "NOT EXISTS");
    }
    @Test void publicationLockIncludesAllProviderEvidenceWithoutChangingData() throws IOException {
        var sql=new ClassPathResource("db/migration/V81__lock_provider_qa_evidence_during_policy_publication.sql").getContentAsString(StandardCharsets.UTF_8);
        var previous=new ClassPathResource("db/migration/V77__add_attachment_policy_publication_receipt.sql").getContentAsString(StandardCharsets.UTF_8);
        String originalTables=previous.substring(previous.indexOf("LOCK TABLE ")+11,previous.indexOf(" IN EXCLUSIVE MODE NOWAIT"));
        String expandedTables=sql.substring(sql.indexOf("LOCK TABLE ")+11,sql.indexOf(" IN EXCLUSIVE MODE NOWAIT"));
        assertThat(expandedTables).startsWith(originalTables).endsWith("announcement_attachment_provider_qa_run_plans");
        assertThat(expandedTables.split(",")).hasSize(21);
        assertThat(sql).contains("CREATE OR REPLACE FUNCTION attachment_policy_publication_lock()", "announcement_attachment_provider_qa_runs,announcement_attachment_provider_qa_cases");
        assertThat(sql).doesNotContain("INSERT INTO", "UPDATE ", "DELETE FROM", "DROP TABLE", "DISABLE TRIGGER");
    }
    @Test void providerQaApprovedSegmentPlanIsImmutableAndRequiredOnlyForNewRuns() throws IOException {
        var sql=new ClassPathResource("db/migration/V80__bind_provider_qa_approved_segment_plan.sql").getContentAsString(StandardCharsets.UTF_8);
        assertThat(sql).contains("CREATE TABLE announcement_attachment_provider_qa_run_plans","provider QA approved plan is immutable",
                "p.created_xid=r.created_xid","r.expected_case_count<=NEW.executable_case_count","sum(c.maximum_seconds+60)",
                "maximum_seconds_including_margin BETWEEN 61 AND 82800","DEFERRABLE INITIALLY DEFERRED","AFTER INSERT ON announcement_attachment_provider_qa_runs");
        assertThat(sql).doesNotContain("ALTER TABLE announcement_attachment_provider_qa_runs","UPDATE announcement_","DELETE FROM", "SELECT *","DISABLE TRIGGER");
    }
    @Test void providerQaLedgerPreservesScopeBudgetsOwnershipAndCompletedIsNotPolicyPublication() throws IOException {
        var sql=new ClassPathResource("db/migration/V79__add_provider_qa_execution_ledger.sql").getContentAsString(StandardCharsets.UTF_8);
        assertThat(sql).contains("CREATE TABLE announcement_attachment_provider_qa_runs", "CREATE TABLE announcement_attachment_provider_qa_cases",
                "maximum_seconds BETWEEN 1 AND 420","maximum_requests BETWEEN 1 AND 44","maximum_bytes BETWEEN 1 AND 83886080",
                "provider QA preparation must seal in the same transaction","DEFERRABLE INITIALLY DEFERRED","UNIQUE(run_id,ordinal)","UNIQUE(run_id,case_code)",
                "provider QA cannot hide unfinished cases","provider QA final usage must equal all case usage",
                "provider QA lease cannot extend or accept a late response","originalFilesRemoved","isPolicyQaPassed",
                "provider_qa_case_id IS NULL AND provider_qa_lease_token IS NULL","NEW.lease_expires_at>clock_timestamp()+interval '8 minutes'");
        assertThat(sql).doesNotContain("SELECT *","r.*","source_url","body_text","extracted_text","UPDATE announcement_source_snapshots",
                "INSERT INTO announcement_attachment_jobs","UPDATE announcement_attachment_policies","DISABLE TRIGGER","NOT VALID");
    }
    @Test void gov24AttachmentConstraintsAddActualProviderWithoutRewritingHistory() throws IOException {
        var sql=new ClassPathResource("db/migration/V78__align_attachment_gov24_provider_code.sql").getContentAsString(StandardCharsets.UTF_8);
        assertThat(sql).contains("ALTER TABLE announcement_attachment_jobs", "announcement_attachment_jobs_frozen_provider_code_check",
                "ALTER TABLE announcement_attachment_backfill_items", "announcement_attachment_backfill_items_provider_code_check",
                "CHECK (frozen_provider_code IN ('BIZINFO','GOV24','GOV24_PUBLIC_SERVICE','LOCAL_GOV_NOTICE'))",
                "CHECK (provider_code IN ('BIZINFO','GOV24','GOV24_PUBLIC_SERVICE','LOCAL_GOV_NOTICE'))");
        assertThat(sql).doesNotContain("UPDATE ","DELETE FROM ","INSERT INTO ","DISABLE TRIGGER","DROP TABLE","NOT VALID");
    }
    @Test void publicationReceiptRequiresVerifiedExactScopeAndAtomicPolicyReplacement() throws IOException {
        var sql=new ClassPathResource("db/migration/V77__add_attachment_policy_publication_receipt.sql").getContentAsString(StandardCharsets.UTF_8);
        assertThat(sql).contains("IN EXCLUSIVE MODE NOWAIT","policy publication receipt is immutable","v.run_status_code='VERIFIED'",
                "s.requested_by=NEW.published_by","NEW.policy_hash<>approved.qa_snapshot_hash","EXCEPT (SELECT i.entity_type_code","EXCEPT (SELECT m.entity_type_code",
                "NEW.runtime_hash IS DISTINCT FROM approved.runtime_hash","p.settings_json->>'extractorConfigHash'=NEW.runtime_hash",
                "previous_policy_row_version+1","DEFERRABLE INITIALLY DEFERRED","publication and previous policy retirement must commit atomically");
        assertThat(sql).doesNotContain("UPDATE announcement_source_snapshots","INSERT INTO announcement_attachment_jobs","SELECT *");
    }
    @Test void publicationScopeSealsExactImmutableMembershipWithoutPublishing() throws IOException {
        var sql=new ClassPathResource("db/migration/V76__add_attachment_policy_publication_scope.sql").getContentAsString(StandardCharsets.UTF_8);
        assertThat(sql).contains("PRIMARY KEY (scope_id,entity_type_code,entity_id)","idempotency_key uuid NOT NULL UNIQUE",
                "FOREIGN KEY (qa_run_id,policy_id)","ix_att_publication_scope_rule","ix_att_publication_scope_qa","r.rule_snapshot_hash",
                "expires_at<=created_at+interval '15 minutes'", "created_xid<>pg_current_xact_id()",
                "publication scope history is immutable", "publication scope members are immutable", "DEFERRABLE INITIALLY DEFERRED",
                "p.row_version=scope_row.policy_row_version", "r.row_version=scope_row.rule_row_version", "EXCEPT (SELECT i.entity_type_code",
                "EXCEPT (SELECT m.entity_type_code", "'POLICY'::varchar", "'SOURCE'::varchar", "'JOB'::varchar", "'COLLECTION_PLAN'::varchar", "'COLLECTOR'::varchar");
        assertThat(sql).doesNotContain("UPDATE announcement_source_snapshots", "UPDATE announcement_attachment_policies", "INSERT INTO announcement_attachment_jobs", "SELECT *", "LIMIT ");
    }
    @Test void linkedBackfillBatchesRequireAtomicExactMembershipAndKeepExistingExecutionGuards() throws IOException {
        var sql=new ClassPathResource("db/migration/V75__link_attachment_backfill_segments_to_batches.sql").getContentAsString(StandardCharsets.UTF_8);
        assertThat(sql).contains("PRIMARY KEY (run_id,segment_no)","batch_id uuid NOT NULL UNIQUE", "backfill segment reservation receipt is immutable",
                "backfill marked batch requires its atomic segment receipt", "binding.scope_item_count+binding.deleted_before_reservation<>binding.item_count",
                "binding.batch_deleted_count+binding.deleted_before_reservation<>binding.deleted_item_count", "ct_att_backfill_fixed_job",
                "ct_att_backfill_segment_batch", "attachment_backfill_batch_input_unchanged(j.id)", "previous_confirmation_id",
                "previous_is_review_required", "NOT EXISTS (SELECT 1 FROM announcement_source_links", "i.input_hash=attachment_backfill_input_hash(i.source_id)");
        assertThat(sql).doesNotContain("UPDATE announcement_source_snapshots", "SET batch_status_code=", "CREATE TABLE users", "SELECT *");
    }
    @Test void fullBackfillInventoryHasImmutableMembershipCascadeDenominatorsAndNoExecutionSideEffects() throws IOException {
        var sql=new ClassPathResource("db/migration/V74__add_attachment_backfill_inventory.sql").getContentAsString(StandardCharsets.UTF_8);
        assertThat(sql).contains("candidate_count bigint", "segment_count=(candidate_count-1)/segment_size+1", "PRIMARY KEY (run_id,source_id)",
                "UNIQUE (run_id,ordinal)", "FOREIGN KEY (base_evaluation_id,source_id,content_version_id,rule_release_id)",
                "ON DELETE CASCADE", "inventory.created_xid<>pg_current_xact_id()", "NEW.segment_no<>(NEW.ordinal-1)/inventory.segment_size+1",
                "backfill item removal requires source or base cascade", "deleted_item_count=deleted_item_count+1,row_version=row_version+1",
                "ct_att_backfill_inventory_complete", "DEFERRABLE INITIALLY DEFERRED", "actual_count<>NEW.candidate_count OR actual_hash<>NEW.candidate_hash",
                "backfill inventory must contain every frozen candidate exactly once", "pg_trigger_depth()<2", "attachment_normal_reservation_context_hash(s.id,e.id)");
        assertThat(sql).doesNotContain("INSERT INTO announcement_attachment_jobs", "UPDATE announcement_source_snapshots", "ALTER TABLE announcement_source_snapshots", "SELECT *", "body_text", "extracted_text");
        assertThat(countOccurrences(sql,"CREATE CONSTRAINT TRIGGER ct_att_backfill_inventory_complete")).isEqualTo(1);
    }
    @Test void normalRollbackRequiresAtomicApprovalForAppliedAndFailedReservationWithoutFabricatingApplication() throws IOException {
        var sql=new ClassPathResource("db/migration/V73__add_attachment_worker_execution_contract.sql").getContentAsString(StandardCharsets.UTF_8);
        assertThat(sql).contains("announcement_attachment_normal_rollback_actions", "mode_code IN ('APPLIED','FAILED_RESERVATION')",
                "attachment_normal_job_recovery_state", "normal rollback requires unchanged preview and explicit effects", "ct_att_normal_rollback_complete",
                "normal rollback must restore source evaluation confirmation and receipt atomically", "normal rollback approval is immutable",
                "attachment recovered normal execution cannot be restarted", "j.reservation_context_hash=attachment_normal_reservation_context_hash",
                "j.application_status_code='PENDING' AND j.job_status_code IN ('FAILED','CONFLICT','CANCELLED')", "j.normal_rollback_action_id=a.id AND j.rollback_status_code='ROLLED_BACK'");
        assertThat(countOccurrences(sql,"CREATE TABLE announcement_attachment_normal_rollback_actions (")).isEqualTo(1);
    }
    @Test void normalJobsFreezeReservationBeforeInvalidationAndKeepAppliedEvidenceImmutable() throws IOException {
        var sql=new ClassPathResource("db/migration/V73__add_attachment_worker_execution_contract.sql").getContentAsString(StandardCharsets.UTF_8);
        assertThat(sql).contains("capture_attachment_normal_reservation", "NEW.batch_id IS NOT NULL OR NEW.execution_snapshot_json IS NULL",
                "reservation_attachment_version", "is_reservation_previous_evaluation_current", "is_reservation_confirmation_valid",
                "coalesce(r.attachment_version,c.confirmed_attachment_version)=s.attachment_row_version", "s.attachment_row_version>2147483645", "aset.manifest_hash=c.set_hash",
                "attachment normal previous bindings are immutable", "attachment normal applied evidence is immutable",
                "NEW.reservation_attachment_version::bigint+2", "attachment normal rollback requires a dedicated approval contract");
        assertThat(countOccurrences(sql,"CREATE FUNCTION attachment_normal_job_application_hash(")).isEqualTo(1);
    }
    @Test void normalApplicationFingerprintContainsOnlyVersionedIdentifiersAndHashes() throws IOException {
        var sql=new ClassPathResource("db/migration/V73__add_attachment_worker_execution_contract.sql").getContentAsString(StandardCharsets.UTF_8);
        String hash=sql.substring(sql.indexOf("CREATE FUNCTION attachment_normal_job_application_hash("),sql.indexOf("CREATE FUNCTION protect_attachment_normal_job_evidence("));
        assertThat(hash).contains("j.execution_snapshot_json", "j.reservation_attachment_version", "s.attachment_row_version",
                "base.decision_status_code", "e.input_hash", "e.decision_hash", "aset.manifest_hash", "p.policy_hash", "r.rule_snapshot_hash", "to_jsonb(s.agency_name)::text",
                "announcement_source_links", "announcement_source_attachment_confirmations", "other.id<>j.id", "aset.set_status_code='SEALED'")
                .doesNotContain("extracted_text", "display_name", "s.source_url", "s.title", "policy_status_code", "release_status_code", "attempt_count", "UPDATE ", "INSERT ");
    }
    @Test void rollbackRequiresFullImmutableApprovalAndSeparatesApplicationFromRecovery() throws IOException {
        var sql=new ClassPathResource("db/migration/V73__add_attachment_worker_execution_contract.sql").getContentAsString(StandardCharsets.UTF_8);
        assertThat(sql).contains("CREATE TABLE announcement_attachment_batch_rollback_actions","CREATE TABLE announcement_attachment_batch_rollback_items",
                "ct_att_batch_rollback_action_complete","rollback_attempt_count BETWEEN 0 AND 3","APPLICATION_CANCELLED_BY_ROLLBACK",
                "attachment rollback terminal result is immutable","attachment rollback job requires approved scope","attachment rollback result does not match restored source");
        assertThat(countOccurrences(sql,"ADD CONSTRAINT uq_att_job_source ")).isEqualTo(1);
    }
    @Test void restoredConfirmationKeepsImmutableReceiptAndRequiresAtomicVersionBoundEvidence() throws IOException {
        String migration=new ClassPathResource("db/migration/V73__add_attachment_worker_execution_contract.sql").getContentAsString(StandardCharsets.UTF_8);
        assertThat(migration).contains("CREATE TABLE announcement_attachment_confirmation_restorations",
                "FOREIGN KEY(job_id,source_id)","FOREIGN KEY(confirmation_id,source_id)","UNIQUE(source_id,attachment_version)",
                "attachment_confirmation_restored_binding", "j.rollback_status_code='ROLLED_BACK'",
                "j.applied_attachment_version::bigint+1=r.attachment_version", "ct_att_confirmation_restoration_complete",
                "stale or unversioned prior confirmation cannot be revalidated by restoration",
                "attachment restoration evidence is immutable", "stale confirmation requires matching restoration evidence");
    }
    @Test void batchApplicationRequiresImmutableApprovalAndCapturesRecoveryVersions() throws IOException {
        String migration=new ClassPathResource("db/migration/V73__add_attachment_worker_execution_contract.sql").getContentAsString(StandardCharsets.UTF_8);
        assertThat(migration).contains("CREATE TABLE announcement_attachment_batch_application_actions", "application_approval_id uuid",
                "FOREIGN KEY(application_preview_id,batch_id)", "application_attempt_count BETWEEN 0 AND 3", "applied_source_version integer", "applied_input_hash varchar(64)",
                "attachment approved preview binding is immutable", "attachment application requires an approved selected item", "attachment terminal application result is immutable",
                "attachment applied result and current binding mismatch", "attachment pause or resume requires matching action");
    }
    @Test void batchPreviewHistoryHasExactSelectionAndSourceCascadeWithoutLegacyHashConstraint() throws IOException {
        String migration=new ClassPathResource("db/migration/V73__add_attachment_worker_execution_contract.sql").getContentAsString(StandardCharsets.UTF_8);
        assertThat(migration).contains("CREATE TABLE announcement_attachment_batch_previews", "CREATE TABLE announcement_attachment_batch_preview_items",
                "FOREIGN KEY(job_id,batch_id) REFERENCES announcement_attachment_jobs(id,batch_id) ON DELETE CASCADE",
                "FOREIGN KEY(current_preview_id,id)", "attachment batch preview history is immutable", "attachment preview items cannot be appended to old history",
                "ct_att_batch_preview_complete", "ct_att_batch_current_selection", "ct_att_job_current_selection",
                "is_eligible=(readiness_code='READY')", "NOT is_selected OR is_eligible", "scope_item_count=remaining_item_count+deleted_item_count")
                .doesNotContain("FOREIGN KEY(id,preview_hash) REFERENCES announcement_attachment_batch_previews");
    }

    @Test void batchCollectionRequiresApprovalAndFrozenLocatorAndReviewBindings() throws IOException {
        String migration=new ClassPathResource("db/migration/V73__add_attachment_worker_execution_contract.sql").getContentAsString(StandardCharsets.UTF_8);
        assertThat(migration).contains("ck_att_batch_collection_approval", "collection_started_at IS NOT NULL AND collection_approval_hash IS NOT NULL",
                "OLD.collection_started_at IS NOT NULL", "NEW.approved_by,NEW.collection_started_at,NEW.collection_approval_hash",
                "CREATE FUNCTION attachment_source_locator_hash", "CREATE FUNCTION attachment_batch_job_input_unchanged",
                "j.frozen_locator_hash=attachment_source_locator_hash(j.source_id)", "NEW.frozen_locator_hash", "OLD.frozen_locator_hash",
                "attachment batch previous bindings are immutable", "batch_state NOT IN ('SCOPE_READY','CANCELLED') AND job_status_code='SCOPE_READY'");
    }

    @Test void backfillScopeMaterializesJobsPreservesHistoryAndCountsDeletedSources() throws IOException {
        String migration=new ClassPathResource("db/migration/V73__add_attachment_worker_execution_contract.sql").getContentAsString(StandardCharsets.UTF_8);
        assertThat(migration).contains("ADD COLUMN scope_item_count integer", "scope_item_count<=maximum_count", "fk_att_job_batch_policy",
                "REFERENCES announcement_attachment_batches(id,policy_id)", "managed attachment batch history cannot be deleted",
                "NEW.scope_fixed_at,NEW.reason_hash,NEW.idempotency_key,NEW.request_hash,NEW.created_at,NEW.scope_item_count,NEW.policy_snapshot_json",
                "deleted_item_count=deleted_item_count+1", "expected<>(SELECT count(1) FROM announcement_attachment_jobs WHERE batch_id=batch_key)+deleted",
                "batch_state='SCOPE_READY' AND job_status_code<>'SCOPE_READY'", "batch_state='CANCELLED' AND job_status_code<>'CANCELLED'",
                "CREATE CONSTRAINT TRIGGER ct_att_batch_scope_count", "CREATE CONSTRAINT TRIGGER ct_att_batch_job_count",
                "NEW.frozen_provider_code", "OLD.frozen_provider_code");
    }

    @Test void policyQaHistoryPinsInputsAndFencesLifecycleAndPartialEvidence() throws IOException {
        String migration=new ClassPathResource("db/migration/V73__add_attachment_worker_execution_contract.sql").getContentAsString(StandardCharsets.UTF_8);
        assertThat(migration).contains("CREATE TABLE announcement_attachment_policy_validation_runs",
                "CREATE TABLE announcement_attachment_policy_validation_steps", "uq_att_validation_active",
                "'CLASSIFICATION_GOLDEN','INSTALLED_RUNTIME','PROVIDER_PROFILES','WORKER_DB_RECOVERY'",
                "NEW.input_snapshot_json,NEW.requested_by,NEW.idempotency_key,NEW.request_hash,NEW.created_at",
                "BEFORE INSERT OR UPDATE OR DELETE ON announcement_attachment_policy_validation_runs",
                "BEFORE INSERT OR UPDATE OR DELETE ON announcement_attachment_policy_validation_steps",
                "NEW.started_at IS DISTINCT FROM OLD.started_at", "NEW.lease_token,NEW.lease_expires_at",
                "NEW.error_code IS NOT DISTINCT FROM 'LEASE_EXPIRED'", "WHERE run_id=NEW.id AND status_code='PASSED')<>4",
                "run_status_code='RUNNING' AND lease_expires_at>clock_timestamp() FOR UPDATE",
                "validation evidence requires its owned extraction slot");
    }
    @Test void policyQaAndWorkerResourceOwnersAreMutuallyExclusive() throws IOException {
        String migration=new ClassPathResource("db/migration/V73__add_attachment_worker_execution_contract.sql").getContentAsString(StandardCharsets.UTF_8);
        assertThat(migration).contains("ck_att_resource_owner", "job_id IS NOT NULL AND job_lease_token IS NOT NULL AND policy_validation_id IS NULL",
                "job_id IS NULL AND job_lease_token IS NULL AND policy_validation_id IS NOT NULL AND policy_validation_lease_token IS NOT NULL",
                "resource_code='EXTRACTION' AND resource_key='GLOBAL' AND slot_no=1",
                "l.policy_validation_id=v.id AND l.policy_validation_lease_token=v.lease_token");
    }
    @Test void policyClassificationChecksAreImmutableAndVersionBoundWithoutPublicationState() throws IOException {
        String migration=new ClassPathResource("db/migration/V73__add_attachment_worker_execution_contract.sql").getContentAsString(StandardCharsets.UTF_8);
        assertThat(migration).contains("CREATE TABLE announcement_attachment_policy_checks","CHECK (check_type_code='CLASSIFICATION_GOLDEN')",
                "r.row_version=NEW.rule_row_version","p.row_version=NEW.policy_row_version","CREATE TRIGGER tr_att_policy_check_immutable BEFORE UPDATE",
                "CREATE INDEX ix_att_policy_check_history","CREATE INDEX ix_att_policy_check_rule","CREATE INDEX ix_att_policy_check_actor");
    }

    @Test void policyDraftIdentityAndCreationRequestsAreAdditiveAndImmutable() throws IOException {
        String migration=new ClassPathResource("db/migration/V73__add_attachment_worker_execution_contract.sql").getContentAsString(StandardCharsets.UTF_8);
        assertThat(migration).contains("ADD COLUMN creation_idempotency_key uuid UNIQUE","ADD CONSTRAINT ck_att_policy_creation",
                "creation_operation_code='CREATE' AND copied_from_policy_id IS NULL",
                "creation_operation_code='REVISION' AND copied_from_policy_id IS NOT NULL AND copied_from_policy_id<>id",
                "CREATE INDEX ix_att_policy_parent","CREATE INDEX ix_att_policy_list",
                "CREATE TRIGGER tr_att_policy_revision_parent BEFORE INSERT ON announcement_attachment_policies",
                "parent.id=NEW.copied_from_policy_id AND parent.policy_code=NEW.policy_code",
                "parent.version_no<NEW.version_no",
                "CREATE TRIGGER tr_att_policy_draft_identity BEFORE UPDATE ON announcement_attachment_policies",
                "NEW.creation_idempotency_key,NEW.creation_request_hash,NEW.creation_operation_code,NEW.copied_from_policy_id",
                "NEW.row_version<>OLD.row_version+1","managed attachment policy version must advance once");
        assertThat(migration).doesNotContain("DROP TRIGGER tr_att_policy_immutable","INSERT INTO announcement_attachment_policies");
    }

    @Test void attachmentHistoryHasStableSourceScopedPaginationIndex() throws IOException {
        String migration=new ClassPathResource("db/migration/V73__add_attachment_worker_execution_contract.sql").getContentAsString(StandardCharsets.UTF_8);
        assertThat(migration).contains("CREATE INDEX ix_att_eval_source_history ON announcement_source_attachment_evaluations(source_id,evaluated_at DESC,id DESC)");
    }
    @Test void manualNetworkRequestsHaveOneSharedWindowIndex() throws IOException {
        String migration=new ClassPathResource("db/migration/V73__add_attachment_worker_execution_contract.sql").getContentAsString(StandardCharsets.UTF_8);
        assertThat(migration).contains("CREATE INDEX ix_att_job_manual_network_window ON announcement_attachment_jobs(source_id,created_at DESC)",
                "WHERE requested_by IS NOT NULL AND operation_code IN ('COLLECT','RETRY_FILES')");
    }

    @Test void manualRetryScopeUsesImmutableSourceAndFileBindings() throws IOException {
        String migration=new ClassPathResource("db/migration/V73__add_attachment_worker_execution_contract.sql").getContentAsString(StandardCharsets.UTF_8);
        assertThat(migration).contains("fk_att_retry_job FOREIGN KEY (job_id,source_id,reference_set_id)",
                "fk_att_retry_file FOREIGN KEY (file_id,reference_set_id,source_id)","tr_att_retry_scope_present",
                "tr_att_retry_scope_immutable","attachment retry scope must select failed files only","attachment retry sealed set is immutable");
        String mapper=new ClassPathResource("mapper/announcementattachment/AnnouncementAttachmentRetryMapper.xml").getContentAsString(StandardCharsets.UTF_8);
        assertThat(mapper).contains("interval '60 seconds'","interval '24 hours'","j.lease_token=#{leaseToken}","f.set_id=j.reference_set_id");
    }

    @Test void retryCheckpointIsBoundedJobScopedAndNeverAVisibleAttachmentSet() throws IOException {
        String migration=new ClassPathResource("db/migration/V73__add_attachment_worker_execution_contract.sql").getContentAsString(StandardCharsets.UTF_8);
        assertThat(migration).contains("fk_att_checkpoint_job FOREIGN KEY (job_id,source_id)","octet_length(file_result_json::text)<=16777216",
                "tr_att_checkpoint_immutable","tr_att_checkpoint_finished","attachment checkpoint file limit exceeded");
        String mapper=new ClassPathResource("mapper/announcementattachment/AnnouncementAttachmentEvidenceMapper.xml").getContentAsString(StandardCharsets.UTF_8);
        assertThat(mapper).contains("j.lease_token=#{leaseToken}","j.operation_code IN ('COLLECT','RETRY_FILES') AND j.set_id IS NULL",
                "e.id=j.base_evaluation_id","e.is_current","completedAt,javaType=java.time.OffsetDateTime,jdbcType=TIMESTAMP_WITH_TIMEZONE");
        assertThat(mapper.substring(mapper.indexOf("<select id=\"selectSetList\""),mapper.indexOf("<select id=\"selectFileList\"")))
                .doesNotContain("file_checkpoints");
    }

    @Test void attachmentCurrentProjectionUsesFlywayJobSetColumn() throws IOException {
        String migration=new ClassPathResource("db/migration/V72__add_announcement_attachment_evidence.sql").getContentAsString(StandardCharsets.UTF_8);
        String mapper=new ClassPathResource("mapper/announcementattachment/AnnouncementAttachmentCurrentMapper.xml").getContentAsString(StandardCharsets.UTF_8);
        assertThat(migration).contains("CONSTRAINT fk_att_job_set FOREIGN KEY (set_id,source_id,content_version_id,policy_id)");
        assertThat(mapper).contains("q.set_id", "jset.id=j.set_id").doesNotContain("last_set_id");
    }
    @Test void roleChangesPreserveSameSourceExtractionAndDisallowExternalExecution() throws IOException {
        String migration=new ClassPathResource("db/migration/V73__add_attachment_worker_execution_contract.sql").getContentAsString(StandardCharsets.UTF_8);
        assertThat(migration).contains("fk_att_job_reference_set", "reserved_download_bytes=0", "fk_att_extraction_reused",
                "tr_att_extraction_reuse", "tr_att_role_job_sets", "attachment role job sealed set is immutable");
        String mapper=new ClassPathResource("mapper/announcementattachment/AnnouncementAttachmentJobMapper.xml").getContentAsString(StandardCharsets.UTF_8);
        assertThat(mapper).contains("j.operation_code IN ('COLLECT','RETRY_FILES') AND EXISTS");
        String copies=new ClassPathResource("mapper/announcementattachment/AnnouncementAttachmentRoleMapper.xml").getContentAsString(StandardCharsets.UTF_8);
        assertThat(copies).contains("original.created_at,original.id", "original.source_id=#{sourceId}").doesNotContain("UPDATE announcement_source_attachment_files");
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @throws IOException 처리 중 예외가 발생한 경우
     */
    @Test
    void v1MigrationContainsMvpTables() throws IOException {
        String sql = selectV1Migration();

        assertThat(sql).contains(
                "CREATE TABLE roles",
                "CREATE TABLE users",
                "CREATE TABLE user_roles",
                "CREATE TABLE auth_login_histories",
                "CREATE TABLE member_profiles",
                "CREATE TABLE business_profiles",
                "CREATE TABLE family_members",
                "CREATE TABLE partner_profiles",
                "CREATE TABLE partner_verifications",
                "CREATE TABLE verification_documents",
                "CREATE TABLE announcements",
                "CREATE TABLE announcement_numeric_conditions",
                "CREATE TABLE matching_cases",
                "CREATE TABLE matching_result_details",
                "CREATE TABLE announcement_progress_steps",
                "CREATE TABLE application_progresses",
                "CREATE TABLE application_action_logs",
                "CREATE TABLE audit_logs"
        );
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @throws IOException 처리 중 예외가 발생한 경우
     */
    @Test
    void v1MigrationDoesNotContainExcludedMatchingMetrics() throws IOException {
        String sql = selectV1Migration().toLowerCase();

        assertThat(sql).doesNotContain(
                "recommendation_score",
                "priority_score",
                "selection_probability",
                "bonus_score",
                "ai_decision"
        );
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @throws IOException 처리 중 예외가 발생한 경우
     */
    @Test
    void v4MigrationContainsDynamicAnnouncementInputTables() throws IOException {
        String sql = selectV4Migration();

        assertThat(sql).contains(
                "CREATE TABLE announcement_input_requirements",
                "CREATE TABLE announcement_input_options",
                "CREATE TABLE application_input_values",
                "CONSTRAINT uq_announcement_input_requirements_field_key",
                "CONSTRAINT uq_announcement_input_options_code",
                "CREATE UNIQUE INDEX uq_application_input_values_single_value",
                "CREATE UNIQUE INDEX uq_application_input_values_option_value"
        );
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @throws IOException 처리 중 예외가 발생한 경우
     */
    @Test
    void v6MigrationAllowsMatchingWithoutVerification() throws IOException {
        String sql = selectV6Migration();

        assertThat(sql).contains(
                "ALTER COLUMN verification_id DROP NOT NULL",
                "CREATE UNIQUE INDEX uq_matching_cases_without_verification"
        );
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @throws IOException 처리 중 예외가 발생한 경우
     */
    @Test
    void v7MigrationContainsUserConsentTables() throws IOException {
        String sql = selectV7Migration();

        assertThat(sql).contains(
                "CREATE TABLE consent_versions",
                "CREATE TABLE user_consents",
                "CREATE UNIQUE INDEX uq_consent_versions_current",
                "CREATE INDEX ix_user_consents_user_code_consented_at",
                "'TERMS_OF_SERVICE'",
                "'PRIVACY_POLICY'",
                "'E_CERT'",
                "'CREDIT_CHECK'"
        );
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @throws IOException 처리 중 예외가 발생한 경우
     */
    @Test
    void v8MigrationContainsDocumentFileSubmissionTables() throws IOException {
        String sql = selectV8Migration();

        assertThat(sql).contains(
                "CREATE TABLE stored_files",
                "CREATE TABLE document_submissions",
                "CREATE TABLE document_submission_reviews",
                "CONSTRAINT uq_stored_files_storage_key",
                "CREATE INDEX ix_document_submissions_resource",
                "PARTNER_VERIFICATION",
                "APPLICATION_PROGRESS"
        );
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @throws IOException 처리 중 예외가 발생한 경우
     */
    @Test
    void v9MigrationContainsConsultationReservationTables() throws IOException {
        String sql = selectV9Migration();

        assertThat(sql).contains(
                "CREATE TABLE partner_availability_slots",
                "CREATE TABLE consultation_reservations",
                "CREATE TABLE consultation_histories",
                "CREATE UNIQUE INDEX uq_consultation_reservations_active_slot",
                "'REQUESTED'",
                "'CONFIRMED'",
                "'CANCELED'"
        );
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @throws IOException 처리 중 예외가 발생한 경우
     */
    @Test
    void v10MigrationContainsSubscriptionPaymentTables() throws IOException {
        String sql = selectV10Migration();

        assertThat(sql).contains(
                "CREATE TABLE subscription_plans",
                "CREATE TABLE user_subscriptions",
                "CREATE TABLE payment_transactions",
                "CREATE TABLE refund_transactions",
                "CREATE TABLE payment_provider_events",
                "CREATE UNIQUE INDEX uq_user_subscriptions_current",
                "CREATE UNIQUE INDEX uq_payment_transactions_provider_key",
                "'PAYMENT_APPROVED'",
                "'REFUND_APPROVED'"
        );
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @throws IOException 처리 중 예외가 발생한 경우
     */
    @Test
    void v11MigrationContainsNotificationAndOperationTaskTables() throws IOException {
        String sql = selectV11Migration();

        assertThat(sql).contains(
                "CREATE TABLE notification_templates",
                "CREATE TABLE notification_messages",
                "CREATE TABLE notification_delivery_logs",
                "CREATE TABLE operation_tasks",
                "CREATE TABLE operation_task_comments",
                "CREATE TABLE operation_task_assignments",
                "'SUPPLEMENT_REQUEST'",
                "'CONSULTATION_PENDING'",
                "'IN_APP'",
                "'KAKAO'"
        );
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @throws IOException 처리 중 예외가 발생한 경우
     */
    @Test
    void v12MigrationContainsAdminReportExportTables() throws IOException {
        String sql = selectV12Migration();

        assertThat(sql).contains(
                "CREATE TABLE report_exports",
                "CREATE TABLE admin_report_snapshots",
                "'OPERATION_SUMMARY'",
                "'CSV'",
                "'EXCEL'",
                "'COMPLETED'"
        );
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @throws IOException 처리 중 예외가 발생한 경우
     */
    @Test
    void v14MigrationContainsReviewerAndManualConsultationChanges() throws IOException {
        String sql = selectV14Migration();

        assertThat(sql).contains(
                "'REVIEWER'",
                "'검수자'",
                "ALTER COLUMN slot_id DROP NOT NULL",
                "ALTER COLUMN partner_user_id DROP NOT NULL",
                "'ASSIGNED'",
                "CREATE UNIQUE INDEX uq_progress_reminder_logs_progress_type",
                "CREATE INDEX ix_operation_tasks_open_resource_type"
        );
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @throws IOException 처리 중 예외가 발생한 경우
     */
    @Test
    void v15MigrationContainsStandardDocumentFieldsAndBasicIncomeAdditions() throws IOException {
        String sql = selectV15Migration();

        assertThat(sql).contains(
                "CREATE TABLE standard_document_fields",
                "CONSTRAINT uq_standard_document_fields_key",
                "ADD COLUMN standard_field_id uuid",
                "ADD COLUMN income_presence_code varchar(30)",
                "ADD COLUMN annual_revenue numeric(18, 2)",
                "CREATE UNIQUE INDEX uq_matching_cases_no_verification",
                "'BUSINESS_REGISTRATION'",
                "'HEALTH_INSURANCE_QUALIFICATION'"
        );
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @throws IOException 처리 중 예외가 발생한 경우
     */
    @Test
    void v16MigrationContainsMemberDocumentInputValues() throws IOException {
        String sql = selectV16Migration();

        assertThat(sql).contains(
                "CREATE TABLE member_document_input_values",
                "CONSTRAINT uq_member_document_input_values_field",
                "CONSTRAINT ck_member_document_input_values_single_value",
                "WORKPLACE_ADDRESS",
                "INSURED_PERSON_INFO"
        );
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @throws IOException 처리 중 예외가 발생한 경우
     */
    @Test
    void v17MigrationContainsMockMonthlySubscriptionPlan() throws IOException {
        String sql = selectV17Migration();

        assertThat(sql).contains(
                "INSERT INTO subscription_plans",
                "SANEB_MONTHLY_MOCK",
                "사내비 월 구독",
                "MONTHLY",
                "12900.00",
                "ON CONFLICT (plan_code) DO UPDATE"
        );
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @throws IOException 처리 중 예외가 발생한 경우
     */
    @Test
    void v19MigrationContainsMatchingStageFlow() throws IOException {
        String sql = selectV19Migration();

        assertThat(sql).contains(
                "ADD COLUMN matching_stage_code",
                "ADD COLUMN matching_basis_code",
                "CREATE UNIQUE INDEX uq_matching_cases_stage_no_verification",
                "'BASIC'",
                "'FINAL'",
                "'BASIC_INFO'",
                "'DOCUMENT_INPUT'"
        );
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @throws IOException 처리 중 예외가 발생한 경우
     */
    @Test
    void v21MigrationContainsStandardCodeCatalogs() throws IOException {
        String sql = selectV21Migration();

        assertThat(sql).contains(
                "ADD COLUMN condition_usage_code",
                "'INPUT_ONLY'",
                "'CONDITION_READY'",
                "'STANDARDIZATION_REQUIRED'",
                "CREATE TABLE standard_code_groups",
                "CREATE TABLE standard_codes",
                "CREATE TABLE standard_field_code_groups",
                "CONSTRAINT uq_standard_code_groups_code",
                "CONSTRAINT uq_standard_codes_group_code",
                "CONSTRAINT ck_standard_field_code_groups_usage",
                "'KSIC_11'",
                "'REGION_SIDO'",
                "'HEALTH_INSURANCE_TYPE'",
                "'REFERENCE_MAPPING'"
        );
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @throws IOException 처리 중 예외가 발생한 경우
     */
    @Test
    void v22MigrationContainsStructuredAddressFields() throws IOException {
        String sql = selectV22Migration();

        assertThat(sql).contains(
                "ADD COLUMN postal_code varchar(20)",
                "ADD COLUMN road_address varchar(500)",
                "ADD COLUMN legal_dong_code varchar(30)",
                "ADD COLUMN workplace_postal_code varchar(20)",
                "ADD COLUMN workplace_road_address varchar(500)",
                "ADD COLUMN workplace_legal_dong_code varchar(30)",
                "ck_member_profiles_address_source",
                "ck_business_profiles_workplace_address_source",
                "ix_member_profiles_legal_dong_code",
                "ix_business_profiles_workplace_legal_dong_code"
        );
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @throws IOException 처리 중 예외가 발생한 경우
     */
    @Test
    void v25MigrationContainsMemberInterviewResponses() throws IOException {
        String sql = selectV25Migration();

        assertThat(sql).contains(
                "ADD COLUMN has_existing_loan boolean",
                "CREATE TABLE member_interview_responses",
                "CONSTRAINT uq_member_interview_responses_question",
                "CONSTRAINT ck_member_interview_responses_question",
                "SAME_BUSINESS_IN_PROGRESS",
                "DUPLICATE_SUPPORT_USAGE",
                "BUSINESS_ACTUALLY_OPERATING",
                "OTHER_RESTRICTION",
                "CONSTRAINT ck_member_interview_responses_answer"
        );
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @throws IOException 처리 중 예외가 발생한 경우
     */
    @Test
    void v26MigrationContainsAnnouncementSourceCollectionTables() throws IOException {
        String sql = selectV26Migration();

        assertThat(sql).contains(
                "CREATE TABLE announcement_source_collection_requests",
                "CREATE TABLE announcement_source_collection_runs",
                "CREATE TABLE announcement_source_collection_run_items",
                "CREATE TABLE announcement_source_snapshots",
                "CREATE TABLE announcement_source_attachments",
                "CREATE TABLE announcement_source_highlights",
                "CREATE TABLE announcement_source_review_histories",
                "CREATE TABLE announcement_source_links",
                "'APPROVAL_PENDING'",
                "'BATCH'",
                "'MANUAL'",
                "'REVIEW_PENDING'",
                "'ACTIVATED'",
                "'SKIPPED_ENDED'"
        );
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @throws IOException 처리 중 예외가 발생한 경우
     */
    @Test
    void v27MigrationContainsAnnouncementSourceDuplicateCandidates() throws IOException {
        String sql = selectV27Migration();

        assertThat(sql).contains(
                "CREATE TABLE announcement_source_duplicate_candidates",
                "CONSTRAINT uq_announcement_source_duplicate_candidates_source_announcement",
                "EXACT_DUPLICATE",
                "SIMILAR",
                "CREATE_NEW_SELECTED",
                "UPDATE_EXISTING_SELECTED",
                "IGNORED"
        );
    }

    /**
     * 지자체 URL 관리, URL별 결과, 교차 중복과 승인 스케줄 스키마를 확인합니다.
     *
     * @throws IOException migration 읽기 오류
     */
    @Test
    void v28MigrationContainsLocalGovernmentNoticeCollectionContracts() throws IOException {
        String sql = selectV28Migration();

        assertThat(sql).contains(
                "CREATE TABLE local_government_notice_sources",
                "CREATE TABLE local_government_notice_parser_profiles",
                "CREATE TABLE announcement_source_collection_source_results",
                "CREATE TABLE announcement_source_snapshot_duplicates",
                "CREATE TABLE announcement_source_collection_schedules",
                "CREATE TABLE announcement_source_schedule_executions",
                "LOCAL_GOV_NOTICE",
                "ck_announcement_source_snapshot_duplicates_order",
                "uq_announcement_source_schedule_executions_slot"
        );
    }

    /**
     * 지자체 URL 정적 seed의 수량·보정·기본 OFF 정책을 확인합니다.
     *
     * @throws IOException migration 읽기 오류
     */
    @Test
    void v29MigrationContainsReviewedLocalGovernmentNoticeSeed() throws IOException {
        String sql = selectV29Migration();

        assertThat(sql).contains(
                "Expected 244 local-government notice sources",
                "Expected 244 unique local-government district codes",
                "https://www.dalseong.daegu.kr/index.do?menu_id=00000194",
                "https://seohae.go.kr/open_content/main/community/news/gosi.jsp",
                "https://www.osan.go.kr/portal/saeol/gosi/list.do?mId=0302010000",
                "https://www.dh.go.kr/www/selectBbsNttList.do?bbsNo=87&key=478",
                "https://www.sangju.go.kr/page/10297/10606.tc",
                "'MANUAL_ONLY'",
                "false"
        );
    }

    /**
     * 제한형 휴리스틱 파서 프로필 추가 계약을 확인합니다.
     *
     * @throws IOException migration 읽기 오류
     */
    @Test
    void v30MigrationContainsHeuristicLocalGovernmentParser() throws IOException {
        String sql = selectV30Migration();

        assertThat(sql).contains(
                "HEURISTIC_NOTICE",
                "제한형 공고 링크 탐색",
                "ck_local_government_notice_parser_profiles_type"
        );
    }

    /**
     * 전수 QA 통과·보류·실패 결과 반영 계약을 확인합니다.
     *
     * @throws IOException migration 읽기 오류
     */
    @Test
    void v31MigrationContainsFullParserQaResult() throws IOException {
        String sql = selectV31Migration();

        assertThat(sql).contains(
                "WITH qa_pass",
                "('LGS-000002', 'HEURISTIC_NOTICE')",
                "('LGS-000244', 'SPRING_BBS')",
                "validation_status_code = 'CHECK_REQUIRED'",
                "validation_status_code = 'FAILED'",
                "is_enabled = false"
        );
    }

    /**
     * 검증 URL 보정과 기관별 HTTP 요청 프로필 계약을 확인합니다.
     *
     * @throws IOException migration 읽기 오류
     */
    @Test
    void v32MigrationContainsReviewedUrlsAndRequestProfiles() throws IOException {
        String sql = selectV32Migration();

        assertThat(sql).contains(
                "ADD COLUMN request_profile_code",
                "BROWSER_HTTP1",
                "('LGS-000011', 'https://www.dobong.go.kr/bbs.asp?code=10008769')",
                "('LGS-000233', 'https://eminwon.haman.go.kr/emwp/jsp/ofr/OfrNotAncmtLSub.jsp?not_ancmt_se_code=01,04')",
                "validation_status_code = 'CHECK_REQUIRED'",
                "is_enabled = false"
        );
    }

    /**
     * 파서 보강 후 추가 전수 QA 통과 결과 계약을 확인합니다.
     *
     * @throws IOException migration 읽기 오류
     */
    @Test
    void v33MigrationContainsHardenedParserQaResult() throws IOException {
        String sql = selectV33Migration();

        assertThat(sql).contains(
                "WITH qa_pass",
                "('LGS-000021', 'SPRING_BBS')",
                "('LGS-000066', 'HEURISTIC_NOTICE')",
                "('LGS-000117', 'CHUNCHEON_NOTICE_JSON')",
                "('LGS-000223', 'SPRING_BBS')",
                "('LGS-000239', 'HEURISTIC_NOTICE')",
                "validation_status_code = 'VERIFIED'"
        );
    }

    /**
     * 안전한 상세 링크 템플릿과 공통 플랫폼 프로필 계약을 확인합니다.
     *
     * @throws IOException migration 읽기 오류
     */
    @Test
    void v34MigrationContainsSafeLinkTemplateProfiles() throws IOException {
        String sql = selectV34Migration();

        assertThat(sql).contains(
                "ADD COLUMN link_strategy_code",
                "SAFE_TEMPLATE",
                "SAFE_BOARD_VIEW",
                "SAFE_BOARD_VIEW_SITE",
                "SAFE_YH_BOARD_POST",
                "SAFE_ICMS_BOARD",
                "SAFE_OPENWORKS_BOARD",
                "SAFE_BD_SELECT_BBS",
                "SAFE_GOTO_VIEW",
                "SAFE_ICMS_BOARD_EXTENDED",
                "SAFE_ANSAN_BBS",
                "SAFE_GWD_BULLETIN",
                "SAFE_SANGJU_GOSI",
                "SAFE_GORYEONG_BOARD",
                "{arg:6}",
                "{attr:data-req-get-p-idx}",
                "{input:bbsId}"
        );
    }

    /**
     * 안전 링크 상세 URL 검증을 통과한 출처만 승격하는 계약을 확인합니다.
     *
     * @throws IOException migration 읽기 오류
     */
    @Test
    void v35MigrationContainsSafeLinkTemplateQaResult() throws IOException {
        String sql = selectV35Migration();

        assertThat(sql).contains(
                "WITH qa_pass",
                "('LGS-000016', 'SAFE_YANGCHEON_SEOL')",
                "('LGS-000037', 'SAFE_BOARD_VIEW')",
                "('LGS-000049', 'SAFE_ICMS_BOARD_EXTENDED')",
                "('LGS-000082', 'SAFE_GOTO_VIEW')",
                "('LGS-000092', 'SAFE_ANSAN_BBS')",
                "('LGS-000098', 'SAFE_BOARD_VIEW_SITE')",
                "('LGS-000116', 'SAFE_GWD_BULLETIN')",
                "('LGS-000203', 'SAFE_GOTO_VIEW_EXTENDED')",
                "('LGS-000205', 'SAFE_YH_BOARD_POST')",
                "('LGS-000208', 'SAFE_SANGJU_GOSI')",
                "('LGS-000216', 'SAFE_GORYEONG_BOARD')",
                "('LGS-000231', 'SAFE_YH_BOARD_POST')",
                "validation_status_code = 'VERIFIED'",
                "is_enabled = false"
        );
    }

    /**
     * 공식 지자체 URL 보정이 자동 활성화 없이 적용되는지 확인합니다.
     *
     * @throws IOException migration 읽기 오류
     */
    @Test
    void v36MigrationContainsReviewedOfficialUrls() throws IOException {
        String sql = selectV36Migration();

        assertThat(sql).contains(
                "WITH reviewed_url (public_code, notice_url)",
                "('LGS-000014', 'https://www.sdm.go.kr/news/notice.do')",
                "parser_profile_code = 'MANUAL_ONLY'",
                "validation_status_code = 'CHECK_REQUIRED'",
                "is_enabled = false"
        );
    }

    /**
     * 반복되는 공공 게시판 구조의 공통 프로필 계약을 확인합니다.
     *
     * @throws IOException migration 읽기 오류
     */
    @Test
    void v37MigrationContainsCommonParserProfiles() throws IOException {
        String sql = selectV37Migration();

        assertThat(sql).contains(
                "SAFE_SEODAEMUN_NOTICE",
                "/news/notice/notice.do?mode=view&sdmBoardSeq={arg:1}",
                "SAFE_SAEOL_EMINWON",
                "SUBJECT_NOTICE_TABLE",
                "SCMS_CARD_NOTICE",
                "is_enabled = false"
        );
    }

    /**
     * JSON, 대전 통합, 셀 클릭형 등 잔여 공통 구조 지원 계약을 확인합니다.
     *
     * @throws IOException migration 읽기 오류
     */
    @Test
    void v38MigrationContainsRemainingParserProfiles() throws IOException {
        String sql = selectV38Migration();

        assertThat(sql).contains(
                "DAEJEON_EMINWON",
                "SAFE_SAEOL_EMINWON_CELL",
                "table tr:has(td:nth-of-type(3)[onclick*=searchDetail])",
                "DAMYANG_NOTICE_JSON",
                "('LGS-000071', 'DAEJEON_EMINWON_AGGREGATOR')",
                "('LGS-000183', 'DAMYANG_NOTICE_JSON')",
                "is_enabled = false"
        );
    }

    /**
     * 공개 게시판 폼 POST와 최종 QA 승격 계약을 확인합니다.
     *
     * @throws IOException migration 읽기 오류
     */
    @Test
    void v39MigrationContainsPostFormAndFinalQaResult() throws IOException {
        String sql = selectV39Migration();

        assertThat(sql).contains(
                "ADD COLUMN request_method_code",
                "ADD COLUMN request_form_json",
                "request_method_code = 'POST_FORM'",
                "selectListOfrNotAncmtHomepage",
                "('LGS-000089', 'SAFE_SAEOL_EMINWON')",
                "('LGS-000145', 'SAFE_SAEOL_EMINWON_CELL')",
                "validation_status_code = 'VERIFIED'",
                "is_enabled = false"
        );
    }

    /**
     * 244개 전수 QA의 통과 및 부분 통과 상태 동기화 계약을 확인합니다.
     *
     * @throws IOException migration 읽기 오류
     */
    @Test
    void v40MigrationContainsComprehensiveQaResult() throws IOException {
        String sql = selectV40Migration();

        assertThat(sql).contains(
                "WITH qa_pass(public_code, parser_profile_code)",
                "('LGS-000014', 'SAFE_SEODAEMUN_NOTICE')",
                "('LGS-000071', 'DAEJEON_EMINWON_AGGREGATOR')",
                "('LGS-000089', 'SAFE_SAEOL_EMINWON')",
                "('LGS-000183', 'DAMYANG_NOTICE_JSON')",
                "('LGS-000244', 'SPRING_BBS')",
                "WITH qa_partial(public_code, parser_profile_code)",
                "validation_status_code = 'CHECK_REQUIRED'",
                "is_enabled = false"
        );
        assertThat(sql.split("\\('LGS-", -1).length - 1).isEqualTo(229);
    }

    /**
     * 추가 공식 URL 보정이 비활성 검증대기 상태를 유지하는지 확인합니다.
     *
     * @throws IOException migration 읽기 오류
     */
    @Test
    void v41MigrationContainsAdditionalOfficialUrlCorrections() throws IOException {
        String sql = selectV41Migration();

        assertThat(sql).contains(
                "('LGS-000045', 'https://eminwon.jung.daegu.kr/",
                "('LGS-000130', 'https://eminwon.ihc.go.kr/",
                "('LGS-000141', 'https://www.yd21.go.kr/kr/html/sub02/020103.html?GotoPage=1&mode=L')",
                "('LGS-000230', 'https://www.geoje.go.kr/index.geoje?menuCd=DOM_000008902001002001')",
                "validation_status_code = 'CHECK_REQUIRED'",
                "is_enabled = false"
        );
    }

    /**
     * 좁은 공통 파서 프로필이 상세 링크 서명을 기준으로 등록되는지 확인합니다.
     *
     * @throws IOException migration 읽기 오류
     */
    @Test
    void v42MigrationContainsNarrowReusableParserProfiles() throws IOException {
        String sql = selectV42Migration();

        assertThat(sql).contains(
                "SAFE_SAEOL_EMINWON_LEGACY",
                "SAFE_SAEOL_EMINWON_HREF",
                "SAFE_EGOV_DETAIL_BUTTON",
                "RFC_BLOGLIST_NOTICE",
                "GURYE_BOARD_NOTICE",
                "tr:has(td:nth-of-type(3) a[onclick*=searchDetail])",
                "tr:has(td.subject button[onclick*=fn_search_detail])",
                "is_enabled"
        );
        assertThat(sql.split("2a57f03e-2b48-4c3f-88cc-cc7bc1e142", -1).length - 1).isEqualTo(5);
    }

    /**
     * 격리 QA로 확인한 8개 수집원만 검증 완료로 승격하는지 확인합니다.
     *
     * @throws IOException migration 읽기 오류
     */
    @Test
    void v43MigrationContainsNarrowParserQaResults() throws IOException {
        String sql = selectV43Migration();

        assertThat(sql).contains(
                "('LGS-000029', 'RFC_BLOGLIST_NOTICE')",
                "('LGS-000045', 'SAFE_SAEOL_EMINWON_LEGACY')",
                "('LGS-000074', 'SAFE_EGOV_DETAIL_BUTTON')",
                "('LGS-000130', 'SAFE_SAEOL_EMINWON_LEGACY')",
                "('LGS-000141', 'SAEOL_GOSI')",
                "('LGS-000161', 'SAFE_EGOV_DETAIL_BUTTON')",
                "('LGS-000185', 'GURYE_BOARD_NOTICE')",
                "('LGS-000230', 'SAEOL_GOSI')",
                "validation_status_code = 'VERIFIED'",
                "is_enabled = false"
        );
        assertThat(sql.split("\\('LGS-", -1).length - 1).isEqualTo(8);
    }

    /**
     * 공주형 전자정부 게시판 프로필 계약을 검증합니다.
     *
     * @throws IOException migration 읽기 오류
     */
    @Test
    void v44MigrationContainsSafeEgovDetailCellParserProfile() throws IOException {
        String sql = selectV44Migration();

        assertThat(sql).contains(
                "SAFE_EGOV_DETAIL_CELL",
                "table.table-default tbody tr:has(td.subject a[onclick*=fn_search_detail])",
                "td[data-cell-header=\"등록일\"]",
                "view.do?notAncmtMgtNo={arg:1}",
                "SAFE_TEMPLATE"
        );
    }

    /**
     * 공주시청의 실사이트 QA 결과 반영 계약을 검증합니다.
     *
     * @throws IOException migration 읽기 오류
     */
    @Test
    void v45MigrationAppliesGongjuQaResultWithoutEnablingCollection() throws IOException {
        String sql = selectV45Migration();

        assertThat(sql).contains(
                "public_code = 'LGS-000149'",
                "parser_profile_code = 'SAFE_EGOV_DETAIL_CELL'",
                "validation_status_code = 'VERIFIED'",
                "collection_status_code = 'READY'",
                "is_enabled = false"
        );
    }

    /**
     * 구형 공공사이트 호환 요청 프로필과 적용 대상을 검증합니다.
     *
     * @throws IOException migration 읽기 오류
     */
    @Test
    void v46MigrationAddsLegacyBrowserRequestProfile() throws IOException {
        String sql = selectV46Migration();

        assertThat(sql).contains(
                "'DEFAULT', 'BROWSER_HTTP1', 'LEGACY_BROWSER'",
                "request_profile_code = 'LEGACY_BROWSER'",
                "'LGS-000008'",
                "'LGS-000093'",
                "'LGS-000148'",
                "'LGS-000158'",
                "validation_status_code = 'CHECK_REQUIRED'",
                "is_enabled = false"
        );
    }

    /**
     * 구형 게시판별 좁은 파서와 안전한 상세 URL 계약을 검증합니다.
     *
     * @throws IOException migration 읽기 오류
     */
    @Test
    void v47MigrationAddsLegacyBoardParserProfiles() throws IOException {
        String sql = selectV47Migration();

        assertThat(sql).contains(
                "JUNGNANG_CONTEST_BOARD",
                "SAFE_PYEONGTAEK_BOARD_RENEWAL",
                "SAFE_EGOV_BOARD_BUTTON",
                "SAFE_EGOV_DATA_BUTTON",
                "/pyeongtaek/board/post/view.do?bcIdx={arg:4}&idx={arg:5}&mid={arg:6}",
                "view.do?nttId={attr:data-ntt-id}",
                "SAFE_TEMPLATE"
        );
    }

    /**
     * 구형 게시판 실사이트 QA 결과와 중랑구 보류 사유를 검증합니다.
     *
     * @throws IOException migration 읽기 오류
     */
    @Test
    void v48MigrationAppliesLegacyBoardQaResults() throws IOException {
        String sql = selectV48Migration();

        assertThat(sql).contains(
                "('LGS-000093', 'SAFE_PYEONGTAEK_BOARD_RENEWAL')",
                "('LGS-000148', 'SAFE_EGOV_BOARD_BUTTON')",
                "('LGS-000158', 'SAFE_EGOV_DATA_BUTTON')",
                "validation_status_code = 'VERIFIED'",
                "public_code = 'LGS-000008'",
                "last_error_code = 'STALE_SOURCE_CONTENT'",
                "is_enabled = false"
        );
    }

    /**
     * 은평·강릉·순천의 현재 공식 목록 URL과 요청 프로필을 검증합니다.
     *
     * @throws IOException migration 읽기 오류
     */
    @Test
    void v49MigrationCorrectsRemainingOfficialNoticeUrls() throws IOException {
        String sql = selectV49Migration();

        assertThat(sql).contains(
                "https://eminwon.ep.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do",
                "https://www.gn.go.kr/www/selectGosiNttList.do",
                "https://www.suncheon.go.kr/kr/news/0001/0001/?mode=list",
                "'LEGACY_BROWSER'",
                "validation_status_code = 'CHECK_REQUIRED'",
                "is_enabled = false"
        );
    }

    /**
     * 축약 열 구조의 새올 전자민원 파서 계약을 검증합니다.
     *
     * @throws IOException migration 읽기 오류
     */
    @Test
    void v50MigrationAddsCompactSaeolParserProfile() throws IOException {
        String sql = selectV50Migration();

        assertThat(sql).contains(
                "SAFE_SAEOL_EMINWON_COMPACT",
                "table.board1 tbody tr:has(td:nth-of-type(2) a[onclick*=searchDetail])",
                "td:nth-of-type(4)",
                "not_ancmt_mgt_no={arg:1}",
                "SAFE_TEMPLATE"
        );
    }

    /**
     * 공식 URL 교체 후 은평·강릉·순천의 실사이트 QA 결과를 검증합니다.
     *
     * @throws IOException migration 읽기 오류
     */
    @Test
    void v51MigrationAppliesCorrectedOfficialUrlQaResults() throws IOException {
        String sql = selectV51Migration();

        assertThat(sql).contains(
                "('LGS-000013', 'SAFE_SAEOL_EMINWON_COMPACT')",
                "('LGS-000119', 'SAEOL_GOSI')",
                "('LGS-000180', 'SUBJECT_NOTICE_TABLE')",
                "validation_status_code = 'VERIFIED'",
                "collection_status_code = 'READY'",
                "is_enabled = false"
        );
    }

    /**
     * 강동구의 느린 전자민원 응답을 명시적 호환 요청으로 분리하는 계약을 검증합니다.
     *
     * @throws IOException migration 읽기 오류
     */
    @Test
    void v52MigrationStabilizesGangdongRequestProfile() throws IOException {
        String sql = selectV52Migration();

        assertThat(sql).contains(
                "request_profile_code = 'LEGACY_BROWSER'",
                "public_code IN ('LGS-000026')",
                "validation_status_code = 'CHECK_REQUIRED'",
                "is_enabled = false"
        );
    }

    /**
     * 강동구 호환 전송 적용 후 실사이트 QA 결과를 검증합니다.
     *
     * @throws IOException migration 읽기 오류
     */
    @Test
    void v53MigrationAppliesGangdongTransportQaResult() throws IOException {
        String sql = selectV53Migration();

        assertThat(sql).contains(
                "public_code = 'LGS-000026'",
                "parser_profile_code = 'SAFE_SAEOL_EMINWON_HREF'",
                "validation_status_code = 'VERIFIED'",
                "collection_status_code = 'READY'",
                "is_enabled = false"
        );
    }

    /**
     * 잔여 3개 실사이트 QA 통과 결과가 수동 파서를 대체하는지 검증합니다.
     *
     * @throws IOException migration 읽기 오류
     */
    @Test
    void v54MigrationAppliesRemainingVerifiedParserResults() throws IOException {
        String sql = selectV54Migration();

        assertThat(sql).contains(
                "('LGS-000034', 'SAFE_SAEOL_EMINWON_LEGACY')",
                "('LGS-000108', 'SAEOL_GOSI')",
                "('LGS-000135', 'SAEOL_GOSI')",
                "validation_status_code = 'VERIFIED'",
                "collection_status_code = 'READY'",
                "is_enabled = false"
        );
    }

    /**
     * 잔여 지자체의 현재 공식 공고 목록과 수집 endpoint 복구 계약을 검증합니다.
     *
     * @throws IOException migration 읽기 오류
     */
    @Test
    void v55MigrationRecoversRemainingLocalGovernmentNoticeSources() throws IOException {
        String sql = selectV55Migration();

        assertThat(sql).contains(
                "'LGS-000008'",
                "https://www.jungnang.go.kr/portal/bbs/list/B0000117.do?menuNo=200475",
                "'SPRING_BBS'",
                "'LGS-000019'",
                "https://www.geumcheon.go.kr/portal/tblSeolGosiDetailList.do?key=294&rep=1",
                "'SAEOL_GOSI'",
                "'LGS-000036'",
                "https://www.haeundae.go.kr/board/list.do?boardId=BBS_0000038",
                "'LGS-000089'",
                "https://www.seongnam.go.kr/notice/publicNotice.do?menuIdx=1000499&returnURL=/main.do",
                "http://eminwon.seongnam.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do",
                "request_method_code = 'POST_FORM'",
                "'LGS-000094'",
                "https://www.anyang.go.kr/main/emwsWebList.do",
                "'LGS-000122'",
                "https://www.sokcho.go.kr/sc/portal/sokchonews/notification",
                "http://eminwon.sokcho.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do",
                "http://eminwon.gangdong.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do",
                "http://eminwon.bsnamgu.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do",
                "request_profile_code = 'LEGACY_BROWSER'",
                "WHERE public_code IN ('LGS-000034')",
                "'LGS-000167'",
                "https://www.jeongeup.go.kr/board/list.jeongeup?boardId=BBS_0000012",
                "'LGS-000174'",
                "https://www.imsil.go.kr/board/list.imsil?boardId=BBS_0000002",
                "WHERE public_code IN ('LGS-000036', 'LGS-000167', 'LGS-000174')",
                "'LGS-000224'",
                "https://www.changwon.go.kr/cwportal/10310/10438/10439.web?section=gosi",
                "request_profile_code = 'BROWSER_HTTP1'",
                "validation_status_code = 'VERIFIED'",
                "collection_status_code = 'READY'",
                "is_enabled = false"
        );
    }

    /**
     * 안양시·밀양시·함양군의 공식 고시·공고 URL 및 기존 공통 파서 재사용 계약을 검증합니다.
     *
     * @throws IOException migration 읽기 오류
     */
    @Test
    void v57MigrationCorrectsMiryangAndHamyangOfficialNoticeSources() throws IOException {
        String sql = selectV57Migration();

        assertThat(sql).contains(
                "'LGS-000094'",
                "https://www.anyang.go.kr/main/selectEminwonList.do",
                "parser_profile_code = 'SPRING_BBS'",
                "'LGS-000229'",
                "https://miryang.go.kr/web/eMiryangMinwonList.do",
                "parser_profile_code = 'SPRING_BBS'",
                "'LGS-000239'",
                "https://eminwon.hygn.go.kr/emwp/jsp/ofr/OfrNotAncmtLSub.jsp",
                "'LGS-000230'",
                "https://www.geoje.go.kr/index.geoje?menuCd=DOM_000008902001002001&startPage=1",
                "'LGS-000059'",
                "https://biz.namdong.go.kr/main/news/announce.jsp",
                "'LGS-000122'",
                "https://www.sokcho.go.kr/sc/portal/sokchonews/notification",
                "https://eminwon.hygn.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do",
                "parser_profile_code = 'SAFE_SAEOL_EMINWON'",
                "request_method_code = 'POST_FORM'",
                "\"not_ancmt_se_code\":\"01,02,03,04,07\"",
                "source_board_type_code = 'LEGAL_NOTICE'",
                "collection_policy_code = 'COLLECT_ALL'",
                "'TLS12_BROWSER'",
                "request_profile_code = 'TLS12_BROWSER'",
                "WHERE public_code IN ('LGS-000094')",
                "WHERE public_code IN ('LGS-000059', 'LGS-000078', 'LGS-000229', 'LGS-000239')",
                "WHERE public_code IN ('LGS-000122')",
                "is_enabled = false"
        );
    }

    /**
     * 밀양시·함양군 사용자 바로가기와 내부 수집 endpoint 분리 계약을 검증합니다.
     *
     * @throws IOException migration 읽기 오류
     */
    @Test
    void v58MigrationSeparatesOfficialNoticeLinksFromCollectionEndpoints() throws IOException {
        String sql = selectV58Migration();

        assertThat(sql).contains(
                "'LGS-000229'",
                "https://www.miryang.go.kr/web/eMiryangMinwonList.do",
                "'LGS-000239'",
                "https://www.hygn.go.kr/00429/00543/00549.web",
                "https://eminwon.hygn.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do",
                "is_enabled = false"
        );
    }

    /**
     * 새올 셀 클릭형 파서가 상위 레이아웃 행을 공고 행으로 중복 선택하지 않는지 검증합니다.
     *
     * @throws IOException migration 읽기 오류
     */
    @Test
    void v59MigrationSelectsOnlyDirectSaeolNoticeRows() throws IOException {
        String sql = selectV59Migration();

        assertThat(sql).contains(
                "profile_code = 'SAFE_SAEOL_EMINWON_CELL'",
                "tr:has(> td:nth-of-type(3)[onclick*=searchDetail])"
        );
    }

    /**
     * 남은 부분 실패 출처가 실제 공고 행만 선택하는 좁은 프로필을 사용하는지 검증합니다.
     *
     * @throws IOException migration 읽기 오류
     */
    @Test
    void v60MigrationStabilizesRemainingPartialRows() throws IOException {
        String sql = selectV60Migration();

        assertThat(sql).contains(
                "td.subject > a, td.title > a, td.bb-list-title > a",
                "'SEONGBUK_EMINWON_TABLE'",
                "table.p-table.simple tbody.text_center > tr:has(> td.p-subject > a)",
                "'CHANGWON_GOSI_TABLE'",
                "table.t3 tbody.tb > tr:has(> td.tal > a.a1)",
                "public_code = 'LGS-000009'",
                "public_code = 'LGS-000224'"
        );
    }

    /**
     * 일반 공지 출처를 공식 고시공고 게시판으로 보정하는 정적 계약을 검증합니다.
     *
     * @throws IOException migration 읽기 오류
     */
    @Test
    void v61MigrationCorrectsReviewedSourcesToOfficialLegalNoticeBoards() throws IOException {
        String sql = selectV61Migration();
        String reviewedSources = sql.substring(
                sql.indexOf("WITH reviewed_source ("),
                sql.indexOf("UPDATE local_government_notice_sources AS source", sql.indexOf("WITH reviewed_source ("))
        );

        assertThat(countOccurrences(reviewedSources, "('LGS-")).isEqualTo(203);
        assertThat(sql).contains(
                "source_board_type_code = 'LEGAL_NOTICE'",
                "collection_policy_code = 'KEYWORD_FILTERED'",
                "validation_status_code = 'VERIFIED'",
                "is_semantically_verified = true",
                "parser_profile_code = 'MANUAL_ONLY'",
                "request_method_code = 'POST_FORM'",
                "'SAFE_SAEOL_EMINWON_LIST'",
                "'MAPO_LEGAL_NOTICE_TABLE'",
                "'SAFE_DAEGU_LEGAL_NOTICE'",
                "'SAFE_INCHEON_CITYNET_NOTICE'",
                "'SAFE_GWANGJU_NAMGU_NOTICE'",
                "'SAFE_DAEJEON_DATA_KEY_NOTICE'",
                "'SAFE_YUSEONG_LEGAL_NOTICE'",
                "'SAFE_HWASEONG_LEGAL_NOTICE'",
                "'SAFE_PORTAL_SAEOL_BOARD_VIEW'",
                "'SAFE_GWANGMYEONG_LEGAL_NOTICE'",
                "'SAFE_EGOV_DATA_LIST_NOTICE'",
                "'YEONGCHEON_LEGAL_NOTICE'",
                "https://www.gangnam.go.kr/notice/list.do?mid=ID05_040201",
                "https://www.donggu.go.kr/dg/kor/contents/916",
                "https://www.yongin.go.kr/home/yiNw/yiNwStable/yiNwStable02/yiNwStable02_01.jsp",
                "https://www.seocheon.go.kr/prog/saeolGosi/03/kor/sub04_06_03/list.do",
                "'LGS-000158', 'https://eminwon.seocheon.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do'"
        );
        assertThat(sql.toLowerCase()).doesNotContain("delete from", "truncate table");
    }

    /**
     * 전수 QA를 통과한 출처만 활성화하고 실패 출처는 진단값과 함께 격리하는 계약을 검증합니다.
     *
     * @throws IOException migration 읽기 오류
     */
    @Test
    void v62MigrationActivatesOnlyQaPassedSourcesAndIsolatesFailures() throws IOException {
        String sql = selectV62Migration();
        String passedSources = sql.substring(
                sql.indexOf("WITH qa_pass_source"),
                sql.indexOf("UPDATE local_government_notice_sources AS source")
        );
        String blockedSources = sql.substring(
                sql.indexOf("WITH qa_blocked_source"),
                sql.indexOf("UPDATE local_government_notice_sources AS source", sql.indexOf("WITH qa_blocked_source"))
        );

        assertThat(countOccurrences(passedSources, "('LGS-")).isEqualTo(190);
        assertThat(countOccurrences(blockedSources, "('LGS-")).isEqualTo(13);
        assertThat(sql).contains(
                "is_enabled = true",
                "is_enabled = false",
                "'ITEM_FIELDS_MISSING'",
                "'LIST_SELECTOR_NOT_MATCHED'",
                "'HTTP_ERROR'",
                "enabled_count <> 190 OR blocked_count <> 13"
        );
        assertThat(sql.toLowerCase()).doesNotContain("delete from", "truncate table");
    }

    /**
     * 운영 공고의 다중 대상·지원유형 카탈로그와 결정적 대상 backfill 계약을 검증합니다.
     *
     * @throws IOException migration 읽기 오류
     */
    @Test
    void v63MigrationCreatesClassificationCatalogsAndBackfillsOnlyTargets() throws IOException {
        String sql = selectV63Migration();

        assertThat(sql).contains(
                "CREATE TABLE announcement_target_categories",
                "CREATE TABLE announcement_support_types",
                "CREATE TABLE announcement_target_category_assignments",
                "CREATE TABLE announcement_support_type_assignments",
                "WHERE is_primary = true",
                "'LEGACY_BACKFILL'",
                "category.category_code = announcement.target_type_code"
        );
        assertThat(countOccurrences(sql, "md5('announcement-target-category-")).isEqualTo(5);
        assertThat(countOccurrences(sql, "md5('announcement-support-type-")).isEqualTo(7);
        assertThat(sql).doesNotContain("INSERT INTO announcement_support_type_assignments (");
        assertThat(sql.toLowerCase()).doesNotContain("drop column target_type_code");
    }

    /**
     * 규칙 release, 그룹, 규칙, term의 FK·UNIQUE·부분 UNIQUE 계약을 검증합니다.
     *
     * @throws IOException migration 읽기 오류
     */
    @Test
    void v64MigrationCreatesVersionedRuleContract() throws IOException {
        String sql = selectV64Migration();

        assertThat(sql).contains(
                "CREATE TABLE announcement_source_classification_rule_releases",
                "CREATE TABLE announcement_source_classification_rule_groups",
                "CREATE TABLE announcement_source_classification_keyword_rules",
                "CREATE TABLE announcement_source_classification_keyword_terms",
                "release_status_code IN ('DRAFT', 'ACTIVE', 'RETIRED')",
                "combination_operator_code = 'AND'",
                "body_unavailable_action_code = 'REVIEW_REQUIRED'",
                "attachment_analysis_enabled = false",
                "auto_activation_enabled = false",
                "WHERE release_status_code = 'ACTIVE'",
                "UNIQUE (id, group_id)",
                "FOREIGN KEY (keyword_rule_id, group_id)",
                "UNIQUE (group_id, normalized_term_text, match_mode_code)",
                "WHERE term_type_code = 'CANONICAL'"
        );
        assertThat(sql).doesNotContain("${", "SELECT *");
    }

    /**
     * 초기 규칙이 비활성 DRAFT이고 B6 확정 구문만 활성화되는지 검증합니다.
     *
     * @throws IOException migration 읽기 오류
     */
    @Test
    void v65MigrationSeedsDraftWithoutBroadAdministrativeTerms() throws IOException {
        String sql = selectV65Migration();
        String enabledB6 = sql.substring(
                sql.indexOf("-- Group B: title EXCLUDED"),
                sql.indexOf("-- B6 unconfirmed remainder")
        );
        String disabledB6 = sql.substring(
                sql.indexOf("-- B6 unconfirmed remainder"),
                sql.indexOf("-- B5 agency terms")
        );

        assertThat(sql).contains(
                "'ASCR-000001'",
                "'DRAFT'",
                "'AND'",
                "'REVIEW_REQUIRED'",
                "'AUTO_EXCLUDE_B_EXPORT'",
                "'PROTECTED_METADATA_AGENCY'",
                "('기간제 근로자 채용', '기간제근로자 채용')",
                "('임기제 공무원 채용', '임기제공무원 채용')",
                "('물품 구매 입찰', '물품구매 입찰')"
        );
        assertThat(countOccurrences(enabledB6, "('AUTO_EXCLUDE_B_ADMINISTRATIVE'"))
                .isEqualTo(13);
        assertThat(countOccurrences(disabledB6, "('AUTO_EXCLUDE_B_ADMINISTRATIVE'"))
                .isEqualTo(58);
        assertThat(sql).doesNotContain(
                "('AUTO_EXCLUDE_B_ADMINISTRATIVE', '채용공고'",
                "('AUTO_EXCLUDE_B_ADMINISTRATIVE', '입찰공고'",
                "('AUTO_EXCLUDE_B_ADMINISTRATIVE', '고시'",
                "('AUTO_EXCLUDE_B_ADMINISTRATIVE', '의원'",
                "('AUTO_EXCLUDE_B_ADMINISTRATIVE', '입찰'",
                "('AUTO_EXCLUDE_B_ADMINISTRATIVE', '용역'",
                "('AUTO_EXCLUDE_B_ADMINISTRATIVE', '물품구매'",
                "('AUTO_EXCLUDE_B_ADMINISTRATIVE', '물품 구매'"
        );
        assertThat(sql).doesNotContain("announcement_source_semantic_keyword_rules");
    }

    /**
     * 원문 버전·판정·근거 match·수동 확정 태그의 append-only 계약을 검증합니다.
     *
     * @throws IOException migration 읽기 오류
     */
    @Test
    void v66MigrationCreatesClassificationEvidenceWithoutLegacyEvaluationConversion() throws IOException {
        String sql = selectV66Migration();

        assertThat(sql).contains(
                "CREATE TABLE announcement_source_content_versions",
                "CREATE TABLE announcement_source_classification_evaluations",
                "CREATE TABLE announcement_source_classification_matches",
                "CREATE TABLE announcement_source_classification_target_matches",
                "CREATE TABLE announcement_source_classification_support_matches",
                "CREATE TABLE announcement_source_confirmed_target_categories",
                "CREATE TABLE announcement_source_confirmed_support_types",
                "WHERE is_current = true",
                "match_location_code IN ('TITLE', 'BODY')",
                "confirmation_status_code IN ('CURRENT', 'STALE')",
                "ADD COLUMN rule_release_id uuid",
                "ADD COLUMN classification_row_version integer NOT NULL DEFAULT 0",
                "INSERT INTO announcement_source_content_versions"
        );
        assertThat(sql).doesNotContain(
                "INSERT INTO announcement_source_classification_evaluations",
                "INSERT INTO announcement_source_classification_matches"
        );
    }

    /**
     * QA 원문과 운영 근거가 기본값과 CHECK 제약으로 분리되는지 검증합니다.
     *
     * @throws IOException migration 읽기 오류
     */
    @Test
    void v67MigrationSeparatesQaAndProductionDataPurpose() throws IOException {
        String sql = selectV67Migration();

        assertThat(countOccurrences(
                sql,
                "ADD COLUMN data_purpose_code varchar(20) NOT NULL DEFAULT 'PRODUCTION'"
        )).isEqualTo(2);
        assertThat(countOccurrences(
                sql,
                "data_purpose_code IN ('PRODUCTION', 'QA')"
        )).isEqualTo(2);
        assertThat(sql.toLowerCase()).doesNotContain("delete from", "truncate table");
    }

    /**
     * 하나의 수집 원문이 둘 이상의 운영 공고로 전환되지 않는 DB 계약을 검증합니다.
     *
     * @throws IOException migration 읽기 오류
     */
    @Test
    void v68MigrationEnforcesOneAnnouncementLinkPerSourceWithoutDataCleanup() throws IOException {
        String sql = selectV68Migration();
        String normalizedSql = sql.toLowerCase();

        assertThat(sql).contains(
                "FROM announcement_source_links",
                "GROUP BY source_id",
                "HAVING count(1) > 1",
                "RAISE EXCEPTION USING",
                "ADD CONSTRAINT uq_announcement_source_links_source UNIQUE (source_id)"
        );
        assertThat(normalizedSql).doesNotContain(
                "unique (announcement_id)",
                "delete from announcement_source_links",
                "truncate table announcement_source_links",
                "update announcement_source_links"
        );
    }

    @Test
    void v69MigrationCreatesAuditableAndRecoverableReclassificationRuns() throws IOException {
        String sql = selectV69Migration();
        String mapper = selectAnnouncementSourceReclassificationRunMapper();

        assertThat(sql).contains(
                "CREATE TABLE announcement_source_reclassification_runs",
                "CREATE TABLE announcement_source_reclassification_run_items",
                "rule_release_id uuid NOT NULL",
                "scope_snapshot_at timestamptz NOT NULL DEFAULT now()",
                "request_reason_hash varchar(64) NOT NULL",
                "UNIQUE (run_id, source_id)",
                "'PREVIEW_PENDING'",
                "'APPLY_PAUSED'",
                "'ROLLBACK_PENDING'",
                "'ROLLED_BACK'"
        );
        assertThat(mapper).contains("snapshot.data_purpose_code = 'PRODUCTION'");
        assertThat(sql).doesNotContain("DROP TABLE", "TRUNCATE TABLE", "DELETE FROM announcement_source_snapshots");
    }

    /**
     * 제목 제외 공고가 원문 없이 비가역 tombstone과 규칙 참조로 전환되는지 검증합니다.
     *
     * @throws IOException migration 읽기 오류
     */
    @Test
    void v70MigrationRemovesExcludedPlaintextAndPreservesNonReversibleEvidence() throws IOException {
        String sql = selectV70Migration();

        assertThat(sql).contains(
                "CREATE TABLE announcement_source_exclusion_tombstones",
                "CREATE TABLE announcement_source_exclusion_rule_matches",
                "digest(",
                "'sha256'",
                "identity_hash varchar(64) NOT NULL",
                "FOREIGN KEY (run_id) REFERENCES announcement_source_collection_runs (id) ON DELETE SET NULL",
                "FOREIGN KEY (rule_release_id)",
                "RAISE EXCEPTION 'Linked excluded announcement sources must be separated before V70 cleanup.'",
                "SET source_id = NULL",
                "provider_notice_id = NULL",
                "source_url = NULL",
                "semantic_matched_keywords = NULL",
                "ADD COLUMN exclusion_id uuid",
                "ALTER COLUMN source_id DROP NOT NULL",
                "ALTER COLUMN content_version_id DROP NOT NULL",
                "Active reclassification runs for excluded sources must finish before V70 cleanup.",
                "UPDATE announcement_source_reclassification_run_items AS item",
                "previous_evaluation_id = NULL",
                "applied_evaluation_id = NULL",
                "DELETE FROM announcement_source_snapshots",
                "WHERE semantic_status_code = 'EXCLUDED'"
        );
        assertThat(sql).doesNotContain(
                "title varchar",
                "body_text text",
                "raw_payload_json jsonb",
                "matched_text varchar",
                "source_url text",
                "provider_notice_id varchar"
        );
    }

    /**
     * 대량 제외 source 정리 시 수집 실행 항목의 FK 탐색이 전체 스캔으로 반복되지 않는지 검증합니다.
     *
     * @throws IOException migration 읽기 오류
     */
    @Test
    void v71MigrationIndexesCollectionRunItemsBySource() throws IOException {
        String sql = selectV71Migration();

        assertThat(sql).contains(
                "CREATE INDEX IF NOT EXISTS ix_announcement_source_collection_run_items_source",
                "ON announcement_source_collection_run_items (source_id)",
                "WHERE source_id IS NOT NULL"
        );
        assertThat(sql).doesNotContain("DELETE FROM", "TRUNCATE TABLE", "DROP TABLE");
    }

    /**
     * 수집 승인 요청 INSERT가 nullable UUID의 PostgreSQL 타입을 명시하는지 확인합니다.
     *
     * @throws IOException Mapper 읽기 오류
     */
    @Test
    void announcementSourceMapperCastsNullableApprovedByAsUuid() throws IOException {
        String mapper = selectAnnouncementSourceMapper();

        assertThat(mapper)
                .contains("CASE WHEN CAST(#{approvedBy} AS uuid) IS NOT NULL THEN now() ELSE NULL END")
                .contains(
                        "<arg column=\"total_count\" javaType=\"_int\"/>",
                        "<arg column=\"collected_count\" javaType=\"_int\"/>",
                        "<arg column=\"skipped_ended_count\" javaType=\"_int\"/>",
                        "<arg column=\"duplicate_count\" javaType=\"_int\"/>",
                        "<arg column=\"failed_count\" javaType=\"_int\"/>"
                )
                .doesNotContain("CASE WHEN #{approvedBy} IS NOT NULL THEN now() ELSE NULL END");
    }

    /**
     * 운영 공고 전환 Mapper가 원문 잠금과 실제 공고 공개 코드 컬럼을 사용하는지 검증합니다.
     *
     * @throws IOException Mapper 읽기 오류
     */
    @Test
    void announcementSourceMapperLocksSourceAndSelectsLinkedAnnouncementPublicCode() throws IOException {
        String mapper = selectAnnouncementSourceMapper();

        assertThat(mapper).contains(
                "<select id=\"selectSourceDetailsForUpdate\" resultMap=\"SourceSnapshotRowMap\">",
                "WHERE ass.id = #{sourceId}",
                "FOR UPDATE",
                "<select id=\"selectLinkedAnnouncementDetails\" resultMap=\"SourceLinkedAnnouncementRowMap\">",
                "a.public_code AS announcement_code"
        );
        assertThat(mapper).doesNotContain("a.announcement_code");
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @return 처리 결과
     *
     * @throws IOException 처리 중 예외가 발생한 경우
     */
    private String selectV1Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource("db/migration/V1__create_mvp_schema.sql");
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @return 처리 결과
     *
     * @throws IOException 처리 중 예외가 발생한 경우
     */
    private String selectV4Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource("db/migration/V4__create_dynamic_announcement_inputs.sql");
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @return 처리 결과
     *
     * @throws IOException 처리 중 예외가 발생한 경우
     */
    private String selectV6Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource("db/migration/V6__allow_matching_without_verification.sql");
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @return 처리 결과
     *
     * @throws IOException 처리 중 예외가 발생한 경우
     */
    private String selectV7Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource("db/migration/V7__create_user_consents.sql");
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @return 처리 결과
     *
     * @throws IOException 처리 중 예외가 발생한 경우
     */
    private String selectV8Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource("db/migration/V8__create_document_file_submissions.sql");
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @return 처리 결과
     *
     * @throws IOException 처리 중 예외가 발생한 경우
     */
    private String selectV9Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource("db/migration/V9__create_consultation_reservations.sql");
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @return 처리 결과
     *
     * @throws IOException 처리 중 예외가 발생한 경우
     */
    private String selectV10Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource("db/migration/V10__create_subscription_payments.sql");
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @return 처리 결과
     *
     * @throws IOException 처리 중 예외가 발생한 경우
     */
    private String selectV11Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource("db/migration/V11__create_notifications_operation_tasks.sql");
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @return 처리 결과
     *
     * @throws IOException 처리 중 예외가 발생한 경우
     */
    private String selectV12Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource("db/migration/V12__create_admin_report_exports.sql");
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @return 처리 결과
     *
     * @throws IOException 처리 중 예외가 발생한 경우
     */
    private String selectV14Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource("db/migration/V14__add_reviewer_and_manual_consultation.sql");
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @return 처리 결과
     *
     * @throws IOException 처리 중 예외가 발생한 경우
     */
    private String selectV15Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource("db/migration/V15__create_standard_document_fields.sql");
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @return 처리 결과
     *
     * @throws IOException 처리 중 예외가 발생한 경우
     */
    private String selectV16Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource("db/migration/V16__create_member_document_input_values.sql");
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @return 처리 결과
     *
     * @throws IOException 처리 중 예외가 발생한 경우
     */
    private String selectV17Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource("db/migration/V17__seed_mock_subscription_plan.sql");
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @return 처리 결과
     *
     * @throws IOException 처리 중 예외가 발생한 경우
     */
    private String selectV19Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource("db/migration/V19__add_matching_stage_flow.sql");
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @return 처리 결과
     *
     * @throws IOException 처리 중 예외가 발생한 경우
     */
    private String selectV21Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource("db/migration/V21__create_standard_code_catalogs.sql");
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @return 처리 결과
     *
     * @throws IOException 처리 중 예외가 발생한 경우
     */
    private String selectV22Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource("db/migration/V22__add_structured_address_fields.sql");
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @return 처리 결과
     *
     * @throws IOException 처리 중 예외가 발생한 경우
     */
    private String selectV25Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource("db/migration/V25__add_member_interview_responses.sql");
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @return 처리 결과
     *
     * @throws IOException 처리 중 예외가 발생한 경우
     */
    private String selectV26Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource("db/migration/V26__create_announcement_source_collection.sql");
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @return 처리 결과
     *
     * @throws IOException 처리 중 예외가 발생한 경우
     */
    private String selectV27Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource(
                "db/migration/V27__create_announcement_source_duplicate_candidates.sql"
        );
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * V28 migration을 UTF-8로 조회합니다.
     *
     * @return V28 SQL
     * @throws IOException migration 읽기 오류
     */
    private String selectV28Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource(
                "db/migration/V28__create_local_government_notice_collection.sql"
        );
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * V29 migration을 UTF-8로 조회합니다.
     *
     * @return V29 SQL
     * @throws IOException migration 읽기 오류
     */
    private String selectV29Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource(
                "db/migration/V29__seed_local_government_notice_sources.sql"
        );
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * V30 migration을 UTF-8로 조회합니다.
     *
     * @return V30 SQL
     * @throws IOException migration 읽기 오류
     */
    private String selectV30Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource(
                "db/migration/V30__add_heuristic_local_government_notice_parser.sql"
        );
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * V31 migration을 UTF-8로 조회합니다.
     *
     * @return V31 SQL
     * @throws IOException migration 읽기 오류
     */
    private String selectV31Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource(
                "db/migration/V31__apply_local_government_parser_qa_results.sql"
        );
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * V32 migration을 UTF-8로 조회합니다.
     *
     * @return V32 SQL
     * @throws IOException migration 읽기 오류
     */
    private String selectV32Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource(
                "db/migration/V32__harden_local_government_notice_source_requests.sql"
        );
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * V33 migration을 UTF-8로 조회합니다.
     *
     * @return V33 SQL
     * @throws IOException migration 읽기 오류
     */
    private String selectV33Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource(
                "db/migration/V33__apply_local_government_parser_qa_hardening_results.sql"
        );
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * V34 migration을 UTF-8로 조회합니다.
     *
     * @return V34 SQL
     * @throws IOException migration 읽기 오류
     */
    private String selectV34Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource(
                "db/migration/V34__add_safe_local_government_link_templates.sql"
        );
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * V35 migration을 UTF-8로 조회합니다.
     *
     * @return V35 SQL
     * @throws IOException migration 읽기 오류
     */
    private String selectV35Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource(
                "db/migration/V35__apply_safe_link_template_parser_qa_results.sql"
        );
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * V36 migration을 UTF-8로 조회합니다.
     *
     * @return V36 SQL
     * @throws IOException migration 읽기 오류
     */
    private String selectV36Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource(
                "db/migration/V36__correct_official_local_government_notice_urls.sql"
        );
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * V37 migration을 UTF-8로 조회합니다.
     *
     * @return V37 SQL
     * @throws IOException migration 읽기 오류
     */
    private String selectV37Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource(
                "db/migration/V37__add_common_local_government_notice_parser_profiles.sql"
        );
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * V38 migration을 UTF-8로 조회합니다.
     *
     * @return V38 SQL
     * @throws IOException migration 읽기 오류
     */
    private String selectV38Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource(
                "db/migration/V38__support_remaining_local_government_notice_structures.sql"
        );
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * V39 migration을 UTF-8로 조회합니다.
     *
     * @return V39 SQL
     * @throws IOException migration 읽기 오류
     */
    private String selectV39Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource(
                "db/migration/V39__correct_final_reviewed_local_government_notice_urls.sql"
        );
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * V40 migration을 UTF-8로 조회합니다.
     *
     * @return V40 SQL
     * @throws IOException migration 읽기 오류
     */
    private String selectV40Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource(
                "db/migration/V40__apply_comprehensive_local_government_parser_qa_results.sql"
        );
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * V41 migration을 UTF-8로 조회합니다.
     *
     * @return V41 SQL
     * @throws IOException migration 읽기 오류
     */
    private String selectV41Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource(
                "db/migration/V41__correct_additional_official_local_government_notice_urls.sql"
        );
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * V42 migration을 UTF-8로 조회합니다.
     *
     * @return V42 SQL
     * @throws IOException migration 읽기 오류
     */
    private String selectV42Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource(
                "db/migration/V42__add_narrow_local_government_notice_parser_profiles.sql"
        );
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * V43 migration을 UTF-8로 조회합니다.
     *
     * @return V43 SQL
     * @throws IOException migration 읽기 오류
     */
    private String selectV43Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource(
                "db/migration/V43__apply_narrow_parser_qa_results.sql"
        );
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * V44 migration을 UTF-8로 조회합니다.
     *
     * @return V44 SQL
     * @throws IOException migration 읽기 오류
     */
    private String selectV44Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource(
                "db/migration/V44__add_egov_detail_cell_parser_profile.sql"
        );
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * V45 migration을 UTF-8로 조회합니다.
     *
     * @return V45 SQL
     * @throws IOException migration 읽기 오류
     */
    private String selectV45Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource(
                "db/migration/V45__apply_gongju_parser_qa_result.sql"
        );
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * V46 migration을 UTF-8로 조회합니다.
     *
     * @return V46 SQL
     * @throws IOException migration 읽기 오류
     */
    private String selectV46Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource(
                "db/migration/V46__add_legacy_browser_request_profile.sql"
        );
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * V47 migration을 UTF-8로 조회합니다.
     *
     * @return V47 SQL
     * @throws IOException migration 읽기 오류
     */
    private String selectV47Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource(
                "db/migration/V47__add_legacy_board_parser_profiles.sql"
        );
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * V48 migration을 UTF-8로 조회합니다.
     *
     * @return V48 SQL
     * @throws IOException migration 읽기 오류
     */
    private String selectV48Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource(
                "db/migration/V48__apply_legacy_board_parser_qa_results.sql"
        );
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * V49 migration을 UTF-8로 조회합니다.
     *
     * @return V49 SQL
     * @throws IOException migration 읽기 오류
     */
    private String selectV49Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource(
                "db/migration/V49__correct_remaining_official_notice_urls.sql"
        );
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * V50 migration을 UTF-8로 조회합니다.
     *
     * @return V50 SQL
     * @throws IOException migration 읽기 오류
     */
    private String selectV50Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource(
                "db/migration/V50__add_compact_saeol_parser_profile.sql"
        );
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * V51 migration을 UTF-8로 조회합니다.
     *
     * @return V51 SQL
     * @throws IOException migration 읽기 오류
     */
    private String selectV51Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource(
                "db/migration/V51__apply_corrected_official_url_qa_results.sql"
        );
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * V52 migration을 UTF-8로 조회합니다.
     *
     * @return V52 SQL
     * @throws IOException migration 읽기 오류
     */
    private String selectV52Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource(
                "db/migration/V52__stabilize_gangdong_notice_request.sql"
        );
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * V53 migration을 UTF-8로 조회합니다.
     *
     * @return V53 SQL
     * @throws IOException migration 읽기 오류
     */
    private String selectV53Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource(
                "db/migration/V53__apply_gangdong_transport_qa_result.sql"
        );
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * V54 migration을 UTF-8로 조회합니다.
     *
     * @return V54 SQL
     * @throws IOException migration 읽기 오류
     */
    private String selectV54Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource(
                "db/migration/V54__apply_remaining_verified_parser_results.sql"
        );
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * V55 migration을 UTF-8로 조회합니다.
     *
     * @return V55 SQL
     * @throws IOException migration 읽기 오류
     */
    private String selectV55Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource(
                "db/migration/V55__recover_remaining_local_government_notice_sources.sql"
        );
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * V57 migration을 UTF-8로 조회합니다.
     *
     * @return V57 SQL
     * @throws IOException migration 읽기 오류
     */
    private String selectV57Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource(
                "db/migration/V57__correct_miryang_hamyang_official_notice_sources.sql"
        );
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * V58 migration을 UTF-8로 조회합니다.
     *
     * @return V58 SQL
     * @throws IOException migration 읽기 오류
     */
    private String selectV58Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource(
                "db/migration/V58__separate_official_notice_links_from_collection_endpoints.sql"
        );
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * V59 migration을 UTF-8로 조회합니다.
     *
     * @return V59 SQL
     * @throws IOException migration 읽기 오류
     */
    private String selectV59Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource(
                "db/migration/V59__tighten_local_government_notice_row_detection.sql"
        );
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * V60 migration을 UTF-8로 조회합니다.
     *
     * @return V60 SQL
     * @throws IOException migration 읽기 오류
     */
    private String selectV60Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource(
                "db/migration/V60__stabilize_remaining_local_government_notice_rows.sql"
        );
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * V61 migration을 UTF-8로 조회합니다.
     *
     * @return V61 SQL
     * @throws IOException migration 읽기 오류
     */
    private String selectV61Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource(
                "db/migration/V61__correct_general_notice_sources_to_official_legal_boards.sql"
        );
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * V62 migration을 UTF-8로 조회합니다.
     *
     * @return V62 SQL
     * @throws IOException migration 읽기 오류
     */
    private String selectV62Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource(
                "db/migration/V62__apply_corrected_legal_notice_parser_qa_results.sql"
        );
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * V63 migration을 UTF-8로 조회합니다.
     *
     * @return V63 SQL
     * @throws IOException migration 읽기 오류
     */
    private String selectV63Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource(
                "db/migration/V63__create_announcement_classification_catalogs.sql"
        );
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * V64 migration을 UTF-8로 조회합니다.
     *
     * @return V64 SQL
     * @throws IOException migration 읽기 오류
     */
    private String selectV64Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource(
                "db/migration/V64__create_versioned_announcement_classification_rules.sql"
        );
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * V65 migration을 UTF-8로 조회합니다.
     *
     * @return V65 SQL
     * @throws IOException migration 읽기 오류
     */
    private String selectV65Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource(
                "db/migration/V65__seed_announcement_classification_draft_v1.sql"
        );
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * V66 migration을 UTF-8로 조회합니다.
     *
     * @return V66 SQL
     * @throws IOException migration 읽기 오류
     */
    private String selectV66Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource(
                "db/migration/V66__create_announcement_classification_evidence.sql"
        );
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * V67 migration을 UTF-8로 조회합니다.
     *
     * @return V67 SQL
     * @throws IOException migration 읽기 오류
     */
    private String selectV67Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource(
                "db/migration/V67__separate_announcement_source_data_purpose.sql"
        );
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * V68 migration을 UTF-8로 조회합니다.
     *
     * @return V68 SQL
     * @throws IOException migration 읽기 오류
     */
    private String selectV68Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource(
                "db/migration/V68__enforce_single_announcement_source_link.sql"
        );
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    private String selectV69Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource(
                "db/migration/V69__create_announcement_source_reclassification_runs.sql"
        );
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    private String selectV70Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource(
                "db/migration/V70__stop_persisting_title_excluded_sources.sql"
        );
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    private String selectV71Migration() throws IOException {
        ClassPathResource resource = new ClassPathResource(
                "db/migration/V71__index_collection_run_items_by_source.sql"
        );
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    private String selectAnnouncementSourceReclassificationRunMapper() throws IOException {
        ClassPathResource resource = new ClassPathResource(
                "mapper/announcementsource/AnnouncementSourceReclassificationRunMapper.xml"
        );
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * 대상 문자열에서 검색 문자열이 나타난 횟수를 계산합니다.
     *
     * @param source 대상 문자열
     * @param token 검색 문자열
     * @return 검색 문자열 출현 횟수
     */
    private int countOccurrences(String source, String token) {
        int count = 0;
        int offset = 0;
        while ((offset = source.indexOf(token, offset)) >= 0) {
            count++;
            offset += token.length();
        }
        return count;
    }

    /**
     * 외부 공고 수집 Mapper를 UTF-8로 조회합니다.
     *
     * @return 외부 공고 수집 Mapper XML
     * @throws IOException Mapper 읽기 오류
     */
    private String selectAnnouncementSourceMapper() throws IOException {
        ClassPathResource resource = new ClassPathResource(
                "mapper/announcementsource/AnnouncementSourceMapper.xml"
        );
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }
}
