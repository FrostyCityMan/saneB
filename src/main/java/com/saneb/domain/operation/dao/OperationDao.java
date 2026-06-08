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
import com.saneb.domain.operation.vo.StalledApplicationProgressRow;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface OperationDao {

    long selectUserCount(UUID userId);

    NotificationTemplateRow selectNotificationTemplateByCode(
            @Param("templateCode") String templateCode,
            @Param("channelCode") String channelCode
    );

    List<NotificationMessageRow> selectNotificationMessageList(NotificationMessageSearchCondition condition);

    long selectNotificationMessageCount(NotificationMessageSearchCondition condition);

    NotificationMessageRow selectNotificationMessageDetails(UUID notificationId);

    void insertNotificationMessage(NotificationMessageInsertCommand command);

    int updateNotificationMessageRead(@Param("notificationId") UUID notificationId);

    void insertNotificationDeliveryLog(NotificationDeliveryLogCommand command);

    List<StalledApplicationProgressRow> selectStalledApplicationProgressList(
            @Param("reminderTypeCode") String reminderTypeCode,
            @Param("thresholdAt") OffsetDateTime thresholdAt,
            @Param("limit") int limit
    );

    void insertProgressReminderLog(ProgressReminderInsertCommand command);

    List<OperationTaskRow> selectOperationTaskList(OperationTaskSearchCondition condition);

    long selectOperationTaskCount(OperationTaskSearchCondition condition);

    OperationTaskRow selectOperationTaskDetails(UUID taskId);

    void insertOperationTask(OperationTaskInsertCommand command);

    long selectOpenOperationTaskCount(
            @Param("taskTypeCode") String taskTypeCode,
            @Param("resourceType") String resourceType,
            @Param("resourceId") UUID resourceId
    );

    int updateOperationTaskStatus(OperationTaskStatusCommand command);

    OperationTaskCommentRow selectOperationTaskCommentDetails(UUID commentId);

    void insertOperationTaskComment(OperationTaskCommentInsertCommand command);

    OperationTaskAssignmentRow selectOperationTaskAssignmentDetails(UUID assignmentId);

    void insertOperationTaskAssignment(OperationTaskAssignmentInsertCommand command);

    void insertAuditLog(AuditLogCommand command);
}
