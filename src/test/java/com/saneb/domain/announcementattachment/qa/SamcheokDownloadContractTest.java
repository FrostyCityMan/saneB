package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import com.saneb.domain.announcementattachment.discovery.AttachmentDiscoveryProfile;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.provider.content.SamcheokNoticePage;
import com.saneb.domain.announcementattachment.worker.AttachmentFileTypeValidator;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Download;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

class SamcheokDownloadContractTest {
    private final AnnouncementAttachmentBbsOfficialObservationTest.ObservationCase sample = SamcheokDownloadCases.selectCase();
    private String proxy(String inner, String name) { return "/DownloadEx.do?url=" + URLEncoder.encode(inner, StandardCharsets.UTF_8) + "&name=" + URLEncoder.encode(name, StandardCharsets.UTF_8); }
    private String file(int id, String ext) { return "http://eminwon.samcheok.go.kr/emwp/jsp/ofr/FileDown.jsp?user_file_nm=file" + id + "." + ext + "&sys_file_nm=saved" + id + "." + ext + "&file_path=/ntishome/file/upload/ofr/ofr/20260101"; }
    private String item(int id, String ext) { return "<li><a href='" + proxy(file(id,ext),"file" + id + "." + ext) + "'>file" + id + "." + ext + "</a></li>"; }
    private String page(String items) {
        return "<form id=saeolGosiVO name=saeolGosiVO method=get><div class=bbs1view1><h1 class=h1>소상공인 지원</h1>"
                + "<div class=info1>수출 담당부서</div><div class=attach1><ul>" + items + "</ul></div><div class=substance>소상공인 지원금</div></div></form>";
    }
    @Test void officialProxyFilesAndPartialFailuresStaySeparate() {
        var p=sample.profile();String good=item(1,"pdf")+item(2,"hwp")+item(3,"hwpx");
        var result=p.selectDescriptors(sample.source(),"<nav><a href='/DownloadEx.do'>기타 링크</a></nav>"+page(good));
        assertThat(result.complete()).isTrue();assertThat(result.descriptors()).hasSize(3).allSatisfy(d->{
            assertThat(d.downloadAllowed()).isTrue();assertThat(d.documentRole()).isEqualTo("UNKNOWN");
            assertThat(d.fetchUri().getHost()).isEqualTo("www.samcheok.go.kr");assertThat(p.selectApprovedRequest(d.selectRequest(),d.selectRequest())).isTrue();
            assertThat(d.locator().identifiers()).containsEntry("noticeId","36177");
        });
        for(String bad:List.of("<a href='https://evil.example/file.pdf'>공고.pdf</a>","<script>loadFiles()</script>","확인하지 못한 목록",
                item(4,"hwp").replace("eminwon.samcheok.go.kr","127.0.0.1"),item(4,"pdf").replace(">file4.pdf<",">other.pdf<"))){
            var partial=p.selectDescriptors(sample.source(),page(good+bad));assertThat(partial.complete()).isFalse();assertThat(partial.descriptors()).hasSize(3);
        }
        var unsupported=p.selectDescriptors(sample.source(),page(good+item(4,"xlsx")));
        assertThat(unsupported.complete()).isTrue();assertThat(unsupported.descriptors().getLast().downloadAllowed()).isFalse();
    }
    @Test void duplicateConflictMissingAreaAndLimitRemainVisible() {
        var p=sample.profile();String one=item(1,"hwp");
        assertThat(p.selectDescriptors(sample.source(),page(one+one)).descriptors()).hasSize(1);
        assertThat(p.selectDescriptors(sample.source(),page(one+one.replace("file1.hwp","other.hwp"))).complete()).isFalse();
        assertThat(p.selectDescriptors(sample.source(),page("")).status()).isEqualTo("NO_FILES");
        assertThat(p.selectDescriptors(sample.source(),"<p>첨부 없음</p>").status()).isEqualTo("FAILED");
        var limited=p.selectDescriptors(sample.source(),page(IntStream.rangeClosed(1,11).mapToObj(i->item(i,"pdf")).collect(Collectors.joining())));
        assertThat(limited.status()).isEqualTo("LIMIT_EXCEEDED");assertThat(limited.descriptors()).hasSize(10);
    }
    @Test void internalParsingUriAndNestedRemoteUrlsCannotBeFetched() {
        var p=sample.profile();var n=new AnnouncementSourceIdentityNormalizer();String url=sample.source().sourceUrl();
        for(String bad:List.of(url.replace("mgtNo","not_ancmt_mgt_no"),url+"&mgtNo=1",url+"&extra=1",url.replace("cd=01","cd=02"),url.replace("https:","http:"),url.replace("amode=view","amode=delete"))){
            assertThat(p.selectApprovedRequest(URI.create(bad))).isFalse();
            assertThatThrownBy(()->p.selectDetailUri(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(bad)),bad,"LGS-000123","SCMS_CARD_NOTICE"))).hasMessage("PROFILE_REQUIRED");
        }
        assertThat(p.selectApprovedRequest(URI.create("https://www.samcheok.go.kr/media/00084/00095.web?amode=view&not_ancmt_mgt_no=36177"))).isFalse();
        for(String inner:List.of(file(1,"pdf").replace("eminwon.samcheok.go.kr","evil.example"),file(1,"pdf").replace("/FileDown.jsp","/other.jsp"),file(1,"pdf")+"&extra=1")){
            assertThat(p.selectApprovedRequest(URI.create("https://www.samcheok.go.kr"+proxy(inner,"file1.pdf")))).isFalse();
        }
        assertThatThrownBy(()->p.selectDetailUri(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE","wrong",url,"LGS-000123","SCMS_CARD_NOTICE"))).hasMessage("PROFILE_REQUIRED");
        assertThatThrownBy(()->p.selectDetailUri(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",sample.source().providerNoticeId(),url,"LGS-000127","SCMS_CARD_NOTICE"))).hasMessage("PROFILE_REQUIRED");
        assertThat(p.selectApprovedRequest(URI.create(file(1,"pdf")))).isFalse();assertThat(p.selectApprovedRequest(URI.create("https:opaque"))).isFalse();
        var first=p.selectDescriptors(sample.source(),page(item(1,"hwp"))).descriptors().getFirst().selectRequest();
        assertThat(p.selectApprovedRequest(first,Request.selectGet(URI.create(url)))).isFalse();
        assertThat(p.selectApprovedRequest(new Request(first.uri(),"POST",Map.of("x","y")))).isFalse();
    }
    @Test void bodyTitleAndBudgetContract() {
        var doc=Jsoup.parse(page(item(1,"hwp")));
        assertThat(SamcheokNoticePage.selectContent(doc).text()).isEqualTo("소상공인 지원금");
        AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(doc,"소상공인 지원",sample.titleLayout());
        assertThatThrownBy(()->SamcheokNoticePage.selectContent(Jsoup.parse(doc.toString()+doc))).isInstanceOf(IllegalArgumentException.class);
        var budget=new AnnouncementAttachmentBbsOfficialObservationTest.Budget(sample.profile(),false);
        assertThat(budget.maximumRequests).isEqualTo(6);assertThat(budget.maximumBytes).isEqualTo(23L*1024*1024);
    }
    @Test @EnabledIfEnvironmentVariable(named="SANEB_SAMCHEOK_SURVEY_FIXTURE",matches="true")
    void actualHtmlContract() throws Exception {
        String html=Files.readString(Path.of("build/qa-samcheok-20260930/samcheok-detail.html"));var doc=Jsoup.parse(html);
        AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(doc,sample.title(),sample.titleLayout());
        var result=sample.profile().selectDescriptors(sample.source(),html);
        assertThat(result.complete()).as("%s %s",result.status(),result.warnings()).isTrue();
        assertThat(result.descriptors()).hasSize(1).allSatisfy(d->assertThat(d.downloadAllowed()).isTrue());
        assertThat(SamcheokNoticePage.selectContent(doc).text()).isNotBlank();
        var binary=Path.of("build/qa-samcheok-20260930/samcheok-probe.hwp");
        var headers=Files.readAllLines(Path.of("build/qa-samcheok-20260930/samcheok-probe.headers"),StandardCharsets.ISO_8859_1);
        var types=headers.stream().filter(h->h.toLowerCase(Locale.ROOT).startsWith("content-type:")).map(h->h.substring(h.indexOf(':')+1).strip()).toList();
        var dispositions=headers.stream().filter(h->h.toLowerCase(Locale.ROOT).startsWith("content-disposition:")).map(h->h.substring(h.indexOf(':')+1).strip()).toList();
        assertThat(types).hasSize(1);assertThat(dispositions).hasSize(1);
        var response=new Download(Files.size(binary),"a".repeat(64),types.getFirst(),dispositions.getFirst());
        assertThat(new AttachmentFileTypeValidator().selectFormat(binary,response,"HWP",sample.profile().selectUtf8DispositionOctets(),sample.profile().selectLegacyBinaryContentTypes())).isEqualTo("HWP");
    }
    @Test void legacyMimeStillRequiresSignatureExpectedFormatAndAttachmentDisposition() throws Exception {
        var p=sample.profile();var v=new AttachmentFileTypeValidator();
        assertThat(p.selectLegacyBinaryContentTypes()).containsExactly("application/x-msdownload");assertThat(p.selectUtf8DispositionOctets()).isTrue();
        var file=Files.createTempFile("saneb-samcheok-signature-",".bin");
        try {
            Files.write(file,new byte[]{(byte)0xd0,(byte)0xcf,0x11,(byte)0xe0,(byte)0xa1,(byte)0xb1,0x1a,(byte)0xe1});
            String header=new String("attachment; filename=\"합성 공고.hwp\"".getBytes(StandardCharsets.UTF_8),StandardCharsets.ISO_8859_1);
            var response=new Download(8,"a".repeat(64),"application/x-msdownload",header);
            assertThat(v.selectFormat(file,response,"HWP",true,p.selectLegacyBinaryContentTypes())).isEqualTo("HWP");
            assertThatThrownBy(()->v.selectFormat(file,response,"HWP",false,Set.of())).hasMessage("ATTACHMENT_CONTENT_TYPE_MISMATCH");
            assertThatThrownBy(()->v.selectFormat(file,response,null,true,p.selectLegacyBinaryContentTypes())).hasMessage("ATTACHMENT_CONTENT_TYPE_MISMATCH");
            assertThatThrownBy(()->v.selectFormat(file,response,"PDF",true,p.selectLegacyBinaryContentTypes())).hasMessage("ATTACHMENT_FORMAT_MISMATCH");
            for(String bad:List.of("inline; filename=\"공고.hwp\"","attachment","attachment; filename=\"../공고.hwp\""))assertThatThrownBy(()->v.selectFormat(file,new Download(8,"a".repeat(64),"application/x-msdownload",bad),"HWP",true,p.selectLegacyBinaryContentTypes())).hasMessage("ATTACHMENT_DISPOSITION_INVALID");
            Files.writeString(file,"<html>error</html>");assertThatThrownBy(()->v.selectFormat(file,response,"HWP",true,p.selectLegacyBinaryContentTypes())).hasMessage("ATTACHMENT_SIGNATURE_UNSUPPORTED");
        } finally {Files.deleteIfExists(file);}
    }
    @Test void referenceIsNotApprovedExpectation() throws Exception {
        var mapper=new com.fasterxml.jackson.databind.ObjectMapper();var notices=mapper.readTree(Files.readString(Path.of("src/main/resources/announcement-attachment/provider-qa-catalog-v2.json"))).path("notices");
        var ref=StreamSupport.stream(notices.spliterator(),false).filter(r->sample.code().equals(r.path("caseCode").asText())).findFirst().orElseThrow();
        assertThat(ref.path("source")).isEqualTo(mapper.valueToTree(sample.source()));assertThat(ref.hasNonNull("expectation")).isFalse();
    }
}
