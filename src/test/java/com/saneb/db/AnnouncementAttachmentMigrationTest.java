package com.saneb.db;

import static org.assertj.core.api.Assertions.*;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named="SANEB_ATTACHMENT_MIGRATION_TEST",matches="true")
class AnnouncementAttachmentMigrationTest {
    @Test void immutableEvidenceRejectsMismatchedBindingsAndCascadesWithSource() throws Exception {
        try (var pg=EmbeddedPostgres.builder().setPort(0).setServerConfig("listen_addresses","127.0.0.1").start()) {
            var dataSource=pg.getPostgresDatabase();
            Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();
            var sql=new org.springframework.jdbc.core.JdbcTemplate(dataSource);
            UUID actor=UUID.randomUUID(), source=UUID.randomUUID(), other=UUID.randomUUID(), content=UUID.randomUUID();
            UUID policy=UUID.randomUUID(), otherPolicy=UUID.randomUUID(), set=UUID.randomUUID(), file=UUID.randomUUID();
            UUID base=UUID.randomUUID(), evaluation=UUID.randomUUID();
            UUID release=sql.queryForObject("SELECT id FROM announcement_source_classification_rule_releases ORDER BY version_no LIMIT 1",UUID.class);
            sql.update("INSERT INTO users(id,login_id,password_hash,name,status_code,password_reset_required) VALUES (?,?,?,?,'ACTIVE',false)",
                    actor,"attachment-fixture-admin","unused-fixture-hash","첨부 검증 전용");
            for (UUID sourceId:java.util.List.of(source,other)) sql.update("INSERT INTO announcement_source_snapshots(id,provider_code,title,raw_hash) VALUES (?,'BIZINFO','소상공인 지원금',?)",sourceId,sourceId.toString().replace("-","").repeat(2));
            sql.update("INSERT INTO announcement_source_content_versions(id,source_id,raw_hash,title,body_source_code,body_availability_code,collected_at) VALUES (?,?,repeat('a',64),'소상공인 지원금','NONE','UNAVAILABLE',now())",content,source);
            sql.update("INSERT INTO announcement_source_classification_evaluations(id,source_id,content_version_id,rule_release_id,engine_version,body_source_code,body_availability_code,title_stage_code,body_stage_code,decision_status_code,reason_code) VALUES (?,?,?,?,'fixture','NONE','UNAVAILABLE','COMBINATION_MATCHED','UNAVAILABLE','REVIEW_REQUIRED','BODY_UNAVAILABLE')",base,source,content,release);
            for (UUID policyId:java.util.List.of(policy,otherPolicy)) sql.update("INSERT INTO announcement_attachment_policies(id,policy_code,version_no,mode_code,rule_release_id,settings_json,profile_manifest_json,created_by) VALUES (?,?,1,'COLLECT_ONLY',?,'{}','[]',?)",policyId,policyId.toString(),release,actor);
            sql.update("INSERT INTO announcement_source_attachment_sets(id,source_id,content_version_id,policy_id,data_purpose_code,profile_hash) VALUES (?,?,?,?,'PRODUCTION',repeat('b',64))",set,source,content,policy);
            assertThatThrownBy(() -> sql.update("INSERT INTO announcement_source_attachment_files(id,set_id,source_id,stable_locator_hash,safe_locator_json,sort_order) VALUES (?,?,?,repeat('c',64),'{}',0)",UUID.randomUUID(),set,other))
                    .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
            sql.update("INSERT INTO announcement_source_attachment_files(id,set_id,source_id,stable_locator_hash,safe_locator_json,sort_order,download_status_code,downloaded_bytes,binary_hash) VALUES (?,?,?,repeat('c',64),'{}',0,'SUCCEEDED',100,repeat('d',64))",file,set,source);
            assertThatThrownBy(() -> sql.update("UPDATE announcement_source_attachment_sets SET set_status_code='SEALED',discovery_status_code='FOUND',is_discovery_complete=true,discovered_count=1,processed_count=1,manifest_hash=repeat('e',64),sealed_at=now() WHERE id=?",set))
                    .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
            sql.update("INSERT INTO announcement_source_attachment_extractions(file_id,set_id,source_id,attempt_no,extractor_code,extractor_version,extractor_config_hash,quality_code,extracted_text,text_hash,blocks_json,character_count,duration_ms) VALUES (?,?,?,1,'HWPX','1.0.0',repeat('f',64),'COMPLETE_TEXT','소상공인 지원금',repeat('a',64),'[{\"index\":0,\"startOffset\":0,\"endOffset\":8}]',8,10)",file,set,source);
            sql.update("UPDATE announcement_source_attachment_sets SET set_status_code='SEALED',discovery_status_code='FOUND',is_discovery_complete=true,discovered_count=1,processed_count=1,manifest_hash=repeat('e',64),sealed_at=now() WHERE id=?",set);
            assertThatThrownBy(() -> sql.update("UPDATE announcement_source_attachment_files SET document_role_code='NOTICE' WHERE id=?",file))
                    .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
            assertThatThrownBy(() -> sql.update("UPDATE announcement_source_snapshots SET data_purpose_code='QA' WHERE id=?",source))
                    .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
            String insertEvaluation="INSERT INTO announcement_source_attachment_evaluations(id,source_id,content_version_id,base_evaluation_id,set_id,policy_id,rule_release_id,engine_version,input_hash,decision_hash,decision_status_code,reason_code) VALUES (?,?,?,?,?,?,?,'fixture',repeat('1',64),repeat('2',64),'REVIEW_REQUIRED','ATTACHMENT_CONTEXT_REVIEW')";
            assertThatThrownBy(() -> sql.update(insertEvaluation,UUID.randomUUID(),source,content,base,set,otherPolicy,release))
                    .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
            sql.update(insertEvaluation,evaluation,source,content,base,set,policy,release);
            assertThatThrownBy(() -> sql.update("UPDATE announcement_source_snapshots SET current_attachment_evaluation_id=? WHERE id=?",evaluation,source))
                    .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
            new org.springframework.transaction.support.TransactionTemplate(new org.springframework.jdbc.datasource.DataSourceTransactionManager(dataSource)).execute(status -> {
                sql.update("UPDATE announcement_source_attachment_evaluations SET is_current=true WHERE id=?",evaluation);
                sql.update("UPDATE announcement_source_snapshots SET current_attachment_evaluation_id=? WHERE id=?",evaluation,source);
                return null;
            });
            sql.update("DELETE FROM announcement_source_snapshots WHERE id=?",source);
            assertThat(sql.queryForObject("SELECT count(1) FROM announcement_source_attachment_files",Integer.class)).isZero();
            assertThat(sql.queryForObject("SELECT count(1) FROM announcement_source_attachment_extractions",Integer.class)).isZero();
            assertThat(sql.queryForObject("SELECT count(1) FROM announcement_source_attachment_evaluations",Integer.class)).isZero();
        }
    }

