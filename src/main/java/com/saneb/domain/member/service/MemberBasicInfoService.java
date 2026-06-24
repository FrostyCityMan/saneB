/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: MemberBasicInfoService.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.member.service;

import com.saneb.domain.member.dto.MemberBasicInfoResponse;
import com.saneb.domain.member.dto.MemberBasicInfoSaveRequest;
import java.util.UUID;
import org.springframework.security.core.Authentication;

public interface MemberBasicInfoService {

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param authentication 입력 값
     *
     * @return 처리 결과
     */
    MemberBasicInfoResponse selectMyBasicInfo(Authentication authentication);

    /**
     * 업무 데이터를 저장합니다.
     *
     * @param authentication 입력 값
     *
     * @param request 입력 값
     *
     * @return 처리 결과
     */
    MemberBasicInfoResponse saveMyBasicInfo(Authentication authentication, MemberBasicInfoSaveRequest request);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param authentication 입력 값
     *
     * @param userId 입력 값
     *
     * @return 처리 결과
     */
    MemberBasicInfoResponse selectMemberBasicInfo(Authentication authentication, UUID userId);

    /**
     * 업무 데이터를 저장합니다.
     *
     * @param authentication 입력 값
     *
     * @param userId 입력 값
     *
     * @param request 입력 값
     *
     * @return 처리 결과
     */
    MemberBasicInfoResponse saveMemberBasicInfo(
            Authentication authentication,
            UUID userId,
            MemberBasicInfoSaveRequest request
    );
}
