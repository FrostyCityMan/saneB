package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import java.net.URI;
import java.util.*;
import java.util.stream.*;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.provider.content.SeongnamNoticePage;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.classification.*;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.*;

class SeongnamDownloadContractTest {
    private final AnnouncementAttachmentBbsOfficialObservationTest.ObservationCase sample=SeongnamDownloadCases.selectCase();
    private String selectFile(int id,String ext){return "<a href=\"javascript:goDownLoad('공고"+id+"."+ext+"','system"+id+"."+ext+"','/ntishome/file/upload/ofr/ofr/20260109')\">공고"+id+"."+ext+"</a>";}
    private String selectPage(String items){return "<nav>메뉴</nav><form name=form1 method=post><div class=boardWrap><table class='bd00view mb20'><tr><th scope=row>제목</th><td class='bd01td listx'>"+sample.title()+"</td><th>담당부서</th><td>수출 부서</td></tr><tr><th scope=row>첨부파일</th><td colspan=3 class=bd01td>"+items+"</td></tr><tr><td colspan=4 scope=row class=bd01tdC>소상공인 특례보증 지원</td></tr></table></div></form>";}
    @Test void reusesExistingEngineWithExactSourceAndRequestBoundary(){
        var p=sample.profile();var u=p.selectDetailUri(sample.source());assertThat(p.selectApprovedRequest(u)).isTrue();
        for(String bad:List.of(u+"&extra=1",u+"&subCheck=Y",u+"#fragment",u.toString().replace("https:","http:"),u.toString().replace("eminwon.seongnam","evil.seongnam")))assertThat(p.selectApprovedRequest(URI.create(bad))).isFalse();
        var s=sample.source();assertThatThrownBy(()->p.selectDetailUri(new AttachmentDiscoveryProfile.Source(s.providerCode(),s.providerNoticeId(),s.sourceUrl(),"LGS-000023",s.listParserProfileCode()))).isInstanceOf(IllegalArgumentException.class);
        var file=p.selectDescriptors(s,selectPage(selectFile(1,"hwp"))).descriptors().getFirst();assertThat(file.downloadAllowed()).isTrue();assertThat(file.expectedFormat()).isEqualTo("HWP");assertThat(p.selectApprovedRequest(file.fetchUri())).isTrue();
        assertThat(p.selectApprovedRequest(new Request(file.fetchUri(),"POST",Map.of("x","y")))).isFalse();
    }
    @Test void preservesGoodFilesBesideUnresolvedLinksAndUnsupportedMetadata(){
        var p=sample.profile();var r=p.selectDescriptors(sample.source(),selectPage(selectFile(1,"hwp")+selectFile(2,"xlsx")+"<a href='/unknown'>오류 링크</a>"));
        assertThat(r.complete()).isFalse();assertThat(r.descriptors()).hasSize(2).extracting(d->d.downloadAllowed()).containsExactly(true,false);
        assertThat(p.selectDescriptors(sample.source(),selectPage("")).status()).isEqualTo("NO_FILES");
        assertThat(p.selectDescriptors(sample.source(),selectPage(selectFile(1,"pdf"))+"<a href='/outside.pdf'>바깥 링크</a>").descriptors()).hasSize(1);
    }
    @Test void limitsAndDeduplicatesWithoutDroppingGoodFiles(){
        var p=sample.profile();String file=selectFile(1,"pdf");assertThat(p.selectDescriptors(sample.source(),selectPage(file+file)).descriptors()).hasSize(1);
        String many=IntStream.rangeClosed(1,11).mapToObj(i->selectFile(i,"hwpx")).collect(Collectors.joining());var r=p.selectDescriptors(sample.source(),selectPage(many));assertThat(r.status()).isEqualTo("LIMIT_EXCEEDED");assertThat(r.descriptors()).hasSize(10);
    }
    @Test void bodyAndTitleRequireOneOfficialTable(){
        String page=selectPage(selectFile(1,"hwp"));assertThat(SeongnamNoticePage.selectContent(Jsoup.parse(page)).text()).isEqualTo("소상공인 특례보증 지원");
        AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(Jsoup.parse(page),sample.title(),sample.titleLayout());
        assertThatThrownBy(()->AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(Jsoup.parse(page),"다른 제목",sample.titleLayout())).isInstanceOf(AssertionError.class);
        for(String bad:List.of(page+page,page.replace("bd01tdC","other"),page.replace("<th scope=row>제목</th>","<th scope=row>다른 라벨</th>")))assertThatThrownBy(()->SeongnamNoticePage.selectContent(Jsoup.parse(bad))).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void rejectsTraversalAndExecutableSuffixes(){
        String file=selectFile(1,"hwp");for(String bad:List.of(file.replace("system1.hwp","../system1.hwp"),file.replace("20260109')","20260109');alert(1)")))assertThat(sample.profile().selectDescriptors(sample.source(),selectPage(bad)).descriptors()).isEmpty();
        assertThat(sample.profile().selectDescriptors(sample.source(),selectPage(file).replace("첨부파일","다른 영역")).complete()).isFalse();
    }
    @Test void preservesApprovedTitlePolicy()throws Exception{
        var d=new AnnouncementSourceClassificationEngine().selectDecision(new AnnouncementSourceClassificationInput("LOCAL_GOV_NOTICE",sample.title(),null,null,List.of(),BodySourceCode.NONE,BodyAvailabilityCode.UNAVAILABLE),AnnouncementAttachmentRealFileQaTest.selectDraftRuleSet());
        assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectTitleMayProceed(d)).isTrue();
    }
}
