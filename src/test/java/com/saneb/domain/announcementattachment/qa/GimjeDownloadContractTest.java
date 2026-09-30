package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import java.net.URI;
import java.util.*;
import java.util.stream.*;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.provider.content.GimjeNoticePage;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.classification.*;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.*;

class GimjeDownloadContractTest {
    private final AnnouncementAttachmentBbsOfficialObservationTest.ObservationCase sample = GimjeDownloadCases.selectCase();
    private String selectFile(int id, String extension) {
        String query = "?boardId=BBS_0000044&amp;menuCd=DOM_000000104003000000&amp;paging=ok&amp;startPage=1&amp;dataSid=310426&amp;command=update&amp;fileSid=" + id;
        return "<dd><span><a href=\"javascript:void('0')\" title='공고." + extension + "'>공고." + extension + " (100 kb)</a></span>"
                + "<p><a class=sbtn_down href='/board/download.gimje" + query + "' title='공고." + extension + " 다운로드'>다운로드</a>"
                + "<a class=sbtn_file2 href='/board/SynapViewer.gimje" + query + "'>미리보기</a></p></dd>";
    }
    private String selectPage(String files) {
        return "<nav>수출 메뉴</nav><div class=bbs_skin><div class=bbs_view><div class=bbs_vtop><h4>" + sample.title()
                + "</h4><ul><li>특허 부서</li></ul></div><div class=bbs_con>소상공인 카드수수료 지원</div>"
                + "<div class=bbs_filedown><dl><dt>첨부파일</dt>" + files + "</dl></div></div></div>";
    }
    @Test void officialSourceAndDownloadsAreBoundToBoardAndNotice() {
        var profile = sample.profile(); URI uri = profile.selectDetailUri(sample.source());
        for (String bad : List.of(uri + "&extra=1", uri + "&dataSid=1", uri + "#x", uri.toString().replace("https:", "http:"),
                uri.toString().replace("www.gimje.go.kr", "127.0.0.1"), uri.toString().replace("BBS_0000044", "BBS_0000099")))
            assertThat(profile.selectApprovedRequest(URI.create(bad))).as(bad).isFalse();
        var source = sample.source();
        assertThatThrownBy(() -> profile.selectDetailUri(new AttachmentDiscoveryProfile.Source(source.providerCode(), source.providerNoticeId(),
                source.sourceUrl(), "LGS-000168", source.listParserProfileCode()))).isInstanceOf(IllegalArgumentException.class);
        var result = profile.selectDescriptors(source, selectPage(selectFile(196565, "hwpx") + selectFile(196574, "hwp")));
        assertThat(result.status()).isEqualTo("FOUND"); assertThat(result.complete()).isTrue();
        assertThat(result.descriptors()).extracting(AttachmentDiscoveryProfile.Descriptor::expectedFormat).containsExactly("HWPX", "HWP");
        var file = result.descriptors().getFirst();
        assertThat(file.displayName()).isEqualTo("공고.hwpx"); assertThat(file.documentRole()).isEqualTo("UNKNOWN");
        assertThat(profile.selectApprovedRequest(file.selectRequest())).isTrue();
        assertThat(profile.selectApprovedRequest(new Request(file.fetchUri(), "POST", Map.of("fileSid", "196565")))).isFalse();
        assertThat(profile.selectApprovedRequest(file.selectRequest(), new Request(URI.create(file.fetchUri() + "&x=1"), "GET", Map.of()))).isFalse();
    }
    @Test void rejectsCrossNoticeExternalAndExecutableDownloads() {
        String file = selectFile(1, "pdf");
        for (String bad : List.of(file.replace("dataSid=310426", "dataSid=99"), file.replace("href='/board/download", "href='https://evil.example/board/download"),
                file.replace("class=sbtn_down", "class=sbtn_down onclick='alert(1)'"), file.replace("command=update", "command=delete")))
            assertThat(sample.profile().selectDescriptors(sample.source(), selectPage(bad)).descriptors()).isEmpty();
    }
    @Test void unresolvedAndUnsupportedAreSeparateFromGoodFiles() {
        var result = sample.profile().selectDescriptors(sample.source(), selectPage(selectFile(1, "pdf") + selectFile(2, "xlsx") + "<dd><a href='/unknown'>미확인</a></dd>"));
        assertThat(result.complete()).isFalse(); assertThat(result.descriptors()).hasSize(2)
                .extracting(AttachmentDiscoveryProfile.Descriptor::downloadAllowed).containsExactly(true, false);
        assertThat(sample.profile().selectDescriptors(sample.source(), selectPage(selectFile(1, "hwp")).replace("SynapViewer.gimje", "unknown.gimje")).complete()).isFalse();
    }
    @Test void missingAreaIsNotNoFilesAndGlobalLinksAreIgnored() {
        assertThat(sample.profile().selectDescriptors(sample.source(), selectPage("")).status()).isEqualTo("NO_FILES");
        assertThat(sample.profile().selectDescriptors(sample.source(), selectPage("").replace("bbs_filedown", "changed")).status()).isEqualTo("FAILED");
        assertThat(sample.profile().selectDescriptors(sample.source(), selectPage(selectFile(1, "pdf")) + "<a href='/outside.pdf'>메뉴</a>").descriptors()).hasSize(1);
    }
    @Test void duplicatesAndLimitPreserveTheSuccessfulSubset() {
        String one = selectFile(1, "hwp");
        assertThat(sample.profile().selectDescriptors(sample.source(), selectPage(one + one)).descriptors()).hasSize(1);
        var result = sample.profile().selectDescriptors(sample.source(), selectPage(IntStream.rangeClosed(1, 11).mapToObj(i -> selectFile(i, "hwp")).collect(Collectors.joining())));
        assertThat(result.status()).isEqualTo("LIMIT_EXCEEDED"); assertThat(result.descriptors()).hasSize(10);
    }
    @Test void titleAndBodyExcludeMetadataAndRequireOneContainer() {
        String page = selectPage(selectFile(1, "hwp"));
        assertThat(GimjeNoticePage.selectContent(Jsoup.parse(page)).text()).isEqualTo("소상공인 카드수수료 지원");
        AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(Jsoup.parse(page), sample.title(), sample.titleLayout());
        assertThatThrownBy(() -> GimjeNoticePage.selectRoot(Jsoup.parse(page + page))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(Jsoup.parse(page), "다른 제목", sample.titleLayout())).isInstanceOf(AssertionError.class);
    }
    @Test void preservesTitleRulesAndBoundedCollectionBudget() throws Exception {
        var decision = new AnnouncementSourceClassificationEngine().selectDecision(new AnnouncementSourceClassificationInput("LOCAL_GOV_NOTICE", sample.title(), null, null,
                List.of(), BodySourceCode.NONE, BodyAvailabilityCode.UNAVAILABLE), AnnouncementAttachmentRealFileQaTest.selectDraftRuleSet());
        assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectTitleMayProceed(decision)).isTrue();
        var budget = AnnouncementAttachmentBbsOfficialObservationTest.selectBudget(sample.profile(), true, false);
        assertThat(budget.maximumRequests).isEqualTo(6); assertThat(budget.maximumBytes).isEqualTo(23 * 1024 * 1024);
    }
}
