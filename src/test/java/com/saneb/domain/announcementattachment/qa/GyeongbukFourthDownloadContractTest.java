package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import com.saneb.domain.announcementattachment.discovery.AttachmentDiscoveryProfile;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.classification.*;
import java.net.URI;
import java.util.*;
import java.util.stream.*;
import org.jsoup.Jsoup;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class GyeongbukFourthDownloadContractTest {
    private String selectLink(int id,String ext){return "<a href='#' onclick=\"goDownload('공고"+id+"."+ext+"','저장"+id+"."+ext+"','/ntishome/file/upload/ofr/ofr/20260929'); return false;\"><span>공고"+id+"."+ext+"</span></a>";}
    private String selectPage(String group,String links){var s=GyeongbukFourthDownloadCases.selectCase(group);String title=group.equals("CHILGOK")?"<h4>"+s.title()+"</h4>":"<div class=subject>"+s.title()+"</div>";return "<form id=detailForm><div class=bod_view>"+title+"<dl class=view_file><dt>첨부 파일</dt><dd><div><ul>"+links+"</ul></div></dd></dl></div></form>";}
    @ParameterizedTest @ValueSource(strings={"CHEONGDO","CHILGOK","BONGHWA"})
    void keepsGoodFilesWhenUnsupportedOrUnknownItemsExist(String group){
        var s=GyeongbukFourthDownloadCases.selectCase(group);var p=s.profile();var r=p.selectDescriptors(s.source(),selectPage(group,selectLink(1,"hwp")+selectLink(2,"jpg")));
        assertThat(r.complete()).isTrue();assertThat(r.descriptors()).hasSize(2);assertThat(r.descriptors().getFirst().downloadAllowed()).isTrue();assertThat(r.descriptors().getLast().downloadAllowed()).isFalse();
        for(String bad:List.of("<a href='/unknown'>미확인</a>","<button>첨부</button>","<script>alert(1)</script>","<img src='/unknown'>")){var partial=p.selectDescriptors(s.source(),selectPage(group,bad+selectLink(1,"hwpx")));assertThat(partial.complete()).isFalse();assertThat(partial.descriptors()).hasSize(1);}
        r.descriptors().forEach(d->{assertThat(p.selectApprovedRequest(d.selectRequest())).isTrue();assertThat(d.documentRole()).isEqualTo("UNKNOWN");assertThat(d.toString()).doesNotContain("공고","http","저장");});
    }
    @ParameterizedTest @ValueSource(strings={"CHEONGDO","CHILGOK","BONGHWA"})
    void distinguishesMissingEmptyConflictingAndExcessFiles(String group){
        var s=GyeongbukFourthDownloadCases.selectCase(group);var p=s.profile();assertThat(p.selectDescriptors(s.source(),selectPage(group,"")).status()).isEqualTo("NO_FILES");assertThat(p.selectDescriptors(s.source(),"<html>오류</html>").complete()).isFalse();
        String link=selectLink(1,"pdf");assertThat(p.selectDescriptors(s.source(),selectPage(group,link+link)).descriptors()).hasSize(1);assertThat(p.selectDescriptors(s.source(),selectPage(group,link+link.replace("공고1","변경"))).complete()).isFalse();
        var limit=p.selectDescriptors(s.source(),selectPage(group,IntStream.rangeClosed(1,11).mapToObj(i->selectLink(i,"hwp")).collect(Collectors.joining())));assertThat(limit.status()).isEqualTo("LIMIT_EXCEEDED");assertThat(limit.descriptors()).hasSize(10);
    }
    @ParameterizedTest @ValueSource(strings={"CHEONGDO","CHILGOK","BONGHWA"})
    void sourceTitleAndDraftEligibilityRemainUnapproved(String group)throws Exception{
        var s=GyeongbukFourthDownloadCases.selectCase(group);var p=s.profile();assertThat(p.selectDetailUri(s.source()).toString()).isEqualTo(s.source().sourceUrl());
        assertThatThrownBy(()->p.selectDetailUri(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",s.source().providerNoticeId(),s.source().sourceUrl(),"LGS-000001",s.source().listParserProfileCode()))).hasMessage("PROFILE_REQUIRED");
        assertThatThrownBy(()->p.selectDetailUri(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE","invalid",s.source().sourceUrl(),s.source().localSourceCode(),s.source().listParserProfileCode()))).hasMessage("PROFILE_REQUIRED");
        var page=Jsoup.parse(selectPage(group,selectLink(1,"hwp")));AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,s.title(),s.titleLayout());assertThatThrownBy(()->AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,"다른 제목",s.titleLayout())).isInstanceOf(AssertionError.class);
        var d=new AnnouncementSourceClassificationEngine().selectDecision(new AnnouncementSourceClassificationInput("LOCAL_GOV_NOTICE",s.title(),null,null,List.of(),AnnouncementSourceClassificationCodes.BodySourceCode.NONE,AnnouncementSourceClassificationCodes.BodyAvailabilityCode.UNAVAILABLE),AnnouncementAttachmentRealFileQaTest.selectDraftRuleSet());assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectTitleMayProceed(d)).isTrue();
    }
    @ParameterizedTest @ValueSource(strings={"CHEONGDO","CHILGOK","BONGHWA"})
    void rejectsCrossHostQueryScriptAndRequestChanges(String group){
        var s=GyeongbukFourthDownloadCases.selectCase(group);var p=s.profile();String url=s.source().sourceUrl();
        for(String bad:List.of("https:/missing-host",url+"&extra=1",url+"&notAncmtMgtNo=2",url.replace("https:","http:"),url.replace("www.","evil."),url+"#fragment"))assertThat(p.selectApprovedRequest(URI.create(bad))).isFalse();
        var a=p.selectDescriptors(s.source(),selectPage(group,selectLink(1,"hwp"))).descriptors().getFirst();var b=p.selectDescriptors(s.source(),selectPage(group,selectLink(2,"hwp"))).descriptors().getFirst();assertThat(p.selectApprovedRequest(a.selectRequest(),a.selectRequest())).isTrue();assertThat(p.selectApprovedRequest(a.selectRequest(),b.selectRequest())).isFalse();assertThat(p.selectApprovedRequest(new Request(a.fetchUri(),"POST",Map.of("x","y")))).isFalse();
        assertThat(p.selectDescriptors(s.source(),selectPage(group,selectLink(1,"hwp").replace("return false;","alert(1);return false;"))).descriptors()).isEmpty();
    }
    @org.junit.jupiter.api.Test void bonghwaExternalPreviewRemainsUnresolvedWithoutDiscardingDownloads(){
        var s=GyeongbukFourthDownloadCases.selectCase("BONGHWA");String preview="<a href='#' onclick=\"fn_egov_gosi_preview_external('32956','공고1.hwp','저장1.hwp','/ntishome/file/upload/ofr/ofr/20260929'); return false;\">바로 보기</a>";
        var r=s.profile().selectDescriptors(s.source(),selectPage("BONGHWA",selectLink(1,"hwp")+preview));assertThat(r.complete()).isFalse();assertThat(r.warnings()).contains("ATTACHMENT_LINK_UNRESOLVED");assertThat(r.descriptors()).hasSize(1);assertThat(r.descriptors().getFirst().downloadAllowed()).isTrue();
    }
}
