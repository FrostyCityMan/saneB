package com.saneb.domain.address.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.saneb.common.response.PageResponse;
import com.saneb.domain.address.dto.AddressSearchResponse;
import com.saneb.domain.address.service.AddressService;
import com.saneb.domain.auth.vo.AuthUserDetailsRow;
import com.saneb.domain.auth.vo.AuthenticatedUserDetails;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "spring.flyway.enabled=false")
@AutoConfigureMockMvc
class AddressControllerSmokeTest {

    private static final UUID USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000002");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AddressService addressService;

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.when(addressService.selectRoadAddressList(
                        eq("세종 도움6로 42"),
                        eq(1),
                        eq(10),
                        eq("road"),
                        eq(false)
                ))
                .thenReturn(PageResponse.of(List.of(new AddressSearchResponse(
                        "30112",
                        "세종특별자치시 도움6로 42",
                        "세종특별자치시 도움6로 42",
                        "",
                        "세종특별자치시 어진동 572",
                        "세종특별자치시",
                        "",
                        "어진동",
                        "3611010300",
                        "361103258001",
                        "3611010300105720000000001",
                        "행정안전부",
                        false
                )), 1, 10, 1));
    }

    @Test
    void selectRoadAddressListReturnsApiResponse() throws Exception {
        mockMvc.perform(get("/api/v1/addresses/road")
                        .queryParam("keyword", "세종 도움6로 42")
                        .queryParam("firstSort", "road")
                        .with(user(userPrincipal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].postalCode").value("30112"))
                .andExpect(jsonPath("$.data.items[0].roadAddress").value("세종특별자치시 도움6로 42"))
                .andExpect(jsonPath("$.data.items[0].legalDongCode").value("3611010300"));
    }

    @Test
    void selectRoadAddressListRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/addresses/road")
                        .queryParam("keyword", "세종 도움6로 42"))
                .andExpect(status().isUnauthorized());
    }

    private static AuthenticatedUserDetails userPrincipal() {
        return new AuthenticatedUserDetails(
                new AuthUserDetailsRow(USER_ID, "user01", "{noop}pw", "사용자", "ACTIVE", false, null, null, null),
                List.of("USER")
        );
    }
}
