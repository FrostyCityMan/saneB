/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: BillingService.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.billing.service;

import com.saneb.common.response.PageResponse;
import com.saneb.domain.billing.dto.PaymentCreateRequest;
import com.saneb.domain.billing.dto.MockMonthlyPaymentRequest;
import com.saneb.domain.billing.dto.MockMonthlyPaymentResponse;
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

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param authentication 입력 값
     *
     * @param active 입력 값
     *
     * @param page 입력 값
     *
     * @param size 입력 값
     *
     * @return 처리 결과
     */
    PageResponse<SubscriptionPlanResponse> selectSubscriptionPlanList(
            Authentication authentication,
            Boolean active,
            int page,
            int size
    );

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param authentication 입력 값
     *
     * @param request 입력 값
     *
     * @return 처리 결과
     */
    SubscriptionPlanResponse insertSubscriptionPlan(
            Authentication authentication,
            SubscriptionPlanCreateRequest request
    );

    /**
     * 업무 데이터를 수정합니다.
     *
     * @param authentication 입력 값
     *
     * @param planId 입력 값
     *
     * @param request 입력 값
     *
     * @return 처리 결과
     */
    SubscriptionPlanResponse updateSubscriptionPlanStatus(
            Authentication authentication,
            UUID planId,
            SubscriptionPlanStatusUpdateRequest request
    );

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param authentication 입력 값
     *
     * @param userId 입력 값
     *
     * @param statusCode 입력 값
     *
     * @param page 입력 값
     *
     * @param size 입력 값
     *
     * @return 처리 결과
     */
    PageResponse<UserSubscriptionResponse> selectUserSubscriptionList(
            Authentication authentication,
            UUID userId,
            String statusCode,
            int page,
            int size
    );

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param authentication 입력 값
     *
     * @param request 입력 값
     *
     * @return 처리 결과
     */
    UserSubscriptionResponse insertUserSubscription(
            Authentication authentication,
            UserSubscriptionCreateRequest request
    );

    /**
     * 업무 데이터를 수정합니다.
     *
     * @param authentication 입력 값
     *
     * @param subscriptionId 입력 값
     *
     * @param request 입력 값
     *
     * @return 처리 결과
     */
    UserSubscriptionResponse updateUserSubscriptionCancel(
            Authentication authentication,
            UUID subscriptionId,
            UserSubscriptionCancelRequest request
    );

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param authentication 입력 값
     *
     * @param userId 입력 값
     *
     * @param subscriptionId 입력 값
     *
     * @param statusCode 입력 값
     *
     * @param page 입력 값
     *
     * @param size 입력 값
     *
     * @return 처리 결과
     */
    PageResponse<PaymentTransactionResponse> selectPaymentTransactionList(
            Authentication authentication,
            UUID userId,
            UUID subscriptionId,
            String statusCode,
            int page,
            int size
    );

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param authentication 입력 값
     *
     * @param request 입력 값
     *
     * @return 처리 결과
     */
    PaymentTransactionResponse insertPaymentTransaction(
            Authentication authentication,
            PaymentCreateRequest request
    );

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param authentication 입력 값
     *
     * @param request 입력 값
     *
     * @return 처리 결과
     */
    MockMonthlyPaymentResponse insertMockMonthlyPayment(
            Authentication authentication,
            MockMonthlyPaymentRequest request
    );

    /**
     * 업무 데이터를 수정합니다.
     *
     * @param authentication 입력 값
     *
     * @param paymentId 입력 값
     *
     * @param request 입력 값
     *
     * @return 처리 결과
     */
    PaymentTransactionResponse updatePaymentTransactionStatus(
            Authentication authentication,
            UUID paymentId,
            PaymentStatusUpdateRequest request
    );

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param authentication 입력 값
     *
     * @param userId 입력 값
     *
     * @param paymentId 입력 값
     *
     * @param statusCode 입력 값
     *
     * @param page 입력 값
     *
     * @param size 입력 값
     *
     * @return 처리 결과
     */
    PageResponse<RefundTransactionResponse> selectRefundTransactionList(
            Authentication authentication,
            UUID userId,
            UUID paymentId,
            String statusCode,
            int page,
            int size
    );

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param authentication 입력 값
     *
     * @param request 입력 값
     *
     * @return 처리 결과
     */
    RefundTransactionResponse insertRefundTransaction(
            Authentication authentication,
            RefundCreateRequest request
    );

    /**
     * 업무 데이터를 수정합니다.
     *
     * @param authentication 입력 값
     *
     * @param refundId 입력 값
     *
     * @param request 입력 값
     *
     * @return 처리 결과
     */
    RefundTransactionResponse updateRefundTransactionStatus(
            Authentication authentication,
            UUID refundId,
            RefundStatusUpdateRequest request
    );

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param providerCode 입력 값
     *
     * @param webhookSecret 입력 값
     *
     * @param request 입력 값
     *
     * @return 처리 결과
     */
    PaymentProviderEventResponse insertPaymentProviderEvent(
            String providerCode,
            String webhookSecret,
            PaymentProviderEventRequest request
    );
}
