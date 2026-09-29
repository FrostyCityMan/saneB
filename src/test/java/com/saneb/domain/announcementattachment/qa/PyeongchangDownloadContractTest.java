package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import com.saneb.domain.announcementattachment.discovery.AttachmentDiscoveryProfile;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.provider.content.PyeongchangNoticePage;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

class PyeongchangDownloadContractTest {
    private final AnnouncementAttachmentBbsOfficialObservationTest.ObservationCase sample = PyeongchangDownloadCases.selectCase();
    private String item(int id, String ext) {
        return "<div class=attachFile><a href=\"javascript:goDownLoad('공고." + ext + "', '공고_" + id + "." + ext
                + "', '/ntishome/file/upload/ofr/ofr/20260417')\"><span class='icoFile skinMr-small'></span>공고." + ext + "</a></div>";
    }
    private String page(String items) {
        return "<div id=contentsArea><div class='skinTb skinTb-data-resList skinTb-data-bgSbj'>"
                + "<div class=skinTb-tr><div class=skinTb-th>제목</div><div class='skinTb-td skinTb-sbj'>소상공인 지원</div></div>"
                + "<div class=skinTb-tr><div class=skinTb-th>첨부파일</div><div class=skinTb-td>" + items + "</div></div>"
                + "<div class=skinTb-tr><div class=skinTb-th>내용</div><div class='skinTb-td skinTb-conts'>소상공인 지원금</div></div></div></div>";
    }
    @Test void supportedFilesAndPartialFailuresAreIndependent() {
        var p = sample.profile(); String good = item(1,"pdf") + item(2,"hwp") + item(3,"hwpx");
        var result = p.selectDescriptors(sample.source(),page(good));
        assertThat(result.complete()).isTrue();
        assertThat(result.descriptors()).hasSize(3).allSatisfy(d -> {
            assertThat(d.downloadAllowed()).isTrue(); assertThat(d.documentRole()).isEqualTo("UNKNOWN");
            assertThat(p.selectApprovedRequest(d.selectRequest(),d.selectRequest())).isTrue();
        });
        for (String bad : List.of("<div class=attachFile><a href='https://evil.example/file.pdf'>공고.pdf</a></div>",
                "<script>loadFiles()</script>", item(4,"pdf").replace("goDownLoad", "eval"),
                item(4,"pdf").replace("/ntishome/", "/other/"), item(4,"pdf").replace(")\"", ");alert(1)\""),
                item(4,"pdf").replace(">공고.pdf</a>", ">다른.pdf</a>"), "확인하지 못한 파일")) {
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
    }
    @Test void sourceRequestAndRedirectAreBounded() {
        var p = sample.profile(); var n = new AnnouncementSourceIdentityNormalizer();
        for (String url : List.of(sample.source().sourceUrl() + "&mode=delete", sample.source().sourceUrl() + "&noticeMgrNo=99",
                sample.source().sourceUrl() + "&unknown=1", sample.source().sourceUrl().replace("https:", "http:"))) {
            assertThatThrownBy(() -> p.selectDetailUri(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",
                    n.hash(n.canonicalizeUrl(url)),url,"LGS-000127","SPRING_BBS"))).hasMessage("PROFILE_REQUIRED");
        }
        for (var source : List.of(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE","wrong",sample.source().sourceUrl(),"LGS-000127","SPRING_BBS"),
                new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",sample.source().providerNoticeId(),sample.source().sourceUrl(),"LGS-000128","SPRING_BBS"))) {
            assertThatThrownBy(() -> p.selectDetailUri(source)).hasMessage("PROFILE_REQUIRED");
        }
        var first = p.selectDescriptors(sample.source(),page(item(1,"hwp"))).descriptors().getFirst().selectRequest();
        for (String url : List.of("https:opaque", first.uri().toString().replace("https:","http:"),
                first.uri().toString().replace("eminwon.pc.go.kr", "127.0.0.1"), first.uri() + "&extra=1", first.uri() + "#f")) {
            assertThat(p.selectApprovedRequest(URI.create(url))).isFalse();
            if (!"https:opaque".equals(url)) assertThat(p.selectApprovedRequest(first,Request.selectGet(URI.create(url)))).isFalse();
        }
        assertThat(p.selectApprovedRequest(new Request(first.uri(),"POST",Map.of("x","y")))).isFalse();
        assertThat(p.selectApprovedRequest(first,Request.selectGet(URI.create(sample.source().sourceUrl())))).isFalse();
    }
    @Test void bodyTitleAndBudgetContract() {
        var document = Jsoup.parse(page(item(1,"hwp")));
        assertThat(PyeongchangNoticePage.selectContent(document).text()).isEqualTo("소상공인 지원금");
        AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(document,"소상공인 지원",sample.titleLayout());
        assertThatThrownBy(() -> PyeongchangNoticePage.selectContent(Jsoup.parse(document.toString() + document))).isInstanceOf(IllegalArgumentException.class);
        var budget = new AnnouncementAttachmentBbsOfficialObservationTest.Budget(sample.profile(),false);
        assertThat(budget.maximumRequests).isEqualTo(6); assertThat(budget.maximumBytes).isEqualTo(23L * 1024 * 1024);
    }
    @Test @EnabledIfEnvironmentVariable(named="SANEB_PYEONGCHANG_SURVEY_FIXTURE",matches="true")
    void actualHtmlContract() throws Exception {
        String html = Files.readString(Path.of("build/qa-gangwon-next-20260930/pyeongchang-support.html"));
        var document = Jsoup.parse(html);
        AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(document,sample.title(),sample.titleLayout());
        var result = sample.profile().selectDescriptors(sample.source(),html);
        assertThat(result.complete()).as("%s %s",result.status(),result.warnings()).isTrue();
        assertThat(result.descriptors()).hasSize(1).allSatisfy(d -> assertThat(d.downloadAllowed()).isTrue());
        assertThat(PyeongchangNoticePage.selectContent(document).text()).isNotBlank();
    }
    @Test void catalogReferenceIsNotPolicyApproval() throws Exception {
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        var notices = mapper.readTree(Files.readString(Path.of("src/main/resources/announcement-attachment/provider-qa-catalog-v2.json"))).path("notices");
        var ref = StreamSupport.stream(notices.spliterator(),false).filter(row -> sample.code().equals(row.path("caseCode").asText())).findFirst().orElseThrow();
        assertThat(ref.path("source")).isEqualTo(mapper.valueToTree(sample.source()));
        assertThat(ref.hasNonNull("expectation")).isFalse();
    }
}
