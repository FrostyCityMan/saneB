package com.saneb.domain.announcementattachment.dao;

import com.saneb.domain.announcementattachment.dto.AttachmentBatchHistoryResponses.Entry;
import com.saneb.domain.announcementattachment.vo.AttachmentBatchHistorySearch;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AnnouncementAttachmentBatchHistoryDao {
    long selectActionCount(AttachmentBatchHistorySearch search);
    List<Entry> selectActionList(AttachmentBatchHistorySearch search);
}
