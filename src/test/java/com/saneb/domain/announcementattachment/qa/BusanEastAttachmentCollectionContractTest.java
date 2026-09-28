package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import java.net.URI;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Stream;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.classification.*;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.*;

class BusanEastAttachmentCollectionContractTest {
    static Stream<String> selectGroups(){return Stream.of("HAEUNDAE","GIJANG");}
    static String selectLink(String format){return "<a href=\"javascript:goDownLoad('문서."+format+"','123."+format+"','/ntishome/file/upload/ofr/ofr/20260928')\">첨부</a>";}
    static String selectGijangPage(String contents){return "<form name='form' method='post'><table class='tb_board_read'><thead><tr><th scope='col'>청년 지원사업</th></tr></thead><tbody><tr><td class='attach_list'>"+contents+"</td></tr></tbody></table></form>";}
    static String selectHaeundaePage(String contents){return "<form name='form1' method='post'><article class='news_view'><h2 class='newsTitle'>청년 지원사업<p class='small'>등록일 ㅣ 2026-09-28</p></h2><table class='tstyle_view'><tr><th>첨부파일</th><td>"+contents+"</td></tr></table></article></form>";}
    @Test void gijangKeepsAllFilesAndOnlyRemovesObservedLeadingLiteral(){
        var sample=AnnouncementAttachmentBbsOfficialObservationTest.selectCases("GIJANG").findFirst().orElseThrow();var p=sample.profile();var s=sample.source();
        String links=selectLink("pdf")+selectLink("hwp")+selectLink("hwpx");
        for(String prefix:List.of(""," \n< \n")){
            var r=p.selectDescriptors(s,selectGijangPage(prefix+links));assertThat(r.complete()).isTrue();assertThat(r.descriptors()).hasSize(3);
            assertThat(r.descriptors()).extracting(AttachmentDiscoveryProfile.Descriptor::expectedFormat).containsExactly("PDF","HWP","HWPX");
            assertThat(r.descriptors()).allSatisfy(d->{assertThat(d.documentRole()).isEqualTo("UNKNOWN");assertThat(d.downloadAllowed()).isTrue();});
        }
        for(String prefix:List.of("<<","< 누락파일","다른 파일", "<span>&lt;</span>"))assertThat(p.selectDescriptors(s,selectGijangPage(prefix+links)).complete()).isFalse();
        assertThat(p.selectDescriptors(s,selectGijangPage("<")).complete()).isFalse();
        assertThat(p.selectDescriptors(s,selectGijangPage("")).status()).isEqualTo("NO_FILES");
        var mixed=p.selectDescriptors(s,selectGijangPage("< "+links+selectLink("jpeg")));
        assertThat(mixed.descriptors()).hasSize(4);assertThat(mixed.descriptors().getLast().downloadAllowed()).isFalse();
        assertThat(p.selectDescriptors(s,selectGijangPage("< "+selectLink("hwp")+selectLink("hwp"))).descriptors()).hasSize(1);
    }
    @Test void gijangDoesNotBorrowAnotherAreaOrHideUnresolvedLinks(){
        var sample=AnnouncementAttachmentBbsOfficialObservationTest.selectCases("GIJANG").findFirst().orElseThrow();var p=sample.profile();var s=sample.source();
        String html=selectGijangPage("< "+selectLink("hwp"));
        for(String invalid:List.of(html+html,html.replace("name='form'","name='form1'"),html.replace("attach_list","other"),html.replace("tb_board_read","other"),
                html.replace("<td class=","<td onclick='hidden()' class="),html.replace("</tr></tbody>","<td>별도 칸</td></tr></tbody>"),
                html.replace("</tbody>","<tr><td class='attach_list'></td></tr></tbody>")))assertThat(p.selectDescriptors(s,invalid).complete()).isFalse();
        for(String extra:List.of("<a href='/unknown'>다른 첨부</a>","<button>파일</button>","<script>hidden()</script>","나머지 파일"))
            assertThat(p.selectDescriptors(s,selectGijangPage("< "+selectLink("hwp")+extra)).complete()).isFalse();
    }
    @ParameterizedTest @MethodSource("selectGroups") void titleCannotBeBorrowedFromDateOrNestedContent(String group){
        var sample=AnnouncementAttachmentBbsOfficialObservationTest.selectCases(group).findFirst().orElseThrow();
        String html=group.equals("GIJANG")?selectGijangPage(selectLink("hwp")):selectHaeundaePage(selectLink("hwp"));
        AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(Jsoup.parse(html),"청년 지원사업",sample.titleLayout());
        for(String invalid:List.of(html+html,html.replace("청년 지원사업","다른 제목"),html.replace("청년 지원사업","<span>청년 지원사업</span>")))
            assertThatThrownBy(()->AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(Jsoup.parse(invalid),"청년 지원사업",sample.titleLayout())).isInstanceOf(AssertionError.class);
        if(group.equals("HAEUNDAE"))assertThatThrownBy(()->AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(Jsoup.parse(html.replace("등록일 ㅣ 2026-09-28","청년 지원사업")),"청년 지원사업",sample.titleLayout())).isInstanceOf(AssertionError.class);
    }
    @ParameterizedTest @MethodSource("selectGroups") void exactSourceHostAndBudgetAreFixed(String group){
        var sample=AnnouncementAttachmentBbsOfficialObservationTest.selectCases(group).findFirst().orElseThrow();var p=sample.profile();var s=sample.source();
        assertThat(p.selectDetailUri(s).getScheme()).isEqualTo("https");
        for(var changed:List.of(new AttachmentDiscoveryProfile.Source(s.providerCode(),s.providerNoticeId(),s.sourceUrl(),"LGS-000001",s.listParserProfileCode()),
                new AttachmentDiscoveryProfile.Source(s.providerCode(),s.providerNoticeId(),s.sourceUrl(),s.localSourceCode(),"WRONG"),
                new AttachmentDiscoveryProfile.Source(s.providerCode(),"a".repeat(64),s.sourceUrl(),s.localSourceCode(),s.listParserProfileCode())))
            assertThatThrownBy(()->p.selectDetailUri(changed)).hasMessage("PROFILE_REQUIRED");
        assertThat(p.selectApprovedRequest(URI.create(s.sourceUrl().replace("https:","http:")))).isFalse();
        assertThat(p.selectApprovedRequest(URI.create(s.sourceUrl().replace(p.selectDetailUri(s).getHost(),"example.com")))).isFalse();
        for(boolean diagnostic:List.of(true,false)){var b=new AnnouncementAttachmentBbsOfficialObservationTest.Budget(p,diagnostic);
            assertThat(b.maximumRequests).isEqualTo(group.equals("GIJANG")?7:6);assertThat(b.maximumBytes).isEqualTo(23L*1024*1024);}
    }
    @Test void fixedTitlesFollowDraftPolicyAndCatalogRemainsReferenceOnly() throws Exception {
        var rules=AnnouncementAttachmentRealFileQaTest.selectDraftRuleSet();var engine=new AnnouncementSourceClassificationEngine();
        var json=new com.fasterxml.jackson.databind.ObjectMapper();var catalog=json.readTree(Files.readString(Path.of("src/main/resources/announcement-attachment/provider-qa-catalog-v2.json"))).path("notices");
        for(String group:List.of("HAEUNDAE","GIJANG"))for(var sample:AnnouncementAttachmentBbsOfficialObservationTest.selectCases(group).toList()){
            var result=engine.selectDecision(new AnnouncementSourceClassificationInput("LOCAL_GOV_NOTICE",sample.title(),null,null,List.of(),BodySourceCode.NONE,BodyAvailabilityCode.UNAVAILABLE),rules);
            assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectTitleMayProceed(result)).as(sample.code()).isTrue();
            var reference=java.util.stream.StreamSupport.stream(catalog.spliterator(),false).filter(n->sample.code().equals(n.path("caseCode").asText())).findFirst().orElseThrow();
            assertThat(reference.path("source")).isEqualTo(json.valueToTree(sample.source()));assertThat(reference.hasNonNull("expectation")).isFalse();
        }
    }
    @Test @EnabledIfEnvironmentVariable(named="SANEB_BUSAN_EAST_SAVED_DETAILS",matches=".+")
    void savedOfficialPagesProveWholeSetsWithoutNewRequests() throws Exception {
        Path root=Path.of(System.getenv("SANEB_BUSAN_EAST_SAVED_DETAILS")).toRealPath();assertThat(root.startsWith(Path.of("build").toRealPath())).isTrue();
        for(String group:List.of("HAEUNDAE","GIJANG"))for(var sample:AnnouncementAttachmentBbsOfficialObservationTest.selectCases(group).toList()){
            Path path=root.resolve(sample.code()+".html");assertThat(Files.size(path)).isLessThan(1048576);String html=Files.readString(path);
            AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(Jsoup.parse(html),sample.title(),sample.titleLayout());
            var r=sample.profile().selectDescriptors(sample.source(),html);assertThat(r.complete()).as(sample.code()).isTrue();assertThat(r.status()).isEqualTo("FOUND");
            assertThat(r.descriptors()).hasSize(sample.listedFileCount()).allSatisfy(d->{assertThat(d.downloadAllowed()).isTrue();assertThat(d.documentRole()).isEqualTo("UNKNOWN");});
        }
    }
}
