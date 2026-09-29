package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.provider.content.SeoulSeventhNoticePage;
import com.saneb.domain.announcementsource.provider.content.SeoulSeventhNoticePage.Site;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SeoulSeventhDownloadContractTest {
    private String selectItem(String group,int id,String ext){String name="지원 공고."+ext;
        if(group.equals("SEOUL")){String url="https://seoulboard.seoul.go.kr/comm/getFile?srvcId=BBSTY1&upperNo=466130&fileTy=ATTACH&fileNo="+id+"&bbsNo=277";return "<div class=sib-viw-file><button class=sib-button type=button data-type=preview data-url='"+url+"' data-name='"+name+"'>미리보기</button><p data-srvcid=BBSTY1 data-upperno=466130 data-filety=ATTACH data-fileno="+id+" data-downbbsno=277 data-downnttno=466130><span class=sib-ico-set-file>첨부파일</span><a href='"+url+"'>"+name+"</a> (61 KB Bytes, 다운로드: 258 회 )</p></div>";}
        if(group.equals("SEOUL_JUNGGU")){String q="bid=469&cid=1475799545&fileIndex="+id,stored="stored_"+id+"."+ext;return "<p class=mb05><em class=file>파일</em><a href='/cwsboard/board.do?mode=download&"+q+"&filename="+stored+"'>"+name+"</a><a class=btn_view href='/convert.jsp?"+q+"&filePath=469/&fileName="+stored+"'>미리보기</a></p>";}
        String q="atchFileId=d203e7f162fb41f592ecbefd58ca782b&fileSn="+id;return "<div class=item><a class=file href='/portal/cmmn/file/fileDown.do?menuNo=200233&"+q+"'><i class=ico-hwp></i>"+name+" [용량:110.5 KByte]</a><a class=viewer-link href='/portal/singl/convert/convertToHtml.do?viewType=CONTBODY&"+q+"'>미리보기</a></div>";
    }
    private String selectPage(String group,String files){return switch(group){
        case "SEOUL"->"<div class=sib-viw-type-basic><h3>소상공인 지원</h3><div class=sib-viw-file-list>"+files+"</div><div class=sib-viw-type-basic-content><iframe src='https://invalid.example/viewer'></iframe><div id=scrabArea>소상공인 지원금</div></div></div>";
        case "SEOUL_JUNGGU"->"<div class=board_view_02><table><tr><th class=view_tit>소상공인 지원</th></tr><tr><th>첨부</th><td>"+files+"</td></tr><tr><td class=article_body>소상공인 지원금</td></tr></table></div>";
        default->"<div id=content><div class=bd-view><h2 class=subject>소상공인 지원</h2><div class=table-dl><dl class=file-list><dd><div class=file-list--set><script>function convert(){throw 'never execute';}</script>"+files+"</div></dd></dl></div><div class=dbdata>소상공인 지원금</div></div></div>";};}
    @ParameterizedTest @ValueSource(strings={"SEOUL","SEOUL_JUNGGU","YONGSAN"})
    void keepsGoodFilesWhenOtherLinksFail(String group){var s=SeoulSeventhDownloadCases.selectCase(group);var p=s.profile();String files=selectItem(group,1,"pdf")+selectItem(group,2,"hwp")+selectItem(group,3,"hwpx");var r=p.selectDescriptors(s.source(),selectPage(group,files));assertThat(r.complete()).isTrue();assertThat(r.descriptors()).hasSize(3);r.descriptors().forEach(d->{assertThat(d.downloadAllowed()).isTrue();assertThat(d.documentRole()).isEqualTo("UNKNOWN");assertThat(d.selectRequest().method()).isEqualTo("GET");assertThat(p.selectApprovedRequest(d.selectRequest())).isTrue();assertThat(p.selectApprovedRequest(d.selectRequest(),d.selectRequest())).isTrue();});
        for(String extra:List.of("<a href='/unknown'>첨부</a>","<button>파일</button>","<img src='/unknown'>")){var partial=p.selectDescriptors(s.source(),selectPage(group,files+extra));assertThat(partial.complete()).isFalse();assertThat(partial.descriptors()).hasSize(3);}
        var unsupported=p.selectDescriptors(s.source(),selectPage(group,files+selectItem(group,4,"docx")));assertThat(unsupported.complete()).isTrue();assertThat(unsupported.descriptors()).hasSize(4);assertThat(unsupported.descriptors().getLast().downloadAllowed()).isFalse();
        assertThat(p.selectDescriptors(s.source(),selectPage(group,"")).status()).isEqualTo("NO_FILES");assertThat(p.selectDescriptors(s.source(),"<p>첨부 없음</p>").complete()).isFalse();assertThat(p.selectDescriptors(s.source()," ".repeat(1_000_001)).complete()).isFalse();
        var limit=p.selectDescriptors(s.source(),selectPage(group,IntStream.rangeClosed(1,11).mapToObj(i->selectItem(group,i,"pdf")).collect(Collectors.joining())));assertThat(limit.status()).isEqualTo("LIMIT_EXCEEDED");assertThat(limit.descriptors()).hasSize(10);
    }
    @ParameterizedTest @ValueSource(strings={"SEOUL","SEOUL_JUNGGU","YONGSAN"})
    void enforcesSourceAndRequestIdentity(String group){var s=SeoulSeventhDownloadCases.selectCase(group);var p=s.profile();var site=Site.valueOf(group);var b=p.selectSourceBindings().getFirst();String one=selectItem(group,1,"pdf");var d=p.selectDescriptors(s.source(),selectPage(group,one)).descriptors().getFirst();
        for(String bad:List.of(d.fetchUri().toString().replace("https:","http:"),d.fetchUri().toString().replace(d.fetchUri().getHost(),"evil.example"),d.fetchUri()+"&extra=1",d.fetchUri()+"#x",d.fetchUri().toString().replace(d.fetchUri().getHost(),"127.0.0.1"),d.fetchUri()+"&"+(group.equals("SEOUL")?"fileNo":group.equals("SEOUL_JUNGGU")?"fileIndex":"fileSn")+"=2")){var next=Request.selectGet(URI.create(bad));assertThat(p.selectApprovedRequest(next)).isFalse();assertThat(p.selectApprovedRequest(d.selectRequest(),next)).isFalse();}
        assertThat(p.selectApprovedRequest(new Request(d.fetchUri(),"POST",Map.of("unexpected","value")))).isFalse();var n=new AnnouncementSourceIdentityNormalizer();
        for(String bad:List.of(s.source().sourceUrl()+"&extra=1",s.source().sourceUrl()+"&"+site.idKey+"=99",s.source().sourceUrl().replace("https:","http:")))assertThatThrownBy(()->p.selectDetailUri(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(bad)),bad,b.localSourceCode(),b.listParserProfileCode()))).hasMessage("PROFILE_REQUIRED");
        assertThatThrownBy(()->p.selectDetailUri(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE","wrong",s.source().sourceUrl(),b.localSourceCode(),b.listParserProfileCode()))).hasMessage("PROFILE_REQUIRED");assertThatThrownBy(()->p.selectDetailUri(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",s.source().providerNoticeId(),s.source().sourceUrl(),"LGS-OTHER",b.listParserProfileCode()))).hasMessage("PROFILE_REQUIRED");
        assertThat(p.selectDescriptors(s.source(),selectPage(group,one+one)).descriptors()).hasSize(1);var conflict=p.selectDescriptors(s.source(),selectPage(group,one+one.replace("지원 공고","다른 공고")));assertThat(conflict.complete()).isFalse();assertThat(conflict.descriptors()).hasSize(1);
        var budget=new AnnouncementAttachmentBbsOfficialObservationTest.Budget(p,false);assertThat(budget.maximumRequests).isEqualTo(6);assertThat(budget.maximumBytes).isEqualTo(23L*1024*1024);
    }
    @ParameterizedTest @ValueSource(strings={"SEOUL","SEOUL_JUNGGU","YONGSAN"})
    void previewMismatchDoesNotLoseDownloadAndNeverTriggersPreview(String group){var s=SeoulSeventhDownloadCases.selectCase(group);String one=selectItem(group,1,"hwpx");String bad=switch(group){case "SEOUL"->one.replace("data-name='지원 공고.hwpx'","data-name='다른 공고.hwpx'");case "SEOUL_JUNGGU"->one.replace("/convert.jsp?bid=469","/convert.jsp?bid=99");default->one.replace("viewType=CONTBODY","viewType=OTHER");};var r=s.profile().selectDescriptors(s.source(),selectPage(group,bad));assertThat(r.complete()).isFalse();assertThat(r.descriptors()).hasSize(1);assertThat(r.descriptors().getFirst().downloadAllowed()).isTrue();}
    @ParameterizedTest @ValueSource(strings={"SEOUL","SEOUL_JUNGGU","YONGSAN"})
    void validatesIndependentBodyBoundary(String group){var s=SeoulSeventhDownloadCases.selectCase(group);var page=Jsoup.parse(selectPage(group,selectItem(group,1,"pdf")));assertThat(SeoulSeventhNoticePage.selectContent(Site.valueOf(group),page).text()).isEqualTo("소상공인 지원금");AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,"소상공인 지원",s.titleLayout());assertThatThrownBy(()->SeoulSeventhNoticePage.selectContent(Site.valueOf(group),Jsoup.parse(page.toString()+page))).isInstanceOf(IllegalArgumentException.class);}
    @EnabledIfEnvironmentVariable(named="SANEB_SEOUL_SEVENTH_SURVEY_FIXTURE",matches="true")
    @ParameterizedTest @ValueSource(strings={"SEOUL","SEOUL_JUNGGU","YONGSAN"})
    void officialHtmlBoundaries(String group)throws Exception{var s=SeoulSeventhDownloadCases.selectCase(group);String file=group.equals("SEOUL")?"SEOUL-youth-detail":""+group+"-detail";String html=Files.readString(Path.of("build/qa-seoul-seventh-20260929/"+file+".html"));var page=Jsoup.parse(html);AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,s.title(),s.titleLayout());var r=s.profile().selectDescriptors(s.source(),html);assertThat(r.complete()).isTrue();assertThat(r.descriptors()).hasSize(group.equals("SEOUL")?2:3).allSatisfy(d->assertThat(d.downloadAllowed()).isTrue());assertThat(SeoulSeventhNoticePage.selectContent(Site.valueOf(group),page).text()).isNotBlank();}
    @Test @EnabledIfEnvironmentVariable(named="SANEB_SEOUL_SEVENTH_SURVEY_FIXTURE",matches="true")
    void observedDocxStaysUnsupportedWithoutDownload()throws Exception{String url="https://www.seoul.go.kr/news/news_notice.do?bbsNo=277&nttNo=457461";var n=new AnnouncementSourceIdentityNormalizer();var p=new SeoulSeventhAttachmentProfileConfiguration().selectSeoulProfileDetails();var s=new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(url)),url,"LGS-000001","SAFE_SEOUL_NOTICE");var r=p.selectDescriptors(s,Files.readString(Path.of("build/qa-seoul-seventh-20260929/SEOUL-detail.html")));assertThat(r.complete()).isTrue();assertThat(r.descriptors()).hasSize(1).allSatisfy(d->assertThat(d.downloadAllowed()).isFalse());}
    @ParameterizedTest @ValueSource(strings={"SEOUL","SEOUL_JUNGGU","YONGSAN"})
    void catalogReferenceHasCanonicalIdentity(String group)throws Exception{var s=SeoulSeventhDownloadCases.selectCase(group);var json=new com.fasterxml.jackson.databind.ObjectMapper();var notices=json.readTree(Files.readString(Path.of("src/main/resources/announcement-attachment/provider-qa-catalog-v2.json"))).path("notices");var ref=StreamSupport.stream(notices.spliterator(),false).filter(n->s.code().equals(n.path("caseCode").asText())).findFirst().orElseThrow();assertThat(ref.path("source")).isEqualTo(json.valueToTree(s.source()));assertThat(ref.hasNonNull("expectation")).isFalse();}
}
