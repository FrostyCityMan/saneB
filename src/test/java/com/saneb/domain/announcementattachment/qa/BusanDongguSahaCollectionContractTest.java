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
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient;
import com.saneb.domain.announcementsource.classification.*;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.*;

class BusanDongguSahaCollectionContractTest {
    static Stream<String> selectGroups(){return Stream.of("BSDONGGU","SAHA");}
    static String selectLink(String group,String format,int id){
        String call="goDownLoad('문서."+format+"','"+id+"."+format+"','/ntishome/file/upload/ofr/ofr/20260928')";
        return group.equals("SAHA")?"<a href='#' onclick=\""+call+"\" onkeypress=\""+call+"\">첨부</a>":"<a href=\"javascript:"+call+"\">첨부</a>";
    }
    static String selectPage(String group,String content){return group.equals("SAHA")?
            "<form name='form1' method='post'><table class='board_read'><thead><tr><th scope='col'>제목</th><td colspan='3'><b>청년 지원사업</b></td></tr></thead><tbody><tr><th scope='col'>첨부파일</th><td colspan='3'>"+content+"</td></tr></tbody></table></form>":
            "<form name='form1' method='post'><table width='100%' border='0' cellspacing='1' cellpadding='0'><tr><td>제목</td><td>청년 지원사업</td></tr><tr><td colspan='2'><table><tr><td>첨부파일 : </td><td>"+content+"</td></tr></table></td></tr></table></form>";}
    @ParameterizedTest @MethodSource("selectGroups") void wholeSetAndUnknownContents(String group){
        var sample=AnnouncementAttachmentBbsOfficialObservationTest.selectCases(group).findFirst().orElseThrow();var p=sample.profile();var s=sample.source();
        String links=selectLink(group,"pdf",1)+selectLink(group,"hwp",2)+selectLink(group,"hwpx",3);
        var r=p.selectDescriptors(s,selectPage(group,links));assertThat(r.complete()).isTrue();
        assertThat(r.descriptors()).extracting(AttachmentDiscoveryProfile.Descriptor::expectedFormat).containsExactly("PDF","HWP","HWPX");
        assertThat(r.descriptors()).allSatisfy(d->{assertThat(d.documentRole()).isEqualTo("UNKNOWN");assertThat(d.postForm()).isEmpty();assertThat(d.downloadAllowed()).isTrue();});
        assertThat(p.selectDescriptors(s,selectPage(group,"&nbsp;")).status()).isEqualTo("NO_FILES");
        assertThat(p.selectDescriptors(s,selectPage(group,selectLink(group,"hwp",1).repeat(2))).descriptors()).hasSize(1);
        var mixed=p.selectDescriptors(s,selectPage(group,links+selectLink(group,"jpeg",4)));assertThat(mixed.descriptors()).hasSize(4);assertThat(mixed.descriptors().getLast().downloadAllowed()).isFalse();
        for(String extra:List.of("<a href='/other'>다른 첨부</a>","<button>다른 파일</button>","<script>hidden()</script>","확인할 파일"))assertThat(p.selectDescriptors(s,selectPage(group,links+extra)).complete()).isFalse();
        String many=java.util.stream.IntStream.rangeClosed(1,11).mapToObj(i->selectLink(group,"hwp",i)).collect(java.util.stream.Collectors.joining());
        assertThat(p.selectDescriptors(s,selectPage(group,many)).status()).isEqualTo("LIMIT_EXCEEDED");
    }
    @Test void sahaReadsOnlyMatchingBoundedHandlersWithoutExecutingJavascript(){
        var sample=AnnouncementAttachmentBbsOfficialObservationTest.selectCases("SAHA").findFirst().orElseThrow();var p=sample.profile();var s=sample.source();String html=selectPage("SAHA",selectLink("SAHA","hwp",1));
        for(String invalid:List.of(html+html,html.replace("board_read","other"),html.replace("scope='col'","scope='row'"),html.replace("onkeypress=", "onmouseover="),html.replace("href='#'","href='/other'"),html.replace("onclick=", "data-other='1' onclick="),html.replace("')\"", "');evil()\""),html.replace("onkeypress=\"goDownLoad", "onkeypress=\"wrong")))
            assertThat(p.selectDescriptors(s,invalid).complete()).isFalse();
        var request=p.selectDescriptors(s,html).descriptors().getFirst().selectRequest();assertThat(p.selectApprovedRequest(request,request)).isTrue();
        assertThat(p.selectApprovedRequest(request,AttachmentPinnedDownloadClient.Request.selectGet(URI.create(request.uri().toString().replace("1.hwp","2.hwp"))))).isFalse();
        String oldUrl=s.sourceUrl().replace("https:","http:");var n=new AnnouncementSourceIdentityNormalizer();
        var old=new AttachmentDiscoveryProfile.Source(s.providerCode(),n.hash(n.canonicalizeUrl(oldUrl)),oldUrl,s.localSourceCode(),s.listParserProfileCode());
        assertThat(p.selectDetailUri(old)).isEqualTo(URI.create(s.sourceUrl()));assertThat(p.selectApprovedRequest(URI.create(oldUrl))).isFalse();
    }
    @ParameterizedTest @MethodSource("selectGroups") void titleSourceAndBudgetStayBound(String group){
        var sample=AnnouncementAttachmentBbsOfficialObservationTest.selectCases(group).findFirst().orElseThrow();var p=sample.profile();var s=sample.source();String html=selectPage(group,selectLink(group,"hwp",1));
        AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(Jsoup.parse(html),"청년 지원사업",sample.titleLayout());
        for(String invalid:List.of(html+html,html.replace("청년 지원사업","다른 제목")))assertThatThrownBy(()->AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(Jsoup.parse(invalid),"청년 지원사업",sample.titleLayout())).isInstanceOf(AssertionError.class);
        if(group.equals("SAHA"))assertThatThrownBy(()->AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(Jsoup.parse(html.replace("<b>","<b><span>").replace("</b>","</span></b>")),"청년 지원사업",sample.titleLayout())).isInstanceOf(AssertionError.class);
        for(var bad:List.of(new AttachmentDiscoveryProfile.Source(s.providerCode(),s.providerNoticeId(),s.sourceUrl(),"LGS-000001",s.listParserProfileCode()),new AttachmentDiscoveryProfile.Source(s.providerCode(),s.providerNoticeId(),s.sourceUrl(),s.localSourceCode(),"WRONG"),new AttachmentDiscoveryProfile.Source(s.providerCode(),"a".repeat(64),s.sourceUrl(),s.localSourceCode(),s.listParserProfileCode())))assertThatThrownBy(()->p.selectDetailUri(bad)).hasMessage("PROFILE_REQUIRED");
        assertThat(p.selectApprovedRequest(URI.create(s.sourceUrl().replace(p.selectDetailUri(s).getHost(),"example.com")))).isFalse();
        for(boolean diagnostic:List.of(true,false)){var b=new AnnouncementAttachmentBbsOfficialObservationTest.Budget(p,diagnostic);assertThat(b.maximumRequests).isEqualTo(6);assertThat(b.maximumBytes).isEqualTo(23L*1024*1024);}
    }
    @Test void fixedTitlesAndCatalogRemainUnapprovedReferences() throws Exception {
        var rules=AnnouncementAttachmentRealFileQaTest.selectDraftRuleSet();var engine=new AnnouncementSourceClassificationEngine();var json=new com.fasterxml.jackson.databind.ObjectMapper();
        var catalog=json.readTree(Files.readString(Path.of("src/main/resources/announcement-attachment/provider-qa-catalog-v2.json"))).path("notices");
        for(String group:selectGroups().toList())for(var sample:AnnouncementAttachmentBbsOfficialObservationTest.selectCases(group).toList()){
            var result=engine.selectDecision(new AnnouncementSourceClassificationInput("LOCAL_GOV_NOTICE",sample.title(),null,null,List.of(),BodySourceCode.NONE,BodyAvailabilityCode.UNAVAILABLE),rules);
            assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectTitleMayProceed(result)).as(sample.code()+" "+result.titleStageCode()).isTrue();
            var ref=java.util.stream.StreamSupport.stream(catalog.spliterator(),false).filter(n->sample.code().equals(n.path("caseCode").asText())).findFirst().orElseThrow();
            assertThat(ref.path("source")).isEqualTo(json.valueToTree(sample.source()));assertThat(ref.hasNonNull("expectation")).isFalse();
        }
    }
    @Test @EnabledIfEnvironmentVariable(named="SANEB_BUSAN_DONGGU_SAHA_SAVED_DETAILS",matches=".+")
    void savedOfficialPagesProveFullSetAndNoFilesWithoutNetwork() throws Exception {
        Path root=Path.of(System.getenv("SANEB_BUSAN_DONGGU_SAHA_SAVED_DETAILS")).toRealPath();assertThat(root.startsWith(Path.of("build").toRealPath())).isTrue();
        for(String group:selectGroups().toList())for(var sample:AnnouncementAttachmentBbsOfficialObservationTest.selectCases(group).toList()){
            Path path=root.resolve(sample.code()+".html");assertThat(Files.size(path)).isLessThan(1048576);String html=Files.readString(path);
            AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(Jsoup.parse(html),sample.title(),sample.titleLayout());
            var r=sample.profile().selectDescriptors(sample.source(),html);assertThat(r.complete()).as(sample.code()).isTrue();assertThat(r.status()).isEqualTo(sample.listedFileCount()==0?"NO_FILES":"FOUND");
            assertThat(r.descriptors()).hasSize(sample.listedFileCount()).allSatisfy(d->{assertThat(d.downloadAllowed()).isTrue();assertThat(d.documentRole()).isEqualTo("UNKNOWN");assertThat(d.postForm()).isEmpty();});
        }
    }
}
