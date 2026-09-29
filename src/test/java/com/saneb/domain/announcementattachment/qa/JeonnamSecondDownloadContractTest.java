package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import java.net.URI;
import java.util.*;
import java.util.stream.*;
import org.jsoup.Jsoup;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import com.saneb.domain.announcementattachment.discovery.AttachmentDiscoveryProfile;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.classification.*;

class JeonnamSecondDownloadContractTest {
    static Stream<String> selectGroups(){return JeonnamSecondDownloadCases.GROUPS.stream();}
    static String selectLink(String g,int id,String ext){
        String name="공고"+id+"."+ext;
        if(g.equals("GOKSEONG")){
            String href="javascript:goDownLoad('user"+"A".repeat(30)+id+"','system"+"B".repeat(30)+id+"','/ntisho"+"C".repeat(64)+"')";
            return "<li><a href=\""+href+"\">"+name+"</a><div class=inner_btn><a href=\""+href+"\">다운로드</a></div></li>";
        }
        String q="?fileNo="+id+"&fileMgtNo=25159&stype="+ext;
        return "<li class=file_ico><a href='/doc/indexDown.jsp"+q+"'><span class=blind>파일</span><em>"+name+"</em></a><a class=down href='/doc/dataDown.jsp"+q+"'><img alt=다운로드></a></li>";
    }
    static String selectPage(String g,String links){
        String title=JeonnamSecondDownloadCases.selectCase(g).title();
        return g.equals("GOKSEONG")?"<div class=board_view><h3>"+title+"</h3><ul class=file_down>"+links+"</ul></div><form name=nnn method=post action='https://eminwon.gokseong.go.kr/emwp/jsp/ofr/FileDownNew.jsp'><input type=hidden name=user_file_nm><input type=hidden name=sys_file_nm><input type=hidden name=file_path></form>":"<div class=board_view><dl class=view_head><dt><em class=tit>제목</em><span class=txt>"+title+"</span></dt></dl><div class=view_foot><ul class=board_file>"+links+"</ul></div></div>";
    }
    @ParameterizedTest @MethodSource("selectGroups") void retainsGoodFilesAndSeparatesUnsupportedUnknownAndEmpty(String g){
        var s=JeonnamSecondDownloadCases.selectCase(g);var p=s.profile();var r=p.selectDescriptors(s.source(),selectPage(g,selectLink(g,1,"hwp")));
        assertThat(r.complete()).isTrue();assertThat(r.descriptors()).hasSize(1);assertThat(r.descriptors().getFirst().downloadAllowed()).isTrue();
        var unsupported=p.selectDescriptors(s.source(),selectPage(g,selectLink(g,1,"xlsx")));assertThat(unsupported.complete()).isTrue();assertThat(unsupported.descriptors()).hasSize(1);assertThat(unsupported.descriptors().getFirst().downloadAllowed()).isFalse();
        var partial=p.selectDescriptors(s.source(),selectPage(g,selectLink(g,1,"pdf")+selectLink(g,2,"xlsx")+"<li><a href='/unknown'>미확인</a></li>"));
        assertThat(partial.complete()).isFalse();assertThat(partial.descriptors()).hasSize(2);assertThat(partial.descriptors().getFirst().downloadAllowed()).isTrue();assertThat(partial.descriptors().getLast().downloadAllowed()).isFalse();
        partial.descriptors().forEach(d->assertThat(d.documentRole()).isEqualTo("UNKNOWN"));
    }
    @ParameterizedTest @MethodSource("selectGroups") void checksEmptyAreaLimitSourceAndTitle(String g)throws Exception{
        var s=JeonnamSecondDownloadCases.selectCase(g);var p=s.profile();
        assertThat(p.selectDescriptors(s.source(),selectPage(g,"" )).status()).isEqualTo("NO_FILES");
        assertThat(p.selectDescriptors(s.source(),selectPage(g,"<li></li>")).complete()).isFalse();
        assertThat(p.selectDescriptors(s.source(),"<html>오류</html>").complete()).isFalse();
        var many=p.selectDescriptors(s.source(),selectPage(g,IntStream.rangeClosed(1,11).mapToObj(i->selectLink(g,i,"hwp")).collect(Collectors.joining())));
        assertThat(many.status()).isEqualTo("LIMIT_EXCEEDED");assertThat(many.descriptors()).hasSize(10);
        var page=Jsoup.parse(selectPage(g,selectLink(g,1,"hwp")));AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,s.title(),s.titleLayout());
        assertThatThrownBy(()->AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,"다른 공고",s.titleLayout())).isInstanceOf(AssertionError.class);
        var src=s.source();assertThatThrownBy(()->p.selectDetailUri(new AttachmentDiscoveryProfile.Source(src.providerCode(),src.providerNoticeId(),src.sourceUrl(),"LGS-000001",src.listParserProfileCode()))).isInstanceOf(IllegalArgumentException.class);
        var d=new AnnouncementSourceClassificationEngine().selectDecision(new AnnouncementSourceClassificationInput("LOCAL_GOV_NOTICE",s.title(),null,null,List.of(),AnnouncementSourceClassificationCodes.BodySourceCode.NONE,AnnouncementSourceClassificationCodes.BodyAvailabilityCode.UNAVAILABLE),AnnouncementAttachmentRealFileQaTest.selectDraftRuleSet());assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectTitleMayProceed(d)).isTrue();
    }
    @ParameterizedTest @MethodSource("selectGroups") void rejectsWrongHostInjectedScriptChangedFormsAndRedirect(String g){
        var s=JeonnamSecondDownloadCases.selectCase(g);var p=s.profile();String html=selectPage(g,selectLink(g,1,"hwp"));
        var file=p.selectDescriptors(s.source(),html).descriptors().getFirst();assertThat(p.selectApprovedRequest(file.selectRequest())).isTrue();
        assertThat(file.toString()).doesNotContain("http","공고","user");assertThat(file.locator().toString()).doesNotContain("AAAA","BBBB");assertThat(file.documentRole()).isEqualTo("UNKNOWN");
        assertThat(p.selectApprovedRequest(URI.create("https:/missing-host"))).isFalse();
        assertThat(p.selectApprovedRequest(URI.create(p.selectDetailUri(s.source())+"&unknown=1"))).isFalse();
        assertThat(p.selectApprovedRequest(file.selectRequest(),new Request(URI.create(file.fetchUri().toString().replace(".go.kr",".example.com")),file.selectRequest().method(),file.postForm()))).isFalse();
        assertThat(p.selectDescriptors(s.source(),selectPage(g,selectLink(g,1,"hwp")+"<script>alert(1)</script>")).complete()).isFalse();
        assertThat(p.selectDescriptors(s.source(),html+"<a href='/screen.pdf'>인쇄</a>").descriptors()).hasSize(1);
        if(g.equals("GOKSEONG")){
            assertThat(p.selectDescriptors(s.source(),html.replace("name=file_path","name=extra")).complete()).isFalse();
            assertThat(p.selectDescriptors(s.source(),html.replace(")\">",");alert(1)\">" )).descriptors()).isEmpty();
            var n=new AnnouncementSourceIdentityNormalizer();String official=p.selectDetailUri(s.source()).toString();
            assertThat(p.selectDetailUri(new AttachmentDiscoveryProfile.Source(s.source().providerCode(),n.hash(n.canonicalizeUrl(official)),official,s.source().localSourceCode(),s.source().listParserProfileCode())).toString()).isEqualTo(official);
        }else{
            assertThat(p.selectDescriptors(s.source(),html.replace("fileMgtNo=25159","fileMgtNo=99999")).descriptors()).isEmpty();
            assertThat(p.selectDescriptors(s.source(),html.replace("/doc/indexDown.jsp?fileNo=1","/doc/indexDown.jsp?fileNo=2")).complete()).isFalse();
            assertThat(p.selectApprovedRequest(URI.create(file.fetchUri()+"&fileNo=2"))).isFalse();
        }
    }
}
