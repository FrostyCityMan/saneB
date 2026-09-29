package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import java.net.URI;
import java.util.List;
import java.util.stream.Stream;
import org.jsoup.Jsoup;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import com.saneb.domain.announcementattachment.discovery.AttachmentDiscoveryProfile;
import com.saneb.domain.announcementsource.classification.*;

class JeonbukFirstDownloadContractTest {
    static Stream<String> selectGroups(){return JeonbukFirstDownloadCases.GROUPS.stream();}
    private String selectLink(String group,int n,String ext){
        String args="'공고."+ext+"','"+n+"."+ext+"','/ntishome/file/upload/ofr/ofr/20260929'";
        return List.of("WANJU","MUJU").contains(group)?"<span><a href='#' onclick=\"goDownLoad("+args+");return false;\">공고</a></span>":"<a href=\"javascript:goDownLoad("+args+")\">공고</a><br>";
    }
    private String selectPage(String group,String title,String links){
        if(List.of("WANJU","MUJU").contains(group))return "<form name=form1 method=post><table class=tstyle><tr><th scope=row>제목</th><td colspan=3>"+title+"</td></tr></table><div class=filebox><p class='btn btn-file'>첨부파일</p><div class=allfile>"+links+"</div></div></form>";
        if(group.equals("SUNCHANG"))return "<form name=form1 method=post><table class=bbs_view><tr><th scope=row>제목</th><td>"+title+"</td></tr><tr><th scope=row>첨부파일</th><td>"+links+"</td></tr></table></form>";
        boolean jangsu=group.equals("JANGSU"),td=jangsu||group.equals("JINAN");
        String tag=td?"td":"th";
        return "<form name='"+(jangsu?"form":"form1")+"' method=post><table width='"+(jangsu?"98%":"100%")+"' border=0 cellspacing=1 cellpadding=0><tr><"+tag+">제목</"+tag+"><td>"+title+"</td></tr><tr><td>첨부파일 :</td><td>"+links+"</td></tr></table></form>";
    }
    @ParameterizedTest @MethodSource("selectGroups") void preservesKnownFilesAlongsideUnknownAndUnsupportedAttachments(String group){
        var s=JeonbukFirstDownloadCases.selectCase(group);var p=s.profile();
        String html=selectPage(group,s.title(),selectLink(group,1,"hwp")+"<a href='/unknown'>미확인</a>"+selectLink(group,2,"hwpx")+selectLink(group,3,"xlsx"));
        var r=p.selectDescriptors(s.source(),html);assertThat(r.complete()).isFalse();assertThat(r.status()).isEqualTo("FAILED");
        assertThat(r.warnings()).containsExactly("ATTACHMENT_LINK_UNRESOLVED");assertThat(r.descriptors()).hasSize(3);
        assertThat(r.descriptors().stream().filter(d->d.downloadAllowed())).hasSize(2);
        assertThat(r.descriptors().getLast().downloadAllowed()).isFalse();r.descriptors().forEach(d->assertThat(d.documentRole()).isEqualTo("UNKNOWN"));
        assertThat(p.selectDescriptors(s.source(),selectPage(group,s.title(),"")).status()).isEqualTo("NO_FILES");
        var all=p.selectDescriptors(s.source(),selectPage(group,s.title(),selectLink(group,1,"pdf")));assertThat(all.complete()).isTrue();assertThat(all.descriptors()).hasSize(1);
    }
    @ParameterizedTest @MethodSource("selectGroups") void fixedSourceAndOfficialTitleBoundariesRemainMandatory(String group)throws Exception{
        var s=JeonbukFirstDownloadCases.selectCase(group);var p=s.profile();var source=s.source();
        assertThatThrownBy(()->p.selectDetailUri(new AttachmentDiscoveryProfile.Source(source.providerCode(),source.providerNoticeId(),source.sourceUrl(),"LGS-000001",source.listParserProfileCode()))).isInstanceOf(IllegalArgumentException.class);
        assertThat(p.selectApprovedRequest(URI.create(source.sourceUrl().replace("https://","http://")))).isFalse();
        assertThat(p.selectApprovedRequest(URI.create(source.sourceUrl()+"&extra=1"))).isFalse();
        String html=selectPage(group,s.title(),selectLink(group,1,"hwp"));
        assertThat(p.selectDescriptors(source,html.replace("method=post","method=get")).complete()).isFalse();
        var page=Jsoup.parse(html);AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,s.title(),s.titleLayout());
        assertThatThrownBy(()->AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,"다른 공고",s.titleLayout())).isInstanceOf(AssertionError.class);
        var d=new AnnouncementSourceClassificationEngine().selectDecision(new AnnouncementSourceClassificationInput("LOCAL_GOV_NOTICE",s.title(),null,null,List.of(),AnnouncementSourceClassificationCodes.BodySourceCode.NONE,AnnouncementSourceClassificationCodes.BodyAvailabilityCode.UNAVAILABLE),AnnouncementAttachmentRealFileQaTest.selectDraftRuleSet());
        assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectTitleMayProceed(d)).isTrue();
    }
    @ParameterizedTest @ValueSource(strings={"WANJU","MUJU"}) void fileboxCannotExecuteExtraJavascriptOrHideUnknownSiblings(String group){
        var s=JeonbukFirstDownloadCases.selectCase(group);var p=s.profile();
        String good=selectLink(group,1,"hwp"),bad=selectLink(group,2,"hwp").replace("return false;","return false;alert(1);");
        var r=p.selectDescriptors(s.source(),selectPage(group,s.title(),good+bad));assertThat(r.complete()).isFalse();assertThat(r.descriptors()).hasSize(1);
        String outside=selectPage(group,s.title(),good).replace("</div></div>","</div><a href='/another'>다른 첨부</a></div>");
        var extra=p.selectDescriptors(s.source(),outside);assertThat(extra.complete()).isFalse();assertThat(extra.descriptors()).hasSize(1);
        assertThat(p.selectDescriptors(s.source(),selectPage(group,s.title(),good).replace("class=allfile","class=changed")).descriptors()).isEmpty();
    }
}
