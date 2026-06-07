package com.saneb.domain.applicationprogress.service.impl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.saneb.common.error.ApiException;
import com.saneb.common.error.ErrorCode;
import com.saneb.domain.applicationprogress.dao.ApplicationProgressDao;
import com.saneb.domain.applicationprogress.dto.ApplicationProgressStartRequest;
import com.saneb.domain.applicationprogress.vo.MatchingCaseProgressRow;
import com.saneb.domain.auth.vo.AuthUserDetailsRow;
import com.saneb.domain.auth.vo.AuthenticatedUserDetails;
import com.saneb.domain.dynamicinput.dao.DynamicAnnouncementInputDao;
import java.util.List;
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

    private ApplicationProgressDao applicationProgressDao;
    private ApplicationProgressServiceImpl applicationProgressService;

    @BeforeEach
    void setUp() {
        applicationProgressDao = mock(ApplicationProgressDao.class);
        applicationProgressService = new ApplicationProgressServiceImpl(
                applicationProgressDao,
                mock(DynamicAnnouncementInputDao.class)
        );
    }

    @Test
    void insertApplicationProgressRejectsOtherMembersMatchingCaseForUser() {
        when(applicationProgressDao.selectApplicationProgressByMatchingCaseId(MATCHING_CASE_ID)).thenReturn(null);
        when(applicationProgressDao.selectMatchingCaseForProgress(MATCHING_CASE_ID)).thenReturn(
                new MatchingCaseProgressRow(
                        MATCHING_CASE_ID,
                        ANNOUNCEMENT_ID,
                        OTHER_USER_ID,
                        "MATCHED"
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
