/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: SaneBApplication.java
 * 작성자: 김도훈
 *
 */

package com.saneb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class SaneBApplication {

    /**
     * 애플리케이션을 시작합니다.
     *
     * @param args 입력 값
     */
    public static void main(String[] args) {
        SpringApplication.run(SaneBApplication.class, args);
    }
}
