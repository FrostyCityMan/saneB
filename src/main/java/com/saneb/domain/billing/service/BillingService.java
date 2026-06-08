package com.saneb.domain.billing.service;

import com.saneb.common.response.PageResponse;
import com.saneb.domain.billing.dto.PaymentCreateRequest;
import com.saneb.domain.billing.dto.PaymentProviderEventRequest;
import com.saneb.domain.billing.dto.PaymentProviderEventResponse;
import com.saneb.domain.billing.dto.PaymentStatusUpdateRequest;
import com.saneb.domain.billing.dto.PaymentTransactionResponse;
import com.saneb.domain.billing.dto.RefundCreateRequest;
import com.saneb.domain.billing.dto.RefundStatusUpdateRequest;
import com.saneb.domain.billing.dto.RefundTransactionResponse;
import com.saneb.domain.billing.dto.SubscriptionPlanCreateRequest;
import com.saneb.domain.billing.dto.SubscriptionPlanResponse;
import com.saneb.domain.billing.dto.SubscriptionPlanStatusUpdateRequest;
import com.saneb.domain.billing.dto.UserSubscriptionCancelRequest;
import com.saneb.domain.billing.dto.UserSubscriptionCreateRequest;
import com.saneb.domain.billing.dto.UserSubscriptionResponse;
import java.util.UUID;
import org.springframework.security.core.Authentication;

public interface BillingService {

    PageResponse<SubscriptionPlanResponse> selectSubscriptionPlanList(
            Authentication authentication,
            Boolean active,
            int page,
            int size
    );

    SubscriptionPlanResponse insertSubscriptionPlan(
            Authentication authentication,
            SubscriptionPlanCreateRequest request
    );

    SubscriptionPlanResponse updateSubscriptionPlanStatus(
            Authentication authentication,
            UUID planId,
            SubscriptionPlanStatusUpdateRequest request
    );

    PageResponse<UserSubscriptionResponse> selectUserSubscriptionList(
            Authentication authentication,
            UUID userId,
            String statusCode,
            int page,
            int size
    );

    UserSubscriptionResponse insertUserSubscription(
            Authentication authentication,
            UserSubscriptionCreateRequest request
    );

    UserSubscriptionResponse updateUserSubscriptionCancel(
            Authentication authentication,
            UUID subscriptionId,
            UserSubscriptionCancelRequest request
    );

    PageResponse<PaymentTransactionResponse> selectPaymentTransactionList(
            Authentication authentication,
            UUID userId,
            UUID subscriptionId,
            String statusCode,
            int page,
            int size
    );

    PaymentTransactionResponse insertPaymentTransaction(
            Authentication authentication,
            PaymentCreateRequest request
    );

    PaymentTransactionResponse updatePaymentTransactionStatus(
            Authentication authentication,
            UUID paymentId,
            PaymentStatusUpdateRequest request
    );

    PageResponse<RefundTransactionResponse> selectRefundTransactionList(
            Authentication authentication,
            UUID userId,
            UUID paymentId,
            String statusCode,
            int page,
            int size
    );

    RefundTransactionResponse insertRefundTransaction(
            Authentication authentication,
            RefundCreateRequest request
    );

    RefundTransactionResponse updateRefundTransactionStatus(
            Authentication authentication,
            UUID refundId,
            RefundStatusUpdateRequest request
    );

    PaymentProviderEventResponse insertPaymentProviderEvent(
            String providerCode,
            String webhookSecret,
            PaymentProviderEventRequest request
    );
}
