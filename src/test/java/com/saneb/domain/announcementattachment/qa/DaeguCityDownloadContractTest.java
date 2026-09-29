package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import com.saneb.domain.announcementattachment.discovery.AttachmentDiscoveryProfile;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.provider.content.DaeguCityNoticePage;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Download;
import com.saneb.domain.announcementattachment.worker.AttachmentFileTypeValidator;
import java.nio.charset.StandardCharsets;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

class DaeguCityDownloadContractTest {
    private final AnnouncementAttachmentBbsOfficialObservationTest.ObservationCase sample = DaeguCityDownloadCases.selectCase();
    private String item(int id, String ext) {
        return "<span class=attfile><a class='link_button txt download' href=\"javascript:fn_egov_downFile('gF22022313452364361 ','" + id + "')\">공고." + ext + "&nbsp;[100&nbsp;byte]</a>"
                + "<a class='link_button ico view' href=\"javascript:filePreview('gF22022313452364361 ','" + id + "')\">미리보기</a></span>";
    }
    static String page(String items, int count) {
        return "<nav>수출 메뉴</nav><form id=sidoGosiAPIVO><input type=hidden name=sno value=33505><input type=hidden name=gosi_gbn value=A><div id=bbsView>"
                + "<div class=form_group><dl class=title><dt>제목</dt><dd>소상공인 지원</dd></dl></div>"
                + "<div class=form_group><dl><dt>담당부서</dt><dd>특허 부서</dd></dl></div>"
                + "<div class=form_group><dl class=content><dt>내용</dt><dd>소상공인 지원금</dd></dl></div>"
                + "<div class=form_group><dl class=attfile><dt>첨부파일</dt><dd><script>function filePreview(){}</script>"
                + "<input type=hidden name=atchFileId value=gF22022313452364361><input type=hidden name=fileSn>"
                + "<input type=hidden name=fileListCnt value=" + count + ">" + items + "</dd></dl></div></div></form><footer>하단 영역</footer>";
    }
    @Test void supportedFilesAndPartialFailuresAreIndependent() {
        var p = sample.profile(); String good = item(1,"pdf") + item(2,"hwp") + item(3,"hwpx");
        var result = p.selectDescriptors(sample.source(),page(good,3));
        assertThat(result.complete()).isTrue();
        assertThat(result.descriptors()).hasSize(3).allSatisfy(d -> {
            assertThat(d.downloadAllowed()).isTrue(); assertThat(d.documentRole()).isEqualTo("UNKNOWN");
            assertThat(d.displayName()).doesNotContain("byte");
            assertThat(p.selectApprovedRequest(d.selectRequest(),d.selectRequest())).isTrue();
        });
        for (String bad : List.of(item(4,"pdf").replace("fn_egov_downFile", "unknown"),
                "<a href='https://evil.example/file.pdf'>다른 파일</a>", item(4,"pdf").replace("gF22022313452364361", "otherGroup"),
                item(4,"pdf").replace(">공고.pdf", ">../공고.pdf"), "확인하지 못한 파일",
                item(4,"pdf").replace("<a ", "<a onclick='run()' "), item(4,"pdf").replace("'4')", "'4');evil();"))) {
            var partial = p.selectDescriptors(sample.source(),page(good + bad,4));
            assertThat(partial.complete()).isFalse(); assertThat(partial.descriptors()).hasSize(3);
        }
        var unsupported = p.selectDescriptors(sample.source(),page(good + item(4,"xlsx"),4));
        assertThat(unsupported.complete()).isTrue(); assertThat(unsupported.descriptors().getLast().downloadAllowed()).isFalse();
    }
    @Test void duplicateConflictsLimitsAndAbsentAreaAreSeparate() {
        var p = sample.profile(); String one = item(1,"hwp");
        assertThat(p.selectDescriptors(sample.source(),page(one + one,2)).descriptors()).hasSize(1);
        assertThat(p.selectDescriptors(sample.source(),page(one + one.replace("공고.hwp", "변경.hwp"),2)).complete()).isFalse();
        assertThat(p.selectDescriptors(sample.source(),page("",0)).status()).isEqualTo("NO_FILES");
        assertThat(p.selectDescriptors(sample.source(),"<p>첨부 없음</p>").complete()).isFalse();
        var limited = p.selectDescriptors(sample.source(),page(IntStream.rangeClosed(1,11).mapToObj(id -> item(id,"pdf")).collect(Collectors.joining()),11));
        assertThat(limited.status()).isEqualTo("LIMIT_EXCEEDED"); assertThat(limited.descriptors()).hasSize(10);
        assertThat(p.selectDescriptors(sample.source(),"<a href='https://evil.example/other.pdf'>외부 링크</a>" + page(one,1)).complete()).isTrue();
    }
    @Test void metadataCountAndPreviewErrorsPreserveNormalFiles() {
        var p=sample.profile();String html=page(item(1,"hwp"),1);
        for(String bad:List.of(html.replace("name=fileListCnt value=1", "name=fileListCnt value=2"),html.replace("name=fileListCnt", "name=unknown"),
                html.replace("javascript:filePreview('gF22022313452364361 '","javascript:filePreview('other'"),
                html.replace("미리보기</a>","미리보기<script>bad()</script></a>"))){
            var result=p.selectDescriptors(sample.source(),bad);assertThat(result.complete()).isFalse();assertThat(result.descriptors()).hasSize(1);
        }
        assertThat(p.selectDescriptors(sample.source(),html.replace("name=sno value=33505","name=sno value=999")).descriptors()).isEmpty();
    }
    @Test void sourceRequestAndRedirectAreBounded() {
        var p = sample.profile(); var n = new AnnouncementSourceIdentityNormalizer();
        for (String url : List.of(sample.source().sourceUrl() + "&mode=delete", sample.source().sourceUrl() + "&sno=99",
                sample.source().sourceUrl().replace("gosi_gbn=A", "gosi_gbn=X"), sample.source().sourceUrl().replace("https:", "http:"),
                sample.source().sourceUrl().replace("menu_id=00940170", "menu_id=00940171"))) {
            assertThatThrownBy(() -> p.selectDetailUri(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",
                    n.hash(n.canonicalizeUrl(url)),url,"LGS-000044","SAFE_DAEGU_LEGAL_NOTICE"))).hasMessage("PROFILE_REQUIRED");
        }
        assertThatThrownBy(() -> p.selectDetailUri(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE","wrong",sample.source().sourceUrl(),"LGS-000044","SAFE_DAEGU_LEGAL_NOTICE"))).hasMessage("PROFILE_REQUIRED");
        var first = p.selectDescriptors(sample.source(),page(item(1,"hwp"),1)).descriptors().getFirst().selectRequest();
        for (String url : List.of("https:opaque", first.uri().toString().replace("https:","http:"),
                first.uri().toString().replace("www.daegu.go.kr", "127.0.0.1"), first.uri() + "&extra=1", first.uri() + "#f",
                first.uri().toString().replace("FileDown.do","FileDownForViewer.do"),first.uri()+"&fileSn=2")) {
            assertThat(p.selectApprovedRequest(URI.create(url))).isFalse();
            if (!"https:opaque".equals(url)) assertThat(p.selectApprovedRequest(first,Request.selectGet(URI.create(url)))).isFalse();
        }
        assertThat(p.selectApprovedRequest(new Request(first.uri(),"POST",Map.of("x","y")))).isFalse();
        assertThat(p.selectApprovedRequest(first,Request.selectGet(URI.create(sample.source().sourceUrl())))).isFalse();
    }
    @Test void bodyTitleAndBudgetContract() {
        var document = Jsoup.parse(page(item(1,"hwp"),1));
        assertThat(DaeguCityNoticePage.selectContent(document,URI.create(sample.source().sourceUrl())).text()).isEqualTo("소상공인 지원금");
        AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(document,"소상공인 지원",sample.titleLayout());
        assertThatThrownBy(() -> DaeguCityNoticePage.selectContent(Jsoup.parse(document.toString() + document),URI.create(sample.source().sourceUrl()))).isInstanceOf(IllegalArgumentException.class);
        var budget = new AnnouncementAttachmentBbsOfficialObservationTest.Budget(sample.profile(),false);
        assertThat(budget.maximumRequests).isEqualTo(6); assertThat(budget.maximumBytes).isEqualTo(23L * 1024 * 1024);
    }
    @Test @EnabledIfEnvironmentVariable(named="SANEB_DAEGU_CITY_SURVEY_FIXTURE",matches="true")
    void actualHtmlContract() throws Exception {
        String html = Files.readString(Path.of("build/qa-daegu-city-20260930/detail.html"));
        var document = Jsoup.parse(html);
        AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(document,sample.title(),sample.titleLayout());
        var result = sample.profile().selectDescriptors(sample.source(),html);
        assertThat(result.complete()).as("%s %s",result.status(),result.warnings()).isTrue();
        assertThat(result.descriptors()).hasSize(1).allSatisfy(d -> assertThat(d.downloadAllowed()).isTrue());
        assertThat(DaeguCityNoticePage.selectContent(document,URI.create(sample.source().sourceUrl())).text()).isNotBlank();
        var binary=Path.of("build/qa-daegu-city-20260930/diagnostic.hwp");
        var headers=Files.readAllLines(Path.of("build/qa-daegu-city-20260930/file-get-head.txt"),StandardCharsets.ISO_8859_1);
        var types=headers.stream().filter(h->h.toLowerCase(Locale.ROOT).startsWith("content-type:")).map(h->h.substring(h.indexOf(':')+1).strip()).toList();
        var dispositions=headers.stream().filter(h->h.toLowerCase(Locale.ROOT).startsWith("content-disposition:")).map(h->h.substring(h.indexOf(':')+1).strip()).toList();
        assertThat(types).hasSize(1);assertThat(dispositions).hasSize(1);
        var response=new Download(Files.size(binary),"a".repeat(64),types.getFirst(),dispositions.getFirst());
        assertThat(new AttachmentFileTypeValidator().selectFormat(binary,response,"HWP",sample.profile().selectUtf8DispositionOctets(),sample.profile().selectLegacyBinaryContentTypes())).isEqualTo("HWP");
    }
    @Test void legacyMimeStillRequiresSignatureFormatAndSafeDisposition() throws Exception {
        var p=sample.profile();var v=new AttachmentFileTypeValidator();
        assertThat(p.selectLegacyBinaryContentTypes()).containsExactly("application/x-msdownload");assertThat(p.selectUtf8DispositionOctets()).isTrue();
        var file=Files.createTempFile("saneb-daegu-city-signature-",".bin");
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
    @Test void catalogReferenceIsNotPolicyApproval() throws Exception {
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        var notices = mapper.readTree(Files.readString(Path.of("src/main/resources/announcement-attachment/provider-qa-catalog-v2.json"))).path("notices");
        var ref = StreamSupport.stream(notices.spliterator(),false).filter(row -> sample.code().equals(row.path("caseCode").asText())).findFirst().orElseThrow();
        assertThat(ref.path("source")).isEqualTo(mapper.valueToTree(sample.source()));
        assertThat(ref.hasNonNull("expectation")).isFalse();
    }
}
