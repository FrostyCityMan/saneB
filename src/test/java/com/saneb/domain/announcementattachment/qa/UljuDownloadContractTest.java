package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.domain.announcementattachment.discovery.AttachmentDiscoveryProfile;
import com.saneb.domain.announcementsource.classification.*;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.provider.content.UljuNoticePage;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

class UljuDownloadContractTest {
    private final AnnouncementAttachmentBbsOfficialObservationTest.ObservationCase sample = UljuDownloadCases.selectCase();
    private String selectItem(int id,String extension) {
        String name = "지원 공고 " + id + "." + extension, stored = "stored_" + id + "." + extension, path = "/ntishome/file/upload/ofr/ofr/20260723";
        return "<li><a href='#' onclick=\"goDownload_location('" + name + "','" + stored + "','" + path + "'); return false;\"><span>" + name
                + "</span></a><a class=bt_white_s href='#' onclick=\"downloadToHttp('67082','" + stored + "','" + path + "'); return false;\"><span class=blank>바로 보기</span></a></li>";
    }
    private String selectPage(String files) {
        return "<form id=detailForm><div class=bod_wrap><div class=bod_view><h4>" + sample.title()
                + "</h4><div class=view_info>수출 부서</div><div class=view_cont>소상공인 자금지원</div>"
                + "<dl class=view_file><dt><span>첨부 파일</span></dt><dd><div><ul id=updateFileList>" + files + "</ul></div></dd></dl></div></div></form>";
    }
    @Test void collectsSupportedFormatsWithoutRequestingPreview() {
        var result = sample.profile().selectDescriptors(sample.source(),selectPage(selectItem(1,"hwp") + selectItem(2,"hwpx") + selectItem(3,"pdf")));
        assertThat(result.complete()).isTrue(); assertThat(result.descriptors()).hasSize(3).allSatisfy(file -> {
            assertThat(file.downloadAllowed()).isTrue(); assertThat(file.documentRole()).isEqualTo("UNKNOWN");
            assertThat(file.fetchUri().getHost()).isEqualTo("eminwon.ulju.ulsan.kr");
            assertThat(file.selectRequest().method()).isEqualTo("GET");
            assertThat(file.locator().toString()).doesNotContain("지원", "stored_", "ntishome");
        });
    }
    @Test void previewErrorsDoNotDiscardTheVerifiedDownload() {
        for (String bad : List.of(selectItem(1,"pdf").replace("downloadToHttp('67082'","downloadToHttp('67083'"),
                selectItem(1,"pdf").replace("downloadToHttp(","evil();downloadToHttp("),
                selectItem(1,"pdf").replace("class=bt_white_s","class=bt_white_s onmouseover='evil()'"))) {
            var result = sample.profile().selectDescriptors(sample.source(),selectPage(bad));
            assertThat(result.complete()).isFalse(); assertThat(result.descriptors()).hasSize(1);
        }
    }
    @Test void badLinksAndUnsupportedFilesAreSeparateFromGoodFiles() {
        for (String bad : List.of("<li><a href='/unknown'>미확인</a></li>",selectItem(2,"pdf").replace("/20260723","/../private"),
                selectItem(2,"pdf").replace("goDownload_location(","evil();goDownload_location("),
                selectItem(2,"pdf").replace("<span>지원","<span onmouseover='evil()'>지원"))) {
            var result = sample.profile().selectDescriptors(sample.source(),selectPage(selectItem(1,"hwp") + bad));
            assertThat(result.complete()).isFalse(); assertThat(result.descriptors()).hasSize(1);
        }
        var result = sample.profile().selectDescriptors(sample.source(),selectPage(selectItem(1,"hwp") + selectItem(2,"xlsx")));
        assertThat(result.complete()).isTrue(); assertThat(result.descriptors().getLast().downloadAllowed()).isFalse();
    }
    @Test void emptyChangedDuplicateAndLimitRemainDistinct() {
        var profile = sample.profile();
        assertThat(profile.selectDescriptors(sample.source(),selectPage("")).status()).isEqualTo("NO_FILES");
        for (String bad : List.of("<p>에러</p>",selectPage("") + selectPage(""),selectPage("").replace("첨부 파일","파일"),selectPage("<li></li>")))
            assertThat(profile.selectDescriptors(sample.source(),bad).complete()).isFalse();
        String one = selectItem(1,"hwp");
        assertThat(profile.selectDescriptors(sample.source(),selectPage(one + one)).descriptors()).hasSize(1);
        var conflict = profile.selectDescriptors(sample.source(),selectPage(one + one.replace("지원 공고","다른 공고")));
        assertThat(conflict.complete()).isFalse(); assertThat(conflict.descriptors()).hasSize(1);
        var limit = profile.selectDescriptors(sample.source(),selectPage(IntStream.rangeClosed(1,11).mapToObj(i->selectItem(i,"pdf")).collect(Collectors.joining())));
        assertThat(limit.status()).isEqualTo("LIMIT_EXCEEDED"); assertThat(limit.descriptors()).hasSize(10);
    }
    @Test void requestAndIdentityStayBoundToTheOfficialNotice() {
        var source = sample.source(); var profile = sample.profile();
        assertThat(profile.selectDetailUri(source).toString()).isEqualTo(source.sourceUrl());
        assertThat(UljuNoticePage.selectDetailUri(URI.create(source.sourceUrl().replace("0403020000","0403010000"))).toString()).contains("0403010000");
        for (String bad : List.of(source.sourceUrl() + "&token=x",source.sourceUrl() + "&notAncmtMgtNo=1",source.sourceUrl() + "#x",
                source.sourceUrl().replace("https:","http:"),source.sourceUrl().replace("www.ulju.ulsan.kr","evil.example"),
                source.sourceUrl().replace("0403020000","0403030000"),source.sourceUrl().replace("/ulju/","/%75lju/")))
            assertThat(profile.selectApprovedRequest(URI.create(bad))).isFalse();
        assertThat(profile.selectApprovedRequest(URI.create("https:opaque"))).isFalse();
        assertThatThrownBy(() -> profile.selectDetailUri(new AttachmentDiscoveryProfile.Source(source.providerCode(),source.providerNoticeId(),source.sourceUrl(),"LGS-OTHER",source.listParserProfileCode()))).hasMessage("PROFILE_REQUIRED");
        var files = profile.selectDescriptors(source,selectPage(selectItem(1,"pdf") + selectItem(2,"pdf"))).descriptors();
        assertThat(profile.selectApprovedRequest(files.getFirst().selectRequest(),files.getLast().selectRequest())).isFalse();
        assertThat(profile.selectApprovedRequest(new Request(files.getFirst().fetchUri(),"POST",Map.of("x","y")))).isFalse();
    }
    @Test void bodyAndTitleAreNotTheMetadataOrAttachmentText() throws Exception {
        var page = Jsoup.parse(selectPage(selectItem(1,"hwp")) + "<a href='/unrelated.pdf'>무관한 파일</a>");
        AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,sample.title(),sample.titleLayout());
        assertThat(UljuNoticePage.selectContent(page).text()).isEqualTo("소상공인 자금지원");
        assertThatThrownBy(() -> AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,"다른 제목",sample.titleLayout())).isInstanceOf(AssertionError.class);
        var decision = new AnnouncementSourceClassificationEngine().selectDecision(new AnnouncementSourceClassificationInput("LOCAL_GOV_NOTICE",sample.title(),null,null,List.of(),AnnouncementSourceClassificationCodes.BodySourceCode.NONE,AnnouncementSourceClassificationCodes.BodyAvailabilityCode.UNAVAILABLE),AnnouncementAttachmentRealFileQaTest.selectDraftRuleSet());
        assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectTitleMayProceed(decision)).isTrue();
    }
    @Test void catalogBudgetAndReferenceDoNotClaimOperatingApproval() throws Exception {
        var json = new ObjectMapper(); var notices = json.readTree(Files.readString(Path.of("src/main/resources/announcement-attachment/provider-qa-catalog-v2.json"))).path("notices");
        var entry = StreamSupport.stream(notices.spliterator(),false).filter(n->sample.code().equals(n.path("caseCode").asText())).findFirst().orElseThrow();
        assertThat(entry.path("source")).isEqualTo(json.valueToTree(sample.source())); assertThat(entry.hasNonNull("expectation")).isFalse();
        assertThat(Files.readString(Path.of("src/main/resources/db/migration/V61__correct_general_notice_sources_to_official_legal_boards.sql"))).contains("'LGS-000082', '" + sample.listUrl() + "'");
        var budget = AnnouncementAttachmentBbsOfficialObservationTest.selectBudget(sample.profile(),true,false);
        assertThat(budget.maximumRequests).isEqualTo(6); assertThat(budget.maximumBytes).isEqualTo(23L*1024*1024);
        assertThat(sample.profile().selectLegacyBinaryContentTypes()).isEmpty();
    }
}
