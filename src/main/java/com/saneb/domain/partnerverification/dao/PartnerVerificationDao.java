package com.saneb.domain.partnerverification.dao;

import com.saneb.domain.partnerverification.vo.AuditLogCommand;
import com.saneb.domain.partnerverification.vo.PartnerVerificationCreateCommand;
import com.saneb.domain.partnerverification.vo.PartnerVerificationRow;
import com.saneb.domain.partnerverification.vo.PartnerVerificationSearchCondition;
import com.saneb.domain.partnerverification.vo.PartnerVerificationStatusCommand;
import com.saneb.domain.partnerverification.vo.VerificationBusinessValuesCommand;
import com.saneb.domain.partnerverification.vo.VerificationBusinessValuesRow;
import com.saneb.domain.partnerverification.vo.VerificationDocumentCommand;
import com.saneb.domain.partnerverification.vo.VerificationDocumentRow;
import com.saneb.domain.partnerverification.vo.VerificationFamilyValueCommand;
import com.saneb.domain.partnerverification.vo.VerificationFamilyValueRow;
import com.saneb.domain.partnerverification.vo.VerificationMemberValuesCommand;
import com.saneb.domain.partnerverification.vo.VerificationMemberValuesRow;
import com.saneb.domain.partnerverification.vo.VerificationRestrictionFlagCommand;
import com.saneb.domain.partnerverification.vo.VerificationRestrictionFlagRow;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PartnerVerificationDao {

    List<PartnerVerificationRow> selectPartnerVerificationList(PartnerVerificationSearchCondition condition);

    long selectPartnerVerificationCount(PartnerVerificationSearchCondition condition);

    PartnerVerificationRow selectPartnerVerificationDetails(@Param("verificationId") UUID verificationId);

    VerificationMemberValuesRow selectVerificationMemberValues(@Param("verificationId") UUID verificationId);

    VerificationBusinessValuesRow selectVerificationBusinessValues(@Param("verificationId") UUID verificationId);

    List<VerificationFamilyValueRow> selectVerificationFamilyValueList(@Param("verificationId") UUID verificationId);

    List<VerificationRestrictionFlagRow> selectVerificationRestrictionFlagList(@Param("verificationId") UUID verificationId);

    List<VerificationDocumentRow> selectVerificationDocumentList(@Param("verificationId") UUID verificationId);

    long selectUserCountById(@Param("userId") UUID userId);

    long selectBusinessProfileCountById(@Param("businessProfileId") UUID businessProfileId);

    void updateCurrentVerificationInactiveByMemberUserId(
            @Param("memberUserId") UUID memberUserId,
            @Param("actorUserId") UUID actorUserId
    );

    void insertPartnerVerification(PartnerVerificationCreateCommand command);

    void deleteVerificationMemberValues(@Param("verificationId") UUID verificationId);

    void insertVerificationMemberValues(VerificationMemberValuesCommand command);

    void deleteVerificationBusinessValues(@Param("verificationId") UUID verificationId);

    void insertVerificationBusinessValues(VerificationBusinessValuesCommand command);

    void deleteVerificationFamilyValues(@Param("verificationId") UUID verificationId);

    void insertVerificationFamilyValue(VerificationFamilyValueCommand command);

    void deleteVerificationRestrictionFlags(@Param("verificationId") UUID verificationId);

    void insertVerificationRestrictionFlag(VerificationRestrictionFlagCommand command);

    void deleteVerificationDocuments(@Param("verificationId") UUID verificationId);

    void insertVerificationDocument(VerificationDocumentCommand command);

    int updatePartnerVerificationStatus(PartnerVerificationStatusCommand command);

    void insertAuditLog(AuditLogCommand command);
}
