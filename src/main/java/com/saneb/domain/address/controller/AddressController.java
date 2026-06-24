/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: AddressController.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.address.controller;

import com.saneb.common.response.ApiResponse;
import com.saneb.common.response.PageResponse;
import com.saneb.domain.address.dto.AddressSearchResponse;
import com.saneb.domain.address.service.AddressService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/addresses")
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping("/road")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageResponse<AddressSearchResponse>> selectRoadAddressList(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "none") String firstSort,
            @RequestParam(defaultValue = "false") Boolean includeHistory
    ) {
        return ApiResponse.success(addressService.selectRoadAddressList(keyword, page, size, firstSort, includeHistory));
    }
}
