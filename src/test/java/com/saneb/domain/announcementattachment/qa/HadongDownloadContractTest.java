package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import java.net.URI;
import java.util.*;
import java.util.stream.*;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import com.saneb.domain.announcementattachment.discovery.AttachmentDiscoveryProfile;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.classification.*;

class HadongDownloadContractTest {
    private final AnnouncementAttachmentBbsOfficialObservationTest.ObservationCase sample=HadongDownloadCases.selectCase();
    private String selectItem(int id,String ext){return "<li><a class=filename href='https://eminwon.hadong.go.kr/emwp/jsp/ofr/FileDown.jsp?user_file_nm=공고%20"+id+"."+ext+"&sys_file_nm=공고_"+id+"."+ext+"&file_path=/ntishome/file/upload/ofr/ofr/20260921'>공고 "+id+"."+ext+"</a></li>";}
    private String selectPage(String items){return "<form id=saeolGosiVO><div class=bbs1view1><h1 class=h1>"+sample.title()+"</h1><div class=info1>부서 정보</div><div class=attach1><ul>"+items+"</ul></div><div class=substance>소상공인 지원</div></div></form>";}
    @Test void collectionBudgetStaysBounded(){var b=AnnouncementAttachmentBbsOfficialObservationTest.selectBudget(sample.profile(),true,false);assertThat(b.maximumRequests).isEqualTo(6);assertThat(b.maximumBytes).isEqualTo(23*1024*1024);}
    @Test void readsOnlyOfficialDirectFiles(){
        var r=sample.profile().selectDescriptors(sample.source(),selectPage(selectItem(1,"hwpx"))+"<a href='/other.pdf'>다른 링크</a>");
        assertThat(r.status()).isEqualTo("FOUND");assertThat(r.complete()).isTrue();assertThat(r.descriptors()).singleElement().satisfies(d->{assertThat(d.displayName()).isEqualTo("공고 1.hwpx");assertThat(d.downloadAllowed()).isTrue();assertThat(d.documentRole()).isEqualTo("UNKNOWN");assertThat(d.postForm()).isEmpty();assertThat(sample.profile().selectApprovedRequest(d.fetchUri())).isTrue();assertThat(d.toString()).doesNotContain("공고","https");});
    }
    @Test void goodFilesSurviveMalformedLinksAndUnsupportedFormats(){
        var r=sample.profile().selectDescriptors(sample.source(),selectPage(selectItem(1,"hwpx")+selectItem(2,"xlsx")+"<li><a href='/unknown'>알 수 없음</a></li>"));
        assertThat(r.complete()).isFalse();assertThat(r.descriptors()).hasSize(2);assertThat(r.descriptors().getFirst().downloadAllowed()).isTrue();assertThat(r.descriptors().getLast().downloadAllowed()).isFalse();
    }
    @Test void distinguishesEmptyChangedAndLimitedAreas(){
        var p=sample.profile();assertThat(p.selectDescriptors(sample.source(),selectPage("")).status()).isEqualTo("NO_FILES");
        for(String bad:List.of("<html>오류</html>",selectPage("<li></li>"),selectPage("")+selectPage(""),selectPage("").replace("class=attach1","class=other")))assertThat(p.selectDescriptors(sample.source(),bad).complete()).isFalse();
        var r=p.selectDescriptors(sample.source(),selectPage(IntStream.rangeClosed(1,11).mapToObj(i->selectItem(i,"pdf")).collect(Collectors.joining())));assertThat(r.status()).isEqualTo("LIMIT_EXCEEDED");assertThat(r.descriptors()).hasSize(10);
    }
    @Test void requiresSourceIdentityTitleAndTitlePolicy()throws Exception{
        var p=sample.profile();var s=sample.source();assertThat(p.selectDetailUri(s).toString()).isEqualTo(s.sourceUrl());
        assertThatThrownBy(()->p.selectDetailUri(new AttachmentDiscoveryProfile.Source(s.providerCode(),s.providerNoticeId(),s.sourceUrl(),"LGS-000001",s.listParserProfileCode()))).isInstanceOf(IllegalArgumentException.class);
        var page=Jsoup.parse(selectPage(selectItem(1,"hwpx")));AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,sample.title(),sample.titleLayout());assertThatThrownBy(()->AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,"다른 제목",sample.titleLayout())).isInstanceOf(AssertionError.class);
        var d=new AnnouncementSourceClassificationEngine().selectDecision(new AnnouncementSourceClassificationInput("LOCAL_GOV_NOTICE",sample.title(),null,null,List.of(),AnnouncementSourceClassificationCodes.BodySourceCode.NONE,AnnouncementSourceClassificationCodes.BodyAvailabilityCode.UNAVAILABLE),AnnouncementAttachmentRealFileQaTest.selectDraftRuleSet());assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectTitleMayProceed(d)).isTrue();
    }
    @Test void rejectsHostQueryPostAndRedirectExpansion(){
        var p=sample.profile();var detail=p.selectDetailUri(sample.source());var file=p.selectDescriptors(sample.source(),selectPage(selectItem(1,"hwpx"))).descriptors().getFirst().fetchUri();
        for(String bad:List.of(detail+"&not_ancmt_mgt_no=2",detail+"&extra=x",detail.toString().replace("https:","http:"),file+"&extra=x",file.toString().replace("eminwon.hadong.go.kr","evil.go.kr"),file.toString().replace("/20260921","/../20260921")))assertThat(p.selectApprovedRequest(URI.create(bad))).isFalse();
        assertThat(p.selectApprovedRequest(new Request(file,"POST",Map.of("x","y")))).isFalse();assertThat(p.selectApprovedRequest(Request.selectGet(detail),Request.selectGet(file))).isFalse();
    }
    @Test void duplicatesDeduplicateButUnknownControlsRemainErrors(){
        String item=selectItem(1,"hwpx");var p=sample.profile();assertThat(p.selectDescriptors(sample.source(),selectPage(item+item)).descriptors()).hasSize(1);
        for(String extra:List.of("<script>x</script>","<button>파일</button>",item.replace("class=filename","onclick='x()'"),item.replace(">공고 1.hwpx",">다른 파일.hwpx"))){var r=p.selectDescriptors(sample.source(),selectPage(item+extra));assertThat(r.complete()).isFalse();assertThat(r.descriptors()).hasSize(1);}
    }
}
