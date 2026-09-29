package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import com.saneb.domain.announcementattachment.discovery.AttachmentDiscoveryProfile.Source;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.provider.content.UlsanCityNoticePage;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

class UlsanCityDownloadContractTest {
    private final AnnouncementAttachmentBbsOfficialObservationTest.ObservationCase sample=UlsanCityDownloadCases.selectCase();
    private String item(int id,String extension){return "<a href='https://minwon.ulsan.go.kr/citynet/jsp/cmm/attach/download.jsp?mode=download&amp;fid=%23"+"a".repeat(64)+"&amp;index="+id+"&amp;other=%23"+"b".repeat(96)+"'>공고."+extension+"</a><br>";}
    private String page(String links){return "<div id=contents_inner><table class=tbl_bd_view><tr><th scope=row>제목</th><td colspan=3>소상공인 지원</td></tr><tr><th scope=row>담당부서</th><td>수출 부서</td></tr><tr><th scope=row>첨부파일</th><td colspan=3>"+links+"</td></tr><tr><th scope=row colspan=4>내용</th></tr><tr><td colspan=4>소상공인 지원금</td></tr></table></div>";}
    @Test void independentFilesSurviveDiscoveryErrors(){
        var p=sample.profile();String good=item(1,"pdf")+item(2,"hwp")+item(3,"hwpx");
        var result=p.selectDescriptors(sample.source(),page(good));assertThat(result.complete()).isTrue();assertThat(result.descriptors()).hasSize(3).allSatisfy(d->{assertThat(d.downloadAllowed()).isTrue();assertThat(d.documentRole()).isEqualTo("UNKNOWN");assertThat(p.selectApprovedRequest(d.selectRequest())).isTrue();assertThat(d.locator().toString()).doesNotContain("a".repeat(64));});
        for(String bad:List.of(item(4,"pdf").replace("minwon.ulsan.go.kr","evil.example"),item(4,"pdf").replace("mode=download","mode=delete"),item(4,"pdf").replace("index=4","index=4&amp;extra=1"),item(4,"pdf").replace("<a ","<a onclick='run()' "),"<script>loadFiles()</script>")){var partial=p.selectDescriptors(sample.source(),page(good+bad));assertThat(partial.complete()).isFalse();assertThat(partial.descriptors()).hasSize(3);}
        assertThat(p.selectDescriptors(sample.source(),page(item(1,"xlsx"))).descriptors().getFirst().downloadAllowed()).isFalse();
    }
    @Test void absenceDuplicatesLimitsAndOfficialAreaRemainSeparate(){
        var p=sample.profile();String one=item(1,"pdf");assertThat(p.selectDescriptors(sample.source(),page("")).status()).isEqualTo("NO_FILES");assertThat(p.selectDescriptors(sample.source(),"<p>오류</p>").complete()).isFalse();
        assertThat(p.selectDescriptors(sample.source(),page(one+one)).descriptors()).hasSize(1);assertThat(p.selectDescriptors(sample.source(),page(one+one.replace("공고.pdf","변경.pdf"))).complete()).isFalse();
        var limit=p.selectDescriptors(sample.source(),page(IntStream.rangeClosed(1,11).mapToObj(i->item(i,"pdf")).collect(Collectors.joining())));assertThat(limit.status()).isEqualTo("LIMIT_EXCEEDED");assertThat(limit.descriptors()).hasSize(10);
        assertThat(p.selectDescriptors(sample.source(),page(one)+"<a href='https://evil.example/a.pdf'>다른 영역</a>").complete()).isTrue();
        assertThat(p.selectDescriptors(sample.source(),page(one)+page(one)).complete()).isFalse();
    }
    @Test void onlyPairedKnownPreviewButtonIsIgnoredWithoutExecution(){
        String link=item(1,"hwpx").replace("<br>",""),preview="<button type=button class=btn_preview onclick=\"fn_fileNoticePreivew('F2608272029225540','1');\"><span>미리보기</span></button>";
        var p=sample.profile();assertThat(p.selectDescriptors(sample.source(),page(link+preview)).complete()).isTrue();
        for(String bad:List.of(preview.replace("'1'","'2'"),preview.replace("F2608272029225540","not-known"),preview.replace("<span>","<span onclick='run()'>"))){var r=p.selectDescriptors(sample.source(),page(link+bad));assertThat(r.complete()).isFalse();assertThat(r.descriptors()).hasSize(1);}
    }
    @Test void sourceMenuHostsAndRequestMethodsStayBounded(){
        var p=sample.profile();var n=new AnnouncementSourceIdentityNormalizer();String url=sample.source().sourceUrl();
        for(String bad:List.of(url+"&extra=1",url+"&gosiGbn=A",url.replace("001004002000000000","001004001000000000"),url.replace("47059.ulsan","list.ulsan"),url.replace("https:","http:"),url.replace("www.ulsan.go.kr","127.0.0.1"))){assertThatThrownBy(()->p.selectDetailUri(new Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(bad)),bad,"LGS-000077","SAEOL_GOSI"))).hasMessage("PROFILE_REQUIRED");}
        assertThatThrownBy(()->p.selectDetailUri(new Source("LOCAL_GOV_NOTICE","wrong",url,"LGS-000077","SAEOL_GOSI"))).hasMessage("PROFILE_REQUIRED");
        var first=p.selectDescriptors(sample.source(),page(item(1,"hwp"))).descriptors().getFirst().selectRequest();
        for(String bad:List.of(first.uri().toString().replace("minwon.ulsan.go.kr","www.ulsan.go.kr"),first.uri().toString().replace("https:","http:"),first.uri()+"&index=2",first.uri()+"#f","https:opaque"))assertThat(p.selectApprovedRequest(URI.create(bad))).isFalse();
        assertThat(p.selectApprovedRequest(new Request(first.uri(),"POST",Map.of("x","y")))).isFalse();assertThat(p.selectApprovedRequest(first,Request.selectGet(URI.create(first.uri().toString().replace("index=1","index=2"))))).isFalse();
    }
    @Test void bodyTitleAndBudgetsAreExplicit(){
        var doc=Jsoup.parse(page(item(1,"hwp")));var uri=sample.profile().selectDetailUri(sample.source());assertThat(UlsanCityNoticePage.selectContent(doc,uri).text()).isEqualTo("소상공인 지원금");AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(doc,"소상공인 지원",sample.titleLayout());
        assertThatThrownBy(()->UlsanCityNoticePage.selectContent(Jsoup.parse(page("").replace("<td colspan=4>","<td colspan=2>")),uri)).isInstanceOf(IllegalArgumentException.class);
        var budget=new AnnouncementAttachmentBbsOfficialObservationTest.Budget(sample.profile(),false);assertThat(budget.maximumRequests).isEqualTo(6);assertThat(budget.maximumBytes).isEqualTo(23L*1024*1024);
    }
    @Test @EnabledIfEnvironmentVariable(named="SANEB_ULSAN_CITY_SURVEY_FIXTURE",matches="true")
    void actualOfficialHtmlContract() throws Exception {
        String html=Files.readString(Path.of("build/qa-ulsan-next-20260930/city-detail.html"));var doc=Jsoup.parse(html);AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(doc,sample.title(),sample.titleLayout());
        var r=sample.profile().selectDescriptors(sample.source(),html);assertThat(r.complete()).as("%s",r.warnings()).isTrue();assertThat(r.descriptors()).hasSize(1).allSatisfy(d->assertThat(d.expectedFormat()).isEqualTo("HWPX"));assertThat(UlsanCityNoticePage.selectContent(doc,sample.profile().selectDetailUri(sample.source())).text()).isNotBlank();
    }
    @Test void catalogIsReferenceOnly() throws Exception {
        var mapper=new com.fasterxml.jackson.databind.ObjectMapper();var refs=mapper.readTree(Files.readString(Path.of("src/main/resources/announcement-attachment/provider-qa-catalog-v2.json"))).path("notices");var ref=StreamSupport.stream(refs.spliterator(),false).filter(r->sample.code().equals(r.path("caseCode").asText())).findFirst().orElseThrow();assertThat(ref.path("source")).isEqualTo(mapper.valueToTree(sample.source()));assertThat(ref.hasNonNull("expectation")).isFalse();
    }
}
