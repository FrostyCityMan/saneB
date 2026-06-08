package com.saneb.domain.operation.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.saneb.common.response.PageResponse;
import com.saneb.domain.auth.vo.AuthUserDetailsRow;
import com.saneb.domain.auth.vo.AuthenticatedUserDetails;
import com.saneb.domain.operation.dto.NotificationMessageResponse;
import com.saneb.domain.operation.dto.OperationTaskAssignmentResponse;
import com.saneb.domain.operation.dto.OperationTaskCommentResponse;
import com.saneb.domain.operation.dto.OperationTaskResponse;
import com.saneb.domain.operation.service.OperationService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "spring.flyway.enabled=false")
@AutoConfigureMockMvc
class OperationControllerSmokeTest {

    private static final UUID USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000002");
    private static final UUID TASK_ID = UUID.fromString("91000000-0000-0000-0000-000000000001");
    private static final UUID NOTIFICATION_ID = UUID.fromString("91000000-0000-0000-0000-000000000002");
    private static final UUID COMMENT_ID = UUID.fromString("91000000-0000-0000-0000-000000000003");
    private static final UUID ASSIGNMENT_ID = UUID.fromString("91000000-0000-0000-0000-000000000004");
    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-06-08T10:00:00+09:00");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OperationService operationService;

    @BeforeEach
    void setUp() {
        when(operationService.selectMyNotificationList(any(), any(), eq(1), eq(20)))
                .thenReturn(PageResponse.of(List.of(notification(false)), 1, 20, 1));
        when(operationService.updateNotificationRead(any(), eq(NOTIFICATION_ID))).thenReturn(notification(true));
        when(operationService.insertAdminNotification(any(), any())).thenReturn(notification(false));
        when(operationService.selectOperationTaskList(any(), any(), any(), any(), eq(1), eq(20)))
                .thenReturn(PageResponse.of(List.of(task("OPEN")), 1, 20, 1));
        when(operationService.insertOperationTask(any(), any())).thenReturn(task("OPEN"));
        when(operationService.updateOperationTaskStatus(any(), eq(TASK_ID), any())).thenReturn(task("IN_PROGRESS"));
        when(operationService.insertOperationTaskComment(any(), eq(TASK_ID), any())).thenReturn(comment());
        when(operationService.insertOperationTaskAssignment(any(), eq(TASK_ID), any())).thenReturn(assignment());
    }

    @Test
    void selectMyNotificationListReturnsPagedApiResponse() throws Exception {
        mockMvc.perform(get("/api/v1/notifications/me")
                        .with(user(userPrincipal()))
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].notificationId").value(NOTIFICATION_ID.toString()));
    }

    @Test
    void updateNotificationReadReturnsApiResponse() throws Exception {
        mockMvc.perform(patch("/api/v1/notifications/{notificationId}/read", NOTIFICATION_ID)
                        .with(user(userPrincipal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.readAt").exists());
    }

    @Test
    void insertAdminNotificationRejectsUserRole() throws Exception {
        mockMvc.perform(post("/api/v1/admin/notifications/send")
                        .with(user(userPrincipal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(notificationRequest()))
                .andExpect(status().isForbidden());
    }

    @Test
    void insertAdminNotificationReturnsApiResponse() throws Exception {
        mockMvc.perform(post("/api/v1/admin/notifications/send")
                        .with(user(operatorPrincipal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(notificationRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.channelCode").value("IN_APP"));
    }

    @Test
    void insertOperationTaskReturnsApiResponse() throws Exception {
        mockMvc.perform(post("/api/v1/operation-tasks")
                        .with(user(operatorPrincipal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "taskTypeCode": "SUPPLEMENT_REQUEST",
                                  "priorityCode": "HIGH",
                                  "title": "보완 요청 확인",
                                  "description": "사용자 보완 요청 확인",
                                  "resourceType": "APPLICATION_PROGRESS",
                                  "assigneeUserIds": ["%s"]
                                }
                                """.formatted(USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.taskId").value(TASK_ID.toString()));
    }

    @Test
    void updateOperationTaskStatusReturnsApiResponse() throws Exception {
        mockMvc.perform(patch("/api/v1/operation-tasks/{taskId}/status", TASK_ID)
                        .with(user(operatorPrincipal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "statusCode": "IN_PROGRESS"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.statusCode").value("IN_PROGRESS"));
    }

    @Test
    void insertOperationTaskCommentReturnsApiResponse() throws Exception {
        mockMvc.perform(post("/api/v1/operation-tasks/{taskId}/comments", TASK_ID)
                        .with(user(operatorPrincipal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "commentText": "확인했습니다."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.commentId").value(COMMENT_ID.toString()));
    }

    @Test
    void insertOperationTaskAssignmentReturnsApiResponse() throws Exception {
        mockMvc.perform(post("/api/v1/operation-tasks/{taskId}/assignments", TASK_ID)
                        .with(user(operatorPrincipal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "assigneeUserId": "%s"
                                }
                                """.formatted(USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assignmentId").value(ASSIGNMENT_ID.toString()));
    }

    private String notificationRequest() {
        return """
                {
                  "recipientUserId": "%s",
                  "channelCode": "IN_APP",
                  "title": "보완 요청",
                  "body": "서류 보완이 필요합니다.",
                  "resourceType": "APPLICATION_PROGRESS"
                }
                """.formatted(USER_ID);
    }

    private NotificationMessageResponse notification(boolean read) {
        return new NotificationMessageResponse(
                NOTIFICATION_ID,
                USER_ID,
                "IN_APP",
                "보완 요청",
                "서류 보완이 필요합니다.",
                "SENT",
                "APPLICATION_PROGRESS",
                null,
                read ? CREATED_AT : null,
                CREATED_AT,
                CREATED_AT
        );
    }

    private OperationTaskResponse task(String statusCode) {
        return new OperationTaskResponse(
                TASK_ID,
                "SUPPLEMENT_REQUEST",
                statusCode,
                "HIGH",
                "보완 요청 확인",
                "사용자 보완 요청 확인",
                "APPLICATION_PROGRESS",
                null,
                null,
                null,
                CREATED_AT,
                CREATED_AT
        );
    }

    private OperationTaskCommentResponse comment() {
        return new OperationTaskCommentResponse(COMMENT_ID, TASK_ID, USER_ID, "확인했습니다.", CREATED_AT);
    }

    private OperationTaskAssignmentResponse assignment() {
        return new OperationTaskAssignmentResponse(ASSIGNMENT_ID, TASK_ID, USER_ID, "ASSIGNED", USER_ID, CREATED_AT, null);
    }

    private AuthenticatedUserDetails userPrincipal() {
        return principal("local_user", List.of("USER"));
    }

    private AuthenticatedUserDetails operatorPrincipal() {
        return principal("local_operator", List.of("OPERATOR"));
    }

    private AuthenticatedUserDetails principal(String loginId, List<String> roles) {
        return new AuthenticatedUserDetails(
                new AuthUserDetailsRow(
                        USER_ID,
                        loginId,
                        "password-hash",
                        "Local User",
                        "ACTIVE",
                        false,
                        null,
                        null,
                        null
                ),
                roles
        );
    }
}
