package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.domain.announcementattachment.discovery.AttachmentDiscoveryProfile;
import com.saneb.domain.announcementsource.classification.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.provider.content.SeoulGangseoNoticePage;
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

class SeoulGangseoDownloadContractTest {
    private final AnnouncementAttachmentBbsOfficialObservationTest.ObservationCase sample = SeoulGangseoDownloadCases.selectCase();
    private String selectItem(int id,String extension) {
        String name = "지원 공고(청년, 개인) " + id + "." + extension;
        return "<li><span class=name>" + name + " (176KB)</span><span class=download><a href=\"javascript:void(0);\" onclick=\"goDownLoad('"
                + name + "','stored_" + id + "." + extension + "','/ntishome/file/upload/ofr/ofr/20260911')\">다운로드</a></span></li>";
    }
    private String selectPage(String files) {
        return "<div class=board-view-wrap><div class=board-view-head><div class=top-element><div class=subject>" + sample.title()
                + "</div></div><div class=board-info>수출 부서</div></div><div class=board-view-body><div class=view-content>"
                + "<div class=gosi-con><pre>청년 지원사업</pre></div></div><div class=file-element><dl><dt>첨부파일</dt><dd><ul>"
                + files + "</ul></dd></dl></div></div></div>";
    }
    @Test void onlyOfficialAttachmentsProduceFileRequests() {
        var result = sample.profile().selectDescriptors(sample.source(),selectPage(selectItem(1,"pdf") + selectItem(2,"hwp") + selectItem(3,"hwpx")) + "<a href='/other.pdf'>무관</a>");
        assertThat(result.complete()).isTrue(); assertThat(result.descriptors()).hasSize(3).allSatisfy(file -> {
            assertThat(file.downloadAllowed()).isTrue(); assertThat(file.documentRole()).isEqualTo("UNKNOWN");
            assertThat(file.fetchUri().getHost()).isEqualTo("eminwon.gangseo.seoul.kr");
            assertThat(file.selectRequest().method()).isEqualTo("GET"); assertThat(sample.profile().selectApprovedRequest(file.selectRequest())).isTrue();
            assertThat(file.locator().toString()).doesNotContain("지원", "stored_", "ntishome");
        });
    }
    @Test void badFilesAndActiveMarkupDoNotDiscardOtherGoodFiles() {
        for (String bad : List.of("<a href='/unknown'>미확인</a>",selectItem(2,"pdf").replace("/20260911","/../private"),
                selectItem(2,"pdf").replace("goDownLoad(","evil();goDownLoad("),selectItem(2,"pdf").replace("<a ","<a onmouseover='evil()' "),
                selectItem(2,"pdf").replace(" (176KB)"," 다른 파일"),"<div>" + selectItem(2,"pdf") + "</div>")) {
            var result = sample.profile().selectDescriptors(sample.source(),selectPage(selectItem(1,"pdf") + bad));
            assertThat(result.complete()).isFalse(); assertThat(result.descriptors()).hasSize(1);
        }
        var unsupported = sample.profile().selectDescriptors(sample.source(),selectPage(selectItem(1,"pdf") + selectItem(2,"xlsx")));
        assertThat(unsupported.complete()).isTrue(); assertThat(unsupported.descriptors().getLast().downloadAllowed()).isFalse();
        var extra = sample.profile().selectDescriptors(sample.source(),selectPage(selectItem(1,"pdf").replace("</li>","<span>미확인 첨부 정보</span></li>")));
        assertThat(extra.complete()).isFalse(); assertThat(extra.descriptors()).hasSize(1);
    }
    @Test void emptyAndMissingAttachmentAreasAreDifferent() {
        var profile = sample.profile(); assertThat(profile.selectDescriptors(sample.source(),selectPage("")).status()).isEqualTo("NO_FILES");
        for (String bad : List.of("<p>에러</p>",selectPage("") + selectPage(""),selectPage("").replace("<dt>첨부파일</dt>","<dt>기타</dt>"),selectPage("<button>다운로드</button>")))
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
    @Test void storedIdentityAndSearchLinksResolveToFixedNotice() {
        var source = sample.source(); var profile = sample.profile(); var normalizer = new AnnouncementSourceIdentityNormalizer();
        assertThat(source.providerNoticeId()).isEqualTo(normalizer.hash(source.sourceUrl()));
        String raw = "https://www.gangseo.seoul.kr/gs040301/view?mgtNo=66840";
        String search = raw + "&srchPage=&curPage=1&srchKey=&srchText=%EC%A7%80%EC%9B%90";
        for (String stored : List.of(source.sourceUrl(),search,normalizer.canonicalizeUrl(search))) {
            var selected = new AttachmentDiscoveryProfile.Source(source.providerCode(),normalizer.hash(stored),stored,source.localSourceCode(),source.listParserProfileCode());
            assertThat(profile.selectDetailUri(selected)).isEqualTo(URI.create(raw));
        }
        assertThatThrownBy(() -> profile.selectDetailUri(new AttachmentDiscoveryProfile.Source(source.providerCode(),"0".repeat(64),source.sourceUrl(),source.localSourceCode(),source.listParserProfileCode()))).hasMessage("PROFILE_REQUIRED");
    }
    @Test void requestBoundariesAndDifferentFileRedirectsAreRejected() {
        var source = sample.source(); var profile = sample.profile(); String raw = profile.selectDetailUri(source).toString();
        for (String bad : List.of(raw + "&extra=x",raw + "&mgtNo=1",raw + "#x",raw.replace("https:","http:"),
                raw.replace("www.gangseo.seoul.kr","evil.example"),raw.replace("/view","/%76iew"),raw + "&curPage=evil"))
            assertThatThrownBy(() -> SeoulGangseoNoticePage.selectDetailUri(URI.create(bad))).isInstanceOf(IllegalArgumentException.class);
        var files = profile.selectDescriptors(source,selectPage(selectItem(1,"pdf") + selectItem(2,"pdf"))).descriptors();
        assertThat(profile.selectApprovedRequest(files.getFirst().selectRequest(),files.getLast().selectRequest())).isFalse();
        assertThat(profile.selectApprovedRequest(new Request(files.getFirst().fetchUri(),"POST",Map.of("x","y")))).isFalse();
        assertThat(profile.selectApprovedRequest(URI.create(files.getFirst().fetchUri() + "&extra=x"))).isFalse();
    }
    @Test void titleAndBodyStaySeparateFromMetadata() throws Exception {
        var page = Jsoup.parse(selectPage(selectItem(1,"pdf")));
        AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,sample.title(),sample.titleLayout());
        assertThat(SeoulGangseoNoticePage.selectContent(page).text()).isEqualTo("청년 지원사업");
        var decision = new AnnouncementSourceClassificationEngine().selectDecision(new AnnouncementSourceClassificationInput("LOCAL_GOV_NOTICE",sample.title(),null,null,List.of(),AnnouncementSourceClassificationCodes.BodySourceCode.NONE,AnnouncementSourceClassificationCodes.BodyAvailabilityCode.UNAVAILABLE),AnnouncementAttachmentRealFileQaTest.selectDraftRuleSet());
        assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectTitleMayProceed(decision)).isTrue();
    }
    @Test void catalogBudgetAndReferenceDoNotClaimOperatingApproval() throws Exception {
        var json = new ObjectMapper(); var notices = json.readTree(Files.readString(Path.of("src/main/resources/announcement-attachment/provider-qa-catalog-v2.json"))).path("notices");
        var entry = StreamSupport.stream(notices.spliterator(),false).filter(n -> sample.code().equals(n.path("caseCode").asText())).findFirst().orElseThrow();
        assertThat(entry.path("source")).isEqualTo(json.valueToTree(sample.source())); assertThat(entry.hasNonNull("expectation")).isFalse();
        assertThat(Files.readString(Path.of("src/main/resources/db/migration/V36__correct_official_local_government_notice_urls.sql"))).contains("'LGS-000017', '" + sample.listUrl() + "'");
        var budget = AnnouncementAttachmentBbsOfficialObservationTest.selectBudget(sample.profile(),true,false);
        assertThat(budget.maximumRequests).isEqualTo(6); assertThat(budget.maximumBytes).isEqualTo(23L*1024*1024);
    }
}
