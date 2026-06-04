package com.saneb.domain.admindashboard.dao;

import com.saneb.domain.admindashboard.vo.AdminApplicationProgressSummaryRow;
import com.saneb.domain.admindashboard.vo.AdminAnnouncementSummaryRow;
import com.saneb.domain.admindashboard.vo.AdminAuditSummaryRow;
import com.saneb.domain.admindashboard.vo.AdminStatusCountRow;
import com.saneb.domain.admindashboard.vo.AdminUserSummaryRow;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AdminDashboardDao {

    AdminUserSummaryRow selectUserSummary();

    AdminAnnouncementSummaryRow selectAnnouncementSummary();

    List<AdminStatusCountRow> selectPartnerVerificationStatusCountList();

    List<AdminStatusCountRow> selectMatchingCaseStatusCountList();

    AdminApplicationProgressSummaryRow selectApplicationProgressSummary();

    AdminAuditSummaryRow selectAuditSummary();
}
