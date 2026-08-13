/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: FlywayMigrationIntegrationTest.java
 * 작성자: 김도훈
 *
 */

package com.saneb.db;

import static org.assertj.core.api.Assertions.assertThat;

import com.saneb.domain.announcementsource.dto.AnnouncementSourceRulePublicationRequest;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceRuleGoldenSetRunRequest;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceRuleReleaseCreateRequest;
import com.saneb.domain.announcementsource.service.AnnouncementSourceRuleReleaseService;
import com.saneb.domain.auth.vo.AuthUserDetailsRow;
import com.saneb.domain.auth.vo.AuthenticatedUserDetails;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.transaction.annotation.Transactional;

@EnabledIfEnvironmentVariable(named = "SANEB_FLYWAY_INTEGRATION", matches = "true")
@SpringBootTest(properties = "spring.main.web-application-type=none")
class FlywayMigrationIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AnnouncementSourceRuleReleaseService ruleReleaseService;

    @Test
    @Transactional
    void initialClassificationDraftPublishesThroughMapperAndServerGoldenGate() {
        UUID actorUserId = UUID.fromString("73000000-0000-0000-0000-000000000001");
        jdbcTemplate.update("""
                INSERT INTO users (id, login_id, password_hash, name, status_code, password_reset_required)
                VALUES (?, ?, ?, ?, 'ACTIVE', false)
                """, actorUserId, "classification-gate-admin", "unused", "분류 검증 관리자");
        UUID releaseId = jdbcTemplate.queryForObject("""
                SELECT id
                FROM announcement_source_classification_rule_releases
                WHERE release_code = 'ASCR-000001'
                """, UUID.class);
        AuthenticatedUserDetails principal = new AuthenticatedUserDetails(
                new AuthUserDetailsRow(
                        actorUserId,
                        "classification-gate-admin",
                        "unused",
                        "분류 검증 관리자",
                        "ACTIVE",
                        false,
                        null,
                        null,
                        null
                ),
                List.of("ADMIN")
        );
        var authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities()
        );

        var firstGoldenRun = ruleReleaseService.insertGoldenSetRun(
                authentication,
                releaseId,
                new AnnouncementSourceRuleGoldenSetRunRequest(0)
        );
        var response = ruleReleaseService.updateRuleReleasePublication(
                authentication,
                releaseId,
                new AnnouncementSourceRulePublicationRequest(
                        0,
                        "PostgreSQL Golden Gate 검증",
                        firstGoldenRun.goldenSetRunId()
                )
        );

        assertThat(response.activeRelease().releaseStatusCode()).isEqualTo("ACTIVE");
        assertThat(response.activeRelease().attachmentAnalysisEnabled()).isFalse();
        assertThat(response.activeRelease().autoActivationEnabled()).isFalse();
        assertThat(response.goldenCaseCount()).isEqualTo(20);
        assertThat(response.goldenSetRunId()).startsWith("GOLDEN-");
        var clonedDraft = ruleReleaseService.insertRuleReleaseDraft(
                authentication,
                new AnnouncementSourceRuleReleaseCreateRequest(
                        response.activeRelease().rowVersion(),
                        "ACTIVE 복제 검증"
                )
        );
        assertThat(clonedDraft.releaseStatusCode()).isEqualTo("DRAFT");
        assertThat(clonedDraft.versionNo()).isEqualTo(2);

        var clonedGoldenRun = ruleReleaseService.insertGoldenSetRun(
                authentication,
                clonedDraft.releaseId(),
                new AnnouncementSourceRuleGoldenSetRunRequest(clonedDraft.rowVersion())
        );
        var clonedPublication = ruleReleaseService.updateRuleReleasePublication(
                authentication,
                clonedDraft.releaseId(),
                new AnnouncementSourceRulePublicationRequest(
                        clonedDraft.rowVersion(),
                        "복제 release 게시 검증",
                        clonedGoldenRun.goldenSetRunId()
                )
        );
        assertThat(clonedPublication.previousRelease().releaseStatusCode()).isEqualTo("RETIRED");
        assertThat(clonedPublication.activeRelease().releaseCode()).isEqualTo("ASCR-000002");
        assertThat(clonedPublication.expectedDecisionChangeCount()).isZero();
        assertThat(jdbcTemplate.queryForObject("""
                SELECT count(1)
                FROM announcement_source_classification_rule_releases
                WHERE release_status_code = 'ACTIVE'
                """, Long.class)).isEqualTo(1L);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT count(1)
                FROM announcement_source_classification_rule_releases
                WHERE release_status_code = 'RETIRED'
                """, Long.class)).isEqualTo(1L);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT count(1)
                FROM announcement_source_classification_keyword_terms
                WHERE term_text = 'R&D'
                  AND normalized_term_text = 'r d'
                """, Long.class)).isEqualTo(2L);
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @throws SQLException 처리 중 예외가 발생한 경우
     */
    @Test
    void mvpMigrationsApplyToPostgreSql() throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            assertThat(selectLong(statement, """
                    select count(1)
                    from flyway_schema_history
                    where version = '1'
                      and success = true
                    """)).isEqualTo(1);
            assertThat(selectLong(statement, """
                    select count(1)
                    from flyway_schema_history
                    where version = '4'
                      and success = true
                    """)).isEqualTo(1);
            assertThat(selectLong(statement, """
                    select count(1)
                    from flyway_schema_history
                    where version = '6'
                      and success = true
                    """)).isEqualTo(1);
            assertThat(selectLong(statement, """
                    select count(1)
                    from flyway_schema_history
                    where version = '7'
                      and success = true
                    """)).isEqualTo(1);
            assertThat(selectLong(statement, """
                    select count(1)
                    from flyway_schema_history
                    where version = '8'
                      and success = true
                    """)).isEqualTo(1);
            assertThat(selectLong(statement, """
                    select count(1)
                    from flyway_schema_history
                    where version = '9'
                      and success = true
                    """)).isEqualTo(1);
            assertThat(selectLong(statement, """
                    select count(1)
                    from flyway_schema_history
                    where version = '10'
                      and success = true
                    """)).isEqualTo(1);
            assertThat(selectLong(statement, """
                    select count(1)
                    from flyway_schema_history
                    where version = '11'
                      and success = true
                    """)).isEqualTo(1);
            assertThat(selectLong(statement, """
                    select count(1)
                    from flyway_schema_history
                    where version = '12'
                      and success = true
                    """)).isEqualTo(1);
            assertThat(selectLong(statement, """
                    select count(1)
                    from flyway_schema_history
                    where version = '13'
                      and success = true
                    """)).isEqualTo(1);
            assertThat(selectLong(statement, """
                    select count(1)
                    from flyway_schema_history
                    where version = '14'
                      and success = true
                    """)).isEqualTo(1);
            assertThat(selectLong(statement, """
                    select count(1)
                    from flyway_schema_history
                    where version = '28'
                      and success = true
                    """)).isEqualTo(1);
            assertThat(selectLong(statement, """
                    select count(1)
                    from flyway_schema_history
                    where version = '29'
                      and success = true
                    """)).isEqualTo(1);
            assertThat(selectLong(statement, """
                    select count(1)
                    from flyway_schema_history
                    where version = '39'
                      and success = true
                    """)).isEqualTo(1);
            assertThat(selectLong(statement, """
                    select count(1)
                    from flyway_schema_history
                    where version = '40'
                      and success = true
                    """)).isEqualTo(1);
            assertThat(selectLong(statement, """
                    select count(1)
                    from flyway_schema_history
                    where version = '41'
                      and success = true
                    """)).isEqualTo(1);
            assertThat(selectLong(statement, """
                    select count(1)
                    from flyway_schema_history
                    where version = '42'
                      and success = true
                    """)).isEqualTo(1);
            assertThat(selectLong(statement, """
                    select count(1)
                    from flyway_schema_history
                    where version = '43'
                      and success = true
                    """)).isEqualTo(1);
            assertThat(selectLong(statement, """
                    select count(1)
                    from flyway_schema_history
                    where version = '44'
                      and success = true
                    """)).isEqualTo(1);
            assertThat(selectLong(statement, """
                    select count(1)
                    from flyway_schema_history
                    where version = '45'
                      and success = true
                    """)).isEqualTo(1);
            for (String version : List.of(
                    "46", "47", "48", "49", "50", "51", "52", "53", "54", "55", "56", "57", "58",
                    "59", "60", "61", "62", "63", "64", "65", "66", "67", "68"
            )) {
                assertThat(selectLong(statement, """
                        select count(1)
                        from flyway_schema_history
                        where version = '%s'
                          and success = true
                        """.formatted(version))).as("Flyway V%s".formatted(version)).isEqualTo(1);
            }

            for (String tableName : selectRequiredTableNames()) {
                assertThat(selectText(statement, "select to_regclass('public." + tableName + "')"))
                        .as(tableName)
                        .isEqualTo(tableName);
            }

            assertThat(selectLong(statement, """
                    SELECT count(1)
                    FROM pg_constraint constraint_row
                    INNER JOIN pg_class table_row ON table_row.oid = constraint_row.conrelid
                    INNER JOIN pg_namespace schema_row ON schema_row.oid = table_row.relnamespace
                    WHERE schema_row.nspname = 'public'
                      AND table_row.relname = 'announcement_source_links'
                      AND constraint_row.conname = 'uq_announcement_source_links_source'
                      AND constraint_row.contype = 'u'
                      AND pg_get_constraintdef(constraint_row.oid) = 'UNIQUE (source_id)'
                    """)).isEqualTo(1);
            assertThat(selectLong(statement, """
                    SELECT count(1)
                    FROM pg_constraint constraint_row
                    INNER JOIN pg_class table_row ON table_row.oid = constraint_row.conrelid
                    INNER JOIN pg_namespace schema_row ON schema_row.oid = table_row.relnamespace
                    WHERE schema_row.nspname = 'public'
                      AND table_row.relname = 'announcement_source_links'
                      AND constraint_row.contype = 'u'
                      AND pg_get_constraintdef(constraint_row.oid) = 'UNIQUE (announcement_id)'
                    """)).isZero();

            assertThat(selectLong(statement, "select count(1) from roles")).isEqualTo(6);
            assertThat(selectLong(statement, "select count(1) from roles where role_code = 'REVIEWER'"))
                    .isEqualTo(1);
            assertThat(selectLong(statement, "select count(1) from consent_versions")).isEqualTo(4);
            assertThat(selectLong(statement, "select count(1) from local_government_notice_sources"))
                    .isEqualTo(244);
            assertThat(selectLong(statement, "select count(1) from local_government_notice_sources where is_enabled = false"))
                    .isEqualTo(54);
            assertThat(selectLong(statement, "select count(1) from local_government_notice_sources where is_enabled = true"))
                    .isEqualTo(190);
            assertThat(selectLong(statement, "select count(distinct sigungu_code) from local_government_notice_sources"))
                    .isEqualTo(244);
            assertThat(selectLong(statement, """
                    select count(1)
                    from local_government_notice_sources
                    where validation_status_code = 'CHECK_REQUIRED'
                    """)).isZero();
            assertThat(selectLong(statement, """
                    select count(1)
                    from local_government_notice_sources
                    where validation_status_code = 'VERIFIED'
                    """)).isEqualTo(244);
            assertThat(selectLong(statement, """
                    select count(1)
                    from local_government_notice_sources
                    where validation_status_code = 'FAILED'
                    """)).isZero();
            assertThat(selectLong(statement, """
                    select count(1)
                    from local_government_notice_sources
                    where parser_profile_code = 'HEURISTIC_NOTICE'
                    """)).isPositive();
            assertThat(selectLong(statement, """
                    select count(1)
                    from local_government_notice_sources
                    where request_profile_code = 'BROWSER_HTTP1'
                    """)).isGreaterThanOrEqualTo(29);
            assertThat(selectLong(statement, """
                    select count(1)
                    from local_government_notice_sources
                    where request_profile_code = 'LEGACY_BROWSER'
                    """)).isEqualTo(6);
            assertThat(selectLong(statement, """
                    select count(1)
                    from local_government_notice_sources
                    where request_profile_code = 'TLS12_BROWSER'
                    """)).isEqualTo(1);
            assertThat(selectLong(statement, """
                    select count(1)
                    from local_government_notice_sources
                    where request_profile_code = 'SESSION_BROWSER'
                    """)).isEqualTo(1);
            assertThat(selectLong(statement, """
                    select count(1)
                    from local_government_notice_parser_profiles
                    where link_strategy_code = 'SAFE_TEMPLATE'
                    """)).isGreaterThanOrEqualTo(32);
            assertThat(selectLong(statement, """
                    select count(1)
                    from local_government_notice_sources
                    where parser_profile_code like 'SAFE_%'
                    """)).isGreaterThanOrEqualTo(59);
            assertThat(selectLong(statement, """
                    select count(1)
                    from local_government_notice_sources
                    where request_method_code = 'POST_FORM'
                      and request_form_json is not null
                    """)).isGreaterThanOrEqualTo(49);
            assertThat(selectText(statement, """
                    select parser_profile_code
                    from local_government_notice_sources
                    where public_code = 'LGS-000009'
                    """)).isEqualTo("SEONGBUK_EMINWON_TABLE");
            assertThat(selectText(statement, """
                    select parser_profile_code
                    from local_government_notice_sources
                    where public_code = 'LGS-000224'
                    """)).isEqualTo("CHANGWON_GOSI_TABLE");
            assertThat(selectLong(statement, """
                    select count(1)
                    from local_government_notice_sources
                    where source_board_type_code = 'LEGAL_NOTICE'
                      and collection_policy_code = 'COLLECT_ALL'
                    """)).isEqualTo(30);
            assertThat(selectLong(statement, """
                    select count(1)
                    from local_government_notice_sources
                    where source_board_type_code = 'GENERAL_NOTICE'
                      and collection_policy_code = 'KEYWORD_FILTERED'
                    """)).isZero();
            assertThat(selectLong(statement, """
                    select count(1)
                    from local_government_notice_sources
                    where source_board_type_code = 'LEGAL_NOTICE'
                      and collection_policy_code = 'KEYWORD_FILTERED'
                    """)).isEqualTo(203);
            assertThat(selectLong(statement, """
                    select count(1)
                    from local_government_notice_sources
                    where source_board_type_code = 'SUPPORT_RECRUITMENT'
                      and collection_policy_code = 'COLLECT_ALL'
                    """)).isEqualTo(10);
            assertThat(selectLong(statement, """
                    select count(1)
                    from local_government_notice_sources
                    where source_board_type_code = 'PRESS_RELEASE'
                      and collection_policy_code = 'EXCLUDED'
                    """)).isEqualTo(1);
            assertThat(selectLong(statement, """
                    select count(1)
                    from local_government_notice_sources
                    where semantic_verification_note like '2026-07-28 공식 고시공고 URL 전수 보정:%'
                    """)).isEqualTo(203);
            assertThat(selectLong(statement, """
                    select count(1)
                    from local_government_notice_sources
                    where semantic_verification_note like '2026-07-28 공식 고시공고 URL 전수 보정:%'
                      and is_enabled = true
                    """)).isEqualTo(190);
            assertThat(selectLong(statement, """
                    select count(1)
                    from local_government_notice_sources
                    where semantic_verification_note like '2026-07-28 공식 고시공고 URL 전수 보정:%'
                      and is_enabled = false
                    """)).isEqualTo(13);
            assertThat(selectLong(statement, """
                    select count(1)
                    from local_government_notice_sources
                    where semantic_verification_note like '2026-07-28 공식 고시공고 URL 전수 보정:%'
                      and is_enabled = true
                      and parser_profile_code = 'MANUAL_ONLY'
                    """)).isZero();
            assertThat(selectText(statement, """
                    select parser_profile_code
                    from local_government_notice_sources
                    where public_code = 'LGS-000089'
                    """)).isEqualTo("SAFE_SAEOL_EMINWON");
            assertThat(selectText(statement, """
                    select notice_url
                    from local_government_notice_sources
                    where public_code = 'LGS-000011'
                    """)).isEqualTo("https://www.dobong.go.kr/Contents.asp?code=10008772");
            assertThat(selectText(statement, """
                    select notice_url
                    from local_government_notice_sources
                    where public_code = 'LGS-000024'
                    """)).isEqualTo("https://www.gangnam.go.kr/notice/list.do?mid=ID05_040201");
            assertThat(selectText(statement, """
                    select notice_url
                    from local_government_notice_sources
                    where public_code = 'LGS-000072'
                    """)).isEqualTo("https://www.donggu.go.kr/dg/kor/contents/916");
            assertThat(selectText(statement, """
                    select notice_url
                    from local_government_notice_sources
                    where public_code = 'LGS-000086'
                    """)).isEqualTo(
                    "https://www.yongin.go.kr/home/yiNw/yiNwStable/yiNwStable02/yiNwStable02_01.jsp"
            );
            assertThat(selectText(statement, """
                    select notice_url
                    from local_government_notice_sources
                    where public_code = 'LGS-000158'
                    """)).isEqualTo(
                    "https://www.seocheon.go.kr/prog/saeolGosi/03/kor/sub04_06_03/list.do"
            );
            assertThat(selectText(statement, """
                    select parser_profile_code
                    from local_government_notice_sources
                    where public_code = 'LGS-000158'
                    """)).isEqualTo("SAFE_SAEOL_EMINWON_CELL");
            assertThat(selectText(statement, """
                    select notice_url
                    from local_government_notice_sources
                    where public_code = 'LGS-000229'
                    """)).startsWith("https://www.miryang.go.kr/web/eMiryangMinwonList.do");
            assertThat(selectText(statement, """
                    select notice_url
                    from local_government_notice_sources
                    where public_code = 'LGS-000239'
                    """)).isEqualTo("https://www.hygn.go.kr/00429/00543/00549.web");
            assertThat(selectText(statement, """
                    select collection_endpoint_url
                    from local_government_notice_sources
                    where public_code = 'LGS-000239'
                    """)).isEqualTo(
                    "https://eminwon.hygn.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do"
            );
            assertThat(selectText(statement, """
                    select parser_profile_code
                    from local_government_notice_sources
                    where public_code = 'LGS-000239'
                    """)).isEqualTo("SAFE_SAEOL_EMINWON");
            assertThat(selectText(statement, """
                    select parser_profile_code
                    from local_government_notice_sources
                    where public_code = 'LGS-000094'
                    """)).isEqualTo("SPRING_BBS");
            assertThat(selectText(statement, """
                    select notice_url
                    from local_government_notice_sources
                    where public_code = 'LGS-000230'
                    """)).isEqualTo(
                    "https://www.geoje.go.kr/index.geoje?menuCd=DOM_000008902001002001&startPage=1"
            );
            assertThat(selectText(statement, """
                    select notice_url
                    from local_government_notice_sources
                    where public_code = 'LGS-000059'
                    """)).isEqualTo(
                    "https://biz.namdong.go.kr/main/news/announce.jsp"
            );

            assertThat(selectLong(statement, "select count(1) from announcement_target_categories"))
                    .isEqualTo(5);
            assertThat(selectLong(statement, "select count(1) from announcement_support_types"))
                    .isEqualTo(7);
            assertThat(selectLong(statement, """
                    select count(1)
                    from announcement_target_category_assignments
                    where is_primary = true
                      and assignment_source_code = 'LEGACY_BACKFILL'
                    """)).isPositive();
            assertThat(selectLong(statement, """
                    select count(1)
                    from announcement_target_category_assignments as assignment
                    join announcements as announcement
                      on announcement.id = assignment.announcement_id
                    join announcement_target_categories as category
                      on category.id = assignment.target_category_id
                    where assignment.is_primary = true
                      and assignment.assignment_source_code = 'LEGACY_BACKFILL'
                      and category.category_code <> announcement.target_type_code
                    """)).isZero();
            assertThat(selectLong(statement, """
                    select count(1)
                    from announcement_source_classification_rule_releases
                    where release_code = 'ASCR-000001'
                      and version_no = 1
                      and release_status_code = 'DRAFT'
                      and combination_operator_code = 'AND'
                      and body_unavailable_action_code = 'REVIEW_REQUIRED'
                      and attachment_analysis_enabled = false
                      and auto_activation_enabled = false
                    """)).isEqualTo(1);
            assertThat(selectLong(statement, """
                    select count(1)
                    from announcement_source_classification_rule_releases
                    where release_status_code = 'ACTIVE'
                    """)).isZero();
            assertThat(selectLong(statement, """
                    select count(1)
                    from announcement_source_classification_rule_groups
                    where release_id = md5('announcement-classification-release-v1')::uuid
                    """)).isEqualTo(23);
            assertThat(selectLong(statement, """
                    select count(1)
                    from announcement_source_classification_keyword_rules as rule
                    join announcement_source_classification_rule_groups as rule_group
                      on rule_group.id = rule.group_id
                    where rule_group.group_code = 'AUTO_EXCLUDE_B_ADMINISTRATIVE'
                      and rule.is_enabled = true
                    """)).isEqualTo(13);
            assertThat(selectLong(statement, """
                    select count(1)
                    from announcement_source_classification_keyword_rules as rule
                    join announcement_source_classification_rule_groups as rule_group
                      on rule_group.id = rule.group_id
                    where rule_group.group_code = 'AUTO_EXCLUDE_B_ADMINISTRATIVE'
                      and rule.is_enabled = false
                    """)).isEqualTo(58);
            assertThat(selectLong(statement, """
                    select count(1)
                    from announcement_source_classification_keyword_terms as term
                    join announcement_source_classification_rule_groups as rule_group
                      on rule_group.id = term.group_id
                    join announcement_source_classification_keyword_rules as rule
                      on rule.id = term.keyword_rule_id
                    where rule_group.group_code = 'AUTO_EXCLUDE_B_ADMINISTRATIVE'
                      and rule.is_enabled = true
                      and term.is_enabled = true
                    """)).isEqualTo(16);
            assertThat(selectLong(statement, """
                    select count(1)
                    from announcement_source_classification_keyword_terms as term
                    join announcement_source_classification_rule_groups as rule_group
                      on rule_group.id = term.group_id
                    where rule_group.group_code = 'AUTO_EXCLUDE_B_ADMINISTRATIVE'
                      and term.normalized_term_text in (
                          '채용공고', '입찰공고', '고시', '의원',
                          '입찰', '용역', '물품구매', '물품 구매'
                      )
                    """)).isZero();
            assertThat(selectLong(statement, """
                    select count(1)
                    from announcement_source_classification_keyword_terms as term
                    join announcement_source_classification_rule_groups as rule_group
                      on rule_group.id = term.group_id
                    where rule_group.group_kind_code = 'PROTECTED_METADATA'
                      and term.is_enabled = true
                      and term.is_classification_term = false
                    """)).isEqualTo(15);
            assertThat(selectLong(statement, """
                    select count(1)
                    from announcement_source_classification_keyword_terms
                    where is_discovery_term = true
                      and is_enabled = true
                    """)).isEqualTo(12);
            assertThat(selectLong(statement, """
                    select count(1)
                    from announcement_source_snapshots as snapshot
                    where not exists (
                        select 1
                        from announcement_source_content_versions as content_version
                        where content_version.source_id = snapshot.id
                          and content_version.raw_hash = snapshot.raw_hash
                    )
                    """)).isZero();
            assertThat(selectLong(statement, """
                    select count(1)
                    from announcement_source_semantic_keyword_rules
                    """)).isEqualTo(43);
            assertThat(selectLong(statement, """
                    select count(1)
                    from announcement_source_collection_requests
                    where data_purpose_code not in ('PRODUCTION', 'QA')
                    """)).isZero();
            assertThat(selectLong(statement, """
                    select count(1)
                    from announcement_source_snapshots
                    where data_purpose_code not in ('PRODUCTION', 'QA')
                    """)).isZero();
        }
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @return 처리 결과
     */
    private List<String> selectRequiredTableNames() {
        return List.of(
                "roles",
                "users",
                "user_roles",
                "auth_login_histories",
                "member_profiles",
                "business_profiles",
                "family_members",
                "partner_profiles",
                "partner_verifications",
                "verification_documents",
                "announcements",
                "announcement_numeric_conditions",
                "matching_cases",
                "matching_result_details",
                "announcement_progress_steps",
                "application_progresses",
                "application_action_logs",
                "audit_logs",
                "announcement_input_requirements",
                "announcement_input_options",
                "application_input_values",
                "consent_versions",
                "user_consents",
                "stored_files",
                "document_submissions",
                "document_submission_reviews",
                "partner_availability_slots",
                "consultation_reservations",
                "consultation_histories",
                "subscription_plans",
                "user_subscriptions",
                "payment_transactions",
                "refund_transactions",
                "payment_provider_events",
                "notification_templates",
                "notification_messages",
                "notification_delivery_logs",
                "operation_tasks",
                "operation_task_comments",
                "operation_task_assignments",
                "report_exports",
                "admin_report_snapshots",
                "ai_assist_requests",
                "ai_assist_results",
                "announcement_source_collection_requests",
                "announcement_source_collection_runs",
                "announcement_source_collection_run_items",
                "announcement_source_snapshots",
                "announcement_source_attachments",
                "announcement_source_highlights",
                "announcement_source_duplicate_candidates",
                "announcement_source_review_histories",
                "announcement_source_links",
                "local_government_notice_parser_profiles",
                "local_government_notice_sources",
                "announcement_source_collection_source_results",
                "announcement_source_snapshot_duplicates",
                "announcement_source_collection_schedules",
                "announcement_source_schedule_executions",
                "announcement_target_categories",
                "announcement_support_types",
                "announcement_target_category_assignments",
                "announcement_support_type_assignments",
                "announcement_source_classification_rule_releases",
                "announcement_source_classification_rule_groups",
                "announcement_source_classification_keyword_rules",
                "announcement_source_classification_keyword_terms",
                "announcement_source_content_versions",
                "announcement_source_classification_evaluations",
                "announcement_source_classification_matches",
                "announcement_source_classification_target_matches",
                "announcement_source_classification_support_matches",
                "announcement_source_confirmed_target_categories",
                "announcement_source_confirmed_support_types"
        );
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param statement 입력 값
     *
     * @param sql 입력 값
     *
     * @return 처리 결과
     *
     * @throws SQLException 처리 중 예외가 발생한 경우
     */
    private long selectLong(Statement statement, String sql) throws SQLException {
        try (ResultSet resultSet = statement.executeQuery(sql)) {
            assertThat(resultSet.next()).isTrue();
            return resultSet.getLong(1);
        }
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param statement 입력 값
     *
     * @param sql 입력 값
     *
     * @return 처리 결과
     *
     * @throws SQLException 처리 중 예외가 발생한 경우
     */
    private String selectText(Statement statement, String sql) throws SQLException {
        try (ResultSet resultSet = statement.executeQuery(sql)) {
            assertThat(resultSet.next()).isTrue();
            return resultSet.getString(1);
        }
    }
}
