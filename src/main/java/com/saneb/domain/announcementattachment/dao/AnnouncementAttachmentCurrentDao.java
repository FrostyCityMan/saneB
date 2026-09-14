package com.saneb.domain.announcementattachment.dao;

import com.saneb.domain.announcementattachment.vo.AttachmentCurrentSourceRow;
import com.saneb.domain.announcementattachment.vo.AttachmentSourceSearchCondition;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AnnouncementAttachmentCurrentDao {
    List<AttachmentCurrentSourceRow> selectSourceList(AttachmentSourceSearchCondition condition);
    long selectSourceCount(AttachmentSourceSearchCondition condition);
    AttachmentCurrentSourceRow selectSourceDetails(@Param("sourceId") UUID sourceId);
}
