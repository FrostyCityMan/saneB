package com.saneb.domain.operation.controller;

import com.saneb.common.response.ApiResponse;
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
import com.saneb.domain.operation.service.OperationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1")
public class OperationController {

    private final OperationService operationService;

    public OperationController(OperationService operationService) {
        this.operationService = operationService;
    }

    @GetMapping("/notifications/me")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageResponse<NotificationMessageResponse>> selectMyNotificationList(
            Authentication authentication,
            @RequestParam(required = false) Boolean unreadOnly,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ApiResponse.success(operationService.selectMyNotificationList(authentication, unreadOnly, page, size));
    }

    @PatchMapping("/notifications/{notificationId}/read")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<NotificationMessageResponse> updateNotificationRead(
            Authentication authentication,
            @PathVariable UUID notificationId
    ) {
        return ApiResponse.success(operationService.updateNotificationRead(authentication, notificationId));
    }

    @PostMapping("/admin/notifications/send")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ApiResponse<NotificationMessageResponse> insertAdminNotification(
            Authentication authentication,
            @Valid @RequestBody NotificationSendRequest request
    ) {
        return ApiResponse.success(operationService.insertAdminNotification(authentication, request));
    }

    @GetMapping("/operation-tasks")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ApiResponse<PageResponse<OperationTaskResponse>> selectOperationTaskList(
            Authentication authentication,
            @RequestParam(required = false) String taskTypeCode,
            @RequestParam(required = false) String statusCode,
            @RequestParam(required = false) String priorityCode,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ApiResponse.success(operationService.selectOperationTaskList(
                authentication,
                taskTypeCode,
                statusCode,
                priorityCode,
                page,
                size
        ));
    }

    @PostMapping("/operation-tasks")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ApiResponse<OperationTaskResponse> insertOperationTask(
            Authentication authentication,
            @Valid @RequestBody OperationTaskCreateRequest request
    ) {
        return ApiResponse.success(operationService.insertOperationTask(authentication, request));
    }

    @PatchMapping("/operation-tasks/{taskId}/status")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ApiResponse<OperationTaskResponse> updateOperationTaskStatus(
            Authentication authentication,
            @PathVariable UUID taskId,
            @Valid @RequestBody OperationTaskStatusUpdateRequest request
    ) {
        return ApiResponse.success(operationService.updateOperationTaskStatus(authentication, taskId, request));
    }

    @PostMapping("/operation-tasks/{taskId}/comments")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ApiResponse<OperationTaskCommentResponse> insertOperationTaskComment(
            Authentication authentication,
            @PathVariable UUID taskId,
            @Valid @RequestBody OperationTaskCommentCreateRequest request
    ) {
        return ApiResponse.success(operationService.insertOperationTaskComment(authentication, taskId, request));
    }

    @PostMapping("/operation-tasks/{taskId}/assignments")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ApiResponse<OperationTaskAssignmentResponse> insertOperationTaskAssignment(
            Authentication authentication,
            @PathVariable UUID taskId,
            @Valid @RequestBody OperationTaskAssignmentCreateRequest request
    ) {
        return ApiResponse.success(operationService.insertOperationTaskAssignment(authentication, taskId, request));
    }
}
