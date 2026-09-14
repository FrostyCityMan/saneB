package com.saneb.domain.announcementattachment.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties="spring.flyway.enabled=false")
@AutoConfigureMockMvc
class AnnouncementAttachmentPolicyViewControllerSmokeTest {
    private static final String PATH="/app/admin/announcement-attachment-policies";
    @Autowired MockMvc mvc;
    @ParameterizedTest @ValueSource(strings={"ADMIN","OPERATOR","APPROVER"})
    void workspaceSeparatesDraftQaAndPublicationWithNoStore(String role) throws Exception {
        var response=mvc.perform(get(PATH).with(user("policy-view-qa").roles(role))).andExpect(status().isOk())
                .andExpect(view().name("app/announcement-attachment-policies")).andExpect(header().string("Cache-Control","no-store")).andReturn().getResponse();
        var html=Jsoup.parse(response.getContentAsString(StandardCharsets.UTF_8));
        assertThat(html.selectFirst("[data-attachment-policies]").attr("data-is-admin")).isEqualTo(String.valueOf(role.equals("ADMIN")));
        assertThat(html.select("[data-editor-fields][disabled], [data-approval-fields][disabled], [data-save][disabled], [data-qa][disabled]")).hasSize(4);
        assertThat(html.selectFirst("input[name=acknowledged]").hasAttr("checked")).isFalse();
        assertThat(html.selectFirst("input[name=acknowledged]").hasAttr("required")).isTrue();
        assertThat(html.selectFirst("textarea#policy-reason").attr("aria-describedby")).contains("policy-error");
        assertThat(html.selectFirst("input[name=maximumSourceBytes]").attr("max")).isEqualTo("83886080");
        assertThat(html.select("script:not([src])")).isEmpty();
        assertThat(html.select("script[src='/js/saneb-attachment-policy-core.js'],script[src='/js/saneb-attachment-policies.js']")).hasSize(2);
        assertThat(html.select("link[href='/css/saneb-announcement-attachment-review.css']")).hasSize(1);
        assertThat(html.select("input[name=profile], select[name=profile], input[name=passed], [data-publication]")).isEmpty();
        assertThat(html.text()).contains("실제 게시는 서로 다른 단계", "운영에 활성화되지 않습니다", "원래 요청 그대로 재확인", "전체 수집 방식·실파일", "파서·수집 방식은 시스템이 고정", "불변 게시 영수증", "누락된 QA를 관리자 확인으로 대체할 수 없으며");
        assertThat(html.select("[data-prepare][disabled], [data-publish][disabled], [data-publication-consents][hidden][disabled]")).hasSize(3);
        assertThat(html.select("[data-publication-consents] input[required]")).hasSize(3);
        assertThat(html.select("[data-publication-consents] input[checked]")).isEmpty();
        assertThat(html.selectFirst("[data-publish]").attr("aria-describedby")).isEqualTo("policy-publication-condition");
        assertThat(html.selectFirst("[data-error]").attr("role")).isEqualTo("alert");
        assertThat(html.select("a[href='"+PATH+"'][aria-current=page]")).hasSize(1);
        if ("true".equals(System.getenv("SANEB_ATTACHMENT_POLICY_VIEW_EXPORT")) && "ADMIN".equals(role)) {
            html.select("meta[name=_csrf], meta[name=_csrf_header], input[name=_csrf], link[href^=http]").remove();
            html.select("script").stream().filter(e -> !java.util.Set.of("/js/saneb-attachment-policy-core.js",
                    "/js/saneb-attachment-policies.js", "/js/saneb-layout.js").contains(e.attr("src"))).forEach(org.jsoup.nodes.Element::remove);
            html.selectFirst("[data-attachment-policies]").prependElement("p").attr("role", "note")
                    .text("합성 QA 전용 화면입니다. 운영 DB·인증·정책·수집원과 연결하지 않습니다.");
            var target=Path.of("build", "attachment-policy-ui-qa", "index.html");
            Files.createDirectories(target.getParent());
            Files.writeString(target,html.outerHtml(),StandardCharsets.UTF_8);
        }
    }
    @ParameterizedTest @ValueSource(strings={"USER","PARTNER","REVIEWER"})
    void externalRolesCannotReadPolicyWorkspace(String role) throws Exception {
        mvc.perform(get(PATH).with(user("policy-view-qa").roles(role))).andExpect(status().isForbidden());
    }
    @Test void anonymousCannotReadPolicyWorkspace() throws Exception {mvc.perform(get(PATH)).andExpect(status().isUnauthorized());}
    @Test void internalCollectionWorkspaceProvidesPolicyEntry() throws Exception {
        var html=Jsoup.parse(mvc.perform(get("/app/admin/collected-announcements").with(user("policy-view-qa").roles("ADMIN")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(html.select("a[href='"+PATH+"']").text()).isEqualTo("공고 첨부 정책 관리");
    }
}
