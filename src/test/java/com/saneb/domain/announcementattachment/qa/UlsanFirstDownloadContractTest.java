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

class UlsanFirstDownloadContractTest {
    static Stream<String> selectGroups() { return UlsanFirstDownloadCases.GROUPS.stream().sorted(); }
    private String selectLink(String group, int id, String ext) {
        return group.equals("ULSAN_JUNGGU")
                ? "<a href=\"javascript:goDownLoad('"+"a".repeat(64)+"','"+id+"b".repeat(64)+"','/ntisho"+"c".repeat(64)+"')\">공고문."+ext+"</a>"
                : "<a href=\"javascript:goDownLoad('공고문."+ext+"','saved"+id+"."+ext+"','/ntishome/file/upload/ofr/ofr/20260929')\">공고문."+ext+"</a>";
    }
    private String selectPage(String group, String contents) {
        boolean jung=group.equals("ULSAN_JUNGGU");
        return (jung ? "<form name='nnn' method='post' action='/emwp/jsp/ofr/FileDownNew.jsp'><input type='hidden' name='user_file_nm'><input type='hidden' name='sys_file_nm'><input type='hidden' name='file_path'><input type='hidden' name='isHome' value='Y'></form>" : "")
                + "<form name='form1' method='post'><table class='"+(jung?"public_view":"cont_table")+"' width='100%' border='0' cellspacing='1' cellpadding='0'><tr><th>제목</th><td>청년 지원사업</td></tr><tr><td><table><tr><"+(jung?"td":"th")+">첨부파일 :</"+(jung?"td":"th")+"><td>"+contents+"</td></tr></table></td></tr></table></form>";
    }
    @ParameterizedTest @MethodSource("selectGroups") void retainsSuccessfulDescriptorsAndSeparatesUnknownLinks(String group) {
        var s=UlsanFirstDownloadCases.selectCase(group);var p=s.profile();String links=selectLink(group,1,"hwp")+selectLink(group,2,"hwpx")+selectLink(group,3,"pdf");
        var r=p.selectDescriptors(s.source(),selectPage(group,links));assertThat(r.complete()).isTrue();assertThat(r.descriptors()).hasSize(3);
        r.descriptors().forEach(d->{assertThat(d.documentRole()).isEqualTo("UNKNOWN");assertThat(d.downloadAllowed()).isTrue();assertThat(p.selectApprovedRequest(d.selectRequest())).isTrue();assertThat(d.fetchUri().getPath()).isEqualTo("/emwp/jsp/ofr/FileDownNew.jsp");});
        for(String unknown:List.of("<a href='/other.pdf'>미확인</a>","<script>bad()</script>","<button>파일</button>","<img src='/unknown'>")){var partial=p.selectDescriptors(s.source(),selectPage(group,links+unknown));assertThat(partial.complete()).isFalse();assertThat(partial.descriptors()).hasSize(3);}
        var mixed=p.selectDescriptors(s.source(),selectPage(group,links+selectLink(group,4,"jpg")));assertThat(mixed.descriptors()).hasSize(4);assertThat(mixed.descriptors().getLast().downloadAllowed()).isFalse();
        assertThat(p.selectDescriptors(s.source(),selectPage(group,"")).status()).isEqualTo("NO_FILES");
        assertThat(p.selectDescriptors(s.source(),"<html/>").warnings()).contains("ATTACHMENT_SELECTOR_CHANGED");
    }
    @ParameterizedTest @MethodSource("selectGroups") void deduplicatesAndKeepsLimits(String group) {
        var s=UlsanFirstDownloadCases.selectCase(group);String one=selectLink(group,1,"hwp");
        assertThat(s.profile().selectDescriptors(s.source(),selectPage(group,one+one)).descriptors()).hasSize(1);
        String other=one.replace(">공고문.hwp</a>",">변경.hwp</a>");if(group.equals("ULSAN_BUKGU"))other=other.replace("'공고문.hwp'","'변경.hwp'");
        var conflict=s.profile().selectDescriptors(s.source(),selectPage(group,one+other));assertThat(conflict.complete()).isFalse();assertThat(conflict.descriptors()).hasSize(1);
        var limit=s.profile().selectDescriptors(s.source(),selectPage(group,IntStream.rangeClosed(1,11).mapToObj(i->selectLink(group,i,"hwp")).collect(Collectors.joining())));assertThat(limit.status()).isEqualTo("LIMIT_EXCEEDED");assertThat(limit.descriptors()).hasSize(10);
    }
    @ParameterizedTest @MethodSource("selectGroups") void enforcesSourceTitleBudgetAndRequestIdentity(String group) {
        var s=UlsanFirstDownloadCases.selectCase(group);var p=s.profile();var page=Jsoup.parse(selectPage(group,selectLink(group,1,"hwp")));
        AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,"청년 지원사업",s.titleLayout());assertThatThrownBy(()->AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,"다른 제목",s.titleLayout())).isInstanceOf(AssertionError.class);
        assertThat(p.selectDetailUri(s.source()).toString()).isEqualTo(s.source().sourceUrl());
        for(var bad:List.of(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE","bad",s.source().sourceUrl(),s.source().localSourceCode(),s.source().listParserProfileCode()),new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",s.source().providerNoticeId(),s.source().sourceUrl(),"LGS-000001",s.source().listParserProfileCode())))assertThatThrownBy(()->p.selectDetailUri(bad)).hasMessage("PROFILE_REQUIRED");
        for(String bad:List.of(s.source().sourceUrl()+"&extra=1",s.source().sourceUrl()+"&subCheck=Y",s.source().sourceUrl().replace("https:","http:"),s.source().sourceUrl()+"#fragment"))assertThat(p.selectApprovedRequest(URI.create(bad))).isFalse();
        var d=p.selectDescriptors(s.source(),selectPage(group,selectLink(group,1,"hwp"))).descriptors().getFirst();var req=d.selectRequest();assertThat(p.selectApprovedRequest(req,req)).isTrue();
        assertThat(p.selectApprovedRequest(req,new Request(URI.create(req.uri().toString().replace("eminwon.","other.")),req.method(),req.form()))).isFalse();
        assertThat(d.locator().toString()).doesNotContain("a".repeat(64));
        for(boolean diagnostic:List.of(true,false)){var budget=new AnnouncementAttachmentBbsOfficialObservationTest.Budget(p,diagnostic);assertThat(budget.maximumRequests).isEqualTo(6);assertThat(budget.maximumBytes).isEqualTo(23L*1024*1024);}
    }
    @Test void bukguUsesOnlyPlainGetNewPathWithoutRedirectChanges() {
        var s=UlsanFirstDownloadCases.selectCase("ULSAN_BUKGU");var p=s.profile();var d=p.selectDescriptors(s.source(),selectPage("ULSAN_BUKGU",selectLink("ULSAN_BUKGU",1,"hwp"))).descriptors().getFirst();
        assertThat(d.selectRequest().method()).isEqualTo("GET");assertThat(d.locator().path()).isEqualTo(d.fetchUri().getPath());
        assertThat(p.selectApprovedRequest(URI.create(d.fetchUri().toString().replace("FileDownNew.jsp","FileDown.jsp")))).isFalse();
        assertThat(p.selectApprovedRequest(URI.create(d.fetchUri().toString().replace("FileDownNew.jsp","FileDown%4Eew.jsp")))).isFalse();
        assertThat(p.selectApprovedRequest(new Request(d.fetchUri(),"POST",Map.of("user_file_nm","value")))).isFalse();
        assertThat(p.selectApprovedRequest(d.selectRequest(),Request.selectGet(URI.create(d.fetchUri().toString().replace("saved1","saved2"))))).isFalse();
    }
    @Test void jungguRequiresExactOpaquePostForm() {
        var s=UlsanFirstDownloadCases.selectCase("ULSAN_JUNGGU");var p=s.profile();String html=selectPage("ULSAN_JUNGGU",selectLink("ULSAN_JUNGGU",1,"hwp"));
        var req=p.selectDescriptors(s.source(),html).descriptors().getFirst().selectRequest();assertThat(req.method()).isEqualTo("POST");assertThat(p.selectApprovedRequest(Request.selectGet(req.uri()))).isFalse();
        var changed=new LinkedHashMap<>(req.form());changed.put("sys_file_nm","d".repeat(64));assertThat(p.selectApprovedRequest(req,new Request(req.uri(),"POST",changed))).isFalse();changed.put("extra","Y");assertThat(p.selectApprovedRequest(new Request(req.uri(),"POST",changed))).isFalse();
        for(String bad:List.of(html.replace("public_view","other"),html.replace("name='file_path'","name='file_path' value='preset'"),html.replace("FileDownNew.jsp","FileDown.jsp"),html.replace("/ntisho","/wrong"),html.replace("name='isHome' value='Y'","name='isHome' value='N'"),html.replace("<input type='hidden' name='isHome' value='Y'>","")))assertThat(p.selectDescriptors(s.source(),bad).complete()).isFalse();
        var wrongFlag=new LinkedHashMap<>(req.form());wrongFlag.put("isHome","N");assertThat(p.selectApprovedRequest(new Request(req.uri(),"POST",wrongFlag))).isFalse();
    }
    @Test void draftTitleRulesAndCatalogRemainUnapproved() throws Exception {
        var json=new com.fasterxml.jackson.databind.ObjectMapper();var catalog=json.readTree(Files.readString(Path.of("src/main/resources/announcement-attachment/provider-qa-catalog-v2.json"))).path("notices");
        for(String group:selectGroups().toList()){
            var s=UlsanFirstDownloadCases.selectCase(group);assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectCases(group).count()).isEqualTo(1);
            var r=new AnnouncementSourceClassificationEngine().selectDecision(new AnnouncementSourceClassificationInput("LOCAL_GOV_NOTICE",s.title(),null,null,List.of(),BodySourceCode.NONE,BodyAvailabilityCode.UNAVAILABLE),AnnouncementAttachmentRealFileQaTest.selectDraftRuleSet());assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectTitleMayProceed(r)).isTrue();
            var entry=StreamSupport.stream(catalog.spliterator(),false).filter(n->s.code().equals(n.path("caseCode").asText())).findFirst().orElseThrow();assertThat(entry.path("source")).isEqualTo(json.valueToTree(s.source()));assertThat(entry.hasNonNull("expectation")).isFalse();
        }
    }
}
