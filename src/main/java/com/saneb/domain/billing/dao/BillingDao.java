/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: BillingDao.java
 * 작성자: 김도훈
 *
 */

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

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param condition 입력 값
     *
     * @return 처리 결과
     */
    List<SubscriptionPlanRow> selectSubscriptionPlanList(SubscriptionPlanSearchCondition condition);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param condition 입력 값
     *
     * @return 처리 결과
     */
    long selectSubscriptionPlanCount(SubscriptionPlanSearchCondition condition);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param planId 입력 값
     *
     * @return 처리 결과
     */
    SubscriptionPlanRow selectSubscriptionPlanDetails(UUID planId);

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param command 입력 값
     */
    void insertSubscriptionPlan(SubscriptionPlanInsertCommand command);

    /**
     * 업무 데이터를 수정합니다.
     *
     * @param command 입력 값
     *
     * @return 처리 결과
     */
    int updateSubscriptionPlanStatus(SubscriptionPlanStatusCommand command);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param condition 입력 값
     *
     * @return 처리 결과
     */
    List<UserSubscriptionRow> selectUserSubscriptionList(UserSubscriptionSearchCondition condition);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param condition 입력 값
     *
     * @return 처리 결과
     */
    long selectUserSubscriptionCount(UserSubscriptionSearchCondition condition);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param subscriptionId 입력 값
     *
     * @return 처리 결과
     */
    UserSubscriptionRow selectUserSubscriptionDetails(UUID subscriptionId);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param userId 입력 값
     *
     * @return 처리 결과
     */
    UserSubscriptionRow selectCurrentUserSubscriptionDetails(UUID userId);

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param command 입력 값
     */
    void insertUserSubscription(UserSubscriptionInsertCommand command);

    /**
     * 업무 데이터를 수정합니다.
     *
     * @param command 입력 값
     *
     * @return 처리 결과
     */
    int updateUserSubscriptionStatus(UserSubscriptionStatusCommand command);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param condition 입력 값
     *
     * @return 처리 결과
     */
    List<PaymentTransactionRow> selectPaymentTransactionList(PaymentTransactionSearchCondition condition);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param condition 입력 값
     *
     * @return 처리 결과
     */
    long selectPaymentTransactionCount(PaymentTransactionSearchCondition condition);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param paymentId 입력 값
     *
     * @return 처리 결과
     */
    PaymentTransactionRow selectPaymentTransactionDetails(UUID paymentId);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param merchantUid 입력 값
     *
     * @return 처리 결과
     */
    PaymentTransactionRow selectPaymentTransactionByMerchantUid(String merchantUid);

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param command 입력 값
     */
    void insertPaymentTransaction(PaymentTransactionInsertCommand command);

    /**
     * 업무 데이터를 수정합니다.
     *
     * @param command 입력 값
     *
     * @return 처리 결과
     */
    int updatePaymentTransactionStatus(PaymentTransactionStatusCommand command);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param condition 입력 값
     *
     * @return 처리 결과
     */
    List<RefundTransactionRow> selectRefundTransactionList(RefundTransactionSearchCondition condition);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param condition 입력 값
     *
     * @return 처리 결과
     */
    long selectRefundTransactionCount(RefundTransactionSearchCondition condition);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param refundId 입력 값
     *
     * @return 처리 결과
     */
    RefundTransactionRow selectRefundTransactionDetails(UUID refundId);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param paymentId 입력 값
     *
     * @return 처리 결과
     */
    BigDecimal selectApprovedRefundAmount(UUID paymentId);

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param command 입력 값
     */
    void insertRefundTransaction(RefundTransactionInsertCommand command);

    /**
     * 업무 데이터를 수정합니다.
     *
     * @param command 입력 값
     *
     * @return 처리 결과
     */
    int updateRefundTransactionStatus(RefundTransactionStatusCommand command);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param providerCode 입력 값
     *
     * @param providerEventId 입력 값
     *
     * @return 처리 결과
     */
    PaymentProviderEventRow selectPaymentProviderEventByKey(
            @Param("providerCode") String providerCode,
            @Param("providerEventId") String providerEventId
    );

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param command 입력 값
     */
    void insertPaymentProviderEvent(PaymentProviderEventInsertCommand command);

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param command 입력 값
     */
    void insertAuditLog(AuditLogCommand command);
}
