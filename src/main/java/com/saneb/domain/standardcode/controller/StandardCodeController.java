package com.saneb.domain.standardcode.controller;

import com.saneb.common.response.ApiResponse;
import com.saneb.common.response.PageResponse;
import com.saneb.domain.standardcode.dto.StandardCodeGroupResponse;
import com.saneb.domain.standardcode.dto.StandardCodeResponse;
import com.saneb.domain.standardcode.service.StandardCodeService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1")
public class StandardCodeController {

    private final StandardCodeService standardCodeService;

    public StandardCodeController(StandardCodeService standardCodeService) {
        this.standardCodeService = standardCodeService;
    }

    @GetMapping("/standard-code-groups")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<StandardCodeGroupResponse>> selectStandardCodeGroupList() {
        return ApiResponse.success(standardCodeService.selectStandardCodeGroupList());
    }

    @GetMapping("/standard-codes")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageResponse<StandardCodeResponse>> selectStandardCodeList(
            @RequestParam String groupCode,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "true") Boolean active,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ApiResponse.success(standardCodeService.selectStandardCodeList(
                groupCode,
                keyword,
                active,
                page,
                size
        ));
    }
}
