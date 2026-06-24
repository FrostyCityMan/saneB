/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: PartnerVerificationDao.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.partnerverification.dao;

import com.saneb.domain.partnerverification.vo.AuditLogCommand;
import com.saneb.domain.partnerverification.vo.PartnerVerificationCreateCommand;
import com.saneb.domain.partnerverification.vo.PartnerVerificationRow;
import com.saneb.domain.partnerverification.vo.PartnerVerificationSearchCondition;
import com.saneb.domain.partnerverification.vo.PartnerVerificationStatusCommand;
import com.saneb.domain.partnerverification.vo.VerificationBusinessValuesCommand;
import com.saneb.domain.partnerverification.vo.VerificationBusinessValuesRow;
import com.saneb.domain.partnerverification.vo.VerificationDocumentCommand;
import com.saneb.domain.partnerverification.vo.VerificationDocumentRow;
import com.saneb.domain.partnerverification.vo.VerificationFamilyValueCommand;
import com.saneb.domain.partnerverification.vo.VerificationFamilyValueRow;
import com.saneb.domain.partnerverification.vo.VerificationMemberValuesCommand;
import com.saneb.domain.partnerverification.vo.VerificationMemberValuesRow;
import com.saneb.domain.partnerverification.vo.VerificationRestrictionFlagCommand;
import com.saneb.domain.partnerverification.vo.VerificationRestrictionFlagRow;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PartnerVerificationDao {

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param condition 입력 값
     *
     * @return 처리 결과
     */
    List<PartnerVerificationRow> selectPartnerVerificationList(PartnerVerificationSearchCondition condition);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param condition 입력 값
     *
     * @return 처리 결과
     */
    long selectPartnerVerificationCount(PartnerVerificationSearchCondition condition);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param verificationId 입력 값
     *
     * @return 처리 결과
     */
    PartnerVerificationRow selectPartnerVerificationDetails(@Param("verificationId") UUID verificationId);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param verificationId 입력 값
     *
     * @return 처리 결과
     */
    VerificationMemberValuesRow selectVerificationMemberValues(@Param("verificationId") UUID verificationId);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param verificationId 입력 값
     *
     * @return 처리 결과
     */
    VerificationBusinessValuesRow selectVerificationBusinessValues(@Param("verificationId") UUID verificationId);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param verificationId 입력 값
     *
     * @return 처리 결과
     */
    List<VerificationFamilyValueRow> selectVerificationFamilyValueList(@Param("verificationId") UUID verificationId);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param verificationId 입력 값
     *
     * @return 처리 결과
     */
    List<VerificationRestrictionFlagRow> selectVerificationRestrictionFlagList(@Param("verificationId") UUID verificationId);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param verificationId 입력 값
     *
     * @return 처리 결과
     */
    List<VerificationDocumentRow> selectVerificationDocumentList(@Param("verificationId") UUID verificationId);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param userId 입력 값
     *
     * @return 처리 결과
     */
    long selectUserCountById(@Param("userId") UUID userId);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param businessProfileId 입력 값
     *
     * @return 처리 결과
     */
    long selectBusinessProfileCountById(@Param("businessProfileId") UUID businessProfileId);

    /**
     * 업무 데이터를 수정합니다.
     *
     * @param memberUserId 입력 값
     *
     * @param actorUserId 입력 값
     */
    void updateCurrentVerificationInactiveByMemberUserId(
            @Param("memberUserId") UUID memberUserId,
            @Param("actorUserId") UUID actorUserId
    );

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param command 입력 값
     */
    void insertPartnerVerification(PartnerVerificationCreateCommand command);

    /**
     * 업무 데이터를 삭제합니다.
     *
     * @param verificationId 입력 값
     */
    void deleteVerificationMemberValues(@Param("verificationId") UUID verificationId);

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param command 입력 값
     */
    void insertVerificationMemberValues(VerificationMemberValuesCommand command);

    /**
     * 업무 데이터를 삭제합니다.
     *
     * @param verificationId 입력 값
     */
    void deleteVerificationBusinessValues(@Param("verificationId") UUID verificationId);

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param command 입력 값
     */
    void insertVerificationBusinessValues(VerificationBusinessValuesCommand command);

    /**
     * 업무 데이터를 삭제합니다.
     *
     * @param verificationId 입력 값
     */
    void deleteVerificationFamilyValues(@Param("verificationId") UUID verificationId);

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param command 입력 값
     */
    void insertVerificationFamilyValue(VerificationFamilyValueCommand command);

    /**
     * 업무 데이터를 삭제합니다.
     *
     * @param verificationId 입력 값
     */
    void deleteVerificationRestrictionFlags(@Param("verificationId") UUID verificationId);

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param command 입력 값
     */
    void insertVerificationRestrictionFlag(VerificationRestrictionFlagCommand command);

    /**
     * 업무 데이터를 삭제합니다.
     *
     * @param verificationId 입력 값
     */
    void deleteVerificationDocuments(@Param("verificationId") UUID verificationId);

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param command 입력 값
     */
    void insertVerificationDocument(VerificationDocumentCommand command);

    /**
     * 업무 데이터를 수정합니다.
     *
     * @param command 입력 값
     *
     * @return 처리 결과
     */
    int updatePartnerVerificationStatus(PartnerVerificationStatusCommand command);

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param command 입력 값
     */
    void insertAuditLog(AuditLogCommand command);
}
