package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.domain.announcementattachment.discovery.AttachmentDiscoveryProfile;
import com.saneb.domain.announcementsource.classification.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.provider.content.UlsanDongguNoticePage;
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

class UlsanDongguDownloadContractTest {
    @org.junit.jupiter.api.io.TempDir Path temporary;
    private final AnnouncementAttachmentBbsOfficialObservationTest.ObservationCase sample = UlsanDongguDownloadCases.selectCase();

    @Test void officialReadPostIsBoundToTheSameNoticeAndWorkerFlow() throws Exception {
        var profile = sample.profile();
        var initial = Request.selectGet(profile.selectDetailUri(sample.source()));
        var post = UlsanDongguNoticePage.selectRequest(initial.uri());
        assertThat(post.method()).isEqualTo("POST");
        assertThat(post.form()).hasSize(7);
        assertThat(post.uri().getRawQuery()).isNull();
        assertThat(profile.selectApprovedRequest(initial,initial)).isFalse();
        assertThat(profile.selectApprovedRequest(initial,post)).isTrue();
        var changed = new java.util.HashMap<>(post.form());
        changed.put("not_ancmt_mgt_no","27835");
        assertThat(profile.selectApprovedRequest(initial,new Request(post.uri(),"POST",changed))).isFalse();
        changed.put("method","deleteOfrNotAncmt");
        assertThat(profile.selectApprovedRequest(new Request(post.uri(),"POST",changed))).isFalse();
        var calls = new java.util.ArrayList<Request>();
        com.saneb.domain.announcementattachment.discovery.AttachmentProfileDownloadFlow.selectDownload(
                profile,initial,temporary.resolve("detail"),1024,(request,limit,approved)->{
                    assertThat(approved.test(request)).isTrue();
                    assertThat(approved.test(initial)).isFalse();
                    calls.add(request);return null;
                });
        assertThat(calls).containsExactly(post);
    }

