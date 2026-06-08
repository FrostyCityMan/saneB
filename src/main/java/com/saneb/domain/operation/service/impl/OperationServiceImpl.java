package com.saneb.domain.operation.service.impl;

import com.saneb.common.error.ApiException;
import com.saneb.common.error.ErrorCode;
import com.saneb.common.response.PageResponse;
import com.saneb.domain.auth.vo.AuthenticatedUserDetails;
import com.saneb.domain.operation.dao.OperationDao;
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
import com.saneb.domain.operation.vo.AuditLogCommand;
import com.saneb.domain.operation.vo.NotificationDeliveryLogCommand;
import com.saneb.domain.operation.vo.NotificationMessageInsertCommand;
import com.saneb.domain.operation.vo.NotificationMessageRow;
import com.saneb.domain.operation.vo.NotificationMessageSearchCondition;
import com.saneb.domain.operation.vo.NotificationTemplateRow;
import com.saneb.domain.operation.vo.OperationTaskAssignmentInsertCommand;
import com.saneb.domain.operation.vo.OperationTaskAssignmentRow;
import com.saneb.domain.operation.vo.OperationTaskCommentInsertCommand;
import com.saneb.domain.operation.vo.OperationTaskCommentRow;
import com.saneb.domain.operation.vo.OperationTaskInsertCommand;
import com.saneb.domain.operation.vo.OperationTaskRow;
import com.saneb.domain.operation.vo.OperationTaskSearchCondition;
import com.saneb.domain.operation.vo.OperationTaskStatusCommand;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OperationServiceImpl implements OperationService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_TITLE_LENGTH = 200;
    private static final Set<String> OPERATING_ROLES = Set.of("OPERATOR", "ADMIN");
    private static final Set<String> CHANNEL_CODES = Set.of("IN_APP", "EMAIL", "SMS", "KAKAO");
    private static final Set<String> TASK_TYPE_CODES = Set.of(
            "DELAYED_PROGRESS",
            "SUPPLEMENT_REQUEST",
            "RECONTACT",
            "PAYMENT_FAILED",
            "CONSULTATION_PENDING",
            "GENERAL"
    );
    private static final Set<String> TASK_STATUS_CODES = Set.of("OPEN", "IN_PROGRESS", "WAITING", "DONE", "CANCELED");
    private static final Set<String> TASK_PRIORITY_CODES = Set.of("LOW", "NORMAL", "HIGH", "URGENT");
    private static final Set<String> RESOURCE_TYPES = Set.of(
            "GENERAL",
            "ANNOUNCEMENT",
            "PARTNER_VERIFICATION",
            "MATCHING_CASE",
            "APPLICATION_PROGRESS",
            "CONSULTATION_RESERVATION",
            "PAYMENT_TRANSACTION",
            "DOCUMENT_SUBMISSION",
            "OPERATION_TASK"
    );

    private final OperationDao operationDao;

    public OperationServiceImpl(OperationDao operationDao) {
        this.operationDao = operationDao;
    }

    @Override
    public PageResponse<NotificationMessageResponse> selectMyNotificationList(
            Authentication authentication,
            Boolean unreadOnly,
            int page,
            int size
    ) {
        validatePageRequest(page, size);
        AuthenticatedUserDetails actor = selectRequiredPrincipal(authentication);
        NotificationMessageSearchCondition condition = new NotificationMessageSearchCondition(
                actor.userId(),
                unreadOnly,
                page,
                size,
                (page - 1) * size
        );
        long totalCount = operationDao.selectNotificationMessageCount(condition);
        List<NotificationMessageResponse> items = operationDao.selectNotificationMessageList(condition).stream()
                .map(this::toNotificationResponse)
                .toList();
        return PageResponse.of(items, page, size, totalCount);
    }

    @Override
    @Transactional
    public NotificationMessageResponse insertAdminNotification(
            Authentication authentication,
            NotificationSendRequest request
    ) {
        AuthenticatedUserDetails actor = selectRequiredPrincipal(authentication);
        validateOperatingRole(actor, "알림 발송 권한이 없습니다.");
        validateUserExists(request.recipientUserId());
        String channelCode = normalizeRequiredCode("channelCode", request.channelCode(), CHANNEL_CODES);
        String resourceType = normalizeResourceType(request.resourceType());
        NotificationTemplateRow template = selectTemplateIfRequested(request.templateCode(), channelCode);
        String statusCode = "IN_APP".equals(channelCode) ? "SENT" : "CREATED";
        UUID notificationId = UUID.randomUUID();
        operationDao.insertNotificationMessage(new NotificationMessageInsertCommand(
                notificationId,
                request.recipientUserId(),
                template == null ? null : template.templateId(),
                channelCode,
                trimTitle(request.title()),
                trimRequired(request.body(), "알림 내용을 입력하세요."),
                statusCode,
                resourceType,
                request.resourceId(),
                actor.userId()
        ));
        operationDao.insertNotificationDeliveryLog(new NotificationDeliveryLogCommand(
                UUID.randomUUID(),
                notificationId,
                channelCode,
                selectProviderCode(channelCode),
                "IN_APP".equals(channelCode) ? "SUCCESS" : "SKIPPED",
                1,
                null,
                "IN_APP".equals(channelCode) ? null : "PROVIDER_NOT_CONFIGURED",
                "IN_APP".equals(channelCode) ? null : "외부 발송 provider가 아직 설정되지 않았습니다.",
                metadata("channelCode", channelCode, "resourceType", resourceType, "providerConfigured", "false")
        ));
        insertAudit(actor.userId(), "NOTIFICATION_MESSAGE_SEND", "NOTIFICATION_MESSAGE", notificationId, metadata(
                "recipientUserId", request.recipientUserId().toString(),
                "channelCode", channelCode,
                "resourceType", resourceType
        ));
        return toNotificationResponse(selectNotificationRow(notificationId));
    }

    @Override
    @Transactional
    public NotificationMessageResponse updateNotificationRead(Authentication authentication, UUID notificationId) {
        AuthenticatedUserDetails actor = selectRequiredPrincipal(authentication);
        NotificationMessageRow row = selectNotificationRow(notificationId);
        if (!row.recipientUserId().equals(actor.userId())) {
            throw new ApiException(ErrorCode.AUTH_FORBIDDEN, HttpStatus.FORBIDDEN, "본인 알림만 읽음 처리할 수 있습니다.");
        }
        operationDao.updateNotificationMessageRead(notificationId);
        insertAudit(actor.userId(), "NOTIFICATION_MESSAGE_READ", "NOTIFICATION_MESSAGE", notificationId, metadata(
                "recipientUserId", actor.userId().toString(),
                "channelCode", row.channelCode(),
                "alreadyRead", String.valueOf(row.readAt() != null)
        ));
        return toNotificationResponse(selectNotificationRow(notificationId));
    }

    @Override
    public PageResponse<OperationTaskResponse> selectOperationTaskList(
            Authentication authentication,
            String taskTypeCode,
            String statusCode,
            String priorityCode,
            int page,
            int size
    ) {
        validatePageRequest(page, size);
        AuthenticatedUserDetails actor = selectRequiredPrincipal(authentication);
        validateOperatingRole(actor, "운영 업무 조회 권한이 없습니다.");
        String normalizedTaskTypeCode = normalizeOptionalCode(taskTypeCode);
        String normalizedStatusCode = normalizeOptionalCode(statusCode);
        String normalizedPriorityCode = normalizeOptionalCode(priorityCode);
        validateOptionalCode("taskTypeCode", normalizedTaskTypeCode, TASK_TYPE_CODES);
        validateOptionalCode("statusCode", normalizedStatusCode, TASK_STATUS_CODES);
        validateOptionalCode("priorityCode", normalizedPriorityCode, TASK_PRIORITY_CODES);

        OperationTaskSearchCondition condition = new OperationTaskSearchCondition(
                normalizedTaskTypeCode,
                normalizedStatusCode,
                normalizedPriorityCode,
                page,
                size,
                (page - 1) * size
        );
        long totalCount = operationDao.selectOperationTaskCount(condition);
        List<OperationTaskResponse> items = operationDao.selectOperationTaskList(condition).stream()
                .map(this::toTaskResponse)
                .toList();
        return PageResponse.of(items, page, size, totalCount);
    }

    @Override
    @Transactional
    public OperationTaskResponse insertOperationTask(
            Authentication authentication,
            OperationTaskCreateRequest request
    ) {
        AuthenticatedUserDetails actor = selectRequiredPrincipal(authentication);
        validateOperatingRole(actor, "운영 업무 생성 권한이 없습니다.");
        String taskTypeCode = normalizeRequiredCode("taskTypeCode", request.taskTypeCode(), TASK_TYPE_CODES);
        String priorityCode = normalizeOptionalCode(request.priorityCode());
        if (priorityCode == null) {
            priorityCode = "NORMAL";
        }
        validateOptionalCode("priorityCode", priorityCode, TASK_PRIORITY_CODES);
        String resourceType = normalizeResourceType(request.resourceType());

        UUID taskId = UUID.randomUUID();
        operationDao.insertOperationTask(new OperationTaskInsertCommand(
                taskId,
                taskTypeCode,
                priorityCode,
                trimTitle(request.title()),
                trimToNull(request.description()),
                resourceType,
                request.resourceId(),
                request.dueAt(),
                actor.userId()
        ));
        if (request.assigneeUserIds() != null) {
            for (UUID assigneeUserId : request.assigneeUserIds()) {
                insertAssignment(taskId, assigneeUserId, actor.userId());
            }
        }
        insertAudit(actor.userId(), "OPERATION_TASK_CREATE", "OPERATION_TASK", taskId, metadata(
                "taskTypeCode", taskTypeCode,
                "priorityCode", priorityCode,
                "resourceType", resourceType
        ));
        return toTaskResponse(selectTaskRow(taskId));
    }

    @Override
    @Transactional
    public OperationTaskResponse updateOperationTaskStatus(
            Authentication authentication,
            UUID taskId,
            OperationTaskStatusUpdateRequest request
    ) {
        AuthenticatedUserDetails actor = selectRequiredPrincipal(authentication);
        validateOperatingRole(actor, "운영 업무 상태 변경 권한이 없습니다.");
        OperationTaskRow before = selectTaskRow(taskId);
        String afterStatusCode = normalizeRequiredCode("statusCode", request.statusCode(), TASK_STATUS_CODES);
        validateTaskTransition(before.statusCode(), afterStatusCode);
        operationDao.updateOperationTaskStatus(new OperationTaskStatusCommand(taskId, afterStatusCode, actor.userId()));
        insertAudit(actor.userId(), "OPERATION_TASK_STATUS_UPDATE", "OPERATION_TASK", taskId, metadata(
                "beforeStatusCode", before.statusCode(),
                "afterStatusCode", afterStatusCode,
                "taskTypeCode", before.taskTypeCode()
        ));
        return toTaskResponse(selectTaskRow(taskId));
    }

    @Override
    @Transactional
    public OperationTaskCommentResponse insertOperationTaskComment(
            Authentication authentication,
            UUID taskId,
            OperationTaskCommentCreateRequest request
    ) {
        AuthenticatedUserDetails actor = selectRequiredPrincipal(authentication);
        validateOperatingRole(actor, "운영 업무 댓글 권한이 없습니다.");
        selectTaskRow(taskId);
        UUID commentId = UUID.randomUUID();
        operationDao.insertOperationTaskComment(new OperationTaskCommentInsertCommand(
                commentId,
                taskId,
                actor.userId(),
                trimRequired(request.commentText(), "댓글 내용을 입력하세요.")
        ));
        insertAudit(actor.userId(), "OPERATION_TASK_COMMENT_CREATE", "OPERATION_TASK", taskId, metadata(
                "commentId", commentId.toString(),
                "authorUserId", actor.userId().toString(),
                "commentProvided", "true"
        ));
        return toCommentResponse(operationDao.selectOperationTaskCommentDetails(commentId));
    }

    @Override
    @Transactional
    public OperationTaskAssignmentResponse insertOperationTaskAssignment(
            Authentication authentication,
            UUID taskId,
            OperationTaskAssignmentCreateRequest request
    ) {
        AuthenticatedUserDetails actor = selectRequiredPrincipal(authentication);
        validateOperatingRole(actor, "운영 업무 담당자 배정 권한이 없습니다.");
        selectTaskRow(taskId);
        OperationTaskAssignmentRow row = insertAssignment(taskId, request.assigneeUserId(), actor.userId());
        insertAudit(actor.userId(), "OPERATION_TASK_ASSIGNMENT_CREATE", "OPERATION_TASK", taskId, metadata(
                "assignmentId", row.assignmentId().toString(),
                "assigneeUserId", row.assigneeUserId().toString(),
                "statusCode", row.statusCode()
        ));
        return toAssignmentResponse(row);
    }

    private OperationTaskAssignmentRow insertAssignment(UUID taskId, UUID assigneeUserId, UUID actorUserId) {
        validateUserExists(assigneeUserId);
        UUID assignmentId = UUID.randomUUID();
        operationDao.insertOperationTaskAssignment(new OperationTaskAssignmentInsertCommand(
                assignmentId,
                taskId,
                assigneeUserId,
                actorUserId
        ));
        return operationDao.selectOperationTaskAssignmentDetails(assignmentId);
    }

    private void validateTaskTransition(String beforeStatusCode, String afterStatusCode) {
        if ("DONE".equals(beforeStatusCode) || "CANCELED".equals(beforeStatusCode)) {
            throw new ApiException(ErrorCode.INVALID_STATUS_TRANSITION, HttpStatus.BAD_REQUEST, "완료 또는 취소된 업무는 변경할 수 없습니다.");
        }
        if ("OPEN".equals(beforeStatusCode) && Set.of("OPEN", "IN_PROGRESS", "WAITING", "DONE", "CANCELED")
                .contains(afterStatusCode)) {
            return;
        }
        if (Set.of("IN_PROGRESS", "WAITING").contains(beforeStatusCode)
                && Set.of("IN_PROGRESS", "WAITING", "DONE", "CANCELED").contains(afterStatusCode)) {
            return;
        }
        throw new ApiException(ErrorCode.INVALID_STATUS_TRANSITION, HttpStatus.BAD_REQUEST, "허용되지 않는 운영 업무 상태 변경입니다.");
    }

    private NotificationTemplateRow selectTemplateIfRequested(String templateCode, String channelCode) {
        String normalizedTemplateCode = normalizeOptionalCode(templateCode);
        if (normalizedTemplateCode == null) {
            return null;
        }
        NotificationTemplateRow row = operationDao.selectNotificationTemplateByCode(normalizedTemplateCode, channelCode);
        if (row == null || !row.active()) {
            throw notFound("알림 템플릿을 찾을 수 없습니다.");
        }
        return row;
    }

    private String selectProviderCode(String channelCode) {
        return switch (channelCode) {
            case "IN_APP" -> "INTERNAL";
            case "EMAIL" -> "EMAIL";
            case "SMS" -> "SMS";
            case "KAKAO" -> "KAKAO";
            default -> "MANUAL";
        };
    }

    private NotificationMessageRow selectNotificationRow(UUID notificationId) {
        NotificationMessageRow row = operationDao.selectNotificationMessageDetails(notificationId);
        if (row == null) {
            throw notFound("알림을 찾을 수 없습니다.");
        }
        return row;
    }

    private OperationTaskRow selectTaskRow(UUID taskId) {
        OperationTaskRow row = operationDao.selectOperationTaskDetails(taskId);
        if (row == null) {
            throw notFound("운영 업무를 찾을 수 없습니다.");
        }
        return row;
    }

    private void validateUserExists(UUID userId) {
        if (userId == null || operationDao.selectUserCount(userId) == 0) {
            throw notFound("회원을 찾을 수 없습니다.");
        }
    }

    private String normalizeResourceType(String resourceType) {
        String normalizedResourceType = normalizeOptionalCode(resourceType);
        if (normalizedResourceType == null) {
            return "GENERAL";
        }
        validateOptionalCode("resourceType", normalizedResourceType, RESOURCE_TYPES);
        return normalizedResourceType;
    }

    private void validatePageRequest(int page, int size) {
        if (page < 1 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new ApiException(ErrorCode.INVALID_PAGE_REQUEST, HttpStatus.BAD_REQUEST, "페이지 요청 값이 올바르지 않습니다.");
        }
    }

    private String normalizeRequiredCode(String fieldName, String value, Set<String> allowedValues) {
        String code = normalizeOptionalCode(value);
        if (code == null || !allowedValues.contains(code)) {
            throw validationFailed(fieldName + " 값이 올바르지 않습니다.");
        }
        return code;
    }

    private void validateOptionalCode(String fieldName, String value, Set<String> allowedValues) {
        if (value != null && !allowedValues.contains(value)) {
            throw validationFailed(fieldName + " 값이 올바르지 않습니다.");
        }
    }

    private String normalizeOptionalCode(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed.toUpperCase(Locale.ROOT);
    }

    private String trimTitle(String value) {
        String title = trimRequired(value, "제목을 입력하세요.");
        return title.length() > MAX_TITLE_LENGTH ? title.substring(0, MAX_TITLE_LENGTH) : title;
    }

    private String trimRequired(String value, String message) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw validationFailed(message);
        }
        return trimmed;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private AuthenticatedUserDetails selectRequiredPrincipal(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ApiException(ErrorCode.AUTH_REQUIRED, HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
        }
        if (authentication.getPrincipal() instanceof AuthenticatedUserDetails principal) {
            return principal;
        }
        throw new ApiException(ErrorCode.AUTH_REQUIRED, HttpStatus.UNAUTHORIZED, "DB 인증 사용자만 사용할 수 있습니다.");
    }

    private void validateOperatingRole(AuthenticatedUserDetails actor, String message) {
        if (!hasOperatingRole(actor)) {
            throw new ApiException(ErrorCode.AUTH_FORBIDDEN, HttpStatus.FORBIDDEN, message);
        }
    }

    private boolean hasOperatingRole(AuthenticatedUserDetails actor) {
        return actor.roles().stream().anyMatch(OPERATING_ROLES::contains);
    }

    private NotificationMessageResponse toNotificationResponse(NotificationMessageRow row) {
        return new NotificationMessageResponse(
                row.notificationId(),
                row.recipientUserId(),
                row.channelCode(),
                row.title(),
                row.body(),
                row.statusCode(),
                row.resourceType(),
                row.resourceId(),
                row.readAt(),
                row.sentAt(),
                row.createdAt()
        );
    }

    private OperationTaskResponse toTaskResponse(OperationTaskRow row) {
        return new OperationTaskResponse(
                row.taskId(),
                row.taskTypeCode(),
                row.statusCode(),
                row.priorityCode(),
                row.title(),
                row.description(),
                row.resourceType(),
                row.resourceId(),
                row.dueAt(),
                row.completedAt(),
                row.createdAt(),
                row.updatedAt()
        );
    }

    private OperationTaskCommentResponse toCommentResponse(OperationTaskCommentRow row) {
        return new OperationTaskCommentResponse(
                row.commentId(),
                row.taskId(),
                row.authorUserId(),
                row.commentText(),
                row.createdAt()
        );
    }

    private OperationTaskAssignmentResponse toAssignmentResponse(OperationTaskAssignmentRow row) {
        return new OperationTaskAssignmentResponse(
                row.assignmentId(),
                row.taskId(),
                row.assigneeUserId(),
                row.statusCode(),
                row.assignedBy(),
                row.assignedAt(),
                row.completedAt()
        );
    }

    private void insertAudit(UUID actorUserId, String actionCode, String resourceType, UUID resourceId, String metadataJson) {
        operationDao.insertAuditLog(new AuditLogCommand(
                actorUserId,
                actionCode,
                resourceType,
                resourceId,
                "SUCCESS",
                metadataJson
        ));
    }

    private String metadata(String key1, String value1, String key2, String value2, String key3, String value3) {
        return "{\"" + key1 + "\":\"" + safeValue(value1) + "\",\""
                + key2 + "\":\"" + safeValue(value2) + "\",\""
                + key3 + "\":\"" + safeValue(value3) + "\"}";
    }

    private String safeValue(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private ApiException validationFailed(String message) {
        return new ApiException(ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST, message);
    }

    private ApiException notFound(String message) {
        return new ApiException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, message);
    }
}
