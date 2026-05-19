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

    public AuthenticatedUserDetails(AuthUserDetailsRow user, List<String> roles) {
        this.user = user;
        this.roles = List.copyOf(roles);
    }

    public UUID userId() {
        return user.userId();
    }

    public String loginId() {
        return user.loginId();
    }

    public String name() {
        return user.name();
    }

    public String statusCode() {
        return user.statusCode();
    }

    public boolean passwordResetRequired() {
        return Boolean.TRUE.equals(user.passwordResetRequired());
    }

    public UUID memberProfileId() {
        return user.memberProfileId();
    }

    public UUID businessProfileId() {
        return user.businessProfileId();
    }

    public UUID partnerProfileId() {
        return user.partnerProfileId();
    }

    public List<String> roles() {
        return roles;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
    }

    @Override
    public String getPassword() {
        return user.passwordHash();
    }

    @Override
    public String getUsername() {
        return user.loginId();
    }

    @Override
    public boolean isAccountNonLocked() {
        return !"LOCKED".equals(user.statusCode());
    }

    @Override
    public boolean isEnabled() {
        return "ACTIVE".equals(user.statusCode());
    }
}
