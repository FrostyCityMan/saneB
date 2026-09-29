package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.provider.content.SeoulFifthNoticePage;
import com.saneb.domain.announcementsource.provider.content.SeoulFifthNoticePage.Site;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SeoulFifthDownloadContractTest {
    private String encode(String value){return URLEncoder.encode(value,StandardCharsets.UTF_8);}
    private String selectItem(String group,int id,String ext){var s=Site.valueOf(group);String name="지원 공고."+ext,stored="stored"+id+"."+ext,path="/ntishome/file/upload/ofr/ofr/20260916";
        String url="https://"+s.fileHost+s.download+"?user_file_nm="+encode(name)+"&sys_file_nm="+stored+"&file_path="+encode(path);
        return switch(s){
            case DONGDAEMUN->"<li><a href=\"javascript:goDownLoad('"+name+"','"+stored+"','"+path+"')\">"+name+"</a><button class=p-attach__preview onclick=\"fn_goPreView('"+url+"','"+encode(name)+"');\">미리보기</button></li>";
            case SEONGBUK->"<li><a class=btn_file href='"+url+"'>"+name+"</a><a class=preview href='#n' onclick=\"fn_goPreView('"+url+"','"+name+"','"+stored+"');\">미리보기</a></li>";
            case YEONGDEUNGPO->"<li class=p-attach__item><a class=p-attach__link href='"+url+"'><span class=p-icon>파일</span>"+name+"<i class=p-icon>다운로드</i></a><a class=p-attach__preview href=\"javascript:fn_PreView('"+url+"','"+stored+"','"+encode(name)+"');\">미리보기</a></li>";
        };
    }
    private String selectPage(String group,String files){String title="소상공인 지원",body="소상공인 지원금";
        return switch(group){
            case "DONGDAEMUN"->"<form name=downForm method=post action='https://eminwon.ddm.go.kr/emwp/jsp/ofr/FileDownNew.jsp'><input type=hidden name=user_file_nm><input type=hidden name=sys_file_nm><input type=hidden name=file_path></form><div class='p-wrap bbs bbs__view'><div class=table-responsive><table class='p-table scroll'><tr><th>고시공고명</th><td>"+title+"</td></tr><tr><th>내용</th><td><pre>"+body+"</pre></td></tr><tr><th>첨부파일</th><td><ul>"+files+"</ul></td></tr></table></div></div>";
            case "SEONGBUK"->"<table class='p-table block'><tr><th>제목</th><td>"+title+"</td></tr><tr><th>내용</th><td>"+body+"</td></tr><tr><th>첨부파일</th><td><ul class=upload_list>"+files+"</ul></td></tr></table>";
            default->"<div class='p-wrap bbs bbs__view'><table class='p-table block'><tr class=p-table__subject><td><span class=p-table__subject_text>"+title+"</span></td></tr><tr><td class=p-table__content colspan=4>"+body+"</td></tr><tr><th>파일</th><td><ul class=p-attach>"+files+"</ul></td></tr></table></div>";
        };
    }
    @ParameterizedTest @ValueSource(strings={"DONGDAEMUN","SEONGBUK","YEONGDEUNGPO"})
    void preservesDownloadsAndSeparatesUnsupportedAndUnresolvedLinks(String group){var s=SeoulFifthDownloadCases.selectCase(group);var p=s.profile();String files=selectItem(group,1,"pdf")+selectItem(group,2,"hwp")+selectItem(group,3,"hwpx");var r=p.selectDescriptors(s.source(),selectPage(group,files));assertThat(r.complete()).isTrue();assertThat(r.descriptors()).hasSize(3);
        r.descriptors().forEach(d->{assertThat(d.downloadAllowed()).isTrue();assertThat(d.documentRole()).isEqualTo("UNKNOWN");assertThat(d.selectRequest().method()).isEqualTo(group.equals("DONGDAEMUN")?"POST":"GET");assertThat(p.selectApprovedRequest(d.selectRequest())).isTrue();assertThat(p.selectApprovedRequest(d.selectRequest(),d.selectRequest())).isTrue();});
        for(String extra:List.of("<a href='/unknown'>첨부</a>","<button>파일</button>","<img src='/unknown'>")){var partial=p.selectDescriptors(s.source(),selectPage(group,files+extra));assertThat(partial.complete()).isFalse();assertThat(partial.descriptors()).hasSize(3);}
        var unsupported=p.selectDescriptors(s.source(),selectPage(group,files+selectItem(group,4,"xlsx")));assertThat(unsupported.complete()).isTrue();assertThat(unsupported.descriptors()).hasSize(4);assertThat(unsupported.descriptors().getLast().downloadAllowed()).isFalse();
        assertThat(p.selectDescriptors(s.source(),selectPage(group,"")).status()).isEqualTo("NO_FILES");assertThat(p.selectDescriptors(s.source(),"<p>첨부 없음</p>").complete()).isFalse();assertThat(p.selectDescriptors(s.source()," ".repeat(1_000_001)).complete()).isFalse();
        var limit=p.selectDescriptors(s.source(),selectPage(group,IntStream.rangeClosed(1,11).mapToObj(i->selectItem(group,i,"pdf")).collect(Collectors.joining())));assertThat(limit.status()).isEqualTo("LIMIT_EXCEEDED");assertThat(limit.descriptors()).hasSize(10);
    }
    @ParameterizedTest @ValueSource(strings={"DONGDAEMUN","SEONGBUK","YEONGDEUNGPO"})
    void rejectsForeignRequestsAndChangedFileOrSourceIdentity(String group){var s=SeoulFifthDownloadCases.selectCase(group);var p=s.profile();var b=p.selectSourceBindings().getFirst();String one=selectItem(group,1,"pdf");var d=p.selectDescriptors(s.source(),selectPage(group,one)).descriptors().getFirst();var request=d.selectRequest();
        for(String bad:List.of(d.fetchUri().toString().replace("https:","http:"),d.fetchUri().toString().replace(d.fetchUri().getHost(),"evil.example"),d.fetchUri()+"&extra=1",d.fetchUri()+"#x",d.fetchUri().toString().replace(d.fetchUri().getHost(),"127.0.0.1"))){var next=new Request(URI.create(bad),request.method(),request.form());assertThat(p.selectApprovedRequest(next)).isFalse();assertThat(p.selectApprovedRequest(request,next)).isFalse();}
        assertThatThrownBy(()->new Request(request.uri(),"PUT",request.form())).hasMessage("ATTACHMENT_REQUEST_INVALID");
        var n=new AnnouncementSourceIdentityNormalizer();for(String bad:List.of(s.source().sourceUrl()+"&extra=1",s.source().sourceUrl()+"&notAncmtMgtNo=99",s.source().sourceUrl().replace("https:","http:")))assertThatThrownBy(()->p.selectDetailUri(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(bad)),bad,b.localSourceCode(),b.listParserProfileCode()))).hasMessage("PROFILE_REQUIRED");
        assertThatThrownBy(()->p.selectDetailUri(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE","wrong",s.source().sourceUrl(),b.localSourceCode(),b.listParserProfileCode()))).hasMessage("PROFILE_REQUIRED");
        assertThat(p.selectDescriptors(s.source(),selectPage(group,one+one)).descriptors()).hasSize(1);
        var conflict=p.selectDescriptors(s.source(),selectPage(group,one+one.replace("지원 공고","다른 공고").replace(encode("지원 공고"),encode("다른 공고"))));assertThat(conflict.complete()).isFalse();assertThat(conflict.descriptors()).hasSize(1);
        String badPreview=one.replace(group.equals("YEONGDEUNGPO")?"javascript:fn_PreView(":"fn_goPreView(","evil();preview(");var partial=p.selectDescriptors(s.source(),selectPage(group,badPreview));assertThat(partial.complete()).isFalse();assertThat(partial.descriptors()).hasSize(1);
        var budget=new AnnouncementAttachmentBbsOfficialObservationTest.Budget(p,false);assertThat(budget.maximumRequests).isEqualTo(6);assertThat(budget.maximumBytes).isEqualTo(23L*1024*1024);
    }
    @Test void dongdaemunPrivatePreviewDoesNotDiscardPublicPostOrAuthorizePrivateRequest(){var s=SeoulFifthDownloadCases.selectCase("DONGDAEMUN");var p=s.profile();String page=selectPage("DONGDAEMUN",selectItem("DONGDAEMUN",1,"hwpx").replace("fn_goPreView('https://eminwon.ddm.go.kr", "fn_goPreView('http://127.0.0.1"));var r=p.selectDescriptors(s.source(),page);assertThat(r.complete()).isFalse();assertThat(r.warnings()).contains("ATTACHMENT_LINK_UNRESOLVED");assertThat(r.descriptors()).hasSize(1);var d=r.descriptors().getFirst();assertThat(p.selectApprovedRequest(d.selectRequest())).isTrue();assertThat(p.selectApprovedRequest(Request.selectGet(d.fetchUri()))).isFalse();
        var form=new HashMap<>(d.selectRequest().form());form.put("file_path","/ntishome/file/upload/ofr/ofr/../secret");assertThat(p.selectApprovedRequest(new Request(d.fetchUri(),"POST",form))).isFalse();
        assertThat(p.selectDescriptors(s.source(),page.replace("name=downForm","name=unknown")).descriptors()).isEmpty();assertThat(p.selectDescriptors(s.source(),page.replace("name=file_path","name=file_path value=changed")).descriptors()).isEmpty();
    }
    @ParameterizedTest @ValueSource(strings={"DONGDAEMUN","SEONGBUK","YEONGDEUNGPO"})
    void validatesIndependentTitleAndBodyBoundaries(String group){var s=SeoulFifthDownloadCases.selectCase(group);var page=Jsoup.parse(selectPage(group,selectItem(group,1,"pdf")));assertThat(SeoulFifthNoticePage.selectContent(Site.valueOf(group),page).text()).isEqualTo("소상공인 지원금");AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,"소상공인 지원",s.titleLayout());assertThatThrownBy(()->SeoulFifthNoticePage.selectContent(Site.valueOf(group),Jsoup.parse(page.toString()+page))).isInstanceOf(IllegalArgumentException.class);}
    @EnabledIfEnvironmentVariable(named="SANEB_SEOUL_FIFTH_SURVEY_FIXTURE",matches="true")
    @ParameterizedTest @ValueSource(strings={"DONGDAEMUN","SEONGBUK","YEONGDEUNGPO"})
    void officialHtmlBoundaries(String group)throws Exception{var s=SeoulFifthDownloadCases.selectCase(group);String html=Files.readString(Path.of("build/qa-seoul-fifth-20260929/"+group+"-detail.html"));var page=Jsoup.parse(html);AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,s.title(),s.titleLayout());var r=s.profile().selectDescriptors(s.source(),html);assertThat(r.complete()).isEqualTo(!group.equals("DONGDAEMUN"));assertThat(r.descriptors()).hasSize(group.equals("SEONGBUK")?3:2);assertThat(r.descriptors().stream().filter(AttachmentDiscoveryProfile.Descriptor::downloadAllowed).count()).isEqualTo(group.equals("SEONGBUK")?3:group.equals("DONGDAEMUN")?2:1);if(group.equals("DONGDAEMUN"))assertThat(r.warnings()).contains("ATTACHMENT_LINK_UNRESOLVED");assertThat(SeoulFifthNoticePage.selectContent(Site.valueOf(group),page).text()).isNotBlank();}
    @ParameterizedTest @ValueSource(strings={"DONGDAEMUN","SEONGBUK","YEONGDEUNGPO"})
    void catalogIsReferenceOnlyWithCanonicalSource(String group)throws Exception{var s=SeoulFifthDownloadCases.selectCase(group);var json=new com.fasterxml.jackson.databind.ObjectMapper();var notices=json.readTree(Files.readString(Path.of("src/main/resources/announcement-attachment/provider-qa-catalog-v2.json"))).path("notices");var ref=StreamSupport.stream(notices.spliterator(),false).filter(n->s.code().equals(n.path("caseCode").asText())).findFirst().orElseThrow();assertThat(ref.path("source")).isEqualTo(json.valueToTree(s.source()));assertThat(ref.hasNonNull("expectation")).isFalse();}
}
