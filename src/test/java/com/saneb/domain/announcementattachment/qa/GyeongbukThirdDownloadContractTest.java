package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import com.saneb.domain.announcementattachment.discovery.AttachmentDiscoveryProfile;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.classification.*;
import java.net.URI;
import java.util.*;
import java.util.stream.*;
import org.jsoup.Jsoup;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class GyeongbukThirdDownloadContractTest {
    @org.junit.jupiter.api.Test void yeongjuRedirectRequiresItsOfficialDownloadAndFixedPublicDirectory(){
        var s=GyeongbukThirdDownloadCases.selectCase("YEONGJU");var p=s.profile();var initial=p.selectDescriptors(s.source(),selectPage("YEONGJU",selectLink("YEONGJU",1,"pdf"))).descriptors().getFirst().selectRequest();
        var next=Request.selectGet(URI.create("https://eminwon.yeongju.go.kr/emwp/jsp/ofr/FileDown.jsp?user_file_nm=notice.pdf&sys_file_nm=stored.pdf&file_path=%2Fntishome%2Ffile%2Fupload%2Fofr%2Fofr%2F20260929"));
        assertThat(p.selectApprovedRequest(initial,next)).isTrue();assertThat(p.selectApprovedRequest(Request.selectGet(p.selectDetailUri(s.source())),next)).isFalse();assertThat(p.selectApprovedRequest(next,initial)).isFalse();
        for(String bad:List.of(next.uri().toString().replace("eminwon.yeongju.go.kr","evil.example"),next.uri()+"&extra=1",next.uri().toString().replace("20260929",".."),next.uri().toString().replace("https:","http:")))assertThat(p.selectApprovedRequest(initial,Request.selectGet(URI.create(bad)))).isFalse();
    }
    private String selectLink(String group,int file,String ext){
        if(group.equals("YECHEON"))return "<a href=\"javascript:goDownLoad('공고"+file+"."+ext+"','stored"+file+"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz','/ntishoABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz')\">공고"+file+"."+ext+"</a>";
        return "<a href='"+(group.equals("YEONGJU")?"/programs/board/saeol/notice/download.do?file_seq="+file+"&not_ancmt_mgt_no=24194":"/programs/board/board_download.do?file_uid="+file)+"'>공고"+file+"."+ext+(group.equals("SEONGJU")?" [112 KB]":"")+"</a>";
    }
    private String selectPage(String group,String links){
        String title=GyeongbukThirdDownloadCases.selectCase(group).title();
        return switch(group){
            case "YEONGJU"->"<div class=news_view><div class=data_top><h4>"+title+"</h4></div><div class=data_add><span><span>파일</span><span class=span_r>"+links+"</span></span></div></div>";
            case "SEONGJU"->"<form id=frm><div class=bod_view><h4>"+title+"</h4><dl class=view_file><dt>첨부 파일</dt><dd>"+links+"</dd></dl></div></form>";
            default->"<div class=km-view><form name=form1 method=post action='https://eminwon.ycg.kr/emwp/jsp/ofr/FileDownNew.jsp'><input type=hidden name=user_file_nm id=user_file_nm value=''><input type=hidden name=sys_file_nm id=sys_file_nm value=''><input type=hidden name=file_path id=file_path value=''></form><div class=km-view-title><strong>제목</strong><span class=content>"+title+"</span></div><div class='km-view-title km-view-file'><strong>파일</strong><ul class=eminwon-files>"+links+"</ul></div></div>";
        };
    }
    @ParameterizedTest @ValueSource(strings={"YEONGJU","SEONGJU","YECHEON"})
    void collectsKnownFilesAndSeparatesUnsupportedOrUnresolvedItems(String group){
        var s=GyeongbukThirdDownloadCases.selectCase(group);var p=s.profile();var r=p.selectDescriptors(s.source(),selectPage(group,selectLink(group,1,"hwp")+selectLink(group,2,"jpg")));
        assertThat(r.complete()).isTrue();assertThat(r.descriptors()).hasSize(2);assertThat(r.descriptors().getFirst().downloadAllowed()).isTrue();assertThat(r.descriptors().getLast().downloadAllowed()).isFalse();
        for(String extra:List.of("<a href='/unknown'>미확인</a>","<button>파일</button>","<script>alert(1)</script>","<img src='/unknown'>")){var partial=p.selectDescriptors(s.source(),selectPage(group,extra+selectLink(group,1,"pdf")));assertThat(partial.complete()).isFalse();assertThat(partial.descriptors()).hasSize(1);}
        r.descriptors().forEach(d->{assertThat(d.documentRole()).isEqualTo("UNKNOWN");assertThat(d.toString()).doesNotContain("공고","http","stored");assertThat(p.selectApprovedRequest(d.selectRequest())).isTrue();});
    }
    @ParameterizedTest @ValueSource(strings={"YEONGJU","SEONGJU","YECHEON"})
    void distinguishesEmptyMissingConflictingAndExcessFiles(String group){
        var s=GyeongbukThirdDownloadCases.selectCase(group);var p=s.profile();assertThat(p.selectDescriptors(s.source(),selectPage(group,"")).status()).isEqualTo("NO_FILES");assertThat(p.selectDescriptors(s.source(),"<html>오류</html>").complete()).isFalse();assertThat(p.selectDescriptors(s.source(),selectPage(group,"<li></li>")).complete()).isFalse();
        String link=selectLink(group,1,"hwpx");assertThat(p.selectDescriptors(s.source(),selectPage(group,link+link)).descriptors()).hasSize(1);assertThat(p.selectDescriptors(s.source(),selectPage(group,link+link.replace("공고1","다른파일"))).complete()).isFalse();
        var limit=p.selectDescriptors(s.source(),selectPage(group,IntStream.rangeClosed(1,11).mapToObj(i->selectLink(group,i,"hwp")).collect(Collectors.joining())));assertThat(limit.status()).isEqualTo("LIMIT_EXCEEDED");assertThat(limit.descriptors()).hasSize(10);
    }
    @ParameterizedTest @ValueSource(strings={"YEONGJU","SEONGJU","YECHEON"})
    void verifiesSourceTitleAndDraftEligibilityWithoutPolicyApproval(String group)throws Exception{
        var s=GyeongbukThirdDownloadCases.selectCase(group);var p=s.profile();assertThat(p.selectDetailUri(s.source()).toString()).isEqualTo(s.source().sourceUrl());
        for(var bad:List.of(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",s.source().providerNoticeId(),s.source().sourceUrl(),"LGS-000001",s.source().listParserProfileCode()),new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE","bad",s.source().sourceUrl(),s.source().localSourceCode(),s.source().listParserProfileCode())))assertThatThrownBy(()->p.selectDetailUri(bad)).hasMessage("PROFILE_REQUIRED");
        AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(Jsoup.parse(selectPage(group,"")),s.title(),s.titleLayout());
        var d=new AnnouncementSourceClassificationEngine().selectDecision(new AnnouncementSourceClassificationInput("LOCAL_GOV_NOTICE",s.title(),null,null,List.of(),AnnouncementSourceClassificationCodes.BodySourceCode.NONE,AnnouncementSourceClassificationCodes.BodyAvailabilityCode.UNAVAILABLE),AnnouncementAttachmentRealFileQaTest.selectDraftRuleSet());assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectTitleMayProceed(d)).isTrue();
    }
    @ParameterizedTest @ValueSource(strings={"YEONGJU","SEONGJU","YECHEON"})
    void rejectsRequestInjectionAndKeepsRequestsBound(String group){
        var s=GyeongbukThirdDownloadCases.selectCase(group);var p=s.profile();String url=s.source().sourceUrl();
        for(String bad:List.of("https:/missing-host",url+"&extra=1",url.replace("https:","http:"),url.replace("www.","evil."),url+"#fragment",url+"&"+(group.equals("YECHEON")?"id":"mnu_uid")+"=2"))assertThat(p.selectApprovedRequest(URI.create(bad))).isFalse();
        var a=p.selectDescriptors(s.source(),selectPage(group,selectLink(group,1,"hwp"))).descriptors().getFirst();var b=p.selectDescriptors(s.source(),selectPage(group,selectLink(group,2,"hwp"))).descriptors().getFirst();assertThat(p.selectApprovedRequest(a.selectRequest(),a.selectRequest())).isTrue();assertThat(p.selectApprovedRequest(a.selectRequest(),b.selectRequest())).isFalse();assertThat(p.selectApprovedRequest(new Request(a.fetchUri(),"POST",Map.of("x","y")))).isFalse();
        assertThat(p.selectDescriptors(s.source(),selectPage(group,selectLink(group,1,"hwp").replace("<a ","<a onclick='alert(1)' "))).descriptors()).isEmpty();
        assertThat(p.selectDescriptors(s.source(),selectPage(group,selectLink(group,1,"hwp"))+"<a href='/outside.pdf'>외부</a>").complete()).isTrue();
    }
    @org.junit.jupiter.api.Test void onlyRecognizesPairedPreviews(){
        for(String group:List.of("YEONGJU","SEONGJU")){var s=GyeongbukThirdDownloadCases.selectCase(group);var p=s.profile();String preview=group.equals("YEONGJU")?"<a href='/programs/board/saeol/notice/fileView.do?file_seq=1&not_ancmt_mgt_no=24194'>[미리보기]</a>":"<a href='#self' onclick='openViewFiles(1)'>[미리보기]</a>";
            assertThat(p.selectDescriptors(s.source(),selectPage(group,selectLink(group,1,"hwp")+preview)).complete()).isTrue();assertThat(p.selectDescriptors(s.source(),selectPage(group,preview)).complete()).isFalse();assertThat(p.selectDescriptors(s.source(),selectPage(group,selectLink(group,1,"hwp")+preview.replace("1","2"))).complete()).isFalse();}
    }
    @org.junit.jupiter.api.Test void yecheonRequiresExactPublicFormAndNeverExecutesScript(){
        var s=GyeongbukThirdDownloadCases.selectCase("YECHEON");String page=selectPage("YECHEON",selectLink("YECHEON",1,"hwp"));
        for(String bad:List.of(page.replace("eminwon.ycg.kr","evil.example"),page.replace("name=form1","name=other"),page.replace("value=''","value='injected'"),page.replace("')\"", "');alert(1);\""),page.replace("/ntisho","/etc/"))){assertThat(bad).isNotEqualTo(page);assertThat(s.profile().selectDescriptors(s.source(),bad).complete()).isFalse();}
    }
    @org.junit.jupiter.api.Test void seongjuMapsOnlyTheObservedListTransition(){
        var s=GyeongbukThirdDownloadCases.selectCase("SEONGJU");String url=s.source().sourceUrl().replace("cmd=258","cmd=2")+"&&srchEnable=1&srchColumn=bod_title&srchKwd=&pageNo=1&";var n=new com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer();
        var source=new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(url)),url,s.source().localSourceCode(),s.source().listParserProfileCode());assertThat(s.profile().selectDetailUri(source).toString()).isEqualTo(s.source().sourceUrl());
    }
}
