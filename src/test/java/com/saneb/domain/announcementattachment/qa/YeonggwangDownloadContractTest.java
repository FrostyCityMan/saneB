package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.domain.announcementattachment.discovery.AttachmentDiscoveryProfile;
import com.saneb.domain.announcementsource.classification.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.provider.content.YeonggwangNoticePage;
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

class YeonggwangDownloadContractTest {
    private final AnnouncementAttachmentBbsOfficialObservationTest.ObservationCase sample = YeonggwangDownloadCases.selectCase();
    private String selectItem(int id,String extension) {
        String name = "지원 공고(청년, 개인) " + id + "." + extension;
        return "<a href=\"javascript:goDownLoad('" + name + "','stored_" + id + "." + extension + "','/ntishome/file/upload/ofr/ofr/20260911')\">" + name + "</a><br>";
    }
    private String selectPage(String files) {
        return "<div id=board_view><table><tr><th>제목</th><td colspan=3>" + sample.title()
                + "</td></tr><tr><th>담당부서</th><td>수출 부서</td><th>등록일</th><td>2026-09-14</td></tr>"
                + "<tr><th>첨부</th><td colspan=3>" + files + "</td></tr><tr><td colspan=4 class='leftcell rightcell'>"
                + "<div class=board_view_contents>청년 지원사업</div></td></tr></table></div>";
    }
    @Test void onlyOfficialCellFilesAreCollectedWithUnknownRoles() {
        var result = sample.profile().selectDescriptors(sample.source(),selectPage(selectItem(1,"pdf") + selectItem(2,"hwp") + selectItem(3,"hwpx")) + "<a href='/other.pdf'>무관한 파일</a>");
        assertThat(result.complete()).isTrue(); assertThat(result.descriptors()).hasSize(3).allSatisfy(file -> {
            assertThat(file.downloadAllowed()).isTrue(); assertThat(file.documentRole()).isEqualTo("UNKNOWN");
            assertThat(file.fetchUri().getHost()).isEqualTo("eminwon.yeonggwang.jeonnam.kr");
            assertThat(file.selectRequest().method()).isEqualTo("GET");
            assertThat(file.locator().toString()).doesNotContain("지원", "stored_", "ntishome");
        });
    }
    @Test void errorsAndUnsupportedFilesDoNotDiscardOtherFiles() {
        for (String bad : List.of("<a href='/unknown'>미확인</a>",selectItem(2,"pdf").replace("/20260911","/../private"),
                selectItem(2,"pdf").replace("javascript:goDownLoad(","javascript:evil();goDownLoad("),selectItem(2,"pdf").replace("<a ","<a onmouseover='evil()' "),
                "<div>" + selectItem(2,"pdf") + "</div>")) {
            var result = sample.profile().selectDescriptors(sample.source(),selectPage(selectItem(1,"pdf") + bad));
            assertThat(result.complete()).isFalse(); assertThat(result.descriptors()).hasSize(1);
        }
        var result = sample.profile().selectDescriptors(sample.source(),selectPage(selectItem(1,"pdf") + selectItem(2,"xlsx")));
        assertThat(result.complete()).isTrue(); assertThat(result.descriptors().getLast().downloadAllowed()).isFalse();
    }
    @Test void emptyAndMissingAttachmentRowsRemainDistinct() {
        var profile = sample.profile(); assertThat(profile.selectDescriptors(sample.source(),selectPage("")).status()).isEqualTo("NO_FILES");
        for (String bad : List.of("<p>에러</p>",selectPage("") + selectPage(""),selectPage("").replace("<th>첨부</th>","<th>기타</th>"),selectPage("<button>다운로드</button>")))
            assertThat(profile.selectDescriptors(sample.source(),bad).complete()).isFalse();
    }
    @Test void duplicateConflictAndLimitAreExplicit() {
        String one = selectItem(1,"pdf"); var profile = sample.profile();
        assertThat(profile.selectDescriptors(sample.source(),selectPage(one + one)).descriptors()).hasSize(1);
        var conflict = profile.selectDescriptors(sample.source(),selectPage(one + one.replace("지원 공고","다른 공고")));
        assertThat(conflict.complete()).isFalse(); assertThat(conflict.descriptors()).hasSize(1);
        var limit = profile.selectDescriptors(sample.source(),selectPage(IntStream.rangeClosed(1,11).mapToObj(i -> selectItem(i,"pdf")).collect(Collectors.joining())));
        assertThat(limit.status()).isEqualTo("LIMIT_EXCEEDED"); assertThat(limit.descriptors()).hasSize(10);
    }
    @Test void sourceIdentityRetainsCollectorCanonicalAndSearchLinks() {
        var source = sample.source(); var profile = sample.profile(); var normalizer = new AnnouncementSourceIdentityNormalizer();
        assertThat(source.providerNoticeId()).isEqualTo(normalizer.hash(source.sourceUrl()));
        String raw = profile.selectDetailUri(source).toString(); assertThat(normalizer.canonicalizeUrl(raw)).isEqualTo(source.sourceUrl());
        String search = raw + "&sc_key=subject&sc_word=%EC%A7%80%EC%9B%90";
        for (String stored : List.of(search,normalizer.canonicalizeUrl(search))) {
            var selected = new AttachmentDiscoveryProfile.Source(source.providerCode(),normalizer.hash(stored),stored,source.localSourceCode(),source.listParserProfileCode());
            assertThat(profile.selectDetailUri(selected)).isEqualTo(URI.create(raw));
        }
        assertThatThrownBy(() -> profile.selectDetailUri(new AttachmentDiscoveryProfile.Source(source.providerCode(),"0".repeat(64),source.sourceUrl(),source.localSourceCode(),source.listParserProfileCode()))).hasMessage("PROFILE_REQUIRED");
    }
    @Test void requestBoundariesAndDifferentFileRedirectsAreRejected() {
        var source = sample.source(); var profile = sample.profile(); String raw = profile.selectDetailUri(source).toString();
        for (String bad : List.of(raw + "&extra=x",raw + "&bs_idx=1",raw + "#x",raw.replace("https:","http:"),
                raw.replace("www.yeonggwang.go.kr","evil.example"),raw.replace("mn=9059","mn=9060"),raw.replace("/bbs/","/%62bs/"),raw + "&sc_key=subject"))
            assertThatThrownBy(() -> YeonggwangNoticePage.selectDetailUri(URI.create(bad))).isInstanceOf(IllegalArgumentException.class);
        var files = profile.selectDescriptors(source,selectPage(selectItem(1,"pdf") + selectItem(2,"pdf"))).descriptors();
        assertThat(profile.selectApprovedRequest(files.getFirst().selectRequest(),files.getLast().selectRequest())).isFalse();
        assertThat(profile.selectApprovedRequest(new Request(files.getFirst().fetchUri(),"POST",Map.of("x","y")))).isFalse();
    }
    @Test void titleBodyAndNestedMetadataStaySeparate() throws Exception {
        var page = Jsoup.parse(selectPage(selectItem(1,"pdf")));
        AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,sample.title(),sample.titleLayout());
        assertThat(YeonggwangNoticePage.selectContent(page).text()).isEqualTo("청년 지원사업");
        var nested = Jsoup.parse(selectPage("").replace("청년 지원사업","<table><tr><th>제목</th><td>본문 표</td></tr></table>"));
        assertThat(YeonggwangNoticePage.selectTitle(YeonggwangNoticePage.selectRoot(nested)).text()).isEqualTo(sample.title());
    }
    @Test void titleStopAndEligibleSupportRemainDifferentWithoutChangingRules() throws Exception {
        var rules = AnnouncementAttachmentRealFileQaTest.selectDraftRuleSet();
        for (boolean stopped : List.of(false,true)) {
            var selected = YeonggwangDownloadCases.selectCase(stopped);
            var decision = new AnnouncementSourceClassificationEngine().selectDecision(new AnnouncementSourceClassificationInput("LOCAL_GOV_NOTICE",selected.title(),null,null,List.of(),AnnouncementSourceClassificationCodes.BodySourceCode.NONE,AnnouncementSourceClassificationCodes.BodyAvailabilityCode.UNAVAILABLE),rules);
            assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectTitleMayProceed(decision)).isEqualTo(!stopped);
        }
    }
    @Test void catalogBudgetAndReferenceDoNotClaimOperatingApproval() throws Exception {
        var json = new ObjectMapper(); var notices = json.readTree(Files.readString(Path.of("src/main/resources/announcement-attachment/provider-qa-catalog-v2.json"))).path("notices");
        for (boolean stopped : List.of(false,true)) {
            var selected = YeonggwangDownloadCases.selectCase(stopped);
            var entry = StreamSupport.stream(notices.spliterator(),false).filter(n -> selected.code().equals(n.path("caseCode").asText())).findFirst().orElseThrow();
            assertThat(entry.path("source")).isEqualTo(json.valueToTree(selected.source())); assertThat(entry.hasNonNull("expectation")).isFalse();
        }
        assertThat(Files.readString(Path.of("src/main/resources/db/migration/V61__correct_general_notice_sources_to_official_legal_boards.sql"))).contains("'LGS-000195', '" + sample.listUrl() + "'");
        var budget = AnnouncementAttachmentBbsOfficialObservationTest.selectBudget(sample.profile(),true,false);
        assertThat(budget.maximumRequests).isEqualTo(6); assertThat(budget.maximumBytes).isEqualTo(23L*1024*1024);
    }
}
