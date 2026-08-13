package com.saneb.domain.announcementsource.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

import com.saneb.common.error.ApiException;
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
import com.saneb.domain.announcementsource.dto.AnnouncementSourceClassificationDetailsResponse;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceReclassificationRequest;
import com.saneb.domain.announcementsource.service.AnnouncementSourceClassificationManagementService;
import com.saneb.domain.announcementsource.service.AnnouncementSourceClassificationPersistenceService;
import com.saneb.domain.announcementsource.service.AnnouncementSourceRuleReleaseService;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceClassificationStateRow;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceContentVersionRow;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceSnapshotRow;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceAuditLogCommand;
import com.saneb.domain.auth.vo.AuthUserDetailsRow;
import com.saneb.domain.auth.vo.AuthenticatedUserDetails;
import com.saneb.domain.operation.dao.OperationDao;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class AnnouncementSourceReclassificationServiceImplTest {

    private static final UUID SOURCE_ID = UUID.fromString("96000000-0000-0000-0000-000000000001");
    private static final UUID DECISION_ID = UUID.fromString("96000000-0000-0000-0000-000000000002");
    private static final UUID RELEASE_ID = UUID.fromString("96000000-0000-0000-0000-000000000003");
    private static final UUID CONTENT_ID = UUID.fromString("96000000-0000-0000-0000-000000000004");
    private static final UUID EVALUATION_ID = UUID.fromString("96000000-0000-0000-0000-000000000005");

    @Mock private AnnouncementSourceClassificationDao classificationDao;
    @Mock private AnnouncementSourceDao sourceDao;
    @Mock private AnnouncementSourceRuleReleaseService ruleReleaseService;
    @Mock private AnnouncementSourceClassificationPersistenceService persistenceService;
    @Mock private AnnouncementSourceClassificationManagementService managementService;
    @Mock private OperationDao operationDao;

    private AnnouncementSourceReclassificationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AnnouncementSourceReclassificationServiceImpl(
                classificationDao,
                sourceDao,
                ruleReleaseService,
                persistenceService,
                managementService,
                operationDao
        );
    }

    @Test
    void appendsEvaluationWithoutChangingOperationalAnnouncement() {
        AnnouncementSourceReclassificationRequest request = new AnnouncementSourceReclassificationRequest(
                RELEASE_ID, DECISION_ID, 3, "규칙 오탐 보정"
        );
        when(classificationDao.selectClassificationStateDetails(SOURCE_ID))
                .thenReturn(new AnnouncementSourceClassificationStateRow(
                        SOURCE_ID, DECISION_ID, "REVIEW_REQUIRED", "BODY_UNAVAILABLE",
                        "REVIEW_PENDING", 3, 1L, 1L, 0L
                ));
        when(classificationDao.selectLatestContentVersionDetails(SOURCE_ID))
                .thenReturn(new AnnouncementSourceContentVersionRow(
                        CONTENT_ID, SOURCE_ID, "소상공인 지원금", "소상공인 지원금 신청 안내",
                        "PROVIDER_SUMMARY", "AVAILABLE", "https://example.go.kr/detail/1"
                ));
        when(sourceDao.selectSourceDetails(SOURCE_ID)).thenReturn(sourceRow());
        when(ruleReleaseService.selectActiveRuleSet(RELEASE_ID)).thenReturn(ruleSet());
        when(persistenceService.saveExistingContentEvaluation(
                eq(SOURCE_ID), eq(CONTENT_ID), eq(RELEASE_ID), any(), any(), eq("REVIEW_PENDING"), eq(3)
        )).thenReturn(EVALUATION_ID);
        AnnouncementSourceClassificationDetailsResponse response =
                org.mockito.Mockito.mock(AnnouncementSourceClassificationDetailsResponse.class);
        when(managementService.selectClassificationDetails(SOURCE_ID)).thenReturn(response);

        AnnouncementSourceClassificationDetailsResponse actual = service.insertReclassification(
                authentication(), SOURCE_ID, request
        );

        assertThat(actual).isSameAs(response);
        verify(persistenceService).saveExistingContentEvaluation(
                eq(SOURCE_ID), eq(CONTENT_ID), eq(RELEASE_ID), any(), any(), eq("REVIEW_PENDING"), eq(3)
        );
        verify(classificationDao, never()).updateClassificationRowVersion(any(), any(Integer.class));
        ArgumentCaptor<AnnouncementSourceAuditLogCommand> auditCaptor =
                ArgumentCaptor.forClass(AnnouncementSourceAuditLogCommand.class);
        verify(sourceDao).insertAuditLog(auditCaptor.capture());
        assertThat(auditCaptor.getValue().metadataJson())
                .contains(EVALUATION_ID.toString(), "changeReasonHash")
                .doesNotContain("규칙 오탐 보정");
    }

    @Test
    void rejectsStaleDecisionBeforeWriting() {
        when(classificationDao.selectClassificationStateDetails(SOURCE_ID))
                .thenReturn(new AnnouncementSourceClassificationStateRow(
                        SOURCE_ID, UUID.randomUUID(), "ACCEPTED", "TARGET_SUPPORT_CONFIRMED",
                        "REVIEW_PENDING", 3, 0L, 0L, 0L
                ));
        AnnouncementSourceReclassificationRequest request = new AnnouncementSourceReclassificationRequest(
                RELEASE_ID, DECISION_ID, 3, "재분류"
        );

        assertThatThrownBy(() -> service.insertReclassification(authentication(), SOURCE_ID, request))
                .isInstanceOf(ApiException.class);
        verify(classificationDao, never()).updateClassificationRowVersion(any(), any(Integer.class));
        verify(persistenceService, never()).saveExistingContentEvaluation(
                any(), any(), any(), any(), any(), any(), any(Integer.class)
        );
    }

    private AnnouncementSourceClassificationRuleSet ruleSet() {
        AnnouncementSourceClassificationRule target = new AnnouncementSourceClassificationRule(
                "TARGET_BUSINESS_001", "TARGET_BUSINESS", RuleGroupKindCode.TARGET,
                "소상공인", StrengthCode.STRONG, TargetCategoryCode.BUSINESS, null,
                List.of(new AnnouncementSourceClassificationTerm(
                        TermTypeCode.CANONICAL, "소상공인", MatchModeCode.NORMALIZED_PHRASE, true, true
                )), true
        );
        AnnouncementSourceClassificationRule support = new AnnouncementSourceClassificationRule(
                "SUPPORT_GRANT_001", "SUPPORT_GRANT", RuleGroupKindCode.SUPPORT_TYPE,
                "지원금", StrengthCode.STRONG, null, SupportTypeCode.GRANT_SUBSIDY,
                List.of(new AnnouncementSourceClassificationTerm(
                        TermTypeCode.CANONICAL, "지원금", MatchModeCode.NORMALIZED_PHRASE, true, true
                )), true
        );
        return new AnnouncementSourceClassificationRuleSet("ACTIVE-1", List.of(target, support));
    }

    private AnnouncementSourceSnapshotRow sourceRow() {
        OffsetDateTime now = OffsetDateTime.parse("2026-08-12T12:00:00+09:00");
        return new AnnouncementSourceSnapshotRow(
                SOURCE_ID, "SRC-000001", "BIZINFO", "BIZ-1", "소상공인 지원금", "서울시",
                null, null, now, now, "https://example.go.kr/detail/1", null, null, null,
                "COMPLETE", null, "a".repeat(64), "REVIEW_PENDING", "REVIEW_REQUIRED",
                "BODY_UNAVAILABLE", null, 3, now, now, now
        );
    }

    private Authentication authentication() {
        AuthenticatedUserDetails principal = new AuthenticatedUserDetails(
                new AuthUserDetailsRow(
                        UUID.randomUUID(), "admin", "password-hash", "관리자", "ACTIVE",
                        false, null, null, null
                ),
                List.of("ADMIN")
        );
        return new UsernamePasswordAuthenticationToken(principal, "n/a", principal.getAuthorities());
    }
}
