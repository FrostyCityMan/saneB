/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: BillingServiceImplTest.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.billing.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.saneb.common.error.ApiException;
import com.saneb.common.error.ErrorCode;
import com.saneb.domain.auth.vo.AuthUserDetailsRow;
import com.saneb.domain.auth.vo.AuthenticatedUserDetails;
import com.saneb.domain.billing.dao.BillingDao;
import com.saneb.domain.billing.dto.PaymentProviderEventRequest;
import com.saneb.domain.billing.dto.PaymentStatusUpdateRequest;
import com.saneb.domain.billing.dto.RefundCreateRequest;
import com.saneb.domain.billing.dto.UserSubscriptionCreateRequest;
import com.saneb.domain.billing.vo.PaymentTransactionRow;
import com.saneb.domain.billing.vo.PaymentTransactionStatusCommand;
import com.saneb.domain.billing.vo.SubscriptionPlanRow;
import com.saneb.domain.billing.vo.UserSubscriptionInsertCommand;
import com.saneb.domain.billing.vo.UserSubscriptionRow;
import com.saneb.domain.billing.vo.UserSubscriptionStatusCommand;
import java.math.BigDecimal;
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
class BillingServiceImplTest {

    private static final UUID USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000002");
    private static final UUID PLAN_ID = UUID.fromString("90000000-0000-0000-0000-000000000001");
    private static final UUID SUBSCRIPTION_ID = UUID.fromString("90000000-0000-0000-0000-000000000002");
    private static final UUID PAYMENT_ID = UUID.fromString("90000000-0000-0000-0000-000000000003");
    private static final UUID REFUND_ID = UUID.fromString("90000000-0000-0000-0000-000000000004");
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-06-08T10:00:00+09:00");

    @Mock
    private BillingDao billingDao;

    private BillingServiceImpl billingService;

    /**
     * 업무 처리를 수행합니다.
     */
    @BeforeEach
    void setUp() {
        billingService = new BillingServiceImpl(billingDao, "secret");
    }

    /**
     * 업무 데이터를 등록합니다.
     */
    @Test
    void insertUserSubscriptionActivatesFreePlanWithoutPayment() {
        when(billingDao.selectSubscriptionPlanDetails(PLAN_ID)).thenReturn(plan(BigDecimal.ZERO));
        when(billingDao.selectUserSubscriptionDetails(any())).thenReturn(subscription("ACTIVE"));

        var response = billingService.insertUserSubscription(
                authentication(USER_ID, List.of("USER")),
                new UserSubscriptionCreateRequest(null, PLAN_ID)
        );

        ArgumentCaptor<UserSubscriptionInsertCommand> captor =
                ArgumentCaptor.forClass(UserSubscriptionInsertCommand.class);
        verify(billingDao).insertUserSubscription(captor.capture());
        assertThat(captor.getValue().userId()).isEqualTo(USER_ID);
        assertThat(captor.getValue().statusCode()).isEqualTo("ACTIVE");
        verify(billingDao).insertAuditLog(any());
        assertThat(response.statusCode()).isEqualTo("ACTIVE");
    }

    /**
     * 업무 데이터를 수정합니다.
     */
    @Test
    void updatePaymentTransactionStatusApprovesPaymentAndActivatesSubscription() {
        when(billingDao.selectPaymentTransactionDetails(PAYMENT_ID))
                .thenReturn(payment("REQUESTED"), payment("APPROVED"));
        when(billingDao.selectUserSubscriptionDetails(SUBSCRIPTION_ID)).thenReturn(subscription("PENDING"));

        var response = billingService.updatePaymentTransactionStatus(
                authentication(USER_ID, List.of("OPERATOR")),
                PAYMENT_ID,
                new PaymentStatusUpdateRequest("approved", "provider-key", null, null)
        );

        ArgumentCaptor<PaymentTransactionStatusCommand> paymentCaptor =
                ArgumentCaptor.forClass(PaymentTransactionStatusCommand.class);
        verify(billingDao).updatePaymentTransactionStatus(paymentCaptor.capture());
        assertThat(paymentCaptor.getValue().statusCode()).isEqualTo("APPROVED");
        assertThat(paymentCaptor.getValue().providerPaymentKey()).isEqualTo("provider-key");

        ArgumentCaptor<UserSubscriptionStatusCommand> subscriptionCaptor =
                ArgumentCaptor.forClass(UserSubscriptionStatusCommand.class);
        verify(billingDao).updateUserSubscriptionStatus(subscriptionCaptor.capture());
        assertThat(subscriptionCaptor.getValue().statusCode()).isEqualTo("ACTIVE");
        assertThat(response.statusCode()).isEqualTo("APPROVED");
    }

