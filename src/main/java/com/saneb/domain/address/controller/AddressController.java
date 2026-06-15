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
