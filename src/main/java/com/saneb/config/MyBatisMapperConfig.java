/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: MyBatisMapperConfig.java
 * 작성자: 김도훈
 *
 */

package com.saneb.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan(basePackages = {
        "com.saneb.domain.auth.dao",
        "com.saneb.domain.dashboard.dao",
        "com.saneb.domain.announcement.dao",
        "com.saneb.domain.member.dao",
        "com.saneb.domain.partnerverification.dao",
        "com.saneb.domain.matching.dao",
        "com.saneb.domain.applicationprogress.dao",
        "com.saneb.domain.dynamicinput.dao",
        "com.saneb.domain.admindashboard.dao",
        "com.saneb.domain.operatordashboard.dao",
        "com.saneb.domain.approverreview.dao",
        "com.saneb.domain.adminuser.dao",
        "com.saneb.domain.auditlog.dao",
        "com.saneb.domain.candidatepreview.dao",
        "com.saneb.domain.consent.dao",
        "com.saneb.domain.documentfile.dao",
        "com.saneb.domain.consultation.dao",
        "com.saneb.domain.billing.dao",
        "com.saneb.domain.operation.dao",
        "com.saneb.domain.adminreport.dao",
        "com.saneb.domain.aiassist.dao",
        "com.saneb.domain.standardcode.dao"
})
public class MyBatisMapperConfig {
}
