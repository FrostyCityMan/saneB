package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.domain.announcementattachment.discovery.AttachmentDiscoveryProfile;
import com.saneb.domain.announcementsource.provider.content.CheonanNoticePage;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

class CheonanDownloadContractTest {
    private final AnnouncementAttachmentBbsOfficialObservationTest.ObservationCase sample=CheonanDownloadCases.selectCase();
    private String link(int id,String format){return "<a href=\"javascript:goDownLoad('지원 공고"+id+"."+format+"','saved"+id+"."+format+"','/ntishome/file/upload/ofr/ofr/20210118')\">지원 공고</a>";}
    private String page(String files){return "<form name=form method=post><table width=98% border=0 cellspacing=1 cellpadding=0><tr><td>제목</td><td>"+sample.title()+"</td><td>담당부서</td><td>경제과</td></tr><tr><td colspan=4 style='word-break:break-all;'>소상공인 자금 지원</td></tr><tr><td colspan=4><table><tr><td>첨부파일 : </td><td>"+files+"</td></tr></table></td></tr></table></form>";}
    @Test void officialNestedAttachmentCellRetainsAllSupportedAndUnsupportedFiles(){
        var result=sample.profile().selectDescriptors(sample.source(),"<a href='/outside.pdf'>외부</a>"+page(link(1,"hwp")+link(2,"hwpx")+link(3,"pdf")+link(4,"xlsx")));
        assertThat(result.complete()).isTrue();assertThat(result.descriptors()).hasSize(4);
        for(var file:result.descriptors()){
            assertThat(file.documentRole()).isEqualTo("UNKNOWN");assertThat(file.selectRequest().method()).isEqualTo("GET");
            assertThat(file.fetchUri().getHost()).isEqualTo("eminwon.cheonan.go.kr");assertThat(file.locator().toString()).doesNotContain("saved","지원 공고","ntishome");
            assertThat(sample.profile().selectApprovedRequest(file.selectRequest())).isTrue();
        }
        assertThat(result.descriptors().getLast().downloadAllowed()).isFalse();
    }
    @Test void unknownLinksDoNotDiscardGoodFiles(){
        for(String bad:List.of("<a href=\"javascript:preview('x')\">미리보기</a>","<button>파일</button>",link(2,"hwp").replace("/20210118","/../private"))){
            var result=sample.profile().selectDescriptors(sample.source(),page(link(1,"hwp")+bad));
            assertThat(result.complete()).isFalse();assertThat(result.descriptors()).hasSize(1);
        }
        var limit=sample.profile().selectDescriptors(sample.source(),page(IntStream.rangeClosed(1,11).mapToObj(i->link(i,"hwp")).collect(Collectors.joining())));
        assertThat(limit.status()).isEqualTo("LIMIT_EXCEEDED");assertThat(limit.descriptors()).hasSize(10);
        assertThat(sample.profile().selectDescriptors(sample.source(),page(link(1,"hwp")+link(1,"hwp"))).descriptors()).hasSize(1);
    }
    @Test void missingAndChangedStructureIsNotNoFiles(){
        assertThat(sample.profile().selectDescriptors(sample.source(),page("")).status()).isEqualTo("NO_FILES");
        for(String bad:List.of(page("")+page(""),page("").replace("name=form","name=other"),page("").replace("width=98%","width=100%"),page("").replace("첨부파일 :","참고")))
            assertThat(sample.profile().selectDescriptors(sample.source(),bad).complete()).isFalse();
        assertThatThrownBy(()->CheonanNoticePage.selectContent(Jsoup.parse(page("").replace("word-break:break-all;","color:red;")))).hasMessage("CHEONAN_STRUCTURE_CHANGED");
    }
    @Test void sourceUrlAndFollowupRequestsCannotChange(){
        var profile=sample.profile();var source=sample.source();String url=source.sourceUrl();
        assertThat(profile.selectDetailUri(source)).isEqualTo(URI.create(url));
        for(String bad:List.of(url.replace("https:","http:"),url+"#x",url+"&subCheck=N",url.replace("eminwon.cheonan.go.kr","evil.example"),url.replace("context=NTIS","context=OTHER")))
            assertThatThrownBy(()->CheonanNoticePage.selectDetailUri(URI.create(bad))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->profile.selectDetailUri(new AttachmentDiscoveryProfile.Source(source.providerCode(),"0".repeat(64),url,source.localSourceCode(),source.listParserProfileCode()))).hasMessage("PROFILE_REQUIRED");
        var first=new Request(URI.create(url),"GET",Map.of());
        assertThat(profile.selectApprovedRequest(first,first)).isTrue();
        assertThat(profile.selectApprovedRequest(first,new Request(URI.create(url.replace("107953","107954")),"GET",Map.of()))).isFalse();
        assertThat(profile.selectApprovedRequest(first,new Request(first.uri(),"POST",Map.of("context","NTIS")))).isFalse();
        assertThatThrownBy(()->new Request(first.uri(),"POST",Map.of())).hasMessage("ATTACHMENT_REQUEST_INVALID");
    }
    @Test void actualTitleAndCatalogStayReferenceOnly() throws Exception {
        var doc=Jsoup.parse(page(link(1,"hwp")));
        AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(doc,sample.title(),sample.titleLayout());
        assertThatThrownBy(()->AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(doc,"다른 제목",sample.titleLayout())).isInstanceOf(AssertionError.class);
        assertThat(CheonanNoticePage.selectContent(doc).text()).isEqualTo("소상공인 자금 지원");
        var json=new ObjectMapper();var notices=json.readTree(Files.readString(Path.of("src/main/resources/announcement-attachment/provider-qa-catalog-v2.json"))).path("notices");
        var entry=StreamSupport.stream(notices.spliterator(),false).filter(n->sample.code().equals(n.path("caseCode").asText())).findFirst().orElseThrow();
        assertThat(entry.path("source")).isEqualTo(json.valueToTree(sample.source()));assertThat(entry.hasNonNull("expectation")).isFalse();
        var budget=AnnouncementAttachmentBbsOfficialObservationTest.selectBudget(sample.profile(),true,false);
        assertThat(budget.maximumRequests).isEqualTo(6);assertThat(budget.maximumBytes).isEqualTo(23L*1024*1024);
    }
}
