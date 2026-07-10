/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: LocalGovernmentNoticeServiceImplTest.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.announcementsource.localgov.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.saneb.common.error.ApiException;
import com.saneb.domain.announcementsource.dao.LocalGovernmentNoticeDao;
import com.saneb.domain.announcementsource.localgov.dto.LocalGovernmentNoticeQaCleanupRequest;
import com.saneb.domain.announcementsource.localgov.dto.LocalGovernmentNoticeQaCleanupResponse;
import com.saneb.domain.announcementsource.localgov.support.LocalGovernmentNoticeUrlValidator;
import com.saneb.domain.announcementsource.service.AnnouncementSourceService;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceAuditLogCommand;
import com.saneb.domain.auth.vo.AuthUserDetailsRow;
import com.saneb.domain.auth.vo.AuthenticatedUserDetails;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class LocalGovernmentNoticeServiceImplTest {

    private static final UUID ADMIN_ID = UUID.fromString("97000000-0000-0000-0000-000000000001");

    @Mock
    private LocalGovernmentNoticeDao localGovernmentNoticeDao;

    @Mock
    private AnnouncementSourceService announcementSourceService;

    @Mock
    private LocalGovernmentNoticeUrlValidator urlValidator;

    private LocalGovernmentNoticeServiceImpl service;

    /**
     * 테스트 대상 서비스를 생성합니다.
     */
    @BeforeEach
    void setUp() {
        service = new LocalGovernmentNoticeServiceImpl(
                localGovernmentNoticeDao,
                announcementSourceService,
                urlValidator
        );
    }

    /**
     * 운영 공고와 연결된 원문이 있으면 QA 정리를 차단합니다.
     */
    @Test
    void deleteQaArtifactsRejectsLinkedSnapshot() {
        when(localGovernmentNoticeDao.selectLinkedQaSnapshotCount()).thenReturn(1L);

        assertThatThrownBy(() -> service.deleteQaArtifacts(authentication(), cleanupRequest()))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("운영 공고와 연결된 수집 원문");

        verify(localGovernmentNoticeDao, never()).deleteQaCollectionRunList();
        verify(localGovernmentNoticeDao, never()).deleteQaSnapshotList();
    }

    /**
     * 연결되지 않은 QA 원문과 수집 이력을 순서대로 정리합니다.
     */
    @Test
    void deleteQaArtifactsDeletesOnlyLocalGovernmentCollectionArtifacts() {
        when(localGovernmentNoticeDao.selectLinkedQaSnapshotCount()).thenReturn(0L);
        when(localGovernmentNoticeDao.deleteQaScheduleExecutionList()).thenReturn(2);
        when(localGovernmentNoticeDao.deleteQaCollectionRunList()).thenReturn(6);
        when(localGovernmentNoticeDao.deleteQaCollectionRequestList()).thenReturn(6);
        when(localGovernmentNoticeDao.deleteQaSnapshotList()).thenReturn(25);
        when(localGovernmentNoticeDao.resetQaSourceCollectionState()).thenReturn(244);

        LocalGovernmentNoticeQaCleanupResponse response = service.deleteQaArtifacts(
                authentication(),
                cleanupRequest()
        );

        assertThat(response.deletedScheduleExecutionCount()).isEqualTo(2);
        assertThat(response.deletedRunCount()).isEqualTo(6);
        assertThat(response.deletedRequestCount()).isEqualTo(6);
        assertThat(response.deletedSnapshotCount()).isEqualTo(25);
        assertThat(response.resetSourceCount()).isEqualTo(244);

        ArgumentCaptor<AnnouncementSourceAuditLogCommand> auditCaptor =
                ArgumentCaptor.forClass(AnnouncementSourceAuditLogCommand.class);
        verify(localGovernmentNoticeDao).insertAuditLog(auditCaptor.capture());
        assertThat(auditCaptor.getValue().actionCode()).isEqualTo("LOCAL_GOV_NOTICE_QA_ARTIFACTS_DELETE");
        assertThat(auditCaptor.getValue().metadataJson()).contains("\"deletedSnapshotCount\":25");
    }

    /**
     * 관리자 인증 객체를 생성합니다.
     */
    private Authentication authentication() {
        AuthenticatedUserDetails principal = new AuthenticatedUserDetails(
                new AuthUserDetailsRow(
                        ADMIN_ID,
                        "admin",
                        "hash",
                        "관리자",
                        "ACTIVE",
                        false,
                        null,
                        null,
                        null
                ),
                List.of("ADMIN")
        );
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    /**
     * 올바른 삭제 확인 요청을 생성합니다.
     */
    private LocalGovernmentNoticeQaCleanupRequest cleanupRequest() {
        return new LocalGovernmentNoticeQaCleanupRequest("DELETE_LOCAL_GOVERNMENT_QA_DATA");
    }
}
