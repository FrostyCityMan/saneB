package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import com.saneb.domain.announcementattachment.discovery.AttachmentDiscoveryProfile;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.provider.content.AndongNoticePage;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

class AndongDownloadContractTest {
    private final AnnouncementAttachmentBbsOfficialObservationTest.ObservationCase sample=AndongDownloadCases.selectCase();
    private String args(int id,String ext){return "'공고"+id+"."+ext+"','저장"+id+"."+ext+"','/ntishome/file/upload/ofr/ofr/20260901'";}
    private String link(int id,String ext){return "<li><a href=\"javascript:goDownload("+args(id,ext)+")\">공고"+id+"."+ext+"</a><a href='#' class=btn_fileview onclick=\"fn_egov_gosi_preview('63386','63386-"+id+"."+ext+"',"+args(id,ext)+"); return false;\"><img src='/mayor/images/board/btn_fileview.png' alt=바로보기></a></li>";}
    private String page(String links){return "<form id=detailForm name=detailForm method=post></form><h4 class=hidden>전체 게시판 내용보기</h4><table class=bod_view><tr><th scope=col colspan=4 class=title>소상공인 지원</th></tr><tr><th scope=row>담당부서</th><td>수출 부서</td></tr><tr><th scope=row class=list_file>첨부파일</th><td colspan=3 class=box_file><ul class=list_file>"+links+"</ul></td></tr><tr><td colspan=4 class=cont><div class=cont_box>소상공인 지원금</div></td></tr></table>";}
    @Test void filesAndPairedPreviewsAreDistinct(){
        var p=sample.profile();String good=link(1,"hwp")+link(2,"pdf")+link(3,"hwpx");var r=p.selectDescriptors(sample.source(),page(good));assertThat(r.complete()).isTrue();assertThat(r.descriptors()).hasSize(3).allSatisfy(d->{assertThat(d.downloadAllowed()).isTrue();assertThat(d.documentRole()).isEqualTo("UNKNOWN");assertThat(p.selectApprovedRequest(d.selectRequest())).isTrue();});
        for(String bad:List.of("<a href='/unknown'>미확인</a>","<script>loadFiles()</script>","<button>첨부</button>",link(4,"hwp").replace("/ntishome/","/other/"),link(4,"pdf").replace("javascript:goDownload(","javascript:eval("))){var partial=p.selectDescriptors(sample.source(),page(good+bad));assertThat(partial.complete()).isFalse();assertThat(partial.descriptors()).hasSize(3);}
        assertThat(p.selectDescriptors(sample.source(),page(good)+"<a href='/outside.pdf'>다른 링크</a>").descriptors()).hasSize(3);
        var unsupported=p.selectDescriptors(sample.source(),page("<li><a href=\"javascript:goDownload("+args(1,"xlsx")+")\">공고1.xlsx</a></li>"));assertThat(unsupported.complete()).isTrue();assertThat(unsupported.descriptors().getFirst().downloadAllowed()).isFalse();
    }
    @Test void mismatchedPreviewCannotHideDiscoveryErrorOrDiscardFile(){
        var p=sample.profile();for(String bad:List.of(link(1,"hwp").replace("'63386-1.hwp'","'99999-1.hwp'"),link(1,"hwp").replace("return false;","alert(1);return false;"),link(1,"hwp").replace("alt=바로보기","alt=다른이미지"))){var r=p.selectDescriptors(sample.source(),page(bad));assertThat(r.complete()).isFalse();assertThat(r.descriptors()).hasSize(1);}
    }
    @Test void absenceConflictsAndLimitsAreIndependent(){
        var p=sample.profile();String one=link(1,"hwp");assertThat(p.selectDescriptors(sample.source(),page("")).status()).isEqualTo("NO_FILES");assertThat(p.selectDescriptors(sample.source(),"<p>오류</p>").status()).isEqualTo("FAILED");assertThat(p.selectDescriptors(sample.source(),page("<li></li>")).status()).isEqualTo("FAILED");
        assertThat(p.selectDescriptors(sample.source(),page(one+one)).descriptors()).hasSize(1);assertThat(p.selectDescriptors(sample.source(),page(one+one.replace("공고1.hwp","다른.hwp"))).complete()).isFalse();
        var limited=p.selectDescriptors(sample.source(),page(IntStream.rangeClosed(1,11).mapToObj(i->link(i,"pdf")).collect(Collectors.joining())));assertThat(limited.status()).isEqualTo("LIMIT_EXCEEDED");assertThat(limited.descriptors()).hasSize(10);
    }
    @Test void sourceAndRequestsRejectOtherMenusAndRedirects(){
        var p=sample.profile();var n=new AnnouncementSourceIdentityNormalizer();String url=sample.source().sourceUrl();
        for(String bad:List.of(url+"&extra=1",url+"&notAncmtMgtNo=1",url.replace("0401020100","0401020200"),url.replace("isLinkage=Y","isLinkage=N"),url.replace("https:","http:"),url.replace("www.andong.go.kr","127.0.0.1"))){assertThatThrownBy(()->p.selectDetailUri(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(bad)),bad,"LGS-000204","SPRING_BBS"))).hasMessage("PROFILE_REQUIRED");}
        assertThatThrownBy(()->p.selectDetailUri(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE","wrong",url,"LGS-000204","SPRING_BBS"))).hasMessage("PROFILE_REQUIRED");
        assertThatThrownBy(()->p.selectDetailUri(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",sample.source().providerNoticeId(),url,"LGS-000201","SAEOL_GOSI"))).hasMessage("PROFILE_REQUIRED");
        var r=p.selectDescriptors(sample.source(),page(link(1,"hwp"))).descriptors().getFirst().selectRequest();assertThat(p.selectApprovedRequest(r,r)).isTrue();assertThat(p.selectApprovedRequest(r,p.selectDescriptors(sample.source(),page(link(2,"hwp"))).descriptors().getFirst().selectRequest())).isFalse();assertThat(p.selectApprovedRequest(new Request(r.uri(),"POST",Map.of("x","y")))).isFalse();
        for(String bad:List.of("https:/path",r.uri()+"&extra=1",r.uri().toString().replace("https:","http:"),r.uri().toString().replace("eminwon.andong.go.kr","evil.example")))assertThat(p.selectApprovedRequest(URI.create(bad))).isFalse();
    }
    @Test void bodyTitleAndBudgetBoundaries(){var doc=Jsoup.parse(page(link(1,"hwp")));assertThat(AndongNoticePage.selectContent(doc).text()).isEqualTo("소상공인 지원금");AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(doc,"소상공인 지원",sample.titleLayout());assertThatThrownBy(()->AndongNoticePage.selectContent(Jsoup.parse(page("")+page("")))).isInstanceOf(IllegalArgumentException.class);var b=new AnnouncementAttachmentBbsOfficialObservationTest.Budget(sample.profile(),false);assertThat(b.maximumRequests).isEqualTo(6);assertThat(b.maximumBytes).isEqualTo(23L*1024*1024);}
    @Test @EnabledIfEnvironmentVariable(named="SANEB_ANDONG_SURVEY_FIXTURE",matches="true")
    void actualOfficialHtmlMatchesProfile() throws Exception {String html=Files.readString(Path.of("build/qa-andong-20260930/detail.html"));var doc=Jsoup.parse(html);AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(doc,sample.title(),sample.titleLayout());var r=sample.profile().selectDescriptors(sample.source(),html);assertThat(r.complete()).as("%s %s",r.status(),r.warnings()).isTrue();assertThat(r.descriptors()).hasSize(3).allSatisfy(d->assertThat(d.downloadAllowed()).isTrue());assertThat(r.descriptors()).extracting(AttachmentDiscoveryProfile.Descriptor::expectedFormat).containsExactly("HWPX","HWPX","PDF");assertThat(AndongNoticePage.selectContent(doc).text()).isNotBlank();}
    @Test void catalogRemainsReferenceOnly() throws Exception {var mapper=new com.fasterxml.jackson.databind.ObjectMapper();var notices=mapper.readTree(Files.readString(Path.of("src/main/resources/announcement-attachment/provider-qa-catalog-v2.json"))).path("notices");var ref=StreamSupport.stream(notices.spliterator(),false).filter(r->sample.code().equals(r.path("caseCode").asText())).findFirst().orElseThrow();assertThat(ref.path("source")).isEqualTo(mapper.valueToTree(sample.source()));assertThat(ref.hasNonNull("expectation")).isFalse();}
}
