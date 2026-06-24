/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: AdminBootstrapServiceImplTest.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.auth.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.saneb.domain.auth.dao.AdminBootstrapDao;
import com.saneb.domain.auth.vo.AdminBootstrapCommand;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapServiceImplTest {

    @Mock
    private AdminBootstrapDao adminBootstrapDao;

    @Mock
    private PasswordEncoder passwordEncoder;

    /**
     * 업무 데이터를 저장합니다.
     */
    @Test
    void saveBootstrapAdminSkipsWhenActiveAdminExists() {
        when(adminBootstrapDao.selectActiveAdminCount()).thenReturn(1);
        AdminBootstrapServiceImpl service = new AdminBootstrapServiceImpl(adminBootstrapDao, passwordEncoder);

        service.saveBootstrapAdmin("admin", "StrongPassword!234", "관리자");

        verify(adminBootstrapDao, never()).selectUserIdByLoginId(any());
        verify(adminBootstrapDao, never()).insertAdminUser(any());
        verify(adminBootstrapDao, never()).insertAdminRole(any());
    }

    /**
     * 업무 데이터를 저장합니다.
     */
    @Test
    void saveBootstrapAdminInsertsAdminWhenNoActiveAdminExists() {
        when(adminBootstrapDao.selectActiveAdminCount()).thenReturn(0);
        when(adminBootstrapDao.selectUserIdByLoginId("admin")).thenReturn(null);
        when(passwordEncoder.encode("StrongPassword!234")).thenReturn("bcrypt-hash");
        AdminBootstrapServiceImpl service = new AdminBootstrapServiceImpl(adminBootstrapDao, passwordEncoder);

        service.saveBootstrapAdmin(" admin ", "StrongPassword!234", " 관리자 ");

        ArgumentCaptor<AdminBootstrapCommand> captor = ArgumentCaptor.forClass(AdminBootstrapCommand.class);
        verify(adminBootstrapDao).insertAdminUser(captor.capture());
        verify(adminBootstrapDao).insertAdminRole(captor.getValue().userId());
        assertThat(captor.getValue().loginId()).isEqualTo("admin");
        assertThat(captor.getValue().passwordHash()).isEqualTo("bcrypt-hash");
        assertThat(captor.getValue().name()).isEqualTo("관리자");
    }

    /**
     * 업무 데이터를 저장합니다.
     */
    @Test
    void saveBootstrapAdminUpdatesExistingLoginWhenNoActiveAdminExists() {
        UUID existingUserId = UUID.randomUUID();
        when(adminBootstrapDao.selectActiveAdminCount()).thenReturn(0);
        when(adminBootstrapDao.selectUserIdByLoginId("admin")).thenReturn(existingUserId);
        when(passwordEncoder.encode("StrongPassword!234")).thenReturn("bcrypt-hash");
        AdminBootstrapServiceImpl service = new AdminBootstrapServiceImpl(adminBootstrapDao, passwordEncoder);

        service.saveBootstrapAdmin("admin", "StrongPassword!234", "관리자");

        ArgumentCaptor<AdminBootstrapCommand> captor = ArgumentCaptor.forClass(AdminBootstrapCommand.class);
        verify(adminBootstrapDao).updateAdminUser(captor.capture());
        verify(adminBootstrapDao).insertAdminRole(existingUserId);
        assertThat(captor.getValue().userId()).isEqualTo(existingUserId);
    }

    /**
     * 업무 데이터를 저장합니다.
     */
    @Test
    void saveBootstrapAdminRejectsShortPassword() {
        AdminBootstrapServiceImpl service = new AdminBootstrapServiceImpl(adminBootstrapDao, passwordEncoder);

        assertThatThrownBy(() -> service.saveBootstrapAdmin("admin", "short", "관리자"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least 12 characters");
    }
}
