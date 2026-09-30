package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import java.net.URI;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.*;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.provider.content.*;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;

class DongjakDownloadContractTest {
    @org.junit.jupiter.api.io.TempDir Path temporary;
    private final AnnouncementAttachmentBbsOfficialObservationTest.ObservationCase sample=MetroRemainderDownloadCases.selectCase(false);
    @Test void bodyObservationUsesTheRegisteredCollectionEndpoint()throws Exception{
        assertThat(sample.listUrl()).isEqualTo("https://dongjak.eminwon.seoul.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do");
        assertThat(URI.create(sample.listUrl()).getHost()).isEqualTo(URI.create(sample.source().sourceUrl()).getHost());
        String migration=java.nio.file.Files.readString(Path.of("src/main/resources/db/migration/V61__correct_general_notice_sources_to_official_legal_boards.sql"));
        assertThat(migration.lines().filter(line->line.contains("LGS-000021")).toList()).anySatisfy(line->assertThat(line).contains("'"+sample.listUrl()+"'"));
    }
    private String item(int n,String ext){return "<a href='#n' onclick=\"goDownLoad('지원 공고 "+n+"."+ext+"','stored_"+n+"."+ext+"','/ntishome/file/upload/ofr/ofr/20260930')\">지원 공고 "+n+"."+ext+"</a><br>";}
    private String page(String files){return "<form name=form method=post><div class=view><p class=subject>"+sample.title()+"</p><dl><dt>담당부서</dt><dd>수출 담당</dd><dt>첨부파일</dt><dd><strong>첨부파일</strong><br>"+files+"</dd></dl><div class=dbData>청년 사업자 금융지원</div></div></form>";}
    @Test void fixedReadPostIsBoundToSameLogicalNotice()throws Exception{
        var p=sample.profile();var initial=Request.selectGet(p.selectDetailUri(sample.source()));var post=DongjakNoticePage.selectRequest(initial.uri());
        assertThat(p.selectApprovedRequest(initial,post)).isTrue();assertThat(p.selectApprovedRequest(initial,initial)).isFalse();assertThat(post.method()).isEqualTo("POST");assertThat(post.form()).hasSize(7);assertThat(post.uri().getRawQuery()).isNull();
        var calls=new ArrayList<Request>();((AttachmentDownloadFlowProfile)p).selectDownload(initial,Path.of("unused"),1024,(request,limit)->{calls.add(request);assertThat(limit).isEqualTo(1024);return null;});assertThat(calls).containsExactly(post);
        var changed=new HashMap<>(post.form());changed.put("not_ancmt_mgt_no","29507");assertThat(p.selectApprovedRequest(initial,new Request(post.uri(),"POST",changed))).isFalse();changed.put("method","deleteOfrNotAncmt");assertThat(p.selectApprovedRequest(new Request(post.uri(),"POST",changed))).isFalse();
    }
    @Test void commonWorkerFlowChecksTheConvertedRequestBeforeTransport()throws Exception{
        var p=sample.profile();var initial=Request.selectGet(p.selectDetailUri(sample.source()));var requests=new ArrayList<Request>();
        AttachmentProfileDownloadFlow.selectDownload(p,initial,temporary.resolve("detail"),1024,(request,limit,approved)->{assertThat(approved.test(request)).isTrue();assertThat(approved.test(initial)).isFalse();requests.add(request);return null;});
        assertThat(requests).containsExactly(DongjakNoticePage.selectRequest(initial.uri()));
    }
    @Test void fileDownloadsRemainDirectGetAndCannotBecomePost()throws Exception{
        var p=sample.profile();var r=p.selectDescriptors(sample.source(),page(item(1,"hwpx")));assertThat(r.complete()).isTrue();assertThat(r.descriptors()).hasSize(1);var d=r.descriptors().getFirst();assertThat(d.expectedFormat()).isEqualTo("HWPX");assertThat(d.documentRole()).isEqualTo("UNKNOWN");assertThat(d.fetchUri().getPath()).isEqualTo("/emwp/jsp/ofr/FileDown.jsp");assertThat(p.selectApprovedRequest(d.selectRequest(),d.selectRequest())).isTrue();assertThat(p.selectApprovedRequest(new Request(d.fetchUri(),"POST",Map.of("x","y")))).isFalse();
        var calls=new ArrayList<Request>();((AttachmentDownloadFlowProfile)p).selectDownload(d.selectRequest(),Path.of("unused"),1024,(request,limit)->{calls.add(request);return null;});assertThat(calls).containsExactly(d.selectRequest());assertThat(d.locator().toString()).doesNotContain("stored_","ntishome","지원");
    }
    @Test void partialErrorsPreserveGoodFilesAndUnsupportedNames(){
        var p=sample.profile();for(String extra:List.of("<a href='/unknown'>파일</a>","<button>파일</button>",item(2,"hwp").replace("goDownLoad(","evil();goDownLoad("),item(2,"hwp").replace("<a ","<a onmouseover='evil()' "),item(2,"hwp").replace("/20260930","/../private"))){var r=p.selectDescriptors(sample.source(),page(item(1,"pdf")+extra));assertThat(r.complete()).isFalse();assertThat(r.descriptors()).hasSize(1);}
        var unsupported=p.selectDescriptors(sample.source(),page(item(1,"pdf")+item(2,"xlsx")));assertThat(unsupported.complete()).isTrue();assertThat(unsupported.descriptors().getLast().downloadAllowed()).isFalse();
    }
    @Test void missingAreasDoNotBecomeEmptyAndDuplicateLimitsAreBounded(){
        var p=sample.profile();assertThat(p.selectDescriptors(sample.source(),page("")).status()).isEqualTo("NO_FILES");for(String s:List.of("<p>실패</p>",page("")+page(""),page("").replace("<dt>첨부파일</dt>","<dt>기타</dt>")))assertThat(p.selectDescriptors(sample.source(),s).complete()).isFalse();
        var d=p.selectDescriptors(sample.source(),page(item(1,"pdf")+item(1,"pdf")));assertThat(d.complete()).isTrue();assertThat(d.descriptors()).hasSize(1);
        var limit=p.selectDescriptors(sample.source(),page(IntStream.rangeClosed(1,11).mapToObj(i->item(i,"pdf")).collect(Collectors.joining())));assertThat(limit.status()).isEqualTo("LIMIT_EXCEEDED");assertThat(limit.descriptors()).hasSize(10);
    }
    @Test void rejectsWrongSourceQueryHostAndCrossFileRedirect(){
        var p=sample.profile();var s=sample.source();assertThatThrownBy(()->p.selectDetailUri(new AttachmentDiscoveryProfile.Source(s.providerCode(),s.providerNoticeId(),s.sourceUrl(),"LGS-OTHER",s.listParserProfileCode()))).hasMessage("PROFILE_REQUIRED");
        for(String u:List.of(s.sourceUrl()+"&extra=x",s.sourceUrl()+"&not_ancmt_mgt_no=2",s.sourceUrl().replace("https:","http:"),s.sourceUrl().replace("dongjak.eminwon.seoul.kr","evil.example"),s.sourceUrl().replace("/emwp/","/%65mwp/")))assertThat(p.selectApprovedRequest(URI.create(u))).isFalse();
        var files=p.selectDescriptors(s,page(item(1,"pdf")+item(2,"pdf"))).descriptors();assertThat(p.selectApprovedRequest(files.getFirst().selectRequest(),files.getLast().selectRequest())).isFalse();
    }
    @Test void titleBodyAndFileBoundariesStaySeparate(){
        var doc=Jsoup.parse(page(item(1,"hwpx")));AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(doc,sample.title(),sample.titleLayout());assertThat(DongjakNoticePage.selectContent(doc).text()).isEqualTo("청년 사업자 금융지원");assertThatThrownBy(()->AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(doc,"다른 제목",sample.titleLayout())).isInstanceOf(AssertionError.class);
        var r=sample.profile().selectDescriptors(sample.source(),page(item(1,"hwpx"))+"<a href='/unrelated.pdf'>다른 파일</a>");assertThat(r.complete()).isTrue();assertThat(r.descriptors()).hasSize(1);
    }
    @Test void budgetDoesNotAddMimeExceptions(){var b=AnnouncementAttachmentBbsOfficialObservationTest.selectBudget(sample.profile(),true,false);assertThat(b.maximumRequests).isEqualTo(6);assertThat(b.maximumBytes).isEqualTo(23L*1024*1024);assertThat(sample.profile().selectLegacyBinaryContentTypes()).isEmpty();assertThat(sample.profile().selectUtf8DispositionOctets()).isFalse();}
}
