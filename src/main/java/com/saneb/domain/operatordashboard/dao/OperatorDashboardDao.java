package com.saneb.domain.operatordashboard.dao;

import com.saneb.domain.operatordashboard.vo.OperatorAnnouncementWorkRow;
import com.saneb.domain.operatordashboard.vo.OperatorApplicationProgressWorkRow;
import com.saneb.domain.operatordashboard.vo.OperatorMatchingWorkRow;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OperatorDashboardDao {

    OperatorAnnouncementWorkRow selectAnnouncementWork();

    OperatorMatchingWorkRow selectMatchingWork();

    OperatorApplicationProgressWorkRow selectApplicationProgressWork();
}
