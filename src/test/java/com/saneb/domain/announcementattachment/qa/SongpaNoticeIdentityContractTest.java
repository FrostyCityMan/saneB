package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;
import com.saneb.domain.announcementsource.provider.content.*;
import java.net.URI;
import java.util.List;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SongpaNoticeIdentityContractTest {
    private static final String DETAIL="https://www.songpa.go.kr/www/selectGosiData.do?key=2776&not_ancmt_mgt_no=33174";
    private static final String TITLE="2026년 송파구 중소기업 융자지원(협력자금) 계획 공고";
    private static final String FIELD="<input type=hidden name=not_ancmt_mgt_no value=33174>";
    private String page(String field) {
        return "<div class='p-wrap bbs bbs__view'><form name=gosiFrm>"+field+"<table class='p-table block'>"
                +"<tr><th>제목</th><td></td><th>담당부서</th><td>부서</td></tr>"
                +"<tr><th>내용</th><td>중소기업 지원 내용</td></tr><tr><th>파일</th><td><ul class=view_attach>"
                +"<li><a href=\"javascript:gourl('http://songpa.eminwon.seoul.kr/emwp/jsp/ofr/FileDown.jsp?user_file_nm=지원.hwpx&sys_file_nm=stored.hwpx&file_path=/ntishome/file/upload/ofr/ofr/20260629');\">지원.hwpx</a></li>"
                +"</ul></td></tr></table></form></div>";
    }
    private String link(String id,String title) {return "<table><tr><td class=p-subject><a href='./selectGosiData.do?key=2776&not_ancmt_mgt_no="+id+"'>"+title+"</a></td></tr></table>";}

    @Test void missingTitleKeepsFilesButNeverClaimsCompleteDiscovery() {
        var sample=SeoulFourthDownloadCases.selectCase("SONGPA");
        var p=Jsoup.parse(page(FIELD),DETAIL);
        assertThat(SeoulFourthNoticePage.selectContent(SeoulFourthNoticePage.Site.SONGPA,p).text()).isEqualTo("중소기업 지원 내용");
        var result=sample.profile().selectDescriptors(sample.source(),page(FIELD));
        assertThat(result.descriptors()).hasSize(1);assertThat(result.descriptors().getFirst().downloadAllowed()).isTrue();
        assertThat(result.complete()).isFalse();assertThat(result.status()).isEqualTo("FAILED");
        assertThat(result.warnings()).containsExactly("ATTACHMENT_DETAIL_TITLE_UNAVAILABLE");
        assertThatThrownBy(()->AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(p,TITLE,sample.titleLayout())).isInstanceOf(AssertionError.class);
    }
    @ParameterizedTest @ValueSource(strings={"", "<input type=hidden name=not_ancmt_mgt_no value=33175>", "<input name=not_ancmt_mgt_no value=33174>", "<input type=hidden name=not_ancmt_mgt_no value=33174><input type=hidden name=not_ancmt_mgt_no value=33174>"})
    void missingDuplicateOrDifferentIdCannotDiscover(String field) {
        var sample=SeoulFourthDownloadCases.selectCase("SONGPA");
        var result=sample.profile().selectDescriptors(sample.source(),page(field));
        assertThat(result.descriptors()).isEmpty();assertThat(result.complete()).isFalse();
    }
    @Test void rejectsWrongDetailOriginAndAmbiguousStructure() {
        for(String url:List.of(DETAIL.replace("https:","http:"),DETAIL.replace("www.songpa.go.kr","evil.example"),DETAIL+"&extra=1",DETAIL+"#fragment",DETAIL.replace("33174","33175")))
            assertThatThrownBy(()->SeoulFourthNoticePage.selectRoot(SeoulFourthNoticePage.Site.SONGPA,Jsoup.parse(page(FIELD),url))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->SeoulFourthNoticePage.selectRoot(SeoulFourthNoticePage.Site.SONGPA,Jsoup.parse(page(FIELD)+page(FIELD),DETAIL))).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void currentOfficialListRequiresSameIdAndTitleExactlyOnce() {
        var uri=SongpaNoticeIdentity.selectListUri(TITLE);
        SongpaNoticeIdentity.validateListTitle(Jsoup.parse(link("33174",TITLE),uri.toASCIIString()),URI.create(DETAIL),TITLE);
        // 공식 검색 결과는 검색어의 한글·공백을 href에 그대로 되돌려 준다. query 공백만 URI로 표현한다.
        SongpaNoticeIdentity.validateListTitle(Jsoup.parse(link("33174",TITLE).replace("33174'", "33174&searchCnd=SJ&searchKrwd="+TITLE+"&pageIndex=1'"),uri.toASCIIString()),URI.create(DETAIL),TITLE);
        for(String html:List.of(link("33175",TITLE),link("33174","다른 제목"),link("33174",TITLE)+link("33174",TITLE),link("33174",TITLE).replace("./selectGosiData.do","https://evil.example/www/selectGosiData.do")))
            assertThatThrownBy(()->SongpaNoticeIdentity.validateListTitle(Jsoup.parse(html,uri.toASCIIString()),URI.create(DETAIL),TITLE)).hasMessage("SONGPA_NOTICE_IDENTITY_UNCONFIRMED");
        assertThatThrownBy(()->SongpaNoticeIdentity.validateListTitle(Jsoup.parse(link("33174",TITLE),"https://evil.example/"),URI.create(DETAIL),TITLE)).hasMessage("SONGPA_NOTICE_IDENTITY_UNCONFIRMED");
    }
    @Test void listRequestIsSingleExactBoundedGetAndNotAGeneralProfilePermission() {
        var sample=SeoulFourthDownloadCases.selectCase("SONGPA");
        var initial=AttachmentPinnedDownloadClient.Request.selectGet(SongpaNoticeIdentity.selectListUri(TITLE));
        var budget=AnnouncementAttachmentBbsOfficialObservationTest.selectBudget(sample.profile(),true,false);
        assertThat(budget.maximumRequests).isEqualTo(7);assertThat(budget.maximumBytes).isEqualTo(23L*1024*1024);
        assertThat(sample.profile().selectApprovedRequest(initial)).isFalse();
        assertThat(budget.selectSongpaIdentityListRequestAllowed(initial,AttachmentPinnedDownloadClient.Request.selectGet(URI.create(initial.uri()+"&extra=1")))).isFalse();
        assertThat(budget.selectSongpaIdentityListRequestAllowed(initial,initial)).isTrue();
        var other=AnnouncementAttachmentBbsOfficialObservationTest.selectBudget(SeoulFourthDownloadCases.selectCase("SEONGDONG").profile(),true,false);
        assertThat(other.selectSongpaIdentityListRequestAllowed(initial,initial)).isFalse();
        assertThat(budget.selectSongpaIdentityListRequestAllowed(initial,initial)).isFalse();
        var exhausted=AnnouncementAttachmentBbsOfficialObservationTest.selectBudget(sample.profile(),true,false);exhausted.requests=7;
        assertThat(exhausted.selectSongpaIdentityListRequestAllowed(initial,initial)).isFalse();
        for(String url:List.of(initial.uri()+"&extra=1",initial.uri().toString().replace("www.songpa.go.kr","evil.example"),initial.uri().toString().replace("https:","http:"))) {
            var request=AttachmentPinnedDownloadClient.Request.selectGet(URI.create(url));
            assertThat(AnnouncementAttachmentBbsOfficialObservationTest.selectBudget(sample.profile(),true,false).selectSongpaIdentityListRequestAllowed(request,request)).isFalse();
        }
    }
}
