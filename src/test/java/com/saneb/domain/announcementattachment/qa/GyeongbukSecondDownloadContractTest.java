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

class GyeongbukSecondDownloadContractTest {
    @org.junit.jupiter.api.Test void uiseongRedirectIsLimitedToItsOfficialDownloadAndPublicFileDirectory(){
        var s=GyeongbukSecondDownloadCases.selectCase("UISEONG");var p=s.profile();var initial=p.selectDescriptors(s.source(),selectPage("UISEONG",selectLink("UISEONG",1,"hwp"))).descriptors().getFirst().selectRequest();
        var next=Request.selectGet(URI.create("https://eminwon.uiseong.go.kr/emwp/jsp/ofr/FileDown.jsp?user_file_nm=notice.hwp&sys_file_nm=stored.hwp&file_path=%2Fntishome%2Ffile%2Fupload%2Fofr%2Fofr%2F20260929"));
        assertThat(p.selectApprovedRequest(initial,next)).isTrue();assertThat(p.selectApprovedRequest(Request.selectGet(p.selectDetailUri(s.source())),next)).isFalse();
        assertThat(p.selectApprovedRequest(next,initial)).isFalse();
        for(String bad:List.of(next.uri().toString().replace("eminwon.uiseong.go.kr","evil.example"),next.uri()+"&extra=1",next.uri().toString().replace("20260929",".."),next.uri().toString().replace("https:","http:")))assertThat(p.selectApprovedRequest(initial,Request.selectGet(URI.create(bad)))).isFalse();
        assertThat(p.selectUtf8DispositionOctets()).isFalse();assertThat(GyeongbukSecondDownloadCases.selectCase("GYEONGJU").profile().selectUtf8DispositionOctets()).isTrue();assertThat(GyeongbukSecondDownloadCases.selectCase("GYEONGSAN").profile().selectUtf8DispositionOctets()).isTrue();
    }
    private String selectLink(String group,int file,String ext){
        String id=GyeongbukSecondDownloadCases.selectCase(group).code().split("-")[1];
        if(group.equals("UISEONG"))return "<a href='/programs/board/saeol/notice/download.do?file_seq="+file+"&not_ancmt_mgt_no="+id+"'>공고"+file+"."+ext+"</a>";
        return "<a class=clsFileDownload id=downFiles_"+file+" href='#downFiles_"+file+"' onclick=\"openDownloadFiles("+file+(group.equals("GYEONGSAN")?","+id+",2160":"")+");return false;\">공고"+file+"."+ext+"</a>";
    }
    private String selectPage(String group,String links){
        String title=GyeongbukSecondDownloadCases.selectCase(group).title();
        return group.equals("UISEONG")?"<div class=boardView><dl class=title><dt>"+title+"</dt><dd><ul>"+links+"</ul></dd></dl></div>":"<div id=viewBoardContent class=board_view><h4 class=view_tle>"+title+"</h4><dl class=view_file><dt>파일</dt><dd><ul>"+links+"</ul></dd></dl></div>";
    }
    private String selectPreview(String group,int file){String id=GyeongbukSecondDownloadCases.selectCase(group).code().split("-")[1];return group.equals("UISEONG")?"<a href='#' onclick=\"return openSynap(this, '"+file+"', '"+id+"');\">[미리보기]</a>":"<a id=viewFiles_"+file+" href='#viewFiles_"+file+"' onclick=\"openViewFiles("+file+","+id+");return false;\"><img src='/design/common/img/board/btn_fileview.png' alt=바로보기></a>";}
    @ParameterizedTest @ValueSource(strings={"GYEONGJU","GYEONGSAN","UISEONG"})
    void collectsKnownFilesWhilePreservingIndividualProblems(String group){
        var s=GyeongbukSecondDownloadCases.selectCase(group);var p=s.profile();var result=p.selectDescriptors(s.source(),selectPage(group,selectLink(group,1,"hwp")+selectLink(group,2,"xlsx")));
        assertThat(result.complete()).isTrue();assertThat(result.descriptors()).hasSize(2);assertThat(result.descriptors().getFirst().downloadAllowed()).isTrue();assertThat(result.descriptors().getLast().downloadAllowed()).isFalse();
        for(String extra:List.of("<a href='/unknown'>미확인</a>","<button>파일</button>","<script>alert(1)</script>","<img src='/unknown'>")){var partial=p.selectDescriptors(s.source(),selectPage(group,extra+selectLink(group,1,"pdf")));assertThat(partial.complete()).isFalse();assertThat(partial.descriptors()).hasSize(1);}
        result.descriptors().forEach(d->{assertThat(d.documentRole()).isEqualTo("UNKNOWN");assertThat(d.toString()).doesNotContain("공고","http");assertThat(p.selectApprovedRequest(d.selectRequest())).isTrue();});
    }
    @ParameterizedTest @ValueSource(strings={"GYEONGJU","GYEONGSAN","UISEONG"})
    void distinguishesAbsentAreaEmptyAreaConflictsAndLimits(String group){
        var s=GyeongbukSecondDownloadCases.selectCase(group);var p=s.profile();assertThat(p.selectDescriptors(s.source(),selectPage(group,"")).status()).isEqualTo("NO_FILES");assertThat(p.selectDescriptors(s.source(),"<html>오류</html>").complete()).isFalse();assertThat(p.selectDescriptors(s.source(),selectPage(group,"<li></li>")).complete()).isFalse();
        String link=selectLink(group,1,"hwpx");assertThat(p.selectDescriptors(s.source(),selectPage(group,link+link)).descriptors()).hasSize(1);assertThat(p.selectDescriptors(s.source(),selectPage(group,link+link.replace("공고1","다른파일"))).complete()).isFalse();
        var limit=p.selectDescriptors(s.source(),selectPage(group,IntStream.rangeClosed(1,11).mapToObj(i->selectLink(group,i,"hwp")).collect(Collectors.joining())));assertThat(limit.status()).isEqualTo("LIMIT_EXCEEDED");assertThat(limit.descriptors()).hasSize(10);
    }
    @ParameterizedTest @ValueSource(strings={"GYEONGJU","GYEONGSAN","UISEONG"})
    void verifiesSourceTitleAndDraftEligibilityWithoutApproval(String group)throws Exception{
        var s=GyeongbukSecondDownloadCases.selectCase(group);var p=s.profile();assertThat(p.selectDetailUri(s.source()).toString()).isEqualTo(s.source().sourceUrl());
        assertThatThrownBy(()->p.selectDetailUri(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",s.source().providerNoticeId(),s.source().sourceUrl(),"LGS-000001",s.source().listParserProfileCode()))).hasMessage("PROFILE_REQUIRED");
        assertThatThrownBy(()->p.selectDetailUri(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE","invalid",s.source().sourceUrl(),s.source().localSourceCode(),s.source().listParserProfileCode()))).hasMessage("PROFILE_REQUIRED");
        var page=Jsoup.parse(selectPage(group,selectLink(group,1,"hwp")));AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,s.title(),s.titleLayout());assertThatThrownBy(()->AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,"다른 공고",s.titleLayout())).isInstanceOf(AssertionError.class);
        var d=new AnnouncementSourceClassificationEngine().selectDecision(new AnnouncementSourceClassificationInput("LOCAL_GOV_NOTICE",s.title(),null,null,List.of(),AnnouncementSourceClassificationCodes.BodySourceCode.NONE,AnnouncementSourceClassificationCodes.BodyAvailabilityCode.UNAVAILABLE),AnnouncementAttachmentRealFileQaTest.selectDraftRuleSet());assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectTitleMayProceed(d)).isTrue();
    }
    @ParameterizedTest @ValueSource(strings={"GYEONGJU","GYEONGSAN","UISEONG"})
    void keepsOfficialListQueryContextAndRejectsRequestExtensions(String group){
        var s=GyeongbukSecondDownloadCases.selectCase(group);var p=s.profile();String url=s.source().sourceUrl();
        String context=group.equals("UISEONG")?"&board_code=&srchSDate=&srchKwd=&srchColumn=title&srchDept=&srchEDate=&pageNo=1":"&pageNo=1&pagePrvNxt=1&pageRef=0&pageOrder=0&srchVoteType=-1&parm_mnu_uid=0&srchEnable=1&srchBgpUid=-1&srchKeyword=&srchSDate=1960-01-01&srchColumn=bod_title&srchEDate=9999-12-31&";
        assertThat(p.selectApprovedRequest(URI.create(url+context))).isTrue();
        for(String bad:List.of("https:/missing-host",url+"&mnu_uid=2",url+"&extra=1",url.replace("https:","http:"),url.replace("www.","evil."),url.replace("/page.do","/%70age.do"),url+"#fragment"))assertThat(p.selectApprovedRequest(URI.create(bad))).isFalse();
        var a=p.selectDescriptors(s.source(),selectPage(group,selectLink(group,1,"hwp"))).descriptors().getFirst();var b=p.selectDescriptors(s.source(),selectPage(group,selectLink(group,2,"hwp"))).descriptors().getFirst();assertThat(p.selectApprovedRequest(a.selectRequest(),a.selectRequest())).isTrue();assertThat(p.selectApprovedRequest(a.selectRequest(),b.selectRequest())).isFalse();assertThat(p.selectApprovedRequest(new Request(a.fetchUri(),"POST",Map.of("x","y")))).isFalse();
    }
    @ParameterizedTest @ValueSource(strings={"GYEONGJU","GYEONGSAN","UISEONG"})
    void recognizesOnlyThePairedPreviewAndNeverExecutesIt(String group){
        var s=GyeongbukSecondDownloadCases.selectCase(group);var p=s.profile();String link=selectLink(group,1,"hwp"),preview=selectPreview(group,1);var good=p.selectDescriptors(s.source(),selectPage(group,"<li>"+link+preview+"</li>"));assertThat(good.complete()).isTrue();assertThat(good.descriptors()).hasSize(1);
        for(String bad:List.of(selectPreview(group,2),preview.replace(s.code().split("-")[1],"999999"),preview.replace(";\"", ";alert(1);\""))){var result=p.selectDescriptors(s.source(),selectPage(group,"<li>"+link+bad+"</li>"));assertThat(result.complete()).isFalse();assertThat(result.descriptors()).hasSize(1);}
        assertThat(p.selectDescriptors(s.source(),selectPage(group,preview)).complete()).isFalse();
    }
    @ParameterizedTest @ValueSource(strings={"GYEONGJU","GYEONGSAN","UISEONG"})
    void rejectsScriptInjectionWrongFileAndCrossNoticeLinks(String group){
        var s=GyeongbukSecondDownloadCases.selectCase(group);var p=s.profile();String link=selectLink(group,1,"hwp");
        String bad=group.equals("UISEONG")?link.replace("<a ","<a onclick='alert(1)' "):link.replace("return false;","alert(1);return false;");assertThat(p.selectDescriptors(s.source(),selectPage(group,bad)).descriptors()).isEmpty();
        if(!group.equals("GYEONGJU")){var changed=link.replace(s.code().split("-")[1],"999999");assertThat(p.selectDescriptors(s.source(),selectPage(group,changed)).descriptors()).isEmpty();}
        else assertThat(p.selectDescriptors(s.source(),selectPage(group,link.replace("href='#downFiles_1'","href='#downFiles_2'"))).descriptors()).isEmpty();
        assertThat(p.selectDescriptors(s.source(),selectPage(group,link)+"<a href='/outside.pdf'>외부 링크</a>").complete()).isTrue();
    }
}
