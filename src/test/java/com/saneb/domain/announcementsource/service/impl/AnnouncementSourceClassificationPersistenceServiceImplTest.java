package com.saneb.domain.announcementsource.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.AppliedActionCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.BodyAvailabilityCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.BodySourceCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.BodyStageCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.MatchLocationCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.ReasonCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.RuleGroupKindCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.SemanticStatusCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.SupportTypeCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.TargetCategoryCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.TitleStageCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationMatch;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationResult;
import com.saneb.domain.announcementsource.dao.AnnouncementSourceClassificationDao;
import com.saneb.domain.announcementsource.provider.AnnouncementSourceProviderItem;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnnouncementSourceClassificationPersistenceServiceImplTest {

    @Mock
    private AnnouncementSourceClassificationDao classificationDao;

    private AnnouncementSourceClassificationPersistenceServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AnnouncementSourceClassificationPersistenceServiceImpl(classificationDao);
    }

    @Test
    void savesAllEvidenceAndReturnsEvaluationId() {
        UUID sourceId = UUID.randomUUID();
        UUID contentId = UUID.randomUUID();
        when(classificationDao.selectContentVersionId(eq(sourceId), anyString())).thenReturn(contentId);
        when(classificationDao.insertClassificationMatch(any())).thenReturn(1);
        when(classificationDao.insertClassificationTargetMatch(any())).thenReturn(1);
        when(classificationDao.insertClassificationSupportMatch(any())).thenReturn(1);
        when(classificationDao.updateSnapshotClassificationProjection(
                any(), anyString(), anyString(), anyString(), anyString(), any()
        )).thenReturn(1);

        UUID evaluationId = service.saveNewContentEvaluation(
                sourceId, UUID.randomUUID(), UUID.randomUUID(), item(), result(), "REVIEW_PENDING"
        );

        assertThat(evaluationId).isNotNull();
        verify(classificationDao).insertClassificationEvaluation(any());
        verify(classificationDao).insertClassificationMatch(any());
        verify(classificationDao).insertClassificationTargetMatch(any());
        verify(classificationDao).insertClassificationSupportMatch(any());
    }

    @Test
    void rollsBackWhenMatchedRuleTermCannotBePersisted() {
        UUID sourceId = UUID.randomUUID();
        when(classificationDao.selectContentVersionId(eq(sourceId), anyString())).thenReturn(UUID.randomUUID());
        when(classificationDao.insertClassificationMatch(any())).thenReturn(0);

        assertThatThrownBy(() -> service.saveNewContentEvaluation(
                sourceId, UUID.randomUUID(), UUID.randomUUID(), item(), result(), "REVIEW_PENDING"
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("판정 일치 근거");
    }

    @Test
    void appendsChangedContentAndUsesExpectedSnapshotVersion() {
        UUID sourceId = UUID.randomUUID();
        UUID runId = UUID.randomUUID();
        UUID releaseId = UUID.randomUUID();
        UUID contentId = UUID.randomUUID();
        when(classificationDao.selectContentVersionId(eq(sourceId), anyString())).thenReturn(contentId);
        when(classificationDao.insertClassificationMatch(any())).thenReturn(1);
        when(classificationDao.insertClassificationTargetMatch(any())).thenReturn(1);
        when(classificationDao.insertClassificationSupportMatch(any())).thenReturn(1);
        when(classificationDao.updateSnapshotClassificationProjection(
                any(), anyString(), anyString(), anyString(), anyString(), eq(7)
        )).thenReturn(1);

        UUID evaluationId = service.saveChangedContentEvaluation(
                sourceId, runId, releaseId, item(), result(), "REVIEW_PENDING", 7
        );

        assertThat(evaluationId).isNotNull();
        verify(classificationDao).insertContentVersion(any());
        verify(classificationDao).insertClassificationEvaluation(any());
        verify(classificationDao).updateSnapshotClassificationProjection(
                sourceId,
                "ACCEPTED",
                "TARGET_SUPPORT_CONFIRMED",
                "소상공인",
                "REVIEW_PENDING",
                7
        );
    }

    private AnnouncementSourceProviderItem item() {
        return new AnnouncementSourceProviderItem(
                "BIZINFO", "BIZ-1", "소상공인 지원금", "테스트기관",
                null, null, null, null, "https://example.go.kr/1",
                "소상공인에게 지원금을 지급합니다.", null, null,
                "COMPLETE", null, "{}", "a".repeat(64), List.of(), null
        );
    }

    private AnnouncementSourceClassificationResult result() {
        return new AnnouncementSourceClassificationResult(
                "BIZINFO", "DRAFT-1", SemanticStatusCode.ACCEPTED,
                ReasonCode.TARGET_SUPPORT_CONFIRMED, TitleStageCode.COMBINATION_MATCHED,
                BodyStageCode.COMBINATION_CONFIRMED, BodySourceCode.PROVIDER_SUMMARY,
                BodyAvailabilityCode.AVAILABLE, List.of(TargetCategoryCode.BUSINESS),
                List.of(SupportTypeCode.GRANT_SUBSIDY), List.of(), List.of(),
                List.of(new AnnouncementSourceClassificationMatch(
                        "TARGET_BUSINESS_001", "TARGET_BUSINESS", RuleGroupKindCode.TARGET,
                        "소상공인", "소상공인", "소상공인", MatchLocationCode.TITLE,
                        0, 5, AppliedActionCode.TAG, false
                ))
        );
    }
}
