package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.provider.content.ChungcheongSixthNoticePage;
import com.saneb.domain.announcementsource.provider.content.ChungcheongSixthNoticePage.Site;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ChungcheongSixthDownloadContractTest {
    static Stream<String> selectGroups(){return ChungcheongSixthDownloadCases.GROUPS.stream().sorted();}
    private String selectItem(String group,int id,String ext){
        String name="지원 공고문  (1)."+ext,stored="stored"+id+"@file."+ext,path="/ntishome/file/upload/ofr/ofr/20260908",args="('"+name+"','"+stored+"', '"+path+"')";
        return "<a href=\"javascript:goDownLoad"+args+"\">"+name+(group.equals("ASAN")?"<span>(211.94kb)</span>":"")+"</a>"+(group.equals("ASAN")?"<br>":"<a href='#' onclick=\"fn_previewDownload"+args+";return false;\" title='새창으로 이동'><img src='/common/images/board/btn_view.jpg' alt='바로보기'></a><br>");
    }
    private String selectPage(String group,String files){
        String content=group.equals("ASAN")?"<div class='customContents'><div class='viewForm'><dl class='ct_th04'><dt>청년농업인 지원</dt><dd>담당자</dd></dl><div class='ct_tc14'><div class='ct_btn04'><b>첨부파일</b>"+files+"</div><div class='ct_tc14'><div class='field-name-body'><div class='field-items'>청년농업인 지원금</div></div></div></div></div></div>"
                :"<form name='form1'><table class='bbs_default view'><tbody><tr class='subject'><th>제목</th><td><span class='subject_text'>청년농업인 지원</span></td></tr><tr><th>담당자</th><td>담당자</td></tr><tr><td colspan='2' title='내용' class='bbs_content'>청년농업인 지원금</td></tr><tr><th><span>첨부파일</span></th><td>"+files+"</td></tr></tbody></table></form>";
        return "<nav>수출 메뉴</nav>"+content+"<form name='nnn' method='post' action='https://eminwon.asan.go.kr/emwp/jsp/ofr/FileDown.jsp'><input type='hidden' name='user_file_nm'><input type='hidden' name='sys_file_nm'><input type='hidden' name='file_path'></form><footer>푸터</footer>";
    }
    @ParameterizedTest @MethodSource("selectGroups") void keepsGoodFilesSeparatesErrorsAndUnsupported(String group){
        var s=ChungcheongSixthDownloadCases.selectCase(group);var p=s.profile();String files=selectItem(group,1,"hwp")+selectItem(group,2,"pdf")+selectItem(group,3,"hwpx");
        var r=p.selectDescriptors(s.source(),selectPage(group,files));assertThat(r.complete()).isTrue();assertThat(r.descriptors()).hasSize(3);r.descriptors().forEach(d->{assertThat(d.documentRole()).isEqualTo("UNKNOWN");assertThat(d.downloadAllowed()).isTrue();assertThat(p.selectApprovedRequest(d.selectRequest())).isTrue();assertThat(d.selectRequest().method()).isEqualTo(group.equals("ASAN")?"POST":"GET");});
        for(String extra:List.of("<a href='/unknown'>다른 첨부</a>","<button>파일</button>","<img src='/unknown'>")){var partial=p.selectDescriptors(s.source(),selectPage(group,files+extra));assertThat(partial.complete()).isFalse();assertThat(partial.descriptors()).hasSize(3);}
        var unsupported=p.selectDescriptors(s.source(),selectPage(group,files+selectItem(group,4,"jpg")));assertThat(unsupported.descriptors()).hasSize(4);assertThat(unsupported.descriptors().getLast().downloadAllowed()).isFalse();
        assertThat(p.selectDescriptors(s.source(),selectPage(group,"")).status()).isEqualTo("NO_FILES");assertThat(p.selectDescriptors(s.source(),"<main>첨부 없음</main>").complete()).isFalse();
        var limit=p.selectDescriptors(s.source(),selectPage(group,IntStream.rangeClosed(1,11).mapToObj(i->selectItem(group,i,"hwp")).collect(Collectors.joining())));assertThat(limit.status()).isEqualTo("LIMIT_EXCEEDED");assertThat(limit.descriptors()).hasSize(10);
    }
    @ParameterizedTest @MethodSource("selectGroups") void bindsIdentityRequestsAndBudget(String group){
        var s=ChungcheongSixthDownloadCases.selectCase(group);var p=s.profile();String one=selectItem(group,1,"hwp"),page=selectPage(group,one);
        assertThat(p.selectDescriptors(s.source(),selectPage(group,one+one)).descriptors()).hasSize(1);
        var conflict=p.selectDescriptors(s.source(),selectPage(group,one+one.replace("지원 공고문  (1).hwp","다른 공고문.hwp")));assertThat(conflict.complete()).isFalse();assertThat(conflict.descriptors()).hasSize(1);
        var request=p.selectDescriptors(s.source(),page).descriptors().getFirst().selectRequest();assertThat(p.selectApprovedRequest(request,request)).isTrue();assertThat(p.selectApprovedRequest(request,Request.selectGet(p.selectDetailUri(s.source())))).isFalse();
        for(String bad:List.of("https:opaque",request.uri().toString().replace("https:","http:"),request.uri().toString().replace(request.uri().getHost(),"evil.example"),request.uri()+"#x"))assertThat(p.selectApprovedRequest(new Request(URI.create(bad),request.method(),request.form()))).isFalse();
        var n=new AnnouncementSourceIdentityNormalizer();for(String bad:List.of(s.source().sourceUrl()+"&unknown=1",s.source().sourceUrl().replace("https:","http:"),s.source().sourceUrl()+"&"+Site.valueOf(group).idKey+"=0"))assertThatThrownBy(()->p.selectDetailUri(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(bad)),bad,s.source().localSourceCode(),s.source().listParserProfileCode()))).hasMessage("PROFILE_REQUIRED");
        assertThatThrownBy(()->p.selectDetailUri(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE","wrong",s.source().sourceUrl(),s.source().localSourceCode(),s.source().listParserProfileCode()))).hasMessage("PROFILE_REQUIRED");
        assertThat(ChungcheongSixthNoticePage.selectContent(Site.valueOf(group),Jsoup.parse(page)).text()).isEqualTo("청년농업인 지원금");AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(Jsoup.parse(page),"청년농업인 지원",s.titleLayout());
        var budget=new AnnouncementAttachmentBbsOfficialObservationTest.Budget(p,false);assertThat(budget.maximumRequests).isEqualTo(6);assertThat(budget.maximumBytes).isEqualTo(23L*1024*1024);budget.reserveBody();assertThat(budget.bytes).isEqualTo(2L*1024*1024);
        assertThat(p.selectDescriptors(s.source()," ".repeat(1048577)).complete()).isFalse();
        for(String bad:List.of(page.replace("javascript:goDownLoad(","javascript:evil();goDownLoad("),page.replace("javascript:goDownLoad(","javascript:goDownLoad(alert(1),"),page.replace("stored1@file.hwp","../secret.hwp")))assertThat(p.selectDescriptors(s.source(),bad).descriptors()).isEmpty();
    }
    @Test void asanPreservesPlainValuesAndObservedAliasesWithoutOpeningRedirects(){
        var s=ChungcheongSixthDownloadCases.selectCase("ASAN");String page=selectPage("ASAN",selectItem("ASAN",1,"pdf"));var p=s.profile();var r=p.selectDescriptors(s.source(),page).descriptors().getFirst().selectRequest();assertThat(r.form().get("user_file_nm")).isEqualTo("지원 공고문  (1).pdf");assertThat(p.selectApprovedRequest(r.uri())).isFalse();
        for(String bad:List.of(page.replace("name='file_path'","name='other'"),page.replace("method='post'","method='get'"),page.replace("eminwon.asan.go.kr","evil.example"),page.replace("(211.94kb)","다른 파일")))assertThat(p.selectDescriptors(s.source(),bad).descriptors()).isEmpty();
        var n=new AnnouncementSourceIdentityNormalizer();String alias=s.source().sourceUrl().replace("www.asan.go.kr","asan.go.kr");var a=p.selectDetailUri(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(alias)),alias,s.source().localSourceCode(),s.source().listParserProfileCode()));assertThat(a.getHost()).isEqualTo("asan.go.kr");assertThat(p.selectApprovedRequest(Request.selectGet(p.selectDetailUri(s.source())),Request.selectGet(a))).isFalse();
    }
    @Test void seosanDoesNotUsePreviewServiceAndRejectsAdditionalQuery(){
        var s=ChungcheongSixthDownloadCases.selectCase("SEOSAN");String page=selectPage("SEOSAN",selectItem("SEOSAN",1,"pdf"));var p=s.profile();var r=p.selectDescriptors(s.source(),page).descriptors().getFirst().selectRequest();assertThat(r.uri().getHost()).isEqualTo("eminwon.seosan.go.kr");assertThat(p.selectApprovedRequest(URI.create(r.uri()+"&other=1"))).isFalse();assertThat(p.selectApprovedRequest(new Request(URI.create("https://eminwon.seosan.go.kr/emwp/jsp/ofr/FileDown.jsp"),"POST",Map.of("user_file_nm","f.pdf","sys_file_nm","s.pdf","file_path","/ntishome/file/upload/ofr/ofr/20260908")))).isFalse();
        var bad=p.selectDescriptors(s.source(),page.replace("fn_previewDownload","unknownPreview"));assertThat(bad.complete()).isFalse();assertThat(bad.descriptors()).hasSize(1);
    }
    @EnabledIfEnvironmentVariable(named="SANEB_CHUNGCHEONG_SIXTH_SURVEY_FIXTURE",matches="true")
    @ParameterizedTest @MethodSource("selectGroups") void actualOfficialBoundaries(String group)throws Exception{
        var s=ChungcheongSixthDownloadCases.selectCase(group);var html=Files.readString(Path.of("build/qa-chungcheong-sixth-20260929/"+group+(group.equals("ASAN")?"-www":"")+"-detail.html"));var page=Jsoup.parse(html);AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,s.title(),s.titleLayout());var r=s.profile().selectDescriptors(s.source(),html);assertThat(r.complete()).as(group).isTrue();assertThat(r.descriptors()).hasSize(2);assertThat(ChungcheongSixthNoticePage.selectContent(Site.valueOf(group),page).text()).contains(group.equals("ASAN")?"청년농업인":"소상공인");
    }
    @Test void catalogStaysReferenceOnly()throws Exception{
        var json=new com.fasterxml.jackson.databind.ObjectMapper();var notices=json.readTree(Files.readString(Path.of("src/main/resources/announcement-attachment/provider-qa-catalog-v2.json"))).path("notices");for(String group:selectGroups().toList()){var s=ChungcheongSixthDownloadCases.selectCase(group);var ref=StreamSupport.stream(notices.spliterator(),false).filter(n->s.code().equals(n.path("caseCode").asText())).findFirst().orElseThrow();assertThat(ref.path("source")).isEqualTo(json.valueToTree(s.source()));assertThat(ref.hasNonNull("expectation")).isFalse();assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectCases(group).count()).isEqualTo(1);}
    }
}
