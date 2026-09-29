package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import com.saneb.domain.announcementattachment.discovery.AttachmentDiscoveryProfile;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.classification.*;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.*;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.*;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class GyeongnamSecondDownloadContractTest {
    private String selectFile(String group, int id, String ext) {
        return (group.equals("GOSEONG") ? "https://eminwon.goseong.go.kr" : "http://eminwon.changwon.go.kr")
                + "/emwp/jsp/ofr/FileDown.jsp?user_file_nm=file" + id + "." + ext + "&sys_file_nm=saved" + id + "." + ext + "&file_path=/ntishome/file/upload/ofr/ofr/20260929";
    }
    private String selectProxy(String inner, String name) {
        return "https://www.changwon.go.kr/cwportal/DownloadEx.do?url=" + URLEncoder.encode(inner, StandardCharsets.UTF_8) + "&name=" + name;
    }
    private String selectLink(String group, int id, String ext) {
        String file = selectFile(group, id, ext), name = "file" + id + "." + ext;
        return "<li><a href='" + (group.equals("GOSEONG") ? file : selectProxy(file, name)) + "'>" + name + "</a></li>";
    }
    private String selectPage(String group, String links) {
        String title = GyeongnamSecondDownloadCases.selectCase(group).title();
        return group.equals("GOSEONG") ? "<div class=bdvTitWrap><p class=bdvTit>" + title + "</p></div><div class=bdvFileWrap><p class=bdvf_tit>첨부파일</p><ul class=bdvFileBox>" + links + "</ul></div>"
                : "<form id=saeolGosiVO><div class=bbs1view1><h1 class=h1>" + title + "</h1><div class=attach1><ul>" + links + "</ul></div></div></form>";
    }
    @ParameterizedTest @ValueSource(strings={"GOSEONG", "CHANGWON"})
    void collectsGoodFilesDespiteUnresolvedPreviewAndSeparatesUnsupportedFiles(String group) {
        var sample = GyeongnamSecondDownloadCases.selectCase(group); var p = sample.profile();
        String links = selectLink(group, 1, "hwp") + selectLink(group, 2, "hwpx") + selectLink(group, 3, "pdf") + selectLink(group, 4, "jpg");
        var found = p.selectDescriptors(sample.source(), "<nav><a href='/noise.pdf'>noise.pdf</a></nav>" + selectPage(group, links));
        assertThat(found.complete()).isTrue(); assertThat(found.descriptors()).hasSize(4);
        assertThat(found.descriptors().stream().filter(AttachmentDiscoveryProfile.Descriptor::downloadAllowed)).hasSize(3);
        found.descriptors().forEach(d -> assertThat(d.documentRole()).isEqualTo("UNKNOWN"));
        for (String unknown : List.of("<a href='/preview'>미리보기</a>", "<script>loading_convert()</script>", "<button>첨부</button>", "<img src='/other'>")) {
            var partial = p.selectDescriptors(sample.source(), selectPage(group, links + unknown));
            assertThat(partial.complete()).isFalse(); assertThat(partial.warnings()).contains("ATTACHMENT_LINK_UNRESOLVED"); assertThat(partial.descriptors()).hasSize(4);
        }
    }
    @ParameterizedTest @ValueSource(strings={"GOSEONG", "CHANGWON"})
    void deduplicatesLimitsAndRetainsConflictsAsErrors(String group) {
        var s = GyeongnamSecondDownloadCases.selectCase(group); var p = s.profile(); String one = selectLink(group, 1, "hwp");
        assertThat(p.selectDescriptors(s.source(), selectPage(group, one + one)).descriptors()).hasSize(1);
        var conflict = p.selectDescriptors(s.source(), selectPage(group, one + one.replace("file1", "other1")));
        assertThat(conflict.complete()).isFalse(); assertThat(conflict.descriptors()).hasSize(1);
        var limit = p.selectDescriptors(s.source(), selectPage(group, IntStream.rangeClosed(1, 11).mapToObj(i -> selectLink(group, i, "hwp")).collect(Collectors.joining())));
        assertThat(limit.status()).isEqualTo("LIMIT_EXCEEDED"); assertThat(limit.descriptors()).hasSize(10);
        assertThat(p.selectDescriptors(s.source(), "<html/>").warnings()).contains("ATTACHMENT_SELECTOR_CHANGED");
        assertThat(p.selectDescriptors(s.source(), selectPage(group, "")).status()).isEqualTo("NO_FILES");
        assertThat(p.selectDescriptors(s.source(), selectPage(group, "<li></li>")).complete()).isFalse();
    }
    @ParameterizedTest @ValueSource(strings={"GOSEONG", "CHANGWON"})
    void verifiesSourceTitleAndUnchangedDraftSeed(String group) throws Exception {
        var s = GyeongnamSecondDownloadCases.selectCase(group); var p = s.profile();
        assertThat(p.selectDetailUri(s.source()).toString()).isEqualTo(s.source().sourceUrl());
        assertThatThrownBy(() -> p.selectDetailUri(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE", "bad", s.source().sourceUrl(), s.source().localSourceCode(), s.source().listParserProfileCode()))).hasMessage("PROFILE_REQUIRED");
        var page = Jsoup.parse(selectPage(group, selectLink(group, 1, "hwp")));
        AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page, s.title(), s.titleLayout());
        assertThatThrownBy(() -> AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page, "다른 제목", s.titleLayout())).isInstanceOf(AssertionError.class);
        var result = new AnnouncementSourceClassificationEngine().selectDecision(new AnnouncementSourceClassificationInput("LOCAL_GOV_NOTICE", s.title(), null, null, List.of(), BodySourceCode.NONE, BodyAvailabilityCode.UNAVAILABLE), AnnouncementAttachmentRealFileQaTest.selectDraftRuleSet());
        assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectTitleMayProceed(result)).isTrue();
    }
    @ParameterizedTest @ValueSource(strings={"GOSEONG", "CHANGWON"})
    void rejectsUnboundRequestsAndRedirects(String group) {
        var s = GyeongnamSecondDownloadCases.selectCase(group); var p = s.profile(); String url = s.source().sourceUrl();
        for (String bad : List.of("https:/missing-host", url + "&extra=1", url + "&" + (group.equals("GOSEONG") ? "dataSid" : "not_ancmt_mgt_no") + "=2", url.replace("https:", "http:"), url.replace("www.", "evil."), url + "#fragment"))
            assertThat(p.selectApprovedRequest(URI.create(bad))).isFalse();
        var a = p.selectDescriptors(s.source(), selectPage(group, selectLink(group, 1, "hwp"))).descriptors().getFirst();
        var b = p.selectDescriptors(s.source(), selectPage(group, selectLink(group, 2, "hwp"))).descriptors().getFirst();
        assertThat(p.selectApprovedRequest(a.selectRequest(), a.selectRequest())).isTrue();
        assertThat(p.selectApprovedRequest(a.selectRequest(), b.selectRequest())).isFalse();
        assertThat(p.selectApprovedRequest(new Request(a.fetchUri(), "POST", Map.of("x", "y")))).isFalse();
    }
    @Test void proxyCannotAccessAnotherHostOrMismatchedFilename() {
        var p = GyeongnamSecondDownloadCases.selectCase("CHANGWON").profile(); String file = selectFile("CHANGWON", 1, "hwp");
        for (String inner : List.of("/relative", file.replace("eminwon.changwon.go.kr", "127.0.0.1"), file.replace("FileDown.jsp", "Other.jsp"), file + "&extra=1", file.replace("http://", "http://user@"), file + "#fragment"))
            assertThat(p.selectApprovedRequest(URI.create(selectProxy(inner, "file1.hwp")))).isFalse();
        assertThat(p.selectApprovedRequest(URI.create(selectProxy(file, "other.hwp")))).isFalse();
        assertThat(p.selectApprovedRequest(URI.create(selectProxy(file, "file1.hwp")))).isTrue();
    }
    @Test void directFileRequiresOfficialHostPathAndMatchingNames() {
        var s = GyeongnamSecondDownloadCases.selectCase("GOSEONG"); String link = selectLink("GOSEONG", 1, "hwp");
        for (String bad : List.of(link.replace("eminwon.goseong.go.kr", "evil.go.kr"), link.replace("FileDown.jsp", "Other.jsp"), link.replace(">file1.hwp<", ">other.hwp<")))
            assertThat(s.profile().selectDescriptors(s.source(), selectPage("GOSEONG", bad)).descriptors()).isEmpty();
        var mismatch = s.profile().selectDescriptors(s.source(), selectPage("GOSEONG", link.replace("saved1.hwp", "saved1.jpg")));
        assertThat(mismatch.descriptors()).hasSize(1); assertThat(mismatch.descriptors().getFirst().downloadAllowed()).isFalse();
    }
    @Test void proxyAcceptsOfficialNestedFilenameWithKoreanSpacesWithoutChangingFetchUrl() {
        var s = GyeongnamSecondDownloadCases.selectCase("CHANGWON"); String name = "소상공인 지원 공고(안).hwpx";
        String inner = selectFile("CHANGWON", 1, "hwpx").replace("file1.hwpx", name).replace("saved1.hwpx", "저장 공고(안)_1.hwpx");
        String proxy = selectProxy(inner, URLEncoder.encode(name, StandardCharsets.UTF_8));
        assertThat(s.profile().selectApprovedRequest(URI.create(proxy))).isTrue();
        var result = s.profile().selectDescriptors(s.source(), selectPage("CHANGWON", "<li><a href='" + proxy + "'>" + name + "</a></li>"));
        assertThat(result.complete()).isTrue(); assertThat(result.descriptors()).hasSize(1);
        assertThat(result.descriptors().getFirst().fetchUri().toString()).isEqualTo(proxy);
        assertThat(result.descriptors().getFirst().displayName()).isEqualTo(name);
    }
}
