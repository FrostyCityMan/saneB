package com.saneb.domain.member.controller;

import com.saneb.common.response.ApiResponse;
import com.saneb.domain.member.dto.MemberBasicInfoResponse;
import com.saneb.domain.member.dto.MemberBasicInfoSaveRequest;
import com.saneb.domain.member.service.MemberBasicInfoService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/member/basic-info")
public class MemberBasicInfoController {

    private final MemberBasicInfoService memberBasicInfoService;

    public MemberBasicInfoController(MemberBasicInfoService memberBasicInfoService) {
        this.memberBasicInfoService = memberBasicInfoService;
    }

    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<MemberBasicInfoResponse> selectMyBasicInfo(Authentication authentication) {
        return ApiResponse.success(memberBasicInfoService.selectMyBasicInfo(authentication));
    }

    @PutMapping
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<MemberBasicInfoResponse> saveMyBasicInfo(
            Authentication authentication,
            @Valid @RequestBody MemberBasicInfoSaveRequest request
    ) {
        return ApiResponse.success(memberBasicInfoService.saveMyBasicInfo(authentication, request));
    }
}
