package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import com.saneb.domain.announcementattachment.discovery.AttachmentDiscoveryProfile;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.provider.content.HongcheonNoticePage;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

class HongcheonDownloadContractTest {
    private final AnnouncementAttachmentBbsOfficialObservationTest.ObservationCase sample = HongcheonDownloadCases.selectCase();
    private String item(int id, String ext) {
        return "<li class=p-attach__item><a class=p-attach__link href=\"https://eminwon.hongcheon.go.kr/emwp/jsp/ofr/FileDown.jsp?user_file_nm=공고."
                + ext + "&amp;sys_file_nm=공고_" + id + "." + ext + "&amp;file_path=/ntishome/file/upload/ofr/ofr/20260116\">"
                + "<span class='p-icon p-icon__hwp'>hwp 파일</span><span>공고." + ext + "</span><i class='p-icon p-icon__arrow-circle-down'></i></a></li>";
    }
    private String page(String items) {
        return "<nav>수출 메뉴</nav><div class='p-wrap bbs bbs__view'><table class='p-table block'><tbody>"
                + "<tr><th>담당부서</th><td>특허 부서</td></tr>"
                + "<tr class=p-table__subject><th>제목</th><td colspan=3><span class=p-table__subject_text>소상공인 지원</span></td></tr>"
                + "<tr><td colspan=4>소상공인 지원금</td></tr>"
                + "<tr><th>첨부파일</th><td colspan=3><ul class=p-attach>" + items + "</ul></td></tr>"
                + "</tbody></table></div><footer>하단 영역</footer>";
    }
    @Test void supportedFilesAndPartialFailuresAreIndependent() {
        var p = sample.profile(); String good = item(1,"pdf") + item(2,"hwp") + item(3,"hwpx");
        var result = p.selectDescriptors(sample.source(),page(good));
        assertThat(result.complete()).isTrue();
        assertThat(result.descriptors()).hasSize(3).allSatisfy(d -> {
            assertThat(d.downloadAllowed()).isTrue(); assertThat(d.documentRole()).isEqualTo("UNKNOWN");
            assertThat(p.selectApprovedRequest(d.selectRequest(),d.selectRequest())).isTrue();
        });
        for (String bad : List.of(item(4,"pdf").replace("eminwon.hongcheon.go.kr", "evil.example"),
                "<script>loadFiles()</script>", item(4,"pdf").replace("/ntishome/", "/other/"),
                item(4,"pdf").replace("<span>공고.pdf", "<span>다른.pdf"), "확인하지 못한 파일",
                item(4,"pdf").replace("a class=", "a onclick='run()' class="))) {
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
        for (String url : List.of(sample.source().sourceUrl() + "&mode=delete", sample.source().sourceUrl() + "&not_ancmt_mgt_no=99",
                sample.source().sourceUrl() + "&unknown=1", sample.source().sourceUrl().replace("https:", "http:"),
                sample.source().sourceUrl().replace("key=278", "key=279"))) {
            assertThatThrownBy(() -> p.selectDetailUri(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",
                    n.hash(n.canonicalizeUrl(url)),url,"LGS-000124","SPRING_BBS"))).hasMessage("PROFILE_REQUIRED");
        }
        for (var source : List.of(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE","wrong",sample.source().sourceUrl(),"LGS-000124","SPRING_BBS"),
                new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",sample.source().providerNoticeId(),sample.source().sourceUrl(),"LGS-000125","SPRING_BBS"))) {
            assertThatThrownBy(() -> p.selectDetailUri(source)).hasMessage("PROFILE_REQUIRED");
        }
        String listUrl = sample.source().sourceUrl() + "&pageUnit=10&searchCnd=B_Subject&searchKrwd=지원";
        assertThat(p.selectDetailUri(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(listUrl)),listUrl,"LGS-000124","SPRING_BBS")))
                .isEqualTo(URI.create(sample.source().sourceUrl()));
        var first = p.selectDescriptors(sample.source(),page(item(1,"hwp"))).descriptors().getFirst().selectRequest();
        for (String url : List.of("https:opaque", first.uri().toString().replace("https:","http:"),
                first.uri().toString().replace("eminwon.hongcheon.go.kr", "127.0.0.1"), first.uri() + "&extra=1", first.uri() + "#f")) {
            assertThat(p.selectApprovedRequest(URI.create(url))).isFalse();
            if (!"https:opaque".equals(url)) assertThat(p.selectApprovedRequest(first,Request.selectGet(URI.create(url)))).isFalse();
        }
        assertThat(p.selectApprovedRequest(new Request(first.uri(),"POST",Map.of("x","y")))).isFalse();
        assertThat(p.selectApprovedRequest(first,Request.selectGet(URI.create(sample.source().sourceUrl())))).isFalse();
    }
    @Test void bodyTitleAndBudgetContract() {
        var document = Jsoup.parse(page(item(1,"hwp")));
        assertThat(HongcheonNoticePage.selectContent(document).text()).isEqualTo("소상공인 지원금");
        AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(document,"소상공인 지원",sample.titleLayout());
        assertThatThrownBy(() -> HongcheonNoticePage.selectContent(Jsoup.parse(document.toString() + document))).isInstanceOf(IllegalArgumentException.class);
        var budget = new AnnouncementAttachmentBbsOfficialObservationTest.Budget(sample.profile(),false);
        assertThat(budget.maximumRequests).isEqualTo(6); assertThat(budget.maximumBytes).isEqualTo(23L * 1024 * 1024);
    }
    @Test @EnabledIfEnvironmentVariable(named="SANEB_HONGCHEON_SURVEY_FIXTURE",matches="true")
    void actualHtmlContract() throws Exception {
        String html = Files.readString(Path.of("build/qa-hongcheon-20260930/hongcheon-detail.html"));
        var document = Jsoup.parse(html);
        AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(document,sample.title(),sample.titleLayout());
        var result = sample.profile().selectDescriptors(sample.source(),html);
        assertThat(result.complete()).as("%s %s",result.status(),result.warnings()).isTrue();
        assertThat(result.descriptors()).hasSize(1).allSatisfy(d -> assertThat(d.downloadAllowed()).isTrue());
        assertThat(HongcheonNoticePage.selectContent(document).text()).isNotBlank();
    }
    @Test void catalogReferenceIsNotPolicyApproval() throws Exception {
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        var notices = mapper.readTree(Files.readString(Path.of("src/main/resources/announcement-attachment/provider-qa-catalog-v2.json"))).path("notices");
        var ref = StreamSupport.stream(notices.spliterator(),false).filter(row -> sample.code().equals(row.path("caseCode").asText())).findFirst().orElseThrow();
        assertThat(ref.path("source")).isEqualTo(mapper.valueToTree(sample.source()));
        assertThat(ref.hasNonNull("expectation")).isFalse();
    }
}
