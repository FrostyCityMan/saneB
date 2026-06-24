/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: DashboardDao.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.dashboard.dao;

import com.saneb.domain.dashboard.vo.DashboardCandidateSummaryRow;
import com.saneb.domain.dashboard.vo.DashboardCurrentStepRow;
import com.saneb.domain.dashboard.vo.DashboardProgressSummaryRow;
import com.saneb.domain.dashboard.vo.DashboardVerificationStatusRow;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DashboardDao {

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param loginId 입력 값
     *
     * @return 처리 결과
     */
    UUID selectUserIdByLoginId(@Param("loginId") String loginId);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param userId 입력 값
     *
     * @return 처리 결과
     */
    DashboardVerificationStatusRow selectCurrentVerificationStatus(@Param("userId") UUID userId);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param userId 입력 값
     *
     * @return 처리 결과
     */
    DashboardCandidateSummaryRow selectCandidateSummary(@Param("userId") UUID userId);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param userId 입력 값
     *
     * @return 처리 결과
     */
    DashboardProgressSummaryRow selectProgressSummary(@Param("userId") UUID userId);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param userId 입력 값
     *
     * @return 처리 결과
     */
    DashboardCurrentStepRow selectCurrentStepDetails(@Param("userId") UUID userId);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param userId 입력 값
     *
     * @return 처리 결과
     */
    long selectBasicInfoSavedCount(@Param("userId") UUID userId);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param userId 입력 값
     *
     * @return 처리 결과
     */
    long selectActiveSubscriptionCount(@Param("userId") UUID userId);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param userId 입력 값
     *
     * @return 처리 결과
     */
    long selectConsultationReservationCount(@Param("userId") UUID userId);
}
