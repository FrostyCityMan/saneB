package com.saneb.domain.consent.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.saneb.common.error.ApiException;
import com.saneb.common.error.ErrorCode;
import com.saneb.domain.auth.vo.AuthUserDetailsRow;
import com.saneb.domain.auth.vo.AuthenticatedUserDetails;
import com.saneb.domain.consent.dao.ConsentDao;
import com.saneb.domain.consent.dto.ConsentSaveRequest;
import com.saneb.domain.consent.dto.UserConsentResponse;
import com.saneb.domain.consent.vo.ConsentVersionRow;
import com.saneb.domain.consent.vo.UserConsentInsertCommand;
import com.saneb.domain.consent.vo.UserConsentRow;
import jakarta.servlet.http.HttpServletRequest;
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

@ExtendWith(MockitoExtension.class)
class ConsentServiceImplTest {

    private static final UUID USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID CONSENT_VERSION_ID = UUID.fromString("70000000-0000-0000-0000-000000000001");
    private static final UUID USER_CONSENT_ID = UUID.fromString("71000000-0000-0000-0000-000000000001");
    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-06-08T10:00:00+09:00");

    @Mock
    private ConsentDao consentDao;

    @Mock
    private HttpServletRequest httpRequest;

    private ConsentServiceImpl consentService;

    @BeforeEach
    void setUp() {
        consentService = new ConsentServiceImpl(consentDao);
    }

    @Test
    void selectCurrentConsentListMapsCurrentVersions() {
        when(consentDao.selectCurrentConsentVersionList()).thenReturn(List.of(version("PRIVACY_POLICY")));

        var response = consentService.selectCurrentConsentList();

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().consentName()).isEqualTo("개인정보 처리방침");
    }

    @Test
    void insertMyConsentSavesCurrentVersionAndRequestMetadata() {
        when(consentDao.selectCurrentConsentVersionDetailsByCode("E_CERT")).thenReturn(version("E_CERT"));
        when(consentDao.insertUserConsent(any())).thenReturn(USER_CONSENT_ID);
        when(consentDao.selectUserConsentDetails(USER_CONSENT_ID)).thenReturn(userConsent("E_CERT"));
        when(httpRequest.getHeader("X-Forwarded-For")).thenReturn(null);
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(httpRequest.getHeader("User-Agent")).thenReturn("JUnit");

        UserConsentResponse response = consentService.insertMyConsent(
                authentication(),
                new ConsentSaveRequest("e_cert", true),
                httpRequest
        );

        assertThat(response.consentCode()).isEqualTo("E_CERT");
        ArgumentCaptor<UserConsentInsertCommand> captor = ArgumentCaptor.forClass(UserConsentInsertCommand.class);
        verify(consentDao).insertUserConsent(captor.capture());
        assertThat(captor.getValue().userId()).isEqualTo(USER_ID);
        assertThat(captor.getValue().consentCode()).isEqualTo("E_CERT");
        assertThat(captor.getValue().ipAddress()).isEqualTo("127.0.0.1");
    }

    @Test
    void insertSignupRequiredConsentsStoresTermsAndPrivacy() {
        when(consentDao.selectCurrentConsentVersionDetailsByCode("TERMS_OF_SERVICE")).thenReturn(
                version("TERMS_OF_SERVICE")
        );
        when(consentDao.selectCurrentConsentVersionDetailsByCode("PRIVACY_POLICY")).thenReturn(
                version("PRIVACY_POLICY")
        );
        when(httpRequest.getHeader("X-Forwarded-For")).thenReturn(null);
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");

        consentService.insertSignupRequiredConsents(USER_ID, httpRequest);

        ArgumentCaptor<UserConsentInsertCommand> captor = ArgumentCaptor.forClass(UserConsentInsertCommand.class);
        verify(consentDao, org.mockito.Mockito.times(2)).insertUserConsent(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(UserConsentInsertCommand::consentCode)
                .containsExactly("TERMS_OF_SERVICE", "PRIVACY_POLICY");
    }

    @Test
    void insertMyConsentRejectsInvalidConsentCode() {
        assertThatThrownBy(() -> consentService.insertMyConsent(
                authentication(),
                new ConsentSaveRequest("BAD", true),
                httpRequest
        ))
                .isInstanceOf(ApiException.class)
                .extracting(error -> ((ApiException) error).errorCode())
                .isEqualTo(ErrorCode.VALIDATION_FAILED);
    }

    private ConsentVersionRow version(String consentCode) {
        return new ConsentVersionRow(
                CONSENT_VERSION_ID,
                consentCode,
                switch (consentCode) {
                    case "TERMS_OF_SERVICE" -> "이용약관";
                    case "E_CERT" -> "전자증명 이용 동의";
                    default -> "개인정보 처리방침";
                },
                1,
                true,
                CREATED_AT
        );
    }

    private UserConsentRow userConsent(String consentCode) {
        return new UserConsentRow(
                USER_CONSENT_ID,
                CONSENT_VERSION_ID,
                consentCode,
                "전자증명 이용 동의",
                1,
                true,
                CREATED_AT
        );
    }

    private UsernamePasswordAuthenticationToken authentication() {
        AuthUserDetailsRow row = new AuthUserDetailsRow(
                USER_ID,
                "user01",
                "hash",
                "사용자",
                "ACTIVE",
                false,
                null,
                null,
                null
        );
        AuthenticatedUserDetails principal = new AuthenticatedUserDetails(row, List.of("USER"));
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }
}
