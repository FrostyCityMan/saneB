package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.provider.content.CapitalSeventhNoticePage;
import com.saneb.domain.announcementsource.provider.content.CapitalSeventhNoticePage.Site;
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

class CapitalSeventhDownloadContractTest {
    static Stream<String> selectGroups() { return CapitalSeventhDownloadCases.GROUPS.stream().sorted(); }
    private String selectItem(String group,int id,String ext) {
        var site=Site.valueOf(group);String name="지원 공고문."+ext;
        String uri="https://"+site.fileHost+site.download+"?user_file_nm="+name+"&sys_file_nm=saved"+id+"."+ext+"&file_path=/ntishome/file/upload/ofr/ofr/20260929";
        if(site==Site.POCHEON) return "<div class='attach_item'><span class='text'><em>"+name+"</em></span><a class='attach_btn down' href='"+uri+"'><span>다운로드</span></a></div>";
        return "<li><div class='down_view'><span><img src='/file-icon'>"+name+"</span><a class='file_down' href='"+uri+"'>"+ext+" 파일 다운로드</a><a class='file_view' onclick=\"fn_goPreView('"+uri.replace("https:","http:")+"', '"+name+"');\">"+ext+" 파일 미리보기<i></i></a></div></li>";
    }
    private String selectPage(String group,String files) {
        if(group.equals("POCHEON")) return "<nav>수출 메뉴</nav><div class='p-wrap bbs bbs__view uiux_type'><div class='bbs_viewbox'><div class='subjectbox'><span class='subject'>소상공인 지원</span><div class='fieldlistbox'>담당자</div></div><div class='viewcontentbox'><div class='viewcontent'><div class='contenttext'>소상공인 지원금</div></div><div class='viewcontent'><div class='attachedfile'><span class='attach_tit'>첨부파일</span><div class='attach_list'>"+files+"</div></div></div></div></div></div><footer>특허 푸터</footer>";
        return "<nav>수출 메뉴</nav><table class='bbs_default view'><tr><th>제목</th><td>소상공인 지원</td></tr><tr><th>담당부서</th><td>메타데이터</td></tr><tr><th>내용</th><td title='내용' class='bbs_content'>소상공인 지원금</td></tr><tr><th>파일</th><td><ul class='view_attach'>"+files+"</ul></td></tr></table><footer>특허 푸터</footer>";
    }
    @ParameterizedTest @MethodSource("selectGroups") void keepsGoodFilesAndSeparatesUnknownOrUnsupportedFiles(String group) {
        var s=CapitalSeventhDownloadCases.selectCase(group);String files=selectItem(group,1,"hwp")+selectItem(group,2,"pdf")+selectItem(group,3,"hwpx");
        var r=s.profile().selectDescriptors(s.source(),selectPage(group,files));assertThat(r.complete()).isTrue();assertThat(r.descriptors()).hasSize(3);
        r.descriptors().forEach(d->{assertThat(d.downloadAllowed()).isTrue();assertThat(d.documentRole()).isEqualTo("UNKNOWN");assertThat(s.profile().selectApprovedRequest(d.selectRequest())).isTrue();});
        for(String extra:List.of("<li><a href='/unknown'>미확인</a></li>","<button>다른 파일</button>","<img src='/extra'>")) {
            var partial=s.profile().selectDescriptors(s.source(),selectPage(group,files+extra));assertThat(partial.complete()).isFalse();assertThat(partial.descriptors()).hasSize(3);
        }
        var unsupported=s.profile().selectDescriptors(s.source(),selectPage(group,files+selectItem(group,4,"jpg")));assertThat(unsupported.descriptors()).hasSize(4);assertThat(unsupported.descriptors().getLast().downloadAllowed()).isFalse();
        assertThat(s.profile().selectDescriptors(s.source(),selectPage(group,"")).status()).isEqualTo("NO_FILES");
        assertThat(s.profile().selectDescriptors(s.source(),"<main>첨부 없음</main>").complete()).isFalse();
    }
    @ParameterizedTest @MethodSource("selectGroups") void preservesIdentityMethodLimitsAndTextBoundaries(String group) {
        var s=CapitalSeventhDownloadCases.selectCase(group);String one=selectItem(group,1,"hwp");var p=s.profile();
        assertThat(p.selectDescriptors(s.source(),selectPage(group,one+one)).descriptors()).hasSize(1);
        var conflict=p.selectDescriptors(s.source(),selectPage(group,one+one.replace("지원 공고문.hwp","변경 공고문.hwp")));assertThat(conflict.complete()).isFalse();assertThat(conflict.descriptors()).hasSize(1);
        var limit=p.selectDescriptors(s.source(),selectPage(group,IntStream.rangeClosed(1,11).mapToObj(i->selectItem(group,i,"hwp")).collect(Collectors.joining())));assertThat(limit.status()).isEqualTo("LIMIT_EXCEEDED");assertThat(limit.descriptors()).hasSize(10);
        var request=p.selectDescriptors(s.source(),selectPage(group,one)).descriptors().getFirst().selectRequest();assertThat(request.method()).isEqualTo("GET");assertThat(p.selectApprovedRequest(request,request)).isTrue();
        assertThat(p.selectApprovedRequest(request,Request.selectGet(p.selectDetailUri(s.source())))).isFalse();
        assertThat(p.selectApprovedRequest(new Request(request.uri(),"POST",Map.of("unused","value")))).isFalse();
        for(String bad:List.of("https:opaque",request.uri().toString().replace("https:","http:"),request.uri().toString().replace(Site.valueOf(group).fileHost,"evil.example"),request.uri()+"&unknown=1",request.uri()+"#x")) assertThat(p.selectApprovedRequest(URI.create(bad))).isFalse();
        var n=new AnnouncementSourceIdentityNormalizer();for(String bad:List.of(s.source().sourceUrl()+"&unknown=1",s.source().sourceUrl()+"&key=0",s.source().sourceUrl().replace("https:","http:"))) assertThatThrownBy(()->p.selectDetailUri(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(bad)),bad,s.source().localSourceCode(),s.source().listParserProfileCode()))).hasMessage("PROFILE_REQUIRED");
        assertThatThrownBy(()->p.selectDetailUri(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE","mismatched",s.source().sourceUrl(),s.source().localSourceCode(),s.source().listParserProfileCode()))).hasMessage("PROFILE_REQUIRED");
        var page=Jsoup.parse(selectPage(group,one));assertThat(CapitalSeventhNoticePage.selectContent(Site.valueOf(group),page).text()).isEqualTo("소상공인 지원금");AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,"소상공인 지원",s.titleLayout());
        var budget=new AnnouncementAttachmentBbsOfficialObservationTest.Budget(p,false);assertThat(budget.maximumRequests).isEqualTo(6);assertThat(budget.maximumBytes).isEqualTo(23L*1024*1024);
    }
    @Test void gangneungPreviewIsNeverARequestAndMismatchDoesNotLoseDownload() {
        var s=CapitalSeventhDownloadCases.selectCase("GANGNEUNG");String item=selectItem("GANGNEUNG",1,"hwpx");
        for(String bad:List.of(item.replace("fn_goPreView(","otherPreview("),item.replace("http://eminwon.gangneung.go.kr","http://evil.example"),item.replace("fn_goPreView('","fn_goPreView('../"),item.replace("');\"", "');other();\""))) {
            var partial=s.profile().selectDescriptors(s.source(),selectPage("GANGNEUNG",bad));assertThat(partial.complete()).isFalse();assertThat(partial.descriptors()).hasSize(1);
            assertThat(partial.descriptors().getFirst().fetchUri().getScheme()).isEqualTo("https");
        }
        var invalid=s.profile().selectDescriptors(s.source(),selectPage("GANGNEUNG",item.replace("href='https:","href='http:")));assertThat(invalid.descriptors()).isEmpty();assertThat(invalid.complete()).isFalse();
    }
    @EnabledIfEnvironmentVariable(named="SANEB_CAPITAL_SEVENTH_SURVEY_FIXTURE",matches="true")
    @ParameterizedTest @MethodSource("selectGroups") void actualOfficialBoundaries(String group)throws Exception {
        var s=CapitalSeventhDownloadCases.selectCase(group);String html=Files.readString(Path.of("build/qa-capital-seventh-20260929/"+group+"-detail.html"));
        AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(Jsoup.parse(html),s.title(),s.titleLayout());var result=s.profile().selectDescriptors(s.source(),html);assertThat(result.complete()).as(group).isTrue();assertThat(result.descriptors()).hasSize(1);
        assertThat(CapitalSeventhNoticePage.selectContent(Site.valueOf(group),Jsoup.parse(html)).text()).contains("소상공인");
    }
    @Test void catalogStaysReferenceOnly()throws Exception {
        var json=new com.fasterxml.jackson.databind.ObjectMapper();var notices=json.readTree(Files.readString(Path.of("src/main/resources/announcement-attachment/provider-qa-catalog-v2.json"))).path("notices");
        for(String group:selectGroups().toList()) { var s=CapitalSeventhDownloadCases.selectCase(group);var ref=StreamSupport.stream(notices.spliterator(),false).filter(n->s.code().equals(n.path("caseCode").asText())).findFirst().orElseThrow();assertThat(ref.path("source")).isEqualTo(json.valueToTree(s.source()));assertThat(ref.hasNonNull("expectation")).isFalse();assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectCases(group).count()).isEqualTo(1); }
    }
}
