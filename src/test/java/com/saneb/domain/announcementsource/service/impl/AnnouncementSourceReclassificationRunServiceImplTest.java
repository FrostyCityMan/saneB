package com.saneb.domain.announcementsource.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.AppliedActionCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.MatchModeCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.RuleGroupKindCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.StrengthCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.SupportTypeCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.TargetCategoryCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.TermTypeCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationRule;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationRuleSet;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationTerm;
import com.saneb.domain.announcementsource.dao.AnnouncementSourceClassificationDao;
import com.saneb.domain.announcementsource.dao.AnnouncementSourceDao;
import com.saneb.domain.announcementsource.dao.AnnouncementSourceReclassificationRunDao;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceReclassificationRunPreviewRequest;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceReclassificationRunResponse;
import com.saneb.domain.announcementsource.service.AnnouncementSourceClassificationPersistenceService;
import com.saneb.domain.announcementsource.service.AnnouncementSourceRuleReleaseService;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceAuditLogCommand;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceContentVersionRow;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceClassificationStateRow;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceReclassificationRunItemRow;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceReclassificationRunRow;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceSnapshotRow;
import com.saneb.domain.auth.vo.AuthUserDetailsRow;
import com.saneb.domain.auth.vo.AuthenticatedUserDetails;
import com.saneb.domain.operation.dao.OperationDao;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

class AnnouncementSourceReclassificationRunServiceImplTest {

    private static final UUID RUN_ID = UUID.fromString("73000000-0000-0000-0000-000000000001");
    private static final UUID RELEASE_ID = UUID.fromString("73000000-0000-0000-0000-000000000002");
    private static final UUID SOURCE_ID = UUID.fromString("73000000-0000-0000-0000-000000000003");
    private static final UUID CONTENT_ID = UUID.fromString("73000000-0000-0000-0000-000000000004");
    private static final UUID ACTOR_ID = UUID.fromString("73000000-0000-0000-0000-000000000005");

    private AnnouncementSourceReclassificationRunDao runDao;
    private AnnouncementSourceClassificationDao classificationDao;
    private AnnouncementSourceDao sourceDao;
    private AnnouncementSourceRuleReleaseService ruleReleaseService;
    private AnnouncementSourceClassificationPersistenceService persistenceService;
    private OperationDao operationDao;
    private AnnouncementSourceReclassificationRunServiceImpl service;

    @BeforeEach
    void setUp() {
        runDao = mock(AnnouncementSourceReclassificationRunDao.class);
        classificationDao = mock(AnnouncementSourceClassificationDao.class);
        sourceDao = mock(AnnouncementSourceDao.class);
        ruleReleaseService = mock(AnnouncementSourceRuleReleaseService.class);
        persistenceService = mock(AnnouncementSourceClassificationPersistenceService.class);
        operationDao = mock(OperationDao.class);
        PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
        when(transactionManager.getTransaction(any(TransactionDefinition.class)))
                .thenReturn(mock(TransactionStatus.class));
        service = new AnnouncementSourceReclassificationRunServiceImpl(
                runDao,
                classificationDao,
                sourceDao,
                ruleReleaseService,
                persistenceService,
                operationDao,
                transactionManager
        );
    }

    @Test
    void createsPreviewScopeWithoutPersistingPlainChangeReason() {
        AnnouncementSourceReclassificationRunPreviewRequest request =
                new AnnouncementSourceReclassificationRunPreviewRequest(
                        RELEASE_ID, "BIZINFO", null, null, false, 500, 50, "운영 영향도 확인"
                );
        when(ruleReleaseService.selectActiveRuleSet(RELEASE_ID)).thenReturn(ruleSet());
        when(runDao.insertRun(any())).thenReturn(1);
        when(runDao.insertRunTargetItems(any())).thenReturn(3);
        when(runDao.updateRunTotalCount(any())).thenReturn(1);
        when(runDao.selectRunDetails(any())).thenReturn(runRow("PREVIEW_PENDING", 3, 1));

        AnnouncementSourceReclassificationRunResponse response =
                service.insertPreviewRun(authentication(), request);

        assertThat(response.totalCount()).isEqualTo(3);
        ArgumentCaptor<AnnouncementSourceAuditLogCommand> audit =
                ArgumentCaptor.forClass(AnnouncementSourceAuditLogCommand.class);
        verify(sourceDao).insertAuditLog(audit.capture());
        assertThat(audit.getValue().metadataJson()).doesNotContain("운영 영향도 확인");
        verify(persistenceService, never()).saveExistingContentEvaluation(
                any(), any(), any(), any(), any(), anyString(), any(Integer.class)
        );
    }

