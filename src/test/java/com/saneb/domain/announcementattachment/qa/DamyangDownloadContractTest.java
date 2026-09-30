package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

class DamyangDownloadContractTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final AnnouncementAttachmentBbsOfficialObservationTest.ObservationCase sample = DamyangDownloadCases.selectCase();
    private String script(int id, String extension) {
        return "javascript:goDownLoad('공고 & 안내." + extension + "','file" + id + "." + extension
                + "','/ntishome/file/upload/ofr/ofr/20260319');return false;";
    }
    private String payload(List<String> names, List<String> scripts) throws Exception {
        return mapper.writeValueAsString(Map.of("RSLT_CD","0000","RSLT_DATA",Map.of("searchDetail",Map.of(
                "col4","소상공인 지원","col8","소상공인 지원금","fileNameArrList",names,"fileScriptArrList",scripts))));
    }
    private String one() throws Exception { return payload(List.of("공고 & 안내.pdf"),List.of(script(1,"pdf"))); }
    @Test void sourceApiAndBoundedRequestsAreExact() throws Exception {
        var p=sample.profile();var n=new AnnouncementSourceIdentityNormalizer();
        assertThat(p.selectDetailUri(sample.source()).toString()).isEqualTo("https://www.damyang.go.kr/eminwon/refreshSearchDetail?notAncmtMgtNo=37086");
        for(String url:List.of(sample.source().sourceUrl()+"&extra=1",sample.source().sourceUrl()+"&notAncmtMgtNo=99",
                sample.source().sourceUrl().replace("https:","http:"),sample.source().sourceUrl().replace("www.damyang.go.kr","127.0.0.1"),
                sample.source().sourceUrl().replace("listType=01","listType=02")))
            assertThatThrownBy(()->p.selectDetailUri(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(url)),url,"LGS-000183","DAMYANG_NOTICE_JSON"))).hasMessage("PROFILE_REQUIRED");
        assertThatThrownBy(()->p.selectDetailUri(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE","wrong",sample.source().sourceUrl(),"LGS-000183","DAMYANG_NOTICE_JSON"))).hasMessage("PROFILE_REQUIRED");
        assertThatThrownBy(()->p.selectDetailUri(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",sample.source().providerNoticeId(),sample.source().sourceUrl(),"LGS-000183","SAEOL_GOSI"))).hasMessage("PROFILE_REQUIRED");
    }
    @Test void jsonMimeUtf8AndSizeMustBeValid() throws Exception {
        var p=(AttachmentJsonDetailProfile)sample.profile();byte[] bytes=one().getBytes(StandardCharsets.UTF_8);
        for(String type:List.of("application/json","application/json;charset=UTF-8"))assertThat(p.selectJsonPayload(new ByteArrayInputStream(bytes),type)).isEqualTo(one());
        for(String type:Arrays.asList(null,"","text/html","application/json; charset=EUC-KR","application/jsonp"))
            assertThatThrownBy(()->p.selectJsonPayload(new ByteArrayInputStream(bytes),type)).hasMessage("ATTACHMENT_DETAIL_CONTENT_TYPE");
        assertThatThrownBy(()->p.selectJsonPayload(new ByteArrayInputStream(new byte[]{(byte)0xc3,0x28}),"application/json")).hasMessage("ATTACHMENT_DETAIL_ENCODING");
        assertThatThrownBy(()->p.selectJsonPayload(new ByteArrayInputStream(new byte[1_048_577]),"application/json")).hasMessage("ATTACHMENT_DETAIL_SIZE");
    }
    @Test void changedOrFailedManifestIsNotNoFiles() throws Exception {
        String good=one();var p=sample.profile();
        for(String bad:List.of(good.replace("0000","9999"),good+"{}",good.replace("\"col4\":","\"col4\":\"duplicate\",\"col4\":"),
                good.replace("fileNameArrList","unknown"),good.replace("\"RSLT_DATA\":{","\"RSLT_DATA\":{\"needsRefresh\":true,"),"<html>차단</html>")){
            var result=p.selectDescriptors(sample.source(),bad);assertThat(result.complete()).isFalse();assertThat(result.descriptors()).isEmpty();
        }
        assertThat(p.selectDescriptors(sample.source(),payload(List.of(),List.of())).status()).isEqualTo("NO_FILES");
        assertThat(((AttachmentJsonDetailProfile)p).selectJsonTitle(sample.source(),good)).isEqualTo("소상공인 지원");
    }
    @Test void validFilesSurviveBadPairsWithoutExecutingScripts() throws Exception {
        var p=sample.profile();
        for(String bad:List.of(script(2,"hwp")+"alert(1)",script(2,"hwp").replace("goDownLoad","eval"),
                script(2,"hwp").replace("/ntishome/","/../ntishome/"),script(2,"hwp").replace("file2.hwp","../file2.hwp"),"")){
            var result=p.selectDescriptors(sample.source(),payload(List.of("공고 & 안내.pdf","공고 & 안내.hwp"),List.of(script(1,"pdf"),bad)));
            assertThat(result.complete()).isFalse();assertThat(result.descriptors()).hasSize(1);assertThat(result.descriptors().getFirst().downloadAllowed()).isTrue();
        }
        var mismatch=p.selectDescriptors(sample.source(),payload(List.of("공고 & 안내.pdf","없는 파일.hwp"),List.of(script(1,"pdf"))));
        assertThat(mismatch.complete()).isFalse();assertThat(mismatch.descriptors()).hasSize(1);
        var wrongName=p.selectDescriptors(sample.source(),payload(List.of("공고 & 안내.pdf","다른 이름.hwp"),List.of(script(1,"pdf"),script(2,"hwp"))));
        assertThat(wrongName.complete()).isFalse();assertThat(wrongName.descriptors()).hasSize(1);
    }
    @Test void formatsDuplicatesAndLimitsAreIndependent() throws Exception {
        var p=sample.profile();var names=List.of("공고 & 안내.pdf","공고 & 안내.hwp","공고 & 안내.hwpx","공고 & 안내.png");
        var scripts=List.of(script(1,"pdf"),script(2,"hwp"),script(3,"hwpx"),script(4,"png"));
        var result=p.selectDescriptors(sample.source(),payload(names,scripts));assertThat(result.complete()).isTrue();assertThat(result.descriptors()).hasSize(4);
        assertThat(result.descriptors().subList(0,3)).allSatisfy(d->{assertThat(d.downloadAllowed()).isTrue();assertThat(d.documentRole()).isEqualTo("UNKNOWN");});
        assertThat(result.descriptors().getLast().downloadAllowed()).isFalse();
        assertThat(p.selectDescriptors(sample.source(),payload(List.of(names.getFirst(),names.getFirst()),List.of(scripts.getFirst(),scripts.getFirst()))).descriptors()).hasSize(1);
        var conflict=p.selectDescriptors(sample.source(),payload(List.of(names.getFirst(),"다른.pdf"),List.of(scripts.getFirst(),scripts.getFirst().replace("공고 & 안내","다른"))));
        assertThat(conflict.complete()).isFalse();assertThat(conflict.descriptors()).hasSize(1);
        var limit=p.selectDescriptors(sample.source(),payload(Collections.nCopies(11,"공고 & 안내.pdf"),IntStream.rangeClosed(1,11).mapToObj(i->script(i,"pdf")).toList()));
        assertThat(limit.status()).isEqualTo("LIMIT_EXCEEDED");assertThat(limit.descriptors()).hasSize(10);
    }
    @Test void fileRequestsCannotChangeHostPathMethodOrIdentity() throws Exception {
        var p=sample.profile();var first=p.selectDescriptors(sample.source(),one()).descriptors().getFirst();
        assertThat(p.selectApprovedRequest(first.selectRequest(),first.selectRequest())).isTrue();
        for(String bad:List.of(first.fetchUri().toString().replace("https:","http:"),first.fetchUri().toString().replace("eminwon.damyang.jeonnam.kr","127.0.0.1"),first.fetchUri()+"&x=1",first.fetchUri()+"&file_path=other",first.fetchUri()+"#f",first.fetchUri().toString().replace("FileDownNew.jsp","FileDown.jsp"))){
            assertThat(p.selectApprovedRequest(URI.create(bad))).isFalse();assertThat(p.selectApprovedRequest(first.selectRequest(),Request.selectGet(URI.create(bad)))).isFalse();
        }
        var second=p.selectDescriptors(sample.source(),payload(List.of("공고 & 안내.pdf"),List.of(script(2,"pdf")))).descriptors().getFirst();
        assertThat(p.selectApprovedRequest(first.selectRequest(),second.selectRequest())).isFalse();
        assertThat(p.selectApprovedRequest(new Request(first.fetchUri(),"POST",Map.of("x","y")))).isFalse();
    }
    @Test @EnabledIfEnvironmentVariable(named="SANEB_DAMYANG_SURVEY_FIXTURE",matches="true")
    void actualJsonContract() throws Exception {
        String json=Files.readString(Path.of("build/qa-damyang-json-20260930/refresh.json"));var result=sample.profile().selectDescriptors(sample.source(),json);
        assertThat(result.complete()).as("%s %s",result.status(),result.warnings()).isTrue();assertThat(result.descriptors()).hasSize(1);
        assertThat(result.descriptors().getFirst().expectedFormat()).isEqualTo("HWP");
        assertThat(((AttachmentJsonDetailProfile)sample.profile()).selectJsonTitle(sample.source(),json)).isEqualTo(sample.title());
    }
    @Test void catalogAndBudgetDoNotApproveExpectation() throws Exception {
        var notices=mapper.readTree(Files.readString(Path.of("src/main/resources/announcement-attachment/provider-qa-catalog-v2.json"))).path("notices");
        var ref=StreamSupport.stream(notices.spliterator(),false).filter(r->sample.code().equals(r.path("caseCode").asText())).findFirst().orElseThrow();
        assertThat(ref.path("source")).isEqualTo(mapper.valueToTree(sample.source()));assertThat(ref.hasNonNull("expectation")).isFalse();
        var budget=new AnnouncementAttachmentBbsOfficialObservationTest.Budget(sample.profile(),false);assertThat(budget.maximumRequests).isEqualTo(6);assertThat(budget.maximumBytes).isEqualTo(23L*1024*1024);
    }
}
