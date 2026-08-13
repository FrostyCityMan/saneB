package com.saneb.domain.announcementsource.dao;

import com.saneb.domain.announcementsource.vo.AnnouncementSourceAuditLogCommand;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceKeywordRuleInsertCommand;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceKeywordRuleRow;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceKeywordRuleSearchCondition;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceKeywordRuleUpdateCommand;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceKeywordTermInsertCommand;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceRuleGroupRow;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceRuleReleaseInsertCommand;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceRuleReleaseRow;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceRuleReleaseSearchCondition;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceRuleTermRow;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 규칙 release와 DRAFT 키워드 편집 SQL만 호출합니다. */
@Mapper
public interface AnnouncementSourceRuleReleaseDao {

    List<AnnouncementSourceRuleReleaseRow> selectRuleReleaseList(
            AnnouncementSourceRuleReleaseSearchCondition condition
    );

    long selectRuleReleaseCount(AnnouncementSourceRuleReleaseSearchCondition condition);

    AnnouncementSourceRuleReleaseRow selectRuleReleaseDetails(@Param("releaseId") UUID releaseId);

    AnnouncementSourceRuleReleaseRow selectRuleReleaseDetailsForUpdate(@Param("releaseId") UUID releaseId);

    AnnouncementSourceRuleReleaseRow selectActiveRuleReleaseDetailsForUpdate();

    int selectNextRuleReleaseVersionNo();

    void insertClonedRuleRelease(AnnouncementSourceRuleReleaseInsertCommand command);

    void insertClonedRuleGroupList(AnnouncementSourceRuleReleaseInsertCommand command);

    void insertClonedKeywordRuleList(AnnouncementSourceRuleReleaseInsertCommand command);

    void insertClonedKeywordTermList(AnnouncementSourceRuleReleaseInsertCommand command);

    List<AnnouncementSourceKeywordRuleRow> selectKeywordRuleList(
            AnnouncementSourceKeywordRuleSearchCondition condition
    );

    long selectKeywordRuleCount(AnnouncementSourceKeywordRuleSearchCondition condition);

    AnnouncementSourceKeywordRuleRow selectKeywordRuleDetails(
            @Param("releaseId") UUID releaseId,
            @Param("ruleId") UUID ruleId
    );

    AnnouncementSourceKeywordRuleRow selectKeywordRuleDetailsForUpdate(
            @Param("releaseId") UUID releaseId,
            @Param("ruleId") UUID ruleId
    );

    AnnouncementSourceRuleGroupRow selectRuleGroupDetails(
            @Param("releaseId") UUID releaseId,
            @Param("groupCode") String groupCode
    );

    List<AnnouncementSourceRuleTermRow> selectRuleTermList(@Param("releaseId") UUID releaseId);

    long selectDuplicateTermCount(
            @Param("groupId") UUID groupId,
            @Param("matchModeCode") String matchModeCode,
            @Param("normalizedTermList") List<String> normalizedTermList,
            @Param("excludedRuleId") UUID excludedRuleId
    );

    void insertKeywordRule(AnnouncementSourceKeywordRuleInsertCommand command);

    void insertKeywordTerm(AnnouncementSourceKeywordTermInsertCommand command);

    int updateKeywordRule(AnnouncementSourceKeywordRuleUpdateCommand command);

    void deleteKeywordTermList(@Param("ruleId") UUID ruleId);

    int updateKeywordRuleStatus(
            @Param("ruleId") UUID ruleId,
            @Param("enabled") boolean enabled,
            @Param("expectedVersion") int expectedVersion,
            @Param("actorUserId") UUID actorUserId
    );

    int deleteKeywordRule(
            @Param("ruleId") UUID ruleId,
            @Param("expectedVersion") int expectedVersion
    );

    int updateRuleReleaseRowVersion(
            @Param("releaseId") UUID releaseId,
            @Param("changeReason") String changeReason
    );

    int updateRuleReleaseRowVersionExpected(
            @Param("releaseId") UUID releaseId,
            @Param("expectedVersion") int expectedVersion,
            @Param("changeReason") String changeReason
    );

    int updateActiveRuleReleaseRetired(
            @Param("excludedReleaseId") UUID excludedReleaseId,
            @Param("changeReason") String changeReason
    );

    int updateRuleReleaseActive(
            @Param("releaseId") UUID releaseId,
            @Param("expectedVersion") int expectedVersion,
            @Param("ruleSnapshotHash") String ruleSnapshotHash,
            @Param("changeReason") String changeReason,
            @Param("actorUserId") UUID actorUserId
    );

    void insertAuditLog(AnnouncementSourceAuditLogCommand command);
}
