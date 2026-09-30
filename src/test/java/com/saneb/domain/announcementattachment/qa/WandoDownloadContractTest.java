package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import java.net.URI;
import java.util.*;
import java.util.stream.*;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import com.saneb.domain.announcementattachment.discovery.AttachmentDiscoveryProfile;
import com.saneb.domain.announcementsource.provider.content.WandoNoticePage;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.classification.*;

class WandoDownloadContractTest {
    private final AnnouncementAttachmentBbsOfficialObservationTest.ObservationCase sample=WandoDownloadCases.selectCase();
    private String selectFile(int id,String ext){return "<li><span><img alt='첨부파일' src='/images/common/ext_img/"+ext+".gif'></span><a href=\"javascript:goDownLoad('user"+"A".repeat(30)+id+"','system"+"B".repeat(30)+id+"','/ntisho"+"C".repeat(64)+"')\">공고"+id+"."+ext+"</a><br></li>";}
    private String selectPage(String files,int count){return "<nav>수출 메뉴</nav><div id=board_basic_view><div class=news_tit><h3><td>"+sample.title()+"</td></h3></div><div class=set-box>특허 부서</div><div class=file_attach><h5>첨부파일<span>(<strong>"+count+"</strong>)</span></h5><div class=attach_thum><ul>"+files+"</ul></div></div><div class=board_cont><p><td>소상공인 디지털 전환 지원</td></p></div></div><form name=nnn method=post action='https://eminwon.wando.go.kr/emwp/jsp/ofr/FileDownNew.jsp'><input type=hidden name=user_file_nm><input type=hidden name=sys_file_nm><input type=hidden name=file_path></form>";}
    @Test void readsOnlyOfficialFileAreaAndOpaquePostForm(){
        var p=sample.profile();var r=p.selectDescriptors(sample.source(),selectPage(selectFile(1,"hwp"),1));
        assertThat(r.status()).isEqualTo("FOUND");assertThat(r.complete()).isTrue();
        var d=r.descriptors().getFirst();assertThat(d.expectedFormat()).isEqualTo("HWP");assertThat(d.documentRole()).isEqualTo("UNKNOWN");
        assertThat(p.selectApprovedRequest(d.selectRequest())).isTrue();assertThat(p.selectApprovedRequest(d.fetchUri())).isFalse();
        assertThat(d.postForm().keySet()).containsExactlyInAnyOrder("user_file_nm","sys_file_nm","file_path");
        assertThat(d.toString()).doesNotContain("공고","AAAA","http");assertThat(d.locator().toString()).doesNotContain("AAAA","BBBB");
    }
    @Test void preservesGoodFilesAlongsideUnsupportedAndUnresolved(){
        var r=sample.profile().selectDescriptors(sample.source(),selectPage(selectFile(1,"hwpx")+selectFile(2,"xlsx"),2));
        assertThat(r.complete()).isTrue();assertThat(r.descriptors()).extracting(AttachmentDiscoveryProfile.Descriptor::downloadAllowed).containsExactly(true,false);
        for(String extra:List.of("<li><a href='/unknown'>미확인</a></li>","<script>alert(1)</script>","<button>파일</button>","<img src='/unknown'>")){
            var mixed=sample.profile().selectDescriptors(sample.source(),selectPage(selectFile(1,"hwp")+extra,2));
            assertThat(mixed.complete()).isFalse();assertThat(mixed.descriptors()).hasSize(1);
        }
    }
    @Test void distinguishesAbsentAreaEmptyAreaAndCountMismatch(){
        var p=sample.profile();assertThat(p.selectDescriptors(sample.source(),selectPage("",0)).status()).isEqualTo("NO_FILES");
        assertThat(p.selectDescriptors(sample.source(),selectPage("",1)).complete()).isFalse();
        String one=selectPage(selectFile(1,"hwp"),1);
        for(String bad:List.of(one+one,one.replace("file_attach","changed"),one.replace("첨부파일<span>","다른영역<span>")))assertThat(p.selectDescriptors(sample.source(),bad).complete()).isFalse();
        assertThat(p.selectDescriptors(sample.source(),one+"<a href='/outside.pdf'>인쇄</a>").complete()).isTrue();
    }
    @Test void deduplicatesAndBoundsFileSet(){
        String one=selectFile(1,"hwp");var p=sample.profile();var dedup=p.selectDescriptors(sample.source(),selectPage(one+one,1));
        assertThat(dedup.complete()).isTrue();assertThat(dedup.descriptors()).hasSize(1);
        assertThat(p.selectDescriptors(sample.source(),selectPage(one+one.replace("공고1.hwp","다른이름.hwp"),1)).complete()).isFalse();
        var result=p.selectDescriptors(sample.source(),selectPage(IntStream.rangeClosed(1,11).mapToObj(i->selectFile(i,"hwp")).collect(Collectors.joining()),11));
        assertThat(result.status()).isEqualTo("LIMIT_EXCEEDED");assertThat(result.descriptors()).hasSize(10);
    }
    @Test void disallowsSourceHostMenuQueryAndFormChanges(){
        var p=sample.profile();var s=sample.source();URI detail=p.selectDetailUri(s);
        for(String bad:List.of(detail+"&m=1031",detail+"&extra=1",detail+"#x",detail.toString().replace("1031","318"),detail.toString().replace("https:","http:"),detail.toString().replace("www.wando.go.kr","127.0.0.1")))assertThat(p.selectApprovedRequest(URI.create(bad))).isFalse();
        assertThatThrownBy(()->p.selectDetailUri(new AttachmentDiscoveryProfile.Source(s.providerCode(),s.providerNoticeId(),s.sourceUrl(),"LGS-000198",s.listParserProfileCode()))).isInstanceOf(IllegalArgumentException.class);
        String page=selectPage(selectFile(1,"hwp"),1);var d=p.selectDescriptors(s,page).descriptors().getFirst();var form=new HashMap<>(d.postForm());form.put("sys_file_nm","other"+"D".repeat(30));
        assertThat(p.selectApprovedRequest(d.selectRequest(),new Request(d.fetchUri(),"POST",form))).isFalse();
        for(String bad:List.of(page.replace("name=file_path","name=extra"),page.replace("type=hidden name=file_path","type=text name=file_path"),page.replace("name=file_path>","name=file_path value=unexpected>"),page.replace("https://eminwon.wando.go.kr","https://evil.example")))assertThat(p.selectDescriptors(s,bad).complete()).isFalse();
        assertThat(p.selectDescriptors(s,page.replace(")\">",");alert(1)\">" )).descriptors()).isEmpty();
    }
    @Test void invalidFormForOneFileDoesNotDiscardAnother(){
        String bad=selectFile(2,"hwp").replace("user"+"A".repeat(30)+2,"../bad");
        var result=sample.profile().selectDescriptors(sample.source(),selectPage(selectFile(1,"hwp")+bad,2));
        assertThat(result.complete()).isFalse();assertThat(result.descriptors()).hasSize(1);
    }
    @Test void bodyAndTitleExcludeMetadataAndKeepPolicy()throws Exception{
        var page=Jsoup.parse(selectPage(selectFile(1,"hwp"),1));assertThat(WandoNoticePage.selectContent(page).text()).isEqualTo("소상공인 디지털 전환 지원");
        AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,sample.title(),sample.titleLayout());
        assertThatThrownBy(()->AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,"다른 제목",sample.titleLayout())).isInstanceOf(AssertionError.class);
        var result=new AnnouncementSourceClassificationEngine().selectDecision(new AnnouncementSourceClassificationInput("LOCAL_GOV_NOTICE",sample.title(),null,null,List.of(),AnnouncementSourceClassificationCodes.BodySourceCode.NONE,AnnouncementSourceClassificationCodes.BodyAvailabilityCode.UNAVAILABLE),AnnouncementAttachmentRealFileQaTest.selectDraftRuleSet());
        assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectTitleMayProceed(result)).isTrue();
        var budget=AnnouncementAttachmentBbsOfficialObservationTest.selectBudget(sample.profile(),true,false);assertThat(budget.maximumRequests).isEqualTo(6);assertThat(budget.maximumBytes).isEqualTo(23*1024*1024);
    }
}
