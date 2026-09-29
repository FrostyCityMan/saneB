package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.provider.content.HampyeongNoticePage;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

class JeonnamFifthDownloadContractTest {
    private String selectItem(int id,String ext){return "<a href='#' onclick=\"goDown('공고"+"A".repeat(24)+"==','파일"+id+"B".repeat(48)+"==','/ntisho"+"C".repeat(60)+"')\">지원 공고문."+ext+"</a><br>";}
    private String selectPage(String files){return "<nav>수출 메뉴</nav><form name='ffile' method='post' action='https://eminwon.hampyeong.go.kr/emwp/jsp/ofr/FileDownNew.jsp'><input type='hidden' name='seq'><input type='hidden' name='user_file_nm'><input type='hidden' name='sys_file_nm'><input type='hidden' name='file_path'></form><div id='board_view'><table class='basic_table'><tbody><tr><th>제목</th><td colspan='3'>소상공인 지원</td></tr><tr><th>담당부서</th><td>담당자</td></tr><tr><td colspan='4'>소상공인 지원금</td></tr><tr><th>첨부파일</th><td colspan='3'>"+files+"</td></tr></tbody></table></div><footer>푸터</footer>";}
    @Test void preservesGoodFilesSeparatesFailuresAndUnsupported(){
        var s=JeonnamFifthDownloadCases.selectCase("HAMPYEONG");var p=s.profile();String files=selectItem(1,"pdf")+selectItem(2,"hwp")+selectItem(3,"hwpx");var r=p.selectDescriptors(s.source(),selectPage(files));assertThat(r.complete()).isTrue();assertThat(r.descriptors()).hasSize(3);
        r.descriptors().forEach(d->{assertThat(d.documentRole()).isEqualTo("UNKNOWN");assertThat(d.downloadAllowed()).isTrue();assertThat(d.selectRequest().method()).isEqualTo("POST");assertThat(d.selectRequest().form().get("seq")).isEmpty();assertThat(p.selectApprovedRequest(d.selectRequest())).isTrue();assertThat(d.locator().toString()).doesNotContain("BBBBBBBBBBBBBBBBBBBBBBBB");});
        for(String extra:List.of("<a href='/unknown'>다른 첨부</a>","<button>파일</button>","<img src='/unknown'>","<script>unknown()</script>")){var partial=p.selectDescriptors(s.source(),selectPage(files+extra));assertThat(partial.complete()).isFalse();assertThat(partial.descriptors()).hasSize(3);}
        var unsupported=p.selectDescriptors(s.source(),selectPage(files+selectItem(4,"jpg")));assertThat(unsupported.complete()).isTrue();assertThat(unsupported.descriptors().getLast().downloadAllowed()).isFalse();assertThat(p.selectDescriptors(s.source(),selectPage("")).status()).isEqualTo("NO_FILES");assertThat(p.selectDescriptors(s.source(),"<main>첨부 없음</main>").complete()).isFalse();
        var limit=p.selectDescriptors(s.source(),selectPage(IntStream.rangeClosed(1,11).mapToObj(i->selectItem(i,"hwp")).collect(Collectors.joining())));assertThat(limit.status()).isEqualTo("LIMIT_EXCEEDED");assertThat(limit.descriptors()).hasSize(10);
    }
    @Test void validatesIdentityFormsAndRequestBoundary(){
        var s=JeonnamFifthDownloadCases.selectCase("HAMPYEONG");var p=s.profile();String one=selectItem(1,"pdf"),page=selectPage(one);var req=p.selectDescriptors(s.source(),page).descriptors().getFirst().selectRequest();assertThat(p.selectApprovedRequest(req,req)).isTrue();assertThat(p.selectApprovedRequest(req.uri())).isFalse();assertThat(p.selectApprovedRequest(req,Request.selectGet(p.selectDetailUri(s.source())))).isFalse();
        for(String bad:List.of("https:opaque",req.uri().toString().replace("https:","http:"),req.uri().toString().replace("eminwon.hampyeong.go.kr","evil.example"),req.uri()+"#x",req.uri()+"?x=1"))assertThat(p.selectApprovedRequest(new Request(URI.create(bad),req.method(),req.form()))).isFalse();
        var fields=new HashMap<>(req.form());fields.put("seq","32368");assertThat(p.selectApprovedRequest(new Request(req.uri(),"POST",fields))).isFalse();fields.put("seq","");fields.put("other","x");assertThat(p.selectApprovedRequest(new Request(req.uri(),"POST",fields))).isFalse();
        for(String bad:List.of(page.replace("method='post'","method='get'"),page.replace("name='seq'","name='other'"),page.replace("name='seq'","name='seq' value='1'"),page.replace("eminwon.hampyeong.go.kr","evil.example"),page.replace("goDown(","evil();goDown("),page.replace("/ntisho","../ntisho")))assertThat(p.selectDescriptors(s.source(),bad).descriptors()).isEmpty();
        var n=new AnnouncementSourceIdentityNormalizer();for(String bad:List.of(s.source().sourceUrl()+"&SEQ=1",s.source().sourceUrl()+"&unknown=1",s.source().sourceUrl().replace("https:","http:"),s.source().sourceUrl().replace("www273","www999")))assertThatThrownBy(()->p.selectDetailUri(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(bad)),bad,"LGS-000194","HEURISTIC_NOTICE"))).hasMessage("PROFILE_REQUIRED");assertThatThrownBy(()->p.selectDetailUri(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE","wrong",s.source().sourceUrl(),"LGS-000194","HEURISTIC_NOTICE"))).hasMessage("PROFILE_REQUIRED");
        assertThat(p.selectDescriptors(s.source()," ".repeat(1048577)).complete()).isFalse();assertThat(p.selectDescriptors(s.source(),selectPage(one+one)).descriptors()).hasSize(1);var conflict=p.selectDescriptors(s.source(),selectPage(one+one.replace("지원 공고문.pdf","다른 파일.pdf")));assertThat(conflict.complete()).isFalse();assertThat(conflict.descriptors()).hasSize(1);
    }
    @Test void verifiesBodyTitleAndBudget(){
        var s=JeonnamFifthDownloadCases.selectCase("HAMPYEONG");var page=Jsoup.parse(selectPage(selectItem(1,"hwpx")));assertThat(HampyeongNoticePage.selectContent(page).text()).isEqualTo("소상공인 지원금");AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,"소상공인 지원",s.titleLayout());assertThatThrownBy(()->HampyeongNoticePage.selectContent(Jsoup.parse(page+page.toString()))).isInstanceOf(IllegalArgumentException.class);
        var b=new AnnouncementAttachmentBbsOfficialObservationTest.Budget(s.profile(),false);assertThat(b.maximumRequests).isEqualTo(6);assertThat(b.maximumBytes).isEqualTo(23L*1024*1024);b.reserveBody();assertThat(b.bytes).isEqualTo(2L*1024*1024);
    }
    @EnabledIfEnvironmentVariable(named="SANEB_JEONNAM_FIFTH_SURVEY_FIXTURE",matches="true")
    @Test void actualOfficialBoundaries()throws Exception{
        var s=JeonnamFifthDownloadCases.selectCase("HAMPYEONG");String html=Files.readString(Path.of("build/qa-jeonnam-fifth-20260929/HAMPYEONG-detail.html"));var page=Jsoup.parse(html);AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,s.title(),s.titleLayout());var r=s.profile().selectDescriptors(s.source(),html);assertThat(r.complete()).isTrue();assertThat(r.descriptors()).hasSize(1);assertThat(HampyeongNoticePage.selectContent(page).text()).contains("소상공인","제외업종");
    }
    @Test void catalogStaysReferenceOnly()throws Exception{
        var s=JeonnamFifthDownloadCases.selectCase("HAMPYEONG");var json=new com.fasterxml.jackson.databind.ObjectMapper();var notices=json.readTree(Files.readString(Path.of("src/main/resources/announcement-attachment/provider-qa-catalog-v2.json"))).path("notices");var ref=StreamSupport.stream(notices.spliterator(),false).filter(n->s.code().equals(n.path("caseCode").asText())).findFirst().orElseThrow();assertThat(ref.path("source")).isEqualTo(json.valueToTree(s.source()));assertThat(ref.hasNonNull("expectation")).isFalse();assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectCases("HAMPYEONG").count()).isEqualTo(1);
    }
}
