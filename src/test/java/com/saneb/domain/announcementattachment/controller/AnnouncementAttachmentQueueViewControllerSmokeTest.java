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
class AnnouncementAttachmentQueueViewControllerSmokeTest {
    private static final String PATH="/app/admin/announcement-attachment-queue";
    @Autowired MockMvc mvc;
    @ParameterizedTest @ValueSource(strings={"ADMIN","OPERATOR","APPROVER"})
    void readOnlyQueueHasLabelsExplicitScopeAndAllNineServerStates(String role) throws Exception {
        var response=mvc.perform(get(PATH).with(user("queue-qa").roles(role))).andExpect(status().isOk())
                .andExpect(view().name("app/announcement-attachment-queue"))
                .andExpect(header().string("Cache-Control","no-store")).andReturn().getResponse();
        var html=Jsoup.parse(response.getContentAsString(StandardCharsets.UTF_8));
        assertThat(html.select("select[name=processingFlowStatusCode] option")).hasSize(10);
        assertThat(html.selectFirst("select[name=processingFlowStatusCode] option[selected]").val()).isEqualTo("READY_FOR_FINAL_REVIEW");
        assertThat(html.select("select[name=processingFlowStatusCode] option").eachAttr("value"))
                .containsAll(com.saneb.domain.announcementattachment.service.AttachmentProcessingFlow.STATUS_CODES);
        for(String field:java.util.List.of("queue-flow","queue-provider","queue-keyword"))
            assertThat(html.selectFirst("label[for="+field+"]")).isNotNull();
        assertThat(html.select("[data-queue-first][disabled], [data-queue-prev][disabled], [data-queue-next][disabled]")).hasSize(3);
        assertThat(html.selectFirst("[data-queue-error]").attr("role")).isEqualTo("alert");
        assertThat(html.selectFirst("[data-queue-status]").attr("aria-live")).isEqualTo("polite");
        assertThat(html.select("script:not([src])")).isEmpty();
        assertThat(html.select("script[src='/js/saneb-attachment-queue.js']")).hasSize(1);
        assertThat(html.select("link[href='/css/saneb-announcement-attachment-review.css']")).hasSize(1);
        assertThat(html.select("[data-review-form], [data-operation-form], [data-create-draft]")).isEmpty();
        assertThat(html.text()).contains("재수집·재분류·정책 적용을 실행하지 않습니다", "관리자 확인 완료와 자동 분석 완료는 서로 다릅니다", "제목 제외 원문과 QA 데이터는 제공하지 않습니다");
    }
    @ParameterizedTest @ValueSource(strings={"USER","PARTNER","REVIEWER"})
    void externalRolesCannotReadQueue(String role) throws Exception {
        mvc.perform(get(PATH).with(user("queue-qa").roles(role))).andExpect(status().isForbidden());
    }
    @Test void anonymousIsNotGivenInternalQueue() throws Exception {
        mvc.perform(get(PATH)).andExpect(status().isUnauthorized());
    }
    @Test void legacyCollectionListLinksToSeparateQueueWithoutReplacingV1() throws Exception {
        var response=mvc.perform(get("/app/admin/collected-announcements").with(user("queue-qa").roles("ADMIN")))
                .andExpect(status().isOk()).andReturn().getResponse();
        var html=Jsoup.parse(response.getContentAsString(StandardCharsets.UTF_8));
        assertThat(html.selectFirst("a[href='"+PATH+"']")).isNotNull();
        assertThat(html.selectFirst("[data-collected-announcement-page]").attr("data-source-url"))
                .isEqualTo("/api/v1/admin/announcement-sources");
    }
}
