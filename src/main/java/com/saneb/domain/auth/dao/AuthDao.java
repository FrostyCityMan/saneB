package com.saneb.domain.auth.dao;

import com.saneb.domain.auth.vo.AuthLoginHistoryCommand;
import com.saneb.domain.auth.vo.AuthPasswordUpdateCommand;
import com.saneb.domain.auth.vo.AuthSignupCommand;
import com.saneb.domain.auth.vo.AuthUserDetailsRow;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AuthDao {

    AuthUserDetailsRow selectAuthUserDetailsByLoginId(@Param("loginId") String loginId);

    UUID selectUserIdByPhone(@Param("phone") String phone);

    List<String> selectRoleCodeListByUserId(@Param("userId") UUID userId);

    UUID insertUser(AuthSignupCommand command);

    void insertUserRole(@Param("userId") UUID userId, @Param("roleCode") String roleCode);

    void insertAuthLoginHistory(AuthLoginHistoryCommand command);

    void updateUserLastLoginAt(@Param("userId") UUID userId);

    void updatePassword(AuthPasswordUpdateCommand command);
}