    @Test void cardSupportKeepsThreeListedFilesAndTheExistingTitlePolicy() throws Exception {
        var card = UlsanDongguDownloadCases.selectCase(true);
        assertThat(card.listedFileCount()).isEqualTo(3);
        assertThat(card.source().providerNoticeId()).isNotEqualTo(sample.source().providerNoticeId());
        assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectCases("ULSAN_DONGGU_CARD").map(c->c.code()).toList()).containsExactly(card.code());
        var decision = new AnnouncementSourceClassificationEngine().selectDecision(new AnnouncementSourceClassificationInput("LOCAL_GOV_NOTICE",card.title(),null,null,List.of(),AnnouncementSourceClassificationCodes.BodySourceCode.NONE,AnnouncementSourceClassificationCodes.BodyAvailabilityCode.UNAVAILABLE),AnnouncementAttachmentRealFileQaTest.selectDraftRuleSet());
        assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectTitleMayProceed(decision)).isTrue();
    }

    private String selectItem(int id, String extension) {
        return "<a href='#download' onclick=\"goDownLoad('지원 공고 " + id + "." + extension
                + "','stored_" + id + "." + extension + "','/ntishome/file/upload/ofr/ofr/20260930'); return false;\" onkeypress=''>"
                + "지원 공고 " + id + "." + extension + "</a><br>";
    }

    private String selectPage(String files) {
        return "<form name=form1 method=post><div id=viewTable1vw><div class='bbs_detail bbs_detail_basic'>"
                + "<div class=bbs_detail_tit><h2>" + sample.title() + "</h2><span>수출 담당</span></div>"
                + "<div class=bbs_detail_file id=download>" + files + "</div>"
                + "<div class=bbs_detail_content>소상공인 융자지원</div><div class=bbs_btn>메뉴</div></div></div></form>";
    }

    @Test void discoversOfficialFilesWithoutInferringDocumentRole() {
        var result = sample.profile().selectDescriptors(sample.source(), selectPage(selectItem(1,"hwpx") + selectItem(2,"pdf") + selectItem(3,"hwp")));
        assertThat(result.complete()).isTrue();
        assertThat(result.descriptors()).hasSize(3).allSatisfy(file -> {
            assertThat(file.documentRole()).isEqualTo("UNKNOWN");
            assertThat(file.downloadAllowed()).isTrue();
            assertThat(file.selectRequest().method()).isEqualTo("GET");
            assertThat(file.fetchUri().getHost()).isEqualTo(UlsanDongguNoticePage.HOST);
            assertThat(sample.profile().selectApprovedRequest(file.selectRequest(),file.selectRequest())).isTrue();
            assertThat(file.locator().toString()).doesNotContain("지원", "stored_", "ntishome");
        });
    }

    @Test void preservesGoodFilesWhenAnotherLinkOrFormatIsInvalid() {
        for (String invalid : List.of("<a href='/unknown'>미확인</a>", "<button>첨부</button>",
                selectItem(2,"pdf").replace("onkeypress=''", "onkeypress='evil()'"),
                selectItem(2,"pdf").replace("goDownLoad(", "evil();goDownLoad("),
                selectItem(2,"pdf").replace("/20260930", "/../private"),
                selectItem(2,"pdf").replace("href='#download'", "href='https://evil.example'"))) {
            var result = sample.profile().selectDescriptors(sample.source(),selectPage(selectItem(1,"hwp") + invalid));
            assertThat(result.complete()).isFalse();
            assertThat(result.descriptors()).hasSize(1);
        }
        var unsupported = sample.profile().selectDescriptors(sample.source(), selectPage(selectItem(1,"pdf") + selectItem(2,"xlsx")));
        assertThat(unsupported.complete()).isTrue();
        assertThat(unsupported.descriptors().getLast().downloadAllowed()).isFalse();
    }

    @Test void emptyChangedDuplicateAndLimitedSetsRemainDistinct() {
        var profile = sample.profile();
        assertThat(profile.selectDescriptors(sample.source(),selectPage("")).status()).isEqualTo("NO_FILES");
        for (String invalid : List.of("<p>에러</p>", selectPage("") + selectPage(""), selectPage("").replace("id=download", "id=other")))
            assertThat(profile.selectDescriptors(sample.source(),invalid).complete()).isFalse();
        String one = selectItem(1,"hwpx");
        assertThat(profile.selectDescriptors(sample.source(),selectPage(one + one)).descriptors()).hasSize(1);
        var conflict = profile.selectDescriptors(sample.source(),selectPage(one + one.replace("지원 공고", "다른 공고")));
        assertThat(conflict.complete()).isFalse();
        assertThat(conflict.descriptors()).hasSize(1);
        var limit = profile.selectDescriptors(sample.source(),selectPage(IntStream.rangeClosed(1,11).mapToObj(i->selectItem(i,"pdf")).collect(Collectors.joining())));
        assertThat(limit.status()).isEqualTo("LIMIT_EXCEEDED");
        assertThat(limit.descriptors()).hasSize(10);
    }

    @Test void bindsSourceAndRejectsAdditionalQueriesAndFileRedirects() {
        var profile = sample.profile(); var source = sample.source();
        assertThat(profile.selectDetailUri(source).toString()).isEqualTo(source.sourceUrl());
        for (String invalid : List.of(source.sourceUrl() + "&extra=x", source.sourceUrl() + "&not_ancmt_mgt_no=2",
                source.sourceUrl().replace("https:","http:"), source.sourceUrl().replace("/emwp/","/%65mwp/"),
                source.sourceUrl().replace("01%2C03%2C04%2C05","01"), source.sourceUrl().replace("eminwon.donggu.ulsan.kr","evil.example")))
            assertThat(profile.selectApprovedRequest(URI.create(invalid))).isFalse();
        assertThat(profile.selectApprovedRequest(URI.create("https:opaque"))).isFalse();
        assertThatThrownBy(() -> profile.selectDetailUri(new AttachmentDiscoveryProfile.Source(source.providerCode(),source.providerNoticeId(),source.sourceUrl(),"LGS-000021",source.listParserProfileCode()))).hasMessage("PROFILE_REQUIRED");
        assertThatThrownBy(() -> profile.selectDetailUri(new AttachmentDiscoveryProfile.Source(source.providerCode(),"wrong",source.sourceUrl(),source.localSourceCode(),source.listParserProfileCode()))).hasMessage("PROFILE_REQUIRED");
        var files = profile.selectDescriptors(source,selectPage(selectItem(1,"pdf") + selectItem(2,"pdf"))).descriptors();
        assertThat(profile.selectApprovedRequest(files.getFirst().selectRequest(),files.getLast().selectRequest())).isFalse();
        assertThat(profile.selectApprovedRequest(new Request(files.getFirst().fetchUri(),"POST",Map.of("x","y")))).isFalse();
        for (String invalid : List.of(files.getFirst().fetchUri() + "&extra=x", files.getFirst().fetchUri() + "#fragment",
                files.getFirst().fetchUri().toString().replace("/jsp/","/%6asp/"))) assertThat(profile.selectApprovedRequest(URI.create(invalid))).isFalse();
    }

    @Test void titleBodyAttachmentsAndNoticeIdentityStaySeparate() throws Exception {
        var page = Jsoup.parse(selectPage(selectItem(1,"hwpx")) + "<a href='/unrelated.pdf'>무관한 파일</a>");
        AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,sample.title(),sample.titleLayout());
        assertThat(UlsanDongguNoticePage.selectContent(page).text()).isEqualTo("소상공인 융자지원");
        assertThatThrownBy(() -> AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,"다른 제목",sample.titleLayout())).isInstanceOf(AssertionError.class);
        assertThat(sample.profile().selectDescriptors(sample.source(),page.toString()).descriptors()).hasSize(1);
        var normalizer = new AnnouncementSourceIdentityNormalizer();
        assertThat(normalizer.hash(normalizer.canonicalizeUrl(sample.source().sourceUrl().replace("28029","28030")))).isNotEqualTo(sample.source().providerNoticeId());
        var decision = new AnnouncementSourceClassificationEngine().selectDecision(new AnnouncementSourceClassificationInput("LOCAL_GOV_NOTICE",sample.title(),null,null,List.of(),AnnouncementSourceClassificationCodes.BodySourceCode.NONE,AnnouncementSourceClassificationCodes.BodyAvailabilityCode.UNAVAILABLE),AnnouncementAttachmentRealFileQaTest.selectDraftRuleSet());
        assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectTitleMayProceed(decision)).isTrue();
    }

    @Test void usesRegisteredEndpointAndReferenceCatalogWithoutApproval() throws Exception {
        assertThat(sample.listUrl()).isEqualTo("https://" + UlsanDongguNoticePage.HOST + UlsanDongguNoticePage.DETAIL);
        String migration = Files.readString(Path.of("src/main/resources/db/migration/V61__correct_general_notice_sources_to_official_legal_boards.sql"));
        assertThat(migration.lines().filter(line->line.contains("LGS-000080")).toList()).anySatisfy(line->assertThat(line).contains("'"+sample.listUrl()+"'"));
        var json = new ObjectMapper();
        var notices = json.readTree(Files.readString(Path.of("src/main/resources/announcement-attachment/provider-qa-catalog-v2.json"))).path("notices");
        var entry = StreamSupport.stream(notices.spliterator(),false).filter(n->sample.code().equals(n.path("caseCode").asText())).findFirst().orElseThrow();
        assertThat(entry.path("source")).isEqualTo(json.valueToTree(sample.source()));
        assertThat(entry.hasNonNull("expectation")).isFalse();
        assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectCases("ULSAN_DONGGU").map(c->c.code()).toList()).containsExactly(sample.code());
        var budget = AnnouncementAttachmentBbsOfficialObservationTest.selectBudget(sample.profile(),true,false);
        assertThat(budget.maximumRequests).isEqualTo(6);
        assertThat(budget.maximumBytes).isEqualTo(23L*1024*1024);
        assertThat(sample.profile().selectLegacyBinaryContentTypes()).isEmpty();
        assertThat(sample.profile().selectUtf8DispositionOctets()).isFalse();
    }
}
