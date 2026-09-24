package com.saneb.domain.announcementattachment.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
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
class AnnouncementAttachmentViewControllerSmokeTest {
    private static final String PATH="/app/admin/collected-announcements/"+UUID.randomUUID()+"/attachments";
    @Autowired MockMvc mvc;
    @ParameterizedTest @ValueSource(strings={"ADMIN","OPERATOR","APPROVER"})
    void internalRolesReceiveNoStoreShellAndDisabledVersionBoundForms(String role) throws Exception {
        var response=mvc.perform(get(PATH).with(user("attachment-view-qa").roles(role))).andExpect(status().isOk())
                .andExpect(view().name("app/announcement-attachment-review")).andExpect(header().string("Cache-Control","no-store"))
                .andReturn().getResponse();
        var html=Jsoup.parse(response.getContentAsString(StandardCharsets.UTF_8));
        assertThat(html.selectFirst("[data-attachment-review-page]").attr("data-can-manage")).isEqualTo(String.valueOf(!role.equals("APPROVER")));
        assertThat(html.selectFirst("[data-attachment-review-page]").attr("data-can-rollback")).isEqualTo(String.valueOf(role.equals("ADMIN")));
        assertThat(html.select("[data-review-fields][disabled], [data-draft-fields][disabled]")).hasSize(2);
        assertThat(html.select("[data-confirm][disabled], [data-create-draft][disabled]")).hasSize(2);
        assertThat(html.selectFirst("label[for=attachment-note]").text()).contains("검수 사유");
        assertThat(html.select("script:not([src])")).isEmpty();
        assertThat(html.select("link[href='/css/saneb-announcement-attachment-review.css']")).hasSize(1);
        assertThat(html.selectFirst("[data-review-error]").attr("role")).isEqualTo("alert");
        assertThat(html.selectFirst("input[name=confirmationAcknowledged]").hasAttr("required")).isTrue();
        assertThat(html.selectFirst("input[name=draftAcknowledged]").hasAttr("checked")).isFalse();
        assertThat(html.select("[data-operation-fields][disabled], [data-operation-submit][disabled], [data-operation-job][disabled]")).hasSize(3);
        assertThat(html.selectFirst("input[name=operationAcknowledged]").hasAttr("required")).isTrue();
        assertThat(html.selectFirst("input[name=operationAcknowledged]").hasAttr("checked")).isFalse();
        assertThat(html.select("script[src='/js/saneb-attachment-operations.js']")).hasSize(1);
        assertThat(html.select("script[src='/js/saneb-attachment-recovery.js']")).hasSize(1);
        assertThat(html.select("script[src='/js/saneb-attachment-segments.js']")).hasSize(1);
        assertThat(html.select("[data-segments][role=region][tabindex='-1'], [data-blocks][role=region][tabindex='-1']")).hasSize(2);
        assertThat(html.select("script[src]").eachAttr("src").indexOf("/js/saneb-attachment-segments.js"))
                .isLessThan(html.select("script[src]").eachAttr("src").indexOf("/js/saneb-announcement-attachment-review.js"));
        assertThat(html.select("[data-recovery-fields][disabled], [data-recovery-submit][disabled], [data-recovery-load][disabled]")).hasSize(3);
        assertThat(html.selectFirst("input[name=restorationAcknowledged]").hasAttr("checked")).isFalse();
        assertThat(html.selectFirst("input[name=restorationAcknowledged]").hasAttr("required")).isTrue();
        assertThat(html.selectFirst("textarea#attachment-recovery-reason").attr("aria-describedby")).contains("attachment-recovery-error");
        assertThat(html.selectFirst("[data-recovery-panel]").text()).contains("배치 작업", "원래 수집 실패", "사유 지문", "이후 작업", "원복 영수증");
        assertThat(html.select("[data-recovery-permission]")).hasSize(role.equals("ADMIN")?0:1);
        assertThat(html.selectFirst("[data-operation-panel]").text()).contains("최근 24시간 최대 3회", "외부 요청 없음", "이전 검수 확인", "바이트");
        assertThat(html.text()).contains("자동 활성화하지 않습니다", "브라우저 저장소에는 보관하지 않습니다", "과거 집합 조회는 현재 검수 기준을 바꾸지 않습니다");
        // 명시적인 로컬 브라우저 QA에서만 synthetic SSR을 산출한다. 실제 세션/운영 DB와 무관하다.
        if ("true".equals(System.getenv("SANEB_ATTACHMENT_UI_FIXTURE")) && role.equals("ADMIN")) {
            html.select("meta[name=_csrf], meta[name=_csrf_header], input[name=_csrf], link[href^=http]").remove();
            html.select("script").stream().filter(e->!e.attr("src").equals("/js/saneb-attachment-review-core.js")
                    && !e.attr("src").equals("/js/saneb-attachment-operations.js")
                    && !e.attr("src").equals("/js/saneb-attachment-recovery.js")
                    && !e.attr("src").equals("/js/saneb-attachment-segments.js")
                    && !e.attr("src").equals("/js/saneb-layout.js")
                    && !e.attr("src").equals("/js/saneb-announcement-attachment-review.js")).forEach(org.jsoup.nodes.Element::remove);
            html.selectFirst("[data-attachment-review-page]").prependElement("p").attr("role","note")
                    .text("로컬 QA 모형입니다. 운영 데이터·실제 서버 검수가 아니며 모든 API 응답은 합성 자료입니다.");
            var target=Path.of("build","attachment-ui-qa","index.html"); Files.createDirectories(target.getParent());
            Files.writeString(target,html.outerHtml(),StandardCharsets.UTF_8);
        }
    }
    @ParameterizedTest @ValueSource(strings={"USER","PARTNER","REVIEWER"})
    void externalRolesCannotReadShell(String role) throws Exception {
        mvc.perform(get(PATH).with(user("attachment-view-qa").roles(role))).andExpect(status().isForbidden());
    }
    @Test void mixedRolesRetainOperatorPermission() throws Exception {
        var html=mvc.perform(get(PATH).with(user("attachment-view-qa").roles("APPROVER","OPERATOR")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(Jsoup.parse(html).selectFirst("[data-attachment-review-page]").attr("data-can-manage")).isEqualTo("true");
    }
    @Test void anonymousIsNotGivenAuthenticatedShell() throws Exception {
        mvc.perform(get(PATH)).andExpect(status().isUnauthorized());
        mvc.perform(get(PATH).accept("text/html")).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/invalid-access?reason=auth"));
    }
    @Test void existingDetailProvidesSpecificReviewEntryWithoutMutatingSource() throws Exception {
        var script=mvc.perform(get("/js/saneb-collected-announcements.js")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(script).contains("/app/admin/collected-announcements/${encodeURIComponent(data.sourceId)}/attachments", "본문·첨부 근거와 검수");
    }
}
