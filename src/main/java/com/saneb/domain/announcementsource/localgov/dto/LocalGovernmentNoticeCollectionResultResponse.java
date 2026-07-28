/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: LocalGovernmentNoticeCollectionResultResponse.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.announcementsource.localgov.dto;

import com.saneb.domain.announcementsource.localgov.vo.LocalGovernmentNoticeCollectionResultRow;
import java.time.OffsetDateTime;
import java.util.UUID;

public record LocalGovernmentNoticeCollectionResultResponse(
        UUID resultId,
        UUID runId,
        String runPublicCode,
        UUID sourceId,
        String sourcePublicCode,
        String institutionName,
        String noticeUrl,
        String resultStatusCode,
        int discoveredCount,
        int newCount,
        int duplicateCount,
        int failedCount,
        int excludedCount,
        Integer httpStatus,
        String errorCode,
        String errorMessage,
        String diagnosticReasonCode,
        String diagnosticTitle,
        String recommendedAction,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt
) {

    /**
     * URL 단위 수집 결과를 관리자 진단 응답으로 변환합니다.
     *
     * @param row URL 단위 수집 결과
     * @return 관리자 진단 응답
     */
    public static LocalGovernmentNoticeCollectionResultResponse from(LocalGovernmentNoticeCollectionResultRow row) {
        String reasonCode = selectDiagnosticReason(row);
        return new LocalGovernmentNoticeCollectionResultResponse(
                row.resultId(), row.runId(), row.runPublicCode(), row.sourceId(), row.sourcePublicCode(),
                row.institutionName(), row.noticeUrl(), row.resultStatusCode(), row.discoveredCount(),
                row.newCount(), row.duplicateCount(), row.failedCount(), row.excludedCount(), row.httpStatus(),
                row.errorCode(), row.errorMessage(), reasonCode, selectTitle(reasonCode, row.errorCode()),
                selectRecommendedAction(reasonCode, row.errorCode()), row.startedAt(), row.finishedAt()
        );
    }

    /**
     * 수집 결과를 장애·제외 사유로 분류합니다.
     *
     * @param row URL 단위 수집 결과
     * @return 진단 사유 코드
     */
    private static String selectDiagnosticReason(LocalGovernmentNoticeCollectionResultRow row) {
        if ("PARTIAL_FAILED".equals(row.resultStatusCode())
                || "REQUIRED_FIELDS_MISSING".equals(row.errorCode())
                || "JSON_REQUIRED_FIELDS_MISSING".equals(row.errorCode())) {
            return "PARTIAL_FIELDS";
        }
        if (row.errorCode() != null) {
            return switch (row.errorCode()) {
                case "RETRYABLE", "NETWORK_ERROR", "DNS_LOOKUP_FAILED", "TLS_HANDSHAKE_FAILED",
                        "CONNECTION_REFUSED", "CONNECTION_RESET", "HTTP_ERROR", "EMPTY_RESPONSE",
                        "COLLECTION_INTERRUPTED", "URL_VALIDATION_FAILED", "REDIRECT_URL_BLOCKED",
                        "TOO_MANY_REDIRECTS", "REDIRECT_LOCATION_MISSING", "ACCESS_BLOCKED" -> "TRANSPORT_FAILED";
                case "PARSER_ERROR", "PARSER_NOT_CONFIGURED", "LIST_SELECTOR_NOT_MATCHED",
                        "HEURISTIC_ITEMS_NOT_FOUND", "UNSUPPORTED_CONTENT_TYPE",
                        "JSON_PARSER_NOT_CONFIGURED", "JSON_ITEMS_NOT_FOUND", "JSON_PARSE_ERROR",
                        "DAEJEON_EMINWON_ITEMS_NOT_FOUND" -> "PARSER_FAILED";
                case "ITEM_FIELDS_MISSING", "JSON_ITEM_FIELDS_MISSING" -> "PARTIAL_FIELDS";
                case "PROCESSING_FAILED" -> "PROCESSING_FAILED";
                default -> "UNCLASSIFIED_ERROR";
            };
        }
        if (row.excludedCount() > 0) {
            return "IRRELEVANT_CONTENT";
        }
        return null;
    }

    /**
     * 진단 사유를 관리자용 제목으로 변환합니다.
     *
     * @param reasonCode 진단 사유 코드
     * @param errorCode 원본 오류 코드
     * @return 관리자용 제목
     */
    private static String selectTitle(String reasonCode, String errorCode) {
        if ("TRANSPORT_FAILED".equals(reasonCode)) {
            return switch (String.valueOf(errorCode)) {
                case "DNS_LOOKUP_FAILED" -> "기관 주소 조회 실패";
                case "TLS_HANDSHAKE_FAILED" -> "보안 연결 협상 실패";
                case "CONNECTION_REFUSED" -> "기관 연결 거부";
                case "CONNECTION_RESET" -> "기관 연결 중단";
                case "RETRYABLE" -> "기관 응답 시간 초과";
                default -> "기관 사이트 접속 실패";
            };
        }
        return switch (String.valueOf(reasonCode)) {
            case "PARSER_FAILED" -> "게시판 자료 구조 확인 필요";
            case "PARTIAL_FIELDS" -> "필수 필드 일부 누락";
            case "SEMANTIC_MISMATCH" -> "게시판 종류 불일치";
            case "IRRELEVANT_CONTENT" -> "무관 게시물 제외";
            case "PROCESSING_FAILED" -> "내부 처리 실패";
            case "UNCLASSIFIED_ERROR" -> "분류되지 않은 수집 오류";
            default -> null;
        };
    }

    /**
     * 진단 사유에 맞는 복구 지침을 반환합니다.
     *
     * @param reasonCode 진단 사유 코드
     * @param errorCode 원본 오류 코드
     * @return 권장 조치
     */
    private static String selectRecommendedAction(String reasonCode, String errorCode) {
        if ("TRANSPORT_FAILED".equals(reasonCode)) {
            return switch (String.valueOf(errorCode)) {
                case "DNS_LOOKUP_FAILED" -> "기관 공식 주소와 운영 서버 DNS 조회 상태를 확인하세요.";
                case "TLS_HANDSHAKE_FAILED" -> "기관 사이트의 TLS 지원 방식과 요청 프로필을 재검증하세요.";
                case "CONNECTION_REFUSED" -> "기관 방화벽·접근 제한과 공식 대체 URL을 확인하세요.";
                case "CONNECTION_RESET" -> "기관의 자동수집 차단 여부와 요청 간격을 확인한 뒤 재수집하세요.";
                case "RETRYABLE" -> "응답 제한시간 후 단일 URL로 다시 수집하세요.";
                default -> "기관 사이트 연결과 차단 여부를 확인한 뒤 재수집하세요.";
            };
        }
        return switch (String.valueOf(reasonCode)) {
            case "PARSER_FAILED" -> "원문 게시판 구조와 자동수집 구성을 다시 점검하세요.";
            case "PARTIAL_FIELDS" -> "제목, 등록일, 원문 링크 선택자를 확인하세요.";
            case "SEMANTIC_MISMATCH" -> "공식 고시·공고 또는 지원사업 게시판 URL로 보정하세요.";
            case "IRRELEVANT_CONTENT" -> "일치 키워드와 제외 규칙을 확인하세요.";
            case "PROCESSING_FAILED" -> "실행 상세와 서버 오류 로그를 확인한 뒤 다시 수집하세요.";
            case "UNCLASSIFIED_ERROR" -> "저장된 오류 코드와 서버 로그를 확인한 뒤 분류 규칙을 보강하세요.";
            default -> null;
        };
    }
}
