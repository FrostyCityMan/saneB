package com.saneb.domain.standardcode.service;

import com.saneb.common.response.PageResponse;
import com.saneb.domain.standardcode.dto.StandardCodeGroupResponse;
import com.saneb.domain.standardcode.dto.StandardCodeResponse;
import java.util.List;

public interface StandardCodeService {

    List<StandardCodeGroupResponse> selectStandardCodeGroupList();

    PageResponse<StandardCodeResponse> selectStandardCodeList(
            String groupCode,
            String keyword,
            Boolean active,
            int page,
            int size
    );
}
