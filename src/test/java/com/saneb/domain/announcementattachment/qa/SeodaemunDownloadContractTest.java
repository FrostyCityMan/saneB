package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.SeodaemunNoticePage;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

class SeodaemunDownloadContractTest {
    private String selectItem(int id,String ext){String q="path=/board/82&oriFileNm=지원_공고."+ext+"&saveFileNm=24DF9124-87E7-8C6F-08FB-"+String.format("%012d",id)+"."+ext;return "<strong><a title='다운로드' href='/downloadFile.do?"+q+"'>지원 공고."+ext+"</a></strong><strong><a href='/htmlView/html.jsp?"+q+"'>미리보기</a></strong><br>";}
    private String selectPage(String files){return "<nav>메뉴</nav><table class=boardWrite><tbody><tr><td class=subject>소상공인 지원</td></tr><tr><th>담당자</th><td>메타데이터</td></tr><tr><td id=viewCon class=viewCon colspan=4>소상공인 지원금<img src='/image'></td></tr><tr><th scope=row>첨부파일</th><td colspan=3>"+files+"</td></tr></tbody></table><footer>푸터</footer>";}
    @Test void preservesGoodFilesAndValidatesPreviewWithoutRequestingIt(){
        var s=SeodaemunDownloadCases.selectCase();var p=s.profile();String files=selectItem(1,"pdf")+selectItem(2,"hwp")+selectItem(3,"hwpx");var r=p.selectDescriptors(s.source(),selectPage(files));assertThat(r.complete()).isTrue();assertThat(r.descriptors()).hasSize(3);
        r.descriptors().forEach(d->{assertThat(d.documentRole()).isEqualTo("UNKNOWN");assertThat(d.downloadAllowed()).isTrue();assertThat(d.selectRequest().method()).isEqualTo("GET");assertThat(p.selectApprovedRequest(d.selectRequest())).isTrue();assertThat(d.fetchUri().getPath()).isEqualTo("/downloadFile.do");assertThat(d.fetchUri().getScheme()).isEqualTo("https");});
        for(String extra:List.of("<a href='/unknown'>파일</a>","<button>첨부</button>","<img src='/unknown'>","<a href='/htmlView/html.jsp?path=/board/82&oriFileNm=unknown.pdf&saveFileNm=24DF9124-87E7-8C6F-08FB-000000000099.pdf'>미리보기</a>")){var partial=p.selectDescriptors(s.source(),selectPage(files+extra));assertThat(partial.complete()).isFalse();assertThat(partial.descriptors()).hasSize(3);}
        var unsupported=p.selectDescriptors(s.source(),selectPage(files+selectItem(4,"jpg")));assertThat(unsupported.complete()).isTrue();assertThat(unsupported.descriptors().getLast().downloadAllowed()).isFalse();
        assertThat(p.selectDescriptors(s.source(),selectPage("")).status()).isEqualTo("NO_FILES");assertThat(p.selectDescriptors(s.source(),"<main>첨부 없음</main>").complete()).isFalse();assertThat(p.selectDescriptors(s.source(),selectPage(IntStream.rangeClosed(1,11).mapToObj(i->selectItem(i,"pdf")).collect(Collectors.joining()))).status()).isEqualTo("LIMIT_EXCEEDED");
    }
    @Test void checksIdentityRequestsAndBodyBoundary(){
        var s=SeodaemunDownloadCases.selectCase();var p=s.profile();String one=selectItem(1,"pdf");var d=p.selectDescriptors(s.source(),selectPage(one)).descriptors().getFirst();
        for(String bad:List.of(d.fetchUri().toString().replace("https:","http:"),d.fetchUri().toString().replace("www.sdm.go.kr","evil.example"),d.fetchUri()+"&extra=1",d.fetchUri()+"#x",d.fetchUri().toString().replace("%2Fboard%2F82","%2Fboard%2F83"),d.fetchUri().toString().replace("24DF9124","not-uuid"),d.fetchUri().toString().replace("/downloadFile.do","/htmlView/html.jsp")))assertThat(p.selectApprovedRequest(URI.create(bad))).isFalse();
        var n=new AnnouncementSourceIdentityNormalizer();for(String bad:List.of(s.source().sourceUrl()+"&sdmBoardSeq=1",s.source().sourceUrl()+"&extra=1",s.source().sourceUrl().replace("https:","http:"),s.source().sourceUrl().replace("Seq=82","Seq=83")))assertThatThrownBy(()->p.selectDetailUri(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(bad)),bad,"LGS-000014","SAFE_SEODAEMUN_NOTICE"))).hasMessage("PROFILE_REQUIRED");
        assertThatThrownBy(()->p.selectDetailUri(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE","wrong",s.source().sourceUrl(),"LGS-000014","SAFE_SEODAEMUN_NOTICE"))).hasMessage("PROFILE_REQUIRED");
        assertThat(p.selectDescriptors(s.source(),selectPage(one+one)).descriptors()).hasSize(1);assertThat(p.selectDescriptors(s.source(),selectPage(one.replace("지원 공고.pdf</a>","다른 파일.pdf</a>"))).descriptors()).isEmpty();assertThat(p.selectDescriptors(s.source()," ".repeat(1_000_001)).complete()).isFalse();
        var conflict=p.selectDescriptors(s.source(),selectPage(one+one.replace("지원_공고","다른_공고").replace("지원 공고","다른 공고")));assertThat(conflict.complete()).isFalse();assertThat(conflict.descriptors()).hasSize(1);
        var page=Jsoup.parse(selectPage(one));assertThat(SeodaemunNoticePage.selectContent(page).text()).isEqualTo("소상공인 지원금");AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,"소상공인 지원",s.titleLayout());assertThatThrownBy(()->SeodaemunNoticePage.selectContent(Jsoup.parse(page.toString()+page))).isInstanceOf(IllegalArgumentException.class);
        var b=new AnnouncementAttachmentBbsOfficialObservationTest.Budget(p,false);assertThat(b.maximumRequests).isEqualTo(7);assertThat(b.maximumBytes).isEqualTo(23L*1024*1024);
    }
    @EnabledIfEnvironmentVariable(named="SANEB_SEODAEMUN_SURVEY_FIXTURE",matches="true")
    @Test void officialHtmlBoundaries()throws Exception{
        var s=SeodaemunDownloadCases.selectCase();String html=Files.readString(Path.of("build/qa-seoul-third-20260929/SEODAEMUN-detail.html"),Charset.forName("EUC-KR"));var page=Jsoup.parse(html);AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,s.title(),s.titleLayout());var r=s.profile().selectDescriptors(s.source(),html);assertThat(r.complete()).isTrue();assertThat(r.descriptors()).hasSize(4);assertThat(r.descriptors()).allSatisfy(d->assertThat(d.expectedFormat()).isEqualTo("HWPX"));assertThat(SeodaemunNoticePage.selectContent(page).text()).contains("소상공인","라이브커머스");
    }
    @Test void catalogIsReferenceOnly()throws Exception{
        var s=SeodaemunDownloadCases.selectCase();var json=new com.fasterxml.jackson.databind.ObjectMapper();var notices=json.readTree(Files.readString(Path.of("src/main/resources/announcement-attachment/provider-qa-catalog-v2.json"))).path("notices");var ref=StreamSupport.stream(notices.spliterator(),false).filter(n->s.code().equals(n.path("caseCode").asText())).findFirst().orElseThrow();assertThat(ref.path("source")).isEqualTo(json.valueToTree(s.source()));assertThat(ref.hasNonNull("expectation")).isFalse();
    }
}
