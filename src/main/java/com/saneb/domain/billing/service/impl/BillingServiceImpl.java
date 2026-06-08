package com.saneb.domain.billing.service.impl;

import com.saneb.common.error.ApiException;
import com.saneb.common.error.ErrorCode;
import com.saneb.common.response.PageResponse;
import com.saneb.domain.auth.vo.AuthenticatedUserDetails;
import com.saneb.domain.billing.dao.BillingDao;
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
import com.saneb.domain.billing.vo.AuditLogCommand;
import com.saneb.domain.billing.vo.PaymentProviderEventInsertCommand;
import com.saneb.domain.billing.vo.PaymentProviderEventRow;
import com.saneb.domain.billing.vo.PaymentTransactionInsertCommand;
import com.saneb.domain.billing.vo.PaymentTransactionRow;
import com.saneb.domain.billing.vo.PaymentTransactionSearchCondition;
import com.saneb.domain.billing.vo.PaymentTransactionStatusCommand;
import com.saneb.domain.billing.vo.RefundTransactionInsertCommand;
import com.saneb.domain.billing.vo.RefundTransactionRow;
import com.saneb.domain.billing.vo.RefundTransactionSearchCondition;
import com.saneb.domain.billing.vo.RefundTransactionStatusCommand;
import com.saneb.domain.billing.vo.SubscriptionPlanInsertCommand;
import com.saneb.domain.billing.vo.SubscriptionPlanRow;
import com.saneb.domain.billing.vo.SubscriptionPlanSearchCondition;
import com.saneb.domain.billing.vo.SubscriptionPlanStatusCommand;
import com.saneb.domain.billing.vo.UserSubscriptionInsertCommand;
import com.saneb.domain.billing.vo.UserSubscriptionRow;
import com.saneb.domain.billing.vo.UserSubscriptionSearchCondition;
import com.saneb.domain.billing.vo.UserSubscriptionStatusCommand;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BillingServiceImpl implements BillingService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_CODE_LENGTH = 80;
    private static final Set<String> OPERATING_ROLES = Set.of("OPERATOR", "ADMIN");
    private static final Set<String> PLAN_CYCLE_CODES = Set.of("ONE_TIME", "MONTHLY", "YEARLY");
    private static final Set<String> PROVIDER_CODES = Set.of("MANUAL", "TOSS", "NICEPAY", "KCP", "STRIPE");
    private static final Set<String> SUBSCRIPTION_STATUS_CODES = Set.of(
            "PENDING", "ACTIVE", "PAST_DUE", "CANCELED", "EXPIRED"
    );
    private static final Set<String> PAYMENT_STATUS_CODES = Set.of(
            "REQUESTED", "APPROVED", "FAILED", "CANCELED", "REFUNDED"
    );
    private static final Set<String> PAYMENT_UPDATE_STATUS_CODES = Set.of("APPROVED", "FAILED", "CANCELED");
    private static final Set<String> REFUND_STATUS_CODES = Set.of("REQUESTED", "APPROVED", "FAILED");
    private static final Set<String> REFUND_UPDATE_STATUS_CODES = Set.of("APPROVED", "FAILED");
    private static final Set<String> PROVIDER_EVENT_TYPES = Set.of(
            "PAYMENT_APPROVED",
            "PAYMENT_FAILED",
            "PAYMENT_CANCELED",
            "REFUND_APPROVED",
            "REFUND_FAILED"
    );

    private final BillingDao billingDao;
    private final String configuredWebhookSecret;

    @Autowired
    public BillingServiceImpl(BillingDao billingDao, Environment environment) {
        this(billingDao, environment.getProperty("saneb.payment.webhook-secret", ""));
    }

    BillingServiceImpl(BillingDao billingDao, String configuredWebhookSecret) {
        this.billingDao = billingDao;
        this.configuredWebhookSecret = configuredWebhookSecret == null ? "" : configuredWebhookSecret;
    }

    @Override
    public PageResponse<SubscriptionPlanResponse> selectSubscriptionPlanList(
            Authentication authentication,
            Boolean active,
            int page,
            int size
    ) {
        validatePageRequest(page, size);
        AuthenticatedUserDetails actor = selectRequiredPrincipal(authentication);
        Boolean effectiveActive = hasOperatingRole(actor) ? active : Boolean.TRUE;
        SubscriptionPlanSearchCondition condition = new SubscriptionPlanSearchCondition(
                effectiveActive,
                page,
                size,
                (page - 1) * size
        );
        long totalCount = billingDao.selectSubscriptionPlanCount(condition);
        List<SubscriptionPlanResponse> items = billingDao.selectSubscriptionPlanList(condition).stream()
                .map(this::toPlanResponse)
                .toList();
        return PageResponse.of(items, page, size, totalCount);
    }

    @Override
    @Transactional
    public SubscriptionPlanResponse insertSubscriptionPlan(
            Authentication authentication,
            SubscriptionPlanCreateRequest request
    ) {
        AuthenticatedUserDetails actor = selectRequiredPrincipal(authentication);
        validateOperatingRole(actor, "요금제 등록 권한이 없습니다.");
        UUID planId = UUID.randomUUID();
        billingDao.insertSubscriptionPlan(new SubscriptionPlanInsertCommand(
                planId,
                normalizeFreeCode("planCode", request.planCode()),
                trimRequired(request.planName(), "요금제 이름을 입력하세요."),
                normalizeRequiredCode("billingCycleCode", request.billingCycleCode(), PLAN_CYCLE_CODES),
                validateAmount(request.priceAmount(), false),
                normalizeCurrencyCode(request.currencyCode()),
                request.active() == null || request.active(),
                request.sortOrder() == null ? 0 : Math.max(0, request.sortOrder()),
                trimToNull(request.description()),
                actor.userId()
        ));
        return toPlanResponse(selectPlanRow(planId));
    }

    @Override
    @Transactional
    public SubscriptionPlanResponse updateSubscriptionPlanStatus(
            Authentication authentication,
            UUID planId,
            SubscriptionPlanStatusUpdateRequest request
    ) {
        AuthenticatedUserDetails actor = selectRequiredPrincipal(authentication);
        validateOperatingRole(actor, "요금제 상태 변경 권한이 없습니다.");
        int updatedCount = billingDao.updateSubscriptionPlanStatus(new SubscriptionPlanStatusCommand(
                planId,
                request.active(),
                actor.userId()
        ));
        if (updatedCount == 0) {
            throw notFound("요금제를 찾을 수 없습니다.");
        }
        return toPlanResponse(selectPlanRow(planId));
    }

    @Override
    public PageResponse<UserSubscriptionResponse> selectUserSubscriptionList(
            Authentication authentication,
            UUID userId,
            String statusCode,
            int page,
            int size
    ) {
        validatePageRequest(page, size);
        AuthenticatedUserDetails actor = selectRequiredPrincipal(authentication);
        String normalizedStatusCode = normalizeOptionalCode(statusCode);
        validateOptionalCode("statusCode", normalizedStatusCode, SUBSCRIPTION_STATUS_CODES);
        UUID effectiveUserId = hasOperatingRole(actor) ? userId : actor.userId();
        UserSubscriptionSearchCondition condition = new UserSubscriptionSearchCondition(
                effectiveUserId,
                normalizedStatusCode,
                page,
                size,
                (page - 1) * size
        );
        long totalCount = billingDao.selectUserSubscriptionCount(condition);
        List<UserSubscriptionResponse> items = billingDao.selectUserSubscriptionList(condition).stream()
                .map(this::toSubscriptionResponse)
                .toList();
        return PageResponse.of(items, page, size, totalCount);
    }

    @Override
    @Transactional
    public UserSubscriptionResponse insertUserSubscription(
            Authentication authentication,
            UserSubscriptionCreateRequest request
    ) {
        AuthenticatedUserDetails actor = selectRequiredPrincipal(authentication);
        SubscriptionPlanRow plan = selectPlanRow(request.planId());
        if (!plan.active()) {
            throw validationFailed("활성화된 요금제만 구독할 수 있습니다.");
        }
        UUID targetUserId = selectTargetUserId(actor, request.userId());
        OffsetDateTime now = OffsetDateTime.now();
        boolean freePlan = plan.priceAmount().compareTo(BigDecimal.ZERO) == 0;
        String statusCode = freePlan ? "ACTIVE" : "PENDING";
        UUID subscriptionId = UUID.randomUUID();
        billingDao.insertUserSubscription(new UserSubscriptionInsertCommand(
                subscriptionId,
                targetUserId,
                plan.planId(),
                statusCode,
                freePlan ? now : null,
                freePlan ? selectPeriodEnd(plan.billingCycleCode(), now) : null,
                actor.userId()
        ));
        insertAudit(actor.userId(), "SUBSCRIPTION_CREATE", "SUBSCRIPTION", subscriptionId, metadata(
                "userId", targetUserId.toString(),
                "planCode", plan.planCode(),
                "statusCode", statusCode
        ));
        return toSubscriptionResponse(selectSubscriptionRow(subscriptionId));
    }

    @Override
    @Transactional
    public UserSubscriptionResponse updateUserSubscriptionCancel(
            Authentication authentication,
            UUID subscriptionId,
            UserSubscriptionCancelRequest request
    ) {
        AuthenticatedUserDetails actor = selectRequiredPrincipal(authentication);
        UserSubscriptionRow subscription = selectSubscriptionRow(subscriptionId);
        validateUserAccess(actor, subscription.userId(), "구독을 취소할 수 없습니다.");
        if (!Set.of("PENDING", "ACTIVE", "PAST_DUE").contains(subscription.statusCode())) {
            throw new ApiException(ErrorCode.INVALID_STATUS_TRANSITION, HttpStatus.BAD_REQUEST, "취소할 수 없는 구독 상태입니다.");
        }
        updateSubscriptionStatus(subscription, "CANCELED", trimToNull(request.cancelReason()), actor.userId());
        insertAudit(actor.userId(), "SUBSCRIPTION_CANCEL", "SUBSCRIPTION", subscriptionId, metadata(
                "beforeStatusCode", subscription.statusCode(),
                "afterStatusCode", "CANCELED",
                "reasonProvided", String.valueOf(trimToNull(request.cancelReason()) != null)
        ));
        return toSubscriptionResponse(selectSubscriptionRow(subscriptionId));
    }

    @Override
    public PageResponse<PaymentTransactionResponse> selectPaymentTransactionList(
            Authentication authentication,
            UUID userId,
            UUID subscriptionId,
            String statusCode,
            int page,
            int size
    ) {
        validatePageRequest(page, size);
        AuthenticatedUserDetails actor = selectRequiredPrincipal(authentication);
        String normalizedStatusCode = normalizeOptionalCode(statusCode);
        validateOptionalCode("statusCode", normalizedStatusCode, PAYMENT_STATUS_CODES);
        UUID effectiveUserId = hasOperatingRole(actor) ? userId : actor.userId();
        PaymentTransactionSearchCondition condition = new PaymentTransactionSearchCondition(
                effectiveUserId,
                subscriptionId,
                normalizedStatusCode,
                page,
                size,
                (page - 1) * size
        );
        long totalCount = billingDao.selectPaymentTransactionCount(condition);
        List<PaymentTransactionResponse> items = billingDao.selectPaymentTransactionList(condition).stream()
                .map(this::toPaymentResponse)
                .toList();
        return PageResponse.of(items, page, size, totalCount);
    }

    @Override
    @Transactional
    public PaymentTransactionResponse insertPaymentTransaction(
            Authentication authentication,
            PaymentCreateRequest request
    ) {
        AuthenticatedUserDetails actor = selectRequiredPrincipal(authentication);
        UserSubscriptionRow subscription = selectSubscriptionRow(request.subscriptionId());
        validateUserAccess(actor, subscription.userId(), "결제를 요청할 수 없습니다.");
        if (!Set.of("PENDING", "ACTIVE", "PAST_DUE").contains(subscription.statusCode())) {
            throw new ApiException(ErrorCode.PROGRESS_CONDITION_NOT_MET, HttpStatus.CONFLICT, "결제할 수 없는 구독 상태입니다.");
        }
        String providerCode = normalizeRequiredCode("providerCode", request.providerCode(), PROVIDER_CODES);
        BigDecimal amount = validateAmount(request.amount(), false);
        if (subscription.priceAmount().compareTo(amount) != 0) {
            throw validationFailed("결제 금액이 요금제 금액과 일치하지 않습니다.");
        }
        String currencyCode = normalizeCurrencyCode(request.currencyCode());
        if (!subscription.currencyCode().equals(currencyCode)) {
            throw validationFailed("결제 통화가 요금제 통화와 일치하지 않습니다.");
        }

        UUID paymentId = UUID.randomUUID();
        billingDao.insertPaymentTransaction(new PaymentTransactionInsertCommand(
                paymentId,
                subscription.subscriptionId(),
                subscription.userId(),
                subscription.planId(),
                providerCode,
                generateMerchantUid(paymentId),
                amount,
                currencyCode,
                actor.userId()
        ));
        insertAudit(actor.userId(), "PAYMENT_TRANSACTION_CREATE", "PAYMENT_TRANSACTION", paymentId, metadata(
                "subscriptionId", subscription.subscriptionId().toString(),
                "providerCode", providerCode,
                "amount", amount.toPlainString()
        ));
        return toPaymentResponse(selectPaymentRow(paymentId));
    }

    @Override
    @Transactional
    public PaymentTransactionResponse updatePaymentTransactionStatus(
            Authentication authentication,
            UUID paymentId,
            PaymentStatusUpdateRequest request
    ) {
        AuthenticatedUserDetails actor = selectRequiredPrincipal(authentication);
        validateOperatingRole(actor, "결제 상태 변경 권한이 없습니다.");
        PaymentTransactionRow payment = selectPaymentRow(paymentId);
        String afterStatusCode = normalizeRequiredCode(
                "statusCode",
                request.statusCode(),
                PAYMENT_UPDATE_STATUS_CODES
        );
        updatePaymentStatus(payment, afterStatusCode, request.providerPaymentKey(), request.failureCode(),
                request.failureMessage(), actor.userId());
        return toPaymentResponse(selectPaymentRow(paymentId));
    }

    @Override
    public PageResponse<RefundTransactionResponse> selectRefundTransactionList(
            Authentication authentication,
            UUID userId,
            UUID paymentId,
            String statusCode,
            int page,
            int size
    ) {
        validatePageRequest(page, size);
        AuthenticatedUserDetails actor = selectRequiredPrincipal(authentication);
        String normalizedStatusCode = normalizeOptionalCode(statusCode);
        validateOptionalCode("statusCode", normalizedStatusCode, REFUND_STATUS_CODES);
        UUID effectiveUserId = hasOperatingRole(actor) ? userId : actor.userId();
        RefundTransactionSearchCondition condition = new RefundTransactionSearchCondition(
                effectiveUserId,
                paymentId,
                normalizedStatusCode,
                page,
                size,
                (page - 1) * size
        );
        long totalCount = billingDao.selectRefundTransactionCount(condition);
        List<RefundTransactionResponse> items = billingDao.selectRefundTransactionList(condition).stream()
                .map(this::toRefundResponse)
                .toList();
        return PageResponse.of(items, page, size, totalCount);
    }

    @Override
    @Transactional
    public RefundTransactionResponse insertRefundTransaction(
            Authentication authentication,
            RefundCreateRequest request
    ) {
        AuthenticatedUserDetails actor = selectRequiredPrincipal(authentication);
        PaymentTransactionRow payment = selectPaymentRow(request.paymentId());
        validateUserAccess(actor, payment.userId(), "환불을 요청할 수 없습니다.");
        if (!"APPROVED".equals(payment.statusCode())) {
            throw new ApiException(ErrorCode.PROGRESS_CONDITION_NOT_MET, HttpStatus.CONFLICT, "승인된 결제만 환불할 수 있습니다.");
        }
        BigDecimal refundAmount = validateAmount(request.refundAmount(), true);
        BigDecimal approvedRefundAmount = billingDao.selectApprovedRefundAmount(payment.paymentId());
        if (approvedRefundAmount == null) {
            approvedRefundAmount = BigDecimal.ZERO;
        }
        if (approvedRefundAmount.add(refundAmount).compareTo(payment.amount()) > 0) {
            throw validationFailed("환불 금액이 결제 금액을 초과합니다.");
        }

        UUID refundId = UUID.randomUUID();
        billingDao.insertRefundTransaction(new RefundTransactionInsertCommand(
                refundId,
                payment.paymentId(),
                payment.userId(),
                payment.providerCode(),
                refundAmount,
                trimToNull(request.reason()),
                actor.userId()
        ));
        insertAudit(actor.userId(), "REFUND_TRANSACTION_CREATE", "REFUND_TRANSACTION", refundId, metadata(
                "paymentId", payment.paymentId().toString(),
                "providerCode", payment.providerCode(),
                "amount", refundAmount.toPlainString()
        ));
        return toRefundResponse(selectRefundRow(refundId));
    }

    @Override
    @Transactional
    public RefundTransactionResponse updateRefundTransactionStatus(
            Authentication authentication,
            UUID refundId,
            RefundStatusUpdateRequest request
    ) {
        AuthenticatedUserDetails actor = selectRequiredPrincipal(authentication);
        validateOperatingRole(actor, "환불 상태 변경 권한이 없습니다.");
        RefundTransactionRow refund = selectRefundRow(refundId);
        String afterStatusCode = normalizeRequiredCode(
                "statusCode",
                request.statusCode(),
                REFUND_UPDATE_STATUS_CODES
        );
        updateRefundStatus(refund, afterStatusCode, request.providerRefundKey(), request.failureCode(),
                request.failureMessage(), actor.userId());
        return toRefundResponse(selectRefundRow(refundId));
    }

    @Override
    @Transactional
    public PaymentProviderEventResponse insertPaymentProviderEvent(
            String providerCode,
            String webhookSecret,
            PaymentProviderEventRequest request
    ) {
        validateWebhookSecret(webhookSecret);
        String normalizedProviderCode = normalizeRequiredCode("providerCode", providerCode, PROVIDER_CODES);
        String providerEventId = trimRequired(request.eventId(), "eventId 값이 필요합니다.");
        PaymentProviderEventRow existingEvent = billingDao.selectPaymentProviderEventByKey(
                normalizedProviderCode,
                providerEventId
        );
        if (existingEvent != null) {
            return toProviderEventResponse(existingEvent);
        }

        String eventTypeCode = normalizeRequiredCode("eventTypeCode", request.eventTypeCode(), PROVIDER_EVENT_TYPES);
        UUID paymentId = null;
        UUID refundId = null;
        if (eventTypeCode.startsWith("PAYMENT_")) {
            PaymentTransactionRow payment = selectPaymentForProviderEvent(request);
            validateProviderEventAmount(payment.amount(), payment.currencyCode(), request.amount(), request.currencyCode());
            String afterStatusCode = switch (eventTypeCode) {
                case "PAYMENT_APPROVED" -> "APPROVED";
                case "PAYMENT_FAILED" -> "FAILED";
                case "PAYMENT_CANCELED" -> "CANCELED";
                default -> throw validationFailed("eventTypeCode 값이 올바르지 않습니다.");
            };
            updatePaymentStatus(payment, afterStatusCode, request.providerPaymentKey(), request.failureCode(),
                    request.failureMessage(), null);
            paymentId = payment.paymentId();
        } else {
            RefundTransactionRow refund = selectRefundForProviderEvent(request);
            validateProviderEventAmount(refund.refundAmount(), null, request.amount(), null);
            String afterStatusCode = "REFUND_APPROVED".equals(eventTypeCode) ? "APPROVED" : "FAILED";
            updateRefundStatus(refund, afterStatusCode, request.providerRefundKey(), request.failureCode(),
                    request.failureMessage(), null);
            refundId = refund.refundId();
        }

        UUID eventId = UUID.randomUUID();
        billingDao.insertPaymentProviderEvent(new PaymentProviderEventInsertCommand(
                eventId,
                normalizedProviderCode,
                providerEventId,
                eventTypeCode,
                paymentId,
                refundId,
                "RECEIVED",
                metadata(
                        "eventTypeCode", eventTypeCode,
                        "hasFailure", String.valueOf(trimToNull(request.failureCode()) != null),
                        "amountProvided", String.valueOf(request.amount() != null)
                )
        ));
        insertAudit(null, "PAYMENT_PROVIDER_EVENT_RECEIVE",
                paymentId == null ? "REFUND_TRANSACTION" : "PAYMENT_TRANSACTION",
                paymentId == null ? refundId : paymentId,
                metadata("providerCode", normalizedProviderCode, "eventTypeCode", eventTypeCode, "eventId", providerEventId));
        return toProviderEventResponse(billingDao.selectPaymentProviderEventByKey(normalizedProviderCode, providerEventId));
    }

    private void updatePaymentStatus(
            PaymentTransactionRow payment,
            String afterStatusCode,
            String providerPaymentKey,
            String failureCode,
            String failureMessage,
            UUID actorUserId
    ) {
        validatePaymentTransition(payment.statusCode(), afterStatusCode);
        billingDao.updatePaymentTransactionStatus(new PaymentTransactionStatusCommand(
                payment.paymentId(),
                afterStatusCode,
                trimToNull(providerPaymentKey),
                trimToNull(failureCode),
                trimToNull(failureMessage),
                actorUserId
        ));
        if ("APPROVED".equals(afterStatusCode)) {
            UserSubscriptionRow subscription = selectSubscriptionRow(payment.subscriptionId());
            OffsetDateTime now = OffsetDateTime.now();
            updateSubscriptionStatus(subscription, "ACTIVE", null, actorUserId, now,
                    selectPeriodEnd(subscription.billingCycleCode(), now));
        } else if ("FAILED".equals(afterStatusCode)) {
            UserSubscriptionRow subscription = selectSubscriptionRow(payment.subscriptionId());
            if ("PENDING".equals(subscription.statusCode())) {
                updateSubscriptionStatus(subscription, "PAST_DUE", null, actorUserId);
            }
        }
        insertAudit(actorUserId, "PAYMENT_TRANSACTION_STATUS_UPDATE", "PAYMENT_TRANSACTION", payment.paymentId(),
                metadata("beforeStatusCode", payment.statusCode(), "afterStatusCode", afterStatusCode,
                        "providerPaymentKeyProvided", String.valueOf(trimToNull(providerPaymentKey) != null)));
    }

    private void updateRefundStatus(
            RefundTransactionRow refund,
            String afterStatusCode,
            String providerRefundKey,
            String failureCode,
            String failureMessage,
            UUID actorUserId
    ) {
        if (!"REQUESTED".equals(refund.statusCode())) {
            throw new ApiException(ErrorCode.INVALID_STATUS_TRANSITION, HttpStatus.BAD_REQUEST, "변경할 수 없는 환불 상태입니다.");
        }
        billingDao.updateRefundTransactionStatus(new RefundTransactionStatusCommand(
                refund.refundId(),
                afterStatusCode,
                trimToNull(providerRefundKey),
                trimToNull(failureCode),
                trimToNull(failureMessage),
                actorUserId
        ));
        if ("APPROVED".equals(afterStatusCode)) {
            PaymentTransactionRow payment = selectPaymentRow(refund.paymentId());
            BigDecimal approvedRefundAmount = billingDao.selectApprovedRefundAmount(payment.paymentId());
            if (approvedRefundAmount != null && approvedRefundAmount.compareTo(payment.amount()) >= 0) {
                billingDao.updatePaymentTransactionStatus(new PaymentTransactionStatusCommand(
                        payment.paymentId(),
                        "REFUNDED",
                        payment.providerPaymentKey(),
                        null,
                        null,
                        actorUserId
                ));
            }
        }
        insertAudit(actorUserId, "REFUND_TRANSACTION_STATUS_UPDATE", "REFUND_TRANSACTION", refund.refundId(), metadata(
                "beforeStatusCode", refund.statusCode(),
                "afterStatusCode", afterStatusCode,
                "providerRefundKeyProvided", String.valueOf(trimToNull(providerRefundKey) != null)
        ));
    }

    private void updateSubscriptionStatus(
            UserSubscriptionRow subscription,
            String statusCode,
            String cancelReason,
            UUID actorUserId
    ) {
        updateSubscriptionStatus(subscription, statusCode, cancelReason, actorUserId,
                subscription.currentPeriodStart(), subscription.currentPeriodEnd());
    }

    private void updateSubscriptionStatus(
            UserSubscriptionRow subscription,
            String statusCode,
            String cancelReason,
            UUID actorUserId,
            OffsetDateTime currentPeriodStart,
            OffsetDateTime currentPeriodEnd
    ) {
        billingDao.updateUserSubscriptionStatus(new UserSubscriptionStatusCommand(
                subscription.subscriptionId(),
                statusCode,
                currentPeriodStart,
                currentPeriodEnd,
                cancelReason,
                actorUserId
        ));
    }

    private void validatePaymentTransition(String beforeStatusCode, String afterStatusCode) {
        boolean allowed = switch (beforeStatusCode) {
            case "REQUESTED" -> Set.of("APPROVED", "FAILED", "CANCELED").contains(afterStatusCode);
            case "APPROVED" -> "REFUNDED".equals(afterStatusCode);
            default -> false;
        };
        if (!allowed) {
            throw new ApiException(ErrorCode.INVALID_STATUS_TRANSITION, HttpStatus.BAD_REQUEST, "허용되지 않는 결제 상태 변경입니다.");
        }
    }

    private PaymentTransactionRow selectPaymentForProviderEvent(PaymentProviderEventRequest request) {
        if (request.paymentId() != null) {
            return selectPaymentRow(request.paymentId());
        }
        String merchantUid = trimToNull(request.merchantUid());
        if (merchantUid != null) {
            PaymentTransactionRow row = billingDao.selectPaymentTransactionByMerchantUid(merchantUid);
            if (row != null) {
                return row;
            }
        }
        throw notFound("결제 거래를 찾을 수 없습니다.");
    }

    private RefundTransactionRow selectRefundForProviderEvent(PaymentProviderEventRequest request) {
        if (request.refundId() == null) {
            throw validationFailed("refundId 값이 필요합니다.");
        }
        return selectRefundRow(request.refundId());
    }

    private void validateProviderEventAmount(
            BigDecimal expectedAmount,
            String expectedCurrencyCode,
            BigDecimal requestAmount,
            String requestCurrencyCode
    ) {
        if (requestAmount != null && validateAmount(requestAmount, true).compareTo(expectedAmount) != 0) {
            throw validationFailed("결제사 이벤트 금액이 저장된 금액과 일치하지 않습니다.");
        }
        if (expectedCurrencyCode != null && requestCurrencyCode != null
                && !expectedCurrencyCode.equals(normalizeCurrencyCode(requestCurrencyCode))) {
            throw validationFailed("결제사 이벤트 통화가 저장된 통화와 일치하지 않습니다.");
        }
    }

    private void validateWebhookSecret(String webhookSecret) {
        if (configuredWebhookSecret.isBlank()
                || webhookSecret == null
                || !configuredWebhookSecret.equals(webhookSecret)) {
            throw new ApiException(ErrorCode.AUTH_FORBIDDEN, HttpStatus.FORBIDDEN, "결제사 이벤트 인증 값이 올바르지 않습니다.");
        }
    }

    private UUID selectTargetUserId(AuthenticatedUserDetails actor, UUID requestedUserId) {
        if (hasOperatingRole(actor)) {
            return requestedUserId == null ? actor.userId() : requestedUserId;
        }
        if (requestedUserId != null && !requestedUserId.equals(actor.userId())) {
            throw new ApiException(ErrorCode.AUTH_FORBIDDEN, HttpStatus.FORBIDDEN, "본인 구독만 생성할 수 있습니다.");
        }
        return actor.userId();
    }

    private void validateUserAccess(AuthenticatedUserDetails actor, UUID ownerUserId, String message) {
        if (!hasOperatingRole(actor) && !ownerUserId.equals(actor.userId())) {
            throw new ApiException(ErrorCode.AUTH_FORBIDDEN, HttpStatus.FORBIDDEN, message);
        }
    }

    private void validateOperatingRole(AuthenticatedUserDetails actor, String message) {
        if (!hasOperatingRole(actor)) {
            throw new ApiException(ErrorCode.AUTH_FORBIDDEN, HttpStatus.FORBIDDEN, message);
        }
    }

    private SubscriptionPlanRow selectPlanRow(UUID planId) {
        SubscriptionPlanRow row = billingDao.selectSubscriptionPlanDetails(planId);
        if (row == null) {
            throw notFound("요금제를 찾을 수 없습니다.");
        }
        return row;
    }

    private UserSubscriptionRow selectSubscriptionRow(UUID subscriptionId) {
        UserSubscriptionRow row = billingDao.selectUserSubscriptionDetails(subscriptionId);
        if (row == null) {
            throw notFound("구독을 찾을 수 없습니다.");
        }
        return row;
    }

    private PaymentTransactionRow selectPaymentRow(UUID paymentId) {
        PaymentTransactionRow row = billingDao.selectPaymentTransactionDetails(paymentId);
        if (row == null) {
            throw notFound("결제 거래를 찾을 수 없습니다.");
        }
        return row;
    }

    private RefundTransactionRow selectRefundRow(UUID refundId) {
        RefundTransactionRow row = billingDao.selectRefundTransactionDetails(refundId);
        if (row == null) {
            throw notFound("환불 거래를 찾을 수 없습니다.");
        }
        return row;
    }

    private OffsetDateTime selectPeriodEnd(String billingCycleCode, OffsetDateTime startAt) {
        return switch (billingCycleCode) {
            case "MONTHLY" -> startAt.plusMonths(1);
            case "YEARLY" -> startAt.plusYears(1);
            default -> null;
        };
    }

    private String generateMerchantUid(UUID paymentId) {
        return "SANEB-" + OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + "-" + paymentId.toString().substring(0, 8);
    }

    private BigDecimal validateAmount(BigDecimal amount, boolean positiveOnly) {
        if (amount == null || amount.scale() > 2) {
            throw validationFailed("금액은 소수점 둘째 자리까지 입력하세요.");
        }
        int comparison = amount.compareTo(BigDecimal.ZERO);
        if ((positiveOnly && comparison <= 0) || (!positiveOnly && comparison < 0)) {
            throw validationFailed(positiveOnly ? "금액은 0보다 커야 합니다." : "금액은 0 이상이어야 합니다.");
        }
        return amount;
    }

    private void validatePageRequest(int page, int size) {
        if (page < 1 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new ApiException(ErrorCode.INVALID_PAGE_REQUEST, HttpStatus.BAD_REQUEST, "페이지 요청 값이 올바르지 않습니다.");
        }
    }

    private String normalizeRequiredCode(String fieldName, String value, Set<String> allowedValues) {
        String code = normalizeOptionalCode(value);
        if (code == null || !allowedValues.contains(code)) {
            throw validationFailed(fieldName + " 값이 올바르지 않습니다.");
        }
        return code;
    }

    private String normalizeFreeCode(String fieldName, String value) {
        String code = normalizeOptionalCode(value);
        if (code == null || code.length() > MAX_CODE_LENGTH || !code.matches("[A-Z0-9_\\-]+")) {
            throw validationFailed(fieldName + " 값이 올바르지 않습니다.");
        }
        return code;
    }

    private void validateOptionalCode(String fieldName, String value, Set<String> allowedValues) {
        if (value != null && !allowedValues.contains(value)) {
            throw validationFailed(fieldName + " 값이 올바르지 않습니다.");
        }
    }

    private String normalizeOptionalCode(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed.toUpperCase(Locale.ROOT);
    }

    private String normalizeCurrencyCode(String value) {
        String code = normalizeOptionalCode(value);
        if (code == null) {
            return "KRW";
        }
        if (!code.matches("[A-Z]{3}")) {
            throw validationFailed("통화 코드는 영문 3자리로 입력하세요.");
        }
        return code;
    }

    private String trimRequired(String value, String message) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw validationFailed(message);
        }
        return trimmed;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private AuthenticatedUserDetails selectRequiredPrincipal(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ApiException(ErrorCode.AUTH_REQUIRED, HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
        }
        if (authentication.getPrincipal() instanceof AuthenticatedUserDetails principal) {
            return principal;
        }
        throw new ApiException(ErrorCode.AUTH_REQUIRED, HttpStatus.UNAUTHORIZED, "DB 인증 사용자만 사용할 수 있습니다.");
    }

    private boolean hasOperatingRole(AuthenticatedUserDetails actor) {
        return actor.roles().stream().anyMatch(OPERATING_ROLES::contains);
    }

    private SubscriptionPlanResponse toPlanResponse(SubscriptionPlanRow row) {
        return new SubscriptionPlanResponse(
                row.planId(),
                row.planCode(),
                row.planName(),
                row.billingCycleCode(),
                row.priceAmount(),
                row.currencyCode(),
                row.active(),
                row.sortOrder(),
                row.description(),
                row.createdAt(),
                row.updatedAt()
        );
    }

    private UserSubscriptionResponse toSubscriptionResponse(UserSubscriptionRow row) {
        return new UserSubscriptionResponse(
                row.subscriptionId(),
                row.userId(),
                row.planId(),
                row.planCode(),
                row.planName(),
                row.billingCycleCode(),
                row.priceAmount(),
                row.currencyCode(),
                row.statusCode(),
                row.currentPeriodStart(),
                row.currentPeriodEnd(),
                row.canceledAt(),
                row.cancelReason(),
                row.createdAt(),
                row.updatedAt()
        );
    }

    private PaymentTransactionResponse toPaymentResponse(PaymentTransactionRow row) {
        return new PaymentTransactionResponse(
                row.paymentId(),
                row.subscriptionId(),
                row.userId(),
                row.planId(),
                row.providerCode(),
                row.merchantUid(),
                row.providerPaymentKey(),
                row.statusCode(),
                row.amount(),
                row.currencyCode(),
                row.requestedAt(),
                row.approvedAt(),
                row.failedAt(),
                row.failureCode(),
                row.failureMessage(),
                row.createdAt(),
                row.updatedAt()
        );
    }

    private RefundTransactionResponse toRefundResponse(RefundTransactionRow row) {
        return new RefundTransactionResponse(
                row.refundId(),
                row.paymentId(),
                row.userId(),
                row.providerCode(),
                row.providerRefundKey(),
                row.statusCode(),
                row.refundAmount(),
                row.reason(),
                row.requestedBy(),
                row.requestedAt(),
                row.completedAt(),
                row.failureCode(),
                row.failureMessage(),
                row.createdAt(),
                row.updatedAt()
        );
    }

    private PaymentProviderEventResponse toProviderEventResponse(PaymentProviderEventRow row) {
        return new PaymentProviderEventResponse(
                row.eventId(),
                row.providerCode(),
                row.providerEventId(),
                row.eventTypeCode(),
                row.paymentId(),
                row.refundId(),
                row.resultCode(),
                row.receivedAt()
        );
    }

    private void insertAudit(UUID actorUserId, String actionCode, String resourceType, UUID resourceId, String metadataJson) {
        billingDao.insertAuditLog(new AuditLogCommand(
                actorUserId,
                actionCode,
                resourceType,
                resourceId,
                "SUCCESS",
                metadataJson
        ));
    }

    private String metadata(String key1, String value1, String key2, String value2, String key3, String value3) {
        return "{\"" + key1 + "\":\"" + safeValue(value1) + "\",\""
                + key2 + "\":\"" + safeValue(value2) + "\",\""
                + key3 + "\":\"" + safeValue(value3) + "\"}";
    }

    private String safeValue(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private ApiException validationFailed(String message) {
        return new ApiException(ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST, message);
    }

    private ApiException notFound(String message) {
        return new ApiException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, message);
    }
}
