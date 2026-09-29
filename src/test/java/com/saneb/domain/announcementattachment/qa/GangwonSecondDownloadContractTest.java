package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.provider.content.GangwonSecondNoticePage;
import com.saneb.domain.announcementsource.provider.content.GangwonSecondNoticePage.Site;
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

class GangwonSecondDownloadContractTest {
    static Stream<String> selectGroups(){return GangwonSecondDownloadCases.GROUPS.stream().sorted();}
    private String selectItem(String group,int id,String ext){
        String name="지원 공고문."+ext;
        if(group.equals("INJE"))return "<div class='attachFile'><a data-ajax='false' href='/egf/bp/board/article/download?fileSeq="+id+"'><span class='icoFile'></span>"+name+"</a><span class='attachFile-txt'>(다운로드 수: 147)</span></div>";
        String call="javascript:opendownload('announcement', 159267,"+id+")",stored="1787891871440_508585226."+ext;
        return "<li><p class='filename'><a href=\""+call+"\">"+name+"</a></p><span class='docview docdown'><a href=\""+call+"\">다운로드<img src='/icon'></a></span>"
                +"<span class='docview'><a href='/synap/docview?filePath=/data1/yanggu_uploadfiles/board/announcement/&filename="+stored+"'>미리보기<img src='/preview-icon'></a></span>"
                +"<span class='docview'><input id='SBPAPIBUN' type='button' onclick=\"javascript:fn_sbapi_preview('"+stored+"','board/announcement','159267'); return false;\"><input type='hidden' name='backboardfilename' value='"+stored+"'><input type='hidden' name='backbcd' value='board/announcement'><input type='hidden' name='backboardkey' value='159267'></span><p class='hit'><span>down :</span> 55</p><p class='clear'></p></li>";
    }
    private String selectPage(String group,String files){
        if(group.equals("YANGGU"))return "<nav>수출 메뉴</nav><div id='user_board_whole'><form id='registform' name='registform' method='post'><fieldset><div id='user_board_read_title'>소상공인 지원</div><div class='read_information'>담당자</div><div id='user_board_read_view'><div class='user_board_read_view_pre'>소상공인 지원금</div></div><div id='user_board_read_file'><table><tr><th>파일</th><td class='file_list'><ul>"+files+"</ul></td></tr></table></div></fieldset></form></div><footer>특허 푸터</footer>";
        return "<nav>수출 메뉴</nav><div class='skinTb skinTb-data-resList skinTb-data-bgSbj'><div class='skinTb-tr'><div class='skinTb-th'>제목</div><div class='skinTb-td skinTb-sbj'>소상공인 지원</div></div><div class='skinTb-tr'><div class='skinTb-td skinTb-conts'>소상공인 지원금</div></div><div class='skinTb-tr'><div class='skinTb-th'>첨부파일</div><div class='skinTb-td'>"+files+"</div></div></div><footer>특허 푸터</footer>";
    }
    @ParameterizedTest @MethodSource("selectGroups") void keepsGoodFilesAndSeparatesUnknownAndUnsupported(String group){
        var s=GangwonSecondDownloadCases.selectCase(group);var p=s.profile();String files=selectItem(group,1,"hwp")+selectItem(group,2,"pdf")+selectItem(group,3,"hwpx");
        assertThat(p.selectLegacyBinaryContentTypes()).isEqualTo(group.equals("YANGGU")?Set.of("application/octer-stream"):Set.of());
        assertThat(p.selectUtf8DispositionOctets()).isEqualTo(group.equals("YANGGU"));
        var r=p.selectDescriptors(s.source(),selectPage(group,files));assertThat(r.complete()).isTrue();assertThat(r.descriptors()).hasSize(3);r.descriptors().forEach(d->{assertThat(d.documentRole()).isEqualTo("UNKNOWN");assertThat(d.downloadAllowed()).isTrue();assertThat(p.selectApprovedRequest(d.selectRequest())).isTrue();});
        for(String extra:List.of("<a href='/unknown'>다른 첨부</a>","<button>파일</button>","<img src='/unknown'>")){var partial=p.selectDescriptors(s.source(),selectPage(group,files+extra));assertThat(partial.complete()).isFalse();assertThat(partial.descriptors()).hasSize(3);}
        var unsupported=p.selectDescriptors(s.source(),selectPage(group,files+selectItem(group,4,"jpg")));assertThat(unsupported.descriptors()).hasSize(4);assertThat(unsupported.descriptors().getLast().downloadAllowed()).isFalse();
        assertThat(p.selectDescriptors(s.source(),selectPage(group,"")).status()).isEqualTo("NO_FILES");assertThat(p.selectDescriptors(s.source(),"<main>첨부 없음</main>").complete()).isFalse();
    }
    @ParameterizedTest @MethodSource("selectGroups") void bindsSourceMethodFileLimitsAndBody(String group){
        var s=GangwonSecondDownloadCases.selectCase(group);var p=s.profile();String one=selectItem(group,1,"hwp");
        assertThat(p.selectDescriptors(s.source(),selectPage(group,one+one)).descriptors()).hasSize(1);
        var conflict=p.selectDescriptors(s.source(),selectPage(group,one+one.replace("지원 공고문.hwp","변경 공고문.hwp")));assertThat(conflict.complete()).isFalse();assertThat(conflict.descriptors()).hasSize(1);
        var limit=p.selectDescriptors(s.source(),selectPage(group,IntStream.rangeClosed(1,11).mapToObj(i->selectItem(group,i,"hwp")).collect(Collectors.joining())));assertThat(limit.status()).isEqualTo("LIMIT_EXCEEDED");assertThat(limit.descriptors()).hasSize(10);
        var request=p.selectDescriptors(s.source(),selectPage(group,one)).descriptors().getFirst().selectRequest();assertThat(p.selectApprovedRequest(request,request)).isTrue();assertThat(p.selectApprovedRequest(request,Request.selectGet(p.selectDetailUri(s.source())))).isFalse();assertThat(p.selectApprovedRequest(new Request(request.uri(),"POST",Map.of("unused","value")))).isFalse();
        for(String bad:List.of("https:opaque",request.uri().toString().replace("https:","http:"),request.uri().toString().replace(Site.valueOf(group).host,"evil.example"),request.uri()+"&unknown=1",request.uri()+"#x"))assertThat(p.selectApprovedRequest(URI.create(bad))).isFalse();
        var n=new AnnouncementSourceIdentityNormalizer();for(String bad:List.of(s.source().sourceUrl()+"&unknown=1",s.source().sourceUrl().replace("https:","http:"),s.source().sourceUrl()+"&"+Site.valueOf(group).idKey+"=0"))assertThatThrownBy(()->p.selectDetailUri(new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(bad)),bad,s.source().localSourceCode(),s.source().listParserProfileCode()))).hasMessage("PROFILE_REQUIRED");
        var page=Jsoup.parse(selectPage(group,one));assertThat(GangwonSecondNoticePage.selectContent(Site.valueOf(group),page).text()).isEqualTo("소상공인 지원금");AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,"소상공인 지원",s.titleLayout());
        var budget=new AnnouncementAttachmentBbsOfficialObservationTest.Budget(p,false);assertThat(budget.maximumRequests).isEqualTo(6);assertThat(budget.maximumBytes).isEqualTo(23L*1024*1024);
    }
    @Test void yangguPreviewAndDuplicateMustMatchButCannotSuppressGoodFile(){
        var s=GangwonSecondDownloadCases.selectCase("YANGGU");String item=selectItem("YANGGU",1,"hwpx");
        for(String bad:List.of(item.replace("fn_sbapi_preview(","otherPreview("),item.replace("value='159267'","value='159268'"),item.replace("href='/synap/docview","href='https://evil.example/synap/docview"))){var r=s.profile().selectDescriptors(s.source(),selectPage("YANGGU",bad));assertThat(r.complete()).isFalse();assertThat(r.descriptors()).hasSize(1);}
        var r=s.profile().selectDescriptors(s.source(),selectPage("YANGGU",item.replace("opendownload('","opendownload('../")));assertThat(r.complete()).isFalse();assertThat(r.descriptors()).isEmpty();
    }
    @EnabledIfEnvironmentVariable(named="SANEB_GANGWON_SECOND_SURVEY_FIXTURE",matches="true")
    @ParameterizedTest @MethodSource("selectGroups") void actualOfficialBoundaries(String group)throws Exception{
        var s=GangwonSecondDownloadCases.selectCase(group);var html=Files.readString(Path.of("build/qa-gangwon-second-20260929/"+group+"-detail.html"));var page=Jsoup.parse(html);AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(page,s.title(),s.titleLayout());var r=s.profile().selectDescriptors(s.source(),html);assertThat(r.complete()).as(group).isTrue();assertThat(r.descriptors()).hasSize(1);assertThat(GangwonSecondNoticePage.selectContent(Site.valueOf(group),page).text()).contains("소상공인");
    }
    @Test void catalogStaysReferenceOnly()throws Exception{
        var json=new com.fasterxml.jackson.databind.ObjectMapper();var notices=json.readTree(Files.readString(Path.of("src/main/resources/announcement-attachment/provider-qa-catalog-v2.json"))).path("notices");
        for(String group:selectGroups().toList()){var s=GangwonSecondDownloadCases.selectCase(group);var ref=StreamSupport.stream(notices.spliterator(),false).filter(n->s.code().equals(n.path("caseCode").asText())).findFirst().orElseThrow();assertThat(ref.path("source")).isEqualTo(json.valueToTree(s.source()));assertThat(ref.hasNonNull("expectation")).isFalse();assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectCases(group).count()).isEqualTo(1);}
    }
}
