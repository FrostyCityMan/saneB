/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: DocumentFileDao.java
 * 작성자: 김도훈
 *
 */

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

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param command 입력 값
     */
    void insertStoredFile(StoredFileInsertCommand command);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param fileId 입력 값
     *
     * @return 처리 결과
     */
    StoredFileRow selectStoredFileDetails(@Param("fileId") UUID fileId);

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param command 입력 값
     */
    void insertDocumentSubmission(DocumentSubmissionInsertCommand command);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param condition 입력 값
     *
     * @return 처리 결과
     */
    List<DocumentSubmissionRow> selectDocumentSubmissionList(DocumentSubmissionSearchCondition condition);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param condition 입력 값
     *
     * @return 처리 결과
     */
    long selectDocumentSubmissionCount(DocumentSubmissionSearchCondition condition);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param submissionId 입력 값
     *
     * @return 처리 결과
     */
    DocumentSubmissionRow selectDocumentSubmissionDetails(@Param("submissionId") UUID submissionId);

    /**
     * 업무 데이터를 수정합니다.
     *
     * @param command 입력 값
     *
     * @return 처리 결과
     */
    int updateDocumentSubmissionReview(DocumentSubmissionReviewCommand command);

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param command 입력 값
     */
    void insertDocumentSubmissionReview(DocumentSubmissionReviewCommand command);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param verificationId 입력 값
     *
     * @return 처리 결과
     */
    PartnerVerificationAccessRow selectPartnerVerificationAccess(@Param("verificationId") UUID verificationId);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param progressId 입력 값
     *
     * @return 처리 결과
     */
    ApplicationProgressAccessRow selectApplicationProgressAccess(@Param("progressId") UUID progressId);

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param command 입력 값
     */
    void insertAuditLog(AuditLogCommand command);
}
