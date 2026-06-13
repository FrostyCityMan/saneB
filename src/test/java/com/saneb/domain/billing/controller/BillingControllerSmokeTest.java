package com.saneb.domain.billing.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.saneb.common.response.PageResponse;
import com.saneb.domain.auth.vo.AuthUserDetailsRow;
import com.saneb.domain.auth.vo.AuthenticatedUserDetails;
import com.saneb.domain.billing.dto.MockMonthlyPaymentResponse;
import com.saneb.domain.billing.dto.PaymentProviderEventResponse;
import com.saneb.domain.billing.dto.PaymentTransactionResponse;
import com.saneb.domain.billing.dto.RefundTransactionResponse;
import com.saneb.domain.billing.dto.SubscriptionPlanResponse;
import com.saneb.domain.billing.dto.UserSubscriptionResponse;
import com.saneb.domain.billing.service.BillingService;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "spring.flyway.enabled=false")
@AutoConfigureMockMvc
class BillingControllerSmokeTest {

    private static final UUID USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000002");
    private static final UUID PLAN_ID = UUID.fromString("90000000-0000-0000-0000-000000000001");
    private static final UUID SUBSCRIPTION_ID = UUID.fromString("90000000-0000-0000-0000-000000000002");
    private static final UUID PAYMENT_ID = UUID.fromString("90000000-0000-0000-0000-000000000003");
    private static final UUID REFUND_ID = UUID.fromString("90000000-0000-0000-0000-000000000004");
    private static final UUID EVENT_ID = UUID.fromString("90000000-0000-0000-0000-000000000005");
    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-06-08T10:00:00+09:00");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BillingService billingService;

    @BeforeEach
    void setUp() {
        when(billingService.selectSubscriptionPlanList(any(), any(), eq(1), eq(20)))
                .thenReturn(PageResponse.of(List.of(plan()), 1, 20, 1));
        when(billingService.insertSubscriptionPlan(any(), any())).thenReturn(plan());
        when(billingService.updateSubscriptionPlanStatus(any(), eq(PLAN_ID), any())).thenReturn(plan());
        when(billingService.selectUserSubscriptionList(any(), any(), any(), eq(1), eq(20)))
                .thenReturn(PageResponse.of(List.of(subscription("PENDING")), 1, 20, 1));
        when(billingService.insertUserSubscription(any(), any())).thenReturn(subscription("PENDING"));
        when(billingService.updateUserSubscriptionCancel(any(), eq(SUBSCRIPTION_ID), any()))
                .thenReturn(subscription("CANCELED"));
        when(billingService.selectPaymentTransactionList(any(), any(), any(), any(), eq(1), eq(20)))
                .thenReturn(PageResponse.of(List.of(payment("REQUESTED")), 1, 20, 1));
        when(billingService.insertPaymentTransaction(any(), any())).thenReturn(payment("REQUESTED"));
        when(billingService.insertMockMonthlyPayment(any(), any()))
                .thenReturn(new MockMonthlyPaymentResponse(
                        subscription("ACTIVE"),
                        payment("APPROVED"),
                        "모의 결제가 완료되어 월 구독이 활성화되었습니다."
                ));
        when(billingService.updatePaymentTransactionStatus(any(), eq(PAYMENT_ID), any())).thenReturn(payment("APPROVED"));
        when(billingService.selectRefundTransactionList(any(), any(), any(), any(), eq(1), eq(20)))
                .thenReturn(PageResponse.of(List.of(refund("REQUESTED")), 1, 20, 1));
        when(billingService.insertRefundTransaction(any(), any())).thenReturn(refund("REQUESTED"));
        when(billingService.updateRefundTransactionStatus(any(), eq(REFUND_ID), any())).thenReturn(refund("APPROVED"));
        when(billingService.insertPaymentProviderEvent(eq("MANUAL"), eq("secret"), any()))
                .thenReturn(providerEvent());
    }

    @Test
    void selectSubscriptionPlanListReturnsPagedApiResponse() throws Exception {
        mockMvc.perform(get("/api/v1/subscription-plans")
                        .with(user(userPrincipal()))
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].planCode").value("BASIC"));
    }

