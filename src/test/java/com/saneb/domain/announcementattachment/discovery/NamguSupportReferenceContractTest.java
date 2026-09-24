package com.saneb.domain.announcementattachment.discovery;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

class NamguSupportReferenceContractTest {
    private static final String TITLE = SaeolAttachmentProfileLiveQaTest.NAMGU_SUPPORT_TITLES.get("44466");

    @Test void fixesThreeKnownNoticesAndOneFilePerNoticeWithoutNewProfile() {
        var cases = SaeolAttachmentProfileLiveQaTest.selectNamguSupportCases().toList();
        assertEquals(List.of("44466", "44381", "42871"), cases.stream().map(SaeolGetAttachmentDiscoveryProfileTest.Case::noticeId).toList());
        for (var sample : cases) {
            assertEquals("LGS-000034", sample.sourceCode());
            assertEquals(1, sample.fileCount());
            assertEquals("LOCAL_BUSAN_NAMGU_GET_V1", sample.profile().selectProfileCode());
            var source = SaeolGetAttachmentDiscoveryProfileTest.selectSource(sample, "https");
            var uri = sample.profile().selectDetailUri(source);
            assertEquals("eminwon.bsnamgu.go.kr", uri.getHost());
            assertEquals(7, uri.getRawQuery().split("&").length);
            assertTrue(sample.profile().selectApprovedRequest(
                    com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request.selectGet(uri)));
        }
    }

    private static String page(String title) {
        return "<form name='form1' method='post'><table class='table_03'><tr><th colspan='4'>" + title + "</th></tr></table></form>";
    }

    @Test void acceptsOnlyFixedTitleInMeasuredFormAndTable() {
        assertDoesNotThrow(() -> SaeolAttachmentProfileLiveQaTest.validateNamguTitle(Jsoup.parse(page(TITLE)), TITLE));
        assertDoesNotThrow(() -> SaeolAttachmentProfileLiveQaTest.validateNamguTitle(Jsoup.parse(page(TITLE.replace(" ", "  "))), TITLE));
    }

    @Test void rejectsWrongTitleAndNestedTitleBorrowing() {
        for (String html : List.of(page("다른 공고"), page("").replace("</th>", "<table><tr><th colspan='4'>" + TITLE + "</th></tr></table></th>"),
                page(TITLE) + page(TITLE), page(TITLE).replace("table_03", "other"), page(TITLE).replace("method='post'", "method='get'"))) {
            assertThrows(AssertionError.class, () -> SaeolAttachmentProfileLiveQaTest.validateNamguTitle(Jsoup.parse(html), TITLE));
        }
        assertThrows(AssertionError.class, () -> SaeolAttachmentProfileLiveQaTest.validateNamguTitle(Jsoup.parse(page(TITLE)), "다른 공고"));
    }
}
