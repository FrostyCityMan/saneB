package com.saneb.domain.auditlog.dao;

import com.saneb.domain.auditlog.vo.AuditLogDetailsRow;
import com.saneb.domain.auditlog.vo.AuditLogSearchCondition;
import com.saneb.domain.auditlog.vo.AuditLogSummaryRow;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AuditLogDao {

    List<AuditLogSummaryRow> selectAuditLogList(AuditLogSearchCondition condition);

    long selectAuditLogCount(AuditLogSearchCondition condition);

    AuditLogDetailsRow selectAuditLogDetails(@Param("auditLogId") UUID auditLogId);
}