    /**
     * 업무 데이터를 등록합니다.
     */
    @Test
    void insertRefundRejectsAmountGreaterThanRemainingApprovedPayment() {
        when(billingDao.selectPaymentTransactionDetails(PAYMENT_ID)).thenReturn(payment("APPROVED"));
        when(billingDao.selectApprovedRefundAmount(PAYMENT_ID)).thenReturn(new BigDecimal("50000.00"));

        assertThatThrownBy(() -> billingService.insertRefundTransaction(
                authentication(USER_ID, List.of("USER")),
                new RefundCreateRequest(PAYMENT_ID, new BigDecimal("60000.00"), "취소 요청")
        ))
                .isInstanceOf(ApiException.class)
                .extracting(error -> ((ApiException) error).errorCode())
                .isEqualTo(ErrorCode.VALIDATION_FAILED);
    }

    /**
     * 업무 데이터를 등록합니다.
     */
    @Test
    void insertPaymentProviderEventRejectsMissingSecret() {
        assertThatThrownBy(() -> billingService.insertPaymentProviderEvent(
                "MANUAL",
                null,
                new PaymentProviderEventRequest(
                        "event-001",
                        PAYMENT_ID,
                        null,
                        null,
                        "provider-key",
                        null,
                        "PAYMENT_APPROVED",
                        new BigDecimal("99000.00"),
                        "KRW",
                        null,
                        null
                )
        ))
                .isInstanceOf(ApiException.class)
                .extracting(error -> ((ApiException) error).errorCode())
                .isEqualTo(ErrorCode.AUTH_FORBIDDEN);
    }

    /**
     * 업무 데이터를 등록합니다.
     */
    @Test
    void insertPaymentProviderEventApprovesPaymentWithValidSecret() {
        when(billingDao.selectPaymentProviderEventByKey("MANUAL", "event-001"))
                .thenReturn(null)
                .thenReturn(providerEvent());
        when(billingDao.selectPaymentTransactionDetails(PAYMENT_ID)).thenReturn(payment("REQUESTED"));
        when(billingDao.selectUserSubscriptionDetails(SUBSCRIPTION_ID)).thenReturn(subscription("PENDING"));

        var response = billingService.insertPaymentProviderEvent(
                "manual",
                "secret",
                new PaymentProviderEventRequest(
                        "event-001",
                        PAYMENT_ID,
                        null,
                        null,
                        "provider-key",
                        null,
                        "payment_approved",
                        new BigDecimal("99000.00"),
                        "KRW",
                        null,
                        null
                )
        );

        verify(billingDao).insertPaymentProviderEvent(any());
        verify(billingDao).updatePaymentTransactionStatus(any());
        assertThat(response.resultCode()).isEqualTo("RECEIVED");
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param priceAmount 입력 값
     *
     * @return 처리 결과
     */
    private SubscriptionPlanRow plan(BigDecimal priceAmount) {
        return new SubscriptionPlanRow(
                PLAN_ID,
                "BASIC",
                "기본 요금제",
                "MONTHLY",
                priceAmount,
                "KRW",
                true,
                10,
                null,
                NOW,
                NOW
        );
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param statusCode 입력 값
     *
     * @return 처리 결과
     */
    private UserSubscriptionRow subscription(String statusCode) {
        return new UserSubscriptionRow(
                SUBSCRIPTION_ID,
                USER_ID,
                PLAN_ID,
                "BASIC",
                "기본 요금제",
                "MONTHLY",
                new BigDecimal("99000.00"),
                "KRW",
                statusCode,
                null,
                null,
                null,
                null,
                NOW,
                NOW
        );
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param statusCode 입력 값
     *
     * @return 처리 결과
     */
    private PaymentTransactionRow payment(String statusCode) {
        return new PaymentTransactionRow(
                PAYMENT_ID,
                SUBSCRIPTION_ID,
                USER_ID,
                PLAN_ID,
                "MANUAL",
                "SANEB-TEST",
                null,
                statusCode,
                new BigDecimal("99000.00"),
                "KRW",
                NOW,
                "APPROVED".equals(statusCode) ? NOW : null,
                null,
                null,
                null,
                NOW,
                NOW
        );
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @return 처리 결과
     */
    private com.saneb.domain.billing.vo.PaymentProviderEventRow providerEvent() {
        return new com.saneb.domain.billing.vo.PaymentProviderEventRow(
                REFUND_ID,
                "MANUAL",
                "event-001",
                "PAYMENT_APPROVED",
                PAYMENT_ID,
                null,
                "RECEIVED",
                NOW
        );
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param userId 입력 값
     *
     * @param roles 입력 값
     *
     * @return 처리 결과
     */
    private UsernamePasswordAuthenticationToken authentication(UUID userId, List<String> roles) {
        AuthenticatedUserDetails principal = new AuthenticatedUserDetails(
                new AuthUserDetailsRow(
                        userId,
                        "user01",
                        "hash",
                        "Local User",
                        "ACTIVE",
                        false,
                        null,
                        null,
                        null
                ),
                roles
        );
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }
}
