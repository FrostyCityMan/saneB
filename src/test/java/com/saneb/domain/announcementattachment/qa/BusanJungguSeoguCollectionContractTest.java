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
import com.saneb.domain.announcementsource.classification.*;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.*;

class BusanJungguSeoguCollectionContractTest {
    static Stream<String> selectGroups(){return Stream.of("BSJUNGGU","BSSEOGU");}
    static String selectLink(String format,int id){return "<a href=\"javascript:goDownLoad('문서."+format+"','"+id+"."+format+"','/ntishome/file/upload/ofr/ofr/20260928')\">첨부</a>";}
    static String selectPage(String group,String content){return group.equals("BSJUNGGU")?
            "<form name='form1' method='post'><table class='bbs_vtype'><tr><th colspan='4'>청년 지원사업</th></tr><tr><th>첨부파일</th><td colspan='3'>"+content+"</td></tr></table></form>":
            "<form name='form1' method='post'><table width='100%' border='0' cellspacing='1' cellpadding='0'><tr><th class='w_90'>제목</th><td>&nbsp;청년 지원사업</td></tr><tr><th>첨부파일</th><td>"+content+"</td></tr></table></form>";}
    @ParameterizedTest @MethodSource("selectGroups") void wholeOfficialSetAndUnresolvedContents(String group){
        var sample=AnnouncementAttachmentBbsOfficialObservationTest.selectCases(group).findFirst().orElseThrow();var p=sample.profile();var s=sample.source();
        String links=selectLink("pdf",1)+selectLink("hwp",2)+selectLink("hwpx",3);var result=p.selectDescriptors(s,selectPage(group,links));
        assertThat(result.complete()).isTrue();assertThat(result.descriptors()).extracting(AttachmentDiscoveryProfile.Descriptor::expectedFormat).containsExactly("PDF","HWP","HWPX");
        assertThat(result.descriptors()).allSatisfy(d->{assertThat(d.documentRole()).isEqualTo("UNKNOWN");assertThat(d.downloadAllowed()).isTrue();assertThat(d.postForm()).isEmpty();assertThat(p.selectApprovedRequest(d.selectRequest())).isTrue();});
        assertThat(p.selectDescriptors(s,selectPage(group,"&nbsp;")).status()).isEqualTo("NO_FILES");
        assertThat(p.selectDescriptors(s,selectPage(group,selectLink("hwp",1).repeat(2))).descriptors()).hasSize(1);
        var mixed=p.selectDescriptors(s,selectPage(group,links+selectLink("jpeg",4)));assertThat(mixed.descriptors()).hasSize(4);assertThat(mixed.descriptors().getLast().downloadAllowed()).isFalse();
        for(String extra:List.of("<a href='/other'>다른 파일</a>","<button>첨부</button>","<script>hidden()</script>","남은 파일"))assertThat(p.selectDescriptors(s,selectPage(group,links+extra)).complete()).isFalse();
        for(String invalid:List.of(selectPage(group,links)+selectPage(group,links),selectPage(group,links).replace("name='form1'","name='other'"),selectPage(group,links).replace("첨부파일","다른 제목")))assertThat(p.selectDescriptors(s,invalid).complete()).isFalse();
        String many=java.util.stream.IntStream.rangeClosed(1,11).mapToObj(i->selectLink("hwp",i)).collect(java.util.stream.Collectors.joining());assertThat(p.selectDescriptors(s,selectPage(group,many)).status()).isEqualTo("LIMIT_EXCEEDED");
    }
    @ParameterizedTest @MethodSource("selectGroups") void sourceTitleAndBudgetStayBound(String group){
        var sample=AnnouncementAttachmentBbsOfficialObservationTest.selectCases(group).findFirst().orElseThrow();var p=sample.profile();var s=sample.source();String html=selectPage(group,selectLink("hwp",1));
        AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(Jsoup.parse(html),"청년 지원사업",sample.titleLayout());
        for(String invalid:List.of(html+html,html.replace("청년 지원사업","다른 제목"),html.replace(group.equals("BSJUNGGU")?"bbs_vtype":"w_90","other")))assertThatThrownBy(()->AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(Jsoup.parse(invalid),"청년 지원사업",sample.titleLayout())).isInstanceOf(AssertionError.class);
        if(group.equals("BSSEOGU"))assertThatThrownBy(()->AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(Jsoup.parse(html.replace("청년 지원사업","<b>청년 지원사업</b>")),"청년 지원사업",sample.titleLayout())).isInstanceOf(AssertionError.class);
        for(var bad:List.of(new AttachmentDiscoveryProfile.Source(s.providerCode(),s.providerNoticeId(),s.sourceUrl(),"LGS-000001",s.listParserProfileCode()),new AttachmentDiscoveryProfile.Source(s.providerCode(),s.providerNoticeId(),s.sourceUrl(),s.localSourceCode(),"WRONG"),new AttachmentDiscoveryProfile.Source(s.providerCode(),"a".repeat(64),s.sourceUrl(),s.localSourceCode(),s.listParserProfileCode())))assertThatThrownBy(()->p.selectDetailUri(bad)).hasMessage("PROFILE_REQUIRED");
        assertThat(p.selectApprovedRequest(URI.create(s.sourceUrl().replace("https:","http:")))).isFalse();
        assertThat(p.selectApprovedRequest(URI.create(s.sourceUrl().replace(p.selectDetailUri(s).getHost(),"example.com")))).isFalse();
        assertThat(p.selectApprovedRequest(URI.create(s.sourceUrl()+"&homepagetype=other"))).isFalse();
        for(boolean diagnostic:List.of(true,false)){var b=new AnnouncementAttachmentBbsOfficialObservationTest.Budget(p,diagnostic);assertThat(b.maximumRequests).isEqualTo(6);assertThat(b.maximumBytes).isEqualTo(23L*1024*1024);}
    }
    @Test void fixedTitlesAndCatalogAreReferenceOnly() throws Exception {
        var rules=AnnouncementAttachmentRealFileQaTest.selectDraftRuleSet();var engine=new AnnouncementSourceClassificationEngine();var json=new com.fasterxml.jackson.databind.ObjectMapper();
        var catalog=json.readTree(Files.readString(Path.of("src/main/resources/announcement-attachment/provider-qa-catalog-v2.json"))).path("notices");
        for(String group:selectGroups().toList())for(var sample:AnnouncementAttachmentBbsOfficialObservationTest.selectCases(group).toList()){
            var result=engine.selectDecision(new AnnouncementSourceClassificationInput("LOCAL_GOV_NOTICE",sample.title(),null,null,List.of(),BodySourceCode.NONE,BodyAvailabilityCode.UNAVAILABLE),rules);
            assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectTitleMayProceed(result)).as(sample.code()+" "+result.titleStageCode()).isTrue();
            var ref=java.util.stream.StreamSupport.stream(catalog.spliterator(),false).filter(n->sample.code().equals(n.path("caseCode").asText())).findFirst().orElseThrow();
            assertThat(ref.path("source")).isEqualTo(json.valueToTree(sample.source()));assertThat(ref.hasNonNull("expectation")).isFalse();
        }
    }
    @Test @EnabledIfEnvironmentVariable(named="SANEB_BUSAN_JUNGGU_SEOGU_SAVED_DETAILS",matches=".+")
    void savedOfficialPagesProveWholeSetsWithoutNetwork() throws Exception {
        Path root=Path.of(System.getenv("SANEB_BUSAN_JUNGGU_SEOGU_SAVED_DETAILS")).toRealPath();assertThat(root.startsWith(Path.of("build").toRealPath())).isTrue();
        for(String group:selectGroups().toList())for(var sample:AnnouncementAttachmentBbsOfficialObservationTest.selectCases(group).toList()){
            Path path=root.resolve(sample.code()+".html");assertThat(Files.size(path)).isLessThan(1048576);String html=Files.readString(path);
            AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(Jsoup.parse(html),sample.title(),sample.titleLayout());
            var r=sample.profile().selectDescriptors(sample.source(),html);assertThat(r.complete()).as(sample.code()).isTrue();assertThat(r.status()).isEqualTo("FOUND");
            assertThat(r.descriptors()).hasSize(sample.listedFileCount()).allSatisfy(d->{assertThat(d.downloadAllowed()).isTrue();assertThat(d.documentRole()).isEqualTo("UNKNOWN");assertThat(d.postForm()).isEmpty();});
        }
    }
}
