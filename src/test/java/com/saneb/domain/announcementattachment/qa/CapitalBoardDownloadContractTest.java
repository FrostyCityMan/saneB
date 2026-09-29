package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.classification.*;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.CapitalEminwonNoticePage;
import com.saneb.domain.announcementsource.provider.content.CapitalEminwonNoticePage.Site;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class CapitalBoardDownloadContractTest {
    static Stream<String> selectGroups(){return CapitalBoardDownloadCases.GROUPS.stream().sorted();}
    private String selectLink(String group,int id,String ext){String href=group.equals("GUNPO")?"javascript:goDownLoad('공고문."+ext+"','saved"+id+"."+ext+"','/ntishome/file/upload/ofr/ofr/20260929');"
            :"https://"+Site.valueOf(group).fileHost+"/emwp/jsp/ofr/FileDown.jsp?user_file_nm=공고문."+ext+"&sys_file_nm=saved"+id+"."+ext+"&file_path=/ntishome/file/upload/ofr/ofr/20260929";
        return "<a class='p-attach__link' href=\""+href+"\"><span class='p-icon'>파일</span><span>공고문."+ext+"</span><i class='p-icon'></i></a>";}
    private String selectPage(String group,String files){boolean y=group.equals("YANGJU"),g=group.equals("GUNPO");
        String form=g?"<form name='form2' method='post'><input type='hidden' name='user_file_nm'><input type='hidden' name='sys_file_nm'><input type='hidden' name='file_path'></form>":"";
        return "<main><nav>수출 메뉴</nav>"+form+"<div class='p-wrap bbs bbs__view'><table class='"+(y?"bbs_default view":"p-table block")+"'>"
                +(y||g?"<tr><th>제목</th><td>청년 지원사업</td></tr>":"<tr><td><span class='p-table__subject_text'>청년 지원사업</span></td></tr>")
                +"<tr>"+(y||g?"<th>"+(y?"내용":"상세내용")+"</th>":"")+"<td title='내용' class='"+(y?"bbs_content":"p-table__content")+"'>사업자 지원금 <a href='/noise.pdf'>본문 링크</a></td></tr>"
                +"<tr>"+(y||g?"<th>"+(y?"파일":"첨부")+"</th>":"")+"<td class='p-table--attach'><ul><li>"+files+"</li></ul></td></tr></table></div><footer>특허 푸터</footer></main>";}
    @ParameterizedTest @MethodSource("selectGroups") void keepsGoodFilesDespiteUnknownAndUnsupportedFiles(String group){
        var s=CapitalBoardDownloadCases.selectCase(group);var p=s.profile();String links=selectLink(group,1,"hwp")+selectLink(group,2,"hwpx")+selectLink(group,3,"pdf");
        var r=p.selectDescriptors(s.source(),selectPage(group,links));assertThat(r.complete()).isTrue();assertThat(r.descriptors()).hasSize(3);r.descriptors().forEach(d->{assertThat(d.documentRole()).isEqualTo("UNKNOWN");assertThat(p.selectApprovedRequest(d.selectRequest())).isTrue();assertThat(d.downloadAllowed()).isTrue();});
        for(String unknown:List.of("<a href='/bad.pdf'>미확인</a>","<script>bad()</script>","<button>첨부</button>","<img src='/bad'>")){var partial=p.selectDescriptors(s.source(),selectPage(group,links+unknown));assertThat(partial.complete()).isFalse();assertThat(partial.descriptors()).hasSize(3);}
        var mixed=p.selectDescriptors(s.source(),selectPage(group,links+selectLink(group,4,"jpg")));assertThat(mixed.descriptors()).hasSize(4);assertThat(mixed.descriptors().getLast().downloadAllowed()).isFalse();
        assertThat(p.selectDescriptors(s.source(),selectPage(group,"")).status()).isEqualTo("NO_FILES");assertThat(p.selectDescriptors(s.source(),"<main/>").complete()).isFalse();
    }
    @ParameterizedTest @MethodSource("selectGroups") void enforcesDuplicatesConflictsLimitsAndRedirectScope(String group){
        var s=CapitalBoardDownloadCases.selectCase(group);var p=s.profile();String one=selectLink(group,1,"hwp");assertThat(p.selectDescriptors(s.source(),selectPage(group,one+one)).descriptors()).hasSize(1);
        var conflict=p.selectDescriptors(s.source(),selectPage(group,one+one.replace("공고문.hwp","변경.hwp")));assertThat(conflict.complete()).isFalse();assertThat(conflict.descriptors()).hasSize(1);
        var limit=p.selectDescriptors(s.source(),selectPage(group,IntStream.rangeClosed(1,11).mapToObj(i->selectLink(group,i,"hwp")).collect(Collectors.joining())));assertThat(limit.status()).isEqualTo("LIMIT_EXCEEDED");assertThat(limit.descriptors()).hasSize(10);
        var req=p.selectDescriptors(s.source(),selectPage(group,one)).descriptors().getFirst().selectRequest();assertThat(req.method()).isEqualTo(group.equals("GUNPO")?"POST":"GET");assertThat(p.selectApprovedRequest(req,req)).isTrue();assertThat(p.selectApprovedRequest(req,Request.selectGet(URI.create(s.source().sourceUrl())))).isFalse();
        assertThat(p.selectApprovedRequest(new Request(URI.create(req.uri().toString().replace("https:","http:")),req.method(),req.form()))).isFalse();
    }
    @ParameterizedTest @MethodSource("selectGroups") void verifiesSourceIdentityTitleBodyBoundaryAndBudget(String group){
        var s=CapitalBoardDownloadCases.selectCase(group);var p=s.profile();var n=new AnnouncementSourceIdentityNormalizer();String url=s.source().sourceUrl()+"&pageIndex=2&searchCnd=B_Subject&searchKrwd=test";
        assertThat(p.selectDetailUri(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(url)),url,s.source().localSourceCode(),"SPRING_BBS"))).isEqualTo(p.selectDetailUri(s.source()));
        for(String bad:List.of(url+"&bad=1",url+"&key=9",url.replace("https:","http:"),url+"#x"))assertThatThrownBy(()->p.selectDetailUri(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(bad)),bad,s.source().localSourceCode(),"SPRING_BBS"))).hasMessage("PROFILE_REQUIRED");
        assertThatThrownBy(()->p.selectDetailUri(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE","bad",url,s.source().localSourceCode(),"SPRING_BBS"))).hasMessage("PROFILE_REQUIRED");
        var page=Jsoup.parse(selectPage(group,selectLink(group,1,"hwp")));AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,"청년 지원사업",s.titleLayout());assertThatThrownBy(()->AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,"다른 제목",s.titleLayout())).isInstanceOf(AssertionError.class);
        assertThat(CapitalEminwonNoticePage.selectContent(Site.valueOf(group),page).text()).isEqualTo("사업자 지원금 본문 링크");
        assertThatThrownBy(()->CapitalEminwonNoticePage.selectContent(Site.valueOf(group),Jsoup.parse(selectPage(group,"").replace("title='내용'","title='변경'")))).isInstanceOf(IllegalArgumentException.class);
        for(boolean diagnostic:List.of(true,false)){var b=new AnnouncementAttachmentBbsOfficialObservationTest.Budget(p,diagnostic);assertThat(b.maximumRequests).isEqualTo(6);assertThat(b.maximumBytes).isEqualTo(23L*1024*1024);}
    }
    @Test void gunpoPreviewIsPairedAndNeverDownloaded(){
        var s=CapitalBoardDownloadCases.selectCase("GUNPO");String preview="<a class='p-attach__preview' href='/www/previewEminwonFile.do?fileUrl=/ntishome/file/upload/ofr/ofr/20260929&fileReNm=saved1.hwp&fileNm=공고문.hwp&notAncmtMgtNo=43659'>파일 미리보기</a>";
        String html=selectPage("GUNPO",selectLink("GUNPO",1,"hwp")+preview);assertThat(s.profile().selectDescriptors(s.source(),html).complete()).isTrue();
        for(String bad:List.of(html.replace("notAncmtMgtNo=43659","notAncmtMgtNo=1"),html.replace("method='post'","method='get'"),html.replace("name='file_path'","name='other'")))assertThat(s.profile().selectDescriptors(s.source(),bad).complete()).isFalse();
        var partial=s.profile().selectDescriptors(s.source(),html.replace("notAncmtMgtNo=43659","notAncmtMgtNo=1"));assertThat(partial.descriptors()).hasSize(1);
        var req=partial.descriptors().getFirst().selectRequest();assertThat(s.profile().selectApprovedRequest(Request.selectGet(req.uri()))).isFalse();var changed=new HashMap<>(req.form());changed.put("extra","x");assertThat(s.profile().selectApprovedRequest(new Request(req.uri(),"POST",changed))).isFalse();
    }
    @EnabledIfEnvironmentVariable(named="SANEB_CAPITAL_BOARD_SURVEY_FIXTURE",matches="true")
    @ParameterizedTest @MethodSource("selectGroups") void actualOfficialBoundaries(String group) throws Exception {
        var s=CapitalBoardDownloadCases.selectCase(group);String html=Files.readString(Path.of("build/qa-capital-board-20260929/"+group+"-detail.html"));
        AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(Jsoup.parse(html),s.title(),s.titleLayout());var r=s.profile().selectDescriptors(s.source(),html);assertThat(r.complete()).as(group).isTrue();assertThat(r.descriptors()).hasSize(1);
    }
    @Test void titleAndCatalogAreNotAutomaticallyApproved() throws Exception {
        var json=new com.fasterxml.jackson.databind.ObjectMapper();var notices=json.readTree(Files.readString(Path.of("src/main/resources/announcement-attachment/provider-qa-catalog-v2.json"))).path("notices");
        for(String group:selectGroups().toList()){var s=CapitalBoardDownloadCases.selectCase(group);var decision=new AnnouncementSourceClassificationEngine().selectDecision(new AnnouncementSourceClassificationInput("LOCAL_GOV_NOTICE",s.title(),null,null,List.of(),BodySourceCode.NONE,BodyAvailabilityCode.UNAVAILABLE),AnnouncementAttachmentRealFileQaTest.selectDraftRuleSet());assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectTitleMayProceed(decision)).as(group).isTrue();assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectCases(group).count()).isEqualTo(1);
            var ref=StreamSupport.stream(notices.spliterator(),false).filter(n->s.code().equals(n.path("caseCode").asText())).findFirst().orElseThrow();assertThat(ref.path("source")).isEqualTo(json.valueToTree(s.source()));assertThat(ref.hasNonNull("expectation")).isFalse();}
    }
}
