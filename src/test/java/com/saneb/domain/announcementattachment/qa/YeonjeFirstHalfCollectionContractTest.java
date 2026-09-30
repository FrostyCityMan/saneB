package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.domain.announcementsource.classification.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.StreamSupport;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

class YeonjeFirstHalfCollectionContractTest {
    private final AnnouncementAttachmentBbsOfficialObservationTest.ObservationCase sample=YeonjeGuryeDownloadCases.selectYeonjeFirstHalfCase();

    @Test void newFixedCaseKeepsOldFailureCaseAndUnchangedTitlePolicy() throws Exception {
        assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectCases("YEONJE_FIRST_HALF").map(c->c.code()).toList()).containsExactly("YEONJE-42552");
        assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectCases("YEONJE_GURYE").map(c->c.code()).toList()).containsExactly("YEONJE-43358","GURYE-25440");
        assertThat(sample.profile().selectDetailUri(sample.source()).toString()).isEqualTo(sample.source().sourceUrl());
        var decision=new AnnouncementSourceClassificationEngine().selectDecision(new AnnouncementSourceClassificationInput("LOCAL_GOV_NOTICE",sample.title(),null,null,List.of(),
                AnnouncementSourceClassificationCodes.BodySourceCode.NONE,AnnouncementSourceClassificationCodes.BodyAvailabilityCode.UNAVAILABLE),AnnouncementAttachmentRealFileQaTest.selectDraftRuleSet());
        assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectTitleMayProceed(decision)).isTrue();
        var budget=AnnouncementAttachmentBbsOfficialObservationTest.selectBudget(sample.profile(),true,false);
        assertThat(budget.maximumRequests).isEqualTo(6);assertThat(budget.maximumBytes).isEqualTo(23L*1024*1024);
    }

    private String selectPage(){
        return "<form id=detailForm><div class=bod_wrap><div class=bod_view><h4>"+sample.title()+"</h4><div class=view_cont>본문</div>"
                +"<dl class=view_file><dt>첨부파일</dt><dd><a href='#' onclick=\"goDownload('공고.hwp', 'system.hwp', '/ntishome/file/upload/ofr/ofr/20260331'); return false;\">공고.hwp</a>"
                +"<a class=bt_white_s onclick=\"fn_egov_gosi_preview('42552','42552-0.hwp','공고.hwp','system.hwp','/ntishome/file/upload/ofr/ofr/20260331'); return false;\">바로 보기</a></dd></dl></div></div></form>";
    }

    @Test void oneOfficialHwpAndSameNoticePreviewDoNotDuplicateFiles(){
        var result=sample.profile().selectDescriptors(sample.source(),selectPage());
        assertThat(result.complete()).isTrue();assertThat(result.descriptors()).hasSize(1);
        var file=result.descriptors().getFirst();assertThat(file.expectedFormat()).isEqualTo("HWP");
        assertThat(file.documentRole()).isEqualTo("UNKNOWN");assertThat(file.downloadAllowed()).isTrue();
        assertThat(sample.profile().selectApprovedRequest(file.selectRequest())).isTrue();
        assertThat(sample.profile().selectDescriptors(sample.source(),selectPage().replace("'42552','42552-0.hwp'","'43358','43358-0.hwp'")).complete()).isFalse();
    }

    @Test void differentTitleCannotBorrowValidFiles(){
        AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(Jsoup.parse(selectPage()),sample.title(),sample.titleLayout());
        assertThatThrownBy(()->AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(Jsoup.parse(selectPage()),"다른 공고",sample.titleLayout())).isInstanceOf(AssertionError.class);
    }

    @Test void newCatalogReferenceCannotApprovePolicy() throws Exception {
        var mapper=new ObjectMapper();var notices=mapper.readTree(Files.readString(Path.of("src/main/resources/announcement-attachment/provider-qa-catalog-v2.json"))).path("notices");
        var selected=StreamSupport.stream(notices.spliterator(),false).filter(n->sample.code().equals(n.path("caseCode").asText())).toList();
        assertThat(selected).hasSize(1);assertThat(selected.getFirst().path("source")).isEqualTo(mapper.valueToTree(sample.source()));
        assertThat(selected.getFirst().path("profileCode").asText()).isEqualTo(sample.profile().selectProfileCode());
        assertThat(selected.getFirst().hasNonNull("expectation")).isFalse();
    }
}
