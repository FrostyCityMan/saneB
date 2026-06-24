/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: ConsultationDao.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.consultation.dao;

import com.saneb.domain.consultation.vo.ApplicationProgressOwnerRow;
import com.saneb.domain.consultation.vo.AuditLogCommand;
import com.saneb.domain.consultation.vo.ConsultationHistoryCommand;
import com.saneb.domain.consultation.vo.ConsultationReservationInsertCommand;
import com.saneb.domain.consultation.vo.ConsultationReservationRow;
import com.saneb.domain.consultation.vo.ConsultationReservationSearchCondition;
import com.saneb.domain.consultation.vo.ConsultationReservationStatusCommand;
import com.saneb.domain.consultation.vo.ConsultationSlotInsertCommand;
import com.saneb.domain.consultation.vo.ConsultationSlotRow;
import com.saneb.domain.consultation.vo.ConsultationSlotSearchCondition;
import com.saneb.domain.consultation.vo.ConsultationSlotStatusCommand;
import com.saneb.domain.consultation.vo.PartnerVerificationOwnerRow;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ConsultationDao {

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param userId 입력 값
     *
     * @return 처리 결과
     */
    long selectUserCount(@Param("userId") UUID userId);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param publicCode 입력 값
     *
     * @return 처리 결과
     */
    UUID selectUserIdByPublicCode(@Param("publicCode") String publicCode);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param condition 입력 값
     *
     * @return 처리 결과
     */
    List<ConsultationSlotRow> selectConsultationSlotList(ConsultationSlotSearchCondition condition);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param condition 입력 값
     *
     * @return 처리 결과
     */
    long selectConsultationSlotCount(ConsultationSlotSearchCondition condition);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param slotId 입력 값
     *
     * @return 처리 결과
     */
    ConsultationSlotRow selectConsultationSlotDetails(@Param("slotId") UUID slotId);

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param command 입력 값
     */
    void insertConsultationSlot(ConsultationSlotInsertCommand command);

    /**
     * 업무 데이터를 수정합니다.
     *
     * @param command 입력 값
     *
     * @return 처리 결과
     */
    int updateConsultationSlotStatus(ConsultationSlotStatusCommand command);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param condition 입력 값
     *
     * @return 처리 결과
     */
    List<ConsultationReservationRow> selectConsultationReservationList(
            ConsultationReservationSearchCondition condition
    );

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param condition 입력 값
     *
     * @return 처리 결과
     */
    long selectConsultationReservationCount(ConsultationReservationSearchCondition condition);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param reservationId 입력 값
     *
     * @return 처리 결과
     */
    ConsultationReservationRow selectConsultationReservationDetails(@Param("reservationId") UUID reservationId);

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param command 입력 값
     */
    void insertConsultationReservation(ConsultationReservationInsertCommand command);

    /**
     * 업무 데이터를 수정합니다.
     *
     * @param command 입력 값
     *
     * @return 처리 결과
     */
    int updateConsultationReservationStatus(ConsultationReservationStatusCommand command);

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param command 입력 값
     */
    void insertConsultationHistory(ConsultationHistoryCommand command);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param progressId 입력 값
     *
     * @return 처리 결과
     */
    ApplicationProgressOwnerRow selectApplicationProgressOwner(@Param("progressId") UUID progressId);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param verificationId 입력 값
     *
     * @return 처리 결과
     */
    PartnerVerificationOwnerRow selectPartnerVerificationOwner(@Param("verificationId") UUID verificationId);

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param command 입력 값
     */
    void insertAuditLog(AuditLogCommand command);
}
