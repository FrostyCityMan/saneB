package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import com.saneb.domain.announcementattachment.discovery.AttachmentDiscoveryProfile;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.provider.content.DaeguSeoguNoticePage;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

class DaeguSeoguDownloadContractTest {
    private final AnnouncementAttachmentBbsOfficialObservationTest.ObservationCase sample = DaeguSeoguDownloadCases.selectCase();
    @Test void currentAndPastMenusShareOneBindingWithoutRedirectingBetweenThem() {
        var p=sample.profile();var n=new AnnouncementSourceIdentityNormalizer();
        assertThat(p.selectSourceBindings()).containsExactly(new AttachmentDiscoveryProfile.SourceBinding("LGS-000047","SPRING_BBS"));
        String past=sample.source().sourceUrl(),current=past.replace("0601020200","0601020100");
        for(String url:List.of(current,past)){
            var source=new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(url)),url,"LGS-000047","SPRING_BBS");
            assertThat(p.selectDetailUri(source)).isEqualTo(URI.create(url));
            var result=p.selectDescriptors(source,page(link(1,"hwp")));
            assertThat(result.complete()).isTrue();assertThat(result.descriptors()).hasSize(1);
            assertThat(DaeguSeoguNoticePage.selectContent(Jsoup.parse(page("")),URI.create(url)).text()).isEqualTo("소상공인 지원금");
        }
        assertThat(p.selectApprovedRequest(Request.selectGet(URI.create(current)),Request.selectGet(URI.create(past)))).isFalse();
        assertThatThrownBy(()->p.selectDetailUri((AttachmentDiscoveryProfile.Source)null)).hasMessage("PROFILE_REQUIRED");
    }
    private String link(int id,String ext) {
        return "<a href='#' onclick=\"goDownload('공고"+id+"."+ext+"','저장"+id+"."+ext+"','/ntishome/file/upload/ofr/ofr/20260901'); return false;\"><span>공고"+id+"."+ext+"</span></a>";
    }
    private String page(String links) {
        return "<form id=detailForm name=detailForm method=post><div class=bod_view><div class=subject>소상공인 지원</div>"
                +"<div class=view_info>수출 담당부서</div><div class=view_cont>소상공인 지원금</div>"
                +"<dl class=view_file><dt><span>첨부 파일</span></dt><dd><div><ul id=updateFileList class=file-list>"+links+"</ul></div></dd></dl></div></form>";
    }
    @Test void sharedEngineRetainsSuccessfulFilesAndIndependentErrors() {
        var p=sample.profile();String good=link(1,"pdf")+link(2,"hwp")+link(3,"hwpx");
        var r=p.selectDescriptors(sample.source(),page(good));assertThat(r.complete()).isTrue();
        assertThat(r.descriptors()).hasSize(3).allSatisfy(d->{assertThat(d.downloadAllowed()).isTrue();assertThat(d.documentRole()).isEqualTo("UNKNOWN");assertThat(p.selectApprovedRequest(d.selectRequest(),d.selectRequest())).isTrue();});
        for(String bad:List.of("<a href='/unknown'>미확인</a>","<script>loadFiles()</script>","<button>파일</button>",
                link(4,"hwp").replace("return false;","alert(1);return false;"),link(4,"pdf").replace("/ntishome/","/other/"))){
            var partial=p.selectDescriptors(sample.source(),page(good+bad));assertThat(partial.complete()).isFalse();assertThat(partial.descriptors()).hasSize(3);
        }
        var unsupported=p.selectDescriptors(sample.source(),page(good+link(4,"xlsx")));
        assertThat(unsupported.complete()).isTrue();assertThat(unsupported.descriptors().getLast().downloadAllowed()).isFalse();
        assertThat(p.selectDescriptors(sample.source(),page(good)+"<a href='/outside.pdf'>다른 링크</a>").complete()).isTrue();
    }
    @Test void absenceConflictsAndLimitsRemainSeparate() {
        var p=sample.profile();String one=link(1,"hwp");
        assertThat(p.selectDescriptors(sample.source(),page("")).status()).isEqualTo("NO_FILES");
        assertThat(p.selectDescriptors(sample.source(),"<p>오류</p>").complete()).isFalse();
        assertThat(p.selectDescriptors(sample.source(),page(one+one)).descriptors()).hasSize(1);
        assertThat(p.selectDescriptors(sample.source(),page(one+one.replace("공고1.hwp","변경.hwp"))).complete()).isFalse();
        var limited=p.selectDescriptors(sample.source(),page(IntStream.rangeClosed(1,11).mapToObj(i->link(i,"pdf")).collect(Collectors.joining())));
        assertThat(limited.status()).isEqualTo("LIMIT_EXCEEDED");assertThat(limited.descriptors()).hasSize(10);
    }
    @Test void sourceAndRequestBoundariesRejectOtherHostsMenusAndRedirects() {
        var p=sample.profile();var n=new AnnouncementSourceIdentityNormalizer();String url=sample.source().sourceUrl();
        for(String bad:List.of(url+"&extra=1",url+"&notAncmtMgtNo=1",url.replace("mid=0601020200","mid=0601030000"),url.replace("https:","http:"),url.replace("www.dgs.go.kr","127.0.0.1"))){
            assertThatThrownBy(()->p.selectDetailUri(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(bad)),bad,"LGS-000047","SPRING_BBS"))).hasMessage("PROFILE_REQUIRED");
        }
        assertThatThrownBy(()->p.selectDetailUri(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE","wrong",url,"LGS-000047","SPRING_BBS"))).hasMessage("PROFILE_REQUIRED");
        assertThatThrownBy(()->p.selectDetailUri(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",sample.source().providerNoticeId(),url,"LGS-000048","SPRING_BBS"))).hasMessage("PROFILE_REQUIRED");
        var initial=p.selectDescriptors(sample.source(),page(link(1,"hwp"))).descriptors().getFirst().selectRequest();
        var other=p.selectDescriptors(sample.source(),page(link(2,"hwp"))).descriptors().getFirst().selectRequest();
        assertThat(p.selectApprovedRequest(initial,other)).isFalse();
        assertThatThrownBy(()->new Request(initial.uri(),"POST",Map.of())).hasMessage("ATTACHMENT_REQUEST_INVALID");
        assertThat(p.selectApprovedRequest(new Request(initial.uri(),"POST",Map.of("x","y")))).isFalse();
        for(String bad:List.of(initial.uri()+"&extra=1",initial.uri().toString().replace("eminwon.dgs.go.kr","evil.example"),initial.uri().toString().replace("https:","http:")))assertThat(p.selectApprovedRequest(URI.create(bad))).isFalse();
    }
    @Test void bodyTitleAndBudgetAreBounded() {
        var document=Jsoup.parse(page(link(1,"hwp")));URI uri=URI.create(sample.source().sourceUrl());
        assertThat(DaeguSeoguNoticePage.selectContent(document,uri).text()).isEqualTo("소상공인 지원금");
        AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(document,"소상공인 지원",sample.titleLayout());
        assertThatThrownBy(()->DaeguSeoguNoticePage.selectContent(Jsoup.parse(document.toString()+document),uri)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->DaeguSeoguNoticePage.selectContent(document,URI.create(uri+"&extra=1"))).isInstanceOf(IllegalArgumentException.class);
        var budget=new AnnouncementAttachmentBbsOfficialObservationTest.Budget(sample.profile(),false);
        assertThat(budget.maximumRequests).isEqualTo(6);assertThat(budget.maximumBytes).isEqualTo(23L*1024*1024);
    }
    @Test @EnabledIfEnvironmentVariable(named="SANEB_DAEGU_SEOGU_SURVEY_FIXTURE",matches="true")
    void actualOfficialHtmlMatchesSharedEngine() throws Exception {
        String html=Files.readString(Path.of("build/qa-daegu-seogu-20260930/detail.html"));var doc=Jsoup.parse(html);
        AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(doc,sample.title(),sample.titleLayout());
        var result=sample.profile().selectDescriptors(sample.source(),html);
        assertThat(result.complete()).as("%s %s",result.status(),result.warnings()).isTrue();
        assertThat(result.descriptors()).hasSize(1).allSatisfy(d->{assertThat(d.downloadAllowed()).isTrue();assertThat(d.expectedFormat()).isEqualTo("HWP");});
        assertThat(DaeguSeoguNoticePage.selectContent(doc,URI.create(sample.source().sourceUrl())).text()).isNotBlank();
    }
    @Test void catalogIsReferenceNotPolicyApproval() throws Exception {
        var mapper=new com.fasterxml.jackson.databind.ObjectMapper();var notices=mapper.readTree(Files.readString(Path.of("src/main/resources/announcement-attachment/provider-qa-catalog-v2.json"))).path("notices");
        var ref=StreamSupport.stream(notices.spliterator(),false).filter(r->sample.code().equals(r.path("caseCode").asText())).findFirst().orElseThrow();
        assertThat(ref.path("source")).isEqualTo(mapper.valueToTree(sample.source()));assertThat(ref.hasNonNull("expectation")).isFalse();
    }
}
