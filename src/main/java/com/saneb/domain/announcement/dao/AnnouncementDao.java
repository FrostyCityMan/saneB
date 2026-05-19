package com.saneb.domain.announcement.dao;

import com.saneb.domain.announcement.vo.AnnouncementDetailsRow;
import com.saneb.domain.announcement.vo.AnnouncementDocumentRequirementCommand;
import com.saneb.domain.announcement.vo.AnnouncementDocumentRequirementRow;
import com.saneb.domain.announcement.vo.AnnouncementIndustryConditionCommand;
import com.saneb.domain.announcement.vo.AnnouncementIndustryConditionRow;
import com.saneb.domain.announcement.vo.AnnouncementManualStatusCommand;
import com.saneb.domain.announcement.vo.AnnouncementNumericConditionCommand;
import com.saneb.domain.announcement.vo.AnnouncementNumericConditionRow;
import com.saneb.domain.announcement.vo.AnnouncementOptionCommand;
import com.saneb.domain.announcement.vo.AnnouncementOptionConditionCommand;
import com.saneb.domain.announcement.vo.AnnouncementOptionConditionRow;
import com.saneb.domain.announcement.vo.AnnouncementOptionRow;
import com.saneb.domain.announcement.vo.AnnouncementProgressStepCommand;
import com.saneb.domain.announcement.vo.AnnouncementProgressStepRow;
import com.saneb.domain.announcement.vo.AnnouncementSaveCommand;
import com.saneb.domain.announcement.vo.AnnouncementSearchCondition;
import com.saneb.domain.announcement.vo.AnnouncementStatusHistoryCommand;
import com.saneb.domain.announcement.vo.AnnouncementStepButtonCommand;
import com.saneb.domain.announcement.vo.AnnouncementStepButtonRow;
import com.saneb.domain.announcement.vo.AnnouncementStepDocumentCommand;
import com.saneb.domain.announcement.vo.AnnouncementStepDocumentRow;
import com.saneb.domain.announcement.vo.AnnouncementSummaryRow;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AnnouncementDao {

    List<AnnouncementSummaryRow> selectAnnouncementList(AnnouncementSearchCondition condition);

    long selectAnnouncementCount(AnnouncementSearchCondition condition);

    AnnouncementDetailsRow selectAnnouncementDetails(@Param("announcementId") UUID announcementId);

    List<AnnouncementOptionRow> selectAnnouncementOptionList(@Param("announcementId") UUID announcementId);

    List<AnnouncementIndustryConditionRow> selectAnnouncementIndustryConditionList(
            @Param("announcementId") UUID announcementId
    );

    List<AnnouncementNumericConditionRow> selectAnnouncementNumericConditionList(
            @Param("announcementId") UUID announcementId
    );

    List<AnnouncementOptionConditionRow> selectAnnouncementOptionConditionList(
            @Param("announcementId") UUID announcementId
    );

    List<AnnouncementDocumentRequirementRow> selectAnnouncementDocumentRequirementList(
            @Param("announcementId") UUID announcementId
    );

    List<AnnouncementProgressStepRow> selectAnnouncementProgressStepList(
            @Param("announcementId") UUID announcementId
    );

    List<AnnouncementStepDocumentRow> selectAnnouncementStepDocumentList(
            @Param("announcementId") UUID announcementId
    );

    List<AnnouncementStepButtonRow> selectAnnouncementStepButtonList(
            @Param("announcementId") UUID announcementId
    );

    void insertAnnouncement(AnnouncementSaveCommand command);

    int updateAnnouncement(AnnouncementSaveCommand command);

    int updateAnnouncementManualStatus(AnnouncementManualStatusCommand command);

    void insertAnnouncementStatusHistory(AnnouncementStatusHistoryCommand command);

    void deleteAnnouncementOptions(@Param("announcementId") UUID announcementId);

    void insertAnnouncementOption(AnnouncementOptionCommand command);

    void deleteAnnouncementIndustryConditions(@Param("announcementId") UUID announcementId);

    void insertAnnouncementIndustryCondition(AnnouncementIndustryConditionCommand command);

    void deleteAnnouncementNumericConditions(@Param("announcementId") UUID announcementId);

    void insertAnnouncementNumericCondition(AnnouncementNumericConditionCommand command);

    void deleteAnnouncementOptionConditions(@Param("announcementId") UUID announcementId);

    void insertAnnouncementOptionCondition(AnnouncementOptionConditionCommand command);

    void deleteAnnouncementDocumentRequirements(@Param("announcementId") UUID announcementId);

    void insertAnnouncementDocumentRequirement(AnnouncementDocumentRequirementCommand command);

    void deleteAnnouncementStepButtons(@Param("announcementId") UUID announcementId);

    void deleteAnnouncementStepDocuments(@Param("announcementId") UUID announcementId);

    void deleteAnnouncementProgressSteps(@Param("announcementId") UUID announcementId);

    void insertAnnouncementProgressStep(AnnouncementProgressStepCommand command);

    void insertAnnouncementStepButton(AnnouncementStepButtonCommand command);

    void insertAnnouncementStepDocument(AnnouncementStepDocumentCommand command);
}
