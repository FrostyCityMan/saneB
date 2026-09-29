package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import com.saneb.domain.announcementattachment.discovery.AttachmentDiscoveryProfile;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.classification.*;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.*;
import java.net.URI;
import java.util.*;
import java.util.stream.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class GyeongbukFifthDownloadContractTest {
    private String selectLink(String group,int i,String ext){String url=group.equals("YEONGDEOK")?"https://www.yd.go.kr/?action=kboard_file_download&uid=366709&file=file"+i:"https://eminwon.uljin.go.kr/emwp/jsp/ofr/FileDown.jsp?file_path=/ntishome/file/upload/ofr/ofr/20260929&sys_file_nm=saved"+i+"."+ext+"&user_file_nm=file"+i+"."+ext;return "<a href='"+url+"'>file"+i+"."+ext+"</a>";}
    private String selectPage(String group,String files){String title=GyeongbukFifthDownloadCases.selectCase(group).title();return group.equals("YEONGDEOK")?"<div class=kboard-document-wrap><div class=kboard-title>"+title+"</div><div class=kboard-attach>첨부파일 : "+files+"</div></div>":"<table class=bbs_tablev><tbody><tr><th>제목</th><td>"+title+"</td></tr><tr><th>첨부파일</th><td>"+files+"</td></tr></tbody></table>";}
    @ParameterizedTest @ValueSource(strings={"YEONGDEOK","ULJIN"})
    void collectsOnlyOfficialAreaAndKeepsPartialFiles(String group){var s=GyeongbukFifthDownloadCases.selectCase(group);var p=s.profile();
        var r=p.selectDescriptors(s.source(),"<nav><a href='/outside.pdf'>outside.pdf</a></nav>"+selectPage(group,selectLink(group,1,"hwp")+selectLink(group,2,"jpg")));assertThat(r.complete()).isTrue();assertThat(r.descriptors()).hasSize(2);assertThat(r.descriptors().getFirst().downloadAllowed()).isTrue();assertThat(r.descriptors().getLast().downloadAllowed()).isFalse();
        for(String bad:List.of("<a href='/unknown'>미리보기</a>","<script>alert(1)</script>","<button>다운로드</button>","<img src='/x'>")){var partial=p.selectDescriptors(s.source(),selectPage(group,bad+selectLink(group,1,"hwp")));assertThat(partial.complete()).isFalse();assertThat(partial.warnings()).contains("ATTACHMENT_LINK_UNRESOLVED");assertThat(partial.descriptors()).hasSize(1);}
        r.descriptors().forEach(d->assertThat(d.documentRole()).isEqualTo("UNKNOWN"));
    }
    @ParameterizedTest @ValueSource(strings={"YEONGDEOK","ULJIN"})
    void rejectsUnsafeRequestsAndCrossNotice(String group){var s=GyeongbukFifthDownloadCases.selectCase(group);var p=s.profile();String url=s.source().sourceUrl();
        for(String bad:List.of("https:/missing-host",url+"&unexpected=1",url+"&"+(group.equals("YEONGDEOK")?"uid":"ancmtMgtNo")+"=2",url.replace("https:","http:"),url.replace("www.","evil."),url+"#fragment",url.replace("https://","https://user@")))assertThat(p.selectApprovedRequest(URI.create(bad))).isFalse();
        var a=p.selectDescriptors(s.source(),selectPage(group,selectLink(group,1,"hwp"))).descriptors().getFirst();var b=p.selectDescriptors(s.source(),selectPage(group,selectLink(group,2,"hwp"))).descriptors().getFirst();assertThat(p.selectApprovedRequest(a.selectRequest(),a.selectRequest())).isTrue();assertThat(p.selectApprovedRequest(a.selectRequest(),b.selectRequest())).isFalse();assertThat(p.selectApprovedRequest(new Request(a.fetchUri(),"POST",Map.of("x","y")))).isFalse();
        String bad=group.equals("YEONGDEOK")?selectLink(group,1,"hwp").replace("uid=366709","uid=2"):selectLink(group,1,"hwp").replace("user_file_nm=file1","user_file_nm=other");assertThat(p.selectDescriptors(s.source(),selectPage(group,bad)).descriptors()).isEmpty();
        assertThatThrownBy(()->p.selectDetailUri(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE","bad",url,s.source().localSourceCode(),s.source().listParserProfileCode()))).isInstanceOf(IllegalArgumentException.class);
    }
    @ParameterizedTest @ValueSource(strings={"YEONGDEOK","ULJIN"})
    void limitsDeduplicatesAndDoesNotClaimMissingSelectorAsNoFiles(String group){var s=GyeongbukFifthDownloadCases.selectCase(group);var p=s.profile();String one=selectLink(group,1,"pdf");assertThat(p.selectDescriptors(s.source(),selectPage(group,one+one)).descriptors()).hasSize(1);
        var conflict=p.selectDescriptors(s.source(),selectPage(group,one+one.replace("file1.pdf</a>","other.pdf</a>")));assertThat(conflict.complete()).isFalse();assertThat(conflict.descriptors()).hasSize(1);
        var limit=p.selectDescriptors(s.source(),selectPage(group,IntStream.rangeClosed(1,11).mapToObj(i->selectLink(group,i,"hwpx")).collect(Collectors.joining())));assertThat(limit.status()).isEqualTo("LIMIT_EXCEEDED");assertThat(limit.descriptors()).hasSize(10);
        assertThat(p.selectDescriptors(s.source(),"<html></html>").warnings()).contains("ATTACHMENT_SELECTOR_CHANGED");assertThat(p.selectDescriptors(s.source(),selectPage(group,"")).status()).isEqualTo("NO_FILES");
    }
    @ParameterizedTest @ValueSource(strings={"YEONGDEOK","ULJIN"})
    void seededTitlePolicyStillAllowsTheFixedSample(String group)throws Exception{var s=GyeongbukFifthDownloadCases.selectCase(group);var r=new AnnouncementSourceClassificationEngine().selectDecision(new AnnouncementSourceClassificationInput("LOCAL_GOV_NOTICE",s.title(),null,null,List.of(),BodySourceCode.NONE,BodyAvailabilityCode.UNAVAILABLE),AnnouncementAttachmentRealFileQaTest.selectDraftRuleSet());assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectTitleMayProceed(r)).isTrue();}
}
