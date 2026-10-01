package com.saneb.domain.announcementattachment.discovery;

import static org.assertj.core.api.Assertions.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.UlsanNamguNoticePage;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient.Request;
import java.net.URI;
import java.util.List;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

class UlsanNamguAttachmentDiscoveryProfileTest {
    private final UlsanNamguAttachmentDiscoveryProfile profile = new UlsanNamguAttachmentDiscoveryProfile();
    private final String url = "https://eminwon.ulsannamgu.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do?context=NTIS&homepage_pbs_yn=Y&jndinm=OfrNotAncmtEJB&method=selectOfrNotAncmt&methodnm=selectOfrNotAncmtRegst&subCheck=Y&not_ancmt_mgt_no=53732";
    private AttachmentDiscoveryProfile.Source source() {
        var identity = new AnnouncementSourceIdentityNormalizer();
        return new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",identity.hash(identity.canonicalizeUrl(url)),url,"LGS-000079","SAFE_SAEOL_EMINWON_COMPACT");
    }
    private String link(int index,String extension) {
        return "<li><a href=\"javascript:goDownLoad('안내."+extension+"','saved"+index+"."+extension+"','/ntishome/file/upload/ofr/ofr/20261001')\">안내."+extension+"</a></li>";
    }
    private String page(String links) {
        return "<form name='form1' method='post'><div class='bbs_detail bbs_detail_basic'>"
                +"<div class='bbs_detail_tit'><h2>소상공인 지원사업</h2><ul class='info'><li>담당 정보</li></ul></div>"
                +"<ul class='bbs_detail_content2'>"+links+"</ul><ul class='bbs_detail_content2'><li>공고번호 정보</li><li>공고기간 정보</li></ul>"
                +"<div class='bbs-view-content'>본문 안내</div></div><div class='pdt10'><a href='/list'>목록</a></div></form>";
    }
    @Test void measuredTitleBodyAndFileRegionsStaySeparate() {
        var html = page(link(1,"hwpx"));
        var root = UlsanNamguNoticePage.selectRoot(Jsoup.parse(html));
        assertThat(UlsanNamguNoticePage.selectTitle(root).text()).isEqualTo("소상공인 지원사업");
        assertThat(UlsanNamguNoticePage.selectContent(root).text()).isEqualTo("본문 안내");
        var result = profile.selectDescriptors(source(),html);
        assertThat(result.complete()).isTrue();
        assertThat(result.descriptors()).hasSize(1);
        var file = result.descriptors().getFirst();
        assertThat(file.expectedFormat()).isEqualTo("HWPX");
        assertThat(file.documentRole()).isEqualTo("UNKNOWN");
        assertThat(file.fetchUri().getPath()).isEqualTo("/emwp/jsp/ofr/FileDown.jsp");
        assertThat(profile.selectApprovedRequest(file.selectRequest())).isTrue();
        assertThat(profile.selectApprovedRequest(file.selectRequest(),file.selectRequest())).isTrue();
    }
    @Test void partialAndUnsupportedFilesDoNotDiscardSuccessfulDescriptors() {
        var result = profile.selectDescriptors(source(),page(link(1,"hwpx")+"<li><a href='/unknown'>미해석</a></li>"+link(2,"jpg")));
        assertThat(result.complete()).isFalse();
        assertThat(result.warnings()).contains("ATTACHMENT_LINK_UNRESOLVED");
        assertThat(result.descriptors()).hasSize(2);
        assertThat(result.descriptors().getFirst().downloadAllowed()).isTrue();
        assertThat(result.descriptors().getLast().downloadAllowed()).isFalse();
    }
    @Test void unknownEmptyOrMovedRegionsAreNotClaimedAsNoAttachments() {
        var html = page(link(1,"hwpx"));
        for (String value:List.of(page(""),html.replace("bbs_detail_basic","other"),html.replace("<h2>","<h3>").replace("</h2>","</h3>"),
                html.replace("<li>공고번호 정보</li>",link(2,"hwp")),html+"<form></form>",
                html.replaceFirst("<ul class='bbs_detail_content2'>","<ul class='bbs_detail_content2' onclick='unknown()'>"))) {
            var result = profile.selectDescriptors(source(),value);
            assertThat(result.status()).isEqualTo("FAILED");
            assertThat(result.complete()).isFalse();
        }
    }
    @Test void fixedOfficialSourceAndFilePathCannotBeSubstituted() {
        assertThat(profile.selectDetailUri(source()).toString()).isEqualTo(url);
        for (String bad:List.of(url+"&extra=1",url+"&subCheck=Y",url+"#fragment",url.replace("https:","http:"),url.replace("ulsannamgu","other")))
            assertThat(profile.selectApprovedRequest(URI.create(bad))).isFalse();
        var file = profile.selectDescriptors(source(),page(link(1,"hwpx"))).descriptors().getFirst();
        assertThat(profile.selectApprovedRequest(file.selectRequest(),Request.selectGet(URI.create(file.fetchUri().toString().replace("saved1","saved2"))))).isFalse();
        var wrong = new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",source().providerNoticeId(),url,"LGS-000001","SAFE_SAEOL_EMINWON_COMPACT");
        assertThatThrownBy(()->profile.selectDetailUri(wrong)).hasMessage("PROFILE_REQUIRED");
    }
    @Test void duplicateAndMaximumFileLimitsArePreserved() {
        assertThat(profile.selectDescriptors(source(),page(link(1,"hwpx")+link(1,"hwpx"))).descriptors()).hasSize(1);
        String links = java.util.stream.IntStream.rangeClosed(1,11).mapToObj(i->link(i,"hwpx")).collect(java.util.stream.Collectors.joining());
        var result=profile.selectDescriptors(source(),page(links));
        assertThat(result.status()).isEqualTo("LIMIT_EXCEEDED");
        assertThat(result.descriptors()).hasSize(10);
    }
}
