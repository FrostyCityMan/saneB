package com.saneb.domain.announcementattachment.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.nio.charset.StandardCharsets;
import org.jsoup.Jsoup;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties="spring.flyway.enabled=false")
@AutoConfigureMockMvc
class AnnouncementConversionViewSmokeTest {
    private static final String PATH="/app/admin/collected-announcements/11111111-1111-4111-8111-111111111111/attachments";
    @Autowired MockMvc mvc;
    @ParameterizedTest @ValueSource(strings={"ADMIN","OPERATOR","APPROVER"})
    void defaultViewFocusesOnConversionWithoutMaintenanceControls(String role) throws Exception {
        var response=mvc.perform(get(PATH).with(user("conversion-qa").roles(role)))
                .andExpect(status().isOk()).andExpect(view().name("app/announcement-conversion-review"))
                .andExpect(header().string("Cache-Control","no-store")).andReturn().getResponse();
        var html=Jsoup.parse(response.getContentAsString(StandardCharsets.UTF_8));
        assertThat(html.selectFirst("[data-conversion-review]").attr("data-can-manage")).isEqualTo(String.valueOf(!role.equals("APPROVER")));
        assertThat(html.select("[data-fields][disabled], [data-convert][disabled]")).hasSize(2);
        assertThat(html.select("[data-operation-form], [data-recovery-form], [data-load-history], script:not([src])")).isEmpty();
        assertThat(html.select("[data-acknowledgements], [data-retry], [data-blocks]")).hasSize(3);
        assertThat(html.select("[data-classification][hidden], #conversion-blocker[role=status]")).hasSize(2);
        assertThat(html.selectFirst("[data-form]").attr("aria-describedby")).isEqualTo("conversion-blocker");
        assertThat(html.selectFirst("input[name=acknowledged]").hasAttr("required")).isTrue();
        assertThat(html.selectFirst("input[name=acknowledged]").hasAttr("checked")).isFalse();
        assertThat(html.text()).contains("검수 사유", "자동 활성화하지 않습니다");
        assertThat(html.select("script[src]").eachAttr("src")).contains("/js/saneb-announcement-conversion.js");
    }
    @ParameterizedTest @ValueSource(strings={"USER","PARTNER","REVIEWER"})
    void externalRolesAreDenied(String role) throws Exception {
        mvc.perform(get(PATH).with(user("conversion-qa").roles(role))).andExpect(status().isForbidden());
    }
}
