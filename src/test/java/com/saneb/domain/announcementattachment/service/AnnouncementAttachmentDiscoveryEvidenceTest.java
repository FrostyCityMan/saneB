package com.saneb.domain.announcementattachment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.common.error.ApiException;
import com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentEvidenceDao;
import com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentJobDao;
import com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentRetryDao;
import com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentRoleDao;
import com.saneb.domain.announcementattachment.service.impl.AnnouncementAttachmentEvidenceServiceImpl;
import com.saneb.domain.announcementattachment.vo.*;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** DAO 대역으로 실패 근거의 검증/봉인 연결을 검사한다. 실제 PostgreSQL 검증은 별도다. */
class AnnouncementAttachmentDiscoveryEvidenceTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final AnnouncementAttachmentJobDao jobs = mock(AnnouncementAttachmentJobDao.class);
    private final AnnouncementAttachmentEvidenceDao evidence = mock(AnnouncementAttachmentEvidenceDao.class);
    private final AnnouncementAttachmentEvidenceService service = new AnnouncementAttachmentEvidenceServiceImpl(jobs,
            evidence, mock(AnnouncementAttachmentRetryDao.class), mock(AnnouncementAttachmentRoleDao.class), mapper);
    private AttachmentJobRow job;
    private AttachmentExecutionSnapshot execution;
    private AttachmentSetRow saved;

    @BeforeEach void configure() throws Exception {
        execution = new AttachmentExecutionSnapshot("LOCAL_HWACHEON_POST_V1", "a".repeat(64), "1.0.0", "1.0.0", "b".repeat(64));
        job = new AttachmentJobRow(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), null, null, 1, 0, 3, "RUNNING", 1, null, UUID.randomUUID(),
                OffsetDateTime.now().plusMinutes(2), null, UUID.randomUUID(), "a".repeat(64), mapper.writeValueAsString(execution), 0L, 0L, 0);
        when(jobs.selectJobDetails(job.jobId())).thenReturn(job);
        when(jobs.selectOwnedJobDetailsForUpdate(job.jobId(), job.leaseToken())).thenReturn(job);
        when(jobs.selectSourceContextDetailsForUpdate(job.sourceId())).thenReturn(new AttachmentSourceContextRow(job.sourceId(),
                "LOCAL_GOV_NOTICE", "PRODUCTION", "REVIEW_REQUIRED", job.baseEvaluationId(), job.contentVersionId(),
                job.ruleReleaseId(), "COMBINATION_MATCHED", 0, 3, true, job.policyId(), null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ATTACHMENT_DETAIL_UNAVAILABLE", "ATTACHMENT_SELECTOR_CHANGED", "ATTACHMENT_DOWNLOAD_FORM_CHANGED",
            "ATTACHMENT_LINK_UNRESOLVED", "ATTACHMENT_FILE_LIMIT"})
    void storesFixedDiscoveryReasonWithoutInventingFilesOrExtraction(String warning) throws Exception {
        when(evidence.insertSet(any())).thenReturn(1);
        when(evidence.updateSetSealed(any(), anyString(), anyString(), anyBoolean(), anyInt(), anyString())).thenAnswer(call -> {
            List<String> warnings = mapper.readValue(call.getArgument(5, String.class),
                    mapper.getTypeFactory().constructCollectionType(List.class, String.class));
            saved = new AttachmentSetRow(call.getArgument(0), job.sourceId(), job.contentVersionId(), job.policyId(),
                    call.getArgument(2), "SEALED", call.getArgument(1), execution.profileHash(), 0, 0,
                    call.getArgument(3), null, null, null, warnings);
            return 1;
        });
        when(evidence.updateJobSet(eq(job.jobId()), eq(job.leaseToken()), any())).thenReturn(1);
        when(evidence.selectSetDetails(eq(job.sourceId()), any())).thenAnswer(call -> saved);
        String status = warning.equals("ATTACHMENT_FILE_LIMIT") ? "LIMIT_EXCEEDED" : "FAILED";
        var result = service.saveAttachmentSet(job.jobId(), job.leaseToken(),
                new AttachmentSetEvidence(status, false, List.of(), List.of(warning))).orElseThrow();
        assertThat(result.warningCodes()).containsExactly(warning);
        assertThat(result.discoveryStatus()).isEqualTo(status).isNotEqualTo("NO_FILES");
        assertThat(result.discoveryComplete()).isFalse();
        assertThat(result.manifestHash()).matches("[0-9a-f]{64}");
        verify(evidence, never()).insertFile(any());
        verify(evidence, never()).insertExtraction(any());
        verify(evidence).deleteFileCheckpoints(job.jobId());
    }

    @Test void rejectsUnknownCodesAndFalseCompletionBeforeAnyWrite() {
        for (var result : List.of(new AttachmentSetEvidence("FAILED", false, List.of(), List.of("RAW_UNKNOWN_CODE")),
                new AttachmentSetEvidence("FAILED", true, List.of(), List.of("ATTACHMENT_DOWNLOAD_FORM_CHANGED")),
                new AttachmentSetEvidence("NO_FILES", false, List.of(), List.of("ATTACHMENT_DOWNLOAD_FORM_CHANGED")))) {
            assertThatThrownBy(() -> service.saveAttachmentSet(job.jobId(), job.leaseToken(), result)).isInstanceOf(ApiException.class);
        }
        verifyNoInteractions(evidence);
    }
}
