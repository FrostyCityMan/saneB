/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: AuthDao.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.auth.dao;

import com.saneb.domain.auth.vo.AuthLoginHistoryCommand;
import com.saneb.domain.auth.vo.AuthPasswordUpdateCommand;
import com.saneb.domain.auth.vo.AuthSignupCommand;
import com.saneb.domain.auth.vo.AuthUserDetailsRow;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AuthDao {

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param loginId 입력 값
     *
     * @return 처리 결과
     */
    AuthUserDetailsRow selectAuthUserDetailsByLoginId(@Param("loginId") String loginId);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param phone 입력 값
     *
     * @return 처리 결과
     */
    UUID selectUserIdByPhone(@Param("phone") String phone);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param userId 입력 값
     *
     * @return 처리 결과
     */
    List<String> selectRoleCodeListByUserId(@Param("userId") UUID userId);

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param command 입력 값
     *
     * @return 처리 결과
     */
    UUID insertUser(AuthSignupCommand command);

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param userId 입력 값
     *
     * @param roleCode 입력 값
     */
    void insertUserRole(@Param("userId") UUID userId, @Param("roleCode") String roleCode);

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param command 입력 값
     */
    void insertAuthLoginHistory(AuthLoginHistoryCommand command);

    /**
     * 업무 데이터를 수정합니다.
     *
     * @param userId 입력 값
     */
    void updateUserLastLoginAt(@Param("userId") UUID userId);

    /**
     * 업무 데이터를 수정합니다.
     *
     * @param command 입력 값
     */
    void updatePassword(AuthPasswordUpdateCommand command);
}
