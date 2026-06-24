/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: DbBackedUserDetailsService.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.auth.service.impl;

import com.saneb.domain.auth.dao.AuthDao;
import com.saneb.domain.auth.vo.AuthUserDetailsRow;
import com.saneb.domain.auth.vo.AuthenticatedUserDetails;
import java.util.Comparator;
import java.util.List;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class DbBackedUserDetailsService implements UserDetailsService {

    private final AuthDao authDao;

    /**
     * 객체를 생성합니다.
     *
     * @param authDao 입력 값
     */
    public DbBackedUserDetailsService(AuthDao authDao) {
        this.authDao = authDao;
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param username 입력 값
     *
     * @return 처리 결과
     */
    @Override
    public UserDetails loadUserByUsername(String username) {
        AuthUserDetailsRow user = authDao.selectAuthUserDetailsByLoginId(username);
        if (user == null) {
            throw new UsernameNotFoundException("User was not found.");
        }

        List<String> roles = authDao.selectRoleCodeListByUserId(user.userId()).stream()
                .sorted(Comparator.comparingInt(this::selectRolePriority))
                .toList();
        return new AuthenticatedUserDetails(user, roles.isEmpty() ? List.of("USER") : roles);
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param role 입력 값
     *
     * @return 처리 결과
     */
    private int selectRolePriority(String role) {
        return switch (role) {
            case "ADMIN" -> 1;
            case "APPROVER" -> 2;
            case "OPERATOR" -> 3;
            case "REVIEWER" -> 4;
            case "PARTNER" -> 5;
            default -> 6;
        };
    }
}
