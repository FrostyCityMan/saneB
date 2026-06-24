/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: LocalSafeAiAssistProvider.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.aiassist.provider;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class LocalSafeAiAssistProvider implements AiAssistProvider {

    private static final String DEFAULT_PROVIDER_CODE = "LOCAL_SAFE";
    private static final String DEFAULT_MODEL_CODE = "RULE_TEMPLATE_V1";

    private final String providerCode;
    private final String modelCode;

    /**
     * 객체를 생성합니다.
     *
     * @param environment 입력 값
     */
    public LocalSafeAiAssistProvider(Environment environment) {
        this.providerCode = environment.getProperty("saneb.ai.provider-code", DEFAULT_PROVIDER_CODE);
        this.modelCode = environment.getProperty("saneb.ai.model-code", DEFAULT_MODEL_CODE);
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param request 입력 값
     *
     * @return 처리 결과
     */
    @Override
    public AiAssistProviderResponse generate(AiAssistProviderRequest request) {
        String resultText = switch (request.assistTypeCode()) {
            case "ANNOUNCEMENT_SUMMARY" -> announcementSummaryTemplate();
            case "DOCUMENT_DRAFT" -> documentDraftTemplate();
            case "OPERATION_MEMO_SUMMARY" -> operationMemoTemplate();
            case "USER_REPLY_DRAFT" -> userReplyTemplate();
            default -> generalTemplate();
        };
        String metadataJson = "{\"providerMode\":\"localSafe\",\"inputLength\":\""
                + request.inputLength()
                + "\",\"hasOperatorNote\":\""
                + request.hasOperatorNote()
                + "\"}";
        return new AiAssistProviderResponse(
                providerCode,
                modelCode,
                resultText,
                Math.max(1, request.inputLength() / 4),
                Math.max(1, resultText.length() / 4),
                metadataJson
        );
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @return 처리 결과
     */
    private String announcementSummaryTemplate() {
        return """
                공고 요약 초안
                - 목적: 공고의 지원 목적을 한 문장으로 정리해 주세요.
                - 대상: 지원 대상, 제외 조건, 지역 조건을 확인해 주세요.
                - 혜택: 지원 금액, 지급 방식, 접수 기간을 확인해 주세요.
                - 다음 조치: 필요 서류와 진행 단계를 운영자가 최종 검토해 주세요.
                """;
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @return 처리 결과
     */
    private String documentDraftTemplate() {
        return """
                필요 서류 초안
                - 사업자 확인 서류
                - 매출 또는 소득 확인 서류
                - 국세·지방세 납부 확인 서류
                - 가족 또는 세대 확인 서류
                - 공고별 추가 서류는 운영자가 직접 확인해 주세요.
                """;
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @return 처리 결과
     */
    private String operationMemoTemplate() {
        return """
                운영 메모 요약 초안
                - 현재 상태
                - 확인해야 할 조건
                - 사용자에게 요청할 보완 사항
                - 다음 담당자 행동
                """;
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @return 처리 결과
     */
    private String userReplyTemplate() {
        return """
                사용자 답변 초안
                안녕하세요. 문의하신 내용은 확인 후 안내드리겠습니다.
                현재 단계에서 필요한 정보와 서류를 다시 점검하고 있으며, 확정된 내용만 전달드리겠습니다.
                추가 확인이 필요한 경우 별도로 연락드리겠습니다.
                """;
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @return 처리 결과
     */
    private String generalTemplate() {
        return """
                AI 보조 초안
                - 입력 원문은 저장하지 않았습니다.
                - 이 초안은 운영자 검토 전 사용자에게 확정 안내로 사용할 수 없습니다.
                - 실제 발송 또는 업무 반영 전 공고 조건과 개인정보 포함 여부를 확인해 주세요.
                """;
    }
}
