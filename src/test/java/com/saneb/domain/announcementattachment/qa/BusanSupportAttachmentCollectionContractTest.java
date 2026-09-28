package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import java.nio.file.*;
import java.util.List;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import com.saneb.domain.announcementsource.classification.*;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.*;

class BusanSupportAttachmentCollectionContractTest {
    @Test void fixedSamplesRespectTitlePolicyAndDoNotInflateGeumjeongCoverage() throws Exception {
        var rules=AnnouncementAttachmentRealFileQaTest.selectDraftRuleSet();
        var engine=new AnnouncementSourceClassificationEngine();
        var json=new com.fasterxml.jackson.databind.ObjectMapper();
        var catalog=json.readTree(Files.readString(Path.of("src/main/resources/announcement-attachment/provider-qa-catalog-v2.json"))).path("notices");
        for(String group:List.of("BUSANJIN","GEUMJEONG")) {
            var samples=AnnouncementAttachmentBbsOfficialObservationTest.selectCases(group).toList();
            assertThat(samples).hasSize(group.equals("BUSANJIN")?5:1);
            for(var sample:samples) {
                var result=engine.selectDecision(new AnnouncementSourceClassificationInput("LOCAL_GOV_NOTICE",sample.title(),null,null,List.of(),
                        BodySourceCode.NONE,BodyAvailabilityCode.UNAVAILABLE),rules);
                if(sample.expectedTitleStopStage()==null) {
                    assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectTitleMayProceed(result)).as(sample.code()).isTrue();
                } else {
                    assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectTitleMayProceed(result)).as(sample.code()).isFalse();
                    assertThat(result.titleStageCode()).as(sample.code()).isEqualTo(sample.expectedTitleStopStage());
                }
                var reference=java.util.stream.StreamSupport.stream(catalog.spliterator(),false).filter(n->sample.code().equals(n.path("caseCode").asText())).findFirst().orElseThrow();
                assertThat(reference.path("source")).isEqualTo(json.valueToTree(sample.source()));
                assertThat(reference.hasNonNull("expectation")).isFalse();
                for(boolean diagnostic:List.of(false,true)) {
                    var budget=new AnnouncementAttachmentBbsOfficialObservationTest.Budget(sample.profile(),diagnostic);
                    assertThat(budget.maximumRequests).isEqualTo(6);assertThat(budget.maximumBytes).isEqualTo(23L*1024*1024);
                }
            }
        }
    }
    @Test void measuredTitleSelectorsRejectDuplicateAndBorrowedTitles() {
        String title="청년 지원사업";
        String busanjin="<form name='form1' method='post'><table class='jin_gosi_table'><tr><th class='w_90'>제목</th><td class='w_330'>"+title+"</td></tr></table></form>";
        String geumjeong="<form name='form1' method='post'><table class='bbs_vtype'><thead><tr><th colspan='4'>"+title+"</th></tr></thead></table></form>";
        for(String group:List.of("BUSANJIN","GEUMJEONG")) {
            var layout=AnnouncementAttachmentBbsOfficialObservationTest.selectCases(group).findFirst().orElseThrow().titleLayout();
            String html=group.equals("BUSANJIN")?busanjin:geumjeong;
            java.util.function.Consumer<String> validate=h->AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(Jsoup.parse(h),title,layout);
            validate.accept(html);
            for(String invalid:List.of(html+html,html.replace(title,"다른 제목"),html.replace("form1","form2"),
                    html.replace(title,"<table><tr><td>"+title+"</td></tr></table>")))
                assertThatThrownBy(()->validate.accept(invalid)).isInstanceOf(AssertionError.class);
        }
    }
    @Test @EnabledIfEnvironmentVariable(named="SANEB_BUSAN_SUPPORT_SAVED_DETAILS",matches=".+")
    void savedOfficialDetailsProveWholeFileSetsWithoutDownloadingAgain() throws Exception {
        Path root=Path.of(System.getenv("SANEB_BUSAN_SUPPORT_SAVED_DETAILS")).toRealPath();
        assertThat(root.startsWith(Path.of("build").toRealPath())).isTrue();
        for(String group:List.of("BUSANJIN","GEUMJEONG"))for(var sample:AnnouncementAttachmentBbsOfficialObservationTest.selectCases(group).toList()) {
            Path file=root.resolve(sample.code()+".html");assertThat(Files.size(file)).isLessThan(1048576);
            String html=Files.readString(file);
            AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(Jsoup.parse(html),sample.title(),sample.titleLayout());
            var result=sample.profile().selectDescriptors(sample.source(),html);
            assertThat(result.status()).isEqualTo("FOUND");assertThat(result.complete()).isTrue();
            assertThat(result.descriptors()).hasSize(sample.listedFileCount()).allSatisfy(d->{
                assertThat(d.downloadAllowed()).isTrue();assertThat(d.documentRole()).isEqualTo("UNKNOWN");
                assertThat(d.expectedFormat()).isEqualTo(group.equals("BUSANJIN")?"HWP":"HWPX");
                assertThat(sample.profile().selectApprovedRequest(d.selectRequest())).isTrue();
            });
        }
    }
}
