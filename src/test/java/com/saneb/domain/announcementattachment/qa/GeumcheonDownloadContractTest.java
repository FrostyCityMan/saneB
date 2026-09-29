package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import com.saneb.domain.announcementattachment.discovery.AttachmentDiscoveryProfile;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.provider.content.GeumcheonNoticePage;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

class GeumcheonDownloadContractTest {
    private final AnnouncementAttachmentBbsOfficialObservationTest.ObservationCase sample = GeumcheonDownloadCases.selectCase();
    private String item(int id, String ext) {
        return "<li class=p-attch__item><a href=\"javascript:seol_file_download('공고." + ext
                + "', '공고_" + id + "." + ext + "', '/ntishome/file/upload/ofr/ofr/20260116');\">공고." + ext + "</a></li>";
    }
    private String page(String items) {
        return "<nav>수출 메뉴</nav><div id=contents class=cts294><div class=program><div class='veterinary_contract view'><div class='p-wrap bbs bbs__view'><table class='p-table block'><tbody>"
                + "<tr><th>담당부서</th><td>특허 부서</td></tr>"
                + "<tr><td colspan=4 data-brl-flag=1>소상공인 지원</td></tr>"
                + "<tr><td colspan=4 data-brl-flag=7>소상공인 지원금</td></tr>"
                + "<tr><th>첨부파일</th><td colspan=3><ul class=p-attach>" + items + "</ul></td></tr>"
                + "</tbody></table></div></div></div></div><footer>하단 영역</footer>";
    }
    @Test void supportedFilesAndPartialFailuresAreIndependent() {
        var p = sample.profile(); String good = item(1,"pdf") + item(2,"hwp") + item(3,"hwpx");
        var result = p.selectDescriptors(sample.source(),page(good));
        assertThat(result.complete()).isTrue();
        assertThat(result.descriptors()).hasSize(3).allSatisfy(d -> {
            assertThat(d.downloadAllowed()).isTrue(); assertThat(d.documentRole()).isEqualTo("UNKNOWN");
            assertThat(p.selectApprovedRequest(d.selectRequest(),d.selectRequest())).isTrue();
        });
        for (String bad : List.of(item(4,"pdf").replace("seol_file_download", "unknown"),
                "<script>loadFiles()</script>", item(4,"pdf").replace("/ntishome/", "/other/"),
                item(4,"pdf").replace(">공고.pdf", ">다른.pdf"), "확인하지 못한 파일",
                item(4,"pdf").replace("<a ", "<a onclick='run()' "), item(4,"pdf").replace(");", ");evil();"))) {
            var partial = p.selectDescriptors(sample.source(),page(good + bad));
            assertThat(partial.complete()).isFalse(); assertThat(partial.descriptors()).hasSize(3);
        }
        var unsupported = p.selectDescriptors(sample.source(),page(good + item(4,"xlsx")));
        assertThat(unsupported.complete()).isTrue(); assertThat(unsupported.descriptors().getLast().downloadAllowed()).isFalse();
    }
    @Test void duplicateConflictsLimitsAndAbsentAreaAreSeparate() {
        var p = sample.profile(); String one = item(1,"hwp");
        assertThat(p.selectDescriptors(sample.source(),page(one + one)).descriptors()).hasSize(1);
        assertThat(p.selectDescriptors(sample.source(),page(one + one.replace("공고.hwp", "변경.hwp"))).complete()).isFalse();
        assertThat(p.selectDescriptors(sample.source(),page("")).status()).isEqualTo("NO_FILES");
        assertThat(p.selectDescriptors(sample.source(),"<p>첨부 없음</p>").complete()).isFalse();
        var limited = p.selectDescriptors(sample.source(),page(IntStream.rangeClosed(1,11).mapToObj(id -> item(id,"pdf")).collect(Collectors.joining())));
        assertThat(limited.status()).isEqualTo("LIMIT_EXCEEDED"); assertThat(limited.descriptors()).hasSize(10);
        String outside = "<a href='https://evil.example/other.pdf'>외부 링크</a>";
        assertThat(p.selectDescriptors(sample.source(),outside + page(one)).complete()).isTrue();
    }
    @Test void sourceRequestAndRedirectAreBounded() {
        var p = sample.profile(); var n = new AnnouncementSourceIdentityNormalizer();
        for (String url : List.of(sample.source().sourceUrl() + "&mode=delete", sample.source().sourceUrl() + "&notAncmtMgtNo=99",
                sample.source().sourceUrl() + "&unknown=1", sample.source().sourceUrl().replace("https:", "http:"),
                sample.source().sourceUrl().replace("key=294", "key=279"))) {
            assertThatThrownBy(() -> p.selectDetailUri(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",
                    n.hash(n.canonicalizeUrl(url)),url,"LGS-000019","SAEOL_GOSI"))).hasMessage("PROFILE_REQUIRED");
        }
        for (var source : List.of(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE","wrong",sample.source().sourceUrl(),"LGS-000019","SAEOL_GOSI"),
                new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",sample.source().providerNoticeId(),sample.source().sourceUrl(),"LGS-000125","SAEOL_GOSI"))) {
            assertThatThrownBy(() -> p.selectDetailUri(source)).hasMessage("PROFILE_REQUIRED");
        }
        assertThat(p.selectDetailUri(sample.source())).isEqualTo(URI.create(sample.source().sourceUrl()));
        var first = p.selectDescriptors(sample.source(),page(item(1,"hwp"))).descriptors().getFirst().selectRequest();
        for (String url : List.of("https:opaque", first.uri().toString().replace("https:","http:"),
                first.uri().toString().replace("eminwon.geumcheon.go.kr", "127.0.0.1"), first.uri() + "&extra=1", first.uri() + "#f")) {
            assertThat(p.selectApprovedRequest(URI.create(url))).isFalse();
            if (!"https:opaque".equals(url)) assertThat(p.selectApprovedRequest(first,Request.selectGet(URI.create(url)))).isFalse();
        }
        assertThat(p.selectApprovedRequest(new Request(first.uri(),"POST",Map.of("x","y")))).isFalse();
        assertThat(p.selectApprovedRequest(first,Request.selectGet(URI.create(sample.source().sourceUrl())))).isFalse();
    }
    @Test void bodyTitleAndBudgetContract() {
        var document = Jsoup.parse(page(item(1,"hwp")));
        assertThat(GeumcheonNoticePage.selectContent(document).text()).isEqualTo("소상공인 지원금");
        AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(document,"소상공인 지원",sample.titleLayout());
        assertThatThrownBy(() -> GeumcheonNoticePage.selectContent(Jsoup.parse(document.toString() + document))).isInstanceOf(IllegalArgumentException.class);
        var budget = new AnnouncementAttachmentBbsOfficialObservationTest.Budget(sample.profile(),false);
        assertThat(budget.maximumRequests).isEqualTo(6); assertThat(budget.maximumBytes).isEqualTo(23L * 1024 * 1024);
    }
    @Test @EnabledIfEnvironmentVariable(named="SANEB_GEUMCHEON_SURVEY_FIXTURE",matches="true")
    void actualHtmlContract() throws Exception {
        String html = Files.readString(Path.of("build/qa-geumcheon-20260930/detail.html"));
        var document = Jsoup.parse(html);
        AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(document,sample.title(),sample.titleLayout());
        var result = sample.profile().selectDescriptors(sample.source(),html);
        assertThat(result.complete()).as("%s %s",result.status(),result.warnings()).isTrue();
        assertThat(result.descriptors()).hasSize(3).allSatisfy(d -> assertThat(d.downloadAllowed()).isTrue());
        assertThat(GeumcheonNoticePage.selectContent(document).text()).isNotBlank();
    }
    @Test void catalogReferenceIsNotPolicyApproval() throws Exception {
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        var notices = mapper.readTree(Files.readString(Path.of("src/main/resources/announcement-attachment/provider-qa-catalog-v2.json"))).path("notices");
        var ref = StreamSupport.stream(notices.spliterator(),false).filter(row -> sample.code().equals(row.path("caseCode").asText())).findFirst().orElseThrow();
        assertThat(ref.path("source")).isEqualTo(mapper.valueToTree(sample.source()));
        assertThat(ref.hasNonNull("expectation")).isFalse();
    }
    @Test void previewAndAudioDoNotBecomeDownloadsOrHideBadLinks() {
        String extras="<input type=hidden value='/ntishome/file/upload/ofr/ofr/20260116/공고_1.hwp'>"
                + "<a href='#' class=p-attach__preview onclick=\"SViewerSeol('공고.hwp','공고_1.hwp','/ntishome/file/upload/ofr/ofr/20260116','소상공인 지원')\">미리보기</a>"
                + "<a href='#' class=p-attach__preview onclick=\"call_viewer_tts('https://eminwon.geumcheon.go.kr/emwp/jsp/ofr/FileDown.jsp?user_file_nm=공고.hwp&amp;sys_file_nm=공고_1.hwp&amp;file_path=/ntishome/file/upload/ofr/ofr/20260116')\">미리듣기</a>";
        var profile=sample.profile();String html=page(item(1,"hwp").replace("</li>",extras+"</li>"));
        var complete=profile.selectDescriptors(sample.source(),html);
        assertThat(complete.complete()).isTrue();assertThat(complete.descriptors()).hasSize(1);
        for(String bad:List.of(html.replace("SViewerSeol('공고.hwp'","SViewerSeol('다른.hwp'"),
                html.replace("https://eminwon.geumcheon.go.kr","https://evil.example"),html.replace("미리듣기</a>","미리듣기<script>bad()</script></a>"))){
            var partial=profile.selectDescriptors(sample.source(),bad);
            assertThat(partial.complete()).isFalse();assertThat(partial.descriptors()).hasSize(1);
        }
    }
}
