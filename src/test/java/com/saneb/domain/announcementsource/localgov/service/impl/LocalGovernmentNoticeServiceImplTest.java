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
import com.saneb.domain.announcementsource.localgov.dto.LocalGovernmentNoticeSourceEnabledRequest;
import com.saneb.domain.announcementsource.localgov.dto.LocalGovernmentNoticeSourceSaveRequest;
import com.saneb.domain.announcementsource.localgov.support.LocalGovernmentNoticeUrlValidator;
import com.saneb.domain.announcementsource.localgov.vo.LocalGovernmentNoticeSourceCommand;
import com.saneb.domain.announcementsource.localgov.vo.LocalGovernmentNoticeSourceEnabledCommand;
import com.saneb.domain.announcementsource.localgov.vo.LocalGovernmentNoticeSourceRow;
import com.saneb.domain.announcementsource.service.AnnouncementSourceService;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceAuditLogCommand;
import com.saneb.domain.auth.vo.AuthUserDetailsRow;
import com.saneb.domain.auth.vo.AuthenticatedUserDetails;
import java.time.OffsetDateTime;
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
    private static final UUID SOURCE_ID = UUID.fromString("97000000-0000-0000-0000-000000000002");

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
     * 신규 출처는 사용자 요청값과 관계없이 시스템 대기 파서로 등록합니다.
     */
    @Test
    void insertSourceAssignsSystemManagedParser() {
        when(localGovernmentNoticeDao.selectSourceDetails(any(UUID.class)))
                .thenReturn(sourceRow("MANUAL_ONLY"));

        service.insertSource(authentication(), sourceRequest("SPRING_BBS"));

        ArgumentCaptor<LocalGovernmentNoticeSourceCommand> commandCaptor =
                ArgumentCaptor.forClass(LocalGovernmentNoticeSourceCommand.class);
        verify(localGovernmentNoticeDao).insertSource(commandCaptor.capture());
        assertThat(commandCaptor.getValue().parserProfileCode()).isEqualTo("MANUAL_ONLY");
    }

    /**
     * 출처 수정 시 사용자가 보낸 파서 값 대신 기존 시스템 배정을 보존합니다.
     */
    @Test
    void updateSourcePreservesSystemManagedParser() {
        LocalGovernmentNoticeSourceRow existing = sourceRow("SAEOL_GOSI");
        when(localGovernmentNoticeDao.selectSourceDetails(SOURCE_ID)).thenReturn(existing);
        when(localGovernmentNoticeDao.updateSource(any(LocalGovernmentNoticeSourceCommand.class))).thenReturn(1);

        service.updateSource(authentication(), SOURCE_ID, sourceRequest("SPRING_BBS"));

        ArgumentCaptor<LocalGovernmentNoticeSourceCommand> commandCaptor =
                ArgumentCaptor.forClass(LocalGovernmentNoticeSourceCommand.class);
        verify(localGovernmentNoticeDao).updateSource(commandCaptor.capture());
        assertThat(commandCaptor.getValue().parserProfileCode()).isEqualTo("SAEOL_GOSI");
    }

    /**
     * 자동수집 준비 전에는 기술적인 파서 선택 요구 없이 ON 전환을 차단합니다.
     */
    @Test
    void updateSourceEnabledExplainsAutomaticCollectionReadiness() {
        when(localGovernmentNoticeDao.selectSourceDetails(SOURCE_ID))
                .thenReturn(sourceRow("MANUAL_ONLY"));

        assertThatThrownBy(() -> service.updateSourceEnabled(
                authentication(),
                SOURCE_ID,
                new LocalGovernmentNoticeSourceEnabledRequest(true)
        ))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("자동수집 준비가 완료되지 않았습니다");

        verify(localGovernmentNoticeDao, never())
                .updateSourceEnabled(any(LocalGovernmentNoticeSourceEnabledCommand.class));
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

    /**
     * 출처 저장 요청을 생성합니다.
     *
     * @param parserProfileCode 클라이언트가 보낸 파서 코드
     * @return 저장 요청
     */
    private LocalGovernmentNoticeSourceSaveRequest sourceRequest(String parserProfileCode) {
        return new LocalGovernmentNoticeSourceSaveRequest(
                "11",
                "서울특별시",
                "11680",
                "강남구",
                "BASIC_LOCAL_GOVERNMENT",
                "강남구청",
                "https://www.gangnam.go.kr",
                "https://www.gangnam.go.kr/notice/list.do?mid=ID05_040201",
                null,
                "public_notice_board",
                "DEFAULT",
                parserProfileCode,
                null,
                "HIGH",
                "VERIFIED",
                "LEGAL_NOTICE",
                "KEYWORD_FILTERED",
                true,
                "공식 고시공고 게시판 확인"
        );
    }

    /**
     * 출처 조회 행을 생성합니다.
     *
     * @param parserProfileCode 시스템 파서 코드
     * @return 출처 조회 행
     */
    private LocalGovernmentNoticeSourceRow sourceRow(String parserProfileCode) {
        OffsetDateTime now = OffsetDateTime.now();
        return new LocalGovernmentNoticeSourceRow(
                SOURCE_ID,
                "LGS-TEST",
                "11",
                "서울특별시",
                "11680",
                "강남구",
                "BASIC_LOCAL_GOVERNMENT",
                "강남구청",
                "https://www.gangnam.go.kr",
                "https://www.gangnam.go.kr/notice/list.do?mid=ID05_040201",
                null,
                "public_notice_board",
                "DEFAULT",
                "GET",
                null,
                parserProfileCode,
                null,
                null,
                "HIGH",
                "VERIFIED",
                "LEGAL_NOTICE",
                "KEYWORD_FILTERED",
                true,
                now,
                ADMIN_ID,
                "공식 고시공고 게시판 확인",
                false,
                "READY",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                now,
                now,
                null
        );
    }
}
