package com.saneb.domain.announcementsource.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.saneb.domain.announcementsource.dao.AnnouncementSourceClassificationDao;
import com.saneb.domain.announcementsource.dao.AnnouncementSourceDao;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceConfirmedClassificationSaveRequest;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceAuditLogCommand;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceClassificationDetailsRow;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceClassificationStateRow;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceReviewHistoryCommand;
import com.saneb.domain.auth.vo.AuthUserDetailsRow;
import com.saneb.domain.auth.vo.AuthenticatedUserDetails;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class AnnouncementSourceClassificationManagementServiceImplTest {

    private static final UUID SOURCE_ID = UUID.fromString("97000000-0000-0000-0000-000000000001");
    private static final UUID EVALUATION_ID = UUID.fromString("97000000-0000-0000-0000-000000000002");
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-08-12T14:00:00+09:00");

    @Mock private AnnouncementSourceClassificationDao classificationDao;
    @Mock private AnnouncementSourceDao sourceDao;

    private AnnouncementSourceClassificationManagementServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AnnouncementSourceClassificationManagementServiceImpl(classificationDao, sourceDao);
    }

    @Test
    void storesTrimmedReviewNoteOnlyInBusinessHistoryAndAuditsItsFingerprint() throws Exception {
        String originalReviewNote = "  홍길동 대표 자격 확인 / TEST_SECRET_MARKER  ";
        String normalizedReviewNote = originalReviewNote.strip();
        stubSuccessfulSave(
                List.of("PARENT", "BUSINESS"),
                List.of("VOUCHER_BENEFIT", "GENERAL_SUPPORT"),
                List.of("SPOUSE", "BUSINESS"),
                List.of("POLICY_FINANCE", "GRANT_SUBSIDY")
        );

        service.saveConfirmedClassification(
                authentication(),
                SOURCE_ID,
                new AnnouncementSourceConfirmedClassificationSaveRequest(
                        EVALUATION_ID,
                        3,
                        List.of("SPOUSE", "BUSINESS"),
                        List.of("POLICY_FINANCE", "GRANT_SUBSIDY"),
                        originalReviewNote
                )
        );

        ArgumentCaptor<AnnouncementSourceReviewHistoryCommand> historyCaptor =
                ArgumentCaptor.forClass(AnnouncementSourceReviewHistoryCommand.class);
        verify(sourceDao).insertSourceReviewHistory(historyCaptor.capture());
        assertThat(historyCaptor.getValue().previousStatusCode()).isEqualTo("REVIEW_PENDING");
        assertThat(historyCaptor.getValue().nextStatusCode()).isEqualTo("REVIEW_PENDING");
        assertThat(historyCaptor.getValue().reason()).isEqualTo(normalizedReviewNote);

        ArgumentCaptor<AnnouncementSourceAuditLogCommand> auditCaptor =
                ArgumentCaptor.forClass(AnnouncementSourceAuditLogCommand.class);
        verify(sourceDao).insertAuditLog(auditCaptor.capture());
        String metadata = auditCaptor.getValue().metadataJson();
        assertThat(metadata)
                .contains("\"sourceId\":\"" + SOURCE_ID + "\"")
                .contains("\"evaluationId\":\"" + EVALUATION_ID + "\"")
                .contains("\"previousTargetCategoryCodes\":[\"BUSINESS\",\"PARENT\"]")
                .contains("\"nextTargetCategoryCodes\":[\"BUSINESS\",\"SPOUSE\"]")
                .contains("\"previousSupportTypeCodes\":[\"GENERAL_SUPPORT\",\"VOUCHER_BENEFIT\"]")
                .contains("\"nextSupportTypeCodes\":[\"GRANT_SUBSIDY\",\"POLICY_FINANCE\"]")
                .contains("\"reviewNoteProvided\":true")
                .contains("\"reviewNoteLength\":" + normalizedReviewNote.length())
                .contains("\"reviewNoteSha256\":\"" + sha256(normalizedReviewNote) + "\"")
                .doesNotContain(normalizedReviewNote, "홍길동", "TEST_SECRET_MARKER");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   \t\n   "})
    void treatsEmptyOrBlankReviewNoteAsNotProvided(String reviewNote) {
        stubSuccessfulSave(
                List.of("BUSINESS"),
                List.of("GENERAL_SUPPORT"),
                List.of("BUSINESS"),
                List.of("GENERAL_SUPPORT")
        );

        service.saveConfirmedClassification(
                authentication(),
                SOURCE_ID,
                new AnnouncementSourceConfirmedClassificationSaveRequest(
                        EVALUATION_ID,
                        3,
                        List.of("BUSINESS"),
                        List.of("GENERAL_SUPPORT"),
                        reviewNote
                )
        );

        ArgumentCaptor<AnnouncementSourceReviewHistoryCommand> historyCaptor =
                ArgumentCaptor.forClass(AnnouncementSourceReviewHistoryCommand.class);
        verify(sourceDao).insertSourceReviewHistory(historyCaptor.capture());
        assertThat(historyCaptor.getValue().reason()).isNull();

        ArgumentCaptor<AnnouncementSourceAuditLogCommand> auditCaptor =
                ArgumentCaptor.forClass(AnnouncementSourceAuditLogCommand.class);
        verify(sourceDao).insertAuditLog(auditCaptor.capture());
        assertThat(auditCaptor.getValue().metadataJson())
                .contains("\"reviewNoteProvided\":false")
                .contains("\"reviewNoteLength\":0")
                .contains("\"reviewNoteSha256\":null");
    }

    private void stubSuccessfulSave(
            List<String> previousTargetCodes,
            List<String> previousSupportCodes,
            List<String> nextTargetCodes,
            List<String> nextSupportCodes
    ) {
        AnnouncementSourceClassificationStateRow previousState = new AnnouncementSourceClassificationStateRow(
                SOURCE_ID,
                EVALUATION_ID,
                "REVIEW_REQUIRED",
                "BODY_UNAVAILABLE",
                "REVIEW_PENDING",
                3,
                1L,
                1L,
                0L
        );
        AnnouncementSourceClassificationStateRow nextState = new AnnouncementSourceClassificationStateRow(
                SOURCE_ID,
                EVALUATION_ID,
                "REVIEW_REQUIRED",
                "BODY_UNAVAILABLE",
                "REVIEW_PENDING",
                4,
                (long) nextTargetCodes.size(),
                (long) nextSupportCodes.size(),
                0L
        );
        when(classificationDao.selectClassificationStateDetails(SOURCE_ID))
                .thenReturn(previousState, nextState);
        when(classificationDao.selectConfirmedTargetCategoryCodeList(SOURCE_ID))
                .thenReturn(previousTargetCodes, nextTargetCodes);
        when(classificationDao.selectConfirmedSupportTypeCodeList(SOURCE_ID))
                .thenReturn(previousSupportCodes, nextSupportCodes);
        when(classificationDao.updateClassificationRowVersion(SOURCE_ID, 3)).thenReturn(1);
        when(classificationDao.selectClassificationDetails(SOURCE_ID))
                .thenReturn(new AnnouncementSourceClassificationDetailsRow(
                        SOURCE_ID,
                        EVALUATION_ID,
                        "ASCR-000001",
                        "REVIEW_REQUIRED",
                        "BODY_UNAVAILABLE",
                        "COMBINATION_MATCHED",
                        "UNAVAILABLE",
                        "NONE",
                        "UNAVAILABLE",
                        4,
                        NOW
                ));
        when(classificationDao.selectAutomaticTargetCategoryCodeList(SOURCE_ID)).thenReturn(List.of());
        when(classificationDao.selectAutomaticSupportTypeCodeList(SOURCE_ID)).thenReturn(List.of());
        when(classificationDao.selectClassificationMatchList(SOURCE_ID)).thenReturn(List.of());
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

    private String sha256(String value) throws Exception {
        return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))
        );
    }
}
