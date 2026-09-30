package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import java.net.URI;
import java.util.*;
import java.util.stream.*;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import com.saneb.domain.announcementattachment.discovery.AttachmentDiscoveryProfile;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.classification.*;

class JangheungDownloadContractTest {
    private final AnnouncementAttachmentBbsOfficialObservationTest.ObservationCase sample=JangheungDownloadCases.selectCase();
    private String selectItem(int id,String ext){return "<li><span class=file><i class=ico_hwp></i><span class=name>공고 "+id+"</span><span class=info>["+ext+"]</span></span><div><button type=button onclick=\"window.open('https://eminwon.jangheung.go.kr/emwp/jsp/ofr/FileDown.jsp?user_file_nm=공고 "+id+"."+ext+"&sys_file_nm=공고 "+id+"_ofr_20260128."+ext+"&file_path=/ntishome/file/upload/ofr/ofr/20260128', '_blank')\"><span>다운로드</span><i class=ico_download></i></button><button type=button onclick=\"window.open('/Viewer_gosi/27081_"+id+"', '_blank')\">바로가기</button></div></li>";}
    private String selectPage(String items){return "<div id=content><div class=view_title><p class=title>"+sample.title()+"</p></div><div class=view_box>소상공인 지원</div><div class='view_box file_area'><div class=file_tit><span class=tit>첨부파일</span></div><div class=file_cnt><ul class=file_list>"+items+"</ul></div></div></div>";}
    @Test void collectionBudgetStaysBounded(){var b=AnnouncementAttachmentBbsOfficialObservationTest.selectBudget(sample.profile(),true,false);assertThat(b.maximumRequests).isEqualTo(6);assertThat(b.maximumBytes).isEqualTo(23*1024*1024);}
    @Test void readsOfficialGetButtonsWithoutFetchingPreviews(){
        var r=sample.profile().selectDescriptors(sample.source(),selectPage(selectItem(1,"hwp")));assertThat(r.status()).isEqualTo("FOUND");assertThat(r.complete()).isTrue();
        assertThat(r.descriptors()).singleElement().satisfies(d->{assertThat(d.displayName()).isEqualTo("공고 1.hwp");assertThat(d.downloadAllowed()).isTrue();assertThat(d.documentRole()).isEqualTo("UNKNOWN");assertThat(d.postForm()).isEmpty();assertThat(sample.profile().selectApprovedRequest(d.fetchUri())).isTrue();assertThat(d.toString()).doesNotContain("공고","https");});
    }
    @Test void supportedUnsupportedAndMalformedFilesRemainSeparate(){
        var r=sample.profile().selectDescriptors(sample.source(),selectPage(selectItem(1,"hwp")+selectItem(2,"xlsx")+"<li><a href='/unknown'>알 수 없음</a></li>"));assertThat(r.complete()).isFalse();assertThat(r.descriptors()).hasSize(2);assertThat(r.descriptors().getFirst().downloadAllowed()).isTrue();assertThat(r.descriptors().getLast().downloadAllowed()).isFalse();
    }
    @Test void titleIdentityAndSeedCombinationAreRequired()throws Exception{
        var p=sample.profile();var s=sample.source();assertThat(p.selectDetailUri(s).toString()).isEqualTo(s.sourceUrl());
        assertThatThrownBy(()->p.selectDetailUri(new AttachmentDiscoveryProfile.Source(s.providerCode(),s.providerNoticeId(),s.sourceUrl(),"LGS-000001",s.listParserProfileCode()))).isInstanceOf(IllegalArgumentException.class);
        var page=Jsoup.parse(selectPage(selectItem(1,"hwp")));AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,sample.title(),sample.titleLayout());assertThatThrownBy(()->AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,"다른 제목",sample.titleLayout())).isInstanceOf(AssertionError.class);
        var d=new AnnouncementSourceClassificationEngine().selectDecision(new AnnouncementSourceClassificationInput("LOCAL_GOV_NOTICE",sample.title(),null,null,List.of(),AnnouncementSourceClassificationCodes.BodySourceCode.NONE,AnnouncementSourceClassificationCodes.BodyAvailabilityCode.UNAVAILABLE),AnnouncementAttachmentRealFileQaTest.selectDraftRuleSet());assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectTitleMayProceed(d)).isTrue();
    }
    @Test void distinguishesNoFilesMissingAreaAndLimit(){
        var p=sample.profile();assertThat(p.selectDescriptors(sample.source(),selectPage("")).status()).isEqualTo("NO_FILES");
        for(String bad:List.of("<html>오류</html>",selectPage("<li></li>"),selectPage("").replace("첨부파일","다른 영역"),selectPage("")+selectPage("")))assertThat(p.selectDescriptors(sample.source(),bad).complete()).isFalse();
        var r=p.selectDescriptors(sample.source(),selectPage(IntStream.rangeClosed(1,11).mapToObj(i->selectItem(i,"pdf")).collect(Collectors.joining())));assertThat(r.status()).isEqualTo("LIMIT_EXCEEDED");assertThat(r.descriptors()).hasSize(10);
    }
    @Test void rejectsExtendedJavascriptDifferentLabelsAndUnknownPreviews(){
        var p=sample.profile();String item=selectItem(1,"hwp");
        for(String bad:List.of(item.replace("_blank')","_blank');alert(1)"),item.replace("<span class=name>공고 1","<span class=name>다른 파일"),item.replace("eminwon.jangheung.go.kr","evil.go.kr"))){var r=p.selectDescriptors(sample.source(),selectPage(bad+selectItem(2,"hwp")));assertThat(r.complete()).isFalse();assertThat(r.descriptors()).hasSize(1);}
        var r=p.selectDescriptors(sample.source(),selectPage(item.replace("/Viewer_gosi/27081_1","/Viewer_gosi/27082_1")));assertThat(r.complete()).isFalse();assertThat(r.descriptors()).hasSize(1);
    }
    @Test void duplicateAndExtensionMismatchAreNotApproved(){
        var p=sample.profile();String item=selectItem(1,"hwp");assertThat(p.selectDescriptors(sample.source(),selectPage(item+item)).descriptors()).hasSize(1);
        var r=p.selectDescriptors(sample.source(),selectPage(item.replace("_20260128.hwp","_20260128.pdf")));
        assertThat(r.descriptors()).singleElement().satisfies(d->assertThat(d.downloadAllowed()).isFalse());
    }
    @Test void requestBoundaryRejectsPostQueryInjectionAndRedirect(){
        var p=sample.profile();var detail=p.selectDetailUri(sample.source());var file=p.selectDescriptors(sample.source(),selectPage(selectItem(1,"hwp"))).descriptors().getFirst().fetchUri();
        for(String bad:List.of(detail+"&idx=2",detail+"&unknown=x",detail.toString().replace("https:","http:"),file+"&extra=x",file.toString().replace("/20260128","/../20260128"),"https:/no-host"))assertThat(p.selectApprovedRequest(URI.create(bad))).isFalse();
        assertThat(p.selectApprovedRequest(new Request(file,"POST",Map.of("x","y")))).isFalse();assertThat(p.selectApprovedRequest(Request.selectGet(detail),Request.selectGet(file))).isFalse();
    }
    @Test void ignoresOutsideLinksButPreservesUnknownControlsInside(){
        String page=selectPage(selectItem(1,"hwp"));assertThat(sample.profile().selectDescriptors(sample.source(),page+"<a href='/print.pdf'>인쇄</a>").complete()).isTrue();
        for(String extra:List.of("<script>x</script>","<img src='/unknown'>","<input type=hidden>","<button>파일</button>")){var r=sample.profile().selectDescriptors(sample.source(),selectPage(selectItem(1,"hwp")+extra));assertThat(r.complete()).isFalse();assertThat(r.descriptors()).hasSize(1);}
    }
}
