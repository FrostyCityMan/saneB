/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: MatchingDao.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.matching.dao;

import com.saneb.domain.matching.vo.AnnouncementMatchingRow;
import com.saneb.domain.matching.vo.AuditLogCommand;
import com.saneb.domain.matching.vo.MatchingCaseCreateCommand;
import com.saneb.domain.matching.vo.MatchingCandidateAnnouncementRow;
import com.saneb.domain.matching.vo.MatchingCaseRow;
import com.saneb.domain.matching.vo.MatchingCaseSearchCondition;
import com.saneb.domain.matching.vo.MatchingCaseStageStatusCommand;
import com.saneb.domain.matching.vo.MatchingCaseStatusCommand;
import com.saneb.domain.matching.vo.MatchingMemberLookupRow;
import com.saneb.domain.matching.vo.MatchingMemberLookupSearchCondition;
import com.saneb.domain.matching.vo.MatchingResultDetailCommand;
import com.saneb.domain.matching.vo.MatchingResultDetailRow;
import com.saneb.domain.matching.vo.VerificationMatchingRow;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MatchingDao {

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param condition 입력 값
     *
     * @return 처리 결과
     */
    List<MatchingCaseRow> selectMatchingCaseList(MatchingCaseSearchCondition condition);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param condition 입력 값
     *
     * @return 처리 결과
     */
    long selectMatchingCaseCount(MatchingCaseSearchCondition condition);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param memberUserId 입력 값
     *
     * @return 처리 결과
     */
    long selectMatchingMemberUserCount(@Param("memberUserId") UUID memberUserId);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param memberUserId 입력 값
     *
     * @param finalMatching 입력 값
     *
     * @return 처리 결과
     */
    List<MatchingCandidateAnnouncementRow> selectEligibleAnnouncementCandidateList(
            @Param("memberUserId") UUID memberUserId,
            @Param("finalMatching") boolean finalMatching
    );

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param condition 입력 값
     *
     * @return 처리 결과
     */
    List<MatchingMemberLookupRow> selectMatchingMemberLookupList(MatchingMemberLookupSearchCondition condition);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param condition 입력 값
     *
     * @return 처리 결과
     */
    long selectMatchingMemberLookupCount(MatchingMemberLookupSearchCondition condition);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param matchingCaseId 입력 값
     *
     * @return 처리 결과
     */
    MatchingCaseRow selectMatchingCaseDetails(@Param("matchingCaseId") UUID matchingCaseId);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param announcementId 입력 값
     *
     * @param memberUserId 입력 값
     *
     * @param verificationId 입력 값
     *
     * @return 처리 결과
     */
    MatchingCaseRow selectMatchingCaseDetailsByBusinessKey(
            @Param("announcementId") UUID announcementId,
            @Param("memberUserId") UUID memberUserId,
            @Param("verificationId") UUID verificationId
    );

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param announcementId 입력 값
     *
     * @param memberUserId 입력 값
     *
     * @param verificationId 입력 값
     *
     * @param matchingStageCode 입력 값
     *
     * @return 처리 결과
     */
    MatchingCaseRow selectMatchingCaseDetailsByStageBusinessKey(
            @Param("announcementId") UUID announcementId,
            @Param("memberUserId") UUID memberUserId,
            @Param("verificationId") UUID verificationId,
            @Param("matchingStageCode") String matchingStageCode
    );

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param matchingCaseId 입력 값
     *
     * @return 처리 결과
     */
    List<MatchingResultDetailRow> selectMatchingResultDetailList(@Param("matchingCaseId") UUID matchingCaseId);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param announcementId 입력 값
     *
     * @return 처리 결과
     */
    AnnouncementMatchingRow selectAnnouncementForMatching(@Param("announcementId") UUID announcementId);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param verificationId 입력 값
     *
     * @return 처리 결과
     */
    VerificationMatchingRow selectVerificationForMatching(@Param("verificationId") UUID verificationId);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param verificationId 입력 값
     *
     * @return 처리 결과
     */
    List<String> selectCheckedRestrictionFlagCodeList(@Param("verificationId") UUID verificationId);

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param command 입력 값
     */
    void insertMatchingCase(MatchingCaseCreateCommand command);

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param command 입력 값
     */
    void insertMatchingResultDetail(MatchingResultDetailCommand command);

    /**
     * 업무 데이터를 수정합니다.
     *
     * @param command 입력 값
     *
     * @return 처리 결과
     */
    int updateMatchingCaseStatus(MatchingCaseStatusCommand command);

    /**
     * 업무 데이터를 수정합니다.
     *
     * @param announcementId 입력 값
     *
     * @param memberUserId 입력 값
     *
     * @param verificationId 입력 값
     *
     * @param matchingStageCode 입력 값
     *
     * @param statusCode 입력 값
     *
     * @param matchingBasisCode 입력 값
     *
     * @param actorUserId 입력 값
     *
     * @return 처리 결과
     */
    int updateMatchingCaseStageStatus(
            @Param("announcementId") UUID announcementId,
            @Param("memberUserId") UUID memberUserId,
            @Param("verificationId") UUID verificationId,
            @Param("matchingStageCode") String matchingStageCode,
            @Param("statusCode") String statusCode,
            @Param("matchingBasisCode") String matchingBasisCode,
            @Param("actorUserId") UUID actorUserId
    );

    /**
     * 업무 데이터를 수정합니다.
     *
     * @param command 입력 값
     *
     * @return 처리 결과
     */
    int updateMatchingCaseStageNotEligible(MatchingCaseStageStatusCommand command);

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param command 입력 값
     */
    void insertAuditLog(AuditLogCommand command);
}
