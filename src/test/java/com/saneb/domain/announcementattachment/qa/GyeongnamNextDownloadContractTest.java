package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.*;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import com.saneb.domain.announcementattachment.discovery.AttachmentDiscoveryProfile;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.classification.*;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.ObservationCase;

class GyeongnamNextDownloadContractTest {
    private List<ObservationCase> selectCases(){return GyeongnamNextDownloadCases.selectCases().toList();}
    private String enc(String v){return URLEncoder.encode(v,StandardCharsets.UTF_8);}
    private String selectLink(ObservationCase c,int id,String ext){String host=URI.create(c.source().sourceUrl()).getHost().replace("www.","eminwon.");String name="공고 "+id+"."+ext;String inner="http://"+host+"/emwp/jsp/ofr/FileDown.jsp?user_file_nm="+enc(name)+"&sys_file_nm="+enc("공고_"+id+"."+ext)+"&file_path="+enc("/ntishome/file/upload/ofr/ofr/20260130");return "<a href='/DownloadEx.do?url="+enc(inner)+"&amp;name="+enc(name)+"'>"+name+"</a>";}
    private String selectPage(ObservationCase c,String links){String form=c.code().startsWith("GIMHAE")?"saeolGosiVO":"seolVO";return "<form id="+form+"><div class=bbs1view1><h1 class=h1>"+c.title()+"</h1><div class=info1>수출 부서</div><div class=attach1>"+links+"</div><div class=substance>소상공인 지원</div></div></form>";}
    @Test void registersTwoDistinctSourcesAndBounds(){
        assertThat(selectCases()).hasSize(2);assertThat(selectCases().stream().map(c->c.profile().selectProfileHash()).distinct()).hasSize(2);
        for(var c:selectCases()){var b=AnnouncementAttachmentBbsOfficialObservationTest.selectBudget(c.profile(),true,false);assertThat(b.maximumRequests).isEqualTo(6);assertThat(b.maximumBytes).isEqualTo(23*1024*1024);assertThat(c.profile().selectDetailUri(c.source()).toString()).isEqualTo(c.source().sourceUrl());}
    }
    @Test void enablesOnlyMeasuredLegacyResponseMode(){
        for(var c:selectCases()){assertThat(c.profile().selectLegacyBinaryContentTypes()).containsExactly("application/x-msdownload");assertThat(c.profile().selectUtf8DispositionOctets()).isTrue();assertThat(c.profile()).isInstanceOf(com.saneb.domain.announcementattachment.discovery.AttachmentDownloadFlowProfile.class);}
    }
    @Test void downloadsOnlyOfficialProxyAndDoesNotCallNestedHttp(){
        for(var c:selectCases()){var r=c.profile().selectDescriptors(c.source(),selectPage(c,selectLink(c,1,"hwpx")));assertThat(r.status()).isEqualTo("FOUND");assertThat(r.complete()).isTrue();assertThat(r.descriptors()).singleElement().satisfies(d->{assertThat(d.downloadAllowed()).isTrue();assertThat(d.documentRole()).isEqualTo("UNKNOWN");assertThat(d.fetchUri().getScheme()).isEqualTo("https");assertThat(d.fetchUri().getHost()).isEqualTo(URI.create(c.source().sourceUrl()).getHost());assertThat(c.profile().selectApprovedRequest(d.fetchUri())).isTrue();assertThat(d.toString()).doesNotContain("공고","https");});}
    }
    @Test void keepsGoodFilesWhenUnknownLinksScriptsOrUnsupportedFilesExist(){
        for(var c:selectCases()){for(String extra:List.of("<script>viewer()</script>","<a href='/viewer'>미리보기</a>","<input type=hidden>","<img src='/unknown'>")){var r=c.profile().selectDescriptors(c.source(),selectPage(c,selectLink(c,1,"hwp")+extra));assertThat(r.complete()).isFalse();assertThat(r.descriptors()).hasSize(1);}
            var r=c.profile().selectDescriptors(c.source(),selectPage(c,selectLink(c,1,"hwp")+selectLink(c,2,"xlsx")));assertThat(r.complete()).isTrue();assertThat(r.descriptors()).hasSize(2);assertThat(r.descriptors().getLast().downloadAllowed()).isFalse();}
    }
    @Test void rejectsWrongSourceAndTitle(){
        for(var c:selectCases()){var s=c.source();assertThatThrownBy(()->c.profile().selectDetailUri(new AttachmentDiscoveryProfile.Source(s.providerCode(),s.providerNoticeId(),s.sourceUrl(),"LGS-000001",s.listParserProfileCode()))).isInstanceOf(IllegalArgumentException.class);
            var page=Jsoup.parse(selectPage(c,selectLink(c,1,"pdf")));AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,c.title(),c.titleLayout());assertThatThrownBy(()->AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,"다른 제목",c.titleLayout())).isInstanceOf(AssertionError.class);}
    }
    @Test void titleGateUsesExistingDraftSeedWithoutKeywordChanges()throws Exception{
        for(var c:selectCases()){var d=new AnnouncementSourceClassificationEngine().selectDecision(new AnnouncementSourceClassificationInput("LOCAL_GOV_NOTICE",c.title(),null,null,List.of(),AnnouncementSourceClassificationCodes.BodySourceCode.NONE,AnnouncementSourceClassificationCodes.BodyAvailabilityCode.UNAVAILABLE),AnnouncementAttachmentRealFileQaTest.selectDraftRuleSet());assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectTitleMayProceed(d)).isTrue();}
    }
    @Test void distinguishesAbsentMissingDuplicateAndLimit(){
        for(var c:selectCases()){var p=c.profile();assertThat(p.selectDescriptors(c.source(),selectPage(c,"")).status()).isEqualTo("NO_FILES");for(String bad:List.of("<html>오류</html>",selectPage(c,"")+selectPage(c,""),selectPage(c,"").replace("class=attach1","class=other"),selectPage(c,"<li></li>")))assertThat(p.selectDescriptors(c.source(),bad).complete()).isFalse();
            var r=p.selectDescriptors(c.source(),selectPage(c,IntStream.rangeClosed(1,11).mapToObj(i->selectLink(c,i,"pdf")).collect(Collectors.joining())));assertThat(r.status()).isEqualTo("LIMIT_EXCEEDED");assertThat(r.descriptors()).hasSize(10);}
    }
    @Test void forbidsUnapprovedHostsQueryExtensionsAndRedirects(){
        for(var c:selectCases()){var p=c.profile();URI detail=p.selectDetailUri(c.source());URI file=p.selectDescriptors(c.source(),selectPage(c,selectLink(c,1,"hwp"))).descriptors().getFirst().fetchUri();
            for(String bad:List.of(detail+"&not_ancmt_mgt_no=2",detail+"&unknown=1",detail+"&section=02",file+"&extra=1",file.toString().replace("eminwon.","evil."),file.toString().replace("https:","http:")))assertThat(p.selectApprovedRequest(URI.create(bad))).isFalse();
            assertThat(p.selectApprovedRequest(Request.selectGet(detail),Request.selectGet(file))).isFalse();assertThat(p.selectApprovedRequest(new Request(file,"POST",Map.of("x","y")))).isFalse();}
    }
    @Test void validatesNestedFilenameAndPreservesGoodOnMismatch(){
        for(var c:selectCases()){String good=selectLink(c,1,"hwp"),bad=selectLink(c,2,"hwp").replace("&amp;name=","&amp;name=wrong");var r=c.profile().selectDescriptors(c.source(),selectPage(c,good+bad));assertThat(r.complete()).isFalse();assertThat(r.descriptors()).hasSize(1);assertThat(c.profile().selectDescriptors(c.source(),selectPage(c,good+good)).descriptors()).hasSize(1);}
    }
}
