package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import com.saneb.domain.announcementattachment.discovery.AttachmentDiscoveryProfile;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.classification.*;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.*;
import java.net.URI;
import java.util.*;
import java.util.stream.*;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class GyeongbukSixthDownloadContractTest {
    private static final String CSRF="a".repeat(64);
    private String selectLink(String g,int i,String ext){return switch(g){
        case "CHEONGSONG"->"<li><a href='https://eminwon.cs.go.kr/emwp/jsp/ofr/FileDown.jsp?user_file_nm=file"+i+"."+ext+"&sys_file_nm=saved"+i+"."+ext+"&file_path=/ntishome/file/upload/ofr/ofr/20260929'>file"+i+"."+ext+"</a></li>";
        case "ULLEUNG"->"<li><p class=file><a href='/programs/board/saeol/notice/download.do?file_seq="+i+"&not_ancmt_mgt_no=23003'>file"+i+"."+ext+"</a><a href='/programs/board/saeol/notice/fileView.do?file_seq="+i+"&not_ancmt_mgt_no=23003'>[미리보기]</a></p></li>";
        default->"<li><span class=file><span class=name>file"+i+"</span><span class=info>["+ext+"]</span></span><div><button type=button onclick=\"goDownLoad('file"+i+"."+ext+"','stored"+i+""+"A".repeat(24)+"','/ntisho"+"B".repeat(44)+"')\">다운로드</button></div></li>";};}
    private String selectPage(String g,String files){String t=GyeongbukSixthDownloadCases.selectCase(g).title();return switch(g){
        case "CHEONGSONG"->"<form id=saeolGosiVO><div class=board><div class=view><div class=title><h3>"+t+"</h3></div><dl class=attach><dt>첨부파일</dt><dd><ul>"+files+"</ul></dd></dl></div></div></form>";
        case "ULLEUNG"->"<div class=boardView><dl class=title><dt>"+t+"</dt><dd><ul>"+files+"</ul></dd></dl></div>";
        default->"<form name=nnn method=post action='https://eminwon.yyg.go.kr/emwp/jsp/ofr/FileDownNew.jsp'><input type=hidden name=csrf_token value='"+CSRF+"'><input type=hidden name=user_file_nm><input type=hidden name=sys_file_nm><input type=hidden name=file_path></form><div class=view_title><p class=title>"+t+"</p></div><div class='view_box file_area'><div class=file_tit><span class=tit>첨부파일</span></div><div class=file_cnt><ul class=file_list>"+files+"</ul></div></div>";};}
    @ParameterizedTest @ValueSource(strings={"CHEONGSONG","YEONGYANG","ULLEUNG"})
    void preservesGoodFilesAlongsideUnsupportedAndUnresolved(String g){var s=GyeongbukSixthDownloadCases.selectCase(g);var p=s.profile();var r=p.selectDescriptors(s.source(),"<a href='/outside.pdf'>outside.pdf</a>"+selectPage(g,selectLink(g,1,"hwp")+selectLink(g,2,"jpg")));assertThat(r.complete()).isTrue();assertThat(r.descriptors()).hasSize(2);assertThat(r.descriptors().getFirst().downloadAllowed()).isTrue();assertThat(r.descriptors().getLast().downloadAllowed()).isFalse();
        for(String bad:List.of("<a href='/unknown'>미확인</a>","<button>미확인</button>","<script>alert(1)</script>","<img src='/unknown'>")){var part=p.selectDescriptors(s.source(),selectPage(g,bad+selectLink(g,1,"hwp")));assertThat(part.complete()).isFalse();assertThat(part.descriptors()).hasSize(1);}
        r.descriptors().forEach(d->{assertThat(d.documentRole()).isEqualTo("UNKNOWN");assertThat(d.locator().toString()).doesNotContain(CSRF);assertThat(d.toString()).doesNotContain(CSRF);});
    }
    @ParameterizedTest @ValueSource(strings={"CHEONGSONG","YEONGYANG","ULLEUNG"})
    void limitsFilesAndRejectsConflict(String g){var s=GyeongbukSixthDownloadCases.selectCase(g);var p=s.profile();String one=selectLink(g,1,"pdf");assertThat(p.selectDescriptors(s.source(),selectPage(g,one+one)).descriptors()).hasSize(1);
        var conflict=p.selectDescriptors(s.source(),selectPage(g,one+one.replace("file1","other1")));assertThat(conflict.complete()).isFalse();assertThat(conflict.descriptors()).hasSize(1);
        var r=p.selectDescriptors(s.source(),selectPage(g,IntStream.rangeClosed(1,11).mapToObj(i->selectLink(g,i,"hwp")).collect(Collectors.joining())));assertThat(r.status()).isEqualTo("LIMIT_EXCEEDED");assertThat(r.descriptors()).hasSize(10);assertThat(p.selectDescriptors(s.source(),"<html/>").warnings()).contains("ATTACHMENT_SELECTOR_CHANGED");assertThat(p.selectDescriptors(s.source(),selectPage(g,"")).status()).isEqualTo("NO_FILES");
    }
    @ParameterizedTest @ValueSource(strings={"CHEONGSONG","YEONGYANG","ULLEUNG"})
    void validatesSourceTitleAndDraftSeed(String g)throws Exception{var s=GyeongbukSixthDownloadCases.selectCase(g);var p=s.profile();assertThat(p.selectDetailUri(s.source()).toString()).isEqualTo(s.source().sourceUrl());assertThatThrownBy(()->p.selectDetailUri(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE","bad",s.source().sourceUrl(),s.source().localSourceCode(),s.source().listParserProfileCode()))).hasMessage("PROFILE_REQUIRED");
        var doc=Jsoup.parse(selectPage(g,selectLink(g,1,"hwp")));AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(doc,s.title(),s.titleLayout());assertThatThrownBy(()->AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(doc,"다른 제목",s.titleLayout())).isInstanceOf(AssertionError.class);
        var r=new AnnouncementSourceClassificationEngine().selectDecision(new AnnouncementSourceClassificationInput("LOCAL_GOV_NOTICE",s.title(),null,null,List.of(),BodySourceCode.NONE,BodyAvailabilityCode.UNAVAILABLE),AnnouncementAttachmentRealFileQaTest.selectDraftRuleSet());assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectTitleMayProceed(r)).isTrue();
    }
    @ParameterizedTest @ValueSource(strings={"CHEONGSONG","YEONGYANG","ULLEUNG"})
    void rejectsUnsafeRequestsAndCrossFile(String g){var s=GyeongbukSixthDownloadCases.selectCase(g);var p=s.profile();String url=s.source().sourceUrl();for(String bad:List.of("https:/missing-host",url+"&unexpected=1",url+"&"+(g.equals("YEONGYANG")?"idx":"not_ancmt_mgt_no")+"=2",url.replace("https:","http:"),url.replace("www.","evil."),url+"#fragment"))assertThat(p.selectApprovedRequest(URI.create(bad))).isFalse();
        var a=p.selectDescriptors(s.source(),selectPage(g,selectLink(g,1,"hwp"))).descriptors().getFirst();var b=p.selectDescriptors(s.source(),selectPage(g,selectLink(g,2,"hwp"))).descriptors().getFirst();assertThat(p.selectApprovedRequest(a.selectRequest(),a.selectRequest())).isTrue();assertThat(p.selectApprovedRequest(a.selectRequest(),b.selectRequest())).isFalse();assertThat(p.selectApprovedRequest(new Request(a.fetchUri(),"POST",Map.of("x","y")))).isFalse();
    }
    @Test void validatesYeongyangFormAndLiteralCall(){var s=GyeongbukSixthDownloadCases.selectCase("YEONGYANG");String html=selectPage("YEONGYANG",selectLink("YEONGYANG",1,"hwp"));for(String bad:List.of(html.replace(CSRF,"invalid"),html.replace("method=post","method=get"),html.replace("eminwon.yyg.go.kr","evil.example"),html.replace("name=file_path","name=extra")))assertThat(s.profile().selectDescriptors(s.source(),bad).warnings()).contains("ATTACHMENT_DOWNLOAD_FORM_CHANGED");
        assertThat(s.profile().selectDescriptors(s.source(),html.replace("goDownLoad(","alert(1);goDownLoad(")).descriptors()).isEmpty();var r=s.profile().selectDescriptors(s.source(),html);assertThat(r.descriptors().getFirst().postForm()).hasSize(4);assertThat(r.descriptors().getFirst().selectRequest().toString()).doesNotContain(CSRF);
    }
    @Test void ulleungRedirectOnlyAllowsOfficialFixedDownload(){var s=GyeongbukSixthDownloadCases.selectCase("ULLEUNG");var p=s.profile();var first=p.selectDescriptors(s.source(),selectPage("ULLEUNG",selectLink("ULLEUNG",1,"hwp"))).descriptors().getFirst().selectRequest();String url="https://eminwon.ulleung.go.kr/emwp/jsp/ofr/FileDown.jsp?user_file_nm=file1.hwp&sys_file_nm=saved1.hwp&file_path=/ntishome/file/upload/ofr/ofr/20260929";assertThat(p.selectApprovedRequest(first,Request.selectGet(URI.create(url)))).isTrue();assertThat(p.selectApprovedRequest(first,Request.selectGet(URI.create(url.replace("eminwon.ulleung.go.kr","evil.example"))))).isFalse();assertThat(p.selectApprovedRequest(first,Request.selectGet(URI.create(url.replace("FileDown.jsp","Other.jsp"))))).isFalse();}
}
