package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import com.saneb.domain.announcementattachment.discovery.AttachmentDiscoveryProfile;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.provider.content.IncheonCityNoticePage;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

class IncheonCityDownloadContractTest {
    private final AnnouncementAttachmentBbsOfficialObservationTest.ObservationCase sample = IncheonCityDownloadCases.selectCase();
    private String item(int id, String ext) {
        return "<tr><td class=tb_left><a target=downLoadFrame href='/citynet/jsp/cmm/attach/download.jsp?mode=download&amp;fid=%23" + "a".repeat(64)
                + "&amp;index=" + id + "&amp;other=%23" + "b".repeat(96) + "'>공고." + ext + "</a></td></tr>";
    }
    private String page(String items) {
        return "<nav>수출 메뉴</nav><form name=myform><input type=hidden name=sno value=66970><input type=hidden name=gosiGbn value=A><input type=hidden name=flag value=gosiGL>"
                + "<table><tbody><tr><th class=tb_tit_center>제목</th><td class=tb_left colspan=3>소상공인 지원</td></tr>"
                + "<tr><th class=tb_tit_center>담당부서</th><td class=tb_left>특허 부서</td></tr>"
                + "<tr><th class=tb_tit_center colspan=4>내 용</th></tr><tr><td class=board_line colspan=4></td></tr>"
                + "<tr><td class=tb_left colspan=4 wrap=VIRTUAL>소상공인 지원금</td></tr>"
                + "<tr><th class=tb_tit_center>첨부파일</th><td class=tb_left colspan=5><table><tbody>" + items
                + "</tbody></table></td></tr></tbody></table></form><footer>하단 영역</footer>";
    }
    @Test void supportedFilesAndPartialErrorsAreIndependent() {
        var p=sample.profile();String good=item(1,"pdf")+item(2,"hwp")+item(3,"hwpx");
        var result=p.selectDescriptors(sample.source(),page(good));assertThat(result.complete()).isTrue();
        assertThat(result.descriptors()).hasSize(3).allSatisfy(d->{assertThat(d.downloadAllowed()).isTrue();assertThat(d.documentRole()).isEqualTo("UNKNOWN");assertThat(p.selectApprovedRequest(d.selectRequest(),d.selectRequest())).isTrue();assertThat(d.locator().toString()).doesNotContain("a".repeat(64));});
        for(String bad:List.of(item(4,"pdf").replace("href='/citynet","href='https://evil.example/citynet"),item(4,"pdf").replace("download.jsp","other.jsp"),
                item(4,"pdf").replace("mode=download","mode=delete"),item(4,"pdf").replace("index=4","index=4&amp;extra=1"),
                item(4,"pdf").replace("<a ","<a onclick='run()' "),"<script>loadFiles()</script>","확인되지 않은 파일")){
            var partial=p.selectDescriptors(sample.source(),page(good+bad));assertThat(partial.complete()).isFalse();assertThat(partial.descriptors()).hasSize(3);
        }
        var unsupported=p.selectDescriptors(sample.source(),page(good+item(4,"xlsx")));assertThat(unsupported.complete()).isTrue();assertThat(unsupported.descriptors().getLast().downloadAllowed()).isFalse();
    }
    @Test void duplicateLimitsAbsentAreaAndIdentityAreSeparate() {
        var p=sample.profile();String one=item(1,"hwp");
        assertThat(p.selectDescriptors(sample.source(),page(one+one)).descriptors()).hasSize(1);
        assertThat(p.selectDescriptors(sample.source(),page(one+one.replace("공고.hwp","변경.hwp"))).complete()).isFalse();
        assertThat(p.selectDescriptors(sample.source(),page("")).status()).isEqualTo("NO_FILES");
        assertThat(p.selectDescriptors(sample.source(),"<p>첨부 없음</p>").complete()).isFalse();
        var limited=p.selectDescriptors(sample.source(),page(IntStream.rangeClosed(1,11).mapToObj(i->item(i,"pdf")).collect(Collectors.joining())));
        assertThat(limited.status()).isEqualTo("LIMIT_EXCEEDED");assertThat(limited.descriptors()).hasSize(10);
        assertThat(p.selectDescriptors(sample.source(),page(one).replace("value=66970","value=999")).descriptors()).isEmpty();
        assertThat(p.selectDescriptors(sample.source(),"<a href='https://evil.example/file.pdf'>다른 영역</a>"+page(one)).complete()).isTrue();
    }
    @Test void storedHttpIdentityDoesNotPermitHttpFetchOrRedirect() {
        var p=sample.profile();var n=new AnnouncementSourceIdentityNormalizer();
        assertThat(p.selectDetailUri(sample.source()).getScheme()).isEqualTo("https");
        assertThat(p.selectApprovedRequest(URI.create(sample.source().sourceUrl()))).isFalse();
        for(String url:List.of(sample.source().sourceUrl()+"&sno=1",sample.source().sourceUrl()+"&extra=1",sample.source().sourceUrl().replace("sido=ic","sido=other"),sample.source().sourceUrl().replace("searchDetail","searchList"),sample.source().sourceUrl().replace("gosiGbn=A","gosiGbn=X"),sample.source().sourceUrl().replace(".go.kr/",".go.kr:443/"))){
            assertThatThrownBy(()->p.selectDetailUri(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(url)),url,"LGS-000054","SAFE_INCHEON_CITYNET_NOTICE"))).hasMessage("PROFILE_REQUIRED");
        }
        assertThatThrownBy(()->p.selectDetailUri(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE","wrong",sample.source().sourceUrl(),"LGS-000054","SAFE_INCHEON_CITYNET_NOTICE"))).hasMessage("PROFILE_REQUIRED");
        var first=p.selectDescriptors(sample.source(),page(item(1,"hwp"))).descriptors().getFirst().selectRequest();
        for(String url:List.of("https:opaque",first.uri().toString().replace("https:","http:"),first.uri().toString().replace("announce.incheon.go.kr","127.0.0.1"),first.uri()+"#f",first.uri()+"&index=2",first.uri().toString().replace("%23"+"a".repeat(64),"%23short"))){assertThat(p.selectApprovedRequest(URI.create(url))).isFalse();}
        assertThat(p.selectApprovedRequest(new Request(first.uri(),"POST",Map.of("x","y")))).isFalse();
        assertThat(p.selectApprovedRequest(first,Request.selectGet(URI.create(first.uri().toString().replace("index=1","index=2"))))).isFalse();
    }
    @Test void bodyTitleAndBudgetContract() {
        var doc=Jsoup.parse(page(item(1,"hwp")));var uri=sample.profile().selectDetailUri(sample.source());
        assertThat(IncheonCityNoticePage.selectContent(doc,uri).text()).isEqualTo("소상공인 지원금");
        AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(doc,"소상공인 지원",sample.titleLayout());
        assertThatThrownBy(()->IncheonCityNoticePage.selectContent(Jsoup.parse(doc.toString()+doc),uri)).isInstanceOf(IllegalArgumentException.class);
        var budget=new AnnouncementAttachmentBbsOfficialObservationTest.Budget(sample.profile(),false);assertThat(budget.maximumRequests).isEqualTo(6);assertThat(budget.maximumBytes).isEqualTo(23L*1024*1024);
    }
    @Test @EnabledIfEnvironmentVariable(named="SANEB_INCHEON_CITY_SURVEY_FIXTURE",matches="true")
    void actualEucKrHtmlContract() throws Exception {
        var uri=sample.profile().selectDetailUri(sample.source());
        try(var input=Files.newInputStream(Path.of("build/qa-incheon-city-20260930/detail.html"))){
            var doc=Jsoup.parse(input,null,uri.toASCIIString());
            AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(doc,sample.title(),sample.titleLayout());
            var result=sample.profile().selectDescriptors(sample.source(),doc.outerHtml());
            assertThat(result.complete()).as("%s %s",result.status(),result.warnings()).isTrue();assertThat(result.descriptors()).hasSize(1).allSatisfy(d->assertThat(d.expectedFormat()).isEqualTo("HWPX"));
            assertThat(IncheonCityNoticePage.selectContent(doc,uri).text()).isNotBlank().doesNotContain("\ufffd");
        }
    }
    @Test void catalogReferenceIsNotPolicyApproval() throws Exception {
        var mapper=new com.fasterxml.jackson.databind.ObjectMapper();var notices=mapper.readTree(Files.readString(Path.of("src/main/resources/announcement-attachment/provider-qa-catalog-v2.json"))).path("notices");
        var ref=StreamSupport.stream(notices.spliterator(),false).filter(r->sample.code().equals(r.path("caseCode").asText())).findFirst().orElseThrow();
        assertThat(ref.path("source")).isEqualTo(mapper.valueToTree(sample.source()));assertThat(ref.hasNonNull("expectation")).isFalse();
    }
}
