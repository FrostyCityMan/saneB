package com.saneb.domain.auth.dao;

import com.saneb.domain.auth.vo.AdminBootstrapCommand;
import java.util.UUID;
import org.apache.ibatis.annotations.Param;

public interface AdminBootstrapDao {

    int selectActiveAdminCount();

    UUID selectUserIdByLoginId(@Param("loginId") String loginId);

    void insertAdminUser(AdminBootstrapCommand command);

    int updateAdminUser(AdminBootstrapCommand command);

    void insertAdminRole(@Param("userId") UUID userId);
}
