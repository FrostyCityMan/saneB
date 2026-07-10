/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: LocalGovernmentNoticeDao.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.announcementsource.dao;

import com.saneb.domain.announcementsource.localgov.vo.AnnouncementSourceScheduleCommand;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceAuditLogCommand;
import com.saneb.domain.announcementsource.localgov.vo.AnnouncementSourceScheduleExecutionCommand;
import com.saneb.domain.announcementsource.localgov.vo.AnnouncementSourceScheduleRow;
import com.saneb.domain.announcementsource.localgov.vo.AnnouncementSourceScheduleStatusCommand;
import com.saneb.domain.announcementsource.localgov.vo.LocalGovernmentNoticeCollectionResultCommand;
import com.saneb.domain.announcementsource.localgov.vo.LocalGovernmentNoticeCollectionSummaryRow;
import com.saneb.domain.announcementsource.localgov.vo.LocalGovernmentNoticeParserProfileRow;
import com.saneb.domain.announcementsource.localgov.vo.LocalGovernmentNoticeSourceCollectionStatusCommand;
import com.saneb.domain.announcementsource.localgov.vo.LocalGovernmentNoticeSourceCommand;
import com.saneb.domain.announcementsource.localgov.vo.LocalGovernmentNoticeSourceEnabledCommand;
import com.saneb.domain.announcementsource.localgov.vo.LocalGovernmentNoticeSourceRow;
import com.saneb.domain.announcementsource.localgov.vo.LocalGovernmentNoticeSourceSearchCondition;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Param;

public interface LocalGovernmentNoticeDao {

    /**
     * 지자체 URL과 스케줄 변경을 감사 로그에 기록합니다.
     *
     * @param command 감사 로그 명령
     */
    void insertAuditLog(AnnouncementSourceAuditLogCommand command);

    /**
     * 지자체 공고 URL을 등록합니다.
     *
     * @param command 등록 명령
     */
    void insertSource(LocalGovernmentNoticeSourceCommand command);

    /**
     * 지자체 공고 URL을 수정합니다.
     *
     * @param command 수정 명령
     * @return 수정 건수
     */
    int updateSource(LocalGovernmentNoticeSourceCommand command);

    /**
     * 지자체 공고 URL 사용 여부를 수정합니다.
     *
     * @param command 사용 여부 변경 명령
     * @return 수정 건수
     */
    int updateSourceEnabled(LocalGovernmentNoticeSourceEnabledCommand command);

    /**
     * 지자체 공고 URL을 소프트 삭제합니다.
     *
     * @param sourceId URL 식별자
     * @param actorUserId 처리 사용자 식별자
     * @return 수정 건수
     */
    int deleteSource(@Param("sourceId") UUID sourceId, @Param("actorUserId") UUID actorUserId);

    /**
     * 운영 공고와 연결된 지자체 수집 원문 건수를 조회합니다.
     *
     * @return 연결된 원문 건수
     */
    long selectLinkedQaSnapshotCount();

    /**
     * 지자체 수집 스케줄 실행 이력을 삭제합니다.
     *
     * @return 삭제 건수
     */
    int deleteQaScheduleExecutionList();

    /**
     * 지자체 수집 실행 이력을 삭제합니다.
     *
     * @return 삭제 건수
     */
    int deleteQaCollectionRunList();

    /**
     * 지자체 수집 승인 요청 이력을 삭제합니다.
     *
     * @return 삭제 건수
     */
    int deleteQaCollectionRequestList();

    /**
     * 지자체 수집 원문을 삭제합니다.
     *
     * @return 삭제 건수
     */
    int deleteQaSnapshotList();

    /**
     * 지자체 URL의 마지막 수집 상태를 초기화합니다.
     *
     * @return 초기화 건수
     */
    int resetQaSourceCollectionState();

    /**
     * 지자체 공고 URL 목록을 조회합니다.
     *
     * @param condition 검색 조건
     * @return URL 목록
     */
    List<LocalGovernmentNoticeSourceRow> selectSourceList(LocalGovernmentNoticeSourceSearchCondition condition);

    /**
     * 지자체 공고 URL 건수를 조회합니다.
     *
     * @param condition 검색 조건
     * @return URL 건수
     */
    long selectSourceCount(LocalGovernmentNoticeSourceSearchCondition condition);

