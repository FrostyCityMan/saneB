package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.domain.announcementattachment.discovery.AttachmentDiscoveryProfile;
import com.saneb.domain.announcementsource.classification.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.provider.content.GyeongbukProvinceNoticePage;
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

class GyeongbukProvinceDownloadContractTest {
    private final AnnouncementAttachmentBbsOfficialObservationTest.ObservationCase sample = GyeongbukProvinceDownloadCases.selectCase();
    private String selectCall(int id,String extension) {
        return "boardView.viewFile.putFileHtml(\"FL00000001196\",\"" + id + "\",\"지원 공고(학생, 개인) " + id + "." + extension + "\",\"157076\",\"" + extension + "\");\n";
    }
    private String selectPage(String calls) {
        return "<form id=boardViewForm method=get><div class=table_shape_tit><p>" + sample.title() + "</p></div>"
                + "<ul class='table_shape input_form board_view'><li><div class=th_shape>첨부파일</div><div class='td_shape file'><section>"
                + "<div class='fileWrap fileId' data-id=fileId data-vl=FL00000001196></div></section></div></li></ul>"
                + "<div class=view_info>수출 부서</div><div class=board_view><div class=text>대학생 이자지원 사업</div></div></form>"
                + "<script>boardView.form = $(\"#boardViewForm\");\nboardView.viewFile = boardView.form.find(\"div.fileWrap.fileId\").viewFile({template:\"front\", previewYn:\"Y\"});\n" + calls + "</script>";
    }
    @Test void officialManifestProducesOnlyFixedFileRequests() {
        var result = sample.profile().selectDescriptors(sample.source(),selectPage(selectCall(1,"pdf") + selectCall(2,"hwp") + selectCall(3,"hwpx")));
        assertThat(result.complete()).isTrue(); assertThat(result.descriptors()).hasSize(3).allSatisfy(file -> {
            assertThat(file.downloadAllowed()).isTrue(); assertThat(file.documentRole()).isEqualTo("UNKNOWN");
            assertThat(file.fetchUri().getHost()).isEqualTo("www.gb.go.kr"); assertThat(file.fetchUri().getPath()).isEqualTo("/file/readFile.do");
            assertThat(file.selectRequest().method()).isEqualTo("GET"); assertThat(sample.profile().selectApprovedRequest(file.selectRequest())).isTrue();
            assertThat(file.locator().toString()).doesNotContain("지원","FL00000001196");
        });
    }
    @Test void commentsStringsAndOtherObjectsCannotAddFiles() throws Exception {
        String fake = selectCall(2,"pdf"), quoted = new ObjectMapper().writeValueAsString(fake);
        for (String noise : List.of("/*" + fake + "*/\n", "//" + fake, "const fake=" + quoted + ";\n", fake.replace("boardView.viewFile","other.viewFile"))) {
            var result = sample.profile().selectDescriptors(sample.source(),selectPage(noise + selectCall(1,"pdf")));
            assertThat(result.complete()).isTrue(); assertThat(result.descriptors()).hasSize(1);
        }
    }
    @Test void expressionsAndMalformedEntriesPreserveOtherGoodFiles() {
        for (String bad : List.of(selectCall(2,"pdf").replace("\"2\"","resolveNumber()"),selectCall(2,"pdf").replace("\"2\"","\"2\" + \"3\""),
                selectCall(2,"pdf").replace("\"157076\"","\"wrong\""),selectCall(2,"pdf").replace("FL00000001196","FL00000009999"),
                selectCall(2,"pdf").replace("지원 공고","../지원 공고"),selectCall(2,"pdf").replace(".pdf\"",".hwp\""))) {
            var result = sample.profile().selectDescriptors(sample.source(),selectPage(bad + selectCall(1,"pdf")));
            assertThat(result.complete()).isFalse(); assertThat(result.descriptors()).hasSize(1);
        }
    }
    @Test void unsupportedDuplicateConflictAndLimitAreSeparate() {
        String one = selectCall(1,"pdf"); var profile = sample.profile();
        var unsupported = profile.selectDescriptors(sample.source(),selectPage(one + selectCall(2,"xlsx")));
        assertThat(unsupported.complete()).isTrue(); assertThat(unsupported.descriptors().getLast().downloadAllowed()).isFalse();
        assertThat(profile.selectDescriptors(sample.source(),selectPage(one + one)).descriptors()).hasSize(1);
        for (String conflict : List.of(one.replace("지원 공고","다른 공고"),one.replace("157076","157077"))) {
            var result = profile.selectDescriptors(sample.source(),selectPage(one + conflict));
            assertThat(result.complete()).isFalse(); assertThat(result.descriptors()).hasSize(1);
        }
        var limit = profile.selectDescriptors(sample.source(),selectPage(IntStream.rangeClosed(1,11).mapToObj(i -> selectCall(i,"pdf")).collect(Collectors.joining())));
        assertThat(limit.status()).isEqualTo("LIMIT_EXCEEDED"); assertThat(limit.descriptors()).hasSize(10);
    }
    @Test void missingManifestIsNotNoFiles() {
        var profile = sample.profile(); assertThat(profile.selectDescriptors(sample.source(),selectPage("")).complete()).isFalse();
        assertThat(profile.selectDescriptors(sample.source(),selectPage("").replace("data-vl=FL00000001196","data-vl=''" )).status()).isEqualTo("NO_FILES");
        for (String bad : List.of(selectPage(selectCall(1,"pdf")).replace("boardView.form =","other.form ="),selectPage("") + selectPage(""),"<p>에러</p>"))
            assertThat(profile.selectDescriptors(sample.source(),bad).complete()).isFalse();
        var partial = profile.selectDescriptors(sample.source(),selectPage(selectCall(1,"pdf")).replace("</section>","<a href='/unknown'>미확인</a></section>"));
        assertThat(partial.complete()).isFalse(); assertThat(partial.descriptors()).hasSize(1);
    }
    @Test void canonicalEncodingIsRestoredOnlyForTheFixedImportRoute() {
        var source = sample.source(); var normalizer = new AnnouncementSourceIdentityNormalizer(); var profile = sample.profile();
        assertThat(source.sourceUrl()).contains("importUrl=%252Fboard%252Fview.do");
        assertThat(source.providerNoticeId()).isEqualTo(normalizer.hash(source.sourceUrl())); String raw = profile.selectDetailUri(source).toString();
        assertThat(normalizer.canonicalizeUrl(raw)).isEqualTo(source.sourceUrl());
        for (String bad : List.of(raw + "&extra=x",raw.replace("boardMngNo=71","boardMngNo=72"),raw.replace("view.do","list.do"),
                raw.replace("%2Fboard%2Fview.do","%25252Fboard%25252Fview.do"),raw.replace("www.gb.go.kr","evil.example"),raw.replace("https:","http:"),raw + "#x"))
            assertThatThrownBy(() -> GyeongbukProvinceNoticePage.selectDetailUri(URI.create(bad))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> profile.selectDetailUri(new AttachmentDiscoveryProfile.Source(source.providerCode(),"0".repeat(64),source.sourceUrl(),source.localSourceCode(),source.listParserProfileCode()))).hasMessage("PROFILE_REQUIRED");
    }
    @Test void fileHostMethodAndRedirectAreBound() {
        var profile = sample.profile(); var files = profile.selectDescriptors(sample.source(),selectPage(selectCall(1,"pdf") + selectCall(2,"pdf"))).descriptors();
        assertThat(profile.selectApprovedRequest(files.getFirst().selectRequest(),files.getLast().selectRequest())).isFalse();
        assertThat(profile.selectApprovedRequest(new Request(files.getFirst().fetchUri(),"POST",Map.of("x","y")))).isFalse();
        assertThat(profile.selectApprovedRequest(URI.create(files.getFirst().fetchUri() + "&extra=x"))).isFalse();
    }
    @Test void titleAndBodyAreSeparateFromMetadataAndFiles() throws Exception {
        var page = Jsoup.parse(selectPage(selectCall(1,"pdf")));
        AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,sample.title(),sample.titleLayout());
        assertThat(GyeongbukProvinceNoticePage.selectContent(page).text()).isEqualTo("대학생 이자지원 사업");
        var decision = new AnnouncementSourceClassificationEngine().selectDecision(new AnnouncementSourceClassificationInput("LOCAL_GOV_NOTICE",sample.title(),null,null,List.of(),AnnouncementSourceClassificationCodes.BodySourceCode.NONE,AnnouncementSourceClassificationCodes.BodyAvailabilityCode.UNAVAILABLE),AnnouncementAttachmentRealFileQaTest.selectDraftRuleSet());
        assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectTitleMayProceed(decision)).isTrue();
    }
    @Test void catalogAndBudgetDoNotClaimOperatingApproval() throws Exception {
        var json = new ObjectMapper(); var notices = json.readTree(Files.readString(Path.of("src/main/resources/announcement-attachment/provider-qa-catalog-v2.json"))).path("notices");
        var entry = StreamSupport.stream(notices.spliterator(),false).filter(n -> sample.code().equals(n.path("caseCode").asText())).findFirst().orElseThrow();
        assertThat(entry.path("source")).isEqualTo(json.valueToTree(sample.source())); assertThat(entry.hasNonNull("expectation")).isFalse();
        assertThat(Files.readString(Path.of("src/main/resources/db/migration/V61__correct_general_notice_sources_to_official_legal_boards.sql"))).contains("'LGS-000200', '" + sample.listUrl() + "'");
        var budget = AnnouncementAttachmentBbsOfficialObservationTest.selectBudget(sample.profile(),true,false);
        assertThat(budget.maximumRequests).isEqualTo(6); assertThat(budget.maximumBytes).isEqualTo(23L*1024*1024);
    }
}
