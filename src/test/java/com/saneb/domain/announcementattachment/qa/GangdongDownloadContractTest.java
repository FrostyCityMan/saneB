package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.domain.announcementattachment.discovery.AttachmentDiscoveryProfile;
import com.saneb.domain.announcementsource.classification.*;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.*;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.provider.content.GangdongNoticePage;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

class GangdongDownloadContractTest {
    private final AnnouncementAttachmentBbsOfficialObservationTest.ObservationCase sample=GangdongDownloadCases.selectCase();
    private String selectFile(int id,String extension){String name="지원 공고("+id+")."+extension;
        return "<a href=\"javascript:\" title=\"다운로드\" onclick=\"goDownLoad('"+name+"','saved"+id+"."+extension+"','/ntishome/file/upload/ofr/ofr/20210517')\">"+name+"</a>";}
    private String selectPage(String files){return "<form id=frmNotice method=get action=/web/newportal/notice/01><table><tbody>"
            +"<tr><th>담당부서</th><td>수출 지원</td></tr><tr><th>제목</th><td colspan=3>"+sample.title()+"</td></tr>"
            +"<tr><td colspan=4>소상공인 융자 지원</td></tr><tr><th>첨부파일</th><td colspan=3>"+files+"</td></tr></tbody></table></form>";}
    @Test void onlyOfficialPostFieldsAndUnknownRolesAreUsed(){
        var result=sample.profile().selectDescriptors(sample.source(),"<a href='/outside.pdf'>외부</a>"+selectPage(selectFile(1,"hwp")+selectFile(2,"hwpx")+selectFile(3,"pdf")));
        assertThat(result.complete()).isTrue();assertThat(result.descriptors()).hasSize(3).allSatisfy(file->{
            assertThat(file.documentRole()).isEqualTo("UNKNOWN");assertThat(file.downloadAllowed()).isTrue();
            assertThat(file.selectRequest().method()).isEqualTo("POST");assertThat(file.postForm()).containsEntry("isHome","N").hasSize(4);
            assertThat(sample.profile().selectApprovedRequest(file.selectRequest())).isTrue();
            assertThat(sample.profile().selectApprovedRequest(file.fetchUri())).isFalse();
            assertThat(file.locator().toString()).doesNotContain("지원","saved","ntishome");
        });
    }
    @Test void goodFilesSurviveMalformedLinksAndUnexecutedPreviewScripts(){
        for(String bad:List.of("<script>fnPreChk('aFile1','file.hwp')</script>","<a onclick=\"goViewer('x')\">바로보기</a>",
                selectFile(2,"hwp").replace("goDownLoad(","evil();goDownLoad("),selectFile(2,"hwp").replace("/20210517","/../private"),
                selectFile(2,"hwp").replace("<a ","<a onmouseover='evil()' "),selectFile(2,"hwp").replace("다운로드","파일"),
                selectFile(2,"hwp").replace(">지원 공고",">다른 공고"))){
            var result=sample.profile().selectDescriptors(sample.source(),selectPage(selectFile(1,"pdf")+bad));
            assertThat(result.complete()).isFalse();assertThat(result.descriptors()).hasSize(1);
        }
        var unsupported=sample.profile().selectDescriptors(sample.source(),selectPage(selectFile(1,"pdf")+selectFile(2,"xlsx")));
        assertThat(unsupported.complete()).isTrue();assertThat(unsupported.descriptors().getLast().downloadAllowed()).isFalse();
    }
    @Test void duplicatesConflictsAndBoundariesRemainExplicit(){
        String one=selectFile(1,"hwp");var profile=sample.profile();
        assertThat(profile.selectDescriptors(sample.source(),selectPage(one+one)).descriptors()).hasSize(1);
        var conflict=profile.selectDescriptors(sample.source(),selectPage(one+one.replace("지원 공고","다른 공고")));
        assertThat(conflict.complete()).isFalse();assertThat(conflict.descriptors()).hasSize(1);
        var limit=profile.selectDescriptors(sample.source(),selectPage(IntStream.rangeClosed(1,11).mapToObj(i->selectFile(i,"hwp")).collect(Collectors.joining())));
        assertThat(limit.status()).isEqualTo("LIMIT_EXCEEDED");assertThat(limit.descriptors()).hasSize(10);
    }
    @Test void missingOrChangedStructureIsNotAnEmptyAttachmentSet(){
        assertThat(sample.profile().selectDescriptors(sample.source(),selectPage("")).status()).isEqualTo("NO_FILES");
        for(String bad:List.of(selectPage("")+selectPage(""),selectPage("").replace("첨부파일","자료"),selectPage("<li></li>"),"<html/>"))
            assertThat(sample.profile().selectDescriptors(sample.source(),bad).complete()).isFalse();
        assertThatThrownBy(()->GangdongNoticePage.selectContent(Jsoup.parse(selectPage("").replace("colspan=4","colspan=2")))).hasMessage("GANGDONG_STRUCTURE_CHANGED");
    }
    @Test void urlSourceAndFileIdentityCannotBeChanged(){
        var profile=sample.profile();String url=sample.source().sourceUrl();
        assertThat(profile.selectDetailUri(sample.source())).isEqualTo(URI.create(url));
        for(String bad:List.of(url+"?x=1",url+"#x",url.replace("https:","http:"),url.replace("www.gangdong.go.kr","evil.example"),url.replace("/37327","/%33%37%33%32%37"),url+"/../37328"))
            assertThat(profile.selectApprovedRequest(URI.create(bad))).isFalse();
        var source=sample.source();assertThatThrownBy(()->profile.selectDetailUri(new AttachmentDiscoveryProfile.Source(source.providerCode(),"0".repeat(64),url,source.localSourceCode(),source.listParserProfileCode()))).hasMessage("PROFILE_REQUIRED");
        var files=profile.selectDescriptors(source,selectPage(selectFile(1,"hwp")+selectFile(2,"pdf"))).descriptors();var first=files.getFirst().selectRequest();
        assertThat(profile.selectApprovedRequest(first,first)).isTrue();assertThat(profile.selectApprovedRequest(first,files.getLast().selectRequest())).isFalse();
        for(var entry:Map.of("isHome","Y","file_path","/etc/private","sys_file_nm","../file.hwp","extra","x").entrySet()){
            var fields=new HashMap<>(first.form());fields.put(entry.getKey(),entry.getValue());assertThat(profile.selectApprovedRequest(new Request(first.uri(),"POST",fields))).isFalse();
        }
        assertThat(profile.selectApprovedRequest(new Request(URI.create(first.uri()+"?x=1"),"POST",first.form()))).isFalse();
        assertThat(profile.selectApprovedRequest(new Request(URI.create(first.uri().toString().replace("eminwon.gangdong.go.kr","127.0.0.1")),"POST",first.form()))).isFalse();
    }
    @Test void titleBodyAndDraftPolicyAreVerifiedSeparately() throws Exception {
        var page=Jsoup.parse(selectPage(selectFile(1,"hwp")));AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,sample.title(),sample.titleLayout());
        assertThatThrownBy(()->AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,"다른 제목",sample.titleLayout())).isInstanceOf(AssertionError.class);
        assertThat(GangdongNoticePage.selectContent(page).text()).isEqualTo("소상공인 융자 지원");
        var decision=new AnnouncementSourceClassificationEngine().selectDecision(new AnnouncementSourceClassificationInput("LOCAL_GOV_NOTICE",sample.title(),null,null,List.of(),BodySourceCode.NONE,BodyAvailabilityCode.UNAVAILABLE),AnnouncementAttachmentRealFileQaTest.selectDraftRuleSet());
        assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectTitleMayProceed(decision)).isTrue();
    }
    @Test void catalogAndBudgetRemainReferenceOnly() throws Exception {
        var json=new ObjectMapper();var notices=json.readTree(Files.readString(Path.of("src/main/resources/announcement-attachment/provider-qa-catalog-v2.json"))).path("notices");
        var entry=StreamSupport.stream(notices.spliterator(),false).filter(n->sample.code().equals(n.path("caseCode").asText())).findFirst().orElseThrow();
        assertThat(entry.path("source")).isEqualTo(json.valueToTree(sample.source()));assertThat(entry.hasNonNull("expectation")).isFalse();
        var budget=AnnouncementAttachmentBbsOfficialObservationTest.selectBudget(sample.profile(),true,false);assertThat(budget.maximumRequests).isEqualTo(9);assertThat(budget.maximumBytes).isEqualTo(23L*1024*1024);
    }
}