    /**
     * 지자체 공고 URL 상세를 조회합니다.
     *
     * @param sourceId URL 식별자
     * @return URL 상세
     */
    LocalGovernmentNoticeSourceRow selectSourceDetails(@Param("sourceId") UUID sourceId);

    /**
     * 수집 가능한 지자체 공고 URL 목록을 조회합니다.
     *
     * @param sourceId 특정 URL 식별자, 전체이면 null
     * @param maxCount 최대 URL 건수
     * @return 수집 가능한 URL 목록
     */
    List<LocalGovernmentNoticeSourceRow> selectEnabledSourceList(
            @Param("sourceId") UUID sourceId,
            @Param("maxCount") int maxCount
    );

    /**
     * 활성 파서 프로필 목록을 조회합니다.
     *
     * @return 파서 프로필 목록
     */
    List<LocalGovernmentNoticeParserProfileRow> selectParserProfileList();

    /**
     * 파서 프로필 상세를 조회합니다.
     *
     * @param profileCode 파서 프로필 코드
     * @return 파서 프로필 상세
     */
    LocalGovernmentNoticeParserProfileRow selectParserProfileDetails(@Param("profileCode") String profileCode);

    /**
     * 지자체 수집 현황을 집계합니다.
     *
     * @return 수집 현황
     */
    LocalGovernmentNoticeCollectionSummaryRow selectCollectionSummary();

    /**
     * URL 단위 수집 결과를 등록합니다.
     *
     * @param command 수집 결과 명령
     */
    void insertCollectionResult(LocalGovernmentNoticeCollectionResultCommand command);

    /**
     * URL 단위 신규·중복 결과 건수를 누적합니다.
     *
     * @param runId 실행 식별자
     * @param sourceId 지자체 URL 식별자
     * @param newIncrement 신규 증가 건수
     * @param duplicateIncrement 중복 증가 건수
     */
    void updateCollectionResultCounts(
            @Param("runId") UUID runId,
            @Param("sourceId") UUID sourceId,
            @Param("newIncrement") int newIncrement,
            @Param("duplicateIncrement") int duplicateIncrement
    );

    /**
     * 지자체 URL의 마지막 수집 상태를 수정합니다.
     *
     * @param command 수집 상태 명령
     */
    void updateSourceCollectionStatus(LocalGovernmentNoticeSourceCollectionStatusCommand command);

    /**
     * 정기 수집 스케줄을 등록합니다.
     *
     * @param command 등록 명령
     */
    void insertSchedule(AnnouncementSourceScheduleCommand command);

    /**
     * 정기 수집 스케줄 목록을 조회합니다.
     *
     * @return 스케줄 목록
     */
    List<AnnouncementSourceScheduleRow> selectScheduleList();

    /**
     * 정기 수집 스케줄 상세를 조회합니다.
     *
     * @param scheduleId 스케줄 식별자
     * @return 스케줄 상세
     */
    AnnouncementSourceScheduleRow selectScheduleDetails(@Param("scheduleId") UUID scheduleId);

    /**
     * 정기 수집 스케줄 상태를 수정합니다.
     *
     * @param command 상태 변경 명령
     * @return 수정 건수
     */
    int updateScheduleStatus(AnnouncementSourceScheduleStatusCommand command);

    /**
     * 실행 예정인 승인 스케줄을 조회합니다.
     *
     * @param dueAt 기준 시각
     * @return 실행 예정 스케줄 목록
     */
    List<AnnouncementSourceScheduleRow> selectDueScheduleList(@Param("dueAt") OffsetDateTime dueAt);

    /**
     * 동일 예정시각 중복을 차단하며 스케줄 실행 슬롯을 등록합니다.
     *
     * @param command 실행 슬롯 명령
     * @return 등록 건수
     */
    int insertScheduleExecution(AnnouncementSourceScheduleExecutionCommand command);

    /**
     * 스케줄 실행 결과를 수정합니다.
     *
     * @param command 실행 결과 명령
     */
    void updateScheduleExecution(AnnouncementSourceScheduleExecutionCommand command);

    /**
     * 스케줄의 마지막·다음 실행 시각을 수정합니다.
     *
     * @param scheduleId 스케줄 식별자
     * @param lastRunAt 마지막 실행 시각
     * @param nextRunAt 다음 실행 시각
     */
    void updateScheduleNextRun(
            @Param("scheduleId") UUID scheduleId,
            @Param("lastRunAt") OffsetDateTime lastRunAt,
            @Param("nextRunAt") OffsetDateTime nextRunAt
    );
}
