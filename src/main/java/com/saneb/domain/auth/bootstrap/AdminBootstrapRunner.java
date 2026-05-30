package com.saneb.domain.auth.bootstrap;

import com.saneb.domain.auth.service.AdminBootstrapService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final Environment environment;
    private final AdminBootstrapService adminBootstrapService;

    public AdminBootstrapRunner(Environment environment, AdminBootstrapService adminBootstrapService) {
        this.environment = environment;
        this.adminBootstrapService = adminBootstrapService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!selectEnabled()) {
            return;
        }

        String loginId = selectRequiredProperty("saneb.bootstrap.admin.login-id");
        String password = selectRequiredProperty("saneb.bootstrap.admin.password");
        String name = selectOptionalProperty("saneb.bootstrap.admin.name", "기초 관리자");
        adminBootstrapService.saveBootstrapAdmin(loginId, password, name);
        log.info("Initial admin bootstrap check completed.");
    }

    private boolean selectEnabled() {
        return Boolean.TRUE.equals(environment.getProperty(
                "saneb.bootstrap.admin.enabled",
                Boolean.class,
                false
        ));
    }

    private String selectRequiredProperty(String key) {
        String value = selectOptionalProperty(key, null);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(key + " is required when admin bootstrap is enabled.");
        }
        return value.trim();
    }

    private String selectOptionalProperty(String key, String defaultValue) {
        String value = environment.getProperty(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim();
    }
}
