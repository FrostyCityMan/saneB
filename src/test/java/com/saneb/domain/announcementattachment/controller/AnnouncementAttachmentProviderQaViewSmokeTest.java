package com.saneb.domain.announcementattachment.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
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
class AnnouncementAttachmentProviderQaViewSmokeTest {
    private static final String PATH="/app/admin/announcement-attachment-provider-qa";
    @Autowired MockMvc mvc;
    @ParameterizedTest @ValueSource(strings={"ADMIN","OPERATOR","APPROVER"})
    void workspaceSeparatesReadOnlyPlanAndExplicitNetworkApproval(String role) throws Exception {
        var response=mvc.perform(get(PATH).with(user("provider-view-qa").roles(role)))
                .andExpect(status().isOk()).andExpect(view().name("app/announcement-attachment-provider-qa"))
                .andExpect(header().string("Cache-Control","no-store")).andReturn().getResponse();
        var html=Jsoup.parse(response.getContentAsString(StandardCharsets.UTF_8));
        assertThat(html.selectFirst("[data-provider-qa]").attr("data-is-admin")).isEqualTo(String.valueOf(role.equals("ADMIN")));
        assertThat(html.select("[data-approval-fields][disabled], [data-submit][disabled], [data-cancel][disabled]")).hasSize(3);
        assertThat(html.select("[data-provider-qa] input[checked]")).isEmpty();
        assertThat(html.select("input[name=acknowledgeScope][required],input[name=acknowledgeNetworkBudget][required]")).hasSize(2);
        assertThat(html.selectFirst("textarea[name=reason]").attr("aria-describedby")).contains("provider-qa-error");
        assertThat(html.select("input[name=maximumBytes],input[name=url],input[name=profile],input[name=passed]")).isEmpty();
        assertThat(html.select("script:not([src])")).isEmpty();
        assertThat(html.select("script[src='/js/saneb-csrf.js'],script[src='/js/saneb-attachment-policy-core.js'],script[src='/js/saneb-attachment-provider-qa-core.js'],script[src='/js/saneb-attachment-provider-qa.js']")).hasSize(4);
        assertThat(html.select("meta[name=_csrf],meta[name=_csrf_header]")).hasSize(2);
        assertThat(html.selectFirst("[data-error]").attr("role")).isEqualTo("alert");
        assertThat(html.text()).contains("제목 → 정제 본문 → 실제 PDF·HWP·HWPX 텍스트 → 관리자 최종 검증", "전체 QA 통과 또는 정상 공고 판정이 아닙니다", "원래 키·입력", "이미 수행한 요청을 되돌리지는 않음");
        if ("true".equals(System.getenv("SANEB_ATTACHMENT_PROVIDER_QA_VIEW_EXPORT")) && "ADMIN".equals(role)) {
            html.select("meta[name=_csrf],meta[name=_csrf_header],input[name=_csrf],link[href^=http]").remove();
            html.select("script").stream().filter(e -> !Set.of("/js/saneb-csrf.js","/js/saneb-attachment-policy-core.js",
                    "/js/saneb-attachment-provider-qa-core.js","/js/saneb-attachment-provider-qa.js","/js/saneb-layout.js").contains(e.attr("src"))).forEach(org.jsoup.nodes.Element::remove);
            html.selectFirst("[data-provider-qa]").prependElement("p").attr("role","note")
                    .text("합성 QA 전용 화면입니다. 운영 DB·인증·정책·수집원과 연결하지 않습니다.");
            var target=Path.of("build","attachment-provider-ui-qa","index.html");Files.createDirectories(target.getParent());Files.writeString(target,html.outerHtml(),StandardCharsets.UTF_8);
        }
    }
    @ParameterizedTest @ValueSource(strings={"USER","PARTNER","REVIEWER"})
    void externalRolesCannotReadProviderQa(String role) throws Exception {mvc.perform(get(PATH).with(user("provider-view-qa").roles(role))).andExpect(status().isForbidden());}
    @Test void anonymousCannotReadProviderQa() throws Exception {mvc.perform(get(PATH)).andExpect(status().isUnauthorized());}
}
