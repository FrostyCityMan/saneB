/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: AnnouncementSourceHighlightServiceImplTest.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.announcementsource.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.saneb.domain.announcementsource.vo.AnnouncementSourceHighlightCommand;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AnnouncementSourceHighlightServiceImplTest {

    private final AnnouncementSourceHighlightServiceImpl service = new AnnouncementSourceHighlightServiceImpl();

    /**
     * 업무 데이터를 조회합니다.
     */
    @Test
    void selectHighlightListReturnsReferenceOnlyHighlights() {
        UUID sourceId = UUID.randomUUID();

        List<AnnouncementSourceHighlightCommand> highlights = service.selectHighlightList(
                sourceId,
                """
                        지원대상: 서울 소재 소상공인
                        매출 조건: 연매출 3억원 이하
                        필요서류: 사업자등록증, 부가세과세표준증명
                        """,
                "문의처: 서울시",
                "신청방법: 온라인 접수"
        );

        assertThat(highlights)
                .extracting(AnnouncementSourceHighlightCommand::highlightTypeCode)
                .contains("TARGET", "SALES_CONDITION", "REQUIRED_DOCUMENT", "INQUIRY", "APPLICATION_METHOD");
        assertThat(highlights)
                .allSatisfy(highlight -> assertThat(highlight.sourceId()).isEqualTo(sourceId));
    }
}
