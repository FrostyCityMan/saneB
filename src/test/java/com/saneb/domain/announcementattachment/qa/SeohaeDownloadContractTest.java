package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import com.saneb.domain.announcementattachment.discovery.AttachmentDiscoveryProfile.Source;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.provider.content.SeohaeNoticePage;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

class SeohaeDownloadContractTest {
    private final AnnouncementAttachmentBbsOfficialObservationTest.ObservationCase sample=SeohaeDownloadCases.selectCase();
    private String item(int id,String extension){return "<li class=margin_b5><a href='/open_content/main/bbs/bbsMsgFileDown.do?bcd=gosi&amp;msg_seq=42495&amp;fileno="+id+"'><img src='/open_content/share/images/filetype/"+extension+".gif'>공고."+extension+"</a><span class='sfont wfont'>(59KByte)</span><a class=btn_preview href='/open_content/main/bbs/bbsMsgFileView.do?bcd=gosi&amp;msg_seq=42495&amp;fileno="+id+"'><span>미리보기</span></a></li>";}
    private String page(String links){return "<div class=board_view><h4 class=title>소상공인 지원</h4><ul class=datalist><li><dl><dt>담당부서</dt><dd>수출 부서</dd></dl></li><li><dl><dt>첨부파일</dt><dd><ul>"+links+"</ul></dd></dl></li></ul><div class=con>소상공인 지원금</div></div>";}
    @Test void independentFilesSurviveDiscoveryErrors(){
        var p=sample.profile();String good=item(1,"pdf")+item(2,"hwp")+item(3,"hwpx");
        var result=p.selectDescriptors(sample.source(),page(good));assertThat(result.complete()).isTrue();assertThat(result.descriptors()).hasSize(3).allSatisfy(d->{assertThat(d.downloadAllowed()).isTrue();assertThat(d.documentRole()).isEqualTo("UNKNOWN");assertThat(p.selectApprovedRequest(d.selectRequest())).isTrue();});
        for(String bad:List.of(item(4,"pdf").replace("href='/open_content","href='https://evil.example/open_content"),item(4,"pdf").replace("msg_seq=42495","msg_seq=42496"),item(4,"pdf").replace("fileno=4","fileno=4&amp;extra=1"),item(4,"pdf").replace("<a ","<a onclick='run()' "),"<script>loadFiles()</script>")){var partial=p.selectDescriptors(sample.source(),page(good+bad));assertThat(partial.complete()).isFalse();assertThat(partial.descriptors()).hasSize(3);}
        assertThat(p.selectDescriptors(sample.source(),page(item(1,"xlsx"))).descriptors().getFirst().downloadAllowed()).isFalse();
    }
    @Test void absenceDuplicatesLimitsAndOfficialAreaRemainSeparate(){
        var p=sample.profile();String one=item(1,"pdf");assertThat(p.selectDescriptors(sample.source(),page("")).status()).isEqualTo("NO_FILES");assertThat(p.selectDescriptors(sample.source(),"<p>오류</p>").complete()).isFalse();
        assertThat(p.selectDescriptors(sample.source(),page(one+one)).descriptors()).hasSize(1);assertThat(p.selectDescriptors(sample.source(),page(one+one.replace("공고.pdf","변경.pdf"))).complete()).isFalse();
        var limit=p.selectDescriptors(sample.source(),page(IntStream.rangeClosed(1,11).mapToObj(i->item(i,"pdf")).collect(Collectors.joining())));assertThat(limit.status()).isEqualTo("LIMIT_EXCEEDED");assertThat(limit.descriptors()).hasSize(10);
        assertThat(p.selectDescriptors(sample.source(),page(one)+"<a href='https://evil.example/a.pdf'>다른 영역</a>").complete()).isTrue();
        assertThat(p.selectDescriptors(sample.source(),page(one)+page(one)).complete()).isFalse();
        assertThat(p.selectDescriptors(sample.source(),page(one).replace("<dt>첨부파일</dt>","<dt>파일 없음</dt>")).complete()).isFalse();
    }
    @Test void onlyPairedKnownPreviewIsIgnoredWithoutExecution(){
        var p=sample.profile();String one=item(1,"hwp");assertThat(p.selectDescriptors(sample.source(),page(one)).complete()).isTrue();
        for(String bad:List.of(one.replace("bbsMsgFileView.do?bcd=gosi","bbsMsgFileView.do?bcd=other"),one.replace("<span>미리보기","<span onclick='run()'>미리보기"),one.replace("<a class=btn_preview","<a onclick='run()' class=btn_preview"),one.replace("<img src=","<img onerror='run()' src="))){var r=p.selectDescriptors(sample.source(),page(bad));assertThat(r.complete()).isFalse();assertThat(r.descriptors()).hasSize(1);}
    }
    @Test void sourceIdentityQueryHostsAndRequestMethodsStayBounded(){
        var p=sample.profile();var n=new AnnouncementSourceIdentityNormalizer();String url=sample.source().sourceUrl();
        String searched=url+"&keyfield=title&keyword=test&listsz=10";assertThat(p.selectDetailUri(new Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(searched)),searched,"LGS-000062","HEURISTIC_NOTICE"))).isEqualTo(p.selectDetailUri(sample.source()));
        for(String bad:List.of(url+"&extra=1",url+"&bcd=gosi",url.replace("bcd=gosi","bcd=news"),url.replace("42495","0"),url.replace("https:","http:"),url.replace("seohae.go.kr","127.0.0.1"),url+"#f")){assertThatThrownBy(()->p.selectDetailUri(new Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(bad)),bad,"LGS-000062","HEURISTIC_NOTICE"))).hasMessage("PROFILE_REQUIRED");}
        assertThatThrownBy(()->p.selectDetailUri(new Source("LOCAL_GOV_NOTICE","wrong",url,"LGS-000062","HEURISTIC_NOTICE"))).hasMessage("PROFILE_REQUIRED");
        var first=p.selectDescriptors(sample.source(),page(item(1,"hwp"))).descriptors().getFirst().selectRequest();
        for(String bad:List.of(first.uri().toString().replace("seohae.go.kr","www.seohae.go.kr"),first.uri().toString().replace("https:","http:"),first.uri()+"&fileno=2",first.uri()+"#f","https:opaque"))assertThat(p.selectApprovedRequest(URI.create(bad))).isFalse();
        assertThat(p.selectApprovedRequest(new Request(first.uri(),"POST",Map.of("x","y")))).isFalse();assertThat(p.selectApprovedRequest(first,Request.selectGet(URI.create(first.uri().toString().replace("fileno=1","fileno=2"))))).isFalse();
    }
    @Test void bodyTitleAndBudgetsAreExplicit(){
        var doc=Jsoup.parse(page(item(1,"hwp")));var uri=sample.profile().selectDetailUri(sample.source());assertThat(SeohaeNoticePage.selectContent(doc,uri).text()).isEqualTo("소상공인 지원금");AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(doc,"소상공인 지원",sample.titleLayout());
        assertThatThrownBy(()->SeohaeNoticePage.selectContent(Jsoup.parse(page("").replace("class=con","class=unknown")),uri)).isInstanceOf(IllegalArgumentException.class);
        var budget=new AnnouncementAttachmentBbsOfficialObservationTest.Budget(sample.profile(),false);assertThat(budget.maximumRequests).isEqualTo(6);assertThat(budget.maximumBytes).isEqualTo(23L*1024*1024);
    }
    @Test @EnabledIfEnvironmentVariable(named="SANEB_SEOHAE_SURVEY_FIXTURE",matches="true")
    void actualOfficialHtmlContract() throws Exception {
        String html=Files.readString(Path.of("build/qa-seohae-20260930/detail.html"));var doc=Jsoup.parse(html);AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(doc,sample.title(),sample.titleLayout());
        var r=sample.profile().selectDescriptors(sample.source(),html);assertThat(r.complete()).as("%s",r.warnings()).isTrue();assertThat(r.descriptors()).hasSize(1).allSatisfy(d->assertThat(d.expectedFormat()).isEqualTo("HWP"));assertThat(SeohaeNoticePage.selectContent(doc,sample.profile().selectDetailUri(sample.source())).text()).isNotBlank();
    }
    @Test void catalogIsReferenceOnly() throws Exception {
        var mapper=new com.fasterxml.jackson.databind.ObjectMapper();var refs=mapper.readTree(Files.readString(Path.of("src/main/resources/announcement-attachment/provider-qa-catalog-v2.json"))).path("notices");var ref=StreamSupport.stream(refs.spliterator(),false).filter(r->sample.code().equals(r.path("caseCode").asText())).findFirst().orElseThrow();assertThat(ref.path("source")).isEqualTo(mapper.valueToTree(sample.source()));assertThat(ref.hasNonNull("expectation")).isFalse();
    }
}
