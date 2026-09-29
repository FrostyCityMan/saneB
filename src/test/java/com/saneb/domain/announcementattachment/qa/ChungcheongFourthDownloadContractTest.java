package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.provider.content.ChungcheongFourthNoticePage;
import com.saneb.domain.announcementsource.provider.content.ChungcheongFourthNoticePage.Site;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ChungcheongFourthDownloadContractTest {
    static Stream<String> selectGroups(){return ChungcheongFourthDownloadCases.GROUPS.stream().sorted();}
    private String selectItem(String group,int id,String ext){
        String name="지원 공고문 (1)."+ext,stored="stored"+id+"."+ext,path="/ntishome/file/upload/ofr/ofr/20260908",args="('"+name+"','"+stored+"', '"+path+"');";
        if(group.equals("YESAN"))return "<div class='bbs-file__download'><a href='#' onclick=\"fn_egov_downFile"+args+" return false;\"><i></i>"+name+"</a></div>";
        return "<a href=\"javascript:fn_egov_downFile"+args+"\"><i></i>"+name+"</a><a href='/synapsoft/SaeolFileViewer.do?user_file_nm="+name+"&sys_file_nm="+stored+"&file_path="+path+"'>미리보기<i></i></a>";
    }
    private String selectPage(String group,String files){
        String content=group.equals("HONGSEONG")?"<div class='program--contents'><div class='ui bbs--view'><div class='ui bbs--view--header'><h2 class='ui bbs--view--tit'>소상공인 지원</h2><span>담당자</span></div><div class='ui bbs--view--file'>"+files+"</div><div class='ui bbs--view--cont'><div class='ui bbs--detail--cont'><div class='ui bbs--view--content'>소상공인 지원금</div></div></div></div></div>"
                :"<div class='card program--view'><div class='card-body prog bucket-form'><div class='form-group'><div class='control-label'><label for='notAncmtSj'>제목</label></div><div><span id='notAncmtSj'>소상공인 지원</span></div></div><div class='form-group'><div class='control-label'><label for='notAncmtCn'>내용</label></div><div><span id='notAncmtCn'>소상공인 지원금</span></div></div><div class='form-group'><div class='control-label'><label>파일</label></div><div class='col-sm-9'><div class='ui bbs--view--file'>"+files+"</div></div></div></div></div>";
        return "<nav>수출 메뉴</nav>"+content+"<form id='fileForm' name='fileForm' method='post' action='"+(group.equals("YESAN")?"":"https://eminwon.hongseong.go.kr/emwp/jsp/ofr/FileDown.jsp")+"'><input type='hidden' name='user_file_nm' value=''><input type='hidden' name='sys_file_nm' value=''><input type='hidden' name='file_path' value=''></form><footer>푸터</footer>";
    }
    @ParameterizedTest @MethodSource("selectGroups") void keepsGoodFilesSeparatesErrorsAndUnsupported(String group){
        var s=ChungcheongFourthDownloadCases.selectCase(group);var p=s.profile();String files=selectItem(group,1,"hwp")+selectItem(group,2,"pdf")+selectItem(group,3,"hwpx");
        var r=p.selectDescriptors(s.source(),selectPage(group,files));assertThat(r.complete()).isTrue();assertThat(r.descriptors()).hasSize(3);r.descriptors().forEach(d->{assertThat(d.documentRole()).isEqualTo("UNKNOWN");assertThat(d.downloadAllowed()).isTrue();assertThat(p.selectApprovedRequest(d.selectRequest())).isTrue();assertThat(d.selectRequest().method()).isEqualTo("POST");assertThat(p.selectApprovedRequest(d.fetchUri())).isFalse();});
        for(String extra:List.of("<a href='/unknown'>다른 첨부</a>","<button>파일</button>","<img src='/unknown'>")){var partial=p.selectDescriptors(s.source(),selectPage(group,files+extra));assertThat(partial.complete()).isFalse();assertThat(partial.descriptors()).hasSize(3);}
        var unsupported=p.selectDescriptors(s.source(),selectPage(group,files+selectItem(group,4,"jpg")));assertThat(unsupported.descriptors()).hasSize(4);assertThat(unsupported.descriptors().getLast().downloadAllowed()).isFalse();
        assertThat(p.selectDescriptors(s.source(),selectPage(group,"")).status()).isEqualTo("NO_FILES");assertThat(p.selectDescriptors(s.source(),"<main>첨부 없음</main>").complete()).isFalse();
        var limit=p.selectDescriptors(s.source(),selectPage(group,IntStream.rangeClosed(1,11).mapToObj(i->selectItem(group,i,"hwp")).collect(Collectors.joining())));assertThat(limit.status()).isEqualTo("LIMIT_EXCEEDED");assertThat(limit.descriptors()).hasSize(10);
    }
    @ParameterizedTest @MethodSource("selectGroups") void bindsIdentityEncodingAndBudget(String group){
        var s=ChungcheongFourthDownloadCases.selectCase(group);var p=s.profile();String one=selectItem(group,1,"hwp"),page=selectPage(group,one);
        assertThat(p.selectDescriptors(s.source(),selectPage(group,one+one)).descriptors()).hasSize(1);
        var conflict=p.selectDescriptors(s.source(),selectPage(group,one+one.replace("지원 공고문 (1).hwp","다른 공고문.hwp")));assertThat(conflict.complete()).isFalse();assertThat(conflict.descriptors()).hasSize(1);
        var request=p.selectDescriptors(s.source(),page).descriptors().getFirst().selectRequest();String encoded=request.form().get("user_file_nm");assertThat(encoded).contains("%25").contains("(1)");assertThat(URLDecoder.decode(URLDecoder.decode(encoded,StandardCharsets.UTF_8),StandardCharsets.UTF_8)).isEqualTo("지원 공고문 (1).hwp");
        assertThat(p.selectApprovedRequest(request,request)).isTrue();assertThat(p.selectApprovedRequest(request,Request.selectGet(p.selectDetailUri(s.source())))).isFalse();
        for(String bad:List.of("지원 공고문 (1).hwp","..%252Fsecret.hwp","%250Afile.hwp","%25ZZ")){var form=new HashMap<>(request.form());form.put("user_file_nm",bad);assertThat(p.selectApprovedRequest(new Request(request.uri(),"POST",form))).isFalse();}
        for(String bad:List.of("https:opaque",request.uri().toString().replace("https:","http:"),request.uri().toString().replace(Site.valueOf(group).fileHost,"evil.example"),request.uri()+"?unknown=1",request.uri()+"#x"))assertThat(p.selectApprovedRequest(new Request(URI.create(bad),"POST",request.form()))).isFalse();
        var n=new AnnouncementSourceIdentityNormalizer();for(String bad:List.of(s.source().sourceUrl()+"&unknown=1",s.source().sourceUrl().replace("https:","http:"),s.source().sourceUrl()+"&notAncmtMgtNo=0"))assertThatThrownBy(()->p.selectDetailUri(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(bad)),bad,s.source().localSourceCode(),s.source().listParserProfileCode()))).hasMessage("PROFILE_REQUIRED");
        assertThat(ChungcheongFourthNoticePage.selectContent(Site.valueOf(group),Jsoup.parse(page)).text()).isEqualTo("소상공인 지원금");AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(Jsoup.parse(page),"소상공인 지원",s.titleLayout());
        var budget=new AnnouncementAttachmentBbsOfficialObservationTest.Budget(p,false);assertThat(budget.maximumRequests).isEqualTo(6);assertThat(budget.maximumBytes).isEqualTo((group.equals("YESAN")?26L:23L)*1024*1024);budget.reserveBody();assertThat(budget.bytes).isEqualTo((group.equals("YESAN")?4L:2L)*1024*1024);
        assertThat(p.selectDescriptors(s.source()," ".repeat(1_050_000)+page).complete()).isEqualTo(group.equals("YESAN"));assertThat(p.selectDescriptors(s.source()," ".repeat(2_097_153)).complete()).isFalse();
    }
    @Test void rejectsChangedFormAndRetainsGoodFileWithBadPreview(){
        var s=ChungcheongFourthDownloadCases.selectCase("HONGSEONG");String page=selectPage("HONGSEONG",selectItem("HONGSEONG",1,"pdf"));
        for(String bad:List.of(page.replace("name='file_path' value=''","name='file_path' value='/other'"),page.replace("method='post'","method='get'"),page.replace("id='fileForm'","id='otherForm'"))){var r=s.profile().selectDescriptors(s.source(),bad);assertThat(r.complete()).isFalse();assertThat(r.descriptors()).isEmpty();}
        var r=s.profile().selectDescriptors(s.source(),page.replace("/synapsoft/SaeolFileViewer.do","https://evil.example/view"));assertThat(r.complete()).isFalse();assertThat(r.descriptors()).hasSize(1);
        assertThat(s.profile().selectDescriptors(s.source(),page.replace("javascript:fn_egov_downFile(","javascript:evil();fn_egov_downFile(")).descriptors()).isEmpty();
    }
    @Test void detailLimitCannotExceedFixedBounds(){var profile=org.mockito.Mockito.mock(AttachmentDiscoveryProfile.class,org.mockito.Mockito.withSettings().extraInterfaces(AttachmentDetailLimitProfile.class));for(long value:List.of(0L,-1L,3L*1024*1024,Long.MAX_VALUE)){org.mockito.Mockito.when(((AttachmentDetailLimitProfile)profile).selectDetailMaximumBytes()).thenReturn(value);assertThatThrownBy(()->com.saneb.domain.announcementattachment.discovery.AttachmentDetailLimitProfile.selectBoundedDetailMaximumBytes(profile)).hasMessage("PROFILE_REQUIRED");}assertThat(com.saneb.domain.announcementattachment.discovery.AttachmentDetailLimitProfile.selectBoundedDetailMaximumBytes(new BizInfoAttachmentDiscoveryProfile())).isEqualTo(1024L*1024);}
    @EnabledIfEnvironmentVariable(named="SANEB_CHUNGCHEONG_FOURTH_SURVEY_FIXTURE",matches="true")
    @ParameterizedTest @MethodSource("selectGroups") void actualOfficialBoundaries(String group)throws Exception{
        var s=ChungcheongFourthDownloadCases.selectCase(group);var html=Files.readString(Path.of("build/qa-chungcheong-fourth-20260929/"+group+"-detail.html"));var page=Jsoup.parse(html);AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,s.title(),s.titleLayout());var r=s.profile().selectDescriptors(s.source(),html);assertThat(r.complete()).as(group).isTrue();assertThat(r.descriptors()).hasSize(group.equals("YESAN")?2:1);assertThat(ChungcheongFourthNoticePage.selectContent(Site.valueOf(group),page).text()).contains("소상공인");
    }
    @Test void catalogStaysReferenceOnly()throws Exception{
        var json=new com.fasterxml.jackson.databind.ObjectMapper();var notices=json.readTree(Files.readString(Path.of("src/main/resources/announcement-attachment/provider-qa-catalog-v2.json"))).path("notices");for(String group:selectGroups().toList()){var s=ChungcheongFourthDownloadCases.selectCase(group);var ref=StreamSupport.stream(notices.spliterator(),false).filter(n->s.code().equals(n.path("caseCode").asText())).findFirst().orElseThrow();assertThat(ref.path("source")).isEqualTo(json.valueToTree(s.source()));assertThat(ref.hasNonNull("expectation")).isFalse();assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectCases(group).count()).isEqualTo(1);}
    }
}
