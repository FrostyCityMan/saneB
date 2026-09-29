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

class JeonnamThirdDownloadContractTest {
    private final AnnouncementAttachmentBbsOfficialObservationTest.ObservationCase sample=JeonnamThirdDownloadCases.selectJangseongCase();
    private String selectLink(int id,String ext){return "<a href=\"javascript:goDownLoad('user"+"A".repeat(30)+id+"','system"+"B".repeat(30)+id+"','/ntisho"+"C".repeat(64)+"')\">공고"+id+"."+ext+"</a>";}
    private String selectPage(String links){return "<form name=nnn method=post action='https://eminwon.jangseong.go.kr/emwp/jsp/ofr/FileDownNew.jsp'><input type=hidden name=user_file_nm><input type=hidden name=sys_file_nm><input type=hidden name=file_path></form><div class=show_info><h3>"+sample.title()+"</h3><div class=file_down><table><tr><td>첨부파일</td><td>"+links+"</td></tr></table></div></div>";}
    @Test void collectsSupportedFilesAndMarksUnsupportedWithoutLosingGoodFiles(){
        var p=sample.profile();var r=p.selectDescriptors(sample.source(),selectPage(selectLink(1,"hwpx")+selectLink(2,"xlsx")));assertThat(r.complete()).isTrue();assertThat(r.descriptors()).hasSize(2);assertThat(r.descriptors().getFirst().downloadAllowed()).isTrue();assertThat(r.descriptors().getLast().downloadAllowed()).isFalse();
        var mixed=p.selectDescriptors(sample.source(),selectPage(selectLink(1,"hwp")+"<a href='/unknown'>미확인</a>"));assertThat(mixed.complete()).isFalse();assertThat(mixed.descriptors()).hasSize(1);
        r.descriptors().forEach(d->{assertThat(d.documentRole()).isEqualTo("UNKNOWN");assertThat(d.toString()).doesNotContain("공고","AAAA","http");assertThat(d.locator().toString()).doesNotContain("AAAA","BBBB");});
    }
    @Test void distinguishesMissingAreaEmptyAreaAndFileLimit(){
        var p=sample.profile();assertThat(p.selectDescriptors(sample.source(),selectPage("")).status()).isEqualTo("NO_FILES");assertThat(p.selectDescriptors(sample.source(),"<html>오류</html>").complete()).isFalse();assertThat(p.selectDescriptors(sample.source(),selectPage("<li></li>")).complete()).isFalse();
        var r=p.selectDescriptors(sample.source(),selectPage(IntStream.rangeClosed(1,11).mapToObj(i->selectLink(i,"hwp")).collect(Collectors.joining())));assertThat(r.status()).isEqualTo("LIMIT_EXCEEDED");assertThat(r.descriptors()).hasSize(10);
    }
    @Test void verifiesSourceTitleAndEligibilityWithoutApproval()throws Exception{
        var p=sample.profile();var s=sample.source();assertThat(p.selectDetailUri(s).toString()).isEqualTo(s.sourceUrl());
        assertThatThrownBy(()->p.selectDetailUri(new AttachmentDiscoveryProfile.Source(s.providerCode(),s.providerNoticeId(),s.sourceUrl(),"LGS-000001",s.listParserProfileCode()))).isInstanceOf(IllegalArgumentException.class);
        var page=Jsoup.parse(selectPage(selectLink(1,"hwpx")));AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,sample.title(),sample.titleLayout());assertThatThrownBy(()->AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,"다른 제목",sample.titleLayout())).isInstanceOf(AssertionError.class);
        var d=new AnnouncementSourceClassificationEngine().selectDecision(new AnnouncementSourceClassificationInput("LOCAL_GOV_NOTICE",sample.title(),null,null,List.of(),AnnouncementSourceClassificationCodes.BodySourceCode.NONE,AnnouncementSourceClassificationCodes.BodyAvailabilityCode.UNAVAILABLE),AnnouncementAttachmentRealFileQaTest.selectDraftRuleSet());assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectTitleMayProceed(d)).isTrue();
    }
    @Test void forbidsHostPathQueryAndFormExtensions(){
        var p=sample.profile();var detail=p.selectDetailUri(sample.source());
        for(String u:List.of("https:/missing-host",detail+"&page=2",detail+"&unknown=1",detail.toString().replace("https:","http:"),detail.toString().replace("www.","evil."),detail.toString().replace("/show/29332","/show/%32%39%33%33%32")))assertThat(p.selectApprovedRequest(URI.create(u))).isFalse();
        String html=selectPage(selectLink(1,"hwp"));var f=p.selectDescriptors(sample.source(),html).descriptors().getFirst();assertThat(p.selectApprovedRequest(f.selectRequest())).isTrue();assertThat(p.selectApprovedRequest(f.fetchUri())).isFalse();
        var altered=new HashMap<>(f.postForm());altered.put("sys_file_nm","other"+"D".repeat(30));assertThat(p.selectApprovedRequest(f.selectRequest(),new Request(f.fetchUri(),"POST",altered))).isFalse();
        for(String bad:List.of(html.replace("name=file_path","name=extra"),html.replace("type=hidden name=file_path","type=text name=file_path"),html.replace("<input type=hidden name=file_path>","<input type=hidden name=file_path value=unexpected>"),html.replace("action='https://eminwon.","action='https://evil.")))assertThat(p.selectDescriptors(sample.source(),bad).complete()).isFalse();
    }
    @Test void ignoresOutsideLinksButNotUnknownControlsInsideArea(){
        var p=sample.profile();String html=selectPage(selectLink(1,"hwp"));assertThat(p.selectDescriptors(sample.source(),html+"<a href='/print.pdf'>인쇄</a>").complete()).isTrue();
        for(String extra:List.of("<script>alert(1)</script>","<button>파일</button>","<img src='/unknown'>","<a href='/viewer'>뷰어</a>")){var r=p.selectDescriptors(sample.source(),selectPage(selectLink(1,"hwp")+extra));assertThat(r.complete()).isFalse();assertThat(r.descriptors()).hasSize(1);}
        assertThat(p.selectDescriptors(sample.source(),html.replace(")\">",");alert(1)\">" )).descriptors()).isEmpty();
    }
    @Test void deduplicatesOnlyIdenticalFileNamesAndForms(){
        var p=sample.profile();String link=selectLink(1,"hwp");assertThat(p.selectDescriptors(sample.source(),selectPage(link+link)).descriptors()).hasSize(1);assertThat(p.selectDescriptors(sample.source(),selectPage(link+link)).complete()).isTrue();
        var r=p.selectDescriptors(sample.source(),selectPage(link+link.replace("공고1.hwp","다른이름.hwp")));assertThat(r.descriptors()).hasSize(1);assertThat(r.complete()).isFalse();
    }
    @Test void oversizedPublicFormDoesNotDiscardOtherFiles(){
        String bad="<a href=\"javascript:goDownLoad('"+"가".repeat(1000)+"A".repeat(30)+"','"+"나".repeat(1000)+"B".repeat(30)+"','/ntisho"+"C".repeat(64)+"')\">크기초과.hwp</a>";
        var r=sample.profile().selectDescriptors(sample.source(),selectPage(bad+selectLink(1,"hwp")));assertThat(r.complete()).isFalse();assertThat(r.descriptors()).hasSize(1);assertThat(r.descriptors().getFirst().downloadAllowed()).isTrue();
    }
}
