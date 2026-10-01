package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.classification.*;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.StreamSupport;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

class UlsanNamguDownloadContractTest {
    @Test void fixedReferenceIsBoundButNotApproved() throws Exception {
        var sample=UlsanNamguDownloadCases.selectCase();var json=new ObjectMapper();
        var notices=json.readTree(Files.readString(Path.of("src/main/resources/announcement-attachment/provider-qa-catalog-v2.json"))).path("notices");
        var entry=StreamSupport.stream(notices.spliterator(),false).filter(n->sample.code().equals(n.path("caseCode").asText())).findFirst().orElseThrow();
        assertThat(entry.path("source")).isEqualTo(json.valueToTree(sample.source()));
        assertThat(entry.path("profileCode").asText()).isEqualTo(sample.profile().selectProfileCode());
        assertThat(entry.hasNonNull("expectation")).isFalse();
        assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectCases("ULSAN_NAMGU").count()).isEqualTo(1);
        assertThat(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(sample.title().getBytes(StandardCharsets.UTF_8))))
                .isEqualTo("2893374bb36426de317c4a6dc0a6e8f0553915b4aa3ec9129d0d0dba3e8c4108");
        var decision=new AnnouncementSourceClassificationEngine().selectDecision(new AnnouncementSourceClassificationInput("LOCAL_GOV_NOTICE",sample.title(),null,null,List.of(),BodySourceCode.NONE,BodyAvailabilityCode.UNAVAILABLE),AnnouncementAttachmentRealFileQaTest.selectDraftRuleSet());
        assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectTitleMayProceed(decision)).isTrue();
        try(var context=new AnnotationConfigApplicationContext(UlsanAttachmentProfileConfiguration.class)) {
            var matches=context.getBeansOfType(AttachmentDiscoveryProfile.class).values().stream()
                    .filter(p->p.selectSourceBindings().stream().anyMatch(b->b.localSourceCode().equals("LGS-000079"))).toList();
            assertThat(matches).singleElement().satisfies(p->{assertThat(p.selectProfileHash()).isEqualTo(sample.profile().selectProfileHash());assertThat(p.selectDetailUri(sample.source()).toString()).isEqualTo(sample.source().sourceUrl());});
        }
        var budget=AnnouncementAttachmentBbsOfficialObservationTest.selectBudget(sample.profile(),true,false);
        assertThat(budget.maximumRequests).isEqualTo(6);assertThat(budget.maximumBytes).isEqualTo(23L*1024*1024);
    }
    @Test void measuredTitleAndPartialAttachmentAreKeptSeparate() {
        var sample=UlsanNamguDownloadCases.selectCase();
        String page="<form name=form1 method=post><div class='bbs_detail bbs_detail_basic'><div class=bbs_detail_tit><h2>"+sample.title()+"</h2></div>"
                +"<ul class=bbs_detail_content2><li><a href='/unresolved' onclick=\"goDownLoad('공고.hwpx','saved.hwpx','/ntishome/file/upload/ofr/ofr/20261001');\">공고.hwpx</a></li></ul>"
                +"<ul class=bbs_detail_content2><li>담당 정보</li></ul><div class=bbs-view-content>본문 안내</div></div></form>";
        AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(Jsoup.parse(page),sample.title(),sample.titleLayout());
        assertThatThrownBy(()->AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(Jsoup.parse(page),"다른 제목",sample.titleLayout())).isInstanceOf(AssertionError.class);
        assertThatThrownBy(()->AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(Jsoup.parse(page+page),sample.title(),sample.titleLayout())).isInstanceOf(IllegalArgumentException.class);
        var result=sample.profile().selectDescriptors(sample.source(),page);
        assertThat(result.complete()).isFalse();assertThat(result.warnings()).contains("ATTACHMENT_LINK_UNRESOLVED");
        assertThat(result.descriptors()).singleElement().satisfies(d->{assertThat(d.downloadAllowed()).isTrue();assertThat(d.documentRole()).isEqualTo("UNKNOWN");assertThat(d.fetchUri().getPath()).isEqualTo("/emwp/jsp/ofr/FileDown.jsp");});
    }
}
