package com.saneb.domain.standardcode.dao;

import com.saneb.domain.standardcode.vo.StandardCodeGroupRow;
import com.saneb.domain.standardcode.vo.StandardCodeRow;
import com.saneb.domain.standardcode.vo.StandardCodeSearchCondition;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StandardCodeDao {

    List<StandardCodeGroupRow> selectStandardCodeGroupList();

    List<StandardCodeRow> selectStandardCodeList(StandardCodeSearchCondition condition);

    long selectStandardCodeCount(StandardCodeSearchCondition condition);
}
