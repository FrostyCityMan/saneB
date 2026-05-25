package com.saneb.domain.dynamicinput.dao;

import com.saneb.domain.dynamicinput.vo.AnnouncementInputOptionCommand;
import com.saneb.domain.dynamicinput.vo.AnnouncementInputOptionRow;
import com.saneb.domain.dynamicinput.vo.AnnouncementInputRequirementCommand;
import com.saneb.domain.dynamicinput.vo.AnnouncementInputRequirementRow;
import com.saneb.domain.dynamicinput.vo.ApplicationInputValueCommand;
import com.saneb.domain.dynamicinput.vo.ApplicationInputValueRow;
import com.saneb.domain.dynamicinput.vo.ApplicationProgressInputRow;
import com.saneb.domain.dynamicinput.vo.AuditLogCommand;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DynamicAnnouncementInputDao {

    long selectAnnouncementCount(@Param("announcementId") UUID announcementId);

    List<AnnouncementInputRequirementRow> selectAnnouncementInputRequirementList(
            @Param("announcementId") UUID announcementId
    );

    List<AnnouncementInputOptionRow> selectAnnouncementInputOptionList(
            @Param("announcementId") UUID announcementId
    );

    List<AnnouncementInputOptionRow> selectAnnouncementInputOptionListByRequirementId(
            @Param("requirementId") UUID requirementId
    );

    long selectApplicationProgressCountByAnnouncementId(@Param("announcementId") UUID announcementId);

    long selectApplicationInputValueCountByRequirementId(@Param("requirementId") UUID requirementId);

    long selectApplicationInputValueCountByRequirementOption(
            @Param("requirementId") UUID requirementId,
            @Param("optionCode") String optionCode
    );

    void insertAnnouncementInputRequirement(AnnouncementInputRequirementCommand command);

    int updateAnnouncementInputRequirement(AnnouncementInputRequirementCommand command);

    void deleteAnnouncementInputOptionsByRequirementId(@Param("requirementId") UUID requirementId);

    void deleteAnnouncementInputOption(
            @Param("requirementId") UUID requirementId,
            @Param("optionCode") String optionCode
    );

    void deleteAnnouncementInputRequirement(@Param("requirementId") UUID requirementId);

    void insertAnnouncementInputOption(AnnouncementInputOptionCommand command);

    int updateAnnouncementInputOption(AnnouncementInputOptionCommand command);

    ApplicationProgressInputRow selectApplicationProgressForInput(@Param("progressId") UUID progressId);

    List<ApplicationInputValueRow> selectApplicationInputValueList(@Param("progressId") UUID progressId);

    void deleteApplicationInputValues(@Param("progressId") UUID progressId);

    void insertApplicationInputValue(ApplicationInputValueCommand command);

    long selectMissingRequiredApplicationInputCount(@Param("progressId") UUID progressId);

    void insertAuditLog(AuditLogCommand command);
}
