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

    public DbBackedUserDetailsService(AuthDao authDao) {
        this.authDao = authDao;
    }

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
