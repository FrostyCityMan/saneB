/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: MemberBasicInfoDao.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.member.dao;

import com.saneb.domain.member.vo.BusinessProfileCommand;
import com.saneb.domain.member.vo.BusinessProfileRow;
import com.saneb.domain.member.vo.FamilyMemberCommand;
import com.saneb.domain.member.vo.FamilyMemberRow;
import com.saneb.domain.member.vo.MemberDocumentFieldRow;
import com.saneb.domain.member.vo.MemberDocumentInputValueCommand;
import com.saneb.domain.member.vo.MemberDocumentInputValueRow;
import com.saneb.domain.member.vo.MemberInterviewResponseCommand;
import com.saneb.domain.member.vo.MemberInterviewResponseRow;
import com.saneb.domain.member.vo.MemberProfileCommand;
import com.saneb.domain.member.vo.MemberProfileRow;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MemberBasicInfoDao {

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
    int selectUserCountByUserId(@Param("userId") UUID userId);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param userId 입력 값
     *
     * @return 처리 결과
     */
    MemberProfileRow selectMemberProfileDetails(@Param("userId") UUID userId);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param userId 입력 값
     *
     * @return 처리 결과
     */
    BusinessProfileRow selectBusinessProfileDetails(@Param("userId") UUID userId);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param userId 입력 값
     *
     * @return 처리 결과
     */
    List<FamilyMemberRow> selectFamilyMemberList(@Param("userId") UUID userId);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @return 처리 결과
     */
    List<MemberDocumentFieldRow> selectMemberDocumentFieldList();

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param userId 입력 값
     *
     * @return 처리 결과
     */
    List<MemberDocumentInputValueRow> selectMemberDocumentInputValueList(@Param("userId") UUID userId);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param userId 입력 값
     *
     * @return 처리 결과
     */
    List<MemberInterviewResponseRow> selectMemberInterviewResponseList(@Param("userId") UUID userId);

    /**
     * 업무 데이터를 저장합니다.
     *
     * @param command 입력 값
     */
    void saveMemberProfile(MemberProfileCommand command);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param userId 입력 값
     *
     * @return 처리 결과
     */
    UUID selectBusinessProfileIdByUserId(@Param("userId") UUID userId);

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param command 입력 값
     */
    void insertBusinessProfile(BusinessProfileCommand command);

    /**
     * 업무 데이터를 수정합니다.
     *
     * @param command 입력 값
     *
     * @return 처리 결과
     */
    int updateBusinessProfile(BusinessProfileCommand command);

    /**
     * 업무 데이터를 삭제합니다.
     *
     * @param userId 입력 값
     */
    void deleteFamilyMemberList(@Param("userId") UUID userId);

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param command 입력 값
     */
    void insertFamilyMember(FamilyMemberCommand command);

    /**
     * 업무 데이터를 삭제합니다.
     *
     * @param userId 입력 값
     */
    void deleteMemberInterviewResponseList(@Param("userId") UUID userId);

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param command 입력 값
     */
    void insertMemberInterviewResponse(MemberInterviewResponseCommand command);

    /**
     * 업무 데이터를 삭제합니다.
     *
     * @param userId 입력 값
     */
    void deleteMemberDocumentInputValueList(@Param("userId") UUID userId);

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param command 입력 값
     */
    void insertMemberDocumentInputValue(MemberDocumentInputValueCommand command);
}
