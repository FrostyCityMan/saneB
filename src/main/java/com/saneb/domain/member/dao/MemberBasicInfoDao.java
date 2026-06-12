package com.saneb.domain.member.dao;

import com.saneb.domain.member.vo.BusinessProfileCommand;
import com.saneb.domain.member.vo.BusinessProfileRow;
import com.saneb.domain.member.vo.FamilyMemberCommand;
import com.saneb.domain.member.vo.FamilyMemberRow;
import com.saneb.domain.member.vo.MemberDocumentFieldRow;
import com.saneb.domain.member.vo.MemberDocumentInputValueCommand;
import com.saneb.domain.member.vo.MemberDocumentInputValueRow;
import com.saneb.domain.member.vo.MemberProfileCommand;
import com.saneb.domain.member.vo.MemberProfileRow;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MemberBasicInfoDao {

    UUID selectUserIdByLoginId(@Param("loginId") String loginId);

    MemberProfileRow selectMemberProfileDetails(@Param("userId") UUID userId);

    BusinessProfileRow selectBusinessProfileDetails(@Param("userId") UUID userId);

    List<FamilyMemberRow> selectFamilyMemberList(@Param("userId") UUID userId);

    List<MemberDocumentFieldRow> selectMemberDocumentFieldList();

    List<MemberDocumentInputValueRow> selectMemberDocumentInputValueList(@Param("userId") UUID userId);

    void saveMemberProfile(MemberProfileCommand command);

    UUID selectBusinessProfileIdByUserId(@Param("userId") UUID userId);

    void insertBusinessProfile(BusinessProfileCommand command);

    int updateBusinessProfile(BusinessProfileCommand command);

    void deleteFamilyMemberList(@Param("userId") UUID userId);

    void insertFamilyMember(FamilyMemberCommand command);

    void deleteMemberDocumentInputValueList(@Param("userId") UUID userId);

    void insertMemberDocumentInputValue(MemberDocumentInputValueCommand command);
}
