package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import java.net.URI;
import java.util.*;
import java.util.stream.*;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.provider.content.YeongdongNoticePage;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.classification.*;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.*;

class YeongdongDownloadContractTest {
    private final AnnouncementAttachmentBbsOfficialObservationTest.ObservationCase sample = YeongdongDownloadCases.selectCase();
    private String selectFile(int id, String extension) {
        return "<li><a href='?mode=D&amp;no=759fdcd35933d6237c5cf16b4908416b&amp;file_id=" + id
                + "' title='공고." + extension + "'><span></span>공고." + extension + "</a> "
                + "<a href='/_prog/bbs/pre_viewer.php?site_dvs_cd=kr&amp;bbs_cd=kor_sub020103&amp;nm=20260116_file."
                + extension + "' class='btn btn-file'><i></i>파일 미리보기</a></li>";
    }
    private String selectPage(String files) {
        return "<nav>수출 메뉴</nav><div class=program--contents><div class='ui bbs--view'><div class='ui bbs--view--header'>"
                + "<h2 class='ui bbs--view--tit'>" + sample.title() + "</h2><div>특허 부서</div></div>"
                + "<div class='ui bbs--view--file'>" + files + "</div><div class='ui bbs--view--cont'><div class='ui bbs--detail--cont'>"
                + "<div class='ui bbs--view--content'>소상공인 이차보전금 지원</div></div></div></div></div>";
    }
    @Test void fixedSourceAndDownloadRemainBoundToTheOfficialBoard() {
        var profile = sample.profile(); var uri = profile.selectDetailUri(sample.source());
        assertThat(profile.selectApprovedRequest(uri)).isTrue();
        for (String bad : List.of(uri + "&extra=1", uri + "&mode=V", uri + "#x", uri.toString().replace("https:", "http:"),
                uri.toString().replace("www.yd21.go.kr", "127.0.0.1"), uri.toString().replace("020103", "020112")))
            assertThat(profile.selectApprovedRequest(URI.create(bad))).isFalse();
        var source = sample.source();
        assertThatThrownBy(() -> profile.selectDetailUri(new AttachmentDiscoveryProfile.Source(source.providerCode(), source.providerNoticeId(),
                source.sourceUrl(), "LGS-000142", source.listParserProfileCode()))).isInstanceOf(IllegalArgumentException.class);
        var result = profile.selectDescriptors(source, selectPage(selectFile(174573, "hwp")));
        assertThat(result.status()).isEqualTo("FOUND"); assertThat(result.complete()).isTrue();
        var file = result.descriptors().getFirst();
        assertThat(file.fetchUri().getPath()).isEqualTo(YeongdongNoticePage.PATH);
        assertThat(file.expectedFormat()).isEqualTo("HWP"); assertThat(file.documentRole()).isEqualTo("UNKNOWN");
        assertThat(profile.selectApprovedRequest(file.selectRequest())).isTrue();
        assertThat(profile.selectApprovedRequest(new Request(file.fetchUri(), "POST", Map.of("file_id", "174573")))).isFalse();
        assertThat(profile.selectApprovedRequest(file.selectRequest(), new Request(URI.create(file.fetchUri() + "&x=1"), "GET", Map.of()))).isFalse();
    }
    @Test void rejectsCrossNoticeExecutableAndUnapprovedFilePaths() {
        String file = selectFile(174573, "hwp"); var profile = sample.profile();
        for (String bad : List.of(file.replace("759fdcd35933d6237c5cf16b4908416b", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"),
                file.replace("?mode=D", "https://evil.example/kr/html/sub02/020103.html?mode=D"),
                file.replace("?mode=D", "?mode=V"), file.replace("<a href='?", "<a onclick='alert(1)' href='?")))
            assertThat(profile.selectDescriptors(sample.source(), selectPage(bad)).descriptors()).isEmpty();
    }
    @Test void unresolvedAndUnsupportedFilesDoNotDiscardGoodDescriptors() {
        var result = sample.profile().selectDescriptors(sample.source(), selectPage(selectFile(1, "pdf") + selectFile(2, "xlsx") + "<a href='/unknown'>미확인</a>"));
        assertThat(result.complete()).isFalse(); assertThat(result.descriptors()).hasSize(2).extracting(AttachmentDiscoveryProfile.Descriptor::downloadAllowed).containsExactly(true, false);
        assertThat(sample.profile().selectDescriptors(sample.source(), selectPage(selectFile(1, "hwp")).replace("pre_viewer.php", "unknown.php")).complete()).isFalse();
    }
    @Test void missingAreaIsNotNoFilesAndOutsideLinksAreNotCollected() {
        assertThat(sample.profile().selectDescriptors(sample.source(), selectPage("")).status()).isEqualTo("NO_FILES");
        assertThat(sample.profile().selectDescriptors(sample.source(), selectPage("").replace("bbs--view--file", "missing")).status()).isEqualTo("FAILED");
        assertThat(sample.profile().selectDescriptors(sample.source(), selectPage(selectFile(1, "pdf")) + "<a href='/outside.pdf'>메뉴</a>").descriptors()).hasSize(1);
    }
    @Test void deduplicatesAndBoundsTheWholeFileSet() {
        String one = selectFile(1, "hwp");
        assertThat(sample.profile().selectDescriptors(sample.source(), selectPage(one + one)).descriptors()).hasSize(1);
        String files = IntStream.rangeClosed(1, 11).mapToObj(i -> selectFile(i, "hwp")).collect(Collectors.joining());
        var result = sample.profile().selectDescriptors(sample.source(), selectPage(files));
        assertThat(result.status()).isEqualTo("LIMIT_EXCEEDED"); assertThat(result.descriptors()).hasSize(10);
    }
    @Test void titleAndBodyExcludeMetadataAndRequireOneContainer() {
        String page = selectPage(selectFile(1, "hwp"));
        assertThat(YeongdongNoticePage.selectContent(Jsoup.parse(page)).text()).isEqualTo("소상공인 이차보전금 지원");
        AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(Jsoup.parse(page), sample.title(), sample.titleLayout());
        assertThatThrownBy(() -> YeongdongNoticePage.selectRoot(Jsoup.parse(page + page))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(Jsoup.parse(page), "다른 제목", sample.titleLayout())).isInstanceOf(AssertionError.class);
    }
    @Test void titlePolicyAndRequestBudgetsAreUnchanged() throws Exception {
        var decision = new AnnouncementSourceClassificationEngine().selectDecision(new AnnouncementSourceClassificationInput("LOCAL_GOV_NOTICE", sample.title(), null, null,
                List.of(), BodySourceCode.NONE, BodyAvailabilityCode.UNAVAILABLE), AnnouncementAttachmentRealFileQaTest.selectDraftRuleSet());
        assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectTitleMayProceed(decision)).isTrue();
        var budget = AnnouncementAttachmentBbsOfficialObservationTest.selectBudget(sample.profile(), true, false);
        assertThat(budget.maximumRequests).isEqualTo(6); assertThat(budget.maximumBytes).isEqualTo(23 * 1024 * 1024);
    }
}
