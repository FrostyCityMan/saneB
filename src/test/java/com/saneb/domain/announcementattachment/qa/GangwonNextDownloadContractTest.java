package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.classification.*;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.*;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class GangwonNextDownloadContractTest {
    static Stream<String> selectGroups(){return GangwonNextDownloadCases.GROUPS.stream().sorted();}
    private String selectLink(String group,int id,String ext){return group.equals("YANGYANG")
            ? "<a href=\"javascript:goDownLoad('"+"a".repeat(64)+"','"+id+"b".repeat(64)+"','/ntisho"+"c".repeat(64)+"')\">공고문."+ext+"</a>"
            : "<a href=\"javascript:goDownLoad('공고문."+ext+"','saved"+id+"."+ext+"','/ntishome/file/upload/ofr/ofr/20260929')\">공고문."+ext+"</a>";}
    private String selectPage(String group,String contents){return switch(group){
        case "DONGHAE" -> "<form name='form' method='post'><table width='98%' cellspacing='1' cellpadding='0'><tr><td>제목</td><td>청년 지원사업</td></tr><tr><td><table><tr><td>첨부파일 :</td><td>"+contents+"</td></tr></table></td></tr></table></form>";
        case "GW_GOSEONG" -> "<form name='form1' method='post'><table class='tb_style1'><tr><th>제목</th><td>청년 지원사업</td></tr><tr><th>첨부파일1</th><td>"+contents+"</td></tr></table></form>";
        case "JEONGSEON" -> "<form name='form1' method='post'><div class='skinTb eminwon'><div class='skinTb-tr'><div class='skinTb-th'>제목</div><div class='skinTb-td'>청년 지원사업</div></div><div class='skinTb-tr'><div class='skinTb-td skinTb-conts'>본문 <a href='/body-link.pdf'>본문 링크</a><br><br>첨부파일<br>"+contents+"</div></div></div></form>";
        default -> "<form name='nnn' method='post' action='/emwp/jsp/ofr/FileDownNew.jsp'><input type='hidden' name='user_file_nm'><input type='hidden' name='sys_file_nm'><input type='hidden' name='file_path'></form><form name='form1' method='post'><div class='skinTb'><div class='skinTb-tr'><div class='skinTb-th'>제목</div><div class='skinTb-td'>청년 지원사업</div></div><div class='skinTb-tr'><div class='skinTb-th'>첨부파일</div><div class='skinTb-td'><div class='attachFile'>"+contents+"</div></div></div></div></form>";
    };}
    @ParameterizedTest @MethodSource("selectGroups") void isolatesOfficialBoundaryAndRetainsGoodFiles(String group){
        var s=GangwonNextDownloadCases.selectCase(group);var p=s.profile();String links=selectLink(group,1,"hwp")+selectLink(group,2,"hwpx")+selectLink(group,3,"pdf");
        var r=p.selectDescriptors(s.source(),"<nav><a href='/noise.pdf'>메뉴</a></nav>"+selectPage(group,links));assertThat(r.complete()).isTrue();assertThat(r.descriptors()).hasSize(3);
        r.descriptors().forEach(d->{assertThat(d.downloadAllowed()).isTrue();assertThat(d.documentRole()).isEqualTo("UNKNOWN");assertThat(p.selectApprovedRequest(d.selectRequest())).isTrue();});
        for(String extra:List.of("<a href='/unknown.pdf'>다른 파일</a>","<script>bad()</script>","<img src='/unknown'>","<button>첨부</button>")){var partial=p.selectDescriptors(s.source(),selectPage(group,links+extra));assertThat(partial.complete()).isFalse();assertThat(partial.descriptors()).hasSize(3);}
        var mixed=p.selectDescriptors(s.source(),selectPage(group,links+selectLink(group,4,"jpg")));assertThat(mixed.descriptors()).hasSize(4);assertThat(mixed.descriptors().getLast().downloadAllowed()).isFalse();
        assertThat(p.selectDescriptors(s.source(),selectPage(group,"")).status()).isEqualTo("NO_FILES");assertThat(p.selectDescriptors(s.source(),"<html/>").warnings()).contains("ATTACHMENT_SELECTOR_CHANGED");
    }
    @ParameterizedTest @MethodSource("selectGroups") void deduplicatesAndEnforcesFileLimit(String group){
        var s=GangwonNextDownloadCases.selectCase(group);String one=selectLink(group,1,"hwp");assertThat(s.profile().selectDescriptors(s.source(),selectPage(group,one+one)).descriptors()).hasSize(1);
        String other=one.replace(">공고문.hwp</a>",">변경.hwp</a>");if(!group.equals("YANGYANG"))other=other.replace("'공고문.hwp'","'변경.hwp'");var conflict=s.profile().selectDescriptors(s.source(),selectPage(group,one+other));assertThat(conflict.complete()).isFalse();assertThat(conflict.descriptors()).hasSize(1);
        var limit=s.profile().selectDescriptors(s.source(),selectPage(group,IntStream.rangeClosed(1,11).mapToObj(i->selectLink(group,i,"hwp")).collect(Collectors.joining())));assertThat(limit.status()).isEqualTo("LIMIT_EXCEEDED");assertThat(limit.descriptors()).hasSize(10);
    }
    @ParameterizedTest @MethodSource("selectGroups") void enforcesSourceTitleAndBoundedRequests(String group){
        var s=GangwonNextDownloadCases.selectCase(group);var p=s.profile();var html=selectPage(group,selectLink(group,1,"hwp"));var page=Jsoup.parse(html);
        AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,"청년 지원사업",s.titleLayout());assertThatThrownBy(()->AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,"다른 제목",s.titleLayout())).isInstanceOf(AssertionError.class);
        assertThatThrownBy(()->AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(Jsoup.parse(html+html),"청년 지원사업",s.titleLayout())).isInstanceOf(AssertionError.class);
        assertThat(p.selectDetailUri(s.source()).toString()).isEqualTo(s.source().sourceUrl());
        assertThatThrownBy(()->p.selectDetailUri(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE","bad",s.source().sourceUrl(),s.source().localSourceCode(),s.source().listParserProfileCode()))).hasMessage("PROFILE_REQUIRED");
        for(String bad:List.of(s.source().sourceUrl()+"&extra=1",s.source().sourceUrl()+"&subCheck=Y",s.source().sourceUrl().replace("https:","http:"),s.source().sourceUrl()+"#fragment"))assertThat(p.selectApprovedRequest(URI.create(bad))).isFalse();
        var req=p.selectDescriptors(s.source(),html).descriptors().getFirst().selectRequest();assertThat(p.selectApprovedRequest(req,req)).isTrue();assertThat(p.selectApprovedRequest(req,new Request(URI.create(req.uri().toString().replace("eminwon.","other.")),req.method(),req.form()))).isFalse();
        for(boolean diagnostic:List.of(true,false)){var b=new AnnouncementAttachmentBbsOfficialObservationTest.Budget(p,diagnostic);assertThat(b.maximumRequests).isEqualTo(6);assertThat(b.maximumBytes).isEqualTo(23L*1024*1024);}
    }
    @Test void jeongseonRequiresOneDirectMarkerAndKeepsOnlyItsFollowingNodes(){
        var s=GangwonNextDownloadCases.selectCase("JEONGSEON");String html=selectPage("JEONGSEON",selectLink("JEONGSEON",1,"hwp"));
        for(String bad:List.of(html.replace("첨부파일<br>","<span>첨부파일</span><br>"),html.replace("첨부파일<br>","첨부파일<br>첨부파일<br>"),html.replace("첨부파일<br>","파일<br>"),html.replace("<br><br>첨부파일","첨부파일")))assertThat(s.profile().selectDescriptors(s.source(),bad).complete()).isFalse();
        assertThat(s.profile().selectDescriptors(s.source(),html).descriptors()).hasSize(1);
    }
    @Test void goseongCollectsAllNumberedRowsAndRetainsFilesOnMissingNumber(){
        var s=GangwonNextDownloadCases.selectCase("GW_GOSEONG");String first=selectLink("GW_GOSEONG",1,"hwp"),second=selectLink("GW_GOSEONG",2,"pdf");String page=selectPage("GW_GOSEONG",first+"</td></tr><tr><th>첨부파일2</th><td>"+second);
        var r=s.profile().selectDescriptors(s.source(),page);assertThat(r.complete()).isTrue();assertThat(r.descriptors()).hasSize(2);
        var gap=s.profile().selectDescriptors(s.source(),page.replace("첨부파일2","첨부파일3"));assertThat(gap.complete()).isFalse();assertThat(gap.descriptors()).hasSize(2);
    }
    @Test void yangyangRequiresExactOpaquePostAndThreeHiddenFields(){
        var s=GangwonNextDownloadCases.selectCase("YANGYANG");String html=selectPage("YANGYANG",selectLink("YANGYANG",1,"hwp"));var req=s.profile().selectDescriptors(s.source(),html).descriptors().getFirst().selectRequest();assertThat(req.method()).isEqualTo("POST");assertThat(s.profile().selectApprovedRequest(Request.selectGet(req.uri()))).isFalse();
        var changed=new LinkedHashMap<>(req.form());changed.put("isHome","Y");assertThat(s.profile().selectApprovedRequest(new Request(req.uri(),"POST",changed))).isFalse();
        for(String bad:List.of(html.replace("FileDownNew.jsp","FileDown.jsp"),html.replace("name='file_path'","name='file_path' value='preset'"),html.replace("<div class='skinTb-th'>첨부파일</div>","<div>다른 영역</div>")))assertThat(s.profile().selectDescriptors(s.source(),bad).complete()).isFalse();
    }
    @Test void draftTitleAndReferenceCatalogDoNotApprovePolicy() throws Exception {
        var json=new com.fasterxml.jackson.databind.ObjectMapper();var catalog=json.readTree(Files.readString(Path.of("src/main/resources/announcement-attachment/provider-qa-catalog-v2.json"))).path("notices");
        for(String group:selectGroups().toList()){assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectCases(group).count()).isEqualTo(group.equals("JEONGSEON")?2:1);for(var s:AnnouncementAttachmentBbsOfficialObservationTest.selectCases(group).toList()){var decision=new AnnouncementSourceClassificationEngine().selectDecision(new AnnouncementSourceClassificationInput("LOCAL_GOV_NOTICE",s.title(),null,null,List.of(),BodySourceCode.NONE,BodyAvailabilityCode.UNAVAILABLE),AnnouncementAttachmentRealFileQaTest.selectDraftRuleSet());if(s.expectedTitleStopStage()==null)assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectTitleMayProceed(decision)).as(group).isTrue();else{assertThat(decision.titleStageCode()).isEqualTo(s.expectedTitleStopStage());assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectTitleMayProceed(decision)).isFalse();}var ref=StreamSupport.stream(catalog.spliterator(),false).filter(n->s.code().equals(n.path("caseCode").asText())).findFirst().orElseThrow();assertThat(ref.path("source")).isEqualTo(json.valueToTree(s.source()));assertThat(ref.hasNonNull("expectation")).isFalse();}}
    }
}
