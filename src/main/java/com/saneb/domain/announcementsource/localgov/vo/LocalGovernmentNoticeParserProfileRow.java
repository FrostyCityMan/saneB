/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: LocalGovernmentNoticeParserProfileRow.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.announcementsource.localgov.vo;

public record LocalGovernmentNoticeParserProfileRow(
        String profileCode,
        String profileName,
        String parserTypeCode,
        String listItemSelector,
        String titleSelector,
        String dateSelector,
        String linkSelector,
        String datePattern,
        String responseTypeCode,
        String jsonItemsPath,
        String jsonTitleField,
        String jsonDateField,
        String jsonLinkField,
        String jsonLinkTemplate,
        String linkStrategyCode,
        String linkFunctionName,
        Integer linkFunctionArgumentCount,
        String linkUrlTemplate,
        boolean enabled
) {
}
