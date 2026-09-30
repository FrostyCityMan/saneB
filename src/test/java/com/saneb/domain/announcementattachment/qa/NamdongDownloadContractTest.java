package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.provider.content.NamdongNoticePage;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;
import org.jsoup.Jsoup;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class NamdongDownloadContractTest {
    @org.junit.jupiter.api.Test void officialTitlesRespectExistingCombinationGate()throws Exception{
        var engine=new com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationEngine();
        var rules=AnnouncementAttachmentRealFileQaTest.selectDraftRuleSet();
        for(var sample:List.of(NamdongDownloadCases.selectCase())){
            var input=new com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationInput("LOCAL_GOV_NOTICE",sample.title(),null,null,List.of(),com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.BodySourceCode.NONE,com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.BodyAvailabilityCode.UNAVAILABLE);
            assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectTitleMayProceed(engine.selectDecision(input,rules))).isTrue();
        }
    }
    @org.junit.jupiter.api.Test void sourceMigrationIsNotSilentlyApplied()throws Exception{
        var sample=NamdongDownloadCases.selectCase();var n=new AnnouncementSourceIdentityNormalizer();
        assertThat(Files.readString(Path.of("src/main/resources/db/migration/V57__correct_miryang_hamyang_official_notice_sources.sql"))).contains("https://biz.namdong.go.kr/main/news/announce.jsp");
        assertThat(sample.listUrl()).isEqualTo("https://www.namdong.go.kr/main/news/announce.jsp");
        String stored=n.canonicalizeUrl(sample.source().sourceUrl()+"&keyfield=title&keyword=%EC%A7%80%EC%9B%90");
        var source=new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(stored),stored,"LGS-000059","HEURISTIC_NOTICE");
        assertThat(sample.profile().selectDetailUri(source).toString()).isEqualTo(sample.source().sourceUrl());
        assertThat(sample.profile().selectApprovedRequest(URI.create(sample.source().sourceUrl().replace("www.namdong","biz.namdong")))).isFalse();
    }
    private String encode(String s){return URLEncoder.encode(s,StandardCharsets.UTF_8);}
    private String selectItem(String group,int id,String ext){String suffix="A".repeat(42)+id+"==",path="/ntisho"+"B".repeat(50),url="https://"+"eminwon.namdong.go.kr"+"/emwp/jsp/ofr/FileDownNew.jsp?user_file_nm="+encode("공고 "+suffix)+"&sys_file_nm="+encode("저장 "+suffix)+"&file_path="+encode(path);return "<li><a href='"+url+"'><img src='/share/images/filetype/"+ext+".gif' alt='"+ext+"' class=middle>지원 공고."+ext+"</a></li>";}
    private String selectPage(String group,String files){String title="<div class=title><p>소상공인 지원</p></div>",meta="<dl class=data><dt>담당부서</dt><dd>수출 메타데이터</dd></dl>",file="<div class=add_file><dl><dt>첨부파일</dt><dd><ul>"+files+"</ul></dd></dl></div>";return "<div class=board_view>"+title+meta+file+"<div class=con><div class=detail>소상공인 지원금</div></div></div>";}
    @ParameterizedTest @ValueSource(strings={"NAMDONG"})
    void successfulDownloadsAreIndependentOfOtherLinkErrors(String group){var s=NamdongDownloadCases.selectCase();var p=s.profile();String files=selectItem(group,1,"pdf")+selectItem(group,2,"hwp")+selectItem(group,3,"hwpx");var r=p.selectDescriptors(s.source(),selectPage(group,files));assertThat(r.complete()).isTrue();assertThat(r.descriptors()).hasSize(3);r.descriptors().forEach(d->{assertThat(d.downloadAllowed()).isTrue();assertThat(d.documentRole()).isEqualTo("UNKNOWN");assertThat(d.selectRequest().method()).isEqualTo("GET");assertThat(p.selectApprovedRequest(d.selectRequest(),d.selectRequest())).isTrue();assertThat(d.locator().toString()).doesNotContain("공고","저장","ntisho");});
        for(String extra:List.of("<a href='/unknown'>첨부</a>","<button>파일</button>","<img src='/unknown'>")){var partial=p.selectDescriptors(s.source(),selectPage(group,files+extra));assertThat(partial.complete()).isFalse();assertThat(partial.descriptors()).hasSize(3);}
        var unsupported=p.selectDescriptors(s.source(),selectPage(group,files+selectItem(group,4,"docx")));assertThat(unsupported.complete()).isTrue();assertThat(unsupported.descriptors().getLast().downloadAllowed()).isFalse();assertThat(p.selectDescriptors(s.source(),selectPage(group,"")).status()).isEqualTo("NO_FILES");assertThat(p.selectDescriptors(s.source(),"<p>첨부 없음</p>").complete()).isFalse();
        var limit=p.selectDescriptors(s.source(),selectPage(group,IntStream.rangeClosed(1,11).mapToObj(i->selectItem(group,i,"pdf")).collect(Collectors.joining())));assertThat(limit.status()).isEqualTo("LIMIT_EXCEEDED");assertThat(limit.descriptors()).hasSize(10);
    }
    @ParameterizedTest @ValueSource(strings={"NAMDONG"})
    void rejectsWrongSourceHostMethodQueryAndRedirect(String group){var s=NamdongDownloadCases.selectCase();var p=s.profile();var b=p.selectSourceBindings().getFirst();String one=selectItem(group,1,"pdf");var d=p.selectDescriptors(s.source(),selectPage(group,one)).descriptors().getFirst();
        for(String bad:List.of(d.fetchUri().toString().replace("https:","http:"),d.fetchUri().toString().replace(d.fetchUri().getHost(),"evil.example"),d.fetchUri()+"&extra=1",d.fetchUri()+"#x",d.fetchUri()+"&file_path=other",d.fetchUri().toString().replace("/emwp/","/emwp/../"),d.fetchUri().toString().replace("/jsp/","/%6asp/"))){var next=Request.selectGet(URI.create(bad));assertThat(p.selectApprovedRequest(next)).isFalse();assertThat(p.selectApprovedRequest(d.selectRequest(),next)).isFalse();}
        assertThat(p.selectApprovedRequest(URI.create("https:opaque"))).isFalse();assertThat(p.selectApprovedRequest(new Request(d.fetchUri(),"POST",Map.of("unexpected","value")))).isFalse();var n=new AnnouncementSourceIdentityNormalizer();
        for(String bad:List.of(s.source().sourceUrl()+"&extra=1",s.source().sourceUrl()+"&mgt_no=99",s.source().sourceUrl().replace("https:","http:")))assertThatThrownBy(()->p.selectDetailUri(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(bad)),bad,b.localSourceCode(),b.listParserProfileCode()))).hasMessage("PROFILE_REQUIRED");
        assertThatThrownBy(()->p.selectDetailUri(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE","wrong",s.source().sourceUrl(),b.localSourceCode(),b.listParserProfileCode()))).hasMessage("PROFILE_REQUIRED");assertThatThrownBy(()->p.selectDetailUri(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",s.source().providerNoticeId(),s.source().sourceUrl(),"LGS-OTHER",b.listParserProfileCode()))).hasMessage("PROFILE_REQUIRED");
        var other=p.selectDescriptors(s.source(),selectPage(group,selectItem(group,2,"pdf"))).descriptors().getFirst();assertThat(p.selectApprovedRequest(d.selectRequest(),other.selectRequest())).isFalse();var budget=AnnouncementAttachmentBbsOfficialObservationTest.selectBudget(p,true,false);assertThat(budget.maximumRequests).isEqualTo(6);assertThat(budget.maximumBytes).isEqualTo(23L*1024*1024);
    }
    @ParameterizedTest @ValueSource(strings={"NAMDONG"})
    void keepsOpaqueBytesAndSeparatesDuplicateConflictAndBadIcon(String group){var s=NamdongDownloadCases.selectCase();var p=s.profile();String one=selectItem(group,1,"pdf");var r=p.selectDescriptors(s.source(),selectPage(group,one+one));assertThat(r.complete()).isTrue();assertThat(r.descriptors()).hasSize(1);assertThat(r.descriptors().getFirst().fetchUri().toString()).isEqualTo(Jsoup.parse(one).selectFirst("a").attr("href"));
        assertThat(p.selectDescriptors(s.source(),selectPage(group,one+one.replace("지원 공고","다른 공고"))).complete()).isFalse();
        for(String bad:List.of(one.replace("<a href=","<a onclick='evil()' href="),one.replace("alt='pdf'","alt='exe'"),one.replace("src='/share/","src='https://evil.example/"),one.replace("file_path=%2Fntisho","file_path=%2F..%2Fntisho"),one.replace("<img ","<img onerror='evil()' "))){var partial=p.selectDescriptors(s.source(),selectPage(group,selectItem(group,2,"hwp")+bad));assertThat(partial.complete()).isFalse();assertThat(partial.descriptors()).hasSize(1);}
    }
    @ParameterizedTest @ValueSource(strings={"NAMDONG"})
    void bodyTitleAndAttachmentBoundariesAreIndependent(String group){var s=NamdongDownloadCases.selectCase();var page=Jsoup.parse(selectPage(group,selectItem(group,1,"pdf")));assertThat(NamdongNoticePage.selectContent(page).text()).isEqualTo("소상공인 지원금");AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,"소상공인 지원",s.titleLayout());assertThatThrownBy(()->NamdongNoticePage.selectContent(Jsoup.parse(page.toString()+page))).isInstanceOf(IllegalArgumentException.class);}
    @ParameterizedTest @ValueSource(strings={"NAMDONG"})
    void catalogOnlyStoresReference(String group)throws Exception{var s=NamdongDownloadCases.selectCase();var json=new com.fasterxml.jackson.databind.ObjectMapper();var notices=json.readTree(Files.readString(Path.of("src/main/resources/announcement-attachment/provider-qa-catalog-v2.json"))).path("notices");var ref=StreamSupport.stream(notices.spliterator(),false).filter(n->s.code().equals(n.path("caseCode").asText())).findFirst().orElseThrow();assertThat(ref.path("source")).isEqualTo(json.valueToTree(s.source()));assertThat(ref.hasNonNull("expectation")).isFalse();}
}
