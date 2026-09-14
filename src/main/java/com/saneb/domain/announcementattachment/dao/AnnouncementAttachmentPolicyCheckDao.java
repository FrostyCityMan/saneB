package com.saneb.domain.announcementattachment.dao;

import com.saneb.domain.announcementattachment.vo.AttachmentPolicyCheckRows;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AnnouncementAttachmentPolicyCheckDao {
    List<AttachmentPolicyCheckRows.Row> selectCheckList(AttachmentPolicyCheckRows.Search search);
    long selectCheckCount(AttachmentPolicyCheckRows.Search search);
    AttachmentPolicyCheckRows.Row selectCheckDetails(@Param("key") UUID key);
    String selectRequestLock(@Param("key") UUID key);
    int insertCheck(AttachmentPolicyCheckRows.Insert command);
}
