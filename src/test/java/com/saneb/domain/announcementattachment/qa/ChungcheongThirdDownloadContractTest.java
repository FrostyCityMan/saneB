package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.provider.content.ChungcheongThirdNoticePage;
import com.saneb.domain.announcementsource.provider.content.ChungcheongThirdNoticePage.Site;
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

class ChungcheongThirdDownloadContractTest {
    static Stream<String> selectGroups(){return ChungcheongThirdDownloadCases.GROUPS.stream().sorted();}
    private String selectItem(String group,int id,String ext){
        String name="지원 공고문."+ext;
        if(group.equals("GONGJU")){
            String args="('"+name+"','stored"+id+"."+ext+"', '/ntishome/file/upload/ofr/ofr/20260908');";
            return "<a href=\"javascript:fn_egov_downFile"+args+"\"><i></i>"+name+"</a><a href=\"javascript:fn_egov_preview_File"+args+"\">파일 바로보기<i></i></a>";
        }
        String fid="%23"+"a".repeat(64),other="%23"+"b".repeat(96);
        return "<div class='attach__item'><span class='text'><em>"+name+"</em></span><a class='attach_btn down' href='https://sido.chungbuk.go.kr/citynet/jsp/cmm/attach/download.jsp?mode=download&fid="+fid+"&index="+id+"&other="+other+"'><span>다운로드</span></a><a class='attach_btn preview' href='./previewGosiPblancAtchmnfl.do?no=67302&fileIndex="+id+"&fileFid="+fid+"&fileOther="+other+"'><span>미리보기</span></a></div>";
    }
    private String selectPage(String group,String files){
        if(group.equals("CHUNGBUK"))return "<nav>수출 메뉴</nav><div class='p-wrap bbs bbs__view uiux_type'><div class='bbs_viewbox'><div class='subjectbox'><span class='subject'>소상공인 지원</span><div class='fieldlistbox'>담당자</div></div><div class='viewcontentbox'><div class='viewcontent'><div class='contenttext'>소상공인 지원금</div></div><div class='viewcontent'><div class='attachedfile'><span class='attach_tit'>첨부파일</span><div class='attach_list'>"+files+"</div></div></div></div></div></div><footer>푸터</footer>";
        return "<nav>수출 메뉴</nav><div class='program--contents'><div class='ui bbs--view'><div class='ui bbs--view--header'><h2 class='ui bbs--view--tit'>소상공인 지원</h2><span>담당자</span></div><div class='ui bbs--view--file'>"+files+"</div><div class='ui bbs--view--cont'><div class='ui bbs--detail--cont'><div class='ui bbs--view--content'>소상공인 지원금</div></div></div></div></div><form id='fileForm' name='fileForm' method='post' action='https://eminwon.gongju.go.kr/emwp/jsp/ofr/FileDown.jsp'><input type='hidden' name='user_file_nm' value=''><input type='hidden' name='sys_file_nm' value=''><input type='hidden' name='file_path' value=''></form><footer>푸터</footer>";
    }
    @ParameterizedTest @MethodSource("selectGroups") void keepsGoodFilesAndSeparatesUnknownUnsupportedAndLimits(String group){
        var s=ChungcheongThirdDownloadCases.selectCase(group);var p=s.profile();String files=selectItem(group,1,"hwp")+selectItem(group,2,"pdf")+selectItem(group,3,"hwpx");
        var r=p.selectDescriptors(s.source(),selectPage(group,files));assertThat(r.complete()).isTrue();assertThat(r.descriptors()).hasSize(3);r.descriptors().forEach(d->{assertThat(d.documentRole()).isEqualTo("UNKNOWN");assertThat(d.downloadAllowed()).isTrue();assertThat(p.selectApprovedRequest(d.selectRequest())).isTrue();});
        for(String extra:List.of("<a href='/unknown'>다른 첨부</a>","<button>파일</button>","<img src='/unknown'>")){var partial=p.selectDescriptors(s.source(),selectPage(group,files+extra));assertThat(partial.complete()).isFalse();assertThat(partial.descriptors()).hasSize(3);}
        var unsupported=p.selectDescriptors(s.source(),selectPage(group,files+selectItem(group,4,"jpg")));assertThat(unsupported.descriptors()).hasSize(4);assertThat(unsupported.descriptors().getLast().downloadAllowed()).isFalse();
        assertThat(p.selectDescriptors(s.source(),selectPage(group,"")).status()).isEqualTo("NO_FILES");assertThat(p.selectDescriptors(s.source(),"<main>첨부 없음</main>").complete()).isFalse();
        var limit=p.selectDescriptors(s.source(),selectPage(group,IntStream.rangeClosed(1,11).mapToObj(i->selectItem(group,i,"hwp")).collect(Collectors.joining())));assertThat(limit.status()).isEqualTo("LIMIT_EXCEEDED");assertThat(limit.descriptors()).hasSize(10);
        assertThat(p.selectLegacyBinaryContentTypes()).isEqualTo(group.equals("CHUNGBUK")?Set.of("application/x-msdownload"):Set.of());assertThat(p.selectUtf8DispositionOctets()).isFalse();
    }
    @ParameterizedTest @MethodSource("selectGroups") void bindsSourceMethodsPreviewDuplicatesAndBody(String group){
        var s=ChungcheongThirdDownloadCases.selectCase(group);var p=s.profile();String one=selectItem(group,1,"hwp");
        assertThat(p.selectDescriptors(s.source(),selectPage(group,one+one)).descriptors()).hasSize(1);
        var conflict=p.selectDescriptors(s.source(),selectPage(group,one+one.replace("지원 공고문.hwp","변경 공고문.hwp")));assertThat(conflict.complete()).isFalse();assertThat(conflict.descriptors()).hasSize(1);
        String mismatch=group.equals("CHUNGBUK")?one.replace("fileIndex=1","fileIndex=2"):one.replace("fn_egov_preview_File(","otherPreview(");
        var partial=p.selectDescriptors(s.source(),selectPage(group,mismatch));assertThat(partial.complete()).isFalse();assertThat(partial.descriptors()).hasSize(1);
        var request=p.selectDescriptors(s.source(),selectPage(group,one)).descriptors().getFirst().selectRequest();assertThat(p.selectApprovedRequest(request,request)).isTrue();assertThat(p.selectApprovedRequest(request,Request.selectGet(p.selectDetailUri(s.source())))).isFalse();
        if(group.equals("GONGJU")){assertThat(request.method()).isEqualTo("POST");assertThat(request.form().get("user_file_nm")).isEqualTo("지원 공고문.hwp");assertThat(p.selectApprovedRequest(request.uri())).isFalse();}
        else assertThat(p.selectApprovedRequest(new Request(request.uri(),"POST",Map.of("unused","value")))).isFalse();
        for(String bad:List.of("https:opaque",request.uri().toString().replace("https:","http:"),request.uri().toString().replace(Site.valueOf(group).fileHost,"evil.example"),request.uri()+"?unknown=1",request.uri()+"#x"))assertThat(p.selectApprovedRequest(URI.create(bad))).isFalse();
        var n=new AnnouncementSourceIdentityNormalizer();for(String bad:List.of(s.source().sourceUrl()+"&unknown=1",s.source().sourceUrl().replace("https:","http:"),s.source().sourceUrl()+"&"+Site.valueOf(group).idKey+"=0"))assertThatThrownBy(()->p.selectDetailUri(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(bad)),bad,s.source().localSourceCode(),s.source().listParserProfileCode()))).hasMessage("PROFILE_REQUIRED");
        var page=Jsoup.parse(selectPage(group,one));assertThat(ChungcheongThirdNoticePage.selectContent(Site.valueOf(group),page).text()).isEqualTo("소상공인 지원금");AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,"소상공인 지원",s.titleLayout());
        var budget=new AnnouncementAttachmentBbsOfficialObservationTest.Budget(p,false);assertThat(budget.maximumRequests).isEqualTo(6);assertThat(budget.maximumBytes).isEqualTo(23L*1024*1024);
    }
    @Test void gongjuNeverExecutesCallOrSubmitsUnapprovedForm(){
        var s=ChungcheongThirdDownloadCases.selectCase("GONGJU");String page=selectPage("GONGJU",selectItem("GONGJU",1,"pdf"));
        for(String bad:List.of(page.replace("name='file_path' value=''","name='file_path' value='/other'"),page.replace("method='post'","method='get'"),page.replace("id='fileForm'","id='otherForm'"))){var r=s.profile().selectDescriptors(s.source(),bad);assertThat(r.complete()).isFalse();assertThat(r.descriptors()).isEmpty();}
        assertThat(s.profile().selectDescriptors(s.source(),page.replace("javascript:fn_egov_downFile(","javascript:evil();fn_egov_downFile(")).descriptors()).isEmpty();
        assertThat(s.profile().selectDescriptors(s.source(),page.replace("/ntishome/file/upload/ofr/ofr/20260908","/etc/private")).descriptors()).isEmpty();
    }
    @EnabledIfEnvironmentVariable(named="SANEB_CHUNGCHEONG_THIRD_SURVEY_FIXTURE",matches="true")
    @ParameterizedTest @MethodSource("selectGroups") void actualOfficialBoundaries(String group)throws Exception{
        var s=ChungcheongThirdDownloadCases.selectCase(group);var html=Files.readString(Path.of("build/qa-chungcheong-third-20260929/"+group+"-detail.html"));var page=Jsoup.parse(html);AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,s.title(),s.titleLayout());var r=s.profile().selectDescriptors(s.source(),html);assertThat(r.complete()).as(group).isTrue();assertThat(r.descriptors()).hasSize(group.equals("GONGJU")?2:1);assertThat(ChungcheongThirdNoticePage.selectContent(Site.valueOf(group),page).text()).contains("소상공인");
    }
    @Test void catalogStaysReferenceOnly()throws Exception{
        var json=new com.fasterxml.jackson.databind.ObjectMapper();var notices=json.readTree(Files.readString(Path.of("src/main/resources/announcement-attachment/provider-qa-catalog-v2.json"))).path("notices");
        for(String group:selectGroups().toList()){var s=ChungcheongThirdDownloadCases.selectCase(group);var ref=StreamSupport.stream(notices.spliterator(),false).filter(n->s.code().equals(n.path("caseCode").asText())).findFirst().orElseThrow();assertThat(ref.path("source")).isEqualTo(json.valueToTree(s.source()));assertThat(ref.hasNonNull("expectation")).isFalse();assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectCases(group).count()).isEqualTo(1);}
    }
}
