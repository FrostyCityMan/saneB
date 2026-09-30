package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.domain.announcementsource.classification.*;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.*;
import java.nio.file.*;
import java.util.stream.StreamSupport;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

class GoesanDownloadContractTest {
    @Test void fixedCaseTitleAndReferenceOnlyCatalogMatch() throws Exception {
        var sample=GoesanDownloadCases.selectCase();var json=new ObjectMapper();
        var notices=json.readTree(Files.readString(Path.of("src/main/resources/announcement-attachment/provider-qa-catalog-v2.json"))).path("notices");
        var entry=StreamSupport.stream(notices.spliterator(),false).filter(n->sample.code().equals(n.path("caseCode").asText())).findFirst().orElseThrow();
        assertThat(entry.path("source")).isEqualTo(json.valueToTree(sample.source()));
        assertThat(entry.path("profileCode").asText()).isEqualTo(sample.profile().selectProfileCode());
        assertThat(entry.hasNonNull("expectation")).isFalse();
        String page="<form name=form1 method=post><div><table class=table_view><tr><th>제목</th><td>"+sample.title()+"</td><th>담당부서</th><td>경제과</td></tr></table></div></form>";
        AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(Jsoup.parse(page),sample.title(),sample.titleLayout());
        assertThatThrownBy(()->AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(Jsoup.parse(page),"다른 제목",sample.titleLayout())).isInstanceOf(AssertionError.class);
        assertThatThrownBy(()->AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(Jsoup.parse(page+page),sample.title(),sample.titleLayout())).isInstanceOf(AssertionError.class);
        var decision=new AnnouncementSourceClassificationEngine().selectDecision(new AnnouncementSourceClassificationInput("LOCAL_GOV_NOTICE",sample.title(),null,null,java.util.List.of(),BodySourceCode.NONE,BodyAvailabilityCode.UNAVAILABLE),AnnouncementAttachmentRealFileQaTest.selectDraftRuleSet());
        assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectTitleMayProceed(decision)).isTrue();
        var budget=AnnouncementAttachmentBbsOfficialObservationTest.selectBudget(sample.profile(),true,false);
        assertThat(budget.maximumRequests).isEqualTo(6);assertThat(budget.maximumBytes).isEqualTo(23L*1024*1024);
    }
    @Test void officialAttachmentAndPreviewFailureRemainSeparate() {
        var sample=GoesanDownloadCases.selectCase();
        String link="<a href=\"javascript:goDownLoad('지원 공고.hwp','공고_ofr_20260127.hwp','/ntishome/file/upload/ofr/ofr/20260127')\">지원 공고.hwp</a>";
        String page="<form name=form1 method=post><table class=table_view><tr><th>첨부파일</th><td colspan=3>"+link+"</td></tr></table></form>";
        var files=sample.profile().selectDescriptors(sample.source(),page);
        assertThat(files.complete()).isTrue();assertThat(files.descriptors()).hasSize(1);
        var file=files.descriptors().getFirst();assertThat(file.expectedFormat()).isEqualTo("HWP");assertThat(file.documentRole()).isEqualTo("UNKNOWN");
        assertThat(file.selectRequest().method()).isEqualTo("GET");assertThat(file.fetchUri().getHost()).isEqualTo("eminwon.goesan.go.kr");
        var partial=sample.profile().selectDescriptors(sample.source(),page.replace("</td>","<a href=\"javascript:preview('x')\">바로보기</a></td>"));
        assertThat(partial.complete()).isFalse();assertThat(partial.descriptors()).hasSize(1);
    }
}
