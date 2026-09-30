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

class BusanCitySupportCollectionContractTest {
    private final AnnouncementAttachmentBbsOfficialObservationTest.ObservationCase sample=BusanCitySupportDownloadCases.selectCase();

    @Test void officialSupportTitleAndExistingProfileKeepBoundedCollection() throws Exception {
        assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectCases("BUSAN_CITY_SUPPORT").map(c->c.code()).toList()).containsExactly(sample.code());
        assertThat(sample.profile().selectDetailUri(sample.source()).toString()).isEqualTo(sample.source().sourceUrl());
        assertThat(sample.profile().selectProfileCode()).isEqualTo("LOCAL_BUSAN_LEGAL_GET_V1");
        var decision=new AnnouncementSourceClassificationEngine().selectDecision(new AnnouncementSourceClassificationInput("LOCAL_GOV_NOTICE",sample.title(),null,null,List.of(),
                AnnouncementSourceClassificationCodes.BodySourceCode.NONE,AnnouncementSourceClassificationCodes.BodyAvailabilityCode.UNAVAILABLE),AnnouncementAttachmentRealFileQaTest.selectDraftRuleSet());
        assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectTitleMayProceed(decision)).isTrue();
        var budget=new AnnouncementAttachmentBbsOfficialObservationTest.Budget(sample.profile(),false);
        assertThat(budget.maximumRequests).isEqualTo(6);assertThat(budget.maximumBytes).isEqualTo(23L*1024*1024);
    }

    @Test void titleMustBelongToSingleOfficialViewAndMatchFixedSample() {
        String view="<div class=boardView><div class=form-group><h4 class=form-data-subject>"+sample.title()+"</h4></div></div>";
        AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(Jsoup.parse("<nav>다른 메뉴 제목</nav>"+view),sample.title(),sample.titleLayout());
        for(String bad:List.of(view+view,view.replace("boardView","other"),view.replace(sample.title(),"다른 공고"),view.replace("</h4>","</h4><h4 class=form-data-subject>중복</h4>"))) {
            assertThatThrownBy(()->AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(Jsoup.parse(bad),sample.title(),sample.titleLayout())).isInstanceOf(AssertionError.class);
        }
    }

    @Test void observedZeroSequenceAndDuplicateDownloadLinkUseOneDescriptor() {
        String path="/nbgosi/download?fileId=F26091517382020184&amp;seq=0";
        String html="<dl class=form-data-info><dt>첨부파일</dt><dd><ul class=attfiles><li><a href='"+path+"' title='공고.hwpx'>공고.hwpx</a>"
                +"<a href='"+path+"' class='btnTypeS btnColorType5' title='새창'>다운로드</a></li></ul></dd><dt>조회수</dt><dd>1</dd></dl>";
        var result=sample.profile().selectDescriptors(sample.source(),html);
        assertThat(result.complete()).isTrue();assertThat(result.descriptors()).hasSize(1);
        assertThat(result.descriptors().getFirst().expectedFormat()).isEqualTo("HWPX");
        assertThat(result.descriptors().getFirst().documentRole()).isEqualTo("UNKNOWN");
        assertThat(sample.profile().selectApprovedRequest(result.descriptors().getFirst().selectRequest())).isTrue();
    }

    @Test void catalogAddsOnlyReferenceWithExactSourceIdentity() throws Exception {
        var mapper=new ObjectMapper();var notices=mapper.readTree(Files.readString(Path.of("src/main/resources/announcement-attachment/provider-qa-catalog-v2.json"))).path("notices");
        var matches=StreamSupport.stream(notices.spliterator(),false).filter(n->sample.code().equals(n.path("caseCode").asText())).toList();
        assertThat(matches).hasSize(1);assertThat(matches.getFirst().path("source")).isEqualTo(mapper.valueToTree(sample.source()));
        assertThat(matches.getFirst().path("profileCode").asText()).isEqualTo(sample.profile().selectProfileCode());
        assertThat(matches.getFirst().hasNonNull("expectation")).isFalse();
    }
}
