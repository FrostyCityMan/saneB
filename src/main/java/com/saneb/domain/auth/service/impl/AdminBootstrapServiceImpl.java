/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: AdminBootstrapServiceImpl.java
 * 작성자: 김도훈
 *
 */

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

    /**
     * 객체를 생성합니다.
     *
     * @param adminBootstrapDao 입력 값
     *
     * @param passwordEncoder 입력 값
     */
    public AdminBootstrapServiceImpl(AdminBootstrapDao adminBootstrapDao, PasswordEncoder passwordEncoder) {
        this.adminBootstrapDao = adminBootstrapDao;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 업무 데이터를 저장합니다.
     *
     * @param loginId 입력 값
     *
     * @param rawPassword 입력 값
     *
     * @param name 입력 값
     */
    @Override
    /**
     * 업무 데이터를 저장합니다.
     *
     * @param loginId 입력 값
     *
     * @param rawPassword 입력 값
     *
     * @param name 입력 값
     */
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

    /**
     * 입력 값을 표준 형식으로 정규화합니다.
     *
     * @param field 입력 값
     *
     * @param value 입력 값
     *
     * @return 처리 결과
     */
    private String normalizeRequired(String field, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required.");
        }
        return value.trim();
    }

    /**
     * 요청 값과 업무 규칙을 검증합니다.
     *
     * @param rawPassword 입력 값
     */
    private void validatePassword(String rawPassword) {
        if (rawPassword == null || rawPassword.length() < MIN_BOOTSTRAP_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("Bootstrap admin password must be at least 12 characters.");
        }
    }
}
