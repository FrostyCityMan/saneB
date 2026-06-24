/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: AuthenticatedUserDetails.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.auth.vo;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class AuthenticatedUserDetails implements UserDetails {

    private final AuthUserDetailsRow user;
    private final List<String> roles;

    /**
     * 객체를 생성합니다.
     *
     * @param user 입력 값
     *
     * @param roles 입력 값
     */
    public AuthenticatedUserDetails(AuthUserDetailsRow user, List<String> roles) {
        this.user = user;
        this.roles = List.copyOf(roles);
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @return 처리 결과
     */
    public UUID userId() {
        return user.userId();
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @return 처리 결과
     */
    public String loginId() {
        return user.loginId();
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @return 처리 결과
     */
    public String name() {
        return user.name();
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @return 처리 결과
     */
    public String statusCode() {
        return user.statusCode();
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @return 처리 결과
     */
    public boolean passwordResetRequired() {
        return Boolean.TRUE.equals(user.passwordResetRequired());
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @return 처리 결과
     */
    public UUID memberProfileId() {
        return user.memberProfileId();
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @return 처리 결과
     */
    public UUID businessProfileId() {
        return user.businessProfileId();
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @return 처리 결과
     */
    public UUID partnerProfileId() {
        return user.partnerProfileId();
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @return 처리 결과
     */
    public List<String> roles() {
        return roles;
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @return 처리 결과
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @return 처리 결과
     */
    @Override
    public String getPassword() {
        return user.passwordHash();
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @return 처리 결과
     */
    @Override
    public String getUsername() {
        return user.loginId();
    }

    /**
     * 조건 충족 여부를 확인합니다.
     *
     * @return 처리 결과
     */
    @Override
    public boolean isAccountNonLocked() {
        return !"LOCKED".equals(user.statusCode());
    }

    /**
     * 조건 충족 여부를 확인합니다.
     *
     * @return 처리 결과
     */
    @Override
    public boolean isEnabled() {
        return "ACTIVE".equals(user.statusCode());
    }
}
