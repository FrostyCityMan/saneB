package com.saneb.domain.consent.dao;

import com.saneb.domain.consent.vo.ConsentVersionRow;
import com.saneb.domain.consent.vo.UserConsentInsertCommand;
import com.saneb.domain.consent.vo.UserConsentRow;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ConsentDao {

    List<ConsentVersionRow> selectCurrentConsentVersionList();

    ConsentVersionRow selectCurrentConsentVersionDetailsByCode(@Param("consentCode") String consentCode);

    List<UserConsentRow> selectUserConsentList(@Param("userId") UUID userId);

    UUID insertUserConsent(UserConsentInsertCommand command);

    UserConsentRow selectUserConsentDetails(@Param("userConsentId") UUID userConsentId);
}
