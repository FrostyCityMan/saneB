package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.provider.content.ChungcheongFifthNoticePage;
import com.saneb.domain.announcementsource.provider.content.ChungcheongFifthNoticePage.Site;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
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
import org.junit.jupiter.params.provider.MethodSource;

class ChungcheongFifthDownloadContractTest {
    static Stream<String> selectGroups(){return ChungcheongFifthDownloadCases.GROUPS.stream().sorted();}
    private String selectItem(String group,int id,String ext){
        String name="지원 공고문  (1)."+ext,stored="20260908093000_"+String.format("%030d",id)+"."+ext,path="/ntishome/file/upload/ofr/ofr/20260908",args="('"+name+"','"+stored+"', '"+path+"');";
        if(group.equals("GEUMSAN"))return "<a class='btn-file' href='/_prog/download/?func_gbn_cd=gosi&amp;site_dvs_cd=kr&amp;filename="+stored+"&amp;file_realname="+URLEncoder.encode(name,StandardCharsets.UTF_8)+"'>"+name+"</a>";
        return "<a href='javascript:void(0);' onclick=\"fn_saeol_downFile"+args+"\" title='"+name+"'>"+name+"<strong><strong>&nbsp;(173.52 KB)</strong></strong></a>&nbsp;<a href='javascript:void(0);' onclick=\"fn_egov_preview_File"+args+"\" title='미리보기 새창열림'><img src='/images/common/buyeo_see_btn.gif' alt='바로보기'></a><br>";
    }
    private String selectPage(String group,String files){
        String content=group.equals("GEUMSAN")?"<div class='program--contents'><div class='ui bbs--view'><div class='ui bbs--view--header'><h2 class='ui bbs--view--tit'>소상공인 지원</h2><span>담당자</span></div><div class='ui bbs--view--file'>"+files+"</div><div class='ui bbs--view--cont'><div class='ui bbs--detail--cont'><div class='ui bbs--view--content'>소상공인 지원금</div></div></div></div></div>"
                :"<section id='con_body'><div id='txt'><div class='board_viewTit'><h4>소상공인 지원</h4></div><ul class='board_viewInfo'><li>담당자</li></ul><div class='board_viewDetail'>소상공인 지원금</div><ul class='board_viewInfo'><li class='file'><span>파일</span><div>"+files+"</div></li></ul></div></section>";
        return "<nav>수출 메뉴</nav>"+content+"<form name='fileForm' method='post'><input type='hidden' name='user_file_nm' value=''><input type='hidden' name='sys_file_nm' value=''><input type='hidden' name='file_path' value=''></form><footer>푸터</footer>";
    }
    @ParameterizedTest @MethodSource("selectGroups") void keepsGoodFilesSeparatesErrorsAndUnsupported(String group){
        var s=ChungcheongFifthDownloadCases.selectCase(group);var p=s.profile();String files=selectItem(group,1,"hwp")+selectItem(group,2,"pdf")+selectItem(group,3,"hwpx");
        var r=p.selectDescriptors(s.source(),selectPage(group,files));assertThat(r.complete()).isTrue();assertThat(r.descriptors()).hasSize(3);r.descriptors().forEach(d->{assertThat(d.documentRole()).isEqualTo("UNKNOWN");assertThat(d.downloadAllowed()).isTrue();assertThat(p.selectApprovedRequest(d.selectRequest())).isTrue();assertThat(d.selectRequest().method()).isEqualTo(group.equals("GEUMSAN")?"GET":"POST");});
        for(String extra:List.of("<a href='/unknown'>다른 첨부</a>","<button>파일</button>","<img src='/unknown'>")){var partial=p.selectDescriptors(s.source(),selectPage(group,files+extra));assertThat(partial.complete()).isFalse();assertThat(partial.descriptors()).hasSize(3);}
        var unsupported=p.selectDescriptors(s.source(),selectPage(group,files+selectItem(group,4,"jpg")));assertThat(unsupported.descriptors()).hasSize(4);assertThat(unsupported.descriptors().getLast().downloadAllowed()).isFalse();
        assertThat(p.selectDescriptors(s.source(),selectPage(group,"")).status()).isEqualTo("NO_FILES");assertThat(p.selectDescriptors(s.source(),"<main>첨부 없음</main>").complete()).isFalse();
        var limit=p.selectDescriptors(s.source(),selectPage(group,IntStream.rangeClosed(1,11).mapToObj(i->selectItem(group,i,"hwp")).collect(Collectors.joining())));assertThat(limit.status()).isEqualTo("LIMIT_EXCEEDED");assertThat(limit.descriptors()).hasSize(10);
    }
    @ParameterizedTest @MethodSource("selectGroups") void bindsIdentityAndBudget(String group){
        var s=ChungcheongFifthDownloadCases.selectCase(group);var p=s.profile();String one=selectItem(group,1,"hwp"),page=selectPage(group,one);
        assertThat(p.selectDescriptors(s.source(),selectPage(group,one+one)).descriptors()).hasSize(1);
        var conflict=p.selectDescriptors(s.source(),selectPage(group,one+one.replace("지원 공고문  (1).hwp","다른 공고문.hwp").replace(URLEncoder.encode("지원 공고문  (1).hwp",StandardCharsets.UTF_8),URLEncoder.encode("다른 공고문.hwp",StandardCharsets.UTF_8))));assertThat(conflict.complete()).isFalse();assertThat(conflict.descriptors()).hasSize(1);
        var request=p.selectDescriptors(s.source(),page).descriptors().getFirst().selectRequest();assertThat(p.selectApprovedRequest(request,request)).isTrue();assertThat(p.selectApprovedRequest(request,Request.selectGet(p.selectDetailUri(s.source())))).isFalse();
        for(String bad:List.of("https:opaque",request.uri().toString().replace("https:","http:"),request.uri().toString().replace(request.uri().getHost(),"evil.example"),request.uri()+"#x"))assertThat(p.selectApprovedRequest(new Request(URI.create(bad),request.method(),request.form()))).isFalse();
        var n=new AnnouncementSourceIdentityNormalizer();for(String bad:List.of(s.source().sourceUrl()+"&unknown=1",s.source().sourceUrl().replace("https:","http:"),s.source().sourceUrl()+"&mng_no=0"))assertThatThrownBy(()->p.selectDetailUri(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(bad)),bad,s.source().localSourceCode(),s.source().listParserProfileCode()))).hasMessage("PROFILE_REQUIRED");
        assertThatThrownBy(()->p.selectDetailUri(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE","wrong",s.source().sourceUrl(),s.source().localSourceCode(),s.source().listParserProfileCode()))).hasMessage("PROFILE_REQUIRED");
        assertThat(ChungcheongFifthNoticePage.selectContent(Site.valueOf(group),Jsoup.parse(page)).text()).isEqualTo("소상공인 지원금");AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(Jsoup.parse(page),"소상공인 지원",s.titleLayout());
        var budget=new AnnouncementAttachmentBbsOfficialObservationTest.Budget(p,false);assertThat(budget.maximumRequests).isEqualTo(6);assertThat(budget.maximumBytes).isEqualTo((group.equals("GEUMSAN")?26L:23L)*1024*1024);budget.reserveBody();assertThat(budget.bytes).isEqualTo((group.equals("GEUMSAN")?4L:2L)*1024*1024);
        assertThat(p.selectDescriptors(s.source()," ".repeat(1_050_000)+page).complete()).isEqualTo(group.equals("GEUMSAN"));assertThat(p.selectDescriptors(s.source()," ".repeat(2_097_153)).complete()).isFalse();
    }
    @Test void buyeoUsesPlainFormAndRejectsChangedFormsAndCode(){
        var s=ChungcheongFifthDownloadCases.selectCase("BUYEO");String page=selectPage("BUYEO",selectItem("BUYEO",1,"pdf"));var p=s.profile();var request=p.selectDescriptors(s.source(),page).descriptors().getFirst().selectRequest();assertThat(request.form().get("user_file_nm")).isEqualTo("지원 공고문  (1).pdf");assertThat(p.selectApprovedRequest(request.uri())).isFalse();
        for(String bad:List.of(page.replace("name='file_path' value=''","name='file_path' value='/other'"),page.replace("method='post'","method='get'"),page.replace("name='fileForm'","name='otherForm'"))){var r=p.selectDescriptors(s.source(),bad);assertThat(r.complete()).isFalse();assertThat(r.descriptors()).isEmpty();}
        for(String bad:List.of("../secret.pdf","a%2F.pdf")){var form=new HashMap<>(request.form());form.put("sys_file_nm",bad);assertThat(p.selectApprovedRequest(new Request(request.uri(),"POST",form))).isFalse();}
        var controlForm=new HashMap<>(request.form());controlForm.put("sys_file_nm","a\n.pdf");assertThatThrownBy(()->new Request(request.uri(),"POST",controlForm)).hasMessage("ATTACHMENT_FORM_INVALID");
        for(String bad:List.of(page.replace("fn_saeol_downFile(","evil();fn_saeol_downFile("),page.replace("fn_saeol_downFile(","fn_saeol_downFile(alert(1),"),page.replace("(173.52 KB)","다른 파일")))assertThat(p.selectDescriptors(s.source(),bad).descriptors()).isEmpty();
        var r=p.selectDescriptors(s.source(),page.replace("fn_egov_preview_File","unknownPreview"));assertThat(r.complete()).isFalse();assertThat(r.descriptors()).hasSize(1);
    }
    @Test void geumsanRejectsQueryAndNameConfusion(){
        var s=ChungcheongFifthDownloadCases.selectCase("GEUMSAN");String page=selectPage("GEUMSAN",selectItem("GEUMSAN",1,"pdf"));var p=s.profile();
        for(String bad:List.of(page.replace("site_dvs_cd=kr","site_dvs_cd=other"),page.replace("func_gbn_cd=gosi","func_gbn_cd=gosi&amp;func_gbn_cd=gosi"),page.replace("/_prog/download/","https://evil.example/_prog/download/"),page.replace("filename=20260908093000_","filename=../20260908093000_"),page.replace("file_realname=","file_realname=%0A")))assertThat(p.selectDescriptors(s.source(),bad).descriptors()).isEmpty();
    }
    @EnabledIfEnvironmentVariable(named="SANEB_CHUNGCHEONG_FIFTH_SURVEY_FIXTURE",matches="true")
    @ParameterizedTest @MethodSource("selectGroups") void actualOfficialBoundaries(String group)throws Exception{
        var s=ChungcheongFifthDownloadCases.selectCase(group);var html=Files.readString(Path.of("build/qa-chungcheong-fifth-20260929/"+group+"-detail.html"));var page=Jsoup.parse(html);AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,s.title(),s.titleLayout());var r=s.profile().selectDescriptors(s.source(),html);assertThat(r.complete()).as(group).isTrue();assertThat(r.descriptors()).hasSize(2);assertThat(ChungcheongFifthNoticePage.selectContent(Site.valueOf(group),page).text()).contains("소상공인");
    }
    @Test void catalogStaysReferenceOnly()throws Exception{
        var json=new com.fasterxml.jackson.databind.ObjectMapper();var notices=json.readTree(Files.readString(Path.of("src/main/resources/announcement-attachment/provider-qa-catalog-v2.json"))).path("notices");for(String group:selectGroups().toList()){var s=ChungcheongFifthDownloadCases.selectCase(group);var ref=StreamSupport.stream(notices.spliterator(),false).filter(n->s.code().equals(n.path("caseCode").asText())).findFirst().orElseThrow();assertThat(ref.path("source")).isEqualTo(json.valueToTree(s.source()));assertThat(ref.hasNonNull("expectation")).isFalse();assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectCases(group).count()).isEqualTo(1);}
    }
}
