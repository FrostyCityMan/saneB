package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.domain.announcementattachment.discovery.AttachmentDiscoveryProfile;
import com.saneb.domain.announcementsource.classification.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.provider.content.HwaseongNoticePage;
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

class HwaseongDownloadContractTest {
    private final AnnouncementAttachmentBbsOfficialObservationTest.ObservationCase sample = HwaseongDownloadCases.selectCase();
    private String selectItem(int id,String extension) {
        String args = "'지원 공고 " + id + "." + extension + "','stored_" + id + "." + extension + "','/ntishome/file/upload/ofr/ofr/20260122'";
        return "<li><img src='/resources/health/img/sub/file_icon.png' alt=''><a href=\"javascript:goDownLoad(" + args
                + ")\">지원 공고 " + id + "." + extension + "</a><button class=btn-view onclick=\"call_viewer('141781'," + args
                + ")\">바로보기</button><button class=btn-view onclick=\"call_viewer_tts('141781'," + args + ")\">바로듣기</button></li>";
    }
    private String selectPage(String files) {
        return "<div class='board_write mt_18'><table><tr><th>제목</th><td colspan=3>" + sample.title()
                + "</td></tr><tr><th>담당부서</th><td>수출 부서</td></tr><tr><th>첨부파일</th><td colspan=3><div class=file_down><ul>"
                + files + "</ul></div></td></tr><tr><th>내용</th><td colspan=3><div class=txt>소상공인 자금지원</div></td></tr></table></div>";
    }

    @Test void collectsThreeFormatsWithoutFetchingTheMatchingPreviewButtons() {
        var result = sample.profile().selectDescriptors(sample.source(),selectPage(selectItem(1,"hwp") + selectItem(2,"hwpx") + selectItem(3,"pdf")));
        assertThat(result.complete()).isTrue();
        assertThat(result.descriptors()).hasSize(3).allSatisfy(file -> {
            assertThat(file.downloadAllowed()).isTrue();
            assertThat(file.documentRole()).isEqualTo("UNKNOWN");
            assertThat(file.selectRequest().method()).isEqualTo("GET");
            assertThat(file.fetchUri().getHost()).isEqualTo("eminwon.hscity.go.kr");
            assertThat(file.fetchUri().getPath()).isEqualTo("/emwp/jsp/ofr/FileDown.jsp");
            assertThat(file.locator().toString()).doesNotContain("지원", "stored_", "ntishome");
        });
    }

    @Test void badPreviewDoesNotDiscardTheGoodDownloadOrBecomeComplete() {
        for (String bad : List.of(selectItem(1,"pdf").replace("call_viewer('141781'","call_viewer('141782'"),
                selectItem(1,"pdf").replace("call_viewer_tts(","evil();call_viewer_tts("),
                selectItem(1,"pdf").replace("<button ","<button onmouseover='evil()' "),
                selectItem(1,"pdf").replace("file_icon.png","other.png"))) {
            var result = sample.profile().selectDescriptors(sample.source(),selectPage(bad));
            assertThat(result.complete()).isFalse(); assertThat(result.descriptors()).hasSize(1);
        }
    }

    @Test void invalidDownloadAndUnsupportedFileDoNotDiscardOtherGoodFiles() {
        for (String bad : List.of("<li><a href='/unknown'>미확인</a></li>",selectItem(2,"pdf").replace("/20260122","/../private"),
                selectItem(2,"pdf").replace("javascript:goDownLoad(","javascript:evil();goDownLoad("))) {
            var result = sample.profile().selectDescriptors(sample.source(),selectPage(selectItem(1,"hwp") + bad));
            assertThat(result.complete()).isFalse(); assertThat(result.descriptors()).hasSize(1);
        }
        var result = sample.profile().selectDescriptors(sample.source(),selectPage(selectItem(1,"hwp") + selectItem(2,"xlsx")));
        assertThat(result.complete()).isTrue(); assertThat(result.descriptors().getLast().downloadAllowed()).isFalse();
    }

    @Test void emptyChangedDuplicateAndLimitedSetsRemainDistinct() {
        var profile = sample.profile();
        assertThat(profile.selectDescriptors(sample.source(),selectPage("")).status()).isEqualTo("NO_FILES");
        for (String bad : List.of("<p>에러</p>",selectPage("") + selectPage(""),selectPage("").replace("첨부파일","파일"),selectPage("<li></li>")))
            assertThat(profile.selectDescriptors(sample.source(),bad).complete()).isFalse();
        String one = selectItem(1,"hwp");
        assertThat(profile.selectDescriptors(sample.source(),selectPage(one + one)).descriptors()).hasSize(1);
        var conflict = profile.selectDescriptors(sample.source(),selectPage(one + one.replace("지원 공고","다른 공고")));
        assertThat(conflict.complete()).isFalse(); assertThat(conflict.descriptors()).hasSize(1);
        var limit = profile.selectDescriptors(sample.source(),selectPage(IntStream.rangeClosed(1,11).mapToObj(i->selectItem(i,"pdf")).collect(Collectors.joining())));
        assertThat(limit.status()).isEqualTo("LIMIT_EXCEEDED"); assertThat(limit.descriptors()).hasSize(10);
    }

