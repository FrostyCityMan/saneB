/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: LocalGovernmentNoticeSourceResponse.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.announcementsource.localgov.dto;

import com.saneb.domain.announcementsource.localgov.vo.LocalGovernmentNoticeSourceRow;
import java.time.OffsetDateTime;
import java.util.UUID;

public record LocalGovernmentNoticeSourceResponse(
        UUID sourceId,
        String publicCode,
        String sidoCode,
        String sidoName,
        String sigunguCode,
        String sigunguName,
        String institutionTypeCode,
        String institutionName,
        String homepageUrl,
        String noticeUrl,
        String collectionEndpointUrl,
        String pageTypeCode,
        String requestProfileCode,
        String requestMethodCode,
        String parserProfileCode,
        String parserProfileName,
        String collectionHint,
        String confidenceCode,
        String validationStatusCode,
        String sourceBoardTypeCode,
        String collectionPolicyCode,
        boolean semanticallyVerified,
        OffsetDateTime semanticVerifiedAt,
        String semanticVerificationNote,
        boolean enabled,
        String collectionStatusCode,
        String trafficLightCode,
        String diagnosticReasonCode,
        String diagnosticTitle,
        String recommendedAction,
        OffsetDateTime lastCollectedAt,
        OffsetDateTime lastSuccessAt,
        Integer lastHttpStatus,
        String lastErrorCode,
        String lastErrorMessage,
        OffsetDateTime updatedAt
) {

    /**
     * DB 조회 결과를 관리자 응답으로 변환합니다.
     *
     * @param row 지자체 공고 URL 조회 결과
     * @return 관리자 응답
     */
    public static LocalGovernmentNoticeSourceResponse from(LocalGovernmentNoticeSourceRow row) {
        return new LocalGovernmentNoticeSourceResponse(
                row.sourceId(), row.publicCode(), row.sidoCode(), row.sidoName(), row.sigunguCode(), row.sigunguName(),
                row.institutionTypeCode(), row.institutionName(), row.homepageUrl(), row.noticeUrl(),
                row.collectionEndpointUrl(), row.pageTypeCode(),
                row.requestProfileCode(), row.requestMethodCode(), row.parserProfileCode(), row.parserProfileName(),
                row.collectionHint(), row.confidenceCode(),
                row.validationStatusCode(), row.sourceBoardTypeCode(), row.collectionPolicyCode(),
                row.semanticallyVerified(), row.semanticVerifiedAt(), row.semanticVerificationNote(),
                row.enabled(), row.collectionStatusCode(), trafficLight(row),
                diagnosticReason(row), diagnosticTitle(row), recommendedAction(row),
                row.lastCollectedAt(), row.lastSuccessAt(), row.lastHttpStatus(), row.lastErrorCode(),
                row.lastErrorMessage(), row.updatedAt()
        );
    }

    /**
     * 운영 상태를 관리자 신호등 코드로 변환합니다.
     *
     * @param row 지자체 공고 URL 조회 결과
     * @return RED, YELLOW 또는 GREEN
     */
    private static String trafficLight(LocalGovernmentNoticeSourceRow row) {
        String reasonCode = diagnosticReason(row);
        if (reasonCode != null && !reasonCode.equals("IRRELEVANT_CONTENT")) {
            return "RED";
        }
        if (row.lastErrorCode() != null || switch (row.collectionStatusCode()) {
            case "FAILED", "URL_ERROR", "ACCESS_BLOCKED", "PARSER_UNSUPPORTED" -> true;
            default -> false;
        }) {
            return "RED";
        }
        if (!row.enabled() || "CHECK_REQUIRED".equals(row.collectionStatusCode())
                || "CHECK_REQUIRED".equals(row.validationStatusCode())) {
            return "YELLOW";
        }
        return "GREEN";
    }

    /**
     * 수집 상태와 최신 URL 실행 결과를 운영 진단 사유로 변환합니다.
     *
     * @param row 지자체 공고 URL 조회 결과
     * @return 진단 사유 코드
     */
    private static String diagnosticReason(LocalGovernmentNoticeSourceRow row) {
        if (!row.semanticallyVerified()
                || "UNVERIFIED".equals(row.sourceBoardTypeCode())
                || "PRESS_RELEASE".equals(row.sourceBoardTypeCode())
                || "EXCLUDED".equals(row.collectionPolicyCode())) {
            return "SEMANTIC_MISMATCH";
        }
        if ("PARTIAL_FAILED".equals(row.latestResultStatusCode())
                || "REQUIRED_FIELDS_MISSING".equals(row.lastErrorCode())
                || "JSON_REQUIRED_FIELDS_MISSING".equals(row.lastErrorCode())) {
            return "PARTIAL_FIELDS";
        }
        if (row.lastErrorCode() != null) {
            return switch (row.lastErrorCode()) {
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
        if (!automaticCollectionReady(row)) {
            return "PARSER_FAILED";
        }
        if (row.latestResultExcludedCount() != null && row.latestResultExcludedCount() > 0) {
            return "IRRELEVANT_CONTENT";
        }
        return null;
    }

    /**
     * 진단 사유를 관리자용 제목으로 변환합니다.
     *
     * @param row 지자체 공고 URL 조회 결과
     * @return 관리자용 진단 제목
     */
    private static String diagnosticTitle(LocalGovernmentNoticeSourceRow row) {
        if ("TRANSPORT_FAILED".equals(diagnosticReason(row))) {
            return switch (String.valueOf(row.lastErrorCode())) {
                case "DNS_LOOKUP_FAILED" -> "기관 사이트 주소를 조회하지 못했습니다.";
                case "TLS_HANDSHAKE_FAILED" -> "기관 사이트와 보안 연결을 협상하지 못했습니다.";
                case "CONNECTION_REFUSED" -> "기관 사이트가 연결을 거부했습니다.";
                case "CONNECTION_RESET" -> "기관 사이트가 연결을 중단했습니다.";
                case "RETRYABLE" -> "기관 사이트 응답 시간이 초과되었습니다.";
                default -> "기관 사이트에 연결하지 못했습니다.";
            };
        }
        return switch (String.valueOf(diagnosticReason(row))) {
            case "PARSER_FAILED" -> automaticCollectionReady(row)
                    ? "게시판 구조를 읽지 못했습니다."
                    : "자동수집 준비가 완료되지 않았습니다.";
            case "PARTIAL_FIELDS" -> "일부 공고의 제목·등록일·링크가 누락되었습니다.";
            case "SEMANTIC_MISMATCH" -> "고시·공고 또는 지원사업 게시판으로 확인되지 않았습니다.";
            case "IRRELEVANT_CONTENT" -> "지원사업과 무관한 게시물을 제외했습니다.";
            case "PROCESSING_FAILED" -> "공고 수집 내부 처리에 실패했습니다.";
            case "UNCLASSIFIED_ERROR" -> "분류되지 않은 수집 오류가 발생했습니다.";
            default -> null;
        };
    }

    /**
     * 진단 사유에 맞는 관리자 조치 방법을 반환합니다.
     *
     * @param row 지자체 공고 URL 조회 결과
     * @return 권장 조치
     */
    private static String recommendedAction(LocalGovernmentNoticeSourceRow row) {
        if ("TRANSPORT_FAILED".equals(diagnosticReason(row))) {
            return switch (String.valueOf(row.lastErrorCode())) {
                case "DNS_LOOKUP_FAILED" -> "기관 공식 주소와 운영 서버 DNS 조회 상태를 확인하세요.";
                case "TLS_HANDSHAKE_FAILED" -> "기관 사이트의 TLS 지원 방식과 요청 프로필을 재검증하세요.";
                case "CONNECTION_REFUSED" -> "기관 방화벽·접근 제한과 공식 대체 URL을 확인하세요.";
                case "CONNECTION_RESET" -> "기관의 자동수집 차단 여부와 요청 간격을 확인한 뒤 재수집하세요.";
                case "RETRYABLE" -> "응답 제한시간 후 단일 URL로 다시 수집하세요.";
                default -> "기관 사이트 접속 상태와 차단 여부를 확인한 뒤 다시 수집하세요.";
            };
        }
        return switch (String.valueOf(diagnosticReason(row))) {
            case "PARSER_FAILED" -> automaticCollectionReady(row)
                    ? "원문 게시판 구조를 확인하고 자동수집 구성을 점검하세요."
                    : "시스템 QA가 완료되면 자동수집이 활성화됩니다.";
            case "PARTIAL_FIELDS" -> "누락된 행의 제목, 등록일, 원문 링크 선택자를 확인하세요.";
            case "SEMANTIC_MISMATCH" -> "공식 고시·공고 또는 지원사업 URL로 보정하고 의미 검증을 완료하세요.";
            case "IRRELEVANT_CONTENT" -> "정적 키워드 판정 결과를 확인하고 필요한 경우 키워드 규칙을 보정하세요.";
            case "PROCESSING_FAILED" -> "실행 상세와 서버 오류 로그를 확인한 뒤 다시 수집하세요.";
            case "UNCLASSIFIED_ERROR" -> "저장된 오류 코드와 서버 로그를 확인한 뒤 분류 규칙을 보강하세요.";
            default -> null;
        };
    }

    /**
     * 출처에 실행 가능한 시스템 파서가 배정됐는지 확인합니다.
     *
     * @param row 지자체 공고 URL 조회 결과
     * @return 자동수집 준비 여부
     */
    private static boolean automaticCollectionReady(LocalGovernmentNoticeSourceRow row) {
        return row.parserProfileCode() != null
                && !row.parserProfileCode().isBlank()
                && !"MANUAL_ONLY".equals(row.parserProfileCode());
    }
}
