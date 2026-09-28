package com.saneb.domain.announcementattachment.discovery;

import static org.assertj.core.api.Assertions.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

class GuroGosiAttachmentDiscoveryProfileTest {
    private final GuroGosiAttachmentDiscoveryProfile profile=new GuroGosiAttachmentDiscoveryProfile();
    private final String url="https://www.guro.go.kr/www/selectBbsNttGosiView.do?bbsNo=663&nttNo=49626&key=1791";
    private AttachmentDiscoveryProfile.Source selectSource(String value){var n=new AnnouncementSourceIdentityNormalizer();
        return new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(value)),value,"LGS-000018","SAEOL_GOSI");}
    private String selectFile(){return "https://eminwon.guro.go.kr/emwp/jsp/ofr/FileDown.jsp?user_file_nm=지원.pdf&amp;sys_file_nm=지원_1.pdf&amp;file_path=/ntishome/file/upload/ofr/ofr/20260227";}
    private String selectItem(){return "<li class='p-attach__item'><a class='p-attach__link' href='"+selectFile()+"'><span class='p-icon'>pdf</span><span>지원.pdf</span><svg><use xlink:href='/common/images/program/p-icon.svg#arrow-circle-down'>파일다운로드</use></svg></a>"
            +"<a class='p-attach__preview' href='"+selectFile().replace("https://eminwon.guro.go.kr/emwp/jsp/ofr/FileDown.jsp","/previewBbs.do")+"'>미리보기</a></li>";}
    private String selectPage(String items){return "<div class='p-wrap bbs bbs__view'><table class='p-table block'><tr><th>제목</th><td><span class='p-table__subject_text'>소상공인 융자지원 공고</span></td></tr>"
            +"<tr><td title='내용' class='p-table__content'>지원 내용</td></tr><tr><th>파일</th><td>"+items+"</td></tr></table></div>";}
    @Test void selectsOnlyOfficialCellAndNeverRequestsPreview(){
        var r=profile.selectDescriptors(selectSource(url),selectPage("<ul class='p-attach'>"+selectItem()+"</ul>"));
        assertThat(r.complete()).isTrue();assertThat(r.descriptors()).hasSize(1);
        var f=r.descriptors().getFirst();assertThat(f.expectedFormat()).isEqualTo("PDF");assertThat(f.documentRole()).isEqualTo("UNKNOWN");
        assertThat(profile.selectApprovedRequest(f.fetchUri())).isTrue();
        assertThat(profile.selectApprovedRequest(URI.create(url.replace("/www/selectBbsNttGosiView.do","/previewBbs.do")))).isFalse();
        assertThat(profile.selectDescriptors(selectSource(url),selectPage("")).status()).isEqualTo("NO_FILES");
    }
    @Test void normalizesOnlyKnownStoredSearchParametersAndRejectsOtherSources(){
        assertThat(profile.selectDetailUri(selectSource(url+"&&rowNum=1663&searchCnd=SJ&searchKrwd=abc&"))).isEqualTo(URI.create(url));
        for(String bad:List.of(url.replace("https:","http:"),url.replace("663","662"),url.replace("1791","1"),url+"&nttNo=1",url+"&redirect=https://evil.invalid"))
            assertThatThrownBy(()->profile.selectDetailUri(selectSource(bad))).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void rejectsChangedOrAmbiguousAttachmentArea(){
        String page=selectPage("<ul class='p-attach'>"+selectItem()+"</ul>");
        for(String bad:List.of(page+page,page.replace(">파일<",">다른파일<"),page.replace("지원.pdf</span>","다른.pdf</span>"),
                page.replace("p-attach__item'","p-attach__item' onclick='evil()'"),page.replace("</li>","<a href='https://evil.invalid'>외부</a></li>"),
                page.replace("eminwon.guro.go.kr","evil.invalid"),page.replace("/20260227","/../private")))
            assertThat(profile.selectDescriptors(selectSource(url),bad).complete()).isFalse();
        assertThat(profile.selectDescriptors(selectSource(url),selectPage("첨부 확인 필요")).complete()).isFalse();
    }
    @Test void forbidsRedirectsToOtherNoticeOrFile(){
        var initial=AttachmentPinnedDownloadClient.Request.selectGet(URI.create(url));
        assertThat(profile.selectApprovedRequest(initial,initial)).isTrue();
        assertThat(profile.selectApprovedRequest(initial,AttachmentPinnedDownloadClient.Request.selectGet(URI.create(url.replace("49626","39520"))))).isFalse();
        for(String bad:List.of("http://eminwon.guro.go.kr/emwp/jsp/ofr/FileDown.jsp?a=b","https://127.0.0.1/file.pdf",selectFile().replace("&amp;","&").replace("지원.pdf","../지원.pdf")))
            assertThat(profile.selectApprovedRequest(URI.create(bad))).isFalse();
    }
    @Test void unknownEmptyAreaUnsupportedTypesAndOverflowStayVisible(){
        assertThat(profile.selectDescriptors(selectSource(url),selectPage("<ul class='p-attach'>목록 로딩 실패</ul>")).complete()).isFalse();
        var unsupported=profile.selectDescriptors(selectSource(url),selectPage("<ul class='p-attach'>"+selectItem().replace(".pdf",".docx")+"</ul>"));
        assertThat(unsupported.complete()).isTrue();assertThat(unsupported.descriptors()).hasSize(1);
        assertThat(unsupported.descriptors().getFirst().downloadAllowed()).isFalse();
        StringBuilder items=new StringBuilder();for(int i=0;i<11;i++)items.append(selectItem().replace("지원_1.pdf","지원_"+i+".pdf"));
        var overflow=profile.selectDescriptors(selectSource(url),selectPage("<ul class='p-attach'>"+items+"</ul>"));
        assertThat(overflow.status()).isEqualTo("LIMIT_EXCEEDED");assertThat(overflow.complete()).isFalse();assertThat(overflow.descriptors()).hasSize(10);
    }
    @Test @EnabledIfEnvironmentVariable(named="SANEB_GURO_SAVED_DETAIL",matches=".+")
    void validatesLocallySavedOfficialDetailWithoutNewRequest() throws Exception {
        var r=profile.selectDescriptors(selectSource(url),Files.readString(Path.of(System.getenv("SANEB_GURO_SAVED_DETAIL"))));
        assertThat(r.complete()).isTrue();assertThat(r.status()).isEqualTo("FOUND");assertThat(r.descriptors()).hasSize(1);
        assertThat(r.descriptors().getFirst().expectedFormat()).isEqualTo("PDF");
    }
}
