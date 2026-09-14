package com.saneb.domain.announcementattachment.dao;

import com.saneb.domain.announcementattachment.vo.AttachmentJobRow;
import com.saneb.domain.announcementattachment.vo.AttachmentRoleRows;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AnnouncementAttachmentRoleDao {
    AttachmentJobRow selectSetJobDetails(@Param("sourceId") UUID sourceId,@Param("setId") UUID setId,@Param("evaluationId") UUID evaluationId);
    List<AttachmentRoleRows.File> selectFileCopyList(@Param("sourceId") UUID sourceId,@Param("setId") UUID setId);
    int insertFileCopy(AttachmentRoleRows.Copy command);
    int insertExtractionCopy(AttachmentRoleRows.Copy command);
}