    @Test
    void insertSubscriptionPlanRejectsUserRole() throws Exception {
        mockMvc.perform(post("/api/v1/subscription-plans")
                        .with(user(userPrincipal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(planRequest()))
                .andExpect(status().isForbidden());
    }

    @Test
    void insertSubscriptionPlanReturnsApiResponseForOperator() throws Exception {
        mockMvc.perform(post("/api/v1/subscription-plans")
                        .with(user(operatorPrincipal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(planRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.planId").value(PLAN_ID.toString()));
    }

    @Test
    void insertSubscriptionReturnsApiResponse() throws Exception {
        mockMvc.perform(post("/api/v1/subscriptions")
                        .with(user(userPrincipal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "planId": "%s"
                                }
                                """.formatted(PLAN_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.subscriptionId").value(SUBSCRIPTION_ID.toString()))
                .andExpect(jsonPath("$.data.statusCode").value("PENDING"));
    }

    @Test
    void insertPaymentReturnsApiResponse() throws Exception {
        mockMvc.perform(post("/api/v1/payments")
                        .with(user(userPrincipal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "subscriptionId": "%s",
                                  "providerCode": "MANUAL",
                                  "amount": 99000,
                                  "currencyCode": "KRW"
                                }
                                """.formatted(SUBSCRIPTION_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paymentId").value(PAYMENT_ID.toString()))
                .andExpect(jsonPath("$.data.statusCode").value("REQUESTED"));
    }

    @Test
    void insertMockMonthlyPaymentReturnsActiveSubscription() throws Exception {
        mockMvc.perform(post("/api/v1/mock-payments/monthly-subscription")
                        .with(user(userPrincipal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "planId": "%s",
                                  "simulateFailure": false
                                }
                                """.formatted(PLAN_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.subscription.statusCode").value("ACTIVE"))
                .andExpect(jsonPath("$.data.payment.statusCode").value("APPROVED"))
                .andExpect(jsonPath("$.data.resultMessage").value("모의 결제가 완료되어 월 구독이 활성화되었습니다."));
    }

    @Test
    void insertMockMonthlyPaymentRejectsOperatorRole() throws Exception {
        mockMvc.perform(post("/api/v1/mock-payments/monthly-subscription")
                        .with(user(operatorPrincipal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "planId": "%s",
                                  "simulateFailure": false
                                }
                                """.formatted(PLAN_ID)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updatePaymentStatusRejectsUserRole() throws Exception {
        mockMvc.perform(patch("/api/v1/payments/{paymentId}/status", PAYMENT_ID)
                        .with(user(userPrincipal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "statusCode": "APPROVED"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void insertRefundReturnsApiResponse() throws Exception {
        mockMvc.perform(post("/api/v1/refunds")
                        .with(user(userPrincipal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "paymentId": "%s",
                                  "refundAmount": 99000,
                                  "reason": "취소 요청"
                                }
                                """.formatted(PAYMENT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.refundId").value(REFUND_ID.toString()));
    }

    @Test
    void insertPaymentProviderEventIsAvailableWithoutLoginButUsesSecretHeader() throws Exception {
        mockMvc.perform(post("/api/v1/payment-webhooks/MANUAL")
                        .header("X-SANEB-WEBHOOK-SECRET", "secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventId": "event-001",
                                  "paymentId": "%s",
                                  "eventTypeCode": "PAYMENT_APPROVED",
                                  "amount": 99000,
                                  "currencyCode": "KRW"
                                }
                                """.formatted(PAYMENT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.providerEventId").value("event-001"));
    }

    private String planRequest() {
        return """
                {
                  "planCode": "BASIC",
                  "planName": "기본 요금제",
                  "billingCycleCode": "MONTHLY",
                  "priceAmount": 99000,
                  "currencyCode": "KRW",
                  "active": true,
                  "sortOrder": 10
                }
                """;
    }

    private SubscriptionPlanResponse plan() {
        return new SubscriptionPlanResponse(
                PLAN_ID,
                "BASIC",
                "기본 요금제",
                "MONTHLY",
                new BigDecimal("99000.00"),
                "KRW",
                true,
                10,
                null,
                CREATED_AT,
                CREATED_AT
        );
    }

    private UserSubscriptionResponse subscription(String statusCode) {
        return new UserSubscriptionResponse(
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
                "CANCELED".equals(statusCode) ? CREATED_AT : null,
                "CANCELED".equals(statusCode) ? "취소" : null,
                CREATED_AT,
                CREATED_AT
        );
    }

    private PaymentTransactionResponse payment(String statusCode) {
        return new PaymentTransactionResponse(
                PAYMENT_ID,
                SUBSCRIPTION_ID,
                USER_ID,
                PLAN_ID,
                "MANUAL",
                "SANEB-TEST",
                "APPROVED".equals(statusCode) ? "provider-key" : null,
                statusCode,
                new BigDecimal("99000.00"),
                "KRW",
                CREATED_AT,
                "APPROVED".equals(statusCode) ? CREATED_AT : null,
                null,
                null,
                null,
                CREATED_AT,
                CREATED_AT
        );
    }

    private RefundTransactionResponse refund(String statusCode) {
        return new RefundTransactionResponse(
                REFUND_ID,
                PAYMENT_ID,
                USER_ID,
                "MANUAL",
                "APPROVED".equals(statusCode) ? "refund-key" : null,
                statusCode,
                new BigDecimal("99000.00"),
                "취소 요청",
                USER_ID,
                CREATED_AT,
                "APPROVED".equals(statusCode) ? CREATED_AT : null,
                null,
                null,
                CREATED_AT,
                CREATED_AT
        );
    }

    private PaymentProviderEventResponse providerEvent() {
        return new PaymentProviderEventResponse(
                EVENT_ID,
                "MANUAL",
                "event-001",
                "PAYMENT_APPROVED",
                PAYMENT_ID,
                null,
                "RECEIVED",
                CREATED_AT
        );
    }

    private AuthenticatedUserDetails userPrincipal() {
        return principal(USER_ID, "local_user", List.of("USER"));
    }

    private AuthenticatedUserDetails operatorPrincipal() {
        return principal(USER_ID, "local_operator", List.of("OPERATOR"));
    }

    private AuthenticatedUserDetails principal(UUID userId, String loginId, List<String> roles) {
        return new AuthenticatedUserDetails(
                new AuthUserDetailsRow(
                        userId,
                        loginId,
                        "password-hash",
                        "Local User",
                        "ACTIVE",
                        false,
                        null,
                        null,
                        null
                ),
                roles
        );
    }
}
