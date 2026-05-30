package com.saneb.domain.auth.service.impl;

import com.saneb.domain.auth.dao.AdminBootstrapDao;
import com.saneb.domain.auth.service.AdminBootstrapService;
import com.saneb.domain.auth.vo.AdminBootstrapCommand;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminBootstrapServiceImpl implements AdminBootstrapService {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapServiceImpl.class);
    private static final int MIN_BOOTSTRAP_PASSWORD_LENGTH = 12;

    private final AdminBootstrapDao adminBootstrapDao;
    private final PasswordEncoder passwordEncoder;

    public AdminBootstrapServiceImpl(AdminBootstrapDao adminBootstrapDao, PasswordEncoder passwordEncoder) {
        this.adminBootstrapDao = adminBootstrapDao;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void saveBootstrapAdmin(String loginId, String rawPassword, String name) {
        String normalizedLoginId = normalizeRequired("loginId", loginId);
        String normalizedName = normalizeRequired("name", name);
        validatePassword(rawPassword);

        if (adminBootstrapDao.selectActiveAdminCount() > 0) {
            log.info("Active admin account already exists. Admin bootstrap skipped.");
            return;
        }

        UUID userId = adminBootstrapDao.selectUserIdByLoginId(normalizedLoginId);
        AdminBootstrapCommand command = new AdminBootstrapCommand(
                userId == null ? UUID.randomUUID() : userId,
                normalizedLoginId,
                passwordEncoder.encode(rawPassword),
                normalizedName
        );

        if (userId == null) {
            adminBootstrapDao.insertAdminUser(command);
            log.info("Initial admin account inserted by bootstrap.");
        } else {
            adminBootstrapDao.updateAdminUser(command);
            log.info("Existing bootstrap login account was promoted to admin.");
        }
        adminBootstrapDao.insertAdminRole(command.userId());
    }

    private String normalizeRequired(String field, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required.");
        }
        return value.trim();
    }

    private void validatePassword(String rawPassword) {
        if (rawPassword == null || rawPassword.length() < MIN_BOOTSTRAP_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("Bootstrap admin password must be at least 12 characters.");
        }
    }
}
