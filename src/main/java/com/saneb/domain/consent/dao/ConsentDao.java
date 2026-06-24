/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: ConsentDao.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.consent.dao;

import com.saneb.domain.consent.vo.ConsentVersionRow;
import com.saneb.domain.consent.vo.UserConsentInsertCommand;
import com.saneb.domain.consent.vo.UserConsentRow;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ConsentDao {

    /**
     * 업무 데이터를 조회합니다.
     *
     * @return 처리 결과
     */
    List<ConsentVersionRow> selectCurrentConsentVersionList();

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param consentCode 입력 값
     *
     * @return 처리 결과
     */
    ConsentVersionRow selectCurrentConsentVersionDetailsByCode(@Param("consentCode") String consentCode);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param userId 입력 값
     *
     * @return 처리 결과
     */
    List<UserConsentRow> selectUserConsentList(@Param("userId") UUID userId);

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param command 입력 값
     *
     * @return 처리 결과
     */
    UUID insertUserConsent(UserConsentInsertCommand command);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param userConsentId 입력 값
     *
     * @return 처리 결과
     */
    UserConsentRow selectUserConsentDetails(@Param("userConsentId") UUID userConsentId);
}
