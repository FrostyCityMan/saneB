package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.MapoNoticePage;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

class MapoDownloadContractTest {
    private String selectItem(int id,String ext){String q="user_file_nm=지원 공고 (1)."+ext+"&sys_file_nm=file"+id+"."+ext+"&file_path=/ntishome/file/upload/ofr/ofr/20260227",base="eminwon.mapo.go.kr/emwp/jsp/ofr/FileDown.jsp?";return "<li><a class=file_name href='https://"+base+q+"'>지원 공고 (1)."+ext+"</a><a href='#none' class=file_view_btn onclick=\"viewapplys(19775,'http://"+base+q+"');return false;\">바로보기</a></li>";}
    private String selectPage(String files){return "<nav>메뉴</nav><div class=bbs_view><div class=bbs_view_body><div class=tbl_wrap3><table><tbody><tr><th>제목</th><td colspan=3>소상공인 지원</td></tr><tr><th>담당자</th><td>메타데이터</td></tr><tr><td colspan=4>소상공인 지원금</td></tr><!-- <tr><th>첨부파일</th><td><a href='/old'>과거 첨부</a></td></tr> --></tbody></table></div></div><div class=bbs_view_file><strong class=file_tit>첨부파일</strong><ul class=bbs_view_files_wrap>"+files+"</ul></div></div><footer>푸터</footer>";}
    @Test void preservesGoodFilesAndValidatesPreviewWithoutRequestingIt(){
        var s=MapoDownloadCases.selectCase();var p=s.profile();String files=selectItem(1,"pdf")+selectItem(2,"hwp")+selectItem(3,"hwpx");var r=p.selectDescriptors(s.source(),selectPage(files));assertThat(r.complete()).isTrue();assertThat(r.descriptors()).hasSize(3);
        r.descriptors().forEach(d->{assertThat(d.documentRole()).isEqualTo("UNKNOWN");assertThat(d.downloadAllowed()).isTrue();assertThat(d.selectRequest().method()).isEqualTo("GET");assertThat(p.selectApprovedRequest(d.selectRequest())).isTrue();assertThat(d.fetchUri().getScheme()).isEqualTo("https");});
        for(String extra:List.of("<a href='/unknown'>파일</a>","<button>첨부</button>","<img src='/unknown'>")){var partial=p.selectDescriptors(s.source(),selectPage(files+extra));assertThat(partial.complete()).isFalse();assertThat(partial.descriptors()).hasSize(3);}
        for(String changed:List.of(files.replace("viewapplys(19775","viewapplys(19776"),files.replace("viewapplys(","evil();viewapplys("),files.replace("http://eminwon.mapo.go.kr","http://evil.example"))){var partial=p.selectDescriptors(s.source(),selectPage(changed));assertThat(partial.complete()).isFalse();assertThat(partial.descriptors()).hasSize(3);}
        var unsupported=p.selectDescriptors(s.source(),selectPage(files+selectItem(4,"jpg")));assertThat(unsupported.complete()).isTrue();assertThat(unsupported.descriptors().getLast().downloadAllowed()).isFalse();
        assertThat(p.selectDescriptors(s.source(),selectPage("")).status()).isEqualTo("NO_FILES");assertThat(p.selectDescriptors(s.source(),"<main>첨부 없음</main>").complete()).isFalse();assertThat(p.selectDescriptors(s.source(),selectPage(IntStream.rangeClosed(1,11).mapToObj(i->selectItem(i,"pdf")).collect(Collectors.joining()))).status()).isEqualTo("LIMIT_EXCEEDED");
    }
    @Test void checksIdentityRequestsAndBodyBoundary(){
        var s=MapoDownloadCases.selectCase();var p=s.profile();String one=selectItem(1,"pdf");var d=p.selectDescriptors(s.source(),selectPage(one)).descriptors().getFirst();
        for(String bad:List.of(d.fetchUri().toString().replace("https:","http:"),d.fetchUri().toString().replace("eminwon.mapo.go.kr","evil.example"),d.fetchUri()+"&extra=1",d.fetchUri()+"#x"))assertThat(p.selectApprovedRequest(URI.create(bad))).isFalse();
        var n=new AnnouncementSourceIdentityNormalizer();for(String bad:List.of(s.source().sourceUrl()+"&bcId=1",s.source().sourceUrl()+"&extra=1",s.source().sourceUrl().replace("https:","http:")))assertThatThrownBy(()->p.selectDetailUri(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(bad)),bad,"LGS-000015","MAPO_LEGAL_NOTICE_TABLE"))).hasMessage("PROFILE_REQUIRED");
        assertThatThrownBy(()->p.selectDetailUri(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE","wrong",s.source().sourceUrl(),"LGS-000015","MAPO_LEGAL_NOTICE_TABLE"))).hasMessage("PROFILE_REQUIRED");
        assertThat(p.selectDescriptors(s.source(),selectPage(one+one)).descriptors()).hasSize(1);assertThat(p.selectDescriptors(s.source(),selectPage(one.replace("지원 공고 (1).pdf</a>","다른 파일.pdf</a>"))).descriptors()).isEmpty();assertThat(p.selectDescriptors(s.source()," ".repeat(1_000_001)).complete()).isFalse();
        var page=Jsoup.parse(selectPage(one));assertThat(MapoNoticePage.selectContent(page).text()).isEqualTo("소상공인 지원금");AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,"소상공인 지원",s.titleLayout());assertThatThrownBy(()->MapoNoticePage.selectContent(Jsoup.parse(page.toString()+page))).isInstanceOf(IllegalArgumentException.class);
        var b=new AnnouncementAttachmentBbsOfficialObservationTest.Budget(p,false);assertThat(b.maximumRequests).isEqualTo(6);assertThat(b.maximumBytes).isEqualTo(23L*1024*1024);
    }
    @EnabledIfEnvironmentVariable(named="SANEB_MAPO_SURVEY_FIXTURE",matches="true")
    @Test void officialHtmlBoundaries()throws Exception{
        var s=MapoDownloadCases.selectCase();String html=Files.readString(Path.of("build/qa-seoul-second-20260929/MAPO-detail.html"));var page=Jsoup.parse(html);AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,s.title(),s.titleLayout());var r=s.profile().selectDescriptors(s.source(),html);assertThat(r.complete()).isTrue();assertThat(r.descriptors()).hasSize(1);assertThat(r.descriptors().getFirst().expectedFormat()).isEqualTo("HWP");assertThat(MapoNoticePage.selectContent(page).text()).contains("소상공인","융자");
    }
    @Test void catalogIsReferenceOnly()throws Exception{
        var s=MapoDownloadCases.selectCase();var json=new com.fasterxml.jackson.databind.ObjectMapper();var notices=json.readTree(Files.readString(Path.of("src/main/resources/announcement-attachment/provider-qa-catalog-v2.json"))).path("notices");var ref=StreamSupport.stream(notices.spliterator(),false).filter(n->s.code().equals(n.path("caseCode").asText())).findFirst().orElseThrow();assertThat(ref.path("source")).isEqualTo(json.valueToTree(s.source()));assertThat(ref.hasNonNull("expectation")).isFalse();
    }
}
