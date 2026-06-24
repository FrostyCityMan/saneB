/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: OperationServiceImplTest.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.operation.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.saneb.common.error.ApiException;
import com.saneb.common.error.ErrorCode;
import com.saneb.domain.auth.vo.AuthUserDetailsRow;
import com.saneb.domain.auth.vo.AuthenticatedUserDetails;
import com.saneb.domain.operation.dao.OperationDao;
import com.saneb.domain.operation.dto.NotificationSendRequest;
import com.saneb.domain.operation.dto.OperationTaskCreateRequest;
import com.saneb.domain.operation.dto.OperationTaskStatusUpdateRequest;
import com.saneb.domain.operation.vo.NotificationDeliveryLogCommand;
import com.saneb.domain.operation.vo.NotificationMessageInsertCommand;
import com.saneb.domain.operation.vo.NotificationMessageRow;
import com.saneb.domain.operation.vo.OperationTaskAssignmentInsertCommand;
import com.saneb.domain.operation.vo.OperationTaskInsertCommand;
import com.saneb.domain.operation.vo.OperationTaskRow;
import com.saneb.domain.operation.vo.OperationTaskStatusCommand;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class OperationServiceImplTest {

    private static final UUID USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000002");
    private static final UUID TASK_ID = UUID.fromString("91000000-0000-0000-0000-000000000001");
    private static final UUID NOTIFICATION_ID = UUID.fromString("91000000-0000-0000-0000-000000000002");
    private static final UUID ASSIGNMENT_ID = UUID.fromString("91000000-0000-0000-0000-000000000003");
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-06-08T10:00:00+09:00");

    @Mock
    private OperationDao operationDao;

    private OperationServiceImpl operationService;

    /**
     * 업무 처리를 수행합니다.
     */
    @BeforeEach
    void setUp() {
        operationService = new OperationServiceImpl(operationDao);
    }

    /**
     * 업무 데이터를 등록합니다.
     */
    @Test
    void insertAdminNotificationCreatesInAppMessageAndDeliveryLog() {
        when(operationDao.selectUserCount(USER_ID)).thenReturn(1L);
        when(operationDao.selectNotificationMessageDetails(any())).thenReturn(notification(false));

        var response = operationService.insertAdminNotification(
                authentication(List.of("OPERATOR")),
                new NotificationSendRequest(
                        USER_ID,
                        null,
                        "in_app",
                        "보완 요청",
                        "서류 보완이 필요합니다.",
                        "application_progress",
                        null
                )
        );

        ArgumentCaptor<NotificationMessageInsertCommand> messageCaptor =
                ArgumentCaptor.forClass(NotificationMessageInsertCommand.class);
        verify(operationDao).insertNotificationMessage(messageCaptor.capture());
        assertThat(messageCaptor.getValue().channelCode()).isEqualTo("IN_APP");
        assertThat(messageCaptor.getValue().statusCode()).isEqualTo("SENT");

        ArgumentCaptor<NotificationDeliveryLogCommand> logCaptor =
                ArgumentCaptor.forClass(NotificationDeliveryLogCommand.class);
        verify(operationDao).insertNotificationDeliveryLog(logCaptor.capture());
        assertThat(logCaptor.getValue().providerCode()).isEqualTo("INTERNAL");
        assertThat(logCaptor.getValue().deliveryStatusCode()).isEqualTo("SUCCESS");
        assertThat(response.statusCode()).isEqualTo("SENT");
    }

    /**
     * 업무 데이터를 수정합니다.
     */
    @Test
    void updateNotificationReadRejectsDifferentRecipient() {
        when(operationDao.selectNotificationMessageDetails(NOTIFICATION_ID))
                .thenReturn(new NotificationMessageRow(
                        NOTIFICATION_ID,
                        UUID.fromString("10000000-0000-0000-0000-000000000099"),
                        "IN_APP",
                        "제목",
                        "내용",
                        "SENT",
                        "GENERAL",
                        null,
                        null,
                        NOW,
                        NOW
                ));

        assertThatThrownBy(() -> operationService.updateNotificationRead(
                authentication(List.of("USER")),
                NOTIFICATION_ID
        ))
                .isInstanceOf(ApiException.class)
                .extracting(error -> ((ApiException) error).errorCode())
                .isEqualTo(ErrorCode.AUTH_FORBIDDEN);
    }

    /**
     * 업무 데이터를 등록합니다.
     */
    @Test
    void insertOperationTaskCreatesAssignments() {
        when(operationDao.selectUserCount(USER_ID)).thenReturn(1L);
        when(operationDao.selectOperationTaskAssignmentDetails(any())).thenReturn(assignment());
        when(operationDao.selectOperationTaskDetails(any())).thenReturn(task("OPEN"));

        var response = operationService.insertOperationTask(
                authentication(List.of("ADMIN")),
                new OperationTaskCreateRequest(
                        "supplement_request",
                        "high",
                        "보완 요청 확인",
                        "사용자 보완 요청 확인",
                        "application_progress",
                        null,
                        null,
                        List.of(USER_ID)
                )
        );

        ArgumentCaptor<OperationTaskInsertCommand> taskCaptor = ArgumentCaptor.forClass(OperationTaskInsertCommand.class);
        verify(operationDao).insertOperationTask(taskCaptor.capture());
        assertThat(taskCaptor.getValue().taskTypeCode()).isEqualTo("SUPPLEMENT_REQUEST");
        assertThat(taskCaptor.getValue().priorityCode()).isEqualTo("HIGH");
        verify(operationDao).insertOperationTaskAssignment(any());
        assertThat(response.statusCode()).isEqualTo("OPEN");
    }

    /**
     * 업무 데이터를 수정합니다.
     */
    @Test
    void updateOperationTaskStatusRejectsDoneTask() {
        when(operationDao.selectOperationTaskDetails(TASK_ID)).thenReturn(task("DONE"));

        assertThatThrownBy(() -> operationService.updateOperationTaskStatus(
                authentication(List.of("OPERATOR")),
                TASK_ID,
                new OperationTaskStatusUpdateRequest("IN_PROGRESS")
        ))
                .isInstanceOf(ApiException.class)
                .extracting(error -> ((ApiException) error).errorCode())
                .isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION);
    }

    /**
     * 업무 데이터를 수정합니다.
     */
    @Test
    void updateOperationTaskStatusAllowsOpenToInProgress() {
        when(operationDao.selectOperationTaskDetails(TASK_ID)).thenReturn(task("OPEN"), task("IN_PROGRESS"));

        var response = operationService.updateOperationTaskStatus(
                authentication(List.of("OPERATOR")),
                TASK_ID,
                new OperationTaskStatusUpdateRequest("in_progress")
        );

        ArgumentCaptor<OperationTaskStatusCommand> captor = ArgumentCaptor.forClass(OperationTaskStatusCommand.class);
        verify(operationDao).updateOperationTaskStatus(captor.capture());
        assertThat(captor.getValue().statusCode()).isEqualTo("IN_PROGRESS");
        assertThat(response.statusCode()).isEqualTo("IN_PROGRESS");
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param read 입력 값
     *
     * @return 처리 결과
     */
    private NotificationMessageRow notification(boolean read) {
        return new NotificationMessageRow(
                NOTIFICATION_ID,
                USER_ID,
                "IN_APP",
                "보완 요청",
                "서류 보완이 필요합니다.",
                "SENT",
                "APPLICATION_PROGRESS",
                null,
                read ? NOW : null,
                NOW,
                NOW
        );
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param statusCode 입력 값
     *
     * @return 처리 결과
     */
    private OperationTaskRow task(String statusCode) {
        return new OperationTaskRow(
                TASK_ID,
                "SUPPLEMENT_REQUEST",
                statusCode,
                "HIGH",
                "보완 요청 확인",
                "사용자 보완 요청 확인",
                "APPLICATION_PROGRESS",
                null,
                null,
                "DONE".equals(statusCode) ? NOW : null,
                NOW,
                NOW
        );
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @return 처리 결과
     */
    private com.saneb.domain.operation.vo.OperationTaskAssignmentRow assignment() {
        return new com.saneb.domain.operation.vo.OperationTaskAssignmentRow(
                ASSIGNMENT_ID,
                TASK_ID,
                USER_ID,
                "ASSIGNED",
                USER_ID,
                NOW,
                null
        );
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param roles 입력 값
     *
     * @return 처리 결과
     */
    private UsernamePasswordAuthenticationToken authentication(List<String> roles) {
        AuthenticatedUserDetails principal = new AuthenticatedUserDetails(
                new AuthUserDetailsRow(
                        USER_ID,
                        "user01",
                        "hash",
                        "Local User",
                        "ACTIVE",
                        false,
                        null,
                        null,
                        null
                ),
                roles
        );
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }
}
