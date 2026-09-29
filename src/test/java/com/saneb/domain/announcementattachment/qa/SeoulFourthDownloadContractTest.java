package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.provider.content.SeoulFourthNoticePage;
import com.saneb.domain.announcementsource.provider.content.SeoulFourthNoticePage.Site;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SeoulFourthDownloadContractTest {
    private String selectItem(String group,int id,String ext){String name="지원 공고."+ext;
        if(group.equals("SEONGDONG")){String q="atchmnflNo="+id+"&bbsNo=184&nttNo=356569";return "<li class=p-attach__item><a class=p-attach__preview href='/previewBbs.do?"+q+"'>미리보기</a><a class='p-attach__link download' href='/\r\n\tmain\t/downloadBbsFile.do?key=1473&"+q+"'><span class=p-icon>파일</span><span>"+name+"</span></a></li>";}
        if(group.equals("SONGPA")){String q="user_file_nm="+name+"&sys_file_nm=stored"+id+"."+ext+"&file_path=/ntishome/file/upload/ofr/ofr/20260629";return "<li><a href=\"javascript:gourl('http://songpa.eminwon.seoul.kr/emwp/jsp/ofr/FileDown.jsp?"+q+"');\">"+name+"</a><a class=p-attach__preview href='/gosiPreview.do?atchmnflNo="+id+"&"+q+"'><svg></svg>미리보기</a></li>";}
        String path="/portal/cmmn/file/fileDown.do?menuNo=200192&atchFileId="+"a".repeat(64)+"&fileSn="+id,url="https://www.gwangjin.go.kr"+path;return "<div><a href='"+path+"' title='"+name+"'><i></i><span class=orignlFileNm>지원 공고.h</span><span class=fileExtsnNm>"+ext+"</span><span>(83 KB)</span></a><br><a href=\"javascript:previewAjax('"+url+"', '"+name+"');\">바로보기</a><a href=\"javascript:preListen('"+url+"', '"+name+"');\">바로듣기</a></div>";
    }
    private String selectPage(String group,String files){String title="소상공인 지원",body="소상공인 지원금";
        return switch(group){
            case "SEONGDONG"->"<div class='p-wrap bbs bbs__view'><table class='p-table block'><tr class=p-table__subject><td><span class=p-table__subject_text>"+title+"</span></td></tr><tr><th>첨부파일</th><td><ul class=p-attach>"+files+"</ul></td></tr><tr><td class=p-table__content><div class=preview_frame><iframe src='/preview'></iframe></div><div class=ntt_cn_container>"+body+"</div></td></tr></table></div>";
            case "SONGPA"->"<div class='p-wrap bbs bbs__view'><form name=gosiFrm><table class='p-table block'><tr><th>제목</th><td>"+title+"</td><th>담당부서</th><td>부서</td></tr><tr><th>내용</th><td class=bbs_content>"+body+"</td></tr><tr><th>파일</th><td><ul class=view_attach>"+files+"</ul></td></tr></table></form></div>";
            default->"<div class=view><div class=t><dl><dt>공고명</dt><dd>"+title+"</dd></dl><dl><dt>내용</dt><dd>"+body+"</dd></dl><dl><dt>첨부파일</dt><dd><script>notExecuted()</script><style>.test{}</style><link rel=stylesheet href='/not-fetched'><div class=fileList>"+files+"</div></dd></dl></div></div>";
        };
    }
    @ParameterizedTest @ValueSource(strings={"SEONGDONG","SONGPA","GWANGJIN"})
    void discoversFilesWithoutPreviewAndKeepsGoodFilesWhenAnotherLinkFails(String group){
        var s=SeoulFourthDownloadCases.selectCase(group);var p=s.profile();String files=selectItem(group,1,"pdf")+selectItem(group,2,"hwp")+selectItem(group,3,"hwpx");var r=p.selectDescriptors(s.source(),selectPage(group,files));assertThat(r.complete()).isTrue();assertThat(r.descriptors()).hasSize(3);
        r.descriptors().forEach(d->{assertThat(d.downloadAllowed()).isTrue();assertThat(d.documentRole()).isEqualTo("UNKNOWN");assertThat(d.selectRequest().method()).isEqualTo("GET");assertThat(p.selectApprovedRequest(d.selectRequest())).isTrue();assertThat(d.fetchUri().getScheme()).isEqualTo("https");});
        for(String extra:List.of("<a href='/unknown'>알 수 없는 파일</a>","<button>첨부</button>","<img src='/unknown'>")){var partial=p.selectDescriptors(s.source(),selectPage(group,files+extra));assertThat(partial.complete()).isFalse();assertThat(partial.descriptors()).hasSize(3);}
        var unsupported=p.selectDescriptors(s.source(),selectPage(group,files+selectItem(group,4,"jpg")));assertThat(unsupported.complete()).isTrue();assertThat(unsupported.descriptors().getLast().downloadAllowed()).isFalse();
        assertThat(p.selectDescriptors(s.source(),selectPage(group,"")).status()).isEqualTo("NO_FILES");assertThat(p.selectDescriptors(s.source(),"<p>첨부 없음</p>").complete()).isFalse();assertThat(p.selectDescriptors(s.source(),selectPage(group,IntStream.rangeClosed(1,11).mapToObj(i->selectItem(group,i,"pdf")).collect(Collectors.joining()))).status()).isEqualTo("LIMIT_EXCEEDED");
    }
    @ParameterizedTest @ValueSource(strings={"SEONGDONG","SONGPA","GWANGJIN"})
    void preservesSourceAndFileIdentityAndRejectsForeignRequests(String group){
        var s=SeoulFourthDownloadCases.selectCase(group);var p=s.profile();var b=p.selectSourceBindings().getFirst();String one=selectItem(group,1,"pdf");var d=p.selectDescriptors(s.source(),selectPage(group,one)).descriptors().getFirst();
        for(String bad:List.of(d.fetchUri().toString().replace("https:","http:"),d.fetchUri().toString().replace(d.fetchUri().getHost(),"evil.example"),d.fetchUri()+"&extra=1",d.fetchUri()+"#x"))assertThat(p.selectApprovedRequest(URI.create(bad))).isFalse();
        var n=new AnnouncementSourceIdentityNormalizer();for(String bad:List.of(s.source().sourceUrl()+"&extra=1",s.source().sourceUrl().replace("https:","http:"),s.source().sourceUrl()+"&"+Site.valueOf(group).idKey+"=99"))assertThatThrownBy(()->p.selectDetailUri(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(bad)),bad,b.localSourceCode(),b.listParserProfileCode()))).hasMessage("PROFILE_REQUIRED");
        assertThatThrownBy(()->p.selectDetailUri(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE","wrong",s.source().sourceUrl(),b.localSourceCode(),b.listParserProfileCode()))).hasMessage("PROFILE_REQUIRED");assertThat(p.selectApprovedRequest(d.selectRequest(),Request.selectGet(URI.create(d.fetchUri()+"&extra=1")))).isFalse();
        assertThat(p.selectDescriptors(s.source(),selectPage(group,one+one)).descriptors()).hasSize(1);assertThat(p.selectDescriptors(s.source()," ".repeat(1_000_001)).complete()).isFalse();
        var conflict=p.selectDescriptors(s.source(),selectPage(group,one+one.replace("지원 공고","다른 공고")));assertThat(conflict.complete()).isFalse();assertThat(conflict.descriptors()).hasSize(1);
        String badPreview=switch(group){case "SEONGDONG"->one.replace("/previewBbs.do?atchmnflNo=1","/previewBbs.do?atchmnflNo=9");case "SONGPA"->one.replace("/gosiPreview.do?atchmnflNo=1&user_file_nm=지원 공고.pdf","/gosiPreview.do?atchmnflNo=1&user_file_nm=다른.pdf");default->one.replace("javascript:preListen(","javascript:evil();preListen(");};var partial=p.selectDescriptors(s.source(),selectPage(group,badPreview));assertThat(partial.complete()).isFalse();assertThat(partial.descriptors()).hasSize(1);
        var budget=new AnnouncementAttachmentBbsOfficialObservationTest.Budget(p,false);assertThat(budget.maximumRequests).isEqualTo(6);assertThat(budget.maximumBytes).isEqualTo(23L*1024*1024);
    }
    @ParameterizedTest @ValueSource(strings={"SEONGDONG","SONGPA","GWANGJIN"})
    void bodyBoundaryAndTitleAreIndependentFromMetadataAndAttachments(String group){var s=SeoulFourthDownloadCases.selectCase(group);var page=Jsoup.parse(selectPage(group,selectItem(group,1,"pdf")));assertThat(SeoulFourthNoticePage.selectContent(Site.valueOf(group),page).text()).isEqualTo("소상공인 지원금");AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,"소상공인 지원",s.titleLayout());assertThatThrownBy(()->SeoulFourthNoticePage.selectContent(Site.valueOf(group),Jsoup.parse(page.toString()+page))).isInstanceOf(IllegalArgumentException.class);}
    @EnabledIfEnvironmentVariable(named="SANEB_SEOUL_FOURTH_SURVEY_FIXTURE",matches="true")
    @ParameterizedTest @ValueSource(strings={"SEONGDONG","SONGPA","GWANGJIN"})
    void officialHtmlBoundaries(String group)throws Exception{
        var s=SeoulFourthDownloadCases.selectCase(group);String html=Files.readString(Path.of("build/qa-seoul-fourth-20260929/"+group+"-detail.html"));var page=Jsoup.parse(html);
        if(group.equals("SONGPA")){
            // 공식 상세의 제목 셀이 비어 있다. 목록 제목을 상세 검증 성공으로 대체하지 않는다.
            assertThatThrownBy(()->AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,s.title(),s.titleLayout())).hasMessage("SEOUL_FOURTH_STRUCTURE_CHANGED");
            var failed=s.profile().selectDescriptors(s.source(),html);assertThat(failed.complete()).isFalse();assertThat(failed.descriptors()).isEmpty();assertThat(failed.warnings()).contains("ATTACHMENT_SELECTOR_CHANGED");return;
        }
        AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,s.title(),s.titleLayout());var r=s.profile().selectDescriptors(s.source(),html);assertThat(r.complete()).isTrue();assertThat(r.descriptors()).hasSize(group.equals("SEONGDONG")?2:3);assertThat(r.descriptors()).allSatisfy(d->assertThat(d.downloadAllowed()).isTrue());assertThat(SeoulFourthNoticePage.selectContent(Site.valueOf(group),page).text()).isNotBlank();
    }
    @ParameterizedTest @ValueSource(strings={"SEONGDONG","SONGPA","GWANGJIN"})
    void catalogIsReferenceOnly(String group)throws Exception{var s=SeoulFourthDownloadCases.selectCase(group);var json=new com.fasterxml.jackson.databind.ObjectMapper();var notices=json.readTree(Files.readString(Path.of("src/main/resources/announcement-attachment/provider-qa-catalog-v2.json"))).path("notices");var ref=StreamSupport.stream(notices.spliterator(),false).filter(n->s.code().equals(n.path("caseCode").asText())).findFirst().orElseThrow();assertThat(ref.path("source")).isEqualTo(json.valueToTree(s.source()));assertThat(ref.hasNonNull("expectation")).isFalse();}
}
