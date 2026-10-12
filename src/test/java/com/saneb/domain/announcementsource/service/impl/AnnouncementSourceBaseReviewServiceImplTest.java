package com.saneb.domain.announcementsource.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.saneb.common.error.ApiException;
import com.saneb.domain.announcementsource.dao.AnnouncementSourceDao;
import com.saneb.domain.announcementsource.dto.*;
import com.saneb.domain.announcementsource.service.*;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceSnapshotRow;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceAuditLogCommand;
import com.saneb.domain.auth.vo.AuthenticatedUserDetails;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

class AnnouncementSourceBaseReviewServiceImplTest {
    private final AnnouncementSourceDao sources = mock(AnnouncementSourceDao.class);
    private final AnnouncementSourceService sourceService = mock(AnnouncementSourceService.class);
    private final AnnouncementSourceClassificationManagementService classification = mock(AnnouncementSourceClassificationManagementService.class);
    private final AnnouncementSourceV2ConversionService conversion = mock(AnnouncementSourceV2ConversionService.class);
    private final AnnouncementSourceBaseReviewServiceImpl service = new AnnouncementSourceBaseReviewServiceImpl(sources, sourceService, classification, conversion);
    private final UUID id = UUID.randomUUID(), decision = UUID.randomUUID(), actorId = UUID.randomUUID();

    private UsernamePasswordAuthenticationToken actor() {
        var principal = mock(AuthenticatedUserDetails.class);
        when(principal.userId()).thenReturn(actorId);
        return new UsernamePasswordAuthenticationToken(principal, null, List.of());
    }

    private AnnouncementSourceBaseReviewRequest request(String method) {
        return new AnnouncementSourceBaseReviewRequest(new AnnouncementSourceV2ToAnnouncementRequest(
                "BUSINESS", List.of("BUSINESS"), List.of("GENERAL_SUPPORT"), null, decision, 1), "직접 확인한 검수 사유", method);
    }

    private void ready() {
        when(sources.selectSourceDetailsForUpdate(id)).thenReturn(mock(AnnouncementSourceSnapshotRow.class));
        var context = mock(AnnouncementSourceConversionContextResponse.class);
        when(context.convertible()).thenReturn(true);
        when(context.decisionId()).thenReturn(decision);
        when(context.version()).thenReturn(1);
        when(conversion.selectConversionContextDetails(id)).thenReturn(context);
        var result = mock(AnnouncementSourceClassificationDetailsResponse.class);
        when(result.decisionId()).thenReturn(decision);
        when(result.version()).thenReturn(2);
        when(result.semanticStatusCode()).thenReturn("ACCEPTED");
        when(classification.saveConfirmedClassification(any(), eq(id), any())).thenReturn(result);
    }

    @ParameterizedTest @ValueSource(strings = {"EXTRACTED_TEXT", "MANUAL_SOURCE_CHECK"})
    void recordsMethodWithoutCopyingReviewNoteToAuditMetadata(String method) {
        ready();
        var link = new AnnouncementSourceLinkResponse(id, "SRC-TEST", UUID.randomUUID(), "ANN-TEST");
        when(conversion.insertOperationalAnnouncement(any(), eq(id), any())).thenReturn(link);
        assertThat(service.insertReviewedAnnouncement(actor(), id, request(method))).isEqualTo(link);
        var audit = ArgumentCaptor.forClass(AnnouncementSourceAuditLogCommand.class);
        verify(sources).insertAuditLog(audit.capture());
        assertThat(audit.getValue().actorUserId()).isEqualTo(actorId);
        assertThat(audit.getValue().metadataJson()).isEqualTo("{\"reviewMethodCode\":\"" + method + "\"}");
        assertThat(audit.getValue().metadataJson()).doesNotContain("직접 확인한 검수 사유");
        var payload = ArgumentCaptor.forClass(AnnouncementSourceV2ToAnnouncementRequest.class);
        verify(conversion).insertOperationalAnnouncement(any(), eq(id), payload.capture());
        assertThat(payload.getValue().incomeJudgementCode()).isNull();
        assertThat(payload.getValue().expectedVersion()).isEqualTo(2);
    }

    @Test void legacyRequestDoesNotInventReviewMethod() {
        ready();
        service.insertReviewedAnnouncement(actor(), id, new AnnouncementSourceBaseReviewRequest(request(null).classification(), "검수 사유"));
        verify(sources, never()).insertAuditLog(any());
    }

    @ParameterizedTest @ValueSource(strings = {"", "UNKNOWN", "MANUAL_SOURCE_CHECK\""})
    void invalidMethodStopsBeforeAnySourceAccess(String method) {
        assertThatThrownBy(() -> service.insertReviewedAnnouncement(actor(), id, request(method)))
                .isInstanceOf(ApiException.class).hasMessageContaining("검수 방법");
        verifyNoInteractions(sources, classification, conversion);
    }

    @Test void failedDraftDoesNotRecordSuccessfulReviewMethod() {
        ready();
        when(conversion.insertOperationalAnnouncement(any(), eq(id), any())).thenThrow(new IllegalStateException("초안 생성 실패"));
        assertThatThrownBy(() -> service.insertReviewedAnnouncement(actor(), id, request("EXTRACTED_TEXT")))
                .isInstanceOf(IllegalStateException.class);
        verify(sources, never()).insertAuditLog(any());
    }
}
