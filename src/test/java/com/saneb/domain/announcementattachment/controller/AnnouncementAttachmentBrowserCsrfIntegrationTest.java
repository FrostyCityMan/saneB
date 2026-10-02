package com.saneb.domain.announcementattachment.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.domain.announcementattachment.dto.*;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentPolicyService;
import java.time.OffsetDateTime;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// csrf()는 공유 FilterChain의 저장소를 테스트용으로 교체하므로 별도 context에서 실제 쿠키 계약을 검사한다.
@SpringBootTest(properties={"spring.flyway.enabled=false","saneb.test.browser-csrf=true"})
@AutoConfigureMockMvc
class AnnouncementAttachmentBrowserCsrfIntegrationTest {
    private static final String ROOT="/api/v2/admin/announcement-attachment-policies";
    private static final UUID ID=UUID.randomUUID(),RULE=UUID.randomUUID(),KEY=UUID.randomUUID();
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @MockitoBean AnnouncementAttachmentPolicyService service;
    @Test void browserCookieAndRawHeaderCanCreateDraftWithoutCsrfTestPostProcessor() throws Exception {
        var page=mvc.perform(get("/login")).andExpect(status().isOk()).andReturn();
        var cookie=page.getResponse().getCookie("XSRF-TOKEN");
        org.junit.jupiter.api.Assertions.assertNotNull(cookie);
        when(service.insertPolicy(any(),eq(KEY),any())).thenReturn(details());
        mvc.perform(post(ROOT).with(user("qa").roles("ADMIN"))
                .cookie(cookie,new jakarta.servlet.http.Cookie("JSESSIONID","synthetic-session"))
                .header("X-XSRF-TOKEN",cookie.getValue()).header("Idempotency-Key",KEY)
                .contentType(MediaType.APPLICATION_JSON).content(create()))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.policy.policyStatusCode").value("DRAFT"));
        verify(service).insertPolicy(any(),eq(KEY),any());
    }
    @ParameterizedTest @ValueSource(strings={"missing-header","wrong-header","missing-cookie","masked-header"})
    void browserInvalidCookieHeaderNeverReachesPolicyService(String variant) throws Exception {
        var page=mvc.perform(get("/login")).andExpect(status().isOk()).andReturn();
        var cookie=page.getResponse().getCookie("XSRF-TOKEN");
        org.junit.jupiter.api.Assertions.assertNotNull(cookie);
        var request=post(ROOT).with(user("qa").roles("ADMIN"))
                .cookie(new jakarta.servlet.http.Cookie("JSESSIONID","synthetic-session"))
                .header("Idempotency-Key",KEY).contentType(MediaType.APPLICATION_JSON).content(create());
        if(!variant.equals("missing-cookie")) request.cookie(cookie);
        if(!variant.equals("missing-header")) {
            var masked=(org.springframework.security.web.csrf.CsrfToken)page.getRequest().getAttribute("_csrf");
            request.header("X-XSRF-TOKEN",variant.equals("wrong-header")?"invalid-test-value":
                    variant.equals("masked-header")?masked.getToken():cookie.getValue());
        }
        mvc.perform(request).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
    @Test void validBrowserCsrfDoesNotGrantNonAdminMutationPermission() throws Exception {
        var cookie=mvc.perform(get("/login")).andReturn().getResponse().getCookie("XSRF-TOKEN");
        org.junit.jupiter.api.Assertions.assertNotNull(cookie);
        mvc.perform(post(ROOT).with(user("qa").roles("OPERATOR"))
                .cookie(cookie,new jakarta.servlet.http.Cookie("JSESSIONID","synthetic-session"))
                .header("X-XSRF-TOKEN",cookie.getValue()).header("Idempotency-Key",KEY)
                .contentType(MediaType.APPLICATION_JSON).content(create())).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
    @Test void maskedFormLogoutStillWorksAndNextPageIssuesFreshCookie() throws Exception {
        var page=mvc.perform(get("/login")).andExpect(status().isOk()).andReturn();
        var cookie=page.getResponse().getCookie("XSRF-TOKEN");
        org.junit.jupiter.api.Assertions.assertNotNull(cookie);
        var masked=(org.springframework.security.web.csrf.CsrfToken)page.getRequest().getAttribute("_csrf");
        org.junit.jupiter.api.Assertions.assertNotEquals(cookie.getValue(),masked.getToken());
        mvc.perform(post("/logout").with(user("qa").roles("ADMIN")).cookie(cookie)
                .param("_csrf",masked.getToken())).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login")).andExpect(cookie().maxAge("XSRF-TOKEN",0));
        var fresh=mvc.perform(get("/login")).andReturn().getResponse().getCookie("XSRF-TOKEN");
        org.junit.jupiter.api.Assertions.assertNotNull(fresh);
        org.junit.jupiter.api.Assertions.assertNotEquals(cookie.getValue(),fresh.getValue());
        verifyNoInteractions(service);
    }
    @Test void rawFormTokenCannotReplaceMaskedThymeleafFormToken() throws Exception {
        var cookie=mvc.perform(get("/login")).andReturn().getResponse().getCookie("XSRF-TOKEN");
        org.junit.jupiter.api.Assertions.assertNotNull(cookie);
        mvc.perform(post("/logout").with(user("qa").roles("ADMIN")).cookie(cookie)
                .param("_csrf",cookie.getValue())).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
    private AttachmentPolicyResponses.Summary summary() {
        return new AttachmentPolicyResponses.Summary(ID,"ATT-QA",1,0,"DRAFT","ENFORCE",RULE,"ACTIVE",null,OffsetDateTime.parse("2026-09-11T11:00:00+09:00"),null);
    }
    private AttachmentPolicyResponses.Details details() {
        return new AttachmentPolicyResponses.Details(summary(),new AttachmentPolicyResponses.Configuration("attachment-v1","1.0.0",null,83886080L),
                List.of(new AttachmentPolicyResponses.Profile("BIZINFO","BIZINFO_DETAIL_V1","a".repeat(64))),null,true,true,summary().createdAt());
    }
    private String create() throws Exception { return mapper.writeValueAsString(new AttachmentPolicyRequests.Create(RULE,"ENFORCE",83886080L,"정책 QA")); }
}
