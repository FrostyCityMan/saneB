/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: AnnouncementSourceHighlightServiceImpl.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.announcementsource.service.impl;

import com.saneb.domain.announcementsource.service.AnnouncementSourceHighlightService;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceHighlightCommand;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AnnouncementSourceHighlightServiceImpl implements AnnouncementSourceHighlightService {

    private static final int MAX_MATCHED_TEXT_LENGTH = 1000;
    private static final Map<String, List<String>> KEYWORDS = Map.ofEntries(
            Map.entry("TARGET", List.of("지원대상", "지원 대상", "신청대상", "신청 대상", "대상")),
            Map.entry("SUPPORT_CONTENT", List.of("지원내용", "지원 내용", "지원금", "지원규모")),
            Map.entry("APPLICATION_PERIOD", List.of("신청기간", "접수기간", "신청 기간", "접수 기간")),
            Map.entry("APPLICATION_METHOD", List.of("신청방법", "접수방법", "신청 방법", "접수 방법")),
            Map.entry("EXCLUDED_TARGET", List.of("제외대상", "지원제외", "신청 제외", "제외 조건")),
            Map.entry("PREFERRED_CONDITION", List.of("우대조건", "우대 사항", "가점")),
            Map.entry("BUSINESS_AGE_CONDITION", List.of("업력", "창업 후", "개업일")),
            Map.entry("SALES_CONDITION", List.of("매출", "수입금액", "공급가액")),
            Map.entry("INDUSTRY_CONDITION", List.of("업종", "업태", "종목", "표준산업분류")),
            Map.entry("REGION_CONDITION", List.of("지역", "소재지", "주소", "사업장")),
            Map.entry("INCOME_CONDITION", List.of("소득", "가구소득", "중위소득")),
            Map.entry("ASSET_CONDITION", List.of("재산", "자산")),
            Map.entry("HEALTH_INSURANCE_CONDITION", List.of("건강보험료", "건보료", "건강보험")),
            Map.entry("REQUIRED_DOCUMENT", List.of("필요서류", "제출서류", "구비서류", "첨부서류")),
            Map.entry("INQUIRY", List.of("문의처", "문의", "담당자"))
    );

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param sourceId 입력 값
     *
     * @param bodyText 입력 값
     *
     * @param inquiryText 입력 값
     *
     * @param applicationMethodText 입력 값
     *
     * @return 처리 결과
     */
    @Override
    public List<AnnouncementSourceHighlightCommand> selectHighlightList(
            UUID sourceId,
            String bodyText,
            String inquiryText,
            String applicationMethodText
    ) {
        String text = String.join("\n",
                nullToBlank(bodyText),
                nullToBlank(applicationMethodText),
                nullToBlank(inquiryText)
        );
        if (text.isBlank()) {
            return List.of();
        }

        List<AnnouncementSourceHighlightCommand> highlights = new ArrayList<>();
        String[] lines = text.split("\\R");
        int offset = 0;
        for (int index = 0; index < lines.length; index++) {
            String line = lines[index].trim();
            if (!line.isBlank()) {
                appendLineHighlights(sourceId, highlights, line, index + 1, offset);
            }
            offset += lines[index].length() + 1;
        }
        return highlights;
    }

    /**
     * 하이라이트 후보를 추가합니다.
     *
     * @param sourceId 입력 값
     *
     * @param highlights 입력 값
     *
     * @param line 입력 값
     *
     * @param lineNo 입력 값
     *
     * @param lineOffset 입력 값
     */
    private void appendLineHighlights(
            UUID sourceId,
            List<AnnouncementSourceHighlightCommand> highlights,
            String line,
            int lineNo,
            int lineOffset
    ) {
        String normalized = line.replaceAll("\\s+", "");
        for (Map.Entry<String, List<String>> entry : KEYWORDS.entrySet()) {
            boolean matched = entry.getValue().stream()
                    .map(keyword -> keyword.replaceAll("\\s+", ""))
                    .anyMatch(normalized::contains);
            if (!matched || alreadyAdded(highlights, entry.getKey(), lineNo)) {
                continue;
            }
            int startOffset = Math.max(0, lineOffset);
            int endOffset = startOffset + line.length();
            highlights.add(new AnnouncementSourceHighlightCommand(
                    UUID.randomUUID(),
                    sourceId,
                    entry.getKey(),
                    truncate(line),
                    startOffset,
                    endOffset,
                    lineNo,
                    selectRuleCode(line)
            ));
        }
    }

    /**
     * 중복 하이라이트 여부를 확인합니다.
     *
     * @param highlights 입력 값
     *
     * @param typeCode 입력 값
     *
     * @param lineNo 입력 값
     *
     * @return 처리 결과
     */
    private boolean alreadyAdded(List<AnnouncementSourceHighlightCommand> highlights, String typeCode, int lineNo) {
        return highlights.stream()
                .anyMatch(highlight -> highlight.highlightTypeCode().equals(typeCode) && lineNo == highlight.lineNo());
    }

    /**
     * 규칙 코드를 선택합니다.
     *
     * @param line 입력 값
     *
     * @return 처리 결과
     */
    private String selectRuleCode(String line) {
        return line.contains(":") || line.contains("：") || line.contains("■") || line.contains("[")
                ? "RULE_HEADING"
                : "RULE_KEYWORD";
    }

    /**
     * 값을 제한 길이로 자릅니다.
     *
     * @param value 입력 값
     *
     * @return 처리 결과
     */
    private String truncate(String value) {
        if (value == null || value.length() <= MAX_MATCHED_TEXT_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_MATCHED_TEXT_LENGTH);
    }

    /**
     * null 값을 빈 문자열로 변환합니다.
     *
     * @param value 입력 값
     *
     * @return 처리 결과
     */
    private String nullToBlank(String value) {
        return value == null ? "" : value;
    }
}
