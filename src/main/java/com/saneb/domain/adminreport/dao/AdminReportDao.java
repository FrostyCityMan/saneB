package com.saneb.domain.adminreport.dao;

import com.saneb.domain.adminreport.vo.AdminReportSnapshotInsertCommand;
import com.saneb.domain.adminreport.vo.AdminReportSummaryRow;
import com.saneb.domain.adminreport.vo.AuditLogCommand;
import com.saneb.domain.adminreport.vo.ReportExportInsertCommand;
import com.saneb.domain.adminreport.vo.ReportExportRow;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AdminReportDao {

    AdminReportSummaryRow selectAdminReportSummary();

    void insertReportExport(ReportExportInsertCommand command);

    ReportExportRow selectReportExportDetails(UUID exportId);

    void insertAdminReportSnapshot(AdminReportSnapshotInsertCommand command);

    void insertAuditLog(AuditLogCommand command);
}
