package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.SokchoNoticePage;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

class SokchoDownloadContractTest {
    private String selectItem(int id,String ext){String q="user_file_nm=지원 공고 (1)."+ext+"&sys_file_nm=file"+id+"."+ext+"&file_path=/ntishome/file/upload/ofr/ofr/20260113",base="eminwon.sokcho.go.kr/emwp/jsp/ofr/FileDown.jsp?";return "<div class=attachFile><a href='https://"+base+q+"'><span class=icoFile></span>지원 공고 (1)."+ext+"</a><a href='#nolink' class='btn-file-ezview' data-url='http://"+base+q+"' data-file-seq=''>미리보기</a></div>";}
    private String selectPage(String files){return "<nav>수출 메뉴</nav><div id=content-bx><div class='skinTb skinTb-data-resList skinTb-data-bgSbj'><div class=skinTb-tr><div class=skinTb-th>제목</div><div class='skinTb-td skinTb-sbj'>소상공인 지원</div><div class=skinTb-th>담당부서</div><div class=skinTb-td>메타데이터</div></div><div class=skinTb-tr><div class='skinTb-td skinTb-conts'>소상공인 지원금</div></div><div class=skinTb-tr><div class=skinTb-th>첨부파일</div><div class=skinTb-td>"+files+"</div></div></div></div><footer>푸터</footer>";}
    @Test void preservesKnownFilesAndDoesNotRequestPreview(){
        var s=SokchoDownloadCases.selectCase();var p=s.profile();String files=selectItem(1,"pdf")+selectItem(2,"hwp")+selectItem(3,"hwpx");var r=p.selectDescriptors(s.source(),selectPage(files));assertThat(r.complete()).isTrue();assertThat(r.descriptors()).hasSize(3);
        r.descriptors().forEach(d->{assertThat(d.documentRole()).isEqualTo("UNKNOWN");assertThat(d.downloadAllowed()).isTrue();assertThat(d.selectRequest().method()).isEqualTo("GET");assertThat(p.selectApprovedRequest(d.selectRequest())).isTrue();assertThat(d.fetchUri().getScheme()).isEqualTo("https");});
        for(String extra:List.of("<a href='/unknown'>파일</a>","<button>첨부</button>","<img src='/unknown'>")){var partial=p.selectDescriptors(s.source(),selectPage(files+extra));assertThat(partial.complete()).isFalse();assertThat(partial.descriptors()).hasSize(3);}
        var mismatch=p.selectDescriptors(s.source(),selectPage(files.replace("data-url='http://eminwon.sokcho.go.kr","data-url='http://evil.example")));assertThat(mismatch.complete()).isFalse();assertThat(mismatch.descriptors()).hasSize(3);
        var unsupported=p.selectDescriptors(s.source(),selectPage(files+selectItem(4,"jpg")));assertThat(unsupported.complete()).isTrue();assertThat(unsupported.descriptors().getLast().downloadAllowed()).isFalse();
        assertThat(p.selectDescriptors(s.source(),selectPage("")).status()).isEqualTo("NO_FILES");assertThat(p.selectDescriptors(s.source(),"<main>첨부 없음</main>").complete()).isFalse();
        assertThat(p.selectDescriptors(s.source(),selectPage(IntStream.rangeClosed(1,11).mapToObj(i->selectItem(i,"pdf")).collect(Collectors.joining()))).status()).isEqualTo("LIMIT_EXCEEDED");
    }
    @Test void checksSourceFileIdentityAndMalformedStructure(){
        var s=SokchoDownloadCases.selectCase();var p=s.profile();String one=selectItem(1,"pdf");var d=p.selectDescriptors(s.source(),selectPage(one)).descriptors().getFirst();
        for(String bad:List.of(d.fetchUri().toString().replace("https:","http:"),d.fetchUri().toString().replace("eminwon.sokcho.go.kr","evil.example"),d.fetchUri()+"&extra=1",d.fetchUri()+"#x"))assertThat(p.selectApprovedRequest(URI.create(bad))).isFalse();
        var n=new AnnouncementSourceIdentityNormalizer();for(String bad:List.of(s.source().sourceUrl()+"&notAncmtMgtNo=1",s.source().sourceUrl()+"&extra=1",s.source().sourceUrl().replace("https:","http:")))assertThatThrownBy(()->p.selectDetailUri(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(bad)),bad,"LGS-000122","SAFE_SAEOL_EMINWON"))).hasMessage("PROFILE_REQUIRED");
        assertThatThrownBy(()->p.selectDetailUri(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE","wrong",s.source().sourceUrl(),"LGS-000122","SAFE_SAEOL_EMINWON"))).hasMessage("PROFILE_REQUIRED");
        assertThat(p.selectDescriptors(s.source(),selectPage(one).replace("id=content-bx","id=other")).complete()).isFalse();
        assertThat(p.selectDescriptors(s.source(),selectPage(one+one)).descriptors()).hasSize(1);
        assertThat(p.selectDescriptors(s.source(),selectPage(one.replace("지원 공고 (1).pdf</a>","다른 파일.pdf</a>"))).descriptors()).isEmpty();
        assertThat(p.selectDescriptors(s.source()," ".repeat(1_000_001)).complete()).isFalse();
    }
    @Test void bodyTitleAndBudgetAreSeparate(){
        var s=SokchoDownloadCases.selectCase();var page=Jsoup.parse(selectPage(selectItem(1,"pdf")));assertThat(SokchoNoticePage.selectContent(page).text()).isEqualTo("소상공인 지원금");AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,"소상공인 지원",s.titleLayout());assertThatThrownBy(()->SokchoNoticePage.selectContent(Jsoup.parse(page.toString()+page))).isInstanceOf(IllegalArgumentException.class);
        var b=new AnnouncementAttachmentBbsOfficialObservationTest.Budget(s.profile(),false);assertThat(b.maximumRequests).isEqualTo(6);assertThat(b.maximumBytes).isEqualTo(23L*1024*1024);
    }
    @EnabledIfEnvironmentVariable(named="SANEB_SOKCHO_SURVEY_FIXTURE",matches="true")
    @Test void officialHtmlBoundaries()throws Exception{
        var s=SokchoDownloadCases.selectCase();String html=Files.readString(Path.of("build/qa-saeol-next-20260929/SOKCHO-detail.html"));var page=Jsoup.parse(html);AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,s.title(),s.titleLayout());var r=s.profile().selectDescriptors(s.source(),html);assertThat(r.complete()).isTrue();assertThat(r.descriptors()).hasSize(2).allSatisfy(d->assertThat(d.expectedFormat()).isEqualTo("HWP"));assertThat(SokchoNoticePage.selectContent(page).text()).contains("소상공인","지원대상");
    }
    @Test void catalogIsReferenceOnly()throws Exception{
        var s=SokchoDownloadCases.selectCase();var json=new com.fasterxml.jackson.databind.ObjectMapper();var notices=json.readTree(Files.readString(Path.of("src/main/resources/announcement-attachment/provider-qa-catalog-v2.json"))).path("notices");var ref=StreamSupport.stream(notices.spliterator(),false).filter(n->s.code().equals(n.path("caseCode").asText())).findFirst().orElseThrow();assertThat(ref.path("source")).isEqualTo(json.valueToTree(s.source()));assertThat(ref.hasNonNull("expectation")).isFalse();
    }
}
