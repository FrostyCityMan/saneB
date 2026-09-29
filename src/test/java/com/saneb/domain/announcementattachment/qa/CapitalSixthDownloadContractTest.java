package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.provider.content.CapitalSixthNoticePage;
import com.saneb.domain.announcementsource.provider.content.CapitalSixthNoticePage.Site;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class CapitalSixthDownloadContractTest {
    static Stream<String> selectGroups() { return CapitalSixthDownloadCases.GROUPS.stream().sorted(); }
    private String selectItem(String group,int id,String ext) {
        String name="지원 공고문."+ext,fileId="1768494252902O3IP51944NE3RIE4D2BIBLEQW"+id;
        if(group.equals("SIHEUNG")) return "<li><a href='#' onclick=\"goDownload('"+name+"', 'saved"+id+"."+ext+"', '/ntishome/file/upload/ofr/ofr/20260929'); return false;\"><span>"+name+"</span></a></li>";
        return "<li class='p-attach__item'><a class='p-attach__link' href='#' onclick=\"fnFileDownLoad('"+fileId+"'); return false;\"><span class='p-icon'>파일</span><span>"+name+"</span><i class='p-icon'>다운로드</i></a><a class='p-attach__preview' href='#' onclick=\"fnOpenPreview('"+fileId+"'); return false;\"><i class='p-icon'></i>미리보기</a></li>";
    }
    private String selectPage(String group,String files) {
        if(group.equals("SIHEUNG")) return "<main><nav>수출 메뉴</nav><form id='detailForm' name='detailForm' method='post'><div class='bod_wrap'><div class='bod_view'><h4>소상공인 지원</h4><div class='view_info'>담당자</div><div class='view_cont'>소상공인 지원금</div><dl class='view_file'><dt>첨부 파일</dt><dd><div id='updateFileList'><ul>"+files+"</ul></div></dd></dl></div></div></form></main>";
        return "<main><nav>수출 메뉴</nav><form id='aform' method='get'><input type='hidden' name='bbs_code' value='WWW13'><input type='hidden' name='bbs_seq' value='1660335'><input type='hidden' name='file_id'><div class='p-wrap bbs bbs__view'><table class='p-table'><tr><th>제목</th><td>소상공인 지원</td></tr><tr><th>담당부서</th><td>메타데이터</td></tr><tr><th>내용</th><td>소상공인 지원금</td></tr><tr><th>파일</th><td><ul class='p-attach'>"+files+"</ul></td></tr></table></div></form><footer>특허 푸터</footer></main>";
    }

    @ParameterizedTest @MethodSource("selectGroups") void keepsGoodFilesAndSeparatesUnknownOrUnsupportedFiles(String group) {
        var s=CapitalSixthDownloadCases.selectCase(group);String files=selectItem(group,1,"hwp")+selectItem(group,2,"pdf")+selectItem(group,3,"hwpx");
        var r=s.profile().selectDescriptors(s.source(),selectPage(group,files));assertThat(r.complete()).isTrue();assertThat(r.descriptors()).hasSize(3);
        r.descriptors().forEach(d->{assertThat(d.downloadAllowed()).isTrue();assertThat(d.documentRole()).isEqualTo("UNKNOWN");assertThat(s.profile().selectApprovedRequest(d.selectRequest())).isTrue();});
        for(String extra:List.of("<li><a href='/unknown'>미확인</a></li>","<button>다른 파일</button>","<img src='/extra'>")) {
            var partial=s.profile().selectDescriptors(s.source(),selectPage(group,files+extra));assertThat(partial.complete()).isFalse();assertThat(partial.descriptors()).hasSize(3);
        }
        var unsupported=s.profile().selectDescriptors(s.source(),selectPage(group,files+selectItem(group,4,"jpg")));assertThat(unsupported.descriptors()).hasSize(4);assertThat(unsupported.descriptors().getLast().downloadAllowed()).isFalse();
        assertThat(s.profile().selectDescriptors(s.source(),selectPage(group,"")).status()).isEqualTo("NO_FILES");
        assertThat(s.profile().selectDescriptors(s.source(),"<main>첨부 없음</main>").complete()).isFalse();
    }

    @ParameterizedTest @MethodSource("selectGroups") void preservesIdentityMethodLimitsAndTextBoundaries(String group) {
        var s=CapitalSixthDownloadCases.selectCase(group);String one=selectItem(group,1,"hwp");var p=s.profile();
        assertThat(p.selectDescriptors(s.source(),selectPage(group,one+one)).descriptors()).hasSize(1);
        var conflict=p.selectDescriptors(s.source(),selectPage(group,one+one.replace("지원 공고문.hwp","변경 공고문.hwp")));assertThat(conflict.complete()).isFalse();assertThat(conflict.descriptors()).hasSize(1);
        var limit=p.selectDescriptors(s.source(),selectPage(group,IntStream.rangeClosed(1,11).mapToObj(i->selectItem(group,i,"hwp")).collect(Collectors.joining())));assertThat(limit.status()).isEqualTo("LIMIT_EXCEEDED");assertThat(limit.descriptors()).hasSize(10);
        var request=p.selectDescriptors(s.source(),selectPage(group,one)).descriptors().getFirst().selectRequest();assertThat(request.method()).isEqualTo("GET");assertThat(p.selectApprovedRequest(request,request)).isTrue();
        assertThat(p.selectApprovedRequest(request,Request.selectGet(p.selectDetailUri(s.source())))).isFalse();
        assertThatThrownBy(()->new Request(request.uri(),"POST",Map.of())).hasMessage("ATTACHMENT_REQUEST_INVALID");
        assertThat(p.selectApprovedRequest(new Request(request.uri(),"POST",Map.of("file_id","unused")))).isFalse();
        for(String bad:List.of("https:opaque",request.uri().toString().replace("https:","http:"),request.uri()+"&unknown=1",request.uri()+"#x")) assertThat(p.selectApprovedRequest(URI.create(bad))).isFalse();
        var n=new AnnouncementSourceIdentityNormalizer();for(String bad:List.of(s.source().sourceUrl()+"&unknown=1",s.source().sourceUrl()+"&"+Site.valueOf(group).menuKey+"=0")) assertThatThrownBy(()->p.selectDetailUri(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(bad)),bad,s.source().localSourceCode(),s.source().listParserProfileCode()))).hasMessage("PROFILE_REQUIRED");
        var page=Jsoup.parse(selectPage(group,one));assertThat(CapitalSixthNoticePage.selectContent(Site.valueOf(group),page).text()).isEqualTo("소상공인 지원금");AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,"소상공인 지원",s.titleLayout());
        var budget=new AnnouncementAttachmentBbsOfficialObservationTest.Budget(p,false);assertThat(budget.maximumRequests).isEqualTo(6);assertThat(budget.maximumBytes).isEqualTo(23L*1024*1024);
    }

    @Test void ansanFormAndPreviewStayBoundToNoticeAndFile() {
        var s=CapitalSixthDownloadCases.selectCase("ANSAN");String html=selectPage("ANSAN",selectItem("ANSAN",1,"hwp"));
        assertThat(s.profile().selectDescriptors(s.source(),html.replace("value='1660335'","value='1660336'")).warnings()).containsExactly("ATTACHMENT_DOWNLOAD_FORM_CHANGED");
        var partial=s.profile().selectDescriptors(s.source(),html.replace("fnOpenPreview(","otherPreview("));assertThat(partial.complete()).isFalse();assertThat(partial.descriptors()).hasSize(1);
        var invalid=s.profile().selectDescriptors(s.source(),html.replace("fnFileDownLoad('","fnFileDownLoad('../"));assertThat(invalid.descriptors()).isEmpty();assertThat(invalid.complete()).isFalse();
    }

    @EnabledIfEnvironmentVariable(named="SANEB_CAPITAL_SIXTH_SURVEY_FIXTURE",matches="true")
    @ParameterizedTest @MethodSource("selectGroups") void actualOfficialBoundaries(String group)throws Exception {
        var s=CapitalSixthDownloadCases.selectCase(group);String html=Files.readString(Path.of("build/qa-capital-sixth-20260929/"+group+"-detail.html"));
        AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(Jsoup.parse(html),s.title(),s.titleLayout());var result=s.profile().selectDescriptors(s.source(),html);assertThat(result.complete()).as(group).isTrue();assertThat(result.descriptors()).hasSize(1);
    }
    @Test void catalogStaysReferenceOnly()throws Exception {
        var json=new com.fasterxml.jackson.databind.ObjectMapper();var notices=json.readTree(Files.readString(Path.of("src/main/resources/announcement-attachment/provider-qa-catalog-v2.json"))).path("notices");
        for(String group:selectGroups().toList()) { var s=CapitalSixthDownloadCases.selectCase(group);var ref=StreamSupport.stream(notices.spliterator(),false).filter(n->s.code().equals(n.path("caseCode").asText())).findFirst().orElseThrow();assertThat(ref.path("source")).isEqualTo(json.valueToTree(s.source()));assertThat(ref.hasNonNull("expectation")).isFalse();assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectCases(group).count()).isEqualTo(1); }
    }
}
