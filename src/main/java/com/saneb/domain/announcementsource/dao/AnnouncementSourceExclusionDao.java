package com.saneb.domain.announcementsource.dao;

import com.saneb.domain.announcementsource.vo.AnnouncementSourceExclusionCountRow;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AnnouncementSourceExclusionDao {
    List<AnnouncementSourceExclusionCountRow> selectExclusionCountList();
}
