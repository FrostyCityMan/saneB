package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import com.saneb.domain.announcementattachment.discovery.AttachmentDiscoveryProfile;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.classification.*;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.*;
import java.net.URI;
import java.util.*;
import java.util.stream.*;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SeoulFirstDownloadContractTest {
    private String selectLink(int id, String ext) {
        return "<a href=\"javascript:goDownLoad('file" + id + "." + ext + "','saved" + id + "." + ext + "','/ntishome/file/upload/ofr/ofr/20260929')\">file" + id + "." + ext + "</a>";
    }
    private String selectPage(String group, String links) {
        String title = SeoulFirstDownloadCases.selectCase(group).title();
        String header="<tr><th>제목</th><td>"+title+"</td></tr>";
        String files=group.equals("EUNPYEONG")?"<tr><td><table><tr><td><font>첨부파일 : </font></td><td>"+links+"</td></tr></table></td></tr>":"<tr><th>첨부파일</th><td>"+links+"</td></tr>";
        return "<form name=form1 method=post><table class='"+(group.equals("EUNPYEONG")?"board2":"view")+"'>"+header+files+"</table></form>";
    }
    @ParameterizedTest @ValueSource(strings={"EUNPYEONG", "SEOCHO"})
    void keepsGoodFilesWhileSeparatingUnknownAndUnsupportedAttachments(String group) {
        var s = SeoulFirstDownloadCases.selectCase(group); String links = selectLink(1, "hwp") + selectLink(2, "hwpx") + selectLink(3, "pdf") + selectLink(4, "jpg");
        var result = s.profile().selectDescriptors(s.source(), "<nav><a href='/noise.pdf'>noise.pdf</a></nav>" + selectPage(group, links));
        assertThat(result.complete()).isTrue(); assertThat(result.descriptors()).hasSize(4);
        assertThat(result.descriptors().stream().filter(AttachmentDiscoveryProfile.Descriptor::downloadAllowed)).hasSize(3);
        result.descriptors().forEach(d -> assertThat(d.documentRole()).isEqualTo("UNKNOWN"));
        for (String unknown : List.of("<a href='/other'>미확인</a>", "<script>alert(1)</script>", "<button>첨부</button>", "<img src='/other'>")) {
            var partial = s.profile().selectDescriptors(s.source(), selectPage(group, links + unknown));
            assertThat(partial.complete()).isFalse(); assertThat(partial.descriptors()).hasSize(4);
        }
    }
    @ParameterizedTest @ValueSource(strings={"EUNPYEONG", "SEOCHO"})
    void sourceAndDraftTitleAreBound(String group) throws Exception {
        var s = SeoulFirstDownloadCases.selectCase(group); assertThat(s.profile().selectDetailUri(s.source()).toString()).isEqualTo(s.source().sourceUrl());
        assertThatThrownBy(() -> s.profile().selectDetailUri(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE", "bad", s.source().sourceUrl(), s.source().localSourceCode(), s.source().listParserProfileCode()))).hasMessage("PROFILE_REQUIRED");
        var page = Jsoup.parse(selectPage(group, selectLink(1, "hwp"))); AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page, s.title(), s.titleLayout());
        assertThatThrownBy(() -> AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page, "다른 제목", s.titleLayout())).isInstanceOf(AssertionError.class);
        var r = new AnnouncementSourceClassificationEngine().selectDecision(new AnnouncementSourceClassificationInput("LOCAL_GOV_NOTICE", s.title(), null, null, List.of(), BodySourceCode.NONE, BodyAvailabilityCode.UNAVAILABLE), AnnouncementAttachmentRealFileQaTest.selectDraftRuleSet());
        assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectTitleMayProceed(r)).isTrue();
    }
    @ParameterizedTest @ValueSource(strings={"EUNPYEONG", "SEOCHO"})
    void limitsDeduplicatesAndDoesNotHideMissingAttachmentArea(String group) {
        var s = SeoulFirstDownloadCases.selectCase(group); String one = selectLink(1, "hwp");
        assertThat(s.profile().selectDescriptors(s.source(), selectPage(group, one + one)).descriptors()).hasSize(1);
        var conflict = s.profile().selectDescriptors(s.source(), selectPage(group, one + one.replace("file1", "other1")));
        assertThat(conflict.complete()).isFalse(); assertThat(conflict.descriptors()).hasSize(1);
        var limit = s.profile().selectDescriptors(s.source(), selectPage(group, IntStream.rangeClosed(1, 11).mapToObj(i -> selectLink(i, "hwp")).collect(Collectors.joining())));
        assertThat(limit.status()).isEqualTo("LIMIT_EXCEEDED"); assertThat(limit.descriptors()).hasSize(10);
        assertThat(s.profile().selectDescriptors(s.source(), "<html/>").warnings()).contains("ATTACHMENT_SELECTOR_CHANGED");
        assertThat(s.profile().selectDescriptors(s.source(), selectPage(group, "")).status()).isEqualTo("NO_FILES");
    }
    @ParameterizedTest @ValueSource(strings={"EUNPYEONG", "SEOCHO"})
    void rejectsForeignHostsQueryChangesAndRequests(String group) {
        var s = SeoulFirstDownloadCases.selectCase(group); var p = s.profile(); String url = s.source().sourceUrl();
        for (String bad : List.of(url + "&extra=1", url + "&" + "not_ancmt_mgt_no" + "=2", url.replace("https:", "http:"), url.replace("https://", "https://user@"), url + "#fragment"))
            assertThat(p.selectApprovedRequest(URI.create(bad))).isFalse();
        var file = p.selectDescriptors(s.source(), selectPage(group, selectLink(1, "hwp"))).descriptors().getFirst();
        assertThat(p.selectApprovedRequest(file.selectRequest(), file.selectRequest())).isTrue();
        assertThat(p.selectApprovedRequest(new Request(file.fetchUri(), "POST", Map.of("x", "y")))).isFalse();
        assertThat(p.selectApprovedRequest(URI.create(file.fetchUri().toString().replace("eminwon.", "evil.")))).isFalse();
        assertThat(p.selectDescriptors(s.source(), selectPage(group, selectLink(1, "hwp").replace("goDownLoad(", "evil("))).descriptors()).isEmpty();
    }
    @Test void eunpyeongRequiresOfficialNestedLabelAndKeepsAllFileElements() {
        var s=SeoulFirstDownloadCases.selectCase("EUNPYEONG");String page=selectPage("EUNPYEONG",selectLink(1,"hwp"));
        assertThat(s.profile().selectDescriptors(s.source(),page.replace("<font>","<font onclick='bad()'>")).complete()).isFalse();
        assertThat(s.profile().selectDescriptors(s.source(),page.replace("board2","other")).warnings()).contains("ATTACHMENT_SELECTOR_CHANGED");
        assertThat(s.profile().selectDescriptors(s.source(),page.replace("첨부파일", "관련 링크")).descriptors()).isEmpty();
    }
}
