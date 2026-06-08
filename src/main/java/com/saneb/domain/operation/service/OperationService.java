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

    PageResponse<NotificationMessageResponse> selectMyNotificationList(
            Authentication authentication,
            Boolean unreadOnly,
            int page,
            int size
    );

    NotificationMessageResponse insertAdminNotification(
            Authentication authentication,
            NotificationSendRequest request
    );

    NotificationMessageResponse updateNotificationRead(
            Authentication authentication,
            UUID notificationId
    );

    PageResponse<OperationTaskResponse> selectOperationTaskList(
            Authentication authentication,
            String taskTypeCode,
            String statusCode,
            String priorityCode,
            int page,
            int size
    );

    OperationTaskResponse insertOperationTask(
            Authentication authentication,
            OperationTaskCreateRequest request
    );

    OperationTaskResponse updateOperationTaskStatus(
            Authentication authentication,
            UUID taskId,
            OperationTaskStatusUpdateRequest request
    );

    OperationTaskCommentResponse insertOperationTaskComment(
            Authentication authentication,
            UUID taskId,
            OperationTaskCommentCreateRequest request
    );

    OperationTaskAssignmentResponse insertOperationTaskAssignment(
            Authentication authentication,
            UUID taskId,
            OperationTaskAssignmentCreateRequest request
    );
}
