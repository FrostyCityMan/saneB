package com.saneb.domain.documentfile.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.saneb.common.response.PageResponse;
import com.saneb.domain.auth.vo.AuthUserDetailsRow;
import com.saneb.domain.auth.vo.AuthenticatedUserDetails;
import com.saneb.domain.documentfile.dto.DocumentSubmissionResponse;
import com.saneb.domain.documentfile.dto.StoredFileResponse;
import com.saneb.domain.documentfile.service.DocumentFileService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "spring.flyway.enabled=false")
@AutoConfigureMockMvc
class DocumentFileControllerSmokeTest {

    private static final UUID USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000002");
    private static final UUID FILE_ID = UUID.fromString("80000000-0000-0000-0000-000000000001");
    private static final UUID SUBMISSION_ID = UUID.fromString("80000000-0000-0000-0000-000000000002");
    private static final UUID RESOURCE_ID = UUID.fromString("60000000-0000-0000-0000-000000000001");
    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-06-08T10:00:00+09:00");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DocumentFileService documentFileService;

    @BeforeEach
    void setUp() {
        when(documentFileService.insertStoredFile(any(), any())).thenReturn(storedFile());
        when(documentFileService.selectStoredFileDetails(any(), eq(FILE_ID))).thenReturn(storedFile());
        when(documentFileService.insertDocumentSubmission(any(), any())).thenReturn(submission("SUBMITTED"));
        when(documentFileService.selectDocumentSubmissionList(any(), any(), any(), any(), eq(1), eq(20)))
                .thenReturn(PageResponse.of(List.of(submission("SUBMITTED")), 1, 20, 1));
        when(documentFileService.updateDocumentSubmissionReview(any(), eq(SUBMISSION_ID), any()))
                .thenReturn(submission("APPROVED"));
    }

    @Test
    void insertStoredFileReturnsApiResponse() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "business.pdf",
                "application/pdf",
                "pdf-data".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/files")
                        .file(file)
                        .with(user(userPrincipal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fileId").value(FILE_ID.toString()))
                .andExpect(jsonPath("$.data.originalFilename").value("business.pdf"));
    }

    @Test
    void selectStoredFileDetailsReturnsApiResponse() throws Exception {
        mockMvc.perform(get("/api/v1/files/{fileId}", FILE_ID)
                        .with(user(userPrincipal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.checksumSha256").value("0".repeat(64)));
    }

    @Test
    void insertDocumentSubmissionReturnsApiResponse() throws Exception {
        mockMvc.perform(post("/api/v1/document-submissions")
                        .with(user(userPrincipal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fileId": "%s",
                                  "resourceTypeCode": "APPLICATION_PROGRESS",
                                  "resourceId": "%s",
                                  "documentTypeCode": "BUSINESS_REGISTRATION"
                                }
                                """.formatted(FILE_ID, RESOURCE_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.submissionId").value(SUBMISSION_ID.toString()))
                .andExpect(jsonPath("$.data.statusCode").value("SUBMITTED"));
    }

    @Test
    void selectDocumentSubmissionListReturnsPagedApiResponse() throws Exception {
        mockMvc.perform(get("/api/v1/document-submissions")
                        .with(user(operatorPrincipal()))
                        .param("statusCode", "SUBMITTED")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].fileId").value(FILE_ID.toString()))
                .andExpect(jsonPath("$.data.totalCount").value(1));
    }

    @Test
    void updateDocumentSubmissionReviewReturnsApiResponse() throws Exception {
        mockMvc.perform(patch("/api/v1/document-submissions/{submissionId}/review", SUBMISSION_ID)
                        .with(user(operatorPrincipal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "statusCode": "APPROVED",
                                  "reviewNote": "확인 완료"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.statusCode").value("APPROVED"));
    }

    @Test
    void documentSubmissionReviewRejectsUserRole() throws Exception {
        mockMvc.perform(patch("/api/v1/document-submissions/{submissionId}/review", SUBMISSION_ID)
                        .with(user(userPrincipal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "statusCode": "APPROVED"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    private StoredFileResponse storedFile() {
        return new StoredFileResponse(
                FILE_ID,
                USER_ID,
                "business.pdf",
                "application/pdf",
                8,
                "0".repeat(64),
                "STORED",
                CREATED_AT
        );
    }

    private DocumentSubmissionResponse submission(String statusCode) {
        return new DocumentSubmissionResponse(
                SUBMISSION_ID,
                FILE_ID,
                "business.pdf",
                "application/pdf",
                8,
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

    private AuthenticatedUserDetails userPrincipal() {
        return new AuthenticatedUserDetails(
                new AuthUserDetailsRow(
                        USER_ID,
                        "local_user",
                        "password-hash",
                        "Local User",
                        "ACTIVE",
                        false,
                        null,
                        null,
                        null
                ),
                List.of("USER")
        );
    }

    private AuthenticatedUserDetails operatorPrincipal() {
        return new AuthenticatedUserDetails(
                new AuthUserDetailsRow(
                        USER_ID,
                        "local_operator",
                        "password-hash",
                        "Local Operator",
                        "ACTIVE",
                        false,
                        null,
                        null,
                        null
                ),
                List.of("OPERATOR")
        );
    }
}
