package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import com.saneb.domain.announcementattachment.discovery.AttachmentDiscoveryProfile;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.provider.content.SangjuNoticePage;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

class SangjuDownloadContractTest {
    private final AnnouncementAttachmentBbsOfficialObservationTest.ObservationCase sample=SangjuDownloadCases.selectCase();
    private String link(int id,String ext){return "<a href='javascript:;' onclick=\"fnFileDown('공고"+id+"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA==','저장"+id+"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA==','/ntishoAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA');\">공고"+id+"."+ext+",</a><br>";}
    private String page(String links){return "<form id=form2 name=form2 method=post action='https://eminwon.sangju.go.kr/emwp/jsp/ofr/FileDownNew.jsp'><input type=hidden name=user_file_nm><input type=hidden name=sys_file_nm><input type=hidden name=file_path></form>"
            +"<form id=form1 name=form1 method=post><table class=comp-tbl_datatype><tr><th scope=row>제목</th><td colspan=3>소상공인 지원</td></tr><tr><th scope=row>부서</th><td>수출 부서</td></tr><tr><th scope=row>고시공고 내용</th><td colspan=3>소상공인 지원금</td></tr><tr><th scope=row>첨부파일</th><td colspan=3>"+links+"</td></tr></table></form>";}
    @Test void successfulFilesRemainWhenOneLinkFails(){
        var p=sample.profile();String good=link(1,"hwp")+link(2,"pdf")+link(3,"hwpx");var result=p.selectDescriptors(sample.source(),page(good));
        assertThat(result.complete()).isTrue();assertThat(result.descriptors()).hasSize(3).allSatisfy(d->{assertThat(d.downloadAllowed()).isTrue();assertThat(d.documentRole()).isEqualTo("UNKNOWN");assertThat(p.selectApprovedRequest(d.selectRequest())).isTrue();assertThat(d.displayName()).doesNotEndWith(",");});
        for(String bad:List.of("<a href='/unknown'>미확인</a>","<script>loadFiles()</script>","<button>첨부</button>",link(4,"pdf").replace("/ntisho","/etc"),link(4,"hwp").replace(");\"", ");alert(1);\""),link(4,"pdf").replace("fnFileDown(","eval("))){
            var partial=p.selectDescriptors(sample.source(),page(good+bad));assertThat(partial.complete()).isFalse();assertThat(partial.descriptors()).hasSize(3);
        }
        var unsupported=p.selectDescriptors(sample.source(),page(good+link(4,"xlsx")));assertThat(unsupported.complete()).isTrue();assertThat(unsupported.descriptors().getLast().downloadAllowed()).isFalse();
        assertThat(p.selectDescriptors(sample.source(),page(good)+"<a href='/outside.pdf'>외부</a>").descriptors()).hasSize(3);
    }
    @Test void missingFormsAndConflictingFilesAreNotAbsence(){
        var p=sample.profile();String one=link(1,"hwp");assertThat(p.selectDescriptors(sample.source(),page("")).status()).isEqualTo("NO_FILES");
        assertThat(p.selectDescriptors(sample.source(),"<p>오류</p>").status()).isEqualTo("FAILED");
        for(String value:List.of(page(one).replace("id=form2","id=other"),page(one).replace("eminwon.sangju.go.kr","evil.example"),page(one).replace("name=file_path","name=unknown"),page(one).replace("name=file_path","value=unexpected name=file_path"))){assertThat(p.selectDescriptors(sample.source(),value).complete()).isFalse();assertThat(p.selectDescriptors(sample.source(),value).descriptors()).isEmpty();}
        assertThat(p.selectDescriptors(sample.source(),page(one+one)).descriptors()).hasSize(1);
        assertThat(p.selectDescriptors(sample.source(),page(one+one.replace("공고1.hwp","다른.hwp"))).complete()).isFalse();
        var limited=p.selectDescriptors(sample.source(),page(IntStream.rangeClosed(1,11).mapToObj(i->link(i,"pdf")).collect(Collectors.joining())));assertThat(limited.status()).isEqualTo("LIMIT_EXCEEDED");assertThat(limited.descriptors()).hasSize(10);
    }
    @Test void sourceAndPostRequestAreNarrowlyBound(){
        var p=sample.profile();var n=new AnnouncementSourceIdentityNormalizer();String url=sample.source().sourceUrl();
        for(String bad:List.of(url+"&extra=1",url+"&mgtNo=1",url.replace("10297","10300"),url.replace("https:","http:"),url.replace("www.sangju.go.kr","127.0.0.1"))){assertThatThrownBy(()->p.selectDetailUri(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(bad)),bad,"LGS-000208","SAFE_SANGJU_GOSI"))).hasMessage("PROFILE_REQUIRED");}
        assertThatThrownBy(()->p.selectDetailUri(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE","wrong",url,"LGS-000208","SAFE_SANGJU_GOSI"))).hasMessage("PROFILE_REQUIRED");
        assertThatThrownBy(()->p.selectDetailUri(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",sample.source().providerNoticeId(),url,"LGS-000187","SPRING_BBS"))).hasMessage("PROFILE_REQUIRED");
        var request=p.selectDescriptors(sample.source(),page(link(1,"hwp"))).descriptors().getFirst().selectRequest();assertThat(p.selectApprovedRequest(request,request)).isTrue();
        assertThat(p.selectApprovedRequest(new Request(request.uri(),"GET",Map.of()))).isFalse();
        for(String bad:List.of(request.uri()+"?x=1",request.uri().toString().replace("https:","http:"),request.uri().toString().replace("eminwon.sangju.go.kr","evil.example"))){assertThat(p.selectApprovedRequest(new Request(URI.create(bad),"POST",request.form()))).isFalse();}
        var other=p.selectDescriptors(sample.source(),page(link(2,"hwp"))).descriptors().getFirst().selectRequest();assertThat(p.selectApprovedRequest(request,other)).isFalse();
        var badFields=new HashMap<>(request.form());badFields.put("extra","value");assertThat(p.selectApprovedRequest(new Request(request.uri(),"POST",badFields))).isFalse();
        assertThat(p.selectApprovedRequest(URI.create("https:/path"))).isFalse();
    }
    @Test void bodyTitleAndBudgetRemainSeparate(){
        var document=Jsoup.parse(page(link(1,"hwp")));assertThat(SangjuNoticePage.selectContent(document).text()).isEqualTo("소상공인 지원금");AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(document,"소상공인 지원",sample.titleLayout());
        assertThatThrownBy(()->SangjuNoticePage.selectContent(Jsoup.parse(page("")+page("")))).isInstanceOf(IllegalArgumentException.class);
        var budget=new AnnouncementAttachmentBbsOfficialObservationTest.Budget(sample.profile(),false);assertThat(budget.maximumRequests).isEqualTo(6);assertThat(budget.maximumBytes).isEqualTo(23L*1024*1024);
    }
    @Test @EnabledIfEnvironmentVariable(named="SANEB_SANGJU_SURVEY_FIXTURE",matches="true")
    void actualOfficialHtmlMatchesProfile() throws Exception {
        String html=Files.readString(Path.of("build/qa-sangju-20260930/detail.html"));var doc=Jsoup.parse(html);AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(doc,sample.title(),sample.titleLayout());
        var result=sample.profile().selectDescriptors(sample.source(),html);assertThat(result.complete()).as("%s %s",result.status(),result.warnings()).isTrue();assertThat(result.descriptors()).hasSize(2).allSatisfy(d->assertThat(d.downloadAllowed()).isTrue());assertThat(result.descriptors()).extracting(AttachmentDiscoveryProfile.Descriptor::expectedFormat).containsExactly("HWP","PDF");assertThat(SangjuNoticePage.selectContent(doc).text()).isNotBlank();
    }
    @Test void catalogRemainsReferenceOnly() throws Exception {
        var mapper=new com.fasterxml.jackson.databind.ObjectMapper();var notices=mapper.readTree(Files.readString(Path.of("src/main/resources/announcement-attachment/provider-qa-catalog-v2.json"))).path("notices");var ref=StreamSupport.stream(notices.spliterator(),false).filter(r->sample.code().equals(r.path("caseCode").asText())).findFirst().orElseThrow();assertThat(ref.path("source")).isEqualTo(mapper.valueToTree(sample.source()));assertThat(ref.hasNonNull("expectation")).isFalse();
    }
}
