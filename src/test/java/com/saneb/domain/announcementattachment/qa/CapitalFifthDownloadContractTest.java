package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.provider.content.CapitalFifthNoticePage;
import com.saneb.domain.announcementsource.provider.content.CapitalFifthNoticePage.Site;
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

class CapitalFifthDownloadContractTest {
    static Stream<String> selectGroups() { return CapitalFifthDownloadCases.GROUPS.stream().sorted(); }
    private String selectItem(String group,int id,String ext) {
        var site=Site.valueOf(group); var sample=CapitalFifthDownloadCases.selectCase(group);
        String notice=sample.code().substring(sample.code().lastIndexOf('-')+1);
        String name="지원 공고문."+ext,stored="saved"+id+"."+ext,path="/ntishome/file/upload/ofr/ofr/20260929";
        String args="'"+name+"', '"+stored+"', '"+path+"'";
        String link=site==Site.GG_GWANGJU?"<a href='#' onclick=\"goDownload("+args+"); return false;\">"+name+"</a>":"<a href=\"javascript:goDownload("+args+")\">"+name+"</a>";
        String preview=site==Site.GG_GWANGJU?"previewAjax("+args+", 'N')":"fn_egov_gosi_preview('"+notice+"','"+notice+"-"+(10-id)+"."+ext+"',"+args+(site==Site.UIJEONGBU?",'N'":"")+")";
        String tag=site==Site.ANSEONG?"p":"li";
        return "<"+tag+">"+link+"<a href='#' onclick=\""+preview+"; return false;\">"+(site==Site.GG_GWANGJU?"바로보기":"바로 보기")+"</a></"+tag+">";
    }
    private String selectPage(String group,String files) {
        var site=Site.valueOf(group); boolean post=site==Site.UIJEONGBU;
        String fileArea=site==Site.ANSEONG?files:post?"<ul>"+files+"</ul><iframe id='hidden_frame' name='hidden_frame'></iframe>":"<div><ul id='updateFileList'>"+files+"</ul></div>";
        String form=post?"<form id='gosiFiledownFrm' name='gosiFiledownFrm' method='post' action='https://"+site.fileHost+site.download+"'><input type='hidden' name='user_file_nm'><input type='hidden' name='sys_file_nm'><input type='hidden' name='file_path'></form>":"";
        return "<main><nav>수출 메뉴</nav><form id='detailForm' name='detailForm' method='post'>"+(post?"":"<div class='bod_wrap'>")+"<div class='bod_view'><h4>소상공인 지원</h4><div class='view_info'>담당부서</div><div class='view_cont'>소상공인 지원금</div><dl class='view_file'><dt>첨부 파일</dt><dd>"+fileArea+"</dd></dl></div>"+(post?"":"</div>")+"</form>"+form+"<footer>특허 푸터</footer></main>";
    }

    @ParameterizedTest @MethodSource("selectGroups") void keepsGoodFilesWhenOtherLinksFail(String group) {
        var sample=CapitalFifthDownloadCases.selectCase(group); var profile=sample.profile();
        String files=selectItem(group,1,"hwp")+selectItem(group,2,"pdf")+selectItem(group,3,"hwpx");
        var result=profile.selectDescriptors(sample.source(),selectPage(group,files));
        assertThat(result.complete()).as(group).isTrue(); assertThat(result.descriptors()).hasSize(3);
        result.descriptors().forEach(file->{ assertThat(file.downloadAllowed()).isTrue(); assertThat(file.documentRole()).isEqualTo("UNKNOWN"); assertThat(profile.selectApprovedRequest(file.selectRequest())).isTrue(); });
        for(String unknown:List.of("<a href='/unknown.pdf'>미확인 파일</a>","<button>추가 첨부</button>","<img src='/more'>")) {
            var partial=profile.selectDescriptors(sample.source(),selectPage(group,files+unknown));
            assertThat(partial.complete()).isFalse(); assertThat(partial.descriptors()).hasSize(3);
        }
        var unsupported=profile.selectDescriptors(sample.source(),selectPage(group,files+selectItem(group,4,"jpg")));
        assertThat(unsupported.descriptors()).hasSize(4); assertThat(unsupported.descriptors().getLast().downloadAllowed()).isFalse();
        assertThat(profile.selectDescriptors(sample.source(),selectPage(group,"")).status()).isEqualTo("NO_FILES");
        assertThat(profile.selectDescriptors(sample.source(),"<main>등록된 게시물이 없습니다.</main>").complete()).isFalse();
    }

