package com.saneb.db;

import static org.assertj.core.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.domain.announcementattachment.extraction.AttachmentRuntimeIdentity;
import com.saneb.common.error.ApiException;
import com.saneb.config.typehandler.UuidTypeHandler;
import com.saneb.domain.announcementattachment.dao.*;
import com.saneb.domain.announcementattachment.dto.*;
import com.saneb.domain.announcementattachment.service.impl.AnnouncementAttachmentBackfillServiceImpl;
import com.saneb.domain.announcementattachment.service.impl.AnnouncementAttachmentBatchServiceImpl;
import com.saneb.domain.announcementattachment.service.impl.AnnouncementAttachmentBackfillSegmentServiceImpl;
import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementattachment.vo.AttachmentBackfillRows;
import com.saneb.domain.announcementsource.dao.AnnouncementSourceDao;
import com.saneb.domain.auth.vo.*;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.time.*;
import java.util.*;
import java.util.function.Supplier;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mybatis.spring.*;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/** 실제 격리 PostgreSQL/Flyway/MyBatis 계약. 환경 미실행은 정적 시험으로 대체하지 않는다. */
@EnabledIfEnvironmentVariable(named="SANEB_ATTACHMENT_MIGRATION_TEST",matches="true")
class AnnouncementAttachmentBackfillIntegrationTest {
    private static EmbeddedPostgres postgres;
    private static JdbcTemplate sql;
    private static TransactionTemplate tx;
    private static TransactionTemplate writeTx;
    private static AnnouncementAttachmentBackfillDao dao;
    private static AnnouncementAttachmentBackfillServiceImpl service;
    private static AnnouncementAttachmentBatchServiceImpl batches;
    private static AnnouncementAttachmentBackfillSegmentServiceImpl segments;
    private static UUID actor,release,policy;
    private static int sequence;
    private static long noticeSequence;
    private OffsetDateTime from;
    @BeforeAll static void start() throws Exception {
        postgres=EmbeddedPostgres.builder().setPort(0).setServerConfig("listen_addresses","127.0.0.1").start();
        try {
            var dataSource=postgres.getPostgresDatabase();
            Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();
            sql=new JdbcTemplate(dataSource);tx=new TransactionTemplate(new DataSourceTransactionManager(dataSource));
            tx.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);tx.setTimeout(30);
            writeTx=new TransactionTemplate(new DataSourceTransactionManager(dataSource));writeTx.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);writeTx.setTimeout(30);
            var factory=new SqlSessionFactoryBean();factory.setDataSource(dataSource);factory.setTypeHandlers(new UuidTypeHandler());
            factory.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath*:mapper/**/*.xml"));
            var template=new SqlSessionTemplate(Objects.requireNonNull(factory.getObject()));
            dao=template.getMapper(AnnouncementAttachmentBackfillDao.class);
            service=new AnnouncementAttachmentBackfillServiceImpl(dao,template.getMapper(AnnouncementAttachmentBatchDao.class),
                    template.getMapper(AnnouncementSourceDao.class),new ObjectMapper().findAndRegisterModules());
            var objectMapper=new ObjectMapper().findAndRegisterModules();var profile=new BizInfoAttachmentDiscoveryProfile();
            batches=new AnnouncementAttachmentBatchServiceImpl(template.getMapper(AnnouncementAttachmentBatchDao.class),template.getMapper(AnnouncementAttachmentJobDao.class),
                    template.getMapper(AnnouncementAttachmentIntakeDao.class),new AttachmentDiscoveryProfileRegistry(List.of(profile)),template.getMapper(AnnouncementSourceDao.class),objectMapper);
            segments=new AnnouncementAttachmentBackfillSegmentServiceImpl(template.getMapper(AnnouncementAttachmentBackfillSegmentDao.class),template.getMapper(AnnouncementAttachmentBatchDao.class),
                    batches,service,template.getMapper(AnnouncementSourceDao.class),objectMapper);
            actor=UUID.randomUUID();policy=UUID.randomUUID();
            sql.update("INSERT INTO users(id,login_id,password_hash,name,status_code,password_reset_required) VALUES (?,?,?,'목록 검증','ACTIVE',false)",actor,"backfill-fixture","unused-fixture-hash");
            release=sql.queryForObject("SELECT id FROM announcement_source_classification_rule_releases ORDER BY version_no LIMIT 1",UUID.class);
            sql.update("UPDATE announcement_source_classification_rule_releases SET release_status_code='ACTIVE',activated_at=now(),rule_snapshot_hash=repeat('c',64) WHERE id=?",release);
            var settings=objectMapper.writeValueAsString(Map.of("engineVersion","attachment-1.0.0","extractorVersion",AttachmentRuntimeIdentity.EXTRACTOR_VERSION,"extractorConfigHash","b".repeat(64),"maximumSourceBytes",83886080L));
            var manifest=objectMapper.writeValueAsString(List.of(Map.of("providerCode","BIZINFO","profileCode",profile.selectProfileCode(),"profileHash",profile.selectProfileHash())));
            sql.update("INSERT INTO announcement_attachment_policies(id,policy_code,version_no,policy_status_code,mode_code,rule_release_id,policy_hash,settings_json,profile_manifest_json,created_by,published_at) VALUES (?, ?,1,'ACTIVE','COLLECT_ONLY',?,repeat('d',64),CAST(? AS jsonb),CAST(? AS jsonb),?,now())",policy,policy.toString(),release,settings,manifest,actor);
        } catch(Exception exception) {postgres.close();postgres=null;throw exception;}
    }
    @AfterAll static void stop() throws Exception {if(postgres!=null)postgres.close();}
    @BeforeEach void scopeDate() {from=OffsetDateTime.parse("2026-06-01T00:00:00Z").plusDays(sequence++);}
    private <T> T transaction(Supplier<T> action) {return tx.execute(status->action.get());}
    private <T> T writeTransaction(Supplier<T> action) {return writeTx.execute(status->action.get());}
    private Authentication auth() {
        var user=new AuthenticatedUserDetails(new AuthUserDetailsRow(actor,"fixture","unused","QA","ACTIVE",false,null,null,null),List.of("ADMIN"));
        return UsernamePasswordAuthenticationToken.authenticated(user,null,user.getAuthorities());
    }
    private AttachmentBackfillRequests.Scope scope(int size) {return new AttachmentBackfillRequests.Scope(policy,List.of("BIZINFO"),from,from.plusDays(1),null,null,size);}
    private AttachmentBackfillRequests.Inventory request(int size) {var scope=scope(size);var preview=transaction(()->service.selectScopePreview(auth(),scope));return new AttachmentBackfillRequests.Inventory(scope,preview.scopeHash(),preview.candidateCount(),"격리 DB 전체 목록 검증");}
    private AttachmentBackfillResponses.Inventory freeze(int size) {var request=request(size);return transaction(()->service.insertInventory(auth(),UUID.randomUUID(),request));}
    private List<UUID> insertSources(int count) {return insertSources(count,"BIZINFO");}
    private List<UUID> insertSources(int count,String provider) {
        var ids=new ArrayList<UUID>();var sources=new ArrayList<Object[]>();var contents=new ArrayList<Object[]>();var bases=new ArrayList<Object[]>();
        for(int i=0;i<count;i++) {
            UUID source=UUID.randomUUID(),content=UUID.randomUUID(),base=UUID.randomUUID();ids.add(source);
            sources.add(new Object[]{source,provider,String.format(Locale.ROOT,"PBLN_%015d",++noticeSequence),source.toString().replace("-","").repeat(2),from.plusHours(1)});
            contents.add(new Object[]{content,source,from.plusHours(1)});bases.add(new Object[]{base,source,content,release});
        }
        sql.batchUpdate("INSERT INTO announcement_source_snapshots(id,provider_code,provider_notice_id,title,raw_hash,collected_at) VALUES (?,?,?,'소상공인 지원금',?,?)",sources);
        sql.batchUpdate("INSERT INTO announcement_source_content_versions(id,source_id,raw_hash,title,body_source_code,body_availability_code,collected_at) VALUES (?,?,repeat('a',64),'소상공인 지원금','NONE','UNAVAILABLE',?)",contents);
        sql.batchUpdate("INSERT INTO announcement_source_classification_evaluations(id,source_id,content_version_id,rule_release_id,engine_version,body_source_code,body_availability_code,title_stage_code,body_stage_code,decision_status_code,reason_code) VALUES (?,?,?,?,'fixture','NONE','UNAVAILABLE','COMBINATION_MATCHED','UNAVAILABLE','REVIEW_REQUIRED','BODY_UNAVAILABLE')",bases);
        return ids;
    }
    @Test void gov24AliasMaterializesActualProviderAndMissingProfileBlocksReservationWithoutLosingCandidates() {
        var sources=insertSources(2,"GOV24_PUBLIC_SERVICE");insertSources(1);
        var scope=new AttachmentBackfillRequests.Scope(policy,List.of("GOV24"),from,from.plusDays(1),null,null,1000);
        var preview=transaction(()->service.selectScopePreview(auth(),scope));
        assertThat(preview.candidateCount()).isEqualTo(2);
        assertThat(preview.counts()).singleElement().satisfies(bucket->{
            assertThat(bucket.providerCode()).isEqualTo("GOV24");assertThat(bucket.count()).isEqualTo(2);
        });
        var request=new AttachmentBackfillRequests.Inventory(scope,preview.scopeHash(),2L,"격리 정부24 계약 검증");UUID key=UUID.randomUUID();
        var run=transaction(()->service.insertInventory(auth(),key,request));
        assertThat(sql.queryForList("SELECT source_id FROM announcement_attachment_backfill_items WHERE run_id=?",UUID.class,run.runId()))
                .containsExactlyInAnyOrderElementsOf(sources);
        assertThat(sql.queryForList("SELECT provider_code FROM announcement_attachment_backfill_items WHERE run_id=?",String.class,run.runId()))
                .containsOnly("GOV24_PUBLIC_SERVICE").hasSize(2);
        var batchScope=new AttachmentBatchRequests.Scope(policy,List.of("GOV24"),from,from.plusDays(1),null,null,1000);
        var direct=transaction(()->batches.selectScopePreview(auth(),batchScope));
        assertThat(direct.candidateCount()).isEqualTo(2);assertThat(direct.canReserve()).isFalse();
        assertThat(direct.items()).allSatisfy(item->assertThat(item.readinessCode()).isEqualTo("PROFILE_REQUIRED"));
        assertThatThrownBy(()->transaction(()->batches.insertBatch(auth(),UUID.randomUUID(),
                new AttachmentBatchRequests.Reservation(batchScope,direct.scopeHash(),"미지원 출처 차단"))))
                .isInstanceOf(ApiException.class).hasMessageContaining("미지원 출처");
        var segment=transaction(()->segments.selectReservationPreview(auth(),run.runId(),1));
        assertThat(segment.readinessCode()).isEqualTo("BATCH_NOT_READY");assertThat(segment.canReserve()).isFalse();
        assertThat(segment.batchPreview().items()).hasSize(2).allSatisfy(item->{
            assertThat(item.providerCode()).isEqualTo("GOV24_PUBLIC_SERVICE");assertThat(item.readinessCode()).isEqualTo("PROFILE_REQUIRED");
        });
        insertSources(1,"GOV24_PUBLIC_SERVICE");
        assertThat(transaction(()->service.insertInventory(auth(),key,request)).runId()).isEqualTo(run.runId());
        assertThat(transaction(()->service.selectRunDetails(auth(),run.runId())).candidateCount()).isEqualTo(2);
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_attachment_jobs j JOIN announcement_source_snapshots s ON s.id=j.source_id WHERE s.collected_at>=? AND s.collected_at<?",Long.class,from,from.plusDays(1))).isZero();
    }
    @Test void all1001CandidatesAreMaterializedExactlyOnceAndNewcomersAreNotAdded() {
        var sources=insertSources(1001);var request=request(1000);UUID key=UUID.randomUUID();
        var run=transaction(()->service.insertInventory(auth(),key,request));
        assertThat(run.candidateCount()).isEqualTo(1001);assertThat(run.segmentCount()).isEqualTo(2);
        var segments=transaction(()->service.selectSegmentList(auth(),run.runId(),1,100));
        assertThat(segments.items()).extracting(AttachmentBackfillRows.Segment::itemCount).containsExactly(1000,1);
        assertThat(sql.queryForList("SELECT source_id FROM announcement_attachment_backfill_items WHERE run_id=?",UUID.class,run.runId())).containsExactlyInAnyOrderElementsOf(sources);
        assertThat(sql.queryForObject("SELECT count(DISTINCT source_id) FROM announcement_attachment_backfill_items WHERE run_id=?",Long.class,run.runId())).isEqualTo(1001);
        var ordered=sql.queryForList("SELECT source_id FROM announcement_attachment_backfill_items WHERE run_id=? ORDER BY ordinal",UUID.class,run.runId());
        assertThat(ordered).containsExactlyElementsOf(sources.stream().sorted(Comparator.comparing(UUID::toString)).toList());
        insertSources(1);
        assertThat(transaction(()->service.insertInventory(auth(),key,request)).runId()).isEqualTo(run.runId());
        assertThat(transaction(()->service.selectRunDetails(auth(),run.runId())).remainingItemCount()).isEqualTo(1001);
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_attachment_jobs j JOIN announcement_source_snapshots s ON s.id=j.source_id WHERE s.collected_at>=? AND s.collected_at<?",Long.class,from,from.plusDays(1))).isZero();
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_source_snapshots WHERE collected_at>=? AND collected_at<? AND (classification_row_version<>0 OR attachment_row_version<>0)",Long.class,from,from.plusDays(1))).isZero();
    }
    @Test void sourceDeletionCascadesIdentityButRetainsOriginalAndEmptySegment() {
        var ids=insertSources(3);var run=freeze(2);
        for(UUID source:ids)sql.update("DELETE FROM announcement_source_snapshots WHERE id=?",source);
        var current=transaction(()->service.selectRunDetails(auth(),run.runId()));
        assertThat(current.candidateCount()).isEqualTo(3);assertThat(current.remainingItemCount()).isZero();assertThat(current.deletedItemCount()).isEqualTo(3);
        assertThat(current.rowVersion()).isEqualTo(3);assertThat(current.statusCode()).isEqualTo("INVENTORIED");
        var segments=transaction(()->service.selectSegmentList(auth(),run.runId(),1,100));assertThat(segments.totalCount()).isEqualTo(2);
        assertThat(segments.items()).allSatisfy(segment->assertThat(segment.remainingItemCount()).isZero());
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_attachment_backfill_items WHERE run_id=?",Long.class,run.runId())).isZero();
    }
    @Test void sourceChangesInvalidatePreviewAndFrozenInputWithoutReplacingCandidate() {
        var source=insertSources(1).getFirst();var request=request(100);long before=dao.selectRunCount();
        sql.update("UPDATE announcement_source_snapshots SET classification_row_version=classification_row_version+1 WHERE id=?",source);
        assertThatThrownBy(()->transaction(()->service.insertInventory(auth(),UUID.randomUUID(),request))).isInstanceOf(ApiException.class).hasMessageContaining("바뀌었습니다");
        assertThat(dao.selectRunCount()).isEqualTo(before);var agencyRun=freeze(100);
        sql.update("UPDATE announcement_source_snapshots SET agency_name='변경 기관' WHERE id=?",source);
        assertThat(transaction(()->service.selectItemList(auth(),agencyRun.runId(),1,1,100)).items().getFirst().currentInputMatches()).isFalse();
        var run=freeze(100);
        sql.update("UPDATE announcement_source_snapshots SET source_url='https://www.bizinfo.go.kr/changed' WHERE id=?",source);
        var items=transaction(()->service.selectItemList(auth(),run.runId(),1,1,100));
        assertThat(items.items()).hasSize(1);assertThat(items.items().getFirst().sourceId()).isEqualTo(source);assertThat(items.items().getFirst().currentInputMatches()).isFalse();
    }
    @Test void immutableRunSegmentItemAndAdditionalMembershipCannotBeChangedDirectly() {
        insertSources(2);var run=freeze(1);
        for(String statement:List.of(
                "UPDATE announcement_attachment_backfill_runs SET deleted_item_count=deleted_item_count+1,row_version=row_version+1 WHERE id=?",
                "DELETE FROM announcement_attachment_backfill_runs WHERE id=?",
                "UPDATE announcement_attachment_backfill_segments SET deleted_item_count=deleted_item_count+1 WHERE run_id=?",
                "DELETE FROM announcement_attachment_backfill_segments WHERE run_id=?",
                "UPDATE announcement_attachment_backfill_items SET segment_no=2 WHERE run_id=?",
                "DELETE FROM announcement_attachment_backfill_items WHERE run_id=?",
                "INSERT INTO announcement_attachment_backfill_segments(run_id,segment_no,item_count) VALUES (?,3,1)"))
            assertThatThrownBy(()->sql.update(statement,run.runId())).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThat(transaction(()->service.selectRunDetails(auth(),run.runId())).remainingItemCount()).isEqualTo(2);
    }
    @Test void incompleteInventoryAndWrongCompositeBindingsRollbackEntireTransaction() {
        insertSources(2);var preview=transaction(()->service.selectScopePreview(auth(),scope(1)));long before=dao.selectRunCount();
        var command=new AttachmentBackfillRows.Insert(UUID.randomUUID(),policy,"{}","{}",preview.scopeHash(),preview.candidateHash(),2L,1,2L,actor,UUID.randomUUID(),"a".repeat(64),"b".repeat(64));
        assertThatThrownBy(()->transaction(()->dao.insertRun(command))).isInstanceOf(RuntimeException.class);
        assertThat(dao.selectRunCount()).isEqualTo(before);
        assertThatThrownBy(()->transaction(()->{
            dao.insertRun(command);dao.insertSegments(new AttachmentBackfillRows.Materialize(command.runId(),scope(1)));
            sql.update("INSERT INTO announcement_attachment_backfill_items(run_id,segment_no,ordinal,source_id,content_version_id,base_evaluation_id,rule_release_id,provider_code,input_hash) SELECT ?,1,1,e.source_id,?,e.id,e.rule_release_id,'BIZINFO',attachment_backfill_input_hash(e.source_id) FROM announcement_source_classification_evaluations e JOIN announcement_source_snapshots s ON s.id=e.source_id WHERE s.collected_at>=? AND s.collected_at<? ORDER BY s.id LIMIT 1",command.runId(),UUID.randomUUID(),from,from.plusDays(1));
            return null;
        })).isInstanceOf(RuntimeException.class);
        assertThat(dao.selectRunCount()).isEqualTo(before);
    }
    @Test void concurrentSameKeyNeverCreatesTwoInventoriesAndAllowsExactReplay() throws Exception {
        insertSources(3);var request=request(2);UUID key=UUID.randomUUID();long before=dao.selectRunCount();
        var start=new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.Callable<UUID> action=()->{
            start.await(5,java.util.concurrent.TimeUnit.SECONDS);
            try {return transaction(()->service.insertInventory(auth(),key,request)).runId();}
            catch(ApiException exception) {assertThat(exception.httpStatus().value()).isEqualTo(409);return null;}
        };
        try(var executor=java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var first=executor.submit(action);var second=executor.submit(action);start.countDown();
            UUID a=first.get(35,java.util.concurrent.TimeUnit.SECONDS),b=second.get(35,java.util.concurrent.TimeUnit.SECONDS);
            assertThat(a!=null || b!=null).isTrue();var replay=transaction(()->service.insertInventory(auth(),key,request));
            if(a!=null)assertThat(replay.runId()).isEqualTo(a);if(b!=null)assertThat(replay.runId()).isEqualTo(b);
            assertThat(dao.selectRunCount()).isEqualTo(before+1);assertThat(replay.candidateCount()).isEqualTo(3);
        }
    }
    @Test void concurrentSourceDeletionPreservesBothCountersWithoutLostUpdates() throws Exception {
        var ids=insertSources(2);var run=freeze(2);var start=new java.util.concurrent.CountDownLatch(1);
        try(var executor=java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var futures=ids.stream().map(source->executor.submit(()->{start.await(5,java.util.concurrent.TimeUnit.SECONDS);
                return sql.update("DELETE FROM announcement_source_snapshots WHERE id=?",source);})).toList();
            start.countDown();for(var future:futures)assertThat(future.get(35,java.util.concurrent.TimeUnit.SECONDS)).isEqualTo(1);
        }
        var current=transaction(()->service.selectRunDetails(auth(),run.runId()));
        assertThat(current.candidateCount()).isEqualTo(2);assertThat(current.deletedItemCount()).isEqualTo(2);assertThat(current.remainingItemCount()).isZero();
        assertThat(current.rowVersion()).isEqualTo(2);
    }
    private AttachmentBackfillSegmentRequests.Reservation segmentRequest(UUID runId,long segmentNo) {
        var preview=transaction(()->segments.selectReservationPreview(auth(),runId,segmentNo));
        assertThat(preview.canReserve()).isTrue();return new AttachmentBackfillSegmentRequests.Reservation(preview.runVersion(),preview.segmentHash(),preview.remainingItemCount(),preview.deletedItemCount(),"격리 분할 예약");
    }
    private AttachmentBackfillSegmentResponses.Reservation reserve(UUID runId,long segmentNo) {
        var request=segmentRequest(runId,segmentNo);return writeTransaction(()->segments.insertReservation(auth(),runId,segmentNo,UUID.randomUUID(),request));
    }
    private AttachmentBatchRequests.Collection collection(UUID batchId) {
        var batch=transaction(()->batches.selectBatchDetails(auth(),batchId));
        return new AttachmentBatchRequests.Collection(batch.rowVersion(),batch.scopeHash(),batch.itemCount(),batch.deletedItemCount(),
                ((Number)batch.frozenScope().get("maximumDownloadBytes")).longValue(),((Number)batch.frozenScope().get("maximumHttpRequests")).longValue(),"격리 DB 수집 승인 검증");
    }
    // V83 이전 Linux 실측 71~82초인 1,001건 전수 계약이다. 독립 실행의 기본 60초 대신 이 사례만 120초로 제한한다.
    // 전체 namespace 600초와 각 transaction 30초, 전수 건수/중복/누락 검증은 유지한다.
    @Test @Timeout(120)
    void full1001InventoryReservesBothSegmentsWithoutDuplicateOrNewArrivalAndAccountsAllDimensions() {
        var sources=insertSources(1001);var run=freeze(1000);insertSources(1);
        var first=reserve(run.runId(),1);var second=reserve(run.runId(),2);
        assertThat(first.reservedItemCount()).isEqualTo(1000);assertThat(second.reservedItemCount()).isEqualTo(1);
        assertThat(sql.queryForList("SELECT j.source_id FROM announcement_attachment_jobs j JOIN announcement_attachment_backfill_segment_batches l ON l.batch_id=j.batch_id WHERE l.run_id=?",UUID.class,run.runId())).containsExactlyInAnyOrderElementsOf(sources);
        var summary=transaction(()->segments.selectSummaryDetails(auth(),run.runId()));
        assertThat(summary.candidateCount()).isEqualTo(1001);assertThat(summary.reservedSegmentCount()).isEqualTo(2);assertThat(summary.unreservedItemCount()).isZero();
        assertThat(summary.collectionCounts()).containsExactlyEntriesOf(Map.of("SCOPE_READY",1001L));
        assertThat(summary.applicationCounts()).containsExactlyEntriesOf(Map.of("NOT_REQUESTED",1001L));assertThat(summary.rollbackCounts()).containsExactlyEntriesOf(Map.of("NOT_REQUESTED",1001L));
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_attachment_jobs j JOIN announcement_attachment_backfill_segment_batches l ON l.batch_id=j.batch_id WHERE l.run_id=? AND (j.attempt_count<>0 OR j.reserved_download_bytes<>0)",Long.class,run.runId())).isZero();
    }
    @Test void orderedMembershipComparisonMatchesPreviousGuardForWholeTuplesNullsDuplicatesAndOrder() throws Exception {
        var a=new LinkedHashMap<String,String>();
        for(String key:List.of("source_id","content_version_id","base_evaluation_id","rule_release_id"))a.put(key,UUID.randomUUID().toString());
        a.put("provider_code","BIZINFO");
        var b=new LinkedHashMap<>(a);b.put("source_id",UUID.randomUUID().toString());
        var expected=List.<Map<String,String>>of(a,b);
        var examples=new ArrayList<List<Map<String,String>>>();
        examples.add(expected);examples.add(List.of(b,a));examples.add(List.of(a));examples.add(List.of(a,a));examples.add(List.of(a,b,b));examples.add(List.of());
        for(String key:a.keySet()) {
            var changed=new LinkedHashMap<>(a);changed.put(key,key.equals("provider_code")?"LOCAL_GOV_NOTICE":UUID.randomUUID().toString());
            examples.add(List.of(changed,b));
            var absent=new LinkedHashMap<>(a);absent.put(key,null);examples.add(List.of(absent,b));
        }
        var migration=new org.springframework.core.io.ClassPathResource("db/migration/V83__compare_backfill_batch_membership_as_ordered_sets.sql")
                .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        // 복사한 기대 로직이 아니라 실제 migration의 비교식을 이전 guard와 대조한다.
        int comparisonStart=migration.indexOf("OR coalesce(")+3;
        var comparison=migration.substring(comparisonStart,migration.indexOf(" THEN",comparisonStart))
                .replace("announcement_attachment_jobs","qa_jobs").replace("announcement_attachment_backfill_items","qa_items")
                .replace("WHERE j.batch_id=batch_key","").replace("WHERE i.run_id=binding.run_id AND i.segment_no=binding.segment_no","");
        var query="""
                -- 공개 원문 없는 합성 tuple로 이전 소속 규칙과 V83 비교식의 동등성을 검사한다.
                WITH qa_jobs AS (
                    SELECT j.source_id,j.content_version_id,j.base_evaluation_id,j.rule_release_id,j.provider_code AS frozen_provider_code
                    FROM jsonb_to_recordset(CAST(? AS jsonb)) AS j(source_id uuid,content_version_id uuid,base_evaluation_id uuid,rule_release_id uuid,provider_code text)
                ), qa_items AS (
                    SELECT i.source_id,i.content_version_id,i.base_evaluation_id,i.rule_release_id,i.provider_code
                    FROM jsonb_to_recordset(CAST(? AS jsonb)) AS i(source_id uuid,content_version_id uuid,base_evaluation_id uuid,rule_release_id uuid,provider_code text)
                )
                SELECT ((SELECT count(1) FROM qa_jobs)=(SELECT count(1) FROM qa_items)
                    AND NOT EXISTS (SELECT 1 FROM qa_jobs j WHERE NOT EXISTS (SELECT 1 FROM qa_items i
                        WHERE i.source_id=j.source_id AND i.content_version_id=j.content_version_id AND i.base_evaluation_id=j.base_evaluation_id
                            AND i.rule_release_id=j.rule_release_id AND i.provider_code=j.frozen_provider_code))
                    AND NOT EXISTS (SELECT 1 FROM qa_items i WHERE NOT EXISTS (SELECT 1 FROM qa_jobs j
                        WHERE j.source_id=i.source_id AND j.content_version_id=i.content_version_id AND j.base_evaluation_id=i.base_evaluation_id AND j.rule_release_id=i.rule_release_id))) AS previous_matches,
                NOT (
                """+comparison+") AS revised_matches";
        var json=new ObjectMapper();
        for(var observed:examples) {
            var result=sql.queryForMap(query,json.writeValueAsString(observed),json.writeValueAsString(expected));
            assertThat(result.get("revised_matches")).isEqualTo(result.get("previous_matches"));
        }
        var empty=sql.queryForMap(query,"[]","[]");
        assertThat(empty).containsEntry("previous_matches",true).containsEntry("revised_matches",true);
    }
    @Test void deletionsBeforeAndAfterReservationKeepWholeDenominatorAndDoNotAllowInitialCollection() {
        var ids=insertSources(3);var run=freeze(3);sql.update("DELETE FROM announcement_source_snapshots WHERE id=?",ids.get(0));
        var receipt=reserve(run.runId(),1);assertThat(receipt.originalItemCount()).isEqualTo(3);assertThat(receipt.reservedItemCount()).isEqualTo(2);assertThat(receipt.deletedBeforeReservation()).isEqualTo(1);
        sql.update("DELETE FROM announcement_source_snapshots WHERE id=?",ids.get(1));
        var summary=transaction(()->segments.selectSummaryDetails(auth(),run.runId()));assertThat(summary.candidateCount()).isEqualTo(3);assertThat(summary.deletedItemCount()).isEqualTo(2);assertThat(summary.remainingItemCount()).isEqualTo(1);
        var request=collection(receipt.batchId());assertThatThrownBy(()->writeTransaction(()->batches.updateCollectionStart(auth(),receipt.batchId(),request))).isInstanceOf(ApiException.class).hasMessageContaining("삭제된 원문");
    }
    @Test void agencyChangeAfterReservationFencesExistingClaimAndCollectionStartWithoutNetwork() {
        var source=insertSources(1).getFirst();var run=freeze(1);var receipt=reserve(run.runId(),1);
        UUID job=sql.queryForObject("SELECT id FROM announcement_attachment_jobs WHERE batch_id=?",UUID.class,receipt.batchId());
        assertThat(sql.queryForObject("SELECT attachment_batch_job_input_unchanged(?)",Boolean.class,job)).isTrue();
        sql.update("UPDATE announcement_source_snapshots SET agency_name='예약 후 기관 변경' WHERE id=?",source);
        assertThat(sql.queryForObject("SELECT attachment_batch_job_input_unchanged(?)",Boolean.class,job)).isFalse();
        var request=collection(receipt.batchId());assertThatThrownBy(()->writeTransaction(()->batches.updateCollectionStart(auth(),receipt.batchId(),request))).isInstanceOf(ApiException.class);
        assertThat(sql.queryForObject("SELECT job_status_code FROM announcement_attachment_jobs WHERE id=?",String.class,job)).isEqualTo("SCOPE_READY");
    }
    @Test void unlinkedMarkedBatchCannotCommitAndReceiptCannotBeDeletedOrReplacedAfterCancellation() {
        var source=insertSources(1).getFirst();var run=freeze(1);var fixed=new com.saneb.domain.announcementattachment.vo.AttachmentBatchRows.FixedScope(run.runId(),1,List.of(source));
        var scope=new AttachmentBatchRequests.Scope(policy,List.of("BIZINFO"),from,from.plusDays(1),null,null,1);
        var preview=transaction(()->batches.selectFixedScopePreview(auth(),scope,fixed));
        assertThatThrownBy(()->writeTransaction(()->batches.insertFixedBatch(auth(),UUID.randomUUID(),new AttachmentBatchRequests.Reservation(scope,preview.scopeHash(),"연결 누락 검증"),fixed))).isInstanceOf(RuntimeException.class);
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_attachment_jobs WHERE source_id=?",Long.class,source)).isZero();
        var receipt=reserve(run.runId(),1);assertThatThrownBy(()->sql.update("DELETE FROM announcement_attachment_backfill_segment_batches WHERE run_id=?",run.runId())).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        writeTransaction(()->batches.updateScopeCancellation(auth(),receipt.batchId(),new AttachmentBatchRequests.Cancellation(0,"취소 검증")));
        var current=transaction(()->segments.selectReservationPreview(auth(),run.runId(),1));assertThat(current.readinessCode()).isEqualTo("ALREADY_RESERVED");assertThat(current.batchId()).isEqualTo(receipt.batchId());
        assertThat(transaction(()->segments.selectSummaryDetails(auth(),run.runId())).collectionCounts()).containsExactlyEntriesOf(Map.of("CANCELLED",1L));
    }
    @Test void concurrentDifferentKeysForOneSegmentProduceExactlyOneBatchAndReplayRemainsAvailable() throws Exception {
        insertSources(2);var run=freeze(2);var request=segmentRequest(run.runId(),1);var start=new java.util.concurrent.CountDownLatch(1);
        UUID firstKey=UUID.randomUUID(),secondKey=UUID.randomUUID();
        try(var executor=java.util.concurrent.Executors.newFixedThreadPool(2)) {
            java.util.function.Function<UUID,java.util.concurrent.Callable<UUID>> action=key->()->{
                start.await(5,java.util.concurrent.TimeUnit.SECONDS);
                try{return writeTransaction(()->segments.insertReservation(auth(),run.runId(),1,key,request)).batchId();}
                catch(ApiException exception){assertThat(exception.httpStatus().value()).isEqualTo(409);return null;}
            };
            var first=executor.submit(action.apply(firstKey));var second=executor.submit(action.apply(secondKey));start.countDown();
            UUID a=first.get(35,java.util.concurrent.TimeUnit.SECONDS),b=second.get(35,java.util.concurrent.TimeUnit.SECONDS);
            assertThat((a==null)!=(b==null)).isTrue();UUID winning=a==null?secondKey:firstKey;
            assertThat(writeTransaction(()->segments.insertReservation(auth(),run.runId(),1,winning,request)).batchId()).isEqualTo(a==null?b:a);
        }
        assertThat(sql.queryForObject("SELECT count(1) FROM announcement_attachment_backfill_segment_batches WHERE run_id=?",Long.class,run.runId())).isEqualTo(1);
        assertThat(transaction(()->segments.selectSummaryDetails(auth(),run.runId())).reservedRemainingItemCount()).isEqualTo(2);
    }
}
