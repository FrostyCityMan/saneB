package com.saneb.domain.announcementattachment.qa;
import static org.assertj.core.api.Assertions.*;
import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.provider.content.CapitalFourthNoticePage;
import com.saneb.domain.announcementsource.provider.content.CapitalFourthNoticePage.Site;
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

class CapitalFourthDownloadContractTest {
    static Stream<String> selectGroups(){return CapitalFourthDownloadCases.GROUPS.stream().sorted();}
    private String selectItem(String group,int id,String ext){var s=Site.valueOf(group);String name="공고문."+ext,stored="saved"+id+"."+ext,path="/ntishome/file/upload/ofr/ofr/20260929";
        if(s==Site.GIMPO)return "<li><a class='p-attach__link ntfc_file_down' href='#n' data-user-file-nm='"+name+"' data-sys-file-nm='"+stored+"' data-file-path='"+path+"'><span class='p-icon'>파일</span><span>"+name+"</span></a></li>";
        if(s==Site.DONGDUCHEON)return "<li><div class='down_view'><span><img src='/icon.gif'>"+name+"</span><a class='file_down' href='https://"+s.fileHost+s.download+"?user_file_nm="+name+"&sys_file_nm="+stored+"&file_path="+path+"'>다운로드<i></i></a></div><style>.file_down{right:0}</style></li>";
        String args="('"+name+"', '"+stored+"', '"+path+"'); return false;";
        return "<li><span class='view_list_file'><a href='#' onclick=\"goDownload"+args+"\">"+name+"</a></span><a href='#' class='btn_white none' onclick=\"fn_egov_gosi_preview('95902','95902-"+(id-1)+"."+ext+"','"+name+"','"+stored+"','"+path+"'); return false;\">미리보기/음성듣기</a></li>";
    }
    private String selectPage(String group,String files){var s=Site.valueOf(group);
        if(s==Site.PYEONGTAEK)return "<main><nav>수출 메뉴</nav><form id='detailForm' name='detailForm' method='post'><div class='bod_wrap'><div class='bod_view'><h4>소상공인 지원</h4><div class='view_info'>담당부서</div><div class='view_cont'>소상공인 지원금</div><dl class='view_file'><dt><span>첨부 파일</span></dt><dd><ul id='updateFileList' class='file_list'>"+files+"</ul></dd></dl></div></div></form></main>";
        return "<main><div id='contents'><table class='"+(s==Site.GIMPO?"p-table block":"bbs_default view")+"'><tr><th>"+(s==Site.GIMPO?"제목":"제 목")+"</th><td>소상공인 지원</td></tr>"
                +(s==Site.GIMPO?"<tr><td colspan='4'><div>소상공인 지원금</div></td></tr>":"<tr><th>내용</th><td>소상공인 지원금</td></tr>")+"<tr><th>첨부파일</th><td><ul class='"+(s==Site.GIMPO?"p-attach":"view_attach")+"'>"+files+"</ul>"
                +(s==Site.DONGDUCHEON?"<br><span>※ 게재기간이 지난 첨부파일은 다운로드가 불가하오니 담당부서로 문의하시기 바랍니다.</span>":"<script>neverExecute()</script>")+"</td></tr></table></div><footer>특허 푸터</footer></main>";
    }
    @ParameterizedTest @MethodSource("selectGroups") void preservesFilesWhileUnknownLinksAndUnsupportedTypesRemainSeparate(String g){var s=CapitalFourthDownloadCases.selectCase(g);String files=selectItem(g,1,"hwp")+selectItem(g,2,"hwpx")+selectItem(g,3,"pdf");
        var r=s.profile().selectDescriptors(s.source(),selectPage(g,files));assertThat(r.complete()).as(g).isTrue();assertThat(r.descriptors()).hasSize(3);r.descriptors().forEach(d->{assertThat(d.downloadAllowed()).isTrue();assertThat(d.documentRole()).isEqualTo("UNKNOWN");assertThat(s.profile().selectApprovedRequest(d.selectRequest())).isTrue();});
        for(String unknown:List.of("<li><a href='/unknown.pdf'>미확인</a></li>","<button>추가 첨부</button>","<img src='/extra'>")){var p=s.profile().selectDescriptors(s.source(),selectPage(g,files+unknown));assertThat(p.complete()).isFalse();assertThat(p.descriptors()).hasSize(3);}
        var mixed=s.profile().selectDescriptors(s.source(),selectPage(g,files+selectItem(g,4,"jpg")));assertThat(mixed.descriptors()).hasSize(4);assertThat(mixed.descriptors().getLast().downloadAllowed()).isFalse();
        assertThat(s.profile().selectDescriptors(s.source(),selectPage(g,"")).status()).isEqualTo("NO_FILES");assertThat(s.profile().selectDescriptors(s.source(),"<main>게시기간이 아닙니다.</main>").complete()).isFalse();
    }
    @ParameterizedTest @MethodSource("selectGroups") void constrainsIdentityLimitsAndRequestScope(String g){var s=CapitalFourthDownloadCases.selectCase(g);var p=s.profile();String one=selectItem(g,1,"hwp");
        assertThat(p.selectDescriptors(s.source(),selectPage(g,one+one)).descriptors()).hasSize(1);var conflict=p.selectDescriptors(s.source(),selectPage(g,one+one.replace("공고문.hwp","변경.hwp")));assertThat(conflict.complete()).isFalse();assertThat(conflict.descriptors()).hasSize(1);
        var limit=p.selectDescriptors(s.source(),selectPage(g,IntStream.rangeClosed(1,11).mapToObj(i->selectItem(g,i,"hwp")).collect(Collectors.joining())));assertThat(limit.status()).isEqualTo("LIMIT_EXCEEDED");assertThat(limit.descriptors()).hasSize(10);
        var req=p.selectDescriptors(s.source(),selectPage(g,one)).descriptors().getFirst().selectRequest();assertThat(req.method()).isEqualTo("GET");assertThat(p.selectApprovedRequest(req,req)).isTrue();assertThat(p.selectApprovedRequest(req,Request.selectGet(p.selectDetailUri(s.source())))).isFalse();
        for(String bad:List.of("https:opaque",req.uri().toString().replace("https:","http:"),req.uri()+"&extra=x",req.uri()+"#x"))assertThat(p.selectApprovedRequest(URI.create(bad))).isFalse();
        var n=new AnnouncementSourceIdentityNormalizer();for(String bad:List.of(s.source().sourceUrl()+"&unknown=1",s.source().sourceUrl()+"&"+Site.valueOf(g).menuKey+"=0"))assertThatThrownBy(()->p.selectDetailUri(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(bad)),bad,s.source().localSourceCode(),"SAEOL_GOSI"))).hasMessage("PROFILE_REQUIRED");
        var page=Jsoup.parse(selectPage(g,one));assertThat(CapitalFourthNoticePage.selectContent(Site.valueOf(g),page).text()).isEqualTo("소상공인 지원금");AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,"소상공인 지원",s.titleLayout());
        var budget=new AnnouncementAttachmentBbsOfficialObservationTest.Budget(p,false);assertThat(budget.maximumRequests).isEqualTo(6);assertThat(budget.maximumBytes).isEqualTo(23L*1024*1024);
    }
    @Test void pyeongtaekPreviewConflictNeverDiscardsValidDownload(){var s=CapitalFourthDownloadCases.selectCase("PYEONGTAEK");var r=s.profile().selectDescriptors(s.source(),selectPage("PYEONGTAEK",selectItem("PYEONGTAEK",1,"hwp").replace("'95902-0.hwp'","'other.hwp'")));assertThat(r.complete()).isFalse();assertThat(r.descriptors()).hasSize(1);}
    @EnabledIfEnvironmentVariable(named="SANEB_CAPITAL_FOURTH_SURVEY_FIXTURE",matches="true")
    @ParameterizedTest @MethodSource("selectGroups") void actualOfficialBoundaries(String g)throws Exception{var s=CapitalFourthDownloadCases.selectCase(g);String html=Files.readString(Path.of("build/qa-capital-fourth-20260929/"+g+(g.equals("GIMPO")?"-loan":"")+"-detail.html"));
        AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(Jsoup.parse(html),s.title(),s.titleLayout());var r=s.profile().selectDescriptors(s.source(),html);assertThat(r.complete()).as(g).isTrue();assertThat(r.descriptors()).hasSize(g.equals("DONGDUCHEON")?3:1);
        if(g.equals("GIMPO"))assertThat(s.profile().selectDescriptors(s.source(),Files.readString(Path.of("build/qa-capital-fourth-20260929/GIMPO-detail.html"))).complete()).isFalse();}
    @Test void catalogIsReferenceOnlyWithMatchingSourceIdentity()throws Exception{var json=new com.fasterxml.jackson.databind.ObjectMapper();var notices=json.readTree(Files.readString(Path.of("src/main/resources/announcement-attachment/provider-qa-catalog-v2.json"))).path("notices");for(String g:selectGroups().toList()){var s=CapitalFourthDownloadCases.selectCase(g);var ref=StreamSupport.stream(notices.spliterator(),false).filter(n->s.code().equals(n.path("caseCode").asText())).findFirst().orElseThrow();assertThat(ref.path("source")).isEqualTo(json.valueToTree(s.source()));assertThat(ref.hasNonNull("expectation")).isFalse();assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectCases(g).count()).isEqualTo(1);}}
}
