package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import com.saneb.domain.announcementattachment.discovery.AttachmentDiscoveryProfile;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.provider.content.GangwonProvinceNoticePage;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

class GangwonProvinceDownloadContractTest {
    private final AnnouncementAttachmentBbsOfficialObservationTest.ObservationCase sample = GangwonProvinceDownloadCases.selectCase("GANGWON_PROVINCE");
    private String item(int id, String extension) {
        String path = "/egf/bp/common/front/" + id + "/download";
        return "<div class=attachFile><a href='" + path + "'><span class='icoFile icoFile-data-hwpx'></span>공고." + extension
                + "</a><span class=attachFile-txt>(다운로드: 121 | 146.7kb)</span><div class='contsBtn contsBtnSmall skinBtnBo v2'><span>바로보기</span><a class=contsBtn-more href='javascript:void(0);' onclick=\"previewAjax('"
                + path + "');\">바로보기</a></div></div>";
    }
    private String page(String items) {
        return "<div id=content-bx><div class='skinTb skinTb-data-resList skinTb-data-bgSbj'>"
                + "<div class=skinTb-tr><div class=skinTb-th>제목</div><div class='skinTb-td skinTb-sbj'>소상공인 지원</div></div>"
                + "<div class=skinTb-tr><div class=skinTb-th>첨부파일</div><div class=skinTb-td>" + items + "</div></div>"
                + "<div class=skinTb-tr><div class=skinTb-th>내용</div><div class='skinTb-td skinTb-conts'>소상공인 지원금</div></div></div></div>";
    }
    @Test void filesAndPartialErrorsRemainIndependent() {
        var p = sample.profile();
        String good = item(1,"pdf") + item(2,"hwp") + item(3,"hwpx");
        var complete = p.selectDescriptors(sample.source(),page(good));
        assertThat(complete.complete()).isTrue();
        assertThat(complete.descriptors()).hasSize(3).allSatisfy(d -> {
            assertThat(d.downloadAllowed()).isTrue();
            assertThat(d.documentRole()).isEqualTo("UNKNOWN");
            assertThat(p.selectApprovedRequest(d.selectRequest(),d.selectRequest())).isTrue();
        });
        for (String bad : List.of("<div class=attachFile><a href='https://evil.example/file.pdf'>공고.pdf</a></div>",
                "<a href='/unknown'>추가 파일</a>", "<script>loadFiles()</script>", "확인하지 못한 목록")) {
            var partial = p.selectDescriptors(sample.source(),page(good + bad));
            assertThat(partial.complete()).isFalse();
            assertThat(partial.descriptors()).hasSize(3);
        }
        var unsupported = p.selectDescriptors(sample.source(),page(good + item(4,"xlsx")));
        assertThat(unsupported.complete()).isTrue();
        assertThat(unsupported.descriptors().getLast().downloadAllowed()).isFalse();
    }
    @Test void duplicatesLimitAndEmptyAreDistinguished() {
        var p = sample.profile(); String one = item(1,"pdf");
        assertThat(p.selectDescriptors(sample.source(),page(one + one)).descriptors()).hasSize(1);
        assertThat(p.selectDescriptors(sample.source(),page(one + one.replace("공고.pdf","다른.pdf"))).complete()).isFalse();
        assertThat(p.selectDescriptors(sample.source(),page("")).status()).isEqualTo("NO_FILES");
        assertThat(p.selectDescriptors(sample.source(),"<p>첨부 없음</p>").complete()).isFalse();
        var limit = p.selectDescriptors(sample.source(),page(IntStream.rangeClosed(1,11).mapToObj(id -> item(id,"pdf")).collect(Collectors.joining())));
        assertThat(limit.status()).isEqualTo("LIMIT_EXCEEDED");
        assertThat(limit.descriptors()).hasSize(10);
    }
    @Test void previewMismatchNeverRemovesValidDownload() {
        var result = sample.profile().selectDescriptors(sample.source(),page(item(1,"pdf")
                .replace("previewAjax('/egf/bp/common/front/1/download');", "previewAjax('/egf/bp/common/front/2/download');")));
        assertThat(result.complete()).isFalse(); assertThat(result.descriptors()).hasSize(1);
    }
    @Test void sourceAndRequestAreBoundToExactHostAndIdentity() {
        var p = sample.profile(); var n = new AnnouncementSourceIdentityNormalizer();
        for (String bad : List.of(sample.source().sourceUrl() + "&extra=1", sample.source().sourceUrl() + "&articleSeq=99",
                sample.source().sourceUrl() + "&mode=delete", sample.source().sourceUrl().replace("https:","http:"))) {
            assertThatThrownBy(() -> p.selectDetailUri(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",
                    n.hash(n.canonicalizeUrl(bad)), bad, "LGS-000116", "SAFE_GWD_BULLETIN"))).hasMessage("PROFILE_REQUIRED");
        }
        assertThatThrownBy(() -> p.selectDetailUri(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE","wrong",
                sample.source().sourceUrl(),"LGS-000116","SAFE_GWD_BULLETIN"))).hasMessage("PROFILE_REQUIRED");
        assertThatThrownBy(() -> p.selectDetailUri(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",sample.source().providerNoticeId(),
                sample.source().sourceUrl(),"LGS-000117","SAFE_GWD_BULLETIN"))).hasMessage("PROFILE_REQUIRED");
        var first = Request.selectGet(URI.create("https://state.gwd.go.kr/egf/bp/common/front/1/download"));
        for (String bad : List.of("http://state.gwd.go.kr/egf/bp/common/front/1/download",
                "https://127.0.0.1/egf/bp/common/front/1/download", "https://state.gwd.go.kr/egf/bp/common/front/1/download?x=1",
                "https://state.gwd.go.kr/egf/bp/common/front/1/download#f", "https://state.gwd.go.kr/egf/bp/common/front/../1/download", "https:opaque")) {
            assertThat(p.selectApprovedRequest(URI.create(bad))).isFalse();
            assertThat(p.selectApprovedRequest(first,Request.selectGet(URI.create(bad)))).isFalse();
        }
        assertThat(p.selectApprovedRequest(first,Request.selectGet(URI.create("https://state.gwd.go.kr/egf/bp/common/front/2/download")))).isFalse();
        assertThat(p.selectApprovedRequest(new Request(first.uri(),"POST",Map.of("x","y")))).isFalse();
    }
    @Test void bodyTitleAndBudgetContract() {
        var document = Jsoup.parse(page(item(1,"hwp")));
        assertThat(GangwonProvinceNoticePage.selectContent(document).text()).isEqualTo("소상공인 지원금");
        AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(document,"소상공인 지원",sample.titleLayout());
        assertThatThrownBy(() -> GangwonProvinceNoticePage.selectContent(Jsoup.parse(document.toString() + document))).isInstanceOf(IllegalArgumentException.class);
        var budget = new AnnouncementAttachmentBbsOfficialObservationTest.Budget(sample.profile(),false);
        assertThat(budget.maximumRequests).isEqualTo(6);
        assertThat(budget.maximumBytes).isEqualTo(23L * 1024 * 1024);
    }
    @Test @EnabledIfEnvironmentVariable(named="SANEB_GANGWON_PROVINCE_SURVEY_FIXTURE",matches="true")
    void actualHtmlContract() throws Exception {
        String html = Files.readString(Path.of("build/qa-capital-ninth-20260930/GANGWON-detail.html"));
        var document = Jsoup.parse(html);
        AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(document,sample.title(),sample.titleLayout());
        var result = sample.profile().selectDescriptors(sample.source(),html);
        assertThat(result.complete()).as("%s %s",result.status(),result.warnings()).isTrue();
        assertThat(result.descriptors()).hasSize(1).allSatisfy(d -> assertThat(d.downloadAllowed()).isTrue());
        assertThat(GangwonProvinceNoticePage.selectContent(document).text()).isNotBlank();
    }
    @Test void catalogReferenceIsNotApprovedExpectation() throws Exception {
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        var notices = mapper.readTree(Files.readString(Path.of("src/main/resources/announcement-attachment/provider-qa-catalog-v2.json"))).path("notices");
        var ref = StreamSupport.stream(notices.spliterator(),false).filter(row -> sample.code().equals(row.path("caseCode").asText())).findFirst().orElseThrow();
        assertThat(ref.path("source")).isEqualTo(mapper.valueToTree(sample.source()));
        assertThat(ref.hasNonNull("expectation")).isFalse();
    }
}