    @Test void freshSchemaAndV71UpgradePreservePriorChecksums() throws Exception {
        try (var pg = EmbeddedPostgres.builder().setPort(0)
                .setServerConfig("listen_addresses","127.0.0.1").start()) {
            var dataSource = pg.getPostgresDatabase();
            Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").target("71").load().migrate();
            try (Connection connection=dataSource.getConnection(); var statement=connection.createStatement()) {
                statement.execute("CREATE TABLE prior_checksums AS SELECT version,checksum FROM flyway_schema_history WHERE success");
            }
            var upgrade=Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").target("72").load();
            assertThat(upgrade.migrate().migrationsExecuted).isEqualTo(1);
            upgrade.validate();
            try (Connection connection=dataSource.getConnection(); var statement=connection.createStatement()) {
                try (var rows=statement.executeQuery("SELECT count(1) FROM prior_checksums p JOIN flyway_schema_history f USING(version) WHERE p.checksum IS DISTINCT FROM f.checksum")) {
                    rows.next(); assertThat(rows.getInt(1)).isZero();
                }
                try (var rows=statement.executeQuery("SELECT count(1) FROM information_schema.tables WHERE table_schema='public' AND table_name IN ('announcement_attachment_policies','announcement_attachment_batches','announcement_attachment_jobs','announcement_source_attachment_sets','announcement_source_attachment_files','announcement_source_attachment_extractions','announcement_source_attachment_evaluations','announcement_source_attachment_evaluation_inputs','announcement_source_attachment_matches','announcement_source_attachment_confirmations','announcement_source_attachment_tags')")) {
                    rows.next(); assertThat(rows.getInt(1)).isEqualTo(11);
                }
                try (var rows=statement.executeQuery("SELECT count(1) FROM announcement_attachment_policies WHERE policy_status_code='ACTIVE'")) {
                    rows.next(); assertThat(rows.getInt(1)).isZero();
                }
                statement.execute("CREATE DATABASE attachment_fresh");
            }
            var workerUpgrade = Flyway.configure().dataSource(dataSource)
                    .locations("classpath:db/migration").target("73").load();
            assertThat(workerUpgrade.migrate().migrationsExecuted).isEqualTo(1);
            workerUpgrade.validate();
            var workerSql = new org.springframework.jdbc.core.JdbcTemplate(dataSource);
            assertThat(workerSql.queryForObject("SELECT count(1) FROM announcement_attachment_resource_leases", Integer.class)).isZero();
            assertThat(workerSql.queryForObject("SELECT count(1) FROM information_schema.columns WHERE table_schema='public' AND table_name='announcement_source_attachment_confirmations' AND column_name IN ('confirmed_source_version','confirmed_attachment_version')",Integer.class)).isEqualTo(2);
            assertThat(workerSql.queryForObject("SELECT count(1) FROM information_schema.columns WHERE table_schema='public' AND table_name='announcement_source_links' AND column_name IN ('attachment_confirmation_id','attachment_request_hash')",Integer.class)).isEqualTo(2);
            assertThat(workerSql.queryForObject("SELECT count(1) FROM pg_trigger WHERE tgname IN ('tr_att_confirmation_immutable','tr_att_link_request_immutable') AND NOT tgisinternal",Integer.class)).isEqualTo(2);
            assertThat(workerSql.queryForObject("SELECT count(1) FROM announcement_attachment_policies WHERE policy_status_code='ACTIVE'", Integer.class)).isZero();
            assertThat(workerSql.queryForObject("SELECT count(1) FROM prior_checksums p JOIN flyway_schema_history f USING(version) WHERE p.checksum IS DISTINCT FROM f.checksum", Integer.class)).isZero();
            var inventoryUpgrade=Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").target("74").load();
            assertThat(inventoryUpgrade.migrate().migrationsExecuted).isEqualTo(1);
            inventoryUpgrade.validate();
            assertThat(workerSql.queryForObject("SELECT count(1) FROM information_schema.tables WHERE table_schema='public' AND table_name IN ('announcement_attachment_backfill_runs','announcement_attachment_backfill_segments','announcement_attachment_backfill_items')",Integer.class)).isEqualTo(3);
            assertThat(workerSql.queryForObject("SELECT count(1) FROM announcement_attachment_backfill_runs",Integer.class)).isZero();
            var linkUpgrade=Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").target("75").load();
            assertThat(linkUpgrade.migrate().migrationsExecuted).isEqualTo(1);linkUpgrade.validate();
            assertThat(workerSql.queryForObject("SELECT count(1) FROM announcement_attachment_backfill_segment_batches",Integer.class)).isZero();
            var scopeUpgrade=Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").target("76").load();
            assertThat(scopeUpgrade.migrate().migrationsExecuted).isEqualTo(1);scopeUpgrade.validate();
            assertThat(workerSql.queryForObject("SELECT count(1) FROM announcement_attachment_policy_publication_scopes",Integer.class)).isZero();
            assertThat(workerSql.queryForObject("SELECT count(1) FROM announcement_attachment_policy_publication_scope_items",Integer.class)).isZero();
            var publicationUpgrade=Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").target("77").load();
            assertThat(publicationUpgrade.migrate().migrationsExecuted).isEqualTo(1);publicationUpgrade.validate();
            assertThat(workerSql.queryForObject("SELECT count(1) FROM announcement_attachment_policy_publications",Integer.class)).isZero();
            workerSql.execute("CREATE TABLE prior_attachment_checksums AS SELECT version,checksum FROM flyway_schema_history WHERE success");
            var providerUpgrade=Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").target("78").load();
            assertThat(providerUpgrade.migrate().migrationsExecuted).isEqualTo(1);providerUpgrade.validate();
            assertThat(workerSql.queryForObject("SELECT count(1) FROM prior_attachment_checksums p JOIN flyway_schema_history f USING(version) WHERE p.checksum IS DISTINCT FROM f.checksum",Integer.class)).isZero();
            for(String constraint:java.util.List.of("announcement_attachment_jobs_frozen_provider_code_check","announcement_attachment_backfill_items_provider_code_check")) {
                var definition=workerSql.queryForObject("SELECT pg_get_constraintdef(oid) FROM pg_constraint WHERE conname=? AND convalidated",String.class,constraint);
                assertThat(definition).contains("GOV24_PUBLIC_SERVICE", "GOV24");
                String column=constraint.startsWith("announcement_attachment_jobs_")?"frozen_provider_code":"provider_code";
                // 각 JdbcTemplate 호출은 별도 연결이다. 이 격리 테스트 DB의 probe만 생성/삭제한다.
                workerSql.execute("CREATE TABLE gov24_provider_probe ("+column+" varchar(30), "+definition+")");
                for(String code:java.util.List.of("BIZINFO","GOV24","GOV24_PUBLIC_SERVICE","LOCAL_GOV_NOTICE"))
                    assertThat(workerSql.update("INSERT INTO gov24_provider_probe VALUES (?)",code)).isEqualTo(1);
                assertThatThrownBy(()->workerSql.update("INSERT INTO gov24_provider_probe VALUES ('UNKNOWN')"))
                        .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
                workerSql.execute("DROP TABLE gov24_provider_probe");
            }
            assertThat(workerSql.queryForObject("SELECT count(1) FROM announcement_attachment_policies WHERE policy_status_code='ACTIVE'",Integer.class)).isZero();
            assertThat(workerSql.queryForObject("SELECT count(1) FROM prior_checksums p JOIN flyway_schema_history f USING(version) WHERE p.checksum IS DISTINCT FROM f.checksum",Integer.class)).isZero();
            var providerLedgerUpgrade=Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").target("79").load();
            assertThat(providerLedgerUpgrade.migrate().migrationsExecuted).isEqualTo(1);providerLedgerUpgrade.validate();
            assertThat(workerSql.queryForObject("SELECT count(1) FROM announcement_attachment_provider_qa_runs",Integer.class)).isZero();
            assertThat(workerSql.queryForObject("SELECT count(1) FROM announcement_attachment_provider_qa_cases",Integer.class)).isZero();
            var providerPlanUpgrade=Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").target("80").load();
            assertThat(providerPlanUpgrade.migrate().migrationsExecuted).isEqualTo(1);providerPlanUpgrade.validate();
            assertThat(workerSql.queryForObject("SELECT count(1) FROM announcement_attachment_provider_qa_run_plans",Integer.class)).isZero();
            var providerLockUpgrade=Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").target("81").load();
            assertThat(providerLockUpgrade.migrate().migrationsExecuted).isEqualTo(1);providerLockUpgrade.validate();
            assertThat(workerSql.queryForObject("SELECT pg_get_functiondef('attachment_policy_publication_lock'::regproc)",String.class))
                    .contains("announcement_attachment_provider_qa_runs","announcement_attachment_provider_qa_cases","announcement_attachment_provider_qa_run_plans");
            assertThat(workerSql.queryForObject("SELECT count(1) FROM prior_checksums p JOIN flyway_schema_history f USING(version) WHERE p.checksum IS DISTINCT FROM f.checksum",Integer.class)).isZero();
            assertThat(workerSql.queryForObject("SELECT pg_get_constraintdef(oid) FROM pg_constraint WHERE conname='ck_att_resource_owner' AND convalidated",String.class))
                    .contains("provider_qa_case_id","provider_qa_lease_token","job_id","policy_validation_id");
            assertThat(workerSql.queryForObject("SELECT count(1) FROM prior_checksums p JOIN flyway_schema_history f USING(version) WHERE p.checksum IS DISTINCT FROM f.checksum",Integer.class)).isZero();
            Flyway.configure().dataSource(pg.getDatabase("postgres","attachment_fresh"))
                    .locations("classpath:db/migration").load().migrate();
        }
    }
}
