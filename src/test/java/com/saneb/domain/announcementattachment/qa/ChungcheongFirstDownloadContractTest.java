package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import java.net.URI;
import java.util.List;
import java.util.stream.Stream;
import org.jsoup.Jsoup;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import com.saneb.domain.announcementattachment.discovery.AttachmentDiscoveryProfile;
import com.saneb.domain.announcementsource.classification.*;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient;

class ChungcheongFirstDownloadContractTest {
    static Stream<String> selectGroups(){return Stream.of("EUMSEONG","NONSAN","DANGJIN","CHEONGYANG");}
    private String selectLink(int n){return "<a href=\"javascript:goDownLoad('공고.pdf','"+n+".pdf','/ntishome/file/upload/ofr/ofr/20260929')\">공고.pdf</a><br>";}
    private String selectPage(String group,String title,String links){
        if(group.equals("NONSAN"))return "<form name=form1 method=post><table class=bbs_view><thead><tr class=head><th scope=row>제 목</th><td colspan=5>"+title+"</td></tr></thead><tbody><tr><th>첨부화일</th><td colspan=5>"+links+"</td></tr></tbody></table></form>";
        if(group.equals("EUMSEONG"))return "<form name=form1 method=post><table class=board_view><tr><th class=first scope=row>제목</th><td colspan=3>"+title+"</td></tr><tr><th>첨부파일</th><td colspan=3>"+links+"</td></tr></table></form>";
        return "<form name=form method=post><table width='98%' border=0 cellspacing=1 cellpadding=0><tr><td>제목</td><td>"+title+"</td></tr><tr><td>첨부파일 :</td><td>"+links+"</td></tr></table></form>";
    }
    @ParameterizedTest @MethodSource("selectGroups") void partialDiscoveryRetainsEveryKnownSafeFile(String group){
        var sample=AnnouncementAttachmentBbsOfficialObservationTest.selectCases(group).findFirst().orElseThrow();var p=sample.profile();
        var result=p.selectDescriptors(sample.source(),selectPage(group,sample.title(),selectLink(1)+"<a href='/unknown'>미확인</a>"+selectLink(2)));
        assertThat(result.status()).isEqualTo("FAILED");assertThat(result.complete()).isFalse();assertThat(result.descriptors()).hasSize(2);
        assertThat(result.warnings()).containsExactly("ATTACHMENT_LINK_UNRESOLVED");
        result.descriptors().forEach(d->{assertThat(d.downloadAllowed()).isTrue();assertThat(p.selectApprovedRequest(d.selectRequest())).isTrue();assertThat(d.documentRole()).isEqualTo("UNKNOWN");});
        assertThat(p.selectDescriptors(sample.source(),selectPage(group,sample.title(),"")).status()).isEqualTo("NO_FILES");
        assertThat(p.selectDescriptors(sample.source(),selectPage(group,sample.title(),selectLink(1)+selectLink(2))).descriptors()).hasSize(2);
    }
    @ParameterizedTest @MethodSource("selectGroups") void boundariesAndMalformedStructureCannotBeSilentlyAccepted(String group){
        var sample=AnnouncementAttachmentBbsOfficialObservationTest.selectCases(group).findFirst().orElseThrow();var p=sample.profile();var s=sample.source();
        assertThat(p.selectDescriptors(s,selectPage(group,sample.title(),selectLink(1)).replace("method=post","method=get")).complete()).isFalse();
        assertThatThrownBy(()->p.selectDetailUri(new AttachmentDiscoveryProfile.Source(s.providerCode(),s.providerNoticeId(),s.sourceUrl(),"LGS-000001",s.listParserProfileCode()))).isInstanceOf(IllegalArgumentException.class);
        var d=p.selectDescriptors(s,selectPage(group,sample.title(),selectLink(1))).descriptors().getFirst();
        assertThat(p.selectApprovedRequest(URI.create(d.fetchUri().toString().replace("https://","http://")))).isFalse();
        assertThat(p.selectApprovedRequest(AttachmentPinnedDownloadClient.Request.selectGet(URI.create("https://example.com/file.pdf")))).isFalse();
        assertThat(p.selectApprovedRequest(URI.create(d.fetchUri()+"&extra=1"))).isFalse();
        assertThat(p.selectDescriptors(s,selectPage(group,sample.title(),"<button>다른 파일</button>")).complete()).isFalse();
    }
    @ParameterizedTest @MethodSource("selectGroups") void officialTitleAndSeedGateAreIndependentOfDownloadSuccess(String group)throws Exception{
        var sample=AnnouncementAttachmentBbsOfficialObservationTest.selectCases(group).findFirst().orElseThrow();
        var decision=new AnnouncementSourceClassificationEngine().selectDecision(new AnnouncementSourceClassificationInput("LOCAL_GOV_NOTICE",sample.title(),null,null,List.of(),AnnouncementSourceClassificationCodes.BodySourceCode.NONE,AnnouncementSourceClassificationCodes.BodyAvailabilityCode.UNAVAILABLE),AnnouncementAttachmentRealFileQaTest.selectDraftRuleSet());
        assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectTitleMayProceed(decision)).isTrue();
        var page=Jsoup.parse(selectPage(group,sample.title(),selectLink(1)));
        AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,sample.title(),sample.titleLayout());
        assertThatThrownBy(()->AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,"다른 공고",sample.titleLayout())).isInstanceOf(AssertionError.class);
    }
}
