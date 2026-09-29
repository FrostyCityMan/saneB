package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.provider.content.SeoulSixthNoticePage;
import com.saneb.domain.announcementsource.provider.content.SeoulSixthNoticePage.Site;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SeoulSixthDownloadContractTest {
    private String encode(String value){return URLEncoder.encode(value,StandardCharsets.UTF_8);}
    private String selectItem(String group,int id,String ext){var site=Site.valueOf(group);String name="지원 공고."+ext,stored="stored_"+id+"."+ext,path="/ntishome/file/upload/ofr/ofr/20260921";
        if(group.equals("YANGCHEON"))return "<li><span class=file-hangul><a class=file href='#' onclick=\"doUrlDownload('"+name+"','"+stored+"', '"+path+"');\">"+name+"</a></span></li>";
        String url="https://"+site.fileHost+"/emwp/jsp/ofr/FileDown.jsp?user_file_nm="+encode(" "+name)+"&sys_file_nm="+stored+"&file_path="+encode(path),q="not_ancmt_mgt_no=41842&streFileNm=41842-"+id+"."+ext;
        return "<li><a class=filedown href='"+url+"'>"+name+"</a><a class=viewbtns href='/synapGosiView.do?"+q+"'>미리보기</a><a class='viewbtns docubrailleview' href='#' onclick=\"viewAttachFileBraille4Speech('"+q+"&Downtype=gosi' ,'"+name+"');return false;\">점자보기</a><a class='viewbtns voiceview' href='#' onclick=\"viewAttachFileBraille('"+q+"&Downtype=gosi' ,'"+name+"');return false;\">음성보기</a></li>";
    }
    private String selectPage(String group,String files){return group.equals("YANGCHEON")?"<form id=SeolCollectVo><div class='new-basic-view basic-view'><div class=view-subj><div id=bbsTitle>소상공인 지원</div></div><div class=view-info>부서 메타데이터</div><div class=view-content><div class=txt-area>소상공인 지원금</div></div><div class=view-attachment><dl><dt>첨부파일</dt><dd><ul class=attached-files>"+files+"</ul></dd></dl></div></div></form>":"<div class=board><div class=board-view><div class=tit><strong>소상공인 지원</strong></div><div class=view-info>부서 메타데이터</div><div class=view-attachment><ul>"+files+"</ul></div><div class=view_contents><div class=txt-area><pre>소상공인 지원금</pre></div></div></div></div>";}
    @ParameterizedTest @ValueSource(strings={"YANGCHEON","GWANAK"})
    void keepsValidDownloadsWhileSeparatingUnsupportedAndUnresolvedFiles(String group){var s=SeoulSixthDownloadCases.selectCase(group);var p=s.profile();String files=selectItem(group,1,"pdf")+selectItem(group,2,"hwp")+selectItem(group,3,"hwpx");var r=p.selectDescriptors(s.source(),selectPage(group,files));assertThat(r.complete()).isTrue();assertThat(r.descriptors()).hasSize(3);r.descriptors().forEach(d->{assertThat(d.downloadAllowed()).isTrue();assertThat(d.documentRole()).isEqualTo("UNKNOWN");assertThat(d.selectRequest().method()).isEqualTo("GET");assertThat(p.selectApprovedRequest(d.selectRequest())).isTrue();assertThat(p.selectApprovedRequest(d.selectRequest(),d.selectRequest())).isTrue();});
        for(String extra:List.of("<a href='/unknown'>첨부</a>","<button>파일</button>","<img src='/unknown'>")){var partial=p.selectDescriptors(s.source(),selectPage(group,files+extra));assertThat(partial.complete()).isFalse();assertThat(partial.descriptors()).hasSize(3);}
        var unsupported=p.selectDescriptors(s.source(),selectPage(group,files+selectItem(group,4,"xlsx")));assertThat(unsupported.complete()).isTrue();assertThat(unsupported.descriptors()).hasSize(4);assertThat(unsupported.descriptors().getLast().downloadAllowed()).isFalse();
        assertThat(p.selectDescriptors(s.source(),selectPage(group,"")).status()).isEqualTo("NO_FILES");assertThat(p.selectDescriptors(s.source(),"<p>첨부 없음</p>").complete()).isFalse();assertThat(p.selectDescriptors(s.source()," ".repeat(1_000_001)).complete()).isFalse();
        var limit=p.selectDescriptors(s.source(),selectPage(group,IntStream.rangeClosed(1,11).mapToObj(i->selectItem(group,i,"pdf")).collect(Collectors.joining())));assertThat(limit.status()).isEqualTo("LIMIT_EXCEEDED");assertThat(limit.descriptors()).hasSize(10);
    }
    @ParameterizedTest @ValueSource(strings={"YANGCHEON","GWANAK"})
    void sourceAndRequestBoundariesRejectForeignHostsMethodsAndDuplicateIdentity(String group){var s=SeoulSixthDownloadCases.selectCase(group);var p=s.profile();var b=p.selectSourceBindings().getFirst();String one=selectItem(group,1,"pdf");var d=p.selectDescriptors(s.source(),selectPage(group,one)).descriptors().getFirst();
        for(String bad:List.of(d.fetchUri().toString().replace("https:","http:"),d.fetchUri().toString().replace(d.fetchUri().getHost(),"evil.example"),d.fetchUri()+"&extra=1",d.fetchUri()+"#x",d.fetchUri().toString().replace(d.fetchUri().getHost(),"127.0.0.1"),d.fetchUri()+"&sys_file_nm=other.pdf")){var next=Request.selectGet(URI.create(bad));assertThat(p.selectApprovedRequest(next)).isFalse();assertThat(p.selectApprovedRequest(d.selectRequest(),next)).isFalse();}
        assertThat(p.selectApprovedRequest(new Request(d.fetchUri(),"POST",Map.of("unexpected","value")))).isFalse();
        var n=new AnnouncementSourceIdentityNormalizer();for(String bad:List.of(s.source().sourceUrl()+"&extra=1",s.source().sourceUrl()+"&not_ancmt_mgt_no=99",s.source().sourceUrl().replace("https:","http:")))assertThatThrownBy(()->p.selectDetailUri(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(bad)),bad,b.localSourceCode(),b.listParserProfileCode()))).hasMessage("PROFILE_REQUIRED");
        assertThatThrownBy(()->p.selectDetailUri(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE","wrong",s.source().sourceUrl(),b.localSourceCode(),b.listParserProfileCode()))).hasMessage("PROFILE_REQUIRED");assertThat(p.selectDescriptors(s.source(),selectPage(group,one+one)).descriptors()).hasSize(1);
        var conflict=p.selectDescriptors(s.source(),selectPage(group,one+one.replace("지원 공고","다른 공고").replace(encode("지원 공고"),encode("다른 공고"))));assertThat(conflict.complete()).isFalse();assertThat(conflict.descriptors()).hasSize(1);
        var budget=new AnnouncementAttachmentBbsOfficialObservationTest.Budget(p,false);assertThat(budget.maximumRequests).isEqualTo(6);assertThat(budget.maximumBytes).isEqualTo(23L*1024*1024);
    }
    @ParameterizedTest @ValueSource(strings={"YANGCHEON","GWANAK"})
    void scriptAndPreviewChangesStayErrorsWithoutExecutingThem(String group){var s=SeoulSixthDownloadCases.selectCase(group);String one=selectItem(group,1,"hwpx");
        if(group.equals("YANGCHEON")){for(String bad:List.of(one.replace("doUrlDownload(","evil();doUrlDownload("),one.replace("/ntishome/file/upload/ofr/ofr/20260921","/ntishome/file/upload/ofr/ofr/../secret"))){var r=s.profile().selectDescriptors(s.source(),selectPage(group,one+bad));assertThat(r.complete()).isFalse();assertThat(r.descriptors()).hasSize(1);}}
        else for(String bad:List.of(one.replace("/synapGosiView.do?not_ancmt_mgt_no=41842","/synapGosiView.do?not_ancmt_mgt_no=99"),one.replace("viewAttachFileBraille4Speech(","evil();viewAttachFileBraille4Speech("))){var r=s.profile().selectDescriptors(s.source(),selectPage(group,bad));assertThat(r.complete()).isFalse();assertThat(r.descriptors()).hasSize(1);}
    }
    @ParameterizedTest @ValueSource(strings={"YANGCHEON","GWANAK"})
    void validatesIndependentTitleAndBodyBoundaries(String group){var s=SeoulSixthDownloadCases.selectCase(group);var page=Jsoup.parse(selectPage(group,selectItem(group,1,"pdf")));assertThat(SeoulSixthNoticePage.selectContent(Site.valueOf(group),page).text()).isEqualTo("소상공인 지원금");AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,"소상공인 지원",s.titleLayout());assertThatThrownBy(()->SeoulSixthNoticePage.selectContent(Site.valueOf(group),Jsoup.parse(page.toString()+page))).isInstanceOf(IllegalArgumentException.class);}
    @EnabledIfEnvironmentVariable(named="SANEB_SEOUL_SIXTH_SURVEY_FIXTURE",matches="true")
    @ParameterizedTest @ValueSource(strings={"YANGCHEON","GWANAK"})
    void officialHtmlBoundaries(String group)throws Exception{var s=SeoulSixthDownloadCases.selectCase(group);String html=Files.readString(Path.of("build/qa-seoul-sixth-20260929/"+group+"-detail.html"));var page=Jsoup.parse(html);AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,s.title(),s.titleLayout());var r=s.profile().selectDescriptors(s.source(),html);assertThat(r.complete()).isTrue();assertThat(r.descriptors()).hasSize(group.equals("YANGCHEON")?3:2);assertThat(r.descriptors()).allSatisfy(d->assertThat(d.downloadAllowed()).isTrue());assertThat(SeoulSixthNoticePage.selectContent(Site.valueOf(group),page).text()).isNotBlank();}
    @ParameterizedTest @ValueSource(strings={"YANGCHEON","GWANAK"})
    void catalogIsReferenceOnlyWithCanonicalSource(String group)throws Exception{var s=SeoulSixthDownloadCases.selectCase(group);var json=new com.fasterxml.jackson.databind.ObjectMapper();var notices=json.readTree(Files.readString(Path.of("src/main/resources/announcement-attachment/provider-qa-catalog-v2.json"))).path("notices");var ref=StreamSupport.stream(notices.spliterator(),false).filter(n->s.code().equals(n.path("caseCode").asText())).findFirst().orElseThrow();assertThat(ref.path("source")).isEqualTo(json.valueToTree(s.source()));assertThat(ref.hasNonNull("expectation")).isFalse();}
}