    @Test
    void previewsLegacySourceWithoutCurrentV2Evaluation() {
        AnnouncementSourceReclassificationRunRow run = runRow("PREVIEW_RUNNING", 1, 1);
        AnnouncementSourceReclassificationRunItemRow item = itemRow("PENDING", null, null, null);
        when(runDao.selectNextRunnableRunDetails()).thenReturn(run);
        when(runDao.selectRunItemList(RUN_ID, "PENDING", 50)).thenReturn(List.of(item));
        when(ruleReleaseService.selectPublishedRuleSet(RELEASE_ID)).thenReturn(ruleSet());
        when(sourceDao.selectSourceDetails(SOURCE_ID)).thenReturn(sourceRow("REVIEW_PENDING", 0));
        when(classificationDao.selectLatestContentVersionDetails(SOURCE_ID)).thenReturn(contentRow());
        when(runDao.updateItemPreviewed(eq(item.itemId()), eq("ACCEPTED"), eq("TARGET_SUPPORT_CONFIRMED"), anyString()))
                .thenReturn(1);

        service.insertNextRunBatch();

        verify(runDao).updateItemPreviewed(
                eq(item.itemId()), eq("ACCEPTED"), eq("TARGET_SUPPORT_CONFIRMED"), anyString()
        );
        verify(persistenceService, never()).saveExistingContentEvaluation(
                any(), any(), any(), any(), any(), anyString(), any(Integer.class)
        );
    }

    @Test
    void rollbackRestoresPreviousCurrentEvaluationAndProjectionWithoutDeletingHistory() {
        UUID previousEvaluationId = UUID.fromString("73000000-0000-0000-0000-000000000007");
        UUID appliedEvaluationId = UUID.fromString("73000000-0000-0000-0000-000000000008");
        AnnouncementSourceReclassificationRunRow run = runRow("ROLLBACK_RUNNING", 1, 3);
        AnnouncementSourceReclassificationRunItemRow item =
                itemRow("APPLIED", "d".repeat(64), previousEvaluationId, appliedEvaluationId);
        when(runDao.selectNextRunnableRunDetails()).thenReturn(run);
        when(runDao.selectRunItemList(RUN_ID, "APPLIED", 50)).thenReturn(List.of(item));
        when(classificationDao.selectClassificationStateDetails(SOURCE_ID)).thenReturn(
                new AnnouncementSourceClassificationStateRow(
                        SOURCE_ID, appliedEvaluationId, "EXCLUDED", "TITLE_GROUP_B_MATCHED",
                        "ARCHIVED", 1, 0L, 0L, 0L
                )
        );
        when(runDao.updateAppliedEvaluationNotCurrent(SOURCE_ID, appliedEvaluationId)).thenReturn(1);
        when(runDao.updatePreviousEvaluationCurrent(SOURCE_ID, previousEvaluationId)).thenReturn(1);
        when(runDao.updateSnapshotProjectionRollback(
                SOURCE_ID, "ACCEPTED", "PROVIDER_TRUSTED", null, "REVIEW_PENDING", 1
        )).thenReturn(1);
        when(runDao.updateItemRolledBack(item.itemId())).thenReturn(1);

        service.insertNextRunBatch();

        verify(runDao).updateAppliedEvaluationNotCurrent(SOURCE_ID, appliedEvaluationId);
        verify(runDao).updatePreviousEvaluationCurrent(SOURCE_ID, previousEvaluationId);
        verify(runDao).updateConfirmedTargetCurrentForEvaluation(SOURCE_ID, previousEvaluationId);
        verify(runDao).updateConfirmedSupportCurrentForEvaluation(SOURCE_ID, previousEvaluationId);
        verify(runDao).updateItemRolledBack(item.itemId());
    }

    @Test
    void rollbackCompletionIgnoresEarlierApplyConflicts() {
        AnnouncementSourceReclassificationRunRow running = runRow("ROLLBACK_RUNNING", 1, 4, 1);
        AnnouncementSourceReclassificationRunRow completed = runRow("ROLLBACK_COMPLETED", 1, 5, 1);
        when(runDao.selectNextRunnableRunDetails()).thenReturn(running);
        when(runDao.selectRunItemList(RUN_ID, "APPLIED", 50)).thenReturn(List.of());
        when(runDao.selectRunDetails(RUN_ID)).thenReturn(running, completed);
        when(runDao.selectRunItemStatusCount(
                RUN_ID, List.of("ROLLBACK_CONFLICT", "ROLLBACK_FAILED")
        )).thenReturn(0);
        when(runDao.updateRunStatus(
                RUN_ID, 4, List.of("ROLLBACK_RUNNING"), "ROLLBACK_COMPLETED", null
        )).thenReturn(1);

        service.insertNextRunBatch();

        verify(runDao).updateRunStatus(
                RUN_ID, 4, List.of("ROLLBACK_RUNNING"), "ROLLBACK_COMPLETED", null
        );
    }

