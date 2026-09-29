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
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.classification.*;

class JeonbukSecondDownloadContractTest {
    static Stream<String> selectGroups(){return JeonbukSecondDownloadCases.GROUPS.stream();}
    static boolean selectSaeol(String group){return List.of("GUNSAN","JEONGEUP").contains(group);}
    static String selectLink(String group,int id,String ext){
        var s=JeonbukSecondDownloadCases.selectCase(group);
        if(selectSaeol(group))return "<a href=\"javascript:goDownLoad('공고."+ext+"','"+id+"."+ext+"','/ntishome/file/upload/ofr/ofr/20260929')\">공고</a>";
        String uri=s.source().sourceUrl().replace("/view.","/download.");
        if(group.equals("NAMWON"))return "<li><p><span class=text><a href='"+uri+"&atchFileUid="+"0".repeat(31)+id+"'>공고."+ext+"</a></span>[123Byte]</p><p><a class=btn_down href='"+uri+"&atchFileUid="+"0".repeat(31)+id+"'>다운로드</a><a class=btn_view href=\"javascript:filePreView('', '"+"0".repeat(31)+id+"');\">미리보기</a></p></li>";
        uri+="&command=update&fileSid="+id;
        return "<a href='"+uri+"' title='공고."+ext+"'>공고."+ext+"(80 kb)</a><a class='"+(group.equals("BUAN")?"sbtn_file":"ico_viewer")+"' href='"+uri.replace("/download.","/SynapViewer.")+"'>바로보기</a>";
    }
    static String selectPage(String group,String links){
        String title=JeonbukSecondDownloadCases.selectCase(group).title();
        if(selectSaeol(group)){
            String tag=group.equals("GUNSAN")?"th":"td";
            return "<form name=form1 method=post><table width='100%' border=0 cellspacing=1 cellpadding=0><tr><"+tag+">제목</"+tag+"><td>"+title+"</td></tr><tr><td>첨부파일 :</td><td>"+links+"</td></tr></table></form>";
        }
        if(group.equals("NAMWON"))return "<table class=view_table><thead><tr><td class=title><strong>"+title+"</strong></td></tr></thead><tbody><tr><td><ul class=file_list>"+links+"</ul></td></tr></tbody></table>";
        String header="<div class=bbs_view><div class=bbs_vtop><h4>"+title+"</h4></div>";
        return group.equals("BUAN")?header+"<div class=bbs_filedown><dl><dt>첨부파일</dt><dd>"+links+"</dd></dl></div></div>":header+"</div><p class=bbs_filedown>"+links+"</p>";
    }
    @ParameterizedTest @MethodSource("selectGroups") void retainsSupportedFilesAndSeparatesUnknownAndUnsupported(String group){
        var s=JeonbukSecondDownloadCases.selectCase(group);var p=s.profile();
        var complete=p.selectDescriptors(s.source(),selectPage(group,selectLink(group,1,"hwp")));assertThat(complete.complete()).isTrue();assertThat(complete.descriptors()).hasSize(1);
        var partial=p.selectDescriptors(s.source(),selectPage(group,selectLink(group,1,"hwp")+selectLink(group,2,"xlsx")+"<a href='/unknown'>미확인</a>"));
        assertThat(partial.complete()).isFalse();assertThat(partial.warnings()).containsExactly("ATTACHMENT_LINK_UNRESOLVED");assertThat(partial.descriptors()).hasSize(2);
        assertThat(partial.descriptors().getFirst().downloadAllowed()).isTrue();assertThat(partial.descriptors().getLast().downloadAllowed()).isFalse();
        partial.descriptors().forEach(d->assertThat(d.documentRole()).isEqualTo("UNKNOWN"));
        assertThat(p.selectDescriptors(s.source(),selectPage(group,"")).status()).isEqualTo("NO_FILES");
        assertThat(p.selectDescriptors(s.source(),"<html>수집 오류</html>").complete()).isFalse();
    }
    @ParameterizedTest @MethodSource("selectGroups") void verifiesSourceTitleAndDraftEligibility(String group)throws Exception{
        var s=JeonbukSecondDownloadCases.selectCase(group);var p=s.profile();var source=s.source();
        assertThatThrownBy(()->p.selectDetailUri(new AttachmentDiscoveryProfile.Source(source.providerCode(),source.providerNoticeId(),source.sourceUrl(),"LGS-000001",source.listParserProfileCode()))).isInstanceOf(IllegalArgumentException.class);
        assertThat(p.selectApprovedRequest(URI.create(source.sourceUrl().replace("https://","http://")))).isFalse();
        assertThat(p.selectDetailUri(source).getScheme()).isEqualTo("https");
        assertThat(p.selectApprovedRequest(URI.create(p.selectDetailUri(source)+"&extra=1"))).isFalse();
        var page=Jsoup.parse(selectPage(group,selectLink(group,1,"pdf")));AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,s.title(),s.titleLayout());
        assertThatThrownBy(()->AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,"다른 제목",s.titleLayout())).isInstanceOf(AssertionError.class);
        var d=new AnnouncementSourceClassificationEngine().selectDecision(new AnnouncementSourceClassificationInput("LOCAL_GOV_NOTICE",s.title(),null,null,List.of(),AnnouncementSourceClassificationCodes.BodySourceCode.NONE,AnnouncementSourceClassificationCodes.BodyAvailabilityCode.UNAVAILABLE),AnnouncementAttachmentRealFileQaTest.selectDraftRuleSet());
        assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectTitleMayProceed(d)).isTrue();
    }
    @ParameterizedTest @ValueSource(strings={"NAMWON","BUAN","GOCHANG"}) void forbidsOtherNoticeOrHostAndPreservesUnknownPreview(String group){
        var s=JeonbukSecondDownloadCases.selectCase(group);var p=s.profile();String links=selectLink(group,1,"pdf");
        var result=p.selectDescriptors(s.source(),selectPage(group,links));var file=result.descriptors().getFirst();
        assertThat(p.selectApprovedRequest(file.selectRequest(),new Request(URI.create(file.fetchUri().toString().replace("fileSid=1","fileSid=2").replace("atchFileUid="+"0".repeat(31)+"1","atchFileUid="+"0".repeat(31)+"2")),"GET",java.util.Map.of()))).isFalse();
        String bad=links.replace(group.equals("NAMWON")?"postUid=33037092911b4bd8be3b8c15657d8aef":"dataSid="+(group.equals("BUAN")?"363827":"807493"),group.equals("NAMWON")?"postUid="+"f".repeat(32):"dataSid=999999");
        assertThat(p.selectDescriptors(s.source(),selectPage(group,bad)).descriptors()).isEmpty();
        assertThat(p.selectApprovedRequest(URI.create(file.fetchUri().toString().replace("www.","evil.")))).isFalse();
        var extra=p.selectDescriptors(s.source(),selectPage(group,links+"<a class=btn_view href=\"javascript:alert(1)\">미리보기</a>"));assertThat(extra.complete()).isFalse();assertThat(extra.descriptors()).hasSize(1);
        var script=p.selectDescriptors(s.source(),selectPage(group,links+"<script>alert(1)</script>"));assertThat(script.complete()).isFalse();assertThat(script.descriptors()).hasSize(1);
        var dup=p.selectDescriptors(s.source(),selectPage(group,links+"<a href='"+file.fetchUri()+"' title='다른 이름.pdf'>다른 이름.pdf</a>"));assertThat(dup.complete()).isFalse();assertThat(dup.descriptors()).hasSize(1);
        assertThat(p.selectApprovedRequest(URI.create(file.fetchUri()+"&"+(group.equals("NAMWON")?"postUid":"dataSid")+"=1"))).isFalse();
    }
}
