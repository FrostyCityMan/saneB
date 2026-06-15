package com.saneb.domain.auth.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "spring.flyway.enabled=false")
@AutoConfigureMockMvc
class AuthViewControllerSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void selectLoginPageReturnsThymeleafView() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"))
                .andExpect(content().string(containsString("data-login-form")))
                .andExpect(content().string(containsString("/api/v1/auth/login")))
                .andExpect(content().string(containsString("/signup")))
                .andExpect(content().string(not(containsString("th:utext"))));
    }

    @Test
    void selectSignupPageReturnsThymeleafView() throws Exception {
        mockMvc.perform(get("/signup"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/signup"))
                .andExpect(content().string(containsString("data-signup-form")))
                .andExpect(content().string(containsString("/api/v1/auth/signup")))
                .andExpect(content().string(not(containsString("th:utext"))));
    }

    @Test
    void selectInvalidAccessPageReturnsLoginReturnAction() throws Exception {
        mockMvc.perform(get("/invalid-access")
                        .queryParam("reason", "auth"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/invalid-access"))
                .andExpect(content().string(containsString("잘못된 접근")))
                .andExpect(content().string(containsString("로그인 화면으로 돌아가기")))
                .andExpect(content().string(containsString("/login")))
                .andExpect(content().string(not(containsString("th:utext"))));
    }

    @Test
    @WithMockUser(username = "user01", roles = "USER")
    void forbiddenBrowserPageRedirectsToInvalidAccessPage() throws Exception {
        mockMvc.perform(get("/app/admin/users")
                        .accept(org.springframework.http.MediaType.TEXT_HTML))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/invalid-access?reason=forbidden"))
                .andExpect(header().string("Location", "/invalid-access?reason=forbidden"));
    }

    @Test
    @WithMockUser(username = "user01", roles = "USER")
    void selectLoginPageRedirectsAuthenticatedUserToDefaultRoute() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/app/dashboard"))
                .andExpect(header().string("Location", "/app/dashboard"));
    }

    @Test
    @WithMockUser(username = "user01", roles = "USER")
    void selectSignupPageRedirectsAuthenticatedUserToDefaultRoute() throws Exception {
        mockMvc.perform(get("/signup"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/app/dashboard"))
                .andExpect(header().string("Location", "/app/dashboard"));
    }

    @Test
    @WithMockUser(username = "admin01", roles = "ADMIN")
    void selectLoginPageRedirectsAdminUserToAdminDashboardRoute() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/app/admin/dashboard"))
                .andExpect(header().string("Location", "/app/admin/dashboard"));
    }

    @Test
    @WithMockUser(username = "operator01", roles = "OPERATOR")
    void selectLoginPageRedirectsOperatorUserToOperatorDashboardRoute() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/app/operator/dashboard"))
                .andExpect(header().string("Location", "/app/operator/dashboard"));
    }

    @Test
    @WithMockUser(username = "approver01", roles = "APPROVER")
    void selectLoginPageRedirectsApproverUserToApproverReviewRoute() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/app/approver/reviews"))
                .andExpect(header().string("Location", "/app/approver/reviews"));
    }

    @Test
    @WithMockUser(username = "user01", roles = "USER")
    void selectPasswordPageReturnsThymeleafView() throws Exception {
        mockMvc.perform(get("/password"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/password"))
                .andExpect(content().string(containsString("data-password-form")))
                .andExpect(content().string(containsString("/api/v1/auth/password")))
                .andExpect(content().string(not(containsString("th:utext"))));
    }

    @Test
    @WithMockUser(username = "user01", roles = "USER")
    void logoutRedirectsAuthenticatedUserToLoginPage() throws Exception {
        mockMvc.perform(post("/logout")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(header().string("Location", "/login"));
    }

    @Test
    @WithMockUser(username = "user01", roles = "USER")
    void logoutWithoutCsrfIsForbidden() throws Exception {
        mockMvc.perform(post("/logout"))
                .andExpect(status().isForbidden());
    }
}