    private AnnouncementSourceClassificationRuleSet ruleSet() {
        AnnouncementSourceClassificationRule target = new AnnouncementSourceClassificationRule(
                "TARGET_BUSINESS_001",
                "TARGET_BUSINESS",
                RuleGroupKindCode.TARGET,
                "소상공인",
                StrengthCode.STRONG,
                TargetCategoryCode.BUSINESS,
                null,
                List.of(new AnnouncementSourceClassificationTerm(
                        TermTypeCode.CANONICAL, "소상공인", MatchModeCode.NORMALIZED_PHRASE, true, true
                )),
                true
        );
        AnnouncementSourceClassificationRule support = new AnnouncementSourceClassificationRule(
                "SUPPORT_GRANT_001",
                "SUPPORT_GRANT",
                RuleGroupKindCode.SUPPORT_TYPE,
                "지원금",
                StrengthCode.STRONG,
                null,
                SupportTypeCode.GRANT_SUBSIDY,
                List.of(new AnnouncementSourceClassificationTerm(
                        TermTypeCode.CANONICAL, "지원금", MatchModeCode.NORMALIZED_PHRASE, true, true
                )),
                true
        );
        return new AnnouncementSourceClassificationRuleSet("ACTIVE-1", List.of(target, support));
    }

    private AnnouncementSourceReclassificationRunRow runRow(String status, int totalCount, int rowVersion) {
        return runRow(status, totalCount, rowVersion, 0);
    }

    private AnnouncementSourceReclassificationRunRow runRow(
            String status,
            int totalCount,
            int rowVersion,
            int conflictCount
    ) {
        OffsetDateTime now = OffsetDateTime.parse("2026-08-18T12:00:00+09:00");
        return new AnnouncementSourceReclassificationRunRow(
                RUN_ID, RELEASE_ID, "CLASSIFICATION-V1", "a".repeat(64), status,
                "BIZINFO", null, null, false, 500, 50, totalCount,
                "PREVIEW_RUNNING".equals(status) ? totalCount : 0,
                0, 0, 0, 0, 0, conflictCount, 0, 0,
                ACTOR_ID, rowVersion, now, now, null, null, null
        );
    }

    private AnnouncementSourceReclassificationRunItemRow itemRow(
            String status,
            String predictionHash,
            UUID previousEvaluationId,
            UUID appliedEvaluationId
    ) {
        return new AnnouncementSourceReclassificationRunItemRow(
                UUID.fromString("73000000-0000-0000-0000-000000000006"),
                RUN_ID,
                SOURCE_ID,
                CONTENT_ID,
                "b".repeat(64),
                0,
                previousEvaluationId,
                "ACCEPTED",
                "PROVIDER_TRUSTED",
                null,
                "REVIEW_PENDING",
                null,
                null,
                predictionHash,
                status,
                appliedEvaluationId,
                appliedEvaluationId == null ? null : 1
        );
    }

    private AnnouncementSourceSnapshotRow sourceRow(String reviewStatusCode, int version) {
        OffsetDateTime now = OffsetDateTime.parse("2026-08-18T12:00:00+09:00");
        return new AnnouncementSourceSnapshotRow(
                SOURCE_ID, "SRC-000001", "BIZINFO", "BIZ-1", "소상공인 지원금", "서울시",
                null, null, now, now, "https://example.go.kr/1", null, null, null,
                "COMPLETE", null, "c".repeat(64), reviewStatusCode, "ACCEPTED",
                "PROVIDER_TRUSTED", null, version, now, now, now
        );
    }

    private AnnouncementSourceContentVersionRow contentRow() {
        return new AnnouncementSourceContentVersionRow(
                CONTENT_ID, SOURCE_ID, "소상공인 지원금", "소상공인 지원금 신청 안내",
                "PROVIDER_SUMMARY", "AVAILABLE", "https://example.go.kr/1"
        );
    }

    private UsernamePasswordAuthenticationToken authentication() {
        AuthenticatedUserDetails principal = new AuthenticatedUserDetails(
                new AuthUserDetailsRow(
                        ACTOR_ID, "admin01", "unused", "관리자", "ACTIVE", false,
                        null, null, null
                ),
                List.of("ADMIN")
        );
        return UsernamePasswordAuthenticationToken.authenticated(principal, "unused", principal.getAuthorities());
    }
}
