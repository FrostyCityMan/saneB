package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.provider.content.YeonjeGuryeNoticePage;
import com.saneb.domain.announcementsource.provider.content.YeonjeGuryeNoticePage.Site;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import com.saneb.domain.announcementsource.classification.*;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.*;

class YeonjeGuryeDownloadContractTest {
    private String selectItem(boolean y,int id,String ext){
        String name="공고"+id+"."+ext,system="system"+id+"."+ext,path="/ntishome/file/upload/ofr/ofr/20260930";
        if(y)return "<dd><a href='#' onclick=\"goDownload('"+name+"','"+system+"','"+path+"'); return false;\">"+name+"</a><a class=bt_white_s onclick=\"fn_egov_gosi_preview('43358','43358-"+id+"."+ext+"','"+name+"','"+system+"','"+path+"'); return false;\">바로 보기</a></dd>";
        return "<li><a href=\"javascript:goDownLoad('공고"+id+"AAAAAAAAAAAAAAAAAAAAAAAAAAAA==','system"+id+"BBBBBBBBBBBBBBBBBBBBBBBBBBBB==','/ntishoCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC==')\">"+name+"</a></li>";
    }
    private String selectPage(boolean y,String items){
        String title=YeonjeGuryeDownloadCases.selectCase(y).title();
        if(y)return "<nav>메뉴</nav><form id=detailForm><div class=bod_wrap><div class=bod_view><h4>"+title+"</h4><div class=view_info>담당자</div><div class=view_cont>본문 내용</div><dl class=view_file><dt>첨부파일</dt>"+items+"</dl></div></div></form>";
        return "<nav>메뉴</nav><div class=boardGroup><div class=board_view><h3>"+title+"</h3><ul class=write_info><li>담당자</li></ul><div class=board_con>본문 내용</div><ul class=file_down>"+items+"</ul></div></div><form id=command name=nnn method=post action='https://eminwon.gurye.go.kr/emwp/jsp/ofr/FileDownNew.jsp'><input type=hidden name=user_file_nm><input type=hidden name=sys_file_nm><input type=hidden name=file_path><input type=hidden name=CSRFToken value='fixture-only-not-forwarded'></form>";
    }
    @Test @EnabledIfEnvironmentVariable(named="SANEB_YEONJE_GURYE_SURVEY_FIXTURE",matches="true")
    void officialDetailsPreserveCompleteFileSetsAndBody()throws Exception{
        for(boolean y:List.of(true,false)){
            var sample=YeonjeGuryeDownloadCases.selectCase(y);String html=Files.readString(Path.of("build/qa-yeonje-gurye-20260930/"+sample.code()+".html"));
            var result=sample.profile().selectDescriptors(sample.source(),html);
            assertThat(result.status()).isEqualTo("FOUND");assertThat(result.complete()).isTrue();assertThat(result.descriptors()).hasSize(y?1:2).allSatisfy(d->{assertThat(d.downloadAllowed()).isTrue();assertThat(d.selectRequest().form()).doesNotContainKey("CSRFToken");});
            AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(Jsoup.parse(html),sample.title(),sample.titleLayout());
            assertThat(YeonjeGuryeNoticePage.selectContent(Jsoup.parse(html),y?Site.YEONJE:Site.GURYE).text()).isNotBlank();
        }
    }
    @Test void keepsGoodFilesWhileReportingUnknownLinksAndExcludesMenus(){
        for(boolean y:List.of(true,false)){
            var sample=YeonjeGuryeDownloadCases.selectCase(y);String html=selectPage(y,selectItem(y,1,"hwpx")+"<a href='/unknown'>미해석</a>");
            assertThat(YeonjeGuryeNoticePage.selectContent(Jsoup.parse(html),y?Site.YEONJE:Site.GURYE).text()).isEqualTo("본문 내용");
            var r=sample.profile().selectDescriptors(sample.source(),html);assertThat(r.complete()).isFalse();assertThat(r.descriptors()).hasSize(1);
            assertThat(sample.profile().selectDescriptors(sample.source(),selectPage(y,selectItem(y,1,"hwpx"))+"<a href='/outside.pdf'>바깥 파일</a>").descriptors()).hasSize(1);
        }
    }
    @Test void preservesTitlePolicyAndExactTitleBinding()throws Exception{
        for(var sample:YeonjeGuryeDownloadCases.selectCases().toList()){
            var d=new AnnouncementSourceClassificationEngine().selectDecision(new AnnouncementSourceClassificationInput("LOCAL_GOV_NOTICE",sample.title(),null,null,List.of(),BodySourceCode.NONE,BodyAvailabilityCode.UNAVAILABLE),AnnouncementAttachmentRealFileQaTest.selectDraftRuleSet());
            assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectTitleMayProceed(d)).isTrue();
            boolean y=sample.code().startsWith("YEONJE");AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(Jsoup.parse(selectPage(y,"")),sample.title(),sample.titleLayout());
            assertThatThrownBy(()->AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(Jsoup.parse(selectPage(y,"")),"다른 제목",sample.titleLayout())).isInstanceOf(AssertionError.class);
        }
    }
    @Test void rejectsChangedSourceUriAndDownloadRedirect(){
        for(boolean y:List.of(true,false)){
            var s=YeonjeGuryeDownloadCases.selectCase(y);var p=s.profile();var u=p.selectDetailUri(s.source());assertThat(p.selectApprovedRequest(u)).isTrue();
            for(String bad:List.of(u+"&extra=1",u+"&pageIndex=2",u+"#f",u.toString().replace("https:","http:"),u.toString().replace("www.","evil.")))assertThat(p.selectApprovedRequest(URI.create(bad))).isFalse();
            assertThatThrownBy(()->p.selectDetailUri(new AttachmentDiscoveryProfile.Source(s.source().providerCode(),"wrong",s.source().sourceUrl(),s.source().localSourceCode(),s.source().listParserProfileCode()))).isInstanceOf(IllegalArgumentException.class);
            var a=p.selectDescriptors(s.source(),selectPage(y,selectItem(y,1,"hwpx"))).descriptors().getFirst().selectRequest();
            var b=p.selectDescriptors(s.source(),selectPage(y,selectItem(y,2,"pdf"))).descriptors().getFirst().selectRequest();
            assertThat(p.selectApprovedRequest(a,a)).isTrue();assertThat(p.selectApprovedRequest(a,b)).isFalse();
            assertThat(p.selectApprovedRequest(new Request(URI.create(a.uri().toString().replace("eminwon.","evil.")),a.method(),a.form()))).isFalse();
        }
    }
    @Test void retainsUnsupportedMetadataAndCapsFiles(){
        for(boolean y:List.of(true,false)){
            var s=YeonjeGuryeDownloadCases.selectCase(y);var p=s.profile();
            assertThat(p.selectDescriptors(s.source(),selectPage(y,"")).status()).isEqualTo("NO_FILES");
            var mixed=p.selectDescriptors(s.source(),selectPage(y,selectItem(y,1,"pdf")+selectItem(y,2,"xlsx")));assertThat(mixed.complete()).isTrue();assertThat(mixed.descriptors()).extracting(d->d.downloadAllowed()).containsExactly(true,false);
            String many=IntStream.rangeClosed(1,11).mapToObj(i->selectItem(y,i,"hwpx")).collect(Collectors.joining());
            var limited=p.selectDescriptors(s.source(),selectPage(y,many));assertThat(limited.status()).isEqualTo("LIMIT_EXCEEDED");assertThat(limited.descriptors()).hasSize(10);
        }
    }
    @Test void rejectsScriptSuffixTraversalAndAlteredPostForm(){
        var y=YeonjeGuryeDownloadCases.selectCase(true);String item=selectItem(true,1,"hwpx");
        for(String bad:List.of(item.replace("system1.hwpx","../system1.hwpx"),item.replace("return false;","alert(1);")))assertThat(y.profile().selectDescriptors(y.source(),selectPage(true,bad)).descriptors()).isEmpty();
        var g=YeonjeGuryeDownloadCases.selectCase(false);String page=selectPage(false,selectItem(false,1,"pdf"));
        for(String bad:List.of(page.replace("FileDownNew.jsp","FileDown.jsp"),page.replace("name=file_path","name=unexpected"),page.replace("type=hidden name=user_file_nm","type=text name=user_file_nm"),page.replace("</form>","<input type=hidden name=sys_file_nm></form>")))assertThat(g.profile().selectDescriptors(g.source(),bad).complete()).isFalse();
        var request=g.profile().selectDescriptors(g.source(),page).descriptors().getFirst().selectRequest();var fields=new HashMap<>(request.form());fields.put("CSRFToken","fixture-only");assertThat(g.profile().selectApprovedRequest(new Request(request.uri(),"POST",fields))).isFalse();
    }
    @Test void rejectsMissingDuplicateAndNestedStructure(){
        for(boolean y:List.of(true,false)){
            var s=YeonjeGuryeDownloadCases.selectCase(y);String page=selectPage(y,selectItem(y,1,"pdf"));
            for(String bad:List.of(page+page,page.replace(y?"class=view_file":"class=file_down","class=other"),page.replace(y?"<h4>":"<h3>",y?"<div><h4>":"<div><h3>").replace(y?"</h4>":"</h3>",y?"</h4></div>":"</h3></div>")))assertThat(s.profile().selectDescriptors(s.source(),bad).complete()).isFalse();
            var dup=s.profile().selectDescriptors(s.source(),selectPage(y,selectItem(y,1,"pdf")+selectItem(y,1,"pdf")));assertThat(dup.descriptors()).hasSize(1);assertThat(dup.complete()).isTrue();
        }
    }
}
