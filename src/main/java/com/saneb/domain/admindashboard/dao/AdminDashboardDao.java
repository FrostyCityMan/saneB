/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: AdminDashboardDao.java
 * 작성자: 김도훈
 *
 */

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

    /**
     * 업무 데이터를 조회합니다.
     *
     * @return 처리 결과
     */
    AdminUserSummaryRow selectUserSummary();

    /**
     * 업무 데이터를 조회합니다.
     *
     * @return 처리 결과
     */
    AdminAnnouncementSummaryRow selectAnnouncementSummary();

    /**
     * 업무 데이터를 조회합니다.
     *
     * @return 처리 결과
     */
    List<AdminStatusCountRow> selectPartnerVerificationStatusCountList();

    /**
     * 업무 데이터를 조회합니다.
     *
     * @return 처리 결과
     */
    List<AdminStatusCountRow> selectMatchingCaseStatusCountList();

    /**
     * 업무 데이터를 조회합니다.
     *
     * @return 처리 결과
     */
    AdminApplicationProgressSummaryRow selectApplicationProgressSummary();

    /**
     * 업무 데이터를 조회합니다.
     *
     * @return 처리 결과
     */
    AdminAuditSummaryRow selectAuditSummary();
}
