package com.saneb.domain.member.controller;

import com.saneb.common.response.ApiResponse;
import com.saneb.domain.member.dto.MemberBasicInfoResponse;
import com.saneb.domain.member.dto.MemberBasicInfoSaveRequest;
import com.saneb.domain.member.service.MemberBasicInfoService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/admin/member-basic-info")
public class AdminMemberBasicInfoController {

    private final MemberBasicInfoService memberBasicInfoService;

    public AdminMemberBasicInfoController(MemberBasicInfoService memberBasicInfoService) {
        this.memberBasicInfoService = memberBasicInfoService;
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<MemberBasicInfoResponse> selectMemberBasicInfo(
            Authentication authentication,
            @PathVariable UUID userId
    ) {
        return ApiResponse.success(memberBasicInfoService.selectMemberBasicInfo(authentication, userId));
    }

    @PutMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<MemberBasicInfoResponse> saveMemberBasicInfo(
            Authentication authentication,
            @PathVariable UUID userId,
            @Valid @RequestBody MemberBasicInfoSaveRequest request
    ) {
        return ApiResponse.success(memberBasicInfoService.saveMemberBasicInfo(authentication, userId, request));
    }
}
