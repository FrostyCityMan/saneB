package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import com.saneb.domain.announcementattachment.discovery.*;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

class HwasunDownloadContractTest {
    private String selectLink(int id,String ext){return "<a href=\"javascript:goDownLoad('지원 공고."+ext+"','file"+id+"."+ext+"','/ntishome/file/upload/ofr/ofr/20260910')\">지원 공고."+ext+"</a><br>";}
    private String selectPage(String files){return "<nav>수출 메뉴</nav><form name=form1 method=post><table width='100%' border=0 cellspacing=1 cellpadding=0><tr><td>제목</td><td>소상공인 지원</td></tr><tr><td colspan=4 style='word-break:break-all;'>소상공인 지원금</td></tr><tr><td colspan=4><table><tr><td>첨부파일 :</td><td>"+files+"</td></tr></table></td></tr></table></form><footer>푸터</footer>";}
    @Test void sharesExistingEngineAndPreservesGoodFilesOnPartialFailure(){
        var s=HwasunDownloadCases.selectCase();var p=s.profile();assertThat(p).isInstanceOf(SaeolGetAttachmentDiscoveryProfile.class);
        String files=selectLink(1,"pdf")+selectLink(2,"hwp")+selectLink(3,"hwpx");var r=p.selectDescriptors(s.source(),selectPage(files));assertThat(r.complete()).isTrue();assertThat(r.descriptors()).hasSize(3);
        r.descriptors().forEach(d->{assertThat(d.documentRole()).isEqualTo("UNKNOWN");assertThat(d.downloadAllowed()).isTrue();assertThat(d.selectRequest().method()).isEqualTo("GET");assertThat(p.selectApprovedRequest(d.selectRequest())).isTrue();});
        for(String extra:List.of("<a href='/unknown'>다른 파일</a>","<button>첨부</button>","<img src='/unknown'>")){var partial=p.selectDescriptors(s.source(),selectPage(files+extra));assertThat(partial.complete()).isFalse();assertThat(partial.descriptors()).hasSize(3);}
        var unsupported=p.selectDescriptors(s.source(),selectPage(files+selectLink(4,"jpg")));assertThat(unsupported.descriptors().getLast().downloadAllowed()).isFalse();assertThat(p.selectDescriptors(s.source(),selectPage("")).status()).isEqualTo("NO_FILES");assertThat(p.selectDescriptors(s.source(),"<main>첨부 없음</main>").complete()).isFalse();
        assertThat(p.selectDescriptors(s.source(),selectPage(IntStream.rangeClosed(1,11).mapToObj(i->selectLink(i,"pdf")).collect(Collectors.joining()))).status()).isEqualTo("LIMIT_EXCEEDED");
    }
    @Test void checksSourceAndDownloadBoundaries(){
        var s=HwasunDownloadCases.selectCase();var p=s.profile();var d=p.selectDescriptors(s.source(),selectPage(selectLink(1,"pdf"))).descriptors().getFirst();
        for(String bad:List.of(d.fetchUri().toString().replace("https:","http:"),d.fetchUri().toString().replace("eminwon.hwasun.go.kr","evil.example"),d.fetchUri()+"&extra=1",d.fetchUri()+"#x"))assertThat(p.selectApprovedRequest(URI.create(bad))).isFalse();
        assertThatThrownBy(()->p.selectDetailUri(new AttachmentDiscoveryProfile.Source(s.source().providerCode(),s.source().providerNoticeId(),s.source().sourceUrl(),"LGS-000233",s.source().listParserProfileCode()))).hasMessage("PROFILE_REQUIRED");
        assertThat(p.selectDescriptors(s.source(),selectPage(selectLink(1,"pdf")).replace("method=post","method=get")).complete()).isFalse();
        AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(Jsoup.parse(selectPage("")),"소상공인 지원",s.titleLayout());
        var b=new AnnouncementAttachmentBbsOfficialObservationTest.Budget(p,false);assertThat(b.maximumRequests).isEqualTo(6);assertThat(b.maximumBytes).isEqualTo(23L*1024*1024);
    }
    @EnabledIfEnvironmentVariable(named="SANEB_HWASUN_SURVEY_FIXTURE",matches="true")
    @Test void validatesObservedOfficialHtml()throws Exception{
        var s=HwasunDownloadCases.selectCase();String html=Files.readString(Path.of("build/qa-hwasun-20260929/detail.html"));AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(Jsoup.parse(html),s.title(),s.titleLayout());var r=s.profile().selectDescriptors(s.source(),html);assertThat(r.complete()).isTrue();assertThat(r.descriptors()).hasSize(1);assertThat(r.descriptors().getFirst().expectedFormat()).isEqualTo("HWPX");
    }
    @Test void catalogIsReferenceOnly()throws Exception{
        var s=HwasunDownloadCases.selectCase();var json=new com.fasterxml.jackson.databind.ObjectMapper();var notices=json.readTree(Files.readString(Path.of("src/main/resources/announcement-attachment/provider-qa-catalog-v2.json"))).path("notices");var ref=StreamSupport.stream(notices.spliterator(),false).filter(n->s.code().equals(n.path("caseCode").asText())).findFirst().orElseThrow();assertThat(ref.path("source")).isEqualTo(json.valueToTree(s.source()));assertThat(ref.hasNonNull("expectation")).isFalse();assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectCases("HWASUN").count()).isEqualTo(1);
    }
}
