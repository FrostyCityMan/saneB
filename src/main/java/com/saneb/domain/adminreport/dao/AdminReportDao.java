/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: AdminReportDao.java
 * 작성자: 김도훈
 *
 */

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

    /**
     * 업무 데이터를 조회합니다.
     *
     * @return 처리 결과
     */
    AdminReportSummaryRow selectAdminReportSummary();

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param command 입력 값
     */
    void insertReportExport(ReportExportInsertCommand command);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param exportId 입력 값
     *
     * @return 처리 결과
     */
    ReportExportRow selectReportExportDetails(UUID exportId);

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param command 입력 값
     */
    void insertAdminReportSnapshot(AdminReportSnapshotInsertCommand command);

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param command 입력 값
     */
    void insertAuditLog(AuditLogCommand command);
}
