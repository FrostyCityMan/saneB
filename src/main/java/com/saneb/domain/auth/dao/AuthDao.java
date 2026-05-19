package com.saneb.domain.auth.dao;

import com.saneb.domain.auth.vo.AuthLoginHistoryCommand;
import com.saneb.domain.auth.vo.AuthPasswordUpdateCommand;
import com.saneb.domain.auth.vo.AuthUserDetailsRow;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AuthDao {

    AuthUserDetailsRow selectAuthUserDetailsByLoginId(@Param("loginId") String loginId);

    List<String> selectRoleCodeListByUserId(@Param("userId") UUID userId);

    void insertAuthLoginHistory(AuthLoginHistoryCommand command);

    void updateUserLastLoginAt(@Param("userId") UUID userId);

    void updatePassword(AuthPasswordUpdateCommand command);
}