    @ParameterizedTest @MethodSource("selectGroups") void checksIdentityRequestMethodDedupAndBudget(String group) {
        var sample=CapitalFifthDownloadCases.selectCase(group); var profile=sample.profile(); String one=selectItem(group,1,"hwp");
        assertThat(profile.selectDescriptors(sample.source(),selectPage(group,one+one)).descriptors()).hasSize(1);
        var conflict=profile.selectDescriptors(sample.source(),selectPage(group,one+one.replace("지원 공고문.hwp","다른 공고문.hwp")));
        assertThat(conflict.complete()).isFalse(); assertThat(conflict.descriptors()).hasSize(1);
        var limited=profile.selectDescriptors(sample.source(),selectPage(group,IntStream.rangeClosed(1,11).mapToObj(i->selectItem(group,i,"hwp")).collect(Collectors.joining())));
        assertThat(limited.status()).isEqualTo("LIMIT_EXCEEDED"); assertThat(limited.descriptors()).hasSize(10);
        var request=profile.selectDescriptors(sample.source(),selectPage(group,one)).descriptors().getFirst().selectRequest();
        assertThat(request.method()).isEqualTo(group.equals("UIJEONGBU")?"POST":"GET");
        if(group.equals("UIJEONGBU")) { assertThat(request.form().get("user_file_nm")).isEqualTo("지원공고문.hwp"); assertThat(profile.selectApprovedRequest(Request.selectGet(request.uri()))).isFalse(); }
        assertThat(profile.selectApprovedRequest(request,request)).isTrue();
        assertThat(profile.selectApprovedRequest(request,Request.selectGet(profile.selectDetailUri(sample.source())))).isFalse();
        for(String bad:List.of("https:opaque",request.uri().toString().replace("https:","http:"),request.uri()+"#x")) assertThat(profile.selectApprovedRequest(URI.create(bad))).isFalse();
        var n=new AnnouncementSourceIdentityNormalizer();
        for(String bad:List.of(sample.source().sourceUrl()+"&unknown=1",sample.source().sourceUrl()+"&mId=0")) assertThatThrownBy(()->profile.selectDetailUri(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(bad)),bad,sample.source().localSourceCode(),sample.source().listParserProfileCode()))).hasMessage("PROFILE_REQUIRED");
        var budget=new AnnouncementAttachmentBbsOfficialObservationTest.Budget(profile,false); assertThat(budget.maximumRequests).isEqualTo(6); assertThat(budget.maximumBytes).isEqualTo(23L*1024*1024);
        var page=Jsoup.parse(selectPage(group,one)); assertThat(CapitalFifthNoticePage.selectContent(Site.valueOf(group),page).text()).isEqualTo("소상공인 지원금");
        AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,"소상공인 지원",sample.titleLayout());
    }

    @ParameterizedTest @MethodSource("selectGroups") void rejectsExecutableDownloadAndSeparatesPreviewConflict(String group) {
        var sample=CapitalFifthDownloadCases.selectCase(group); String item=selectItem(group,1,"hwp");
        var injected=sample.profile().selectDescriptors(sample.source(),selectPage(group,item.replace("goDownload(","goDownload(alert(1),")));
        assertThat(injected.complete()).isFalse(); assertThat(injected.descriptors()).isEmpty();
        String preview=group.equals("GG_GWANGJU")?"previewAjax(":"fn_egov_gosi_preview(";
        var conflict=sample.profile().selectDescriptors(sample.source(),selectPage(group,item.replace(preview,"unknownPreview(")));
        assertThat(conflict.complete()).isFalse(); assertThat(conflict.descriptors()).hasSize(1);
    }

    @Test void postFormMustRemainTheOfficialPlainThreeFieldForm() {
        var sample=CapitalFifthDownloadCases.selectCase("UIJEONGBU"); String page=selectPage("UIJEONGBU",selectItem("UIJEONGBU",1,"pdf"));
        for(String changed:List.of(page.replace("eminwon.ui4u.go.kr","other.invalid"),page.replace("name='file_path'","name='other'"),page.replace("name='user_file_nm'","value='unexpected' name='user_file_nm'"))) {
            var result=sample.profile().selectDescriptors(sample.source(),changed); assertThat(result.warnings()).containsExactly("ATTACHMENT_DOWNLOAD_FORM_CHANGED"); assertThat(result.descriptors()).isEmpty();
        }
        var partial=sample.profile().selectDescriptors(sample.source(),page.replace("id='hidden_frame'","src='https://other.invalid' id='hidden_frame'"));
        assertThat(partial.complete()).isFalse(); assertThat(partial.descriptors()).hasSize(1);
    }

    @EnabledIfEnvironmentVariable(named="SANEB_CAPITAL_FIFTH_SURVEY_FIXTURE",matches="true")
    @ParameterizedTest @MethodSource("selectGroups") void actualOfficialBoundaries(String group)throws Exception {
        var sample=CapitalFifthDownloadCases.selectCase(group);
        String html=Files.readString(Path.of("build/qa-capital-fifth-20260929/"+group+"-detail"+(group.equals("ANSEONG")?"-retry":"")+".html"));
        AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(Jsoup.parse(html),sample.title(),sample.titleLayout());
        var result=sample.profile().selectDescriptors(sample.source(),html);
        assertThat(result.complete()).as(group).isTrue(); assertThat(result.descriptors()).hasSize(group.equals("ANSEONG")?3:1);
    }

    @Test void catalogRemainsReferenceOnly()throws Exception {
        var json=new com.fasterxml.jackson.databind.ObjectMapper(); var notices=json.readTree(Files.readString(Path.of("src/main/resources/announcement-attachment/provider-qa-catalog-v2.json"))).path("notices");
        for(String group:selectGroups().toList()) {
            var sample=CapitalFifthDownloadCases.selectCase(group);
            var ref=StreamSupport.stream(notices.spliterator(),false).filter(n->sample.code().equals(n.path("caseCode").asText())).findFirst().orElseThrow();
            assertThat(ref.path("source")).isEqualTo(json.valueToTree(sample.source())); assertThat(ref.hasNonNull("expectation")).isFalse();
            assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectCases(group).count()).isEqualTo(1);
        }
    }
}