    @Test void detailUrlPreservesDbTemplateIdentityAndRejectsOtherHostsOrQueries() {
        var source = sample.source(); var profile = sample.profile(); var normalizer = new AnnouncementSourceIdentityNormalizer();
        assertThat(profile.selectDetailUri(source).toString()).isEqualTo(source.sourceUrl());
        String categoryUrl = source.sourceUrl() + "&q_notAncmtSeCode=04";
        assertThat(profile.selectDetailUri(new AttachmentDiscoveryProfile.Source(source.providerCode(),normalizer.hash(normalizer.canonicalizeUrl(categoryUrl)),categoryUrl,source.localSourceCode(),source.listParserProfileCode()))).isEqualTo(URI.create(source.sourceUrl()));
        for (String bad : List.of(source.sourceUrl() + "&extra=1",source.sourceUrl() + "&q_notAncmtMgtNo=2",
                source.sourceUrl().replace("https:","http:"),source.sourceUrl().replace("/www/","/%77ww/"),
                source.sourceUrl().replace("www.hscity.go.kr","evil.example"),source.sourceUrl() + "#x"))
            assertThat(profile.selectApprovedRequest(URI.create(bad))).isFalse();
        assertThat(profile.selectApprovedRequest(URI.create("https:opaque"))).isFalse();
        assertThatThrownBy(() -> profile.selectDetailUri(new AttachmentDiscoveryProfile.Source(source.providerCode(),source.providerNoticeId(),source.sourceUrl(),"LGS-OTHER",source.listParserProfileCode()))).hasMessage("PROFILE_REQUIRED");
        var files = profile.selectDescriptors(source,selectPage(selectItem(1,"pdf") + selectItem(2,"pdf"))).descriptors();
        assertThat(profile.selectApprovedRequest(files.getFirst().selectRequest(),files.getLast().selectRequest())).isFalse();
        assertThat(profile.selectApprovedRequest(new Request(files.getFirst().fetchUri(),"POST",Map.of("x","y")))).isFalse();
        assertThat(normalizer.hash(normalizer.canonicalizeUrl(source.sourceUrl().replace("141781","141338")))).isNotEqualTo(source.providerNoticeId());
    }

    @Test void titleBodyAndAttachmentsKeepTheirOwnTableBoundaries() throws Exception {
        var page = Jsoup.parse(selectPage(selectItem(1,"hwp")) + "<a href='/unrelated.pdf'>무관한 파일</a>");
        AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,sample.title(),sample.titleLayout());
        assertThat(HwaseongNoticePage.selectContent(page).text()).isEqualTo("소상공인 자금지원");
        assertThatThrownBy(() -> AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,"다른 제목",sample.titleLayout())).isInstanceOf(AssertionError.class);
        var nested = Jsoup.parse(selectPage("").replace("소상공인 자금지원</div>","<table><tr><th>제목</th><td>본문 표</td></tr></table></div>"));
        assertThat(HwaseongNoticePage.selectTitle(HwaseongNoticePage.selectRoot(nested)).text()).isEqualTo(sample.title());
        var decision = new AnnouncementSourceClassificationEngine().selectDecision(new AnnouncementSourceClassificationInput("LOCAL_GOV_NOTICE",sample.title(),null,null,List.of(),AnnouncementSourceClassificationCodes.BodySourceCode.NONE,AnnouncementSourceClassificationCodes.BodyAvailabilityCode.UNAVAILABLE),AnnouncementAttachmentRealFileQaTest.selectDraftRuleSet());
        assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectTitleMayProceed(decision)).isTrue();
    }

    @Test void catalogAndBudgetDoNotClaimPolicyApprovalOrOperatingCollection() throws Exception {
        var json = new ObjectMapper(); var notices = json.readTree(Files.readString(Path.of("src/main/resources/announcement-attachment/provider-qa-catalog-v2.json"))).path("notices");
        var entry = StreamSupport.stream(notices.spliterator(),false).filter(n->sample.code().equals(n.path("caseCode").asText())).findFirst().orElseThrow();
        assertThat(entry.path("source")).isEqualTo(json.valueToTree(sample.source())); assertThat(entry.hasNonNull("expectation")).isFalse();
        assertThat(Files.readString(Path.of("src/main/resources/db/migration/V61__correct_general_notice_sources_to_official_legal_boards.sql")))
                .contains("'BD_selectNoticeDetail.do?q_notAncmtMgtNo={arg:1}'", "'LGS-000088', '" + sample.listUrl() + "'");
        var budget = AnnouncementAttachmentBbsOfficialObservationTest.selectBudget(sample.profile(),true,false);
        assertThat(budget.maximumRequests).isEqualTo(6); assertThat(budget.maximumBytes).isEqualTo(23L*1024*1024);
        assertThat(sample.profile().selectLegacyBinaryContentTypes()).isEmpty(); assertThat(sample.profile().selectUtf8DispositionOctets()).isFalse();
    }
}
