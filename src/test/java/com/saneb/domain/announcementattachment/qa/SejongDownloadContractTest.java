package com.saneb.domain.announcementattachment.qa;
import static org.assertj.core.api.Assertions.*;
import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.provider.content.SejongNoticePage;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
class SejongDownloadContractTest {
    private String selectItem(int id,String ext) {String name="지원 공고."+ext;return "<div class=download><img src='/images/mimetype/"+ext+".gif' alt='파일'><a href='https://eminwon.sejong.go.kr/emwp/jsp/ofr/FileDown.jsp?user_file_nm="+name+"&amp;sys_file_nm=file"+id+"."+ext+"&amp;file_path=/ntishome/file/upload/ofr/ofr/20260129'>"+name+"</a></div>";}
    private String selectPage(String files){return "<div id=txt><div class=table-responsive><table class='table table-bordered'><tr><th>제목</th><td>소상공인 지원</td></tr><tr><th>담당부서</th><td>수출 부서</td></tr><tr><th>파일첨부</th><td>"+files+"</td></tr><tr><td class='tbl_cnts cell_left'>소상공인 지원금</td></tr></table></div></div>";}
    @Test void preservesGoodFilesAndSeparatesIncompleteDiscovery() {
        var s=SejongDownloadCases.selectCase();var p=s.profile();String good=selectItem(1,"pdf")+selectItem(2,"hwp")+selectItem(3,"hwpx");
        var r=p.selectDescriptors(s.source(),selectPage(good));assertThat(r.complete()).isTrue();assertThat(r.descriptors()).hasSize(3).allSatisfy(d->{assertThat(d.downloadAllowed()).isTrue();assertThat(d.documentRole()).isEqualTo("UNKNOWN");assertThat(d.selectRequest().method()).isEqualTo("GET");assertThat(p.selectApprovedRequest(d.selectRequest(),d.selectRequest())).isTrue();});
        for(String bad:List.of("<a href='/unknown'>파일</a>","<button>파일</button>","<img src='/unknown'>",selectItem(4,"pdf").replace("eminwon.sejong.go.kr","evil.example"))){var partial=p.selectDescriptors(s.source(),selectPage(good+bad));assertThat(partial.complete()).isFalse();assertThat(partial.descriptors()).hasSize(3);}
        var unsupported=p.selectDescriptors(s.source(),selectPage(good+selectItem(4,"xlsx")));assertThat(unsupported.complete()).isTrue();assertThat(unsupported.descriptors().getLast().downloadAllowed()).isFalse();
        assertThat(p.selectDescriptors(s.source(),selectPage("")).status()).isEqualTo("NO_FILES");assertThat(p.selectDescriptors(s.source(),"<p>첨부 없음</p>").complete()).isFalse();
        var limit=p.selectDescriptors(s.source(),selectPage(IntStream.rangeClosed(1,11).mapToObj(i->selectItem(i,"pdf")).collect(Collectors.joining())));assertThat(limit.status()).isEqualTo("LIMIT_EXCEEDED");assertThat(limit.descriptors()).hasSize(10);
    }
    @Test void requestsAreBoundToExactSourceAndFile() {
        var s=SejongDownloadCases.selectCase();var p=s.profile();var d=p.selectDescriptors(s.source(),selectPage(selectItem(1,"pdf"))).descriptors().getFirst();
        for(String bad:List.of(d.fetchUri().toString().replace("https:","http:"),d.fetchUri().toString().replace("eminwon.sejong.go.kr","127.0.0.1"),d.fetchUri()+"&extra=1",d.fetchUri()+"&sys_file_nm=other.pdf",d.fetchUri()+"#x","https:opaque"))assertThat(p.selectApprovedRequest(URI.create(bad))).isFalse();
        assertThat(p.selectApprovedRequest(new Request(d.fetchUri(),"POST",Map.of("x","y")))).isFalse();assertThatThrownBy(()->new Request(d.fetchUri(),"GET",Map.of("x","y"))).hasMessage("ATTACHMENT_REQUEST_INVALID");assertThatThrownBy(()->new Request(d.fetchUri(),"POST",Map.of())).hasMessage("ATTACHMENT_REQUEST_INVALID");
        var other=p.selectDescriptors(s.source(),selectPage(selectItem(2,"pdf"))).descriptors().getFirst();assertThat(p.selectApprovedRequest(d.selectRequest(),other.selectRequest())).isFalse();
        var n=new AnnouncementSourceIdentityNormalizer();for(String bad:List.of(s.source().sourceUrl()+"&extra=1",s.source().sourceUrl()+"&not_ancmt_mgt_no=99",s.source().sourceUrl().replace("C1_1","C4"),s.source().sourceUrl().replace("https:","http:")))assertThatThrownBy(()->p.selectDetailUri(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(bad)),bad,"LGS-000083","SAEOL_GOSI"))).hasMessage("PROFILE_REQUIRED");
        assertThatThrownBy(()->p.selectDetailUri(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE","wrong",s.source().sourceUrl(),"LGS-000083","SAEOL_GOSI"))).hasMessage("PROFILE_REQUIRED");
    }
    @Test void duplicateConflictsAndUnsafeLinksStayVisible() {
        var s=SejongDownloadCases.selectCase();String one=selectItem(1,"pdf");var p=s.profile();assertThat(p.selectDescriptors(s.source(),selectPage(one+one)).descriptors()).hasSize(1);
        assertThat(p.selectDescriptors(s.source(),selectPage(one+one.replace("지원 공고","다른 공고"))).complete()).isFalse();
        for(String bad:List.of(one.replace("/ntishome/","/../ntishome/"),one.replace("<a href","<a onclick='evil()' href"),one.replace("href='https://","href='javascript:evil();https://"),one.replace("지원 공고.pdf</a>","상충.pdf</a>")))assertThat(p.selectDescriptors(s.source(),selectPage(bad)).complete()).isFalse();
    }
    @Test void bodyAndTitleAreSeparateFromMetadata() {
        var page=Jsoup.parse(selectPage(selectItem(1,"pdf")));assertThat(SejongNoticePage.selectContent(page).text()).isEqualTo("소상공인 지원금");AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,"소상공인 지원",SejongDownloadCases.selectCase().titleLayout());
        assertThatThrownBy(()->SejongNoticePage.selectContent(Jsoup.parse(page.toString()+page))).isInstanceOf(IllegalArgumentException.class);
        var budget=new AnnouncementAttachmentBbsOfficialObservationTest.Budget(SejongDownloadCases.selectCase().profile(),false);assertThat(budget.maximumRequests).isEqualTo(6);assertThat(budget.maximumBytes).isEqualTo(23L*1024*1024);
    }
    @Test @EnabledIfEnvironmentVariable(named="SANEB_SEJONG_SURVEY_FIXTURE",matches="true")
    void actualHtmlAttachmentAndBodyBoundaries()throws Exception {
        var s=SejongDownloadCases.selectCase();String html=Files.readString(Path.of("build/qa-ulsan-sejong-20260930/SEJONG-detail.html"));var page=Jsoup.parse(html);AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,s.title(),s.titleLayout());var r=s.profile().selectDescriptors(s.source(),html);assertThat(r.complete()).as("%s %s",r.status(),r.warnings()).isTrue();assertThat(r.descriptors()).hasSize(1).allSatisfy(d->{assertThat(d.downloadAllowed()).isTrue();assertThat(d.expectedFormat()).isEqualTo("HWP");});assertThat(SejongNoticePage.selectContent(page).text()).isNotBlank();
    }
    @Test void catalogExpectationRemainsUnapproved()throws Exception {
        var s=SejongDownloadCases.selectCase();var json=new com.fasterxml.jackson.databind.ObjectMapper();var notices=json.readTree(Files.readString(Path.of("src/main/resources/announcement-attachment/provider-qa-catalog-v2.json"))).path("notices");var ref=StreamSupport.stream(notices.spliterator(),false).filter(n->s.code().equals(n.path("caseCode").asText())).findFirst().orElseThrow();assertThat(ref.path("source")).isEqualTo(json.valueToTree(s.source()));assertThat(ref.hasNonNull("expectation")).isFalse();
    }
}
