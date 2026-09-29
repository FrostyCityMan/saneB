package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import java.net.URI;
import java.util.stream.Stream;
import org.jsoup.Jsoup;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import com.saneb.domain.announcementattachment.discovery.AttachmentDiscoveryProfile;
import com.saneb.domain.announcementsource.classification.*;

class ChungbukFirstDownloadContractTest {
    static Stream<String> selectGroups(){return Stream.of("JEUNGPYEONG","DANYANG");}
    private String selectPage(String group,String links){return "<form name='"+(group.equals("DANYANG")?"form":"form1")+"' method='post'>"
            +"<table width='"+(group.equals("DANYANG")?"98%":"100%")+"' border='0' cellspacing='1' cellpadding='0'><tr><td>제목</td><td>소상공인 지원사업</td></tr>"
            +"<tr><td>첨부파일 :</td><td>"+links+"</td></tr></table></form>";}
    private String selectLink(int n){return "<a href=\"javascript:goDownLoad('공고.pdf','"+n+".pdf','/ntishome/file/upload/ofr/ofr/20260929')\">공고.pdf</a><br>";}
    @ParameterizedTest @MethodSource("selectGroups") void collectsKnownFilesWhenAnotherLinkCannotBeParsed(String group){
        var sample=AnnouncementAttachmentBbsOfficialObservationTest.selectCases(group).findFirst().orElseThrow();var p=sample.profile();
        var result=p.selectDescriptors(sample.source(),selectPage(group,selectLink(1)+selectLink(2)+"<a href='/unknown'>미확인 첨부</a>"));
        assertThat(result.status()).isEqualTo("FAILED");assertThat(result.complete()).isFalse();assertThat(result.descriptors()).hasSize(2);
        assertThat(result.warnings()).containsExactly("ATTACHMENT_LINK_UNRESOLVED");
        result.descriptors().forEach(d->{assertThat(d.downloadAllowed()).isTrue();assertThat(p.selectApprovedRequest(d.selectRequest())).isTrue();assertThat(d.documentRole()).isEqualTo("UNKNOWN");});
        assertThat(p.selectDescriptors(sample.source(),selectPage(group,"" )).status()).isEqualTo("NO_FILES");
    }
    @ParameterizedTest @MethodSource("selectGroups") void fixedSourceFormAndRequestIdentityCannotBeExpanded(String group){
        var sample=AnnouncementAttachmentBbsOfficialObservationTest.selectCases(group).findFirst().orElseThrow();var p=sample.profile();
        assertThat(p.selectDescriptors(sample.source(),selectPage(group,selectLink(1)).replace("method='post'","method='get'")).complete()).isFalse();
        var s=sample.source();assertThatThrownBy(()->p.selectDetailUri(new AttachmentDiscoveryProfile.Source(s.providerCode(),s.providerNoticeId(),s.sourceUrl(),"LGS-000001",s.listParserProfileCode())))
                .isInstanceOf(IllegalArgumentException.class);
        var d=p.selectDescriptors(s,selectPage(group,selectLink(1))).descriptors().getFirst();
        assertThat(p.selectApprovedRequest(URI.create(d.fetchUri().toString().replace("https://","http://")))).isFalse();
        var other=com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request.selectGet(URI.create(d.fetchUri()+"&extra=1"));
        assertThat(p.selectApprovedRequest(d.selectRequest(),other)).isFalse();
    }
    @ParameterizedTest @MethodSource("selectGroups") void fixedOfficialTitlePassesDraftGateAndTitleIdentityIsChecked(String group)throws Exception{
        var sample=AnnouncementAttachmentBbsOfficialObservationTest.selectCases(group).findFirst().orElseThrow();
        var decision=new AnnouncementSourceClassificationEngine().selectDecision(new AnnouncementSourceClassificationInput("LOCAL_GOV_NOTICE",sample.title(),null,null,java.util.List.of(),
                AnnouncementSourceClassificationCodes.BodySourceCode.NONE,AnnouncementSourceClassificationCodes.BodyAvailabilityCode.UNAVAILABLE),AnnouncementAttachmentRealFileQaTest.selectDraftRuleSet());
        assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectTitleMayProceed(decision)).isTrue();
        var page=Jsoup.parse(selectPage(group,selectLink(1)).replace("소상공인 지원사업",sample.title()));
        AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,sample.title(),sample.titleLayout());
        assertThatThrownBy(()->AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,"다른 공고",sample.titleLayout())).isInstanceOf(AssertionError.class);
    }
}
