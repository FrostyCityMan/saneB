package com.saneb.domain.documentfile.dao;

import com.saneb.domain.documentfile.vo.ApplicationProgressAccessRow;
import com.saneb.domain.documentfile.vo.AuditLogCommand;
import com.saneb.domain.documentfile.vo.DocumentSubmissionInsertCommand;
import com.saneb.domain.documentfile.vo.DocumentSubmissionReviewCommand;
import com.saneb.domain.documentfile.vo.DocumentSubmissionRow;
import com.saneb.domain.documentfile.vo.DocumentSubmissionSearchCondition;
import com.saneb.domain.documentfile.vo.PartnerVerificationAccessRow;
import com.saneb.domain.documentfile.vo.StoredFileInsertCommand;
import com.saneb.domain.documentfile.vo.StoredFileRow;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DocumentFileDao {

    void insertStoredFile(StoredFileInsertCommand command);

    StoredFileRow selectStoredFileDetails(@Param("fileId") UUID fileId);

    void insertDocumentSubmission(DocumentSubmissionInsertCommand command);

    List<DocumentSubmissionRow> selectDocumentSubmissionList(DocumentSubmissionSearchCondition condition);

    long selectDocumentSubmissionCount(DocumentSubmissionSearchCondition condition);

    DocumentSubmissionRow selectDocumentSubmissionDetails(@Param("submissionId") UUID submissionId);

    int updateDocumentSubmissionReview(DocumentSubmissionReviewCommand command);

    void insertDocumentSubmissionReview(DocumentSubmissionReviewCommand command);

    PartnerVerificationAccessRow selectPartnerVerificationAccess(@Param("verificationId") UUID verificationId);

    ApplicationProgressAccessRow selectApplicationProgressAccess(@Param("progressId") UUID progressId);

    void insertAuditLog(AuditLogCommand command);
}
