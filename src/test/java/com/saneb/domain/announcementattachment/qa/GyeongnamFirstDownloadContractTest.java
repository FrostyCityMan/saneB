package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import com.saneb.domain.announcementattachment.discovery.AttachmentDiscoveryProfile;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.classification.*;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.*;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.*;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class GyeongnamFirstDownloadContractTest {
    private String selectInner(int i,String ext){return "http://eminwon.jinju.go.kr/emwp/jsp/ofr/FileDown.jsp?user_file_nm=file"+i+"."+ext+"&sys_file_nm=saved"+i+"."+ext+"&file_path=/ntishome/file/upload/ofr/ofr/20260929";}
    private String selectProxy(String inner,String name){return "https://www.jinju.go.kr/DownloadEx.do?url="+URLEncoder.encode(inner,StandardCharsets.UTF_8)+"&name="+name;}
    private String selectLink(String g,int i,String ext){return g.equals("GORYEONG")?"<p><a class=bV_file href='/front/viewFile.do?IDX_FI="+i+"&BRD_ID=1023'><img src='/icon.png' alt='첨부파일'>file"+i+"."+ext+" ["+ext+", 100.0KB]</a><a href='/front/downFile.do?IDX_FI="+i+"&BRD_ID=1023'>다운로드</a></p>":"<li><a href='"+selectProxy(selectInner(i,ext),"file"+i+"."+ext)+"'>file"+i+"."+ext+"</a></li>";}
    private String selectPage(String g,String files){String title=GyeongnamFirstDownloadCases.selectCase(g).title();return g.equals("GORYEONG")?"<table class=boardView_table><tbody><tr><th scope=col colspan=4>"+title+"<span class=bV_date>2026-09-29</span></th></tr><tr><th>첨부</th><td>"+files+"</td></tr></tbody></table>":"<div class=bbs1view1><h1 class=h1>"+title+"</h1><div class=attach1><ul>"+files+"</ul></div></div>";}
    @ParameterizedTest @ValueSource(strings={"GORYEONG","JINJU"})
    void preservesGoodFilesAndSeparatesErrors(String g){var s=GyeongnamFirstDownloadCases.selectCase(g);var p=s.profile();var r=p.selectDescriptors(s.source(),"<nav><a href='/outside.pdf'>outside.pdf</a></nav>"+selectPage(g,selectLink(g,1,"hwp")+selectLink(g,2,"jpg")));assertThat(r.complete()).isTrue();assertThat(r.descriptors()).hasSize(2);assertThat(r.descriptors().getFirst().downloadAllowed()).isTrue();assertThat(r.descriptors().getLast().downloadAllowed()).isFalse();
        for(String bad:List.of("<a href='/unknown'>미확인</a>","<button>첨부</button>","<script>loading_convert()</script>","<img src='/other'>")){var part=p.selectDescriptors(s.source(),selectPage(g,bad+selectLink(g,1,"hwpx")));assertThat(part.complete()).isFalse();assertThat(part.descriptors()).hasSize(1);}r.descriptors().forEach(d->assertThat(d.documentRole()).isEqualTo("UNKNOWN"));
    }
    @ParameterizedTest @ValueSource(strings={"GORYEONG","JINJU"})
    void limitsAndDeduplicatesWithoutClaimingMissingAreaAsNoFiles(String g){var s=GyeongnamFirstDownloadCases.selectCase(g);var p=s.profile();String one=selectLink(g,1,"hwp");assertThat(p.selectDescriptors(s.source(),selectPage(g,one+one)).descriptors()).hasSize(1);var conflict=p.selectDescriptors(s.source(),selectPage(g,one+one.replace("file1","other1")));assertThat(conflict.complete()).isFalse();assertThat(conflict.descriptors()).hasSize(1);
        var limit=p.selectDescriptors(s.source(),selectPage(g,IntStream.rangeClosed(1,11).mapToObj(i->selectLink(g,i,"hwp")).collect(Collectors.joining())));assertThat(limit.status()).isEqualTo("LIMIT_EXCEEDED");assertThat(limit.descriptors()).hasSize(10);assertThat(p.selectDescriptors(s.source(),"<html/>").warnings()).contains("ATTACHMENT_SELECTOR_CHANGED");assertThat(p.selectDescriptors(s.source(),selectPage(g,"")).status()).isEqualTo("NO_FILES");
    }
    @ParameterizedTest @ValueSource(strings={"GORYEONG","JINJU"})
    void sourceTitleAndDraftSeedRemainBound(String g)throws Exception{var s=GyeongnamFirstDownloadCases.selectCase(g);var p=s.profile();assertThat(p.selectDetailUri(s.source()).getScheme()).isEqualTo("https");assertThatThrownBy(()->p.selectDetailUri(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE","bad",s.source().sourceUrl(),s.source().localSourceCode(),s.source().listParserProfileCode()))).hasMessage("PROFILE_REQUIRED");var page=Jsoup.parse(selectPage(g,selectLink(g,1,"hwp")));AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,s.title(),s.titleLayout());assertThatThrownBy(()->AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,"다른 제목",s.titleLayout())).isInstanceOf(AssertionError.class);
        var r=new AnnouncementSourceClassificationEngine().selectDecision(new AnnouncementSourceClassificationInput("LOCAL_GOV_NOTICE",s.title(),null,null,List.of(),BodySourceCode.NONE,BodyAvailabilityCode.UNAVAILABLE),AnnouncementAttachmentRealFileQaTest.selectDraftRuleSet());assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectTitleMayProceed(r)).isTrue();
    }
    @ParameterizedTest @ValueSource(strings={"GORYEONG","JINJU"})
    void rejectsUnsafeRequestsAndRedirects(String g){var s=GyeongnamFirstDownloadCases.selectCase(g);var p=s.profile();String url=p.selectDetailUri(s.source()).toString();for(String bad:List.of("https:/missing-host",url+"&extra=1",url+"&"+(g.equals("GORYEONG")?"BOARD_IDX":"not_ancmt_mgt_no")+"=2",url.replace("https:","http:"),url.replace("www.","evil."),url+"#fragment"))assertThat(p.selectApprovedRequest(URI.create(bad))).isFalse();var a=p.selectDescriptors(s.source(),selectPage(g,selectLink(g,1,"hwp"))).descriptors().getFirst();var b=p.selectDescriptors(s.source(),selectPage(g,selectLink(g,2,"hwp"))).descriptors().getFirst();assertThat(p.selectApprovedRequest(a.selectRequest(),a.selectRequest())).isTrue();assertThat(p.selectApprovedRequest(a.selectRequest(),b.selectRequest())).isFalse();assertThat(p.selectApprovedRequest(new Request(a.fetchUri(),"POST",Map.of("x","y")))).isFalse();}
    @Test void proxyCannotFetchArbitraryInnerUrls(){var p=GyeongnamFirstDownloadCases.selectCase("JINJU").profile();for(String inner:List.of("/relative",selectInner(1,"hwp").replace("eminwon.jinju.go.kr","127.0.0.1"),selectInner(1,"hwp").replace("FileDown.jsp","Other.jsp"),selectInner(1,"hwp")+"&extra=1",selectInner(1,"hwp").replace("http://","http://user@"),selectInner(1,"hwp")+"#fragment"))assertThat(p.selectApprovedRequest(URI.create(selectProxy(inner,"file1.hwp")))).isFalse();assertThat(p.selectApprovedRequest(URI.create(selectProxy(selectInner(1,"hwp"),"other.hwp")))).isFalse();assertThat(p.selectApprovedRequest(URI.create(selectProxy(selectInner(1,"hwp"),"file1.hwp")))).isTrue();}
    @Test void goryeongPreviewPairMustUseSameBoardAndFile(){var s=GyeongnamFirstDownloadCases.selectCase("GORYEONG");String html=selectPage("GORYEONG",selectLink("GORYEONG",1,"hwp"));for(String bad:List.of(html.replace("viewFile.do?IDX_FI=1","viewFile.do?IDX_FI=2"),html.replace("BRD_ID=1023","BRD_ID=999"),html.replace("다운로드</a>","실행</a>")))assertThat(s.profile().selectDescriptors(s.source(),bad).descriptors()).isEmpty();}
}
