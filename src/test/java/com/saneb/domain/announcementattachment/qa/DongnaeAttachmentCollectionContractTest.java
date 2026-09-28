package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import java.nio.file.*;
import java.util.List;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import com.saneb.domain.announcementsource.classification.*;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.*;

class DongnaeAttachmentCollectionContractTest {
    @Test void adapterRejectsOtherFormsHostsSourcesAndRedirectSubstitution() {
        var sample=AnnouncementAttachmentBbsOfficialObservationTest.selectCases("DONGNAE").findFirst().orElseThrow();
        var profile=sample.profile();var source=sample.source();
        String html="<form name='form2' method='post'><table class='tb_t1'><tr><td>첨부파일 :</td><td>"
                +"<a href=\"javascript:goDownLoad('공고.pdf','notice.pdf','/ntishome/file/upload/ofr/ofr/20260909')\">공고.pdf</a>"
                +"</td></tr></table></form>";
        assertThat(profile.selectDescriptors(source,html).complete()).isTrue();
        for(String changed:List.of(html+html,html.replace("form2","form1"),html.replace("tb_t1","unknown"),
                html.replace("method='post'","method='get'")))assertThat(profile.selectDescriptors(source,changed).complete()).isFalse();
        var altered=new com.saneb.domain.announcementattachment.discovery.AttachmentDiscoveryProfile.Source(
                source.providerCode(),source.providerNoticeId(),source.sourceUrl(),"LGS-000034",source.listParserProfileCode());
        assertThatThrownBy(()->profile.selectDetailUri(altered)).hasMessage("PROFILE_REQUIRED");
        var first=com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request.selectGet(profile.selectDetailUri(source));
        var other=com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request.selectGet(
                java.net.URI.create(first.uri().toString().replace("43807","43719")));
        assertThat(profile.selectApprovedRequest(first,first)).isTrue();
        assertThat(profile.selectApprovedRequest(first,other)).isFalse();
        assertThat(profile.selectApprovedRequest(java.net.URI.create(first.uri().toString().replace("eminwon.dongnae.go.kr","other.go.kr")))).isFalse();
    }
    @Test void threeFixedSupportSamplesKeepTitlePolicyAndFiniteBudget() throws Exception {
        var samples=AnnouncementAttachmentBbsOfficialObservationTest.selectCases("DONGNAE").toList();
        assertThat(samples).hasSize(3);
        var rules=AnnouncementAttachmentRealFileQaTest.selectDraftRuleSet();
        var engine=new AnnouncementSourceClassificationEngine();
        var json=new com.fasterxml.jackson.databind.ObjectMapper();
        var catalog=json.readTree(Files.readString(Path.of("src/main/resources/announcement-attachment/provider-qa-catalog-v2.json"))).path("notices");
        for(var sample:samples) {
            var reference=java.util.stream.StreamSupport.stream(catalog.spliterator(),false).filter(n->sample.code().equals(n.path("caseCode").asText())).findFirst().orElseThrow();
            assertThat(reference.path("source")).isEqualTo(json.valueToTree(sample.source()));
            assertThat(reference.hasNonNull("expectation")).isFalse();
            var result=engine.selectDecision(new AnnouncementSourceClassificationInput("LOCAL_GOV_NOTICE",sample.title(),null,null,List.of(),
                    BodySourceCode.NONE,BodyAvailabilityCode.UNAVAILABLE),rules);
            assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectTitleMayProceed(result)).as(sample.code()).isTrue();
            assertThat(sample.listedFileCount()).isEqualTo(1);
            assertThat(sample.expectedTitleStopStage()).isNull();
            assertThat(sample.profile().selectDetailUri(sample.source()).getHost()).isEqualTo("eminwon.dongnae.go.kr");
            for(boolean diagnostic:List.of(false,true)) {
                var budget=new AnnouncementAttachmentBbsOfficialObservationTest.Budget(sample.profile(),diagnostic);
                assertThat(budget.maximumRequests).isEqualTo(6);
                assertThat(budget.maximumBytes).isEqualTo(23L*1024*1024);
            }
        }
    }
    @Test void titleMustBelongToUniqueMeasuredTable() {
        String title="동래구 청년 지원사업",table="<table class='tb_t1'><tr><th>제목</th><td colspan='3'>"+title+"</td></tr></table>";
        String html="<form name='form2' method='post'>"+table+"</form>";
        var layout=AnnouncementAttachmentBbsOfficialObservationTest.TitleLayout.DONGNAE_LABEL;
        java.util.function.Consumer<String> validate=h->AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(Jsoup.parse(h),title,layout);
        validate.accept(html);
        for(String invalid:List.of(html+html,html.replace("tb_t1","other"),html.replace("colspan='3'","colspan='2'"),
                html.replace(title,"다른 제목"),html.replace(title,"<span>"+title+"</span>"),html.replace("</form>",table+"</form>")))
            assertThatThrownBy(()->validate.accept(invalid)).isInstanceOf(AssertionError.class);
    }
    @Test @EnabledIfEnvironmentVariable(named="SANEB_DONGNAE_SAVED_DETAILS",matches=".+")
    void savedOfficialDetailsHaveExactlyOneOwnedSupportedFile() throws Exception {
        Path root=Path.of(System.getenv("SANEB_DONGNAE_SAVED_DETAILS")).toRealPath();
        assertThat(root.startsWith(Path.of("build").toRealPath())).isTrue();
        for(var sample:AnnouncementAttachmentBbsOfficialObservationTest.selectCases("DONGNAE").toList()) {
            Path path=root.resolve(sample.code().substring("DONGNAE-".length())+".html");
            assertThat(Files.size(path)).isLessThan(1048576);
            String html=Files.readString(path);
            AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(Jsoup.parse(html),sample.title(),sample.titleLayout());
            var result=sample.profile().selectDescriptors(sample.source(),html);
            assertThat(result.status()).isEqualTo("FOUND");assertThat(result.complete()).isTrue();
            assertThat(result.descriptors()).singleElement().satisfies(d->{
                assertThat(d.downloadAllowed()).isTrue();assertThat(d.documentRole()).isEqualTo("UNKNOWN");
                assertThat(d.expectedFormat()).isEqualTo(sample.code().endsWith("43807")?"PDF":"HWP");
                assertThat(d.fetchUri().getHost()).isEqualTo("eminwon.dongnae.go.kr");
                assertThat(d.fetchUri().getPath()).isEqualTo("/emwp/jsp/ofr/FileDown.jsp");
            });
        }
    }
}
