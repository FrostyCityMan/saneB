package com.saneb.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan(basePackages = {
        "com.saneb.domain.auth.dao",
        "com.saneb.domain.dashboard.dao",
        "com.saneb.domain.announcement.dao"
})
public class MyBatisMapperConfig {
}
