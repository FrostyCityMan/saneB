package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import java.net.URI;
import java.util.*;
import java.util.stream.*;
import org.jsoup.Jsoup;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import com.saneb.domain.announcementattachment.discovery.AttachmentDiscoveryProfile;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.classification.*;

class JeonnamFirstDownloadContractTest {
    static Stream<String> selectGroups(){return JeonnamFirstDownloadCases.GROUPS.stream();}
    static boolean selectPost(String g){return Set.of("MOKPO","YEOSU","MUAN").contains(g);}
    static String selectLink(String g,int id,String ext){
        String name="공고"+id+"."+ext,host="eminwon."+g.toLowerCase(Locale.ROOT)+".go.kr";
        if(selectPost(g)){
            String user=g.equals("MUAN")?"user"+"A".repeat(30)+id:name;
            return "<li><a href='#none' onclick=\"goDownLoad('"+user+"','system"+"B".repeat(30)+id+"','/ntisho"+"C".repeat(64)+"')\">"+(g.equals("MUAN")?"":"<span class='icon file_icon'>hwp파일</span>")+name+"</a></li>";
        }
        String uri="https://"+host+"/emwp/jsp/ofr/FileDown.jsp?user_file_nm="+name+"&sys_file_nm="+id+"."+ext+"&file_path=/ntishome/file/upload/ofr/ofr/20260929";
        String notice=g.equals("NAJU")?"40288":"28097";
        if(g.equals("NAJU"))return "<li><span class=file><span class=name>공고"+id+"</span><span class=info>["+ext+"]</span></span><button class=btn_text type=button onclick=\"window.open('"+uri+"', '_blank')\">다운로드</button><button class=btn_text type=button onclick=\"window.open('/Viewer_gosi/"+notice+"_"+id+"', '_blank')\">바로가기</button></li>";
        return "<li><span class=txt><span class=name>"+name+"</span><span class=info>["+ext+", 1KB]</span></span><a class=ico_down href='"+uri+"'>다운로드</a><a class=ico_view href='/Viewer_gosi/"+notice+"_"+id+"'>바로보기</a></li>";
    }
    static String selectPage(String g,String links,int count){
        String title=JeonnamFirstDownloadCases.selectCase(g).title();
        String form=selectPost(g)?"<form name=nnn method=post action='https://eminwon."+g.toLowerCase(Locale.ROOT)+".go.kr/emwp/jsp/ofr/FileDownNew.jsp'><input type=hidden name=user_file_nm><input type=hidden name=sys_file_nm><input type=hidden name=file_path>"+(g.equals("YEOSU")?"":"<input type=hidden name=csrf_token value='"+"a".repeat(64)+"'>")+"</form>":"";
        return form+switch(g){
            case "MOKPO","YEOSU"->"<div class=view_titlebox><h3>"+title+"</h3></div><div class=file_viewbox><div class=left_box><strong>첨부파일</strong></div><div class=right_box><ul>"+links+"</ul></div></div>";
            case "MUAN"->"<div id=board_basic_view><div class=news_tit><h3>"+title+"</h3></div><div class=file_attach><h5>첨부파일(<strong>"+count+"</strong>)</h5><div class=attach_thum><ul>"+links+"</ul></div></div></div>";
            case "NAJU"->"<div class=view_title><p class=title>"+title+"</p></div><div class='view_box file_area'><div class=file_tit><span class=tit>첨부파일</span></div><div class=file_cnt><ul class=file_list>"+links+"</ul></div></div>";
            default->"<div class=view_titlebox><h3>"+title+"</h3></div><div><div class=file_head><span>첨부파일</span></div><div class=file_body><ul class=file_list>"+links+"</ul></div></div>";
        };
    }
    @ParameterizedTest @MethodSource("selectGroups") void collectsKnownFilesAlongsideUnknownAndUnsupportedWithoutPrintingDocuments(String g){
        var s=JeonnamFirstDownloadCases.selectCase(g);var p=s.profile();
        var complete=p.selectDescriptors(s.source(),selectPage(g,selectLink(g,1,"hwp"),1));assertThat(complete.complete()).isTrue();assertThat(complete.descriptors()).hasSize(1);
        var partial=p.selectDescriptors(s.source(),selectPage(g,selectLink(g,1,"pdf")+selectLink(g,2,"xlsx")+"<a href='/unknown'>미확인</a>",2));
        assertThat(partial.complete()).isFalse();assertThat(partial.descriptors()).hasSize(2);assertThat(partial.descriptors().getFirst().downloadAllowed()).isTrue();assertThat(partial.descriptors().getLast().downloadAllowed()).isFalse();
        partial.descriptors().forEach(d->{assertThat(d.documentRole()).isEqualTo("UNKNOWN");assertThat(d.toString()).doesNotContain("공고","csrf", "http");});
        assertThat(p.selectDescriptors(s.source(),selectPage(g,"",0)).status()).isEqualTo("NO_FILES");
        assertThat(p.selectDescriptors(s.source(),"<html>오류</html>").complete()).isFalse();
        assertThat(p.selectDescriptors(s.source(),selectPage(g,"<li></li>",0)).complete()).isFalse();
        String many=IntStream.rangeClosed(1,11).mapToObj(i->selectLink(g,i,"hwp")).collect(Collectors.joining());
        var limit=p.selectDescriptors(s.source(),selectPage(g,many,11));assertThat(limit.status()).isEqualTo("LIMIT_EXCEEDED");assertThat(limit.descriptors()).hasSize(10);
    }
    @ParameterizedTest @MethodSource("selectGroups") void enforcesOfficialSourceTitleAndNoRedirect(String g)throws Exception{
        var s=JeonnamFirstDownloadCases.selectCase(g);var p=s.profile();var source=s.source();
        assertThatThrownBy(()->p.selectDetailUri(new AttachmentDiscoveryProfile.Source(source.providerCode(),source.providerNoticeId(),source.sourceUrl(),"LGS-000001",source.listParserProfileCode()))).isInstanceOf(IllegalArgumentException.class);
        URI detail=p.selectDetailUri(source);assertThat(p.selectApprovedRequest(URI.create(detail.toString().replace("https:","http:")))).isFalse();
        assertThat(p.selectApprovedRequest(URI.create("https:/missing-host"))).isFalse();
        assertThat(p.selectApprovedRequest(URI.create(detail+"&extra=1"))).isFalse();assertThat(p.selectApprovedRequest(URI.create(detail+"&idx=1"))).isFalse();
        String html=selectPage(g,selectLink(g,1,"hwp"),1);var page=Jsoup.parse(html);AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,s.title(),s.titleLayout());
        assertThatThrownBy(()->AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,"다른 공고",s.titleLayout())).isInstanceOf(AssertionError.class);
        var d=p.selectDescriptors(source,html).descriptors().getFirst();assertThat(p.selectApprovedRequest(d.selectRequest())).isTrue();
        assertThat(p.selectApprovedRequest(d.selectRequest(),new Request(URI.create(d.fetchUri().toString().replace("eminwon.","evil.")),d.selectRequest().method(),d.postForm()))).isFalse();
        assertThat(p.selectDescriptors(source,html+"<a href='/screen_print.pdf'>인쇄</a>").descriptors()).hasSize(1);
        var decision=new AnnouncementSourceClassificationEngine().selectDecision(new AnnouncementSourceClassificationInput("LOCAL_GOV_NOTICE",s.title(),null,null,List.of(),AnnouncementSourceClassificationCodes.BodySourceCode.NONE,AnnouncementSourceClassificationCodes.BodyAvailabilityCode.UNAVAILABLE),AnnouncementAttachmentRealFileQaTest.selectDraftRuleSet());
        assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectTitleMayProceed(decision)).isTrue();
    }
    @ParameterizedTest @ValueSource(strings={"MOKPO","YEOSU","MUAN"}) void fixedPublicFormFieldsAreEphemeralAndJavascriptCannotBeExtended(String g){
        var s=JeonnamFirstDownloadCases.selectCase(g);var p=s.profile();String html=selectPage(g,selectLink(g,1,"hwp"),1);
        var d=p.selectDescriptors(s.source(),html).descriptors().getFirst();assertThat(d.selectRequest().toString()).doesNotContain("a".repeat(64),"system");assertThat(d.locator().toString()).doesNotContain("a".repeat(64),"csrf");
        if(!g.equals("YEOSU")){
            assertThat(d.postForm().get("csrf_token")).isEqualTo("a".repeat(64));
            var missing=new HashMap<>(d.postForm());missing.remove("csrf_token");assertThat(p.selectApprovedRequest(new Request(d.fetchUri(),"POST",missing))).isFalse();
            assertThat(p.selectDescriptors(s.source(),html.replace("a".repeat(64),"invalid")).complete()).isFalse();
        }
        assertThat(p.selectDescriptors(s.source(),html.replace("name=file_path","name=unknown")).complete()).isFalse();
        assertThat(p.selectDescriptors(s.source(),selectPage(g,selectLink(g,1,"hwp").replace(")\">",");alert(1)\">"),1)).descriptors()).isEmpty();
        if(g.equals("MUAN"))assertThat(p.selectDescriptors(s.source(),selectPage(g,"",1)).complete()).isFalse();
    }
    @ParameterizedTest @ValueSource(strings={"NAJU","GANGJIN"}) void knownPreviewDoesNotHideUnknownLinksOrOtherNotice(String g){
        var s=JeonnamFirstDownloadCases.selectCase(g);var p=s.profile();String links=selectLink(g,1,"hwp");
        var bad=p.selectDescriptors(s.source(),selectPage(g,links.replace("/Viewer_gosi/"+(g.equals("NAJU")?"40288":"28097"),"/Viewer_gosi/999999"),1));assertThat(bad.complete()).isFalse();assertThat(bad.descriptors()).hasSize(1);
        String malformed=g.equals("NAJU")?"<button onclick=\"window.open('https:/emwp/jsp/ofr/FileDown.jsp', '_blank')\">잘못된 링크</button>":"<a href='https:/emwp/jsp/ofr/FileDown.jsp'>잘못된 링크</a>";
        var mixed=p.selectDescriptors(s.source(),selectPage(g,links+malformed,1));assertThat(mixed.complete()).isFalse();assertThat(mixed.descriptors()).hasSize(1);
        var script=p.selectDescriptors(s.source(),selectPage(g,links+"<script>alert(1)</script>",1));assertThat(script.complete()).isFalse();assertThat(script.descriptors()).hasSize(1);
        if(g.equals("NAJU"))assertThat(p.selectDescriptors(s.source(),selectPage(g,links.replace("'_blank')","'_blank');alert(1)"),1)).descriptors()).isEmpty();
    }
}
