/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: AnnouncementSourceHighlightService.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.announcementsource.service;

import com.saneb.domain.announcementsource.vo.AnnouncementSourceHighlightCommand;
import java.util.List;
import java.util.UUID;

public interface AnnouncementSourceHighlightService {

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
    List<AnnouncementSourceHighlightCommand> selectHighlightList(
            UUID sourceId,
            String bodyText,
            String inquiryText,
            String applicationMethodText
    );
}
