package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import com.saneb.domain.announcementattachment.discovery.AttachmentDiscoveryProfile;
import com.saneb.domain.announcementsource.provider.content.JeonbukThirdNoticePage;
import com.saneb.domain.announcementsource.provider.content.JeonbukThirdNoticePage.Site;
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

class JeonbukThirdDownloadContractTest {
    static Stream<String> selectGroups(){return JeonbukThirdDownloadCases.GROUPS.stream().sorted();}
    private String selectItem(String group,int id,String ext){
        var s=Site.valueOf(group);String fileId=s==Site.JEONJU?String.format("%032x",id):Integer.toString(id),name="지원 공고문 (1)."+ext;
        String query=s.boardKey+"="+s.board+"&"+s.fileKey+"="+fileId+(s==Site.JEONJU?"":"&menuCd="+s.menu+"&dataSid=670379&command=update&paging=ok&startPage=1");
        return s==Site.JEONJU?"<div><a href='"+s.download+"?"+query+"'>"+name+"(92KB)</a><a href='/synap/convert.jsp?fileUid="+fileId+"'><span>미리보기</span></a></div>":name+" [311 kb]<a class='ico_file' title='"+name+"' href='"+s.download+"?"+query+"'>다운로드</a><a class='ico_viewer' href='/board/SynapViewer.jeonbuk?"+query+"'>바로보기</a><br>";
    }
    private String selectPage(String group,String files){return "<nav>수출 메뉴</nav>"+(group.equals("JEONJU")?"<div id='board_wrap'><div class='view-group'><div class='view-table'><ul><li><strong>제목</strong><span>소상공인 지원</span></li><li><strong>담당자</strong><span>담당자</span></li><li><strong>첨부파일</strong><div>"+files+"</div></li></ul></div><div class='view-list'><div class='view-con'>소상공인 지원금</div></div></div></div>":"<div class='bbs_skin'><div class='bbs_view'><div class='bbs_vtop'><h4>소상공인 지원</h4><ul><li>담당자</li></ul></div><div class='bbs_con'>소상공인 지원금</div><p class='bbs_filedown'>"+files+"</p></div></div>")+"<footer>푸터</footer>";}
    @ParameterizedTest @MethodSource("selectGroups") void preservesGoodFilesAndSeparatesFailures(String group){
        var s=JeonbukThirdDownloadCases.selectCase(group);var p=s.profile();String files=selectItem(group,1,"hwp")+selectItem(group,2,"pdf")+selectItem(group,3,"hwpx");
        var r=p.selectDescriptors(s.source(),selectPage(group,files));assertThat(r.complete()).isTrue();assertThat(r.descriptors()).hasSize(3);r.descriptors().forEach(d->{assertThat(d.downloadAllowed()).isTrue();assertThat(d.documentRole()).isEqualTo("UNKNOWN");assertThat(d.selectRequest().method()).isEqualTo("GET");assertThat(p.selectApprovedRequest(d.selectRequest())).isTrue();});
        for(String extra:List.of("<a href='/unknown'>다른 파일</a>","<button>첨부</button>","<img src='/unknown'>","<script>unknown()</script>")){var partial=p.selectDescriptors(s.source(),selectPage(group,files+extra));assertThat(partial.complete()).isFalse();assertThat(partial.descriptors()).hasSize(3);}
        var unsupported=p.selectDescriptors(s.source(),selectPage(group,files+selectItem(group,4,"jpg")));assertThat(unsupported.descriptors()).hasSize(4);assertThat(unsupported.descriptors().getLast().downloadAllowed()).isFalse();
        assertThat(p.selectDescriptors(s.source(),selectPage(group,"")).status()).isEqualTo("NO_FILES");assertThat(p.selectDescriptors(s.source(),"<main>첨부 없음</main>").complete()).isFalse();
        var limit=p.selectDescriptors(s.source(),selectPage(group,IntStream.rangeClosed(1,11).mapToObj(i->selectItem(group,i,"hwp")).collect(Collectors.joining())));assertThat(limit.status()).isEqualTo("LIMIT_EXCEEDED");assertThat(limit.descriptors()).hasSize(10);
        String one=selectItem(group,1,"pdf");assertThat(p.selectDescriptors(s.source(),selectPage(group,one+one)).descriptors()).hasSize(1);var conflict=p.selectDescriptors(s.source(),selectPage(group,one+one.replace("지원 공고문 (1).pdf","다른 공고문.pdf")));assertThat(conflict.complete()).isFalse();assertThat(conflict.descriptors()).hasSize(1);
    }
    @ParameterizedTest @MethodSource("selectGroups") void validatesIdentityUriBodyAndLimits(String group){
        var s=JeonbukThirdDownloadCases.selectCase(group);var p=s.profile();var site=Site.valueOf(group);String page=selectPage(group,selectItem(group,1,"pdf"));var r=p.selectDescriptors(s.source(),page).descriptors().getFirst().selectRequest();
        assertThat(p.selectApprovedRequest(r,r)).isTrue();assertThat(p.selectApprovedRequest(r,Request.selectGet(p.selectDetailUri(s.source())))).isFalse();assertThat(p.selectApprovedRequest(new Request(r.uri(),"POST",Map.of("x","y")))).isFalse();
        for(String bad:List.of("https:opaque",r.uri().toString().replace("https:","http:"),r.uri().toString().replace(site.host,"evil.example"),r.uri()+"#x",r.uri()+"&other=1",r.uri()+"&"+site.fileKey+"=1"))assertThat(p.selectApprovedRequest(URI.create(bad))).as(bad).isFalse();
        var n=new AnnouncementSourceIdentityNormalizer();for(String bad:List.of(s.source().sourceUrl()+"&unknown=1",s.source().sourceUrl().replace("https:","http:"),s.source().sourceUrl()+"&"+site.idKey+"=0"))assertThatThrownBy(()->p.selectDetailUri(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(bad)),bad,site.sourceCode,"SPRING_BBS"))).hasMessage("PROFILE_REQUIRED");
        assertThatThrownBy(()->p.selectDetailUri(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE","wrong",s.source().sourceUrl(),site.sourceCode,"SPRING_BBS"))).hasMessage("PROFILE_REQUIRED");
        assertThat(JeonbukThirdNoticePage.selectContent(site,Jsoup.parse(page)).text()).isEqualTo("소상공인 지원금");AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(Jsoup.parse(page),"소상공인 지원",s.titleLayout());
        assertThatThrownBy(()->JeonbukThirdNoticePage.selectContent(site,Jsoup.parse(page+page))).isInstanceOf(IllegalArgumentException.class);
        var budget=new AnnouncementAttachmentBbsOfficialObservationTest.Budget(p,false);assertThat(budget.maximumRequests).isEqualTo(6);assertThat(budget.maximumBytes).isEqualTo(23L*1024*1024);budget.reserveBody();assertThat(budget.bytes).isEqualTo(2L*1024*1024);
        assertThat(p.selectDescriptors(s.source()," ".repeat(1048577)).complete()).isFalse();assertThat(p.selectDescriptors(s.source(),page.replace(site.download,"https://evil.example/download" )).descriptors()).isEmpty();
    }
    @Test void jeonjuAcceptsObservedEmptySeparatorsButNeverDuplicateParameters(){
        var s=JeonbukThirdDownloadCases.selectCase("JEONJU");var n=new AnnouncementSourceIdentityNormalizer();String url=s.source().sourceUrl().replace("&","&&");assertThat(s.profile().selectDetailUri(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(url)),url,"LGS-000164","SPRING_BBS"))).isEqualTo(s.profile().selectDetailUri(s.source()));
    }
    @Test void jeonbukPairsPreviewAndArchiveWithoutDownloadingThem(){
        var s=JeonbukThirdDownloadCases.selectCase("JEONBUK");String all="<a class='ico_allfile' href='/board/downloadAll.jeonbuk?boardId=BBS_0000129&dataSid=670379'>전체파일다운</a>";String page=selectPage("JEONBUK",all+selectItem("JEONBUK",1,"hwpx"));var p=s.profile();var r=p.selectDescriptors(s.source(),page);assertThat(r.complete()).isTrue();assertThat(r.descriptors()).hasSize(1);
        for(String bad:List.of(page.replace("dataSid=670379","dataSid=1"),page.replace("BBS_0000129","BBS_0000006")))assertThat(p.selectDescriptors(s.source(),bad).descriptors()).isEmpty();
        assertThat(p.selectApprovedRequest(URI.create("https://www.jeonbuk.go.kr/board/downloadAll.jeonbuk?boardId=BBS_0000129&dataSid=670379"))).isFalse();
        var partial=p.selectDescriptors(s.source(),page.replace("SynapViewer.jeonbuk","unknownPreview"));assertThat(partial.complete()).isFalse();assertThat(partial.descriptors()).hasSize(1);
    }
    @EnabledIfEnvironmentVariable(named="SANEB_JEONBUK_THIRD_SURVEY_FIXTURE",matches="true")
    @ParameterizedTest @MethodSource("selectGroups") void actualOfficialBoundaries(String group)throws Exception{
        var s=JeonbukThirdDownloadCases.selectCase(group);String html=Files.readString(Path.of("build/qa-jeonbuk-third-20260929/"+group+"-detail.html"));var page=Jsoup.parse(html);AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,s.title(),s.titleLayout());var r=s.profile().selectDescriptors(s.source(),html);assertThat(r.complete()).as(group).isTrue();assertThat(r.descriptors()).hasSize(1);assertThat(JeonbukThirdNoticePage.selectContent(Site.valueOf(group),page).text()).contains(group.equals("JEONJU")?"소상공인":"경영안정자금");
    }
    @Test void catalogStaysReferenceOnly()throws Exception{
        var json=new com.fasterxml.jackson.databind.ObjectMapper();var notices=json.readTree(Files.readString(Path.of("src/main/resources/announcement-attachment/provider-qa-catalog-v2.json"))).path("notices");for(String group:selectGroups().toList()){var s=JeonbukThirdDownloadCases.selectCase(group);var ref=StreamSupport.stream(notices.spliterator(),false).filter(n->s.code().equals(n.path("caseCode").asText())).findFirst().orElseThrow();assertThat(ref.path("source")).isEqualTo(json.valueToTree(s.source()));assertThat(ref.hasNonNull("expectation")).isFalse();assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectCases(group).count()).isEqualTo(1);}
    }
}
