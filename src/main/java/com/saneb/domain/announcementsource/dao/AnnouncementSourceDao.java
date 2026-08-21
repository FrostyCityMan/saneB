/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: AnnouncementSourceDao.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.announcementsource.dao;

import com.saneb.domain.announcementsource.vo.AnnouncementSourceAttachmentCommand;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceAuditLogCommand;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceAttachmentRow;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceCollectionApprovalCommand;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceCollectionRequestCommand;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceCollectionRequestRow;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceCollectionRequestSearchCondition;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceCollectionRunCommand;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceCollectionRunItemCommand;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceCollectionRunItemRow;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceCollectionRunRow;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceCollectionRunSearchCondition;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceDuplicateCandidateCommand;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceDuplicateCandidateRow;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceDuplicateDecisionCommand;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceExclusionRuleMatchCommand;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceExclusionTombstoneCommand;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceHighlightCommand;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceHighlightRow;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceLinkCommand;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceLinkedAnnouncementRow;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceReviewHistoryCommand;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceReviewStatusCommand;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceSearchCondition;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceSnapshotCommand;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceSnapshotDuplicateCommand;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceSnapshotDuplicateDecisionCommand;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceSnapshotDuplicateRow;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceSnapshotRefreshCommand;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceSnapshotRow;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AnnouncementSourceDao {

    /**
     * 공고 수집 운영 변경을 감사 로그에 기록합니다.
     *
     * @param command 감사 로그 명령
     */
    void insertAuditLog(AnnouncementSourceAuditLogCommand command);

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param command 입력 값
     */
    void insertCollectionRequest(AnnouncementSourceCollectionRequestCommand command);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param condition 입력 값
     *
     * @return 처리 결과
     */
    List<AnnouncementSourceCollectionRequestRow> selectCollectionRequestList(
            AnnouncementSourceCollectionRequestSearchCondition condition
    );

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param condition 입력 값
     *
     * @return 처리 결과
     */
    long selectCollectionRequestCount(AnnouncementSourceCollectionRequestSearchCondition condition);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param requestId 입력 값
     *
     * @return 처리 결과
     */
    AnnouncementSourceCollectionRequestRow selectCollectionRequestDetails(@Param("requestId") UUID requestId);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param providerCode 입력 값
     *
     * @param searchRegionCode 입력 값
     *
     * @param searchCategoryCode 입력 값
     *
     * @return 처리 결과
     */
    long selectOpenBatchRequestCount(
            @Param("providerCode") String providerCode,
            @Param("searchRegionCode") String searchRegionCode,
            @Param("searchCategoryCode") String searchCategoryCode
    );

    /**
     * 업무 데이터를 수정합니다.
     *
     * @param command 입력 값
     *
     * @return 처리 결과
     */
    int updateCollectionRequestApproval(AnnouncementSourceCollectionApprovalCommand command);

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param command 입력 값
     */
    void insertCollectionRun(AnnouncementSourceCollectionRunCommand command);

    /**
     * 업무 데이터를 수정합니다.
     *
     * @param command 입력 값
     *
     * @return 처리 결과
     */
    int updateCollectionRunResult(AnnouncementSourceCollectionRunCommand command);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param condition 입력 값
     *
     * @return 처리 결과
     */
    List<AnnouncementSourceCollectionRunRow> selectCollectionRunList(
            AnnouncementSourceCollectionRunSearchCondition condition
    );

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param condition 입력 값
     *
     * @return 처리 결과
     */
    long selectCollectionRunCount(AnnouncementSourceCollectionRunSearchCondition condition);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param runId 입력 값
     *
     * @return 처리 결과
     */
    AnnouncementSourceCollectionRunRow selectCollectionRunDetails(@Param("runId") UUID runId);

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param command 입력 값
     */
    void insertCollectionRunItem(AnnouncementSourceCollectionRunItemCommand command);

    /**
     * 제목 제외 공고의 비가역 식별자와 판정 근거를 저장합니다.
     *
     * @param command 제외 tombstone 저장 명령
     */
    void insertExclusionTombstone(AnnouncementSourceExclusionTombstoneCommand command);

    /**
     * 제목 제외 공고의 tombstone 식별자를 조회합니다.
     *
     * @param providerCode 제공자 코드
     * @param identityHash 비가역 공고 식별자
     * @return tombstone 식별자
     */
    UUID selectExclusionTombstoneId(
            @Param("providerCode") String providerCode,
            @Param("identityHash") String identityHash
    );

    /**
     * 제목 제외 판정의 규칙 참조를 원문 문자열 없이 저장합니다.
     *
     * @param command 제외 규칙 근거 저장 명령
     */
    void insertExclusionRuleMatch(AnnouncementSourceExclusionRuleMatchCommand command);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param runId 입력 값
     *
     * @return 처리 결과
     */
    List<AnnouncementSourceCollectionRunItemRow> selectCollectionRunItemList(@Param("runId") UUID runId);

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param command 입력 값
     */
    void insertSourceSnapshot(AnnouncementSourceSnapshotCommand command);

    /**
     * 동일 provider 원문의 변경된 current snapshot을 낙관적으로 갱신합니다.
     *
     * @param command 변경 원문과 기존 raw hash·분류 버전
     * @return 수정 건수
     */
    int updateSourceSnapshotContent(AnnouncementSourceSnapshotRefreshCommand command);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param providerCode 입력 값
     *
     * @param providerNoticeId 입력 값
     *
     * @return 처리 결과
     */
    AnnouncementSourceSnapshotRow selectSourceByProviderNoticeId(
            @Param("providerCode") String providerCode,
            @Param("providerNoticeId") String providerNoticeId
    );

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param providerCode 입력 값
     *
     * @param sourceUrl 입력 값
     *
     * @return 처리 결과
     */
    AnnouncementSourceSnapshotRow selectSourceByUrl(
            @Param("providerCode") String providerCode,
            @Param("sourceUrl") String sourceUrl
    );

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param providerCode 입력 값
     *
     * @param rawHash 입력 값
     *
     * @return 처리 결과
     */
    AnnouncementSourceSnapshotRow selectSourceByRawHash(
            @Param("providerCode") String providerCode,
            @Param("rawHash") String rawHash
    );

    /**
     * 다른 제공자의 정확한 중복 원문을 조회합니다.
     *
     * @param providerCode 현재 제공자 코드
     * @param canonicalSourceUrl canonical 원문 URL
     * @param normalizedTitle 정규화 제목
     * @param normalizedAgencyName 정규화 기관명
     * @param postedDate 등록일
     * @return 정확한 중복 원문
     */
    AnnouncementSourceSnapshotRow selectExactSourceAcrossProviders(
            @Param("providerCode") String providerCode,
            @Param("canonicalSourceUrl") String canonicalSourceUrl,
            @Param("normalizedTitle") String normalizedTitle,
            @Param("normalizedAgencyName") String normalizedAgencyName,
            @Param("postedDate") java.time.LocalDate postedDate
    );

    /**
     * 새 원문과 유사한 다른 제공자 원문을 조회합니다.
     *
     * @param sourceId 새 원문 식별자
     * @return 유사 원문 목록
     */
    List<AnnouncementSourceSnapshotRow> selectSimilarSourceAcrossProvidersList(@Param("sourceId") UUID sourceId);

    /**
     * 교차 제공자 중복 또는 유사 관계를 등록합니다.
     *
     * @param command 중복 관계 명령
     */
    void insertSnapshotDuplicate(AnnouncementSourceSnapshotDuplicateCommand command);

    /**
     * 원문의 교차 제공자 중복 관계를 조회합니다.
     *
     * @param sourceId 원문 식별자
     * @return 중복 관계 목록
     */
    List<AnnouncementSourceSnapshotDuplicateRow> selectSnapshotDuplicateList(@Param("sourceId") UUID sourceId);

    /**
     * 운영자 판단 대기 중인 교차 제공자 유사 후보 건수를 조회합니다.
     *
     * @param sourceId 원문 식별자
     * @return 대기 건수
     */
    long selectPendingSnapshotDuplicateCount(@Param("sourceId") UUID sourceId);

    /**
     * 교차 제공자 유사 후보의 운영자 결정을 저장합니다.
     *
     * @param command 결정 명령
     * @return 수정 건수
     */
    int updateSnapshotDuplicateDecision(AnnouncementSourceSnapshotDuplicateDecisionCommand command);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param condition 입력 값
     *
     * @return 처리 결과
     */
    List<AnnouncementSourceSnapshotRow> selectSourceList(AnnouncementSourceSearchCondition condition);

    List<com.saneb.domain.announcementsource.vo.AnnouncementSourceClassificationTagSummaryRow>
            selectSourceClassificationTagSummaryList(@org.apache.ibatis.annotations.Param("sourceIds") List<UUID> sourceIds);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param condition 입력 값
     *
     * @return 처리 결과
     */
    long selectSourceCount(AnnouncementSourceSearchCondition condition);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param sourceId 입력 값
     *
     * @return 처리 결과
     */
    AnnouncementSourceSnapshotRow selectSourceDetails(@Param("sourceId") UUID sourceId);

    /**
     * 운영 공고 전환을 직렬화하기 위해 원문 row를 잠가 조회합니다.
     *
     * @param sourceId 원문 식별자
     * @return 잠긴 원문 row
     */
    AnnouncementSourceSnapshotRow selectSourceDetailsForUpdate(@Param("sourceId") UUID sourceId);

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param command 입력 값
     */
    void insertSourceAttachment(AnnouncementSourceAttachmentCommand command);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param sourceId 입력 값
     *
     * @return 처리 결과
     */
    List<AnnouncementSourceAttachmentRow> selectSourceAttachmentList(@Param("sourceId") UUID sourceId);

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param command 입력 값
     */
    void insertSourceHighlight(AnnouncementSourceHighlightCommand command);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param sourceId 입력 값
     *
     * @return 처리 결과
     */
    List<AnnouncementSourceHighlightRow> selectSourceHighlightList(@Param("sourceId") UUID sourceId);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param sourceId 입력 값
     *
     * @return 처리 결과
     */
    List<AnnouncementSourceDuplicateCandidateRow> selectActiveAnnouncementDuplicateCandidateList(
            @Param("sourceId") UUID sourceId
    );

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param command 입력 값
     */
    void insertDuplicateCandidate(AnnouncementSourceDuplicateCandidateCommand command);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param sourceId 입력 값
     *
     * @return 처리 결과
     */
    List<AnnouncementSourceDuplicateCandidateRow> selectDuplicateCandidateList(@Param("sourceId") UUID sourceId);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param sourceId 입력 값
     *
     * @return 처리 결과
     */
    long selectPendingDuplicateCandidateCount(@Param("sourceId") UUID sourceId);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param sourceId 입력 값
     *
     * @return 처리 결과
     */
    long selectPendingExactDuplicateCandidateCount(@Param("sourceId") UUID sourceId);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param sourceId 입력 값
     *
     * @param candidateId 입력 값
     *
     * @return 처리 결과
     */
    AnnouncementSourceDuplicateCandidateRow selectDuplicateCandidateDetails(
            @Param("sourceId") UUID sourceId,
            @Param("candidateId") UUID candidateId
    );

    /**
     * 업무 데이터를 수정합니다.
     *
     * @param command 입력 값
     *
     * @return 처리 결과
     */
    int updateDuplicateCandidateDecision(AnnouncementSourceDuplicateDecisionCommand command);

    /**
     * 업무 데이터를 수정합니다.
     *
     * @param command 입력 값
     *
     * @return 처리 결과
     */
    int updateSourceReviewStatus(AnnouncementSourceReviewStatusCommand command);

    /**
     * 원문에 연결된 승인·정상 노출 운영 공고 건수를 조회합니다.
     *
     * @param sourceId 원문 식별자
     * @return 승인·정상 노출 공고 건수
     */
    long selectApprovedLinkedAnnouncementCount(@Param("sourceId") UUID sourceId);

    /**
     * 원문에 이미 연결된 운영 공고 식별자와 공개 코드를 조회합니다.
     *
     * @param sourceId 원문 식별자
     * @return 기존 연결 운영 공고 또는 null
     */
    AnnouncementSourceLinkedAnnouncementRow selectLinkedAnnouncementDetails(@Param("sourceId") UUID sourceId);

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param command 입력 값
     */
    void insertSourceReviewHistory(AnnouncementSourceReviewHistoryCommand command);

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param command 입력 값
     */
    void insertSourceLink(AnnouncementSourceLinkCommand command);
}
