package com.saneb.domain.documentfile.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.saneb.common.error.ApiException;
import com.saneb.common.error.ErrorCode;
import com.saneb.domain.auth.vo.AuthUserDetailsRow;
import com.saneb.domain.auth.vo.AuthenticatedUserDetails;
import com.saneb.domain.documentfile.dao.DocumentFileDao;
import com.saneb.domain.documentfile.dto.DocumentSubmissionCreateRequest;
import com.saneb.domain.documentfile.dto.DocumentSubmissionReviewRequest;
import com.saneb.domain.documentfile.vo.ApplicationProgressAccessRow;
import com.saneb.domain.documentfile.vo.DocumentSubmissionInsertCommand;
import com.saneb.domain.documentfile.vo.DocumentSubmissionReviewCommand;
import com.saneb.domain.documentfile.vo.DocumentSubmissionRow;
import com.saneb.domain.documentfile.vo.StoredFileInsertCommand;
import com.saneb.domain.documentfile.vo.StoredFileRow;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class DocumentFileServiceImplTest {

    private static final UUID USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000002");
    private static final UUID FILE_ID = UUID.fromString("80000000-0000-0000-0000-000000000001");
    private static final UUID SUBMISSION_ID = UUID.fromString("80000000-0000-0000-0000-000000000002");
    private static final UUID RESOURCE_ID = UUID.fromString("60000000-0000-0000-0000-000000000001");
    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-06-08T10:00:00+09:00");

    @TempDir
    private Path tempDir;

    @Mock
    private DocumentFileDao documentFileDao;

    private DocumentFileServiceImpl documentFileService;

    @BeforeEach
    void setUp() {
        documentFileService = new DocumentFileServiceImpl(documentFileDao, tempDir.toString(), 1024);
    }

    @Test
    void insertStoredFileWritesFileAndSavesMetadata() throws Exception {
        when(documentFileDao.selectStoredFileDetails(any())).thenAnswer(invocation -> storedFile(
                invocation.getArgument(0),
                "docs/test.pdf",
                "business.pdf"
        ));

        var response = documentFileService.insertStoredFile(
                authentication(List.of("USER")),
                new MockMultipartFile("file", "business.pdf", "application/pdf", "pdf-data".getBytes())
        );

        ArgumentCaptor<StoredFileInsertCommand> captor = ArgumentCaptor.forClass(StoredFileInsertCommand.class);
        verify(documentFileDao).insertStoredFile(captor.capture());
        StoredFileInsertCommand command = captor.getValue();
        assertThat(command.ownerUserId()).isEqualTo(USER_ID);
        assertThat(command.originalFilename()).isEqualTo("business.pdf");
        assertThat(command.checksumSha256()).hasSize(64);
        assertThat(Files.exists(tempDir.resolve(command.storageKey()))).isTrue();
        assertThat(response.fileId()).isEqualTo(command.fileId());
    }

    @Test
    void insertStoredFileRejectsEmptyFile() {
        assertThatThrownBy(() -> documentFileService.insertStoredFile(
                authentication(List.of("USER")),
                new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0])
        ))
                .isInstanceOf(ApiException.class)
                .extracting(error -> ((ApiException) error).errorCode())
                .isEqualTo(ErrorCode.VALIDATION_FAILED);
    }

    @Test
    void insertDocumentSubmissionValidatesResourceAndWritesAudit() {
        when(documentFileDao.selectStoredFileDetails(FILE_ID)).thenReturn(storedFile(FILE_ID, "docs/test.pdf", "business.pdf"));
        when(documentFileDao.selectApplicationProgressAccess(RESOURCE_ID)).thenReturn(
                new ApplicationProgressAccessRow(RESOURCE_ID, USER_ID)
        );
        when(documentFileDao.selectDocumentSubmissionDetails(any())).thenReturn(submission("SUBMITTED"));

        var response = documentFileService.insertDocumentSubmission(
                authentication(List.of("USER")),
                new DocumentSubmissionCreateRequest(
                        FILE_ID,
                        "application_progress",
                        RESOURCE_ID,
                        "business_registration"
                )
        );

        ArgumentCaptor<DocumentSubmissionInsertCommand> captor =
                ArgumentCaptor.forClass(DocumentSubmissionInsertCommand.class);
        verify(documentFileDao).insertDocumentSubmission(captor.capture());
        assertThat(captor.getValue().resourceTypeCode()).isEqualTo("APPLICATION_PROGRESS");
        assertThat(captor.getValue().documentTypeCode()).isEqualTo("BUSINESS_REGISTRATION");
        verify(documentFileDao).insertAuditLog(any());
        assertThat(response.statusCode()).isEqualTo("SUBMITTED");
    }

    @Test
    void updateDocumentSubmissionReviewSavesLatestStateAndHistory() {
        when(documentFileDao.selectDocumentSubmissionDetails(SUBMISSION_ID))
                .thenReturn(submission("SUBMITTED"), submission("APPROVED"));
        when(documentFileDao.updateDocumentSubmissionReview(any())).thenReturn(1);

        var response = documentFileService.updateDocumentSubmissionReview(
                authentication(List.of("OPERATOR")),
                SUBMISSION_ID,
                new DocumentSubmissionReviewRequest("approved", "확인 완료")
        );

        ArgumentCaptor<DocumentSubmissionReviewCommand> captor =
                ArgumentCaptor.forClass(DocumentSubmissionReviewCommand.class);
        verify(documentFileDao).updateDocumentSubmissionReview(captor.capture());
        assertThat(captor.getValue().beforeStatusCode()).isEqualTo("SUBMITTED");
        assertThat(captor.getValue().afterStatusCode()).isEqualTo("APPROVED");
        verify(documentFileDao).insertDocumentSubmissionReview(any());
        verify(documentFileDao).insertAuditLog(any());
        assertThat(response.statusCode()).isEqualTo("APPROVED");
    }

    @Test
    void selectStoredFileDetailsRejectsOtherUserFile() {
        when(documentFileDao.selectStoredFileDetails(FILE_ID)).thenReturn(
                storedFile(FILE_ID, "docs/test.pdf", "business.pdf", UUID.randomUUID())
        );

        assertThatThrownBy(() -> documentFileService.selectStoredFileDetails(authentication(List.of("USER")), FILE_ID))
                .isInstanceOf(ApiException.class)
                .extracting(error -> ((ApiException) error).errorCode())
                .isEqualTo(ErrorCode.AUTH_FORBIDDEN);
    }

    private StoredFileRow storedFile(UUID fileId, String storageKey, String originalFilename) {
        return storedFile(fileId, storageKey, originalFilename, USER_ID);
    }

    private StoredFileRow storedFile(UUID fileId, String storageKey, String originalFilename, UUID ownerUserId) {
        return new StoredFileRow(
                fileId,
                ownerUserId,
                originalFilename,
                fileId + ".pdf",
                storageKey,
                "application/pdf",
                8L,
                "0".repeat(64),
                "STORED",
                CREATED_AT
        );
    }

    private DocumentSubmissionRow submission(String statusCode) {
        return new DocumentSubmissionRow(
                SUBMISSION_ID,
                FILE_ID,
                "business.pdf",
                "application/pdf",
                8L,
                "APPLICATION_PROGRESS",
                RESOURCE_ID,
                "BUSINESS_REGISTRATION",
                statusCode,
                USER_ID,
                CREATED_AT,
                "APPROVED".equals(statusCode) ? USER_ID : null,
                "APPROVED".equals(statusCode) ? CREATED_AT : null,
                "APPROVED".equals(statusCode) ? "확인 완료" : null
        );
    }

    private UsernamePasswordAuthenticationToken authentication(List<String> roles) {
        AuthUserDetailsRow row = new AuthUserDetailsRow(
                USER_ID,
                "user01",
                "hash",
                "Local User",
                "ACTIVE",
                false,
                null,
                null,
                null
        );
        AuthenticatedUserDetails principal = new AuthenticatedUserDetails(row, roles);
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }
}
