/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: AuditLogServiceImpl.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.auditlog.service.impl;

import com.saneb.common.error.ApiException;
import com.saneb.common.error.ErrorCode;
import com.saneb.common.response.PageResponse;
import com.saneb.domain.auditlog.dao.AuditLogDao;
import com.saneb.domain.auditlog.dto.AuditLogDetailsResponse;
import com.saneb.domain.auditlog.dto.AuditLogSummaryResponse;
import com.saneb.domain.auditlog.service.AuditLogService;
import com.saneb.domain.auditlog.vo.AuditLogDetailsRow;
import com.saneb.domain.auditlog.vo.AuditLogSearchCondition;
import com.saneb.domain.auditlog.vo.AuditLogSummaryRow;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class AuditLogServiceImpl implements AuditLogService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> RESULT_CODES = Set.of("SUCCESS", "FAIL");
    private static final Set<String> RESOURCE_TYPES = Set.of(
            "USER",
            "PARTNER_VERIFICATION",
            "MATCHING_CASE",
            "APPLICATION_PROGRESS",
            "DOCUMENT_SUBMISSION",
            "CONSULTATION_RESERVATION",
            "SUBSCRIPTION",
            "PAYMENT_TRANSACTION",
            "REFUND_TRANSACTION",
            "NOTIFICATION_MESSAGE",
            "OPERATION_TASK",
            "REPORT_EXPORT"
    );

    private final AuditLogDao auditLogDao;

    /**
     * 객체를 생성합니다.
     *
     * @param auditLogDao 입력 값
     */
    public AuditLogServiceImpl(AuditLogDao auditLogDao) {
        this.auditLogDao = auditLogDao;
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param keyword 입력 값
     *
     * @param actionCode 입력 값
     *
     * @param resourceType 입력 값
     *
     * @param resultCode 입력 값
     *
     * @param page 입력 값
     *
     * @param size 입력 값
     *
     * @return 처리 결과
     */
    @Override
    public PageResponse<AuditLogSummaryResponse> selectAuditLogList(
            String keyword,
            String actionCode,
            String resourceType,
            String resultCode,
            int page,
            int size
    ) {
        validatePageRequest(page, size);
        String normalizedResourceType = normalizeOptionalCode(resourceType);
        String normalizedResultCode = normalizeOptionalCode(resultCode);
        validateOptionalCode("resourceType", normalizedResourceType, RESOURCE_TYPES);
        validateOptionalCode("resultCode", normalizedResultCode, RESULT_CODES);

        AuditLogSearchCondition condition = new AuditLogSearchCondition(
                trimToNull(keyword),
                trimToNull(actionCode),
                normalizedResourceType,
                normalizedResultCode,
                page,
                size,
                (page - 1) * size
        );
        long totalCount = auditLogDao.selectAuditLogCount(condition);
        List<AuditLogSummaryResponse> items = auditLogDao.selectAuditLogList(condition).stream()
                .map(this::toSummaryResponse)
                .toList();
        return PageResponse.of(items, page, size, totalCount);
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param auditLogId 입력 값
     *
     * @return 처리 결과
     */
    @Override
    public AuditLogDetailsResponse selectAuditLogDetails(UUID auditLogId) {
        AuditLogDetailsRow row = auditLogDao.selectAuditLogDetails(auditLogId);
        if (row == null) {
            throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "감사 로그를 찾을 수 없습니다.");
        }
        return toDetailsResponse(row);
    }

    /**
     * 업무 데이터를 응답 형식으로 변환합니다.
     *
     * @param row 입력 값
     *
     * @return 처리 결과
     */
    private AuditLogSummaryResponse toSummaryResponse(AuditLogSummaryRow row) {
        return new AuditLogSummaryResponse(
                row.auditLogId(),
                row.actorUserId(),
                actorDisplayName(row.actorName(), row.actorLoginId()),
                row.actionCode(),
                actionLabel(row.actionCode()),
                row.resourceType(),
                resourceLabel(row.resourceType()),
                row.resourceId(),
                row.resultCode(),
                resultLabel(row.resultCode()),
                row.createdAt()
        );
    }

    /**
     * 업무 데이터를 응답 형식으로 변환합니다.
     *
     * @param row 입력 값
     *
     * @return 처리 결과
     */
    private AuditLogDetailsResponse toDetailsResponse(AuditLogDetailsRow row) {
        return new AuditLogDetailsResponse(
                row.auditLogId(),
                row.actorUserId(),
                actorDisplayName(row.actorName(), row.actorLoginId()),
                row.actionCode(),
                actionLabel(row.actionCode()),
                row.resourceType(),
                resourceLabel(row.resourceType()),
                row.resourceId(),
                row.resultCode(),
                resultLabel(row.resultCode()),
                row.ipAddress(),
                row.userAgent(),
                row.metadataJson(),
                row.createdAt()
        );
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param actorName 입력 값
     *
     * @param actorLoginId 입력 값
     *
     * @return 처리 결과
     */
    private String actorDisplayName(String actorName, String actorLoginId) {
        if (actorName != null && !actorName.isBlank() && actorLoginId != null && !actorLoginId.isBlank()) {
            return actorName + " (" + actorLoginId + ")";
        }
        if (actorName != null && !actorName.isBlank()) {
            return actorName;
        }
        if (actorLoginId != null && !actorLoginId.isBlank()) {
            return actorLoginId;
        }
        return "시스템";
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param actionCode 입력 값
     *
     * @return 처리 결과
     */
    private String actionLabel(String actionCode) {
        return switch (actionCode) {
            case "USER_STATUS_UPDATE" -> "계정 상태 변경";
            case "USER_ROLES_UPDATE" -> "권한 변경";
            case "PARTNER_VERIFICATION_CREATE" -> "검증 생성";
            case "PARTNER_VERIFICATION_MEMBER_VALUES_SAVE" -> "회원 검증값 저장";
            case "PARTNER_VERIFICATION_BUSINESS_VALUES_SAVE" -> "사업 검증값 저장";
            case "PARTNER_VERIFICATION_FAMILY_VALUES_SAVE" -> "가족 검증값 저장";
            case "PARTNER_VERIFICATION_DOCUMENTS_SAVE" -> "검증 서류 저장";
            case "PARTNER_VERIFICATION_RESTRICTION_FLAGS_SAVE" -> "제한 항목 저장";
            case "PARTNER_VERIFICATION_SUBMIT" -> "검증 제출";
            case "PARTNER_VERIFICATION_VERIFY" -> "검증 완료";
            case "PARTNER_VERIFICATION_REJECT" -> "검증 반려";
            case "MATCHING_CASE_CREATE" -> "매칭 생성";
            case "MATCHING_CASE_STATUS_UPDATE" -> "매칭 상태 변경";
            case "APPLICATION_PROGRESS_CREATE" -> "신청 진행 생성";
            case "APPLICATION_PROGRESS_STEP_ACTION" -> "진행 단계 처리";
            case "APPLICATION_PROGRESS_DOCUMENTS_SAVE" -> "진행 서류 저장";
            case "APPLICATION_PROGRESS_RECEIPT_SAVE" -> "접수 정보 저장";
            case "APPLICATION_PROGRESS_RESULT_SAVE" -> "최종 결과 저장";
            case "APPLICATION_INPUT_VALUES_SAVE" -> "추가 입력값 저장";
            case "SUBSCRIPTION_CREATE" -> "구독 생성";
            case "SUBSCRIPTION_CANCEL" -> "구독 취소";
            case "PAYMENT_TRANSACTION_CREATE" -> "결제 요청 생성";
            case "PAYMENT_TRANSACTION_STATUS_UPDATE" -> "결제 상태 변경";
            case "PAYMENT_PROVIDER_EVENT_RECEIVE" -> "결제사 이벤트 수신";
            case "REFUND_TRANSACTION_CREATE" -> "환불 요청 생성";
            case "REFUND_TRANSACTION_STATUS_UPDATE" -> "환불 상태 변경";
            case "NOTIFICATION_MESSAGE_SEND" -> "알림 발송";
            case "NOTIFICATION_MESSAGE_READ" -> "알림 읽음";
            case "OPERATION_TASK_CREATE" -> "운영 업무 생성";
            case "OPERATION_TASK_STATUS_UPDATE" -> "운영 업무 상태 변경";
            case "OPERATION_TASK_COMMENT_CREATE" -> "운영 업무 댓글 등록";
            case "OPERATION_TASK_ASSIGNMENT_CREATE" -> "운영 업무 담당자 배정";
            case "REPORT_EXPORT_CREATE" -> "리포트 내보내기 생성";
            default -> actionCode;
        };
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param resourceType 입력 값
     *
     * @return 처리 결과
     */
    private String resourceLabel(String resourceType) {
        return switch (resourceType) {
            case "USER" -> "회원";
            case "PARTNER_VERIFICATION" -> "검증";
            case "MATCHING_CASE" -> "매칭";
            case "APPLICATION_PROGRESS" -> "신청 진행";
            case "DOCUMENT_SUBMISSION" -> "서류 제출";
            case "CONSULTATION_RESERVATION" -> "상담 예약";
            case "SUBSCRIPTION" -> "구독";
            case "PAYMENT_TRANSACTION" -> "결제";
            case "REFUND_TRANSACTION" -> "환불";
            case "NOTIFICATION_MESSAGE" -> "알림";
            case "OPERATION_TASK" -> "운영 업무";
            case "REPORT_EXPORT" -> "리포트 내보내기";
            default -> resourceType;
        };
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param resultCode 입력 값
     *
     * @return 처리 결과
     */
    private String resultLabel(String resultCode) {
        return switch (resultCode) {
            case "SUCCESS" -> "성공";
            case "FAIL" -> "실패";
            default -> resultCode;
        };
    }

    /**
     * 요청 값과 업무 규칙을 검증합니다.
     *
     * @param page 입력 값
     *
     * @param size 입력 값
     */
    private void validatePageRequest(int page, int size) {
        if (page < 1 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new ApiException(
                    ErrorCode.INVALID_PAGE_REQUEST,
                    HttpStatus.BAD_REQUEST,
                    "페이지 요청 값이 올바르지 않습니다."
            );
        }
    }

    /**
     * 요청 값과 업무 규칙을 검증합니다.
     *
     * @param fieldName 입력 값
     *
     * @param code 입력 값
     *
     * @param allowedCodes 입력 값
     */
    private void validateOptionalCode(String fieldName, String code, Set<String> allowedCodes) {
        if (code != null && !allowedCodes.contains(code)) {
            throw new ApiException(
                    ErrorCode.VALIDATION_FAILED,
                    HttpStatus.BAD_REQUEST,
                    fieldName + " 값이 올바르지 않습니다."
            );
        }
    }

    /**
     * 입력 값을 표준 형식으로 정규화합니다.
     *
     * @param value 입력 값
     *
     * @return 처리 결과
     */
    private String normalizeOptionalCode(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed.toUpperCase();
    }

    /**
     * 문자열 입력 값을 정리합니다.
     *
     * @param value 입력 값
     *
     * @return 처리 결과
     */
    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
