/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: OperationService.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.operation.service;

import com.saneb.common.response.PageResponse;
import com.saneb.domain.operation.dto.NotificationMessageResponse;
import com.saneb.domain.operation.dto.NotificationSendRequest;
import com.saneb.domain.operation.dto.OperationTaskAssignmentCreateRequest;
import com.saneb.domain.operation.dto.OperationTaskAssignmentResponse;
import com.saneb.domain.operation.dto.OperationTaskCommentCreateRequest;
import com.saneb.domain.operation.dto.OperationTaskCommentResponse;
import com.saneb.domain.operation.dto.OperationTaskCreateRequest;
import com.saneb.domain.operation.dto.OperationTaskResponse;
import com.saneb.domain.operation.dto.OperationTaskStatusUpdateRequest;
import java.util.UUID;
import org.springframework.security.core.Authentication;

public interface OperationService {

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param authentication 입력 값
     *
     * @param unreadOnly 입력 값
     *
     * @param page 입력 값
     *
     * @param size 입력 값
     *
     * @return 처리 결과
     */
    PageResponse<NotificationMessageResponse> selectMyNotificationList(
            Authentication authentication,
            Boolean unreadOnly,
            int page,
            int size
    );

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param authentication 입력 값
     *
     * @param request 입력 값
     *
     * @return 처리 결과
     */
    NotificationMessageResponse insertAdminNotification(
            Authentication authentication,
            NotificationSendRequest request
    );

    /**
     * 업무 데이터를 수정합니다.
     *
     * @param authentication 입력 값
     *
     * @param notificationId 입력 값
     *
     * @return 처리 결과
     */
    NotificationMessageResponse updateNotificationRead(
            Authentication authentication,
            UUID notificationId
    );

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param authentication 입력 값
     *
     * @param taskTypeCode 입력 값
     *
     * @param statusCode 입력 값
     *
     * @param priorityCode 입력 값
     *
     * @param page 입력 값
     *
     * @param size 입력 값
     *
     * @return 처리 결과
     */
    PageResponse<OperationTaskResponse> selectOperationTaskList(
            Authentication authentication,
            String taskTypeCode,
            String statusCode,
            String priorityCode,
            int page,
            int size
    );

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param authentication 입력 값
     *
     * @param request 입력 값
     *
     * @return 처리 결과
     */
    OperationTaskResponse insertOperationTask(
            Authentication authentication,
            OperationTaskCreateRequest request
    );

    /**
     * 업무 데이터를 수정합니다.
     *
     * @param authentication 입력 값
     *
     * @param taskId 입력 값
     *
     * @param request 입력 값
     *
     * @return 처리 결과
     */
    OperationTaskResponse updateOperationTaskStatus(
            Authentication authentication,
            UUID taskId,
            OperationTaskStatusUpdateRequest request
    );

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param authentication 입력 값
     *
     * @param taskId 입력 값
     *
     * @param request 입력 값
     *
     * @return 처리 결과
     */
    OperationTaskCommentResponse insertOperationTaskComment(
            Authentication authentication,
            UUID taskId,
            OperationTaskCommentCreateRequest request
    );

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param authentication 입력 값
     *
     * @param taskId 입력 값
     *
     * @param request 입력 값
     *
     * @return 처리 결과
     */
    OperationTaskAssignmentResponse insertOperationTaskAssignment(
            Authentication authentication,
            UUID taskId,
            OperationTaskAssignmentCreateRequest request
    );
}
