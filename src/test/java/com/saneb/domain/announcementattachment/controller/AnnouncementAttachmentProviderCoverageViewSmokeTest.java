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
class AnnouncementAttachmentProviderCoverageViewSmokeTest {
    private static final String PATH="/app/admin/announcement-attachment-provider-coverage";
    @Autowired MockMvc mvc;
    @ParameterizedTest @ValueSource(strings={"ADMIN","OPERATOR","APPROVER"})
    void internalRolesReadCoverageWithoutMutationControls(String role) throws Exception {
        var response=mvc.perform(get(PATH).with(user("coverage-view-qa").roles(role)))
                .andExpect(status().isOk()).andExpect(view().name("app/announcement-attachment-provider-coverage"))
                .andExpect(header().string("Cache-Control","no-store")).andReturn().getResponse();
        var html=Jsoup.parse(response.getContentAsString(StandardCharsets.UTF_8));
        assertThat(html.select("[data-provider-coverage]")).hasSize(1);
        assertThat(html.select("[data-refresh][disabled], [data-prev][disabled], [data-next][disabled]")).hasSize(3);
        assertThat(html.select("[data-provider-coverage] form, [data-provider-coverage] input, [data-provider-coverage] select")).isEmpty();
        assertThat(html.select("script:not([src])")).isEmpty();
        assertThat(html.select("script[src='/js/saneb-attachment-policy-core.js'],script[src='/js/saneb-attachment-provider-coverage.js']")).hasSize(2);
        assertThat(html.select("link[href='/css/saneb-announcement-attachment-review.css']")).hasSize(1);
        assertThat(html.selectFirst("[data-error]").attr("role")).isEqualTo("alert");
        assertThat(html.selectFirst("[data-status]").attr("aria-live")).isEqualTo("polite");
        assertThat(html.selectFirst("#coverage-targets").attr("tabindex")).isEqualTo("-1");
        assertThat(html.text()).contains("제목 1차 판정 → 정제 본문 2차 판정", "실제 QA 통과 아님", "미지원·첨부 없음·검증 면제", "전체 대상 수", "정상 공고 3건", "정상 다중 첨부 공고 1건");
        if ("true".equals(System.getenv("SANEB_ATTACHMENT_COVERAGE_VIEW_EXPORT")) && "ADMIN".equals(role)) {
            html.select("meta[name=_csrf], meta[name=_csrf_header], input[name=_csrf], link[href^=http]").remove();
            html.select("script").stream().filter(e -> !Set.of("/js/saneb-attachment-policy-core.js",
                    "/js/saneb-attachment-provider-coverage.js", "/js/saneb-layout.js").contains(e.attr("src"))).forEach(org.jsoup.nodes.Element::remove);
            html.selectFirst("[data-provider-coverage]").prependElement("p").attr("role", "note")
                    .text("합성 QA 전용 화면입니다. 운영 DB·인증·정책·수집원과 연결하지 않습니다.");
            var target=Path.of("build", "attachment-coverage-ui-qa", "index.html");
            Files.createDirectories(target.getParent());Files.writeString(target,html.outerHtml(),StandardCharsets.UTF_8);
        }
    }
    @ParameterizedTest @ValueSource(strings={"USER","PARTNER","REVIEWER"})
    void externalRolesCannotReadCoverage(String role) throws Exception {
        mvc.perform(get(PATH).with(user("coverage-view-qa").roles(role))).andExpect(status().isForbidden());
    }
    @Test void anonymousCannotReadCoverage() throws Exception {mvc.perform(get(PATH)).andExpect(status().isUnauthorized());}
}
