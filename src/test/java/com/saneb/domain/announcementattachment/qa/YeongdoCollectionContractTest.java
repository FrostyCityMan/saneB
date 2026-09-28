package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.classification.*;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.*;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;

class YeongdoCollectionContractTest {
    final YeongdoAttachmentDiscoveryProfile profile=new YeongdoAttachmentDiscoveryProfile();
    final AnnouncementAttachmentBbsOfficialObservationTest.ObservationCase sample=AnnouncementAttachmentBbsOfficialObservationTest.selectCases("YEONGDO").findFirst().orElseThrow();
    String selectLink(String suffix,int id){
        String name="문서 "+id+"."+suffix;
        return "<li><a class='filename' href='https://eminwon.yeongdo.go.kr/emwp/jsp/ofr/FileDown.jsp?user_file_nm="+URLEncoder.encode(name,StandardCharsets.UTF_8)+"&amp;sys_file_nm="+id+"."+suffix+"&amp;file_path=%2Fntishome%2Ffile%2Fupload%2Fofr%2Fofr%2F20260928'>"+name+"</a></li>";
    }
    String selectPage(String links){return "<form id='saeolGosiVO' name='saeolGosiVO' method='get'><div class='bbs1view1'><h1 class='h1'>청년 지원사업</h1><div class='attach1'><ul>"+links+"</ul></div></div></form>";}
    @Test void allOfficialLinksArePreservedWithoutInferringRoles(){
        String links=selectLink("pdf",1)+selectLink("hwp",2)+selectLink("hwpx",3);
        var r=profile.selectDescriptors(sample.source(),selectPage(links));assertThat(r.complete()).isTrue();
        assertThat(r.descriptors()).extracting(AttachmentDiscoveryProfile.Descriptor::expectedFormat).containsExactly("PDF","HWP","HWPX");
        assertThat(r.descriptors()).allSatisfy(d->{assertThat(d.documentRole()).isEqualTo("UNKNOWN");assertThat(d.postForm()).isEmpty();assertThat(d.downloadAllowed()).isTrue();assertThat(profile.selectApprovedRequest(d.selectRequest())).isTrue();});
        assertThat(profile.selectDescriptors(sample.source(),selectPage(links+selectLink("pdf",1))).descriptors()).hasSize(3);
        var unsupported=profile.selectDescriptors(sample.source(),selectPage(links+selectLink("jpg",4)));assertThat(unsupported.complete()).isTrue();assertThat(unsupported.descriptors()).hasSize(4);assertThat(unsupported.descriptors().getLast().downloadAllowed()).isFalse();
        assertThat(profile.selectDescriptors(sample.source(),selectPage(" ")).status()).isEqualTo("NO_FILES");
        assertThat(profile.selectDescriptors(sample.source(),selectPage("<li></li>")).complete()).isFalse();
        String many=java.util.stream.IntStream.rangeClosed(1,11).mapToObj(i->selectLink("pdf",i)).collect(java.util.stream.Collectors.joining());assertThat(profile.selectDescriptors(sample.source(),selectPage(many)).status()).isEqualTo("LIMIT_EXCEEDED");
    }
    @Test void unresolvedOrChangedStructureNeverBecomesAnEmptySet(){
        for(String extra:List.of("<li><a href='/other'>파일</a></li>","<script>hidden()</script>","<button>첨부</button>","남은 파일","<img src='/hidden'>","<li onmouseover='hidden()'></li>"))assertThat(profile.selectDescriptors(sample.source(),selectPage(selectLink("pdf",1)+extra)).complete()).as(extra).isFalse();
        String good=selectPage(selectLink("pdf",1));
        for(String html:List.of(good+good,good.replace("method='get'","method='post'"),good.replace("bbs1view1","other"),good.replace("filename","other"),good.replace("<ul>","<ul><div>").replace("</ul>","</div></ul>"),good.replace("문서 1.pdf</a>","다른 파일.pdf</a>"),good.replace("<div class='attach1'>","<div class='attach1' onload='hidden()'>")))assertThat(profile.selectDescriptors(sample.source(),html).complete()).isFalse();
    }
    @Test void requestAndSourceRemainBoundToOfficialHostsAndNotice(){
        var source=sample.source();URI detail=profile.selectDetailUri(source);assertThat(detail.toString()).isEqualTo(source.sourceUrl());
        for(String bad:List.of(source.sourceUrl().replace("https:","http:"),source.sourceUrl().replace("www.yeongdo.go.kr","example.com"),source.sourceUrl()+"&amode=view",source.sourceUrl()+"&extra=1",source.sourceUrl()+"#fragment",source.sourceUrl().replace("amode=view","amode=write"),source.sourceUrl().replace("/00000/","/00000/../00000/")))assertThat(profile.selectApprovedRequest(URI.create(bad))).isFalse();
        assertThat(profile.selectApprovedRequest(new Request(detail,"POST",Map.of("amode","view")))).isFalse();
        assertThat(profile.selectApprovedRequest(Request.selectGet(detail),Request.selectGet(URI.create(detail.toString().replace("36435","36164"))))).isFalse();
        for(var bad:List.of(new AttachmentDiscoveryProfile.Source(source.providerCode(),source.providerNoticeId(),source.sourceUrl(),"LGS-000040",source.listParserProfileCode()),new AttachmentDiscoveryProfile.Source(source.providerCode(),source.providerNoticeId(),source.sourceUrl(),source.localSourceCode(),"SAEOL_GOSI"),new AttachmentDiscoveryProfile.Source(source.providerCode(),"a".repeat(64),source.sourceUrl(),source.localSourceCode(),source.listParserProfileCode())))assertThatThrownBy(()->profile.selectDetailUri(bad)).hasMessage("PROFILE_REQUIRED");
        var d=profile.selectDescriptors(source,selectPage(selectLink("pdf",1))).descriptors().getFirst();
        for(String bad:List.of(d.fetchUri()+"&extra=1",d.fetchUri().toString().replace("eminwon.yeongdo.go.kr","www.yeongdo.go.kr"),d.fetchUri().toString().replace("user_file_nm=","user_file_nm=..%2F")))assertThat(profile.selectApprovedRequest(URI.create(bad))).isFalse();
    }
    @Test void titleAndBudgetsAreExplicit(){
        String html=selectPage(selectLink("pdf",1));var layout=sample.titleLayout();
        AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(Jsoup.parse(html),"청년 지원사업",layout);
        for(String bad:List.of(html+html,html.replace("청년 지원사업","다른 제목"),html.replace("<h1 class='h1'>청년 지원사업","<h1 class='h1'><b>청년 지원사업</b>")))assertThatThrownBy(()->AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(Jsoup.parse(bad),"청년 지원사업",layout)).isInstanceOf(AssertionError.class);
        for(boolean diagnostic:List.of(true,false)){var b=new AnnouncementAttachmentBbsOfficialObservationTest.Budget(profile,diagnostic);assertThat(b.maximumRequests).isEqualTo(7);assertThat(b.maximumBytes).isEqualTo(23L*1024*1024);}
    }
    @Test void fixedCasesKeepTitlePolicyAndReferenceOnlyCatalog() throws Exception {
        var rules=AnnouncementAttachmentRealFileQaTest.selectDraftRuleSet();var engine=new AnnouncementSourceClassificationEngine();var json=new com.fasterxml.jackson.databind.ObjectMapper();
        var catalog=json.readTree(Files.readString(Path.of("src/main/resources/announcement-attachment/provider-qa-catalog-v2.json"))).path("notices");
        for(var item:AnnouncementAttachmentBbsOfficialObservationTest.selectCases("YEONGDO").toList()){
            var result=engine.selectDecision(new AnnouncementSourceClassificationInput("LOCAL_GOV_NOTICE",item.title(),null,null,List.of(),BodySourceCode.NONE,BodyAvailabilityCode.UNAVAILABLE),rules);
            assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectTitleMayProceed(result)).as(item.code()).isTrue();
            var ref=java.util.stream.StreamSupport.stream(catalog.spliterator(),false).filter(n->item.code().equals(n.path("caseCode").asText())).findFirst().orElseThrow();
            assertThat(ref.path("source")).isEqualTo(json.valueToTree(item.source()));assertThat(ref.hasNonNull("expectation")).isFalse();
        }
    }
    @Test @EnabledIfEnvironmentVariable(named="SANEB_YEONGDO_SAVED_DETAILS",matches=".+")
    void officialSavedDetailsProveWholeSetsWithoutNetwork() throws Exception {
        Path root=Path.of(System.getenv("SANEB_YEONGDO_SAVED_DETAILS")).toRealPath();assertThat(root.startsWith(Path.of("build").toRealPath())).isTrue();
        for(var item:AnnouncementAttachmentBbsOfficialObservationTest.selectCases("YEONGDO").toList()){
            Path file=root.resolve(item.code()+".html");assertThat(Files.size(file)).isLessThan(1048576);String html=Files.readString(file);
            AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(Jsoup.parse(html),item.title(),item.titleLayout());
            var r=profile.selectDescriptors(item.source(),html);assertThat(r.complete()).as(item.code()).isTrue();assertThat(r.status()).isEqualTo("FOUND");
            assertThat(r.descriptors()).hasSize(item.listedFileCount()).allSatisfy(d->{assertThat(d.downloadAllowed()).isTrue();assertThat(d.documentRole()).isEqualTo("UNKNOWN");assertThat(profile.selectApprovedRequest(d.selectRequest())).isTrue();});
        }
    }
}
