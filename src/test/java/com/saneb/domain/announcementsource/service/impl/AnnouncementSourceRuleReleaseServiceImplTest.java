package com.saneb.domain.announcementsource.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.saneb.common.error.ApiException;
import com.saneb.common.error.ErrorCode;
import com.saneb.domain.announcementsource.dao.AnnouncementSourceRuleReleaseDao;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceKeywordRuleSaveRequest;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceKeywordRuleStatusUpdateRequest;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceRulePublicationRequest;
import com.saneb.domain.announcementsource.service.impl.AnnouncementSourceRuleGoldenGate.GoldenGateResult;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceAuditLogCommand;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceKeywordRuleRow;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceKeywordRuleUpdateCommand;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceKeywordTermInsertCommand;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceRuleGroupRow;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceRuleReleaseRow;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceRuleTermRow;
import com.saneb.domain.auth.vo.AuthUserDetailsRow;
import com.saneb.domain.auth.vo.AuthenticatedUserDetails;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class AnnouncementSourceRuleReleaseServiceImplTest {

    private static final UUID RELEASE_ID = UUID.fromString("71000000-0000-0000-0000-000000000001");
    private static final UUID RULE_ID = UUID.fromString("71000000-0000-0000-0000-000000000002");
    private static final UUID GROUP_ID = UUID.fromString("71000000-0000-0000-0000-000000000003");
    private static final UUID USER_ID = UUID.fromString("71000000-0000-0000-0000-000000000004");

    @Mock
    private AnnouncementSourceRuleReleaseDao ruleReleaseDao;

    @Mock
    private AnnouncementSourceRuleGoldenGate goldenGate;

    private AnnouncementSourceRuleReleaseServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AnnouncementSourceRuleReleaseServiceImpl(ruleReleaseDao, goldenGate);
    }

    @Test
    void updateKeywordRulePreservesDiscoveryFieldsWhenUiOmitsThem() {
        AnnouncementSourceRuleReleaseRow draft = release("DRAFT", 11);
        AnnouncementSourceKeywordRuleRow before = keywordRule("DRAFT", 7, true, 2);
        when(ruleReleaseDao.selectRuleReleaseDetailsForUpdate(RELEASE_ID)).thenReturn(draft);
        when(ruleReleaseDao.selectKeywordRuleDetailsForUpdate(RELEASE_ID, RULE_ID)).thenReturn(before);
        when(ruleReleaseDao.selectRuleGroupDetails(RELEASE_ID, "TARGET_BUSINESS"))
                .thenReturn(targetGroup("DRAFT"));
        when(ruleReleaseDao.selectDuplicateTermCount(eq(GROUP_ID), eq("NORMALIZED_PHRASE"), any(), eq(RULE_ID)))
                .thenReturn(0L);
        when(ruleReleaseDao.updateKeywordRule(any(AnnouncementSourceKeywordRuleUpdateCommand.class))).thenReturn(1);
        when(ruleReleaseDao.updateRuleReleaseRowVersion(RELEASE_ID, "UI 수정")).thenReturn(1);
        when(ruleReleaseDao.selectKeywordRuleDetails(RELEASE_ID, RULE_ID)).thenReturn(before);

        service.updateKeywordRule(
                authentication(),
                RELEASE_ID,
                RULE_ID,
                new AnnouncementSourceKeywordRuleSaveRequest(
                        "TARGET_BUSINESS",
                        "소상공인",
                        List.of("자영업자"),
                        "STRONG",
                        "NORMALIZED_PHRASE",
                        10,
                        null,
                        null,
                        7,
                        "  UI 수정  "
                )
        );

        ArgumentCaptor<AnnouncementSourceKeywordTermInsertCommand> termCaptor =
                ArgumentCaptor.forClass(AnnouncementSourceKeywordTermInsertCommand.class);
        verify(ruleReleaseDao, org.mockito.Mockito.times(2)).insertKeywordTerm(termCaptor.capture());
        AnnouncementSourceKeywordTermInsertCommand canonical = termCaptor.getAllValues().stream()
                .filter(command -> "CANONICAL".equals(command.termTypeCode()))
                .findFirst()
                .orElseThrow();
        assertThat(canonical.discoveryTerm()).isTrue();
        assertThat(canonical.discoveryOrder()).isEqualTo(2);

        ArgumentCaptor<AnnouncementSourceAuditLogCommand> auditCaptor =
                ArgumentCaptor.forClass(AnnouncementSourceAuditLogCommand.class);
        verify(ruleReleaseDao).insertAuditLog(auditCaptor.capture());
        AnnouncementSourceAuditLogCommand audit = auditCaptor.getValue();
        assertThat(audit.actionCode()).isEqualTo("KEYWORD_RULE_UPDATED");
        assertThat(audit.metadataJson())
                .contains("\"changeReasonProvided\":\"true\"")
                .contains("\"changeReasonLength\":\"5\"")
                .contains("\"changeReasonSha256\":\"dd6c600ee3998083fb564505b62945da6d94f810ee3d0bf142c9b1fba063e4f1\"")
                .doesNotContain("UI 수정");
    }

    @Test
    void updateKeywordRuleStatusRejectsActiveReleaseBeforeRuleMutation() {
        when(ruleReleaseDao.selectRuleReleaseDetailsForUpdate(RELEASE_ID)).thenReturn(release("ACTIVE", 12));

        assertThatThrownBy(() -> service.updateKeywordRuleStatus(
                authentication(),
                RELEASE_ID,
                RULE_ID,
                new AnnouncementSourceKeywordRuleStatusUpdateRequest(false, 7, "비활성화")
        )).isInstanceOfSatisfying(ApiException.class, exception ->
                assertThat(exception.errorCode()).isEqualTo(ErrorCode.ANNOUNCEMENT_SOURCE_RULE_RELEASE_NOT_DRAFT));

        verify(ruleReleaseDao, never()).updateKeywordRuleStatus(any(), anyBoolean(), anyInt(), any());
    }

    @Test
    void updateRuleReleasePublicationRequiresMatchingServerGoldenResult() {
        AnnouncementSourceRuleReleaseRow draft = release("DRAFT", 11);
        AnnouncementSourceRuleReleaseRow active = release("ACTIVE", 12);
        List<AnnouncementSourceRuleTermRow> rows = List.of(targetTermRow());
        Map<String, String> signatures = IntStream.rangeClosed(1, 20)
                .boxed()
                .collect(Collectors.toMap(
                        number -> "QA-%02d".formatted(number),
                        number -> "signature-" + number,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        GoldenGateResult serverResult = new GoldenGateResult("GOLDEN-SERVER-RESULT", signatures);

        when(ruleReleaseDao.selectRuleReleaseDetailsForUpdate(RELEASE_ID)).thenReturn(draft);
        when(ruleReleaseDao.selectRuleTermList(RELEASE_ID)).thenReturn(rows);
        when(goldenGate.selectValidatedResult(any(), anyString())).thenReturn(serverResult);
        when(ruleReleaseDao.selectActiveRuleReleaseDetailsForUpdate()).thenReturn(null);
        when(ruleReleaseDao.updateRuleReleaseActive(eq(RELEASE_ID), eq(11), anyString(), eq("게시 승인"), eq(USER_ID)))
                .thenReturn(1);
        when(ruleReleaseDao.selectRuleReleaseDetails(RELEASE_ID)).thenReturn(active);

        var response = service.updateRuleReleasePublication(
                authentication(),
                RELEASE_ID,
                new AnnouncementSourceRulePublicationRequest(11, "게시 승인", "GOLDEN-SERVER-RESULT")
        );

        assertThat(response.goldenSetRunId()).isEqualTo("GOLDEN-SERVER-RESULT");
        assertThat(response.goldenCaseCount()).isEqualTo(20);
        verify(goldenGate).selectValidatedResult(any(), anyString());
        verify(ruleReleaseDao).updateRuleReleaseActive(
                eq(RELEASE_ID), eq(11), anyString(), eq("게시 승인"), eq(USER_ID)
        );
    }

    @Test
    void updateRuleReleasePublicationRejectsStaleClientGoldenReference() {
        AnnouncementSourceRuleReleaseRow draft = release("DRAFT", 11);
        List<AnnouncementSourceRuleTermRow> rows = List.of(targetTermRow());
        GoldenGateResult serverResult = new GoldenGateResult(
                "GOLDEN-CURRENT",
                Map.of("QA-01", "signature")
        );
        when(ruleReleaseDao.selectRuleReleaseDetailsForUpdate(RELEASE_ID)).thenReturn(draft);
        when(ruleReleaseDao.selectRuleTermList(RELEASE_ID)).thenReturn(rows);
        when(goldenGate.selectValidatedResult(any(), anyString())).thenReturn(serverResult);

        assertThatThrownBy(() -> service.updateRuleReleasePublication(
                authentication(),
                RELEASE_ID,
                new AnnouncementSourceRulePublicationRequest(11, "게시 승인", "GOLDEN-STALE")
        )).isInstanceOfSatisfying(ApiException.class, exception ->
                assertThat(exception.errorCode()).isEqualTo(ErrorCode.ANNOUNCEMENT_SOURCE_RULE_INVALID));

        verify(ruleReleaseDao, never()).selectActiveRuleReleaseDetailsForUpdate();
        verify(ruleReleaseDao, never()).updateRuleReleaseActive(any(), anyInt(), anyString(), anyString(), any());
    }

    @Test
    void selectActiveRuleSetLoadsOnlyRequestedActiveRelease() {
        when(ruleReleaseDao.selectRuleReleaseDetails(RELEASE_ID)).thenReturn(release("ACTIVE", 12));
        when(ruleReleaseDao.selectRuleTermList(RELEASE_ID)).thenReturn(List.of(targetTermRow()));

        var ruleSet = service.selectActiveRuleSet(RELEASE_ID);

        assertThat(ruleSet.releaseCode()).isEqualTo("ASCR-000001");
        assertThat(ruleSet.rules()).hasSize(1);
    }

    @Test
    void selectActiveRuleSetRejectsDraftRelease() {
        when(ruleReleaseDao.selectRuleReleaseDetails(RELEASE_ID)).thenReturn(release("DRAFT", 11));

        assertThatThrownBy(() -> service.selectActiveRuleSet(RELEASE_ID))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.errorCode())
                                .isEqualTo(ErrorCode.ANNOUNCEMENT_SOURCE_RULE_RELEASE_NOT_ACTIVE));

        verify(ruleReleaseDao, never()).selectRuleTermList(RELEASE_ID);
    }

    private AnnouncementSourceRuleReleaseRow release(String status, int rowVersion) {
        OffsetDateTime now = OffsetDateTime.parse("2026-08-11T12:00:00+09:00");
        return new AnnouncementSourceRuleReleaseRow(
                RELEASE_ID,
                "ASCR-000001",
                1,
                rowVersion,
                status,
                null,
                "AND",
                "REVIEW_REQUIRED",
                false,
                false,
                "테스트",
                1L,
                1L,
                now,
                "ACTIVE".equals(status) ? now : null,
                null
        );
    }

    private AnnouncementSourceRuleGroupRow targetGroup(String releaseStatus) {
        return new AnnouncementSourceRuleGroupRow(
                GROUP_ID,
                RELEASE_ID,
                releaseStatus,
                "TARGET_BUSINESS",
                "사업자",
                "TARGET",
                "BUSINESS",
                null,
                true
        );
    }

    private AnnouncementSourceKeywordRuleRow keywordRule(
            String releaseStatus,
            int ruleVersion,
            boolean discoveryTerm,
            Integer discoveryOrder
    ) {
        return new AnnouncementSourceKeywordRuleRow(
                RULE_ID,
                RELEASE_ID,
                releaseStatus,
                11,
                ruleVersion,
                GROUP_ID,
                "TARGET_BUSINESS",
                "사업자",
                "TARGET",
                "BUSINESS",
                null,
                "TARGET_BUSINESS_SMALL_BUSINESS",
                "STRONG",
                true,
                10,
                "소상공인",
                "NORMALIZED_PHRASE",
                discoveryTerm,
                discoveryOrder,
                "자영업자"
        );
    }

    private AnnouncementSourceRuleTermRow targetTermRow() {
        return new AnnouncementSourceRuleTermRow(
                RELEASE_ID,
                "ASCR-000001",
                "DRAFT",
                1,
                11,
                GROUP_ID,
                "TARGET_BUSINESS",
                "TARGET",
                "CONTINUE",
                "CONTINUE",
                10,
                true,
                "BUSINESS",
                null,
                RULE_ID,
                "TARGET_BUSINESS_SMALL_BUSINESS",
                "STRONG",
                10,
                1,
                true,
                UUID.fromString("71000000-0000-0000-0000-000000000005"),
                "CANONICAL",
                "소상공인",
                "소상공인",
                "NORMALIZED_PHRASE",
                true,
                1,
                true,
                true
        );
    }

    private Authentication authentication() {
        AuthenticatedUserDetails principal = new AuthenticatedUserDetails(
                new AuthUserDetailsRow(
                        USER_ID,
                        "admin01",
                        "unused",
                        "관리자",
                        "ACTIVE",
                        false,
                        null,
                        null,
                        null
                ),
                List.of("ADMIN")
        );
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }
}
