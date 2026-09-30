package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.provider.content.GwangjuSeoguNoticePage;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.classification.*;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.*;

class GwangjuSeoguDownloadContractTest {
    private final AnnouncementAttachmentBbsOfficialObservationTest.ObservationCase sample=GwangjuSeoguDownloadCases.selectCase();
    private String selectItem(int id,String ext){String name="공고"+id+"."+ext;return "<li><img src='/upload/skin/board/basic/"+ext+".png'>"+name+"<span class=link><a class=btn_line href=\"javascript:goDownLoad('"+name+"','system"+id+"."+ext+"','/ntishome/file/upload/ofr/ofr/20260930')\">다운로드<i class=xi-download></i></a></span></li>";}
    private String selectPage(String items){return "<nav>메뉴 정보</nav><form name=form1 method=post><article class=board_view><h2 class=title>"+sample.title()+"</h2><ul class=info><li>담당자 정보</li></ul><div class=contents>소상공인 카드 수수료 지원 내용</div><div class=file><strong class=title>첨부파일</strong><ul class=list>"+items+"</ul></div></article></form>";}
    @Test @EnabledIfEnvironmentVariable(named="SANEB_GWANGJU_SEOGU_SURVEY_FIXTURE",matches="true")
    void officialDetailsVerifySupportedAndUnsupportedFilesWithoutDownloading()throws Exception{
        for(var entry:Map.of("detail53423.html","HWP","detail56115.html","HWPX","detail56101.html","UNSUPPORTED").entrySet()){
            String html=Files.readString(Path.of("build/qa-gwangju-seogu-20260930/"+entry.getKey()));
            var result=sample.profile().selectDescriptors(sample.source(),html);
            assertThat(result.complete()).isTrue();assertThat(result.descriptors()).hasSize(1);
            var file=result.descriptors().getFirst();assertThat(file.downloadAllowed()).isEqualTo(!entry.getValue().equals("UNSUPPORTED"));
            if(file.downloadAllowed())assertThat(file.expectedFormat()).isEqualTo(entry.getValue());
            if(entry.getKey().equals("detail53423.html")){
                AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(Jsoup.parse(html),sample.title(),sample.titleLayout());
                assertThat(GwangjuSeoguNoticePage.selectContent(Jsoup.parse(html)).text()).contains("지원대상");
            }
        }
    }
    @Test void extractsOnlyBodyAndKeepsGoodFilesBesideErrors(){
        String page=selectPage(selectItem(1,"hwp")+"<li><a href='/unknown'>알 수 없는 파일</a></li>");
        assertThat(GwangjuSeoguNoticePage.selectContent(Jsoup.parse(page)).text()).isEqualTo("소상공인 카드 수수료 지원 내용");
        var r=sample.profile().selectDescriptors(sample.source(),page);assertThat(r.complete()).isFalse();assertThat(r.descriptors()).hasSize(1);
    }
    @Test void distinguishesUnsupportedFilesFromConfirmedEmpty(){
        var p=sample.profile();var mixed=p.selectDescriptors(sample.source(),selectPage(selectItem(1,"hwp")+selectItem(2,"xlsx")));
        assertThat(mixed.complete()).isTrue();assertThat(mixed.descriptors()).extracting(d->d.downloadAllowed()).containsExactly(true,false);
        assertThat(p.selectDescriptors(sample.source(),selectPage("")).status()).isEqualTo("NO_FILES");
        assertThat(p.selectDescriptors(sample.source(),selectPage("").replace("첨부파일","다른 영역")).complete()).isFalse();
    }
    @Test void rejectsMissingDuplicateAndNestedTitleStructures(){
        String page=selectPage(selectItem(1,"hwp"));
        for(String html:List.of(page+page,page.replace("h2 class=title","h2 class=other"),page.replace("<h2 class=title>","<div><h2 class=title>").replace("</h2>","</h2></div>")))
            assertThat(sample.profile().selectDescriptors(sample.source(),html).complete()).isFalse();
        assertThat(sample.profile().selectDescriptors(sample.source(),page+"<a href='/outside.pdf'>파일</a>").descriptors()).hasSize(1);
    }
    @Test void preservesExistingTitlePolicy()throws Exception{
        var decision=new AnnouncementSourceClassificationEngine().selectDecision(new AnnouncementSourceClassificationInput("LOCAL_GOV_NOTICE",sample.title(),null,null,List.of(),BodySourceCode.NONE,BodyAvailabilityCode.UNAVAILABLE),AnnouncementAttachmentRealFileQaTest.selectDraftRuleSet());
        assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectTitleMayProceed(decision)).isTrue();
        AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(Jsoup.parse(selectPage("")),sample.title(),sample.titleLayout());
        assertThatThrownBy(()->AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(Jsoup.parse(selectPage("")),"다른 공고",sample.titleLayout())).isInstanceOf(AssertionError.class);
    }
    @Test void bindsSourceAndRequestToExactOfficialHostPathAndParameters(){
        var p=sample.profile();var s=sample.source();var uri=p.selectDetailUri(s);assertThat(p.selectApprovedRequest(uri)).isTrue();
        for(String u:List.of(uri+"&extra=1",uri+"&mid=a10807010000",uri.toString().replace("https:","http:"),uri.toString().replace("www.seogu","evil.seogu"),uri.toString().replace("gosiXmlView.es","gosiXmlList.es"),uri+"#fragment"))assertThat(p.selectApprovedRequest(URI.create(u))).isFalse();
        assertThatThrownBy(()->p.selectDetailUri(new AttachmentDiscoveryProfile.Source(s.providerCode(),s.providerNoticeId(),s.sourceUrl(),"LGS-000074",s.listParserProfileCode()))).isInstanceOf(IllegalArgumentException.class);
        var d=p.selectDescriptors(s,selectPage(selectItem(1,"hwp"))).descriptors().getFirst();assertThat(p.selectApprovedRequest(d.selectRequest())).isTrue();
        assertThat(p.selectApprovedRequest(new Request(d.fetchUri(),"POST",Map.of("x","y")))).isFalse();
        var other=p.selectDescriptors(s,selectPage(selectItem(2,"hwp"))).descriptors().getFirst();assertThat(p.selectApprovedRequest(d.selectRequest(),other.selectRequest())).isFalse();
    }
    @Test void limitsFilesWithoutDiscardingPreviouslyDiscoveredOnes(){
        String items=IntStream.rangeClosed(1,11).mapToObj(i->selectItem(i,"hwpx")).collect(Collectors.joining());
        var r=sample.profile().selectDescriptors(sample.source(),selectPage(items));assertThat(r.status()).isEqualTo("LIMIT_EXCEEDED");assertThat(r.descriptors()).hasSize(10);
    }
    @Test void rejectsPathTraversalScriptSuffixAndNameMismatch(){
        String item=selectItem(1,"hwp");
        for(String bad:List.of(item.replace("system1.hwp","../system1.hwp"),item.replace("20260930')","20260930');alert(1)"),item.replace(">공고1.hwp<",">다른이름.hwp<")))assertThat(sample.profile().selectDescriptors(sample.source(),selectPage(bad)).descriptors()).isEmpty();
    }
    @Test void deduplicatesOnlyIdenticalFileLocators(){
        var p=sample.profile();String item=selectItem(1,"hwp");assertThat(p.selectDescriptors(sample.source(),selectPage(item+item)).descriptors()).hasSize(1);
        var conflict=p.selectDescriptors(sample.source(),selectPage(item+item.replace("공고1.hwp","다른.hwp")));assertThat(conflict.complete()).isFalse();assertThat(conflict.descriptors()).hasSize(1);
    }
}
