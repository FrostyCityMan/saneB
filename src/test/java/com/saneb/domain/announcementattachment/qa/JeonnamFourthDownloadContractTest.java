package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.provider.content.BoseongNoticePage;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

class JeonnamFourthDownloadContractTest {
    private String selectItem(int id,String ext){return "<li><span><img src='/images/common/ext_img/default.gif' alt='첨부파일'></span><a href='#none' onclick=\"goDownLoad('공고"+"A".repeat(24)+"==','파일"+id+"B".repeat(48)+"==','/ntisho"+"C".repeat(60)+"')\">지원 공고문."+ext+"</a></li>";}
    private String selectPage(String items,int count){return "<nav>수출 메뉴</nav><div id='content'><form name='nnn' method='post' action='https://eminwon.boseong.go.kr/emwp/jsp/ofr/FileDownNew.jsp'><input type='hidden' name='user_file_nm'><input type='hidden' name='sys_file_nm'><input type='hidden' name='file_path'></form><div id='board_basic_view'><div class='news_tit'><h3>청년 지원</h3><dl><dd>담당자</dd></dl></div><div class='file_attach'><h5>첨부파일<span>(<strong>"+count+"</strong>)</span></h5><div class='attach_thum'><ul>"+items+"</ul></div></div><div class='board_cont'>청년 문화비 지원</div></div></div><footer>푸터</footer>";}
    @Test void preservesGoodFilesSeparatesFailuresAndUnsupported(){
        var s=JeonnamFourthDownloadCases.selectCase("BOSEONG");var p=s.profile();String items=selectItem(1,"pdf")+selectItem(2,"hwp")+selectItem(3,"hwpx");var r=p.selectDescriptors(s.source(),selectPage(items,3));
        assertThat(r.complete()).isTrue();assertThat(r.descriptors()).hasSize(3);r.descriptors().forEach(d->{assertThat(d.documentRole()).isEqualTo("UNKNOWN");assertThat(d.downloadAllowed()).isTrue();assertThat(d.selectRequest().method()).isEqualTo("POST");assertThat(p.selectApprovedRequest(d.selectRequest())).isTrue();assertThat(d.locator().toString()).doesNotContain("BBBBBBBBBBBBBBBBBBBBBBBB");});
        for(String extra:List.of("<li><a href='/unknown'>다른 첨부</a></li>","<li><button>파일</button></li>","<li><img src='/unknown'></li>","<script>unknown()</script>")){var partial=p.selectDescriptors(s.source(),selectPage(items+extra,4));assertThat(partial.complete()).isFalse();assertThat(partial.descriptors()).hasSize(3);}
        var unsupported=p.selectDescriptors(s.source(),selectPage(items+selectItem(4,"jpg"),4));assertThat(unsupported.complete()).isTrue();assertThat(unsupported.descriptors().getLast().downloadAllowed()).isFalse();
        assertThat(p.selectDescriptors(s.source(),selectPage("",0)).status()).isEqualTo("NO_FILES");assertThat(p.selectDescriptors(s.source(),"<main>첨부 없음</main>").complete()).isFalse();
        assertThat(p.selectDescriptors(s.source(),selectPage(items,2)).complete()).isFalse();
        var limit=p.selectDescriptors(s.source(),selectPage(IntStream.rangeClosed(1,11).mapToObj(i->selectItem(i,"hwp")).collect(Collectors.joining()),11));assertThat(limit.status()).isEqualTo("LIMIT_EXCEEDED");assertThat(limit.descriptors()).hasSize(10);
    }
    @Test void validatesIdentityFormsAndRequestBoundary(){
        var s=JeonnamFourthDownloadCases.selectCase("BOSEONG");var p=s.profile();String one=selectItem(1,"pdf"),page=selectPage(one,1);var req=p.selectDescriptors(s.source(),page).descriptors().getFirst().selectRequest();
        assertThat(p.selectApprovedRequest(req,req)).isTrue();assertThat(p.selectApprovedRequest(req.uri())).isFalse();assertThat(p.selectApprovedRequest(req,Request.selectGet(p.selectDetailUri(s.source())))).isFalse();
        for(String bad:List.of("https:opaque",req.uri().toString().replace("https:","http:"),req.uri().toString().replace("eminwon.boseong.go.kr","evil.example"),req.uri()+"#x",req.uri()+"?x=1"))assertThat(p.selectApprovedRequest(new Request(URI.create(bad),req.method(),req.form()))).isFalse();
        var fields=new HashMap<>(req.form());fields.put("extra","value");assertThat(p.selectApprovedRequest(new Request(req.uri(),"POST",fields))).isFalse();
        for(String bad:List.of(page.replace("method='post'","method='get'"),page.replace("name='file_path'","name='other'"),page.replace("eminwon.boseong.go.kr","evil.example"),page.replace("goDownLoad(","evil();goDownLoad("),page.replace("/ntisho","../ntisho")))assertThat(p.selectDescriptors(s.source(),bad).descriptors()).isEmpty();
        var n=new AnnouncementSourceIdentityNormalizer();for(String bad:List.of(s.source().sourceUrl()+"&idx=1",s.source().sourceUrl()+"&unknown=1",s.source().sourceUrl().replace("https:","http:")))assertThatThrownBy(()->p.selectDetailUri(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(bad)),bad,"LGS-000187","SPRING_BBS"))).hasMessage("PROFILE_REQUIRED");
        assertThatThrownBy(()->p.selectDetailUri(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE","wrong",s.source().sourceUrl(),"LGS-000187","SPRING_BBS"))).hasMessage("PROFILE_REQUIRED");
        assertThat(p.selectDescriptors(s.source()," ".repeat(1048577)).complete()).isFalse();assertThat(p.selectDescriptors(s.source(),selectPage(one+one,1)).descriptors()).hasSize(1);
        var conflict=p.selectDescriptors(s.source(),selectPage(one+one.replace("지원 공고문.pdf","다른 파일.pdf"),2));assertThat(conflict.complete()).isFalse();assertThat(conflict.descriptors()).hasSize(1);
    }
    @Test void verifiesBodyTitleAndBudget(){
        var s=JeonnamFourthDownloadCases.selectCase("BOSEONG");var page=Jsoup.parse(selectPage(selectItem(1,"hwpx"),1));assertThat(BoseongNoticePage.selectContent(page).text()).isEqualTo("청년 문화비 지원");AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,"청년 지원",s.titleLayout());assertThatThrownBy(()->BoseongNoticePage.selectContent(Jsoup.parse(page+page.toString()))).isInstanceOf(IllegalArgumentException.class);
        var b=new AnnouncementAttachmentBbsOfficialObservationTest.Budget(s.profile(),false);assertThat(b.maximumRequests).isEqualTo(6);assertThat(b.maximumBytes).isEqualTo(23L*1024*1024);b.reserveBody();assertThat(b.bytes).isEqualTo(2L*1024*1024);
    }
    @EnabledIfEnvironmentVariable(named="SANEB_JEONNAM_FOURTH_SURVEY_FIXTURE",matches="true")
    @Test void actualOfficialBoundaries()throws Exception{
        var s=JeonnamFourthDownloadCases.selectCase("BOSEONG");String html=Files.readString(Path.of("build/qa-jeonnam-fourth-20260929/BOSEONG-detail.html"));var page=Jsoup.parse(html);AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,s.title(),s.titleLayout());var r=s.profile().selectDescriptors(s.source(),html);assertThat(r.complete()).isTrue();assertThat(r.descriptors()).hasSize(1);assertThat(BoseongNoticePage.selectContent(page).text()).contains("청년","25만원");
    }
    @Test void catalogStaysReferenceOnly()throws Exception{
        var s=JeonnamFourthDownloadCases.selectCase("BOSEONG");var json=new com.fasterxml.jackson.databind.ObjectMapper();var notices=json.readTree(Files.readString(Path.of("src/main/resources/announcement-attachment/provider-qa-catalog-v2.json"))).path("notices");var ref=StreamSupport.stream(notices.spliterator(),false).filter(n->s.code().equals(n.path("caseCode").asText())).findFirst().orElseThrow();assertThat(ref.path("source")).isEqualTo(json.valueToTree(s.source()));assertThat(ref.hasNonNull("expectation")).isFalse();assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectCases("BOSEONG").count()).isEqualTo(1);
    }
}
