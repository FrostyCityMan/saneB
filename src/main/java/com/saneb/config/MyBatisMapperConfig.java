package com.saneb.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan(basePackages = {
        "com.saneb.domain.auth.dao",
        "com.saneb.domain.dashboard.dao",
        "com.saneb.domain.announcement.dao",
        "com.saneb.domain.partnerverification.dao",
        "com.saneb.domain.matching.dao",
        "com.saneb.domain.applicationprogress.dao",
        "com.saneb.domain.dynamicinput.dao",
        "com.saneb.domain.admindashboard.dao",
        "com.saneb.domain.operatordashboard.dao",
        "com.saneb.domain.approverreview.dao",
        "com.saneb.domain.adminuser.dao",
        "com.saneb.domain.auditlog.dao",
        "com.saneb.domain.consent.dao",
        "com.saneb.domain.documentfile.dao",
        "com.saneb.domain.consultation.dao",
        "com.saneb.domain.billing.dao",
        "com.saneb.domain.operation.dao",
        "com.saneb.domain.adminreport.dao",
        "com.saneb.domain.aiassist.dao"
})
public class MyBatisMapperConfig {
}
