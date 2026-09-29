package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import com.saneb.domain.announcementattachment.discovery.AttachmentDiscoveryProfile.Source;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.provider.content.DaejeonAggregatorNoticePage;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

class DaejeonAggregatorDownloadContractTest {
    private final AnnouncementAttachmentBbsOfficialObservationTest.ObservationCase sample = DaejeonAggregatorDownloadCases.selectCase();
    private String item(int id, String extension) {
        return "<a href=\"javascript:goDownLoad('공고." + extension + "','file" + id + "." + extension + "','/ntishome/file/upload/ofr/ofr/20260324')\">공고." + extension + "</a><br>";
    }
    private String page(String items) {
        return "<nav>수출 메뉴</nav><form name=form1 method=post><table class=tbl_board><tr><th>담당부서</th><td>특허 부서</td></tr>"
                + "<tr><th>제목</th><td class='aleft end' colspan=3>소상공인 지원</td></tr>"
                + "<tr><td class='aleft end' colspan=4>소상공인 지원금</td></tr>"
                + "<tr><th>첨부파일</th><td class='aleft end' colspan=3>" + items + "</td></tr></table></form>";
    }
    @Test void sharedEnginePreservesSupportedFilesAndSeparatesErrors() {
        var p=sample.profile();String good=item(1,"hwp")+item(2,"pdf")+item(3,"hwpx");
        var result=p.selectDescriptors(sample.source(),page(good));assertThat(result.complete()).isTrue();
        assertThat(result.descriptors()).hasSize(3).allSatisfy(d->{assertThat(d.downloadAllowed()).isTrue();assertThat(d.documentRole()).isEqualTo("UNKNOWN");assertThat(p.selectApprovedRequest(d.selectRequest())).isTrue();assertThat(d.locator().profileCode()).isEqualTo(p.selectProfileCode());});
        for(String bad:List.of("<a href='https://evil.example/a.hwp'>공고</a>","<button>더보기</button>",item(4,"pdf").replace("file4.pdf","../file.pdf"))){var partial=p.selectDescriptors(sample.source(),page(good+bad));assertThat(partial.complete()).isFalse();assertThat(partial.descriptors()).hasSize(3);}
        var unsupported=p.selectDescriptors(sample.source(),page(item(1,"xlsx")));assertThat(unsupported.descriptors().getFirst().downloadAllowed()).isFalse();
        assertThat(p.selectDescriptors(sample.source(),page(item(1,"hwp")+item(1,"hwp"))).descriptors()).hasSize(1);
        var limit=p.selectDescriptors(sample.source(),page(IntStream.rangeClosed(1,11).mapToObj(i->item(i,"pdf")).collect(Collectors.joining())));assertThat(limit.status()).isEqualTo("LIMIT_EXCEEDED");assertThat(limit.descriptors()).hasSize(10);
    }
    @Test void sourceIdentityAndUnverifiedBranchesAreNotSilentlyRewritten() {
        var p=sample.profile();var n=new AnnouncementSourceIdentityNormalizer();
        assertThat(p.selectDetailUri(sample.source()).toString()).isEqualTo(sample.source().sourceUrl());
        for(String url:List.of(sample.source().sourceUrl()+"&homepage_pbs_yn=Y",sample.source().sourceUrl()+"&not_ancmt_mgt_no=1",sample.source().sourceUrl().replace("https:","http:"),sample.source().sourceUrl().replace("eminwon.seogu.go.kr","eminwon.djjunggu.go.kr"),sample.source().sourceUrl().replace("subCheck=Y","subCheck=N"))){assertThat(p.selectApprovedRequest(URI.create(url))).isFalse();assertThatThrownBy(()->p.selectDetailUri(new Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(url)),url,"LGS-000071","DAEJEON_EMINWON_AGGREGATOR"))).hasMessage("PROFILE_REQUIRED");}
        var r=p.selectDescriptors(sample.source(),page(item(1,"hwp"))).descriptors().getFirst().selectRequest();
        assertThat(p.selectApprovedRequest(new Request(r.uri(),"POST",Map.of("x","y")))).isFalse();
        assertThat(p.selectApprovedRequest(r,Request.selectGet(URI.create(r.uri()+"&x=1")))).isFalse();
        assertThat(p.selectApprovedRequest(URI.create("https:opaque"))).isFalse();
        assertThatThrownBy(()->p.selectDetailUri(new Source("LOCAL_GOV_NOTICE","bad",sample.source().sourceUrl(),"LGS-000071","DAEJEON_EMINWON_AGGREGATOR"))).hasMessage("PROFILE_REQUIRED");
    }
    @Test void bodyAndAbsenceAreScopedToOfficialTable() {
        var p=sample.profile();var uri=p.selectDetailUri(sample.source());var doc=Jsoup.parse(page(item(1,"hwp")));
        assertThat(DaejeonAggregatorNoticePage.selectContent(doc,uri).text()).isEqualTo("소상공인 지원금");
        AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(doc,"소상공인 지원",sample.titleLayout());
        assertThat(p.selectDescriptors(sample.source(),page("")).status()).isEqualTo("NO_FILES");
        assertThat(p.selectDescriptors(sample.source(),"<p>첨부 없음</p>").complete()).isFalse();
        assertThat(p.selectDescriptors(sample.source(),page("")+page("")).complete()).isFalse();
        assertThatThrownBy(()->DaejeonAggregatorNoticePage.selectContent(Jsoup.parse(page("").replace("colspan=4","colspan=2")),uri)).isInstanceOf(IllegalArgumentException.class);
        var budget=new AnnouncementAttachmentBbsOfficialObservationTest.Budget(p,false);assertThat(budget.maximumRequests).isEqualTo(6);assertThat(budget.maximumBytes).isEqualTo(23L*1024*1024);
    }
    @Test @EnabledIfEnvironmentVariable(named="SANEB_DAEJEON_AGGREGATOR_SURVEY_FIXTURE",matches="true")
    void actualOfficialFixture() throws Exception {
        var doc=Jsoup.parse(Files.readString(Path.of("build/qa-daejeon-city-20260930/detail.html")));
        AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(doc,sample.title(),sample.titleLayout());
        var result=sample.profile().selectDescriptors(sample.source(),doc.outerHtml());assertThat(result.complete()).as("%s",result.warnings()).isTrue();assertThat(result.descriptors()).hasSize(1);
        assertThat(DaejeonAggregatorNoticePage.selectContent(doc,sample.profile().selectDetailUri(sample.source())).text()).isNotBlank();
    }
    @Test void catalogReferenceDoesNotApprovePolicy() throws Exception {
        var mapper=new com.fasterxml.jackson.databind.ObjectMapper();var refs=mapper.readTree(Files.readString(Path.of("src/main/resources/announcement-attachment/provider-qa-catalog-v2.json"))).path("notices");
        var ref=StreamSupport.stream(refs.spliterator(),false).filter(r->sample.code().equals(r.path("caseCode").asText())).findFirst().orElseThrow();
        assertThat(ref.path("source")).isEqualTo(mapper.valueToTree(sample.source()));assertThat(ref.hasNonNull("expectation")).isFalse();
    }
}
