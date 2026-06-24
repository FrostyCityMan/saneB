/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: AdminBootstrapDao.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.auth.dao;

import com.saneb.domain.auth.vo.AdminBootstrapCommand;
import java.util.UUID;
import org.apache.ibatis.annotations.Param;

public interface AdminBootstrapDao {

    /**
     * 업무 데이터를 조회합니다.
     *
     * @return 처리 결과
     */
    int selectActiveAdminCount();

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param loginId 입력 값
     *
     * @return 처리 결과
     */
    UUID selectUserIdByLoginId(@Param("loginId") String loginId);

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param command 입력 값
     */
    void insertAdminUser(AdminBootstrapCommand command);

    /**
     * 업무 데이터를 수정합니다.
     *
     * @param command 입력 값
     *
     * @return 처리 결과
     */
    int updateAdminUser(AdminBootstrapCommand command);

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param userId 입력 값
     */
    void insertAdminRole(@Param("userId") UUID userId);
}
