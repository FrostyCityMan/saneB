package com.saneb.domain.aiassist.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.saneb.domain.aiassist.dao.AiAssistDao;
import com.saneb.domain.aiassist.dto.AiAssistCreateRequest;
import com.saneb.domain.aiassist.dto.AiAssistReviewRequest;
import com.saneb.domain.aiassist.provider.AiAssistProvider;
import com.saneb.domain.aiassist.provider.AiAssistProviderRequest;
import com.saneb.domain.aiassist.provider.AiAssistProviderResponse;
import com.saneb.domain.aiassist.vo.AiAssistInsertCommand;
import com.saneb.domain.aiassist.vo.AiAssistResultInsertCommand;
import com.saneb.domain.aiassist.vo.AiAssistRow;
import com.saneb.domain.auth.vo.AuthUserDetailsRow;
import com.saneb.domain.auth.vo.AuthenticatedUserDetails;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.TestingAuthenticationToken;

class AiAssistServiceImplTest {

    private static final UUID OPERATOR_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID RESULT_ID = UUID.fromString("93000000-0000-0000-0000-000000000002");
    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-06-08T10:00:00+09:00");

    private AiAssistDao aiAssistDao;
    private AiAssistProvider aiAssistProvider;
    private AiAssistServiceImpl aiAssistService;

    @BeforeEach
    void setUp() {
        aiAssistDao = org.mockito.Mockito.mock(AiAssistDao.class);
        aiAssistProvider = org.mockito.Mockito.mock(AiAssistProvider.class);
        aiAssistService = new AiAssistServiceImpl(aiAssistDao, aiAssistProvider);
        when(aiAssistProvider.generate(any())).thenReturn(new AiAssistProviderResponse(
                "LOCAL_SAFE",
                "RULE_TEMPLATE_V1",
                "공고 요약 초안",
                10,
                20,
                "{\"providerMode\":\"localSafe\"}"
        ));
        when(aiAssistDao.selectAiAssistDetails(any())).thenAnswer(invocation ->
                row(invocation.getArgument(0), "PENDING_REVIEW")
        );
        when(aiAssistDao.selectAiAssistDetailsByResultId(RESULT_ID)).thenReturn(row(
                UUID.fromString("93000000-0000-0000-0000-000000000001"),
                "PENDING_REVIEW"
        ));
    }

    @Test
    void insertAiAssistRequestStoresHashAndLengthWithoutRawInput() {
        String rawInput = "홍길동 010-1111-2222 개인정보 포함 가능 원문";

        aiAssistService.insertAiAssistRequest(authentication(), new AiAssistCreateRequest(
                "ANNOUNCEMENT_SUMMARY",
                "ANNOUNCEMENT",
                null,
                rawInput,
                "운영자 메모"
        ));

        ArgumentCaptor<AiAssistInsertCommand> requestCaptor = ArgumentCaptor.forClass(AiAssistInsertCommand.class);
        ArgumentCaptor<AiAssistProviderRequest> providerCaptor = ArgumentCaptor.forClass(AiAssistProviderRequest.class);
        ArgumentCaptor<AiAssistResultInsertCommand> resultCaptor = ArgumentCaptor.forClass(AiAssistResultInsertCommand.class);
        verify(aiAssistDao).insertAiAssistRequest(requestCaptor.capture());
        verify(aiAssistProvider).generate(providerCaptor.capture());
        verify(aiAssistDao).insertAiAssistResult(resultCaptor.capture());

        assertThat(requestCaptor.getValue().inputHashSha256()).hasSize(64);
        assertThat(requestCaptor.getValue().inputLength()).isEqualTo(rawInput.length());
        assertThat(requestCaptor.getValue().inputHashSha256()).doesNotContain("홍길동");
        assertThat(providerCaptor.getValue().inputLength()).isEqualTo(rawInput.length());
        assertThat(resultCaptor.getValue().resultText()).isEqualTo("공고 요약 초안");
    }

    @Test
    void updateAiAssistResultReviewUpdatesReviewStatus() {
        aiAssistService.updateAiAssistResultReview(authentication(), RESULT_ID, new AiAssistReviewRequest("ACCEPTED"));

        verify(aiAssistDao).updateAiAssistResultReviewStatus(RESULT_ID, "ACCEPTED", OPERATOR_ID);
    }

    private TestingAuthenticationToken authentication() {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(principal(), null);
        authentication.setAuthenticated(true);
        return authentication;
    }

    private AuthenticatedUserDetails principal() {
        return new AuthenticatedUserDetails(
                new AuthUserDetailsRow(
                        OPERATOR_ID,
                        "operator",
                        "password-hash",
                        "Operator User",
                        "ACTIVE",
                        false,
                        null,
                        null,
                        null
                ),
                List.of("OPERATOR")
        );
    }

    private AiAssistRow row(UUID requestId, String reviewStatusCode) {
        return new AiAssistRow(
                requestId,
                RESULT_ID,
                "ANNOUNCEMENT_SUMMARY",
                "ANNOUNCEMENT",
                null,
                "COMPLETED",
                "LOCAL_SAFE",
                "RULE_TEMPLATE_V1",
                reviewStatusCode,
                "공고 요약 초안",
                OPERATOR_ID,
                CREATED_AT,
                CREATED_AT
        );
    }
}
