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

class ChungcheongNextDownloadContractTest {
    static Stream<String> selectGroups(){return ChungcheongNextDownloadCases.GROUPS.stream().sorted();}
    private String selectLink(String group,int id,String ext){return group.equals("TAEAN")
            ? "<a href=\"javascript:goDownLoad('"+"a".repeat(64)+"','"+id+"b".repeat(64)+"','/ntisho"+"c".repeat(64)+"')\">공고문."+ext+"</a>"
            : "<a href=\"javascript:goDownLoad('공고문."+ext+"','saved"+id+"."+ext+"','/ntishome/file/upload/ofr/ofr/20260929')\">공고문."+ext+"</a>";}
    private String selectPage(String group,String contents){return group.equals("JINCHEON")
            ? "<form name='form1' method='post'><table class='contTable'><tr><th>제목</th><td>청년 지원사업</td></tr><tr><th>첨부파일</th><td>"+contents+"</td></tr></table></form>"
            : "<form name='nnn' method='post' action='/emwp/jsp/ofr/FileDownNew.jsp'><input type='hidden' name='user_file_nm'><input type='hidden' name='sys_file_nm'><input type='hidden' name='file_path'><input type='hidden' name='isHome' value='Y'></form><form name='form1' method='post'><table width='100%' border='0' cellspacing='1' cellpadding='0'><tr><th>제목</th><td>청년 지원사업</td></tr><tr><td><table><tr><td>첨부파일 :</td><td>"+contents+"</td></tr></table></td></tr></table></form>";}
    @ParameterizedTest @MethodSource("selectGroups") void preservesGoodFilesAndSeparatesUnknownLinks(String group){
        var s=ChungcheongNextDownloadCases.selectCase(group);var p=s.profile();String links=selectLink(group,1,"hwp")+selectLink(group,2,"hwpx")+selectLink(group,3,"pdf");
        var r=p.selectDescriptors(s.source(),"<nav><a href='/noise.pdf'>메뉴</a></nav>"+selectPage(group,links));assertThat(r.complete()).isTrue();assertThat(r.descriptors()).hasSize(3);r.descriptors().forEach(d->{assertThat(d.documentRole()).isEqualTo("UNKNOWN");assertThat(d.downloadAllowed()).isTrue();assertThat(p.selectApprovedRequest(d.selectRequest())).isTrue();});
        for(String unknown:List.of("<a href='/other.pdf'>미확인</a>","<script>bad()</script>","<img src='/unknown'>","<button>첨부</button>")){var partial=p.selectDescriptors(s.source(),selectPage(group,links+unknown));assertThat(partial.complete()).isFalse();assertThat(partial.descriptors()).hasSize(3);}
        var mixed=p.selectDescriptors(s.source(),selectPage(group,links+selectLink(group,4,"jpg")));assertThat(mixed.descriptors()).hasSize(4);assertThat(mixed.descriptors().getLast().downloadAllowed()).isFalse();
        assertThat(p.selectDescriptors(s.source(),selectPage(group,"")).status()).isEqualTo("NO_FILES");assertThat(p.selectDescriptors(s.source(),"<html/>").warnings()).contains("ATTACHMENT_SELECTOR_CHANGED");
    }
    @ParameterizedTest @MethodSource("selectGroups") void deduplicatesAndEnforcesLimits(String group){
        var s=ChungcheongNextDownloadCases.selectCase(group);String one=selectLink(group,1,"hwp");assertThat(s.profile().selectDescriptors(s.source(),selectPage(group,one+one)).descriptors()).hasSize(1);
        String other=one.replace(">공고문.hwp</a>",">변경.hwp</a>");if(group.equals("JINCHEON"))other=other.replace("'공고문.hwp'","'변경.hwp'");var conflict=s.profile().selectDescriptors(s.source(),selectPage(group,one+other));assertThat(conflict.complete()).isFalse();assertThat(conflict.descriptors()).hasSize(1);
        var limit=s.profile().selectDescriptors(s.source(),selectPage(group,IntStream.rangeClosed(1,11).mapToObj(i->selectLink(group,i,"hwp")).collect(Collectors.joining())));assertThat(limit.status()).isEqualTo("LIMIT_EXCEEDED");assertThat(limit.descriptors()).hasSize(10);
    }
    @ParameterizedTest @MethodSource("selectGroups") void enforcesSourceTitleAndBudget(String group){
        var s=ChungcheongNextDownloadCases.selectCase(group);var p=s.profile();String html=selectPage(group,selectLink(group,1,"hwp"));AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(Jsoup.parse(html),"청년 지원사업",s.titleLayout());assertThatThrownBy(()->AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(Jsoup.parse(html),"다른 제목",s.titleLayout())).isInstanceOf(AssertionError.class);
        assertThat(p.selectDetailUri(s.source()).toString()).isEqualTo(s.source().sourceUrl());for(var bad:List.of(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE","bad",s.source().sourceUrl(),s.source().localSourceCode(),s.source().listParserProfileCode()),new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",s.source().providerNoticeId(),s.source().sourceUrl(),"LGS-000001",s.source().listParserProfileCode())))assertThatThrownBy(()->p.selectDetailUri(bad)).hasMessage("PROFILE_REQUIRED");
        for(String bad:List.of(s.source().sourceUrl()+"&extra=1",s.source().sourceUrl()+"&subCheck=Y",s.source().sourceUrl().replace("https:","http:"),s.source().sourceUrl()+"#fragment"))assertThat(p.selectApprovedRequest(URI.create(bad))).isFalse();
        for(boolean diagnostic:List.of(true,false)){var budget=new AnnouncementAttachmentBbsOfficialObservationTest.Budget(p,diagnostic);assertThat(budget.maximumRequests).isEqualTo(6);assertThat(budget.maximumBytes).isEqualTo(23L*1024*1024);}
    }
    @Test void taeansSubCheckNAndFixedHomeFlagCannotBeChanged(){
        var s=ChungcheongNextDownloadCases.selectCase("TAEAN");var p=s.profile();String html=selectPage("TAEAN",selectLink("TAEAN",1,"hwpx"));var req=p.selectDescriptors(s.source(),html).descriptors().getFirst().selectRequest();assertThat(req.method()).isEqualTo("POST");assertThat(req.form().get("isHome")).isEqualTo("Y");assertThat(p.selectApprovedRequest(Request.selectGet(req.uri()))).isFalse();assertThat(p.selectApprovedRequest(URI.create(s.source().sourceUrl().replace("subCheck=N","subCheck=Y")))).isFalse();
        var changed=new LinkedHashMap<>(req.form());changed.put("isHome","N");assertThat(p.selectApprovedRequest(new Request(req.uri(),"POST",changed))).isFalse();changed.put("isHome","Y");changed.put("extra","x");assertThat(p.selectApprovedRequest(new Request(req.uri(),"POST",changed))).isFalse();changed.remove("extra");changed.put("sys_file_nm","d".repeat(64));assertThat(p.selectApprovedRequest(req,new Request(req.uri(),"POST",changed))).isFalse();
        for(String bad:List.of(html.replace("FileDownNew.jsp","FileDown.jsp"),html.replace("name='isHome' value='Y'","name='isHome' value='N'"),html.replace("name='file_path'","name='file_path' value='preset'"),html.replace("width='100%'","width='99%'")))assertThat(p.selectDescriptors(s.source(),bad).complete()).isFalse();
    }
    @Test void jincheonReusesPlainGetWithoutPost(){
        var s=ChungcheongNextDownloadCases.selectCase("JINCHEON");var p=s.profile();var req=p.selectDescriptors(s.source(),selectPage("JINCHEON",selectLink("JINCHEON",1,"hwp"))).descriptors().getFirst().selectRequest();assertThat(req.method()).isEqualTo("GET");assertThat(req.uri().getPath()).isEqualTo("/emwp/jsp/ofr/FileDown.jsp");assertThat(p.selectApprovedRequest(new Request(req.uri(),"POST",Map.of("x","y")))).isFalse();assertThat(p.selectApprovedRequest(URI.create(req.uri().toString().replace("FileDown.jsp","FileDownNew.jsp")))).isFalse();
    }
    @Test void titleRulesAndCatalogRemainUnapproved() throws Exception {
        var json=new com.fasterxml.jackson.databind.ObjectMapper();var catalog=json.readTree(Files.readString(Path.of("src/main/resources/announcement-attachment/provider-qa-catalog-v2.json"))).path("notices");for(String group:selectGroups().toList()){var s=ChungcheongNextDownloadCases.selectCase(group);assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectCases(group).count()).isEqualTo(1);var decision=new AnnouncementSourceClassificationEngine().selectDecision(new AnnouncementSourceClassificationInput("LOCAL_GOV_NOTICE",s.title(),null,null,List.of(),BodySourceCode.NONE,BodyAvailabilityCode.UNAVAILABLE),AnnouncementAttachmentRealFileQaTest.selectDraftRuleSet());assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectTitleMayProceed(decision)).as(group).isTrue();var ref=StreamSupport.stream(catalog.spliterator(),false).filter(n->s.code().equals(n.path("caseCode").asText())).findFirst().orElseThrow();assertThat(ref.path("source")).isEqualTo(json.valueToTree(s.source()));assertThat(ref.hasNonNull("expectation")).isFalse();}
    }
}
