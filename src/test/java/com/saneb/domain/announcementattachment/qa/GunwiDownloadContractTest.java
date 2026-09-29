package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import com.saneb.domain.announcementattachment.discovery.AttachmentDiscoveryProfile;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.provider.content.GunwiNoticePage;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

class GunwiDownloadContractTest {
    private final AnnouncementAttachmentBbsOfficialObservationTest.ObservationCase sample=GunwiDownloadCases.selectCase();
    private String link(int id,String extension){return "<li><p class=file><a title='파일 다운로드' href='/programs/board/saeol/notice/download.do?file_seq="+id+"&not_ancmt_mgt_no=25554'>공고"+id+"."+extension+"</a></p></li>";}
    private String page(String links){return "<div class=boardView><div class=title><h4>소상공인 지원</h4><div><dl>수출 부서</dl></div><div><ul>"+links+"</ul></div></div><div class=cont><div class=board_content>소상공인 지원금</div></div></div>";}
    @Test void successfulFilesSurviveIndependentDiscoveryErrors(){
        var p=sample.profile();String good=link(1,"pdf")+link(2,"hwp")+link(3,"hwpx");
        var r=p.selectDescriptors(sample.source(),page(good));assertThat(r.complete()).isTrue();
        assertThat(r.descriptors()).hasSize(3).allSatisfy(d->{assertThat(d.downloadAllowed()).isTrue();assertThat(d.documentRole()).isEqualTo("UNKNOWN");assertThat(p.selectApprovedRequest(d.selectRequest())).isTrue();});
        for(String bad:List.of("<a href='/unknown'>미확인</a>","<script>loadFiles()</script>","<button>첨부</button>",link(4,"pdf").replace("25554","1"),link(4,"hwp").replace("title=","onclick='run()' title="))){
            var partial=p.selectDescriptors(sample.source(),page(good+bad));assertThat(partial.complete()).isFalse();assertThat(partial.descriptors()).hasSize(3);
        }
        var unsupported=p.selectDescriptors(sample.source(),page(good+link(4,"xlsx")));assertThat(unsupported.complete()).isTrue();assertThat(unsupported.descriptors().getLast().downloadAllowed()).isFalse();
        assertThat(p.selectDescriptors(sample.source(),page(good)+"<a href='/outside.pdf'>다른 링크</a>").descriptors()).hasSize(3);
    }
    @Test void absenceConflictsAndLimitsStayDistinct(){
        var p=sample.profile();String one=link(1,"hwp");
        assertThat(p.selectDescriptors(sample.source(),page("")).status()).isEqualTo("NO_FILES");
        assertThat(p.selectDescriptors(sample.source(),"<p>오류</p>").status()).isEqualTo("FAILED");
        assertThat(p.selectDescriptors(sample.source(),page("<li></li>")).status()).isEqualTo("FAILED");
        assertThat(p.selectDescriptors(sample.source(),page(one+one)).descriptors()).hasSize(1);
        assertThat(p.selectDescriptors(sample.source(),page(one+one.replace("공고1.hwp","변경.hwp"))).complete()).isFalse();
        var limited=p.selectDescriptors(sample.source(),page(IntStream.rangeClosed(1,11).mapToObj(i->link(i,"pdf")).collect(Collectors.joining())));
        assertThat(limited.status()).isEqualTo("LIMIT_EXCEEDED");assertThat(limited.descriptors()).hasSize(10);
    }
    @Test void sourceAndRedirectBoundaries(){
        var p=sample.profile();var n=new AnnouncementSourceIdentityNormalizer();String url=sample.source().sourceUrl();
        for(String bad:List.of(url+"&extra=1",url+"&cmd=2",url.replace("666","667"),url.replace("https:","http:"),url.replace("www.gunwi.go.kr","127.0.0.1"))){
            assertThatThrownBy(()->p.selectDetailUri(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(bad)),bad,"LGS-000053","GUNWI_NOTICE_TABLE"))).hasMessage("PROFILE_REQUIRED");
        }
        assertThatThrownBy(()->p.selectDetailUri(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE","wrong",url,"LGS-000053","GUNWI_NOTICE_TABLE"))).hasMessage("PROFILE_REQUIRED");
        assertThatThrownBy(()->p.selectDetailUri(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",sample.source().providerNoticeId(),url,"LGS-000211","GUNWI_NOTICE_TABLE"))).hasMessage("PROFILE_REQUIRED");
        var initial=p.selectDescriptors(sample.source(),page(link(1,"hwp"))).descriptors().getFirst().selectRequest();
        var redirect=new Request(URI.create("https://eminwon.gunwi.go.kr/emwp/jsp/ofr/FileDown.jsp?user_file_nm=notice.hwp&sys_file_nm=stored.hwp&file_path=%2Fntishome%2Ffile%2Fupload%2Fofr%2Fofr%2F20260901"),"GET",Map.of());
        assertThat(p.selectApprovedRequest(initial,initial)).isTrue();assertThat(p.selectApprovedRequest(initial,redirect)).isTrue();
        assertThat(p.selectApprovedRequest(redirect,initial)).isFalse();
        assertThat(p.selectApprovedRequest(new Request(p.selectDetailUri(sample.source()),"GET",Map.of()),redirect)).isFalse();
        assertThat(p.selectApprovedRequest(initial,p.selectDescriptors(sample.source(),page(link(2,"pdf"))).descriptors().getFirst().selectRequest())).isFalse();
        assertThat(p.selectApprovedRequest(new Request(initial.uri(),"POST",Map.of("x","y")))).isFalse();
        for(String bad:List.of("https:/path",initial.uri()+"&extra=1",initial.uri()+"&file_seq=2",initial.uri().toString().replace("https:","http:"),initial.uri().toString().replace("www.gunwi.go.kr","evil.example"),redirect.uri().toString().replace("ntishome","etc")))assertThat(p.selectApprovedRequest(URI.create(bad))).isFalse();
    }
    @Test void titleBodyAndBudgetBoundaries(){
        var doc=Jsoup.parse(page(link(1,"hwp")));assertThat(GunwiNoticePage.selectContent(doc).text()).isEqualTo("소상공인 지원금");
        AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(doc,"소상공인 지원",sample.titleLayout());
        assertThatThrownBy(()->GunwiNoticePage.selectContent(Jsoup.parse(doc.toString()+doc))).isInstanceOf(IllegalArgumentException.class);
        var budget=new AnnouncementAttachmentBbsOfficialObservationTest.Budget(sample.profile(),false);assertThat(budget.maximumRequests).isEqualTo(6);assertThat(budget.maximumBytes).isEqualTo(23L*1024*1024);
    }
    @Test @EnabledIfEnvironmentVariable(named="SANEB_GUNWI_SURVEY_FIXTURE",matches="true")
    void actualOfficialHtmlMatchesProfile() throws Exception {
        String html=Files.readString(Path.of("build/qa-gunwi-20260930/detail.html"));var doc=Jsoup.parse(html);
        AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(doc,sample.title(),sample.titleLayout());
        var r=sample.profile().selectDescriptors(sample.source(),html);assertThat(r.complete()).as("%s %s",r.status(),r.warnings()).isTrue();
        assertThat(r.descriptors()).hasSize(1).allSatisfy(d->{assertThat(d.downloadAllowed()).isTrue();assertThat(d.expectedFormat()).isEqualTo("HWP");});
        assertThat(GunwiNoticePage.selectContent(doc).text()).isNotBlank();
    }
    @Test void catalogIsReferenceNotApproval() throws Exception {
        var mapper=new com.fasterxml.jackson.databind.ObjectMapper();var notices=mapper.readTree(Files.readString(Path.of("src/main/resources/announcement-attachment/provider-qa-catalog-v2.json"))).path("notices");
        var ref=StreamSupport.stream(notices.spliterator(),false).filter(r->sample.code().equals(r.path("caseCode").asText())).findFirst().orElseThrow();
        assertThat(ref.path("source")).isEqualTo(mapper.valueToTree(sample.source()));assertThat(ref.hasNonNull("expectation")).isFalse();
    }
}
