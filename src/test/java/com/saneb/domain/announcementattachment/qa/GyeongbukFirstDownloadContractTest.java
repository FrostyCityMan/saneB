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

class GyeongbukFirstDownloadContractTest {
    private String selectLink(String group,int id,String ext){return "<a href='#' onclick=\""+(group.equals("GIMCHEON")?"goDownloadPost":"goDownload")+"('공고"+id+"."+ext+"','저장"+id+"."+ext+"','/ntishome/file/upload/ofr/ofr/20260929'); return false;\">공고"+id+"."+ext+"</a>";}
    private String selectPage(String group,String links){
        var s=GyeongbukFirstDownloadCases.selectCase(group);String heading=group.equals("GUMI")?"<div class=subject>"+s.title()+"</div>":"<h4>"+s.title()+"</h4>";
        return "<form id=detailForm><div class=bod_view>"+heading+"<dl class=view_file><dt><span>첨부 파일</span></dt><dd><div id=updateFileList><ul>"+links+"</ul></div></dd></dl></div></form>"+(group.equals("GIMCHEON")?"<form id=fileDownFrm method=post action='https://eminwon.gc.go.kr/emwp/jsp/ofr/FileDown.jsp'><input type=hidden name=user_file_nm><input type=hidden name=sys_file_nm><input type=hidden name=file_path></form>":"");
    }
    @ParameterizedTest @ValueSource(strings={"GIMCHEON","GUMI","YEONGCHEON","MUNGYEONG"})
    void preservesGoodFilesAndDistinguishesUnsupportedAndUnknown(String group){
        var s=GyeongbukFirstDownloadCases.selectCase(group);var p=s.profile();
        var result=p.selectDescriptors(s.source(),selectPage(group,selectLink(group,1,"pdf")+selectLink(group,2,"xlsx")));
        assertThat(result.complete()).isTrue();assertThat(result.descriptors()).hasSize(2);assertThat(result.descriptors().getFirst().downloadAllowed()).isTrue();assertThat(result.descriptors().getLast().downloadAllowed()).isFalse();
        for(String extra:List.of("<a href='/unknown'>미확인</a>","<button>다운로드</button>","<script>alert(1)</script>","<img src='/unknown'>")){
            var partial=p.selectDescriptors(s.source(),selectPage(group,extra+selectLink(group,1,"hwp")));assertThat(partial.complete()).isFalse();assertThat(partial.descriptors()).hasSize(1);
        }
        result.descriptors().forEach(d->{assertThat(d.documentRole()).isEqualTo("UNKNOWN");assertThat(d.toString()).doesNotContain("공고","http");assertThat(d.locator().toString()).doesNotContain("저장");assertThat(p.selectApprovedRequest(d.selectRequest())).isTrue();});
    }
    @ParameterizedTest @ValueSource(strings={"GIMCHEON","GUMI","YEONGCHEON","MUNGYEONG"})
    void distinguishesNoFilesMissingAreaDuplicateAndLimit(String group){
        var s=GyeongbukFirstDownloadCases.selectCase(group);var p=s.profile();
        assertThat(p.selectDescriptors(s.source(),selectPage(group,"")).status()).isEqualTo("NO_FILES");assertThat(p.selectDescriptors(s.source(),"<html>오류</html>").complete()).isFalse();assertThat(p.selectDescriptors(s.source(),selectPage(group,"<li></li>")).complete()).isFalse();
        String link=selectLink(group,1,"hwpx");assertThat(p.selectDescriptors(s.source(),selectPage(group,link+link)).descriptors()).hasSize(1);
        var conflict=p.selectDescriptors(s.source(),selectPage(group,link+link.replace("공고1.hwpx","변경.hwpx")));assertThat(conflict.complete()).isFalse();assertThat(conflict.descriptors()).hasSize(1);
        var limit=p.selectDescriptors(s.source(),selectPage(group,IntStream.rangeClosed(1,11).mapToObj(i->selectLink(group,i,"hwp")).collect(Collectors.joining())));assertThat(limit.status()).isEqualTo("LIMIT_EXCEEDED");assertThat(limit.descriptors()).hasSize(10);
    }
    @ParameterizedTest @ValueSource(strings={"GIMCHEON","GUMI","YEONGCHEON","MUNGYEONG"})
    void sourceBoundaryTitleAndDraftEligibilityAreVerified(String group)throws Exception{
        var s=GyeongbukFirstDownloadCases.selectCase(group);var p=s.profile();assertThat(p.selectDetailUri(s.source()).toString()).isEqualTo(s.source().sourceUrl());
        assertThatThrownBy(()->p.selectDetailUri(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",s.source().providerNoticeId(),s.source().sourceUrl(),"LGS-000001",s.source().listParserProfileCode()))).hasMessage("PROFILE_REQUIRED");
        assertThatThrownBy(()->p.selectDetailUri(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE","invalid",s.source().sourceUrl(),s.source().localSourceCode(),s.source().listParserProfileCode()))).hasMessage("PROFILE_REQUIRED");
        var page=Jsoup.parse(selectPage(group,selectLink(group,1,"hwp")));AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,s.title(),s.titleLayout());assertThatThrownBy(()->AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,"다른 제목",s.titleLayout())).isInstanceOf(AssertionError.class);
        var d=new AnnouncementSourceClassificationEngine().selectDecision(new AnnouncementSourceClassificationInput("LOCAL_GOV_NOTICE",s.title(),null,null,List.of(),AnnouncementSourceClassificationCodes.BodySourceCode.NONE,AnnouncementSourceClassificationCodes.BodyAvailabilityCode.UNAVAILABLE),AnnouncementAttachmentRealFileQaTest.selectDraftRuleSet());assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectTitleMayProceed(d)).isTrue();
    }
    @ParameterizedTest @ValueSource(strings={"GIMCHEON","GUMI","YEONGCHEON","MUNGYEONG"})
    void forbidsRequestExtensionsScriptInjectionAndCrossFileRedirect(String group){
        var s=GyeongbukFirstDownloadCases.selectCase(group);var p=s.profile();String url=s.source().sourceUrl();
        for(String bad:List.of("https:/missing-host",url+"&notAncmtMgtNo=2",url+"&extra=1",url.replace("https:","http:"),url.replace("www.","evil."),url.replace("/view.do","/%76iew.do"),url+"#fragment"))assertThat(p.selectApprovedRequest(URI.create(bad))).isFalse();
        String html=selectPage(group,selectLink(group,1,"hwp"));assertThat(p.selectDescriptors(s.source(),html.replace("return false;","alert(1); return false;")).descriptors()).isEmpty();
        var a=p.selectDescriptors(s.source(),html).descriptors().getFirst();var b=p.selectDescriptors(s.source(),selectPage(group,selectLink(group,2,"hwp"))).descriptors().getFirst();assertThat(p.selectApprovedRequest(a.selectRequest(),a.selectRequest())).isTrue();assertThat(p.selectApprovedRequest(a.selectRequest(),b.selectRequest())).isFalse();
        if(group.equals("GIMCHEON")){
            assertThat(p.selectApprovedRequest(a.fetchUri())).isFalse();assertThat(p.selectDescriptors(s.source(),html.replace("name=file_path","name=extra")).complete()).isFalse();
            var form=new HashMap<>(a.postForm());form.put("extra","x");assertThat(p.selectApprovedRequest(new Request(a.fetchUri(),"POST",form))).isFalse();
        }
    }
    @ParameterizedTest @ValueSource(strings={"GUMI","YEONGCHEON","MUNGYEONG"})
    void skipsOnlyPreviewPairedWithTheSameOfficialDownload(String group){
        var s=GyeongbukFirstDownloadCases.selectCase(group);String id=s.code().split("-")[1];
        String preview="<a href='#' onclick=\"fn_egov_gosi_preview('"+id+"','"+id+"-0.hwp','공고1.hwp','저장1.hwp','/ntishome/file/upload/ofr/ofr/20260929'); return false;\">바로 보기</a>";
        var p=s.profile();var good=p.selectDescriptors(s.source(),selectPage(group,"<li>"+selectLink(group,1,"hwp")+preview+"</li>"));assertThat(good.complete()).isTrue();assertThat(good.descriptors()).hasSize(1);
        for(String bad:List.of(preview.replace("저장1","다른저장"),preview.replace(id,"999999"),preview.replace("return false;","alert(1);"))){var result=p.selectDescriptors(s.source(),selectPage(group,"<li>"+selectLink(group,1,"hwp")+bad+"</li>"));assertThat(result.complete()).isFalse();assertThat(result.descriptors()).hasSize(1);}
    }
}
