package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.classification.*;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.*;

class MetroSaeolFirstDownloadContractTest {
    static Stream<String> selectGroups(){return Stream.of("DAEGU_NAMGU","DAEGU_BUKGU");}
    static String selectLink(String group,String format,int id){return group.equals("DAEGU_BUKGU")
            ? "<a href=\"javascript:goDownLoad('"+"a".repeat(64)+"','"+id+"b".repeat(64)+"','/ntisho"+"c".repeat(64)+"')\">문서."+format+"</a>"
            : "<a href=\"javascript:goDownLoad('문서."+format+"','"+id+"."+format+"','/ntishome/file/upload/ofr/ofr/20260928')\">첨부</a>";}
    static String selectPage(String group,String contents){return group.equals("DAEGU_NAMGU")
            ? "<form name='form1' method='post'><table class='boardw wps_100'><tr><th scope='row'>제목</th><td>청년 지원사업</td></tr><tr><th scope='row'>첨부파일</th><td>"+contents+"</td></tr></table></form>"
            : "<form name='nnn' method='post' action='/emwp/jsp/ofr/FileDownNew.jsp'><input type='hidden' name='user_file_nm'><input type='hidden' name='sys_file_nm'><input type='hidden' name='file_path'></form><form name='form1' method='post'><table class='wps_100 view mg_b20'><tr><th>제목</th><td>청년 지원사업</td></tr><tr><th>첨부파일</th><td>"+contents+"</td></tr></table></form>";}
    @ParameterizedTest @MethodSource("selectGroups") void wholeSetUnknownRolesAndUnresolvedLinks(String group){
        var sample=MetroSaeolFirstDownloadCases.selectCase(group);var p=sample.profile();var s=sample.source();
        String links=selectLink(group,"pdf",1)+selectLink(group,"hwp",2)+selectLink(group,"hwpx",3);
        var r=p.selectDescriptors(s,selectPage(group,links));assertThat(r.complete()).isTrue();
        assertThat(r.descriptors()).extracting(AttachmentDiscoveryProfile.Descriptor::expectedFormat).containsExactly("PDF","HWP","HWPX");
        assertThat(r.descriptors()).allSatisfy(d->{assertThat(d.documentRole()).isEqualTo("UNKNOWN");assertThat(d.downloadAllowed()).isTrue();assertThat(p.selectApprovedRequest(d.selectRequest())).isTrue();});
        assertThat(p.selectDescriptors(s,selectPage(group,"")).status()).isEqualTo("NO_FILES");
        assertThat(p.selectDescriptors(s,selectPage(group,selectLink(group,"hwp",1).repeat(2))).descriptors()).hasSize(1);
        var mixed=p.selectDescriptors(s,selectPage(group,links+selectLink(group,"jpeg",4)));assertThat(mixed.descriptors()).hasSize(4);assertThat(mixed.descriptors().getLast().downloadAllowed()).isFalse();
        for(String extra:List.of("<a href='/other'>파일</a>","<button>파일</button>","<script>hidden()</script>","나머지 파일"))assertThat(p.selectDescriptors(s,selectPage(group,links+extra)).complete()).isFalse();
        String many=java.util.stream.IntStream.rangeClosed(1,11).mapToObj(i->selectLink(group,"hwp",i)).collect(java.util.stream.Collectors.joining());assertThat(p.selectDescriptors(s,selectPage(group,many)).status()).isEqualTo("LIMIT_EXCEEDED");
    }
    @Test void bukguOnlyAcceptsExactOpaquePostAndOfficialTable(){
        var sample=MetroSaeolFirstDownloadCases.selectCase("DAEGU_BUKGU");var p=sample.profile();String html=selectPage("DAEGU_BUKGU",selectLink("DAEGU_BUKGU","hwp",1));
        var d=p.selectDescriptors(sample.source(),html).descriptors().getFirst();var request=d.selectRequest();
        assertThat(request.method()).isEqualTo("POST");assertThat(request.form().keySet()).containsExactlyInAnyOrder("user_file_nm","sys_file_nm","file_path");
        assertThat(p.selectApprovedRequest(Request.selectGet(request.uri()))).isFalse();assertThat(p.selectApprovedRequest(request,request)).isTrue();
        var changed=new LinkedHashMap<>(request.form());changed.put("sys_file_nm","d".repeat(64));assertThat(p.selectApprovedRequest(request,new Request(request.uri(),"POST",changed))).isFalse();
        changed.put("isHome","Y");assertThat(p.selectApprovedRequest(new Request(request.uri(),"POST",changed))).isFalse();
        for(String invalid:List.of(html.replace("FileDownNew.jsp","FileDown.jsp"),html.replace("name='file_path'","value='preset' name='file_path'"),html.replace("name='file_path'","name='isHome'"),html.replace("/ntisho","/wrong"),html.replace("view mg_b20","other"),html.replace("<th>첨부파일</th>","<th>첨부파일</th><td>다른 영역</td>")))assertThat(p.selectDescriptors(sample.source(),invalid).complete()).isFalse();
        assertThat(d.locator().toString()).doesNotContain("a".repeat(64),"b".repeat(64),"c".repeat(64));
    }
    @ParameterizedTest @MethodSource("selectGroups") void exactSourceTitleAndBudget(String group){
        var sample=MetroSaeolFirstDownloadCases.selectCase(group);var p=sample.profile();var s=sample.source();String html=selectPage(group,selectLink(group,"hwp",1));
        AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(Jsoup.parse(html),"청년 지원사업",sample.titleLayout());
        for(String invalid:List.of(html+html,html.replace("청년 지원사업","다른 제목"),html.replace(">제목</th>",">다른 항목</th>")))assertThatThrownBy(()->AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(Jsoup.parse(invalid),"청년 지원사업",sample.titleLayout())).isInstanceOf(AssertionError.class);
        for(String invalid:List.of(html+html,html.replace("name='form1'","name='other'")))assertThat(p.selectDescriptors(s,invalid).complete()).isFalse();
        for(var invalid:List.of(new AttachmentDiscoveryProfile.Source(s.providerCode(),s.providerNoticeId(),s.sourceUrl(),"LGS-000001",s.listParserProfileCode()),new AttachmentDiscoveryProfile.Source(s.providerCode(),s.providerNoticeId(),s.sourceUrl(),s.localSourceCode(),"WRONG"),new AttachmentDiscoveryProfile.Source(s.providerCode(),"a".repeat(64),s.sourceUrl(),s.localSourceCode(),s.listParserProfileCode())))assertThatThrownBy(()->p.selectDetailUri(invalid)).hasMessage("PROFILE_REQUIRED");
        for(String invalid:List.of(s.sourceUrl().replace("https:","http:"),s.sourceUrl().replace(p.selectDetailUri(s).getHost(),"example.com"),s.sourceUrl()+"&unknown=1",s.sourceUrl()+"&subCheck=Y",s.sourceUrl()+"#fragment"))assertThat(p.selectApprovedRequest(URI.create(invalid))).isFalse();
        for(boolean diagnostic:List.of(true,false)){var b=new AnnouncementAttachmentBbsOfficialObservationTest.Budget(p,diagnostic);assertThat(b.maximumRequests).isEqualTo(6);assertThat(b.maximumBytes).isEqualTo(23L*1024*1024);}
    }
    @Test void titlePolicyAndCatalogRemainReferenceOnly() throws Exception {
        var rules=AnnouncementAttachmentRealFileQaTest.selectDraftRuleSet();var engine=new AnnouncementSourceClassificationEngine();var json=new com.fasterxml.jackson.databind.ObjectMapper();
        var catalog=json.readTree(Files.readString(Path.of("src/main/resources/announcement-attachment/provider-qa-catalog-v2.json"))).path("notices");
        assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectCases("DAEGU_NAMGU").count()).isEqualTo(1);
        for(String group:selectGroups().toList())for(var sample:AnnouncementAttachmentBbsOfficialObservationTest.selectCases(group).toList()){
            var result=engine.selectDecision(new AnnouncementSourceClassificationInput("LOCAL_GOV_NOTICE",sample.title(),null,null,List.of(),BodySourceCode.NONE,BodyAvailabilityCode.UNAVAILABLE),rules);
            assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectTitleMayProceed(result)).as(sample.code()).isTrue();
            var ref=java.util.stream.StreamSupport.stream(catalog.spliterator(),false).filter(n->sample.code().equals(n.path("caseCode").asText())).findFirst().orElseThrow();
            assertThat(ref.path("source")).isEqualTo(json.valueToTree(sample.source()));assertThat(ref.hasNonNull("expectation")).isFalse();
        }
    }
}
