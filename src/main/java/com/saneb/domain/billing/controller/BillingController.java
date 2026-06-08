package com.saneb.domain.billing.controller;

import com.saneb.common.response.ApiResponse;
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
import com.saneb.domain.billing.service.BillingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1")
public class BillingController {

    private final BillingService billingService;

    public BillingController(BillingService billingService) {
        this.billingService = billingService;
    }

    @GetMapping("/subscription-plans")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageResponse<SubscriptionPlanResponse>> selectSubscriptionPlanList(
            Authentication authentication,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ApiResponse.success(billingService.selectSubscriptionPlanList(authentication, active, page, size));
    }

    @PostMapping("/subscription-plans")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ApiResponse<SubscriptionPlanResponse> insertSubscriptionPlan(
            Authentication authentication,
            @Valid @RequestBody SubscriptionPlanCreateRequest request
    ) {
        return ApiResponse.success(billingService.insertSubscriptionPlan(authentication, request));
    }

    @PatchMapping("/subscription-plans/{planId}/status")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ApiResponse<SubscriptionPlanResponse> updateSubscriptionPlanStatus(
            Authentication authentication,
            @PathVariable UUID planId,
            @Valid @RequestBody SubscriptionPlanStatusUpdateRequest request
    ) {
        return ApiResponse.success(billingService.updateSubscriptionPlanStatus(authentication, planId, request));
    }

    @GetMapping("/subscriptions")
    @PreAuthorize("hasAnyRole('USER', 'OPERATOR', 'ADMIN')")
    public ApiResponse<PageResponse<UserSubscriptionResponse>> selectUserSubscriptionList(
            Authentication authentication,
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) String statusCode,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ApiResponse.success(billingService.selectUserSubscriptionList(
                authentication,
                userId,
                statusCode,
                page,
                size
        ));
    }

    @PostMapping("/subscriptions")
    @PreAuthorize("hasAnyRole('USER', 'OPERATOR', 'ADMIN')")
    public ApiResponse<UserSubscriptionResponse> insertUserSubscription(
            Authentication authentication,
            @Valid @RequestBody UserSubscriptionCreateRequest request
    ) {
        return ApiResponse.success(billingService.insertUserSubscription(authentication, request));
    }

    @PatchMapping("/subscriptions/{subscriptionId}/cancel")
    @PreAuthorize("hasAnyRole('USER', 'OPERATOR', 'ADMIN')")
    public ApiResponse<UserSubscriptionResponse> updateUserSubscriptionCancel(
            Authentication authentication,
            @PathVariable UUID subscriptionId,
            @RequestBody UserSubscriptionCancelRequest request
    ) {
        return ApiResponse.success(billingService.updateUserSubscriptionCancel(authentication, subscriptionId, request));
    }

    @GetMapping("/payments")
    @PreAuthorize("hasAnyRole('USER', 'OPERATOR', 'ADMIN')")
    public ApiResponse<PageResponse<PaymentTransactionResponse>> selectPaymentTransactionList(
            Authentication authentication,
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) UUID subscriptionId,
            @RequestParam(required = false) String statusCode,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ApiResponse.success(billingService.selectPaymentTransactionList(
                authentication,
                userId,
                subscriptionId,
                statusCode,
                page,
                size
        ));
    }

    @PostMapping("/payments")
    @PreAuthorize("hasAnyRole('USER', 'OPERATOR', 'ADMIN')")
    public ApiResponse<PaymentTransactionResponse> insertPaymentTransaction(
            Authentication authentication,
            @Valid @RequestBody PaymentCreateRequest request
    ) {
        return ApiResponse.success(billingService.insertPaymentTransaction(authentication, request));
    }

    @PatchMapping("/payments/{paymentId}/status")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ApiResponse<PaymentTransactionResponse> updatePaymentTransactionStatus(
            Authentication authentication,
            @PathVariable UUID paymentId,
            @Valid @RequestBody PaymentStatusUpdateRequest request
    ) {
        return ApiResponse.success(billingService.updatePaymentTransactionStatus(authentication, paymentId, request));
    }

    @GetMapping("/refunds")
    @PreAuthorize("hasAnyRole('USER', 'OPERATOR', 'ADMIN')")
    public ApiResponse<PageResponse<RefundTransactionResponse>> selectRefundTransactionList(
            Authentication authentication,
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) UUID paymentId,
            @RequestParam(required = false) String statusCode,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ApiResponse.success(billingService.selectRefundTransactionList(
                authentication,
                userId,
                paymentId,
                statusCode,
                page,
                size
        ));
    }

    @PostMapping("/refunds")
    @PreAuthorize("hasAnyRole('USER', 'OPERATOR', 'ADMIN')")
    public ApiResponse<RefundTransactionResponse> insertRefundTransaction(
            Authentication authentication,
            @Valid @RequestBody RefundCreateRequest request
    ) {
        return ApiResponse.success(billingService.insertRefundTransaction(authentication, request));
    }

    @PatchMapping("/refunds/{refundId}/status")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ApiResponse<RefundTransactionResponse> updateRefundTransactionStatus(
            Authentication authentication,
            @PathVariable UUID refundId,
            @Valid @RequestBody RefundStatusUpdateRequest request
    ) {
        return ApiResponse.success(billingService.updateRefundTransactionStatus(authentication, refundId, request));
    }

    @PostMapping("/payment-webhooks/{providerCode}")
    public ApiResponse<PaymentProviderEventResponse> insertPaymentProviderEvent(
            @PathVariable String providerCode,
            @RequestHeader(value = "X-SANEB-WEBHOOK-SECRET", required = false) String webhookSecret,
            @Valid @RequestBody PaymentProviderEventRequest request
    ) {
        return ApiResponse.success(billingService.insertPaymentProviderEvent(providerCode, webhookSecret, request));
    }
}
