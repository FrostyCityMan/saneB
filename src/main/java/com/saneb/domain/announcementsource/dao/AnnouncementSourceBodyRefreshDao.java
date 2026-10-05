package com.saneb.domain.announcementsource.dao;

import com.saneb.domain.announcementsource.vo.SourceBodyRefreshRows;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AnnouncementSourceBodyRefreshDao {
    SourceBodyRefreshRows.Boundary selectBoundaryDetails(@Param("sourceId") UUID sourceId);
    int insertPreview(SourceBodyRefreshRows.Preview preview);
    SourceBodyRefreshRows.Preview selectPreviewDetailsForUpdate(@Param("sourceId") UUID sourceId, @Param("previewId") UUID previewId);
    int updateBody(@Param("sourceId") UUID sourceId, @Param("bodyText") String bodyText, @Param("expectedVersion") int expectedVersion);
    int updateApplied(@Param("previewId") UUID previewId, @Param("evaluationId") UUID evaluationId);
}
