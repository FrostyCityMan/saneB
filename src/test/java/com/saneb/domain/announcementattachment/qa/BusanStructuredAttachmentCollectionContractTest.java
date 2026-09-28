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
import com.saneb.domain.announcementattachment.discovery.AttachmentDiscoveryProfile;
import com.saneb.domain.announcementsource.classification.*;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.*;

class BusanStructuredAttachmentCollectionContractTest {
    static Stream<String> selectGroups(){return Stream.of("SUYEONG","SASANG");}
    static String selectLink(String format){return "<a href=\"javascript:goDownLoad('문서."+format+"','123."+format+"','/ntishome/file/upload/ofr/ofr/20260928')\">첨부</a>";}
    static String selectPage(String group,String links){
        return "<form name='form1' method='post'>"+(group.equals("SUYEONG")
                ?"<div class='view01'><h3>청년 지원사업</h3><dl><dt>첨부파일</dt><dd>"+links+"</dd></dl></div>"
                :"<table class='basic'><thead class='tb'><tr><th class='fi_la' colspan='2'>청년 지원사업</th></tr></thead><tbody><tr><td colspan='2'><strong>첨부파일 : </strong>"+links+"</td></tr></tbody></table>")+"</form>";
    }
    @ParameterizedTest @MethodSource("selectGroups") void wholeContainerKeepsFormatsUnknownRolesAndUnsupportedFiles(String group){
        var sample=AnnouncementAttachmentBbsOfficialObservationTest.selectCases(group).findFirst().orElseThrow();
        var profile=sample.profile();String links=selectLink("pdf")+selectLink("hwp")+selectLink("hwpx");
        var result=profile.selectDescriptors(sample.source(),selectPage(group,links));
        assertThat(result.status()).isEqualTo("FOUND");assertThat(result.complete()).isTrue();
        assertThat(result.descriptors()).extracting(AttachmentDiscoveryProfile.Descriptor::expectedFormat).containsExactly("PDF","HWP","HWPX");
        assertThat(result.descriptors()).allSatisfy(d->{assertThat(d.downloadAllowed()).isTrue();assertThat(d.documentRole()).isEqualTo("UNKNOWN");});
        var unsupported=profile.selectDescriptors(sample.source(),selectPage(group,links+selectLink("jpeg")));
        assertThat(unsupported.complete()).isTrue();assertThat(unsupported.descriptors()).hasSize(4);
        assertThat(unsupported.descriptors().getLast().downloadAllowed()).isFalse();
        assertThat(profile.selectDescriptors(sample.source(),selectPage(group,selectLink("hwp")+selectLink("hwp"))).descriptors()).hasSize(1);
        assertThat(profile.selectDescriptors(sample.source(),selectPage(group,"")).status()).isEqualTo("NO_FILES");
        for(String extra:List.of("<button>다른 파일</button>","<a href='/unknown'>첨부</a>","<script>hidden()</script>","파일이 더 있음")){
            var failed=profile.selectDescriptors(sample.source(),selectPage(group,links+extra));
            assertThat(failed.complete()).isFalse();assertThat(failed.status()).isEqualTo("FAILED");
        }
    }
    @ParameterizedTest @MethodSource("selectGroups") void rejectsAmbiguousOrMovedOfficialArea(String group){
        var sample=AnnouncementAttachmentBbsOfficialObservationTest.selectCases(group).findFirst().orElseThrow();
        String html=selectPage(group,selectLink("hwp"));var profile=sample.profile();
        for(String invalid:List.of(html+html,html.replace("form1","form2"),html.replace("첨부파일","기타"),
                html.replace("view01","other").replace("basic","other"),
                html.replace("첨부파일","<span>첨부파일</span>"),
                html.replace("<dd>","<dd onclick='hidden()'>").replace("<td colspan='2'>","<td colspan='2' onclick='hidden()'>"))){
            assertThat(profile.selectDescriptors(sample.source(),invalid).complete()).isFalse();
        }
        String duplicate=group.equals("SUYEONG")?"<dl><dt>첨부파일</dt><dd></dd></dl>":"<strong>첨부파일</strong>";
        assertThat(profile.selectDescriptors(sample.source(),html.replace("</form>",duplicate+"</form>")).complete()).isFalse();
        assertThat(profile.selectDescriptors(sample.source(),null).warnings()).contains("ATTACHMENT_DETAIL_UNAVAILABLE");
    }
    @ParameterizedTest @MethodSource("selectGroups") void pinsSourceParserHostAndDownloadMethod(String group){
        var sample=AnnouncementAttachmentBbsOfficialObservationTest.selectCases(group).findFirst().orElseThrow();var p=sample.profile();var s=sample.source();
        for(var changed:List.of(new AttachmentDiscoveryProfile.Source(s.providerCode(),s.providerNoticeId(),s.sourceUrl(),"LGS-000001",s.listParserProfileCode()),
                new AttachmentDiscoveryProfile.Source(s.providerCode(),s.providerNoticeId(),s.sourceUrl(),s.localSourceCode(),"WRONG"),
                new AttachmentDiscoveryProfile.Source(s.providerCode(),"a".repeat(64),s.sourceUrl(),s.localSourceCode(),s.listParserProfileCode())))
            assertThatThrownBy(()->p.selectDetailUri(changed)).hasMessage("PROFILE_REQUIRED");
        var request=p.selectDescriptors(s,selectPage(group,selectLink("hwp"))).descriptors().getFirst().selectRequest();
        assertThat(request.method()).isEqualTo("GET");assertThat(p.selectApprovedRequest(request)).isTrue();
        assertThat(p.selectApprovedRequest(URI.create(request.uri().toString().replace("https:","http:")))).isFalse();
        assertThat(p.selectApprovedRequest(URI.create(request.uri().toString().replace(request.uri().getHost(),"example.com")))).isFalse();
    }
    @ParameterizedTest @MethodSource("selectGroups") void titleIdentityCannotComeFromAnotherFormOrNestedContent(String group){
        var sample=AnnouncementAttachmentBbsOfficialObservationTest.selectCases(group).findFirst().orElseThrow();
        String html=selectPage(group,selectLink("hwp"));var layout=sample.titleLayout();
        AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(Jsoup.parse(html),"청년 지원사업",layout);
        for(String invalid:List.of(html+html,html.replace("form1","form2"),html.replace("청년 지원사업","다른 제목"),
                html.replace("청년 지원사업","<span>청년 지원사업</span>")))
            assertThatThrownBy(()->AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(Jsoup.parse(invalid),"청년 지원사업",layout)).isInstanceOf(AssertionError.class);
    }
    @Test void fixedSamplesKeepTitlePolicyAndReferenceOnlyCatalog() throws Exception {
        var rules=AnnouncementAttachmentRealFileQaTest.selectDraftRuleSet();var engine=new AnnouncementSourceClassificationEngine();
        var json=new com.fasterxml.jackson.databind.ObjectMapper();var catalog=json.readTree(Files.readString(Path.of("src/main/resources/announcement-attachment/provider-qa-catalog-v2.json"))).path("notices");
        for(String group:List.of("SUYEONG","SASANG"))for(var sample:AnnouncementAttachmentBbsOfficialObservationTest.selectCases(group).toList()){
            var result=engine.selectDecision(new AnnouncementSourceClassificationInput("LOCAL_GOV_NOTICE",sample.title(),null,null,List.of(),BodySourceCode.NONE,BodyAvailabilityCode.UNAVAILABLE),rules);
            assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectTitleMayProceed(result)).as(sample.code()).isTrue();
            var reference=java.util.stream.StreamSupport.stream(catalog.spliterator(),false).filter(n->sample.code().equals(n.path("caseCode").asText())).findFirst().orElseThrow();
            assertThat(reference.path("source")).isEqualTo(json.valueToTree(sample.source()));assertThat(reference.hasNonNull("expectation")).isFalse();
            var budget=new AnnouncementAttachmentBbsOfficialObservationTest.Budget(sample.profile(),true);
            assertThat(budget.maximumRequests).isEqualTo(6);assertThat(budget.maximumBytes).isEqualTo(23L*1024*1024);
        }
    }
    @Test @EnabledIfEnvironmentVariable(named="SANEB_BUSAN_STRUCTURED_SAVED_DETAILS",matches=".+")
    void savedOfficialPagesPreserveAllListedAttachmentsWithoutMoreRequests() throws Exception {
        Path root=Path.of(System.getenv("SANEB_BUSAN_STRUCTURED_SAVED_DETAILS")).toRealPath();assertThat(root.startsWith(Path.of("build").toRealPath())).isTrue();
        for(String group:List.of("SUYEONG","SASANG"))for(var sample:AnnouncementAttachmentBbsOfficialObservationTest.selectCases(group).toList()){
            Path path=root.resolve(sample.code()+".html");assertThat(Files.size(path)).isLessThan(1048576);String html=Files.readString(path);
            AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(Jsoup.parse(html),sample.title(),sample.titleLayout());
            var result=sample.profile().selectDescriptors(sample.source(),html);assertThat(result.complete()).as(sample.code()).isTrue();
            assertThat(result.status()).isEqualTo("FOUND");assertThat(result.descriptors()).hasSize(sample.listedFileCount());
            assertThat(result.descriptors().stream().filter(d->!d.downloadAllowed()).count()).isEqualTo(sample.code().equals("SASANG-40426")?1:0);
        }
    }
}
