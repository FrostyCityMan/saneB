package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.provider.content.ChuncheonNoticePage;
import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

class ChuncheonDownloadContractTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final AnnouncementAttachmentBbsOfficialObservationTest.ObservationCase sample = ChuncheonDownloadCases.selectCase();
    private String item(int id, String extension) {
        return "{\"file_seq\":\"" + id + "\",\"file_nm\":\"공고 & 안내." + extension
                + "\",\"sys_file_nm\":\"file" + id + "." + extension + "\",\"file_path\":\"/ntishome/file/upload/ofr/ofr/20260115\"}";
    }
    private String payload(String files) {
        return "{\"board\":{\"not_ancmt_mgt_no\":\"73071\",\"not_ancmt_sj\":\"소상공인 지원\",\"not_ancmt_cn\":\"소상공인 지원금\"},\"file\":[" + files + "]}";
    }
    @Test void exactSourceAndApiBindingWithProviderWithheldNotices() {
        var p = sample.profile(); var n = new AnnouncementSourceIdentityNormalizer();
        assertThat(p.selectDetailUri(sample.source()).toString()).isEqualTo("https://www.chuncheon.go.kr/_chuncheon/noticeView.do?notAncmtMgtNo=73071");
        var invalid = new ArrayList<>(List.of(sample.source().sourceUrl() + "&extra=1",sample.source().sourceUrl() + "&notAncmtMgtNo=99",
                sample.source().sourceUrl().replace("https:","http:"),sample.source().sourceUrl().replace("www.chuncheon.go.kr","127.0.0.1")));
        for (String id : List.of("18207","18265","26243","40349","65304","68021")) invalid.add(sample.source().sourceUrl().replace("73071",id));
        for (String url : invalid) assertThatThrownBy(() -> p.selectDetailUri(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",
                n.hash(n.canonicalizeUrl(url)),url,"LGS-000117","CHUNCHEON_NOTICE_JSON"))).hasMessage("PROFILE_REQUIRED");
        assertThatThrownBy(() -> p.selectDetailUri(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE","wrong",sample.source().sourceUrl(),"LGS-000117","CHUNCHEON_NOTICE_JSON"))).hasMessage("PROFILE_REQUIRED");
        assertThatThrownBy(() -> p.selectDetailUri(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",sample.source().providerNoticeId(),sample.source().sourceUrl(),"LGS-000117","SAEOL_GOSI"))).hasMessage("PROFILE_REQUIRED");
    }
    @Test void jsonMimeUtf8AndBoundedSizeAreSpecificToSelectedProfile() throws Exception {
        var p = (AttachmentJsonDetailProfile)sample.profile(); byte[] bytes = payload("").getBytes(StandardCharsets.UTF_8);
        for (String type : List.of("","application/json","application/json; charset=UTF-8")) assertThat(p.selectJsonPayload(new ByteArrayInputStream(bytes),type)).isEqualTo(payload(""));
        assertThat(p.selectJsonPayload(new ByteArrayInputStream(bytes),null)).isEqualTo(payload(""));
        for (String type : List.of("text/html","application/pdf","application/json; charset=EUC-KR","application/jsonp"))
            assertThatThrownBy(() -> p.selectJsonPayload(new ByteArrayInputStream(bytes),type)).hasMessage("ATTACHMENT_DETAIL_CONTENT_TYPE");
        assertThatThrownBy(() -> p.selectJsonPayload(new ByteArrayInputStream(new byte[]{(byte)0xc3,0x28}),"")).hasMessage("ATTACHMENT_DETAIL_ENCODING");
        assertThatThrownBy(() -> p.selectJsonPayload(new ByteArrayInputStream(new byte[1_048_577]),"")).hasMessage("ATTACHMENT_DETAIL_SIZE");
        assertThat(new GangwonProvinceAttachmentDiscoveryProfile()).isNotInstanceOf(AttachmentJsonDetailProfile.class);
    }
    @Test void manifestIdentityAndDuplicateKeysDoNotBecomeNoFiles() {
        var p = sample.profile();String json=payload(item(1,"pdf"));
        for(String bad:List.of(json.replace("73071","999"),json.replace("\"board\":","\"board\":{},\"board\":"),json+"{}","<html>차단 안내</html>",json.replace("\"file\":[","\"unknown\":["),json.replace("\"file\":["+item(1,"pdf")+"]","\"file\":null"))){
            var result=p.selectDescriptors(sample.source(),bad);assertThat(result.complete()).isFalse();assertThat(result.descriptors()).isEmpty();
        }
        assertThat(p.selectDescriptors(sample.source(),payload("")).status()).isEqualTo("NO_FILES");
        assertThat(((AttachmentJsonDetailProfile)p).selectJsonTitle(sample.source(),json)).isEqualTo("소상공인 지원");
    }
    @Test void partialFilesUnsupportedDuplicatesAndLimitRemainSeparate() {
        var p=sample.profile();String good=item(1,"pdf")+","+item(2,"hwp")+","+item(3,"hwpx");
        var result=p.selectDescriptors(sample.source(),payload(good));assertThat(result.complete()).isTrue();assertThat(result.descriptors()).hasSize(3).allSatisfy(d->{assertThat(d.documentRole()).isEqualTo("UNKNOWN");assertThat(d.downloadAllowed()).isTrue();assertThat(p.selectApprovedRequest(d.selectRequest(),d.selectRequest())).isTrue();});
        for(String bad:List.of("{}",item(4,"pdf").replace("/ntishome/","/../ntishome/"),item(4,"pdf").replace("file4.pdf","../file4.pdf"))){var partial=p.selectDescriptors(sample.source(),payload(good+","+bad));assertThat(partial.complete()).isFalse();assertThat(partial.descriptors()).hasSize(3);}
        var unsupported=p.selectDescriptors(sample.source(),payload(good+","+item(4,"xlsx")));assertThat(unsupported.complete()).isTrue();assertThat(unsupported.descriptors().getLast().downloadAllowed()).isFalse();
        String one=item(1,"pdf");assertThat(p.selectDescriptors(sample.source(),payload(one+","+one)).descriptors()).hasSize(1);
        assertThat(p.selectDescriptors(sample.source(),payload(one+","+one.replace("공고 & 안내","다른 공고"))).complete()).isFalse();
        assertThat(p.selectDescriptors(sample.source(),payload(one+","+item(2,"pdf").replace("\"file_seq\":\"2\"","\"file_seq\":\"1\""))).complete()).isFalse();
        var limit=p.selectDescriptors(sample.source(),payload(IntStream.rangeClosed(1,11).mapToObj(id->item(id,"pdf")).collect(Collectors.joining(","))));assertThat(limit.status()).isEqualTo("LIMIT_EXCEEDED");assertThat(limit.descriptors()).hasSize(10);
    }
    @Test void fileRequestsCannotChangeHostPathMethodOrRedirectIdentity() {
        var p=sample.profile();var first=p.selectDescriptors(sample.source(),payload(item(1,"pdf"))).descriptors().getFirst();
        var second=p.selectDescriptors(sample.source(),payload(item(2,"pdf"))).descriptors().getFirst();
        for(String bad:List.of(first.fetchUri().toString().replace("https:","http:"),first.fetchUri().toString().replace("eminwon.chuncheon.go.kr","127.0.0.1"),first.fetchUri()+"&x=1",first.fetchUri()+"&file_path=other",first.fetchUri()+"#f",first.fetchUri().toString().replace("FileDown.jsp","Other.jsp"))){assertThat(p.selectApprovedRequest(URI.create(bad))).isFalse();assertThat(p.selectApprovedRequest(first.selectRequest(),Request.selectGet(URI.create(bad)))).isFalse();}
        assertThat(p.selectApprovedRequest(first.selectRequest(),second.selectRequest())).isFalse();
        assertThat(p.selectApprovedRequest(new Request(first.fetchUri(),"POST",Map.of("x","y")))).isFalse();
        assertThat(p.selectApprovedRequest(URI.create("https://www.chuncheon.go.kr/_chuncheon/synapViewerExt.do"))).isFalse();
    }
    @Test @EnabledIfEnvironmentVariable(named="SANEB_CHUNCHEON_SURVEY_FIXTURE",matches="true")
    void actualJsonContract() throws Exception {
        String json=Files.readString(Path.of("build/qa-chuncheon-20260930/CHUNCHEON-view.json"));var result=sample.profile().selectDescriptors(sample.source(),json);
        assertThat(result.complete()).as("%s %s",result.status(),result.warnings()).isTrue();assertThat(result.descriptors()).hasSize(1);assertThat(result.descriptors().getFirst().expectedFormat()).isEqualTo("HWP");
        assertThat(((AttachmentJsonDetailProfile)sample.profile()).selectJsonTitle(sample.source(),json)).isEqualTo(sample.title());
    }
    @Test void catalogAndBudgetDoNotApproveExpectation() throws Exception {
        var notices=mapper.readTree(Files.readString(Path.of("src/main/resources/announcement-attachment/provider-qa-catalog-v2.json"))).path("notices");
        var ref=StreamSupport.stream(notices.spliterator(),false).filter(r->sample.code().equals(r.path("caseCode").asText())).findFirst().orElseThrow();
        assertThat(ref.path("source")).isEqualTo(mapper.valueToTree(sample.source()));assertThat(ref.hasNonNull("expectation")).isFalse();
        var budget=new AnnouncementAttachmentBbsOfficialObservationTest.Budget(sample.profile(),false);assertThat(budget.maximumRequests).isEqualTo(6);assertThat(budget.maximumBytes).isEqualTo(23L*1024*1024);
    }
}
