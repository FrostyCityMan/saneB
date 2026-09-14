package com.saneb.domain.announcementattachment.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.nio.charset.StandardCharsets;
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
class AnnouncementAttachmentBatchViewControllerSmokeTest {
    private static final String PATH="/app/admin/announcement-attachment-batches";
    @Autowired MockMvc mvc;
    @ParameterizedTest @ValueSource(strings={"ADMIN","OPERATOR","APPROVER"})
    void internalRolesReceiveNoStoreAndExplicitDisabledApproval(String role) throws Exception {
        var response=mvc.perform(get(PATH).with(user("batch-view-qa").roles(role))).andExpect(status().isOk())
                .andExpect(view().name("app/announcement-attachment-batches")).andExpect(header().string("Cache-Control","no-store")).andReturn().getResponse();
        var html=Jsoup.parse(response.getContentAsString(StandardCharsets.UTF_8));
        assertThat(html.selectFirst("[data-attachment-batches]").attr("data-is-admin")).isEqualTo(String.valueOf(role.equals("ADMIN")));
        assertThat(html.select("[data-scope-fields][disabled], [data-approval-fields][disabled], [data-approve][disabled]")).hasSize(3);
        assertThat(html.selectFirst("input[name=acknowledged]").hasAttr("required")).isTrue();
        assertThat(html.selectFirst("input[name=acknowledged]").hasAttr("checked")).isFalse();
        assertThat(html.selectFirst("label[for=batch-reason]").text()).contains("1~1000자");
        assertThat(html.selectFirst("textarea#batch-reason").attr("aria-describedby")).contains("batch-error");
        assertThat(html.select("script:not([src])")).isEmpty();
        assertThat(html.select("script[src='/js/saneb-attachment-batch-core.js'],script[src='/js/saneb-attachment-batches.js']")).hasSize(2);
        assertThat(html.select("link[href='/css/saneb-announcement-attachment-review.css']")).hasSize(1);
        assertThat(html.select("input[name=provider][value=GOV24]")).hasSize(1);
        assertThat(html.text()).contains("한 배치의 완료는 전체 기존 데이터의 완료가 아닙니다", "접수는 처리 완료가 아닙니다", "최초 요청 성공 여부는 미확정", "원래 수집 실패", "페이지를 바꿔도", "서울");
        assertThat(html.selectFirst("[data-error]").attr("role")).isEqualTo("alert");
        assertThat(html.select("[data-history-refresh][disabled]")).hasSize(1);
        assertThat(html.selectFirst("#batch-receipt-title").attr("tabindex")).isEqualTo("-1");
        assertThat(html.selectFirst("#batch-history-title").text()).isEqualTo("적용·원복 승인 전체 이력");
        assertThat(html.text()).contains("승인 당시 범위와 현재 처리 결과는 별도", "일반 감사 로그가 아닙니다");
    }
    @ParameterizedTest @ValueSource(strings={"USER","PARTNER","REVIEWER"})
    void externalRolesCannotReadWorkspace(String role) throws Exception {mvc.perform(get(PATH).with(user("batch-view-qa").roles(role))).andExpect(status().isForbidden());}
    @Test void anonymousCannotReadWorkspace() throws Exception {mvc.perform(get(PATH)).andExpect(status().isUnauthorized());}
    @Test void collectedMenuProvidesEntry() throws Exception {
        var html=Jsoup.parse(mvc.perform(get("/app/admin/collected-announcements").with(user("batch-view-qa").roles("ADMIN"))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(html.select("a[href='"+PATH+"']").text()).isEqualTo("첨부 배치 작업");
    }
}
