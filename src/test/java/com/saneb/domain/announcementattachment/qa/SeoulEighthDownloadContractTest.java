package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Download;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.provider.content.SeoulEighthNoticePage;
import com.saneb.domain.announcementsource.provider.content.SeoulEighthNoticePage.Site;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class SeoulEighthDownloadContractTest {
    @TempDir Path temporary;
    private String item(Site site,int number,String ext){
        if(site==Site.DOBONG)return "<div class=file_list><a class=file_name title='공고."+ext+" 다운로드' href='javascript:filedown("+number+", 4734);'>공고."+ext+" (78 KB)</a></div>";
        return "<li><a href='https://gangnam.eminwon.seoul.kr/emwp/jsp/ofr/FileDown_gn.jsp?user_file_nm=공고."+ext+"&sys_file_nm=공고_ofr_ofr_ABC123_20260729160040444_"+number+"."+ext+"&file_path=/ntishome/file/upload/ofr/ofr/20260729'>공고."+ext+"</a></li>";
    }
    private String page(Site site,String items){
        if(site==Site.DOBONG)return "<div class=bbsView><table class=boardView><tbody><tr><td class=title>청년 지원금</td></tr><tr><th>첨부파일</th><td class=file>"+items+"</td></tr></tbody></table><div class=bbsCont>청년 지원금 본문</div></div>";
        return "<div class='board view'><div class=bbs-view><div class=post-title>중소기업 지원<br>일반공고 번호</div><div class=post-content>중소기업 지원금 본문</div><div class=bbs-view-file><ul id=fileListCollap class=view-file-list>"+items+"</ul></div></div></div>";
    }
    @ParameterizedTest @EnumSource(Site.class) void sourceAndCanonicalDetailAreBound(Site site){
        var sample=SeoulEighthDownloadCases.selectCase(site.name());var p=sample.profile();var n=new AnnouncementSourceIdentityNormalizer();
        assertThat(p.selectDetailUri(sample.source()).toString()).isEqualTo(sample.source().sourceUrl());
        for(String url:List.of(sample.source().sourceUrl()+"&unknown=1",sample.source().sourceUrl()+"&"+site.id+"=99",sample.source().sourceUrl().replace("https:","http:"),sample.source().sourceUrl().replace(site.host,"127.0.0.1")))
            assertThatThrownBy(()->p.selectDetailUri(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(url)),url,site.source,site.parser))).hasMessage("PROFILE_REQUIRED");
        assertThatThrownBy(()->p.selectDetailUri(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE","wrong",sample.source().sourceUrl(),site.source,site.parser))).hasMessage("PROFILE_REQUIRED");
        assertThatThrownBy(()->p.selectDetailUri(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",sample.source().providerNoticeId(),sample.source().sourceUrl(),"LGS-000999",site.parser))).hasMessage("PROFILE_REQUIRED");
    }
    @ParameterizedTest @EnumSource(Site.class) void knownAreaSeparatesTitleBodyAndFiles(Site site){
        var sample=SeoulEighthDownloadCases.selectCase(site.name());String html=page(site,item(site,1,"pdf"));var document=Jsoup.parse(html);
        assertThat(SeoulEighthNoticePage.selectTitle(SeoulEighthNoticePage.selectRoot(document,site),site)).isEqualTo(site==Site.GANGNAM?"중소기업 지원":"청년 지원금");
        assertThat(SeoulEighthNoticePage.selectContent(document,URI.create(sample.source().sourceUrl())).text()).endsWith("지원금 본문");
        var result=sample.profile().selectDescriptors(sample.source(),"<a href='/other.pdf'>다른 파일</a>"+html);assertThat(result.complete()).isTrue();assertThat(result.descriptors()).hasSize(1);
        for(String bad:List.of(html+html,html.replace("첨부파일","다른 영역").replace("bbs-view-file","other"),html.replace("fileListCollap","unknown").replace("class=file>","class=other>"))){assertThat(sample.profile().selectDescriptors(sample.source(),bad).complete()).isFalse();}
        assertThat(sample.profile().selectDescriptors(sample.source(),page(site,"")).status()).isEqualTo("NO_FILES");
    }
    @ParameterizedTest @EnumSource(Site.class) void partialUnknownAndUnsupportedFilesRemainSeparate(Site site){
        var sample=SeoulEighthDownloadCases.selectCase(site.name());var p=sample.profile();String good=item(site,1,"pdf")+item(site,2,"hwpx");
        for(String bad:List.of("<a href='https://other.example/file.pdf'>미확인</a>","<script>alert(1)</script>",item(site,3,"pdf").replace("4734","999").replace("gangnam.eminwon.seoul.kr","other.example"))){
            var result=p.selectDescriptors(sample.source(),page(site,good+bad));assertThat(result.complete()).isFalse();assertThat(result.descriptors()).hasSize(2);
        }
        var result=p.selectDescriptors(sample.source(),page(site,good+item(site,3,"png")));assertThat(result.complete()).isTrue();assertThat(result.descriptors().getLast().downloadAllowed()).isFalse();
        assertThat(result.descriptors()).allSatisfy(d->assertThat(d.documentRole()).isEqualTo("UNKNOWN"));
        assertThat(p.selectDescriptors(sample.source(),page(site,good+item(site,1,"pdf"))).descriptors()).hasSize(2);
        var limit=p.selectDescriptors(sample.source(),page(site,IntStream.rangeClosed(1,11).mapToObj(i->item(site,i,"pdf")).collect(Collectors.joining())));
        assertThat(limit.status()).isEqualTo("LIMIT_EXCEEDED");assertThat(limit.descriptors()).hasSize(10);
    }
    @ParameterizedTest @EnumSource(Site.class) void fileRequestsCannotSwitchHostMethodOrIdentity(Site site){
        var sample=SeoulEighthDownloadCases.selectCase(site.name());var p=sample.profile();var result=p.selectDescriptors(sample.source(),page(site,item(site,1,"pdf")+item(site,2,"pdf")));
        var first=result.descriptors().getFirst().selectRequest();var second=result.descriptors().getLast().selectRequest();
        assertThat(p.selectApprovedRequest(first,first)).isTrue();assertThat(p.selectApprovedRequest(first,second)).isFalse();
        for(String url:List.of(first.uri()+"&extra=1",first.uri()+"#fragment",first.uri().toString().replace("https:","http:"),first.uri().toString().replace(first.uri().getHost(),"127.0.0.1")))assertThat(p.selectApprovedRequest(URI.create(url))).isFalse();
        assertThat(p.selectApprovedRequest(new Request(first.uri(),"POST",Map.of("idx","4734")))).isFalse();
    }
    @Test void dobongPreflightMustSucceedAndMatchNoticeBeforeDownload()throws Exception{
        var sample=SeoulEighthDownloadCases.selectCase("DOBONG");var p=sample.profile();var initial=p.selectDescriptors(sample.source(),page(Site.DOBONG,item(Site.DOBONG,1,"pdf"))).descriptors().getFirst().selectRequest();
        var foreign=new Request(URI.create("https://www.dobong.go.kr/WDB_DEV/gosigong_go/ajax_user_attach.asp"),"POST",Map.of("idx","999"));
        assertThat(p.selectApprovedRequest(initial,foreign)).isFalse();
        for(String status:List.of("OK","nodata","error","<html>로그인</html>")){
            Path output=temporary.resolve(UUID.randomUUID()+".bin");var calls=new ArrayList<Request>();
            AttachmentProfileDownloadFlow.Transport transport=(request,limit,approved)->{
                assertThat(approved.test(request)).isTrue();calls.add(request);
                if("POST".equals(request.method())){assertThat(request.form()).containsExactly(Map.entry("idx","4734"));assertThat(limit).isEqualTo(256);Files.writeString(output,status);return new Download(status.length(),"a".repeat(64),"text/html; Charset=utf-8");}
                assertThat(request).isEqualTo(initial);assertThat(Files.exists(output)).isFalse();Files.writeString(output,"%PDF-test");return new Download(9,"b".repeat(64),"application/pdf");
            };
            if("OK".equals(status)){assertThat(AttachmentProfileDownloadFlow.selectDownload(p,initial,output,1048576,transport).contentType()).isEqualTo("application/pdf");assertThat(calls).hasSize(2);Files.delete(output);}
            else{assertThatThrownBy(()->AttachmentProfileDownloadFlow.selectDownload(p,initial,output,1048576,transport)).hasMessage("ATTACHMENT_DOWNLOAD_BLOCKED");assertThat(calls).hasSize(1);assertThat(Files.exists(output)).isFalse();}
        }
    }
    @ParameterizedTest @EnumSource(Site.class) @EnabledIfEnvironmentVariable(named="SANEB_SEOUL_EIGHTH_SURVEY_FIXTURE",matches="true")
    void actualPageContract(Site site)throws Exception{
        var sample=SeoulEighthDownloadCases.selectCase(site.name());String html=Files.readString(Path.of("build/qa-seoul-next-20260930/"+site+"-detail.html"));
        assertThat(SeoulEighthNoticePage.selectTitle(SeoulEighthNoticePage.selectRoot(Jsoup.parse(html),site),site)).isEqualTo(sample.title());
        var result=sample.profile().selectDescriptors(sample.source(),html);assertThat(result.complete()).as("%s %s",result.status(),result.warnings()).isTrue();assertThat(result.descriptors()).hasSize(4).allSatisfy(d->assertThat(d.downloadAllowed()).isTrue());
    }
    @Test @EnabledIfEnvironmentVariable(named="SANEB_SEOUL_EIGHTH_SURVEY_FIXTURE",matches="true")
    void actualDobongUtf8DispositionPreservesFormatValidation()throws Exception{
        var p=SeoulEighthDownloadCases.selectCase("DOBONG").profile();
        assertThat(p).isInstanceOf(AttachmentDownloadFlowProfile.class);assertThat(p.selectUtf8DispositionOctets()).isTrue();
        assertThat(SeoulEighthDownloadCases.selectCase("GANGNAM").profile().selectUtf8DispositionOctets()).isFalse();
        Path binary=Path.of("build/qa-seoul-next-20260930/DOBONG-file.bin");
        String headers=Files.readString(Path.of("build/qa-seoul-next-20260930/DOBONG-file-head.txt"),java.nio.charset.StandardCharsets.ISO_8859_1);
        String disposition=headers.lines().filter(l->l.toLowerCase(Locale.ROOT).startsWith("content-disposition:")).findFirst().orElseThrow().split(":",2)[1].strip();
        var response=new Download(Files.size(binary),"80958d61150815ccfb16bee5f9e04daf53bdf7794aa97b7bc42a7906f3076b0e","application/octet-stream; Charset=utf-8",disposition);
        var validator=new com.saneb.domain.announcementattachment.worker.AttachmentFileTypeValidator();
        assertThatThrownBy(()->validator.selectFormat(binary,response,"HWPX",false)).hasMessage("ATTACHMENT_DISPOSITION_INVALID");
        assertThat(validator.selectFormat(binary,response,"HWPX",p.selectUtf8DispositionOctets())).isEqualTo("HWPX");
        assertThatThrownBy(()->validator.selectFormat(binary,response,"PDF",p.selectUtf8DispositionOctets())).hasMessage("ATTACHMENT_FORMAT_MISMATCH");
    }
    @ParameterizedTest @EnumSource(Site.class) void catalogAndBudgetStayCollectionOnly(Site site)throws Exception{
        var sample=SeoulEighthDownloadCases.selectCase(site.name());var mapper=new ObjectMapper();var notices=mapper.readTree(Files.readString(Path.of("src/main/resources/announcement-attachment/provider-qa-catalog-v2.json"))).path("notices");
        var ref=StreamSupport.stream(notices.spliterator(),false).filter(r->sample.code().equals(r.path("caseCode").asText())).findFirst().orElseThrow();
        assertThat(ref.path("source")).isEqualTo(mapper.valueToTree(sample.source()));assertThat(ref.hasNonNull("expectation")).isFalse();
        var budget=new AnnouncementAttachmentBbsOfficialObservationTest.Budget(sample.profile(),false);assertThat(budget.maximumRequests).isEqualTo(site==Site.DOBONG?12:8);assertThat(budget.maximumBytes).isEqualTo(43L*1024*1024);
    }
}
