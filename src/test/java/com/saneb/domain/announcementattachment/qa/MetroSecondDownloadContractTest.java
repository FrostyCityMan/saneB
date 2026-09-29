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
import org.junit.jupiter.params.provider.MethodSource;

class MetroSecondDownloadContractTest {
    static Stream<String> selectGroups() { return MetroSecondDownloadCases.GROUPS.stream().sorted(); }
    private String selectLink(int id, String ext) { return "<a href=\"javascript:goDownLoad('file"+id+"."+ext+"','saved"+id+"."+ext+"','/ntishome/file/upload/ofr/ofr/20260929')\">file"+id+"."+ext+"</a>"; }
    private String selectPage(String group, String links) {
        String title = MetroSecondDownloadCases.selectCase(group).title();
        return switch(group) {
            case "GWANGJU_DONGGU" -> "<form name=form1 method=post><div class=tstyle_view><div class=title>"+title+"</div><div class=add_file><strong>첨부파일</strong><div>"+links+"</div></div></div></form>";
            case "GWANGJU_BUKGU" -> "<form name=form method=post><div class=board_read_wrap><div class=board_read><h3 class=title>"+title+"</h3><dl class=info_basic><dt>첨부파일 : </dt><dd>"+links+"</dd></dl></div></div></form>";
            default -> "<form name=form1 method=post><table width='100%' border=0 cellspacing=1 cellpadding=0><tr><th>제목</th><td>"+title+"</td></tr><tr><td><table><tr><td>첨부파일 :</td><td>"+links+"</td></tr></table></td></tr></table></form>";
        };
    }
    @ParameterizedTest @MethodSource("selectGroups") void keepsGoodFilesAndSeparatesUnknownLinks(String group) {
        var s=MetroSecondDownloadCases.selectCase(group); String links=selectLink(1,"hwp")+selectLink(2,"hwpx")+selectLink(3,"pdf")+selectLink(4,"jpg");
        var r=s.profile().selectDescriptors(s.source(),"<nav><a href='/noise.pdf'>noise.pdf</a></nav>"+selectPage(group,links));
        assertThat(r.complete()).isTrue();assertThat(r.descriptors()).hasSize(4);assertThat(r.descriptors().stream().filter(AttachmentDiscoveryProfile.Descriptor::downloadAllowed)).hasSize(3);
        r.descriptors().forEach(d->{assertThat(d.documentRole()).isEqualTo("UNKNOWN");assertThat(s.profile().selectApprovedRequest(d.selectRequest())).isTrue();});
        for(String unknown:List.of("<a href='/other'>미확인</a>","<script>bad()</script>","<button>파일</button>","<img src='/unknown'>")){var partial=s.profile().selectDescriptors(s.source(),selectPage(group,links+unknown));assertThat(partial.complete()).isFalse();assertThat(partial.descriptors()).hasSize(4);}
    }
    @ParameterizedTest @MethodSource("selectGroups") void bindsSourceTitleAndDraftSeed(String group) throws Exception {
        var s=MetroSecondDownloadCases.selectCase(group);assertThat(s.profile().selectDetailUri(s.source()).toString()).isEqualTo(s.source().sourceUrl());
        assertThatThrownBy(()->s.profile().selectDetailUri(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE","bad",s.source().sourceUrl(),s.source().localSourceCode(),s.source().listParserProfileCode()))).hasMessage("PROFILE_REQUIRED");
        var page=Jsoup.parse(selectPage(group,selectLink(1,"hwp")));AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,s.title(),s.titleLayout());
        assertThatThrownBy(()->AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,"다른 제목",s.titleLayout())).isInstanceOf(AssertionError.class);
        var r=new AnnouncementSourceClassificationEngine().selectDecision(new AnnouncementSourceClassificationInput("LOCAL_GOV_NOTICE",s.title(),null,null,List.of(),BodySourceCode.NONE,BodyAvailabilityCode.UNAVAILABLE),AnnouncementAttachmentRealFileQaTest.selectDraftRuleSet());assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectTitleMayProceed(r)).isTrue();
        var budget=new AnnouncementAttachmentBbsOfficialObservationTest.Budget(s.profile(),true);assertThat(budget.maximumRequests).isEqualTo(6);assertThat(budget.maximumBytes).isEqualTo(23L*1024*1024);
    }
    @ParameterizedTest @MethodSource("selectGroups") void keepsLimitsConflictsAndAbsentAreaDistinct(String group) {
        var s=MetroSecondDownloadCases.selectCase(group);String one=selectLink(1,"hwp");assertThat(s.profile().selectDescriptors(s.source(),selectPage(group,one+one)).descriptors()).hasSize(1);
        var conflict=s.profile().selectDescriptors(s.source(),selectPage(group,one+one.replace("file1","other1")));assertThat(conflict.complete()).isFalse();assertThat(conflict.descriptors()).hasSize(1);
        var limit=s.profile().selectDescriptors(s.source(),selectPage(group,IntStream.rangeClosed(1,11).mapToObj(i->selectLink(i,"hwp")).collect(Collectors.joining())));assertThat(limit.status()).isEqualTo("LIMIT_EXCEEDED");assertThat(limit.descriptors()).hasSize(10);
        assertThat(s.profile().selectDescriptors(s.source(),"<html/>").warnings()).contains("ATTACHMENT_SELECTOR_CHANGED");assertThat(s.profile().selectDescriptors(s.source(),selectPage(group,"")).status()).isEqualTo("NO_FILES");
    }
    @ParameterizedTest @MethodSource("selectGroups") void rejectsForeignRequestsAndUnexpectedFunctions(String group) {
        var s=MetroSecondDownloadCases.selectCase(group);var p=s.profile();String url=s.source().sourceUrl();
        for(String bad:List.of(url+"&extra=1",url+"&not_ancmt_mgt_no=2",url.replace("https:","http:"),url.replace("https://","https://user@"),url+"#fragment"))assertThat(p.selectApprovedRequest(URI.create(bad))).isFalse();
        var d=p.selectDescriptors(s.source(),selectPage(group,selectLink(1,"hwp"))).descriptors().getFirst();assertThat(p.selectApprovedRequest(new Request(d.fetchUri(),"POST",Map.of("x","y")))).isFalse();assertThat(p.selectApprovedRequest(URI.create(d.fetchUri().toString().replace("eminwon.","evil.")))).isFalse();
        assertThat(p.selectDescriptors(s.source(),selectPage(group,selectLink(1,"hwp").replace("goDownLoad(","other("))).descriptors()).isEmpty();
    }
    @Test void bukguPreservesMultipleFileCellsAndRecognizesOnlyPairedStaticIcons() {
        var s=MetroSecondDownloadCases.selectCase("GWANGJU_BUKGU");String icon="<img src='http://bukgu.gwangju.kr/upload/skin/board/basic/ico_file.gif' alt='첨부파일 file1.hwp 다운로드'>";
        String page=selectPage("GWANGJU_BUKGU",icon+selectLink(1,"hwp")+"</dd><dd>"+selectLink(2,"pdf"));var result=s.profile().selectDescriptors(s.source(),page);assertThat(result.complete()).isTrue();assertThat(result.descriptors()).hasSize(2);
        for(String bad:List.of(page.replace("ico_file.gif","other.gif"),page.replace("alt='첨부파일","alt='변조 첨부파일"),page.replace("<img ","<img onclick='bad()' "))){var partial=s.profile().selectDescriptors(s.source(),bad);assertThat(partial.complete()).isFalse();assertThat(partial.descriptors()).hasSize(2);}
    }
}
