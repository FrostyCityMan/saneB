/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 프로젝트명: saneB
 * 파일명: AnnouncementSourceClassificationDao.java
 * 작성자: 김도훈
 */

package com.saneb.domain.announcementsource.dao;

import com.saneb.domain.announcementsource.vo.AnnouncementSourceClassificationStateRow;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceClassificationDetailsRow;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceClassificationMatchRow;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceClassificationRuleTermRow;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceConfirmedSupportCommand;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceConfirmedTargetCommand;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceContentVersionCommand;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceContentVersionRow;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceClassificationEvaluationCommand;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceClassificationMatchCommand;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceClassificationSupportMatchCommand;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceClassificationTargetMatchCommand;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AnnouncementSourceClassificationDao {

    List<AnnouncementSourceClassificationRuleTermRow> selectActiveClassificationRuleTermList();

    int updateCollectionRunRuleRelease(
            @Param("runId") UUID runId,
            @Param("ruleReleaseId") UUID ruleReleaseId,
            @Param("searchPlanHash") String searchPlanHash,
            @Param("searchPlanJson") String searchPlanJson
    );

    void insertContentVersion(AnnouncementSourceContentVersionCommand command);

    UUID selectContentVersionId(
            @Param("sourceId") UUID sourceId,
            @Param("rawHash") String rawHash
    );

    String selectLatestContentVersionHash(@Param("sourceId") UUID sourceId);

    AnnouncementSourceContentVersionRow selectLatestContentVersionDetails(
            @Param("sourceId") UUID sourceId
    );

    int updateCurrentEvaluationNotCurrent(@Param("sourceId") UUID sourceId);

    int updateConfirmedTargetClassificationStale(@Param("sourceId") UUID sourceId);

    int updateConfirmedSupportClassificationStale(@Param("sourceId") UUID sourceId);

    void insertClassificationEvaluation(AnnouncementSourceClassificationEvaluationCommand command);

    int insertClassificationMatch(AnnouncementSourceClassificationMatchCommand command);

    int insertClassificationTargetMatch(AnnouncementSourceClassificationTargetMatchCommand command);

    int insertClassificationSupportMatch(AnnouncementSourceClassificationSupportMatchCommand command);

    int updateSnapshotClassificationProjection(
            @Param("sourceId") UUID sourceId,
            @Param("semanticStatusCode") String semanticStatusCode,
            @Param("semanticReasonCode") String semanticReasonCode,
            @Param("semanticMatchedKeywords") String semanticMatchedKeywords,
            @Param("reviewStatusCode") String reviewStatusCode,
            @Param("expectedVersion") Integer expectedVersion
    );

    AnnouncementSourceClassificationStateRow selectClassificationStateDetails(
            @Param("sourceId") UUID sourceId
    );

    AnnouncementSourceClassificationDetailsRow selectClassificationDetails(
            @Param("sourceId") UUID sourceId
    );

    List<AnnouncementSourceClassificationMatchRow> selectClassificationMatchList(
            @Param("sourceId") UUID sourceId
    );

    List<String> selectAutomaticTargetCategoryCodeList(@Param("sourceId") UUID sourceId);

    List<String> selectAutomaticSupportTypeCodeList(@Param("sourceId") UUID sourceId);

    List<String> selectConfirmedTargetCategoryCodeList(@Param("sourceId") UUID sourceId);

    List<String> selectConfirmedSupportTypeCodeList(@Param("sourceId") UUID sourceId);

    int updateClassificationRowVersion(
            @Param("sourceId") UUID sourceId,
            @Param("expectedVersion") int expectedVersion
    );

    void deleteConfirmedTargetCategoryList(@Param("sourceId") UUID sourceId);

    void insertConfirmedTargetCategory(AnnouncementSourceConfirmedTargetCommand command);

    void deleteConfirmedSupportTypeList(@Param("sourceId") UUID sourceId);

    void insertConfirmedSupportType(AnnouncementSourceConfirmedSupportCommand command);
}
