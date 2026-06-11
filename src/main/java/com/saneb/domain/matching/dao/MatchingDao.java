package com.saneb.domain.matching.dao;

import com.saneb.domain.matching.vo.AnnouncementMatchingRow;
import com.saneb.domain.matching.vo.AuditLogCommand;
import com.saneb.domain.matching.vo.MatchingCaseCreateCommand;
import com.saneb.domain.matching.vo.MatchingCandidateAnnouncementRow;
import com.saneb.domain.matching.vo.MatchingCaseRow;
import com.saneb.domain.matching.vo.MatchingCaseSearchCondition;
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

    List<MatchingCaseRow> selectMatchingCaseList(MatchingCaseSearchCondition condition);

    long selectMatchingCaseCount(MatchingCaseSearchCondition condition);

    long selectMatchingMemberUserCount(@Param("memberUserId") UUID memberUserId);

    List<MatchingCandidateAnnouncementRow> selectEligibleAnnouncementCandidateList(
            @Param("memberUserId") UUID memberUserId
    );

    List<MatchingMemberLookupRow> selectMatchingMemberLookupList(MatchingMemberLookupSearchCondition condition);

    long selectMatchingMemberLookupCount(MatchingMemberLookupSearchCondition condition);

    MatchingCaseRow selectMatchingCaseDetails(@Param("matchingCaseId") UUID matchingCaseId);

    MatchingCaseRow selectMatchingCaseDetailsByBusinessKey(
            @Param("announcementId") UUID announcementId,
            @Param("memberUserId") UUID memberUserId,
            @Param("verificationId") UUID verificationId
    );

    List<MatchingResultDetailRow> selectMatchingResultDetailList(@Param("matchingCaseId") UUID matchingCaseId);

    AnnouncementMatchingRow selectAnnouncementForMatching(@Param("announcementId") UUID announcementId);

    VerificationMatchingRow selectVerificationForMatching(@Param("verificationId") UUID verificationId);

    List<String> selectCheckedRestrictionFlagCodeList(@Param("verificationId") UUID verificationId);

    void insertMatchingCase(MatchingCaseCreateCommand command);

    void insertMatchingResultDetail(MatchingResultDetailCommand command);

    int updateMatchingCaseStatus(MatchingCaseStatusCommand command);

    void insertAuditLog(AuditLogCommand command);
}
