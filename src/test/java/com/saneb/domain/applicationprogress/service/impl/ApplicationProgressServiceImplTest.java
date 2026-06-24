/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: ApplicationProgressServiceImplTest.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.applicationprogress.service.impl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.saneb.common.error.ApiException;
import com.saneb.common.error.ErrorCode;
import com.saneb.domain.applicationprogress.dao.ApplicationProgressDao;
import com.saneb.domain.applicationprogress.dto.ApplicationProgressStartRequest;
import com.saneb.domain.applicationprogress.dto.ProgressActionRequest;
import com.saneb.domain.applicationprogress.vo.ApplicationProgressRow;
import com.saneb.domain.applicationprogress.vo.ApplicationStepStateRow;
import com.saneb.domain.applicationprogress.vo.MatchingCaseProgressRow;
import com.saneb.domain.applicationprogress.vo.StepButtonRow;
import com.saneb.domain.auth.vo.AuthUserDetailsRow;
import com.saneb.domain.auth.vo.AuthenticatedUserDetails;
import com.saneb.domain.dynamicinput.dao.DynamicAnnouncementInputDao;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

class ApplicationProgressServiceImplTest {

    private static final UUID USER_ID = UUID.fromString("62000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_USER_ID = UUID.fromString("62000000-0000-0000-0000-000000000002");
    private static final UUID MATCHING_CASE_ID = UUID.fromString("62000000-0000-0000-0000-000000000003");
    private static final UUID ANNOUNCEMENT_ID = UUID.fromString("62000000-0000-0000-0000-000000000004");
    private static final UUID PROGRESS_ID = UUID.fromString("62000000-0000-0000-0000-000000000005");
    private static final UUID STEP_ID = UUID.fromString("62000000-0000-0000-0000-000000000006");
    private static final UUID STEP_STATE_ID = UUID.fromString("62000000-0000-0000-0000-000000000007");

    private ApplicationProgressDao applicationProgressDao;
    private DynamicAnnouncementInputDao dynamicAnnouncementInputDao;
    private ApplicationProgressServiceImpl applicationProgressService;

    /**
     * 업무 처리를 수행합니다.
     */
    @BeforeEach
    void setUp() {
        applicationProgressDao = mock(ApplicationProgressDao.class);
        dynamicAnnouncementInputDao = mock(DynamicAnnouncementInputDao.class);
        applicationProgressService = new ApplicationProgressServiceImpl(
                applicationProgressDao,
                dynamicAnnouncementInputDao
        );
    }

    /**
     * 업무 데이터를 등록합니다.
     */
    @Test
    void insertApplicationProgressRejectsOtherMembersMatchingCaseForUser() {
        when(applicationProgressDao.selectApplicationProgressByMatchingCaseId(MATCHING_CASE_ID)).thenReturn(null);
        when(applicationProgressDao.selectMatchingCaseForProgress(MATCHING_CASE_ID)).thenReturn(
                new MatchingCaseProgressRow(
                        MATCHING_CASE_ID,
                        ANNOUNCEMENT_ID,
                        OTHER_USER_ID,
                        "MATCHED",
                        "FINAL"
                )
        );

        assertThatThrownBy(() -> applicationProgressService.insertApplicationProgress(
                authentication(),
                new ApplicationProgressStartRequest(MATCHING_CASE_ID)
        ))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.AUTH_FORBIDDEN)
                );
    }

    /**
     * 업무 데이터를 수정합니다.
     */
    @Test
    void updateProgressStepActionRejectsReceiptConditionWhenReceiptIsMissing() {
        when(applicationProgressDao.selectApplicationProgressDetails(PROGRESS_ID)).thenReturn(progressRow());
        when(applicationProgressDao.selectApplicationStepState(PROGRESS_ID, STEP_ID)).thenReturn(stepState("RECEIPT_SAVED"));
        when(applicationProgressDao.selectStepButton(STEP_ID, "RECEIPT_DONE")).thenReturn(
                new StepButtonRow(STEP_ID, "RECEIPT_DONE", "접수 완료", "MOVE_NEXT", null, 1)
        );
        when(applicationProgressDao.selectRequiredUncheckedDocumentCount(PROGRESS_ID, STEP_ID)).thenReturn(0L);
        when(dynamicAnnouncementInputDao.selectMissingRequiredApplicationInputCount(PROGRESS_ID)).thenReturn(0L);

        assertThatThrownBy(() -> applicationProgressService.updateProgressStepAction(
                authentication(),
                PROGRESS_ID,
                STEP_ID,
                new ProgressActionRequest("RECEIPT_DONE", Map.of())
        ))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.PROGRESS_CONDITION_NOT_MET)
                );
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @return 처리 결과
     */
    private ApplicationProgressRow progressRow() {
        OffsetDateTime now = OffsetDateTime.now();
        return new ApplicationProgressRow(
                PROGRESS_ID,
                MATCHING_CASE_ID,
                ANNOUNCEMENT_ID,
                USER_ID,
                "APP-000001",
                "MCH-000001",
                "ANN-000001",
                "USR-000001",
                STEP_ID,
                "IN_PROGRESS",
                null,
                null,
                null,
                null,
                null,
                null,
                now,
                now
        );
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param completionConditionCode 입력 값
     *
     * @return 처리 결과
     */
    private ApplicationStepStateRow stepState(String completionConditionCode) {
        OffsetDateTime now = OffsetDateTime.now();
        return new ApplicationStepStateRow(
                STEP_STATE_ID,
                PROGRESS_ID,
                STEP_ID,
                4,
                "접수 진행",
                "접수 정보를 저장합니다.",
                "접수 정보를 저장한 뒤 접수 완료를 선택하세요.",
                completionConditionCode,
                "READY",
                now,
                null
        );
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @return 처리 결과
     */
    private Authentication authentication() {
        AuthenticatedUserDetails principal = new AuthenticatedUserDetails(
                new AuthUserDetailsRow(
                        USER_ID,
                        "local_user",
                        "password-hash",
                        "Local User",
                        "ACTIVE",
                        false,
                        null,
                        null,
                        null
                ),
                List.of("USER")
        );
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }
}
