/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: OperationDao.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.operation.dao;

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
import com.saneb.domain.operation.vo.ProgressReminderInsertCommand;
import com.saneb.domain.operation.vo.StatusRefreshTargetUserRow;
import com.saneb.domain.operation.vo.StalledApplicationProgressRow;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface OperationDao {

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param userId 입력 값
     *
     * @return 처리 결과
     */
    long selectUserCount(UUID userId);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param templateCode 입력 값
     *
     * @param channelCode 입력 값
     *
     * @return 처리 결과
     */
    NotificationTemplateRow selectNotificationTemplateByCode(
            @Param("templateCode") String templateCode,
            @Param("channelCode") String channelCode
    );

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param condition 입력 값
     *
     * @return 처리 결과
     */
    List<NotificationMessageRow> selectNotificationMessageList(NotificationMessageSearchCondition condition);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param condition 입력 값
     *
     * @return 처리 결과
     */
    long selectNotificationMessageCount(NotificationMessageSearchCondition condition);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param notificationId 입력 값
     *
     * @return 처리 결과
     */
    NotificationMessageRow selectNotificationMessageDetails(UUID notificationId);

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param command 입력 값
     */
    void insertNotificationMessage(NotificationMessageInsertCommand command);

    /**
     * 업무 데이터를 수정합니다.
     *
     * @param notificationId 입력 값
     *
     * @return 처리 결과
     */
    int updateNotificationMessageRead(@Param("notificationId") UUID notificationId);

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param command 입력 값
     */
    void insertNotificationDeliveryLog(NotificationDeliveryLogCommand command);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param reminderTypeCode 입력 값
     *
     * @param thresholdAt 입력 값
     *
     * @param limit 입력 값
     *
     * @return 처리 결과
     */
    List<StalledApplicationProgressRow> selectStalledApplicationProgressList(
            @Param("reminderTypeCode") String reminderTypeCode,
            @Param("thresholdAt") OffsetDateTime thresholdAt,
            @Param("limit") int limit
    );

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param reminderTypeCode 입력 값
     *
     * @param targetDate 입력 값
     *
     * @param limit 입력 값
     *
     * @return 처리 결과
     */
    List<StalledApplicationProgressRow> selectDeadlineReminderProgressList(
            @Param("reminderTypeCode") String reminderTypeCode,
            @Param("targetDate") LocalDate targetDate,
            @Param("limit") int limit
    );

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param thresholdAt 입력 값
     *
     * @param recentSince 입력 값
     *
     * @param limit 입력 값
     *
     * @return 처리 결과
     */
    List<StatusRefreshTargetUserRow> selectStatusRefreshTargetUserList(
            @Param("thresholdAt") OffsetDateTime thresholdAt,
            @Param("recentSince") OffsetDateTime recentSince,
            @Param("limit") int limit
    );

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param command 입력 값
     */
    void insertProgressReminderLog(ProgressReminderInsertCommand command);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param condition 입력 값
     *
     * @return 처리 결과
     */
    List<OperationTaskRow> selectOperationTaskList(OperationTaskSearchCondition condition);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param condition 입력 값
     *
     * @return 처리 결과
     */
    long selectOperationTaskCount(OperationTaskSearchCondition condition);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param taskId 입력 값
     *
     * @return 처리 결과
     */
    OperationTaskRow selectOperationTaskDetails(UUID taskId);

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param command 입력 값
     */
    void insertOperationTask(OperationTaskInsertCommand command);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param taskTypeCode 입력 값
     *
     * @param resourceType 입력 값
     *
     * @param resourceId 입력 값
     *
     * @return 처리 결과
     */
    long selectOpenOperationTaskCount(
            @Param("taskTypeCode") String taskTypeCode,
            @Param("resourceType") String resourceType,
            @Param("resourceId") UUID resourceId
    );

    /**
     * 업무 데이터를 수정합니다.
     *
     * @param command 입력 값
     *
     * @return 처리 결과
     */
    int updateOperationTaskStatus(OperationTaskStatusCommand command);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param commentId 입력 값
     *
     * @return 처리 결과
     */
    OperationTaskCommentRow selectOperationTaskCommentDetails(UUID commentId);

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param command 입력 값
     */
    void insertOperationTaskComment(OperationTaskCommentInsertCommand command);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param assignmentId 입력 값
     *
     * @return 처리 결과
     */
    OperationTaskAssignmentRow selectOperationTaskAssignmentDetails(UUID assignmentId);

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param command 입력 값
     */
    void insertOperationTaskAssignment(OperationTaskAssignmentInsertCommand command);

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param command 입력 값
     */
    void insertAuditLog(AuditLogCommand command);
}
