package com.saneb.domain.billing.dao;

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
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface BillingDao {

    List<SubscriptionPlanRow> selectSubscriptionPlanList(SubscriptionPlanSearchCondition condition);

    long selectSubscriptionPlanCount(SubscriptionPlanSearchCondition condition);

    SubscriptionPlanRow selectSubscriptionPlanDetails(UUID planId);

    void insertSubscriptionPlan(SubscriptionPlanInsertCommand command);

    int updateSubscriptionPlanStatus(SubscriptionPlanStatusCommand command);

    List<UserSubscriptionRow> selectUserSubscriptionList(UserSubscriptionSearchCondition condition);

    long selectUserSubscriptionCount(UserSubscriptionSearchCondition condition);

    UserSubscriptionRow selectUserSubscriptionDetails(UUID subscriptionId);

    UserSubscriptionRow selectCurrentUserSubscriptionDetails(UUID userId);

    void insertUserSubscription(UserSubscriptionInsertCommand command);

    int updateUserSubscriptionStatus(UserSubscriptionStatusCommand command);

    List<PaymentTransactionRow> selectPaymentTransactionList(PaymentTransactionSearchCondition condition);

    long selectPaymentTransactionCount(PaymentTransactionSearchCondition condition);

    PaymentTransactionRow selectPaymentTransactionDetails(UUID paymentId);

    PaymentTransactionRow selectPaymentTransactionByMerchantUid(String merchantUid);

    void insertPaymentTransaction(PaymentTransactionInsertCommand command);

    int updatePaymentTransactionStatus(PaymentTransactionStatusCommand command);

    List<RefundTransactionRow> selectRefundTransactionList(RefundTransactionSearchCondition condition);

    long selectRefundTransactionCount(RefundTransactionSearchCondition condition);

    RefundTransactionRow selectRefundTransactionDetails(UUID refundId);

    BigDecimal selectApprovedRefundAmount(UUID paymentId);

    void insertRefundTransaction(RefundTransactionInsertCommand command);

    int updateRefundTransactionStatus(RefundTransactionStatusCommand command);

    PaymentProviderEventRow selectPaymentProviderEventByKey(
            @Param("providerCode") String providerCode,
            @Param("providerEventId") String providerEventId
    );

    void insertPaymentProviderEvent(PaymentProviderEventInsertCommand command);

    void insertAuditLog(AuditLogCommand command);
}
