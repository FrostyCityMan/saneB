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
    @Test void segmentAnalysisBindsFullTextAndRejectsTamperingWithoutChangingFileRole() throws Exception {
        try (var pg=EmbeddedPostgres.builder().setPort(0).setServerConfig("listen_addresses","127.0.0.1").start()) {
            var ds=pg.getPostgresDatabase();
            Flyway.configure().dataSource(ds).locations("classpath:db/migration").load().migrate();
            var sql=new org.springframework.jdbc.core.JdbcTemplate(ds);
            var json=new com.fasterxml.jackson.databind.ObjectMapper();
            UUID actor=UUID.randomUUID(),source=UUID.randomUUID(),content=UUID.randomUUID(),policy=UUID.randomUUID();
            UUID set=UUID.randomUUID(),file=UUID.randomUUID(),extraction=UUID.randomUUID();
            UUID release=sql.queryForObject("SELECT id FROM announcement_source_classification_rule_releases ORDER BY version_no LIMIT 1",UUID.class);
            sql.update("INSERT INTO users(id,login_id,password_hash,name,status_code,password_reset_required) VALUES (?,'segment-probe','unused-fixture-hash','합성 구간 QA','ACTIVE',false)",actor);
            sql.update("INSERT INTO announcement_source_snapshots(id,provider_code,title,raw_hash) VALUES (?,'BIZINFO','합성 구간 검증',repeat('a',64))",source);
            sql.update("INSERT INTO announcement_source_content_versions(id,source_id,raw_hash,title,body_source_code,body_availability_code,collected_at) VALUES (?,?,repeat('a',64),'합성 구간 검증','NONE','UNAVAILABLE',now())",content,source);
            sql.update("INSERT INTO announcement_attachment_policies(id,policy_code,version_no,mode_code,rule_release_id,settings_json,profile_manifest_json,created_by) VALUES (?,'segment-probe',1,'COLLECT_ONLY',?,'{}','[]',?)",policy,release,actor);
            sql.update("INSERT INTO announcement_source_attachment_sets(id,source_id,content_version_id,policy_id,data_purpose_code,profile_hash) VALUES (?,?,?,?,'PRODUCTION',repeat('b',64))",set,source,content,policy);
            sql.update("INSERT INTO announcement_source_attachment_files(id,set_id,source_id,stable_locator_hash,safe_locator_json,sort_order,download_status_code,downloaded_bytes,binary_hash) VALUES (?,?,?,repeat('c',64),'{}',0,'SUCCEEDED',100,repeat('d',64))",file,set,source);
            String text="😀 사업 지원 안내\n지원대상: 소상공인\n지원내용: 지원금\n신청기간: 9월\n지원 신청서\n성 명\n(서명 또는 인)";
            var blocks=java.util.List.of(new com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence.Block(0,0,text.codePointCount(0,text.length()),"p:0",true,"p:0"));
            var analysis=new com.saneb.domain.announcementattachment.classification.AttachmentSegmentRoleAnalyzer().selectAnalysis(
                    new com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence.Extraction("COMPLETE_TEXT",text,blocks,1,1));
            sql.update("INSERT INTO announcement_source_attachment_extractions(id,file_id,set_id,source_id,attempt_no,extractor_code,extractor_version,extractor_config_hash,quality_code,extracted_text,text_hash,blocks_json,character_count,duration_ms) VALUES (?,?,?,?,1,'HWPX','1.0.3',repeat('f',64),'COMPLETE_TEXT',?,encode(digest(?,'sha256'),'hex'),CAST(? AS jsonb),char_length(?),1)",extraction,file,set,source,text,text,json.writeValueAsString(blocks),text);
            String insert="INSERT INTO announcement_attachment_segment_analyses(extraction_id,file_id,set_id,source_id,analysis_version,rules_hash,text_hash,blocks_hash,analysis_json) VALUES (?,?,?,?,?,?,?,?,CAST(? AS jsonb))";
            String valid=json.writeValueAsString(analysis);
            assertThatThrownBy(()->sql.update(insert,extraction,file,set,source,analysis.analysisVersion(),analysis.rulesHash(),analysis.textHash(),analysis.blocksHash(),valid))
                    .hasRootCauseInstanceOf(SQLException.class);
            sql.update("UPDATE announcement_source_attachment_sets SET set_status_code='SEALED',discovery_status_code='FOUND',is_discovery_complete=true,discovered_count=1,processed_count=1,manifest_hash=repeat('e',64),sealed_at=now() WHERE id=?",set);
            // 유일성 위반에 기대지 않고 각 위조 입력을 먼저 거부하는지 검증한다.
            for (String mutation:java.util.List.of("hash","gap","overlap","end","role","evidence","raw","source","file")) {
                var value=(com.fasterxml.jackson.databind.node.ObjectNode)json.readTree(valid);
                var first=(com.fasterxml.jackson.databind.node.ObjectNode)value.path("segments").get(0);
                var second=(com.fasterxml.jackson.databind.node.ObjectNode)value.path("segments").get(1);
                switch(mutation) {
                    case "hash" -> value.put("textHash","0".repeat(64));
                    case "gap" -> second.put("startOffset",second.path("startOffset").asInt()+1);
                    case "overlap" -> second.put("startOffset",second.path("startOffset").asInt()-1);
                    case "end" -> second.put("endOffset",analysis.textLength()-1);
                    case "role" -> first.put("roleCode","UNKNOWN");
                    case "evidence" -> ((com.fasterxml.jackson.databind.node.ObjectNode)first.path("evidence").get(0)).put("endOffset",analysis.textLength()+1);
                    case "raw" -> value.put("rawText","SYNTHETIC_CANARY");
                    default -> { }
                }
                assertThatThrownBy(()->sql.update(insert,extraction,mutation.equals("file")?UUID.randomUUID():file,set,
                        mutation.equals("source")?UUID.randomUUID():source,analysis.analysisVersion(),analysis.rulesHash(),analysis.textHash(),analysis.blocksHash(),value.toString()))
                        .as("구간 위조 거부: %s",mutation).hasRootCauseInstanceOf(SQLException.class);
            }
            assertThat(sql.update(insert,extraction,file,set,source,analysis.analysisVersion(),analysis.rulesHash(),analysis.textHash(),analysis.blocksHash(),valid)).isEqualTo(1);
            var configuration=new org.apache.ibatis.session.Configuration(new org.apache.ibatis.mapping.Environment("segment-qa",
                    new org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory(),ds));
            configuration.getTypeHandlerRegistry().register(UUID.class,com.saneb.config.typehandler.UuidTypeHandler.class);
            var resource=new org.springframework.core.io.ClassPathResource("mapper/announcementattachment/AnnouncementAttachmentSegmentMapper.xml");
            try(var stream=resource.getInputStream()) {
                new org.apache.ibatis.builder.xml.XMLMapperBuilder(stream,configuration,resource.getPath(),configuration.getSqlFragments()).parse();
            }
            try(var session=new org.apache.ibatis.session.SqlSessionFactoryBuilder().build(configuration).openSession(true)) {
                var mapper=session.getMapper(com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentSegmentDao.class);
                var saved=mapper.selectAnalysisDetails(source,extraction,analysis.analysisVersion(),analysis.rulesHash());
                assertThat(saved.extractionId()).isEqualTo(extraction);
                assertThat(json.readTree(saved.analysisJson())).isEqualTo(json.readTree(valid));
                assertThat(mapper.selectAnalysisDetails(UUID.randomUUID(),extraction,analysis.analysisVersion(),analysis.rulesHash())).isNull();
                assertThat(mapper.insertAnalysis(new com.saneb.domain.announcementattachment.vo.AttachmentSegmentRows.Insert(UUID.randomUUID(),source,set,file,extraction,
                        analysis.analysisVersion(),analysis.rulesHash(),analysis.textHash(),analysis.blocksHash(),valid))).isZero();
                // 현재 제목 통과 평가가 없는 원문은 SHADOW 분석 운영 API에도 노출하지 않는다.
                assertThat(mapper.selectExtractionDetails(source,extraction)).isNull();
                sql.update("INSERT INTO announcement_source_classification_evaluations(source_id,content_version_id,rule_release_id,engine_version,body_source_code,body_availability_code,title_stage_code,body_stage_code,decision_status_code,reason_code) VALUES (?,?,?,'segment-fixture','NONE','UNAVAILABLE','COMBINATION_MATCHED','UNAVAILABLE','REVIEW_REQUIRED','BODY_UNAVAILABLE')",source,content,release);
                var input=mapper.selectExtractionDetails(source,extraction);
                assertThat(input.extractedText()).isEqualTo(text);
                assertThat(input.fileId()).isEqualTo(file);
                var audits=org.mockito.Mockito.mock(com.saneb.domain.announcementsource.dao.AnnouncementSourceDao.class);
                var service=new com.saneb.domain.announcementattachment.service.impl.AnnouncementAttachmentSegmentServiceImpl(mapper,audits,json);
                var response=service.selectAnalysisDetails(source,extraction);
                assertThat(response.analysis()).isEqualTo(analysis);
                assertThat(response.applicationMode()).isEqualTo("SHADOW");
                org.mockito.Mockito.verifyNoInteractions(audits);
            }
            assertThatThrownBy(()->sql.update(insert,extraction,file,set,source,analysis.analysisVersion(),analysis.rulesHash(),analysis.textHash(),analysis.blocksHash(),valid)).hasRootCauseInstanceOf(SQLException.class);
            assertThatThrownBy(()->sql.update("UPDATE announcement_attachment_segment_analyses SET analysis_json=analysis_json WHERE extraction_id=?",extraction)).hasRootCauseInstanceOf(SQLException.class);
            assertThatThrownBy(()->sql.update("DELETE FROM announcement_attachment_segment_analyses WHERE extraction_id=?",extraction)).hasRootCauseInstanceOf(SQLException.class);
            assertThat(sql.queryForObject("SELECT document_role_code FROM announcement_source_attachment_files WHERE id=?",String.class,file)).isEqualTo("UNKNOWN");
            assertThat(sql.queryForObject("SELECT count(1) FROM announcement_source_attachment_evaluations WHERE source_id=?",Integer.class,source)).isZero();
            // 기존 원문 삭제의 cascade는 새로운 분석 행 때문에 차단되면 안 된다.
            sql.update("DELETE FROM announcement_source_snapshots WHERE id=?",source);
            assertThat(sql.queryForObject("SELECT count(1) FROM announcement_attachment_segment_analyses WHERE source_id=?",Integer.class,source)).isZero();
        }
    }
    @Test void textRoleEvidenceRejectsForgedHashesPositionsVersionsAndPreservesCodePointBinding() throws Exception {
        try(var pg=EmbeddedPostgres.builder().setPort(0).setServerConfig("listen_addresses","127.0.0.1").start()) {
            var ds=pg.getPostgresDatabase();Flyway.configure().dataSource(ds).locations("classpath:db/migration").load().migrate();
            var sql=new org.springframework.jdbc.core.JdbcTemplate(ds);
            var tx=new org.springframework.transaction.support.TransactionTemplate(new org.springframework.jdbc.datasource.DataSourceTransactionManager(ds));
            var json=new com.fasterxml.jackson.databind.ObjectMapper();
            UUID actor=UUID.randomUUID(),source=UUID.randomUUID(),content=UUID.randomUUID(),policy=UUID.randomUUID();
            UUID release=sql.queryForObject("SELECT id FROM announcement_source_classification_rule_releases ORDER BY version_no LIMIT 1",UUID.class);
            sql.update("INSERT INTO users(id,login_id,password_hash,name,status_code,password_reset_required) VALUES (?,'role-probe','unused-fixture-hash','합성 역할 QA','ACTIVE',false)",actor);
            sql.update("INSERT INTO announcement_source_snapshots(id,provider_code,title,raw_hash) VALUES (?,'BIZINFO','합성 역할 검증',repeat('a',64))",source);
            sql.update("INSERT INTO announcement_source_content_versions(id,source_id,raw_hash,title,body_source_code,body_availability_code,collected_at) VALUES (?,?,repeat('a',64),'합성 역할 검증','NONE','UNAVAILABLE',now())",content,source);
            String text="😀 지원사업 공고\n지원대상: 소상공인\n지원내용: 지원금\n신청기간: 9월";
            var block=new com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence.Block(0,0,text.codePointCount(0,text.length()),"한글문단😀".repeat(15),true,"첫 페이지😀");
            var extraction=new com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence.Extraction("COMPLETE_TEXT",text,java.util.List.of(block),1,1);
            var assessment=new com.saneb.domain.announcementattachment.classification.AttachmentDocumentRoleClassifier().selectAssessment(extraction);
            String settings=json.writeValueAsString(java.util.Map.of("roleRuleVersion",assessment.ruleVersion(),"roleRulesHash",assessment.rulesHash()));
            sql.update("INSERT INTO announcement_attachment_policies(id,policy_code,version_no,mode_code,rule_release_id,settings_json,profile_manifest_json,created_by) VALUES (?,'role-probe',1,'COLLECT_ONLY',?,CAST(? AS jsonb),'[]',?)",policy,release,settings,actor);
            String blocks=json.writeValueAsString(extraction.blocks()),valid=json.writeValueAsString(assessment);
            assertThat(sql.queryForObject("SELECT attachment_role_blocks_hash(CAST(? AS jsonb))",String.class,blocks)).isEqualTo(assessment.blocksHash());
            UUID file=tx.execute(status->insertRoleProbe(sql,source,content,policy,text,blocks,valid));
            assertThat(sql.queryForObject("SELECT role_origin_code FROM announcement_source_attachment_files WHERE id=?",String.class,file)).isEqualTo("TEXT_RULE");
            assertThatThrownBy(()->sql.update("UPDATE announcement_source_attachment_files SET document_role_code='FORM' WHERE id=?",file))
                    .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
            for(String key:java.util.List.of("textHash","blocksHash","rulesHash","ruleVersion")) {
                var changed=(com.fasterxml.jackson.databind.node.ObjectNode)json.readTree(valid);
                changed.put(key,key.equals("ruleVersion")?"other-version":"f".repeat(64));
                assertThatThrownBy(()->tx.execute(status->insertRoleProbe(sql,source,content,policy,text,blocks,changed.toString())))
                        .hasRootCauseInstanceOf(SQLException.class);
            }
            var wrongOffset=(com.fasterxml.jackson.databind.node.ObjectNode)json.readTree(valid);
            ((com.fasterxml.jackson.databind.node.ObjectNode)wrongOffset.path("evidence").get(0)).put("endOffset",text.codePointCount(0,text.length())+1);
            assertThatThrownBy(()->tx.execute(status->insertRoleProbe(sql,source,content,policy,text,blocks,wrongOffset.toString())))
                    .hasRootCauseInstanceOf(SQLException.class);
            var extra=(com.fasterxml.jackson.databind.node.ObjectNode)json.readTree(valid);extra.put("rawText","PRIVATE_CANARY");
            assertThatThrownBy(()->tx.execute(status->insertRoleProbe(sql,source,content,policy,text,blocks,extra.toString())))
                    .hasRootCauseInstanceOf(SQLException.class);
            sql.update("DELETE FROM announcement_source_snapshots WHERE id=?",source);
            assertThat(sql.queryForObject("SELECT count(1) FROM announcement_source_attachment_files WHERE source_id=?",Integer.class,source)).isZero();
        }
    }
    private static UUID insertRoleProbe(org.springframework.jdbc.core.JdbcTemplate sql,UUID source,UUID content,UUID policy,String text,String blocks,String assessment) {
        UUID set=UUID.randomUUID(),file=UUID.randomUUID(),extraction=UUID.randomUUID();
        sql.update("INSERT INTO announcement_source_attachment_sets(id,source_id,content_version_id,policy_id,data_purpose_code,profile_hash) VALUES (?,?,?,?,'PRODUCTION',repeat('b',64))",set,source,content,policy);
        sql.update("INSERT INTO announcement_source_attachment_files(id,set_id,source_id,stable_locator_hash,safe_locator_json,sort_order,document_role_code,role_origin_code,download_status_code,downloaded_bytes,binary_hash,role_extraction_id,role_assessment_json) VALUES (?,?,?,repeat('c',64),'{}',0,'NOTICE','TEXT_RULE','SUCCEEDED',100,repeat('d',64),?,CAST(? AS jsonb))",file,set,source,extraction,assessment);
        sql.update("INSERT INTO announcement_source_attachment_extractions(id,file_id,set_id,source_id,attempt_no,extractor_code,extractor_version,extractor_config_hash,quality_code,extracted_text,text_hash,blocks_json,character_count,duration_ms) VALUES (?,?,?,?,1,'HWPX','1.0.0',repeat('f',64),'COMPLETE_TEXT',?,encode(digest(?,'sha256'),'hex'),CAST(? AS jsonb),char_length(?),1)",extraction,file,set,source,text,text,blocks,text);
        return file;
    }
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
            var legacySql = new org.springframework.jdbc.core.JdbcTemplate(dataSource);
            insertLegacyBusinessFixtures(legacySql);
            assertLegacyAttachmentCheck(legacySql);
            var legacySnapshots = selectLegacySnapshots(legacySql);
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
            var roleUpgrade=Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").target("82").load();
            assertThat(roleUpgrade.migrate().migrationsExecuted).isEqualTo(1);roleUpgrade.validate();
            assertThat(workerSql.queryForObject("SELECT count(1) FROM information_schema.columns WHERE table_schema='public' AND table_name='announcement_source_attachment_files' AND column_name IN ('role_extraction_id','role_assessment_json')",Integer.class)).isEqualTo(2);
            assertThat(workerSql.queryForObject("SELECT count(1) FROM pg_constraint WHERE conname='fk_att_file_role_extract' AND convalidated AND condeferrable AND condeferred",Integer.class)).isEqualTo(1);
            assertThat(workerSql.queryForObject("SELECT count(1) FROM announcement_attachment_policies WHERE policy_status_code='ACTIVE'",Integer.class)).isZero();
            assertThat(workerSql.queryForObject("SELECT count(1) FROM prior_checksums p JOIN flyway_schema_history f USING(version) WHERE p.checksum IS DISTINCT FROM f.checksum",Integer.class)).isZero();
            var membershipUpgrade=Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").target("83").load();
            assertThat(membershipUpgrade.migrate().migrationsExecuted).isEqualTo(1);membershipUpgrade.validate();
            assertThat(workerSql.queryForObject("SELECT pg_get_functiondef('check_attachment_backfill_segment_batch'::regproc)",String.class))
                    .contains("jsonb_agg","IS DISTINCT FROM","frozen_provider_code");
            assertThat(workerSql.queryForObject("SELECT count(1) FROM pg_trigger WHERE tgname IN ('ct_att_backfill_segment_batch','ct_att_backfill_marked_batch','ct_att_backfill_fixed_job') AND tgdeferrable AND tginitdeferred AND tgenabled='O'",Integer.class)).isEqualTo(3);
            var segmentUpgrade=Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").target("84").load();
            assertThat(segmentUpgrade.migrate().migrationsExecuted).isEqualTo(1);segmentUpgrade.validate();
            assertThat(workerSql.queryForObject("SELECT count(1) FROM announcement_attachment_segment_analyses",Integer.class)).isZero();
            assertThat(workerSql.queryForObject("SELECT count(1) FROM prior_checksums p JOIN flyway_schema_history f USING(version) WHERE p.checksum IS DISTINCT FROM f.checksum",Integer.class)).isZero();
            assertLegacyAttachmentCheck(workerSql);
            for (var snapshot : legacySnapshots) {
                assertThat(workerSql.queryForObject(snapshot.query(), String.class))
                        .as("V71 기존 컬럼/전체 행 보존: %s", snapshot.table()).isEqualTo(snapshot.hash());
            }
            var freshDataSource = pg.getDatabase("postgres","attachment_fresh");
            var freshFlyway = Flyway.configure().dataSource(freshDataSource)
                    .locations("classpath:db/migration").load();
            freshFlyway.migrate();
            freshFlyway.validate();
            assertLegacyAttachmentCheck(new org.springframework.jdbc.core.JdbcTemplate(freshDataSource));
        }
    }

    private static void insertLegacyBusinessFixtures(org.springframework.jdbc.core.JdbcTemplate sql) {
        // 이 메서드는 새로 생성한 loopback 시험 DB에만 합성 업무 데이터를 넣는다.
        UUID actor = UUID.randomUUID(), announcement = UUID.randomUUID();
        UUID release = sql.queryForObject("SELECT id FROM announcement_source_classification_rule_releases ORDER BY version_no LIMIT 1", UUID.class);
        sql.update("INSERT INTO users(id,login_id,password_hash,name,status_code,password_reset_required) VALUES (?,'migration-preservation','unused-fixture-hash','마이그레이션 합성 담당자','ACTIVE',false)", actor);
        sql.update("INSERT INTO announcements(id,target_type_code,title,agency_name,manual_status_code,approval_status_code) VALUES (?,'BUSINESS','합성 지원 공고','합성 기관','HIDDEN','DRAFT')", announcement);
        for (boolean available : java.util.List.of(true, false)) {
            UUID source = UUID.randomUUID(), content = UUID.randomUUID();
            sql.update("INSERT INTO announcement_source_snapshots(id,provider_code,title,raw_hash) VALUES (?,'BIZINFO','합성 소상공인 지원금',?)", source, source.toString().replace("-", "").repeat(2));
            sql.update("INSERT INTO announcement_source_content_versions(id,source_id,raw_hash,title,body_text,body_source_code,body_availability_code,collected_at) VALUES (?,?,repeat('a',64),'합성 소상공인 지원금',?,?,?,now())",
                    content, source, available ? "소상공인 대상 경영 지원금 합성 본문" : null,
                    available ? "DETAIL_PAGE_TEXT" : "NONE", available ? "AVAILABLE" : "UNAVAILABLE");
            sql.update("INSERT INTO announcement_source_classification_evaluations(id,source_id,content_version_id,rule_release_id,engine_version,body_source_code,body_availability_code,title_stage_code,body_stage_code,decision_status_code,reason_code) VALUES (?,?,?,?,'migration-fixture',?,?,'COMBINATION_MATCHED',?,?,?)",
                    UUID.randomUUID(), source, content, release, available ? "DETAIL_PAGE_TEXT" : "NONE",
                    available ? "AVAILABLE" : "UNAVAILABLE", available ? "COMBINATION_CONFIRMED" : "UNAVAILABLE",
                    available ? "ACCEPTED" : "REVIEW_REQUIRED", available ? "TARGET_SUPPORT_CONFIRMED" : "BODY_UNAVAILABLE");
            if (available) sql.update("INSERT INTO announcement_source_links(id,source_id,announcement_id,linked_by) VALUES (?,?,?,?)", UUID.randomUUID(), source, announcement, actor);
        }
    }

    private static java.util.List<LegacySnapshot> selectLegacySnapshots(org.springframework.jdbc.core.JdbcTemplate sql) {
        var snapshots = new java.util.ArrayList<LegacySnapshot>();
        for (String table : java.util.List.of("users", "announcements", "announcement_source_snapshots",
                "announcement_source_content_versions", "announcement_source_classification_evaluations",
                "announcement_source_links", "announcement_source_classification_rule_releases",
                "announcement_source_classification_rule_groups", "announcement_source_classification_keyword_rules",
                "announcement_source_classification_keyword_terms")) {
            // allowlist 테이블의 V71 컬럼만 고정한다. 이후 additive 컬럼은 기존 데이터와 분리한다.
            var columns = sql.queryForList("SELECT column_name FROM information_schema.columns WHERE table_schema='public' AND table_name=? ORDER BY ordinal_position", String.class, table);
            assertThat(columns).isNotEmpty().allMatch(name -> name.matches("[a-z][a-z0-9_]*"));
            String projection = columns.stream().map(name -> "\"" + name + "\"").collect(java.util.stream.Collectors.joining(","));
            String query = "SELECT encode(digest(coalesce(string_agg(to_jsonb(legacy)::text, E'\\n' ORDER BY legacy.id),'[]'),'sha256'),'hex') FROM (SELECT "
                    + projection + " FROM " + table + ") legacy";
            assertThat(sql.queryForObject("SELECT count(1) FROM " + table, Integer.class)).as(table).isPositive();
            snapshots.add(new LegacySnapshot(table, query, sql.queryForObject(query, String.class)));
        }
        return snapshots;
    }

    private static void assertLegacyAttachmentCheck(org.springframework.jdbc.core.JdbcTemplate sql) {
        // false 값 존재뿐 아니라 INSERT/UPDATE 양쪽을 PostgreSQL CHECK가 거부하는지 확인한다.
        for (String statement : java.util.List.of(
                "INSERT INTO announcement_source_classification_rule_releases(release_code,version_no,attachment_analysis_enabled) VALUES ('migration-forbidden',9001,true)",
                "UPDATE announcement_source_classification_rule_releases SET attachment_analysis_enabled=true WHERE version_no=1")) {
            assertThatThrownBy(() -> sql.update(statement)).satisfies(error -> {
                Throwable cause = org.springframework.core.NestedExceptionUtils.getMostSpecificCause(error);
                assertThat(cause).isInstanceOf(SQLException.class);
                assertThat(((SQLException) cause).getSQLState()).isEqualTo("23514");
                assertThat(cause.getMessage()).contains("ck_announcement_source_classification_releases_attachment");
            });
        }
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_source_classification_rule_releases WHERE attachment_analysis_enabled", Integer.class)).isZero();
    }

    private record LegacySnapshot(String table, String query, String hash) { }
}
