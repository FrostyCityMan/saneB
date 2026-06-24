/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: AdminBootstrapRunner.java
 * 작성자: 김도훈
 *
 */

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

    /**
     * 객체를 생성합니다.
     *
     * @param environment 입력 값
     *
     * @param adminBootstrapService 입력 값
     */
    public AdminBootstrapRunner(Environment environment, AdminBootstrapService adminBootstrapService) {
        this.environment = environment;
        this.adminBootstrapService = adminBootstrapService;
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param args 입력 값
     */
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

    /**
     * 업무 데이터를 조회합니다.
     *
     * @return 처리 결과
     */
    private boolean selectEnabled() {
        return Boolean.TRUE.equals(environment.getProperty(
                "saneb.bootstrap.admin.enabled",
                Boolean.class,
                false
        ));
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param key 입력 값
     *
     * @return 처리 결과
     */
    private String selectRequiredProperty(String key) {
        String value = selectOptionalProperty(key, null);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(key + " is required when admin bootstrap is enabled.");
        }
        return value.trim();
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param key 입력 값
     *
     * @param defaultValue 입력 값
     *
     * @return 처리 결과
     */
    private String selectOptionalProperty(String key, String defaultValue) {
        String value = environment.getProperty(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim();
    }
}
