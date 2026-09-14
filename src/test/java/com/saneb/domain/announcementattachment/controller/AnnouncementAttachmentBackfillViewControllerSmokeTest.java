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
class AnnouncementAttachmentBackfillViewControllerSmokeTest {
    private static final String PATH="/app/admin/announcement-attachment-backfills";
    @Autowired MockMvc mvc;
    @ParameterizedTest @ValueSource(strings={"ADMIN","OPERATOR","APPROVER"})
    void internalRolesReceiveNoStoreAndExplicitDisabledApproval(String role) throws Exception {
        var response=mvc.perform(get(PATH).with(user("backfill-view-qa").roles(role))).andExpect(status().isOk())
                .andExpect(view().name("app/announcement-attachment-backfills")).andExpect(header().string("Cache-Control","no-store")).andReturn().getResponse();
        var html=Jsoup.parse(response.getContentAsString(StandardCharsets.UTF_8));
        assertThat(html.selectFirst("[data-attachment-backfills]").attr("data-is-admin")).isEqualTo(String.valueOf(role.equals("ADMIN")));
        assertThat(html.select("[data-scope-fields][disabled], [data-approval-fields][disabled], [data-approve][disabled], [data-reserve][disabled]")).hasSize(4);
        assertThat(html.selectFirst("input[name=acknowledged]").hasAttr("required")).isTrue();
        assertThat(html.selectFirst("input[name=acknowledged]").hasAttr("checked")).isFalse();
        assertThat(html.selectFirst("label[for=backfill-reason]").text()).contains("1~1000자");
        assertThat(html.selectFirst("textarea#backfill-reason").attr("aria-describedby")).contains("backfill-error");
        assertThat(html.selectFirst("input[name=segmentSize]").attr("max")).isEqualTo("1000");
        assertThat(html.select("input[name=maximumCount], input[name=sourceIds]")).isEmpty();
        assertThat(html.select("script:not([src])")).isEmpty();
        assertThat(html.select("script[src='/js/saneb-attachment-backfill-core.js'],script[src='/js/saneb-attachment-backfills.js']")).hasSize(2);
        assertThat(html.select("link[href='/css/saneb-announcement-attachment-review.css']")).hasSize(1);
        assertThat(html.text()).contains("분할 크기는 전체 후보 수의 상한이 아닙니다","목록 고정과 예약은 처리 완료가 아닙니다", "삭제·미예약·입력 변경·실패", "원래 요청 그대로 재확인", "서울");
        assertThat(html.selectFirst("[data-error]").attr("role")).isEqualTo("alert");
    }
    @ParameterizedTest @ValueSource(strings={"USER","PARTNER","REVIEWER"})
    void externalRolesCannotReadWorkspace(String role) throws Exception {mvc.perform(get(PATH).with(user("backfill-view-qa").roles(role))).andExpect(status().isForbidden());}
    @Test void anonymousCannotReadWorkspace() throws Exception {mvc.perform(get(PATH)).andExpect(status().isUnauthorized());}
    @Test void batchWorkspaceProvidesWholeInventoryEntry() throws Exception {
        var html=Jsoup.parse(mvc.perform(get("/app/admin/announcement-attachment-batches").with(user("backfill-view-qa").roles("ADMIN"))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(html.select("a[href='"+PATH+"']").text()).isEqualTo("전체 대상·분할 관리");
    }
}
