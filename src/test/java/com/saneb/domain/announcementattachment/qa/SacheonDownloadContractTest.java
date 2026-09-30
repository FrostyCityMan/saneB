package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.*;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import com.saneb.domain.announcementattachment.discovery.AttachmentDiscoveryProfile;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.classification.*;

class SacheonDownloadContractTest {
    private final AnnouncementAttachmentBbsOfficialObservationTest.ObservationCase sample=SacheonDownloadCases.selectCase();
    private String selectItem(int id,String ext,String session){String name="공고 "+id+"."+ext,q="gcode=2017&amp;name="+URLEncoder.encode(name,StandardCharsets.UTF_8),file="/board/download.do"+(session==null?"":";jsessionid="+session)+"?"+q;return "<li><a class=filename href='"+file+"'>"+name+"</a><a href='/sn3hcv_convert.jsp?"+q+"'>바로보기</a><a href='/sn3hcv_convert.jsp?"+q+"&amp;tts=1'>바로듣기</a><a class='b1 download' href='"+file+"' title='"+name+" 다운로드'><i></i>다운로드</a></li>";}
    private String selectPage(String items){return "<div class=bbs1view1><h1 class=h1 id=sns_bbs_title>"+sample.title()+"</h1><div class=substance><div class=substanceautolink>소상공인 지원</div></div><div class=attach1><ul>"+items+"</ul></div></div>";}
    @Test void collectsOneFileWithoutDownloadingViewersOrDuplicateButtons(){
        for(String s:List.of("A".repeat(32),"")){var r=sample.profile().selectDescriptors(sample.source(),selectPage(selectItem(1,"hwpx",s.isEmpty()?null:s)));assertThat(r.complete()).isTrue();assertThat(r.descriptors()).singleElement().satisfies(d->{assertThat(d.displayName()).isEqualTo("공고 1.hwpx");assertThat(d.downloadAllowed()).isTrue();assertThat(d.documentRole()).isEqualTo("UNKNOWN");assertThat(sample.profile().selectApprovedRequest(d.fetchUri())).isTrue();});}
    }
    @Test void ephemeralSessionIsNeverStoredInLocatorAndDoesNotChangeIdentity(){
        var a=sample.profile().selectDescriptors(sample.source(),selectPage(selectItem(1,"hwp","A".repeat(32)))).descriptors().getFirst();var b=sample.profile().selectDescriptors(sample.source(),selectPage(selectItem(1,"hwp","B".repeat(32)))).descriptors().getFirst();assertThat(a.fetchUri()).isNotEqualTo(b.fetchUri());assertThat(a.locator()).isEqualTo(b.locator());assertThat(a.locator().toString()).doesNotContain("jsessionid","A".repeat(32),"공고");assertThat(a.toString()).doesNotContain("jsessionid","A".repeat(32));assertThat(a.selectRequest().toString()).doesNotContain("A".repeat(32));assertThat(sample.profile().selectApprovedRequest(a.selectRequest(),b.selectRequest())).isFalse();
    }
    @Test void verifiesSourceTitleAndSeedEligibility()throws Exception{
        var p=sample.profile();var s=sample.source();assertThat(p.selectDetailUri(s).toString()).isEqualTo(s.sourceUrl());assertThatThrownBy(()->p.selectDetailUri(new AttachmentDiscoveryProfile.Source(s.providerCode(),s.providerNoticeId(),s.sourceUrl(),"LGS-000228",s.listParserProfileCode()))).isInstanceOf(IllegalArgumentException.class);
        var page=Jsoup.parse(selectPage(selectItem(1,"hwp",null)));AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,sample.title(),sample.titleLayout());assertThatThrownBy(()->AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,"다른 제목",sample.titleLayout())).isInstanceOf(AssertionError.class);
        var d=new AnnouncementSourceClassificationEngine().selectDecision(new AnnouncementSourceClassificationInput("LOCAL_GOV_NOTICE",sample.title(),null,null,List.of(),AnnouncementSourceClassificationCodes.BodySourceCode.NONE,AnnouncementSourceClassificationCodes.BodyAvailabilityCode.UNAVAILABLE),AnnouncementAttachmentRealFileQaTest.selectDraftRuleSet());assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectTitleMayProceed(d)).isTrue();
    }
    @Test void preservesGoodFilesAndUnsupportedWhenIndividualControlsFail(){
        for(String extra:List.of("<script>x()</script>","<a href='/unknown'>미확인</a>","<img src='/other'>","<button>파일</button>")){var r=sample.profile().selectDescriptors(sample.source(),selectPage(selectItem(1,"hwp",null)+extra));assertThat(r.complete()).isFalse();assertThat(r.descriptors()).hasSize(1);}
        var r=sample.profile().selectDescriptors(sample.source(),selectPage(selectItem(1,"hwp",null)+selectItem(2,"xlsx",null)));assertThat(r.complete()).isTrue();assertThat(r.descriptors()).hasSize(2);assertThat(r.descriptors().getLast().downloadAllowed()).isFalse();
    }
    @Test void distinguishesNoFilesMissingAreaDuplicateAndLimit(){
        var p=sample.profile();assertThat(p.selectDescriptors(sample.source(),selectPage("")).status()).isEqualTo("NO_FILES");
        for(String bad:List.of("<html>오류</html>",selectPage("")+selectPage(""),selectPage("").replace("class=attach1","class=other"),selectPage("<li></li>")))assertThat(p.selectDescriptors(sample.source(),bad).complete()).isFalse();
        var r=p.selectDescriptors(sample.source(),selectPage(IntStream.rangeClosed(1,11).mapToObj(i->selectItem(i,"pdf",null)).collect(Collectors.joining())));assertThat(r.status()).isEqualTo("LIMIT_EXCEEDED");assertThat(r.descriptors()).hasSize(10);
    }
    @Test void refusesInvalidSessionsOtherBoardsEncodedPathAndArbitraryRedirect(){
        var p=sample.profile();URI detail=p.selectDetailUri(sample.source());URI file=p.selectDescriptors(sample.source(),selectPage(selectItem(1,"hwp","A".repeat(32)))).descriptors().getFirst().fetchUri();
        for(String bad:List.of(file.toString().replace("A".repeat(32),"A".repeat(31)),file+"&gcode=2017",file.toString().replace("gcode=2017","gcode=2018"),file.toString().replace(";jsessionid=","%3Bjsessionid="),file.toString().replace("https:","http:"),file.toString().replace("www.sacheon.go.kr","evil.go.kr"),detail+"&idx=2",detail+"&extra=1"))assertThat(p.selectApprovedRequest(URI.create(bad))).isFalse();
        assertThat(p.selectApprovedRequest(Request.selectGet(detail),Request.selectGet(file))).isFalse();assertThat(p.selectApprovedRequest(new Request(file,"POST",Map.of("x","y")))).isFalse();
    }
    @Test void mismatchedPreviewOrDuplicateButtonDoesNotDiscardGoodFile(){
        String item=selectItem(1,"hwp",null);
        for(String bad:List.of(item.replace("tts=1","tts=2"),item.replace("공고 1.hwp 다운로드","다른 파일 다운로드"),item.replace("바로보기","다른 보기"))){var r=sample.profile().selectDescriptors(sample.source(),selectPage(bad));assertThat(r.complete()).isFalse();assertThat(r.descriptors()).hasSize(1);}
        var r=sample.profile().selectDescriptors(sample.source(),selectPage(item+item));assertThat(r.complete()).isTrue();assertThat(r.descriptors()).hasSize(1);
    }
    @Test void titleFilenameAndSessionConflictsRemainErrors(){
        String item=selectItem(1,"hwp","A".repeat(32));var r=sample.profile().selectDescriptors(sample.source(),selectPage(item+item.replace("A".repeat(32),"B".repeat(32))));assertThat(r.complete()).isFalse();assertThat(r.descriptors()).hasSize(1);
        r=sample.profile().selectDescriptors(sample.source(),selectPage(item.replace(">공고 1.hwp</a>",">다른 파일.hwp</a>")));assertThat(r.complete()).isFalse();assertThat(r.descriptors()).isEmpty();
    }
    @Test void boundedBudgetAndMeasuredMimeAreExplicit(){var p=sample.profile();var b=AnnouncementAttachmentBbsOfficialObservationTest.selectBudget(p,true,false);assertThat(b.maximumRequests).isEqualTo(6);assertThat(b.maximumBytes).isEqualTo(23*1024*1024);assertThat(p.selectLegacyBinaryContentTypes()).containsExactly("application/x-msdownload");assertThat(p.selectUtf8DispositionOctets()).isTrue();}
}
