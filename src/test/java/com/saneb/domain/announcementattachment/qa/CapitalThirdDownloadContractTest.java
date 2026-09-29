package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.provider.content.CapitalThirdNoticePage;
import com.saneb.domain.announcementsource.provider.content.CapitalThirdNoticePage.Site;
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

class CapitalThirdDownloadContractTest {
    static Stream<String> selectGroups(){return CapitalThirdDownloadCases.GROUPS.stream().sorted();}
    private String selectItem(String group,int id,String ext){var s=Site.valueOf(group);String name="공고문."+ext,stored="saved"+id+"."+ext,path="/ntishome/file/upload/ofr/ofr/20260929";
        if(s==Site.GURI)return "<li class='p-attach__item'><a class='p-attach__link' href='https://"+s.fileHost+s.download+"?user_file_nm="+name+"&sys_file_nm="+stored+"&file_path="+path+"'><span class='p-icon'>파일</span><span>"+name+"</span><i class='p-icon'></i></a></li>";
        String args="('"+name+"','"+stored+"','"+path+"');";
        return "<li class='p-attach__item'><span class='p-attach__text'>"+name+"</span><a class='p-attach__down' href=\"javascript:"+(s==Site.HANAM?"gourl":"goDownLoad")+args+"\">다운로드</a>"
                +(s==Site.HANAM?"<button class='p-attach__preview' onclick=\"fn_goPreView"+args+"\">미리보기</button>":"<a class='p-attach__preview' href=\"javascript:goPreviewFile"+args+"\">미리보기</a>")+"</li>";
    }
    private String selectPage(String group,String files){var s=Site.valueOf(group);
        String form=s==Site.NAMYANGJU?"<form name='nnn' method='post' action='https://"+s.fileHost+s.download+"'><input type='hidden' name='user_file_nm'><input type='hidden' name='sys_file_nm'><input type='hidden' name='file_path'></form>":"";
        return "<main><nav>특허 메뉴</nav>"+form+"<div class='p-wrap bbs bbs__view'>"+(s==Site.NAMYANGJU?"<div class='card board_bottom'><div class='card_title'><div class='bbs_view_title'>소상공인 지원 공고</div></div></div>":"")
                +"<table class='p-table block'>"+(s==Site.HANAM?"<tr><td><span class='p-table__subject_text'>소상공인 지원 공고</span></td></tr>":s==Site.GURI?"<tr><th>제목</th><td>소상공인 지원 공고</td></tr>":"")
                +(s==Site.HANAM?"<tr><td class='p-table__content'><textarea title='내용' disabled>소상공인 지원금</textarea></td></tr>":"<tr><th>내용</th><td>소상공인 지원금</td></tr>")
                +"<tr><th>"+(s==Site.GURI?"파일":"첨부파일")+"</th><td><ul class='p-attach'>"+files+"</ul></td></tr></table></div><footer>수출 푸터</footer></main>";
    }
    @ParameterizedTest @MethodSource("selectGroups") void goodFilesSurviveUnknownLinksAndUnsupportedFormats(String group){
        var s=CapitalThirdDownloadCases.selectCase(group);String files=selectItem(group,1,"hwp")+selectItem(group,2,"hwpx")+selectItem(group,3,"pdf");
        var r=s.profile().selectDescriptors(s.source(),selectPage(group,files));assertThat(r.complete()).isTrue();assertThat(r.descriptors()).hasSize(3);
        r.descriptors().forEach(d->{assertThat(d.documentRole()).isEqualTo("UNKNOWN");assertThat(d.downloadAllowed()).isTrue();assertThat(s.profile().selectApprovedRequest(d.selectRequest())).isTrue();});
        for(String unknown:List.of("<a href='/unknown.pdf'>추가 파일</a>","<script>bad()</script>","<button>첨부</button>","<img src='/image'>")){var p=s.profile().selectDescriptors(s.source(),selectPage(group,files+unknown));assertThat(p.complete()).isFalse();assertThat(p.descriptors()).hasSize(3);}
        var mixed=s.profile().selectDescriptors(s.source(),selectPage(group,files+selectItem(group,4,"jpg")));assertThat(mixed.descriptors()).hasSize(4);assertThat(mixed.descriptors().getLast().downloadAllowed()).isFalse();
        assertThat(s.profile().selectDescriptors(s.source(),selectPage(group,"")).status()).isEqualTo("NO_FILES");assertThat(s.profile().selectDescriptors(s.source(),"<main/>").complete()).isFalse();
    }
    @ParameterizedTest @MethodSource("selectGroups") void validatesIdentityDuplicatesRedirectsBudgetAndBoundary(String group){
        var s=CapitalThirdDownloadCases.selectCase(group);var p=s.profile();String one=selectItem(group,1,"hwp");
        assertThat(p.selectDescriptors(s.source(),selectPage(group,one+one)).descriptors()).hasSize(1);
        var conflict=p.selectDescriptors(s.source(),selectPage(group,one+one.replace("공고문.hwp","변경.hwp")));assertThat(conflict.complete()).isFalse();assertThat(conflict.descriptors()).hasSize(1);
        var limit=p.selectDescriptors(s.source(),selectPage(group,IntStream.rangeClosed(1,11).mapToObj(i->selectItem(group,i,"pdf")).collect(Collectors.joining())));assertThat(limit.status()).isEqualTo("LIMIT_EXCEEDED");assertThat(limit.descriptors()).hasSize(10);
        var req=p.selectDescriptors(s.source(),selectPage(group,one)).descriptors().getFirst().selectRequest();assertThat(req.method()).isEqualTo(group.equals("NAMYANGJU")?"POST":"GET");assertThat(p.selectApprovedRequest(req,req)).isTrue();
        assertThat(p.selectApprovedRequest(req,Request.selectGet(p.selectDetailUri(s.source())))).isFalse();assertThat(p.selectApprovedRequest(new Request(URI.create(req.uri().toString().replace("https:","http:")),req.method(),req.form()))).isFalse();
        var n=new AnnouncementSourceIdentityNormalizer();for(String bad:List.of(s.source().sourceUrl()+"&key=9",s.source().sourceUrl()+"&unknown=x",s.source().sourceUrl().replace("https:","http:")))assertThatThrownBy(()->p.selectDetailUri(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(bad)),bad,s.source().localSourceCode(),"SAEOL_GOSI"))).hasMessage("PROFILE_REQUIRED");
        var page=Jsoup.parse(selectPage(group,one));assertThat(CapitalThirdNoticePage.selectContent(Site.valueOf(group),page).text()).isEqualTo("소상공인 지원금");
        AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,"소상공인 지원 공고",s.titleLayout());assertThatThrownBy(()->AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,"다른 제목",s.titleLayout())).isInstanceOf(AssertionError.class);
        var budget=new AnnouncementAttachmentBbsOfficialObservationTest.Budget(p,false);assertThat(budget.maximumRequests).isEqualTo(6);assertThat(budget.maximumBytes).isEqualTo(23L*1024*1024);
    }
    @Test void previewConflictsAndInvalidFormNeverBecomeDownloads(){for(String g:List.of("HANAM","NAMYANGJU")){var s=CapitalThirdDownloadCases.selectCase(g);String page=selectPage(g,selectItem(g,1,"hwp"));String func=g.equals("HANAM")?"fn_goPreView":"goPreviewFile";
        var r=s.profile().selectDescriptors(s.source(),page.replace(func+"('공고문.hwp'",func+"('다른.hwp'"));assertThat(r.complete()).isFalse();assertThat(r.descriptors()).hasSize(1);
        if(g.equals("NAMYANGJU"))assertThat(s.profile().selectDescriptors(s.source(),page.replace("method='post'","method='get'")).warnings()).contains("ATTACHMENT_DOWNLOAD_FORM_CHANGED");}}
    @EnabledIfEnvironmentVariable(named="SANEB_CAPITAL_THIRD_SURVEY_FIXTURE",matches="true")
    @ParameterizedTest @MethodSource("selectGroups") void actualOfficialBoundaries(String group)throws Exception{var s=CapitalThirdDownloadCases.selectCase(group);String html=Files.readString(Path.of("build/qa-capital-third-20260929/"+group+"-detail.html"));
        AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(Jsoup.parse(html),s.title(),s.titleLayout());var r=s.profile().selectDescriptors(s.source(),html);assertThat(r.complete()).isTrue();assertThat(r.descriptors()).hasSize(group.equals("HANAM")?2:1);}
    @Test void referencesHaveMatchingIdentityButNoPolicyApproval()throws Exception{var json=new com.fasterxml.jackson.databind.ObjectMapper();var notices=json.readTree(Files.readString(Path.of("src/main/resources/announcement-attachment/provider-qa-catalog-v2.json"))).path("notices");
        for(String g:selectGroups().toList()){var s=CapitalThirdDownloadCases.selectCase(g);var ref=StreamSupport.stream(notices.spliterator(),false).filter(x->x.path("caseCode").asText().equals(s.code())).findFirst().orElseThrow();assertThat(ref.path("source")).isEqualTo(json.valueToTree(s.source()));assertThat(ref.hasNonNull("expectation")).isFalse();assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectCases(g).count()).isEqualTo(1);}}
}
