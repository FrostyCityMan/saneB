package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient;
import com.saneb.domain.announcementsource.provider.content.UlsanNamguNoticePage;
import java.net.URI;
import java.util.List;
import java.util.Set;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

/** 공식 고시공고의 div/ul 영역만 기존 새올 GET 엔진에 연결한다. 보조 게시판으로 대체하지 않는다. */
public final class UlsanNamguAttachmentDiscoveryProfile implements AttachmentDiscoveryProfile {
    private final SaeolGetAttachmentDiscoveryProfile delegate = new SaeolGetAttachmentDiscoveryProfile(
            "LOCAL_ULSAN_NAMGU_GET_V1", "LGS-000079", "eminwon.ulsannamgu.go.kr", "SAFE_SAEOL_EMINWON_COMPACT", "th", false);
    private final String hash = new AnnouncementSourceIdentityNormalizer().hash(delegate.selectProfileHash()+"|"
            +AttachmentProfileFingerprint.selectHash("ULSAN_NAMGU:1|form1-post|bbs_detail_basic|first-of-two-bbs_detail_content2|whole-list|no-empty-proof|same-uri-redirect",
                    getClass())+"|"+AttachmentProfileFingerprint.selectHash("ULSAN_NAMGU_DOM:1",UlsanNamguNoticePage.class));

    @Override public String selectProviderCode() { return delegate.selectProviderCode(); }
    @Override public String selectProfileCode() { return delegate.selectProfileCode(); }
    @Override public String selectProfileHash() { return hash; }
    @Override public List<SourceBinding> selectSourceBindings() { return delegate.selectSourceBindings(); }
    @Override public Set<String> selectApprovedHosts() { return delegate.selectApprovedHosts(); }
    @Override public URI selectDetailUri(String id) { return delegate.selectDetailUri(id); }
    @Override public URI selectDetailUri(Source source) { return delegate.selectDetailUri(source); }
    @Override public boolean selectApprovedRequest(URI uri) { return delegate.selectApprovedRequest(uri); }
    @Override public boolean selectApprovedRequest(AttachmentPinnedDownloadClient.Request initial,AttachmentPinnedDownloadClient.Request next) {
        return selectApprovedRequest(initial)&&selectApprovedRequest(next)&&initial.uri().equals(next.uri());
    }
    @Override public Result selectDescriptors(String id,String html) { throw new IllegalArgumentException("PROFILE_REQUIRED"); }
    @Override public Result selectDescriptors(Source source,String html) {
        URI detail = selectDetailUri(source);
        if (html==null || html.length()>1_000_000) return new Result("FAILED",false,List.of(),List.of("ATTACHMENT_DETAIL_UNAVAILABLE"));
        Element container;
        try { container = UlsanNamguNoticePage.selectAttachments(UlsanNamguNoticePage.selectRoot(Jsoup.parse(html,detail.toASCIIString()))); }
        catch (IllegalArgumentException failure) { return new Result("FAILED",false,List.of(),List.of("ATTACHMENT_SELECTOR_CHANGED")); }
        var standard = new Element("form").attr("name","form1").attr("method","post");
        var row = standard.appendElement("table").appendElement("tr");
        row.appendElement("th").text("첨부파일");
        row.appendElement("td").html(container.html());
        return delegate.selectDescriptors(source,standard.outerHtml());
    }
}
