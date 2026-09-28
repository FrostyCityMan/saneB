package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient;
import com.saneb.domain.announcementsource.classification.*;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.*;

class BusanNorthWestAttachmentCollectionContractTest {
    static Stream<String> selectGroups(){return Stream.of("BSBUKGU","BSGANGSEO");}
    static String selectLink(String group,String format,int id){
        return group.equals("BSBUKGU") ? "<a href=\"javascript:goDownLoad('"+"a".repeat(64)+"','"+id+"b".repeat(64)+"','/ntisho"+"c".repeat(64)+"')\">문서."+format+"</a>"
                : "<a href=\"javascript:goDownLoad('문서."+format+"','"+id+"."+format+"','/ntishome/file/upload/ofr/ofr/20260928')\">첨부</a>";
    }
    static String selectPage(String group,String contents){
        return group.equals("BSBUKGU")
                ? "<form name='nnn' method='post' action='/emwp/jsp/ofr/FileDownNew.jsp'><input type='hidden' name='user_file_nm'><input type='hidden' name='sys_file_nm'><input type='hidden' name='file_path'></form><form name='form1' method='post'><table class='tbl taC'><tr><th scope='row'>제목</th><td class='taL' colspan='3'>청년 지원사업</td></tr><tr><th scope='row'>첨부파일</th><td>"+contents+"</td></tr></table></form>"
                : "<form name='docfm' method='post' action='http://example.com/viewer'><input type='hidden' name='doc'></form><form name='form1' method='post'><div class='board'><div class='b_view'><div class='view_head'><h3>청년 지원사업</h3></div><ul class='view_file'>"+contents+"</ul></div></div></form>";
    }
    @ParameterizedTest @MethodSource("selectGroups") void wholeSetAndUnknownLinksArePreserved(String group){
        var sample=AnnouncementAttachmentBbsOfficialObservationTest.selectCases(group).findFirst().orElseThrow();var p=sample.profile();var s=sample.source();
        String links=selectLink(group,"pdf",1)+selectLink(group,"hwp",2)+selectLink(group,"hwpx",3);
        var result=p.selectDescriptors(s,selectPage(group,links));assertThat(result.complete()).isTrue();
        assertThat(result.descriptors()).extracting(AttachmentDiscoveryProfile.Descriptor::expectedFormat).containsExactly("PDF","HWP","HWPX");
        assertThat(result.descriptors()).allSatisfy(d->{assertThat(d.documentRole()).isEqualTo("UNKNOWN");assertThat(d.downloadAllowed()).isTrue();});
        assertThat(p.selectDescriptors(s,selectPage(group,"")).status()).isEqualTo("NO_FILES");
        assertThat(p.selectDescriptors(s,selectPage(group,selectLink(group,"hwp",1).repeat(2))).descriptors()).hasSize(1);
        var mixed=p.selectDescriptors(s,selectPage(group,links+selectLink(group,"jpeg",4)));assertThat(mixed.descriptors()).hasSize(4);assertThat(mixed.descriptors().getLast().downloadAllowed()).isFalse();
        for(String extra:List.of("<a href='/unknown'>추가 파일</a>","<button>파일</button>","<script>hidden()</script>","나머지 파일"))
            assertThat(p.selectDescriptors(s,selectPage(group,links+extra)).complete()).isFalse();
        String eleven=java.util.stream.IntStream.rangeClosed(1,11).mapToObj(i->selectLink(group,"hwp",i)).collect(java.util.stream.Collectors.joining());
        assertThat(p.selectDescriptors(s,selectPage(group,eleven)).status()).isEqualTo("LIMIT_EXCEEDED");
    }
    @Test void bukguUsesOnlyExactPostWithoutRevealingOpaqueArguments(){
        var sample=AnnouncementAttachmentBbsOfficialObservationTest.selectCases("BSBUKGU").findFirst().orElseThrow();var p=sample.profile();
        var d=p.selectDescriptors(sample.source(),selectPage("BSBUKGU",selectLink("BSBUKGU","hwp",1))).descriptors().getFirst();
        var request=d.selectRequest();
        assertThat(request.form().keySet()).containsExactlyInAnyOrder("user_file_nm","sys_file_nm","file_path");assertThat(p.selectApprovedRequest(request)).isTrue();
        assertThat(p.selectApprovedRequest(new AttachmentPinnedDownloadClient.Request(request.uri(),"GET",Map.of()))).isFalse();
        assertThat(p.selectApprovedRequest(request,request)).isTrue();
        var changed=new LinkedHashMap<>(request.form());changed.put("sys_file_nm","d".repeat(64));
        assertThat(p.selectApprovedRequest(request,new AttachmentPinnedDownloadClient.Request(request.uri(),"POST",changed))).isFalse();
        changed.put("isHome","Y");assertThat(p.selectApprovedRequest(new AttachmentPinnedDownloadClient.Request(request.uri(),"POST",changed))).isFalse();
        assertThat(d.locator().toString()).doesNotContain("a".repeat(64),"b".repeat(64),"c".repeat(64));
        String html=selectPage("BSBUKGU",selectLink("BSBUKGU","hwp",1));
        for(String invalid:List.of(html.replace("name='file_path'","name='isHome'"),html.replace("name='file_path'","value='preset' name='file_path'"),html.replace("FileDownNew.jsp","FileDown.jsp"),html.replace("/ntisho","/wrong")))
            assertThat(p.selectDescriptors(sample.source(),invalid).complete()).isFalse();
    }
    @ParameterizedTest @MethodSource("selectGroups") void exactBindingTitleAndBudget(String group){
        var sample=AnnouncementAttachmentBbsOfficialObservationTest.selectCases(group).findFirst().orElseThrow();var p=sample.profile();var s=sample.source();String html=selectPage(group,selectLink(group,"hwp",1));
        AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(Jsoup.parse(html),"청년 지원사업",sample.titleLayout());
        for(String invalid:List.of(html+html,html.replace("청년 지원사업","다른 제목"),html.replace("청년 지원사업","<span>청년 지원사업</span>")))
            assertThatThrownBy(()->AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(Jsoup.parse(invalid),"청년 지원사업",sample.titleLayout())).isInstanceOf(AssertionError.class);
        for(String invalid:List.of(html+html,html.replace("name='form1'","name='other'"),html.replace(group.equals("BSBUKGU")?"tbl taC":"view_file","other")))
            assertThat(p.selectDescriptors(s,invalid).complete()).isFalse();
        for(var invalid:List.of(new AttachmentDiscoveryProfile.Source(s.providerCode(),s.providerNoticeId(),s.sourceUrl(),"LGS-000001",s.listParserProfileCode()),new AttachmentDiscoveryProfile.Source(s.providerCode(),s.providerNoticeId(),s.sourceUrl(),s.localSourceCode(),"WRONG"),new AttachmentDiscoveryProfile.Source(s.providerCode(),"a".repeat(64),s.sourceUrl(),s.localSourceCode(),s.listParserProfileCode())))
            assertThatThrownBy(()->p.selectDetailUri(invalid)).hasMessage("PROFILE_REQUIRED");
        assertThat(p.selectApprovedRequest(URI.create(s.sourceUrl().replace("https:","http:")))).isFalse();
        assertThat(p.selectApprovedRequest(URI.create(s.sourceUrl().replace(p.selectDetailUri(s).getHost(),"example.com")))).isFalse();
        for(boolean diagnostic:List.of(true,false)){var b=new AnnouncementAttachmentBbsOfficialObservationTest.Budget(p,diagnostic);assertThat(b.maximumRequests).isEqualTo(6);assertThat(b.maximumBytes).isEqualTo(23L*1024*1024);}
    }
    @Test void fixedTitlesAndReferenceOnlyCatalog() throws Exception {
        var rules=AnnouncementAttachmentRealFileQaTest.selectDraftRuleSet();var engine=new AnnouncementSourceClassificationEngine();var json=new com.fasterxml.jackson.databind.ObjectMapper();
        var catalog=json.readTree(Files.readString(Path.of("src/main/resources/announcement-attachment/provider-qa-catalog-v2.json"))).path("notices");
        for(String group:selectGroups().toList())for(var sample:AnnouncementAttachmentBbsOfficialObservationTest.selectCases(group).toList()){
            var result=engine.selectDecision(new AnnouncementSourceClassificationInput("LOCAL_GOV_NOTICE",sample.title(),null,null,List.of(),BodySourceCode.NONE,BodyAvailabilityCode.UNAVAILABLE),rules);
            assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectTitleMayProceed(result)).as(sample.code()).isTrue();
            var ref=java.util.stream.StreamSupport.stream(catalog.spliterator(),false).filter(n->sample.code().equals(n.path("caseCode").asText())).findFirst().orElseThrow();
            assertThat(ref.path("source")).isEqualTo(json.valueToTree(sample.source()));assertThat(ref.hasNonNull("expectation")).isFalse();
        }
    }
    @Test @EnabledIfEnvironmentVariable(named="SANEB_BUSAN_NORTH_WEST_SAVED_DETAILS",matches=".+")
    void savedOfficialHtmlProvesAllFilesWithoutNetwork() throws Exception {
        Path root=Path.of(System.getenv("SANEB_BUSAN_NORTH_WEST_SAVED_DETAILS")).toRealPath();assertThat(root.startsWith(Path.of("build").toRealPath())).isTrue();
        for(String group:selectGroups().toList())for(var sample:AnnouncementAttachmentBbsOfficialObservationTest.selectCases(group).toList()){
            Path path=root.resolve(sample.code()+".html");assertThat(Files.size(path)).isLessThan(1048576);String html=Files.readString(path);
            AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(Jsoup.parse(html),sample.title(),sample.titleLayout());
            var r=sample.profile().selectDescriptors(sample.source(),html);assertThat(r.complete()).as(sample.code()).isTrue();assertThat(r.status()).isEqualTo("FOUND");
            assertThat(r.descriptors()).hasSize(sample.listedFileCount()).allSatisfy(d->{assertThat(d.downloadAllowed()).isTrue();assertThat(d.documentRole()).isEqualTo("UNKNOWN");
                assertThat(d.postForm().isEmpty()).isEqualTo(group.equals("BSGANGSEO"));});
        }
    }
}
